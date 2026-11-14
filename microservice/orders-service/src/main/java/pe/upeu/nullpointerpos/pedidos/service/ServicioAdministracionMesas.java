package pe.upeu.nullpointerpos.pedidos.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudMesaAdministracion;
import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaMesa;
import pe.upeu.nullpointerpos.pedidos.mapper.MesaMapper;
import pe.upeu.nullpointerpos.pedidos.repository.MesaRepositorio;

import java.util.List;

@Service
public class ServicioAdministracionMesas {
    private final MesaRepositorio repositorio;
    private final MesaMapper mapper;

    public ServicioAdministracionMesas(MesaRepositorio repositorio, MesaMapper mapper) {
        this.repositorio = repositorio;
        this.mapper = mapper;
    }

    public List<RespuestaMesa> listar(boolean incluirInactivas) {
        return repositorio.listarAdministracion(incluirInactivas).stream().map(mapper::aRespuesta).toList();
    }

    @Transactional
    public RespuestaMesa crear(SolicitudMesaAdministracion solicitud, Long idActor) {
        try {
            Long id = repositorio.crearMesa(solicitud, idActor);
            return repositorio.buscarPorId(id).map(mapper::aRespuesta)
                    .orElseThrow(() -> error(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo recuperar la mesa creada."));
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw error(HttpStatus.CONFLICT, "El código de mesa ya está registrado.");
        }
    }

    @Transactional
    public RespuestaMesa actualizar(Long id, SolicitudMesaAdministracion solicitud, Long idActor) {
        var mesa = repositorio.bloquearPorId(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Mesa no encontrada o inactiva."));
        if ("FUERA_SERVICIO".equalsIgnoreCase(mesa.estado())) {
            throw error(HttpStatus.CONFLICT, "Primero habilita la mesa antes de modificar sus datos.");
        }
        if (repositorio.tienePedidosActivos(id)) {
            throw error(HttpStatus.CONFLICT, "No se puede modificar una mesa que tiene un pedido activo.");
        }
        try {
            if (!repositorio.actualizarMesa(id, solicitud, idActor)) throw error(HttpStatus.NOT_FOUND, "Mesa no encontrada o inactiva.");
            return repositorio.buscarPorId(id).map(mapper::aRespuesta).orElseThrow();
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw error(HttpStatus.CONFLICT, "El código de mesa ya está registrado.");
        }
    }

    @Transactional
    public void desactivar(Long id, Long idActor) {
        repositorio.bloquearPorId(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Mesa no encontrada o inactiva."));
        if (repositorio.tienePedidosActivos(id)) {
            throw error(HttpStatus.CONFLICT, "No se puede desactivar una mesa con pedidos activos.");
        }
        if (!repositorio.cambiarActivo(id, false, idActor)) throw error(HttpStatus.NOT_FOUND, "Mesa no encontrada o ya inactiva.");
    }

    @Transactional
    public RespuestaMesa restaurar(Long id, Long idActor) {
        if (!repositorio.cambiarActivo(id, true, idActor)) throw error(HttpStatus.CONFLICT, "La mesa no existe o ya está activa.");
        return repositorio.buscarPorId(id).map(mapper::aRespuesta).orElseThrow();
    }

    @Transactional
    public RespuestaMesa marcarFueraServicio(Long id, Long idActor) {
        repositorio.bloquearPorId(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Mesa no encontrada o inactiva."));
        if (repositorio.tienePedidosActivos(id)) {
            throw error(HttpStatus.CONFLICT, "Primero debe cerrarse o trasladarse el pedido activo de esta mesa.");
        }
        if (!repositorio.actualizarEstadoAdministrativo(id, "FUERA_SERVICIO", idActor)) {
            throw error(HttpStatus.NOT_FOUND, "Mesa no encontrada o inactiva.");
        }
        return repositorio.buscarPorId(id).map(mapper::aRespuesta).orElseThrow();
    }

    @Transactional
    public RespuestaMesa habilitar(Long id, Long idActor) {
        repositorio.bloquearPorId(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Mesa no encontrada o inactiva."));
        if (repositorio.tienePedidosActivos(id)) {
            throw error(HttpStatus.CONFLICT, "No se puede habilitar manualmente una mesa que ya tiene un pedido activo.");
        }
        if (!repositorio.actualizarEstadoAdministrativo(id, "LIBRE", idActor)) {
            throw error(HttpStatus.NOT_FOUND, "Mesa no encontrada o inactiva.");
        }
        return repositorio.buscarPorId(id).map(mapper::aRespuesta).orElseThrow();
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}
