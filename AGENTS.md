# Repository Guidelines

## Structure and Read Order

Backend code, builds, and docs belong under `be/`; CMS lives under `cms/`.
Backend paths: `src/main/java` (code), `src/main/resources` (config/Flyway),
`src/test/java` (tests), `docs` (design), and `pom.xml` (build), relative to `be/`.
Team skills live under `.codex/skills`.

Before planning or implementing, read in order:

1. This file.
2. `cms/AGENTS.md` for CMS work.
3. `be/docs/project-memory.md` for backend standards or architecture work.
4. Relevant ADRs under `be/docs/adr/`.
5. `architecture.md` and relevant design docs. It is the canonical backend architecture:
   consult it before backend design, planning, schema, boundary, or implementation decisions.

For CMS layout/interaction changes use `ui-ux-pro-max`; for shared primitives/frontend
architecture use `senior-frontend`; for TellPal UI review/regression use `cms-ui-guardrails`.
Follow the additional skill triggers and safeguards in `cms/AGENTS.md`.

## Standards and Language

Follow `standards/` for comments/Javadoc, REST documentation, ADRs, project memory, and bootstrap guidance.
When changing durable policies, update project memory and the relevant ADR using
`apply-project-standards`. For REST controller changes, read and follow
`standards/rest-api-documentation-standard.md`.

Use Turkish for conversations, questions, progress updates, and final summaries; English for
new long technical documents, ADRs, project memory, code comments, and API documentation.
Explicit language requests take precedence. Preserve existing Turkish documents during routine
edits; translate on request or agreed replacement. Product localization keeps its audience language
(ADR-0013). Keep rules concise without dropping unique safeguards; read deployment details when
working on deployment and archived tasks only for relevant history (ADR-0015).

## Build and Test Commands

Use the Maven wrapper from `be/`:

- `docker compose up -d postgres`: local database.
- `./mvnw spring-boot:run`: backend with default `local` profile.
- `./mvnw test`: full test suite; `./mvnw verify`: full verification including integration checks.
- `./mvnw flyway:migrate`: migrations against the configured database.
- `./mvnw spring-modulith:document`: module docs, if the plugin is configured.

From `cms/`: `npm run build` (production), `npm run test` (Vitest),
`npm run test:e2e` (Playwright), `npm run test:e2e:visual` (visual regression).
Backend PRs and main pushes run `./mvnw verify` via `.github/workflows/backend-verify.yml`.

## Deployment

Railway is the production target. Before deployment work, read `ops/railway/README.md` for
service topology, environment variables, commands, admin bootstrap, and verification; update it
when deployment behavior changes. Use `ops/railway/deploy.ps1` from the root for manual operations.
Pushes to `main` affecting backend, CMS, Railway ops, or the deploy workflow trigger production
through `.github/workflows/railway-deploy.yml` (requires `RAILWAY_TOKEN`).
Before production deploys, run backend `./mvnw test` for backend changes and CMS `npm run build`
for CMS changes; follow the runbook's complete production verification requirements.
Keep deployment environment-driven: never hard-code domains, Firebase credential paths,
database URLs, admin credentials, or secrets. Local and production share Firebase project/bucket;
isolate storage through configured prefixes.

## Coding, Security, and Verification

- Treat each top-level `com.tellpal.v2` package as a Spring Modulith module. Interact through
  application APIs, events, or IDs; no cross-module internal imports/entity references or cycles.
- Use constructor injection, small methods, and explicit DDD entities/services. No field injection
  or business logic in controllers/infrastructure adapters; services orchestrate use cases.
- Java packages: lowercase; DB tables/columns: `snake_case`; enums: `UPPER_SNAKE_CASE`;
  Flyway: `V{number}__description.sql`.
- Never commit secrets, tokens, or private download URLs.
- Use JUnit 5, jqwik for invariants, and Testcontainers/PostgreSQL for integration tests. Prefer
  business-rule, persistence-constraint, and module-boundary coverage. Name tests descriptively
  and place them beside their modules. Add integration coverage for Flyway, repositories,
  transactions, and REST API changes.
- Use short conventional commit subjects. PRs include a change summary, impacted modules/packages,
  migration notes when applicable, and test evidence.
