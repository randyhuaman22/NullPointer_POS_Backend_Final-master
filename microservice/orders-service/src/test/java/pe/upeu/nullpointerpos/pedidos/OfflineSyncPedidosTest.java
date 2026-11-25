package pe.upeu.nullpointerpos.pedidos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.pedidos.client.ClienteProductos;
import pe.upeu.nullpointerpos.pedidos.dto.external.RespuestaOpcionesValidadas;
import pe.upeu.nullpointerpos.pedidos.dto.external.RespuestaProductoCatalogo;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudDetallePedido;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudSincronizarPedidoOffline;
import pe.upeu.nullpointerpos.pedidos.entity.DetallePedido;
import pe.upeu.nullpointerpos.pedidos.entity.Mesa;
import pe.upeu.nullpointerpos.pedidos.entity.Pedido;
import pe.upeu.nullpointerpos.pedidos.mapper.PedidoMapper;
import pe.upeu.nullpointerpos.pedidos.repository.MesaRepositorio;
import pe.upeu.nullpointerpos.pedidos.repository.PedidoRepositorio;
import pe.upeu.nullpointerpos.pedidos.service.impl.ServicioPedidosImpl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OfflineSyncPedidosTest {

    private PedidoRepositorio pedidosRepo;
    private MesaRepositorio mesasRepo;
    private ClienteProductos productosClient;
    private ServicioPedidosImpl servicio;

    @BeforeEach
    void preparar() {
        pedidosRepo = mock(PedidoRepositorio.class);
        mesasRepo = mock(MesaRepositorio.class);
        productosClient = mock(ClienteProductos.class);
        servicio = new ServicioPedidosImpl(pedidosRepo, mesasRepo, productosClient, new PedidoMapper(), new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void sincronizarOfflineCreaYEnviaIdempotentementeSinDuplicarAlReintentar() {
        OffsetDateTime horaOffline = OffsetDateTime.parse("2026-11-25T15:40:00-05:00");
        var sol = new SolicitudSincronizarPedidoOffline(
                1L,
                "Sin ají",
                List.of(new SolicitudDetallePedido(10L, 2, "Bien dorado", List.of())),
                "offline-mozo-001",
                true,
                horaOffline
        );

        when(pedidosRepo.buscarIdPorIdempotencia("offline-mozo-001")).thenReturn(Optional.empty(), Optional.of(42L));
        when(mesasRepo.bloquearPorId(1L)).thenReturn(Optional.of(new Mesa(1L, "M01", "SALON", 4, "LIBRE", true)));
        when(productosClient.buscarDisponible(10L)).thenReturn(new RespuestaProductoCatalogo(
                10L, "P-010", "1/4 Pollo a la Brasa", null, null, new BigDecimal("22.00"), "Pollos", "PARRILLA", true
        ));
        when(productosClient.validarOpciones(eq(10L), anyList())).thenReturn(new RespuestaOpcionesValidadas(BigDecimal.ZERO, List.of()));
        when(pedidosRepo.insertarPedido(anyString(), eq(1L), eq(2L), eq("MOZ001"), eq("Sin ají"), eq("offline-mozo-001"), eq(horaOffline)))
                .thenReturn(42L);
        when(pedidosRepo.calcularSubtotal(42L)).thenReturn(new BigDecimal("44.00"));

        var enviado = new Pedido(42L, "NP-42", 1L, "M01", 2L, "MOZ001", "ENVIADO_COCINA", "Sin ají",
                new BigDecimal("44.00"), BigDecimal.ZERO, new BigDecimal("44.00"), horaOffline, OffsetDateTime.now(), null);
        when(pedidosRepo.buscarEncabezado(42L)).thenReturn(Optional.of(enviado));
        when(pedidosRepo.listarDetalles(42L)).thenReturn(List.of(
                new DetallePedido(100L, 10L, "1/4 Pollo a la Brasa", 2, new BigDecimal("22.00"), "Notas: Bien dorado",
                        new BigDecimal("44.00"), "PENDIENTE", "PARRILLA", null)
        ));
        when(pedidosRepo.buscarBloqueando(42L)).thenReturn(Optional.of(enviado));

        var creado = servicio.sincronizarOffline(sol, "MOZ001", 2L, false);
        assertEquals(42L, creado.idPedido());
        assertEquals("ENVIADO_COCINA", creado.estado());
        verify(pedidosRepo).cambiarEstadoAEnviado(42L);
        verify(pedidosRepo).guardarEventoPendiente(eq(42L), anyString());

        // Reintento tras reconexión: devuelve pedido existente sin insertar otro ni exigir mesa libre
        var reintento = servicio.sincronizarOffline(sol, "MOZ001", 2L, false);
        assertEquals(42L, reintento.idPedido());
        verify(pedidosRepo, times(1)).insertarPedido(anyString(), anyLong(), anyLong(), anyString(), any(), any(), any());
    }

    @Test
    void sincronizarOfflineInformaConflictoCuandoMesaFueOcupadaPorOtroMozo() {
        var sol = new SolicitudSincronizarPedidoOffline(
                3L, null, List.of(new SolicitudDetallePedido(10L, 1, null, List.of())),
                "offline-mozo-002", true, OffsetDateTime.now()
        );
        when(pedidosRepo.buscarIdPorIdempotencia("offline-mozo-002")).thenReturn(Optional.empty());
        when(mesasRepo.bloquearPorId(3L)).thenReturn(Optional.of(new Mesa(3L, "M03", "SALON", 4, "OCUPADA", true)));
        when(mesasRepo.perteneceAGrupoActivoNoPrincipal(3L)).thenReturn(true);
        when(pedidosRepo.buscarResumenActivoPorMesa(3L)).thenReturn(Optional.of(
                new Pedido(15L, null, 3L, null, null, "MOZ002", "EN_PREPARACION", null, null, null, new BigDecimal("25.00"), null, null, null)
        ));

        var ex = assertThrows(ResponseStatusException.class,
                () -> servicio.sincronizarOffline(sol, "MOZ001", 2L, false));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("MOZ002"));
        assertTrue(ex.getReason().contains("#15"));
    }

    @Test
    void enviarACocinaYMarcarListoSoportanReintentoIdempotente() {
        var enPrep = new Pedido(8L, "NP-8", 1L, "M01", 2L, "MOZ001", "EN_PREPARACION", null,
                new BigDecimal("30.00"), BigDecimal.ZERO, new BigDecimal("30.00"), OffsetDateTime.now(), OffsetDateTime.now(), null);
        when(pedidosRepo.buscarBloqueando(8L)).thenReturn(Optional.of(enPrep));
        when(pedidosRepo.buscarEncabezado(8L)).thenReturn(Optional.of(enPrep));
        when(pedidosRepo.listarDetalles(8L)).thenReturn(List.of(
                new DetallePedido(50L, 1L, "Pollo", 1, new BigDecimal("30.00"), null, new BigDecimal("30.00"), "ENTREGADO", "PARRILLA", null)
        ));

        // Reintento de enviarACocina cuando cocina ya avanzó a EN_PREPARACION no falla
        var respEnvio = servicio.enviarACocina(8L, "MOZ001", 2L, false);
        assertEquals("EN_PREPARACION", respEnvio.estado());
        verify(pedidosRepo, never()).cambiarEstadoAEnviado(8L);

        // Detalle ya ENTREGADO no falla al reintentar marcarDetalleListo desde cocina offline
        when(pedidosRepo.buscarIdPedidoPorDetalle(50L)).thenReturn(8L);
        when(pedidosRepo.marcarDetalleListo(50L)).thenReturn(0);
        var respPlato = servicio.marcarDetalleListo(50L, "COC001", 3L);
        assertEquals(8L, respPlato.idPedido());

        // Marcar pedido completo listo desde cocina publica evento la 1ra vez y es idempotente la 2da
        when(pedidosRepo.marcarTodosDetallesListos(8L)).thenReturn(1, 0);
        servicio.marcarPedidoListoCocina(8L, "COC001", 3L);
        verify(pedidosRepo).guardarEventoPendiente(eq(8L), eq("DETALLE_PLATO_ACTUALIZADO"), anyString());
        servicio.marcarPedidoListoCocina(8L, "COC001", 3L);
        verify(pedidosRepo, times(1)).guardarEventoPendiente(eq(8L), eq("DETALLE_PLATO_ACTUALIZADO"), anyString());
    }
}
