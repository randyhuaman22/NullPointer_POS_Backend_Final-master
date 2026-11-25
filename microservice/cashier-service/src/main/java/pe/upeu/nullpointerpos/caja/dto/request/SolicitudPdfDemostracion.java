package pe.upeu.nullpointerpos.caja.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-64 START] Solicitud para generar una vista previa PDF sin valor tributario. */
public record SolicitudPdfDemostracion(
        @NotNull Long idPedido,
        @Pattern(regexp = "(?i)^(BOLETA|FACTURA)$") String tipoComprobante,
        @Pattern(regexp = "(?i)^(SIN_DATOS|MANUAL|DNI|RUC)$") String modalidad,
        @Pattern(regexp = "(?i)^(DNI|RUC)?$") String tipoDocumento,
        @Pattern(regexp = "^$|^[0-9]{8}$|^[0-9]{11}$") String numeroDocumento,
        @Size(max = 160) String nombreCompleto,
        @Size(max = 200) String direccion,
        @Pattern(regexp = "^$|^\\+?[0-9]{9,15}$") String telefono,
        @Pattern(regexp = "^$|^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+$") @Size(max = 160) String correo
) {}
/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-64 END] */
