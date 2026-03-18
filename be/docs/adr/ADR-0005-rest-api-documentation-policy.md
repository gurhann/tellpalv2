# ADR-0005: REST API Documentation Policy

## Status

Accepted

## Context

The backend exposes multiple admin, mobile, and webhook REST endpoints, but the project has no
generated API specification or stable policy for documenting endpoint behavior, security, and
errors.

## Decision

The project adopts OpenAPI-based REST documentation for the full REST surface. Generated docs are
backed by a policy that requires controller and operation-level annotations for summaries, security
requirements, and core error responses.

OpenAPI and Swagger UI are disabled by default and enabled only in local or explicitly configured
environments.

## Consequences

- Engineers can inspect a live spec instead of reconstructing endpoint behavior from controllers.
- Documentation remains close to code and can be kept current through normal review.
- Production does not expose Swagger or OpenAPI unless intentionally enabled.

## Alternatives Considered

- Markdown-only endpoint documentation was rejected because it drifts from code too easily.
- Generated spec without a policy was rejected because annotation quality would become inconsistent.
- Always-on Swagger in production was rejected because it adds unnecessary public surface area.

## Related Files or Modules

- `standards/rest-api-documentation-standard.md`
- `be/src/main/java/com/tellpal/v2/*/web`
- `be/docs/project-memory.md`

## Supersedes / Superseded By

- None
