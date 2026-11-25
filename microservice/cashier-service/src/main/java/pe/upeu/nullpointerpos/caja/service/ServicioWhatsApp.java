package pe.upeu.nullpointerpos.caja.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [HU-19 START] Adaptador de envío de comprobantes PDF vía Green-API (WhatsApp QR hospedado).
 * Sube el PDF generado en memoria mediante multipart/form-data (`sendFileByUpload`) normalizando
 * teléfonos peruanos con código de país `51` para formato `chatId` (`519XXXXXXXX@c.us`).
 */
@Service
public class ServicioWhatsApp {
    private static final String GREENAPI_DEFAULT_MEDIA_URL = "https://media.green-api.com";
    private final RestClient restClient;
    private final String greenMediaUrl;
    private final String greenIdInstance;
    private final String greenApiToken;
    private final String legacyEndpoint;
    private final String legacyToken;

    @Autowired
    public ServicioWhatsApp(
            RestClient.Builder builder,
            @Value("${app.greenapi.media-url:https://media.green-api.com}") String greenMediaUrl,
            @Value("${app.greenapi.id-instance:}") String greenIdInstance,
            @Value("${app.greenapi.api-token:}") String greenApiToken,
            @Value("${app.whatsapp.endpoint:}") String legacyEndpoint,
            @Value("${app.whatsapp.token:}") String legacyToken) {
        this.restClient = builder.build();
        this.greenMediaUrl = limpiar(greenMediaUrl, GREENAPI_DEFAULT_MEDIA_URL);
        this.greenIdInstance = limpiar(greenIdInstance, "");
        this.greenApiToken = limpiar(greenApiToken, "");
        this.legacyEndpoint = limpiar(legacyEndpoint, "");
        this.legacyToken = limpiar(legacyToken, "");
    }

    public ServicioWhatsApp(RestClient.Builder builder, String legacyEndpoint, String legacyToken) {
        this(builder, GREENAPI_DEFAULT_MEDIA_URL, "", "", legacyEndpoint, legacyToken);
    }

    public boolean estaConfigurado() {
        return !resolverUrlSubidaPdf().isBlank();
    }

