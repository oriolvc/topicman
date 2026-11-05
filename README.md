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
PostgreSQL on port 5431, so it never clashes with a container publishing 5432, and a `topicman`
database owned by a `topicman` user with password `topicman`, as in `compose.yaml`. Set
`SPRING_DATASOURCE_URL` to use another address.
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

Run every check before pushing. The integration tests start PostgreSQL, Kafka, Pulsar and RabbitMQ
with Testcontainers, so they need Docker even when you develop with the `local` profile:

```bash
./gradlew check        # format check, tests and architecture rules, frontend checks
```

Producing and consuming need a running broker. To try all three, start the app with the `demo`
profile: it runs PostgreSQL, Kafka, Pulsar and RabbitMQ from
[`compose.demo.yaml`](compose.demo.yaml) and removes them when the app stops. That file lists the
address of each broker to enter in the UI:

```bash
SPRING_PROFILES_ACTIVE=demo ./gradlew bootRun
```

For a throwaway Kafka on `localhost:9092` alongside the default setup:

```bash
docker compose -f compose.kafka.yaml up -d     # stop it with `down`; its data is discarded
```

### Master key

Broker credentials are encrypted at rest with the key in `TOPICMAN_MASTER_KEY`
([ADR 0005](docs/adr/0005-credential-encryption-at-rest.md)). It must be a Base64-encoded 256-bit
key; TopicMan does not start without one. Store it safely and reuse it: **if you lose the key, the
saved credentials cannot be recovered** and must be entered again.

### Secured brokers

Connections authenticate and use TLS as each broker does
([ADR 0008](docs/adr/0008-tls-and-sasl-for-broker-connections.md)):

| Broker   | Authentication                                        | TLS                         |
|----------|-------------------------------------------------------|-----------------------------|
| Kafka    | Username and password with SASL PLAIN, SCRAM-SHA-256 or SCRAM-SHA-512 | the connection's "Use TLS" switch |
| Pulsar   | JWT token                                             | a `pulsar+ssl://` address   |
| RabbitMQ | Username and password                                 | an `amqps://` address       |

TopicMan always verifies the broker's certificate and hostname. For a broker whose certificate a
private or self-signed CA issued, paste that CA's certificate (PEM) into the connection's trusted
certificates; otherwise the JVM's default CAs are trusted. Client certificates (mutual TLS) are not
supported yet.

To try it, the `demo` profile's brokers also listen with authentication and TLS: Kafka on
`localhost:9094`, Pulsar on `pulsar+ssl://localhost:6651` and RabbitMQ on `amqps://localhost:5671`.
The header of [`compose.demo.yaml`](compose.demo.yaml) lists their demo credentials and the CA
certificate to trust.

### Expose TopicMan

TopicMan listens on `127.0.0.1` by default, so only this machine can reach it. **It has no login:
anyone who can reach it can use every saved connection**, and so produce to and read from your
brokers with the saved credentials, although the API never returns them.

To reach it from another machine, prefer an SSH tunnel, which needs no change:

```bash
ssh -L 8080:localhost:8080 user@topicman-host     # then open http://localhost:8080
```

To expose it, put it behind a reverse proxy that handles authentication and TLS, on the same
machine, or set the address to listen on, and restrict who can reach it with a firewall:

```bash
SERVER_ADDRESS=0.0.0.0 ./gradlew bootRun          # every network interface
```

## Design

Hexagonal architecture in a single Spring Boot service, one adapter per broker behind common ports.
Decisions are recorded as ADRs in [`docs/adr/`](docs/adr/).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[Apache License 2.0](LICENSE)
