#!/bin/sh
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<'SQL'
SELECT 'CREATE DATABASE bd_autenticacion' WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'bd_autenticacion')\gexec
SELECT 'CREATE DATABASE bd_comandas' WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'bd_comandas')\gexec
SELECT 'CREATE DATABASE bd_menu' WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'bd_menu')\gexec
SELECT 'CREATE DATABASE bd_cocina' WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'bd_cocina')\gexec
SELECT 'CREATE DATABASE bd_caja' WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'bd_caja')\gexec
SELECT 'CREATE DATABASE bd_inventario' WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'bd_inventario')\gexec
SQL
