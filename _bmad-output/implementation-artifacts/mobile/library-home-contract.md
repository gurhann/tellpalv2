# Library home catalog contract — Story 1.1

Status: proposed for Checkpoint 1; not a runtime acceptance record.
Date: 2026-10-02.
Updated: 2026-10-03 after the authorized preparation review corrections. The schema and commands below remain implementation proposals; runtime acceptance is pending.
Scope: the controlled-test reading Library home, not detail, search, pagination, production authentication or store entitlements.
Baseline: b372c60b53f828f2fcadff40eb1c212684ad3d51.

## Ownership and access

The user module verifies identity and selects the configured test audience. It calls the category public API; category composes content previews through content public APIs; asset owns cover preparation and delivery. Add only the user → category::api edge, record ADR-0019, and update the canonical architecture/module allowlist and project memory. Controllers validate/map requests; application services orchestrate. No cross-module entity imports, cycles or feature orchestration in shared. LibraryHomeQueryApi accepts/returns language strings across the user boundary and converts to LanguageCode inside category; do not add user→shared::domain. Update be/src/test/java/com/tellpal/v2/ApplicationModulesTest.java to expect the new category dependency.

GET /api/test/library?lang=tr is registered only under local/test profiles. Authentication uses UserResolutionApi and the existing verified Firebase identity mapping. A separate empty-by-default allowlist admits test UIDs; an empty-by-default subscriber UID set must be a subset of that allowlist, otherwise startup fails. An admitted UID outside the subscriber set has NON_SUBSCRIBER audience; an admitted subscriber UID has SUBSCRIBER audience. This mapping is a presentation fixture, never a genuine RevenueCat entitlement or permission to read protected media. No audience, premium or free-set selector comes from the client. UserResolutionApi.resolveOrCreateByIdToken may write app_users/user_profiles: complete identity resolution outside the category read-only transaction; do not wrap the whole user orchestration in read-only REQUIRED propagation.

Missing/invalid Bearer credentials return 401; a verified but unlisted UID returns 403. Missing, malformed or unsupported language returns 400. Supported values are tr/en/pt, normalized using the existing LanguageCode conventions. Success, including an empty catalog, returns 200. Errors use documented ProblemDetail without token/URL/internal-path contents. Unexpected failures remain server errors; they never become an empty 200.

The Library development entry accepts configuration through an ephemeral harness/environment, without a token entry UI, persistence or logging. It fails closed in release mode. Integration tests may substitute the existing local/test verifier, but the actual Firebase acceptance run must explicitly override tellpal.security.firebase.stub-tokens-enabled=false and prove the real verifier was selected. The base YAML environment variable alone does not override the literal true in application-local.yml. Production configuration remains untouched; the retained media PoC is not certified as a production application by this story.

## CMS home configuration

The backend/CMS companion includes an editor changing and saving Library category row order independently per presentation language. It is separate from the existing within-category content order. Changing Turkish row order must leave English/Portuguese row order and all within-category card order unchanged. Filtering hidden or empty categories preserves the relative order of remaining rows.

Proposed category_localizations home configuration:

| Field | Semantics |
| --- | --- |
| home_display_order | Nullable nonnegative integer; null excludes that presentation-language category from home. Unique per language among configured rows. |
| home_audience | ALL or NON_SUBSCRIBER; default ALL. Independent of the existing category premium flag. |
| home_content_language | Explicit content/curation language for a deliberately labeled different-language row. Null uses the selected presentation language. No substitution based on missing content. |

Use one server-configurable positive preview limit with an initial default of **5 eligible items per category**. This is a shared Library setting, applied after eligibility filtering. There is no category-localization limit field or per-category override; editors cannot change the limit through category configuration. Existing rows remain opted out until deliberately configured; verify that initialization choice against the actual CMS dataset before migration. Slug/ID order must not substitute for saved editorial order.

Authenticated admin GET/PUT /api/admin/categories/{categoryId}/localizations/{lang}/home reads or writes displayOrder, audience and contentLanguage. PUT validates the complete configuration and commits atomically. Null displayOrder removes home visibility without deleting curation. Missing category/localization is 404; invalid input is 400; duplicate language/order is 409. Keep existing admin JWT authorization and REST documentation conventions.

