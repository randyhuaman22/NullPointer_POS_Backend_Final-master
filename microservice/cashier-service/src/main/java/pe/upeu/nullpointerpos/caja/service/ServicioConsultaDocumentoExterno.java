package pe.upeu.nullpointerpos.caja.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Cliente HTTP para consulta en línea de DNI (RENIEC) y RUC (SUNAT).
 * Soporta APISPERU (recomendado por defecto) y Decolecta / apis.net.pe.
 */
@Service
public class ServicioConsultaDocumentoExterno {
    private static final Logger LOG = LoggerFactory.getLogger(ServicioConsultaDocumentoExterno.class);

    public static final String PROVEEDOR_APISPERU = "APISPERU";
    public static final String PROVEEDOR_DECOLECTA = "DECOLECTA";
    private static final String URL_APISPERU = "https://dniruc.apisperu.com/api/v1";
    private static final String URL_DECOLECTA = "https://api.decolecta.com/v1";

    public record DatosDocumentoExterno(
            String tipoDocumento,
            String numeroDocumento,
            String nombreCompleto,
            String direccion,
            String estado,
            String condicion,
            String origenDatos
    ) {}

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final String proveedor;
    private final String baseUrl;
    private final String token;

    @Autowired
    public ServicioConsultaDocumentoExterno(
            RestClient.Builder builder,
            ObjectMapper mapper,
            @Value("${app.consulta-doc.proveedor:APISPERU}") String proveedor,
            @Value("${app.consulta-doc.base-url:}") String baseUrl,
            @Value("${app.consulta-doc.token:}") String token,
            @Value("${app.consulta-doc.timeout-ms:5000}") int timeoutMs) {
        this(construirConTimeout(builder, timeoutMs), mapper, proveedor, baseUrl, token);
    }

    /** Constructor de pruebas compatible con MockRestServiceServer sin reemplazar su RequestFactory. */
    public ServicioConsultaDocumentoExterno(
            RestClient.Builder builder,
            ObjectMapper mapper,
            String proveedor,
            String baseUrl,
            String token) {
        this(builder.build(), mapper, proveedor, baseUrl, token);
    }

    private ServicioConsultaDocumentoExterno(
            RestClient restClient,
            ObjectMapper mapper,
            String proveedor,
            String baseUrl,
            String token) {
        this.restClient = restClient;
        this.mapper = mapper;
        this.proveedor = normalizarProveedor(proveedor);
        this.baseUrl = resolverBaseUrl(this.proveedor, baseUrl);
        this.token = token == null ? "" : token.trim();
    }

    private static RestClient construirConTimeout(RestClient.Builder builder, int timeoutMs) {
        int timeout = Math.max(1000, timeoutMs);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return builder.requestFactory(factory).build();
    }

    public boolean estaConfigurado() {
        return !token.isBlank();
    }

    public String getProveedor() {
        return proveedor;
    }

