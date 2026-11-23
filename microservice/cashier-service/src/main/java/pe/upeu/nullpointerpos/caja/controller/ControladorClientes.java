package pe.upeu.nullpointerpos.caja.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.caja.dto.request.SolicitudCliente;
import pe.upeu.nullpointerpos.caja.repository.RepositorioCaja;
import pe.upeu.nullpointerpos.caja.service.DirectorioDemostracionDocumentos;
import pe.upeu.nullpointerpos.caja.service.ServicioDatosComprobante;

import java.util.Map;

/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-60..S3-T-62 START] Registro local y consulta DNI/RUC en línea (APISPERU/Decolecta). */
@RestController
@RequestMapping("/api/v1/clientes")
public class ControladorClientes {
    private final RepositorioCaja repositorio;
    private final ServicioDatosComprobante servicioDatosComprobante;

    @Autowired
    public ControladorClientes(RepositorioCaja repositorio, ServicioDatosComprobante servicioDatosComprobante) {
        this.repositorio = repositorio;
        this.servicioDatosComprobante = servicioDatosComprobante;
    }

    public ControladorClientes(RepositorioCaja repositorio, DirectorioDemostracionDocumentos directorioDemostracion) {
        this(repositorio, new ServicioDatosComprobante(repositorio, directorioDemostracion));
    }

    @GetMapping("/{tipoDocumento}/{numeroDocumento}")
    public Map<String, Object> buscar(@PathVariable String tipoDocumento, @PathVariable String numeroDocumento) {
        return servicioDatosComprobante.consultarCliente(tipoDocumento, numeroDocumento)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        servicioDatosComprobante.consultaExternaConfigurada()
                                ? "Documento no encontrado en el padrón ni en la base local."
                                : "Documento no encontrado en la base local (configure CONSULTA_DOC_TOKEN para consultar DNI/RUC reales en línea, o use DNI 12345678 / RUC 20123456789)."
                ));
    }

    @PutMapping
    public Map<String, Object> guardar(@Valid @RequestBody SolicitudCliente solicitud) {
        validar(solicitud.tipoDocumento(), solicitud.numeroDocumento());
        return repositorio.guardarCliente(solicitud.tipoDocumento(), solicitud.numeroDocumento(),
                solicitud.nombreCompleto().trim(), solicitud.telefono(), solicitud.correo());
    }

    private void validar(String tipo, String numero) {
        String t = tipo == null ? "" : tipo.toUpperCase();
        if (t.equals("DNI") && (numero == null || !numero.matches("[0-9]{8}")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DNI debe tener 8 dígitos.");
        if (t.equals("RUC") && (numero == null || !numero.matches("[0-9]{11}")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RUC debe tener 11 dígitos.");
        if (!t.equals("DNI") && !t.equals("RUC"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo debe ser DNI o RUC.");
    }
}
/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-60..S3-T-62 END] */
