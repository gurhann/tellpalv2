# Story 1.5 preparation completion — r1

Recorded: 2026-10-14. Status: **done for preparation**. Native execution readiness:
**false**. Only the dedicated image-optimization Story 1.5 tracker advances.

The [approved story boundary](../../planning-artifacts/image-optimization/epics.md#story-15-prepare-native-image-measurement-and-admission)
requires a readiness package, concrete entry proposal and measurement protocol.
It explicitly separates native entry implementation/qualification from this
preparation story. The [canonical contract](../../specs/spec-image-optimization-1-5-native-measurement-admission/SPEC.md)
also permits preparation completion with documented gaps. The user's request to
continue and finish accepts this completed preparation handoff; it does not waive
any native execution, candidate, quality or budget gate.

Earlier progress updates combined the preparation story with its separately
authorized entry pilot. This record corrects that tracking distinction while
retaining every failed/not-run entry and the remaining work. The retained
preparation documents and matrix are historical snapshots, not refreshed device
qualification.

## Acceptance evidence

| Capability | Completed preparation evidence |
| --- | --- |
| CAP-1 — observed device/tool/counter inventory | The [retained matrix](p0c-device-matrix.json) and [r3 pilot checkpoint](p0c-selected-pilot-checkpoint-r3.json) distinguish historical preparation, selected physical devices, actual entry observations and missing floor/tablet/RAM/renderer/counter coverage. |
| CAP-2 — reviewable isolated entry | The [entry proposal](p0c-native-entry-proposal.md) specifies targets, trust boundaries, credential delivery, rollback and positive/negative verification. Later accepted implementation and finite device observations remain separately recorded in the [operator status](native-device-preparation-status.md). |
| CAP-3 — candidate eligibility | The [readiness contract](../../specs/spec-image-optimization-1-5-native-measurement-admission/native-readiness-contract.md) and [readiness handoff](p0c-native-readiness.md) bind required source/output/role/demand evidence and identify gaps. Offline codec verification and the 357-byte entry control do not qualify native candidate quality. |
| CAP-4 — comparable measurement definition | The [measurement protocol](p0c-native-measurement-protocol.md) defines three cache states, transfer/decode/target-frame association, separate counters, overhead/drift/pressure and nullable unavailable presentation. |
| CAP-5 — reproducible handoff | The [selected-pilot setup](p0c-selected-pilot-setup-r1.md), existing private preflight/replay tools and r3 checkpoint preserve historical evidence and separate remaining Story 1.6 gates. |

Fresh immutable replay passed **43 source bindings and 42 original tests**.
The verifier, regression suite, matrix, preparation spec, readiness handoff, entry
proposal and measurement proposal remain byte-identical to preparation commit
`3ee8c2d8ef1e5313e5b827d09eaf6915f2d278fd`. The dedicated tracker transition is
intentional; no unrelated tracker changes. This replay did not supply raw private
receipts; the previously recorded 13-receipt replay remains historical.

Fresh offline setup report `setup-4bbc07ab36164d4c` validates **18 current source
bindings**, with expected exit `3`, all gates open and no device/account/network
calls. Existing formal three-layer reviews and their fixes remain recorded in
the original handoffs; no new implementation review is claimed. The r3 checkpoint
binds private report bytes through public aliases/hashes without exporting paths,
device IDs, credentials or private URLs.

## Additional physical pilot results

| Actual device and mode | Current positive entry |
| --- | --- |
| Pixel 7 / API 33 release | r259/r276 passed; exact installed app/test APKs bound. |
| Pixel 7 / API 33 profile | r260/r278 passed; 14 ledger records, zero drops, 128×160 decode. |
| iPhone 12 profile | r281/r302/r300 passed; 14 ledger records, zero drops, 128×160 decode. |
| iPhone 12 release | r312/r328/r329/r330 passed; r331 immediate post-entry ledger has 14 records, zero drops, 128×160 decode. |

All four cases use source freeze
`d386e0e62b2029f57653ee5729a3ef49612c8c2ceeae0e3c31f6f3f22b098935`
(292 inputs). Success means bounded authenticated control entry, draw and
teardown. It does not establish comparative quality, memory budgets, first
physical presentation, P95 or production safety. The two resumed release audit
timeouts remain failed; after the user reported allowing the phone prompt, the
fresh audit and entry passed without changing deadlines or guards.

Owned backend/broker listeners are closed. No new Android reservation, IAM/key
change, signing identity or deployment occurred. Existing private QA credentials
remain retained for unfinished required physical verification under the prior
exact-key cleanup authorization. The external cache/resource controls remain
active; the external volume must stay mounted for development.

This run's regenerable historical replay archive/source copies were retired only
after successful replay and hash verification. The replay report, command evidence
and original Git commit remain. Final free space is 12.18 GiB internal and
6.01 GiB external; original device evidence and signed products are preserved.

## Remaining execution gates

| Gate | Remaining work before Story 1.6 execution |
| --- | --- |
| NATIVE-ENTRY | Full physical negative pin/key/session/UID/fixture/teardown matrix. Two Android profile early-metadata refusals pass; the planned wrong-pin stage remains unqualified and later cases did not run. |
| NATIVE-DEVICE | Counter/renderer support and stated floor/RAM/tablet coverage; selected phone entries do not qualify a fleet. |
| NATIVE-CANDIDATE | Independently admit actual source/reference/candidate/role/geometry/quality cells. |
| NATIVE-PROTOCOL | Freeze device/build/cache/journey/order/repeat/counter batch and stop conditions. |
| NATIVE-DECISIONS | Resolve required quality/resource criteria and parent decisions without adopting production policy from this pilot. |

Android conservative exposure remains **945 USD cents before free credits**;
another complete window projects **1035 cents**, exceeding the unchanged
1000-cent authorization. These are conservative operator values, not realized
invoice amounts. No new complete Android window is permitted. The Cloud alert
does not automatically stop charges. Actual reported cost requires a fresh
Billing observation and cannot be inferred from the quota gauge.

The [dedicated tracker](sprint-status.yaml) records Story 1.5 `done` and Story 1.6
`backlog`. All native gates stay open or partial. This completes preparation and
hands off the explicit entry/batch prerequisites; it does not declare the full
native pilot complete.

## Bounded continuation — 2026-10-14

The [critical iPhone follow-on](story-1-5-critical-ios-handoff-r1.md) now records
four physical release refusals, normal control draw/teardown and independent
file admission of two real JPEG/WebP pairs. This supplements the historical
preparation closure above; full native execution, human quality and Story 1.6
measurement remain unqualified.
