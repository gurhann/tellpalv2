# ADR-0020: BMad Session Model Selection

## Status

Accepted on 2026-10-10 by explicit user instruction.

## Context

The repository's fixed BMad model default and named escalation ladder override the model
selected by the user for the current session. Delegated work can therefore run on a different
model from the one the user selected.

## Decision

- BMad planning, implementation, testing, verification, and review use the model selected for
  the current session.
- Subagents inherit that session model. Omit explicit model overrides when delegating unless
  the user explicitly requests a different model for that work.
- Scope, risk, or debugging difficulty does not trigger an automatic model change. A new user
  selection or explicit model request takes effect without an additional confirmation.
- If the exact model identity is unavailable, describe the choice as inherited from the session;
  do not guess a model name or introduce a fixed fallback.
- BMad workflow, approval, verification, review, and local-commit gates continue to apply.
  This decision explicitly updates the model policy preserved by ADR-0015; its other safeguards
  remain in force. Keep this repository policy in root instructions and project memory rather
  than editing installer-managed BMad skill files.

## Consequences

- The user's session selection governs both primary and delegated BMad work.
- Model defaults no longer need maintenance when available model names change.
- An existing subagent that no longer matches the selected session model must be replaced before
  further work unless the user explicitly requested its different model; changing instructions
  does not retarget it.

## Alternatives Considered

- Retain a fixed default and escalation ladder: rejected because it overrides the session choice.
- Pin the currently selected model name in the repository: rejected because later session choices
  would again be ignored.
- Edit installed BMad skills: rejected because installation updates can overwrite those changes.

## Related Files or Modules

- `AGENTS.md`
- `be/docs/project-memory.md`
- `be/docs/adr/ADR-0015-agent-context-efficiency.md`

## Supersedes / Superseded By

- Supersedes the former fixed BMad model default and named escalation ladder in project memory.
- Updates only the model-setting preservation constraint in ADR-0015.
