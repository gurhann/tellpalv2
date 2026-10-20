# P4b5 elle tüketici ölçümü — v2 yerel doğrulama

Tarih: 2026-10-20. Yerel uygulama, test ve offline APK hazırlığı tamamlandı. Gerçek kurulum, ilk başlangıç, backend, Firebase girişi ve ölçüm yapılmadı. Üç bağımsız inceleme ve somut koşu için ayrı kullanıcı onayı bekleniyor.

İlk turdaki [v1 raporu](verification-2026-10-20.md), kaynak arşivi ve bütün önceki kayıtlar korundu. Bu rapordaki sonuçlar yeniden türetilen v2 koduna aittir.

## Değişiklikler

- Ayrı debug girişinde gerçek mevcut router, tek karşılaştırma Cubit’i ve ortak detail/reader görsel alanları kullanılır. Başlat öncesinde medya Cubit’i veya ağ işi oluşturulmaz. Yeni QA Cubit’i get_it/injectable ve Freezed ile kurulur.
- Android `filesDir` izni değiştirilmez. QA alt dizinleri 0700, dosyaları 0600 olur. Canonical yol, symlink, CREATE_NEW, nonce/PID/startTicks/lifetime/kapsam ve tek consume kontrolleri vardır. Credential yalnız ADB stdin üzerinden özel başlangıç dosyasına gider.
- On aşama hash zinciriyle kaydedilir. Dört gerçek paint sonrasında gezinme, aynı receipt/epoch/PID/lifetime/nonce ve PNG SHA’sına bağlı tek host ACK’sini bekler. Route geçişinin çizimi gizlediği süre ilk paint kapısında beklenir. Dördüncü ACK ve beforeBackground receipt’i tamamlanınca mevcut kontrol alanında Home yönergesi görünür; görsel slot ölçüleri korunur.
- Retry ve sıra dışı gezinme yeni istek açamaz. Background/resume yeni admission vermez. `stopped`, credential ve dosya cleanup başarılı olduktan sonra yazılır. Cleanup hatası veya kayıp ACK terminaldir.
- Python ADB adapter’ı shell v2, `-T`, iki shell sınırında korunan quoting ve uzak exit kodunu kullanır. Exit 3 yalnız NOT_READY’dır. Boş çıktı/hata başarı olmaz. Screenshot binary’si mevcut `exec-out screencap` yolunda kalır. Credential cleanup PID’den bağımsızdır; süreç, reverse ve backend temizlikleri ayrı sonuç üretir.
- Python/Java gerçek ayrı onay kapıları source/run/APK bağını kontrol eder. Java startup claim’i bu kapılardan sonra yazılır. Yeni manual kapsam, numaralı `sourceDirectory` bağını kullanır; eski kapsamların `source` varsayılanı korunur.
- İki kapalı denemenin 12+6 dosyalık proof zinciri gerçek dosya SHA’larıyla yeniden doğrulandı. Aynı budgetRunId ve 39 işlem / 44.742.970 byte eski borç korunur. Üretim Java kodu, şema ve worker değiştirilmedi.

## Geçen kapılar

| Kontrol | Sonuç |
|---|---|
| `flutter pub get --offline` | Kilitli/cache’li paketlerle geçti; yalnız `path_provider: 2.1.6` doğrudan bağımlı oldu |
| `dart run build_runner build --delete-conflicting-outputs` | Bir kez çalıştı; gereken QA Freezed/DI çıktıları alındı; görev dışı üretilen kaynaklar korunmuş sürümlerine döndürüldü |
| `flutter test --no-pub test/reading_images` | 188 geçti |
| `flutter analyze --no-pub` | No issues found |
| `python -m unittest discover -s tools/image-optimization -p 'test_p4b5*.py'` | 61 geçti; skip yok; üç kapsamın yeni gerçek native JSON logu seçildi |
| `bash ./mvnw -o -Dtest=P4b5ConsumerQa*Test,P4b5ConsumerQaIT test` | 24 geçti; hata/skip yok; iki yeni izole sentetik PostgreSQL DB’si kullanıldı |
| Ayrı debug APK / Gradle offline | `build-0008`, BUILD SUCCESSFUL |
| Bağların salt okunur self-check’i | Kaynak, seçilmiş APK, Python binary/import closure ve iki eski proof geçti |

