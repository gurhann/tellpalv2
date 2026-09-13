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
- A non-empty body text source is required for every group/language before a standard live import. Sources may
  be UTF-8 `.txt`/`.md` files or DOCX files. Use a body directory containing
  `<group>/<language>.<txt|md|docx>` files, flat `<group>.<language>.<txt|md|docx>` files, or pass
  explicit mappings with `--body-source GROUP/LANGUAGE=PATH`. Unrelated files in the body directory
  are ignored.

When body sources are not available yet, an explicitly approved staged import can create the
available media and metadata while leaving each missing body as `null`. Both `--allow-missing-body`
and `--no-publish` are mandatory for this mode; the API receives `DRAFT`/`PENDING` localizations
and no publication request is made. The missing group/language entries are listed in the preview
and run report. The normal strict behavior remains unchanged without the opt-in flag.

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

For an approved staged import when body files are not available, pass both explicit safety flags:

```powershell
& $python -B '<skill-dir>\scripts\inspect_meditations.py' `
  'C:\github\tellpalv2\cms\yuklenecek_hikayeler\meditations\meditations.csv' `
  --service-account-json 'C:\dev\keys\tellpal-v2-firebase-storage-pk.json' `
  --allow-missing-body --no-publish
& $python -B '<skill-dir>\scripts\import_meditations.py' `
  'C:\github\tellpalv2\cms\yuklenecek_hikayeler\meditations\meditations.csv' `
  --service-account-json 'C:\dev\keys\tellpal-v2-firebase-storage-pk.json' `
  --allow-missing-body --no-publish
```

After editorial text arrives, complete one staged localization through the existing PUT endpoint.
Keep the audio and duration values from the original import, leave the localization in DRAFT/PENDING,
and provide a non-blank `bodyText`:

```powershell
$payload = @{
  title = 'Staged Meditation'
  description = 'A staged description'
  bodyText = 'Breathe slowly.'
  coverMediaId = $null
  audioMediaId = 123
  durationMinutes = 8
  status = 'DRAFT'
  processingStatus = 'PENDING'
  publishedAt = $null
} | ConvertTo-Json
Invoke-RestMethod -Method Put `
  -Uri "$env:TELLPAL_API_BASE_URL/api/admin/contents/456/localizations/en" `
  -Headers @{ Authorization = "Bearer $adminToken" } `
  -ContentType 'application/json' -Body $payload
```

Use `--no-publish` to leave all localizations in `DRAFT`, `--inactive` to create inactive content,
`--external-key` to limit a run to one generated group, `--storage-bucket` to select another
bucket, and `--service-account-json` for private GCS objects (this optional mode requires the
`google-auth` runtime). Add `--allow-missing-body` together with `--no-publish` to stage incomplete
MEDITATION localizations. The importer never resumes, updates,
deletes, or rolls back partial writes. Reports are diagnostic only and are stored under
`%LOCALAPPDATA%\TellPal\meditation-import-agent\runs`.
