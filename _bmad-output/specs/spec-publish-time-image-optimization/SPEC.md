---
id: SPEC-publish-time-image-optimization
status: draft
created: 2026-10-08
companions:
  - brownfield.md
  - rendition-evaluation.md
  - p0-small-jpeg-findings.md
  - implementation-plan.md
  - ../../../be/docs/adr/ADR-0008-firebase-storage-direct-upload.md
  - ../../../be/docs/adr/ADR-0010-content-cover-ownership.md
  - ../../../be/docs/adr/ADR-0017-story-page-media-manifests.md
  - ../../../be/docs/adr/ADR-0019-library-home-catalog.md
  - ../../planning-artifacts/architecture/architecture-tellpalv2-2026-09-25/ARCHITECTURE-SPINE.md
sources:
  - ../../planning-artifacts/post-story-1-2-image-optimization.md
---

> **Draft contract.** This SPEC and its companions preserve the requested scope and existing constraints. Assumptions and open questions remain explicit; this is not implementation or production approval.

# Publish-Time Image Optimization

## Why

Readers should receive appropriately sized cover and content images after publication, rather than raw uploads. Library thumbnails already have verified derivatives, but Story 1.2 detail still delivers original cover bytes. Existing editorial publication and legacy processing completion do not establish that every required image was actually optimized. Plan a verified publication-to-delivery contract with measurable quality, size and recovery criteria.

## Capabilities

- **CAP-1**
  - **intent:** Publication prepares the images required by the selected content, language and experience.
  - **success:** A requested revision becomes delivery-ready only after every required optimized object exists and passes independent stored-byte verification; a missing or corrupt output cannot complete it.
- **CAP-2**
  - **intent:** Mobile surfaces receive verified renditions appropriate to their image role.
  - **success:** New listing, detail and reader contracts reference rendition IDs with actual MIME, dimensions, byte size, checksum, profile and revision; they do not silently fall back to original uploads.
- **CAP-3**
  - **intent:** Failed or obsolete processing can be retried without disrupting admitted live content.
  - **success:** Idempotent retries and worker recovery cannot promote or fail a replacement attempt; failed candidates leave the last eligible live revision and session-required media intact.
- **CAP-4**
  - **intent:** The team selects image profiles using demonstrated quality and resource benefits.
  - **success:** Representative source/output pairs record actual before/after sizes, dimensions, checksums, encoder identity and phone/tablet quality/resource measurements before profiles and acceptance budgets are finalized.
- **CAP-5**
  - **intent:** Existing published images transition through a controlled migration.
  - **success:** Inventory, dry-run, a small cohort, output verification and rollback protect original/live/session references; historical `COMPLETED` jobs are not automatically certified.

## Constraints

- Preserve originals and localized page/reading-cover ownership, shared listening artwork, static versus animated roles, aspect ratio and existing revision/access boundaries. Do not upscale or introduce destructive crops by default.
- `asset` owns transformation, storage and durable jobs; `content` owns publication scope. Preserve public module APIs/events/IDs, shared-bucket prefix isolation, server authorization and immutable source/profile identity. Query handlers do not transform images.
- Editorial status, processing status and deliverable revision readiness are distinct. Direct create/update, manual ready/status endpoints and legacy completion must not bypass the new delivery gate.
- Every new ready output carries observed metadata and verified stored bytes; extension, registry row or a job status alone is insufficient. Candidate/live promotion, stale callbacks and source/profile changes require explicit atomicity/fencing contracts.
- Retain client bounded decoding, cache ownership and integrity checks. Smaller compressed files alone do not establish lower decoded memory or production performance.
- **Rollout decision (human,2026-10-08):** enforce optimization for new publications first; transition existing publications through a separate controlled backfill. This does not authorize production writes or deployment.
- Real authenticated QA uses `jLRypTmoBrhhX21HsDJojurcKTC3`; credentials, tokens and signed URLs remain outside repository artifacts and telemetry.

## Non-goals

- Audio conversion, ZIP generation, reader/paywall/auth implementation or CMS redesign.
- Production deployment, bulk overwrite/deletion, replacing legacy client contracts, or implementing the complete candidate approval workflow through this planning task.
- Selecting final codecs, pixel/quality/byte budgets or claiming measured savings before representative evaluation; the iOS screenshot envelope change is unrelated to content optimization.

## Success signal

An agreed representative content revision demonstrates verified role-specific outputs, legible illustrations/text on phone and tablet, recorded before/after transfer and decode measurements, failure-safe publication/retry and continued access to the previous admitted revision. Implementation can start only after the relevant profile, readiness and rollout questions below are resolved.

## Assumptions

- **A-2:** Start with STORY reading covers/detail and localized pages, then qualify other image roles. ADR-0010 keeps STORY textless source artwork separate from localized reading and shared listening covers; LULLABY static listing and playback/detail artwork are independent. Animated artwork requires a separate qualified path; the first slice does not complete all image roles. A-1 was resolved by the rollout decision above.

## Open Questions

- **OQ-2:** Which representative high-resolution assets and devices establish final resolution, quality, transfer and worker/client memory budgets? The bounded seven-cover JPEG experiment is recorded in `p0-small-jpeg-findings.md`; it does not close full P0 or approve a production profile.
- **OQ-3:** For already-small or already-optimized sources, must a new encode be smaller, or can a verified rendition meet the profile with equal bytes? No silent raw fallback is permitted.
- **OQ-4:** Confirm transparency, color/orientation and animation requirements and the rollout scope for non-STORY artwork.
- **OQ-5:** Agree the minimal publication/ready-revision and last-live retention contract, compatibility of legacy/admin status paths, and its relationship to EXT-PUBLICATION/EXT-MEDIA. Do not import the whole Epic 5 workflow by implication.

OQ-1 is resolved: new publications first, existing content through a separate controlled transition. P5 must specify historical-content eligibility for the new contract without disturbing existing admitted delivery.
