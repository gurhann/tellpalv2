# Critical-claim spot verification

Accessed: 2026-10-08. Fresh-context, bounded verification of the four `*-r1.md` digests and `geometry-examples.json` only. No project code, configuration, memory, or measurements were admitted. Five web calls; eight retained primary sources. Search discoveries outside the retained list were excluded. Apple HTML, indexed text, and its JSON representation are one document, not independent sources. Publication dates remain unknown unless explicitly supplied below.

**Epistemic rule:** training proposes questions, never establishes a finding. `verified` means supported within the stated scope; it does not mean measured on a shipped app. `single-primary-source` explicitly excludes independent-publisher verification. Flutter/Android documents are not treated as independent publishers from Google. Imported digest claims remain distinguishable from fresh retrieval.

## Retained source register

| ID | Exact source URL / publisher | Publication or update date | Accessed | Retrieval limit |
|---|---|---|---|---|
| S1 | https://github.com/flutter/flutter/blob/main/packages/flutter/lib/src/painting/image_cache.dart — Flutter upstream / Google | Publication unknown; mutable `main` | 2026-10-08 | Retrieved source snapshot; not the deployed SDK revision. |
| S2 | https://api.flutter.dev/flutter/widgets/precacheImage.html — Flutter / Google | Unknown | 2026-10-08 | Rolling API documentation with implementation. |
| S3 | https://api.flutter.dev/flutter/painting/applyBoxFit.html — Flutter / Google | Unknown | 2026-10-08 | Rolling fit implementation; no decoder benchmark. |
| S4 | https://api.flutter.dev/flutter/dart-ui/instantiateImageCodec.html — Flutter / Google | Unknown | 2026-10-08 | Portable documented contract, not every engine/device capability. |
| S5 | https://developer.android.com/reference/android/content/ComponentCallbacks2 — Android Developers / Google | Publication unknown; updated 2026-09-24 | 2026-10-08 | Platform API contract; embedding propagation untested. |
| S6 | https://developer.android.com/media/platform/supported-formats — Android Developers / Google | Publication unknown; updated 2026-10-07 | 2026-10-08 | Android decoder/encoder columns must remain separate. |
| S7 | https://developer.apple.com/documentation/Xcode/testing-a-release-build?changes=_9_5&language=objc — Apple Developer | Unknown | 2026-10-08 | Official indexed full text was readable; canonical HTML is JavaScript-only. Canonical: https://developer.apple.com/documentation/xcode/testing-a-release-build |
| S8 | https://datatracker.ietf.org/doc/html/rfc9111 — IETF | Published 2022-06 | 2026-10-08 | Normative HTTP semantics; not proof of any cache library's compliance. |

## Claim statuses and corrections

