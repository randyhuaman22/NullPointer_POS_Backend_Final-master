package pe.upeu.nullpointerpos.autenticacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudInicioSesion(
        @NotBlank(message = "codigoTrabajador es obligatorio") @Size(max = 30) String codigoTrabajador,
        @NotBlank(message = "clave es obligatoria")
        @Pattern(regexp = "\\d{4}", message = "La clave debe tener exactamente 4 dígitos") String clave) { }
