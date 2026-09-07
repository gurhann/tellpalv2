# Cover ownership matrix

| Content type / experience | Source cover | Listening-experience cover | Localization cover | Rule |
| --- | --- | --- | --- | --- |
| `STORY` reading experience | `Content.textlessCoverMediaId`: textless version of the ordinary story cover | Not used by reading cover | `ContentLocalization.coverMediaId` | The localized cover can contain the title; illustrators use the source cover to produce it. |
| `STORY` audio experience | Kept separate; never reused as listening cover | New content-level listening-cover reference | No replacement of the localized reading cover | The listening cover is shared regardless of narration language. |
| `MEDITATION` | Not applicable | New content-level listening-cover reference | No listening-cover field in localization | One textless listening cover is shared; existing localized audio/text behavior is unchanged. |
| `LULLABY` | Not applicable | New content-level listening-cover reference | No listening-cover field in localization | One textless listening cover is shared; common playback remains deferred to Story 1.4. |

## Validation and read rules

- A non-null source or listening-cover ID is positive and references an `IMAGE` asset through the asset module's public API.
- The listening-cover reference is returned by the content/admin projection used by every localization; it is distinct from both the source cover and localization cover.
- Setting, replacing, or clearing a listening cover is content-scoped. It does not write the source cover, any localization row, story narration, story-page audio, or processing state.
- Asset URLs and derived cover outputs are resolved when read or processed; they are never duplicated into content or localization persistence.

## Acceptance matrix

| Scenario | Expected result |
| --- | --- |
| `STORY` has `tr` and `en` localizations; listening cover changes | Both reads expose the new listening-cover asset ID; their source and localized covers are unchanged. |
| `MEDITATION` or `LULLABY` receives an audio/nonexistent asset ID | The write is rejected and the existing shared cover remains unchanged. |
| Listening cover is cleared | The content stores no listening cover; source/localized covers and publication state remain unchanged. |
| Editor opens a `STORY` localization | The language-specific title-bearing cover remains editable there; neither source nor listening cover is duplicated into that form. |
| Existing content has no listening cover | It remains valid and no processing job is created. |
