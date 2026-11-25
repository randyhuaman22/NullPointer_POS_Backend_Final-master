package pe.upeu.nullpointerpos.inventario.repository;

import com.fasterxml.jackson.databind.JsonNode;import com.fasterxml.jackson.databind.ObjectMapper;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;import org.springframework.stereotype.Repository;import org.springframework.transaction.annotation.Transactional;import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
import pe.upeu.nullpointerpos.inventario.dto.request.*;import java.math.BigDecimal;import java.util.*;
@Repository public class RepositorioInventario {
 private final JdbcTemplate jdbc;private final ObjectMapper json;public RepositorioInventario(JdbcTemplate j,ObjectMapper o){jdbc=j;json=o;}
 public List<Map<String,Object>> listar(){return jdbc.queryForList("SELECT id_insumo,codigo,nombre,unidad,stock_actual,stock_minimo,costo_unitario,activo,fecha_actualizacion,(stock_actual<=stock_minimo) AS bajo_minimo FROM esquema_inventario.insumos WHERE activo=TRUE ORDER BY nombre");}
 @Transactional public Map<String,Object> crear(SolicitudInsumo s,Long actor){Long id=jdbc.queryForObject("INSERT INTO esquema_inventario.insumos(codigo,nombre,unidad,stock_actual,stock_minimo,costo_unitario) VALUES(?,?,?,?,?,?) RETURNING id_insumo",Long.class,s.codigo(),s.nombre().trim(),s.unidad().trim(),s.stockInicial(),s.stockMinimo(),s.costoUnitario());auditarInsumo(id,actor,"CREAR","jsonb_build_object('nombre', ?, 'unidad', ?)",s.nombre().trim(),s.unidad().trim());if(s.stockInicial().signum()>0)jdbc.update("INSERT INTO esquema_inventario.movimientos(id_insumo,tipo,cantidad,costo_unitario,referencia,observacion,id_usuario_actor) VALUES(?,'AJUSTE',?,?,?,'Stock inicial',?)",id,s.stockInicial(),s.costoUnitario(),"ALTA-"+id,actor);return buscar(id);}
 public Map<String,Object> buscar(Long id){return jdbc.query("SELECT * FROM esquema_inventario.insumos WHERE id_insumo=?",rs->rs.next()?Map.of("idInsumo",rs.getLong("id_insumo"),"codigo",Objects.toString(rs.getString("codigo"),""),"nombre",rs.getString("nombre"),"unidad",rs.getString("unidad"),"stockActual",rs.getBigDecimal("stock_actual"),"stockMinimo",rs.getBigDecimal("stock_minimo"),"costoUnitario",rs.getBigDecimal("costo_unitario"),"activo",rs.getBoolean("activo")):null,id);}
 @Transactional
 public Map<String,Object> actualizarInsumo(Long id,pe.upeu.nullpointerpos.inventario.dto.request.SolicitudActualizarInsumo s,Long actor){
  int n=jdbc.update("UPDATE esquema_inventario.insumos SET codigo=NULLIF(UPPER(?),''),nombre=?,unidad=?,stock_minimo=?,costo_unitario=?,fecha_actualizacion=NOW() WHERE id_insumo=? AND activo=TRUE",
    s.codigo()==null?null:s.codigo().trim(),s.nombre().trim(),s.unidad().trim(),s.stockMinimo(),s.costoUnitario(),id);
  if(n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Insumo no encontrado o inactivo.");
  auditarInsumo(id,actor,"MODIFICAR","jsonb_build_object('nombre', ?, 'unidad', ?, 'stockMinimo', ?, 'costoUnitario', ?)",s.nombre().trim(),s.unidad().trim(),s.stockMinimo(),s.costoUnitario());
  return buscar(id);
 }
 public boolean tieneRecetas(Long id){Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM esquema_inventario.recetas WHERE id_insumo=?",Integer.class,id);return n!=null&&n>0;}
 @Transactional
 public boolean cambiarActivo(Long id,boolean activo,Long actor){
  int n=jdbc.update("UPDATE esquema_inventario.insumos SET activo=?,fecha_actualizacion=NOW() WHERE id_insumo=? AND activo=?",activo,id,!activo);
  if(n>0)auditarInsumo(id,actor,activo?"RESTAURAR":"DESACTIVAR","jsonb_build_object('activo', ?)",activo);
  return n>0;
 }
 @Transactional public Map<String,Object> ajustarStock(Long id,pe.upeu.nullpointerpos.inventario.dto.request.SolicitudAjusteStock s,Long actor){
  Map<String,Object> anterior;
  try { anterior=jdbc.queryForMap("SELECT stock_actual AS \"stockActual\" FROM esquema_inventario.insumos WHERE id_insumo=? AND activo=TRUE FOR UPDATE",id); }
  catch(org.springframework.dao.EmptyResultDataAccessException ex){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Insumo no encontrado o inactivo.");}
  BigDecimal stockAnterior=(BigDecimal)anterior.get("stockActual");
  BigDecimal diferencia=s.nuevoStock().subtract(stockAnterior);
  if(diferencia.signum()==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El nuevo stock es igual al stock registrado; no hay ajuste que aplicar.");
  jdbc.update("UPDATE esquema_inventario.insumos SET stock_actual=?,fecha_actualizacion=NOW() WHERE id_insumo=? AND activo=TRUE",s.nuevoStock(),id);
  jdbc.update("INSERT INTO esquema_inventario.movimientos(id_insumo,tipo,cantidad,referencia,observacion,id_usuario_actor) VALUES(?,'AJUSTE',?,?,?,?)",
    id,diferencia.abs(),"AJUSTE-"+UUID.randomUUID(),(diferencia.signum()>0?"AUMENTO: ":"DISMINUCION: ")+s.motivo(),actor);
  auditarInsumo(id,actor,"AJUSTAR_STOCK","jsonb_build_object('stockAnterior', ?, 'stockNuevo', ?, 'motivo', ?)",stockAnterior,s.nuevoStock(),s.motivo());
  return buscar(id);
 }
 public Map<String,Object> buscarActivo(Long id){Map<String,Object> datos=buscar(id);return datos!=null&&Boolean.TRUE.equals(datos.get("activo"))?datos:null;}
 public List<Map<String,Object>> receta(Long idProducto){return jdbc.queryForList("SELECT r.id_producto,r.id_insumo,i.codigo,i.nombre,i.unidad,r.cantidad_por_plato FROM esquema_inventario.recetas r JOIN esquema_inventario.insumos i ON i.id_insumo=r.id_insumo WHERE r.id_producto=? AND i.activo=TRUE ORDER BY i.nombre",idProducto);}
 public boolean existenTodosInsumosActivos(List<IngredienteReceta> ingredientes){
  if(ingredientes==null||ingredientes.isEmpty())return false;
  List<Long> ids=ingredientes.stream().map(IngredienteReceta::idInsumo).toList();
  Integer cantidad=new NamedParameterJdbcTemplate(jdbc).queryForObject("SELECT COUNT(*) FROM esquema_inventario.insumos WHERE activo=TRUE AND id_insumo IN (:ids)",Map.of("ids",ids),Integer.class);
  return cantidad!=null&&cantidad==ids.size();
 }
 /** Convierte la plantilla jsonb_build_object('clave', ?, 'otra', true) en JSON enlazado como parametro (sin concatenar SQL). */
 private String datosAuditoria(String expresion,Object[] valores){
  final String prefijo="jsonb_build_object(";
  if(expresion==null||!expresion.startsWith(prefijo)||!expresion.endsWith(")"))throw new IllegalArgumentException("Plantilla de auditoria no permitida.");
  String[] t=expresion.substring(prefijo.length(),expresion.length()-1).split(",");
  if(t.length==0||t.length%2!=0)throw new IllegalArgumentException("Plantilla de auditoria no permitida.");
  Map<String,Object> datos=new LinkedHashMap<>();int v=0;
  for(int i=0;i<t.length;i+=2){
   String k=t[i].trim(),x=t[i+1].trim();
   if(k.length()<3||k.charAt(0)!='\''||k.charAt(k.length()-1)!='\'')throw new IllegalArgumentException("Plantilla de auditoria no permitida.");
   Object valor;
   if("?".equals(x)){if(v>=valores.length)throw new IllegalArgumentException("Faltan valores de auditoria.");valor=valores[v++];}
   else if("true".equals(x)||"false".equals(x))valor=Boolean.valueOf(x);
   else throw new IllegalArgumentException("Plantilla de auditoria no permitida.");
   datos.put(k.substring(1,k.length()-1),valor);
  }
  try{return json.writeValueAsString(datos);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("No se pudo serializar la auditoria.",e);}
 }
 private void auditarInsumo(Long id,Long actor,String accion,String expresion,Object... valores){
  jdbc.update("INSERT INTO esquema_inventario.auditoria_insumos(id_insumo,id_usuario_actor,accion,datos) VALUES(?,?,?,CAST(? AS jsonb))",id,actor,accion,datosAuditoria(expresion,valores));
 }
 public List<Map<String,Object>> auditoriaInsumos(Long id){if(id==null)return jdbc.queryForList("SELECT * FROM esquema_inventario.auditoria_insumos ORDER BY fecha_creacion DESC LIMIT 500");return jdbc.queryForList("SELECT * FROM esquema_inventario.auditoria_insumos WHERE id_insumo=? ORDER BY fecha_creacion DESC",id);}

 @Transactional public void guardarReceta(SolicitudReceta s){if(!existenTodosInsumosActivos(s.ingredientes()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La receta contiene insumos inexistentes o inactivos.");jdbc.update("DELETE FROM esquema_inventario.recetas WHERE id_producto=?",s.idProducto());for(var i:s.ingredientes())jdbc.update("INSERT INTO esquema_inventario.recetas(id_producto,id_insumo,cantidad_por_plato) VALUES(?,?,?)",s.idProducto(),i.idInsumo(),i.cantidadPorPlato());}
 @Transactional public Long comprar(SolicitudCompra s,Long actor){Long id=jdbc.queryForObject("INSERT INTO esquema_inventario.compras(proveedor,documento,id_usuario_actor,observacion) VALUES(?,?,?,?) RETURNING id_compra",Long.class,s.proveedor(),s.documento(),actor,s.observacion());for(var i:s.items()){int actualizado=jdbc.update("UPDATE esquema_inventario.insumos SET stock_actual=stock_actual+?,costo_unitario=?,fecha_actualizacion=NOW() WHERE id_insumo=? AND activo=TRUE",i.cantidad(),i.costoUnitario(),i.idInsumo());if(actualizado!=1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El insumo "+i.idInsumo()+" no existe o está inactivo; la compra fue revertida.");jdbc.update("INSERT INTO esquema_inventario.detalles_compra(id_compra,id_insumo,cantidad,costo_unitario) VALUES(?,?,?,?)",id,i.idInsumo(),i.cantidad(),i.costoUnitario());jdbc.update("INSERT INTO esquema_inventario.movimientos(id_insumo,tipo,cantidad,costo_unitario,referencia,observacion,id_usuario_actor) VALUES(?,'COMPRA',?,?,?,?,?)",i.idInsumo(),i.cantidad(),i.costoUnitario(),"COMPRA-"+id,s.observacion(),actor);}return id;}
 @Transactional public void merma(SolicitudMerma s,Long actor){int n=jdbc.update("UPDATE esquema_inventario.insumos SET stock_actual=stock_actual-?,fecha_actualizacion=NOW() WHERE id_insumo=? AND activo=TRUE AND stock_actual>=?",s.cantidad(),s.idInsumo(),s.cantidad());if(n==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Insumo inexistente/inactivo o la merma supera el stock disponible.");jdbc.update("INSERT INTO esquema_inventario.movimientos(id_insumo,tipo,cantidad,referencia,observacion,id_usuario_actor) VALUES(?,'MERMA',?,?,?,?)",s.idInsumo(),s.cantidad(),"MERMA-"+UUID.randomUUID(),s.motivo(),actor);}

 public List<Map<String,Object>> compras(){return jdbc.queryForList("SELECT c.id_compra,c.proveedor,c.documento,c.fecha_compra,c.observacion,COUNT(d.id_detalle) AS cantidad_items,COALESCE(SUM(d.cantidad*d.costo_unitario),0) AS total FROM esquema_inventario.compras c LEFT JOIN esquema_inventario.detalles_compra d ON d.id_compra=c.id_compra GROUP BY c.id_compra ORDER BY c.fecha_compra DESC LIMIT 500");}
 public List<Map<String,Object>> alertas(){return jdbc.queryForList("SELECT id_insumo,codigo,nombre,unidad,stock_actual,stock_minimo FROM esquema_inventario.insumos WHERE activo=TRUE AND stock_actual<=stock_minimo ORDER BY (stock_minimo-stock_actual) DESC,nombre");}
 // [SPRINT 3 / HU-22 / S3-T-27..S3-T-30 START]
 // Consumo idempotente del evento de venta. Se descuenta solo lo disponible,
 // se registra el movimiento físico real y se deja el déficit en una tabla independiente.
 @Transactional
 public void procesarPedidoPagado(Map<String,Object> evento) {
  if (evento == null || !(evento.get("idEvento") instanceof Number)) {
   throw new IllegalArgumentException("Evento de pedido pagado sin idEvento numérico");
  }
  JsonNode root = json.valueToTree(evento);
  long eventId = root.path("idEvento").asLong();
  JsonNode pedido = root.path("pedido");
  long idPedido = pedido.path("idPedido").asLong(0);
  if (eventId <= 0 || idPedido <= 0) {
   throw new IllegalArgumentException("Evento de pedido pagado sin idEvento o idPedido válido");
  }
  JsonNode detalles = pedido.path("detalles");
  if (!detalles.isArray()) {
   throw new IllegalArgumentException("Evento no contiene detalles de pedido");
  }
  int nuevo = jdbc.update(
      "INSERT INTO esquema_inventario.eventos_procesados(id_evento,id_pedido) VALUES(?,?) ON CONFLICT(id_evento) DO NOTHING",
      eventId, idPedido);
  if (nuevo == 0) return; // RabbitMQ puede entregar un mensaje más de una vez.

  for (JsonNode detalle : detalles) {
   long idProducto = detalle.path("idProducto").asLong(0);
   int cantidadPlatos = detalle.path("cantidad").asInt(0);
   if (idProducto <= 0 || cantidadPlatos <= 0) continue;
   BigDecimal factor = BigDecimal.valueOf(cantidadPlatos);
   List<Map<String,Object>> receta = jdbc.queryForList(
       "SELECT r.id_insumo,r.cantidad_por_plato,i.stock_actual,i.costo_unitario " +
       "FROM esquema_inventario.recetas r JOIN esquema_inventario.insumos i ON i.id_insumo=r.id_insumo " +
       "WHERE r.id_producto=? AND i.activo=TRUE ORDER BY r.id_insumo FOR UPDATE OF i", idProducto);
   for (Map<String,Object> item : receta) {
    Long idInsumo = ((Number)item.get("id_insumo")).longValue();
    BigDecimal requerido = ((BigDecimal)item.get("cantidad_por_plato")).multiply(factor);
    BigDecimal stockDisponible = (BigDecimal)item.get("stock_actual");
    BigDecimal costo = (BigDecimal)item.get("costo_unitario");
    BigDecimal descontado = stockDisponible.min(requerido);
    BigDecimal faltante = requerido.subtract(descontado).max(BigDecimal.ZERO);

    if (descontado.signum() > 0) {
     jdbc.update("UPDATE esquema_inventario.insumos SET stock_actual=stock_actual-?,fecha_actualizacion=NOW() WHERE id_insumo=?",
         descontado, idInsumo);
     jdbc.update("INSERT INTO esquema_inventario.movimientos(id_insumo,tipo,cantidad,costo_unitario,referencia,observacion) " +
         "VALUES(?,'VENTA',?,?,?,?)", idInsumo, descontado, costo, "PEDIDO-" + idPedido,
         "Venta: requerido=" + requerido + "; descontado=" + descontado);
    }
    if (faltante.signum() > 0) {
     jdbc.update("INSERT INTO esquema_inventario.faltantes_venta(id_pedido,id_producto,id_insumo,cantidad_requerida,cantidad_disponible,cantidad_faltante) " +
         "VALUES(?,?,?,?,?,?)", idPedido, idProducto, idInsumo, requerido, descontado, faltante);
    }
   }
  }
 }
 // [SPRINT 3 / HU-22 / S3-T-27..S3-T-30 END]
 public List<Map<String,Object>> faltantesVenta(){return jdbc.queryForList("SELECT id_faltante,id_pedido,id_producto,id_insumo,cantidad_requerida,cantidad_disponible,cantidad_faltante,fecha_creacion FROM esquema_inventario.faltantes_venta ORDER BY fecha_creacion DESC LIMIT 500");}
 public List<Map<String,Object>> movimientos(Long id){if(id==null)return jdbc.queryForList("SELECT * FROM esquema_inventario.movimientos ORDER BY fecha_creacion DESC LIMIT 500");return jdbc.queryForList("SELECT * FROM esquema_inventario.movimientos WHERE id_insumo=? ORDER BY fecha_creacion DESC",id);}
}
