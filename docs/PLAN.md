# TopicMan — Project plan

Status legend: `[ ]` todo · `[x]` done. Tick a task only when `./gradlew check` and the frontend
checks pass. One task at a time; each phase ends with something publishable.

## MVP goal

Someone with no context runs `docker compose -f compose.demo.yaml up` and, in under 5 minutes,
produces a message and sees it arrive on Pulsar, Kafka and RabbitMQ from the UI.

## MVP scope

In:

1. **Connections**: create, edit, test and delete connections to Pulsar, Kafka and RabbitMQ.
   Auth: Pulsar JWT + TLS; Kafka SASL PLAIN/SCRAM + TLS; RabbitMQ user/password + TLS.
   Credentials encrypted at rest and never returned by the API.
2. **Produce**: destination (topic, or exchange + routing key), key, headers, JSON or text payload;
   JSON well-formedness check; send 1 or N copies; show the broker acknowledgement.
3. **Consume without side effects**: ephemeral session from latest or earliest, streamed live over
   SSE; shows key, headers, timestamp and formatted payload.
4. **Collections and environments**: saved message templates with `{{variable}}` placeholders
   resolved per environment; YAML export/import.

Out (post-MVP):

- v2: forms generated from a schema registry (Avro, JSON Schema, Protobuf)
- v2: consumer test scenarios (produce to A, assert a matching message on B)
- v2: replay a consumed message, or save it to a collection in one click
- Later: users and roles, broker administration, more brokers (NATS, SQS, MQTT, RabbitMQ Streams)
- Never: a public hosted version that stores third-party credentials

## Phase 0 — Repository foundations

- [x] Spring Boot 4.1 + Java 25 skeleton from Initializr, virtual threads enabled
- [x] PostgreSQL in `compose.yaml`, Docker Compose support, Flyway, Testcontainers for PostgreSQL
- [x] Apache 2.0 `LICENSE`
- [x] Migrate the build from Groovy to Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`) with a version catalog
- [x] Pin the PostgreSQL image version and use a `topicman` database and user in `compose.yaml`
- [x] Set `spring.jpa.hibernate.ddl-auto: validate` and `spring.jpa.open-in-view: false`
- [x] Spotless (palantir-java-format) wired into `./gradlew check`
- [x] ArchUnit `ArchitectureTest` enforcing the layer and context rules from `CLAUDE.md`
- [x] GitHub Actions CI: build and test on every push and pull request
- [x] README stub replacing `HELP.md`, and `CONTRIBUTING.md`
- [x] `openapi-generator` Gradle plugin generating per-context Java interfaces and DTOs from
      `src/main/resources/openapi/`; springdoc serves the hand-written spec (ADR 0004)

## Phase 1 — Kafka end to end (API only)

- [x] `messaging` domain: `Message`, `Destination`, `ConsumeSession`, ports `MessageProducer` and `MessageReader`
- [x] `connection` domain and persistence: `Connection` aggregate, repository, migrations
- [x] Credential encryption (AES-GCM, `TOPICMAN_MASTER_KEY`)
- [x] Kafka adapter: produce with key, headers and acknowledgement
- [x] Kafka adapter: side-effect-free reader (no group, `assign()`, latest or earliest)
- [x] Abstract `BrokerAdapterContractTest`, with Kafka passing it via Testcontainers
- [x] OpenAPI spec for connections, produce and consume sessions (SSE), reviewed before coding
- [x] REST API for connections and produce; SSE endpoint for consume sessions
- [x] Session limits and idle timeout for consume sessions

## Phase 2 — Minimal UI (Kafka only)

- [x] Vite + React + TypeScript + Mantine app in `frontend/`, built by Gradle and bundled into the jar
- [x] TypeScript client generated from the OpenAPI specs
- [x] Connections screen: list, create, edit, delete
- [x] Produce screen: destination, key, headers, JSON payload, send N, result
- [x] Consume screen: live stream with pause, clear and payload formatting

## Phase 3 — Pulsar and RabbitMQ

- [x] Pulsar adapter (producer + Reader API) passing the contract test
- [x] RabbitMQ adapter (publisher confirms + temporary exclusive queue) passing the contract test
- [x] Broker capability model in the domain (e.g. `canReadFromBeginning`) exposed by the API
- [x] Test-connection use case for all three brokers, with a test action on the connections screen
- [x] Produce to an unreachable broker fails fast with a clear problem (today Kafka blocks Send for
      about 10s, then reports "Topic not present in metadata")

## Phase 4 — Collections and environments

- [x] `collection` domain: `MessageTemplate`, `Collection`, `Environment`, variable resolution
- [x] OpenAPI spec for collections and environments, reviewed before coding
- [x] Persistence and REST API for collections and environments
- [x] UI: save from the produce screen, browse collections, switch environment
- [x] YAML export and import

## Phase 5 — Security and packaging

- [x] TLS and SASL options in connections, covered by integration tests
- [ ] Mask credentials in the `toString()` of the generated request DTOs, so logging a request
      never prints a password or token
- [ ] Production Docker image published to GHCR on release
- [x] `compose.demo.yaml` with PostgreSQL, Pulsar, Kafka and RabbitMQ
- [ ] App binds to localhost by default; documented way to expose it

## Phase 6 — Launch

- [ ] README: 20-second GIF, quick start, broker differences table
- [ ] ADRs up to date
- [ ] Release v0.1.0
- [ ] LinkedIn post explaining the design
