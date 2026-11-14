package pe.upeu.nullpointerpos.pedidos;

import org.junit.jupiter.api.Test;
import pe.upeu.nullpointerpos.pedidos.service.impl.CalculadoraDemoraPedido;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;

/** [HU-09 / T-14 START] Umbrales exactos de 9:59, 10:00, 14:59 y 15:00. */
class CalculadoraDemoraPedidoTest {
    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-11-11T10:00:00-05:00");
    private CalculadoraDemoraPedido.Resultado transcurridos(int segundos, boolean listo) {
        return CalculadoraDemoraPedido.calcular(INICIO, INICIO.plusSeconds(segundos), listo);
    }

    @Test void nueveMinutosCincuentaYNueveSegundosEsNormal() {
        var r=transcurridos(599,false); assertEquals("NORMAL",r.nivelDemora()); assertEquals(10,r.siguienteUmbralMinutos());
    }
    @Test void diezMinutosEsPreventiva() {
        var r=transcurridos(600,false); assertEquals("PREVENTIVA",r.nivelDemora()); assertEquals(15,r.siguienteUmbralMinutos());
    }
    @Test void catorceMinutosCincuentaYNueveSegundosContinuaPreventiva() {
        var r=transcurridos(899,false); assertEquals("PREVENTIVA",r.nivelDemora());
    }
    @Test void quinceMinutosEsCritica() {
        var r=transcurridos(900,false); assertEquals("CRITICA",r.nivelDemora()); assertNull(r.siguienteUmbralMinutos());
    }
    @Test void pedidoListoNoTieneUmbralPendiente() {
        var r=transcurridos(1800,true); assertEquals("LISTO",r.nivelDemora()); assertNull(r.siguienteUmbralMinutos());
    }
    @Test void fechasInvertidasNoGeneranDemoraNegativa() {
        var r=CalculadoraDemoraPedido.calcular(INICIO,INICIO.minusMinutes(1),false);
        assertEquals(0,r.segundosTranscurridos()); assertEquals("NORMAL",r.nivelDemora());
    }
}
/** [HU-09 / T-14 END] */
