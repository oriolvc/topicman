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

Requirements: Java 25, plus Docker or a local PostgreSQL.

Create a master key once and keep it (see [Master key](#master-key)), for example:

```bash
mkdir -p ~/.config/topicman
echo "export TOPICMAN_MASTER_KEY='$(openssl rand -base64 32)'" > ~/.config/topicman/env
source ~/.config/topicman/env
```

Then start the backend in one of two ways:

```bash
./gradlew bootRun                              # starts PostgreSQL with Docker Compose
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun  # uses the PostgreSQL on this machine
```

The `local` profile ([`application-local.yaml`](src/main/resources/application-local.yaml)) expects
a `topicman` database owned by a `topicman` user with password `topicman`, as in `compose.yaml`.
Create them once:

```bash
sudo -u postgres psql \
  -c "CREATE ROLE topicman LOGIN PASSWORD 'topicman';" \
  -c "CREATE DATABASE topicman OWNER topicman;"
```

Either way the backend serves the API and the UI at <http://localhost:8080>; Gradle downloads its
own Node.js to build the UI. To work on the UI with hot reload (Node.js 22.22 or later), keep the
backend running and start the Vite dev server, which proxies API calls to it:

```bash
cd frontend
npm ci
npm run dev            # http://localhost:5173
```

Run every check before pushing. The integration tests start PostgreSQL and Kafka with
Testcontainers, so they need Docker even when you develop with the `local` profile:

```bash
./gradlew check        # format check, tests and architecture rules, frontend checks
```

Producing and consuming need a running broker, such as a local Kafka on `localhost:9092`.

### Master key

Broker credentials are encrypted at rest with the key in `TOPICMAN_MASTER_KEY`
([ADR 0005](docs/adr/0005-credential-encryption-at-rest.md)). It must be a Base64-encoded 256-bit
key; TopicMan does not start without one. Store it safely and reuse it: **if you lose the key, the
saved credentials cannot be recovered** and must be entered again.

## Design

Hexagonal architecture in a single Spring Boot service, one adapter per broker behind common ports.
Decisions are recorded as ADRs in [`docs/adr/`](docs/adr/).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[Apache License 2.0](LICENSE)
