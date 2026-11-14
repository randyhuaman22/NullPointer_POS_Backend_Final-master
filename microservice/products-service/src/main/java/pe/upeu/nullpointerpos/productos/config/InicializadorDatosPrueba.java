package pe.upeu.nullpointerpos.productos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class InicializadorDatosPrueba {
    @Bean
    CommandLineRunner inicializarMenu(JdbcTemplate jdbc) {
        return args -> {
            asegurarProducto(jdbc, "POLLO-025", "Pollo a la brasa 1/4", "Pollos", "COCINA", "22.90");
            asegurarProducto(jdbc, "POLLO-050", "Pollo a la brasa 1/2", "Pollos", "COCINA", "39.90");
            asegurarProducto(jdbc, "BEB-COLA", "Gaseosa personal", "Bebidas", "BEBIDAS", "4.00");
            asegurarProducto(jdbc, "GUAR-PAPA", "Papas fritas", "Guarniciones", "COCINA", "8.00");
            asegurarProducto(jdbc, "COMBO-001", "Combo personal", "Combos", "COCINA", "26.90");
        };
    }

    private void asegurarProducto(JdbcTemplate jdbc, String codigo, String nombre, String categoria,
                                  String estacion, String precio) {
        Integer cantidad = jdbc.queryForObject(
                "SELECT COUNT(*) FROM esquema_menu.productos WHERE codigo_producto = ?", Integer.class, codigo);
        if (cantidad != null && cantidad > 0) return;
        int insertados = jdbc.update(
                "INSERT INTO esquema_menu.productos(id_categoria, id_estacion_cocina, codigo_producto, nombre, descripcion, precio_unitario) " +
                "SELECT c.id_categoria, e.id_estacion_cocina, ?, ?, ?, ?::numeric " +
                "FROM esquema_menu.categorias_productos c CROSS JOIN esquema_menu.estaciones_cocina e " +
                "WHERE c.nombre = ? AND e.nombre = ? " +
                "AND NOT EXISTS (SELECT 1 FROM esquema_menu.productos WHERE codigo_producto = ?)",
                codigo, nombre, "Producto de prueba para Sprint 1", precio, categoria, estacion, codigo);
        if (insertados == 0) throw new IllegalStateException("No se pudo registrar el producto de prueba " + codigo);
    }
}
