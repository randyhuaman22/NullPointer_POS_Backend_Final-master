package pe.upeu.nullpointerpos.autenticacion.repository;

import pe.upeu.nullpointerpos.autenticacion.entity.Usuario;
import pe.upeu.nullpointerpos.autenticacion.entity.UsuarioEstado;
import pe.upeu.nullpointerpos.autenticacion.mapper.UsuarioMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/** Único punto de acceso SQL a usuarios, roles y auditoría de autenticación. */
@Repository
public class UsuarioRepositorio {
    private static final String SELECT_USUARIO = "SELECT u.id_usuario, u.codigo_trabajador, u.nombre_completo, " +
            "u.correo, u.hash_clave, u.activo, r.nombre AS nombre_rol, u.fecha_creacion " +
            "FROM esquema_autenticacion.usuarios u JOIN esquema_autenticacion.roles r ON r.id_rol = u.id_rol ";
    private final JdbcTemplate jdbc;
    private final UsuarioMapper mapper;

    public UsuarioRepositorio(JdbcTemplate jdbc, UsuarioMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public Optional<Usuario> buscarPorCodigo(String codigo) {
        return jdbc.query(SELECT_USUARIO + "WHERE u.codigo_trabajador = ?",
                rs -> rs.next() ? Optional.of(mapper.desdeFila(rs)) : Optional.empty(), codigo);
    }

    public List<Usuario> listar() {
        return jdbc.query(SELECT_USUARIO + "ORDER BY u.id_usuario", (rs, fila) -> mapper.desdeFila(rs));
    }

    public boolean existeRolActivo(String rol) {
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_autenticacion.roles " +
                "WHERE nombre = ? AND activo = TRUE", Integer.class, rol);
        return cantidad != null && cantidad > 0;
    }

    public void crear(String codigo, String nombre, String correo, String hash, String rol) {
        jdbc.update("INSERT INTO esquema_autenticacion.usuarios(id_rol, codigo_trabajador, nombre_completo, correo, hash_clave) " +
                        "SELECT id_rol, ?, ?, NULLIF(?, ''), ? FROM esquema_autenticacion.roles WHERE nombre = ?",
                codigo, nombre, correo, hash, rol);
    }

    public Long buscarIdPorCodigo(String codigo) {
        return jdbc.queryForObject("SELECT id_usuario FROM esquema_autenticacion.usuarios " +
                "WHERE codigo_trabajador = ?", Long.class, codigo);
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return jdbc.query(SELECT_USUARIO + "WHERE u.id_usuario = ?",
                rs -> rs.next() ? Optional.of(mapper.desdeFila(rs)) : Optional.empty(), id);
    }

    public Optional<UsuarioEstado> buscarEstadoBloqueando(Long id) {
        return jdbc.query("SELECT u.codigo_trabajador, r.nombre AS rol, u.activo " +
                        "FROM esquema_autenticacion.usuarios u JOIN esquema_autenticacion.roles r ON r.id_rol = u.id_rol " +
                        "WHERE u.id_usuario = ? FOR UPDATE",
                rs -> rs.next() ? Optional.of(new UsuarioEstado(rs.getString("codigo_trabajador"),
                        rs.getString("rol"), rs.getBoolean("activo"))) : Optional.empty(), id);
    }

    public void bloquearRolAdministrador() {
        jdbc.queryForObject("SELECT id_rol FROM esquema_autenticacion.roles " +
                "WHERE nombre = 'ADMINISTRADOR' FOR UPDATE", Long.class);
    }

    public int contarAdministradoresActivos() {
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_autenticacion.usuarios u " +
                "JOIN esquema_autenticacion.roles r ON r.id_rol = u.id_rol " +
                "WHERE r.nombre = 'ADMINISTRADOR' AND u.activo = TRUE", Integer.class);
        return cantidad == null ? 0 : cantidad;
    }

    public void actualizarEstado(Long id, boolean activo) {
        jdbc.update("UPDATE esquema_autenticacion.usuarios SET activo = ?, fecha_actualizacion = NOW() " +
                "WHERE id_usuario = ?", activo, id);
    }


    public void actualizarDatos(Long id, String nombre, String correo, String hashClave, String rol) {
        jdbc.update("""
                UPDATE esquema_autenticacion.usuarios u
                SET nombre_completo = ?, correo = NULLIF(?, ''),
                    hash_clave = COALESCE(?, hash_clave), id_rol = r.id_rol,
                    fecha_actualizacion = NOW()
                FROM esquema_autenticacion.roles r
                WHERE u.id_usuario = ? AND r.nombre = ? AND r.activo = TRUE
                """, nombre, correo, hashClave, id, rol);
    }

    public void registrarAuditoriaUsuario(Long idObjetivo, String actor, String accion,
                                          String nombre, String correo, String rol, boolean cambioClave) {
        jdbc.update("""
                INSERT INTO esquema_autenticacion.auditoria_usuarios
                    (id_usuario_objetivo, codigo_administrador, accion, datos)
                VALUES (?, ?, ?, jsonb_build_object('nombreCompleto', ?, 'correo', ?, 'rol', ?, 'claveRestablecida', ?))
                """, idObjetivo, actor, accion, nombre, correo, rol, cambioClave);
    }

    public List<java.util.Map<String,Object>> listarAuditoriaUsuarios(Long idUsuario) {
        if (idUsuario == null) {
            return jdbc.queryForList("SELECT * FROM esquema_autenticacion.auditoria_usuarios ORDER BY fecha_creacion DESC LIMIT 500");
        }
        return jdbc.queryForList("SELECT * FROM esquema_autenticacion.auditoria_usuarios WHERE id_usuario_objetivo=? ORDER BY fecha_creacion DESC", idUsuario);
    }

    public void registrarEvento(Long idUsuario, String codigoIntentado) {
        if (idUsuario == null) {
            jdbc.update("INSERT INTO esquema_autenticacion.eventos_autenticacion(codigo_trabajador_intentado, tipo_evento) " +
                    "VALUES (?, 'INICIO_SESION_FALLIDO')", codigoIntentado);
        } else {
            jdbc.update("INSERT INTO esquema_autenticacion.eventos_autenticacion(id_usuario, codigo_trabajador_intentado, tipo_evento) " +
                    "VALUES (?, ?, 'INICIO_SESION_CORRECTO')", idUsuario, codigoIntentado);
        }
    }
}
