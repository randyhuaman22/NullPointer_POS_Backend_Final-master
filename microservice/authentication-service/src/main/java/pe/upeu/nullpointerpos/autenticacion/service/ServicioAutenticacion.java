package pe.upeu.nullpointerpos.autenticacion.service;

import pe.upeu.nullpointerpos.autenticacion.dto.request.SolicitudInicioSesion;
import pe.upeu.nullpointerpos.autenticacion.dto.response.RespuestaInicioSesion;

public interface ServicioAutenticacion {
    RespuestaInicioSesion iniciarSesion(SolicitudInicioSesion solicitud);
}
