# P3 local verification evidence

Date: 2026-10-17 (Europe/Istanbul). Baseline: `ed4bf60c30d806518fae3394b2d6e0fdfb1b70d5`.
Scope: the approved P3 specification and mandatory investigation. Implementation and initial verification completed before three independent BMad reviews. All six deduplicated findings were triaged as patches; all six repairs were inspected against their executed checks and closed by the parent workflow. No cloud run, deployment, production activation, backfill or deletion was performed. The unrelated `.memlog.md` change remains untouched.

## Implementation boundary

V36 adds new source-admission, asset availability and content publication records without certifying historical media. Explicit admission verifies pinned supported bytes, plans ownership before conditional storage creation and atomically registers a **new** original ID. Originals and interrupted admission evidence survive; content must explicitly bind admitted IDs. Legacy writes, signed upload/token paths and arbitrary registry registration cannot target the reserved admission namespace.

Content Publish commits intent without media I/O. The asset API supplies a complete effective role/limits/encoder/runtime profile-set identity outside DB transactions; desired intent persists it, drift renews generation/capture even with unchanged limits, and explicit Retry detaches obsolete job bindings. Exact-language generation/profile identity and expiring dispatch claims survive restart. Current editorial/binding/profile checks plus the asset DB-only complete proof guard automatically adopt verified current media. Retained LIVE stores its own generation, revision and manifest digest independently of desired generation. Archive/deactivation closes retained eligibility; a later Publish cannot reopen it before adoption. Failed/invalid replacements preserve still-eligible prior LIVE.

Source admission and fresh source/output verification use P2's parent memory admission and one shared monotonic revision deadline. Storage SDK in-flight/slow-trickle limitations remain open. Asset proof availability epochs prevent older observations from overwriting later failure/withdrawal. Capacity, parent-memory admission and elapsed-budget rejections are inconclusive and preserve healthy retained availability; observed pinned media corruption or missing output still closes it. No asset-to-content dependency, storage process/IPC layer, access URL or session/cache/retention policy is added.

## Executed checks

| Check | Result | Local raw log |
| --- | --- | --- |
| P3 focused PostgreSQL tests before final expired-claim case | PASS: 16 orchestration tests + 1 authenticated HTTP test | `/tmp/p3-final-focused.log` |
| Existing P2 preparation/guards plus initial P3 matrix | PASS | `/tmp/p3-focused.log` |
| First complete backend `verify` | FAILED: 449 tests, 2 failures (latest migration 35→36 expectation) + 2 errors (mocked persisted content ID missing); no skips | `/tmp/p3-verify-first-complete-failed.log` |
| Repair-focused P3/P2-guard/snapshot/migration check | PASS: 36 tests, no failures/errors/skips; exit 0 | `/tmp/p3-repair-focused.log` |
| Pre-review backend `bash ./mvnw verify` with PostgreSQL/Flyway/module boundaries | PASS: 450 unit/integration/property tests + 1 Failsafe module documentation test; no failures/errors/skips; exit 0 | `/tmp/p3-verify-final.log` |
| CMS all Vitest before final free-access invalidation | PASS: 90 files, 376 tests | `/tmp/p3-cms-all.log` |
| Pre-review CMS all Vitest | PASS: 90 files, 376 tests; exit 0 | `/tmp/p3-cms-all-final.log` |
| Existing CMS content create/edit/publish browser flow | PASS: 1 test; exit 0 | `/tmp/p3-content-e2e.log` |
| CMS content-detail Playwright/visual, 1280 and 1440 | PASS: 8 tests; exit 0; disabled/pending/failed/LIVE plus retry/adoption dirty-input assertions | `/tmp/p3-visual-final.log` |
| CMS production build before review | PASS: exit 0; existing >500 kB bundle warning retained | `/tmp/p3-cms-build-final.log` |
| Post-review complete backend `bash ./mvnw verify` | PASS: 455 Surefire + 1 Failsafe tests, no failures/errors/skips; BUILD SUCCESS, 03:34, exit 0; all final repair code included | `/private/tmp/p3-review-verify.log` |
| Post-review focused backend | PASS: 39 tests, no failures/errors/skips; exit 0 | `/private/tmp/p3-review-focused-repaired.log` |
| Post-review all CMS Vitest | PASS: 90 files, 383 tests; exit 0 | `/private/tmp/p3-review-cms-all.log` |
| Post-review CMS production build | PASS: exit 0; existing >500 kB bundle warning retained | `/private/tmp/p3-review-cms-build.log` |
| Post-review existing content Playwright | PASS: 1 test; exit 0 | `/private/tmp/p3-review-content-e2e.log` |
| Post-review content-detail 1280/1440 Playwright visuals | PASS: 8 tests; exit 0 | `/private/tmp/p3-review-content-visual.log` |
| Incidental broader visual selection | FAILED: 31 tests selected, 15 passed / 16 failed; exit 1; all 8 P3 content-detail cases passed | `/private/tmp/p3-review-visual.log` |

