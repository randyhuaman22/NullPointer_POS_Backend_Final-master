package pe.upeu.nullpointerpos.autenticacion.dto.response;

public record RespuestaInicioSesion(String token, String tipoToken, long expiraEnSegundos,
                                    UsuarioAutenticado usuario) { }
