# NullPointer POS - Wendy's Food

Backend académico para los tres Sprints del proyecto **NullPointer POS – Sistema de Gestión de Pedidos y Caja**, de la UPeU, Célula G5. Está organizado como **Maven multimódulo y microservicios independientes**; no es una aplicación monolítica. Los servicios de negocio tienen responsabilidades y bases de datos separadas.

> **Estado de validación (24/11/2026):** en la PC de desarrollo se reportó `BUILD SUCCESS` de `mvnw.cmd clean verify` para los 10 módulos y 33 pruebas sin fallos. Una construcción Docker en paralelo sufrió descargas incompletas de Maven Central; construir las imágenes una por una y reintentar los fallos resolvió el arranque en esa PC. En una PC nueva debe tenerse conexión a Internet durante la primera construcción. La construcción se hace manualmente desde PowerShell. El éxito de compilación no sustituye las pruebas funcionales de cada flujo.

## Arquitectura de microservicios

| Componente | Puerto local | Responsabilidad | Base de datos |
|---|---:|---|---|
| `api-gateway` | 8080 | Entrada HTTP, rutas y validación JWT | No aplica |
| `authentication-service` | 8081 | Login, JWT, roles, administración de cuentas | `bd_autenticacion` |
| `orders-service` | 8082 | Mesas, comandas, estado de platos, descuentos, auditoría y reportes | `bd_comandas` |
| `kitchen-service` | 8083 | Consumir eventos de RabbitMQ y difundirlos por WebSocket/STOMP | Sin BD de negocio |
| `products-service` | 8084 | Cuatro áreas de negocio, CRUD de categorías/productos, opciones configurables, estaciones, disponibilidad y auditoría | `bd_menu` |
| `cashier-service` | 8085 | Apertura/cierre de caja, pagos en efectivo/digital/mixto, clientes y tickets | `bd_caja` |
| `inventory-service` | 8086 | Insumos, recetas, compras, mermas y alertas de stock | `bd_inventario` |
| `config-server` | 8888 | Configuración centralizada (perfil native) | No aplica |
| `eureka-server` | 8761 | Registro y descubrimiento de servicios | No aplica |
| PostgreSQL | 5432 | Motor relacional; una BD por servicio dueño de datos | 6 bases independientes |
| RabbitMQ | 5672 / 15672 | Eventos asíncronos / consola web | Volumen propio |

**Regla de propiedad de datos:** cada microservicio escribe en su propia base. `cashier-service` consulta el total de un pedido mediante el contrato HTTP de `orders-service` y publica `pago.confirmado`; no escribe directamente en `bd_comandas`. `inventory-service` recibe `pedido.pagado` mediante RabbitMQ; no lee `bd_comandas`.

### Flujos importantes

1. **Comanda a cocina:** `orders-service` guarda pedido + evento outbox en una transacción; el publicador envía a RabbitMQ (`nullpointer.eventos`, clave `pedido.enviado.cocina`); `kitchen-service` valida el contrato y publica por STOMP en `/topic/cocina`. El endpoint WebSocket pasa por el gateway: `/ws/cocina`.
2. **Pago a inventario:** `cashier-service` guarda pago + partes + outbox en una transacción; publica `pago.confirmado`; `orders-service` valida el importe y cambia el pedido a `PAGADO` en su propia BD; luego emite `pedido.pagado`; `inventory-service` deduplica por ID de evento y descuenta insumos según la receta configurada.
3. **Producto agotado:** `products-service` actualiza disponibilidad, auditoría y outbox en la misma base; el publicador emite `producto.disponibilidad.actualizada`; `kitchen-service` la expone en `/topic/cocina/disponibilidad`. La suscripción desde `shared`/`androidApp` sigue fuera de este ZIP de backend.

## Requisitos

- Docker Desktop y Docker Compose v2.
- Internet la primera vez para descargar imágenes de PostgreSQL, RabbitMQ, Maven y dependencias Java.
- Para compilar localmente: Java 21 y Maven, o ejecutar el wrapper si puede descargar Maven.

## Inicio en Windows (recomendado para una PC nueva)

1. Instala Docker Desktop y comprueba que Windows permite ejecutar contenedores Linux. Mantén conexión a Internet durante la primera construcción.
2. Descomprime el ZIP completo en una carpeta local.
3. Abre PowerShell en la raíz del proyecto y ejecuta `docker compose build` y después `docker compose up -d`.
4. El lanzador comprueba Docker, construye las nueve imágenes de microservicios **una por una**, reintenta una vez una construcción que falle y, si todo sale bien, ejecuta `docker compose up -d` y muestra el estado.

