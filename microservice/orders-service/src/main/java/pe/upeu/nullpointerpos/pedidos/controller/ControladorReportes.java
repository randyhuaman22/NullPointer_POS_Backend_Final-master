package pe.upeu.nullpointerpos.pedidos.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.upeu.nullpointerpos.pedidos.service.impl.ServicioReportes;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/** [SPRINT 3 / HU-27..HU-30 / S3-T-43..S3-T-59 START] Reportes de ventas, platos populares y Excel. */
@RestController
@RequestMapping("/api/v1/reportes")
public class ControladorReportes {
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private final ServicioReportes reportes;
    public ControladorReportes(ServicioReportes reportes) { this.reportes = reportes; }
    private LocalDate normalizar(LocalDate fecha) { return fecha == null ? LocalDate.now(ZONA_LIMA) : fecha; }
    @GetMapping("/ventas-por-hora")
    public List<Map<String,Object>> ventasPorHora(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
                                                  @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate d=normalizar(desde), h=normalizar(hasta); if (h.isBefore(d)) throw new IllegalArgumentException("hasta no puede ser anterior a desde");
        return reportes.ventasPorHora(d,h);
    }
    @GetMapping("/platos-populares")
    public List<Map<String,Object>> platosPopulares(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
                                                    @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate d=normalizar(desde), h=normalizar(hasta); if (h.isBefore(d)) throw new IllegalArgumentException("hasta no puede ser anterior a desde");
        return reportes.platosPopulares(d,h);
    }
    @GetMapping(value="/ventas/exportar.xlsx", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportar(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate d=normalizar(desde), h=normalizar(hasta); if (h.isBefore(d)) throw new IllegalArgumentException("hasta no puede ser anterior a desde");
        byte[] bytes=reportes.exportarVentas(d,h);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=NullPointer_POS_ventas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(bytes);
    }
}
/** [SPRINT 3 / HU-27..HU-30 / S3-T-43..S3-T-59 END] */
