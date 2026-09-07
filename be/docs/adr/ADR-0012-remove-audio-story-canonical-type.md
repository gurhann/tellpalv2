# ADR-0012: Remove AUDIO_STORY as a Canonical Content Type

## Status

Accepted

## Context

The audio version of a story has the same editorial identity and content as the story itself.
Persisting it as a second `Content` row and category family duplicates identity, curation, and
lifecycle state. The audio experience is localization-scoped because one language may have a
completed narration while another remains reading-only.

## Decision

Canonical content, category, and asset-processing types are limited to:

- `STORY`
- `MEDITATION`
- `LULLABY`

`AUDIO_STORY` is not accepted by canonical create/update/filter contracts and is not exposed as a
CMS content or category option. A story's audio experience is derived from its localization
narration and may be represented as `experienceType: AUDIO_STORY` by a discovery projection or a
legacy response surface. That presentation label does not create a new content identity or
category type.

The V28 Flyway migration performs a read-only preflight for persisted `AUDIO_STORY` rows in
`contents`, `categories`, and `asset_processing`. It fails with the affected record class before
changing constraints. When the preflight is clean, the three canonical checks are narrowed while
scoped processing, STORY narration, and existing MEDITATION/LULLABY rules remain intact.

Legacy independent audio-story records are not imported, deleted, or silently converted by this
decision. A future import requires its own mapping, rollback, and publication-impact plan.

## Consequences

- New CMS and admin clients cannot create canonical `AUDIO_STORY` records.
- Existing canonical queries and category curation use only the three remaining content types.
- A localization can still expose an audio-story discovery experience without duplicating `contentId`.
- Databases containing legacy `AUDIO_STORY` rows require explicit cleanup or a separately approved
  import/migration before V28 can complete.

## Alternatives Considered

- Keeping `AUDIO_STORY` as a hidden canonical alias was rejected because it would allow new
  duplicate identities and keep database constraints ambiguous.
- Automatically converting legacy rows during V28 was rejected because identity, category,
  localization, asset, and publication mappings require editorial decisions.
- Removing the audio presentation label was rejected because existing UI/UX and compatibility
  surfaces may need to distinguish reading and audio experiences for the same story.

## Related Files or Modules

- `be/src/main/resources/db/migration/V28__remove_audio_story_canonical_type.sql`
- `be/src/main/java/com/tellpal/v2/content/domain/ContentType.java`
- `be/src/main/java/com/tellpal/v2/category/domain/CategoryType.java`
- `be/src/main/java/com/tellpal/v2/asset/api/AssetProcessingContentType.java`
- `cms/src/features/contents/`
- `cms/src/features/categories/`
- `_bmad-output/specs/spec-story-audio-experience/audio-story-contract.md`

## Supersedes / Superseded By

- Supersedes `ADR-0007-category-type-aligns-with-content-type.md` for the canonical type set.
- None
