package pe.upeu.nullpointerpos.caja.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;

/** Consulta el contrato público de servicio-pedidos y reenvía el JWT; no accede a su base de datos. */
@Component
public class ClientePedidos {
    private final RestClient rest; private final String base;
    public ClientePedidos(RestClient.Builder b,@Value("${app.servicios.pedidos-url:http://localhost:8082}")String base){this.rest=b.build();this.base=base.replaceAll("/$","");}
    public ResumenPedido buscarResumen(Long id) {
        try { return rest.get().uri(base+"/api/v1/pedidos/{id}/resumen-pago",id).headers(h->{
            if(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a){String auth=a.getRequest().getHeader(HttpHeaders.AUTHORIZATION);if(auth!=null)h.set(HttpHeaders.AUTHORIZATION,auth);}
        }).retrieve().body(ResumenPedido.class); }
        catch(RestClientResponseException ex){
            int c=ex.getStatusCode().value();
            if(c==404) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"El pedido "+id+" no existe.",ex);
            if(c==401||c==403) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"servicio-pedidos rechazó el token del cajero ("+c+").",ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No se pudo validar el pedido en servicio-pedidos: "+ex.getStatusCode(),ex);
        }
        catch(org.springframework.web.client.ResourceAccessException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"servicio-pedidos no responde.",ex);}
    }
    public record ResumenPedido(Long idPedido,String codigoPedido,String estado,BigDecimal importeTotal,BigDecimal totalDescuento){}
}
