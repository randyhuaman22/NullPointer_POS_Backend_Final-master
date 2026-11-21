-- [SPRINT 3 / HU-22 / S3-T-30 START]
-- Separar el stock físicamente descontado del faltante teórico de una venta.
-- Es una migración nueva para no modificar el checksum de V1.
CREATE TABLE IF NOT EXISTS esquema_inventario.faltantes_venta(
 id_faltante BIGSERIAL PRIMARY KEY,
 id_pedido BIGINT NOT NULL,
 id_producto BIGINT NOT NULL,
 id_insumo BIGINT NOT NULL REFERENCES esquema_inventario.insumos(id_insumo),
 cantidad_requerida NUMERIC(14,3) NOT NULL CHECK(cantidad_requerida>0),
 cantidad_disponible NUMERIC(14,3) NOT NULL CHECK(cantidad_disponible>=0),
 cantidad_faltante NUMERIC(14,3) NOT NULL CHECK(cantidad_faltante>0),
 fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_faltantes_pedido_fecha ON esquema_inventario.faltantes_venta(id_pedido,fecha_creacion DESC);
-- [SPRINT 3 / HU-22 / S3-T-30 END]
