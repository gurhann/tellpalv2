---
title: 'Diagnose Android Native Launcher Refusal'
type: 'bugfix'
created: '2026-10-12'
status: 'done'
review_loop_iteration: 0
baseline_commit: 442b648d51ba4d6bd485d35c3f0a335f3f7dfa79
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/references/architecture.md'
  - '{project-root}/tools/image-optimization/NATIVE_ENTRY.md'
---

<frozen-after-approval reason="the user approved the proposed bounded Android launcher stage diagnostics with Olur">

## Intent

**Problem:** Actual r140 Pixel 7/API 33 profile verification returned `NATIVE_LAUNCHER_REFUSED` after 6.176 seconds with no pairing-key report, despite qualified installed APK bytes. Existing catches merge self-test, metadata, activity/bootstrap and reachability failures. Android also lacks the existing Dart `reportDiagnostic` handler. The physical cause remains unknown.

**Approach:** Retain only the first finite refusal stage across instrumentation, the owned Activity/bootstrap and the existing Dart channel. Emit that allowlisted code only in the failed instrumentation result, alongside the unchanged generic reason/result code. Successful launches stay silent. Diagnose before any cause-specific repair.

## Boundaries & Constraints

**Always:** Preserve all original validators, crypto vectors/self-tests, operation ordering, exception objects where rethrown, request/header/pin/hostname/trust checks, timing limits, admission/settlement, once-only take/revoke and owned cleanup. Observer failures cannot replace or delay refusal/cleanup. Keep diagnostics experiment-only, bounded, thread-safe and first-only; optional observers default to no output. Preserve ordinary targets, dependency locks, signing and all Apple/Dart production bytes.

**Ask First:** Different identities, account/billing changes, weaker guards, public tunnels or new external access.

**Never:** Serialize errors, types/stacks, metadata/envelopes, tokens/claims, certificate bytes, URLs, device/user/session IDs or paths. No diagnostic settlement, admission retry, timer extension, cause repair, Firebase/backend/device operation or Streaming reservation. Do not infer a physical cause or close Story 1.5/OQ-6/Story 1.6 from host checks.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
| --- | --- | --- | --- |
| Success | All original checks pass | One unchanged pairing report and settled result; no diagnostic result field | Existing behavior |
| Native failure | A specific original operation throws/refuses | First corresponding finite stage; unchanged refused result and owned cleanup | Original refusal retained |
| Dart report | Existing channel receives a known fixed stage | Retain only if no earlier native/Dart failure | No changes to session state |
| Invalid report | Unknown/non-string/oversized/CR/LF/NUL/raw marker input | Reject silently without retaining/exporting input | Fixed boolean response |
| Broken observer | Diagnostic callback/output throws | Original failure/result and cleanup remain identical | Observer failure contained |

</frozen-after-approval>

## Code Map

Android root: `app/mobile/android/app/src/`.

- `imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentInstrumentation.kt:23` — shared catch; preserve pairing and terminal result.
- Adjacent `ImageExperimentHandshakeTests.kt:12,90` — real JCA/gate/settlement/JSON/ledger checks; retain with no-op subphase observation.
- `imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentActivity.kt:16,29` — constructor-to-null and owned channel; add strict existing Dart diagnostic handler.
- Adjacent `ImageExperimentBootstrap.kt:88,114,141,265,296` — constructor and async probe/admission guards.
- Read-only `app/mobile/lib/app/image_experiment_configuration.dart:8` and `image_experiment_bootstrap.dart:12` — existing finite Dart enum and first-only channel; no Dart edits.
- `app/mobile/ios/ImageExperimentUITests/test_build_guard.py:119` — register executed Kotlin harness and preserve existing guards.
- Cached Kotlin 2.2.20, Corretto 17 and Android-compatible JSON jar available; android.jar stubs are non-executable.

## Implementation Notes

