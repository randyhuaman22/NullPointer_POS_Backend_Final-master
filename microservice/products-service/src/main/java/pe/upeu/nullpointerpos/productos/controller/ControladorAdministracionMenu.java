package pe.upeu.nullpointerpos.productos.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudCategoria;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudEstacionCocina;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudProducto;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudConfiguracionOpcionesProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaGrupoOpcionesProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaCategoriaAdministracion;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaEstacionCocina;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaProductoAdministracion;
import pe.upeu.nullpointerpos.productos.service.ServicioAdministracionMenu;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/menu/administracion")
public class ControladorAdministracionMenu {
    private final ServicioAdministracionMenu servicio;

    public ControladorAdministracionMenu(ServicioAdministracionMenu servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/categorias")
    public List<RespuestaCategoriaAdministracion> listarCategorias(
            @RequestParam(required = false) String area,
            @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        return servicio.listarCategorias(area, incluirInactivas);
    }

    @GetMapping("/categorias/{idCategoria}")
    public RespuestaCategoriaAdministracion buscarCategoria(@PathVariable Long idCategoria) {
        return servicio.buscarCategoria(idCategoria);
    }

    @PostMapping("/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaCategoriaAdministracion crearCategoria(@Valid @RequestBody SolicitudCategoria solicitud,
                                                            Authentication autenticacion) {
        return servicio.crearCategoria(solicitud, actor(autenticacion));
    }

    @PutMapping("/categorias/{idCategoria}")
    public RespuestaCategoriaAdministracion actualizarCategoria(@PathVariable Long idCategoria,
                                                                 @Valid @RequestBody SolicitudCategoria solicitud,
                                                                 Authentication autenticacion) {
        return servicio.actualizarCategoria(idCategoria, solicitud, actor(autenticacion));
    }

    @DeleteMapping("/categorias/{idCategoria}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarCategoria(@PathVariable Long idCategoria, Authentication autenticacion) {
        servicio.desactivarCategoria(idCategoria, actor(autenticacion));
    }

    @PatchMapping("/categorias/{idCategoria}/restaurar")
    public RespuestaCategoriaAdministracion restaurarCategoria(@PathVariable Long idCategoria,
                                                                Authentication autenticacion) {
        return servicio.restaurarCategoria(idCategoria, actor(autenticacion));
    }

    @GetMapping("/productos")
    public List<RespuestaProductoAdministracion> listarProductos(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return servicio.listarProductos(area, idCategoria, busqueda, incluirInactivos);
    }

    @GetMapping("/productos/{idProducto}")
    public RespuestaProductoAdministracion buscarProducto(@PathVariable Long idProducto) {
        return servicio.buscarProducto(idProducto);
    }

    @PostMapping("/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaProductoAdministracion crearProducto(@Valid @RequestBody SolicitudProducto solicitud,
                                                          Authentication autenticacion) {
        return servicio.crearProducto(solicitud, actor(autenticacion));
    }

    @PutMapping("/productos/{idProducto}")
    public RespuestaProductoAdministracion actualizarProducto(@PathVariable Long idProducto,
                                                               @Valid @RequestBody SolicitudProducto solicitud,
                                                               Authentication autenticacion) {
        return servicio.actualizarProducto(idProducto, solicitud, actor(autenticacion));
    }

    @DeleteMapping("/productos/{idProducto}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarProducto(@PathVariable Long idProducto, Authentication autenticacion) {
        servicio.desactivarProducto(idProducto, actor(autenticacion));
    }

    @PatchMapping("/productos/{idProducto}/restaurar")
    public RespuestaProductoAdministracion restaurarProducto(@PathVariable Long idProducto,
                                                              Authentication autenticacion) {
        return servicio.restaurarProducto(idProducto, actor(autenticacion));
    }

    @GetMapping("/productos/{idProducto}/opciones")
    public List<RespuestaGrupoOpcionesProducto> listarOpcionesProducto(@PathVariable Long idProducto) {
        return servicio.listarOpcionesProducto(idProducto);
    }

    @PutMapping("/productos/{idProducto}/opciones")
    public List<RespuestaGrupoOpcionesProducto> guardarOpcionesProducto(
            @PathVariable Long idProducto,
            @Valid @RequestBody SolicitudConfiguracionOpcionesProducto solicitud,
            Authentication autenticacion) {
        return servicio.guardarOpcionesProducto(idProducto, solicitud, actor(autenticacion));
    }

    @GetMapping("/estaciones")
    public List<RespuestaEstacionCocina> listarEstaciones() {
        return servicio.listarEstaciones();
    }

    @PostMapping("/estaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaEstacionCocina crearEstacion(@Valid @RequestBody SolicitudEstacionCocina solicitud,
                                                 Authentication autenticacion) {
        return servicio.crearEstacion(solicitud, actor(autenticacion));
    }

    @PutMapping("/estaciones/{idEstacion}")
    public RespuestaEstacionCocina actualizarEstacion(@PathVariable Long idEstacion,
                                                       @Valid @RequestBody SolicitudEstacionCocina solicitud,
                                                       Authentication autenticacion) {
        return servicio.actualizarEstacion(idEstacion, solicitud, actor(autenticacion));
    }

    @DeleteMapping("/estaciones/{idEstacion}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarEstacion(@PathVariable Long idEstacion, Authentication autenticacion) {
        servicio.desactivarEstacion(idEstacion, actor(autenticacion));
    }

    @PatchMapping("/estaciones/{idEstacion}/restaurar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restaurarEstacion(@PathVariable Long idEstacion, Authentication autenticacion) {
        servicio.restaurarEstacion(idEstacion, actor(autenticacion));
    }

    @GetMapping("/auditoria")
    public List<Map<String, Object>> listarAuditoria(@RequestParam(required = false) String entidad,
                                                     @RequestParam(required = false) Long idEntidad,
                                                     @RequestParam(defaultValue = "100") int limite) {
        if (limite < 1 || limite > 500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El límite debe estar entre 1 y 500.");
        return servicio.listarAuditoria(entidad, idEntidad, limite);
    }

    private String actor(Authentication autenticacion) {
        return autenticacion == null ? "DESCONOCIDO" : autenticacion.getName();
    }
}
