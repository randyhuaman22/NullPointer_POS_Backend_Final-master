package pe.upeu.nullpointerpos.pedidos.dto.response;

import java.math.BigDecimal;

/** Contrato mínimo de lectura para cashier-service; no expone la base de datos de pedidos. */
public record RespuestaResumenPago(Long idPedido, String codigoPedido, String estado,
                                   BigDecimal importeTotal, BigDecimal totalDescuento) { }
