package pe.upeu.nullpointerpos.productos.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RespuestaOpcionesValidadas(BigDecimal precioAdicional, List<String> opcionesSeleccionadas) { }
