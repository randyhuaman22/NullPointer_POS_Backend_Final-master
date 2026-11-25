# Guía técnica del backend — NullPointer POS (Sprints 1, 2 y 3)

**Universidad Peruana Unión (UPeU) · Célula G5 · Wendy’s Food**  
**Propósito:** ubicar cada HU/tarea en el código, entender la arquitectura distribuida y preparar la exposición.

> **Declaración honesta del estado:** esta entrega incluye código de backend para las tres iteraciones, configuraciones y documentación, pero no se puede certificar como “100 % funcional” hasta ejecutar `mvn clean verify` y `docker compose up --build` y validar los flujos integrados. En el entorno en que se generó este ZIP, Maven no pudo descargarse y no había un daemon Docker disponible. Los renglones marcados “parcial” o “pendiente” deben conservar ese estado hasta completarlos. El ZIP recibido no contiene `shared/` ni `androidApp/`.

## 1. Resumen ejecutivo

El backend es un proyecto Maven multimódulo, formado por servicios Spring Boot separados, cada uno con responsabilidades propias y acceso exclusivo a su base de datos. La integración entre servicios se hace por HTTP para consultas requeridas y por RabbitMQ/outbox para eventos asíncronos. El servicio de cocina difunde los eventos al KDS por WebSocket/STOMP. No es un monolito y ningún servicio debe conectarse directamente a la base de datos de otro servicio.

## 2. Arquitectura y puertos

| Componente | Puerto | Responsabilidad | Base de datos |
|---|---:|---|---|
| `api-gateway` | 8080 | Entrada HTTP, rutas y filtro JWT | — |
| `authentication-service` | 8081 | Login, JWT, usuarios y roles | `bd_autenticacion` |
| `orders-service` | 8082 | Mesas, comandas, estado de platos, descuentos, auditoría y reportes | `bd_comandas` |
| `kitchen-service` | 8083 | Consume RabbitMQ y publica WebSocket/STOMP | Sin BD de negocio |
| `products-service` | 8084 | Categorías, productos, estaciones, disponibilidad y auditoría | `bd_menu` |
| `cashier-service` | 8085 | Apertura/cierre de caja y cobros | `bd_caja` |
| `inventory-service` | 8086 | Insumos, recetas, compras, mermas y alertas de stock | `bd_inventario` |
| `config-server` | 8888 | Configuración centralizada | — |
| `eureka-server` | 8761 | Registro/descubrimiento de servicios | — |
| PostgreSQL | 5432 | Persistencia por servicio | 6 DBs separadas |
| RabbitMQ | 5672 / panel 15672 | Mensajería asíncrona | volumen Rabbit |

### Flujo A: comanda a cocina

1. El cliente llama a `POST /api/v1/pedidos` y luego a `POST /api/v1/pedidos/{id}/enviar-cocina` a través del gateway.  2. `orders-service` guarda el cambio y el evento en su outbox, dentro de su transacción.  3. El publicador de eventos pendientes envía a RabbitMQ.  4. `kitchen-service` valida el contrato, controla duplicados recientes y publica en `/topic/cocina` mediante `/ws/cocina`.  5. El cliente KDS (no incluido en este ZIP) debe suscribirse, fusionar el estado inicial y eventos, y reconectar.

### Flujo B: cobro y descuento de inventario

1. `cashier-service` consulta el resumen del pedido en `orders-service`, valida el pago y registra pago/partes/outbox en `bd_caja`.  2. RabbitMQ entrega `pago.confirmado` a `orders-service`, que actualiza su propio pedido a `PAGADO` y produce `pedido.pagado`.  3. `inventory-service` consume `pedido.pagado`, registra el ID de evento procesado y descuenta la receta de la venta sin acceder a `bd_comandas`.  4. Hay que configurar recetas y existencias antes de demostrar el descuento. La integración completa aún requiere ejecución en Docker.

### Flujo C: disponibilidad de producto

`products-service` persiste disponibilidad, auditoría y evento; su publicador envía `producto.disponibilidad.actualizada`; `kitchen-service` lo expone en `/topic/cocina/disponibilidad`. La suscripción/actualización automática del catálogo móvil está pendiente porque `shared/` no forma parte del ZIP.

## 3. Sprint 1 — mapa de tareas técnicas del Excel

El Excel describe tareas móviles y backend. Cuando el artefacto está en `androidApp/`, `shared/` o `desktopApp/`, se marca como pendiente porque este paquete solo contiene backend. Las rutas originales del Excel, basadas en una estructura Kotlin/monolítica de referencia, fueron adaptadas donde correspondía a los servicios Java actuales.

### S1-T-01 — Diseñar los módulos KMP (shared/androidApp/desktopApp).
- **Ubicación real / referencia:** `Fuera del backend: el ZIP solo entrega microservicios Java; no contiene KMP, Android ni desktop.`
- **Estado:** **PENDIENTE fuera del alcance del ZIP**
- **Para explicar:** No afirmar que la arquitectura móvil ya está integrada.

### S1-T-02 — Configurar Koin y Ktor Client en la capa compartida.
- **Ubicación real / referencia:** `Fuera del backend: no existe shared/src/commonMain/di/AppModule.kt.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** Necesita implementarse en el repositorio móvil.

### S1-T-03 — Crear esquemas de base de datos.
- **Ubicación real / referencia:** `base-de-datos/crear-bases-datos.sh; base-de-datos/inicializar-bases-datos.sh; microservice/*/src/main/resources/db/migration/`
- **Estado:** **ADAPTADO a microservicios**
- **Para explicar:** Usa bases por servicio: bd_autenticacion, bd_comandas, bd_menu, bd_caja, bd_inventario.

### S1-T-04 — Configurar pool HikariCP y variables de entorno.
- **Ubicación real / referencia:** `microservice/authentication-service/src/main/resources/application.yml; orders-service/.../application.yml; products-service/.../application.yml; cashier-service/.../application.yml; inventory-service/.../application.yml`
- **Estado:** **IMPLEMENTADO en configuración**
- **Para explicar:** Valores se inyectan por Docker Compose; probar conexión real en Docker.

### S1-T-05 — Firmar JWT.
- **Ubicación real / referencia:** `microservice/authentication-service/src/main/java/pe/upeu/nullpointerpos/autenticacion/security/ServicioJwt.java`
- **Estado:** **IMPLEMENTADO en código**
- **Para explicar:** Generación y validación JWT centralizadas en authentication-service.

### S1-T-06 — Filtro Authorization: Bearer.
- **Ubicación real / referencia:** `microservice/*-service/src/main/java/pe/upeu/nullpointerpos/*/security/FiltroAutenticacionJwt.java; infrastructure/api-gateway/src/main/java/pe/upeu/nullpointerpos/puertaenlace/security/FiltroJwtPuertaEnlace.java`
- **Estado:** **IMPLEMENTADO en código**
- **Para explicar:** Filtros repetidos en cada servicio permiten verificar tokens sin compartir base de datos.

### S1-T-07 — Pruebas BCrypt y sesiones expiradas.
- **Ubicación real / referencia:** `microservice/authentication-service/src/test/java/pe/upeu/nullpointerpos/autenticacion/ServicioJwtTest.java`
- **Estado:** **PRUEBAS ESCRITAS, NO EJECUTADAS**
- **Para explicar:** Maven no pudo descargarse en el entorno de creación.

### S1-T-08 — Pantalla de login con PIN.
- **Ubicación real / referencia:** `androidApp/src/main/kotlin/com/nullpointer/pos/ui/login/LoginScreen.kt no está en este ZIP.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** El backend valida credenciales, pero no contiene pantalla.

### S1-T-09 — AuthViewModel móvil.
- **Ubicación real / referencia:** `shared/src/commonMain/viewmodel/AuthViewModel.kt no está en este ZIP.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** No hay cliente KMP incluido.

### S1-T-10 — Endpoint login.
- **Ubicación real / referencia:** `microservice/authentication-service/src/main/java/pe/upeu/nullpointerpos/autenticacion/controller/ControladorAutenticacion.java; POST /api/v1/autenticacion/iniciar-sesion`
- **Estado:** **IMPLEMENTADO en código**
- **Para explicar:** La ruta real difiere de la ruta de ejemplo antigua /api/v1/auth/login.

### S1-T-11 — Cuadrícula de mesas.
- **Ubicación real / referencia:** `androidApp/.../ui/mesas/MesasScreen.kt no está incluido.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** El backend suministra datos, no UI.

### S1-T-12 — Consultar mesas y estados.
- **Ubicación real / referencia:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorMesas.java; MesaRepositorio.java; GET /api/v1/mesas`
- **Estado:** **IMPLEMENTADO en código**
- **Para explicar:** La vista de mesa se debe construir en el cliente.

### S1-T-13 — Renderizado verde/libre y rojo/ocupado.
- **Ubicación real / referencia:** `UI móvil ausente.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** El estado de mesa se devuelve desde orders-service.

### S1-T-14 — Clic sobre mesas libres.
- **Ubicación real / referencia:** `UI/ViewModel ausentes.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** La regla de selección pertenece al cliente.

### S1-T-15 — Pestañas de categorías.
- **Ubicación real / referencia:** `UI ausente.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** API de categorías existe.

### S1-T-16 — Consumir catálogo/caché local.
- **Ubicación real / referencia:** `La API está en products-service; el repository Kotlin compartido no está incluido.`
- **Estado:** **PARCIAL backend**
- **Para explicar:** GET /api/v1/menu/productos y /categorias; caché móvil pendiente.

### S1-T-17 — Tarjeta visual de producto.
- **Ubicación real / referencia:** `UI ausente.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** Datos de producto se sirven por products-service.

### S1-T-18 — Dialog de notas al plato.
- **Ubicación real / referencia:** `UI ausente.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** El backend permite guardar observaciones en detalles.

### S1-T-19 — Persistir nota en detalle de comanda.
- **Ubicación real / referencia:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/dto/request/SolicitudDetallePedido.java; ServicioPedidosImpl.java; PedidoRepositorio.java`
- **Estado:** **IMPLEMENTADO en código**
- **Para explicar:** La nota se asocia a la línea del pedido.

### S1-T-20 — Botones +/−/eliminar de la línea.
- **Ubicación real / referencia:** `UI ausente.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** Backend acepta actualización antes de enviar.

### S1-T-21 — Recalcular total en cliente.
- **Ubicación real / referencia:** `ViewModel Kotlin ausente.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** El backend debe recalcular y persistir total autoritativamente.

### S1-T-22 — Acción para mover mesa.
- **Ubicación real / referencia:** `UI ausente.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** Endpoint backend de traslado sí existe.

