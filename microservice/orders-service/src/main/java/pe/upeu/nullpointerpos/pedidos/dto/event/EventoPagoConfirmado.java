package pe.upeu.nullpointerpos.pedidos.dto.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EventoPagoConfirmado(Long idEvento, Long idPago, Long idPedido, BigDecimal importe,
                                   String metodo, Long idCajero, String codigoCajero,
                                   OffsetDateTime fechaConfirmacion) { }
