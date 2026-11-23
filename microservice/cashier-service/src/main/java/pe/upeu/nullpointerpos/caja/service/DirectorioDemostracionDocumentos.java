package pe.upeu.nullpointerpos.caja.service;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;

/**
 * [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-61 START]
 * Directorio de prueba local. Los registros son ficticios y NO representan datos reales de RENIEC o SUNAT.
 */
@Component
public class DirectorioDemostracionDocumentos {
    public record Datos(String tipoDocumento, String numeroDocumento, String nombreCompleto, String direccion) {}
    private final Map<String, Datos> registros = Map.of(
        "DNI:12345678", new Datos("DNI", "12345678", "CLIENTE DE PRUEBA", ""),
        "RUC:20123456789", new Datos("RUC", "20123456789", "EMPRESA DEMOSTRATIVA S.A.C.", "DIRECCIÓN DE PRUEBA")
    );
    public Optional<Datos> buscar(String tipo, String numero) {
        if (tipo == null || numero == null) return Optional.empty();
        String clave = tipo.trim().toUpperCase(java.util.Locale.ROOT) + ":" + numero.trim();
        return Optional.ofNullable(registros.get(clave));
    }
}
/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-61 END] */
