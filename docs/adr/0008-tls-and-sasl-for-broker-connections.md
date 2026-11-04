# 8. Connections carry TLS settings and SASL mechanisms, and adapters receive them in full

Date: 2026-11-04

## Status

Accepted

## Context

A connection can store credentials (ADR 0005), but they never reach a broker: `BrokerEndpoint`
holds only the broker type and address, and the connection context's application API never returns
secrets. Phase 5 adds TLS and SASL. Brokers outside a laptop rarely accept anonymous plaintext
connections, so without this TopicMan only works against local brokers.

The brokers differ in how they switch TLS on. Pulsar and RabbitMQ addresses are URLs whose scheme
says so (`pulsar+ssl://`, `amqps://`). Kafka bootstrap servers are a plain `host:port` list with no
scheme. Kafka also offers several SASL mechanisms for a username and password; RabbitMQ and Pulsar
do not need a choice.

## Decision

**Scope.**
- **Kafka:** SASL PLAIN, SCRAM-SHA-256 and SCRAM-SHA-512.
- **Pulsar:** a JWT token.
- **RabbitMQ:** a username and password.
- **TLS:** server TLS for all three, trusting the JVM's default CAs, or one or more CA certificates
  pasted in PEM form. The hostname is always verified, and there is no switch to skip
  verification.
- **Out of scope:** client certificates (mutual TLS), OAUTHBEARER, Kerberos and skipping
  verification. They would each need a later ADR.

**SASL mechanism.** `Credentials.UsernamePassword` gains a nullable `SaslMechanism`:
- **Kafka:** it is required. The API defaults it to `PLAIN` when it is left out.
- **RabbitMQ:** the domain rejects it.

The mechanism is not a secret. It is stored in a plain column, and responses return it, so a client
can show it without decrypting the credentials.

**TLS settings.** A `Connection` holds `TlsSettings(enabled, trustedCertificates)`, next to its
credentials:
- **`enabled`:** only Kafka sets it. For Pulsar and RabbitMQ the address scheme switches TLS on,
  and the domain rejects `enabled = true` for them, so each broker has one place that says whether
  TLS is used.
- **`trustedCertificates`:** optional for every broker. It is a `TrustedCertificates` value object
  that parses its PEM with the JDK's `CertificateFactory` (no framework, so it stays in the domain),
  so a malformed certificate is a 400 when the connection is saved, not a failure later. It allows
  at most 64 KiB and at least one certificate. Certificates are public, so they are stored in plain
  text and returned by the API.
- **Certificates on a plaintext address:** the adapter ignores them. The UI shows the certificate
  field only when TLS is in use.

**Storage.** A new migration, `V2`, adds three columns to `broker_connection`:
- `sasl_mechanism`, nullable;
- `tls_enabled`, `NOT NULL DEFAULT false`;
- `tls_trusted_certificates`, nullable `TEXT`.

The encrypted credential layout from ADR 0005 does not change.

**Getting credentials to the adapters.** The `connection` application layer gets one new use case,
`GetBrokerAccessUseCase`. It is the only API that returns decrypted credentials:
- **Who calls it:** only the messaging context's `BrokerEndpoints` port does. No controller calls
  it, so secrets still never leave the server.
- **What it returns:** a `BrokerAccessResult` with the address, the credentials and the TLS
  settings.
- **Masking:** `Secret` moves to the shared kernel (`shared.domain`), so both contexts use the type
  whose `toString()` is `****`.

**Messaging model.** `BrokerEndpoint` gains two messaging-domain types, `Authentication` (`None`,
`Token`, `UsernamePassword` with an optional mechanism) and `Tls` (`enabled`, trusted PEM). These
mirror the connection types but don't import them, as ADR 0002 forbids.

**Adapters.** Only the JDK and the existing clients are used, with no new dependency.

| Broker   | TLS                                                                          | Authentication |
|----------|------------------------------------------------------------------------------|----------------|
| Kafka    | `security.protocol` is `SSL` or `SASL_SSL` when `enabled`; trust is set with `ssl.truststore.type=PEM` and `ssl.truststore.certificates` | `sasl.mechanism` and `sasl.jaas.config` (`PlainLoginModule` or `ScramLoginModule`), with quotes and backslashes escaped. Kafka logs this setting as `[hidden]` |
| Pulsar   | `pulsar+ssl://`, with `enableTlsHostnameVerification(true)`, which is off by default in the client. Trust comes from a PEM file written to a temp file, as the client only takes a path; the certificate is public | `AuthenticationFactory.token` |
| RabbitMQ | `amqps://`, with an `SSLContext` built from the PEM and `enableHostnameVerification()` | Username and password on the `ConnectionFactory`. These override any user info in the URI |

**Tests.**
- **Integration tests:** one per adapter, against a Testcontainers broker that needs TLS and
  authentication. They produce and read a message, and check that a wrong password or token and an
  untrusted certificate give a failed connection test.
- **Kafka mechanisms:** the Kafka test covers PLAIN, SCRAM-SHA-256 and SCRAM-SHA-512.
- **Certificates:** a test-only CA and a server certificate for `localhost` and `127.0.0.1` are
  committed under `src/test/resources/tls/`, with the script that made them. They are valid for 100
  years and must never be used outside tests.

## Consequences

- Saved credentials are finally used: connections to secured brokers work.
- A second API returns secrets. It is limited to the application layer. Its only caller is
  messaging's `BrokerEndpoints`, enforced by an ArchUnit rule.
- Adding a broker still changes `application` code in neither context, but its adapter must map
  `Authentication` and `Tls`.
- Clusters that need client certificates or OAuth can't be reached yet.
- Pulsar writes the trusted certificates to a temp file, deleted when the client closes.
