---
title: 'Android launcher pairing lifecycle'
type: 'bugfix'
created: '2026-10-13'
status: 'done'
baseline_commit: 'ad9a7c48fc9d588677efc7fcbc6b9836855653e1'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Physical r164 passed installed-byte/clock/probe checks, then refused at `NATIVE_PAIRING` after 30.334 seconds with zero key reports. Sources expose a cycle: synchronous startup waits for Flutter UI, Dart awaits native handoff, and native awaits launcher pairing. Device-run attribution remains an inference without a thread trace.

**Approach:** Publish owned Activity creation independently of startup idle/first draw; the existing instrumentation thread reports its key after unchanged reachability admission.

## Boundaries & Constraints

**Always:** Preserve launch metadata/Intent identity, signature/self-test/validation/trust/pin/hostname/authentication gates, wire fields, first-stage diagnostics, settlement and owned revoke/finish. Synchronize readiness visibility. Bound creation plus reachability to 30 seconds from launch; retain absolute metadata deadline and 180-second settlement bound. Close admission before cleanup; late owned callbacks cannot reopen it or escape cleanup. One owned instance/report/terminal result.

**Ask First:** Changed identities/signers, broader application behavior, or spending beyond the already authorized 10 USD.

**Never:** Worker-wrapped `startActivitySync`, global registry, foreign Activity adoption/cleanup, logged metadata/credentials, guard relaxation, dependency upgrades, or Dart/Apple/production behavior changes. Host checks cannot close Story 1.5/OQ-6/presentation/benchmark gates.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected behavior | Error handling |
| --- | --- | --- | --- |
| Startup dependency | First draw withheld until handoff; broker unpaired until status | One exact public-key status precedes handoff/draw; existing settled success | Unchanged refusal if admission fails |
| Creation failure | Missing/null bootstrap, constructor or launch failure, early destroy | No pairing report or false success | First-stage refusal and owned cleanup |
| Ownership | Wrong class/metadata, second instance, repeated callback | Foreign instances remain untouched; owner never replaced; duplicates inert | No duplicate status/finish |
| Deadline race | No callback or callback after closure | Bounded refusal; late owner cannot become admitted | Revoke/finish only proven owned instance |
| Existing failures | Probe/pairing/claim/ledger/settlement failure | Existing outcome/fields and first diagnostic survive | Key clearing and disconnect preserved |

</frozen-after-approval>

## Code Map

Path aliases: `D` = `app/mobile/android/app/src/imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment`; `H` = `app/mobile/ios/ImageExperimentUITests`.

- `D/ImageExperimentInstrumentation.kt:16,46` — `newActivity` ownership; synchronous start precedes status. Reuse asynchronous `targetContext.startActivity` and post-super `callActivityOnCreate(Activity, Bundle?)` readiness; callbacks publish only.
- `app/mobile/android/app/src/imageExperiment/kotlin/com/tellpal/imageexperiment/{ImageExperimentActivity,ImageExperimentBootstrap}.kt` — read-only constructor/channel, probe→pair→handoff/cleanup contracts.
- `app/mobile/lib/main_image_experiment.dart:19` — read-only handoff-before-`runApp` order; include exact bytes in regression provenance.
- `H/android_host_stubs/{Instrumentation,Context}.kt` — missing lifecycle/asynchronous APIs; synchronous stub currently returns immediately.
- `H/android_host_harness/{HostRuntime,HostedConnection,LauncherHarness}.kt` — `/paired` currently independent of status; second engine overwrites global handler. Model per-instance ownership.
- `H/{android_diagnostic_harness.py,test_build_guard.py}` — exact source-copy/hash, real JSON/JCA, bounded originals, registered matrix/observer mutant.
- `_bmad-output/implementation-artifacts/image-optimization/native-device-preparation-status.md` — r164/r165 evidence, source freeze and budget. Baseline: `ad9a7c48fc9d588677efc7fcbc6b9836855653e1`.

## Tasks & Acceptance

**Execution:**
- [x] `D/ImageExperimentInstrumentation.kt` — implement owned bounded creation readiness and unchanged subsequent protocol.
- [x] `H/android_host_stubs/` and `H/android_host_harness/` — actual lifecycle callbacks, separate startup gate, broker pairing only from validated status, every matrix row.
- [x] `H/android_diagnostic_harness.py` and `H/test_build_guard.py` — register cases/private synchronous-start mutant; preserve diagnostics/mutants/provenance.

