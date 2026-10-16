---
title: 'P2: verified STORY reading image preparation engine'
type: 'feature'
created: '2026-10-16'
status: 'done'
baseline_commit: '7dafccf22862decb001a56cd5b904480b1528912'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/be/docs/image-revision-readiness-contract.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p2-preimplementation-investigation.md'
---

<frozen-after-approval reason="human-owned P2 engine scope">

## Intent

**Problem:** Existing page processing cannot certify the complete localized reading image set with immutable source/profile identity and atomic, attempt-fenced completion.

**Approach:** Implement a durable backend preparation engine for exact-language STORY/READING cover, detail and every ordered page. Prepare from retained originals into immutable paths and independently verify persisted objects before READY.

## Boundaries & Constraints

**Always:** Follow P1 and the mandatory implementation/verification requirements in the investigation companion. Content owns bindings; asset owns media. Public APIs/scalar IDs only; no external I/O in DB transactions. READY grants neither LIVE nor access.

Human decisions: engine/local test profiles, production disabled; PNG output directly from originals, configurable geometry/no upscale; single-frame opaque 8-bit sRGB PNG/JPEG, identity orientation. Unsupported normalization/malformed containers and source shortage fail. Growth requires explicit profile admission/evidence. Production cannot activate test profiles.

Fingerprint all effective encoder/runtime, transformation, normalization/admission and resource settings. Jointly bound bytes, pixels/frames/full raster, heap/native/process memory, disk, duration, concurrency and encoder threads. Observe process teardown before releasing capacity.

**Ask First:** Production profiles/resource admission; broader normalization/roles; access/cache/session/retention policy; changed existing publication behavior.

**Never:** P3 Publish/CMS/LIVE, consumer migration, new delivery/URLs, production operations/backfill/deletion. Preserve P0 quality and raw-evidence limitations.

## I/O & Edge-Case Matrix

| Scenario | Required behavior |
| --- | --- |
| Complete language snapshot | Verify cover/detail and every ordered page; atomically save manifest and complete owned job. |
| Missing/corrupt/partial output or unsupported source | No partial READY; actionable owned-attempt failure; retain previous revisions. |
| Duplicate/restart/conflict | Reverify immutable objects, compare full manifest equality and return matching proof; unequal conflict fails. |
| Expired/replaced/contradictory callback | No mutation of replacement or first terminal result; identical replay is read-only; explicit retry claims a new token. |
| Same ID changed bytes/profile/order/language | Different captured identity; no stale reuse or false current-source certification. |
| Empty source set/page deletion | Explicit invalid snapshot; historical scalar provenance survives mutable-row deletion. |
| Timeout/resource exhaustion | Bound work, confirm child exit and fence late callbacks; no READY. |

</frozen-after-approval>

## Code Map

Java paths start at `be/src/main/java/com/tellpal/v2/`; detailed anchors/caveats are in the mandatory investigation companion.

- `asset/application/LibraryCoverPreparationService.java#publish` — short atomic locked-job precedent.
- `asset/domain/AssetProcessing.java`, `asset/infrastructure/processing/AssetProcessingPoller.java` — claim/lease precedent.
- `asset/infrastructure/media/RegisteringAssetProcessingJobExecutor.java` — legacy page completion is non-atomic; preserve experiment behavior.
- `asset/infrastructure/storage/AssetStorageClient.java` — lacks immutable create/pinned read.
- `content/application/StoryPageManagementService.java` — localized stable IDs/order, legacy empty-list early return.
- `be/src/main/resources/db/migration/V32__add_story_page_media_revisions.sql` — avoid mutable-page FK. Latest V34; recheck numbering.

## Tasks & Acceptance

**Execution:**

- [x] `content/api/ReadingImageSourceSnapshotApi.java`, `content/application/ReadingImageSourceSnapshotService.java` — exact-language complete/invalid bindings; no Publish hooks or new asset-to-content edge.
- [x] `asset/api/ReadingImagePreparationApi.java`, `asset/domain/reading/` — typed identities, profiles, work/attempt/manifest invariants; distinguish captured proof from P3 current-generation adoption.
- [x] `asset/infrastructure/storage/` — bounded original capture, conditional immutable create, pinned reread, binding/version comparison; preserve legacy API.
- [x] `be/src/main/resources/db/migration/V35__reading_image_preparation.sql`, `asset/infrastructure/persistence/reading/` — descriptors/jobs/evidence/orphans, unique identities, scalar page provenance, atomic equality-checked finalization.
- [x] `asset/{application/reading,infrastructure/media/reading,infrastructure/processing/reading}/` — dedup, recovery/retry, supervised conversion, independent verification, guarded success/failure and integrity state; test-only admission.
- [x] `be/src/test/java/com/tellpal/v2/{asset,content}/` — every matrix row, real encoder/stored objects, PostgreSQL races/rollback, no-I/O transactions, resource limits and actual teardown.
- [x] `be/docs/reading-image-preparation.md`, `be/docs/adr/ADR-0022-reading-image-preparation-engine.md`, `be/docs/project-memory.md`, `architecture.md`, parent `implementation-plan.md` — implementation/evidence and remaining gates; apply project standards.

