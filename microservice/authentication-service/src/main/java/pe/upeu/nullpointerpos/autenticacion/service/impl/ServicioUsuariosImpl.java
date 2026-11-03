package pe.upeu.nullpointerpos.autenticacion.service.impl;

import pe.upeu.nullpointerpos.autenticacion.dto.request.SolicitudCrearUsuario;
import pe.upeu.nullpointerpos.autenticacion.dto.request.SolicitudActualizarUsuario;
import pe.upeu.nullpointerpos.autenticacion.dto.response.RespuestaUsuario;
import pe.upeu.nullpointerpos.autenticacion.entity.UsuarioEstado;
import pe.upeu.nullpointerpos.autenticacion.mapper.UsuarioMapper;
import pe.upeu.nullpointerpos.autenticacion.repository.UsuarioRepositorio;
import pe.upeu.nullpointerpos.autenticacion.service.ServicioUsuarios;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class ServicioUsuariosImpl implements ServicioUsuarios {
    private final UsuarioRepositorio repositorio;
    private final UsuarioMapper mapper;
    private final PasswordEncoder codificador;

    public ServicioUsuariosImpl(UsuarioRepositorio repositorio, UsuarioMapper mapper, PasswordEncoder codificador) {
        this.repositorio = repositorio;
        this.mapper = mapper;
        this.codificador = codificador;
    }

    @Override
    public List<RespuestaUsuario> listarUsuarios() {
        return repositorio.listar().stream().map(mapper::aRespuesta).toList();
    }

    @Override
    public RespuestaUsuario buscarUsuario(Long idUsuario) {
        return repositorio.buscarPorId(idUsuario).map(mapper::aRespuesta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    @Override
    @Transactional
    public RespuestaUsuario crearUsuario(SolicitudCrearUsuario solicitud, String codigoAdministrador) {
        String rol = solicitud.rol().trim().toUpperCase();
        if (!repositorio.existeRolActivo(rol)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol indicado no existe o está inactivo.");
        }
        String codigo = solicitud.codigoTrabajador().trim().toUpperCase();
        String correo = solicitud.correo() == null ? "" : solicitud.correo().trim();
        repositorio.crear(codigo, solicitud.nombreCompleto().trim(), correo, codificador.encode(solicitud.clave()), rol);
        Long id = repositorio.buscarIdPorCodigo(codigo);
        repositorio.registrarAuditoriaUsuario(id, codigoAdministrador, "CREAR",
                solicitud.nombreCompleto().trim(), correo, rol, true);
        return mapper.aRespuesta(repositorio.buscarPorId(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No se pudo recuperar el usuario creado.")));
    }


    @Override
    @Transactional
    public RespuestaUsuario actualizarUsuario(Long idUsuario, SolicitudActualizarUsuario solicitud, String codigoAdministrador) {
        UsuarioEstado objetivo = repositorio.buscarEstadoBloqueando(idUsuario).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
        String nuevoRol = solicitud.rol().trim().toUpperCase();
        if (!repositorio.existeRolActivo(nuevoRol)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol indicado no existe o está inactivo.");
        }
        if (objetivo.codigoTrabajador().equalsIgnoreCase(codigoAdministrador)
                && !objetivo.rol().equalsIgnoreCase(nuevoRol)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No puedes cambiar tu propio rol mientras administras el sistema.");
        }
        if ("ADMINISTRADOR".equalsIgnoreCase(objetivo.rol())
                && !"ADMINISTRADOR".equalsIgnoreCase(nuevoRol)) {
            repositorio.bloquearRolAdministrador();
            if (repositorio.contarAdministradoresActivos() <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede cambiar el rol del último administrador activo.");
            }
        }
        String nombre = solicitud.nombreCompleto().trim();
        String correo = solicitud.correo() == null ? "" : solicitud.correo().trim();
        String hash = solicitud.nuevaClave() == null ? null : codificador.encode(solicitud.nuevaClave());
        try {
            repositorio.actualizarDatos(idUsuario, nombre, correo, hash, nuevoRol);
            repositorio.registrarAuditoriaUsuario(idUsuario, codigoAdministrador, "MODIFICAR",
                    nombre, correo, nuevoRol, hash != null);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo indicado ya está registrado en otra cuenta.");
        }
        return mapper.aRespuesta(repositorio.buscarPorId(idUsuario).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado después de actualizar.")));
    }

    @Override
    public List<java.util.Map<String,Object>> listarAuditoria(Long idUsuario) {
        return repositorio.listarAuditoriaUsuarios(idUsuario);
    }

    @Override
    @Transactional
    public RespuestaUsuario cambiarEstado(Long idUsuario, boolean activo, String codigoAdministrador) {
        UsuarioEstado objetivo = repositorio.buscarEstadoBloqueando(idUsuario).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
        if (!activo && objetivo.activo()) {
            if (codigoAdministrador != null && objetivo.codigoTrabajador().equalsIgnoreCase(codigoAdministrador)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No puedes desactivar tu propia cuenta de administrador.");
            }
            if ("ADMINISTRADOR".equalsIgnoreCase(objetivo.rol())) {
                repositorio.bloquearRolAdministrador();
                if (repositorio.contarAdministradoresActivos() <= 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "No se puede desactivar al último administrador activo.");
                }
            }
        }
        repositorio.actualizarEstado(idUsuario, activo);
        repositorio.registrarAuditoriaUsuario(idUsuario, codigoAdministrador,
                activo ? "ACTIVAR" : "DESACTIVAR", null, null, objetivo.rol(), false);
        return mapper.aRespuesta(repositorio.buscarPorId(idUsuario).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado.")));
    }
}
