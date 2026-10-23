---
title: 'CMS Yayınla akışında otomatik görsel hazırlığı'
type: feature
created: '2026-10-23'
status: done
baseline_commit: 094d57e2bc5ef85e1abf88281669f9e0ae4de53c
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/cms/AGENTS.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/be/docs/adr/ADR-0023-reading-publication-orchestration.md'
  - '{project-root}/architecture.md'
  - '{project-root}/cms/docs/ui-standards.md'
  - '{project-root}/cms/docs/ui-regression-task-list.md'
---

<frozen-after-approval reason="2026-10-23 kullanıcı: Tamam bunu yapalım">

## Intent

**Sorun:** Hazırlama motoru çalışıyor; normal CMS yüklemeleri elle kaynak kabulü/bağlama olmadan bu hatta ulaşamıyor. Hazırlanan dosyalar CMS'den görülemiyor.

**Yaklaşım:** Kullanıcı mevcut yükleme ve tek Yayınla komutunu kullanır. Kaynak kabulü, hazırlık, doğrulama ve uygun görsellerin benimsenmesi otomatik yürür. Aynı yayın kartı durumu, hatayı, yeniden denemeyi ve hazırlanan kapak/sayfa önizlemesini gösterir.

## Boundaries & Constraints

**Daima:** Yerel/test ve varsayılan kapalı kapılar; gerçek orijinaller korunur. Admission/storage/codec I/O transaction dışında, bağlama ve benimseme kısa kilit altında yapılır. Generation/lease/dil/profil/kaynak karşılaştırmaları korunur. Yeni optimize okuma erişimi ilk doğrulanmış LIVE öncesinde açılmaz. Editoryal PUBLISHED tek başına görseller hazır diye gösterilmez. Eski uygun LIVE yalnız görselleri korur. Açık retry vardır; değişmeyen kabul edilmiş kaynak/profil yeniden kodlanmaz. CMS mevcut i18n ve bileşenleri kullanır.

**Önce sor:** Canlı etkinleştirme, dağıtım veya bulut yazısı kapsamı değişirse.

**Asla:** Firebase/canlı DB/storage, orijinal dosya değişikliği, push, backfill/silme; yeni codec/cihaz/corpus araştırması; Flutter/full reader/audio/session/paywall değişikliği. Mevcut legacy public/listing metadata görünürlüğü bu yerel optimize erişimle karıştırılmaz ve topluca değiştirilmez.

## I/O & Edge-Case Matrix

| Senaryo | Girdi/durum | Beklenen davranış | Hata |
|---|---|---|---|
| Normal yayın | CMS'den yeni JPEG kapak ve PNG sayfa | Tek Yayınla; otomatik admitted bağlar, JPEG/WebP doğrulanmış LIVE ve önizleme | Manuel admit/rebind yok |
| Tekrar | Aynı admitted kaynak/profil, metin değişikliği veya tekrar Publish | Doğrulanmış iş tekrar kullanılır | Tekrarlı dönüşüm yok |
| İlk hata | Bozuk/desteklenmeyen yeni kaynak | Hazır sonucu/optimize erişim/önizleme yok; açık hata ve retry | Sahte başarı yok |
| Değişiklik hatası | Eski uygun LIVE, başarısız yeni aday | Önceki görsellerin önizlemesi sürer, eski olduğu belirtilir | Yeni aday benimsenmez |
| Yarış/kesinti | Admission sırasında edit/archive/deactivate/profil değişimi; restart | Güncel seçim ezilmez; stale iş benimsenmez; kalıcı niyet toparlanır | Orphan kanıtı korunur |
| Önizleme güvenliği | Yanlış dil/rol/sayfa/key, eski tuple, bozuk byte veya yetkisiz istek | Ret; yalnız aynı uygun LIVE'ın doğrulanmış rendition'ı | private,no-store; kaynak yolu/URL sızmaz |
| CMS yaşamı | Dirty form, dil değişimi, geciken byte, kapanış | Dirty/dil korunur; stale görüntü çizilmez; istek iptal/object URL revoke | Ağ hatası görünür ve açık tekrar |

</frozen-after-approval>

## Code Map

