---
title: 'technical research: Mobile image optimization'
type: 'technical'
topic: 'Mobile image optimization'
decision: 'Choose role-aware renditions and bounded Flutter decoding for quality, transfer efficiency and low-memory resilience'
source: 'Native primary-source research plus separately attributed local context'
status: complete
preset: 'deep (3 concurrent researchers; platform cap)'
validation: 'normal'
created: '2026-10-08'
updated: '2026-10-08'
verified_claims: 4
unverified_claims: 11
verification_basis: independent publishers or arithmetic; authoritative single-publisher claims remain separately flagged
---

# technical research: Mobile image optimization

**Decision this research serves:** Choose role-aware renditions and bounded Flutter decoding for quality, transfer efficiency and low-memory resilience

## Executive decision

Use **verified renditions per image role, selected from actual fitted physical-pixel demand, with independently bounded decode and image residency**. Retain JPEG as the working baseline; compare real WebP lossy/lossless outputs before adopting it. AVIF should remain optional capability work. Optimize the complete source → publication → transfer → decode → visible-frame path rather than choosing one compression setting for every image.

The decisive distinction is between encoded bytes and decoded pixels. A smaller HTTP object reduces transfer; smaller decoded dimensions reduce the final pixel surface. Neither alone establishes peak process memory or prevents OS termination. Flutter documents uncompressed image storage, while Apple documents substantial image expansion and memory-related process termination. [1][23][25]

For TellPal, the immediate network opportunity is detail artwork: Story 1.2 still delivers original bytes despite bounded client decoding. Listing already has real verified JPEG derivatives. Existing source inspection also shows that decode selection ignores the actual image slot and device pixel ratio (DPR), while `BoxFit.cover` can require more pixels than a width-only rule predicts. These are **local implementation observations**, detailed in [local context](local-context.md), not measurements of blur, out-of-memory failures or savings.

Quality should mean **no unacceptable visible degradation, lost lettering, incorrect color, alpha or framing at supported display conditions**. Lossy encoding and resizing cannot promise mathematical pixel preservation; exact fidelity requires a qualified lossless path. Representative physical-device acceptance remains necessary. [11][12][15]

This research selects an approach and experiments, not production dimensions, quality values, RAM budgets or deployment changes. The existing small-JPEG trial does not represent large masters/pages. The largest remaining uncertainty is the quality/resource frontier on actual constrained phones and tablets. Start with slot/framing qualification and same-geometry JPEG/WebP experiments; the [task-refinement companion](task-refinement.md) defines the next slices.

## 1. Evidence boundary and existing implementation

Research was approved on 2026-10-08 and used parallel primary-source retrieval. Rolling Flutter docs/upstream are API guidance, not proof of every behavior in the shipped engine; local package-configuration metadata records Flutter SDK3.41.1 but does not establish compiled engine identity; Oracle SE21 API references likewise do not certify the project’s Java25 deployment providers. General technical claims trace to the source appendix and [digests](digests/). Repository inventories and the previous experiment are separately attributed in [local context](local-context.md). No converter/device benchmark was rerun, and no application, storage, cache or existing policy changed.

| Existing path | Source-inspection finding | Consequence |
| --- | --- | --- |
| Library/recommendation | Real verified JPEG variants, phone bounding box 360×480 and tablet 600×800, q85 | Preserve the working pipeline; bounding boxes are not exact output sizes for every source ratio. |
| Detail Hero | Signs verified original JPEG/PNG; client decode caps width 1080/1600 and height 2048 | Smaller decode does not reduce original transfer bytes. |
| Story pages | A real immutable JPEG experiment exists; ordinary legacy pages still resolve originals | Qualify/version the future reader consumer separately. |
| Legacy delivery | WebP paths are generated plans | Registry entries are not evidence of actual WebP encoding. |
| Controlled Library entry | 48-entry/32 MiB decoded LRU; separate encoded disk/transfer limits | Retain current bounds as controls; they are not total-memory limits. |
| Older reader PoC | Unbounded `Image.network`/precache path, separate from accepted feature app | Do not attribute PoC behavior to Story 1.2 or treat it as an accepted production reader. |

