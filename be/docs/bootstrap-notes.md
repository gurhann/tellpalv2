# Bootstrap Notes

## Automatic Bootstrap

Flyway handles schema bootstrap and reference seed data during startup or
explicit migration runs.

The current seed set includes:

- `languages`: `tr`, `en`, `es`, `pt`, `de`
- `admin_roles`: `ADMIN`, `CONTENT_MANAGER`, `VIEWER`
- purchase lookup data: event types, stores, environments, and reason codes

The current backend does not seed a first admin user, sample content, or Firebase
test users.

## First Admin Bootstrap

There is no application-level bootstrap endpoint for the first admin user yet.
Create the initial admin account through a controlled SQL transaction after
generating a BCrypt hash with the configured strength (`TELLPAL_ADMIN_BCRYPT_STRENGTH`,
default `10`).

Example bootstrap transaction:

```sql
begin;

with created_admin as (
    insert into admin_users (username, password_hash, is_active)
    values ('bootstrap-admin', '$2a$10$replace_with_bcrypt_hash', true)
    returning id
)
insert into admin_user_roles (admin_user_id, admin_role_id)
select created_admin.id, admin_roles.id
from created_admin
join admin_roles on admin_roles.code = 'ADMIN';

commit;
```

After the first successful login, rotate the bootstrap password through the
normal controlled operator process.

## Local Bootstrap Sequence

1. `cd be && docker compose up -d postgres`
2. `cd be && ./mvnw flyway:migrate`
3. `cd be && ./mvnw spring-boot:run`

## Local CMS Sample Data Notes

The backend does not seed sample CMS content automatically. When local CMS
screens need realistic data, prefer creating it through admin APIs instead of
hard-coded frontend fixtures.

Current content localization validation rules to remember:

- `STORY` localizations must not include `bodyText`.
- Story narrative text belongs in `story_page_localizations`, not in
  `content_localizations`.
- `AUDIO_STORY` and `MEDITATION` localizations require `bodyText`.
- Non-story localizations require an `audioMediaId`.
- Local development can satisfy `audioMediaId` with `LOCAL_STUB`
  `ORIGINAL_AUDIO` assets in `media_assets`.

Recommended local CMS seed shape:

- one active `STORY` with at least two localizations and multiple story pages
- one active non-story content item with draft or processing localizations
- one inactive content item to validate admin read and filtering behavior

## Manual Bootstrap Expectations

- Non-local environments must provide `TELLPAL_ADMIN_JWT_SECRET`.
- Firebase service account credentials must stay outside the repository.
- RevenueCat authorization must be configured in the deployment environment.
- The local admin secret must never be promoted into shared or production
  environments.

## Legacy Data Import

Firebase migration format and staging rules are documented in
[firebase-migration.md](/C:/github/tellpalv2/be/docs/firebase-migration.md).

Recommended import flow:

1. Run migration in `dry-run=true` mode.
2. Validate staging file counts and JSON shape.
3. Re-run the same staging input with `dry-run=false`.
4. Check the import summary plus duplicate-skip counts in logs.

## Generated Documentation

After `./mvnw verify`, Spring Modulith documentation is written under:

- `be/docs/modulith/`

Keep that directory current on the release branch.
