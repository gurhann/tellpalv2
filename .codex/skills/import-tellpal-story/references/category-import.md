# TellPal Category Import Contract

## Input and approved grouping

The input is `tellpal_public_categories.csv` with columns `id,name,description,language,type,image_url`.
The importer loads `category_import_mapping.json`; it never infers multilingual identity at runtime.
The approved mapping contains 33 slugs and accounts for all 105 source IDs exactly once.

The source language set is `tr`, `en`, `pt`, and `de`. Canonical category types are `STORY`,
`MEDITATION`, and `LULLABY`. Source `AUDIO_STORY` is accepted only for IDs 29, 31, 38, and 163;
these rows become the separate `audio-books` slug with canonical type `STORY`.

`image_url` is treated as a transient Firebase Storage locator. The manifest extracts only the
object path and stores a SHA-256 object-path key in reports. Query parameters, including signed
tokens, must never be printed or persisted.

## Local inspection

Run the no-write preflight first:

```text
python3 -B .codex/skills/import-tellpal-story/scripts/inspect_categories.py \
  /absolute/path/to/tellpal_public_categories.csv
```

The command reports the source fingerprint, row/group/language counts, source IDs, canonical types,
and image-key count. It rejects malformed headers, duplicate or unassigned IDs, unsupported values,
blank names, duplicate languages inside a group, unsafe Firebase object paths, and incompatible
mapping types. It does not perform HTTP requests.

## Live import

Set `TELLPAL_API_BASE_URL` and `TELLPAL_ADMIN_USERNAME`, then run the live command in an interactive
terminal. The script asks for the password without storing it and requires the standalone `import`
confirmation keyword. It performs remote read-only preflight before the prompt and repeats that
preflight after confirmation.

```text
python3 -B .codex/skills/import-tellpal-story/scripts/import_categories.py \
  /absolute/path/to/tellpal_public_categories.csv
```

New categories use `{type, premium:false, active:true}`. New localizations use
`{name, description, imageMediaId, status:"DRAFT", publishedAt:null}`. The importer registers each
unique Firebase object path once through `POST /api/admin/media`, then sends only the returned asset
ID to the category localization endpoint. Compatible existing categories and localizations are
reused; conflicting editorial state is not overwritten.

## Safety and recovery

No mutation occurs before local validation, read-only verification of each unique signed image URL,
remote preflight, confirmation, and a source fingerprint recheck. A deterministic conflict blocks the
affected group. An ambiguous transport error stops the run and requires manual inspection; the importer
never retries a mutation or deletes partial data.
Run artifacts are written outside the repository and source folder under the platform TellPal data
directory. They contain IDs, statuses, fingerprints, and object-path hashes only.

Category-content membership, display order, publication, and Audio Books narration-readiness curation
are intentionally deferred to a later operation.
