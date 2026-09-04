---
title: 'Role göre contributor picker ve inline create geliştir'
type: 'feature'
created: '2026-09-04'
status: 'done'
baseline_commit: '7dcf3bbfd42e51128b78d8c80d2c8ea7c22524dd'
review_loop_iteration: 0
context:
  - 'C:/github/tellpalv2/AGENTS.md'
  - 'C:/github/tellpalv2/cms/AGENTS.md'
  - 'C:/github/tellpalv2/_bmad-output/specs/spec-contributor-workflow/SPEC.md'
  - 'C:/github/tellpalv2/be/docs/adr/ADR-0009-registry-read-pagination.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** İçerik detayındaki contributor atama dialogu tüm profiller arasında sınırlı bir son-12 araması yapıyor, rolü sonuçlardan bağımsız ayrı bir select ile seçtiriyor ve kişi bulunamadığında editörü registry sayfasına gönderiyor. Bu akış, CAP-3 role göre keşif ve CAP-4 aynı akışta oluşturup atama hedeflerini karşılamıyor.

**Approach:** Picker'ı seçilen rolü başlangıç bağlamı kabul eden database-backed registry sorgusuna bağla; sonuç satırını doğrudan seçilebilir yap; exact normalize ad eşleşmesi yoksa aynı dialog içinde rolü seçili gelen contributor oluşturma ve ardından mevcut içeriğe otomatik atama akışını ekle. Atama başarısız olursa oluşturulan profil ve bağlam korunarak yeniden deneme sunulsun.

## Boundaries & Constraints

**Always:** Picker bir rol bağlamıyla açılır ve rol profilde bulunan rollerle sınırlı kalır. Arama `q`, `role`, sıfır tabanlı `page` ve `size` ile `/api/admin/contributor-registry` üzerinden yapılır; istemci tüm contributorları yüklemez veya filtrelemez. Sonuç satırı ayrı contributor select'i olmadan seçilir. Inline create görünen adı ve başlatan rolü taşır; başlatan rol kaldırılamaz. Create sonrası aynı content, rol ve varsayılan dil kapsamına assignment otomatik denenir. Assignment hatasında oluşturulan contributor kaybolmaz ve retry eylemi görünür. Ham `sortOrder` alanı kullanıcıya gösterilmez; sıra backend tarafından belirlenir. Görünen metinler TR/EN i18n anahtarlarından gelir.

**Ask First:** Backend endpoint/envelope veya create/assignment response sözleşmesini değiştirmek; contributor bölümünü rol ve dil gruplarıyla yeniden düzenlemek; reorder veya bağımsız contributor detay ekranını bu story'ye almak; mobil/public API davranışını değiştirmek.

**Never:** Eski son-12 listesini role göre istemci belleğinde filtrelemek; registry sayfasına yönlendirerek inline create'i ertelemek; rol profilde bulunmadığı halde assignment göndermek; kullanıcıdan sayısal sıra almak; mevcut içerik detayındaki picker dışındaki assignment/persistence kurallarını sessizce değiştirmek.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Role-scoped search | Dialog `AUTHOR` rolüyle açılır, `q` yazılır | Yalnız AUTHOR profilleri backend sayfasından görünür ve satır seçilebilir | API problemi dialog içinde retry ile gösterilir |
| No match | Normalize edilmiş ad eşleşmez | Yazılan ad ve seçili rolle “oluştur ve ata” eylemi görünür | Boş veya yalnız boşluklu ad gönderilemez |
| Duplicate name | Backend normalize ad conflict döndürür | Mevcut contributor seçilebilir olarak sunulur, yeni kayıt korunmaz | Conflict detayındaki mevcut kimlik kaybolmaz |
| Create and assign | Geçerli ad, rol ve içerik vardır | Contributor oluşturulur, varsayılan scope ile otomatik atanır ve dialog kapanır | Assignment başarısızsa profile seçili kalır ve retry sunulur |
| Global/local scope | İçerikte localization yok veya seçili dil vardır | Global rol tüm dillere, NARRATOR seçili dile varsayılan atanır | Geçersiz scope alan hatası gösterilir |

</frozen-after-approval>

## Code Map

