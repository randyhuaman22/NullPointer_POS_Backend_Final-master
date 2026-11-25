package pe.upeu.nullpointerpos.caja.dto.request;
import jakarta.validation.constraints.*;

/** [HU-19 START] Validación de teléfono E.164/local normalizado para ticket WhatsApp. */
public record SolicitudTicketWhatsApp(
        @NotNull Long idPedido,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{9,15}$") String telefono) { }
/** [HU-19 END] */
