# Anexo técnico — Administración del backend NullPointer POS

**Proyecto:** NullPointer POS — Wendy's Food  
**Actualización:** 24 de noviembre de 2026  
**Alcance:** backend Spring Boot / PostgreSQL / RabbitMQ. Este anexo describe funciones API; no incluye pantallas Android.

## 1. Estructura del catálogo por áreas

Las cuatro áreas de venta se representan en una sola tabla de áreas de negocio y comparten el catálogo de productos. No son cuatro microservicios ni cuatro bases de datos. De esta forma, cada producto conserva una identidad, precio y disponibilidad únicos, y se puede consultar desde el POS, cocina y caja.

| Código API | Área que verá el usuario | Categorías iniciales |
|---|---|---|
| `POLLERIA` | Pollería | Pollos, Guarniciones, Combos |
| `MENU` | Menú | Menú del día, Entradas, Sopas, Segundos |
| `HELADERIA` | Heladería | Helados, Conos y vasos, Toppings |
| `JUGUERIA` | Juguería | Bebidas, Jugos, Extractos, Batidos |

Los nombres son categorías iniciales sugeridas. El administrador puede crear, cambiar, ordenar, desactivar y restaurar categorías. El nombre debe ser único dentro de cada área; por ejemplo, puede existir una categoría «Bebidas» en Juguería y otra con el mismo nombre en Pollería si el negocio lo necesita. Los productos reales, precios, imágenes y recetas deben cargarse con información confirmada por Wendy's Food; no se inventaron precios ni una carta real.

### Consulta para el cliente POS

- `GET /api/v1/menu/areas`
- `GET /api/v1/menu/categorias?area=POLLERIA`
- `GET /api/v1/menu/productos?area=POLLERIA&soloDisponibles=true`
- `GET /api/v1/menu/productos?area=HELADERIA&categoria=Helados&soloDisponibles=true`

Los códigos admitidos son `POLLERIA`, `MENU`, `HELADERIA` y `JUGUERIA`. Si no se envía `area`, las consultas existentes siguen listando todas las áreas para conservar compatibilidad.

## 2. Administración del catálogo (solo ADMINISTRADOR)

