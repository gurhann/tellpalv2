---
title: 'Story 1.6: iPhone and Android artwork quality, latency and memory'
type: 'feature'
created: '2026-10-14'
status: 'done'
baseline_commit: '6252fdb7787ae0db8f7dc4edb47e25834212f547'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Paint teardown prevents measurement.

**Approach:** Verify isolated instrumentation; compare retained JPEG q85/WebP q85 on iPhone 12 and cloud Pixel 7/API33, within USD10 total Test Lab usage cost.

## Boundaries & Constraints

**Always:** Preserve auth/trust/envelope/integrity/expiry/ownership guards, canonical QA, private evidence and existing credentials/signing. Freeze source/build/tool/device/admission identities; retain failures. Release AOT, portrait, observed layout/DPR, centered contain, requested 280×460 slot. Content71/page1/TR and Content19/page1/TR: four `ios-visual-preview-admission-r1.json` outputs plus normalized-pixel-equal PNG references. Record network/thermal conditions; identify Android proxy delivery separately.

**Ask First:** Scope/budget increases, new credentials/signing or production actions. Quality needs a user verdict.

**Never:** Change ordinary flows/envelope, re-encode/deploy, infer RAM from bytes, add overlapping counters, equate PSS with footprint, claim production P95/safety or unexercised coverage.

**Android budget:** `story-1-6-cloud-budget-reconciliation-r2.json` separates reported TRY0 from pending-cost estimates. The user approved maximum13 reservations with USD10 unchanged. Before each create, debit6min for the pending300s-root window; reconcile every terminal owned window using its full root duration plus1s, rounded up to whole minutes, including setup/queue/failure. Preserve the old40min bound and take the maximum with fresh project/month usage observations. Projected monthly usage cost must remain below USD10/TRY489.82. Require fresh catalog/usage/inventory/owned receipts. Unknown allocation/cleanup stops execution.

## I/O & Edge-Case Matrix

| State | Expected behavior | Failure evidence |
|---|---|---|
| Admitted run | Bounded phases, exact release/authenticated revoke | Ledger/outcome |
| Expired/refused/corrupt/revoked | Cancel; refuse/cleanup immediately | Retained failure |
| Missing counter/layout/release | Null/reason; no substitute | Ineligible metric |
| Stale/overflow or budget/cleanup refusal | Reject metric/allocation | Failure/unrun cells |

</frozen-after-approval>

## Code Map

- `app/mobile/lib/app/image_experiment_composition.dart`: owned teardown.
- `app/mobile/lib/features/image_experiment/presentation/{cubit/raster_case_cubit.dart,view/raster_case_view.dart}`: load/watchdog/paint teardown.
- `app/mobile/lib/core/diagnostics/{image_experiment_target_probe.dart,image_experiment_recorder.dart}`: identities/Dart clock;512 rows/250000 bytes.
- `app/mobile/ios/ImageExperiment/{AppDelegate.swift,ImageExperimentLedger.swift}`: isolated channel/schema; TASK_VM_INFO.
- `app/mobile/android/app/src/imageExperiment/kotlin/com/tellpal/imageexperiment/{ImageExperimentActivity.kt,ImageExperimentLedger.kt}`: channel/schema; Debug.MemoryInfo.
- `app/mobile/ios/ImageExperimentUITests/ImageExperimentBootstrapTests.swift`: capture launcher.
- `tools/image-optimization/{native_entry_driver.py,native_fixture_server.py,critical_ios_guard_pilot.py}`: admission/audit/outcomes; Android requires >195s reservation remaining at every invocation.
- Same-directory admission, `spec-1-5-critical-ios-handoff.md`, `epic-1-context-r4.md`: continuity.

## Tasks & Acceptance

