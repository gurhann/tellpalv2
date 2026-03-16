---
name: write-testcontainers-integration-test
description: Generate Spring Boot integration tests that use Testcontainers with PostgreSQL for backends built with Spring Modulith, Flyway, and JUnit 5. Use when Codex needs to write integration tests, verify repository behavior, test database constraints, validate REST API flows, test Flyway migrations, or check transactional behavior against a real database instead of mocks.
---

# Write Testcontainers Integration Test

## Overview

Write integration tests that exercise real Spring Boot wiring against a real PostgreSQL container. Keep the test focused on observable persistence or API behavior so the user gets both runnable test code and a short explanation of what database behavior is being verified.

## Workflow

1. Inspect the target module, Spring test style, repository or API entry point, and Flyway configuration before writing the test.
2. Identify the scope: repository constraint test, transactional workflow test, migration verification, or REST API flow.
3. Configure a PostgreSQL Testcontainer with `@Testcontainers` and make Spring Boot use it for the test context.
4. Write a focused `@SpringBootTest` integration test that verifies real database behavior, such as unique constraints, foreign keys, cascade behavior, transaction commit or rollback, or HTTP responses backed by persisted state.
5. Explain the tested behavior, the container setup, and the database verification logic after the code.

## Authoring Rules

- Use `@SpringBootTest`.
- Use `@Testcontainers`.
- Use a PostgreSQL container.
- Configure container lifecycle clearly and reuse project patterns for static containers or shared base classes when they already exist.
- Let Flyway run against the containerized database so schema behavior is real.
- Prefer repository, service, or HTTP-level assertions over mocking persistence.
- Use `MockMvc` or `WebTestClient` for API tests according to the project style.
- Verify HTTP status codes, response payloads, and validation errors for API scenarios.
- Keep each test focused on one persistence or API behavior.
- Ensure test isolation through setup, cleanup, transactional strategy, or fresh data creation instead of relying on test order.

## Database and Transaction Checks

- Verify migrations start successfully on the PostgreSQL container.
- Verify unique constraints, foreign keys, and cascade rules with real inserts and deletes.
- Verify transactional boundaries by asserting committed state, rollback behavior, or visibility across operations.
- Verify lazy-loading or repository mapping behavior only when the test scope truly requires it.
- Verify persisted data with repositories, JDBC queries, or API reads, depending on what the use case is proving.

## Output Format

Return all of the following:

1. The complete integration test class.
2. The container configuration.
3. A short explanation of the tested behavior.
4. A concise explanation of the database verification logic.

## Safety Checks

- Verify the test hits a real PostgreSQL container, not an in-memory substitute.
- Verify container wiring is compatible with Spring Boot test startup.
- Verify the assertions prove actual persistence behavior instead of only checking mocks.
- Verify data setup is isolated and repeatable.
- Verify API tests assert both HTTP outcomes and underlying persisted effects when relevant.

## Reference

Read [`references/testcontainers-integration-checklist.md`](references/testcontainers-integration-checklist.md) when you need a compact checklist for PostgreSQL container setup, Spring Boot wiring, migration validation, and persistence assertions.