### S1-T-23 — Selector modal de mesas libres.
- **Ubicación real / referencia:** `UI ausente.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** Requiere cliente móvil.

### S1-T-24 — Endpoint para traslado de mesa.
- **Ubicación real / referencia:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorTrasladoMesas.java; ServicioMesasImpl.java; PUT /api/v1/mesas/trasladar`
- **Estado:** **IMPLEMENTADO, ruta adaptada**
- **Para explicar:** La ruta real es /trasladar, no /transferir como en la hoja original.

### S1-T-25 — Botón Enviar a Cocina.
- **Ubicación real / referencia:** `UI ausente.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** El endpoint de envío existe.

### S1-T-26 — Validar comanda no vacía en cliente.
- **Ubicación real / referencia:** `UseCase Kotlin ausente; el backend valida que haya detalles.`
- **Estado:** **PARCIAL backend**
- **Para explicar:** Validación en UI pendiente.

### S1-T-27 — DTO compartido del pedido.
- **Ubicación real / referencia:** `shared DTO Kotlin no incluido; DTO Java en microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/dto/request/SolicitudCrearPedido.java`
- **Estado:** **ADAPTADO backend**
- **Para explicar:** Modelo de solicitud Java por servicio.

### S1-T-28 — Crear pedido con transacción.
- **Ubicación real / referencia:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorPedidos.java; ServicioPedidosImpl.java; PedidoRepositorio.java`
- **Estado:** **IMPLEMENTADO en código**
- **Para explicar:** Crear y editar comandas se ejecuta en orders-service.

### S1-T-29 — Evento WebSocket de comanda.
- **Ubicación real / referencia:** `orders-service PublicadorEventosPendientes.java -> RabbitMQ -> kitchen-service ConfiguracionSocketCocina.java/ServicioEventosCocinaImpl.java`
- **Estado:** **IMPLEMENTADO como flujo distribuido**
- **Para explicar:** El emisor no escribe directo a cocina; se usa RabbitMQ y outbox.

### S1-T-30 — Actualizar estado de mesa vía WebSocket en todas las apps.
- **Ubicación real / referencia:** `Backend actualiza estado; cliente KMP/WebSocket móvil no está incluido.`
- **Estado:** **PARCIAL backend**
- **Para explicar:** Refresco de UI pendiente.

### S1-T-31 — Formulario web de administración.
- **Ubicación real / referencia:** `desktopApp UI no está incluida.`
- **Estado:** **PENDIENTE frontend**
- **Para explicar:** Existe API administrativa.

### S1-T-32 — REST de administración de usuarios.
- **Ubicación real / referencia:** `microservice/authentication-service/src/main/java/pe/upeu/nullpointerpos/autenticacion/controller/ControladorUsuarios.java; GET/POST /api/v1/administracion/usuarios; activar/desactivar`
- **Estado:** **IMPLEMENTADO en código**
- **Para explicar:** La ruta se cambió al namespace /administracion/usuarios.

### S1-T-33 — Asignar roles Mozo/Cajero/Admin.
- **Ubicación real / referencia:** `microservice/authentication-service/src/main/java/pe/upeu/nullpointerpos/autenticacion/service/impl/ServicioUsuariosImpl.java; entity/Usuario.java`
- **Estado:** **IMPLEMENTADO en código**
- **Para explicar:** Los roles se validan en backend; cuentas demo solo local.

### S1-T-34 — Pruebas de acceso por roles.
- **Ubicación real / referencia:** `Pruebas de seguridad son incompletas/no ejecutadas en el entorno. Los filtros y reglas están en los servicios.`
- **Estado:** **PARCIAL: ejecutar pruebas**
- **Para explicar:** Añadir casos de matriz de permisos y probarlos con Docker/Postman.

## 4. Sprint 1 — historias de usuario

| HU | Función | Backend y endpoint | Estado |
|---|---|---|---|
| HU-01 | Inicio de sesión | `authentication-service/.../ControladorAutenticacion.java`; `POST /api/v1/autenticacion/iniciar-sesion` | Backend implementado; cliente móvil ausente |
| HU-02 | Estado de mesas | `orders-service/.../ControladorMesas.java`; `GET /api/v1/mesas` | Backend implementado; colores/UI pendientes |
| HU-03 | Categorías/menú | `products-service/.../ControladorMenu.java`; `GET /api/v1/menu/categorias`, `/productos` | Backend implementado; catálogo visual/caché no incluidos |
| HU-04 | Notas por plato | `orders-service/.../SolicitudDetallePedido.java`, `ServicioPedidosImpl.java`, `PedidoRepositorio.java` | Backend implementado |
| HU-05 | Editar pedido antes de enviar | `orders-service/.../ControladorPedidos.java`; `PUT /api/v1/pedidos/{idPedido}/detalles` | Backend implementado; botones UI pendientes |
| HU-06 | Trasladar pedido | `orders-service/.../ControladorTrasladoMesas.java`; `PUT /api/v1/mesas/trasladar` | Backend implementado; ruta adaptada respecto al Excel |
| HU-07 | Enviar a cocina | `orders-service/.../ControladorPedidos.java`; `POST /api/v1/pedidos/{idPedido}/enviar-cocina`, outbox/RabbitMQ | Backend implementado; KDS visual pendiente |
| HU-26 | Crear/administrar cuentas | `authentication-service/.../ControladorUsuarios.java`; `/api/v1/administracion/usuarios` | Backend implementado; UI admin pendiente |

## 5. Sprint 2 — tareas T-01 a T-48

Los IDs de Sprint 2 proceden de la lista técnica proporcionada. En este sprint se separa lo implementado en backend de las pantallas y ViewModels que faltan.

### T-01 · HT-03 — Configurar WebSocket/STOMP y /topic/cocina.
- **Archivos/rutas:** `microservice/kitchen-service/src/main/java/pe/upeu/nullpointerpos/cocina/config/ConfiguracionSocketCocina.java`
- **Estado:** **IMPLEMENTADO backend; integración por probar**
- **Nota técnica:** Configura endpoint /ws/cocina, broker simple /topic y destino de cocina.

### T-02 · HT-03 — Contrato y validación del evento de pedido.
- **Archivos/rutas:** `microservice/kitchen-service/src/main/java/pe/upeu/nullpointerpos/cocina/dto/event/EventoPedidoCocina.java; .../mapper/EventoCocinaMapper.java`
- **Estado:** **IMPLEMENTADO + test escrito**
- **Nota técnica:** Valida campos requeridos y detalles antes de difundir el evento.

### T-03 · HT-03 — RabbitMQ -> servicio de difusión WebSocket.
- **Archivos/rutas:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/service/impl/PublicadorEventosPendientes.java; microservice/kitchen-service/src/main/java/pe/upeu/nullpointerpos/cocina/messaging/ReceptorEventosCocina.java; .../service/impl/ServicioEventosCocinaImpl.java`
- **Estado:** **IMPLEMENTADO backend; end-to-end no verificado**
- **Nota técnica:** Patrón outbox/publicador en orders y receptor/difusor en kitchen.

### T-04 · HT-03 — Cliente KDS WebSocket con reconexión.
- **Archivos/rutas:** `ClienteWebSocketCocina.kt y PantallaCocina.kt no están en el ZIP.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** No confundir el endpoint server-side con el cliente KDS.

### T-05 · HT-03 — Pruebas de integración: eventos, reconexión, duplicados y latencia.
- **Archivos/rutas:** `microservice/kitchen-service/src/test/java/pe/upeu/nullpointerpos/cocina/EventoCocinaMapperTest.java`
- **Estado:** **PARCIAL: tests escritos, no ejecutados**
- **Nota técnica:** Hay prueba del mapper; la matriz de integración y medición de latencia queda por ejecutar/crear.

### T-06 · HU-08 — Pantalla KDS con tarjetas.
- **Archivos/rutas:** `androidApp/src/main/kotlin/com/nullpointer/pos/ui/cocina/PantallaCocina.kt no existe en este ZIP.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** El backend ofrece datos; no pantalla.

### T-07 · HU-08 — API inicial de pedidos pendientes para KDS.
- **Archivos/rutas:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorPedidosCocina.java; ServicioPedidosImpl.java; GET /api/v1/cocina/pedidos`
- **Estado:** **IMPLEMENTADO backend**
- **Nota técnica:** Devuelve pedido, detalles y métricas de demora.

### T-08 · HU-08 — Mezclar lista inicial + WebSocket sin duplicados en ViewModel.
- **Archivos/rutas:** `shared/src/commonMain/viewmodel/CocinaViewModel.kt no está incluido.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** El servicio server-side mantiene deduplicación limitada de eventos recibidos.

### T-09 · HU-08 — Tarjetas con mesa/hora/platos/cantidades/notas/estado.
- **Archivos/rutas:** `UI TarjetaPedido.kt ausente; DTO backend en orders-service/.../dto/response/RespuestaPedidoCocina.java`
- **Estado:** **PARCIAL backend**
- **Nota técnica:** El contrato contiene la información; presentación pendiente.

### T-10 · HU-08 — Alerta sonora/visual sin duplicar tras reconexión.
- **Archivos/rutas:** `androidApp UI ausente.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** No hay alerta visual o sonora backend.

### T-11 · HU-09 — Calcular tiempo transcurrido desde el envío.
- **Archivos/rutas:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/service/impl/CalculadoraDemoraPedido.java; ServicioPedidosImpl.java`
- **Estado:** **PARCIAL (backend sí, UseCase móvil no)**
- **Nota técnica:** Umbrales: NORMAL <10 min, PREVENTIVA 10–<15, CRITICA >=15; LISTO sin umbral.

### T-12 · HU-09 — Borde amarillo a 10m y rojo a 15m.
- **Archivos/rutas:** `UI TarjetaPedido.kt ausente.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** El servidor entrega nivel de demora, pero el coloreado es de Android.

### T-13 · HU-09 — Alerta visible/accesible por demora.
- **Archivos/rutas:** `androidApp/.../AlertaDemoraPedido.kt ausente.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** No se puede afirmar que el alerta se vea hasta implementar la UI.

### T-14 · HU-09 — Reset del temporizador y probar 9:59/10:00/14:59/15:00.
- **Archivos/rutas:** `microservice/orders-service/src/test/java/pe/upeu/nullpointerpos/pedidos/service/impl/CalculadoraDemoraPedidoTest.java; Kotlin ViewModel test ausente`
- **Estado:** **PARCIAL: pruebas backend escritas, no ejecutadas**
- **Nota técnica:** Timer y reset visual pertenecen al cliente.

### T-15 · HU-10 — Botón Marcar listo en cada detalle KDS.
- **Archivos/rutas:** `androidApp/.../ui/cocina/components/DetallePlatoItem.kt no incluido.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** Se implementó el endpoint server-side.

