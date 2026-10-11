# Native Entry and Instrumentation Proposal

## Status and current evidence

This is the required shape of a reviewable OQ-6 proposal. Its target names and seams below are proposed, not selected or implemented. Story 1.5 must turn them into exact platform-specific proposed patches and name unresolved choices before requesting entry acceptance. Preparing or accepting the story spec grants no entry implementation or native execution approval.

| Existing seam | Observed boundary |
| --- | --- |
| `app/mobile/lib/main_library_dev.dart` | `runLibrary` refuses release before configuration; sets decoded LRU controls to 48 entries / 32 MiB |
| `app/mobile/lib/app/library_dev_harness.dart` | Ephemeral debugger/integration configuration; release install/wait rejected; development service extensions |
| `app/mobile/lib/app/library_configuration.dart` | Ephemeral URL/token/context, finite catalog/artwork waits; no server signature verification |
| `app/mobile/lib/app/injection.dart` | Dedicated GetIt composition with generated injectable configuration |
| `app/mobile/lib/core/diagnostics/library_diagnostics.dart` | Bounded catalog latency and sanitized errors only |
| `app/mobile/lib/core/network/library_image_cache.dart` | Account/access cache and owned verified transfer/file lifecycle; `profile` seam allows separate configuration |
| `app/mobile/lib/core/network/story_detail_image_cache.dart` | Separate bounded hero profile and admitted SHA/size checks |
| `be/src/main/java/com/tellpal/v2/user/web/mobile/LibraryHomeTestController.java` | `/api/test/library` exists in local/test only; metadata grants no playback rights |
| `be/src/main/java/com/tellpal/v2/user/application/LibraryHomeTestQueryService.java` | Verified Firebase identity, configured UID admission and server-owned subscriber fixtures |
| Android app build / iOS Podfile | API 24 / iOS 15 floors; current Android release uses debug signing, which does not establish store signing or an isolated experiment artifact |

Existing service-extension cache counts and loaded states are not target-frame, transfer, owner-residency or process/native measurements. Recheck these code facts against the source revision recorded in the preparation output.

## Proposed isolated composition

The reviewable proposal should use a separate experiment target and app composition, preserving `main.dart`, `main_library_dev.dart` and all existing release checks. Suggested additions to make the review concrete:

| Proposed path/change | Required behavior and verification |
| --- | --- |
| `app/mobile/lib/main_image_experiment.dart` | Separate entry; fails closed without a valid admitted experiment session; production/dev targets remain intact |
| `app/mobile/lib/app/image_experiment_configuration.dart` | Binds approved scope/batch, private environment origin, session generation, admitted source/candidate manifest and isolated cache identity; refuses missing, malformed or stale configuration |
| `app/mobile/lib/app/image_experiment_bootstrap.dart` | Receives an ephemeral session through a separately accepted platform mechanism, independently of development service extensions; excludes credentials from defines, assets, UI, logs and persisted experiment evidence |
| `app/mobile/lib/app/image_experiment_composition.dart` | Owns separate GetIt/injectable transport/cache/diagnostic lifetime and teardown; reuses existing feature/domain contracts without importing infrastructure into domain |
| `app/mobile/lib/core/diagnostics/image_experiment_recorder.dart` | Bounded sanitized records for request/decode/frame and counter observations with attempted/not-run/failure/null states; cannot replace application failure behavior |
| Dedicated experiment cache/provider adapters | New namespace per batch/account/access/revision; verifies exact candidates and matching decode providers; preserves accepted cache settings and files |
| Android/iOS experiment build definitions | Distinct application/bundle identity, target/flavor/configuration and output destination; records actual signing/build/engine/renderer identities; cannot overwrite an existing installed app or enter production delivery |
| Experiment-only host driver and checks | Private session handoff, bounded scenario selection, raw evidence retention, stop/teardown and negative guard verification; synthetic tests remain labeled |

These are proposed patch locations, not instructions to implement them in Story 1.5. Reusing existing screens requires preserving inherited crop, aspect ratio, overlays and bounded provider identity. Maintain feature-first Clean Architecture, BLoC/Cubit, `auto_route`, `get_it`/`injectable` and app-owned disposal; route disposal cannot end an application media session. A new domain feature or replacement production auth flow is not implied.

## Authentication and delivery decisions required in the proposal

The exact Android and iOS credential handoff must be specified separately: host/device transport, trust boundary, storage/memory lifetime, replay/expiry checks, revocation/teardown and debugger-free operation. A local JWT claim/expiry check is only selection hygiene; the backend remains the verifier. Use the canonical QA UID after checking account existence/enabled state if a later authenticated flow is performed. Do not create an alternative account or modify claims/subscriptions by implication.

The proposed entry must retain server verification and the relevant allowlists. It may use a profile build for performance and a separately verified debugger-free release-equivalent build for lifecycle validation; passing the existing development entry's profile branch alone does not close OQ-6. Name exact lifecycle/instrumentation differences between those artifacts rather than asserting equivalence from a mode label.

Offline candidate files are not served by the existing Library contract automatically. The proposal must describe either an explicitly admitted isolated fixture path or a separately scoped controlled delivery adapter, with exact byte/manifest identities and auth/access behavior. State which claims it can support. Local fixture decode cannot measure protected network delivery or real catalog/reader journeys. New upload/endpoints/storage rights and future reader geometry require their own concrete scope before implementation; no public/raw-original fallback is introduced. Existing metadata eligibility cannot be used as playback authorization.

## Verification required before OQ-6 closes

- Existing Library release entry and harness still refuse release; ordinary product targets expose no experiment bootstrap, service endpoint or configuration surface.
- Missing/malformed/expired/replayed session, wrong UID/access scope, wrong batch/revision and disallowed environment cannot start candidate delivery. Backend token/admission rejection remains visible; local checks never replace it.
- Artifact and installed application identities are isolated; tokens, credentials and signed URLs are absent from defines/assets/binaries and sanitized logs. Exact source/build/lock/signing/engine/renderer provenance is retained privately where required.
- Candidate corruption, wrong MIME/dimensions/checksum, unknown manifest entry, excessive input and obsolete callbacks fail without raw fallback or current-state mutation.
- Experiment cache reset/teardown touches only its owned namespace, releases active/pending owners and cancels owned transport without deleting accepted cache/session media. Account/session changes cannot reuse another admission scope.
- Instrumented decoded-ready and target-frame association refer to the same immutable candidate/provider/decode/attempt. Unavailable frame/counter support is reported with reasons; it cannot pass a first-painted/resource criterion.
- Profile and debugger-free lifecycle paths are built/run on the declared physical subset with the frozen checks; emulator/debug checks remain supplementary. Verification reports the actual mode and attachment state.

## Rollback and approval handoff

The final proposal records exact additions/patches, build/install commands, controlled backend/fixture configuration, generated-file impact and private artifact locations. Rollback stops the experiment, disposes its composition/owned transfers, revokes the ephemeral session, removes only its application/cache artifacts and restores only scoped experiment configuration. Original/live/session-required objects and accepted application data remain protected.

Record OQ-6 human acceptance, entry implementation and verification as three separate receipts. If any is absent, report NATIVE-ENTRY open. Story 1.6 additionally needs its admitted candidates, physical subset and frozen protocol; this proposal supplies no native result or production policy.
