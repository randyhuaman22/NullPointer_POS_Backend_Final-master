# Informe de revisión integral — NullPointer POS

## Cambios aplicados

1. **Corrección de `kitchen-service/src/test`:** se agregó `spring-boot-starter-test` con alcance `test` en `microservice/kitchen-service/pom.xml`. El test `EventoCocinaMapperTest` usa JUnit 5 y aserciones de JUnit; sin esa dependencia, el IDE no resuelve los imports y la compilación de pruebas falla.
2. **Nombres de registro de microservicios en español:** `servicio-autenticacion`, `servicio-productos`, `servicio-pedidos`, `servicio-cocina`, `servicio-caja`, `servicio-inventario`, `servidor-configuracion`, `servidor-descubrimiento` y `puerta-enlace-api`.
3. **Gateway:** las rutas de descubrimiento `lb://` y `lb:ws://` apuntan a los mismos identificadores españoles registrados en Eureka.
4. **Docker Compose:** se tradujeron los nombres de servicio y se actualizaron dependencias, nombres DNS internos, URLs interservicio y nombres de contenedor. Se conservaron las rutas físicas de carpetas/Dockerfiles para no romper el contexto de compilación.
5. **Nombres visibles Maven:** se actualizaron los `<name>` de los módulos a español, manteniendo los `artifactId`/`finalName` existentes para evitar romper los Dockerfiles y las dependencias entre módulos.

## Verificaciones realizadas

- 10 archivos `pom.xml` analizados como XML válido.
- Todos los archivos YAML/YML analizados sintácticamente.
- Archivos JSON analizados sintácticamente.
- Dependencia de pruebas verificada en cada microservicio con pruebas existentes.
- Nombres de Eureka comparados con los destinos del API Gateway: todos los destinos encontrados.
- Dependencias `depends_on` de Docker Compose y rutas de Dockerfiles comprobadas: sin referencias a servicios inexistentes.
- Integridad del archivo ZIP comprobada tras generarlo.

## Verificaciones pendientes por limitación del entorno

Se revisaron estáticamente 164 archivos Java: no se detectaron desbalances de llaves ni inconsistencias entre las rutas y declaraciones de paquetes. Esto no sustituye la compilación real. No se pudo ejecutar `mvn clean verify` porque Maven Wrapper intentó descargar Maven 3.9.9 desde Maven Central y la descarga falló. En este entorno tampoco hay ejecutable `mvn` ni `docker`. Por ello, no se declara que los tests Java hayan pasado ni que las imágenes Docker se hayan construido.

## Convención de idioma y compatibilidad

Los paquetes Java de los servicios de negocio e infraestructura se han traducido a nombres de dominio en español: `autenticacion`, `productos`, `pedidos`, `cocina`, `caja`, `inventario`, `puertaenlace`, `servidorconfiguracion` y `servidordescubrimiento`. Los nombres de servicio registrados en Spring/Eureka y los nombres visibles de Docker Compose también están en español. Se conservan las carpetas físicas Maven con nombres históricos en inglés (`orders-service`, `cashier-service`, etc.) porque los Dockerfiles y el POM raíz las referencian. Los campos públicos JSON se conservan cuando forman parte de contratos existentes; traducirlos sin actualizar Android/Postman podría romper compatibilidad.

## Prueba recomendada en Windows

Desde la raíz que contiene `pom.xml`, `mvnw.cmd` y `docker-compose.yml`, con JDK 21 instalado:

```powershell
java -version
.\mvnw.cmd -B -ntp -pl microservice/kitchen-service -am test
.\mvnw.cmd -B -ntp clean verify
docker compose config
docker compose up --build -d
docker compose ps -a
```

Si la compilación falla, guardar el primer error de Maven completo; no basta con el subrayado rojo del IDE.

## Actualización de caja y comprobantes (24/11/2026)

