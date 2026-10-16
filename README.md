# TopicMan

**Postman for message brokers.** A self-hosted web tool to produce, consume and save test messages
for Apache Pulsar, Apache Kafka and RabbitMQ from one UI.

> Status: early development. Nothing to download yet — see [the plan](docs/PLAN.md) for progress.

## Why

Testing an event consumer usually means writing a throwaway producer, using each broker's CLI, or
fighting an admin console built for operators. TopicMan focuses on the developer's testing loop:

- **Produce** a message to Pulsar, Kafka or RabbitMQ with key, headers and payload
- **Consume** live, without side effects: it never moves offsets, acknowledges messages or steals
  them from real consumers ([ADR 0003](docs/adr/0003-side-effect-free-consumption.md))
- **Save** messages as templates in collections, with variables per environment, versionable in Git

## Run locally (development)

Requirements: Java 25 and Docker.

```bash
./gradlew bootRun      # starts PostgreSQL through Docker Compose automatically
./gradlew check        # format check, tests and architecture rules
```

## Design

Hexagonal architecture in a single Spring Boot service, one adapter per broker behind common ports.
Decisions are recorded as ADRs in [`docs/adr/`](docs/adr/).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[Apache License 2.0](LICENSE)
