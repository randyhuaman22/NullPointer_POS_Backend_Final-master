package pe.upeu.nullpointerpos.productos.dto.response;

import java.util.List;

public record RespuestaGrupoOpcionesProducto(Long idGrupoOpcion, String nombre, boolean obligatorio,
                                              Integer seleccionMinima, Integer seleccionMaxima,
                                              Integer ordenVisualizacion,
                                              List<RespuestaOpcionProducto> opciones) { }
