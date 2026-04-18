# TellPal CMS UI Standards

This document is the canonical UI standard for work under `cms/`.

## 1. Required Design Review Flow

- Every layout-affecting change must use `ui-ux-pro-max`.
- Every shared component or composition change must use `senior-frontend`.
- Shared defaults or review expectations changed by the task must update this document in the same change.

UI issue workflow:

1. Inspect the route with a real screenshot or rendered page.
2. Determine whether the issue is route-local or shared-primitive driven.
3. Prefer a shared primitive fix over a page-local override.
4. Add a regression guard before closing the issue.

## 2. Registry Page Contract

Registry pages include at least:

- page header
- primary action area
- registry toolbar
- main table or list
- optional right rail

Registry routes must use the shared `RegistryToolbar` primitive instead of composing `FilterBar` directly in the route.

Allowed slots:

- `search`
- `actions`
- `filters`
- `summary`

Registry toolbar rules:

- `search` must remain visually substantial on desktop and may not collapse into a narrow field beside large unused space.
- `summary` must sit below the filter groups by default when filters and summary would otherwise compete on one row.
- filter groups must have visible labels
- chip sizing and spacing must be consistent across the registry
- horizontal scroll is forbidden
- mobile and narrow widths must stack vertically with readable spacing

## 3. Search and Filter Rules

- Search uses a visible group label plus an accessible field label.
- Filter groups must be organized by meaning, not by raw implementation order.
- Reset states such as `All types` or `All states` are allowed, but they must be visually subordinate to the group label.
- Summary text should express the active filter state in compact language and must not dominate the toolbar.
- Search and summary must not create large empty visual zones.

## 4. Shared Primitive Rules

- Shared primitives own layout rules; routes provide content, not structural improvisation.
- Route-level `className` overrides on shared primitives are allowed only for small spacing adjustments, not layout rewrites.
- If two routes need the same layout fix, the fix belongs in the shared primitive.
- Mockup routes may reuse production-safe primitives, but production routes must not depend on mockup-only components.

## 5. Production Copy and Aside Rules

- Generic sidebar note cards are not allowed on production routes.
- Titles such as `notes`, `editor notes`, `curation notes`, `snapshot notes`, or similar workflow reminders must not be added as standalone cards.
- If guidance is necessary, keep it in one of these places:
  - the page or section description
  - structured status or metric cards
  - short inline helper text directly adjacent to the action it explains
- Right rails should prioritize operational state, readiness, counts, and actionable handoff context over prose reminders.
- Explanatory copy that does not change user decisions should be removed instead of moved into another decorative card.

## 6. Accessibility and Interaction Rules

- Visible labels are required for search and filter groups.
- Buttons and chips must preserve keyboard access and announce selected state.
- Touch targets must remain comfortable at all supported breakpoints.
- Reduced-motion users must not receive decorative motion that changes layout comprehension.

## 7. Visual Regression Policy

- Functional tests alone are not sufficient for layout-affecting route changes.
- Screenshot tests are required for key CMS surfaces where spacing, hierarchy, or composition regressions are likely.
- The first required visual set is:
  - contents registry toolbar
  - categories registry toolbar
- Additional registry and detail surfaces should be added through `cms/docs/ui-regression-task-list.md`.

Visual harness requirements:

- deterministic session
- fixed viewport matrix
- disabled animations and transitions
- stable time when the UI depends on dates or clocks
- suppressed transient visual noise when possible

## 8. Current Reference Problems and Resolutions

Reference issue A:

- Registry search fields became visually compressed while filter chips and summary blocks occupied the dominant area.

Required resolution pattern:

- use `RegistryToolbar`
- keep search in a stable desktop column
- place summary below filter groups
- ensure filters wrap cleanly without creating empty whitespace imbalance

Reference issue B:

- Production right rails accumulated generic `notes` cards that repeated workflow guidance and diluted the operational summary.

Required resolution pattern:

- remove generic note cards from production routes
- keep aside content limited to metrics, status summaries, key-value grids, and direct handoff actions
- add regression assertions so note-card headings do not silently return
