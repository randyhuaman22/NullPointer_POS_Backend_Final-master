package pe.upeu.nullpointerpos.caja;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.caja.client.ClientePedidos;
import pe.upeu.nullpointerpos.caja.dto.request.SolicitudCobroDigital;
import pe.upeu.nullpointerpos.caja.dto.request.SolicitudCobroEfectivo;
import pe.upeu.nullpointerpos.caja.dto.response.RespuestaPago;
import pe.upeu.nullpointerpos.caja.repository.RepositorioCaja;
import pe.upeu.nullpointerpos.caja.service.impl.ServicioCajaImpl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CajaIdempotenciaOfflineTest {

    private RepositorioCaja repo;
    private ClientePedidos pedidos;
    private ServicioCajaImpl servicio;

    @BeforeEach
    void preparar() {
        repo = mock(RepositorioCaja.class);
        pedidos = mock(ClientePedidos.class);
        servicio = new ServicioCajaImpl(repo, pedidos, new ObjectMapper());
    }

    @Test
    void reintentoDeCobroConMismaIdempotenciaYPedidoDevuelvePagoExistente() {
        var pagoEfectivoPrevio = new RespuestaPago(17L, 9L, 1L, "EFECTIVO",
                new BigDecimal("49.80"), new BigDecimal("50.00"), new BigDecimal("0.20"), "CONFIRMADO", OffsetDateTime.now());
        when(repo.buscarPagoPorIdempotencia("idem-ef-9")).thenReturn(pagoEfectivoPrevio);

        var resEf = servicio.cobrarEfectivo(5L, "CAJ001", new SolicitudCobroEfectivo(9L, new BigDecimal("50.00"), "idem-ef-9"));
        assertEquals(17L, resEf.idPago());
        verify(repo, never()).insertarPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        var pagoYapePrevio = new RespuestaPago(22L, 11L, 1L, "YAPE",
                new BigDecimal("35.00"), new BigDecimal("35.00"), BigDecimal.ZERO, "CONFIRMADO", OffsetDateTime.now());
        when(repo.buscarPagoPorIdempotencia("idem-yape-11")).thenReturn(pagoYapePrevio);
        when(repo.existeNumeroOperacion("OP-9988")).thenReturn(true);

        var resYape = servicio.cobrarDigital(5L, "CAJ001", new SolicitudCobroDigital(11L, "YAPE", "OP-9988", "1234", "idem-yape-11"));
        assertEquals(22L, resYape.idPago());
        verify(repo, never()).existeNumeroOperacion("OP-9988");
    }

    @Test
    void rechazaIdempotenciaSiSeUsaEnOtroPedido() {
        var otroPago = new RespuestaPago(17L, 99L, 1L, "EFECTIVO",
                new BigDecimal("20.00"), new BigDecimal("20.00"), BigDecimal.ZERO, "CONFIRMADO", OffsetDateTime.now());
        when(repo.buscarPagoPorIdempotencia("idem-duplicada")).thenReturn(otroPago);

        var ex = assertThrows(ResponseStatusException.class,
                () -> servicio.cobrarEfectivo(5L, "CAJ001", new SolicitudCobroEfectivo(9L, new BigDecimal("20.00"), "idem-duplicada")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}
