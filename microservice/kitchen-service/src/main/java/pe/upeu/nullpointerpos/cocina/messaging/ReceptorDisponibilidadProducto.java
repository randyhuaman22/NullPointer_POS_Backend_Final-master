package pe.upeu.nullpointerpos.cocina.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.upeu.nullpointerpos.cocina.service.ServicioEventosCocina;
import java.util.Map;

/** [HU-12 / T-28 START] Reenvía cambios de disponibilidad de catálogo al KDS en tiempo real. */
@Component
public class ReceptorDisponibilidadProducto {
    private final ServicioEventosCocina servicio;
    public ReceptorDisponibilidadProducto(ServicioEventosCocina servicio) { this.servicio = servicio; }
    @RabbitListener(queues = "cocina.producto.disponibilidad.actualizada")
    public void recibir(Map<String,Object> evento) {
        if (evento.get("idProducto") == null || evento.get("disponible") == null) {
            throw new IllegalArgumentException("Evento de disponibilidad incompleto.");
        }
        servicio.publicarDisponibilidad(evento);
    }
}
/** [HU-12 / T-28 END] */
