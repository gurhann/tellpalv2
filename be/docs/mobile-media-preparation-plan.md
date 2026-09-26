# Mobile Media Preparation Plan

Date: 2026-09-25
Status: Proposal for joint architecture review; not implementation approval.

## Outcome and boundaries

Produce real, verified, revision-addressed media from the existing source assets so a new Flutter reader/player can be tested without releasing content to ordinary users. Then complete the production publication lifecycle. Flutter is the current leading candidate, not a benchmark-proven final selection.

The old mobile implementation is not reused. Its existing function inventory remains the behavioral reference. This plan does not reopen product discovery, introduce offline-download UI, require story ZIPs, deploy services, modify production assets or finalize the mobile architecture.

## Current repository evidence

This is a source-code inspection, not an audit of the live database/bucket or a test execution.

| Evidence | Current behavior / consequence |
| --- | --- |
| `asset/infrastructure/media/DefaultImageOptimizationAdapter.java` | Produces four cover output plans with WebP names; does not read, resize or encode source images. |
| `asset/infrastructure/media/DefaultAudioOptimizationAdapter.java` | Produces an optimized-audio path/MIME plan; does not transcode audio. Ordinary STORY delivery has no per-page audio transformation here. |
| `asset/infrastructure/media/RegisteringAssetProcessingJobExecutor.java` | Validates source registry records, registers planned outputs with unknown sizes, then completes the job; does not prove output objects exist or decode. |
| `asset/infrastructure/processing/AssetProcessingPoller.java` | Persistent polling, start and expired-lease recovery already exist and should be evolved, not replaced by fire-and-forget work. |
| `asset/infrastructure/storage/AssetStorageClient.java` | Read, upload and metadata capabilities exist for storage adapters. |
| `asset/infrastructure/storage/AssetProcessingPathBuilder.java` | Most output paths are stable per content/language, not immutable release revisions. |
| `asset/application/ContentAssetBundleService.java` | Resolves completed-job registry entries, including legacy ZIP shapes, rather than a revisioned page manifest. |
| `content/application/query/PublicContentQueryService.java` | Page reads load source illustration/audio references; current visibility/free flags must not be mistaken for a new protected-start authorization contract. |
| `content/domain/ContentPublicationPolicy.java` | Checks editorial/page readiness and sets publication status. This is not the planned immutable-candidate/device-approval lifecycle. |
| `asset/AssetProcessingIntegrationTest.java` | The inspected success case checks completed status and registered output kinds, not actual converted media bytes. |

Java paths above are relative to `be/src/main/java/com/tellpal/v2/`; the test is under `be/src/test/java/com/tellpal/v2/`.

## Proposed flow

```text
Retained originals + ordered locale-specific source snapshot
  -> durable processing job
  -> actual image/audio outputs in isolated, immutable paths
  -> byte-level and content validation
  -> versioned manifest + TEST_READY candidate
  -> authorized device playback and approval
  -> explicit LIVE promotion
```

Processing state and release state are separate. A failed/replaced candidate must never overwrite the previous live revision. A changed source snapshot invalidates approval, rather than mutating an approved manifest.

## Ownership and existing constraints

- `content` owns content/language/experience identity, ordered page snapshots, release approval and live/candidate pointers. STORY reading and narration have separate release scopes; LULLABY shared media remains content-scoped with locale-specific eligibility.
- `asset` owns source/derived media, transformations, storage and durable processing. It receives a snapshot through public contracts rather than importing content entities or introducing a circular module dependency.
- Mobile adapters remain in their owning modules. Access composition must use public APIs and verified user context; the concrete dependency direction is an architecture gate, not permission to add a central mobile business module.
- Keep localized page illustrations, localized reading covers, textless source artwork, listening artwork and static LULLABY listing artwork distinct (ADR-0006/0010/0012).
- Preserve originals and existing references. Use the configured environment prefix in the shared Firebase bucket (ADR-0008); a test preview must not require publishing production content.
- Canonical architecture currently describes ZIP delivery and publish-before-processing. Discovery/PRD require manifest delivery and candidate approval. Record these explicit amendments through ADR/project-memory/canonical-document updates before implementation; do not silently override standing architecture.

