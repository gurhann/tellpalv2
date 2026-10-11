---
title: 'Image Optimization Story 1.5 — Native Measurement Preparation and Admission'
type: chore
created: 2026-10-11
status: done
baseline_commit: 7e3ee8f58a210196ae6988dfa38481d5bd9a7957
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/SPEC.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Current diagnostics, historical virtual-device runs and the release-refusing Library entry do not establish native measurement readiness.

**Approach:** Deliver one auditable P0c.1 readiness package: observed device/tool matrix, exact proposed entry/credential/delivery changes and a reproducible measurement protocol, with explicit Story 1.6 gates.

## Boundaries & Constraints

**Always:** Follow the [canonical contract](../../specs/spec-image-optimization-1-5-native-measurement-admission/SPEC.md) and every companion. Preserve Library release refusal, Firebase admission/default QA UID, original/source/history, accepted profiles/caches and unrelated trackers. Use read-only discovery, opaque device aliases, hashes and nullable observations with provenance. Keep raw device IDs, private paths, tokens and URLs outside Git. Record failed sandbox queries separately from successful authorized discovery. Physical, virtual, disconnected, historical and unverified states cannot be substituted.

**Ask First:** Implementing/running the new native entry, build/install/signing changes, authenticated candidate delivery, acquisition/installation, expanded roles/batches, production or final policy decisions. These are separate downstream scopes, not preparation tasks.

**Never:** Modify app/backend/CMS/config/dependencies, connect/boot/provision devices, reset accepted caches, run native measurements, change frozen corpus/outputs or claim native quality/budgets/P95/crash safety. Do not close OQ-6 or hide device/counter gaps.

</frozen-after-approval>

## Code Map

- `app/mobile/lib/main_library_dev.dart:13` and `app/mobile/lib/app/library_dev_harness.dart:14` — release refusal and ephemeral development injection; read-only.
- `app/mobile/lib/app/injection.dart`, `library_app.dart` and `core/network/` — existing composition/provider/cache ownership; reuse boundaries, not implementation targets.
- `app/mobile/lib/core/diagnostics/library_diagnostics.dart:5` — catalog/errors only; no target-frame/process instrumentation.
- `be/src/main/java/com/tellpal/v2/user/web/mobile/LibraryHomeTestController.java:13` and `user/application/LibraryHomeTestQueryService.java:19` — local/test Firebase/server admission; read-only.
- `app/mobile/android/app/build.gradle.kts`, `app/mobile/ios/Podfile`, `app/mobile/ios/Runner.xcodeproj/project.pbxproj` — current identities/floors/signing; read-only.
- `p0c-device-matrix.json` — root-collected sanitized observations/receipt hashes; preserve observed values and source bindings when validating.
- `p0a-slot-source-inventory.md`, `p0a-trial-protocol.md`, `p0b-worker-findings.md` — retained prerequisites and offline limits; read-only.
- `p0c-native-entry-proposal.md`, `p0c-native-measurement-protocol.md` — planning investigations delivered these documents; verify/integrate them without changing accepted source contracts.

## Tasks & Acceptance

**Execution:**

- [x] `_bmad-output/implementation-artifacts/image-optimization/p0c-device-matrix.json` — retain actual source/device/tool observations, six physical coverage categories, counter gaps and native gates; unknown values never zero.
- [x] `_bmad-output/implementation-artifacts/image-optimization/p0c-native-entry-proposal.md` — verify concrete platform target/bootstrap/delivery/DI/instrumentation/rollback proposals and citations; acceptance, implementation and verification stay separate.
- [x] `_bmad-output/implementation-artifacts/image-optimization/p0c-native-measurement-protocol.md` — verify cache/clock/frame/counter/repeat/settlement/failure definitions and distinguish proposed from exercised methods.
- [x] `_bmad-output/implementation-artifacts/image-optimization/verify-p0c-readiness.py` — independently verify matrix types/coverage/receipt-source binding, current guard/floor/source hashes, local links/public privacy and open native gates; standard-library only, no device/network calls. Support `--matrix` for negative checks and optional `--private-evidence-root` for exact receipt rehash/observation replay; distinguish public-only validation from private replay.
- [x] `_bmad-output/implementation-artifacts/image-optimization/p0c-native-readiness.md` — derive readiness verdict/coverage/next actions from the matrix and proposals; document AC coverage, actually run checks and remaining native gates.
- [x] `_bmad-output/implementation-artifacts/image-optimization/spec-1-5-native-measurement-admission.md` — record completion and validation; synchronize only Story 1.5 in the dedicated `sprint-status.yaml` after root review.

