# Actual P2 storage-probe build evidence

Date: 2026-10-16. Host: macOS / Homebrew OpenJDK 25.0.2, Python 3.14.6. Storage SDK: project dependency 2.58.1. The implementation-build evidence was generated without real credentials/cloud operations. A subsequent separately authorized [live storage diagnostic](live-gcs-2026-10-16.md) passed on the selected real target; it is recorded independently below. The approved SDK-only correction changes the reading clone retry strategy and complete profile identity. No storage child/IPC layer, Spring configuration, database, scheduler, legacy client or production admission changed.

| Evidence class | Actual result | Raw evidence |
| --- | --- | --- |
| `LOCAL_CONFIGURATION_AND_GUARDS` | 7 tests PASS: zero-I/O dry run, argument refusal, credential binding/privacy, exclusive receipt, real-adapter conflict/pinned assertions, stop after ambiguous create | [sdk-fix-focused-final.txt](sdk-fix-focused-final.txt) |
| `LOCAL_SDK_FAULT_INJECTION` metadata stall | PASS: actual SDK/adapter, 200-ms configured timeout, bounded failure, one request | [sdk-fix-focused-final.txt](sdk-fix-focused-final.txt) |
| `WIRING_ONLY` | PASS: connect/read/RPC/total timeout settings and one-attempt configuration; legacy options unchanged | [sdk-fix-focused-final.txt](sdk-fix-focused-final.txt) |
| Original `LOCAL_SDK_FAULT_INJECTION` partial body | **Historical FAIL preserved**: 300-ms configured limit; still running at 2,500 ms; seven pinned media attempts; pre-cleanup client stack in SDK body read | [focused-tests-attempt-4.txt](focused-tests-attempt-4.txt), [diagnosis](body-stall-diagnosis.md) |
| Corrected `LOCAL_SDK_FAULT_INJECTION` partial body | **PASS**: 459 ms, one metadata/media request, actual first body byte flushed, generation pinned, SDK rejected and diagnostic child exited normally | [sdk-fix-focused-final.txt](sdk-fix-focused-final.txt) |
| Profile/encoder regression | **11 PASS** within 21-test focused run; new retry policy/semantics/implementation identity are captured; actual existing encoder child remains working | [sdk-fix-focused-final.txt](sdk-fix-focused-final.txt) |
| Original fault-child process containment | PASS independently: actual child forcibly terminated and exit observed before failed body assertion returned | [focused-tests-attempt-4.txt](focused-tests-attempt-4.txt) |
| `LOCAL_LAUNCHER` after independent review | **18 tests PASS**: real dry-run/mutation, SIGTERM and setup-failure teardown, private complete receipt validation, plus actual watchdog/overflow/descendant and mode-isolation cases | [review-patches-python.txt](review-patches-python.txt), [review triage](review-triage.md); earlier 8-test run: [sdk-fix-python-final.txt](sdk-fix-python-final.txt) |
| Pre-review parent independent rerun | 8 Python tests PASS and actual credential-free dry run PASS | [Python](parent-python-tests.txt), [dry run](parent-dry-run.txt) |
| Prepared dry run after review | PASS with explicit Java and nonexistent credential/receipt directories; no Maven/credential/network work | [review-dry-run.txt](review-dry-run.txt); earlier [sdk-fix-dry-run-final.txt](sdk-fix-dry-run-final.txt) |
| Explicit build/classpath preparation | PASS before the final mode separation, with actual supervised JVM dry-run exit; Maven cache required sandbox escalation | [dry-run-preparation-final.txt](dry-run-preparation-final.txt) |
| Packaged Temurin SDK proof | PASS: actual executable JAR in pinned Temurin25; six source sentinels, metadata stall 220 ms, partial-body stall 326 ms, one request each | [post-review output](review-packaged-temurin.txt), [current JAR SHA256](review-packaged-jar-sha256.txt), [earlier output](packaged-temurin-results.txt), [earlier JAR SHA256](packaged-jar-sha256.txt), [helper](PackagedStorageProbe.java), [exact runbook](../../reading-image-storage-probe.md#packaged-sdk-fault-proof) |
| `LIVE_GCS` selected synthetic create/conflict/pinned-decode cases | **PASS**, subsequent explicitly approved run: 81-byte PNG, generation unchanged, four conditional creates on one retained key, complete receipt and child exit verified | [live report](live-gcs-2026-10-16.md), [worker](live-gcs-2026-10-16-receipt.txt), [supervisor](live-gcs-2026-10-16-supervisor.txt), [output](live-gcs-2026-10-16-output.txt) |
| Full Maven `test` / `verify`, independent BMad review gate | Post-review full `test` 431 PASS; `verify` 431 Surefire + 1 Failsafe PASS, no failures/errors/skips; all six local review patches resolved | [final test](review-full-test.txt), [final verify](review-full-verify.txt), [names/counts](test-counts.json), [review](review-triage.md); earlier [test](full-test-sdk-fix.txt)/[verify](full-verify-sdk-fix.txt) |

Failures are retained: [attempt 1](focused-tests-attempt-1.txt) is an import compilation failure corrected to `com.google.cloud.NoCredentials`; [attempt 2](focused-tests-attempt-2.txt) is sandbox loopback bind denial; [attempt 3](focused-tests-attempt-3.txt) first exposed the body fault under a 15-second JUnit timeout. Attempt 4 contains the original bounded separate-process reproduction; the same regression remains enabled and now passes with the SDK-only correction. [Preparation attempt 1](preparation-attempt-1.txt) records sandbox denial writing Maven resolver state; [initial dry-run refusal](dry-run.txt) records its finite public result. The final launcher separates optional build preparation from network-free dry run.

The revised scope's SDK inactivity-stall/one-attempt gate passes through the SDK built-in handler correction. The original failure remains historical evidence. Absolute in-flight slow-trickle/authentication duration is deferred and unproven; diagnostic supervisor containment does not close that production gap. The [operator runbook](../../reading-image-storage-probe.md) documents the exact preparation commands, separate operator-selected live template and retained-object effect. Selected live GCS storage cases now pass separately. Remote connect/TLS outage, absolute in-flight duration, historical generation retention, production admission, P3/CMS/LIVE and broader P0 quality remain unproven; the local packaged SDK proof is scoped to the cases above.


## Matrix coverage audit

All ten storage guard/transport cases in the final full verify ran and passed; none was skipped:

- `ambiguousCreateFailureStopsWithoutLaterCreateAttemptsOrNewKey`
- `invalidLivePrivateInputHasFiniteSanitizedFailureAndNoReceipt`
- `actualAdapterAndWrapperIndependentlyEnforceConflictEqualityAndPinnedMetadata`
- `unknownDuplicateRelativeAndTraversalTargetOptionsAreRefused`
- `receiptCreationIsExclusivePrivateAndPreservesPartialOwnership`
- `rejectsWrongCredentialTypeProjectTokenEndpointAndEmailBindingBeforeKeyParsing`
- `dryRunDoesNotOpenEvenMissingCredentialOrReceiptParents`
- `realSdkPartialMediaStallFailsWithinBoundWithPinnedGenerationAndOneAttempt`
- `realSdkMetadataStallFailsWithinBoundAndMakesOneAttempt`
- `readingCloneSetsTimeoutsAndOneAttemptWithoutChangingLegacyOptions`

Python watchdog/overflow/descendant-pipe cases execute actual children and observe teardown. Absolute slow-trickle qualification is explicitly deferred by the latest human scope; documentation/profile semantics do not claim that proof.

The post-review Java guard run also passed [7/7](review-patches-focused.txt), including real exclusive receipt creation and parent-directory forcing on the host. No review patch changed production code or the SDK retry strategy. Synthetic worker-receipt tests remain local protocol checks; the subsequent [authorized live result](live-gcs-2026-10-16.md) supplies separate actual GCS evidence.

Raw `.txt` command output retains Maven whitespace unchanged; source/document whitespace checks exclude these raw logs. The evidence volume includes original failures and both pre-review and post-review full runs.
