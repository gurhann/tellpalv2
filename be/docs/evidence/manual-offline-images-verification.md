# Ağsız elle görsel kontrolü — hazırlık kanıtı

Tarih: 2026-10-19. Emülatörde ekran hazırlığı ve teslimi tamamlandı.
İnsan kalite kabulü **bekliyor**.

## Kapsam

Yeni `main_manual_reading_images.dart` girişi, mevcut yerel QA Cubit/yükleyici/DI
kompozisyonunu değiştirmeden kullanır. Altı sabit PNG/kayıpsız WebP dosyası
pakete alınır. Başlat → altı sıralı görsel → Bitir akışı hazırlanmıştır.
Normal ürün girişi, Library/detail/reader, ses, erişim ve cache değiştirilmedi.
Bu, mevcut çalışma ağacına bağlı yerel araçtır; tabandaki untracked dosyalar
sessizce Git’e alınmadı. Kullanım ve sınırlar: [rehber](../manual-reading-images-qa.md).

## Yerel doğrulama

- `tests-0008.log`: 22 Flutter testi geçti. Altı sıralı paint, 822×1334 fiziksel
  geometri, tek raster sahipliği, hızlı dokunma, arka plan, geç sonuç iptali ve
  bozuk dosyada fallback yapılmaması sınandı.
- Yeni regresyon testi: Başlat öncesi alan değişebilir; Başlat sonrası alan
  değişimi terminal ret verir ve raster bırakılır.
- `analyze-0005.log`: Son üç kaynak/test dosyası için analiz temiz.
- `build-0003.log`: Son debug APK offline derlendi; build başarılı.
- BMad kör inceleme, geometri düzeltmesi sonrası tekrar inceleme ve son kanıt
  incelemesi: işlem gerektiren bulgu yok. İnceleme kaynaklı patch/defer/reject: 0/0/0.
- Başlangıçtaki 43 mevcut çalışma dosyası SHA-256 ile koruma kontrolüne alındı.
  Son kontrolde değişen dosya yok. `.memlog.md` ve ilgisiz giriş WIP’i korunuyor.

Widget testlerindeki küçük sentetik raster, sahiplik ve UI sırası içindir.
Gerçek hikâye görseli kalitesi veya fiziksel cihaz kanıtı değildir.

## İlk emülatör hazırlığı ve korunan hatalar

Mevcut `emulator-5554`, 822×1828 override ve density 320 ile doğrulandı.
Yeni paket yalnız `com.tellpal.tellpal_mobile_poc.offlineimageqa` olarak ayrıldı.

İlk APK kuruldu. İlk açılış, Başlat öncesindeki alan korumasında durdu;
görsel yüklenmedi. `screens/ready-0001.png`, ilk APK ve makbuz korundu.
Geometriyi Başlat anında sabitleyen düzeltme test edildi ve yeniden derlendi.
Bu ilk denemede düzeltmenin emülatör kanıtı yoktu. Sonraki kontrol aşağıdadır.

Son APK ile güncelleme `Requested internal only, but not enough space` hatası
verdi. Yalnız bu görevde oluşturulan ilk başarısız QA paketi kaldırıldı.
Temiz kurulum da `INSTALL_FAILED_INSUFFICIENT_STORAGE` verdi. Bu hata kayıtları
korundu. Son salt okunur `/data` kontrolünde 445.576 KiB boş alan vardı.
Bu aşamada son paket kurulu değildi. Diğer TellPal paketleri kaldırılmadı.

Yer açma için iki eski test paketinin kaldırılması kullanıcıya sunuldu.
Kurulu APK özetleri, mevcut kopyalarla salt okunur eşleştirildi:

| Paket son eki | APK byte | SHA-256 | Mevcut kopya |
|---|---:|---|---|
| `readingconsumerqa` | 85.767.575 | `5a4200f6102a341859250074474a37c25ebd006781f2534ba8101d5b592155dc` | MAC_EK, kapanmış P4b5 tekrar kökündeki `consumer.apk` |
| `readingcomparison.media` | 107.323.604 | `915377fed18f111517851a6b499f7f291b7a6a15a3b606989e4428e2aa5eb3a4` | Depodaki ignored `app/mobile/build/app/outputs/apk/debug/app-debug.apk` |

## Onaylı yer açma ve gerçek ekran kontrolü

