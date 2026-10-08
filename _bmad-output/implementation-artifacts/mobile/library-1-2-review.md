# Story 1.2 complete-diff review

Baseline: `8380313c4ed32a471e68b83ad780827a92dffcc0`.
All three context-free review layers completed before triage. The complete private diff includes tracked and untracked paths; no staging was used. Root applies patches because the original implementation agent is no longer available with its context.

| Finding | Severity | Route | Evidence and required patch |
|---|---|---|---|
| R1: recycled loaded Hero ignores its replacement provider failure | medium | patch | Loaded → disposal → remount retains loaded status, so loading-only callbacks reject the new failure. Give the remounted consumer a fresh bounded attempt; reject obsolete callbacks. |
| R2: pending Hero decoder key is untracked at disposal | medium | patch | Provider ownership is assigned only after decode in imageBuilder. Track the exact admitted ResizeImage key before resolution and evict it on disposal; verify late real codec completion cannot repopulate it. |
| R3: long fallback title overflows the Hero | medium | patch | Unbounded Text in the fixed Hero Column displaces localized recovery. Bound visible title while preserving full parent heading semantics and 48dp recovery. |
| R4: Hero dimension multiplication can overflow | high | patch | Positive 64-bit dimensions 4611686018427387904 × 3 bypass the 40M ceiling. Use overflow-safe division after positive guards; verify real HTTP parsing and boundary values. |
| V1: detail pagination beyond the first hundred candidates lacks regression coverage | medium | patch | Existing detail fixture has eight candidates; the home pagination test exercises another service. Add real PostgreSQL detail coverage with a full rejected candidate batch followed by ordered eligible results. |

These are local implementation/verification omissions with unambiguous remedies under the already approved bounded artwork, failure recovery, accessibility and ordered recommendation contract. No frozen intent, policy, architectural choice or unspecified product behavior changes. No findings require intent_gap/bad_spec loopback; review_loop_iteration remains zero. None are deferred.

Patch verification is pending. Previous source ce5446d70fbd2cf4d841bcc93e69bf0f55749bd0496f40d505610d2a77e30b73 evidence remains historical once production code changes; it cannot certify the patch source. Final acceptance and workflow completion remain open.

## Patch source verification

All five patches are implemented. Ten Flutter regressions were added: real HTTP dimension boundaries/overflow (six), recycled current consumer failure/timeout (two), pending real codec disposal (one), and long-title scaled semantics/retry reachability (one). Before the fix, dimension overflow was accepted, the title produced an 8621-pixel RenderFlex overflow, and both remount cases retained loaded status. A separately bounded decoder regression failed because the pending key remained cached after disposal. All now pass in the full223-test serial suite; existing assertions and limits are unchanged.

Code generation and analyzer pass. Independent audit matches101 build sources/five SVGs, all nine executed matrix rows, and the unchanged approved frozen intent. Native source90 seal: `cb6f2759ac2e1f32493d7a67ed65772ebb6822eb869c0a19a6b5dcd77daced04`.

Full configured Maven verify passes385 Surefire tests plus one Failsafe documentation integration test, with no failures/errors/skips. The new detail pagination fixture rejects the first100 candidates (selected plus99unready) and verifies exactly five later-page IDs in reverse insertion/CMS order. See [backend verification](library-1-2-review-backend-verification.json).

The initial default-parallel full Flutter run passed222 cases and failed the shared home/detail image overlap assertion already present in the pre-review Story1.2 diff (expected two decoded RawImages, observed zero). Exact cause is unproven; no source, assertion, wait or budget was changed between that run and the passing full serial suite, which uses the preceding acceptance command. The failed run remains separately qualified in build evidence. This test is part of Story1.2, not an unrelated baseline defect; a fresh default-parallel full-suite check remains planned after the active native replay.

Current-source native/profile/reference/platform replay remains pending. Previous native46 passing runs and1387 original PNGs are historical under the new seal; none is mislabeled current. No cache reset, vendor edit, wait/budget increase, new account, remote mutation or image optimization was introduced.

### Current native checkpoint A42: Android phone baseline

