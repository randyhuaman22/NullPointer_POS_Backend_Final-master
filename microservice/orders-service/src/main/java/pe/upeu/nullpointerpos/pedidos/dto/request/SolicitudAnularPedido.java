package pe.upeu.nullpointerpos.pedidos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HU-29: se exige causa para auditar la anulación. */
public record SolicitudAnularPedido(@NotBlank @Size(max=500) String motivo) { }
