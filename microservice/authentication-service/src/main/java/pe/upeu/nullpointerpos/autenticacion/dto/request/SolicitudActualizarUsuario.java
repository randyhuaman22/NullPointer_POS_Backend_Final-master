package pe.upeu.nullpointerpos.autenticacion.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudActualizarUsuario(
        @NotBlank @Size(max = 120) String nombreCompleto,
        @Email @Size(max = 160) String correo,
        @NotBlank @Pattern(regexp = "(?i)^(MOZO|CAJERO|ADMINISTRADOR|COCINERO|ALMACENERO)$") String rol,
        @Pattern(regexp = "\\d{4}", message = "La nueva clave debe tener exactamente 4 dígitos") String nuevaClave) { }
