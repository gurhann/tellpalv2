# Legacy Meditation Import

The meditation importer consumes the legacy `meditations.csv` directly and groups rows by the
normalized `image_url` filename stem. The `_kapak` and `cover` suffixes are removed before the
stable stem is generated, so translated rows become one `meditation.<stem>` MEDITATION content.
The Turkish row is mandatory for every group. Legacy IDs select Firebase Storage objects only and
are never sent as API external keys.

For each group:

- `image_url` is downloaded from `cover_images/` and attached once as the content-level
  `listeningCoverMediaId`.
- `summary_image_url` is diagnostic only. A different, empty, or legacy URL value is retained as a
  warning and is not downloaded or attached.
- Every root `{id}.zip` object is downloaded, checked for exactly one MP3, validated for an MPEG
  frame, and measured in whole minutes.
- CSV `summary` is sent as the localization `description`.
- A non-empty body text source is required for every group/language before live import. Sources may
  be UTF-8 `.txt`/`.md` files or DOCX files. Use a body directory containing
  `<group>/<language>.<txt|md|docx>` files, flat `<group>.<language>.<txt|md|docx>` files, or pass
  explicit mappings with `--body-source GROUP/LANGUAGE=PATH`. Unrelated files in the body directory
  are ignored.

Run the read-only storage preflight first:

```powershell
$python = '<bundled-python>'
& $python -B '<skill-dir>\scripts\inspect_meditations.py' `
  'C:\github\tellpalv2\cms\yuklenecek_hikayeler\meditations\meditations.csv'
```

The live importer requires `TELLPAL_API_BASE_URL` and `TELLPAL_ADMIN_USERNAME`. It asks for the
password through an interactive terminal, performs remote preflight, prints the complete plan, and
requires the standalone word `import` before any content or media write:

```powershell
& $python -B '<skill-dir>\scripts\import_meditations.py' `
  'C:\github\tellpalv2\cms\yuklenecek_hikayeler\meditations\meditations.csv' `
  --body-directory 'C:\path\to\meditation-bodies'
```

Use `--no-publish` to leave all localizations in `DRAFT`, `--inactive` to create inactive content,
`--external-key` to limit a run to one generated group, `--storage-bucket` to select another
bucket, and `--service-account-json` for private GCS objects (this optional mode requires the
`google-auth` runtime). The importer never resumes, updates,
deletes, or rolls back partial writes. Reports are diagnostic only and are stored under
`%LOCALAPPDATA%\TellPal\meditation-import-agent\runs`.
