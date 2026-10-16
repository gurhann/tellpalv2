# P2 Storage Verification Preparation

Date: 2026-10-16. Baseline: `543c7ccf86377e88df74994463807d0f85b5f97d`.
Status: investigation and proposed execution plan only; no live storage or credential operation performed.

## Intent and preserved state

The user selected live-storage verification as the next P2 activity and asked to continue preparing its narrow test plan and runnable tool. This does not approve production profiles, P3 orchestration, deployment, backfill, deletion, new access policies or credential creation. The existing unrelated `.memlog.md` modification remains untouched. The current P2 branch is relevant to this continuation.

The engine already has real PostgreSQL, actual child encoder, persisted local output and packaged Temurin JRE evidence. Its remaining storage limitation is explicit: disk generation fixtures and SDK compilation are not live Firebase/GCS evidence.

## Reuse map

- `be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/AssetStorageConfiguration.java#firebaseAssetStorageClient`: direct `ServiceAccountCredentials` / `StorageOptions` construction example. Do not start this Spring configuration or a DB to run the probe.
- `FirebaseStorageAssetClient`: package-private production adapter. Put the standalone probe in the same package under test sources; no visibility change or production JAR entry is necessary.
- `FirebaseStorageAssetClient#withReadingDeadline`: creates reading-only SDK/transport settings with one attempt and explicit timeout values; legacy storage options must remain unchanged.
- `observeVersion`, `openVersion`, `createImmutable`: actual generation-pinned metadata/read and does-not-exist precondition paths. Adapter create conflict alone does not check payload equality.
- `ReadingObjectStorage#create/read`: actual MIME/size/SHA equality, bounded reads and no-I/O-in-transaction guard. Reuse this wrapper rather than recreate its rules.
- `AssetStorageClientRegistry(List.of(client))`, `AssetStorageFirebaseProperties`: construct a real isolated adapter without fake runtime wiring.
- `be/pom.xml`: Java 25, Google Cloud Storage 2.58.1, Surefire tests and Failsafe `*IT` convention. A manually invoked `main` under test sources must not match automatic cloud-test execution.

## Proposed live scope, requiring later concrete execution authorization

Use an explicitly selected project, bucket and existing private credential file. Do not discover ADC/home credentials, create keys, change IAM, list existing objects or infer a bucket from an email. Validate the service-account project and Google token endpoints before network use. Repository local configuration identifies an existing credential path, but this investigation has neither opened nor validated it.

The tool generates one fresh run UUID and one exact key, `local/reading/p2-storage-probe/<UUID>/fixture.png`. No custom object key, alternate prefix, production path, leading slash or traversal is accepted. Write an exclusive private receipt describing ownership before the first possible upload. The deterministic synthetic opaque PNG is at most 64 KiB; the tool accepts no user asset/source path.

The entire live run has one supervised 60-second deadline and observed child termination, with three-second operation limits. These are diagnostic conditions, not production profiles. There are four create attempts against the same generated key and at most one retained cloud object; every write uses does-not-exist. SDK subrequests and OAuth calls are not falsely advertised as a fixed HTTP request count. There is no retry with a fresh key or automatic rerun after ambiguous upload failure.

| Case | Check |
| --- | --- |
| Initial creation | Reobserve generation, pinned reread, stored MIME/size/SHA and independently decoded source-content sentinels match. |
| Identical create conflict | Return the same generation and unchanged bytes. |
| Same-size changed bytes | Wrapper rejects immutable conflict; reread proves original unchanged. |
| Changed MIME | Reject conflict; generation and original bytes remain unchanged. |
| Impossible generation | Reject rather than read the current version. This does not prove old-version retention after replacement. |
| Forged MIME/size evidence | Reject pinned metadata mismatch. |
| Uncertain write/timeout | Persist a fixed failure category and planned ownership; stop without cleanup or new key. |

No object is overwritten or deleted. Retain the single synthetic object and receipt. No signed URL, public endpoint, Firebase ID-token login, subscription change, bucket configuration or production path is involved. This service-account storage probe does not exercise a mobile QA user flow.

