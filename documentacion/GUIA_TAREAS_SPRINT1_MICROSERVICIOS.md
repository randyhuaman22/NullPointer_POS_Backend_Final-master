# Guía de tareas del Sprint 1 — Arquitectura de microservicios

**Proyecto:** NullPointer POS · UPeU · Célula G5 · Wendy’s Food 2026  
**Alcance de esta entrega:** `infrastructure/api-gateway`, `microservice/authentication-service`, `microservice/orders-service` y `microservice/kitchen-service`. Los cuatro son proyectos Spring Boot y contenedores independientes.  
**Importante:** la app KMP y el frontend web no están implementados en este ZIP. Cada tarea de UI incluye un espacio para indicar y adjuntar la evidencia correspondiente.

## Cómo leer el estado
- **Implementado:** existe código en el backend, pero aún debe ejecutarse en tu PC para adjuntar evidencia.
- **Parcial:** parte backend lista; falta una parte de app/pruebas o la validación final.
- **Pendiente en este ZIP:** corresponde a la aplicación KMP o al frontend web, fuera del backend entregado.

## Mapa de microservicios
- `infrastructure/api-gateway/`: puerto 8080, entrada única y validación JWT.
- `microservice/authentication-service/`: puerto 8081, login, roles y administración de usuarios; base `bd_autenticacion`.
- `microservice/orders-service/`: puerto 8082, mesas, catálogo y pedidos; base `bd_comandas`.
- `microservice/kitchen-service/`: puerto 8083, consume RabbitMQ y emite STOMP `/topic/cocina`; la KDS completa corresponde al Sprint 2.
- `bd_cocina` se crea como base reservada para Sprint 2.

## T-01 — Diseñar módulos base KMP (shared, androidApp, desktopApp)
- **Pertenece a:** App móvil / arquitectura cliente
- **Ubicación:** `App móvil KMP; no pertenece a Spring Boot.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** La app KMP completa no está incluida.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: estructura shared / androidApp / desktopApp]

## T-02 — Configurar Koin y Ktor Client en la capa compartida
- **Pertenece a:** App móvil / integración API
- **Ubicación:** `App móvil: shared/ (módulos Koin y cliente Ktor).`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** Debe consumir el Gateway en lugar de llamar a puertos internos.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: módulos Koin/Ktor y base URL]

## T-03 — Crear esquemas relacionales de autenticación y POS
- **Pertenece a:** Backend / datos
- **Ubicación:** `microservice/authentication-service/src/main/resources/db/migration/V1__crear_esquema_autenticacion.sql; microservice/orders-service/src/main/resources/db/migration/V1__crear_esquema_comandas.sql`
- **Estado:** Implementado en código; requiere ejecución local
- **Qué hace / qué falta:** Flyway crea esquema_autenticacion dentro de bd_autenticacion y esquema_comandas dentro de bd_comandas. bd_cocina queda reservada para Sprint 2.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: logs Flyway y bases bd_autenticacion/bd_comandas]

## T-04 — Configurar HikariCP y variables de entorno
- **Pertenece a:** Backend / configuración
- **Ubicación:** `microservice/authentication-service/src/main/resources/application.yml; microservice/orders-service/src/main/resources/application.yml; docker-compose.yml; .env.example`
- **Estado:** Implementado en código; requiere ejecución local
- **Qué hace / qué falta:** Cada servicio configura su pool y lee URL_BASE_DATOS, USUARIO_BASE_DATOS, CLAVE_BASE_DATOS y SECRETO_JWT desde el entorno.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: docker compose ps / variables sin mostrar secretos]

## T-05 — Implementar generación y firma JWT
- **Pertenece a:** Backend / microservice/authentication-service
- **Ubicación:** `microservice/authentication-service/src/main/java/pe/upeu/nullpointerpos/autenticacion/security/ServicioJwt.java`
- **Estado:** Implementado
- **Qué hace / qué falta:** Firma tokens con `idUsuario`, `codigoTrabajador`, `nombreCompleto`, `rol` y expiración.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: respuesta JSON de login con token censurado]

## T-06 — Configurar filtro de JWT para cabeceras HTTP
- **Pertenece a:** Backend / Gateway y servicios
- **Ubicación:** `infrastructure/api-gateway/src/main/java/pe/upeu/nullpointerpos/puertaenlace/security/FiltroJwtPuertaEnlace.java; microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/security/FiltroAutenticacionJwt.java; microservice/authentication-service/src/main/java/pe/upeu/nullpointerpos/autenticacion/security/FiltroAutenticacionJwt.java`
- **Estado:** Implementado en código; requiere ejecución local
- **Qué hace / qué falta:** Gateway valida firma/expiración; los servicios vuelven a validar el token. Comanda autoriza usando el rol incluido en el JWT.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: 401 sin token y 200 con token]

## T-07 — Pruebas unitarias de BCrypt y sesiones/tokens
- **Pertenece a:** Backend / pruebas
- **Ubicación:** `microservice/authentication-service/src/test/java/pe/upeu/nullpointerpos/autenticacion/PruebaServicioJwt.java; microservice/orders-service/src/test/java/pe/upeu/nullpointerpos/pedidos/PruebaServicioJwt.java`
- **Estado:** Pruebas JWT y BCrypt escritas; ejecución pendiente
- **Qué hace / qué falta:** Hay pruebas para emisión/lectura/expiración JWT en autenticación y validación JWT en comandas, además de BCrypt. Ejecuta `mvn test` en tu PC.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: salida mvn test]

## T-08 — Maquetar pantalla de Login con PIN de 4 dígitos
- **Pertenece a:** App móvil / UI
- **Ubicación:** `App móvil KMP: pantalla de login Compose.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** El backend exige PIN de cuatro dígitos en SolicitudInicioSesion.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: Login con teclado PIN]

