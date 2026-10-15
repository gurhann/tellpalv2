# Story 1.6 final review

Date: 2026-10-16. Baseline: `7a05dd6588bafb74dcd92bc0fb5388ee50885199`.

Three context-free reviewers inspected the complete tracked/untracked diff concurrently. All returned before triage. The reviewed diff contained196625bytes, SHA-256 `fcea233078dc505def21b8f604cf5b9b55292d8ed917c058c89ba09e5d58ad8f`. The blind reviewer reported no actionable findings. Earlier interrupted review outputs were unavailable; this review replaces that incomplete collection.

| Finding | Severity | Route | Resolution |
| --- | --- | --- | --- |
| Joining an assigned but unstarted capture worker can mask its startup failure and prevent subsequent cleanup. | Medium | patch | Join only a thread with an assigned identity; verify the original exception survives and an incomplete worker cannot qualify. |
| Matrix source equality permits all arms to omit the same provenance identities. | High | patch | Require a dictionary containing five valid SHA-256 source/preparation identities on every capture before cross-arm equality. Test missing, empty, boolean and malformed identities across every arm. |
| Direct observer helper tests do not exercise actual launcher batch routing. | Medium | patch | Run all three reader-quality batches through actual `onCreate`/`onStart`,200ms UI responses, device facts and authenticated settlement. Preserve legacy150ms refusal. A compiled mutant selecting the legacy observer must fail these tests. |
| The3×5 source fixture rounds1× and0.75× to identical dimensions. | Medium | patch | Use30×50 originals and20×30 slots; independently reproduce0.75× and reject substituted1× dimensions. |
| Earlier verification-gap review identified missing pre-dispatch quality-source refusal coverage. | Medium | patch | Call the actual reader entry with missing sources and assert prerequisite, broker/authentication and native dispatch are never called; retain a failed receipt. |

The captured intent and acceptance boundaries are unchanged. These are implementation/test patches that require no new product decision. No deferred or unresolved code finding remains.

## Verification

- 381 portable Python tests passed, including the expanded quality/source/worker tests. A pre-existing boundary test's frozen monotonic root was made exactly representable after an intermediate full-suite failure; the actual180s/150s guards were unchanged.
- 65 real Kotlin host cases passed, including all three quality batches through actual launcher entry. Native propagation, startup, cleanup and finish-ack causal mutants remain enforced; the new batch-dispatch mutant compiled successfully and failed its named runtime assertion as required.
- Four native harness/launcher guard tests passed with retained raw output.
- Four private in-memory mutants independently removed the new provenance guard, restored wrong0.75× sizing, removed early source admission, and restored the unstarted-thread join. Each was detected by its corresponding regression. Repository sources were never mutated by those controls.
- The original gallery runtime error was reproduced before repair. Six groups/eighteen variants and selection/zoom controls passed JavaScript execution against a DOM test double; this is not a browser-rendering result. All18 original screenshot hashes remained unchanged.

Intermediate local failures, their logs, final receipts and the successful Kotlin harness snapshot are retained in the surviving-evidence supplement. These counts are separate scopes, not totals to add.

## Source and evidence scope

Physical quality captures retain recorded source `a1e827ddf2285b6d4babade85dd62b9142ea2f973a21a0ce28c1e0cefe1fd0d3`. Final selected repository source, including expanded harness/tests, is `8f51bd5afb28af528a5bca374dc3161eabb2ced8eb8fb59dcd74d102ef174b57` across310files. The final review changed host collector guards and test fixtures, not the signed native instrumentation. No physical image is relabelled as produced by the later repository snapshot, and no new cloud/device operation was needed for these patches.

The original temporary quality root is now unavailable. Its original native ledgers, sidecars, signed build receipt, cleanup originals and historical private-test receipts could not be re-read. Their hashes and structured observations remain recorded, but the new archive must not be called a full original-root archive. [The retention audit](p0f-evidence-retention.json) identifies surviving screenshots, unavailable paths and the resulting independent-audit limitation. The parent performance archive checksum was reverified as `60d535e77971990d12dd82135448e6e4dc7f81dbc0d00446bc324958ab80237d`; its old contents were not requalified.
