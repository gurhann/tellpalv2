# Story1.1 Frozen Matrix Test Audit

Audit date:2026-10-05, review iteration3. This audit maps the unchanged13-row frozen matrix and ten acceptance criteria to executed passing tests of the corrected source. It does not declare human checkpoint acceptance. Original2026-10-04 results are historical in the verification inventory.

## Executed verification baseline

- Backend full verify:364 tests plus one documentation IT, no failures/errors/skips; current build SHA and actual real-Firebase/GCS catalog are recorded in [HTTP evidence](library-1-1-http-evidence.json).
- CMS:89 files/371 tests, production build, three affected Home visual/interaction checks and two category-toolbar captures pass. The real backend reorder/conflict test and audience/content-language selections passed in both server audience contexts; final fixture restored. No unrelated snapshot was refreshed. Historical pre-existing contents1–13-pixel raster differences remain an explicit unrelated gap.
- Flutter generation passed; current analyze reports no issues. Full unfiltered machine run passed61 visible tests (including four retained PoC tests,12 matched viewport/language/text-scale cases, four new disposal regressions and the corrected refresh/provider/resize/DTO cases), zero failures or skips. Hidden suite-loading events are not counted as tests.
- Native corrected-source matrices: Android API30 phone NON_SUBSCRIBER and SUBSCRIBER, API36 tablet NON_SUBSCRIBER, iOS27 phone and iPad NON_SUBSCRIBER each passed all six TR/EN/PT ×1.0/1.3 cases. Every case independently compares fresh real HTTP, actual decoded Firebase JPEGs, ordered identities/languages/locks, disabled controls/reselection/back and five scroll cycles. Actual Android app bounds are411×914dp and800×1280dp; iOS natural bounds402×874dp and1032×1376dp are recorded separately from matched widget references.
- Android/iOS fault runs passed:15s catalog and actual4s header deadlines (UI artwork fallback10s; body inactivity4s, total transfer8s, admission2s), independent new transfer before old completion, corrupt-cover isolation/retry, forced expired-cover403 then failed catalog renewal503 retaining catalog/geometry, and explicit renewal retry recovery. Screenshots wait for actual rendered RawImage bitmaps. The proxy uses real backend/GCS JPEGs; provider expiry itself was not induced.
- Current Android/iOS exact main_library_dev.dart entry smokes passed. The independent main.dart PoC source/dependencies are unchanged; their iteration2 native evidence is reused with matching current source hashes, alongside four current passing unit tests. The unconfigured development entry remains closed; the PoC opens without entering credentials. Protected playback was not exercised. Minimum Android API24 / iOS15 compile settings and unavailable minimum/older runtimes/physical performance remain explicit in [platform evidence](library-1-1-platform-builds.json).

Current runner events, named methods, source hashes, timings/cache maxima and remaining gaps are in [the verification inventory](library-1-1-verification-audit.json) and [nonnative report](library-1-1-loop3-nonnative.json).

## Named test/source register


All paths are repository-relative from the project root. The links resolve relative to this audit document.

