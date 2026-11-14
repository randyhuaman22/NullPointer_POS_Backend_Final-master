package pe.upeu.nullpointerpos.productos.service;

import pe.upeu.nullpointerpos.productos.dto.response.RespuestaAreaNegocio;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaCategoria;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaGrupoOpcionesProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaOpcionesValidadas;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudSeleccionOpcionProducto;
import java.util.List;
import java.util.Map;

public interface ServicioMenu {
    List<RespuestaAreaNegocio> listarAreasNegocio();
    List<RespuestaCategoria> listarCategorias(String areaNegocio);
    List<RespuestaProducto> listarProductos(String areaNegocio, String categoria, String busqueda, boolean soloDisponibles);
    RespuestaProducto buscarProductoDisponibleParaPedido(Long idProducto);
    RespuestaProducto actualizarDisponibilidad(Long idProducto, boolean disponible, String motivo, String usuarioActor);
    List<Map<String,Object>> auditoriaDisponibilidad(Long idProducto);
    List<RespuestaGrupoOpcionesProducto> listarOpcionesProducto(Long idProducto);
    RespuestaOpcionesValidadas validarOpcionesProducto(Long idProducto, List<SolicitudSeleccionOpcionProducto> seleccionadas);
}
