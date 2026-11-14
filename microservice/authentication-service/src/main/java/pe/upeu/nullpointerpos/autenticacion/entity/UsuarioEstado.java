package pe.upeu.nullpointerpos.autenticacion.entity;

/** Datos mínimos de un usuario que se bloquean durante la activación/desactivación. */
public record UsuarioEstado(String codigoTrabajador, String rol, boolean activo) { }
