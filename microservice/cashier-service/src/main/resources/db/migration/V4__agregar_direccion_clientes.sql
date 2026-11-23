-- Agrega almacenamiento de dirección fiscal/referencial en caché local de clientes
ALTER TABLE esquema_caja.clientes
    ADD COLUMN IF NOT EXISTS direccion VARCHAR(250);