- Instrumentation's existing `newActivity` lifecycle seam directly attaches its first-stage keeper to the newly owned experiment Activity before `onCreate`. No diagnostic Intent extra, process-global registry, launcher identity or session-state transition is added.
- Native observation wraps existing constructor and self-test operations or records the current asynchronous phase only on their original failure path. Original exceptions are rethrown by identity; observer exceptions are contained. The original combined envelope time/token predicate remains one `NATIVE_ENVELOPE_TIME_TOKEN` boundary rather than being split or reordered for finer diagnostics.
- The keeper stores one enum through atomic compare-and-set. Dart input maps only exact existing Dart enum codes to that enum; rejected input is never retained. Only the instrumentation's existing refused terminal result optionally exports `diagnosticStage`, with diagnostic output failures contained after the original owned cleanup.

## Tasks & Acceptance

**Execution:**
- [x] Android Kotlin — finite first-failure keeper and passive native/self-test boundaries; strict Dart handler; failure-only result field. Preserve original validation, exceptions, requests and cleanup. Record safer seams within intent.
- [x] Native/host tests — execute actual instrumentation with controlled Android boundaries, actual Bootstrap/Ledger/HandshakeTests with real JSON/JCA, and actual Activity/channel wiring. Cover every matrix row, ordering, first-only concurrency, silence and cleanup. Removing observer propagation in a private mutant must fail. Verify the host ignores the extra result field without admitting pairing/success.
- [x] `tools/image-optimization/NATIVE_ENTRY.md` and preparation status — document finite result field, exact source freeze and host-only verification; physical gates stay open and old artifacts remain historical.

**Acceptance Criteria:**
- Given early refusal, when instrumentation finishes, then its optional fixed stage distinguishes original boundaries without changing generic reason/result code.
- Given later/simultaneous reports, when recorded, then only the first code survives without altering admission, settlement, keys, timers or request count.
- Given success, when reported, then diagnostics are absent and original settlement remains required.
- Given invalid input/observer failure, when handled, then no raw value escapes and original cleanup/exceptions survive.

## Spec Change Log

- 2026-10-12: “Olur” approved the proposed bounded diagnostic action. Approval persists; no sprint key; physical execution separate.

## Verification

Execute registered Kotlin harnesses, complete native guards, all locked `test_native_entry_*.py` tests and `git diff --check`; matrix tests have zero skips. Preserve private bounded raw outputs, exact counts/maps and meaningful mutant failure. No real signing, device, Cloud, credentials or backend operation in this implementation handoff.

Initial implementation verification: 102 locked host tests and all 30 native guards passed with zero skips. The registered Kotlin guard executed 33 actual-source cases with real JSON/JCA and controlled Android/Flutter boundaries; its private observer-removal mutant compiled and then failed the specific first-stage propagation assertion. Its 291-input source freeze, `f221912b3275417e10624e4dd881ec8f19eaf1c9e07328300437bd40859f080e`, and original private proof remain historical.

Review triage: all three context-free layers completed; the blind layer had no actionable findings. Four findings were independently classified medium/patch (hash/copy consistency, timeout evidence, actual Dart allowlist oracle and trust-context terminal regression), accepted and fixed. There are no deferred, intent-gap or bad-spec findings. The frozen approved block is unchanged and production bytes are unchanged by these review fixes.

Reviewed implementation verification: 102 locked host tests and all 33 native guards passed with zero skips. The registered Kotlin guard executed 35 actual-source cases, including all 47 codes from the independent actual Dart producer through the Activity channel and the real launcher refusal for a correct-hash expired synthetic certificate. Three helper tests verify one-read hash/copy consistency and bounded timeout/output-limit evidence. The existing observer-removal mutant compiles and fails the specific first-stage assertion; a private missing-Dart-code mutant also compiles and fails the independent actual channel contract. Every approved matrix row is covered by executed passing cases. Exact final source freeze: 291 selected inputs, `0cd9670c9363c55f385f9a8d5a2824187a658a9fc85b633536703abbeb79ab15`; all 266 protected inputs match the independently rehashed prior signed-build source map. Complete required checks were rerun; bounded raw outputs, maps and mutation hashes are retained in a new private proof root without overwriting earlier evidence. Physical cause and physical gates remain open.

