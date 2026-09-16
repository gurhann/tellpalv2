---
name: TellPal CMS — Contents Registry
status: draft
sources: []
updated: 2026-09-16
---

## Foundation

This is a responsive CMS registry and content-detail workspace for an editorial
user. It uses the existing TellPal application shell, shared shadcn-based
primitives, and the visual tokens in `DESIGN.md`. The registry remains a working
list, while the detail route provides a focused locale editing handoff rather
than a dashboard.

## Information Architecture

`Contents` → content-type tabs → compact registry controls → active type table →
row-level editor handoff → selected-locale workspace. A type tab changes the table
schema, not merely the rows shown in one combined table. The detail workspace keeps
the selected locale primary, then exposes shared metadata and compact contributor
assignments; operational indicators stay in a small rail.

## Voice and Tone

| Situation | Copy direction |
| --- | --- |
| Page introduction | Calm, direct, task-oriented: find content and understand its selected-locale readiness. |
| Status | Use `Action required`, `Ready to publish`, and `Published`; avoid internal processing jargon as the primary label. |
| Blockers | State each missing requirement plainly and without priority ranking. |
| Empty result | Explain which search or filter produced the empty state and offer a clear reset. |
| Mockup-only action | Make the boundary explicit so a prototype is not mistaken for a saved production action. |

## Component Patterns

- The type tab row is a semantic tablist with counts and a visible active
  indicator. `All` is intentionally absent. Audio narration is a STORY
  localization feature, not a separate row or tab; each canonical type owns its
  own table schema and may wrap on small screens.
- The registry toolbar groups controls by visible label. Search has the strongest
  width and emphasis; selects stay compact.
- A row with blockers shows its status and an inline disclosure trigger. Expanded
  blockers render in the row and remain available to keyboard and touch users.
- A content row keeps the title primary. The Stories table shows the verified
  page count field. Meditations and lullabies may show the verified duration
  field, with meditation duration following the selected localization and
  lullaby playback duration remaining content-scoped. External key, ID, locale,
  and readiness remain supporting context.
- The detail route has one dominant locale workspace. Locale tabs carry language
  and status only; the selected locale shows its content and verified readiness
  fields. Shared metadata and contributor assignments are compact follow-on
  sections, while the rail contains operational indicators only.

## State Patterns

- **Default:** `Stories`, Turkish selected, all readiness states, and the most
  recently edited ordering.
- **Active type:** switching a tab changes both the current rows and the columns.
  Reset clears search, language, and readiness without leaving the current type.
- **Action required:** a warning status and a disclosure for all blockers; no
  blocker receives artificial priority.
- **Empty:** preserve the controls and provide a focused reset path.
- **Reference/demo:** keep mockup fixtures clearly labeled so review context is
  not confused with production content.

## Interaction Primitives

The named protagonist is Ece, an editor reviewing the Turkish catalog. She can
choose a type tab, search by title/key/ID, change locale or readiness, inspect all
blockers, and open a demo workspace from a row. Tabs must support arrow-key
navigation, selects must expose their labels to assistive technology, and row
actions must not rely on hover.

## Accessibility Floor

Use semantic tabs, labeled comboboxes, visible focus styles, text status labels,
and an accessible blocker region. The blocker disclosure must expose its expanded
state and stop its click from accidentally opening the row. No toolbar or tab row
may require horizontal scrolling at the supported CMS breakpoints.

## Responsive & Platform

The mockup is checked at 390, 768, 1280, and 1440px. At desktop widths the search
and secondary selects share one toolbar row. At tablet and mobile widths the
search takes the first row, controls stack or wrap, and tabs wrap without clipping
or introducing a horizontal scroll container.

## Key Flows

### Ece reviews what needs action

1. Ece opens the Stories table with Turkish selected.
2. She switches to a type when she needs that model's dedicated fields.
3. She reads the row-level readiness status and type-specific columns.
4. If action is required, she opens the blocker disclosure and sees every missing
   requirement.
5. She selects the story row to enter the editor context and lands in the selected
   locale workspace.

### Ece reviews one locale without losing the operational context

1. Ece opens the detail workspace with the locale selected from the registry.
2. She switches locale tabs when she needs to review another localization.
3. She works from one locale summary and readiness grid instead of reconciling
   repeated status cards.
4. She checks shared metadata and contributor assignments below the locale work.
5. She uses the operational rail for visibility, processing, and story-page
   context, then opens the story-page mockup when needed.

### Ece narrows the registry without losing context

1. Ece searches for a title, external key, or content ID.
2. She changes Language or Readiness using the labeled selects.
3. The selected type, locale, result count, and reset action remain visible.
