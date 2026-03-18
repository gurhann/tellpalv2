# REST API Documentation Standard

## Purpose

This standard defines how REST endpoints are documented so that engineers and agents can discover
request shapes, security expectations, and failure semantics from a generated OpenAPI document.

The standard uses two layers:

- generated OpenAPI specification and Swagger UI
- repo policy that defines how controllers and operations must be annotated

## Coverage

Document all user-facing REST controllers, including:

- admin APIs
- mobile or public APIs
- webhook endpoints

Exclude internal non-HTTP flows.

## Required Controller Annotations

Every REST controller should declare:

- `@Tag`
- a concise class-level description through tag text or controller-level Javadoc
- class-level security requirement when every operation shares the same auth model

## Required Operation Annotations

Every documented endpoint should declare:

- `@Operation(summary = ...)`
- a short behavior-oriented description when the summary alone is not enough
- auth requirement when the operation is protected
- minimum `@ApiResponses` entries for success plus important error outcomes

## Response Rules

Document:

- success response
- validation or malformed input errors
- auth errors when applicable
- access denied errors for protected admin endpoints
- not found or conflict responses when the endpoint regularly produces them

Use `ProblemDetail` as the default error schema unless the API intentionally returns a different
error shape.

## Security Rules

- Admin endpoints use bearer JWT documentation.
- Mobile authenticated endpoints use bearer Firebase token documentation.
- Webhook endpoints document header-based authorization.
- Public read endpoints must explicitly omit auth requirements.

## Grouping Rules

OpenAPI groups should separate:

- `admin`
- `mobile`
- `webhook`

Tags may be narrower than groups when it improves readability.

## Style

- summaries are action-oriented
- descriptions stay concise
- do not repeat path or method names
- use English
