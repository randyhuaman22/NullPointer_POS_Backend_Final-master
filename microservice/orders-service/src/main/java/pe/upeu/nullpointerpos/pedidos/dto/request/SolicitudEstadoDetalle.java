package pe.upeu.nullpointerpos.pedidos.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record SolicitudEstadoDetalle(@NotBlank @Pattern(regexp="(?i)LISTO") String estado) { }