    public Optional<DatosDocumentoExterno> consultar(String tipoDocumento, String numeroDocumento) {
        if (!estaConfigurado() || tipoDocumento == null || numeroDocumento == null) {
            return Optional.empty();
        }
        String tipo = tipoDocumento.trim().toUpperCase(Locale.ROOT);
        String numero = numeroDocumento.trim();
        if (("DNI".equals(tipo) && !numero.matches("[0-9]{8}"))
                || ("RUC".equals(tipo) && !numero.matches("[0-9]{11}"))
                || (!"DNI".equals(tipo) && !"RUC".equals(tipo))) {
            return Optional.empty();
        }

        URI uri = construirUri(tipo, numero);
        try {
            RestClient.RequestHeadersSpec<?> req = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON);
            if (PROVEEDOR_DECOLECTA.equals(proveedor)) {
                req = req.header("Authorization", "Bearer " + token);
            }
            String body = req.retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            return parsearRespuesta(tipo, numero, body);
        } catch (RestClientResponseException ex) {
            // Nunca registrar la URI completa porque en APISPERU contiene ?token=
            LOG.warn("Consulta externa {} {} en {} devolvió HTTP {}", tipo, numero, proveedor, ex.getStatusCode().value());
            return Optional.empty();
        } catch (RestClientException ex) {
            LOG.warn("Error de conexión consultando {} {} en {}: {}", tipo, numero, proveedor, ex.getClass().getSimpleName());
            return Optional.empty();
        } catch (Exception ex) {
            LOG.warn("Respuesta inválida consultando {} {} en {}: {}", tipo, numero, proveedor, ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private URI construirUri(String tipo, String numero) {
        boolean esDni = "DNI".equals(tipo);
        if (PROVEEDOR_DECOLECTA.equals(proveedor)) {
            String path = esDni ? "/reniec/dni" : "/sunat/ruc";
            return UriComponentsBuilder.fromUriString(baseUrl + path)
                    .queryParam("numero", numero)
                    .build()
                    .toUri();
        }
        String path = (esDni ? "/dni/" : "/ruc/") + numero;
        return UriComponentsBuilder.fromUriString(baseUrl + path)
                .queryParam("token", token)
                .build()
                .toUri();
    }

    private Optional<DatosDocumentoExterno> parsearRespuesta(String tipo, String numero, String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        // APISPERU puede responder HTTP 200 con {"success":false,"message":"..."} cuando no halla el documento
        if (root.has("success") && !root.path("success").asBoolean(true)) {
            return Optional.empty();
        }
        if ("DNI".equals(tipo)) {
            String completo = primerTexto(root, "full_name", "nombreCompleto", "nombre_completo");
            if (completo.isBlank()) {
                String nombres = primerTexto(root, "nombres", "preNombres", "first_name");
                String pat = primerTexto(root, "apellidoPaterno", "apellido_paterno", "first_last_name");
                String mat = primerTexto(root, "apellidoMaterno", "apellido_materno", "second_last_name");
                completo = unirNoVacios(" ", nombres, pat, mat);
            }
            if (completo.isBlank()) {
                completo = primerTexto(root, "nombre");
            }
            if (completo.isBlank()) {
                return Optional.empty();
            }
            String direccion = limpiarGuion(primerTexto(root, "direccion", "domicilio_fiscal"));
            return Optional.of(new DatosDocumentoExterno(tipo, numero, completo, direccion, "", "", proveedor));
        }

        String razon = primerTexto(root, "razonSocial", "razon_social", "nombre_o_razon_social", "nombre");
        if (razon.isBlank()) {
            return Optional.empty();
        }
        String dirBase = limpiarGuion(primerTexto(root, "direccion", "domicilio_fiscal", "domicilioFiscal"));
        String distrito = limpiarGuion(primerTexto(root, "distrito"));
        String provincia = limpiarGuion(primerTexto(root, "provincia"));
        String departamento = limpiarGuion(primerTexto(root, "departamento"));
        String direccion = formatearDireccionFiscal(dirBase, distrito, provincia, departamento);

        String estado = primerTexto(root, "estado");
        String condicion = primerTexto(root, "condicion");
        return Optional.of(new DatosDocumentoExterno(tipo, numero, razon, direccion, estado, condicion, proveedor));
    }

    private static String formatearDireccionFiscal(String base, String distrito, String provincia, String departamento) {
        if (base.isBlank()) {
            return unirNoVacios(" - ", distrito, provincia, departamento);
        }
        String upper = base.toUpperCase(Locale.ROOT);
        List<String> extras = new ArrayList<>();
        if (!distrito.isBlank() && !upper.contains(distrito.toUpperCase(Locale.ROOT))) {
            extras.add(distrito);
        }
        if (!provincia.isBlank() && !upper.contains(provincia.toUpperCase(Locale.ROOT))) {
            extras.add(provincia);
        }
        if (extras.isEmpty()) {
            return base;
        }
        return base + " - " + String.join(", ", extras);
    }

    private static String normalizarProveedor(String raw) {
        if (raw == null || raw.isBlank()) {
            return PROVEEDOR_APISPERU;
        }
        String p = raw.trim().toUpperCase(Locale.ROOT);
        if (p.contains("DECOLECTA") || p.contains("APIS.NET") || p.contains("APISNET")) {
            return PROVEEDOR_DECOLECTA;
        }
        return PROVEEDOR_APISPERU;
    }

    private static String resolverBaseUrl(String proveedorNormalizado, String rawUrl) {
        String url = (rawUrl == null || rawUrl.isBlank())
                ? (PROVEEDOR_DECOLECTA.equals(proveedorNormalizado) ? URL_DECOLECTA : URL_APISPERU)
                : rawUrl.trim();
        int fin = url.length();
        while (fin > 0 && url.charAt(fin - 1) == '/') fin--;
        return url.substring(0, fin);
    }

    private static String primerTexto(JsonNode root, String... keys) {
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node != null && !node.isNull() && !node.isMissingNode()) {
                String txt = node.asText("").trim();
                if (!txt.isBlank() && !"null".equalsIgnoreCase(txt)) {
                    return txt;
                }
            }
        }
        return "";
    }

    private static String limpiarGuion(String valor) {
        return "-".equals(valor) ? "" : valor;
    }

    private static String unirNoVacios(String sep, String... partes) {
        List<String> validas = new ArrayList<>();
        for (String p : partes) {
            if (p != null && !p.isBlank()) {
                validas.add(p.trim());
            }
        }
        return String.join(sep, validas);
    }
}
