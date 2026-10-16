# P2 bounded engine verification

Date: 2026-10-16. Baseline: `7dafccf22862decb001a56cd5b904480b1528912`; branch `codex/image-optimization-p2-generation`. Status: approved bounded engine implemented, independently reviewed, patched and verified; BMad review complete. Production profiles/admission remain closed.

## Executed checks

| Check | Observed outcome | Raw evidence |
| --- | --- | --- |
| Full Maven `test`, final sources | 421 tests, zero failure/error/skip | [Accepted run](final-full-test.txt) |
| Full Maven `verify`, final reviewed sources | 421 Surefire tests and 1 Failsafe documentation integration test, zero failure/error/skip | [Accepted verify](final-full-verify.txt) |
| P2 matrix plus module boundaries inside that verify | 39 tests: 24 PostgreSQL preparation/guard, 11 process/storage/resource, 2 content snapshot unit and 2 module tests; not additional to the 421 | [Machine-readable names/counts](final-test-counts.json) |
| Actual host and deployment-base ImageIO | Real PNG/JPEG encode, disk write, fresh decoder reread; PNG pixel equality; missing/truncated negatives | [Preflight source/results/reproduction](../p2-imageio-preflight/README.md) |
| Actual packaged engine child | Executable Spring Boot JAR launched the real PNG child in official Temurin 25 JRE, observed exit, independently reopened 40×30 PNG with four fixed expected RGB source-content sentinels and source/hash checks | [Latest raw results](packaged-final-temurin-results.txt), [tested JAR SHA-256](packaged-final-jar-sha256.txt), [probe source](../p2-imageio-preflight/PackagedReadingProbe.java) |

Tests used real PostgreSQL 15 (observed server 15.19) through Testcontainers on existing local Colima, Docker 29.5.2/aarch64. Host Java was Homebrew 25.0.2. The packaged run used `eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64`, Adoptium 25.0.4.1+1-LTS/Linux/aarch64. It had no network, read-only root, one CPU, 768 MiB memory/swap, 128 PIDs and 64 MiB tmpfs. Parent heap/direct limits were 256/32 MiB; its declared synthetic parent reservation was 32 MiB; the explicit synthetic child profile appears in raw output. These numbers are test conditions, not production resource admission or continuous peak-memory measurements.

## Frozen matrix audit

Tests below are in `be/src/test/java/com/tellpal/v2/asset/reading/` unless noted. Every named test ran and passed in the final accepted verify; none was skipped.

| Required row | Covering tests and evidence boundary |
| --- | --- |
| Complete language snapshot | `completeStoredRoleSetDeduplicatesAndReverifiesIntegrity`; `contentExactLanguageSnapshotBecomesInvalidAfterLastPageDeletionWhileReadySurvives` uses real content/page rows and the Spring snapshot service. |
| Missing/corrupt/partial/unsupported | `storageRejectsWrongVersionMissingTruncatedAndTransactionIo`, `persistedWrongMimeShaDimensionsAndEncodedLimitCannotVerify`, `validCrcAnimationColorAndOversizedMetadataAreExplicitlyRejected`, `wrongGeometryChildCannotCertifyEvenAValidPersistedPng`, `restartAfterPartialObjectsReverifiesAndCompletesWithoutPartialReady`. |
| Duplicate/restart/conflict | `completeStoredRoleSetDeduplicatesAndReverifiesIntegrity`, `postgresDuplicateScheduleAndSuccessFailureRacePreserveFirstTerminalResult`, `restartAfterPartialObjectsReverifiesAndCompletesWithoutPartialReady`; real create-only disk conflict and pinned reread are exercised. Restart is worker recreation/recovery from persisted PostgreSQL work, not a physical host reboot. |
| Expired/replaced/contradictory/retry | `explicitRetryAndLateTerminalCallbacksRemainFenced`, `concurrentClaimAndExpiredReplacementAllowOnlyOneOwner`, `postgresDuplicateScheduleAndSuccessFailureRacePreserveFirstTerminalResult`; `manifestAndCompletionRollbackTogetherOnDatabaseFailure` forces a database trigger failure to prove atomic rollback. |
| Same-ID bytes/profile/order/language | `sourceByteLanguageChangesAndInvalidOrProductionInputsFailClosed`, `sameBytesWithReorderedPagesOrChangedRoleGeometryCreateDifferentRevisions`, `identicalOriginalBytesAtANewStorageGenerationReuseTheVerifiedRevision`. |
| Empty/pages removed/retained provenance | `contentExactLanguageSnapshotBecomesInvalidAfterLastPageDeletionWhileReadySurvives`, plus `content/reading/ReadingImageSourceSnapshotTest`; old immutable READY remains after actual editable page deletion and the new snapshot is invalid. |
| Timeout/resources | `timeoutAndConcurrencyObserveActualChildExit`, `resourceAdmissionAndActualRssOrDiskExhaustionFailWithObservedExit`, malformed/byte/pixel/frame/metadata negatives. Actual stress children are killed and their exit observed. This is not a production multi-replica/container stress benchmark. |

Additional tests `equalPngBytesRequireExplicitGrowthAdmissionBeforeRealWorkerCanComplete` and `growingPngBytesRequireExplicitGrowthAdmissionBeforeRealWorkerCanComplete` exercise real byte comparison: denial yields no READY; explicit synthetic profile evidence admits the separately identified work. `longFailureEvidenceDoesNotPreventOwnedFailureAndDeduplicatesReplay` proves that long diagnostics do not exceed an indexed-text limit and block owned failure. A simulated active transaction rejects storage I/O; actual source/conversion/upload/readback flows execute outside job persistence transactions.

