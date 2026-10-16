# STORY Reading Image Preparation

Status: bounded P2 engine implemented/tested, 2026-10-16; workflow reviews complete. [ADR-0022](adr/ADR-0022-reading-image-preparation-engine.md) records the approved boundary and the [verification report](evidence/p2-reading-preparation/verification.md) records actual test/runtime outcomes. This document does not admit production profiles or connect Publish/CMS/LIVE.

## Internal boundary

`content::api` exposes `ReadingImageSourceSnapshotApi`. Capture resolves one exact canonical language, the localized reading cover for both cover/detail obligations, and every ordered localized page illustration with stable page/localization scalar provenance. No language fallback, textless source substitution or listening cover is permitted. Missing content/localization/cover/page or an empty page set is an explicit invalid snapshot. Capture uses a short independent REQUIRES_NEW / REPEATABLE_READ transaction, including when called from an outer READ_COMMITTED transaction, and performs no image I/O.

`asset::api` exposes `ReadingImagePreparationApi` with `prepare`, `find` and explicit `retry`. The snapshot is a captured binding value, not a publication generation. P3 must compare current content bindings, immutable source admission and the still-desired eligible publication generation before adoption. READY does not mean LIVE. The existing Publish operation, localization statuses, listing placeholders, test manifests, originals and public consumers are unchanged.

There is no new HTTP preparation or media endpoint, signed URL, client permission, TTL, cache policy or cleanup command. Production admission remains closed.

## Admission and source capture

The engine is disabled by default. Test use requires explicit `tellpal.asset.reading.test-engine-enabled=true`, an admitted local/test runtime and a non-production environment prefix. Production profiles and production storage-prefix admission are rejected even if the flag is supplied. Do not enable this against a production environment. Automated fixtures use isolated test storage; local Firebase storage remains the existing real shared bucket under configured prefix isolation.

Every required role needs an explicit `TestProfile`. There is no default production pixel size, factor, quality, byte saving, memory or concurrency budget. The descriptor captures all effective settings, including bounds, no-upscale geometry/rounding/resampling, normalization/admission, PNG provider/version and exact Java/runtime/OS identity. A descriptor change changes profile identity. Test numbers and synthetic growth evidence are not production approval.

Sources must be registered original IMAGE assets. Capture observes and pins an object generation, reads bounded actual bytes, checks observed MIME against actual format, validates supported semantics and source sufficiency, computes SHA-256 and retains a create-only immutable capture. It rechecks original registry/path/generation bindings after capture. Changed bindings fail; matching asset IDs alone do not establish freshness. Workers reread pinned retained source bytes and verify their hash before transforming.

The narrow admission path supports only the implemented subset of opaque, single-frame 8-bit sRGB PNG/JPEG with identity orientation. Container/header/metadata checks precede full decode. Unsupported alpha, ICC/color conversion, EXIF/mirroring, animation, bit depth and malformed/trailing data fail explicitly. This is intentionally more restrictive than a generic ImageIO decoder; accepting frame zero or silently ignoring metadata is insufficient. Preserve source detail and aspect ratio; source shortage cannot be solved by upscaling.

## Identity and persistence

P1 ASCII canonical JSON and domain-separated SHA-256 bind the role/source/profile rendition identity and the exact scope/ordered obligations revision. Publication generation and signed URLs are absent from media identity. Source observation/provenance is retained, but unchanged bytes must not require repeated lossy conversion merely because an original object generation changes.

V35 adds separate reading-image jobs, immutable manifests, source-capture ownership, attempt evidence and integrity observations. Each table has a primary key; FK/query indexes cover job/manifest/integrity/capture lookup. Diagnostic deduplication indexes a short MD5 expression instead of arbitrary-length error text; it has no role in SHA-256 media identity. Revision uniqueness deduplicates work; jobs retain the complete captured work/profile descriptors. Page/localization IDs in retained evidence are historical scalars, not FKs to editable page rows. The existing V32 experiment is not silently migrated or newly certified.

Claim captures job ID, incrementing attempt, unique lease, lease expiry, source-set and profile-set fingerprints. Finalization locks the owning job, checks the complete token and PROCESSING state, validates complete ordered obligations and writes manifest plus COMPLETED in one transaction. Full immutable equality is required on insert conflict. Failure uses the same ownership fence. Lease time is refreshed after the row lock and immediately before commit; time measured before a blocking lock cannot extend ownership.

The first committed terminal result wins. Identical terminal replay returns read-only proof; contradictory replay or expired/replaced callbacks cannot mutate READY, terminal state or a replacement attempt. Explicit retry applies to failed work and creates a new claimed attempt. Durable polling discovers pending and expired processing work after restart. A crash after object upload can leave isolated retained objects; no READY follows unless the complete output set independently verifies and commits.

