# P3 publication orchestration investigation and proposed boundary

Date: 2026-10-17. Status: proposal for the BMad checkpoint; no implementation or rollout approval is inferred. Baseline `ed4bf60c30d806518fae3394b2d6e0fdfb1b70d5`, branch `codex/image-optimization-p3-publication`. The existing unrelated `.memlog.md` change must remain unstaged and unchanged. No cloud operation, deployment, billing query or database mutation was performed during this investigation.

## Inherited intent and exact acceptance boundary

The user requested P3 after completing the explicitly bounded P2 engine and the separately approved live storage diagnostic. Publish must start durable STORY/READING work for an exact language, automatically adopt only the current eligible verified revision, require no additional per-revision manual approval, preserve a previous eligible LIVE on candidate failure and expose no first-publication LIVE without verification.

The proposed implementation remains explicitly enabled local/test functionality with test profiles and a non-production prefix. Production profiles remain disabled. It provides content-owned publication intent/LIVE eligibility and CMS feedback; it does not migrate ordinary/mobile/listing/original/test-manifest delivery. P4 still owns versioned consumers/access integration. Therefore first-publication and previous-LIVE assertions concern the new internal contract, not the entire existing mutable text/audio/reader experience. CMS must not suggest that this internal test workflow has rolled out to ordinary mobile users.

No new numerical production profile, source normalization policy, URL/cache/session/retention right, backfill, cleanup or cloud execution is proposed. Existing P0 evidence limitations, P2 SDK-only policy and absolute in-flight duration gap survive.

## Read-only findings and reuse map

Java paths in this document are relative to `be/src/main/java/com/tellpal/v2/` unless another root is shown.

| Path / symbol | Observed behavior and implementation consequence |
| --- | --- |
| `content/application/ContentPublicationService.java:42,55` | Transactional Publish currently sets editorial status; archive is separate. Record intent without calling media I/O, then dispatch/reconcile after commit through durable rows. |
| `content/domain/ContentPublicationPolicy.java:21`, `StoryPublicationReadinessPolicy.java:14` | Reuse active/localization/description/cover/nonempty-page and localized text/audio/illustration prerequisites; editorial completeness is not media evidence. |
| `content/application/ReadingImageSourceSnapshotService.java:14,21,51` | Existing capture uses REQUIRES_NEW/REPEATABLE_READ. Preserve its P2 callers; extract pure snapshot assembly for a locked current aggregate inside adoption rather than calling `matches()` as an atomic guard. |
| `asset/api/ReadingImagePreparationApi.java:9` | Explicit `prepare(snapshot, profiles)`, `find(job)`, `retry(job)`. TestProfile has no production admission. Preserve existing callers. |
| `asset/application/reading/ReadingImagePreparationService.java:38,74,91` | Prepare captures latest originals and retained immutable bytes; find independently rereads outputs and records integrity. Both perform external I/O. Neither belongs inside the publication/adoption transaction. |
| `asset/infrastructure/processing/reading/ReadingImageWorker.java:30` and `asset/infrastructure/persistence/reading/JdbcReadingJobStore.java:47` | Reuse P2 durable jobs, leases, retries, immutable manifests and fenced completion. No second conversion engine or asset-to-content dependency is needed. |
| `asset/domain/reading/ReadingJobStore.java` and `JdbcReadingJobStore.java:114` | Integrity observations are append-only; add a transactionally guarded current availability/proof epoch for adoption without rewriting historical manifests. |
| `content/package-info.java`, `asset/package-info.java` | Actual allowed direction is content → asset::api. Content reconciles via asset APIs; asset must not call content or import its entities/repositories. |
| `content/domain/ContentRepository.java`, `content/infrastructure/persistence/SpringDataContentRepository.java:68` | Existing contributor/playback locks are not a general publication mutation guard. All relevant publication/content/page writers must participate in one content-row locking protocol. |

### Mutation coverage

- `content/application/ContentManagementService.java:101,125`: active/deactivation affects all admitted reading scopes. Preserve textless/listening/listing role ownership.
- The same service at `:135,163,190,206`: localization create/update and manual status/ready calls cannot manufacture LIVE. Localized artwork/metadata changes advance only the affected language; closing editorial changes withdraw intent/eligibility.
- `content/application/StoryPageManagementService.java:50,99,114,153`: page add/remove/order changes affect all admitted languages; localized edits affect one language. The last-page removal must persist an explicit invalid desired generation despite the old scheduler's empty-list early return.
- Text/audio-only edits may advance publication generation and policy validation without changing the image revision. Embedded-title image bytes do not change automatically when title text changes.
- `content/application/ContentFreeAccessService.java:46,67`: serialize relevant access changes with adoption and recheck current eligibility; do not invent new entitlement or session policy.
- `content/application/AssetProcessingStatusListener.java:20`: legacy DELIVERY completion remains separate from P3 READY/LIVE.

