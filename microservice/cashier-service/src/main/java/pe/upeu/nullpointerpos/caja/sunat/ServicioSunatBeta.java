package pe.upeu.nullpointerpos.caja.sunat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Servicio encargado de generar el XML UBL 2.1, firmar digitalmente y
 * gestionar la comunicación con el WebService SOAP de SUNAT Beta.
 */
@Service
public class ServicioSunatBeta {
    private static final Logger LOG = LoggerFactory.getLogger(ServicioSunatBeta.class);
    private final ConfiguracionSunat config;

    public ServicioSunatBeta(ConfiguracionSunat config) {
        this.config = config;
    }

    public record ResultadoEmisionXml(
        String serie,
        Long correlativo,
        String numeroCompleto,
        String xmlFirmado,
        String hashCpe,
        String cadenaQr,
        String codigoCdr,
        String descripcionCdr,
        String estadoSunat
    ) {}

    public ResultadoEmisionXml procesarEmision(
        String tipoComprobante,
        Long correlativo,
        String tipoDocReceptor,
        String numDocReceptor,
        String nombreReceptor,
        String direccionReceptor,
        BigDecimal totalImporte,
        String metodoPago,
        String referenciaOperacion
    ) {
        var preparado = prepararEmision(tipoComprobante, correlativo, tipoDocReceptor,
            numDocReceptor, nombreReceptor, direccionReceptor, totalImporte, metodoPago, referenciaOperacion);
        return finalizar(preparado.serie(), preparado.correlativo(), preparado.numeroCompleto(), preparado.xmlFirmado(), preparado.cadenaQr());
    }

    public ResultadoEmisionXml prepararEmision(
        String tipoComprobante,
        Long correlativo,
        String tipoDocReceptor,
        String numDocReceptor,
        String nombreReceptor,
        String direccionReceptor,
        BigDecimal totalImporte,
        String metodoPago,
        String referenciaOperacion
    ) {
        boolean esFactura = "FACTURA".equalsIgnoreCase(tipoComprobante);
        String codigoTipoCpe = esFactura ? "01" : "03";
        String serie = esFactura ? "F001" : "B001";
        String correlativoStr = String.format(java.util.Locale.ROOT, "%08d", correlativo);
        String numeroCompleto = serie + "-" + correlativoStr;

        // Cálculos tributarios oficiales (IGV 18%)
        BigDecimal total = totalImporte.setScale(2, RoundingMode.HALF_UP);
        BigDecimal divisorIgv = new BigDecimal("1.18");
        BigDecimal totalGravado = total.divide(divisorIgv, 2, RoundingMode.HALF_UP);
        BigDecimal totalIgv = total.subtract(totalGravado).setScale(2, RoundingMode.HALF_UP);

        String fechaEmision = LocalDate.now(java.time.ZoneId.of("America/Lima")).format(DateTimeFormatter.ISO_DATE);
        String horaEmision = LocalTime.now(java.time.ZoneId.of("America/Lima")).format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        // Código tipo documento SUNAT receptor
        String codTipoDocRec = "0"; // Sin documento / Clientes varios
        String numDocRec = numDocReceptor != null && !numDocReceptor.isBlank() ? numDocReceptor.trim() : "00000000";
        String razonSocialRec = nombreReceptor != null && !nombreReceptor.isBlank() ? nombreReceptor.trim() : "CLIENTES VARIOS";

        if ("DNI".equalsIgnoreCase(tipoDocReceptor)) codTipoDocRec = "1";
        else if ("RUC".equalsIgnoreCase(tipoDocReceptor)) codTipoDocRec = "6";

        // Código SUNAT método de pago (Catálogo 59)
        String codigoMedioPago = "008"; // Efectivo por defecto
        if ("TARJETA".equalsIgnoreCase(metodoPago)) codigoMedioPago = "005"; // Tarjeta de débito/crédito
        else if ("YAPE".equalsIgnoreCase(metodoPago) || "PLIN".equalsIgnoreCase(metodoPago)) codigoMedioPago = "006"; // Billetera digital

        // 1. Construir XML UBL 2.1
        String xml = construirXmlUbl21(
            codigoTipoCpe, serie, correlativoStr, fechaEmision, horaEmision,
            config.getRucEmisor(), config.getRazonSocial(), config.getNombreComercial(), config.getDireccion(), config.getUbigeo(),
            codTipoDocRec, numDocRec, razonSocialRec,
            totalGravado, totalIgv, total, codigoMedioPago, metodoPago, referenciaOperacion
        );

        String cadenaQr = String.format(java.util.Locale.ROOT,
            "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
            config.getRucEmisor(), codigoTipoCpe, serie, correlativoStr,
            totalIgv, total, fechaEmision, codTipoDocRec, numDocRec, "");

        return new ResultadoEmisionXml(serie, correlativo, numeroCompleto, xml, "", cadenaQr,
            "", "Pendiente de firma y envio a SUNAT Beta.", "PENDIENTE_FIRMA");
    }

