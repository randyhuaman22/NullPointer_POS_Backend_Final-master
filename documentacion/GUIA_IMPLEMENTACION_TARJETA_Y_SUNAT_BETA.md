# Guía de Implementación: Flujo de Caja, Cobro con Tarjeta y Facturación Electrónica SUNAT Beta

## Correcciones del 25 de noviembre de 2026

La emisión guarda primero la identidad del comprobante y después intenta firmar y enviar. Los reintentos conservan el número y el pago. La migración V3 protege el pago con un bloqueo transaccional y un índice único para el comprobante vigente. Conserva los registros históricos duplicados para auditoría y marca como pendientes los registros anteriores sin firma verificable.

El estado `ACEPTADO` requiere un CDR con código `0` referido al documento enviado. Sin certificado el estado es `PENDIENTE_FIRMA`. Los fallos de transporte y las respuestas sin CDR válido quedan como `PENDIENTE_ENVIO`. Un CDR de rechazo devuelve `RECHAZADO`.

Para firma y envío se requiere un archivo PKCS12 con clave privada. Docker monta `./certificados` en `/certificados`. Las variables opcionales del archivo `.env` son `SUNAT_CERTIFICADO_RUTA` (por ejemplo `/certificados/emisor.p12`), `SUNAT_CLAVE_CERTIFICADO`, `SUNAT_CERTIFICADO_ALIAS`, `SUNAT_RUC_EMISOR` y `SUNAT_RAZON_SOCIAL`. El backend puede arrancar y registrar ventas sin esas variables. La aceptación en Beta depende de la respuesta del servicio externo.

El endpoint `POST /api/v1/comprobantes/{idComprobante}/reenviar` permite reintentar comprobantes pendientes con el mismo número. Requiere el rol de cajero o administrador. Las rutas y los campos usados por la app se conservan. La app necesita incorporar el botón de reintento y mostrar los estados de emisión.

El PDF muestra el estado registrado y la identificación del comprobante. Se retiraron el dibujo que simulaba un QR y el texto que afirmaba una autorización. La cadena QR sigue disponible en la respuesta de la API.

El iniciador `utilidades/iniciar-en-windows.bat` reconstruye y levanta los servicios mediante `docker compose up -d --build`.

## 1. Resumen Ejecutivo de la Implementación

En esta actualización se incorporaron los módulos pendientes para completar el flujo operativo del **Cajero** y la **Facturación Electrónica** oficial de la pollería, cumpliendo con los estándares exigidos:

1. **Método de Pago TARJETA:**
   - Se habilitó `TARJETA` en `SolicitudCobroDigital` y `PartePago`.
   - Se incorporó la migración Flyway `V2__agregar_metodo_tarjeta_y_comprobantes_sunat.sql` para actualizar las restricciones `CHECK` de la base de datos.
   - Se implementó el **Simulador de Tarjeta propio** (`POST /api/v1/caja/simulador-tarjeta`) con validación matemática de **Algoritmo de Luhn**, detección automática de marcas (**VISA**, **MASTERCARD**, **AMEX**, **DINERS**) y generación de códigos de autorización y voucher.

2. **Deducción de Inventario y Cocina:**
   - Al confirmar el cobro en caja (`EFECTIVO`, `YAPE`, `PLIN` o `TARJETA`), `cashier-service` emite el evento transaccional `pago.confirmado` vía RabbitMQ Outbox.
   - `orders-service` actualiza el pedido a `PAGADO`, libera la mesa asociada y publica `pedido.pagado`.
   - `inventory-service` recibe `pedido.pagado` y descuenta automáticamente los insumos de la receta (pollos, porciones de papas, aceite, etc.) de `esquema_inventario.insumos`.

3. **Facturación Electrónica SUNAT (Modo Beta):**
   - Configurado con los servidores de prueba oficiales de SUNAT:
     - **RUC Emisor:** `20000000001`
     - **Razón Social:** `POLLERIA Y PARRILLAS WENDY'S FOOD S.A.C.`
     - **Usuario SOL:** `MODDATOS`
     - **Clave SOL:** `MODDATOS`
     - **Endpoint WebService SOAP:** `https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService`
   - Genera el documento XML bajo el estándar **UBL 2.1** (`<Invoice>`).
   - Calcula el **DigestValue SHA-256** de la firma electrónica.
   - Genera la **Cadena QR oficial de SUNAT**: `RUC|TIPO|SERIE|CORRELATIVO|IGV|TOTAL|FECHA|TIPO_DOC|NUM_DOC|HASH`.
   - Lee el CDR devuelto por SUNAT y verifica el código y el documento al que se refiere.
   - Genera el **Ticket PDF para impresora térmica de 80mm** con el estado de emisión y la identificación del comprobante.

---

## 2. Flujo Completo del Cajero (Paso a Paso)

### Paso 1: El cajero consulta el pedido
El cajero consulta el monto del pedido a cobrar:
* **Endpoint:** `GET /api/v1/pedidos/{idPedido}/resumen-pago`
* **Ejemplo de respuesta:** Total `S/ 65.00`, estado `LISTO`.

### Paso 2: El cliente indica cómo va a pagar

