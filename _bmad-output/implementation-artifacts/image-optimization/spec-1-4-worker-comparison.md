---
title: 'Image Optimization Story 1.4 — Frozen Candidate and Worker Comparison'
type: chore
created: 2026-10-10
status: done
baseline_commit: 0f21043f3c0bb4d251fc42668196ab22903d3398
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p0b-worker-measurement-protocol.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/epic-1-context-r4.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Story 1.3 verifies bytes but observes no worker resources.

**Approach:** Measure one local canonical-reference batch for frozen candidate bytes, phase costs and a decision handoff.

## Boundaries & Constraints

**Always:** Preserve r3/v7 inputs, previous stories/batches and build. Reuse two admitted geometries, 14 pairs, 28 separate initial arms, maximum 56 attempts and frozen settings/order. Measure full canonical pixels→RGB→encode and separate full output decode; original-source decode/normalization stays unmeasured. Keep private/image/raw material outside Git; retain failures/not-run rows and separate output/resource validity. Follow the [protocol](p0b-worker-measurement-protocol.md).

**Ask First:** Runtime/geometry/settings/normalization changes, installation, acquisition, expanded pipeline or claims. Approve this concrete scope before image execution.

**Never:** Change app/CMS, dependencies, production, deployment or unrelated trackers. Claim Java/Railway/native quality, universal savings, production P95/budgets or full P0. Visual/reader/native verdicts stay pending; quality-qualified savings null; existing future gates stay open.

## I/O & Edge-Case Matrix

| Scenario | Input/state | Expected behavior | Failure handling |
|---|---|---|---|
| Valid batch | Matching inputs/records | 28 output/resource-valid arms | Derive counts/denominators |
| Drift | Source/runtime/code/old output changed | No further encode | Preserve not-run/history |
| Evidence loss | Unexpected loss/replayed/PID/log forgery | Resource invalid, output separate | Instrumentation stop |
| Bad metrics | Bool/nonfinite/negative duration/bytes, bad unit/order | Reject; missing never zero | Preserve diagnostic; signed RSS delta allowed |
| Sampling gaps | Short stage/delayed tick/unsupported counter | Coverage and null reasons | No inferred peaks or overlapping sums |
| Output failure | Spawn/encode/readback failure/timeout | One retry; retain partial lifecycle/null reasons | Exhaust arm; continue |
| Global stop | Integrity/instrumentation/deadline/disk/cancellation | No later attempts | Retain remainder |
| Unsafe/missing bytes | Existing/symlink/overlap root, absent output | Refuse writes; unavailable delta | No overwrite/substitution |

</frozen-after-approval>

## Code Map

`O=_bmad-output/implementation-artifacts/image-optimization`; all predecessor files are read-only.

- `O/p0a-corpus.json` — frozen pairs/IDs/settings/order and original denominators.
- `O/run-p0b-encoder-pilot.py:21,46,62,113` — exclusive receipts, output cap, projection/save and finite-attempt lifecycle; no source decode.
- `O/verify-p0b-encoder-pilot.py:80,132,156,243,253,312,405` — guards, canonical checks, schedule, integrity, admission, reread and history; old schema stays resource=false.
- `O/p0b-encoder-pilot.json`, `O/p0b-encoder-pilot.md` — actual v2 output/runtime bindings; reverify files rather than copying claimed hashes.
- `O/test-p0b-encoder-pilot.py:370,396,644,727` — history, log, real-save and deadline regression patterns.

## Tasks & Acceptance

**Execution:**

- [x] `O/run-p0b-worker-measurement.py` — admit/freeze scope; collect bounded serial child phases/samples/counters and immutable attempts.
- [x] `O/verify-p0b-worker-measurement.py` — reread outputs/records; derive measurements/coverage/counts; import neither collector nor any encoder.
- [x] `O/test-p0b-worker-measurement.py` — run all matrix cases with synthetic records and tiny actual save/meter seams.
- [x] `O/p0b-comparison.json`, `O/p0b-worker-findings.md` — execute approved batch; publish observations/growth/gaps/OQ proposals; retain private proofs.
- [x] `O/spec-1-4-worker-comparison.md`, `O/sprint-status.yaml` — finish workflow; update only Story 1.4, preserving prior review states.

