package pe.upeu.nullpointerpos.pedidos.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record SolicitudGrupoMesas(
        @NotEmpty List<@NotNull @Positive Long> idsMesas,
        @NotNull @Positive Long idMesaPrincipal) { }
