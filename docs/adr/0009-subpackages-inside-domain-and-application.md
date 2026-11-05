# 9. Subpackages inside domain and application

Date: 2026-11-05

## Status

Accepted

## Context

ADR 0002 packages by bounded context, then by layer. The layers have grown flat: `collection` has 36
files in `application` and 34 in `domain`, so a repository port, a value object and an exception sit
side by side, and finding the use cases means reading past their commands and results.

## Decision

Each bounded context splits its two inner layers by the role of each class:

```
<context>/domain/       model/      aggregates, value objects, domain services
                        port/       outbound ports, always interfaces
                        exception/  domain exceptions
<context>/application/  usecase/    use cases
                        model/      commands, results and the other types use cases expose
                        helper/     what use cases share, such as `ActiveSession`, when any
```

- Singular names, as in the rest of the code base (`infrastructure/web`).
- `shared/domain` stays flat. It is a small kernel of seven files.
- Helpers have their own package, so a use case folder lists only use cases. They are public as a
  result, so ArchUnit keeps everything outside the application layer away from them: for example,
  only the consume session use cases may change the state of an `ActiveSession`.
- ArchUnit enforces the layout. It rejects classes directly in `domain` or `application`, use cases,
  commands, results or domain exceptions outside their subpackage, anything but interfaces in
  `port`, and use of `helper` from outside the application layer.

## Consequences

- The role of a class can be read from its package, and flat folders cannot come back unnoticed.
- Domain exception constructors, the `of` factories of results and the helpers were package-private
  and must now be public, as their callers are in another package. Code outside the use cases can
  therefore build them, except for the helpers, which ArchUnit guards. This is a real loss of encapsulation, accepted for the clearer layout; the rule that decrypted
  credentials reach only broker endpoints is still enforced by ArchUnit (ADR 0008).
- Packages inside `domain` depend on each other in both directions, as models throw exceptions that
  name models. No rule forbids this: the subpackages organise one layer, they are not layers.
