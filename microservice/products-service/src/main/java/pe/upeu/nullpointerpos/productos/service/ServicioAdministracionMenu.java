package pe.upeu.nullpointerpos.productos.service;

import pe.upeu.nullpointerpos.productos.dto.request.SolicitudCategoria;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudEstacionCocina;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudProducto;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudConfiguracionOpcionesProducto;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudSeleccionOpcionProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaGrupoOpcionesProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaOpcionesValidadas;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaCategoriaAdministracion;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaEstacionCocina;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaProductoAdministracion;
import pe.upeu.nullpointerpos.productos.repository.RepositorioAdministracionMenu;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class ServicioAdministracionMenu {
    private final RepositorioAdministracionMenu repositorio;

    public ServicioAdministracionMenu(RepositorioAdministracionMenu repositorio) {
        this.repositorio = repositorio;
    }

    public List<RespuestaCategoriaAdministracion> listarCategorias(String area, boolean incluirInactivas) {
        validarAreaOpcional(area);
        return repositorio.listarCategoriasAdministracion(area, incluirInactivas);
    }

    public RespuestaCategoriaAdministracion buscarCategoria(Long id) {
        return repositorio.buscarCategoriaAdministracion(id)
                .orElseThrow(() -> noEncontrado("Categoría no encontrada."));
    }

    public RespuestaCategoriaAdministracion crearCategoria(SolicitudCategoria solicitud, String actor) {
        validarArea(solicitud.areaNegocio());
        validarCategoriaPadre(null, solicitud.idCategoriaPadre(), solicitud.areaNegocio());
        try {
            return repositorio.crearCategoria(solicitud, actor);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una categoría con ese nombre o la categoría padre no es válida.");
        }
    }

    public RespuestaCategoriaAdministracion actualizarCategoria(Long id, SolicitudCategoria solicitud, String actor) {
        validarArea(solicitud.areaNegocio());
        var anterior = repositorio.buscarCategoriaAdministracion(id)
                .orElseThrow(() -> noEncontrado("Categoría no encontrada."));
        if (!anterior.activo()) throw noEncontrado("La categoría está inactiva; no se puede modificar.");
        validarCategoriaPadre(id, solicitud.idCategoriaPadre(), solicitud.areaNegocio());
        boolean cambiaArea = !anterior.areaNegocio().equalsIgnoreCase(solicitud.areaNegocio());
        if (cambiaArea && (repositorio.tieneProductosActivos(id) || repositorio.tieneSubcategoriasActivas(id))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede mover de área una categoría que contiene productos o subcategorías activas.");
        }
        if (repositorio.tieneSubcategoriasActivas(id) && solicitud.idCategoriaPadre() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Una categoría que tiene subcategorías no puede convertirse en subcategoría.");
        }
        try {
            var respuesta = repositorio.actualizarCategoria(id, solicitud, actor);
            if (respuesta == null) throw noEncontrado("Categoría no encontrada.");
            return respuesta;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre ya está utilizado por otra categoría.");
        }
    }

    public void desactivarCategoria(Long id, String actor) {
        if (!repositorio.buscarCategoriaAdministracion(id).isPresent()) throw noEncontrado("Categoría no encontrada.");
        if (repositorio.tieneProductosActivos(id) || repositorio.tieneSubcategoriasActivas(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede desactivar la categoría mientras tenga productos o subcategorías activas.");
        }
        if (!repositorio.desactivarCategoria(id, actor)) throw noEncontrado("Categoría no encontrada o ya inactiva.");
    }

    public RespuestaCategoriaAdministracion restaurarCategoria(Long id, String actor) {
        var categoria = repositorio.buscarCategoriaAdministracion(id)
                .orElseThrow(() -> noEncontrado("Categoría no encontrada."));
        if (categoria.activo()) throw new ResponseStatusException(HttpStatus.CONFLICT, "La categoría ya está activa.");
        validarArea(categoria.areaNegocio());
        if (categoria.idCategoriaPadre() != null && !repositorio.existeCategoriaActiva(categoria.idCategoriaPadre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Primero debes restaurar la categoría padre.");
        }
        if (!repositorio.restaurarCategoria(id, actor)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se pudo restaurar la categoría. Verifica que su área y categoría padre estén activas.");
        return repositorio.buscarCategoriaAdministracion(id).orElseThrow();
    }

    public List<RespuestaProductoAdministracion> listarProductos(String area, Long categoria,
                                                                  String busqueda, boolean incluirInactivos) {
        validarAreaOpcional(area);
        if (categoria != null && !repositorio.existeCategoriaActiva(categoria) && !incluirInactivos) {
            throw noEncontrado("La categoría indicada no existe o está inactiva.");
        }
        return repositorio.listarProductosAdministracion(area, categoria, busqueda, incluirInactivos);
    }

    public RespuestaProductoAdministracion buscarProducto(Long id) {
        return repositorio.buscarProductoAdministracion(id)
                .orElseThrow(() -> noEncontrado("Producto no encontrado."));
    }

    public RespuestaProductoAdministracion crearProducto(SolicitudProducto solicitud, String actor) {
        validarProducto(solicitud);
        try {
            return repositorio.crearProducto(solicitud, actor);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El código de producto ya existe o se incumplió una regla de integridad.");
        }
    }

    public RespuestaProductoAdministracion actualizarProducto(Long id, SolicitudProducto solicitud, String actor) {
        validarProducto(solicitud);
        if (repositorio.buscarProductoAdministracion(id).filter(RespuestaProductoAdministracion::activo).isEmpty()) {
            throw noEncontrado("Producto no encontrado o inactivo.");
        }
        try {
            var respuesta = repositorio.actualizarProducto(id, solicitud, actor);
            if (respuesta == null) throw noEncontrado("Producto no encontrado o inactivo.");
            return respuesta;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El código de producto ya existe o se incumplió una regla de integridad.");
        }
    }

    public void desactivarProducto(Long id, String actor) {
        if (!repositorio.desactivarProducto(id, actor)) throw noEncontrado("Producto no encontrado o ya inactivo.");
    }

    public RespuestaProductoAdministracion restaurarProducto(Long id, String actor) {
        var producto = repositorio.buscarProductoAdministracion(id)
                .orElseThrow(() -> noEncontrado("Producto no encontrado."));
        if (!repositorio.existeCategoriaActiva(producto.idCategoria())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede restaurar el producto porque su categoría está inactiva.");
        }
        if (!repositorio.restaurarProducto(id, actor)) throw noEncontrado("El producto ya está activo.");
        return repositorio.buscarProductoAdministracion(id).orElseThrow();
    }

    public List<RespuestaEstacionCocina> listarEstaciones() {
        return repositorio.listarEstaciones();
    }

    public RespuestaEstacionCocina crearEstacion(SolicitudEstacionCocina solicitud, String actor) {
        try {
            return repositorio.crearEstacion(solicitud, actor);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una estación con ese nombre.");
        }
    }

    public RespuestaEstacionCocina actualizarEstacion(Long id, SolicitudEstacionCocina solicitud, String actor) {
        try {
            var respuesta = repositorio.actualizarEstacion(id, solicitud, actor);
            if (respuesta == null) throw noEncontrado("Estación no encontrada o inactiva.");
            return respuesta;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una estación con ese nombre.");
        }
    }

    public void desactivarEstacion(Long id, String actor) {
        if (repositorio.estacionTieneProductosActivos(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede desactivar una estación que tiene productos activos asignados.");
        }
        if (!repositorio.desactivarEstacion(id, actor)) throw noEncontrado("Estación no encontrada o ya inactiva.");
    }

    public void restaurarEstacion(Long id, String actor) {
        if (!repositorio.restaurarEstacion(id, actor)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La estación no existe o ya está activa.");
    }

    public List<RespuestaGrupoOpcionesProducto> listarOpcionesProducto(Long idProducto) {
        return repositorio.listarOpcionesProducto(idProducto);
    }

    public List<RespuestaGrupoOpcionesProducto> guardarOpcionesProducto(
            Long idProducto, SolicitudConfiguracionOpcionesProducto solicitud, String actor) {
        if (solicitud.grupos().size() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un producto puede tener como máximo 20 grupos de opciones.");
        }
        java.util.Set<String> nombres = new java.util.HashSet<>();
        for (var grupo : solicitud.grupos()) {
            if (!nombres.add(grupo.nombre().trim().toLowerCase(java.util.Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No repitas nombres de grupos de opciones.");
            }
        }
        try {
            return repositorio.guardarOpcionesProducto(idProducto, solicitud, actor);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se pudo guardar la configuración: revisa los nombres, límites y valores de las opciones.");
        }
    }

    public RespuestaOpcionesValidadas validarOpcionesProducto(
            Long idProducto, List<SolicitudSeleccionOpcionProducto> seleccionadas) {
        return repositorio.validarOpcionesProducto(idProducto, seleccionadas);
    }

    public List<Map<String, Object>> listarAuditoria(String entidad, Long idEntidad, int limite) {
        return repositorio.listarAuditoria(entidad, idEntidad, limite);
    }

    private void validarProducto(SolicitudProducto solicitud) {
        if (!repositorio.existeCategoriaActiva(solicitud.idCategoria())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una categoría activa.");
        }
        if (!repositorio.existeEstacionActiva(solicitud.idEstacionCocina())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una estación de preparación activa.");
        }
        if (solicitud.tiempoPreparacionMinutos() != null && solicitud.tiempoPreparacionMinutos() > 240) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tiempo de preparación no puede superar 240 minutos.");
        }
    }

    private void validarArea(String codigo) {
        if (!repositorio.existeAreaActiva(codigo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El área debe ser POLLERIA, MENU, HELADERIA o JUGUERIA.");
        }
    }

    private void validarAreaOpcional(String codigo) {
        if (codigo != null && !codigo.isBlank()) validarArea(codigo);
    }

    private void validarCategoriaPadre(Long idActual, Long idPadre, String area) {
        if (idPadre == null) return;
        if (idActual != null && idActual.equals(idPadre)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Una categoría no puede ser su propia categoría padre.");
        }
        if (!repositorio.existeCategoriaPadreActiva(idPadre)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría padre no existe o está inactiva.");
        }
        if (!repositorio.esCategoriaRaiz(idPadre)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se permite un nivel de subcategorías para mantener sencillo el catálogo.");
        }
        if (!repositorio.areaDeCategoria(idPadre).orElse("").equalsIgnoreCase(area)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La categoría padre debe pertenecer a la misma área del restaurante.");
        }
    }

    private ResponseStatusException noEncontrado(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }
}
