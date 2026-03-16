# Repository Guidelines

## Project Structure & Module Organization
This repository is now organized for multiple apps. The Spring Boot backend lives under [`be/`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be), so all backend code, build files, and backend docs should stay there.

Key backend paths:

- [`be/src/main/java`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/src/main/java): application code
- [`be/src/main/resources`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/src/main/resources): config and Flyway migrations
- [`be/src/test/java`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/src/test/java): JUnit, jqwik, and integration tests
- [`be/docs`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/docs): schema and database design docs
- [`be/pom.xml`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/pom.xml): Maven build
- [`.codex/skills`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/.codex/skills): team-shared Codex skills for backend work

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

## Build, Test, and Development Commands
Run commands from [`be/`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be):

- `cd be && ./mvnw test`: run the full test suite
- `cd be && ./mvnw verify`: run full verification, including integration checks
- `cd be && ./mvnw flyway:migrate`: validate migrations against the configured database
- `cd be && ./mvnw spring-modulith:document`: regenerate Modulith docs if the plugin is configured

Use the Maven wrapper instead of a system Maven install when possible.

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

Use [`architecture.md`](C:\github\tellpalv2\architecture.md) at the repository root as the canonical backend architecture reference. For backend design, planning, schema, module boundary, and implementation decisions, consult this document first and keep new work aligned with it.
