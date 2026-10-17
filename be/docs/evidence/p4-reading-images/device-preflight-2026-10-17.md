# P4a2 fiziksel cihaz ön kontrolü

Tarih: 2026-10-17, 19:45–19:47 TRT. Kod temeli: `b6a69284e9db18d3f4b7172615de7801e7b23d8c`.
Amaç, bulut üretimi başlamadan gerçek karşılaştırma alanını ölçmeye hazırlanmaktır.
Bu kayıt kalite veya performans kabulü değildir.

## Gözlenen durum

| Kontrol | Sonuç |
|---|---|
| Git | `codex/image-optimization-p3-publication`; P3 `79b6cf4` mevcut ve HEAD'in atası. Başlangıçta yalnız ilgisiz `.memlog.md` değişikliği vardı; dosyaya dokunulmadı. |
| Android | İzinli `adb devices -l` iki kontrolde boş liste döndürdü. Fiziksel cihaz ölçümü yapılamadı. İlk sandbox denemesindeki port hatası cihaz yokluğu kanıtı sayılmadı. |
| iPhone | `devicectl` gerçek uygulama envanterini okuyabildi. Önceki bağlantı zaman aşımı bu denemede tekrarlanmadı. iPhone 12 fiziksel cihazına bağlantı kuruldu. |
| iPhone ekranı | `device info displays`: nativeSize ve bounds `1170×2532`, pointScale `3`, portrait. Bu işletim sistemi ekran bilgisidir; Flutter widget slotu veya uygulama içi DPR ölçümü değildir. |
| iPhone kilidi | Ekran ışığı kapalı, `passcodeRequired=true`, `unlockedSinceBoot=true`. Cihazın kullanıcı tarafından açılması gerekir. |
| iPhone uygulamaları | TellPal Image Experiment profile/release ve ilgili UI test runner'ları mevcut. Karşılaştırma uygulaması kurulmadı; mevcut uygulama değiştirilmedi. |
| İmzalama | Yerel 9 provisioning profili çözümlendi. TellPal ile eşleşenler yalnız imageexperiment profile/release ve onların UI test runner kimlikleridir. Ayrı karşılaştırma kimliği veya wildcard profili bulunmadı. Apple portal işlemi yapılmadı. |
| Süreçler | CMS node PID `55056`, port `5173` dinlemeye devam ediyor. Port `8080` üzerinde backend dinleyicisi görülmedi. Backend/frontend durdurulmadı. |

## Kurulum ve ölçüm kapıları

Android Gradle yapılandırmasında karşılaştırma Dart girişi tek başına ayrı paket kimliği sağlamaz.
Normal kimlik `com.tellpal.tellpal_mobile_poc` olur. Mevcut `imageExperiment` seçeneği yalnız
`main_image_experiment.dart` girişini kabul eder. Karşılaştırma girişini bu seçenekle çalıştırmak
mevcut korumayı aşarak denenmez. Cihaz bağlandığında, mevcut paket envanteri okunmalı ve mevcut
uygulamayı değiştirmeyen kurulum yolu hazırlanmalıdır.

iOS'ta da karşılaştırma için ayrı target/profile yolu henüz yoktur. Image Experiment'ın bundle
kimliğini kullanarak üzerine kurulum yapılmaz. Fiziksel iPhone kurulumundan önce ayrı kimliğin
imzalama yolu çözülmelidir. Cihazın inventory'de görünmesi veya uygulama listesinin okunması,
karşılaştırma uygulamasının çalıştığı anlamına gelmez.

## Bir sonraki gerçek ölçüm

1. Android bağlanır ve USB hata ayıklama yetkisi verilir; ya da iPhone açılır ve ayrı imzalama yolu çözülür.
2. Ağsız `main_reading_image_comparison.dart` girişi mevcut uygulamayı değiştirmeyen kimlikle çalıştırılır.
3. Kapak ve iki sayfa için gerçek LayoutBuilder slotu, uygulama içi DPR, fit, yön, SafeArea ve metin ölçeği kaydedilir. Kaynak/rendition kolunda aynı slot doğrulanır.
4. Bu ölçümden sayısal PNG profil önerisi ve kaynak/bulut işlem sınırları çıkarılır; gerçek üretimden önce kullanıcıya sunulur.

Sentetik `1170×2532 / DPR 3` widget testinin cihaz ekranıyla aynı sayıları kullanması, o testteki
slot boyutlarını fiziksel ölçüme dönüştürmez. Bu denemede Flutter karşılaştırma ekranı açılmadı;
gerçek slot, seçilmiş rendition, HTTP payload, decode/bellek ve görüntü kalitesi ölçülmedi.

Kaynak adayları ve ayrı ücretsiz QA kopyası için önceki onay korunur. Sayısal profil ve gerçek bulut
çalışması açık kalır. Upload, admission, yayın, uygulama DB migration'ı veya dağıtım yapılmadı.
TR71, legacy #112, poller ve normal mobil tüketiciler değiştirilmedi.

İlgili kayıtlar: [karşılaştırma sözleşmesi](../../reading-image-comparison.md),
[önceki doğrulama](verification.md), [kaynak ön incelemesi](p4a2-preflight.md).

## Android bağlantısı yeniden denendi — 20:21 TRT

Kullanıcı Android'i bağladığını bildirdi. İzinli `adb devices -l` kontrolleri yine boş döndü.
ADB `35.0.0-11411520` sunucusu `127.0.0.1:5037` üzerinde çalışıyordu; repo SDK yolu ile
çalıştırılan ADB aynı Android SDK dizinine aitti. Sunucu kapatılmadı veya yeniden başlatılmadı.

`ioreg -a -r -c IOUSBHostDevice` ile macOS USBHost envanteri okundu. Hub, ağ, ses ve depolama
aygıtları göründü; telefon aygıtı görülmedi. Bu gözlem kablonun bozuk olduğunu kesinleştirmez;
fakat ADB yetkilendirmesinden önce USB bağlantısının doğrulanması gerektiğini gösterir. Kullanıcıdan
doğrudan Mac portu, veri kablosu, açık ekran ve Dosya aktarımı seçeneği istendi. Telefonun paket
listesi, işletim sistemi veya ekran bilgisi okunamadı. Kurulum ve gerçek widget ölçümü yapılmadı.

## Emülatörle devam

Kullanıcı `MAC_EK` diskindeki Android ortamını belirtti. Fiziksel USB cihaz kontrolünden ayrı
olarak, SDK ve `Medium_Phone` AVD tanımı diskte bulundu. Mevcut AVD disk alanı kontrolünde kaldı.
İç diskte yeni geçici AVD ile ağsız Flutter ekranı açıldı; kapak ve iki sayfanın iki kolu ölçüldü.
Kol düğmesinin metin boyuna bağlı slot değişimi bulundu ve dar widget düzeltmesiyle giderildi.

[Emülatör geometri kanıtı](emulator-geometry-2026-10-17.md) önce/sonra ölçümlerini ve ekranlarını
içerir. Bu kayıt, yukarıdaki başlangıçta çalıştırılamadı sonucunu emülatör kapsamı için günceller.
Fiziksel Android/iPhone ve gerçek medya kabulü açık kalır. Kalıcı cihazlar için ayrı paket/imzalama
kapısı kapanmadı. [Disk incelemesi](mac-ek-space-2026-10-17.md) fiziksel kapasite ile MAC_EK bölümünü ayırır.
