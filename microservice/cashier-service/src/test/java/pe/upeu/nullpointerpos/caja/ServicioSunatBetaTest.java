package pe.upeu.nullpointerpos.caja;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.upeu.nullpointerpos.caja.sunat.ConfiguracionSunat;
import pe.upeu.nullpointerpos.caja.sunat.GeneradorPdfTicketSunat;
import pe.upeu.nullpointerpos.caja.sunat.RespuestaComprobanteSunat;
import pe.upeu.nullpointerpos.caja.sunat.ServicioSunatBeta;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ServicioSunatBetaTest {

    private ServicioSunatBeta servicioSunat;
    private GeneradorPdfTicketSunat generadorTicket;

    @BeforeEach
    void setUp() {
        ConfiguracionSunat config = new ConfiguracionSunat();
        config.setRucEmisor("20000000001");
        config.setRazonSocial("POLLERIA Y PARRILLAS WENDY'S FOOD S.A.C.");
        servicioSunat = new ServicioSunatBeta(config);
        generadorTicket = new GeneradorPdfTicketSunat();
    }

    @Test
    void emisionBoletaBetaDebeCalcularIgvYCadenaQrOficial() {
        // Pedido de S/ 65.00 con pago por Tarjeta
        BigDecimal total = new BigDecimal("65.00");
        ServicioSunatBeta.ResultadoEmisionXml res = servicioSunat.procesarEmision(
                "BOLETA", 1L, "DNI", "12345678", "JUAN PEREZ",
                "AV. REVOLUCION 123", total, "TARJETA", "AUTH-992144"
        );

        assertEquals("B001", res.serie());
        assertEquals(1L, res.correlativo());
        assertEquals("B001-00000001", res.numeroCompleto());
        assertEquals("", res.codigoCdr());
        assertEquals("PENDIENTE_FIRMA", res.estadoSunat());
        assertFalse(res.xmlFirmado().contains("ds:Signature"));

        // Debe contener el XML UBL 2.1 con Invoice y RUC emisor
        assertTrue(res.xmlFirmado().contains("<Invoice"));
        assertTrue(res.xmlFirmado().contains("20000000001"));
        assertTrue(res.xmlFirmado().contains("JUAN PEREZ"));

        // Cadena QR oficial: RUC|TIPO|SERIE|CORRELATIVO|IGV|TOTAL|FECHA|DOC|NUM|HASH
        assertTrue(res.cadenaQr().startsWith("20000000001|03|B001|00000001|9.92|65.00|"));
        assertNotNull(res.hashCpe());
    }

    @Test
    void generadorPdfTicketDebeGenerarBytesPdfValidos() {
        RespuestaComprobanteSunat cpe = new RespuestaComprobanteSunat(
                1L, 10L, 5L, "BOLETA", "B001", 1L, "B001-00000001",
                OffsetDateTime.now(), "20000000001", "POLLERIA WENDY'S FOOD",
                "DNI", "12345678", "JUAN PEREZ", "AV. LIMA 123",
                new BigDecimal("55.08"), new BigDecimal("9.92"), new BigDecimal("65.00"),
                "PEN", "TARJETA", "hashCpePrueba==", "0", "Aceptada", "ACEPTADO",
                "20000000001|03|B001|00000001|9.92|65.00|2026-11-25|1|12345678|hashCpePrueba==",
                "/api/v1/comprobantes/1/pdf", "/api/v1/comprobantes/1/xml"
        );

        byte[] pdf = generadorTicket.generarTicket80mm(cpe, "PED-10");
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);

        // Cabecera estándar PDF
        String header = new String(pdf, 0, 5);
        assertEquals("%PDF-", header);
    }
}