| ID | Actual registered test name | Source and executed proof |
| --- | --- | --- |
| B1 | `savedIndependentOrdersFilterBeforeFiveAndNeverPadTurkishWithEnglish` | [LibraryHomeIntegrationTest.java](../../../be/src/test/java/com/tellpal/v2/category/LibraryHomeIntegrationTest.java); Current Surefire six-case suite passed. |
| B2 | `staleDuplicateAndInvalidMappingWritesAreAtomicAndExistingRowsStartOptedOut`; `anAlreadyLoadedJpaLocalizationEditCannotOverwriteAtomicHomeConfiguration` | Same LibraryHomeIntegrationTest source and passing Surefire report. |
| B3 | `realBytePreparationAndLateCallbacksPreserveTheReplacementAndReadyManifest` | Same LibraryHomeIntegrationTest source; passing PostgreSQL/byte-decoding/lease-replacement assertions. |
| B4 | `missingTokenIsProblemDetailBeforeAnyCatalogQuery`; `invalidTokenIsProblemDetailAndNeverQueriesCatalog`; `verifiedUnlistedIdentityIsProblemDetailAndNeverQueriesCatalog`; `unavailableLanguageIsProblemDetailAfterAuthorizedResolution` | [LibraryHomeTestControllerTest.java](../../../be/src/test/java/com/tellpal/v2/user/web/mobile/LibraryHomeTestControllerTest.java); all four passing Surefire cases. |
| B5 | `rejectsMissingInvalidAndUnlistedIdentityBeforeAnyCatalogCall`; `selectsAudienceOnlyAfterVerifiedIdentityAndRejectsInvalidLanguage`; `subscriberFixturesMustBeAnAllowlistSubset` | [LibraryHomeTestQueryServiceTest.java](../../../be/src/test/java/com/tellpal/v2/user/application/LibraryHomeTestQueryServiceTest.java); all three passing Surefire cases. |
| B6 | `rejectedCompletionCatchFailureStillUsesExpiredOwnershipAndCannotFailReplacement`; `explicitStalePublicationDoesNotEnterFailureCallback` | [ListingCoverProcessingExecutorTest.java](../../../be/src/test/java/com/tellpal/v2/asset/infrastructure/media/ListingCoverProcessingExecutorTest.java); five-case suite passed. |
| B7 | `secondUploadFailureNeverPublishesHalfReadyManifestAndCarriesCapturedAttempt`; `storedMimeAndChecksumMismatchRejectReadyPublication`; `generatedOnlyRegistryMetadataNeverMakesArtworkReady` | Same ListingCoverProcessingExecutorTest source and passing suite. |
| B8 | `fitsBothBoundsWithoutUpscalingAndCompositesAlphaWhite`; `rejectsDeclaredMimeMismatchAndExcessPixelsBeforeDecode`; `sourceProfilesAreCapturedAndRoundTrip` | [ListingCoverRenditionProcessorTest.java](../../../be/src/test/java/com/tellpal/v2/asset/infrastructure/media/ListingCoverRenditionProcessorTest.java); three passing Surefire cases. |
| C1 | `Library save failure keeps draft, retry/reload persists Turkish and leaves English order independent`; `Library home order visual - laptop`; `Library home order visual - desktop` | [category-home.visual.spec.ts](../../../cms/e2e/visual/category-home.visual.spec.ts); all three individually passed in the current affected-scope runner log; explicit font readiness and reviewed new Home baselines. |
| C2 | `real administered fixture saves Turkish row order independently and survives reload` | [library-real.spec.ts](../../../cms/e2e/library-real.spec.ts); [actual CMS evidence](library-1-1-cms-real.json) records `englishUnchanged`, `realConflictRecovery` and `cardOrdersUnchanged` all true. Required private harness variables were supplied for this run; this was not the skip branch. |
| F1 | `same-context retry rejects an earlier success and error in reversed order` | [library_cubit_test.dart](../../../app/mobile/test/library/library_cubit_test.dart); successful named machine-output `testDone`, no skip; also current full 61-test run. |
| F2 | `language and access context change reject obsolete responses and close rejects callbacks` | Same Cubit source, individually successful current machine-output run and current full 61-test run. |
| F3 | `deadline leaves loading, retry has new attempt and expired completion is ignored` | Same Cubit source, individually successful current machine-output run and current full 61-test run. |
| F4 | `expired signed artwork allows only one automatic refresh per current catalog` | Same Cubit source, individually successful current machine-output run and current full 61-test run. This is controlled unit coverage, not provider-side expiry. |
| F5 | `artwork attempt rejects old success, failure and timeout across retry, context and disposal` | Same Cubit source, individually successful current machine-output run and current full 61-test run. |
| W1 | `long titles, missing artwork and disabled semantics {tr,en,pt} Size({411.0, 914.0},{800.0, 1280.0}) scale {1.0,1.3}` — 12 registered combinations | [library_view_test.dart](../../../app/mobile/test/library/library_view_test.dart); all 12 passed with literal [pre-UI reference titles](library-1-1-reference-cases.md), fixture metadata, goldens, no layout exception, semantic/control checks and scroll retention. |
| W2 | `loading, empty, failure and retry remain distinct and localized` | Same widget source; passed in current full 61-test run. |
| N1 | `real Library matrix matches final CMS fixture, disabled controls retain scroll and cache remains bounded` | [library_live_test.dart](../../../app/mobile/integration_test/library_live_test.dart); passed [phone](library-1-1-native/android-phone/evidence.json), [subscriber phone](library-1-1-native/android-phone-subscriber/evidence.json), [Android tablet](library-1-1-native/android-tablet/evidence.json), [iOS phone](library-1-1-native/ios-phone/evidence.json) and [iOS tablet](library-1-1-native/ios-tablet/evidence.json) native runs. |
| N2 | `actual native transfers enforce 15s catalog and 4s artwork headers with current-attempt retry` | [library_deadline_live_test.dart](../../../app/mobile/integration_test/library_deadline_live_test.dart); passed [Android](library-1-1-native/android-phone-deadlines/evidence.json) and [iOS](library-1-1-native/ios-phone-deadlines/evidence.json) deadline/corrupt evidence. The named test includes its added one-cover corruption/recovery branch. |


