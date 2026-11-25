# Índice de tareas y marcas dentro del código

Este índice acompaña al código para localizar la implementación sin recorrer todo el repositorio. Las marcas `START` y `END` aparecen en comentarios de clases o métodos. Algunos componentes abarcan varias HUs porque son una misma capa técnica; eso no significa que se hayan agrupado historias para el análisis funcional.

## Cómo buscar

En VS Code usa `Ctrl+Shift+F` y busca `HU-07 START`, `T-03 START`, etc. En consola, desde la raíz:

```bash
grep -R "\[T-03\|\[HU-07" -n microservice infrastructure
```

No todas las tareas de interfaz tienen código aquí porque el ZIP no incluye `androidApp/`, `shared/` ni `desktopApp/`. El código del backend contiene marcas solo donde el artefacto existe.

## Sprint 1

| Tarea/HU | Ruta principal | Qué localizar |
|---|---|---|
| S1-T-03/04 | `base-de-datos/*.sh`; `microservice/*/src/main/resources/application.yml` | Bases separadas, credenciales/URL de conexión y pool de conexiones. |
| S1-T-05 | `microservice/authentication-service/src/main/java/pe/upeu/nullpointerpos/autenticacion/security/ServicioJwt.java` | Creación y validación de JWT. |
| S1-T-06 | `microservice/*-service/src/main/java/pe/upeu/nullpointerpos/*/security/FiltroAutenticacionJwt.java`; `infrastructure/api-gateway/.../FiltroJwtPuertaEnlace.java` | Validación de cabecera y autorización. |
| S1-T-07 | `microservice/authentication-service/src/test/java/pe/upeu/nullpointerpos/autenticacion/ServicioJwtTest.java` | Pruebas de token (escritas; no ejecutadas en este entorno). |
| HU-01 / S1-T-10 | `microservice/authentication-service/.../controller/ControladorAutenticacion.java`; `service/impl/ServicioAutenticacionImpl.java` | Login y emisión de token. |
| HU-02 / S1-T-12 | `microservice/orders-service/.../controller/ControladorMesas.java`; `repository/MesaRepositorio.java`; `service/impl/ServicioMesasImpl.java` | Lectura de mesas y ocupación. |
| HU-03 / S1-T-16 | `microservice/products-service/.../controller/ControladorMenu.java`; `service/impl/ServicioMenuImpl.java`; `repository/MenuRepositorio.java` | Categorías, productos y disponibilidad. |
| HU-04/05/07 / S1-T-19/28/29 | `microservice/orders-service/.../controller/ControladorPedidos.java`; `service/impl/ServicioPedidosImpl.java`; `repository/PedidoRepositorio.java`; `service/impl/PublicadorEventosPendientes.java` | Crear/editar/enviar comanda y outbox. |
| HU-06 / S1-T-24 | `microservice/orders-service/.../controller/ControladorTrasladoMesas.java`; `service/impl/ServicioMesasImpl.java` | Traslado de pedido/mesa. Ruta real `PUT /api/v1/mesas/trasladar`. |
| HU-26 / S1-T-32/33 | `microservice/authentication-service/.../controller/ControladorUsuarios.java`; `service/impl/ServicioUsuariosImpl.java` | Crear, listar y activar/desactivar colaboradores. |

## Sprint 2

