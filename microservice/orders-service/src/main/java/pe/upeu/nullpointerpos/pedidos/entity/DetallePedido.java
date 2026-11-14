package pe.upeu.nullpointerpos.pedidos.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** T-20: la estación se conserva como snapshot para que el KDS pueda filtrar sin consultar otra BD. */
public record DetallePedido(Long idDetallePedido, Long idProducto, String nombreProducto, Integer cantidad,
                            BigDecimal precioUnitario, String observaciones, BigDecimal totalLinea,
                            String estado, String estacionCocina, OffsetDateTime fechaFinalizacion) { }
