package pe.upeu.nullpointerpos.pedidos.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.upeu.nullpointerpos.pedidos.repository.PedidoRepositorio;
import java.util.List;
import java.util.Map;

/** HU-29: lectura del registro de auditoría reservada a administración. */
@RestController
@RequestMapping("/api/v1/auditoria")
public class ControladorAuditoria {
    private final PedidoRepositorio repositorio;
    public ControladorAuditoria(PedidoRepositorio repositorio) { this.repositorio = repositorio; }
    @GetMapping("/pedidos")
    public List<Map<String,Object>> listar(@RequestParam(required=false) Long idPedido) { return repositorio.listarAuditoria(idPedido); }
}
