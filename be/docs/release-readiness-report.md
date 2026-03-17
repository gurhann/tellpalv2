# Release Readiness Report

## Scope

This report records the backend release-readiness result at the end of `F14`.

Execution date: `2026-03-17`

## Commands Run

- `cd be && ./mvnw test`
- `cd be && ./mvnw verify`

## Results

- `./mvnw test`
  - result: `BUILD SUCCESS`
  - finished at: `2026-03-17T20:36:41+03:00`
  - suite summary: `Tests run: 124, Failures: 0, Errors: 0, Skipped: 0`
- `./mvnw verify`
  - result: `BUILD SUCCESS`
  - finished at: `2026-03-17T20:39:04+03:00`
  - surefire summary: `Tests run: 124, Failures: 0, Errors: 0, Skipped: 0`
  - failsafe summary: `ApplicationModulesDocumentationIT` passed
  - packaging: repackaged Boot jar created under `be/target/`
  - docs generation: Modulith docs regenerated under `be/docs/modulith/`

## Quality Gates Covered

- unit tests
- property tests
- integration tests with Testcontainers and PostgreSQL
- Modulith boundary verification
- packaged Boot jar creation
- generated Modulith documentation

## Deployment Preconditions

- Production secrets must be set:
  - `TELLPAL_ADMIN_JWT_SECRET`
  - `TELLPAL_FIREBASE_PROJECT_ID`
  - `TELLPAL_FIREBASE_CREDENTIALS_PATH`
  - `TELLPAL_REVENUECAT_AUTHORIZATION_HEADER`
  - `TELLPAL_DB_URL`
  - `TELLPAL_DB_USERNAME`
  - `TELLPAL_DB_PASSWORD`
- PostgreSQL 15 must be available in the target environment.
- Flyway migrations must validate against an empty or compatible database.
- Firebase and RevenueCat config validation must stay fail-fast outside
  `local` and `test`.
- CI must have Docker access because Testcontainers is part of the verification
  chain.

## Known Non-Blocking Notes

- JDK 25 still emits Mockito/ByteBuddy dynamic-agent warnings.
- Those warnings did not fail `test` or `verify`.

## Remaining Watch Items

- FFmpeg and real media optimization binaries still need deployment-environment
  validation.
- Firebase service-account rotation and secret-manager wiring still belong to the
  deployment pipeline.

## Open Decisions

- None at `F14` close. Remaining items are operational follow-up, not release
  blockers for the current backend codebase.
