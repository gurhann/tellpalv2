---
title: 'Import legacy multilingual categories through the Admin API'
type: 'feature'
created: '2026-09-19'
status: 'done'
baseline_commit: 'eb54da0237f5c4f705c05e0646f6e33225d90a44'
review_loop_iteration: 0
context:
  - `{project-root}/AGENTS.md`
  - `{project-root}/be/docs/project-memory.md`
  - `{project-root}/architecture.md`
  - `{project-root}/be/docs/adr/ADR-0012-remove-audio-story-canonical-type.md`
  - `{project-root}/.codex/skills/import-tellpal-story/references/admin-api.md`
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `tellpal_public_categories.csv` contains 105 multilingual legacy rows without stable group keys or slugs, and four `AUDIO_STORY` rows that cannot be persisted as a canonical v2 category type. The current system needs one category aggregate per approved editorial group and one language-scoped localization per source row.

**Approach:** Add a guarded category manifest, inspection command, and live importer beside the existing TellPal import scripts. Use the approved 33-group mapping explicitly, validate the complete CSV before writes, register Firebase image object paths through the Admin API, create compatible category/localization records idempotently, and persist a non-secret audit report.

## Boundaries & Constraints

**Always:** Preserve the CSV and source IDs; assign all 105 rows exactly once to the approved 33 groups. Use `STORY`, `MEDITATION`, and `LULLABY` only. Map `AUDIO_STORY` rows 29/31/38/163 to the separate `audio-books` slug with canonical type `STORY`; retain the source type only in the audit manifest. Trim names/descriptions, accept `tr`, `en`, `pt`, and `de`, and import new categories as `active=true`, `premium=false` with all localizations `DRAFT` and `publishedAt=null`. Derive Firebase object paths from image URLs without logging query strings or tokens. Resolve/register each unique image object once per run and retain only asset IDs/object-path fingerprints in reports. Perform local validation and read-only remote preflight before the exact `import` confirmation; recheck the CSV fingerprint before the first mutation. Compatible existing slugs/localizations are reused; type, policy, language, or content conflicts are reported without overwriting editorial data. Category-content membership and Audio Books audio-readiness curation are out of this import.

**Ask First:** None; the operator has accepted `DRAFT`, `active=true`, `premium=false`, the 33-group mapping, and category/localization-only scope.

**Never:** Create `AUDIO_STORY` category rows, infer groups from image URLs or translated names at runtime, persist signed Firebase URLs, mutate the source CSV, publish localizations, attach content, or retry an ambiguous mutation response.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|----------------------------|----------------|
| HAPPY_PATH | Valid CSV, approved mapping, resolvable images | 33 categories and 105 DRAFT localizations are created/reused | Report all source-to-v2 mappings |
| INVALID_SOURCE | Missing columns, duplicate/unknown ID, unsupported language/type, blank text, unassigned ID | No remote mutation | Deterministic validation error with line number |
| AUDIO_STORY | Source type `AUDIO_STORY` | One `audio-books` STORY category with four localizations | Reject any canonical AUDIO_STORY payload |
| EXISTING_STATE | Compatible or conflicting slug/localization | Compatible records are skipped; conflicts remain unchanged | Record conflict and stop the affected group |
| IMAGE_FAILURE | Invalid Firebase URL, missing object path, or registration conflict without a known asset ID | Localization is not mutated | Record unresolved image work without exposing the URL token |
| RERUN | Same CSV and existing imported records | No duplicate category or category-language rows | Verify counts and source fingerprint |

</frozen-after-approval>

## Code Map

- `{project-root}/_bmad-output/specs/spec-category-import/SPEC.md` and `category-import-mapping.md` -- approved 105-row source profile, 33 slugs, Audio Books rule, policy decisions, and data-quality findings.
- `{project-root}/.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py:34-241` -- authenticated JSON client and multipart media path; extend with category, localization, and storage-location registration calls.
- `{project-root}/be/src/main/java/com/tellpal/v2/category/web/admin/CategoryAdminController.java:42-182` -- existing category list/create/read and localization list/create/update endpoints; no bulk endpoint exists.
- `{project-root}/be/src/main/java/com/tellpal/v2/category/application/CategoryManagementService.java:25-115` and `category/domain/Category.java:32-132` -- slug uniqueness, image reference validation, localization upsert, and DRAFT/PUBLISHED invariants.
- `{project-root}/be/src/main/java/com/tellpal/v2/category/domain/CategoryType.java:7-21` and `{project-root}/be/docs/adr/ADR-0012-remove-audio-story-canonical-type.md:16-34` -- canonical type boundary and Audio Books compatibility rule.
- `{project-root}/be/src/main/java/com/tellpal/v2/asset/web/admin/AssetAdminController.java:87-183` and `asset/application/AssetRegistryService.java:194-249` -- backend upload/registration and storage-location identity; signed URLs must remain transient.
- `{project-root}/yuklenecek_hikayeler/tellpal_public_categories.csv` -- 105-row UTF-8 source supplied by the operator; do not copy or rewrite it.

