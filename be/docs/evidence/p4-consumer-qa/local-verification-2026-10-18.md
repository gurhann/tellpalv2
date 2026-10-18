# P4a5 yerel doğrulama ve gerçek çalışma devri

Tarih: 2026-10-18. Bu kayıt yalnız yerel hazırlığı doğrular. Gerçek login, Firebase Storage
okuması, yeni APK build/kurulum/başlatma ve 180 saniyelik çalışma bu kayıtta yapılmadı.
Kanonik kapsam [P4a5 spec](../../../../_bmad-output/implementation-artifacts/spec-p4a5-existing-consumer-smoke.md)
ve SHA `079bfbe37c238bfcfc5cc3bc84f5ef420f5f7577b05b868dcd5622f73e267bf6` olan öneridir.

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb`.
Kaynak kopyası `source/`, ham koşu kayıtları `logs/`, kimlik/koruma kayıtları `receipts/` altındadır.
Credential veya token dosyası kopyalanmadı. Eski QA dizinlerine çıktı yazılmadı.

## Hazırlanan araçlar

- Yeni Java ledger, son terminal borcu olan 47 read/243.641.782 byte/verifier 20/login 2
  üzerinden başlar. Tek exclusive ledger/pencere kullanır. Çağrı öncesi fsync, retained kimlik,
  dört sıralı rendition path/generation/MIME/byte bağı ve HTTP/auth tavanları zorunludur.
- Yeni test kompozisyonu scheduler ve Flyway'i kapatır. Yalnız aynı V37 klonu ve aynı runId
  kabul edilir. İlk auth öncesi 25 işlem/31.572.086 byte DB borcu, retained LIVE ve normal
  tüketicilerden dışlanma yeniden denetlenir. SDK transport'u ve üretim kodu değişmez.
- Ayrı debug girişinde gerçek consumer QA router/Cubit kullanılır. Source, HTTP'den önce
  manifest 1/status en çok 18/media en çok 4 sınırını ve sabit kimliği denetler. Probe,
  gerçek slot/DPR/fit, byte/decode, aynı raster/epoch paint ve native sahiplik kayıtları üretir.
- Python aracı ayrı offline debug build, bekleme APK'sını bağlama, tek rezervasyonlu mevcut
  login yardımcısı ve gerçek UI düğmeleriyle dört geçişi hazırlar. Yerel build denemeleri
  `android/builds/build-0001`, `build-0002` gibi ayrı dizinlere yazılır. Bağlantı, seçilen
  immutable build makbuzuna `buildName` ve APK/source SHA ile bağlanır.
- Preflight/expired/UI/son ekran/cleanup hatası başarı sayılmaz. Süre her dış adımdan önce
  ve sonra denetlenir. VM/ADB/QA süreç ve yalnız owned backend temizliği bağımsız yürür.
  Watchdog sonlandırması native drain kanıtı olarak sunulmaz.

## Çalışan kapılar

| Kapı | Sonuç | Özel ham kayıt |
|---|---|---|
| Yeni ve eski Python sürücü testleri | 43/43 geçti | `logs/python-tests-07.log` |
| Yeni Flutter probe testleri | 6/6 geçti | `logs/flutter-probe-tests-03.log` |
| Reading-images regresyonu | 128/128 geçti | `logs/flutter-reading-regression-01.log` |
| P4a4'ün 228 eski testi ve 6 yeni test | 234/234 geçti | `logs/flutter-full-regression-02.log` |
| Üç yeni Flutter dosyasında analyzer | Sorun yok | `logs/flutter-analyze-02.log` |
| Yeni Java koruma testleri | 9/9 geçti | `logs/java-new-tests-04.log` |
| PostgreSQL/Tomcat/modül IT | 30/30 geçti; hata/atlama yok | `logs/java-real-it-02.log` |
| Korunan dosyalar ve eski makbuzlar | 3241 repo, 2 plan ve 72 eski run dosyası değişmedi | `receipts/implementation-audit.private.json` |
| Yeni fixture taraması | Token/credential/özel download URL yok | Aynı audit makbuzu |
| `git diff --check` | Geçti | Audit komut sonucu |

Python kapısı `python3 -m unittest discover -s tools/image-optimization -p 'test_p4*py'` ile
çalıştı. Flutter komutları özel kopyada `--no-pub` kullandı. Tam regresyon kapsamı
`test/reading_images`, `test/reading_image_viewport_test.dart`, `test/reading_consumer_frames_test.dart`,
`test/story_detail`, `test/image_experiment/reader_resolution_test.dart` idi.
Java komutları özel kopyanın `be/` Maven wrapper'ını `sh .../mvnw -o` ile kullandı.
IT sınıfları `ReadingImageComparisonServiceIT` 20, `ReadingImageComparisonTestControllerIT` 8,
`ApplicationModulesTest` 2 testtir. Mevcut Colima ve cache'teki PostgreSQL 15/Ryuk kullanıldı;
PostgreSQL 15.19 üzerinde V1–V37 migration ve gerçek Tomcat HTTP çalıştı. Storage fixture'ı
sentetik pinned disk dosyasıdır. İsteğe bağlı Docker ortam tanı kontrolü kapatıldı; test atlanmadı.
Annotation değişmedi; codegen çalıştırılmadı.

## İlk hata kayıtlarının sınırı

İlk Java test ham logu ikinci koşuda yanlışlıkla overwrite edildi. İlk Python hata logu da
geçici dosyada overwrite edildi. Bunların tam ham kayıtları korunmamıştır. Tool çıktısından
kurtarılabilen özet `logs/initial-log-loss-recovery.txt` içindedir; eski ham kanıt gibi sunulmaz.
İlk Java hatası test fixture'ındaki eksik local profil; Python hataları test geçici yolunun
symlink normalizasyonuydu. Sonraki koşular ayrı loglarla tutuldu.

`flutter-probe-tests.log` ve `flutter-probe-tests-02.log`, native zone desteği eksik ilk
Flutter denemelerini ve bunların kesilmesini korur. Tam regresyonun `-01` kaydı, özel kopyada
tek integration test destek kaynağı eksik olduğu için derlenemeyen ilk koşudur. `-02` geçti.
`java-real-it-01.log`, Colima hazırlığı beklenirken derleme aşamasında durdurulan denemedir;
IT kabulü değildir. Sonraki `-02` gerçek IT sonucudur.

## Açık kapılar

Root, kaynak/build incelemesi ile üç bağımsız ön çalışma incelemesinden sonra tek gerçek
login/pencere/koşuyu yürütür. `P4ConsumerQaSetup --execute <özel-kök>/backend` yalnız sabit
private backend dizinini kabul eder. `local-ready.json` kapıları geçmeden login açılmaz;
`open.request` ve geçerli credential makbuzu olmadan medya/auth penceresi açılmaz.
Gerçek başarısızlık yeniden deneme hakkı üretmez.

Dört gerçek UI paint'i, güncel retained/storage doğrulaması ve gerçek süreç kapanışı henüz
kanıtlanmadı. Yerel test bu sonucu ikame etmez. Genel tüketici geçişi, P4b, üretim profili,
fiziksel Android/iPhone kalite kabulü ve peak RSS/GPU sonucu açık kalır.
