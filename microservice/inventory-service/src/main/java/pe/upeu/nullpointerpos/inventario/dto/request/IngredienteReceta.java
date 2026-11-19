package pe.upeu.nullpointerpos.inventario.dto.request;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record IngredienteReceta(@NotNull Long idInsumo,@NotNull @DecimalMin("0.001") BigDecimal cantidadPorPlato){}
