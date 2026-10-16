---
id: SPEC-publish-time-image-optimization
status: draft
created: 2026-10-08
updated: 2026-10-16
companions:
  - brownfield.md
  - rendition-evaluation.md
  - client-image-contract.md
  - p0-small-jpeg-findings.md
  - implementation-plan.md
  - ../../planning-artifacts/research/technical-mobile-image-optimization-2026-10-08/research.md
  - ../../../be/docs/adr/ADR-0008-firebase-storage-direct-upload.md
  - ../../../be/docs/adr/ADR-0010-content-cover-ownership.md
  - ../../../be/docs/adr/ADR-0017-story-page-media-manifests.md
  - ../../../be/docs/adr/ADR-0019-library-home-catalog.md
  - ../../../be/docs/image-revision-readiness-contract.md
  - ../../../be/docs/adr/ADR-0021-image-revision-readiness-contract.md
  - ../../planning-artifacts/architecture/architecture-tellpalv2-2026-09-25/ARCHITECTURE-SPINE.md
sources:
  - ../../planning-artifacts/post-story-1-2-image-optimization.md
  - ../../planning-artifacts/research/technical-mobile-image-optimization-2026-10-08/task-refinement.md
---

> **Draft contract.** This SPEC and its companions preserve the requested scope and existing constraints. Assumptions and open questions remain explicit; this is not implementation or production approval.

> **P1 design accepted, implementation pending (2026-10-16).** The [revision/readiness contract](../../../be/docs/image-revision-readiness-contract.md) and [ADR-0021](../../../be/docs/adr/ADR-0021-image-revision-readiness-contract.md) fix the initial STORY/READING invariants and automatic Publish preparation/current eligible-revision promotion. Runtime and existing consumers are unchanged; P2 and the complete publication workflow remain gated.

# Publish-Time Image Optimization

## Why

Readers need legible, correctly framed artwork on phones and tablets with bounded transfer and image residency. Library thumbnails already have verified derivatives, but Story 1.2 detail still delivers original cover bytes. Editorial publication and legacy processing completion do not establish verified, appropriately sized images. Qualify the complete publication-to-visible-frame path; compressed bytes, decoded pixels and process memory require separate evidence.

## Capabilities

- **CAP-1**
  - **intent:** Publication prepares the images required by the selected content, language and experience.
  - **success:** Publish automatically starts durable preparation and automatically promotes the verified current eligible revision when every required object passes independent stored-byte verification, with no extra per-revision manual approval. READY and LIVE are separate; a missing or corrupt output cannot complete or promote it. See the accepted P1 contract.
- **CAP-2**
  - **intent:** Mobile surfaces receive verified renditions appropriate to their image role.
  - **success:** New listing, detail and reader contracts expose verified rendition metadata; consumers select quality-qualified variants for actual role/slot/DPR/fit demand and bound aspect-preserving decoding. Source/profile/resource shortages remain explicit; original uploads are not a silent fallback. See `client-image-contract.md`.
- **CAP-3**
  - **intent:** Failed or obsolete processing can be retried without disrupting admitted live content.
  - **success:** Idempotent retries and worker recovery cannot promote or fail a replacement attempt; failed candidates leave the last eligible live revision and session-required media intact.
- **CAP-4**
  - **intent:** The team selects image profiles using demonstrated quality and resource benefits.
  - **success:** Representative source/output pairs compare codecs at equal normalized geometry and record identity, actual bytes, native quality/framing, decode dimensions, first visible paint and client/worker resources for identical journeys before profiles or budgets are finalized. See `rendition-evaluation.md`.
- **CAP-5**
  - **intent:** Existing published images transition through a controlled migration.
  - **success:** Inventory, dry-run, a small cohort, output verification and rollback protect original/live/session references; historical `COMPLETED` jobs are not automatically certified.

## Constraints

