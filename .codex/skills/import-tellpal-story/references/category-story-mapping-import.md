# Category/story mapping import

Run `scripts/inspect_category_story_mappings.py <csv>` for local inspection and
`scripts/import_category_story_mappings.py <csv>` in an interactive terminal for import.
The live command requires `TELLPAL_API_BASE_URL`, `TELLPAL_ADMIN_USERNAME`, a masked
password, and the `import` confirmation. It repeats remote preflight before writing.

Preflight skips missing, ambiguous, inactive, unpublished, or conflicting assignments
and continues valid assignments, including other rows in the same category-language
lane. An unresolved category or corrupt stored curation skips the affected lane.
Categories with no valid assignments are not published. If the entire plan has no
valid assignments, the command exits before writes.

Existing links and their display orders are preserved, including links outside the
CSV. New links retain their original source positions; skipped rows may leave gaps.
Re-running after a partial import reuses matching existing links.

During writes, definitive HTTP 400, 404, 409, and 422 rejections are recorded and
skipped. A rejected category publication skips that lane. Authentication failures,
transport failures, unexpected server errors, source changes, and verification
failures still stop the run. Mutations are never retried automatically.

The token-free external `result.json` contains `skippedRecords` with source rows and
reasons, mutation records, and `summary.skippedLinks`. Verified runs containing skips
end with `COMPLETED_WITH_SKIPS`; otherwise they end with `COMPLETED`.

Content DRAFT status remains a skipped precondition. Category and content languages
still use the existing same-language API contract. Cross-language English-story
categories are not resolved by this change.
