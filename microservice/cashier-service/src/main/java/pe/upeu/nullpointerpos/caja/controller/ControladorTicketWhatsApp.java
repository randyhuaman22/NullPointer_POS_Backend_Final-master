package pe.upeu.nullpointerpos.caja.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.caja.client.ClientePedidos;
import pe.upeu.nullpointerpos.caja.dto.request.SolicitudTicketWhatsApp;
import pe.upeu.nullpointerpos.caja.dto.response.RespuestaDatosComprobante;
import pe.upeu.nullpointerpos.caja.repository.RepositorioCaja;
import pe.upeu.nullpointerpos.caja.service.GeneradorPdfComprobanteDemostracion;
import pe.upeu.nullpointerpos.caja.service.ServicioWhatsApp;
import pe.upeu.nullpointerpos.caja.sunat.GeneradorPdfTicketSunat;
import pe.upeu.nullpointerpos.caja.sunat.RespuestaComprobanteSunat;
import java.util.LinkedHashMap;
import java.util.Map;

/** [SPRINT 3 / HU-19 / S3-T-11..S3-T-14 START] Genera el comprobante PDF y lo envía por WhatsApp vía Green-API. */
@RestController
@RequestMapping("/api/v1/tickets")
public class ControladorTicketWhatsApp {
    private final RepositorioCaja repo;
    private final ClientePedidos pedidos;
    private final ServicioWhatsApp whatsapp;
    private final GeneradorPdfTicketSunat generadorTicketSunat;
    private final GeneradorPdfComprobanteDemostracion generadorDemo;

    public ControladorTicketWhatsApp(
            RepositorioCaja repo,
            ClientePedidos pedidos,
            ServicioWhatsApp whatsapp,
            GeneradorPdfTicketSunat generadorTicketSunat,
            GeneradorPdfComprobanteDemostracion generadorDemo) {
        this.repo = repo;
        this.pedidos = pedidos;
        this.whatsapp = whatsapp;
        this.generadorTicketSunat = generadorTicketSunat;
        this.generadorDemo = generadorDemo;
    }

    @PostMapping("/whatsapp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> whatsapp(@Valid @RequestBody SolicitudTicketWhatsApp s, Authentication auth) {
        var p = pedidos.buscarResumen(s.idPedido());
        if (p == null || java.util.Set.of("BORRADOR", "ANULADO").contains(p.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede generar un ticket para este pedido.");
        }
        RespuestaComprobanteSunat cpe = repo.buscarComprobantePorPedido(s.idPedido());
        String referencia = cpe != null
                ? cpe.tipoComprobante() + " " + cpe.numeroCompleto()
                : "Ticket " + p.codigoPedido();
        String contenido = "Wendy's Food - " + referencia
                + "\nPedido: " + p.codigoPedido()
                + "\nTotal: S/ " + (cpe != null ? cpe.totalImporte() : p.importeTotal())
                + "\nEstado: " + p.estado();

        Long id = repo.guardarTicket(s.idPedido(), s.telefono(), contenido);
        return enviarPdfGuardado(id, s.idPedido(), s.telefono(), contenido, p.codigoPedido(), p.importeTotal(), cpe);
    }

    @PostMapping("/{idTicket}/reintentar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> reintentar(@PathVariable Long idTicket) {
        Map<String, Object> t = repo.ticket(idTicket);
        if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado.");
        Long idPedido = ((Number) t.get("idPedido")).longValue();
        var p = pedidos.buscarResumen(idPedido);
        RespuestaComprobanteSunat cpe = repo.buscarComprobantePorPedido(idPedido);
        String codigoPedido = p != null ? p.codigoPedido() : "PED-" + idPedido;
        java.math.BigDecimal importe = p != null && p.importeTotal() != null ? p.importeTotal() : java.math.BigDecimal.ZERO;
        return enviarPdfGuardado(
                idTicket,
                idPedido,
                String.valueOf(t.get("telefono")),
                String.valueOf(t.get("contenido")),
                codigoPedido,
                importe,
                cpe);
    }

    private Map<String, Object> enviarPdfGuardado(
            Long idTicket,
            Long idPedido,
            String telefono,
            String contenido,
            String codigoPedido,
            java.math.BigDecimal importePedido,
            RespuestaComprobanteSunat cpe) {
        if (!whatsapp.estaConfigurado()) {
            repo.actualizarEstadoTicket(idTicket, "PENDIENTE_CONFIGURACION",
                    "Falta configurar GREENAPI_ID_INSTANCE y GREENAPI_API_TOKEN.");
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("idTicket", idTicket);
            out.put("estado", "PENDIENTE_CONFIGURACION");
            out.put("mensaje", "Ticket guardado; configure GREENAPI_ID_INSTANCE y GREENAPI_API_TOKEN para enviar el PDF.");
            out.put("contenido", contenido);
            return out;
        }
        try {
            byte[] pdfBytes;
            String nombreArchivo;
            String leyenda;
            if (cpe != null) {
                pdfBytes = generadorTicketSunat.generarTicket80mm(cpe, codigoPedido);
                nombreArchivo = cpe.numeroCompleto() + ".pdf";
                leyenda = "Wendy's Food - " + cpe.tipoComprobante() + " " + cpe.numeroCompleto()
                        + " | Total S/ " + cpe.totalImporte();
            } else {
                var datosDemo = new RespuestaDatosComprobante(
                        "BOLETA", "SIN_DATOS", false, null, null,
                        "CLIENTES VARIOS", null, telefono, null, "COMPROBANTE", "Envío directo WhatsApp");
                pdfBytes = generadorDemo.generar(codigoPedido, importePedido, datosDemo);
                nombreArchivo = "Ticket-" + codigoPedido + ".pdf";
                leyenda = "Wendy's Food - Comprobante " + codigoPedido + " | Total S/ " + importePedido;
            }

            String respuesta = whatsapp.enviarPdf(telefono, pdfBytes, nombreArchivo, leyenda);
            repo.actualizarEstadoTicket(idTicket, "ENVIADO", limitar(respuesta));
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("idTicket", idTicket);
            out.put("estado", "ENVIADO");
            out.put("archivoPdf", nombreArchivo);
            out.put("mensaje", "PDF " + nombreArchivo + " enviado por WhatsApp correctamente.");
            return out;
        } catch (RuntimeException ex) {
            String detalle = limitar(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            repo.actualizarEstadoTicket(idTicket, "ERROR_ENVIO", detalle);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("idTicket", idTicket);
            out.put("estado", "ERROR_ENVIO");
            out.put("mensaje", "Ticket guardado, pero Green-API no pudo enviar el PDF.");
            out.put("detalle", detalle);
            return out;
        }
    }

    private String limitar(String valor) { return valor.length() <= 1500 ? valor : valor.substring(0, 1500); }
}
/** [SPRINT 3 / HU-19 / S3-T-11..S3-T-14 END] */
