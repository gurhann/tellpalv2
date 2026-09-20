---
title: 'Import legacy story-to-category curation mappings through the Admin API'
type: 'feature'
created: '2026-09-19'
status: 'done'
baseline_commit: '74dd191aad02cf55f070ed27e79f3672c08ea1d6'
review_loop_iteration: 0
context:
  - `{project-root}/AGENTS.md`
  - `{project-root}/_bmad-output/implementation-artifacts/spec-category-import.md`
  - `{project-root}/be/docs/admin-api-rules.md`
  - `{project-root}/.codex/skills/import-tellpal-story/references/admin-api.md`
  - `{project-root}/.codex/skills/import-tellpal-story/references/category-import.md`
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** `category_f_stories.csv` contains 1,434 legacy category-membership rows, but it has no content IDs or category IDs. Its positional columns are `language,type,category name,story name`, and repeated titles, translated titles, duplicate rows, and `STORY`/`AUDIO_STORY` overlap make direct live writes unsafe.

**Approach:** Add a guarded mapping preflight/import workflow beside the existing TellPal import scripts. Resolve category localization names and published existing content localizations to canonical IDs, preserve many-to-many membership and source order, publish the previously imported category localizations as explicitly approved, then add only deterministic missing curation links through the existing Admin API.

## Boundaries & Constraints

**Always:** Treat the first and second `name` CSV columns positionally as category name and story title. Parse quoted multiline titles, trim whitespace, NFC-normalize and case-fold matching keys, preserve source language and canonical category type, map `AUDIO_STORY` category rows to the existing `STORY` content type, and deduplicate only identical `(language,type,category,title)` rows. Resolve categories by `(language,normalized category name)` against existing category localizations and content by `(language,canonical type,normalized title)` against `GET /api/admin/contents`. Require category and content localizations to be published, content to be active, and category/content types to match. Read existing curation links before writes; reuse exact links, preserve existing editorial order, and assign source order to new links without deleting or silently reordering existing links. Publish only existing category localizations whose source/category state is compatible, using an explicit `publishedAt`, then add curation links. Require local validation, complete remote preflight, source fingerprint recheck, masked password input, and standalone `import` confirmation. Write token-free audit reports outside the repository.

**Ask First:** None. The operator approved publishing the previously imported category localizations before mapping (`A`).

**Never:** Infer content identity from translated titles across languages, choose the first candidate when matching is ambiguous, create content or categories, mutate content localization publication state, delete links, overwrite an existing order, retry an ambiguous POST/PUT, send `AUDIO_STORY` as an API type, or persist credentials, tokens, or raw signed URLs.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| HAPPY_PATH | Valid CSV, existing published/compatible content and category localization candidates | Preflight plans deduplicated links; confirmation publishes compatible categories and creates missing links in source order | Verify every affected lane and report counts |
| DUPLICATE_SOURCE | Identical source row repeated | One planned link | Record skipped duplicate; do not double-write |
| AMBIGUOUS_CONTENT | Multiple live content localizations share the normalized language/type/title key | No mutation for the ambiguous row or run | Emit candidates and require an explicit override manifest |
| MISSING_OR_UNPUBLISHED | Category/content localization or active content is absent/unpublished | No affected curation mutation | Report source row, key, and required state |
| EXISTING_CURATED | Link already exists or an existing order conflicts with source order | Reuse exact link; do not reorder or overwrite conflicting editorial state | Report conflict and stop affected lane |
| TYPE_MISMATCH | Category type differs from canonical content type | No POST | Report API-compatible type conflict |

</frozen-after-approval>

## Code Map

