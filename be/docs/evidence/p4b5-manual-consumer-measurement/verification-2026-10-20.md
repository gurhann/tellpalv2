# P4b5 elle ölçüm: yerel hazırlık doğrulaması

Tarih: 2026-10-20. Bu rapor ilk uygulamanın tarihsel test/build kanıtıdır. [İlk üç inceleme](pre-run-review-iteration-1.md) kusur buldu; kaynak saklandı ve yalnız bu görevin kodu yeniden türetme için başlangıca alındı. Aşağıdaki sayılar yeni kodun kabulü değildir. Gerçek koşu onaylanmadı ve çalıştırılmadı.

## Uygulama

- Ayrı `reading_consumer_manual_entry.dart` girişi FlutterDriver extension açmaz. Başlat öncesi karşılaştırma Cubit’i veya medya işi oluşturmaz. Koordinatör get_it/injectable ve generator üretimi Freezed durumunu kullanır.
- Opt-in politika yalnız QA kontrollerini sınırlar. Detail → sayfa 1 → sayfa 2 → detail → arka plan → dönüş → kapat sırası korunur. Her yeni gezinme, gerçek frame sonrasındaki `painted(epoch)` kaydını bekler. Retry kapalıdır. Ortak görsel slotun geometrisi değiştirilmedi.
- Başlangıç JSON’u mevcut parser ile doğrulanır. Doğrulama için oluşan geçici credential nesnesi kapatılır. Tek Başlat, mevcut probe’un tek configure/admission yolunu açar.
- App-private başlangıç ve receipt dizinleri ayrıdır. Dosya aktarımı yalnız sabit paket/yollar ve ADB stdin kullanır. `handoff.owner` nonce bağı, yarım kalmış `configure.json.part` dosyasının da yalnız bu denemenin sahipliğiyle temizlenmesini sağlar. Token argv, intent, APK veya sırsız receipt’e yazılmaz.
- On receipt; nonce, PID, `/proc` startTicks, lifetime, sıra ve önceki dosya SHA’sına bağlanır. Her dosya en çok 1 MiB; toplam en çok 12 MiB’dir. CREATE_NEW ve atomik yayın uygulanır. 128 olay taşması terminaldir; geçmiş olaylar sessizce silinmez.
- Baseline `supportDirectory` alanını taşır. Host bu alanı gerçek `run-as pwd` kökünün `/files` altıyla tam eşleştirir. Android dosya/dizin izinleri 0600/0700 olarak doğrulanır. Gerçek Android dosya akışının cihaz kabulü ayrı koşuya bırakılmıştır; bu hazırlıkta uygulama kurulmadı veya başlatılmadı.
- Yeni Python yürütücüsü mevcut saf kaynak/phase doğrulayıcılarını kullanır. Eski VM kapısını başarı üretmek için kullanmaz. Sonuç yalnız on aşama ve backend terminal uzlaştırması birlikte sağlanırsa kabul edilir.
- Java değişiklikleri yalnız test harness’indedir. Üretim Java kodu, worker, şema ve normal uygulama davranışı değiştirilmedi. İki eski kapalı denemenin 12 ve 6 dosyalık zinciri gerçek hash’leriyle doğrulandı. Eski dosyalara yazılmadı. Aynı budgetRunId, 39 işlem / 44.742.970 byte borcu ve mevcut tavanlar korundu.
- Gerçek aşamalar için üç bağımsız inceleme ve APK/run hash’lerine bağlı `APPROVE_ONE_REAL_MANUAL_ATTEMPT` kararı ayrıca zorunludur. Yerel hazırlık onayı bu kararı sağlamaz.

## Çalıştırılan kapılar

Flutter işlemleri yeni, credential içermeyen MAC_EK kaynak kopyasında mevcut SDK/cache ile yapıldı. Yeni paket veya runtime indirilmedi. `path_provider: 2.1.6` mevcut kilitli sürümde doğrudan bağımlılık oldu.

