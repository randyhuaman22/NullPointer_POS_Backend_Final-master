package pe.upeu.nullpointerpos.caja.dto.response;
import java.math.BigDecimal;import java.time.OffsetDateTime;
public record RespuestaTurnoCaja(Long idTurno,String codigoCajero,BigDecimal montoInicial,String estado,OffsetDateTime fechaApertura,OffsetDateTime fechaCierre,BigDecimal montoEsperado,BigDecimal montoReal,BigDecimal diferencia){}
