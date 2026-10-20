# P4b5 yerel başlangıç tanısı onarımı

2026-10-20. Onaylı [yerel tanı spec'i](../../../../_bmad-output/implementation-artifacts/spec-p4b5-local-startup-diagnostics.md) uygulandı. Yerel testlerde iki tanı kaybı giderildi. Bu sonuç gerçek Android başlangıcı, kapalı denemenin yeniden açılması veya P4b kabulü değildir.

## Değişiklik ve sınırlar

Host adapter artık boş ilk cmdline alanını `EMPTY_CMDLINE`, bozuk alanı `INVALID_CMDLINE`, farklı geçerli süreç adını `COMMAND_CHANGED` ile bildirir. Terminal hata kodları sırasıyla `QA_PROCESS_CMDLINE_EMPTY`, `QA_PROCESS_CMDLINE_INVALID`, `QA_PROCESS_COMMAND_CHANGED` olur. Bozuk sınıfı ilk alandaki `[A-Za-z0-9_.:-]+` dışındaki byte'ları kapsar. Boş ilk alan, sonrasında veri olsa da boş sınıfındadır. Kabul hâlâ ilk NUL öncesindeki tam paket byte eşitliğidir. NUL'suz tam paket ve ilk NUL sonrası argümanların önceki kabulü korunur. Boşluk temizleme, retry ve süre uzatma eklenmedi. Okuma/timeout, PID ve başlangıç tick kontrolleri korunur. Tanı yalnız sabit operation/code taşır; sekiz olay sınırı sürer.

Dart protokolü `directories` aşamasında dört enum alt kodu üretir: `supportTypeMismatch`, `supportTypeReadFailed`, `supportCanonicalMismatch`, `supportResolveFailed`. Son symlink tür kontrolünde, ara symlink canonical eşitlikte reddedilir. Resolver sonucu kabul için otomatik düzeltilmez. Genel `failed` kaydı ve initialize tamamlanmadığında `failureMarker unavailable` korunur. Güvenilmeyen support yolunda baseline, configure veya terminal dosyası oluşturulmaz. Üst dizin izinleri değişmez; mevcut alt dizin `0700`, dosya `0600` ve exclusive-create korumaları sürer.

Kod değişikliği iki kaynak ve iki yeni testle sınırlıdır; yeni spec ve kanıtlar ayrıca kaydedilir. Önceki pending kapsam/sahiplik değişiklikleri korunmuştur. Eski spec, test, rapor, `.memlog.md` ve özel run kayıtları düzenlenmedi. Yeni kaynak kopyası, APK, kurulum, gerçek uygulama/cihaz koşusu, ADB/VM bağlantısı, backend, giriş, ölçüm veya bulut işi yapılmadı. Commit ve staging yapılmadı.

## Yerel doğrulama

Mevcut Python `tools/image-optimization/.venv/bin/python`; mevcut Flutter `/Users/gurhankucuk/Documents/dev/flutter/bin/flutter` kullanıldı. Flutter repo `app/mobile/` içinden `--no-version-check`, `--no-pub`, `FLUTTER_SUPPRESS_ANALYTICS=true`, `DART_SUPPRESS_ANALYTICS=true`, `CI=true` ile çalıştırıldı. Pub işlemi, indirme, upgrade, SDK kopyası ve kalıcı ayar değişikliği yoktur. Test VM'si yalnız headless yerel test çalıştırıcısıdır.

| Kontrol | Sonuç |
|---|---|
| Yeni Python `test_p4b5_startup_diagnostics` | 5 test geçti |
| Mevcut `ManualAdapterBoundaryTest` | 4 test geçti |
| Mevcut `ManualProcessTransportTest` | 3 test geçti |
| Mevcut `ManualBothScopesCleanupTest` | 1 test geçti |
| Yeni Flutter `manual_consumer_startup_diagnostics_test.dart` | 16 test geçti |
| Mevcut `manual_consumer_bootstrap_test.dart` | 18 test geçti |
| Mevcut `manual_consumer_protocol_test.dart` | 7 test geçti |
| Yukarıdaki üç Dart test dosyası ve `private_protocol.dart` analyzer | 4 dosyada sorun yok |

Python toplam 13, Flutter toplam 41 test geçti; skip yoktur. Tüm eski Python modülleri çalıştırılmadı. Seçim yalnız hermetik adapter ve süreç sahipliği regresyonlarıdır; native JSON'a bağlı testler bu sonuca dahil değildir.

Tekrarlanabilir komutlar:

```sh
# tools/image-optimization/ içinde; PYTHONDONTWRITEBYTECODE=1 ile
.venv/bin/python -B -m unittest -v test_p4b5_startup_diagnostics test_p4b5_manual_consumer_qa.ManualAdapterBoundaryTest test_p4b5_manual_consumer_qa.ManualProcessTransportTest test_p4b5_manual_consumer_qa.ManualBothScopesCleanupTest

# app/mobile/ içinde; yukarıdaki geçici ortam değişkenleriyle
/Users/gurhankucuk/Documents/dev/flutter/bin/flutter --no-version-check test --no-pub --reporter expanded test/reading_images/manual_consumer_startup_diagnostics_test.dart test/reading_images/manual_consumer_bootstrap_test.dart test/reading_images/manual_consumer_protocol_test.dart
/Users/gurhankucuk/Documents/dev/flutter/bin/flutter --no-version-check analyze --no-pub test_driver/manual_consumer_qa/private_protocol.dart test/reading_images/manual_consumer_startup_diagnostics_test.dart test/reading_images/manual_consumer_bootstrap_test.dart test/reading_images/manual_consumer_protocol_test.dart
```

## Spec tablosunun test karşılığı

| Girdi | Test ve gözlenen sonuç |
|---|---|
| Tam paket ve geçerli stat | Python `test_exact_package_bytes_keep_existing_nul_and_no_nul_acceptance`: iki kapsam, NUL'suz/NUL'lu/argümanlı giriş aynı PID ve tick ile kabul edildi. |
| Boş/bozuk/yabancı cmdline | Python `test_empty_malformed_and_foreign_cmdline_have_distinct_terminal_codes_without_signal`: iki kapsamda ayrı tanı, terminal `FAILED_NO_RETRY`; stat okuması ve sinyal komutu yok. Boşluk, CR/LF, non-ASCII ve `:remote` kabul edilmedi. |
| Cmdline/stat okuma hatası veya timeout | Python `test_cmdline_and_stat_read_failures_and_timeouts_are_not_retried`: her iki okumada başarısız exit, istisna ve timeout mevcut ayrı retlerle durdu; komut tekrar edilmedi. Ek bozuk stat testi başlangıç kimliği reddini doğruladı. |
| Geçerli ham support dizini, 0771 | Flutter `ham 0771 support yolu`: iki kapsamda gerçek bootstrap/DI/protokol baseline üretti; support yolu önceden canonical yapılmadı, üst izin 0771 kaldı, baseline 0600 oldu. Configure ve HTTP yok; kaynak sayaçları değişmedi. |
| Eksik/yanlış tür veya son/ara symlink | Flutter `missing`, `file`, `finalLink`, `intermediateLink`: iki kapsamda tür/canonical alt kodu doğru; önce/sonra dosya ağacı ve izinler aynı, izin aracı çağrısı sıfır. Baseline/configure/medya işi ve terminal yazımı yok. |
| Canonical uyuşmazlığı veya resolve istisnası | Flutter `canonicalMismatch`, `resolveError`: gerçek `../` ham yolu ile uyuşmazlık; dar Directory fake ile resolve istisnası ayrı kod verdi. Fallback ve yazma yok. `typeReadError` yalnız testte dar IOOverrides ile ayrı tür okuma kodunu doğruladı. |

Yeni Flutter negatif testleri mevcut `temporary()` helper'ını kullanmaz; her ham support girdisi resolver'a olduğu gibi verilir. Taşınabilir fixture kurulumu yalnız `Directory.systemTemp` üst dizinini canonical yapar, sonra `createTemp` ile yeni child oluşturur. Yeni child veya negatif symlink/canonical girdileri sonradan normalize edilmez. Directory fake yalnız resolve hata kolundadır. IOOverrides yalnız tür okuma hatasını üretir. Her negatif test sabit tanı biçimini, gizli sentinel ve yol yokluğunu, `failureMarker unavailable`, boş probe olaylarını, sıfır Cubit/HTTP ve değişmeyen kaynak sayacını doğrular.

## Ham kanıtlar

Ham yerel çıktılar `/Volumes/MAC_EK/tellpal-qa-private/p4b5-startup-local-20261020-01/` altında kalır. Dizin yeni `0700`, her dosya yeni `O_EXCL` ve `0600` ile oluşturuldu; son izin kontrolü eşleşti. Aşağıdaki ilk başarısız test çıktıları da korundu. İlk Python fixture'ı mevcut serial kuralına uyarlanarak; ilk Flutter fixture'ındaki gereksiz IOOverrides yalnız hata testine daraltılarak düzeltildi. Bunlar geçmiş gerçek olayın nedenine ilişkin kanıt değildir.

| Log | Sonuç | SHA-256 |
|---|---|---|
| `01-dart-format.log` | İki hedef Dart dosyası formatlandı | `902bbcb4416023b163bef41c6c419398343b43e4b4f9a077aa8657c3953194c6` |
| `02-python-tests.log` | İlk fixture hataları; PASS sayılmadı | `824ba8c983fdcde584a6a6b1359a65db6fb18de9bae4024386f61904bcfa6c9c` |
| `03-flutter-tests.log` | İlk fixture hataları; PASS sayılmadı | `45c510937d3371c0aa741c55fdaed7e9334266f995866658f08acd8e94912b23` |
| `04-dart-format.log` | Yeni test fixture'ı formatlandı | `0c610b8d0c3c27b7f5ec92d917a98cd34e425c383425f71eac3cba2781eaf8bd` |
| `05-python-tests.log` | 13/13 PASS | `110ebcd6b7e3678280e957b5d92a8830fc662ae1c47eb7ab3c081d911c3dd27b` |
| `06-flutter-tests.log` | 41/41 PASS | `8ff8d6b9d4dbff7677c1f1737d37a9edc1c9b3760152f8822dd5e2a886401f45` |
| `07-flutter-analyze.log` | Sorun yok | `f65c6f8f8c8197bd22766e70a1330d178ee5a7062171015d76bac490f6cb7e0d` |
| `08-dart-format.log` | İnceleme patch'i sonrası format temiz | `1685974b88bd671112e17334bdc2abb8ef350e01ae026547459ec7a8307375ec` |
| `09-python-tests.log` | İnceleme patch'i sonrası 13/13 PASS | `19c8ce17554d6a91e2221a2a7bc1bc3590fcfb2a21416baa451a4dfcdcea667f` |
| `10-flutter-tests.log` | İnceleme patch'i sonrası 41/41 PASS | `d21b91e7bf66165a4ed2b5e7a97082de0f6ae8c8c3a65db14405b86de25569f3` |
| `11-flutter-analyze.log` | İnceleme patch'i sonrası 4 dosyada sorun yok | `68977f52316d1db3bac7606d3f72fb1f5bc30b26a304849565be78a0b25761d1` |

İnceleme patch'i sonrası aynı doğrulama seçimi yeniden çalıştırıldı: 13 Python ve 41 Flutter testi geçti; skip yoktur. Analyzer dört ilgili dosyada sorun bildirmedi. Güncel kanıt `09–11` loglarıdır. Önceki yedi log korunmuştur; tüm 11 dosyanın `0600` ve dizinin `0700` izinleri tekrar doğrulandı.

## Açık kök neden ve incelemeler

[Eski gerçek sonuç](../devtools-consumer/verification.md) salt okunur kaldı. İlk host reddinin cmdline byte'ları yoktur. Dart'ın o andaki support string'i ve resolve sonucu da yoktur. Bu nedenle tarihsel host reddinin boş/bozuk/yabancı girdiden hangisi olduğu ve tarihsel support reddinin alt nedeni hâlâ bilinmiyor. Yerel onarım yalnız sonraki retlerde bu ayrımı güvenli biçimde görünür kılar. Shell alias kaydı Dart canonical sonucu yerine geçmez.

Edge incelemesinin üst ajan tarafından iletilen taşınabilirlik bulgusu kabul edilip yalnız test fixture'larında düzeltildi: sabit `/private/tmp` Linux gibi ortamlarda bulunmayabilir ve testler bootstrap'a ulaşamadan düşebilirdi. İki kurulum sistem geçici dizininin canonical üst yolu üzerinden yeni child oluşturacak biçimde değiştirildi. Support'un ham yol denetimi ve negatif senaryolar korunur. Üretim/adapter kaynaklarında bu inceleme için değişiklik yapılmadı. Düzeltmenin yerel doğrulaması mevcut macOS ortamındadır; Linux koşusu yapılmadı. Üç incelemenin son kayıt ve kapanışı üst ajana aittir; uygulama tesliminde aşağıdaki satırlar açık bırakılmıştır.

| İnceleme | Durum |
|---|---|
| Blind | Üst ajanın son kaydı bekleniyor |
| Verification | Üst ajanın son kaydı bekleniyor |
| Edge | Geçici dizin fixture düzeltmesi ve tekrar doğrulama tamam; üst ajanın son kaydı bekleniyor |

Yeni APK veya gerçek Android koşusu yapılmadığından cihazdaki başlangıç ve P4b yaşam döngüsü kabulü açık kalır. Kapalı ölçüm spec'i tamamlandı yapılmadı; önceki başarısızlıklar, APK/run bağları, borç ve tavanlar değişmedi.

## Son koruma ve teslim kaydı

Root, 14 eski özel kanıtı ve görev dışındaki 3.377 repo dosyasının yol/SHA zincirini yeniden doğruladı; önceki değişiklikler korundu. [İnceleme kaydı](review-2026-10-20.json) üç katmanı, kapanan test bulgusunu ve son kaynak hash’lerini tutar. İlk [görev farkı](task-review.diff) korunur; taşınabilirlik patch’i dahil [son fark](task-review-0002.diff) ayrı kayıttır. İkisi başlangıçtaki çalışma ağacına göredir, eski pending hunk’ları yeni değişiklik gibi içermez.

Kaynak düzeltmesi ve yeni testler çalışma ağacında bırakılır. Eski pending kodu commit’e karıştırmamak için yerel commit yalnız bu spec ve kanıt belgelerini kapsar; push yoktur. Bu belge commit’i kaynakların temiz checkout’ta tamamlandığı iddiası taşımaz.
