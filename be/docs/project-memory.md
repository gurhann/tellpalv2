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

- `ADR-0019`: Story 1.1 Library home uses per-language opt-in category order and explicit content-language mapping, one preview default of five, lightweight module APIs, verified real LISTING_COVER revisions with captured attempt/lease/source/profile guards, and local/test Firebase UID admission. The approved public edge is `user` → `category::api`. CMS `/categories/home` preserves card order; the isolated Flutter entry follows ADR-0018 and retains the PoC. Runtime evidence and remaining gates live in `library-1-1-evidence.md`; test audience is not production authorization.

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
- `ADR-0016`: TellPal CMS is a web-only admin surface; supported visual QA targets are laptop and desktop browser widths (`1280` and `1440`), while mobile/tablet CMS fidelity is out of scope unless explicitly revisited.
- `ADR-0017`: localized story-page image renditions are stored under immutable source/profile revisions and exposed to allowlisted Firebase testers through a versioned manifest; existing page audio and legacy delivery remain unchanged.

## Coding and Documentation Defaults

- `ADR-0018`: Flutter development uses the installed `RobertAlvv/flutter-skills` suite.
  `flutter-scalable-app` and its references govern client architecture: feature-first Clean
  Architecture, BLoC/Cubit, `auto_route`, and `get_it` + `injectable`. Use the relevant auditors
  for reviews. This client policy complements BMad workflows and does not change backend
  architecture or imply that existing Flutter code has already been migrated.
  The mobile spine at `_bmad-output/planning-artifacts/architecture/architecture-tellpalv2-2026-09-25/ARCHITECTURE-SPINE.md`
  and `epics-mobile.md` now reflect this choice, replacing their earlier Riverpod/go_router
  selections. AD-12 fixes DI/disposal ownership; all story readiness and backend-contract gates
  remain open until verified separately.

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

- `ADR-0020`: BMad planning, implementation, testing, verification, and review use the model
  selected for the current session. Subagents inherit it; omit explicit model overrides unless
  the user requests a different model for that work.
- Before starting a BMad implementation, state the selected model when its identity is available;
  otherwise state that it is inherited from the session without guessing a model name.
- Scope, risk, or debugging difficulty does not trigger an automatic model change. Apply a new
  user selection or explicit model request without asking for another confirmation.
- Model selection does not weaken the BMad workflow: the same approval, verification, review, and
  local-commit gates remain in force. Keep repository defaults outside installer-managed skills.

## Module Interaction and Testing Defaults

Follow root `AGENTS.md` for boundaries and testing. Modules must not depend on another module's
`domain` or `infrastructure`; exposed contracts belong in `api`. Keep shared types small, stable,
and broadly reusable (ADR-0001, ADR-0002, ADR-0004).

## Local Development Defaults

- Always use existing Firebase UID `jLRypTmoBrhhX21HsDJojurcKTC3` for real authenticated development and QA unless the user explicitly changes the selection. Read `tools/mobile-test-account.json`; verify existence/enabled status before obtaining temporary sessions and store tokens outside the repository. This user-approved default does not change production access, account claims/subscriptions, synthetic unit fixtures, or the backend's empty-by-default test allowlist (ADR-0019).
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