En la primera ejecución puede tardar porque descarga imágenes y dependencias Maven. En siguientes ejecuciones Docker reutiliza las capas ya descargadas. Si falla un servicio después de dos intentos, revisa la conexión y vuelve a abrir el archivo; no se borran las bases ni los volúmenes.

Para detener los contenedores sin borrar datos, ejecuta `utilidades\detener-backend.bat`. **No uses `docker compose down -v`** salvo que realmente quieras eliminar todos los datos locales.

Si tienes que diagnosticar un error, abre PowerShell en la raíz y usa `docker compose ps -a` y `docker compose logs --tail=150 servicio-pedidos`.

### Direcciones locales

- Gateway: `http://localhost:8080`
- Salud global: `GET http://localhost:8080/api/v1/salud`
- Eureka: `http://localhost:8761`
- Config Server: `http://localhost:8888/actuator/health`
- RabbitMQ Management: `http://localhost:15672`
- WebSocket/STOMP: `ws://localhost:8080/ws/cocina`; tópico de pedidos `/topic/cocina`; eventos de disponibilidad `/topic/cocina/disponibilidad`

La aplicación móvil o Postman deben utilizar el **gateway en el puerto 8080**; no deben depender de los puertos internos de cada servicio.

## Cuentas de demostración

En una base recién inicializada, `authentication-service` genera usuarios de prueba con clave `1234`:

| Código | Rol |
|---|---|
| `ADM001` | Administrador |
| `MOZ001` / `MOZ002` | Mozo |
| `COC001` | Cocinero |
| `CAJ001` | Cajero |
| `ALM001` | Almacenero |

Estas cuentas y contraseñas son exclusivamente para el entorno académico local. Cambia o elimina estas credenciales antes de cualquier exposición pública.

**Login:** `POST http://localhost:8080/api/v1/autenticacion/iniciar-sesion`

```json
{ "codigoTrabajador": "MOZ001", "clave": "1234" }
```

Usa la respuesta `token` en las demás solicitudes: `Authorization: Bearer <token>`. El token de cada rol solo permite sus rutas según la configuración de seguridad de cada servicio.

## Principales endpoints

| Sprint / HU | Método y endpoint | Servicio dueño |
|---|---|---|
| HU-01 | `POST /api/v1/autenticacion/iniciar-sesion` | authentication |
| HU-26 | `GET/POST/PUT /api/v1/administracion/usuarios`; `PATCH /api/v1/administracion/usuarios/{id}/activar`; `PATCH /api/v1/administracion/usuarios/{id}/desactivar`; `GET /auditoria` | authentication |
| HU-02 | `GET /api/v1/mesas`; `PUT /api/v1/mesas/trasladar` | orders |
| HU-03 | `GET /api/v1/menu/areas`; `GET /api/v1/menu/categorias?area=POLLERIA`; `GET /api/v1/menu/productos?area=POLLERIA` | products |
| HU-04/05/07 | `POST /api/v1/pedidos`; `PUT /api/v1/pedidos/{idPedido}/detalles`; `POST /api/v1/pedidos/{idPedido}/enviar-cocina` | orders |
| HU-08/09/11 | `GET /api/v1/cocina/pedidos` | orders |
| HU-10 | `PATCH /api/v1/pedidos/detalle/{id}` | orders |
| HU-12 | `PATCH /api/v1/menu/disponibilidad`; `GET /api/v1/menu/auditoria-disponibilidad` | products |
| HU-13 | `POST /api/v1/caja/apertura`; `GET /api/v1/caja/turno-abierto` | cashier |
| HU-14/15/16 | `POST /api/v1/caja/cobrar`; `POST /api/v1/caja/cobrar-digital`; `POST /api/v1/caja/cobrar-mixto` | cashier |
| HU-18 | `GET /api/v1/clientes/{tipoDocumento}/{numeroDocumento}`; `PUT /api/v1/clientes` | cashier |
| HU-19 | `POST /api/v1/tickets/whatsapp`; `POST /api/v1/tickets/{idTicket}/reintentar` | cashier |
| HU-20 | `POST /api/v1/caja/cierre` | cashier |
| HU-21..25 | `/api/v1/inventario/insumos`, `/recetas/{idProducto}`, `/compras`, `/mermas`, `/alertas`, `/movimientos` | inventory |
| HU-17/29 | `PATCH /api/v1/pedidos/{idPedido}/descuento`; `POST /api/v1/pedidos/{idPedido}/anular`; `GET /api/v1/auditoria/pedidos` | orders |
| HU-27/28/30 | `GET /api/v1/reportes/ventas-por-hora`; `GET /api/v1/reportes/platos-populares`; `GET /api/v1/reportes/ventas/exportar.xlsx` | orders |

