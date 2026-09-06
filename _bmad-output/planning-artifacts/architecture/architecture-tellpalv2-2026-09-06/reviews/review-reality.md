# Brownfield Reality Review

## Verdict

**Revision required before implementation.** The proposed module ownership is compatible with the
existing Modulith boundaries, but the spine currently leaves two compatibility-critical state
transitions implicit. As written, an implementer can make an audio-processing failure hide the
ordinary reading experience, or can attempt to store shared lullaby processing in a schema and API
that require a localization. It also silently resolves the instrument-label ownership that the
driving specification explicitly leaves open.

## Verified alignment

- The `content` and `asset` module split in AD-1 matches `architecture.md`, ADR-0001 and
  ADR-0002: `content` already validates media through `asset.api`, and `asset` already provides
  processing and delivery-bundle APIs.
- The current domain is in the pre-change shape described by the spine: `ContentType`,
  `ContentApiType`, `CategoryType`, `ProcessingContentType`, the V5 `contents` check constraint,
  V13 `asset_processing` check constraint, V17 category check constraint, and CMS schemas all
  still expose `AUDIO_STORY`.
- The current contributor model can represent global musicians: `content_contributors.language_code`
  is nullable and `MUSICIAN` is an existing role. AD-3 needs to bind that lullaby musician
  assignments use `language_code = NULL`; it is not true automatically because the same model also
  permits language-scoped assignments.
- `contents.textless_cover_media_id` already exists (V19), which is a potential existing home for
  the common lullaby cover only if the implementation explicitly reserves it for the proposed
  purpose. The spine presently introduces a separate playback cover without deciding whether this
  field is reused, migrated, or forbidden for `LULLABY`.

## Findings

### P1 — The reading and narration readiness states must be separate

**Spine:** AD-2, AD-5 and AD-6.

The story spec requires a failed narration to remove only the `AUDIO_STORY` projection while the
`READING` projection remains available. The brownfield model has exactly one localization
`processing_status`; `ContentLocalization.isVisibleToMobile()` requires it to be `COMPLETED`.
`AssetProcessingStatusListener` maps every asset-processing event directly to that same status, and
the public query service filters all content through `isVisibleToMobile()`.

AD-5 says narration readiness is localization-scoped, but it does not name a distinct narration
state/read model or explicitly prevent reuse of `ContentLocalization.processingStatus`. Reusing it
would violate the driving spec: a failed or pending narration would hide the normal story.

**Required spine correction:** add an explicit story-narration child/readiness owner (for example,
`StoryNarration` owned by `ContentLocalization`) with its own source audio, duration and processing
state. Define reading visibility from the existing localization readiness and audio experience
visibility from the narration readiness. Add it to the structural seed and specify which processing
status-change event updates each state.

**Evidence:**

- `be/src/main/java/com/tellpal/v2/content/domain/ContentLocalization.java`:
  `isVisibleToMobile()` combines `PUBLISHED` with the single `processingStatus`.
- `be/src/main/java/com/tellpal/v2/content/application/AssetProcessingStatusListener.java` writes
  every processing event to the localization status.
- `be/src/main/java/com/tellpal/v2/content/application/query/PublicContentQueryService.java` uses
  that visibility predicate for both list and detail reads.
- `be/src/main/java/com/tellpal/v2/content/domain/Content.java` currently forbids a single audio
  media reference on a `STORY` localization, so an intentional new child/field and validation path
  are required rather than a CMS-only change.

### P1 — Content-scoped lullaby processing is not representable by the existing asset contract

**Spine:** AD-3, AD-5 and AD-6.

The existing `asset_processing` table makes `language_code` non-null, has a unique key on
`(content_id, language_code)`, and has a foreign key to `(content_id, language_code)` in
`content_localizations`. Its API commands, events, repository lookup and bundle API all require a
`LanguageCode`; generated object paths also always include it. Therefore a `CONTENT` scope cannot
be implemented by merely adding a scope enum or passing no language as AD-5 states.

