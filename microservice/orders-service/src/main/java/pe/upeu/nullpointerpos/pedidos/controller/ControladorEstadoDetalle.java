package pe.upeu.nullpointerpos.pedidos.controller;

import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaPedido;
import pe.upeu.nullpointerpos.pedidos.service.ServicioPedidos;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** [T-17 START] Endpoint solicitado en el Excel para marcar un detalle de plato como listo. */
/** [HU-10 / T-15..T-19 START] Operación backend para marcar un detalle/plato como listo. */
@RestController
@RequestMapping("/api/v1/pedidos/detalle")
public class ControladorEstadoDetalle {
    private final ServicioPedidos servicio;
    public ControladorEstadoDetalle(ServicioPedidos servicio) { this.servicio = servicio; }
    @PatchMapping("/{id}")
    public RespuestaPedido marcarListo(@PathVariable Long id, Authentication auth) {
        return servicio.marcarDetalleListo(id, auth.getName(), Long.valueOf(auth.getCredentials().toString()));
    }
}
/** [T-17 END] */
/** [HU-10 / T-15..T-19 END] */
