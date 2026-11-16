package pe.upeu.nullpointerpos.cocina.dto.event;

import java.util.Map;

public record EventoPedidoCocina(Long idEvento, String tipoEvento, String fechaHora, Map<String, Object> pedido) { }
