package pe.upeu.nullpointerpos.pedidos.dto.response;
public record RespuestaMesa(Long idMesa, String codigoMesa, String nombreArea, Integer capacidad,
                            String estado, boolean activo) { }
