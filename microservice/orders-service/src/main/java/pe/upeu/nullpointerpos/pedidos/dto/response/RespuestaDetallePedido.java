package pe.upeu.nullpointerpos.pedidos.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RespuestaDetallePedido(Long idDetallePedido, Long idProducto, String nombreProducto,
                                     Integer cantidad, BigDecimal precioUnitario, String observaciones,
                                     BigDecimal totalLinea, String estado, String estacionCocina,
                                     OffsetDateTime fechaFinalizacion) { }