| ID / digest claim | Status | Evidence and bounded conclusion |
|---|---|---|
| V1 — Compressed bytes versus decoded/rendered memory (`flutter` F01; `memory` C01/I01) | **verified; independent-publisher corroboration** | S7, “Support user-defined input,” explicitly distinguishes compressed disk data from uncompressed display data and derives one 32-bit image plane from dimensions. This independently corroborates the Flutter Image documentation already imported in the digests. `W × H × 4` describes an assumed pixel plane; it is not a process peak, a universal color format, or a guarantee of proportional savings. |
| V2 — LRU cap versus live/process ownership (`flutter` F07; `memory` I03) | **verified; single-primary-source** | S1 distinguishes keepAlive LRU, pending, and live tracking. Limits/counters govern cached entries; other owners survive cache eviction. Default bytes are `100 << 20` = 100 MiB. A cache limit cannot establish a total-process bound. No independent publisher validates these Flutter internals. |
| V3 — Unfinished loads automatically re-enter after `clear()` (`memory` C03) | **disputed; correction required** | S1's prose says unfinished loads can enter later, but `clear()` removes pending listeners and clears pending tracking. Its listener performs LRU insertion. Preserve the explicit documentation/implementation discrepancy already recognized in `flutter` O05; do not promote automatic reinsertion to guaranteed standard-cache behavior. Clearing cache bookkeeping is not evidence of HTTP cancellation. |
| V4 — Precache Future means success/retention (`flutter` F08; `memory` C04) | **verified rejection; single-primary-source** | S2 completes the Future for availability or failure; `onError` carries failure. Matching key plus retention are required for reuse. Retention can be refused; persistent listeners can pin pixels. A completed Future alone proves neither success, permanent retention, nor first paint. |
| V5 — Precache listener always waits for frame end (`flutter` F08 wording) | **overturned as a universal statement; correction required** | S2's success callback schedules removal after the frame. Its error callback removes the listener immediately. Narrow the wording to “on success, the precache listener remains until the post-frame callback; on failure it is removed immediately.” |
| V6 — Physical-pixel contain/cover formulas (`flutter` F02–F03) | **verified mathematical derivation** | S3 retains the full source for contain and crops it for cover. Hence full-image sampling scales are respectively `min(Pw/Iw, Ph/Ih)` and `max(Pw/Iw, Ph/Ih)`. Clamping scale to 1 avoids decode upscaling; rounding each result upward introduces under one pixel per-axis excess. These are sampling choices, not measured sharpness or transient-memory guarantees. |
| V7 — AVIF as sole portable delivery format (`codecs` R03; `delivery` D4) | **verified as a compatibility restriction; single-publisher evidence** | S4's documented format list excludes AVIF and makes additional platform decoding conditional. S6 lists baseline 8-bit AVIF decoding from Android 12, with optional 10-bit support; this does not establish coverage at the requested API 24 floor. This supports retaining a compatible fallback. Exact iOS 15 native AVIF support, pinned Flutter fallback, and device results remain **unverified**. It does not prove AVIF fails on every device. |
| V8 — Modern Android warning limits (`memory` C07) | **verified; single-primary-source** | S5 marks RUNNING_MODERATE/LOW/CRITICAL, MODERATE and COMPLETE deprecated at API 35 and undelivered from API 34. Its inherited onLowMemory entry says never called from API 34 and deprecated at API 35. UI_HIDDEN/BACKGROUND remain documented. Threshold comparisons are advised. Delivery through a particular Flutter embedding remains **unverified**. |
| V9 — `private`, `no-cache`, `no-store` (`delivery` D1/D3) | **verified; independent-publisher corroboration for HTTP semantics** | S8 §§5.2.2.4, .5, .7: unqualified private excludes shared storage while permitting private caching subject to other rules; unqualified no-cache requires successful validation before reuse; no-store prohibits storage/reuse, with its documented must-understand exception. Cache directives alone are insufficient access/privacy controls. This independently corroborates the imported Google explanation, not Cloud Storage integration details or signed-URL behavior. |
| V10 — Physical-device/release validation (`memory` C11/C12) | **verified; independent-publisher corroboration for general method** | S7 calls for actual-device release tests across supported hardware/OS combinations; debugger attachment changes watchdog/suspension behavior. It recommends proactive memory investigation with Instruments/MetricKit. This corroborates general validation principles independently of Flutter/Google. Exact DevTools categories, 500-ms sampling, and Flutter profile-mode semantics remain **single-primary-source**, not independently verified here. |

## Independent geometry recalculation

Recomputed from the JSON inputs using Python arithmetic, without reading the generating implementation. Card physical extents equal logical extents times DPR. Integer sampling targets apply ceiling. Hero extents are supplied hypothetical inputs; no real layout or source inventory is established.

| Case | Raw card physical extent | Integer card target | Hero extent | Full-source contain decode, ceiling | Full-source cover decode, ceiling | 512×909 source visible-height fraction under cover |
|---|---|---|---|---|---|---|
| phone | 508.995 × 678.66 | 509 × 679 | 1233 × 1370 | 1233 × 771 | **2192 × 1370** | **0.6258403618139592** |
| tablet | 465.6 × 620.8 | 466 × 621 | 1600 × 1454.5454545454545 | 1600 × 1000 | **2328 × 1455** | **0.5120512051205120** |

All stored numbers agree within floating-point rounding. Visibility uses `min(1, Ph / (909 × max(Pw/512, Ph/909)))`. The JSON's raw fractional physical extents are not integer decode arguments. If the tablet destination is first rounded to 1600×1455, its cover decode remains 2328×1455 but its visible-height fraction becomes 0.5122112211221123. Label which convention is used rather than mixing them.

One-plane payloads were independently recomputed as `W × H × 4 / 1024²`: 4096×3072 = **48 MiB**; 1024×768 = **3 MiB**; 1440×1920 = **10.546875 MiB**; 2048×1536 = **12 MiB**. All match. These deterministic illustrations are not device measurements or safe budgets.

No load-bearing conclusion was otherwise overturned. Required text corrections are V3 and V5; exact package/engine/device behavior and all performance gains remain outside this spot-check.
