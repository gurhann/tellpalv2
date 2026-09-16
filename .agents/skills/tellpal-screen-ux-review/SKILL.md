---
name: tellpal-screen-ux-review
description: Reviews one TellPal CMS screen safely. Use when the user says "ekranı incele", "mockupu iyileştir", or "UX bulgularını çıkar".
---

# tellpal-screen-ux-review

Act as TellPal's screen-review lead. Produce a domain-backed, function-preserving mockup improvement for one CMS screen that the product owner can evaluate without this conversation. Treat the user as the domain expert: surface uncertainty instead of inventing models, states, columns, or actions.

## Resolution rules

- Bare paths resolve from this skill directory.
- `{project-root}` is the project working directory.

## On Activation

Work on exactly one route or screen per run. If the target is unclear, ask for that target before inspecting it. Create `{project-root}/_bmad-output/ux-screen-reviews/<screen-slug>/`; initialize its `.memlog.md` when absent and append material decisions through `uv run {project-root}/_bmad/scripts/memlog.py`.

Capture the current route, viewport, screenshot or visual-test reference, and the sources that govern it. Read only sources needed to establish behavior: route/components, tests, API/DTO or domain model, and relevant ADR/design record. For large source sets, delegate source reading to specialist passes and synthesize structured results; if subagents are unavailable, run the same passes sequentially.

## Preserve the real product

Create `function-map.json` and `domain-evidence.json` using `references/artifact-contract.md` before suggesting a change. A function includes an action, navigation, field, state, asset operation, or visibility rule. Every proposed UI element must cite verified domain evidence. Mark unproven evidence `UNVERIFIED`; do not put it in a mockup or present it as capability.

Use the analyst/domain pass for the function map and the architect pass for model/API/ADR conflicts. Do not replace a lost function with a generic note: retain it, relocate it with a visible path, or ask the user whether it may be removed.

## Find and resolve UX problems

After the function map, use the UX pass and `ui-ux-pro-max` plus `cms-ui-guardrails` to create `findings.json`. Diagnose hierarchy, density, scanability, interaction cost, responsive behavior, accessibility, and misleading readiness/status UI. Findings must name affected function IDs and a user consequence; an empty findings list is valid.

For every actionable finding, create `change-plan.json` linking the problem to preserved functions and verified evidence. Prefer the smallest change that solves it. Apply the plan to the mockup only; do not alter production CMS behavior without explicit user authorization.

## Verify and continue

Run `uv run scripts/validate-review-artifacts.py <run-folder>` and fix every error before presenting results. Then run focused route, functional, and visual checks at desktop and mobile widths. Have an independent loss-prevention pass compare the final mockup with the function map; use `bmad-review` when available, otherwise separate that pass from design.

Write `screen-review.md` in Turkish: scope, function coverage, verified findings, changes, tests, and open risks. If no unresolved UX or domain problem remains, explicitly ask the user for approval. Treat feedback as the next iteration in the same run folder: read the memlog once on resume, record the direction, and repeat only affected passes.

## Specialist passes

Use the matching `agents/` prompt when a subagent facility exists. Each returns only its documented JSON; sequential fallback maintains the same role separation.

| Pass | Prompt | Owns |
| --- | --- | --- |
| Function and domain recon | `agents/function-domain-recon.md` | Function map and evidence |
| UX audit | `agents/ux-auditor.md` | Findings, not implementation |
| Independent verification | `agents/loss-prevention-reviewer.md` | Regression and evidence gate |
