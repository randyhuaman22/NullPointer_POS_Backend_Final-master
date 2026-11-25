# Recuperar Flyway y los contenedores sin borrar datos

Los registros compartidos muestran dos problemas distintos: Docker no pudo crear `servidor-descubrimiento` porque ya existe un contenedor con ese nombre; además, Flyway detecta checksums diferentes en la migración 6 de `bd_menu` y la migración 4 de `bd_comandas`.

**No escribas sentencias SQL directamente en la consola normal de PowerShell.** Usa los comandos `docker compose exec ... psql -c "..."` de esta guía. No uses `docker compose down -v` ni elimines volúmenes.

## 1. Elimina solamente los contenedores antiguos de aplicación que chocan por nombre

Desde la carpeta que contiene `docker-compose.yml`, ejecuta en PowerShell:

```powershell
$apps = @('servidor-descubrimiento','servidor-configuracion','servicio-autenticacion','servicio-productos','servicio-pedidos','servicio-cocina','servicio-caja','servicio-inventario','puerta-enlace-api')
foreach ($app in $apps) { docker rm -f $app 2>$null }
```

Esto no elimina `postgres`, `rabbitmq` ni los volúmenes de datos. El archivo Compose declara el nombre de proyecto anterior para volver a usar la red y los volúmenes existentes sin advertencias por cambio de carpeta.

## 2. Inicia solamente PostgreSQL y RabbitMQ, y crea una copia de seguridad

```powershell
docker compose up -d postgres rabbitmq
docker compose ps -a
docker compose exec -T postgres pg_dumpall -U nullpointer > respaldo_nullpointer_antes_flyway.sql
```

Comprueba que `respaldo_nullpointer_antes_flyway.sql` no esté vacío antes de continuar.

## 3. Corrige los checksums reportados por Flyway

Ejecuta cada línea completa en PowerShell. No pegues el SQL suelto en la consola.

```powershell
docker compose exec -T postgres psql -U nullpointer -d bd_menu -c "UPDATE public.flyway_schema_history SET checksum = -1443738284 WHERE version = '6' AND success = TRUE;"
```

```powershell
docker compose exec -T postgres psql -U nullpointer -d bd_comandas -c "UPDATE public.flyway_schema_history SET checksum = -356826361 WHERE version = '4' AND success = TRUE;"
```

Cada comando debe indicar `UPDATE 1`. Si indica `UPDATE 0`, o aparece que la tabla/base no existe, detente y comparte el resultado; no improvises otros cambios.

## 4. Construye e inicia el backend

```powershell
docker compose build servicio-productos servicio-pedidos
docker compose up -d
docker compose ps -a
```

Espera un minuto y revisa:

```powershell
docker compose logs --tail=150 servicio-productos
docker compose logs --tail=150 servicio-pedidos
```

Las migraciones `V7` de productos y `V5` de pedidos se ejecutan después de que Flyway pueda validar las versiones anteriores. No uses `docker compose down -v`: elimina los volúmenes y puede borrar los datos.

## Si todavía falla

Comparte la salida completa de los dos comandos `docker compose logs`. No ejecutes más `UPDATE` si los valores de checksum o la base de datos que aparece en el log son diferentes.
