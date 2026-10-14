---
title: 'Android experiment plugin registration'
type: 'bugfix'
created: '2026-10-13'
status: 'done'
baseline_commit: 'cc917796e78c1361cb9036bd38666fd9c197c519'
review_loop_iteration: 0
context: ['{project-root}/AGENTS.md', '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md']
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Both experiment APKs omit Flutter's generated registrant. Local API 33 r198 crashes after QA admission; matched symbols identify an uninitialized JNI JavaVM pointer.

**Approach:** Restore generated registration in profile/release, reject incomplete APKs, then verify locally.

## Boundaries & Constraints

**Always:** Add only the SDK-generated registrant; preserve experiment isolation and ordinary configuration. Keep private snapshots, offline resolution, locks, SDK, identities/signer, launcher, TLS/authentication/time guards, diagnostics and cleanup. Bind generated-source/inspection provenance to APK bytes; keep originals/credentials outside Git. Physical/Story 1.5/OQ-6/presentation/benchmark gates remain open.

**Ask First:** Broader runtime/ordinary-app changes, SDK/dependencies, identities/signers or spending beyond 10 USD. No Cloud allocation here; later physical work requires existing fresh budget/prerequisite/watchdog controls.

**Never:** Broad `src/main/java` inclusion, manual plugin lists/generated edits, JNI/renderer patches, weaker guards, credentials/IAM changes, deployment or emulator-based physical qualification. Do not remove the pre-existing profile `MainActivity`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected behavior | Error handling |
| --- | --- | --- | --- |
| Both modes | Current generated source | Compiled registrant, registration method and JNI plugins | Accept only bound evidence |
| Missing/stale generation | Missing, empty or incompatible source | No cached substitute | Fixed build refusal |
| Incomplete packaging | Missing definition/method; reference-only decoy | No accepted APK receipt | Fixed refusal |
| Inspection failure | Malformed/truncated output, timeout, tool failure | No false success | Existing private bounds/cleanup |
| Ordinary build | Experiment opt-in absent | Existing behavior | Existing guards |

</frozen-after-approval>

## Code Map

Aliases: `A` = `app/mobile/android/app`; `T` = `tools/image-optimization`; `R` = `_bmad-output/implementation-artifacts/image-optimization`.

- `A/build.gradle.kts:99–101` — excludes `src/main/java/io/flutter/plugins/GeneratedPluginRegistrant.java`. Add a dedicated source directory for this exact SDK output; validate before copy even on Gradle `NO-SOURCE`.
- `T/native_entry_driver.py:40,512–598` — private locked `flutter pub get --offline` regenerates omitted registrant. Add Android-only admission before receipt; reuse `checked_run`, deadline/private bounds.
- `T/native_entry_android_plugins.py` — new inspector. Prefer targeted `apkanalyzer dex code`; profile package listing exceeds 1 MiB. Verify raw definitions independently of mapping; resolve release parameter obfuscation from this build's mapping.
- `T/test_native_entry_driver.py`, new `T/test_native_entry_android_plugins.py` — portable synthetic orchestration and admission cases.
- `T/android_plugin_registration_integration.py`, `T/README.md` — explicitly configured, fail-closed real SDK mode/mutant, Gradle refusal and byte-bound old APK checks.
- `A/src/imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentActivity.kt:13` — read-only superclass registration call.
- `R/native-device-preparation-status.md` — r198 symbols/boundaries; private r166/r167 are negative controls on freeze `9540df33494a17e6062bf319b93460f1e31ed542ca8b83f7732dba320060925c`.

Private root `P`: `/Users/gurhankucuk/.local/share/tellpal-native-qa/story15-20261012-iqk34w91`.
- `P/android-{profile-r166,release-r167}/build/build-receipt.json` — old artifacts/provenance; `P/android-plugin-registration-diagnosis-r200/full-dex-readback.json` — complete negative controls.
- `P/android-emulator-diagnostic-r198/config.json` — prior local configuration; renew expired QA token with existing credential/login helper only. `P/android-api33-environment-r192/owned-emulator-start-r197.json` — owned external API 33 environment; revalidate before use. Retained `P/android-emulator-diagnostic-r187/operator/` helpers require current source/API bindings, never physical qualification.

## Tasks & Acceptance

**Execution:**
- [x] `A/build.gradle.kts` — restore registrant compilation in both modes; validate/copy only that file, remove stale owned output, explicitly order compilation.
- [x] `T/native_entry_android_plugins.py`, `T/native_entry_driver.py` — verify defined `io.flutter.plugins.GeneratedPluginRegistrant`, callable `registerWith`, JNI/JNIFlutter definitions and registration code. Bind source/inspection hashes to APK before receipt; honor release obfuscation and valid nonempty stderr.
- [x] `T/test_native_entry_android_plugins.py`, `T/test_native_entry_driver.py` — matrix, reference-only decoys, failure-before-receipt and private real old APK negatives.
- [x] `R/native-device-preparation-status.md` — record fresh token-free builds and bounded local API 33 entry separately from physical evidence.

