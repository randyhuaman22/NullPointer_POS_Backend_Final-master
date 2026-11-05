CREATE SCHEMA IF NOT EXISTS esquema_menu;
CREATE TABLE IF NOT EXISTS esquema_menu.estaciones_cocina (
    id_estacion_cocina BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS esquema_menu.categorias_productos (
    id_categoria BIGSERIAL PRIMARY KEY,
    id_categoria_padre BIGINT REFERENCES esquema_menu.categorias_productos(id_categoria) ON DELETE SET NULL,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(250),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    orden_visualizacion INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS esquema_menu.productos (
    id_producto BIGSERIAL PRIMARY KEY,
    id_categoria BIGINT NOT NULL REFERENCES esquema_menu.categorias_productos(id_categoria),
    id_estacion_cocina BIGINT REFERENCES esquema_menu.estaciones_cocina(id_estacion_cocina) ON DELETE SET NULL,
    codigo_producto VARCHAR(40) UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    descripcion TEXT,
    url_imagen TEXT,
    precio_unitario NUMERIC(12,2) NOT NULL CHECK(precio_unitario >= 0),
    disponible BOOLEAN NOT NULL DEFAULT TRUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_productos_categoria_activo
    ON esquema_menu.productos(id_categoria, activo, disponible);
INSERT INTO esquema_menu.estaciones_cocina(nombre)
VALUES ('COCINA'), ('PARRILLA'), ('BEBIDAS') ON CONFLICT(nombre) DO NOTHING;
INSERT INTO esquema_menu.categorias_productos(nombre, descripcion, orden_visualizacion)
VALUES ('Pollos','Pollos y platos a base de pollo',1),
       ('Bebidas','Bebidas frías y calientes',2),
       ('Guarniciones','Acompañamientos y extras',3),
       ('Combos','Combos y promociones',4)
ON CONFLICT(nombre) DO NOTHING;
