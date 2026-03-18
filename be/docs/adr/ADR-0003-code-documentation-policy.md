# ADR-0003: Code Documentation Policy

## Status

Accepted

## Context

The codebase contains meaningful business flows and module boundaries that are expensive to recover
from signatures alone. Ad hoc comments do not scale across engineers or future projects.

## Decision

The project uses a selective documentation policy:

- package docs for exposed `api` packages
- type docs for public module contracts and important aggregates
- method docs for public use-case methods and non-obvious business behavior
- selective domain comments for invariants, state transitions, and visibility rules

All durable code documentation is written in English and kept concise and behavior-oriented.

## Consequences

- Public contracts and business flows are easier to read without documenting every method.
- Review must reject boilerplate comments that merely restate code.
- Future projects can reuse the same policy through the standards package.

## Alternatives Considered

- Documenting everything was rejected because it creates noise and stale comments.
- Writing almost no comments was rejected because too much intent stayed implicit in business code.

## Related Files or Modules

- `be/docs/code-documentation-guidelines.md`
- `be/docs/project-memory.md`
- `standards/commenting-standard.md`

## Supersedes / Superseded By

- None
