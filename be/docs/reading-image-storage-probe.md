# Bounded P2 storage diagnostic

Status: SDK-only retry correction, independent review patches, full test/verify and packaged SDK checks pass. One separately approved [live storage diagnostic](evidence/p2-storage-probe/live-gcs-2026-10-16.md) passed on 2026-10-16. Future live invocations still require their own exact-target and retained-object approval. See [actual evidence](evidence/p2-storage-probe/README.md) and [original diagnosis/current resolution](evidence/p2-storage-probe/body-stall-diagnosis.md). The observed inactivity stall is fixed; absolute in-flight slow-trickle protection remains deferred.

The standalone class lives only under test sources, in the adapter's package. It constructs `FirebaseStorageAssetClient` and `ReadingObjectStorage` directly, without Spring, database, scheduler, fake storage, production visibility changes, application startup or automatic Maven cloud-test discovery. Python 3.10+ with POSIX process groups/private permissions and Java 25 are required. Maven preparation uses the existing wrapper and installed dependencies.

## Preparation commands

From `be/`, these commands exercise no live credentials or GCS:

```bash
python3 -m unittest discover -s tools -p test_reading_storage_probe.py -v
bash ./mvnw -Dtest=ReadingStorageLiveProbeTest,ReadingStorageTransportTest test
python3 tools/reading_storage_probe.py --prepare
python3 tools/reading_storage_probe.py --project probe-local --bucket probe-bucket --credential /nonexistent/private/credential.json --receipt /nonexistent/private/receipt
```

The second command retains the original partial-body regression and now passes with one media attempt after the SDK policy correction. The last command was actually executed successfully with missing credential/receipt directories. A dry run validates argument syntax and runs a supervised Java child from the prepared classpath. It never invokes Maven, opens credential/receipt paths or performs network calls. Missing classpath preparation is a finite refusal. The separate explicit `--prepare` mode builds test classes/classpath and may resolve Maven dependencies; it never launches the storage probe. For strictly offline preparation use an already populated Maven cache and prepare the classpath offline before invoking the Java dry run directly, as shown below.

```bash
bash ./mvnw -o -q -DskipTests test-compile dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile=target/reading-storage-probe.classpath
java -Djava.awt.headless=true -cp "target/test-classes:target/classes:$(cat target/reading-storage-probe.classpath)" com.tellpal.v2.asset.infrastructure.storage.ReadingStorageLiveProbe --project probe-local --bucket probe-bucket --credential /nonexistent/private/credential.json --receipt /nonexistent/private/receipt --repository "$(cd .. && pwd -P)"
```

No credentials, IAM, cloud object, bucket listing, billing query or mobile Firebase session is part of preparation. The standalone class is a manual `main`; its name cannot match ordinary Surefire/Failsafe cloud execution.

## Operator-selected live command, separately gated

The tool accepts an explicitly selected project, bucket, existing private service-account JSON and a new receipt path. It does not discover ADC, search for credentials or infer a bucket. Select existing credentials outside the repository, in an existing owner-only directory (`0700`), with an owner-only regular file (`0600` or `0400`). Paths must be absolute and canonical; symlinks and group/other permissions are rejected. The receipt must not exist, and its parent must be private, owned by the current user and outside the repository. Both `<receipt>` and `<receipt>.supervisor` are created exclusively as `0600`; preserve both indefinitely after a started live run.

Before approval, substitute actual selections into this dry-run command. Its output renders the exact shell-quoted live command for human review:

```bash
python3 tools/reading_storage_probe.py --project '<selected-project>' --bucket '<selected-bucket>' --credential '/absolute/private/existing-service-account.json' --receipt '/absolute/private/new-p2-storage-receipt.txt'
```

After the required verification gates complete and the human explicitly approves that concrete command and retained object, the corresponding execution form is:

```bash
python3 tools/reading_storage_probe.py --project '<selected-project>' --bucket '<selected-bucket>' --credential '/absolute/private/existing-service-account.json' --receipt '/absolute/private/new-p2-storage-receipt.txt' --execute-live
```

These remain operator templates and do not authorize another run. The separate approved 2026-10-16 diagnostic used an existing credential through a private copy; the implementation build itself was credential-free. Service-account `type`, exact `project_id`, project-bound `client_email`, Google universe and exact `https://oauth2.googleapis.com/token` endpoint must match before SDK construction. Duplicate JSON fields and files larger than 64 KiB are rejected. Child environments remove implicit Google credential settings and injected Java options. Invalid target/private files/bindings yield finite sanitized failure categories.

## Finite retained-object scope

One fresh run UUID determines exactly `local/reading/p2-storage-probe/<UUID>/fixture.png`; custom keys/prefixes are not accepted. The object is a deterministic opaque 3×2 RGB PNG, at most 64 KiB. A private ownership receipt precedes the first upload; both its file contents and exclusive parent-directory entry are forced to disk before the worker can continue. Four create operations target that same key with the actual SDK's does-not-exist precondition: initial bytes, identical bytes, different same-size bytes, and wrong MIME. At most one object remains retained; there is no overwrite, delete, listing, cleanup, public URL, signed URL or new-key retry.

