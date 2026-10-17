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

## Flutter Development

For Flutter planning, implementation, refactoring, and review (including `app/mobile/`),
use the installed `RobertAlvv/flutter-skills` skills. Before architectural decisions or
implementation, read `.codex/skills/flutter-scalable-app/SKILL.md` and its relevant
references. Its feature-first Clean Architecture,
BLoC/Cubit, `auto_route`, and `get_it` + `injectable` patterns are required.
Use `architecture-auditor` for architectural reviews and the relevant specialized auditor
for state management, widget/runtime performance, atomic design, testability, or CI/CD
from `.codex/skills/`.
See `be/docs/adr/ADR-0018-flutter-skill-architecture-policy.md` for the skill inventory,
installation revision, and scope. Apply these skills alongside the existing BMad workflow;
Flutter technical choices follow this policy. Record any explicitly approved deviation in
the ADR and project memory. Skill installation alone does not migrate existing app code.

## Standards and Language

Follow `standards/` for comments/Javadoc, REST documentation, ADRs, project memory, and bootstrap guidance.
When changing durable policies, update project memory and the relevant ADR using
`apply-project-standards`. For REST controller changes, read and follow
`standards/rest-api-documentation-standard.md`.

BMad workflows and their subagents use the model selected for the current session. Omit model
overrides when delegating unless the user explicitly requests a different model. Apply new user
selections without an additional confirmation; see `be/docs/adr/ADR-0020-bmad-session-model-selection.md`.

İletişim ve bundan sonra üretilen tüm belgeler Türkçe olmalıdır. Bu kural teknik belgeler,
spec ve planlar, ADR'ler, proje hafızası, raporlar, kullanım/operasyon rehberleri, API açıklamaları
ve kod yorumları/Javadoc için de geçerlidir. Açık, sade ve doğrudan yaz; kısa cümleler kullan,
gereksiz ayrıntı ve jargondan kaçın. Anlamı netleştiren yerleşik teknik terimler İngilizce kalabilir;
gerektiğinde ilk kullanımda kısa Türkçe açıklama ekle. Kod kimliklerini, API alan adlarını, komutları,
dosya yollarını ve makine tarafından okunan anahtarları çevirme. Bu tercih BMad iş akışları ve
alt ajanlar dahil tüm belge üretimine uygulanır; şablonlardaki İngilizce çıktı varsayılanından önceliklidir.
Kullanıcı belirli bir çıktı için başka dil isterse o isteği uygula. Mevcut belgeleri topluca çevirme;
yeni ve yeniden yazılan bölümleri Türkçe üret. Ürün içerikleri ve çevirileri hedef kitlenin dilini
korur (ADR-0013). Kuralları, gerekli korumaları eksiltmeden kısa tut. Dağıtım ayrıntılarını dağıtım
işlerinde, arşivlenmiş görevleri yalnız ilgili geçmiş gerektiğinde oku (ADR-0015).

## Build and Test Commands

For real authenticated development and QA flows, always use Firebase UID
`jLRypTmoBrhhX21HsDJojurcKTC3` unless the user explicitly changes it. The canonical
selection is `tools/mobile-test-account.json` (ADR-0019). Keep session tokens outside
the repository; this default does not enable production access or replace synthetic unit fixtures.

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
