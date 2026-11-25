# NullPointer POS — corrección de migraciones REV3

## Error que se corrigió
- `servicio-productos`: se conserva la migración V6 exactamente como en la versión que ya tenía la base de datos (checksum Flyway `1105442694`). El catálogo ampliado de 80 registros pasa a una nueva V7.
- `servicio-pedidos`: se restaura la migración V4 histórica exacta y se eliminan las dos V4 duplicadas. El Java usa `grupos_mesas_detalle`, `usuario_creacion` y `fecha_finalizacion`, que corresponden al esquema existente.
- El seed V7 omite productos cuyo nombre ya exista en la misma área, y también ignora códigos duplicados.
- Compose recupera el nombre de proyecto anterior para reutilizar los volúmenes nombrados de PostgreSQL y RabbitMQ y evitar el aviso por proyecto distinto.

## Actualizar sin perder datos
1. En la carpeta donde está ejecutándose el backend actual, ejecuta `docker compose down` (sin `-v`).
2. Extrae este ZIP en una carpeta limpia.
3. Abre PowerShell en la carpeta que contiene `pom.xml` y `docker-compose.yml`.
4. Ejecuta `docker compose config`.
5. Ejecuta `$env:COMPOSE_PARALLEL_LIMIT=1`.
6. Ejecuta `docker compose build`.
7. Ejecuta `docker compose up -d`.
8. Comprueba `docker compose ps -a` y luego `docker compose logs --tail=100 servicio-productos servicio-pedidos`.

**No ejecutes `docker compose down -v` ni borres volúmenes.** El volumen PostgreSQL conserva los datos existentes.

## Validación realizada al paquete
- YAML de Compose válido: 11 servicios.
- Flyway: no hay versiones duplicadas en las migraciones de pedidos/productos.
- Checksum V6 de productos coincide con el valor ya aplicado.
- Catálogo V7 contiene 80 filas de semilla.
- Colección Postman válida: 85 solicitudes.
- ZIP comprobado íntegro.

No se pudo ejecutar una compilación Maven ni encender Docker desde el entorno de empaquetado. La validación final de arranque debe hacerse en el equipo que tiene Docker Desktop, revisando los logs indicados arriba.
