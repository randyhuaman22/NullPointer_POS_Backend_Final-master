package pe.upeu.nullpointerpos.pedidos.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record RespuestaPedido(Long idPedido, String codigoPedido, Long idMesa, String codigoMesa,
                              Long idUsuarioMozo, String codigoMozo, String estado, String observaciones,
                              BigDecimal subtotal, BigDecimal totalDescuento, BigDecimal importeTotal,
                              OffsetDateTime fechaCreacion, OffsetDateTime fechaEnvio,
                              OffsetDateTime fechaPago, List<RespuestaDetallePedido> detalles) { }
