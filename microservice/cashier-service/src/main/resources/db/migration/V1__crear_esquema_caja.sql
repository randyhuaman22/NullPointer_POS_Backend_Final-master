CREATE SCHEMA IF NOT EXISTS esquema_caja;
CREATE TABLE IF NOT EXISTS esquema_caja.turnos_caja(
 id_turno BIGSERIAL PRIMARY KEY, id_cajero BIGINT NOT NULL, codigo_cajero VARCHAR(30) NOT NULL,
 monto_inicial NUMERIC(12,2) NOT NULL CHECK(monto_inicial>=0), estado VARCHAR(15) NOT NULL DEFAULT 'ABIERTO' CHECK(estado IN('ABIERTO','CERRADO')),
 fecha_apertura TIMESTAMPTZ NOT NULL DEFAULT NOW(), fecha_cierre TIMESTAMPTZ, monto_esperado NUMERIC(12,2), monto_real NUMERIC(12,2), diferencia NUMERIC(12,2), motivo_cierre VARCHAR(300));
CREATE UNIQUE INDEX IF NOT EXISTS ux_caja_un_turno_abierto_por_cajero ON esquema_caja.turnos_caja(id_cajero) WHERE estado='ABIERTO';
CREATE TABLE IF NOT EXISTS esquema_caja.pagos(
 id_pago BIGSERIAL PRIMARY KEY, id_pedido BIGINT NOT NULL, id_turno BIGINT NOT NULL REFERENCES esquema_caja.turnos_caja(id_turno),
 id_cajero BIGINT NOT NULL, codigo_cajero VARCHAR(30) NOT NULL, metodo VARCHAR(15) NOT NULL CHECK(metodo IN('EFECTIVO','YAPE','PLIN','MIXTO')),
 importe NUMERIC(12,2) NOT NULL CHECK(importe>0), importe_recibido NUMERIC(12,2), vuelto NUMERIC(12,2) NOT NULL DEFAULT 0,
 numero_operacion VARCHAR(100), ultimos_digitos VARCHAR(4), idempotencia VARCHAR(100) NOT NULL UNIQUE,
 estado VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO', fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE UNIQUE INDEX IF NOT EXISTS ux_caja_operacion_digital ON esquema_caja.pagos(numero_operacion) WHERE numero_operacion IS NOT NULL;
CREATE TABLE IF NOT EXISTS esquema_caja.referencias_operacion_digital( numero_operacion VARCHAR(100) PRIMARY KEY, id_pago BIGINT NOT NULL REFERENCES esquema_caja.pagos(id_pago), fecha_registro TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE TABLE IF NOT EXISTS esquema_caja.partes_pago(
 id_parte BIGSERIAL PRIMARY KEY, id_pago BIGINT NOT NULL REFERENCES esquema_caja.pagos(id_pago) ON DELETE CASCADE,
 metodo VARCHAR(15) NOT NULL CHECK(metodo IN('EFECTIVO','YAPE','PLIN')), importe NUMERIC(12,2) NOT NULL CHECK(importe>0),
 numero_operacion VARCHAR(100), ultimos_digitos VARCHAR(4));
CREATE UNIQUE INDEX IF NOT EXISTS ux_partes_operacion_digital ON esquema_caja.partes_pago(numero_operacion) WHERE numero_operacion IS NOT NULL;
CREATE TABLE IF NOT EXISTS esquema_caja.outbox_eventos(
 id_evento BIGSERIAL PRIMARY KEY, tipo_evento VARCHAR(60) NOT NULL, contenido_evento JSONB NOT NULL, fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(), fecha_publicacion TIMESTAMPTZ);
CREATE TABLE IF NOT EXISTS esquema_caja.clientes(
 id_cliente BIGSERIAL PRIMARY KEY, tipo_documento VARCHAR(3) NOT NULL CHECK(tipo_documento IN('DNI','RUC')), numero_documento VARCHAR(11) NOT NULL,
 nombre_completo VARCHAR(160) NOT NULL, telefono VARCHAR(20), correo VARCHAR(160), fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE(tipo_documento,numero_documento),
 CHECK((tipo_documento='DNI' AND numero_documento ~ '^[0-9]{8}$') OR (tipo_documento='RUC' AND numero_documento ~ '^[0-9]{11}$')));
CREATE TABLE IF NOT EXISTS esquema_caja.tickets_whatsapp(
 id_ticket BIGSERIAL PRIMARY KEY,id_pedido BIGINT NOT NULL,telefono VARCHAR(20) NOT NULL,contenido TEXT NOT NULL,
 estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_CONFIGURACION', fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(), fecha_envio TIMESTAMPTZ, respuesta_proveedor TEXT);
CREATE INDEX IF NOT EXISTS ix_pagos_fecha ON esquema_caja.pagos(fecha_creacion);
CREATE INDEX IF NOT EXISTS ix_pagos_pedido ON esquema_caja.pagos(id_pedido);