### Administración del catálogo, mesas y personal

El catálogo se organiza por cuatro áreas: `POLLERIA`, `MENU`, `HELADERIA` y `JUGUERIA`. No son cuatro microservicios separados; son áreas configurables dentro de `products-service`, con categorías y productos en el catálogo único. El administrador puede crear, modificar, desactivar y restaurar productos/categorías; administrar estaciones y consultar auditoría. Las bajas son lógicas para conservar el historial de pedidos y ventas.

- Catálogo: `/api/v1/menu/administracion/**` (solo ADMINISTRADOR). Incluye mantenimiento de productos/categorías/estaciones y opciones por producto.
- Variantes: `GET /api/v1/menu/productos/{idProducto}/opciones` consulta tamaños/sabores/extras; `POST /api/v1/menu/productos/{idProducto}/opciones/validar` comprueba selección y calcula el recargo. El pedido guarda precio y selección como snapshot.
- Usuarios: `PUT /api/v1/administracion/usuarios/{id}` permite modificar nombre, correo, rol y restablecer PIN; `GET /api/v1/administracion/usuarios/auditoria` consulta cambios.
- Mesas: `/api/v1/mesas/administracion/**` permite crear, editar capacidad/zona, desactivar/restaurar y marcar fuera de servicio. No permite cambios con pedidos activos.
- Insumos: editar datos maestros, activar/desactivar y auditar está reservado al administrador; almacenero y administrador pueden operar compras, mermas y ajustes de stock con movimientos registrados.

La guía detallada de rutas, cuerpos JSON, permisos, migraciones y limitaciones está en `documentacion/ADMINISTRACION_COMPLETA_Y_AREAS_NEGOCIO.md`.

La colección principal de Postman es `postman/NullPointer_POS.postman_collection.json`. Está organizada estrictamente en cuatro niveles: **NullPointer POS → rol/área → módulo → endpoint**. Importa también `postman/NullPointer_POS_Local.postman_environment.json` si deseas el `baseUrl` local. En cada carpeta de rol ejecuta primero “Iniciar sesión”; el test guarda automáticamente el token de ese rol. Para escenarios con pedido y pago, crea y envía la comanda primero, consulta el total real y abre un turno de caja antes de cobrar. La colección `NullPointer_POS_Sprint1.postman_collection.json` es solo una colección auxiliar antigua para Sprint 1; para las pruebas completas usa la colección principal.

## Configuración opcional para enviar tickets por WhatsApp

La función HU-19 siempre registra el ticket localmente. Para el envío real desde WhatsApp Business Cloud API configura en `.env`:

```dotenv
WHATSAPP_API_URL=https://graph.facebook.com/<VERSION>/<PHONE_NUMBER_ID>/messages
WHATSAPP_ACCESS_TOKEN=<TOKEN_DE_ACCESO>
```

La versión y `PHONE_NUMBER_ID` deben corresponder a la cuenta de Meta del equipo. Sin ambas variables el API responde `PENDIENTE_CONFIGURACION`; con ellas, registra `ENVIADO` si el proveedor acepta la petición o `ERROR_ENVIO` si falla. Una respuesta exitosa del proveedor indica aceptación, no garantiza lectura del mensaje por el cliente.

## Compilación y pruebas

Desde Windows:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

O desde la raíz con Maven instalado:

```bash
mvn -B -ntp clean verify
```

Se incluyen pruebas unitarias de JWT, umbrales de demora, validación del contrato WebSocket, aritmética de caja y consumo por receta. Estas no reemplazan las pruebas de integración con PostgreSQL, RabbitMQ, WebSocket y gateway; esas pruebas deben ejecutarse tras levantar Docker.

## Límites conocidos que no deben ocultarse en la exposición

