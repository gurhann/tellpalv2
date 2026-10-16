---
title: 'P3: automatic reading publication and CMS feedback'
type: 'feature'
created: '2026-10-17'
status: 'done'
baseline_commit: 'ed4bf60c30d806518fae3394b2d6e0fdfb1b70d5'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/cms/AGENTS.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p3-preimplementation-investigation.md'
---

<frozen-after-approval reason="human-owned P3 scope">

## Intent

**Problem:** Publish changes editorial flags without P2 current-generation adoption or preparation feedback.

**Approach:** Durable exact-language STORY/READING preparation, automatic eligible LIVE adoption and CMS pending/failure/retry feedback; no extra revision approval.

## Boundaries & Constraints

**Always:** Follow P1/ADR-0021/0022 and the mandatory investigation companion. Enable only explicit local/test scopes/profiles; default/production disabled. New internal LIVE eligibility is separate from ordinary delivery. Keep eligible prior LIVE on failure; verify before first LIVE. Serialize mutations/adoption, fence attempts/restarts and keep external I/O outside transactions. Preserve unrelated changes.

P3 accepts new immutable source-admission IDs only. Copy/verify selected originals into a reserved backend-only namespace with new original IDs; preserve originals and require explicit rebinding. Reject non-admitted IDs. Preserve P2 callers; reuse SDK conditional/pinned operations. Changed bytes require a new admitted ID.

**Ask First:** Production profiles/activation, broader roles, source-authority changes, new media access, cloud runs, deployment or backfill.

**Never:** Storage process/IPC layers, signed writes/unconditional uploads into admitted sources, automatic legacy sealing/rebinding, per-revision approval, deletion, P4 consumer migration, new session/cache/retention policy or claims of ordinary mobile rollout.

## I/O & Edge-Case Matrix

| Scenario | Required outcome |
| --- | --- |
| First Publish | Commit intent; prepare asynchronously; adopt exact current verified proof automatically. |
| Duplicate / restart | Reconcile durable work idempotently; never lose intent or duplicate LIVE. |
| Failed/invalid replacement | Retain eligible R1; expose failure and explicit retry for current obligations. |
| Mutation / obsolete completion | Advance/invalidate generation; reject stale attachment/adoption. |
| Archive / deactivation / bypass flags | Withdraw eligibility; retained evidence survives; manual flags cannot create LIVE. |
| Source / integrity change | Admission/availability epochs fence adoption; no raw fallback or status-only proof. |
| CMS polling | Preserve unsaved input/language; stop terminal polling; finite sanitized errors. |
| Disabled / production | Preserve legacy behavior; no new preparation/adoption. |

</frozen-after-approval>

## Code Map

Java paths use `be/src/main/java/com/tellpal/v2/`; CMS paths use `cms/src/`. The mandatory companion maps all supporting files/tests.

- `content/application/ContentPublicationService.java:42` — transactional command seam; source assembly currently uses REQUIRES_NEW.
- `asset/application/reading/ReadingImagePreparationService.java:38,91` — prepare/find perform storage I/O; reuse outside transactions.
- `content/application/ContentManagementService.java:101`, `StoryPageManagementService.java:50` — shared mutation guard/invalidation coverage.
- `features/contents/components/publication-actions.tsx:104`; `localization-tabs.tsx:75` — feedback; status-based form keys risk dirty-input loss.

## Tasks & Acceptance

- [x] `be/src/main/resources/db/migration/V36__reading_publication_orchestration.sql` — additive source admission, availability and content scope/generation/LIVE persistence; no historical certification.
- [x] `asset/api/ReadingSourceAdmissionApi.java`, `asset/application/reading/ReadingSourceAdmissionService.java` — verified pinned admission and durable orphan/eligibility persistence mapped in companion.
- [x] `asset/infrastructure/storage/FirebaseStorageAssetClient.java`, `AssetStorageClientRegistry.java`, `asset/application/AssetRegistryService.java` — protect reserved sources; preserve legacy uploads elsewhere.
- [x] `asset/api/ReadingPublicationReadyApi.java`, existing reading API/service/work/profile/store files — admitted pinned preparation, fresh verification outside transactions and DB-only exact adoption guard.
- [x] `content/api/ReadingPublicationApi.java`, `content/application/publication/ReadingPublicationService.java` and companion-mapped domain/store/reconciler/config files — durable generation/claim/reconcile/retry/eligibility; explicit profiles, no defaults.
- [x] `content/application/ReadingImageSnapshotAssembler.java` and mapped mutation/repository services — common lock/invalidation, metadata reuse, invalid empty pages; retained LIVE independent.
- [x] `content/web/admin/ReadingPublicationAdminController.java` and existing Publish response — authenticated capability/status/retry; document finite errors.
- [x] `features/contents/api/content-admin.ts`, `features/contents/queries/use-reading-publication.ts` and mapped models/mutations/components — truthful feedback, retry, dirty-form protection and query invalidation.
- [x] `be/src/test/java/com/tellpal/v2/content/publication/ReadingPublicationIntegrationTest.java` and companion-mapped race/HTTP/source/CMS tests — actual encoded/stored bytes and PostgreSQL matrix coverage.
- [x] `be/docs/adr/ADR-0023-reading-publication-orchestration.md`, project memory, architecture and evidence — record bounded implementation and remaining production/P4 gates.

