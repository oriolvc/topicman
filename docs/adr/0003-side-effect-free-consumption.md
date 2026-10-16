# 3. Consuming never affects real consumers

Date: 2026-10-16

## Status

Accepted

## Context

Developers point TopicMan at shared dev and staging brokers. If reading messages in TopicMan
moved offsets, acknowledged messages or removed them from queues, it would silently break the
services under test.

## Decision

Consume sessions are read-only and ephemeral on every broker:

| Broker | Mechanism | Read from the beginning |
| --- | --- | --- |
| Pulsar | Reader API, no durable subscription | Yes (`MessageId.earliest`) |
| Kafka | Consumer without `group.id`, `assign()` partitions, no offset commits | Yes (`seekToBeginning`) |
| RabbitMQ | Exclusive, auto-delete temporary queue bound to the exchange | No, only new messages |

The domain exposes per-broker capabilities so the UI can explain limitations instead of hiding them.

## Consequences

- TopicMan is safe to use against shared environments.
- RabbitMQ sessions cannot show messages published before the session started.
- Reading from existing RabbitMQ queues is out of scope for the MVP.