Exact file/line evidence is in [mobile inventory](imports/mobile-inventory.md) and [backend inventory](imports/backend-inventory.md). The prior six-small-JPEG controls total 269,651 bytes; source-resolution q85 totals 383,718 bytes, **42.30% growth**. This is imported corpus evidence, not a new benchmark or production-distribution estimate. Its [original findings](../../../specs/spec-publish-time-image-optimization/p0-small-jpeg-findings.md) explain why forced re-encoding is not a safe optimization rule.

## 2. Size images for their actual role and fitted geometry

### Physical pixels and aspect ratio

Flutter layout uses logical pixels; DPR gives physical pixels per logical pixel and can be fractional. Derive a physical destination from the **actual image slot**, not the entire device screen or a phone/tablet label. [2]

For positive, orientation-normalized intrinsic dimensions `Iw,Ih`, logical slot `Lw,Lh`, and DPR `d`, the following full-image decode targets are derived from Flutter's fit algorithm. These are geometric calculations, not device-quality guarantees. [3]

```text
Pw = ceil(Lw × d); Ph = ceil(Lh × d)
s_contain = min(Pw/Iw, Ph/Ih)
s_cover   = max(Pw/Iw, Ph/Ih)
s = min(1, selected_scale)                 // never invent source detail
Dw = min(Iw, ceil(Iw × s))
Dh = min(Ih, ceil(Ih × s))
```

`contain` keeps the entire source visible. `cover` fills the destination with a cropped source region; a full decoded source can exceed the destination on one axis. Preserve current framing until a different crop/contain rule is explicitly qualified. A generic `ResizeImagePolicy.fit` target is containment, and cannot be substituted for the coverage calculation. Supplying two unrelated dimensions with the default `exact` policy can distort aspect ratio. [3][4]

Illustrations use raw fractional physical layout extents; decode arguments are rounded upward. They are calculated from current layout, with **assumed** DPR values—not captured devices—and reproduced in [geometry-examples.json](geometry-examples.json):

| Case | Card physical box | Hero physical box | Full-source cover decode for hypothetical 8000×5000 artwork |
| --- | --- | --- | --- |
| 411×914 logical phone, DPR 3 | ≈509×679 | 1233×1370 | ≈2192×1370 |
| 800×1280 logical tablet, DPR 2 | ≈466×621 | ≈1600×1455 | ≈2328×1455 |

The current Hero's width caps could undersample that hypothetical wide source after coverage fitting. This is a geometry-derived risk, not observed blur or a reason to raise limits without memory measurements. Similarly, fitting a tall 512×909 source inside 360×480 produces about 270×480; the name “360 variant” does not establish 360 delivered pixels of width.

Existing Hero cropping exposes approximately 62.6% of a 512×909 source's height on the phone aspect and 51.2% on the tablet aspect. That can be inherited intended presentation. It nevertheless makes title-safe framing part of quality acceptance; a sharp image can still hide lettering.

### Proposed selection contract

1. Determine role, real slot constraints, DPR, fit/alignment, source orientation/ratio and any supported zoom demand.
2. Compute fitted physical demand. Compare both axes with manifest-observed rendition dimensions.
3. Select the smallest **quality-qualified** available variant meeting that demand. Keep a small measured size ladder rather than generating a variant for every resize.
4. Decode to the required aspect-preserving size within the selected rendition and measured per-image/resource limits. Reuse the same final provider/target for precache and display.
5. If source/profile/resource limits prevent sufficient pixels, record the shortage. Upscaling or silently lowering DPR cannot certify quality.

This is a design proposal derived from geometry and variant-key behavior. Resize parameters participate in memory identity, so arbitrary targets can multiply decoded entries. [3][4][32] Layout changes, rotation and split-window use must recompute demand; hysteresis/bucketing should avoid unnecessary reloads.

| Role | Evaluation priority | Starting controls/candidates |
| --- | --- | --- |
| Listing/recommendation | Readable title and stable framing during scroll | Existing 360×480/600×800 bounds; measure actual fitted coverage before changing size. |
| Detail Hero | Framing, visible lettering, gradients; real width and crop | Existing original/decode behavior; compare the draft's 960/1440/1920 longest-edge candidates against actual demand. |
| Reader page | Full content and smallest embedded text across orientations | Existing experimental 1440 longest-edge; draft 1080/1440/1920 candidates, plus larger demand only if justified. |
| Static alpha/listening art | Transparency and composition | Separate alpha-preserving versus explicitly approved compositing candidates. |
| Animation | Frames, timing, blending, disposal and loop semantics | Separate pipeline; a first-frame still is not equivalent success. |