### T-16 · HU-10 — DTO para estado LISTO.
- **Archivos/rutas:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/dto/request/SolicitudEstadoDetalle.java`
- **Estado:** **IMPLEMENTADO backend**
- **Nota técnica:** DTO tipado para operación del estado del detalle.

### T-17 · HU-10 — PATCH /api/v1/pedidos/detalle/{id}.
- **Archivos/rutas:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorEstadoDetalle.java`
- **Estado:** **IMPLEMENTADO backend**
- **Nota técnica:** Ruta real está publicada desde orders-service.

### T-18 · HU-10 — Actualizar detalle + fecha de finalización atómicamente.
- **Archivos/rutas:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/service/impl/ServicioPedidosImpl.java; .../repository/PedidoRepositorio.java; db/migration/V3__estado_detalle_plato.sql`
- **Estado:** **IMPLEMENTADO en código; migración por validar en ejecución**
- **Nota técnica:** Actualiza estado y fecha, y evalúa estado general del pedido.

### T-19 · HU-10 — Publicar cambio y probar permisos/no existe/transición/reintento.
- **Archivos/rutas:** `orders-service ServicioPedidosImpl/PublicadorEventosPendientes; kitchen-service ServicioEventosCocinaImpl; pruebas parciales`
- **Estado:** **PARCIAL**
- **Nota técnica:** El evento se emite vía outbox; falta ejecutar integración y casos de seguridad/transiciones.

### T-20 · HU-11 — Persistir estación de producción por producto.
- **Archivos/rutas:** `microservice/products-service/src/main/java/pe/upeu/nullpointerpos/productos/entity/Producto.java; db/migration/V2__estacion_produccion.sql; orders-service detalle y DTO KDS`
- **Estado:** **IMPLEMENTADO backend; datos/migración a probar**
- **Nota técnica:** Estación se copia como snapshot en la línea de pedido.

### T-21 · HU-11 — Filtros UI Todos/Parrillas/Bebidas/Cocina.
- **Archivos/rutas:** `androidApp/.../ui/cocina/FiltroEstacionRow.kt ausente.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** No implementado en este repositorio backend.

### T-22 · HU-11 — Filtrado reactivo de tarjetas en ViewModel.
- **Archivos/rutas:** `shared/src/commonMain/viewmodel/CocinaViewModel.kt ausente.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** Backend no filtra tarjetas en memoria de UI.

### T-23 · HU-11 — Pruebas de filtros/estación vacía.
- **Archivos/rutas:** `shared/src/commonTest/.../CocinaViewModelTest.kt ausente.`
- **Estado:** **PENDIENTE frontend/test**
- **Nota técnica:** Crear y ejecutar cuando exista shared.

### T-24 · HU-12 — Acción KDS disponible/agotado.
- **Archivos/rutas:** `androidApp/.../ui/cocina/DisponibilidadProductoDialog.kt ausente.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** El backend sí expone actualización.

### T-25 · HU-12 — DTO + PATCH disponibilidad.
- **Archivos/rutas:** `microservice/products-service/src/main/java/pe/upeu/nullpointerpos/productos/controller/ControladorMenu.java; dto/request/SolicitudDisponibilidadProducto.java; PATCH /api/v1/menu/disponibilidad`
- **Estado:** **IMPLEMENTADO backend**
- **Nota técnica:** El cambio está protegido por rol y el DTO valida entrada.

### T-26 · HU-12 — Persistir disponibilidad, auditoría y evento.
- **Archivos/rutas:** `microservice/products-service/src/main/java/pe/upeu/nullpointerpos/productos/service/impl/ServicioMenuImpl.java; repository/MenuRepositorio.java; service/impl/PublicadorDisponibilidadProducto.java; V3 migration`
- **Estado:** **IMPLEMENTADO en código; base/mensajería por probar**
- **Nota técnica:** Transacción local, auditoría y outbox.

### T-27 · HU-12 — Comprobar disponibilidad al crear/editar comanda.
- **Archivos/rutas:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/client/ClienteProductos.java; .../service/impl/ServicioPedidosImpl.java`
- **Estado:** **IMPLEMENTADO en código**
- **Nota técnica:** Orders consulta products-service antes de aceptar la línea.

### T-28 · HU-12 — Propagar estado a clientes vía Rabbit/refresco.
- **Archivos/rutas:** `products-service PublicadorDisponibilidadProducto; kitchen-service ReceptorDisponibilidadProducto; STOMP /topic/cocina/disponibilidad`
- **Estado:** **PARCIAL backend**
- **Nota técnica:** La difusión del servidor existe; refresco de catálogo en shared pendiente.

### T-29 · HU-13 — Crear cashier-service independiente y registrarlo en Compose/Gateway.
- **Archivos/rutas:** `microservice/cashier-service/{pom.xml,Dockerfile,src/main/resources/application.yml}; pom.xml raíz; docker-compose.yml; gateway application.yml`
- **Estado:** **IMPLEMENTADO en archivos; contenedor no probado**
- **Nota técnica:** Tiene puerto 8085 y BD `bd_caja`.

### T-30 · HU-13 — Persistir turno, cajero, apertura y estado.
- **Archivos/rutas:** `microservice/cashier-service/src/main/resources/db/migration/V1__crear_esquema_caja.sql; repository/RepositorioCaja.java`
- **Estado:** **IMPLEMENTADO en código**
- **Nota técnica:** Una restricción de BD evita más de un turno abierto por cajero.

### T-31 · HU-13 — Formulario móvil de apertura.
- **Archivos/rutas:** `androidApp/.../ui/caja/AperturaCajaScreen.kt y shared ViewModel no incluidos.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** Solo existe el endpoint.

### T-32 · HU-13 — POST apertura con autorización y turno único.
- **Archivos/rutas:** `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/controller/ControladorCaja.java; service/impl/ServicioCajaImpl.java; POST /api/v1/caja/apertura`
- **Estado:** **IMPLEMENTADO en código**
- **Nota técnica:** Autorización se aplica por filtro y reglas del servicio.

### T-33 · HU-14 — Formulario móvil de efectivo.
- **Archivos/rutas:** `androidApp/.../ui/caja/CobroEfectivoScreen.kt ausente.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** No forma parte del ZIP backend.

### T-34 · HU-14 — Calcular vuelto/redondeo/validar recibido.
- **Archivos/rutas:** `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/service/impl/ServicioCajaImpl.java; tests/CajaImportesTest.java`
- **Estado:** **PARCIAL backend**
- **Nota técnica:** Vuelto calculado server-side con BigDecimal; use case Kotlin no existe y prueba no ejecutada.

### T-35 · HU-14 — DTO efectivo.
- **Archivos/rutas:** `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/dto/request/SolicitudCobroEfectivo.java`
- **Estado:** **IMPLEMENTADO backend**
- **Nota técnica:** Recibe idPedido, importeRecibido e idempotencia.

### T-36 · HU-14 — POST cobro efectivo y transacción.
- **Archivos/rutas:** `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/controller/ControladorCaja.java; service/impl/ServicioCajaImpl.java; repository/RepositorioCaja.java; POST /api/v1/caja/cobrar`
- **Estado:** **IMPLEMENTADO en código; flujo E2E no probado**
- **Nota técnica:** Registra pago, partes/outbox dentro de transacción.

### T-37 · HU-14 — Evento pago confirmado, orders marca PAGADO.
- **Archivos/rutas:** `cashier-service PublicadorEventosCaja.java; orders-service/messaging/ReceptorPagoConfirmado.java; ServicioPedidosImpl.java`
- **Estado:** **IMPLEMENTADO en código; mensajería real por probar**
- **Nota técnica:** Servicios mantienen sus propias bases y se sincronizan por RabbitMQ.

### T-38 · HU-14 — Abrir cajón USB/serie.
- **Archivos/rutas:** `No existe ServicioCajonDineroImpl.java; requiere hardware/protocolo no indicado.`
- **Estado:** **PENDIENTE opcional**
- **Nota técnica:** No declarar como funcional sin modelo de cajón y pruebas físicas.

### T-39 · HU-15 — Pantalla Yape/Plin con QR.
- **Archivos/rutas:** `androidApp/.../ui/caja/CobroDigitalScreen.kt ausente.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** La API registra los datos de operación.

### T-40 · HU-15 — Validar número de operación y últimos cuatro dígitos.
- **Archivos/rutas:** `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/dto/request/SolicitudCobroDigital.java; ServicioCajaImpl.java`
- **Estado:** **PARCIAL backend**
- **Nota técnica:** Validación server-side; ViewModel y UI pendientes.

### T-41 · HU-15 — DTO + POST pago digital.
- **Archivos/rutas:** `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/controller/ControladorCaja.java; dto/request/SolicitudCobroDigital.java; POST /api/v1/caja/cobrar-digital`
- **Estado:** **IMPLEMENTADO en código**
- **Nota técnica:** Métodos admitidos YAPE/PLIN, referencia, últimos cuatro y llave de idempotencia.

### T-42 · HU-15 — Evitar referencia digital repetida.
- **Archivos/rutas:** `cashier-service/db/migration/V1__crear_esquema_caja.sql; repository/RepositorioCaja.java`
- **Estado:** **IMPLEMENTADO en código**
- **Nota técnica:** Restricción única de referencia digital; probar conflicto real en PostgreSQL.

### T-43 · HU-15 — Validar total y publicar pago.
- **Archivos/rutas:** `cashier-service ServicioCajaImpl/PublicadorEventosCaja; orders-service ReceptorPagoConfirmado`
- **Estado:** **PARCIAL: lógica escrita, integración no probada**
- **Nota técnica:** La verificación automática contra Yape/Plin no está integrada; registra declaración/referencia.

### T-44 · HU-16 — Formulario de pago mixto.
- **Archivos/rutas:** `androidApp/.../ui/caja/PagoMixtoScreen.kt ausente.`
- **Estado:** **PENDIENTE frontend**
- **Nota técnica:** UI no está en el ZIP.

### T-45 · HU-16 — DTO de partes de pago.
- **Archivos/rutas:** `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/dto/request/SolicitudPagoMixto.java; dto/request/PartePago.java`
- **Estado:** **IMPLEMENTADO backend**
- **Nota técnica:** Admite lista de partes con método e importe y campos digitales.

