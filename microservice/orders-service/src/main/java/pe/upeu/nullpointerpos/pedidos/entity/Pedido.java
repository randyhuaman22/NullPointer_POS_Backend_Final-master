package pe.upeu.nullpointerpos.pedidos.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Modelo interno del pedido. HU-17: conserva subtotal, descuento y total calculado en el backend. */
public record Pedido(Long idPedido, String codigoPedido, Long idMesa, String codigoMesa,
                     Long idUsuarioMozo, String codigoMozo, String estado, String observaciones,
                     BigDecimal subtotal, BigDecimal totalDescuento, BigDecimal importeTotal,
                     OffsetDateTime fechaCreacion, OffsetDateTime fechaEnvio, OffsetDateTime fechaPago) { }