**Acceptance Criteria:**
- Given r166/r167, when inspected, then both fail for absent registrant definitions; name references cannot satisfy admission.
- Given a clean private snapshot, when either mode builds, then generated registration is compiled, definitions/code pass and provenance matches the APK with unchanged locks/signer.
- Given the repaired profile APK and legitimate UTC, when original QA/TLS/control-fixture entry runs locally on API 33, then the null-JVM crash is absent and authenticated status, full PNG, verified decode, app revoke and settlement occur. Retain any later failure; verification stays open.
- Given baseline files, when verification ends, then Dart, Apple, bootstrap/launcher, locks and ordinary signing/source behavior are preserved; no Cloud session was created.

## Spec Change Log

## Design Notes

The superclass registers plugins by reflection. Matched JNI symbols prove null-pointer attribution; cache/path-provider triggering remains source correlation. Physical r175 lacks a stack.

[APK Analyzer](https://developer.android.com/tools/apkanalyzer) distinguishes definitions from references; mapping cannot establish absent raw definitions.

## Verification

- `tools/image-optimization/.venv/bin/python -B -m unittest discover -s tools/image-optimization -p 'test_native*.py'` — pass, no skips.
- `tools/image-optimization/.venv/bin/python -B app/mobile/ios/ImageExperimentUITests/test_build_guard.py` — existing cases/mutants pass.
- `git diff --check` — clean.
- Explicit integration harness (`android_plugin_registration_integration.py --config` with private SDK/Java/signer/archive/output paths) — required real SDK profile/release content, rejected `releaseMode:false` mutant, real Gradle refusal and old-APK negatives; missing inputs refuse, never skip.
- Private profile/release `build-entry` — APK/plugin provenance, unchanged identities/signatures/locks.
- Local API 33 — installed-byte binding, UID crash evidence and cleanup; no physical prerequisite fabrication.

### Implementation checkpoint

Final private release r208 and profile r209 builds pass on the 292-input source freeze `58aa9b86f2be6cecb5aad4d3676ca838f9f43f2a5d871b13823fac5eb3d192f5`, with unchanged signer/locks and bound SDK-generated-source/DEX/mapping/APK provenance. All 191 native host tests pass without skips, including actual old APK negatives and four real Gradle source-refusal/stale-copy cases. All 33 existing Apple/native build guards and mutants pass; `git diff --check` is clean.

Local API 33 profile r223 completed in 13.418 seconds: original QA/TLS/time guards, full installed APK binding, one pairing report, authenticated admission/status, complete 357-byte control PNG, explicit app revoke and native settlement passed. The independently extracted owned-app ledger has 14 records, zero drops and actual 128×160 decode. Both UID crash buffers are empty; broker/server/reverse cleanup passed, followed by owned emulator/ADB shutdown. No Cloud session was created. Physical Android profile/release, negative matrix, Story 1.5/OQ-6, presentation and benchmark gates remain open. Private originals are under `P/android-emulator-diagnostic-r223/`.

### Review patch checkpoint

Default native discovery now contains only portable synthetic checks; private SDK/Java/signer/archive paths are mandatory inputs to the explicitly invoked integration harness documented in `tools/image-optimization/README.md`. Whitespace-only DEX class/method headers receive fixed `EntryError` refusals, and reflection admission requires a public registrant class. The selected production source freeze after this patch is 292 inputs, `c50615b6a410ec58212c62666a3551d9ce89d867a21c3138340d783137a5332e`; r208/r209/r223 remain historical evidence for their preceding exact bytes. Fresh signed builds and local runtime binding remain required before final delivery.

The review patch passed 193 default synthetic native tests without skips and the explicit private r227 integration harness: actual SDK mode contents, rejected `releaseMode:false` invocation mutant, four real Gradle generation/stale-copy refusals and both original byte-bound APK negatives. Private original traces/receipt are retained separately; no SDK, signer, identities or original APKs changed.

### Current verification checkpoint — 2026-10-14

The review patch passed 193 portable native host tests with zero skips, the explicit r227 SDK/Gradle/archive integration checks, and a fresh run of all 33 Apple/native build guards and mutants. Release r233 and profile r234 signed app/test builds passed admission on the 292-input source freeze `c50615b6a410ec58212c62666a3551d9ce89d867a21c3138340d783137a5332e`, with unchanged signer, engine and locks. The preceding r229 build retained a failed admission because the operator selected the legacy APK Analyzer; no accepted receipt was written. The corrected builds use the existing current SDK analyzer.

| Current product | App APK SHA-256 | Test APK SHA-256 |
| --- | --- | --- |
| Release r233 | `ee3771a9a0c9be56430f559c0e1622abf2d1a175dffaf4144fe923c4b3ed210f` | `6a3bafac265e80b8f2f62735d6954b94792403c8b3554255a4fc4b2dbb0242bf` |
| Profile r234 | `b15acdfa99e3d57f23f54ffab3cfaa0fc82ad96ad384b7bbc3e9e9f0f7b16890` | `f1dcf1c3f821d1e28c986b7864fb413f12c86a5ce55273fa043640f455c59a47` |

Fresh local attempts r231 and r237 bound the installed r234 app/test bytes, passed both token-free probes, one pairing report, consumed proof and canonical QA admission, then refused at `NATIVE_ENVELOPE_TIME_TOKEN` before fixture delivery. They completed in 6.328 and 5.342 seconds. Both attempts have empty before/after UID crash buffers and successful broker/server/reverse cleanup; neither is a positive entry receipt. The combined native predicate does not identify its failed condition. The 906-byte token excludes an oversized-token explanation; clock measurements alone do not establish the cause. The r235 clock-only attempt refused alignment and did not launch the app. Original failures and clock observations are retained privately.

The earlier r223 full fixture/decode success is historical evidence for r209 and must not qualify the latest source-bound artifacts. Verification remains open; no final acceptance or physical qualification is claimed. No Cloud session was allocated during this repair. A read-only 2026-10-14 04:47 UTC check found the r226 emulator/ADB processes absent, ports 5038/5584/5585 closed and the former local QA backend listener on 61433 closed. A future run needs fresh owned runtime readiness and authentication evidence.

### Local time/token diagnosis completed — 2026-10-14

Current r234 profile entry passed twice on the unchanged 292-input `c50615b6a410ec58212c62666a3551d9ce89d867a21c3138340d783137a5332e` source selection: r243 with a passive observer in 13.447 seconds, then r244 without that observer in 11.938 seconds. Both used the original canonical QA, pinned TLS, time/token predicates, installed APK/UID/stat binding and launcher outcome validation. Each observed both probes, one pairing report, authenticated admission/status, one complete 357-byte PNG, explicit authenticated app revoke and native settlement. Each independently extracted app ledger contains 14 records, zero drops, encoded verification, draw and actual 128×160 decode. Both before/after UID crash buffers are empty.

The cold emulated clock initially measured approximately 642 milliseconds behind the host. A bounded persistent ADB channel reduced connection overhead, independently checked Apple/Google NTP, and estimated actual clock-command application latency. At most three correction passes, each capped to 25 milliseconds and the original ten-second channel deadline, target measured UTC; there is no arbitrary future pad. Final median offsets were +7.052 milliseconds versus the +5.595-millisecond reference in r243, and +9.078 versus +8.304 in r244, each within the five-millisecond verification tolerance. Host time, Android time-server settings and native validators were preserved.

The passive r243 observer retained only numeric envelope fields: TTL 176,387 milliseconds and token byte count 905. It returned the original immutable claim response without modification and wrote no token. Device/host clock brackets near envelope issuance were consistent with the successful original time predicates. The unobserved r244 repeat rules out dependence on this wrapper. This resolves the current local runtime verification; the exact failed clause in the earlier combined refusals was not observed and is not retrospectively asserted.

Preserved unsuccessful preparation evidence includes r240's stopped-container dynamic-port declaration check, r241's first clock alignment refusal, and r242's missing Python spawn entry guard in the private observer. The r242 import guard was corrected only in the separately copied operator; a passive spawn import and actual subsequent authentication passed. No application, SDK, signer, dependency, Firebase worker, credential identity or production source changed during this diagnosis.

Private originals and bound helpers are under `local-time-token-r240` through `local-time-token-r244`. The exact owned r241 backend/emulator/ADB process start times, command hashes and process groups were checked before shutdown; ports 61433/5038/5584/5585/18347 are closed. Database data, the canonical QA account and the existing replacement key remain retained for unfinished physical verification. No Cloud session, IAM/key change or deployment occurred. Physical Android profile/release and the negative matrix, Story 1.5/OQ-6, presentation, performance and candidate admission remain open.

## Suggested Review Order

**Build entry and artifact admission**

- Bind generation and raw APK admission before accepting a signed build.
  [native_entry_driver.py:515](../../tools/image-optimization/native_entry_driver.py#L515)

**SDK-generated isolated source**

- Copy only validated SDK-generated registration into the isolated experiment source set.
  [build.gradle.kts:39](../../app/mobile/android/app/build.gradle.kts#L39)

- Invoke the selected SDK generator with explicit profile/release mode and unchanged locks.
  [native_entry_android_plugins.py:38](../../tools/image-optimization/native_entry_android_plugins.py#L38)

**Definition and registration verification**

- Reject nonpublic, malformed or incomplete registration rather than accepting references.
  [native_entry_android_plugins.py:126](../../tools/image-optimization/native_entry_android_plugins.py#L126)

- Bind generated source, DEX inspections, release mapping and exact APK bytes.
  [native_entry_android_plugins.py:156](../../tools/image-optimization/native_entry_android_plugins.py#L156)

**Verification and operator guidance**

- Keep default refusal/provenance tests independent of personal files and signing.
  [test_native_entry_android_plugins.py:34](../../tools/image-optimization/test_native_entry_android_plugins.py#L34)

- Run explicitly configured real SDK, mutant, Gradle and archived APK checks.
  [android_plugin_registration_integration.py:139](../../tools/image-optimization/android_plugin_registration_integration.py#L139)

- Separate synthetic discovery from mandatory operator-owned real integration evidence.
  [README.md:422](../../tools/image-optimization/README.md#L422)
