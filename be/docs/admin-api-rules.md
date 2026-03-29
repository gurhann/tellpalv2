# CMS Admin API Rules

## Purpose

This document is the canonical rulebook for CMS-facing admin APIs in the TellPal V2 backend.
Use it before implementing or changing CMS query screens, forms, mutation flows, and local sample
data.

Scope is limited to CMS-backed admin modules:

- content and story pages
- contributors and free access
- categories and curation
- assets and media processing

Admin authentication is intentionally out of scope for this document.

## Verification Standard

Rules in this document are recorded only when they are supported by at least one of the following:

- an admin controller contract
- an application service, validator, or domain aggregate invariant
- an integration test or controller test

If a behavior is not verified, do not treat it as a rule. Record it as a gap or follow-up item
instead.

## Maintenance Rule

When a new CMS-related backend validation or precondition is discovered:

1. verify it in a controller, service, validator, domain rule, or integration test
2. update this document
3. then continue frontend implementation

## Shared Admin API Behavior

These behaviors apply across all CMS admin controllers because they use the shared admin exception
stack.

- Missing or invalid bearer token returns `401`.
- Permission failures return `403` with `errorCode=access_denied`.
- Bean validation failures return `400` with `errorCode=validation_error` and a `fieldErrors`
  object.
- Malformed JSON bodies return `400` with `errorCode=invalid_body`.
- Application or domain `IllegalArgumentException` returns `400` with
  `errorCode=invalid_request`, unless a module-specific exception handler maps the case to a more
  specific code.
- Recent-list endpoints sanitize large limits down to `100` instead of failing:
  - `GET /api/admin/contributors`
  - `GET /api/admin/media`
  - `GET /api/admin/media-processing`
- `GET /api/admin/contents` is not paginated and includes inactive content.

## Content and Story Pages

### Verified Sources

- `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java`
- `be/src/main/java/com/tellpal/v2/content/web/admin/StoryPageAdminController.java`
- `be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java`
- `be/src/main/java/com/tellpal/v2/content/application/ContentPublicationService.java`
- `be/src/main/java/com/tellpal/v2/content/application/StoryPageManagementService.java`
- `be/src/main/java/com/tellpal/v2/content/application/ContentAssetReferenceValidator.java`
- `be/src/main/java/com/tellpal/v2/content/domain/Content.java`
- `be/src/main/java/com/tellpal/v2/content/domain/ContentLocalization.java`
- `be/src/main/java/com/tellpal/v2/content/domain/StoryPage.java`
- `be/src/main/java/com/tellpal/v2/content/domain/StoryPageLocalization.java`
- `be/src/main/java/com/tellpal/v2/content/domain/ContentPublicationPolicy.java`
- `be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminIntegrationTest.java`
- `be/src/test/java/com/tellpal/v2/content/web/admin/ContentPublicationAdminIntegrationTest.java`

### Covered Admin Endpoints

- `GET /api/admin/contents`
- `GET /api/admin/contents/{contentId}`
- `DELETE /api/admin/contents/{contentId}`
- `POST /api/admin/contents`
- `PUT /api/admin/contents/{contentId}`
- `POST /api/admin/contents/{contentId}/localizations/{languageCode}`
- `PUT /api/admin/contents/{contentId}/localizations/{languageCode}`
- `PATCH /api/admin/contents/{contentId}/localizations/{languageCode}/processing-status`
- `POST /api/admin/contents/{contentId}/localizations/{languageCode}/publish`
- `POST /api/admin/contents/{contentId}/localizations/{languageCode}/archive`
- `POST /api/admin/contents/{contentId}/story-pages`
- `PUT /api/admin/contents/{contentId}/story-pages/{pageNumber}`
- `DELETE /api/admin/contents/{contentId}/story-pages/{pageNumber}`
- `PUT /api/admin/contents/{contentId}/story-pages/{pageNumber}/localizations/{languageCode}`

### Required Fields

- Content create:
  - `type`
  - `externalKey`
  - `active`
- Content update:
  - `externalKey`
  - `active`
- Content localization create and update:
  - `title`
  - `status`
  - `processingStatus`
- Story page create:
  - `pageNumber`
- Story page update:
  - no body field is mandatory, but `illustrationMediaId` must be positive when present
- Story page localization upsert:
  - no body field is mandatory at request-validation level
  - `audioMediaId` must be positive when present

### Forbidden Combinations

