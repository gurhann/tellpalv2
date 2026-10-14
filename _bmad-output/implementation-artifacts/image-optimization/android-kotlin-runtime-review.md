# Android Kotlin runtime repair review

Date: 2026-10-14. Baseline: `17f119aa94150992604c647b96a7ba3257af80e4`.
Contract: [approved repair spec](../spec-android-release-instrumentation-kotlin-runtime.md).

Three context-free review layers examined the complete tracked/untracked diff,
SHA-256 `e6df6aecfa30e7b9702fe6f8c762055a718c55d6224b15e6a2aced603c94d7b5`.
The blind layer reported no actionable findings. Root triage classifies the
following edge-case and verification findings as medium-severity patches inside
the approved verification scope; no intent gap, spec loopback or deferred issue
was found.

| Finding | Required patch | Verification |
| --- | --- | --- |
| Non-object build receipt reaches `.get` | Reject its root type before access | Fixed public refusal and exit 4; no traceback |
| Non-object plugin provenance reaches `.get` | Reject its type before access | Fixed provenance refusal |
| Shallow copied-source path indexes a missing ancestor | Validate canonical path and ancestor count first | Fixed provenance refusal |
| Failure receipt write can mask the original refusal | Keep public refusal/exit 4 when diagnostic writing fails | Occupied/unavailable output regression; never convert success-write failure into success |
| Recorded generated/compiled source binding has no targeted regression | Test equal valid source/copy changed together after build | Both current copies must still match recorded build hashes; removed-condition mutant fails |
| Final build-receipt rehash has no targeted regression | Mutate each receipt while SDK/APK bytes stay stable | Exit 4, no accepted receipt; removed-condition mutant fails |

Existing original local r266 and passive-observer r268 release successes remain
immutable evidence of their exact runtime artifacts. Checker/test patches do not
change the selected 292 runtime inputs or require repeating successful native
entry. Fresh checker execution and source/artifact preservation are required
before completion. The real SDK/profile/release/plugin and partial-retention
controls remain separately bound; no paid Cloud trial is part of this repair.

All six patches are complete. The reviewed final run passed **194 native host
tests, 22 runtime tests and 33 native guards**, with zero skips and clean
whitespace. Genuine temporary source/copy and per-receipt mutations exercise the
recorded bindings; both removed-condition mutants fail their targeted assertions
without test errors. Malformed inputs and diagnostic-storage failures retain fixed
public refusal and exit 4, including a successful-receipt write failure.

The patched real SDK checker r272 passes on unchanged r259/r260/r233 APKs, with
15 callable Kotlin references per new mode and the original missing-definition
negatives. Its receipt SHA-256 is
`730eaf2b9fe86f2fa2ce2d0fb99c5eac8fd8fbc355fc3a7691bcba96172cb3c7`.
Selected runtime source remains 292 inputs,
`d386e0e62b2029f57653ee5729a3ef49612c8c2ceeae0e3c31f6f3f22b098935`.
No unchanged build or native launch was repeated for checker-only patches.

The private `kotlin-runtime-reviewed-final-proof-r273/receipt.json` binds current
checker/test bytes, the original SDK/build/native evidence and rehashed old/new
APKs/source copies; SHA-256
`b36f7abd390770968e9240afbeae3197fef1ede4d38d8eb565e3b4b211015e34`.
Original r270 and all failures remain preserved. The approved frozen intent is
unchanged. All test-owned processes/listeners are closed; physical Android
release/negative coverage and Story 1.5/performance acceptance remain open.
