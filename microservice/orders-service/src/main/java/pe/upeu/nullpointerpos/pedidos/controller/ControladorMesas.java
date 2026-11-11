package pe.upeu.nullpointerpos.pedidos.controller;
import pe.upeu.nullpointerpos.pedidos.service.ServicioMesas;
import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaMesa;
import org.springframework.web.bind.annotation.*;
import java.util.List;
/** [HU-02 START] Consulta de mesas y su estado operativo. */
@RestController
@RequestMapping("/api/v1/mesas")
public class ControladorMesas {
    private final ServicioMesas servicio;
    public ControladorMesas(ServicioMesas servicio) { this.servicio = servicio; }
    @GetMapping public List<RespuestaMesa> listar() { return servicio.listarMesas(); }
}
/** [HU-02 END] */
