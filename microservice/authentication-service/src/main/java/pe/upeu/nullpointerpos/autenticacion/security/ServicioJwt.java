package pe.upeu.nullpointerpos.autenticacion.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class ServicioJwt {
    private final SecretKey claveFirma;
    private final long minutosVigencia;

    public ServicioJwt(@Value("${app.jwt.secreto}") String secreto,
                       @Value("${app.jwt.vigencia-minutos:480}") long minutosVigencia) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalArgumentException("SECRETO_JWT debe tener al menos 32 bytes.");
        this.claveFirma = Keys.hmacShaKeyFor(bytes);
        this.minutosVigencia = minutosVigencia;
    }

    public String emitir(Long idUsuario, String codigoTrabajador, String nombreCompleto, String rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(codigoTrabajador)
                .claim("idUsuario", idUsuario)
                .claim("nombreCompleto", nombreCompleto)
                .claim("rol", rol)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(minutosVigencia, ChronoUnit.MINUTES)))
                .signWith(claveFirma)
                .compact();
    }

    public Claims leer(String token) {
        return Jwts.parser().verifyWith(claveFirma).build().parseSignedClaims(token).getPayload();
    }

    public long segundosVigencia() {
        return minutosVigencia * 60L;
    }
}
