# Rendition Evaluation Protocol

Status: evaluation protocol, 2026-10-08. A bounded seven-small-cover JPEG slice has produced actual local outputs and byte results in [experiment findings](p0-small-jpeg-findings.md). The broader corpus, native quality/resource evidence and production profile selection remain pending; the matrix below is not adopted production configuration.

## Corpus and controls

Begin P0a with real listing/detail/reader/listening slots: logical constraints, DPR, fit/alignment, source ratio/orientation, zoom demand and text-safe region on phone/tablet portrait/landscape and windowed layouts. Use the two-axis geometry in `client-image-contract.md`; current bounding boxes are not guaranteed output sizes. Preserve existing crop while assessing visible lettering. Flag insufficient originals instead of certifying upscaling.

Select exact asset IDs/checksums and representative rights-cleared originals before evaluation. Start with the retained small JPEGs as regression controls, then add a high-resolution illustrated cover, title-bearing TR/EN/PT reading covers, fine-line/gradient/dark artwork, ordinary and text-dense pages, transparent PNG, EXIF-rotated JPEG and ICC-tagged color cases. Include truncated/mismatched-MIME and excessive-byte/pixel inputs as rejection fixtures. Assess animated artwork separately with frame/timing/disposal/loop checks; never use a still first frame as success.

Record usage role, source ID/hash, actual MIME/bytes/dimensions, alpha, orientation/color/animation metadata and intended rendered size/DPR. Keep source files and private access URLs outside public evidence. Compare each candidate to the same orientation/color-normalized reference at the same rendered size; preserve embedded text, crop and aspect ratio. Source-language labels alone do not prove baked lettering is localized.

## Candidate matrix

These are proposed experiment candidates, not approval to change existing profiles. Cap every target at source resolution. The sample's actual aspect ratio and rendered physical pixels determine whether a candidate is useful.

| Role | Control | Initial candidates to compare |
| --- | --- | --- |
| Listing | Existing phone 360×480/tablet 600×800 JPEG 85 bounding boxes | Keep dimensions first; compare quality 75/80/85 and conditional WebP at equal rendered quality |
| Detail Hero | Original bytes plus a controlled equivalent rendered reference | Longest-edge960/1440/1920 candidates; JPEG 80/85/90; discard candidates that miss display quality |
| Story page | Original and existing1440-longest-edge JPEG 85 experiment | Longest-edge1080/1440/1920; JPEG 80/85/90, text-focused inspection; conditional lossless/lossy comparison |
| Alpha/static listening art | Original composition | Alpha-preserving versus approved background composition; no general white-matte policy inferred |
| Animation | Original full animation | Separate profile/converter evaluation after animation scope is confirmed |

A longest-edge target is different from a width target or width/height bounding box. P0b compares every codec from identical orientation/color-normalized pixels and geometry, then finds acceptable visible quality rather than matching numeric q. Keep Java JPEG as the baseline; compare actual WebP lossy/lossless and PNG reference/possible output where relevant. WebP q100 alone is not lossless; hidden transparent RGB needs an explicit preservation policy. Select/pin a provider/bridge or `cwebp` only after actual packaged runtime round-trip and client MIME/decode evidence. A writer registry, successful build or extension is insufficient. AVIF is optional later work with qualified fallback and exact engine/OS/device proof at Android API 24 and iOS/iPadOS 15; it is not required for first JPEG sizing improvements.

Normalize orientation/mirroring before discarding metadata; qualify color conversion, alpha/background and animation independently. Resize retained source pixels once per candidate, avoiding derivative generations. Metadata removal/container support alone proves neither incorrect colors nor faithful display. The prior sample-specific P3 diagnostic remains separate from general normalization acceptance.

OQ-3 evaluates verified reuse/copy/reference of already compliant encoded bytes as an explicitly admitted immutable role/profile rendition. It does not require re-encoding merely to show processing occurred, and does not approve reuse or a raw fallback. Record role/profile/format/geometry/color/quality checks and the accepted byte-versus-quality rule before implementing that policy.

