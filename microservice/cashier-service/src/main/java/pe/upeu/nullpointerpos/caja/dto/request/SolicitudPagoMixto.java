package pe.upeu.nullpointerpos.caja.dto.request;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.util.List;
public record SolicitudPagoMixto(@NotNull Long idPedido,@NotBlank @Size(max=100) String idempotencia,@NotEmpty List<@Valid PartePago> partes){}