The spine must choose one concrete migration-compatible shape: a separate content-scoped processing
aggregate/table and content-level delivery API, or a carefully redesigned processing key that can
support both scopes without weakening the localization foreign key. It must also bind the public
reader to use `findForContent(contentId)` (or equivalent) for lullabies. Without this, a shared
lullaby either has no valid processing row or is processed once per localization—the exact outcome
AD-5 is meant to prevent.

**Evidence:**

- `be/src/main/resources/db/migration/V12__create_asset_processing_tables.sql`: non-null language,
  `(content_id, language_code)` uniqueness and composite localization FK.
- `be/src/main/java/com/tellpal/v2/asset/api/AssetProcessingCommands.java` and
  `AssetProcessingApi`: every lifecycle command is localization-keyed.
- `be/src/main/java/com/tellpal/v2/asset/api/ContentAssetBundleApi.java` and
  `asset/application/ContentAssetBundleService.java`: only `findForLocalization` exists.
- `be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/AssetProcessingPathBuilder.java`:
  all roots require and embed a language code.

### P2 — AD-4 closes an explicitly open product/API decision without recording it

**Spine:** AD-4 and the public-contract convention table.

The lullaby spec leaves backend-localized instrument labels versus client-side translation as an
open question. AD-4 instead requires `InstrumentCatalog` to have localized display labels, and the
public contract requires a locale-resolved display name. That is a valid option, but it is neither
an adopted brownfield convention nor a settled spec constraint.

**Required spine correction:** either mark this as an `[ASSUMPTION]`/open question with a revisit
condition, or explicitly obtain/adopt the decision and add the resulting catalog-localization
ownership and API contract to the structural seed. Do not present it as `[ADOPTED]` until then.

**Evidence:** `spec-lullaby-shared-audio-and-instruments/SPEC.md` lists this exact question under
`Open Questions`; `lullaby-data-and-api-contract.md` only requires a stable ID and managed display
name.

### P2 — AD-8's removal inventory is incomplete

**Spine:** AD-8.

The fail-fast guard correctly protects persisted `contents` and category rows, but `AUDIO_STORY`
also persists in `asset_processing.content_type` and is part of the current asset command/domain
enums and path context. Removing only the canonical content/category support leaves migration and
runtime incompatibilities behind. A pending/failed processing row can outlive the content migration
and still block a narrowed V13 check constraint.

**Required spine correction:** make the preflight enumerate and fail on (or separately clean under
an explicit, non-import operation) `asset_processing` rows with `content_type = 'AUDIO_STORY'`,
then remove/update all four type enum families and processing-path assumptions in the same rollout.
This is not legacy content import; it is schema integrity for the records already owned by the
current runtime.

**Evidence:** V13 adds `AUDIO_STORY` to `asset_processing.content_type`; both
`asset/domain/ProcessingContentType.java` and `asset/api/AssetProcessingContentType.java` still
contain it, as does `AssetProcessingCommands.requiresSingleAudioAsset`.

### P3 — The shared-cover ownership needs a migration decision

**Spine:** AD-3 structural seed.

V19 already provides `contents.textless_cover_media_id`, while the spine introduces a cover ID in a
new `LullabyPlayback` child. The two are both plausible representations of the common textless
lullaby cover. Leaving both valid for `LULLABY` would create duplicate sources of truth and unclear
read precedence.

**Required spine correction:** bind one representation: reuse and type-restrict
`textless_cover_media_id`, migrate it into `LullabyPlayback`, or state that it is unrelated and
cannot be used by lullabies. Add the selected source to the schema/migration plan.

**Evidence:** `be/src/main/resources/db/migration/V19__add_content_textless_cover.sql` and
`content/domain/Content.java` expose the existing content-level cover reference.

## No-code-change note

This review did not modify the spine or production code.
