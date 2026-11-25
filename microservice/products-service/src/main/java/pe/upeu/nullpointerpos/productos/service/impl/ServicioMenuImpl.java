package pe.upeu.nullpointerpos.productos.service.impl;

import pe.upeu.nullpointerpos.productos.dto.response.RespuestaCategoria;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaAreaNegocio;
import pe.upeu.nullpointerpos.productos.repository.RepositorioAdministracionMenu;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaGrupoOpcionesProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaOpcionesValidadas;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudSeleccionOpcionProducto;
import pe.upeu.nullpointerpos.productos.mapper.MenuMapper;
import pe.upeu.nullpointerpos.productos.repository.MenuRepositorio;
import pe.upeu.nullpointerpos.productos.service.ServicioMenu;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioMenuImpl implements ServicioMenu {
    private final MenuRepositorio repositorio;
    private final MenuMapper mapper;
    private final RepositorioAdministracionMenu repositorioAdministracion;
    public ServicioMenuImpl(MenuRepositorio repositorio, MenuMapper mapper,
                            RepositorioAdministracionMenu repositorioAdministracion) {
        this.repositorio = repositorio; this.mapper = mapper;
        this.repositorioAdministracion = repositorioAdministracion;
    }
    @Override public List<RespuestaAreaNegocio> listarAreasNegocio() {
        return repositorioAdministracion.listarAreas();
    }
    @Override public List<RespuestaCategoria> listarCategorias(String areaNegocio) {
        return repositorio.listarCategorias(areaNegocio).stream().map(mapper::aRespuesta).toList();
    }
    @Override public List<RespuestaProducto> listarProductos(String areaNegocio, String categoria, String busqueda, boolean soloDisponibles) {
        return repositorio.listarProductos(areaNegocio, categoria, busqueda, soloDisponibles).stream().map(mapper::aRespuesta).toList();
    }
    @Override public RespuestaProducto buscarProductoDisponibleParaPedido(Long idProducto) {
        return repositorio.buscarDisponibleParaPedido(idProducto).map(mapper::aRespuesta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El producto " + idProducto + " no existe o no está disponible."));
    }
    @Override public List<RespuestaGrupoOpcionesProducto> listarOpcionesProducto(Long idProducto) {
        return repositorioAdministracion.listarOpcionesProducto(idProducto);
    }
    @Override public RespuestaOpcionesValidadas validarOpcionesProducto(
            Long idProducto, List<SolicitudSeleccionOpcionProducto> seleccionadas) {
        return repositorioAdministracion.validarOpcionesProducto(idProducto, seleccionadas);
    }
    // [T-26 START]
    @Override public RespuestaProducto actualizarDisponibilidad(Long idProducto, boolean disponible, String motivo, String usuarioActor) {
        var actualizado = repositorio.actualizarDisponibilidad(idProducto, disponible, motivo, usuarioActor);
        if (actualizado == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado o inactivo.");
        return mapper.aRespuesta(actualizado);
    }
    @Override public List<java.util.Map<String,Object>> auditoriaDisponibilidad(Long idProducto) { return repositorio.auditoriaDisponibilidad(idProducto); }
    // [T-26 END]
}
