package pe.upeu.nullpointerpos.autenticacion.controller;

import jakarta.validation.Valid;
import pe.upeu.nullpointerpos.autenticacion.dto.request.SolicitudInicioSesion;
import pe.upeu.nullpointerpos.autenticacion.dto.response.RespuestaInicioSesion;
import pe.upeu.nullpointerpos.autenticacion.service.ServicioAutenticacion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** [HU-01 START] Endpoint de inicio de sesión del personal; emite JWT si la clave es válida. */
@RestController
@RequestMapping("/api/v1/autenticacion")
public class ControladorAutenticacion {
    private final ServicioAutenticacion servicio;
    public ControladorAutenticacion(ServicioAutenticacion servicio) { this.servicio = servicio; }
    @PostMapping("/iniciar-sesion")
    public ResponseEntity<RespuestaInicioSesion> iniciarSesion(@Valid @RequestBody SolicitudInicioSesion solicitud) {
        return ResponseEntity.ok(servicio.iniciarSesion(solicitud));
    }
}
/** [HU-01 END] */