| Kapı | Sonuç |
|---|---|
| `flutter pub get --offline` | Geçti; sürüm değişimi yok |
| `dart run build_runner build --delete-conflicting-outputs` | Gerekli QA Freezed/DI çıktıları gerçek generator ile üretildi |
| `flutter test --no-pub test/reading_images` | 178 geçti |
| `flutter analyze --no-pub` | No issues found |
| Python `test_p4b5*.py` | 54 geçti; atlanan test yok |
| Maven wrapper, `P4b5ConsumerQa*Test,P4b5ConsumerQaIT` | 23 geçti; atlanan test yok |
| Çevrimdışı ayrı debug APK | `build-0004` hazır; paket aapt ile doğrulandı |
| `git diff --check` | Geçti |

İlk Python koşusunda iki isteğe bağlı eski native JSON testi girdileri seçilmediği için atlanmıştı. Son koşuda eski iki scope ve yeni manual scope için güncel Flutter loopback/native JSON logu seçildi. Üçü de çalıştı ve geçti. İlk sandbox koşusunun `ps` erişim kısıtı, yalnız sentetik test sürecini okuyan yetkili yerel koşuyla giderildi. Mevcut süreç testindeki exec hazırlık yarışı, sentetik çocuğun hazır kaydı beklenerek deterministik hale getirildi.

İlk generator koşusunda varsayılan hedef `test_driver` kaynaklarını taramadı. `build.yaml` bu dizini ekler; lib/test/integration_test ve diğer varsayılan kaynak kategorileri korunur. Yalnız yeni QA’nın gerekli üretilen dosyaları repoya alındı. İlk sentetik PostgreSQL başlangıcı uzun Unix socket yolu nedeniyle kapandı. Yalnız bu görevin kısa geçici socket yoluyla testler geçti. Test cluster’ı durduruldu. Gerçek QA DB’si sorgulanmadı veya kullanılmadı.

## Matris karşılığı

| Beklenti | Çalışan kanıt |
|---|---|
| Doğru kimlik, tek Başlat, dört sıralı çizim | `manual_consumer_qa_test.dart` FSM sırası; üç scope gerçek loopback/native paint testi; mevcut ortak consumer frame/HTTP testleri |
| Eski/yanlış kayıt, ikinci Başlat, sıra hatası | Yeni Dart config/Başlat/sıra testleri; Python receipt kimlik, sıra, nonce, PID/startTicks ve eski proof mutasyonları; Java scope/proof testleri |
| Süre, hata, eksik kanıt, PID değişimi | Dart 120 saniye ve medya hatası testleri; Python missing/duplicate/truncated/chain/PID/epoch/overflow testleri |
| Arka plan, dönüş, kapanışta yeni medya yok; kaynaklar sıfır | Yeni FSM ve drain mutasyonları; gerçek native JSON phase doğrulaması; mevcut pending HTTP/native cleanup testleri |
| Borç ve tavan korunur | Java ledger dört medya/beşinci medya reddi; izole PostgreSQL V37 restart ve 63→64 için iki eşzamanlı rezervasyonda yalnız bir kabul |
| Gizlilik ve yarım aktarım temizliği | Python stdin/argv, sabit yol/symlink komutu, boyut ve nonce sahibi `.part` cleanup testleri |

Sentetik Flutter/native testleri gerçek Firebase/storage ölçümü değildir. Android app-private yönteminin gerçek kurulumdan sonraki baseline kapısını bu rapor geçmiş saymaz.

