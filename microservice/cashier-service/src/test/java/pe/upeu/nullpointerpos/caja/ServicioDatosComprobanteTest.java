package pe.upeu.nullpointerpos.caja;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.upeu.nullpointerpos.caja.dto.request.SolicitudDatosComprobante;
import pe.upeu.nullpointerpos.caja.repository.RepositorioCaja;
import pe.upeu.nullpointerpos.caja.service.DirectorioDemostracionDocumentos;
import pe.upeu.nullpointerpos.caja.service.ServicioDatosComprobante;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-63 START] Pruebas locales de las cuatro modalidades de comprador. */
class ServicioDatosComprobanteTest {
    private RepositorioCaja repositorio;
    private ServicioDatosComprobante servicio;
    @BeforeEach void preparar() {
        repositorio = mock(RepositorioCaja.class);
        servicio = new ServicioDatosComprobante(repositorio, new DirectorioDemostracionDocumentos());
    }
    @Test void permiteBoletaSinDatos() {
        var r = servicio.preparar(new SolicitudDatosComprobante("BOLETA","SIN_DATOS",null,null,null,null,null,null));
        assertEquals("SIN_DATOS", r.modalidad()); assertFalse(r.requiereDatosCliente());
    }
    @Test void permiteDatosManualesSinDocumentoParaBoleta() {
        var r = servicio.preparar(new SolicitudDatosComprobante("BOLETA","MANUAL",null,null,"Cliente Manual",null,"987654321",""));
        assertEquals("Cliente Manual", r.nombreCompleto()); assertEquals("INGRESO_MANUAL", r.origenDatos());
        verify(repositorio, never()).guardarCliente(anyString(), anyString(), anyString(), any(), any());
    }
    @Test void consultaDniEnDirectorioLocalFicticio() {
        when(repositorio.buscarCliente("DNI", "12345678")).thenReturn(java.util.Collections.emptyMap());
        var r = servicio.preparar(new SolicitudDatosComprobante("BOLETA","DNI","DNI","12345678",null,null,null,null));
        assertEquals("CLIENTE DE PRUEBA", r.nombreCompleto()); assertEquals("DEMO_LOCAL", r.origenDatos());
    }
    @Test void consultaRucEnDirectorioLocalFicticio() {
        when(repositorio.buscarCliente("RUC", "20123456789")).thenReturn(java.util.Collections.emptyMap());
        var r = servicio.preparar(new SolicitudDatosComprobante("FACTURA","RUC","RUC","20123456789",null,null,null,null));
        assertEquals("EMPRESA DEMOSTRATIVA S.A.C.", r.nombreCompleto()); assertEquals("DEMO_LOCAL", r.origenDatos());
    }
    @Test void priorizaRegistroLocalCuandoTieneNombre() {
        when(repositorio.buscarCliente("DNI", "12345678")).thenReturn(java.util.Map.of(
                "nombreCompleto", "CLIENTE REGISTRADO", "telefono", "999111222", "correo", "cliente@ejemplo.com"));
        var r = servicio.preparar(new SolicitudDatosComprobante("BOLETA","DNI","DNI","12345678",null,null,null,null));
        assertEquals("CLIENTE REGISTRADO", r.nombreCompleto());
        assertEquals("REGISTRO_LOCAL", r.origenDatos());
    }
    @Test void documentoNoRegistradoNiDePruebaDevuelve404() {
        when(repositorio.buscarCliente("DNI", "87654321")).thenReturn(java.util.Collections.emptyMap());
        var ex = assertThrows(ResponseStatusException.class, () -> servicio.preparar(
                new SolicitudDatosComprobante("BOLETA","DNI","DNI","87654321",null,null,null,null)));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
    @Test void rechazaFacturaSinDatos() {
        assertThrows(ResponseStatusException.class, () -> servicio.preparar(new SolicitudDatosComprobante("FACTURA","SIN_DATOS",null,null,null,null,null,null)));
    }
    @Test void rechazaDniConFactura() {
        assertThrows(ResponseStatusException.class, () -> servicio.preparar(new SolicitudDatosComprobante("FACTURA","DNI","DNI","12345678",null,null,null,null)));
    }
}
/** [SPRINT 3 / HU-COMPROBANTE-CLIENTE / S3-T-63 END] */
