# Testcontainers Integration Checklist

## Container Setup

- use PostgreSQL Testcontainers
- annotate with `@Testcontainers`
- expose datasource properties to Spring Boot using the project pattern
- reuse shared container setup only when it already exists

## Spring Boot Wiring

- use `@SpringBootTest`
- let Flyway run on startup
- use real beans instead of mocked repositories
- choose `MockMvc` or `WebTestClient` based on the existing test stack

## Database Verification

- assert unique constraints with duplicate writes
- assert foreign keys and cascade behavior with real persisted rows
- assert migration success through context startup or explicit schema expectations
- assert committed or rolled-back state through repository or JDBC reads

## Isolation

- create fresh data per test
- avoid dependence on test ordering
- clean up through transaction strategy or explicit teardown when needed
- keep each test focused on one integration behavior

## API Flows

- verify status codes and payload shape
- verify validation failures for bad input
- verify persisted side effects when the endpoint writes data
- keep controller behavior connected to real application wiring
