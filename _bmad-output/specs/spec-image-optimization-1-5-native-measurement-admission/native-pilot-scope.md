# Selected Native Pilot Scope

## Accepted platform choice

On 2026-10-11 the user selected a local physical iPhone 12 and Android Device Streaming for remote physical Android testing, and requested continuing after the spec update. This resolves platform selection and authorizes setup/preflight work. Exact entry acceptance, implementation, verification and frozen batch admission remain separate receipts.

The user also reports access to the TellPal Firebase project. This is user-declared access; the exact project ID, current Streaming roles/quota and active reservation have not been observed. Do not convert it into an IAM or quota verification receipt.

| Cell | Selected scope | Evidence required before execution |
| --- | --- | --- |
| iOS | User-owned local physical iPhone 12 | Current connection/developer mode, model/OS/build, runnable isolated signed artifact, engine/renderer/mode and actual Instruments/counter support |
| Android | One reserved physical Android phone through Android Device Streaming | Current Streaming client, project/IAM/quota, provider/reservation alias, physical model/API/OS/build, ADB connection, isolated artifact/signing/instrumentation and counter permissions |
| Additional coverage | Explicitly deferred | API 24, iOS/iPadOS 15, low-RAM classes, Android tablets, iPads and other modern controls remain gaps until actually observed |

Device selection is not a floor, model family, fleet or production-performance claim. Simulator/emulator checks may support guard and functional verification but cannot replace physical pilot quality/resource evidence.

## Setup and trust boundaries

Use an existing current Android Studio or current Android CLI Streaming client. The legacy Android SDK `android` launcher cannot reserve Streaming devices. Inspect client availability without login, installation, app upload or reservation mutations. Record actual project permission and minute quota before reservation; never assume free or unlimited use. Android Device Streaming is the selected provider; Firebase Test Lab is not the execution path.

Streaming provides remote physical ADB access over SSL. This does not prove `adb reverse`, profiler permissions or reachability of the laptop's local/test backend and proposed TLS broker. Prove host-service reachability with token-free traffic before any credential handoff. If unsupported, record the gap and concretize an authenticated isolated network path; do not expose the backend or substitute production services automatically.

Keep the canonical QA account and existing backend verifier/allowlists. Account existence/enabled status is verified only when the later authenticated flow is authorized and performed. Tokens, signed URLs, raw device/reservation IDs, media and detailed command traces stay outside Git, telemetry, binaries and command arguments. Setup reports distinguish local device metadata, connected state, build/install/run qualification and counter availability.

Local iPhone inspection can qualify supported physical screen/color cases. Streaming video quality, network lag and screenshot timing cannot establish physical Android color fidelity, first paint or presentation latency. Device-side target-frame association and memory counters require actual support; unavailable values stay null with reasons. Keep each platform's tool/counter scope, clock, overhead and network conditions separate.

## Versioning and next sequence

1. Preserve original preparation at commit `3ee8c2d8ef1e5313e5b827d09eaf6915f2d278fd`; its source baseline remains `7e3ee8f58a210196ae6988dfa38481d5bd9a7957`. Original relative links and evidence apply in that snapshot.
2. Replay original verifier/tests in a private read-only snapshot. Changed current sources must not be accepted by rewriting the old matrix.
3. Create selected-pilot preflight tooling and a runbook. Capture new observations privately with timestamps, nullable values, provenance and failures; report missing clients/account access/devices explicitly.
4. Qualify the connection and token-free service path, then complete the exact native entry's acceptance, implementation and positive/negative verification.
5. Independently admit actual candidates and freeze role/geometry/cache/journey/order/repeat/counter cells before Story 1.6. Retain failed/not-run rows and supported-floor gaps.

Preparation can complete with documented gaps. Story 1.6 remains gated until the relevant NATIVE-ENTRY, NATIVE-DEVICE, NATIVE-CANDIDATE, NATIVE-PROTOCOL and NATIVE-DECISIONS receipts pass; selecting these two platforms closes none of those verification gates by itself.