    /**
     * Sube y envía un PDF real por WhatsApp vía Green-API `sendFileByUpload`.
     */
    public String enviarPdf(String telefono, byte[] pdfBytes, String nombreArchivo, String leyenda) {
        String urlUpload = resolverUrlSubidaPdf();
        if (urlUpload.isBlank()) {
            throw new IllegalStateException("Configure GREENAPI_ID_INSTANCE y GREENAPI_API_TOKEN para enviar el PDF por WhatsApp.");
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("El archivo PDF del comprobante está vacío.");
        }
        String chatId = formatearChatId(telefono);
        String archivoFinal = sanitizarNombrePdf(nombreArchivo);

        MultipartBodyBuilder multipart = new MultipartBodyBuilder();
        multipart.part("chatId", chatId);
        multipart.part("fileName", archivoFinal);
        if (leyenda != null && !leyenda.isBlank()) {
            multipart.part("caption", leyenda.trim());
        }
        multipart.part("file", new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return archivoFinal;
            }
        }, MediaType.APPLICATION_PDF);

        String respuesta = restClient.post()
                .uri(urlUpload)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipart.build())
                .retrieve()
                .body(String.class);
        return respuesta == null ? "{}" : respuesta;
    }

    /**
     * Envía mensaje de texto simple por Green-API `sendMessage`.
     */
    public String enviarTexto(String telefono, String contenido) {
        String urlTexto = resolverUrlTexto();
        if (urlTexto.isBlank()) {
            throw new IllegalStateException("Configure GREENAPI_ID_INSTANCE y GREENAPI_API_TOKEN para enviar mensajes por WhatsApp.");
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("chatId", formatearChatId(telefono));
        cuerpo.put("message", contenido == null ? "" : contenido);

        String respuesta = restClient.post()
                .uri(urlTexto)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .body(String.class);
        return respuesta == null ? "{}" : respuesta;
    }

    public String formatearChatId(String telefono) {
        String digitos = telefono == null ? "" : telefono.replaceAll("[^0-9]", "");
        if (digitos.startsWith("00")) {
            digitos = digitos.substring(2);
        }
        // En Perú, celulares locales tienen 9 dígitos empezando en 9 -> anteponer código país 51.
        if (digitos.length() == 9 && digitos.startsWith("9")) {
            digitos = "51" + digitos;
        }
        if (digitos.length() < 9) {
            throw new IllegalArgumentException("Número de WhatsApp inválido: " + telefono);
        }
        return digitos + "@c.us";
    }

    String resolverUrlSubidaPdf() {
        if (!greenIdInstance.isBlank() && !greenApiToken.isBlank()) {
            String host = normalizarHostMedia(greenMediaUrl);
            return host + "/waInstance" + greenIdInstance + "/sendFileByUpload/" + greenApiToken;
        }
        if (!legacyEndpoint.isBlank()) {
            if (legacyEndpoint.contains("/sendFileByUpload/")) {
                return legacyEndpoint;
            }
            if (legacyEndpoint.contains("/waInstance") && !legacyToken.isBlank()) {
                String host = normalizarHostMedia(quitarDesdeWaInstance(legacyEndpoint));
                String instanceSeg = extraerSegmentoInstancia(legacyEndpoint);
                if (!instanceSeg.isBlank()) {
                    return host + "/waInstance" + instanceSeg + "/sendFileByUpload/" + legacyToken;
                }
            }
        }
        return "";
    }

    String resolverUrlTexto() {
        if (!greenIdInstance.isBlank() && !greenApiToken.isBlank()) {
            String host = normalizarHostApi(greenMediaUrl);
            return host + "/waInstance" + greenIdInstance + "/sendMessage/" + greenApiToken;
        }
        if (!legacyEndpoint.isBlank() && legacyEndpoint.contains("/waInstance")) {
            return resolverUrlSubidaPdf().replace("/sendFileByUpload/", "/sendMessage/").replace(".media.green", ".api.green");
        }
        return "";
    }

    /** Elimina las barras finales sin usar regex (evita backtracking superlineal). */
    private static String quitarBarrasFinales(String texto) {
        int fin = texto.length();
        while (fin > 0 && texto.charAt(fin - 1) == '/') fin--;
        return texto.substring(0, fin);
    }

    private String normalizarHostMedia(String raw) {
        String url = quitarBarrasFinales(quitarDesdeWaInstance(limpiar(raw, GREENAPI_DEFAULT_MEDIA_URL)));
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        return url;
    }

    private String normalizarHostApi(String raw) {
        String url = quitarBarrasFinales(quitarDesdeWaInstance(limpiar(raw, "https://api.green-api.com")));
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        return url;
    }

    private String quitarDesdeWaInstance(String url) {
        int idx = url.indexOf("/waInstance");
        return idx > 0 ? url.substring(0, idx) : url;
    }

    private String extraerSegmentoInstancia(String url) {
        int idx = url.indexOf("/waInstance");
        if (idx < 0) return "";
        String tail = url.substring(idx + "/waInstance".length());
        int slash = tail.indexOf('/');
        return slash >= 0 ? tail.substring(0, slash) : tail;
    }

    private String sanitizarNombrePdf(String nombreArchivo) {
        String base = (nombreArchivo == null || nombreArchivo.isBlank()) ? "comprobante.pdf" : nombreArchivo.trim();
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return base.toLowerCase().endsWith(".pdf") ? base : base + ".pdf";
    }

    private String limpiar(String valor, String porDefecto) {
        return (valor == null || valor.isBlank()) ? porDefecto : valor.trim();
    }
}
/** [HU-19 END] */