### T-46 · HU-16 — Sumas positivas que igualen total.
- **Archivos/rutas:** `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/service/impl/ServicioCajaImpl.java; DTO PartePago.java`
- **Estado:** **IMPLEMENTADO en lógica, test no ejecutado**
- **Nota técnica:** La suma usa BigDecimal y debe coincidir con el saldo del pedido.

### T-47 · HU-16 — Registrar partes en una transacción y emitir evento posterior al commit.
- **Archivos/rutas:** `cashier-service ServicioCajaImpl.java; RepositorioCaja.java; PublicadorEventosCaja.java`
- **Estado:** **IMPLEMENTADO en código; integración no probada**
- **Nota técnica:** Outbox programado publica el evento tras registro; confirmar en Docker.

### T-48 · HU-16 — Pruebas de pago mixto, duplicados y rollback.
- **Archivos/rutas:** `microservice/cashier-service/src/test/java/pe/upeu/nullpointerpos/caja/CajaImportesTest.java`
- **Estado:** **PARCIAL: tests aritméticos escritos, no ejecutados**
- **Nota técnica:** Añadir integración real con Testcontainers/PostgreSQL/RabbitMQ y ejecutar `mvn verify`.

## 6. Sprint 3 — mapa de HU

| HU | Función | Dónde está | Estado y explicación |
|---|---|---|---|
| HU-17 | Descuentos/promociones | `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorOperacionesPedido.java; service/impl/ServicioPedidosImpl.java; repository/PedidoRepositorio.java; PATCH /api/v1/pedidos/{idPedido}/descuento` | **PARCIAL.** Descuento porcentual con motivo y auditoría; no hay motor completo por cupón, calendario o combinaciones promocionales. |
| HU-18 | Consultar DNI/RUC | `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/controller/ControladorClientes.java; dto/request/SolicitudCliente.java; repository/RepositorioCaja.java; GET/PUT /api/v1/clientes` | **PARCIAL.** Valida DNI de 8 y RUC de 11 dígitos y consulta registro local; no integra RENIEC/SUNAT. |
| HU-19 | Enviar ticket por WhatsApp | `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/controller/ControladorTicketWhatsApp.java; service/ServicioWhatsApp.java; repository/RepositorioCaja.java; V1__crear_esquema_caja.sql` | **PARCIAL CONFIGURABLE.** Crea ticket; envío real requiere WHATSAPP_API_URL y WHATSAPP_ACCESS_TOKEN válidos y no pudo probarse. |
| HU-20 | Cierre y arqueo de caja | `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/controller/ControladorCaja.java; service/impl/ServicioCajaImpl.java; repository/RepositorioCaja.java; POST /api/v1/caja/cierre` | **IMPLEMENTADO en código.** Compara efectivo esperado (apertura + partes en efectivo) contra importe real y guarda diferencia. |
| HU-21 | Recetas/insumos por plato | `microservice/inventory-service/src/main/java/pe/upeu/nullpointerpos/inventario/controller/ControladorInventario.java; repository/RepositorioInventario.java; PUT /api/v1/inventario/recetas/{idProducto}` | **IMPLEMENTADO backend.** Configurar las recetas antes de esperar descuento de stock. |
| HU-22 | Descontar stock cuando se vende | `microservice/inventory-service/src/main/java/pe/upeu/nullpointerpos/inventario/messaging/ReceptorPedidoPagado.java; repository/RepositorioInventario.java; RabbitMQ `pedido.pagado`` | **PARCIAL: lógica implementada, integración no probada.** Deduplica por evento. Si falta stock, registra déficit y deja stock en cero; el negocio debe decidir si se debe rechazar la venta. |
| HU-23 | Aviso de bajo stock | `microservice/inventory-service/src/main/java/pe/upeu/nullpointerpos/inventario/controller/ControladorInventario.java; repository/RepositorioInventario.java; GET /api/v1/inventario/alertas` | **PARCIAL.** La API lista insumos <= mínimo; no hay notificación push ni UI de alerta. |
| HU-24 | Registrar compras | `microservice/inventory-service/src/main/java/pe/upeu/nullpointerpos/inventario/controller/ControladorInventario.java; repository/RepositorioInventario.java; POST /api/v1/inventario/compras` | **IMPLEMENTADO en código.** Incrementa stock y registra movimiento; integración de negocio no probada. |
| HU-25 | Registrar merma | `microservice/inventory-service/src/main/java/pe/upeu/nullpointerpos/inventario/controller/ControladorInventario.java; repository/RepositorioInventario.java; POST /api/v1/inventario/mermas` | **IMPLEMENTADO en código.** Registra motivo y movimiento de salida con validación de stock. |
| HU-27 | Reporte ventas por hora | `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorReportes.java; service/impl/ServicioReportes.java; repository/PedidoRepositorio.java; GET /api/v1/reportes/ventas-por-hora` | **IMPLEMENTADO en código; datos reales no probados.** Requiere rango de fechas opcional para consulta. |
| HU-28 | Platos populares | `orders-service ControladorReportes/ServicioReportes/PedidoRepositorio; GET /api/v1/reportes/platos-populares` | **IMPLEMENTADO en código; datos reales no probados.** Ranking agregado por producto vendido. |
| HU-29 | Auditar anulaciones/descuentos | `orders-service/.../controller/ControladorAuditoria.java; ControladorOperacionesPedido.java; GET /api/v1/auditoria/pedidos` | **IMPLEMENTADO en código.** Auditoría asociada a anulación y descuento; probar usuario y trazabilidad en ejecución. |
| HU-30 | Exportar resumen Excel | `orders-service/.../controller/ControladorReportes.java; GET /api/v1/reportes/ventas/exportar.xlsx` | **IMPLEMENTADO en código; archivo real no verificado.** Devuelve XLSX con hojas de ventas por hora y platos populares. |

## 7. Endpoints principales

| Método | Ruta | Servicio | Para qué sirve |
|---|---|---|---|
| POST | `/api/v1/autenticacion/iniciar-sesion` | authentication | Autenticar trabajador y emitir JWT |
| GET/POST | `/api/v1/administracion/usuarios` | authentication | Listar/crear cuentas |
| PATCH | `/api/v1/administracion/usuarios/{id}/activar` y `/desactivar` | authentication | Activar/desactivar cuentas |
| GET | `/api/v1/mesas` | orders | Listar mesas y estado |
| PUT | `/api/v1/mesas/trasladar` | orders | Cambiar pedido de mesa |
| POST | `/api/v1/pedidos` | orders | Crear comanda |
| PUT | `/api/v1/pedidos/{idPedido}/detalles` | orders | Actualizar productos/observaciones antes de envío |
| POST | `/api/v1/pedidos/{idPedido}/enviar-cocina` | orders | Cambiar comanda a enviada y generar evento |
| GET | `/api/v1/cocina/pedidos` | orders | Listado de KDS con demora |
| PATCH | `/api/v1/pedidos/detalle/{id}` | orders | Marcar detalle listo |
| GET/PATCH | `/api/v1/menu/productos`, `/api/v1/menu/disponibilidad` | products | Catálogo y disponibilidad |
| POST | `/api/v1/caja/apertura` | cashier | Abrir turno |
| POST | `/api/v1/caja/cobrar` | cashier | Pago efectivo |
| POST | `/api/v1/caja/cobrar-digital` | cashier | Pago Yape/Plin registrado |
| POST | `/api/v1/caja/cobrar-mixto` | cashier | Pago dividido |
| POST | `/api/v1/caja/cierre` | cashier | Cierre/arqueo |
| GET/PUT | `/api/v1/clientes/{tipoDocumento}/{numeroDocumento}`, `/api/v1/clientes` | cashier | Consulta/registro local de DNI/RUC |
| POST | `/api/v1/tickets/whatsapp` | cashier | Crear/enviar ticket si se configuró proveedor |
| GET/POST | `/api/v1/inventario/insumos` | inventory | Consultar/registrar insumos |
| PUT | `/api/v1/inventario/recetas/{idProducto}` | inventory | Definir receta de producto |
| POST | `/api/v1/inventario/compras` | inventory | Registrar compra |
| POST | `/api/v1/inventario/mermas` | inventory | Registrar merma |
| GET | `/api/v1/inventario/alertas`, `/movimientos` | inventory | Alertas por mínimo e historial |
| GET | `/api/v1/auditoria/pedidos` | orders | Consultar auditoría |
| GET | `/api/v1/reportes/ventas-por-hora`, `/platos-populares` | orders | Reportes |
| GET | `/api/v1/reportes/ventas/exportar.xlsx` | orders | Exportar XLSX |

## 8. Cómo ejecutar (Windows PowerShell)

1. Instalar e iniciar Docker Desktop y comprobar que Docker Compose funciona.  2. Descomprimir el proyecto y abrir terminal en la carpeta raíz.  3. Copiar `.env.example` como `.env`; cambiar secretos si se va más allá de una demo local.  4. Ejecutar:

```powershell
docker compose config
docker compose up --build -d
docker compose ps -a
docker compose logs -f puerta-enlace-api servicio-autenticacion servicio-pedidos servicio-productos servicio-cocina servicio-caja servicio-inventario
```

En una instalación limpia, verificar `http://localhost:8761` (Eureka), `http://localhost:15672` (RabbitMQ) y `GET http://localhost:8080/api/v1/salud`. Para recrear todas las BD locales desde cero, `docker compose down -v` elimina volúmenes y datos; luego se puede ejecutar `docker compose up --build -d`.

### Credenciales demo

| Código | Rol | Clave demo |
|---|---|---|
| ADM001 | ADMINISTRADOR | `1234` |
| MOZ001 / MOZ002 | MOZO | `1234` |
| COC001 | COCINERO | `1234` |
| CAJ001 | CAJERO | `1234` |
| ALM001 | ALMACENERO | `1234` |

Solo para entorno local. No publicar estas credenciales ni el `SECRETO_JWT` de ejemplo. El cliente debe enviar `Authorization: Bearer <token>` en las rutas protegidas.

### Ejemplo de prueba en Postman

1. `POST /api/v1/autenticacion/iniciar-sesion` con `{"codigoTrabajador":"MOZ001","clave":"1234"}`.  2. Guardar `token` como variable.  3. `GET /api/v1/mesas` y `GET /api/v1/menu/categorias`.  4. `POST /api/v1/pedidos` con `idMesa` y `detalles` válidos.  5. `POST /api/v1/pedidos/{id}/enviar-cocina`.  6. Probar `GET /api/v1/cocina/pedidos` y suscripción `/ws/cocina` + `/topic/cocina`.  7. Como cajero, abrir caja, efectuar cobro usando el pedido válido y verificar el evento y estado en órdenes.  8. Crear insumos y receta antes de verificar el descuento automático.

