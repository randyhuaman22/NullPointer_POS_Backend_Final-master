package pe.upeu.nullpointerpos.pedidos.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** [HU-08 / HU-09 / T-07 START] Respuesta plana para el KDS con datos del pedido y su demora. */
public record RespuestaPedidoCocina(
        Long idPedido, String codigoPedido, Long idMesa, String codigoMesa,
        Long idUsuarioMozo, String codigoMozo, String estado, String observaciones,
        BigDecimal subtotal, BigDecimal totalDescuento, BigDecimal importeTotal,
        OffsetDateTime fechaCreacion, OffsetDateTime fechaEnvio, OffsetDateTime fechaPago,
        List<RespuestaDetallePedido> detalles,
        long segundosTranscurridos, long minutosTranscurridos,
        String nivelDemora, Integer siguienteUmbralMinutos) { }
/** [HU-08 / HU-09 / T-07 END] */