Add the CMS Library row-order editing/save surface using the existing category feature and shared primitives. Define the exact surface and bulk reorder/save contract before implementation: exchanging occupied positions must succeed atomically without temporarily hiding a row or exposing a partial permutation. Reject duplicate IDs/positions or stale edits with a recoverable response. This does not reorder category contents. Reload saved order from the backend and compare it with the mobile response; a save failure must not be shown as persisted success. Follow cms/AGENTS.md and its applicable UI/test skills when implementing; this preparation review does not introduce a new CMS layout.

Before implementing the mapping field, record its storage and admin representation against V7 curation constraints. For a Turkish presentation deliberately showing English content, select the same category's explicitly configured English curation lane; maintain the existing requirement that category and content localizations exist in the link language. Use the published Turkish name/description for presentation and eligible English item titles/artwork/access metadata. Do not insert English-only items into an ordinary Turkish lane or weaken curation validation. Missing/invalid mapping is a configuration error, never a reason to choose another language. CMS curation/configuration must make the different content language clear in the row label.

## Visibility and ordering

Only active STORY categories with a published selected-presentation-language localization and configured home order can appear. NON_SUBSCRIBER rows are omitted for subscribers. Resolve each row's explicit content/curation language: the selected language for an ordinary row, or the deliberately configured language for a clearly labeled different-language row. Language/test visibility, publication and audience eligibility apply to both presentation and content languages.

Resolve candidates in that language's existing curated display order. Include only active STORY content with an eligible published, processing-complete reading localization in that exact content language. Missing/unready localization is excluded; never choose English or another language automatically. A Turkish ordinary row with three eligible Turkish items returns those three even if additional English versions exist. Omit a row with no eligible items in its declared content language. Missing detail/start localization remains unavailable in the consuming stories.

Use ContentFreeAccessApi with the server-selected default free set in the explicit content language. isLocked = !isFree && audience != SUBSCRIBER. A listing lock flag never admits protected full media. Test audience is not candidate-tester permission; only the eligible published catalog is listed.

Deduplicate content IDs within each row without changing their first curated position. Filter eligibility before applying the shared preview limit, so excluded early candidates do not consume slots. Category row order is independent of card order. Return explicit item language for later detail/reading/listening routing while retaining the user's UI language; this story does not implement those destinations.

Queries use scalar, bounded/batched read projections. They must not hydrate the complete content/story-page graph, invoke PublicContentSummary/loadAssets or resolve full audio/ZIP packages. Do not mutate cached download URLs inside a read-only catalog transaction.

## Response v1

The response contains schemaVersion=1, requestedLanguage, audience, the effective shared previewLimit and rows. Each row contains categoryId, slug, name, nullable description, displayOrder, explicit contentLanguage and items. Each item contains contentId, externalKey, type=STORY, experienceType=READING, contentLanguage, title, nullable ageRange/durationMinutes, isFree, isLocked and covers. Item language matches the row's explicit content language. There is no automatic fallback flag or selection branch.

covers holds nullable phone/tablet references. Each reference contains assetId, kind, mimeType, url and expiresAt. Only verified image thumbnails are permitted; no raw object path, original story page, audio, archive, manifest or private credential is serialized. Optional metadata uses explicit nulls rather than fabricated values.

Image absence/failure retains card geometry, title and backend lock state with the defined artwork-error placeholder. Representative successful acceptance fixtures must contain real, decodable phone/tablet artwork, not only placeholders or asset records. Signed image expiry triggers at most one catalog refresh for the current request context and attempt; persistent failure stays a card placeholder without a loop or catalog empty/error outcome. Any resulting refresh gets a new catalog attempt identity; obsolete image completions cannot update the refreshed catalog.

## Cover preparation requirement

Current DefaultImageOptimizationAdapter only creates file plans, and the job executor can register those plans without producing thumbnail bytes. Processing COMPLETED or an asset record is therefore insufficient.

