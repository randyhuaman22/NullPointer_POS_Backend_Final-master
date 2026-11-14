package pe.upeu.nullpointerpos.productos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/** Valida el JWT usando el mismo secreto del servicio de autenticación, sin consultar su base de datos. */
@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {
    private final ServicioJwt servicioJwt;
    public FiltroAutenticacionJwt(ServicioJwt servicioJwt) { this.servicioJwt = servicioJwt; }

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
            String rol = datos.get("rol", String.class);
            Object id = datos.get("idUsuario");
            if (codigo == null || rol == null || id == null) throw new IllegalArgumentException("JWT incompleto.");
            var autenticacion = new UsernamePasswordAuthenticationToken(
                    codigo, String.valueOf(id), List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
            autenticacion.setDetails(datos);
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
