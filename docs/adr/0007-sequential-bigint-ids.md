# 7. Stored aggregates have sequential bigint ids allocated by their repository

Date: 2026-11-03

## Status

Accepted

## Context

Every stored row has a random UUID today, generated in the domain (`ConnectionId.newId()`,
`TemplateId.newId()`). We prefer sequential `bigint` ids in PostgreSQL: they are smaller, index and
sort better, and are easier to read in URLs, logs and SQL. Sequential ids are guessable and reveal
how many rows exist, which matters little for a self-hosted, single-user tool whose exports carry no
ids (ADR 0006).

Two constraints shape the change. Aggregates must always have an id, including before they are
stored, as the domain refers to them by id (a collection checks template ids, credentials are
encrypted with the connection id as associated data, ADR 0005). And V1 and V2 are already pushed,
while `CLAUDE.md` forbids editing a migration on `main`.

## Decision

**Bigint ids from sequences.** Connections, collections, message templates and environments get
`BIGINT` primary keys, each table with its own sequence. Id value objects (`ConnectionId`,
`CollectionId`, `TemplateId`, `EnvironmentId`) wrap a positive `long`.

**The repository allocates ids.** Each repository port gets `nextId()` (and the collection
repository `nextTemplateId()`), which takes the next value of the table's sequence. The application
layer asks for an id and passes it to the factory (`Connection.create(id, ...)`), so an aggregate
never exists without its id and the domain stays free of persistence. Ids are never reused: a
sequence value taken by a failed request is skipped.

**Consume session ids stay UUIDs.** Sessions live in memory, not in PostgreSQL, and their id is what
a client needs to attach to a session's stream, so it must stay unguessable.

**API.** Ids of stored resources are JSON integers (`int64`, minimum 1) in bodies and path
parameters, in every context spec.

**Migrations are squashed once.** Nothing is released, so V1 to V3 are replaced by a single V1 that
creates the schema with bigint ids. This is a one-off exception to "never edit a migration on
`main`", made before v0.1.0: an existing local database must be dropped once, as Flyway rejects the
changed checksums. The rule applies again from here on.

**Credentials.** The associated data of an encrypted credential becomes the connection's bigint id.
Credentials stored under a UUID are not converted; they go with the dropped database.

## Consequences

- Creating an aggregate takes one extra round trip to the database for its id.
- Ids reveal creation order and row counts to anyone who can call the API.
- Every developer database created before this change must be dropped once.
- In-memory test repositories allocate ids from a counter, like a sequence.