## Work sequence and acceptance gates

### M0 — Approve the source snapshot and rendition experiment

Select representative existing content by read-only inventory: one ordinary illustrated story, one heavy/text-dense story, one long audio item, and one animated lullaby. Start with one language, then exercise a second language to prove isolation. Include phone and tablet targets.

For every input capture source asset ID, immutable object version or content hash, observed MIME/bytes, pixel dimensions/orientation/animation or audio duration/codec/channels/sample rate. Capture ordered page IDs and both per-page source references. Reject missing objects, invalid media and incomplete page sets with useful diagnostics.

Proposed experimental image sizes, not production promises: list longest-edge 320/640 px; detail artwork 960/1440 px; text-bearing story pages 1080/1440 px wide, capped at source resolution with no upscaling. Compare JPEG and WebP output at several quality settings. Preserve aspect ratio and embedded text; do not bake a destructive phone/tablet crop into the source. Actual screen/DPR, legibility, decode memory and transfer size determine final profiles.

Do not mistake compressed size for decoded memory: width × height × approximately four bytes is a useful RGBA estimate, excluding extra buffers and cache overhead. Backend sizing and bounded client decoding/cache are complementary.

For audio, retain an original baseline and compare validated pass-through against a speech-appropriate encoded profile. Do not transcode already-compressed audio merely to rename it; duration, seek behavior, audible quality and download cost must justify conversion. Lullaby music and animation need their own policies; a static thumbnail is never the sole replacement for animated playback art.

**Exit:** chosen sample IDs and provisional profile matrix recorded; originals untouched. Pixel/quality values are experiments awaiting device approval, not new PRD limits.

### M1 — Real conversion and trustworthy readiness

Replace plan-only execution with storage read -> bounded transform -> temporary output -> decode/probe validation -> upload -> stored-object verification -> registry completion. Record observed size, dimensions/duration, checksum, source fingerprint and pipeline-profile version. A file extension or database row alone cannot establish readiness.

Keep the persistent job model. Add source/profile-based idempotency, immutable output identities, concurrency-safe claims and attempt fencing so a recovered stale worker cannot complete a newer attempt. Long jobs need bounded execution time and a lease strategy; avoid holding database transactions open while transforming media. Cap concurrent jobs, input pixels/frames/bytes, temporary disk, retries and process resources; serialize heavy work initially until capacity is measured.

Keep media transforms in infrastructure adapters. The canonical Thumbnailator/FFmpeg direction is the baseline, but WebP encoding must be demonstrated in the packaged runtime, not inferred from a `.webp` filename. Options to evaluate are Thumbnailator plus a verified encoder, or a narrowly scoped `cwebp` adapter. Choose after the same sample comparison; pin and verify versions before implementation. Animated artwork requires a separate animation-capable path.

**Exit:** all required objects for one story physically exist and decode/play; missing/corrupt output blocks completion; retry/restart is safe. Existing COMPLETED rows are not automatically certified: audit a selected sample and regenerate into new paths without mass deletion or overwriting.

### M2 — Minimal protected test delivery: Flutter experiment gate

Produce an immutable manifest containing schema version, content/revision/experience identity, requested and effective language, ordered pages, image rendition IDs/dimensions/MIME/bytes/checksums, audio IDs/duration/MIME/bytes/checksums and any required playback artwork. Source snapshots include all assets needed to reproduce it.

Keep expiring access credentials separate from stable asset identity and manifest integrity. An authenticated, server-authorized tester may start a test revision and obtain short-lived media access. Ordinary accounts cannot enumerate or play candidates. The mobile client must not embed admin credentials, storage service keys or a client-controlled tester bypass. Listing/detail metadata must not issue protected full-media access.

