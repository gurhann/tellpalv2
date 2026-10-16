---
title: 'P2: storage diagnostic and SDK reading retry fix'
type: 'chore'
created: '2026-10-16'
status: 'done'
baseline_commit: '543c7ccf86377e88df74994463807d0f85b5f97d'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p2-storage-probe-investigation.md'
---

<frozen-after-approval reason="human-renegotiated SDK-only storage scope">

## Intent

**Problem:** The real SDK partial-body test exposed repeated resumptions outside the configured deadline; P2 also lacks live GCS evidence.

**Approach:** Use the SDK’s built-in no-retry handler through its reading-only strategy configuration. Complete the diagnostic and SDK fault tests; no storage child-process architecture.

## Boundaries & Constraints

**Always:** Follow ADR-0008/0021/0022 and the mandatory investigation companion, including its approved extension. Only the existing reading adapter’s SDK retry configuration, complete profile identity and evidence change. Preserve legacy behavior, immutable generation/equality, transaction/attempt fences and unrelated changes.

Keep existing byte/memory and between-call deadline checks. Fingerprint the reading SDK retry policy. No custom runtime process/IPC/connection/policy/adapter layer or standalone retry implementation. The diagnostic launcher still bounds its own process/output. Use explicit credentials safely; no implicit ADC or secret output. Socket inactivity timeout is not an absolute in-flight duration guarantee; preserve that production gate.

Dry run opens no credentials/network. Live execution needs explicit target/private credential/receipt approval. One generated `local/reading/p2-storage-probe/<UUID>/fixture.png`, PNG <=64 KiB, four conditional creates, one retained object maximum, 3-second operations and one supervised 60-second run. No new-key retry after uncertainty. These are diagnostic limits, not production profiles.

**Ask First:** Concrete live execution/retention effect; credentials/IAM; broader keys or production admission; unrelated backend changes.

**Never:** Live cloud calls in this build; overwrite/delete/list existing objects; signed URLs/public endpoints; deployment/backfill; credential discovery/creation; billing; P3/CMS/LIVE or production-profile approval. Preserve P0 limitations.

## I/O & Edge-Case Matrix

| Scenario | Required behavior |
| --- | --- |
| Dry run / no execute flag | Reviewable finite scope; zero credential/network I/O. |
| Invalid target/private files/binding | Refuse before cloud I/O; sanitized reason. |
| First create and pinned reread | Actual generation/MIME/size/SHA and independent decoded content match. |
| Equal / changed same-size bytes / wrong MIME conflict | Preserve first generation; only exact equality reuses. |
| Impossible generation / forged metadata | Reject without latest fallback; no historical-retention claim. |
| Metadata/body stall | Actual SDK rejects the stalled read within the observed bound and makes one attempt; legacy options remain unchanged. |
| Slow trickle / absolute in-flight time | Document unproven/deferred; never claim a hard wall-clock bound. |
| Overflow/malformed response/ambiguous upload | Stop and observe owned work; retain evidence; no cleanup/new-key retry. |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/`: `AssetStorageConfiguration#firebaseAssetStorageClient` explicit construction; `FirebaseStorageAssetClient` real SDK; `ReadingObjectStorage#create/read` equality and pinned bytes.
- `asset/domain/reading/ReadingProfiles#capture`, reading service/worker: identity and remaining budget.
- `be/pom.xml`: Java25/Storage2.58.1/JUnit. Companion holds anchors and safeguards.

## Tasks & Acceptance

Java paths below start at `be/src/`. The nine unintegrated storage-process files were removed on human request; preserve existing diagnostic sources.

- [x] `main/java/com/tellpal/v2/asset/infrastructure/storage/FirebaseStorageAssetClient.java#withReadingDeadline` — use existing SDK non-idempotent no-retry handler for both reading classifications via minimal inline strategy configuration; no separate runtime helper class.
- [x] `main/java/com/tellpal/v2/asset/domain/reading/ReadingProfiles.java` — include retry behavior and honest inactivity/absolute-deadline semantics; do not alter service/worker architecture.
- [x] `test/java/com/tellpal/v2/asset/infrastructure/storage/ReadingStorageLiveProbe.java` — explicit modes, real-adapter cases, private receipts.
- [x] `test/java/com/tellpal/v2/asset/infrastructure/storage/ReadingStorageLiveProbeTest.java` — guard, zero-I/O, conflict/pinned, receipt assertions.
- [x] `test/java/com/tellpal/v2/asset/infrastructure/storage/ReadingStorageTransportTest.java` — retain the original body-stall regression and exact one-attempt/generation checks, metadata/wiring and legacy settings assertions.
- [x] `be/tools/reading_storage_probe.py`, `be/tools/test_reading_storage_probe.py` — bounded launcher, actual synthetic process teardown and safe output.
- [x] `be/docs/reading-image-storage-probe.md`, `be/docs/evidence/p2-storage-probe/` — actual logs, packaged proof and separately gated live command.
- [x] `be/docs/adr/ADR-0022-reading-image-preparation-engine.md`, `be/docs/project-memory.md`, `architecture.md` — approved boundary using project standards.

