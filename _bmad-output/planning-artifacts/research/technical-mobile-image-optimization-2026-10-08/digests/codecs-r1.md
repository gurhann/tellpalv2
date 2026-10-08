# Codec and publication-transform evidence digest

Research date/access date: 2026-10-08. Scope: static illustrated storybook pages with embedded text, color, and possible alpha; native Flutter on Android API 24 and iOS 15; publication transforms orchestrated by a Java server. These platform floors are brief constraints, not externally researched project facts.

No repository code, project memory, architecture, or other project files were read as evidence. Two focused rounds, eight web calls, twelve selected primary sources. Search-result alternatives and a JavaScript-only Apple enum page were not used as evidence. No local corpus or runtime benchmark was performed. Unknown publication dates are explicitly marked; an update date is not a publication date.

## Research rules (verbatim)

1. Never conclude from training data alone. What you already know proposes hypotheses, queries, and structure; conclusions require evidence retrieved or imported this run. A claim you cannot evidence is stated as an unverified belief or not at all.
2. The research firewall. Project context — briefs, PRDs, code, memory, persistent facts — shapes what to ask, never what is true. It is inadmissible as evidence: every claim in a research artifact traces to a digest or import file with a source. Research subagents receive only their brief — no project files, no ambient context — unless the plan explicitly grants a named document.

## Evidence ledger

The metadata in each C block applies to its claim and explicitly stated limitations. Confidence concerns the scoped claim, not universal visual quality or measured performance on the target deployment. Recommendations below are separately labeled engineering inferences/proposals and inherit source metadata through their cited C IDs.

### C01 — Flutter format contract and output geometry

