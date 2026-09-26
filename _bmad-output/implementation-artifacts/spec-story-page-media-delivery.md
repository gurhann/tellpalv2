---
title: 'Prepare and Deliver Versioned Story Page Media'
type: 'feature'
created: '2026-09-26'
status: 'done'
review_loop_iteration: 0
baseline_commit: 'a2a2e9b2bd7fbc49061ed72fb3d83d30676b6e72'
context:
  - 'be/docs/mobile-media-preparation-plan.md'
  - 'architecture.md'
  - 'be/docs/adr/ADR-0006-story-page-illustration-localization.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Asset processing currently registers planned image outputs and marks jobs complete without reading, transforming, uploading, or verifying bytes. Mobile story pages therefore receive original high-resolution images, which can increase memory use and crash risk.

**Approach:** Build a real, repeatable pipeline for localized story-page image renditions and expose ready outputs through an immutable, revisioned page-media manifest suitable for a protected Flutter playback experiment. Keep original assets and current legacy delivery available during this test.

## Boundaries & Constraints

**Always:** Keep localized source ownership in `content` and transformation/storage ownership in `asset`, communicating only through public module APIs. A processing job may complete only after every required output exists and its MIME type, byte size, checksum, and image decodability are verified. Revisioned outputs must not overwrite assets referenced by an older manifest. Use environment-prefixed storage paths. Add byte-level tests using generated fixtures; no real production media is required. The initial slice optimizes page illustrations only; page audio remains the existing source reference. Update canonical architecture and record any durable contract decision in an ADR/project memory as required by repository standards.

**Ask First:** Halt before choosing a materially different public mobile API, making test media anonymously accessible, changing production media, deleting old revisions, or committing fixed production rendition limits without device/content evidence. If test-only access cannot be safely scoped using existing authorization, present the options before implementing that exposure.

**Never:** Remove or rewrite original media; require legacy clients/tablets to migrate; introduce UI/UX changes; transcode audio or add interactive stories/parent guides; claim a job is ready based only on database rows; use production assets or credentials in tests; clean up revisions as part of this slice.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Ready revision | Ordered localized pages with valid source images | Create verified rendition objects and publish a manifest whose entries preserve page order and identify revision/profile | Retry through existing persistent job lifecycle; never publish partial readiness |
| Invalid source | Missing, unreadable, or unsupported image | No manifest entry is marked ready; job remains failed/retryable according to existing policy | Record actionable failure without exposing storage internals |
| Page source changes | New source fingerprint for a page | Create a new immutable revision; old manifest and objects remain readable | Do not overwrite the prior revision on collision |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/asset/infrastructure/media/` -- current plan-only transform adapters; replace planning-only success with actual image processing.
- `be/src/main/java/com/tellpal/v2/asset/application/` -- persistent processing executor, registry integration, and output verification boundary.
- `be/src/main/java/com/tellpal/v2/asset/api/` -- public asset processing and delivery contracts; avoid exposing asset internals to content.
- `be/src/main/java/com/tellpal/v2/content/application/query/PublicContentQueryService.java` -- localized page sources are resolved here for mobile delivery.
- `be/src/main/java/com/tellpal/v2/content/web/mobile/` -- current page response exposes original illustration/audio references.
- `be/src/test/java/com/tellpal/v2/asset/` and `.../content/web/mobile/` -- extend processing and public delivery integration coverage.
- `architecture.md`, `be/docs/adr/`, `be/docs/project-memory.md` -- align canonical delivery design with the manifest approach.

## Tasks & Acceptance

**Execution:**
- [x] Add an ordered page-image source snapshot through a public content API contract; persist enough source identity to detect changed inputs and produce stable revisioned outputs.
- [x] Implement bounded image decoding/encoding and storage upload; register actual metadata and verify stored bytes before completing the job. Keep rendition profiles configurable and avoid upscaling; document any experimental defaults.
- [x] Expose a revisioned page-media manifest for the test playback path, preserving page order and existing audio references. Keep the test route/access protected and leave legacy delivery unchanged.
- [x] Update architecture/ADR/project memory for the approved manifest and processing invariants; add unit and integration tests for success, corrupt/missing inputs, retry, revision immutability, metadata, and protected delivery.

**Acceptance Criteria:**
- Given valid page-image sources, when processing finishes, then each manifest image points to a real decodable derivative with matching persisted MIME, size, and SHA-256, and the job is complete only after verification.
- Given an invalid source or failed upload, when processing runs, then no incomplete revision is published as ready and the persistent retry/failure behavior remains observable.
- Given a page image is replaced, when a new revision is processed, then the prior revision remains unchanged and both manifest revisions resolve to their respective bytes.
- Given a legacy client requests current story pages, when it uses existing delivery, then its behavior and original source availability remain compatible.
- Given a request outside the authorized test path, when it requests test-only delivery, then it cannot retrieve protected test media.

## Design Notes

An asset job needs an ordered snapshot of page IDs and localized source asset IDs, not just `pageCount`. The snapshot must cross the module boundary through a public `content.api` contract; the `asset` module must not query content tables. Revision identity should incorporate source fingerprints and the rendition profile/version, so a changed source or encoder profile cannot silently reuse stale bytes. The existing mobile pages response can guide field naming, but a manifest is a new versioned delivery contract rather than a mutation of old assets.

## Verification

**Commands:**
- `cd be && ./mvnw test` -- expected: backend unit and integration tests pass.
- `cd be && ./mvnw verify` -- expected: full verification and module checks pass.

**Manual checks:**
- Inspect generated fixture outputs and manifest revisions; verify an older manifest continues to resolve after a source update.
- Confirm unauthorized test delivery is rejected and legacy page delivery still references the original sources.

**Verification Results:**
- `env TESTCONTAINERS_RYUK_DISABLED=true mvn -q -Djava.awt.headless=true test` -- passed (complete backend suite, including Docker-backed integration tests).
- `env TESTCONTAINERS_RYUK_DISABLED=true mvn -q -Djava.awt.headless=true verify` -- passed (full Maven verification).
- Testcontainers Ryuk was disabled because Docker Desktop's raw socket cannot be mounted in this environment; test containers shut down normally.

## Suggested Review Order

Review the processing boundary first, then image transformation and storage verification, followed by revision persistence and protected delivery.

1. [Persistent processing executor](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/media/RegisteringAssetProcessingJobExecutor.java:44)
2. [Bounded page-image rendition processor](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/media/PageImageRenditionProcessor.java:19)
3. [Manifest persistence](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/persistence/JdbcStoryPageMediaManifestStore.java:21)
4. [Protected test delivery endpoint](../../be/src/main/java/com/tellpal/v2/user/web/mobile/TestStoryPageMediaController.java:37)
5. [Manifest schema migration](../../be/src/main/resources/db/migration/V32__add_story_page_media_revisions.sql:8)

Review the automated evidence last, covering generated image bytes, revision immutability, retry behavior, migration compatibility, and delivery authorization.

6. [Image transformation unit tests](../../be/src/test/java/com/tellpal/v2/asset/infrastructure/media/PageImageRenditionProcessorTest.java:13)
7. [Processing and persistence integration tests](../../be/src/test/java/com/tellpal/v2/asset/AssetProcessingIntegrationTest.java:59)
8. [Protected delivery tests](../../be/src/test/java/com/tellpal/v2/user/web/mobile/TestStoryPageMediaControllerTest.java:37)
9. [Architecture decision](../../be/docs/adr/ADR-0017-story-page-media-manifests.md)
