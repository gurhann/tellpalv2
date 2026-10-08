# Exact 3.4.1 image-package verification

Accessed: 2026-10-08. This bounded follow-up checks only `cached_network_image 3.4.1` and `flutter_cache_manager 3.4.1` provider identity and memory/disk resizing. The parent supplied these version numbers as a selection constraint; they are not evidence of package behavior. No project files were read.

## Standing rules

1. Never conclude from training data alone. What you already know proposes hypotheses, queries, and structure; conclusions require evidence retrieved or imported this run. A claim you cannot evidence is stated as an unverified belief or not at all.
2. The research firewall. Project context — briefs, PRDs, code, memory, persistent facts — shapes what to ask, never what is true. It is inadmissible as evidence: every claim in a research artifact traces to a digest or import file with a source. Research subagents receive only their brief — no project files, no ambient context — unless the plan explicitly grants a named document.

## Retrieval and provenance

The four attempted exact-version pub.dev API URLs were inaccessible through the web tool. Upstream `v3.4.1` tag pages were available. Three additional web calls brought the total for r1+r2 to 12. For complete source bodies, the same three tag files were imported read-only from raw.githubusercontent.com after the sandbox network restriction was resolved through automatic approval. This added no broader topics or package versions. Temporary full-text imports are at `/private/tmp/tellpal-image-research-release-3.4.1/`; hashes below identify the exact retrieved bytes. These are **tag-bound upstream implementation** observations; the pub.dev distribution archive was not compared byte-for-byte with the tag.

All claims below carry publisher Baseflow, pub_date unknown, accessed 2026-10-08. “Unknown” is not replaced by a crawl date. Confidence is high for observed source behavior and medium where explicitly making a broader operational inference.

### R2-01 — Provider equality is confirmed in 3.4.1

The exact tag compares `cacheKey ?? url`, scale, maxHeight, and maxWidth; hashCode uses the same fields. Headers and cache-manager identity are absent. Thus an identical custom key and identical scale/maxima can yield the same memory identity across different URLs or request headers. This confirms r1 F10 for the selected upstream tag. Keys must distinguish response content/version/variant/access context when these change bytes or reuse safety.

Evidence: [CachedNetworkImageProvider v3.4.1](https://github.com/Baseflow/flutter_cached_network_image/blob/v3.4.1/cached_network_image/lib/src/image_provider/cached_network_image_provider.dart), Baseflow; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: implementation, with collision implication classified as inference and key guidance as proposal. Source lines 176–187. Raw import SHA-256: `2c338a6fae91bb4fc08bfeaebd5651de11e3ce0f408b7e4d50cc6d75b81d70e3`.

### R2-02 — Memory and disk sizing remain separate in 3.4.1

The widget documents memory dimensions as ResizeImage sizing and disk maxima as disk-file sizing. Constructor forwarding sends disk maxima to provider maxWidth/maxHeight. `build` sends memory dimensions to OctoImage. A custom imageBuilder receives the underlying provider. This confirms r1 F09's forwarding observations for the selected tag. It does not independently verify OctoImage's internal wrapping or arbitrary custom-builder output; do not overstate that point.

Evidence: [CachedNetworkImage widget v3.4.1](https://github.com/Baseflow/flutter_cached_network_image/blob/v3.4.1/cached_network_image/lib/src/cached_image_widget.dart), Baseflow; pub_date: unknown; accessed: 2026-10-08; confidence: high; class: implementation. Source lines 193–203, 245–255, 272–300. Raw import SHA-256: `a5006d5d4dbc085d75090de4e26868bef7bb53b507c90ae9649ae63887603ee8`.

### R2-03 — Disk variant identity and original transfer are confirmed

The exact manager tag constructs a resized key from width, height, and base key; running resize streams are shared by that key. When no usable resized file is present, it gets the original through `getFileStream` before transforming it. Disk maxima do not negotiate a smaller server response or reduce the original first transfer. These details confirm r1 F12 against 3.4.1, superseding the latest-3.4.5 evidence for this package selection.

### R2-04 — Disk resizing first decodes without a target

The resize path first invokes `_decodeImage(originalFile.file)` without dimensions to inspect the image, then invokes it again with resize dimensions if needed. `_decodeImage` resolves a plain FileImage without targets, or a ResizeImage when targets are present. This is not a guaranteed low-peak-memory preprocessing path. The source only resizes extensions jpg/jpeg/png/tga/cur/ico; other extensions return the original. Resized output bytes use PNG encoding. No peak allocation or savings factor was measured.

Evidence for R2-03 and R2-04: [ImageCacheManager v3.4.1](https://github.com/Baseflow/flutter_cache_manager/blob/v3.4.1/flutter_cache_manager/lib/src/cache_managers/image_cache_manager.dart), Baseflow; pub_date: unknown; accessed: 2026-10-08; confidence: high for observed calls, medium for the operational memory implication; class: implementation/inference. Source locators: `getImageFile`, `_resizeImageFile`, `_fetchedResizedFile`, `_decodeImage`; lines 9, 33–61, 73–100, 128–145, 151–168. Raw import SHA-256: `b6daee339be11ce9326009b5f9acfd981b625248b40126ef72b7cedb21e2810c`.

## Consequences and remaining limits

For the selected upstream 3.4.1 tags, r1 F09/F10/F12 now have release-bound supporting sources. Prefer these URLs when making version-specific claims. R1 F11's loader implementation and all HTTP-cancellation conclusions remain outside this narrow release verification. The absence of a cancellation guarantee remains a limitation, not proof that every transport always continues.

No peak-memory, perceptual-quality, compatibility, or process-survival measurement was performed. No installed-version or platform-support claim is made.
