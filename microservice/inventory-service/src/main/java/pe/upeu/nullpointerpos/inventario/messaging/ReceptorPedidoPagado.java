package pe.upeu.nullpointerpos.inventario.messaging;
import org.springframework.amqp.rabbit.annotation.RabbitListener;import org.springframework.stereotype.Component;import pe.upeu.nullpointerpos.inventario.repository.RepositorioInventario;import pe.upeu.nullpointerpos.inventario.config.ConfiguracionMensajeriaRabbit;import java.util.Map;
/** [SPRINT 3 / HU-22 / S3-T-26..S3-T-30 START] Inventario recibe eventos confirmados; nunca accede a bd_comandas. */
@Component public class ReceptorPedidoPagado {private final RepositorioInventario repo;public ReceptorPedidoPagado(RepositorioInventario r){repo=r;}@RabbitListener(queues="inventario.pedido.pagado") public void recibir(Map<String,Object> evento){repo.procesarPedidoPagado(evento);}}
/** [SPRINT 3 / HU-22 / S3-T-26..S3-T-30 END] */
