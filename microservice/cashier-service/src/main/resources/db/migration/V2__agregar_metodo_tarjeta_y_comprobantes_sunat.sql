-- ========================================================
-- V2: Soporte para método TARJETA y Emisión Tributaria SUNAT
-- ========================================================

-- 1. Actualizar validación de métodos de pago en pagos y partes de pago
ALTER TABLE esquema_caja.pagos DROP CONSTRAINT IF EXISTS pagos_metodo_check;
ALTER TABLE esquema_caja.pagos ADD CONSTRAINT pagos_metodo_check 
    CHECK(metodo IN ('EFECTIVO', 'YAPE', 'PLIN', 'TARJETA', 'MIXTO'));

ALTER TABLE esquema_caja.partes_pago DROP CONSTRAINT IF EXISTS partes_pago_metodo_check;
ALTER TABLE esquema_caja.partes_pago ADD CONSTRAINT partes_pago_metodo_check 
    CHECK(metodo IN ('EFECTIVO', 'YAPE', 'PLIN', 'TARJETA'));

-- 2. Tabla para registrar los comprobantes emitidos hacia SUNAT
CREATE TABLE IF NOT EXISTS esquema_caja.comprobantes_sunat (
    id_comprobante BIGSERIAL PRIMARY KEY,
    id_pago BIGINT REFERENCES esquema_caja.pagos(id_pago),
    id_pedido BIGINT NOT NULL,
    tipo_comprobante VARCHAR(20) NOT NULL CHECK(tipo_comprobante IN ('BOLETA', 'FACTURA')),
    serie VARCHAR(10) NOT NULL,
    correlativo BIGINT NOT NULL,
    tipo_documento_receptor VARCHAR(10),
    numero_documento_receptor VARCHAR(20),
    denominacion_receptor VARCHAR(200),
    direccion_receptor VARCHAR(250),
    moneda VARCHAR(5) NOT NULL DEFAULT 'PEN',
    total_gravado NUMERIC(12,2) NOT NULL,
    total_igv NUMERIC(12,2) NOT NULL,
    total_importe NUMERIC(12,2) NOT NULL,
    metodo_pago VARCHAR(20) NOT NULL,
    hash_cpe VARCHAR(100) NOT NULL,
    codigo_cdr VARCHAR(10) NOT NULL DEFAULT '0',
    descripcion_cdr VARCHAR(300) NOT NULL,
    estado_sunat VARCHAR(30) NOT NULL DEFAULT 'ACEPTADO',
    cadena_qr TEXT NOT NULL,
    xml_firmado TEXT NOT NULL,
    fecha_emision TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(serie, correlativo)
);

CREATE INDEX IF NOT EXISTS ix_comprobantes_pedido ON esquema_caja.comprobantes_sunat(id_pedido);
CREATE INDEX IF NOT EXISTS ix_comprobantes_pago ON esquema_caja.comprobantes_sunat(id_pago);
CREATE INDEX IF NOT EXISTS ix_comprobantes_fecha ON esquema_caja.comprobantes_sunat(fecha_emision);

-- 3. Secuencias independientes para correlativos oficiales
CREATE SEQUENCE IF NOT EXISTS esquema_caja.seq_correlativo_boleta START WITH 1;
CREATE SEQUENCE IF NOT EXISTS esquema_caja.seq_correlativo_factura START WITH 1;
