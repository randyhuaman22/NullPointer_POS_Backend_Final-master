package pe.upeu.nullpointerpos.caja.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Parte individual para pagos mixtos en caja (EFECTIVO, YAPE, PLIN o TARJETA). */
public record PartePago(
    @NotBlank @Pattern(regexp="(?i)^(EFECTIVO|YAPE|PLIN|TARJETA)$", message="Método debe ser EFECTIVO, YAPE, PLIN o TARJETA.") String metodo,
    @NotNull @DecimalMin(value="0.01", message="El importe debe ser mayor que cero.") BigDecimal importe,
    @Size(max=100) String numeroOperacion,
    @Pattern(regexp="^$|^[0-9]{4}$", message="Los últimos dígitos deben ser 4 números o dejarse vacío para efectivo.") String ultimosCuatroDigitos
){}
