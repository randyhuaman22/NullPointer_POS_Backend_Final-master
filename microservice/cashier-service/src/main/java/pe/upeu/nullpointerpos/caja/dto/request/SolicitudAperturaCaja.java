package pe.upeu.nullpointerpos.caja.dto.request;
import jakarta.validation.constraints.DecimalMin;import jakarta.validation.constraints.NotNull;import java.math.BigDecimal;
public record SolicitudAperturaCaja(@NotNull @DecimalMin(value="0.00") BigDecimal montoInicial){}
