# Isolated native fixture entry r1

This tooling implements the host part of the accepted [r1 contract](../../_bmad-output/implementation-artifacts/image-optimization/native-entry-implementation-r1.md). It supports one declared raster case per launcher session. It provides no measurement batch, production route, media right, credential acquisition, account mutation, cloud reservation or automatic installation. Acceptance, synthetic implementation verification, signed builds and physical profile/release verification are separate receipts. `NATIVE-ENTRY` remains open until the complete physical positive and negative matrix has actual evidence.

## Locked host environment and verification

Python 3.11–3.14 is supported. The isolated project pins `firebase-admin==7.1.0`, `cryptography==46.0.3` and `Pillow==12.0.0`; `uv.lock` pins their transitive resolution. Existing backend/mobile dependencies are untouched. Run from `tools/image-optimization`:

```sh
uv sync --locked --python 3.11
uv run --locked --offline --python 3.11 python -B -m unittest discover -s . -p 'test_native_entry_*.py' -v
```

The suite uses synthetic identities and temporary private files, a temporary loopback TLS listener and injected admission checks. It does not call Firebase or a real backend/device. Localhost listener permission is needed for these synthetic TLS tests. A sandbox refusal is not a protocol test pass. The public P-256 positive/negative vector in `native_entry_crypto.CRYPTO_VECTOR` is also frozen in Kotlin/Swift tests; its known test key is never used for runtime pairing.

After preparing this locked environment, run from `app/mobile`:

```sh
dart run build_runner build --delete-conflicting-outputs
flutter analyze
flutter test --concurrency=1 test/image_experiment test/native_runtime_guard_test.dart test/library test/story_detail
```

The fixture tests include a real Dart-to-Python `FixtureHandler` pinned TLS connection using the host `.venv` interpreter. They also exercise the actual routed view/cache/provider draw and its automatic authenticated teardown. Test-only certificate/key material is temporary and synthetic. Generate certificates with exact hostname SAN, `extendedKeyUsage=serverAuth` and explicit `digitalSignature,keyEncipherment` key usage; static config validation alone cannot qualify a platform's TLS trust policy.

## Private inputs

Copy the example into a private directory outside the checkout, replace every `REPLACE_*` value there and set mode `0600`. The checked-in example is deliberately incomplete and cannot authorize a launch. Config is strict UTF-8 JSON, at most 16 KiB, with exactly its listed fields: unknown/duplicate keys, trailing JSON, nonfinite numbers and booleans used as integers refuse with a fixed code. The configured project ID must be explicitly accepted, match the private Admin credential and the verified token; a display name is insufficient. The canonical account is read only from `tools/mobile-test-account.json`.

All input files must be external, owned regular `0600` files with no symlink component. Raster files may also be immutable `0400`. Output must name a nonexistent unique directory beneath an existing owned `0700` parent outside the checkout. Outputs cannot overlap config, credential, manifest or raster inputs. Logs and receipts are `0600`; no original input is deleted or changed. Do not put a token, private key or signing password in command arguments, launcher metadata, environment, assets or Dart defines.

The backend origin is an explicit-port loopback HTTP/HTTPS origin; the broker origin is an explicit-port private-address/loopback HTTPS origin. Userinfo, paths, fragments, queries, public broker hosts, production environments and ordinary application IDs reject. Broker hostname must appear exactly in the certificate SAN. The private PEM certificate, standard-base64 DER and lowercase DER SHA-256 must match, be currently valid and match the private TLS key. Every client TLS connection verifies validity, hostname and exact leaf DER pin before bearer headers. Rotation needs a fresh launch.

Private backend startup JSON has exactly:

```text
schemaVersion, backendEnvironment, backendOrigin, pid, processStartTime,
commandSha256, sourceSha256, sourceFiles,
artifactPath, artifactSha256, configPath, configSha256
```

`sourceFiles` maps repository-relative `be/` filenames to exact SHA-256 hashes. `sourceSha256` hashes the compact, ASCII JSON bytes of that ordered mapping (retain the same insertion order). Artifact/config paths are external; the config is a private regular file. `processStartTime` is the stripped `ps -p PID -o lstart=` output and `commandSha256` hashes stripped `ps -p PID -o command=` bytes. These raw values stay private. Before authentication the driver rehashes files, checks live PID/start/command, requires an explicit `--spring.profiles.active=local|test` or `-Dspring.profiles.active=local|test`, and independently checks this PID's actual loopback-only listener with `lsof`. A label, stale receipt or HTTP 401 cannot establish this binding. No backend code/schema change is made.

## Frozen manifest