**Acceptance Criteria:**

- Given ordinary Maven tests, when run, then no real credentials/live GCS are accessed.
- Given a dry run, when complete, then exact live scope/command is reviewable before execution approval.
- Given a partial-body stall, when the SDK inactivity deadline expires, then the read fails without internal resumptions; no hard slow-trickle guarantee is inferred.

## Spec Change Log

- 2026-10-16: Human approved tool/test scope with “A”.
- 2026-10-16: Body stall failed at 300 ms with seven resumptions over 2,500 ms. Human accepted absolute child-I/O/no-retry fix with “Tamam uygun”. Renegotiate only this boundary; KEEP working tools/guards and original failure. Live execution remains gated.

- 2026-10-16: Human requested rollback of the extra SDK/process classes and continuation at the earlier point. Remove the nine unintegrated runtime classes; narrow to existing SDK policy configuration. Absolute in-flight I/O protection is deferred explicitly, not silently certified. Keep diagnostic tools, all raw failures and P2 baseline.

## Verification

Focused SDK/process, launcher and dry-run tests; then `bash ./mvnw test` and `bash ./mvnw verify` from `be/`. Prove real packaged SDK fault behavior in pinned Temurin JRE; retain failure/final logs; independent BMad review. Matrix rows need passing tests or explicit documented deferred proof as required. No live/production/P3 PASS follows; preserve original failed evidence.

## Implementation evidence

SDK-only correction and diagnostic completed; nine unintegrated storage-runtime classes were removed. Actual focused 21 checks and post-review Python18/Java7/dry-run checks passed. Full test431 and verify431+1 passed without skips/errors. Pinned Temurin packaged SDK proof passed six pixels, generation pinning and one-attempt metadata/body stalls. See [evidence](../../be/docs/evidence/p2-storage-probe/README.md). Absolute in-flight/slow-trickle proof remains deferred; live GCS was not executed. Three independent review layers completed; six local patches resolved. Post-patch full test/verify and packaged SDK proof passed again. Sprint sync skipped because story_key is unset.

## Suggested Review Order

**Reading SDK correction**

- Use the existing SDK handler to stop inner media resumptions.
  [FirebaseStorageAssetClient.java:131](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/FirebaseStorageAssetClient.java#L131)

- Bind effective policy, implementation bytes and honest timeout semantics to profile identity.
  [ReadingProfiles.java:34](../../be/src/main/java/com/tellpal/v2/asset/domain/reading/ReadingProfiles.java#L34)

- Keep absolute duration, live access and production admission explicitly gated.
  [reading-image-storage-probe.md:3](../../be/docs/reading-image-storage-probe.md#L3)

**Diagnostic boundaries**

- Dry-run returns before credentials; live work remains explicitly selected.
  [ReadingStorageLiveProbe.java:32](../../be/src/test/java/com/tellpal/v2/asset/infrastructure/storage/ReadingStorageLiveProbe.java#L32)

- Verify immutable conflicts and persisted pinned bytes independently.
  [ReadingStorageLiveProbe.java:76](../../be/src/test/java/com/tellpal/v2/asset/infrastructure/storage/ReadingStorageLiveProbe.java#L76)

- Persist exclusive ownership and its directory entry before upload.
  [ProbeReceipt.java:18](../../be/src/test/java/com/tellpal/v2/asset/infrastructure/storage/ProbeReceipt.java#L18)

- Contain diagnostic processes and observe exit through setup errors and SIGTERM.
  [reading_storage_probe.py:30](../../be/tools/reading_storage_probe.py#L30)

- Require complete private worker evidence before reporting live success.
  [reading_storage_probe.py:118](../../be/tools/reading_storage_probe.py#L118)

**Verification and review evidence**

- Retain the actual SDK partial-body failure regression and pinned one-attempt checks.
  [ReadingStorageTransportTest.java:43](../../be/src/test/java/com/tellpal/v2/asset/infrastructure/storage/ReadingStorageTransportTest.java#L43)

- Exercise actual launcher paths, mutation rejection and signal cleanup without credentials.
  [test_reading_storage_probe.py:122](../../be/tools/test_reading_storage_probe.py#L122)

- Inspect full-suite results, packaged runtime proof and remaining limitations.
  [README.md:1](../../be/docs/evidence/p2-storage-probe/README.md#L1)

- Check independent findings and completed local patch dispositions.
  [review-triage.md:1](../../be/docs/evidence/p2-storage-probe/review-triage.md#L1)
