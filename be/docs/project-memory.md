# Project Memory

## Purpose

This document is the fast entry point for active engineering defaults in TellPal V2 backend work.
It summarizes the current decisions that engineers and agents should know before changing code.

Read order:

1. root `AGENTS.md`
2. this file
3. relevant files under `be/docs/adr/`
4. `architecture.md`

## Canonical Documents

- `AGENTS.md`
- `architecture.md`
- `be/docs/admin-api-rules.md`
- `be/docs/backend-architecture-guide.tr.md`
- `be/docs/code-documentation-guidelines.md`
- `be/docs/adr/`

## Active Architectural Decisions

- `ADR-0001`: the backend is a Spring Modulith-style modular monolith and modules interact through APIs, events, and IDs.
- `ADR-0002`: each top-level module owns its `domain` and `infrastructure` packages and may expose only `api`.
- `ADR-0003`: public contracts and use-case flows use concise English documentation.
- `ADR-0004`: `shared` is a small stable kernel, not a dumping ground.
- `ADR-0005`: REST endpoints use OpenAPI-based documentation with operation-level summaries, auth, and core error responses.
- `ADR-0006`: story-page illustrations are localization-scoped and resolved from `story_page_localizations`, not page roots.
- `ADR-0007`: category type aligns with curated content type and no longer supports legacy `CONTENT` or `PARENT_GUIDANCE` values.
- `ADR-0008`: asset runtime uses real Firebase Storage with one bucket, prefix isolation, and direct browser uploads.

## Coding and Documentation Defaults

- Durable code comments, ADRs, and project-memory content are written in English.
- Javadoc is required for public module contracts, public application services, and non-obvious domain rules.
- Comments stay behavior-oriented and avoid boilerplate restatement of names.
- Method-level Javadoc is placed above annotations for consistency.
- `TODO` and `FIXME` comments require owner, reason, and exit condition.
- REST controllers use OpenAPI annotations for summaries, auth requirements, and core `ProblemDetail` responses.
- API docs are disabled by default and enabled only in local or explicitly configured environments.

## Module Interaction Defaults

- Cross-module interaction happens through `api` packages, events, or identifiers.
- Modules do not depend on another module's `domain` or `infrastructure` package.
- Shared types remain small, stable, and broadly reusable.

## Testing Defaults

- Prefer tests that validate business rules, persistence constraints, and module boundaries.
- Use Maven wrapper commands from `be/`.
- Add integration coverage when changes affect persistence, transactions, migrations, or REST flows.

## Local Development Defaults

- The backend does not auto-seed sample CMS content for local environments.
- `be/docs/admin-api-rules.md` is the canonical CMS admin validation and precondition reference for frontend and local seed work.
- When preparing local CMS sample data, create records through admin APIs or controlled SQL and follow the content localization validation rules documented in `be/docs/bootstrap-notes.md`.
- Local CMS verification is easier with a mixed sample set: one story with story pages and localizations, one active non-story item, and one inactive item.
- Story-page illustrations are now locale-scoped. Local seed data and CMS assumptions must attach illustration assets to `story_page_localizations`, not `story_pages`.
- Category type is now content-aligned. Category seed and curation test data must use one of `STORY`, `AUDIO_STORY`, `MEDITATION`, or `LULLABY`, and curated content must match the selected category type.
- Asset runtime now expects real Firebase Storage credentials in local development. Local and production share one bucket, and environment isolation happens through the configured path prefix (`local` or `prod`).
- Asset upload and generated processing paths are prefix-aware. New manual uploads land under `/{prefix}/manual/...`, and generated variants/packages land under `/{prefix}/content/...`.

## Review Red Flags

- new public contracts without documentation
- durable policy changes without ADR or project-memory updates
- comments that narrate obvious code instead of exposing intent
- new cross-module dependencies into internal packages
