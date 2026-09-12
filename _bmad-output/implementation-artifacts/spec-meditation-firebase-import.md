---
title: 'Grouped Firebase meditation importer'
type: 'feature'
created: '2026-09-12'
status: 'done'
review_loop_iteration: 0
baseline_commit: '97f72369c812ef8d16f4c57107a917928a926a67'
context:
  - C:/github/tellpalv2/.codex/skills/import-tellpal-story/SKILL.md
  - C:/github/tellpalv2/be/docs/adr/ADR-0010-content-cover-ownership.md
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The legacy meditation CSV has translated rows but no import workflow. The rows must be grouped as one meditation per stable source name instead of creating one content record per language or incorrectly splitting rows by legacy IDs.

**Approach:** Add a dry-run and guarded live importer beside the existing lullaby importer. It will group rows by the normalized cover filename stem, download `cover_images/<image>` and each language's `<id>.zip` from Firebase Storage, extract and validate one MP3 per ZIP, and create one `MEDITATION` content with localized metadata/audio.

## Boundaries & Constraints

**Always:** Use the existing Admin API client and interactive password/`import` confirmation; never mutate before remote preflight and source-fingerprint recheck. Group by normalized `image_url` stem after removing `_kapak`/`cover` suffixes; reject duplicate languages, missing Turkish rows, unsafe object names, invalid image/ZIP/MP3 signatures, and duplicate external keys. Use `image_url` as the single content-level `listeningCoverMediaId`; warn when `summary_image_url` differs. Map CSV `summary` to description. Treat missing DOCX/body text as a preflight warning and block live import until each language has a body-text source. Keep processing status API-owned (`PENDING` on create; never force `COMPLETED`).

**Ask First:** None beyond the explicit user decisions: Firebase Storage root ZIPs are `{id}.zip`, images live under `cover_images/`, and grouping follows stable source names. Publication remains opt-in through the existing `--no-publish`/publish flag semantics.

**Never:** Do not modify backend/CMS application code, migrations, API contracts, source CSV/assets, or resume/update/delete a partial import. Do not use legacy IDs in API external keys or silently merge groups whose normalized names conflict.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| HAPPY_PATH | One row per language sharing a cover stem; valid cover and `{id}.zip` MP3s | One `meditation.<stem>` content, one listening cover, one localization/audio per language; preview lists all groups | N/A |
| SUMMARY_COVER_MISMATCH | Same group has different `summary_image_url` values | Group stays intact and preview reports the ignored legacy field | Warning only; import requires confirmation |
| MISSING_BODY_TEXT | DOCX/body source not supplied yet | Local preflight reports assets/groups but marks live import unavailable | Live importer stops before login/writes with actionable message |
| INVALID_REMOTE_ASSET | Missing cover/ZIP, ZIP not exactly one MP3, invalid signature, or unreadable duration | No API mutation | Stop during preflight with source object and group |
| DUPLICATE_GROUP_MEMBER | Duplicate language or conflicting normalized group key | No plan is produced | Stop with line numbers and conflicting values |

</frozen-after-approval>

## Code Map

- `C:/github/tellpalv2/.codex/skills/import-tellpal-story/scripts/lullaby_manifest.py` -- existing Firebase Storage staging, image/ZIP/MP3 validation, duration and checksum patterns to reuse or extract without changing lullaby semantics.
- `C:/github/tellpalv2/.codex/skills/import-tellpal-story/scripts/lullaby_import_workflow.py` -- guarded remote preflight, mutation ordering, verification, and report conventions for a grouped importer.
- `C:/github/tellpalv2/.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py` -- existing login, content, localization, media upload, update, publish, and read methods.
- `C:/github/tellpalv2/.codex/skills/import-tellpal-story/scripts/import_lullabies.py` and `inspect_lullabies.py` -- interactive CLI and local preflight entry-point conventions.
- `C:/github/tellpalv2/cms/yuklenecek_hikayeler/meditations/meditations.csv` -- ignored local source; 18 rows, four languages, five stable cover/name groups, no DOCX/body text.
- `C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/content/domain/Content.java` -- MEDITATION requires body text and per-localization audio; listening cover is content-scoped.
- `C:/github/tellpalv2/be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java` -- Admin payloads for content-level listening cover and localization audio/body fields.