## Review guard regression coverage

All nine accepted review findings were patched without changing the frozen scope or production gates. The focused post-review run passed 39 tests (11 process, 14 preparation, 10 PostgreSQL guard, 2 snapshot and 2 module tests), with no failures/errors/skips: [raw log](review-rss-transition-focused.txt). See the [review triage](review-triage.md).

- `leaseExpiryDuringRealPostgresRowLockWaitRejectsBothSuccessAndFailure` observes actual PostgreSQL lock waits beyond lease expiry; `expiredUnreplacedProcessingTokenRejectsBothFirstTerminalCallbacks` separately checks unreplaced tokens.
- `finalCaptureAndFinalBindingRecheckCannotSchedulePastTotalBudget`, `readyFindUsesOneTotalBudgetAcrossAllPersistedOutputs`, `workerTotalBudgetExpiryRecordsOwnedFailureAndReleasesParentCapacity` and `scheduleAndSuccessCannotCommitWhenTotalBudgetExpiresInPostgresLockWait` cover total duration through final persistence. Expired work leaves no new READY; valid ownership can still record FAILED.
- `parentPrepareRejectsExcessiveActualHeapReservationBeforeAnySourceIo` and `parentHeapAdmissionRejectsUnboundedReservationBeforeChildLaunch` reject allocation before source/child work; `exitingChildMayLoseItsRssStatusButLiveProbeErrorsStillFailClosed` distinguishes actual exit from a live unobservable child. `missingRssWithTransientAliveStateRequiresBoundedConfirmedExit` deterministically exposes a transient alive notification after OS RSS disappearance and requires confirmed exit through a single bounded 20 ms wait. Live probe errors still reject work and force observed teardown.
- `readCommittedCallerStillGetsAnIndependentRepeatableReadCommittedSnapshot` proves an independent committed RR view under an actual outer READ_COMMITTED transaction and concurrent edit.
- `currentOriginalOrRegistryBindingChangingInsideRetainedCreateCannotScheduleStaleCapture` changes generation/path at a capture barrier; no new job/manifest appears and prior READY remains readable.
- `realChildRendersDirectOriginalAndVerifiesPersistedPixels` checks independently known RGB quadrants after real child conversion and disk reread, and rejects a valid blank PNG mutant. `jpegOriginalCompletesThroughCaptureActualChildAndIndependentStoredPngPixelDecode` exercises admitted baseline JFIF JPEG through the complete real engine path; tolerance is limited to synthetic JPEG source colors, not a quality approval.

## Failures retained and corrected

- [Initial focused failures](focused-initial-failures.txt): null diagnostic formatting in test assertions; corrected without weakening expected states.
- [Concurrent-terminal test failure](focused-terminal-race-failure.txt): Spring repository exception translation hid the expected ownership-rejection type. A dedicated domain rejection exception now preserves fenced callback handling.
- [Initial full test failure](full-test-schema-expectation-failure.txt): two existing migration tests expected schema 34 after V35 was added; expectations now require 35 while retaining business assertions.
- [First post-review full verify RSS failure](review-full-verify-rss-failure.txt): one guard-test setup conversion was rejected because RSS became unavailable during a macOS child-exit transition. The fail-closed rejection was retained; bounded exit confirmation and a deterministic transition regression address the supervisor race. This failed run is not counted as acceptance.
- Earlier accepted runs [full test 403](full-test-accepted.txt), [matrix verify 408+1](matrix-verify-accepted.txt) and [post-review full test 420](review-full-test.txt) are historical, not the final reviewed version or extra counts.
- [Earlier accepted verify](full-verify-before-schema-hardening.txt) and [schema-hardened verify](full-verify-accepted.txt) are earlier separate runs, not extra tests to add to the final count.
- Initial sandbox `ps` restriction was observed in tool output but that initial raw command output was not captured to a file; no retained raw log is invented. Escalated actual process tests passed. The separate Colima mount failure is retained with the codec preflight.

## Limits and remaining work

Storage fixtures persist actual bytes on disk and model immutable generations/create-only operations. The compiled GCS 2.58.1 adapter uses generation-pinned reads, does-not-exist writes and reading-only RPC/connect/read settings. No live Firebase/GCS network write, immutable-generation race or live timeout was exercised; local files and SDK compilation do not establish that external-service proof.

The packaged probe exercises actual engine child/provider/runtime/byte persistence, not a deployed server, production access or the whole job/publication journey inside a deployment. Resource samples can miss peaks; local reservations are not fleet-wide limits. No production codec/geometry/budget, native mobile quality, savings, crash prevention, broader normalization or complete P0 qualification follows.

P0 Android 1×/iPhone 0.75× approval remains limited to the shown TR71/TR19 geometries; Android 0.75× text blur, unresolved JPEG/WebP sharpness and the missing original raw-evidence limitation persist. No P0 originals were recreated by assertion.

P3 Publish/CMS/current-generation invalidation/LIVE adoption remains unimplemented. Retained READY preservation is not a test of eligible LIVE rollback or first-publication access. Existing legacy Publish/status/consumer behavior is unchanged. Production profiles, protected media/renewal/cache/session/retention, deployment, backfill and deletion remain unapproved.

## Subsequent storage-only live diagnostic

The engine-acceptance evidence above is historical. A separately authorized [2026-10-16 live GCS diagnostic](../p2-storage-probe/live-gcs-2026-10-16.md) subsequently passed the selected synthetic immutable create/conflict and pinned stored-byte/decode cases. This does not extend the earlier engine run into an integrated production/READY claim or qualify remote outages, slow trickles, production profiles, P0 quality or P3.