**Acceptance Criteria:**
- Given existing legacy fixtures, when the feature is disabled or new internal LIVE changes, then ordinary delivery/listing/tester contracts remain unchanged.

## Spec Change Log

## Verification

Focused source/adoption/mutation/restart/admin tests and `bash ./mvnw verify` from `be/`, including module boundaries, real encoding/storage and no-I/O assertions. CMS Vitest, content Playwright/1280–1440 visuals and `npm run build`. Retain failures/results; no cloud run or production claim.


## Suggested Review Order

**Publish and adoption**

- Commit durable intent at the existing Publish boundary.
  [ContentPublicationService.java:48](../../be/src/main/java/com/tellpal/v2/content/application/ContentPublicationService.java#L48)

- Reconcile outside transactions; adopt only current fenced proof.
  [ReadingPublicationService.java:93](../../be/src/main/java/com/tellpal/v2/content/application/publication/ReadingPublicationService.java#L93)

- Advance desired generation when complete runtime profile identity changes.
  [JdbcReadingPublicationStore.java:60](../../be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/publication/JdbcReadingPublicationStore.java#L60)

**Immutable media and persistence**

- Create verified new source IDs without rebinding existing originals.
  [ReadingSourceAdmissionService.java:44](../../be/src/main/java/com/tellpal/v2/asset/application/reading/ReadingSourceAdmissionService.java#L44)

- Reserve observation epochs, verify stored bytes, then guard adoption.
  [ReadingPublicationReadyService.java:65](../../be/src/main/java/com/tellpal/v2/asset/application/reading/ReadingPublicationReadyService.java#L65)

- Keep historical LIVE and desired obligations separately in additive tables.
  [V36__reading_publication_orchestration.sql:36](../../be/src/main/resources/db/migration/V36__reading_publication_orchestration.sql#L36)

**Mutation and CMS feedback**

- Serialize content mutations before publication scope changes.
  [ReadingPublicationMutationGuard.java:33](../../be/src/main/java/com/tellpal/v2/content/application/publication/ReadingPublicationMutationGuard.java#L33)

- Expose truthful preparation states and finite explicit retry.
  [publication-actions.tsx:64](../../cms/src/features/contents/components/publication-actions.tsx#L64)

- Poll exact-language state independently from dirty editors.
  [use-reading-publication.ts:11](../../cms/src/features/contents/queries/use-reading-publication.ts#L11)

**Verification and supporting evidence**

- Prove automatic Publish-to-LIVE through real Spring scheduled components.
  [ReadingPublicationSchedulerIntegrationTest.java:57](../../be/src/test/java/com/tellpal/v2/content/publication/ReadingPublicationSchedulerIntegrationTest.java#L57)

- Exercise retained identity, corruption, concurrency, source admission and runtime drift.
  [ReadingPublicationIntegrationTest.java:37](../../be/src/test/java/com/tellpal/v2/content/publication/ReadingPublicationIntegrationTest.java#L37)

- Keep activation explicitly bounded to configured local/test profiles.
  [ReadingPublicationProperties.java:32](../../be/src/main/java/com/tellpal/v2/content/application/publication/ReadingPublicationProperties.java#L32)

- Review executed results and separate existing broader visual baseline failures.
  [verification.md:1](../../be/docs/evidence/p3-reading-publication/verification.md#L1)

- Inspect independent findings, repair proofs and final review closure.
  [review.md:1](../../be/docs/evidence/p3-reading-publication/review.md#L1)
