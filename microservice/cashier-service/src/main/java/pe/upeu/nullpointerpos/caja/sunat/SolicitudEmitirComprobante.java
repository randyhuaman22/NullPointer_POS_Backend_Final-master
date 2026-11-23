package pe.upeu.nullpointerpos.caja.sunat;

import jakarta.validation.constraints.*;

/** Solicitud para emitir formalmente la Boleta o Factura Electrónica hacia SUNAT Beta. */
public record SolicitudEmitirComprobante(
    @NotNull(message="Debe especificar el ID del pedido.") Long idPedido,
    @NotNull(message="Debe especificar el ID del pago confirmado.") Long idPago,
    @NotBlank @Pattern(regexp="(?i)^(BOLETA|FACTURA)$", message="Tipo de comprobante debe ser BOLETA o FACTURA.") String tipoComprobante,
    @NotBlank @Pattern(regexp="(?i)^(SIN_DATOS|MANUAL|DNI|RUC)$", message="Modalidad debe ser SIN_DATOS, MANUAL, DNI o RUC.") String modalidad,
    String tipoDocumento,
    String numeroDocumento,
    String nombreCompleto,
    String direccion,
    String correo,
    String telefono
){}
