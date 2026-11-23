package pe.upeu.nullpointerpos.caja.dto.response;

/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-60 START] Respuesta normalizada para la pantalla de caja. */
public record RespuestaDatosComprobante(
        String tipoComprobante, String modalidad, boolean requiereDatosCliente,
        String tipoDocumento, String numeroDocumento, String nombreCompleto,
        String direccion, String telefono, String correo, String origenDatos, String mensaje
) {}
/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-60 END] */
