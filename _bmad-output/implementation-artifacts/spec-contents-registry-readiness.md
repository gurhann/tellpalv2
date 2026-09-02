---
title: 'Contents registry readiness'
type: 'feature'
created: '2026-09-02'
status: 'in-progress'
baseline_commit: '74db778500e5c1cb85ca1c452b33dbd2ade083a3'
review_loop_iteration: 0
context:
  - 'AGENTS.md'
  - 'cms/AGENTS.md'
  - 'be/docs/admin-api-rules.md'
  - 'cms/docs/ui-standards.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Contents listesi bugün tüm içeriği tek seferde, kimlik sırasıyla getiriyor ve
editöre seçili dilde mobil yayını engelleyenleri söylemiyor. Hikâye için açıklama ve lokalize
kapak da yayın koşulu olmalı; liste ile publish komutu birbirini çeliştirmemeli.

**Approach:** İçerik modülüne sayfalı, dil-bağlamlı bir registry read API ekle; STORY
yayınlanabilirliğini merkezi policy ile üret. CMS Contents ekranını bu API, URL'de korunan
filtreler ve erişilebilir engel ayrıntılarıyla yeniden kur.

Yeni endpoint `GET /api/admin/content-registry` olur. Yanıt, `items`, `page`, `size`,
`totalItems` ve her öğede seçili dile ait `title`, `readiness`, `blockers`, `pageCount`,
`lastEditedAt` alanlarını taşır.

## Boundaries & Constraints

**Always:** Varsayılan dil `tr`; arama seçili dil başlığı, `externalKey` ve sayısal ID'de
çalışır; varsayılan sıra en güncel değişikliktedir. Durumlar sadece `ACTION_REQUIRED`,
`READY_TO_PUBLISH`, `PUBLISHED` olur. STORY için aktif içerik, seçili lokalizasyon, başlık,
açıklama, lokalize kapak, en az bir sayfa, her sayfada lokalizasyon/metin/ses/görsel ve
`COMPLETED` işleme gerekir. Aynı policy publish komutu ve registry sorgusunda kullanılır.
`lastEditedAt`, content, seçili content localization, story page ve seçili story-page
localization içindeki en güncel timestamp'tir. Registry endpoint'i `language` zorunlu,
`type`, `readiness`, `q`, `page`, `size` opsiyonel parametrelerini kabul eder; `size`
varsayılan 25, en çok 100'dür.

**Ask First:** STORY dışındaki türlere ait yayınlanabilirlik kuralları; dashboard tür×dil
sağlık matrisi; mevcut olmayan seçili dil lokalizasyonu için ithalat iş akışı.

