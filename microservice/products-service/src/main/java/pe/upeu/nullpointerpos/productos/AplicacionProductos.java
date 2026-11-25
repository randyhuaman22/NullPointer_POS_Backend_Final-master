package pe.upeu.nullpointerpos.productos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AplicacionProductos {
    public static void main(String[] args) {
        SpringApplication.run(AplicacionProductos.class, args);
    }
}
