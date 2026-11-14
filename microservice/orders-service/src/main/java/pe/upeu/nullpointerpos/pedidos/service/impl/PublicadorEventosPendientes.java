package pe.upeu.nullpointerpos.pedidos.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.upeu.nullpointerpos.pedidos.config.ConfiguracionMensajeriaRabbit;
import pe.upeu.nullpointerpos.pedidos.repository.EventoPendienteRepositorio;
import pe.upeu.nullpointerpos.pedidos.entity.EventoPendiente;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Publica eventos guardados en la misma transacción del pedido (Transactional Outbox). */
@Component
public class PublicadorEventosPendientes {
    private static final Logger LOG = LoggerFactory.getLogger(PublicadorEventosPendientes.class);
    private final EventoPendienteRepositorio repositorio;
    private final RabbitTemplate rabbit;
    private final ObjectMapper objectMapper;

    public PublicadorEventosPendientes(EventoPendienteRepositorio repositorio, RabbitTemplate rabbit, ObjectMapper objectMapper) {
        this.repositorio = repositorio;
        this.rabbit = rabbit;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.intervalo-ms:1000}")
    @Transactional
    public void publicarPendientes() {
        var pendientes = repositorio.buscarPendientesBloqueando();

        for (EventoPendiente pendiente : pendientes) {
            try {
                Map<String, Object> evento = objectMapper.readValue(
                        pendiente.contenido(), new TypeReference<Map<String, Object>>() { });
                evento.put("idEvento", pendiente.idEvento());
                CorrelationData confirmacion = new CorrelationData(String.valueOf(pendiente.idEvento()));
                rabbit.convertAndSend(ConfiguracionMensajeriaRabbit.INTERCAMBIO,
                        claveEnrutamiento(pendiente.tipoEvento()), evento, confirmacion);
                CorrelationData.Confirm resultado = confirmacion.getFuture().get(10, TimeUnit.SECONDS);
                if (!resultado.isAck() || confirmacion.getReturned() != null) {
                    throw new IllegalStateException("RabbitMQ no confirmó el evento: " + resultado.getReason());
                }
                repositorio.marcarPublicado(pendiente.idEvento());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LOG.warn("Publicación del evento {} interrumpida. Queda pendiente.", pendiente.idEvento());
                throw new IllegalStateException("Publicación del evento de cocina interrumpida.", ie);
            } catch (Exception ex) {
                LOG.warn("No se pudo publicar el evento {}. Queda pendiente para volver a intentar.",
                        pendiente.idEvento(), ex);
                // Al salir del método, la transacción se revierte y el evento conserva fecha_publicacion NULL.
                throw new IllegalStateException("Falló la publicación del evento de cocina.", ex);
            }
        }
    }

    private String claveEnrutamiento(String tipoEvento) {
        return switch (tipoEvento) {
            case "PEDIDO_ENVIADO_COCINA" -> "pedido.enviado.cocina";
            case "DETALLE_PLATO_ACTUALIZADO" -> "pedido.detalle.actualizado";
            case "PEDIDO_PAGADO" -> "pedido.pagado";
            case "PEDIDO_ANULADO" -> "pedido.anulado";
            case "DESCUENTO_APLICADO" -> "pedido.descuento.aplicado";
            default -> throw new IllegalArgumentException("No existe ruta RabbitMQ para tipo de evento: " + tipoEvento);
        };
    }

}