**Execution:**
- [x] Add owned coordination at mapped paths; gate signed batch/purpose `native_comparison_r1`/`native_quality_r1`, preserve legacy behavior. Primary:30s baseline/10s load watchdog/10s hold/30s post-release. Quality:load/10s hold/teardown, no sampling. Require remaining session budget; revoke/close cancels timers. Baseline includes status polling.
- [x] Sample serially2Hz: iOS self phys_footprint with Mach result/count checks; Android background Debug.MemoryInfo.totalPss ×1024 bytes. Record null/reason, read duration, Dart call bounds/cadence, actual layout/decode. Extend Dart/Swift/Kotlin/host schemas together. Render disposal/end-of-frame plus eviction/release completion precede release observation; package/GPU retention unknown.
- [x] Add `tools/image-optimization/native_artwork_comparison.py` and phase/result/budget edge tests. iPhone:5 repetitions×2 sources×2 arms=20; source71 then19, odd JPEG→WebP/even reverse. Android:1 repetition,71 JPEG→WebP,19 WebP→JPEG=4; no per-arm quantile. Every run needs fresh process/session/namespace, absent keys and one verified complete transfer; OS/network-cold uncertified.
- [x] Review/sign/install/audit fresh release builds; verify tiny-control success and certificate/expiry/UID/corruption refusals on each platform before measurements. Guard failure stops that platform.
- [x] Capture6 quality runs/platform, PNG/JPEG/WebP/source, outside primary runs. Private XCTest or exact-device ADB screencap; bind session/case/provider/attempt/layout before/after. No tokens/attachments. One retry/cell within budget. Android sharing preserves fresh invocations, >195s entry guard,280s cancellation/300s cleanup, authoritative POST ownership. Queue/setup/failures debit budget; incomplete cells stay explicit.
- [x] Write `p0c-native-results.json` and `p0c-quality-resource-findings.md` here: evidence hashes, paired differences, denominators/failures, private quality comparisons, user lettering/diacritics/lines/gradients/color/framing verdict. Close owned resources; retain credential-cleanup obligations.


**Acceptance Criteria:**
- Given frozen runs, when collected, then outcomes/payload/decode/timing/memory have fresh identities or explicit failure/unavailability.
- Given phase samples, when summarized, then baseline median, sampled load/onstage maximum, final10s release median/deltas include gaps/count/observer durations; no true-peak/leak claim.
- Given events, when timed, then dispatch→ready/target-paint share one clock; raster completion/physical presentation stay unavailable.
- Given captures, when reviewed, then human verdict/PNG provenance are separate from byte savings; pending quality cannot qualify savings.

## Spec Change Log

- 2026-10-14: The user explicitly approved renewing the expired private Android QA broker TLS leaf for 48 hours using the same existing TLS key. Its SAN and key identity are retained; the private receipt records the changed certificate pin. No new key, IAM identity, app signing identity, production action, or budget increase is authorized by this exception.
- 2026-10-15: After the host's DHCP address changed, the user explicitly approved a 48-hour private iOS QA TLS leaf update using the same existing iOS TLS key, adding the current LAN address to the existing SANs and updating the runtime certificate pin. Private receipts retain the previous configuration and bind the new certificate and metadata-only negative pin control. Application signing, source, IAM and the cloud budget are unchanged.
- 2026-10-15: The user explicitly approved “10 USD sabit; 13 pencereyi onaylıyorum” after reviewing the closed-window reconciliation proposal. Only the Android budget constraint above is renegotiated: maximum13 windows, unchanged USD10 cap, six-minute pre-create debit, and terminal windows reconciled from their complete owned root plus one second rounded upward. The prior40-minute bound and fresh project/month usage maximum remain. At approval, seven closed windows rounded to13minutes; six further full debits projected at most USD8.85. Original nine-window/six-minute receipts remain retained.

## Verification

- Dart phase/ownership/capacity/cancellation tests/analyzer; locked Python admission/result/budget/guard tests; Swift/Android release builds and physical preflight.
- Inspect captures; single Android observations do not establish repeatability. Full OQ-6, floors, warm caches, rotation/pressure/lifecycle, enlarged text and reader coverage remain open.

## Execution Evidence — 2026-10-15

