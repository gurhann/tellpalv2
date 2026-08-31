---
name: import-tellpal-story
description: Validate and import prepared multilingual TellPal story folders through the existing CMS Admin API. Use when a user provides metadata.csv, per-language DOCX/images/audio, and yazısız source images and wants to automate content, localization, story-page, media, publication, and contributor entry without changing backend or CMS application code.
---

# Import TellPal Story

Use the bundled scripts as the only mutation path. Do not recreate the API workflow manually and do not modify `be/`, `cms/src/`, migrations, or API contracts.

## Prepare the runtime

1. Load Codex workspace dependencies and prefer the returned bundled Python executable. If the loader is unavailable, use an available Python 3.11+ runtime; the import scripts have no third-party runtime dependencies. Do not install packages.
2. Resolve this skill directory and the supplied story directory to absolute paths.
3. Read [references/admin-api.md](references/admin-api.md) before a live import or when diagnosing an API failure.

## Run local preflight

Run this first; it performs no HTTP requests and does not change the source folder:

```powershell
& '<bundled-python>' -B '<skill-dir>\scripts\inspect_story.py' '<story-directory>'
```

Report the external key, languages, page count, upload count, contributor scopes, publish languages, fingerprint, and warnings. Stop on every validation error. Do not weaken validation or silently rename files.

The preflight also rejects empty media and invalid JPEG, PNG, GIF, or MP3 signatures. Treat image names containing the standalone token `kapak` or `cover` as cover candidates, and treat a leading integer in a media filename as its page number. Report every non-exact selection and stop on unresolved same-priority ambiguity. Files warned as ignored are not uploaded but are deliberately included in the whole-folder fingerprint.

Accept page markers stored as text or as decimal Word automatic-list numbering, including titleless documents whose marked page starts have unmarked continuation paragraphs. Accept a titleless DOCX when it yields exactly `page_count` blank-separated blocks or exactly `page_count` non-empty paragraphs. Keep each block or paragraph as one page and preserve its internal line breaks.

When normalized localization titles are duplicated across languages, do not infer their rows. Pass one explicit `--metadata-row LANGUAGE=LEGACY_ID` argument for every duplicated-title language to both preflight and live import. Use the CSV `id` only to select the row; never send it to the API.

When visual inspection proves that textless source images are numbered incorrectly, keep the source folder unchanged and pass one explicit `--textless-page PAGE=RELATIVE_PATH` argument per corrected page to both preflight and live import. Show these mappings as warnings and reject out-of-range, outside-folder, missing, unsupported, or multiply assigned files.

When the user explicitly approves importing a story with no textless media, pass `--allow-missing-textless` to both preflight and live import. Accept this exception only when the yazısız/yazisiz directory is empty or absent; reject partially populated textless media and reject combining the option with `--textless-page`. Show zero textless attachments and a prominent warning in the preview. Leave the default strict requirement unchanged.

When the user explicitly asks to replace or clear one localization description without editing `metadata.csv`, pass `--description LANGUAGE=TEXT` to both preflight and live import. Use an empty value such as `--description de=` to send `null`. Show the override in the preview warnings.

When remote preflight finds multiple case-insensitive exact contributor-name matches, stop before writes. After the user or existing usage identifies the canonical record, pass `--contributor-id NAME=ID` to live import. The importer must verify that the selected ID is among the current exact-name matches before allowing writes.

## Run the live import

1. Require `TELLPAL_API_BASE_URL` and `TELLPAL_ADMIN_USERNAME` in the environment.
2. Tell the user that the skill is pausing for a masked password and the `import` confirmation keyword.
3. Launch the importer in a user-visible interactive PowerShell terminal. Do not run it through a non-interactive shell, pass the password in chat, add a password environment variable, or bypass the confirmation prompt.

```powershell
& '<bundled-python>' -B '<skill-dir>\scripts\import_story.py' '<story-directory>'
```

If preflight used `--textless-page`, `--metadata-row`, `--description`, or `--allow-missing-textless` arguments, repeat the exact same arguments on this command. Add any reviewed `--contributor-id` mappings required by remote preflight.

The importer logs in, performs read-only remote preflight, prints the approved plan, and accepts writes only after the user types `import` (case-insensitive). Reject blank input or any additional text. It rechecks the source fingerprint immediately before the first content write.

## Enforce safety boundaries

- Stop without writes when the external key already exists.
- Stop when a contributor name has multiple case-insensitive exact matches.
- Never resume, update, delete, deactivate, or clean up a partial import automatically.
- Never retry a mutation after an ambiguous transport failure. Read-only calls may use the script's bounded retry behavior.
- Never mark media processing `COMPLETED`; publication may remain invisible to mobile while processing is `PENDING`.
- Never expose passwords, access tokens, refresh tokens, or authorization headers in commentary, commands, or reports.

## Report the result

Return the content ID, external key, imported languages, page/media/contributor counts, published languages, and the absolute `result.json` path. On failure, report the failed phase and last endpoint from the run report and state that recovery is manual.

Run artifacts are stored under `%LOCALAPPDATA%\TellPal\story-import-agent\runs\<run-id>\`. They are diagnostic only and cannot be used to resume.