## Conversion and resource accounting

Conversion uses a supervised child JVM launched from the same Java home. The packaged executable JAR must also locate and launch the child; Surefire-only classpath success is insufficient evidence. The child decodes originals and emits PNG directly, without derivative-to-derivative encoding. Resizing once per output preserves aspect ratio, avoids upscaling and strips only metadata permitted by the admitted semantics. PNG encoding is lossless; resized pixels are not identical to full-source pixels.

Profiles jointly describe encoded source/output bounds, header pixels/frames, full-raster estimates, parent allocations, heap/direct memory, process RSS, temporary space, source/revision duration, concurrency and encoder threads. The test engine accepts one local reservation and at most 102 obligations (cover/detail plus 100 pages); those effective limits are included in the descriptor, not admitted production defaults. Shared local capacity covers preparation/verification as well as conversion. Before parent allocation, the declared reservation must fit both one quarter of actual parent maximum heap and half of currently allocatable heap. This parent admission is separate from child profile deserialization. Each prepare call, READY lookup and claimed worker execution uses one transient monotonic revision budget across all its own stages: capture/binding rechecks/scheduling, stored rereads, or worker conversion/storage/finalization. Each operation is capped by its execution’s remaining time; durable queue waiting and explicit later attempts do not share one in-memory timer. The budget is not a profile identity change. An owned failure remains recordable after budget exhaustion while its lease is still valid. Deadline/resource failures must leave no READY. Capacity is not released merely because a future was cancelled: the supervisor forces termination and waits for actual child exit, including interruption paths. If OS RSS disappears during child exit notification, a single bounded 20 ms wait must confirm actual exit; a live unobservable child still fails closed. Lease expiry still requires callback fencing; it does not kill a worker.

The reading-only GCS client clone uses explicit connect/read/RPC deadlines and one attempt without changing legacy client settings. Pinned streams additionally enforce encoded bytes and elapsed read duration. These adapter settings are included in the profile. Local disk fixtures and SDK API compatibility do not substitute for live network timeout verification.

Resource checks and synthetic tests are not production qualification. Sampling can miss short peaks; Java heap is not whole-process memory, and per-JVM limits are not a fleet-wide replica budget. Record actual measurements and unavailable native/container evidence. Read-time subsampling is not admitted by this engine slice.

## Stored verification, reuse and retention

New source/rendition objects live in separate environment-prefixed reading namespaces and use conditional immutable creation. Pinned reread and full byte equality are required when a path already exists; path existence alone is not success. No original object is overwritten. The complete manifest binds immutable provider/path/version, actual MIME/bytes/SHA and output dimensions for every required role/page.

Verification reopens stored output versions independently of encoder buffers, checks actual format plus storage MIME, positive bytes, SHA-256, exact expected dimensions and full decode. Missing, corrupt, truncated, partial or mismatched output prevents the whole revision from becoming READY. A registry row or successful write does not suffice.

READY lookup revalidates stored objects before reuse. Integrity/availability observations remain separate from immutable historical manifest identity; corruption fails closed rather than silently replacing bytes or falling back to originals. A larger/equal output is accepted only where the captured profile explicitly admits growth and supplies evidence. Test-only growth admission grants no production permission.

Retain originals, immutable sources, verified revisions and attempt/orphan provenance. There is no deletion or garbage collection. Retention does not confer public access, renewable session rights or cache permission.

## Verification and remaining gates

The [codec preflight](evidence/p2-imageio-preflight/README.md) proves actual host Java and deployment-base JRE PNG/JPEG encoding, persisted file reread and negative missing/truncated cases. It does not prove the integrated P2 pipeline, live Firebase semantics, production quality or the complete packaged backend.

Required engine acceptance includes actual child conversion, independent persisted-object corruption negatives, complete role membership, no-I/O transaction assertions, PostgreSQL/Flyway concurrency/rollback/retry/restart/terminal fencing, retained-page lifetime and observed process teardown. Run module boundaries and full Maven test/verify and retain raw output; skipped or unavailable tests do not count as passing evidence. Verify the packaged JRE separately and distinguish local generation-aware fixtures/mock SDK tests from live GCS network proof.

P0 Android 1× and iPhone 0.75× PNG acceptance remains limited to the shown TR71/TR19 portrait examples/geometries. Android 0.75× embedded text was blurry; JPEG/WebP sharpness remains unresolved. Missing original raw native ledgers/sidecars/build/cleanup evidence remains a limitation. Broader corpus/device/geometry/resource qualification, production profiles, protected delivery/session/retention policy, P3 orchestration, P4 consumers and P5 migration/rollout remain separate gates.
