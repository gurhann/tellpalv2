# Code Documentation Guidelines

## Purpose

This repository uses code documentation to expose module contracts, business flows, and non-obvious
rules without turning the codebase into comment-heavy noise. The primary audience is backend
engineers and autonomous agents working in the repository, not external API consumers.

Documentation should help a reader answer:

- Why does this type or package exist?
- When should this use case be called?
- Which failures, side effects, or state transitions matter?
- Which module boundary is safe to depend on?

`architecture.md` remains the canonical high-level architecture reference. `be/docs/project-memory.md`
and `be/docs/adr/` store durable project decisions. Code comments should complement those artifacts,
not restate them.

## Comment Types

Use the following comment types:

- `Package doc`: for exposed `api` packages and other public package boundaries
- `Type doc`: for public contracts, important services, aggregates, and other meaningful public types
- `Use-case method doc`: for public business methods with validation, side effects, or failure semantics
- `Domain rule comment`: for invariants, state transitions, idempotency, and visibility rules
- `Inline local comment`: for a local rule that is hard to infer while scanning the method body
- `Decision comment`: rarely, for a surprising implementation choice that should point back to ADRs or project memory
- `TODO/FIXME`: only with owner, reason, and exit condition

## Where Documentation Is Required

Documentation is required for:

- `api` package documentation
- module-facing interfaces in `api`
- command, result, reference, and record types in `api`
- public application service classes
- public methods that carry business behavior, validation, side effects, or error semantics
- domain methods and code blocks with non-obvious invariants, state transitions, idempotency, or visibility rules

## Where Documentation Should Be Avoided

Do not add documentation for:

- obvious getters and setters
- trivial mappers that only translate one type to another
- framework annotations or wiring that are already self-explanatory
- comments that simply repeat the method or class name
- response DTOs or plumbing code whose intent is already clear from names

If a type is only infrastructure plumbing and its intent is already obvious, prefer no comment.

## Style Rules

- Write durable comments and Javadoc in English.
- Keep them short and behavior-oriented.
- Prefer describing preconditions, outcomes, side effects, ownership, and failure semantics.
- Avoid verbose `@param` and `@return` sections unless they add real information beyond the signature.
- Prefer one short summary line plus one short paragraph when extra context is needed.
- Place method-level Javadoc above annotations for consistency.
- Use inline comments sparingly and only for local rules that are easy to miss while scanning the code.
- Do not leave `TODO` or `FIXME` comments without owner, reason, and exit condition.

## Recommended Templates

```java
/**
 * Module-facing use case for rotating admin refresh tokens.
 *
 * <p>Reuses, expired tokens, and revoked tokens are rejected as business failures.
 */
public interface ExampleApi {
```

```java
/**
 * Application service that orchestrates content publication.
 *
 * <p>This service coordinates aggregate updates and external side effects inside a single
 * transaction boundary.
 */
public class ExampleService {
```

```java
/**
 * Aggregate root for category curation rules.
 *
 * <p>Visible content ordering is owned per language and duplicate links are ignored.
 */
public class ExampleAggregate {
```

## Rollout Order

When adding documentation incrementally, use this order:

1. `api` package documentation and public contracts
2. public application services and use-case methods
3. only the domain rules that are not obvious from code
4. selective infrastructure or web documentation when behavior would otherwise surprise a reader
