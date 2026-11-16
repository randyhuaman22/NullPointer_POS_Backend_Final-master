package pe.upeu.nullpointerpos.cocina.mapper;

import pe.upeu.nullpointerpos.cocina.dto.event.EventoPedidoCocina;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/** [T-02 START] Contrato estricto para eventos que actualizan el KDS. */
@Component
public class EventoCocinaMapper {
    @SuppressWarnings("unchecked")
    public EventoPedidoCocina desdeMensaje(Map<String, Object> mensaje) {
        if (mensaje == null) throw new IllegalArgumentException("El evento recibido está vacío.");
        Object id = mensaje.get("idEvento");
        if (!(id instanceof Number numero)) throw new IllegalArgumentException("El evento debe incluir idEvento numérico.");
        String tipo = textoObligatorio(mensaje.get("tipoEvento"), "tipoEvento");
        if (!List.of("PEDIDO_ENVIADO_COCINA", "DETALLE_PLATO_ACTUALIZADO").contains(tipo)) {
            throw new IllegalArgumentException("Tipo de evento de cocina no permitido: " + tipo);
        }
        String fecha = textoObligatorio(mensaje.get("fechaHora"), "fechaHora");
        try { OffsetDateTime.parse(fecha); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("fechaHora debe estar en formato ISO-8601 con zona.", ex); }

        Object pedido = mensaje.get("pedido");
        if (!(pedido instanceof Map<?, ?> crudo)) throw new IllegalArgumentException("El evento debe incluir el objeto pedido.");
        Map<String,Object> datosPedido = (Map<String,Object>) crudo;
        numeroObligatorio(datosPedido.get("idPedido"), "pedido.idPedido");
        numeroObligatorio(datosPedido.get("idMesa"), "pedido.idMesa");
        textoObligatorio(datosPedido.get("codigoMesa"), "pedido.codigoMesa");
        textoObligatorio(datosPedido.get("estado"), "pedido.estado");
        Object detalles = datosPedido.get("detalles");
        if (!(detalles instanceof List<?> filas) || filas.isEmpty()) throw new IllegalArgumentException("El pedido debe tener al menos un detalle.");
        for (int i = 0; i < filas.size(); i++) {
            if (!(filas.get(i) instanceof Map<?, ?> detalle)) throw new IllegalArgumentException("Detalle inválido en posición " + i);
            numeroObligatorio(detalle.get("idDetallePedido"), "detalles[" + i + "].idDetallePedido");
            textoObligatorio(detalle.get("nombreProducto"), "detalles[" + i + "].nombreProducto");
            Object cantidad = detalle.get("cantidad");
            if (!(cantidad instanceof Number n) || n.intValue() < 1) throw new IllegalArgumentException("La cantidad de cada plato debe ser positiva.");
            textoObligatorio(detalle.get("estado"), "detalles[" + i + "].estado");
        }
        return new EventoPedidoCocina(numero.longValue(), tipo, fecha, datosPedido);
    }

    private String textoObligatorio(Object valor, String campo) {
        if (!(valor instanceof String s) || s.isBlank()) throw new IllegalArgumentException("Falta el campo obligatorio " + campo + ".");
        return s;
    }
    private void numeroObligatorio(Object valor, String campo) {
        if (!(valor instanceof Number)) throw new IllegalArgumentException("El campo " + campo + " debe ser numérico.");
    }
}
/** [T-02 END] */
