# Corrección: 401 falso en el cajero (y en inventario)

## Causa
`servicio-caja` y `servicio-inventario` no tenían `@RestControllerAdvice`. Cuando un controlador lanzaba
`ResponseStatusException` (404 "no hay turno abierto", 409, 400 de validación, etc.), Spring hacía un
despacho interno a `/error`. Ese despacho pasaba otra vez por Spring Security, sin el JWT en contexto
(sesión STATELESS), y respondía **401 "Debes iniciar sesión"** ocultando el error real.
Por eso el login funcionaba y el token era válido, pero cualquier actividad con error parecía "sin sesión".

## Cambios
- Nuevo `exception/ManejadorExcepcionesGlobales` y `ErrorApi` en caja e inventario (mismo formato que auth/pedidos/productos).
- `ConfiguracionSeguridad` de caja, inventario, pedidos, autenticación y productos: se permiten los despachos `ERROR`/`FORWARD` y `/error`.
- `ClientePedidos`: 404 → 404, 401/403 → 403, pedidos caído → 503 (antes todo era 400).

## Cómo probar
1. `docker compose build servicio-caja servicio-inventario` y `docker compose up -d`.
2. Postman: Cajero > Acceso > iniciar sesión (CAJ001 / 1234).
3. `GET /api/v1/caja/turno-abierto` sin turno → ahora 404 con mensaje claro (antes 401).
4. `POST /api/v1/caja/apertura` → 201; repetir → 409; luego `turno-abierto` → 200.

# Corrección 2: 500 al cerrar caja (`/api/v1/caja/cierre`)

- Causa: `RepositorioCaja.totalEfectivo` usaba `SUM(importe)` con `partes_pago` JOIN `pagos`; ambas tablas tienen
  columna `importe`, por lo que PostgreSQL fallaba con "column reference importe is ambiguous". Ahora es `SUM(pp.importe)`.
- Los manejadores globales ahora registran el stack trace de los errores 500 (`docker compose logs servicio-caja`).
