# Flutter native image delivery and decode research digest

Date/access date: 2026-10-08. Scope: native Flutter Android/iOS; requested platform floor Android API 24/iOS 15. This is external research, not a repository audit. No project code, configuration, brief, PRD, or memory was consulted. No measurements or benchmark estimates were generated.

## Standing rules

1. Never conclude from training data alone. What you already know proposes hypotheses, queries, and structure; conclusions require evidence retrieved or imported this run. A claim you cannot evidence is stated as an unverified belief or not at all.
2. The research firewall. Project context — briefs, PRDs, code, memory, persistent facts — shapes what to ask, never what is true. It is inadmissible as evidence: every claim in a research artifact traces to a digest or import file with a source. Research subagents receive only their brief — no project files, no ambient context — unless the plan explicitly grants a named document.

## Retrieval accounting and interpretation

Nine web calls: one broad discovery search; subsequent focused retrieval/verification of the crucial API and upstream implementation leads. Twelve distinct evidence sources are retained below. Failed fetches and discovery hits are not evidence. Flutter API pages rendered a `Flutter 0.0.0` footer, so they do not establish a stable SDK release. GitHub `main`/`develop` URLs are mutable and some equivalent branch paths failed retrieval. Findings are observations of retrieved material, not proof about an installed package revision. `pub_date: unknown` means the examined page did not expose a trustworthy publication date; crawl dates and relative package publication labels were not converted into publication dates.

Classes: **fact** = explicit API documentation; **implementation** = observed upstream code; **inference** = reasoning from stated sources; **proposal** = a suggested policy requiring product/device validation; **open** = evidence insufficient. Confidence refers to the stated claim within these limitations, not universal behavior across all Flutter versions.

## Evidence and claims

### F01 — Layout dimensions, decoded dimensions, and memory

`Image.width`/`height` are logical pixels. Image storage is uncompressed for rendering. `cacheWidth`/`cacheHeight` specify decode/store dimensions instead of natural dimensions. A smaller paint rectangle alone does not establish a smaller decoded image. Under an explicitly assumed 32 bits/pixel, nominal decoded pixel payload is `4 × decodedWidth × decodedHeight`; that is not peak process memory.