This slice must prepare real phone/tablet cover thumbnails before listing acceptance: obtain the localized reading cover from the owning content/asset APIs, decode and resize it, upload actual image bytes to the configured non-production prefix, verify stored size/decodability, then expose a ready reference. Generation is an explicit preparation command, never a side effect of GET /api/test/library. Existing detail/audio/ZIP behavior is outside this narrow repair. Never sign a fabricated/generated-only thumbnail path.

Use deterministic source/version-specific paths; changing the source must not overwrite a ready thumbnail used by another response. Failure leaves no ready marker. Repeating preparation for the same source is idempotent. Keep encoding consistent with the returned MIME type, preserve the 3:4 reference presentation and bound decoding/processing resources.

Use a separately retryable LISTING_COVER job, not DELIVERY readiness. Reuse the existing schedule/lease/retry lifecycle. Only a STORY localization with a positive current cover source is valid; no audio/page requirement is introduced. Keep publication status unchanged.

Add POST /api/admin/contents/{id}/localizations/{lang}/listing-cover-processing. ContentManagementService obtains the current localization coverMediaId/externalKey; the client cannot choose an arbitrary source asset. An existing ready source/profile returns the same revision; scheduled work returns202 with an operation identity. Missing localization/source is404/409; existing admin JWT and ProblemDetail conventions apply.

The initial listing-cover profile uses aspect-preserving, no-upscale fits within360×480px(phone) and600×800px(tablet), JPEG quality85, white compositing for PNG alpha, max source20MiB and max decoded40million pixels. Keep these environment-configurable with validation. A profile revision includes both sizes, quality and encoder policy. Read image dimensions before allocating; verify the declared MIME matches decoding. Reuse the existing ImageIO/Graphics2D approach, not an unavailable WebP encoder.

Persist source/profile identity in the job context. Profile/source changes reschedule explicitly; a worker publishes against its captured source/profile, never the latest mutable values. Deterministic environment-prefixed source-SHA/profile paths end in.jpg and return image/jpeg. Upload both variants and verify stored MIME, size, SHA256, dimensions and successful decode before atomically publishing one listing_cover_revisions record. Partial failure cannot publish readiness. Before publishing the manifest and completing this job in one transaction, verify the worker still owns the unexpired captured lease attempt/source/profile. Extend both new-kind completion and failure commands with captured attempt, lease, source and profile identity. Validate ownership and mutate state atomically in the persistence transaction; a stale or superseded worker cannot publish, complete or fail a newer attempt. Reject stale callbacks without changing the current job or ready manifest. The executor must not turn a rejected stale completion into an unguarded FailAssetProcessingCommand against the current target/kind. Test worker A lease expiry followed by retry B: A late completion, A late failure and rejection-to-catch failure must all leave B unchanged. Keep this new-kind guard scoped to LISTING_COVER; existing DELIVERY behavior remains outside this slice. Old profiles/source references never satisfy current lookup; existing generated-only registry records are rejected. Real read delivery signs verified image IDs without writing cached URL state.

### Preparation Code Map

Java paths are relative to be/src/main/java/com/tellpal/v2/:
- asset/api/AssetProcessingKind.java and AssetProcessingCommands.java; asset/domain/AssetProcessing.java — LISTING_COVER and captured profile/source context; preserve constructors/defaults for existing job kinds.
- be/src/main/resources/db/migration/V34__listing_cover_revisions.sql — job kind/profile persistence and verified two-image manifest constraints.
- content/application/ContentManagementService.java and content/web/admin/ContentAdminController.java — authorized source-bound preparation operation.
- asset/infrastructure/media/ListingCoverRenditionProcessor.java — bounded ImageIO generation; reuse PageImageRenditionProcessor.java as read-only encoder evidence.
- asset/infrastructure/media/ListingCoverProcessingExecutor.java and RegisteringAssetProcessingJobExecutor.java — narrow new-kind dispatch, upload/integrity checks; leave existing detail/audio/ZIP outputs unchanged.
- asset/infrastructure/storage/AssetProcessingPathBuilder.java; asset/infrastructure/persistence/ListingCoverRevisionStore.java — immutable paths and atomic ready manifests.
- asset/application/AssetProcessingService.java — source/profile-aware rescheduling/idempotency; asset/application/LibraryCoverQueryService.java and asset/api/LibraryCoverQueryApi.java — query-only ready thumbnail resolution/signing.
- asset/application/LibraryCoverPreparationService.java — schedule/query orchestration only; storage/encoding stay in infrastructure adapters.
- be/src/main/resources/application.yml — environment-driven listing profile properties; no hardcoded infrastructure/credential values.

