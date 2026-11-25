package pe.upeu.nullpointerpos.autenticacion.service.impl;

import pe.upeu.nullpointerpos.autenticacion.dto.request.SolicitudInicioSesion;
import pe.upeu.nullpointerpos.autenticacion.dto.response.RespuestaInicioSesion;
import pe.upeu.nullpointerpos.autenticacion.dto.response.UsuarioAutenticado;
import pe.upeu.nullpointerpos.autenticacion.entity.Usuario;
import pe.upeu.nullpointerpos.autenticacion.repository.UsuarioRepositorio;
import pe.upeu.nullpointerpos.autenticacion.security.ServicioJwt;
import pe.upeu.nullpointerpos.autenticacion.service.ServicioAutenticacion;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioAutenticacionImpl implements ServicioAutenticacion {
    private final UsuarioRepositorio repositorio;
    private final PasswordEncoder codificador;
    private final ServicioJwt servicioJwt;

    public ServicioAutenticacionImpl(UsuarioRepositorio repositorio, PasswordEncoder codificador, ServicioJwt servicioJwt) {
        this.repositorio = repositorio;
        this.codificador = codificador;
        this.servicioJwt = servicioJwt;
    }

    @Override
    public RespuestaInicioSesion iniciarSesion(SolicitudInicioSesion solicitud) {
        String codigo = solicitud.codigoTrabajador().trim().toUpperCase();
        Usuario usuario = repositorio.buscarPorCodigo(codigo).orElse(null);
        boolean credencialesValidas = usuario != null && usuario.activo()
                && codificador.matches(solicitud.clave(), usuario.hashClave());
        if (!credencialesValidas) {
            repositorio.registrarEvento(null, codigo);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código de trabajador o clave incorrectos.");
        }
        repositorio.registrarEvento(usuario.idUsuario(), usuario.codigoTrabajador());
        String token = servicioJwt.emitir(usuario.idUsuario(), usuario.codigoTrabajador(),
                usuario.nombreCompleto(), usuario.rol());
        UsuarioAutenticado datos = new UsuarioAutenticado(usuario.idUsuario(), usuario.codigoTrabajador(),
                usuario.nombreCompleto(), usuario.rol());
        return new RespuestaInicioSesion(token, "Bearer", servicioJwt.segundosVigencia(), datos);
    }
}