## Parent Operator Build Verification

After review verification, the parent separately built the token-free r143 profile and r144 release app/test APK pairs using the unchanged `native_entry_driver.build_entry` and the existing approved experiment signer. The private wrapper differs from its prior approved version only in the reviewed source hash. Both builds passed actual Android/Flutter compilation, application-ID and signer inspection, and exact artifact/source bindings. All four APK hashes and the current 291-input source snapshot were independently rechecked. The common public signing-certificate SHA-256 remains `913d78e46bb9bc3b3a1b0c545fb855cb6f57b3b91eaa7c1e87223fb3a188c967`.

The durable private aggregate r146 receipt hashes to `e82d6336e0f7a1437b404f94c711cd93c18e679ade46f8593a87b46d6153d4ac` and binds the original separate build receipts/provenance and all four APKs. No QA credential or fixture manifest was read, no production signer changed, and no installation, Cloud reservation or physical verification occurred. A new qualified device window and exact installed-byte checks remain necessary to observe the actual refusal stage; Story 1.5, OQ-6 and Story 1.6 remain open.

## Suggested Review Order

**Launcher and first-stage contract**

- Follow original launch/refusal flow with one optional fixed result field.
  [ImageExperimentInstrumentation.kt:26](../../app/mobile/android/app/src/imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentInstrumentation.kt#L26)

- Bind only the newly owned Activity before bootstrap construction.
  [ImageExperimentInstrumentation.kt:16](../../app/mobile/android/app/src/imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentInstrumentation.kt#L16)

- Keep one finite code atomically, with strict Dart input acceptance.
  [ImageExperimentRefusalDiagnostics.kt:86](../../app/mobile/android/app/src/imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentRefusalDiagnostics.kt#L86)

- Connect existing Dart reports without changing session state.
  [ImageExperimentActivity.kt:41](../../app/mobile/android/app/src/imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentActivity.kt#L41)

- Observe existing constructor and asynchronous failures while retaining original guards.
  [ImageExperimentBootstrap.kt:85](../../app/mobile/android/app/src/imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentBootstrap.kt#L85)

- Preserve combined time/token validation and exact exception identity.
  [ImageExperimentBootstrap.kt:264](../../app/mobile/android/app/src/imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentBootstrap.kt#L264)

- Run original real self-tests with passive subphase observations.
  [ImageExperimentHandshakeTests.kt:12](../../app/mobile/android/app/src/imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentHandshakeTests.kt#L12)

**Executed verification**

- Check every code independently from the actual Dart producer.
  [ActivityHarness.kt:24](../../app/mobile/ios/ImageExperimentUITests/android_host_harness/ActivityHarness.kt#L24)

- Exercise expired-certificate refusal through the real owned launcher.
  [LauncherHarness.kt:68](../../app/mobile/ios/ImageExperimentUITests/android_host_harness/LauncherHarness.kt#L68)

- Compile exactly hashed bytes and retain bounded failure evidence.
  [android_diagnostic_harness.py:25](../../app/mobile/ios/ImageExperimentUITests/android_diagnostic_harness.py#L25)

- Register actual-source Kotlin, mutation and helper regressions.
  [test_build_guard.py:31](../../app/mobile/ios/ImageExperimentUITests/test_build_guard.py#L31)

- Prove optional diagnostics cannot pair or settle a refused launch.
  [test_native_entry_driver.py:496](../../tools/image-optimization/test_native_entry_driver.py#L496)

**Operator evidence**

- Document failure-only output and the preserved host/native contract.
  [NATIVE_ENTRY.md:142](../../tools/image-optimization/NATIVE_ENTRY.md#L142)

- Separate host checks, local signed builds and still-open physical gates.
  [native-device-preparation-status.md:25](image-optimization/native-device-preparation-status.md#L25)
