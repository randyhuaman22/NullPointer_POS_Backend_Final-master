package pe.upeu.nullpointerpos.productos.repository;

import pe.upeu.nullpointerpos.productos.dto.request.SolicitudCategoria;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudEstacionCocina;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudProducto;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudConfiguracionOpcionesProducto;
import pe.upeu.nullpointerpos.productos.dto.request.SolicitudSeleccionOpcionProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaGrupoOpcionesProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaOpcionProducto;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaOpcionesValidadas;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Set;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaAreaNegocio;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaCategoriaAdministracion;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaEstacionCocina;
import pe.upeu.nullpointerpos.productos.dto.response.RespuestaProductoAdministracion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RepositorioAdministracionMenu {
    private static final com.fasterxml.jackson.databind.ObjectMapper AUDITORIA_JSON = new com.fasterxml.jackson.databind.ObjectMapper();
    private final JdbcTemplate jdbc;

    public RepositorioAdministracionMenu(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RespuestaAreaNegocio> listarAreas() {
        return jdbc.query("""
                SELECT id_area_negocio, codigo, nombre, descripcion, orden_visualizacion
                FROM esquema_menu.areas_negocio
                WHERE activo = TRUE
                ORDER BY orden_visualizacion, nombre
                """, (rs, fila) -> new RespuestaAreaNegocio(
                rs.getLong("id_area_negocio"), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("descripcion"), rs.getInt("orden_visualizacion")));
    }

    public boolean existeAreaActiva(String codigo) {
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_menu.areas_negocio WHERE codigo = UPPER(?) AND activo = TRUE",
                Integer.class, codigo.trim());
        return cantidad != null && cantidad > 0;
    }

    public List<RespuestaCategoriaAdministracion> listarCategoriasAdministracion(String area, boolean incluirInactivas) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id_categoria, c.nombre, c.descripcion, c.orden_visualizacion,
                       a.codigo AS area_negocio, c.id_categoria_padre, c.activo
                FROM esquema_menu.categorias_productos c
                JOIN esquema_menu.areas_negocio a ON a.id_area_negocio = c.id_area_negocio
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (!incluirInactivas) sql.append(" AND c.activo = TRUE AND a.activo = TRUE ");
        if (area != null && !area.isBlank()) {
            sql.append(" AND a.codigo = UPPER(?) ");
            args.add(area.trim());
        }
        sql.append(" ORDER BY a.orden_visualizacion, c.orden_visualizacion, c.nombre ");
        return jdbc.query(sql.toString(), (rs, fila) -> new RespuestaCategoriaAdministracion(
                rs.getLong("id_categoria"), rs.getString("nombre"), rs.getString("descripcion"),
                rs.getInt("orden_visualizacion"), rs.getString("area_negocio"),
                (Long) rs.getObject("id_categoria_padre"), rs.getBoolean("activo")), args.toArray());
    }

    public boolean existeCategoriaActiva(Long idCategoria) {
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_menu.categorias_productos WHERE id_categoria = ? AND activo = TRUE",
                Integer.class, idCategoria);
        return cantidad != null && cantidad > 0;
    }

    public Optional<String> areaDeCategoria(Long idCategoria) {
        List<String> resultados = jdbc.query("""
                SELECT a.codigo FROM esquema_menu.categorias_productos c
                JOIN esquema_menu.areas_negocio a ON a.id_area_negocio = c.id_area_negocio
                WHERE c.id_categoria = ? AND c.activo = TRUE AND a.activo = TRUE
                """, (rs, fila) -> rs.getString(1), idCategoria);
        return resultados.stream().findFirst();
    }

    public boolean existeCategoriaPadreActiva(Long idCategoriaPadre) {
        return existeCategoriaActiva(idCategoriaPadre);
    }

    public boolean esCategoriaRaiz(Long idCategoria) {
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_menu.categorias_productos WHERE id_categoria = ? AND activo = TRUE AND id_categoria_padre IS NULL",
                Integer.class, idCategoria);
        return cantidad != null && cantidad > 0;
    }

    public boolean tieneProductosActivos(Long idCategoria) {
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_menu.productos WHERE id_categoria = ? AND activo = TRUE",
                Integer.class, idCategoria);
        return cantidad != null && cantidad > 0;
    }

    public boolean tieneSubcategoriasActivas(Long idCategoria) {
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_menu.categorias_productos WHERE id_categoria_padre = ? AND activo = TRUE",
                Integer.class, idCategoria);
        return cantidad != null && cantidad > 0;
    }

    @Transactional
    public RespuestaCategoriaAdministracion crearCategoria(SolicitudCategoria solicitud, String actor) {
        Long id = jdbc.queryForObject("""
                INSERT INTO esquema_menu.categorias_productos
                    (id_area_negocio, id_categoria_padre, nombre, descripcion, orden_visualizacion)
                SELECT a.id_area_negocio, ?, ?, ?, COALESCE(?, 0)
                FROM esquema_menu.areas_negocio a
                WHERE a.codigo = UPPER(?) AND a.activo = TRUE
                RETURNING id_categoria
                """, Long.class, solicitud.idCategoriaPadre(), solicitud.nombre().trim(),
                solicitud.descripcion(), solicitud.ordenVisualizacion(), solicitud.areaNegocio());
        registrarAuditoria("CATEGORIA", id, "CREAR", actor,
                "jsonb_build_object('nombre', ?, 'areaNegocio', ?, 'idCategoriaPadre', ?)",
                solicitud.nombre().trim(), solicitud.areaNegocio().toUpperCase(), solicitud.idCategoriaPadre());
        return buscarCategoriaAdministracion(id).orElseThrow();
    }

    @Transactional
    public RespuestaCategoriaAdministracion actualizarCategoria(Long id, SolicitudCategoria solicitud, String actor) {
        int actualizados = jdbc.update("""
                UPDATE esquema_menu.categorias_productos c
                SET id_area_negocio = a.id_area_negocio,
                    id_categoria_padre = ?, nombre = ?, descripcion = ?,
                    orden_visualizacion = COALESCE(?, 0)
                FROM esquema_menu.areas_negocio a
                WHERE c.id_categoria = ? AND c.activo = TRUE
                  AND a.codigo = UPPER(?) AND a.activo = TRUE
                """, solicitud.idCategoriaPadre(), solicitud.nombre().trim(), solicitud.descripcion(),
                solicitud.ordenVisualizacion(), id, solicitud.areaNegocio());
        if (actualizados == 0) return null;
        registrarAuditoria("CATEGORIA", id, "MODIFICAR", actor,
                "jsonb_build_object('nombre', ?, 'areaNegocio', ?, 'idCategoriaPadre', ?)",
                solicitud.nombre().trim(), solicitud.areaNegocio().toUpperCase(), solicitud.idCategoriaPadre());
        return buscarCategoriaAdministracion(id).orElse(null);
    }

    @Transactional
    public boolean desactivarCategoria(Long id, String actor) {
        int actualizados = jdbc.update("UPDATE esquema_menu.categorias_productos SET activo = FALSE WHERE id_categoria = ? AND activo = TRUE", id);
        if (actualizados > 0) registrarAuditoria("CATEGORIA", id, "DESACTIVAR", actor, "jsonb_build_object('activo', false)");
        return actualizados > 0;
    }

    @Transactional
    public boolean restaurarCategoria(Long id, String actor) {
        int actualizados = jdbc.update("""
                UPDATE esquema_menu.categorias_productos c SET activo = TRUE
                FROM esquema_menu.areas_negocio a
                WHERE c.id_categoria = ? AND c.activo = FALSE
                  AND a.id_area_negocio = c.id_area_negocio AND a.activo = TRUE
                  AND (c.id_categoria_padre IS NULL OR EXISTS (
                    SELECT 1 FROM esquema_menu.categorias_productos p
                    WHERE p.id_categoria = c.id_categoria_padre AND p.activo = TRUE))
                """, id);
        if (actualizados > 0) registrarAuditoria("CATEGORIA", id, "RESTAURAR", actor, "jsonb_build_object('activo', true)");
        return actualizados > 0;
    }

    public Optional<RespuestaCategoriaAdministracion> buscarCategoriaAdministracion(Long id) {
        List<RespuestaCategoriaAdministracion> datos = jdbc.query("""
                SELECT c.id_categoria, c.nombre, c.descripcion, c.orden_visualizacion,
                       a.codigo AS area_negocio, c.id_categoria_padre, c.activo
                FROM esquema_menu.categorias_productos c
                JOIN esquema_menu.areas_negocio a ON a.id_area_negocio = c.id_area_negocio
                WHERE c.id_categoria = ?
                """, (rs, fila) -> new RespuestaCategoriaAdministracion(
                rs.getLong("id_categoria"), rs.getString("nombre"), rs.getString("descripcion"),
                rs.getInt("orden_visualizacion"), rs.getString("area_negocio"),
                (Long) rs.getObject("id_categoria_padre"), rs.getBoolean("activo")), id);
        return datos.stream().findFirst();
    }

    public List<RespuestaProductoAdministracion> listarProductosAdministracion(String area, Long idCategoria,
                                                                                 String busqueda, boolean incluirInactivos) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id_producto, p.id_categoria, c.nombre AS categoria, a.codigo AS area_negocio,
                       p.id_estacion_cocina, e.nombre AS estacion_cocina, p.codigo_producto, p.nombre,
                       p.descripcion, p.url_imagen, p.precio_unitario, p.disponible, p.activo,
                       p.tiempo_preparacion_minutos, p.es_destacado, p.orden_visualizacion
                FROM esquema_menu.productos p
                JOIN esquema_menu.categorias_productos c ON c.id_categoria = p.id_categoria
                JOIN esquema_menu.areas_negocio a ON a.id_area_negocio = c.id_area_negocio
                LEFT JOIN esquema_menu.estaciones_cocina e ON e.id_estacion_cocina = p.id_estacion_cocina
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (!incluirInactivos) sql.append(" AND p.activo = TRUE ");
        if (area != null && !area.isBlank()) { sql.append(" AND a.codigo = UPPER(?) "); args.add(area.trim()); }
        if (idCategoria != null) { sql.append(" AND p.id_categoria = ? "); args.add(idCategoria); }
        if (busqueda != null && !busqueda.isBlank()) {
            sql.append(" AND (LOWER(p.nombre) LIKE LOWER(?) OR LOWER(COALESCE(p.codigo_producto, '')) LIKE LOWER(?)) ");
            String termino = "%" + busqueda.trim() + "%"; args.add(termino); args.add(termino);
        }
        sql.append(" ORDER BY a.orden_visualizacion, c.orden_visualizacion, p.orden_visualizacion, p.nombre ");
        return jdbc.query(sql.toString(), (rs, fila) -> mapearProductoAdministracion(rs), args.toArray());
    }

    public boolean existeEstacionActiva(Long idEstacion) {
        if (idEstacion == null) return false;
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_menu.estaciones_cocina WHERE id_estacion_cocina = ? AND activo = TRUE",
                Integer.class, idEstacion);
        return cantidad != null && cantidad > 0;
    }

    @Transactional
    public RespuestaProductoAdministracion crearProducto(SolicitudProducto solicitud, String actor) {
        Long id = jdbc.queryForObject("""
                INSERT INTO esquema_menu.productos
                    (id_categoria, id_estacion_cocina, codigo_producto, nombre, descripcion, url_imagen,
                     precio_unitario, disponible, tiempo_preparacion_minutos, es_destacado, orden_visualizacion)
                VALUES (?, ?, NULLIF(?, ''), ?, ?, ?, ?, COALESCE(?, TRUE), COALESCE(?, 0),
                        COALESCE(?, FALSE), COALESCE(?, 0))
                RETURNING id_producto
                """, Long.class, solicitud.idCategoria(), solicitud.idEstacionCocina(),
                normalizar(solicitud.codigoProducto()), solicitud.nombre().trim(), solicitud.descripcion(),
                solicitud.urlImagen(), solicitud.precioUnitario(), solicitud.disponible(),
                solicitud.tiempoPreparacionMinutos(), solicitud.destacado(), solicitud.ordenVisualizacion());
        registrarAuditoria("PRODUCTO", id, "CREAR", actor,
                "jsonb_build_object('nombre', ?, 'precioUnitario', ?, 'idCategoria', ?)",
                solicitud.nombre().trim(), solicitud.precioUnitario(), solicitud.idCategoria());
        return buscarProductoAdministracion(id).orElseThrow();
    }

    @Transactional
    public RespuestaProductoAdministracion actualizarProducto(Long id, SolicitudProducto solicitud, String actor) {
        int actualizados = jdbc.update("""
                UPDATE esquema_menu.productos
                SET id_categoria = ?, id_estacion_cocina = ?, codigo_producto = NULLIF(?, ''),
                    nombre = ?, descripcion = ?, url_imagen = ?, precio_unitario = ?,
                    disponible = COALESCE(?, disponible),
                    tiempo_preparacion_minutos = COALESCE(?, tiempo_preparacion_minutos),
                    es_destacado = COALESCE(?, es_destacado), orden_visualizacion = COALESCE(?, orden_visualizacion),
                    fecha_actualizacion = NOW()
                WHERE id_producto = ? AND activo = TRUE
                """, solicitud.idCategoria(), solicitud.idEstacionCocina(), normalizar(solicitud.codigoProducto()),
                solicitud.nombre().trim(), solicitud.descripcion(), solicitud.urlImagen(), solicitud.precioUnitario(),
                solicitud.disponible(), solicitud.tiempoPreparacionMinutos(), solicitud.destacado(),
                solicitud.ordenVisualizacion(), id);
        if (actualizados == 0) return null;
        registrarAuditoria("PRODUCTO", id, "MODIFICAR", actor,
                "jsonb_build_object('nombre', ?, 'precioUnitario', ?, 'idCategoria', ?)",
                solicitud.nombre().trim(), solicitud.precioUnitario(), solicitud.idCategoria());
        return buscarProductoAdministracion(id).orElse(null);
    }

    @Transactional
    public boolean desactivarProducto(Long id, String actor) {
        int actualizados = jdbc.update("UPDATE esquema_menu.productos SET activo = FALSE, disponible = FALSE, fecha_actualizacion = NOW() WHERE id_producto = ? AND activo = TRUE", id);
        if (actualizados > 0) registrarAuditoria("PRODUCTO", id, "DESACTIVAR", actor, "jsonb_build_object('activo', false, 'disponible', false)");
        return actualizados > 0;
    }

    @Transactional
    public boolean restaurarProducto(Long id, String actor) {
        int actualizados = jdbc.update("UPDATE esquema_menu.productos SET activo = TRUE, disponible = FALSE, fecha_actualizacion = NOW() WHERE id_producto = ? AND activo = FALSE", id);
        if (actualizados > 0) registrarAuditoria("PRODUCTO", id, "RESTAURAR", actor, "jsonb_build_object('activo', true, 'disponible', false)");
        return actualizados > 0;
    }

    public Optional<RespuestaProductoAdministracion> buscarProductoAdministracion(Long id) {
        List<RespuestaProductoAdministracion> datos = jdbc.query("""
                SELECT p.id_producto, p.id_categoria, c.nombre AS categoria, a.codigo AS area_negocio,
                       p.id_estacion_cocina, e.nombre AS estacion_cocina, p.codigo_producto, p.nombre,
                       p.descripcion, p.url_imagen, p.precio_unitario, p.disponible, p.activo,
                       p.tiempo_preparacion_minutos, p.es_destacado, p.orden_visualizacion
                FROM esquema_menu.productos p
                JOIN esquema_menu.categorias_productos c ON c.id_categoria = p.id_categoria
                JOIN esquema_menu.areas_negocio a ON a.id_area_negocio = c.id_area_negocio
                LEFT JOIN esquema_menu.estaciones_cocina e ON e.id_estacion_cocina = p.id_estacion_cocina
                WHERE p.id_producto = ?
                """, (rs, fila) -> mapearProductoAdministracion(rs), id);
        return datos.stream().findFirst();
    }

    public List<RespuestaEstacionCocina> listarEstaciones() {
        return jdbc.query("SELECT id_estacion_cocina, nombre, activo FROM esquema_menu.estaciones_cocina ORDER BY nombre",
                (rs, fila) -> new RespuestaEstacionCocina(rs.getLong("id_estacion_cocina"), rs.getString("nombre"), rs.getBoolean("activo")));
    }

    @Transactional
    public RespuestaEstacionCocina crearEstacion(SolicitudEstacionCocina solicitud, String actor) {
        Long id = jdbc.queryForObject("INSERT INTO esquema_menu.estaciones_cocina(nombre) VALUES (?) RETURNING id_estacion_cocina",
                Long.class, solicitud.nombre().trim().toUpperCase());
        registrarAuditoria("ESTACION", id, "CREAR", actor, "jsonb_build_object('nombre', ?)", solicitud.nombre().trim().toUpperCase());
        return buscarEstacion(id).orElseThrow();
    }

    @Transactional
    public RespuestaEstacionCocina actualizarEstacion(Long id, SolicitudEstacionCocina solicitud, String actor) {
        int actualizados = jdbc.update("UPDATE esquema_menu.estaciones_cocina SET nombre = ? WHERE id_estacion_cocina = ? AND activo = TRUE",
                solicitud.nombre().trim().toUpperCase(), id);
        if (actualizados == 0) return null;
        registrarAuditoria("ESTACION", id, "MODIFICAR", actor, "jsonb_build_object('nombre', ?)", solicitud.nombre().trim().toUpperCase());
        return buscarEstacion(id).orElse(null);
    }

    @Transactional
    public boolean desactivarEstacion(Long id, String actor) {
        int actualizados = jdbc.update("UPDATE esquema_menu.estaciones_cocina SET activo = FALSE WHERE id_estacion_cocina = ? AND activo = TRUE", id);
        if (actualizados > 0) registrarAuditoria("ESTACION", id, "DESACTIVAR", actor, "jsonb_build_object('activo', false)");
        return actualizados > 0;
    }

    @Transactional
    public boolean restaurarEstacion(Long id, String actor) {
        int actualizados = jdbc.update("UPDATE esquema_menu.estaciones_cocina SET activo = TRUE WHERE id_estacion_cocina = ? AND activo = FALSE", id);
        if (actualizados > 0) registrarAuditoria("ESTACION", id, "RESTAURAR", actor, "jsonb_build_object('activo', true)");
        return actualizados > 0;
    }

    public boolean estacionTieneProductosActivos(Long id) {
        Integer cantidad = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_menu.productos WHERE id_estacion_cocina = ? AND activo = TRUE",
                Integer.class, id);
        return cantidad != null && cantidad > 0;
    }

    private Optional<RespuestaEstacionCocina> buscarEstacion(Long id) {
        List<RespuestaEstacionCocina> datos = jdbc.query("SELECT id_estacion_cocina, nombre, activo FROM esquema_menu.estaciones_cocina WHERE id_estacion_cocina = ?",
                (rs, fila) -> new RespuestaEstacionCocina(rs.getLong("id_estacion_cocina"), rs.getString("nombre"), rs.getBoolean("activo")), id);
        return datos.stream().findFirst();
    }

    public List<Map<String, Object>> listarAuditoria(String entidad, Long idEntidad, int limite) {
        StringBuilder sql = new StringBuilder("SELECT id_auditoria, tipo_entidad, id_entidad, accion, usuario_actor, datos::text AS datos, fecha_creacion FROM esquema_menu.auditoria_catalogo WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (entidad != null && !entidad.isBlank()) { sql.append("AND tipo_entidad = UPPER(?) "); args.add(entidad.trim()); }
        if (idEntidad != null) { sql.append("AND id_entidad = ? "); args.add(idEntidad); }
        sql.append("ORDER BY fecha_creacion DESC LIMIT ?");
        args.add(Math.max(1, Math.min(limite, 500)));
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    private void registrarAuditoria(String entidad, Long id, String accion, String actor,
                                    String expresionJson, Object... valores) {
        jdbc.update("INSERT INTO esquema_menu.auditoria_catalogo(tipo_entidad,id_entidad,accion,usuario_actor,datos) VALUES (?,?,?,?,CAST(? AS jsonb))",
                entidad, id, accion, actor, datosAuditoria(expresionJson, valores));
    }

    /** Convierte la plantilla jsonb_build_object('clave', ?, 'otra', true) en JSON enlazado como parametro (sin concatenar SQL). */
    private static String datosAuditoria(String expresion, Object[] valores) {
        final String prefijo = "jsonb_build_object(";
        if (expresion == null || !expresion.startsWith(prefijo) || !expresion.endsWith(")"))
            throw new IllegalArgumentException("Plantilla de auditoria no permitida.");
        String[] t = expresion.substring(prefijo.length(), expresion.length() - 1).split(",");
        if (t.length == 0 || t.length % 2 != 0) throw new IllegalArgumentException("Plantilla de auditoria no permitida.");
        Map<String, Object> datos = new LinkedHashMap<>();
        int v = 0;
        for (int i = 0; i < t.length; i += 2) {
            String k = t[i].trim(), x = t[i + 1].trim();
            if (k.length() < 3 || k.charAt(0) != '\'' || k.charAt(k.length() - 1) != '\'')
                throw new IllegalArgumentException("Plantilla de auditoria no permitida.");
            Object valor;
            if ("?".equals(x)) {
                if (v >= valores.length) throw new IllegalArgumentException("Faltan valores de auditoria.");
                valor = valores[v++];
            } else if ("true".equals(x) || "false".equals(x)) {
                valor = Boolean.valueOf(x);
            } else {
                throw new IllegalArgumentException("Plantilla de auditoria no permitida.");
            }
            datos.put(k.substring(1, k.length() - 1), valor);
        }
        try {
            return AUDITORIA_JSON.writeValueAsString(datos);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la auditoria.", e);
        }
    }

    private RespuestaProductoAdministracion mapearProductoAdministracion(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RespuestaProductoAdministracion(
                rs.getLong("id_producto"), rs.getLong("id_categoria"), rs.getString("categoria"),
                rs.getString("area_negocio"), (Long) rs.getObject("id_estacion_cocina"),
                rs.getString("estacion_cocina"), rs.getString("codigo_producto"), rs.getString("nombre"),
                rs.getString("descripcion"), rs.getString("url_imagen"), rs.getBigDecimal("precio_unitario"),
                rs.getBoolean("disponible"), rs.getBoolean("activo"), rs.getInt("tiempo_preparacion_minutos"),
                rs.getBoolean("es_destacado"), rs.getInt("orden_visualizacion"));
    }


    public List<RespuestaGrupoOpcionesProducto> listarOpcionesProducto(Long idProducto) {
        if (!productoActivo(idProducto)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado o inactivo.");
        }
        List<Map<String, Object>> grupos = jdbc.queryForList("""
                SELECT id_grupo_opcion, nombre, obligatorio, seleccion_minima, seleccion_maxima, orden_visualizacion
                FROM esquema_menu.grupos_opciones_producto
                WHERE id_producto = ? AND activo = TRUE
                ORDER BY orden_visualizacion, nombre
                """, idProducto);
        List<RespuestaGrupoOpcionesProducto> respuesta = new ArrayList<>();
        for (Map<String, Object> grupo : grupos) {
            Long idGrupo = ((Number) grupo.get("id_grupo_opcion")).longValue();
            List<RespuestaOpcionProducto> opciones = jdbc.query("""
                    SELECT id_opcion, nombre, precio_adicional, orden_visualizacion
                    FROM esquema_menu.opciones_producto
                    WHERE id_grupo_opcion = ? AND activo = TRUE
                    ORDER BY orden_visualizacion, nombre
                    """, (rs, fila) -> new RespuestaOpcionProducto(
                    rs.getLong("id_opcion"), rs.getString("nombre"), rs.getBigDecimal("precio_adicional"),
                    rs.getInt("orden_visualizacion")), idGrupo);
            respuesta.add(new RespuestaGrupoOpcionesProducto(idGrupo, (String) grupo.get("nombre"),
                    (Boolean) grupo.get("obligatorio"), ((Number) grupo.get("seleccion_minima")).intValue(),
                    ((Number) grupo.get("seleccion_maxima")).intValue(),
                    ((Number) grupo.get("orden_visualizacion")).intValue(), opciones));
        }
        return respuesta;
    }

    @Transactional
    public List<RespuestaGrupoOpcionesProducto> guardarOpcionesProducto(
            Long idProducto, SolicitudConfiguracionOpcionesProducto solicitud, String actor) {
        if (!productoActivo(idProducto)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado o inactivo.");
        }
        jdbc.update("UPDATE esquema_menu.grupos_opciones_producto SET activo = FALSE WHERE id_producto = ? AND activo = TRUE", idProducto);
        int indiceGrupo = 0;
        for (var grupo : solicitud.grupos()) {
            validarGrupoOpciones(grupo.nombre(), grupo.obligatorio(), grupo.seleccionMinima(),
                    grupo.seleccionMaxima(), grupo.opciones());
            Long idGrupo = jdbc.queryForObject("""
                    INSERT INTO esquema_menu.grupos_opciones_producto
                        (id_producto, nombre, obligatorio, seleccion_minima, seleccion_maxima, orden_visualizacion, activo)
                    VALUES (?, ?, ?, ?, ?, ?, TRUE) RETURNING id_grupo_opcion
                    """, Long.class, idProducto, grupo.nombre().trim(), grupo.obligatorio(),
                    grupo.seleccionMinima(), grupo.seleccionMaxima(),
                    grupo.ordenVisualizacion() == null ? indiceGrupo : grupo.ordenVisualizacion());
            Set<String> nombres = new HashSet<>();
            int indiceOpcion = 0;
            for (var opcion : grupo.opciones()) {
                String nombre = opcion.nombre().trim();
                if (!nombres.add(nombre.toLowerCase(java.util.Locale.ROOT))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "No repitas el nombre de una opción dentro del mismo grupo: " + nombre);
                }
                jdbc.update("""
                        INSERT INTO esquema_menu.opciones_producto
                            (id_grupo_opcion, nombre, precio_adicional, orden_visualizacion, activo)
                        VALUES (?, ?, ?, ?, TRUE)
                        """, idGrupo, nombre, opcion.precioAdicional(),
                        opcion.ordenVisualizacion() == null ? indiceOpcion : opcion.ordenVisualizacion());
                indiceOpcion++;
            }
            indiceGrupo++;
        }
        registrarAuditoria("PRODUCTO", idProducto, "CONFIGURAR_OPCIONES", actor,
                "jsonb_build_object('cantidadGrupos', ?)", solicitud.grupos().size());
        return listarOpcionesProducto(idProducto);
    }

    public RespuestaOpcionesValidadas validarOpcionesProducto(
            Long idProducto, List<SolicitudSeleccionOpcionProducto> seleccionadas) {
        if (!productoActivoYDisponible(idProducto)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El producto no existe, está inactivo o no está disponible.");
        }
        List<SolicitudSeleccionOpcionProducto> seleccion = seleccionadas == null ? List.of() : seleccionadas;
        Set<Long> idsUnicos = new HashSet<>();
        for (var opcion : seleccion) {
            if (opcion.idOpcion() == null || !idsUnicos.add(opcion.idOpcion())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La selección contiene opciones repetidas o inválidas.");
            }
        }

        List<Map<String, Object>> grupos = jdbc.queryForList("""
                SELECT id_grupo_opcion, nombre, obligatorio, seleccion_minima, seleccion_maxima
                FROM esquema_menu.grupos_opciones_producto
                WHERE id_producto = ? AND activo = TRUE
                ORDER BY orden_visualizacion, nombre
                """, idProducto);
        Map<Long, Integer> cantidadPorGrupo = new LinkedHashMap<>();
        for (var grupo : grupos) cantidadPorGrupo.put(((Number) grupo.get("id_grupo_opcion")).longValue(), 0);

        BigDecimal recargo = BigDecimal.ZERO;
        List<String> resumen = new ArrayList<>();
        if (!seleccion.isEmpty()) {
            List<Object> idsOpciones = seleccion.stream().map(SolicitudSeleccionOpcionProducto::idOpcion)
                    .map(id -> (Object) id).toList();
            List<Map<String, Object>> elegidas = new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbc).queryForList("""
                    SELECT o.id_opcion, o.nombre AS opcion, o.precio_adicional,
                           g.id_grupo_opcion, g.nombre AS grupo
                    FROM esquema_menu.opciones_producto o
                    JOIN esquema_menu.grupos_opciones_producto g ON g.id_grupo_opcion = o.id_grupo_opcion
                    WHERE o.id_opcion IN (:ids) AND o.activo = TRUE AND g.activo = TRUE AND g.id_producto = :idProducto
                    """,
                    new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("ids", idsOpciones).addValue("idProducto", idProducto));
            if (elegidas.size() != seleccion.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Una o más opciones no pertenecen al producto o están inactivas.");
            }
            Map<Long, SolicitudSeleccionOpcionProducto> porId = new LinkedHashMap<>();
            seleccion.forEach(o -> porId.put(o.idOpcion(), o));
            for (Map<String, Object> elegida : elegidas) {
                Long idGrupo = ((Number) elegida.get("id_grupo_opcion")).longValue();
                cantidadPorGrupo.computeIfPresent(idGrupo, (k, v) -> v + 1);
                SolicitudSeleccionOpcionProducto opcion = porId.get(((Number) elegida.get("id_opcion")).longValue());
                BigDecimal unitario = (BigDecimal) elegida.get("precio_adicional");
                recargo = recargo.add(unitario.multiply(BigDecimal.valueOf(opcion.cantidad())));
                String nombre = (String) elegida.get("opcion");
                resumen.add(opcion.cantidad() > 1 ? nombre + " x" + opcion.cantidad() : nombre);
            }
        }
        for (Map<String, Object> grupo : grupos) {
            Long idGrupo = ((Number) grupo.get("id_grupo_opcion")).longValue();
            int minimo = ((Number) grupo.get("seleccion_minima")).intValue();
            int maximo = ((Number) grupo.get("seleccion_maxima")).intValue();
            int cantidad = cantidadPorGrupo.getOrDefault(idGrupo, 0);
            if (cantidad < minimo || cantidad > maximo) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El grupo '" + grupo.get("nombre") + "' requiere seleccionar entre " + minimo + " y " + maximo + " opción(es).");
            }
        }
        return new RespuestaOpcionesValidadas(recargo.setScale(2, java.math.RoundingMode.HALF_UP), resumen);
    }

    private boolean productoActivo(Long idProducto) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_menu.productos WHERE id_producto = ? AND activo = TRUE", Integer.class, idProducto);
        return n != null && n > 0;
    }

    private boolean productoActivoYDisponible(Long idProducto) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM esquema_menu.productos WHERE id_producto = ? AND activo = TRUE AND disponible = TRUE", Integer.class, idProducto);
        return n != null && n > 0;
    }

    private void validarGrupoOpciones(String nombre, boolean obligatorio, int minimo, int maximo,
                                      List<?> opciones) {
        if (obligatorio && minimo < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un grupo obligatorio debe exigir al menos una selección.");
        }
        if (minimo < 0 || maximo < 1 || minimo > maximo || maximo > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El grupo '" + nombre + "' debe tener límites de selección válidos (0-20).");
        }
        if (opciones == null || opciones.isEmpty() || opciones.size() > 50 || maximo > opciones.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cada grupo debe contener entre 1 y 50 opciones y su máximo no puede superar las opciones disponibles.");
        }
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim().toUpperCase();
    }
}
