---
name: TellPal CMS — Contents Registry
description: TellPal CMS content registry for finding content, reading selected-locale readiness, and moving into the editor.
status: draft
sources: []
updated: 2026-09-16
colors:
  surface-base: '#FAF9F4'
  surface-raised: '#FFFFFF'
  ink-primary: '#1B1C19'
  ink-secondary: '#6B655A'
  accent: '#59452B'
  warning: '#92400E'
  success: '#166534'
  border-hairline: '#E5E0D8'
typography:
  heading: {fontFamily: 'Geist Variable', fontWeight: '650', lineHeight: '1.15'}
  body: {fontFamily: 'Geist Variable', fontWeight: '400', lineHeight: '1.5'}
  meta: {fontFamily: 'Geist Variable', fontWeight: '500', lineHeight: '1.35'}
rounded:
  sm: 8px
  md: 12px
  lg: 16px
  full: 9999px
spacing:
  '1': 4px
  '2': 8px
  '3': 12px
  '4': 16px
  '6': 24px
  '8': 32px
  gutter: 24px
components:
  type-tabs:
    active-indicator: '{colors.ink-primary}'
    radius: '{rounded.sm}'
  registry-toolbar:
    surface: '{colors.surface-raised}'
    radius: '{rounded.lg}'
---

## Brand & Style

TellPal Contents is a calm editorial operations surface. The page should feel
focused and trustworthy: type is used to establish hierarchy, warm neutrals keep
the workspace approachable, and status color is reserved for operational meaning.
The mockup uses the existing CMS shell and shadcn-based primitives.

## Colors

- `surface-base` carries the page background and keeps the table readable.
- `surface-raised` is reserved for the registry toolbar and other contained work
  surfaces.
- `ink-primary` is used for titles, active tab emphasis, and primary actions.
- `ink-secondary` supports metadata and helper copy without competing with the
  content title.
- `accent` identifies demo/reference context and the ready-to-publish state.
- `warning` is used for action-required status and expanded blocker details.
- `success` is used for published status.
- `border-hairline` separates regions without introducing heavy cards.

Status must always have a text label; color is supporting information, never the
only signal.

## Typography

Use the CMS shell typography and Geist Variable. The content title is the visual
anchor, while keys, IDs, locale codes, and helper text use the meta scale. Avoid
all-caps labels except for compact technical metadata such as `TR` and `EN`.

## Layout & Spacing

The hierarchy is: page heading, content-type tabs, compact registry controls, then
the active type's table. A tab is not a type filter: it selects a separate table
schema for that content model. Search remains the dominant control for the active
type; language and readiness are compact labeled selects in the toolbar. Use the
shared 8px rhythm, 24px page gutter, and allow controls to wrap on narrow screens
rather than creating horizontal toolbar scroll.

## Elevation & Depth

Prefer one raised toolbar surface and a quiet table. Use hairline borders and the
existing shell shadow tokens. Expanded blocker details may use a warm warning
surface, but should not become a second competing panel.

## Shapes

Use 8px control corners, 12px table/detail grouping corners, and 16px for the
toolbar container. Use full rounding only for status pills and count badges.

## Components

- **Content-type tabs:** `Stories`, `Meditations`, and `Lullabies`, each with a
  record count. Audio stories belong to the Stories table, not to a separate tab.
  There is no combined `All` table. The active tab uses the shared line indicator
  and remains keyboard navigable.
- **Registry toolbar:** one search field plus labeled `Language` and `Readiness`
  selects. A reset action appears only when a non-default filter is active.
- **Readiness cell:** a text status pill with an inline blocker disclosure when
  action is required. The disclosure works with click, focus, keyboard, and touch.
- **Type-specific tables:** Stories show the verified page count field. Meditations
  and lullabies may show the domain-verified duration field; meditation duration
  is localization-scoped while lullaby playback duration is content-scoped.
  Unsupported concepts such as focus or arrangement are not invented in the
  mockup. Every table retains locale coverage and readiness. Selecting a story row
  opens its workspace directly; a redundant action column is omitted.
- **Content detail workspace:** Keep one locale workspace as the dominant editing
  surface. Locale tabs show only language and status; the selected locale owns its
  title, description, asset readiness, visibility, and processing state. Shared
  metadata and contributor assignments follow as compact sections. The right rail
  is reserved for operational summary only. Do not repeat the same status in a
  toolbar handoff card or add a generic explanatory notes card.

## Do's and Don'ts

| Do | Don't |
| --- | --- |
| Keep type navigation visibly separate from secondary filters. | Repeat type choices as a second dense filter-chip row. |
| Keep labels visible for language and readiness controls. | Depend on unlabeled icons or color-only statuses. |
| Let the table carry the operational density. | Add a generic right rail that competes with the registry. |
| Reveal every blocker through an accessible inline interaction. | Make blocker explanations hover-only. |
| Keep detail focused on one selected locale and its next action. | Duplicate status summaries across toolbar, locale cards, and the right rail. |
