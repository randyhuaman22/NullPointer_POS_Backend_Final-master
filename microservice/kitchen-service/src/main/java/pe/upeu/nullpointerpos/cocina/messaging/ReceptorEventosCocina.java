package pe.upeu.nullpointerpos.cocina.messaging;

import pe.upeu.nullpointerpos.cocina.config.ConfiguracionMensajeriaRabbit;
import pe.upeu.nullpointerpos.cocina.service.ServicioEventosCocina;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;

/** [T-03 / T-05 / T-19 START] Un mensaje inválido se rechaza sin bucle infinito de reintentos. */
@Component
public class ReceptorEventosCocina {
    private static final Logger LOG = LoggerFactory.getLogger(ReceptorEventosCocina.class);
    private final ServicioEventosCocina servicio;
    public ReceptorEventosCocina(ServicioEventosCocina servicio) { this.servicio = servicio; }

    @RabbitListener(queues = ConfiguracionMensajeriaRabbit.COLA_COCINA)
    public void recibir(Map<String, Object> evento) {
        try { servicio.publicarEnPantalla(evento); }
        catch (RuntimeException ex) {
            LOG.error("Se rechaza un evento inválido para KDS: {}", ex.getMessage());
            throw new AmqpRejectAndDontRequeueException("Evento de cocina inválido; revisar el log del servicio.", ex);
        }
    }
}
/** [T-03 / T-05 / T-19 END] */
