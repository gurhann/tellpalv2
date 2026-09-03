---
title: 'Contributor rol modelini ve güvenli migrationı oluştur'
type: 'feature'
created: '2026-09-02'
status: 'done'
baseline_commit: '1ef4661ef7938907520f30b5d2d98aec26c3030f'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/specs/spec-contributor-workflow/SPEC.md'
  - '{project-root}/_bmad-output/specs/spec-contributor-workflow/contributor-model-and-workflows.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/architecture.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Contributor profilleri rol taşımıyor; adların trim + harf duyarsız benzersizliği veritabanında korunmuyor. Mevcut kayıtlar geçirilirken tarihsel atamalar kaybolmamalı ve mükerrer profiller otomatik birleştirilmemeli.

**Approach:** Önce çakışmaları raporlayıp duran, sonra normalize ad ile çoklu rol kalıcılığını kuran ileri yönlü V20 migrationını ekle. `Contributor` aggregate'ini en az bir rolle genişlet ve migration/domain davranışını otomatik test et.

## Boundaries & Constraints

**Always:** Duplicate preflight V20'nin ilk SQL işlemi olmalı ve tüm çakışan ID/adları raporlamalı. Normalize anahtar DB-owned `lower(btrim(display_name))` olmalı. Roller yalnız `AUTHOR`, `ILLUSTRATOR`, `NARRATOR`, `MUSICIAN`; profil en az bir benzersiz rol taşır. Tarihsel distinct assignment rolleri backfill edilir, atamasız contributor `AUTHOR` alır. `content_contributors` verisi değişmez; değişiklik `content` modülünde kalır.

**Ask First:** En az bir rolü deferrable DB trigger'ıyla zorlamak; normalize algoritmasını/rol kataloğunu değiştirmek; assignment verisini yeniden yazmak.

**Never:** Duplicate profilleri merge/silme; uygulanmış migrationı değiştirme; API/CMS rol sözleşmesini bu hikâyeye alma; `CREATE INDEX CONCURRENTLY` kullanma.

## I/O & Edge-Case Matrix

| Senaryo | Durum | Beklenen | Hata |
|---|---|---|---|
| Tarihsel roller | Bir profilde farklı assignment rolleri | Distinct roller eklenir; assignmentlar aynı kalır | N/A |
| Atamasız profil | Assignment yok | `AUTHOR` eklenir | N/A |
| Mevcut çakışma | `" Ada "`, `"ada"` | V20 değişikliği uygulanmaz | Tüm çakışan ID/adlarıyla exception |
| Yeni çakışma | V20 sonrası normalize eş ad | İkinci yazım reddedilir | DB uniqueness hatası |
| Geçersiz profil | Boş/null roller | Aggregate oluşmaz | `IllegalArgumentException` |

</frozen-after-approval>

## Code Map

- `be/src/main/resources/db/migration/V6__create_contributor_tables.sql:1` -- genişletilecek mevcut şema; assignment rolü/kredi/dil/sıra korunur.
- `be/src/main/resources/db/migration/V17__align_category_types_with_content_types.sql:4` -- açıklayıcı `DO $$`/`RAISE EXCEPTION` örneği; V20'de preflight DDL'den önce gelir.
- `be/src/main/java/com/tellpal/v2/content/domain/Contributor.java:14` -- generated normalize kolon eşlemesi ve rol kümesi.
- `be/src/main/java/com/tellpal/v2/content/domain/ContributorRole.java:6` -- mevcut dört değerli katalog.
- `be/src/main/java/com/tellpal/v2/content/domain/ContentContributor.java:18` -- gerçek kredi rolünün değişmeyecek sahibi.
- `be/src/main/java/com/tellpal/v2/content/application/ContributorManagementService.java:46` -- Story 2'ye kadar yalnız-ad create, geriye uyumlu `AUTHOR` köprüsünü kullanır.
- `be/src/test/java/com/tellpal/v2/category/migration/CategoryTypeMigrationIntegrationTest.java:20` -- PostgreSQL 15 + Flyway target/latest test kalıbı.
- `be/src/test/java/com/tellpal/v2/content/ContentManagementIntegrationTest.java:63` -- child tablo temizliği ve mevcut create/assignment regresyonu.

