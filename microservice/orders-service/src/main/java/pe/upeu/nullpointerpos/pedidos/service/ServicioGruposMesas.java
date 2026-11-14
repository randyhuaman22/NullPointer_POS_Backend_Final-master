package pe.upeu.nullpointerpos.pedidos.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudGrupoMesas;

import java.util.*;

@Service
public class ServicioGruposMesas {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate jdbcNombrado;

    public ServicioGruposMesas(JdbcTemplate jdbc) { this.jdbc = jdbc; this.jdbcNombrado = new NamedParameterJdbcTemplate(jdbc); }

    @Transactional
    public Map<String,Object> unir(SolicitudGrupoMesas solicitud, Long idActor) {
        List<Long> ids = solicitud.idsMesas().stream().distinct().sorted().toList();
        if (ids.size() < 2) throw error(HttpStatus.BAD_REQUEST, "Selecciona al menos dos mesas distintas.");
        if (ids.size() != solicitud.idsMesas().size()) throw error(HttpStatus.BAD_REQUEST, "No repitas mesas en el grupo.");
        if (!ids.contains(solicitud.idMesaPrincipal())) throw error(HttpStatus.BAD_REQUEST, "La mesa principal debe pertenecer al grupo.");

        MapSqlParameterSource parametrosIds = new MapSqlParameterSource("ids", ids);
        List<Map<String,Object>> mesas = jdbcNombrado.queryForList(
                "SELECT id_mesa, estado, activo FROM esquema_comandas.mesas WHERE id_mesa IN (:ids) ORDER BY id_mesa FOR UPDATE",
                parametrosIds);
        if (mesas.size() != ids.size()) throw error(HttpStatus.NOT_FOUND, "Una o más mesas no existen.");
        for (Map<String,Object> mesa : mesas) {
            if (!Boolean.TRUE.equals(mesa.get("activo"))) throw error(HttpStatus.CONFLICT, "No puedes unir una mesa inactiva.");
            if (!"LIBRE".equalsIgnoreCase(String.valueOf(mesa.get("estado"))))
                throw error(HttpStatus.CONFLICT, "Todas las mesas deben estar libres y habilitadas.");
        }
        Integer gruposPrevios = jdbcNombrado.queryForObject(
                "SELECT COUNT(*) FROM esquema_comandas.grupos_mesas_detalle d JOIN esquema_comandas.grupos_mesas g USING(id_grupo_mesas) WHERE g.estado='ACTIVO' AND d.id_mesa IN (:ids)",
                parametrosIds, Integer.class);
        if (gruposPrevios != null && gruposPrevios > 0)
            throw error(HttpStatus.CONFLICT, "Una o más mesas ya pertenecen a un grupo activo.");

        Long idGrupo = jdbc.queryForObject(
                "INSERT INTO esquema_comandas.grupos_mesas(id_mesa_principal,usuario_creacion) VALUES (?,?) RETURNING id_grupo_mesas",
                Long.class, solicitud.idMesaPrincipal(), idActor);
        for (Long id : ids) jdbc.update(
                "INSERT INTO esquema_comandas.grupos_mesas_detalle(id_grupo_mesas,id_mesa) VALUES (?,?)", idGrupo, id);
        jdbcNombrado.update("UPDATE esquema_comandas.mesas SET estado='OCUPADA' WHERE id_mesa IN (:ids)", parametrosIds);
        return consultar(idGrupo);
    }

    public Map<String,Object> consultar(Long idGrupo) {
        List<Map<String,Object>> grupos = jdbc.queryForList(
                "SELECT id_grupo_mesas AS \"idGrupoMesas\", id_mesa_principal AS \"idMesaPrincipal\", estado, fecha_creacion AS \"fechaCreacion\", fecha_finalizacion AS \"fechaCierre\" FROM esquema_comandas.grupos_mesas WHERE id_grupo_mesas=?",
                idGrupo);
        if (grupos.isEmpty()) throw error(HttpStatus.NOT_FOUND, "Grupo de mesas no encontrado.");
        Map<String,Object> respuesta = new LinkedHashMap<>(grupos.get(0));
        respuesta.put("mesas", jdbc.queryForList(
                "SELECT m.id_mesa AS \"idMesa\", m.codigo_mesa AS \"codigoMesa\", m.nombre_area AS \"area\", m.capacidad, m.estado FROM esquema_comandas.grupos_mesas_detalle d JOIN esquema_comandas.mesas m USING(id_mesa) WHERE d.id_grupo_mesas=? ORDER BY m.id_mesa",
                idGrupo));
        return respuesta;
    }

    @Transactional
    public Map<String,Object> finalizar(Long idGrupo) {
        Map<String,Object> grupo = consultar(idGrupo);
        if (!"ACTIVO".equalsIgnoreCase(String.valueOf(grupo.get("estado"))))
            throw error(HttpStatus.CONFLICT, "El grupo ya está finalizado.");
        Integer pedidos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM esquema_comandas.pedidos p JOIN esquema_comandas.grupos_mesas_detalle d ON d.id_mesa=p.id_mesa WHERE d.id_grupo_mesas=? AND p.estado NOT IN ('PAGADO','ANULADO')",
                Integer.class, idGrupo);
        if (pedidos != null && pedidos > 0)
            throw error(HttpStatus.CONFLICT, "No puedes separar el grupo mientras tenga pedidos activos.");
        jdbc.update("UPDATE esquema_comandas.grupos_mesas SET estado='FINALIZADO',fecha_finalizacion=NOW() WHERE id_grupo_mesas=? AND estado='ACTIVO'", idGrupo);
        jdbc.update("UPDATE esquema_comandas.mesas SET estado='LIBRE' WHERE id_mesa IN (SELECT id_mesa FROM esquema_comandas.grupos_mesas_detalle WHERE id_grupo_mesas=?) AND estado <> 'FUERA_SERVICIO'", idGrupo);
        return consultar(idGrupo);
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
