# Local Development

## Purpose

This document is the local runbook for the Spring Boot backend under `be/`.

## Prerequisites

- Java 25
- Docker Desktop
- Git

## Local Configuration

The backend starts with the `local` profile by default. `application-local.yml`
provides local-only defaults for fast startup:

- `tellpal.security.admin.jwt-secret=change-me-change-me-change-me-32-bytes`
- `tellpal.security.firebase.stub-tokens-enabled=true`

The following values are required outside `local` and `test` profiles:

- `TELLPAL_ADMIN_JWT_SECRET`
- `TELLPAL_FIREBASE_PROJECT_ID`
- `TELLPAL_FIREBASE_CREDENTIALS_PATH`
- `TELLPAL_REVENUECAT_AUTHORIZATION_HEADER`
- `TELLPAL_DB_URL`
- `TELLPAL_DB_USERNAME`
- `TELLPAL_DB_PASSWORD`

Use [be/.env.example](/C:/github/tellpalv2/be/.env.example) as the starting point
for local environment values.

Important: Spring Boot does not automatically read a repository-root `.env` file
when you run `./mvnw spring-boot:run`. Keep required values in your shell, IDE run
configuration, or another explicit environment-loading mechanism. The `.env` file
is mainly useful for local reference and Docker Compose workflows.

## Bootstrap Data Seeded By Flyway

Flyway creates schema and seeds a small set of reference data automatically:

- `languages`: `tr`, `en`, `es`, `pt`, `de`
- `admin_roles`: `ADMIN`, `CONTENT_MANAGER`, `VIEWER`
- purchase lookup tables such as event types, stores, environments, and reason
  codes

Flyway does not create a sample admin user, Firebase user, or sample content.
Those remain environment-specific bootstrap steps.

## Start PostgreSQL

```bash
cd be
docker compose up -d postgres
```

Default local PostgreSQL values:

- database: `tellpal_v2`
- username: `tellpal`
- password: `tellpal`
- port: `5432`

## Apply Migrations

```bash
cd be
./mvnw flyway:migrate
```

Windows PowerShell:

```powershell
cd be
.\mvnw.cmd flyway:migrate
```

## Run The Application

```bash
cd be
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
cd be
.\mvnw.cmd spring-boot:run
```

## Useful Commands

```bash
cd be
./mvnw test
./mvnw verify
./mvnw flyway:migrate
```

`verify` covers:

- unit, property, and integration tests
- Modulith boundary checks
- packaged Boot jar creation
- generated Modulith docs under `be/docs/modulith/`

## Operational Notes

- The local admin JWT secret is intentionally fixed only for `local` and `test`.
- Firebase mobile auth runs with the stub token verifier in `local` and `test`.
- RevenueCat authorization must be configured outside local/test.
- Request correlation uses `X-Request-Id` and logs `requestId=...`.
- Actuator health is available under `/actuator/health`.
- Testcontainers requires Docker access for integration tests and `verify`.

## Generated Outputs

After `./mvnw verify`, the main generated outputs are:

- `be/target/tellpal-v2-backend-0.0.1-SNAPSHOT.jar`
- `be/docs/modulith/*.adoc`
- `be/docs/modulith/*.puml`
