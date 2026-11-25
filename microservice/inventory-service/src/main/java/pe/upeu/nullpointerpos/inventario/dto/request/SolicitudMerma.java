package pe.upeu.nullpointerpos.inventario.dto.request;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record SolicitudMerma(@NotNull Long idInsumo,@NotNull @DecimalMin("0.001") BigDecimal cantidad,@NotBlank @Size(max=300) String motivo){}
