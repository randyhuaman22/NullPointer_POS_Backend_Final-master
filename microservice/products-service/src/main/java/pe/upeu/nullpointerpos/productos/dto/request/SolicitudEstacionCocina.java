package pe.upeu.nullpointerpos.productos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudEstacionCocina(@NotBlank @Size(max = 60) String nombre) { }
