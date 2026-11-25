package pe.upeu.nullpointerpos.pedidos.dto.external;

import java.math.BigDecimal;
import java.util.List;

public record RespuestaOpcionesValidadas(BigDecimal precioAdicional, List<String> opcionesSeleccionadas) { }
