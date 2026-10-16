# TopicMan

"Postman for message brokers": a self-hosted web tool to produce, consume and save test messages
for Apache Pulsar, Apache Kafka and RabbitMQ from one UI. Open source (Apache 2.0).

- Scope, phases and current status: `docs/PLAN.md`. Read it before starting any task.
- Architecture decisions: `docs/adr/`. Read the relevant ADR before changing what it covers.

## Tech stack

- Backend: Java 25, Spring Boot 4.x, Gradle, Spring Data JPA, PostgreSQL, Flyway, springdoc-openapi
- Broker clients: `kafka-clients`, `pulsar-client`, `amqp-client` used directly (no Spring broker starters)
- Frontend: React + TypeScript (Vite), Mantine, TanStack Query
- Tests: JUnit 5, AssertJ, ArchUnit, Testcontainers; Vitest + Testing Library on the frontend

## Repository layout

```
src/main/java/        Spring Boot application (single Gradle project at the repo root)
src/test/java/        backend tests
frontend/             React app; its build output is bundled into the jar
docs/                 PLAN.md and ADRs
compose.yaml          PostgreSQL for local development
compose.demo.yaml     PostgreSQL + Pulsar + Kafka + RabbitMQ for demos
```

## Commands

- Run backend (starts PostgreSQL via Docker Compose support): `./gradlew bootRun`
- Run frontend dev server: `cd frontend && npm run dev`
- Full verification (format check, tests, ArchUnit): `./gradlew check`
- Format Java code: `./gradlew spotlessApply`
- Frontend checks: `cd frontend && npm run lint && npm run typecheck && npm test`

## Architecture (non-negotiable)

Hexagonal architecture, packaged by bounded context, then by layer.

- Base package: `com.oriolvc.topicman`
- Bounded contexts: `connection` (broker connections, credentials), `messaging` (produce, consume
  sessions, broker adapters), `collection` (message templates, collections, environments)
- Each context has `domain`, `application` and `infrastructure` packages; shared kernel in `shared`
- Dependencies point inward: `infrastructure` → `application` → `domain`. Never the reverse.
- `domain` has zero framework imports (no Spring, no JPA, no Jackson, no broker clients).
  ArchUnit tests enforce this; never weaken or skip them.
- Contexts talk through application-layer APIs or domain events, never through another context's
  `domain` or `infrastructure` internals.
- Each broker is one outbound adapter implementing `MessageProducer` and `MessageReader`. Adding a
  broker must not change `domain` or `application` code.

## Core domain rules

- Consuming never changes the state of real consumers:
  - Pulsar: Reader API, never a durable subscription
  - Kafka: no `group.id`, `assign()` partitions, never commit offsets
  - RabbitMQ: exclusive, auto-delete temporary queue bound to the exchange; never read from an
    existing queue
- Credentials are encrypted at rest (AES-GCM, master key from `TOPICMAN_MASTER_KEY`), never logged,
  never returned by the API, never included in exceptions or test snapshots.

## Workflow

- Work on one task from `docs/PLAN.md` at a time. When it is done and `./gradlew check` passes,
  tick its checkbox in `docs/PLAN.md`.
- Test-first for `domain` and `application` code: write the failing test, then the code.
- Ask before adding any new dependency, and say why it is needed.
- If a change needs an architectural decision not covered by an ADR, stop and propose a new ADR
  in `docs/adr/` before implementing.
- Commits follow Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `build:`),
  scoped by context when relevant: `feat(messaging): add Kafka reader`.
- A task is not done until `./gradlew check` and the frontend checks pass. Never report success
  with failing or skipped tests.
- Code, comments, docs and commit messages are in English.

Detailed conventions load automatically from `.claude/rules/` when working in matching files.