PostgreSQL 15 runs through the existing Colima VM with `DOCKER_HOST=unix:///Users/gurhankucuk/.colima/default/docker.sock`, `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` and `TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1`. Storage uses real disk-backed generation-aware conditional objects and actual PNG bytes; preparation uses the real supervised child encoder. ReadingObjectStorage/ReadingPreparationCapacity reject transactional I/O, so successful end-to-end cases also exercise the no-I/O-in-transaction boundary. These are local fixtures, not live Firebase qualification.

## Acceptance and edge-case matrix

All methods below are in `content/publication/ReadingPublicationIntegrationTest` unless qualified. The final Maven log/XML count/name summary is retained in [`backend-test-summary.txt`](backend-test-summary.txt).

| Approved matrix scenario | Executed method/evidence | Assertion |
| --- | --- | --- |
| First Publish | `publishCommitsIntentThenActualStoredBytesAreAutomaticallyAdoptedAndDuplicatesAreIdempotent` | Intent commits before first LIVE; actual stored role outputs decode independently; complete verified current revision adopts automatically. |
| Duplicate / restart | Same method; `restartAfterSchedulingAndReadyRecoversAndConcurrentAdoptersCommitOneLive`; `restartAfterExpiredDispatchLeaseRecoversIntentAndRejectsOldAttachment` | `ReadingPublicationSchedulerIntegrationTest.publishAutomaticallyReachesLiveThroughSpringScheduledReconcilerAndWorker` additionally calls only Publish and waits for actual scheduled polls. Duplicate pending/LIVE requests are idempotent; READY and abandoned dispatch recovery; two concurrent adopters and stale claim rejection. |
| Failed / invalid replacement | `failedLegacyReplacementRetainsLiveAndExplicitRetryDoesNotSpin`; `metadataOnlyEditReusesJobAndLastPageRemovalExplicitlyInvalidatesWhileRetainingLive`; `integrityLossRetryRequiresFreshSuccessfulVerificationAndSourceWithdrawalFencesProof` | Retained live g1/revision/digest stays separate from desired g2; finite failure/retry; no automatic failed-work spin; corrupt output retry fails until equal-byte repair and fresh proof. |
| Mutation / obsolete completion | `staleClaimAndForgedSnapshotSourcesOrProfilesCannotAdopt`; `profileChangeAfterReadyAdvancesGenerationAndCannotAdoptOldProfile`; `languageMutationsDoNotAdvanceOrAdoptAnotherScope`; `sameLimitsDifferentRuntimeIdentityRecapturesAndRetryDetachesObsoleteCompletedJobWhileRetainingLive`; `newPageLocalizationCommandPersistsPositiveBindingBeforeAdvancingAndAdoptingDesiredGeneration`; metadata/last-page method | Exact job work/snapshot/admissions/profiles, current generation/profile and language fences. P2 `sameBytesWithReorderedPagesOrChangedRoleGeometryCreateDifferentRevisions` additionally covers captured order/profile identity. |
| Archive / deactivation / bypass flags | `archiveThenChangedSourceAndNewPublishCannotReopenHistoricalLive`; `deactivationAndManualFlagsCannotManufactureLive` | Withdrawn historical proof remains retained but ineligible across changed source/new Publish; manual flags cannot create LIVE. |
| Source / integrity change | `olderSuccessfulObservationCannotOverwriteALaterIntegrityFailure`; `admissionOrphanEvidenceSurvivesInterruptedCreateAndPinnedSourceCorruptionFailsClosed`; source admission and integrity/retry methods | Real pinned-source/output corruption, concurrent older-success/later-failure CAS fencing while preserving healthy retained LIVE during a new observation, source withdrawal, reserved-write rejection and durable planned orphan ownership. `freshVerificationEnforcesParentMemoryBeforeDecodeAndOneSharedRevisionDeadline` proves resource-budget rejection without false integrity observations. `inconclusiveCapacityRejectionPreservesHealthyLiveButActualCorruptionClosesIt` holds parent capacity during verification and proves healthy retained LIVE survives, then deletes actual output and proves fail-closed. `AssetAdminIntegrationTest.authenticatedRegistryRejectsBackendOwnedReadingNamespaceWithoutInsertingMedia` proves HTTP rejection has no registry side effect; its normal allowed registration test also passes. |
| CMS polling | `use-reading-publication.test.tsx`; `publication-actions.test.tsx`; `localization-tabs.test.tsx`; content-detail visual spec | Exact language keys, cancellation, terminal/network stop, no mutation retries, pending/failure/retry/LIVE copy, retained revision feedback and dirty title/language preservation across polling/retry/adoption. |
| Disabled / production | `disabledAndProductionAdmissionPreserveLegacyAndNeverPrepare`; `disablingDuringExternalVerificationCannotAdoptAnInFlightProof`; `ReadingPublicationAdminIntegrationTest.authenticatedCapabilityIsExplicitDisabledByDefaultAndLanguageValidationIsFinite`; existing P2 production rejection | Disabled capability performs no new preparation/adoption; production+test profiles cannot activate; admin authentication and exact-language validation remain enforced. |

