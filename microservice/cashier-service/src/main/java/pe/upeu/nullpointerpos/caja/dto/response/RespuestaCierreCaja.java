package pe.upeu.nullpointerpos.caja.dto.response;
import java.math.BigDecimal;
public record RespuestaCierreCaja(Long idTurno,BigDecimal montoInicial,BigDecimal totalEfectivo,BigDecimal montoEsperado,BigDecimal montoReal,BigDecimal diferencia,String estado){}
