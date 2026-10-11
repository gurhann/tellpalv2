# Selected native pilot setup — r1

Recorded: 2026-10-11T07:46:57Z. Scope: `image-optimization-selected-pilot-r1`.

The selected-platform setup prerequisite is implemented and ready for review.
The local physical iPhone 12 and Android Device Streaming choice is preserved;
selection, service observation, selected-device binding, entry verification and
measurement admission remain separate. All five native gates are **open** and
`native_execution_ready=false`. No signed artifact, authenticated account check,
cloud reservation or benchmark was performed.

## Contract and outputs

- [Canonical SPEC](../../specs/spec-image-optimization-1-5-native-measurement-admission/SPEC.md)
  and [selected pilot scope](../../specs/spec-image-optimization-1-5-native-measurement-admission/native-pilot-scope.md).
- [Setup implementation contract](spec-1-5-selected-pilot-setup.md) and
  [commands, strict schemas and official setup guidance](../../../tools/image-optimization/README.md).
- [Dedicated prerequisite tracker](selected-pilot-setup-status.yaml). The shared
  [sprint tracker](sprint-status.yaml) is unchanged: original Story 1.5 preparation
  remains `review`; Story 1.6 remains `backlog`.
- [Retained completed preparation handoff](p0c-native-readiness.md),
  [matrix](p0c-device-matrix.json), [entry proposal](p0c-native-entry-proposal.md)
  and [measurement proposal](p0c-native-measurement-protocol.md). Their source
  identities and observations apply to their immutable historical snapshot.

The stdlib tools validate a private configuration, capture new source hashes,
optionally collect bounded read-only tool/inventory receipts, and replay the
original preparation in a private safe archive. Output directories are unique
and mode `0700`, files `0600`, outside Git; terminal messages expose no raw
identifiers, private paths, credentials or tool output. The replay never changes
current evidence or relabels historical preparation as current readiness.

## Verification receipts

| Check | Result and provenance |
| --- | --- |
| New synthetic setup suite | `python3 -B -m unittest discover -s tools/image-optimization -p test_native_pilot_setup.py`: **45 tests PASS, 0 skips**. Matrix rejections, mixed Apple metadata/platform isolation, subprocess timeout/output caps, private paths/modes, secret-free CLI errors, stale-source rejection and archive/receipt tampering are exercised. |
| Offline preflight | Valid private report, exit `3`, all gates open. Receipt alias `setup-afd7f98cb5624fa5`, observed `2026-10-11T07:44:03Z`; this is a pre-handoff source snapshot and no device commands ran. |
| Sandbox local preflight | Valid blocked report, exit `3`, alias `setup-568dbaa9c4d242b8`, observed `2026-10-11T07:44:12Z`. ADB service failed and devicectl timed out under sandbox access. Both results remain unknown; raw failed observations are retained privately. |
| Permission-qualified local preflight | Root read-only observation alias `setup-fe4bf70c3074463d`, observed `2026-10-11T07:44:12Z`, private report SHA-256 `ba8fd8dbf5f62624bb76ea72d983aa231e5f591cfdcc3b34351f6806ab707daa`. ADB service successfully listed zero rows. Apple service listed 7 rows: one physical iOS iPhone 12/disconnected, five simulated iOS/disconnected and one incomplete legacy row. Selected-device binding and runnable state remain unverified. |
| Permission-qualified Apple receipt | Native JSON SHA-256 `99380f3a71fa81afecfc6d06f562bfcbae6125b87a6175bd2dd3117a964f821d`. Detailed metadata, IDs and command traces stay private. Missing legacy metadata is nullable per row; other valid observations are retained. |
| Historical replay without raw receipts | Original fixed preparation commit, original **43 source bindings and 42 tests PASS**, exit `0`; no current/native readiness claim. Original private receipt bytes are explicitly not rehashed in this mode. |
| Historical replay with actual original raw receipts | Root private replay **PASS**, started `2026-10-11T07:40:46.110984+00:00`, finished `2026-10-11T07:40:52.729989+00:00`. **43 source bindings, 42 original tests, 13 private receipts rehashed**. Private report SHA-256 `7f73bfbb836a82b3c68606551cf328c46e18fd441e689533f90892f0daaad749`. All five gates open, both native/current readiness false. |
| Preservation and host checks | Eight original preparation/shared-tracker files are byte-identical to `3ee8c2d8ef1e5313e5b827d09eaf6915f2d278fd`; `git diff --check` and local-link checks pass. App/backend/native build, signing and deployment were not changed. |

