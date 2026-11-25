CREATE TABLE IF NOT EXISTS esquema_autenticacion.auditoria_usuarios (
    id_auditoria BIGSERIAL PRIMARY KEY,
    id_usuario_objetivo BIGINT REFERENCES esquema_autenticacion.usuarios(id_usuario) ON DELETE SET NULL,
    codigo_administrador VARCHAR(30) NOT NULL,
    accion VARCHAR(40) NOT NULL,
    datos JSONB NOT NULL DEFAULT '{}'::JSONB,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_auditoria_usuarios_fecha
    ON esquema_autenticacion.auditoria_usuarios(fecha_creacion DESC);
