---
title: 'Story 1.6: close reader sharpness decisions on both platforms'
type: 'feature'
created: '2026-10-15'
status: 'done'
baseline_commit: '7a05dd6588bafb74dcd92bc0fb5388ee50885199'
review_loop_iteration: 0
context:
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/tools/image-optimization/NATIVE_ENTRY.md'
---

<frozen-after-approval reason="explicit human continuation: complete Android sharpness and resolve iPhone blur; monetary cap lifted">

## Intent

**Problem:** Speed/memory observations are complete, but Android reader resolution quality is unreviewed and iPhone JPEG/WebP q85 sharpness remains unsuitable for an equal-quality substitution.

**Approach:** Capture full/1.0×/0.75× PNG for TR71/TR19 page1 on a physical low-memory Moto E20 actual reader. Bind all six screenshots and offer one local comparison page. Resolve iPhone's codec decision using retained captures/pixel diagnostics and previously accepted reader PNG, without claiming lossy q85 became sharper. Complete the bounded Story1.6 decision handoff after human review.

## Boundaries & Constraints

**Always:** Canonical QA UID, existing signers/keys, original sources and receipts, source/build/artifact identity, exact TLS/auth/integrity/UTC checks and fresh process/session/providers. Reuse signed f570/310 source; host-only changes. Start with actual A geometry discovery, then full/100/075 quality; quality contains two TR pages and no memory sampling. Keep native≤180s, full-entry≥150s, provider expiry≤root+718s, independent cancellation690s and hard cleanup718s. Each resource is exclusive; cancellation uses its authoritative successful POST name. A subsequent root requires prior terminal cleanup and a verified correction, never an automatic paid loop. No billing/usage/price/exchange queries; previous USD10 cap is explicitly removed.

**Human boundary:** Human visual acceptance is required; a missing/rejected verdict stays explicit. The prior iPhone PNG acceptance covers only two sources and its observed geometry. q85 rejection is a valid terminal codec decision, not invented quality-qualified savings.

**Never:** Production/deploy/schema/policy changes, new keys/IAM/signers, broadened native guards, overwritten evidence, re-encoding JPEG/WebP candidates, forced quality acceptance or full-P0/fleet/P95/thermal/leak claims. Earlier performance and quality artifacts remain immutable.

## I/O & Edge-Case Matrix

| State | Behavior |
|---|---|
| Valid A→full→100→075 | Same unit/build and discovered geometry; exactly two bound captures per quality arm |
| Wrong purpose/order/allocation/geometry or entry<150s | Refuse dispatch; preserve failure and owned cleanup |
| Changed/stale/null marker, wrong case/provider/decode/native UTC | Refuse capture; preserve original PNG and diagnostic |
| Missing/duplicate capture, overflow, timeout or live worker | No qualified collection; bounded child cleanup |
| q85 pixel loss or human blur | Retain codec concern/rejection; select only an independently accepted PNG candidate |
| Human verdict absent/rejected | Story remains review; retain fallback comparison rather than invent acceptance |

</frozen-after-approval>

## Code Map

- `tools/image-optimization/reader_android_window.py:15,30,138,215`: exact typed window/lease and existing profiles. Add a separate finite quality profile with A/full/100/075 order; preserve default orders, limits and final dispatch guard.
- `reader_resolution_experiment.py:198,255,620,808`: `admit_quality_sources`, explicit variants, qualification and observer. Wire a reader-only Android collector into its existing Pairing. Require complete collection before eligible result publication; raw captures remain available on failure.
- `native_artwork_comparison.py:298,382,427`: field constants and bounded screencap pattern. Existing 280×460/native_quality validator remains unchanged; a separate reader validator uses discovered geometry and independent `binding.providers[case]` decode/identity.
- `app/mobile/android/app/src/imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentInstrumentation.kt:94`: signed build already publishes live native quality bindings. Reader quality switches pages at8s and ends at16s; screenshot work must not block stdout consumption.
- Private source `/private/tmp/tellpal-selected075-54d1022d3ccb/operator/{selected075_android_r11,selected075_stream_r11,physical_clock_measured_r10,reader_api30_clock_full_envelope_r8}.py`: reuse bounded ownership/install/clock operations in a fresh root. Existing f570 build receipts and keys remain read-only. Original decoder runtime is Pillow12.0.0, unresolved repository venv Python.
- Fresh private evidence root `/private/tmp/tellpal-quality-closure-35ff4afb586f`. Retained TR source admission is `/private/tmp/tellpal-reader-extension-cda4f004236e/admission/retained-tr-quality-inputs.json`; original v7 source root is under Documents/TellPal-Private. Never import/run old budget helpers.

