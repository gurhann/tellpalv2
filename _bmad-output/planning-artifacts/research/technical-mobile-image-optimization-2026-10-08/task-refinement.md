# Post-Story 1.2 task refinement

Date: 2026-10-08. Status: research recommendations; no production profile or implementation approval.

Feeds [existing follow-up](../../post-story-1-2-image-optimization.md) and [draft SPEC](../../../specs/spec-publish-time-image-optimization/SPEC.md). Preserve the existing P0–P5 phases and approved new-publications-first/separate historical-transition order. This companion adds research-derived acceptance work; it does not silently change the SPEC, ADRs or project memory. Evidence: [research](research.md), [local context](local-context.md).

## Recommended execution slices

| Slice | Concrete result | Acceptance evidence/dependency |
| --- | --- | --- |
| P0a — Slot and source qualification | Enumerate actual card,detail,reader/listening-art slots; physical pixels,fit,alignment/orientation,source aspect and embedded-text safe region. | Phone/tablet portrait/landscape plus high DPR. Existing thumbnail bounding boxes are compared against required coverage, not called exact output sizes. Low-resolution originals are flagged; no invented detail through upscaling. |
| P0b — Same-geometry codec experiment | Keep current JPEG control; compare JPEG qualities from existing protocol and actual packaged WebP lossy/lossless candidates at the same normalized pixels. | Add large illustrated covers,pages,text/diacritics,fine lines,gradients,alpha,EXIF rotation/ICC plus existing small-JPEG regression controls. Freeze exact IDs/hashes and encoder settings. No equal-q or universal percentage comparison. |
| P0c — Resource and quality qualification | Collect real transferred bytes,first onstage paint,actual decode dimensions,peak/settled process/native memory and worker resources. | Physical low RAM Android,oldest supported iPhone/iPad plus modern phone/tablet; profile/release-equivalent entry with existing admission. Accepted dev entry refuses release; do not remove that guard by implication. Preserve accepted caches through an isolated experiment namespace. |
| P1 — Ready-rendition and compliant-source policy | Decide required roles,manifest metadata,source/profile/attempt fences,atomic ready/live promotion and explicit reuse of already compliant sources. | Resolve SPEC OQ3–5. A verified copied/referenced compliant encoded object may avoid new lossy encoding only if formally admitted as the new role/profile rendition. No silent raw fallback. Keep stale attempts and failed replacements from changing last eligible live/session revisions. |
| P2/P3 — Bounded real production preparation | Generate/verify outputs in durable jobs under byte,pixel,time,memory/temp-disk and concurrency bounds; connect readiness to all publication entry paths. | Real bytes and reread SHA/MIME/dimensions/decode,not filename/status. Existing listing fencing is a reusable model; page jobs and legacy plan-only entries require separate treatment. Codec installed in actual runtime must be demonstrated. |
| P4a — Role/slot-aware consumer | Select smallest qualified manifest variant covering actual fitted physical pixels; bound decoded dimensions without ratio distortion. | Native cases include tall and wide sources,high DPR,split/window changes and source shortage. Preserve current framing until separately agreed. Bound target-size buckets; avoid variant proliferation across tiny resize changes. |
| P4b — Residency and scheduling | Coordinate image work across Library/detail/future reader; visible work first,small measured preload window,cancel obsolete ownership and release listeners. | Rapid scroll/detail/back/page turns,URL renewal,corruption,account switch and memory pressure. Warm identity excludes expiring signedURL but includes immutable rendition/profile and access scope. Keep integrity,promotion,disposal and retry semantics. |
| P5 — Controlled historical transition | Separate dry-run/cohort/backfill and rollback after new-publication behavior is qualified. | Preserve originals,last live,retained sessions and revision/access boundaries. Storage totals and network journey savings are reported separately. |

## Decisions to record after P0

- Per-role quality means acceptable appearance/legibility at intended display; exact pixel fidelity requires a lossless path. Resizing and lossy coding are not mathematically lossless.
- Select rendition pixel/byte bounds and permitted quality tradeoffs from representative physical-device measurements. Existing values and experiment candidates are controls,not validated new budgets.
- Select decoded-LRU budget,live-image/concurrency/preload window and low RAM policy jointly. Retain current 32MiB/48-entry controlled cache until evidence justifies a change; never treat it as process-memory ceiling.
- Decide alpha/background,color space,EXIF normalization and animation separately. Do not approve white matting or a first-frame still through the generic image optimization label.
- WebP is an experiment candidate; AVIF is optional later capability work with guaranteed fallback and exact Flutter/OS/device proof. Neither is required to close first JPEG sizing improvements.

## Exit evidence

A reviewer can trace one content/language/revision from retained source → verified role-specific object → admitted manifest → exact client rendition/cache/decode → first visible paint. Compare an identical user journey against its control with raw observations and explicit cache states. Quality rejection,source shortage or exceeded resource limits remains visible and cannot be reported as optimization success. Passing a finite device matrix reduces risk; it does not prove every old phone crash-free.

Next BMad step: use bmad-spec to update the existing draft from these findings,starting with P0a/P0b and the unresolved readiness/compliant-source decisions. Implementation begins only after the relevant decisions in the existing draft are resolved.
