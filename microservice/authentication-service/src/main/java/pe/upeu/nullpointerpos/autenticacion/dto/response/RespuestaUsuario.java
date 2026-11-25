package pe.upeu.nullpointerpos.autenticacion.dto.response;

import java.time.OffsetDateTime;

public record RespuestaUsuario(Long idUsuario, String codigoTrabajador, String nombreCompleto,
                               String correo, String rol, boolean activo, OffsetDateTime fechaCreacion) { }
