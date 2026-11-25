-- Conserva los duplicados historicos para auditoria y protege nuevas emisiones.
ALTER TABLE esquema_caja.comprobantes_sunat ADD COLUMN vigente BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE esquema_caja.comprobantes_sunat SET vigente=FALSE WHERE id_comprobante IN (
 SELECT id_comprobante FROM (
  SELECT id_comprobante, ROW_NUMBER() OVER(PARTITION BY id_pago ORDER BY id_comprobante DESC) AS posicion
  FROM esquema_caja.comprobantes_sunat WHERE id_pago IS NOT NULL
 ) anteriores WHERE posicion > 1
);
CREATE UNIQUE INDEX ux_comprobante_pago_vigente ON esquema_caja.comprobantes_sunat(id_pago) WHERE vigente;
ALTER TABLE esquema_caja.comprobantes_sunat ALTER COLUMN codigo_cdr SET DEFAULT '';
ALTER TABLE esquema_caja.comprobantes_sunat ALTER COLUMN estado_sunat SET DEFAULT 'PENDIENTE_FIRMA';
-- Las emisiones anteriores sin SignatureValue carecen de confirmacion verificable.
UPDATE esquema_caja.comprobantes_sunat SET estado_sunat='PENDIENTE_FIRMA', codigo_cdr='',
 descripcion_cdr='Registro anterior sin firma digital verificable. Requiere revision.'
 WHERE estado_sunat='ACEPTADO' AND xml_firmado NOT LIKE '%SignatureValue%';
