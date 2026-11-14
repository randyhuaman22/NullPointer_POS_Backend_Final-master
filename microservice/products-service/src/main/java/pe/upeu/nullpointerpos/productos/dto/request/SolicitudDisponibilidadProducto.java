package pe.upeu.nullpointerpos.productos.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** [T-25 START] Cambio de estado del catálogo solicitado desde cocina. */
public record SolicitudDisponibilidadProducto(@NotNull Long idProducto,
                                               @NotNull Boolean disponible,
                                               @Size(max=250) String motivo) { }
/** [T-25 END] */
