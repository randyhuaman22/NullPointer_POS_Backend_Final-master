package pe.upeu.nullpointerpos.pedidos.controller;

import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaPedido;
import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaPedidoCocina;
import pe.upeu.nullpointerpos.pedidos.service.ServicioPedidos;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** [HU-08 / HU-09 / HU-11 START] Consulta KDS con tiempos de demora y datos por estación. */
@RestController
@RequestMapping("/api/v1/cocina")
public class ControladorPedidosCocina {
    private final ServicioPedidos servicio;
    public ControladorPedidosCocina(ServicioPedidos servicio) { this.servicio = servicio; }

    // [HU-08 / T-07 START] Listado KDS con niveles de demora calculados en servidor.
    @GetMapping("/pedidos")
    public List<RespuestaPedidoCocina> listarPedidos() { return servicio.listarPedidosCocinaConDemora(); }
    // [HU-08 / T-07 END]

}
/** [HU-08 / HU-09 / HU-11 END] */
