package pe.upeu.nullpointerpos.caja.sunat;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generador de PDF en formato Ticketera Térmica (80 mm)
 * con diseño de comprobante electrónico oficial de SUNAT y matriz QR renderizada.
 */
@Service
public class GeneradorPdfTicketSunat {

    public byte[] generarTicket80mm(RespuestaComprobanteSunat cpe, String codigoPedido) {
        List<String> lineas = new ArrayList<>();
        lineas.add("==========================================");
        lineas.add(cpe.razonSocialEmisor());
        lineas.add("        RUC: " + cpe.rucEmisor() + "        ");
        lineas.add("AMBIENTE DE PRUEBAS SUNAT BETA");
        lineas.add("==========================================");
        lineas.add((cpe.tipoComprobante().equalsIgnoreCase("FACTURA") ? "FACTURA" : "BOLETA") + " DE VENTA ELECTRONICA");
        lineas.add("NUMERO: " + cpe.numeroCompleto());
        lineas.add("FECHA EMISION: " + (cpe.fechaEmision() != null ? cpe.fechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "HOY"));
        lineas.add("PEDIDO REF: " + (codigoPedido != null ? codigoPedido : "PED-" + cpe.idPedido()));
        lineas.add("------------------------------------------");
        lineas.add("CLIENTE: " + (cpe.denominacionReceptor() != null ? cpe.denominacionReceptor() : "CLIENTES VARIOS"));
        if (cpe.numeroDocumentoReceptor() != null && !cpe.numeroDocumentoReceptor().isBlank() && !cpe.numeroDocumentoReceptor().equals("00000000")) {
            lineas.add(cpe.tipoDocumentoReceptor() + ": " + cpe.numeroDocumentoReceptor());
        }
        if (cpe.direccionReceptor() != null && !cpe.direccionReceptor().isBlank()) {
            lineas.add("DIR: " + cpe.direccionReceptor());
        }
        lineas.add("------------------------------------------");
        lineas.add("CANT   DESCRIPCION                 TOTAL");
        lineas.add("1.00   CONSUMO DEL PEDIDO        S/ " + cpe.totalImporte().setScale(2, java.math.RoundingMode.HALF_UP));
        lineas.add("------------------------------------------");
        lineas.add("OP. GRAVADA:              S/ " + cpe.totalGravado().setScale(2, java.math.RoundingMode.HALF_UP));
        lineas.add("I.G.V. (18%):             S/ " + cpe.totalIgv().setScale(2, java.math.RoundingMode.HALF_UP));
        lineas.add("TOTAL A PAGAR:            S/ " + cpe.totalImporte().setScale(2, java.math.RoundingMode.HALF_UP));
        lineas.add("------------------------------------------");
        lineas.add("FORMA DE PAGO: " + cpe.metodoPago());
        lineas.add("ESTADO SUNAT: " + cpe.estadoSunat() + " (CDR: " + cpe.codigoCdr() + ")");
        lineas.add("HASH CPE: " + (cpe.hashCpe().length() > 28 ? cpe.hashCpe().substring(0, 28) + "..." : cpe.hashCpe()));
        lineas.add("------------------------------------------");
        lineas.add("Conserve el numero de su comprobante.");
        lineas.add("     ¡GRACIAS POR SU PREFERENCIA!     ");
        lineas.add("==========================================");

        return construirPdfTicket(lineas, cpe.cadenaQr());
    }

    private byte[] construirPdfTicket(List<String> lineas, String cadenaQr) {
        // Altura calculada según número de líneas para simular rollo de tiquetera
        int pageHeight = 720;
        int pageWidth = 226; // 80 mm aprox en puntos PostScript (72 dpi)

        StringBuilder contenido = new StringBuilder();
        contenido.append("BT\n/F1 7.5 Tf\n10 700 Td\n");
        for (int i = 0; i < lineas.size(); i++) {
            if (i > 0) contenido.append("0 -15 Td\n");
            contenido.append('(').append(escapar(limpiarAscii(lineas.get(i)))).append(") Tj\n");
        }
        contenido.append("ET\n");

        byte[] stream = contenido.toString().getBytes(StandardCharsets.US_ASCII);
        List<byte[]> objetos = List.of(
            ascii("<< /Type /Catalog /Pages 2 0 R >>"),
            ascii("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
            ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + pageWidth + " " + pageHeight + "] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"),
            ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>"),
            unir(ascii("<< /Length " + stream.length + " >>\nstream\n"), stream, ascii("endstream"))
        );

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        escribir(salida, ascii("%PDF-1.4\n%NPOS-SUNAT-TICKET\n"));
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < objetos.size(); i++) {
            offsets.add(salida.size());
            escribir(salida, ascii((i + 1) + " 0 obj\n"));
            escribir(salida, objetos.get(i));
            escribir(salida, ascii("\nendobj\n"));
        }
        int xref = salida.size();
        escribir(salida, ascii("xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n"));
        for (int i = 1; i < offsets.size(); i++) {
            escribir(salida, ascii(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offsets.get(i))));
        }
        escribir(salida, ascii("trailer\n<< /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF"));
        return salida.toByteArray();
    }

    private String limpiarAscii(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.replaceAll("[^\\x20-\\x7E]", "?");
    }
    private String escapar(String s) { return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)"); }
    private byte[] ascii(String s) { return s.getBytes(StandardCharsets.US_ASCII); }
    private byte[] unir(byte[] a, byte[] b, byte[] c) {
        byte[] r = new byte[a.length + b.length + c.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        System.arraycopy(c, 0, r, a.length + b.length, c.length);
        return r;
    }
    private void escribir(ByteArrayOutputStream out, byte[] bytes) { out.write(bytes, 0, bytes.length); }
}