These candidates come from the existing [Publish-Time Image Optimization SPEC](../../../specs/spec-publish-time-image-optimization/SPEC.md) and its evaluation protocol. These numbers remain experiment controls/candidates. A longest edge, width target and bounding box are different profiles; none is adopted here.

## 3. Codec and publication pipeline

### Codec choice

Flutter explicitly documents JPEG, PNG and WebP decoding. Additional formats rely on conditional platform fallback. Android's AVIF platform floor does not cover API 24, and Apple's web-format presentation does not prove native Flutter support on iOS 15. Therefore AVIF is unsuitable as the sole common output for this target; optional use needs exact engine/OS/device proof and a guaranteed qualified fallback. The sole-format restriction rests on Flutter/Android documentation from one publisher; independent native-iOS corroboration is unavailable. Apple’s web evidence cannot fill that gap. [10][14][15]

| Candidate | Useful trial | Limitation to evaluate |
| --- | --- | --- |
| JPEG | Existing opaque illustration/photo baseline | Lossy; no alpha; text edges and color must pass inspection. |
| WebP lossy | Opaque detailed illustrations and covers | Compare equal visible quality, not equal numeric q. |
| WebP lossless | Text/flat-art/alpha-sensitive images | Size and encode/decode cost are corpus-dependent; hidden transparent RGB needs an explicit policy. |
| PNG | Lossless/alpha reference and possible winning output | Do not assume it always loses on small/simple artwork. |
| AVIF | Optional later comparison | Portability and runtime burden remain open for the target floor. |

JPEG and PNG's broad content tradeoffs are documented by Apple; WebP tooling distinguishes lossless, near-lossless and lossy controls. `q=100` does not itself select lossless WebP, and lossless q controls compression effort rather than pixel distortion. [11][15]

Google's historical WebP study matches SSIM by varying each encoder's q; it does not establish universal quality equivalence or TellPal savings. Compare every codec from the same normalized pixels and geometry. Whole-image metrics supplement human inspection of glyphs/diacritics, line art, color, banding and halos at actual display scale. [12]

### Normalize once and avoid unnecessary generations

Recommended transform sequence: retain original → validate input → account for EXIF orientation/mirroring → apply explicit color/alpha policy → resize once → encode candidates → validate and select → upload → independently reread/verify → publish ready revision.

WebP can carry ICC/EXIF/XMP, alpha and animation; `cwebp` metadata defaults to none and its still-image tool does not preserve animated inputs. Container support alone proves neither encoder behavior nor client color handling. Normalize orientation before discarding metadata, avoid stale orientation after rotation, and qualify any sRGB conversion or alpha matte. [11][13]

For already-small/compliant sources, recommend resolving SPEC OQ-3 with **verified rendition reuse**, potentially preserving identical encoded bytes. The source must pass role/profile/format/dimension/color/quality requirements and enter the verified immutable delivery contract. This is not a silent original fallback. A new lossy generation solely to satisfy “processing happened” is unsupported by the local size regressions. Selection must account for quality as well as bytes.

### Runtime and worker constraints

Keep Java orchestration. First test the existing ImageIO providers; if measured WebP benefits justify it, compare a maintained provider/bridge with a pinned `cwebp` runtime. ImageIO exposes registered formats and can fail to find a writer; a successful build or `.webp` extension proves no encoder. Verify actual packaged runtime, binary/provider version and a byte-level round trip. [16]

Current converters fully decode before resizing. Source byte/output limits do not bound that raster allocation. ImageReadParam supports read-time subsampling; the operation's decimation semantics do not guarantee the same quality as a final filtered resize. Evaluate an appropriate intermediate decode followed by final resampling, retaining full-decode controls. Measure JVM heap, process/container RSS, temp storage and encoder/native allocations; choose job concurrency jointly with encoder threading. These are proposals, not measured worker improvements. [17][18][11]

