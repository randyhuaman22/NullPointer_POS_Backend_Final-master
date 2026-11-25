package pe.upeu.nullpointerpos.caja;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pe.upeu.nullpointerpos.caja.client.ClientePedidos;
import pe.upeu.nullpointerpos.caja.controller.ControladorTicketWhatsApp;
import pe.upeu.nullpointerpos.caja.dto.request.SolicitudTicketWhatsApp;
import pe.upeu.nullpointerpos.caja.repository.RepositorioCaja;
import pe.upeu.nullpointerpos.caja.service.GeneradorPdfComprobanteDemostracion;
import pe.upeu.nullpointerpos.caja.service.ServicioWhatsApp;
import pe.upeu.nullpointerpos.caja.sunat.GeneradorPdfTicketSunat;
import pe.upeu.nullpointerpos.caja.sunat.RespuestaComprobanteSunat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ServicioWhatsAppGreenApiTest {

    @Test
    void normalizaTelefonoPeruYHostMediaGreenApi() {
        RestClient.Builder builder = RestClient.builder();
        ServicioWhatsApp servicio = new ServicioWhatsApp(
                builder,
                "https://7103.api.greenapi.com/",
                "7103123456",
                "tok_secret",
                "",
                ""
        );

        assertTrue(servicio.estaConfigurado());
        assertEquals("51987654321@c.us", servicio.formatearChatId("987654321"));
        assertEquals("51987654321@c.us", servicio.formatearChatId("+51 987-654-321"));
    }

    @Test
    void subePdfMultipartHaciaGreenApiSendFileByUpload() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        ServicioWhatsApp servicio = new ServicioWhatsApp(
                builder,
                "https://7107.api.greenapi.com",
                "710722814112",
                "tok_123",
                "",
                ""
        );

        server.expect(requestTo("https://7107.api.greenapi.com/waInstance710722814112/sendFileByUpload/tok_123"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(content().string(containsString("51987654321@c.us")))
                .andExpect(content().string(containsString("B001-00000001.pdf")))
                .andExpect(content().string(containsString("%PDF-1.4")))
                .andRespond(withSuccess("{\"idMessage\":\"3EB0ABC123\"}", MediaType.APPLICATION_JSON));

        byte[] pdf = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        String resp = servicio.enviarPdf("987654321", pdf, "B001-00000001.pdf", "Wendy's Food - Boleta");

        assertTrue(resp.contains("3EB0ABC123"));
        server.verify();
    }

    @Test
    void controladorAdjuntaPdfSunatRealCuandoComprobanteExiste() {
        RepositorioCaja repo = mock(RepositorioCaja.class);
        ClientePedidos pedidos = mock(ClientePedidos.class);
        ServicioWhatsApp whatsapp = mock(ServicioWhatsApp.class);
        GeneradorPdfTicketSunat genSunat = new GeneradorPdfTicketSunat();
        GeneradorPdfComprobanteDemostracion genDemo = new GeneradorPdfComprobanteDemostracion();
        ControladorTicketWhatsApp ctrl = new ControladorTicketWhatsApp(repo, pedidos, whatsapp, genSunat, genDemo);

        var resumen = new ClientePedidos.ResumenPedido(9L, "NP-009", "PAGADO", new BigDecimal("49.80"), BigDecimal.ZERO);
        var cpe = new RespuestaComprobanteSunat(
                5L, 9L, 4L, "BOLETA", "B001", 1L, "B001-00000001",
                OffsetDateTime.now(), "20609123456", "WENDY'S FOOD",
                "DNI", "12345678", "CLIENTE DE PRUEBA", null,
                new BigDecimal("42.20"), new BigDecimal("7.60"), new BigDecimal("49.80"),
                "PEN", "EFECTIVO", "hash123456", "0", "Aceptado", "ACEPTADO",
                "QRDATA", "/api/v1/comprobantes/5/pdf", "/api/v1/comprobantes/5/xml"
        );

        when(pedidos.buscarResumen(9L)).thenReturn(resumen);
        when(repo.buscarComprobantePorPedido(9L)).thenReturn(cpe);
        when(repo.guardarTicket(eq(9L), eq("987654321"), anyString())).thenReturn(77L);
        when(whatsapp.estaConfigurado()).thenReturn(true);
        when(whatsapp.enviarPdf(eq("987654321"), any(byte[].class), eq("B001-00000001.pdf"), anyString()))
                .thenReturn("{\"idMessage\":\"MSG-OK\"}");

        Map<String, Object> resultado = ctrl.whatsapp(new SolicitudTicketWhatsApp(9L, "987654321"), mock(Authentication.class));

        assertEquals("ENVIADO", resultado.get("estado"));
        assertEquals("B001-00000001.pdf", resultado.get("archivoPdf"));

        ArgumentCaptor<byte[]> pdfCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(whatsapp).enviarPdf(eq("987654321"), pdfCaptor.capture(), eq("B001-00000001.pdf"), contains("B001-00000001"));
        String pdfTexto = new String(pdfCaptor.getValue(), StandardCharsets.US_ASCII);
        assertTrue(pdfTexto.startsWith("%PDF-1.4"));
        assertTrue(pdfTexto.contains("B001-00000001"));
        verify(repo).actualizarEstadoTicket(eq(77L), eq("ENVIADO"), contains("MSG-OK"));
    }

    @Test
    void controladorInformaPendienteConfiguracionCuandoFaltanCredencialesGreenApi() {
        RepositorioCaja repo = mock(RepositorioCaja.class);
        ClientePedidos pedidos = mock(ClientePedidos.class);
        ServicioWhatsApp whatsapp = new ServicioWhatsApp(RestClient.builder(), "https://media.green-api.com", "", "", "", "");
        ControladorTicketWhatsApp ctrl = new ControladorTicketWhatsApp(
                repo, pedidos, whatsapp, new GeneradorPdfTicketSunat(), new GeneradorPdfComprobanteDemostracion());

        when(pedidos.buscarResumen(10L)).thenReturn(
                new ClientePedidos.ResumenPedido(10L, "NP-010", "PAGADO", new BigDecimal("25.00"), BigDecimal.ZERO));
        when(repo.buscarComprobantePorPedido(10L)).thenReturn(null);
        when(repo.guardarTicket(eq(10L), eq("987654321"), anyString())).thenReturn(88L);

        Map<String, Object> resultado = ctrl.whatsapp(new SolicitudTicketWhatsApp(10L, "987654321"), mock(Authentication.class));

        assertEquals("PENDIENTE_CONFIGURACION", resultado.get("estado"));
        verify(repo).actualizarEstadoTicket(eq(88L), eq("PENDIENTE_CONFIGURACION"), contains("GREENAPI_ID_INSTANCE"));
    }
}
