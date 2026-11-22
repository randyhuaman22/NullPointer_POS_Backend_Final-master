package pe.upeu.nullpointerpos.caja.dto.response;

/** Resultado de la simulación o validación de tarjeta en el POS del cajero. */
public record RespuestaSimuladorTarjeta(
    boolean aprobado,
    String marca,
    String codigoAutorizacion,
    String numeroOperacion,
    String ultimosCuatroDigitos,
    String titular,
    String mensaje
){}
