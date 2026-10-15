---
title: 'Story 1.6 extension: resolution in the actual reader on a low-memory phone'
type: 'feature'
created: '2026-10-15'
status: 'done'
baseline_commit: '4721dccf090b29ce7b2d773d789873dbfbb324ae'
review_loop_iteration: 0
context:
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/tools/image-optimization/NATIVE_ENTRY.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Original1.6 omitted reader resolution and physical low-memory comparisons.

**Approach:** Run six real pages in existing `StoryPlayerPage`: full PNG/full decode(A), same PNG/bounded decode(B), resized PNG/bounded decode(C). Local iPhone12:A/B/C; physical Moto E20/API30/nominal2GB:A/C. Select the smallest resolution preserving reference sharpness; blur is not accepted merely for byte savings.

## Boundaries & Constraints

**Always:** Preserve ordinary behavior, original1.6, canonical QA and auth/TLS/integrity/expiry/ownership/release guards. Fresh release builds, existing credentials/signers, private evidence, separate sessions. Freeze source/asset/order/reference/variant/runtime/settings/geometry/build identities. Follow Flutter feature/BLoC/DI/router policy.

**Budget:** USD10 INCLUDING other services. Retain verified TRY73.73 at48.98225000067947TRY/USD + protected streaming USD5.85 + USD0.50 reserve. At most TWO new windows, each six minutes/USD0.90 debited BEFORE creation: upper USD9.65524. Preserve prior failures/debits and reserve. Refresh ownership/cleanup/catalog/usage via read-only APIs; no repeated billing UI. Reject increases/unknowns breaking the bound. Reconcile whole roots+1second, ceil minutes. No third window/automatic retry/replacement. Root300s/cancel280s; enter before105s with >195s remaining. Close owned resources.

**Ask First:** New credentials/signers, budget/windows. Human quality acceptance stays explicit.

**Never:** Production/backend/schema writes, deploy, cloud media acquisition, global trust/cache overrides, invented narration/publication, fleet/P95/leak claims. PoC uses `audio:null`; narration/audio geometry untested.

## I/O & Edge-Case Matrix

| State | Behavior |
|---|---|
| Wrong identity/expiry/access/bytes | Cancel; retain failure |
| Missing counter/device, death/gap | Explicit unavailable; never zero/success |
| A crashes/OOM | Retain failure; C allowed only after verified fresh guards/ownership/cleanup within its existing allocation |
| Budget/guard/deadline/cleanup refusal | Stop; retain unrun |

</frozen-after-approval>

## Code Map

- `app/mobile/lib/story_player_page.dart`: precache119/216/display340; `story_manifest{,_client}.dart`: client seam. `main.dart:10` sets the actual reader LRU24MiB; isolated legacy experiment32MiB must remain distinct.
- Existing composition/router/transport/probe/recorder, native ledger/launchers and host admission paths: see `spec-1-6-native-comparison.md` Code Map. Preserve legacy single-case contracts,512 rows/250000 bytes and legacy13-window gate; extension uses a separate purpose/budget gate.
- `p0c-quality-resource-findings.md`, `story-1-6-final-cost-reconciliation.json`: immutable evidence/private admission pointers. Retained content71 ZIP:18 ordered1712×2778 textless pages, first6 distinct. TR71/19 separately cover lettering at each candidate resolution and actual display size. Label QA IDs/revision, never publication.

## Tasks & Acceptance