| Tarea | Ruta principal | Qué localizar |
|---|---|---|
| T-01 | `microservice/kitchen-service/.../config/ConfiguracionSocketCocina.java` | Endpoint WebSocket/STOMP y broker. |
| T-02 / T-05 | `.../dto/event/EventoPedidoCocina.java`; `.../mapper/EventoCocinaMapper.java`; `src/test/.../EventoCocinaMapperTest.java` | Contrato, validación, prueba de mapper. |
| T-03 | `orders-service/.../service/impl/PublicadorEventosPendientes.java`; `kitchen-service/.../messaging/ReceptorEventosCocina.java`; `.../service/impl/ServicioEventosCocinaImpl.java` | Outbox, recepción RabbitMQ y difusión STOMP. |
| T-04, T-06, T-08, T-10, T-12, T-13, T-21–T-24, T-31, T-33, T-39, T-44 | `androidApp/` y `shared/` esperados, ausentes del ZIP | Pendientes frontend (no insertar marcas falsas en backend). |
| T-07 / T-11 | `orders-service/.../controller/ControladorPedidosCocina.java`; `.../dto/response/RespuestaPedidoCocina.java`; `.../service/impl/CalculadoraDemoraPedido.java` | Consulta de pedidos KDS y niveles de demora. |
| T-14 | `orders-service/src/test/java/pe/upeu/nullpointerpos/pedidos/service/impl/CalculadoraDemoraPedidoTest.java` | Pruebas de umbrales escritas, no ejecutadas. |
| T-15–T-19 | `orders-service/.../controller/ControladorEstadoDetalle.java`; `.../dto/request/SolicitudEstadoDetalle.java`; `.../service/impl/ServicioPedidosImpl.java`; `.../repository/PedidoRepositorio.java` | Estado LISTO, fecha de finalización y evento. |
| T-20 / T-25–T-28 | `products-service/.../entity/Producto.java`; `.../controller/ControladorMenu.java`; `.../service/impl/ServicioMenuImpl.java`; `.../repository/MenuRepositorio.java`; `.../service/impl/PublicadorDisponibilidadProducto.java`; `kitchen-service/.../messaging/ReceptorDisponibilidadProducto.java` | Estación, disponibilidad, auditoría, outbox y difusión. |
| T-29–T-32 | `cashier-service/pom.xml`; `Dockerfile`; `.../controller/ControladorCaja.java`; `.../service/impl/ServicioCajaImpl.java`; `.../db/migration/V1__crear_esquema_caja.sql` | Nuevo servicio, apertura de turno y reglas. |
| T-34–T-37 | `cashier-service/.../service/impl/ServicioCajaImpl.java`; `.../dto/request/SolicitudCobroEfectivo.java`; `.../service/impl/PublicadorEventosCaja.java`; `orders-service/.../messaging/ReceptorPagoConfirmado.java` | Vuelto, cobro y propagación de pago. |
| T-40–T-43 | `cashier-service/.../dto/request/SolicitudCobroDigital.java`; `.../service/impl/ServicioCajaImpl.java`; `.../db/migration/V1__crear_esquema_caja.sql` | Yape/Plin, referencia única, evento de pago. |
| T-45–T-48 | `cashier-service/.../dto/request/SolicitudPagoMixto.java`; `PartePago.java`; `.../service/impl/ServicioCajaImpl.java`; `src/test/.../CajaImportesTest.java` | Pago dividido, validación sumatoria y prueba aritmética. |
| T-38 | No implementado | Dispositivo USB/serie requiere especificación de hardware. |

## Sprint 3

| HU | Ruta principal | Qué localizar |
|---|---|---|
| HU-17 | `orders-service/.../controller/ControladorOperacionesPedido.java`; `.../service/impl/ServicioPedidosImpl.java`; `.../repository/PedidoRepositorio.java` | Descuento porcentual + auditoría; promociones complejas pendientes. |
| HU-18 | `cashier-service/.../controller/ControladorClientes.java`; `.../dto/request/SolicitudCliente.java`; `.../repository/RepositorioCaja.java` | Validación y búsqueda DNI/RUC local. |
| HU-19 | `cashier-service/.../controller/ControladorTicketWhatsApp.java`; `.../service/ServicioWhatsApp.java`; `.../repository/RepositorioCaja.java` | Alta ticket/envío configurable y reintento. |
| HU-20 | `cashier-service/.../controller/ControladorCaja.java`; `.../service/impl/ServicioCajaImpl.java`; `.../repository/RepositorioCaja.java` | Cierre y arqueo. |
| HU-21–25 | `inventory-service/.../controller/ControladorInventario.java`; `.../repository/RepositorioInventario.java`; `.../messaging/ReceptorPedidoPagado.java` | Insumos, recetas, descuento por venta, compras, merma y alertas. |
| HU-27/28/30 | `orders-service/.../controller/ControladorReportes.java`; `.../service/impl/ServicioReportes.java`; `.../repository/PedidoRepositorio.java` | Ventas por hora, platos populares, exportación Excel. |
| HU-29 | `orders-service/.../controller/ControladorAuditoria.java`; `.../controller/ControladorOperacionesPedido.java`; `.../repository/PedidoRepositorio.java` | Eventos/auditoría de anulaciones y descuentos. |

