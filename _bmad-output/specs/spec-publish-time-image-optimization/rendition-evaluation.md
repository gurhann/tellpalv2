# Rendition Evaluation Protocol

Status: evaluation protocol, 2026-10-08. A bounded seven-small-cover JPEG slice has produced actual local outputs and byte results in [experiment findings](p0-small-jpeg-findings.md). The broader corpus, native quality/resource evidence and production profile selection remain pending; the matrix below is not adopted production configuration.

## Corpus and controls

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

A longest-edge target is different from a width target or width/height bounding box. Do not compare equal numeric quality values across codecs as equal visual quality. Existing Java JPEG is the baseline; WebP is a conditional comparison requiring an actual packaged encoder, byte validation and backend/mobile MIME support. Select and pin any new encoder/version only after current runtime fit is verified. AVIF or a new processing service is not required by this proposal.

## Record per source/output pair

- Source/output SHA-256, rendition/profile/encoder identity, actual byte counts, dimensions and MIME; source checksum remains unchanged.
- For each selected rendition, `saved_bytes = baseline_bytes - candidate_bytes`, `saved_percent = 100 × saved_bytes / baseline_bytes`; record regressions and the precise positive denominator. Compare the same source, role, device and render conditions. A source feeding multiple variants does not count as multiple baseline downloads.
- Report transfer totals for an identical user journey separately from storage totals. Select the actual phone/tablet rendition used by that journey; count actual request payloads, retries and prefetch, distinguishing cache hits from downloads. Storage accounting includes retained originals, all variants and retained revisions and may increase despite lower transfer. Never call that object total a network saving or combine device variants with unstated weights.
- Encoding time, peak worker memory, temporary disk and concurrency; source/full-decode allocations count toward the budget.
- Actual mobile downloaded bytes, time to first decoded/rendered image, decoded dimensions and peak/settled memory, for cold/warm runs on iOS/Android phone/tablet. Record physical/logical size,DPR,OS,build,network and cache state.
- Original versus candidate visual comparison at normal/enlarged UI text where relevant, supported reader orientations and high-DPR/tablet sizes. Check titles, small glyphs/accents, line art, banding, halos, alpha edges and color/orientation.
- Human quality verdict and rejection reason. PSNR/SSIM may assist diagnosis, but cannot replace readable text and correct artwork on devices.

## Reproducible measurement boundaries

The following is a proposed P0 measurement protocol, not an accepted performance budget. Freeze it with the corpus before running the experiment; record deviations rather than comparing unlike runs.

| Measurement | Boundary and method |
| --- | --- |
| Worker stages | Measure source read/download separately; transform starts with complete source bytes and ends after encoded output closes; encoding-only covers writer start through output close. Record upload and stored-object reread/verification separately. End-to-end job time starts at claimed attempt and ends at committed ready revision. |
| Client latency | Start at explicit current-image request dispatch; end at the first actually painted onstage frame containing that revision's decoded image. Metadata,transfer,decode and paint milestones remain separate. A completed Future,placeholder or offstage decoded image is not the endpoint. |
| Worker memory | Record idle baseline and absolute/delta peak Java heap, process RSS and container/cgroup usage where available. Include source decode,encoded buffers and native allocations. Attribute concurrency and other job activity; output pixel size is not a peak-memory measurement. |
| Client memory | Record Dart heap and platform process/native image memory separately; label unavailable GPU/native counters. Retain decoded width×height×4 as an estimate, not total memory. A Dart-only trace cannot establish image/process memory. |
| Sampling and settlement | Proposed 1 Hz process/container sampling plus available event/allocation traces. Record sampling limits and absolute peak; report settled memory after 30 seconds with no active transfer/decode/job work following the case. End-of-run snapshots can miss short peaks. |
| Cache modes | Cold uses a new isolated experiment namespace/profile with no memory/disk hits; preserve accepted Story1.2 caches. Disk-warm keeps verified files but recreates decoded state; memory-warm keeps decoded state. Record each layer,hits,source requests and source/revision identity separately. |
| Repeats | Proposed minimum 5 cold and 5 warm observations per selected case and candidate; distinguish disk/memory warm variants. Randomize/interleave control/candidate order and record network/environment. Publish raw observations,median and min/max; this small sample does not establish P95 or production reliability. |

Use the same supported screen orientation,DPR and network conditions across each baseline/candidate pair. Record process/device state and background work. Device quality approval is separate from any experimental latency or memory result.

## Selection gates

1. Freeze the corpus and evaluation conditions before comparing candidates; do not cherry-pick favorable images.
2. Choose the smallest candidate that passes agreed visual/legibility and resource criteria for its role. Confirm numerical transfer/latency/memory/worker budgets before implementation qualification; no thresholds have been accepted in this draft.
3. A low-resolution source cannot qualify high-DPR quality through upscaling. Flag insufficient originals rather than inventing detail.
4. A candidate can be larger than an already-small JPEG. Record the outcome; OQ-3 must resolve equal-size/verified-pass-through policy before treating this as publication success. Never repeatedly degrade quality just to hit a percentage.
5. A role that cannot pass quality or format preservation remains unready with an actionable cause. Do not substitute a raw original or flatten animation to claim success.
6. Confirm actual format decode on supported clients and encode support in the deployment image. Emulator observations and fixture sizes do not certify low-memory physical-device or production performance.

## Primary references checked 2026-10-08

- [Oracle JPEGImageWriteParam](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/plugins/jpeg/JPEGImageWriteParam.html): explicit JPEG compression settings support the existing baseline. Quality is an encoder control, not a product quality guarantee.
- [Google cwebp](https://developers.google.com/speed/webp/docs/cwebp): still-image quality,resize and alpha controls support an optional experiment; animated PNG/WebP inputs are not supported by this command.
- [Flutter Image](https://api.flutter.dev/flutter/widgets/Image-class.html): decoded image memory and custom decode sizing explain why transfer bytes and decoded memory require separate measurements.
