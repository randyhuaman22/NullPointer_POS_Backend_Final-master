package pe.upeu.nullpointerpos.pedidos.dto.external;

import java.math.BigDecimal;

/** Respuesta pública del catálogo consumida por el servicio de pedidos. */
public record RespuestaProductoCatalogo(
        Long idProducto,
        String codigoProducto,
        String nombre,
        String descripcion,
        String urlImagen,
        BigDecimal precioUnitario,
        String categoria,
        String estacionCocina,
        boolean disponible) { }
