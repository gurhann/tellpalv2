# Selected native pilot setup

This runbook prepares the selected local physical iPhone 12 and one remote physical
Android phone through Android Device Streaming. It follows the [canonical
SPEC](../../_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/SPEC.md)
and [pilot scope](../../_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-pilot-scope.md).
Selection, current observation, entry verification and native measurement admission
are separate records.

The setup tools do not install clients or applications, sign builds, log in, obtain
tokens, reserve/connect/remove cloud sessions, boot devices, upload files, serve
candidates or run benchmarks. They leave `native_execution_ready=false` and every
native gate open. An incomplete setup with concrete gaps is a valid preparation
result. Story 1.6 remains backlog.

## Private preflight

Run from the repository root using Python 3 and its standard library. The default
is an offline plan: no device commands or cloud/account calls run, and no account
configuration is required.

```sh
python3 -B tools/image-optimization/native_pilot_preflight.py
```

For an explicit output location, create a new private parent outside the checkout.
On this macOS host use a real path such as `/private/tmp`, avoiding the `/tmp`
symlink. These commands create only private local report storage.

```sh
umask 077
PILOT_PRIVATE_PARENT="$(mktemp -d /private/tmp/tellpal-pilot.XXXXXX)"
python3 -B tools/image-optimization/native_pilot_preflight.py \
  --output-parent "$PILOT_PRIVATE_PARENT"
```

Each invocation creates a unique child directory; it never reuses an earlier
report. Directories have mode `0700`, files `0600`. Read its `report.json` privately.
Without `--output-parent`, the child is named `tellpal-native-pilot-*` under the
resolved system temporary directory; using an explicit private parent makes it
easier for its owner to locate the report.
Terminal output contains only opaque report/status/time summaries, with no output
path, raw identifiers, subprocess output or credentials. Do not pipe the private
JSON or receipts into tracked evidence, CI logs or chat. The parent remains under
the operator's ownership for retention and later cleanup.

Optional local collection is explicitly requested:

```sh
python3 -B tools/image-optimization/native_pilot_preflight.py \
  --collect-local --output-parent "$PILOT_PRIVATE_PARENT"
```

Local collection uses bounded read-only argument-array probes for tool identities
and ADB/devicectl inventory. Every probe has a finite timeout and output cap. It
does not invoke cloud authentication or reservation commands. Device-service,
sandbox, permission, timeout and parse failures are unknown observations with
reasons and retained private receipts; they do not establish device absence.
An inventory row cannot establish that it is the selected phone, a cloud physical
device, a runnable build or a qualified counter.

## Configuration contract

`--config PRIVATE_JSON` accepts one strict JSON object in a regular private file
outside the repository, without symlinks or group/other permissions. For example,
save this configuration privately with mode `0600`; no configuration file is needed
for the default plan.

```json
{
  "schema_version": 1,
  "scope": "image-optimization-selected-pilot-r1",
  "qa_uid": "jLRypTmoBrhhX21HsDJojurcKTC3",
  "selection": {
    "ios": {
      "alias": "pilot-ios",
      "platform": "ios",
      "kind": "physical",
      "provider": "local",
      "selected_model": "iPhone 12"
    },
    "android": {
      "alias": "pilot-android",
      "platform": "android",
      "kind": "physical",
      "provider": "android_device_streaming",
      "selected_model": null
    }
  },
  "cloud": {
    "project_alias": null,
    "account_alias": null,
    "quota_minutes": null,
    "spending_limit_usd": null
  },
  "collection_limits": {
    "timeout_seconds": 8,
    "max_output_bytes": 262144
  }
}
```

```sh
python3 -B tools/image-optimization/native_pilot_preflight.py \
  --config "$PILOT_PRIVATE_PARENT/config.json" \
  --output-parent "$PILOT_PRIVATE_PARENT"
```