**Execution:**
- [x] Add `tools/image-optimization/reader_resolution_experiment.py`: source admission, pinned direct LANCZOS resize from v7 pixels, per-size PNG verification, results/budget gate. Preserve originals;1.0×/0.75× demand candidates, latter stress only.
- [x] Add scoped composition under `app/mobile/lib/features/image_experiment/` and reader/client seams. Selected/next/display share pinned integrity-checked providers; finite manifest/media permissions retain revocation. Reader runs retain its24MiB LRU baseline; legacy32MiB remains unchanged.
- [x] Extend mapped Dart/Swift/Kotlin/native launchers: page paint, transfer totals, lifecycle, cache ownership, aggregate frames, physical device facts. Measure actual slot/insets/DPR/text scale before freezing per-device geometry. Moto A binds source dimensions and supplies geometry; derive/freeze C1.0× between windows, recheck identical geometry in C or refuse. Contain target: ceil(slot×DPR), source-cap scale, ceil dimensions; no upscale/crop. Validate actual decode independently.
- [x] Add `test_reader_resolution_experiment.py` and Flutter reader regressions: matrix, rapid stale selection, provider/precache consistency, cancellation, budget refusal, teardown. Build/sign/audit releases; local positive/negative guards precede paid allocation.
- [x] Execute fresh process/session iPhone A/B/C, Android A/C. Each:20s baseline, ≤60s pages1–6 forward/back/rapid selection plus real OS background/return,30s natural post-route,10s after QA-owned eviction; native deadline180s. Serial1Hz sampling, frame aggregates. Record actual model/API/ABI/MemTotal/low-RAM flag/memoryClass. Unavailable Moto/failed guard means unrun;3GB cannot qualify2GB.
- [x] Write `p0d-reader-resolution-results.json` and `p0d-reader-resolution-findings.md` here: identities/hashes, native counters, gaps/nulls/observer costs, transfers/paint/decode, failures/cleanup. Quality only after sealing primary: ≤20s/two pages within remaining180s/native and300s/root deadlines, otherwise unavailable without another window. Android primary is A/C1.0× only;0.75× is quality/stress. Present private full/1.0×/0.75× comparisons. Update dedicated image tracker only; `story_key` unset.

**Acceptance Criteria:**
- Given frozen variants, when measured, then source sufficiency and independent actual decode/geometry agree or qualification fails.
- Given route exit, when summarized, then natural retention and owned eviction remain distinct; pending/live/LRU ownership and background gaps are disclosed, never inferred as zero/success.
- Given comparisons, when interpreted, then A/C combines resolution/decode; B isolates decode, B/C may differ in resampling. Record unit identity, geometry and conditions across reservations; different units make A/C descriptive, not causal. One Android observation supplies no repeatability claim. PNG results do not qualify JPEG/WebP performance.
- Given terminal/unrun cells, when reported, then quality verdicts and cleanup are explicit; select the smallest candidate accepted for lettering/diacritics/lines/gradients/color/framing, otherwise retain full resolution; original iPhone q85 concern remains unresolved.

## Spec Change Log

- 2026-10-15: The user accepted reference-sharpness-first selection and explicitly requested applying the party review corrections and continuing tests. Added written TR checks, cross-unit comparison limits and safe continuation from A failure to C without extra windows. Approval is granted for this revised scope.

## Verification

- Locked host/protocol/budget tests, Flutter regressions/analyzer, real native memory checks, release build/install/audit and native guard controls.
- Inspect captures/terminal receipts. Retained billing is neither live invoice nor account spending lock.

## Execution closure

- iPhone A/B/C each qualified on frozen source `faa3a37ba01862375d03963a28325a971878fea049aacc9118e91c1d4a33da93`; primary seal SHA-256 `ffdd2a2eb5fd2b960bf5b5e2e36d92d771feee2594817ce3817cdff1dfca1c86`. Actual decode, identical geometry, separate retention/owned eviction, counters and gaps are reported in the companion artifacts.
- Android A ended before installation/native entry at the unchanged physical clock guard; C is explicitly unrun and unallocated. No replacement window was opened. The final read-only reconciliation records 34/34 current-principal sessions FINISHED and a conservative USD8.75523914272981 bound including the retained other-services baseline and reserve, not a live invoice.
- Quality captures followed the sealed primary runs: two source-bound captures per full/1.0×/0.75× variant. The human later reviewed the six original captures in one offline comparison and replied “hepsi ok gibi” (all seem okay). All three factors are accepted for TR71/TR19 at the recorded iPhone geometry; the smallest tested quality candidate is0.75×. Its primary performance is unmeasured; the A/B/C1.0× observations remain separate and no production resolution change is made.
- Current verification passed: 336 Flutter tests, 309 host tests and analyzer; receipt SHA-256 `488e6db437352494e9b4901f69f915caee53ca1795ae60f5d989eba03a2dd93f`. Historical native 33-test evidence is labeled separately with unchanged native inputs.
- Every frozen matrix row is covered by executed passing tests in `reader-matrix-closure-r1.json`, SHA-256 `aba3be5f5d224e3adbe8d665a4c9d4aeb45d456333a9e8297f886f8411669310` (76 named synthetic methods). Actual Android terminal failure/cleanup is separate from simulated failure coverage. Capture qualification also passed 9 methods.
- All 31 recorded local endpoints are closed by actual-address socket and kernel-listener checks; owned backend is absent and the shared database is retained. Cleanup receipt SHA-256 `7de9ea8e3d934f97d1ff027ecd46eb4ade9622de0cc42cfe201b6d6f48472827`.


