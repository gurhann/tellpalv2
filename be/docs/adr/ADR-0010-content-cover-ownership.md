# ADR-0010: Content Cover Ownership Is Split by Experience

## Status

Accepted

## Context

STORY content has a textless source cover used by illustrators when producing localized,
title-bearing reading covers. The audio-story experience needs a separate textless listening cover.
MEDITATION and LULLABY also use one content-level listening cover across languages. Reusing one
field for both purposes makes source artwork and runtime presentation ambiguous.

## Decision

Keep `contents.textless_cover_media_id` as the optional STORY source-cover reference. Add a
separate optional `contents.listening_cover_media_id` for STORY narration, MEDITATION, and LULLABY.
Both are positive IMAGE references owned by the content module; localized `cover_media_id` remains
language-specific. The listening cover is not copied into localization rows and does not affect
publication or processing readiness.

## Consequences

- Admin and CMS contracts must expose the two content-level references separately.
- Database constraints and domain validation prevent unsupported content types and identical source/
  listening references.
- Future audio playback and public projections can consume the listening cover without changing
  translation source artwork.

## Alternatives Considered

- Reusing `textless_cover_media_id` for audio surfaces was rejected because it conflates translation
  source artwork with runtime listening presentation.
- Storing the listening cover on each localization was rejected because the asset is not language-specific.
