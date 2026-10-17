# P4a2 sınırlı gerçek karşılaştırma doğrulaması

Tarih: 2026-10-17. **Yerel kod doğrulaması ve bağımsız inceleme tamamlandı. Gerçek run başlamadı.**

Kullanıcı 822×1334 PNG profilini ve [tek denemenin sınırlarını](bounded-run-proposal-2026-10-17.md)
açıkça onayladı. [JSON planı](bounded-run-proposal-2026-10-17.json) sonradan değiştirilmedi.
Dosyadaki eski `PROPOSED_NOT_APPROVED_NOT_EXECUTED` alanı onay öncesi anlık kayıttır.
Onayın kaynağı kullanıcı mesajıdır. Plan SHA-256:
`f024f17075a11cac9e61019503736cd384ac6ecf0db3236052c62df9fd439be3`.

Run: `p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09`.
Başlangıç commit'i: `8f7adab818db262c9faa2385d299b3f851c3ddad`.
P3 `79b6cf4` commit'i mevcut branch'in geçmişinde bulunuyor.
İlgisiz `.memlog.md` SHA-256 değeri korunuyor:
`338cc79fa0194c80e533a1bfc01c0a3349959cf88a6dfd5177c9c8f3e9e1800c`.

## Araçların sınırı

- `be/tools/p4_local_comparison.py`: sabit plan/hash, üç kaynak hash'i, salt okunur V36
  kaynak ve ayrı V37 klon kontrolü. Varsayılan çağrı input/credential/DB/ağ açmaz.
- `P4LocalComparison*.java`: refresh öncesi scheduler kaldırma, yeni ücretsiz QA kopyası,
  üç admission, yalnız seçilen scope/job için iki reconcile ve tek worker çağrısı.
  Başarısız run yeniden açılmaz; nesne veya klon otomatik silinmez.
- SDK deadline kopyaları aynı kalıcı ledger'ı kullanır. Her çağrıdan önce fsync rezervasyonu
  vardır. SDK'nin beklenen 412 metadata okuması da sayılır. Bu mantıksal çağrı kaydı,
  ham HTTP veya faturalanan işlem sayısı değildir. Mutlak in-flight SDK iptal süresi açık kalır.
- `tools/image-optimization/p4_reading_comparison.py`: login başlatmaz. Ayrı debug APK,
  yalnız `emulator-5554`, 822×1828/320 dpi/fontScale1 ve özel runtime yapılandırması ister.
  Altı medya durumu sabittir; başarısız yürütmenin attempt kaydı sıfırlanmaz.
- `app/mobile/test_driver/reading_comparison_*`: normal app girişini değiştirmez.
  Token runtime belleğinde en fazla180 saniye tutulur; compile-time define/UI/log/kanıta
  yazılmaz. Mevcut get_it/Cubit/auto_route ve HTTP/decode yolu kullanılır. Yeni cache,
  prefetch, retry, Firebase paketi veya storage süreç/IPC katmanı eklenmez.

SDK özel yapılandırması ve login için seçilen mevcut proje aynı olmalıdır. Kanonik QA UID
korunur. Login attempt rezervasyonu işlemden önce, ready makbuzu gerçek doğrulama sonrasında
yazılır. Login helper'ın bir verifier çağrısı toplam64 içinde tüketilir; backend'e63 kalır.
Token dosyası repo dışında,0700 dizinde0600 olur. Makbuz tokeni Java'ya taşımaz.

## Geçen yerel doğrulamalar

| Kontrol | Sonuç ve sınır |
|---|---|
| Backend `bash ./mvnw -o -q verify` | **503 geçti**:479 Surefire +24 Failsafe; hata/atlama0. |
| Yeni backend odaklı test |8 ledger,1 gerçek Spring kompozisyon testi ve1 PostgreSQL15.19/HTTP/disk-storage IT geçti. Gerçek encoder dört çıktı üretip otomatik LIVE oldu. |
| Python backend launcher |10 test geçti. Default I/O yokluğu, plan/kaynak/DB/credential/restart ret kapıları. |
| Python mobil sürücü |16 test geçti. Özel dosya, tek attempt, kimlik/süre, geometri/payload ve loopback kontrolleri. |
| Yeni Flutter probe |6 test geçti. Gerçek loopback HTTP ve native codec:822×1334 decode, gerçek payload boyutu, kimlik/SHA, background sonrası kaynak bırakma. |
| Flutter analyzer |`lib`, `test_driver` ve yeni testte bulgu yok. |
| Mobil regresyon |İnceleme öncesi tam **418 test** geçti;109 saniye. Son QA düzeltmelerinden sonra ilgili `reading_images` **80 test** geçti. Library/detail/reader/ses/cache ana kodu değişmedi. |

Entegrasyon testi normal public list/detail/pages HTTP görünürlüğünü, Library preview
API'sinde dışlanmayı, legacy PENDING durumunu ve mevcut kayıtların korunmasını denetler.
Storage disk fake'idir; gerçek Firebase teslimatı veya cihaz kalite kanıtı sayılmaz.
Test sırasında scheduler'ın eski adının Spring Boot4'te geçersiz olduğu görüldü.
P4 test helper'ı artık Spring'in `TaskManagementConfigUtils` sabitini kullanır.
P3 helper'ı ve normal uygulama bu görevde değiştirilmedi.

## Matris kapsaması

