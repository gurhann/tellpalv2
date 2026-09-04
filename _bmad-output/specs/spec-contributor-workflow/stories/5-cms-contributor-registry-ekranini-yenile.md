---
title: 'CMS contributor registry ekranını yenile'
type: 'feature'
created: '2026-09-04'
status: 'done'
baseline_commit: '6c513dde9cd51c70e6e945e3eeeb1e4d7e54c248'
review_loop_iteration: 0
context:
  - 'C:/github/tellpalv2/AGENTS.md'
  - 'C:/github/tellpalv2/cms/AGENTS.md'
  - 'C:/github/tellpalv2/cms/docs/ui-standards.md'
  - 'C:/github/tellpalv2/be/docs/adr/ADR-0009-registry-read-pagination.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Contributor registry şu anda eski, sayısız olmayan `GET /api/admin/contributors` listesini yükleyip istemci tarafında yalnızca son 12 kaydı ve ad aramasını uyguluyor. Roller, gerçek toplam, URL ile korunabilen filtreler ve rol içeren profil düzenleme akışı görünür değil.

**Approach:** Registry rotasını database-backed `GET /api/admin/contributor-registry` sözleşmesine bağla; ortak `RegistryToolbar` ve `Pagination` primitive’lerini kullan; rol etiketleri, ad/rol filtreleri ve gerçek toplamı göster; create/edit dialoglarında görünen ad ile zorunlu çoklu rol seçimini destekle. Tüm görünen metinleri i18n katmanına taşı ve masaüstü görsel regression ekle.

## Boundaries & Constraints

**Always:** Backend `q`, `role`, sıfır tabanlı `page` ve `size` parametreleri ile sorgulanacak; istemci tüm kayıtları yüklemeyecek veya filtrelemeyecek. URL filtreleri ve sayfa yenileme/handoff sonrasında korunacak, filtre değişince sayfa sıfırlanacak. Tablo contributor, roller, kullanım özeti, son güncelleme ve eylemler kolonlarını gösterecek. Profil en az bir benzersiz rol ile kaydedilecek; rol etiketleri ve hata/boş/yükleniyor metinleri Türkçe ve İngilizce i18n anahtarlarından gelecek. Ortak toolbar/pagination layout kontratları ve erişilebilir görünür filtre etiketleri korunacak.

**Ask First:** Backend endpoint/envelope değiştirmek; içerik detayındaki picker/assignment davranışını bu story içinde değiştirmek; yeni rol veya bağımsız contributor detay rotası eklemek; mobil/public API kapsamını genişletmek.

**Never:** Client-side son 12 kayıt limiti, toplu liste filtreleme/sıralama/sayfalama, route içinde düşük seviye `FilterBar` ile yeni toolbar düzeni, hard-coded locale metinleri veya mevcut create/update/delete API kontratlarını sessizce değiştirmek.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Varsayılan liste | URL filtresi yok | İlk backend sayfası, roller ve gerçek toplam görünür | API problemi ortak hata görünümünde retry sunar |
| Arama/rol | `q` ve/veya `role` URL’de | Backend yalnız eşleşen sayfayı döndürür; sayfa 0’a döner | Geçersiz rol güvenli biçimde reddedilir veya sıfır sonuç gösterilir |
| Sayfalama | `page`, toplam > page size | Önceki/sonraki kontrolleri doğru disabled olur | Yüklenirken geçiş kilitlenir |
| Sonuç yok | Filtre sonucu boş | Açık boş durum ve filtreyi temizleme yolu | Hata gibi gösterilmez |
| Create/Edit | Ad + en az bir benzersiz rol | Başarılı kayıt sonrası liste invalidation/refetch | Alan hataları dialogda; duplicate/conflict problem görünür |

</frozen-after-approval>

## Code Map