- `STORY` content localization must not store `bodyText`.
- `STORY` content localization must not store a single `audioMediaId`.
- `AUDIO_STORY` and `MEDITATION` content localization require `bodyText`.
- `AUDIO_STORY`, `MEDITATION`, and `LULLABY` content localization require `audioMediaId`.
- `publishedAt` is mandatory whenever a content localization is created or updated with
  `status=PUBLISHED`.

### Type and State Rules

- `ageRange` must be non-negative when present.
- `durationMinutes` must be non-negative when present.
- `externalKey` must be unique across all content.
- Content deletion is a soft delete. `DELETE /api/admin/contents/{contentId}` only flips the
  aggregate to inactive and preserves editorial history.
- `GET /api/admin/contents` and `GET /api/admin/contents/{contentId}` still return inactive
  content.
- Content list and detail responses include localization snapshots. The detail response is
  currently the only admin read endpoint for content localizations.
- `visibleToMobile` is derived state, not an input field. It becomes `true` only when
  `status=PUBLISHED` and `processingStatus=COMPLETED`.
- `coverMediaId` must reference an asset with media type `IMAGE`.
- Content-level `audioMediaId` must reference an asset with media type `AUDIO`.
- Story page `illustrationMediaId` must reference an asset with media type `IMAGE`.
- Story page localization `audioMediaId` must reference an asset with media type `AUDIO`.
- Story pages can only be managed for `STORY` content. Trying to add, update, or remove story
  pages for other content types returns a content state conflict.
- Story page numbers must be positive and unique within one content aggregate.
- Story page localization upsert requires the parent content localization for the same language to
  already exist.

### Publication and Processing Preconditions

- Publish only operates on an existing content localization.
- Story publication requires at least one story page.
- Story publication requires every story page to have a localization for the target language.
- Story publication requires every localized story page to include `bodyText`.
- Story publication requires every localized story page to include `audioMediaId`.
- Archive preserves the previous `publishedAt` timestamp instead of clearing it.
- Processing status updates only replace the workflow flag on the localization. They do not bypass
  publication readiness rules.

### Expected ProblemDetail Error Codes

- `duplicate_external_key`
- `content_not_found`
- `content_localization_exists`
- `content_localization_not_found`
- `story_page_not_found`
- `asset_not_found`
- `asset_media_type_mismatch`
- `content_state_conflict`
- `validation_error`
- `invalid_body`
- `invalid_request`

### Local Sample Seed Notes

- The backend does not auto-seed CMS content.
- For local verification, prepare at least:
  - one active `STORY` item with one or more story pages
  - one active non-story item such as `MEDITATION` or `AUDIO_STORY`
  - one inactive content item to verify admin-only visibility
- Story seed data must place narrative text and per-page audio in `story_page_localizations`,
  not in the content localization body fields.
- Non-story content seed data should use a valid audio asset because `AUDIO_STORY`,
  `MEDITATION`, and `LULLABY` localizations require `audioMediaId`.
- `LOCAL_STUB` assets are valid for local sample content and processing tests.

### Frontend Form and Query Implications

- Content type selection must drive field visibility before submit:
  - `STORY` hides content-level body and single-audio inputs
  - `AUDIO_STORY` and `MEDITATION` require body text
  - `AUDIO_STORY`, `MEDITATION`, and `LULLABY` require audio asset selection
- Content list screens must not assume only active rows are returned.
- Content detail screens can render localization snapshots from `GET /api/admin/contents/{id}`,
  including `visibleToMobile`, without extra localization-read endpoints.
- Story-page editing UI must not allow localization entry before the parent content localization
  exists for the same language.
- Publish buttons for story content should be gated by story-page completeness in the current
  language, otherwise the backend returns `content_state_conflict`.
- Archive UI should preserve and display the last publish timestamp after archival.

### Open Gaps and Unverified Items

- There is no admin `GET` endpoint for story-page lists or story-page details. The current content
  read response exposes only `pageCount`, not the page collection.

## Contributors and Free Access

### Verified Sources

- `be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java`
- `be/src/main/java/com/tellpal/v2/content/web/admin/FreeAccessAdminController.java`
- `be/src/main/java/com/tellpal/v2/content/application/ContributorManagementService.java`
- `be/src/main/java/com/tellpal/v2/content/application/ContentFreeAccessService.java`
- `be/src/main/java/com/tellpal/v2/content/domain/Content.java`
- `be/src/main/java/com/tellpal/v2/content/domain/ContentContributor.java`
- `be/src/main/java/com/tellpal/v2/content/domain/Contributor.java`
- `be/src/test/java/com/tellpal/v2/content/web/admin/ContributorAdminIntegrationTest.java`
- `be/src/test/java/com/tellpal/v2/content/web/admin/FreeAccessAdminIntegrationTest.java`