**Acceptance Criteria:**

- Given matching inputs, when reporting, then every arm retains execution/output/resource status and comparable actual bytes.
- Given raw records, when verifying, then phase time/CPU, live idle/peak/settled RSS, disk and coverage derive with unsupported counters explicit.
- Given final handoff, when considering OQ-2/3/4, then tradeoffs and quality/source/runtime gaps inform proposals without policy adoption.

## Spec Change Log

## Design Notes

Local macOS 27.0.1 arm64/M2/16 GiB, Python 3.14.6/Pillow 12.3.0. OS file cache/background/thermal conditions are uncontrolled. Separate scope/schema preserves old resource=false evidence.

Protocol: 1 Hz external sampling, five-second idle/30-second live settle per encoder/verifier child; one active child. RSS uses macOS proc_taskinfo bytes; phase CPU seconds and wait4 high-water stay separate. Success requires complete telemetry; expected failed lifecycles retain partials, unexpected loss stops. Gaps stay disclosed; no heap/native/container/codec-thread inference.

Limits: 120 seconds/child including idle/settle; 300 seconds/preflight/integrity; 5,400 seconds/batch; 64 MiB/output; 4 GiB/batch; 1 MiB/stdout/stderr, 2 MiB/telemetry, 256 samples/64 events. TERM→two-second grace→KILL/reap. Protocol defines clocks/disk/phase/null semantics.

## Verification

Matching Python: new suite, unchanged pilot suite, corpus/inventory CLI, then real batch/separate verifier sequentially. Rehash predecessors/inputs/build; audit executed matrix tests, code/scope/result/log bindings, syntax/links/whitespace/privacy including untracked files. Retain immutable private proofs. No app builds or remote operations.

## Implementation and Verification Record

Completed 2026-10-10–11; implementation is ready for review. The approved intent and measurement protocol remain unchanged.

- Added bounded serial collector/children, independent raw-record/output verifier and an 85-test synthetic suite. Required live endpoint and boundary handshakes, exact wait4 ownership, retry exhaustion/continuation, finite cleanup, inode-deduplicated disk records and retained stop history are covered.
- Executed `worker-measurement-r3-v1` against the frozen r3/v7 references and actual `encoder-pilot-r3-v2` predecessor. Independent final verification passed: 14 pairs, 28 initial attempts, 28 output-verified and resource-valid arms; zero failures, retries or not-run arms. All new output hashes match separately reread predecessor outputs.
- Published [comparison evidence](p0b-comparison.json) and [worker findings](p0b-worker-findings.md). Retained immutable private scope, attempts, raw samples/events/logs, requests, terminal/final disk records and actual outputs outside Git.
- New suite: 85 passed, no skips. Unchanged pilot suite: 64 passed. Corpus/inventory CLI and predecessor actual-batch reread passed. Source/freeze/predecessor/build preservation, syntax, links, whitespace and public privacy checks passed.
- Scope SHA-256: `7b28e780e1ffae4f5c1e3b5d08f434e4ba5681337f904435b1e928c249fd7ef4`; private execution result: `55d2e73aeba64cf594d68135a69ae8b6a40ffa8efe5cf5279fc5a34062bb9f6f`; original pre-review comparison: `e08ae5e6fc11564c14d7f4e5dc13eb2e0600d678ab4c700890615d4150671102`.

Coverage remains explicit: 109/112 active phases have no periodic sample and retain null phase peaks, while required boundaries/endpoints and terminal records are complete. Event-bounded bootstrap guards exclude earlier imports/request loading, which remain in lifecycle/attempt wall without a fabricated CPU split. This single local observation does not establish visual/native quality, leak behavior, P95 or production budgets. Original-source decode/normalization and unsupported counters remain unmeasured/null; existing OQ-2/3/4/6 and reader/alpha/EXIF/source gates remain open. Quality-qualified savings remain null. Only Story 1.4 moved to review.

## Review Patch Verification Record