| Field | Contract |
| --- | --- |
| `schema_version`, `scope` | Exact values `1` and `image-optimization-selected-pilot-r1`. |
| `qa_uid` | Exact UID selected in [mobile-test-account.json](../mobile-test-account.json). This records selection, not existence, enabled status or server admission. |
| `selection` | Exact platform/kind/provider/model choices shown above. Distinct aliases match `[a-z][a-z0-9-]{0,39}`. Android model stays null in selection; later observed metadata remains separate. |
| `cloud` | Project/account aliases are null or match the same opaque-alias pattern. Minute/cost declarations are null or finite nonboolean numbers from 0 through 1000000. Values remain unverified declarations; they grant no access or spending authority. |
| `collection_limits` | Integer `timeout_seconds` from 1 through 30 and integer `max_output_bytes` from 1024 through 1048576; booleans are rejected. Defaults are 8 seconds and 262144 bytes per probe. |
| `source_snapshot` | Optional exact map of selected repository-relative paths to SHA-256 strings from a previous private report's `source_snapshot`. It must match all current selected inputs; a stale claim is rejected. |

Unknown fields, secret-bearing input, duplicate/malformed JSON, wrong
UID/provider/platform, boolean numeric values and nonfinite numbers are rejected
without echoing input values. Do not add real project IDs, device serials,
reservation IDs, bearer tokens, signed URLs, credentials or arbitrary probe
commands. `--output-parent` must be an existing private directory outside the
repository without symlinks. Unsafe paths and nonprivate locations fail closed.
Source changes require a new observation; never rewrite historical hashes.
Configuration input is capped at 65536 bytes. Its six top-level fields shown
above are required; `source_snapshot` is the only optional field. Nested objects
accept exactly their documented keys. Snapshot values are 64 lowercase
hexadecimal characters; the complete path set must equal the current report's
`source_snapshot`.

## Results and exit statuses

| Exit | Meaning |
| --- | --- |
| `0` | Reserved for native readiness being available; this setup increment cannot produce it. |
| `2` | Invalid configuration, output destination or stale source snapshot; sanitized error. |
| `3` | Valid report produced, native readiness blocked by open gates. Expected for both offline and local-collection runs. |
| `4` | Unexpected operational failure; sanitized error, no readiness claim. |

Handle `3` explicitly in callers; it is a useful blocked-readiness result, not a
benchmark failure. Probe success does not change the five gates. Missing build,
device and counter qualifications remain null with reasons. Private evidence uses
timestamps, provenance, receipt hashes and nullable values; zero is never a
replacement for unknown.

The private `report.json` has the following complete top-level schema:

| Keys | Value contract |
| --- | --- |
| `schema_version`, `scope` | Exact `1` and `image-optimization-selected-pilot-r1`. |
| `report_alias`, `created_at`, `collection_mode` | Generated opaque setup alias, UTC observation timestamp, and `offline` or `local`. |
| `readiness_status`, `native_execution_ready` | Always `blocked` and `false` in this increment. |
| `selection`, `source_snapshot` | Validated selected-platform cells and freshly computed selected source hashes. |
| `qa_account`, `cloud_declarations` | Canonical QA alias/selection source plus unknown account verification; unverified cloud declaration object with evidence reference and reason. |
| `collection` | `tools`, `inventories`, `devices`, `streaming_client_probe`, `raw_receipts_private`; offline lists are empty. Raw traces/paths are private receipts, not terminal summaries. |
| `gates` | Five gate rows: `id`, `status: open`, nullable `acceptance_ref`, `implementation_ref`, `verification_ref`, and concrete `next_action`. |
| `coverage_gaps`, `setup_gaps`, `limitations` | Explicit string arrays of unresolved coverage, setup requirements and claim limits. |
| `build`, `counters` | Nullable observation records; this increment qualifies no app artifact or native counter. |

An observation object has exactly `availability`, `value`, `reason` and
`evidence_ref`. Availability is `observed` or `unverified`; unknown values are
null with a reason. A successful tool probe's identity value contains only parsed
version/build tokens when recognized; otherwise identity stays unknown. The private
receipt retains its raw identity text and SHA-256. Neither establishes build/device/counter compatibility. Tool rows
have `alias`, `identity`, `probe_status`; inventory rows have `alias`, `result`,
`probe_status`. Probe status is `ok`, `missing_executable`, `timeout`,
`output_limit` or `failed`. Inventory parsing can remain unverified even when the
command exits successfully. A successful empty inventory means only that the
service listed zero rows at that time.

