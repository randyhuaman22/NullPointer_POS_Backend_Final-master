package pe.upeu.nullpointerpos.caja;
import org.junit.jupiter.api.Test;import java.math.BigDecimal;import java.math.RoundingMode;import static org.junit.jupiter.api.Assertions.*;
class CajaImportesTest {
 @Test void vueltoRedondeadoDosDecimales(){assertEquals(new BigDecimal("2.50"),new BigDecimal("20.00").subtract(new BigDecimal("17.50")).setScale(2,RoundingMode.HALF_UP));}
 @Test void pagoMixtoDebeSumarImporteExacto(){BigDecimal total=new BigDecimal("49.90");BigDecimal partes=new BigDecimal("25.00").add(new BigDecimal("24.90"));assertEquals(0,total.compareTo(partes));assertNotEquals(0,total.compareTo(partes.add(new BigDecimal("0.01"))));}
}
