package pe.upeu.nullpointerpos.caja.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.caja.client.ClientePedidos;
import pe.upeu.nullpointerpos.caja.dto.request.SolicitudDatosComprobante;
import pe.upeu.nullpointerpos.caja.dto.request.SolicitudPdfDemostracion;
import pe.upeu.nullpointerpos.caja.dto.response.RespuestaDatosComprobante;
import pe.upeu.nullpointerpos.caja.dto.response.RespuestaPago;
import pe.upeu.nullpointerpos.caja.repository.RepositorioCaja;
import pe.upeu.nullpointerpos.caja.service.GeneradorPdfComprobanteDemostracion;
import pe.upeu.nullpointerpos.caja.service.ServicioDatosComprobante;
import pe.upeu.nullpointerpos.caja.sunat.*;

import java.nio.charset.StandardCharsets;

/** API para preparación de datos de cliente y emisión electrónica oficial a SUNAT Beta. */
@RestController
@RequestMapping("/api/v1/comprobantes")
public class ControladorDatosComprobante {
    private final ServicioDatosComprobante servicio;
    private final ClientePedidos pedidos;
    private final GeneradorPdfComprobanteDemostracion generadorPdf;
    private final ServicioSunatBeta servicioSunat;
    private final GeneradorPdfTicketSunat generadorTicket;
    private final RepositorioCaja repo;
    private final org.springframework.transaction.support.TransactionTemplate transacciones;

    public ControladorDatosComprobante(
            ServicioDatosComprobante servicio,
            ClientePedidos pedidos,
            GeneradorPdfComprobanteDemostracion generadorPdf,
            ServicioSunatBeta servicioSunat,
            GeneradorPdfTicketSunat generadorTicket,
            RepositorioCaja repo,
            org.springframework.transaction.PlatformTransactionManager gestorTransacciones
    ) {
        this.servicio = servicio;
        this.pedidos = pedidos;
        this.generadorPdf = generadorPdf;
        this.servicioSunat = servicioSunat;
        this.generadorTicket = generadorTicket;
        this.repo = repo;
        this.transacciones = new org.springframework.transaction.support.TransactionTemplate(gestorTransacciones);
    }

    @PostMapping("/datos-cliente/validar")
    public RespuestaDatosComprobante preparar(@Valid @RequestBody SolicitudDatosComprobante solicitud) {
        return servicio.preparar(solicitud);
    }

    @PostMapping("/emitir")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaComprobanteSunat emitirComprobanteSunat(@Valid @RequestBody SolicitudEmitirComprobante solicitud) {
        // Se confirma la identidad del comprobante antes de llamar al servicio externo.
        var preparado = transacciones.execute(status -> prepararComprobante(solicitud));
        return reenviar(preparado.idComprobante());
    }

    private RespuestaComprobanteSunat prepararComprobante(SolicitudEmitirComprobante solicitud) {
        repo.bloquearPago(solicitud.idPago());
        RespuestaPago pago = repo.buscarPago(solicitud.idPago());
        if (pago == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El pago con ID " + solicitud.idPago() + " no existe.");
        }
        if (!pago.idPedido().equals(solicitud.idPedido())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago indicado no corresponde al pedido " + solicitud.idPedido() + ".");
        }

        // Idempotencia: si ya se emitió un comprobante para este pago, retornarlo
        RespuestaComprobanteSunat existente = repo.buscarComprobantePorPago(solicitud.idPago());
        if (existente != null) {
            return existente;
        }

        // Validar y preparar los datos del comprador según la modalidad elegida
        RespuestaDatosComprobante cliente = servicio.preparar(new SolicitudDatosComprobante(
                solicitud.tipoComprobante(), solicitud.modalidad(),
                solicitud.tipoDocumento(), solicitud.numeroDocumento(),
                solicitud.nombreCompleto(), solicitud.direccion(),
                solicitud.telefono(), solicitud.correo()
        ));

        // Obtener siguiente correlativo oficial
        Long correlativo = repo.siguienteCorrelativo(solicitud.tipoComprobante());

        // Procesar XML UBL 2.1, firma digital y envío a SUNAT Beta
        ServicioSunatBeta.ResultadoEmisionXml resultadoXml = servicioSunat.prepararEmision(
                solicitud.tipoComprobante(),
                correlativo,
                cliente.tipoDocumento(),
                cliente.numeroDocumento(),
                cliente.nombreCompleto(),
                cliente.direccion(),
                pago.importe(),
                pago.metodo(),
                pago.idPago().toString()
        );

        // Guardar registro oficial en la base de datos de caja
        Long idComprobante = repo.insertarComprobante(
                solicitud.idPago(),
                solicitud.idPedido(),
                solicitud.tipoComprobante(),
                resultadoXml.serie(),
                resultadoXml.correlativo(),
                cliente.tipoDocumento(),
                cliente.numeroDocumento(),
                cliente.nombreCompleto(),
                cliente.direccion(),
                pago.importe().divide(new java.math.BigDecimal("1.18"), 2, java.math.RoundingMode.HALF_UP),
                pago.importe().subtract(pago.importe().divide(new java.math.BigDecimal("1.18"), 2, java.math.RoundingMode.HALF_UP)),
                pago.importe(),
                pago.metodo(),
                resultadoXml.hashCpe(),
                resultadoXml.codigoCdr(),
                resultadoXml.descripcionCdr(),
                resultadoXml.estadoSunat(),
                resultadoXml.cadenaQr(),
                resultadoXml.xmlFirmado()
        );

        return repo.buscarComprobantePorId(idComprobante);
    }