The local receipts above precede final handoff status/checklist updates. Their
original hashes remain intact. A final refreshed current-source receipt must be
appended after those updates; neither a changed source nor a successful older
probe may silently inherit a current readiness claim. Recheck source bindings
again after any later tool/spec/input change.

## Actual setup gaps

The user declared access to TellPal's Firebase project. Exact project identity,
enabled Streaming service, applicable IAM roles, current quota/billing plan,
approved spending scope and a human-owned reservation have **no verified
receipt**. This declaration permits no automated login, install or reservation.

The two current CLI probes `android -V` and `android device remote -h` failed;
the existing legacy Android launcher is not a qualified Streaming client. A
compatible current Android Studio or current Android CLI must be selected and
qualified before later manual cloud execution. A successful zero-row local ADB
query does not prove absence of a remote physical device or Streaming capacity.

The Apple service's physical iPhone 12 row is disconnected. It is not bound to
the selected pilot device and cannot establish unlocked/trusted/Developer Mode,
runnable signing/build, engine/renderer or Instruments/counter support. Verify
those actual conditions before native entry qualification. Simulated and
incomplete legacy rows do not close physical coverage gaps.

Device-side token-free reachability to **both** the proposed TLS broker and
isolated local/test backend is unverified. ADB-over-SSL, streamed video or
host-only reachability supplies no substitute. No credentials may be handed off
until the separately accepted paths work. No network exposure or production
fallback is introduced here.

RAM, process/native counters, first-target-raster association, physical
presentation and observer overhead remain `null`/unverified. Streaming video
cannot establish device paint, physical Android color or presentation timing.
Flutter profile, Android profileable release and plain release remain separate
future artifact/counter cells. API 24, iOS/iPadOS 15, low-RAM physical classes,
Android tablets/iPads/other controls and reader/alpha/EXIF/animation/non-STORY
native-quality coverage remain gaps.

## Native execution gates and next actions

| Gate | Status | Concrete next receipt |
| --- | --- | --- |
| `NATIVE-ENTRY` | open | Separate exact OQ-6 human acceptance, isolated entry implementation and positive/negative platform verification, preserving current release/admission guards. |
| `NATIVE-DEVICE` | open | Current bound physical subset, compatible isolated signed artifacts, token-free broker/backend paths and actual tool/counter permissions. |
| `NATIVE-CANDIDATE` | open | Independently checked actual source/reference/output files and per-role demand/delivery eligibility, with deferred classes retained. |
| `NATIVE-PROTOCOL` | open | Freeze selected device/build/cache/journey/order/repeats/counters, deadlines, stop conditions and evidence retention. |
| `NATIVE-DECISIONS` | open | Identify required quality/resource criteria and unresolved parent OQ dependencies; adopt no production policy from preparation. |

Preparation is complete with these explicit gaps. Native/cloud behavior remains
unverified. The next manual reservation step needs a concrete project/model,
finite duration/spend, responsible owner and teardown receipt; it is outside this
increment.

## New source and implementation bindings

These retained bindings record the pre-review inputs at index creation, separate from the
original 43-binding matrix and source baseline. The map is rechecked by preflight;
an optional private config `source_snapshot` must match it exactly. Final handoff
source changes require the appended refreshed receipt described above.