**Acceptance Criteria:**

- Given current discovery and historical inputs, when reviewing the matrix, then every required device/tool/counter has provenance or an explicit gap and no virtual/disconnected record qualifies physical readiness.
- Given existing release/auth/cache boundaries, when reviewing proposed changes, then exact files, credential/delivery trust boundaries, artifact isolation, rollback and positive/negative verification are concrete without implemented-entry claims.
- Given future native journeys, when reviewing the protocol, then three cache identities, target-frame association/null fallback, actual transfer, nonoverlapping counters, sampling/overhead/drift and pressure provenance are defined.
- Given the preparation package, when verifying it, then all four deliverables and source/link/privacy checks pass, OQ-6/native execution gates remain open and unrelated tracked files are unchanged.

## Spec Change Log

- 2026-10-11: Completed the bounded preparation package. Preserved the initial 38 source bindings and all discovery/receipt values; added five inspected-source hashes and explicit tool/device/coverage/counter gap metadata. Integrated the concrete target-frame association proposal while retaining its default null endpoint. Added the standard-library verifier and matrix-derived readiness/AC handoff. Root review and the dedicated Story 1.5 tracker synchronization remain the final workflow step; no frozen intent or canonical contract changed.
- 2026-10-11 review patch: Strengthened receipt-derived phone/tablet classification and simulator absent-metadata null/availability/provenance checks. Bound receipt hashing and strict JSON parsing to one read byte snapshot. Added a permanent standard-library regression suite with synthetic receipt fixtures; matrix observations and frozen/canonical contracts remain unchanged.

## Design Notes

The user's 2026-10-11 “devam edelim” continues the just-prepared Story 1.5 contract and authorizes this same preparation scope. The existing untracked spec files belong to that scope. Leave `story_key` unset because shared trackers have conflicting numeric IDs; root uses the explicit image-optimization tracker only. Missing physical hardware/counters is a valid preparation result, not an implementation gap to fill with invented evidence. Preserve parent and historical OQ labels and disclose their conflict.

## Verification

- `python3 _bmad-output/implementation-artifacts/image-optimization/verify-p0c-readiness.py` — expects preparation/source/document checks to pass while native readiness remains false.
- Run meaningful malformed-matrix checks against the same verifier using isolated temporary copies: false physical readiness, lost provenance, changed source hash, invalid metrics and missing coverage must be rejected.
- Audit links/whitespace/privacy including untracked files and compare tracked source/config/predecessor hashes to the captured baseline. No app build or native measurement is part of preparation verification.

Executed on 2026-10-11:

