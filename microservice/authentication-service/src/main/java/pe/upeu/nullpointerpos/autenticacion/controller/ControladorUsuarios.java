package pe.upeu.nullpointerpos.autenticacion.controller;

import jakarta.validation.Valid;
import pe.upeu.nullpointerpos.autenticacion.dto.request.SolicitudCrearUsuario;
import pe.upeu.nullpointerpos.autenticacion.dto.request.SolicitudActualizarUsuario;
import pe.upeu.nullpointerpos.autenticacion.dto.response.RespuestaUsuario;
import pe.upeu.nullpointerpos.autenticacion.service.ServicioUsuarios;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** [HU-26 START] Administración de cuentas y desactivación de trabajadores. */
@RestController
@RequestMapping("/api/v1/administracion/usuarios")
public class ControladorUsuarios {
    private final ServicioUsuarios servicio;
    public ControladorUsuarios(ServicioUsuarios servicio) { this.servicio = servicio; }
    @GetMapping public List<RespuestaUsuario> listar() { return servicio.listarUsuarios(); }
    @GetMapping("/{id}") public RespuestaUsuario buscar(@PathVariable Long id) { return servicio.buscarUsuario(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public RespuestaUsuario crear(@Valid @RequestBody SolicitudCrearUsuario solicitud, Authentication autenticacion) {
        return servicio.crearUsuario(solicitud, autenticacion.getName());
    }
    @PutMapping("/{id}")
    public RespuestaUsuario actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudActualizarUsuario solicitud,
                                       Authentication autenticacion) {
        return servicio.actualizarUsuario(id, solicitud, autenticacion.getName());
    }
    @GetMapping("/auditoria")
    public List<java.util.Map<String,Object>> auditoria(@RequestParam(required=false) Long idUsuario) {
        return servicio.listarAuditoria(idUsuario);
    }
    @DeleteMapping("/{id}")
    public RespuestaUsuario darDeBaja(@PathVariable Long id, Authentication autenticacion) {
        return servicio.cambiarEstado(id, false, autenticacion.getName());
    }
    @PatchMapping("/{id}/desactivar")
    public RespuestaUsuario desactivar(@PathVariable Long id, Authentication autenticacion) {
        return servicio.cambiarEstado(id, false, autenticacion.getName());
    }
    @PatchMapping("/{id}/activar")
    public RespuestaUsuario activar(@PathVariable Long id, Authentication autenticacion) {
        return servicio.cambiarEstado(id, true, autenticacion.getName());
    }
}
/** [HU-26 END] */
