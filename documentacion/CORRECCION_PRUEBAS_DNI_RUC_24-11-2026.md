# Corrección de pruebas de comprobantes DNI/RUC

## Síntoma

La ejecución `mvnw.cmd -B -ntp clean verify` fallaba en dos pruebas del microservicio de Caja:

- `consultaDniEnDirectorioLocalFicticio`: el nombre esperado `CLIENTE DE PRUEBA` llegaba como `null`.
- `consultaRucEnDirectorioLocalFicticio`: el nombre esperado `EMPRESA DEMOSTRATIVA S.A.C.` llegaba como `null`.

## Causa

`ServicioDatosComprobante` consideraba válido cualquier mapa distinto de `null` devuelto por `RepositorioCaja.buscarCliente`. Un mapa vacío (por ejemplo, en una prueba con Mockito) entraba en la rama de registro local y evitaba consultar el directorio ficticio.

## Corrección

Ahora se usa el registro de base de datos únicamente cuando `nombreCompleto` tiene contenido. Si el mapa es `null`, está vacío o no contiene un nombre utilizable, el servicio consulta `DirectorioDemostracionDocumentos`. La búsqueda de demostración normaliza espacios y mayúsculas.

Se mantienen las siguientes reglas:

- DNI `12345678`: devuelve `CLIENTE DE PRUEBA` (ficticio).
- RUC `20123456789`: devuelve `EMPRESA DEMOSTRATIVA S.A.C.` y `DIRECCIÓN DE PRUEBA` (ficticios).
- Si hay un registro local completo, este tiene prioridad sobre los datos demostrativos.
- Un documento no encontrado devuelve HTTP 404.

## Pruebas añadidas/actualizadas

- DNI de demostración cuando la consulta local retorna mapa vacío.
- RUC de demostración cuando la consulta local retorna mapa vacío.
- Prioridad del registro local cuando contiene nombre.
- HTTP 404 para documento no registrado ni incluido en el directorio demostrativo.

## Validar en Windows

Desde la carpeta raíz del proyecto (donde está `pom.xml`):

```powershell
.\mvnw.cmd -B -ntp -pl microservice/cashier-service -am -Dtest=ServicioDatosComprobanteTest -Dsurefire.failIfNoSpecifiedTests=false test
.\mvnw.cmd -B -ntp clean verify
```

La corrección de la lógica se verificó de forma aislada con Java 21. La ejecución Maven/JUnit completa debe confirmarse en el equipo que dispone de la distribución Maven y dependencias.
