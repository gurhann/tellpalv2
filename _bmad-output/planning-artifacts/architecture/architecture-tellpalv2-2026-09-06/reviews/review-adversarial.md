# Adversarial Review — Localized Story Audio and Shared Lullaby Playback

## Verdict

**Revise before implementation.** The spine establishes the intended ownership boundary, but it leaves enough critical representation and transition decisions unspecified that a compliant implementation can either leak locale-level fields into a lullaby, make all story audio indistinguishable from page narration, or break the current processing and public API contracts.

## What an implementation could do while still following the words

### P0 — Two meanings may occupy the same localization audio fields

AD-2 adds a localization-level full-story narration while retaining `StoryPageLocalization.audioMediaId`. The existing `ContentLocalization.audio_media_id`, `duration_minutes`, and `processing_status` are already in use by non-story single-audio content. The spine does not state whether story narration reuses these columns, introduces a `StoryNarration` child, or which status determines ordinary story publication versus audio-experience readiness.

A developer could reuse `ContentLocalization.audioMediaId` for STORY and pass all stated validations, but then it is unclear how existing page processing and full-story processing coexist. Conversely, a child table could be added but public queries might still read `ContentLocalization.processingStatus`. Require an explicit owned shape and names (prefer a `story_localization_narrations` one-to-one child with `audio_media_id`, duration, and status), and state that ordinary STORY visibility is independent of this child.

### P0 — Processing scope cannot be represented by the current asset contract

AD-5 requires `CONTENT` jobs without a language and `LOCALIZATION` jobs with language. The current `asset_processing` table has a non-null `language_code`, a unique `(content_id, language_code)`, and a foreign key to `(content_id, language_code)` in `content_localizations`; every public command/event also requires `LanguageCode`. Merely adding a scope enum without a new key and event identity cannot schedule, start, retry, or complete a shared lullaby job.

Specify the migration and contract mechanics: either split shared playback processing into a separate table/API or make language nullable with mutually-exclusive scoped check constraints and a scope-safe unique index. Every command, record, event, worker lease operation, and `ContentAssetBundleApi` lookup needs a stable processing identity that is not `(contentId, languageCode)`.

### P0 — A global lullaby failure is allowed to change localization visibility through an unrelated status

AD-3 says a lullaby localization manages publication state; AD-6 says shared playback failure hides all locale projections without changing editorial title or publication state. The existing mobile visibility rule requires `ContentLocalization.processing_status = COMPLETED`. If unchanged, a publication or localized asset update can cause false hiding, while the shared failure cannot be expressed without setting every localization status to FAILED (contrary to AD-6).

Define the precise predicate: `localization.status == PUBLISHED && sharedPlayback.status == COMPLETED` for LULLABY, and make it independent from legacy localization processing status. Also define what happens when audio, cover, duration, or instruments are edited after completion: invalidate shared readiness atomically before the new result is published.

### P1 — Lullaby localization field prohibition is only prose, not enforceable ownership

AD-3 forbids description, body, cover, audio, and duration but the existing table has all except no explicit type-scoped constraints. A CMS-only restriction permits API clients, imports, or direct application commands to persist forbidden localization data. It also does not say how existing required/non-null localization fields are migrated or whether other canonical types retain them.

Add type-aware domain validation on every create/update command and database enforcement that can see the content type (trigger is acceptable in PostgreSQL). State null/retention migration policy for existing LULLABY localization columns and ensure response mappers never fall back to them.

### P1 — Asset validity lacks lifecycle and authorization rules

The spine requires media-type validation and positive IDs, but not that assets exist, are usable/processed, belong to the expected tenant/prefix, or can be replaced safely. This permits references to an IMAGE as audio after an upload state transition, or deletion/replacement of an asset still referenced by published content.

`asset.api` needs explicit read/validation contracts for `AUDIO` and `IMAGE`, including allowed asset states, reference integrity/deletion behavior, and a delivery-resolution failure outcome. Duration source must be authoritative: editor-entered, extracted from audio, or reconciled with a tolerance.