## 9. Puntos para la exposición oral

**Arquitectura:** “Separamos autenticación, catálogo, pedidos, cocina, caja e inventario en servicios independientes. Cada servicio tiene su propia responsabilidad y base de datos. API Gateway es la entrada para el cliente. Eureka descubre servicios y Config Server entrega configuración.”

**Comunicación:** “Las operaciones locales usan transacciones en la base de su propio servicio. Para propagar cambios a otros servicios usamos RabbitMQ y patrón outbox; así el servicio de origen no abre conexiones a la base ajena. Para la pantalla KDS, kitchen-service transforma los mensajes a WebSocket/STOMP.”

**Seguridad:** “El login usa clave de cuatro dígitos para la demo, se compara de forma segura en backend y se emite un JWT. Las rutas protegidas verifican token y rol. Las claves `1234` son solo datos de prueba.”

**Caja e inventario:** “El cobro se guarda en cashier-service. El mensaje de pago se procesa en orders-service para marcar la comanda y luego en inventory-service para descontar receta; eso evita transacciones distribuidas entre varias bases.”

**Limitaciones que se deben declarar:** “En el ZIP no está el frontend KMP. El envío de WhatsApp requiere configuración oficial, consulta de DNI/RUC es local, no hay cajón USB integrado, y las pruebas de compilación/infraestructura aún se tienen que ejecutar en el equipo con Docker/Maven.”

## 10. Pendientes críticos antes de llamarlo 100 % funcional

1. Ejecutar `mvn -B -ntp clean verify` con conectividad al repositorio Maven y resolver cualquier error real de compilación/prueba.  2. Ejecutar `docker compose config` y `docker compose up --build -d`, comprobar health y logs de los ocho componentes principales/infraestructura.  3. Completar una prueba real de ida y vuelta: login -> pedido -> cocina -> marcar plato listo -> abrir caja -> cobrar -> pedido PAGADO -> inventario descontado.  4. Probar roles denegados, idempotencia, duplicados, rollback, referencia digital repetida y cierres.  5. Añadir pruebas de integración automatizadas para RabbitMQ, PostgreSQL, WebSocket, caja e inventario.  6. Construir el cliente KMP/Android: todas las tareas descritas como UI/ViewModel están fuera de este ZIP.  7. Configurar proveedor externo RENIEC/SUNAT si es requisito, WhatsApp Cloud API y dispositivo de cajón si se exigieran.  8. Confirmar las reglas de negocio para ventas con falta de stock, promociones y tratamiento de pagos digitales; actualmente los pagos Yape/Plin registran la referencia declarada, no verifican automáticamente el saldo bancario.

## 11. Validación realizada al preparar la entrega

- Se inspeccionaron rutas, estructura, referencias de tareas, configuraciones y scripts.
- Se verificaron sintaxis/estructura de archivos Maven XML, YAML, JSON y ZIP.
- Se hizo escaneo de sintaxis de Java; esto **no equivale** a compilación con las dependencias.
- No fue posible ejecutar Maven, Docker Compose, pruebas unitarias ni flujos end-to-end en este entorno.
- La guía distingue deliberadamente implementación escrita, implementación parcial, frontend ausente y pruebas aún no ejecutadas.


## 6. Sprint 3 — desglose técnico propuesto de 59 tareas

**Alcance conservado:** este desglose nace de las historias de usuario originales HU-17 a HU-25 y HU-27 a HU-30. La HU-26 se mantiene en Sprint 1, como aparece en el Excel. Los códigos `S3-T-01` a `S3-T-59` son identificadores nuevos para que Sprint 3 no se confunda con los códigos T-01 que se repiten en Sprint 1 y Sprint 2. Es una propuesta desglosada para validar con el equipo/profesor; no reemplaza las HU aprobadas.

El criterio de aceptación describe cómo comprobar la tarea. “Implementado en código” no significa que esté validado en ejecución; todo lo que diga “pruebas pendientes” debe probarse después de abrir el proyecto en Maven y levantar Docker.