### Covered Admin Endpoints

- `POST /api/admin/contributors`
- `GET /api/admin/contributors`
- `PUT /api/admin/contributors/{contributorId}`
- `POST /api/admin/contents/{contentId}/contributors`
- `POST /api/admin/free-access`
- `GET /api/admin/free-access`
- `DELETE /api/admin/free-access/{accessKey}/languages/{languageCode}/contents/{contentId}`

### Required Fields

- Contributor create and rename:
  - `displayName`
- Contributor assignment:
  - `contributorId`
  - `role`
  - `languageCode`
  - `sortOrder` defaults to required integer semantics and must be non-negative
- Free-access grant:
  - `accessKey`
  - `contentId`
  - `languageCode`

### Forbidden Combinations

- Duplicate contributor assignment for the same `contributorId + role + languageCode` is forbidden.
- Duplicate contributor `sortOrder` for the same `role + languageCode` inside one content item is
  forbidden.
- Duplicate free-access grant for the same `accessKey + contentId + languageCode` is forbidden.

### Type and State Rules

- Contributor list `limit` must be positive and is capped at `100`.
- Contributor display names are trimmed and must not be blank on create or rename.
- Contributor assignment requires both the content and contributor to already exist.
- Contributor `creditName` is optional and trimmed to `null` when blank.
- Duplicate contributor assignments and duplicate contributor sort orders currently surface as
  `400 invalid_request`, not as a module-specific conflict code.
- Free-access grant requires the target content to exist.
- Free-access grant requires the target localization for the requested language to exist.
- Admin free-access listing treats a missing or blank `accessKey` query parameter as `default`.
- Admin free-access listing does not fall back from an unknown non-blank key to `default`; it only
  returns entries stored under the requested key.
- Internal free-access resolution for delivery flows falls back from an unknown key to `default`,
  but that is not an admin endpoint behavior.

### Publication and Curation Preconditions

- Contributor assignment has no content-type restriction in the backend.
- Free-access grant does not require the target localization to be published. Existence is enough.
- Free-access revoke requires an exact stored match for `accessKey + contentId + languageCode`.

### Expected ProblemDetail Error Codes

- `contributor_not_found`
- `content_not_found`
- `content_localization_not_found`
- `content_free_access_exists`
- `content_free_access_not_found`
- `validation_error`
- `invalid_body`
- `invalid_request`

### Local Sample Seed Notes

- Create at least one contributor before testing assignment flows.
- Seed one content item before assignment tests because assignments are content-owned.
- Seed a matching content localization before free-access grant tests.
- Use both `default` and one non-default access key when testing admin list behavior so the UI can
  distinguish "empty explicit key" from "default key".

### Frontend Form and Query Implications

- Contributor picker flows must tolerate a recent-only list with no backend search endpoint.
- UI must prevent duplicate contributor role/language combinations and duplicate sort orders before
  submit when enough local state is available.
- CMS should not promise contributor deletion or content-assignment removal yet.
- Free-access screens should treat blank key input as the `default` set on admin list requests.
- Free-access screens must not assume that requesting an unknown non-default key will return the
  effective default set.

### Open Gaps and Unverified Items

- There is no contributor delete endpoint.
- There is no content-contributor unassign endpoint.
- There is no admin read endpoint that returns contributor assignments for a content item.

## Categories and Curation

### Verified Sources

- `be/src/main/java/com/tellpal/v2/category/web/admin/CategoryAdminController.java`
- `be/src/main/java/com/tellpal/v2/category/web/admin/CategoryCurationAdminController.java`
- `be/src/main/java/com/tellpal/v2/category/application/CategoryManagementService.java`
- `be/src/main/java/com/tellpal/v2/category/application/CategoryCurationService.java`
- `be/src/main/java/com/tellpal/v2/category/application/CategoryAssetReferenceValidator.java`
- `be/src/main/java/com/tellpal/v2/category/application/CategoryContentReferenceValidator.java`
- `be/src/main/java/com/tellpal/v2/category/domain/Category.java`
- `be/src/main/java/com/tellpal/v2/category/domain/CategoryLocalization.java`
- `be/src/main/java/com/tellpal/v2/category/domain/CategoryContent.java`
- `be/src/test/java/com/tellpal/v2/category/web/admin/CategoryAdminIntegrationTest.java`

