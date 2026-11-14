package pe.upeu.nullpointerpos.autenticacion.service;

import pe.upeu.nullpointerpos.autenticacion.dto.request.SolicitudCrearUsuario;
import pe.upeu.nullpointerpos.autenticacion.dto.request.SolicitudActualizarUsuario;
import pe.upeu.nullpointerpos.autenticacion.dto.response.RespuestaUsuario;
import java.util.List;

public interface ServicioUsuarios {
    List<RespuestaUsuario> listarUsuarios();
    RespuestaUsuario buscarUsuario(Long idUsuario);
    RespuestaUsuario crearUsuario(SolicitudCrearUsuario solicitud, String codigoAdministrador);
    RespuestaUsuario cambiarEstado(Long idUsuario, boolean activo, String codigoAdministrador);
    RespuestaUsuario actualizarUsuario(Long idUsuario, SolicitudActualizarUsuario solicitud, String codigoAdministrador);
    java.util.List<java.util.Map<String,Object>> listarAuditoria(Long idUsuario);
}
