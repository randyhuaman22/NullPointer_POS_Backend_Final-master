package pe.upeu.nullpointerpos.productos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SolicitudConfiguracionOpcionesProducto(
        @NotNull List<@NotNull @Valid SolicitudGrupoOpcionesProducto> grupos) { }
