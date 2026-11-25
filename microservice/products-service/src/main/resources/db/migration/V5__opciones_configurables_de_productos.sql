-- Permite repetir una categoría (por ejemplo, "Bebidas") en áreas distintas.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'categorias_productos_nombre_key'
          AND conrelid = 'esquema_menu.categorias_productos'::regclass
    ) THEN
        ALTER TABLE esquema_menu.categorias_productos
            DROP CONSTRAINT categorias_productos_nombre_key;
    END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS ux_categoria_area_nombre
    ON esquema_menu.categorias_productos(id_area_negocio, LOWER(nombre));

-- Opciones configurables para tamaños, sabores, presentaciones y agregados.
CREATE TABLE IF NOT EXISTS esquema_menu.grupos_opciones_producto (
    id_grupo_opcion BIGSERIAL PRIMARY KEY,
    id_producto BIGINT NOT NULL REFERENCES esquema_menu.productos(id_producto) ON DELETE CASCADE,
    nombre VARCHAR(80) NOT NULL,
    obligatorio BOOLEAN NOT NULL DEFAULT FALSE,
    seleccion_minima SMALLINT NOT NULL DEFAULT 0 CHECK (seleccion_minima BETWEEN 0 AND 20),
    seleccion_maxima SMALLINT NOT NULL DEFAULT 1 CHECK (seleccion_maxima BETWEEN 1 AND 20),
    orden_visualizacion INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (seleccion_minima <= seleccion_maxima),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_grupos_opciones_producto_nombre_activo
    ON esquema_menu.grupos_opciones_producto(id_producto, LOWER(nombre)) WHERE activo = TRUE;
CREATE INDEX IF NOT EXISTS ix_grupos_opciones_producto_activo
    ON esquema_menu.grupos_opciones_producto(id_producto, activo, orden_visualizacion);

CREATE TABLE IF NOT EXISTS esquema_menu.opciones_producto (
    id_opcion BIGSERIAL PRIMARY KEY,
    id_grupo_opcion BIGINT NOT NULL REFERENCES esquema_menu.grupos_opciones_producto(id_grupo_opcion) ON DELETE CASCADE,
    nombre VARCHAR(80) NOT NULL,
    precio_adicional NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (precio_adicional >= 0),
    orden_visualizacion INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_opciones_grupo_nombre_activo
    ON esquema_menu.opciones_producto(id_grupo_opcion, LOWER(nombre)) WHERE activo = TRUE;
CREATE INDEX IF NOT EXISTS ix_opciones_grupo_activo
    ON esquema_menu.opciones_producto(id_grupo_opcion, activo, orden_visualizacion);
