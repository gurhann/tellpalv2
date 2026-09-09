# ADR-0010: Content Cover Ownership Is Split by Experience

## Status

Accepted

## Context

STORY content has a textless source cover used by illustrators when producing localized,
title-bearing reading covers. The audio-story experience needs a separate textless listening cover.
MEDITATION uses one content-level listening cover across languages. Legacy LULLABY records can
hold both a static list cover and an animated playback/detail cover, so one field cannot represent
both roles without losing their meaning.

## Decision

Keep `contents.textless_cover_media_id` as the optional STORY source-cover reference. Add a
separate optional `contents.listening_cover_media_id` for STORY narration, MEDITATION, and LULLABY.
Both are positive IMAGE references owned by the content module; localized `cover_media_id` remains
language-specific. The listening cover is not copied into localization rows and does not affect
publication or processing readiness.

For LULLABY only, add optional `contents.listing_cover_media_id` as a separate positive IMAGE
reference for the static list/card cover. `listening_cover_media_id` remains the shared playback/
detail cover and can reference a GIF registered as an IMAGE asset. Both LULLABY references are
content-scoped, language-independent, and are not part of `LullabyPlayback`.

## Consequences

- Admin and CMS contracts must expose the applicable content-level references separately.
- Database constraints and domain validation prevent unsupported content types and identical source/
  listening references.
- Future audio playback and public projections can consume the listening cover without changing
  translation source artwork.
- This decision does not add or change public/mobile delivery contracts, animation processing, or
  mobile rendering behavior.

## Alternatives Considered

- Reusing `textless_cover_media_id` for audio surfaces was rejected because it conflates translation
  source artwork with runtime listening presentation.
- Storing the listening cover on each localization was rejected because the asset is not language-specific.
- Reusing `listening_cover_media_id` for the LULLABY list/card image was rejected because it would
  discard the independently managed animated playback/detail asset.
