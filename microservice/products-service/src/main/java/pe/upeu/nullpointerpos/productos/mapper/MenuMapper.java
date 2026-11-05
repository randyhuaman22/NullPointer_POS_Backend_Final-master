package pe.upeu.nullpointerpos.productos.mapper;

import pe.upeu.nullpointerpos.productos.entity.CategoriaProducto;
import pe.upeu.nullpointerpos.productos.entity.Producto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaCategoria;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaProducto;
import org.springframework.stereotype.Component;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MenuMapper {
    public CategoriaProducto categoriaDesdeFila(ResultSet rs) throws SQLException {
        return new CategoriaProducto(rs.getLong("id_categoria"), rs.getString("nombre"),
                rs.getString("descripcion"), rs.getInt("orden_visualizacion"),
                rs.getString("area_negocio"), (Long) rs.getObject("id_categoria_padre"));
    }
    public Producto productoDesdeFila(ResultSet rs) throws SQLException {
        return new Producto(rs.getLong("id_producto"), rs.getString("codigo_producto"), rs.getString("nombre"),
                rs.getString("descripcion"), rs.getString("url_imagen"), rs.getBigDecimal("precio_unitario"),
                rs.getString("nombre_categoria"), rs.getString("nombre_estacion_cocina"), rs.getBoolean("disponible"));
    }
    public RespuestaCategoria aRespuesta(CategoriaProducto c) {
        return new RespuestaCategoria(c.idCategoria(), c.nombre(), c.descripcion(), c.ordenVisualizacion(),
                c.areaNegocio(), c.idCategoriaPadre());
    }
    public RespuestaProducto aRespuesta(Producto p) {
        return new RespuestaProducto(p.idProducto(), p.codigoProducto(), p.nombre(), p.descripcion(), p.urlImagen(),
                p.precioUnitario(), p.categoria(), p.estacionCocina(), p.disponible());
    }
}
