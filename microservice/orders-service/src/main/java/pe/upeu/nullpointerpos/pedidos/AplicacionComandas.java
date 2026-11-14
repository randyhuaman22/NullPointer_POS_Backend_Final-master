package pe.upeu.nullpointerpos.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AplicacionComandas {
    public static void main(String[] args) {
        SpringApplication.run(AplicacionComandas.class, args);
    }
}