## Tasks & Acceptance

- [x] Correct the proven reader-quality observer timeout: use the existing cancellable `readerMain` UI-read discipline, bounded by500ms and the unchanged native absolute deadline. Preserve legacy non-reader quality's150ms read. A late/cancelled callback must not read or publish a binding; all current foreground/lifetime/freshness/source checks remain. An activity retiring during a queued read returns null without reading a binding, preserving normal settlement; original authenticated native postconditions still decide success. Add real Kotlin regressions for150–500ms responsive reader UI, >500ms cancellation, absolute deadline, normal retirement and unchanged legacy behavior. Fresh signed source/build identity is required before physical use.
- [x] Add a separate finite seeded quality-only profile full/100/075; freeze the expected geometry hash before its first entry and require every successful actual geometry to match it. Keep every existing profile unchanged. Require current native device-facts and physical low-memory qualification in the six-capture matrix. Root prepares original-bound variants from the qualified session02 geometry seed before allocation and records that seed's file/result hashes without transferring historical unit/build identity.
- [x] `reader_android_window.py`, tests -- add isolated quality profile/order and boundary regressions.
- [x] `reader_android_quality_capture.py`, tests -- collect bounded fresh before/after native bindings and exact-device PNGs. Stable comparison excludes only native observation time; sidecar timestamps equal those native readings. Require source-bound PNG dimensions/pixels and frozen geometry/decode/provider, finite two-case order, complete worker termination and no partial success.
- [x] `reader_resolution_experiment.py`, tests -- integrate collector and require six qualified captures across three arms without weakening legacy capture or primary paths.
- [x] Root operator -- verify host changes, restart only owned QA backend/auth, prepare original-bound PNG variants, run physical quality batch, then close owned cloud/endpoints/backend and remove fresh login tokens. Discovery A is geometry evidence, not a replacement performance comparison.
- [x] Root operator -- publish `p0f-quality-closure-{results.json,findings.md}` and private local gallery, record human verdict, archive evidence, update dedicated tracker and bounded Story1.6 handoff. Previous artifacts remain unchanged.

Given valid captures, when reviewed, then each source/arm has immutable session/provider/attempt, source/preparation, geometry/decode and screenshot hashes. Given accepted PNG quality on both platforms, when closing, then the smallest accepted tested factor is selected per geometry, q85's unsuitable sharpness is explicit, and production decisions/coverage gaps remain separate. Given incomplete execution or human review, then no completion is asserted.

## Verification

Run new capture/profile regressions and full portable `test_*.py` suite with local socket access; execute actual bounded screenshot subprocess/worker failure scenarios. Recheck f570 source and installed artifact identity. Reuse already passed Flutter/native checks only while their source remains identical. Root owns physical execution and human review after the implementation agent returns; report these tasks incomplete at handoff rather than launching external operations.

## Spec Change Log

- Runtime adaptation (2026-10-15): seeded quality root5 is FINISHED with zero captures. Its fresh diagnostic build definitively reported `NATIVE_QUALITY_UI_WAIT_TIMEOUT` after9.782s, with56null markers. This proves the150ms observer wait returned false in this attempt; it does not establish root2's cause or a decode/paint result. Correct only reader-quality snapshot observation to the existing bounded500ms `readerMain` discipline, including native absolute deadline, cancellation and late-callback exclusion. Retain150ms for the legacy non-reader quality protocol. This observation scheduling correction changes no credential/TLS/UTC/source/foreground/lifetime/native180s/entry150s/provider718s/owner690s guard. Preserve original source7b4a/build and failed evidence; use a new source/build receipt after the verified correction. Earlier plan references to unchanged observer timing remain historical.

- Runtime adaptation (2026-10-15, after four terminal roots): quality acceptance does not require another performance A in its current allocation. Replace only the subsequent operator's A-first assumption with a separate full/100/075 quality-only profile. A qualified prior geometry is an expected-layout seed; all13 actual geometry fields in each new quality ledger must exactly match it, otherwise refuse. First full entry requires the frozen hash; first success cannot replace it. All three captures' current unit/allocation/build/session/source and native/authenticated closure checks remain. Additionally require current `deviceFactsReason=observed` and `lowMemoryPhysicalQualification=observed` per arm so removing A does not remove native low-memory evidence. Earlier A-first profile and every failure remain unchanged. Nine local real Dart/Python controls confirmed that retiring an old pending status handshake can emit the generic pin-validity diagnostic while new revoke succeeds; none reproduces root4's63.708ms failure. Its exact cause remains unavailable. This scope correction decouples the authorized quality task from the already completed114s performance experiment; it is not a native timing/security relaxation or a new performance measurement.

