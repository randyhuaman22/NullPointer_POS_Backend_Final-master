package pe.upeu.nullpointerpos.cocina;

import org.junit.jupiter.api.Test;
import pe.upeu.nullpointerpos.cocina.mapper.EventoCocinaMapper;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** [T-02 / T-05 START] Valida contrato, identificadores, hora y detalles del evento. */
class EventoCocinaMapperTest {
    private final EventoCocinaMapper mapper = new EventoCocinaMapper();
    private Map<String,Object> eventoValido() {
        Map<String,Object> detalle=Map.of("idDetallePedido",11,"nombreProducto","Combo Pollo","cantidad",2,"estado","PENDIENTE");
        Map<String,Object> pedido=Map.of("idPedido",7,"idMesa",3,"codigoMesa","M03","estado","ENVIADO_COCINA","detalles",List.of(detalle));
        return Map.of("idEvento",90,"tipoEvento","PEDIDO_ENVIADO_COCINA","fechaHora","2026-11-11T10:00:00-05:00","pedido",pedido);
    }
    @Test void aceptaEventoCorrecto(){var e=mapper.desdeMensaje(eventoValido());assertEquals(90L,e.idEvento());assertEquals("PEDIDO_ENVIADO_COCINA",e.tipoEvento());}
    @Test void rechazaEventoSinIdentificador(){var e=new java.util.HashMap<>(eventoValido());e.remove("idEvento");assertThrows(IllegalArgumentException.class,()->mapper.desdeMensaje(e));}
    @Test void rechazaPedidoSinDetalle(){var e=new java.util.HashMap<>(eventoValido());e.put("pedido",Map.of("idPedido",7,"idMesa",3,"codigoMesa","M03","estado","ENVIADO_COCINA","detalles",List.of()));assertThrows(IllegalArgumentException.class,()->mapper.desdeMensaje(e));}
    @Test void rechazaFechaNoIso(){var e=new java.util.HashMap<>(eventoValido());e.put("fechaHora","ayer");assertThrows(IllegalArgumentException.class,()->mapper.desdeMensaje(e));}
}
/** [T-02 / T-05 END] */
