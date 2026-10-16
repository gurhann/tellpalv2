# P2 preimplementation investigation

Date: 2026-10-16. Status: investigation and proposed implementation boundary; **not implementation acceptance or production approval**.

## Verified baseline

- Initially clean `codex/image-optimization-p1-contract`, HEAD `7dafccf22862decb001a56cd5b904480b1528912`; the requested P1 commit exists and is the checked-out baseline.
- Created local branch `codex/image-optimization-p2-generation` without modifying P1.
- Loaded root instructions, project memory, relevant ADRs, canonical architecture, the P1 contract/specification, parent SPEC/implementation phases, rendition evaluation and P0f quality findings. No `project-context.md` was found in the repository scan.
- Rendered `bmad-build` exactly once using its instructed command. Current workflow stage: step 02, investigation and draft specification. Session model selection is inherited.
- Local runtime: OpenJDK Homebrew 25.0.2. Docker initially failed sandbox socket access; an approved read-only retry reached the socket but reported that the daemon is unavailable. PostgreSQL/container verification has not run.

## Human decisions

On 2026-10-16 the human selected all three recommended bounded P2 options below. These decisions authorize the engine/test-profile implementation boundary, not a numerical production profile or production access policy.

| Decision | Recommended bounded P2 option | Alternative and consequence |
| --- | --- | --- |
| Profile qualification | Build the preparation engine with explicit local test profiles, PNG lossless encoding and configurable geometry. No enabled production profile and no production READY without separate qualification. | First qualify a representative real corpus and devices and select production profiles; this adds native evaluation before implementation admission. |
| Source semantics | Accept only single-frame, opaque 8-bit sRGB PNG/JPEG with supported identity orientation; reject unsupported EXIF/mirroring, ICC/color conversion, transparency and animation explicitly. Do not infer semantics from MIME or a decoder accepting frame zero. | Include EXIF 1–8 transformations and alpha-preserving PNG initially; requires additional normalization implementation and qualification fixtures. |
| Access and retention | Internal backend contracts only, private immutable objects, no new delivery endpoint/signing/cache/renewal right. Track retained sources, outputs and orphan attempts without deletion. | Decide and implement protected delivery/session/retention policies now, expanding beyond the proposed P2 engine boundary. |

Absence of production numbers is intentional. Test geometry, byte/pixel/memory/time/disk/concurrency settings must be explicit, fingerprinted and labelled test-only. They cannot silently become production defaults. Equal/growing-byte outputs are rejected unless the selected profile explicitly admits them with an evidence reason; synthetic test admission does not authorize production admission. A lossless encoder does not make resizing lossless. Source insufficiency must fail rather than be certified through upscaling.

## Existing code and reuse constraints

Java paths below are relative to `be/src/main/java/com/tellpal/v2/`.