Processor/executor tests cover no-upscale and distinct profiles, alpha, invalid/oversized input, MIME/checksum/dimension mismatches, second upload failure, retries and changed sources/profiles. PostgreSQL tests prove atomic ready publication, kind constraints and stale/generated-only rejection, including late completion/failure callbacks after another attempt acquires the lease.

## Client and reference behavior

Build feature-first data/domain/presentation with pure Dart domain, Either repository results, BLoC/Cubit + immutable Freezed state, auto_route and get_it/injectable. One app composition root owns transport/router configuration; route providers own and close their feature Cubits. Key every request by immutable language/access context plus a distinct attempt identity. Invalidate obsolete attempts on refresh, retry, timeout, context change or disposal. Reject obsolete success, error, timeout and artwork completions before changing any catalog/artwork/loading/error state; context equality alone is insufficient for a retry in the same context. Cover URLs/caches never imply playback permission. The dev composition captures sanitized framework/uncaught errors, request timings and build/device context for feature diagnosis; it excludes tokens, signed URLs and response bodies. Retained test evidence includes scroll/image memory observations; production crash reporting and backend flow-event integration are not certified here.

Keep the existing main.dart media PoC entry and its integration tests. Add main_library_dev.dart and only the shell/composition needed for Library. Library is enabled and selected; reselection retains its current stack and scroll position. Sleep/Profile keep their reference unselected appearance and remain disabled; tapping them does not change the active tab. Cards, All-items, search and applicable premium banner actions retain their reference appearance with disabled destination actions and accessibility semantics. No input, navigation, detail, search or paywall flow starts from those controls. Later consuming stories extend the same shell and introduce their independent tab stacks.

Before UI implementation, establish matched reference cases for disabled shell controls, the artwork-error placeholder and long localized titles at the agreed phone/tablet and normal/enlarged text sizes; reuse those cases at acceptance. Record expected names, roles, selected/disabled semantics and tap outcomes.

References: app/old/docs/design-system/{SCREENS,DESIGN,TABLET}.md; evidence/emulator-home2.png and evidence/tablet-emulated-home-loaded.png. Prefer the legacy Lexend/fonts and navigation/search/lock icons rather than approximations. Import only required assets from the existing tellpal/assets sibling repository. Covers remain backend data.

Tokens: canvas #F5F7F9, primary #5462DE, ink #2C323F, muted #445064, subtle #6A81A6, placeholder #E6ECF4. Library gutters/item spacing 16dp; vertical section gap 24dp; clip radius16/frame20; cover aspect3:4. Rail/card height follows the reference screenHeight×0.230+16 rule, width=height×0.75; tablet remains horizontal rather than a grid. Greeting22/w600, section20/w600, description16/w500. Banner42dp/radius8 with #1F5EFF→#6420F6; search appearance40dp. Interactive wrappers remain at least48dp. Omit parent mode.

Use distinct localized loading, recoverable error+retry and empty states. Semantics include names/roles/selection and decorative-image exclusion; inactive future capabilities are not exposed as functioning buttons. Text must wrap or use defined bounded overflow without clipping controls. Do not silently change the visual design to pass checks.


## Waiting limits and retry isolation

Before integration acceptance, measure, choose and record finite catalog and artwork waiting limits in this contract and the tested configuration/evidence. Numeric values are pending implementation measurements; accepting indefinite loading is prohibited. Record the build, device/network context, effective values and deadline outcomes.

