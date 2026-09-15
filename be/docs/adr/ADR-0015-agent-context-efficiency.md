# ADR-0015: Agent Context Efficiency

## Status

Accepted on 2026-09-15.

## Context

Root instructions and project memory repeat operational details. The CMS task list includes
completed history in its required reading, and BMad's Blind Hunter requires at least ten findings
even when none are supported. These patterns can increase context and review overhead.

## Decision

- Keep unique safeguards and active decisions explicit; remove duplicate prose and skill inventories.
  Route operational details to existing canonical documents with explicit task-based read triggers.
- Require reading the Railway runbook for deployment work and keep it current when behavior changes.
- Keep open CMS regression tasks in the active list. Archive completed records without changing
  their contents or IDs; retain original headings as links so existing fragments still resolve.
  Read archived records only when their history is relevant.
- Override Blind Hunter in BMad build (standard and one-shot) and code review through team TOML
  files under `_bmad/custom/`. Require evidence, locations, and concrete impact, with no finding
  quota. Allow an explicit "No actionable findings." result and stop on empty review content.
- Preserve review layers, activation conditions, test requirements, security rules, model settings,
  and language policy. Do not modify installer-managed skill files for this customization.

## Consequences

- Routine reading becomes shorter while detailed history and operating instructions remain accessible.
- Reviews can finish without unsupported findings; this does not remove review or verification gates.
- Character counts can demonstrate reduced document size, but do not measure token billing savings.
- Links and task-based read triggers must remain valid when documents move.

## Alternatives Considered

- Disabling review layers or reducing tests: rejected to preserve existing quality controls.
- Deleting completed tasks: rejected because regression history and existing links remain useful.
- Editing installed BMad defaults: rejected because updates can overwrite those changes.

## Related Files or Modules

- `AGENTS.md`
- `be/docs/project-memory.md`
- `_bmad/custom/bmad-build.toml`
- `_bmad/custom/bmad-code-review.toml`
- `cms/docs/ui-regression-task-list.md`
- `cms/docs/ui-regression-task-archive.md`
- `ops/railway/README.md`

## Supersedes / Superseded By

- None. Complements existing policies without changing application architecture.
