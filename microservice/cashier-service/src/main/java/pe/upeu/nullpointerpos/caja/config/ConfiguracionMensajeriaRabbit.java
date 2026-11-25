package pe.upeu.nullpointerpos.caja.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** [T-37 / T-43 / T-47 START] Contrato JSON común para publicar la confirmación de pagos a RabbitMQ. */
@Configuration
public class ConfiguracionMensajeriaRabbit {
    @Bean TopicExchange intercambioEventos() { return new TopicExchange("nullpointer.eventos", true, false); }
    @Bean Jackson2JsonMessageConverter convertidorJsonRabbit() { return new Jackson2JsonMessageConverter(); }
}
/** [T-37 / T-43 / T-47 END] */
