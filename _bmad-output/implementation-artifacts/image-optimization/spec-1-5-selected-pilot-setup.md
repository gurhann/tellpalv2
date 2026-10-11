---
title: 'Story 1.5 — Set Up the Selected Native Pilot'
type: 'chore'
created: '2026-10-11'
status: 'done'
review_loop_iteration: 0
baseline_commit: 3ee8c2d8ef1e5313e5b827d09eaf6915f2d278fd
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/SPEC.md'
  - '{project-root}/_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-pilot-scope.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The user selected local physical iPhone 12 and remote physical Android through Android Device Streaming. Historical preparation cannot establish current runnable hardware or Streaming access, and updating the canonical spec invalidates its old current-source verifier.

**Approach:** Implement private selected-pilot preflight and setup guidance, plus immutable historical preparation replay. Preserve platform selection separately from observation, entry verification and measurement admission.

## Boundaries & Constraints

**Always:** Follow the canonical SPEC and companions. Keep original P0c evidence/matrix/verifier/tests/proposals/completed handoff byte-identical. Reports use opaque aliases, timestamps, provenance and nullable unknowns; failed probes are not device absence. All five native gates stay open and `native_execution_ready=false`. Real QA retains tools/mobile-test-account.json; no credentials are needed here. Use bounded read-only subprocesses with argument arrays, private unique output directories outside the repository, restrictive modes and sanitized terminal summaries. Record new source hashes without updating old ones.

**Ask First:** Missing account/project/quota information may be requested while independent work continues; paid reservations, login, tool installation or external uploads require concrete scope before that later step.

**Never:** Change app/backend/native builds/signing, use production, exchange tokens, serve candidates, install/boot devices, start/reserve/remove cloud sessions, run benchmarks, synchronize unrelated trackers, or infer model/RAM/counter support from selection or streamed video.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
| --- | --- | --- | --- |
| Valid pilot | Selected platforms, incomplete external access | Valid private report; gates open; concrete gaps | Distinct blocked readiness status |
| Invalid configuration | Wrong UID/provider/platform; boolean/nonfinite numeric; secret/unrecognized input | Reject without echoing private values | Nonzero, sanitized error |
| Missing or failed probe | Missing executable, timeout, malformed/device-service failure | Unknown value and reason; no invented absence | Retain bounded private receipt |
| Unsafe output | Repository path, symlink escape, reused/nonprivate directory | Refuse writing | Nonzero sanitized error |
| Changed sources | Supplied snapshot hash no longer matches | Reject stale current claim | Explicit recheck required |
| Historical replay | Original snapshot versus changed checkout | Original verifier and 42 tests pass in private archive; current evidence untouched | Nonzero on tampering/missing commit |

</frozen-after-approval>

## Code Map

- `tools/mobile-test-account.json` — canonical real QA UID; selection only, not account verification.
- `_bmad-output/implementation-artifacts/image-optimization/p0c-device-matrix.json` — 43 source hashes, 13 private receipts; immutable preparation snapshot is baseline_commit above, original source baseline is `7e3ee8f58a210196ae6988dfa38481d5bd9a7957`.
- `verify-p0c-readiness.py` / `test_p0c_readiness.py` in that directory — archived ROOT plus read-only Git baseline queries; replay can use private archive with child GIT_DIR pointing to current repository.
- `p0c-native-entry-proposal.md` / `p0c-native-measurement-protocol.md` — future entry/measurement, not implemented by this increment.
- `app/mobile/lib/main_library_dev.dart:16` / `app/mobile/lib/app/library_dev_harness.dart:195` — release guards; read-only source bindings.
- Installed `android` is the legacy SDK launcher, not the current Streaming CLI; distinguish with successful `android -V` and `android device remote -h`. Android Studio was not found. ADB/devicectl sandbox errors are access failures.

## Tasks & Acceptance

