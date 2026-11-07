package pe.upeu.nullpointerpos.productos.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.upeu.nullpointerpos.productos.repository.MenuRepositorio;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** [HU-12 / T-28 START] Publica outbox de disponibilidad con confirmación del broker y reintento automático. */
@Component
public class PublicadorDisponibilidadProducto {
    private static final Logger LOG = LoggerFactory.getLogger(PublicadorDisponibilidadProducto.class);
    private final MenuRepositorio repositorio;
    private final RabbitTemplate rabbit;
    private final ObjectMapper json;

    public PublicadorDisponibilidadProducto(MenuRepositorio repositorio, RabbitTemplate rabbit, ObjectMapper json) {
        this.repositorio = repositorio; this.rabbit = rabbit; this.json = json;
    }

    @Scheduled(fixedDelayString = "${app.outbox.intervalo-ms:1000}")
    public void publicarPendientes() {
        for (Map<String,Object> fila : repositorio.eventosDisponibilidadPendientes()) {
            Long id = ((Number) fila.get("id_evento")).longValue();
            try {
                Map<String,Object> evento = json.readValue((String) fila.get("contenido_evento"), new TypeReference<Map<String,Object>>() { });
                evento.put("idEvento", id);
                CorrelationData confirmacion = new CorrelationData(String.valueOf(id));
                rabbit.convertAndSend("nullpointer.eventos", "producto.disponibilidad.actualizada", evento, confirmacion);
                var resultado = confirmacion.getFuture().get(10, TimeUnit.SECONDS);
                if (!resultado.isAck() || confirmacion.getReturned() != null) {
                    throw new IllegalStateException("RabbitMQ no confirmó el evento de disponibilidad: " + resultado.getReason());
                }
                repositorio.marcarEventoDisponibilidadPublicado(id);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOG.warn("Publicación de disponibilidad {} interrumpida.", id);
                break;
            } catch (Exception ex) {
                LOG.error("No se publicó el cambio de disponibilidad {}. Se reintentará.", id, ex);
                break;
            }
        }
    }
}
/** [HU-12 / T-28 END] */
