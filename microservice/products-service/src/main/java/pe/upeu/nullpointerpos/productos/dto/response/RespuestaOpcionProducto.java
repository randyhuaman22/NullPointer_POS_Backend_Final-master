package pe.upeu.nullpointerpos.productos.dto.response;

import java.math.BigDecimal;

public record RespuestaOpcionProducto(Long idOpcion, String nombre, BigDecimal precioAdicional,
                                      Integer ordenVisualizacion) { }
