---
title: "Align production content detail with the approved mockup"
type: "feature"
created: "2026-09-19"
status: "done"
baseline_revision: "eb54da0237f5c4f705c05e0646f6e33225d90a44"
baseline_commit: "eb54da0237f5c4f705c05e0646f6e33225d90a44"
review_loop_iteration: 0
followup_review_recommended: true
context:
  - "/Users/gurhankucuk/Documents/GitHub/tellpalv2/AGENTS.md"
  - "/Users/gurhankucuk/Documents/GitHub/tellpalv2/cms/AGENTS.md"
  - "/Users/gurhankucuk/Documents/GitHub/tellpalv2/cms/docs/ui-standards.md"
  - "/Users/gurhankucuk/Documents/GitHub/tellpalv2/cms/docs/ui-regression-task-list.md"
  - "/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/docs/adr/ADR-0016-cms-web-only-support-scope.md"
warnings: []
deferred: []
---

<intent-contract>
  <intent>
    Bring the real CMS content detail route into visual and structural alignment with the approved Variant A content-detail mockup while preserving real content APIs, localization editing, publication actions, contributor assignments, and story-page handoff behavior.
  </intent>
  <approach>
    Use the mockup route as the visual information-architecture reference, keep the production route's real feature components, make the shared asset editor compact enough for an editorial web workspace, and harden the visual test so it exercises the authenticated content screen rather than a login redirect.
  </approach>
  <boundaries>
    <always>
      <rule>Keep the content detail route read/write behavior and existing API contracts unchanged.</rule>
      <rule>Use one dominant locale workspace, followed by shared metadata, contributor assignments, and story source handoff sections.</rule>
      <rule>Keep operational readiness information in the right rail and keep the primary editorial actions in the locale workspace header.</rule>
      <rule>Validate the same full application shell at the supported web widths of 1280 and 1440 pixels.</rule>
      <rule>Use real production components and data on the route; mockup fixtures remain reference-only.</rule>
    </always>
    <block_if>
      <rule>Alignment would require removing a real field, action, or type-specific editor that is still supported by the API.</rule>
      <rule>The visual harness cannot mock every preview request needed to keep the route authenticated and deterministic.</rule>
    </block_if>
    <never>
      <rule>Do not add mobile/tablet-specific CMS composition or mobile screenshot baselines; the CMS is a web-only admin surface under ADR-0016.</rule>
      <rule>Do not import mockup route fixtures or mockup-only components into production content detail.</rule>
      <rule>Do not reintroduce duplicate route status cards, generic notes, or a second competing page identity.</rule>
    </never>
  </boundaries>
  <io>
    <inputs>
      <item>Content detail payload, localization summaries, contributor assignments, asset metadata, and story-page capability state from the existing admin queries.</item>
      <item>Approved Variant A mockup hierarchy and existing CMS visual standards.</item>
    </inputs>
    <outputs>
      <item>A production content detail screen with the mockup's hierarchy and editorial density at laptop and desktop web widths.</item>
      <item>Deterministic visual regression coverage that cannot pass on an authentication redirect.</item>
      <item>A concise visual-difference report captured in implementation notes and commit history.</item>
    </outputs>
  </io>
  <edge-case-matrix>
    <row scenario="authenticated-story-detail">
      <input>Story content detail payload with localized cover, contributor assignments, and story-page support.</input>
      <expected>Production detail renders the full web shell, metadata-first hierarchy, locale workspace, contributors, and source-image handoff.</expected>
      <error>Preview-token requests are mocked; a login redirect is a test failure.</error>
    </row>
    <row scenario="non-story-detail">
      <input>Valid content detail payload for a type without story pages.</input>
      <expected>Shared metadata, locale workspace, optional type-specific editor, and contributor operations remain available.</expected>
      <error>Story-only actions and source-image sections remain absent.</error>
    </row>
    <row scenario="asset-preview-failure">
      <input>Preview token or browser media load fails.</input>
      <expected>Existing error and retry UI remains usable without changing route composition.</expected>
      <error>No session redirect or uncaught render failure is allowed.</error>
    </row>
  </edge-case-matrix>
