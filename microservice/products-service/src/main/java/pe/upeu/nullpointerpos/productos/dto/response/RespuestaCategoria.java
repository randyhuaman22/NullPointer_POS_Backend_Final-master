package pe.upeu.nullpointerpos.productos.dto.response;

public record RespuestaCategoria(Long idCategoria, String nombre, String descripcion,
                                  Integer ordenVisualizacion, String areaNegocio,
                                  Long idCategoriaPadre) { }
