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

- Library uses CMS order, bounded previews, audience eligibility and backend locks; omit empty categories. Listings carry summaries, not playback payloads. Complete browsing must not skip/duplicate items; recommendations must have valid destinations or be absent.
- Group reading/audio stories, meditations and lullabies in search. Preserve three-character/500 ms triggering. Verify matching, ranking, counts and pagination with real responses; contributor matches return content.
- Turkish/English/Portuguese remain available. New languages require verified tester/general visibility; configuration failure cannot grant test access. Default to English only without accessible settings; fallback requires eligible published media. Reject stale account/language/query results.
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

Preserve reference 1.0.35+333 with approved changes: actual assets, Lexend/color tokens, 16dp gutters, 3:4 covers, rounded frames/locks, vertical sections/horizontal rows, greeting, banner, search and three equal tabs; omit parent mode. Distinguish loading/error/empty states; retain artwork geometry. Extend one shell. Details never auto-start audio; preserve reader controls and manual/automatic transitions.

Apply documented tablet rules, not uniform phone scaling. Compare matched dimensions/language/content/state. Verify safe areas, normal/enlarged localized text, semantic roles/names/selection, 48dp targets and actual-artwork contrast; settle parameters before screen acceptance.

## Cross-Story Dependencies

Start catalog companion work with Story 1.1 despite missing capabilities. Assign and record EXT-CATALOG contracts, redacted success/error fixtures and real-backend evidence before listing acceptance; fixtures alone are insufficient. EXT-MEDIA/EXT-RETENTION close admission, renewal, integrity, cache and interrupted-transfer contracts before consumers; EXT-HISTORY closes durable progress/deduplication before its first producer.

E1/E2 use authorized test access and free content. AD-5 signed upgrades gate E3 auth/migration. E3 owns production session/preferences, E5 candidate testing, E6 purchase/paywall/subscriber journeys. E2 finishes listening destinations; E7 consolidates evidence. Preserve separate mobile/CMS/backend tracking.
