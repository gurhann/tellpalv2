# P4a5 mevcut çıktı provası: uygulama haritası

Tarih: 2026-10-18. Bu belge plan girdisidir. Henüz gerçek auth/storage/uygulama çalışması yapılmadı.
Kanonik plan: [P4a5](../spec-p4a5-existing-consumer-smoke.md).
Sayısal öneri: [çalışma sınırları](../../../be/docs/evidence/p4-consumer-qa/bounded-run-proposal-2026-10-18.json).

## Doğrulanan başlangıç

HEAD `571a79b2b07941caffa0a3541f21d347138f82a5`; önceki 43 kirli dosya korunur.
P4a4 yerel QA kapsamı tamamdır; 228 mobil, 30 PostgreSQL/Tomcat/modül testi geçmiştir.
Yeni iş, aynı rotaların gerçek mevcut #94 medyasını teslim etmesidir.

Host Postgres PID 948 tanımlandı. Backend/frontend port 8080/5173 dinleyicisi bulunmadı.
Mevcut ARM64 Android emülatörü `emulator-5554`, 822×1828 override, 320 dpi/DPR 2,
font 1.0. Ayar değiştirilmedi. Fiziksel Android yok; iPhone bu dilime alınmaz.
Ana diskte yaklaşık 4,9 GiB, MAC_EK’te 334 GiB boş alan görüldü. Tüm yeni kaynak
kopyası/build/log/ekranlar MAC_EK’te yeni özel dizinde kalır; eski QA dizinleri değişmez.

Yerel V37 klonda iki READ ONLY transaction: #94 STORY aktif, tr PUBLISHED/PENDING,
iki sayfa, bir free bağı, kategori bağı yok. Aynı generation 4 LIVE; job COMPLETED,
revision/digest aynı; availability epoch 2 valid. Admitted 11261/11262/11263 kaynaklarının
epoch 1 ve admission ID bağları proof ile eşleşti. Page ID 1427/1428 sırası 1/2.
V37 aynı run satırında 25 işlem/31.572.086 byte tutuyor. Yeni DB admission çalışma başında
yeniden yapılır; bu okuma güncel storage doğrulaması değildir.

## Tarihsel kanıt ve sayaçlar

Eski parent ledger SHA `60086ea1ad1bdccbcc2423481fcd5a20cd4777602c3e87b1c3636732b298b839`.
Son başarılı child SHA `96dee3fb9e251a7438103f851dec78a09372d858e284e4967440a60de61c5fd1`,
11.068 byte. Son child 47 pinned read/243.641.782 byte/verifier 20 ile kapandı.
Giriş toplamı 2; eski tavan doldu. İki eski ready hash’i ve bütün tüketimler öneri JSON’undadır.
Yeni ledger son child’ın tüketimini taşır; eski 41-read tabanına dönülemez. Yeni runId açıp
aynı V37 bütçesini sıfırlamak yasaktır. Login için en fazla bir yeni lookup/exchange/verify
izin önerilir; mevcut 10 saniye worker + 2 saniye cleanup sınırı korunur. Başarısız giriş de
tüketilir; otomatik ikinci giriş yoktur.

180 saniye, yerel APK/bağlantı/DB/credential kapıları geçtikten sonra bir kez açılır.
Build ve yerel testler bu pencereye girmez. Pending aşamada SDK read/verifier yolu kapalıdır.
Pencere wall clock ve monoton saatle sınanır. Süre bitişi yeni çağrıyı kapatır; mevcut SDK’nin
mutlak in-flight sonlanması kanıtlanmış sayılmaz. Yalnız owned backend için sonlu cleanup/watchdog
kanıtı kaydedilir; yeni storage process/IPC veya retry eklenmez. Başarı garanti edilmez.

## Eski araçların sınırı

- `tools/image-optimization/p4_reading_comparison.py`: eski plan SHA/run, altı kaynak-rendition
  durumu ve reader yüksekliği 694’e bağlıdır. Eski davranışı ve dosyayı koru. Saf özel dosya,
  VM RPC ve komut yardımcıları yan etkisiz importla kullanılabilir; eski plan/run kapısı çağrılmaz.
- `app/mobile/test_driver/reading_comparison_entry.dart` ve `reading_comparison_probe.dart`:
  eski varsayılan comparison ekranını açar. Bunları değiştirme. HTTP gözlem ve aynı raster/epoch
  frame doğrulama yaklaşımını yeni entry/probe’a taşı; body/token/query loglama.
- `P4ExistingComparisonSetup.java:53`: eski runtime fingerprint eşitliği bu yeni media-serving
  koşusuna taşınmaz. PNG encoder 1.0→0.0 düzeltmesi yeni iş fingerprint’i üretmiştir. Eski retained
  descriptor/profile fingerprint’i değişmez; serving JDK/runtime hash’i ayrıca kaydedilir.
- `P4LocalComparisonLedger`: eski child constructor 41-read tabanına bağlıdır. Final ve somut
  tipi storage decorator/context’e bağlı olduğu için yeni read-only test kompozisyonu gerekir.
  Eski launcher/ledger/storage ve bunların testleri byte olarak korunur.

## Yeni backend test araçları

`be/src/test/java/com/tellpal/v2/support/` altında yalnız yeni:

- `P4ConsumerQaLedger.java`: son terminal ledger hash/byte ve proposal SHA ile zincirlenmiş
  yeni exclusive child. Tam sayılar, fsync-before-I/O, tek attempt/pencere, geçmiş tüketim ve
  dört izinli rendition için toplam okuma/byte/auth tavanı. Restart/ikinci açılış ret; borç geri verilmez.
