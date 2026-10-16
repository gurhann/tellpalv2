# Original partial-body deadline failure and SDK-only correction

Environment: host Homebrew OpenJDK 25.0.2; Google Cloud Storage HTTP SDK 2.58.1, project dependency. No credentials, GCS, Spring or database were used. [Attempt 4](focused-tests-attempt-4.txt) preserves the actual failure and client stack. At the original checkpoint no production file had changed. The later approved SDK-only correction changes only the reading clone and profile identity; no storage process/IPC runtime was added.

## Current resolution

The superseding human scope selected only the existing SDK retry-handler configuration. `FirebaseStorageAssetClient.withReadingDeadline` now returns the SDK default non-idempotent handler for both idempotent and nonidempotent classifications in an inline `StorageRetryStrategy`; it does not implement a new retry algorithm or mutate the original client. New profiles record the exact policy, inactivity/absolute semantics and adapter/SDK-handler implementation hashes.

[Final focused evidence](sdk-fix-focused-final.txt) records **21/21 PASS**. The original partial-body regression remains enabled and now rejects at **459 ms** with one metadata request and one media request, both generation pin conditions preserved, the first actual body byte independently flushed, and normal diagnostic-child exit observed. Metadata stall, immutable conflict/ambiguous-create guards, legacy-option/strategy preservation and profile/encoder checks also pass. Full-suite, packaged-runtime and review gates remain separately coordinated; no live GCS was executed.

This fixes internal resumptions after inactivity timeout. It does **not** prove an absolute wall-clock bound for a continuously progressing response or authentication. Existing between-call checks cannot preempt blocking work; that production gate stays open.

## Original runtime evidence

The loopback server binds only `127.0.0.1`. It returns complete metadata identifying generation `17`, positive fixture size and `image/png`, then an HTTP `200` media response with the correct `Content-Length`. It writes and flushes the first actual PNG byte and stalls the remaining body. Every media request must include both `generation=17` and `ifGenerationMatch=17`.

The real adapter uses `withReadingDeadline` and the real wrapper uses `read(..., 300)`. The separate diagnostic child remained running after 2,500 ms and made **seven** pinned media attempts despite `maxAttempts=1`. The parent forcibly terminated it, waited for actual process exit and failed the assertion `SDK_BODY_DEADLINE_NOT_ENFORCED`. The child did not return normally; supervisor containment is not an SDK timeout PASS.

At approximately one second, **before fixture cleanup**, the reader thread stack was:

```text
sun.nio.ch.Net.poll
sun.nio.ch.NioSocketImpl.timedRead
java.net.Socket$SocketInputStream.read
sun.net.www.protocol.http.HttpURLConnection$HttpInputStream.read
com.google.api.client.http.javanet.NetHttpResponse$SizeValidatingInputStream.read
com.google.cloud.storage.ApiaryUnbufferedReadableByteChannel.read:126
com.google.cloud.storage.DefaultBufferedReadableByteChannel.read:106
com.google.cloud.storage.BaseStorageReadChannel.read:130
com.tellpal.v2.asset.infrastructure.storage.ReadingObjectStorage.read:30
```

The stall is in the SDK body-read path, not `HttpServer.stop`, executor teardown or an assertion wait. The separate metadata-stall test terminated within its 2.5-second observation bound with one request at a configured 200-ms timeout. The wiring test confirms 321-ms connect/read/RPC/total settings and maxAttempts=1 on a clone, with unchanged legacy options. These successful checks do not certify body-read termination.

## Dependency source explanation

The installed Maven sources JAR `com/google/cloud/google-cloud-storage/2.58.1/google-cloud-storage-2.58.1-sources.jar` supplies these anchors:

- `StorageImpl.reader`, lines 715–719, returns `BlobReadChannelV2` with a resolved `BlobReadChannelContext`.
- `ApiaryUnbufferedReadableByteChannel.read`, lines 114–157, loops while destination buffers remain. Each `sbc == null` starts `retrier.run(..., this::open, ...)`. At line 126 it reads the body. The catch at 133–147 tests retryability, clears `sbc`, and returns to the outer loop; that loop has no shared elapsed deadline/attempt counter. New successful opens can therefore each have their own one-attempt retrier while the enclosing body operation resumes repeatedly.
- `ReadingObjectStorage.read`, line 30, cannot call its own `deadline.check()` until `input.read(...)` returns. The SDK's internal retry loop prevents that return.
- `StorageRetryStrategy` exposes `getIdempotentHandler()` and `getNonidempotentHandler()`; `HttpStorageOptions.Builder.setStorageRetryStrategy` configures the handlers consumed by that body loop. These APIs permit a reading-only no-retry policy without changing the legacy client.

The runtime attempt count and stack support this explanation. They do not claim remote GCS outage, TLS/connect-stall or slow-trickle qualification.

## Historical candidate analysis before the superseding SDK-only decision

1. **Smallest candidate for this demonstrated fault:** amend production `FirebaseStorageAssetClient.withReadingDeadline` so its reading-only clone supplies a `StorageRetryStrategy` whose idempotent and nonidempotent result handlers deny every retry. The source shows that the first body timeout would then escape the loop instead of clearing/reopening `sbc`. Keep `maxAttempts=1` and current connect/read/RPC settings, preserve all original client settings, and rerun the exact fault plus immutable generation/conflict/legacy tests. This was originally a source-supported candidate. Its approved form now delegates to the existing SDK non-idempotent handler rather than implementing new handlers; the final focused regression above verifies the stalled-read case. It addresses repeated resumptions after a stalled read; a continuously trickling response still needs separate absolute-duration proof because socket read timeout is an inactivity timeout.

2. **Hard elapsed-time containment candidate:** move reading storage operations behind an owned supervised worker process and enforce the remaining monotonic operation budget at the parent; kill and observe actual child exit before releasing capacity. This requires production changes at `ReadingObjectStorage.observe/read/create`, an asset-owned worker/supervisor and packaged-runtime credential/IPC verification. The Python launcher and fault child demonstrate kill-and-reap containment on this host, **not an integrated production implementation**. It can enforce an absolute elapsed bound independently of SDK progress/retry behavior, including slow trickles, but is a larger architectural change.

A direct pinned range RPC is another possible adapter design (`HttpStorageRpc.read` lines 926–957 bounds a requested range and downloads it once). It uses SDK SPI and buffering and still needs an absolute deadline/progress analysis; `Storage.readAllBytes` alone can allocate an unbounded response and is not a safe drop-in solution for the existing bounded streaming contract. No SDK upgrade or transport switch was tested, so none is presented as a verified fix.

The original tool-only specification prohibited the production fix and that checkpoint failed the stalled-read gate. The human subsequently approved and then narrowed the extension to the SDK-only correction described above; the extra unintegrated process classes were removed. The inactivity-stall/one-attempt criterion now passes. Absolute in-flight slow-trickle protection is explicitly deferred, not certified by the diagnostic watchdog. Live execution remains unperformed and separately gated.
