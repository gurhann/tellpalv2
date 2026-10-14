# Android experiment plugin registration review

Review baseline: `cc917796e78c1361cb9036bd38666fd9c197c519`.
Approved frozen intent SHA-256: `d0a9eb8bf03de058c9ce9a0278269d578315898c36785f129f8aa3aecca9081e`.
Three context-free review layers examined the complete tracked and untracked implementation diff. Review patches preserve the approved intent and do not allocate Cloud devices.

| Accepted finding | Severity | Route | Resolution |
| --- | --- | --- | --- |
| Default host test discovery required personal signing and archived APK paths | medium | patch | Move actual SDK/Gradle/archive checks into an explicitly invoked integration harness with operator-supplied private configuration. Keep default tests synthetic. |
| Whitespace-only DEX class/method headers could raise `IndexError` | low | patch | Validate token lists and refuse with the fixed admission error. Add malformed-header cases. |
| A nonpublic registrant class could satisfy the public method check | medium | patch | Require a public registrant class as well as its public static registration method. |
| Mocked SDK invocation did not detect a broken release-mode selection | medium | patch | Execute the selected SDK in both modes, inspect JNI/development-plugin content, and reject the actual `releaseMode: false` mutant. |

The post-review runtime source selection contains 292 inputs on SHA-256 `c50615b6a410ec58212c62666a3551d9ce89d867a21c3138340d783137a5332e`. Earlier r208/r209 signed builds and r223 local success remain historical evidence on their original source selection; fresh signed artifacts and a local run are required for this selection.

Physical Android profile/release entry, the physical negative matrix, Story 1.5/OQ-6, presentation, benchmark and candidate admission remain open.

### Current verification checkpoint — 2026-10-14

The review patch passed 193 portable native host tests with zero skips, the explicit r227 SDK/Gradle/archive integration checks, and a fresh run of all 33 Apple/native build guards and mutants. Release r233 and profile r234 signed app/test builds passed admission on the 292-input source freeze `c50615b6a410ec58212c62666a3551d9ce89d867a21c3138340d783137a5332e`, with unchanged signer, engine and locks. The preceding r229 build retained a failed admission because the operator selected the legacy APK Analyzer; no accepted receipt was written. The corrected builds use the existing current SDK analyzer.

| Current product | App APK SHA-256 | Test APK SHA-256 |
| --- | --- | --- |
| Release r233 | `ee3771a9a0c9be56430f559c0e1622abf2d1a175dffaf4144fe923c4b3ed210f` | `6a3bafac265e80b8f2f62735d6954b94792403c8b3554255a4fc4b2dbb0242bf` |
| Profile r234 | `b15acdfa99e3d57f23f54ffab3cfaa0fc82ad96ad384b7bbc3e9e9f0f7b16890` | `f1dcf1c3f821d1e28c986b7864fb413f12c86a5ce55273fa043640f455c59a47` |

Fresh local attempts r231 and r237 bound the installed r234 app/test bytes, passed both token-free probes, one pairing report, consumed proof and canonical QA admission, then refused at `NATIVE_ENVELOPE_TIME_TOKEN` before fixture delivery. They completed in 6.328 and 5.342 seconds. Both attempts have empty before/after UID crash buffers and successful broker/server/reverse cleanup; neither is a positive entry receipt. The combined native predicate does not identify its failed condition. The 906-byte token excludes an oversized-token explanation; clock measurements alone do not establish the cause. The r235 clock-only attempt refused alignment and did not launch the app. Original failures and clock observations are retained privately.

The earlier r223 full fixture/decode success is historical evidence for r209 and must not qualify the latest source-bound artifacts. Verification remains open; no final acceptance or physical qualification is claimed. No Cloud session was allocated during this repair. A read-only 2026-10-14 04:47 UTC check found the r226 emulator/ADB processes absent, ports 5038/5584/5585 closed and the former local QA backend listener on 61433 closed. A future run needs fresh owned runtime readiness and authentication evidence.

### Local time/token diagnosis completed — 2026-10-14

Current r234 profile entry passed twice on the unchanged 292-input `c50615b6a410ec58212c62666a3551d9ce89d867a21c3138340d783137a5332e` source selection: r243 with a passive observer in 13.447 seconds, then r244 without that observer in 11.938 seconds. Both used the original canonical QA, pinned TLS, time/token predicates, installed APK/UID/stat binding and launcher outcome validation. Each observed both probes, one pairing report, authenticated admission/status, one complete 357-byte PNG, explicit authenticated app revoke and native settlement. Each independently extracted app ledger contains 14 records, zero drops, encoded verification, draw and actual 128×160 decode. Both before/after UID crash buffers are empty.

The cold emulated clock initially measured approximately 642 milliseconds behind the host. A bounded persistent ADB channel reduced connection overhead, independently checked Apple/Google NTP, and estimated actual clock-command application latency. At most three correction passes, each capped to 25 milliseconds and the original ten-second channel deadline, target measured UTC; there is no arbitrary future pad. Final median offsets were +7.052 milliseconds versus the +5.595-millisecond reference in r243, and +9.078 versus +8.304 in r244, each within the five-millisecond verification tolerance. Host time, Android time-server settings and native validators were preserved.

The passive r243 observer retained only numeric envelope fields: TTL 176,387 milliseconds and token byte count 905. It returned the original immutable claim response without modification and wrote no token. Device/host clock brackets near envelope issuance were consistent with the successful original time predicates. The unobserved r244 repeat rules out dependence on this wrapper. This resolves the current local runtime verification; the exact failed clause in the earlier combined refusals was not observed and is not retrospectively asserted.

Preserved unsuccessful preparation evidence includes r240's stopped-container dynamic-port declaration check, r241's first clock alignment refusal, and r242's missing Python spawn entry guard in the private observer. The r242 import guard was corrected only in the separately copied operator; a passive spawn import and actual subsequent authentication passed. No application, SDK, signer, dependency, Firebase worker, credential identity or production source changed during this diagnosis.

Private originals and bound helpers are under `local-time-token-r240` through `local-time-token-r244`. The exact owned r241 backend/emulator/ADB process start times, command hashes and process groups were checked before shutdown; ports 61433/5038/5584/5585/18347 are closed. Database data, the canonical QA account and the existing replacement key remain retained for unfinished physical verification. No Cloud session, IAM/key change or deployment occurred. Physical Android profile/release and the negative matrix, Story 1.5/OQ-6, presentation, performance and candidate admission remain open.