Each device observation has `alias`, `platform`, `kind`, `connection`, `model`,
`os_version`, `os_build`, `selected_device_match` and `runnable`. Every field except
`alias` is an observation object. Missing legacy metadata is nullable per row and
does not discard separate complete rows; other Apple platforms are not labeled
as iOS. ADB transport is not proof of physical
hardware/provider. devicectl hardware/connection fields are service observations;
neither parser binds its observation to the selected iPhone/Streaming phone or
qualifies execution. Counter keys are `ram`, `dart_heap`, `dart_external`,
`decoded_live_pending`, `first_target_raster`, `physical_presentation`,
`android_rss_pss_native_graphics` and `ios_footprint_allocations`; every value is
unverified/null here.

Local receipts contain bounded stdout/stderr and command/time/exit/hash
provenance. Detailed arguments, executable paths and service output remain
private even on parse failure. Public handoffs use only sanitized aliases,
timestamps, statuses, hashes and reasons.
For devicectl's JSON side file, half the per-probe budget is reserved for that
file using a child file-size limit and half for stdout/stderr. The combined raw
disk capture stays bounded during collection as well as after it.

## Historical preparation replay

The original preparation snapshot is
`3ee8c2d8ef1e5313e5b827d09eaf6915f2d278fd`; its source baseline remains
`7e3ee8f58a210196ae6988dfa38481d5bd9a7957`. Its matrix, evidence, verifier,
42 tests, proposals and completed handoff stay byte-identical. Their old
current-source verifier may correctly reject this changed checkout.

The replay tool validates those original files in a unique private historical
archive. It uses read-only Git queries, rejects unsafe extraction entries and
keeps the old verifier/tests unchanged. A passing replay proves historical
preparation consistency only; it cannot prove current tool availability, physical
devices or native readiness. Retain the archive and detailed receipts privately.

```sh
python3 -B tools/image-optimization/replay_p0c_preparation.py \
  --output-parent "$PILOT_PRIVATE_PARENT"
```

Optionally supply the original fixed 13 private receipts from a private directory
outside the repository. The operator supplies its path privately; the following
variable is a placeholder, not a known location.

```sh
python3 -B tools/image-optimization/replay_p0c_preparation.py \
  --output-parent "$PILOT_PRIVATE_PARENT" \
  --private-receipts "$PILOT_ORIGINAL_RECEIPTS"
```

Each receipt must be a regular owner-private `0400` or `0600` file in an
owner-private `0700` directory with no symlink components. The tool rereads exact
bytes, checks the original matrix hashes and retains a private snapshot; input
receipts are never changed. Without this option, raw original receipt bytes are
explicitly not rehashed. Hash mismatch or missing receipts cannot produce a
historical pass. Receipt input is capped at 4 MiB per file and 16 MiB total.

| Replay exit | Meaning |
| --- | --- |
| `0` | Historical verifier passes its original 43 bindings and all 42 original tests without skips; no current readiness is asserted. |
| `2` | Invalid CLI arguments or unsafe output destination; sanitized rejection. |
| `4` | Archive/commit/verifier/test, optional private-receipt validation, tamper or operational failure; no historical pass. |

The private child `tellpal-p0c-historical-*` contains `historical-replay.json`,
`archive/` and receipts. Its report identifies the preparation/source commits,
historical status/reason, original hashes, probe provenance and checked counts.
`current_native_readiness_verified` and `native_execution_ready` stay false, and
all five gates stay open. Detailed archive paths belong only to this private
report. The terminal summary contains fixed historical scope/status/counts and
hash identities without raw output, device IDs or private locations.
Archive capture is capped at 640 MiB and 45 seconds; extraction at 10000 entries,
620 MiB total and 32 MiB per file. Traversal, links and special files are refused.
The original verifier and tests have separate finite 30-second and 90-second
deadlines; timeout or missing preparation commit remains a failed replay.

## Local iPhone 12: later human-owned setup

