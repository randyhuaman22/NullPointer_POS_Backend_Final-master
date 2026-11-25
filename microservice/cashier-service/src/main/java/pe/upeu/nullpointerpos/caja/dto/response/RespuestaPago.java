package pe.upeu.nullpointerpos.caja.dto.response;
import java.math.BigDecimal;import java.time.OffsetDateTime;
public record RespuestaPago(Long idPago,Long idPedido,Long idTurno,String metodo,BigDecimal importe,BigDecimal importeRecibido,BigDecimal vuelto,String estado,OffsetDateTime fechaCreacion){}
