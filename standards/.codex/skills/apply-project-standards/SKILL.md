---
name: apply-project-standards
description: Apply the team engineering standards package to a project by following the comment policy, using project memory and ADRs, and updating durable decision artifacts when architectural defaults change.
---

# Apply Project Standards

## Purpose

Use this skill when starting work in a repository that follows the engineering standards package.
The goal is to keep code comments, decision records, and project memory aligned.

## Read Order

1. Read `AGENTS.md`.
2. Read `docs/project-memory.md` if it exists.
3. Read relevant ADRs under `docs/adr/`.
4. Read project architecture or design documents as needed.

## Workflow

1. Identify whether the task changes code comments, public contracts, architecture, or durable team defaults.
2. Apply the comment standard to any new or changed public contract, use-case method, or non-obvious domain rule.
3. If the task changes a durable engineering decision, update `docs/project-memory.md`.
4. If the decision is architectural, policy-level, or long-lived, create or update an ADR as well.
5. Keep code comments short, behavior-oriented, and in English.
6. Do not add comments that only restate names or obvious mechanics.

## Decision Heuristics

Create or update an ADR when the work changes:

- module boundaries
- persistence strategy
- security model
- integration model
- documentation policy
- testing policy
- shared-kernel or shared-module usage

Update project memory when the work changes:

- active defaults
- canonical document links
- review expectations
- naming or documentation conventions

## Output Expectations

When relevant, mention:

- which standard was applied
- whether project memory was updated
- whether an ADR was added or updated
