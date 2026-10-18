# Ticketing Platform

Reactive, event-driven backend for processing ticket purchases without overselling.
Built with Java 25, Spring Boot 4 / WebFlux, DynamoDB and SQS following Clean Architecture.

## Solution overview

```
 Client ──HTTP──► WebFlux API ──► Use cases ──► DynamoDB (events / tickets / orders)
                     │                              ▲
                     └── publish ──► SQS ──► Consumer ┘  (reserve tickets, conditional writes)
                                               │
                                     Scheduler: release expired reservations
```

1. `POST /api/v1/orders` persists a `PENDING` order, publishes a message to SQS and returns `202 Accepted` with the order id.
2. A long-polling consumer reserves tickets with **conditional writes on a `version` attribute** (optimistic locking). Two consumers can never hold the same ticket; if there is not enough inventory the order is `REJECTED`.
3. Tickets stay `RESERVED` for at most 10 minutes. `POST /api/v1/orders/{id}/confirm` moves them to `SOLD` and the order to `CONFIRMED`.
4. A scheduled job releases expired reservations back to `AVAILABLE` and marks orders as `EXPIRED`.
5. `GET /api/v1/events/{id}/availability` reads live counters from a GSI (`available`, `reserved`, `sold`).

### Ticket state machine

```
AVAILABLE ──► RESERVED ──► PENDING_CONFIRMATION ──► SOLD
    ▲             │                 │
    └─────────────┴─────────────────┘   (release / expiration)
AVAILABLE ──► COMPLIMENTARY
```

## Project layout (Clean Architecture)

```
src/main/java/com/nequi/ticketing
├── domain/            # Pure business model: records, state machines, ports, exceptions
│   ├── model/
│   ├── port/
│   └── exception/
├── application/       # Use cases (framework-agnostic, Reactor types only)
│   └── usecase/
└── infrastructure/    # Adapters + Spring wiring
    ├── adapter/web/           # WebFlux controllers, DTOs, error handling
    ├── adapter/persistence/   # DynamoDB Enhanced Async Client repositories
    ├── adapter/messaging/     # SQS publisher and consumer
    ├── adapter/scheduler/     # Expired reservation sweep
    └── config/                # Properties, AWS clients, use case beans
```

Dependencies point inwards: `infrastructure -> application -> domain`.

## Requirements

- Docker + Docker Compose (everything can run in containers)
- Optional for local development: JDK 25 (Gradle wrapper is included)

## Running with Docker

```bash
# Build the app image and start LocalStack (DynamoDB + SQS) and the API
docker compose up --build

# Follow logs
docker compose logs -f app

# Stop and remove containers
docker compose down -v
```

LocalStack provisions the tables and queues on startup via `docker/localstack/init-aws.sh`.
The API is available at `http://localhost:8080`, health at `/actuator/health`.

## Running locally (without the app container)

```bash
docker compose up localstack          # infrastructure only
cp .env.example .env                  # adjust if needed
./gradlew bootRun                     # uses application.yml defaults -> localhost:4566
```

## Configuration

All settings are environment variables (see `.env.example`):

| Variable | Default | Description |
|---|---|---|
| `AWS_REGION` | `us-east-1` | AWS region |
| `AWS_ENDPOINT` | _(empty)_ | Endpoint override for LocalStack. Empty = real AWS |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | _(empty)_ | Local only. In AWS the default credential chain (IAM role) is used |
| `DYNAMODB_*_TABLE` | `events`, `tickets`, `orders` | Table names |
| `SQS_PURCHASE_QUEUE_URL` | LocalStack URL | Purchase queue |
| `RESERVATION_TTL` | `10m` | Reservation hold time |
| `RESERVATION_CLEANUP_INTERVAL` | `30s` | Expired reservation sweep interval |

Secrets are never stored in the repository; `.env` is git-ignored.

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/events` | Create event and its ticket inventory |
| `GET` | `/api/v1/events` | List events |
| `GET` | `/api/v1/events/{id}` | Event detail |
| `GET` | `/api/v1/events/{id}/availability` | Real-time availability |
| `POST` | `/api/v1/orders` | Place order (requires `Idempotency-Key` header) → `202` |
| `GET` | `/api/v1/orders/{id}` | Order status |
| `POST` | `/api/v1/orders/{id}/confirm` | Confirm a reserved order |

Examples: `requests/flows.sh` (curl, end-to-end flow) and `requests/ticketing.http` (REST Client).

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 6f1c2a1e-1111-4b2b-9c3d-000000000001" \
  -d '{"eventId":"<event-id>","customerId":"customer-1","quantity":2}'
```

Errors follow RFC 9457 (`application/problem+json`): `400` validation, `404` not found, `409` conflicts
(insufficient availability, invalid state transition, concurrent modification).

## Testing

```bash
./gradlew test                              # unit tests + JaCoCo report (build/reports/jacoco)
./gradlew jacocoTestCoverageVerification    # enforces 90% line coverage
```

Tests use JUnit 5, Mockito and `reactor-test` (`StepVerifier`).

## Architectural decisions

- **Clean Architecture**: domain and use cases have no Spring dependency; adapters are swapped via ports.
- **Asynchronous processing**: the API only validates and enqueues; heavy work happens in SQS consumers, which scale independently.
- **No overselling**: every ticket transition is a DynamoDB conditional write (`version = expected`). Lost races surface as `ConcurrentModificationException` and the consumer simply tries the next candidate ticket.
- **Idempotency**: clients send an `Idempotency-Key`; replays return the existing order. Consumers ignore messages for orders no longer `PENDING`, so SQS at-least-once redelivery is safe.
- **Resilience**: Reactor `retryWhen` with exponential backoff for SQS publishing and polling; a DLQ receives messages after 5 failed attempts.
- **Time as a dependency**: `java.time.Clock` is injected so expiration logic is deterministic in tests.
- **DynamoDB access patterns**: GSIs `eventId-status-index` (availability, candidate tickets), `orderId-index` (confirmation), `status-reservedUntil-index` (expiration sweep), `idempotencyKey-index` (duplicate detection).

### Known limitations / next steps

- The expiration job runs in every instance; in production use a single scheduler (e.g. EventBridge → Lambda or a leader lock).
- Candidate-ticket over-fetching (`x3`) can still reject under extreme contention; a retry loop or an atomic availability counter per event would tighten this.
- Add Terraform for DynamoDB tables, SQS/DLQ, ECS/Fargate service, IAM least-privilege roles and VPC networking.
- Add rate limiting / WAF in front of the API and authentication for customers.
