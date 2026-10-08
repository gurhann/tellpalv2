# Semantic citation check

Checked 2026-10-08 against `research.md`, `sources.json`, all research digests, the two imported inventories, `geometry-examples.json`, `claims.json`, and `task-refinement.md`. No project implementation, policy files, or ambient project knowledge supplied scientific evidence. Two web calls reopened two primary documents solely to resolve the source-5/source-32 attribution ambiguity. No measurements were performed.

## Concrete findings

1. **Low — Source 5 metadata overstates its scope.** `sources.json:44` says that `image_cache.dart` supports “LRU/live/pending and resize keys.” Resize-key fields/equality are supported by source 32, `image_provider.dart`, and `flutter-r1.md` F06; source 5 supplies cache ownership/accounting. Fresh retrieval found `ResizeImageKey` in [image_provider.dart](https://github.com/flutter/flutter/blob/main/packages/flutter/lib/src/painting/image_provider.dart), not [image_cache.dart](https://github.com/flutter/flutter/blob/main/packages/flutter/lib/src/painting/image_cache.dart). Remove “and resize keys” from source 5's support label. The report body and its source appendix already assign this evidence correctly to [32].

2. **Low — Local Flutter version wording exceeds the imported evidence.** `research.md:32` calls 3.41.1 the “locally resolved ... engine.” `imports/mobile-inventory.md:8` records Flutter 3.41.1 in generated package-configuration metadata and explicitly says that no SDK/version command or platform build was rerun. It does not establish the engine revision or executable/device engine. Narrow the wording to the locally recorded Flutter 3.41.1 SDK/package metadata; retain the exact-engine verification requirement already present in the report. This does not overturn the report's API guidance or the separately checked Baseflow tags.

No unsupported load-bearing scientific conclusion, fabricated performance measurement, false independent-publisher label, incorrect Baseflow tag claim, or publication/update-date conflation was identified in the bounded material reviewed.

## All 32 source mappings

“Supported” here means supported by retrieved evidence preserved in the named digests, within the report's qualifications; it does not mean a device result or independent replication.

| Ref | Preserved evidence and semantic result |
| --- | --- |
| 1 | `flutter-r1` F01; `memory-r1` C01; `verification` V1. Supports decoded/uncompressed images and assumed pixel-plane memory, with Apple corroboration. |
| 2 | `flutter-r1` F02. Supports logical/physical ratio and fractional DPR; ceiling is explicitly a derived sizing choice. |
| 3 | `flutter-r1` F03; `verification` V6. Supports contain/cover derivation; calculations checked separately below. |
| 4 | `flutter-r1` F04. Supports exact versus aspect-preserving containment and distortion risk. |
| 5 | `flutter-r1` F07/O05; `verification` V2/V3. Supports LRU/live/pending distinctions. Source metadata correction above; report avoids disputed automatic reinsertion. |
| 6 | `memory-r1` C03 plus `verification` V3/corrections. Supports live-owner retention after clear; report avoids the digest's corrected reinsertion assertion. |
| 7 | `flutter-r1` F08; `memory-r1` C04; `verification` V4/V5. Supports failure/retention/pinning limitations; report avoids the corrected universal listener-delay wording. |
| 8 | `flutter-r2` R2-01. Supports exact upstream v3.4.1 provider identity; archive comparison remains expressly absent. |
| 9 | `flutter-r2` R2-03/R2-04. Supports original first miss, untargeted FileImage, possible second decode, extension filter and PNG output for the v3.4.1 manager tag. |
| 10 | `codecs-r1` C01; `delivery-r1` D4; `verification` V7. Supports documented portable formats and scaling caveat, not exact engine/AVIF-device capability. |
| 11 | `codecs-r1` C04. Supports mode-specific q, metadata defaults, animation-input exclusion and runtime controls; no accepted q or savings claimed. |
| 12 | `codecs-r1` C05. Supports historical equal-SSIM methodology, expressly not workload savings or universal quality equivalence. |
| 13 | `codecs-r1` C07. Supports container ICC/EXIF/XMP/alpha/animation features; report separates encoder/client behavior. |
| 14 | `codecs-r1` C02; `verification` V7. Supports Android AVIF platform floor limitation at API 24, not universal failure or native iOS claims. |
| 15 | `codecs-r1` C03. Supports JPEG/PNG tradeoffs; report expressly limits Safari/web evidence's native-iOS applicability. |
| 16 | `codecs-r1` C08. Supports runtime reader/writer-provider checks; SE21/deployment version boundary is explicit. |
| 17 | `codecs-r1` C09. Supports read parameters/subsampling/optional rendering; worker improvements remain proposed. |
| 18 | `codecs-r1` C11. Supports decimation semantics and need to qualify final resampling. |
| 19 | `memory-r1` C02. Supports memory categories/sampling limitations, with exact-tool calibration left open. |
| 20 | `memory-r1` C06. Supports classification/memory-class hints rather than live native/GPU budgets. |
| 21 | `memory-r1` C07; `verification` V8. Supports API34/35 warning-contract changes; embedding behavior remains untested. |
| 22 | `memory-r1` C08. Supports process snapshots, differing counters and platform-dependent graphics rows. |
| 23 | `memory-r1` C09. Supports jetsam diagnostic identity/reason/page-size inspection, not inferred image causation. |
| 24 | `memory-r1` C10. Supports best-effort warnings and rapid-allocation termination risk. |
| 25 | `memory-r1` C11; `verification` V1/V10. Supports physical release/background validation and image expansion. |
| 26 | `memory-r1` C12. Supports physical-device profile investigation, with tool/engine differences qualified. |
| 27 | `memory-r1` C05. Supports image-cache clearing on received pressure; report does not guarantee signal delivery or full memory release. |
| 28 | `delivery-r1` D1. Supports public-access prerequisites for built-in Cloud Storage caching; deployment behavior is uninspected. |
| 29 | `delivery-r1` D2. Supports metadata-derived response policy and MIME/ETag validation limitations. |
| 30 | `delivery-r1` D3; `verification` V9. Supports normative directive distinctions independently of Google integration guidance. |
| 31 | `flutter-r2` R2-02. Supports distinct memory/disk forwarding in upstream v3.4.1 widget; no unsupported custom-builder guarantee made. |
| 32 | `flutter-r1` F06 and fresh primary retrieval. Supports resize-key fields/equality, with mutable-upstream limitation. |

## Arithmetic, dates and independence

Independent arithmetic from the named JSON inputs reproduces full-source cover targets 2192×1370 and 2328×1455, visible-height fractions 0.6258403618139592 and 0.512051205120512, and source-resolution JPEG growth 42.3017159%. Raw fractional physical extents are explicitly distinguished from rounded decode arguments. One-plane MiB calculations agree. These are calculations, not measured device quality or process savings.

All 32 source dates match their preserved digest metadata. Unknown original publication dates remain unknown; observed updates are separately labeled. The ledger contains exactly four `verified` and eleven `unverified` entries. Its four verified entries represent three independent-publisher corroborations plus one independent arithmetic recomputation, matching the report's wording. Google/Flutter/Android agreement is not counted as independent publication, and exact iOS15 native AVIF support remains unverified.
