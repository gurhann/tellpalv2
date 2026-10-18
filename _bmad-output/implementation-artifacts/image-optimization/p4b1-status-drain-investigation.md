# P4b1: QA kapanışında bütün işlerin sonlanması

Tarih: 2026-10-18. Salt okunur inceleme; uygulama ve test başlamadı.
Başlangıç HEAD `35d0bb384b0aba642e4cbfb8cc0a4000fdccf909`.
Bu, Epic story değildir; sprint/story anahtarı yoktur.

## Amaç ve mevcut kanıt

P4a5, aynı #94 retained LIVE çıktılarıyla dört gerçek UI paint'ini ve
13.170.884 byte aktarımı kanıtladı. Gerçek koşu source03'e aittir; son offline
cleanup/test kaynakları source05, testler 93 Python/20 Java'dır. Mobil 234 ve gerçek
backend IT 30 sonuçları önceki çalışmalardır. Bu kaynak dönemleri birleştirilmez.
[Gerçek sonuç](../../../be/docs/evidence/p4-consumer-qa/verification-2026-10-18.md)
fiziksel kalite, peak/GPU veya bütün P4 kabulü değildir.

P4b'nin görünür iş, transfer/decode ve decoded bellek hedeflerinin bazı korumaları
QA hattında zaten vardır. İlk bağımsız dilim, kanıtlanmış status kapanış boşluğunu
onarır. Genel scheduler veya üretim tüketicisi geçişi bu işin amacı değildir.

## Kanıtlı boşluk ve yeniden kullanım

| Yer | Davranış / karar |
|---|---|
| `app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart:294` | `_check()` status Future'ını başlatır; `_checking` aynı anda ikinci kontrolü engeller. |
| Aynı dosya `:46` | `retire()` iptal bildirir; iptal edilmiş status işinin gerçekten sonlanmasını beklemez. |
| Aynı dosya `:359` | `close()` account subscription ve medya `_tail` işini bekler; status Future'ı kapsam dışıdır. |
| `app/mobile/lib/app/reading_image_consumer_qa/flow_screen.dart:49` | Parent `_drain`, Cubit kapanışını tamamlanmış kabul edip route'u kapatır. Bu çağıran test edilmelidir. |
| `app/mobile/lib/features/reading_images/data/data_source/reading_image_comparison_data_source.dart:34` | Mevcut iptal `HttpClient.close(force:true)` kullanır; `finally` Timer/callback/client temizliğini sahiplenir. Yeni transport gerekmez. |
| `app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart:100` | Slot/epoch/görünürlük ve eski iş kontrolü korunur. |
| Aynı dosya `:368` | Singleton `ReadingComparisonWorkGate` eski native Future bitmeden sonraki medyayı başlatmaz. Status'u bu uzun kuyruğun arkasına koymak gerekmez. |
| `app/mobile/lib/app/reading_image_consumer_qa/consumer_image_slot.dart:42` | Yalnız güncel route seçim/paint yapar. |
| `app/mobile/lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart:39` | Mevcut piksel/decoded/peak denetimleri ve native `finally` temizliği korunur. |

Tek hedef: kapanış başladıktan sonra yeni iş kabul etmeden, mevcut medya ve status
işlerinin gerçek sonlanmasını aynı kapanış Future'ında toplamak. Tekrar `close()`
aynı kapanışı paylaşmalıdır. Geç success/error, admission veya paint üretemez;
yakalanmamış async hata da üretemez. Account/background/lease/LIVE emekliliği korunur.
Sahte timeout veya yalnız iptal sinyaliyle kapanmış sonucu verilemez.

## Anlamlı doğrulama

- `reading_image_comparison_cubit_test.dart`: cancellation bildirilse de bekletilen
  sentetik status Future'ı sonlanmadan close tamamlanmasın. Close sırasında seçim,
  retry/choice, visibility ve timer callback'i yeni I/O veya loaded sonucu üretmesin.
  İki close aynı kapanışı beklesin. Geç başarı, Either hatası ve Future exception'ı
  kontrollü sonlansın. Status yoksa mevcut kapanış bozulmasın.
- `reading_image_consumer_qa_test.dart`: gerçek parent kapanışında status gövdesini
  bekleten loopback HTTP sunucusu kullanılsın. Gerçek data source/repository/Cubit
  zinciri geçsin; server isteği ve client Future sonlanması ayrı gözlensin.
  İptal edilmiş isteğin completion'ı beklenmeden route kapanışı başarı sayılmasın.
- Mevcut medya HTTP iptali, native gate ve son-görünür sayfa testleri korunsun.
  Yeni testler gerçek bir status çağrısına ulaştığını doğrulasın; timer'ı çalıştırmadan
  kapanan veya yalnız mock cancellation çağrısı sayan test yeterli değildir.
- Status iptali çıkarma, status await kaldırma ve kapanış iş kabulü korumasını kaldırma
  mutasyonları özel test kopyasında ilgili assertion'ı bozmalı; ardından kaynak geri alınmalı.
  Bu denemeler ayrı loglanır; başarısız ham kayıtların üstüne yazılmaz.

Fixture kimlikleri ve sayıları sentetiktir; profile onayı sayılmaz. Test watchdog'ı
yalnız testin takılmasını yakalar; ürün kapanış garantisi değildir. Fiziksel cihaz
ve gerçek Firebase/storage kanıtı bu işte üretilmeyecektir.

## Normal tüketiciler: korunacak yollar