- Runtime adaptation (2026-10-15): two owned roots are terminal. The second passed source admission but the signed f570 launcher refused during settlement after 14.483 s, with 83 null quality markers and no screenshot. Its failure ledger is byte-identical to discovery A and belongs to A's session; it is not quality evidence. The existing single settlement diagnostic cannot distinguish a 150 ms observer UI timeout from status publication or settlement postconditions. To complete the authorized physical test, add finite diagnostic-only substages to isolated Android instrumentation and produce a fresh signed build/source receipt. Preserve the original frozen plan and both attempts. This supersedes only the host-only/f570-reuse implementation assumption for subsequent Android attempts; every authentication, UTC, timeout, ownership and acceptance guard remains unchanged. No timeout cause is asserted before observation, and old f570 captures/performance remain historical evidence under their original identities.

## Runtime execution notes

- Root4's current-session 403-row A ledger reached complete at113.639809s, then recorded revoke request at113.835825s and failure at113.899533s. No quality arm ran. The broker received authenticated revoke HTTP but its method had neither returned nor raised when closing was observed. The existing `DART_TLS_PIN_VALIDITY` category also covers deliberately closed prior clients, so it does not prove pin mismatch. The 63.708ms request-to-failure interval does not prove a2s timeout. Root4 is FINISHED. Before any further paid attempt, run a local real Dart/Python interoperability reproducer covering pending status-handshake cancellation and delayed fresh verifier teardown. Existing successful fixture bypasses that delay; preserve every original security/timing guard and require causal evidence before behavioral correction.

- Diagnostic source `7b4aec2c31421e5b3ffa820331e0213e9aec75de8cea60b8c90fe0b4f38bd233` (310 files) passed 373 portable tests and 57 real Kotlin host cases with causal mutants. Its fresh Android release uses the existing signer. Root3 closed FINISHED after 47.984s before installation/native: its independent UTC reference request timed out during calibration. The private calibrator now retries only network timeout, at most twice, rebuilding both original Apple/Google observations and retaining each failure. Disagreement, insufficient budget, transport/UTC/correction limits and one calibration invocation per root still refuse. All 38 private controls passed before a subsequent root; the original helper and every failed attempt remain retained. No native cause is inferred from this pre-native network failure.

- First owned Android root completed qualified geometry discovery, then refused quality admission before native dispatch or any screenshot. The private caller supplied the six-page discovery admission where the new quality prover requires independently admitted TR source evidence. Its raw attempted label remains; a separate receipt records pre-native refusal. Authoritative cleanup observed FINISHED. The caller now passes the quality admission through one tested preparation helper. All 34 private controls passed, including real three-arm preparation/provenance roundtrip; restoring the wrong admission in a private in-memory mutant fails with the original refusal. Verification receipt SHA-256 `3e65e6f3ccb45697593fe968b6cbfaa0968005442ad2e4ea796b073ddce79893`. Host/mobile source and every native guard remain unchanged. A second exclusive root follows the verified correction; all original evidence is retained.

- 2026-10-15 implementation handoff: host quality profile, asynchronous capture collection, independent TR PNG reproduction, reader qualification and six-capture matrix are implemented. All 373 portable tests passed with local socket/process-group permissions; original private log is `/private/tmp/tellpal-quality-closure-host-8c197d4f116b/portable-tests.log`. Current mobile source remains `f570e4d098fbeb1663843f7f24ab520e3b57079296cfe9051bec2342ed116b25` across 310 selected files. Physical execution, installed artifact recheck, gallery/human verdict, cleanup archive and bounded Story 1.6 decision remain root-operator tasks; no completion or human acceptance is asserted.

- Physical completion: root6 qualified all six actual-reader captures with current native low-memory facts and matching13-field geometry. All six cloud roots and ten local endpoints are closed; owned QA backend stopped and login files removed. The gallery is delivered; Android human acceptance remains pending. Review proceeds over completed code/evidence while that final human gate stays explicitly open.

