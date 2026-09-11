---
title: 'Lullaby shared processing readiness'
type: 'bugfix'
created: '2026-09-11'
status: 'done'
baseline_commit: 'NO_VCS'
review_loop_iteration: 0
context:
  - 'AGENTS.md'
  - 'architecture.md'
  - 'be/docs/project-memory.md'
  - '_bmad-output/implementation-artifacts/spec-1-4-ninninin-ortak-playbackini-ve-baslik-only-localizationini.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** A completed shared LULLABY playback is incorrectly shown as `ACTION_REQUIRED` and can remain hidden from mobile delivery because `content_localizations.processing_status` deliberately remains `PENDING`. That column is a locale-row sentinel for LULLABY, not the status of its shared playback pipeline.

**Approach:** Resolve LULLABY readiness and visibility from its single `CONTENT`-scoped `DELIVERY` processing record, while preserving the localization column and its title/publication ownership. Apply the same effective-status rule in the CMS registry, public delivery reads, and localization response mappings.

## Boundaries & Constraints

**Always:** LULLABY processing has exactly one authoritative source: `asset_processing` with `target_scope = CONTENT` and `processing_kind = DELIVERY`. Missing, `PENDING`, `PROCESSING`, or `FAILED` shared jobs remain not ready; only `COMPLETED` makes a published localization visible. STORY and MEDITATION continue to use their localization processing status unchanged. Registry filtering, registry blockers, and returned readiness must use the same effective status.

**Ask First:** None.

