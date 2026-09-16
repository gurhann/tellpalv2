---
title: 'Implement content registry and content detail mockups'
type: 'feature'
created: '2026-09-16'
status: 'done'
baseline_revision: 'acafe9bb50dadf62e8b855bae4516f05034723c0'
review_loop_iteration: 1
followup_review_recommended: false
context:
  - 'C:/github/tellpalv2/AGENTS.md'
  - 'C:/github/tellpalv2/cms/AGENTS.md'
  - 'C:/github/tellpalv2/cms/docs/ui-standards.md'
  - 'C:/github/tellpalv2/_bmad-output/implementation-artifacts/spec-contents-registry-readiness.md'
  - 'C:/github/tellpalv2/_bmad-output/planning-artifacts/ux-designs/ux-tellpalv2-2026-09-02/DESIGN.md'
  - 'C:/github/tellpalv2/_bmad-output/planning-artifacts/ux-designs/ux-tellpalv2-2026-09-02/EXPERIENCE.md'
warnings:
  - 'Repo-wide npm.cmd run lint still reports pre-existing React hook, fast-refresh, and unused-variable errors outside this implementation scope.'
deferred:
  - 'The full CMS suite retains one pre-existing date-sensitive asset view-model failure; all 16 content files and router coverage pass.'
  - 'The npm visual script expands its arguments to the whole e2e/visual directory; the scoped Playwright command for Content and Content Detail passes.'
---

<intent-contract>

## Intent

**Problem:** The approved Contents and content-detail mockups now define a type-owned registry workflow and a focused selected-locale editor, but production routes still use the older all-types toolbar/table and a dashboard-like detail composition. The mockup behavior must become production behavior without losing real API-backed editing, publication, contributor, lullaby, or story-page workflows.

**Approach:** Align the production registry with the mockup's Stories/Meditations/Lullabies tabs, URL-preserved language/readiness/search state, accessible blocker disclosure, and type-specific verified columns. Refactor content detail around one dominant locale workspace with compact shared metadata, contributors, source-image handoff, and an operational rail while reusing existing domain-backed forms and mutations.

## Boundaries & Constraints

**Always:** Default to the Stories tab and Turkish (`tr`); keep search, language, readiness, active type, and page state server-backed and URL-preserved; keep the legacy `/api/admin/contents` response unchanged; use real production forms/actions rather than mockup fixtures; show text labels with status color; preserve selected locale when opening detail and story pages; use verified fields only (Stories page count, selected-locale Meditation duration, content-scoped Lullaby playback duration); keep toolbar/detail layouts usable at 390, 768, 1280, and 1440px without horizontal toolbar scroll.

**Block If:** A required type-specific value cannot be derived from the existing domain/API without inventing a new concept or changing a frozen publication rule.

**Never:** Import mockup fixtures/components into production routes; remove or change the legacy contents endpoint; add a combined `All` tab; add generic notes or duplicate metadata/status cards; make blocker details hover-only; replace real asset, contributor, publication, or story-page actions with fake state-only dialogs.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Default registry | `/contents` | Stories, `tr`, all readiness states, server registry request, Stories columns | Invalid query values fall back to safe defaults |
| Type tab | Select Meditations or Lullabies | URL/query type changes and table schema changes without an All view | Empty type keeps controls and a focused empty state |
| Blockers | Registry item has one or more blockers | Status and keyboard/touch disclosure reveal every blocker and page number | Row navigation is not triggered by the disclosure |
| Detail handoff | Story row opened with `language=tr` | Detail opens the same locale in the dominant workspace; story-page/source links preserve it | Missing locale uses the existing create-localization path |
| Type-specific detail | Meditation or Lullaby detail | Only domain-supported locale/shared playback controls render | Unsupported fields remain absent |

</intent-contract>

## Code Map

- `cms/src/app/routes/contents/index.tsx:45-272` -- current URL/query state, legacy type chips, registry toolbar, and create dialog; replace the toolbar composition with type tabs and compact labeled selects while preserving server query and create handoff.
- `cms/src/features/contents/components/content-list-table.tsx:45-176` -- current generic registry columns and blocker disclosure; make columns depend on the active type and render verified page/duration/locale/readiness fields without a redundant action column.
- `cms/src/features/contents/api/content-admin.ts:161-231` and `cms/src/features/contents/queries/use-content-registry.ts:11-27` -- registry schema/query contract; add only the nullable duration projection required by the approved type-specific tables.
- `be/src/main/java/com/tellpal/v2/content/api/AdminContentRegistryItem.java:11-20` -- public registry row contract; extend with nullable duration while retaining all existing fields.
- `be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/JdbcContentRegistryReadRepository.java:29-185` and `be/src/main/java/com/tellpal/v2/content/application/AdminContentQueryService.java:72-129` -- SQL projection and mapping; derive Meditation duration from the selected localization and Lullaby duration from shared playback, with no migration.
- `be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminIntegrationTest.java` and `be/src/test/java/com/tellpal/v2/content/infrastructure/persistence/JdbcContentRegistryReadRepositoryIntegrationTest.java` -- registry API/projection coverage and type-specific duration evidence.
- `cms/src/app/routes/contents/detail.tsx:29-556` -- current detail shell, toolbar, locale workspace, metadata, contributors, source-cover handoff, Lullaby playback, and operational rail; reorganize composition to match the approved detail hierarchy while retaining real hooks.
- `cms/src/features/contents/components/localization-tabs.tsx:31-290`, `cms/src/components/language/language-tabs.tsx:22-136`, and `cms/src/components/workspace/{workspace-shell,task-rail}.tsx` -- shared detail primitives; support compact locale/status tabs and responsive wrapping without duplicating status summaries.
- `cms/src/app/routes/story-pages.tsx:837-1219`, `cms/src/features/contents/components/content-form.tsx`, `content-localization-form.tsx`, `lullaby-playback-editor.tsx`, and `cms/src/features/contributors/components/content-contributor-panel.tsx` -- production handoff/editing primitives that must remain the source of truth.
- `cms/src/features/contents/components/*.test.tsx`, `cms/e2e/visual/registry-toolbar.visual.spec.ts`, `cms/e2e/visual/content-detail.visual.spec.ts`, and `cms/e2e/content.spec.ts` -- component, interaction, and four-viewport regression coverage to update/add.

