# Code Documentation Guidelines

## Purpose

Code documentation exists to make module contracts, business flows, and non-obvious rules easier to
read. It complements architecture documents and ADRs; it does not replace them.

## Required Documentation

- `api` package docs
- module-facing interfaces, records, commands, and result types
- public application service classes
- public methods with business behavior, validation, or failure semantics
- domain rules with non-obvious invariants or state transitions

## Avoid Documentation

- trivial getters and setters
- obvious mapper code
- framework annotations that already explain themselves
- comments that restate names without adding behavior

## Style

- English only
- concise, behavior-oriented wording
- explain intent, side effects, and failure semantics
- use inline comments sparingly

## Comment Types

- package doc
- type doc
- use-case method doc
- domain rule comment
- inline local comment
- decision comment
- TODO/FIXME with owner, reason, and exit condition