**Execution:**
- [x] `tools/image-optimization/native_pilot_preflight.py` — stdlib-only private config validation and explicit read-only collection. Separate selection from observation; default offline plan requires no account. Optional local collection probes tool identities and ADB/devicectl inventory with finite timeout/size caps; cloud login/reservation/auth commands are never run. Document strict input/output schema and exit statuses. Hash selected current source inputs; config snapshot claims must match. Treat raw outputs/IDs/paths as private, including parse errors.
- [x] `tools/image-optimization/replay_p0c_preparation.py` — replay fixed preparation commit in unique private temp archive; safe bounded extraction rejects traversal/links, original verifier/tests unchanged, Git reads only, optional private receipts path. Clearly label result historical, never current readiness.
- [x] `tools/image-optimization/test_native_pilot_setup.py` — meaningful matrix rejection and subprocess/privacy tests using synthetic receipts; run all rows without skips. Native/cloud behavior remains unverified.
- [x] `tools/image-optimization/README.md` — commands/schema/statuses, iPhone connection/unlock/Developer Mode and Streaming client/project/IAM/quota setup, token-free broker/backend reachability check before credentials, mode/counter limitations, reservation ownership/teardown for later manual execution. Cite official docs; do not execute install/login/reservation commands.
- [x] `_bmad-output/implementation-artifacts/image-optimization/p0c-selected-pilot-setup-r1.md` — current setup index, hashes/verification receipts and actual gaps, links to historical replay and canonical scope. Keep Story1.6 backlog and original Story1.5 prep review; record this prerequisite separately in dedicated tracker without touching shared status.

**Acceptance Criteria:**
- Given changed canonical sources, when historical replay runs, then original 43 bindings and 42 tests pass against their snapshot, without rewriting preparation evidence.
- Given no native/client/account qualification, when setup completes, then all five gate limitations and next actions are explicit and no benchmark, signed artifact or physical qualification is claimed.
- Given a read-only collection, when a local tool fails or is unavailable, then the private result preserves provenance and a sanitized summary contains no raw IDs, private paths, credentials or command output.

## Spec Change Log

## Design Notes

The user's platform selection authorizes this setup increment; it does not accept every proposed crypto/native-target mechanism. Do not promote selected iPhone12 to currently connected or an ADB serial to a verified cloud physical device. Routine schema details can be resolved consistently in code, documentation and tests. Historical replay may retain its private archive for investigation; report its location only in private output.

## Verification

- `python3 -B -m unittest discover -s tools/image-optimization -p test_native_pilot_setup.py` — all matrix rows pass without skips.
- Run new preflight in offline/default and local-collection modes with private outputs, retaining failed observations.
- Run historical replay and its original tests; optionally rehash original private receipts when available.
- `git diff --check` and scoped preservation/privacy/link checks; no Flutter/native build is warranted for this host-only increment.

## Suggested Review Order

**Selected scope**

- Read the selected platforms and retained native admission boundaries.
  [SPEC.md:27](../../specs/spec-image-optimization-1-5-native-measurement-admission/SPEC.md#L27)

- Separate user-selected platforms from verified devices and project access.
  [native-pilot-scope.md:3](../../specs/spec-image-optimization-1-5-native-measurement-admission/native-pilot-scope.md#L3)

**Private preflight**

- Reject stale, malformed or unsafe inputs before read-only collection.
  [native_pilot_preflight.py:190](../../../tools/image-optimization/native_pilot_preflight.py#L190)

- Bound private subprocess outputs and preserve failure provenance.
  [native_pilot_preflight.py:228](../../../tools/image-optimization/native_pilot_preflight.py#L228)

- Retain observed inventory without promoting runnable qualification.
  [native_pilot_preflight.py:421](../../../tools/image-optimization/native_pilot_preflight.py#L421)

**Historical evidence**

- Replay immutable preparation without rewriting current evidence.
  [replay_p0c_preparation.py:332](../../../tools/image-optimization/replay_p0c_preparation.py#L332)

**Handoff and verification**

- Inspect actual receipts, remaining gaps and next actions.
  [p0c-selected-pilot-setup-r1.md:1](p0c-selected-pilot-setup-r1.md#L1)

- Follow documented setup and strict report schemas.
  [README.md:1](../../../tools/image-optimization/README.md#L1)

- Review successful-device propagation and malformed-output regression checks.
  [test_native_pilot_setup.py:416](../../../tools/image-optimization/test_native_pilot_setup.py#L416)
