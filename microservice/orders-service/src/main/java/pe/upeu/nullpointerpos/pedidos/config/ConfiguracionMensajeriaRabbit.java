package pe.upeu.nullpointerpos.pedidos.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionMensajeriaRabbit {
    public static final String INTERCAMBIO = "nullpointer.eventos";
    public static final String COLA_COCINA = "cocina.pedido.enviado";
    public static final String CLAVE_COCINA = "pedido.enviado.cocina";
    public static final String COLA_PAGO_CONFIRMADO = "comandas.pago.confirmado";
    public static final String CLAVE_PAGO_CONFIRMADO = "pago.confirmado";

    @Bean
    Jackson2JsonMessageConverter convertidorJsonRabbit() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    TopicExchange intercambioEventos() {
        return new TopicExchange(INTERCAMBIO, true, false);
    }

    @Bean
    Queue colaPedidosCocina() {
        return new Queue(COLA_COCINA, true);
    }

    @Bean
    Binding vinculoPedidosCocina(Queue colaPedidosCocina, TopicExchange intercambioEventos) {
        return BindingBuilder.bind(colaPedidosCocina).to(intercambioEventos).with(CLAVE_COCINA);
    }

    @Bean Queue colaPagoConfirmado() { return new Queue(COLA_PAGO_CONFIRMADO, true); }
    @Bean Binding vinculoPagoConfirmado(Queue colaPagoConfirmado, TopicExchange intercambioEventos) {
        return BindingBuilder.bind(colaPagoConfirmado).to(intercambioEventos).with(CLAVE_PAGO_CONFIRMADO);
    }
}
