# Epic 1 Context: Discover content and read illustrated stories

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Enable authorized test users to discover real-backend content and complete illustrated stories with reference visuals and durable progress. Deliver listing, detail and reading separately. Scope approval is not integration acceptance.

## Stories

- Story 1.1: Discover eligible Library content
- Story 1.2: Open selected story details
- Story 1.3: Start story reading
- Story 1.4: Navigate pages manually and automatically
- Story 1.5: Complete stories with durable records
- Story 1.6: Control narration
- Story 1.7: Preserve interrupted reading sessions
- Story 1.8: Browse complete categories
- Story 1.9: Search grouped content
- Story 1.10: Read in enabled languages
- Story 1.11: Renew access within the active revision
- Story 1.12: Sustain long and repeated reading sessions

## Requirements & Constraints

- Library uses CMS category order, contracted preview selection/within-category order, bounded previews, audience eligibility and backend locks; verify ordered content identities and omit empty categories. Verify non-subscriber-only categories separately with authorized non-subscriber and active-subscriber contexts. Listings carry summaries, not playback payloads. Complete browsing must not skip/duplicate items; recommendations must have valid destinations or be absent.
- Group reading/audio stories, meditations and lullabies in search. Preserve three-character/500 ms triggering. Verify matching, ranking, counts and pagination with real responses; contributor matches return content.
- Turkish/English/Portuguese remain available. New languages require verified tester/general visibility; configuration failure cannot grant test access. Default UI to English only without accessible settings. No automatic content-language fallback: ordinary rows use the selected language; CMS may deliberately curate a clearly labeled different-language category whose explicit item language reaches detail/reading/listening without changing UI preference. Publication/access and language/test visibility still apply. Reject stale account/language/query results. Story 1.1 verifies reversed completion of previous controlled-test context successes, errors and artwork loads; no stale result may alter current loading/error/catalog/artwork state. Document catalog/artwork waiting limits, timeout outcomes and current-context retry before acceptance.
- Visibility never authorizes playback. Ordinary users receive eligible live revisions; tester and subscription permissions differ. Reading/audio publication is independent. Canonical types are STORY/MEDITATION/LULLABY; AUDIO_STORY is an experience.
- Start explicitly when first-page image/narration is ready; bound preparation, align page/audio identity and recover failures. Persist duration/progress/completion without duplication. Exclude branching, reading background music, fixed ZIP boundaries and offline-download management.
- Support Android API 24+ and iOS/iPadOS 15+, phones/tablets. Use the approved emulator/Simulator-first older/lower-capacity and current profiles, supplemented by available physical devices. Record host, device/OS, build, network/cache context. Virtual results cannot replace required physical lifecycle, signed-upgrade or actual-purchase evidence.
- Add crash/memory/latency diagnostics with each feature; exclude credentials/private URLs. Derive numeric budgets from first-working-flow measurements before affected acceptance. Unexplained continuous memory growth, freezes and critical playback failures block acceptance.

## Technical Decisions

- Evolve `app/mobile` as the sole feature-first Clean Architecture client: presentation calls domain; data implements its repositories; domain excludes Flutter/infrastructure. Use BLoC/Cubit, Freezed state, `auto_route`, constructor injection through `get_it` + `injectable`, and `Either<Failure, T>` repositories.
- Lock verified Flutter/Dart, BLoC, router/DI generators, Freezed, JSON and build_runner versions at the first story. Record generation, analysis and minimum-platform compatibility; examples are not evidence. Add infrastructure incrementally.
- The composition root owns shared session/router/media disposal; route/tab providers own feature state. Routes pass IDs and scalar experience/language/revision context, preserve independent tab stacks and recheck session admission. Backend authorization remains authoritative.
- Use one authenticated transport and typed adapters. One media coordinator serializes source changes and rejects stale commands/completions; feature closure cannot dispose playback. Adopt just_audio/audio_service/audio_session when playback begins.
- Admit one immutable revision; renewal preserves it and rechecks access. Cache only complete, verified assets; bound images/media, retain active assets and isolate private cache by account/access. Preserve unacknowledged migration inputs separately.
- Backend content/category/asset modules collaborate through public APIs/events/IDs, never internal entities. Experimental manifests/defaults are not production contracts. Keep configuration environment-driven and development access out of production binaries.

## UX & Interaction Patterns

Story 1.1 preview decision (2026-10-03): the user confirmed the live Library value of 5 and adopted a server-configurable initial default of 5 eligible previews per category. Apply the limit after eligibility filtering, return fewer when fewer qualify and omit empty categories. Per-category overrides are not adopted; backend/mobile verification remains required.

Story 1.1 category-order decision (2026-10-03): add CMS editing/saving and backend persistence of Library category row order independently per language to the backend/CMS companion scope. Editing one language must not change another's saved order. Keep row order separate from within-category card order; preserve remaining row order after hidden/empty-category filtering. Verify different Turkish/English orders and a saved/reloaded order different from slug/ID sorting through backend and mobile. Storage, surface scope, existing-record initialization and tie behavior remain implementation-contract decisions.

Story 1.1 content-language decision (2026-10-03): an ordinary Turkish row with three eligible Turkish stories returns three even if two English versions exist. Omit a row with no eligible content in its declared language. “English Stories” may be an explicitly curated/labeled English-content row in Turkish UI; define its presentation/curation/content-language mapping. Do not substitute missing detail/start localization with another language. This supersedes earlier English content-fallback permission.

Preserve reference 1.0.35+333 with approved changes: actual assets, Lexend/color tokens, 16dp gutters, 3:4 covers, rounded frames/locks, vertical sections/horizontal rows, greeting, banner, search and three equal tabs; omit parent mode. Distinguish loading/error/empty states; retain artwork geometry. Failed card artwork uses a defined error placeholder while preserving the title/lock and the remaining catalog. Story 1.1 retains the current Library stack/scroll on reselection; card destinations, all-items, search, premium actions and Sleep/Profile tabs remain disabled with matching accessibility semantics until their consuming stories, as specified in its interaction scope table. Extend one shell. Details never auto-start audio; preserve reader controls and manual/automatic transitions.

Before Story 1.1 UI implementation, establish reference cases for disabled shell controls, artwork-error placeholders and long localized titles using the agreed phone/tablet and normal/enlarged-text matrix; reuse that matrix at acceptance. Apply documented tablet rules, not uniform phone scaling. Compare matched dimensions/language/content/state. Verify safe areas, normal/enlarged localized text, semantic roles/names/selection, 48dp targets and actual-artwork contrast; settle parameters before screen acceptance.

## Cross-Story Dependencies

Start catalog companion work with Story 1.1 despite missing capabilities. Assign and record EXT-CATALOG contracts, redacted success/error fixtures and real-backend evidence before listing acceptance; fixtures alone are insufficient. Close only the Story 1.1 home-list slice (transport/errors, identity, selection/order/limits, language/explicit category-language mapping and audience/locks) for its acceptance; detail, complete-category and search consumers remain outstanding. Record contract version, backend build, timestamp, test-data identities, language/audience and expected ordered identities with redacted real responses, and compare mobile results against that same dataset; relevant CMS changes require a refreshed baseline. EXT-MEDIA/EXT-RETENTION close admission, renewal, integrity, cache and interrupted-transfer contracts before consumers; EXT-HISTORY closes durable progress/deduplication before its first producer.

E1/E2 use authorized test access and free content. AD-5 signed upgrades gate E3 auth/migration. E3 owns production session/preferences, E5 candidate testing, E6 purchase/paywall/subscriber journeys. E2 finishes listening destinations; E7 consolidates evidence. Preserve separate mobile/CMS/backend tracking.
