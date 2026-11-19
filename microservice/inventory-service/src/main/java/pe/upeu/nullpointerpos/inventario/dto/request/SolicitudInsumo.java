package pe.upeu.nullpointerpos.inventario.dto.request;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record SolicitudInsumo(@Size(max=40) String codigo,@NotBlank @Size(max=120) String nombre,@NotBlank @Size(max=20) String unidad,@NotNull @DecimalMin("0.000") BigDecimal stockInicial,@NotNull @DecimalMin("0.000") BigDecimal stockMinimo,@NotNull @DecimalMin("0.000") BigDecimal costoUnitario){}
