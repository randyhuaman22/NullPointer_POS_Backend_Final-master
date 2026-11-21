package pe.upeu.nullpointerpos.inventario.config;
import org.springframework.amqp.core.*;import org.springframework.context.annotation.Bean;import org.springframework.context.annotation.Configuration;import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
@Configuration public class ConfiguracionMensajeriaRabbit {
 @Bean Jackson2JsonMessageConverter convertidorJsonRabbit(){return new Jackson2JsonMessageConverter();}
 @Bean TopicExchange intercambioEventos(){return new TopicExchange("nullpointer.eventos",true,false);}
 @Bean Queue colaPedidoPagadoInventario(){return new Queue("inventario.pedido.pagado",true);}
 @Bean Binding vinculoPedidoPagadoInventario(Queue colaPedidoPagadoInventario,TopicExchange intercambioEventos){return BindingBuilder.bind(colaPedidoPagadoInventario).to(intercambioEventos).with("pedido.pagado");}
}