The real wrapper independently reobserves the initial generation, reopens pinned stored bytes, checks MIME/size/SHA, fully decodes dimensions and all six pixel sentinels, and rechecks unchanged generation/content after each conflict. Same-size and wrong-MIME conflicts must produce the wrapper's exact immutable-conflict failure; transport failures cannot be mistaken for successful negative assertions. An impossible generation and forged MIME/size must fail with the exact pinned-metadata failure, without latest fallback. This does not prove retention of overwritten historical versions.

Each wrapper operation uses a diagnostic 3-second limit. The launcher supervises one live child for 60 seconds. The separate explicit preparation mode has a 180-second build limit. Both stdout and stderr share a 64-KiB bound and are suppressed; only finite public status categories are emitted. Timeout, overflow, SIGTERM, interruption or process/setup failure kills the owned process group and waits for the direct child to exit before returning. The supervisor restores the prior SIGTERM handler after cleanup. Planned/partial ownership receipts survive; never rerun an ambiguous upload using a new receipt/key. The reading-only SDK strategy prevents the observed internal resumptions using its existing non-idempotent no-retry handler for both classifications. These 3-second socket inactivity/outer checks do not guarantee an absolute in-flight bound for slow trickles or authentication; the launcher alone enforces the diagnostic run's 60-second lifetime. This production-qualification gap remains open.

Live PASS additionally requires a new owner-only regular worker receipt of at most 16 KiB, matching the expected worker/project/bucket, one canonical UUID key in the approved local prefix, and the complete ordered verification markers with consistent generation evidence. Exit code zero alone is insufficient; missing, partial, pre-existing or mismatched receipts fail without printing their contents. Raw child output remains discarded. Synthetic receipt tests establish launcher validation behavior, not live GCS proof.

## Proof boundaries

The [selected synthetic `LIVE_GCS` storage cases](evidence/p2-storage-probe/live-gcs-2026-10-16.md) passed in one explicitly authorized run. Integrated production preparation, remote outage behavior and absolute in-flight duration remain unproven. `LOCAL_SDK_FAULT_INJECTION` covers credential-free loopback HTTP metadata/partial-body faults through the actual adapter. Accepted TCP connections prove read behavior, not connect/TLS outages. `WIRING_ONLY` verifies connect/read/RPC settings, one-attempt configuration and preservation of legacy options; the original body fault showed that `maxAttempts=1` alone did not prevent internal resumptions. The corrected clone delegates both classifications to the SDK built-in no-retry handler, and the retained regression now passes with one media attempt. Watchdog process containment is separate evidence from SDK deadline enforcement.

P0 Android 1× / iPhone 0.75× evidence remains limited to the shown TR71/TR19 portrait samples/geometries; Android 0.75× embedded text was blurry and JPEG/WebP sharpness remains unresolved. Missing native ledgers/sidecars/build/cleanup evidence remains a limitation. No production profile, P3 orchestration, CMS/LIVE mutation, deployment, backfill or broader quality/resource/access admission follows from this diagnostic.


## Packaged SDK fault proof

The JDK-only [PackagedStorageProbe.java](evidence/p2-storage-probe/PackagedStorageProbe.java) deliberately imports no project or test classes. Load it alongside the actual executable JAR using `PropertiesLauncher`; it verifies that the production adapter class origin is `BOOT-INF/classes`, then runs normal generation-pinned bytes with six independently decoded pixel sentinels, a metadata stall and a partial-body stall. The fault server and SDK use literal loopback and `NoCredentials`; external network is disabled. Every stall must reject within its observation bound with one request, and the partial-body case independently confirms that its first byte was flushed. An exit watchdog contains the diagnostic only.

After `bash ./mvnw verify` has produced the JAR, prepare the helper with host Java 25, then run the pinned official JRE (use the existing local image; no deployment):

```bash
javac -d target/packaged-storage-probe docs/evidence/p2-storage-probe/PackagedStorageProbe.java
shasum -a 256 target/tellpal-v2-backend-0.0.1-SNAPSHOT.jar
docker run --rm --name tellpal-p2-storage-probe --network none --read-only --cpus 1 --memory 768m --memory-swap 768m --pids-limit 128 --tmpfs /tmp:rw,noexec,nosuid,size=64m --mount "type=bind,source=$PWD/target,target=/work,readonly" --workdir /work eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64 java -Xmx256m -XX:MaxDirectMemorySize=32m -Djava.awt.headless=true -Dloader.path=/work/packaged-storage-probe -Dloader.main=PackagedStorageProbe -cp /work/tellpal-v2-backend-0.0.1-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher
```

Retain the command exit status, container teardown, JAR hash and actual output under the evidence directory. This command is a local packaged diagnostic, not the separately gated live command. Mark it PASS only after executing it; mounting or startup failure is not timeout evidence. Host focused tests cannot substitute for this runtime gate.
