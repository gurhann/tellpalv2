# Current Image and Publication Behavior

Inspected: 2026-10-08. This is source inspection plus rechecking existing local image bytes, not an audit of production storage, a new compression experiment or a reproduced concurrency incident.

## Existing paths and gaps

| Path | Current evidence | Consequence for this work |
| --- | --- | --- |
| Editorial publish | `ContentPublicationService.publishLocalization` and `ContentPublicationPolicy.publish` validate editorial/page references and record `PUBLISHED`. | Publish does not itself orchestrate or await all required optimized images. |
| Manual readiness | `ContentManagementService` accepts localization processing status through creation/update/status and `markAsReady`. | Audit every mutation path; tightening only the publish controller is insufficient. |
| Legacy visibility | `ContentLocalization.isVisibleToMobile` requires `PUBLISHED`, `COMPLETED` and any type-specific body requirements. | These flags do not prove a verified image revision. |
| Listing | `ListingCoverProcessingExecutor` reads sources, encodes both JPEG variants, uploads and verifies stored metadata/SHA/decode/dimensions. `LibraryCoverPreparationService.publish` guards atomic ready-manifest/completion with captured ownership. | Reuse this verification and fencing model; do not replace healthy thumbnails with originals. |
| Listing trigger | `ContentManagementService.prepareListingCover` is called through a separate admin command. | Existing preparation is not a publish lifecycle guarantee. |
| Story detail | `StoryDetailHeroQueryService.verify` requires `ORIGINAL_IMAGE`, verifies source bytes and signs that original storage path. | Safe source validation is not transfer optimization. Detail needs a verified rendition consumer. |
| Page experiment | `StoryPageManagementService` schedules ordered page snapshots after page/localization edits. The executor creates real immutable JPEG renditions/manifests. | Reuse the real converter and manifest pieces, but distinguish experiment defaults and test admission from production readiness. |
| Page ownership | The executor publishes the manifest and then completes the job. `AssetProcessingService` completion/failure ownership checks and pessimistic load apply specifically to `LISTING_COVER`. | Generalize captured source/profile/attempt/lease fencing and atomic ready publication before reusing page jobs as a production publication gate. This is a source-level gap, not an observed race. |
| Legacy delivery | `DefaultImageOptimizationAdapter` produces WebP path plans. The generic executor registers generated plans without actual image upload/verification and completes the job. | Existing `DETAIL_*`/thumbnail registry entries and `DELIVERY COMPLETED` cannot certify outputs. Audio/ZIP plan-only behavior remains a separate dependency. |
| Existing consumers | Legacy public pages map original localized illustration IDs; public asset bundles can read legacy delivery records. Test page manifests use a verified Firebase allowlist. | Define a versioned consumer transition; do not reinterpret all existing public endpoints or tester access as new production delivery. |
| Mobile | Detail decode targets and cache bounds already limit local resource use. | Those limits do not reduce the original HTTP payload; backend renditions and client limits must work together. |

## Evidence entry points

Paths are repository-relative. These locations describe current behavior, not permission to change a boundary.

- `be/src/main/java/com/tellpal/v2/content/application/ContentPublicationService.java:42`
- `be/src/main/java/com/tellpal/v2/content/domain/StoryPublicationReadinessPolicy.java:14`
- `be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java:73`, `:135`, `:186`
- `be/src/main/java/com/tellpal/v2/content/application/AssetProcessingStatusListener.java:19`
- `be/src/main/java/com/tellpal/v2/asset/infrastructure/media/ListingCoverProcessingExecutor.java:27`
- `be/src/main/java/com/tellpal/v2/asset/application/LibraryCoverPreparationService.java:33`
- `be/src/main/java/com/tellpal/v2/asset/application/StoryDetailHeroQueryService.java:48`
- `be/src/main/java/com/tellpal/v2/content/application/StoryPageManagementService.java:120`
- `be/src/main/java/com/tellpal/v2/asset/infrastructure/media/RegisteringAssetProcessingJobExecutor.java:105`, `:131`, `:172`, `:311`
- `be/src/main/java/com/tellpal/v2/asset/application/AssetProcessingService.java:153`, `:263`, `:315`
- `be/src/main/java/com/tellpal/v2/asset/infrastructure/media/DefaultImageOptimizationAdapter.java:20`
- `be/src/main/java/com/tellpal/v2/content/application/query/PublicContentQueryMapper.java:61`
- `be/src/main/java/com/tellpal/v2/asset/application/ContentAssetBundleService.java:50`
- `be/src/main/java/com/tellpal/v2/user/web/mobile/TestStoryPageMediaController.java:73`
- `app/mobile/lib/features/story_detail/presentation/widgets/story_detail_hero.dart:17`
- `app/mobile/lib/core/network/story_detail_image_cache.dart:8`

## Current operational defaults, not newly selected profiles

| Area | Current value |
| --- | --- |
| Listing JPEG | Quality85; phone bounding box360×480, tablet 600×800; preserve ratio, no upscale, white matte |
| Page experiment JPEG | Quality85; longest edge 1440; preserve ratio, no upscale, white matte |
| Source guards | 20 MiB and40 million pixels for these configured image paths |
| Mobile detail decode | Width≤1080 phone/≤1600 tablet, height≤2048, ≤4,194,304pixels; no upscale |
| Mobile detail cache | 20 files/100 MiB disk;20 MiB transfer/four owned attempts |
| App decoded cache | 48 entries/32 MiB in controlled Library entry |

Code defaults are in `application.yml`, `ListingCoverProfile`, `PageImageRenditionProcessor` and the mobile entry/cache. ADR-0017 explicitly treats the page profile as experimental. Source decoding still allocates the source image; output dimension caps alone do not bound worker peak memory. This source audit establishes no general EXIF/ICC normalization or animation-preservation contract. The subsequent [sample-specific color diagnostic](p0-small-jpeg-findings.md) observed managed sRGB decoding, including one P3 input, before resizing/encoding; do not infer color loss solely from removed ICC metadata.

## Available sample evidence

The historical [artwork evidence](../../implementation-artifacts/mobile/library-1-2-existing-artwork-evidence.json) records21 localized source bindings/19 unique hashes,52 listing rendition observations and actual source/encoder-byte checks on 2026-10-07. Source images are JPEG,16,053–73,182bytes, at321×570,428×695,512×831 or512×909. Local rechecking on 2026-10-08 matched all19 unique source hashes and sizes against the retained source manifest; its SHA-256 is `b960ed8ee0a3b2ad977a23becd76e1026553a701034faa21c9a0618f06f2341e`.

The private directory has 57 JPEG files; the remaining 38 are not classified as derivatives without independent manifest identity. Eight source files also match recorded HTTP Hero asset evidence. This small, already-compressed fixture does not represent high-resolution pages, transparency or orientation cases. The subsequent [frozen experiment](p0-small-jpeg-findings.md) adds one local P3 diagnostic to six matched controls and records actual JPEG size regressions; it does not establish new-codec savings, production image distribution or final quality budgets. Native screenshots and golden files are not content source samples; known baked-language mismatches remain unsuitable for localized artwork parity.

## Relationship to existing plans

The2026-09-25 [media preparation plan](../../../be/docs/mobile-media-preparation-plan.md) remains broader: audio, candidate/live, protected starts and production publication. Some of its initial plan-only observations were superseded by real listing/page processing in ADR-0017/0019. This current inspection is authoritative for those specific paths; it does not silently approve the older proposal's remaining decisions. Parent mobile AD-8/AD-10/AD-11 and EXT-MEDIA/EXT-PUBLICATION remain constraints.
