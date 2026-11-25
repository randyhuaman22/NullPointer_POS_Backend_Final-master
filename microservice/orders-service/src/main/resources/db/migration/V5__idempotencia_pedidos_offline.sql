-- Soporte de sincronización offline idempotente para pedidos creados sin conexión
ALTER TABLE esquema_comandas.pedidos
    ADD COLUMN IF NOT EXISTS llave_idempotencia VARCHAR(100),
    ADD COLUMN IF NOT EXISTS fecha_registro_offline TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS ux_pedidos_llave_idempotencia
    ON esquema_comandas.pedidos(llave_idempotencia)
    WHERE llave_idempotencia IS NOT NULL;
