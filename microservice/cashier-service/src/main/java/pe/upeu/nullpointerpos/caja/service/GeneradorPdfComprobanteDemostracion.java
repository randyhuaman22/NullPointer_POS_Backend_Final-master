package pe.upeu.nullpointerpos.caja.service;

import org.springframework.stereotype.Service;
import pe.upeu.nullpointerpos.caja.dto.response.RespuestaDatosComprobante;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-64 START] Genera un PDF simple local marcado como simulación académica. */
@Service
public class GeneradorPdfComprobanteDemostracion {
    public byte[] generar(String codigoPedido, BigDecimal total, RespuestaDatosComprobante datos) {
        List<String> lineas = new ArrayList<>();
        lineas.add("WENDY'S FOOD - NULLPOINTER POS");
        lineas.add("SIMULACION ACADEMICA - SIN VALOR TRIBUTARIO");
        lineas.add("Documento: " + datos.tipoComprobante());
        lineas.add("Referencia de pedido: " + codigoPedido);
        lineas.add("Modalidad de comprador: " + datos.modalidad());
        if (datos.nombreCompleto() != null) lineas.add("Cliente: " + datos.nombreCompleto());
        if (datos.tipoDocumento() != null && datos.numeroDocumento() != null)
            lineas.add(datos.tipoDocumento() + ": " + datos.numeroDocumento());
        if (datos.direccion() != null) lineas.add("Direccion: " + datos.direccion());
        if (datos.telefono() != null) lineas.add("Telefono: " + datos.telefono());
        if (datos.correo() != null) lineas.add("Correo: " + datos.correo());
        lineas.add("TOTAL: S/ " + total.setScale(2, java.math.RoundingMode.HALF_UP));
        lineas.add("Este archivo es una vista previa de pruebas, no es comprobante electronico valido.");
        return crearPdf(lineas);
    }

    private byte[] crearPdf(List<String> lineas) {
        StringBuilder contenido = new StringBuilder("BT\n/F1 12 Tf\n50 790 Td\n");
        for (int i = 0; i < lineas.size(); i++) {
            if (i > 0) contenido.append("0 -24 Td\n");
            contenido.append('(').append(escapar(limpiarAscii(lineas.get(i)))).append(") Tj\n");
        }
        contenido.append("ET\n");
        byte[] stream = contenido.toString().getBytes(StandardCharsets.US_ASCII);
        List<byte[]> objetos = List.of(
                ascii("<< /Type /Catalog /Pages 2 0 R >>"),
                ascii("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
                ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"),
                ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"),
                unir(ascii("<< /Length " + stream.length + " >>\nstream\n"), stream, ascii("endstream"))
        );
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        escribir(salida, ascii("%PDF-1.4\n%NPOS\n"));
        List<Integer> offsets = new ArrayList<>(); offsets.add(0);
        for (int i = 0; i < objetos.size(); i++) {
            offsets.add(salida.size()); escribir(salida, ascii((i + 1) + " 0 obj\n"));
            escribir(salida, objetos.get(i)); escribir(salida, ascii("\nendobj\n"));
        }
        int xref = salida.size(); escribir(salida, ascii("xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n"));
        for (int i = 1; i < offsets.size(); i++) escribir(salida, ascii(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offsets.get(i))));
        escribir(salida, ascii("trailer\n<< /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF"));
        return salida.toByteArray();
    }
    private String limpiarAscii(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.replaceAll("[^\\x20-\\x7E]", "?");
    }
    private String escapar(String s) { return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)"); }
    private byte[] ascii(String s) { return s.getBytes(StandardCharsets.US_ASCII); }
    private byte[] unir(byte[] a, byte[] b, byte[] c) { byte[] r = new byte[a.length+b.length+c.length]; System.arraycopy(a,0,r,0,a.length);System.arraycopy(b,0,r,a.length,b.length);System.arraycopy(c,0,r,a.length+b.length,c.length);return r; }
    private void escribir(ByteArrayOutputStream out, byte[] bytes) { out.write(bytes,0,bytes.length); }
}
/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-64 END] */