- Source/title: [instantiateImageCodec function](https://api.flutter.dev/flutter/dart-ui/instantiateImageCodec.html)
- Publisher: Flutter; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: primary official API documentation.
- Claim: The explicitly supported set includes JPEG, PNG, WebP, animated WebP, GIF/animated GIF, BMP, and WBMP. AVIF is absent. Flutter may attempt platform APIs for additional formats. Output target dimensions are described as scaling after decoding; one supplied dimension preserves aspect ratio. Upscaling consumes unnecessary memory; `ImageDescriptor` is preferred for more informed sizing decisions.
- Limitations/open leads: The additional-format fallback is conditional, not an AVIF guarantee. Documentation does not prove the decoder path of a particular pinned engine/device or absence of internal decoder optimizations. Verify the shipped Flutter engine and minimum-OS devices with fixtures.

### C02 — Android support is distinct from Flutter support

- Source/title: [Supported media formats](https://developer.android.com/media/platform/supported-formats)
- Publisher: Google / Android Developers; pub_date: unknown; updated: 2026-10-07; accessed: 2026-10-08; confidence: high; class: primary official platform documentation.
- Claim: Android's table lists JPEG/PNG decoding, WebP decoding from Android 4.0, and lossless/transparent WebP decoding from 4.2.1. AVIF baseline 8-bit decoding is listed from Android 12; 10-bit support is optional. The table describes Android platform support.
- Limitations/open leads: API 24 is below Android 12, so the listed AVIF floor does not satisfy this brief's Android minimum. This is not proof that Flutter uses Android's AVIF decoder. Additional earlier device support is possible but unguaranteed. Encoder support is a different table column and must not be confused with decoder support.

### C03 — Content-sensitive JPEG/PNG choice; Apple web evidence boundary

- Source/title: [Explore media formats for the web — WWDC23](https://developer.apple.com/videos/play/wwdc2023/10122/)
- Publisher: Apple; pub_date: 2023 (WWDC23; exact date unknown); accessed: 2026-10-08; confidence: high for stated format properties, limited for native Flutter applicability; class: primary official technical presentation.
- Claim: Apple characterizes JPEG as lossy and suited to photographic detail. PNG is lossless, supports transparency, and suits solid areas, sharp text, logos, and illustrations. The presentation discusses AVIF, including up-to-12-bit color and wide color/HDR advantages, in a Safari/web setting.
- Limitations/open leads: Safari's AVIF statements cannot establish native Flutter decoding on iOS 15. This source does not establish an iOS 15 AVIF native-decoder guarantee. No numerical AVIF saving from this presentation is adopted: its promotional upper bound is not a corpus result applicable here.

### C04 — cwebp controls are mode-specific

- Source/title: [cwebp](https://developers.google.com/speed/webp/docs/cwebp)
- Publisher: Google / WebP team; pub_date: unknown; updated: 2025-08-07; accessed: 2026-10-08; confidence: high; class: primary official tool manual.
- Claim: `-lossless` selects lossless encoding; in lossy mode `-q` trades size/quality, while in lossless mode it controls speed/compression rather than pixel quality. Near-lossless preprocessing changes pixels. `-exact` preserves otherwise-discardable hidden RGB under full transparency. `-metadata` defaults to `none` and can copy ICC/EXIF/XMP. `-sharp_yuv` improves RGB-to-YUV conversion at additional cost. `-mt` enables possible encoding parallelism; `-low_memory` trades memory for speed/output differences. `-version` reports the actual executable version. Animated PNG/WebP inputs are unsupported by this still-image tool.
- Limitations/open leads: `-q 100` in lossy mode is not a lossless-mode declaration. Presets, quality numbers, effort, alpha, and metadata need an explicit tested policy. A resize option alone does not establish bounded peak decode memory. No default q value is recommended as universally safe for embedded text.

### C05 — Equal quality comparisons, not equal q; bounded vendor benchmark

- Source/title: [WebP Compression Study](https://developers.google.com/speed/webp/docs/webp_study)
- Publisher: Google; pub_date: unknown; updated: 2025-08-07; accessed: 2026-10-08; confidence: high for reported method/results, low for transfer to this workload; class: primary vendor benchmark.
- Claim: The experiment encoded the same PNG sources into JPEG, then adjusted WebP q to approximately match JPEG SSIM. It reports 25–34% smaller average WebP sizes at comparable SSIM on its datasets. Tools included libwebp 0.1.2 (released Q1 2011) and libjpeg 6b. It acknowledges PSNR limitations and uses SSIM instead.
- Limitations/open leads: This is an old encoder/corpus-specific result, not expected project savings. It supplies neither universal q equivalence nor a text-readability acceptance threshold. Re-encoding already-lossy sources can retain source artifacts; an output can only be assessed against the supplied source, not recover the original artwork. Whole-image metrics alone are not evidence that every word remains legible (inference).

### C06 — Lossless/alpha benchmark includes encoding memory cost

- Source/title: [Lossless and Transparency Encoding in WebP](https://developers.google.com/speed/webp/docs/webp_lossless_alpha_study)
- Publisher: Google; authors: Jyrki Alakuijala and Vincent Rabaud; pub_date: unknown; body updated: 2017-08-01; accessed: 2026-10-08; confidence: high for reported experiment, low for deployment prediction; class: primary vendor benchmark.
- Claim: The reported default-WebP experiment achieved 42% denser compression than libpng and 23% than ZopfliPNG; results used specified tools/corpora, including 2017 PNG tooling and libwebp from head. It also discusses lossy RGB plus lossless alpha. Its profiling recorded peak RSS and found increased encoding memory compared with the evaluated PNG path.
- Limitations/open leads: No saving, speed, or peak-memory number is promised for storybook pages or the present runtime. Vendor authorship, historical versions, and a limited memory-profile input constrain transferability. Measure candidate encoders on representative source pages and deployment limits.

### C07 — Alpha, ICC, EXIF and animation are explicit WebP features

- Source/title: [WebP Container Specification](https://developers.google.com/speed/webp/docs/riff_container)
- Publisher: Google / WebP; pub_date: unknown; updated: 2025-08-07; accessed: 2026-10-08; confidence: high; class: primary official format specification.
- Claim: WebP supports lossy/lossless pixel encodings, alpha, optional ICC, optional EXIF/XMP, and multiple animation frames. Without an ICC chunk, readers should assume sRGB. Animation has frame timing, blending, and disposal semantics, beyond a static pixel matrix.
- Limitations/open leads: Container capacity does not prove that a chosen encoder copies metadata or that a particular client honors it. C04's default metadata stripping therefore matters. Detect animation as a separate input class; applying a still-image transform risks losing its semantics (inference). Validate actual decoded color/alpha on the target app.

### C08 — ImageIO depends on the registered runtime providers

- Source/title: [ImageIO — Java SE 21 & JDK 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/ImageIO.html)
- Publisher: Oracle; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: primary official Java API documentation.
- Claim: ImageIO discovers registered readers/writers and exposes format-name and MIME enumerations. `read` returns a decoded `BufferedImage`, or null if no reader claims the stream. `write` returns false if no suitable writer exists. Provider registration uses service-provider discovery. Image stream caching may use memory or disk.
- Limitations/open leads: This does not establish the deployment's installed WebP/AVIF providers, writer quality, metadata behavior, or JDK version. Enumerate and round-trip test the actual container/runtime. Check `write`'s result and read back outputs. A successful build does not prove the needed codec is present in production (inference).

### C09 — Java read parameters support destination sizing and subsampling

- Source/title: [ImageReadParam — Java SE 21 & JDK 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/ImageReadParam.html)
- Publisher: Oracle; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: primary official Java API documentation.
- Claim: Read parameters describe source-region clipping, subsampling, and a destination `BufferedImage`. Source render-size support is optional; subsampling must be supported by `ImageReader` implementations under the documented contract. Unrecognized extended reader-specific features can be ignored.
- Limitations/open leads: This API contract is not a guarantee of proportional RSS/CPU savings or identical filters across plugins. A convenience full `ImageIO.read` followed by resize should not be assumed memory-equivalent to an explicit subsampled read (inference). Benchmark the chosen reader's allocation behavior and final text quality.

### C10 — libwebp allocates native picture/output buffers and exposes versioning

- Source/title: [libwebp src/webp/encode.h — upstream main](https://chromium.googlesource.com/webm/libwebp/+/refs/heads/main/src/webp/encode.h)
- Publisher: WebM / libwebp upstream hosted by Google; pub_date: unknown; revision: moving main inspected 2026-10-08; accessed: 2026-10-08; confidence: high for inspected API, conditional for pinned deployment; class: primary upstream API source.
- Claim: The API exposes `WebPGetEncoderVersion`, version-checked initialization, native picture allocation/freeing, and RGBA imports. Picture input can be 32-bit ARGB or YUV planes; upstream recommends ARGB for lossless and YUV for lossy. Encoder threading is configurable. Output ownership requires explicit freeing; initialization/import/allocation/encode can fail. `exact` controls hidden transparent RGB preservation.
- Limitations/open leads: Main is not a pinned release or production binary. A JVM bridge's extra copies and the encoder's actual working-set peak are unmeasured. Pin the selected release/build, verify the loaded version and architecture, and test cleanup/failure behavior. Native allocations contribute to RSS beyond Java heap (inference from native buffers).

### C11 — Subsampling is decimation, not a promised high-quality resize

- Source/title: [IIOParam — Java SE 21 & JDK 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/IIOParam.html)
- Publisher: Oracle; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: primary official Java API documentation.
- Claim: `setSourceSubsampling` defines a decimation grid: period two selects every other row/column. Source regions and grid offsets influence the selected pixels. The API documents seam/artifact concerns when grids differ across assembled tiles.
- Limitations/open leads: Decimation alone does not prove adequate anti-aliasing for small text or thin illustration lines. Consider reader subsampling to a safe intermediate size, then a consistent final resample; compare with full decode when deriving accepted production settings (engineering proposal, not measured equivalence).

### C12 — Orientation is metadata that must be accounted for

- Source/title: [kCGImageDestinationOrientation](https://developer.apple.com/documentation/imageio/kcgimagedestinationorientation?changes=_8&language=objc)
- Publisher: Apple; pub_date: unknown; accessed: 2026-10-08; confidence: high for retrieved official indexed documentation, medium for complete API coverage; class: primary official API documentation retrieved via search extract.
- Claim: Apple defines image destination orientation as an EXIF value from 1 to 8.
- Limitations/open leads: The direct canonical page returned a retrieval error; the official indexed extract supplied the claim. This does not prove Java or Flutter auto-applies orientation. Treat rotation/mirroring as explicit normalization and test all eight orientations; avoid retaining a stale orientation after baking the transform (engineering proposal supported by orientation metadata's existence and C04).

## Decision synthesis — engineering inferences and proposals

These are conditional decisions derived from the ledger, not measured results. Each row identifies its evidence and limitations.

| ID | Proposed decision | Evidence; confidence; limits |
|---|---|---|
| R01 | Use WebP as the principal optimization candidate, with separate lossless and lossy trials; retain JPEG/PNG when they meet the requirements with fewer bytes or lower operational cost. | C01–C08; high confidence in candidate compatibility, unmeasured optimal codec. Native Flutter's documented JPEG/PNG/WebP contract covers the intended common format set; device fixtures still required. |
| R02 | Prefer a lossless candidate first for text-heavy/flat illustration pages and transparency-sensitive content. Allow lossy candidates only after visual and metric acceptance. PNG remains a baseline/reference candidate; JPEG is a candidate for opaque photographic material. | C03–C07; moderate confidence in content-based policy, no universal size winner. Do not flatten alpha silently to obtain JPEG output; explicitly approved compositing would be a separate transform. |
| R03 | Do not use AVIF as the only delivery format for this Android API 24 / iOS 15 native-Flutter floor. Consider it only as an optional future variant after capability and fallback validation. | C01–C03; high confidence that the evidence does not establish universal AVIF coverage. Android AVIF OS support and Safari AVIF support cannot be substituted for Flutter decoder proof. Exact iOS-native AVIF floor remains unverified in this digest. |
| R04 | Never translate “JPEG q=85” directly to “WebP q=85” or to AVIF. Tune each selected encoder/version/mode using the same source/reference and output geometry. | C04–C05; high confidence in rejecting universal q equivalence; no accepted q settings supplied. Lossless effort and lossy distortion controls must have distinct configuration names. |
| R05 | Make already-optimized pass-through a valid success outcome. Preserve the source when no accepted candidate is strictly smaller or when no permitted transform is needed. Record accepted output and original byte counts. | C04–C06, C08; moderate confidence; proposal minimizes unnecessary lossy generations, not a claim that every source is already optimal. Byte comparison alone is insufficient without compatibility, geometry, color, alpha and readability checks. |
| R06 | Separate animation detection/routing from static publication. Preserve or reject unsupported animations under an explicit product rule instead of silently encoding a first frame. | C01, C04, C07; high confidence in the distinction; the brief covers still images, so no animation encoder is selected. |
| R07 | Keep Java orchestration. First assess the existing ImageIO providers. Add a maintained WebP provider or a pinned libwebp/cwebp runtime path only if corpus results justify it. This evidence does not require a service rewrite. | C04, C08–C11; moderate confidence in evaluation order; no provider/library version or deployment image was inspected. JNI and subprocess integration are alternatives to benchmark, not interchangeable measured implementations. |

## Publication and quality experiment protocol — proposed, not a discovered standard

Basis: C03–C12. Confidence: moderate in the method; thresholds, output geometry, concurrency and actual savings remain open. No universal SSIM/PSNR acceptance number is supported by these sources.

1. Build a representative fixture set: smallest embedded text, multiple scripts/diacritics, thin strokes, flat colors, gradients, detailed illustration, dark text on light backgrounds and the inverse, transparent edges, non-sRGB ICC, and all EXIF orientation cases. Include previously compressed JPEG/WebP sources and tiny already-optimized files. This is a proposed corpus, not evidence that these source classes exist in the project.
2. Normalize the reference once: apply orientation/mirroring; choose a documented color policy (for example, explicit conversion to sRGB) and alpha policy; resize once to the exact reference geometry used by every codec candidate. Keep the original source separately. If comparing the effect of downscaling, report that separately from codec distortion.
3. Encode candidates from that same normalized reference. Decode each output back to the same geometry and color/alpha interpretation before computing SSIM/PSNR. Record encoder binary/provider/version and settings, bytes, dimensions, elapsed time, peak process RSS and failures. Do not compare a small encoded derivative to a full-size original using an unexplained resize inside the metric.
4. Use SSIM/PSNR as regression screens and rate–distortion curves, with disclosed implementation/channel/range/settings. They are not a substitute for reading the smallest text and inspecting edge/color/alpha behavior on minimum-supported Android/iOS devices at actual display sizes. Include text/edge crops and transparent composites over light/dark backgrounds. If losslessness is required, check exact pixel equality after normalization, including the chosen policy for hidden RGB.
5. Inspect output MIME/signature, dimensions, alpha, orientation, color policy and decodability. Only publish an accepted smaller candidate. Retain a compatible validated original when candidates fail or do not improve size. Do not classify metadata stripping alone as harmless until orientation/color effects are accounted for.

## Worker memory and concurrency experiment — proposed bounds

Basis: C04, C06, C08–C11. Confidence: high that decoded buffers/native allocations matter; sizing and concurrency figures remain unmeasured.

- Read dimensions and validate pixel-count limits before allocating a full raster. Use explicit ImageReader parameters where suitable; compare subsampled intermediate decode with full decode for quality and measured peak RSS. Output size does not itself bound full-source decode cost.
- For a 32-bit ARGB raster, the pixel plane is arithmetically `width × height × 4` bytes before headers, copies, other planes, encoder state and encoded buffers. Example: 4000 × 3000 is 48,000,000 bytes for that one plane. This is a lower-bound accounting example derived from C10's 32-bit plane, not a measured Java/native peak.
- Budget for simultaneous source/intermediate/output rasters, JVM heap and native memory, encoded input/output buffers and cache/temp storage. A subprocess still contributes to container RSS. Do not set concurrency from compressed file bytes or heap alone.
- Benchmark the exact production-sized worker container at multiple concurrent jobs and encoder thread settings. Admit work through a bounded queue/semaphore under measured pixel/RSS/CPU/time limits. Choose outer-job concurrency jointly with per-encode threading; avoid assuming every job can consume all cores.
- In a native bridge, exercise buffer cleanup and all error returns. In a subprocess path, pin executable plus dependencies, verify `cwebp -version`, validate timeout/exit/output behavior, and bound temporary/output data. In ImageIO, enumerate providers and round-trip JPEG/PNG/WebP fixtures in the actual packaged runtime, including ICC/alpha/orientation.

## Unresolved leads and evidence exclusions

- No project-specific byte savings, latency improvement, optimal dimensions/q, quality thresholds, peak RSS, CPU budget or worker count can be concluded without the normalized corpus benchmark.
- Exact iOS 15 native AVIF support and the shipped Flutter engine's AVIF fallback path were not established. No assumption is made from Safari/browser support.
- No installed JVM WebP/AVIF writer, JVM version, native encoder build, container architecture or metadata implementation was inspected.
- No universal “visually lossless” claim is adopted for lossy q=100 or near-lossless preprocessing. “Lossless WebP” must be paired with an explicit hidden-transparent-RGB policy when pixel identity is required.
- Google benchmark percentages are retained only as labeled historical vendor experiments. They must not be reused as project projections or multiplied by project traffic without measured corpus results.
