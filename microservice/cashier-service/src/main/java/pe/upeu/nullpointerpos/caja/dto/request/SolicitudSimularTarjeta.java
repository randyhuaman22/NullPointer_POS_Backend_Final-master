package pe.upeu.nullpointerpos.caja.dto.request;

import jakarta.validation.constraints.*;

/** Solicitud para el simulador de tarjeta de crédito/débito en caja. */
public record SolicitudSimularTarjeta(
    @NotBlank(message="Debe ingresar el número de tarjeta.") String numeroTarjeta,
    @NotNull(message="Debe ingresar el mes de vencimiento.") @Min(value=1, message="Mes inválido.") @Max(value=12, message="Mes inválido.") Integer mesVencimiento,
    @NotNull(message="Debe ingresar el año de vencimiento.") @Min(value=2024, message="Año vencido.") @Max(value=2036, message="Año fuera de rango.") Integer anioVencimiento,
    @NotBlank(message="Debe ingresar el código CVV.") @Pattern(regexp="^[0-9]{3,4}$", message="CVV debe tener 3 o 4 dígitos.") String cvv,
    @NotBlank(message="Debe ingresar el titular de la tarjeta.") String titular
){}
