---
name: write-jqwik-property-test
description: Generate jqwik property-based tests for Java and Spring Boot backends that use JUnit 5, jqwik, Spring Modulith, PostgreSQL, and DDD. Use when Codex needs to create property-based tests, verify domain invariants, test constraints and validation rules, check uniqueness or ordering behavior, or prove consistency rules with generated inputs instead of fixed examples.
---

# Write jqwik Property Test

## Overview

Write property tests that verify invariants over many generated inputs instead of example-only scenarios. Keep the focus on domain behavior, constraint preservation, and boundary coverage so the user gets both a runnable jqwik test class and a short explanation of what the property proves.

## Workflow

1. Inspect the domain type, validation rules, persistence constraints, and existing JUnit or jqwik test style before writing the test.
2. Identify the invariant to prove, such as uniqueness, ordering, consistency, idempotency, or boundary preservation.
3. Choose meaningful generators that reflect valid and intentionally edge-heavy domain inputs. Prefer jqwik `Arbitraries` and provider methods over ad hoc randomness.
4. Write a `@Property`-based test class with `@ForAll` parameters or named providers and ensure each property runs at least 100 iterations.
5. Explain the tested invariant, the generators, and the edge cases that the generators are designed to cover.

## Authoring Rules

- Use `@Property` for property tests.
- Use `@ForAll` for generated inputs.
- Use jqwik `Arbitraries` or provider methods for domain-specific data generation.
- Do not use `random()` manually.
- Set each property to at least 100 tries when the project default does not already guarantee that.
- Prefer generators that encode the domain shape, such as bounded strings, ordered collections, unique sets, or constrained numeric ranges.
- Test invariants, not example snapshots.
- Keep assertions focused on laws that should hold for all generated inputs.
- Use assumptions sparingly; prefer generators that produce the right input space instead of filtering heavily.
- For persistence-related properties, isolate what is being proven, such as uniqueness, sorting, or round-trip consistency, and avoid turning the property into an integration test unless that is the actual goal.

## Domain and Constraint Checks

- Verify business invariants that should hold for every valid input.
- Verify uniqueness constraints with generated duplicates and distinct values where appropriate.
- Verify ordering invariants with unsorted, repeated, and boundary-sized collections.
- Verify validation rules with edge values such as empty, max-length, negative, zero, or duplicate inputs when relevant.
- Verify consistency rules across transformations, such as create-then-read, sort-then-check, or normalize-then-validate.

## Output Format

Return all of the following:

1. The complete property test class.
2. A short explanation of the tested invariant.
3. A concise explanation of the generators used.
4. A concise explanation of the edge cases covered.

## Safety Checks

- Verify the property checks a real invariant rather than restating the implementation.
- Verify the generators include edge-heavy values without relying on manual randomization.
- Verify iteration count is at least 100 per property.
- Verify shrinking remains useful by keeping generators simple and meaningful.
- Verify the property does not silently skip most inputs through excessive assumptions.

## Reference

Read [`references/jqwik-property-checklist.md`](references/jqwik-property-checklist.md) when you need a compact checklist for jqwik annotations, generator design, invariant selection, and edge-case coverage.
