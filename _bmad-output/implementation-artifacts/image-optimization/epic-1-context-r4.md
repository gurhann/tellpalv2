# Epic 1 Context: Qualify Readable Artwork with Measured Delivery and Memory Costs

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Connect actual display demand to retained sources and verified candidates, then assess readability, framing, transfer and client/worker resource costs. This dedicated P0 epic produces bounded qualification evidence and an explicit decision handoff independently of Mobile Story 1.2 and P1–P5 production implementation. The completed offline encoder pilot advances codec feasibility; it does not establish reader, native, resource or full-P0 acceptance.

## Stories

- Story 1.1: Measure Current Image Slots and Source Coverage
- Story 1.2: Freeze a Representative Image Corpus and Trial Protocol
- Story 1.3: Prove Real Encoder Outputs on a Bounded Pilot
- Story 1.4: Compare Frozen Candidates and Worker Resource Costs
- Story 1.5: Prepare Native Image Measurement and Admission
- Story 1.6: Measure Native Image Quality and Resource Costs

## Requirements & Constraints

- Inventory listing, recommendation, detail, reader and listening-art demand. Distinguish accepted, PoC/future, calculated and physical observations. Record slots, DPR, fit/alignment, orientation/ratio, text-safe framing and source/rendition identity. Separate unclamped two-axis physical demand from source-capped decode targets; expose shortages without qualifying upscale or changed framing.
- Preserve originals, history, accepted UI/cache behavior and production settings. Use isolated files/caches, exclusive writes and retained failures; refuse source/live overwrite. Image bytes, private paths/logs, credentials, tokens and signed URLs stay outside Git. No production writes, deployment, backfill, cleanup, schema or policy changes are included. Future rollout remains new publications first, then separately controlled historical transition.
- Retain frozen corpus/normalization, purpose-scoped admission and exact source/reference/settings/provider identities. The pilot admits static opaque, EXIF-orientation-tag-free sources only for offline source-resolution codec feasibility. Reader/DPR demand, source adequacy and native quality remain unavailable. Real alpha, EXIF rotation/mirroring, animation and unsupported roles need separately approved qualification; synthetic checks cannot close them. Preserve six historical small-JPEG controls and separate P3 diagnostic, including growth. Acquisition or runtime/settings/normalization changes require an explicit revision.
- Compare identical normalized pixels/geometry with positive named denominators; original bytes and same-geometry JPEG q85 controls remain distinct. Retain outputs, growth, rejections, retries and unavailable results. Equal numeric quality is not visual equivalence; WebP q100 is not losslessness. Pixel preservation requires exact decoded equality. Pending visual verdicts leave quality-qualified savings unavailable.
- Freeze measurement conditions before authorized batches. Separate read, transform, encoding-only, verification and end-to-end work; mark unexercised upload/ready-commit stages inapplicable. Record idle/peak/settled runtime/native/process/container counters, temporary disk, duration, concurrency, encoder threads, sampling/overhead and unavailable counters. Never infer Java/cloud resources from Python or add overlapping counters. Full-decode controls and optional subsampling arms require comparable quality evidence.
- Separate storage from journey transfer, LRU from live/pending owners, image-ready from first onstage target-frame latency, and pixel estimates from process memory. Retain failures and baseline drift. Pilots, cache caps, emulators and descriptive quantiles do not establish production P95 or device/crash safety. Profiles/budgets, reuse/growth acceptance and normalization/role scope remain decisions.

## Technical Decisions

- `asset` owns transformation/storage/jobs; `content` owns publication scope through public APIs/events/IDs. Add no module edge or publication gate. Preserve localized reading covers/pages, separate STORY source/listening artwork and LULLABY static listing/playback roles. Editorial status, job completion and verified readiness remain distinct.
- Preserve source/profile/revision and account/access identity, integrity, cancellation and attempt ownership. Flutter retains feature-first Clean Architecture, BLoC/Cubit, `auto_route`, `get_it`/`injectable` and composition-owned teardown; route disposal cannot terminate application media. Current profiles/cache bounds remain controls; Android API 24 and iOS/iPadOS 15 remain floors.
- Real QA uses Firebase UID `jLRypTmoBrhhX21HsDJojurcKTC3` with existing admission. Native performance requires a separately accepted, implemented and verified profile/release-equivalent entry preserving Library's release refusal; preparation cannot authorize it.
- Use dedicated `image-optimization/` paths and leave `story_key` unset unless isolated resolution is demonstrated. Numeric IDs cannot namespace shared/mobile trackers. Preserve shared config, shared/mobile contexts/trackers and r1/r2/r3 history; r4 updates none.

## UX & Interaction Patterns

Preserve inherited crop, aspect ratio and appearance. Human qualification checks lettering/diacritics, lines, gradients, color and applicable alpha/orientation at supported layouts, high DPR and enlarged text. Placeholder/precache completion is not target-image paint. Unimplemented reader/listening roles remain unqualified; no redesign is included.

## Cross-Story Dependencies

Story 1.1 supplies the retained baseline; Story 1.2 supplies the approved r3 corpus/protocol and two checked v7 source-resolution references at 1712×2778 and 1284×2279. The completed [Story 1.3 spec](spec-1-3-encoder-pilot.md) and [pilot report](p0b-encoder-pilot.md) bind batch `encoder-pilot-r3-v2`: 14 pairs, 28 independently reread/decoded arms and four bounded fixture rejections passed in local macOS arm64 Python 3.14.6/Pillow 12.3.0. Actual JPEG, WebP lossy/lossless and PNG outputs are qualified only for that installed offline runtime; separate rereading uses the same pinned decoder and provides no decoder-diversity claim. Java/Railway packaging, mobile decoding, human/native quality and worker resources remain unqualified.

Story 1.4 can build on those exact identities but still requires a concrete bounded worker measurement environment, isolated resources and a frozen reviewable batch/measurement scope before execution. Reusing the pilot cannot supply missing worker observations or authorize expanded geometry/roles/runtime. Story 1.5 may prepare observed device/tool availability and the native-entry proposal independently after 1.1. Story 1.6 requires 1.2–1.5, admitted candidates, physical devices, a frozen protocol and resolved/implemented/verified OQ-6; unavailable floor hardware stays explicit. OQ-2/3/4/6 remain open, and ready/live atomicity, retained-session access and protected-cache decisions in OQ-5 remain prerequisites for dependent production work.
