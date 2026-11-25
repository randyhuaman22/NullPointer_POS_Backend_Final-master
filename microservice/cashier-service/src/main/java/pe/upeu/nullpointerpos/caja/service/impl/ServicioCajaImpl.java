package pe.upeu.nullpointerpos.caja.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.caja.client.ClientePedidos;
import pe.upeu.nullpointerpos.caja.dto.request.*;
import pe.upeu.nullpointerpos.caja.dto.response.*;
import pe.upeu.nullpointerpos.caja.repository.RepositorioCaja;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.security.SecureRandom;

/** [HU-13..HU-16 / HU-20 / T-29..T-48 START] Casos de uso de caja, pagos y outbox. */
@Service
public class ServicioCajaImpl {
    private static final SecureRandom GENERADOR_SEGURO = new SecureRandom();
    private final RepositorioCaja repo;
    private final ClientePedidos pedidos;
    private final ObjectMapper json;

    public ServicioCajaImpl(RepositorioCaja repo, ClientePedidos pedidos, ObjectMapper json) {
        this.repo = repo;
        this.pedidos = pedidos;
        this.json = json;
    }

    // [T-29..T-32 START] Apertura con regla única de un turno abierto por cajero.
    @Transactional
    public RespuestaTurnoCaja abrir(Long idCajero, String codigo, SolicitudAperturaCaja solicitud) {
        if (repo.turnoAbierto(idCajero) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El cajero ya tiene un turno abierto.");
        }
        try {
            Long idTurno = repo.abrir(idCajero, codigo, escala(solicitud.montoInicial()));
            return repo.turnoPorId(idTurno);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un turno abierto para este cajero.", ex);
        }
    }

    public RespuestaTurnoCaja turnoAbierto(Long idCajero) {
        RespuestaTurnoCaja turno = repo.turnoAbierto(idCajero);
        if (turno == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No hay un turno de caja abierto para el cajero.");
        }
        return turno;
    }
    // [T-29..T-32 END]

    // [T-33..T-38 START] Cobro efectivo, vuelto, idempotencia y evento transaccional.
    @Transactional
    public RespuestaPago cobrarEfectivo(Long cajero, String codigo, SolicitudCobroEfectivo solicitud) {
        RespuestaPago previo = recuperarPagoIdempotente(solicitud.idempotencia(), solicitud.idPedido(), "EFECTIVO");
        if (previo != null) {
            return previo;
        }
        var turno = turnoAbierto(cajero);
        var pedido = validarPedido(solicitud.idPedido());
        BigDecimal recibido = escala(solicitud.importeRecibido());
        BigDecimal total = escala(pedido.importeTotal());
        if (recibido.compareTo(total) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El efectivo recibido no cubre el total del pedido.");
        }

        BigDecimal vuelto = recibido.subtract(total).setScale(2, RoundingMode.HALF_UP);
        try {
            Long idPago = repo.insertarPago(solicitud.idPedido(), turno.idTurno(), cajero, codigo,
                    "EFECTIVO", total, recibido, vuelto, null, null, solicitud.idempotencia());
            repo.insertarParte(idPago, "EFECTIVO", total, null, null);
            crearEvento(idPago, solicitud.idPedido(), cajero, codigo, "EFECTIVO", total);
            return repo.buscarPago(idPago);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La clave de idempotencia ya fue utilizada por otro cobro.", ex);
        }
    }
    // [T-33..T-38 END]

    // [T-39..T-43 START] Yape/Plin; la referencia digital es única en toda la BD de caja.
    @Transactional
    public RespuestaPago cobrarDigital(Long cajero, String codigo, SolicitudCobroDigital solicitud) {
        String metodo = solicitud.metodo().toUpperCase(Locale.ROOT);
        RespuestaPago previo = recuperarPagoIdempotente(solicitud.idempotencia(), solicitud.idPedido(), metodo);
        if (previo != null) {
            return previo;
        }
        String referencia = solicitud.numeroOperacion().trim();
        if (repo.existeNumeroOperacion(referencia)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La referencia de operación ya fue utilizada.");
        }

        var turno = turnoAbierto(cajero);
        var pedido = validarPedido(solicitud.idPedido());
        BigDecimal total = escala(pedido.importeTotal());
        try {
            Long idPago = repo.insertarPago(solicitud.idPedido(), turno.idTurno(), cajero, codigo,
                    metodo, total, total, BigDecimal.ZERO, referencia,
                    solicitud.ultimosCuatroDigitos(), solicitud.idempotencia());
            repo.insertarParte(idPago, metodo, total, referencia, solicitud.ultimosCuatroDigitos());
            crearEvento(idPago, solicitud.idPedido(), cajero, codigo, metodo, total);
            return repo.buscarPago(idPago);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La referencia digital o la clave de idempotencia ya fue utilizada.", ex);
        }
    }
    // [T-39..T-43 END]

