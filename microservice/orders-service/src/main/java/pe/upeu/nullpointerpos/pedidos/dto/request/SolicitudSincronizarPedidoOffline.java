package pe.upeu.nullpointerpos.pedidos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

/** Solicitud atómica e idempotente para sincronizar un pedido creado sin conexión al volver la red. */
public record SolicitudSincronizarPedidoOffline(
        @NotNull(message = "idMesa es obligatorio") Long idMesa,
        @Size(max = 1000, message = "observaciones no puede superar 1000 caracteres") String observaciones,
        @NotEmpty(message = "El pedido debe incluir al menos un producto") List<@Valid SolicitudDetallePedido> detalles,
        @NotBlank(message = "idempotencia es obligatoria para sincronizar") @Size(max = 100, message = "idempotencia no puede superar 100 caracteres") String idempotencia,
        Boolean enviarACocina,
        OffsetDateTime fechaRegistroOffline) { }
