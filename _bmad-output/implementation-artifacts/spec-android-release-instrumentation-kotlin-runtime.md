---
title: 'Android release instrumentation Kotlin runtime'
type: 'bugfix'
created: '2026-10-14'
status: 'done'
baseline_commit: '17f119aa94150992604c647b96a7ba3257af80e4'
review_loop_iteration: 0
context: ['{project-root}/AGENTS.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Release target-app R8 removes Kotlin APIs used by separately packaged instrumentation. Local API 33 r253 crashes with `NoClassDefFoundError: kotlin.text.StringsKt` during the shared-vector self-test, before pairing. Physical profile r247 passes; release remains unverified.

**Approach:** Retain shared Kotlin stdlib in experiment release only, verify signed products, then pass original authenticated release entry locally before another paid trial.

## Boundaries & Constraints

**Always:** Preserve experiment opt-in, existing API/plugin retention, R8/minification, ordinary builds, SDK/dependencies/locks, identities/signer, launcher/self-tests, Dart/Apple, TLS/auth/time guards and cleanup. Use canonical QA/existing credential. Keep artifacts/raw receipts outside Git; record APK size delta. Local success cannot close physical/performance gates.

**Ask First:** Broader runtime/ordinary-app changes, SDK/dependencies, identities/signers, security/time semantics, new IAM/keys or spending beyond 10 USD. This repair creates no Cloud session; later physical work requires fresh budget/prerequisite/watchdog controls.

**Never:** Disable shrinking, replace stdlib versions, remove self-tests, suppress linkage errors, weaken guards, change production signing or deploy. Do not fabricate physical prerequisites or release-ledger evidence.

## I/O & Edge-Case Matrix

| State | Expected behavior | Failure handling |
| --- | --- | --- |
| New release | Callable runtime; self-tests pass | Retain later refusal |
| Old r233 / partial keep | Missing definitions detected | Negative control |
| New profile | Plugins/runtime preserved | Fresh byte-bound evidence |
| Ordinary build | Experiment rules unselected | Existing isolation guards |
| Failed inspection | No accepted result | Bounded fixed refusal |

</frozen-after-approval>

## Code Map

Aliases: `A` = `app/mobile/android/app`; `T` = `tools/image-optimization`; `D` = `_bmad-output/implementation-artifacts/image-optimization`. Private evidence/paths/hashes: `D/native-device-preparation-status.md`.

Private `P` = `/Users/gurhankucuk/.local/share/tellpal-native-qa/story15-20261012-iqk34w91`. Reusable configs/helpers: `P/android-physical-preparation-r246/operator/backend-runtime.py`, `P/local-release-r253/{config.json,operator/}`, `P/android-api33-restart-r226/owned-startup-receipt.json`. Rebind every source/artifact/output/runtime reference before reuse; the r254-owned backend/emulator/ADB are closed. Existing login inputs: `P/qa-credential/admin-credential.json`, `P/web-config-recovery/web-api-config.json`; mint a fresh canonical token with `T/native_test_login.py` if expired. Signer: `P/android-signing-recovery/signing.properties`. External SDK/build root: `/Volumes/MAC_EK/dev/{android_sdk,tellpal-plugin-registration-20261013}`. Keep all new private outputs durable and unique.

- `A/build.gradle.kts:145` — experiment-only release ProGuard selection; preserve.
- `A/src/imageExperiment/proguard-rules.pro:1` — add `-keep class kotlin.** { *; }` alongside bootstrap retention; explain cross-APK ownership.
- `A/src/imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentHandshakeTests.kt:116` — first repeat call; read-only.
- `A/src/imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentInstrumentation.kt:92` — self-tests before Activity; read-only.
- `T/native_entry_android_plugins.py` — reuse bounded raw-definition inspection; mapping proves names, not definitions.
- The first repaired release r255 produces a 17,484,863-byte mapping file and correctly refuses the existing 16 MiB read limit. Share a fixed 20 MiB mapping-file cap between plugin admission and runtime verification; retain private no-follow/stable-byte hashing, command/trace/deadline bounds and oversized-file refusal. This is a verification-only accommodation for retained stdlib, not a runtime/security/time change.
- `T/android_plugin_registration_integration.py`, `T/README.md` — explicit private SDK integration pattern; no discovered private paths/skips.

r233 app/test lack `StringsKt`, `RangesKt`, `ResultKt`; test references repeat/split/contains/replace, coerceAtLeast and createFailure. Usage/mapping corroborate removal/optimization. Profile supplies the runtime. r253's byte/UID-bound crash precedes probes/pairing/admission; UTC calibration passed. r254 confirms runtime closure. Preserve originals/current 292-input freeze; rebind new products.

## Tasks & Acceptance

**Execution:**
- [x] `A/src/imageExperiment/proguard-rules.pro` — retain shared stdlib only in experiment release; preserve API rule.
- [x] `T/android_instrumentation_runtime_integration.py`, `T/README.md` — explicit private APK/tool/output checker: full APK hashes, genuine runtime definitions, callable referenced APIs/inheritance, immutable r233 negative. Reuse bounds/token-free receipts.
- [x] `T/test_android_instrumentation_runtime_integration.py` — portable definition/reference/mapping decoys, partial runtime, tool refusal and byte mismatch; real SDK checks explicit.
- [x] `T/native_entry_android_plugins.py`, its portable tests and the runtime checker — use one fixed 20 MiB mapping-file limit; verify legitimate grown mapping and rejection beyond the bound, preserving byte provenance and all other limits.
- [x] `D/native-device-preparation-status.md` — fresh modes, preservation, size delta, inspection/local outcome; retain failures/open physical gates.

**Acceptance Criteria:**
- Given r233, when actual APKs are checked, then missing runtime definitions fail independently of plugin admission.
- Given fresh modes, when inspected, then plugin provenance and required Kotlin definitions/callable APIs pass on exact bytes with unchanged locks/signer/identities.
- Given repaired release/legitimate UTC, when original QA/TLS/control-PNG entry runs on owned API 33, then self-tests, probes, pairing, admission/status, complete fixture, authenticated revoke and settlement pass without a UID crash; otherwise preserve refusal.
- Given baseline inputs, when preservation checks finish, then only retention/verification files change, ordinary behavior remains isolated and no Cloud session is created.

## Spec Change Log

- Implementation evidence r255: retaining the shared stdlib grows the real R8 mapping beyond 16 MiB. Add a shared bounded 20 MiB verification limit and boundary regression; preserve source/API/runtime/auth/time semantics, artifact hashing, trace/deadline bounds, original r255 failure and all approved frozen intent.
- Review patch: add fixed schema/provenance/ancestry and diagnostic-storage refusals, actual-file source/copy and receipt-mutation regressions, and two detected condition-removal mutants. Patched SDK inspection r272 and final r273 proof pass 194 native, 22 runtime and 33 native-guard tests with zero skips; selected runtime inputs and original signed/local evidence remain unchanged.

## Design Notes

Facade-only retention leaves other missing APIs. Complete transitive/member closure is unproved; conservatively retain shared stdlib within the experiment and record size cost. This adopts no production policy. See [Google R8 guidance](https://developer.android.com/topic/performance/app-optimization/keep-rules-overview).

## Verification

- Portable discovery: existing `test_native*.py`, new `test_android_instrumentation_runtime*.py`; no skips.
- Existing native guards: `app/mobile/ios/ImageExperimentUITests/test_build_guard.py`; `git diff --check` clean.
- Explicit private runtime checker/plugin integration: new modes pass; r233 and partial-retention negatives refuse.
- Private `build-entry` both modes, then original local API 33 release entry: source/copy/APK/signature binding, authenticated outcome, UID crash capture and cleanup. No paid trial in this repair.

## Suggested Review Order

**Experiment release boundary**

- Retain shared stdlib for separately packaged instrumentation.
  [proguard-rules.pro:6](../../app/mobile/android/app/src/imageExperiment/proguard-rules.pro#L6)

**Runtime verification**

- Bind configured checks to exact artifacts and fixed refusal outcomes.
  [android_instrumentation_runtime_integration.py:213](../../tools/image-optimization/android_instrumentation_runtime_integration.py#L213)

- Resolve callable methods through actual facade inheritance.
  [android_instrumentation_runtime_integration.py:42](../../tools/image-optimization/android_instrumentation_runtime_integration.py#L42)

- Bind generated and compiled sources to the original signed build.
  [android_instrumentation_runtime_integration.py:176](../../tools/image-optimization/android_instrumentation_runtime_integration.py#L176)

**Bounded mapping inspection**

- Accommodate retained stdlib within a fixed file-size cap.
  [native_entry_android_plugins.py:20](../../tools/image-optimization/native_entry_android_plugins.py#L20)

**Regression evidence**

- Reject malformed, partial, tampered and stale inputs.
  [test_android_instrumentation_runtime_integration.py:73](../../tools/image-optimization/test_android_instrumentation_runtime_integration.py#L73)

- Verify legitimate grown mappings and oversized-file refusal.
  [test_native_entry_android_plugins.py:89](../../tools/image-optimization/test_native_entry_android_plugins.py#L89)

**Evidence and operation**

- Read review triage and immutable verification bindings.
  [android-kotlin-runtime-review.md:1](image-optimization/android-kotlin-runtime-review.md#L1)

- Separate local success from open physical and performance gates.
  [native-device-preparation-status.md:5](image-optimization/native-device-preparation-status.md#L5)

- Run the explicit checker with private inputs and bounded outputs.
  [README.md:483](../../tools/image-optimization/README.md#L483)