The unchanged original baseline target passes16journeys plus real unavailable behavior with49acknowledged original PNGs and zero explicit cover recoveries. Source90 seal, installed APK and unchanged canonical dataset match. Official SDK --no-start-paused is a host execution flag; repository assertions, captures, source limits and waits remain unchanged. The prior source is not reused as current proof. Remaining19required targets and platform/reference refresh remain pending.

The pending-codec regression was additionally strengthened to observe the actual engine codec Future completing before the late-cache assertions, rather than relying on a short elapsed delay. Full223 serial tests pass again with every original assertion. This changes only test source, leaves all90native source hashes and the current replay seal byte-identical, and does not invalidate running device targets.

### Current native checkpoint A42: Android tablet baseline

The unchanged original target passes with53original PNGs, exact source/installed APK/dataset proof and two distinctly qualified real-control cover recoveries (TR and PT). Each observes a current recommendation error, taps the actual localized control once, commits fresh same-context metadata, independently compares HTTP data and decodes the Hero plus every originally required visible/partial recommendation cover. This is not an uninterrupted healthy baseline. Final structured failure observations include admission/package-source TimeoutExceptions; exact underlying cause remains unproven. Earlier progress based only on SDK text logs did not expose those failures and is superseded by this structured result. No wait, limit, source assertion or capture requirement was changed to accept it.


### Current native replay A42 complete; test-only synchronization follow-up

All20 required original native targets now pass on source90 seal `cb6f2759ac2e1f32493d7a67ed65772ebb6822eb869c0a19a6b5dcd77daced04`, with550acknowledged original PNGs, source/installed binary comparisons and restored isolated NON_SUBSCRIBER fixture. Android tablet has two NON_SUBSCRIBER and three SUBSCRIBER distinct, qualified current-error/real-control recoveries; other baseline profiles need no recovery. These are not uninterrupted healthy tablet baselines. Original46 historical passing runs/1387PNGs remain separate.

A second default-parallel full suite passed222 and failed the EN/EN warm-file assertion after a decoded RawImage was already present. Neither that failure nor the earlier overlap failure is reclassified as unrelated or proven external. A test-only real-cache observer delegates to the unchanged actual IO/HTTP/codec/promotion pipeline. The EN warm case deliberately gates publication, verifies real decoded pixels with no immutable warm file, then releases and awaits actual publication before every original warm/source/owner assertion. Real decode/publication completion now replaces incidental30x10ms frame pumping for these IO cases, with a finite deadline using the same existing10s test-cache budget. Production/native waits, limits, assertions and capture requirements are unchanged. This demonstrates the missing asynchronous test prerequisite; it does not establish the unrecoverable exact timing/provenance of both prior failures.

Focused recommendation tests, clean analyzer, full223 default-parallel and full223 serial suites now pass without skips. All101build source/fiveSVG hashes, nine executed matrix rows and unchanged frozen intent independently match. Test-only source changed; native90source is byte-identical, so all20 current device proofs remain valid. Current iOS/Android platform AX and settled reference refresh are still required before Step5.

### Current platform checkpoint A46/A44

iPhone current normal-entry actual CUA inspection now verifies12loaded and9failure/unavailable localized cases, each with actual Back to Library. Actual TR1.0 switch1→0 and recommendation button/child/Back preserve parent narration0. Both phone normal entries match all90current source files and all five installed binary parts. CUA element scrolling returned without viewport movement and coordinate dragging returned `noWindowsAvailable`; accessible card activation works, and no physical CUA scroll success is claimed. Original20 native targets independently prove SDK drag/card/Back/scroll retention.

iPad actual TR1.0 switch/card/Back also passes, with parent narration retained. Five loaded iPad cases are currently observed; remaining cases are open because the tool again explicitly reported a locked Mac during the sixth case. Human unlock requested. The selected iPad row/window and screenshot identify the task A16 simulator; DeviceHub Inspector accessibility text retained stale iPhone values despite the visible Inspector showing iPad UDID, so stale Inspector AX text is not used as identity evidence. Exact configured UUID/installed-container proof separately binds the normal entry.

