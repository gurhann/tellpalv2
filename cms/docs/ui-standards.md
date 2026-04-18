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
- Right rails must not repeat metadata that is already visible in the main editing lane.
- Cards such as `content profile`, `metadata profile`, or similar summary duplicates should be removed when the same values are already editable or visible in the main section.
- Detail routes must have one dominant primary workspace.
- Child components inside a detail route must not open a second competing section or header for the same workflow that the route already owns.
- The same state must not be summarized in the header, rail, and section body at the same time unless each layer supports a different concrete decision.
- Detail screens should read like editors, not dashboards. Inside the primary workspace, prefer one sequential edit flow over side-by-side metric cards.

## 6. Detail Workspace Contract

Detail and workspace routes must follow one shared composition contract.

- The route header owns identity, a short route-level explanation, and route-level actions only.
- Header toolbars must stay compact. Use them for contextual controls or lightweight status identity, not metric dashboards.
- The main lane must present one dominant sequential workflow. Child components must not add a second competing summary section for the same workflow.
- The same state must not be summarized in the toolbar, rail, form-top summary, and publication panel at the same time.
- Right rails on detail routes must stay operational and compact:
  - at most three concise stats by default
  - no nested summary cards when the same information is already visible in the main lane
  - no metadata restatement from editable forms
- Publishing or operational cards may show only the minimum gating context needed for the next action. They must not mirror editable status controls.
- Editorial asset pickers inside forms must use compact editor density:
  - reduced preview height
  - reduced nested borders
  - clear primary and secondary actions
  - technical or debug helper copy hidden behind advanced affordances or kept visually secondary
- Editorial asset picker fields must not be forced into half-width columns beside a single orphan input.
- When a cover or illustration field directly supports publishing decisions, prefer a balanced two-column block:
  - media card on the left
  - compact metadata stack on the right
  - mobile and tablet layouts may collapse back to one column
- Use a full-width media row only when there is no closely related metadata stack to pair beside it.
- Detail routes should preserve the primary workspace at all supported widths. The rail may remain visible, but it must not visually outweigh the main lane.

## 7. Accessibility and Interaction Rules

- Visible labels are required for search and filter groups.
- Buttons and chips must preserve keyboard access and announce selected state.
- Touch targets must remain comfortable at all supported breakpoints.
- Reduced-motion users must not receive decorative motion that changes layout comprehension.

## 8. Visual Regression Policy

- Functional tests alone are not sufficient for layout-affecting route changes.
- Screenshot tests are required for key CMS surfaces where spacing, hierarchy, or composition regressions are likely.
- The first required visual set is:
  - contents registry toolbar
  - categories registry toolbar
- Detail workspace coverage must include:
  - `/contents/:contentId`
  - `/categories/:categoryId`
  - viewport widths `390`, `768`, `1280`, and `1440`
- Additional registry and detail surfaces should be added through `cms/docs/ui-regression-task-list.md`.

Visual harness requirements:

- deterministic session
- fixed viewport matrix
- disabled animations and transitions
- stable time when the UI depends on dates or clocks
- suppressed transient visual noise when possible

## 9. Current Reference Problems and Resolutions

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

Reference issue C:

- Right rails repeated the same metadata that the main form already exposed, adding noise without new decision support.

Required resolution pattern:

- remove duplicated profile cards from the rail
- keep the rail focused on operational summary, not repeated form content

Reference issue D:

- The content detail screen split the same workflow across route sections, nested child sections, and repeated summary cards, making the primary next step unclear.

Required resolution pattern:

- keep locale work as the dominant primary workspace
- let the route own section hierarchy and let child components render body content only
- keep the rail minimal and operational

Reference issue E:

- Detail routes drifted back toward dashboard composition through repeated status summaries, oversized metric toolbars, and heavy asset picker panels.

Required resolution pattern:

- keep header and toolbar compact
- keep one dominant editor flow in the main lane
- keep rails limited to operational summaries that drive a concrete next action
- keep editorial asset pickers visually lighter than the form sections they support
