package pe.upeu.nullpointerpos.productos.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionMensajeriaRabbit {
    @Bean TopicExchange intercambioEventos() { return new TopicExchange("nullpointer.eventos", true, false); }
    @Bean Jackson2JsonMessageConverter convertidorJsonRabbit() { return new Jackson2JsonMessageConverter(); }
}
