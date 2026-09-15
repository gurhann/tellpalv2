---
title: 'Import Firebase audio stories into existing story localizations'
type: 'feature'
created: '2026-09-15'
status: 'done'
baseline_commit: '3d678ca3ab244f1c66a28e58f76050fb5e6906d4'
review_loop_iteration: 0
context:
  - `{project-root}/AGENTS.md`
  - `{project-root}/be/docs/admin-api-rules.md`
  - `{project-root}/be/docs/adr/ADR-0010-content-cover-ownership.md`
  - `{project-root}/be/docs/adr/ADR-0012-remove-audio-story-canonical-type.md`
  - `{project-root}/.codex/skills/import-tellpal-story/SKILL.md`
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `cms/yuklenecek_hikayeler/audio_stories/audio_stories.csv` lists legacy audio stories whose Firebase assets must be attached to existing localized `STORY` records. The operation needs to process many rows while preserving progress when one row is invalid.

**Approach:** Add a dedicated dry-run and guarded live importer beside the existing Firebase importers. It matches an exact normalized `language + name`, validates `cover_images/{image_url}` and `{id}.zip`, uploads one MP3 and a missing shared listening cover, updates nested localization narration while preserving current fields/state, and writes per-row results to a sidecar CSV.

## Boundaries & Constraints

**Always:** Keep the source CSV unchanged; atomically write `<csv-stem>.import.csv` with `status`, `error`, `content_id`, `audio_asset_id`, `cover_asset_id`, `duration_minutes`, and `processed_at`. Use `id` only as the root Firebase ZIP selector. Match only `STORY` localizations in the requested language using trim, NFC normalization, case-folding, and collapsed whitespace; zero or multiple matches are row errors. Require one non-empty valid MP3 per ZIP, validate image signatures, and derive a positive whole-minute duration. Use existing Admin API multipart upload and PUT localization contracts; narration is `{audioMediaId,durationMinutes}`, cover is content-level `listeningCoverMediaId`. Preserve publication/processing/body/reading-cover state and never publish. Identical duplicate rows import once; later copies are `SKIPPED_DUPLICATE`. Deterministic row failures continue, but authentication, storage-wide/preflight, and ambiguous mutation transport failures stop the run.

**Ask First:** None; the operator-approved behavior is fixed by this spec.

**Never:** Create content/localizations; create or accept canonical `AUDIO_STORY`; write directly to DB/Firebase; overwrite existing narration or conflicting listening covers; mark processing complete; mutate the source CSV; automatically resume, delete, clean up, or retry an unknown mutation.

## I/O & Edge-Case Matrix

| Scenario | Expected behavior | Error handling |
|----------|-------------------|----------------|
| HAPPY_PATH | Upload/reuse audio, attach missing listening cover, update narration, mark `SUCCESS`. | Continue. |
| NO_MATCH | No mutation; record exact-match details as `ERROR`. | Continue. |
| ALREADY_IMPORTED | Compatible narration already exists; do not overwrite; record `ALREADY_IMPORTED`. | Continue. |
| DUPLICATE_ROW | Only first identical row can mutate; later copy is `SKIPPED_DUPLICATE`. | Continue. |
| INVALID_STORAGE / CONFLICT | Record invalid object or conflicting asset as `ERROR`; do not overwrite. | Continue. |
| AMBIGUOUS_MUTATION | Record `UNKNOWN` in run diagnostics after timeout/connection reset. | Stop; manual inspection. |

</frozen-after-approval>

## Code Map

- `{project-root}/.codex/skills/import-tellpal-story/scripts/lullaby_manifest.py:86-162,403-527` -- reusable Firebase staging, filename validation, image checks, single-MP3 ZIP extraction, hashing, and duration helpers.
- `{project-root}/.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py:77-141,213-231` -- authenticated Admin API client; add the missing non-retried PUT localization method.
- `{project-root}/.codex/skills/import-tellpal-story/scripts/lullaby_import_workflow.py:15-221,240-258` and `lullaby_import_report.py:13-108` -- guarded preview, media upload, full-update, verification, and non-secret report patterns.
- `{project-root}/be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java:232-414` and `{project-root}/be/docs/admin-api-rules.md:146-167,618-625` -- full localization update, nested STORY narration, and shared cover semantics.
- `{project-root}/cms/yuklenecek_hikayeler/audio_stories/audio_stories.csv` -- real input: 172 rows, `en`/`pt`/`tr`, with identical duplicates that must be classified safely.