    public ResultadoEmisionXml reenviar(RespuestaComprobanteSunat cpe, String xml) {
        return finalizar(cpe.serie(), cpe.correlativo(), cpe.numeroCompleto(), xml, cpe.cadenaQr());
    }

    private ResultadoEmisionXml finalizar(String serie, Long correlativo, String numeroCompleto, String xml, String qr) {
        String codigoTipoCpe = serie.startsWith("F") ? "01" : "03";
        String codigoCdr = "";
        String descripcionCdr = "Configure el certificado PKCS12 para firmar y enviar a SUNAT Beta.";
        String estadoSunat = "PENDIENTE_FIRMA";
        String hashCpe = "";
        if (!config.getCertificadoRuta().isBlank() || xml.contains("SignatureValue")) {
            try {
                boolean xmlCorregido = false;
                if (xml.contains("<cbc:AddressLine>")) {
                    xml = xml.replace("<cbc:AddressLine>", "<cac:AddressLine>")
                             .replace("</cbc:AddressLine>", "</cac:AddressLine>");
                    xmlCorregido = true;
                }
                if (!xml.contains("SignatureValue") || xmlCorregido) {
                    xml = XmlSunat.firmar(xml, config);
                }
                hashCpe = XmlSunat.texto(XmlSunat.leer(xml.getBytes(StandardCharsets.UTF_8)), "DigestValue");
                estadoSunat = "PENDIENTE_ENVIO";
                byte[] zipBytes = empaquetarZip(config.getRucEmisor() + "-" + codigoTipoCpe + "-" + numeroCompleto + ".xml", xml);
                var cdr = enviarASunatBetaSoap(config.getRucEmisor(), codigoTipoCpe, numeroCompleto, zipBytes);
                codigoCdr = cdr.codigo();
                descripcionCdr = cdr.descripcion();
                estadoSunat = "0".equals(codigoCdr) ? "ACEPTADO" : "RECHAZADO";
            } catch (Exception ex) {
                descripcionCdr = "PENDIENTE_FIRMA".equals(estadoSunat)
                    ? "No se pudo firmar. Revise el certificado configurado."
                    : "No se obtuvo un CDR valido de SUNAT Beta. Reintente con el mismo comprobante.";
                LOG.warn("Emision {} en estado {} ({})", numeroCompleto, estadoSunat, ex.getClass().getSimpleName());
            }
        }
        String cadenaQr = qr.substring(0, qr.lastIndexOf('|') + 1) + hashCpe;
        return new ResultadoEmisionXml(
            serie, correlativo, numeroCompleto, xml, hashCpe, cadenaQr,
            codigoCdr, descripcionCdr, estadoSunat
        );
    }

