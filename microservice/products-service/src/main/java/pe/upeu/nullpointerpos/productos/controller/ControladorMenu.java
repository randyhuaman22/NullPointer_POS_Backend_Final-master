package pe.upeu.nullpointerpos.productos.controller;
import pe.upeu.nullpointerpos.productos.service.ServicioMenu;
import pe.upeu.nullpointerpos.productos.dto.response.*;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudDisponibilidadProducto;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudSeleccionOpcionProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaGrupoOpcionesProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaOpcionesValidadas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import java.util.List;
/** [HU-03 / HU-12 START] Consulta del catálogo y actualización de disponibilidad por rol. */
@RestController
@RequestMapping("/api/v1/menu")
public class ControladorMenu {
    private final ServicioMenu servicio;
    public ControladorMenu(ServicioMenu servicio) { this.servicio = servicio; }
    @GetMapping("/areas") public List<RespuestaAreaNegocio> listarAreasNegocio() { return servicio.listarAreasNegocio(); }
    @GetMapping("/categorias") public List<RespuestaCategoria> listarCategorias(
            @RequestParam(name="area", required=false) String area) { return servicio.listarCategorias(area); }
    @GetMapping("/productos/{idProducto}")
    public RespuestaProducto buscarProductoDisponible(@PathVariable Long idProducto) {
        return servicio.buscarProductoDisponibleParaPedido(idProducto);
    }
    @GetMapping("/productos/{idProducto}/opciones")
    public List<RespuestaGrupoOpcionesProducto> listarOpcionesProducto(@PathVariable Long idProducto) {
        return servicio.listarOpcionesProducto(idProducto);
    }
    @PostMapping("/productos/{idProducto}/opciones/validar")
    public RespuestaOpcionesValidadas validarOpcionesProducto(
            @PathVariable Long idProducto,
            @RequestBody(required = false) List<@NotNull @Valid SolicitudSeleccionOpcionProducto> seleccionadas) {
        return servicio.validarOpcionesProducto(idProducto, seleccionadas);
    }
    @PatchMapping("/disponibilidad")
    public RespuestaProducto actualizarDisponibilidad(@Valid @RequestBody SolicitudDisponibilidadProducto solicitud, Authentication auth) {
        RespuestaProducto respuesta = servicio.actualizarDisponibilidad(solicitud.idProducto(), solicitud.disponible(), solicitud.motivo(), auth.getName());
        return respuesta;
    }
    @GetMapping("/auditoria-disponibilidad")
    public List<Map<String,Object>> auditoriaDisponibilidad(@RequestParam(required=false) Long idProducto) { return servicio.auditoriaDisponibilidad(idProducto); }
    @GetMapping("/productos") public List<RespuestaProducto> listarProductos(
            @RequestParam(name="area", required=false) String area,
            @RequestParam(name="categoria", required=false) String categoria,
            @RequestParam(name="busqueda", required=false) String busqueda,
            @RequestParam(name="soloDisponibles", defaultValue="true") boolean soloDisponibles) {
        return servicio.listarProductos(area, categoria, busqueda, soloDisponibles);
    }
}
/** [HU-03 / HU-12 END] */