| Location | Evidence and P2 implication |
| --- | --- |
| `asset/application/LibraryCoverPreparationService.java`, `publish` | Short locked-job transaction saves manifest and completes work. Reuse the atomicity pattern, not its ID/label-only freshness lookup. |
| `asset/domain/AssetProcessing.java`, listing ownership checks; `asset/api/ListingCoverAttempt.java` | Captured attempt/lease checks are useful precedent. P2 also needs explicit PROCESSING/first-terminal-outcome, identical read-only replay and contradictory-callback rules. |
| `asset/infrastructure/media/RegisteringAssetProcessingJobExecutor.java`, story-page path | Existing page manifest publication and completion are separate. Do not certify this legacy path as P1 compliant. |
| `asset/infrastructure/persistence/JdbcStoryPageMediaManifestStore.java`; `JdbcListingCoverRevisionStore.java` | Existing-key/ON CONFLICT behavior is insufficient; compare the complete immutable semantic manifest before returning idempotent success. |
| `asset/infrastructure/storage/AssetStorageClient.java`; `StorageObjectMetadata.java`; `FirebaseStorageAssetClient.java` | No generation-pinned observation/read or conditional immutable create; ordinary upload can overwrite. Add explicit immutable operations without reinterpreting existing upload/delivery semantics. Capture the actual GCS generation and verify reread bytes. |
| `asset/infrastructure/storage/AssetProcessingPathBuilder.java` | Preserve mandatory environment prefix isolation and add separate revision/source/rendition namespaces. Do not overwrite originals. |
| `asset/infrastructure/media/PageImageRenditionProcessor.java` | Real ImageIO JPEG, full source decode, bicubic scaling and implicit white matte. Its 1440/q85, alpha behavior and profile label are experiment settings, not a qualified P2 profile. |
| `asset/infrastructure/processing/AssetProcessingPoller.java` | Durable polling precedent; no adequate conversion-process teardown or combined resource admission. |
| `content/application/StoryPageManagementService.java` | Stable localized page IDs/order are available inside the owner. Existing scheduling returns early for zero pages. A new explicit invalid snapshot must be representable without changing the legacy worker or implementing Publish. |
| `content/api/PublicContentQueryApi.java`; existing admin views | No complete exact-language cover/detail/page binding snapshot contract. Add a content-owned immutable scalar snapshot; asset must not import content entities or repositories. Caller-driven snapshot submission avoids a new asset-to-content module dependency. |
| `asset/domain/MediaAsset.java`; registry service | Mutable registry metadata is not immutable source proof. Capture/version actual bytes outside transactions. Recheck registry binding and pinned source generation; report changed binding instead of certifying current freshness by ID alone. |
| `V32__add_story_page_media_revisions.sql` | Existing retained page FK depends on mutable page lifetime. New P2 manifests use historical scalar page/localization provenance, no FK/cascade to editable page rows. Legacy V32 is not migrated or certified by implication. |
| `V34__listing_cover_revisions.sql` | Latest migration at inspection. Reserve V35 only after rechecking the migration inventory before implementation. |

## Proposed implementation shape

1. Define exact STORY/READING scope, content-owned ordered required bindings and explicit invalid/empty snapshots. P2 returns READY evidence only. Publish intent/generation mutation, source-change invalidation across publication flows and LIVE promotion remain P3 obligations.
2. Capture original bytes and immutable source versions outside transactions. Hash actual bytes and all effective profile/provider/runtime/normalization/resource settings using P1 canonical framing. Profile/source/binding changes cannot reuse an ID-only result. Capture admissible source objects under create-only immutable ownership and reread them before scheduling the captured revision.
3. Persist deduplicated revision work, full captured descriptors, immutable evidence, attempt diagnostics and isolated orphan ownership. Full ordered cover/detail/page membership is required. New tables protect retained snapshots independently of mutable page rows.
4. Claim jobs transactionally with a complete attempt token. Perform source download, conversion, upload and independent stored verification outside transactions. Finalize manifest plus job under one lock/transaction. Explicit retry claims a new attempt; old success/failure cannot alter a replacement or terminal outcome.
5. Separate conversion into a supervised child JVM with explicit heap/direct-memory/encoder-thread settings and bounded files/streams. Bound full source raster admission, encoded source/output bytes, aggregate temp space, per-source/revision wall time and concurrency together. Kill and await process exit before releasing local permits; future cancellation alone is not teardown evidence. Record actual RSS/native/container limits and unavailable measurements honestly. Production container/resource admission remains disabled until measured; a heap cap alone is not an RSS cap. Qualify distributed lease recovery against overlapping old workers rather than assuming lease expiry kills a process. Do not adopt read-time subsampling without separate quality controls.
6. Independently reopen each stored immutable object/version, check actual content format, storage MIME, positive byte count, SHA, exact dimensions and full decode. Corruption, truncation, missing objects, partial uploads, unsupported semantics or conflicting equality prevent the entire revision from becoming READY. Reuse requires fresh stored-object proof; integrity failure is separately recorded without changing historical manifest identity.

## Verification to implement