- Se tradujeron los paquetes base Java de los microservicios a `autenticacion`, `productos`, `pedidos`, `cocina`, `caja`, `inventario`, `puertaenlace`, `servidorconfiguracion` y `servidordescubrimiento`. Los nombres de servicio registrados en Spring/Eureka permanecen en español. Se conservaron los nombres de carpetas Maven heredados para no romper rutas de construcción Docker.
- Se agregó HU-31 como propuesta nueva del Sprint 3, con tareas S3-T-60 a S3-T-64, para preparar los datos de comprador mediante cuatro modalidades: SIN_DATOS, MANUAL, DNI y RUC.
- La consulta de DNI/RUC usa solamente datos locales guardados y un directorio ficticio de prueba. No hay consulta real a RENIEC/SUNAT en esta versión.
- Se agregó `POST /api/v1/comprobantes/datos-cliente/validar` y `POST /api/v1/comprobantes/demostracion/pdf`. El PDF usa el total consultado desde `servicio-pedidos` y lleva la marca `SIMULACIÓN ACADÉMICA - SIN VALOR TRIBUTARIO`.
- Se agregó la prueba unitaria `ServicioDatosComprobanteTest` y una prueba mínima del generador PDF. El generador PDF se compiló aislado con Java 21 y se verificó como PDF válido de una página. Las pruebas Spring/Maven del proyecto completo no pudieron ejecutarse: el Maven Wrapper no pudo descargar Maven 3.9.9 desde Maven Central.
- El ZIP contiene backend y documentación, no la aplicación Android/web. Para que el botón visual del cajero responda, el cliente debe llamar al endpoint y mostrar errores/resultado. El envío automático del PDF por WhatsApp sigue pendiente de integrar la API oficial de Meta.


## Ampliación administrativa del backend (24/11/2026)

- Se incorporó la estructura de cuatro áreas de negocio del menú: `POLLERIA`, `MENU`, `HELADERIA` y `JUGUERIA`, con categorías iniciales por área y filtro de consulta.
- `products-service` ahora incorpora endpoints administrativos para crear, editar, desactivar y restaurar categorías y productos; administrar estaciones; filtrar catálogo por área; y consultar auditoría. Los productos/categorías se desactivan lógicamente para conservar pedidos y ventas históricas.
- `authentication-service` ahora permite actualizar nombre, correo, rol y PIN de una cuenta, con auditoría; mantiene las reglas para impedir que se desactive o degrade al último administrador activo.
- `orders-service` ahora permite administrar mesas (crear, editar, desactivar/restaurar, habilitar y marcar fuera de servicio), con bloqueo de fila y validación para no alterar mesas con pedidos abiertos.
- `inventory-service` ahora permite actualizar y archivar insumos, restaurarlos, registrar ajustes por conteo físico, consultar compras y recetas y validar que los insumos usados en recetas estén activos.
- Se añadieron migraciones Flyway V4 y V5 (productos), V2 (autenticación) y V3 (inventario), además de un anexo de API y solicitudes Postman.
- Se añadieron grupos de opciones por producto (tamaños, sabores, presentaciones y extras), con recargos configurables, validación de pertenencia/límites y cálculo de precio en el catálogo. `orders-service` valida las selecciones contra el catálogo y guarda precio/selecciones en el detalle histórico del pedido.
- Se endureció el mantenimiento de catálogo: toda alta/edición de producto exige estación de preparación activa; mover categorías con subcategorías a otra área queda bloqueado para no dejar relaciones inconsistentes.
- Se corrigió una anotación `@Transactional` duplicada detectada en `RepositorioInventario` durante la revisión estática.
- Verificación estática: 177 fuentes Java revisadas en el árbol actual y ninguna discrepancia detectada entre declaraciones de paquete y ruta del archivo; el analizador `javac` no reportó errores de sintaxis, pero falló por dependencias externas no presentes en el classpath. Se repitió el barrido tras integrar las opciones configurables (192 fuentes Java, incluidas las pruebas). El Maven Wrapper volvió a fallar al descargar Maven 3.9.9 desde Maven Central. No hay Docker en este entorno.
- No se declara compilación, ejecución de pruebas, aplicación real de migraciones o prueba end-to-end aprobada. Las integraciones con RENIEC/SUNAT, emisión tributaria, envío de PDF por WhatsApp, verificación real de Yape/Plin, interfaz Android, almacenamiento de imágenes y las reglas avanzadas de variantes/modificadores siguen siendo temas pendientes explícitamente detallados en `documentacion/ADMINISTRACION_COMPLETA_Y_AREAS_NEGOCIO.md`.


## Verificación estática final de la ampliación administrativa (24/11/2026)

