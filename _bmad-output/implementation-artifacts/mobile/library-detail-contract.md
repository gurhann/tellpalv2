# Story 1.2 controlled Library detail contract

Date: 2026-10-07. Status: proposed implementation contract for BMad plan review; not implemented, integration-verified or production-authorized. Parent: [Story 1.2 specification](spec-1-2-open-the-selected-story-detail-from-library.md).

The human approved same-category, same-language eligible recommendations in CMS order and Library-style loading/error/unavailable states. Remaining technical choices below make that scope executable. The reader and actual paywall remain later stories.

## Ownership and admission

Keep existing edges: `user` → `category::api`; `category` → `content::api`; `content` → `asset::api`. `content` supplies scalar metadata/credits; `asset` supplies verified cover-only access; `category` composes the originating category and recommendations; `user` resolves the established controlled Firebase identity/audience before the read-only query. Do not return entities across modules or add a cycle.

Proposed local/test endpoint:

```text
GET /api/test/library/stories/{contentId}
  ?categoryId={originCategoryId}&lang={uiLanguage}
  &contentLang={explicitCardLanguage}&experience=READING
Authorization: Bearer <ephemeral Firebase ID token>
```

Reuse Story 1.1's empty-by-default UID allowlist, server subscriber-fixture subset and profile restriction. There is no client `freeKey`, subscriber switch or new production admission. Invalid/unauthorized calls disclose no detail. These fixtures are not real subscription entitlements.

## Identity and selection

The route carries positive content/category IDs, `READING`, explicit content language and UI language; no entity, token, URL or caller entitlement. Bind the request to the current account context internally. Recheck the category's active/published/opt-in/audience status, language visibility, explicit language mapping and selected item's full curation membership. Membership is not restricted to the first home preview page. Require active STORY content with its exact requested PUBLISHED/COMPLETED localization.

The originating category stays the recommendation source through nested recommendation detail navigation. Scan that category's explicit-language candidates in bounded batches, reuse publication/access preview filtering, remove the current item and duplicates, then apply the existing shared preview limit (initially five). Preserve CMS card order. Locked-but-visible recommendations remain eligible with server lock presentation. Return an empty array when no eligible items exist; render neither an empty section heading nor an invalid target. No implicit language substitution or recommendation ranking is introduced.

## Success and metadata

Schema version 1 includes `categoryId`, `contentId`, `externalKey`, `type=STORY`, `experienceType=READING`, `requestedLanguage`, `contentLanguage`, `audience`, `title`, nullable `ageRange`/`durationMinutes`/`summary`, `credits`, `isFree`, `isLocked`, nullable `hero`, and `recommendations` containing the existing cover-only preview shape.

`isFree` uses the server default free-access set; `isLocked = !isFree && audience != SUBSCRIBER`. Metadata grants no protected-media authority. Null metadata does not become invented zero values or another language's text. A missing summary/credit/pill is omitted cleanly; fixtures must also cover populated fields.

Credits expose stable contributor ID, role and display name, not contributor entities. Include global or exact-language assignments only. Prefer an exact-language assignment over a global assignment for the same contributor/role; use `creditName`, otherwise that contributor's stored name. Order roles as AUTHOR/NARRATOR/ILLUSTRATOR/MUSICIAN, then stored assignment order and ID. Localize role labels in UI language, without translating names or substituting content language.

## Hero provenance and recovery

Use the current localization's uploaded reading-cover source through a query-only asset API. Require real, decodable image bytes and matching source/checksum provenance before real delivery acceptance. Reuse the current-source verification established by verified listing preparation where applicable. Sign only the cover; never packages, pages or narration. The hero descriptor contains asset ID, checksum, MIME type, size, URL and expiry.

The existing `DETAIL_PHONE`/`DETAIL_TABLET` registry plans and `ContentAssetBundleService` references are not real-file proof. Do not display fabricated generated URLs or silently stretch a listing thumbnail as the accepted hero. A missing/unverified source returns absent hero; signing/storage/decoding failures retain metadata and a geometry-preserving artwork error with retry. Source/header/byte checks and client decoding remain bounded; inherit the configured source ceilings (20 MiB and 40 million pixels) without raising existing home transport/cache limits.

Use a separate detail-hero profile of the existing cache package, owned/disposed by the composition root: at most 20 retained files/100 MiB, four owned attempts including cancelled-but-unsettled work, and at most 20 MiB encoded per transfer. Disk admission counts pending files and cleanup ownership; failed admission shows an artwork error rather than evicting the active file. Bound decoded hero dimensions to 1080px phone/1600px tablet wide and 2048px high, preserving source aspect ratio, with a 16 MiB decoded-pixel ceiling. Only the current visible detail holds its large decoded hero; inactive routes retain metadata/scroll/preference and release their image providers. Do not raise or rewrite accepted Library cache or app-global image-cache settings.

Before mobile integration, prove this envelope with actual source dimensions/bytes and boundary/overflow/cancellation/route-visibility cases. Record the chosen profile and actual memory/resource release in evidence; if a source exceeds safe delivery/decoding limits, keep the metadata/error contract and block its loaded-hero acceptance. The epic's measured first-working-flow memory/latency gate still applies; these profile caps are implementation bounds rather than production performance guarantees.

A fresh detail/artwork retry uses the current account, identity and a new attempt; expired URLs are renewed by refetching the same detail context. Only the current attempt may install refreshed hero/recommendation URLs. Retain loaded metadata during artwork recovery. Cache identity includes account, content language, source asset/checksum and transfer attempt; route disposal cannot close shared transport/cache.