- Real Java 25 PNG encoding and independently reopened local storage; source checksum unchanged, direct-from-original geometry, complete role/page order, explicit byte growth/reuse admission. A preliminary codec probe is runtime availability evidence only.
- Missing/truncated/wrong-MIME/wrong-SHA/wrong-dimension/wrong-version outputs and storage interruption: zero partial READY. Detect malformed input rather than trusting frame-zero decode.
- PostgreSQL 15 tests using `support/PostgresIntegrationTestBase`: concurrent dedup/claim, lease replacement, restart after upload and commit, rollback of manifest/job together, terminal first-outcome/replay/contradiction, explicit retry, retained-page deletion and full conflict equality.
- Resource tests: encoded byte and pixel bombs, many frames, oversized metadata, slow/hung child, forced termination and confirmed exit, memory/disk/concurrency exhaustion. Include backend-parent memory and the complete input raster, not output dimensions alone.
- `ApplicationModulesTest` and Flyway on an empty DB; focused tests followed by `bash ./mvnw test` and `bash ./mvnw verify` with retained logs. Check for skipped database tests; skipped tests do not prove acceptance.
- Actual encoder probe in the packaged Java 25 runtime, including provider/runtime identity. No deployed environment or Firebase writes are implied. Mock storage tests do not establish live GCS conditional-generation behavior; retain that distinction and any unavailable evidence.

## Evidence limits and preserved boundaries

The synthetic local Java 25 preflight passed two real disk-persisted codec round trips and four missing/truncated-output negatives. PNG preserved every RGB pixel of the 257×193 fixture. Retained reproduction source and raw output: [`ImageIoPreflight.java`](../../../be/docs/evidence/p2-imageio-preflight/ImageIoPreflight.java), [`raw-results.txt`](../../../be/docs/evidence/p2-imageio-preflight/raw-results.txt). PNG: 149157 bytes, SHA-256 `ea6033481d8418140d31723627177e652da8b25e02533c642c3793ac3c9a09da`; JPEG: 23857 bytes, SHA-256 `1153c14ab2c07422ddc555cbc2a3dfbc9fa33186b5c9f7009ade9f1370be57a1`. Source SHA-256 `11dac658331dc2bd9adc189a3b2238b3ef9d012331e4036b77052ea186cac9fa`; raw output SHA-256 `5f8d6e3f95622d5489a1158713cfac3010011d3c02d6112eb0c9db34491c366d`. MIME in this probe is derived from actual reader format; it is not external storage metadata verification. JPEG q85 is a synthetic runtime control, not an admitted P2 output profile.

Reproduce from the repository root using a fresh output directory: `probe_dir=$(mktemp -d /private/tmp/tellpal-p2-preflight.XXXXXX)` followed by `java be/docs/evidence/p2-imageio-preflight/ImageIoPreflight.java "$probe_dir"`. The probe creates only its own new fixture files and does not delete them.

P0 Android 1.0× and iPhone 0.75× PNG acceptance is limited to displayed TR71/TR19 portrait examples. Android 0.75× embedded text was blurry; JPEG/WebP sharpness remains unresolved. Missing original raw native ledgers/sidecars/build/cleanup evidence remains a limitation. No whole-corpus/device/production quality or saving is inferred.

P3 Publish/CMS/LIVE adoption, P4 consumer access and production profiles, deployment, backfill and deletion are not completed by this P2 engine. The approved automatic preparation/promotion intent remains unchanged; no per-revision manual approval is introduced.

Technical references checked during planning: [Java 25 ImageReader](https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/javax/imageio/ImageReader.html), [Java 25 Process](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Process.html). Abort is a request; termination must be observed. The online pinned GCS Java 2.58.1 reference was unavailable through the browser; verify the locally resolved exact dependency API before implementation rather than relying on a latest-version signature.

## Planning checkpoint verification

The draft specification contains 1361 tokens measured with `o200k_base` (a planning-size measure, not a session billing measure). The primary agent independently reran the retained Java probe in a fresh temporary directory; provider identities, encoded byte counts/checksums and all six positive/negative outcome rows matched. `git diff --check` passed; new artifact whitespace and relative file links were checked separately. No Maven, PostgreSQL, packaged-container, live Firebase or P2 implementation acceptance is claimed. Implementation waits at the requested bmad-build step-02 checkpoint, after the three product decisions were answered.
