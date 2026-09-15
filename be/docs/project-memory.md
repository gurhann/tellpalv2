# Project Memory

Active backend defaults; follow the read order and shared rules in root `AGENTS.md`.

## Canonical Documents

- `AGENTS.md`
- `architecture.md`
- `be/docs/admin-api-rules.md`
- `be/docs/backend-architecture-guide.tr.md`
- `be/docs/code-documentation-guidelines.md`
- `ops/railway/README.md`
- `be/docs/adr/`

## Active Architectural Decisions

- `ADR-0001`: the backend is a Spring Modulith-style modular monolith and modules interact through APIs, events, and IDs.
- `ADR-0002`: each top-level module owns its `domain` and `infrastructure` packages and may expose only `api`.
- `ADR-0003`: public contracts and use-case flows use concise English documentation.
- `ADR-0004`: `shared` is a small stable kernel, not a dumping ground.
- `ADR-0005`: REST endpoints use OpenAPI-based documentation with operation-level summaries, auth, and core error responses.
- `ADR-0006`: story-page illustrations are localization-scoped and resolved from `story_page_localizations`, not page roots.
- `ADR-0007`: category type aligns with curated content type and no longer supports legacy `CONTENT` or `PARENT_GUIDANCE` values.
- `ADR-0008`: asset runtime uses real Firebase Storage with one bucket, prefix isolation, and backend-mediated CMS uploads/previews.
- `ADR-0009`: registry reads must paginate and filter in the database; `Specification` covers row-local predicates while aggregate readiness needs a dedicated projection/read model.
- `ADR-0010`: STORY source textless covers, localized reading covers, and shared listening covers are separate ownership concepts; LULLABY additionally owns a distinct content-scoped static listing cover while its listening cover remains the playback/detail asset.
- `ADR-0011`: lullaby instrument selections are content-scoped ordered links to a language-independent catalog; display names are resolved from catalog localization rows and are not copied to content localizations.
- `ADR-0012`: canonical content and category types are STORY, MEDITATION, and LULLABY; AUDIO_STORY is a STORY narration experience, not a persisted canonical type. Legacy independent audio-story import is a separate future migration.
- `ADR-0014`: staged MEDITATION localizations may omit body text only as DRAFT/PENDING; publication and archive require non-blank body text, and the admin registry exposes missing body as ACTION_REQUIRED.

## Coding and Documentation Defaults

- `ADR-0013`: follow the language policy in root `AGENTS.md` (Turkish communication, English technical documentation).
- `ADR-0015`: keep unique safeguards, load deployment details for deployment work, and archive completed
  CMS tasks with stable links. Blind Hunter requires evidence-backed findings with no minimum count;
  all review layers remain enabled. See `be/docs/adr/ADR-0015-agent-context-efficiency.md`.
- Javadoc is required for public module contracts, public application services, and non-obvious domain rules.
- Comments stay behavior-oriented and avoid boilerplate restatement of names.
- Method-level Javadoc is placed above annotations for consistency.
- `TODO` and `FIXME` comments require owner, reason, and exit condition.
- REST controllers use OpenAPI annotations for summaries, auth requirements, and core `ProblemDetail` responses.
- API docs are disabled by default and enabled only in local or explicitly configured environments.

## Agent Model Selection Defaults

- For all planning, implementation, verification, and review work, use the appropriate BMad skill;
  small isolated fixes are the only exception.

- Use `gpt-5.6-luna` as the default model for BMad planning, implementation, testing, and review
  workflows in this repository.
- Before starting a BMad implementation, state the selected model and reassess the choice when the
  work's risk or scope changes materially.
- Alert the user and obtain explicit confirmation before switching to `gpt-5.6-terra` for a
  production-impacting migration, proven concurrency/transactional defect, cross-module
  architectural change, broad refactor, or a persistent test/debugging failure that Luna cannot
  resolve safely.
- Alert the user and obtain explicit confirmation before switching to `gpt-5.6-sol` for a
  security-sensitive or irreversible production decision, a high-stakes recovery/data-integrity
  incident, or a complex architecture decision that remains unresolved after Terra-level work.
- Model escalation does not weaken the BMad workflow: the same approval, verification, review, and
  local-commit gates remain in force.

## Module Interaction and Testing Defaults

Follow root `AGENTS.md` for boundaries and testing. Modules must not depend on another module's
`domain` or `infrastructure`; exposed contracts belong in `api`. Keep shared types small, stable,
and broadly reusable (ADR-0001, ADR-0002, ADR-0004).

## Local Development Defaults

- The backend does not auto-seed sample CMS content for local environments.
- `be/docs/admin-api-rules.md` is the canonical CMS admin validation and precondition reference for frontend and local seed work.
- When preparing local CMS sample data, create records through admin APIs or controlled SQL and follow the content localization validation rules documented in `be/docs/bootstrap-notes.md`.
- Local CMS verification is easier with a mixed sample set: one story with story pages and localizations, one active non-story item, and one inactive item.
- Seed/curation data must follow ADR-0006/0007/0012: attach page illustrations to localization rows,
  use STORY/MEDITATION/LULLABY categories, and match curated content to its category type.
- Asset runtime now expects real Firebase Storage credentials in local development. Local and production share one bucket, and environment isolation happens through the configured path prefix (`local` or `prod`).
- Asset upload and generated processing paths are prefix-aware. New manual uploads land under `/{prefix}/manual/...`, and generated variants/packages land under `/{prefix}/content/...`.
- Lullaby playback and instrument selections are shared at content level. Instrument selection uses stable catalog codes and zero-based order; `MUSICIAN` contributors remain a separate concept.
- CMS asset upload and preview are backend-mediated. Browsers send multipart uploads to the admin
  API and render short-lived backend preview URLs; Firebase/GCS signed upload and download URLs
  remain only for deprecated compatibility and mobile/public delivery needs.

## Deployment Defaults

For deployment work, read `ops/railway/README.md` and keep it current when behavior changes.
It owns production topology, Docker/start commands, Firebase credentials and prefix isolation,
CMS hosting, GitHub deploy triggers/secrets, temporary admin bootstrap, RevenueCat authorization,
and every-deploy checks (tests/build, service status, health, CMS load, CORS, and startup logs).
Follow root `AGENTS.md` for pre-deploy checks and environment-driven configuration safeguards.

## Review Red Flags

- new public contracts without documentation
- durable policy changes without ADR or project-memory updates
- comments that narrate obvious code instead of exposing intent
- new cross-module dependencies into internal packages
- registry endpoints that fetch every aggregate and then filter, sort, or paginate in application memory
- deploy changes that bypass `ops/railway/README.md`, hard-code Railway/Firebase secrets, or leave bootstrap credentials configured after use
