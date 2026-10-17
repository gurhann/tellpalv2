# MAC_EK Medium_Phone yeniden denemesi

Tarih: 2026-10-17, 21:47–21:53 TRT. Kod: `8f7adab818db262c9faa2385d299b3f851c3ddad`.
Sonuç: **Mevcut AVD açıldı; güncel ağsız karşılaştırma ekranı ayrı debug paketiyle çalıştı.**

## Ortam ve koruma

- `df`, MAC_EK bölümünü yaklaşık 446 GiB, boş alanı 337 GiB gösterdi. Bu oturumda bölümleme,
  taşıma veya silme yapılmadı. Önceki alan engeli bu ölçümde yoktur.
- Mevcut `Medium_Phone`, SDK emulator `36.1.9` ve Android 16/API 36 sistem imajıyla
  `-read-only -no-snapshot -no-boot-anim -port 5554 -gpu auto` seçenekleriyle açıldı.
  `sys.boot_completed=1` ve AVD adı doğrulandı. Emülatör kullanıcı için açık bırakıldı.
- ADB başlangıçta çalışmıyordu; cihaz kontrolü mevcut SDK'nın sunucusunu başlattı.
  Backend/frontend süreci durdurulmadı. İlk süreç kontrolünde 5173/8080 dinleyicisi görülmedi.
- AVD'de mevcut `com.tellpal.tellpal_mobile_poc` paketi vardı. Üzerine kurulum yapılmadı.
  Ölçüm öncesi/sonrası sürüm `1.0.0`, versionCode `1`, firstInstallTime
  `2026-10-07 11:04:46`, lastUpdateTime `2026-10-08 12:51:32` aynı kaldı.
- Güncel Dart girişi `lib/main_reading_image_comparison.dart` yeniden derlendi. Yalnız bu QA
  derlemesinde geçici Gradle init script'i `androidComponents.finalizeDsl` üzerinden applicationId'yi
  `com.tellpal.tellpal_mobile_poc.readingcomparison.geometry` yaptı. Script yalnız bu giriş,
  `:app:assembleDebug` ve imageExperiment kapalıyken çalışır. Repo build kuralları değişmedi.
  Bu, kalıcı fiziksel cihaz paket/imzalama yolunun uygulandığı anlamına gelmez.
- İlk Flutter APK derlemesi geçti. İlk doğrudan Gradle çağrısı sistem Java'sı nedeniyle
  `Unsupported class file major version 69` ile kaldı. Flutter'ın kullandığı Corretto 17 açıkça
  seçilince ayrı paket `--offline` derlemesi `BUILD SUCCESSFUL` ile tamamlandı.
  APK kimliği `aapt dump badging` ile doğrulandı; mevcut QA kimliği boşken `adb install` yapıldı.
- Harness varsayılan geometri modunda kaldı. Backend, hesap/token, fixture veya medya profili verilmedi.
  Ekran görüntülerindeki renkli alanlar placeholder'dır; gerçek hikâye/rendition değildir.

APK SHA-256: `75165f15456801b23785df3c63a6b2fb48b0945fb5676533799980312e150651`.
Geçici init script SHA-256: `19e62bffbf1e6cb389368e24ef7ffc723621b568a39447fba015b99452a399a9`.
Script `/private/tmp/p4-reading-comparison-geometry.init.gradle` altındadır; kalıcı yapılandırma değildir.
Loglar `/private/tmp/p4-medium-phone-build-20261017.log` ve
`/private/tmp/p4-medium-phone-isolated-build-java17-20261017.log` altındadır. APK repoya eklenmedi.

## Gerçek çalışan ekranda geometri

Native ekran `1080×2400 / 420 dpi` olsa da AVD'de mevcut `822×1828 / 320 dpi` override'ı vardı.
Override kaldırılmadı veya değiştirilmedi. Uygulama DPR `2.0`, Android font scale `1.0` bildirdi.
Bu yüzden önceki geçici AVD'nin `DPR 2.625` ölçümüyle aynı ortam değildir.

Flutter debug servisindeki `reading-comparison-slot` anahtarlı gerçek RenderClipRect,
`ext.flutter.inspector.getLayoutExplorerNode` ile ölçüldü. Debug servis adresi ve erişim bilgisi
repo dışında tutuldu. ADB tıklama konumları açılan QA ekranındaki görünür düğmelerden alındı.
Her durumun ekran görüntüsü ve tam constraint/boyutu kaydedildi.

| Alan | İki kolda logical boyut | DPR ile fiziksel boyut | Fit |
|---|---|---|---|
| Kapak | `411×456,66666666666663` | `822×913,3333333333333` | `cover` |
| Sayfa 1 | `411×694` | `822×1388` | `contain` |
| Sayfa 2 | `411×694` | `822×1388` | `contain` |

Altı durumun kaydı başarılıdır. Her alan için canonical/rendition kollarının slot boyutları eşittir.
Kaynak kodunda `8f7adab` dışı patch veya hot reload kullanılmadı. Bu ölçüm seçim/transfer/decode
işi başlatmaz. Boyut eşitliği tam rect/konum veya bütün erişilebilirlik ölçeklerinin kabulü değildir;
önceki regresyon testlerinin sınırı ayrı korunur.

Ham kayıt: [receipt.json](medium-phone-retry-2026-10-17/receipt.json).
Ekranlar: [kapak kaynak](medium-phone-retry-2026-10-17/cover-source.png),
[sayfa 1 rendition](medium-phone-retry-2026-10-17/page1-rendition.png),
[sayfa 2 kaynak](medium-phone-retry-2026-10-17/page2-source.png).
Klasör diğer üç kolu ve ilk açılış görüntüsünü de içerir.
Altı PNG'nin hash'i receipt ile yeniden eşleştirildi. Ölçüm sonunda inspector nesne grubu serbest
bırakıldı ve yalnız bu oturumun ADB debug port yönlendirmesi kapatıldı; emülatör kapatılmadı.

## Açık kalanlar

Sayısal PNG profil seçimi ve sınırları tamamlanmış bulut çalışma onayı alınmadı. Upload, admission,
P3 yayın, gerçek medya transferi/decode ve DB migration yapılmadı. TR71, legacy #112 ve poller
değişmedi. Fiziksel Android/iPhone, kalite, encoded byte, GPU/RSS/peak ve P4b kabulü açık kalır.
Yeni uygulama kodu veya policy değişmediği için tam suite tekrar çalıştırılmadı; bu oturumun kanıtı
gerçek APK derlemesi ve çalışan ağsız UI ölçümüdür. `git diff --check` geçti; `.memlog.md` korunur.
