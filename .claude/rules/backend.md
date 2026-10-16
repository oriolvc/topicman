---
paths:
  - "src/main/**/*.java"
---

# Backend conventions

## Package structure per bounded context

```
com.oriolvc.topicman.<context>
├── domain/          aggregates, entities, value objects, domain events, domain exceptions,
│                    outbound ports (e.g. ConnectionRepository, MessageProducer)
├── application/     one class per use case, commands, results
└── infrastructure/
    ├── web/         REST and SSE controllers, request/response DTOs, exception mapping
    ├── persistence/ JPA entities, Spring Data repositories, mappers to the domain
    └── broker/      broker adapters: kafka/, pulsar/, rabbitmq/ (messaging context only)
```

## Domain

- Value objects are `record`s that validate in the compact constructor and throw a domain exception.
- Aggregates expose behaviour, not setters: `connection.rename(name)`, not `setName(name)`.
- Aggregates are created through a static factory (`Connection.create(...)`) and rebuilt from
  persistence through `Connection.rehydrate(...)`, which does not raise creation events.
- Domain events are past-tense records: `ConnectionCreated`, `MessageProduced`.
- Domain exceptions extend `DomainException` and carry a stable error code.
- Every package has a `package-info.java` with `@NullMarked` (JSpecify); mark nullable values with
  `@Nullable`. Use `Optional` only as a return type.

## Application

- One use case per class, named `<Verb><Noun>UseCase` (e.g. `ProduceMessageUseCase`), with a single
  public method `execute(<Verb><Noun>Command command)`.
- Commands and results are records. Use cases never receive or return web DTOs or persistence
  entities.
- Transactions start in the application layer (`@Transactional` on use cases), never in controllers
  or the domain.

## Infrastructure

- Constructor injection only; no field injection, no `@Autowired`.
- Controllers are thin: map DTO → command, call the use case, map result → DTO.
- Never expose domain objects in the API. DTOs are records suffixed `Request` / `Response`.
- API paths are versioned and plural: `/api/v1/connections/{id}`.
- Errors are RFC 9457 `ProblemDetail`, mapped from domain error codes in one `@RestControllerAdvice`
  per context.
- JPA entities are separate from domain objects and mapped explicitly; JPA and Spring Data
  annotations never appear in `domain`. Entities live only in `infrastructure/persistence`.
- Schema is owned by Flyway; Hibernate never generates DDL (`ddl-auto: validate`).
- No lazy-loading surprises: no `open-in-view`, fetch what a use case needs explicitly.
- Flyway migrations live in `src/main/resources/db/migration` as
  `V<n>__<snake_case_description>.sql`. Never edit a migration that is already on `main`.
- Broker adapters hide every client type: no `ConsumerRecord`, Pulsar `Message<?>` or AMQP type
  leaves the adapter package.
- Configuration through `@ConfigurationProperties` records with the `topicman.` prefix.

## Style

- Formatting is owned by Spotless (palantir-java-format); never hand-format, run `./gradlew spotlessApply`.
- No Lombok. Use records and modern Java (pattern matching, sealed types, switch expressions).
- Immutability by default; collections returned from the domain are unmodifiable.
- Names over comments. Comment only the *why* of non-obvious decisions.
- Log with SLF4J placeholders; never log payloads at INFO or above, never log credentials.
