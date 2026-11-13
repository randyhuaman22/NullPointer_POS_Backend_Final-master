package pe.upeu.nullpointerpos.pedidos.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.upeu.nullpointerpos.pedidos.config.ConfiguracionMensajeriaRabbit;
import pe.upeu.nullpointerpos.pedidos.dto.event.EventoPagoConfirmado;
import pe.upeu.nullpointerpos.pedidos.service.ServicioPedidos;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/** [T-37 START] Consumo idempotente de confirmaciones de caja; orders-service conserva la propiedad de pedidos. */
@Component
public class ReceptorPagoConfirmado {
    private final ServicioPedidos pedidos;
    private final ObjectMapper mapper;
    public ReceptorPagoConfirmado(ServicioPedidos pedidos, ObjectMapper mapper) { this.pedidos = pedidos; this.mapper = mapper; }
    @RabbitListener(queues = ConfiguracionMensajeriaRabbit.COLA_PAGO_CONFIRMADO)
    public void recibir(Map<String,Object> mensaje) {
        EventoPagoConfirmado evento = mapper.convertValue(mensaje, EventoPagoConfirmado.class);
        if (evento.idEvento() == null || evento.idPedido() == null || evento.idPago() == null
                || evento.importe() == null || evento.importe().signum() <= 0
                || evento.metodo() == null || evento.idCajero() == null) {
            throw new IllegalArgumentException("Evento de pago incompleto o con importe inválido.");
        }
        pedidos.registrarPagoConfirmado(evento);
    }
}
/** [T-37 END] */
