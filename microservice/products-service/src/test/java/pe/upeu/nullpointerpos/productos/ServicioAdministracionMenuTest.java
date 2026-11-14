package pe.upeu.nullpointerpos.productos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudCategoria;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudProducto;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudConfiguracionOpcionesProducto;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudGrupoOpcionesProducto;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudOpcionProducto;
import pe.upeu.nullpointerpos.productos.repository.RepositorioAdministracionMenu;
import pe.upeu.nullpointerpos.productos.service.ServicioAdministracionMenu;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioAdministracionMenuTest {
    @Mock
    private RepositorioAdministracionMenu repositorio;

    @InjectMocks
    private ServicioAdministracionMenu servicio;

    @Test
    void rechazaUnaCategoriaConAreaDesconocida() {
        when(repositorio.existeAreaActiva(anyString())).thenReturn(false);
        SolicitudCategoria solicitud = new SolicitudCategoria(
                "Postres", "Postres de prueba", "OTRA_AREA", null, 1);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.crearCategoria(solicitud, "ADM001"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repositorio, never()).crearCategoria(solicitud, "ADM001");
    }

    @Test
    void impideDesactivarCategoriaQueTodaviaTieneProductos() {
        when(repositorio.buscarCategoriaAdministracion(10L)).thenReturn(Optional.of(
                new pe.upeu.nullpointerpos.productos.dto.response.RespuestaCategoriaAdministracion(
                        10L, "Pollos", "Productos", 1, "POLLERIA", null, true)));
        when(repositorio.tieneProductosActivos(10L)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.desactivarCategoria(10L, "ADM001"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repositorio, never()).desactivarCategoria(10L, "ADM001");
    }


    @Test
    void rechazaProductoSinEstacionDePreparacion() {
        when(repositorio.existeCategoriaActiva(1L)).thenReturn(true);
        when(repositorio.existeEstacionActiva(null)).thenReturn(false);
        SolicitudProducto solicitud = new SolicitudProducto(
                "PRUEBA-02", "Producto sin estación", "Descripción", null,
                new BigDecimal("10.00"), 1L, null, 5, true, false, 1);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.crearProducto(solicitud, "ADM001"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repositorio, never()).crearProducto(solicitud, "ADM001");
    }

    @Test
    void rechazaGruposDeOpcionesDuplicadosAntesDeGuardar() {
        SolicitudGrupoOpcionesProducto grupo1 = new SolicitudGrupoOpcionesProducto(
                "Tamaño", true, 1, 1, 1,
                java.util.List.of(new SolicitudOpcionProducto("Mediano", BigDecimal.ZERO, 1)));
        SolicitudGrupoOpcionesProducto grupo2 = new SolicitudGrupoOpcionesProducto(
                "TAMAÑO", true, 1, 1, 2,
                java.util.List.of(new SolicitudOpcionProducto("Grande", new BigDecimal("3.00"), 1)));
        SolicitudConfiguracionOpcionesProducto solicitud = new SolicitudConfiguracionOpcionesProducto(
                java.util.List.of(grupo1, grupo2));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.guardarOpcionesProducto(1L, solicitud, "ADM001"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repositorio, never()).guardarOpcionesProducto(1L, solicitud, "ADM001");
    }

    @Test
    void rechazaMoverCategoriaConSubcategoriasActivasAOtraArea() {
        when(repositorio.existeAreaActiva("JUGUERIA")).thenReturn(true);
        when(repositorio.buscarCategoriaAdministracion(1L)).thenReturn(Optional.of(
                new pe.upeu.nullpointerpos.productos.dto.response.RespuestaCategoriaAdministracion(
                        1L, "Bebidas", "Bebidas", 1, "POLLERIA", null, true)));
        when(repositorio.tieneSubcategoriasActivas(1L)).thenReturn(true);
        SolicitudCategoria solicitud = new SolicitudCategoria(
                "Bebidas", "Bebidas", "JUGUERIA", null, 1);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.actualizarCategoria(1L, solicitud, "ADM001"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repositorio, never()).actualizarCategoria(1L, solicitud, "ADM001");
    }

    @Test
    void rechazaCrearProductoConCategoriaInactivaOInexistente() {
        when(repositorio.existeCategoriaActiva(99L)).thenReturn(false);
        SolicitudProducto solicitud = new SolicitudProducto(
                "PRUEBA-01", "Producto de prueba", "Descripción", null,
                new BigDecimal("10.00"), 99L, null, 5, true, false, 1);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio.crearProducto(solicitud, "ADM001"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(repositorio, never()).crearProducto(solicitud, "ADM001");
    }
}
