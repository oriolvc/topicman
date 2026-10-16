# 2. Hexagonal architecture, packaged by bounded context

Date: 2026-10-16

## Status

Accepted

## Context

TopicMan must support Pulsar, Kafka and RabbitMQ, and more brokers later. The brokers differ in
destinations, acknowledgements and how messages can be read. Broker-specific logic must not leak
into the rest of the system, or every new broker would touch the core.

## Decision

- Use hexagonal architecture (ports and adapters) in a single Spring Boot service.
- Package by bounded context first (`connection`, `messaging`, `collection`), then by layer
  (`domain`, `application`, `infrastructure`).
- The domain defines outbound ports `MessageProducer` and `MessageReader`; each broker is one
  adapter implementing them, using the official client library directly.
- The domain has no framework dependencies. ArchUnit tests enforce the dependency rules.
- A shared contract test suite must pass for every broker adapter.

## Consequences

- Adding a broker means writing one adapter and passing the contract test.
- Mapping between domain, persistence and API models adds some boilerplate.
- Spring broker starters are not used, so connection lifecycle is managed by the adapters.
