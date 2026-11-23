package pe.upeu.nullpointerpos.caja.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.caja.dto.request.SolicitudDatosComprobante;
import pe.upeu.nullpointerpos.caja.dto.response.RespuestaDatosComprobante;
import pe.upeu.nullpointerpos.caja.repository.RepositorioCaja;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-60..S3-T-62 START]
 * Gestiona las opciones de datos de comprador con caché local, consulta externa (APISPERU/Decolecta) y respaldo demo.
 */
@Service
public class ServicioDatosComprobante {
    private final RepositorioCaja repositorio;
    private final DirectorioDemostracionDocumentos directorio;
    private final ServicioConsultaDocumentoExterno consultaExterna;

    public ServicioDatosComprobante(RepositorioCaja repositorio, DirectorioDemostracionDocumentos directorio) {
        this(repositorio, directorio, null);
    }

    @Autowired
    public ServicioDatosComprobante(
            RepositorioCaja repositorio,
            DirectorioDemostracionDocumentos directorio,
            ServicioConsultaDocumentoExterno consultaExterna) {
        this.repositorio = repositorio;
        this.directorio = directorio;
        this.consultaExterna = consultaExterna;
    }

    public RespuestaDatosComprobante preparar(SolicitudDatosComprobante s) {
        if (s == null) throw error(HttpStatus.BAD_REQUEST, "Debe enviar los datos del comprobante.");
        String tipo = normalizar(s.tipoComprobante()), modalidad = normalizar(s.modalidad());
        if (!tipo.equals("BOLETA") && !tipo.equals("FACTURA")) throw error(HttpStatus.BAD_REQUEST, "El tipo debe ser BOLETA o FACTURA.");
        return switch (modalidad) {
            case "SIN_DATOS" -> sinDatos(tipo);
            case "MANUAL" -> manual(tipo, s);
            case "DNI" -> consultarDocumento(tipo, "DNI", s.numeroDocumento());
            case "RUC" -> consultarDocumento(tipo, "RUC", s.numeroDocumento());
            default -> throw error(HttpStatus.BAD_REQUEST, "Modalidad no válida. Use SIN_DATOS, MANUAL, DNI o RUC.");
        };
    }

    /** Consulta unificada: 1) BD PostgreSQL -> 2) API externa (guardando en caché) -> 3) Directorio demo. */
    public Optional<Map<String, Object>> consultarCliente(String tipoDocumento, String numeroDocumento) {
        validarDocumento(tipoDocumento, numeroDocumento);
        String tipo = normalizar(tipoDocumento);
        String numero = numeroDocumento.trim();

        Map<String, Object> local = repositorio.buscarCliente(tipo, numero);
        String nombreLocal = local == null ? null : limpiar(texto(local.get("nombreCompleto")));
        if (nombreLocal != null) {
            Map<String, Object> respuesta = new LinkedHashMap<>(local);
            respuesta.putIfAbsent("tipoDocumento", tipo);
            respuesta.putIfAbsent("numeroDocumento", numero);
            respuesta.put("nombreCompleto", nombreLocal);
            respuesta.put("origenDatos", "REGISTRO_LOCAL");
            respuesta.put("mensaje", "Datos del registro local; confirme que estén actualizados.");
            return Optional.of(respuesta);
        }

        if (consultaExterna != null && consultaExterna.estaConfigurado()) {
            var externo = consultaExterna.consultar(tipo, numero);
            if (externo.isPresent()) {
                var d = externo.get();
                Map<String, Object> respuesta = new LinkedHashMap<>();
                try {
                    Map<String, Object> guardado = repositorio.guardarCliente(
                            d.tipoDocumento(), d.numeroDocumento(), d.nombreCompleto(), null, null, d.direccion());
                    if (guardado != null && !guardado.isEmpty()) {
                        respuesta.putAll(guardado);
                    }
                } catch (RuntimeException ignored) {
                    // Si falla el cacheo en BD, devolvemos igual los datos verificados al cajero
                }
                respuesta.put("tipoDocumento", d.tipoDocumento());
                respuesta.put("numeroDocumento", d.numeroDocumento());
                respuesta.put("nombreCompleto", d.nombreCompleto());
                respuesta.put("razonSocial", d.nombreCompleto());
                respuesta.put("direccion", d.direccion());
                if (!d.estado().isBlank()) respuesta.put("estadoSunat", d.estado());
                if (!d.condicion().isBlank()) respuesta.put("condicionSunat", d.condicion());
                respuesta.put("origenDatos", d.origenDatos());
                respuesta.put("mensaje", "Datos verificados automáticamente vía " + d.origenDatos() + ".");
                return Optional.of(respuesta);
            }
        }

        var demo = directorio.buscar(tipo, numero);
        if (demo.isPresent()) {
            var d = demo.get();
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("tipoDocumento", d.tipoDocumento());
            respuesta.put("numeroDocumento", d.numeroDocumento());
            respuesta.put("nombreCompleto", d.nombreCompleto());
            respuesta.put("razonSocial", d.nombreCompleto());
            respuesta.put("direccion", d.direccion());
            respuesta.put("origenDatos", "DEMO_LOCAL");
            respuesta.put("mensaje", "Datos ficticios de demostración; no proceden de RENIEC ni SUNAT.");
            return Optional.of(respuesta);
        }

        return Optional.empty();
    }

