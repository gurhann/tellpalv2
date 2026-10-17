---
title: 'P3: okuma görsellerinin otomatik yayını ve CMS geri bildirimi'
type: 'feature'
created: '2026-10-17'
status: 'done'
baseline_commit: 'ed4bf60c30d806518fae3394b2d6e0fdfb1b70d5'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/cms/AGENTS.md'
  - '{project-root}/_bmad-output/implementation-artifacts/image-optimization/p3-preimplementation-investigation.md'
---

<frozen-after-approval reason="kullanıcının onayladığı P3 kapsamı">

## Amaç

**Sorun:** Yayınla işlemi editoryal durumları değiştiriyor; ancak P2 görsellerinin güncel yayın nesline kabulünü ve hazırlık geri bildirimini sağlamıyor.

**Yaklaşım:** STORY/READING görsellerini tam dil eşleşmesiyle, kalıcı iş kayıtları üzerinden hazırlamak, uygun ve doğrulanmış sürümü otomatik olarak LIVE durumuna almak, CMS'de bekleme/hata/yeniden deneme durumlarını göstermek. Her sürüm için ayrıca onay istenmez.

## Kapsam ve kısıtlar

**Her zaman:** P1, ADR-0021/0022 ve zorunlu inceleme belgesine uy. Yalnız açıkça seçilmiş yerel/test kapsamlarını ve profillerini etkinleştir; özellik varsayılan olarak ve üretimde kapalıdır. Yeni iç LIVE uygunluğu, mevcut dağıtım akışından ayrıdır. Yeni aday başarısız olduğunda uygun önceki LIVE sürümünü koru; ilk LIVE kabulünden önce doğrulama yap. Değişiklikleri ve yayına kabulü aynı kilitleme düzeninde sırala. Deneme ve yeniden başlatma kontrolleriyle eski işlemlerin güncel sonucu değiştirmesini engelle. Dış I/O işlemlerini DB transaction dışında tut. İlgisiz mevcut değişiklikleri koru.

P3 yalnız değişmez kaynak kabulünden (source admission) geçmiş yeni kimlikleri kullanır. Seçilen orijinalleri yalnız backend'in yazabildiği ayrılmış bir alana kopyala ve doğrula; yeni orijinal kimlikleri oluştur. Eski orijinalleri koru; içerik bağlantıları yeni kimliklere açıkça değiştirilmelidir. Kabul edilmemiş kimlikleri reddet. Mevcut P2 çağrılarını koru; SDK'nın koşullu yazma ve belirli nesne sürümünü okuma işlemlerini kullan. Kaynak baytları değişirse yeni bir kabul edilmiş kimlik gerekir.

**Önce onay al:** Üretim profilleri veya etkinleştirme, kapsamın başka görsel rollerine açılması, hangi kaynağın esas alınacağını belirleyen kuralların değiştirilmesi, yeni medya erişimi, bulut çalıştırmaları, dağıtım veya toplu geriye dönük işleme (backfill).

**Yapma:** Depolama için process/IPC katmanı ekleme. Kabul edilmiş kaynaklara imzalı yazma veya koşulsuz yükleme açma. Eski kaynakları otomatik olarak değişmez ilan etme ya da yeniden bağlama. Her sürüm için ek onay isteme. Silme yapma. P4 tüketici geçişini bu kapsama alma. Yeni oturum/cache/saklama politikası ekleme veya normal mobil dağıtımın tamamlandığını söyleme.

## Girdi/çıktı ve sınır durumları

| Senaryo | Beklenen sonuç |
| --- | --- |
| İlk Yayınla | Yayınlama talebini kalıcı kaydet; hazırlığı asenkron başlat; yalnız güncel ve tam eşleşen doğrulanmış kanıtla otomatik yayına kabul et. |
| Tekrarlanan istek / yeniden başlatma | Kalıcı işleri aynı istekte aynı sonucu verecek şekilde sürdür; talebi kaybetme ve LIVE kabulünü çoğaltma. |
| Başarısız veya geçersiz yeni sürüm | Uygun R1'i koru; hatayı ve güncel gereksinimler için açık yeniden deneme seçeneğini göster. |
| Değişiklik / eski işin tamamlanması | Yayın neslini ilerlet veya geçersiz kıl; eski işin güncel nesle bağlanmasını ve yayına kabulünü reddet. |
| Arşivleme / devre dışı bırakma / manuel durum değişikliği | Uygunluğu geri çek; geçmiş kanıtı koru. Manuel durum alanları LIVE oluşturamaz. |
| Kaynak / bütünlük değişikliği | Kaynak kabulü ve erişilebilirlik sayaçları (epoch) ile eski kanıtın kabulünü engelle. Orijinale sessizce dönme; yalnız durum bilgisine dayanarak doğrulanmış sayma. |
| CMS durum sorgulaması | Kaydedilmemiş girdileri ve seçili dili koru. İş son duruma ulaştığında periyodik sorgulamayı durdur; yalnız tanımlı, hassas bilgi içermeyen hata kodlarını göster. |
| Özellik kapalı / üretim | Mevcut davranışı koru; yeni hazırlık veya LIVE kabulü yapma. |

