---
title: 'Story 1.5 — Implement the Isolated Native Fixture Entry'
type: 'feature'
created: '2026-10-11'
status: 'done'
review_loop_iteration: 0
baseline_commit: c72a874b9b348fba60826be78d235aee4a7158b6
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/native-entry-implementation-r1.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The iPhone 12/Android Streaming pilot has preflight tooling but no isolated native entry for ephemeral authentication.

**Approach:** Implement r1's isolated profile/release targets, paired TLS bootstrap, authenticated fixture screen and sanitized diagnostics. Reader/catalog journeys remain separate.

## Boundaries & Constraints

**Always:** Follow the r1 companion. Preserve ordinary identities, Library release refusal, backend admission, QA UID, cache controls and historical evidence. Use Clean Architecture, generated injectable/GetIt, Cubit/Freezed and ID-only auto_route. Keep credentials/media/signing/raw receipts outside Git. Acceptance, implementation and physical verification are separate.

**Ask First:** Expanded journeys, weaker auth/TLS, changed controls, public tunnels, paid time or account changes require new scope. Physical installation/authentication requires exact device/artifact binding and prerequisite receipts.

**Never:** Production writes/deployment, cloud reservation automation, credential assets/defines/arguments/logs, original-byte fallback, global container replacement, SDK upgrades or Story 1.6 benchmark execution.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
| --- | --- | --- | --- |
| Admitted entry | Paired key, valid session and exact fixture | Isolated screen, matching provider/attempt, private sanitized ledger | Bounded stop and owned teardown |
| Invalid bootstrap | Missing/replayed/expired envelope, wrong key/pin/UID/scope | No fixture request or composition admission | Fixed refusal reason; no raw error |
| Invalid fixture | Unknown ID, changed SHA/MIME/size/dimensions, redirect | No decoded success or fallback | Retained sanitized failure |
| Stale lifecycle | Revoked/expired session, late callback, departed route | Cancel only owned work; reject stale state | Finite cleanup; failure retained |
| Missing measurement | Unsupported counter/frame mapping or loss | Null with reason | Never infer zero or target paint |

</frozen-after-approval>

## Code Map

Paths below are relative to `app/mobile/`; additions are mapped fully in the companion.

- `lib/main_library_dev.dart:15`, `lib/app/library_dev_harness.dart:196` — unchanged release guards.
- `lib/core/network/library_image_cache.dart:45,486,559,714,906` — profile/validation/ownership/close. Use cancellable clientFactory.
- `lib/app/injection.dart:6`, `lib/app/router.dart:8` — container/router examples; exclude default registrations.
- `android/app/build.gradle.kts:9`, `android/build.gradle.kts:8`, `ios/Runner.xcodeproj/project.pbxproj:173`, `ios/Podfile:6` — isolated build boundaries.

## Tasks & Acceptance

**Execution:**
- [x] `tools/image-optimization/native_entry_{driver,crypto,config}.py`, `native_fixture_server.py`, `pyproject.toml`, `uv.lock` — private config, locked dependencies, pairing/auth, immutable objects and bounded driver.
- [x] `app/mobile/lib/main_image_experiment.dart`, `lib/app/image_experiment_{configuration,bootstrap,composition,app,router}.dart` — once-only bootstrap, generated isolated composition and owned teardown.
- [x] `app/mobile/lib/features/image_experiment/`, `lib/core/network/image_experiment_*.dart`, `lib/core/diagnostics/image_experiment_*.dart` — companion's fixture feature, pinned cancellable cache/provider and nullable ledger.
- [x] `app/mobile/android/`, `app/mobile/ios/` — exact companion source sets/targets, handshake/test launchers, mode/entry guards and private signing/outputs.
- [x] `app/mobile/test/image_experiment/`, native tests, `tools/image-optimization/test_native_entry_*.py` — matrix, pin-before-header, crypto vectors, ownership and target exclusion; generate annotations first.
- [x] `tools/image-optimization/NATIVE_ENTRY.md`, `native-entry-implementation-status-r1.md` beside this spec — commands, receipts, rollback and open gates; preserve trackers.

**Acceptance Criteria:**
- Given ordinary product builds, when source membership and artifacts are inspected, then no experiment bootstrap is present and existing release guards still apply.
- Given unavailable hardware/signing/access, when implementation checks finish, then actual passes and unexecuted physical checks are distinguished and NATIVE-ENTRY remains open.