- `cms/src/app/routes/contributors.tsx` -- route state, toolbar, rail ve dialog orchestration; eski local search/12-record filtresi burada kaldırılacak.
- `cms/src/features/contributors/api/contributor-admin.ts` -- registry page response şeması ve `q/role/page/size` URL parametreleri; mevcut profil komutları korunur.
- `cms/src/features/contributors/queries/use-contributors.ts` -- TanStack Query key ve database-backed sayfa hook’u; toplam/page metadatasını expose eder.
- `cms/src/features/contributors/model/contributor-view-model.ts` -- registry item mapper, rol label/usage/update görünüm modeli; locale metinleri route dışına taşıyacak şekilde genişletilir.
- `cms/src/features/contributors/components/contributor-table.tsx` -- rol chipleri, kullanım özeti, güncelleme ve i18n kolonları; mevcut DataTable durumlarını korur.
- `cms/src/features/contributors/components/contributor-form-dialog.tsx` ve `schema/contributor-schema.ts` -- create/edit çoklu rol alanı, zorunlu validation ve problem gösterimi.
- `cms/src/components/data/registry-toolbar.tsx`, `cms/src/components/data/pagination.tsx` -- shared layout/pagination primitive; route içinde yeniden kopyalanmaz.
- `cms/src/i18n/messages.ts` -- TR/EN contributor registry, rol ve form anahtarları.
- `cms/src/features/contributors/*.test.tsx`, `cms/tests` veya mevcut Playwright harness -- URL filtre, pagination, create/edit davranışı ve desktop screenshot regression.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java` -- yalnızca mevcut response kontratını doğrulamak için read-only referans; backend değişikliği bu story kapsamı değildir.

## Tasks & Acceptance

**Execution:**
- [x] API şema/model/query hook’unu paged registry kontratına bağla; query key filtre ve sayfayı içersin.
- [x] Contributors route’unu `RegistryToolbar` + `Pagination` ile URL-senkron filtre, rol select ve gerçek toplam kullanacak şekilde dönüştür.
- [x] Tabloyu roller, usage ve updatedAt kolonları ile TR/EN i18n’e taşı.
- [x] Create/edit formunu çoklu rol seçimi ve server validation ile güncelle; delete akışını koru.
- [x] Interaction/component testleri ve 390/768/1280/1440 desktop görsel regression kapsamı ekle.

**Acceptance Criteria:**
- Given 25’ten fazla contributor, when registry açılır, then yalnız istenen backend sayfası yüklenir ve toplam backend `totalItems` değeridir.
- Given ad veya rol filtresi, when değer değişir, then URL güncellenir, page 0’a döner ve sorgu backend’e gönderilir.
- Given create/edit dialogu, when rol seçilmeden gönderilir, then kayıt gönderilmez ve görünür alan hatası gösterilir.
- Given TR veya EN locale, when route/table/form render edilir, then kullanıcıya hard-coded karşılığı olmayan doğru locale metinleri görünür.
- Given supported viewport, when screenshot alınır, then toolbar arama alanı, filtreler, summary ve pagination yatay taşma olmadan shared contract’a uyar.

## Verification

**Commands:**
- `cd cms && npm run test` -- expected: contributor route/component tests pass.
- `cd cms && npm run build` -- expected: production TypeScript/build succeeds.
- `cd cms && npm run test:e2e:visual` -- expected: contributor registry baselines pass at required viewports.

## Suggested Review Order

**Registry boundary and URL state**

- Backend sayfalı registry sözleşmesini ve filtre parametrelerini ilk olarak inceleyin.
  [`contributor-admin.ts:123`](../../../../cms/src/features/contributors/api/contributor-admin.ts#L123)

- Route, URL filtrelerini ortak toolbar ve pagination ile backend sorgusuna bağlar.
  [`contributors.tsx:41`](../../../../cms/src/app/routes/contributors.tsx#L41)

- Query hook, filtreli sayfa anahtarını ve gerçek toplam metadatasını taşır.
  [`use-contributors.ts:66`](../../../../cms/src/features/contributors/queries/use-contributors.ts#L66)

**Registry presentation and forms**

- Tablo kolonları roller, kullanım, güncelleme tarihi ve eylemleri ortak veri tablosunda gösterir.
  [`contributor-table.tsx:140`](../../../../cms/src/features/contributors/components/contributor-table.tsx#L140)

- Form, görünen adı ve en az bir rolü aynı create/edit akışında doğrular.
  [`contributor-form-dialog.tsx:86`](../../../../cms/src/features/contributors/components/contributor-form-dialog.tsx#L86)

- Şema, rol listesini ve zorunlu seçim kuralını API modeliyle hizalar.
  [`contributor-schema.ts:9`](../../../../cms/src/features/contributors/schema/contributor-schema.ts#L9)

**Verification and supporting changes**

- Route testleri URL parametrelerini ve filtre değişiminde sayfa sıfırlamasını doğrular.
  [`contributors-route.test.tsx:131`](../../../../cms/src/features/contributors/contributors-route.test.tsx#L131)

- Form testleri boş rol gönderimini engelleyen görünür doğrulamayı korur.
  [`contributor-form-dialog.test.tsx:87`](../../../../cms/src/features/contributors/components/contributor-form-dialog.test.tsx#L87)

- Visual test, desteklenen dört viewport için registry görünümünü sabitler.
  [`contributors-registry.visual.spec.ts:35`](../../../../cms/e2e/visual/contributors-registry.visual.spec.ts#L35)