**Acceptance Criteria:**
- Given the startup gate, when the actual launcher runs, then status→pairing→handoff→startup-ready→explicit settlement occurs with original status fields and `RESULT_OK`.
- Given restored synchronous startup, when the same regression runs, then compilation succeeds and a named runtime assertion fails with zero reports; compiler failure/process timeout is insufficient.
- Given baseline protected inputs, when verification finishes, then Dart, Apple, bootstrap, ordinary Android/build/signing and dependency bytes remain unchanged.

## Spec Change Log

## Design Notes

[API 33 Instrumentation](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-13.0.0_r1/core/java/android/app/Instrumentation.java) waits for Activity idle; installed Flutter embedding delays draw until UI. Model startup-ready opening only from the actual session callback. Use a valid five-second metadata deadline for the mutant; never edit production timeout constants.

## Verification

- `tools/image-optimization/.venv/bin/python -B -m unittest discover -s tools/image-optimization -p 'test_native*.py'` — locked host suite passes with no skips.
- `tools/image-optimization/.venv/bin/python -B app/mobile/ios/ImageExperimentUITests/test_build_guard.py` — complete native guards, actual Kotlin matrix, existing and startup mutants pass their intended assertions.
- `git diff --check` — clean; retain private bounded compiler/runtime originals and source maps.

Parent subsequently builds token-free profile/release APKs with the existing signer. Physical retry requires fresh prerequisites and existing budget/watchdog controls; record physical results separately.

## Implementation and review verification

Final source freeze: **291 inputs**, `9540df33494a17e6062bf319b93460f1e31ed542ca8b83f7732dba320060925c`. All **270 protected selected inputs**, including 82 Dart production inputs, remain equal to the preceding source-bound APK map. The approved intent block is byte-identical.

Verification passed **180 locked native host tests, 33 native guards and 48 actual-source Kotlin cases**, with zero skips. The initial host command refused loopback binding in the sandbox; the same command passed with the authorized synthetic local-socket access. No real credentials, Cloud or devices were used by those tests.

The startup, observer, blocking-cleanup and acknowledgment-removal mutants compile and fail their specific runtime assertions; compiler errors/process timeouts are not counted as detection. Main-thread inline/off-thread queued callbacks, blocked owned creation, late cleanup, foreign/second-instance isolation and responsive finish-before-terminal are executed. Creation/reachability share the original 30-second launch origin and metadata deadline; cleanup acknowledgment is bounded at 250 milliseconds.

All three context-free reviews completed. Two medium patch findings were fixed: responsive owned finish acknowledgment before instrumentation termination and blocked-main deadline coverage with an accurate queued UI model. One pre-existing uncaught framework lifecycle-exception path is recorded in deferred-work; no global exception suppression was added. The prior revoked-key report race was not introduced by this change and does not authorize bootstrap changes. Original bounded Kotlin source/compiler/runtime and review demonstration proofs were copied to persistent private storage; **265 file hashes** were verified.

This standalone implementation is complete. Fresh APK builds and physical entry are separate parent observations; Story 1.5, OQ-6, the physical negative matrix, presentation/resources and Story 1.6 remain open.

## Suggested Review Order

**Launcher lifecycle**

- Owned creation releases pairing independently of Flutter drawing.
  [ImageExperimentInstrumentation.kt:83](../../app/mobile/android/app/src/imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentInstrumentation.kt#L83)

- Capture only the matching instance and publish completed creation.
  [ImageExperimentInstrumentation.kt:25](../../app/mobile/android/app/src/imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentInstrumentation.kt#L25)

- Bound cleanup acknowledgment while preserving late owned cleanup.
  [ImageExperimentInstrumentation.kt:63](../../app/mobile/android/app/src/imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentInstrumentation.kt#L63)

**Executable regression**

- Pairing depends on emitted status; startup remains closed until session callback.
  [HostRuntime.kt:17](../../app/mobile/ios/ImageExperimentUITests/android_host_harness/HostRuntime.kt#L17)

- Exercise startup, ownership and blocked-main boundaries.
  [LauncherHarness.kt:171](../../app/mobile/ios/ImageExperimentUITests/android_host_harness/LauncherHarness.kt#L171)

- Compile real source and require specific mutant runtime failures.
  [android_diagnostic_harness.py:131](../../app/mobile/ios/ImageExperimentUITests/android_diagnostic_harness.py#L131)

- Register the complete matrix and retained mutants.
  [test_build_guard.py:31](../../app/mobile/ios/ImageExperimentUITests/test_build_guard.py#L31)