- Preserve originals, localized page/reading-cover ownership, shared listening artwork, static versus animated roles, aspect ratio and revision/access boundaries. Preserve inherited framing pending qualification; no new destructive crop, alpha matte or animation flattening is implied. Upscaling cannot certify missing detail.
- `asset` owns transformation, storage and durable jobs; `content` owns publication scope. Preserve public module APIs/events/IDs, shared-bucket prefix isolation, server authorization and immutable source/profile identity. Query handlers do not transform images.
- Editorial status, processing status, READY and LIVE are distinct. The accepted P1 contract makes `content` compare desired publication generation, editorial/access eligibility and exact READY identity atomically before automatic promotion. Direct create/update, manual ready/status endpoints and legacy completion cannot certify the future new contract; existing runtime remains unchanged.
- Every new ready output carries observed metadata and verified stored bytes; extension, registry row or a job status alone is insufficient. Terminal job transitions require `PROCESSING` plus captured ownership; the first committed outcome wins, identical replay is read-only proof, contradictory callbacks cannot mutate terminal state/READY, and explicit retry claims a new attempt. Candidate/live promotion, stale callbacks and source/profile changes follow the P1 atomicity/fencing contract.
- Size by actual fitted physical pixels, including both axes under `cover`; phone/tablet names and profile bounding boxes do not establish delivered coverage. Android API 24 and iOS/iPadOS 15 remain the supported floors.
- Bound transfer/decode work and live/pending image residency as well as the decoded LRU. Retain current cache limits as controls until measured changes are justified; neither compressed size nor a cache cap establishes process-memory safety. Preserve immutable account/access-scoped cache identity, integrity, cancellation and disposal ownership.
- JPEG remains the baseline; WebP requires actual packaged encoder/client qualification. AVIF is optional with a qualified fallback and exact engine/OS/device evidence. Alpha/color/orientation, compliant-source reuse and protected HTTP caching require explicit decisions in the companions.
- **Rollout decision (human,2026-10-08):** enforce optimization for new publications first; transition existing publications through a separate controlled backfill. This does not authorize production writes or deployment.
- Real authenticated QA uses `jLRypTmoBrhhX21HsDJojurcKTC3`; credentials, tokens and signed URLs remain outside repository artifacts and telemetry.

## Non-goals

- Audio conversion, ZIP generation, reader/paywall/auth implementation or CMS redesign.
- Production deployment, bulk overwrite/deletion, replacing legacy client contracts, or implementing the complete publication workflow through this documentation task.
- Selecting final codecs, pixel/quality/byte/RAM/concurrency/preload budgets or claiming savings/crash prevention before representative evaluation; introducing a CDN is not required. The iOS screenshot envelope change is unrelated to content optimization.

## Success signal

Trace an agreed content/language/revision from retained source through verified role-specific objects and admission to the exact client rendition/decode and first onstage frame. An identical physical-device journey records quality/framing, transferred bytes, peak/settled memory, latency and failures while publication/retry preserves the previous eligible live/session revision. P0 qualification may proceed within its authorized scope; dependent implementation requires the relevant decisions below.

## Assumptions

- **A-2:** Start with STORY reading covers/detail and localized pages, then qualify other image roles. ADR-0010 keeps STORY textless source artwork separate from localized reading and shared listening covers; LULLABY static listing and playback/detail artwork are independent. Animated artwork requires a separate qualified path; the first slice does not complete all image roles. A-1 was resolved by the rollout decision above.

## Open Questions

- **OQ-2 (remaining qualification):** Which representative corpus, actual slots/DPR/framing and physical devices establish production quality, resolution, transfer, latency, worker/client memory, concurrency and preload budgets? [P0f](../../implementation-artifacts/image-optimization/p0f-quality-closure-findings.md) accepts Android 1.0× and iPhone 0.75× PNG only for the displayed TR71/TR19 portrait examples/geometries; Android 0.75× is rejected for embedded-text blur. Original raw-evidence availability has the recorded [retention limitation](../../implementation-artifacts/image-optimization/p0f-evidence-retention.json). Neither that bounded decision nor the earlier seven-cover JPEG control closes full P0 or selects production profiles.
- **OQ-3 (invariant resolved; profile admission open):** P1 permits reuse only of independently verified immutable role/source/complete-profile outputs, including formally qualifying original bytes admitted by copy/reference. It forbids raw fallback/repeated derivative encoding and requires explicit profile admission/evidence for equal/growing outputs. Concrete compliant-source/codec/geometry/quality and byte-admission profiles remain unselected.
- **OQ-4 (remaining normalization/scope):** Confirm qualified alpha/background, color space, EXIF orientation/mirroring and animation semantics; unsupported cases stay ineligible. Broader roles require separate approval. Exact fidelity needs a qualified lossless path; resizing/lossy encoding cannot promise pixel preservation. P1 captures all effective normalization settings without choosing a production policy.
- **OQ-5 (core invariants resolved; protected delivery policy open):** P1 fixes automatic preparation/promotion, READY versus LIVE, atomic manifest/job completion and content adoption, success/failure fencing, generation freshness, mutation bypass prevention, current versus retained lookup, and retention independent of mutable page-row lifetime. It grants no new access/cache/renewal right or TTL. Exact production EXT-PUBLICATION/EXT-MEDIA integration, retained-session eligibility/revocation/renewal, response caching and retention duration/cleanup policy remain gated. Do not import the whole Epic 5 workflow by implication.
- **OQ-6:** Which separately authorized native profile/release-equivalent experiment entry and instrumentation can qualify P0c while preserving current admission and the controlled Library entry's release refusal?

OQ-1 is resolved: new publications first, existing content through a separate controlled transition. P5 must specify historical-content eligibility for the new contract without disturbing existing admitted delivery.

The adopted research retains its evidence limits: 4 independently corroborated/recomputed claims and 11 single-primary-source claims without independent-publisher verification. Revalidate rolling guidance against exact deployed dependencies before implementation; its 2026-11-08 operational review target does not schedule an automation.
