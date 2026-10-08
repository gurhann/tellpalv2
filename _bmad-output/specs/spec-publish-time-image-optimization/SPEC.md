---
id: SPEC-publish-time-image-optimization
status: draft
created: 2026-10-08
updated: 2026-10-08
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
  - ../../planning-artifacts/architecture/architecture-tellpalv2-2026-09-25/ARCHITECTURE-SPINE.md
sources:
  - ../../planning-artifacts/post-story-1-2-image-optimization.md
  - ../../planning-artifacts/research/technical-mobile-image-optimization-2026-10-08/task-refinement.md
---

> **Draft contract.** This SPEC and its companions preserve the requested scope and existing constraints. Assumptions and open questions remain explicit; this is not implementation or production approval.

# Publish-Time Image Optimization

## Why

Readers need legible, correctly framed artwork on phones and tablets with bounded transfer and image residency. Library thumbnails already have verified derivatives, but Story 1.2 detail still delivers original cover bytes. Editorial publication and legacy processing completion do not establish verified, appropriately sized images. Qualify the complete publication-to-visible-frame path; compressed bytes, decoded pixels and process memory require separate evidence.

## Capabilities

- **CAP-1**
  - **intent:** Publication prepares the images required by the selected content, language and experience.
  - **success:** A requested revision becomes delivery-ready only after every required optimized object exists and passes independent stored-byte verification; a missing or corrupt output cannot complete it.
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
- Editorial status, processing status and deliverable revision readiness are distinct. Direct create/update, manual ready/status endpoints and legacy completion must not bypass the new delivery gate.
- Every new ready output carries observed metadata and verified stored bytes; extension, registry row or a job status alone is insufficient. Candidate/live promotion, stale callbacks and source/profile changes require explicit atomicity/fencing contracts.
- Size by actual fitted physical pixels, including both axes under `cover`; phone/tablet names and profile bounding boxes do not establish delivered coverage. Android API 24 and iOS/iPadOS 15 remain the supported floors.
- Bound transfer/decode work and live/pending image residency as well as the decoded LRU. Retain current cache limits as controls until measured changes are justified; neither compressed size nor a cache cap establishes process-memory safety. Preserve immutable account/access-scoped cache identity, integrity, cancellation and disposal ownership.
- JPEG remains the baseline; WebP requires actual packaged encoder/client qualification. AVIF is optional with a qualified fallback and exact engine/OS/device evidence. Alpha/color/orientation, compliant-source reuse and protected HTTP caching require explicit decisions in the companions.
- **Rollout decision (human,2026-10-08):** enforce optimization for new publications first; transition existing publications through a separate controlled backfill. This does not authorize production writes or deployment.
- Real authenticated QA uses `jLRypTmoBrhhX21HsDJojurcKTC3`; credentials, tokens and signed URLs remain outside repository artifacts and telemetry.

## Non-goals

- Audio conversion, ZIP generation, reader/paywall/auth implementation or CMS redesign.
- Production deployment, bulk overwrite/deletion, replacing legacy client contracts, or implementing the complete candidate approval workflow through this planning task.
- Selecting final codecs, pixel/quality/byte/RAM/concurrency/preload budgets or claiming savings/crash prevention before representative evaluation; introducing a CDN is not required. The iOS screenshot envelope change is unrelated to content optimization.

## Success signal

Trace an agreed content/language/revision from retained source through verified role-specific objects and admission to the exact client rendition/decode and first onstage frame. An identical physical-device journey records quality/framing, transferred bytes, peak/settled memory, latency and failures while publication/retry preserves the previous eligible live/session revision. P0 qualification may proceed within its authorized scope; dependent implementation requires the relevant decisions below.

## Assumptions

- **A-2:** Start with STORY reading covers/detail and localized pages, then qualify other image roles. ADR-0010 keeps STORY textless source artwork separate from localized reading and shared listening covers; LULLABY static listing and playback/detail artwork are independent. Animated artwork requires a separate qualified path; the first slice does not complete all image roles. A-1 was resolved by the rollout decision above.

## Open Questions

- **OQ-2:** Which corpus, actual slots/DPR/framing and physical constrained devices establish quality, resolution, transfer, latency, worker/client memory, concurrency and preload budgets? The seven-cover JPEG evidence remains a bounded control; research adds no device benchmark or final budget.
- **OQ-3:** What qualifies an already compliant encoded object for verified copy/reference reuse under the new role/profile contract, and when are equal-size or larger outputs acceptable? Research recommends avoiding unnecessary lossy generations; reuse/admission and byte-versus-quality acceptance remain undecided. No silent raw fallback is permitted.
- **OQ-4:** Confirm alpha/background, color space, EXIF orientation/mirroring, animation semantics and non-STORY scope. Exact fidelity needs a qualified lossless path; resizing/lossy encoding cannot promise pixel preservation.
- **OQ-5:** Agree publication/ready/live atomicity, retained-session access, legacy/admin compatibility and protected HTTP cache/renewal policy with EXT-PUBLICATION/EXT-MEDIA examples. Do not import the whole Epic 5 workflow by implication.
- **OQ-6:** Which separately authorized native profile/release-equivalent experiment entry and instrumentation can qualify P0c while preserving current admission and the controlled Library entry's release refusal?

OQ-1 is resolved: new publications first, existing content through a separate controlled transition. P5 must specify historical-content eligibility for the new contract without disturbing existing admitted delivery.

The adopted research retains its evidence limits: 4 independently corroborated/recomputed claims and 11 single-primary-source claims without independent-publisher verification. Revalidate rolling guidance against exact deployed dependencies before implementation; its 2026-11-08 operational review target does not schedule an automation.