    @PostMapping("/{idComprobante}/reenviar")
    public RespuestaComprobanteSunat reenviar(@PathVariable Long idComprobante) {
        return transacciones.execute(status -> reenviarComprobante(idComprobante));
    }

    private RespuestaComprobanteSunat reenviarComprobante(Long idComprobante) {
        var cpe = repo.buscarComprobantePorId(idComprobante);
        if (cpe == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado.");
        repo.bloquearPago(cpe.idPago());
        cpe = repo.buscarComprobantePorId(idComprobante);
        var vigente = repo.buscarComprobantePorPago(cpe.idPago());
        if (vigente != null && !vigente.idComprobante().equals(idComprobante)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Use el comprobante vigente de este pago.");
        }
        if (!cpe.estadoSunat().startsWith("PENDIENTE_")) return cpe;
        var resultado = servicioSunat.reenviar(cpe, repo.obtenerXmlComprobante(idComprobante));
        repo.actualizarEmision(idComprobante, resultado);
        return repo.buscarComprobantePorId(idComprobante);
    }

    @GetMapping("/{idComprobante}")
    public RespuestaComprobanteSunat buscarPorId(@PathVariable Long idComprobante) {
        RespuestaComprobanteSunat cpe = repo.buscarComprobantePorId(idComprobante);
        if (cpe == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante electrónico no encontrado.");
        }
        return cpe;
    }

    @GetMapping("/por-pago/{idPago}")
    public RespuestaComprobanteSunat buscarPorPago(@PathVariable Long idPago) {
        RespuestaComprobanteSunat cpe = repo.buscarComprobantePorPago(idPago);
        if (cpe == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe comprobante para el pago " + idPago + ".");
        }
        return cpe;
    }

    @GetMapping(value = "/{idComprobante}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarTicketPdf(@PathVariable Long idComprobante) {
        RespuestaComprobanteSunat cpe = repo.buscarComprobantePorId(idComprobante);
        if (cpe == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante electrónico no encontrado.");
        }
        var pedido = pedidos.buscarResumen(cpe.idPedido());
        String codigoPedido = pedido != null ? pedido.codigoPedido() : "PED-" + cpe.idPedido();

        byte[] pdf = generadorTicket.generarTicket80mm(cpe, codigoPedido);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + cpe.numeroCompleto() + ".pdf")
                .body(pdf);
    }

    @GetMapping(value = "/{idComprobante}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> descargarXmlUbl(@PathVariable Long idComprobante) {
        String xml = repo.obtenerXmlComprobante(idComprobante);
        if (xml == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "XML del comprobante no encontrado.");
        }
        RespuestaComprobanteSunat cpe = repo.buscarComprobantePorId(idComprobante);
        String nombreArchivo = (cpe != null ? cpe.rucEmisor() + "-" + (cpe.tipoComprobante().equalsIgnoreCase("FACTURA") ? "01" : "03") + "-" + cpe.numeroCompleto() : "cpe-" + idComprobante) + ".xml";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo)
                .body(xml.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping(value = "/demostracion/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarPdf(@Valid @RequestBody SolicitudPdfDemostracion solicitud) {
        var pedido = pedidos.buscarResumen(solicitud.idPedido());
        if (pedido == null || pedido.importeTotal() == null || pedido.importeTotal().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido no tiene un total válido para generar la vista previa.");
        }
        var datos = servicio.preparar(new SolicitudDatosComprobante(
                solicitud.tipoComprobante(), solicitud.modalidad(),
                solicitud.tipoDocumento(), solicitud.numeroDocumento(),
                solicitud.nombreCompleto(), solicitud.direccion(),
                solicitud.telefono(), solicitud.correo()
        ));
        byte[] pdf = generadorPdf.generar(pedido.codigoPedido(), pedido.importeTotal(), datos);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=NullPointerPOS-Demo-" + pedido.idPedido() + ".pdf")
                .body(pdf);
    }
}
