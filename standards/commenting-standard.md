# Commenting Standard

## Purpose

This standard defines when comments are required, which comment type to use, and what information
the comment must contain. Comments exist to expose intent, contracts, business rules, and
surprising decisions. They do not exist to narrate obvious code.

All durable engineering comments are written in English.

## Comment Types

### 1. Package Doc

Use for packages that expose contracts to other modules, adapters, or external systems.

Required content:

- what the package exposes
- who may depend on it
- what it intentionally does not expose

### 2. Type Doc

Use for public classes, records, interfaces, enums, and aggregate roots when the type is part of a
module contract or owns important behavior.

Required content:

- why the type exists
- where it should be used
- critical ownership or lifecycle rule when applicable

### 3. Use-Case Method Doc

Use for public application methods and any method with business validation, error semantics, state
transitions, or side effects.

Required content:

- business meaning of the call
- important preconditions
- notable result or side effect
- important business failure modes when not obvious from the signature

### 4. Domain Rule Comment

Use inside aggregates or domain services when a local invariant, visibility rule, idempotency rule,
or state transition is easy to miss while reading the code.

Required content:

- the rule
- why the rule exists if the reason is not already obvious

### 5. Inline Local Comment

Use sparingly for a local block whose intent is difficult to infer from names alone.

Required content:

- the local rule or transformation being protected

### 6. Decision Comment

Use rarely when code contains a non-obvious choice that must remain visible close to the
implementation.

Required content:

- the surprising choice
- the reason for the choice
- an ADR or decision-memory reference when the choice is architectural

### 7. TODO or FIXME

Allowed only when the comment includes:

- owner or team
- reason
- concrete exit condition

`TODO remove later` and similar comments are not allowed.

## Required Format

- English only
- concise, behavior-oriented tone
- prefer one summary sentence and one short clarifying paragraph when needed
- describe intent, constraints, side effects, and failure semantics
- avoid boilerplate `@param` and `@return` text unless it adds real information

## Prohibited Comments

Do not write comments that:

- repeat the type or method name
- explain obvious getters, setters, mappings, or framework annotations
- describe line-by-line mechanics that the code already makes clear
- drift from the current implementation

## Default Coverage Rules

Comments are required for:

- `api` package docs
- module-facing interfaces and records
- public application services
- public methods with business behavior
- domain rules that are non-obvious

Comments are usually unnecessary for:

- trivial controllers
- DTO-only response records
- persistence adapters whose intent is already obvious
- framework configuration with no surprising behavior

## Example

```java
/**
 * Application service for rotating refresh tokens for admin sessions.
 *
 * <p>Expired, reused, and revoked refresh tokens are rejected as business failures.
 */
public class AdminAuthenticationService {
```