Prefijo: `/api/v1/menu/administracion`.

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/categorias?area=POLLERIA&incluirInactivas=true` | Lista categorías, incluido el estado activo. |
| POST | `/categorias` | Crea categoría y la asigna a un área. |
| PUT | `/categorias/{idCategoria}` | Modifica nombre, descripción, área, categoría padre y orden. |
| DELETE | `/categorias/{idCategoria}` | Desactiva lógicamente; no elimina históricos. |
| PATCH | `/categorias/{idCategoria}/restaurar` | Restaura una categoría si el área y su padre siguen activos. |
| GET | `/productos?area=HELADERIA&incluirInactivos=true` | Lista catálogo administrativo con activos e inactivos. |
| POST | `/productos` | Crea un producto. |
| PUT | `/productos/{idProducto}` | Actualiza producto, precio, categoría, imagen URL y estación. |
| GET | `/productos/{idProducto}/opciones` | Consulta tamaños, sabores, presentaciones, toppings y extras configurados. |
| PUT | `/productos/{idProducto}/opciones` | Reemplaza los grupos y las opciones configurables del producto. |
| DELETE | `/productos/{idProducto}` | Desactiva lógicamente; conserva ventas y pedidos. |
| PATCH | `/productos/{idProducto}/restaurar` | Restaura sin habilitar venta automáticamente. |
| GET | `/estaciones` | Lista estaciones de preparación. |
| POST | `/estaciones` | Crea estación. |
| PUT | `/estaciones/{idEstacion}` | Cambia el nombre. |
| DELETE | `/estaciones/{idEstacion}` | Desactiva si no tiene productos activos asignados. |
| PATCH | `/estaciones/{idEstacion}/restaurar` | Restaura estación. |
| GET | `/auditoria?entidad=PRODUCTO&limite=100` | Historial de cambios del catálogo. |

Ejemplo de `POST /categorias`:

```json
{
  "nombre": "Jugos especiales",
  "descripcion": "Combinaciones de frutas",
  "areaNegocio": "JUGUERIA",
  "idCategoriaPadre": null,
  "ordenVisualizacion": 5
}
```

Ejemplo de `POST /productos`:

```json
{
  "codigoProducto": "JUG-001",
  "nombre": "Jugo de papaya",
  "descripcion": "Jugo natural",
  "urlImagen": "https://servidor-de-imagenes/imagen.jpg",
  "precioUnitario": 8.50,
  "idCategoria": 9,
  "idEstacionCocina": 3,
  "tiempoPreparacionMinutos": 5,
  "disponible": true,
  "destacado": false,
  "ordenVisualizacion": 1
}
```

Los IDs del ejemplo son ilustrativos: deben sustituirse por los que devuelva la base real. `urlImagen` almacena una dirección; este backend no es un servidor de carga/almacenamiento de archivos.

Ejemplo de `PUT /productos/{idProducto}/opciones` para un producto de heladería o juguería:

```json
{
  "grupos": [
    {
      "nombre": "Tamaño",
      "obligatorio": true,
      "seleccionMinima": 1,
      "seleccionMaxima": 1,
      "ordenVisualizacion": 1,
      "opciones": [
        { "nombre": "Mediano", "precioAdicional": 0.00, "ordenVisualizacion": 1 },
        { "nombre": "Grande", "precioAdicional": 3.00, "ordenVisualizacion": 2 }
      ]
    },
    {
      "nombre": "Agregados",
      "obligatorio": false,
      "seleccionMinima": 0,
      "seleccionMaxima": 2,
      "ordenVisualizacion": 2,
      "opciones": [
        { "nombre": "Crema", "precioAdicional": 1.50, "ordenVisualizacion": 1 },
        { "nombre": "Topping de chocolate", "precioAdicional": 2.00, "ordenVisualizacion": 2 }
      ]
    }
  ]
}
```

El sistema admite grupos obligatorios u opcionales, límites de selección y hasta 50 opciones por grupo. Para hacer un pedido, el móvil consulta `GET /api/v1/menu/productos/{idProducto}/opciones` y envía en cada detalle la propiedad `opciones`, con `idOpcion` y `cantidad`. El servicio de pedidos pide al catálogo validar esas selecciones; el catálogo verifica pertenencia, vigencia y límites, y devuelve el recargo calculado. El precio recibido del teléfono no se utiliza. Las opciones escogidas se guardan en el texto de observaciones del detalle como snapshot histórico.

### Reglas de integridad

- No se permite asignar un producto a una categoría inexistente o inactiva ni a una estación inactiva.
- No se permite desactivar una categoría con productos o subcategorías activas.
- No se permite desactivar una estación asignada a productos activos.
- Las categorías permiten un nivel de subcategoría para evitar jerarquías circulares.
- Los productos y categorías se desactivan lógicamente. Las comandas conservan su nombre y precio registrados históricamente.
- Restaurar un producto lo deja inicialmente no disponible; el administrador o el cocinero debe habilitarlo de nuevo.
- La disponibilidad sigue generando su evento de outbox para comunicar cambios a cocina.

## 3. Administración de usuarios (solo ADMINISTRADOR)

Prefijo: `/api/v1/administracion/usuarios`.

- `GET /` lista trabajadores.
- `POST /` crea cuenta.
- `PUT /{id}` actualiza nombre, correo y rol; permite restablecer PIN con `nuevaClave` (cuatro dígitos). Enviar `nuevaClave: null` conserva el PIN existente.
- `PATCH /{id}/activar` y `PATCH /{id}/desactivar` cambian el estado sin borrar el historial.
- `GET /auditoria` muestra operaciones administrativas.

Controles adicionales: el administrador no puede cambiar su propio rol ni desactivar su propia cuenta; el sistema impide degradar o desactivar al último administrador activo. Las claves se guardan con BCrypt, no en texto plano.

## 4. Administración de mesas (solo ADMINISTRADOR)

Prefijo: `/api/v1/mesas/administracion`.

- `GET /?incluirInactivas=true` lista mesas activas e inactivas.
- `POST /` crea mesa con código, zona y capacidad.
- `PUT /{idMesa}` cambia código, zona y capacidad.
- `DELETE /{idMesa}` desactiva lógicamente.
- `PATCH /{idMesa}/restaurar` restaura mesa.
- `PATCH /{idMesa}/fuera-servicio` y `PATCH /{idMesa}/habilitar` controlan mantenimiento.

El backend bloquea la fila de la mesa y comprueba que no tenga comandas abiertas antes de editar, desactivar o pasar a fuera de servicio. La ocupación operativa continúa sincronizándose con los pedidos; no se cambia una mesa ocupada a libre manualmente.

## 5. Administración y operación de inventario

El almacenero y administrador pueden operar inventario. La edición de datos maestros, activación/desactivación y consulta de auditoría se restringen al administrador.

- `GET/POST /api/v1/inventario/insumos`
- `GET /api/v1/inventario/insumos/{idInsumo}`
- `PUT /api/v1/inventario/insumos/{idInsumo}` — edición de código, nombre, unidad, stock mínimo y costo; no modifica stock actual.
- `PATCH /api/v1/inventario/insumos/{idInsumo}/desactivar` y `/activar` — bajas lógicas; no permite desactivar insumos utilizados por recetas.
- `POST /api/v1/inventario/insumos/{idInsumo}/ajustes` — establece el stock contado y registra la diferencia con motivo como movimiento `AJUSTE`.
- `GET/PUT /api/v1/inventario/recetas/{idProducto}` — consultar y guardar receta; valida que los insumos existan y estén activos, y reemplaza la lista en una transacción.
- `GET /api/v1/inventario/compras`, `POST /api/v1/inventario/compras`, `POST /api/v1/inventario/mermas`.
- `GET /api/v1/inventario/alertas`, `/movimientos`, `/faltantes-venta` y `/auditoria-insumos`.

Un ajuste de stock no es una compra ni una merma. El sistema conserva el stock anterior, el stock contado, el motivo y el movimiento de diferencia.

## 6. Roles y permisos resumidos

| Rol | Responsabilidades de backend |
|---|---|
| ADMINISTRADOR | Catálogo, áreas/categorías, estaciones, usuarios, mesas, parámetros administrativos de insumos, auditorías, reportes, anulaciones autorizadas y supervisión. |
| MOZO | Consultar menú/mesas, crear y editar comandas propias según reglas del servicio, enviar a cocina y trasladar mesa. |
| COCINERO | Consultar comandas de cocina, marcar detalle listo y cambiar disponibilidad de productos. |
| CAJERO | Apertura/cierre de caja, cobros, consultas de pedido necesarias para cobrar y preparación de datos de comprobante. |
| ALMACENERO | Operar insumos, compras, mermas, recetas, ajustes y consultas de inventario. |

Las rutas administrativas verifican rol en Spring Security del microservicio. No basta con ocultar botones en la aplicación móvil.

## 7. Migraciones añadidas

- `products-service`: `V4__areas_y_administracion_catalogo.sql` crea las cuatro áreas, enlaza las categorías existentes, añade metadatos al producto, siembra categorías iniciales y crea auditoría; `V5__opciones_configurables_de_productos.sql` permite repetir nombres de categoría entre áreas distintas y crea grupos de opciones y alternativas con recargo.
- `authentication-service`: `V2__auditoria_y_edicion_usuarios.sql` crea auditoría de cambios de cuentas.
- `orders-service`: no necesita nuevas columnas para la administración de mesas; utiliza la tabla `mesas` y el registro de auditoría existente.
- `inventory-service`: `V3__auditoria_y_ajustes_inventario.sql` crea auditoría de cambios de insumos.

No se modifica el checksum de migraciones anteriores. En bases existentes, Flyway aplica las nuevas migraciones al iniciar los servicios. No usar `docker compose down -v` sin aceptar que se borrarán los datos locales.

## 8. Validación y limitaciones honestas

Se añadieron pruebas unitarias de reglas administrativas del catálogo y se ampliaron las solicitudes Postman. En este entorno la compilación real no se pudo ejecutar: Maven Wrapper no pudo descargar Maven 3.9.9 desde Maven Central y no hay ejecutable `mvn` ni Docker disponible. Por tanto, no se declara que las pruebas, migraciones reales ni flujos con PostgreSQL/RabbitMQ hayan pasado.

Para verificar en Windows con JDK 21, conexión a Maven Central y Docker Desktop:

```powershell
java -version
.\mvnw.cmd -B -ntp -pl microservice/products-service,microservice/authentication-service,microservice/orders-service,microservice/inventory-service -am test
.\mvnw.cmd -B -ntp clean verify
docker compose config
docker compose up --build -d
docker compose ps -a
```

Luego probar en Postman las cuatro áreas, crear y modificar un producto con token ADM001, configurar grupos y opciones, intentar enviar un pedido con selecciones válidas e inválidas, comprobar que un token MOZ001 recibe 403 en rutas administrativas, crear una mesa sin pedidos activos, editar un usuario sin dejar el sistema sin administrador y registrar un ajuste de stock.

### Funciones que todavía necesitan integración o decisión externa

- Este archivo ZIP contiene backend, no las pantallas de Android Studio. La interfaz del administrador aún debe consumir estas API.
- El backend guarda una URL de imagen; falta decidir e implementar almacenamiento real de imágenes (servidor, S3/MinIO u otro proveedor).
- No se integró una API oficial real de RENIEC/SUNAT; las consultas DNI/RUC de comprobantes son datos de demostración local.
- El PDF de boleta/factura es una simulación académica sin valor tributario. La emisión electrónica real requiere definición del proveedor de facturación, credenciales/RUC y reglas SUNAT.
- La integración WhatsApp actual no envía automáticamente un PDF adjunto mediante API oficial de Meta.
- Yape/Plin registra una referencia declarada; no verifica automáticamente la transacción con el proveedor financiero.
- El cajón de dinero USB/serie depende de hardware/protocolo no especificado.
- Los grupos de opciones permiten tamaños, sabores, presentaciones y extras con precio adicional; para funcionalidades comerciales más avanzadas (por ejemplo, reglas condicionales entre opciones, cantidades mínimas por tipo de producto o recetas distintas por tamaño) sería necesario ampliar el modelo según la carta real.
- El descuento de insumos ocurre tras la confirmación de pago. La política ante faltantes está documentada como déficit registrado y stock físico no negativo; el negocio debe decidir si quiere bloquear la venta antes del cobro.
- La disponibilidad de producto se puede cambiar desde menú/cocina, pero no se calcula automáticamente por disponibilidad de todas las materias primas de una receta.
- Las promociones avanzadas (cupones, horarios, acumulación y reglas combinadas) y la operación multi-sucursal no están definidas en este alcance.

Por estos motivos, el backend queda ampliado y preparado para las funciones administrativas descritas, pero no debe certificarse como “100 % terminado” hasta ejecutar `clean verify`, Compose, migraciones y pruebas end-to-end en el entorno del proyecto y cerrar las integraciones/decisiones externas indicadas.
