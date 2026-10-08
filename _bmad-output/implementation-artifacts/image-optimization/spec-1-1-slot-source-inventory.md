---
title: 'Image Optimization P0a.1 — Slot and Source Inventory'
type: chore
created: 2026-10-08
status: done
baseline_commit: b411ebe50fec7b012c71e61bcb8e659e4540620d
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/epic-1-context.md'
  - '{project-root}/_bmad-output/specs/spec-publish-time-image-optimization/client-image-contract.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Current image limits do not establish whether available pixels meet display demand.

**Approach:** Inventory accepted Library/recommendations/detail, legacy reader PoC and unimplemented reader/listening roles for Story 1.1. Deliver reproducible evidence and gaps; full P0 qualification remains open.

## Boundaries & Constraints

**Always:** Preserve originals, historical evidence, accepted UI/cache behavior and production settings. Separate code observations, hypothetical calculations and device measurements. Follow repository/backend/Flutter guidance; exclude private locations, credentials and URLs.

**Ask First:** Expansion into source acquisition, runtime experiments or application/policy changes.

**Never:** Fetch production media, authenticate, encode, change profiles/budgets/admission, deploy or alter Mobile1.2. Leave workflow `story_key` unset; synchronize only the dedicated tracker explicitly, preserving shared BMad configuration.

</frozen-after-approval>

## Code Map

Read-only repository paths: `M=app/mobile/lib`, `A=be/src/main/java/com/tellpal/v2/asset`, `O=_bmad-output/implementation-artifacts`.

- `M/features/library/presentation/view/library_view.dart:205`; `M/features/story_detail/presentation/view/story_detail_view.dart:480,163`: card height `.230*viewportHeight+16`, width `.75*height`; Hero full width/ratio `.9` phone, `1.1` tablet. SafeArea does not reduce MediaQuery size.
- `M/core/preview/presentation/widgets/preview_card.dart:41,198`: breakpoint 600, tablet→phone fallback, cover/center, decodeWidth 360/600. `M/features/story_detail/presentation/widgets/story_detail_hero.dart:17`: source-ratio cap 1080/1600×2048 and 4194304 pixels; no upscale, cover/center.
- `M/story_player_page.dart:107,216,325`: variable Expanded contain/center; unbounded precache. `M/app/router.dart:12`: accepted Library/detail only. `M/core/network/{library_image_cache,story_detail_image_cache}.dart`: distinct ownership/cache controls, not process-memory evidence.
- `A/infrastructure/media/{ListingCoverProfile,ListingCoverRenditionProcessor}.java:10,44`: q85 JPEG bounding boxes 360×480/600×800. `A/application/StoryDetailHeroQueryService.java:48`: localized original. `A/infrastructure/media/{PageImageRenditionProcessor,DefaultImageOptimizationAdapter}.java:25,20`: page experiment versus legacy plans.
- `O/mobile/library-1-2-existing-artwork-evidence.json`: 21 bindings/19 source hashes, 52 observations/38 rendition hashes. `O/mobile/library-1-2-http-evidence.json`, `/matrices/*/cases/*/hero`: 29 observations/8 hashes; `actualSources` is incomplete. Artwork lacks IDs/MIME.
- `_bmad-output/specs/spec-publish-time-image-optimization/experiments/{corpus.json,small-jpeg-results.json,color-diagnostic/color-comparison.json}`: six controls plus separate P3 diagnostic, not representative masters/pages.

## Tasks & Acceptance

All output paths below are relative to `O/image-optimization/`.

**Execution:**

- [x] `p0a-slot-source-inventory.json` — save role/status, anchors, input hashes/full Git HEAD/dirty provenance, logical slot formulas/constraints, viewport/insets/DPR/fit/alignment, source/rendition identity/dimensions/ratio/orientation/normalization, unclamped demand, bounded decode and gaps. Mark unobserved orientation unavailable. Allowlist evidence fields/pointers; unknown is not false.
- [x] `verify-p0a-inventory.py` — independently reproduce two-axis contain/cover arithmetic with rational/Decimal rounding; validate provenance, joins, counts and completeness. Cover phone/tablet, rotation/window, fractional DPR, tall/wide and 100×100→200×200 shortage with fixed expected values. Separate original, rendition and decode shortages; clamping cannot erase demand.
- [x] `p0a-slot-source-inventory.md` — explain framing/overlays and decode/cache limits. List missing masters, real text pages, alpha, EXIF rotation, ICC diversity and animation with next actions. Mark unmeasured text-safe regions/native observations unavailable; link JSON/verifier.

