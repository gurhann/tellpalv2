---
id: SPEC-category-import
companions:
  - category-import-mapping.md
  - ../spec-story-audio-experience/SPEC.md
  - ../spec-story-audio-experience/audio-story-contract.md
  - ../../../architecture.md
  - ../../../be/docs/adr/ADR-0007-category-type-aligns-with-content-type.md
  - ../../../be/docs/adr/ADR-0012-remove-audio-story-canonical-type.md
  - ../../../be/src/main/java/com/tellpal/v2/category/domain/CategoryType.java
  - ../../../be/src/main/java/com/tellpal/v2/category/web/admin/CategoryAdminController.java
  - ../../../be/src/main/java/com/tellpal/v2/category/application/CategoryContentReferenceValidator.java
sources:
  - /Users/gurhankucuk/Documents/GitHub/tellpalv2/yuklenecek_hikayeler/tellpal_public_categories.csv
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for importing the legacy multilingual public categories into the v2 category model.

# Multilingual Category Import

## Why

The legacy public-category CSV contains 105 localized rows whose source IDs do not express multilingual identity, while the v2 model requires one category aggregate with language-scoped localizations. The import must preserve editorial category boundaries, keep Audio Books separately discoverable, and avoid reintroducing `AUDIO_STORY` as a canonical category type.

## Capabilities

- **CAP-1**
  - **intent:** The importer validates the category CSV before any mutation.
  - **success:** A dry run deterministically reports schema, encoding, required text, supported-language, source-ID, type, and row-assignment validation results.

- **CAP-2**
  - **intent:** The importer maps source rows into explicit category groups and language localizations.
  - **success:** Every source row is assigned exactly once to a reviewed group or an explicit unresolved bucket; grouping never relies on an image URL alone.

- **CAP-3**
  - **intent:** The importer creates or reconciles category aggregates and localizations idempotently.
  - **success:** Repeating the import does not create duplicate slugs or duplicate category-language localizations, and the source-ID mapping remains auditable.

- **CAP-4**
  - **intent:** The importer preserves Audio Books as a separate category while keeping canonical category compatibility valid.
  - **success:** Source rows `29`, `31`, `38`, and `163` become one distinct `audio-books` category with `STORY` canonical type and four localizations; no canonical `AUDIO_STORY` category row is written.

- **CAP-5**
  - **intent:** The importer resolves category images into v2 media references or reports unresolved image work.
  - **success:** A localization receives `imageMediaId` only after valid backend-mediated asset registration, and token-bearing source URLs are absent from persisted import artifacts.

## Constraints

- Canonical category types are `STORY`, `MEDITATION`, and `LULLABY`; source `AUDIO_STORY` is retained as provenance and discovery meaning, not as a persisted canonical type.
- Category grouping is represented by one aggregate and one stable slug with language-scoped localizations; the current model has no parent, family, or group-ID field.
- Category curation accepts content whose canonical content type matches the category type. This CSV contains no category-content links or display orders, so curation is a separate phase.
- Audio Books curation must select `STORY` localizations that are audio-ready. The current category validator checks canonical type, active state, and published localization but does not enforce narration readiness.
- Source `image_url` values are transient asset inputs only; v2 category localizations store positive `imageMediaId` values.
- The source has no slug, premium, active, status, or published timestamp values. The importer must require an explicit policy or leave the corresponding records in a reviewable state.
- The provisional mapping contains 33 candidate category aggregates and 105 localizations; ambiguous editorial groups remain open until reviewed.

## Non-goals

- Restoring `AUDIO_STORY` as a canonical content, category, or asset-processing type.
- Creating or migrating legacy independent `AUDIO_STORY` content records.
- Importing category-content membership or display order from this CSV.
- Inferring multilingual groups solely from names, image filenames, or reused image assets.
- Publishing categories, assigning premium access, or choosing active-state policy without explicit import policy.
- Redesigning the CMS category UI or implementing the broader story-audio experience contract.

## Success signal

The reviewed CSV can be dry-run and imported into v2 as deterministic category aggregates and localizations, with all 105 source rows accounted for, no unsupported canonical type written, no duplicate category-language records on rerun, and an auditable source-to-v2 mapping. Audio Books appears as its own category identity while only audio-ready STORY localizations are eligible for its later curation.

## Assumptions

- The current scope creates category aggregates and localizations; category-content curation follows after content IDs and audio readiness are known.
- The four Audio Books rows are one multilingual category candidate, not four independent categories.
- Token-bearing Firebase image URLs can be used transiently to obtain assets but are not committed, persisted, or emitted in reports.

## Open Questions

- Should imported localizations start as `DRAFT`, or should the importer publish them with supplied publication timestamps?
- What are the intended default values for `premium` and `active` for this public-category import?
- Are the provisional 33 group mappings accepted, especially Favorites, Parent Favorites, Editors' Pick, Sharing/Solidarity, Imagination/Creativity, and the single-language categories?
- Should this rollout only create the Audio Books category, or also validate and attach audio-ready STORY content?
