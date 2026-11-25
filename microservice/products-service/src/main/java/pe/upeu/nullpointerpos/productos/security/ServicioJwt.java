package pe.upeu.nullpointerpos.productos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class ServicioJwt {
    private final SecretKey claveFirma;

    public ServicioJwt(@Value("${app.jwt.secreto}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalArgumentException("SECRETO_JWT debe tener al menos 32 bytes.");
        this.claveFirma = Keys.hmacShaKeyFor(bytes);
    }

    public Claims leer(String token) {
        return Jwts.parser().verifyWith(claveFirma).build().parseSignedClaims(token).getPayload();
    }
}
