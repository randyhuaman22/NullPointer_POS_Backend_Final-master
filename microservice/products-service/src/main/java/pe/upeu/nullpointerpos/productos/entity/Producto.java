package pe.upeu.nullpointerpos.productos.entity;

import java.math.BigDecimal;

public record Producto(Long idProducto, String codigoProducto, String nombre, String descripcion, String urlImagen,
                       BigDecimal precioUnitario, String categoria, String estacionCocina, boolean disponible) { }
