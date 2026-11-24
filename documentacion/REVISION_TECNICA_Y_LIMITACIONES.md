# Revisión técnica — NullPointer POS / Wendy's Food

## Alcance revisado

Se recibió un ZIP de referencia de backend y un Excel de historias/tareas. Sobre la estructura original se consolidaron los módulos de pedidos, autenticación, productos y cocina; se añadieron módulos independientes de caja e inventario, junto con sus configuraciones Docker, rutas del gateway, migraciones y documentación. Este documento registra comprobaciones estáticas; no sustituye pruebas de ejecución.

## Arquitectura declarada

| Componente | Puerto | Responsabilidad | Persistencia |
|---|---:|---|---|
| `api-gateway` | 8080 | Entrada HTTP, rutas públicas y validación JWT | No aplica |
| `authentication-service` | 8081 | Login, JWT, usuarios y roles | `bd_autenticacion` |
| `orders-service` | 8082 | Mesas, comandas, detalles, traslados, pagos confirmados, descuentos, auditoría y reportes | `bd_comandas` |
| `kitchen-service` | 8083 | Consume eventos de cocina y los retransmite por WebSocket/STOMP | Sin BD de negocio |
| `products-service` | 8084 | Categorías, productos, estación y disponibilidad | `bd_menu` |
| `cashier-service` | 8085 | Turnos, cobros en efectivo/digital/mixto, clientes y tickets | `bd_caja` |
| `inventory-service` | 8086 | Insumos, recetas, compras, mermas y alertas | `bd_inventario` |
| `config-server` | 8888 | Configuración centralizada | No aplica |
| `eureka-server` | 8761 | Registro/descubrimiento de servicios | No aplica |
| PostgreSQL | 5432 | Motor relacional | Una base por servicio dueño de datos |
| RabbitMQ | 5672 / 15672 | Mensajería asíncrona y consola | Volumen propio |

## Capas del backend

Los microservicios de negocio separan responsabilidades por paquetes: `controller`, `dto/request`, `dto/response`, `entity` (cuando corresponde), `exception`, `mapper`, `repository`, `security`, `service`, `service/impl` y `messaging` (donde aplica). Los clientes deben entrar por API Gateway en `localhost:8080`; no se comparten tablas entre servicios ni se debe acceder a la base de datos de otro servicio.

## Flujo distribuido principal

1. `orders-service` guarda la comanda y el evento outbox en su propia transacción.
2. El publicador envía el evento a RabbitMQ.
3. `kitchen-service` valida el contrato y difunde el mensaje al KDS mediante STOMP/WebSocket.
4. `cashier-service` persiste el cobro en su base y publica la confirmación usando outbox.
5. `orders-service` procesa la confirmación y marca la comanda como pagada.
6. `inventory-service` consume `pedido.pagado`, deduplica por identificador de evento y actualiza el stock según las recetas configuradas.

## Comprobaciones estáticas realizadas

- Se revisaron los POM, las rutas del gateway, las migraciones, los scripts de bases, la estructura del proyecto y los comentarios `START/END` que identifican bloques de HU/tareas.
- Se validaron sintácticamente los archivos XML de Maven, YAML/YML y JSON, así como la integridad del ZIP final.
- Se verificó que los servicios declarados en Docker Compose tengan sus Dockerfiles referenciados en la estructura del proyecto.
- Se hizo un análisis de sintaxis de Java sin classpath de dependencias. La ausencia de errores de parser no equivale a una compilación.
- Se escribieron pruebas unitarias para JWT, umbrales de demora, contrato de eventos, aritmética de caja y cálculo de recetas; no se pudieron ejecutar en este entorno.

## No verificado en ejecución

El entorno de preparación no pudo descargar Maven y no tenía un daemon Docker operativo. Por eso no se afirma que `mvn clean verify`, `docker compose build/up`, migraciones en PostgreSQL, publicación/consumo real de RabbitMQ, la suscripción WebSocket o los flujos end-to-end hayan terminado correctamente.

Prueba mínima que debe ejecutarse en el equipo del proyecto:

```powershell
Copy-Item .env.example .env
docker compose config
docker compose up --build -d
docker compose ps -a
.\mvnw.cmd -B -ntp clean verify
```

Luego realizar el recorrido login → crear comanda → enviar a cocina → marcar detalle listo → abrir caja → cobrar → comprobar pedido pagado → comprobar descuento de insumos. Revisar errores con `docker compose logs -f puerta-enlace-api servicio-autenticacion servicio-pedidos servicio-productos servicio-cocina servicio-caja servicio-inventario`.

## Limitaciones funcionales conocidas

- El ZIP es backend: no incluye `shared/` ni `androidApp/`, de modo que las tareas UI, ViewModel, temporizadores visuales, notificaciones sonoras y refresco reactivo de cliente no se pueden dar por terminadas aquí.
- HU-17 incluye descuento porcentual y registro de auditoría; no implementa todo un motor de promociones por cupón/calendario/combinación.
- HU-18 valida y consulta DNI/RUC en el registro local; no integra automáticamente RENIEC/SUNAT.
- HU-19 registra tickets y permite el envío configurable, pero requiere `WHATSAPP_API_URL` y `WHATSAPP_ACCESS_TOKEN` oficiales, que no fueron probados.
- HU-22 procesa el evento de pedido pagado con receta; si falta stock, la política actual registra el déficit y deja el saldo en cero. El negocio debe confirmar si prefiere bloquear esa venta.
- HU-23 expone alertas mediante API; no hay push ni interfaz móvil incluida.
- T-38 de Sprint 2 (integración del cajón USB/serie) no está implementada porque depende del hardware y protocolo especificados.
- Yape/Plin registra la referencia declarada y evita referencias repetidas, pero no verifica automáticamente el movimiento con la entidad financiera.

El estado detallado por HU/tarea y las rutas concretas están en `GUIA_TAREAS_SPRINTS_1_2_3_BACKEND.docx` y `INDICE_TAREAS_EN_CODIGO.md`.

## Actualización de la revisión del 18/11/2026

- Se corrigió una importación faltante de `java.util.Map` en `PedidoRepositorio.java`. Esa clase usa `Map` en las consultas de reportes y el tipo no estaba importado; esto puede explicar el error que el IDE mostraba en Repository y las referencias descendientes en ServiceImpl/Controller.
- Se comprobó estáticamente que las llamadas de `ServicioPedidosImpl` a `PedidoRepositorio` y `MesaRepositorio` tienen métodos coincidentes; `ControladorOperacionesPedido` llama métodos declarados en `ServicioPedidos`.
- Se corrigió el comando Maven mostrado en el README y se alineó la etiqueta de tareas Sprint 3 del procesamiento de inventario con el backlog propuesto.
- Las verificaciones Java continúan siendo análisis de sintaxis sin dependencias. La compilación Maven y los flujos con PostgreSQL/RabbitMQ/Docker siguen pendientes de ejecución local.
