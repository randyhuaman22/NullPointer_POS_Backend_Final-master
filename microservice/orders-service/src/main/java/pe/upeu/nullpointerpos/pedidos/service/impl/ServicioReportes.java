package pe.upeu.nullpointerpos.pedidos.service.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pe.upeu.nullpointerpos.pedidos.repository.PedidoRepositorio;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** [SPRINT 3 / HU-27/HU-28/HU-30 / S3-T-44..S3-T-58 START] Los reportes consultan solo la BD de orders-service. */
@Service
public class ServicioReportes {
    private final PedidoRepositorio repositorio;
    public ServicioReportes(PedidoRepositorio repositorio) { this.repositorio = repositorio; }
    public List<Map<String,Object>> ventasPorHora(LocalDate desde, LocalDate hasta) { return repositorio.ventasPorHora(desde,hasta); }
    public List<Map<String,Object>> platosPopulares(LocalDate desde, LocalDate hasta) { return repositorio.platosPopulares(desde,hasta); }
    public byte[] exportarVentas(LocalDate desde, LocalDate hasta) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Ventas");
            Row encabezado = hoja.createRow(0); encabezado.createCell(0).setCellValue("Hora"); encabezado.createCell(1).setCellValue("Pedidos pagados"); encabezado.createCell(2).setCellValue("Total vendido");
            int fila = 1;
            for (Map<String,Object> r : ventasPorHora(desde,hasta)) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(((Number)r.get("hora")).intValue());
                row.createCell(1).setCellValue(((Number)r.get("pedidos")).intValue());
                row.createCell(2).setCellValue(((Number)r.get("total")).doubleValue());
            }
            for (int c=0;c<3;c++) hoja.autoSizeColumn(c);
            Sheet populares = libro.createSheet("Platos populares");
            Row h2 = populares.createRow(0); h2.createCell(0).setCellValue("Producto"); h2.createCell(1).setCellValue("Unidades"); h2.createCell(2).setCellValue("Venta total");
            int f2=1;
            for (Map<String,Object> r : platosPopulares(desde,hasta)) {
                Row row=populares.createRow(f2++); row.createCell(0).setCellValue(String.valueOf(r.get("nombre_producto")));
                row.createCell(1).setCellValue(((Number)r.get("unidades")).doubleValue()); row.createCell(2).setCellValue(((Number)r.get("venta")).doubleValue());
            }
            for (int c=0;c<3;c++) populares.autoSizeColumn(c);
            libro.write(salida); return salida.toByteArray();
        } catch (IOException ex) { throw new IllegalStateException("No se pudo generar el Excel de ventas.", ex); }
    }
}
// [SPRINT 3 / HU-27/HU-28/HU-30 / S3-T-44..S3-T-58 END]
