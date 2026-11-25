# Cambios aplicados sobre el backend proporcionado por el usuario

## Administración de usuarios
- `POST /api/v1/administracion/usuarios`: crear trabajador.
- `GET /api/v1/administracion/usuarios`: listar.
- `GET /api/v1/administracion/usuarios/{id}`: consultar.
- `PUT /api/v1/administracion/usuarios/{id}`: modificar nombre, correo, rol y opcionalmente clave.
- `DELETE /api/v1/administracion/usuarios/{id}`: baja lógica; no borra ventas ni auditoría.
- `PATCH /api/v1/administracion/usuarios/{id}/activar` y `/desactivar`: habilitar/deshabilitar.
- `GET /api/v1/administracion/usuarios/auditoria`: historial.

## Categorías y productos
El backend ya contenía el CRUD de categorías; ahora Postman expone el flujo completo. Las operaciones `DELETE` son desactivación lógica, no borrado físico. `PATCH /restaurar` vuelve a habilitar una categoría o producto. La categoría no se puede desactivar mientras tenga productos activos o subcategorías activas; primero hay que desactivar o reasignar esos elementos. Se conservan productos, ventas y auditoría históricos.

Rutas de categorías:
- `GET /api/v1/menu/administracion/categorias?incluirInactivas=true`
- `POST /api/v1/menu/administracion/categorias`
- `PUT /api/v1/menu/administracion/categorias/{idCategoria}`
- `DELETE /api/v1/menu/administracion/categorias/{idCategoria}`
- `PATCH /api/v1/menu/administracion/categorias/{idCategoria}/restaurar`

Rutas equivalentes para productos bajo `/api/v1/menu/administracion/productos`.

## Menú inicial
Se añadió una migración idempotente con categorías y productos iniciales para Pollería, Menú, Heladería y Juguería. Incluye pollo entero, 1/2, 1/4 y 1/8; combos familiares; broaster, salchipollo/salchibroaster, mostrito, alitas; caldos; chifa; criollos; hamburguesas y sánguches; helados, waffles; jugos, batidos, milkshakes, frappes, frozen, café, frutas y yogurt. Los precios son de demostración y se deben revisar antes de vender. La migración no reemplaza productos existentes: agrega solo códigos de producto que no estén presentes.

## Unión de mesas
- `POST /api/v1/mesas/grupos`: body `{"idsMesas":[1,2],"idMesaPrincipal":1}`.
- `GET /api/v1/mesas/grupos/{idGrupo}`: consulta grupo y mesas asociadas.
- `DELETE /api/v1/mesas/grupos/{idGrupo}`: deshace grupo si todavía no hay pedido activo en la mesa principal.

Solo se pueden unir mesas libres y activas. Todas quedan reservadas/ocupadas; el pedido se crea en la mesa principal. Cuando el pedido pasa a `PAGADO` o `ANULADO`, una función de base de datos cierra el grupo y libera las mesas que no tengan otro pedido activo.

## Docker
- No hay `INICIAR_BACKEND.bat` ni contenedor `db-init`.
- Contenedores e imágenes usan nombres explícitos de servicio: `postgres`, `rabbitmq`, `servicio-autenticacion`, `servicio-productos`, `servicio-pedidos`, `servicio-cocina`, `servicio-caja`, `servicio-inventario`, `servidor-configuracion`, `servidor-descubrimiento`, `puerta-enlace-api`.
- Comandos: `docker compose build`, `docker compose up -d`, `docker compose ps -a`.
- Se mantienen los nombres de volúmenes usados por el ZIP de origen para evitar desconectar los datos ya existentes.

## Validación pendiente
Se validaron sintácticamente YAML y JSON de Postman. En este entorno no se pudo ejecutar Docker integrado ni completar `mvnw`; por tanto, esta revisión no debe declararse probada al 100% hasta ejecutar la compilación, migraciones y flujos desde la PC del usuario.
