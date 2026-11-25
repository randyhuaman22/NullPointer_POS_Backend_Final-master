/** [SPRINT 3 / HU-17 / S3-T-01 START] Request and field validation for an authorized discount. */
package pe.upeu.nullpointerpos.pedidos.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** HU-17: el descuento se calcula en el servidor y siempre requiere motivo auditable. */
public record SolicitudDescuentoPedido(
        @NotNull @DecimalMin(value="0.01") @DecimalMax(value="99.99") BigDecimal porcentaje,
        @NotBlank @Size(max=250) String motivo) { }
/** [SPRINT 3 / HU-17 / S3-T-01 END] */
