# CMS UI Regression Task List

This list tracks UI debt, regression coverage, and standards follow-up work separately from the feature backlog.

## Status Key

- `DONE`
- `TODO`
- `BLOCKED`

## P1

### UI-REG-004 `TODO` Move remaining registry routes to the shared toolbar

- Problem pattern:
  - remaining registries can still drift through local `FilterBar` composition
- Affected routes:
  - `/contributors`
  - `/media`
  - `/free-access`
  - `/media-processing`
- Required standard:
  - same shared registry toolbar contract as contents and categories
- Regression guard:
  - route interaction tests
  - visual screenshot baselines
- Exit criteria:
  - all registry routes use the same toolbar primitive

### UI-REG-005 `TODO` Add screenshot checklist for detail and workspace routes

- Problem pattern:
  - detail header, rail, and workspace balance can regress without behavioral failures
- Affected routes:
  - `/contents/:contentId`
  - `/categories/:categoryId`
  - `/contents/:contentId/story-pages`
- Required standard:
  - screenshot checklist with stable viewports and deterministic data
  - detail workspace routes must be covered at the supported web widths `1280` and `1440`
  - content and category detail must keep one dominant workspace and a compact operational rail
- Regression guard:
  - visual screenshot assertions
- Exit criteria:
  - visual baselines exist for `/contents/:contentId` and `/categories/:categoryId`
  - story pages follow the same checklist when that route is added

## P2

### UI-REG-006 `TODO` Align production and mockup layout primitives

- Problem pattern:
  - production and mockup surfaces can drift when they share intent but not structure
- Affected routes:
  - mockup registry and detail routes
  - production registry and detail routes
- Required standard:
  - production-safe shared primitives reused across both when possible
- Regression guard:
  - mockup regression tests
  - visual checklist updates
- Exit criteria:
  - layout primitives are shared where appropriate without production importing mockup-only code

## Completed Tasks

Full records are in the [archive](ui-regression-task-archive.md); load it only for relevant history.

### UI-REG-001 `DONE` Create a shared registry toolbar contract

[Archived record](ui-regression-task-archive.md#ui-reg-001-done-create-a-shared-registry-toolbar-contract).

### UI-REG-002 `DONE` Move contents and categories to the shared registry toolbar

[Archived record](ui-regression-task-archive.md#ui-reg-002-done-move-contents-and-categories-to-the-shared-registry-toolbar).

### UI-REG-003 `DONE` Add first visual baselines for registry toolbar surfaces

[Archived record](ui-regression-task-archive.md#ui-reg-003-done-add-first-visual-baselines-for-registry-toolbar-surfaces).

### UI-REG-003A `DONE` Remove generic production notes cards from rails

[Archived record](ui-regression-task-archive.md#ui-reg-003a-done-remove-generic-production-notes-cards-from-rails).

### UI-REG-003B `DONE` Remove duplicated rail profile summaries

[Archived record](ui-regression-task-archive.md#ui-reg-003b-done-remove-duplicated-rail-profile-summaries).

### UI-REG-003C `DONE` Simplify content detail into a single primary workspace

[Archived record](ui-regression-task-archive.md#ui-reg-003c-done-simplify-content-detail-into-a-single-primary-workspace).