</intent-contract>

## Current evidence and root causes

The real route currently renders an additional identity/header card before the workspace, places localization before shared metadata, and renders the editor image preview as a tall portrait stage. The mockup renders shared metadata first, then a compact locale workspace with actions, then contributors and source images. This is a structural and density mismatch, not only a styling mismatch.

The production visual test also captured only `main` while the mockup test captured the full page. Its fixture mocked media metadata but not `/api/admin/media/:id/content-token`; the real preview request returned 401, cleared the session, and allowed a login screenshot to become the apparent baseline. The test must mock the preview token and capture the same `body` surface as the mockup.

## Code map

- `cms/src/app/routes/contents/detail.tsx`: production route composition and action placement.
- `cms/src/app/routes/mockups/content-detail.tsx`: approved Variant A reference composition.
- `cms/src/features/contents/components/content-page-shell.tsx` and `cms/src/components/workspace/workspace-shell.tsx`: shared shell/header behavior.
- `cms/src/features/assets/components/asset-field-preview.tsx`: shared editor image density and preview surface.
- `cms/e2e/visual/content-detail.visual.spec.ts`: production route fixture, preview-token mocking, and full-shell screenshot.
- `cms/e2e/visual/registry-toolbar.visual.spec.ts`: mockup screenshot reference.
- `cms/e2e/visual/visual-test-helpers.ts`: supported web visual viewport matrix.

## Implementation tasks

1. Align production route composition with the mockup: suppress the duplicate route header, put shared metadata before the locale workspace, and move the real route actions into the locale workspace section without removing supported actions.
2. Reduce the shared editor image preview's unnecessary stage height and padding while retaining preview, metadata, asset management, lightbox, and error behavior.
3. Keep type-specific sections, contributor operations, source-image handoff, localization fields, and publication controls intact; shorten only redundant explanatory copy where it creates a competing hierarchy.
4. Make the production visual fixture mock every preview-token request and assert the authenticated content detail heading before taking the screenshot. Capture the full body at 1280 and 1440 only.
5. Refresh only the intentionally changed web baselines after visual inspection and remove generated failure artifacts from the source tree.

## Acceptance criteria

- Given a valid Story detail payload, when the production route renders, then its visible section order is metadata, locale workspace, optional type-specific playback, contributor assignments, and source-image handoff.
- Given the production detail route is loaded, when the shell is inspected, then there is no duplicate route identity card above the detail sections and the primary navigation and editorial actions remain available.
- Given an image asset is selected in an editor field, when the detail screen renders at a supported web width, then the preview surface is compact and does not create a mostly-empty portrait column that dominates page height.
- Given the visual fixture loads the content route, when a screenshot is captured, then the test has asserted the authenticated content heading, mocked preview-token requests, and passed against the same full body surface at 1280 and 1440 widths.
- Given CMS unit, visual, and build checks run, when they complete, then they pass without introducing mobile/tablet visual requirements.
- Given existing content detail API data and actions, when this alignment is applied, then backend-facing behavior and read-only production route guarantees remain unchanged.

## Verification

```text
cd cms
npm run test -- --run
npm run test:e2e -- e2e/visual/content-detail.visual.spec.ts e2e/visual/registry-toolbar.visual.spec.ts --project=chromium --workers=1
npm run build
```

Implementation is ready only after the screenshot comparison confirms the production and mockup use the same shell framing and the remaining differences are intentional data-driven differences rather than accidental layout differences.

## Review Triage Log

### 2026-09-19 — Review pass

