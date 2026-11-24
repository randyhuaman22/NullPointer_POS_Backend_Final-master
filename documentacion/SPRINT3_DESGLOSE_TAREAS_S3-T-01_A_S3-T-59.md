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

## HU-31 — Preparar datos del comprador para boleta/factura (nueva propuesta)

Esta historia se incorpora al backlog porque las HU anteriores no describían explícitamente las cuatro modalidades acordadas. El código local implementa una primera versión sin consultas externas.

- **S3-T-60:** endpoint `POST /api/v1/comprobantes/datos-cliente/validar`, DTO y servicio para `SIN_DATOS`, `MANUAL`, `DNI`, `RUC`.
- **S3-T-61:** directorio ficticio local para pruebas, sin llamadas a RENIEC/SUNAT.
- **S3-T-62:** mantener el GET de clientes y devolver respuesta o error 404 explicativo.
- **S3-T-63:** pruebas unitarias escritas para las modalidades y combinaciones inválidas.

Ubicaciones: `microservice/cashier-service/src/main/java/pe/upeu/nullpointerpos/caja/` y prueba en `src/test/java/pe/upeu/nullpointerpos/caja/ServicioDatosComprobanteTest.java`.

**Estado honesto:** código y pruebas escritos; no ejecutados porque Maven Wrapper no pudo descargar Maven 3.9.9. Los datos de ejemplo `12345678` y `20123456789` son ficticios. No existe integración real con RENIEC/SUNAT ni emisión tributaria en esta versión.

- **S3-T-64:** generar PDF de vista previa a partir del total consultado al servicio-pedidos, con marca visible “SIMULACIÓN ACADÉMICA – SIN VALOR TRIBUTARIO”; `POST /api/v1/comprobantes/demostracion/pdf`.
