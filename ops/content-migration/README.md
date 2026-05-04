# TellPal V2 content migration inventory

This tool builds the category-based inventory reports used before copying legacy Drive assets into
the TellPal V2 content structure.

It is intentionally dry-run only. It reads CSV exports, resolves content types, creates import
manifests, and can match against an optional Drive metadata cache. It does not mutate Google Drive.

## Inputs

- `tellpal_stories.csv`: legacy content inventory.
- `categories.csv`: localized category names and V2 category/content types.
- `stories_and_categories.csv`: localized content-to-category links.
- optional `drive_inventory_cache.jsonl`: one Drive item per line with fields such as `id`, `title`,
  `mime_type`, `url`, `path`, and `modified_time`.
- optional `exclusions.csv`: legacy ids intentionally kept out of this migration scope. The default
  file is `ops/content-migration/exclusions.csv`.

## Run

Install Drive API dependencies before using `drive_inventory.py` or `drive_copy.py --execute`:

```powershell
python -m pip install -r ops/content-migration/requirements.txt
```

```powershell
python ops/content-migration/content_inventory.py `
  --stories C:\Users\gurha\Downloads\tellpal_stories.csv `
  --categories C:\Users\gurha\Downloads\categories.csv `
  --story-categories C:\Users\gurha\Downloads\stories_and_categories.csv `
  --exclusions ops/content-migration/exclusions.csv `
  --output-dir ops/content-migration/out
```

With a Drive cache:

```powershell
python ops/content-migration/content_inventory.py `
  --stories C:\Users\gurha\Downloads\tellpal_stories.csv `
  --categories C:\Users\gurha\Downloads\categories.csv `
  --story-categories C:\Users\gurha\Downloads\stories_and_categories.csv `
  --drive-inventory-cache ops/content-migration/drive_inventory_cache.jsonl `
  --output-dir ops/content-migration/out
```

By default, `drive_copy_plan.jsonl` contains only `MATCHED` rows. After manually reviewing
`LIKELY` rows in `match_report.csv`, rerun with `--include-likely-in-copy-plan` if they should be
included in the copy plan.

Build a Drive cache from TELLPAL-ORTAK:

```powershell
python ops/content-migration/drive_inventory.py `
  --root-folder-id 1valmQ5i9wyEhqJ_YG-NLkHdDFwF-fuLi `
  --output ops/content-migration/drive_inventory_cache.jsonl
```

Dry-run the approved copy plan:

```powershell
python ops/content-migration/drive_copy.py `
  --copy-plan ops/content-migration/out/drive_copy_plan.jsonl `
  --target-root-name "V2 İçerik Envanteri" `
  --dry-run
```

Execute the copy plan only after reviewing `match_report.csv` and `drive_copy_plan.jsonl`:

```powershell
python ops/content-migration/drive_copy.py `
  --copy-plan ops/content-migration/out/drive_copy_plan.jsonl `
  --target-root-name "V2 İçerik Envanteri" `
  --execute
```

## Outputs

- `category_type_map.csv`
- `content_type_resolution_report.csv`
- `match_report.csv`
- `drive_copy_plan.jsonl`
- `v2_import_manifest.csv`
- `missing_category_links.jsonl`
- `summary.json`
- `copy_result.csv`
- `post_copy_summary.json`

## Current scope exclusions

`AUDIO_STORY` rows are intentionally marked `OUT_OF_SCOPE`, excluded from
`v2_import_manifest.csv`, and skipped during Drive matching/copy planning. Audio stories are covered
by the `STORY` migration scope, so this pass migrates only `STORY`, `MEDITATION`, and `LULLABY`.

These additional legacy rows are also intentionally marked `OUT_OF_SCOPE`, excluded from
`v2_import_manifest.csv`, and skipped during Drive matching/copy planning:

- `343` / `Os Super Detetives`: category type and `page_count` disagree.
- `366` / `The Opposites-Town`: missing `page_count` and mixed `STORY` / `AUDIO_STORY` category links.
- `442` / `Robô Robison e Gata Aveia ao Trabalho`: category type and `page_count` disagree.

## Type resolution rules

- `categories.csv` is the primary type source.
- `MEDITATION` and `LULLABY` category links are definitive unless they conflict with each other.
- `page_count > 1` resolves `STORY` versus `AUDIO_STORY` conflicts to `STORY`.
- `page_count = 1` resolves non-lullaby/non-meditation audio content to `AUDIO_STORY`, then marks
  it `OUT_OF_SCOPE` for this migration pass.
- Different final content types are never merged into one canonical content record.

## Drive execution

`content_inventory.py --execute` is reserved and fails on purpose. Drive writes happen only through
`drive_copy.py --execute`, using the generated and reviewed `drive_copy_plan.jsonl`.

Set one credential source before calling Drive APIs:

- `GOOGLE_APPLICATION_CREDENTIALS`: service-account JSON path.
- `GOOGLE_OAUTH_CLIENT_SECRET`: installed-app OAuth client JSON path. The token is stored at
  `.google-drive-token.json` unless `GOOGLE_OAUTH_TOKEN_PATH` is set.

Credential files and tokens must not be committed.