- Public-only verifier: **PASS**, 43 current source hashes and tracked baseline byte comparisons; release/harness/local-test/Firebase/default-UID/cache/floor guards; 9 required coverage rows, 13 null counter gaps and 5 open native gates; all four linked deliverables, local links, public privacy and whitespace. Output distinguishes public-only validation from private evidence replay.
- Optional private receipt replay: **PASS**, all 13 exact receipt files rehashed; exact physical-versus-simulated registered-device filtering, available simulator inventory/runtime counts, saved device/tool identities, cached SDK revisions, ADB device count and initial probe status/timestamps independently reprojected. Retained public probe durations use explicit millisecond rounding of the saved discovery duration, not native performance timing. Raw private values/paths were not exported; this is saved-evidence consistency, not a new device query.
- Isolated `--matrix` malformed copies: **19/19 rejected** — false native readiness; disconnected-device qualification; virtual physical substitution; missing provenance; failed sandbox receipt used as observed; wrong receipt filename binding; changed source hash; boolean/negative/nonfinite counters; unknown unit; missing floor coverage; duplicate alias; invalid timestamp; boolean summary count; native gate closure; invented renderer/build; unknown RAM replaced by zero; extra raw-device field. Temporary fixtures were removed and the retained matrix was not mutated by the tests.
- Root independently exercised eight malformed matrices through the verifier; **8/8 rejected**. The review patches add repeatable synthetic regression coverage without changing actual discovery or admitting any native claim.
- Permanent regression command `python3 -B _bmad-output/implementation-artifacts/image-optimization/test_p0c_readiness.py`: **42 tests PASS**, no skips, 1.622 seconds on the recorded rerun. Covers valid public CLI plus the prior 19 malformed categories; duplicate-key/nonfinite/exponent-overflow CLI/parser/private-snapshot rejection; synthetic physical/simulator phone/tablet classification including unsupported/conflicting families; absent simulator metadata exact null/availability/provenance/reason/shape; and hash plus parse using the same single-read bytes. Test fixtures contain only explicit synthetic receipts and temporary files; no private root/raw device values are stored in the test source.
- Review-patch public CLI and optional real private receipt replay reruns: **PASS**, 43 source hashes, 60 local links, 13 exact private receipts, nine coverage rows, thirteen null counters and five open gates. The new test source is included in the verifier's direct public privacy/whitespace inventory. Device observations, first 38 source bindings, canonical/frozen documents and unrelated tracked files remain unchanged.
- Python source compilation/syntax and `git diff --check`: **PASS**. A baseline `git diff --name-only` reports only the scoped pre-existing image-optimization tracker change; app/backend/CMS/config/dependency/history and unrelated tracked files are unchanged. The verifier also audits untracked public deliverables directly.

Final bounded outcome: `preparation_complete=true`, `native_execution_ready=false`, zero qualified physical devices. All native acceptance/implementation/verification references remain individually null. Entry implementation, physical acquisition/connection/build/install/signing, authenticated delivery, frozen batch execution, quality/budgets/P95/crash-safety and downstream policies remain outside this completed preparation scope. Story 1.6 stays gated/backlog; root review synchronized only Story 1.5 to `review` in the dedicated tracker.

Root completion on 2026-10-11: all three fresh-context review layers completed. Four actionable medium patch findings were fixed: receipt-derived form factor, unsupported simulator metadata, one-snapshot receipt hash/parse and durable negative-case regressions. Independent root checks passed: 42 persistent tests, public-only validation, all 13 real private receipts, 43 source hashes and 69 local links including the Suggested Review Order; the initial eight independent malformed checks also passed. All tasks and acceptance criteria are satisfied for preparation. Only the dedicated image-optimization Story 1.5 tracker is set to `review`; Story 1.6 remains backlog and all native gates remain open.

## Suggested Review Order

**Intent and readiness**

- Start with the observed verdict and explicit execution gaps.
  [p0c-native-readiness.md:1](p0c-native-readiness.md#L1)

- Read the canonical capabilities and scope before proposed implementation details.
  [SPEC.md:26](../../specs/spec-image-optimization-1-5-native-measurement-admission/SPEC.md#L26)

**Native execution boundaries**

- Inspect proposed runtime credential pairing and separate platform trust boundaries.
  [p0c-native-entry-proposal.md:47](p0c-native-entry-proposal.md#L47)

- Follow cache identity, actual target draw, frame association and null fallback.
  [p0c-native-measurement-protocol.md:64](p0c-native-measurement-protocol.md#L64)

- Compare physical coverage, nullable counters and open gates with discovered records.
  [p0c-device-matrix.json:1302](p0c-device-matrix.json#L1302)

**Evidence verification**

- Verify one byte snapshot binds private receipt hashing and strict parsing.
  [verify-p0c-readiness.py:518](verify-p0c-readiness.py#L518)

- Check receipt-derived device classes and unsupported simulator metadata rejection.
  [verify-p0c-readiness.py:533](verify-p0c-readiness.py#L533)

**Regression and tracking**

- Exercise valid CLI output and persistent malformed-matrix rejection cases.
  [test_p0c_readiness.py:95](test_p0c_readiness.py#L95)

- Confirm only Story 1.5 advances to preparation review.
  [sprint-status.yaml:44](sprint-status.yaml#L44)
