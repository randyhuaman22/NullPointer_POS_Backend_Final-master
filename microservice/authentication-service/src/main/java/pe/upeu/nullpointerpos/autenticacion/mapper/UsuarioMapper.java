package pe.upeu.nullpointerpos.autenticacion.mapper;

import pe.upeu.nullpointerpos.autenticacion.dto.response.RespuestaUsuario;
import pe.upeu.nullpointerpos.autenticacion.entity.Usuario;
import org.springframework.stereotype.Component;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class UsuarioMapper {
    public Usuario desdeFila(ResultSet rs) throws SQLException {
        return new Usuario(rs.getLong("id_usuario"), rs.getString("codigo_trabajador"),
                rs.getString("nombre_completo"), rs.getString("correo"), rs.getString("hash_clave"),
                rs.getBoolean("activo"), rs.getString("nombre_rol"),
                rs.getObject("fecha_creacion", java.time.OffsetDateTime.class));
    }

    public RespuestaUsuario aRespuesta(Usuario usuario) {
        return new RespuestaUsuario(usuario.idUsuario(), usuario.codigoTrabajador(), usuario.nombreCompleto(),
                usuario.correo(), usuario.rol(), usuario.activo(), usuario.fechaCreacion());
    }
}