- Human quality update (2026-10-15): after correcting the gallery's missing JavaScript `data` binding, the human reported “hayır android 0.75 de görsel yazılar buğulaşıyor”. Android0.75× is rejected for embedded-text blur in the shown comparison. The statement does not identify individual affected content IDs or accept1.0×/full PNG. Record the partial verdict with the corrected gallery and immutable six-capture hashes; keep Android selection and Story1.6 open pending the1.0× review. Prior0.75× performance evidence remains unchanged and must not be presented as quality-qualified savings. iPhone's earlier acceptance retains its original limited scope.

- Human selection (2026-10-15): the follow-up answer “1x net” accepts Android1.0× against full PNG in the displayed comparison. Select1.0× PNG as the smallest accepted tested Android factor; keep0.75× rejected and its partial receipt immutable. This closes the Android visual decision for the shown sources/geometry, without whole-corpus or fleet acceptance. iPhone retains its independently accepted0.75× PNG. Final code review and evidence archival are still required before closing this spec.

- Final review (2026-10-16): all three context-free reviewers returned before triage. Correct unstarted-worker cleanup and require five valid provenance hashes per matrix capture; add actual quality-launcher routing, distinct factor dimensions and pre-dispatch admission regressions.381 portable tests,65 Kotlin cases, four guard tests and causal mutants pass. A frozen test-clock root now uses exactly representable seconds; no runtime180s/150s guard changed. The signed native instrumentation is unchanged from capture time; later source identity includes test/harness changes and is recorded separately.

- Evidence retention (2026-10-16): the original temporary quality root is unavailable for an unknown reason. Preserve surviving18 screenshot originals, gallery, structured physical observations/hashes, human selections and fresh review/test evidence in a clearly labelled supplement. Do not claim this recovers or fully archives missing native ledgers, sidecars, signed-build/cleanup receipts or historical private controls. The earlier performance archive checksum is freshly verified. This limits independent re-audit of physical qualification; bounded displayed-image quality decisions remain recorded separately from that limitation.

## Completion

Bounded Story1.6 is complete with an explicit evidence-retention limitation. Android selects1.0× PNG and rejects0.75× for embedded-text blur; iPhone retains previously accepted0.75× PNG for its shown scope. All original performance results remain unchanged. The surviving-evidence supplement contains903files,35574056bytes; archive SHA-256 `05f2cb057104f62263fbf8edf0def5d5f6a6c1dee4de3ebb260297e32f9c6ddc`. External archive readback verified every indexed file. It does not contain the unavailable original quality root. The committed archive receipt/result record is inserted after the archive snapshot to avoid a self-referential hash. Full P0 and production rollout are outside this bounded completion.

## Suggested Review Order

**Reader entry and ownership**

- Reject unbound originals before prerequisites, authentication or native dispatch.
  [reader_resolution_experiment.py:871](../../../tools/image-optimization/reader_resolution_experiment.py#L871)

- Freeze allocation order and expected geometry under the existing resource deadlines.
  [reader_android_window.py:37](../../../tools/image-optimization/reader_android_window.py#L37)

**Bound capture and source identity**

- Collect fresh before/after bindings without blocking native stdout.
  [reader_android_quality_capture.py:176](../../../tools/image-optimization/reader_android_quality_capture.py#L176)

- Require six captures and complete source identities before accepting a matrix.
  [reader_android_quality_capture.py:286](../../../tools/image-optimization/reader_android_quality_capture.py#L286)

**Native observation**

- Use cancellable reader UI reads while preserving the legacy observer boundary.
  [ImageExperimentInstrumentation.kt:143](../../../app/mobile/android/app/src/imageExperimentAndroidTest/kotlin/com/tellpal/imageexperiment/ImageExperimentInstrumentation.kt#L143)

**Decision and audit scope**

- Read the platform choices and unavailable original-evidence limitation together.
  [p0f-quality-closure-findings.md:1](p0f-quality-closure-findings.md#L1)

**Supporting regressions**

- Exercise real launcher routing and authenticated settlement with delayed UI responses.
  [LauncherHarness.kt:178](../../../app/mobile/ios/ImageExperimentUITests/android_host_harness/LauncherHarness.kt#L178)

- Detect a compiled regression to the legacy reader observer.
  [android_diagnostic_harness.py:345](../../../app/mobile/ios/ImageExperimentUITests/android_diagnostic_harness.py#L345)

- Distinguish resolution factors and refuse missing provenance before native work.
  [test_reader_android_quality_capture.py:132](../../../tools/image-optimization/test_reader_android_quality_capture.py#L132)