## Record per source/output pair

- Source/output SHA-256, rendition/profile/encoder identity, actual byte counts, dimensions and MIME; source checksum remains unchanged.
- For each selected rendition, `saved_bytes = baseline_bytes - candidate_bytes`, `saved_percent = 100 × saved_bytes / baseline_bytes`; record regressions and the precise positive denominator. Compare the same source, role, device and render conditions. A source feeding multiple variants does not count as multiple baseline downloads.
- Report transfer totals for an identical user journey separately from storage totals. Select the actual phone/tablet rendition used by that journey; count actual request payloads, retries and prefetch, distinguishing cache hits from downloads. Storage accounting includes retained originals, all variants and retained revisions and may increase despite lower transfer. Never call that object total a network saving or combine device variants with unstated weights.
- Encoding time, peak worker memory, temporary disk and concurrency; source/full-decode allocations count toward the budget.
- Actual mobile downloaded bytes, first target-image paint, decoded dimensions and peak/settled memory, for cold/warm physical-device journeys. Record physical/logical slot, DPR, OS, exact build/engine/renderer/tool versions, network/thermal/background state and instrumentation overhead.
- Original versus candidate visual comparison at normal/enlarged UI text where relevant, supported reader orientations and high-DPR/tablet sizes. Check titles, small glyphs/accents, line art, banding, halos, alpha edges and color/orientation.
- Human quality verdict and rejection reason. PSNR/SSIM may assist diagnosis, but cannot replace readable text and correct artwork on devices.

## Reproducible measurement boundaries

The following is a proposed P0 measurement protocol, not an accepted performance budget. Freeze it with the corpus before running the experiment; record deviations rather than comparing unlike runs.

| Measurement | Boundary and method |
| --- | --- |
| Worker stages | Measure source read/download separately; transform starts with complete source bytes and ends after encoded output closes; encoding-only covers writer start through output close. Record upload and stored-object reread/verification separately. End-to-end job time starts at claimed attempt and ends at committed ready revision. |
| Client latency | Start at explicit current-image request dispatch; associate the decoded revision with its first rasterized onstage frame. Metadata, transfer, encoded-ready, decoded-ready and paint milestones remain separate. If frame association is unavailable, report image-ready latency and mark first-painted unavailable; a Future, placeholder or offstage image is not the endpoint. Raster completion is distinct from physical display presentation. |
| Worker memory | Record idle baseline and absolute/delta peak Java heap, process RSS and container/cgroup usage where available. Include source decode, encoded buffers and native allocations. Attribute concurrency and other job activity; output pixel size is not a peak-memory measurement. |
| Client memory | Record Dart heap/external, raster cache and RSS plus Android PSS/private-dirty/native/available graphics rows or iOS footprint. Do not add overlapping counters; label unavailable values. Width×height×4 estimates one assumed 32-bit surface, not transient/process memory. Keep jetsam reason/page-size evidence for actual termination; restart alone does not prove image causation. |
| Sampling and settlement | Proposed 1 Hz process/container sampling plus event/allocation traces. Record actual tool/sample intervals, overhead and missed-peak limits; rolling DevTools documentation does not certify the shipped instrumentation. Report absolute/delta peak and settled memory after 30 seconds without active work, plus baseline drift. End snapshots or polling can miss short peaks. |
| Cache modes | Network-cold uses an isolated experiment namespace/profile without disk/decoded hits; preserve accepted caches. Disk-warm keeps verified files with decoded-cold state; memory-warm retains matching decoded state. Verify each layer, live/pending owners, requests and identity separately. Clearing LRU counters with listeners retained does not establish decoded-cold state. |
| Repeats | Proposed minimum 5 cold and 5 warm observations per selected case and candidate; distinguish disk/memory warm variants. Randomize/interleave control/candidate order and record network/environment. Publish raw observations, median and min/max; this small sample does not establish P95 or production reliability. |

