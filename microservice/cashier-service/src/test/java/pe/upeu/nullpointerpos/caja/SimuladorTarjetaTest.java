package pe.upeu.nullpointerpos.caja;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.caja.dto.request.SolicitudSimularTarjeta;
import pe.upeu.nullpointerpos.caja.dto.response.RespuestaSimuladorTarjeta;
import pe.upeu.nullpointerpos.caja.service.impl.ServicioCajaImpl;

import static org.junit.jupiter.api.Assertions.*;

class SimuladorTarjetaTest {

    private ServicioCajaImpl servicio;

    @BeforeEach
    void setUp() {
        // Para pruebas del simulador de tarjeta no se requieren dependencias de BD ni pedidos
        servicio = new ServicioCajaImpl(null, null, null);
    }

    @Test
    void tarjetaVisaValidaConLuhnDebeAprobarse() {
        // Tarjeta de prueba Visa clásica que cumple algoritmo de Luhn
        SolicitudSimularTarjeta sol = new SolicitudSimularTarjeta(
                "4111 1111 1111 1111", 12, 2028, "123", "JUAN PEREZ"
        );
        RespuestaSimuladorTarjeta res = servicio.simularTarjeta(sol);

        assertTrue(res.aprobado());
        assertEquals("VISA", res.marca());
        assertEquals("1111", res.ultimosCuatroDigitos());
        assertNotNull(res.codigoAutorizacion());
        assertTrue(res.codigoAutorizacion().startsWith("AUTH-"));
        assertTrue(res.numeroOperacion().startsWith("VOUCH-"));
    }

    @Test
    void tarjetaMastercardValidaDebeIdentificarMarca() {
        // Tarjeta Mastercard válida con Luhn
        SolicitudSimularTarjeta sol = new SolicitudSimularTarjeta(
                "5425 2334 3010 9903", 6, 2029, "456", "MARIA LOPEZ"
        );
        RespuestaSimuladorTarjeta res = servicio.simularTarjeta(sol);

        assertTrue(res.aprobado());
        assertEquals("MASTERCARD", res.marca());
        assertEquals("9903", res.ultimosCuatroDigitos());
    }

    @Test
    void tarjetaInvalidaPorLuhnDebeLanzarBadRequest() {
        // Tarjeta con número corrupto (no cumple suma Luhn)
        SolicitudSimularTarjeta sol = new SolicitudSimularTarjeta(
                "4111 1111 1111 1112", 10, 2027, "999", "CLIENTE PRUEBA"
        );
        assertThrows(ResponseStatusException.class, () -> servicio.simularTarjeta(sol));
    }

    @Test
    void tarjetaVencidaDebeRechazarse() {
        // Año anterior al actual
        SolicitudSimularTarjeta sol = new SolicitudSimularTarjeta(
                "4111 1111 1111 1111", 1, 2020, "123", "CLIENTE VENCIDO"
        );
        assertThrows(ResponseStatusException.class, () -> servicio.simularTarjeta(sol));
    }
}