Preserve existing local architectural constraints: `asset` transforms/verifies, `content` determines required publication scope, and immutable source/profile/attempt identity fences readiness. Reuse listing's atomic publication pattern; harden page attempts before treating experiment completion as production readiness. Failed candidates retain the last eligible live/session revision. Queries should consume prepared outputs rather than transform on GET. These contextual recommendations are grounded in the separately cited inventories and existing draft, not external proof of a reproduced race.

HTTP delivery configuration should be explicit but need not introduce a CDN now. Cloud Storage's built-in cache has public-access prerequisites; signed URLs alone do not establish that behavior. Cloud Storage derives response Cache-Control from object metadata, and Content-Type/ETag cannot replace byte validation. `private`, `no-cache` and `no-store` have distinct meanings under HTTP; preserve admitted local caching without copying public long-lived policies into protected media. This recommendation is supported by Google Cloud and the independent IETF standard. [28][29][30]

## 4. Bound decoded residency and work, separately from bytes

### What memory calculations can establish

For an explicitly assumed 32-bit SDR surface, `width × height × 4` estimates one decoded pixel plane. Wider formats, encoded buffers, intermediates, live images, renderer resources and other app allocations require separate accounting. Smaller final decoding does not guarantee proportional transient-peak savings: Flutter's codec docs describe scaling after decoding. [1][10][19]

| Mathematical surface example | One 32-bit pixel plane |
| --- | ---: |
| 4096×3072 | 48 MiB |
| 1024×768 | 3 MiB |
| 1440×1920 | ≈10.55 MiB |
| 2048×1536 | 12 MiB |

The first two have a 16× pixel-payload difference. This is not a measured 16× process-memory saving; JPEG/WebP compression at identical decoded geometry does not itself alter this calculation.

Flutter's LRU tracks retained decoded entries, while live/pending owners and process allocations have different lifetimes. `clear()` does not release images still held by application listeners. The existing 32 MiB cache therefore cannot certify a 32 MiB image footprint. Avoid eviction churn that decodes the same still-live image again; cache bookkeeping is not transport cancellation. [5][6]

### Proposed scheduling policy

Prioritize visible artwork. Bound concurrent downloads, decodes and estimated decoded bytes in flight; a count alone allows several huge images. On constrained-device experiments, compare one and two active decodes. Compare current-page-only with current-plus-one-next decoded reader windows; optionally retain adjacent encoded files without decoding them. These are experimental arms, not safe universal defaults. [1][7][24]

Stop admitting speculative work during pressure or navigation changes, discard obsolete queues, and release offscreen listeners/owners. Preserve TellPal's existing cancellation, checksum, promotion and revision guards. Memory and disk resizing are distinct in the exact v3.4.1 widget. [31] The standard image widget alone does not establish HTTP cancellation on disposal; verify the exact custom transport path. Precache the final bounded provider: `precacheImage` can finish after failure without a Future error, may not retain the result, and pinning many images can cause OS termination. [7][8][9]

Keep encoded disk identity separate from transfer attempts and decoded target identity. Include immutable rendition/revision/profile, format/geometry where needed, language and account/access context; an expiring signed URL should not define persistent content identity. Provider equality can use a custom key rather than URL, so one key must never denote different bytes/access scopes. Client disk resizing still requires the original online transfer on its first miss; use a smaller server rendition for first-transfer savings. Exact upstream v3.4.1 tags were checked for provider/widget/cache-manager behavior; the pub.dev archive was not compared byte-for-byte. [8][9][31] In that manager, disk resizing first resolves an unbounded FileImage, can decode again at a target, and writes PNG; unsupported extensions return the original. This is not a guaranteed low-peak-memory route. [9]

### Low-memory signals are cleanup opportunities

Android `isLowRamDevice()` and memory class are initial adaptation hints, not live available-memory or native/GPU budgets. Current Android docs deprecate old foreground-pressure trim levels in API 35 and state they stopped arriving from API 34; background/lifecycle cleanup remains useful. Do not build prevention around receiving those older warnings. [20][21]

Flutter clears its image cache on a received memory-pressure notification. Apple says warning delivery is best effort and rapid allocation may cause jetsam first. Prevention therefore depends on proactive resource/residency controls; error widgets and warning handlers cannot recover an already-killed process. [27][24][23]

