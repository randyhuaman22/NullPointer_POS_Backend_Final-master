package pe.upeu.nullpointerpos.productos.dto.response;
import java.math.BigDecimal;
public record RespuestaProducto(Long idProducto, String codigoProducto, String nombre, String descripcion,
                                String urlImagen, BigDecimal precioUnitario, String categoria,
                                String estacionCocina, boolean disponible) { }