- El ZIP es el **backend**. No contiene `shared/` ni `androidApp/`; por ello, pantallas KDS, temporizadores visuales, avisos sonoros, filtros reactivos, formularios de cobro y otras tareas explícitamente móviles no están implementadas aquí.
- La consulta DNI/RUC busca en el registro local `bd_caja`; no consulta automáticamente RENIEC ni SUNAT hasta integrar un proveedor externo y sus credenciales.
- WhatsApp es configurable; el envío real requiere el endpoint, token y número de teléfono oficial de Meta.
- El cajón de dinero USB/serie (T-38) es opcional y requiere el modelo físico/protocolo del equipo; no está incluido.
- Los descuentos por porcentaje y el registro de auditoría sí están en el backend; un motor completo de promociones por calendario/cupón no está implementado.
- Las opciones de producto admiten grupos, límites y recargos; las reglas comerciales condicionales avanzadas y recetas distintas según variante deben definirse con la carta real.
- La compilación completa y las pruebas unitarias se reportaron exitosas en la PC de desarrollo. El lanzador automatiza la construcción secuencial que evitó las descargas incompletas durante el build paralelo. Aun así, tras instalarlo en otra PC hay que revisar `docker compose ps -a` y validar los flujos entre PostgreSQL, RabbitMQ, los servicios y el gateway.

## Estructura del repositorio

```text
infrastructure/
  api-gateway/
  config-server/
  eureka-server/
microservice/
  authentication-service/
  products-service/
  orders-service/
  kitchen-service/
  cashier-service/
  inventory-service/
base-de-datos/
postman/
documentacion/
```

Los comentarios `START`/`END` junto al código identifican las HU y tareas relacionadas. Consulta `documentacion/INDICE_TAREAS_EN_CODIGO.md` para ubicar los segmentos y `documentacion/GUIA_TAREAS_SPRINTS_1_2_3_BACKEND.md` para los mapas completos de implementación y exposición.


## Archivos rojos en orders-service

Abre la carpeta raíz del Maven multimódulo (donde están `pom.xml` y `docker-compose.yml`), selecciona Java 21 y recarga los proyectos Maven. Ejecuta primero `.\mvnw.cmd -B -ntp -pl microservice/orders-service -am clean verify`. Guía completa: `documentacion/COMO_IMPORTAR_Y_PROBAR_ORDERS_SERVICE.md`.

Para revisar faltantes de stock registrados cuando la receta requiere más insumo del disponible, usa `GET /api/v1/inventario/faltantes-venta` (rol ALMACENERO o ADMINISTRADOR). La cantidad en movimientos refleja solo el stock físicamente descontado.


## Nombres de servicios en español

Los nombres registrados en Eureka y los servicios de `docker-compose.yml` están en español: `servicio-autenticacion`, `servicio-productos`, `servicio-pedidos`, `servicio-cocina`, `servicio-caja`, `servicio-inventario`, `servidor-configuracion`, `servidor-descubrimiento` y `puerta-enlace-api`. Las rutas de carpetas Maven se mantienen como están para no romper los Dockerfiles ni los comandos de compilación.

Para validar el backend localmente: `mvnw.cmd -B -ntp clean verify`. Para construir e iniciar con Docker en Windows, ejecuta `docker compose build` y después `docker compose up -d` desde la raíz.

## Modalidades locales de datos para comprobantes (Sprint 3)

La versión de revisión incluye `POST /api/v1/comprobantes/datos-cliente/validar`, para probar antes de integrar proveedores externos las modalidades `SIN_DATOS`, `MANUAL`, `DNI` y `RUC`. El detalle está en `documentacion/CORRECCION_CAJERO_MODALIDADES_COMPROBANTE.md` y en la colección Postman.

Los documentos `12345678` y `20123456789` son ejemplos ficticios, no consultas reales a RENIEC/SUNAT. Esta API prepara datos, pero todavía no emite una boleta/factura tributaria real ni envía un PDF por WhatsApp.

La misma revisión añade `POST /api/v1/comprobantes/demostracion/pdf`, que genera una vista previa PDF a partir de un pedido existente y del total consultado al servicio-pedidos. El archivo lleva la marca `SIMULACIÓN ACADÉMICA – SIN VALOR TRIBUTARIO`. El envío automático del PDF por WhatsApp sigue pendiente de integración oficial.


## Corrección de prueba DNI/RUC (24/11/2026)

Se corrigió el tratamiento de consultas locales sin coincidencia: un mapa vacío o sin `nombreCompleto` ya no se interpreta como cliente encontrado y se permite consultar el directorio ficticio. La guía y el informe de revisión registran el cambio. Para comprobarlo en Windows, ejecuta primero:

```powershell
.\mvnw.cmd -B -ntp -pl microservice/cashier-service -am -Dtest=ServicioDatosComprobanteTest -Dsurefire.failIfNoSpecifiedTests=false test
.\mvnw.cmd -B -ntp clean verify
```
