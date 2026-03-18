# Project Memory

## Purpose

This document is the fast entry point for active engineering defaults in this project.

Read order:

1. `AGENTS.md`
2. `project-memory.md`
3. relevant ADRs
4. architecture and design documents

## Canonical Documents

- `AGENTS.md`
- `docs/adr/`
- `architecture.md`

## Active Architectural Decisions

- `<ADR link>`: short description

## Coding and Documentation Defaults

- Durable comments and decision artifacts are written in English.
- Public contracts and use-case flows are documented with concise, behavior-oriented comments.

## Module Interaction Defaults

- Cross-module interaction happens through APIs, events, or IDs.

## Testing Defaults

- Prefer tests that validate business rules, persistence constraints, and module boundaries.

## Review Red Flags

- new public contracts without documentation
- durable decisions changed without ADR or project-memory update
- comments that repeat obvious code instead of exposing intent