An active Publish intent follows subsequent relevant edits automatically until withdrawn. Explicit Retry (or a repeated Publish on the current failed intent) may requeue current failed work; there is no unbounded automatic retry loop. Duplicate pending/completed requests are idempotent.

## Source admission: proposed explicit authority

Current manual sources cannot be declared write-once by adding a database flag. `AssetRegistryService.java:79-105` issues a signed PUT/token for a manual UUID path; `FirebaseStorageAssetClient.java:58-75` signs no create-only precondition. Proxy upload (`AssetRegistryService.java:159`) and the legacy writer (`FirebaseStorageAssetClient.java:79`) can reuse that path. Token signature/expiry is stateless (`AssetUploadTokenService.java:64`). Expiry alone does not prove earlier in-flight writes completed. Metadata updates do not prove storage generation freshness.

Proposed bounded choice, to approve with the spec:

1. Add an internal local/test source-admission API. Selected bytes are bounded-read at an exact observed generation, checked for actual supported MIME/SHA/dimensions/full decode, conditionally copied to a new backend-owned reserved namespace, and independently reread.
2. Register the verified destination as a **new ORIGINAL_IMAGE asset ID** with an immutable admission identity/version and separately mutable eligibility epoch/state. The old original is preserved. Existing content bindings are never automatically replaced. A caller explicitly selects the new admitted ID through existing content/page commands; fixtures can do this without a new upload UI.
3. Only these explicitly admitted IDs qualify for the P3 workflow. Ordinary legacy source IDs produce a finite source-not-admitted diagnostic. P2's existing legacy capture API continues working under its existing test boundary.
4. Never mint signed PUT/upload tokens or allow unconditional uploads into the reserved destination namespace, including direct client calls. Reuse existing conditional-create/generation-pinned SDK operations and ReadingObjectStorage. Do not add storage subprocesses, IPC, connection abstractions or a custom retry algorithm.
5. The authoritative P3 source is the selected immutable admission/version, not the latest bytes at the former manual path. Different bytes require a new admitted asset and explicit binding change. Namespace protection is an application guarantee; it does not claim a database can observe arbitrary privileged external cloud writes. Missing/corrupt pinned bytes fail closed.
6. Plan capture ownership before external writes, retain partial/orphan evidence and finalize the new admission/asset atomically. No deletion or automatic legacy sealing/rebinding is permitted. Arbitrary registry rows or metadata updates cannot mint admission evidence.

Proposed files: `asset/api/ReadingSourceAdmissionApi.java`; `asset/application/reading/ReadingSourceAdmissionService.java`; `asset/infrastructure/persistence/reading/JdbcReadingSourceAdmissionStore.java`; `asset/infrastructure/storage/AssetStorageClientRegistry.java` and `FirebaseStorageAssetClient.java`; `asset/application/AssetRegistryService.java`; P2 API/service/work/profile/store files for a typed admitted path while preserving legacy signatures. Source-authority policy must participate in the complete profile/revision identity. Existing conditional immutable operations need no new encoder or SDK dependency.

## Durable publication and atomic adoption design

Add a content-owned scope/generation record, durable reconciliation state and a separate retained LIVE identity. Use an additive `V36__reading_publication_orchestration.sql` after V35; retain ordered scalar provenance without mutable page/localization FKs, and never derive new LIVE from existing PUBLISHED/COMPLETED rows. Snapshot/lease/attempt/source/profile identity and scope uniqueness need PostgreSQL constraints.

Short transactions perform intent creation, claim/attachment, failure/retry or adoption. Capture/prepare/fresh verification occurs outside transactions. Restart after intent, preparation scheduling or READY cannot lose work; restart after adoption cannot duplicate publication. A stale dispatcher cannot attach/fail a newer generation. On a source/profile edit, rebuild only the current candidate; on metadata-only edits, reuse exact verified media when eligible.

Add `asset/api/ReadingPublicationReadyApi.java` with an outside-transaction fresh verification operation and a DB-only adoption guard. The returned proof binds exact scope, source-admission identities/epochs, captured profile/order, job/revision/manifest digest and availability epoch. The guard joins the caller's transaction, locks owned rows in a documented consistent order and rejects a changed proof. It must not call `find()`, storage, signing or another REQUIRES_NEW transaction. Integrity/withdrawal writers use the same row/epoch protocol. Prevent an older observation from overwriting a later withdrawal/failure; test competing verification/adoption observations.

Content locks the current aggregate/scope, reconstructs bindings from that locked state, rechecks desired generation/editorial/access eligibility and asks the public asset guard to compare its exact proof before changing LIVE. Failed, invalid or superseded candidates retain the old LIVE identity. A live lookup checks that retained identity's own eligibility/integrity and current withdrawal rules; it must not compare it with the newest desired page set. Archive/deactivation closes eligibility while retaining evidence; a direct legacy status edit cannot resurrect it.

