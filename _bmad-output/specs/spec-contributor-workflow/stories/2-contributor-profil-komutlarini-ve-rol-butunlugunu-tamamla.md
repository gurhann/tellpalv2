---
title: 'Contributor profil komutlarını ve rol bütünlüğünü tamamla'
type: 'feature'
created: '2026-09-03'
status: 'done'
baseline_commit: '84266aacfb8e2cfd15091a045eb09c7ac0d43891'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/specs/spec-contributor-workflow/SPEC.md'
  - '{project-root}/_bmad-output/specs/spec-contributor-workflow/contributor-model-and-workflows.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/be/docs/admin-api-rules.md'
  - '{project-root}/architecture.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Contributor profil API'si yalnız ad alıyor; rol seti güncellenemiyor, aynı normalize adda ikinci profil oluşturma kullanıcıyı belirsiz hataya düşürüyor ve kullanımda olan bir rolün kaldırılması kredileri geçersiz kılabilir.

**Approach:** Admin create/update sözleşmesini zorunlu çoklu rolle genişlet. Normalize ad çakışmasını mevcut profil kimliğiyle, kullanılan rolü ise kullanım sayısı ve etkilenen içerik referanslarıyla açıklayan 409 ProblemDetail yanıtlarına dönüştür; rol değişimini tek transaction içinde uygula.

## Boundaries & Constraints

**Always:** Create/update trim edilmiş displayName ile en az bir benzersiz roles[] kabul eder; katalog AUTHOR, ILLUSTRATOR, NARRATOR, MUSICIAN ile sınırlıdır. Normalize ad conflict'i mevcut contributor ID'sini; kullanılan rol conflict'i rol, kullanım sayısı ve content ID/external key referanslarını taşır. Başarısız update profil durumunu değiştirmez.

**Ask First:** Yeni rol tanımlamak; duplicate profilleri birleştirmek/silmek; kullanılan rolü zorla kaldırmak veya assignmentları başka role taşımak; HTTP sözleşmesinde roles alanını isteğe bağlı tutmak.

**Never:** Assignment role-membership doğrulamasını, otomatik sıralamayı, reorder'ı, registry paginationını veya CMS formunu bu hikâyeye alma. Mobil/public API değiştirme.

## I/O & Edge-Case Matrix

| Senaryo | Girdi / durum | Beklenen davranış | Hata |
|---|---|---|---|
| Çoklu rollü create | Yeni ad + AUTHOR, MUSICIAN | Profil rollerle oluşur ve yanıt rolleri taşır | N/A |
| Geçersiz roller | Eksik, boş veya tekrarlı roles[] | Profil yazılmaz | 400 validation_error |
| Normalize duplicate | Farklı boşluk/büyük-küçük harfli mevcut ad | Yeni/rename kaydı yazılmaz | 409, existingContributorId |
| Kullanılmayan rol silme | Profilde rol var, assignment yok | Rol kaldırılır | N/A |
| Kullanılan rol silme | Rolün assignmentları var | Ad/rol seti aynı kalır | 409, usageCount, affectedContents |
| Aynı profilde rename | Normalize ad kendisiyle eş | Update başarılıdır | N/A |

</frozen-after-approval>

## Code Map

- be/src/main/java/com/tellpal/v2/content/domain/Contributor.java:35 -- rol kümesi; atomik profil güncelleme davranışının sahibi.
- be/src/main/java/com/tellpal/v2/content/domain/ContributorRepository.java:9 -- normalize ad lookup ve flush portu eklenir.
- be/src/main/java/com/tellpal/v2/content/domain/ContentRepository.java:56 -- role-level usage/read reference portu.
- be/src/main/java/com/tellpal/v2/content/application/ContributorManagementService.java:46 -- create/update, duplicate çevirisi ve kullanılan-rol koruması.
- be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java:53 -- admin request/response ve 409 OpenAPI sözleşmesi.
- be/src/test/java/com/tellpal/v2/content/web/admin/ContributorAdminIntegrationTest.java:49 -- auth'lı API/regresyon testi.
- be/docs/admin-api-rules.md:261 -- doğrulanmış admin sözleşmesi güncellemesi.

## Tasks & Acceptance

