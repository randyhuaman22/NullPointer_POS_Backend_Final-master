package pe.upeu.nullpointerpos.cocina.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class ConfiguracionMensajeriaRabbit {
    public static final String INTERCAMBIO = "nullpointer.eventos";
    public static final String COLA_COCINA = "cocina.pedido.enviado";
    public static final String CLAVE_COCINA = "pedido.enviado.cocina";

    @Bean
    Jackson2JsonMessageConverter convertidorJsonRabbit() { return new Jackson2JsonMessageConverter(); }

    @Bean
    TopicExchange intercambioEventos() { return new TopicExchange(INTERCAMBIO, true, false); }

    @Bean
    Queue colaPedidosCocina() { return new Queue(COLA_COCINA, true); }

    @Bean
    Binding vinculoPedidosCocina(@Qualifier("colaPedidosCocina") Queue colaPedidosCocina, TopicExchange intercambioEventos) {
        return BindingBuilder.bind(colaPedidosCocina).to(intercambioEventos).with(CLAVE_COCINA);
    }

    @Bean
    Binding vinculoActualizacionDetalleCocina(@Qualifier("colaPedidosCocina") Queue colaPedidosCocina, TopicExchange intercambioEventos) {
        return BindingBuilder.bind(colaPedidosCocina).to(intercambioEventos).with("pedido.detalle.actualizado");
    }

    @Bean
    Queue colaDisponibilidadProducto() { return new Queue("cocina.producto.disponibilidad.actualizada", true); }

    @Bean
    Binding vinculoDisponibilidadProducto(@Qualifier("colaDisponibilidadProducto") Queue colaDisponibilidadProducto, TopicExchange intercambioEventos) {
        return BindingBuilder.bind(colaDisponibilidadProducto).to(intercambioEventos).with("producto.disponibilidad.actualizada");
    }
}
