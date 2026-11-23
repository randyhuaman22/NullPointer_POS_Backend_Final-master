package pe.upeu.nullpointerpos.caja.sunat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Respuesta con los datos oficiales del comprobante aceptado por SUNAT Beta. */
public record RespuestaComprobanteSunat(
    Long idComprobante,
    Long idPedido,
    Long idPago,
    String tipoComprobante,
    String serie,
    Long correlativo,
    String numeroCompleto,
    OffsetDateTime fechaEmision,
    String rucEmisor,
    String razonSocialEmisor,
    String tipoDocumentoReceptor,
    String numeroDocumentoReceptor,
    String denominacionReceptor,
    String direccionReceptor,
    BigDecimal totalGravado,
    BigDecimal totalIgv,
    BigDecimal totalImporte,
    String moneda,
    String metodoPago,
    String hashCpe,
    String codigoCdr,
    String descripcionCdr,
    String estadoSunat,
    String cadenaQr,
    String urlPdf,
    String urlXml
){}
