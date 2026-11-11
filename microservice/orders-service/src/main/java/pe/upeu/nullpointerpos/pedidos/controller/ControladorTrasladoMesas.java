package pe.upeu.nullpointerpos.pedidos.controller;
import jakarta.validation.Valid;
import pe.upeu.nullpointerpos.pedidos.service.ServicioPedidos;
import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaTrasladoMesa;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudTrasladoMesa;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
/** [HU-06 START] Traslado de pedido a mesa libre, con validación del propietario. */
@RestController
@RequestMapping("/api/v1/mesas")
public class ControladorTrasladoMesas {
    private final ServicioPedidos servicio;
    public ControladorTrasladoMesas(ServicioPedidos servicio) { this.servicio = servicio; }
    @PutMapping("/trasladar") public RespuestaTrasladoMesa trasladar(@Valid @RequestBody SolicitudTrasladoMesa solicitud,
            Authentication autenticacion) {
        boolean administrador = autenticacion.getAuthorities().stream().anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()));
        Long idUsuario = Long.valueOf(autenticacion.getCredentials().toString());
        return servicio.trasladar(solicitud, autenticacion.getName(), idUsuario, administrador);
    }
}
/** [HU-06 END] */
