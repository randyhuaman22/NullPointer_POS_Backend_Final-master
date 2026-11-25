package pe.upeu.nullpointerpos.caja.dto.request;

import jakarta.validation.constraints.*;

/** Solicitud para cobros digitales: YAPE, PLIN o TARJETA (POS / Simulador). */
public record SolicitudCobroDigital(
    @NotNull Long idPedido,
    @NotBlank @Pattern(regexp="(?i)^(YAPE|PLIN|TARJETA)$", message="El método debe ser YAPE, PLIN o TARJETA.") String metodo,
    @NotBlank @Size(max=100) String numeroOperacion,
    @NotBlank @Pattern(regexp="^[0-9]{4}$", message="Debe ingresar exactamente los últimos 4 dígitos.") String ultimosCuatroDigitos,
    @NotBlank @Size(max=100) String idempotencia
){}