Library/detail halen `/api/test/library` kontrollü hattındadır (`authenticated_transport.dart:24`).
Normal router reader'a bağlanmamıştır (`app/router.dart:15`, `story_detail_view.dart:286`).
Ayrı PoC `story_player_page.dart:35,59,128,134,169,214,231,365` route-owned ses,
ham provider, selected precache, next-page prefetch ve URL yenilemesini taşır.
Bu yollar QA raster'a çevrilmez; ses, gezinme, kimlik ve cache sahipliği değişmez.

`preview_card.dart:198,222,234` bounded provider/precache eşleşmesini,
`story_detail_hero.dart:76,138,141` account/access kimliği ve transfer sahipliğini taşır.
`library_transfer_file_service.dart:46,62,125` gerçek client/socket sahibidir;
evict transport iptali yerine konulmaz. QA `private,no-store` cevabı normal diske
terfi ettirilmez; ADR-0018/0024 istisnası yalnız QA'dır.

Mevcut kontroller üretim onayı değildir: Library LRU 48/32 MiB (`main_library_dev.dart:41`),
PoC 24 MiB (`main.dart:11`), Library disk 80 nesne/10 dakika (`library_image_cache.dart:23`),
detail 20 dosya/100 MiB/4 owned attempt/20 MiB transfer (`library_image_cache.dart:50`,
`story_detail_image_cache.dart:8`). Bu değerler değiştirilmez.

Çoklu Cubit'te resident raster, `_render` bittikten sonra yaşayabilir; seri gate
tek başına toplam resident rezervasyonu değildir. İkinci native image sonrası
kontrol, ön rezervasyon kanıtı sayılmaz. Bu ayrı P4b sınırı açık kalır; tek-parent
QA yolunda zaten çözülen görünür iş seçimi yeniden tasarlanmaz.

## Açık ürün kapıları ve somut seçenekler

| Kapı | Ayrı karar gerektiren seçenekler |
|---|---|
| Erişim / EXT-PUBLICATION | Admitted session eski retained LIVE ile sürsün veya yeni navigation/resume'da admission yenilensin; revoke ve entitlement kaybının kesme sınırı açıkça tanımlanmalı. |
| EXT-MEDIA / URL yenileme | Mevcut backend yetkili pinned-byte teslimatı genelleştirilsin veya mevcut storage için kısa ömürlü yetkili URL/yenileme sözleşmesi kurulsun; immutable kimlik URL'den ayrı kalmalı. |
| HTTP cache | `private,no-store` ve diske yazmayan tüketim veya açık sunucu doğrulaması/yenileme şartlı account-scoped cache. TTL/offline hakkı ayrıca karardır. |
| Session / retention | LIVE, rollback, active-session ve pending-job referanslarını koruyan saklama/yenileme süresi ile cleanup sahibi seçilmeli. Mevcut disk TTL bu karar değildir. |
| OQ-2 üretim profili | Tek/çift decode ve current-only/current-plus-next yalnız deney kollarıdır. Sayısal transfer/decoded bütçe, geometri ve fiziksel Android/iPhone kanıtıyla seçilmeli. |

Bu seçeneklerin hiçbiri P4b1 onayıyla seçilmiş olmaz. Bütün genel tüketici/üretim
kapıları açık kalır. Android 1× ve bazı iPhone 0,75× PNG örnekleri genel profil
değildir; JPEG/WebP bulanıklığı ve P0 ham kanıt sınırlaması sürer.

## Yerel çalışma ve koruma kapsamı

Plan A ile onaylanırsa yalnız izlenen gerekli Flutter kaynakları, test/fixture ve
pubspec/lock dosyaları mevcut SDK/cache ile test için özel kopyaya alınabilir:
`/Volumes/MAC_EK/tellpal-qa-private/p4b1-status-drain-20261018-35d0bb3/`.
Kopya/log dizini 0700; yeni log ve makbuzlar 0600/CREATE_NEW olur. Credential/token,
`.env`, gerçek servis config'i, eski ham çıktı ve ilgisiz local-lossless dosyası kopyalanmaz.
Özel kopya yalnız bu kaynak/test dilimini içerir; yeni paket/SDK indirilmez.
Kaynak/fixture eşitliği ve dışarıda tutulan dosyalar makbuzda listelenir.

Repo'daki `.memlog.md`, `main_image_experiment.dart`, untracked local-lossless işleri,
P4a5 dosyaları, MAC_EK eski denemeleri ve `be/var` salt okunur korunur. Hash/dirty
başlangıcı implementation öncesi mühürlenir. Süreç durdurma, APK/device, Java/backend,
DB, Firebase login, config GET, storage/üretim, ledger/pencere veya fatura sorgusu yoktur.

Özel `source/app/mobile/` içinde `flutter test --no-pub test/reading_images
test/story_detail` ve ilgili `flutter analyze --no-pub` çalışır. Mevcut Library/transfer
test dosyaları da regression'a eklenir; tam izlenen Flutter test kümesi kaynak dışına
çıkan eski işleri içermeden bir kez geçmelidir. Yalnız açık, gerekçeli hata/düzeltmede
tekrar koşulur. Yeni testler gerçek loopback kullanır; yalnız build/mock yeterli değildir.

Üç bağımsız BMad incelemesi, bulgu kapanışları ve yalnız görev dosyalarının yerel
commit'i kapsam içidir. Push, PR yayını, deploy ve yeni gerçek koşu kapsam dışıdır.