## Tasks & Acceptance

**Execution:**
- [x] `be/src/main/resources/db/migration/V20__add_contributor_roles_and_normalized_name.sql` -- preflight, generated normalize kolon/unique constraint, rol tablosu ve backfill.
- [x] `be/src/main/java/com/tellpal/v2/content/domain/Contributor.java` -- trim/normalize ad, en az bir rol ve immutable rol görünümü; mevcut create için geçici AUTHOR varsayımı.
- [x] `be/src/test/java/com/tellpal/v2/content/domain/ContributorTest.java` -- boş, tekli/çoklu rol ve normalize ad testleri.
- [x] `be/src/test/java/com/tellpal/v2/content/migration/ContributorRoleMigrationIntegrationTest.java` -- V19→V20 backfill, veri korunumu, duplicate raporu/rollback ve uniqueness testleri.
- [x] `be/src/test/java/com/tellpal/v2/content/ContentManagementIntegrationTest.java` -- gerekiyorsa temizlik/regresyon uyumu.

**Acceptance Criteria:**
- Given çakışmasız V19, when V20 çalışır, then her contributor en az bir rol ve DB tarafından üretilmiş benzersiz normalize ad taşır.
- Given tarihsel assignmentlar, when V20 tamamlanır, then distinct roller eksiksizdir ve assignment alanları değişmemiştir.
- Given normalize çakışan V19 kayıtları, when V20 çalışır, then hata tüm ID/adları gösterir ve V20 kolon/tablosu oluşmaz.
- Given yeni model, when mevcut create/assignment testleri çalışır, then API kontratı değişmeden geçer.

## Spec Change Log

## Design Notes

`normalized_display_name`, `lower(btrim(display_name))` stored generated kolonudur ve JPA'da yazmaya kapalı eşlenir. `contributor_roles`; `(contributor_id, role)` composite PK, cascade FK, rol check'i ve `(role, contributor_id)` indeksi taşır. Child tabloda “en az bir satır” basit CHECK ile güvenli kurulamadığından invariant domain/service transactionında korunur; trigger kapsam dışıdır.

## Verification

**Commands:**
- `cd be; .\\mvnw.cmd -Dtest=ContributorTest,ContributorRoleMigrationIntegrationTest test` -- domain ve V19→V20 senaryoları geçer.
- `cd be; .\\mvnw.cmd test` -- backend regresyon paketi geçer.

## Suggested Review Order

**Migration güvenliği**

- Eşzamanlı yazıları durdurur, çakışmaları şema değişmeden raporlar.
  [`V20__add_contributor_roles_and_normalized_name.sql:5`](../../../../be/src/main/resources/db/migration/V20__add_contributor_roles_and_normalized_name.sql#L5)

- Normalize anahtarı ve rol kalıcılığını veritabanının tek otoritesi yapar.
  [`V20__add_contributor_roles_and_normalized_name.sql:33`](../../../../be/src/main/resources/db/migration/V20__add_contributor_roles_and_normalized_name.sql#L33)

**Domain modeli**

- Üretilmiş alanı insert ve rename sonrası aynı persistence context'te yeniler.
  [`Contributor.java:32`](../../../../be/src/main/java/com/tellpal/v2/content/domain/Contributor.java#L32)

- Profili en az bir immutable rolle oluşturur; eski akışı AUTHOR ile korur.
  [`Contributor.java:35`](../../../../be/src/main/java/com/tellpal/v2/content/domain/Contributor.java#L35)

**Doğrulama**

- JPA rol koleksiyonu ve generated alanın insert/rename round-trip'ini doğrular.
  [`ContentManagementIntegrationTest.java:213`](../../../../be/src/test/java/com/tellpal/v2/content/ContentManagementIntegrationTest.java#L213)

- Backfill, rollback ve benzersizlik senaryolarını doğrudan PostgreSQL'de sınar.
  [`ContributorRoleMigrationIntegrationTest.java:38`](../../../../be/src/test/java/com/tellpal/v2/content/migration/ContributorRoleMigrationIntegrationTest.java#L38)
