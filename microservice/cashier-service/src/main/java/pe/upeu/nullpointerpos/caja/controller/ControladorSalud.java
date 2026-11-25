package pe.upeu.nullpointerpos.caja.controller;
import org.springframework.web.bind.annotation.GetMapping;import org.springframework.web.bind.annotation.RestController;
@RestController public class ControladorSalud { @GetMapping("/api/v1/salud") public java.util.Map<String,String> salud(){return java.util.Map.of("servicio","servicio-caja","estado","UP");} }
