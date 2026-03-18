# Project Memory

## Purpose

This document is the fast entry point for active engineering defaults in TellPal V2 backend work.
It summarizes the current decisions that engineers and agents should know before changing code.

Read order:

1. root `AGENTS.md`
2. this file
3. relevant files under `be/docs/adr/`
4. `architecture.md`

## Canonical Documents

- `AGENTS.md`
- `architecture.md`
- `be/docs/backend-architecture-guide.tr.md`
- `be/docs/code-documentation-guidelines.md`
- `be/docs/adr/`

## Active Architectural Decisions

- `ADR-0001`: the backend is a Spring Modulith-style modular monolith and modules interact through APIs, events, and IDs.
- `ADR-0002`: each top-level module owns its `domain` and `infrastructure` packages and may expose only `api`.
- `ADR-0003`: public contracts and use-case flows use concise English documentation.
- `ADR-0004`: `shared` is a small stable kernel, not a dumping ground.
- `ADR-0005`: REST endpoints use OpenAPI-based documentation with operation-level summaries, auth, and core error responses.

## Coding and Documentation Defaults

- Durable code comments, ADRs, and project-memory content are written in English.
- Javadoc is required for public module contracts, public application services, and non-obvious domain rules.
- Comments stay behavior-oriented and avoid boilerplate restatement of names.
- Method-level Javadoc is placed above annotations for consistency.
- `TODO` and `FIXME` comments require owner, reason, and exit condition.
- REST controllers use OpenAPI annotations for summaries, auth requirements, and core `ProblemDetail` responses.
- API docs are disabled by default and enabled only in local or explicitly configured environments.

## Module Interaction Defaults

- Cross-module interaction happens through `api` packages, events, or identifiers.
- Modules do not depend on another module's `domain` or `infrastructure` package.
- Shared types remain small, stable, and broadly reusable.

## Testing Defaults

- Prefer tests that validate business rules, persistence constraints, and module boundaries.
- Use Maven wrapper commands from `be/`.
- Add integration coverage when changes affect persistence, transactions, migrations, or REST flows.

## Review Red Flags

- new public contracts without documentation
- durable policy changes without ADR or project-memory updates
- comments that narrate obvious code instead of exposing intent
- new cross-module dependencies into internal packages
