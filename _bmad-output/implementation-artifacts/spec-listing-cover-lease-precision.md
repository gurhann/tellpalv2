---
title: 'Kapak hazırlama işinde zaman hassasiyetini düzelt'
type: 'bugfix'
created: '2026-10-24'
status: 'done'
review_loop_iteration: 0
baseline_commit: d45582de10b4c790b01007cf5655c1d110ad5226
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** CI'daki üç kapak testi null sonucu alıyor. Nanosaniyeli worker lease'i PostgreSQL'de mikrosaniyeye yuvarlanınca tam kimlik eşitliği bozuluyor. Geçerli iş eski sayılıyor; manifest yazılmıyor. Gerçek poller da aynı yolu kullanıyor.

**Approach:** İş başlangıcını lease oluşturulmadan önce mikrosaniyeye sabitle. Mevcut üç senaryoyu nanosaniyeli saatle deterministik regresyon testine dönüştür.

## Boundaries & Constraints

**Always:** Tam lease/attempt/kaynak/profil eşitliği, süre dolumu, 10 dakika lease, retry ve atomik manifest korunsun. Yalnız yerel PostgreSQL ve sahte storage kullanılsın.

**Ask First:** Canlı ayar/token değişimi, push/deploy veya kapsam genişletme.

**Never:** Test atlama, null beklentisini gevşetme, testte worker reread ile hatayı örtme, toleranslı sahiplik kontrolü, Firebase erişimi veya orijinal dosya değişimi.

## I/O & Edge-Case Matrix

| Durum | Beklenti |
|---|---|
| Başlangıç `.123456789Z` veya `.999999999Z` | Worker ve DB lease'i aynı; telefon/tablet kapakları hazır |
| Zaten mikro/tam saniye | Mevcut davranış korunur |
| Eski/expired worker veya değişmiş kaynak/profil | Yeni iş ve manifest değişmez |

</frozen-after-approval>

## Code Map

Aşağıdaki Java yolları `be/src/` altındadır.

- `main/java/com/tellpal/v2/asset/application/AssetProcessingService.java:101`: `start()` düzeltme noktası; DB refresh olmadan worker döner.
- `main/java/com/tellpal/v2/asset/domain/AssetProcessing.java:67`: tam sahiplik kontrolü korunur; `:389` lease üretir.
- `main/java/com/tellpal/v2/asset/application/LibraryCoverPreparationService.java:35`: DB reread ve atomik publish.
- `main/java/com/tellpal/v2/asset/infrastructure/processing/AssetProcessingPoller.java:67`: gerçek worker aynı start sonucunu kullanır.
- `test/java/com/tellpal/v2/category/LibraryHomeIntegrationTest.java`: mevcut Clock spy, kaynak/profil/expired kontrolleri.
- `test/java/com/tellpal/v2/content/web/admin/ListingCoverAdminSourceIntegrationTest.java`: İngilizce kaynak ve HTTP/katalog kabulü.

## Tasks & Acceptance

**Execution:**
- [x] İki test sınıfında nanosaniyeli sabit saat kullan. Önce aynı üç hatayı üret; start dönüşü ile DB lease eşitliğini de doğrula.
- [x] `AssetProcessingService.start()` içinde zamanı `ChronoUnit.MICROS` ile sabitle. Kısa Türkçe gerekçe ekle.
- [x] İlgili testleri ve tam test/verify kapılarını çalıştır; sonuçları kaydet.

**Acceptance Criteria:**
- Given nanosaniyeli Clock, when iş başlatılıp yeniden okunursa, then worker ve DB lease'i birebir eşittir.
- Given geçerli worker, when kapak üretimi biterse, then doğru dil/kaynak/profil ile telefon/tablet manifesti hazırdır.
- Given eski/expired worker, when complete/fail/publish çağrılırsa, then yeni iş ve mevcut manifest değişmez.
- Given mevcut test kapıları, when tam doğrulama çalışırsa, then failure/error yoktur; opt-in skip'ler ayrıca raporlanır.

## Spec Change Log

## Verification

2026-10-24 tarihinde yerel PostgreSQL 15.19 ve sahte storage ile doğrulandı:

- İlgili iki sınıf, düzeltme öncesinde 11 testte aynı üç senaryoda failure üretti; error/skip yoktu.
  `.123456789Z` worker lease'i DB'de `.123457Z`, `.999999999Z` lease'i sonraki tam saniye oldu.
  Düzeltmeden sonra 11 testin tamamı geçti; failure/error/skip yoktu. DB yeniden okuması yalnız
  eşitlik assertion'ında kullanıldı; executor'a doğrudan `start()` dönüşü verildi.
- `cd be && ./mvnw test`: 602 test, sıfır failure/error, 27 mevcut opt-in skip; başarılı, 4 dakika 40 saniye.
- `cd be && ./mvnw verify`: Surefire'da 602 test, sıfır failure/error, 27 opt-in skip;
  Failsafe'da 42 test, sıfır failure/error, 3 opt-in skip; başarılı, 7 dakika 25 saniye.
- Surefire skip'leri: `p5g.local.enabled` gerektiren 23 test, ayrı corpus girdileri gerektiren
  üç Package1 testi ve `TELLPAL_CMS_ACCEPTANCE` gerektiren bir tarayıcı kabul testi.
  Failsafe skip'leri: `tellpal.package3.acceptance` gerektiren bir loopback testi ve izole JDBC
  seçimi gerektiren iki `P4b5ConsumerQaIT` testi. Yeni skip ayarı eklenmedi.
- CI launcher kapısı: `python3 -m unittest discover -s tools -p 'test_*.py' -v`; 47 test geçti.
- Her iki tam Maven kapısı `TELLPAL_READING_NATIVE_TESTS_REQUIRED=true` ile çalıştı.
  Resmi SHA pinleriyle doğrulanmış mac-arm64 `cwebp`/`webpmux` 1.6.0 ve mevcut C kaynağıyla
  aynı SHA kimliğini taşıyan `reading-process-supervisor` kullanıldı. Java sürümü 25.0.2 idi.
- `git diff --check` başarılı. Testlerin ürettiği yalnız sıralama içeren Modulith belge farkları
  geri alındı. Maven wrapper'ın özgün dosya modu korundu.

Başlangıçta `default` ve `tellpal-image-local` Colima profilleri kapalıydı; özel profil geçici
başlatıldı. Testlerden sonra çalışan container kalmadığı doğrulandı ve özel profil kapatıldı.
Docker'ın seçili `desktop-linux` context'i değiştirilmedi. Test logları ve native araçlar
repository dışındaki `/tmp/tellpal-lease-precision.PQNclF/` dizinindedir.

Doğrulama başlangıç commit'i: `d45582de10b4c790b01007cf5655c1d110ad5226`.
PostgreSQL hassasiyeti:
https://www.postgresql.org/docs/15/datatype-datetime.html

## Yürütme notu

Bağımsız incelemeyi ve son yerel commit adımını ana ajan BMad akışının sonraki adımlarında yürütür. Uygulama ajanı commit oluşturmaz. Yerel test altyapısı için mevcut özel `tellpal-image-local` Colima profili kullanılabilir; önceki çalışma durumunu kaydet ve başlatılan test kaynaklarını iş bitiminde kapat. Firebase test profili sahte client kullanır. Test loglarını ve pinli native araçları repository dışında geçici dizinde sakla; gizli değerleri çıktıya verme.

## Bağımsız inceleme

Üç ayrı inceleme tamamlandı: blind hunter, sınır durum ve doğrulama boşluğu. Eyleme dönük bulgu çıkmadı. Kod değişikliği veya ek test tekrarı gerekmedi.

## Suggested Review Order

- İş başlangıcını DB hassasiyetine sabitleyerek worker kimliğini korur.
  [AssetProcessingService.java:102](../../be/src/main/java/com/tellpal/v2/asset/application/AssetProcessingService.java#L102)

- Kaynak değişimi, lease yenileme ve eski callback reddini nanosaniyeli saatle doğrular.
  [LibraryHomeIntegrationTest.java:136](../../be/src/test/java/com/tellpal/v2/category/LibraryHomeIntegrationTest.java#L136)

- Saniye sınırındaki saatle aynı kaynağın farklı profillerini doğrular.
  [LibraryHomeIntegrationTest.java:303](../../be/src/test/java/com/tellpal/v2/category/LibraryHomeIntegrationTest.java#L303)

- İngilizce kaynak ve katalog kapaklarını admin HTTP akışında doğrular.
  [ListingCoverAdminSourceIntegrationTest.java:43](../../be/src/test/java/com/tellpal/v2/content/web/admin/ListingCoverAdminSourceIntegrationTest.java#L43)
