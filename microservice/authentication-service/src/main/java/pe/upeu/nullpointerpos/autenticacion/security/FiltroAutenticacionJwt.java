package pe.upeu.nullpointerpos.autenticacion.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {
    private final ServicioJwt servicioJwt;
    private final JdbcTemplate jdbc;

    public FiltroAutenticacionJwt(ServicioJwt servicioJwt, JdbcTemplate jdbc) {
        this.servicioJwt = servicioJwt;
        this.jdbc = jdbc;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String encabezado = solicitud.getHeader("Authorization");
        if (encabezado == null || !encabezado.startsWith("Bearer ")) {
            cadena.doFilter(solicitud, respuesta);
            return;
        }
        try {
            Claims datos = servicioJwt.leer(encabezado.substring(7).trim());
            String codigo = datos.getSubject();
            if (codigo == null || codigo.isBlank()) {
                cadena.doFilter(solicitud, respuesta);
                return;
            }
            // La cuenta se consulta en la base de este servicio para bloquear usuarios desactivados.
            String rol = jdbc.query(
                    "SELECT r.nombre FROM esquema_autenticacion.usuarios u " +
                    "JOIN esquema_autenticacion.roles r ON r.id_rol = u.id_rol " +
                    "WHERE u.codigo_trabajador = ? AND u.activo = TRUE AND r.activo = TRUE",
                    rs -> rs.next() ? rs.getString("nombre") : null, codigo);
            if (rol == null) {
                SecurityContextHolder.clearContext();
                respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            var autenticacion = new UsernamePasswordAuthenticationToken(
                    codigo, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            respuesta.setContentType("application/json;charset=UTF-8");
            respuesta.getWriter().write("{\"estado\":401,\"mensaje\":\"Token inválido o vencido.\"}");
            return;
        }
        cadena.doFilter(solicitud, respuesta);
    }
}
