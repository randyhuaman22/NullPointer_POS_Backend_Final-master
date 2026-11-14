package pe.upeu.nullpointerpos.autenticacion.dto.response;

public record UsuarioAutenticado(Long idUsuario, String codigoTrabajador, String nombreCompleto, String rol) { }
