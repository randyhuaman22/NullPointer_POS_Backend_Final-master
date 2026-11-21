package pe.upeu.nullpointerpos.cocina.service;

import java.util.Map;

public interface ServicioEventosCocina {
    void publicarEnPantalla(Map<String, Object> mensaje);
    void publicarDisponibilidad(Map<String, Object> mensaje);
}