Gerçek Dart protokolü geçici dizinde 0771 filesDir, alt dizin izinleri/symlink, tek consume, scope/nonce/PID/startTicks/lifetime mutasyonları ve cleanup’ı sınar. Aynı `ManualConsumerQaView` ile çalışan gerçek router testleri beş senaryoyu kapsar: dört paint/ACK ve tam yaşam döngüsü; ACK sırasında lifecycle terminali; retry kilidi; nonce sahibi değiştiğinde cleanup failure ve stopped yokluğu; kayıp ACK’de deadline. Bu testler gerçek binding lifecycle’ını ve gerçek özel dosya protokolünü kullanır. Sentetik loopback dışında Firebase/storage çağrısı yapılmaz.

Python adapter komutları ayrıca iki gerçek `/bin/sh` sınırından geçirilerek stdin, quoting, uzak exit 3, izinler, symlink ve CREATE_NEW/duplicate ACK davranışıyla sınandı. Python ve Java onay testlerinde sentetik geçerli source/run/APK zinciriyle karar, scope, run ve APK ayrı bozulur; ADB/kurulum/startup sınırına geçilmez. Geçerli onaydan sonra eksik prior proof yine reddedilir; başarı uydurulmaz.

Ham loglar MAC_EK `receipts/` dizinindedir: `flutter-tests-v2-accepted.log`, `flutter-analyze-v2-accepted.log`, `python-tests-v2-final.log`, `java-tests-v2.log`, `pub-offline-v2-final.log`, `build-runner-v2.log`. Sentetik PostgreSQL v2 cluster’ı test sonunda kapatıldı; önceki cluster ve DB kayıtları değiştirilmedi.

## Generator kapsamının repository karşılığı

`app/mobile/build.yaml` repository’ye eklendi. Source-0003’te daha önce generator için kullanılan dosyanın aynısıdır. `$default` target’ın mevcut paket/lib/test/integration_test/bin/web/tool/example/benchmark/assets kategorileri korunur; `test_driver/**` yeni QA DI/Freezed kaynaklarının yeniden üretimini kapsar. Başka kod veya test değiştirilmedi.

Dosyanın repository ve kopya SHA-256 değeri `2bd5e6433c0aed7c993e6ee222817b2c2193b50a50237a95196432ceb9e385a2` olup `source-binding-0008` içindeki hem `files` hem `repositoryFiles` alanında bulunur. Bu eksik kaynak-bağı girdisini ekleyen build-0008 offline 18 saniyede geçti; APK byte’ı build-0007 ile aynıdır. Önceki bağlar ve build-0007 korundu. Değişmeyen Flutter/Java testleri tekrarlanmadı.

