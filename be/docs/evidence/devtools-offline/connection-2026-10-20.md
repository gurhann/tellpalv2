# DevTools — mevcut ağsız uygulamaya bağlantı

2026-10-20. **Bağlantı doğrulandı.** Altılı yeni gösterim veya P4b ölçümü yapılmadı.

## Gerçek kanıt

- Mevcut `emulator-5554`, ARM64, ekran override 822×1828 ve density 320 korundu.
- `com.tellpal.tellpal_mobile_poc.offlineimageqa` zaten çalışıyordu. PID 4898 ve `/proc` startTicks bağlantı öncesi/sonrası aynı kaldı. Yeniden başlatma, kurulum veya build yoktur.
- Kurulu APK SHA-256: `ecd6b4bba5c87701158471b4db058053d751e7cb0d391669e3240ba23095a656`; önceki doğrulanmış APK ile eşleşti.
- MAC_EK'teki altı yerel PNG/kayıpsız WebP dosyası ve manifestin byte/SHA kontrolleri geçti. Manifest SHA-256: `7087bc93983022fed3ad920cc9864fe0cbe090892251edf5a2fc1a155c2bdaa9`.
- VM `getVM` ve `getIsolate` yanıtları gerçek süreçten alındı. Tek `main` isolate'ın kök kütüphanesi `package:tellpal_mobile_poc/main_manual_reading_images.dart` oldu.
- Mevcut Flutter 3.41.1 / Dart 3.11.0 / DevTools 2.54.1 kullanıldı. SDK sunucusu `dart --suppress-analytics devtools` ile başlatıldı; mevcut sandbox politikası yalnız loopback ağına izin verir. Global telemetry ayarı değiştirilmedi.
- DevTools arayüzünde bağlantı tamamlandı; Memory sayfasında canlı grafik ve sınıf listesi görüldü. Aynı VM'den 2.224 ms aralığı kapsayan üç bellek örneği alındı. GC, heap dump, hot reload veya debugger pause yapılmadı.
- Başlat/Sonraki/Bitir düğmelerine bu kontrolde dokunulmadı. Backend, DB, Firebase girişi ve medya HTTP işlemi başlatılmadı; P4b5 çalışma penceresi açılmadı.

## Saklama

Yeni özel kök: `/Volumes/MAC_EK/tellpal-qa-private/devtools-offline-20261020-01/`.

- `connection-verification-0001.json`: SHA-256 `6a6d265776b04d2e02972c1e9cb6c5e469c5569e245afe244f8ee93a26a12122`.
- `memory-connected-0001.png`: DevTools Memory ekranı; VM erişim adresi ekran içeriğinde görünmez.
- `connection.private.json`: özel yerel VM adresi ve yalnız bu işin port köprüsü; repoya alınmaz.
- `devtools-server-owner.private.json`: yalnız bu işte başlatılan DevTools sunucusunun sahipliği. Mevcut emülatör/QA/backend/frontend süreçleri durdurulmadı.

Eski özel köklere yazılmadı. `.memlog.md`, `main_image_experiment.dart` ve önceki P4b5 tekrar önerisi hash kontrolünde değişmedi. Bu kayıtlar başlangıç/byte haklarını iade etmez.

## Sınırlar

Bu kanıt bağlantı ve gözlemsel örnek alımını kapsar. Beklemedeki uygulamanın bellek örnekleri görsel decode/paint, kaynak bırakma veya performans kabulü değildir. Cihazın dış ağı kapatılmadı; bütün native trafik bağımsız denetlenmedi. Ağsızlık kapsamı mevcut asset tabanlı giriş ve bu adımda medya/auth çağrısı başlatılmamasıdır; DevTools bağlantısı yerel ADB/loopback üzerinden yapılır.

Tarihsel kayıt APK'yı debug olarak adlandırır; DevTools arayüzü `Flutter native (profile build)` etiketi gösterdi. Bu fark bu bağlantı adımında çözülmedi; profile veya üretim performansı sonucu çıkarılmadı. Fiziksel cihaz ölçümünden önce gerçek build/mode ayrıca doğrulanmalıdır. Emülatördeki etiket fiziksel performans kabulü sağlamaz.

Mevcut elle ekranın background ve geometri değişimi davranışı terminaldir; geri dönüşte kaldığı yerden devam ölçülmüş sayılmaz. Altılı set tam çözünürlüklü orijinal karşılaştırması değildir. Fiziksel cihaz, HTTP byte, gerçek tüketici yaşam döngüsü, üretim profilleri ve P0/P4b kapıları açık kalır.
