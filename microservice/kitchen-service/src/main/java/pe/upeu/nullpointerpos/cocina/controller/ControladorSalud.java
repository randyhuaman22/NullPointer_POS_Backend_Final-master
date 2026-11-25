package pe.upeu.nullpointerpos.cocina.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class ControladorSalud {
    @GetMapping("/api/v1/salud")
    public Map<String, Object> salud() {
        return Map.of("estado", "ACTIVO", "aplicacion", "NullPointer POS - Servicio de Cocina", "alcance", "Recepción de pedidos del Sprint 1");
    }
}