## T-09 — Programar AuthViewModel y estados reactivos
- **Pertenece a:** App móvil / MVVM
- **Ubicación:** `App móvil KMP: AuthViewModel, UiState y repositorio de autenticación.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** Debe llamar POST /api/v1/autenticacion/iniciar-sesion y manejar carga, éxito y error.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: estados de login en app]

## T-10 — Endpoint REST POST /api/v1/autenticacion/iniciar-sesion
- **Pertenece a:** Backend / microservice/authentication-service
- **Ubicación:** `microservice/authentication-service/src/main/java/pe/upeu/nullpointerpos/autenticacion/controller/ControladorAutenticacion.java; service/ServicioAutenticacion.java; service/impl/ServicioAutenticacionImpl.java; dto/request/SolicitudInicioSesion.java`
- **Estado:** Implementado en código; requiere ejecución local
- **Qué hace / qué falta:** Valida código y PIN, BCrypt y devuelve JWT con el rol.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: Postman login exitoso y PIN incorrecto]

## T-11 — Grid interactivo del plano de mesas
- **Pertenece a:** App móvil / UI
- **Ubicación:** `App móvil KMP: pantalla Salón / Grid de mesas.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** Consume GET /api/v1/mesas.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: cuadrícula de mesas]

## T-12 — Servicio REST GET /api/v1/mesas
- **Pertenece a:** Backend / microservice/orders-service
- **Ubicación:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorMesas.java; service/ServicioMesas.java; service/impl/ServicioMesasImpl.java; dto/response/RespuestaMesa.java`
- **Estado:** Implementado en código; requiere ejecución local
- **Qué hace / qué falta:** Devuelve mesas activas y estado libre/ocupada.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: JSON de mesas en Postman]

## T-13 — Renderizado dinámico verde/rojo por estado de mesa
- **Pertenece a:** App móvil / UI
- **Ubicación:** `App móvil KMP: componente MesaCard / pantalla Salón.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** La API entrega estado; los colores los decide la app.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: mesa libre verde y ocupada roja]

## T-14 — Validar clic sobre mesas libres
- **Pertenece a:** App móvil + backend
- **Ubicación:** `App móvil KMP: interacción de selección de mesa; backend valida disponibilidad al crear comanda.`
- **Estado:** Parcial: validación backend implementada
- **Qué hace / qué falta:** microservice/orders-service bloquea la creación si la mesa no está LIBRE y usa transacción/bloqueo de fila.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: seleccionar mesa libre / mensaje para ocupada]

