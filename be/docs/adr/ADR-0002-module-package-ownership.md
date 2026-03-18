# ADR-0002: Module Package Ownership

## Status

Accepted

## Context

Without a package ownership rule, modules can slowly leak implementation details and become tightly
coupled through internal entities or infrastructure classes.

## Decision

Each module owns its `domain`, `application`, `infrastructure`, and `web` packages internally.
Only the `api` package is safe to depend on from another module unless an explicit ADR says
otherwise.

## Consequences

- Module ownership is easier to review because allowed dependencies are clear.
- Shared persistence and web helpers must stay generic or live in `shared`.
- Implementers may need to create extra API records or references instead of reusing internal types.

## Alternatives Considered

- Allowing access into `domain` was rejected because it blurs aggregate ownership.
- Allowing access into `infrastructure` was rejected because adapters are implementation detail, not
  contract.

## Related Files or Modules

- `architecture.md`
- `AGENTS.md`
- `be/src/main/java/com/tellpal/v2/*/api`

## Supersedes / Superseded By

- None