## Tasks & Acceptance

**Execution:**
- [x] `.codex/skills/import-tellpal-story/scripts/audio_story_manifest.py` -- parse rows with line numbers, classify duplicates, stage/validate Firebase assets, derive durations, and retain row-level errors.
- [x] `.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py` -- add the existing PUT localization call without changing API contracts.
- [x] `.codex/skills/import-tellpal-story/scripts/audio_story_import_workflow.py`, `audio_story_import_report.py`, `inspect_audio_stories.py`, and `import_audio_stories.py` -- implement matching, confirmation, per-row continuation, sidecar status, verification, and fatal unknown-mutation handling.
- [x] `.codex/skills/import-tellpal-story/scripts/tests/test_audio_story_import.py`, `references/audio-story-import.md`, and `SKILL.md` -- cover edge cases and document operation/recovery.

**Acceptance Criteria:**
- Given a valid row and matching STORY localization, when confirmed, then narration and any missing shared listening cover are attached without changing publication state.
- Given a deterministic row validation/match/HTTP 4xx failure, then the sidecar records `ERROR` and later rows continue.
- Given compatible existing narration or an identical duplicate, then no duplicate mutation occurs and the correct skip status is written.
- Given an ambiguous mutation transport failure, then the run records `UNKNOWN`, stops, and never retries that mutation.
- Given the CSV changes after preview, then fingerprint validation blocks all writes.

## Design Notes

The importer is localization-oriented: `AUDIO_STORY` is only a discovery presentation, while persisted narration belongs to the existing STORY localization. Content updates must preserve all current cover IDs because the Admin API treats cover fields as a full update. A language row may succeed even if another language of the same content fails; a conflicting shared cover remains an explicit error.

## Verification

**Commands:**
- `python -B -m unittest discover -s .codex/skills/import-tellpal-story/scripts/tests -p "test_audio_story_*.py"` -- expected: new importer tests pass.
- `python -B -m unittest discover -s .codex/skills/import-tellpal-story/scripts/tests -p "test_*.py"` -- expected: existing and new importer tests pass.
- `python -B .codex/skills/import-tellpal-story/scripts/inspect_audio_stories.py --help` -- expected: help renders without API writes.

## Suggested Review Order

**Import contract and row planning**

- Read the plan builder first to understand CSV, Firebase, duplicate, and source-fingerprint rules.
  [`audio_story_manifest.py:124`](../../.codex/skills/import-tellpal-story/scripts/audio_story_manifest.py#L124)

- Confirm sidecar writes are atomic and cannot replace the source CSV.
  [`audio_story_manifest.py:255`](../../.codex/skills/import-tellpal-story/scripts/audio_story_manifest.py#L255)

**Remote safety and mutation workflow**

- Follow preflight matching and existing-asset conflict classification before any writes.
  [`audio_story_import_workflow.py:53`](../../.codex/skills/import-tellpal-story/scripts/audio_story_import_workflow.py#L53)

- Inspect the row loop for continuation, reuse, and unknown-outcome stop behavior.
  [`audio_story_import_workflow.py:89`](../../.codex/skills/import-tellpal-story/scripts/audio_story_import_workflow.py#L89)

- Check duplicate target protection before shared content mutations.
  [`audio_story_import_workflow.py:246`](../../.codex/skills/import-tellpal-story/scripts/audio_story_import_workflow.py#L246)

- Verify full-field preservation and uploaded-media identity after each successful row.
  [`audio_story_import_workflow.py:316`](../../.codex/skills/import-tellpal-story/scripts/audio_story_import_workflow.py#L316)

**API and operator entry points**

- Review authenticated API retry and base-URL credential validation.
  [`tellpal_admin_client.py:353`](../../.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py#L353)

- Confirm dry-run and live CLI gates remain separate and require explicit confirmation.
  [`import_audio_stories.py:17`](../../.codex/skills/import-tellpal-story/scripts/import_audio_stories.py#L17)

**Verification**

- Run the focused importer tests covering duplicates, conflicts, reuse, unknown outcomes, and source changes.
  [`test_audio_story_import.py:169`](../../.codex/skills/import-tellpal-story/scripts/tests/test_audio_story_import.py#L169)