- `be/src/main/java/com/tellpal/v2/content/application/ContentPublicationService.java`: mevcut Publish transaction/intent korunur; burada dış I/O yapılmaz.
- `content/application/publication/ReadingPublicationService.java` (`be/src/main/java/com/tellpal/v2/` altında): claim sonrası admission; `ReadingImageSnapshotAssembler`, `JdbcReadingPublicationStore` ve domain cover/page bağları aynı kilitli transaction'da karşılaştırılıp yenilenir. İç rebind genel `guard.changed` ile kendi lease'ini bozmamalı. Aynı ID tekilleştirilir; tüm rol profillerinin sınırları karşılanır.
- `asset/api/ReadingSourceAdmissionApi.java`, `asset/application/reading/ReadingSourceAdmissionService.java`: admitted ID salt DB kanıtıyla yeniden kullanılabilir; ham ID mevcut pinned kopya/bağımsız reread yolunu izler. Yeni kuyruk gerekmez. Yerel mutlak yol I/O profilleri korunur.
- `asset/api/ReadingImageDeliveryApi.java`: descriptor ve `readRendition`; provider/objectKey/generation dış DTO'ya taşınmaz. `ReadingPublicationAdminController` yanında admin proof yakalama/öncesi-sonrası kontrol; mobile free erişimini gevşetme ve user internal writer import etme. Mevcut admin binary taşıma örüntüsünü, açık byte/pixel/memory/deadline/toplam limitlerini kullan.
- `cms/src/features/contents/components/publication-actions.tsx`, `queries/use-reading-publication.ts`, `api/content-admin.ts`: exact-language polling var. `apiClient.download` bearer/refresh/AbortSignal ile ayrı rendition Blob önizlemesi sağlar; MediaAsset content-token hook'u rendition için kullanılmaz. Form reset/detail invalidation ile dirty alanları ezme.
- `cms/e2e/visual/content-detail.visual.spec.ts`: sabit session/zaman/light/reduced-motion,1280/1440; önceki LIVE ekranı dış run `cms-before/content-detail-reading-live-laptop.png` olarak incelendi. Kompakt mevcut yayın kartı + dialog; büyük dashboard/rail ekleme. Ürün metninde admission/profile/revision/ID jargonunu kaldır. JPEG: değişmeden korundu ve eşit boyut; küçülmeyen sonucu küçülmüş gösterme.
- `be/src/test/java/com/tellpal/v2/content/publication/ReadingPublicationIntegrationTest.java`: ham kaynak artık otomatik kabul edilir; SOURCE_NOT_ADMITTED hata fixture'ı gerçek unsupported/bozuk kaynak olmalı. Legacy görünürlük testi korunur; yeni optimize erişim LIVE gerektirir.
- Korunan 88 dosya ve run: `/private/tmp/tellpal-auto-publish-baseline.json`, `/private/tmp/tellpal-auto-publish-run-path`. Bunlara dokunma/commit etme. Önceki 41 dosyalık kanıtlar salt okunur. Yalnız sahip olunan Colima `tellpal-image-local` kullanılabilir; DOCKER_HOST `unix:///Users/gurhankucuk/.colima/tellpal-image-local/docker.sock`, socket override `/var/run/docker.sock`. Mevcut ortam bu görev için başlatıldı; varsayılan Docker ortamına dokunma.

## Tasks & Acceptance

**Execution:**
- [x] `asset/.../reading`, `content/.../publication` -- otomatik kabul, koşullu rebind, restart/reuse ve hata korumasını tamamla.
- [x] `content/web/admin/ReadingPublicationAdminController.java` ve application servisi -- güvenli manifest/bytes + kaynak/çıktı boyutları; OpenAPI Türkçe.
- [x] `cms/src/features/contents/` -- dürüst durum, tek Publish, retry, doğrulanmış önizleme ve lifecycle.
- [x] Backend/CMS testleri -- matrisi gerçek çalışan testlerle doğrula; normal upload/Publish yolu için gerçek Spring/PG/Flyway ve CMS kabulü ekle. Yalnız storage leaf yerel disk/sentetik auth olabilir; manuel admission/rebind veya test preparation endpoint'i kullanma.
- [x] `be/docs/adr/ADR-0023-reading-publication-orchestration.md`, `architecture.md`, `be/docs/project-memory.md`, `cms/docs/ui-standards.md` ve yeni kısa kanıt -- otomatik yerel akışı ve sınırlarını güncelle.

**Acceptance Criteria:**
- Given yeni yerel hikâye, when normal CMS upload/save/tek Publish çalışır, then gerçek JPEG ve PNG'den hazırlanan çıktılar dialog'da decode edilir; byte boyutları backend manifestiyle aynıdır; orijinaller değişmez.
- Given varsayılan veya production profil, when yeni özellik istenir, then kapalıdır; metadata görünürlüğü veya üretim etkinleştirmesi iddiası yoktur.
- Given değişen CMS yüzeyi, when1280/1440 ve klavye/dirty/dil kontrolleri yürür, then mevcut tasarım ve çalışma korunur.

## Spec Change Log

## Design Notes

