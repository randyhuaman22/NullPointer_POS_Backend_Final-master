-- HU-11/T-20: la estación ya existe en el catálogo; se agrega auditoría del cambio de disponibilidad (HU-12/T-25..T-28).
CREATE TABLE IF NOT EXISTS esquema_menu.auditoria_disponibilidad (
  id_auditoria BIGSERIAL PRIMARY KEY,
  id_producto BIGINT NOT NULL,
  disponible_anterior BOOLEAN NOT NULL,
  disponible_nuevo BOOLEAN NOT NULL,
  motivo VARCHAR(250),
  usuario_actor VARCHAR(30),
  fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_auditoria_disponibilidad_producto_fecha
  ON esquema_menu.auditoria_disponibilidad(id_producto, fecha_creacion DESC);