- All 20 iPhone and four Android primary cells qualified; 21 iPhone attempts include one excluded failed ninth attempt. Both platforms have six independently bound quality captures and five observed tiny guard controls.
- [Native findings](p0c-quality-resource-findings.md) and [structured results](p0c-native-results.json) retain denominators, original failures, evidence hashes, paired differences, actual layout/decode, cadence and observer costs.
- All13 Android windows reached authoritative FINISHED. The owned backend and recorded broker endpoints are closed. [Final read-only cost reconciliation](story-1-6-final-cost-reconciliation.json) gives USD5.85 / TRY286.55 as a conservative usage-cost upper model; Console reported TRY0.00 for the filtered service/date range. Neither is a final invoice. No more reservations are authorized.
- The user inspected the labeled iPhone originals and reported sharper PNG references and slight blur in the other formats. This concern is recorded without inventing an acceptance or per-criterion score. The user accepted all six Android originals (“Android: hepsi kabul”). iPhone approval is withheld after the reported concern; no formal iPhone pass/fail is invented. Quality-qualified savings remain null.
- Frozen application verification:96 Flutter tests, Dart analyzer, Swift checks and native release build/install/audit passed. Updated final host verification:271 tests passed, including actual Kotlin sampler execution; seven external Flutter cache regressions and their analyzer passed. The unchanged96 mobile regressions also passed again. The explicit matrix audit passed its eight named tests with no skipped coverage.
- Comparison disposition: Android accepted for the observed layout; iPhone acceptance ungranted after its reported sharpness concern. Pending quality cannot qualify combined savings. This completes the comparison evidence tasks with an explicit unresolved iPhone quality gate; workflow Step4 completed with five patch findings fixed and verified. All original24 primary ledgers and12 captures passed stricter read-only requalification, with original file bytes and result dictionaries unchanged. No push/deploy has occurred.

## Review Completion

The three final reviewers reported five actionable patch/test gaps and no intent/spec gap. [Review triage](story-1-6-review-r1.md) records the fixes and verification. Host observer r4 adds independent admission dimensions, load-phase bounds and complete typed capture identities. Source/build identities remain unchanged. All original evidence passes; no extra device or cloud allocation occurred. Comparison implementation and evidence collection are complete. Android quality was accepted by the user; iPhone quality approval remains ungranted following the sharpness concern, so combined quality-qualified savings stay null. This is not rollout approval.

## Suggested Review Order

**Owned phase coordination**

- Start here: coordinates bounded measurement phases and owned release.
  [image_experiment_phase_coordinator.dart:7](../../../app/mobile/lib/app/image_experiment_phase_coordinator.dart#L7)

- Disposal and end-of-frame precede cache release observation.
  [image_experiment_target_probe.dart:212](../../../app/mobile/lib/core/diagnostics/image_experiment_target_probe.dart#L212)

**Native sampling and host admission**

- Read self footprint with native result and read-duration evidence.
  [ImageExperimentLedger.swift:98](../../../app/mobile/ios/ImageExperiment/ImageExperimentLedger.swift#L98)

- Read self PSS in bytes; preserve null/error outcomes.
  [ImageExperimentLedger.kt:100](../../../app/mobile/android/app/src/imageExperiment/kotlin/com/tellpal/imageexperiment/ImageExperimentLedger.kt#L100)

- Bind dimensions, event phases and memory summaries to admitted identities.
  [native_artwork_comparison.py:217](../../../tools/image-optimization/native_artwork_comparison.py#L217)

- Reject partial identity expectations before accepting native quality captures.
  [native_artwork_comparison.py:320](../../../tools/image-optimization/native_artwork_comparison.py#L320)

**Findings, failures and budget**

- Read measured outcomes, quality concerns and the bounded cost reconciliation.
  [p0c-quality-resource-findings.md:1](p0c-quality-resource-findings.md#L1)

- Separate reported billing from the conservative cost upper model.
  [story-1-6-final-cost-reconciliation.json:2](story-1-6-final-cost-reconciliation.json#L2)

**Supporting regression evidence**

- Exercise malformed admission, phase contamination and incomplete capture expectations.
  [test_native_artwork_comparison.py:12](../../../tools/image-optimization/test_native_artwork_comparison.py#L12)

- Run real Kotlin sampler and real Flutter cache regressions.
  [README.md:1](../../../tools/image-optimization/native-tests/README.md#L1)
