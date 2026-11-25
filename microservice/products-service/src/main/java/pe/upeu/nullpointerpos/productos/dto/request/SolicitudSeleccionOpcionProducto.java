package pe.upeu.nullpointerpos.productos.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SolicitudSeleccionOpcionProducto(
        @NotNull @Positive Long idOpcion,
        @NotNull @Min(1) @Max(10) Integer cantidad) { }