## Kaynak ve çıktı bağı

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-consumer-measurement-20261020-be80ce1`.

| Kayıt | SHA-256 |
|---|---|
| `android/builds/build-0004/consumer.apk` | `9cece05c54490ae3bffbe6865aa57de5e2693dcad7475712185bc5a6fe91f278` |
| `receipts/run-binding-0001.private.json` | `055a15d2b098212237877e9dbd58fc393a1f449bc9e4ff01d81636bd88b40a6f` |
| `receipts/source-binding-0004.private.json` | `38c11ac4d622fc2579d92fdeb5b3880ab127a97faa3f2d23761a4fd1495f6c26` |
| `receipts/classpath-binding-0002.private.json` | `26c4ef031225b403b3de8fb806914ded2b8bbef41d7c457db1e0fcf290f9511b` |
| `receipts/python-binding-0001.private.json` | `881521c77fd5507cfda1a3ec84ac079fd2c748c96182c47db8f8f6457ffdc4a8` |
| `receipts/prior-attempt-proof-0001.private.json` | `81d4a319f7eb6817df4c3df49242da47126be3231133246a72d1396fbf388a35` |

Bağ 1160 kaynak girdisini, 1303 Java classpath dosyasını ve 1388 Python binary/import girdisini doğrular. Ana spec, workflow durumu ve doğrulama raporu yürütülen kaynak bağına alınmadı. `be/docs` altında yalnız üç değişmez proposal bağlıdır. Hazırlık kaydı `receipts/local-preparation-0001.private.json` içindedir. Kaynak/yorum düzeltmesi gerekirse yeni numaralı bağ ve APK seçimi gerekir; eski kayıtların üzerine yazılmaz.

Son loglar: `receipts/flutter-tests-review.log`, `receipts/flutter-analyze-review.log`, `receipts/python-tests-review.log`, `receipts/java-tests-sealed.log`. Ham dosyalar repoya kopyalanmadı.

## Açık kapılar

Üç bağımsız ön inceleme ve somut gerçek koşu onayı bekleniyor. Bu yeni kapsamda kurulum, ilk uygulama başlangıcı, backend başlangıcı, giriş, ölçüm penceresi ve medya tüketimi sıfırdır. Yeni `execution-approval-01.private.json` veya gerçek çalışma claim’i oluşturulmadı.

Gerçek koşudan önce güncel DB borcu, retained LIVE, cihaz/PID, yüklü APK ve gerçek app baseline yeniden doğrulanmalıdır. Süre, insan gecikmesi ve disk/ADB hatası başarıyı garanti etmez. Hesap değişimi, uzun session, fiziksel Android/iPhone kalite, RSS/GPU zirvesi, JPEG/WebP profili ve genel P4b kabulü açık kalır. Push, PR veya dağıtım yapılmadı.

## Göreve ait dosya listesi

Yeni veya değişen uygulama/test kaynakları:

- `app/mobile/build.yaml`
- `app/mobile/pubspec.yaml`
- `app/mobile/pubspec.lock`
- `app/mobile/lib/app/reading_image_consumer_qa/manual_qa_policy.dart`
- `app/mobile/lib/app/reading_image_consumer_qa/consumer_image_slot.dart`
- `app/mobile/lib/app/reading_image_consumer_qa/detail_screen.dart`
- `app/mobile/lib/app/reading_image_consumer_qa/flow_screen.dart`
- `app/mobile/lib/app/reading_image_consumer_qa/reader_screen.dart`
- `app/mobile/test_driver/reading_consumer_manual_entry.dart`
- `app/mobile/test_driver/reading_consumer_qa_probe.dart`
- `app/mobile/test_driver/manual_consumer_qa/coordinator.dart`
- `app/mobile/test_driver/manual_consumer_qa/coordinator.freezed.dart`
- `app/mobile/test_driver/manual_consumer_qa/injection.dart`
- `app/mobile/test_driver/manual_consumer_qa/injection.config.dart`
- `app/mobile/test_driver/manual_consumer_qa/private_protocol.dart`
- `app/mobile/test/reading_images/manual_consumer_qa_test.dart`
- `app/mobile/test/reading_images/reading_p4b5_qa_probe_test.dart`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaIT.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaLedger.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaLedgerTest.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaManualTest.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaScope.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaScopeTest.java`
- `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaSetup.java`
- `tools/image-optimization/p4b5_consumer_qa.py`
- `tools/image-optimization/p4b5_manual_consumer_qa.py`
- `tools/image-optimization/p4b5_qa_scope.py`
- `tools/image-optimization/test_p4b5_consumer_qa.py`
- `tools/image-optimization/test_p4b5_manual_consumer_qa.py`
- `tools/image-optimization/test_p4b5_qa_scope.py`

Takip belgeleri: ana spec’in görev/doğrulama alanları ve bu rapor güncellendi. Frozen blok değiştirilmedi. Görevin başlangıcında verilen investigation ve `bounded-run-proposal-2026-10-20.json` girdileri korundu. Önceden değişmiş `.memlog.md`, `main_image_experiment.dart` ve local-lossless kaynakları bu görevde değiştirilmedi.
