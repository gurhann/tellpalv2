---
title: 'Contributor atama ve sıralama kontratını güçlendir'
type: 'feature'
created: '2026-09-04'
status: 'done'
baseline_commit: 'fcb0cfecc374f962a320b0b54ac1dad40d7efe24'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/specs/spec-contributor-workflow/SPEC.md'
  - '{project-root}/_bmad-output/specs/spec-contributor-workflow/contributor-model-and-workflows.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/be/docs/admin-api-rules.md'
  - '{project-root}/architecture.md'
  - '{project-root}/be/docs/adr/'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Bir contributor, profilinde olmayan bir rolle içeriğe atanabiliyor; istemci tarafından verilen `sortOrder` ise aynı grubun sırasını yarış koşullarına ve çakışmalara açık bırakıyor. CMS'nin güvenilir rol bazlı çalışma alanı için atamaların profil yetkinliğiyle uyumlu ve sıralamanın sunucu sahipliğinde olması gerekir.

**Approach:** Content-owned atama akışını profil-rolü doğrulamasıyla güçlendir, yeni atamayı rol/dil grubunun sonuna atomik ekle ve assignment kimlikleriyle çalışan grup-içi reorder komutu sun. Mevcut kayıtları sıralı başlangıç durumuna taşı; mevcut CMS gönderimlerini kırmamak için eski `sortOrder` alanını kabul edip yok say.

## Boundaries & Constraints

**Always:** Atama, contributor profilinin `roles` setinde bulunan rolle yapılır; aksi hâlde `400 invalid_request` döner ve aggregate değişmez. Sıra, `content + role + nullable languageCode` grubuna aittir; yeni kayıt grup sonuna sıfırdan ardışık değerle eklenir. Aynı content için atama/reorder yazıları transaction içinde kilitlenir; eşzamanlı eklemeler duplicate sıra üretemez. Reorder yalnız aynı grubun mevcut assignment ID'lerinin eksiksiz ve tekrarsız bir permütasyonunu kabul eder; başarıda sıra `0..n-1` olur. Reorder sırasında benzersiz indeks çakışması güvenli bir geçici aşama ile önlenir ve kalıcı satırlar yine negatif olmayan değerler taşır. Atama/list/reorder yanıtı assignment ID içerir; yeni endpoint admin auth, OpenAPI ve ProblemDetail ile belgelenir.

**Ask First:** Assignment rolünü veya dil kapsamını değiştiren bir komut eklemek; global ve yerel gruplar arasında taşıma; mevcut atama/unassign URL'lerini kaldırmak ya da `sortOrder` alanını API'den tamamen reddetmek; CMS arayüzünü değiştirmek.

**Never:** Contributor profil rolünü otomatik değiştirmek; geçersiz reorder'da kısmi sıralama yazmak; client-side sıra hesaplamasına güvenmek; content modülü dışındaki domain/infrastructure paketlerine bağlanmak; mobil/public API'yi veya CMS'yi bu story'de değiştirmek.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|----------------------------|----------------|
| Uyumlu atama | Profil `AUTHOR`; global AUTHOR grubu iki kayıt | Yeni kayıt `sortOrder=2`, dönen yanıtta assignment ID | N/A |
| Uyuşmayan rol | Profilde `AUTHOR`, istek `NARRATOR` | Hiçbir assignment eklenmez | 400 `invalid_request` |
| Ayrı kapsam | Aynı rolün global ve `tr` grupları | Her grup kendi sonunda sıralanır | N/A |
| Geçerli reorder | Grubun tüm assignment ID'leri yeni sırada | Yanıt sırası ve kalıcı değerler `0..n-1` | N/A |
| Geçersiz reorder | Eksik, tekrarlı veya başka grup/content ID'si | Mevcut sıra aynen kalır | 400 `invalid_request` |
| Eşzamanlı ekleme | Aynı role/dil grubuna iki istek | Her ikisi kalıcı, benzersiz ardışık sırada | N/A |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/content/domain/Content.java` -- contributor aggregate kuralları; otomatik son sıra, grup doğrulama ve iki aşamalı reorder davranışı burada kalmalı.
- `be/src/main/java/com/tellpal/v2/content/domain/ContentContributor.java` -- assignment kimliği ve sadece aggregate'ın kullandığı sıralama güncellemesi.
- `be/src/main/java/com/tellpal/v2/content/domain/ContentRepository.java`, `infrastructure/persistence/SpringDataContentRepository.java`, `JpaContentRepositoryAdapter.java` -- contributor yazıları için fetch edilmiş `PESSIMISTIC_WRITE` aggregate yüklemesi; Spring Data tipi porttan sızmamalı.
- `be/src/main/java/com/tellpal/v2/content/domain/Contributor.java` -- mevcut `roles` seti, atama öncesi yetkinlik doğrulamasının kaynağıdır.
- `be/src/main/java/com/tellpal/v2/content/application/ContributorManagementCommands.java`, `ContributorManagementResults.java`, `ContributorManagementService.java`, `ContentManagementMapper.java` -- client sort girdisini uyumlu biçimde etkisizleştiren komutlar, assignment ID'li read model ve transaction orkestrasyonu.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java`, `AdminContentContributorResponse.java` -- mevcut atama/listeyi genişleten admin HTTP sözleşmesi ve yeni reorder endpoint'i.
- `be/src/main/resources/db/migration/V21__normalize_content_contributor_sort_orders.sql` -- mevcut boşluklu sıraları her role/dil grubu için `0..n-1` normalleştirir; mevcut benzersiz/check kısıtlarını korur.
- `be/src/test/java/com/tellpal/v2/content/domain/ContentTest.java`, `content/web/admin/ContributorAdminControllerTest.java`, `ContributorAdminIntegrationTest.java` -- domain invariantları, HTTP validation, PostgreSQL kilit/persistence ve geri-alma kanıtı.
- `be/docs/admin-api-rules.md` -- doğrulanmış atama, reorder ve eski sort alanı uyumluluk kuralının CMS referansı.