Private manifest keys are `schemaVersion=1`, `scope`, `batchId`, `corpusSha256`, `referenceSha256`, `sourceSha256` and `entries`. Each entry has exactly:

```text
caseId, role=isolated_raster_fixture, arm=control|candidate,
sourceSha256, referenceSha256, outputSha256, mime, byteSize, width, height,
encoderRevision, profileRevision, filePath,
logicalSlotWidth, logicalSlotHeight, dpr, fit=contain|cover,
alignmentX, alignmentY, purposeAcceptanceReceipt
```

IDs/revisions/receipt references use `[A-Za-z0-9_-]{1,64}`. Hashes are lowercase 64-hex. Entries are static opaque, orientation-tag-free JPEG/PNG/WebP, at most 8192 pixels per axis and 16,777,216 pixels, 20 MiB per file, 20 files and 100 MiB total. Slot axes/DPR must be finite positive numbers; alignment axes are in `[-1,1]`. Pillow independently verifies encoded format, dimensions, animation, EXIF orientation and opacity. The host reads through no-follow descriptors, verifies exact size/hash and retains immutable bytes instead of reopening paths. No unknown ID becomes a filename or URL. Successful offline encoding does not supply purpose acceptance, source adequacy or visual-quality approval.

The public projection drops only `filePath`, is frozen once as exact UTF-8 JSON bytes, and is capped at 32 KiB to leave space for base64/token/certificate in the 64 KiB envelope. Its SHA-256 binds those bytes, never a client reserialization. Client admission independently verifies manifest, encoded size/SHA/MIME and intrinsic dimensions.

## Commands and build receipts

The variable below names an operator-provided private config path. It contains no token or actual known location.

```sh
uv run --locked --offline --python 3.11 python -B native_entry_driver.py validate-config --config "$NATIVE_ENTRY_CONFIG"
uv run --locked --offline --python 3.11 python -B native_entry_driver.py build-entry --config "$NATIVE_ENTRY_CONFIG"
```

`validate-config` performs static input/certificate/manifest validation; it makes no authenticated connection and does not qualify a live backend or device. `build-entry` creates `output/source/repository/app/mobile`, preserves the actual repository nesting used by native exclusion guards, copies source into that private snapshot and runs offline Flutter package resolution while preserving `pubspec.lock`. Native/generated Flutter build state stays outside the ordinary checkout. It dispatches only the exact experiment profile or release source/target and inspects actual app/test IDs and selected signing certificates. Both artifacts must have the same certificate; ordinary IDs refuse. Source hashes are checked again after build.

Hashing and copying share one deterministic selection: every copied build input, including Android manifests/Proguard/resources, Podfile/Podfile.lock, native build guard, pubspec files and assets, has an exact content hash. Checked-in generated Dart sources remain build inputs. Transient Flutter/Gradle/Pods/build state, generated plugin registrants/settings, local developer properties and private signing/account files are excluded consistently. Selection is bounded to 4096 files, 64 MiB per file, 512 MiB total and a 10 s scan/copy deadline. The private copy is checked against its original hashes immediately and before writing the build receipt; current checkout and copied input hashes are checked again before consuming that receipt for physical verification. Added, removed or mutated inputs refuse. The receipt records `sourceFiles`, `sourceSha256`, `sourceCopyPath` and `copiedSourceSha256`; build receipt JSON is bounded to 2 MiB.