    private String construirXmlUbl21(
        String tipoDoc, String serie, String correlativo, String fecha, String hora,
        String rucEmisor, String razonSocialEmisor, String nombreComercial, String direccionEmisor, String ubigeo,
        String codDocRec, String numDocRec, String nombreRec,
        BigDecimal gravado, BigDecimal igv, BigDecimal total, String codMedioPago, String metodoPago, String refOperacion
    ) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"\n" +
            "         xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"\n" +
            "         xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\"\n" +
            "         xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"\n" +
            "         xmlns:ext=\"urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2\">\n" +
            "    <ext:UBLExtensions>\n" +
            "        <ext:UBLExtension>\n" +
            "            <ext:ExtensionContent>\n" +
            "            </ext:ExtensionContent>\n" +
            "        </ext:UBLExtension>\n" +
            "    </ext:UBLExtensions>\n" +
            "    <cbc:UBLVersionID>2.1</cbc:UBLVersionID>\n" +
            "    <cbc:CustomizationID>2.0</cbc:CustomizationID>\n" +
            "    <cbc:ID>" + serie + "-" + correlativo + "</cbc:ID>\n" +
            "    <cbc:IssueDate>" + fecha + "</cbc:IssueDate>\n" +
            "    <cbc:IssueTime>" + hora + "</cbc:IssueTime>\n" +
            "    <cbc:InvoiceTypeCode listID=\"0101\">" + tipoDoc + "</cbc:InvoiceTypeCode>\n" +
            "    <cbc:DocumentCurrencyCode>PEN</cbc:DocumentCurrencyCode>\n" +
            "    <cac:Signature>\n" +
            "        <cbc:ID>" + XmlSunat.escapar(rucEmisor) + "</cbc:ID>\n" +
            "        <cac:SignatoryParty>\n" +
            "            <cac:PartyIdentification><cbc:ID>" + XmlSunat.escapar(rucEmisor) + "</cbc:ID></cac:PartyIdentification>\n" +
            "            <cac:PartyName><cbc:Name>" + XmlSunat.escapar(razonSocialEmisor) + "</cbc:Name></cac:PartyName>\n" +
            "        </cac:SignatoryParty>\n" +
            "    </cac:Signature>\n" +
            "    <cac:AccountingSupplierParty>\n" +
            "        <cac:Party>\n" +
            "            <cac:PartyIdentification><cbc:ID schemeID=\"6\">" + XmlSunat.escapar(rucEmisor) + "</cbc:ID></cac:PartyIdentification>\n" +
            "            <cac:PartyLegalEntity>\n" +
            "                <cbc:RegistrationName>" + XmlSunat.escapar(razonSocialEmisor) + "</cbc:RegistrationName>\n" +
            "                <cac:RegistrationAddress>\n" +
            "                    <cbc:ID>" + XmlSunat.escapar(ubigeo) + "</cbc:ID>\n" +
            "                    <cac:AddressLine><cbc:Line>" + XmlSunat.escapar(direccionEmisor) + "</cbc:Line></cac:AddressLine>\n" +
            "                </cac:RegistrationAddress>\n" +
            "            </cac:PartyLegalEntity>\n" +
            "        </cac:Party>\n" +
            "    </cac:AccountingSupplierParty>\n" +
            "    <cac:AccountingCustomerParty>\n" +
            "        <cac:Party>\n" +
            "            <cac:PartyIdentification><cbc:ID schemeID=\"" + XmlSunat.escapar(codDocRec) + "\">" + XmlSunat.escapar(numDocRec) + "</cbc:ID></cac:PartyIdentification>\n" +
            "            <cac:PartyLegalEntity><cbc:RegistrationName>" + XmlSunat.escapar(nombreRec) + "</cbc:RegistrationName></cac:PartyLegalEntity>\n" +
            "        </cac:Party>\n" +
            "    </cac:AccountingCustomerParty>\n" +
            "    <cac:PaymentMeans>\n" +
            "        <cbc:PaymentMeansCode>" + codMedioPago + "</cbc:PaymentMeansCode>\n" +
            "        <cbc:InstructionNote>" + metodoPago + (refOperacion != null ? " Ref: " + XmlSunat.escapar(refOperacion) : "") + "</cbc:InstructionNote>\n" +
            "    </cac:PaymentMeans>\n" +
            "    <cac:TaxTotal>\n" +
            "        <cbc:TaxAmount currencyID=\"PEN\">" + igv + "</cbc:TaxAmount>\n" +
            "        <cac:TaxSubtotal>\n" +
            "            <cbc:TaxableAmount currencyID=\"PEN\">" + gravado + "</cbc:TaxableAmount>\n" +
            "            <cbc:TaxAmount currencyID=\"PEN\">" + igv + "</cbc:TaxAmount>\n" +
            "            <cac:TaxCategory>\n" +
            "                <cac:TaxScheme>\n" +
            "                    <cbc:ID>1000</cbc:ID>\n" +
            "                    <cbc:Name>IGV</cbc:Name>\n" +
            "                    <cbc:TaxTypeCode>VAT</cbc:TaxTypeCode>\n" +
            "                </cac:TaxScheme>\n" +
            "            </cac:TaxCategory>\n" +
            "        </cac:TaxSubtotal>\n" +
            "    </cac:TaxTotal>\n" +
            "    <cac:LegalMonetaryTotal>\n" +
            "        <cbc:LineExtensionAmount currencyID=\"PEN\">" + gravado + "</cbc:LineExtensionAmount>\n" +
            "        <cbc:TaxInclusiveAmount currencyID=\"PEN\">" + total + "</cbc:TaxInclusiveAmount>\n" +
            "        <cbc:PayableAmount currencyID=\"PEN\">" + total + "</cbc:PayableAmount>\n" +
            "    </cac:LegalMonetaryTotal>\n" +
            "    <cac:InvoiceLine>\n" +
            "        <cbc:ID>1</cbc:ID><cbc:InvoicedQuantity unitCode=\"NIU\">1</cbc:InvoicedQuantity>\n" +
            "        <cbc:LineExtensionAmount currencyID=\"PEN\">" + gravado + "</cbc:LineExtensionAmount>\n" +
            "        <cac:PricingReference><cac:AlternativeConditionPrice><cbc:PriceAmount currencyID=\"PEN\">" + total + "</cbc:PriceAmount><cbc:PriceTypeCode>01</cbc:PriceTypeCode></cac:AlternativeConditionPrice></cac:PricingReference>\n" +
            "        <cac:TaxTotal><cbc:TaxAmount currencyID=\"PEN\">" + igv + "</cbc:TaxAmount><cac:TaxSubtotal><cbc:TaxableAmount currencyID=\"PEN\">" + gravado + "</cbc:TaxableAmount><cbc:TaxAmount currencyID=\"PEN\">" + igv + "</cbc:TaxAmount><cac:TaxCategory><cbc:Percent>18</cbc:Percent><cbc:TaxExemptionReasonCode>10</cbc:TaxExemptionReasonCode><cac:TaxScheme><cbc:ID>1000</cbc:ID><cbc:Name>IGV</cbc:Name><cbc:TaxTypeCode>VAT</cbc:TaxTypeCode></cac:TaxScheme></cac:TaxCategory></cac:TaxSubtotal></cac:TaxTotal>\n" +
            "        <cac:Item><cbc:Description>Consumo del pedido</cbc:Description></cac:Item>\n" +
            "        <cac:Price><cbc:PriceAmount currencyID=\"PEN\">" + gravado + "</cbc:PriceAmount></cac:Price>\n" +
            "    </cac:InvoiceLine>\n" +
            "</Invoice>";
    }

    private byte[] empaquetarZip(String nombreArchivo, String contenido) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(nombreArchivo));
            zos.write(contenido.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    private record Cdr(String codigo, String descripcion) {}

    private Cdr enviarASunatBetaSoap(String ruc, String tipoCpe, String serieNumero, byte[] zipBytes) throws Exception {
        String base64Zip = Base64.getEncoder().encodeToString(zipBytes);
        String nombreZip = ruc + "-" + tipoCpe + "-" + serieNumero + ".zip";

        String soapEnvelope =
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://service.sunat.gob.pe\">\n" +
            "   <soapenv:Header>\n" +
            "      <wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "         <wsse:UsernameToken>\n" +
            "            <wsse:Username>" + XmlSunat.escapar(config.getRucEmisor() + config.getUsuarioSol()) + "</wsse:Username>\n" +
            "            <wsse:Password>" + XmlSunat.escapar(config.getClaveSol()) + "</wsse:Password>\n" +
            "         </wsse:UsernameToken>\n" +
            "      </wsse:Security>\n" +
            "   </soapenv:Header>\n" +
            "   <soapenv:Body>\n" +
            "      <ser:sendBill>\n" +
            "         <fileName>" + nombreZip + "</fileName>\n" +
            "         <contentFile>" + base64Zip + "</contentFile>\n" +
            "      </ser:sendBill>\n" +
            "   </soapenv:Body>\n" +
            "</soapenv:Envelope>";

        HttpURLConnection conn = (HttpURLConnection) URI.create(config.getEndpointBeta()).toURL().openConnection();
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
        conn.setRequestProperty("SOAPAction", "urn:sendBill");
        conn.setDoOutput(true);

        try {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(soapEnvelope.getBytes(StandardCharsets.UTF_8));
            }
            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) throw new IOException("SUNAT HTTP " + status);
            byte[] respuesta;
            try (InputStream entrada = conn.getInputStream()) {
                respuesta = entrada.readNBytes(4_000_001);
            }
            if (respuesta.length > 4_000_000) throw new IOException("Respuesta SUNAT demasiado grande");
            var soap = XmlSunat.leer(respuesta);
            if (soap.getElementsByTagNameNS("*", "Fault").getLength() > 0) {
                throw new IOException("SUNAT devolvio SOAP Fault");
            }
            String applicationResponse = XmlSunat.texto(soap, "applicationResponse");
            if (applicationResponse.isBlank()) throw new IOException("Respuesta sin CDR");
            var cdr = XmlSunat.leerCdr(Base64.getMimeDecoder().decode(applicationResponse));
            String codigo = XmlSunat.texto(cdr, "ResponseCode");
            String descripcion = XmlSunat.texto(cdr, "Description");
            String documento = XmlSunat.texto(cdr, "ReferenceID");
            if (codigo.isBlank() || !serieNumero.equals(documento)) {
                throw new IOException("CDR incompleto o de otro comprobante");
            }
            return new Cdr(codigo, descripcion.substring(0, Math.min(descripcion.length(), 300)));
        } finally {
            conn.disconnect();
        }
    }
}
