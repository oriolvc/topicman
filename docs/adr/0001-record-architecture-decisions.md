# 1. Record architecture decisions

Date: 2026-10-16

## Status

Accepted

## Context

TopicMan is an open-source project meant to be read by contributors and reviewers. Decisions
about architecture and trade-offs need to be visible and stay discoverable over time.

## Decision

Record every significant architectural decision as an Architecture Decision Record in `docs/adr/`,
using the format by Michael Nygard: Title, Status, Context, Decision, Consequences.
Files are numbered sequentially: `NNNN-short-title.md`. An ADR is never edited once accepted;
a new ADR supersedes it.

## Consequences

- New contributors can understand why the system looks the way it does.
- A change that conflicts with an accepted ADR requires a new ADR first.
