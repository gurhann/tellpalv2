# Story 1.5 bounded iPhone follow-on — r1

Recorded: 2026-10-14. Status: **bounded preparation follow-on verified**.
Story 1.5 preparation remains `done`; Story 1.6 remains `backlog` and
`native_execution_ready=false`.

## Accepted boundary

The user accepted continuing with essential checks and a small real-image pilot
after the distinction between preparation and full qualification was explained.
The [execution spec](spec-1-5-critical-ios-handoff.md) records that continuation.
This handoff completes the critical iPhone checks and file admission needed to
prepare the visual comparison. It does not replace the full entry contract,
run a Story 1.6 batch or provide a human visual verdict.

The already installed iPhone 12 **release** app and native runner were reused.
All **292** signed-build inputs still match source SHA-256
`d386e0e62b2029f57653ee5729a3ef49612c8c2ceeae0e3c31f6f3f22b098935`.
No Flutter/native/backend runtime, signing identity, dependency lock, account,
IAM role, production configuration or ordinary installed application changed.
The existing canonical QA account was renewed through the previously accepted
existing-key login helper. The isolated local backend reused its unchanged
artifact/config/database, with fresh process/listener and admission bindings.

## Physical observations

The new optional [observer](../../../tools/image-optimization/critical_ios_guard_pilot.py)
calls the original `verify_entry`, validators and cleanup. Only test-returned
metadata, envelope or fixture bytes are faulted. Expected XCTest failures remain
failures; `tool_failed` alone cannot qualify a guard.

| Case | Actual iPhone release evidence | Result |
| --- | --- | --- |
| Alternate certificate trust | Valid alternate DER and matching pin; actual server certificate unchanged. `NATIVE_PROBE` / `refused`; zero probes, pairing, claims or fixture dispatch. | Observed |
| Expired envelope | Canonical admission succeeded; only envelope expiry was changed. `NATIVE_ENVELOPE_EXPIRED` / `refused`; zero fixture dispatch. | Observed |
| Wrong UID envelope | Canonical admission succeeded; a synthetic mismatching UID was injected into the envelope. `NATIVE_ENVELOPE_BINDING` / `refused`; zero fixture dispatch. | Observed |
| Corrupt fixture bytes | One changed byte, unchanged manifest. Native `refused`; original delivery guard rejected. Fresh device ledger: 11 records, zero drops, `decode_refused`, owned release, no `encoded_verified`, `decoded` or `draw`. | Observed |
| Normal entry and teardown | Native `settled`; exact control delivery, explicit authenticated app revoke and closed owned host resources. Fresh device ledger: 14 records, zero drops, ordered verify/decode/draw/revoke and owned release; 128×160 decode. | Observed |

Corrupt and normal ledgers match the **fresh session-derived provider and
attempt hashes**, not merely generation zero or an older file's case name.
The [checkpoint](p0c-critical-ios-checkpoint-r1.json) retains the five results and
**28** rehashed private evidence bindings. Original device reports and traces
stay private. The first alternate-trust invocation did not record the optional
observer-file hash before dispatch; its actual native/trace/boundary evidence was
revalidated with the stable observer. Later invocations bind that hash directly.

These cases qualify the selected native trust/envelope/integrity boundaries.
Expired envelope is not active-session natural expiry; wrong envelope UID is not
sign-in as another real user. Release observations do not certify profile,
Android, other devices or the full pin/key/session/UID/fixture/teardown matrix.

Two fresh installed-byte audits passed. The first audit's unchanged deadline
timed out and remains a failure. A later wrong-UID preflight refused an audit
older than five minutes **before physical dispatch**; a fresh audit and unique
prerequisite then passed. Neither failed preparation counts as a physical guard
result, and no deadline, timestamp or validator was relaxed.

## Actual artwork admission

The [file admission record](ios-visual-preview-admission-r1.json) independently
rehashes two retained originals, their complete canonical RGBA reference streams,
the frozen preparation/scope and four existing JPEG/WebP outputs. Independent
Pillow 12.0.0 format/decode/metadata checks passed; output RGB hashes match the
retained encoder records. Retained source RGB decode expectations are unavailable
and remain explicitly null. Decoder diversity is not claimed.

| Retained specimen | Geometry | JPEG q85 control | WebP q85 candidate | Raw bytes reduction |
| --- | --- | ---: | ---: | ---: |
| Content 71 / page 1 / TR — lettering and diacritics | 1712×2778 | 669,608 | 294,450 | 56.03% |
| Content 19 / page 1 / TR — normalized ICC source | 1284×2279 | 508,346 | 244,442 | 51.91% |

The denominator is each same-geometry normalized JPEG q85 control, not original
source bytes, journey transfer, application memory or quality-qualified savings.
The original r3 corpus/encoder evidence remains unchanged.

A new strict private four-case manifest passed the original `load_manifest`.
Its declared purpose is **isolated raster preview**, with requested 280×460
logical slot, DPR 3, centered `contain`, identical within each pair. Actual native
viewport/DPR/layout and artwork decode are **unobserved/not run**. The purpose ID
links this admission record; the ID's syntax check alone is not acceptance proof.
The existing fixture auto-tears down after drawing, so a later human visual
comparison needs its own explicitly admitted observation method. Readability,
framing/color approval, a real reader journey and latency/memory remain pending.

## Verification and resource closure

- Locked offline command: `uv run --locked --offline --python 3.11 python -B -m unittest test_critical_ios_guard_pilot -q` from `tools/image-optimization`: **25 tests passed**. Synthetic mutation, original driver execution, bounded real child-process traces, cleanup/restore, stale ledger and false-success rejection are covered.
- Three review layers found four verification gaps, all corrected: required
  event-specific identity fields, device release/revoke evidence, consistent
  normal delivery counters, and independent Python/Dart identity verification.
  One temporary Flutter test invoked the actual Dart cache/provider/geometry
  implementation; its two synthetic contain/cover vectors exactly match Python.
  The [shared vectors](../../../tools/image-optimization/fixtures/critical-ios-provider-vector-r1.json)
  are pinned by the new regression. Both original device ledgers and all five
  observations passed the stricter verifier without repeating physical tests.
- Signed source/artifact bindings were checked before physical work and rehashed
  after it. New observer/test filenames remain outside the signed runtime input
  selection, and no rebuild/reinstallation was required.
- Public reports contain hashes/aliases and explicit unavailable results, with no
  credentials, private media/paths, device identifiers or download URLs.
- Only this run's PID/start/command-bound backend was stopped. The actual backend
  and configured broker endpoints are closed; private originals are retained.
  No Android reservation or new Cloud allocation occurred. The existing QA key
  remains retained for unfinished full physical verification under its existing
  exact-key cleanup obligation.

## Remaining wider scope

Full physical profile/release negative qualification, deferred platform/floor/RAM/
tablet coverage, actual artwork layout/native decoding and human visual review,
counter support, and the frozen Story 1.6 quality/resource batch remain separate.
The full `NATIVE-ENTRY` and other affected execution gates remain open. This
bounded handoff does not approve a production codec/profile or numerical budget.