**Acceptance Criteria:**

- Given current inputs, when inventoried, then every role/cell has traceable evidence or an explicit gap; plans never count as encoded bytes.
- Given saved geometry, when verified, then both axes/shortages reproduce; source-capped decode cannot qualify insufficient detail.
- Given repeated evidence, when counted, then bindings/observations/unique hashes stay distinct; IDs/MIME require verified joins; corpus/native gates remain open.
- Given closure, when reviewed, then only dedicated artifacts/tracker transitions changed; source code, evidence and other sprints are unchanged.

## Spec Change Log

## Verification

- Run `python3 _bmad-output/implementation-artifacts/image-optimization/verify-p0a-inventory.py`; require arithmetic/provenance/join/completeness checks to pass.
- Check local links, privacy and diff scope; rehash retained inputs before/after. Application suites do not apply to this artifact-only change.

Execution verification passed: 22 retained inputs, 17 fixed arithmetic/decode fixtures, 137 complete cells and 31 rejected malformed projections. Three independent review layers produced one deduplicated patch: require complete verifier-owned evidence mappings. The patch and negative checks passed; no unresolved findings remain. Full P0/native/corpus gates remain open.

## Suggested Review Order

**Evidence and demand**

- Start with bounded results, inherited framing and explicit evidence gaps.
  [p0a-slot-source-inventory.md:1](p0a-slot-source-inventory.md#L1)

- Keep physical demand independent of source-capped decoding.
  [client-image-contract.md:5](../../specs/spec-publish-time-image-optimization/client-image-contract.md#L5)

- Inspect retained identities before interpreting conditional geometry.
  [p0a-slot-source-inventory.json:641](p0a-slot-source-inventory.json#L641)

**Qualification boundaries**

- Preserve capabilities while making open qualification decisions explicit.
  [SPEC.md:31](../../specs/spec-publish-time-image-optimization/SPEC.md#L31)

- Separate current behavior from attributed research and unavailable source classes.
  [brownfield.md:21](../../specs/spec-publish-time-image-optimization/brownfield.md#L21)

- Require representative corpus, equal geometry and native measurement boundaries.
  [rendition-evaluation.md:5](../../specs/spec-publish-time-image-optimization/rendition-evaluation.md#L5)

- Keep phased evidence work separate from production rollout.
  [implementation-plan.md:24](../../specs/spec-publish-time-image-optimization/implementation-plan.md#L24)

- Retain the append-only research adoption record.
  [.memlog.md:1](../../specs/spec-publish-time-image-optimization/.memlog.md#L1)

**Verification**

- Reproduce exact fitted demand and preserve source shortage.
  [verify-p0a-inventory.py:40](verify-p0a-inventory.py#L40)

- Require complete purpose-specific evidence mappings.
  [verify-p0a-inventory.py:155](verify-p0a-inventory.py#L155)

- Reject missing or remapped evidence through the real verifier.
  [verify-p0a-inventory.py:379](verify-p0a-inventory.py#L379)

**Separate execution tracking**

- Review six staged stories and their independent entry gates.
  [epics.md:99](../../planning-artifacts/image-optimization/epics.md#L99)

- Preserve execution concerns after bounded inventory completion.
  [implementation-readiness.md:13](../../planning-artifacts/image-optimization/implementation-readiness.md#L13)

- Inspect scoped handoff and inherited cross-story constraints.
  [1-1-measure-current-image-slots-and-source-coverage.md:1](1-1-measure-current-image-slots-and-source-coverage.md#L1)

- Keep epic context isolated from Mobile1.2.
  [epic-1-context.md:1](epic-1-context.md#L1)

- Retain deterministic generator output as planning provenance.
  [sprint-generation.json:1](../../planning-artifacts/image-optimization/sprint-generation.json#L1)

- Leave completed implementation in review; later stories remain gated.
  [sprint-status.yaml:37](sprint-status.yaml#L37)
