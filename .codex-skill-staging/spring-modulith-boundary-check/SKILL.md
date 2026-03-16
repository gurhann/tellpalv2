---
name: spring-modulith-boundary-check
description: Detect Spring Modulith boundary violations in Spring Boot modular monoliths that use DDD and module-based packaging. Use when Codex needs to verify module boundaries, review cross-module dependencies, detect illegal access to internal packages, find circular dependencies, validate architecture rules, or recommend `ApplicationModules.verify()` tests and refactorings.
---

# Spring Modulith Boundary Check

## Overview

Review module boundaries as an architecture check, not just an import scan. Focus on whether modules expose only intended APIs, whether dependencies are legal and acyclic, and whether business logic and data references stay inside the right module boundaries.

## Workflow

1. Identify the top-level packages that define modules and inspect their public API versus internal package structure.
2. Review cross-module imports, service calls, entity references, and package access patterns.
3. Flag boundary violations such as internal-package access, illegal dependency direction, circular dependencies, or cross-module entity coupling.
4. Explain why each finding violates modular monolith and Spring Modulith rules.
5. Recommend concrete fixes, including moving logic, introducing application APIs, publishing events, or replacing entity references with IDs.

## What To Check

- illegal cross-module imports
- access to another module's internal packages or implementation types
- direct cross-module entity references
- circular dependencies between modules
- business logic placed outside domain or application services
- modules talking through repositories instead of application APIs or events

## Review Rules

- Treat each top-level package as a module unless the codebase defines a different Modulith mapping explicitly.
- Assume only a module's published API may be used externally.
- Flag internal package access even when Java visibility allows it.
- Prefer communication through application services, published events, or stable API types.
- Prefer IDs across module boundaries instead of JPA entity references.
- Call out architectural drift separately from immediate compile-time errors.
- Recommend `ApplicationModules.of(Application.class).verify()` or equivalent architecture tests when they are missing.

## Output Format

Return all of the following:

1. A list of detected boundary violations.
2. A short explanation of why each one violates the architecture.
3. Recommended fixes.
4. A suggested refactoring approach.

## Safety Checks

- Verify the finding is truly cross-module and not internal to one module.
- Distinguish hard violations from design smells.
- Prefer minimal refactor paths that restore encapsulation without broad rewrites.
- Suggest architecture verification tests when the project lacks automated boundary checks.
- When no violations are found, say so explicitly and mention any residual risks such as missing automated verification.

## Reference

Read [`references/modulith-boundary-checklist.md`](references/modulith-boundary-checklist.md) when you need a compact checklist for module API boundaries, illegal access patterns, circular dependency review, and verification-test recommendations.
