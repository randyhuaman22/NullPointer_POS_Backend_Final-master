package pe.upeu.nullpointerpos.pedidos.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudAnularPedido;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudDescuentoPedido;
import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaPedido;
import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaResumenPago;
import pe.upeu.nullpointerpos.pedidos.service.ServicioPedidos;

/** [HU-17 / HU-29 START] Operaciones administrativas de descuento y anulación auditada. */
@RestController
@RequestMapping("/api/v1/pedidos")
public class ControladorOperacionesPedido {
    private final ServicioPedidos servicio;
    public ControladorOperacionesPedido(ServicioPedidos servicio) { this.servicio = servicio; }

    // [SPRINT 3 / HU-17 / S3-T-02 START]
    @PatchMapping("/{idPedido}/descuento")
    public RespuestaPedido descuento(@PathVariable Long idPedido, @Valid @RequestBody SolicitudDescuentoPedido solicitud, Authentication auth) {
        boolean permitido = auth.getAuthorities().stream().anyMatch(a -> java.util.Set.of("ROLE_CAJERO","ROLE_ADMINISTRADOR").contains(a.getAuthority()));
        return servicio.aplicarDescuento(idPedido, solicitud, auth.getName(), Long.valueOf(auth.getCredentials().toString()), permitido);
    }
    // [SPRINT 3 / HU-17 / S3-T-02 END]

    // [SPRINT 3 / HU-29 / S3-T-51..S3-T-53 START]
    @PostMapping("/{idPedido}/anular")
    public RespuestaPedido anular(@PathVariable Long idPedido, @Valid @RequestBody SolicitudAnularPedido solicitud, Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()));
        return servicio.anular(idPedido, solicitud, auth.getName(), Long.valueOf(auth.getCredentials().toString()), admin);
    }
    // [SPRINT 3 / HU-29 / S3-T-51..S3-T-53 END]

    // [T-36..T-37 START] API mínima de consulta para comprobar el total antes de aceptar cobro en cashier-service.
    @GetMapping("/{idPedido}/resumen-pago")
    public RespuestaResumenPago resumenPago(@PathVariable Long idPedido) { return servicio.resumenPago(idPedido); }

    @PatchMapping("/{idPedido}/listo-cocina")
    public RespuestaPedido marcarPedidoListoCocina(@PathVariable Long idPedido, Authentication auth) {
        return servicio.marcarPedidoListoCocina(idPedido, auth.getName(), Long.valueOf(auth.getCredentials().toString()));
    }
    // [T-36..T-37 END]
}
/** [HU-17 / HU-29 END] */
