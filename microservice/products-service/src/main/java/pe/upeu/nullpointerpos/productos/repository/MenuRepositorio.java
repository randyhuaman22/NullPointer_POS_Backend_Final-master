package pe.upeu.nullpointerpos.productos.repository;

import pe.upeu.nullpointerpos.productos.entity.CategoriaProducto;
import pe.upeu.nullpointerpos.productos.entity.Producto;
import pe.upeu.nullpointerpos.productos.mapper.MenuMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MenuRepositorio {
    private final JdbcTemplate jdbc;
    private final MenuMapper mapper;
    public MenuRepositorio(JdbcTemplate jdbc, MenuMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    public List<CategoriaProducto> listarCategorias(String areaNegocio) {
        String sql = "SELECT c.id_categoria, c.nombre, c.descripcion, c.orden_visualizacion, " +
                "a.codigo AS area_negocio, c.id_categoria_padre " +
                "FROM esquema_menu.categorias_productos c " +
                "JOIN esquema_menu.areas_negocio a ON a.id_area_negocio = c.id_area_negocio " +
                "WHERE c.activo = TRUE AND a.activo = TRUE " +
                (areaNegocio == null || areaNegocio.isBlank() ? "" : "AND a.codigo = UPPER(?) ") +
                "ORDER BY a.orden_visualizacion, c.orden_visualizacion, c.nombre";
        if (areaNegocio == null || areaNegocio.isBlank()) {
            return jdbc.query(sql, (rs, fila) -> mapper.categoriaDesdeFila(rs));
        }
        return jdbc.query(sql, (rs, fila) -> mapper.categoriaDesdeFila(rs), areaNegocio.trim());
    }

    public List<Producto> listarProductos(String areaNegocio, String categoria, String busqueda, boolean soloDisponibles) {
        StringBuilder sql = new StringBuilder("SELECT p.id_producto, p.codigo_producto, p.nombre, p.descripcion, p.url_imagen, " +
                "p.precio_unitario, c.nombre AS nombre_categoria, e.nombre AS nombre_estacion_cocina, p.disponible " +
                "FROM esquema_menu.productos p JOIN esquema_menu.categorias_productos c " +
                "ON c.id_categoria = p.id_categoria JOIN esquema_menu.areas_negocio a ON a.id_area_negocio = c.id_area_negocio " +
                "LEFT JOIN esquema_menu.estaciones_cocina e ON e.id_estacion_cocina = p.id_estacion_cocina " +
                "WHERE p.activo = TRUE AND c.activo = TRUE AND a.activo = TRUE ");
        List<Object> argumentos = new ArrayList<>();
        if (areaNegocio != null && !areaNegocio.isBlank()) { sql.append("AND a.codigo = UPPER(?) "); argumentos.add(areaNegocio.trim()); }
        if (soloDisponibles) sql.append("AND p.disponible = TRUE ");
        if (categoria != null && !categoria.isBlank()) { sql.append("AND LOWER(c.nombre) = LOWER(?) "); argumentos.add(categoria.trim()); }
        if (busqueda != null && !busqueda.isBlank()) {
            sql.append("AND (LOWER(p.nombre) LIKE LOWER(?) OR LOWER(COALESCE(p.codigo_producto, '')) LIKE LOWER(?)) ");
            String termino = "%" + busqueda.trim() + "%"; argumentos.add(termino); argumentos.add(termino);
        }
        sql.append("ORDER BY c.orden_visualizacion, p.nombre");
        return jdbc.query(sql.toString(), (rs, fila) -> mapper.productoDesdeFila(rs), argumentos.toArray());
    }

    public Optional<Producto> buscarDisponibleParaPedido(Long idProducto) {
        return jdbc.query("SELECT p.id_producto, p.codigo_producto, p.nombre, p.descripcion, p.url_imagen, " +
                        "p.precio_unitario, c.nombre AS nombre_categoria, e.nombre AS nombre_estacion_cocina, p.disponible " +
                        "FROM esquema_menu.productos p " +
                        "LEFT JOIN esquema_menu.categorias_productos c ON c.id_categoria = p.id_categoria " +
                        "LEFT JOIN esquema_menu.estaciones_cocina e ON e.id_estacion_cocina = p.id_estacion_cocina " +
                        "WHERE p.id_producto = ? AND p.activo = TRUE AND p.disponible = TRUE",
                rs -> rs.next() ? Optional.of(mapper.productoDesdeFila(rs)) : Optional.empty(), idProducto);
    }
    // [T-26 START] Persistencia transaccional de disponibilidad y auditoría del producto.
    @Transactional
    public Producto actualizarDisponibilidad(Long idProducto, boolean disponible, String motivo, String usuarioActor) {
        var anterior = jdbc.query("SELECT disponible FROM esquema_menu.productos WHERE id_producto=? AND activo=TRUE FOR UPDATE",
                rs -> rs.next() ? (Boolean)rs.getObject(1) : null, idProducto);
        if (anterior == null) return null;
        jdbc.update("UPDATE esquema_menu.productos SET disponible=?, fecha_actualizacion=NOW() WHERE id_producto=?", disponible, idProducto);
        jdbc.update("INSERT INTO esquema_menu.auditoria_disponibilidad(id_producto, disponible_anterior, disponible_nuevo, motivo, usuario_actor) VALUES (?,?,?,?,?)",
                idProducto, anterior, disponible, motivo, usuarioActor);
        // El evento se persiste en la misma transacción; el publicador lo reintenta hasta recibir confirmación.
        jdbc.update("INSERT INTO esquema_menu.eventos_disponibilidad(contenido_evento) " +
                "VALUES (jsonb_build_object('tipoEvento','PRODUCTO_DISPONIBILIDAD_ACTUALIZADA','idProducto',?,'disponible',?,'motivo',?,'usuarioActor',?,'fechaHora',NOW()::text))",
                idProducto, disponible, motivo, usuarioActor);
        return jdbc.query("SELECT p.id_producto,p.codigo_producto,p.nombre,p.descripcion,p.url_imagen,p.precio_unitario, c.nombre AS nombre_categoria, e.nombre AS nombre_estacion_cocina,p.disponible " +
                "FROM esquema_menu.productos p LEFT JOIN esquema_menu.categorias_productos c ON c.id_categoria=p.id_categoria " +
                "LEFT JOIN esquema_menu.estaciones_cocina e ON e.id_estacion_cocina=p.id_estacion_cocina WHERE p.id_producto=?",
                rs -> rs.next() ? mapper.productoDesdeFila(rs) : null, idProducto);
    }
    public List<java.util.Map<String,Object>> eventosDisponibilidadPendientes() {
        return jdbc.queryForList("SELECT id_evento, contenido_evento::text AS contenido_evento FROM esquema_menu.eventos_disponibilidad WHERE fecha_publicacion IS NULL ORDER BY id_evento LIMIT 50");
    }
    public void marcarEventoDisponibilidadPublicado(Long idEvento) {
        jdbc.update("UPDATE esquema_menu.eventos_disponibilidad SET fecha_publicacion=NOW() WHERE id_evento=? AND fecha_publicacion IS NULL", idEvento);
    }
    public List<java.util.Map<String,Object>> auditoriaDisponibilidad(Long idProducto) {
        if (idProducto == null) return jdbc.queryForList("SELECT * FROM esquema_menu.auditoria_disponibilidad ORDER BY fecha_creacion DESC LIMIT 500");
        return jdbc.queryForList("SELECT * FROM esquema_menu.auditoria_disponibilidad WHERE id_producto=? ORDER BY fecha_creacion DESC",idProducto);
    }
    // [T-26 END]

}
