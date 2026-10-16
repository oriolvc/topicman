---
paths:
  - "src/test/**/*.java"
  - "frontend/src/**/*.test.{ts,tsx}"
---

# Testing conventions

## Backend

- JUnit 5 + AssertJ. No Hamcrest, no `assertEquals`.
- Test class mirrors the class under test: `ProduceMessageUseCaseTest`, in the same package.
- Test method names describe behaviour in camelCase: `rejectsPayloadThatIsNotValidJson()`.
- Structure every test as given / when / then, separated by a blank line (no comments needed).
- Domain tests use no Spring context, no mocks and no I/O.
- Use case tests use in-memory fakes of outbound ports (e.g. `InMemoryConnectionRepository`), not
  Mockito. Mockito only for third-party types that cannot be faked reasonably.
- Integration tests are suffixed `IT` and use Testcontainers with real brokers and PostgreSQL;
  never mock a broker client.
- Every broker adapter extends the shared abstract `BrokerAdapterContractTest`. A behaviour that
  differs per broker (e.g. RabbitMQ cannot read from the beginning) is expressed through an
  explicit capability in the contract, never by skipping a test.
- Architecture rules live in `ArchitectureTest` (ArchUnit). Never relax a rule to make code pass;
  change the code, or propose an ADR.
- No `Thread.sleep`. Wait for async results with Awaitility.
- Test data via small builder or factory methods (`aConnection()`), not shared mutable fixtures.

## Frontend

- Vitest + Testing Library. Query by role and accessible name, not by CSS class or test id.
- Mock the network with MSW, never by mocking TanStack Query or fetch directly.
- Test user-visible behaviour, not component internals or Mantine implementation details.
