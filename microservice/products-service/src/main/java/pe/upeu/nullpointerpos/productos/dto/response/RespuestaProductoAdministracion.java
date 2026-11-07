package pe.upeu.nullpointerpos.productos.dto.response;

import java.math.BigDecimal;

public record RespuestaProductoAdministracion(
        Long idProducto,
        Long idCategoria,
        String categoria,
        String areaNegocio,
        Long idEstacionCocina,
        String estacionCocina,
        String codigoProducto,
        String nombre,
        String descripcion,
        String urlImagen,
        BigDecimal precioUnitario,
        boolean disponible,
        boolean activo,
        Integer tiempoPreparacionMinutos,
        boolean destacado,
        Integer ordenVisualizacion) { }
