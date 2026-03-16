---
name: write-spring-service
description: Generate Spring Boot service-layer classes for backends that use Spring Modulith, PostgreSQL, Flyway, and DDD. Use when Codex needs to implement application services, coordinate repositories and aggregates, implement business use cases, enforce business rules, or write transactional workflows without violating module boundaries.
---

# Write Spring Service

## Overview

Write application services that orchestrate use cases cleanly inside one module. Keep business flow explicit, delegate invariants to aggregates where possible, and make transaction and repository choices clear so the user gets both code and a short architectural rationale.

## Workflow

1. Inspect the target module, aggregate types, repository interfaces, and existing use-case patterns before writing service code.
2. Identify the use case boundary: command-style mutation, query-style read, or orchestration that publishes domain outcomes.
3. Keep the service inside its module and avoid direct repository access to other modules. Across modules, use IDs, published events, or an existing application-facing API.
4. Implement a focused `@Service` class with constructor injection, input validation, clear method names, and `@Transactional` only where the workflow changes state.
5. Explain the business logic, transaction boundaries, and repository interactions after the code.

## Authoring Rules

- Use `@Service`.
- Use constructor injection only.
- Keep methods short, focused, and named after a business use case.
- Use `@Transactional` for write workflows and choose read-only transactions deliberately for query methods when the codebase uses them.
- Coordinate repositories and aggregates in the service; do not move SQL or persistence details into the service.
- Let aggregates enforce their own invariants through methods such as `activate()`, `assignPlan()`, or `complete()`.
- Validate required inputs at the application boundary before loading or mutating aggregates.
- Return domain objects, command results, or DTOs according to the surrounding module style; do not invent a new output pattern without reason.
- Prefer one public method per use case unless the existing service style groups related operations together.

## DDD and Modulith Checks

- Ensure the service orchestrates domain behavior instead of becoming a procedural dump of setters.
- Ensure repositories are used only for loading and storing aggregates or projections.
- Ensure no cross-module repository injection is introduced.
- Ensure cross-module communication uses IDs, application APIs, or events rather than entity graphs.
- Ensure domain decisions stay in the domain model when they belong there, not in persistence adapters.

## Output Format

Return all of the following:

1. The complete Spring service class.
2. A short explanation of the business logic.
3. A concise explanation of transaction boundaries.
4. A concise explanation of repository interactions.

## Safety Checks

- Verify each transaction boundary matches the use case and does not span unrelated work.
- Verify repositories are accessed in a module-safe way.
- Verify null and existence checks happen before aggregate mutation.
- Verify exception choices align with the existing application style.
- Verify the service does not expose persistence concepts such as entity manager operations or SQL-shaped logic.

## Reference

Read [`references/spring-service-modulith-checklist.md`](references/spring-service-modulith-checklist.md) when you need a compact checklist for service responsibilities, transaction placement, and module-safe repository orchestration.