- `.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py` -- reuse authentication, read retry, token rotation, and add category localization publication, curation list, and curation add calls without mutation retry.
- `.codex/skills/import-tellpal-story/scripts/import_categories.py` and `category_import_workflow.py` -- reuse interactive password/confirmation, source fingerprint, remote preflight, execution, verification, and failure reporting patterns.
- `.codex/skills/import-tellpal-story/scripts/category_manifest.py` -- reuse CSV fingerprint and safe normalization conventions; the new parser must handle the duplicate positional `name` header and quoted multiline titles.
- `yuklenecek_hikayeler/category_f_stories.csv` -- supplied 1,434-row source; preserve it unchanged and treat rows as many-to-many assignments.
- `be/src/main/java/com/tellpal/v2/category/web/admin/CategoryCurationAdminController.java:104-124` -- POST curation contract with `contentId` and non-negative `displayOrder`.
- `be/src/main/java/com/tellpal/v2/category/application/CategoryCurationService.java:45-64` -- published category localization, active content, type, and published content localization invariants.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java:73-102` and `AdminContentReadResponse.java:7-18` -- read-only content inventory including localized titles, status, type, active state, and narration metadata.
- `be/docs/admin-api-rules.md:455-499` -- duplicate, publication, type, and curation error contract.
- `be/src/test/java/com/tellpal/v2/category/web/admin/CategoryAdminIntegrationTest.java:338-482` -- curation success, ordering, and unpublished-category regression behavior.

## Tasks & Acceptance

**Execution:**
- [x] `.codex/skills/import-tellpal-story/scripts/category_story_mapping_manifest.py` and `tests/test_category_mapping_import.py` -- parse the positional CSV, fingerprint source rows, normalize keys, deduplicate exact assignments, and cover multiline titles, duplicates, type overlap, and ambiguity.
- [x] `.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py` and `category_story_mapping_workflow.py` -- add read-only reconciliation, category publication, curation POSTs, lane verification, conflict reports, and no-retry mutation semantics.
- [x] `.codex/skills/import-tellpal-story/scripts/inspect_category_story_mappings.py`, `import_category_story_mappings.py`, and `category_story_mapping_report.py` -- provide local/live CLI gates, masked password input, confirmation, source recheck, and non-secret audit reporting.

**Acceptance Criteria:**
- Given the supplied CSV, when local preflight runs, then it reports 1,434 logical rows, four languages, 100 category labels, duplicate rows, and ambiguous candidate risks without HTTP writes.
- Given a deterministic remote plan and `import` confirmation, when the workflow runs, then compatible category localizations are published and only missing, type-compatible curation links are added once per category-language lane.
- Given an ambiguous title, missing localization, unpublished content, inactive content, duplicate link, or order conflict, when preflight runs, then the importer performs no affected mutation and emits a token-free diagnostic.
- Given a completed run, when verification runs, then every planned lane contains the expected content IDs and source order, with reused and created links reported separately.

## Design Notes

The CSV's `type` identifies the legacy category lane. `AUDIO_STORY` therefore maps to the existing canonical `STORY` content type and must not force a second content identity. A shared story can legitimately occur in both a normal STORY category and Audio Books; the mapping preserves that many-to-many relationship. Because the API has no cross-lane transaction, preflight must resolve the complete plan before any publication or curation mutation, and an ambiguous mutation response must stop for manual inspection.

## Verification

**Commands:**
- `python3 -B .codex/skills/import-tellpal-story/scripts/inspect_category_story_mappings.py yuklenecek_hikayeler/category_f_stories.csv` -- expected: deterministic row/category/title diagnostics with no HTTP writes.
- `python3 -B -m unittest discover -s .codex/skills/import-tellpal-story/scripts/tests -p 'test_category_mapping_*.py'` -- expected: parser, matching, idempotency, conflict, and report tests pass.
- `git diff --check` -- expected: no whitespace errors.

## Suggested Review Order

**Remote reconciliation and mutation safety**

- Start with the complete remote plan model and lane-level conflict gate.
  [`category_story_mapping_workflow.py:98`](../../.codex/skills/import-tellpal-story/scripts/category_story_mapping_workflow.py#L98)

- Inspect category/content resolution and existing-order protection before any write.
  [`category_story_mapping_workflow.py:135`](../../.codex/skills/import-tellpal-story/scripts/category_story_mapping_workflow.py#L135)

- Verify publication, curation creation, source fingerprint, and post-write lane verification.
  [`category_story_mapping_workflow.py:344`](../../.codex/skills/import-tellpal-story/scripts/category_story_mapping_workflow.py#L344)

- Confirm the Admin API paths for category publication and curation list/add operations.
  [`tellpal_admin_client.py:109`](../../.codex/skills/import-tellpal-story/scripts/tellpal_admin_client.py#L109)

**Source parsing and canonical identity**

- Review positional CSV parsing, exact duplicate handling, and `AUDIO_STORY` canonicalization.
  [`category_story_mapping_manifest.py:207`](../../.codex/skills/import-tellpal-story/scripts/category_story_mapping_manifest.py#L207)

- Review the interactive live-import gates, second preflight, and masked password flow.
  [`import_category_story_mappings.py:28`](../../.codex/skills/import-tellpal-story/scripts/import_category_story_mappings.py#L28)

**Verification and auditability**

- Check local diagnostics and many-to-many source-risk reporting without HTTP writes.
  [`inspect_category_story_mappings.py:11`](../../.codex/skills/import-tellpal-story/scripts/inspect_category_story_mappings.py#L11)

- Check token-free run state, mutation records, and failure reporting outside the repository.
  [`category_story_mapping_report.py:12`](../../.codex/skills/import-tellpal-story/scripts/category_story_mapping_report.py#L12)

- Validate parser, wrapper, conflict, idempotency, fingerprint, CLI-gate, and verification coverage.
  [`test_category_mapping_import.py:101`](../../.codex/skills/import-tellpal-story/scripts/tests/test_category_mapping_import.py#L101)
