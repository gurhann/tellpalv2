---
title: 'Sayfalı contributor registry read API oluştur'
type: 'feature'
created: '2026-09-03'
status: 'done'
baseline_commit: '29c6a4eac15c2e3c7ad253744a5aacbd88ebfa77'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/specs/spec-contributor-workflow/SPEC.md'
  - '{project-root}/_bmad-output/specs/spec-contributor-workflow/contributor-model-and-workflows.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/be/docs/admin-api-rules.md'
  - '{project-root}/architecture.md'
  - '{project-root}/be/docs/adr/ADR-0001-modulith-boundaries.md'
  - '{project-root}/be/docs/adr/ADR-0002-module-package-ownership.md'
  - '{project-root}/be/docs/adr/ADR-0003-code-documentation-policy.md'
  - '{project-root}/be/docs/adr/ADR-0004-shared-module-policy.md'
  - '{project-root}/be/docs/adr/ADR-0005-rest-api-documentation-policy.md'
  - '{project-root}/be/docs/adr/ADR-0006-story-page-illustration-localization.md'
  - '{project-root}/be/docs/adr/ADR-0007-category-type-aligns-with-content-type.md'
  - '{project-root}/be/docs/adr/ADR-0008-firebase-storage-direct-upload.md'
  - '{project-root}/be/docs/adr/ADR-0009-registry-read-pagination.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Contributor registry şu anda yalnızca son kayıtları sınırlı bir liste olarak döndürüyor; rol filtresi, gerçek toplam ve kararlı sayfalama yok. CMS büyüdükçe tüm sonuçları istemciye taşıyıp bellekte filtrelemek arama doğruluğunu ve performansı bozacak.

**Approach:** Mevcut picker uyumluluğunu koruyan yeni admin registry read endpoint'i oluştur. `q`, `role`, `page` ve `size` filtrelerini/sıralamasını PostgreSQL'de uygula; yalnız istenen sayfayı ve contributor kullanım özetini bir read model envelope'ı olarak döndür.

## Boundaries & Constraints

**Always:** Endpoint admin auth gerektirir ve OpenAPI ile belgelenir. `q` görünen ad üzerinde case-insensitive normalize aramadır; `role` yalnız AUTHOR, ILLUSTRATOR, NARRATOR, MUSICIAN kabul eder. Varsayılan sıra `updatedAt DESC, contributorId DESC`; `page` sıfır tabanlı, `size` varsayılan 25 ve en çok 100'dür. Yanıt `items`, `page`, `size`, `totalItems`, `totalPages` taşır; item rolleri, toplam assignment kullanımı, rol bazlı kullanım ve son güncelleme zamanını içerir. Filtreleme, sıralama, count ve pagination veritabanında yapılır; istemci veya application katmanı tüm contributorları yüklemez.

**Ask First:** Mevcut GET endpoint'ini envelope'a çevirmek; cursor pagination'a geçmek; yeni contributor rolü eklemek; kullanım özetini ayrı endpoint'e bölmek.

