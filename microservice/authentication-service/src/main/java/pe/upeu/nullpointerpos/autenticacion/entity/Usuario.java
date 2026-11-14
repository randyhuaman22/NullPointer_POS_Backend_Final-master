package pe.upeu.nullpointerpos.autenticacion.entity;

import java.time.OffsetDateTime;

/** Representa la fila de esquema_autenticacion.usuarios junto con su rol. */
public record Usuario(Long idUsuario, String codigoTrabajador, String nombreCompleto, String correo,
                      String hashClave, boolean activo, String rol, OffsetDateTime fechaCreacion) { }
