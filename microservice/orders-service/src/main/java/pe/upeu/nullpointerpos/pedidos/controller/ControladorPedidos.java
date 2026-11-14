package pe.upeu.nullpointerpos.pedidos.controller;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudCrearPedido;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudModificarDetallesPedido;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudSincronizarPedidoOffline;
import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaPedido;
import pe.upeu.nullpointerpos.pedidos.service.ServicioPedidos;
import java.util.List;
import java.util.Map;
/** [HU-04 / HU-05 / HU-07 START] Creación, edición del borrador, sincronización offline y envío de comanda a cocina. */
@RestController
@RequestMapping("/api/v1/pedidos")
public class ControladorPedidos {
    private final ServicioPedidos servicio;
    public ControladorPedidos(ServicioPedidos servicio) { this.servicio = servicio; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public RespuestaPedido crear(@Valid @RequestBody SolicitudCrearPedido solicitud, Authentication autenticacion) {
        try {
            return servicio.crear(solicitud, autenticacion.getName(), idUsuario(autenticacion));
        } catch (DuplicateKeyException ex) {
            if (solicitud.idempotencia() != null && !solicitud.idempotencia().isBlank()) {
                return servicio.buscarPorIdempotencia(solicitud.idempotencia(), autenticacion.getName(), esAdministrador(autenticacion));
            }
            throw ex;
        }
    }
    @PostMapping("/sincronizar")
    public RespuestaPedido sincronizarOffline(@Valid @RequestBody SolicitudSincronizarPedidoOffline solicitud, Authentication autenticacion) {
        try {
            return servicio.sincronizarOffline(solicitud, autenticacion.getName(), idUsuario(autenticacion), esAdministrador(autenticacion));
        } catch (DuplicateKeyException ex) {
            return servicio.buscarPorIdempotencia(solicitud.idempotencia(), autenticacion.getName(), esAdministrador(autenticacion));
        }
    }
    @GetMapping("/salon-activos")
    public List<RespuestaPedido> listarActivosSalon() {
        return servicio.listarPedidosActivosSalon();
    }
    @GetMapping("/pendientes-cobro")
    public List<RespuestaPedido> listarPendientesCobro() {
        return servicio.listarPendientesCobro();
    }
    @GetMapping("/pendientes-cobro/{idPedido}")
    public RespuestaPedido buscarPendienteCobro(@PathVariable Long idPedido) {
        return servicio.buscarPendienteCobroPorId(idPedido);
    }
    @GetMapping("/{idPedido}")
    public RespuestaPedido buscarPorId(@PathVariable Long idPedido, Authentication autenticacion) {
        return servicio.buscarPorId(idPedido, autenticacion.getName(), esAdministrador(autenticacion));
    }
    @PutMapping("/{idPedido}/detalles")
    public RespuestaPedido actualizarDetalles(@PathVariable Long idPedido,
            @Valid @RequestBody SolicitudModificarDetallesPedido solicitud, Authentication autenticacion) {
        return servicio.actualizarDetalles(idPedido, solicitud, autenticacion.getName(), idUsuario(autenticacion), esAdministrador(autenticacion));
    }
    @PostMapping("/{idPedido}/enviar-cocina")
    public Map<String,Object> enviarACocina(@PathVariable Long idPedido, Authentication autenticacion) {
        RespuestaPedido pedido = servicio.enviarACocina(idPedido, autenticacion.getName(), idUsuario(autenticacion), esAdministrador(autenticacion));
        return Map.of("mensaje", "Pedido enviado a cocina correctamente.", "pedido", pedido);
    }
    private Long idUsuario(Authentication a) { return Long.valueOf(a.getCredentials().toString()); }
    private boolean esAdministrador(Authentication a) { return a.getAuthorities().stream().anyMatch(x -> "ROLE_ADMINISTRADOR".equals(x.getAuthority())); }
}
/** [HU-04 / HU-05 / HU-07 END] */
