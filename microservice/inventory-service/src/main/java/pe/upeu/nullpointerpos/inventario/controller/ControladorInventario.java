package pe.upeu.nullpointerpos.inventario.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.inventario.dto.request.*;
import pe.upeu.nullpointerpos.inventario.repository.RepositorioInventario;

import java.util.*;

/** [SPRINT 3 / HU-21..HU-25 / S3-T-21..S3-T-42] API de inventario y mantenimiento de insumos. */
@RestController
@RequestMapping("/api/v1/inventario")
public class ControladorInventario {
    private final RepositorioInventario repo;
    public ControladorInventario(RepositorioInventario repo) { this.repo = repo; }

    @GetMapping("/insumos")
    public List<Map<String,Object>> listar() { return repo.listar(); }

    @PostMapping("/insumos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> crear(@Valid @RequestBody SolicitudInsumo solicitud, Authentication auth) {
        return repo.crear(solicitud, idActor(auth));
    }

    @GetMapping("/insumos/{idInsumo}")
    public Map<String,Object> buscarInsumo(@PathVariable Long idInsumo) {
        Map<String,Object> insumo = repo.buscar(idInsumo);
        if (insumo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Insumo no encontrado.");
        return insumo;
    }

    @PutMapping("/insumos/{idInsumo}")
    public Map<String,Object> actualizar(@PathVariable Long idInsumo,
                                         @Valid @RequestBody SolicitudActualizarInsumo solicitud,
                                         Authentication auth) {
        return repo.actualizarInsumo(idInsumo, solicitud, idActor(auth));
    }

    @PatchMapping("/insumos/{idInsumo}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long idInsumo, Authentication auth) {
        if (repo.tieneRecetas(idInsumo)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede desactivar un insumo que forma parte de recetas. Actualiza primero las recetas afectadas.");
        if (!repo.cambiarActivo(idInsumo, false, idActor(auth))) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Insumo no encontrado o ya inactivo.");
    }

    @PatchMapping("/insumos/{idInsumo}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activar(@PathVariable Long idInsumo, Authentication auth) {
        if (!repo.cambiarActivo(idInsumo, true, idActor(auth))) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Insumo no encontrado o ya activo.");
    }

    @PostMapping("/insumos/{idInsumo}/ajustes")
    public Map<String,Object> ajustarStock(@PathVariable Long idInsumo,
                                           @Valid @RequestBody SolicitudAjusteStock solicitud,
                                           Authentication auth) {
        return repo.ajustarStock(idInsumo, solicitud, idActor(auth));
    }

    @GetMapping("/compras")
    public List<Map<String,Object>> compras() { return repo.compras(); }

    @GetMapping("/auditoria-insumos")
    public List<Map<String,Object>> auditoriaInsumos(@RequestParam(required=false) Long idInsumo) {
        return repo.auditoriaInsumos(idInsumo);
    }

    @GetMapping("/recetas/{idProducto}")
    public List<Map<String,Object>> receta(@PathVariable Long idProducto) {
        if (idProducto == null || idProducto <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"idProducto debe ser positivo.");
        return repo.receta(idProducto);
    }

    @PutMapping("/recetas/{idProducto}")
    public Map<String,Object> guardarReceta(@PathVariable Long idProducto,
                                            @Valid @RequestBody List<@Valid IngredienteReceta> ingredientes) {
        if (idProducto == null || idProducto <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"idProducto debe ser positivo.");
        if (ingredientes == null || ingredientes.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La receta debe incluir al menos un insumo.");
        Set<Long> ids = new HashSet<>();
        for (IngredienteReceta ingrediente : ingredientes) {
            if (ingrediente.idInsumo() == null || ingrediente.idInsumo() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"idInsumo debe ser positivo.");
            if (!ids.add(ingrediente.idInsumo())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La receta contiene un insumo repetido.");
        }
        repo.guardarReceta(new SolicitudReceta(idProducto, ingredientes));
        return Map.of("idProducto", idProducto, "ingredientes", ingredientes.size(), "estado", "GUARDADO");
    }

    @PostMapping("/compras")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> compra(@Valid @RequestBody SolicitudCompra solicitud, Authentication auth) {
        Long id = repo.comprar(solicitud, idActor(auth));
        return Map.of("idCompra", id, "items", solicitud.items().size(), "estado", "REGISTRADA");
    }

    @PostMapping("/mermas")
    public Map<String,Object> merma(@Valid @RequestBody SolicitudMerma solicitud, Authentication auth) {
        repo.merma(solicitud, idActor(auth));
        return Map.of("estado", "REGISTRADA", "idInsumo", solicitud.idInsumo(), "cantidad", solicitud.cantidad());
    }

    @GetMapping("/alertas") public List<Map<String,Object>> alertas() { return repo.alertas(); }
    @GetMapping("/movimientos") public List<Map<String,Object>> movimientos(@RequestParam(required=false) Long idInsumo) { return repo.movimientos(idInsumo); }
    @GetMapping("/faltantes-venta") public List<Map<String,Object>> faltantesVenta() { return repo.faltantesVenta(); }

    private Long idActor(Authentication auth) {
        if (auth == null || auth.getCredentials() == null) return null;
        try { return Long.valueOf(auth.getCredentials().toString()); }
        catch (NumberFormatException ex) { return null; }
    }
}