Additional registered passing iteration-2 coverage:

- B9: `eligibilityFilteringContinuesPastAFullHundredCandidateBatchBeforeTheLimit` in the current six-case PostgreSQL LibraryHomeIntegrationTest:100 ineligible ordered candidates then six eligible candidates yields the correct first five.
- C3: `keeps draft and original expectedRevision across background GET failure`, `keeps stale draft revision on changed-server background data and failed explicit Reload`, and `successful Save pins all edited fields; successful Reload and language change replace snapshot` in [home.test.tsx](../../../cms/src/app/routes/categories/home.test.tsx). Current371-test run passes all three. Actual [NON_SUBSCRIBER](library-1-1-cms-selections-non_subscriber.json) and [SUBSCRIBER](library-1-1-cms-selections-subscriber.json) save/reload/catalog checks also pass; the final fixture is restored.
- F6: four named `background artwork refresh {network,timeout,empty,invalid} retains the exact successful catalog` tests in [library_cubit_test.dart](../../../app/mobile/test/library/library_cubit_test.dart).
- F7: `manual expired-cover retry remains recoverable after automatic failure and is single-flight` and three `obsolete automatic refresh cannot replace {retry,context,disposal}` tests in the same Cubit source.
- F8: `same-asset retry starts a separate provider transfer before old success completes`, `same-asset retry rejects old transfer error after replacement loaded`, and both `resize {411.0 to 800.0,800.0 to 411.0} replaces asset owner and keeps timeout/retry current` cases in [library_artwork_test.dart](../../../app/mobile/test/library/library_artwork_test.dart). Exact spellings are retained in the machine-run inventory.
- F9: real HTTP DTO requested-language, item-language, duplicate-category and preview-overflow cases each assert invalidResponse and actual Cubit error in [library_repository_test.dart](../../../app/mobile/test/library/library_repository_test.dart).
- D1–D4: `actual scheduled CacheStore cleanup after disposal never calls closed storage`; `cleanup query admitted before disposal completes safely; late callbacks cannot reach closed storage`; `disposal drains admitted read and write, declines new work and coalesces close`; `admitted storage errors propagate and still release the close drain` in [library_cache_disposal_test.dart](../../../app/mobile/test/library/library_cache_disposal_test.dart). These exercise the package's real10s scheduled cleanup under fake time and draining storage admission; all four passed in the full61-test run.

- D5–D8: four real CacheManager/engine-decode promotion tests in [library_cache_promotion_test.dart](../../../app/mobile/test/library/library_cache_promotion_test.dart): concurrent shared-file promotion, warm-hit provenance, corrupt-warm retry eviction and promotion/metadata close draining. Exact registered passing names are in the current machine inventory.

## Frozen matrix row mapping

| Row | Approved input/state | Executed passing coverage | Observed result |
| --- | --- | --- | --- |
|1|Eligible configured rows|B1,B9,N1|Final NON_SUBSCRIBER TR[3,2,1,4], EN/PT[1,2,4,3]; correct identities/languages/locks and first five after filtering;100 earlier ineligible candidates cannot hide the row.|
|2|Missing/unready ordinary selected-language localization|B1,B9,N1|Unready/inactive/draft/missing-language items and empty rows omitted; surviving row/card order preserved.|
|3|Three Turkish plus two English-only versions|B1,N1|Ordinary TR category2 remains exactly[2,3,4]; no language padding.|
|4|Deliberately labeled English row in Turkish UI|B1,N1,C3|Category3 explicitly maps to English and returns[9,10] with English metadata while UI remains TR; resetting mapping removes that ordinary missing-TR row and restoring explicitEN returns it.|
|5|CMS changes only Turkish row order|B1,B2,C1,C2,C3,N1|Actual occupied swap/save/reload yields TR[3,2,1,4]; English/card orders preserved. Actual409 retains draft and original revision; explicit successful Reload recovers. Background error/revision and failed Reload unit regressions pass.|
|6|Subscriber test UID|B1,B5,N1,C3|Same real identity with server SUBSCRIBER fixture returns TR[3,2,1], EN/PT[1,2,3], all unlocked; NON_SUBSCRIBER row4 omitted. Actual audience-selection roundtrips restore the baseline.|
|7|Missing/invalid/unlisted token; bad language|B4,B5|Current-jar real401/403/400 ProblemDetail with no rows; controller/service regressions verify no unauthorized query.|
|8|Loading; empty200; network failure|W2,F2,F9|Current full run proves distinct localized loading/empty/error/retry and invalid DTO handling.|
|9|Catalog exceeds recorded limit|F3,N2|16.5s actual proxy delay yields recoverable timeout at 15015ms Android /15010ms iOS; current retry succeeds and late catalog completion cannot replace it.|
|10|Artwork absent/corrupt/beyond limit|W1,F5,F6,F7,F8,N2|20s actual image delay yields error at 4091ms Android /4089ms iOS (actual native header deadline). Title/lock/geometry/other items persist. One corrupt JPEG is isolated; actual retry starts a new request and decodes it. Forced403→503 retains catalog; explicit retry recovers.|
|11|Old callbacks after context change or same-context retry|F1,F2,F3,F5,F7,F8,D1–D8,N2|Old success/error/timeout rejected after retry/context/disposal and both responsive resize directions. Fresh native retry begins after 170ms Android /169ms iOS before any delayed old completion. Scheduled cleanup cannot reach closed SQL; pending reads/writes drain first.|
|12|Library reselected; disabled/deferred controls tapped|W1,N1|Stack/scroll retained; disabled role/name/selected semantics and48dp sizes verified; no deferred input/navigation/paywall action. Native subscriber conditional banner absent.|
|13|Stale workerA after retryB|B3,B6,B7|PostgreSQL/byte tests and executor catch cases reject stale completion/failure; B and ready manifest survive.|