## Timeout proof must be separate

A normally successful live request does not exercise a network deadline. Automated tests should use the actual Google Storage HTTP SDK and production adapter against a credential-free loopback-only fault server, not a fake storage client. Delay metadata response and stall a partial media body. Observe bounded failure, one-attempt behavior, pinned generation request parameters and server/client teardown. Test deadline option wiring and preservation of original legacy options.

Label these outcomes `LOCAL_SDK_FAULT_INJECTION`, separately from `LIVE_GCS`. An accepted TCP connection that stalls demonstrates read timeout, not connect timeout. Connect-timeout configuration checks are wiring evidence; actual remote cloud outage, TLS/connect-stall behavior and historical generation availability remain unproven unless separately exercised. Never mark skipped, unavailable or unauthorized live execution as PASS.

## Delivery and acceptance

Prepare a dry-run-first standalone Java probe, a bounded Python launcher, credential-free guard/transport tests and an operator runbook. A dry run validates non-secret arguments and emits the proposed scope without opening credentials or connecting. Execution requires an explicit flag and a separately reviewed concrete command with project/bucket/credential/receipt selection. Preparing or approving the implementation specification does not itself run the live test.

Public diagnostics use finite stage/status categories and bounded summaries. Do not print credentials, tokens, raw SDK exceptions, HTTP debug headers or private download URLs. Private receipts are exclusive owner-only files outside tracked repository paths; preserve partial/failure evidence as well as successful observations.

Run guard/fault tests and Maven test/verify, retain actual results, complete independent BMad review, then present the exact live command and retained-object impact for execution approval. Existing production code, P0 limitations, production profile gates and P3 remain unchanged.

## Historical pre-fix implementation checkpoint


The tool/launcher, guard tests and evidence are implemented; status remains in-progress. The transport task and overall acceptance remain incomplete. No production source changed and no credential/live cloud operation occurred. The enabled partial-body regression is retained, not skipped.

- Final Java guard class: 7 PASS; Python launcher: 8 PASS. Parent independently reran all 8 Python tests and the credential/network-free dry run successfully.
- Actual SDK metadata stall and clone/legacy wiring checks passed. Actual partial-body fault failed: a configured 300 ms limit still made seven generation-pinned media attempts over 2,500 ms before supervised termination. The reader stack was captured before fixture cleanup.
- [Runtime diagnosis and concrete candidates](../../be/docs/evidence/p2-storage-probe/body-stall-diagnosis.md), [raw failed regression](../../be/docs/evidence/p2-storage-probe/focused-tests-attempt-4.txt), [all evidence](../../be/docs/evidence/p2-storage-probe/README.md).
- Full Maven test/verify and independent BMad acceptance have not completed. No completion commit was created. The frozen Ask First rule for production code changes prevents fixing the existing adapter within this tool-only scope.

Proposed scope expansion for human decision: enforce an absolute remaining elapsed budget on reading storage I/O, covering stalls, slow trickles, authentication/SDK work and actual worker teardown, with reading-only retry policy and protected credential IPC where isolation is used. Limit backend changes to asset-owned reading storage/supervision and complete profile identity/resource accounting. Preserve legacy client behavior, immutable generation/equality guarantees, disabled production admission and all deployment/access/P3 gates. Add deterministic stall/trickle/interruption tests, rerun full checks and packaged-runtime proof before proposing any live diagnostic command.

The smaller retry-handler-only candidate can address the demonstrated resumptions, but cannot by itself establish an absolute duration bound for a continuously trickling response. Neither candidate has been implemented or runtime-verified.

## Human-approved absolute I/O deadline extension

On 2026-10-16 the human accepted the explained two-layer solution with “Tamam uygun”: disable retries only on the reading SDK clone and supervise reading storage I/O in an owned child with an absolute monotonic deadline, observed forced exit and stall/slow-trickle/interruption tests. This supersedes the earlier tool-only production-edit prohibition solely for the affected asset reading path. Existing working diagnostic/receipt/launcher tests and the original failure evidence must survive. Live execution still needs its final concrete approval.