| Selected current input | SHA-256 |
| --- | --- |
| `AGENTS.md` | `c818ca54e6a2e3c18179d27b8d2cb338d86a59605035be6b3b4d2c7882857faf` |
| `tools/mobile-test-account.json` | `b40d95bb639b7a8ce37fd7b9f5e25e25ff67a3c38279beb201fc2898f2687636` |
| `app/mobile/lib/main_library_dev.dart` | `3c979c5fdeec7db16ca349d799eb519ef5cab61f143e6cc34846bfab6a7cdf30` |
| `app/mobile/lib/app/library_dev_harness.dart` | `fee53c5e76e91ccbf6b342b67b3b8628c5eccb321af41506567df3642864c2ef` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/SPEC.md` | `62f7b17af14f0ba2a20d6ceeb8ae777c2fb8b9a68df32cbd714e164930537b25` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-pilot-scope.md` | `3d2d4c8798b506020381f3e180383119c6339a19ffacfd613ab2491888b4b824` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-readiness-contract.md` | `65d93546fe9612874d1698a9c1edb3fc4ff31492eab4e00c5798ea0f003cdbd9` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-entry-proposal.md` | `24bbcb0bff0af4ab6639b545e27329703a1e8f521407134de8d55cbb28a3b416` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-measurement-protocol.md` | `4b729c2f8f4f8910369f6b911fb72478d6dfa74d6872e5b8427010e60f15d45f` |
| `_bmad-output/implementation-artifacts/image-optimization/spec-1-5-selected-pilot-setup.md` | `92b626705f31f21d260dac1980683510274b7abc1a948b947868948cbb062b05` |
| `_bmad-output/implementation-artifacts/image-optimization/p0c-device-matrix.json` | `2477a34ae32429258dfadcde78601eed87079bfae2e978a847e67b3735366ed3` |
| `_bmad-output/implementation-artifacts/image-optimization/verify-p0c-readiness.py` | `7415fb2bf9888ca933ef3dc5f1d092788f199962457c9e78db0d80d46e8d3a13` |
| `_bmad-output/implementation-artifacts/image-optimization/test_p0c_readiness.py` | `983318f8964eb058112000bad7cd00caaf9075cce5a8663f08f28a76ef8ca663` |
| `_bmad-output/implementation-artifacts/image-optimization/p0c-native-entry-proposal.md` | `96afb71368b290194b8cfb868a24e51cecbd8cf3032d9b775081f4899f77014b` |
| `_bmad-output/implementation-artifacts/image-optimization/p0c-native-measurement-protocol.md` | `34b448e849e68c73fc5de5966f1755cb5a235252f675dda04c8fe42f78835384` |
| `_bmad-output/implementation-artifacts/image-optimization/p0c-native-readiness.md` | `e8193b5d9c9a56346a17a9dbd2c6eea1c86d044de88c091681e559bdb0298030` |
| `tools/image-optimization/native_pilot_preflight.py` | `3cdba9e7c5e4675a28874b2910508782195387a22dfd312fb7554c5e315ee0a6` |
| `tools/image-optimization/replay_p0c_preparation.py` | `38207a80fb4777a029838ce4c4003a3f6c8614b562ea68215f29359a4465210b` |

| New setup deliverable | SHA-256 |
| --- | --- |
| [native_pilot_preflight.py](../../../tools/image-optimization/native_pilot_preflight.py) | `3cdba9e7c5e4675a28874b2910508782195387a22dfd312fb7554c5e315ee0a6` |
| [replay_p0c_preparation.py](../../../tools/image-optimization/replay_p0c_preparation.py) | `38207a80fb4777a029838ce4c4003a3f6c8614b562ea68215f29359a4465210b` |
| [test_native_pilot_setup.py](../../../tools/image-optimization/test_native_pilot_setup.py) | `13c073fff9d86d3a5f32d0f12ad1b0fcb4a9f5b739bc3990fbe6ecb62f165984` |
| [README.md](../../../tools/image-optimization/README.md) | `9e7481ab9381e2c951ad64ad635a8bfeb06a5dd7d415c38223b23aac0973e58f` |


## Root formal review fixes and re-verification

Recorded: 2026-10-11T07:58:13Z. Three independent root review layers produced four
deduplicated fixes within the existing approved setup intent. No canonical or
frozen intent, app/native entry, original preparation or shared tracker changed.

| Formal finding | Applied fix and meaningful regression |
| --- | --- |
| Non-string Apple reality/connection fields | Type guards preserve array/object fields as per-row unknown/null with fixed reasons; valid neighboring rows survive. The mixed-type regression checks all six rows and evidence references. |
| Successful collection missing from final report | A synthetic successful ADB/devicectl flow now runs local collection through the real report writer and asserts four device observations, both counts, field values and evidence references. Removing `devices.extend(rows)` in memory makes this regression fail (`4 != 0`); implementation on disk was preserved. |
| Oversized JSON integer classified as operational failure | Decoder `ValueError` becomes a fixed sanitized `SetupError`; an integer beyond the interpreter digit limit but below the private config byte cap yields exit `2`, no report and no leaked value. |
| Exact permitted output bound misclassified | Pipe EOF/success at the exact budget is accepted; proven extra bytes are rejected without writing past the budget. Exact 1024-byte pipe capture and exact 512-byte sidefile plus 512-byte pipe pass; one-byte excess cases reject while retaining bounded private evidence. Timeouts, group cleanup and child `RLIMIT_FSIZE` remain enforced. |

| Re-verification after fixes | Result |
| --- | --- |
| Required setup suite | **45 tests PASS, 0 skips** using `python3 -B -m unittest discover -s tools/image-optimization -p test_native_pilot_setup.py`. Native/cloud behavior remains unverified. |
| Offline current-source preflight | Exit `3`, alias `setup-bd5cbf4436ca4d26`, timestamp `2026-10-11T07:54:57Z`, private report SHA-256 `ab6978ae8d6c7aeb25769833b7fdea60b1fb2bfed371d0a00a040915dff3f8c2`. All five gates open; no account/device qualification. |
| Sandbox local current-source preflight | Exit `3`, alias `setup-39b556999c044dba`, timestamp `2026-10-11T07:55:06Z`, private report SHA-256 `7dbc1473f61c2046fb06e975e70ef87e1fe827c9feea77f11b7b65d848a19a59`. ADB service failure and devicectl timeout remain unknown/null with raw private receipts; no device absence inferred. |
| Historical replay after collector fixes | Exit `0`, **43 original source bindings and 42 unchanged tests PASS**. Started `2026-10-11T07:54:56.072981+00:00`, finished `2026-10-11T07:55:04.280570+00:00`; private report SHA-256 `edb1dcc35b3d05bc7efde407e0f1cb3619d160c29ba67f5b607e731f2dab6b3d`. This repeat supplied no raw receipts; the separate actual 13-receipt verification above remains preserved. |
| Preservation/privacy/links | Eight original preparation/shared-tracker files remain byte-identical to the fixed commit; 22 local artifact links pass. `git diff --check` and scoped private-ID/path checks pass. |

The earlier bindings and receipts are retained unchanged. The refreshed source
and implementation hashes below bind this patch revision, before root's final
status/checklist handoff. Root will append the final current-source refresh after
that handoff; no old receipt is rewritten or promoted. Native execution remains
`false`; all five native gates remain open.

| Selected source after formal fixes | SHA-256 |
| --- | --- |
| `AGENTS.md` | `c818ca54e6a2e3c18179d27b8d2cb338d86a59605035be6b3b4d2c7882857faf` |
| `tools/mobile-test-account.json` | `b40d95bb639b7a8ce37fd7b9f5e25e25ff67a3c38279beb201fc2898f2687636` |
| `app/mobile/lib/main_library_dev.dart` | `3c979c5fdeec7db16ca349d799eb519ef5cab61f143e6cc34846bfab6a7cdf30` |
| `app/mobile/lib/app/library_dev_harness.dart` | `fee53c5e76e91ccbf6b342b67b3b8628c5eccb321af41506567df3642864c2ef` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/SPEC.md` | `62f7b17af14f0ba2a20d6ceeb8ae777c2fb8b9a68df32cbd714e164930537b25` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-pilot-scope.md` | `3d2d4c8798b506020381f3e180383119c6339a19ffacfd613ab2491888b4b824` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-readiness-contract.md` | `65d93546fe9612874d1698a9c1edb3fc4ff31492eab4e00c5798ea0f003cdbd9` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-entry-proposal.md` | `24bbcb0bff0af4ab6639b545e27329703a1e8f521407134de8d55cbb28a3b416` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-measurement-protocol.md` | `4b729c2f8f4f8910369f6b911fb72478d6dfa74d6872e5b8427010e60f15d45f` |
| `_bmad-output/implementation-artifacts/image-optimization/spec-1-5-selected-pilot-setup.md` | `e34b241754fef0825890854d5090ddee8b57d7e163e37c8e2304e66d73dd015a` |
| `_bmad-output/implementation-artifacts/image-optimization/p0c-device-matrix.json` | `2477a34ae32429258dfadcde78601eed87079bfae2e978a847e67b3735366ed3` |
| `_bmad-output/implementation-artifacts/image-optimization/verify-p0c-readiness.py` | `7415fb2bf9888ca933ef3dc5f1d092788f199962457c9e78db0d80d46e8d3a13` |
| `_bmad-output/implementation-artifacts/image-optimization/test_p0c_readiness.py` | `983318f8964eb058112000bad7cd00caaf9075cce5a8663f08f28a76ef8ca663` |
| `_bmad-output/implementation-artifacts/image-optimization/p0c-native-entry-proposal.md` | `96afb71368b290194b8cfb868a24e51cecbd8cf3032d9b775081f4899f77014b` |
| `_bmad-output/implementation-artifacts/image-optimization/p0c-native-measurement-protocol.md` | `34b448e849e68c73fc5de5966f1755cb5a235252f675dda04c8fe42f78835384` |
| `_bmad-output/implementation-artifacts/image-optimization/p0c-native-readiness.md` | `e8193b5d9c9a56346a17a9dbd2c6eea1c86d044de88c091681e559bdb0298030` |
| `tools/image-optimization/native_pilot_preflight.py` | `5d40ab23782e031c2ce4274f2875d5562bb6a20a94948b3627e2723c53d60587` |
| `tools/image-optimization/replay_p0c_preparation.py` | `38207a80fb4777a029838ce4c4003a3f6c8614b562ea68215f29359a4465210b` |