- A catalog deadline leaves loading and produces a recoverable error distinct from successful empty content. Retry starts a new attempt using the current language/access context.
- An artwork deadline or decode/download failure produces the defined card-level error placeholder while preserving geometry, title, lock and the remaining successful catalog. Artwork retry uses a new current-attempt identity and respects the bounded expiry-refresh rule.
- Deadline expiry invalidates that attempt. Results arriving after timeout, retry, refresh, context change or disposal cannot change the current outcome, even if language/audience are identical. Image completions must also match the current catalog generation, content/asset identity and image attempt.
- Verify deliberately reversed success/error/artwork completions across both context changes and same-context retries; obsolete timeout callbacks must not affect the replacement attempt. Cancellation is useful for resource bounds but does not replace identity checks.

## Acceptance evidence

Exercise a disposable local PostgreSQL/Flyway backend and non-production storage prefix, using administered fixtures rather than production data mutation. Document admin configuration readback and actual HTTP responses, anonymizing UIDs and removing credentials and signed URLs from retained evidence.

Required dataset: saved CMS row order different from slug/ID order; different Turkish/English row orders; more eligible items than the shared limit of five; early unready candidates; empty, inactive and unpublished categories; three eligible Turkish items plus two English-only versions; an ordinary row with no eligible Turkish items; a deliberately labeled English-content row in Turkish UI; free/locked content; a non-subscriber-only category; TR/EN/PT; actual artwork in each declared content language. Verify 401/403/400 and zero unauthorized thumbnail preparation calls.

Exercise CMS edit/save/reload and a fresh backend/mobile fetch of the same baseline. Changing only Turkish row order must preserve English order and all card order; filtering hidden/empty rows preserves remaining relative order. Verify the atomic occupied-position reorder and recoverable save-error outcomes. Run authorized non-subscriber and active-subscriber contexts separately against the same dataset.

Record contract version, backend build, timestamp, redacted test-data identities, selected/content language, authorized audience and expected ordered category/preview identities with the real request/response and thumbnail-transfer evidence. Compare mobile with that same dataset. Relevant CMS changes require refreshing the baseline and repeating the affected comparison. Record the named assignee, companion work, fixture/evidence paths and Story 1.1-only closure in EXT-CATALOG; other consumers remain open.

Frontend unit/widget tests cover the state matrix, catalog/artwork timeout and current-context retry, same-context attempt replacement, deliberately reordered old success/error/artwork/timeout results, disposal, expected ordered rows/items and lock badges, Library reselection/back, disabled-control semantics/taps and decoded-image bounds. Existing PoC tests remain valid. Capture matched411×914dp and800×1280dp phone/tablet views; test TR/EN/PT at text scale1.0 and1.3, safe areas,48dp targets, selection semantics and contrast over actual artwork. Measure normal text contrast≥4.5:1 and large text≥3:1 under the W3C definition: https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html. Record any failure and stop acceptance instead of redesigning silently.

Compile Android with minimumAPI24 and iOS Simulator with deployment target15. Run backend-to-Library and real-image decoding on available Android emulators and iOS Simulators, including phone/tablet and the available older/current profiles. Record unavailable profiles as an evidence gap, never as a pass. Capture platform/build/network/cache context, request timings and a repeated-scroll/image memory observation without secrets. Reader/player numeric budgets and signed-upgrade/store evidence remain their separately recorded gates.

Story1.1 closes only the home-list part of EXT-CATALOG when actual results pass. Detail, category pagination, search, production entitlements and release remain open.


## Implemented configuration and measured waiting limits — 2026-10-04

Contract version 1 implementation uses `/api/admin/categories/home?lang=tr|en|pt` for complete revision-checked atomic configuration and `/categories/home` for CMS editing. Existing rows remain opted out. `TELLPAL_LIBRARY_PREVIEW_LIMIT` defaults to 5; there is no category-level override. Explicit content-language mapping requires an existing category localization/curation lane; the selected and mapped lanes must be active-language/published eligible at query time.

