package pe.upeu.nullpointerpos.caja.dto.request;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record SolicitudCierreCaja(@NotNull @DecimalMin(value="0.00") BigDecimal importeReal,@Size(max=300) String motivo){}
