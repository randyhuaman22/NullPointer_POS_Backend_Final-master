package pe.upeu.nullpointerpos.pedidos.service;
import pe.upeu.nullpointerpos.pedidos.dto.request.*;
import pe.upeu.nullpointerpos.pedidos.dto.response.*;
import java.util.List;
public interface ServicioPedidos {
    RespuestaPedido crear(SolicitudCrearPedido solicitud, String codigoMozo, Long idUsuarioMozo);
    RespuestaPedido sincronizarOffline(SolicitudSincronizarPedidoOffline solicitud, String codigoMozo, Long idUsuarioMozo, boolean administrador);
    RespuestaPedido buscarPorIdempotencia(String idempotencia, String codigoTrabajador, boolean administrador);
    RespuestaPedido actualizarDetalles(Long idPedido, SolicitudModificarDetallesPedido solicitud, String codigoTrabajador, Long idUsuarioActor, boolean administrador);
    RespuestaTrasladoMesa trasladar(SolicitudTrasladoMesa solicitud, String codigoTrabajador, Long idUsuarioActor, boolean administrador);
    RespuestaPedido enviarACocina(Long idPedido, String codigoTrabajador, Long idUsuarioActor, boolean administrador);
    RespuestaPedido marcarPedidoListoCocina(Long idPedido, String actor, Long idUsuarioActor);
    RespuestaPedido buscarPorId(Long idPedido, String codigoTrabajador, boolean administrador);
    List<RespuestaPedido> listarPedidosActivosSalon();
    List<RespuestaPedido> listarPendientesCobro();
    RespuestaPedido buscarPendienteCobroPorId(Long idPedido);
    List<RespuestaPedido> listarPedidosCocina();
    List<pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaPedidoCocina> listarPedidosCocinaConDemora();
    RespuestaPedido marcarDetalleListo(Long idDetalle, String actor, Long idUsuarioActor);
    RespuestaPedido aplicarDescuento(Long idPedido, pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudDescuentoPedido solicitud, String actor, Long idUsuarioActor, boolean autorizado);
    RespuestaPedido anular(Long idPedido, pe.upeu.nullpointerpos.pedidos.dto.request.SolicitudAnularPedido solicitud, String actor, Long idUsuarioActor, boolean autorizado);
    pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaResumenPago resumenPago(Long idPedido);
    void registrarPagoConfirmado(pe.upeu.nullpointerpos.pedidos.dto.event.EventoPagoConfirmado evento);
}
