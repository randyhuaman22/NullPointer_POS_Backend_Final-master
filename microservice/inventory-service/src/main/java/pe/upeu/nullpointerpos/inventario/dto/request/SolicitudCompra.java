package pe.upeu.nullpointerpos.inventario.dto.request;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.util.List;
public record SolicitudCompra(@Size(max=160) String proveedor,@Size(max=80) String documento,@Size(max=300) String observacion,@NotEmpty List<@Valid ItemCompra> items){}
