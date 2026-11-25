package pe.upeu.nullpointerpos.caja.service.impl;
import com.fasterxml.jackson.core.type.TypeReference;import com.fasterxml.jackson.databind.ObjectMapper;import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;import org.springframework.amqp.rabbit.core.RabbitTemplate;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;
import pe.upeu.nullpointerpos.caja.repository.RepositorioCaja;import java.util.*;import java.util.concurrent.TimeUnit;
/** [T-37/T-43/T-47 START] Outbox caja: el pago y su evento se guardan en la misma transacción. */
@Component public class PublicadorEventosCaja {
 private static final Logger LOG=LoggerFactory.getLogger(PublicadorEventosCaja.class);private final RepositorioCaja repo;private final RabbitTemplate rabbit;private final ObjectMapper json;
 public PublicadorEventosCaja(RepositorioCaja repo,RabbitTemplate rabbit,ObjectMapper json){this.repo=repo;this.rabbit=rabbit;this.json=json;}
 @Scheduled(fixedDelayString="${app.outbox.intervalo-ms:1000}") public void publicar(){for(Map<String,Object> r:repo.pendientes()){Long id=((Number)r.get("id_evento")).longValue();try{
   Map<String,Object> e=json.readValue((String)r.get("contenido_evento"),new TypeReference<Map<String,Object>>(){});e.put("idEvento",id);
   CorrelationData c=new CorrelationData(String.valueOf(id));rabbit.convertAndSend("nullpointer.eventos","pago.confirmado",e,c);
   var ok=c.getFuture().get(10,TimeUnit.SECONDS);if(!ok.isAck()||c.getReturned()!=null)throw new IllegalStateException("RabbitMQ no confirmó pago: "+ok.getReason());repo.marcarPublicado(id);
  }catch(InterruptedException ie){Thread.currentThread().interrupt();LOG.warn("Publicación de pago interrumpida {}.",id);break;}catch(Exception ex){LOG.error("No se publicó evento de pago {}. Se reintentará.",id,ex);break;}}}
}
/** [T-37/T-43/T-47 END] */
