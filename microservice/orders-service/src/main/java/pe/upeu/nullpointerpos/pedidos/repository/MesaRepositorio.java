package pe.upeu.nullpointerpos.pedidos.repository;

import pe.upeu.nullpointerpos.pedidos.entity.Mesa;
import pe.upeu.nullpointerpos.pedidos.mapper.MesaMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class MesaRepositorio {
    private final JdbcTemplate jdbc;
    private final MesaMapper mapper;
    public MesaRepositorio(JdbcTemplate jdbc, MesaMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    public List<Mesa> listarActivas() {
        return jdbc.query("SELECT id_mesa, codigo_mesa, nombre_area, capacidad, estado, activo " +
                "FROM esquema_comandas.mesas WHERE activo = TRUE ORDER BY nombre_area, codigo_mesa",
                (rs, fila) -> mapper.desdeFila(rs));
    }

    public List<Mesa> listarAdministracion(boolean incluirInactivas) {
        String sql = "SELECT id_mesa, codigo_mesa, nombre_area, capacidad, estado, activo " +
                "FROM esquema_comandas.mesas " + (incluirInactivas ? "" : "WHERE activo = TRUE ") +
                "ORDER BY nombre_area, codigo_mesa";
        return jdbc.query(sql, (rs, fila) -> mapper.desdeFila(rs));
    }

    public Optional<Mesa> buscarPorId(Long id) {
        return jdbc.query("SELECT id_mesa, codigo_mesa, nombre_area, capacidad, estado, activo " +
                        "FROM esquema_comandas.mesas WHERE id_mesa = ?",
                rs -> rs.next() ? Optional.of(mapper.desdeFila(rs)) : Optional.empty(), id);
    }

    public boolean tienePedidosActivos(Long idMesa) {
        Integer cantidad = jdbc.queryForObject("""
                SELECT COUNT(*) FROM esquema_comandas.pedidos
                WHERE id_mesa = ? AND estado NOT IN ('PAGADO','ANULADO')
                """, Integer.class, idMesa);
        return cantidad != null && cantidad > 0;
    }

    public Long crearMesa(pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudMesaAdministracion solicitud, Long actor) {
        Long id = jdbc.queryForObject("""
                INSERT INTO esquema_comandas.mesas(codigo_mesa,nombre_area,capacidad,estado,activo)
                VALUES (UPPER(?),UPPER(?),?,'LIBRE',TRUE) RETURNING id_mesa
                """, Long.class, solicitud.codigoMesa().trim(), solicitud.nombreArea().trim(), solicitud.capacidad());
        registrarAuditoriaMesa(actor, "CREAR_MESA", id, solicitud.codigoMesa(), solicitud.nombreArea(), solicitud.capacidad());
        return id;
    }

    public boolean actualizarMesa(Long id, pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudMesaAdministracion solicitud, Long actor) {
        int actualizados = jdbc.update("""
                UPDATE esquema_comandas.mesas SET codigo_mesa=UPPER(?), nombre_area=UPPER(?), capacidad=?
                WHERE id_mesa=? AND activo=TRUE AND estado <> 'FUERA_SERVICIO'
                """, solicitud.codigoMesa().trim(), solicitud.nombreArea().trim(), solicitud.capacidad(), id);
        if (actualizados > 0) registrarAuditoriaMesa(actor, "MODIFICAR_MESA", id,
                solicitud.codigoMesa(), solicitud.nombreArea(), solicitud.capacidad());
        return actualizados > 0;
    }

    public boolean cambiarActivo(Long id, boolean activo, Long actor) {
        int actualizados = jdbc.update("""
                UPDATE esquema_comandas.mesas SET activo=?, estado=CASE WHEN ? THEN 'LIBRE' ELSE estado END
                WHERE id_mesa=? AND activo=?
                """, activo, activo, id, !activo);
        if (actualizados > 0) registrarAuditoriaMesa(actor, activo ? "RESTAURAR_MESA" : "DESACTIVAR_MESA", id, null, null, null);
        return actualizados > 0;
    }

    public boolean actualizarEstadoAdministrativo(Long id, String estado, Long actor) {
        int actualizados = jdbc.update("UPDATE esquema_comandas.mesas SET estado=? WHERE id_mesa=? AND activo=TRUE", estado, id);
        if (actualizados > 0) registrarAuditoriaMesa(actor, estado.equals("FUERA_SERVICIO") ? "FUERA_SERVICIO_MESA" : "HABILITAR_MESA", id, null, null, null);
        return actualizados > 0;
    }

    private void registrarAuditoriaMesa(Long actor, String accion, Long id, String codigo, String area, Integer capacidad) {
        jdbc.update("""
                INSERT INTO esquema_comandas.registros_auditoria
                    (id_usuario_actor,accion,nombre_entidad,id_entidad,valores_nuevos)
                VALUES (?,?, 'MESA', ?, jsonb_build_object('codigoMesa', ?, 'nombreArea', ?, 'capacidad', ?))
                """, actor, accion, String.valueOf(id), codigo, area, capacidad);
    }

    public Optional<Mesa> bloquearPorId(Long idMesa) {
        return jdbc.query("SELECT id_mesa, codigo_mesa, nombre_area, capacidad, estado, activo " +
                        "FROM esquema_comandas.mesas WHERE id_mesa = ? AND activo = TRUE FOR UPDATE",
                rs -> rs.next() ? Optional.of(mapper.desdeFila(rs)) : Optional.empty(), idMesa);
    }

    public boolean puedeAbrirPrimerPedidoGrupo(Long idMesaPrincipal) {
        Integer cantidad = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM esquema_comandas.grupos_mesas g
                JOIN esquema_comandas.grupos_mesas_detalle d USING(id_grupo_mesas)
                JOIN esquema_comandas.pedidos p ON p.id_mesa=d.id_mesa
                WHERE g.estado='ACTIVO' AND g.id_mesa_principal=?
                  AND p.estado NOT IN ('PAGADO','ANULADO')
                """, Integer.class, idMesaPrincipal);
        Integer esPrincipal = jdbc.queryForObject("""
                SELECT COUNT(*) FROM esquema_comandas.grupos_mesas
                WHERE estado='ACTIVO' AND id_mesa_principal=?
                """, Integer.class, idMesaPrincipal);
        return esPrincipal != null && esPrincipal > 0 && (cantidad == null || cantidad == 0);
    }

    public boolean perteneceAGrupoActivoNoPrincipal(Long idMesa) {
        Integer cantidad = jdbc.queryForObject("""
                SELECT COUNT(*) FROM esquema_comandas.grupos_mesas g
                JOIN esquema_comandas.grupos_mesas_detalle d USING(id_grupo_mesas)
                WHERE g.estado='ACTIVO' AND d.id_mesa=? AND g.id_mesa_principal<>?
                """, Integer.class, idMesa, idMesa);
        return cantidad != null && cantidad > 0;
    }

    public Optional<String> buscarCodigo(Long idMesa) {
        return jdbc.query("SELECT codigo_mesa FROM esquema_comandas.mesas WHERE id_mesa = ?",
                rs -> rs.next() ? Optional.of(rs.getString(1)) : Optional.empty(), idMesa);
    }

    /**
     * [SPRINT 1 / HU-02 / HU-06 START]
     * Mantiene el estado de la mesa consistente con los pedidos activos del propio orders-service.
     * Se ejecuta dentro de la misma transacción que crea, traslada, paga o anula el pedido.
     */
    public void actualizarEstadoSegunPedidos(Long idMesa) {
        List<Long> gruposActivos = jdbc.query(
                "SELECT g.id_grupo_mesas FROM esquema_comandas.grupos_mesas g " +
                "JOIN esquema_comandas.grupos_mesas_detalle d USING(id_grupo_mesas) " +
                "WHERE g.estado='ACTIVO' AND d.id_mesa=?",
                (rs, fila) -> rs.getLong(1), idMesa);
        if (!gruposActivos.isEmpty()) {
            Long idGrupo = gruposActivos.get(0);
            Integer pedidosActivos = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM esquema_comandas.pedidos p
                    JOIN esquema_comandas.grupos_mesas_detalle d ON d.id_mesa=p.id_mesa
                    WHERE d.id_grupo_mesas=? AND p.estado NOT IN ('PAGADO','ANULADO')
                    """, Integer.class, idGrupo);
            boolean ocupado = pedidosActivos != null && pedidosActivos > 0;
            jdbc.update("""
                    UPDATE esquema_comandas.mesas m
                    SET estado=CASE WHEN m.estado='FUERA_SERVICIO' THEN 'FUERA_SERVICIO' WHEN ? THEN 'OCUPADA' ELSE 'LIBRE' END
                    WHERE m.id_mesa IN (SELECT id_mesa FROM esquema_comandas.grupos_mesas_detalle WHERE id_grupo_mesas=?)
                    """, ocupado, idGrupo);
            if (!ocupado) jdbc.update(
                    "UPDATE esquema_comandas.grupos_mesas SET estado='FINALIZADO',fecha_finalizacion=NOW() WHERE id_grupo_mesas=? AND estado='ACTIVO'",
                    idGrupo);
            return;
        }
        jdbc.update("""
                UPDATE esquema_comandas.mesas m
                   SET estado = CASE
                       WHEN m.estado = 'FUERA_SERVICIO' THEN 'FUERA_SERVICIO'
                       WHEN EXISTS (
                           SELECT 1
                             FROM esquema_comandas.pedidos p
                            WHERE p.id_mesa = m.id_mesa
                              AND p.estado NOT IN ('PAGADO', 'ANULADO')
                       ) THEN 'OCUPADA'
                       ELSE 'LIBRE'
                   END
                 WHERE m.id_mesa = ?
                   AND m.activo = TRUE
                """, idMesa);
    }
    /** [SPRINT 1 / HU-02 / HU-06 END] */
}
