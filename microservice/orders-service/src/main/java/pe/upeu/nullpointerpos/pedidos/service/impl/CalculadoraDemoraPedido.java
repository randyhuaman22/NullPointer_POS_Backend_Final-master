package pe.upeu.nullpointerpos.pedidos.service.impl;

import java.time.Duration;
import java.time.OffsetDateTime;

/** [HU-09 / T-11 / T-14 START] Reglas de demora desacopladas de Spring para pruebas de frontera. */
public final class CalculadoraDemoraPedido {
    private CalculadoraDemoraPedido() { }

    public static Resultado calcular(OffsetDateTime fechaBase, OffsetDateTime ahora, boolean listo) {
        if (fechaBase == null || ahora == null) throw new IllegalArgumentException("Las fechas son obligatorias.");
        long segundos = Math.max(0, Duration.between(fechaBase, ahora).getSeconds());
        long minutos = segundos / 60;
        if (listo) return new Resultado(segundos, minutos, "LISTO", null);
        if (minutos >= 15) return new Resultado(segundos, minutos, "CRITICA", null);
        if (minutos >= 10) return new Resultado(segundos, minutos, "PREVENTIVA", 15);
        return new Resultado(segundos, minutos, "NORMAL", 10);
    }

    public record Resultado(long segundosTranscurridos, long minutosTranscurridos,
                            String nivelDemora, Integer siguienteUmbralMinutos) { }
}
/** [HU-09 / T-11 / T-14 END] */
