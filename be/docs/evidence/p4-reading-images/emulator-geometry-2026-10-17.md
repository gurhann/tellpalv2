# P4a2 Android emülatör geometri kanıtı

Tarih: 2026-10-17. Bu kayıt gerçek çalışan Flutter uygulamasındaki ağsız geometriyi ölçer.
Fiziksel Android/iPhone, gerçek medya, kalite veya performans kabulü değildir.

## Ortam ve çalıştırma

- Kod temeli: `b6a69284e9db18d3f4b7172615de7801e7b23d8c`.
- SDK ve sistem imajı `MAC_EK` üzerindedir. Emulator `36.1.9`, Android `16 / API 36`, ARM64.
- Mevcut `Medium_Phone` AVD'si disk alanı kontrolünden kaldı. `-datadir` değiştirmek bu kontrolün
  esas aldığı AVD konumunu değiştirmedi. Kontrol atlanmadı; kullanıcı dosyası silinmedi.
- Aynı sistem imajı ve ekran ayarlarıyla yeni `TellPal_P4_Geometry` AVD tanımı
  `/private/tmp/tellpal-p4-emulator-20261017/avdhome` altında oluşturuldu. SD card kapatıldı;
  `-read-only -no-snapshot -no-snapshot-save -no-cache` ile açıldı. Eski AVD verisi kopyalanmadı.
- Yeni emülatör açıldığında `pm list packages tellpal` boştu. Debug APK yalnız bu geçici
  emülatöre kuruldu. Paket kimliği `com.tellpal.tellpal_mobile_poc` idi. Bu, kalıcı cihazlar için
  ayrı paket/imzalama kapısının çözüldüğü anlamına gelmez. Mevcut uygulama üzerine kurulmadı.
- Giriş `lib/main_reading_image_comparison.dart`; `ReadingImageComparisonHarness` varsayılan
  ağsız geometri modunda kaldı. Hesap/token, backend, fixture veya sayısal medya profili verilmedi.
- Debug APK gerçek Gradle derlemesiyle üretildi. Sonra yalnız bu Flutter sürecine `SIGUSR1`
  ile hot reload yapılarak dar widget düzeltmesi ölçüldü. Son kayıt düzeltmenin SHA-256'sını taşır.
  Diskteki APK düzeltme öncesi derlemedir; yeni kurulum için güncel kaynak yeniden derlenmelidir.

## Ölçüm yöntemi

ADB ekranı `1080×2400`, density `420 dpi`, Android font scale `1.0`, portrait gösterdi.
Uygulama metni DPR `2.625` bildirdi. `ext.flutter.inspector.getLayoutExplorerNode`,
`reading-comparison-slot` anahtarlı gerçek `RenderClipRect` için tam boyut ve constraint döndürdü.
Ekran görüntüleri `adb exec-out screencap -p` ile alındı.

Sayfa ve kol düğmeleri `uiautomator` anlık metin/bounds bilgisinden seçildi. Her kayıt, görünür
kol düğmesi metniyle doğrulandı. İlk sabit koordinat denemesinde bazı tıklamalar hedefe ulaşmadı;
o deneme geçici klasöre ayrıldı ve kabul kanıtına katılmadı. Aşağıdaki altı durum yeniden alındı.

## Bulgu ve onarım

Kol düğmesindeki kısa/uzun metin, `Wrap` satır sayısını değiştiriyordu. Sayfa alanı iki kol
arasında `48 logical pixel` değişiyordu. Kapakta sabit yükseklik bulunması bu hatayı eski
kapak boyutu testinden gizledi. Yeni regresyon testi, tam rect'i iki yönde karşılaştırır;
`1.5` metin ölçeğinde eski kodda başarısız oldu. `1.0` sentetik testin geçmesi emülatör
kanıtının yerini tutmadı.

Düğme içindeki iki sabit metin `IndexedStack` ile aynı alanı ayırır. Yalnız seçili metin
gösterilir; onPressed/Cubit çağrısı, fit, raster, auth, cache ve normal rotalar değişmez.

