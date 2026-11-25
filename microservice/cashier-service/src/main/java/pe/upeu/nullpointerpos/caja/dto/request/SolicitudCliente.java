package pe.upeu.nullpointerpos.caja.dto.request;
import jakarta.validation.constraints.*;
public record SolicitudCliente(@NotBlank @Pattern(regexp="(?i)^(DNI|RUC)$") String tipoDocumento,
 @NotBlank @Pattern(regexp="^([0-9]{8}|[0-9]{11})$") String numeroDocumento,
 @NotBlank @Size(max=160) String nombreCompleto,@Size(max=20) String telefono,@Email @Size(max=160) String correo){}
