CREATE TABLE IF NOT EXISTS esquema_inventario.auditoria_insumos (
    id_auditoria BIGSERIAL PRIMARY KEY,
    id_insumo BIGINT REFERENCES esquema_inventario.insumos(id_insumo) ON DELETE SET NULL,
    id_usuario_actor BIGINT,
    accion VARCHAR(40) NOT NULL,
    datos JSONB NOT NULL DEFAULT '{}'::JSONB,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_auditoria_insumos_fecha
    ON esquema_inventario.auditoria_insumos(fecha_creacion DESC);
