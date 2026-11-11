package pe.upeu.nullpointerpos.pedidos.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import pe.upeu.nullpointerpos.pedidos.dto.request.*;
import pe.upeu.nullpointerpos.pedidos.dto.response.*;
import pe.upeu.nullpointerpos.pedidos.dto.event.EventoPagoConfirmado;
import pe.upeu.nullpointerpos.pedidos.entity.Mesa;
import pe.upeu.nullpointerpos.pedidos.entity.Pedido;
import pe.upeu.nullpointerpos.pedidos.mapper.PedidoMapper;
import pe.upeu.nullpointerpos.pedidos.repository.MesaRepositorio;
import pe.upeu.nullpointerpos.pedidos.repository.PedidoRepositorio;
import pe.upeu.nullpointerpos.pedidos.service.ServicioPedidos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ServicioPedidosImpl implements ServicioPedidos {
    private final PedidoRepositorio pedidos;
    private final MesaRepositorio mesas;
    private final pe.upeu.nullpointerpos.pedidos.client.ClienteProductos productos;
    private final PedidoMapper mapper;
    private final ObjectMapper objectMapper;

    public ServicioPedidosImpl(PedidoRepositorio pedidos, MesaRepositorio mesas,
                               pe.upeu.nullpointerpos.pedidos.client.ClienteProductos productos,
                               PedidoMapper mapper, ObjectMapper objectMapper) {
        this.pedidos = pedidos; this.mesas = mesas; this.productos = productos;
        this.mapper = mapper; this.objectMapper = objectMapper;
    }

    @Override @Transactional
    public RespuestaPedido crear(SolicitudCrearPedido solicitud, String codigoMozo, Long idUsuarioMozo) {
        return crearPedidoInterno(
                solicitud.idMesa(), solicitud.observaciones(), solicitud.detalles(),
                solicitud.idempotencia(), false, null, codigoMozo, idUsuarioMozo, false
        );
    }

    @Override @Transactional
    public RespuestaPedido sincronizarOffline(SolicitudSincronizarPedidoOffline solicitud,
                                              String codigoMozo, Long idUsuarioMozo, boolean administrador) {
        boolean enviar = solicitud.enviarACocina() == null || solicitud.enviarACocina();
        return crearPedidoInterno(
                solicitud.idMesa(), solicitud.observaciones(), solicitud.detalles(),
                solicitud.idempotencia(), enviar, solicitud.fechaRegistroOffline(),
                codigoMozo, idUsuarioMozo, administrador
        );
    }

    @Override
    public RespuestaPedido buscarPorIdempotencia(String idempotencia, String codigoTrabajador, boolean administrador) {
        Long idPedido = pedidos.buscarIdPorIdempotencia(idempotencia)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado."));
        Pedido pedido = pedidos.buscarEstado(idPedido)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado."));
        verificarPropietario(pedido, codigoTrabajador, administrador);
        return cargarPedido(idPedido);
    }

    private RespuestaPedido crearPedidoInterno(Long idMesa, String observaciones, List<SolicitudDetallePedido> detalles,
                                               String idempotenciaRaw, boolean enviarACocina,
                                               OffsetDateTime fechaOffline, String codigoMozo, Long idUsuarioMozo,
                                               boolean administrador) {
        String llave = (idempotenciaRaw == null || idempotenciaRaw.isBlank()) ? null : idempotenciaRaw.trim();
        if (llave != null) {
            var existenteOpt = pedidos.buscarIdPorIdempotencia(llave);
            if (existenteOpt.isPresent()) {
                Long idExistente = existenteOpt.get();
                Pedido previo = bloquearPedido(idExistente);
                verificarPropietario(previo, codigoMozo, administrador);
                if (enviarACocina && "BORRADOR".equals(previo.estado())) {
                    return transicionarYPublicarEnvioCocina(idExistente, idUsuarioMozo);
                }
                return cargarPedido(idExistente);
            }
        }

        Mesa mesa = bloquearMesa(idMesa);
        if (!"LIBRE".equals(mesa.estado())) {
            if (mesas.perteneceAGrupoActivoNoPrincipal(mesa.idMesa())
                    || !mesas.puedeAbrirPrimerPedidoGrupo(mesa.idMesa())) {
                String detalleConflicto = pedidos.buscarResumenActivoPorMesa(mesa.idMesa())
                        .map(p -> " (comanda activa #" + p.idPedido() + " de " + p.codigoMozo() + ", estado " + p.estado() + ")")
                        .orElse("");
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La mesa " + mesa.codigoMesa() + " no está libre" + detalleConflicto + ".");
            }
        }
        String codigoPedido = "NP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long idPedido = pedidos.insertarPedido(codigoPedido, mesa.idMesa(), idUsuarioMozo, codigoMozo,
                observaciones, llave, fechaOffline);
        reemplazarDetalles(idPedido, detalles);
        recalcularTotales(idPedido);
        // [SPRINT 1 / HU-02 / HU-07 START] La mesa queda ocupada al abrir una comanda activa.
        mesas.actualizarEstadoSegunPedidos(mesa.idMesa());
        // [SPRINT 1 / HU-02 / HU-07 END]
        String accion = fechaOffline != null ? "PEDIDO_SINCRONIZADO_OFFLINE" : "PEDIDO_CREADO";
        pedidos.registrarAuditoria(idUsuarioMozo, accion, idPedido,
                fechaOffline != null ? "Pedido creado sin conexión (" + fechaOffline + ") y sincronizado." : "Pedido guardado como borrador.");
        if (enviarACocina) {
            return transicionarYPublicarEnvioCocina(idPedido, idUsuarioMozo);
        }
        return cargarPedido(idPedido);
    }

    @Override @Transactional
    public RespuestaPedido actualizarDetalles(Long idPedido, SolicitudModificarDetallesPedido solicitud,
                                              String codigoTrabajador, Long idUsuarioActor, boolean administrador) {
        Pedido pedido = bloquearPedido(idPedido);
        verificarPropietario(pedido, codigoTrabajador, administrador);
        if (!"BORRADOR".equals(pedido.estado())) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Solo se puede modificar un pedido que todavía está en BORRADOR.");
        pedidos.eliminarDetalles(idPedido);
        reemplazarDetalles(idPedido, solicitud.detalles());
        recalcularTotales(idPedido);
        pedidos.actualizarFecha(idPedido);
        pedidos.registrarAuditoria(idUsuarioActor, "DETALLES_PEDIDO_ACTUALIZADOS", idPedido,
                "Se editaron los productos antes de enviar a cocina.");
        return cargarPedido(idPedido);
    }

    @Override @Transactional
    public RespuestaTrasladoMesa trasladar(SolicitudTrasladoMesa solicitud, String codigoTrabajador,
                                          Long idUsuarioActor, boolean administrador) {
        Pedido pedido = bloquearPedido(solicitud.idPedido());
        verificarPropietario(pedido, codigoTrabajador, administrador);
        if (List.of("PAGADO", "ANULADO").contains(pedido.estado())) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede trasladar un pedido pagado o anulado.");
        if (pedido.idMesa().equals(solicitud.idMesaDestino())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El pedido ya está asignado a esa mesa.");
        Mesa destino = bloquearMesa(solicitud.idMesaDestino());
        if (!"LIBRE".equals(destino.estado())) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La mesa de destino debe estar libre.");
        String codigoOrigen = mesas.buscarCodigo(pedido.idMesa()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Mesa de origen no encontrada."));
        Long idMesaOrigen = pedido.idMesa();
        pedidos.actualizarMesa(solicitud.idPedido(), destino.idMesa());
        OffsetDateTime fecha = pedidos.registrarTraslado(solicitud.idPedido(), idMesaOrigen, destino.idMesa(),
                idUsuarioActor, solicitud.motivo());
        // [SPRINT 1 / HU-06 START] Recalcular ambos lados del traslado en la misma transacción.
        mesas.actualizarEstadoSegunPedidos(idMesaOrigen);
        mesas.actualizarEstadoSegunPedidos(destino.idMesa());
        // [SPRINT 1 / HU-06 END]
        pedidos.registrarAuditoria(idUsuarioActor, "MESA_TRASLADADA", solicitud.idPedido(), solicitud.motivo());
        return new RespuestaTrasladoMesa(solicitud.idPedido(), codigoOrigen, destino.codigoMesa(), pedido.estado(), fecha);
    }

    @Override @Transactional
    public RespuestaPedido enviarACocina(Long idPedido, String codigoTrabajador, Long idUsuarioActor, boolean administrador) {
        Pedido pedido = bloquearPedido(idPedido);
        verificarPropietario(pedido, codigoTrabajador, administrador);
        if (List.of("ENVIADO_COCINA", "EN_PREPARACION", "LISTO", "ENTREGADO", "PENDIENTE_PAGO").contains(pedido.estado())) {
            return cargarPedido(idPedido); // reintento idempotente seguro tras reconexión
        }
        if (!"BORRADOR".equals(pedido.estado())) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Solo se puede enviar a cocina un pedido en BORRADOR.");
        if (pedidos.contarDetalles(idPedido) == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Agrega al menos un producto antes de enviar a cocina.");
        return transicionarYPublicarEnvioCocina(idPedido, idUsuarioActor);
    }

    private RespuestaPedido transicionarYPublicarEnvioCocina(Long idPedido, Long idUsuarioActor) {
        pedidos.cambiarEstadoAEnviado(idPedido);
        RespuestaPedido respuesta = cargarPedido(idPedido);
        try {
            Map<String, Object> evento = new LinkedHashMap<>();
            evento.put("tipoEvento", "PEDIDO_ENVIADO_COCINA");
            evento.put("fechaHora", OffsetDateTime.now().toString());
            evento.put("pedido", respuesta);
            pedidos.guardarEventoPendiente(idPedido, objectMapper.writeValueAsString(evento));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo preparar el mensaje para cocina.", ex);
        }
        pedidos.registrarAuditoria(idUsuarioActor, "PEDIDO_ENVIADO_COCINA", idPedido, "Pedido enviado a cocina.");
        return respuesta;
    }

    @Override
    public RespuestaPedido buscarPorId(Long idPedido, String codigoTrabajador, boolean administrador) {
        Pedido pedido = pedidos.buscarEstado(idPedido).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado."));
        verificarPropietario(pedido, codigoTrabajador, administrador);
        return cargarPedido(idPedido);
    }

    @Override
    public List<RespuestaPedido> listarPedidosActivosSalon() {
        return pedidos.listarIdsActivosSalon().stream().map(this::cargarPedido).toList();
    }

    @Override
    public List<RespuestaPedido> listarPendientesCobro() {
        return pedidos.listarIdsPendientesCobro().stream().map(this::cargarPedido).toList();
    }

    @Override
    public RespuestaPedido buscarPendienteCobroPorId(Long idPedido) {
        Pedido pedido = pedidos.buscarEstado(idPedido).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado."));
        if (List.of("PAGADO", "ANULADO", "BORRADOR").contains(pedido.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido no está pendiente de cobro.");
        }
        return cargarPedido(idPedido);
    }

    @Override
    public List<RespuestaPedido> listarPedidosCocina() {
        return pedidos.listarIdsParaCocina().stream().map(this::cargarPedido).toList();
    }

    private void reemplazarDetalles(Long idPedido, List<SolicitudDetallePedido> detalles) {
        for (SolicitudDetallePedido detalle : detalles) {
            pe.upeu.nullpointerpos.pedidos.dto.external.RespuestaProductoCatalogo producto =
                    productos.buscarDisponible(detalle.idProducto());
            var opciones = productos.validarOpciones(detalle.idProducto(), detalle.opciones());
            BigDecimal precioConOpciones = producto.precioUnitario().add(opciones.precioAdicional());
            String observaciones = combinarObservaciones(detalle.observaciones(), opciones.opcionesSeleccionadas());
            pedidos.insertarDetalle(idPedido, producto.idProducto(), producto.nombre(), detalle.cantidad(),
                    precioConOpciones, observaciones, producto.estacionCocina());
        }
    }

    private String combinarObservaciones(String observaciones, List<String> opciones) {
        String seleccion = opciones == null || opciones.isEmpty() ? "" : "Opciones: " + String.join(", ", opciones);
        String notas = observaciones == null || observaciones.isBlank() ? "" : "Notas: " + observaciones.trim();
        String combinado = List.of(seleccion, notas).stream().filter(s -> !s.isBlank())
                .collect(java.util.stream.Collectors.joining(" | "));
        if (combinado.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Las opciones y observaciones combinadas superan el límite de 500 caracteres.");
        }
        return combinado.isBlank() ? null : combinado;
    }

    private void recalcularTotales(Long idPedido) { pedidos.actualizarTotales(idPedido, pedidos.calcularSubtotal(idPedido)); }

    private Mesa bloquearMesa(Long idMesa) {
        return mesas.bloquearPorId(idMesa).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Mesa no encontrada o inactiva."));
    }
    private Pedido bloquearPedido(Long idPedido) {
        return pedidos.buscarBloqueando(idPedido).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado."));
    }
    private void verificarPropietario(Pedido pedido, String codigoTrabajador, boolean administrador) {
        if (!administrador && !pedido.codigoMozo().equalsIgnoreCase(codigoTrabajador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El pedido pertenece a otro mozo.");
        }
    }
    // [HU-08 / T-07 START] La consulta KDS incluye edad del pedido y nivel de demora.
    @Override
    public List<RespuestaPedidoCocina> listarPedidosCocinaConDemora() {
        OffsetDateTime ahora = OffsetDateTime.now();
        return listarPedidosCocina().stream().map(p -> {
            OffsetDateTime base = p.fechaEnvio() == null ? p.fechaCreacion() : p.fechaEnvio();
            boolean listo = "LISTO".equals(p.estado()) || (!p.detalles().isEmpty()
                    && p.detalles().stream().allMatch(d -> List.of("LISTO", "ENTREGADO", "ANULADO").contains(d.estado())));
            CalculadoraDemoraPedido.Resultado demora = CalculadoraDemoraPedido.calcular(base, ahora, listo);
            return new RespuestaPedidoCocina(p.idPedido(), p.codigoPedido(), p.idMesa(), p.codigoMesa(),
                    p.idUsuarioMozo(), p.codigoMozo(), p.estado(), p.observaciones(), p.subtotal(),
                    p.totalDescuento(), p.importeTotal(), p.fechaCreacion(), p.fechaEnvio(), p.fechaPago(),
                    p.detalles(), demora.segundosTranscurridos(), demora.minutosTranscurridos(),
                    demora.nivelDemora(), demora.siguienteUmbralMinutos());
        }).toList();
    }
    // [HU-08 / T-07 END]

    // [HU-10 / T-16..T-19 START] Estado por plato; idempotencia y publicación por Outbox.
    @Override @Transactional
    public RespuestaPedido marcarDetalleListo(Long idDetalle, String actor, Long idUsuarioActor) {
        Long idPedido = pedidos.buscarIdPedidoPorDetalle(idDetalle);
        if (idPedido == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Detalle de plato no encontrado.");
        Pedido pedido = bloquearPedido(idPedido);
        if ("PAGADO".equals(pedido.estado()) || "ANULADO".equals(pedido.estado()) || "BORRADOR".equals(pedido.estado()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido no está en producción de cocina.");
        int changed = pedidos.marcarDetalleListo(idDetalle);
        if (changed == 0) {
            var detalle = pedidos.listarDetalles(idPedido).stream().filter(d -> d.idDetallePedido().equals(idDetalle)).findFirst().orElseThrow();
            if (!List.of("LISTO", "ENTREGADO").contains(detalle.estado())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Transición de estado no permitida para el plato.");
            }
            return cargarPedido(idPedido); // repetición idempotente de marcar listo
        }
        pedidos.actualizarPedidoListoSiCorresponde(idPedido);
        RespuestaPedido respuesta = publicarEventoDetalleCocina(idPedido);
        pedidos.registrarAuditoria(idUsuarioActor, "PLATO_MARCADO_LISTO", idPedido, "Detalle=" + idDetalle + "; actor=" + actor);
        return respuesta;
    }

    @Override @Transactional
    public RespuestaPedido marcarPedidoListoCocina(Long idPedido, String actor, Long idUsuarioActor) {
        Pedido pedido = bloquearPedido(idPedido);
        if ("PAGADO".equals(pedido.estado()) || "ANULADO".equals(pedido.estado()) || "BORRADOR".equals(pedido.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido no está en producción de cocina.");
        }
        int changed = pedidos.marcarTodosDetallesListos(idPedido);
        if (changed == 0) {
            return cargarPedido(idPedido);
        }
        pedidos.actualizarPedidoListoSiCorresponde(idPedido);
        RespuestaPedido respuesta = publicarEventoDetalleCocina(idPedido);
        pedidos.registrarAuditoria(idUsuarioActor, "PEDIDO_MARCADO_LISTO", idPedido, "Pedido completo listo; actor=" + actor);
        return respuesta;
    }

    private RespuestaPedido publicarEventoDetalleCocina(Long idPedido) {
        RespuestaPedido respuesta = cargarPedido(idPedido);
        try {
            Map<String, Object> evento = new LinkedHashMap<>();
            evento.put("tipoEvento", "DETALLE_PLATO_ACTUALIZADO");
            evento.put("fechaHora", OffsetDateTime.now().toString());
            evento.put("pedido", respuesta);
            pedidos.guardarEventoPendiente(idPedido, "DETALLE_PLATO_ACTUALIZADO", objectMapper.writeValueAsString(evento));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo preparar el evento de cocina.", ex);
        }
        return respuesta;
    }
    // [HU-10 / T-16..T-19 END]

    // [SPRINT 3 / HU-17 / S3-T-03 START] Solo caja/administración pueden descontar; el motivo queda auditado.
    @Override @Transactional
    public RespuestaPedido aplicarDescuento(Long idPedido, SolicitudDescuentoPedido solicitud, String actor, Long idUsuarioActor, boolean autorizado) {
        if (!autorizado) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo caja o administración puede aplicar descuentos.");
        Pedido pedido = bloquearPedido(idPedido);
        if (List.of("PAGADO","ANULADO","BORRADOR").contains(pedido.estado())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se descuenta un pedido enviado y no pagado.");
        if (solicitud.porcentaje().compareTo(BigDecimal.ZERO) <= 0 || solicitud.porcentaje().compareTo(new BigDecimal("100")) >= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El porcentaje debe ser mayor que 0 y menor que 100.");
        if (pedido.importeTotal() == null || pedido.importeTotal().signum() <= 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede aplicar descuento a un pedido con total cero.");
        pedidos.aplicarDescuento(idPedido, solicitud.porcentaje().setScale(2, RoundingMode.HALF_UP), actor, idUsuarioActor, solicitud.motivo());
        return cargarPedido(idPedido);
    }
    // [SPRINT 3 / HU-17 / S3-T-03 END]

    // [SPRINT 3 / HU-29 / S3-T-51..S3-T-52 START] Anular pedido conservando registro de auditoría.
    @Override @Transactional
    public RespuestaPedido anular(Long idPedido, SolicitudAnularPedido solicitud, String actor, Long idUsuarioActor, boolean autorizado) {
        if (!autorizado) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo administración puede anular pedidos.");
        Pedido pedido = bloquearPedido(idPedido);
        if (List.of("PAGADO","ANULADO").contains(pedido.estado())) throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya está pagado o anulado.");
        if (pedidos.anularPedido(idPedido, solicitud.motivo()) == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo anular el pedido.");
        // [SPRINT 3 / HU-29 START] Liberar la mesa solo si no quedan otros pedidos activos.
        mesas.actualizarEstadoSegunPedidos(pedido.idMesa());
        // [SPRINT 3 / HU-29 END]
        pedidos.registrarAuditoria(idUsuarioActor, "PEDIDO_ANULADO", idPedido, solicitud.motivo() + "; actor=" + actor);
        RespuestaPedido respuesta = cargarPedido(idPedido);
        try {
            Map<String,Object> evento = new LinkedHashMap<>(); evento.put("tipoEvento","PEDIDO_ANULADO");
            evento.put("fechaHora",OffsetDateTime.now().toString()); evento.put("pedido",respuesta);
            pedidos.guardarEventoPendiente(idPedido,"PEDIDO_ANULADO",objectMapper.writeValueAsString(evento));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("No se pudo preparar evento de anulación.",ex); }
        return respuesta;
    }
    // [SPRINT 3 / HU-29 / S3-T-51..S3-T-52 END]

    @Override
    public RespuestaResumenPago resumenPago(Long idPedido) {
        var r = pedidos.resumenPago(idPedido);
        if (r == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado.");
        return new RespuestaResumenPago(r.idPedido(),r.codigoPedido(),r.estado(),r.importeTotal(),r.totalDescuento());
    }

    // [HU-14..HU-16 START] Evento de caja consumido; nunca se escribe directamente en otra BD.
    @Override @Transactional
    public void registrarPagoConfirmado(EventoPagoConfirmado evento) {
        if (evento == null || evento.idPedido() == null || evento.idPago() == null || evento.importe() == null
                || evento.metodo() == null || evento.idCajero() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El evento de pago está incompleto.");
        }
        Pedido pedido = bloquearPedido(evento.idPedido());
        if ("PAGADO".equals(pedido.estado())) return; // duplicado RabbitMQ: idempotente
        if ("ANULADO".equals(pedido.estado())) throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede pagar un pedido anulado.");
        if ("BORRADOR".equals(pedido.estado())) throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede pagar un pedido que no fue enviado a cocina.");
        if (pedido.importeTotal() == null || pedido.importeTotal().compareTo(evento.importe()) != 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El importe confirmado no coincide con el total del pedido.");
        if (pedidos.registrarPagoConfirmadoSiNoPagado(evento.idPedido()) == 0) return;
        // [SPRINT 3 / HU-20 START] Tras confirmar el pago, actualizar el estado operativo de la mesa.
        mesas.actualizarEstadoSegunPedidos(pedido.idMesa());
        // [SPRINT 3 / HU-20 END]
        pedidos.registrarAuditoria(evento.idCajero(), "PAGO_CONFIRMADO", evento.idPedido(), "Pago=" + evento.idPago() + "; método=" + evento.metodo());
        RespuestaPedido respuesta = cargarPedido(evento.idPedido());
        try {
            Map<String,Object> out = new LinkedHashMap<>(); out.put("tipoEvento","PEDIDO_PAGADO");
            out.put("fechaHora",OffsetDateTime.now().toString()); out.put("pago",evento); out.put("pedido",respuesta);
            pedidos.guardarEventoPendiente(evento.idPedido(), "PEDIDO_PAGADO", objectMapper.writeValueAsString(out));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("No se pudo preparar evento de pedido pagado.", ex); }
    }
    // [HU-14..HU-16 END]

    private RespuestaPedido cargarPedido(Long idPedido) {
        Pedido encabezado = pedidos.buscarEncabezado(idPedido).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado."));
        return mapper.aRespuesta(encabezado, pedidos.listarDetalles(idPedido));
    }
}