Spec'in altı I/O/hata satırının her biri çalışan testlerle eşlendi:

| Satır | Geçen test dayanağı |
|---|---|
| Varsayılan çağrı |Launcher ve driver default-no-I/O testleri; `defaultDoesNotRequireAnyConfigurationOrFiles`. |
| Plan/hash/kapsam/DB farkı |`test_plan_byte_drift_refuses`, `test_source_hash_drift_and_symlink_refuse`, uzak DB/klon ve mobil kimlik/süre ret testleri. |
| Bütçe/sayaç/restart |`deadlineCloneSharesBudgetAndReservesBeforeDelegateEvenWhenItFails`, concurrency/auth/SDK412 testleri ve exclusive attempt testleri. |
| Hazırlık |`oneCopyPublishesAutomaticallyWithFourOutputsButRemainsInvisibleThroughRealHttp`: gerçek PG/HTTP/encoder, disk fake. |
| Public yalıtım/bozuk medya |Aynı fixture IT; tam verify'daki mevcut delivery/comparison servis testleri ve418 mobil testindeki yanlış SHA/gövde/codec kontrolleri. |
| Mobil giriş/close/background |Probe'un tek configure ve gerçek HTTP/codec testleri; mevcut Cubit/native sahiplik/lifecycle testleri418 içinde geçti. |

Ayrı Android debug APK mevcut Gradle/SDK ile offline derlendi. Paket adı
`com.tellpal.tellpal_mobile_poc.readingcomparison.media`; normal paket yüklenmedi veya
üzerine yazılmadı. APK yalıtımı için `cicd-architecture-auditor` build/signing referansı
uygulandı. Build başarılı olması cihazda medya veya kalite kabulü değildir.

## Bağımsız inceleme sonucu

Üç bağlamsız inceleme birlikte çalıştı. Blind hunter bulgu üretmedi. Edge incelemesinin4,
verification-gap incelemesinin2 bulgusu yerel patch olarak giderildi. Ana oturumun ek
loopback/proxy incelemesi de aynı düzeltmeye eklendi. Frozen amaç ve sayılar değişmedi.

| Bulgu | Sınıf / önem | Düzeltme ve çalışan kanıt |
|---|---|---|
| Gerçek DI koruması test edilmiyor |patch / high |Ortak initializer üzerinden consumer→storage/verifier çağrısı ve bütçe aşımı; yeni Spring testi geçti. |
| Mobil restart yalnız dosya helper'ında sınanıyor |patch / medium |`run()` girişinde eski attempt korunur; VM/reverse/configure başlamaz. |
| Kanıt yazımı hatasında yanlış finished=true |patch / high |Yalnız tamamlanan verification yazımı başarı olur; hata testi geçti. |
| Cleanup hatası sonraki temizliği engelliyor |patch / medium |Bağımsız stop/socket/reverse/forward sonuçları ve son makbuz; hata testi geçti. |
| Screenshot sabit250ms bekliyor |patch / high |Aynı epoch/raster için endOfFrame ve tamamlanmış paint zorunlu; gerçek paint/stale epoch testi geçti. |
| Kısmi DI hatasında credential kalıyor |patch / high |Erken timer, revoke/close ve kısmi kurulum güvenli stop; Flutter testi geçti. |
| VM istemcisi ortam proxy/redirect kullanabilir |patch / high |Doğrudan127.0.0.1 socket, redirect0 ve HTTP101 şartı; proxy/handshake/302/close testleri geçti. |

## Gerçek çalışma kapısı ve kanıt

Bağımsız inceleme ve yerel testler geçti. Son probe hash'iyle ayrı APK ve emülatör kontrolü tamamlanmadan gerçek
fixture/cloud üretimi başlamaz. Seçilmiş özel QA giriş yapılandırması ayrıca hazır olmalıdır.
Run başladıktan sonra ek kaynak/profil veya retry otomatik açılmaz.

Gerçek çalışma raporu her altı durumda şunları ayrı kaydeder:

- LIVE kimliği, rol/sayfa, rendition key/fingerprint ve kaynak/gösterim SHA-256.
- Gerçek widget alanı/DPR ve iki eksende ihtiyaç; encoded/decode boyutları.
- HTTP okuyucusunun aldığı payload byte ve süre. TLS, header veya storage transfer toplamı değildir.
- Yükleme süresi ve transfer sonundan loaded durumuna üst süre sınırı; bu ikinci ölçüm
  checksum/buffer/codec maliyetlerini birlikte içerir, saf decode süresi değildir.
- Native sahip olunan buffer/descriptor/codec/image sayaçları; debug emülatör PSS/RSS
  anlık örnekleri. Bunlar üretim profili, GPU toplamı veya yakalanmış tepe bellek değildir.
- Aynı alanda kaynak/rendition ekran görüntüleri; background, geri dönüş ve kapama sonrası0 görüntü.

Eski JPEG byte tabanı ve canonical PNG tabanı ayrı raporlanır. Canonical PNG kaynak bu
denemeye ait kontroldür; eski JPEG'in cihazda birebir kontrol kolu olduğu iddia edilmez.
822×1334 profilinin genel üretim onayı, fiziksel Android/iPhone kalite kabulü, JPEG/WebP
bulanıklığının çözümü, P0 eksik ham kanıtı ve P4b genel tüketici entegrasyonu açık kalır.
