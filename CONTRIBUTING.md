# Contributing to TopicMan

Thanks for your interest! TopicMan is in early development, so the best way to help right now is
to open an issue with feedback, a bug report or a use case you'd like covered.

## Before you start

- Read [`docs/PLAN.md`](docs/PLAN.md) for scope and current status, and the ADRs in
  [`docs/adr/`](docs/adr/) for the decisions behind the design.
- For anything larger than a small fix, open an issue first so we can agree on the approach.
- Changes to the HTTP API start in the OpenAPI spec (`src/main/resources/openapi/`), not in code
  ([ADR 0004](docs/adr/0004-api-first-with-openapi.md)).

## Development setup

Requirements: Java 25 and Docker (used by Docker Compose and Testcontainers).

```bash
./gradlew bootRun         # run the app; PostgreSQL starts via Docker Compose
./gradlew check           # format check, all tests, architecture rules
./gradlew spotlessApply   # format the code (palantir-java-format)
```

## Conventions

- **Architecture:** hexagonal, packaged by bounded context then layer. `domain` code has no
  framework dependencies. `ArchitectureTest` enforces this; don't relax its rules to make code pass.
- **Tests:** test-first for domain and application code. Integration tests run against real brokers
  and PostgreSQL with Testcontainers.
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/), scoped by context when
  relevant, e.g. `feat(messaging): add Kafka reader`.
- **Pull requests:** keep them focused, and make sure `./gradlew check` passes. CI runs it on every
  pull request.

## Developer Certificate of Origin

Sign off your commits (`git commit -s`) to certify you have the right to submit them under the
project's license, as described in the [DCO](https://developercertificate.org/).

## License

By contributing, you agree that your contributions are licensed under the
[Apache License 2.0](LICENSE).
