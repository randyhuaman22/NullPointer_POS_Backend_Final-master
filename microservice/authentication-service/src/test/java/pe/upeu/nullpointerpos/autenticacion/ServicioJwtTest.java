package pe.upeu.nullpointerpos.autenticacion;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import pe.upeu.nullpointerpos.autenticacion.security.ServicioJwt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicioJwtTest {
    private static final String SECRETO = "NullPointerPOS-Test-Secret-Must-Be-Long-Enough-2026!";

    @Test
    void emiteTokenFirmadoConCodigoTrabajadorYRol() {
        ServicioJwt servicio = new ServicioJwt(SECRETO, 60);
        String token = servicio.emitir(7L, "MOZ001", "Mozo de prueba", "MOZO");

        Claims datos = servicio.leer(token);

        assertThat(datos.getSubject()).isEqualTo("MOZ001");
        assertThat(datos.get("rol", String.class)).isEqualTo("MOZO");
        assertThat(datos.get("idUsuario")).isNotNull();
        assertThat(datos.get("nombreCompleto", String.class)).isEqualTo("Mozo de prueba");
    }

    @Test
    void rechazaTokenVencido() throws InterruptedException {
        ServicioJwt servicio = new ServicioJwt(SECRETO, 0);
        String token = servicio.emitir(7L, "MOZ001", "Mozo de prueba", "MOZO");
        Thread.sleep(25);

        assertThatThrownBy(() -> servicio.leer(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void verificaClaveConBCryptSinGuardarTextoPlano() {
        BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(4);
        String hash = codificador.encode("1234");

        assertThat(hash).isNotEqualTo("1234");
        assertThat(codificador.matches("1234", hash)).isTrue();
        assertThat(codificador.matches("9999", hash)).isFalse();
    }
}
