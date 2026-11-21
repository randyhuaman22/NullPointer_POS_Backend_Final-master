package pe.upeu.nullpointerpos.caja.security;
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
@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {
    private final ServicioJwt jwt;
    public FiltroAutenticacionJwt(ServicioJwt jwt) { this.jwt=jwt; }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String h=req.getHeader("Authorization"); if(h==null||!h.startsWith("Bearer ")){chain.doFilter(req,res);return;}
        try {
            Claims c=jwt.leer(h.substring(7).trim()); String codigo=c.getSubject(), rol=c.get("rol",String.class); Object id=c.get("idUsuario");
            if(codigo==null||rol==null||id==null) throw new IllegalArgumentException("JWT incompleto.");
            var auth=new UsernamePasswordAuthenticationToken(codigo,String.valueOf(id),List.of(new SimpleGrantedAuthority("ROLE_"+rol)));
            auth.setDetails(c); SecurityContextHolder.getContext().setAuthentication(auth);
        } catch(JwtException|IllegalArgumentException ex) {
            SecurityContextHolder.clearContext(); res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"estado\":401,\"mensaje\":\"Token inválido o vencido.\"}");return;
        }
        chain.doFilter(req,res);
    }
}
