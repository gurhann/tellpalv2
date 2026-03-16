---
name: write-jpa-entity
description: Generate JPA entity classes for Spring Boot backends that use Spring Modulith, PostgreSQL, Flyway, and DDD tactical patterns. Use when Codex needs to create a new JPA entity, design a domain aggregate, implement entity mappings, define relationships, or map a database schema to a domain model without breaking module boundaries.
---

# Write JPA Entity

## Overview

Write entity classes that model domain concepts first and persistence second. Keep aggregates inside their module, use JPA deliberately, and make relationship and invariant decisions explicit so the user gets both code and a short architectural rationale.

## Workflow

1. Inspect the target module, aggregate vocabulary, existing entities, and Flyway schema before writing code.
2. Identify whether the class is an aggregate root, internal entity, or value-object candidate. Do not force everything into `@Entity`.
3. Keep references inside the bounded context. Across modules, store foreign IDs such as `customerId` or `orderId` instead of `@ManyToOne` to another module's entity.
4. Write a complete entity class with explicit table and column mappings, constructor-based initialization, and methods that protect invariants.
5. Explain the design decisions, especially aggregate boundaries, relationship choices, and schema constraints implied by the model.

## Authoring Rules

- Use `@Entity` and `@Table` for persistent entities.
- Prefer explicit `@Column` mappings for important fields, nullable rules, lengths, uniqueness, and names.
- Use `UUID` or `Long` identifiers based on the surrounding module; follow existing project conventions before introducing a new style.
- Keep fields `private`.
- Prefer constructor-based initialization and behavior methods over anemic setter-heavy models.
- Use Lombok sparingly. Only add small annotations such as getters when they match the codebase style.
- Default relationships to `fetch = FetchType.LAZY`.
- Avoid bidirectional relationships unless navigation is required on both sides and the consistency cost is justified.
- Model collections carefully. Prefer aggregate-root control over child lifecycle with `cascade` and `orphanRemoval` only when the child truly belongs to the aggregate.
- Add `createdAt` and `updatedAt` only when the module already uses audit fields or the use case clearly requires them.

## DDD and Modulith Checks

- Ensure the entity represents a domain concept, not a direct table mirror.
- Ensure aggregate roots expose methods that enforce invariants instead of relying on callers to mutate state correctly.
- Ensure module boundaries are respected; no cross-module entity imports.
- Ensure cross-module relationships use identifiers or published events, not JPA associations.
- Ensure persistence concerns do not leak into other modules through shared entity classes.

## Output Format

Return all of the following:

1. The complete entity class.
2. A short explanation of the design decisions.
3. A concise explanation of relationships, invariants, and persistence constraints.

## Safety Checks

- Verify the owning side of each relationship is explicit.
- Verify nullable settings in code match the schema and aggregate rules.
- Verify equals/hashCode strategy is safe; avoid entity equality based on mutable business fields.
- Verify no-arg constructor needs are handled for JPA without exposing invalid public construction.
- Verify the entity can evolve with Flyway migrations without introducing hidden module coupling.

## Reference

Read [`references/jpa-ddd-modulith-checklist.md`](references/jpa-ddd-modulith-checklist.md) when you need a compact checklist for aggregate design, JPA mapping defaults, and cross-module boundary rules.
