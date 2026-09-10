---
title: 'Import legacy lullabies into the live TellPal system'
type: 'feature'
created: '2026-09-10'
status: 'done'
baseline_commit: 'e1dc42cd314885b5cb8bd970661f1dd9f82661c7'
review_loop_iteration: 0
context:
  - C:/github/tellpalv2/AGENTS.md
  - C:/github/tellpalv2/be/docs/project-memory.md
  - C:/github/tellpalv2/be/docs/adr/ADR-0010-content-cover-ownership.md
  - C:/github/tellpalv2/be/docs/adr/ADR-0011-lullaby-instrument-catalog.md

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The legacy lullaby export contains language rows, two cover roles, and one ZIP audio object per legacy ID, while the current model requires one shared LULLABY aggregate with separate listing and listening covers. There is no safe repeatable path to import these records into the live Admin API.

**Approach:** Add a dedicated importer beside the existing story importer. It groups rows by cover pair, downloads and validates GCS images and ZIP audio, derives a Turkish external key and MP3 duration, creates shared playback/covers plus language titles, and publishes only after remote verification and an explicit terminal confirmation.

## Boundaries & Constraints

**Always:** Use the existing Admin API client and multipart media endpoint; keep secrets in memory only; reject duplicate external keys, invalid media, duplicate languages, missing Turkish rows, divergent audio within one group, and ambiguous contributors before the first write; preserve the CSV source; treat summary text as display-only and map only its musician and supported instrument values.

**Ask First:** None for the requested import behavior. The operator may choose `--no-publish` or `--inactive` when invoking the importer.

**Never:** Do not modify backend/CMS application code or migrations; do not write directly to the database or storage; do not resume, update, delete, roll back, or retry an ambiguous mutation; do not mark processing complete; do not send legacy IDs to the API.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|-----------------------------|----------------|
| HAPPY_PATH | CSV rows share a cover pair and all language ZIPs contain the same MP3 | One LULLABY per pair, two content-level covers, one playback source, title localizations, musician/instruments, optional publication | Preview and write only after `import` confirmation |
| DIVERGENT_AUDIO | Same cover pair has different audio content fingerprints | No content/media mutation | Stop during local preflight with the affected IDs |
| EXISTING_KEY | Generated `lullaby.<turkish-slug>` exists remotely | No mutation | Stop during authenticated remote preflight |
| INVALID_MEDIA | Missing/empty/bad-signature image, ZIP, or MP3 | No mutation | Stop with object name and validation reason |
| NO_INSTRUMENT | Summary includes an unmanaged instrument such as Piano | Import continues with supported codes only | Show a warning in preview |

</frozen-after-approval>

## Code Map

- `C:/github/tellpalv2/.codex/skills/import-tellpal-story/scripts/lullaby_manifest.py` -- CSV validation, cover-pair grouping, GCS staging, ZIP extraction, MP3 duration/fingerprint, instrument parsing, and source fingerprint.
- `C:/github/tellpalv2/.codex/skills/import-tellpal-story/scripts/import_lullabies.py` -- interactive live entry point, masked login, confirmation gate, and report lifecycle.
- `C:/github/tellpalv2/.codex/skills/import-tellpal-story/scripts/inspect_lullabies.py` -- local storage preflight and deterministic preview using the same plan builder.
- `C:/github/tellpalv2/.codex/skills/import-tellpal-story/scripts/lullaby_import_workflow.py` -- remote preflight, mutation order, contributor resolution, publication, and post-write verification.
- `C:/github/tellpalv2/.codex/skills/import-tellpal-story/scripts/lullaby_import_report.py` -- non-secret manifest/result reports under the local application data directory.
- `C:/github/tellpalv2/.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py` -- shared HTTP client; add playback, instrument catalog, and instrument replacement calls without changing API contracts.
- `C:/github/tellpalv2/.codex/skills/import-tellpal-story/scripts/tests/test_lullaby_manifest.py` -- fake-storage unit coverage for grouping, parsing, real CSV shape, and divergent audio rejection.
- `C:/github/tellpalv2/.codex/skills/import-tellpal-story/references/lullaby-import.md` -- operator usage and safety documentation.

## Tasks & Acceptance

**Execution:**
- [x] Add the lullaby plan/staging and validation implementation -- provide deterministic, source-preserving input preparation.
- [x] Extend the shared Admin API client and add the guarded import workflow/report -- preserve existing mutation safety and verify stored state.
- [x] Add operator documentation and fake-storage tests -- make the live command reviewable without network writes.

**Acceptance Criteria:**
- Given the supplied `lullabies.csv`, when storage objects are available, then the preflight reports 11 groups, 33 localizations, separate cover roles, and one shared playback plan per group.
- Given a valid plan and an authenticated Admin API, when the operator enters exactly `import`, then writes occur in create, media, cover, playback, localization, contributor, verify, and publish order.
- Given any preflight or verification failure, then the importer stops without automatic cleanup and records a non-secret diagnostic report.

## Spec Change Log

## Design Notes

The importer intentionally groups by `(image_url, summary_image_url)` because the CSV has no canonical content key. It compares normalized MPEG frame content across each group’s legacy IDs, allowing language rows to share one playback asset while avoiding a silent choice when recordings differ. The static `image_url` is assigned to `listingCoverMediaId`; the animated or detail `summary_image_url` is assigned to `listeningCoverMediaId`.

## Verification

**Commands:**
- `C:/Users/gurha/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe -m unittest discover -s .codex/skills/import-tellpal-story/scripts/tests -p 'test_lullaby_manifest.py'` -- expected: 4 tests pass.
- `C:/Users/gurha/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe -m unittest discover -s .codex/skills/import-tellpal-story/scripts/tests` -- expected: 41 tests pass.
- `C:/Users/gurha/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe .codex/skills/import-tellpal-story/scripts/inspect_lullabies.py --help` -- expected: usage text is displayed without network access.

## Suggested Review Order

**Guarded import flow**

- Builds a deterministic, source-preserving plan and rejects unsafe legacy input.
  [lullaby_manifest.py:127](../../.codex/skills/import-tellpal-story/scripts/lullaby_manifest.py#L127)

- Performs authenticated conflict checks, ordered writes, and remote state verification.
  [lullaby_import_workflow.py:49](../../.codex/skills/import-tellpal-story/scripts/lullaby_import_workflow.py#L49)

- Requires masked credentials and explicit confirmation before any mutation.
  [import_lullabies.py:18](../../.codex/skills/import-tellpal-story/scripts/import_lullabies.py#L18)

**Admin API and diagnostics**

- Adds the shared playback and instrument calls without changing API contracts.
  [tellpal_admin_client.py:92](../../.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py#L92)

- Persists non-secret lifecycle diagnostics for failures and partial writes.
  [lullaby_import_report.py:13](../../.codex/skills/import-tellpal-story/scripts/lullaby_import_report.py#L13)

**Operator support and verification**

- Documents the safe preflight and live import commands for operators.
  [lullaby-import.md:1](../../.codex/skills/import-tellpal-story/references/lullaby-import.md#L1)

- Covers validation, import ordering, media safety, and HTTP request contracts.
  [test_lullaby_manifest.py:117](../../.codex/skills/import-tellpal-story/scripts/tests/test_lullaby_manifest.py#L117)