**Never:** Mevcut `GET /api/admin/contributors` array sözleşmesini değiştirmek; CMS formu/picker'ı veya mobil/public API'yi bu story'de değiştirmek; uygulama belleğinde filter/sort/page yapmak; başka modülün domain/infrastructure paketine bağlanmak.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Default page | no filters | first 25 rows, deterministic updated/id order, real totals | N/A |
| Name filter | `q=  ada  ` | DB-side case-insensitive match, trimmed query | N/A |
| Role filter | `role=MUSICIAN` | only profiles containing role | N/A |
| Combined page | `q`, `role`, `page=1`, `size=10` | second filtered page and accurate metadata | N/A |
| Invalid pagination | `page=-1` or `size=0/101` | no query execution | 400 validation_error |
| Invalid role | unsupported enum value | no query execution | 400 validation_error |
| Empty result | valid filter with no matches | empty items and zero totals | N/A |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java` -- existing picker-compatible GET; registry endpoint and OpenAPI boundary.
- `be/src/main/java/com/tellpal/v2/content/application/ContributorManagementService.java` -- current list use case; keep legacy path separate from registry read use case.
- `be/src/main/java/com/tellpal/v2/content/domain/ContributorRepository.java` -- content-owned read port; add paged registry projection contract.
- `be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/SpringDataContributorRepository.java` -- JPA query/specification boundary for role/name predicates and deterministic ordering.
- `be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/JpaContributorRepositoryAdapter.java` -- maps database projection to the application read model.
- `be/src/main/java/com/tellpal/v2/content/domain/ContentRepository.java` and `SpringDataContentRepository.java` -- assignment usage aggregation by contributor/role.
- `be/src/test/java/com/tellpal/v2/content/web/admin/ContributorAdminIntegrationTest.java` -- authenticated PostgreSQL contract and pagination regression coverage.
- `be/src/test/java/com/tellpal/v2/content/web/admin/ContributorAdminControllerTest.java` -- request validation and OpenAPI-facing HTTP behavior.
- `be/docs/admin-api-rules.md` -- canonical admin endpoint and validation documentation.

## Tasks & Acceptance

**Execution:**
- [x] `be/src/main/java/com/tellpal/v2/content/application`, `domain`, `infrastructure/persistence` -- add a database-backed paged registry projection with filters, deterministic ordering, totals, and usage summaries.
- [x] `be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java` and response records -- expose a separate registry endpoint while preserving the legacy array GET and document query/errors.
- [x] `be/src/test/java/com/tellpal/v2/content/web/admin/ContributorAdminControllerTest.java` and `ContributorAdminIntegrationTest.java` -- cover every matrix row, DB-side page boundaries, role filtering, totals, and tie ordering.
- [x] `be/docs/admin-api-rules.md` -- document the registry read contract and legacy endpoint distinction.

**Acceptance Criteria:**
- Given more than one page of contributors, when registry is requested, then only the requested page is returned with accurate total metadata.
- Given name and/or role filters, when registry is requested, then PostgreSQL applies both before ordering and pagination and only matching profiles are returned.
- Given equal update timestamps, when registry is requested repeatedly, then rows retain the same ID tie-break order.
- Given an invalid role or pagination value, when registry is requested, then 400 `validation_error` is returned without a repository query.
- Given the existing picker calls legacy GET, when it requests contributors, then its array response and parameters remain unchanged.

## Spec Change Log

## Design Notes

Registry response intentionally uses a new envelope endpoint because changing the existing array response would break the current picker and older CMS clients. Usage summaries are read-side projections; contributor aggregate mutation remains in Story 2.

## Verification

**Commands:**
- `cd be; .\\mvnw.cmd -Dtest=ContributorAdminControllerTest,ContributorAdminIntegrationTest test` -- expected: validation, filtering, paging and usage contract pass.
- `cd be; .\\mvnw.cmd test` -- expected: complete backend regression suite passes.

## Suggested Review Order

**Registry endpoint ve uygulama akışı**

- Yeni endpoint filtreli, sayfalı envelope yanıtını admin sınırında sunar.
  [`ContributorAdminController.java:94`](../../../../be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java#L94)

- Uygulama servisi yalnız seçilen sayfayı alıp kullanım özetini read modeline dönüştürür.
  [`ContributorManagementService.java:95`](../../../../be/src/main/java/com/tellpal/v2/content/application/ContributorManagementService.java#L95)

**Database-backed sorgu ve sınırlar**

- Domain portu Spring Data tiplerini sızdırmadan sayfa sonucunu taşır.
  [`ContributorRepository.java:30`](../../../../be/src/main/java/com/tellpal/v2/content/domain/ContributorRepository.java#L30)

- JPA adapterı PostgreSQL sayfalamasını deterministic updated/id sıralamasıyla uygular.
  [`JpaContributorRepositoryAdapter.java:44`](../../../../be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/JpaContributorRepositoryAdapter.java#L44)

- Arama wildcard’ları kaçışlanır, rol filtresi ve count sorgusu veritabanında kalır.
  [`SpringDataContributorRepository.java:31`](../../../../be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/SpringDataContributorRepository.java#L31)

**Doğrulama ve geriye dönük uyumluluk**

- Geçersiz enum query değerleri ortak ProblemDetail sözleşmesine çevrilir.
  [`AdminApiExceptionHandler.java:61`](../../../../be/src/main/java/com/tellpal/v2/shared/web/admin/AdminApiExceptionHandler.java#L61)

- Entegrasyon testleri sayfa sınırlarını, rol rekabetini, toplamları ve kullanım özetlerini kanıtlar.
  [`ContributorAdminIntegrationTest.java:159`](../../../../be/src/test/java/com/tellpal/v2/content/web/admin/ContributorAdminIntegrationTest.java#L159)