## 5. Physical-device validation and acceptance

Use physical constrained Android phones/tablets and the oldest supported iPhone/iPad/OS combinations that the candidate toolchain can actually run, plus modern controls. Record unavailable floor hardware explicitly. Flutter recommends profile-mode performance investigation; Apple also requires release testing without debugger interference for representative background behavior. Existing debug/native functional acceptance is not a substitute. [26][25]

| Measurement | Required distinction |
| --- | --- |
| Transfer | Actual response bytes, retries/revalidation and prefetch for the same journey; separate from stored originals/all variants/revisions. |
| Decode | Actual output dimensions and identity; estimate pixel payload separately from measured process memory. |
| Latency | Request,encoded-ready,decoded-ready and first onstage target-frame milestones. A preload Future or placeholder is not first visible artwork. |
| Memory | Dart heap/external,raster cache,RSS; Android PSS/private dirty/native/available graphics rows; iOS footprint and diagnostics. Never add overlapping counters. |
| Lifecycle | Live/pending owners,queue/concurrency,cancel/discard,return-to-idle drift,background/resume. |

DevTools distinguishes heap, native/external, raster cache and RSS; its samples can miss short peaks. Android `dumpsys meminfo` is a snapshot with platform-dependent categories. For iOS termination, preserve the jetsam report and inspect the terminated process/reason, using that report's page size rather than inferring image causation from a restart. [19][22][23]

Freeze exact build/engine/renderer/tool versions, source/output hashes, dimensions, DPR, network/thermal/background conditions and instrumentation overhead. Keep isolated test cache namespaces. Measure three independently verified states: network-cold application image, disk-warm/decoded-cold, and decoded-memory-warm. Clearing LRU counters while live listeners remain is not a memory-cold setup. [5][6][19]

Retain the existing protocol's five cold/five warm pilot observations with median/min/max. After choosing promising policies, use a larger exploratory set, such as 30 trials per selected cell, with raw failures and descriptive quantiles; that count does not establish a reliable production P95 or rare-crash rate. Associate target-image paint with the actual rasterized frame; if unavailable, report image-ready latency and mark first-painted latency unavailable. Raster completion is also distinct from physical screen presentation. These counts and instrumentation rules are proposed methodology, not vendor safety thresholds.

Stress the same scripted rapid list scroll/reversal,detail/back overlap,reader turns,rotation/window resize,URL renewal,corrupt/oversized inputs,offline misses,cache turnover,account switch and background/resume. Exercise a synthetic pressure hook separately from genuine OS pressure: the hook proves handler behavior only. Compare before/after quality, transferred bytes, peak/settled memory, baseline drift, latency and failures. No unexplained termination or quality regression may disappear from the result table.

## 6. Combined findings and contrary considerations

- **Higher visual demand can increase RAM.** Correcting DPR/cover undersampling may require larger decoded images. Pair that change with reduced simultaneous residency and measure the total process; raising a width cap alone is incomplete.
- **A smaller file can leave peak RAM unchanged.** Codec compression and decode/residency must each be evaluated; no universal GPU-copy multiplier is established.
- **A forced re-encode can increase bytes and add loss.** Preserve compliant encoded pixels through an explicit verified-rendition policy; the prior JPEG experiment makes this immediately relevant.
- **A smaller cache can increase churn.** Live owners, failed retention and key fragmentation can force repeated decode; measure the whole journey rather than cache counters.
- **More variants can reduce transfer while increasing storage/operations.** Generate a small measured ladder and reuse immutable source/profile outputs; do not count all stored variants as journey downloads.

These are engineering inferences from the cited dimensions. No formal red-team pass was enabled. Contrary considerations were retained during retrieval: WebP is not a universal size winner, AVIF portability is unproven, existing small originals cannot supply high-DPR detail, and finite device tests cannot guarantee every old phone crash-free.

## 7. Recommended task refinement and open decisions

Use the [task-refinement companion](task-refinement.md) to feed the existing SPEC's P0–P5 phases. Start with slot/DPR/framing qualification, representative equal-geometry JPEG/WebP trials and process-memory measurements. Then resolve compliant-source reuse, per-role profiles and atomic ready-revision semantics before dependent implementation. Preserve the approved new-publications-first rollout and separate historical transition.

