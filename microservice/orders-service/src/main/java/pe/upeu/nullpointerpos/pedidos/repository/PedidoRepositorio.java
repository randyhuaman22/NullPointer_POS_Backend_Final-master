package pe.upeu.nullpointerpos.pedidos.repository;

import pe.upeu.nullpointerpos.pedidos.entity.Pedido;
import pe.upeu.nullpointerpos.pedidos.entity.DetallePedido;
import pe.upeu.nullpointerpos.pedidos.mapper.PedidoMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PedidoRepositorio {
    private final JdbcTemplate jdbc;
    private final PedidoMapper mapper;
    public PedidoRepositorio(JdbcTemplate jdbc, PedidoMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    public Long insertarPedido(String codigo, Long idMesa, Long idUsuario, String codigoMozo, String observaciones) {
        return insertarPedido(codigo, idMesa, idUsuario, codigoMozo, observaciones, null, null);
    }
    public Long insertarPedido(String codigo, Long idMesa, Long idUsuario, String codigoMozo, String observaciones,
                               String llaveIdempotencia, OffsetDateTime fechaRegistroOffline) {
        return jdbc.queryForObject("INSERT INTO esquema_comandas.pedidos(codigo_pedido, id_mesa, id_usuario_mozo, codigo_mozo, estado, observaciones, subtotal, importe_total, llave_idempotencia, fecha_registro_offline) " +
                "VALUES (?, ?, ?, ?, 'BORRADOR', ?, 0, 0, ?, ?) RETURNING id_pedido", Long.class,
                codigo, idMesa, idUsuario, codigoMozo, observaciones, llaveIdempotencia, fechaRegistroOffline);
    }
    public Optional<Long> buscarIdPorIdempotencia(String llaveIdempotencia) {
        if (llaveIdempotencia == null || llaveIdempotencia.isBlank()) return Optional.empty();
        return jdbc.query("SELECT id_pedido FROM esquema_comandas.pedidos WHERE llave_idempotencia = ? LIMIT 1",
                rs -> rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty(), llaveIdempotencia.trim());
    }
    public Optional<Pedido> buscarResumenActivoPorMesa(Long idMesa) {
        return jdbc.query("SELECT id_pedido, id_mesa, estado, codigo_mozo, importe_total FROM esquema_comandas.pedidos " +
                        "WHERE id_mesa = ? AND estado NOT IN ('PAGADO','ANULADO') ORDER BY id_pedido DESC LIMIT 1",
                rs -> rs.next() ? Optional.of(mapper.estadoDesdeFila(rs)) : Optional.empty(), idMesa);
    }
    public void insertarDetalle(Long idPedido, Long idProducto, String nombre, Integer cantidad,
                                BigDecimal precio, String observaciones, String estacionCocina) {
        jdbc.update("INSERT INTO esquema_comandas.detalles_pedido(id_pedido, id_producto, nombre_producto, cantidad, precio_unitario, observaciones, estacion_cocina) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)", idPedido, idProducto, nombre, cantidad, precio, observaciones, estacionCocina);
    }
    public void eliminarDetalles(Long idPedido) {
        jdbc.update("DELETE FROM esquema_comandas.detalles_pedido WHERE id_pedido = ?", idPedido);
    }
    public BigDecimal calcularSubtotal(Long idPedido) {
        BigDecimal subtotal = jdbc.queryForObject("SELECT COALESCE(SUM(total_linea), 0) " +
                "FROM esquema_comandas.detalles_pedido WHERE id_pedido = ?", BigDecimal.class, idPedido);
        return subtotal == null ? BigDecimal.ZERO : subtotal;
    }
    public void actualizarTotales(Long idPedido, BigDecimal subtotal) {
        jdbc.update("UPDATE esquema_comandas.pedidos SET subtotal = ?, total_descuento = 0, importe_total = ?, fecha_actualizacion = NOW() " +
                "WHERE id_pedido = ?", subtotal, subtotal, idPedido);
    }
    public void actualizarFecha(Long idPedido) {
        jdbc.update("UPDATE esquema_comandas.pedidos SET fecha_actualizacion = NOW() WHERE id_pedido = ?", idPedido);
    }
    public void actualizarMesa(Long idPedido, Long idMesaDestino) {
        jdbc.update("UPDATE esquema_comandas.pedidos SET id_mesa = ?, fecha_actualizacion = NOW() WHERE id_pedido = ?",
                idMesaDestino, idPedido);
    }
    public OffsetDateTime registrarTraslado(Long idPedido, Long idOrigen, Long idDestino, Long idUsuario, String motivo) {
        return jdbc.queryForObject("INSERT INTO esquema_comandas.traslados_mesa(id_pedido, id_mesa_origen, id_mesa_destino, id_usuario_traslado, motivo) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING fecha_traslado", (rs, fila) -> rs.getObject(1, OffsetDateTime.class),
                idPedido, idOrigen, idDestino, idUsuario, motivo);
    }
    public void cambiarEstadoAEnviado(Long idPedido) {
        jdbc.update("UPDATE esquema_comandas.pedidos SET estado = 'ENVIADO_COCINA', fecha_envio = NOW(), " +
                "fecha_actualizacion = NOW() WHERE id_pedido = ?", idPedido);
    }
    public int contarDetalles(Long idPedido) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_comandas.detalles_pedido WHERE id_pedido = ?", Integer.class, idPedido);
        return n == null ? 0 : n;
    }
    // [T-01..T-03 START] Outbox transaccional de eventos entre microservicios.
    public void guardarEventoPendiente(Long idPedido, String json) {
        guardarEventoPendiente(idPedido, "PEDIDO_ENVIADO_COCINA", json);
    }
    public void guardarEventoPendiente(Long idPedido, String tipoEvento, String json) {
        jdbc.update("INSERT INTO esquema_comandas.eventos_pendientes(tipo_agregado, id_agregado, tipo_evento, contenido_evento) " +
                "VALUES ('PEDIDO', ?, ?, CAST(? AS jsonb))", idPedido, tipoEvento, json);
    }
    // [T-01..T-03 END]
    public Optional<Pedido> buscarBloqueando(Long idPedido) {
        return jdbc.query("SELECT id_pedido, id_mesa, estado, codigo_mozo, importe_total FROM esquema_comandas.pedidos WHERE id_pedido = ? FOR UPDATE",
                rs -> rs.next() ? Optional.of(mapper.estadoDesdeFila(rs)) : Optional.empty(), idPedido);
    }
    public Optional<Pedido> buscarEstado(Long idPedido) {
        return jdbc.query("SELECT id_pedido, id_mesa, estado, codigo_mozo, importe_total FROM esquema_comandas.pedidos WHERE id_pedido = ?",
                rs -> rs.next() ? Optional.of(mapper.estadoDesdeFila(rs)) : Optional.empty(), idPedido);
    }
    public Optional<Pedido> buscarEncabezado(Long idPedido) {
        String sql = "SELECT p.id_pedido, p.codigo_pedido, p.id_mesa, m.codigo_mesa, p.id_usuario_mozo, " +
                "p.codigo_mozo, p.estado, p.observaciones, p.subtotal, p.total_descuento, p.importe_total, " +
                "p.fecha_creacion, p.fecha_envio, p.fecha_pago FROM esquema_comandas.pedidos p " +
                "JOIN esquema_comandas.mesas m ON m.id_mesa = p.id_mesa WHERE p.id_pedido = ?";
        return jdbc.query(sql, rs -> rs.next() ? Optional.of(mapper.encabezadoDesdeFila(rs)) : Optional.empty(), idPedido);
    }
    public List<DetallePedido> listarDetalles(Long idPedido) {
        return jdbc.query("SELECT id_detalle_pedido, id_producto, nombre_producto, cantidad, precio_unitario, observaciones, total_linea, estado, estacion_cocina, fecha_finalizacion " +
                "FROM esquema_comandas.detalles_pedido WHERE id_pedido = ? ORDER BY id_detalle_pedido",
                (rs, fila) -> mapper.detalleDesdeFila(rs), idPedido);
    }
    public List<Long> listarIdsParaCocina() {
        return jdbc.query("SELECT id_pedido FROM esquema_comandas.pedidos " +
                "WHERE estado IN ('ENVIADO_COCINA','EN_PREPARACION','LISTO') ORDER BY fecha_envio, id_pedido",
                (rs, fila) -> rs.getLong("id_pedido"));
    }
    public List<Long> listarIdsActivosSalon() {
        return jdbc.query("SELECT id_pedido FROM esquema_comandas.pedidos " +
                "WHERE estado IN ('BORRADOR','ENVIADO_COCINA','EN_PREPARACION','LISTO','ENTREGADO','PENDIENTE_PAGO') ORDER BY id_pedido DESC",
                (rs, fila) -> rs.getLong("id_pedido"));
    }
    public List<Long> listarIdsPendientesCobro() {
        return jdbc.query("SELECT id_pedido FROM esquema_comandas.pedidos " +
                "WHERE estado IN ('ENVIADO_COCINA','EN_PREPARACION','LISTO','ENTREGADO','PENDIENTE_PAGO') ORDER BY fecha_envio NULLS LAST, id_pedido DESC",
                (rs, fila) -> rs.getLong("id_pedido"));
    }
    public void registrarAuditoria(Long idUsuario, String accion, Long idPedido, String motivo) {
        jdbc.update("INSERT INTO esquema_comandas.registros_auditoria(id_usuario_actor, accion, nombre_entidad, id_entidad, motivo) " +
                "VALUES (?, ?, 'PEDIDO', ?, ?)", idUsuario, accion,
                idPedido == null ? null : idPedido.toString(), motivo);
    }

    // [T-16..T-19 START] Transición de plato a LISTO bajo bloqueo transaccional.
    public int marcarDetalleListo(Long idDetalle) {
        return jdbc.update("UPDATE esquema_comandas.detalles_pedido SET estado='LISTO', fecha_finalizacion=NOW(), fecha_actualizacion=NOW() " +
                "WHERE id_detalle_pedido=? AND estado IN ('PENDIENTE','EN_PREPARACION')", idDetalle);
    }
    public int marcarTodosDetallesListos(Long idPedido) {
        return jdbc.update("UPDATE esquema_comandas.detalles_pedido SET estado='LISTO', fecha_finalizacion=NOW(), fecha_actualizacion=NOW() " +
                "WHERE id_pedido=? AND estado IN ('PENDIENTE','EN_PREPARACION')", idPedido);
    }
    public Long buscarIdPedidoPorDetalle(Long idDetalle) {
        return jdbc.query("SELECT id_pedido FROM esquema_comandas.detalles_pedido WHERE id_detalle_pedido=?",
                rs -> rs.next() ? rs.getLong(1) : null, idDetalle);
    }
    public int contarDetallesNoListos(Long idPedido) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_comandas.detalles_pedido WHERE id_pedido=? AND estado NOT IN ('LISTO','ENTREGADO','ANULADO')", Integer.class, idPedido);
        return n == null ? 0 : n;
    }
    public void actualizarPedidoListoSiCorresponde(Long idPedido) {
        if (contarDetallesNoListos(idPedido) == 0) {
            jdbc.update("UPDATE esquema_comandas.pedidos SET estado='LISTO', fecha_actualizacion=NOW() WHERE id_pedido=? AND estado IN ('ENVIADO_COCINA','EN_PREPARACION')", idPedido);
        }
    }
    // [T-16..T-19 END]

    // [SPRINT 3 / HU-17 / S3-T-04 START] Descuento calculado/persistido y auditado en orders-service.
    public void aplicarDescuento(Long idPedido, java.math.BigDecimal porcentaje, String codigoActor, Long idUsuarioActor, String motivo) {
        int actualizados = jdbc.update("UPDATE esquema_comandas.pedidos SET total_descuento=LEAST(ROUND(subtotal * ? / 100.0, 2), GREATEST(subtotal - 0.01, 0)), " +
                "importe_total=subtotal-LEAST(ROUND(subtotal * ? / 100.0, 2), GREATEST(subtotal - 0.01, 0)), usuario_descuento=?, " +
                "fecha_actualizacion=NOW() WHERE id_pedido=? AND estado NOT IN ('PAGADO','ANULADO','BORRADOR') " +
                "AND ? > 0 AND ? < 100", porcentaje, porcentaje, codigoActor, idPedido, porcentaje, porcentaje);
        if (actualizados != 1) {
            throw new IllegalStateException("El descuento no pudo aplicarse al pedido; verifique estado y porcentaje.");
        }
        registrarAuditoria(idUsuarioActor, "DESCUENTO_APLICADO", idPedido,
                "Porcentaje=" + porcentaje + "; " + motivo + "; usuario=" + codigoActor);
    }
    // [SPRINT 3 / HU-17 / S3-T-04 END]

    // [SPRINT 3 / HU-29 / S3-T-52..S3-T-53 START] Anulación solo de pedidos todavía no pagados.
    public int anularPedido(Long idPedido, String motivo) {
        return jdbc.update("UPDATE esquema_comandas.pedidos SET estado='ANULADO', motivo_anulacion=?, fecha_actualizacion=NOW() " +
                "WHERE id_pedido=? AND estado NOT IN ('PAGADO','ANULADO')", motivo, idPedido);
    }
    // [SPRINT 3 / HU-29 / S3-T-52 END]
    public void establecerEstadoPagado(Long idPedido) {
        jdbc.update("UPDATE esquema_comandas.pedidos SET estado='PAGADO', fecha_pago=NOW(), fecha_actualizacion=NOW() WHERE id_pedido=? AND estado <> 'ANULADO'", idPedido);
    }
    public int registrarPagoConfirmadoSiNoPagado(Long idPedido) {
        return jdbc.update("UPDATE esquema_comandas.pedidos SET estado='PAGADO', fecha_pago=NOW(), fecha_actualizacion=NOW() WHERE id_pedido=? AND estado NOT IN ('PAGADO','ANULADO','BORRADOR')", idPedido);
    }
    public ResumenPagoFila resumenPago(Long idPedido) {
        return jdbc.query("SELECT id_pedido,codigo_pedido,estado,importe_total,total_descuento FROM esquema_comandas.pedidos WHERE id_pedido=?",
                rs -> rs.next() ? new ResumenPagoFila(rs.getLong("id_pedido"), rs.getString("codigo_pedido"), rs.getString("estado"), rs.getBigDecimal("importe_total"), rs.getBigDecimal("total_descuento")) : null, idPedido);
    }
    public record ResumenPagoFila(Long idPedido, String codigoPedido, String estado, BigDecimal importeTotal, BigDecimal totalDescuento) { }

    public List<Map<String,Object>> ventasPorHora(java.time.LocalDate desde, java.time.LocalDate hasta) {
        return jdbc.queryForList("SELECT EXTRACT(HOUR FROM fecha_pago AT TIME ZONE 'America/Lima')::int AS hora, COUNT(*)::int AS pedidos, COALESCE(SUM(importe_total),0) AS total " +
                "FROM esquema_comandas.pedidos WHERE estado='PAGADO' AND (fecha_pago AT TIME ZONE 'America/Lima')::date >= ? AND (fecha_pago AT TIME ZONE 'America/Lima')::date <= ? " +
                "GROUP BY hora ORDER BY hora", java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta));
    }
    public List<Map<String,Object>> platosPopulares(java.time.LocalDate desde, java.time.LocalDate hasta) {
        return jdbc.queryForList("SELECT d.id_producto, d.nombre_producto, SUM(d.cantidad)::bigint AS unidades, " +
                "SUM(d.total_linea)::numeric(12,2) AS venta FROM esquema_comandas.detalles_pedido d " +
                "JOIN esquema_comandas.pedidos p ON p.id_pedido=d.id_pedido " +
                "WHERE p.estado='PAGADO' AND (p.fecha_pago AT TIME ZONE 'America/Lima')::date >= ? AND (p.fecha_pago AT TIME ZONE 'America/Lima')::date <= ? " +
                "AND d.estado <> 'ANULADO' GROUP BY d.id_producto,d.nombre_producto ORDER BY unidades DESC, venta DESC",
                java.sql.Date.valueOf(desde), java.sql.Date.valueOf(hasta));
    }
    // [SPRINT 3 / HU-29 / S3-T-53 START] Consulta de auditoría, limitada a 500 registros cuando no se filtra pedido.
    public List<Map<String,Object>> listarAuditoria(Long idPedido) {
        if (idPedido == null) return jdbc.queryForList("SELECT id_registro_auditoria,id_usuario_actor,accion,nombre_entidad,id_entidad,motivo,fecha_creacion FROM esquema_comandas.registros_auditoria ORDER BY fecha_creacion DESC LIMIT 500");
        return jdbc.queryForList("SELECT id_registro_auditoria,id_usuario_actor,accion,nombre_entidad,id_entidad,motivo,fecha_creacion FROM esquema_comandas.registros_auditoria WHERE id_entidad=? ORDER BY fecha_creacion DESC", idPedido.toString());
    }
    // [SPRINT 3 / HU-29 / S3-T-53 END]
}
