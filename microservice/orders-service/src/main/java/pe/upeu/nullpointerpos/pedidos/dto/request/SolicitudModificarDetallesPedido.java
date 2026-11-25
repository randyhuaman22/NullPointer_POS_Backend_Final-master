package pe.upeu.nullpointerpos.pedidos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SolicitudModificarDetallesPedido(
        @NotEmpty(message = "El pedido debe conservar al menos un producto") List<@Valid SolicitudDetallePedido> detalles) { }
