# Legacy Lullaby Import

The lullaby importer consumes the legacy CSV directly and maps each unique
`(image_url, summary_image_url)` pair to one canonical `LULLABY` content aggregate.
The Turkish row supplies the stable external key (`lullaby.<turkish-title-slug>`).

For each group:

- `image_url` is downloaded from `cover_images/` and attached as `listingCoverMediaId`.
- `summary_image_url` is downloaded from `cover_images/` and attached as `listeningCoverMediaId`.
- Every `{id}.zip` object is downloaded from the bucket root (or `--audio-prefix`), checked for
  exactly one MP3, and compared across language rows. Different audio content blocks the run because
  the new model has one shared playback source.
- The MP3 duration is detected from MPEG frames and rounded up to whole minutes. Use
  `--duration-minutes` when a source file has no readable MPEG frames.
- Supported instrument names from `summary` are mapped to the managed catalog. Unknown names are
  warned and omitted. The `Music`/`Müzik` value is assigned as one global `MUSICIAN` contributor.
- The legacy free-text `summary` itself is not sent because LULLABY localizations support title and
  publication state only.

Run the read-only storage preflight first:

```powershell
$python = '<bundled-python>'
& $python -B '<skill-dir>\scripts\inspect_lullabies.py' `
  'C:\github\tellpalv2\cms\yuklenecek_hikayeler\lullabies\lullabies.csv'
```

The live importer requires `TELLPAL_API_BASE_URL` and `TELLPAL_ADMIN_USERNAME`. It asks for the
password through the interactive terminal, performs remote preflight, prints the complete plan, and
requires the standalone word `import` before any content or media write:

```powershell
& $python -B '<skill-dir>\scripts\import_lullabies.py' `
  'C:\github\tellpalv2\cms\yuklenecek_hikayeler\lullabies\lullabies.csv'
```

Use `--no-publish` to leave all localizations in `DRAFT`, `--inactive` to create inactive content,
`--storage-bucket` to select another bucket, and `--storage-base-url` when the bucket is exposed
through another public storage endpoint. The importer does not resume, update, delete, or roll back
partial writes. Run reports are stored under
`%LOCALAPPDATA%\TellPal\lullaby-import-agent\runs`; staged source objects are temporary and
removed when the command exits. Reports are diagnostic only.