## T-15 — Diseñar pestañas de navegación por categorías
- **Pertenece a:** App móvil / UI
- **Ubicación:** `App móvil KMP: menú / Tabs de categorías.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** Consume GET /api/v1/menu/categorias.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: pestañas Pollos/Bebidas/Combos]

## T-16 — Consumir API de catálogo y caché local
- **Pertenece a:** Backend + app móvil
- **Ubicación:** `microservice/products-service/src/main/java/pe/upeu/nullpointerpos/productos/controller/ControladorMenu.java; service/ServicioMenu.java; service/impl/ServicioMenuImpl.java; la caché pertenece a shared/ de KMP.`
- **Estado:** API implementada; caché móvil pendiente
- **Qué hace / qué falta:** Backend expone categorías y productos; la caché local no forma parte del backend.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: catálogo cargado y evidencia de caché]

## T-17 — Tarjeta ProductoCard con foto y precio
- **Pertenece a:** App móvil / UI
- **Ubicación:** `App móvil KMP: componente ProductoCard.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** La API devuelve nombre, imagen, precio, categoría y disponibilidad.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: tarjeta de producto]

## T-18 — Dialog de notas por plato
- **Pertenece a:** App móvil / UI
- **Ubicación:** `App móvil KMP: NotesDialog / NotasDialog.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** La API acepta observaciones independientes por cada producto del pedido.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: diálogo “Sin crema”]

## T-19 — Asociar nota al detalle de comanda
- **Pertenece a:** Backend + app móvil
- **Ubicación:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/dto/request/SolicitudDetallePedido.java; service/ServicioPedidos.java; service/impl/ServicioPedidosImpl.java; esquema_comandas.detalles_pedido.observaciones; cliente KMP debe enlazar el estado del diálogo al item.`
- **Estado:** Persistencia backend implementada; enlace UI pendiente
- **Qué hace / qué falta:** Cada plato guarda su nota independiente.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: nota asociada al plato en la comanda]

## T-20 — Botones de incremento, decremento y eliminación
- **Pertenece a:** App móvil / UI
- **Ubicación:** `App móvil KMP: lista de detalles de comanda.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** El backend acepta cantidades mayores que cero; enviar lista actualizada a PUT /api/v1/pedidos/{id}/detalles.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: + / - / eliminar]

## T-21 — Actualizar subtotal/total en tiempo real
- **Pertenece a:** App móvil + backend
- **Ubicación:** La app móvil calcula una vista previa; el backend recalcula los totales en `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/service/impl/ServicioPedidosImpl.java`.
- **Estado:** Backend implementado; vista previa UI pendiente
- **Qué hace / qué falta:** El servidor recalcula subtotal y total desde precio/cantidad guardados, no confía en totales del cliente.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: total cambia al editar cantidades]

## T-22 — Opción Reasignar/Mover mesa
- **Pertenece a:** App móvil / UI
- **Ubicación:** `App móvil KMP: menú de acciones de comanda activa.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** Invoca PUT /api/v1/mesas/trasladar.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: opción mover mesa]

## T-23 — Modal con mesas libres disponibles
- **Pertenece a:** App móvil / UI
- **Ubicación:** `App móvil KMP: selector modal de mesas.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** Debe listar mesas y filtrar estado LIBRE; backend vuelve a validar el destino.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: modal de mesas libres]

## T-24 — Servicio backend PUT /api/v1/mesas/trasladar
- **Pertenece a:** Backend / microservice/orders-service
- **Ubicación:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorTrasladoMesas.java; service/impl/ServicioPedidosImpl.java; esquema_comandas.traslados_mesa`
- **Estado:** Implementado en código; requiere ejecución local
- **Qué hace / qué falta:** Valida propiedad, destino libre y registra historial de traslado.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: respuesta de traslado y estado de ambas mesas]

