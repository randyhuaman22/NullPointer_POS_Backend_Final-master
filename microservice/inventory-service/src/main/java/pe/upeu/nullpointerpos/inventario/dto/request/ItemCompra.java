package pe.upeu.nullpointerpos.inventario.dto.request;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record ItemCompra(@NotNull Long idInsumo,@NotNull @DecimalMin("0.001") BigDecimal cantidad,@NotNull @DecimalMin("0.000") BigDecimal costoUnitario){}