## Tasks & Acceptance

**Execution:**
- `be/src/main/java/com/tellpal/v2/content/{api,application,infrastructure,persistence}` and registry tests -- expose and verify the nullable type-specific duration projection without changing legacy contracts or readiness rules.
- `cms/src/features/contents/api/content-admin.ts`, `content-list-table.tsx`, `cms/src/app/routes/contents/index.tsx` -- implement URL-synchronized type tabs, labeled selects, reset behavior, accessible blockers, type-specific verified columns, and real detail navigation.
- `cms/src/components/language/language-tabs.tsx` and `cms/src/features/contents/components/localization-tabs.tsx` -- provide compact locale/status tabs and responsive wrapping while preserving existing category/story-page consumers.
- `cms/src/app/routes/contents/detail.tsx` plus existing production content/contributor/story-page components -- implement the mockup hierarchy with one dominant locale workspace, compact shared metadata/contributors/source handoff, and an operational-only rail.
- `cms/src/features/contents/**/*.test.tsx`, `cms/e2e/content.spec.ts`, `cms/e2e/visual/{registry-toolbar,content-detail}.visual.spec.ts` -- cover tab/query behavior, blocker interaction, locale handoff, type-specific rendering, and 390/768/1280/1440 visual states.

**Acceptance Criteria:**
- Given `/contents` is opened, when the page loads, then Stories is active, Turkish is selected, the request is server-filtered, and no All tab or type chip row is shown.
- Given a type tab or labeled filter changes, when the URL changes, then the selected type/language/readiness/search/page are reflected in the registry request and reset preserves the active type.
- Given a row has blockers, when the disclosure is activated by mouse, keyboard, or touch, then all blocker labels and page numbers are visible and row navigation remains separate.
- Given a registry row is selected, when detail opens, then the same locale is active and the editor, publication controls, story-page handoff, and source-image handoff share that locale.
- Given any supported content type is opened, when the detail renders, then one locale workspace dominates, shared metadata/contributors remain compact, the rail contains at most three operational stats, and no generic notes or duplicated profile/status cards appear.
- Given the four supported viewport widths, when registry and detail surfaces render, then controls remain readable, keyboard-accessible, and free of horizontal toolbar overflow.

## Spec Change Log

- '2026-09-16: Implemented the approved production registry/detail surfaces and added the nullable type-specific duration projection.'

## Review Triage Log

### Review loop 1

- Blind hunter: the asynchronous post-save invalidation was retained intentionally so successful create/update navigation is not blocked by unrelated active count refetches; list/detail caches are updated before the callback and refresh failures remain non-fatal.
- Edge-case hunter: the direct Lullaby playback join was retained because `lullaby_playbacks.content_id` is uniquely constrained; count badges now render `—` until data exists, supported locale codes are normalized, safe page parsing is enforced, and the locale selection is keyed by content id to avoid stale cross-record state.
- Intent alignment: patched missing-locale/editor consistency, preserved registry type/language/readiness/search/page context through detail handoff, corrected E2E/API Lullaby fixtures to use shared playback duration, and added selected-locale/detail-specific assertions.
- Verification-gap hunter: added assertions for Stories page count/readiness, multiple blocker disclosure with page number, Lullaby table/API duration, selected Turkish detail links, and registry query-context round trips. Visual tests now wait for the selected-locale heading and exercise `/contents/1?language=tr`.
- Scope check: reverted the unrelated asset expiry fixture change; generated test metadata and result artifacts are excluded from the reviewed change.

## Design Notes

The mockups are presentation references, not production state machines. Their visual grouping should be retained, but every save, publish, asset, contributor, and story-page action must continue through the existing API-backed components. The registry duration field is a read-only projection: it is selected-locale scoped for Meditations and content-scoped for Lullabies; Stories keep page count as their type-specific column.

## Verification

**Commands:**
- `cd be; .\\mvnw test` -- expected: backend tests pass, including registry duration projection coverage.
- `cd cms; npm run test` -- expected: CMS unit/component tests pass.
- `cd cms; npm run build` -- expected: TypeScript and production build pass.
- `cd cms; npm run test:e2e -- e2e/content.spec.ts` -- expected: content interaction flows pass.
- `cd cms; .\\node_modules\\.bin\\playwright.CMD test e2e/visual/registry-toolbar.visual.spec.ts e2e/visual/content-detail.visual.spec.ts --workers=1 --grep \"contents registry toolbar visual|content detail visual\"` -- expected: scoped registry/detail visual coverage passes at all configured viewports.

## Auto Run Result

- Status: DONE
- Baseline: `acafe9bb50dadf62e8b855bae4516f05034723c0`
- Review loop: 1 completed; no unresolved intent gaps.
- Backend verification: `mvnw test` passed with 323 tests; targeted `ContentAdminIntegrationTest` passed with 21 tests after the final API assertion.
- CMS verification: scoped content/router tests passed with 16 files and 79 tests; production build passed; changed-file ESLint passed; content E2E passed; scoped Content/Content Detail visual coverage passed 8/8.
- Known repository-level verification notes are recorded in `warnings` and `deferred` above.
