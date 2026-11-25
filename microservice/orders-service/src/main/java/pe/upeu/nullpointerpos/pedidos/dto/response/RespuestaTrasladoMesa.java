package pe.upeu.nullpointerpos.pedidos.dto.response;

import java.time.OffsetDateTime;

public record RespuestaTrasladoMesa(Long idPedido, String codigoMesaOrigen, String codigoMesaDestino,
                                    String estado, OffsetDateTime fechaTraslado) { }
