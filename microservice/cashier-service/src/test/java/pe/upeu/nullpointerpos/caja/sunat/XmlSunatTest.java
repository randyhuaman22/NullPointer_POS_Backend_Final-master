package pe.upeu.nullpointerpos.caja.sunat;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.security.KeyStore;
import java.util.Base64;
import java.util.zip.*;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class XmlSunatTest {
    @TempDir static Path directorio;
    static Path certificado;

    @BeforeAll static void crearCertificadoDePrueba() throws Exception {
        certificado = directorio.resolve("prueba.p12");
        String ejecutable = Path.of(System.getProperty("java.home"), "bin", "keytool" +
            (System.getProperty("os.name").startsWith("Windows") ? ".exe" : "")).toString();
        var proceso = new ProcessBuilder(ejecutable, "-genkeypair", "-alias", "prueba", "-keyalg", "RSA",
            "-keysize", "2048", "-validity", "2", "-dname", "CN=Prueba local",
            "-keystore", certificado.toString(), "-storetype", "PKCS12", "-storepass", "prueba123").redirectErrorStream(true).start();
        proceso.getInputStream().readAllBytes();
        assertEquals(0, proceso.waitFor());
    }

    @Test void firmaEsVerificableYLosDatosEspecialesSonXmlValido() throws Exception {
        var config = configuracion();
        config.setEndpointBeta("http://127.0.0.1:1");
        var resultado = emitir(config);
        assertEquals("PENDIENTE_ENVIO", resultado.estadoSunat());
        var doc = XmlSunat.leer(resultado.xmlFirmado().getBytes(StandardCharsets.UTF_8));
        var almacen = KeyStore.getInstance("PKCS12");
        try (var entrada = java.nio.file.Files.newInputStream(certificado)) { almacen.load(entrada, "prueba123".toCharArray()); }
        var nodo = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature").item(0);
        var context = new DOMValidateContext(almacen.getCertificate("prueba").getPublicKey(), nodo);
        assertTrue(XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(context).validate(context));
        assertEquals("A & B <CLIENTE>", doc.getElementsByTagNameNS("*", "RegistrationName").item(1).getTextContent());
    }

    @Test void soloAceptaUnCdrExitosoDelMismoDocumento() throws Exception {
        assertEquals("ACEPTADO", responder("0", "B001-00000001", 200).estadoSunat());
        assertEquals("RECHAZADO", responder("2335", "B001-00000001", 200).estadoSunat());
        assertEquals("PENDIENTE_ENVIO", responder("0", "B001-00000999", 200).estadoSunat());
        assertEquals("PENDIENTE_ENVIO", responder("0", "B001-00000001", 500).estadoSunat());
    }

    @Test void xmlExternoNoPuedeLeerArchivosLocales() {
        assertThrows(Exception.class, () -> XmlSunat.leer("<!DOCTYPE x [<!ENTITY xxe SYSTEM 'file:///secret'>]><x>&xxe;</x>".getBytes(StandardCharsets.UTF_8)));
    }

    private ConfiguracionSunat configuracion() {
        var config = new ConfiguracionSunat();
        config.setCertificadoRuta(certificado.toString());
        config.setClaveCertificado("prueba123");
        return config;
    }

    private ServicioSunatBeta.ResultadoEmisionXml emitir(ConfiguracionSunat config) {
        return new ServicioSunatBeta(config).procesarEmision("BOLETA", 1L, "DNI", "12345678",
            "A & B <CLIENTE>", "Direccion", new BigDecimal("65.00"), "EFECTIVO", "1");
    }

    private ServicioSunatBeta.ResultadoEmisionXml responder(String codigo, String documento, int status) throws Exception {
        var zip = new ByteArrayOutputStream();
        try (var salida = new ZipOutputStream(zip)) {
            salida.putNextEntry(new ZipEntry("R-cdr.xml"));
            salida.write(("<ApplicationResponse><ResponseCode>" + codigo + "</ResponseCode><Description>Resultado</Description><ReferenceID>" + documento + "</ReferenceID></ApplicationResponse>").getBytes(StandardCharsets.UTF_8));
        }
        String soap = "<Envelope><applicationResponse>" + Base64.getEncoder().encodeToString(zip.toByteArray()) + "</applicationResponse></Envelope>";
        var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = soap.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var salida = exchange.getResponseBody()) { salida.write(bytes); }
        });
        servidor.start();
        try {
            var config = configuracion();
            config.setEndpointBeta("http://127.0.0.1:" + servidor.getAddress().getPort());
            return emitir(config);
        } finally { servidor.stop(0); }
    }
}
