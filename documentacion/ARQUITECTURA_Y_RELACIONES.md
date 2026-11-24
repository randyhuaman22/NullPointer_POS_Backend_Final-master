# Arquitectura y relaciones - Wendy's Food

## Componentes

- `infrastructure/api-gateway` (8080): entrada pública, valida JWT y enruta.
- `infrastructure/eureka-server` (8761): registro y descubrimiento.
- `infrastructure/config-server` (8888): sirve configuración central desde `classpath:/configs`.
- `microservice/authentication-service` (8081): usuarios y roles; BD `bd_autenticacion`.
- `microservice/products-service` (8084): menú, categorías y productos; BD `bd_menu`.
- `microservice/orders-service` (8082): mesas, comandas y eventos; BD `bd_comandas`.
- `microservice/kitchen-service` (8083): consumo RabbitMQ y WebSocket/STOMP.

## Flujo de red

```mermaid
flowchart LR
  APP[Android / Postman] --> GW[API Gateway :8080]
  GW -->|login / usuarios| AU[Authentication :8081]
  GW -->|categorías / menú| PR[Products :8084]
  GW -->|mesas / pedidos| OR[Orders :8082]
  OR -->|consultar precio y disponibilidad| PR
  OR -->|outbox / evento| MQ[(RabbitMQ)]
  MQ --> KI[Kitchen :8083]
  KI -->|STOMP / WebSocket| APP
  AU -. registra .-> EU[Eureka :8761]
  PR -. registra .-> EU
  OR -. registra .-> EU
  KI -. registra .-> EU
  GW -. descubre .-> EU
  CFG[Config Server :8888] -. configura .-> AU
  CFG -. configura .-> PR
  CFG -. configura .-> OR
  CFG -. configura .-> KI
  CFG -. configura .-> GW
  AU --> DBA[(bd_autenticacion)]
  PR --> DBP[(bd_menu)]
  OR --> DBO[(bd_comandas)]
```

## Rutas principales conservadas

| Ruta pública | Destino |
|---|---|
| `/api/v1/autenticacion/**` | authentication-service |
| `/api/v1/administracion/**` | authentication-service |
| `/api/v1/menu/**` | products-service |
| `/api/v1/mesas/**` | orders-service |
| `/api/v1/pedidos/**` | orders-service |
| `/api/v1/cocina/**` | orders-service |
| `/ws/cocina/**` | kitchen-service |

## Separación de datos

`products-service` es propietario de categorías/productos. `orders-service` no consulta tablas de otra base: solicita el producto por HTTP al catálogo y persiste en cada línea el identificador, nombre y precio unitario utilizados en ese pedido. Esto conserva la información histórica incluso si el producto cambia de precio posteriormente.
