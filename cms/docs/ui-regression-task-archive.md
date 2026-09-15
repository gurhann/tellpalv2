# CMS UI Regression Task Archive

Completed records; consult when investigating their routes or regression history.
See the [active task list](ui-regression-task-list.md) for open work.

## P0

### UI-REG-001 `DONE` Create a shared registry toolbar contract

- Problem pattern:
  - registry routes could compose low-level filter layout ad hoc and create poor spacing balance
- Affected routes:
  - `/contents`
  - `/categories`
- Required standard:
  - shared `RegistryToolbar` with fixed `search`, `actions`, `filters`, and `summary` slots
- Regression guard:
  - component contract test
- Exit criteria:
  - routes stop composing low-level `FilterBar` directly for registry toolbar layout

### UI-REG-002 `DONE` Move contents and categories to the shared registry toolbar

- Problem pattern:
  - search field visually compressed, summary competing with filters, empty space imbalance
- Affected routes:
  - `/contents`
  - `/categories`
- Required standard:
  - search maintains stable width
  - summary sits below filters
  - visible filter group labels
- Regression guard:
  - route interaction tests
  - visual screenshot baselines
- Exit criteria:
  - contents and categories registry toolbars match the shared layout contract

### UI-REG-003 `DONE` Add first visual baselines for registry toolbar surfaces

- Problem pattern:
  - layout regressions were not detectable through interaction tests alone
- Affected routes:
  - `/contents`
  - `/categories`
- Required standard:
  - screenshot coverage at `390`, `768`, `1280`, `1440`
- Regression guard:
  - Playwright screenshot assertions
- Exit criteria:
  - visual baselines are committed and runnable via `npm run test:e2e:visual`

### UI-REG-003A `DONE` Remove generic production notes cards from rails

- Problem pattern:
  - right rails accumulated generic notes cards that repeated workflow prose and added visual clutter
- Affected routes:
  - `/contents`
  - `/categories`
  - `/media`
  - `/contents/:contentId`
  - `/categories/:categoryId`
  - `/contents/:contentId/story-pages`
- Required standard:
  - production rails show metrics, status, summaries, and direct handoff context only
  - generic notes panels are not allowed
- Regression guard:
  - route interaction assertions
- Exit criteria:
  - production note-card headings are removed and guarded by tests

### UI-REG-003B `DONE` Remove duplicated rail profile summaries

- Problem pattern:
  - right rails repeated metadata that was already visible and editable in the main lane
- Affected routes:
  - `/contents/:contentId`
- Required standard:
  - rails carry operational summary only and do not mirror the metadata form
- Regression guard:
  - route interaction assertions
- Exit criteria:
  - duplicated profile summary cards are removed from the content detail rail

### UI-REG-003C `DONE` Simplify content detail into a single primary workspace

- Problem pattern:
  - header, rail, and nested child sections all summarized the same content state, so the next editing step was unclear
- Affected routes:
  - `/contents/:contentId`
- Required standard:
  - one dominant locale-first workspace
  - minimal operational rail
  - no duplicate child section ownership
- Regression guard:
  - route assertions
  - visual screenshot baseline
- Exit criteria:
  - content detail reads as one primary workflow with metadata and contributor work as secondary lanes