The controlled Flutter configuration defaults to **15 seconds catalog** and **10 seconds artwork**. Real Firebase fixture measurements on the isolated local backend before CMS reorder were 2019 ms TR, 967 ms EN, 1664 ms PT and 463–2332 ms per artwork transfer, with a maximum listing JPEG of 81,255 bytes. These thresholds are above measured fixture timings and ensure finite recovery; they are not production performance guarantees. Injectable shorter thresholds verify expiration without making the suite wait 15 seconds. Catalog and artwork retries advance independent attempt identities; context changes and disposal invalidate obsolete callbacks. Automatic expired-cover catalog refresh is limited to one per catalog recovery chain.

Image decode widths are bounded to 360 phone / 600 tablet; global decoded-image cache is at most 48 entries / 32 MiB and account-scoped disk cache retains at most 80 entries for 10 minutes. Native repeated-scroll observations and actual deadline tests are recorded separately in `library-1-1-evidence.md`. A source unavailable in its declared fixture language is a gap, never silently substituted.

## Iteration 3 transport lifecycle — 2026-10-05

The image adapter retains the public CacheManager storage, FileService and FileSystem seams; vendor sources are unchanged. For each immutable context/catalog/image/owner transfer key, its native HTTP client is owned by that transfer. Relative to `artworkWait`, headers and body inactivity are each bounded to **0.4 × artworkWait**, and total header-plus-body lifetime is **0.8 × artworkWait**, including continuously trickling bodies. At the default 10-second artwork wait these are **4 seconds / 4 seconds / 8 seconds**. Admission permits at most **10** package operations with at most **80** queued requests; queue waiting is at most **0.2 × artworkWait** (**2 seconds** by default). Queue overflow/expiry is a recoverable card error. Explicit retry cancels the previous transfer before admitting its independent provider/HTTP attempt; current-attempt guards remain authoritative.

Cache closure rejects new work, cancels owned clients and queued admission, consumes each admitted package stream through download/file/metadata publication, and drains promotion and admitted storage work before deleting complete and partial files and closing the repository. Per-operation file tracking is released after publication rather than retained as an unbounded transfer history. The existing 80-object/10-minute disk and 48-image/32-MiB decoded-cache bounds remain in effect. A current fresh decoded transfer may renew an expired shared immutable entry's metadata without overwriting its file; the next owner reuses that renewed entry without HTTP. Promotion remains coalesced and bound to actual delivered provenance; a warm decode cannot promote unrelated bytes.

Executed deterministic real CacheManager/loopback coverage exercises stalled headers, stalled body with observed bytes, continuously trickling body with observed progress, ten occupied slots and current retry, a second HTTP request before an old delayed response, header/body/trickle cancellation plus queued shutdown, delayed file creation and delayed package metadata publication, and no post-close file/metadata/request admission. Engine-decode and widget regressions preserve active reader bytes, warm-cache reuse, short/landscape long-title title/lock semantics and the 48dp retry target. Full source-level verification is recorded in `library-1-1-iteration3-build-verification.json`; real backend/CMS/native acceptance and consumer closure remain separate root verification gates.


### Iteration 4 native catalog lifetime (2026-10-06)

The presentation catalogWait remains 15 seconds. The app-owned AuthenticatedTransport admits at most eight active requests, with independent native clients owned by those attempts and no unbounded native queue. Connection/header and body-inactivity waits are 0.4 × catalogWait (6 seconds); the absolute request/response deadline is 0.8 × catalogWait (12 seconds). Abort and force-close release an attempt's native sockets; body subscriptions are cancelled/drained before the attempt leaves ownership. Success and discarded non-200 bodies share the 1 MiB transport bound. App disposal rejects new admission and aborts all current attempts. The shorter transport limits leave time for the existing recoverable Cubit outcome and current-context retry. Fixed timeout diagnostics contain no URLs, response bodies or credentials.

Expired immutable artwork metadata is renewed only when its bytes exactly equal the actually delivered, successfully decoded fresh bytes. Different/corrupt expired bytes are recovered through metadata ownership changes to the validated delivered file, retaining the prior reader's unchanged file within the existing bounded transfer record. Partial metadata publication restores both original records, including CacheStore's memory view; explicit delivered-file ownership prevents orphan files even when rollback/storage lookup fails. Retired files have a ten-minute lifetime and bounded ownership, and disposal drains all publication/storage before cleanup. Image transport, decoded cache and disk-cache limits remain unchanged.

