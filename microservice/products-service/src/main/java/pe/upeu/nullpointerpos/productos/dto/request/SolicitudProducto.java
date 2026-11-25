package pe.upeu.nullpointerpos.productos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SolicitudProducto(
        @Size(max = 40) String codigoProducto,
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 4000) String descripcion,
        @Size(max = 2000) String urlImagen,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precioUnitario,
        @NotNull @Positive Long idCategoria,
        @NotNull @Positive Long idEstacionCocina,
        @Min(0) @Max(240) Integer tiempoPreparacionMinutos,
        Boolean disponible,
        Boolean destacado,
        @Min(0) Integer ordenVisualizacion) { }