**Never:** Mevcut `GET /api/admin/contents` sözleşmesini değiştirme; içerik listesini
istemcide topluca filtreleme/sıralama; hover'ı engel ayrıntısının tek erişim yolu yapma;
satır içi düzenleme veya bu teslimde dashboard ekleme.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
| --- | --- | --- | --- |
| Hazır hikâye | Aktif STORY; seçili dilde tüm alanlar tamam, lokalizasyon draft/completed | `READY_TO_PUBLISH`, boş blocker listesi | N/A |
| Yayındaki hikâye | Hazır hikâye; lokalizasyon published/completed | `PUBLISHED`, boş blocker listesi | N/A |
| Birden çok eksik | Kapak eksik, 7. sayfada ses eksik | `ACTION_REQUIRED`; iki blocker birlikte, sayfa no ile döner | Popover tümünü klavye/tap ile açar |
| Arama/filtre | `language=tr`, isteğe bağlı type/readiness/q/page/size | Sunucu sayfalı, güncelten eskiye sonuç döner | Geçersiz dil veya page/size 400 doğrulama hatası |
| Eski tüketici | `GET /api/admin/contents` | Mevcut dizi şeması aynen korunur | N/A |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/content/domain/ContentPublicationPolicy.java` -- mevcut STORY sayfa doğrulaması; kapak/açıklama ve yeniden kullanılabilir readiness sonucu buraya yakın bir domain policy'de merkezileşir.
- `be/src/main/java/com/tellpal/v2/content/domain/ContentLocalization.java` ve `StoryPageLocalization.java` -- açıklama/kapak ile sayfa varlıklarının readiness kaynakları; hepsi `BaseJpaEntity.updatedAt` miras alır.
- `be/src/main/java/com/tellpal/v2/content/application/AdminContentQueryService.java` -- bugün tüm admin içeriklerini ID artan sırayla map'ler; yeni registry query use-case'i burada veya ayrık application service'te yer alır.
- `be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/SpringDataContentRepository.java` -- admin read fetch sorguları; registry için sayfalama ile collection fetch'i ayıran read metotları gerekir.
- `be/src/main/java/com/tellpal/v2/content/web/admin/ContentAdminController.java` -- eski list endpoint'i korunur; yeni registry endpoint'i OpenAPI ve validation ile eklenir.
- `be/src/main/java/com/tellpal/v2/content/api/AdminContentQueryApi.java` -- yeni read contract için public module API genişletme noktası.
- `be/src/test/java/com/tellpal/v2/content/web/admin/ContentAdminIntegrationTest.java` ve `ContentPublicationAdminIntegrationTest.java` -- API ve publish policy entegrasyon örnekleri.
- `cms/src/features/contents/api/content-admin.ts` -- Zod API şemaları ve admin HTTP çağrıları.
- `cms/src/app/routes/contents/index.tsx` -- mevcut client-side filtreli registry; URL tabanlı server query ekranına dönüşür.
- `cms/src/features/contents/components/content-list-table.tsx` -- yeni row sütunları ve blocker trigger'ı.
- `cms/src/app/routes/contents/detail.tsx`, `cms/src/features/contents/components/localization-tabs.tsx` -- liste-detal `language` URL bağlamının başlangıç sekmesine aktarılacağı yer.
- `cms/src/components/data/registry-toolbar.tsx` -- korunacak ortak toolbar slot sözleşmesi.
- `cms/e2e/visual/registry-toolbar.visual.spec.ts` -- viewport matrisiyle genişletilecek görsel koruma.

## Tasks & Acceptance

**Execution:**
- [ ] `be/src/main/java/com/tellpal/v2/content/{api,application,domain,infrastructure,web/admin}` -- registry request/response contract'ını, sayfalı read yolunu ve tüm STORY engellerini üreten ortak readiness policy'yi ekle; publish policy bu policy'yi zorunlu kullansın.
- [ ] `be/src/test/java/com/tellpal/v2/content/...` -- bütün durumları, çoklu blocker'ı, yeni kapak/açıklama önkoşullarını, sıralama/sayfalama/arama validation'ını ve eski liste uyumluluğunu test et.
- [ ] `cms/src/features/contents/{api,queries,model,components}` -- registry şeması, query anahtarı, durum rozetleri, blocker popover ve server sayfalı tabloyu ekle.
- [ ] `cms/src/app/routes/contents/{index,detail}.tsx` ve `cms/src/features/contents/components/localization-tabs.tsx` -- URL filtrelerini ve liste-detal dil devrini uygula; ortak `RegistryToolbar` kullanımı korunur.
- [ ] `cms/e2e/**/*.spec.ts`, `cms/e2e/visual/registry-toolbar.visual.spec.ts` -- etkileşim ve 390/768/1280/1440 görsel regresyonları ekle/güncelle.
- [ ] `be/docs/admin-api-rules.md` -- doğrulanmış endpoint ve STORY yayın önkoşullarını kanıtlandıktan sonra güncelle.

**Acceptance Criteria:**
- Given `tr` ile açılan Contents, when kullanıcı filtre veya arama değiştirir, then URL ve server isteği aynı bağlamı taşır ve sonuçlar son güncellenene göre sayfalanır.
- Given bir STORY eksiktir, when editör engel düğmesini açar, then tüm eksikler görünür ve satıra gitmez.
- Given açıklaması veya lokalize kapağı olmayan STORY, when publish istenir, then backend yayınlamayı reddeder; registry aynı öğeyi `ACTION_REQUIRED` gösterir.
- Given bir yayınlanabilir STORY, when published/completed olur, then registry `PUBLISHED`; değilse `READY_TO_PUBLISH` gösterir.
- Given önceki CMS tüketicileri, when eski contents endpoint'ini çağırır, then response şeması değişmez.

## Spec Change Log

- 2026-09-02: Keşifte doğrulanan mevcut işleme akışı korunarak `COMPLETED`, publish komutunun değil mobil dağıtım durumunun koşulu olarak uygulandı. Editoryal alan engelleri ortak STORY policy'de, işleme engeli registry read modelinde hesaplanır; böylece yayın sonrası işleme akışı bozulmaz.
- 2026-09-02: Tüm registry graph'ını application memory'de filtreleme/sıralama/sayfalamanın anti-pattern olduğu kaydedildi. Kalıcı yön `ADR-0009`: row-local kriterlerde database `Specification`, readiness filtrelerinde pagination öncesi dedicated projection/read model.

## Design Notes

Registry durumu backend'de hesaplanır; aksi halde sayfa koleksiyonunu taşımayan mevcut listeden
farklı CMS yüzeyleri çelişkili "hazır" sonuç üretir. Sayfalama, önce registry adaylarını
deterministik olarak seçip sonra yalnızca o sayfanın gerekli story verisini yüklemelidir.

## Verification

**Commands:**
- `cd be; .\mvnw test` -- expected: backend testleri geçer.
- `cd cms; npm run test` -- expected: CMS birim/entegrasyon testleri geçer.
- `cd cms; npm run build` -- expected: TypeScript ve production build geçer.
- `cd cms; npm run test:e2e:visual` -- expected: dört viewport'ta Contents baselines geçer.
