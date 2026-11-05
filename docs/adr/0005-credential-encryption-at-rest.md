# 5. Credentials are encrypted at rest with AES-GCM and a master key

Date: 2026-10-16

## Status

Accepted. ADR 0010 adds a generated key for the embedded database only.

## Context

Connections store broker credentials: Pulsar JWT tokens, Kafka SASL usernames and passwords, and
RabbitMQ usernames and passwords. A copy of the database (a backup, a leaked dump, a shared dev
volume) must not reveal them. `CLAUDE.md` requires AES-GCM with a master key from
`TOPICMAN_MASTER_KEY`, but leaves open the key format, the stored layout, where in the hexagon
encryption happens and what happens without a key.

## Decision

**Domain model.** The `connection` domain gets a sealed `Credentials` type: `None`, `Token(secret)`
and `UsernamePassword(username, password)`. A `Connection` holds one, and the domain checks that
the broker supports it (for example, no `Token` for RabbitMQ). Secret values are wrapped in a
`Secret` value object whose `toString()` is `****`, so a credential cannot leak through logging,
exception messages or test failure output. The domain holds plaintext and never deals with
encryption. SASL mechanisms (PLAIN, SCRAM) and TLS settings come with Phase 5.

**Encryption at the persistence boundary.** The persistence adapter encrypts credentials on write
and decrypts them on read, through a `CredentialCipher` in `connection.infrastructure.crypto`.
The domain and application layers never see ciphertext, and the plaintext exists only in memory.

**Key.** `TOPICMAN_MASTER_KEY` is a Base64-encoded 256-bit key (generate one with
`openssl rand -base64 32`). It is bound to `topicman.crypto.master-key` through a
`@ConfigurationProperties` record. Passphrases and key derivation are not supported: a random key
is stronger, and it avoids choosing KDF parameters. TopicMan never ships a default key. Startup
fails with a clear message if the key is missing, is not valid Base64 or is not 32 bytes, so a
misconfigured instance can never write credentials it cannot read back.

**Algorithm and layout.** AES-256-GCM from the JDK (`javax.crypto`), with no new dependency. Each
encryption uses a fresh random 96-bit IV from `SecureRandom` and a 128-bit tag. The stored value
is a single `bytea` column:

```
version (1 byte, 0x01) | IV (12 bytes) | ciphertext + tag
```

The plaintext is the credentials serialized as JSON by the adapter. The connection id is the
additional authenticated data (AAD), so ciphertext copied into another row does not decrypt. The
credentials type (`NONE`, `TOKEN`, `USERNAME_PASSWORD`) is stored in a plain column so connections
can be listed without decrypting.

**Failures.** A row that fails to decrypt (wrong key, tampered data) raises an infrastructure
exception whose message names the connection id and never includes key or credential material.

**Out of scope.** Key rotation and multiple keys. The version byte leaves room to add a key id
later, through a new ADR.

## Consequences

- A database copy without the master key does not reveal credentials.
- Losing the master key means re-entering every credential. The README must say so.
- Every environment, including local `bootRun` and the demo compose file, must set
  `TOPICMAN_MASTER_KEY`. Tests use a fixed test-only key.
- Rotating the key requires a future migration tool; it cannot be done in place today.
- Plaintext credentials live in memory while a use case runs. That is accepted: TopicMan has to
  hand them to the broker clients anyway.