Kullanıcı iki eski QA paketinin kaldırılmasını açıkça onayladı. Kurulu APK
özetleri yeniden doğrulandı. İki APK da yeni özel kökteki
`approved-package-backups/` klasörüne kopyalandı ve özetleri doğrulandı.
Ardından yalnız `readingconsumerqa` ve `readingcomparison.media` kaldırıldı.
İlkinde çalışan süreç yoktu; ikinci paketin süreci PID `5164` olarak tanımlandı.
Onay kapsamındaki bu uygulamaların emülatör yerel verileri de kaldırıldı.
MAC_EK’teki eski kanıtlar, kaynaklar ve APK’lar korundu.

`approved-storage-preflight-0001.json`, `approved-uninstall-0001.claim.json`
ve ilgili stdout/stderr dosyaları bu işlemi kaydeder. Yer açma sonrası boş alan
528.968 KiB oldu. Normal `com.tellpal.tellpal_mobile_poc` APK’sının SHA-256
özeti işlem öncesi ve son kontrolde aynı kaldı. Normal uygulama kaldırılmadı.

Son APK temiz kuruldu. SHA-256:
`ecd6b4bba5c87701158471b4db058053d751e7cb0d391669e3240ba23095a656`.
Boyutu 90.067.657 byte; bu APK dosya boyutudur, medya HTTP ölçümü değildir.
`am start -W` çıktısı `Status: timeout` bildirdi. Başlangıç kabulü bu komutun
durumuna dayanmadı; gerçekten açılan `screens/ready-0002.png` incelendi.
Başlat düğmesi ve başlangıç yönergesi görünür durumdaydı.

VM veya Flutter Driver komutu kullanılmadan, ekrandaki düğmelere dokunularak
tek bir altılı kontrol yapıldı. Her adımın tam ekran görüntüsü saklandı.
822×1828 ekranın `(0,195)–(822,1529)` alanındaki RGB pikselleri ilgili
822×1334 yerel PNG referansıyla karşılaştırıldı. Altı alanda da fark yoktu.

| Görsel | PNG ekranı | Kayıpsız WebP ekranı |
|---|---|---|
| Kapak, asset `9778` | Piksel eşitliği geçti | Piksel eşitliği geçti |
| Sayfa 1, asset `9816` | Piksel eşitliği geçti | Piksel eşitliği geçti |
| Sayfa 2, asset `9818` | Piksel eşitliği geçti | Piksel eşitliği geçti |

Son ekranın `6/6 · Sayfa 2 · kayıpsız WebP` etiketi ve Bitir düğmesi görsel
olarak doğrulandı. Bitir’e bir kez dokunuldu; `screens/finished-0001.png`
üzerinde “Kontrol bitti” sonucu doğrulandı. Kontrolün özeti
`manual-smoke-0001-six.json`, adımları `manual-smoke-0001-case-*.json` ve
ilk adım için `manual-smoke-0001-first.json` dosyalarındadır.

Kullanıcı teslimi için yalnız bu göreve ait QA süreci, PID `18610` doğrulandıktan
sonra kapatıldı. Uygulama yeni oturumda açıldı. `screens/ready-handoff-0001.png`
incelendi: **Başlat** düğmesi hazır; henüz görsel yüklenmemiş ve üç dakikalık
süre başlamamış durumda. `handoff-0001.json` bu teslimi kaydeder.

Bu sonuç ekranın açıldığını, dosya sırasını ve bu emülatörün 1:1 gösterimini
doğrular. Yeni kaynak kullanımı veya yaşam döngüsü yük testi yapılmadı.
İnsan netlik/kırpılma değerlendirmesi, fiziksel cihaz ve P4b kabulü değildir.

## Saklama ve kabul sınırı

Kaynak/test/build, üç manuel APK, iki eski QA APK yedeği, loglar ve ekranlar:
`/Volumes/MAC_EK/tellpal-qa-private/manual-offline-images-20261019-01/`.
Yeni kayıtlar eski QA köklerine yazılmadı. İlk dosya manifest özeti:
`7087bc93983022fed3ad920cc9864fe0cbe090892251edf5a2fc1a155c2bdaa9`.

Yeni bulut üretimi, Firebase girişi ve backend/DB başlangıcı yapılmadı.
Gerçek HTTP byte veya kaynak kullanım kabulü üretilmedi. Önceki P4b5 terminal
denemesi tekrar açılmadı. Fiziksel Android/iPhone, P4b yaşam döngüsü ve genel
üretim profili kabulü açık kalır. P0 ham kanıt eksikleri korunur.
