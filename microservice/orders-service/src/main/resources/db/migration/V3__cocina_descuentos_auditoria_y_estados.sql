-- HU-10 / T-16..T-19: trazabilidad del estado individual del plato.
ALTER TABLE esquema_comandas.detalles_pedido
    ADD COLUMN IF NOT EXISTS estacion_cocina VARCHAR(60),
    ADD COLUMN IF NOT EXISTS fecha_finalizacion TIMESTAMPTZ;

-- HU-17: ya existe total_descuento en instalaciones nuevas de V1; el IF NOT EXISTS permite bases antiguas.
ALTER TABLE esquema_comandas.pedidos
    ADD COLUMN IF NOT EXISTS total_descuento NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS motivo_anulacion TEXT,
    ADD COLUMN IF NOT EXISTS usuario_descuento VARCHAR(30);

CREATE INDEX IF NOT EXISTS ix_pedidos_fecha_estado ON esquema_comandas.pedidos(fecha_creacion, estado);
CREATE INDEX IF NOT EXISTS ix_pedidos_fecha_pago ON esquema_comandas.pedidos(fecha_pago) WHERE estado = 'PAGADO';
CREATE INDEX IF NOT EXISTS ix_detalle_estado ON esquema_comandas.detalles_pedido(id_pedido, estado);