## Spec Change Log

- 2026-10-11: Completed r1 implementation tasks and retained actual synthetic/source check results plus unexecuted signed/physical gates in `native-entry-implementation-status-r1.md`. Frozen intent and historical trackers are unchanged.

## Design Notes

Fixture composition avoids Library changes. TLS uses exact certificate pins; P-256 keys use SPKI. Limits are laboratory controls.

## Verification

- From `app/mobile/`: `dart run build_runner build --delete-conflicting-outputs`, `flutter analyze`, serial new/relevant cache/ownership/release tests.
- Run locked host tests with synthetic TLS/crypto; inspect manifests/certificates/Xcode membership. Build isolated modes when prerequisites exist.
- `git diff --check`; retain receipts. Physical verification and Story 1.6 admission remain separate.

## Suggested Review Order

**Entry and ownership**

- Start the isolated mode and admit native configuration before creating application state.
  [main_image_experiment.dart:8](../../../app/mobile/lib/main_image_experiment.dart#L8)

- Create an independent generated container and release only its owned resources.
  [image_experiment_composition.dart:24](../../../app/mobile/lib/app/image_experiment_composition.dart#L24)

- Refuse stale credentials before mounting and bound lifecycle cleanup.
  [image_experiment_app.dart:15](../../../app/mobile/lib/app/image_experiment_app.dart#L15)

**Pairing and transport**

- Admit the one-time envelope against exact project, session and fixture bindings.
  [image_experiment_bootstrap.dart:5](../../../app/mobile/lib/app/image_experiment_bootstrap.dart#L5)

- Bound Android JSON before parsing and prevent repeated handoff.
  [ImageExperimentBootstrap.kt:32](../../../app/mobile/android/app/src/imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentBootstrap.kt#L32)

- Validate Apple deadlines without overflow before paired session admission.
  [ImageExperimentBootstrap.swift:8](../../../app/mobile/ios/ImageExperiment/ImageExperimentBootstrap.swift#L8)

- Retire observers safely while completing one authenticated revoke.
  [image_experiment_http_client.dart:11](../../../app/mobile/lib/core/network/image_experiment_http_client.dart#L11)

- Require pinned TLS, bounded authenticated fixture delivery and exact session revocation.
  [native_fixture_server.py:480](../../../tools/image-optimization/native_fixture_server.py#L480)

**Immutable fixture and draw evidence**

- Validate encoded identity through the existing cancellable cache seam.
  [image_experiment_image_cache.dart:25](../../../app/mobile/lib/core/network/image_experiment_image_cache.dart#L25)

- Tie successful teardown to a verified current fixture draw.
  [raster_case_view.dart:12](../../../app/mobile/lib/features/image_experiment/presentation/view/raster_case_view.dart#L12)

- Export bounded sanitized rows and leave unsupported measurements unavailable.
  [image_experiment_recorder.dart:5](../../../app/mobile/lib/core/diagnostics/image_experiment_recorder.dart#L5)

**Host admission and source identity**

- Hash exactly the bounded input set copied into private build workspaces.
  [native_entry_driver.py:194](../../../tools/image-optimization/native_entry_driver.py#L194)

- Reject physical runs without current exact artifact, device and prerequisite binding.
  [native_entry_driver.py:597](../../../tools/image-optimization/native_entry_driver.py#L597)

**Supporting configuration and verification**

- Keep Android target identities and private signing behind explicit opt-in.
  [build.gradle.kts:13](../../../app/mobile/android/app/build.gradle.kts#L13)

- Link isolated Apple targets using validated pinned pod dependencies.
  [Podfile:6](../../../app/mobile/ios/Podfile#L6)

- Exercise automatic draw, authenticated revoke, native settlement and owned release together.
  [app_draw_teardown_test.dart:24](../../../app/mobile/test/image_experiment/app_draw_teardown_test.dart#L24)

- Test real prerequisite validation alongside stale and altered source bindings.
  [test_native_entry_driver.py:238](../../../tools/image-optimization/test_native_entry_driver.py#L238)

- Follow exact private input, command and physical qualification requirements.
  [NATIVE_ENTRY.md:109](../../../tools/image-optimization/NATIVE_ENTRY.md#L109)

- Inspect resolved findings and the remaining physical verification boundary.
  [native-entry-review-r1.md:9](native-entry-review-r1.md#L9)
