-- [ADMINISTRACION CATALOGO] Las cuatro areas son agrupadores de negocio, no microservicios separados.
CREATE TABLE IF NOT EXISTS esquema_menu.areas_negocio (
    id_area_negocio BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(250),
    orden_visualizacion INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO esquema_menu.areas_negocio(codigo, nombre, descripcion, orden_visualizacion)
VALUES
 ('POLLERIA', 'Pollería', 'Pollos a la brasa, parrillas, guarniciones y combos', 1),
 ('MENU', 'Menú', 'Menú del día, entradas, sopas y segundos', 2),
 ('HELADERIA', 'Heladería', 'Helados, conos, vasos y complementos', 3),
 ('JUGUERIA', 'Juguería', 'Jugos, extractos, batidos y bebidas', 4)
ON CONFLICT (codigo) DO NOTHING;

ALTER TABLE esquema_menu.categorias_productos
    ADD COLUMN IF NOT EXISTS id_area_negocio BIGINT;

UPDATE esquema_menu.categorias_productos c
SET id_area_negocio = a.id_area_negocio
FROM esquema_menu.areas_negocio a
WHERE c.id_area_negocio IS NULL
  AND a.codigo = CASE
      WHEN LOWER(c.nombre) IN ('bebidas', 'jugos', 'juguería', 'jugueria', 'extractos') THEN 'JUGUERIA'
      WHEN LOWER(c.nombre) IN ('menú', 'menu', 'menú del día', 'menu del dia', 'entradas', 'sopas', 'segundos') THEN 'MENU'
      WHEN LOWER(c.nombre) IN ('helados', 'heladería', 'heladeria', 'conos y vasos', 'toppings') THEN 'HELADERIA'
      ELSE 'POLLERIA'
  END;

ALTER TABLE esquema_menu.categorias_productos
    ALTER COLUMN id_area_negocio SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_categoria_area_negocio'
          AND conrelid = 'esquema_menu.categorias_productos'::regclass
    ) THEN
        ALTER TABLE esquema_menu.categorias_productos
            ADD CONSTRAINT fk_categoria_area_negocio
            FOREIGN KEY (id_area_negocio) REFERENCES esquema_menu.areas_negocio(id_area_negocio);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_categorias_area_activo
    ON esquema_menu.categorias_productos(id_area_negocio, activo, orden_visualizacion);

ALTER TABLE esquema_menu.productos
    ADD COLUMN IF NOT EXISTS tiempo_preparacion_minutos INTEGER NOT NULL DEFAULT 0 CHECK (tiempo_preparacion_minutos BETWEEN 0 AND 240),
    ADD COLUMN IF NOT EXISTS es_destacado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS orden_visualizacion INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS ix_productos_activo_destacado
    ON esquema_menu.productos(activo, es_destacado, orden_visualizacion);

INSERT INTO esquema_menu.estaciones_cocina(nombre)
VALUES ('MENU'), ('HELADERIA'), ('JUGUERIA')
ON CONFLICT (nombre) DO NOTHING;

-- Categorias iniciales sugeridas. El administrador puede modificarlas o crear otras desde la API.
INSERT INTO esquema_menu.categorias_productos(id_area_negocio, nombre, descripcion, orden_visualizacion)
SELECT a.id_area_negocio, x.nombre, x.descripcion, x.orden
FROM (VALUES
 ('MENU', 'Menú del día', 'Platos del menú diario', 1),
 ('MENU', 'Entradas', 'Entradas y acompañamientos del menú', 2),
 ('MENU', 'Sopas', 'Sopas del día', 3),
 ('MENU', 'Segundos', 'Platos de fondo del menú', 4),
 ('HELADERIA', 'Helados', 'Helados por sabor y tamaño', 1),
 ('HELADERIA', 'Conos y vasos', 'Presentaciones de helado', 2),
 ('HELADERIA', 'Toppings', 'Complementos y agregados', 3),
 ('JUGUERIA', 'Jugos', 'Jugos naturales y combinados', 1),
 ('JUGUERIA', 'Extractos', 'Extractos de frutas y verduras', 2),
 ('JUGUERIA', 'Batidos', 'Batidos y bebidas especiales', 3)
) AS x(area_codigo, nombre, descripcion, orden)
JOIN esquema_menu.areas_negocio a ON a.codigo = x.area_codigo
WHERE NOT EXISTS (
    SELECT 1 FROM esquema_menu.categorias_productos c WHERE LOWER(c.nombre) = LOWER(x.nombre)
);

CREATE TABLE IF NOT EXISTS esquema_menu.auditoria_catalogo (
    id_auditoria BIGSERIAL PRIMARY KEY,
    tipo_entidad VARCHAR(30) NOT NULL,
    id_entidad BIGINT NOT NULL,
    accion VARCHAR(30) NOT NULL,
    usuario_actor VARCHAR(80) NOT NULL,
    datos JSONB,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_auditoria_catalogo_entidad_fecha
    ON esquema_menu.auditoria_catalogo(tipo_entidad, id_entidad, fecha_creacion DESC);
CREATE INDEX IF NOT EXISTS ix_auditoria_catalogo_fecha
    ON esquema_menu.auditoria_catalogo(fecha_creacion DESC);