Applied all seven accepted medium review findings without changing the frozen intent/protocol, admitted settings/runtime or actual measurement batch. Collector fixes apply to future failure paths: terminate owned descendants after leader reap with grace/KILL, preserve evidence through BrokenPipeError and retain exact leader wait4 ownership. Verifier fixes independently retain output validity through resource failure, reject missing periodic/tail sampling and terminal CPU preceding event counters. Added native API sentinel/return-size and independent shrink/growth denominator regressions.

The original three measurement sources are byte-for-byte [historical witnesses](worker-measurement-r3-v1-code/run-p0b-worker-measurement.py). The current final verifier's `--historical-code-root` option is restricted to final rereading, pins the original scope SHA-256 and rehashes the three exact source/protocol files. Current collector/worker request guards remain unchanged in their requirement for current source identities; no old measurement is attributed to the patched collector. Source-witness Git checkout permissions do not replace their exact hash checks; private raw evidence remains immutable.

- Current suite: **109 passed, no skips, 48.115 seconds**. Unchanged pilot suite: **64 passed, 3.580 seconds**. Inventory/corpus CLI and predecessor actual reread passed again.
- Enhanced final verification: **PASS**, 28 output-verified/resource-valid arms, zero failure/retry/not-run. All original public fields and observations match the original snapshot exactly; only separate final-verification provenance was added.
- Original scope/private-result hashes above remain unchanged. Updated public comparison SHA-256: `1914b17fc73e510d0efee60252661b8982a7193ba0df22a378afc9343deb2796`. The original public snapshot SHA remains recorded above as history.
- The actual batch was not rerun, amended or remeasured. Existing quality/future gates and Story 1.4 review status remain unchanged.

Current final verifier source SHA-256: `3ac9387b2e9ad93c8cdfc38c048fe9b17143d767a77436ac42e4d1929bb66d4a`.

## Delivery Verification

All three independent review layers completed; seven accepted patch findings were resolved and regression-tested. Root final rereading matched the published comparison exactly and preserved every original observation. The 109 current tests, 64 unchanged pilot tests, corpus/inventory and predecessor rereading passed. Frozen intent/protocol, predecessor artifacts, generated build file and immutable actual batch remain preserved. The dedicated tracker remains at review for human acceptance.

## Suggested Review Order

**Scope and interpretation**

- Understand the approved canonical-reference path and measurement limits.
  [p0b-worker-measurement-protocol.md:5](p0b-worker-measurement-protocol.md#L5)

**Evidence identity**

- Separate original measurement sources from the current final verifier.
  [verify-p0b-worker-measurement.py:84](verify-p0b-worker-measurement.py#L84)

**Finite failure handling**

- Inspect owned-group cleanup and durable evidence on pipe failures.
  [run-p0b-worker-measurement.py:130](run-p0b-worker-measurement.py#L130)

**Measurement validation**

- Validate raw endpoints, sampling coverage and non-overlapping phase costs.
  [verify-p0b-worker-measurement.py:202](verify-p0b-worker-measurement.py#L202)

- Follow independent output validity, retained attempts and byte denominators.
  [verify-p0b-worker-measurement.py:393](verify-p0b-worker-measurement.py#L393)

**Decision handoff**

- Review local observations and outstanding codec, worker and growth decisions.
  [p0b-worker-findings.md:24](p0b-worker-findings.md#L24)

- Assess production qualification proposals without adopting local budgets.
  [p0b-worker-findings.md:69](p0b-worker-findings.md#L69)

**Regression and tracking**

- Check counter forgery, coverage and native ABI mapping regressions.
  [test-p0b-worker-measurement.py:50](test-p0b-worker-measurement.py#L50)

- Check descendant cleanup, closed pipes and historical-source trust boundaries.
  [test-p0b-worker-measurement.py:160](test-p0b-worker-measurement.py#L160)

- Check success, retry exhaustion and distinct byte denominators.
  [test-p0b-worker-measurement.py:362](test-p0b-worker-measurement.py#L362)

- Confirm only Story 1.4 advances to human review.
  [sprint-status.yaml:44](sprint-status.yaml#L44)
