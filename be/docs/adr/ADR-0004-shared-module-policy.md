# ADR-0004: Shared Module Policy

## Status

Accepted

## Context

Shared modules in modular monoliths tend to become dumping grounds for convenience helpers and
cross-cutting shortcuts, which slowly erase module ownership.

## Decision

`shared` remains a small, stable kernel for broadly reusable primitives and technical helpers. It
must not absorb business concepts that belong to a single module.

Examples that fit:

- `LanguageCode`
- shared persistence primitives
- generic request correlation or admin web support

Examples that do not fit:

- asset ownership types
- content-specific domain rules
- purchase-specific read models

## Consequences

- Reuse stays disciplined and module ownership remains clearer.
- Some duplication across modules is acceptable when it protects boundaries.
- New additions to `shared` should trigger architectural review when ownership is unclear.

## Alternatives Considered

- A broad shared kernel was rejected because it would centralize unrelated business concepts.
- Duplicating all cross-cutting helpers per module was rejected because stable technical primitives
  still benefit from one home.

## Related Files or Modules

- `architecture.md`
- `be/src/main/java/com/tellpal/v2/shared`
- `be/docs/project-memory.md`

## Supersedes / Superseded By

- None
