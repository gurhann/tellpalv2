# Repository Guidelines

## Project Structure & Module Organization
This repository is now organized for multiple apps. The Spring Boot backend lives under `be/`, so
all backend code, build files, and backend docs should stay there.

The CMS frontend lives under `cms/`. Any work that changes frontend layout, shared UI primitives,
route composition, or screenshot regression coverage must also follow `cms/AGENTS.md`.

Key backend paths:

- `be/src/main/java`: application code
- `be/src/main/resources`: config and Flyway migrations
- `be/src/test/java`: JUnit, jqwik, and integration tests
- `be/docs`: schema and database design docs
- `be/pom.xml`: Maven build
- `.codex/skills`: team-shared Codex skills for backend work

Treat each top-level package under `com.tellpal.v2` as a Spring Modulith module.

Shared project skills currently include:

- `write-flyway-migration`
- `write-jpa-entity`
- `write-spring-service`
- `write-rest-controller`
- `write-jqwik-property-test`
- `write-testcontainers-integration-test`
- `spring-modulith-boundary-check`
- `task-decomposition`
- `run-maven-tests`
- `apply-project-standards`
- `cms-ui-guardrails`

## Standards Read Order
Before planning or implementing work, read documents in this order when they exist:

1. `AGENTS.md`
2. `cms/AGENTS.md` when the task touches `cms/`
3. `be/docs/project-memory.md` when the task touches backend standards or architecture
4. relevant files under `be/docs/adr`
5. `architecture.md` and other project-specific design docs

## Standards Package
This repository follows a reusable engineering standards package under
`standards/`.

That package defines:

- comment and Javadoc rules
- REST API documentation rules
- ADR format
- project-memory format
- agent bootstrap guidance for future projects

When changing a durable engineering policy or architectural default, update
`be/docs/project-memory.md` and the relevant ADR.

When adding or changing REST controllers, follow the OpenAPI policy in
`standards/rest-api-documentation-standard.md`.

## Build, Test, and Development Commands
Run commands from `be/`:

- `cd be && ./mvnw test`: run the full test suite
- `cd be && ./mvnw verify`: run full verification, including integration checks
- `cd be && ./mvnw flyway:migrate`: validate migrations against the configured database
- `cd be && ./mvnw spring-modulith:document`: regenerate Modulith docs if the plugin is configured

Use the Maven wrapper instead of a system Maven install when possible.

## Deployment Guidelines

Railway is the canonical production deploy target for this project. Follow
`ops/railway/README.md` for service topology, environment variables, deploy
commands, admin bootstrap, and verification steps.

Before production deploys, run the relevant local checks:

- `cd be && ./mvnw test` for backend changes
- `cd cms && npm run build` for CMS changes

Keep deployment behavior environment-driven. Do not hard-code Railway domains,
Firebase credential paths, database URLs, admin credentials, or secrets in code.
Local and production currently share the same Firebase project and bucket; storage
isolation is handled through the configured path prefix.

## Coding Style & Naming Conventions
Use constructor injection, keep methods small, and avoid field injection. Model DDD concepts explicitly: entities represent domain concepts, services orchestrate use cases, and controllers stay thin.

Conventions:

- Java packages, tables, and columns: `snake_case` for DB, lowercase package names for Java
- Flyway files: `V{number}__description.sql`
- enum values: `UPPER_SNAKE_CASE`
- cross-module interaction: application APIs, events, or IDs, not internal classes or entity references

## Testing Guidelines
Use JUnit 5 for unit tests, jqwik for invariant/property tests, and Testcontainers with PostgreSQL for integration tests. Prefer tests that verify business rules, persistence constraints, and module boundaries.

Keep test names descriptive and place tests beside the module they validate. Add integration coverage when changes affect Flyway, repositories, transactions, or REST APIs.

## Commit & Pull Request Guidelines
Use short conventional commit subjects such as `feat: add content localization endpoint` or `fix: enforce category ordering`.

Pull requests should include:

- a short summary of the backend change
- impacted modules or packages
- migration notes if `be/src/main/resources/db/migration` changed
- test evidence, for example `./mvnw test` or `./mvnw verify`

## Security & Architecture Notes
Do not commit secrets, tokens, or private download URLs. Preserve Spring Modulith boundaries: no cross-module internal imports, no circular dependencies, and no business logic in controllers or infrastructure adapters.

Use `architecture.md` at the repository root as the canonical backend architecture reference. For
backend design, planning, schema, module boundary, and implementation decisions, consult this
document first and keep new work aligned with it.

For CMS frontend work:

- read `cms/AGENTS.md`
- use `ui-ux-pro-max` for layout and interaction changes
- use `senior-frontend` for shared primitive and frontend architecture changes
- use `cms-ui-guardrails` for TellPal CMS-specific UI review and regression rules
