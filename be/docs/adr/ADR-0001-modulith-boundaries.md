# ADR-0001: Spring Modulith Boundaries

## Status

Accepted

## Context

The backend contains multiple business areas with meaningful boundaries, but the operational
overhead of splitting them into separate deployable services is not justified.

## Decision

The backend is implemented as a Spring Modulith-style modular monolith. Each top-level package
under `com.tellpal.v2` is treated as a module. Cross-module interaction happens only through public
APIs, events, or identifiers.

## Consequences

- Module boundaries remain explicit without requiring distributed systems overhead.
- Transactional consistency stays simple because the application uses one database.
- Review and design work must actively prevent cross-module access into internal packages.

## Alternatives Considered

- Separate microservices were rejected because the system is not yet large enough to justify the
  operational complexity.
- A flat monolith was rejected because module boundaries would become tribal knowledge instead of
  enforceable architecture.

## Related Files or Modules

- `architecture.md`
- `com.tellpal.v2.*`
- `be/docs/modulith/`

## Supersedes / Superseded By

- None
