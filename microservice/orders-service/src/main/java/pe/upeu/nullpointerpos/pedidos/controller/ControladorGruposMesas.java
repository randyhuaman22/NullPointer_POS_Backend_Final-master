package pe.upeu.nullpointerpos.pedidos.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudGrupoMesas;
import pe.upeu.nullpointerpos.pedidos.service.ServicioGruposMesas;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mesas/grupos")
public class ControladorGruposMesas {
    private final ServicioGruposMesas servicio;
    public ControladorGruposMesas(ServicioGruposMesas servicio) { this.servicio = servicio; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> unir(@Valid @RequestBody SolicitudGrupoMesas solicitud, Authentication auth) {
        return servicio.unir(solicitud, idActor(auth));
    }

    @GetMapping("/{idGrupo}")
    public Map<String,Object> consultar(@PathVariable Long idGrupo) { return servicio.consultar(idGrupo); }

    @PatchMapping("/{idGrupo}/finalizar")
    public Map<String,Object> finalizar(@PathVariable Long idGrupo) { return servicio.finalizar(idGrupo); }

    private Long idActor(Authentication auth) {
        if (auth == null || auth.getCredentials() == null) return null;
        try { return Long.valueOf(auth.getCredentials().toString()); }
        catch (NumberFormatException ex) { return null; }
    }
}