## Review verification

- The three fresh review layers completed. Ten actionable patch findings were triaged, with no intent/spec loopback; the frame-attribution item is resolved by explicit callback-delivery-phase reporting. Patch regression checks and the separate deferred pre-existing cleanup issue are tracked in `p0d-reader-resolution-review.json`.
- Post-review implementation `de707ee1fb8f40e6e847dc7c3c905a88b6e5195e230e117b6223ad669af6c366` passed340 Flutter/312 portable host/33 native guard/8 explicit native integration/9 capture tests and analyzer. Complete receipt SHA-256 `26f68bb19636c1ab20a64f6337dc453d1aafc5907fe618d2f79475b8e834a4cb`.
- Measured r7 remains `faa3a37ba01862375d03963a28325a971878fea049aacc9118e91c1d4a33da93`. Offline stricter qualification preserves all prior values for3primaries/3quality sessions; receipt SHA-256 `4092e5b3089456cc1e4baadf7fca9073da9e7c9cdd0f16497efb8da1fdcb3f19`. This is no new physical benchmark.
- Human acceptance is explicit for all6captures;0.75× is the smallest tested quality candidate, with primary performance unmeasured. Android remains terminal/unrun; no new window or production change was made.

- Causal real-cache regressions are verified by two private mutants; receipt SHA-256 `01807c21da91f098898c468b03eb6feb9f9f7b919b87276ee72e735e60c6ab83`. The actual repository stayed unchanged.

## Suggested Review Order

**Admission and qualification**

- Start with finite native entry and unchanged release/auth/prerequisite guards.
  [reader_resolution_experiment.py:717](../../../tools/image-optimization/reader_resolution_experiment.py#L717)

- Derive source-capped contain sizes and refuse overflowing physical demands.
  [reader_resolution_experiment.py:62](../../../tools/image-optimization/reader_resolution_experiment.py#L62)

- Keep navigation, memory, lifecycle and decode evidence bound before qualification.
  [reader_resolution_experiment.py:565](../../../tools/image-optimization/reader_resolution_experiment.py#L565)

**Actual reader and owned lifetime**

- Drive the existing reader through forward, back, rapid selection and retention phases.
  [reader_resolution_cubit.dart:42](../../../app/mobile/lib/features/image_experiment/presentation/cubit/reader_resolution_cubit.dart#L42)

- Pin selected, prefetched and displayed images to one verified provider.
  [reader_resolution_images.dart:15](../../../app/mobile/lib/features/image_experiment/presentation/view/reader_resolution_images.dart#L15)

- Complete every owned cleanup while preserving the first retirement failure.
  [reader_resolution_cubit.dart:236](../../../app/mobile/lib/features/image_experiment/presentation/cubit/reader_resolution_cubit.dart#L236)

**Native observation and cache readiness**

- Collect serial process-memory samples; frame aggregates follow callback delivery phase.
  [reader_resolution_observation.dart:71](../../../app/mobile/lib/features/image_experiment/presentation/cubit/reader_resolution_observation.dart#L71)

- Await metadata readiness before enumerating the shared cache.
  [library_image_cache.dart:900](../../../app/mobile/lib/core/network/library_image_cache.dart#L900)

**Results and regression evidence**

- Separate accepted0.75× quality from measured1.0× performance and Android unrun.
  [p0d-reader-resolution-findings.md:1](p0d-reader-resolution-findings.md#L1)

- Exercise real six-provider admission and actual full/bounded decode, including causal mutants.
  [reader_resolution_test.dart:266](../../../app/mobile/test/image_experiment/reader_resolution_test.dart#L266)

- Run admitted Activity callbacks through the channel and launcher observation boundary.
  [integration_native_reader_probes.py:25](../../../tools/image-optimization/integration_native_reader_probes.py#L25)

- Review the three independent layers and explicit patch/deferred triage.
  [p0d-reader-resolution-review.json:4](p0d-reader-resolution-review.json#L4)
