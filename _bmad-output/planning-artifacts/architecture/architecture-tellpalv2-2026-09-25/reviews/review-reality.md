# Reality and currency review — TellPal mobile architecture

## Verdict

The selected Flutter/media/navigation packages exist and fit the intended roles, but this spine is a contract for future production implementation, not proof that those dependencies or app-store identity are already integrated. Keep dependency compatibility and signed upgrade as explicit gates.

## Verified against project reality

- `app/mobile/pubspec.yaml` uses Dart `^3.11.0` and directly depends on `just_audio: ^0.10.6`; it does not yet integrate Riverpod, `go_router` or `audio_service`. The PoC cannot be represented as a production client.
- `app/mobile/pubspec.lock` reports Flutter `>=3.38.4` as the lockfile SDK floor. The development setup has used Flutter 3.41.1, so a current `go_router` release must not be blindly pinned without checking its Flutter floor.
- The legacy deployment guide contains Android/iOS app IDs, while the PoC uses distinct IDs. Neither this guide nor a simulator run proves live signing/provisioning or in-place upgrade access to records.
- `architecture.md` uses client-generated `event_id` for idempotency. ADR-0012 keeps `STORY`, `MEDITATION`, `LULLABY` as canonical content types; `AUDIO_STORY` is an experience, not a canonical type. A shared content-reference contract must preserve that distinction.
- ADR-0017 and the PoC endpoint establish experimental protected story media only; production authorization, revision retention and candidate/live behavior remain contract work.

## Official package checks (2026-09-26)

- [`just_audio`](https://pub.dev/packages/just_audio) documents Android/iOS playback and coordinated multiple players; its current package page lists 0.10.6.
- [`audio_service`](https://pub.dev/packages/audio_service) documents background audio, system controls and an audio handler wrapping `just_audio`; its current page lists 0.18.19.
- [`audio_session`](https://pub.dev/packages/audio_session) remains the audio focus/interruption configuration package.
- [`go_router`](https://pub.dev/packages/go_router) documents redirects, deep links and multiple navigators; its current page lists 18.0.1. SDK compatibility must be checked at integration time against the selected Flutter toolchain.
- [`flutter_riverpod`](https://pub.dev/packages/flutter_riverpod) remains a published Flutter state-management package. Selection is plausible, but the project has not yet proven it in an integrated production slice.

## Disposition

No version is pinned by this review. Before production dependency integration, select a Flutter baseline and resolve exact mutually compatible versions; test background playback and routing on iOS and Android. Before migration implementation, prove signed upgrade and real legacy storage/session access. Do not infer production readiness from the PoC.