Proposed content files: `api/ReadingPublicationApi.java`; `domain/publication/ReadingPublication.java`; `application/publication/ReadingPublicationService.java`, `ReadingPublicationReconciler.java`, `ReadingPublicationProperties.java`; `application/ReadingImageSnapshotAssembler.java`; `infrastructure/persistence/publication/JdbcReadingPublicationStore.java`; `web/admin/ReadingPublicationAdminController.java`; existing publication/management/page/free-access/repository adapters. Keep default/production execution disabled at HTTP, worker, retry and lookup boundaries. Configuration must supply explicit role profiles; test values cannot become production defaults.

## Existing consumers remain separate

`PublicContentQueryService.java:66-171` and mapper return mutable original page assets; `JdbcContentPreviewReadRepository.java:12` and `JdbcStoryDetailMetadataReadRepository.java:14` use legacy flags. `StoryDetailHeroQueryService.java:48` signs the current original cover. `user/web/mobile/TestStoryPageMediaController.java:59` follows a separate experiment/allowlist contract. These are P4 migration surfaces and are read-only for this implementation. Preserve existing listing placeholder behavior, curation order, original delivery and tester semantics. `ContentLookupApi` is also an identity/attribution API and cannot become a visibility filter.

The new internal eligibility API returns identities/status only, with no URLs, access/session grants or text/audio snapshot. Its first-publication/previous-LIVE tests must not be described as proving existing ordinary endpoint rollout or the entire retained reading experience.

## CMS integration map and safeguards

CMS paths below are relative to `cms/src/`.

- `features/contents/components/publication-actions.tsx:104,109,175`: existing Publish sends language, immediately toasts publication and disables on editorial PUBLISHED. Make feedback capability/state-aware while preserving disabled-feature legacy behavior. Reuse Button, ProblemAlert/actions and role=status; add no new approval dialog or processing console.
- `features/contents/api/content-admin.ts:88,337`, `model/content-view-model.ts:223`, `mutations/use-content-localization-actions.ts:113`: add exact-language reading status/retry contracts and schemas; preserve editorial status, processingStatus and visibleToMobile semantics. A separate authenticated preparation query is preferable to polling the entire detail response.
- `features/contents/components/localization-tabs.tsx:75-95`: editor keys include status/timestamps. Polling/terminal cache updates must not remount/reset dirty forms. Add explicit user-input preservation tests.
- `lib/query-keys.ts:43`, new `features/contents/queries/use-reading-publication.ts`: exact content/language key; poll active pending states, stop terminal states, distinguish disabled/no-request from network failure and cancel obsolete language requests. Do not add automatic mutation retries.
- `features/story-pages/mutations/use-story-page-actions.ts:31,109`: invalidate the affected reading query after relevant edits.
- `features/contents/components/content-list-table.tsx:24,153`: only add translated blocker labels if the backend emits new codes. Existing registry enum/legacy publication counters are not P3 readiness.
- `app/routes/contents/detail.tsx:29,369`: preserve URL language/workspace selection and existing route layout. No detail redesign or duplicated rail status.

Server capability must identify the local/test contract truthfully. Accepted means requested/preparing; eligible new-contract LIVE means its verified revision is adopted, not ordinary mobile rollout. Failed replacement feedback can report the separately retained eligible revision. Existing media-processing schedule/retry payloads describe another pipeline and cannot be reused as the P2/P3 job contract.

## Verification plan and review evidence

Use real PostgreSQL/Flyway and actual PNG encoding plus disk-backed generation-aware storage, not status-only mocks, for Publish → durable intent → preparation → independent persisted verification → automatic adoption. Cover duplicate requests, two adopters, restart at each commit boundary, stale same-ID/new-admission/profile/order mutations, metadata reuse, last-page invalidation, source withdrawal/integrity races, archive/deactivation/manual-status bypass and language isolation. Assert that no storage/conversion occurs inside DB transactions and all process exits are observed. Retain source/orphan and prior LIVE identities without deletion.

New tests: `be/src/test/java/com/tellpal/v2/asset/reading/ReadingSourceAdmissionIntegrationTest.java`, `ReadingPublicationReadyIntegrationTest.java`; `content/publication/ReadingPublicationIntegrationTest.java`, `ReadingPublicationRaceIntegrationTest.java`, `ReadingPublicationAdminIntegrationTest.java`. Reuse existing P2 ReadingFixtures/real child and PostgresIntegrationTestBase. Keep public delivery/library/test-manifest and module-boundary regression suites enabled. Run focused checks and full `bash ./mvnw verify`; no new live GCS operation is authorized by this plan.

CMS checks: publication-actions/query/mutation tests and content integration tests for pending/failure/retry/automatic adoption, exact language and dirty-form preservation. Add route Playwright and deterministic content-detail visual states with the existing 1280/1440 harness; run `npm run build`. Existing route baseline was inspected during recon; no new implementation screenshot or passing test is claimed now.

Three independent read-only BMad recon agents supplied backend, CMS and consumer maps. This planning record reports no implementation, test/build success, deployment or production readiness. Implementation must receive BMad review and evidence under the approved spec.