**Never:** Copy the shared status into `content_localizations`, mutate editorial publication data in response to processing changes, loosen LULLABY's title-only localization invariant, or add a data migration solely to rewrite sentinel values.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Completed shared playback | Active LULLABY, published localization with sentinel `PENDING`, `CONTENT`/`DELIVERY` job `COMPLETED` | CMS registry returns `PUBLISHED` without `PROCESSING_NOT_COMPLETED`; public list/detail and mutation response report visible/complete | N/A |
| Shared playback not ready | LULLABY job is absent, `PENDING`, `PROCESSING`, or `FAILED` | Registry returns `ACTION_REQUIRED` with the processing blocker; public list/detail excludes the localization | No false ready state |
| Non-lullaby regression | STORY or MEDITATION localization processing is not completed | Existing locale-scoped readiness and visibility behavior remains unchanged | N/A |
| Playback rescheduled | A completed LULLABY playback source changes and the shared job returns to `PENDING` | Every locale becomes action-required and non-visible without updating its persisted sentinel | N/A |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java` -- `resolveLocalizationProcessingStatus` intentionally assigns the LULLABY sentinel; `toLocalizationRecord` already obtains the shared record and is the reusable response-mapping entry point.
- `be/src/main/java/com/tellpal/v2/content/application/ContentManagementMapper.java` -- supports a supplied effective shared `ProcessingStatus` when building localization records.
- `be/src/main/java/com/tellpal/v2/content/application/ContentPublicationService.java` -- publication response currently maps without the LULLABY effective shared status.
- `be/src/main/java/com/tellpal/v2/content/application/ContentRegistryReadRepository.java` -- registry snapshot contract currently labels the raw localization processing status; document or rename it to the effective read status.
- `be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/JdbcContentRegistryReadRepository.java` -- CTE readiness and snapshot projection both read `cl.processing_status`; join the unique content-scoped delivery job and select its status only for LULLABY.
- `be/src/main/java/com/tellpal/v2/content/application/AdminContentQueryService.java` -- builds the displayed blocker list from the registry snapshot; it must consume the same effective status as the SQL filter.
- `be/src/main/java/com/tellpal/v2/content/application/query/PublicContentQueryService.java` -- `visibleLocalization` currently reads the sentinel directly and must use `AssetProcessingApi` for LULLABY.
- `be/src/main/java/com/tellpal/v2/content/application/ContentAdminQueryMapper.java` -- established reference implementation: it resolves LULLABY status through `findByContent` and calls `isVisibleToMobile(effectiveStatus)`.
- `be/src/test/java/com/tellpal/v2/content/infrastructure/persistence/JdbcContentRegistryReadRepositoryIntegrationTest.java` -- database-backed readiness/filter/pagination coverage.
- `be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminIntegrationTest.java` -- CMS registry endpoint behavior and blocker payload coverage.
- `be/src/test/java/com/tellpal/v2/content/web/mobile/PublicDeliveryIntegrationTest.java` -- public list/detail visibility coverage.
- `be/src/test/java/com/tellpal/v2/content/ContentPublicationServiceIntegrationTest.java` -- publication result maps the shared effective status.

## Tasks & Acceptance

**Execution:**
- [x] `be/src/main/java/com/tellpal/v2/content/{application/ContentRegistryReadRepository.java,infrastructure/persistence/JdbcContentRegistryReadRepository.java,application/AdminContentQueryService.java}` -- expose and use one effective processing status in both registry candidate filtering and blocker projection; for LULLABY derive it from the content-scoped delivery job.
- [x] `be/src/main/java/com/tellpal/v2/content/application/{ContentPublicationService.java,query/PublicContentQueryService.java}` -- resolve LULLABY visibility and localization response state through `AssetProcessingApi`, reusing the existing effective-status domain method; preserve all non-LULLABY behavior.
- [x] `be/src/test/java/com/tellpal/v2/content/infrastructure/persistence/JdbcContentRegistryReadRepositoryIntegrationTest.java`, `be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminIntegrationTest.java`, `be/src/test/java/com/tellpal/v2/content/web/mobile/PublicDeliveryIntegrationTest.java`, and `be/src/test/java/com/tellpal/v2/content/ContentPublicationServiceIntegrationTest.java` -- prove completed, absent/pending/failed, and rescheduled shared-job outcomes plus non-LULLABY regression behavior.

**Acceptance Criteria:**
- Given a LULLABY localization whose stored processing status is `PENDING`, when its content-scoped delivery job is completed, then every localization's readiness and mobile visibility derive as complete without a row update.
- Given the shared job is not completed, when a registry or public query is evaluated, then the localization is action-required or excluded respectively.
- Given a non-LULLABY localization, when its processing state is evaluated, then its current localization-scoped behavior is unchanged.
- Given a LULLABY playback change schedules a new job, when the job is pending, then previously ready locales immediately cease to be ready and visible.

## Spec Change Log

## Design Notes

The processing column remains structurally required but does not own delivery state for LULLABY. Reading the authoritative content-scoped job avoids stale duplicated state and matches the existing admin-detail mapper, which already treats the job status as the effective status.

## Verification

**Commands:**
- `cd be && ./mvnw test -Dtest=JdbcContentRegistryReadRepositoryIntegrationTest,ContentAdminIntegrationTest` -- expected: registry filtering and CMS response assertions pass.
- `cd be && ./mvnw test -Dtest=PublicDeliveryIntegrationTest,ContentPublicationServiceIntegrationTest` -- expected: public visibility and publication response assertions pass.
- `cd be && ./mvnw verify` -- expected: all backend tests, Flyway checks, and module verification pass.

## Suggested Review Order

**Effective processing authority**

- Registry SQL derives LULLABY readiness from the content-scoped delivery job.
  [`JdbcContentRegistryReadRepository.java:39`](../../be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/JdbcContentRegistryReadRepository.java#L39)

- Public reads resolve mobile visibility against the same shared processing status.
  [`PublicContentQueryService.java:170`](../../be/src/main/java/com/tellpal/v2/content/application/query/PublicContentQueryService.java#L170)

- Admin localization responses never fall back to a stale LULLABY sentinel.
  [`ContentAdminQueryMapper.java:68`](../../be/src/main/java/com/tellpal/v2/content/application/ContentAdminQueryMapper.java#L68)

- Mutation responses preserve localization state while reporting effective shared readiness.
  [`ContentPublicationService.java:62`](../../be/src/main/java/com/tellpal/v2/content/application/ContentPublicationService.java#L62)

- Management mappings use PENDING when a LULLABY shared job is absent, without altering other types.
  [`ContentManagementService.java:375`](../../be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java#L375)

**Verification coverage**

- Registry tests cover completed, absent, pending, processing, and failed shared jobs.
  [`JdbcContentRegistryReadRepositoryIntegrationTest.java:117`](../../be/src/test/java/com/tellpal/v2/content/infrastructure/persistence/JdbcContentRegistryReadRepositoryIntegrationTest.java#L117)

- Public tests cover HTTP list/detail visibility, missing jobs, and playback rescheduling.
  [`PublicDeliveryIntegrationTest.java:181`](../../be/src/test/java/com/tellpal/v2/content/web/mobile/PublicDeliveryIntegrationTest.java#L181)

- Publication tests verify completed shared status in the CMS mutation response.
  [`ContentPublicationServiceIntegrationTest.java:174`](../../be/src/test/java/com/tellpal/v2/content/ContentPublicationServiceIntegrationTest.java#L174)