| Setup deliverable after formal fixes | SHA-256 |
| --- | --- |
| [native_pilot_preflight.py](../../../tools/image-optimization/native_pilot_preflight.py) | `5d40ab23782e031c2ce4274f2875d5562bb6a20a94948b3627e2723c53d60587` |
| [replay_p0c_preparation.py](../../../tools/image-optimization/replay_p0c_preparation.py) | `38207a80fb4777a029838ce4c4003a3f6c8614b562ea68215f29359a4465210b` |
| [test_native_pilot_setup.py](../../../tools/image-optimization/test_native_pilot_setup.py) | `de80d16164e363d5b13c626e2ba886c8fdba2fbf3c4882dc5b1506bbb8aae728` |
| [README.md](../../../tools/image-optimization/README.md) | `9e7481ab9381e2c951ad64ad635a8bfeb06a5dd7d415c38223b23aac0973e58f` |

## Final current-source refresh

After the execution handoff was marked `done` and its Suggested Review Order was added, root rechecked the final source bindings. These receipts supersede current-source applicability of earlier setup receipts; all earlier bytes and hashes remain historical. Both valid setup reports retain exit `3`, all five gates open and `native_execution_ready=false`.

| Mode | Alias / timestamp | Private report SHA-256 |
| --- | --- | --- |
| offline | `setup-2eb02dd2aa6d4437` / `2026-10-11T08:00:23Z` | `dec3c75a1664afdf14a160ffdabf4436c7ed3d40d6bd9878ccef04217f29a51e` |
| local | `setup-a91a437bc58c4989` / `2026-10-11T08:00:32Z` | `b75a5007aeedd99ac028ea150a6facdea9a99c661fca3d3ff2d92d91f0b64042` |

