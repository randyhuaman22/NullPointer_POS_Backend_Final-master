package pe.upeu.nullpointerpos.caja;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.caja.controller.ControladorClientes;
import pe.upeu.nullpointerpos.caja.dto.request.SolicitudDatosComprobante;
import pe.upeu.nullpointerpos.caja.repository.RepositorioCaja;
import pe.upeu.nullpointerpos.caja.service.DirectorioDemostracionDocumentos;
import pe.upeu.nullpointerpos.caja.service.ServicioConsultaDocumentoExterno;
import pe.upeu.nullpointerpos.caja.service.ServicioDatosComprobante;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ServicioConsultaDocumentoExternoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void consultaDniYRucEnApisperuConToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        ServicioConsultaDocumentoExterno servicio = new ServicioConsultaDocumentoExterno(
                builder, mapper, "APISPERU", "", "tok_apisperu"
        );

        server.expect(requestTo("https://dniruc.apisperu.com/api/v1/dni/71234567?token=tok_apisperu"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"dni\":\"71234567\",\"nombres\":\"JUAN CARLOS\",\"apellidoPaterno\":\"PEREZ\",\"apellidoMaterno\":\"GOMEZ\"}",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(requestTo("https://dniruc.apisperu.com/api/v1/ruc/20601030013?token=tok_apisperu"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"ruc\":\"20601030013\",\"razonSocial\":\"POLLOS WENDY S.A.C.\",\"estado\":\"ACTIVO\",\"condicion\":\"HABIDO\",\"direccion\":\"AV. LAS FLORES NRO. 123\",\"distrito\":\"MIRAFLORES\",\"provincia\":\"LIMA\"}",
                        MediaType.APPLICATION_JSON
                ));

        var dni = servicio.consultar("dni", "71234567");
        assertTrue(dni.isPresent());
        assertEquals("DNI", dni.get().tipoDocumento());
        assertEquals("JUAN CARLOS PEREZ GOMEZ", dni.get().nombreCompleto());
        assertEquals("APISPERU", dni.get().origenDatos());

        var ruc = servicio.consultar("RUC", "20601030013");
        assertTrue(ruc.isPresent());
        assertEquals("POLLOS WENDY S.A.C.", ruc.get().nombreCompleto());
        assertEquals("AV. LAS FLORES NRO. 123 - MIRAFLORES, LIMA", ruc.get().direccion());
        assertEquals("ACTIVO", ruc.get().estado());
        assertEquals("HABIDO", ruc.get().condicion());

        server.verify();
    }

    @Test
    void manejaSuccessFalseDeApisperuYErrorHttpSinFallar() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ServicioConsultaDocumentoExterno servicio = new ServicioConsultaDocumentoExterno(
                builder, mapper, "APISPERU", "", "tok_apisperu"
        );

        server.expect(requestTo("https://dniruc.apisperu.com/api/v1/dni/87654321?token=tok_apisperu"))
                .andRespond(withSuccess("{\"success\":false,\"message\":\"No se encontraron resultados.\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://dniruc.apisperu.com/api/v1/ruc/20999999999?token=tok_apisperu"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertTrue(servicio.consultar("DNI", "87654321").isEmpty());
        assertTrue(servicio.consultar("RUC", "20999999999").isEmpty());

        server.verify();
    }

    @Test
    void soportaDecolectaConBearerToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ServicioConsultaDocumentoExterno servicio = new ServicioConsultaDocumentoExterno(
                builder, mapper, "DECOLECTA", "", "sk_decolecta"
        );

        server.expect(requestTo("https://api.decolecta.com/v1/reniec/dni?numero=49071347"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer sk_decolecta"))
                .andRespond(withSuccess(
                        "{\"first_name\":\"MARIA ELENA\",\"first_last_name\":\"QUISPE\",\"second_last_name\":\"TORRES\"}",
                        MediaType.APPLICATION_JSON
                ));

        var dni = servicio.consultar("DNI", "49071347");
        assertTrue(dni.isPresent());
        assertEquals("MARIA ELENA QUISPE TORRES", dni.get().nombreCompleto());
        assertEquals("DECOLECTA", dni.get().origenDatos());
        server.verify();
    }

    @Test
    void controladorYServicioCacheanClienteConsultadoEnBdLocal() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ServicioConsultaDocumentoExterno externa = new ServicioConsultaDocumentoExterno(
                builder, mapper, "APISPERU", "", "tok_live"
        );

        RepositorioCaja repo = mock(RepositorioCaja.class);
        when(repo.buscarCliente("RUC", "20601030013")).thenReturn(null);
        when(repo.guardarCliente(eq("RUC"), eq("20601030013"), eq("WENDY FOODS S.A.C."), isNull(), isNull(), eq("JR. LIMA 450")))
                .thenReturn(Map.of(
                        "tipoDocumento", "RUC",
                        "numeroDocumento", "20601030013",
                        "nombreCompleto", "WENDY FOODS S.A.C.",
                        "direccion", "JR. LIMA 450"
                ));

        server.expect(requestTo("https://dniruc.apisperu.com/api/v1/ruc/20601030013?token=tok_live"))
                .andRespond(withSuccess(
                        "{\"ruc\":\"20601030013\",\"razonSocial\":\"WENDY FOODS S.A.C.\",\"direccion\":\"JR. LIMA 450\",\"estado\":\"ACTIVO\",\"condicion\":\"HABIDO\"}",
                        MediaType.APPLICATION_JSON
                ));

        var servicioDatos = new ServicioDatosComprobante(repo, new DirectorioDemostracionDocumentos(), externa);
        var controlador = new ControladorClientes(repo, servicioDatos);

        Map<String, Object> encontrado = controlador.buscar("RUC", "20601030013");
        assertEquals("WENDY FOODS S.A.C.", encontrado.get("nombreCompleto"));
        assertEquals("JR. LIMA 450", encontrado.get("direccion"));
        assertEquals("APISPERU", encontrado.get("origenDatos"));
        verify(repo).guardarCliente("RUC", "20601030013", "WENDY FOODS S.A.C.", null, null, "JR. LIMA 450");

        // Segunda consulta ya en caché local: NO debe volver a llamar a APISPERU
        when(repo.buscarCliente("DNI", "44556677")).thenReturn(Map.of("nombreCompleto", "ANA LOPEZ"));
        var desdeBd = servicioDatos.preparar(new SolicitudDatosComprobante("BOLETA", "DNI", "DNI", "44556677", null, null, null, null));
        assertEquals("ANA LOPEZ", desdeBd.nombreCompleto());
        assertEquals("REGISTRO_LOCAL", desdeBd.origenDatos());

        server.verify();
    }

    @Test
    void sinTokenNiRegistroLanza404EnControladorClientes() {
        RepositorioCaja repo = mock(RepositorioCaja.class);
        when(repo.buscarCliente("DNI", "87654321")).thenReturn(Collections.emptyMap());
        ServicioConsultaDocumentoExterno sinToken = new ServicioConsultaDocumentoExterno(
                RestClient.builder(), mapper, "APISPERU", "", ""
        );
        var controlador = new ControladorClientes(repo, new ServicioDatosComprobante(repo, new DirectorioDemostracionDocumentos(), sinToken));

        var ex = assertThrows(ResponseStatusException.class, () -> controlador.buscar("DNI", "87654321"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