- `P4ConsumerQaStorage.java`: yalnız test kompozisyonunda mevcut `AssetStorageClient` SDK’sını
  sarar; generation-pinned izinli dört çıktı okumasını önce rezerve eder. `withReadingDeadline`
  aynı ledger’ı paylaşır. Create/observe/legacy/signed URL yolları reddedilir. Yeni transport değildir.
- `P4ConsumerQaSetup.java`: `P4LocalComparisonSetup.validateEnvironment/isolation/privateJson`
  kurallarını kullanır; yeni initializer scheduler’ı refresh öncesinde kaldırır ve gerçek verifier/storage
  tüketicilerini sarmalar. Yalnız mevcut V37 klon, local/ephemeral loopback, canonical UID,
  no stub, revoke kontrolü, poller/bootstrap kapalı. Flyway kapalı; fixture/admission/publish/
  reconcile/worker API’si çağrılmaz. Üretim `ReadingImageComparisonApi` ve aynı V37 runId kullanılır.
- İlgili `P4ConsumerQaLedgerTest`, `P4ConsumerQaCompositionTest`, `P4ConsumerQaSetupTest`:
  yanlış SHA/identity/scope/counter, pending/expired/clock rollback, ikinci attempt, yasak I/O,
  gerçek Spring consumer sarımı ve aynı V37 satırını koruma. Mevcut gerçek PostgreSQL/Tomcat
  IT’leri ayrıca çalıştırılır; mock, gerçek IT’nin yerine geçmez.

`ReadingImageComparisonService.proof`, `ReadingPublicationReadyService.retainedEligible` ve
`ReadingImageDeliveryService` değişmez: captured work/proof, source epochs, availability,
manifest ve tam pinned byte SHA/MIME/decode doğrulaması esas alınır. Yalnız kanıtlanan bozulmanın
mevcut withdrawal davranışı geçerlidir. Sağlıklı LIVE elle değiştirilmez.

## Yeni mobil giriş ve sürücü

- `app/mobile/test_driver/reading_consumer_qa_entry.dart`, `reading_consumer_qa_probe.dart`:
  ayrı debug paket, release ret. Yeni proposal SHA/run/UID/#94/lease/token bağlarıyla runtime
  configure; `ReadingImageComparisonApp(consumerQa:true)` ve mevcut DI/Cubit/HTTP yolu.
  Token dosya/define/assets/log içine girmez. Descriptor/credentials route argümanı değildir.
- `app/mobile/test/reading_images/reading_consumer_qa_probe_test.dart`: gerçek loopback/native
  codec ve router. Bozuk wire/identity/role/page/epoch, kapalı giriş, fazla medya, expiry ve stop
  sınanır. P4a4 consumer testleri gerçek CTA ve router davranışının dayanağıdır.
- `tools/image-optimization/p4_consumer_qa.py`, `test_p4_consumer_qa.py`: offline ayrı hedef/paket
  APK build ve hash makbuzu; yalnız mevcut emülatöre kur/bağlan. Bekleyen ekran I/O açmaz.
  Gerçek UI CTA → Sonraki → reader geri → detail flow close komutları; doğrudan Cubit seçim
  komutu kullanma. Build ve probe değişikliği eski APK’ya başarı atfetmez.

Tek manifest, tek Cubit/ilk anchor, detail→page1→page2→detail için dört rendition/paint.
`reading-consumer-slot` alanını ölç; gerçek DPR/fit ve seçici ihtiyacını oracle ile karşılaştır.
694 yüksekliğini sabitleme; alan veya DPR’yi çıktıyı sığdırmak için değiştirme. Shortage ret ve
başarısız kanıttır. Role/pageId/pageNumber/revision/digest/job/profile/SHA, payload/decode/paint
birlikte kaydedilir. Dönüşte eski hidden route ek iş üretemez. Sadece etiket görmek noaudio
kanıtı değildir; HTTP yolu izin listesi, kaynak/import denetimi ve audio/grant çağrı sayaçları gerekir.

Dördüncü paint sonrası background/resume ek I/O açmamalı. Sonra gerçek UI close drain;
owned buffer/descriptor/codec/image sıfır, credential/deadline kapalı. VM/socket/ADB eşlemeleri
ve yalnız bize ait backend kapanır. Süre dolması/ret/hata kayıtları başarıya çevrilmez.
Ekranlar ve süreç PSS/RSS örnekleri, peak/GPU veya fiziksel kalite kabulü değildir.

## Doğrulama ve belge kapsamı

Önce Python yeni ve eski sürücü koruma testleri; özel kopyada probe + mevcut P4a4 tam mobil
regresyonları ve analyzer. Annotation değişmediğinden codegen beklenmez. Yeni Java testleri,
`ReadingImageComparisonServiceIT`, `ReadingImageComparisonTestControllerIT`, `ApplicationModulesTest`
wrapper ile izole PostgreSQL’de. Offline cache kullan; kurulum/indirme yok. Mevcut süreçler
tanımlanmadan durdurulmaz. `git diff --check`, kaynak-kopya eşitliği ve eski dosya hash denetimi.

Gerçek koşudan önce üç BMad inceleme katmanı tamamlanır. Yerel kopuk ortam başarısızlığı açık
kalır. Sonra proposal onayı varsa tek login/pencere/gerçek deneme; başarısızsa tekrar onayı gerekir.
Yeni kanıtlar `be/docs/evidence/p4-consumer-qa/` altında hassas verisiz rapor olur; ham çıktılar
MAC_EK’te kalır. Ekranları repoya kopyalama bu kapsamda yoktur. P4a4 done spec ve tarihsel
kanıtlar değişmez. Ürün politikasında veya backend/mobil üretim sözleşmesinde değişiklik öngörülmez.