</frozen-after-approval>

## Kod haritası

Java yolları `be/src/main/java/com/tellpal/v2/`, CMS yolları `cms/src/` altındadır. Zorunlu inceleme belgesi destekleyici dosyaları ve testleri de listeler.

- `content/application/ContentPublicationService.java:42` — transaction içindeki komut giriş noktası; kaynak görüntüsü alınırken REQUIRES_NEW kullanılır.
- `asset/application/reading/ReadingImagePreparationService.java:38,91` — prepare/find depolama I/O'su yapar; bu çağrıları transaction dışında kullan.
- `content/application/ContentManagementService.java:101`, `StoryPageManagementService.java:50` — ortak değişiklik koruması ve geçersiz kılma noktaları.
- `features/contents/components/publication-actions.tsx:104`; `localization-tabs.tsx:75` — hazırlık geri bildirimi; duruma bağlı form anahtarları kaydedilmemiş girdileri kaybettirebilir.

## Görevler ve kabul

- [x] `be/src/main/resources/db/migration/V36__reading_publication_orchestration.sql` — kaynak kabulü, erişilebilirlik ve içerik kapsamı/nesli/LIVE kayıtları için mevcut yapıya ek tablolar oluştur; geçmiş verileri kendiliğinden doğrulanmış sayma.
- [x] `asset/api/ReadingSourceAdmissionApi.java`, `asset/application/reading/ReadingSourceAdmissionService.java` — belirli nesne sürümünü doğrulayarak kaynak kabulü yap; sahipsiz kalan deneme nesneleri ve uygunluk kayıtlarını inceleme belgesindeki düzene göre kalıcı tut.
- [x] `asset/infrastructure/storage/FirebaseStorageAssetClient.java`, `AssetStorageClientRegistry.java`, `asset/application/AssetRegistryService.java` — ayrılmış kaynak alanını koru; diğer alanlardaki mevcut yükleme davranışını sürdür.
- [x] `asset/api/ReadingPublicationReadyApi.java` ve mevcut reading API/service/work/profile/store dosyaları — kabul edilmiş kaynakların belirli sürümlerini kullan; transaction dışında yeniden doğrula; yayına kabulde yalnız DB işlemleriyle tam eşleşme kontrolü yap.
- [x] `content/api/ReadingPublicationApi.java`, `content/application/publication/ReadingPublicationService.java` ve inceleme belgesindeki domain/store/reconciler/config dosyaları — kalıcı nesil, iş sahiplenme, toparlama, yeniden deneme ve uygunluk akışını kur. Profiller açıkça verilmeli; varsayılan profil olmamalı.
- [x] `content/application/ReadingImageSnapshotAssembler.java` ve ilgili değişiklik/repository servisleri — ortak kilitleme ve geçersiz kılma uygula; yalnız metadata değiştiğinde görselleri yeniden kullan; boş sayfa kümesini geçersiz say. Korunan LIVE kimliğini yeni adaydan bağımsız tut.
- [x] `content/web/admin/ReadingPublicationAdminController.java` ve mevcut Yayınla yanıtı — kimliği doğrulanmış kullanıcıya özelliğin açık/kapalı bilgisini, durumu ve yeniden denemeyi sun; tanımlı hata kodlarını belgele.
- [x] `features/contents/api/content-admin.ts`, `features/contents/queries/use-reading-publication.ts` ve ilgili model/mutation/component dosyaları — doğru durum bilgisi, yeniden deneme, kaydedilmemiş form verilerinin korunması ve sorgu önbelleğinin geçersiz kılınmasını sağla.
- [x] `be/src/test/java/com/tellpal/v2/content/publication/ReadingPublicationIntegrationTest.java` ve ilgili yarış/HTTP/kaynak/CMS testleri — gerçekten kodlanmış ve kaydedilmiş görsellerle, PostgreSQL üzerinde yukarıdaki senaryoları doğrula.
- [x] `be/docs/adr/ADR-0023-reading-publication-orchestration.md`, proje hafızası, mimari ve kanıt kayıtları — tamamlanan sınırlı kapsamı ve açık kalan üretim/P4 koşullarını kaydet.

**Kabul ölçütü:**
- Mevcut test verilerinde, özellik kapalıyken veya yeni iç LIVE kimliği değişirken normal dağıtım, listeleme ve tester sözleşmeleri değişmemelidir.

## Spec değişiklik kaydı

