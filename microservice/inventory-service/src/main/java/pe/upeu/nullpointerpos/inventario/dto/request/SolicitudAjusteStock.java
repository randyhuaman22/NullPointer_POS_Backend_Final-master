package pe.upeu.nullpointerpos.inventario.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SolicitudAjusteStock(
        @NotNull @DecimalMin("0.000") BigDecimal nuevoStock,
        @NotBlank @Size(max = 300) String motivo) { }
