package pe.upeu.nullpointerpos.pedidos.repository;

import pe.upeu.nullpointerpos.pedidos.entity.EventoPendiente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class EventoPendienteRepositorio {
    private final JdbcTemplate jdbc;
    public EventoPendienteRepositorio(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<EventoPendiente> buscarPendientesBloqueando() {
        return jdbc.query("SELECT id_evento, tipo_evento, contenido_evento::text AS contenido_evento " +
                "FROM esquema_comandas.eventos_pendientes WHERE fecha_publicacion IS NULL " +
                "ORDER BY id_evento LIMIT 50 FOR UPDATE SKIP LOCKED",
                (rs, fila) -> new EventoPendiente(rs.getLong("id_evento"), rs.getString("tipo_evento"), rs.getString("contenido_evento")));
    }
    public void marcarPublicado(Long idEvento) {
        jdbc.update("UPDATE esquema_comandas.eventos_pendientes SET fecha_publicacion = NOW() " +
                "WHERE id_evento = ? AND fecha_publicacion IS NULL", idEvento);
    }
}