- intent_gap: 0
- bad_spec: 0
- patch: 6 (high 1, medium 4, low 1)
- defer: 0
- reject: 15
- addressed_findings:
  - `[high|patch]` Restored the route-state header and toolbar for invalid, loading, not-found, and query-error states while keeping the loaded detail route headerless.
  - `[medium|patch]` Updated the browser flow assertion to observe the localized title input after the route hierarchy changed.
  - `[medium|patch]` Made the visual preview-token fixture future-dated and added a supported-width horizontal-overflow assertion.
  - `[medium|patch]` Added DOM-order and action-containment assertions for metadata, locale workspace, contributors, and source handoff.
  - `[medium|patch]` Added the non-story assertion that story-only source-cover UI is absent.
  - `[low|patch]` Updated stale test fixtures for current asset expiry and the nullable listing-cover field so the complete unit suite remains deterministic.

## Auto Run Result

### Implemented change

The production content detail route now follows the approved Variant A hierarchy at supported web widths: metadata first, locale workspace with primary actions, optional type-specific editor, contributor assignments, and source-image handoff. The duplicate route identity card was removed only for the loaded detail state, while fallback states retain their route context. The shared editor image preview was compacted and its narrow-column text wrapping was corrected.

### Visual difference report

- The pre-change production screen had an extra route header/status card; the mockup did not. The loaded production route now uses the shared app shell directly.
- Production rendered localization before shared metadata; the mockup rendered metadata first. The production section order now matches the mockup.
- Production actions were isolated in the route header; the mockup placed them in the locale workspace header. The real actions now live in that section.
- The editor image preview used a tall portrait stage and could collapse long asset paths into a narrow vertical column. Stage sizing, parent grid minimums, and word wrapping were corrected for the supported web widths.
- The visual test compared only `main` and omitted preview-token mocks, allowing a login redirect to become a false baseline. It now mocks content tokens, asserts the authenticated locale heading, captures the full body, and checks horizontal overflow.
- Remaining height differences are intentional: production retains real localization, publication, contributor, and asset controls that the fixture mockup represents with compact static cards.

### Files changed

- `cms/src/app/routes/contents/detail.tsx` — production hierarchy, actions, and fallback-state shell behavior.
- `cms/src/features/assets/components/asset-field-preview.tsx` and `cms/src/features/assets/components/asset-picker-field.tsx` — compact editor preview layout.
- `cms/src/features/contents/components/content-localization-form.tsx` — web-width cover/metadata grid sizing.
- `cms/e2e/visual/content-detail.visual.spec.ts` and visual snapshots — authenticated full-shell regression coverage.
- `cms/e2e/visual/registry-toolbar.visual.spec.ts-snapshots/` — supported web baselines for mockup reference routes.
- `cms/docs/ui-standards.md`, `cms/docs/ui-regression-task-list.md`, `cms/e2e/visual/visual-test-helpers.ts`, `be/docs/adr/ADR-0016-cms-web-only-support-scope.md`, and `be/docs/project-memory.md` — durable CMS web-only scope.
- Route/integration/E2E tests — updated expectations and hierarchy/action assertions.

### Verification

- `cd cms && npm run test -- --run` — 88 test files and 368 tests passed.
- `cd cms && npm run test:e2e -- e2e/content.spec.ts --project=chromium --workers=1` — passed.
- `cd cms && npm run test:e2e -- e2e/visual/content-detail.visual.spec.ts e2e/visual/registry-toolbar.visual.spec.ts --project=chromium --workers=1` — 18 visual tests passed at 1280 and 1440.
- `cd cms && npm run build` — passed; Vite emitted only the existing chunk-size warning.
- Manual screenshot comparison confirmed the same full application shell and intentional data-driven height differences.

### Review outcome and residual risks

- Six patch findings were fixed in this pass; no intent gaps or spec repairs were required. Fifteen review suggestions were rejected as outside the explicit web-only/alignment scope or as already covered by existing behavior/tests.
- Follow-up review recommendation: `true` because one high-severity fallback-state regression was patched; the score is high-severity 1, medium-severity 4, low-severity 1.
- Mobile/tablet CMS fidelity remains intentionally unsupported under ADR-0016. The untracked `yuklenecek_hikayeler/` user content was preserved and excluded from this change.
