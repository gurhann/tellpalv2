# Backend Architecture

## Overview

TellPal v2 backend is a Spring Boot modular monolith organized with Spring Modulith and DDD-style modules. The system manages multilingual content, admin authentication, user profiles, event ingestion, asset metadata, category curation, and purchase attribution on PostgreSQL.

The backend lives under [`be/`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be). Source code is under [`be/src/main/java/com/tellpal/v2`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/src/main/java/com/tellpal/v2), migrations are under [`be/src/main/resources/db/migration`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/src/main/resources/db/migration), and tests are under [`be/src/test/java/com/tellpal/v2`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/src/test/java/com/tellpal/v2).

## Technology Stack

- Java 25
- Spring Boot 4.0.0
- Spring Modulith 1.3.0
- Spring Web, Spring Data JPA, Spring Security
- PostgreSQL 15
- Flyway for schema migrations
- Firebase Admin SDK for Firebase integration
- JJWT for admin JWT tokens
- SpringDoc OpenAPI for API docs
- JUnit 5, jqwik, Testcontainers, Spring Modulith test support

## Module Structure

Each top-level package under `com.tellpal.v2` is treated as a module:

- `admin`: admin auth, refresh tokens, admin user management
- `asset`: media asset metadata, processing status, packaging concerns
- `category`: category definitions and localized curation
- `content`: stories, meditations, lullabies, localizations, contributors, free-access rules
- `event`: content and app event ingestion
- `purchase`: RevenueCat-driven purchase and attribution model
- `shared`: shared kernel types used across modules
- `user`: Firebase-backed user and profile model
- `presentation`: REST controllers and API DTOs

Current modules are marked `OPEN` in their `package-info.java` definitions, but the target rule remains explicit boundaries and low coupling. `presentation` is the entry module that depends on application services from other modules.

## Layering Rules

Inside each business module, keep the structure aligned with DDD:

- `domain`: entities, value objects, invariants, repository contracts
- `application`: use-case orchestration and transaction boundaries
- `infrastructure`: JPA mappings, persistence adapters, security, external integrations
- `api`: only when a module exposes a stable contract to other modules

Controllers must stay thin and belong in `presentation`. Business rules belong in domain models and application services, not controllers or persistence adapters.

## Module Boundary Rules

- Do not access another module's internal classes directly.
- Prefer module interaction through application services, published events, or stable API types.
- Avoid cross-module entity references; prefer IDs at module boundaries.
- Do not create circular dependencies between modules.
- Keep repository access inside the owning module.

Boundary verification is enforced by [`ApplicationModulesTest.java`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/src/test/java/com/tellpal/v2/ApplicationModulesTest.java), which runs `ApplicationModules.of("com.tellpal.v2").verify()` and generates Modulith documentation into `be/target/spring-modulith-docs`.

## Persistence Model

The database is PostgreSQL with Flyway-managed forward-only migrations. JPA runs with `ddl-auto=validate`, so schema is expected to come from SQL migrations, not Hibernate auto-generation.

Important conventions:

- tables and columns use `snake_case`
- migrations use `V{number}__description.sql`
- timestamps are stored with UTC semantics
- foreign keys, uniqueness, and validation rules are modeled explicitly in SQL

Existing migration flow covers languages, media assets, admins, content, categories, users, purchases, and events.

## Application Flow

Typical request flow:

1. HTTP request enters `presentation`
2. Controller validates input DTOs and calls an application service
3. Application service loads aggregates or repositories and coordinates the use case
4. Domain objects enforce invariants
5. Infrastructure persists changes through JPA and PostgreSQL
6. Response DTO is returned from `presentation`

For asynchronous or cross-module behavior, prefer domain or application events over direct internal coupling.

## Testing Strategy

The project uses three main testing layers:

- unit and domain tests with JUnit 5
- property-based invariant tests with jqwik
- integration tests with PostgreSQL Testcontainers

Examples already present in the codebase include:

- module boundary verification in [`ApplicationModulesTest.java`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/src/test/java/com/tellpal/v2/ApplicationModulesTest.java)
- foreign-key integration coverage in [`ForeignKeyIntegrityIntegrationTest.java`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/src/test/java/com/tellpal/v2/infrastructure/ForeignKeyIntegrityIntegrationTest.java)
- multiple jqwik property tests across domain modules

## Runtime Configuration

Primary runtime configuration is in [`application.yml`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be/src/main/resources/application.yml).

Key behavior:

- PostgreSQL datasource via environment variables
- Flyway validation enabled
- Hibernate schema validation enabled
- Swagger/OpenAPI exposed through SpringDoc
- Firebase and JWT secrets supplied externally

## Development Commands

Run from [`be/`](/Users/gurhankucuk/Documents/GitHub/tellpalv2/be):

- `./mvnw test`
- `./mvnw verify`
- `./mvnw flyway:migrate`

When changing architecture-sensitive code, always rerun module verification and the relevant test layer before merging.
