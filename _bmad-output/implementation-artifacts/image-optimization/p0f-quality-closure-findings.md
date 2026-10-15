# Story 1.6 reader quality decision

Date: 2026-10-16. Status: **bounded Story1.6 complete with an evidence-retention limitation; Android1.0× PNG accepted and0.75× rejected**.

The physical low-memory MotoE20 completed full/1.0×/0.75× PNG quality runs in the actual `StoryPlayerPage`. All six original screenshots are qualified and collected in one offline gallery. The human reported blurry embedded text at Android0.75×, excluding that factor from selection, then accepted1.0× with “1x net” in response to comparison against full PNG. Select1.0× PNG, the smallest accepted tested Android factor for the displayed scope. The individual affected0.75× content IDs were not specified; no per-image rejection statement is invented. The iPhone decision retains the previously accepted0.75× PNG and excludes blurry q85 JPEG/WebP from equal-sharpness selection. Final review and the surviving-evidence archive are complete; original raw-evidence availability limits independent re-audit as described below.

| Platform and tested layout | Selected PNG factor | TR71 dimensions | TR19 dimensions |
| --- | ---: | --- | --- |
| Android MotoE20 portrait411.43×631.43@1.75 |1.0×|681×1105|623×1105|
| iPhone portrait390×557@3, prior accepted scope |0.75×|773×1254|707×1254|

Factors are relative to each observed display geometry; equal factors do not imply equal pixel dimensions across devices. These are the smallest accepted tested factors, not global minima for all content or devices.

[Machine-readable evidence](p0f-quality-closure-results.json) binds the captures, original-source reproduction, sessions/providers, builds, observed geometry, native device facts and previous immutable performance evidence. No production resolution policy changed.

## Android actual-reader capture

The three fresh sessions share one current physical unit/allocation and the same signed release. Each completed authenticated app revocation and native settlement. Native facts in every arm report API30, MemTotal1,900,249,088bytes, memory class128MiB and `lowRamDevice=true`. Physical admission additionally uses the owned provider creation/catalog; native `isPhysicalDevice` is null and is not fabricated as positive proof.

The observed portrait slot is411.42857142857144×631.4285714285714 logical pixels at DPR1.75. All13 actual geometry fields match the frozen expected layout. The seed came from a previously qualified A layout; its historical unit/build identity was not transferred. Each current quality ledger and the final matrix independently prove the new geometry and shared current identities. No performance A was repeated in this successful quality-only allocation.

| PNG arm | TR71 actual decode | TR19 actual decode | Original screenshots |
| --- | --- | --- | --- |
| Full |1712×2778|1284×2279|2 ×720×1600|
|1.0×|681×1105|623×1105|2 ×720×1600|
|0.75×|511×829|467×829|2 ×720×1600|

Every capture has fresh before/after foreground native bindings, matching case/provider/attempt/decode and original PNG hashes. Reduced variants were independently reproduced directly from the admitted original sources. PNG encoding is lossless; downsampling still removes spatial detail and0.75× is intentionally enlarged by the reader. Human acceptance must cover that visible result, not pixel identity.

The human statements are “hayır android 0.75 de görsel yazılar buğulaşıyor” followed by “1x net”. [The selection receipt](p0f-android-quality-selection.json) binds both messages and the follow-up question to the corrected gallery, six Android screenshot hashes, matrix, build and geometry; [the partial verdict receipt](p0f-android-quality-human-verdict.json) remains unchanged as history. Select1.0× and reject0.75× for this Android comparison. Keep the previous0.75× speed/memory observations unchanged as measurements of a quality-rejected candidate; they do not establish an acceptable optimization. Neither full PNG nor every captured variant is labelled accepted by inference.

The gallery is `TellPal-1.6-Kalite-Kapanis-2026-10-15/Karsilastirma.html` on Desktop. Its Android71/19 groups show all three arms, with same-position switching and zoom. Twelve unchanged historical iPhone images are included as reference. Initial hash and syntax checks missed a runtime error: the image array lacked its `data` binding, preventing the initial draw. After the human reported an empty gallery, the missing declaration was corrected; the original HTML and receipt were preserved in `delivery-fix-evidence/`. A JavaScript execution check reproduced the original `ReferenceError` and passed initial draw, all six groups/eighteen variants, buttons, keyboard selection, zoom and mode switching after the correction, using a DOM test double rather than browser rendering. All18 original image hashes and dimensions were reverified unchanged. Corrected HTML SHA-256 is `f04ee2c4621992db0bf3aa7e4c7021569d94d905e556a26e3f8de86ad869cda6`. Automated page rendering was blocked by the browser's local-file protocol policy; no workaround was attempted. No Android quality acceptance is inferred from these checks or elapsed time.

## iPhone quality selection

The prior human reply “hepsi ok gibi” accepts the displayed full/1.0×/0.75× PNG comparisons for TR71/TR19 page1 at390×557 logical pixels/DPR3. Retain0.75×, the smallest accepted tested factor for those examples:773×1254 and707×1254 encoded pixels. The receipt and original screenshot hashes remain historical evidence under their original identities.

JPEG/WebP q85 are `rejected_for_selection_due_to_unresolved_sharpness_concern`. This is a selection decision, not an invented formal human rejection or a repaired codec claim. No JPEG/WebP candidate was re-encoded; quality-qualified codec savings remain null.

JPEG already used4:4:4 (`subsampling=0`), quality85, RGB, optimization/progressive disabled. WebP used lossy quality85/method6. Six original encoded files and six native screenshots matched their original hashes. Mean absolute source-resolution RGB pixel differences on the0–255 scale were:

| Source | JPEG q85 | WebP q85 | PNG |
| --- | ---: | ---: | ---: |
|TR71|1.2346|1.6041|0|
|TR19|1.6043|2.0047|0|

