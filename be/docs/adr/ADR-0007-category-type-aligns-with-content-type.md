# ADR-0007: Category Type Aligns with Curated Content Type

## Status

Accepted

## Context

The first category model exposed `CONTENT` and `PARENT_GUIDANCE` as category types. That split
does not match the editorial workflows used by the CMS because story, audio story, meditation, and
lullaby content each need their own curated category families. The old model also allowed one
category to mix incompatible content types because curation validated only language publication and
active state.

## Decision

Category type now aligns directly with content type.

The category aggregate and its external contracts use these values:

- `STORY`
- `AUDIO_STORY`
- `MEDITATION`
- `LULLABY`

The project adopts these defaults:

- category type represents curation compatibility, not a separate category family axis
- category curation accepts only content whose `content.type` exactly matches `category.type`
- legacy `CONTENT` categories migrate only when existing curated content resolves to one distinct
  content type
- legacy `PARENT_GUIDANCE` categories are rollout blockers and require manual cleanup before the
  migration can succeed
- empty or mixed-type legacy `CONTENT` categories also block the migration

No backward-compatible alias is kept in admin or mobile APIs.

## Consequences

- CMS category forms and read screens expose only content-aligned types.
- Mobile category discovery uses the same type filter values as public content discovery.
- Category curation can reject type-mismatched content with a dedicated
  `category_content_type_mismatch` problem code.
- Rollout safety moves into Flyway: ambiguous legacy data fails fast instead of being guessed.

## Alternatives Considered

- Keeping `CONTENT` and adding extra validation metadata was rejected because it would still hide
  the real editorial meaning of category type.
- Keeping `PARENT_GUIDANCE` in the same aggregate was rejected because that concept is not part of
  the current v2 scope and would preserve an invalid migration path.
- Auto-mapping empty legacy `CONTENT` categories to a default type was rejected because it would
  silently misclassify editorial data.

## Related Files or Modules

- `be/src/main/java/com/tellpal/v2/category/domain/CategoryType.java`
- `be/src/main/java/com/tellpal/v2/category/application/CategoryContentReferenceValidator.java`
- `be/src/main/java/com/tellpal/v2/category/application/CategoryCurationService.java`
- `be/src/main/resources/db/migration/V17__align_category_types_with_content_types.sql`
- `be/src/test/java/com/tellpal/v2/category/migration/CategoryTypeMigrationIntegrationTest.java`
- `cms/src/features/categories/`

## Supersedes / Superseded By

- None
