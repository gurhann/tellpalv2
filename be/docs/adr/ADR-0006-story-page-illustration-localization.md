# ADR-0006: Story Page Illustrations Belong to Localizations

## Status

Accepted

## Context

Story-page illustrations were originally stored on `story_pages` as a single page-level asset
reference. That model breaks for multilingual story content because text can be baked into the
image and therefore vary by language. The CMS also needs publication checks to validate
illustration readiness per locale, not only per page.

## Decision

Story-page illustration ownership moves from `story_pages` to `story_page_localizations`.

The project adopts these defaults:

- runtime behavior is localization-only; there is no page-level illustration fallback
- migration copies legacy page-level illustration values into existing localization rows
- story-page localization create and update require `illustrationMediaId`
- story-page localization `illustrationMediaId` must reference an `IMAGE` asset
- public delivery resolves the selected language illustration from `story_page_localizations`

Story-page create and metadata update flows now manage only page structure. Locale-specific media
selection belongs entirely to the localization editor.

## Consequences

- Different locales can serve different story-page illustrations for the same page number.
- Story publication readiness can validate illustration presence in the target language.
- CMS story-page tables and editors must reason about illustration coverage per locale.
- Legacy page-level illustration storage is removed after migration and contract updates.

## Alternatives Considered

- Keeping a single page-level illustration was rejected because it cannot represent localized image
  text safely.
- Adding optional locale overrides on top of a page-level default was rejected because fallback
  behavior would make readiness checks and CMS UX ambiguous.
- Storing illustration localization in a separate asset-link table was rejected because the
  existing `story_page_localizations` aggregate part already owns language-scoped payload data.

## Related Files or Modules

- `be/src/main/java/com/tellpal/v2/content/domain/StoryPage.java`
- `be/src/main/java/com/tellpal/v2/content/domain/StoryPageLocalization.java`
- `be/src/main/java/com/tellpal/v2/content/application/StoryPageManagementService.java`
- `be/src/main/java/com/tellpal/v2/content/application/query/PublicContentQueryService.java`
- `be/src/main/resources/db/migration/V15__localize_story_page_illustrations.sql`
- `be/src/main/resources/db/migration/V16__drop_story_page_level_illustration.sql`
- `cms/src/app/routes/story-pages.tsx`

## Supersedes / Superseded By

- None
