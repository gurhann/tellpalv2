# Release Checklist

## Before Release

- The repository worktree is clean.
- [tasks.md](/C:/github/tellpalv2/tasks.md) and
  [architecture.md](/C:/github/tellpalv2/architecture.md) match the backend
  state.
- `cd be && ./mvnw test` is green locally or in CI.
- `cd be && ./mvnw verify` is green locally or in CI.
- `be/docs/modulith/` contains current generated docs from the latest `verify`
  run.
- Flyway migrations run successfully on a disposable empty PostgreSQL database,
  for example `tellpal_v2_verify_release`.

## Required Runtime Secrets

- `TELLPAL_ADMIN_JWT_SECRET`
- `TELLPAL_FIREBASE_PROJECT_ID`
- `TELLPAL_FIREBASE_CREDENTIALS_PATH`
- `TELLPAL_REVENUECAT_AUTHORIZATION_HEADER`
- `TELLPAL_DB_URL`
- `TELLPAL_DB_USERNAME`
- `TELLPAL_DB_PASSWORD`

## Operational Checks

- `/actuator/health` returns healthy after startup.
- Admin auth failure and success paths produce request-correlated logs.
- RevenueCat webhook failures return sanitized problem details.
- Asset processing emits `asset_processing_transition` logs.
- `be/docs/modulith/components.puml` matches the current module graph.

## Database Checks

- Migration order and deployment sequencing are documented when a new migration
  is added.
- The `languages` seed remains present after migration.
- Admin role seed remains present after migration.
- Event and purchase idempotency constraints remain in place.
- Migration smoke validation is performed against a disposable database such as
  `tellpal_v2_verify_release`, not against a reused local schema.

## Release Gate

- GitHub Actions workflow
  [backend-verify.yml](/C:/github/tellpalv2/.github/workflows/backend-verify.yml)
  is green for the release candidate.
- Known non-blocking risks are recorded in
  [release-readiness-report.md](/C:/github/tellpalv2/be/docs/release-readiness-report.md).
