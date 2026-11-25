package pe.upeu.nullpointerpos.pedidos.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudTrasladoMesa(
        @NotNull(message = "idPedido es obligatorio") Long idPedido,
        @NotNull(message = "idMesaDestino es obligatorio") Long idMesaDestino,
        @Size(max = 250, message = "motivo no puede superar 250 caracteres") String motivo) { }
