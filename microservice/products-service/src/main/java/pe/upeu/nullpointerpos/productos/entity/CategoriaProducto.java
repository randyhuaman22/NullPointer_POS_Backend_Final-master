package pe.upeu.nullpointerpos.productos.entity;

public record CategoriaProducto(Long idCategoria, String nombre, String descripcion,
                                 Integer ordenVisualizacion, String areaNegocio,
                                 Long idCategoriaPadre) { }
