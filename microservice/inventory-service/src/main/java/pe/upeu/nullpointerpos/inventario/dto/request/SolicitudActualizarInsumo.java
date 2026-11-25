package pe.upeu.nullpointerpos.inventario.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SolicitudActualizarInsumo(
        @Size(max = 40) String codigo,
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Size(max = 20) String unidad,
        @NotNull @DecimalMin("0.000") BigDecimal stockMinimo,
        @NotNull @DecimalMin("0.000") BigDecimal costoUnitario) { }