1. The owner connects the iPhone to the Mac with a working data cable, unlocks it
   and accepts the Trust This Computer prompt. Inspect Xcode's device status and
   retain any unavailable reason. Do not upgrade the OS or change deployment
   targets to clear a gap during this increment. Apple separates connection from
   the signed/provisioned artifact needed to launch an app. [Apple: run an app on
   a device](https://help.apple.com/xcode/mac/current/en.lproj/dev5a825a1ca.html).
2. If the observed OS/Xcode pairing requires Developer Mode, the owner enables it
   in Settings > Privacy & Security, restarts the phone and confirms on the phone
   after restart. The option may appear only after initiating pairing with a Mac.
   Record the actual state and OS; do not infer it from the iPhone model. [Apple:
   enabling Developer Mode](https://developer.apple.com/documentation/xcode/enabling-developer-mode-on-a-device).
3. Record a private alias, current model/OS/build, connection/trust/lock state and
   observation time. Connection is not install/run qualification. A later accepted
   isolated target needs its own signing/provisioning, artifact hash, Flutter
   engine/renderer/mode and positive/negative execution receipts. Existing Library
   release guards remain unchanged.
4. Qualify actual Instruments attachment, process footprint, allocation/graphics
   counters, units and capture limits against that build and device. Tool presence
   alone proves none of them. Instruments allocations and footprint have different
   scopes; graphics visibility can also differ by instrument. [Apple: memory use
   information](https://developer.apple.com/documentation/xcode/gathering-information-about-memory-use),
   [Apple: Metal memory analysis](https://developer.apple.com/documentation/xcode/analyzing-the-memory-usage-of-your-metal-app).

## Android Device Streaming: later human-owned setup

Use a compatible existing Android Studio or the current Android CLI. Before a
later approved login or reservation, identify the actual executable privately.
The two local CLI identity checks are `android -V` and `android device remote -h`.
Both must succeed with the current CLI command surface; finding a binary called
`android` does not qualify it. The legacy Android SDK launcher is a different
tool. [Android CLI overview](https://developer.android.com/tools/agents/android-cli),
[remote command reference](https://developer.android.com/tools/agents/android-cli/commands/device_remote).

For Android Studio, verify the installed version against the current [Cloud
services compatibility window](https://developer.android.com/studio/releases).
Opening Device Manager and choosing the Firebase integration is the documented
interactive path. Login and consent remain later human actions; record any work
account policy restriction. [Android Studio service
integrations](https://developer.android.com/studio/services).

Before reserving, the owner records the exact project/account privately, available
quota and billing plan. Google's documented access choices are project
Owner/Editor, or Firebase Test Lab Direct Access Admin plus Service Usage Consumer.
The IAM role name does not change this pilot's provider: the execution path is
Android Device Streaming. The owner selects a physical phone and records the
actual reservation/model/API/OS/build privately after reservation. Streaming
connects remote physical devices through ADB over SSL. [Android Device
Streaming](https://developer.android.com/studio/run/android-device-streaming).

The user declared access to TellPal's Firebase project. This is useful setup
context; the exact project identity, enabled Streaming service, applicable IAM,
remaining quota, billing authorization and reservation are still unverified.
The default private config leaves their aliases/amounts null. Record any later
declaration as a declaration until its actual receipt exists; project access does
not authorize login, client installation or a paid reservation here.

Check project usage and current pricing immediately before a later reservation;
quota is project-level and shared. Do not assume free/unlimited minutes, use Test
Lab test-run quotas as Streaming minutes, or treat an entered spending limit as
a provider-enforced cap. Firebase budget alerts do not cap charges. [Firebase:
usage, quotas and Streaming pricing](https://firebase.google.com/docs/test-lab/usage-quotas-pricing).

The human-owned reservation plan names one project, one model/API, a finite
duration/end time, a maximum approved spend, the responsible operator and a
teardown receipt. Installing or updating clients, account login/consent, enabling
services/partner labs, IAM changes, uploads, reservation creation/extensions and
app installation each require that later concrete scope. None runs here.

## Token-free broker AND backend reachability

Before any later credential handoff, prove both paths from the actual selected
device, in its actual network state:

| Path | Required token-free receipt |
| --- | --- |
| Device to proposed host TLS broker | A bounded harmless challenge reaches the intended broker, validates its TLS identity and returns the expected non-secret response. Record device/build alias, time, transport, result and receipt hash. |
| Device to isolated local/test backend | A bounded token-free request reaches the intended local/test backend and produces a recognizable safe response. Record environment/endpoint aliases, DNS/TLS/transport result and backend receipt. An expected unauthenticated rejection can show reachability; it does not establish Firebase admission. |

These are later qualification criteria, not implemented endpoints or network
commands. A host-side browser/curl success, working ADB session, Streaming video
or broker-only success cannot replace the two device-side receipts. ADB over SSL
does not prove `adb reverse` or the remote device's route to the laptop. Verify
any proposed tunnel/reverse forwarding separately and retain both failures and
successes.

If either path is unavailable, leave credential exchange blocked and specify an
authenticated isolated network alternative for separate acceptance. Do not expose
the backend publicly, swap in production or weaken TLS/admission. Tokens, private
origins, signed URLs and detailed traces stay outside Git, command arguments,
binaries, telemetry and report summaries. Later real authentication verifies the
canonical QA account's existence/enabled status and retains backend token checks,
empty-by-default tester allowlists and server-owned audience fixtures.

## Build modes and counter limits

| Artifact/mode | Permitted interpretation after separate qualification |
| --- | --- |
| Flutter debug | Functional/guard checks only; debug overhead and emulator/simulator behavior do not establish native performance. |
| Flutter profile | Physical-device performance artifact with profiling/tracing features and some service extensions enabled. Record attachment and observer overhead; it does not prove the debugger-free lifecycle path. |
| Android release with `profileable` | A separate manifest/build permission for a subset of Android shell/native profiling. It is independent of Flutter profile mode; verify the packaged manifest and actual tool/device permissions. |
| Plain Flutter release | Debugging and service extensions are disabled. A later isolated accepted entry needs a debugger-free credential mechanism and lifecycle checks; existing `main_library_dev.dart` deliberately refuses release. Do not presume DevTools/counters remain available. |

Flutter documents profile mode on real devices and its differences from release.
The compiled artifact's actual mode/engine/renderer and capture support must be
observed. [Flutter build modes](https://docs.flutter.dev/testing/build-modes).
Android's `profileable` element was introduced in API 29, so it cannot establish
profiling availability at this project's API 24 floor. [Android `profileable`
manifest element](https://developer.android.com/guide/topics/manifest/profileable-element).
Java/Kotlin allocation recording and heap dumps require debuggable processes in
the documented Studio profiler flow; not every task is available for every
process. A streamed ADB connection grants no extra counter permissions. [Android
profiling requirements](https://developer.android.com/studio/profile).

Keep Dart heap/external, decoded LRU, live/pending ownership, Android
RSS/PSS/private-dirty/native/graphics and iOS footprint/allocation counters
separate. Do not add overlapping counters or substitute width × height × 4 for
process memory. Record counter/unit/scope, tool/build/device, clock domain,
planned/actual cadence, missed samples, attachment and observer overhead.
Unavailable support stays null with a reason.

Streaming video latency, compression and screenshots do not establish physical
Android color fidelity, first rasterized target-image frame or physical display
presentation. Image-ready, the first associated rasterized onstage target frame
and physical presentation remain separate boundaries. Missing frame association
never inherits image-ready latency. The [native measurement
protocol](../../_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-measurement-protocol.md)
owns later frozen measurement cells and limitations.

## Later reservation teardown

The reservation owner stops the experiment, cancels owned transfers/collectors,
disposes only the isolated app/cache state and revokes the ephemeral session.
They then return the remote device, confirm reservation release and retain a
private timestamped teardown receipt. In Studio, Google's documented action is
Return and Erase Device. [Streaming session
teardown](https://developer.android.com/studio/run/android-device-streaming).
For the current CLI the documented lifecycle ends with disconnect and removal;
`android device remote remove` releases the reservation and stops billing.
Disconnecting alone is not a release receipt. [CLI reservation
removal](https://developer.android.com/tools/agents/android-cli/commands/device_remote_remove).
Do not run these mutations during setup or target another operator's reservation.

## Native gates remain open

| Gate | Required next receipt |
| --- | --- |
| `NATIVE-ENTRY` | Exact OQ-6 acceptance, isolated entry implementation and positive/negative verification, preserving existing guards and admission. |
| `NATIVE-DEVICE` | Actual connected physical subset, exact isolated build compatibility, service paths and supported counter permissions. |
| `NATIVE-CANDIDATE` | Actual checked source/reference/output identities and per-role geometry/delivery eligibility. |
| `NATIVE-PROTOCOL` | Frozen device/build/candidate/cache/journey/order/repeat/counter boundaries, limits and evidence retention. |
| `NATIVE-DECISIONS` | Agreed quality/resource criteria and explicit unresolved parent decisions. |

All five statuses stay open and `native_execution_ready=false`. Android API 24,
iOS/iPadOS 15, low-RAM devices, tablets/iPads, reader/listening and real
alpha/EXIF/animation coverage remain explicit gaps. Selection, supplied cloud
values, a successful local probe or historical replay closes none of them.

## Host-only verification

```sh
python3 -B -m unittest discover -s tools/image-optimization \
  -p test_native_pilot_setup.py
git diff --check
```

The synthetic tests cover schema rejection, finite subprocesses, private output
and historical replay contracts. They do not validate native/cloud behavior.
Run offline and explicitly requested local preflight separately, handling exit
`3`, and historical replay separately, expecting exit `0` only on a historical
pass. No Flutter/native build is needed for this host-only setup increment.

Official guidance was checked on 2026-10-11. Recheck rolling client compatibility,
IAM, quota/pricing and profiler guidance before the later manual step; this
runbook records prerequisites rather than an executed native or cloud session.

## Android plugin registration verification

The default `test_native*.py` suite uses synthetic private fixtures and requires
no personal SDK, Java installation, signing files or archived APKs. Run it with
the locked host environment:

```sh
tools/image-optimization/.venv/bin/python -B -m unittest discover \
  -s tools/image-optimization -p 'test_native*.py'
```

Real SDK generation, Gradle refusal and historical APK negatives are separate,
mandatory integration evidence for the plugin repair. Invoke them explicitly:

```sh
tools/image-optimization/.venv/bin/python -B \
  tools/image-optimization/android_plugin_registration_integration.py \
  --config "$ANDROID_PLUGIN_INTEGRATION_CONFIG"
```

Create the configuration outside Git with mode `0600`, in an owned `0700`
directory. Every path is explicit and absolute; this example contains placeholders,
not discovered defaults:

```json
{
  "scope": "android-plugin-registration-integration-r1",
  "schemaVersion": 1,
  "flutterExecutable": "/absolute/path/to/flutter/bin/flutter",
  "androidSdkRoot": "/absolute/path/to/android-sdk",
  "javaHome": "/absolute/path/to/existing-java-home",
  "apkanalyzer": "/absolute/path/to/android-sdk/cmdline-tools/current/bin/apkanalyzer",
  "signingConfigFile": "/absolute/private/path/to/existing-experiment-signing.properties",
  "oldProfileBuildReceipt": "/absolute/private/path/to/original-profile/build-receipt.json",
  "oldReleaseBuildReceipt": "/absolute/private/path/to/original-release/build-receipt.json",
  "privateOutputRoot": "/absolute/private/path/to/new-exclusive-integration-output"
}
```

Use the existing selected SDK, signer and r166/r167 archive receipts; never
replace missing inputs with synthetic receipts. Missing, malformed, changed or
incompatible inputs produce a nonzero refusal, never a skip or successful proof.
The output root must be new and have an owned private parent. Inputs, originals
and SDK files remain unchanged; all new traces and receipts stay private.

The harness resolves packages once with `flutter pub get --offline` in a fresh
private source snapshot and checks unchanged lock bytes. It executes the actual
selected SDK generator for both modes: profile must include the project's
`integration_test` plugin, release must exclude that dev plugin, and both must
retain JNI/JNIFlutter registration. A private invocation-only mutant setting
`releaseMode: false` must fail the release content check. No SDK source or
SDK-generated Java is manually edited for this mode check.

The same harness runs the four real Gradle generation refusals, including owned
stale-copy removal, and verifies byte-bound old APK missing-definition negatives.
Its total deadline is 180 seconds and each tool retains the existing private
trace bounds. Exit `0` and `integration-receipt.json` attest all these checks;
exit `4` is refusal. It creates no accepted APK build receipt, device/Cloud session
or physical qualification. Fresh source-bound signed builds and the separate
local runtime check remain required after production helper changes.