### Ordinary consumer acceptance

`enabledInternalLiveDoesNotChangeOrdinaryOriginalListingOrTesterContracts` compares concrete API outputs immediately before and after **enabled internal LIVE** adoption: public details, public original page references (including the bound original illustration ID), ordered public summaries, listing scalar previews, listing cover placeholders and the existing tester manifest lookup remain equal. No experiment manifest is created by P3. This directly covers the approved ordinary-contract AC for the enabled transition.

The existing public-delivery, Library/story-detail, tester HTTP/auth and module-boundary suites additionally run under full `verify`. Those regression suites establish their own unchanged contracts; they do not claim the enabled transition migrated any consumer or supplied protected-media rights. Hero/signing and a complete retained text/audio reading experience are not asserted by the new image identity contract.

## CMS visual artifacts

`cms/e2e/visual/content-detail.visual.spec.ts` uses fixed time, deterministic auth/API responses, reduced motion and 1280/1440 viewports. New baselines:

- `cms/e2e/visual/content-detail.visual.spec.ts-snapshots/content-detail-reading-pending-{laptop,desktop}-chromium-darwin.png`
- `cms/e2e/visual/content-detail.visual.spec.ts-snapshots/content-detail-reading-failed-{laptop,desktop}-chromium-darwin.png`
- `cms/e2e/visual/content-detail.visual.spec.ts-snapshots/content-detail-reading-live-{laptop,desktop}-chromium-darwin.png`

The existing disabled baselines were visually inspected and refreshed for 11/13 pixels of rendering noise without a layout change. The second complete screenshot comparison passed. The pending/failed paths also edit a title and assert it survives automatic adoption/explicit retry and that the URL language remains Turkish.

### Incidental broader visual result