### Covered Admin Endpoints

- `POST /api/admin/categories`
- `GET /api/admin/categories/{categoryId}`
- `PUT /api/admin/categories/{categoryId}`
- `POST /api/admin/categories/{categoryId}/localizations/{languageCode}`
- `PUT /api/admin/categories/{categoryId}/localizations/{languageCode}`
- `POST /api/admin/categories/{categoryId}/localizations/{languageCode}/contents`
- `PUT /api/admin/categories/{categoryId}/localizations/{languageCode}/contents/{contentId}`
- `DELETE /api/admin/categories/{categoryId}/localizations/{languageCode}/contents/{contentId}`

### Required Fields

- Category create and update:
  - `slug`
  - `type`
  - `premium`
  - `active`
- Category localization create and update:
  - `name`
  - `status`
- Curation add:
  - `contentId`
  - `displayOrder` must be non-negative
- Curation reorder:
  - `displayOrder` must be non-negative

### Forbidden Combinations

- Duplicate category slug is forbidden.
- Duplicate category localization for the same language is forbidden on create.
- Published category localization without `publishedAt` is forbidden.
- Duplicate curated `displayOrder` within the same category localization language is forbidden.
- Duplicate curated content link for the same `category + language + contentId` is forbidden.

### Type and State Rules

- Category slug is trimmed and must not be blank.
- Category localization `name` is trimmed and must not be blank.
- Category localization `description` is trimmed and blank values become `null`.
- `imageMediaId` must reference an asset with media type `IMAGE`.
- `imageMediaId` must be positive when present.
- Category curation is scoped by language. Each language keeps its own ordered curated set.
- Duplicate curated links and duplicate display orders currently surface as `400 invalid_request`,
  not as a module-specific conflict code.
- Category detail read currently returns only base metadata. It does not include localizations or
  curated contents.

### Publication and Curation Preconditions

- Category curation requires the category localization to exist.
- Category curation requires the category localization to be `PUBLISHED`.
- Category curation requires the referenced content to exist.
- Category curation requires the referenced content to be active.
- Category curation requires the referenced content localization for the same language to be
  published.
- Category curation does not currently require the content localization processing status to be
  `COMPLETED`. The existing integration test publishes story content with `processingStatus=PENDING`
  and still allows curation.
- Removing curated content requires an existing stored link.

### Expected ProblemDetail Error Codes

- `duplicate_category_slug`
- `category_not_found`
- `category_localization_exists`
- `category_localization_not_found`
- `category_localization_not_published`
- `category_content_not_found`
- `asset_not_found`
- `asset_media_type_mismatch`
- `content_not_found`
- `content_inactive`
- `content_localization_not_published`
- `validation_error`
- `invalid_body`
- `invalid_request`

### Local Sample Seed Notes

- Seed at least one category with a published localization before testing curation.
- Seed at least one active content item with a published localization in the same language before
  adding it to category curation.
- If local category tests use story content, a published content localization is enough for curation.
  Story processing completion is not currently required.

### Frontend Form and Query Implications

- Category editors must require `publishedAt` whenever the UI lets a user set
  `status=PUBLISHED`.
- Curation UI should disable add and reorder actions until the selected category localization is
  published.
- Curation UIs should avoid duplicate display orders before submit.
- Category detail screens cannot rely on a single backend read to hydrate localizations and curated
  content.

### Open Gaps and Unverified Items

- There is no admin category list endpoint.
- There is no admin category delete endpoint.
- There is no admin read endpoint that returns category localizations or curated content collections.

## Assets and Media Processing

### Verified Sources

- `be/src/main/java/com/tellpal/v2/asset/web/admin/AssetAdminController.java`
- `be/src/main/java/com/tellpal/v2/asset/web/admin/AssetProcessingAdminController.java`
- `be/src/main/java/com/tellpal/v2/asset/web/admin/AssetAdminExceptionHandler.java`
- `be/src/main/java/com/tellpal/v2/asset/application/AssetRegistryService.java`
- `be/src/main/java/com/tellpal/v2/asset/application/AssetProcessingService.java`
- `be/src/main/java/com/tellpal/v2/asset/domain/MediaAsset.java`
- `be/src/main/java/com/tellpal/v2/asset/domain/AssetProcessing.java`
- `be/src/main/java/com/tellpal/v2/asset/domain/ProcessingContentType.java`
- `be/src/test/java/com/tellpal/v2/asset/web/admin/AssetAdminIntegrationTest.java`
- `be/src/test/java/com/tellpal/v2/asset/web/admin/AssetProcessingAdminIntegrationTest.java`

