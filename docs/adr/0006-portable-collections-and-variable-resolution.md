# 6. Collections hold portable, broker-typed templates resolved against global environments

Date: 2026-11-03

## Status

Accepted

## Context

Phase 4 adds the `collection` context: saved message templates with `{{variable}}` placeholders,
grouped in collections, resolved per environment, and exported to YAML so a team can version them
in Git. Three things are open:

- What a template targets. Connection ids belong to one TopicMan install, so a template bound to
  one would not survive an export and import, and could not be sent to dev and to staging.
- How the `collection` context models destinations and broker rules. ADR 0002 forbids it from
  using the `messaging` domain (`Destination`, `BrokerCapabilities`), and template fields are raw
  text such as `orders-{{env}}`, which is not a valid destination until it is resolved.
- The variable syntax, which fields it applies to, and what happens when a value is missing.

## Decision

**Templates target a broker type, not a connection.** A `MessageTemplate` stores its `BrokerType`
(shared kernel), a destination, an optional key, headers and a payload with its format. The
connection is chosen when sending, among the saved connections of that broker type. The number of
copies is also a send-time choice, not part of the template.

**Model.** Two aggregates, both identified by UUIDs and named uniquely:

- `Collection`: a name and an ordered list of `MessageTemplate` entities, whose names are unique
  within the collection. Collections are flat; there are no folders.
- `Environment`: a name and an ordered set of variables (name and string value). Environments are
  global: one environment applies to every collection, as in Postman.

**Own template types in the `collection` domain.** A template's destination is a
`TemplateDestination`: a topic name, or an exchange name and routing key, all held as unresolved
text. The `collection` domain does not decide which broker accepts which kind of destination or
whether it supports keys: the application layer asks the `messaging` context through its
application API (`ListBrokersUseCase`) when a template is saved, so adding a broker still changes
only `BrokerType` and `BrokerCapabilities` in the domain.

**Variables.**

- Syntax: `{{name}}`, with optional spaces inside the braces (`{{ name }}`). A variable name
  starts with a letter or `_` and continues with letters, digits, `_`, `.` or `-`.
- Resolution applies to every text a message is made of: the destination name, the routing key,
  the key, header names and values, and the payload. It does not apply to the template's name.
- Resolution is a single pass: a value that contains `{{...}}` is inserted as is, never resolved
  again, so values cannot recurse.
- A placeholder without a value fails the whole resolution with every missing name listed at
  once. Nothing is sent with a placeholder left in it. There is no escaping: a literal `{{x}}` in
  a payload must be given a value.
- Text such as a lone `{{` or `{{ not a name }}` is not a placeholder and stays as written.

**Dynamic variables.** Names starting with `$` are built in and generated on every resolution:
`{{$uuid}}` (random UUID), `{{$timestamp}}` (epoch milliseconds), `{{$isoTimestamp}}` (ISO-8601
UTC) and `{{$randomInt}}` (0 to 1000). Each one has a single value per resolution, so
`{{$uuid}}` in the key and in the payload of one message is the same UUID. An unknown `$` name is
an error. Environment variables cannot start with `$`. The domain gets these values through a
port, so resolution stays deterministic in tests.

**Resolution happens in the `collection` context**, and produces plain text values: broker type,
destination, key, headers and payload. Sending the result goes through the existing produce API,
which keeps checking JSON well-formedness and destination support.

## Consequences

- Exported collections and environments are portable between installs and can live in Git.
- Sending a template takes one more choice, the connection, which the UI can preselect when only
  one connection of the template's broker type exists.
- Destination kinds are modelled twice, in `messaging` and in `collection`, as small sealed types
  with different rules (resolved versus raw text).
- Copies of one send share one resolution, so N copies of a template with `{{$uuid}}` carry the
  same UUID. Per-copy resolution can come later without changing the template format.
- Without escaping, a payload that needs a literal `{{name}}` must define `name` with that text.
