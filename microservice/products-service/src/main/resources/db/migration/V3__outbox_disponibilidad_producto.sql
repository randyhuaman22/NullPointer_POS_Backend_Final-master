-- [HU-12 / T-28 START] Outbox transaccional para no perder los cambios de disponibilidad si RabbitMQ se cae.
CREATE TABLE IF NOT EXISTS esquema_menu.eventos_disponibilidad (
    id_evento BIGSERIAL PRIMARY KEY,
    contenido_evento JSONB NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_publicacion TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_eventos_disponibilidad_pendientes
    ON esquema_menu.eventos_disponibilidad(id_evento) WHERE fecha_publicacion IS NULL;
-- [HU-12 / T-28 END]