Implementation must cover observe, pinned read, conditional create/equality reread and authentication/SDK setup; a timer checked only after a blocking read or a cancelled Future is insufficient. Use the same Java home and executable-JAR launcher precedent. Bound request/response/log bytes before allocation, parent/child heap/direct/RSS, disk if used, and local process concurrency together. Carry the existing remaining operation/revision budget through process setup and I/O; validate completion before success. Hold capacity until child and owned pipe tasks actually exit on success, timeout, interruption, malformed response and overflow. Uploaded-but-unconfirmed objects remain retained with existing ownership evidence. No automatic retry/new key or deletion.

Keep immutable generation conditions, exact MIME/size/SHA/conflict semantics, no-I/O-in-transaction checks, first-terminal fencing and public module boundaries. Use the existing SDK and real immutable operations in the child. Fail unsupported runtime wiring closed; never silently fall back to unbounded in-process production I/O. Existing synthetic disk fixtures may explicitly use their test-only in-process adapter, with no such bypass for real Firebase. No new cross-module edge or schema is needed.

Credentials/configuration must come from the explicit configured storage client; no ADC discovery. Send private credential/configuration material only through protected IPC or owner-only transient files; never argv, public logs, unrestricted environment options or persistent diagnostic output. No new arbitrary endpoint is exposed. Credential-free loopback mode is restricted to literal loopback and NoCredentials fixtures; live transport remains the configured official GCS endpoint. Classpath/launcher must work in the actual packaged Temurin JRE, without test classes standing in for production child code.

All effective I/O process limits, no-retry policy, admission rules and implementation/runtime identity must participate in profile fingerprints; invalidate old profiles rather than certify old descriptors under new behavior. Numerical diagnostic/engine limits do not admit production profiles. Add actual child normal byte/generation/equality, metadata stall, partial-body stall, continuously progressing slow-trickle, interruption, malformed/overflow/capacity and secret-safe IPC coverage. Preserve independent expected pixels/content. Verify initial body failure is now fixed by the production path, retain its original log, and test immutable failures/retained evidence and worker fencing regressions.

Apply project standards to ADR-0022, memory and canonical architecture; do not present local SDK loopback results as live GCS or remote TLS/connect proof. Complete focused/Python, full Maven test/verify, module checks, independent review and executable-JAR Temurin proof. Then propose the separately gated live diagnostic command.

## Superseding human rollback: SDK-only scope

The human requested “bu kadar ileri gitmemize gerek yok ... bu değişiklikleri geri çekip kaldıgın yerden devam”. This supersedes the absolute child-I/O extension above. The nine new, unintegrated runtime classes (ReadingProcessCapacity; ReadingNoRetry; ReadingStorageChild/Connection/Operations/Policy/Process/Protocol/TestAdapter) were removed. Existing tracked backend code had not yet changed, so no baseline P2 code or diagnostic was reverted. Preserve the unrelated .memlog change.

Use only a minimal inline StorageRetryStrategy in FirebaseStorageAssetClient.withReadingDeadline, returning StorageRetryStrategy.getDefaultStorageRetryStrategy().getNonidempotentHandler() for both classifications. The installed SDK source documents this as non-retrying; use the SDK handler rather than reimplement shouldRetry. Preserve original storage options. Record policy in ReadingProfiles and regression assertions. No new runtime helper/source-I/O process or custom storage protocol. Tool-only process watchdog/negative-test child remain diagnostic containment.

Do not claim an absolute duration bound: socket inactivity timers and outer checks still cannot preempt a continuously progressing blocking read. Explicitly keep this gap open in runbook, ADR-0022, project memory and architecture/evidence until later production qualification. Preserve the original failed 300ms/seven-attempt runtime evidence as history; rerun it on the small SDK policy fix. No real credentials/cloud, deployment, access change, production profile or P3 action. Parent coordinates full test/verify/review after focused implementation.