Android signing properties are an external `0600` file with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`, whose keystore is also external/private. The driver uses the platform opt-in properties and invokes only the selected app/test assemble tasks. `apkanalyzer` and `apksigner` must be present. The receipt records actual packaged debuggable state; profile behavior is observed independently rather than inferred.

Android build preparation also requires the selected `flutter` executable on PATH and an explicit `ANDROID_SDK_ROOT` or `ANDROID_HOME`. It generates private `android/local.properties` from those tool locations in the snapshot instead of copying a developer's existing properties. No SDK is installed or upgraded.

Apple private xcconfig must set `DEVELOPMENT_TEAM`, `CODE_SIGN_IDENTITY`, distinct `IMAGE_EXPERIMENT_APP_PROVISIONING_PROFILE_SPECIFIER` and `IMAGE_EXPERIMENT_TEST_PROVISIONING_PROFILE_SPECIFIER`, `IMAGE_EXPERIMENT_PRIVATE_OUTPUT_ROOT`, `IMAGE_EXPERIMENT_SIGNING_CONFIG`, `FLUTTER_ROOT` and an external `FLUTTER_BUILD_DIR`. The latter must be relative to the private mobile snapshot because the installed Flutter script prefixes native-assets paths with the project directory. The driver overrides `FLUTTER_APPLICATION_PATH` to the private snapshot. It uses `pod install --deployment`, the lowercase scheme, selected experiment configuration and private derived data. A stale Podfile lock, absent signing/profile, target/script error or missing tool leaves a failed receipt; there is no dependency upgrade, default-signing fallback or automatic provisioning. Prepare/validate any necessary pinned Podfile lock before claiming a signed build.

`build-receipt.json` contains source/lock/signing/engine/native target/mode/app/test identities and exact artifact hashes. Renderer/debugger attachment/physical presentation stay null with reasons unless actually observed. Artifact build success alone cannot close `NATIVE-ENTRY`, OQ-6 or Story 1.6 gates. Each child tool has argument arrays, a shared 180 s driver deadline, capped private stdout/stderr and owned process-group kill/settlement with a 15 s cleanup budget. Public output contains fixed status/reason codes, aliases and hashes only.

## Pairing and authenticated delivery

Public launcher metadata has exactly `schemaVersion`, `scope`, `batchId`, `caseId`, `sessionId`, `launchNonce`, `deadline`, `expectedUid`, `expectedAppId`, `firebaseProjectId`, `backendEnvironment`, `brokerOrigin`, `brokerCertificateDerBase64`, `brokerCertificateSha256`, `manifestSha256`, `publicManifestSha256`. It contains no credentials. Android carries its base64 JSON in same-signed instrumentation; Apple UITest transfers it with launch environment.

The fixed token-free `GET /experiment/v1/probe` and `GET /experiment/v1/backend-probe` use `X-Experiment-Session` and `X-Experiment-Launch-Nonce`. The latter issues only `/api/test/library?lang=tr` against the admitted loopback backend, records the separate host leg and reports **proxied reachability**. Device-to-broker success cannot replace both legs. A private LAN iPhone certificate needs appropriate SAN and local-network permission. Streaming ADB does not establish laptop routing; owned reverse rules need separate actual qualification and the driver creates none.

Native creates an in-memory P-256 key. Android instrumentation reports public SPKI via `pairing-key` status; Apple UITest reads `imageExperimentPairingKey` and reports public JSON. The driver requires exactly one matching report, then binds that key only after both token-free probes. An early report remains public-only pending state under an owned worker until both legs finish, the original 30 s launch deadline expires, or teardown/revocation cancels it. There is no authentication before that binding and no challenge/claim retry. A network hello cannot pair a key. `GET /experiment/v1/bootstrap/paired?sessionId=...&launchNonce=...` returns only readiness and public-key SHA. Polling is bounded by the original 30 s launch deadline; challenge/claim are each dispatched once.

`POST /experiment/v1/bootstrap/challenge` takes `scope`, `batchId`, `sessionId`, `launchNonce`, `publicKeySpkiBase64`. Claim adds `challenge` and `signatureBase64`. Signatures are SHA256-ECDSA ASN.1 DER over seven ASCII newline-separated fields with a final newline:

```text
tellpal-native-fixture-v1
scope
batchId
sessionId
launchNonce
challenge
sha256(SPKI DER)
```

The challenge is atomically consumed before network admission; concurrent claims yield one envelope. Invalid/missing/replaced key, binding, DER, TTL, signature or replay refuses with a fixed code. The host verifies the canonical account exists/enabled, verifies the existing token signature/project/expiry with revocation checking, then requires the local/test Library allowlist's 200 response. Firebase work runs in an owned process with a total 10 s deadline, private memory IPC and discarded SDK console output. No user/claim/subscription is created or changed. Revocation checks are fresh for every object/status request.

Consumed proof and successful admission are separate observations. The broker records successful admission only after constructing the complete bounded envelope. An admission error or expired session cannot be promoted to successful entry by a launcher finishing. Full selected fixture delivery is recorded only after its immutable response bytes have all been written and flushed; partial writes and wrong size/hash do not qualify. This is a host transfer observation, while decoded admission and physical presentation require their own device evidence.

Runtime envelope fields follow r1, including exact public-manifest bytes/hashes and the token. Native `takeSession` is once-only and drops retained envelope references; Dart detaches the token into an app-owned holder. There is no Firebase SDK session, refresh token or renewal. Token reference lifetime is bounded; guaranteed memory zeroization is not claimed.

`GET /experiment/v1/batches/{batchId}/objects/{caseId}`, `GET /experiment/v1/session/status` and `POST /experiment/v1/session/revoke` require bearer plus `X-Experiment-Session`. Revoke has exact `Content-Type: application/json`, `Content-Length: 2`, body `{}` and no transfer encoding. Only the one selected case may be dispatched. Session expires within 180 s; status is polled every 2 s with a 10 s deadline, so unavailable/negative status ends app admission within the stated 12 s observation bound. Owned close coalesces concurrent callers, rejects new normal requests and retires the status observer before issuing its single authenticated revoke; intentionally retired status callbacks cannot cancel that revoke. Credential revocation and expiry guards remain active until cleanup finishes. Host revocation closes active owned responses; app expiry/revocation rejects late callbacks. Every broker TLS request has an absolute 10 s connection timer and all preauth/status/fixture workers share four slots. Fixture bytes are streamed with absolute deadline and no redirect/fallback.

## Physical verification remains a separate gate

Future `verify-entry` requires an exact signed build receipt, an explicit private prerequisite receipt and one case ID:

```sh
uv run --locked --offline --python 3.11 python -B native_entry_driver.py verify-entry \
  --config "$NATIVE_ENTRY_CONFIG" --case-id "$NATIVE_ENTRY_CASE_ID" \
  --build-receipt "$NATIVE_ENTRY_BUILD_RECEIPT" \
  --prerequisite-receipt "$NATIVE_ENTRY_PREREQUISITE_RECEIPT"
