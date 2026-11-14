package pe.upeu.nullpointerpos.pedidos.mapper;

import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaPedido;
import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaDetallePedido;
import pe.upeu.nullpointerpos.pedidos.entity.DetallePedido;
import pe.upeu.nullpointerpos.pedidos.entity.Pedido;
import org.springframework.stereotype.Component;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class PedidoMapper {
    public Pedido estadoDesdeFila(ResultSet rs) throws SQLException {
        return new Pedido(rs.getLong("id_pedido"), null, rs.getLong("id_mesa"), null,
                null, rs.getString("codigo_mozo"), rs.getString("estado"), null,
                null, null, rs.getBigDecimal("importe_total"), null, null, null);
    }
    public Pedido encabezadoDesdeFila(ResultSet rs) throws SQLException {
        return new Pedido(rs.getLong("id_pedido"), rs.getString("codigo_pedido"), rs.getLong("id_mesa"),
                rs.getString("codigo_mesa"), rs.getLong("id_usuario_mozo"), rs.getString("codigo_mozo"),
                rs.getString("estado"), rs.getString("observaciones"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("total_descuento"), rs.getBigDecimal("importe_total"),
                rs.getObject("fecha_creacion", OffsetDateTime.class),
                rs.getObject("fecha_envio", OffsetDateTime.class), rs.getObject("fecha_pago", OffsetDateTime.class));
    }
    public DetallePedido detalleDesdeFila(ResultSet rs) throws SQLException {
        return new DetallePedido(rs.getLong("id_detalle_pedido"), (Long) rs.getObject("id_producto"),
                rs.getString("nombre_producto"), rs.getInt("cantidad"), rs.getBigDecimal("precio_unitario"),
                rs.getString("observaciones"), rs.getBigDecimal("total_linea"), rs.getString("estado"),
                rs.getString("estacion_cocina"), rs.getObject("fecha_finalizacion", OffsetDateTime.class));
    }
    public RespuestaPedido aRespuesta(Pedido p, List<DetallePedido> detalles) {
        List<RespuestaDetallePedido> filas = detalles.stream().map(d -> new RespuestaDetallePedido(
                d.idDetallePedido(), d.idProducto(), d.nombreProducto(), d.cantidad(), d.precioUnitario(),
                d.observaciones(), d.totalLinea(), d.estado(), d.estacionCocina(), d.fechaFinalizacion())).toList();
        return new RespuestaPedido(p.idPedido(), p.codigoPedido(), p.idMesa(), p.codigoMesa(), p.idUsuarioMozo(),
                p.codigoMozo(), p.estado(), p.observaciones(), p.subtotal(), p.totalDescuento(), p.importeTotal(),
                p.fechaCreacion(), p.fechaEnvio(), p.fechaPago(), filas);
    }
}