**Acceptance Criteria:**

- Given no qualified production profile, when production preparation is requested, then it fails closed without manufacturing production READY.
- Given a test profile/complete snapshot, when stored outputs verify, then role metadata/immutable identity are reproducible without legacy changes.
- Given concurrent workers and PostgreSQL, when success/failure/retry/restart race, then manifest/job commit together and only the owning PROCESSING attempt can finalize.

## Spec Change Log

- 2026-10-16: human selected bounded engine/test-profile, narrow source acceptance and internal-only/no-deletion options; numerical production policies remain unapproved.
- 2026-10-16: human approved this implementation specification with “A”; frozen scope is locked.

## Verification

Run focused encoder/storage/resource/process and PostgreSQL/Flyway tests, `ApplicationModulesTest`, then `bash ./mvnw test` and `bash ./mvnw verify` from `be/`. Retain logs/skips/unavailable evidence. Probe packaged Temurin 25 separately from host Java; local objects/mock GCS cannot prove live Firebase semantics. No P2 production/P3 completion claim.

## Implementation evidence

2026-10-16: bounded engine and all approved tasks implemented. Final reviewed verify: 421 Surefire + 1 Failsafe tests, no failures/errors/skips. All seven frozen matrix rows map to executed passing tests; source/order/profile/generation and equal/growing-byte admission are included. Actual packaged JAR child conversion and independent disk reread pass in pinned Temurin 25 JRE. See [verification report](../../be/docs/evidence/p2-reading-preparation/verification.md). All three independent review layers completed; nine accepted patches and the RSS exit-transition regression passed. Final focused coverage is 39 tests within the full total. The packaged probe also verifies four independent source-color sentinels. Production profiles/P3 remain gated.

## Suggested Review Order

**Preparation and module ownership**

- Capture complete localized work; keep production admission closed.
  [ReadingImagePreparationService.java:38](../../be/src/main/java/com/tellpal/v2/asset/application/reading/ReadingImagePreparationService.java#L38)

- Take an independent consistent language snapshot without media I/O.
  [ReadingImageSourceSnapshotService.java:14](../../be/src/main/java/com/tellpal/v2/content/application/ReadingImageSourceSnapshotService.java#L14)

**Atomic persistence and immutable storage**

- Persist immutable work, complete manifests, ownership and retained orphan evidence.
  [V35__reading_image_preparation.sql:2](../../be/src/main/resources/db/migration/V35__reading_image_preparation.sql#L2)

- Fence complete callbacks after row locks and before atomic commit.
  [JdbcReadingJobStore.java:74](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/persistence/reading/JdbcReadingJobStore.java#L74)

- Pin generation reads and verify create-only conflicts with actual stored bytes.
  [ReadingObjectStorage.java:24](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/ReadingObjectStorage.java#L24)

**Bounded conversion and verification**

- Coordinate retained originals, complete role verification and fenced worker outcomes.
  [ReadingImageWorker.java:36](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/processing/reading/ReadingImageWorker.java#L36)

- Supervise actual child lifetime, resource limits and bounded RSS exit confirmation.
  [ReadingImageProcess.java:25](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingImageProcess.java#L25)

- Reject unsupported container semantics before allocating a full decoded raster.
  [ReadingImageInspector.java:15](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingImageInspector.java#L15)

**Supporting contracts and evidence**

- Capture exact effective encoder, runtime, normalization and admission identity.
  [ReadingProfiles.java:43](../../be/src/main/java/com/tellpal/v2/asset/domain/reading/ReadingProfiles.java#L43)

- Exercise real PostgreSQL races, capture mutation, duration rollback and JPEG content.
  [ReadingPreparationGuardsIntegrationTest.java:87](../../be/src/test/java/com/tellpal/v2/asset/reading/ReadingPreparationGuardsIntegrationTest.java#L87)

- Prove persisted source pixels, blank-mutant rejection and actual process teardown.
  [ReadingImageProcessTest.java:18](../../be/src/test/java/com/tellpal/v2/asset/reading/ReadingImageProcessTest.java#L18)

- Separate final test/runtime proof from live Firebase and production qualification.
  [verification.md:1](../../be/docs/evidence/p2-reading-preparation/verification.md#L1)