## Tasks & Acceptance

**Execution:**
- [x] `.codex/skills/import-tellpal-story/references/category-import.md` and `category_import_mapping.json` -- document the approved mapping, policies, payloads, report format, and recovery rules.
- [x] `.codex/skills/import-tellpal-story/scripts/category_manifest.py` -- parse/normalize/validate the CSV, enforce the explicit 33-group mapping, derive safe object paths, and produce a fingerprinted plan without tokens.
- [x] `.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py` -- add category/localization reads and writes plus media registration with the existing authentication/retry rules.
- [x] `.codex/skills/import-tellpal-story/scripts/category_import_workflow.py`, `category_import_report.py`, `inspect_categories.py`, and `import_categories.py` -- implement local inspection, read-only remote preflight, confirmation gate, idempotent group execution, verification, and atomic non-secret reporting.
- [x] `.codex/skills/import-tellpal-story/scripts/tests/test_category_*.py` and `SKILL.md` -- cover source validation, grouping, Audio Books mapping, token redaction, conflicts, reruns, and CLI usage.

**Acceptance Criteria:**
- Given the supplied CSV, local inspection reports 105 rows, 33 groups, 4 languages, and no unassigned source IDs without making HTTP requests.
- Given a confirmed valid plan, the importer creates or reuses 33 canonical categories and 105 DRAFT localizations; repeated execution creates no duplicate slug or category-language row.
- Given Audio Books rows, the resulting category slug is `audio-books`, its type is `STORY`, and no `AUDIO_STORY` value is sent to the API.
- Given a malformed source, group conflict, unresolved image, or changed fingerprint, the importer performs no affected mutation and emits an actionable, token-free diagnostic.

## Spec Change Log

## Design Notes

The current API separates category and localization mutations, so the workflow preflights the complete group and records partial/unknown outcomes explicitly. The approved mapping is data, not a runtime heuristic: translations with missing language peers remain intentional single/partial groups, while reused image paths are deduplicated only as asset identity and never treated as editorial grouping evidence.

## Verification

**Commands:**
- `python3 -B .codex/skills/import-tellpal-story/scripts/inspect_categories.py /Users/gurhankucuk/Documents/GitHub/tellpalv2/yuklenecek_hikayeler/tellpal_public_categories.csv` -- expected: deterministic 105-row/33-group preview with no HTTP writes.
- `python3 -B -m unittest discover -s .codex/skills/import-tellpal-story/scripts/tests -p 'test_category_*.py'` -- expected: all category importer tests pass.
- `python3 -B -m unittest discover -s .codex/skills/import-tellpal-story/scripts/tests -p 'test_*.py'` -- expected: existing and category importer tests pass, with unrelated baseline failures reported separately.

## Suggested Review Order

**Import entry point**

- Interactive gate, source verification, confirmation, and mutation sequencing.
  [`import_categories.py:24`](../../.codex/skills/import-tellpal-story/scripts/import_categories.py#L24)

**Source validation and safety**

- Build the complete fingerprinted plan and enforce the approved mapping and Audio Books rule.
  [`category_manifest.py:105`](../../.codex/skills/import-tellpal-story/scripts/category_manifest.py#L105)

- Verify each unique signed image URL before any Admin API mutation.
  [`category_manifest.py:194`](../../.codex/skills/import-tellpal-story/scripts/category_manifest.py#L194)

**Remote reconciliation and writes**

- Preflight compatible categories, partial localizations, conflicts, and existing image assets.
  [`category_import_workflow.py:66`](../../.codex/skills/import-tellpal-story/scripts/category_import_workflow.py#L66)

- Resolve assets, create missing records, and preserve idempotent reuse across groups.
  [`category_import_workflow.py:117`](../../.codex/skills/import-tellpal-story/scripts/category_import_workflow.py#L117)

- Verify stored localization state and image asset identity after each group.
  [`category_import_workflow.py:264`](../../.codex/skills/import-tellpal-story/scripts/category_import_workflow.py#L264)

**Audit and verification**

- Keep reports atomic and free from signed URLs or raw object paths.
  [`category_import_report.py:14`](../../.codex/skills/import-tellpal-story/scripts/category_import_report.py#L14)

- Review regression coverage for grouping, conflicts, reruns, source failures, and redaction.
  [`test_category_import.py:109`](../../.codex/skills/import-tellpal-story/scripts/tests/test_category_import.py#L109)
