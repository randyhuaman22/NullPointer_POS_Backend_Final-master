package pe.upeu.nullpointerpos.autenticacion.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudCrearUsuario(
        @NotBlank(message = "codigoTrabajador es obligatorio") @Size(max = 30) String codigoTrabajador,
        @NotBlank(message = "nombreCompleto es obligatorio") @Size(max = 120) String nombreCompleto,
        @Email(message = "correo no tiene un formato válido") @Size(max = 160) String correo,
        @NotBlank(message = "clave es obligatoria")
        @Pattern(regexp = "\\d{4}", message = "La clave debe tener exactamente 4 dígitos") String clave,
        @NotBlank(message = "rol es obligatorio")
        @Pattern(regexp = "(?i)^(MOZO|CAJERO|ADMINISTRADOR|COCINERO|ALMACENERO)$", message = "Rol no permitido") String rol) { }
