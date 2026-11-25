package pe.upeu.nullpointerpos.caja.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.*;
import pe.upeu.nullpointerpos.caja.dto.response.*;
import pe.upeu.nullpointerpos.caja.sunat.RespuestaComprobanteSunat;

/** [HU-13..HU-16 / HU-18..HU-20 / T-30 / T-35..T-37 / T-42 / T-45 / T-47 START] Persistencia exclusiva de bd_caja. */
@Repository
public class RepositorioCaja {
    private final pe.upeu.nullpointerpos.caja.sunat.ConfiguracionSunat configuracionSunat;
    private final JdbcTemplate jdbc;
    public RepositorioCaja(JdbcTemplate jdbc, pe.upeu.nullpointerpos.caja.sunat.ConfiguracionSunat configuracionSunat) {
        this.jdbc = jdbc;
        this.configuracionSunat = configuracionSunat;
    }

    public Long abrir(Long id, String codigo, BigDecimal monto) {
        return jdbc.queryForObject("INSERT INTO esquema_caja.turnos_caja(id_cajero,codigo_cajero,monto_inicial) VALUES(?,?,?) RETURNING id_turno", Long.class, id, codigo, monto);
    }
    public RespuestaTurnoCaja turnoAbierto(Long id) {
        return jdbc.query("SELECT * FROM esquema_caja.turnos_caja WHERE id_cajero=? AND estado='ABIERTO' ORDER BY id_turno DESC LIMIT 1", rs -> rs.next() ? turno(rs) : null, id);
    }
    public RespuestaTurnoCaja turnoPorId(Long id) {
        return jdbc.query("SELECT * FROM esquema_caja.turnos_caja WHERE id_turno=?", rs -> rs.next() ? turno(rs) : null, id);
    }
    private RespuestaTurnoCaja turno(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RespuestaTurnoCaja(rs.getLong("id_turno"), rs.getString("codigo_cajero"), rs.getBigDecimal("monto_inicial"), rs.getString("estado"), rs.getObject("fecha_apertura", java.time.OffsetDateTime.class), rs.getObject("fecha_cierre", java.time.OffsetDateTime.class), rs.getBigDecimal("monto_esperado"), rs.getBigDecimal("monto_real"), rs.getBigDecimal("diferencia"));
    }
    public boolean existeIdempotencia(String key) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_caja.pagos WHERE idempotencia=?", Integer.class, key);
        return n != null && n > 0;
    }
    public boolean existeNumeroOperacion(String referencia) {
        if (referencia == null || referencia.isBlank()) return false;
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_caja.referencias_operacion_digital WHERE numero_operacion=?", Integer.class, referencia.trim());
        return n != null && n > 0;
    }
    public Long insertarPago(Long pedido, Long turno, Long cajero, String codigo, String metodo, BigDecimal importe, BigDecimal recibido, BigDecimal vuelto, String operacion, String ultimos, String key) {
        return jdbc.queryForObject("INSERT INTO esquema_caja.pagos(id_pedido,id_turno,id_cajero,codigo_cajero,metodo,importe,importe_recibido,vuelto,numero_operacion,ultimos_digitos,idempotencia) VALUES(?,?,?,?,?,?,?,?,?,?,?) RETURNING id_pago", Long.class, pedido, turno, cajero, codigo, metodo, importe, recibido, vuelto, operacion, ultimos, key);
    }
    public void insertarParte(Long id, String metodo, BigDecimal importe, String op, String ultimos) {
        if (op != null && !op.isBlank()) jdbc.update("INSERT INTO esquema_caja.referencias_operacion_digital(numero_operacion,id_pago) VALUES(?,?)", op.trim(), id);
        jdbc.update("INSERT INTO esquema_caja.partes_pago(id_pago,metodo,importe,numero_operacion,ultimos_digitos) VALUES(?,?,?,?,?)", id, metodo, importe, op == null || op.isBlank() ? null : op.trim(), ultimos);
    }
    public void guardarEvento(String json) {
        jdbc.update("INSERT INTO esquema_caja.outbox_eventos(tipo_evento,contenido_evento) VALUES('PAGO_CONFIRMADO',CAST(? AS jsonb))", json);
    }
    public List<Map<String, Object>> pendientes() {
        return jdbc.queryForList("SELECT id_evento,contenido_evento::text AS contenido_evento FROM esquema_caja.outbox_eventos WHERE fecha_publicacion IS NULL ORDER BY id_evento LIMIT 50");
    }
    public void marcarPublicado(Long id) {
        jdbc.update("UPDATE esquema_caja.outbox_eventos SET fecha_publicacion=NOW() WHERE id_evento=? AND fecha_publicacion IS NULL", id);
    }
    public RespuestaPago buscarPago(Long id) {
        return buscarPagoUnico("SELECT * FROM esquema_caja.pagos WHERE id_pago=?", id);
    }
    public RespuestaPago buscarPagoPorIdempotencia(String key) {
        if (key == null || key.isBlank()) return null;
        return buscarPagoUnico("SELECT * FROM esquema_caja.pagos WHERE idempotencia=?", key.trim());
    }
    private RespuestaPago buscarPagoUnico(String sql, Object param) {
        return jdbc.query(sql, rs -> rs.next() ? new RespuestaPago(rs.getLong("id_pago"), rs.getLong("id_pedido"), rs.getLong("id_turno"), rs.getString("metodo"), rs.getBigDecimal("importe"), rs.getBigDecimal("importe_recibido"), rs.getBigDecimal("vuelto"), rs.getString("estado"), rs.getObject("fecha_creacion", java.time.OffsetDateTime.class)) : null, param);
    }
    public BigDecimal totalEfectivo(Long turno) {
        BigDecimal n = jdbc.queryForObject("SELECT COALESCE(SUM(pp.importe),0) FROM esquema_caja.partes_pago pp JOIN esquema_caja.pagos p ON p.id_pago=pp.id_pago WHERE p.id_turno=? AND p.estado='CONFIRMADO' AND pp.metodo='EFECTIVO'", BigDecimal.class, turno);
        return n == null ? BigDecimal.ZERO : n;
    }
    public RespuestaCierreCaja cerrar(Long turno, BigDecimal esperado, BigDecimal real, String motivo) {
        int n = jdbc.update("UPDATE esquema_caja.turnos_caja SET estado='CERRADO',fecha_cierre=NOW(),monto_esperado=?,monto_real=?,diferencia=?,motivo_cierre=? WHERE id_turno=? AND estado='ABIERTO'", esperado, real, real.subtract(esperado), motivo, turno);
        if (n == 0) return null;
        RespuestaTurnoCaja t = turnoPorId(turno);
        return new RespuestaCierreCaja(turno, t.montoInicial(), totalEfectivo(turno), esperado, real, real.subtract(esperado), "CERRADO");
    }
    public Map<String, Object> guardarCliente(String tipo, String numero, String nombre, String telefono, String correo) {
        return guardarCliente(tipo, numero, nombre, telefono, correo, null);
    }
    public Map<String, Object> guardarCliente(String tipo, String numero, String nombre, String telefono, String correo, String direccion) {
        String dir = (direccion == null || direccion.isBlank()) ? null : direccion.trim();
        jdbc.update("INSERT INTO esquema_caja.clientes(tipo_documento,numero_documento,nombre_completo,telefono,correo,direccion) VALUES(?,?,?,?,?,?) ON CONFLICT(tipo_documento,numero_documento) DO UPDATE SET nombre_completo=EXCLUDED.nombre_completo,telefono=COALESCE(EXCLUDED.telefono,esquema_caja.clientes.telefono),correo=COALESCE(EXCLUDED.correo,esquema_caja.clientes.correo),direccion=COALESCE(EXCLUDED.direccion,esquema_caja.clientes.direccion),fecha_actualizacion=NOW()", tipo.toUpperCase(Locale.ROOT), numero, nombre, telefono, correo, dir);
        return buscarCliente(tipo, numero);
    }
    public Map<String, Object> buscarCliente(String tipo, String numero) {
        return jdbc.query("SELECT id_cliente,tipo_documento,numero_documento,nombre_completo,direccion,telefono,correo,fecha_actualizacion FROM esquema_caja.clientes WHERE tipo_documento=? AND numero_documento=?", rs -> rs.next() ? new LinkedHashMap<String, Object>() {{
            put("idCliente", rs.getLong("id_cliente"));
            put("tipoDocumento", rs.getString("tipo_documento"));
            put("numeroDocumento", rs.getString("numero_documento"));
            put("nombreCompleto", rs.getString("nombre_completo"));
            put("direccion", rs.getString("direccion"));
            put("telefono", rs.getString("telefono"));
            put("correo", rs.getString("correo"));
            put("fechaActualizacion", rs.getObject("fecha_actualizacion", java.time.OffsetDateTime.class));
        }} : null, tipo.toUpperCase(Locale.ROOT), numero);
    }
    public Long guardarTicket(Long pedido, String telefono, String contenido) {
        return jdbc.queryForObject("INSERT INTO esquema_caja.tickets_whatsapp(id_pedido,telefono,contenido) VALUES(?,?,?) RETURNING id_ticket", Long.class, pedido, telefono, contenido);
    }
    public int actualizarEstadoTicket(Long id, String estado, String respuesta) {
        return jdbc.update("UPDATE esquema_caja.tickets_whatsapp SET estado=?,respuesta_proveedor=?,fecha_envio=CASE WHEN ?='ENVIADO' THEN NOW() ELSE fecha_envio END WHERE id_ticket=?", estado, respuesta, estado, id);
    }
    public Map<String, Object> ticket(Long id) {
        return jdbc.query("SELECT * FROM esquema_caja.tickets_whatsapp WHERE id_ticket=?", rs -> rs.next() ? Map.of("idTicket", rs.getLong("id_ticket"), "idPedido", rs.getLong("id_pedido"), "telefono", rs.getString("telefono"), "contenido", rs.getString("contenido"), "estado", rs.getString("estado"), "fechaCreacion", rs.getObject("fecha_creacion", java.time.OffsetDateTime.class)) : null, id);
    }

    // ==========================================
    // MÉTODOS PARA COMPROBANTES ELECTRÓNICOS SUNAT
    // ==========================================
    public void bloquearPago(Long idPago) {
        jdbc.queryForList("SELECT id_pago FROM esquema_caja.pagos WHERE id_pago=? FOR UPDATE", idPago);
    }

    public Long siguienteCorrelativo(String tipoComprobante) {
        String sql = "FACTURA".equalsIgnoreCase(tipoComprobante)
                ? "SELECT nextval('esquema_caja.seq_correlativo_factura')"
                : "SELECT nextval('esquema_caja.seq_correlativo_boleta')";
        return jdbc.queryForObject(sql, Long.class);
    }

    public Long insertarComprobante(Long idPago, Long idPedido, String tipoComprobante, String serie, Long correlativo,
                                    String tipoDocRec, String numDocRec, String denominacion, String direccion,
                                    BigDecimal gravado, BigDecimal igv, BigDecimal total, String metodoPago,
                                    String hashCpe, String codCdr, String descCdr, String estadoSunat, String cadenaQr, String xml) {
        return jdbc.queryForObject("INSERT INTO esquema_caja.comprobantes_sunat(" +
                "id_pago, id_pedido, tipo_comprobante, serie, correlativo, tipo_documento_receptor, numero_documento_receptor, " +
                "denominacion_receptor, direccion_receptor, total_gravado, total_igv, total_importe, metodo_pago, " +
                "hash_cpe, codigo_cdr, descripcion_cdr, estado_sunat, cadena_qr, xml_firmado) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id_comprobante",
                Long.class, idPago, idPedido, tipoComprobante.toUpperCase(Locale.ROOT), serie, correlativo, tipoDocRec, numDocRec,
                denominacion, direccion, gravado, igv, total, metodoPago, hashCpe, codCdr, descCdr, estadoSunat, cadenaQr, xml);
    }

    public RespuestaComprobanteSunat buscarComprobantePorId(Long idComprobante) {
        return jdbc.query("SELECT * FROM esquema_caja.comprobantes_sunat WHERE id_comprobante=?", rs -> rs.next() ? mapearComprobante(rs) : null, idComprobante);
    }

    public RespuestaComprobanteSunat buscarComprobantePorPago(Long idPago) {
        return jdbc.query("SELECT * FROM esquema_caja.comprobantes_sunat WHERE id_pago=? ORDER BY id_comprobante DESC LIMIT 1", rs -> rs.next() ? mapearComprobante(rs) : null, idPago);
    }

    public RespuestaComprobanteSunat buscarComprobantePorPedido(Long idPedido) {
        return jdbc.query("SELECT * FROM esquema_caja.comprobantes_sunat WHERE id_pedido=? ORDER BY id_comprobante DESC LIMIT 1", rs -> rs.next() ? mapearComprobante(rs) : null, idPedido);
    }

    public void actualizarEmision(Long id, pe.upeu.nullpointerpos.caja.sunat.ServicioSunatBeta.ResultadoEmisionXml resultado) {
        jdbc.update("UPDATE esquema_caja.comprobantes_sunat SET xml_firmado=?, hash_cpe=?, cadena_qr=?, codigo_cdr=?, descripcion_cdr=?, estado_sunat=? WHERE id_comprobante=?",
            resultado.xmlFirmado(), resultado.hashCpe(), resultado.cadenaQr(), resultado.codigoCdr(), resultado.descripcionCdr(), resultado.estadoSunat(), id);
    }

    public String obtenerXmlComprobante(Long idComprobante) {
        return jdbc.query("SELECT xml_firmado FROM esquema_caja.comprobantes_sunat WHERE id_comprobante=?", rs -> rs.next() ? rs.getString("xml_firmado") : null, idComprobante);
    }

    private RespuestaComprobanteSunat mapearComprobante(java.sql.ResultSet rs) throws java.sql.SQLException {
        String serie = rs.getString("serie");
        long corr = rs.getLong("correlativo");
        String completo = serie + "-" + String.format(Locale.ROOT, "%08d", corr);
        long idComp = rs.getLong("id_comprobante");
        return new RespuestaComprobanteSunat(
                idComp,
                rs.getLong("id_pedido"),
                rs.getLong("id_pago"),
                rs.getString("tipo_comprobante"),
                serie,
                corr,
                completo,
                rs.getObject("fecha_emision", java.time.OffsetDateTime.class),
                configuracionSunat.getRucEmisor(),
                configuracionSunat.getRazonSocial(),
                rs.getString("tipo_documento_receptor"),
                rs.getString("numero_documento_receptor"),
                rs.getString("denominacion_receptor"),
                rs.getString("direccion_receptor"),
                rs.getBigDecimal("total_gravado"),
                rs.getBigDecimal("total_igv"),
                rs.getBigDecimal("total_importe"),
                rs.getString("moneda"),
                rs.getString("metodo_pago"),
                rs.getString("hash_cpe"),
                rs.getString("codigo_cdr"),
                rs.getString("descripcion_cdr"),
                rs.getString("estado_sunat"),
                rs.getString("cadena_qr"),
                "/api/v1/comprobantes/" + idComp + "/pdf",
                "/api/v1/comprobantes/" + idComp + "/xml"
        );
    }
}
