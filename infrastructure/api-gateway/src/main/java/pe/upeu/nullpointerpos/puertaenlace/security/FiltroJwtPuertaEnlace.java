package pe.upeu.nullpointerpos.puertaenlace.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class FiltroJwtPuertaEnlace implements GlobalFilter, Ordered {
    private final SecretKey claveFirma;

    public FiltroJwtPuertaEnlace(@Value("${app.jwt.secreto}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("SECRETO_JWT debe tener al menos 32 bytes.");
        }
        this.claveFirma = Keys.hmacShaKeyFor(bytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange intercambio, GatewayFilterChain cadena) {
        String ruta = intercambio.getRequest().getURI().getPath();
        // Las solicitudes previas de CORS no llevan token; deben poder llegar al manejo de CORS.
        if (intercambio.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return cadena.filter(intercambio);
        }
        if (ruta.equals("/api/v1/salud")
                || ruta.equals("/actuator/health")
                || ruta.startsWith("/actuator/health/")
                || ruta.equals("/api/v1/autenticacion/iniciar-sesion")) {
            return cadena.filter(intercambio);
        }

        String encabezado = intercambio.getRequest().getHeaders().getFirst("Authorization");
        String token = null;
        if (encabezado != null && encabezado.startsWith("Bearer ")) {
            token = encabezado.substring(7).trim();
        }
        // Algunos clientes WebSocket no permiten enviar Authorization durante el handshake.
        if ((token == null || token.isBlank()) && ruta.startsWith("/ws/cocina")) {
            token = intercambio.getRequest().getQueryParams().getFirst("token");
        }
        if (token == null || token.isBlank()) {
            return responder(intercambio, HttpStatus.UNAUTHORIZED,
                    "Token ausente. Inicia sesión y envía Authorization: Bearer TOKEN.");
        }

        try {
            Claims datos = Jwts.parser().verifyWith(claveFirma).build().parseSignedClaims(token).getPayload();
            if (ruta.startsWith("/ws/cocina")) {
                String rol = datos.get("rol", String.class);
                if (!"COCINERO".equals(rol) && !"ADMINISTRADOR".equals(rol)) {
                    return responder(intercambio, HttpStatus.FORBIDDEN,
                            "Solo el cocinero o el administrador puede abrir la pantalla de cocina.");
                }
            }
            return cadena.filter(intercambio);
        } catch (JwtException | IllegalArgumentException ex) {
            return responder(intercambio, HttpStatus.UNAUTHORIZED, "El token es inválido o ha vencido.");
        }
    }

    private Mono<Void> responder(ServerWebExchange intercambio, HttpStatus estado, String mensaje) {
        intercambio.getResponse().setStatusCode(estado);
        intercambio.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] cuerpo = ("{\"estado\":" + estado.value() + ",\"mensaje\":\"" + mensaje + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return intercambio.getResponse().writeWith(Mono.just(intercambio.getResponse().bufferFactory().wrap(cuerpo)));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
