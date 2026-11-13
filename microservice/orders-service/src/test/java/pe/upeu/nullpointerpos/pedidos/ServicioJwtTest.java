package pe.upeu.nullpointerpos.pedidos;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import pe.upeu.nullpointerpos.pedidos.security.ServicioJwt;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicioJwtTest {
    private static final String SECRETO = "NullPointerPOS-Test-Secret-Must-Be-Long-Enough-2026!";
    private final SecretKey clave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));

    @Test
    void validaTokenFirmadoYLeeSusDatos() {
        ServicioJwt servicio = new ServicioJwt(SECRETO);
        String token = Jwts.builder()
                .subject("MOZ001")
                .claim("idUsuario", 7L)
                .claim("rol", "MOZO")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(clave)
                .compact();

        Claims datos = servicio.leer(token);

        assertThat(datos.getSubject()).isEqualTo("MOZ001");
        assertThat(datos.get("idUsuario")).isNotNull();
        assertThat(datos.get("rol", String.class)).isEqualTo("MOZO");
    }

    @Test
    void rechazaTokenVencido() {
        ServicioJwt servicio = new ServicioJwt(SECRETO);
        String token = Jwts.builder()
                .subject("MOZ001")
                .claim("idUsuario", 7L)
                .claim("rol", "MOZO")
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(clave)
                .compact();

        assertThatThrownBy(() -> servicio.leer(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}
