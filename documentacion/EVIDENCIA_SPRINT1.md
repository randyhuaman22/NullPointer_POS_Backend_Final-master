# Lista de verificación de evidencias — Sprint 1

Marca una casilla únicamente cuando hayas ejecutado la prueba y guardado su captura o resultado.

## Arranque e infraestructura
- [ ] `docker compose build microservice/authentication-service`, `microservice/orders-service`, `microservice/kitchen-service` y `infrastructure/api-gateway` terminan sin errores (ejecutarlos uno por uno).
- [ ] `docker compose up -d` inicia los contenedores.
- [ ] `docker compose ps -a` muestra PostgreSQL, RabbitMQ y los cuatro microservicios activos.
- [ ] `GET /api/v1/salud` devuelve HTTP 200 a través de la puerta de enlace.
- [ ] Los registros muestran que Flyway creó `esquema_autenticacion` y `esquema_comandas`.

## Historias de usuario
- [ ] HU-01: login correcto con `MOZ001` y clave `1234`; PIN incorrecto devuelve 401.
- [ ] HU-02: `GET /api/v1/mesas` muestra mesas libres y ocupadas.
- [ ] HU-03: categorías y productos se pueden consultar.
- [ ] HU-04: cada detalle guarda sus propias observaciones, por ejemplo `Sin crema`.
- [ ] HU-05: cambiar cantidades y retirar un plato antes de enviar actualiza el pedido.
- [ ] HU-06: trasladar pedido a mesa libre cambia el estado de ambas mesas.
- [ ] HU-07: enviar pedido cambia el estado a `ENVIADO_COCINA`, crea evento outbox y el servicio de cocina lo recibe por RabbitMQ/WebSocket.
- [ ] HU-26: administrador crea, lista, desactiva y reactiva una cuenta.

## Seguridad
- [ ] Una petición protegida sin token devuelve 401.
- [ ] Un token incorrecto o vencido devuelve 401.
- [ ] Un mozo que intenta consultar pedidos de cocina recibe 403.
- [ ] El cocinero puede consultar `/api/v1/cocina/pedidos`.
- [ ] Un usuario que no es administrador recibe 403 en administración de usuarios.

## Pruebas adicionales
- [ ] Ejecutar `mvn test` desde la raíz, con Maven y Java 21 instalados.
- [ ] Adjuntar capturas de Android/KMP y frontend web cuando esas aplicaciones estén integradas.
- [ ] Conservar capturas de `docker compose ps -a`, logs de inicio y resultados Postman.

**Nota:** el ZIP entrega el backend. La aplicación KMP y la interfaz web administrativa no están incluidas. Las casillas de arriba no se consideran completadas por el mero hecho de que exista el código.