    // [T-44..T-48 START] Las partes y el evento se escriben en una única transacción.
    @Transactional
    public RespuestaPago cobrarMixto(Long cajero, String codigo, SolicitudPagoMixto solicitud) {
        RespuestaPago previo = recuperarPagoIdempotente(solicitud.idempotencia(), solicitud.idPedido(), "MIXTO");
        if (previo != null) {
            return previo;
        }
        var turno = turnoAbierto(cajero);
        var pedido = validarPedido(solicitud.idPedido());
        BigDecimal total = escala(pedido.importeTotal());
        BigDecimal suma = BigDecimal.ZERO;
        Set<String> referenciasDeEstaSolicitud = new HashSet<>();

        for (PartePago parte : solicitud.partes()) {
            BigDecimal importe = escala(parte.importe());
            if (importe.signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada parte de pago debe ser positiva.");
            }
            suma = suma.add(importe);
            String metodo = parte.metodo().toUpperCase(Locale.ROOT);
            if (!Set.of("EFECTIVO", "YAPE", "PLIN", "TARJETA").contains(metodo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Método de pago mixto no permitido.");
            }
            if (Set.of("YAPE", "PLIN", "TARJETA").contains(metodo)
                    && (parte.numeroOperacion() == null || parte.numeroOperacion().isBlank()
                    || parte.ultimosCuatroDigitos() == null
                    || !parte.ultimosCuatroDigitos().matches("[0-9]{4}"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Las partes digitales (Yape, Plin o Tarjeta) requieren número de operación/voucher y últimos cuatro dígitos.");
            }
            if (Set.of("YAPE", "PLIN", "TARJETA").contains(metodo)) {
                String referencia = parte.numeroOperacion().trim();
                if (repo.existeNumeroOperacion(referencia) || !referenciasDeEstaSolicitud.add(referencia)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "La referencia digital/voucher ya fue utilizada.");
                }
            }
            if ("EFECTIVO".equals(metodo)
                    && parte.numeroOperacion() != null && !parte.numeroOperacion().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El efectivo no debe tener número de operación digital.");
            }
        }
        if (suma.setScale(2, RoundingMode.HALF_UP).compareTo(total) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La suma de las partes debe ser exactamente igual al total del pedido.");
        }

        try {
            Long idPago = repo.insertarPago(solicitud.idPedido(), turno.idTurno(), cajero, codigo,
                    "MIXTO", total, total, BigDecimal.ZERO, null, null, solicitud.idempotencia());
            for (PartePago parte : solicitud.partes()) {
                String referencia = parte.numeroOperacion() == null || parte.numeroOperacion().isBlank()
                        ? null : parte.numeroOperacion().trim();
                repo.insertarParte(idPago, parte.metodo().toUpperCase(Locale.ROOT),
                        escala(parte.importe()), referencia, parte.ultimosCuatroDigitos());
            }
            crearEvento(idPago, solicitud.idPedido(), cajero, codigo, "MIXTO", total);
            return repo.buscarPago(idPago);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Una referencia digital o clave de idempotencia ya fue utilizada.", ex);
        }
    }
    // [T-44..T-48 END]

    // [SPRINT 3 / HU-20 / S3-T-17..S3-T-19 START] El arqueo esperado suma apertura y partes cobradas en efectivo.
    @Transactional
    public RespuestaCierreCaja cerrar(Long cajero, SolicitudCierreCaja solicitud) {
        var turno = turnoAbierto(cajero);
        BigDecimal efectivo = repo.totalEfectivo(turno.idTurno());
        BigDecimal esperado = escala(turno.montoInicial()).add(efectivo).setScale(2, RoundingMode.HALF_UP);
        var cierre = repo.cerrar(turno.idTurno(), esperado, escala(solicitud.importeReal()), solicitud.motivo());
        if (cierre == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El turno ya está cerrado.");
        }
        return cierre;
    }
    // [SPRINT 3 / HU-20 / S3-T-17..S3-T-19 END]

    private ClientePedidos.ResumenPedido validarPedido(Long idPedido) {
        var pedido = pedidos.buscarResumen(idPedido);
        if (pedido == null || pedido.importeTotal() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pudo obtener el total del pedido.");
        }
        if (Set.of("BORRADOR", "PAGADO", "ANULADO").contains(pedido.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pedido debe estar enviado, pendiente o listo y no puede estar pagado/anulado.");
        }
        if (pedido.importeTotal().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El importe del pedido debe ser mayor que cero antes de cobrar.");
        }
        return pedido;
    }

    private RespuestaPago recuperarPagoIdempotente(String idempotencia, Long idPedido, String metodo) {
        if (idempotencia == null || idempotencia.isBlank()) {
            return null;
        }
        RespuestaPago existente = repo.buscarPagoPorIdempotencia(idempotencia);
        if (existente == null) {
            return null;
        }
        if (Objects.equals(existente.idPedido(), idPedido) && metodo.equalsIgnoreCase(existente.metodo())) {
            return existente;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave de idempotencia ya fue utilizada.");
    }

    private BigDecimal escala(BigDecimal numero) {
        return numero.setScale(2, RoundingMode.HALF_UP);
    }

    private void crearEvento(Long idPago, Long idPedido, Long cajero, String codigo,
                             String metodo, BigDecimal importe) {
        try {
            Map<String, Object> evento = new LinkedHashMap<>();
            evento.put("idPago", idPago);
            evento.put("idPedido", idPedido);
            evento.put("importe", importe);
            evento.put("metodo", metodo);
            evento.put("idCajero", cajero);
            evento.put("codigoCajero", codigo);
            evento.put("fechaConfirmacion", OffsetDateTime.now().toString());
            repo.guardarEvento(json.writeValueAsString(evento));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo preparar el evento de pago.", ex);
        }
    }
    public RespuestaSimuladorTarjeta simularTarjeta(SolicitudSimularTarjeta s) {
        String num = s.numeroTarjeta().replaceAll("[^0-9]", "");
        if (num.length() < 13 || num.length() > 19) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Longitud de tarjeta inválida (debe tener entre 13 y 19 dígitos).");
        }
        if (!validarLuhn(num)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de tarjeta no es válido (falla de verificación algorítmica Luhn).");
        }
        int mesActual = LocalDate.now().getMonthValue();
        int anioActual = LocalDate.now().getYear();
        if (s.anioVencimiento() < anioActual || (s.anioVencimiento() == anioActual && s.mesVencimiento() < mesActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La tarjeta ingresada se encuentra vencida.");
        }
        String marca = detectarMarca(num);
        String ultimos4 = num.substring(num.length() - 4);
        int rand = 100000 + GENERADOR_SEGURO.nextInt(899999);
        String auth = "AUTH-" + rand;
        String voucher = "VOUCH-" + (100000 + GENERADOR_SEGURO.nextInt(899999));
        return new RespuestaSimuladorTarjeta(
            true, marca, auth, voucher, ultimos4, s.titular().trim().toUpperCase(Locale.ROOT),
            "Pago con tarjeta " + marca + " aprobado exitosamente por la entidad bancaria simulada."
        );
    }

    private boolean validarLuhn(String n) {
        int sum = 0;
        boolean alternate = false;
        for (int i = n.length() - 1; i >= 0; i--) {
            int d = n.charAt(i) - '0';
            if (alternate) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    private String detectarMarca(String n) {
        if (n.startsWith("4")) return "VISA";
        if (n.matches("^5[1-5].*") || n.matches("^2(22[1-9]|2[3-9][0-9]|[3-6][0-9]{2}|7[01][0-9]|720).*")) return "MASTERCARD";
        if (n.startsWith("34") || n.startsWith("37")) return "AMEX";
        if (n.startsWith("36") || n.startsWith("38") || n.startsWith("30")) return "DINERS";
        return "TARJETA";
    }
}
/** [HU-13..HU-16 / HU-20 / T-29..T-48 END] */