## Tasks & Acceptance

**Execution:**
- [x] Add meditation manifest/preflight model and tests -- group by stable cover stem, stage Firebase objects, validate assets, compute duration/checksums, and block missing body text for live import.
- [x] Add guarded meditation workflow/CLI and reference documentation -- reuse the Admin client, confirmation gates, report/failure policy, and MEDITATION payload/verification semantics.

**Acceptance Criteria:**
- Given the supplied CSV, when local preflight runs, then it reports exactly five groups and their language/legacy-ID members without making HTTP writes.
- Given valid remote assets and body sources, when the operator confirms `import`, then one MEDITATION content per group is created with one listening cover and language-specific MP3 localizations, followed by read-back verification and optional publication.
- Given absent DOCX/body sources, when live import is attempted, then it stops before any mutation and explains which groups/languages need body text.
- Given a duplicate language, ambiguous group stem, missing remote object, invalid ZIP, or invalid MP3, when preflight runs, then it fails with a precise line/group/object error and produces no write plan.

## Spec Change Log

## Design Notes

The legacy CSV has both `image_url` and `summary_image_url`, but MEDITATION has only one content-level listening cover in the current model. `image_url` is the stable grouping/source selection; `summary_image_url` is retained for diagnostics and warned when inconsistent, preventing the known translated-row split caused by pair-based grouping.

## Verification

**Commands:**
- `python -B -m unittest discover -s .codex/skills/import-tellpal-story/scripts/tests -p "test_meditation_*.py"` -- expected: all meditation importer unit tests pass.
- `python -B -m unittest discover -s .codex/skills/import-tellpal-story/scripts/tests -p "test_*.py"` -- expected: meditation and existing importer tests run; any unrelated baseline lullaby fixture failures are reported separately.
- `python -B .codex/skills/import-tellpal-story/scripts/inspect_meditations.py cms/yuklenecek_hikayeler/meditations/meditations.csv` -- expected: deterministic five-group preflight or a precise missing-asset/body warning without HTTP writes.

## Suggested Review Order

**Import orchestration**

- The workflow sequences guarded content, media, localization, verification, and publication writes.
  [`meditation_import_workflow.py:87`](../../.codex/skills/import-tellpal-story/scripts/meditation_import_workflow.py#L87)

- Remote preflight blocks duplicate external keys before any mutation begins.
  [`meditation_import_workflow.py:64`](../../.codex/skills/import-tellpal-story/scripts/meditation_import_workflow.py#L64)

- The CLI preserves masked authentication, source checks, and exact operator confirmation.
  [`import_meditations.py:22`](../../.codex/skills/import-tellpal-story/scripts/import_meditations.py#L22)

**Grouping and validation**

- Cover stems define stable cross-language groups while legacy IDs remain storage selectors only.
  [`meditation_manifest.py:131`](../../.codex/skills/import-tellpal-story/scripts/meditation_manifest.py#L131)

- CSV normalization and strict row parsing prevent malformed metadata from reaching the API.
  [`meditation_manifest.py:418`](../../.codex/skills/import-tellpal-story/scripts/meditation_manifest.py#L418)

- Body source resolution keeps missing DOCX inputs visible and supports later localized completion.
  [`meditation_manifest.py:510`](../../.codex/skills/import-tellpal-story/scripts/meditation_manifest.py#L510)

**Supporting evidence**

- The test matrix covers grouping, remote assets, body gating, conflicts, fingerprinting, and CLI options.
  [`test_meditation_import.py:162`](../../.codex/skills/import-tellpal-story/scripts/tests/test_meditation_import.py#L162)

- The reference documents Firebase paths, body-source layouts, and safe live-import operation.
  [`meditation-import.md:1`](../../.codex/skills/import-tellpal-story/references/meditation-import.md#L1)