### Iteration 5 cleanup ownership (2026-10-06)

Partial-file ownership is released only after successful deletion or transfer to retained metadata. At most 80 distinct owned partial paths (including pending creations) are admitted; a full set gets one bounded retry pass over failed paths before rejecting new file creation. Repeated references to the same path share its existing owner. Persistent failures keep their paths and prevent unlimited new storage admission.

At most 80 retirement records, including reserved replacements, are admitted. At capacity, one oldest retirement is attempted once; failure retains its explicit file owner and rejects new transfer admission without opening another native request. Retirement timer failures are handled and leave ownership for subsequent capacity/close cleanup. Publication cleanup ownership is also bounded at 80 records. All attempts remain finite; persistent failure never runs an immediate retry loop.

Close rejects admission, cancels/drains transfer and promotion work, drains metadata/storage publication, attempts file cleanup and always disposes the repository. The original cleanup error is propagated. Enumeration/path-resolution failure retains persisted metadata ownership rather than deleting undiscovered files. Concurrent close calls coalesce; a subsequent close retries retained local files without accessing the disposed metadata/FileSystem delegate. Persistent deletion is reported as a failure, never as successful deletion. These failure-path ownership limits supplement the unchanged 80-object/10-minute package cache and 48-image/32-MiB decode bounds; current-source full and native acceptance are recorded separately.


### Iteration 6 package failure ownership (2026-10-06)

All public byte/stream publications, including initial immutable promotion, use an explicit owner before the configured FileSystem resolves a new physical path. Pending path reservations, failed partial files and failed metadata eviction files share a finite 80-path cleanup admission bound. Physical ownership is released only after successful deletion or verified metadata transfer; persistent failure retains ownership and rejects further admission after one bounded cleanup pass. Cache admission and its native transport limits are unchanged.

The app uses the public CacheManager.custom store seam with LibraryCacheStore; flutter_cache_manager vendor sources remain unchanged. Its repository-backed lookup bridge directly settles admitted get/file-resolution errors instead of entering the package's onError-less lookup completer. Package fire-and-forget metadata publications are drained, and their original errors reach the owning consumer before FileInfo publication. Independently retried attempts retain the existing context/attempt/provenance guards. The repository opens once and still drains admitted operations before guaranteed disposal.

Package removal, age/capacity cleanup and cache closure resolve physical handles through the configured FileSystem. Metadata removal transfers ownership to an explicit physical handle before deletion; failed deletion remains bounded and retryable. Timer failures are handled. Closure still preserves persisted ownership after failed enumeration, propagates the original cleanup error and closes the repository; subsequent close retries use retained local handles without accessing a disposed repository or FileSystem delegate. Active-reader bytes, validated expired-file recovery, approved goldens and the PoC remain unchanged.

Initial iteration6 source checkpoint (historical before the later-lookup correction): focused verification passed 39 actual CacheManager/transport/promotion/disposal regressions (29 retained and 10 new package failure cases), Flutter analysis with no issues, and 11 PostgreSQL integration cases including the three new admin EN/current-source, unchanged PROCESSING conflict and eligible V32 upgrade cases. Full source/native acceptance and fresh current build identities remain root verification gates; this checkpoint does not relabel historical evidence or declare Story 1.1 accepted.


Final iteration6 checkpoint: the store retains the original admitted lookup failure by operation owner across CacheManager fallback, rejecting a later fallback lookup before any HTTP request. The adapter releases that record only after delivering the error. Four actual package nth-lookup cases (#1–#4) prove same-error identity, zero HTTP/bytes, independent retry and finite close. All43 focused package regressions,109 full Flutter tests and371 backend tests plus1 documentationIT passed. Corrected-source actual CMS/HTTP and all9 native acceptance runs passed with current source/direct-binary seals; the earlier39-case checkpoint and initial native runs remain historical. Story Step4/human acceptance remains pending.