### Covered Admin Endpoints

- `POST /api/admin/media`
- `GET /api/admin/media`
- `GET /api/admin/media/{assetId}`
- `PUT /api/admin/media/{assetId}/metadata`
- `POST /api/admin/media/{assetId}/download-url-cache/refresh`
- `POST /api/admin/media-processing`
- `GET /api/admin/media-processing`
- `GET /api/admin/media-processing/{contentId}/localizations/{languageCode}`
- `POST /api/admin/media-processing/{contentId}/localizations/{languageCode}/retry`

### Required Fields

- Asset registration:
  - `provider`
  - `objectPath`
  - `kind`
- Asset metadata update:
  - all fields optional, but validated when present
- Processing schedule:
  - `contentId`
  - `languageCode`
  - `contentType`
  - `externalKey`
- Processing retry:
  - `contentType`
  - `externalKey`

### Forbidden Combinations

- Duplicate asset registration for the same `provider + objectPath` is forbidden.
- Asset `byteSize` must not be negative.
- Asset checksum must be a lowercase SHA-256 hex string when present.
- Story processing requires `pageCount` and forbids omitting it.
- Non-story processing forbids `pageCount`.
- Non-story processing requires `audioSourceAssetId`.

### Type and State Rules

- Asset recent list `limit` must be positive and is capped at `100`.
- Asset registration trims `objectPath` and metadata text fields.
- Asset kind determines stored media type. CMS should use returned `mediaType` and `kind` instead
  of inferring media type from filenames.
- Asset metadata update replaces mutable metadata values. Blank text normalizes to `null`.
- Refreshing the cached download URL replaces the stored cache window. The backend requires the
  refreshed expiry to be after the cache timestamp.
- Processing recent list `limit` must be positive and is capped at `100`.
- Scheduling processing for a localization with no existing processing record creates a `PENDING`
  record.
- Scheduling processing for an existing `PENDING` record refreshes the context and keeps the record
  pending instead of returning a conflict.
- Scheduling processing for an existing `PROCESSING` record returns a conflict.
- Scheduling processing for an existing `COMPLETED` record returns a conflict.
- Scheduling processing for an existing `FAILED` record returns a conflict and requires the retry
  endpoint instead.
- Retry only works for `FAILED` processing records.
- Retry against `PENDING`, `PROCESSING`, or `COMPLETED` records returns a conflict.
- `GET /api/admin/media-processing/{contentId}/localizations/{languageCode}` returns `404` until a
  processing row exists for that localization.
- A new processing row requires the target content localization to exist because persistence is
  keyed to that localization.

### Publication and Processing Preconditions

- Cover source asset is optional at the processing aggregate level, but it must be positive when
  present.
- Audio source asset is optional for `STORY` processing and required for non-story processing.
- Story processing may carry a non-negative `pageCount`.
- The admin processing API does not expose worker-only lifecycle transitions such as `start`,
  `complete`, `fail`, or lease recovery.

### Expected ProblemDetail Error Codes

- `media_asset_exists`
- `media_asset_not_found`
- `asset_processing_not_found`
- `asset_processing_localization_not_found`
- `asset_processing_conflict`
- `validation_error`
- `invalid_body`
- `invalid_request`

### Local Sample Seed Notes

- Register local sample assets through `POST /api/admin/media` or through the asset API used in
  integration tests.
- Use `LOCAL_STUB` provider for local development unless the environment explicitly configures
  another storage provider.
- Provide lowercase SHA-256 checksums in local sample data when checksum coverage matters.
- Media-processing tests need both content/localization records and source assets.

### Frontend Form and Query Implications

- Asset library UI should expect recent-only listings with no search or pagination contract.
- Asset register and metadata forms should validate checksum shape and non-negative byte size before
  submit.
- Asset pickers should use backend `mediaType` for filtering instead of only relying on `kind`
  labels.
- Processing consoles should treat "not found" as "job not scheduled yet", not as a generic fatal
  error.
- Processing schedule UI should distinguish:
  - new or pending localization: schedule is allowed
  - failed localization: retry is required
  - running or completed localization: scheduling and retry both conflict
- Story processing UI must provide `pageCount`.
- Non-story processing UI must never send `pageCount`.

### Open Gaps and Unverified Items

- There is no admin asset delete endpoint.
- There is no admin search endpoint for assets or processing jobs beyond recent-list retrieval.
