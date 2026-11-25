package pe.upeu.nullpointerpos.productos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SolicitudOpcionProducto(
        @NotBlank @Size(max = 80) String nombre,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precioAdicional,
        @Min(0) Integer ordenVisualizacion) { }