## Tasks & Acceptance

**Execution:**
- [x] Content domain ve persistence portu -- kilitli contributor-yazı yüklemesini, sunucu sahipliğinde append/reorder kurallarını ve güvenli iki aşamalı sıralamayı ekle.
- [x] `V21__normalize_content_contributor_sort_orders.sql` -- legacy grupları transaction içinde çakışmasız biçimde ardışık sıralara taşı; mevcut kısıtları koru.
- [x] Contributor application/web katmanları -- profil rolü kontrolü, assignment ID'li sonuçlar, eski `sortOrder` uyumluluğu ve rol+opsiyonel dil kapsamlı reorder kontratını OpenAPI ile uygula.
- [x] Domain, controller ve Testcontainers integration testleri -- tüm matris satırlarını; özellikle geçersiz reorder'ın atomikliğini ve eşzamanlı append bütünlüğünü kanıtla.
- [x] `be/docs/admin-api-rules.md` -- artık mevcut olan contributor read/unassign yollarını düzelt ve yeni kontratı belgele.

**Acceptance Criteria:**
- Given contributor profilinde rol yokken, when o rolle atama istenir, then istek `400 invalid_request` döner ve hiçbir atama/sıra değişmez.
- Given aynı role/dil grubuna ardışık veya eşzamanlı atamalar, when transactionlar tamamlanır, then her atama bir kez vardır ve sort değerleri benzersiz, ardışıktır.
- Given geçerli bir grup permütasyonu, when reorder çağrılır, then yalnız o grubun sırası değişir ve response assignment ID'leri içerir.
- Given eksik, duplicate veya grup dışı ID, when reorder çağrılır, then `400 invalid_request` döner ve önceki kalıcı sıra değişmez.
- Given eski CMS atama isteği `sortOrder` taşırken, when gönderilir, then değer sıralamayı belirlemez; backend grubu sonuna ekler.

## Spec Change Log

## Design Notes

Mevcut partial unique indeksler swap sırasında anlık çakışma yaratır. V21 önce legacy sıraları normalize eder; aggregate daha sonra geçici, grubun üstünde benzersiz değerleri flush edip hedef `0..n-1` değerlerini yazar. Böylece check constraint korunur, client'ın büyük/boşluklu eski sıraları kalıcı akışa taşınmaz ve parent-row kilidi aynı grubun yazılarını serileştirir.

## Verification

**Commands:**
- `cd be; .\mvnw.cmd -Dtest=ContentTest,ContributorAdminControllerTest,ContributorAdminIntegrationTest test` -- expected: domain, HTTP, Flyway ve PostgreSQL sıralama akışları geçer.
- `cd be; .\mvnw.cmd test` -- expected: complete backend regression suite passes.

## Suggested Review Order

**Admin komutları ve sözleşme**

- Reorder girişini, id permutationını ve HTTP hata kontratını görünür kılar.
  [`ContributorAdminController.java:184`](../../../../be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java#L184)

- Tek transaction içinde append, reorder ve güvenli unassign sırasını orkestre eder.
  [`ContributorManagementService.java:169`](../../../../be/src/main/java/com/tellpal/v2/content/application/ContributorManagementService.java#L169)

**Aggregate bütünlüğü ve kilitleme**

- Profil rolü, grup sonu sıra, reorder ve kompaksiyon invariantlarını uygular.
  [`Content.java:270`](../../../../be/src/main/java/com/tellpal/v2/content/domain/Content.java#L270)

- Content ve contributor yazı kilitlerini persistence sınırında tutar.
  [`SpringDataContentRepository.java:63`](../../../../be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/SpringDataContentRepository.java#L63)
  [`SpringDataContributorRepository.java:21`](../../../../be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/SpringDataContributorRepository.java#L21)

**Legacy veri ve kanıt**

- Büyük eski sort değerlerinde bile overflow olmadan grupları normalize eder.
  [`V21__normalize_content_contributor_sort_orders.sql:1`](../../../../be/src/main/resources/db/migration/V21__normalize_content_contributor_sort_orders.sql#L1)

- Permütasyon, eşzamanlı yazı ve migration davranışını gerçek PostgreSQL’de doğrular.
  [`ContributorAdminIntegrationTest.java:505`](../../../../be/src/test/java/com/tellpal/v2/content/web/admin/ContributorAdminIntegrationTest.java#L505)

- CMS’e dönük doğrulanmış API kurallarını güncel tutar.
  [`admin-api-rules.md:255`](../../../../be/docs/admin-api-rules.md#L255)
