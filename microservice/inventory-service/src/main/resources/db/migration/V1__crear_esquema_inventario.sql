CREATE SCHEMA IF NOT EXISTS esquema_inventario;
CREATE TABLE IF NOT EXISTS esquema_inventario.insumos(
 id_insumo BIGSERIAL PRIMARY KEY,codigo VARCHAR(40) UNIQUE,nombre VARCHAR(120) NOT NULL,unidad VARCHAR(20) NOT NULL,
 stock_actual NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK(stock_actual>=0),stock_minimo NUMERIC(14,3) NOT NULL DEFAULT 0 CHECK(stock_minimo>=0),costo_unitario NUMERIC(12,4) NOT NULL DEFAULT 0 CHECK(costo_unitario>=0),activo BOOLEAN NOT NULL DEFAULT TRUE,fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE TABLE IF NOT EXISTS esquema_inventario.recetas(
 id_receta BIGSERIAL PRIMARY KEY,id_producto BIGINT NOT NULL,id_insumo BIGINT NOT NULL REFERENCES esquema_inventario.insumos(id_insumo),cantidad_por_plato NUMERIC(14,3) NOT NULL CHECK(cantidad_por_plato>0),UNIQUE(id_producto,id_insumo));
CREATE TABLE IF NOT EXISTS esquema_inventario.movimientos(
 id_movimiento BIGSERIAL PRIMARY KEY,id_insumo BIGINT NOT NULL REFERENCES esquema_inventario.insumos(id_insumo),tipo VARCHAR(20) NOT NULL CHECK(tipo IN('COMPRA','MERMA','VENTA','AJUSTE')),cantidad NUMERIC(14,3) NOT NULL CHECK(cantidad>0),costo_unitario NUMERIC(12,4) NOT NULL DEFAULT 0,referencia VARCHAR(100),observacion VARCHAR(300),id_usuario_actor BIGINT,fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE TABLE IF NOT EXISTS esquema_inventario.eventos_procesados(id_evento BIGINT PRIMARY KEY,id_pedido BIGINT NOT NULL,fecha_procesado TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE TABLE IF NOT EXISTS esquema_inventario.compras(id_compra BIGSERIAL PRIMARY KEY,proveedor VARCHAR(160),documento VARCHAR(80),fecha_compra TIMESTAMPTZ NOT NULL DEFAULT NOW(),id_usuario_actor BIGINT,observacion VARCHAR(300));
CREATE TABLE IF NOT EXISTS esquema_inventario.detalles_compra(id_detalle BIGSERIAL PRIMARY KEY,id_compra BIGINT NOT NULL REFERENCES esquema_inventario.compras(id_compra) ON DELETE CASCADE,id_insumo BIGINT NOT NULL REFERENCES esquema_inventario.insumos(id_insumo),cantidad NUMERIC(14,3) NOT NULL CHECK(cantidad>0),costo_unitario NUMERIC(12,4) NOT NULL CHECK(costo_unitario>=0));
CREATE INDEX IF NOT EXISTS ix_insumos_stock ON esquema_inventario.insumos(stock_actual,stock_minimo) WHERE activo=TRUE;
CREATE INDEX IF NOT EXISTS ix_movimientos_insumo_fecha ON esquema_inventario.movimientos(id_insumo,fecha_creacion DESC);
