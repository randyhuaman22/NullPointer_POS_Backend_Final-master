package pe.upeu.nullpointerpos.autenticacion.exception;

import java.time.Instant;

public record ErrorApi(Instant fechaHora, int codigo, String error, String mensaje, String ruta) { }
