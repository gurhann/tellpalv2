# P4b5: başlangıç ve süreç çıkışı için yerel onarım

2026-10-20. Kullanıcı başarısız tek denemeden sonra “sorunu çözelim” dedi. Başlangıç commit'i `18d4cbafb084cf45d2052f2f68da8f2121751563`. Bu ek, mevcut in-review spec'in dar patch düzeltmesidir. Frozen amaç, eski borç, süreler ve kapalı denemeler değişmez.

## Kapsam

İlk üç inceleme aynı kod kusurlarını doğruladı. Boş PID keşfi, kayıtlı PID'nin `/proc` kimliği okunmadan sahiplik değişimi veya çıkış sayılıyor. Ayrıca ilk Flutter başlangıç hataları baseline oluşmadan kaybolabiliyor. Bunlar mevcut sahiplik, bağımsız cleanup ve gözlenebilir hata sözleşmesinin dar uygulama/test eksikleridir. Yeni ürün kararı gerekmez.

Olayın kesin kök nedeni henüz bilinmiyor. Hata anındaki alt çağrı çıktıları kaydedilmedi. Düzeltmenin bu olaydaki bütün başlangıç sorunlarını çözdüğü gerçek ölçüm yapılmadan ileri sürülemez.

Yalnız yerel kod/test/rapor düzeltmesi yapılır. Eski private kökte source-0007, APK, source/run bağları, yürütücüler, claim, terminal ve kanıtlar korunur. Gerekli credential'sız test kopyası ve yeni loglar yalnız `repairs/local-0001/` altında tutulur. Yeni APK/koşu bağı, cihaz kurulumu/başlangıcı, backend, giriş, pencere veya medya açılmaz. Bulut işlemi ve otomatik tekrar yoktur.

## Dosya ve doğrulama haritası

| Dosya | Dar düzeltme |
|---|---|
| `tools/image-optimization/p4b5_manual_consumer_qa.py` | Sahiplik ve çıkış için kayıtlı `/proc` kimliğini doğrula; boş `pidof` tek başına karar vermesin. Değişmiş/yabancı/belirsiz süreç reddedilsin. Hata tanısı yalnız sınırlı sabit alan/kodlarla kaydedilsin. |
| `tools/image-optimization/test_p4b5_manual_consumer_qa.py` | Gerçek adapter ve cleanup ile boş keşif + yaşayan `/proc`, gerçek yokluk, farklı PID/ticks/komut, okuma hatası ve force-stop sonrası yaşayan süreç testleri. Yalnız subprocess sınırı sentetik olsun. |
| `app/mobile/test_driver/reading_consumer_manual_entry.dart` | Main'in kullandığı gerçek başlangıç zincirini test edilebilir tut; support çözümleme hatası görünür olsun. |
| `app/mobile/test_driver/manual_consumer_qa/{coordinator,private_protocol}.dart` | İlk baseline öncesindeki aşama/hata sabit sırsız kodlarla gözlenebilsin. Güvenilmeyen yola yazma, dosya izinleri ve symlink denetimi korunsun. |
| `app/mobile/test/reading_images/` ilgili giriş/protokol testleri | Gerçek bootstrap → DI → coordinator → özel geçici dizinde baseline zinciri ve erken hata dalları. Medya Cubit'i, credential ve ağ işi sıfır kalsın. |
| `be/docs/evidence/p4b5-manual-consumer-measurement/local-repair-2026-10-20.md` | Kanıtlanan kusur, geçen yerel testler, değişen dosyalar ve gerçek tekrar yapılmadığı açık yazılsın. |

Kabul örnekleri:

- Kayıtlı PID'nin paket komutu ve startTicks değeri eşleşirken `pidof` boşsa sahiplik salt bu boşluk yüzünden reddedilmez. Farklı PID veya başlangıç kimliği varsa kabul edilmez.
- `pidof` boşken aynı `/proc` süreci varsa cleanup çıkış PASS yazmaz. Force-stop sonrasında da aynı süreç varsa başarısız olur. Okuma hatası yokluk kanıtı değildir.
- Gerçek giriş normal başlarken credential/configure beklemeden ilk baseline dosyası oluşur; ağ ve medya kaynakları boş kalır. Dizin, süreç veya izin hazırlığı hata verirse sınırlı sırsız hata aşaması gözlenir; güvenilmeyen yola dosya yazılmaz.
- Yerel testte initialize veya pozitif süreç kontrolü kaldırıldığında regresyon testi kırılır. Testler gerçek Android ölçümü olarak sunulmaz.

## Korunan yapı ve ilk inceleme

Flutter değişikliği mevcut QA Cubit/Freezed, get_it/injectable ve ortak consumer ekranları içinde kalır. Normal Library/detail/reader, erişim, ses, cache ve gezinme davranışı değişmez. Yeni servis, socket, kanal, storage process/IPC veya genel retry katmanı kurulmaz. Anotasyon değişmezse generator çıktısına dokunulmaz.

Kapatılmış mevcut paketin yalnız dosya yolu/izinleri salt okunur kontrol edildi. `pwd`, `pwd -P` ve `readlink -f` aynı `/data/user/0/com.tellpal.tellpal_mobile_poc.readingmanualqa` yolunu verdi; files izni 0771 ve QA alt dizini yoktu. Bu veri yeni başlangıç değildir; bu kontrolün gösterdiği yollar için symlink farkı iddiasını desteklemez.

Root üç ilk incelemeyi birlikte topladı. Kör ve sınır incelemeleri boş keşif/yanlış çıkış ve erken hata tanısı eksiklerini; doğrulama incelemesi gerçek adapter ve gerçek Flutter giriş testi boşluklarını bildirdi. Birlikte dar patch olarak uygulama ajanına gönderildi. Son değişiklikler yine üç bağımsız incelemeden geçecektir. P4b5 kabulü ve yeni gerçek koşu ayrı kalır.

## Yerel sonuç

Patch uygulandı. Flutter 197 ve Python 69 test geçti; analyzer temizdir. Üç son incelemede açık bulgu kalmadı. [Onarım raporu](../../../be/docs/evidence/p4b5-manual-consumer-measurement/local-repair-2026-10-20.md) kesin dosyaları, log/manifest hash'lerini ve mutasyon kanıtını içerir. Eski APK ve kapalı deneme değiştirilmedi. Yeni kurulum, başlangıç, backend, giriş veya medya ölçümü yapılmadı. Gerçek olayın bütün nedenlerinin çözüldüğü iddia edilmez.
