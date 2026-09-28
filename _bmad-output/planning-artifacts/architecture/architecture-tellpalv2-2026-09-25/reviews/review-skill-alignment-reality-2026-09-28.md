# Architecture Reviewer Gate — Reality and Technology Fit

Date: 2026-09-28

Reviewed artifact: `../ARCHITECTURE-SPINE.md`, updated 2026-09-28, status `in-review`.

## Verdict

**PASS for the architecture alignment; implementation readiness remains conditional.** No blocking contradiction was found between the revised technology decisions, maintainer documentation, the inspected PoC, and the preserved PRD requirements. This is a document and source review, not a dependency-resolution, device, security or production-readiness certification. No application code or dependencies were changed or executed.

## Reality check

Inspected `app/mobile/pubspec.yaml`, README, all four paths in `lib/`, and the source owning manifest transport, route entry and playback. The current app is a small media experiment: its manifest declares Dart `^3.11.0`, Flutter, Cupertino icons and `just_audio: ^0.10.6`; it does not declare the newly selected architecture or generation packages. `lib/main.dart` uses direct Navigator entry; `lib/story_player_page.dart` owns an AudioPlayer inside widget state, uses setState and disposes playback on route destruction. Its generation checks and serialized transitions are useful evidence of existing race handling, not proof of an application-wide media coordinator.

The README explicitly excludes production sign-in, entitlement checks, offline media, background audio and full legacy UI. AD-1, AD-3, AD-7 and the Structural Seed correctly describe a target to evolve toward and explicitly prevent treating the protected PoC endpoint or test credentials as production infrastructure. `audio_service` and `audio_session` remain architectural selections, not demonstrated installed production integrations. No claim that the PoC already implements BLoC, auto_route or DI was found.

## Maintainer verification

| Decision | Evidence and assessment |
| --- | --- |
| BLoC/Cubit, selectors and effects | The maintainer documents both state owners, immutable selected values and BlocListener for effects. AD-2 fits. [flutter_bloc](https://pub.dev/packages/flutter_bloc) |
| Provider lifetime | A provider created with `create` closes its bloc; `BlocProvider.value` does not close an existing instance. AD-12 correctly assigns factory feature instances to route/tab providers and existing app singletons to the composition root. A singleton must never be returned from feature `create`. [flutter_bloc lifecycle](https://pub.dev/packages/flutter_bloc#blocprovider) |
| Guard reevaluation | auto_route supports `reevaluateListenable`, including a stream adapter, and calls guards again when notified. Reevaluation re-pushes the existing hierarchy; it is not automatic solely because a Bloc emits. AD-6 requires the correct outcome and is implementable using the documented bridge. [auto_route](https://pub.dev/packages/auto_route) |
| DI and initialization | get_it supplies registration/lifetime facilities; injectable generates registrations, abstract-interface bindings, scopes and async initialization. Its pre-resolved initialization must be awaited. AD-12 expresses this requirement rather than copying the skill's illustrative synchronous configuration signature. [get_it](https://pub.dev/packages/get_it), [injectable](https://pub.dev/packages/injectable) |
| Disposal and scopes | injectable supports explicit singleton disposal and separate generated scope initialization. AD-12 assigns ownership and requires clearing account-bound state. An account scope is an implementation option, not a mandatory new architectural mechanism. Container reset does not by itself establish that every outstanding callback, cache and retained tab has become safe. [injectable disposal and scopes](https://pub.dev/packages/injectable) |
| Immutable events/states | Freezed provides immutable properties and generated value behavior; AD-2 fits. No obsolete version-specific union API is mandated. [Freezed](https://pub.dev/packages/freezed) |
| Data serialization | json_serializable generates annotated serialization through build_runner. Keeping annotations in data models preserves the domain boundary. The required entity-extension convention must be demonstrated with the chosen concrete entity constructors during generation verification. [json_serializable](https://pub.dev/packages/json_serializable) |
| Failure values | dartz exposes Either; the spine's Future/Stream wrapping preserves asynchronous behavior without conflating an emitted failure with an automatically handled thrown exception. Repositories still need to normalize expected transport failures. [dartz](https://pub.dev/packages/dartz) |

These sources verify capabilities, not one mutually compatible version set. The Deferred section explicitly requires selecting and locking SDK, runtime and generator versions before the first consuming story, identifies skill examples as untested together, and preserves Android API 24/iOS 15. That is an honest and necessary limit; no package compatibility was inferred from documentation alone.

## Product safeguards preserved

Checked PRD FR-5, FR-9, FR-11–16, FR-18–29, NFR-1–9 and the addendum's ownership, media and evidence sections. AD-3 preserves the existing loop, interruption, background and cold-relaunch contract by reference; it does not silently authorize automatic interruption resumption. AD-4/5/8 preserve separate local anonymous inputs, receipt-based cleanup, one account migration, coexistence and signed in-place update proof. AD-6/7/8/10 preserve account isolation, backend authority and immutable revision admission. AD-9/11 keep purchase/restore and event delivery before release. The addendum's earlier technology-neutral language allowed architecture selection; the later accepted ADR-0018 selection does not change product behavior.

## Non-blocking implementation obligations

1. Before the first consumer, prove package resolution plus representative route, injectable, Freezed and JSON generation with analyze/tests. Do not copy illustrative skill versions as a solved toolchain.
2. Test cold protected deep links, ready-to-account-switch transitions, migration failures, repeated guard reevaluation and retained tabs. Explicitly wire session stream notifications and ensure reevaluation does not duplicate effects.
3. Test exactly-once disposal: route closure closes feature state; app-scoped session/media survive ordinary navigation; account transition clears retained state before ready and rejects obsolete completions. If get_it scopes are selected, await initialization and teardown and register disposal deliberately.
4. Keep API fixtures, authorization matrices, signed upgrade proof and device media evidence as open prerequisites at their stated consuming-story gates. This review does not close them.

No change to the spine is required to pass this bounded review.
