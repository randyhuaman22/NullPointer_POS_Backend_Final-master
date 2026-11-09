-- El catálogo ahora es propiedad de products-service / bd_menu.
-- No se eliminan las tablas antiguas para preservar datos que pudieran existir
-- en instalaciones previas. orders-service ya no consulta estas tablas.
ALTER TABLE IF EXISTS esquema_comandas.detalles_pedido
    DROP CONSTRAINT IF EXISTS detalles_pedido_id_producto_fkey;

COMMENT ON TABLE esquema_comandas.productos IS
    'LEGACY: catálogo anterior. El catálogo vigente pertenece a products-service y bd_menu.';
COMMENT ON TABLE esquema_comandas.categorias_productos IS
    'LEGACY: categorías anteriores. Las categorías vigentes pertenecen a products-service y bd_menu.';
COMMENT ON TABLE esquema_comandas.estaciones_cocina IS
    'LEGACY: estaciones anteriores. La configuración vigente pertenece a products-service y bd_menu.';
