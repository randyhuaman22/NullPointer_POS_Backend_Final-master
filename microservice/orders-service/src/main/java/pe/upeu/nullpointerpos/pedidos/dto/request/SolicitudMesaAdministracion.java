package pe.upeu.nullpointerpos.pedidos.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudMesaAdministracion(
        @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9_-]+") String codigoMesa,
        @NotBlank @Size(max = 60) String nombreArea,
        @NotNull @Min(1) @Max(50) Integer capacidad) { }