#### Opción A: Cliente paga con YAPE o PLIN
1. El cliente escanea el QR en el mostrador y le muestra su celular al cajero.
2. El cajero registra el pago digital:
   * **Endpoint:** `POST /api/v1/caja/cobrar-digital`
   * **Payload:**
     ```json
     {
       "idPedido": 1,
       "metodo": "YAPE",
       "numeroOperacion": "19482104",
       "ultimosCuatroDigitos": "9821",
       "idempotencia": "cobro-yape-pedido-1"
     }
     ```

#### Opción B: Cliente paga con TARJETA
1. El cajero abre la ventana del simulador en su pantalla:
   * **Endpoint:** `POST /api/v1/caja/simulador-tarjeta`
   * **Payload:**
     ```json
     {
       "numeroTarjeta": "4111 1111 1111 1111",
       "mesVencimiento": 12,
       "anioVencimiento": 2028,
       "cvv": "123",
       "titular": "JUAN PEREZ"
     }
     ```
   * **Respuesta:**
     ```json
     {
       "aprobado": true,
       "marca": "VISA",
       "codigoAutorizacion": "AUTH-784912",
       "numeroOperacion": "VOUCH-892144",
       "ultimosCuatroDigitos": "1111",
       "titular": "JUAN PEREZ",
       "mensaje": "Pago con tarjeta VISA aprobado exitosamente por la entidad bancaria simulada."
     }
     ```
2. El cajero confirma el cobro con tarjeta:
   * **Endpoint:** `POST /api/v1/caja/cobrar-digital`
   * **Payload:**
     ```json
     {
       "idPedido": 1,
       "metodo": "TARJETA",
       "numeroOperacion": "VOUCH-892144",
       "ultimosCuatroDigitos": "1111",
       "idempotencia": "cobro-tarjeta-pedido-1"
     }
     ```

---

### Paso 3: Emisión de la Boleta / Factura Electrónica a SUNAT Beta
Inmediatamente tras confirmar el cobro, el cajero emite el comprobante tributario:
* **Endpoint:** `POST /api/v1/comprobantes/emitir`
* **Payload:**
  ```json
  {
    "idPedido": 1,
    "idPago": 1,
    "tipoComprobante": "BOLETA",
    "modalidad": "DNI",
    "tipoDocumento": "DNI",
    "numeroDocumento": "12345678",
    "nombreCompleto": "JUAN PEREZ LOPEZ",
    "direccion": "AV. REVOLUCION 123 - LIMA",
    "correo": "cliente@gmail.com",
    "telefono": "987654321"
  }
  ```
* **Respuesta del Backend con aprobación de SUNAT:**
  ```json
  {
    "idComprobante": 1,
    "idPedido": 1,
    "idPago": 1,
    "tipoComprobante": "BOLETA",
    "serie": "B001",
    "correlativo": 1,
    "numeroCompleto": "B001-00000001",
    "fechaEmision": "2026-11-25T00:10:00Z",
    "rucEmisor": "20000000001",
    "razonSocialEmisor": "POLLERIA Y PARRILLAS WENDY'S FOOD S.A.C.",
    "tipoDocumentoReceptor": "DNI",
    "numeroDocumentoReceptor": "12345678",
    "denominacionReceptor": "JUAN PEREZ LOPEZ",
    "totalGravado": 55.08,
    "totalIgv": 9.92,
    "totalImporte": 65.00,
    "moneda": "PEN",
    "metodoPago": "TARJETA",
    "hashCpe": "jX7Qk2L8vN9mP4wR6tY1uI3oA5s=",
    "codigoCdr": "0",
    "descripcionCdr": "La Boleta numero B001-00000001 ha sido ACEPTADA por SUNAT (Ambiente Beta Oficial)",
    "estadoSunat": "ACEPTADO",
    "cadenaQr": "20000000001|03|B001|00000001|9.92|65.00|2026-11-25|1|12345678|jX7Qk2L8vN9mP4wR6tY1uI3oA5s=",
    "urlPdf": "/api/v1/comprobantes/1/pdf",
    "urlXml": "/api/v1/comprobantes/1/xml"
  }
  ```

---

### Paso 4: Impresión y Entrega del Ticket con Código QR
* Para abrir o imprimir el ticket en la tiquetera de 80mm:  
  **`GET /api/v1/comprobantes/1/pdf`**
* Para descargar el XML firmado UBL 2.1 oficial:  
  **`GET /api/v1/comprobantes/1/xml`**

---

## 3. Pruebas Unitarias Agregadas
Se incluyeron pruebas automatizadas en `cashier-service`:
1. `SimuladorTarjetaTest`:
   * Validación del algoritmo Luhn para tarjetas Visa y Mastercard válidas.
   * Detección automática de marca de tarjeta.
   * Rechazo con error 400 para tarjetas no válidas matemáticamente.
   * Rechazo de tarjetas vencidas.
2. `ServicioSunatBetaTest`:
   * Verificación de generación de XML UBL 2.1 con etiquetas de emisor y receptor.
   * Cálculo exacto de base imponible gravada y desglose de IGV (18%).
   * Formato de la cadena oficial para el código QR de SUNAT.
   * Generación de bytes válidos `%PDF-` para el ticket térmico de 80mm.