`npm run test:e2e:visual -- e2e/visual/content-detail.visual.spec.ts` retains the script's fixed `e2e/visual` argument and therefore selected the entire 31-case visual suite. This is **not** a broad visual PASS. Four failures are missing Darwin category-detail/contributors-registry baselines (only Windows baselines existed in HEAD). Twelve registry/mockup cases have identical expected/actual dimensions and strict comparisons reporting 1, 3, 4, 6, 11 or 13 differing pixels. Representative registry-toolbar and mockup STORY detail diff images were visually inspected: sparse text/edge pixel differences, no shifted/added/removed layout or content. This is consistent with platform rasterization drift; no browser-version cause is asserted.

The router, mockup routes/fixtures, registry route, `ContentPageShell`, shared primitives, styles and these three visual specs have no diff from HEAD. Mockup pages import their own fixture UI and do not use the changed `PublicationActions`, localization form or reading hook. This supports separating those failures from the P3 behavior change; it does not make the broader suite successful. Its raw failure screenshots, contexts and automatically created missing-baseline images were moved to `/private/tmp/p3-review-unrelated-visual-artifacts/`; no unrelated baseline was refreshed or added. The correct scoped command, `npm run test:e2e -- e2e/visual/content-detail.visual.spec.ts`, separately passed all 8 cases. Own test-results artifacts were removed from the working tree and tracked `.last-run.json` restored to its initially clean content.

## Retained failure history

- Docker's configured desktop daemon was stopped. The existing Colima VM was started instead; no new Docker installation was needed. Global Docker context was restored to its initial `desktop-linux` value; command-scoped DOCKER_HOST was used throughout. After final checks, the parent confirmed no running containers and stopped Colima, restoring its initial stopped state without deleting volumes.
- The first PostgreSQL fixture used `active` rather than existing `is_active`; corrected before behavioral validation.
- A source-withdrawal negative test exposed Spring repository translation of `IllegalStateException`; a dedicated finite domain rejection now preserves the expected admission outcome.
- The initial HTTP fixture omitted required STORY `page_count`; corrected to zero.
- The first complete verify exposed two unit fixtures without persisted content IDs after pure assembly extraction and two latest-schema assertions still expecting V35. Fixtures now supply ID 9 and expect additive V36; migration semantics were unchanged.
- Legacy CMS route mocks initially lacked the new disabled capability and clicked before capability settled; the mocks now explicitly provide disabled state and await the enabled action.
- The initial strict disabled visual comparison differed by 11/13 pixels; inspected, refreshed and rerun successfully.
- Availability observation reservation initially closed healthy retained LIVE before I/O. Reservation now increments only the fence epoch; the concurrent observation test asserts old LIVE remains eligible until a real failure withdraws availability.
- The first post-review focused run (`/private/tmp/p3-review-focused.log`) exposed an enum-key canonical hash mismatch, duplicate test disk-client bean wiring and the old P2 deadline diagnostic expectation. String keys, a test-only disk holder and the inconclusive deadline assertion repaired these; the next focused run passed all 39 tests. No failed full verify was relabeled as successful.
- Intermediate Maven verify runs were interrupted to include completed live-generation, constructor-injection, consumer-contract and shared-budget changes. The first completed failed run and repaired targeted run remain recorded above; the pre-review successful full run covers that implementation snapshot. The separately recorded post-review full run covers all accepted patches.

Raw logs remain outside the repository because test HTTP failure dumps can contain ephemeral synthetic bearer tokens. This document and the safe count/name summary retain results without committing tokens or private URLs.

## Remaining gates

The generated asset/content public API AsciiDoc lists include the new P3 interfaces. Nondeterministic PlantUML ordering changes were restored to their initial versions.

All six accepted patches are closed after parent inspection and final verification in [review](review.md). The pre-existing broader visual-suite baseline issues are recorded in the append-only deferred-work log. Production profiles/activation, cloud qualification, SDK absolute in-flight duration, broader source normalization, protected/versioned consumer access, P4 migration, device quality, cache/session/retention policy and rollout remain separate gates. No ordinary mobile rollout is claimed.