Every row has actually executed passing coverage; skipped or merely defined tests are not used. Finite cache bounds remain48 decoded entries/32MiB and80 disk objects/10min. These are debug emulated observations, not physical-device frame-time claims.

## Acceptance criterion audit

|Criterion|Current executed proof|Result|
|---|---|---|
|AC1 catalog/order/eligibility/limit|B1,B9,N1, real HTTP/JPEG inventory|PASS|
|AC2 independent Turkish CMS save/reload|B1,B2,C1–C3,N1|PASS|
|AC3 selected-language-only/explicit English|B1,C3,N1|PASS|
|AC4 server audience/locks|B1,B4,B5,C3, real audience HTTP and native subscriber N1|PASS|
|AC5 distinct outcomes/retained artwork geometry|W1,W2,F5–F9,N2|PASS|
|AC6 finite waits/current attempts/obsolete disposal|F1–F8,D1–D8, Android/iOS N2|PASS|
|AC7 reselection/back/deferred semantics|W1,N1|PASS|
|AC8 matched references/languages/scales/contrast/48dp|12 unchanged matched W1 cases and five native six-case matrices|PASS on recorded emulated matrix; documented unavailable profiles/physical gaps remain|
|AC9 real cover bytes/stale ownership|B3,B6–B8, actual34 JPEGs|PASS|
|AC10 preserved PoC|Four unchanged unit tests and unchanged-source iteration2 Android/iOS main.dart smokes with matching current hashes|PASS; protected playback not exercised|

The required home-list EXT-CATALOG dependency slice is verified on contractv1. Later detail/pagination/search/entitlement/release consumers remain open. Story status advances only to Step4 review and the required human checkpoint.

## Iteration3 transport, cache and constrained-layout coverage

The current61-test machine inventory records every registered case individually; all have success=true and no skips. New source files and runner logs are hashed in the current reports. Header/body inactivity/absolute transfer/admission limits are4/4/8/2 seconds at artworkWait10s; ten active and eighty queued operations remain bounded. Native delayed-header tests validate the4s limit; real loopback transport tests validate stalled bodies, continuous trickle, full-slot retry progress and cancel/drain-before-cleanup. Twelve portrait goldens remain unchanged.

- `fresh current decode renews expired shared metadata without rewriting active readers; next owner reuses it`
- `landscape long-title artwork error fits tr at enlarged text`
- `landscape long-title artwork error fits en at enlarged text`
- `landscape long-title artwork error fits pt at enlarged text`
- `real CacheManager bounds headers and removes partial files before close`
- `real CacheManager bounds body and removes partial files before close`
- `real CacheManager bounds trickle and removes partial files before close`
- `short enlarged long-title error preserves lock and 48dp retry without overflow`
- `all ten occupied package slots release; current retry fetches independently`
- `explicit retry sends request before the old delayed headers and rejects its success`
- `close cancels headers plus queued work; no late file or metadata publication`
- `close cancels body plus queued work; no late file or metadata publication`
- `close cancels trickle plus queued work; no late file or metadata publication`

Backend current Spring profile and same-source two-profile coverage comprises five LibraryHomeProfileContextTest cases and six LibraryHomeIntegrationTest cases, individually listed in the current inventory. These cover production/default bean and mapping exclusion, local/test exposure, distinct immutable revisions and current catalog covers after P1→P2. The13 approved matrix rows and ten ACs above retain their unchanged expectations; these cases supplement rows10–11 and AC6/9.
