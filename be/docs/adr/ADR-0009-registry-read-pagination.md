# ADR-0009: Registry Read Pagination Must Be Database-Backed

## Status

Accepted

## Context

The first Contents registry implementation loaded every content aggregate, including story pages
and localized page payloads, into the application layer before applying search, filters, ordering,
and pagination. It works for the current small catalogue but makes response size and memory use
grow with the whole catalogue instead of the requested page.

## Decision

Registry read endpoints must apply base predicates and ordering in the database. Use Spring Data
`Specification`/Criteria queries for content type, selected-language title, external key, content
ID, and ordinary aggregate fields. Fetch only the selected page of aggregate IDs, then load the
minimal graph required for the page's readiness calculation.

Readiness is a derived cross-child value: it depends on all story pages and their selected-language
payloads. A `Specification` alone cannot correctly filter or sort it after paging. When a
readiness filter must scale, implement a dedicated SQL projection/read model (or a maintained
readiness projection) that calculates the aggregate condition before pagination.

## Consequences

- Never use `findAll...().stream().filter().sorted().skip().limit()` for an admin registry.
- `Specification` is the default for row-local predicates; it is not a substitute for aggregate
  projections.
- Registry query plans require tests proving that filtering and paging happen before graph loading.

## Alternatives Considered

- In-memory filtering was rejected because it has unbounded memory and query cost.
- A single fetch-join query with `Pageable` was rejected because collection fetch joins can make
  pagination inaccurate and expensive.
- Treating readiness as a normal JPA specification was rejected because its page-level completeness
  semantics would be evaluated after pagination or require unsafe join duplication.

## Related Files or Modules

- `be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/SpringDataContentRepository.java`
- `be/src/main/java/com/tellpal/v2/content/application/AdminContentQueryService.java`
- `be/docs/admin-api-rules.md`
