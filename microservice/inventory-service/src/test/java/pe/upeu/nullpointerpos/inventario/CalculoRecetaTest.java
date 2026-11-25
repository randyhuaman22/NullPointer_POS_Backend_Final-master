package pe.upeu.nullpointerpos.inventario;
import org.junit.jupiter.api.Test;import java.math.BigDecimal;import static org.junit.jupiter.api.Assertions.*;
class CalculoRecetaTest {@Test void consumoSeEscalaPorCantidadDelPlato(){BigDecimal porPlato=new BigDecimal("0.125");BigDecimal cantidad=new BigDecimal("4");assertEquals(new BigDecimal("0.500"),porPlato.multiply(cantidad));}}
