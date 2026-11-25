# Estructura de microservicios por capas - Wendy's Food

Cada microservicio usa un paquete base propio (por ejemplo, `pe.upeu.nullpointerpos.pedidos`) para evitar colisiones entre clases. El proyecto adopta la organización del ZIP de referencia `sistema-ventas-main`: Maven multimódulo, separación entre infraestructura y microservicios de negocio, y paquetes estándar por capa.

## Estructura principal

```text
NullPointer_POS_Sprint1_Microservicios_ES/
├── pom.xml                         # Padre Maven y versiones centralizadas
├── docker-compose.yml
├── infrastructure/
│   ├── api-gateway/                # Gateway HTTP + validación JWT
│   ├── config-server/              # Configuración centralizada Spring Cloud
│   └── eureka-server/              # Registro/descubrimiento de servicios
├── microservice/
│   ├── authentication-service/     # Usuarios, roles e inicio de sesión
│   ├── products-service/           # Menú, categorías y productos
│   ├── orders-service/             # Mesas, comandas y traslado de mesa
│   └── kitchen-service/            # Eventos RabbitMQ y WebSocket/STOMP
├── base-de-datos/
├── postman/
├── utilidades/
└── documentacion/
```

## Paquetes internos de los servicios de negocio

```text
pe.upeu.nullpointerpos.<microservicio>/
├── controller/       # Endpoints REST
├── dto/
│   ├── request/      # Datos recibidos por la API
│   ├── response/     # Respuestas entregadas por la API
│   └── external/     # Contratos recibidos de otros microservicios
├── entity/           # Modelos del dominio y registros leídos de BD
├── exception/        # Errores y manejador global
├── mapper/           # Conversión entre filas/modelos y DTO
├── repository/       # Consultas SQL y persistencia
├── security/         # JWT y filtros de autorización
├── service/          # Interfaces de casos de uso
│   └── impl/         # Implementaciones de los casos de uso
├── config/           # Seguridad, RabbitMQ y configuración técnica
└── client/           # (orders-service) cliente HTTP del catálogo
```

`kitchen-service` usa además `messaging/` para el listener de RabbitMQ, ya que no recibe las comandas mediante un controller REST de escritura.

## Responsabilidad de cada aplicación

| Módulo | Qué contiene | Puerto local |
|---|---|---:|
| `eureka-server` | Registro y descubrimiento de instancias | 8761 |
| `config-server` | Configuración centralizada en perfil nativo | 8888 |
| `api-gateway` | Entrada única y rutas REST/WebSocket | 8080 |
| `authentication-service` | Login, usuarios, roles y JWT | 8081 |
| `products-service` | Categorías, productos y disponibilidad del menú | 8084 |
| `orders-service` | Mesas, borradores, detalles, traslados y envío a cocina | 8082 |
| `kitchen-service` | Consume eventos y distribuye actualizaciones por WebSocket | 8083 |

## Flujo típico de crear una comanda

1. La app envía la petición al `api-gateway`.
2. El gateway valida el JWT y envía `/api/v1/pedidos/**` a `orders-service`.
3. `ControladorPedidos` delega en la interfaz de `service`.
4. `ServicioPedidosImpl` valida mesa y mozo. Para cada producto usa `client/ClienteProductos` y consulta `products-service`.
5. `repository/PedidoRepositorio` guarda el pedido, líneas y totales en `bd_comandas`. El detalle guarda nombre y precio como snapshot histórico.
6. Al pulsar Enviar a Cocina, el outbox publica el evento en RabbitMQ.
7. `kitchen-service` consume el evento y lo difunde por STOMP/WebSocket.

## Persistencia y comunicación

- PostgreSQL mantiene bases independientes: `bd_autenticacion`, `bd_menu`, `bd_comandas` y `bd_cocina` (reservada para ampliaciones de Sprint 2).
- El catálogo es dueño de `bd_menu`; pedidos no consulta ni modifica directamente sus tablas.
- El servicio de pedidos guarda el nombre y precio de cada producto en el detalle para conservar el precio con el que fue tomado el pedido.
- RabbitMQ desacopla el envío de comandas a cocina.
- Config Server es opcional durante la resolución de configuración (`optional:configserver`); los servicios mantienen configuración local de respaldo.
- Eureka registra los servicios y el gateway usa descubrimiento para las rutas HTTP.

## Capas y tecnología

Se conserva JDBC (`JdbcTemplate`) en los repositorios para no cambiar las consultas transaccionales existentes. Por eso `entity/` contiene modelos de dominio y los repositorios son clases `@Repository` con SQL explícito, no interfaces `JpaRepository`. La organización de capas es equivalente a la del proyecto de referencia, sin introducir un cambio masivo a JPA.