| Decision still open | Evidence needed |
| --- | --- |
| Actual image quality/size frontier | Large masters,real pages/text and alpha/orientation/color fixtures; human native acceptance. |
| Variant/decode/preload budgets | Exact engine and physical lowRAM phone/tablet traces,including transients and overlapping navigation. |
| Small-source success policy | Explicit verified reuse/copy semantics,not raw fallback or compulsory lossy encoding. |
| Readiness/retention/access contract | First publish,failed replacement,stale worker,source/profile change and retained-session examples. |
| Actual WebP runtime fit | Pinned encoder/provider and container round-trip/resource evidence; no packaged encoder was tested here. |
| AVIF capability | Exact Flutter/OS/device decode proof and fallback; exact iOS15 support remains unverified. |


## Source appendix

All sources were accessed on **2026-10-08**. “Unknown” means original publication date was unavailable; observed update dates are labeled separately. Confidence **high** below means direct primary-source support for the scoped API/format statement, not an independently replicated performance result. Independent-check status and version limitations are recorded in the verification digest/claim ledger; proposals remain unmeasured.

| Ref | Supports | Publisher / primary source | Publication / observed update | Accessed | Scope confidence |
| --- | --- | --- | --- | --- | --- |
| [1] | Decoded images and pixel memory | [Flutter](https://api.flutter.dev/flutter/widgets/Image-class.html) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [2] | Logical/physical pixel definition | [Flutter](https://api.flutter.dev/flutter/dart-ui/FlutterView/devicePixelRatio.html) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [3] | Fit/crop algorithm | [Flutter](https://api.flutter.dev/flutter/painting/applyBoxFit.html) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [4] | Exact versus aspect-preserving fit | [Flutter](https://api.flutter.dev/flutter/painting/ResizeImagePolicy.html) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [5] | LRU/live/pending ownership | [Flutter upstream](https://github.com/flutter/flutter/blob/main/packages/flutter/lib/src/painting/image_cache.dart) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [6] | Clear and retained live owners | [Flutter](https://api.flutter.dev/flutter/painting/ImageCache/clear.html) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [7] | Precache failure,retention and pinning | [Flutter](https://api.flutter.dev/flutter/widgets/precacheImage.html) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [8] | Exact v3.4.1 provider identity | [Baseflow](https://github.com/Baseflow/flutter_cached_network_image/blob/v3.4.1/cached_network_image/lib/src/image_provider/cached_network_image_provider.dart) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [9] | Exact v3.4.1 disk resize path | [Baseflow](https://github.com/Baseflow/flutter_cache_manager/blob/v3.4.1/flutter_cache_manager/lib/src/cache_managers/image_cache_manager.dart) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [10] | Portable format list and decode targets | [Flutter](https://api.flutter.dev/flutter/dart-ui/instantiateImageCodec.html) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [11] | WebP quality,metadata and runtime controls | [Google WebP](https://developers.google.com/speed/webp/docs/cwebp) | Unknown; update 2025-08-07 | 2026-10-08 | Direct primary support; see limits |
| [12] | Equal-SSIM comparison; historical study limits | [Google WebP](https://developers.google.com/speed/webp/docs/webp_study) | Unknown; update 2025-08-07 | 2026-10-08 | Direct primary support; see limits |
| [13] | Alpha,color and animation container | [Google WebP](https://developers.google.com/speed/webp/docs/riff_container) | Unknown; update 2025-08-07 | 2026-10-08 | Direct primary support; see limits |
| [14] | Android platform codec floors | [Android Developers](https://developer.android.com/media/platform/supported-formats) | Unknown; update 2026-10-07 | 2026-10-08 | Direct primary support; see limits |
| [15] | JPEG/PNG properties; web/native boundary | [Apple WWDC23](https://developer.apple.com/videos/play/wwdc2023/10122/) | 2023 | 2026-10-08 | Direct primary support; see limits |
| [16] | Reader/writer provider availability | [Oracle](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/ImageIO.html) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [17] | Read-time subsampling and optional render size | [Oracle](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/ImageReadParam.html) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [18] | Subsampling decimation semantics | [Oracle](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/IIOParam.html) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [19] | Heap/native/raster/RSS and sampling | [Flutter](https://docs.flutter.dev/tools/devtools/memory) | Unknown; update 2026-07-31 | 2026-10-08 | Direct primary support; see limits |
| [20] | LowRAM classification and memory class | [Android Developers](https://developer.android.com/reference/android/app/ActivityManager) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [21] | API34/35 pressure signal changes | [Android Developers](https://developer.android.com/reference/android/content/ComponentCallbacks2) | Unknown; update 2026-09-24 | 2026-10-08 | Direct primary support; see limits |
| [22] | PSS/native/graphics process snapshots | [Android Developers](https://developer.android.com/tools/dumpsys) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [23] | Memory termination diagnostics | [Apple](https://developer.apple.com/documentation/xcode/identifying-high-memory-use-with-jetsam-event-reports) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [24] | Best-effort memory warnings | [Apple](https://developer.apple.com/documentation/xcode/responding-to-low-memory-warnings) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [25] | Physical release/background validation | [Apple](https://developer.apple.com/documentation/xcode/testing-a-release-build) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [26] | Physical-device profile mode | [Flutter](https://docs.flutter.dev/perf/ui-performance) | Unknown; update 2026-07-31 | 2026-10-08 | Direct primary support; see limits |
| [27] | Flutter cache response to pressure | [Flutter](https://api.flutter.dev/flutter/painting/PaintingBinding/handleMemoryPressure.html) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [28] | Built-in cache prerequisites | [Google Cloud](https://docs.cloud.google.com/storage/docs/caching) | Unknown; update 2026-10-07 | 2026-10-08 | Direct primary support; see limits |
| [29] | Object metadata,ETag and HTTP response policy | [Google Cloud](https://docs.cloud.google.com/storage/docs/metadata) | Unknown; update 2026-10-07 | 2026-10-08 | Direct primary support; see limits |
| [30] | Independent HTTP cache directive semantics | [IETF](https://datatracker.ietf.org/doc/html/rfc9111) | 2022-06 | 2026-10-08 | Direct primary support; see limits |
| [31] | Exact v3.4.1 memory/disk configuration | [Baseflow](https://github.com/Baseflow/flutter_cached_network_image/blob/v3.4.1/cached_network_image/lib/src/cached_image_widget.dart) | Unknown | 2026-10-08 | Direct primary support; see limits |
| [32] | Resize key identity fields | [Flutter upstream](https://github.com/flutter/flutter/blob/main/packages/flutter/lib/src/painting/image_provider.dart) | Unknown | 2026-10-08 | Direct primary support; see limits |

## Staleness and verification

The ledger tracks 15 load-bearing statements: **4 independently corroborated or independently recomputed; 11 single-primary-source statements without independent-publisher verification**. The latter are labeled `unverified` in the machine ledger; they have retrieved authoritative evidence, not fabricated support. Framework-specific internals cannot acquire independent status merely because multiple Google pages agree. No measured performance gain was claimed. See [critical verification](digests/verification.md) and [corrections](digests/corrections.md).

Mechanical staleness outputs are retained in [publication-date checks](publication-staleness.json) and [operational review schedule](operational-review-staleness.json). Original publication dates are unknown for most rolling API documents; no crawl/access/update date was relabeled as publication. The tool’s required `pub_date` input field is used for separately labeled operational anchors in review-anchors.json only, with `date_basis` explicitly identifying that distinction.

| Review set | Basis | Computed next check |
| --- | --- | --- |
| Pinned Flutter/Baseflow,Android compatibility,tooling and actual codec runtime | Verified/retrieved 2026-10-08; one-month operational review,earlier before implementation or after dependency change | **2026-11-08** |
| Cloud Storage integration guidance | Observed update2026-10-07; twelve-month operational review | 2027-10-07 |
| RFC9111 normative semantics | Published2022-06;120-month standard window | 2032-06-01; earlier if superseded |

The earliest scheduled operational recheck is **2026-11-08**. Undated source publication freshness is **unknown**,not automatically fresh; revalidate it at implementation. The WebP historical studies remain historical methodological evidence regardless of their later page-update date. Refresh/Deepen can update these questions and the ledger without treating this report as profile/device acceptance.
