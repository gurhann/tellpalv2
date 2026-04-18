---
name: cms-ui-guardrails
description: Repo-specific UI guardrails for TellPal CMS. Use when changing CMS layout, shared frontend primitives, route composition, or visual regression coverage.
---

# CMS UI Guardrails

Use this skill for any work under `cms/` that changes:

- layout
- spacing
- component composition
- route-level interaction structure
- screenshot or visual regression coverage

## Read Order

1. repository root `AGENTS.md`
2. `cms/AGENTS.md`
3. `cms/docs/ui-standards.md`
4. `cms/docs/ui-regression-task-list.md`

## Mandatory Companion Skills

- `ui-ux-pro-max` for layout, hierarchy, responsive, and accessibility decisions
- `senior-frontend` for shared primitive design, React composition, and maintainable testable structure
- `apply-project-standards` when changing durable frontend defaults or standards docs

## Core Rules

### 1. Diagnose before patching

Start from the actual route and visual evidence. Do not jump straight to route-local utility classes when the issue may come from a shared primitive.

### 2. Prefer shared contracts

If the same issue can appear on multiple routes, solve it in the shared component contract.

### 3. Update the standard when needed

If a UI issue is not already covered by `cms/docs/ui-standards.md`, add the rule in the same task that fixes the issue.

### 4. Leave a regression guard

Every layout-affecting fix must leave behind at least one of:

- component contract test
- interaction test
- screenshot test

### 5. Keep production rails quiet

Do not add generic `notes`, `notlar`, `editor notes`, `curation notes`, or similar reminder cards to production routes.

Use right rails for:

- readiness metrics
- status summaries
- key-value operational context
- direct handoff actions

Do not keep rail cards that simply restate metadata already visible or editable in the main lane.

If guidance is needed, keep it in section descriptions or inline helper text near the relevant control.

## Registry Toolbar Contract

Registry routes must use the shared `RegistryToolbar` primitive.

Required slots:

- `search`
- `actions`
- `filters`
- `summary`

Required behavior:

- search keeps a stable desktop width
- summary sits below filters when one-row competition would compress search or create whitespace imbalance
- filter groups have visible labels
- no horizontal scroll
- responsive stacking at narrow widths

## Visual Regression Expectations

For key CMS surfaces:

- use deterministic Playwright mocks
- freeze time when the surface renders time-sensitive content
- disable animation and transition noise
- test at `390`, `768`, `1280`, and `1440`

Start with:

- contents registry toolbar
- categories registry toolbar

Then follow `cms/docs/ui-regression-task-list.md`.