- `cms/src/features/contributors/components/assign-contributor-dialog.tsx:57` -- mevcut search, contributor/role select, scope ve assignment submit orkestrasyonu; picker'ın role-scoped ve inline create akışının ana giriş noktası.
- `cms/src/features/contributors/queries/use-contributors.ts:66` -- paged registry hook'u; picker için `q/role/page/size` sorgusunu yeniden kullan veya dar bir adapter çıkar.
- `cms/src/features/contributors/api/contributor-admin.ts:123` -- database-backed registry endpoint ve create/assign komutları; mevcut sözleşmeler korunacak.
- `cms/src/features/contributors/mutations/use-contributor-actions.ts:57` -- contributor create ve assignment mutation'ları; create sonrası assignment zinciri ve retry durumları burada koordine edilir.
- `cms/src/features/contributors/schema/content-contributor-schema.ts:1` -- assignment form varsayılanları, rol/scope doğrulaması ve duplicate guardları.
- `cms/src/features/contributors/components/content-contributor-panel.tsx:17` -- dialog'ı içerik detayı içinde açan mevcut çağrı; rol bağlamı ve callback burada sağlanır.
- `cms/src/features/contributors/components/contributor-form-dialog.tsx:86` -- registry create formundaki rol checkbox pattern'i; inline create görünümüne taşınabilecek ortak form davranışı.
- `cms/src/features/contributors/components/assign-contributor-dialog.test.tsx:90` -- mevcut picker empty, submit, scope ve duplicate testleri; yeni edge-case testlerinin ana yeri.
- `cms/src/features/contributors/contributors.integration.test.tsx:120` -- gerçek route/auth/fetch mock kalıbı; create-assignment zinciri için genişletilebilir.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContributorAdminController.java:95` -- registry ve assignment endpointlerinin read-only backend referansı; backend değişikliği bu story kapsamında değildir.

## Tasks & Acceptance

**Execution:**
- [x] `assign-contributor-dialog.tsx` -- role-scoped registry araması, doğrudan satır seçimi, inline create ve assignment retry akışını uygula.
- [x] `use-contributors.ts` ve `contributor-admin.ts` -- picker sorgusunu database-backed `q/role/page/size` kontratına bağla; legacy picker tüketicilerini bozma.
- [x] `use-contributor-actions.ts` ve `content-contributor-schema.ts` -- create→assign zinciri, varsayılan scope ve hata/duplicate bağlamını koru; sortOrder girişini kaldır.
- [x] `messages.ts` -- picker, inline create, retry ve edge-case metinlerini Türkçe/İngilizce ekle.
- [x] `assign-contributor-dialog.test.tsx`, integration ve Playwright coverage -- role filtering, direct selection, no-match create, retry ve scope edge-case'lerini doğrula.

**Acceptance Criteria:**
- Given içerik detayında “Yazar ekle” açılır, when editör arama yapar, then yalnız profilde AUTHOR rolü bulunan database-backed sonuçlar gösterilir.
- Given sonuç satırı görünür, when editör satırı seçer, then ayrı contributor select'i kullanmadan assignment formuna aktarılır.
- Given normalize ad eşleşmesi yoktur, when editör inline create'i onaylar, then contributor başlatan rolle oluşturulur ve aynı içerik/varsayılan scope'a otomatik atanır.
- Given create başarılı fakat assignment başarısızdır, when dialog açık kalır, then oluşturulan contributor ve rol korunur, retry eylemi görünür.
- Given form Türkçe veya İngilizce render edilir, when picker, hata, boş durum veya retry görünür, then hiçbir kullanıcı metni hard-coded locale karşılığı olmadan doğru i18n değerinden gelir.

## Suggested Review Order

**Dialog orchestration**

- Role context, inline create, duplicate recovery, and retry are coordinated here.
  [`assign-contributor-dialog.tsx:52`](../../../../cms/src/features/contributors/components/assign-contributor-dialog.tsx#L52)

**Backend-backed query contract**

- Picker requests preserve database filtering and pagination parameters.
  [`use-contributors.ts:66`](../../../../cms/src/features/contributors/queries/use-contributors.ts#L66)

**Assignment validation and API types**

- Client payload removes user-controlled ordering while retaining scope validation.
  [`content-contributor-schema.ts:1`](../../../../cms/src/features/contributors/schema/content-contributor-schema.ts#L1)

**Verification and localization**

- Edge cases and bilingual UI copy document the supported workflow.
  [`assign-contributor-dialog.test.tsx:85`](../../../../cms/src/features/contributors/components/assign-contributor-dialog.test.tsx#L85)

- Turkish and English picker states remain translation-backed.
  [`messages.ts:81`](../../../../cms/src/i18n/messages.ts#L81)

## Verification

**Commands:**
- `cd cms && .\\node_modules\\.bin\\vitest.cmd run src/features/contributors/components/assign-contributor-dialog.test.tsx src/features/contributors/contributors.integration.test.tsx` -- expected: picker and assignment flows pass.
- `cd cms && .\\node_modules\\.bin\\tsc.cmd -b` -- expected: TypeScript succeeds.
- `cd cms && .\\node_modules\\.bin\\vite.cmd build` -- expected: production build succeeds.
- `cd cms && .\\node_modules\\.bin\\playwright.cmd test e2e/contributors.spec.ts --workers=1` -- expected: role-scoped picker and inline create E2E passes.