### S3-T-01 — HU-17: Aplicar descuento en caja
- **Descripción:** Definir y validar la solicitud de descuento: porcentaje y motivo obligatorio, precisión monetaria y rango permitido.
- **Capa/módulo:** orders-service · DTO
- **Ruta de referencia:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/dto/request/SolicitudDescuentoPedido.java`
- **Criterio de aceptación:** Rechazar campos vacíos, porcentaje 0 o >=100; motivo obligatorio con máximo 250 caracteres.
- **Estado según inspección estática:** Implementado en código; criterio de porcentaje debe alinearse con DTO y servicio.

### S3-T-02 — HU-17: Aplicar descuento en caja
- **Descripción:** Exponer PATCH de descuento y comprobar que solo CAJERO/ADMINISTRADOR pueda ejecutarlo.
- **Capa/módulo:** orders-service · Controller / Security
- **Ruta de referencia:** `orders/controller/ControladorOperacionesPedido.java; orders/config/ConfiguracionSeguridad.java; PATCH /api/v1/pedidos/{idPedido}/descuento`
- **Criterio de aceptación:** MOZO/COCINERO reciben 403; roles autorizados pueden continuar con pedido válido.
- **Estado según inspección estática:** Implementado en código; autorización real pendiente de prueba.

### S3-T-03 — HU-17: Aplicar descuento en caja
- **Descripción:** Validar estado del pedido y calcular descuento con redondeo decimal desde el servidor.
- **Capa/módulo:** orders-service · ServiceImpl
- **Ruta de referencia:** `orders/service/impl/ServicioPedidosImpl.java`
- **Criterio de aceptación:** No aplicar a borrador, pagado o anulado; total descontado y total final coherentes a dos decimales.
- **Estado según inspección estática:** Implementado en código; prueba de negocio pendiente.

### S3-T-04 — HU-17: Aplicar descuento en caja
- **Descripción:** Guardar porcentaje aplicado, nuevo total, actor y motivo dentro de la transacción y registrar auditoría.
- **Capa/módulo:** orders-service · Repository / DB
- **Ruta de referencia:** `orders/repository/PedidoRepositorio.java; db/migration/V3__*.sql`
- **Criterio de aceptación:** Un descuento aplicado queda asociado al pedido y deja un registro de auditoría; error revierte la transacción.
- **Estado según inspección estática:** Implementado en código; validar migraciones y DB real.

### S3-T-05 — HU-17: Aplicar descuento en caja
- **Descripción:** Crear pruebas de descuentos límite, motivo vacío, roles, pedido pagado/anulado y persistencia de auditoría.
- **Capa/módulo:** orders-service · Tests
- **Ruta de referencia:** `microservice/orders-service/src/test/java/pe/upeu/nullpointerpos/pedidos/ (por completar)`
- **Criterio de aceptación:** Pruebas automatizadas pasan con descuentos válidos e inválidos.
- **Estado según inspección estática:** Pendiente: pruebas específicas no están escritas/ejecutadas.

### S3-T-06 — HU-18: Buscar datos de cliente
- **Descripción:** Validar tipo y formato de documento, normalizar tipo a mayúsculas y revisar DNI de 8/RUC de 11 dígitos.
- **Capa/módulo:** cashier-service · DTO / Controller
- **Ruta de referencia:** `cashier/dto/request/SolicitudCliente.java; cashier/controller/ControladorClientes.java`
- **Criterio de aceptación:** DNI/RUC con longitud incorrecta devuelve 400; documento válido se normaliza.
- **Estado según inspección estática:** Implementado en código; pruebas pendientes.

### S3-T-07 — HU-18: Buscar datos de cliente
- **Descripción:** Consultar cliente local por tipo y número de documento, con respuesta clara si no existe.
- **Capa/módulo:** cashier-service · Controller / Repository
- **Ruta de referencia:** `cashier/controller/ControladorClientes.java; cashier/repository/RepositorioCaja.java; GET /api/v1/clientes/{tipoDocumento}/{numeroDocumento}`
- **Criterio de aceptación:** Un registro existente se recupera; inexistente devuelve 404 controlado.
- **Estado según inspección estática:** Implementado: consulta local solamente.

### S3-T-08 — HU-18: Buscar datos de cliente
- **Descripción:** Registrar o actualizar datos de cliente evitando duplicados por tipo y número de documento.
- **Capa/módulo:** cashier-service · Repository / DB
- **Ruta de referencia:** `cashier/repository/RepositorioCaja.java; tabla esquema_caja.clientes; PUT /api/v1/clientes`
- **Criterio de aceptación:** Una persona documentalmente única se actualiza en vez de duplicarse.
- **Estado según inspección estática:** Implementado en código; pruebas pendientes.

### S3-T-09 — HU-18: Buscar datos de cliente
- **Descripción:** Definir adaptador configurable para consultar el proveedor externo autorizado de DNI/RUC, con timeout y gestión de errores.
- **Capa/módulo:** cashier-service · Integración externa
- **Ruta de referencia:** `Nuevo adaptador de proveedor RENIEC/SUNAT o API contratada; configuración en .env/application.yml`
- **Criterio de aceptación:** La consulta externa solo se activa con URL, token/credenciales y contrato válido; no exponer secretos en logs.
- **Estado según inspección estática:** Pendiente: no existe integración RENIEC/SUNAT real en el ZIP.

### S3-T-10 — HU-18: Buscar datos de cliente
- **Descripción:** Probar validación de DNI/RUC, actualización idempotente, documento no registrado y fallos del proveedor externo.
- **Capa/módulo:** cashier-service · Tests
- **Ruta de referencia:** `microservice/cashier-service/src/test/java/pe/upeu/nullpointerpos/caja/ (por completar)`
- **Criterio de aceptación:** Casos válidos e inválidos producen respuestas esperadas sin duplicados.
- **Estado según inspección estática:** Pendiente: pruebas específicas no ejecutadas.

### S3-T-11 — HU-19: Enviar ticket al WhatsApp
- **Descripción:** Generar contenido del ticket a partir del código, estado y total del pedido consultado en orders-service.
- **Capa/módulo:** cashier-service · Controller / HTTP client
- **Ruta de referencia:** `cashier/controller/ControladorTicketWhatsApp.java; cashier/client/ClientePedidos.java`
- **Criterio de aceptación:** El ticket corresponde al pedido solicitado y no se genera para borrador/anulado.
- **Estado según inspección estática:** Implementado en código; no validado end-to-end.

### S3-T-12 — HU-19: Enviar ticket al WhatsApp
- **Descripción:** Persistir el ticket con teléfono, contenido, estado, respuesta del proveedor y marcas de tiempo.
- **Capa/módulo:** cashier-service · Repository / DB
- **Ruta de referencia:** `cashier/repository/RepositorioCaja.java; tabla esquema_caja.tickets_whatsapp`
- **Criterio de aceptación:** El ticket conserva su estado incluso cuando el proveedor no está configurado o falla.
- **Estado según inspección estática:** Implementado en código; persistencia real pendiente de prueba.

### S3-T-13 — HU-19: Enviar ticket al WhatsApp
- **Descripción:** Configurar el envío mediante WhatsApp Business Cloud API a través de URL y token de entorno.
- **Capa/módulo:** cashier-service · Provider adapter
- **Ruta de referencia:** `cashier/service/ServicioWhatsApp.java; WHATSAPP_API_URL; WHATSAPP_ACCESS_TOKEN`
- **Criterio de aceptación:** Sin credenciales informa PENDIENTE_CONFIGURACION; con credenciales procesa respuesta/errores del proveedor.
- **Estado según inspección estática:** Parcial: requiere cuenta, endpoint y token autorizados.

### S3-T-14 — HU-19: Enviar ticket al WhatsApp
- **Descripción:** Permitir reintentar un ticket guardado y evitar perder la causa de error del último intento.
- **Capa/módulo:** cashier-service · Controller / Service
- **Ruta de referencia:** `cashier/controller/ControladorTicketWhatsApp.java; POST /api/v1/tickets/{idTicket}/reintentar`
- **Criterio de aceptación:** El reintento usa teléfono y contenido guardados y responde estado claro.
- **Estado según inspección estática:** Implementado en código; prueba de integración pendiente.

### S3-T-15 — HU-19: Enviar ticket al WhatsApp
- **Descripción:** Probar respuesta del proveedor, fallo de red, credenciales ausentes, reintento y límite de la respuesta guardada.
- **Capa/módulo:** cashier-service · Tests / QA
- **Ruta de referencia:** `ServicioWhatsApp y ControladorTicketWhatsApp; pruebas con proveedor simulado`
- **Criterio de aceptación:** La API no expone secretos y el estado es reproducible según resultado del proveedor.
- **Estado según inspección estática:** Pendiente: no ejecutado con proveedor real.

### S3-T-16 — HU-20: Cierre y arqueo de caja
- **Descripción:** Validar importe contado y motivo del cierre, incluyendo monto real no negativo.
- **Capa/módulo:** cashier-service · DTO
- **Ruta de referencia:** `cashier/dto/request/SolicitudCierreCaja.java`
- **Criterio de aceptación:** Importe negativo o solicitud inválida se rechaza antes de tocar la BD.
- **Estado según inspección estática:** Implementado en código; pruebas pendientes.

### S3-T-17 — HU-20: Cierre y arqueo de caja
- **Descripción:** Calcular el efectivo esperado como fondo inicial más todas las partes de pago en efectivo confirmadas del turno.
- **Capa/módulo:** cashier-service · Service / Repository
- **Ruta de referencia:** `cashier/service/impl/ServicioCajaImpl.java; cashier/repository/RepositorioCaja.java`
- **Criterio de aceptación:** No sumar pagos digitales al efectivo esperado; calcular diferencia = real - esperado.
- **Estado según inspección estática:** Implementado en código; validar con pagos mixtos reales.

### S3-T-18 — HU-20: Cierre y arqueo de caja
- **Descripción:** Cerrar el turno solo si está ABIERTO y persistir esperado, contado y diferencia como operación atómica.
- **Capa/módulo:** cashier-service · Repository / DB
- **Ruta de referencia:** `cashier/repository/RepositorioCaja.java; POST /api/v1/caja/cierre`
- **Criterio de aceptación:** Un turno solo puede cerrarse una vez; doble cierre devuelve conflicto.
- **Estado según inspección estática:** Implementado en código; prueba concurrente pendiente.

### S3-T-19 — HU-20: Cierre y arqueo de caja
- **Descripción:** Restringir apertura/cierre a roles autorizados y garantizar que los pagos se vinculen al turno del cajero.
- **Capa/módulo:** cashier-service · Security / Service
- **Ruta de referencia:** `cashier/config/ConfiguracionSeguridad.java; cashier/service/impl/ServicioCajaImpl.java`
- **Criterio de aceptación:** No se puede cobrar sin turno abierto ni cerrar un turno ajeno.
- **Estado según inspección estática:** Implementado en código; autorización a verificar.

### S3-T-20 — HU-20: Cierre y arqueo de caja
- **Descripción:** Probar caja sin ventas, solo efectivo, pagos mixtos, diferencia positiva/negativa y doble cierre.
- **Capa/módulo:** cashier-service · Tests
- **Ruta de referencia:** `microservice/cashier-service/src/test/java/pe/upeu/nullpointerpos/caja/ (por completar)`
- **Criterio de aceptación:** Las cifras del cierre coinciden con los pagos persistidos.
- **Estado según inspección estática:** Pendiente: pruebas específicas no ejecutadas.

### S3-T-21 — HU-21: Registrar insumos por plato
- **Descripción:** Crear DTO validado de insumos con nombre, unidad, stock inicial, mínimo y costo no negativos.
- **Capa/módulo:** inventory-service · DTO
- **Ruta de referencia:** `inventory/dto/request/SolicitudInsumo.java`
- **Criterio de aceptación:** Nombre/unidad obligatorios; las cantidades y el costo no pueden ser negativos.
- **Estado según inspección estática:** Implementado en código; pruebas pendientes.

### S3-T-22 — HU-21: Registrar insumos por plato
- **Descripción:** Registrar insumos y crear el movimiento de ajuste inicial cuando el stock inicial es mayor que cero.
- **Capa/módulo:** inventory-service · Repository / DB
- **Ruta de referencia:** `inventory/repository/RepositorioInventario.java; POST /api/v1/inventario/insumos`
- **Criterio de aceptación:** Alta y movimiento inicial se guardan en la misma transacción.
- **Estado según inspección estática:** Implementado en código; verificar en PostgreSQL.

### S3-T-23 — HU-21: Registrar insumos por plato
- **Descripción:** Permitir guardar la receta de un plato reemplazando de forma transaccional la lista de ingredientes.
- **Capa/módulo:** inventory-service · Controller / Repository
- **Ruta de referencia:** `inventory/controller/ControladorInventario.java; inventory/repository/RepositorioInventario.java; PUT /api/v1/inventario/recetas/{idProducto}`
- **Criterio de aceptación:** Los ingredientes se actualizan en conjunto; si uno falla, la receta anterior no queda parcialmente borrada.
- **Estado según inspección estática:** Implementado en código; protección contra lista vacía/IDs repetidos añadida.

### S3-T-24 — HU-21: Registrar insumos por plato
- **Descripción:** Verificar existencia del producto del catálogo al guardar una receta, sin escribir directamente en la base de products-service.
- **Capa/módulo:** inventory-service · HTTP integration
- **Ruta de referencia:** `Adaptador de consulta de products-service por API/descubrimiento; hoy no existe en inventory-service`
- **Criterio de aceptación:** Producto inexistente devuelve error controlado y cada microservicio mantiene propiedad de sus datos.
- **Estado según inspección estática:** Pendiente: ID de producto no se valida contra products-service.

### S3-T-25 — HU-21: Registrar insumos por plato
- **Descripción:** Crear pruebas de alta de insumo, receta válida, ingrediente repetido, insumo inexistente y rollback transaccional.
- **Capa/módulo:** inventory-service · Tests
- **Ruta de referencia:** `microservice/inventory-service/src/test/java/pe/upeu/nullpointerpos/inventario/`
- **Criterio de aceptación:** Los casos válidos pasan y los inválidos no dejan datos parciales.
- **Estado según inspección estática:** Parcial: hay prueba de cálculo de receta; faltan pruebas de integración.

### S3-T-26 — HU-22: Descuento automático de stock
- **Descripción:** Publicar el evento PEDIDO_PAGADO desde outbox de orders-service y consumirlo en inventory-service sin conectar bases de otros servicios.
- **Capa/módulo:** orders-service + inventory-service · Messaging
- **Ruta de referencia:** `orders/service/impl/PublicadorEventosPendientes.java; inventory/messaging/ReceptorPedidoPagado.java; RabbitMQ clave pedido.pagado`
- **Criterio de aceptación:** El evento contiene idEvento, pedido.idPedido y detalles con idProducto/cantidad.
- **Estado según inspección estática:** Implementado en código; flujo real requiere levantar RabbitMQ.

### S3-T-27 — HU-22: Descuento automático de stock
- **Descripción:** Validar el contrato del evento y deduplicar por idEvento para tolerar entregas repetidas de RabbitMQ.
- **Capa/módulo:** inventory-service · Messaging / Repository
- **Ruta de referencia:** `inventory/repository/RepositorioInventario.java; esquema_inventario.eventos_procesados`
- **Criterio de aceptación:** Evento repetido no vuelve a descontar stock; mensaje inválido revierte la transacción.
- **Estado según inspección estática:** Implementado en código; pruebas de mensajería pendientes.

### S3-T-28 — HU-22: Descuento automático de stock
- **Descripción:** Calcular requerimientos por producto a partir de la receta y cantidad vendida, bloqueando filas de insumos al modificar stock.
- **Capa/módulo:** inventory-service · Repository / DB
- **Ruta de referencia:** `inventory/repository/RepositorioInventario.java; tabla esquema_inventario.recetas`
- **Criterio de aceptación:** Requerimiento = cantidad_por_plato × cantidad de platos; se toma bloqueo de insumos involucrados.
- **Estado según inspección estática:** Implementado en código; requiere ensayo concurrente.

### S3-T-29 — HU-22: Descuento automático de stock
- **Descripción:** Descontar solo el stock disponible y registrar en movimientos la cantidad físicamente retirada, no el requerimiento teórico.
- **Capa/módulo:** inventory-service · Repository / DB
- **Ruta de referencia:** `inventory/repository/RepositorioInventario.java; tabla esquema_inventario.movimientos`
- **Criterio de aceptación:** La cantidad del movimiento coincide con la reducción real de stock y nunca es negativa/cero.
- **Estado según inspección estática:** Corregido en código; ejecución con PostgreSQL pendiente.

### S3-T-30 — HU-22: Descuento automático de stock
- **Descripción:** Registrar faltantes de venta en una tabla separada y consultarlos para poder reponer sin falsear movimientos de inventario.
- **Capa/módulo:** inventory-service · Migration / API
- **Ruta de referencia:** `inventory-service/src/main/resources/db/migration/V2__crear_faltantes_venta.sql; GET /api/v1/inventario/faltantes-venta`
- **Criterio de aceptación:** Cuando stock disponible < requerido, se registra la brecha y el evento se mantiene idempotente.
- **Estado según inspección estática:** Añadido en código; migración y endpoint pendientes de probar.

### S3-T-31 — HU-23: Aviso de poco stock
- **Descripción:** Definir regla de alerta al alcanzar o quedar por debajo del mínimo configurado por insumo.
- **Capa/módulo:** inventory-service · Repository / Business rule
- **Ruta de referencia:** `inventory/repository/RepositorioInventario.java; GET /api/v1/inventario/alertas`
- **Criterio de aceptación:** Alertar cuando stock_actual <= stock_minimo, ordenando por mayor faltante relativo.
- **Estado según inspección estática:** Implementado en código.

### S3-T-32 — HU-23: Aviso de poco stock
- **Descripción:** Exponer la lista de insumos bajo mínimo con stock actual y mínimo para que la aplicación pueda representarlos.
- **Capa/módulo:** inventory-service · API
- **Ruta de referencia:** `inventory/controller/ControladorInventario.java; GET /api/v1/inventario/alertas`
- **Criterio de aceptación:** La respuesta incluye código, nombre, unidad, stock actual y stock mínimo.
- **Estado según inspección estática:** Implementado en código; no incluye notificación push.

### S3-T-33 — HU-23: Aviso de poco stock
- **Descripción:** Diseñar y conectar la notificación visual/sonora de stock bajo en la pantalla de almacén o administración.
- **Capa/módulo:** shared/androidApp · Client UI
- **Ruta de referencia:** `Fuera del ZIP backend: no contiene shared/ ni androidApp/`
- **Criterio de aceptación:** La interfaz muestra alertas y permite refrescar sin duplicar notificaciones.
- **Estado según inspección estática:** Pendiente frontend; fuera de este ZIP.

### S3-T-34 — HU-23: Aviso de poco stock
- **Descripción:** Probar umbral exacto, por encima del mínimo, stock cero, insumo inactivo y orden de alertas.
- **Capa/módulo:** inventory-service · Tests
- **Ruta de referencia:** `microservice/inventory-service/src/test/java/pe/upeu/nullpointerpos/inventario/`
- **Criterio de aceptación:** Los resultados coinciden exactamente con stock_actual <= stock_minimo.
- **Estado según inspección estática:** Pendiente: pruebas específicas no ejecutadas.

### S3-T-35 — HU-24: Registrar compras de insumos
- **Descripción:** Validar DTO de compra: proveedor/documento/observación y lista no vacía de detalles con cantidades positivas.
- **Capa/módulo:** inventory-service · DTO
- **Ruta de referencia:** `inventory/dto/request/SolicitudCompra.java; ItemCompra.java`
- **Criterio de aceptación:** La compra sin detalles o con cantidad/costo inválidos devuelve 400.
- **Estado según inspección estática:** Implementado en código; pruebas pendientes.

### S3-T-36 — HU-24: Registrar compras de insumos
- **Descripción:** Crear cabecera de compra y persistir sus detalles vinculados en una misma transacción.
- **Capa/módulo:** inventory-service · Repository / DB
- **Ruta de referencia:** `inventory/repository/RepositorioInventario.java; POST /api/v1/inventario/compras`
- **Criterio de aceptación:** No quedan cabeceras incompletas si un detalle falla.
- **Estado según inspección estática:** Implementado en código.

### S3-T-37 — HU-24: Registrar compras de insumos
- **Descripción:** Incrementar stock y actualizar costo unitario solo para insumos activos; revertir todo si algún insumo no existe o está inactivo.
- **Capa/módulo:** inventory-service · Repository / DB
- **Ruta de referencia:** `inventory/repository/RepositorioInventario.java; validación y transacción añadidas`
- **Criterio de aceptación:** Stock, detalle de compra y movimiento se guardan juntos; error deja la compra sin efecto.
- **Estado según inspección estática:** Corregido en código; verificar rollback en BD.

### S3-T-38 — HU-24: Registrar compras de insumos
- **Descripción:** Crear pruebas con compra de varios insumos, ID inválido, cantidad inválida y revisión de los movimientos generados.
- **Capa/módulo:** inventory-service · Tests
- **Ruta de referencia:** `microservice/inventory-service/src/test/java/pe/upeu/nullpointerpos/inventario/ (por completar)`
- **Criterio de aceptación:** No hay actualización parcial si falla una línea de compra.
- **Estado según inspección estática:** Pendiente: pruebas de persistencia no ejecutadas.

### S3-T-39 — HU-25: Registrar merma de insumos
- **Descripción:** Validar ID de insumo, cantidad positiva y motivo obligatorio de la merma.
- **Capa/módulo:** inventory-service · DTO
- **Ruta de referencia:** `inventory/dto/request/SolicitudMerma.java`
- **Criterio de aceptación:** Solicitudes vacías o con cantidad no positiva se rechazan.
- **Estado según inspección estática:** Implementado en código.

### S3-T-40 — HU-25: Registrar merma de insumos
- **Descripción:** Reducir el stock únicamente si el insumo está activo y hay cantidad suficiente, dentro de una transacción.
- **Capa/módulo:** inventory-service · Repository / DB
- **Ruta de referencia:** `inventory/repository/RepositorioInventario.java; POST /api/v1/inventario/mermas`
- **Criterio de aceptación:** No se permiten stocks negativos; falta de stock devuelve conflicto.
- **Estado según inspección estática:** Implementado en código.

### S3-T-41 — HU-25: Registrar merma de insumos
- **Descripción:** Guardar movimiento de tipo MERMA con cantidad, referencia única, motivo y usuario actor.
- **Capa/módulo:** inventory-service · Repository / Audit
- **Ruta de referencia:** `inventory/repository/RepositorioInventario.java; esquema_inventario.movimientos`
- **Criterio de aceptación:** El historial permite justificar la salida de inventario y conocer el responsable.
- **Estado según inspección estática:** Implementado en código.

### S3-T-42 — HU-25: Registrar merma de insumos
- **Descripción:** Probar merma válida, merma superior al stock, insumo inactivo y persistencia del motivo/actor.
- **Capa/módulo:** inventory-service · Tests
- **Ruta de referencia:** `microservice/inventory-service/src/test/java/pe/upeu/nullpointerpos/inventario/ (por completar)`
- **Criterio de aceptación:** Los casos inválidos no alteran el stock.
- **Estado según inspección estática:** Pendiente: pruebas de repositorio no ejecutadas.

### S3-T-43 — HU-27: Reporte de ventas por hora
- **Descripción:** Validar período desde/hasta, usar fechas por defecto cuando no se reciben y limitar el endpoint a administración.
- **Capa/módulo:** orders-service · Controller / Security
- **Ruta de referencia:** `orders/controller/ControladorReportes.java; orders/config/ConfiguracionSeguridad.java`
- **Criterio de aceptación:** Hasta no puede ser anterior a desde; acceso sin rol admin devuelve 403.
- **Estado según inspección estática:** Implementado en código; pruebas de seguridad pendientes.

### S3-T-44 — HU-27: Reporte de ventas por hora
- **Descripción:** Agrupar ventas pagadas por hora y calcular conteo de pedidos y total vendido desde orders-service.
- **Capa/módulo:** orders-service · Repository / SQL
- **Ruta de referencia:** `orders/repository/PedidoRepositorio.java; método ventasPorHora`
- **Criterio de aceptación:** Los pedidos no pagados no aparecen y las horas se devuelven ordenadas.
- **Estado según inspección estática:** Implementado en código.

### S3-T-45 — HU-27: Reporte de ventas por hora
- **Descripción:** Exponer métricas por hora mediante una respuesta JSON estable para su uso en gráficos.
- **Capa/módulo:** orders-service · API
- **Ruta de referencia:** `orders/controller/ControladorReportes.java; GET /api/v1/reportes/ventas-por-hora`
- **Criterio de aceptación:** La respuesta incluye hora, cantidad de pedidos y total; un día sin ventas devuelve lista vacía.
- **Estado según inspección estática:** Implementado en código; el gráfico móvil queda fuera del ZIP.

### S3-T-46 — HU-27: Reporte de ventas por hora
- **Descripción:** Probar límites de fecha, rango de un día, rango mensual, ausencia de ventas y autorización del rol.
- **Capa/módulo:** orders-service · Tests
- **Ruta de referencia:** `microservice/orders-service/src/test/java/pe/upeu/nullpointerpos/pedidos/ (por completar)`
- **Criterio de aceptación:** Los rangos inclusivos/exclusivos corresponden al período solicitado.
- **Estado según inspección estática:** Pendiente: pruebas de consultas no ejecutadas.

### S3-T-47 — HU-28: Reporte de platos populares
- **Descripción:** Agrupar unidades vendidas por producto y sumar las líneas asociadas a pedidos pagados.
- **Capa/módulo:** orders-service · Repository / SQL
- **Ruta de referencia:** `orders/repository/PedidoRepositorio.java; método platosPopulares`
- **Criterio de aceptación:** El ranking se calcula sobre ventas pagadas y se agrupa por producto/nombre.
- **Estado según inspección estática:** Implementado en código.

### S3-T-48 — HU-28: Reporte de platos populares
- **Descripción:** Excluir pedidos no pagados y líneas anuladas; ordenar ranking por unidades y después importe vendido.
- **Capa/módulo:** orders-service · Repository / SQL
- **Ruta de referencia:** `orders/repository/PedidoRepositorio.java; método platosPopulares`
- **Criterio de aceptación:** No aparecen productos de pedidos anulados/no pagados en el ranking.
- **Estado según inspección estática:** Implementado en código.

### S3-T-49 — HU-28: Reporte de platos populares
- **Descripción:** Exponer el ranking con filtro por fecha para el período/mes solicitado.
- **Capa/módulo:** orders-service · API
- **Ruta de referencia:** `orders/controller/ControladorReportes.java; GET /api/v1/reportes/platos-populares?desde=AAAA-MM-DD&hasta=AAAA-MM-DD`
- **Criterio de aceptación:** El endpoint retorna nombre, unidades y venta dentro del período.
- **Estado según inspección estática:** Implementado en código.

### S3-T-50 — HU-28: Reporte de platos populares
- **Descripción:** Probar ranking con varios productos, cantidades iguales, líneas anuladas y mes sin registros.
- **Capa/módulo:** orders-service · Tests
- **Ruta de referencia:** `microservice/orders-service/src/test/java/pe/upeu/nullpointerpos/pedidos/ (por completar)`
- **Criterio de aceptación:** El orden es determinista y una consulta vacía no produce error.
- **Estado según inspección estática:** Pendiente: pruebas SQL no ejecutadas.

### S3-T-51 — HU-29: Revisar pedidos anulados
- **Descripción:** Registrar acción, actor y motivo al aplicar descuento o anular un pedido, sin perder el historial.
- **Capa/módulo:** orders-service · Service / Audit
- **Ruta de referencia:** `orders/service/impl/ServicioPedidosImpl.java; pedido descuento/anulación`
- **Criterio de aceptación:** Cada acción sensible deja un registro asociado a usuario y pedido.
- **Estado según inspección estática:** Implementado en código; validación end-to-end pendiente.

### S3-T-52 — HU-29: Revisar pedidos anulados
- **Descripción:** Persistir la anulación solo si el pedido no está pagado ni anulado y actualizar el estado de la mesa según otros pedidos activos.
- **Capa/módulo:** orders-service · Repository / Service
- **Ruta de referencia:** `orders/repository/PedidoRepositorio.java; orders/service/impl/ServicioPedidosImpl.java`
- **Criterio de aceptación:** No se anula un pedido pagado; una mesa con otro pedido activo permanece ocupada.
- **Estado según inspección estática:** Implementado en código; pruebas de concurrencia pendientes.

### S3-T-53 — HU-29: Revisar pedidos anulados
- **Descripción:** Exponer consulta de auditoría filtrable por pedido y protegerla para administradores.
- **Capa/módulo:** orders-service · Controller / Security / Repository
- **Ruta de referencia:** `orders/controller/ControladorAuditoria.java; GET /api/v1/auditoria/pedidos?idPedido={id}; orders/config/ConfiguracionSeguridad.java`
- **Criterio de aceptación:** Solo ADMINISTRADOR puede consultar auditoría; la consulta general está limitada.
- **Estado según inspección estática:** Implementado en código; verificar permisos con token real.

### S3-T-54 — HU-29: Revisar pedidos anulados
- **Descripción:** Probar auditoría de descuento/anulación, actor, motivo, filtro por pedido y rechazo a otros roles.
- **Capa/módulo:** orders-service · Tests
- **Ruta de referencia:** `microservice/orders-service/src/test/java/pe/upeu/nullpointerpos/pedidos/ (por completar)`
- **Criterio de aceptación:** La secuencia de auditoría es trazable y no se modifica al consultar.
- **Estado según inspección estática:** Pendiente: pruebas de auditoría no ejecutadas.

### S3-T-55 — HU-30: Exportar resumen de ventas a Excel
- **Descripción:** Crear libro XLSX con hojas de ventas por hora y platos populares.
- **Capa/módulo:** orders-service · Service / Apache POI
- **Ruta de referencia:** `orders/service/impl/ServicioReportes.java; dependencia poi-ooxml`
- **Criterio de aceptación:** El libro contiene las dos hojas y encabezados esperados.
- **Estado según inspección estática:** Implementado en código; archivo no generado en este entorno.

### S3-T-56 — HU-30: Exportar resumen de ventas a Excel
- **Descripción:** Completar filas de datos, totales/ventas, cantidad de pedidos, nombres y ajuste de columnas.
- **Capa/módulo:** orders-service · Service / Excel
- **Ruta de referencia:** `orders/service/impl/ServicioReportes.java`
- **Criterio de aceptación:** El reporte incluye datos del período, encabezados y columnas legibles.
- **Estado según inspección estática:** Implementado en código; revisar formato contable/moneda.

### S3-T-57 — HU-30: Exportar resumen de ventas a Excel
- **Descripción:** Exponer descarga con Content-Type XLSX y nombre de archivo estable.
- **Capa/módulo:** orders-service · Controller / HTTP
- **Ruta de referencia:** `orders/controller/ControladorReportes.java; GET /api/v1/reportes/ventas/exportar.xlsx`
- **Criterio de aceptación:** El navegador/Postman recibe archivo .xlsx, no JSON ni texto.
- **Estado según inspección estática:** Implementado en código; descarga real pendiente de probar.

### S3-T-58 — HU-30: Exportar resumen de ventas a Excel
- **Descripción:** Aplicar filtro temporal al exportar y rechazar períodos invertidos.
- **Capa/módulo:** orders-service · Controller / Service
- **Ruta de referencia:** `orders/controller/ControladorReportes.java; ServicioReportes.java`
- **Criterio de aceptación:** El contenido corresponde solo al intervalo solicitado.
- **Estado según inspección estática:** Implementado en código.

### S3-T-59 — HU-30: Exportar resumen de ventas a Excel
- **Descripción:** Probar apertura del XLSX, dos hojas, contenido de ventas, período vacío y errores HTTP.
- **Capa/módulo:** orders-service · Tests / QA
- **Ruta de referencia:** `microservice/orders-service/src/test/java/pe/upeu/nullpointerpos/pedidos/ (por completar)`
- **Criterio de aceptación:** El archivo se puede abrir y las cifras coinciden con los endpoints JSON.
- **Estado según inspección estática:** Pendiente: pruebas de archivo no ejecutadas.

## 7. Correcciones incorporadas en la última revisión

- `orders-service`: añadido `MesaRepositorio.actualizarEstadoSegunPedidos()`; se invoca después de crear el pedido, trasladarlo, anularlo o confirmar su pago. La consulta conserva `FUERA_SERVICIO` y decide `OCUPADA/LIBRE` según pedidos no terminales.
- `orders-service`: `PedidoRepositorio.buscarBloqueando()` y `buscarEstado()` cargan `importe_total`; `PedidoMapper.estadoDesdeFila()` lo asigna. `registrarPagoConfirmado()` ahora puede comparar el importe del evento con el total real antes de cambiar estado.
- `orders-service`: se limitó el porcentaje DTO a 99.99 y se ajustó el redondeo para que un descuento porcentual no consuma el último céntimo de un pedido con total positivo. Un pedido con total cero no admite descuento.
- `inventory-service`: el registro de movimiento `VENTA` usa la cantidad física que se descontó, no el requerimiento teórico. Cuando falta insumo se guarda la diferencia en `esquema_inventario.faltantes_venta` y se expone `GET /api/v1/inventario/faltantes-venta`.
- Se agregó la migración `V2__crear_faltantes_venta.sql`; no se modificó la migración V1, para evitar romper el checksum de Flyway de instalaciones que ya ejecutaron V1.
- `inventory-service`: se valida que las recetas no tengan ID de producto/insumo no positivo, lista vacía ni insumos repetidos. Las compras revierten la transacción si intentan actualizar un insumo inexistente o inactivo.

## 8. Límite de verificación

La revisión con parser de Java reportó cero errores de sintaxis en 155 archivos (`parseErrors=0`). Esto no equivale a una compilación Maven: este entorno no tiene `mvn`, Docker ni un repositorio local de dependencias, y el Maven Wrapper necesita descargar Maven. No se afirma que el conjunto compile o que los flujos distribuidos funcionen hasta ejecutar la guía de pruebas en un equipo con conexión a Maven Central y Docker Desktop.

El ZIP contiene solo backend Java. Las tareas de pantalla, navegación, gráficos móviles, temporizadores y notificaciones en `shared/`/`androidApp/` siguen fuera del paquete y deben implementarse/probarse en el repositorio del cliente móvil.

## 9. Cómo solucionar archivos rojos en orders-service antes de probar

En VS Code o IntelliJ, abre la carpeta raíz interna del proyecto, la que contiene el `pom.xml` multimódulo, `.mvn/`, `microservice/`, `infrastructure/` y `docker-compose.yml`. No abras solamente `microservice/orders-service/src`. Selecciona JDK 21 y vuelve a cargar los proyectos Maven.

En PowerShell desde esa raíz: `java -version`, `.\mvnw.cmd -B -ntp -pl microservice/servicio-pedidos -am clean verify`. Después de que la compilación local pase, ejecuta `docker compose config`, `docker compose up --build -d`, `docker compose ps -a` y `docker compose logs -f servicio-pedidos`. La guía `COMO_IMPORTAR_Y_PROBAR_ORDERS_SERVICE.md` presenta los pasos detallados.

## Nota de corrección posterior: `orders-service` en rojo

En `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/repository/PedidoRepositorio.java` faltaba `import java.util.Map;`, mientras que varias consultas declaraban `List<Map<String,Object>>`. La importación fue añadida. También se comprobó que las llamadas entre `ControladorOperacionesPedido`, `ServicioPedidos`, `ServicioPedidosImpl`, `PedidoRepositorio` y `MesaRepositorio` coinciden estáticamente. Para evitar falsos errores del IDE, abre la carpeta raíz que contiene el `pom.xml` multimódulo, selecciona Java 21 y recarga Maven. Ejecuta `.\mvnw.cmd -B -ntp -pl microservice/orders-service -am clean verify` desde PowerShell. La compilación real aún debe confirmarse en el equipo que tiene Maven/dependencias disponibles.


## Actualización Sprint 3: HU-31 — datos del comprador

Se añadió `POST /api/v1/comprobantes/datos-cliente/validar` para las modalidades `SIN_DATOS`, `MANUAL`, `DNI` y `RUC`. La boleta admite sin datos; factura requiere RUC y razón social. Para autocompletar en el entorno local se usa un directorio de prueba ficticio (DNI `12345678`, RUC `20123456789`); no se consultan páginas externas. El endpoint prepara datos, no emite comprobantes tributarios. Ver `CORRECCION_CAJERO_MODALIDADES_COMPROBANTE.md`.

Las pruebas unitarias nuevas están en `microservice/cashier-service/src/test/java/pe/upeu/nullpointerpos/caja/ServicioDatosComprobanteTest.java`; quedan pendientes de ejecución en un equipo con Maven disponible.


### S3-T-64 — PDF local de demostración

`POST /api/v1/comprobantes/demostracion/pdf` consulta el total del pedido en `servicio-pedidos` y genera un PDF de una página marcado `SIMULACIÓN ACADÉMICA - SIN VALOR TRIBUTARIO`. Implementación: `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/service/GeneradorPdfComprobanteDemostracion.java`; prueba: `microservice/cashier-service/src/test/java/pe/upeu/nullpointerpos/caja/GeneradorPdfComprobanteDemostracionTest.java`. El PDF se probó aisladamente con Java 21 y `pdfinfo`; el build Spring completo sigue pendiente de Maven.
