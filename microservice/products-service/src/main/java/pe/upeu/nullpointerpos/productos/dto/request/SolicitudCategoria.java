package pe.upeu.nullpointerpos.productos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record SolicitudCategoria(
        @NotBlank @Size(max = 80) String nombre,
        @Size(max = 250) String descripcion,
        @NotBlank @Pattern(regexp = "(?i)POLLERIA|MENU|HELADERIA|JUGUERIA") String areaNegocio,
        @Positive Long idCategoriaPadre,
        @Min(0) Integer ordenVisualizacion) { }
