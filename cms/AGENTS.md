# CMS Frontend Guidelines

## Read Order

Before planning or implementing UI work under `cms/`, read these documents in order:

1. repository root `AGENTS.md`
2. `cms/AGENTS.md`
3. `cms/docs/ui-standards.md`
4. `cms/docs/ui-regression-task-list.md`
5. route- or feature-specific docs when they exist

## Mandatory Skills

Use these skills for the matching work:

- `ui-ux-pro-max` for any layout, interaction, responsive, spacing, or visual hierarchy change
- `senior-frontend` for shared component design, React composition, test strategy, and maintainable frontend structure
- `apply-project-standards` when changing durable team defaults or shared standards documents

## UI Issue Workflow

Every reported UI problem must follow this order:

1. Inspect the actual route and screenshot or rendered UI first.
2. Identify the root cause at the shared primitive, layout contract, or spacing contract level before applying a page-local patch.
3. Check whether the problem already violates `cms/docs/ui-standards.md`.
4. If it violates an existing rule, fix the UI by applying that rule.
5. If the rule does not exist yet, add the rule to `cms/docs/ui-standards.md` in the same task.
6. Add at least one regression guard in the same task:
   - interaction test
   - component contract test
   - screenshot test

Do not close a UI issue with a visual patch alone when the root cause is in a shared primitive.

## Shared Component Guardrails

- Registry pages must use the shared registry toolbar primitive; do not compose low-level `FilterBar` layout directly inside a route.
- Shared component extensions are preferred over page-local class stacks.
- Search, filters, summary, and toolbar actions must live in named slots when the page pattern supports them.
- Visible labels are required for filter groups; placeholders are not sufficient labels.
- Desktop layouts must not compress search fields into narrow columns while empty space remains nearby.
- Summary blocks must not compete with filter groups on the same visual row when that causes crowding.
- Horizontal scroll in admin toolbars is not allowed.
- Production routes must not add generic `notes` or `notlar` cards in rails or sidebars.
- If explanatory text is needed, place it in a section description, inline helper text near the action, or a structured status card instead of a standalone notes panel.
- Production rails must not duplicate metadata that is already shown or editable in the main lane.

## Responsive and Visual Review Rules

- Validate layout-affecting work at mobile, tablet, laptop, and large desktop widths.
- For key routes, add or update visual regression coverage before considering the work complete.
- If a screenshot from a user revealed the issue, the closing task must leave behind a visual regression assertion or baseline for that surface.

## Current Canonical Frontend Standards

- Canonical UI standard: `cms/docs/ui-standards.md`
- Open UI debt and regression tasks: `cms/docs/ui-regression-task-list.md`
