package pe.upeu.nullpointerpos.pedidos.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudMesaAdministracion;
import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaMesa;
import pe.upeu.nullpointerpos.pedidos.service.ServicioAdministracionMesas;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mesas/administracion")
public class ControladorAdministracionMesas {
    private final ServicioAdministracionMesas servicio;

    public ControladorAdministracionMesas(ServicioAdministracionMesas servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<RespuestaMesa> listar(@RequestParam(defaultValue = "true") boolean incluirInactivas) {
        return servicio.listar(incluirInactivas);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaMesa crear(@Valid @RequestBody SolicitudMesaAdministracion solicitud, Authentication auth) {
        return servicio.crear(solicitud, idActor(auth));
    }

    @PutMapping("/{idMesa}")
    public RespuestaMesa actualizar(@PathVariable Long idMesa,
                                    @Valid @RequestBody SolicitudMesaAdministracion solicitud,
                                    Authentication auth) {
        return servicio.actualizar(idMesa, solicitud, idActor(auth));
    }

    @DeleteMapping("/{idMesa}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long idMesa, Authentication auth) {
        servicio.desactivar(idMesa, idActor(auth));
    }

    @PatchMapping("/{idMesa}/restaurar")
    public RespuestaMesa restaurar(@PathVariable Long idMesa, Authentication auth) {
        return servicio.restaurar(idMesa, idActor(auth));
    }

    @PatchMapping("/{idMesa}/fuera-servicio")
    public RespuestaMesa fueraServicio(@PathVariable Long idMesa, Authentication auth) {
        return servicio.marcarFueraServicio(idMesa, idActor(auth));
    }

    @PatchMapping("/{idMesa}/habilitar")
    public RespuestaMesa habilitar(@PathVariable Long idMesa, Authentication auth) {
        return servicio.habilitar(idMesa, idActor(auth));
    }

    private Long idActor(Authentication auth) {
        if (auth == null || auth.getCredentials() == null) return null;
        try { return Long.valueOf(auth.getCredentials().toString()); }
        catch (NumberFormatException ex) { return null; }
    }
}