| Alan | Önce rendition | Önce kaynak | Sonra iki kol |
|---|---|---|---|
| Kapak | `411,428571 × 457,142857` | `411,428571 × 457,142857` | `411,428571 × 457,142857` |
| Sayfa 1 | `411,428571 × 694,285714` | `411,428571 × 742,285714` | `411,428571 × 694,285714` |
| Sayfa 2 | `411,428571 × 694,285714` | `411,428571 × 742,285714` | `411,428571 × 694,285714` |

Tablo logical pixel cinsindedir. DPR ile çarpılan slot kapakta `1080×1200`, sayfalarda
`1080×1822,5` fiziksel piksele karşılık gelir. Bunlar slot ölçüleridir; doğrudan encode profili
veya seçilmiş rendition boyutu değildir. `cover/contain`, kaynak oranı ve iki eksen gereksinimi
sayısal çalışma önerisinde ayrıca değerlendirilir. Profil seçimi onaylanmış sayılmaz.

Ham kayıtlar: [önce receipt](emulator-geometry-before-2026-10-17/receipt.json),
[sonra receipt](emulator-geometry-after-2026-10-17/receipt.json).
Her klasörde kapak ve iki sayfanın iki koluna ait altışar PNG bulunur.
Örnek karşılaştırma: [önce sayfa 1 kaynak](emulator-geometry-before-2026-10-17/page1-source.png),
[sonra sayfa 1 kaynak](emulator-geometry-after-2026-10-17/page1-source.png).

## Sınırlar ve oturum sonu

Görüntüler yalnız geometri placeholder'ıdır; gerçek hikâye veya üretilmiş çıktı göstermez.
Seçilen rendition, aktarılmış medya byte'ı, decode/GPU/RSS ve görsel kalite ölçülmedi. Debug
emülatör bu kabulü sağlamaz. P0 ham kanıt sınırlaması, JPEG/WebP belirsizliği, fiziksel Android
ve iPhone doğrulaması ile P4b açık kalır. Bulut upload/admission/yayın veya DB migration yapılmadı.

Kayıt sonrası AVD adı doğrulandı ve yalnız bu oturumun `TellPal_P4_Geometry` emülatörü kapatıldı.
Geçici dosyalar silinmedi. Eski AVD, backend/frontend ve `.memlog.md` korunur.
Diskin kapasite/bölüm ayrımı [MAC_EK alan incelemesinde](mac-ek-space-2026-10-17.md) kayıtlıdır.

## Kod doğrulaması ve inceleme

Yeni iki widget testi kapak, sayfa 1 ve sayfa 2 için iki yöndeki kol değişiminde tam rect eşitliğini
ve sıfır repository çağrısını sınar. Metin ölçekleri `1.0` ve `1.5` olur. Önceki kod `1.5`
testinde başarısız oldu; düzeltmeden sonra bu dosyanın üç testi geçti.

`flutter analyze --no-pub` temizdir. İlk paralel tam suite `413 başarılı / 1 başarısız` bitti:
değişmeyen `library_artwork_test.dart` içindeki `800×1280` decoded card testi `loaded` yerine
`loading` gördü. Bu sonucun nedeni kesinleştirilmedi ve testin kodu değiştirilmedi. Emülatör
kapandıktan sonra aynı dosyanın `8` testi ayrı geçti. Ardından
`flutter test --no-pub --concurrency=1` tam suite'i **414 başarılı** ile tamamladı. Seri sonuç,
ilk paralel başarısızlığı ortadan kaldırılmış bir flaky test sorunu gibi sunulmaz.

Loglar repo dışında `/private/tmp/tellpal-p4-slot-red.log`, `tellpal-p4-slot-green.log`,
`tellpal-p4-slot-analyze.log`, `tellpal-p4-slot-suite.log`, `tellpal-p4-library-recheck.log`
ve `tellpal-p4-slot-suite-final.log` dosyalarındadır. Backend kodu değişmedi; backend testleri
bu dar onarımda tekrar çalıştırılmadı. Annotation değişmediği için generator gerekmedi.
`git diff --check` geçti. BMad one-shot bağımsız blind review sonucu `No actionable findings.`;
inceleme sonrası patch/defer/reject sayıları sıfırdır.
