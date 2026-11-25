package pe.upeu.nullpointerpos.pedidos.client;

import pe.upeu.nullpointerpos.pedidos.dto.external.RespuestaProductoCatalogo;
import pe.upeu.nullpointerpos.pedidos.dto.external.RespuestaOpcionesValidadas;
import pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudSeleccionOpcionProducto;
import org.springframework.http.MediaType;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/** Cliente HTTP interno: el pedido consulta al catálogo y conserva el precio/nombre como snapshot. */
@Component
public class ClienteProductos {
    private final RestClient restClient;
    private final String urlBase;

    public ClienteProductos(RestClient.Builder builder,
                            @Value("${app.servicios.productos-url:http://localhost:8084}") String urlBase) {
        this.restClient = builder.build();
        this.urlBase = urlBase.replaceAll("/$", "");
    }

    public RespuestaProductoCatalogo buscarDisponible(Long idProducto) {
        try {
            return restClient.get()
                    .uri(urlBase + "/api/v1/menu/productos/{idProducto}", idProducto)
                    .headers(headers -> copiarToken(headers))
                    .retrieve()
                    .body(RespuestaProductoCatalogo.class);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El producto " + idProducto + " no existe o no está disponible en el catálogo.", ex);
        }
    }

    public RespuestaOpcionesValidadas validarOpciones(Long idProducto,
                                                       List<SolicitudSeleccionOpcionProducto> opciones) {
        try {
            RespuestaOpcionesValidadas respuesta = restClient.post()
                    .uri(urlBase + "/api/v1/menu/productos/{idProducto}/opciones/validar", idProducto)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(this::copiarToken)
                    .body(opciones == null ? List.of() : opciones)
                    .retrieve()
                    .body(RespuestaOpcionesValidadas.class);
            if (respuesta == null || respuesta.precioAdicional() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "El catálogo no devolvió el precio de las opciones del producto.");
            }
            return respuesta;
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Las opciones seleccionadas del producto " + idProducto + " no son válidas: " + ex.getStatusText(), ex);
        }
    }

    private void copiarToken(HttpHeaders headers) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith("Bearer ")) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
    }
}
