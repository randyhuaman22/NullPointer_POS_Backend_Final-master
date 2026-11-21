package pe.upeu.nullpointerpos.caja;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class AplicacionCaja {
    public static void main(String[] args) { SpringApplication.run(AplicacionCaja.class, args); }
}
