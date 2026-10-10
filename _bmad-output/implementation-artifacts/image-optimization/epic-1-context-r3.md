# Epic 1 Context: Qualify Readable Artwork with Measured Delivery and Memory Costs

Revision: `p0a-story-1-2-2026-10-10-r3`, approved source-resolution preparation on 2026-10-10 with "Uygun".
The approved [r3 geometry decision](p0a-reader-geometry-decision.md) separates offline codec feasibility from reader/DPR qualification. The [r2 context](epic-1-context-r2.md) remains retained unchanged.
The retained [full-scope baseline](epic-1-context.md) remains byte-identical to Story 1.1 input SHA-256 `ce9afdcd11d34d57be1bf9d75bddfab67827ec33f8fe9b5784cff10c1e1af88f`. This context applies the approved offline source-resolution handoff while preserving the broader future qualification requirements.

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Connect actual image display demand to retained sources, verified candidates, visual quality, transfer and resource costs. This dedicated P0 epic produces evidence and a decision handoff independently of Mobile Story 1.2 and future production implementation. A bounded inventory may complete with explicit gaps; it does not certify corpus, native runtime, production profiles or full P0.

## Stories

- Story 1.1: Measure Current Image Slots and Source Coverage
- Story 1.2: Freeze a Representative Image Corpus and Trial Protocol
- Story 1.3: Prove Real Encoder Outputs on a Bounded Pilot
- Story 1.4: Compare Frozen Candidates and Worker Resource Costs
- Story 1.5: Prepare Native Image Measurement and Admission
- Story 1.6: Measure Native Image Quality and Resource Costs

## Requirements & Constraints

- Inventory listing, recommendation, detail, reader and listening-art roles without implementing missing surfaces. Distinguish accepted behavior, reader PoC, future roles, calculations and device observations. Record logical slots, DPR, fit/alignment, normalized source ratio/orientation, text-safe framing, observed source/rendition identity and dimensions, and unavailable cells.
- Calculate two-axis physical demand from actual slots and contain/cover scale. Separate unclamped required dimensions from source-capped decode targets; expose source/profile/resource shortages. Upscaling, changed framing or reduced DPR cannot qualify missing detail. Recompute for rotation, window and layout changes.
- Freeze rights-cleared representative masters, real title/text/diacritic pages, fine lines, gradients, dark artwork and ICC cases before the initial comparisons. Gürhan approved an initial static opaque pilot with no EXIF orientation tag on 2026-10-10. Keep real alpha/EXIF rotation/mirroring missing behind separate future qualification gates; all synthetic normalization checks remain mandatory, and current results cannot qualify those excluded classes. Full-scope work still requires those real cases before their comparisons. Retain the six historical small-JPEG controls and separate P3 diagnostic, including measured growth. They do not represent high-resolution/page sources or qualify other roles. Keep exact IDs, hashes, actual MIME, bytes, dimensions and provenance; isolate synthetic malformed/MIME/byte/pixel rejection fixtures from valid quality specimens.
- Compare JPEG controls, actual packaged WebP lossy/lossless and relevant PNG from identical normalized pixels and geometry. Independently reread and decode outputs; capture provider/binary/runtime/settings identities and every rejection or regression. Equal numeric quality is not equal visual quality, WebP q100 is not proof of losslessness, and resizing/lossy encoding cannot promise pixel preservation. AVIF remains optional and needs exact supported-client/fallback proof before adoption.
- Freeze conditions and measurement boundaries before execution. Separate source-byte and same-role comparisons, all-variant storage and journey transfer, image-ready and first onstage target-frame latency, LRU and live/pending ownership, and pixel estimates and process/native memory. Retain raw failures, sampling limits, overhead, baseline drift and unavailable counters; never add overlapping counters. Pilots, emulators, screenshots and cache caps cannot establish production P95, crash prevention or constrained-device safety.
- Preserve originals, historical evidence, accepted UI/cache behavior and production settings. Experiments use isolated files/caches and refuse source/live-object overwrite. Private source locations, credentials, tokens and signed URLs stay outside tracked artifacts. No deployment, production inventory/write, backfill, cleanup, schema or production policy change is authorized here. Future rollout remains new publications first, followed by a separately controlled historical transition.

