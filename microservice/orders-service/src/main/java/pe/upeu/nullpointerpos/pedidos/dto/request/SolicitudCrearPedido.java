package pe.upeu.nullpointerpos.pedidos.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SolicitudCrearPedido(
        @NotNull(message = "idMesa es obligatorio") Long idMesa,
        @Size(max = 1000, message = "observaciones no puede superar 1000 caracteres") String observaciones,
        @NotEmpty(message = "El pedido debe incluir al menos un producto") List<@Valid SolicitudDetallePedido> detalles,
        @Size(max = 100, message = "idempotencia no puede superar 100 caracteres") String idempotencia) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SolicitudCrearPedido {
    }

    public SolicitudCrearPedido(Long idMesa, String observaciones, List<SolicitudDetallePedido> detalles) {
        this(idMesa, observaciones, detalles, null);
    }
}