Tek kullanıcı hedefi katmanlar arasında birlikte tamamlanır. Onay konuşmada verilmiştir; ek revizyon onayı veya ayrı Optimize düğmesi eklenmez. Kaynak dosya korunur; yalnız seçilen content bağları backend-owned yeni kimliğe koşullu geçirilir. Mevcut P1/P2/P3 kanıtı tarihsel kapsamıyla korunur. Bilinen SDK mutlak süre ve AMD64 üretim yeterlilik açıkları bu işte kapanmış sayılmaz. Skill'in UI search script hedefi kurulu değil; mevcut CMS standardı ve erişilebilir UX kuralları uygulanır. Araştırmada doğrulanmamış nokta çıkarsa mevcut kodla sınayıp en dar uyumlu çözümü seç; kapsamı büyütme.

## Verification

- `be/./mvnw verify`: gerekli PostgreSQL/Modulith, yeni matris ve gerçek upload/yayın testleri; native kabul araçları mevcut resmi libwebp1.6.0 ile, sessiz skip yok.
- `cms/npm run build`, `npm run test -- --run`: değişen sözleşme/bileşen testleri.
- `cms/npm run test:e2e:visual`: değişen yüzey1280/1440; gerçek yerel backend ile normal upload/yayın/önizleme kabulünü ayrıca çalıştır. Ham medya/log/oturum repo dışında; yalnız temiz kısa sonuç Git'e girer.

## Uygulama doğrulama kaydı

- Yedi matris satırının çalışan test eşlemesi ve sınırlar [kısa kanıtta](../../be/docs/evidence/cms-publish-image-preparation/verification.md) yer alır.
- Tam Maven verify başarılı: 603 Surefire +42 Failsafe; 616 test çalıştı, kapsam dışı 29 mevcut opt-in skip var. Yeni yayın/önizleme/native/CMS kabul testlerinde skip yoktur.
- CMS build ve 391 test başarılı; değişen1280/1440 yüzeyin8 görsel karşılaştırması geçti. Son hedefli admin kabulü4/4; gerçek Chrome normal upload/save/tek Publish/önizleme kabulü1/1 geçti.
- Korunan88 başlangıç dosyası değişmedi. Push, dağıtım veya canlı yazı yapılmadı; değişiklikler yerel commit ile kaydedilir.
- Formal review yaması: değişmemiş görseller için kilit altında açık preserve işareti, PREPARING metin kaydında aynı işin korunması, önizleme retry'ında aynı dilin güncel LIVE durumuna geçişi tamamlandı. I/O sırasında archive/deactivate retleri, gerçek download URL/bearer/AbortSignal sözleşmesi ve aynı dil revision/liveGeneration/digest uyuşmazlıkları çalışan testlerle doğrulandı.
- Yama sonrası9 hedefli backend sınıfında93/93 test ve Modulith geçti; admin6/6, gerçek CMS Chrome1/1, CMS92 dosyada400/400, build ve8/8 görsel karşılaştırması başarılı. Tam backend suite'i yeniden açılmadı. Test adları ve ham log konumları kısa kanıtta güncellendi.

## Önerilen inceleme sırası

**Otomatik hazırlık**

- Tek Yayınla sonrasında kaynakları kabul eder ve yalnız güncel nesli benimser.
  [ReadingPublicationService.java:96](../../be/src/main/java/com/tellpal/v2/content/application/publication/ReadingPublicationService.java#L96)

**Metin ve görsel seçimi**

- Metin kaydında güncel görsel bağı korunur; açık görsel seçimi uygulanır.
  [ContentManagementService.java:167](../../be/src/main/java/com/tellpal/v2/content/application/ContentManagementService.java#L167)

- Sayfa metni eski görsel kimliğini geri yazamaz.
  [StoryPageManagementService.java:121](../../be/src/main/java/com/tellpal/v2/content/application/StoryPageManagementService.java#L121)

**Doğrulanmış önizleme**

- Depo okumasından önce ve sonra aynı uygun görselleri doğrular.
  [ReadingPublicationPreviewService.java:51](../../be/src/main/java/com/tellpal/v2/content/application/publication/ReadingPublicationPreviewService.java#L51)

**CMS akışı**

- Durum, retry ve önizleme mevcut yayın bölümünde birleşir.
  [publication-actions.tsx:48](../../cms/src/features/contents/components/publication-actions.tsx#L48)

- Kapak/sayfa seçimini, doğru byte ölçülerini ve iptal yaşamını gösterir.
  [reading-publication-preview.tsx:27](../../cms/src/features/contents/components/reading-publication-preview.tsx#L27)

**Kabul kanıtı**

- Normal HTTP ve gerçek tarayıcı kabulünü yerel depolamayla doğrular.
  [ReadingPublicationAdminIntegrationTest.java:36](../../be/src/test/java/com/tellpal/v2/content/publication/ReadingPublicationAdminIntegrationTest.java#L36)

- Çalışan testleri, matris eşlemesini ve üretim sınırlarını kaydeder.
  [verification.md:1](../../be/docs/evidence/cms-publish-image-preparation/verification.md#L1)