Current original iOS subscriber rasters are inspected and hash-linked. Read-only current Android raster counts verify actual opaque source tokens; seven opaque contrast pairs and257CTA samples meet their thresholds. iOS PNGs embed Display P3: an exploratory exact sRGB-code assertion was invalid across that profile and failed. Raw P3 counts are explicitly not sRGB token/raster contrast certification; source-token contrast remains a separate calculation. No original images were altered.

### Additional current Android AX-only harness A45

The separate phone AX-only target failed after its first persisted PNG, with hostTransportFailureType StateError and no acknowledged actual AX snapshot. It is unaccepted and is not part of original20 native acceptance. The private controller omitted the host driver's required LIBRARY_AX_PROFILE; the current tool environment does not supply it. FreshA48 explicitly supplies phone/tablet and a private wrapper preflight rejects missing profile before launch. The original failed stage was not retained, so exact exception provenance is not asserted retrospectively. No application/native source, original target, wait, capture or AX assertion was changed.

### Current checkpoint after A43/A48

Both separate current-source iOS real404 reference targets pass6cases/6originalPNGs each with all five installed binary parts matched; actual route completed/originzero/eight original frames/48dpBack/sameLibraryowner checks pass. All six enlarged TR/EN/PT rasters are actually inspected, with readable settled messages and no Library transition strip. Read-only booted-device cache audits pass bounds and zero duplicate/orphan/missing files; optional unknown lengths stay qualified. Earlier shutdown-container audit attempts remain unaccepted precondition failures.

Current Android phone/tablet normal entries verify42loaded/failure controls plus six separate AX-only beyondviewport metadata/card snapshots underA48. The corrected private host profile invocation passes without changing application/native90sources, original targets, waits/captures or AX assertions. FailedA45 remains unaccepted; success does not recover its exact original exception stage. Current iPhone21controls and iPad5loaded plus actual switch/card/Back are sealed. Only remaining7loaded/9failure iPad actual-platform observations and final workflow presentation remain development gates. Human Mac unlock request is pending; no active native/normal-entry job remains.


## Final current review-patch verification — 2026-10-08

The remaining seven loaded and nine failure iPad CUA observations passed, each followed by actual Back to Library. Current iOS24loaded/18failure and Android24loaded/18failure platform controls are verified with source/installed-binary proof; six additional Android card/section AX snapshots have separate, narrower scope. Actual narration toggle and recommendation/nested-Back interactions passed on both iOS profiles. Physical CUA viewport scrolling remains tool-limited; original native SDK gestures independently verify drag/card/Back/scroll retention. DeviceHub stale Inspector identity values are excluded from identity proof. All normal-entry and proxy jobs have ended; the final iPad read-only cache audit passes without reset, with optional unknown metadata lengths retained explicitly.

Current223 default-parallel and223serial no-skip Flutter tests, clean analyzer/generation,101source/fiveSVG hashes, unchanged frozen intent, all9executed matrix rows and configured Maven385Surefire+1Failsafe tests pass. Original20current native targets/550originalPNGs match native90seal `cb6f2759ac2e1f32493d7a67ed65772ebb6822eb869c0a19a6b5dcd77daced04`. Separate current iOS12settled real404 references/12originalPNGs pass with six actual enlarged raster inspections. Five complete-diff review findings are patched and verified; no intent loopback or unresolved implementation finding remains. Android tablet baselines retain two NON_SUBSCRIBER and three SUBSCRIBER qualified cover recoveries; exact underlying causes are unproven. Historical failed/unaccepted attempts remain preserved and are not acceptance evidence.

Implementation verification is complete within the approved emulator-first scope. Spec status is done; the explicit mobile tracker is review for human handoff, with Epic1 still in progress. Physical devices, minimum/older OS and production quantitative memory/latency remain release gaps. Controlled detail EXT-CATALOG integration closes for this consumer only. Publish-time cover/content image optimization remains [separate follow-up planning](../../planning-artifacts/post-story-1-2-image-optimization.md); no optimization implementation or production release is implied. This dated closure supersedes pending checkpoint statements above without rewriting their historical scope.