## T-25 — Botón flotante Enviar a Cocina
- **Pertenece a:** App móvil / UI
- **Ubicación:** `App móvil KMP: pantalla de edición de comanda.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** Invoca POST /api/v1/pedidos/{id}/enviar-cocina.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: botón Enviar a Cocina]

## T-26 — Validar que haya al menos un plato
- **Pertenece a:** App móvil + backend
- **Ubicación:** `Frontend: deshabilitar botón sin platos; microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/service/impl/ServicioPedidosImpl.java`
- **Estado:** Validación backend implementada; UI pendiente
- **Qué hace / qué falta:** Backend rechaza el envío de una comanda vacía.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: botón deshabilitado cuando está vacía]

## T-27 — DTO para pedido y lista de detalles
- **Pertenece a:** Backend / DTO
- **Ubicación:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/dto/request/SolicitudCrearPedido.java; dto/request/SolicitudDetallePedido.java; dto/response/RespuestaPedido.java; RespuestaDetallePedido.java`
- **Estado:** Implementado
- **Qué hace / qué falta:** Define la estructura de entrada/salida de la comanda y sus detalles.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: JSON de petición y respuesta]

## T-28 — POST /api/v1/pedidos con transacción PostgreSQL
- **Pertenece a:** Backend / microservice/orders-service
- **Ubicación:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorPedidos.java; microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/service/impl/ServicioPedidosImpl.java`
- **Estado:** Implementado en código; requiere ejecución local
- **Qué hace / qué falta:** Crea cabecera y detalles dentro de una transacción y recalcula totales.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: comanda creada y filas guardadas en bd_comandas]

## T-29 — Emitir evento hacia cocina por tiempo real
- **Pertenece a:** Backend / integración RabbitMQ + WebSocket
- **Ubicación:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/service/impl/PublicadorEventosPendientes.java; repository/EventoPendienteRepositorio.java; microservice/kitchen-service/src/main/java/pe/upeu/nullpointerpos/cocina/messaging/ReceptorEventosCocina.java; config/ConfiguracionSocketCocina.java`
- **Estado:** Implementado en código; requiere ejecución local
- **Qué hace / qué falta:** La transacción guarda evento outbox; el publicador lo envía a RabbitMQ; microservice/kitchen-service lo publica a /topic/cocina.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: logs RabbitMQ y cliente STOMP recibiendo evento]

## T-30 — Actualizar estado visual de mesa a Ocupada
- **Pertenece a:** Backend + app móvil
- **Ubicación:** `microservice/orders-service: trigger func_sincronizar_estado_mesa en migración; app KMP: refrescar/observar cambios.`
- **Estado:** Trigger backend implementado; sincronización visual app pendiente
- **Qué hace / qué falta:** La base actualiza estado; la UI debe refrescar mesas o suscribirse a un canal de eventos.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: mesa cambia de libre a ocupada en la app]

## T-31 — Formulario web administrativo para colaboradores
- **Pertenece a:** Frontend web
- **Ubicación:** `Frontend web: formulario de usuarios.`
- **Estado:** Pendiente en este ZIP
- **Qué hace / qué falta:** Debe llamar endpoints de microservice/authentication-service bajo /api/v1/administracion/usuarios.
- **Evidencia / pantalla:** [ESPACIO PARA CAPTURA: formulario administrativo web]

## T-32 — REST para crear/listar/desactivar usuarios
- **Pertenece a:** Backend / microservice/authentication-service
- **Ubicación:** `microservice/authentication-service/src/main/java/pe/upeu/nullpointerpos/autenticacion/controller/ControladorUsuarios.java; microservice/authentication-service/src/main/java/pe/upeu/nullpointerpos/autenticacion/service/impl/ServicioUsuariosImpl.java; dto/request/SolicitudCrearUsuario.java; dto/response/RespuestaUsuario.java`
- **Estado:** Implementado en código; requiere ejecución local
- **Qué hace / qué falta:** CRUD Sprint 1: listar, crear, desactivar y reactivar cuentas. Protege la cuenta del administrador que inició sesión y evita desactivar al último administrador activo.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: Postman crear/listar/desactivar usuario]