## Errors and UI behavior

| Outcome | Contract | Client |
| --- | --- | --- |
| Invalid/missing scalar input or unsupported experience | 400 ProblemDetail `invalid_request` | Defined error; no language fallback |
| Missing/invalid Firebase identity | 401 ProblemDetail | Recoverable admission error; no data |
| Verified UID outside allowlist | 403 ProblemDetail | Access error; no data |
| Unavailable category/content/localization/mapping | 404 ProblemDetail `content_not_found` | Localized unavailable plus back; no repeated automatic retry |
| Transport timeout, network failure, 5xx | Shared normalized transport failure | Localized error plus explicit retry/back |
| Missing/failed hero | Metadata success with artwork failure | Preserve title/metadata/access and artwork geometry |

Multiple internal unavailable reasons may share 404; do not leak them. Use 15-second detail and 10-second artwork recovery waits inherited from controlled Library configuration; record actual transport settlement/resource release and revise only against evidence. No unbounded body/drain/cancellation or duplicate background retry is permitted.

Preserve reference hero/pills/summary/credits and access-aware CTA. CTA remains visibly present with accurate disabled semantics until its reader/paywall consumer exists; it starts no PoC or purchase flow. Narration toggle is a route-owned preference, initially on as in the references, with no audio side effect. Popping restores Library or the originating detail, including its scroll and preference. Deduplicate each repeated navigation action without preventing deliberate subsequent navigation; stale results cannot replace another story or disposed route.

## Implementation map

All backend paths below are relative to `be/src/main/java/com/tellpal/v2/`; new files are planned, not existing capabilities.

- `content/api/StoryDetailMetadataQueryApi.java`, `StoryDetailMetadata.java`; `content/application/query/StoryDetailMetadataQueryService.java`, `StoryDetailMetadataReadRepository.java`; `content/infrastructure/persistence/JdbcStoryDetailMetadataReadRepository.java` — exact-language scalar metadata and bounded credits projection, excluding full media graphs.
- `asset/api/StoryDetailHeroQueryApi.java`, `asset/application/StoryDetailHeroQueryService.java` — source-qualified cover-only query/signing and bounded validation. Preserve accepted listing preparation and immutable jobs.
- `category/api/LibraryStoryDetailQueryApi.java`, `LibraryStoryDetail.java`; `category/application/query/LibraryStoryDetailQueryService.java`; `category/application/CategoryHomeStore.java` and its existing JDBC implementation — category/mapping/membership checks and bounded ordered recommendations, without new schema or CMS screens.
- `user/application/LibraryHomeTestQueryService.java`, `user/web/mobile/LibraryHomeTestController.java`, `LibraryHomeTestExceptionHandler.java` — reuse admission for the additional profile-restricted detail GET and response/error documentation. Preserve home behavior and constructors' test coverage.
- `app/mobile/lib/features/story_detail/domain/{entity/story_detail.dart,repository/story_detail_repository.dart,use_case/get_story_detail.dart}` — pure domain and `Either` contract.
- `app/mobile/lib/features/story_detail/data/{datasource/story_detail_datasource.dart,model/story_detail_model.dart,repository_impl/story_detail_repository_impl.dart}` — typed shared transport and generated DTOs extending entities.
- `app/mobile/lib/features/story_detail/presentation/{cubit/story_detail_cubit.dart,cubit/story_detail_state.dart,screen/story_detail_screen.dart,view/story_detail_view.dart,widgets/story_detail_hero.dart}` — Freezed attempt/identity outcomes, route-owned state, bounded image decoding and adaptive reference UI.
- `app/mobile/lib/core/network/story_detail_image_cache.dart` — bounded package-backed hero profile and disposal, without changes to home limits.
- `app/mobile/lib/app/{router,injection}.dart`, `app/mobile/lib/features/library/presentation/{view/library_view.dart,widgets/library_card.dart}` — registered ID/scalar route, category context and safe card navigation with retained Library ownership. Generated companions come from build_runner.
- Backend tests: `be/src/test/java/com/tellpal/v2/category/LibraryStoryDetailIntegrationTest.java`, `content/StoryDetailMetadataQueryServiceTest.java`, `asset/StoryDetailHeroQueryServiceTest.java`, `user/web/mobile/LibraryStoryDetailTestControllerTest.java`.
- Flutter tests: `app/mobile/test/story_detail/{story_detail_repository_test.dart,story_detail_cubit_test.dart,story_detail_navigation_test.dart,story_detail_view_test.dart,story_detail_artwork_test.dart}`.

## Verification and acceptance gates

Execute PostgreSQL/Flyway/API/module checks and redacted real-backend detail fixtures before mobile integration. Include both controlled audiences, ordinary/mapped languages, populated/absent credits/hero, withdrawn localization, invalid admission, correct curation beyond home preview, reordered responses, filtered CMS order and valid/empty recommendations. Real source bytes, actual URLs privately exercised and source/build/dataset/time identities must accompany evidence; never commit credentials or signed URLs.

Then run generation/analyze/full Flutter tests, backend verify and the [reference matrix](library-1-2-reference-cases.md) on available Android/iOS phone/tablet environments. Demonstrate actual back/scroll, nested return, no auto-playback, URL expiry/retry, socket/cache release and accessibility. Fixtures or mock calls alone cannot close detail EXT-CATALOG. Preserve separately recorded minimum/older-OS, physical-device and release gaps; no deployment or production access is included.