```

Prerequisite JSON has exactly the fields checked by `validate_physical_prerequisites`: schema/scope/batch/platform/mode/device alias and selector hash, expected app/project, exact app/test/certificate hashes, physical/connected/model/provider observations, millisecond `observedAt`, explicit project/IAM/quota/reservation observations, installed app/test hashes and installation receipt hash, plus explicit installation/authentication authorization. Observations must be at most five minutes old; Android reservation must outlast the driver and cleanup budget. Local Apple selection is iPhone 12; Android provider is Device Streaming. Receipt declarations cannot substitute for the original private device/project/reservation/installation evidence: retain those originals and qualify their provenance before authorizing a real launch. The driver rehashes artifacts and current sources, checks live selected transport and backend startup, and refuses absent prerequisites before constructing Firebase credentials/broker. Installation and forwarding/reservation operations remain operator-owned; this driver dispatches only a previously installed bound launcher.

The verification receipt requires exactly one independently paired launcher key, successful admission, a completed selected fixture transfer, an authenticated explicit app revoke while the session is still active, and an explicit positive native result. Android uses `INSTRUMENTATION_RESULT: reason=NATIVE_LAUNCHER_SETTLED` with `INSTRUMENTATION_CODE: -1`; its refusal uses `NATIVE_LAUNCHER_REFUSED` with code `0`. Apple reports one exact `IMAGE_EXPERIMENT_RESULT settled|refused` line. Missing, repeated, mismatched or refused native results reject. Native settlement additionally requires the app-owned successful completion signal; ordinary closure, timeout or bootstrap failure defaults to refusal. Failed admission, expiry, host revocation, failed fresh token verification or missing fixture delivery writes only a failed receipt and never `verification-receipt.json`. Without an extracted private device ledger, `decodedAdmission` and physical presentation remain null with explicit reasons even on successful host entry verification.

Physical qualification must separately verify profile and debugger-free release, exact installed certificate/identity, app/test lifetime and attachment overhead, direct/proxied network labels, the positive entry and negative pin/key/session/UID/fixture/teardown matrix, and bounded cache/provider ownership. A single verification receipt retains `nativeEntryGate=open` and `negativePhysicalMatrix=not_run`; it never fabricates completion of that matrix. The full implementation status receipt beside the spec distinguishes actual host/build checks and unexecuted physical gates. No native batch, protected-media experiment or Story 1.6 benchmark is run by this tooling.

The 2026-10-11 read-only readiness receipt separately records connected selected iPhone 12 transport, zero valid configured signing identities, an exact privately observed/accepted console project and current principal Owner role. The user-selected console's Device Streaming monthly Android Studio physical streaming quota showed a Blaze limit of 4000 minutes and usage 0; a separate Spark row showed 30 and usage 0. These configured limits do not establish a free billing balance, reservation/session or installed artifact/authentication binding. Automated testing quota is a different observation. Real project/device selectors and original console/device receipts stay private; no IAM/quota/billing/auth/install/reservation operation was performed.

Rollback stops dispatch, revokes only this session, closes owned server/client sockets and workers, settles provider/Cubit/composition owners and removes only exact experiment cache/build/app/test outputs after identity checks. Preserve private original inputs, failed receipts, existing apps/media, canonical QA account and historical trackers. The driver never removes another reservation, forwarding rule or existing app.
