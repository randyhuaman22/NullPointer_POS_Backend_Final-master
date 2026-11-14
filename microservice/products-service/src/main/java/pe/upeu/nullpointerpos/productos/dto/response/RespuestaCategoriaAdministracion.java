package pe.upeu.nullpointerpos.productos.dto.response;

public record RespuestaCategoriaAdministracion(Long idCategoria, String nombre, String descripcion,
                                                Integer ordenVisualizacion, String areaNegocio,
                                                Long idCategoriaPadre, boolean activo) { }