### P1 — `InstrumentCatalog` has no operational data model or locale fallback policy

AD-4 says stable code and localized labels, while the companion contract still leaves backend-versus-mobile localization open. A compliant implementation could store a single Turkish `name`, emit it as `displayName` for English, and claim it is a managed label. It could also delete/deactivate an instrument referenced by a published lullaby.

Close this decision before build: select backend-resolved labels or client-owned translations; define catalog tables/unique code, enabled state, per-language label uniqueness, fallback behavior, and whether referenced instruments can be retired rather than deleted. Require `displayOrder` uniqueness per playback, a positive/contiguous ordering policy, and a foreign key to the catalog.

### P1 — Contributor globality is asserted but not enforced for musicians

The existing `content_contributors` model supports both global and language-scoped rows. AD-3 says `MUSICIAN` is global, but does not prohibit a localized `MUSICIAN` assignment. Public reads could then accidentally choose a locale-specific musician, contradicting the shared playback promise.

Define and enforce `MUSICIAN.language_code IS NULL` for LULLABY (or globally, if intended), plus ordering, duplicate-assignment, and response ordering rules.

### P1 — Discovery projection lacks locale, grouping, and pagination semantics

CAP-2 permits the same content ID in READING and AUDIO_STORY surfaces. AD-6 does not state whether one search response may emit two cards for one `(contentId, languageCode)`, how the UI groups them, whether audio rays are category-filtered, or how pagination/counts avoid double counting. A query can technically return `canonicalType + experienceType` while presenting duplicate cards to the wrong surface.

State projection identity as `(contentId, languageCode, experienceType)`, define which endpoints/groups intentionally produce both, apply audio eligibility at query time, and retain existing sort/pagination/count semantics. Include exact compatibility behavior for old `type: AUDIO_STORY` endpoints and their removal/versioning date; the current deferred item is too late for implementers.

### P1 — Category contraction is underspecified for schema and curated collections

AD-8 detects persisted `AUDIO_STORY` rows, but the existing `contents`, categories, category-content links, `AssetProcessingContentType`, Java enums, and possibly external filters all contain the enum. "Fails otherwise" can make a fresh migration fail if seeded/reference data exists, without identifying which row class must be absent or whether category families must be deleted, rejected, or retained as presentation rails.

Give an ordered, reversible Flyway plan: preflight query/report, remove or constrain `AUDIO_STORY` references only when zero business rows exist, migrate check constraints/enums/API validation in coordinated releases, and retain/replace category routes explicitly. Do not imply that a Flyway migration can safely run an application-level validation and then partially contract schema.

### P2 — Aggregate boundaries and transaction/event choreography are ambiguous

The ER diagram calls `LullabyPlayback` an aggregate-owned child yet delegates processing transitions from `asset` back to `content`. It does not say whether updates to playback and selection schedule a job in the same transaction, how stale completion events are rejected after a replacement, or how eventual-consistency windows affect CMS/public reads.

Add version/source-asset correlation to processing commands/events and an outbox/idempotency rule. Completion must update only the playback revision it processed; replacing audio/cover should invalidate delivery state before a stale worker result can be observed.

## Required acceptance tests absent from the spine

1. A published STORY with no full narration remains readable while an audio-ready TR localization is discoverable and EN is not.
2. Page narration audio and full-story narration coexist for one localization without overwriting either status or media reference.
3. Replacing a completed lullaby asset immediately hides all locale projections until the replacement processing revision completes; a stale completion cannot re-publish it.
4. A shared lullaby job completes/fails/retries without any `content_localizations.language_code` processing identity, and only its shared readiness changes.
5. A LULLABY localization mutation containing description, cover, audio, duration, or a localized musician is rejected below the CMS layer.
6. Duplicate, disabled, or deleted instruments cannot produce an invalid published playback; labels follow the selected locale/fallback rule.
7. Search/ray pagination and legacy audio endpoint compatibility preserve intended card counts and identities.
8. Migration has zero canonical `AUDIO_STORY` content, category, association, and processing references before constraints/enums are contracted, while no import behavior is introduced.