## Formato de comentario recomendado

Para nuevos cambios que se hagan después de esta entrega, usa este patrón y reemplaza el ID por una única tarea/HU; no marques como completado código que solo se diseñó:

```java
// [HU-XX / T-XX START] Qué regla de negocio resuelve y por qué está en este microservicio.
// ... implementación ...
// [HU-XX / T-XX END]
```

Cuando una tarea atraviesa varios servicios, coloca una marca local en cada componente y documenta el flujo completo en `GUIA_TAREAS_SPRINTS_1_2_3_BACKEND.md`.


## Sprint 3 — referencias con códigos únicos S3-T-01 a S3-T-59

- **HU-17 / S3-T-01:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/dto/request/SolicitudDescuentoPedido.java`
- **HU-17 / S3-T-02:** `.../orders/controller/ControladorOperacionesPedido.java` y `.../orders/config/ConfiguracionSeguridad.java`
- **HU-17 / S3-T-03:** `.../orders/service/impl/ServicioPedidosImpl.java`, método `aplicarDescuento`
- **HU-17 / S3-T-04:** `.../orders/repository/PedidoRepositorio.java`, método `aplicarDescuento` y auditoría
- **HU-18 / S3-T-06..08:** `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/controller/ControladorClientes.java`; `.../cashier/repository/RepositorioCaja.java`
- **HU-19 / S3-T-11..14:** `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/controller/ControladorTicketWhatsApp.java`; `.../cashier/service/ServicioWhatsApp.java`
- **HU-20 / S3-T-16..19:** `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/service/impl/ServicioCajaImpl.java`; `.../cashier/repository/RepositorioCaja.java`
- **HU-21 / S3-T-21..25:** `microservice/inventory-service/src/main/java/pe/upeu/nullpointerpos/inventario/controller/ControladorInventario.java`; `.../inventory/repository/RepositorioInventario.java`; `.../inventory/dto/request/`
- **HU-22 / S3-T-26..30:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/service/impl/PublicadorEventosPendientes.java`; `microservice/inventory-service/src/main/java/pe/upeu/nullpointerpos/inventario/messaging/ReceptorPedidoPagado.java`; `.../inventory/repository/RepositorioInventario.java`; `microservice/inventory-service/src/main/resources/db/migration/V2__crear_faltantes_venta.sql`
- **HU-23..25 / S3-T-31..42:** `microservice/inventory-service/src/main/java/pe/upeu/nullpointerpos/inventario/controller/ControladorInventario.java`; `.../inventory/repository/RepositorioInventario.java`
- **HU-27..28 / S3-T-43..50:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorReportes.java`; `.../orders/repository/PedidoRepositorio.java`; `.../orders/service/impl/ServicioReportes.java`
- **HU-29 / S3-T-51..54:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorAuditoria.java`; `.../orders/controller/ControladorOperacionesPedido.java`; `.../orders/service/impl/ServicioPedidosImpl.java`; `.../orders/repository/PedidoRepositorio.java`
- **HU-30 / S3-T-55..59:** `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/controller/ControladorReportes.java`; `.../orders/service/impl/ServicioReportes.java`

La lista detallada, con criterios de aceptación y estado de cada tarea, está en `documentacion/SPRINT3_DESGLOSE_TAREAS_S3-T-01_A_S3-T-59.md` y en la hoja `Sprint3_Tareas` del Excel revisado. Los códigos S3-T-xx son una expansión propuesta para Sprint 3; los T-xx existentes de Sprints 1 y 2 no se renumeraron.
