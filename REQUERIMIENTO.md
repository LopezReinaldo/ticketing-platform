# Plataforma de Procesamiento de Eventos de Ticketing

## Contexto

Empresa de venta de entradas (conciertos, teatro, deportes) necesita modernizar su sistema de procesamiento de compras. El sistema actual no escala ante picos de demanda y presenta problemas de consistencia:

- Compras duplicadas
- Asientos vendidos a múltiples personas (sobreventa)
- Timeouts durante alta concurrencia

La solución requiere una **arquitectura reactiva y basada en eventos** capaz de manejar miles de solicitudes concurrentes, procesando compras de forma asíncrona mediante colas de mensajes.

## Estados de las entradas

| Estado | Descripción | Observaciones |
|---|---|---|
| `AVAILABLE` | Disponible | Estado inicial |
| `RESERVED` | Reservada | No representa venta |
| `PENDING_CONFIRMATION` | En confirmación | No representa venta |
| `SOLD` | Vendida | Final e irreversible |
| `COMPLIMENTARY` | Cortesía | Final, pero no contable |

**Notas generales:**

- Cada entrada tiene un único estado a la vez
- Las transiciones de estado deben ser **atómicas y auditables**

## Objetivo

Backend reactivo que gestione disponibilidad y procesamiento de compras de entradas:

- API reactiva para consultar eventos y realizar compras
- Procesamiento pesado delegado a consumidores asíncronos
- Consistencia de inventario garantizada (sin sobreventa)
- Persistencia NoSQL para acceso rápido
- Orquestación de flujos asíncronos mediante mensajería

## Requisitos Funcionales

1. **Gestión de eventos**: crear y consultar eventos (nombre, fecha, lugar, capacidad total) con inventario de entradas.
2. **Reserva temporal**: al iniciar una compra, reservar entradas por **máximo 10 minutos**; sin confirmación, vuelven al inventario.
3. **Procesamiento asíncrono de compras**: encolar solicitudes inmediatamente y retornar ID de orden; un consumidor valida disponibilidad, actualiza inventario y cambia el estado de la orden.
4. **Consulta de estado de orden**: consultar el estado de una orden en cualquier momento por su identificador.
5. **Control de concurrencia**: mecanismos que eviten sobreventa ante solicitudes concurrentes al mismo evento (condiciones de carrera).
6. **Liberación automática de reservas expiradas**: proceso periódico que libere reservas vencidas sin confirmar.
7. **Consulta reactiva de disponibilidad**: endpoint con disponibilidad en tiempo real (considerando vendidas y reservadas).

## Requisitos Técnicos

### Stack obligatorio

- **Java 25** — Records, Pattern Matching, Virtual Threads si aplica
- **Spring Boot 4.x**
- **Spring WebFlux** — API reactiva no bloqueante
- **Base de datos NoSQL** — DynamoDB Local u otra solución de persistencia
- **Cola de mensajes** — LocalStack/SQS u otra solución equivalente
- **Docker** — contenedorización de app e infraestructura
- **Clean Architecture** — capas Domain / Use Cases / Infrastructure separadas

### Especificaciones

- API completamente reactiva (`Mono` / `Flux`)
- Manejo de errores reactivo con estrategias de retry
- SQS con garantía **at-least-once delivery**
- Actualizaciones de inventario con **optimistic locking** o **conditional writes**
- `docker-compose.yml` que levante todos los servicios

## Entregables

1. **Repositorio** con Clean Architecture:
   - Código y comentarios en inglés
   - Capas claramente separadas
   - Principios SOLID y patrones de diseño
2. **README.md** con:
   - Descripción de la solución
   - Instrucciones de instalación/configuración
   - Comandos Docker
   - Decisiones arquitectónicas
   - Ejemplos de uso de endpoints
3. **Tests unitarios** con cobertura mínima del **90%**:
   - Casos de uso con mocks de repositorios
   - Componentes reactivos (controllers y services WebFlux)
   - Verificación de manejo de concurrencia
   - JUnit 5, Mockito, reactor-test
4. **Colección de solicitudes** (Postman, Insomnia o curl) con los flujos principales
5. **docker-compose.yml** con la app y dependencias (DynamoDB Local, SQS/LocalStack)

## Evaluación

Reunión técnica donde el candidato presenta la solución, explica decisiones de diseño/arquitectura y trade-offs asumidos.

### Criterios

- **Cumplimiento funcional** de los requerimientos
- **Calidad del diseño** (diferencial): patrones, concurrencia/consistencia, asincronía, escalabilidad, resiliencia
- **Seguridad**: manejo de secretos, protección contra reintentos maliciosos, idempotencia, abuso de recursos
- **Comunicación clara**: diagramas de arquitectura, flujo/secuencia, interacciones entre componentes
- **Infraestructura como Código** (diferencial): Terraform con justificación de networking, seguridad, escalabilidad, aislamiento de entornos
- **Experiencia Cloud-Native en AWS** (alto valor): buenas prácticas de seguridad, enfoque a producción, costos, observabilidad, gobernanza
- **Madurez técnica**: reflexión sobre limitaciones, mejoras posibles y qué cambiaría en un entorno productivo real
