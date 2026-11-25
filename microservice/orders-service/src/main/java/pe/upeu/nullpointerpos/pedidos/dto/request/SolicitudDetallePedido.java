package pe.upeu.nullpointerpos.pedidos.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;

public record SolicitudDetallePedido(
        @NotNull(message = "idProducto es obligatorio") Long idProducto,
        @NotNull(message = "cantidad es obligatoria") @Min(value = 1, message = "La cantidad debe ser al menos 1") Integer cantidad,
        @Size(max = 500, message = "observaciones no puede superar 500 caracteres") String observaciones,
        List<@NotNull @Valid SolicitudSeleccionOpcionProducto> opciones) { }