    public boolean consultaExternaConfigurada() {
        return consultaExterna != null && consultaExterna.estaConfigurado();
    }

    private RespuestaDatosComprobante sinDatos(String tipo) {
        if (tipo.equals("FACTURA")) throw error(HttpStatus.BAD_REQUEST, "La factura requiere RUC y razón social del comprador.");
        return new RespuestaDatosComprobante(tipo, "SIN_DATOS", false, null, null, null, null, null, null,
                "SIN_DATOS", "Boleta preparada sin datos identificativos. No es una emisión tributaria.");
    }

    private RespuestaDatosComprobante manual(String tipo, SolicitudDatosComprobante s) {
        String nombre = limpiar(s.nombreCompleto()), documento = limpiar(s.numeroDocumento());
        String tipoDocumento = normalizarOpcional(s.tipoDocumento());
        String direccion = limpiar(s.direccion());
        if (nombre == null) throw error(HttpStatus.BAD_REQUEST, "Ingrese el nombre del cliente o razón social.");
        if (tipo.equals("FACTURA")) {
            if (!"RUC".equals(tipoDocumento) || documento == null || !documento.matches("[0-9]{11}"))
                throw error(HttpStatus.BAD_REQUEST, "Para factura manual se requiere RUC de 11 dígitos.");
            if (direccion == null) throw error(HttpStatus.BAD_REQUEST, "Para factura manual ingrese la dirección del comprador.");
        }
        if (documento != null) {
            if (tipoDocumento == null) throw error(HttpStatus.BAD_REQUEST, "Seleccione DNI o RUC para el documento ingresado.");
            validarDocumento(tipoDocumento, documento);
            repositorio.guardarCliente(tipoDocumento, documento, nombre, limpiar(s.telefono()), limpiar(s.correo()), direccion);
        }
        return new RespuestaDatosComprobante(tipo, "MANUAL", true, tipoDocumento, documento, nombre,
                direccion, limpiar(s.telefono()), limpiar(s.correo()), "INGRESO_MANUAL",
                "Datos ingresados manualmente. Verifique su exactitud antes de continuar.");
    }

    private RespuestaDatosComprobante consultarDocumento(String tipoComprobante, String tipoDocumento, String numero) {
        validarDocumento(tipoDocumento, numero);
        if (tipoDocumento.equals("DNI") && !tipoComprobante.equals("BOLETA")) throw error(HttpStatus.BAD_REQUEST, "Para factura se debe identificar al comprador mediante RUC.");
        if (tipoDocumento.equals("RUC") && !tipoComprobante.equals("FACTURA")) throw error(HttpStatus.BAD_REQUEST, "Para consultar RUC seleccione FACTURA.");

        var encontrado = consultarCliente(tipoDocumento, numero);
        if (encontrado.isPresent()) {
            var m = encontrado.get();
            return new RespuestaDatosComprobante(
                    tipoComprobante,
                    tipoDocumento,
                    true,
                    texto(m.get("tipoDocumento")),
                    texto(m.get("numeroDocumento")),
                    texto(m.get("nombreCompleto")),
                    limpiar(texto(m.get("direccion"))),
                    limpiar(texto(m.get("telefono"))),
                    limpiar(texto(m.get("correo"))),
                    texto(m.get("origenDatos")),
                    texto(m.get("mensaje"))
            );
        }
        throw error(HttpStatus.NOT_FOUND, "Documento no encontrado en el registro local de prueba. Registre los datos manualmente o use el documento de demostración documentado.");
    }

    private void validarDocumento(String tipo, String numero) {
        if (tipo == null || numero == null) throw error(HttpStatus.BAD_REQUEST, "Indique tipo y número de documento.");
        String t = tipo.toUpperCase(Locale.ROOT);
        if (t.equals("DNI") && !numero.matches("[0-9]{8}")) throw error(HttpStatus.BAD_REQUEST, "DNI debe contener exactamente 8 dígitos.");
        if (t.equals("RUC") && !numero.matches("[0-9]{11}")) throw error(HttpStatus.BAD_REQUEST, "RUC debe contener exactamente 11 dígitos.");
        if (!t.equals("DNI") && !t.equals("RUC")) throw error(HttpStatus.BAD_REQUEST, "Tipo de documento permitido: DNI o RUC.");
    }

    private String normalizar(String v) { return v == null ? "" : v.trim().toUpperCase(Locale.ROOT); }
    private String normalizarOpcional(String v) { return v == null || v.isBlank() ? null : v.trim().toUpperCase(Locale.ROOT); }
    private String limpiar(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    private String texto(Object v) { return v == null ? null : String.valueOf(v); }
    private ResponseStatusException error(HttpStatus estado, String mensaje) { return new ResponseStatusException(estado, mensaje); }
}
/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-60..S3-T-62 END] */
