# 10. Embedded database and generated master key

Date: 2026-11-05

## Status

Accepted

## Context

Running TopicMan needs a PostgreSQL database and a master key. That is too much for someone trying
it out or using it at home: many self-hosted images start with `docker run` alone, and we want the
same. Two things block it, the database and `TOPICMAN_MASTER_KEY`, as the app refuses to start
without a key (ADR 0005).

A trial ran the app against H2 2.4 in PostgreSQL mode, through the API and a restart: connections
with encrypted credentials, duplicate names, collections, templates, environments, resolution,
export, cascading deletes and producing all worked, and credentials still decrypted after the
restart. Only the migrations needed changes:

| In the migrations                        | On H2                  | Now                        |
|------------------------------------------|------------------------|----------------------------|
| `TIMESTAMPTZ`                            | Unknown type           | `TIMESTAMP WITH TIME ZONE` |
| A column named `value`                   | A reserved word        | `entry_value`              |
| Several `ADD COLUMN` in one `ALTER`      | Syntax error           | Part of `CREATE TABLE`     |
| `BIGSERIAL` sequences found by name      | Named differently      | Explicit `CREATE SEQUENCE` |

## Decision

**H2 stored on disk is the default database; PostgreSQL is used when configured.**

- The default datasource URL points to an H2 file in `TOPICMAN_DATA_DIR`, which defaults to
  `~/.topicman` and is a volume at `/data` in the Docker image.
- Setting the `SPRING_DATASOURCE_*` variables selects PostgreSQL, as do the `local` and `demo`
  profiles and `./gradlew bootRun`, whose Docker Compose connection takes priority. Development
  therefore keeps using PostgreSQL.
- The URL keeps `MODE=PostgreSQL`, for `nextval('...')` in the repositories.
- The H2 console stays off, and H2 opens no network port.

**One set of migrations, for both databases, until a real limitation appears.** They use SQL both
accept. The existing V1 and V2 are squashed into one V1, a second exception to "never edit a
migration on `main`" after ADR 0007, as nothing is released: every existing database must be dropped
once. If a future migration truly cannot be shared, splitting by database with Flyway's
`db/migration/{vendor}` locations remains possible, and needs its own ADR.

The tests that touch the database also run on H2, in the `embeddedDatabaseTest` task of
`./gradlew check`, so a migration or query that works on only one database fails the build.

**In embedded mode only, a missing master key is generated.** When the app uses the embedded database
and `TOPICMAN_MASTER_KEY` is not set, it creates a random 256-bit key in `master.key` in the data
directory, readable only by its owner, and reuses it from then on. It logs a warning naming the file,
never the key. A key that is set always wins. With PostgreSQL a missing key still stops the app, so
ADR 0005 is unchanged there.

## Alternatives considered

- **One set of migrations per database.** It leaves each database free to use its own SQL, but every
  schema change is written twice, for a limitation we do not have yet.
- **SQLite.** It needs a community Hibernate dialect, and no less migration work.
- **Embedded PostgreSQL.** It keeps a single SQL dialect but ships native binaries per platform, which
  makes the image much bigger and is awkward to run as a non-root user.
- **A purely in-memory database.** Every restart would lose saved work.
- **Always requiring a master key.** It is safer, but `docker run` would no longer work alone.

## Consequences

- `docker run` works with no configuration, and data survives restarts with a volume.
- **A copy of the data directory reveals the credentials**, as it holds both the ciphertext and the
  key. This is weaker than ADR 0005's guarantee and accepted because embedded mode is for testing and
  home use. The warning and the README recommend setting `TOPICMAN_MASTER_KEY` for real credentials.
- Losing `master.key` means entering saved credentials again, as with any lost key.
- The jar grows by about 2.7 MB.
- H2 file formats may change between major versions, so an H2 upgrade can need a fresh data directory,
  and only one process can open the file at a time.
- Flyway logs a warning that H2 2.4 is newer than the version it has verified, 2.3.232. We keep the
  version Spring Boot manages.
- Migrations are limited to SQL both databases accept, and PostgreSQL-only features would force the
  split above.
