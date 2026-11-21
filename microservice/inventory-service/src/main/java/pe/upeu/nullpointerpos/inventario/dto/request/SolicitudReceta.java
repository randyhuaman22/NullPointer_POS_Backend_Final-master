package pe.upeu.nullpointerpos.inventario.dto.request;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.util.List;
public record SolicitudReceta(@NotNull Long idProducto,@NotEmpty List<@Valid IngredienteReceta> ingredientes){}
