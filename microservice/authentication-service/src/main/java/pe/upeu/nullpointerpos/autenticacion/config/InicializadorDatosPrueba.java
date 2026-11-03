package pe.upeu.nullpointerpos.autenticacion.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class InicializadorDatosPrueba {
    @Bean
    CommandLineRunner inicializarUsuarios(JdbcTemplate jdbc, PasswordEncoder codificador) {
        return args -> {
            asegurarUsuario(jdbc, codificador, "ADM001", "Administrador de prueba", "ADMINISTRADOR");
            asegurarUsuario(jdbc, codificador, "MOZ001", "Mozo de prueba", "MOZO");
            asegurarUsuario(jdbc, codificador, "MOZ002", "Mozo de prueba 2", "MOZO");
            asegurarUsuario(jdbc, codificador, "COC001", "Cocinero de prueba", "COCINERO");
            asegurarUsuario(jdbc, codificador, "CAJ001", "Cajero de prueba", "CAJERO");
            asegurarUsuario(jdbc, codificador, "ALM001", "Almacenero de prueba", "ALMACENERO");
        };
    }

    private void asegurarUsuario(JdbcTemplate jdbc, PasswordEncoder codificador,
                                 String codigo, String nombre, String rol) {
        Integer cantidad = jdbc.queryForObject(
                "SELECT COUNT(*) FROM esquema_autenticacion.usuarios WHERE codigo_trabajador = ?",
                Integer.class, codigo);
        if (cantidad != null && cantidad > 0) return;
        int insertados = jdbc.update(
                "INSERT INTO esquema_autenticacion.usuarios(id_rol, codigo_trabajador, nombre_completo, hash_clave) " +
                "SELECT id_rol, ?, ?, ? FROM esquema_autenticacion.roles WHERE nombre = ? AND activo = TRUE",
                codigo, nombre, codificador.encode("1234"), rol);
        if (insertados == 0) throw new IllegalStateException("No se pudo crear el usuario de prueba " + codigo);
    }
}
