# Code Documentation Guidelines

## Purpose

This repository uses code documentation to make module contracts and business flows easier to read.
The primary audience is backend engineers reading the codebase, not external API consumers.

Documentation should help a reader answer:

- Why does this type exist?
- When should this use case be called?
- Which business failures or state transitions matter?
- Which package is safe to depend on from another module?

`architecture.md` remains the canonical high-level architecture reference. Javadoc should complement it,
not restate it.

## Where Javadoc Is Required

Javadoc is required for:

- module-facing interfaces in `api`
- command, result, and record types in `api`
- public application service classes
- public methods that carry business behavior, validation, or error semantics
- domain methods with non-obvious invariants, state transitions, idempotency, or visibility rules

Package-level documentation is required for `api` packages that expose contracts to adapters or other modules.

## Where Javadoc Should Be Avoided

Do not add Javadoc for:

- obvious getters and setters
- trivial mappers that only translate one type to another
- annotations or framework wiring that are already self-explanatory
- comments that simply repeat the method or class name

If a type is only infrastructure plumbing and its intent is already clear from names, prefer no comment.

## Style Rules

- Write Javadoc in English.
- Keep it short and behavior-oriented.
- Prefer describing preconditions, outcomes, side effects, and failure semantics.
- Avoid verbose `@param` and `@return` sections unless they add real information beyond the signature.
- Prefer one short summary line plus one short paragraph when extra context is needed.
- Use inline comments sparingly, only for local rules that are easy to miss while scanning the method body.

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