## Son kaynak ve APK bağı

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-consumer-measurement-20261020-be80ce1`.
Seçilen kaynak: `source-0003`; APK: `android/builds/build-0008/consumer.apk`.
Paket: `com.tellpal.tellpal_mobile_poc.readingmanualqa`.
APK SHA-256: `df1dc74e8db39bed1f5f47b6ec7479c6ddf3403c57629a1723c57ca09ad845a0`.

Bağ 1162 kaynak girdisini, 1303 Java classpath dosyasını ve 1388 Python binary/import girdisini kapsar. `source-root-validation-0002` package_config self root’unun source-0003’e çözüldüğünü ve kullanılan Android girdilerinin source-0002’ye yönelmediğini kaydeder. Yeni kopyanın eski Flutter/Gradle cache’leri kaldırılıp package_config offline üretildi. Son Android build’in JSON/depfile/properties/CMake girdilerinde source-0002 referansı bulunmadı.

188 testin çalıştığı source-0002 ile son kopyadaki 1076 mobil/Java kaynak girdisi ve 1025 Java class dosyası byte olarak eşittir. Son Python kodu ve testleri repository kökünden 61 testle doğrulanıp source-0003’e aynı SHA ile alındı. Değişmeyen Flutter/Java testleri clone nedeniyle tekrarlanmadı.

| Kayıt | SHA-256 |
|---|---|
| `source-binding-0008.private.json` | `684606b6c3c2a29395408185db461ea65d4ae010d33c4524dfeba6fc9587e2c0` |
| `selected-build-0003.private.json` | `559edfe38aa0f4a6a5fdc90b696a617e0a69e816e8b147575ec2c29a09ed7239` |
| `classpath-binding-0004.private.json` | `4af399a7da493ad8564df57749df4cf62a84578703215294b972db6963074c14` |
| `python-binding-0003.private.json` | `fbf402b252781a902a025f2491b95c3da642329bf8cf8b7d94810aef59561811` |
| `run-binding-0003.private.json` | `808cbb2e1666efad679598382c77f5ae218d120015d360b19583147ef1c16bea` |
| `local-preparation-0003.private.json` | `9a082fdcb85726e0078418d648c5ee4d59a530f26bcd531629d433235d698522` |
| `source-root-validation-0002.private.json` | `a53ca03c5959733dfa453f2a4dfd837b6f8ed88a319d9210355d7da27de902f0` |

Clone sürerken erken başlayan yerel `build-0006` durduruldu; 84 girdilik kısmi kaynak bağı ve build logu korundu, seçilmedi. Clone exit_code=0 ile tamamlandıktan ve offline package_config kapısı geçtikten sonra son tam kaynak bağı/build-0008 üretildi. Eski source-0002/build-0005 ve ilk tur arşivleri değiştirilmedi.

Bu kökte gerçek execution approval veya install/initial-start/backend-start/login/window/measurement claim’i oluşturulmadığı ek makbuzda doğrulandı. Üç bağımsız inceleme kaydı henüz üretilmedi; son run bağı incelemelere teslim edilmeye hazırdır.

## Göreve ait kesin kod/test dosyaları

- `app/mobile/build.yaml`
- `app/mobile/lib/app/reading_image_consumer_qa/consumer_image_slot.dart`
- `app/mobile/lib/app/reading_image_consumer_qa/detail_screen.dart`
- `app/mobile/lib/app/reading_image_consumer_qa/flow_screen.dart`
- `app/mobile/lib/app/reading_image_consumer_qa/manual_qa_policy.dart`
- `app/mobile/lib/app/reading_image_consumer_qa/reader_screen.dart`
- `app/mobile/pubspec.lock`
- `app/mobile/pubspec.yaml`
- `app/mobile/test/reading_images/manual_consumer_protocol_test.dart`
- `app/mobile/test/reading_images/manual_consumer_qa_test.dart`
- `app/mobile/test/reading_images/manual_consumer_router_test.dart`
- `app/mobile/test/reading_images/reading_image_consumer_qa_test.dart`
- `app/mobile/test/reading_images/reading_p4b5_qa_probe_test.dart`
- `app/mobile/test_driver/manual_consumer_qa/coordinator.dart`
- `app/mobile/test_driver/manual_consumer_qa/coordinator.freezed.dart`
- `app/mobile/test_driver/manual_consumer_qa/injection.config.dart`
- `app/mobile/test_driver/manual_consumer_qa/injection.dart`
- `app/mobile/test_driver/manual_consumer_qa/private_protocol.dart`
- `app/mobile/test_driver/reading_consumer_manual_entry.dart`
- `app/mobile/test_driver/reading_consumer_qa_probe.dart`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaIT.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaLedger.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaLedgerTest.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaManualTest.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaScope.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaScopeTest.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaSetup.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaSetupTest.java`
- `tools/image-optimization/p4b5_consumer_qa.py`
- `tools/image-optimization/p4b5_manual_consumer_qa.py`
- `tools/image-optimization/p4b5_qa_scope.py`
- `tools/image-optimization/test_p4b5_manual_consumer_qa.py`

Takip belgeleri: bu ayrı v2 raporu ve ana spec’in görev/doğrulama alanları. Frozen amaç/limitler, proposal ve v1 raporu korunur. Önceden değişmiş `.memlog.md`, `main_image_experiment.dart` ve local-lossless dosyaları göreve alınmadı.

## Açık kapılar

Üç bağımsız inceleme henüz yapılmadı. Gerçek koşu için bu somut APK/run bağı üzerinden ayrı kullanıcı onayı gerekir. Güncel cihaz, gerçek QA DB’si, storage veya auth uygunluğu yerel testlerden çıkarılamaz. Eski borç gerçek kapıda tam eşleşmelidir. Aynı 120 saniye, 1 manifest, 12 status, 4 medya / 13.170.884 byte ve 17 HTTP sınırları geçerlidir; retry/fallback yoktur.

Fiziksel Android/iPhone kalite, P0 ham kanıt eksiği, RSS/GPU zirvesi, JPEG/WebP netliği, üretim profili, normal Library entegrasyonu ve tüm P4b kabulü açıktır. Bu yerel çalışma bu kapıları kapatmaz.
