# Cómo importar y probar `orders-service` en Windows

## 1. Abrir la carpeta raíz correcta

Descomprime el ZIP y abre en VS Code/IntelliJ la carpeta interna `NullPointer_POS_Sprint1_Microservicios_ES`, que contiene el `pom.xml` principal, `.mvn/`, `microservice/`, `infrastructure/` y `docker-compose.yml`. No abras solamente la carpeta `orders-service/src/main/java`; al hacerlo, el IDE no resolverá bien el parent POM ni las dependencias gestionadas del proyecto multimódulo.

## 2. Seleccionar Java 21 y recargar Maven

En PowerShell: `java -version`. Debe indicar Java 21.

**VS Code:** instala/activa Extension Pack for Java y Maven for Java; ejecuta `Java: Configure Java Runtime`, selecciona JDK 21, luego `Java: Clean Java Language Server Workspace` y recarga la ventana. En el panel Maven usa **Reload Projects** desde la raíz del proyecto.

**IntelliJ IDEA:** abre el `pom.xml` raíz como proyecto Maven, establece Project SDK y language level en 21, abre Maven tool window y ejecuta **Reload All Maven Projects**. No agregues los JAR de Spring manualmente.

## 3. Compilar primero solo el servicio de órdenes y sus prerequisitos

En PowerShell, desde la raíz: 

```powershell
java -version
.\mvnw.cmd -B -ntp -pl microservice/orders-service -am clean verify
```

La primera ejecución requiere internet para descargar Maven 3.9.9 y dependencias desde Maven Central. Si falla por conexión DNS/HTTP, no es un error del Controller: primero debe descargarse el wrapper y sus dependencias. Conserva el primer bloque `COMPILATION ERROR` completo; corregir errores que aparezcan después sin resolver el primero puede ser engañoso.

## 4. Validar el backend multimódulo con Docker

Primero copia la configuración local y revísala: 

```powershell
Copy-Item .env.example .env
docker compose config
```

Luego, con Docker Desktop iniciado: 

```powershell
docker compose up --build -d
docker compose ps -a
docker compose logs -f servicio-pedidos
```

Para probar los demás servicios: `docker compose logs -f servicio-autenticacion servicio-productos servicio-cocina servicio-caja servicio-inventario puerta-enlace-api`. Todos los endpoints de prueba a través del gateway usan `http://localhost:8080`.

## 5. Flujos mínimos de Postman

1. `POST /api/v1/autenticacion/iniciar-sesion` con usuario de prueba del rol requerido; copiar el JWT a la variable de colección.
2. `GET /api/v1/mesas`; `GET /api/v1/menu/categorias`; `GET /api/v1/menu/productos`.
3. Crear comanda con una mesa libre y un producto disponible; guardar el ID devuelto.
4. Enviar la comanda a cocina y comprobar `/api/v1/cocina/pedidos` y los logs de RabbitMQ/`kitchen-service`.
5. Abrir caja con token CAJERO, consultar resumen de pago, registrar cobro y comprobar el estado `PAGADO`.
6. Crear insumos y recetas antes de verificar el descuento automático; confirmar `/api/v1/inventario/movimientos`, `/api/v1/inventario/alertas` y `/api/v1/inventario/faltantes-venta`.
7. Probar los informes y descargar `/api/v1/reportes/ventas/exportar.xlsx`.

## 6. Cambios específicos de la revisión

- `MesaRepositorio.actualizarEstadoSegunPedidos` ahora usa solo columnas que existen en la tabla `mesas`; conserva `FUERA_SERVICIO` y calcula `LIBRE/OCUPADA`.
- `PedidoMapper.estadoDesdeFila` recibe `importe_total`; antes el método de pago podía quedarse sin el valor necesario para comparar el evento.
- `RepositorioInventario` registra en `movimientos` solo la cantidad realmente descontada. Los faltantes se escriben en una tabla nueva creada por `V2__crear_faltantes_venta.sql`.
- La V1 del inventario se mantuvo intacta para no provocar errores de checksum en instalaciones existentes. Flyway aplica V2 después de V1 al iniciar.

## 7. Advertencias

El análisis estático de esta entrega encontró cero errores de sintaxis Java en 155 archivos, pero **eso no sustituye `mvn clean verify`**. La compilación completa y Docker deben ejecutarse en tu computadora porque este entorno no cuenta con Maven instalado, repositorio local de dependencias ni Docker. Si las clases siguen rojas tras recargar Maven, comparte el primer error del panel **Problems/Build**, no solo la captura de las líneas rojas.

## 8. Corrección específica de compilación identificada en la revisión

En `microservice/orders-service/src/main/java/pe/upeu/nullpointerpos/pedidos/repository/PedidoRepositorio.java` faltaba importar `java.util.Map`, aunque la clase declara métodos que devuelven `List<Map<String,Object>>`. Se agregó la importación. Este error de compilación podía marcar el repositorio en rojo y provocar errores derivados en las clases que lo utilizan.

Además, se verificó estáticamente que las llamadas de `ServicioPedidosImpl` a `PedidoRepositorio` y `MesaRepositorio` apuntan a métodos existentes, y que `ControladorOperacionesPedido` usa métodos declarados en `ServicioPedidos`. La comprobación estática no reemplaza la compilación real. Después de recargar Maven, ejecuta el comando de la sección 3 y corrige cualquier primer error nuevo que el compilador reporte.
