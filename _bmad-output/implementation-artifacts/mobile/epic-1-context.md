# Epic 1 Context: Discover content and read illustrated stories

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Enable an authorized development/test user to discover eligible TellPal content and complete illustrated reading journeys with reference visuals, reliable progress, and recoverable behavior. Epic 1 separates discovery, detail, reading, completion, language, media continuity, and sustained-session concerns so each can be contracted and accepted independently. Scope approval does not constitute implementation or integration acceptance; every consuming flow requires verified backend/provider contracts, representative evidence, and explicit readiness records.

## Stories

- Story 1.1: Discover eligible content on the Library home screen
- Story 1.2: Open the selected story detail from Library
- Story 1.3: Start reading the selected story
- Story 1.4: Navigate story pages manually and automatically
- Story 1.5: Complete a story and preserve its completion record
- Story 1.6: Control narration while reading
- Story 1.7: Preserve reading sessions through interruptions
- Story 1.8: Browse all content in a category
- Story 1.9: Search grouped content
- Story 1.10: Read in enabled languages
- Story 1.11: Renew access within the active story revision
- Story 1.12: Sustain long and repeated reading sessions

## Requirements & Constraints

Discovery must use CMS editorial ordering, bounded previews, exact publication/readiness eligibility, audience visibility, and backend-provided lock state. Empty categories disappear without disturbing remaining order; complete browsing and search must preserve identity, ordering, pagination, grouping, and access rules. Metadata visibility never grants protected media access.

The approved language rule is explicit: ordinary rows use the selected language with no automatic substitution. A CMS-curated, clearly labeled different-language category may carry its own content language while the UI preference remains unchanged. Turkish, English, and Portuguese remain independently available; future languages require verified visibility and tester configuration. Stale language, account, query, or media results must not replace the current context.

Reading starts only through an explicit action after bounded first-page image and narration preparation. Page and audio identity must remain aligned, transitions must avoid duplicate or stale effects, and failures must offer recovery. Completion, duration, progress, and interruption behavior require durable, idempotent contracts and must remain distinct from evictable media or telemetry. Offline-download management, branching, and normal-story background music are outside this epic.

Support Android API 24+ and iOS/iPadOS 15+ across phone and tablet profiles. Record device/OS, build, network, cache, and host context. Establish memory and latency budgets from first-working-flow measurements before quantitative acceptance; crashes, freezes, unexplained memory growth, and critical playback failures block acceptance. Do not treat emulator evidence as physical-device or production-provider evidence.

## Technical Decisions

Use the feature-first Clean Architecture client with presentation, domain, and data separation; BLoC/Cubit state, Freezed models, `auto_route`, and constructor-injected `get_it`/`injectable` dependencies are required. Repository boundaries use typed results such as `Either<Failure, T>`, and generated code/tool versions must be recorded.

The composition root owns shared session, router, and media lifecycle. Feature routes own their state, pass scalar content/experience/language/revision identity, and reject obsolete callbacks. One application-scoped media coordinator serializes source changes. Admit and renew one immutable revision while rechecking access; cache only complete verified assets with bounded, account/access-isolated storage.

Backend modules collaborate through public APIs, events, and IDs. Content/category/asset ownership and authorization remain server-side; experimental manifests and local test access are not production contracts. Configuration, credentials, domains, and storage prefixes remain environment-driven.

## UX & Interaction Patterns

Preserve the approved reference shell, typography, color tokens, spacing, cover geometry, locks, horizontal category rows, navigation states, and accessibility semantics. Use distinct loading, error, empty, unavailable, and artwork-error states while retaining layout geometry. Apply documented tablet rules rather than uniform phone scaling, and verify safe areas, enlarged text, semantic labels/roles, target sizes, and artwork contrast against matched reference cases.

Library owns its stack and scroll context. Deferred destinations remain visibly disabled until their consuming stories. Detail must not auto-start narration or reading; reader controls and manual/automatic page transitions follow the characterized reference behavior.

## Cross-Story Dependencies

Story 1.1 is the entry point for the shared catalog transport and controlled access flow. Its 2026-10-06 human acceptance closed only the home-list EXT-CATALOG consumer slice under qualified evidence; it did not close detail, complete-category, search, media, retention, history, or production release dependencies. Detail contract, error fixtures, and valid/absent related-content evidence remain required for Story 1.2.

Stories 1.3–1.4 depend on detail plus protected-start and immutable-media contracts. Stories 1.5 and 1.7 depend on durable history, progress, duration, interruption, and deduplication contracts. Stories 1.8–1.10 extend catalog consumers and exact-language behavior. Stories 1.11–1.12 depend on media renewal, integrity, cache, retention, and measured resource budgets. Controlled access supports Epic 1; production authentication/migration belongs to Epic 3, purchase/paywall integration to Epic 6, and release-wide verification to Epic 7.