- Se analizaron 10 archivos POM como XML válido, 11 archivos YAML/YML y 3 JSON. La colección Postman contiene 72 solicitudes.
- Se revisaron 192 archivos Java: no hay discrepancias entre paquetes declarados y rutas, no se encontraron anotaciones `@Transactional` duplicadas en secuencia ni diagnósticos de sintaxis en el análisis de parser de `javac`.
- Se comprobaron 9 módulos Maven declarados en el POM raíz, rutas existentes y versiones Flyway sin duplicados por servicio.
- `javac` no pudo compilar el proyecto porque el classpath no contiene las dependencias Spring/Jakarta y el resto de librerías externas. El análisis de parser no sustituye `mvn clean verify`.
- No se pudo ejecutar Maven, Docker Compose ni aplicar las migraciones en PostgreSQL en este entorno. Por tanto, esta es una revisión estática y no una certificación de funcionamiento integral.


## Corrección posterior a la primera ejecución Maven en Windows (24/11/2026)

- El usuario ejecutó `mvnw.cmd -B -ntp clean verify`. La compilación de los módulos anteriores a Caja fue correcta; el proceso se detuvo en `ServicioDatosComprobanteTest` con dos fallos: DNI y RUC devolvían `nombreCompleto=null`. Por esa razón Inventario y API Gateway quedaron omitidos en esa ejecución.
- Causa encontrada: un resultado vacío de `RepositorioCaja.buscarCliente` podía ser interpretado como un cliente local existente, porque el servicio solo comprobaba `local != null`. Mockito puede devolver mapas vacíos para métodos que retornan colecciones/mapas; además, cualquier fila sin nombre debe considerarse un resultado incompleto.
- Corrección aplicada en `ServicioDatosComprobante`: solo se utiliza el registro local si contiene un `nombreCompleto` no vacío; de lo contrario, se consulta `DirectorioDemostracionDocumentos`.
- Se reforzó la búsqueda del directorio ficticio para normalizar espacios y mayúsculas con `Locale.ROOT`.
- Se actualizaron las pruebas para representar explícitamente mapas vacíos y se añadieron casos para priorizar un cliente local válido y responder 404 cuando el documento no existe.
- Validación local de esta corrección: las clases reales de servicio/directorio/DTO se compilaron con Java 21 usando stubs mínimos de infraestructura y se ejecutó una prueba de comportamiento para DNI ficticio, RUC ficticio y prioridad del registro local; resultado: `OK`. Esto no sustituye la ejecución JUnit/Maven del proyecto. El Maven Wrapper de este entorno no pudo descargar Maven 3.9.9 desde Maven Central, por lo que la repetición de `mvn clean verify` queda pendiente en el equipo del usuario.
- Comando recomendado en Windows para validar primero el arreglo: `.\mvnw.cmd -B -ntp -pl microservice/cashier-service -am -Dtest=ServicioDatosComprobanteTest -Dsurefire.failIfNoSpecifiedTests=false test`; después ejecutar `.\mvnw.cmd -B -ntp clean verify`.


## Actualización: inicio de Docker y organización Postman (24/11/2026)

- Validación informada desde la PC del usuario: `mvnw.cmd -B -ntp clean verify` finalizó con `BUILD SUCCESS` para los 10 módulos y se ejecutaron 33 pruebas sin fallos.
- Una construcción inicial de Docker en paralelo falló mientras Maven Central cortaba descargas antes de completar los archivos `byte-buddy-1.17.8` y `spring-core-6.2.19`. La construcción secuencial de imágenes permitió completar el inicio en la PC del usuario.
- Se añadió el lanzador raíz `INICIAR_BACKEND.bat`: comprueba Docker Desktop, construye las nueve imágenes de servicio una por una, reintenta una vez el build que falle e inicia toda la plataforma. El lanzador no elimina datos ni volúmenes.
- Se corrigió `utilidades/iniciar-en-windows.bat`, que usaba nombres de servicio antiguos, y se añadió `utilidades/detener-backend.bat` para detener los contenedores sin borrar datos.
- La colección principal de Postman se reorganizó en cuatro niveles: `NullPointer POS > rol/área > módulo > endpoint`, con token independiente por rol y logins de demostración para administrador, cajero, mozo y cocinero. Quedó en `postman/NullPointer_POS.postman_collection.json`; el entorno local solo define `baseUrl` para no sobrescribir tokens vacíos.
- La verificación de una instalación en otra PC debe completarse revisando `docker compose ps -a`, la salud del Gateway y los flujos funcionales; el lanzador automatiza el build, pero no certifica por sí solo el comportamiento end-to-end.