Both reports bind the same 18 current sources; sorted JSON map SHA-256 is `45e43d757f5c71fd90f3f42636aaf7ee2245f5f295c74175824a13b150421383`. This validates setup provenance, not native execution readiness.

| Final selected input | SHA-256 |
| --- | --- |
| `AGENTS.md` | `c818ca54e6a2e3c18179d27b8d2cb338d86a59605035be6b3b4d2c7882857faf` |
| `tools/mobile-test-account.json` | `b40d95bb639b7a8ce37fd7b9f5e25e25ff67a3c38279beb201fc2898f2687636` |
| `app/mobile/lib/main_library_dev.dart` | `3c979c5fdeec7db16ca349d799eb519ef5cab61f143e6cc34846bfab6a7cdf30` |
| `app/mobile/lib/app/library_dev_harness.dart` | `fee53c5e76e91ccbf6b342b67b3b8628c5eccb321af41506567df3642864c2ef` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/SPEC.md` | `62f7b17af14f0ba2a20d6ceeb8ae777c2fb8b9a68df32cbd714e164930537b25` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-pilot-scope.md` | `3d2d4c8798b506020381f3e180383119c6339a19ffacfd613ab2491888b4b824` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-readiness-contract.md` | `65d93546fe9612874d1698a9c1edb3fc4ff31492eab4e00c5798ea0f003cdbd9` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-entry-proposal.md` | `24bbcb0bff0af4ab6639b545e27329703a1e8f521407134de8d55cbb28a3b416` |
| `_bmad-output/specs/spec-image-optimization-1-5-native-measurement-admission/native-measurement-protocol.md` | `4b729c2f8f4f8910369f6b911fb72478d6dfa74d6872e5b8427010e60f15d45f` |
| `_bmad-output/implementation-artifacts/image-optimization/spec-1-5-selected-pilot-setup.md` | `e09f1ab594f4fa323fdea3bfb59697d9ce7f2ea3133e275ddec9449d2003e6da` |
| `_bmad-output/implementation-artifacts/image-optimization/p0c-device-matrix.json` | `2477a34ae32429258dfadcde78601eed87079bfae2e978a847e67b3735366ed3` |
| `_bmad-output/implementation-artifacts/image-optimization/verify-p0c-readiness.py` | `7415fb2bf9888ca933ef3dc5f1d092788f199962457c9e78db0d80d46e8d3a13` |
| `_bmad-output/implementation-artifacts/image-optimization/test_p0c_readiness.py` | `983318f8964eb058112000bad7cd00caaf9075cce5a8663f08f28a76ef8ca663` |
| `_bmad-output/implementation-artifacts/image-optimization/p0c-native-entry-proposal.md` | `96afb71368b290194b8cfb868a24e51cecbd8cf3032d9b775081f4899f77014b` |
| `_bmad-output/implementation-artifacts/image-optimization/p0c-native-measurement-protocol.md` | `34b448e849e68c73fc5de5966f1755cb5a235252f675dda04c8fe42f78835384` |
| `_bmad-output/implementation-artifacts/image-optimization/p0c-native-readiness.md` | `e8193b5d9c9a56346a17a9dbd2c6eea1c86d044de88c091681e559bdb0298030` |
| `tools/image-optimization/native_pilot_preflight.py` | `5d40ab23782e031c2ce4274f2875d5562bb6a20a94948b3627e2723c53d60587` |
| `tools/image-optimization/replay_p0c_preparation.py` | `38207a80fb4777a029838ce4c4003a3f6c8614b562ea68215f29359a4465210b` |

The permission-qualified final local query again listed zero ADB rows and seven Apple service rows. One physical iOS iPhone 12 registration is disconnected; five iOS simulated records and one unknown legacy record remain separate. This is service metadata, not fresh on-device OS/build verification or selected-device/runnable qualification. Both current Streaming CLI probes failed. No login, installation, reservation, credential exchange or benchmark was performed.

Final historical replay with the actual original private receipts: **PASS**, finished `2026-10-11T08:00:32.036195+00:00`, report SHA-256 `742147db107a56314ecc8ebaeac50ed4a80f50b0e3dfe8e16bd417cf198f925f`. All **43 source bindings, 42 original tests and 13 receipts** passed. Current/native readiness remain false.