## T-33 — Asignar roles de seguridad
- **Pertenece a:** Backend / seguridad
- **Ubicación:** `microservice/authentication-service/src/main/java/pe/upeu/nullpointerpos/autenticacion/security/ServicioJwt.java; microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/config/ConfiguracionSeguridad.java; infrastructure/api-gateway/src/main/java/pe/upeu/nullpointerpos/puertaenlace/security/FiltroJwtPuertaEnlace.java`
- **Estado:** Implementado en código; requiere ejecución local
- **Qué hace / qué falta:** Roles en JWT y reglas por endpoint para MOZO, COCINERO y ADMINISTRADOR.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: token por rol y respuesta 403 al rol incorrecto]

## T-34 — Pruebas de acceso restringido por roles
- **Pertenece a:** Pruebas backend / Postman
- **Ubicación:** `postman/NullPointer_POS.postman_collection.json; microservice/authentication-service/src/main/java/pe/upeu/nullpointerpos/autenticacion/config/ConfiguracionSeguridad.java; microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/config/ConfiguracionSeguridad.java`
- **Estado:** Parcial: colección disponible; ejecución manual pendiente
- **Qué hace / qué falta:** Ejecutar con tokens de MOZO, ADMINISTRADOR y COCINERO; no se ha verificado en Docker/Maven en este entorno.
- **Evidencia / pantalla:** [ESPACIO PARA EVIDENCIA: resultados Postman 200/401/403]

## Historias de usuario cubiertas
- **HU-01 Login:** `microservice/authentication-service` (`POST /api/v1/autenticacion/iniciar-sesion`) + JWT y permisos. Pantalla de login pendiente en KMP.
- **HU-02 Mesas:** `microservice/orders-service` (`GET /api/v1/mesas`) + estado calculado en PostgreSQL. Grid y colores pendientes en KMP.
- **HU-03 Menú:** `GET /api/v1/menu/categorias` y `/api/v1/menu/productos`; pestañas y tarjetas pendientes en KMP.
- **HU-04 Notas por plato:** `SolicitudDetallePedido.observaciones` y `esquema_comandas.detalles_pedido.observaciones`; diálogo UI pendiente.
- **HU-05 Editar cantidades/eliminar:** `PUT /api/v1/pedidos/{id}/detalles`, solo en borrador; botones UI pendientes.
- **HU-06 Traslado de mesa:** `PUT /api/v1/mesas/trasladar` y tabla `esquema_comandas.traslados_mesa`; selector UI pendiente.
- **HU-07 Enviar a cocina:** endpoint `POST /api/v1/pedidos/{id}/enviar-cocina`, outbox transaccional, RabbitMQ y WebSocket/STOMP en `microservice/kitchen-service`; KDS completa en Sprint 2.
- **HU-26 Usuarios:** endpoints de `microservice/authentication-service` para crear/listar/desactivar/reactivar; formulario web pendiente.

## Evidencia de ejecución pendiente
En el entorno donde se preparó el ZIP no están instalados Docker ni Maven; por eso la estructura, XML, YAML y JSON se validaron estáticamente, pero no se pudo ejecutar `docker compose up --build` ni `mvn test`. Completa los espacios de evidencia después de probar el proyecto localmente.


## Validación de esta versión del ZIP

Los Dockerfiles de los cuatro microservicios compilan con `mvn package`; no usan `dependency:go-offline`. Se construyen uno por uno para reducir descargas simultáneas y evitar competir por la conexión de Maven Central. La puerta de enlace es la única aplicación expuesta a la computadora anfitriona por el puerto 8080. El servicio de autenticación administra `bd_autenticacion`; el servicio de comandas administra `bd_comandas`. El servicio de cocina consume RabbitMQ y difunde los eventos por WebSocket; su persistencia completa pertenece al Sprint 2.

La colección Postman prueba el flujo completo de HU-01 a HU-07 y HU-26; la petición de edición cambia la cantidad y elimina el plato omitido de la nueva lista. El ejemplo Kotlin con Ktor de `ejemplo-kotlin/EjemploInicioSesionApi.kt` utiliza los mismos nombres de campos y rutas que los controladores Java. El build debe ejecutarse en un equipo con Docker e Internet disponible para descargar las dependencias Maven; esta preparación no se pudo ejecutar con Maven/Docker en el entorno de edición.