- 2026-10-17: Kullanıcının isteğiyle belge Türkçeye çevrildi. Onaylanan kapsam, kısıtlar, beklenen sonuçlar ve tamamlanma durumu korunmuştur. Teknik kimlikler, dosya yolları, komutlar ve inceleme bağlantıları değiştirilmemiştir. Onaylı İngilizce metin `c63106171daca9fa931bbe2c4dec17f73bba9d0b` commit'inde korunur; bu çeviri yeni uygulama veya üretim onayı değildir.

## Doğrulama

Kaynak kabulü, yayına kabul, değişiklik, yeniden başlatma ve admin akışlarının odaklı testlerini çalıştır. `be/` altında `bash ./mvnw verify` çalıştır; modül sınırlarını, gerçek kodlama/depolama işlemlerini ve transaction içinde dış I/O yapılmadığını doğrula. CMS için Vitest, içerik Playwright akışı, 1280–1440 genişliklerindeki görsel testler ve `npm run build` çalıştır. Hataları ve sonuçları sakla; bulut çalıştırması yapma veya üretime hazır olunduğunu söyleme.

## Önerilen inceleme sırası

**Yayınla ve yayına kabul**

- Mevcut Yayınla giriş noktasında kalıcı talebin nasıl kaydedildiğini incele.
  [ContentPublicationService.java:48](../../be/src/main/java/com/tellpal/v2/content/application/ContentPublicationService.java#L48)

- Transaction dışında ilerleyen akışı ve yalnız güncel kanıtın kabulünü incele.
  [ReadingPublicationService.java:93](../../be/src/main/java/com/tellpal/v2/content/application/publication/ReadingPublicationService.java#L93)

- Tam runtime profil kimliği değiştiğinde yayın neslinin nasıl yenilendiğini incele.
  [JdbcReadingPublicationStore.java:60](../../be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/publication/JdbcReadingPublicationStore.java#L60)

**Değişmez medya ve kalıcı kayıtlar**

- Mevcut orijinalleri yeniden bağlamadan doğrulanmış yeni kaynak kimliklerinin nasıl üretildiğini incele.
  [ReadingSourceAdmissionService.java:44](../../be/src/main/java/com/tellpal/v2/asset/application/reading/ReadingSourceAdmissionService.java#L44)

- Doğrulama sayaçlarını, kaydedilmiş baytların kontrolünü ve yayına kabul korumasını incele.
  [ReadingPublicationReadyService.java:65](../../be/src/main/java/com/tellpal/v2/asset/application/reading/ReadingPublicationReadyService.java#L65)

- Geçmiş LIVE kimliği ile yeni adayın gereksinimlerini ayıran tabloları incele.
  [V36__reading_publication_orchestration.sql:36](../../be/src/main/resources/db/migration/V36__reading_publication_orchestration.sql#L36)

**Değişiklikler ve CMS geri bildirimi**

- İçerik değişikliklerinin yayın kapsamı değişmeden önce nasıl kilitlendiğini incele.
  [ReadingPublicationMutationGuard.java:33](../../be/src/main/java/com/tellpal/v2/content/application/publication/ReadingPublicationMutationGuard.java#L33)

- Hazırlık durumlarının ve açık yeniden denemenin nasıl sunulduğunu incele.
  [publication-actions.tsx:64](../../cms/src/features/contents/components/publication-actions.tsx#L64)

- Dil bazlı durum sorgusunun kaydedilmemiş form girdilerinden nasıl ayrıldığını incele.
  [use-reading-publication.ts:11](../../cms/src/features/contents/queries/use-reading-publication.ts#L11)

**Testler ve destekleyici kayıtlar**

- Gerçek Spring zamanlayıcılarıyla Yayınla işleminden LIVE'a otomatik geçişi doğrulayan testi incele.
  [ReadingPublicationSchedulerIntegrationTest.java:57](../../be/src/test/java/com/tellpal/v2/content/publication/ReadingPublicationSchedulerIntegrationTest.java#L57)

- Korunan kimlik, bozulma, eşzamanlılık, kaynak kabulü ve runtime değişikliği testlerini incele.
  [ReadingPublicationIntegrationTest.java:37](../../be/src/test/java/com/tellpal/v2/content/publication/ReadingPublicationIntegrationTest.java#L37)

- Etkinleştirmenin açıkça yapılandırılmış yerel/test profilleriyle sınırlandığını kontrol et.
  [ReadingPublicationProperties.java:32](../../be/src/main/java/com/tellpal/v2/content/application/publication/ReadingPublicationProperties.java#L32)

- Çalıştırılmış kontrolleri ve kapsam dışındaki mevcut görsel baseline hatalarının ayrımını incele.
  [verification.md:1](../../be/docs/evidence/p3-reading-publication/verification.md#L1)

- Bağımsız inceleme bulgularını, düzeltme kanıtlarını ve kapanış kaydını incele.
  [review.md:1](../../be/docs/evidence/p3-reading-publication/review.md#L1)
