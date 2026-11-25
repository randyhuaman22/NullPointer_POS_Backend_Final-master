package pe.upeu.nullpointerpos.caja;

import org.junit.jupiter.api.Test;
import pe.upeu.nullpointerpos.caja.dto.response.RespuestaDatosComprobante;
import pe.upeu.nullpointerpos.caja.service.GeneradorPdfComprobanteDemostracion;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-64 START] Prueba mínima de generación PDF local. */
class GeneradorPdfComprobanteDemostracionTest {
    @Test void generaPdfMarcadoComoSimulacionSinValorTributario() {
        var datos = new RespuestaDatosComprobante("BOLETA", "MANUAL", true, null, null,
                "CLIENTE DE PRUEBA", null, null, null, "INGRESO_MANUAL", "prueba");
        byte[] archivo = new GeneradorPdfComprobanteDemostracion().generar("PED-0001", new BigDecimal("15.50"), datos);
        String contenido = new String(archivo, StandardCharsets.US_ASCII);
        assertTrue(contenido.startsWith("%PDF-1.4"));
        assertTrue(contenido.contains("SIN VALOR TRIBUTARIO"));
        assertTrue(contenido.contains("S/ 15.50"));
    }
}
/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-64 END] */
