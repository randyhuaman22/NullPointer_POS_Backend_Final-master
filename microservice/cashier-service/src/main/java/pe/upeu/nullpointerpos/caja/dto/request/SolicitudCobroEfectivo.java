package pe.upeu.nullpointerpos.caja.dto.request;
import jakarta.validation.constraints.DecimalMin;import jakarta.validation.constraints.NotNull;import jakarta.validation.constraints.NotBlank;import java.math.BigDecimal;
public record SolicitudCobroEfectivo(@NotNull Long idPedido,@NotNull @DecimalMin(value="0.01") BigDecimal importeRecibido,@NotBlank String idempotencia){}
