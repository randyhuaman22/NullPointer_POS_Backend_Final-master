package pe.upeu.nullpointerpos.cocina.service.impl;

import pe.upeu.nullpointerpos.cocina.mapper.EventoCocinaMapper;
import pe.upeu.nullpointerpos.cocina.service.ServicioEventosCocina;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** [T-03 / T-05 / T-19 / T-28 START] Despacha a WebSocket y suprime duplicados recientes recibidos por reintentos. */
@Service
public class ServicioEventosCocinaImpl implements ServicioEventosCocina {
    private static final int MAX_EVENTOS_RECORDADOS = 5000;
    private final SimpMessagingTemplate mensajeriaWebSocket;
    private final EventoCocinaMapper mapper;
    private final Set<String> vistos = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<String> ordenVistos = new ConcurrentLinkedQueue<>();

    public ServicioEventosCocinaImpl(SimpMessagingTemplate mensajeriaWebSocket, EventoCocinaMapper mapper) {
        this.mensajeriaWebSocket = mensajeriaWebSocket; this.mapper = mapper;
    }

    @Override public void publicarEnPantalla(Map<String, Object> mensaje) {
        if (!registrarSiNuevo(mensaje)) return;
        mensajeriaWebSocket.convertAndSend("/topic/cocina", mapper.desdeMensaje(mensaje));
    }

    @Override public void publicarDisponibilidad(Map<String, Object> mensaje) {
        if (!registrarSiNuevo(mensaje)) return;
        mensajeriaWebSocket.convertAndSend("/topic/cocina/disponibilidad", mensaje);
    }

    private boolean registrarSiNuevo(Map<String,Object> mensaje) {
        Object id = mensaje.get("idEvento");
        if (!(id instanceof Number)) return true; // Compatibility with any legacy, non-persisted event.
        String clave = String.valueOf(mensaje.getOrDefault("tipoEvento", "EVENTO")) + ":" + ((Number) id).longValue();
        if (!vistos.add(clave)) return false;
        ordenVistos.add(clave);
        while (vistos.size() > MAX_EVENTOS_RECORDADOS) {
            String anterior = ordenVistos.poll();
            if (anterior == null) break;
            vistos.remove(anterior);
        }
        return true;
    }
}
/** [T-03 / T-05 / T-19 / T-28 END] */