## Technical Decisions

- `asset` owns transformation, storage and durable jobs; `content` owns publication scope. Use public module APIs, events or IDs; introduce no module edge or publication gate. Editorial status, processing completion and verified readiness remain distinct; plans, extensions and completion flags are not byte verification.
- Preserve localized reading-cover/page ownership, separate STORY source and shared listening artwork, and independent LULLABY static listing versus playback/detail roles. Alpha/background, color, orientation, animation and non-STORY scope require explicit qualification; a still first frame cannot certify animation.
- Preserve immutable source/profile/revision and account/access identity, integrity, cancellation, attempt ownership and composition-owned teardown. Flutter work follows feature-first Clean Architecture, BLoC/Cubit, `auto_route`, and `get_it`/`injectable`; route disposal must not terminate the application media session. Current profiles and cache bounds remain controls pending evidence.
- Android API 24 and iOS/iPadOS 15 remain supported floors. Real authenticated QA uses Firebase UID `jLRypTmoBrhhX21HsDJojurcKTC3` without changing existing allowlists, claims or entitlement. Native performance requires a separately accepted, implemented and verified profile/release-equivalent experiment entry that preserves admission and the controlled Library entry's release refusal. Preparation does not authorize that entry.
- Keep artifacts in dedicated `image-optimization/` directories. Explicitly target this epic and tracker; overlapping story numbers do not namespace shared/mobile trackers. Preserve shared BMad configuration and backend/mobile contexts/statuses.

## UX & Interaction Patterns

Preserve inherited crop, aspect ratio and accepted appearance; no redesign is included. Human quality assessment checks lettering, diacritics, fine lines, gradients, color, alpha edges and framing at supported layouts, high DPR and relevant enlarged text. Placeholder or precache completion cannot count as successful target-image paint. Unimplemented reader/listening roles remain unqualified.

## Cross-Story Dependencies

Only Story 1.1 is immediately executable as a read-only code/evidence inventory. Story 1.2 r3 requires its baseline, available representative originals and independently checked source-resolution references for the approved offline purpose. Reader/DPR demand remains an open qualification gate and cannot be inferred from full-source pixels. Story 1.3 requires the frozen pilot and concrete encoder runtime scope; Story 1.4 additionally needs a bounded worker measurement environment. Story 1.5 may prepare device/tool and entry readiness after 1.1 independently of codec work. Story 1.6 requires 1.2–1.5, the separately authorized verified native entry, admitted candidates, physical devices and a frozen protocol. Unavailable floor hardware remains a gap. Final per-role profiles/budgets, compliant-source reuse and growth acceptance, normalization/role scope, ready/live atomicity, retained-session access and protected caching remain explicit decisions before dependent production work.

## Approved r3 Handoff Boundary

The two retained static opaque, EXIF-orientation-tag-free reading pages use unchanged reference axes 1712×2778 and 1284×2279. CMS ownership remains `reading_page`; experimental purpose is `source_resolution_codec_feasibility`. Admit these references only for that purpose, preregister 14 codec pairs and bind the exact r3 corpus/protocol/reference identities through a detached freeze. Actual provider/binary qualification and candidate outputs remain Story 1.3, with no encoding in this story. Reader logical slot/DPR/fit/unclamped demand/source adequacy/native viewing quality stay unavailable behind an explicit open gate. Current results cannot qualify later reader, sizing or native batches. Preserve the future five-observation/native contract with zero native environments/observations in this offline handoff. Source, reference, fixture, normalization, privacy and alpha/EXIF safeguards remain unchanged.
