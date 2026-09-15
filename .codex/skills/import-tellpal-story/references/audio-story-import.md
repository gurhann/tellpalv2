# Firebase Audio-Story Import

The audio-story importer attaches legacy Firebase audio assets to existing localized `STORY`
records. It does not create a new content type or a new localization. The CSV source is preserved;
the importer writes `<csv-stem>.import.csv` beside it.

For each row in `audio_stories.csv`:

- `language` and `name` select one exact normalized STORY localization.
- `id` selects the root `{id}.zip` Firebase object and is never sent to the Admin API.
- `image_url` selects `cover_images/{image_url}` and becomes the content-level
  `listeningCoverMediaId` when that shared cover is missing.
- The ZIP must contain exactly one non-empty valid MP3. Its duration is rounded up to whole minutes.
- The MP3 is attached as localization `narration: { audioMediaId, durationMinutes }`.
- Existing publication, processing, body, and reading-cover fields are preserved.

## Read-only preflight

Use the bundled Python runtime when available:

```powershell
$python = '<bundled-python>'
& $python -B '<skill-dir>\scripts\inspect_audio_stories.py' `
  'C:\github\tellpalv2\cms\yuklenecek_hikayeler\audio_stories\audio_stories.csv'
```

The command reads Firebase Storage, validates the CSV and media, reports duplicate and invalid rows,
and writes the sidecar status CSV. A non-zero result means at least one row needs attention; valid
rows remain visible in the preview.

Missing objects, invalid ZIP/MP3 data, and exact target/content conflicts are recorded per row so
later rows can continue. Authentication, network-wide, and server-wide storage failures stop the
preflight instead of being misreported as individual missing assets.

Use `--storage-bucket` for another bucket, `--storage-base-url` for another GCS-compatible endpoint,
`--audio-prefix` when ZIPs are not at the root, and `--service-account-json` for private objects.
The service-account file is read only for a short-lived storage read token and is never included in
the run report.

## Live import

Set `TELLPAL_API_BASE_URL` and `TELLPAL_ADMIN_USERNAME`. The importer asks for the password through
the interactive terminal, performs authenticated read-only matching, prints the complete plan, and
requires the standalone word `import` before any Admin API mutation:

```powershell
& $python -B '<skill-dir>\scripts\import_audio_stories.py' `
  'C:\github\tellpalv2\cms\yuklenecek_hikayeler\audio_stories\audio_stories.csv'
```

Status values are `PENDING`, `SUCCESS`, `ALREADY_IMPORTED`, `SKIPPED_DUPLICATE`, `ERROR`, and
`UNKNOWN`. Deterministic row errors continue to later rows. A timeout or connection reset after a
mutation marks the row `UNKNOWN` and stops the run; do not retry it automatically. Inspect the run
report under `%LOCALAPPDATA%\TellPal\audio-story-import-agent\runs\` before taking manual action.

The importer never publishes, overwrites existing narration or conflicting shared covers, writes the
source CSV, or performs cleanup/resume of partial mutations. If a localization already has a
compatible narration but lacks the shared listening cover, the importer attaches only the missing
cover and reuses the narration without sending another narration update.
