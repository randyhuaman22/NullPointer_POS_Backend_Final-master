# Corrección del flujo de cajero y modalidades de datos del comprador

## Alcance de esta versión

Se agregó un endpoint local para que el backend responda a las cuatro modalidades acordadas antes de integrar servicios externos:

1. `SIN_DATOS`: permite preparar una boleta sin identificar al comprador. No permite factura.
2. `MANUAL`: permite escribir los datos del cliente. Para boleta puede omitirse el documento; para factura se exige RUC, razón social y dirección.
3. `DNI`: busca DNI en clientes guardados localmente y luego en un directorio ficticio de demostración.
4. `RUC`: busca RUC en clientes guardados localmente y luego en un directorio ficticio de demostración.

Los datos de demostración `12345678` y `20123456789` son ficticios y solo sirven para probar el flujo. No son datos reales ni se consultan RENIEC o SUNAT. Este endpoint prepara los datos del comprador; no emite todavía un comprobante tributario.

## Endpoint nuevo

`POST /api/v1/comprobantes/datos-cliente/validar`

Requiere un JWT de un usuario con rol `CAJERO` o `ADMINISTRADOR`.

### Prueba 1: boleta sin datos

```json
{
  "tipoComprobante": "BOLETA",
  "modalidad": "SIN_DATOS"
}
```

### Prueba 2: datos manuales para boleta

```json
{
  "tipoComprobante": "BOLETA",
  "modalidad": "MANUAL",
  "nombreCompleto": "Cliente Manual",
  "telefono": "987654321",
  "correo": ""
}
```

### Prueba 3: autocompletar DNI de prueba

```json
{
  "tipoComprobante": "BOLETA",
  "modalidad": "DNI",
  "tipoDocumento": "DNI",
  "numeroDocumento": "12345678"
}
```

### Prueba 4: autocompletar RUC de prueba

```json
{
  "tipoComprobante": "FACTURA",
  "modalidad": "RUC",
  "tipoDocumento": "RUC",
  "numeroDocumento": "20123456789"
}
```

## Endpoint de consulta anterior

Se mantiene `GET /api/v1/clientes/{tipoDocumento}/{numeroDocumento}`. Si el documento no existe en la base local, busca los dos registros ficticios anteriores. Para un documento desconocido devuelve HTTP 404 con un mensaje claro en vez de dejar la interfaz sin respuesta.

## Códigos de respuesta esperados

- `200`: datos preparados o encontrados.
- `400`: formato incorrecto o combinación no permitida (por ejemplo, factura sin RUC).
- `401`: no se envió un JWT válido.
- `403`: el usuario no tiene rol de cajero o administrador.
- `404`: el documento no existe en el registro local ni en los datos ficticios.

## Importante sobre la pantalla de caja

El ZIP que se revisó contiene el backend; no incluye la aplicación móvil o web que presenta el formulario del cajero. El backend ya expone la operación y Postman permite probarla, pero la pantalla debe llamar a este endpoint, mostrar la respuesta y presentar los errores HTTP al usuario. No se puede corregir el comportamiento visual del botón sin el código del cliente.

## WhatsApp y PDF

La integración real con WhatsApp y el envío de PDF quedan fuera de esta fase local. El endpoint actual de ticket de WhatsApp no debe presentarse como envío de PDF: necesita una implementación posterior para generar el documento y utilizar la API oficial de Meta. Hasta configurar credenciales, cualquier solicitud queda en estado pendiente y no se declara enviada.

## Estado de validación

La colección Postman y los POM/JSON se validaron sintácticamente. No se pudo ejecutar `mvnw test` porque el entorno no pudo descargar Maven 3.9.9 desde Maven Central. Por tanto, las pruebas JUnit añadidas requieren ejecución local antes de afirmar que el build completo está aprobado.

## Vista previa PDF local

`POST /api/v1/comprobantes/demostracion/pdf` devuelve un archivo PDF generado localmente. El total y código de pedido se consultan al `servicio-pedidos`; el cliente no puede cambiar el total enviado en el cuerpo. El documento muestra de forma visible `SIMULACION ACADEMICA - SIN VALOR TRIBUTARIO` y no debe presentarse como boleta/factura oficial.

Ejemplo (requiere un pedido existente y JWT de cajero/administrador):

```json
{
  "idPedido": 1,
  "tipoComprobante": "BOLETA",
  "modalidad": "SIN_DATOS"
}
```

Para compartirlo por WhatsApp de forma manual, la aplicación cliente puede descargar el PDF y permitir adjuntarlo en WhatsApp. El envío automático del PDF no está implementado todavía; requiere integrar WhatsApp Business Cloud API y configurar credenciales autorizadas.