After pilot selection, a larger exploratory set such as 30 observations per selected cell may report descriptive quantiles with all raw failures. This is a proposal, not a reliable production P95, rare-crash rate or vendor safety threshold.

## P0c — Physical-device and resource qualification

Include physical low-RAM Android phones/tablets, oldest supported iPhone/iPad/OS combinations runnable by the candidate toolchain and modern controls. Record unavailable floor hardware explicitly. Use profile performance measurement and release/debugger-free lifecycle validation through a separately qualified entry preserving existing admission. The accepted Library development entry refuses release; resolve OQ-6 rather than removing that guard. Existing debug/native functional evidence does not satisfy this gate.

Compare the same scripted list scroll/reversal, detail/back overlap, future reader turns, rotation/window resize, signed-URL renewal, corrupt/oversized inputs, offline misses, cache churn, account switch and background/resume. Include current-only/current-plus-next and one/two-decode experimental arms without assuming safe budgets. Synthetic pressure is separate from genuine OS pressure; record failures, quality rejection, source shortage and resource-limit violations rather than excluding them from summaries. Finite passing cases reduce risk without guaranteeing every old phone crash-free.

For workers, compare full-decode controls with read-time subsampling followed by qualified final resampling. Subsampling decimates pixels and need not match final filtered-resize quality. Measure source/full-raster, encoded/native buffers, heap, RSS/container, temp disk and duration, jointly varying worker concurrency/encoder threading. A source byte cap or small output does not bound peak raster allocation. These arms establish no improvement before measurements.

Use the same supported screen orientation, DPR and network conditions across each baseline/candidate pair. Record process/device state and background work. Device quality approval is separate from any experimental latency or memory result.

## Selection gates

1. Freeze the corpus and evaluation conditions before comparing candidates; do not cherry-pick favorable images.
2. Choose the smallest candidate that passes agreed visual/legibility and resource criteria for its role. Confirm numerical transfer/latency/memory/worker budgets before implementation qualification; no thresholds have been accepted in this draft.
3. A low-resolution source cannot qualify high-DPR quality through upscaling. Flag insufficient originals rather than inventing detail.
4. A candidate can be larger than an already-small JPEG. Record the outcome; OQ-3 must resolve reuse/admission and equal-size/growth policy before publication success. Never repeatedly degrade quality just to hit a percentage.
5. A role that cannot pass quality or format preservation remains unready with an actionable cause. Do not substitute a raw original or flatten animation to claim success.
6. Confirm actual format decode on supported clients and encoder round-trip in the deployment image; freeze providers/binary versions. Emulator observations and fixture sizes do not certify constrained-device or production performance.

## Research evidence boundary

The adopted [research](../../planning-artifacts/research/technical-mobile-image-optimization-2026-10-08/research.md) supplies primary citations for geometry, cache/codec behavior, native measurement and protected HTTP delivery. Four claims have independent corroboration/recomputation; eleven retain single-primary-source limits. Mutable Flutter/main, package tags and Oracle SE21 APIs are guidance, not proof of the compiled Flutter engine or packaged Java25 providers. Revalidate exact dependencies at implementation or earlier after changes; the operational review target is 2026-11-08. No benchmark, blur, OOM, leak, saving or new native acceptance was measured by the research.

## Primary references checked 2026-10-08

- [Oracle JPEGImageWriteParam](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/plugins/jpeg/JPEGImageWriteParam.html): explicit JPEG compression settings support the existing baseline. Quality is an encoder control, not a product quality guarantee.
- [Google cwebp](https://developers.google.com/speed/webp/docs/cwebp): still-image quality, resize and alpha controls support an optional experiment; animated PNG/WebP inputs are not supported by this command.
- [Flutter Image](https://api.flutter.dev/flutter/widgets/Image-class.html): decoded image memory and custom decode sizing explain why transfer bytes and decoded memory require separate measurements.
