-- Unión de mesas para una misma familia/atención. Las mesas se liberan al finalizar el pedido del grupo.
CREATE TABLE IF NOT EXISTS esquema_comandas.grupos_mesas (
 id_grupo_mesas BIGSERIAL PRIMARY KEY,
 id_mesa_principal BIGINT NOT NULL REFERENCES esquema_comandas.mesas(id_mesa),
 estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','FINALIZADO')),
 motivo VARCHAR(250), usuario_creacion BIGINT, usuario_finalizacion BIGINT,
 fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(), fecha_finalizacion TIMESTAMPTZ
);
CREATE TABLE IF NOT EXISTS esquema_comandas.grupos_mesas_detalle (
 id_grupo_mesas BIGINT NOT NULL REFERENCES esquema_comandas.grupos_mesas(id_grupo_mesas),
 id_mesa BIGINT NOT NULL REFERENCES esquema_comandas.mesas(id_mesa),
 PRIMARY KEY (id_grupo_mesas,id_mesa)
);
CREATE INDEX IF NOT EXISTS ix_grupos_mesas_detalle_mesa ON esquema_comandas.grupos_mesas_detalle(id_mesa);
ALTER TABLE esquema_comandas.pedidos ADD COLUMN IF NOT EXISTS id_grupo_mesas BIGINT REFERENCES esquema_comandas.grupos_mesas(id_grupo_mesas);
CREATE INDEX IF NOT EXISTS ix_pedidos_grupo_mesas ON esquema_comandas.pedidos(id_grupo_mesas);
CREATE OR REPLACE FUNCTION esquema_comandas.func_asignar_grupo_mesa_pedido() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE id_grupo BIGINT; id_principal BIGINT;
BEGIN
 IF TG_OP = 'UPDATE' AND OLD.id_grupo_mesas IS NOT NULL AND NEW.id_mesa IS DISTINCT FROM OLD.id_mesa THEN
   RAISE EXCEPTION 'El pedido pertenece a una atención con mesas unidas; no se puede trasladar por separado.';
 END IF;
 SELECT g.id_grupo_mesas, g.id_mesa_principal INTO id_grupo, id_principal
 FROM esquema_comandas.grupos_mesas_detalle d JOIN esquema_comandas.grupos_mesas g ON g.id_grupo_mesas=d.id_grupo_mesas
 WHERE d.id_mesa=NEW.id_mesa AND g.estado='ACTIVO';
 IF id_grupo IS NOT NULL THEN
   IF NEW.id_mesa <> id_principal THEN RAISE EXCEPTION 'Para una atención con mesas unidas, registra el pedido desde la mesa principal %', id_principal; END IF;
   NEW.id_grupo_mesas := id_grupo;
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_asignar_grupo_mesa_pedido ON esquema_comandas.pedidos;
CREATE TRIGGER trg_asignar_grupo_mesa_pedido BEFORE INSERT OR UPDATE OF id_mesa ON esquema_comandas.pedidos
 FOR EACH ROW EXECUTE FUNCTION esquema_comandas.func_asignar_grupo_mesa_pedido();
CREATE OR REPLACE FUNCTION esquema_comandas.func_liberar_grupo_mesas_finalizado() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE id_grupo BIGINT;
BEGIN
 id_grupo := NEW.id_grupo_mesas;
 IF id_grupo IS NOT NULL AND NEW.estado IN ('PAGADO','ANULADO') AND NOT EXISTS (
   SELECT 1 FROM esquema_comandas.pedidos p WHERE p.id_grupo_mesas=id_grupo AND p.estado NOT IN ('PAGADO','ANULADO')
 ) THEN
   UPDATE esquema_comandas.grupos_mesas SET estado='FINALIZADO', fecha_finalizacion=NOW() WHERE id_grupo_mesas=id_grupo AND estado='ACTIVO';
   UPDATE esquema_comandas.mesas SET estado='LIBRE' WHERE id_mesa IN (SELECT id_mesa FROM esquema_comandas.grupos_mesas_detalle WHERE id_grupo_mesas=id_grupo)
     AND NOT EXISTS (SELECT 1 FROM esquema_comandas.pedidos p WHERE p.id_mesa=esquema_comandas.mesas.id_mesa AND p.estado NOT IN ('PAGADO','ANULADO'));
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_liberar_grupo_mesas_finalizado ON esquema_comandas.pedidos;
CREATE TRIGGER trg_liberar_grupo_mesas_finalizado AFTER UPDATE OF estado ON esquema_comandas.pedidos
 FOR EACH ROW EXECUTE FUNCTION esquema_comandas.func_liberar_grupo_mesas_finalizado();
