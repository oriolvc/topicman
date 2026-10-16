# 4. API first: the OpenAPI document is written before the code

Date: 2026-10-16

## Status

Accepted

## Context

TopicMan's REST and SSE API is consumed by its own frontend and, as an open-source tool, possibly
by scripts and other clients. With a code-first approach (springdoc generating the spec from
controllers), the API contract is a by-product of the implementation: it is reviewed late, shaped
by Java types and easy to change by accident.

## Decision

- The OpenAPI 3.1 document is the source of truth for the HTTP API. Every endpoint, request,
  response and error is designed and reviewed in the spec before any controller code is written.
- One spec file per bounded context in `src/main/resources/openapi/` (`connection.yaml`,
  `messaging.yaml`, `collection.yaml`), sharing common schemas such as `ProblemDetail` from
  `common.yaml`.
- The `openapi-generator` Gradle plugin generates, per context, Java controller interfaces and
  DTOs into that context's `infrastructure.web` package as build output (never committed).
  Controllers implement the generated interfaces, so any drift between spec and code is a
  compile error.
- The frontend generates its TypeScript client from the same spec files.
- springdoc only serves the hand-written spec through Swagger UI; it does not generate it.

## Consequences

- The API is reviewed as a contract, independently of the implementation.
- Backend and frontend can be built in parallel against the same spec.
- A new build plugin and a code generation step are added to the build.
- Generated DTOs follow the generator's style rather than hand-written records; schema names in the
  spec must follow the `Request` / `Response` naming convention.