Support first-page image/audio followed by bounded adjacent preparation, without ZIP or complete-story download. Verify seek/range behavior and expiry renewal with actual delivery. Keep the admitted revision stable across renewal. Exact TTL, renewal batching and withdrawal policy remain joint architecture decisions; the access gate must precede URL issuance.

**Exit:** a real device can consume the prepared story through authenticated test delivery, start before all pages download, survive a transient network failure and renew access without changing revision. Unauthorized users are denied. This gate is enough to begin the Flutter media spike; the full production CMS release workflow need not delay it.

### M3 — Device qualification and profile selection

Use the same assets and cases across iOS/Android and phone/tablet: repeated open/close, rapid page jumps, background playback, call/headset interruptions, cold start after process death, degraded connectivity and URL expiry. Include animation and long audio before claiming overall media suitability.

Record app/OS/device, revision/profile, first-image/first-audio latency, peak and settled memory, repeated-session growth, downloaded bytes and visual/audio quality. Agree provisional budgets before qualification; do not choose passing thresholds after seeing results. A short spike is not proof of the production 99.5% crash-free target.

**Exit:** select image/audio profiles and confirm or challenge Flutter suitability using measured evidence. A media-package limitation can trigger a focused adapter experiment, not an automatic whole-app native rewrite.

### M4 — Production publication and operational hardening

Complete independent content/language/experience candidate states, device approval, explicit promotion, rollback and failure-safe live pointers. CMS should distinguish source completeness, processing, test-ready, approved/live and failed states with safe retry diagnostics. Existing screens need a focused follow-up design, not assumed permission for a redesign.

Before production: finalize ordinary/free/paid/test access rules and trusted entitlement integration, signed URL renewal/revocation policy, retention protecting live/rollback/session-required revisions, processing metrics/alerts, resource limits and recovery procedures. Verify runtime codecs in the deployment image and update the Railway runbook when implementation affects deployment. No new deployment topology is selected by this plan.

Roll out by an explicit small-content allowlist and inspect generated objects before expanding. Never reinterpret all historical completed jobs as verified media. Test rollback without deleting source assets or interrupting an existing live revision.

**Exit:** an incomplete, failed, stale or unapproved revision cannot become publicly playable; publishing/reprocessing one scope does not break another or an admitted session. Full first-release RevenueCat/logging requirements remain in force.

## Verification work to include in implementation

- Real image/audio fixtures through actual converters; verify dimensions, format, animation behavior, duration and decodability, not only mocks/registry rows.
- Storage absence, partial upload, invalid format, excessive resource use and converter failure tests.
- Testcontainers/PostgreSQL coverage for revisions, idempotency, retry, concurrency/lease fencing and migration compatibility; jqwik invariants where appropriate.
- Authorization matrix for tester/non-tester, free/locked, unpublished/candidate/live, language fallback and expired credentials.
- Modulith boundary/cycle verification and API contract tests; same-revision manifest refresh and byte-range/seek integration checks.
- Backend test/verification suite before implementation handoff; controlled real-storage checks only with configured isolation and explicit operational scope.

## Source references and decisions still needed

Local: canonical `architecture.md`, ADR-0001/0002/0006/0008/0010/0012, `be/docs/mobile-delivery-discovery.md`, current mobile PRD/addendum, and the code evidence above. Later PRD decisions override discovery's deferred-first-release RevenueCat wording.

Primary tool references checked on 2026-09-25:

- [Thumbnailator](https://github.com/coobird/thumbnailator): existing Java resizing candidate; actual encoder coverage remains a verification item.
- [Google cwebp](https://developers.google.com/speed/webp/docs/cwebp): optional WebP encoder with resize/quality controls; not a commitment to replace the canonical image stack.
- [FFmpeg](https://www.ffmpeg.org/ffmpeg.html): audio conversion/progress tooling; runtime build/version, codecs and resource constraints must be verified when binding it.

Next joint decision: approve the M0-M2 thin slice and sample selection, then specify concrete contracts and implementation stories. The plan intentionally does not fix final codecs, pixel budgets, access TTLs or a new worker deployment without evidence.
