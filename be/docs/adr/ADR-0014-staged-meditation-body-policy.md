# ADR-0014: Staged MEDITATION Body Text Is Explicitly Incomplete

## Status

Accepted on 2026-09-13.

## Context

The legacy meditation import source can provide valid localized titles, descriptions, covers, and
audio before the translated meditation body text is available. The storage schema already permits
an optional localization body, but treating a missing body as complete would make editorial
readiness and mobile publication ambiguous.

## Decision

- A `MEDITATION` localization may have a null or blank-normalized body only while its status is
  `DRAFT` and its processing status is `PENDING`.
- The importer must opt into this state with `--allow-missing-body --no-publish`. It must continue
  validating audio and remote image assets, report every missing group/language, and never publish
  or mark staged processing as completed.
- Direct localization create/update requests with a missing body and a non-`PENDING` processing
  status are rejected. A non-blank body may later be supplied through the existing localization
  update endpoint without changing caller/worker ownership of status fields.
- Publication and archive transitions require non-blank MEDITATION body text. A bodyless staged
  localization therefore cannot become public or be archived through an admin operation.
- The database-backed admin content registry reports bodyless MEDITATION localizations as
  `ACTION_REQUIRED` and exposes the `BODY_TEXT_MISSING` blocker even when processing is complete.
  STORY and LULLABY readiness rules remain unchanged.

No migration is required because the existing nullable localization body column represents this
temporary editorial state.

## Consequences

- Import operators can safely stage available meditation assets while body translations are pending.
- CMS operators receive an explicit actionable registry state instead of a misleading ready row.
- Publication and archive are protected at both the localization validation and domain policy
  boundaries.
- Completing a body remains an explicit editorial update; no placeholder text is fabricated.

## Alternatives Considered

- Reusing the description or a generated summary as body text was rejected because it hides an
  incomplete translation and could make the localization publishable accidentally.
- Marking bodyless rows `COMPLETED` was rejected because processing completion does not represent
  editorial body completion.
- Adding a separate database status or migration was rejected because the existing nullable body,
  DRAFT/PENDING states, and registry projection are sufficient.

## Related Files or Modules

- `be/src/main/java/com/tellpal/v2/content/domain/Content.java`
- `be/src/main/java/com/tellpal/v2/content/domain/ContentPublicationPolicy.java`
- `be/src/main/java/com/tellpal/v2/content/application/AdminContentQueryService.java`
- `be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/JdbcContentRegistryReadRepository.java`
- `.codex/skills/import-tellpal-story/scripts/meditation_import_workflow.py`

## Supersedes / Superseded By

- None.
