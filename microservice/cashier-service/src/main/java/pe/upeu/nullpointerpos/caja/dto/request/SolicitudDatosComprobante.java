package pe.upeu.nullpointerpos.caja.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-60 START]
 * Solicitud local para preparar los datos del comprador antes de emitir un comprobante.
 * No llama a proveedores externos ni emite un documento tributario real.
 */
public record SolicitudDatosComprobante(
        @Pattern(regexp = "(?i)^(BOLETA|FACTURA)$", message = "El tipo debe ser BOLETA o FACTURA.") String tipoComprobante,
        @Pattern(regexp = "(?i)^(SIN_DATOS|MANUAL|DNI|RUC)$", message = "Modalidad no válida.") String modalidad,
        @Pattern(regexp = "(?i)^(DNI|RUC)?$", message = "Tipo de documento no válido.") String tipoDocumento,
        @Pattern(regexp = "^$|^[0-9]{8}$|^[0-9]{11}$", message = "El documento debe tener 8 u 11 dígitos.") String numeroDocumento,
        @Size(max = 160) String nombreCompleto,
        @Size(max = 200) String direccion,
        @Pattern(regexp = "^$|^\\+?[0-9]{9,15}$", message = "Teléfono no válido.") String telefono,
        @Pattern(regexp = "^$|^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+$", message = "Correo no válido.") @Size(max = 160) String correo
) {}
/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-60 END] */