**Execution:**
- [x] be/src/main/java/com/tellpal/v2/content/domain/Contributor.java -- atomik ad+rol güncellemesini ve rol farkını güvenli modelle.
- [x] be/src/main/java/com/tellpal/v2/content/domain/ContributorRepository.java and infrastructure adapters -- normalized lookup, save/flush ve role usage sorguları.
- [x] be/src/main/java/com/tellpal/v2/content/domain/ContentRepository.java and infrastructure adapters -- contributor+role kullanım count ve content referansları.
- [x] be/src/main/java/com/tellpal/v2/content/application/ContributorManagementCommands.java, ContributorManagementResults.java, ContributorManagementService.java, ContentApplicationExceptions.java -- roles[] komutları, conflict modeli ve transaction bütünlüğü.
- [x] be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java, AdminContributorResponse.java, ContentAdminExceptionHandler.java -- request/response rolleri, 409 metadata ve OpenAPI.
- [x] be/src/test/java/com/tellpal/v2/content/domain/ContributorTest.java, ContributorAdminControllerTest.java, ContributorAdminIntegrationTest.java -- matris senaryoları.
- [x] be/docs/admin-api-rules.md -- değişen admin profil sözleşmesini ve conflict yanıtlarını belgele.

**Acceptance Criteria:**
- Given admin geçerli çoklu rollü profil gönderir, when create veya update çağrılır, then roller kalıcıdır ve yanıt bunları içerir.
- Given normalize adı başka profile ait, when create veya rename yapılır, then 409 yanıtı mevcut contributor kimliğini taşır ve kayıt değişmez.
- Given kullanılan bir rol çıkarılır, when update çağrılır, then 409 kullanım sayısı/etkilenen content referanslarıyla döner ve profil değişmeden kalır.
- Given kullanılmayan bir rol çıkarılır, when update çağrılır, then profil güncellenir; aynı profile rename normalize conflict yaratmaz.
- Given missing, empty veya duplicate roles[] gönderilir, when request işlenir, then 400 döner ve hiçbir profil yazılmaz.

## Spec Change Log

## Design Notes

İstemci payload'ı List<ContributorRole> olarak okunur; tekrar eden roller sessizce Set'e düşmez. Domain yalnız doğrulanmış benzersiz Set'i kabul eder. Önceki DB lookup ve yarıştaki unique constraint ihlali aynı duplicate conflict modeline çevrilir. Usage sorgusu yalnız kaldırılan roller için çalışır.

## Verification

**Commands:**
- cd be; .\mvnw.cmd -Dtest=ContributorTest,ContributorAdminControllerTest,ContributorAdminIntegrationTest test -- profil komutları ve 409 sözleşmesi geçer.
- cd be; .\mvnw.cmd test -- backend regresyon paketi geçer.

## Suggested Review Order

**Profil güncelleme ve yarış güvenliği**

- Komutlar tek transaction içinde ad/rol farkını korur ve çakışmayı anlamlı hataya dönüştürür.
  [`ContributorManagementService.java:55`](../../../../be/src/main/java/com/tellpal/v2/content/application/ContributorManagementService.java#L55)

- Bozulmuş flush transaction'ından bağımsız olarak gerçek çakışan profil kimliği çözülür.
  [`ContributorDuplicateNameResolver.java:24`](../../../../be/src/main/java/com/tellpal/v2/content/application/ContributorDuplicateNameResolver.java#L24)

**Kullanımdaki rol koruması**

- Assignment sayısı ile tekil etkilenen içerik referansları ayrı sorgulanır.
  [`SpringDataContentRepository.java:76`](../../../../be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/SpringDataContentRepository.java#L76)

- 409 ProblemDetail, rolü, kullanım sayısını ve açıklayıcı içerik referanslarını taşır.
  [`ContentAdminExceptionHandler.java:151`](../../../../be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminExceptionHandler.java#L151)

**HTTP sözleşmesi ve regresyonlar**

- Create/update request'leri zorunlu, benzersiz roles[] doğrulamasını sınırda uygular.
  [`ContributorAdminController.java:207`](../../../../be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java#L207)

- Matris testleri validation, duplicate rename ve role-in-use durumlarını doğrular.
  [`ContributorAdminIntegrationTest.java:140`](../../../../be/src/test/java/com/tellpal/v2/content/web/admin/ContributorAdminIntegrationTest.java#L140)
