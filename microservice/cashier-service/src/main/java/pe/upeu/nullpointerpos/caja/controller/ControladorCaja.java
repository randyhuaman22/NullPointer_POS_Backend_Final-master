package pe.upeu.nullpointerpos.caja.controller;

import jakarta.validation.Valid;import org.springframework.http.HttpStatus;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
import pe.upeu.nullpointerpos.caja.dto.request.*;import pe.upeu.nullpointerpos.caja.dto.response.*;import pe.upeu.nullpointerpos.caja.repository.RepositorioCaja;import pe.upeu.nullpointerpos.caja.service.impl.ServicioCajaImpl;import java.util.Map;
/** [HU-13..HU-16 / HU-20 / T-29..T-48 START] API de apertura, cobros, pagos mixtos y cierre. */
@RestController @RequestMapping("/api/v1/caja")
public class ControladorCaja {
 private final ServicioCajaImpl servicio; private final RepositorioCaja repo;
 public ControladorCaja(ServicioCajaImpl servicio,RepositorioCaja repo){this.servicio=servicio;this.repo=repo;}
 // [T-32 START]
 @PostMapping("/apertura") @ResponseStatus(HttpStatus.CREATED) public RespuestaTurnoCaja abrir(@Valid @RequestBody SolicitudAperturaCaja s,Authentication a){return servicio.abrir(Long.valueOf(a.getCredentials().toString()),a.getName(),s);}
 // [T-32 END]
 @GetMapping("/turno-abierto") public RespuestaTurnoCaja turno( Authentication a){return servicio.turnoAbierto(Long.valueOf(a.getCredentials().toString()));}
 // [T-36 START]
 @PostMapping("/cobrar") @ResponseStatus(HttpStatus.CREATED) public RespuestaPago efectivo(@Valid @RequestBody SolicitudCobroEfectivo s,Authentication a){return servicio.cobrarEfectivo(Long.valueOf(a.getCredentials().toString()),a.getName(),s);}
 // [T-36 END]
 // [T-41 START]
 @PostMapping("/cobrar-digital") @ResponseStatus(HttpStatus.CREATED) public RespuestaPago digital(@Valid @RequestBody SolicitudCobroDigital s,Authentication a){return servicio.cobrarDigital(Long.valueOf(a.getCredentials().toString()),a.getName(),s);}
 // [T-41 END]
 @PostMapping("/simulador-tarjeta") public RespuestaSimuladorTarjeta simularTarjeta(@Valid @RequestBody SolicitudSimularTarjeta s){return servicio.simularTarjeta(s);}
 // [T-45..T-47 START]
 @PostMapping("/cobrar-mixto") @ResponseStatus(HttpStatus.CREATED) public RespuestaPago mixto(@Valid @RequestBody SolicitudPagoMixto s,Authentication a){return servicio.cobrarMixto(Long.valueOf(a.getCredentials().toString()),a.getName(),s);}
 // [T-45..T-47 END]
 // [HU-20 START]
 @PostMapping("/cierre") public RespuestaCierreCaja cerrar(@Valid @RequestBody SolicitudCierreCaja s,Authentication a){return servicio.cerrar(Long.valueOf(a.getCredentials().toString()),s);}
 // [HU-20 END]
 @GetMapping("/pagos/{idPago}") public RespuestaPago buscarPago(@PathVariable Long idPago){var p=repo.buscarPago(idPago);if(p==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Pago no encontrado.");return p;}
}
/** [HU-13..HU-16 / HU-20 / T-29..T-48 END] */
