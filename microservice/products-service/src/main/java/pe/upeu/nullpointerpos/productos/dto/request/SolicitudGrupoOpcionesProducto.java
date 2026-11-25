package pe.upeu.nullpointerpos.productos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SolicitudGrupoOpcionesProducto(
        @NotBlank @Size(max = 80) String nombre,
        boolean obligatorio,
        @Min(0) @Max(20) int seleccionMinima,
        @Min(1) @Max(20) int seleccionMaxima,
        @Min(0) Integer ordenVisualizacion,
        @NotEmpty List<@NotNull @Valid SolicitudOpcionProducto> opciones) { }