These confirm pixel loss, not a human sharpness score. Codec captures shared the original280×460@3 slot/per-source decode geometry. Native decoder/resampling contributions were not isolated, so compression alone is not proven to explain all perceived blur. This older slot and the accepted PNG reader layout remain separate comparisons.

## Observer correction and retained attempts

| Root | Outcome retained |
| --- | --- |
|1|Qualified layout discovery; private caller supplied wrong corpus admission; quality refused before native dispatch. Corrected actual preparation/provenance passed its causal mutant.|
|2|Full-quality native settlement refused after14.483s;83null markers, no screenshots. Exported failure ledger was byte-identical to prior A, excluded as stale. Exact cause unavailable.|
|3|Independent UTC reference timed out before installation/native; closed after47.984s. Bounded pair retry preserves every clock/correction guard.|
|4|Current403-row A ledger completed reader but failed authenticated teardown. Request-to-failure63.708ms does not prove2s timeout or TLS pin mismatch. Nine local real Dart/Python controls narrowed the misleading generic diagnostic; exact physical cause remains unavailable.|
|5|Diagnostic code definitively proved150ms UI wait returned false: `NATIVE_QUALITY_UI_WAIT_TIMEOUT`,9.782s,56null markers, zero captures.|
|6|New reader observer and seeded quality profile completed all three arms/six captures; owned allocation FINISHED after151.568s.|

Reader-quality observation now uses cancellable500ms UI reads bounded by the original native absolute deadline. Late/interrupted callbacks cannot read or publish a binding. Normal activity retirement produces no binding and leaves original authenticated settlement requirements intact. Legacy non-reader quality retains150ms. This scheduling fix changes no credential, TLS/pin, UTC, source, foreground/lifetime, native180s, full-entry150s, provider718s or owner-cancel690s guard. Root5 proves its own timeout; no earlier failure is silently assigned that cause.

Fresh Android quality source is `a1e827ddf2285b6d4babade85dd62b9142ea2f973a21a0ce28c1e0cefe1fd0d3` across310 selected files. Existing QA signer/keys were reused. All previous evidence remains unchanged.

## Performance handoff and scope

The immutable `p0e-selected075-performance-results.json` and companion findings contain the completed full/1.0×/0.75× speed/process-memory observations, from their original f570 build. That six-page textless corpus and this two-page lettered quality corpus are distinct; these captures are not a new speed/memory benchmark or quality-qualified codec savings.

In the prior single runs, iPhone full→0.75× first paint was1593.392→1497.456ms and sampled onstage footprint delta72.844→53.125MiB. Android was2844.564→1736.032ms and PSS delta21.111→14.176MiB. Android1.0× PSS delta12.219MiB was lower than0.75×, so no universal memory optimum is claimed. Cross-platform counters are different and cannot be compared numerically.

Human acceptance covers only the shown sources/layouts. Whole-corpus quality, smaller untested factors, other physical units/fleet, landscape/rotation, repeatability/P95, continuous memory peaks and sustained thermal behavior remain outside this bounded handoff. The earlier q85 native resampling cause remains unavailable.

## Verification and cleanup

Before final review, recorded checks were378 portable host tests,62 actual Kotlin host cases, causal observer/startup/cleanup/finish-ack mutants, two native guard controls,43 private ownership/clock/source/seed controls and nine local real Dart/Python interoperability controls. Counts describe separate scopes or latest successive suites, not totals to add. The two earlier guard commands were recorded without separate raw logs. The original temporary receipts are now unavailable as described below; no fresh revalidation of those private controls is invented. Previous Flutter checks remain historical because Dart/reader sources did not change.

The final review supersedes the first two counts with381 portable tests and65 real Kotlin host cases, plus four native guard tests and four private in-memory regression mutants. All pass, including actual quality batch dispatch under200ms UI delay and a compiled wrong-dispatch mutant that fails the named assertion. Worker startup failure cannot mask cleanup; every matrix capture must provide five valid source/preparation hashes. The test corpus now distinguishes1.0× from0.75× dimensions, and invalid source admission is rejected before prerequisites/auth/native dispatch. [The review record](p0f-final-review.md) separates these fresh checks from historical private controls. No signed native instrumentation change or new physical run followed the final review.

All six new owned cloud roots are FINISHED. Ten recorded local endpoints are closed, the identity-bound QA backend is stopped and current owned login-token files were removed; the shared database remains running. Cleanup receipt SHA-256: `0292c829c539b82d004fc31bb22f4864cffd1d345f78587ad28ab42ffd89d338`.

No billing, usage, price or exchange query was made. The human explicitly removed the earlier USD10 cap. Final review passed. [The surviving-evidence archive receipt](p0f-surviving-archive-receipt.json) records903files/35,574,056bytes and full external readback verification, with SHA-256 `05f2cb057104f62263fbf8edf0def5d5f6a6c1dee4de3ebb260297e32f9c6ddc`. Its public-document snapshot precedes archive-receipt insertion and the final local commit. Full P0 remains open; no production policy is changed by this decision handoff.

## Evidence-retention limitation

At final archival on2026-10-16, the temporary original quality root was unavailable; its disappearance cause is unknown. All18 Desktop screenshot hashes, the corrected gallery, human verdicts and structured result records survive. Original native ledgers/sidecars, signed-build/cleanup receipts and historical private-test originals cannot now be independently re-read. Their recorded hashes and observations are preserved without claiming fresh revalidation. [The retention audit](p0f-evidence-retention.json) lists this limitation. The parent performance archive checksum was reverified. The final supplement contains surviving evidence and fresh local review/test output; it is not a full archive of the missing original quality root.