Evidence: [Image class](https://api.flutter.dev/flutter/widgets/Image-class.html), Flutter team; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: fact, with the “paint rectangle alone” and payload-boundary statements classified as inference. Locators: Memory usage; width/height properties.

### F02 — Convert a logical destination to a physical pixel target

Device pixel ratio is physical/device pixels per logical pixel; it can be fractional. The framework uses logical pixels. A target `P_w = ceil(L_w × DPR)`, `P_h = ceil(L_h × DPR)` is a conservative integer physical sampling target derived from that definition. Recompute it when layout or DPR changes. The docs recommend MediaQuery-based access for rebuilds on DPR changes.

Evidence: [FlutterView.devicePixelRatio](https://api.flutter.dev/flutter/dart-ui/FlutterView/devicePixelRatio.html), Flutter team; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: fact for definitions/rebuild behavior, inference for the ceiling formula. This is a sampling target, not a guarantee of perceived quality.

### F03 — Aspect-preserving `contain` and `cover` targets

For intrinsic oriented source dimensions `I_w,I_h > 0` and physical destination `P_w,P_h > 0`:

```text
s_contain = min(P_w / I_w, P_h / I_h)
s_cover   = max(P_w / I_w, P_h / I_h)
s_decode  = min(1, selected_scale)       // avoid decode upscaling
D_w       = min(I_w, ceil(I_w * s_decode))
D_h       = min(I_h, ceil(I_h * s_decode))
```

These are derivations of Flutter's fit algorithm for a full-image decode; integer rounding can introduce approximately one pixel of aspect error. `contain` displays the full source with unused destination space. `cover` crops a source subrectangle to fill the destination. The full decoded bitmap for `cover` may therefore exceed the destination on one axis. If the intrinsic source cannot supply the target, native-detail preservation is impossible through decode sizing alone.

Evidence: [applyBoxFit](https://api.flutter.dev/flutter/painting/applyBoxFit.html), Flutter team; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: implementation for fit/crop behavior, inference for decode formulas and implications. Locators: `BoxFit.contain`/`BoxFit.cover` branches.

### F04 — `ResizeImagePolicy.fit` means containment, not coverage

`exact` is the default. With two dimensions it can change source aspect ratio; with one dimension it preserves source aspect ratio. Default `allowUpscaling:false` clamps each exact dimension to intrinsic dimensions, which can also alter the requested aspect ratio. `fit` preserves source aspect ratio within the supplied bounding box and only downscales unless upscaling is enabled. It is conceptually `contain`.

Evidence: [ResizeImagePolicy](https://api.flutter.dev/flutter/painting/ResizeImagePolicy.html), Flutter team; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: fact.

### F05 — A decode target and a paint target are separate choices

`ResizeImage` instructs decoding at specified dimensions and allows painting at other dimensions. It defaults to `exact` and `allowUpscaling:false`. Its docs also warn indirectly of layout coupling: an unconstrained image's logical size can follow the chosen resolution. Set destination layout constraints explicitly rather than relying on the decode size to define presentation.

Evidence: [ResizeImage class](https://api.flutter.dev/flutter/painting/ResizeImage-class.html), Flutter team; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: fact for API behavior, proposal for explicit layout constraints.

### F06 — Memory variant identity includes resize parameters

Observed `ResizeImageKey` equality/hash includes underlying provider key, policy, width, height, and `allowUpscaling`. Therefore two unequal decode specifications can occupy separate memory entries. Reuse the same resolved specification for precache and display; uncontrolled near-identical dimensions can fragment reuse. Size bucketing is a possible policy, with upward buckets capped by intrinsic pixels and validated for memory/quality.

Evidence: [Flutter image_provider.dart](https://github.com/flutter/flutter/blob/main/packages/flutter/lib/src/painting/image_provider.dart), Flutter team; pub_date: unknown; accessed: 2026-10-08; confidence: high for observed key fields, medium for policy effectiveness; class: implementation/inference/proposal. Locator: `ResizeImageKey.operator==` and `hashCode`.

### F07 — LRU accounting is not a process-memory ceiling

Observed defaults are 1,000 entries and `100 << 20` bytes, exactly 100 MiB despite the docs' “100 MB” wording. Limits govern keepAlive LRU entries. Pending entries have not completed; live means a listener remains; live can overlap pending or keepAlive. `currentSizeBytes` counts cached entries, not every live image or process allocation. Oversized images are not admitted to the LRU. `clear()` removes pending tracking/keepAlive entries while live listeners can retain pixels. Clearing live tracking does not itself relieve memory pressure. Evicting live images to relieve pressure can cause duplicate allocation on reload; upstream recommends `includeLive:false` for that purpose. Consequently a small cache limit cannot guarantee OOM avoidance or a safe prefetch count.

Evidence: [Flutter image_cache.dart](https://github.com/flutter/flutter/blob/main/packages/flutter/lib/src/painting/image_cache.dart), Flutter team; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: implementation except final consequence, which is inference. Locators: defaults; `putIfAbsent`; `ImageCacheStatus`; `clear`; `clearLiveImages`; `evict`.

### F08 — Precache is a decoded-image lifecycle operation

The precache future completes at first image availability **or failure** and does not complete with error; use `onError`. Reuse requires matching image keys and cache retention. Its listener is released at the end of the frame after completion. The cache can refuse retention, including for oversized images. Adding a persistent listener pins the image even when it does not fit the cache; official docs warn that large/many pinned images can cause OS process death, sometimes without diagnostics. The optional `size` selects image configuration; it is not documented as a universal decode-resize switch. Precache the final resized provider, keep a bounded prospective set, and do not infer success solely from the returned future.

Evidence: [precacheImage](https://api.flutter.dev/flutter/widgets/precacheImage.html), Flutter team; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: fact, with final usage guidance classified as proposal.

### F09 — CachedNetworkImage exposes two resizing layers

`memCacheWidth`/`memCacheHeight` are documented as in-memory resizing through `ResizeImage`; disk maxima request resized disk files. The widget forwards memory dimensions to OctoImage and disk dimensions to `CachedNetworkImageProvider`. Its custom `imageBuilder` receives the underlying `_image` provider; callers should verify their custom builder's final decode sizing rather than assume the widget's memory settings automatically wrap arbitrary custom output.

Evidence: [cached_image_widget.dart](https://github.com/Baseflow/flutter_cached_network_image/blob/main/cached_network_image/lib/src/cached_image_widget.dart), Baseflow; pub_date: unknown; accessed: 2026-10-08; confidence: high for observed fields/forwarding, medium for custom-builder implication; class: implementation/inference. Locators: resize property docs; constructor; `build`; `_octoImageBuilder`. OctoImage's internals were not separately inspected in this budget.

### F10 — CachedNetworkImage provider keys can reuse across different URLs

Observed provider equality/hash uses `cacheKey ?? url`, scale, maxHeight, and maxWidth. It does not include headers or cache-manager identity. With a custom key, equal scale/maxima and the same key can therefore resolve as the same memory identity even if URLs or headers differ. A key must identify the actual content/version/variant and any relevant access context, not merely a screen role. Never reuse one key for different response bytes. Different resize maxima distinguish provider memory variants in this source.

Evidence: [cached_network_image_provider.dart](https://github.com/Baseflow/flutter_cached_network_image/blob/develop/cached_network_image/lib/src/image_provider/cached_network_image_provider.dart), Baseflow; pub_date: unknown; accessed: 2026-10-08; confidence: high for retrieved implementation, medium for applying it to any released version; class: implementation/inference/proposal. Locator: `operator==`/`hashCode`. The equivalent `main` file fetch failed; release pinning remains required.

### F11 — Disk decode begins after a file is available

The native loader requests `getImageFile` for an ImageCacheManager, otherwise `getFileStream`. Each resulting FileInfo is read fully with `readAsBytes()` and then decoded. Disk resize maxima are ignored with a normal CacheManager, with an assert describing the requirement. In-memory decode sizing does not by itself change the requested URL or eliminate the file-byte read. Encoded bytes and decode activity are additional reasons not to equate the LRU byte counter with peak memory; no peak multiplier is established here.

Evidence: [native _image_loader.dart](https://github.com/Baseflow/flutter_cached_network_image/blob/main/cached_network_image/lib/src/image_provider/_image_loader.dart), Baseflow; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: implementation/inference. Locator: `_load`.

### F12 — Client disk resizing does not reduce the original first transfer

`getImageFile` fits within maxima while preserving aspect ratio, generates disk keys such as `resized_w100_h75_<baseKey>`, and fetches/stores the original from cache or online before resizing when the resized entry is unavailable. Thus these maxima are local file-transform controls, not server image-variant negotiation. First-transfer reduction requires a smaller response from the supplied URL/server variant; a local resized hit can avoid later original fetches. The implementation shares running resize streams per resized key through `_runningResizes`.

Evidence: [ImageCacheManager.getImageFile](https://pub.dev/documentation/flutter_cache_manager/latest/flutter_cache_manager/ImageCacheManager/getImageFile.html), Baseflow (published through pub.dev, footer `flutter_cache_manager 3.4.5`); pub_date: unknown; accessed: 2026-10-08; confidence: high; class: fact/implementation, with transfer consequence classified as inference.

## Decision implications derived from the evidence

These are proposals, not device-tested outcomes. Evidence metadata is inherited from the explicitly linked claims above, whose source URLs/titles/publishers/dates remain authoritative.

- **P01; class: proposal; confidence: medium; evidence: F01–F05, F12.** Choose delivered and decoded pixels by rendered role, measured destination, DPR, fit, and intrinsic aspect ratio. Keep transfer selection, disk file caching, decode selection, and paint/layout as explicit independent decisions. Do not claim that `cacheWidth`, memory maxima, or local disk maxima shrink first-download bytes.
- **P02; class: inference/proposal; confidence: high for geometry, medium for product quality; evidence: F03–F05, F12.** For full-source `cover`, choose aspect-preserving decode dimensions from the maximum scale. For `contain`, choose the minimum scale. Alternatively, a server variant cropped to the role's aspect ratio can supply a destination-sized source. Do not pass a square bounding box through `fit` and expect it to provide full `cover` detail for a tall/wide source. Do not use two mismatched exact dimensions as an aspect-preservation strategy.
- **P03; class: inference/proposal; confidence: high; evidence: F06, F08–F10.** Define a stable, versioned variant identity and use the same provider identity/specification for display and precache. A reusable cacheKey can survive tokenized URL changes only when it still denotes identical content and safe access context. Variant geometry/content changes must change identity.
- **P04; class: proposal; confidence: medium; evidence: F07–F08, F11.** Bound active loads and speculative precache separately from LRU retention. Observe LRU bytes/count plus live/pending counts and process/native memory during real navigation. Cache tuning alone cannot establish a device-safe memory budget.

## Contradictions, uncertainty, and open checks

- **O01; class: open; confidence: high in the limitation; evidence: F09–F12.** The examined public widget/provider/loader does not establish a contract that disposal or removing an ImageStream listener aborts an in-flight HTTP request. `getImageFile` uses a broadcast stream without an exposed cancellation handle here. End-to-end cancellation requires the completer, manager, file service, shared-download subscriptions, and exact package revision to be inspected or experimentally verified. “Scroll away cancels transfer” is unsupported. No universal “never cancels” claim is made either.
- **O02; class: open; confidence: high in the limitation; evidence: F01, F03–F05, F11.** Decoder subsampling strategy, transient/full-resolution intermediate allocations, format-dependent peaks, animation frame retention, renderer textures, filter quality, and perceived sharpness were not quantified. A smaller final decoded image is evidenced; proportional peak-RAM/CPU savings are not. No benchmark or OOM-safe concurrency number can be inferred.
- **O03; class: open; confidence: high in the limitation; evidence: F01–F12.** The requested API 24/iOS 15 floor is a research boundary, not a verified compatibility claim. This digest does not establish Flutter/package support floors, AVIF support, or codec parity across those OS versions. No package-release/SDK lockfile was permitted. Validate exact supported versions and actual devices before implementation.
- **O04; class: inference; confidence: high; evidence: F03–F04.** “Fit” is overloaded: paint `BoxFit.cover` and decode `ResizeImagePolicy.fit` are not equivalent operations. Combining contain-sized decoding with cover painting can require resampling upward. That is a quality risk derived from geometry, not a measured visual outcome.
- **O05; class: implementation discrepancy; confidence: high; evidence: F07.** The cache docs say 100 MB; source gives 100 MiB. `clear()` prose says unfinished images may later enter the cache, while implementation removes pending cache listeners. Do not treat either statement as an HTTP cancellation guarantee; cache bookkeeping and network work are separate.
- **O06; class: open; confidence: high in the limitation; evidence: F09–F10.** Cached widget/main loader and provider/develop snapshots were retrieved from different mutable branches. Their behavior must be rechecked against one pinned released dependency before using implementation-specific key/cancellation conclusions.

No unsupported codec/platform, savings percentage, quality score, peak-memory bound, or cancellation guarantee is promoted to a fact.
