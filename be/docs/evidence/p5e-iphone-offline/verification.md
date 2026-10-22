# P5e — iPhone’da çevrimdışı görsel karşılaştırması

22 Ekim 2026 tarihinde fiziksel iPhone 12’de **Kocaman Bir Sarılma** ve **Pofuduk Mısır Pops** hikâyelerinin dört görseli original/PNG/WebP olarak karşılaştırıldı. **12 tam görünüm ve aynı 12 adet 1:1 yazı kesiti, toplam 24 farklı görünüm, çevrimdışı mühürlü oturum kaydıyla doğrulandı: `SEALED_VERIFIED`.** Kullanıcı son oturum için aynen **“24/24 oldu, Oturum kaydedildi mesajını gördüm”** dedi.

Kullanıcının önceki **“fark görmedim”** gözlemi ayrı insan beyanı olarak korunuyor. Son mühürlü oturum için ayrıca yeni renk/yazı kalite beyanı vermedi; native collector’ın `humanQualityAcceptance=NOT_RUN` alanı otomatik başarıya çevrilmedi.

Fiziksel 24/24 denemesi `p5e-iphone-8db5002f4a8a` içindeki önceki exact artifact ile yapıldı. Review sonrası koruma patch’i ayrı `p5e-iphone-993b7e1f5e6c` paketinde yalnız offline derlenip doğrulandı; **telefona kurulmadı**. Patch sonrasında telefon için reinstall/relaunch/stop komutu verilmedi. İki paketin fiziksel test kanıtı birleştirilmedi; resim verisi ve painter eşitliği aşağıda ayrıca kayıtlı.

Normal TellPal uygulaması, backend, Firebase, auth, storage ve canlı içerik kullanılmadı. P5c dosyaları yeniden dönüştürülmedi. Bu deneme bütün hikâyeler, üretim profili, hız kazancı veya fiziksel P3 doğruluğu için kabul üretmez.

## Doğrulanan sonuçlar

| Adım | Sonuç / kanıt |
|---|---|
| Offline build | `BUILD_VERIFIED`; mevcut SDK/cache ile ağ engelli derleme. Build/tmp/module cache MAC_EK altında. |
| Signed paket | `com.tellpal.tellpalMobilePoc.imageexperiment.release`; 43.917.416 byte. Exact mevcut provisioning profile ve cihaz kapsamı doğrulandı. |
| Bağımlılık sınırı | Sınırlı Dart import/part kapanışı. Native frameworkler yalnız `App.framework` ve `Flutter.framework`; native plugin ve Firebase yapılandırması yok. |
| Gerçek entitlement | Yalnız `application-identifier`, team identifier ve `get-task-allow`. Signed binary’de keychain grubu yok. |
| Kurulum öncesi cihaz | Exact fiziksel iPhone 12, USB/wired bağlantı ve `passcodeRequired=false` doğrulandı. |
| Eski QA veri yedeği | İki ayrı kopyadaki 65 dosya, 655.855 byte; her dosyanın boyutu ve SHA-256 değeri eşit. Eski çalıştırılabilir dosyanın geri yüklenmesi garanti değil. |
| Kurulum | Yalnız mevcut exact QA paketine güncelleme başarılı. Uygulama silinmedi. |
| Açılış | Exact `P5eOffline.app` foreground aktive edildi; PID `52155`. İlk CLI launch çağrısı seçenek sırası nedeniyle süreç başlamadan reddedildi. Doğru sırayla yapılan ikinci çağrı başarılı. CLI düzeltildi ve regresyon testi eklendi; kurulu binary değiştirilmedi. |
| Native başlangıç | Build nonce ile `start → deviceSpace → network=satisfied` kaydı alındı; bu ilk kayıtta presentation yok. İlk ağ gate’i kapalı. |
| Telefon boş alanı | Kurulum öncesi `NOT_MEASURED`. Başarılı kurulumdan sonraki native ölçüm 2.668.666.880 byte. Önceki kapasite bilgisi boş alan sayılmadı. |
| İlk insan çevrimdışı gösterimi | Kullanıcı uçak modu + Wi-Fi kapalı adımından sonra ilk görselin açıldığını doğruladı. Bu, 24 görünümün tamamlandığı veya görsel kalitesinin kabul edildiği anlamına gelmez. |
| Ekran görüntüsü | `NOT_RUN`; mevcut Device Hub ekran akışı “Connecting display…” aşamasında kaldı. Ekran görüntüsünden P3 doğruluğu çıkarılmadı. |
| Son 24 görünüm / mühür / collect | `SEALED_VERIFIED`; 84 presentation, 24 farklı görünüm, `all24Views=true`, bir seal. Exact pin/SHA, decode boyutu, geometri/filter/DPR, 1:1 ROI ve native ağ sırası validator’dan geçti. |
| İnsan renk/yazı görüşü | Önceki “fark görmedim” gözlemi saklandı. Son oturum için yeni ayrı kalite beyanı yok; collector `humanQualityAcceptance=NOT_RUN`. Son 24/24/kayıt mesajı beyanı yalnız teknik tamamlamayı doğruluyor. |
| Fiziksel P3 doğruluğu / hız ölçümü / üretim profili | `NOT_RUN`. |

## Koruma ve dar operasyon uyarlaması

Kurulum anında 3.472 repository dosyasının SHA-256 haritası ve seçilen 12 P5c dosyası + raporun SHA haritası önce/sonra eşleşti. Repository haritasının özeti `9000e275a4b6ad530bb57ca502dbc62ca826ae6e91f2309aa7ab79a67999c026`; 13 P5c girişinin özeti `ef04cd071fdbcf75ad38f10bd66f5c25f95a5b82713cb4e416507990cab9af01`. Kurulumdan sonra istenen CLI düzeltmesi ve bu rapor kaynak ağacına eklendi; kurulum anındaki tam repository hash eşitliği bugünkü tüm dosyaların hash eşitliği iddiasına dönüştürülmedi.

Ana ajan ayrıca salt okunur olarak P5c raporundaki 41 source + 82 candidate dosyanın **123 SHA’sının**, Downloads altındaki 41 orijinalin rapordaki source SHA’larının ve önceden korunmuş 88 repository dosyasının SHA’larının eşleştiğini doğruladı. Bu kontrol insan kalite kabulü değildir.

İlk kurulum denemesi, ilgisiz çalışan emülatörün stdout dosyasına 217 byte eklenince **kurulumdan önce** `PRESERVATION_REFUSED` ile durdu. Tarihsel stdout inventory’si yalnız boyut/mtime içeriyordu. Eski tam içerik veya önek SHA’sı/kopyası bulunamadı; **tarihsel stdout önek doğrulaması `NOT_AVAILABLE`**, 217 byte artış `OBSERVED` olarak kaydedildi. Geçmiş stdout byte’larının değişmediği iddia edilmiyor.

Kullanıcının onaylı kapsamı içinde, repository kaynaklarını ve doğrulanmış artifact’i değiştirmeyen özel bir operasyon adapter’ı kullanıldı. Yalnız aşağıdaki exact iki etkin log için o andaki byte önekleri sabitlendi; sonraki kontroller önek SHA eşitliği ve yalnız append koşulu aradı. Eski stderr’in tarihsel önek SHA kontrolü de korundu. Diğer **86.736 eski QA dosyasının boyut/mtime koruması**, tüm repository/P5c SHA kontrolleri, bundle inventory/signing ve snapshot kapanışı aynen uygulandı. Bütün eski QA dosyaları için tarihsel byte eşitliği kanıtlandığı söylenmiyor.

| Exact etkin dosya | Sabitlenen önek | SHA-256 |
|---|---:|---|
| `/Volumes/MAC_EK/tellpal-qa-private/manual-offline-images-20261019-01/logs/emulator-open-20261020T062855Z.stdout` | 11.052 byte | `ff941f6a26bf5ae1fd40279e84e75189750b3353263a085ad74c7d0e6d194f14` |
| `/Volumes/MAC_EK/tellpal-qa-private/manual-offline-images-20261019-01/logs/emulator-open-20261020T062855Z.stderr` | 57.590 byte | `e90ac666200f5adcaaa72f1650b47425fcd29c129286ce56f1b10a1b24b7f989` |

Eski stderr’in 56.514 byte öneğinin tarihsel SHA’sı ayrıca eşleşti: `2f05d3515ce0d5f8befa3a1ee187e52e3542ef3fd35e951402779ad91376b72e`. İlgisiz emülatöre durdurma veya değiştirme komutu verilmedi.

## Test kanıtı

| Matris / davranış | Çalışan kanıt |
|---|---|
| Manifest, SHA, ICC ve boyut reddi; P5d korumaları | Mevcut Python P5d süitindeki bağımsız SHA/boyut/ICC/byte-limit retleri geçti. Flutter P5d ve raster korumalarıyla toplam 32/32 geçti; validator/decoder salt okunur kullanılıyor. |
| İlk ağ bilinmiyor/erişilebilir | P5e Cubit testi görsel okuması başlamadığını doğruladı. Native başlangıç kaydı ayrıca `satisfied` gösterdi. |
| Ağ geri gelmesi / eski sonuç | P5e hızlı seçim testi rasterın bırakıldığını doğruladı. Python mühür validator’ı mühür öncesi ağ dönüşünü reddetti. Swift native session testi ağ dönüşünden sonra seal’i gerçekten çalıştırıp reddetti. |
| Hızlı seçim / seri decode | Gerçek raster decode ile eski seçimin sonuç üretmediği, yeni seçimin seri açıldığı ve tek raster sahipliği doğrulandı. |
| Gerçek lifecycle temizliği | Kayıtlı `WidgetsBindingObserver`, Flutter binding üzerinden `inactive` bildirimiyle çalıştırıldı; bekleyen seçim iptal edildi, raster/buffer/codec sayıları sıfır kaldı. |
| Decode / günlük hatası | Bozuk encoded giriş ve kayıt hatasında raster bırakma testleri geçti. Swift session yazma hatasından sonra kalıcı ret üretti. |
| Relaunch | Saf Foundation session bileşeni mevcut Swift SDK ile derlenip çalıştırıldı; yeniden açılışta önceki günlük korunarak yeni sessionId ile append doğrulandı. |
| 24 görünüm / mühür | Cubit 24 farklı doğrulanmış görünüm olmadan mühürlemedi; tekrar görünüm sayacı şişirmedi. Python collector exact pin, decoded boyut, DPR, filter, aynı format geometrisi, 1:1 ROI, ağ sırası ve mühür kontrollerini mutasyonlarla reddetti. İlk cihaz denemesi mühür yokluğu nedeniyle reddedildi; son cihaz oturumu 24 farklı görünüm ve geçerli offline seal ile kabul edildi. |
| Signing / exact cihaz / alan / yedek | Python testleri wildcard/yanlış cihaz/süresi dolmuş profile, ek entitlement, yanlış model/bağlantı, yetersiz host alanı, kilit ve eşleşmeyen yedeğin kurulumu engellediğini doğruladı. |
| Launch seçenek sırası | Bundle ID’den önce `--device`, `--timeout`, `--json-output` yerleşimini doğrulayan regresyon testi eklendi. |

Komutlar:

- `python3 -m unittest discover -s tools/image-optimization -p test_p5e_iphone_offline.py -v`: son tur 15/15.
- `python3 -m unittest test_p5e_iphone_offline test_p5d_offline_app -v` (`tools/image-optimization/`): 41/41.
- `flutter test --no-pub test/reading_images/p5e_offline_test.dart test/reading_images/p5d_offline_test.dart test/reading_images/reading_image_comparison_raster_test.dart`: 32/32.
- Hedefli `flutter analyze --no-pub`: sorun yok.
- Mevcut `build_runner`, Freezed ve injectable çıktıları başarıyla üretildi; yeni bağımlılık/SDK kurulmadı.
- Native AppDelegate ve session mevcut iOS SDK ile `swiftc -typecheck`: exit 0. Native session davranış testi Python süitinde gerçek Swift derlemesi/çalıştırması içeriyor.

## Review sonrası koruma patch’i

| Bulgu | Düzeltme ve çalışan kanıt |
|---|---|
| Bekleyen seal’e ikinci tıklama | `sealing` durumu kontrolleri kapatır; çift finish aynı Future’ı döndürür ve yalnız bir seal gönderir. Gecikmeli recorder ile başarı/hata ve raster temizliği doğrulandı. Yinelenen `unsatisfied` callback’i bekleyen seal epoch’unu bozmaz; gerçek ağ dönüşü ret davranışını korur. |
| Kurulum sonucu belirsizliği | Her iki CLI yolunda çağrı sınırından önce `OUTCOME_UNKNOWN` yazılır; yalnız exact native kurulum yanıtı `INSTALLED` üretir. Başlamadan kilit/yedek reddi `NOT_RUN` kalır. Her iki gerçek akışın mock device boundary timeout makbuzları doğrulandı. |
| Sonsuz bekleyen asset/decode | Initialize/read/decode ve recorder işlemlerine mevcut 30 sn sınırı uygulandı. Timeout kapalı hata üretir; kuyruk yeni çizim başlatmaz. `close` sınırlı mantıksal Future’ları bekler. SDK işinin fiziksel iptal edildiği iddia edilmez; native iş geç bir raster döndürürse o raster bırakılır. Kısa test deadline’larıyla bekleyen read/decode, close ve geç gerçek raster cleanup doğrulandı. |
| Native bootstrap hatası | Gerçek MethodChannel handler hatası veya ortam uyuşmazlığı Türkçe kapalı ret ekranı gösterir; runApp öncesi unhandled exception yolu kaldırıldı. Widget testi handler hatası/uyuşmazlık ve geçerli ortamı kapsadı. |
| Gerçek 1:1 çizim kanıtı | `P5eScreen`, 390×844 logical/DPR 3 altında gerçek painter ile çizildi. Kaynak (400,760) başlangıcındaki 8×6 tek piksellik renk deseni, RepaintBoundary çıktısındaki bitişik fiziksel piksellerle ve desen dışı arka planla birebir eşleşti. Yalnız test font fixture’ı sabitlendi. |
| Yedek doğrulama boşluğu | Gerçek `backup` ve `inventory` çalıştı; yalnız device-copy boundary iki geçici ağaç üretti. Eşit boyut/farklı içerik, eksik dosya ve boş yedek reddedildi; eşleşen iki dosyalı yedek kabul edildi. |
| 23 görünüm testinin erken reddi | Eksik görünüm mutasyonundan sonra eventSequence yeniden numaralandırıldı; test özellikle `incomplete_24_views` kodunu doğruladı. |
| Offline sandbox kanıtı | Gerçek `sandbox-exec` child süreci başladı ve sentinel yazdı; yalnız yerel socket bind EPERM/EACCES ile reddedildi. Dış/live istek yapılmadı. Sandbox eksikliği ayrıca herhangi bir child başlamadan reddedildi. |

Dokunulan runtime dosyaları `p5e_cubit.dart`, `p5e_offline_qa.dart`, `p5e_screen.dart` ve `p5e_iphone_offline.py`. Screen’e yalnız saving durum metni ve tespit key’i eklendi; painter sınıfının byte’ları eski fiziksel snapshot ile aynı. Painter SHA-256: `afb44a12a7c466ab66fb47d512671cf504a95c8a421ccaba21cbb6a6561d7650`. Pinler, text crop koordinatları, ortak viewport ve raster decoder dosyaları da byte eşit. Görsel dosyaları/format/ROI/filter/DPR çizim matematiği değiştirilmedi.

Son doğrulama: **41 Python / 32 Flutter**, hedefli analyze temiz, mevcut generator başarılı. Yeni bağımlılık/SDK kurulmadı. Build bittikten sonra generated `p5e_state.freezed.dart:217` satırındaki tek whitespace-only boşluk temizlendi; bu kozmetik belge/whitespace işlemi için test veya build tekrarlanmadı.

Yeni yalnız-build özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p5e-iphone-993b7e1f5e6c`.

| Patched artifact kanıtı | Değer |
|---|---|
| Sonuç | `BUILD_VERIFIED`, `installation=NOT_RUN`, `preservation.unchanged=true` |
| Paket boyutu | 43.933.976 byte |
| Native executable SHA-256 | `2a08d732b133188e46fe820a81d788fafee86b79f5d7a9997738b1db14160c54` |
| `result.private.json` SHA-256 | `b45ae96778ae679150ccbd8f7a6548d2e84c50be27de7f7e579018400ad69d0e` |
| `snapshot-binding.private.json` SHA-256 | `cd0264e0606c44c43fac785ff83e6531e16a4090ee225a804d61f366c2819532` |
| Build sırası repository SHA özeti | Önce/sonra `a0d4e6da9fa40615074329854ac5e1a31a0648f2105fb32243bd72adca4a2e61`; 3.473 dosya |
| Build koruması | 13 P5c SHA ve 87.458 eski QA metadata kaydı; mevcut aktif stderr öneği korunarak kontrol edildi. |

Eski fiziksel artifact’in bundle inventory’si ve mühürlü günlük/özet SHA’ları patch sonrasında da eşleşti. Telefonun ön kurulum boş alanı `NOT_MEASURED` sınırı korunuyor; yeni kullanıcı gate’i veya ölçüm SDK’sı eklenmedi.

## Özel kanıt konumları

Görseller, yedekler ve özel günlükler Git dışında tutulur. Özel run kökü:
`/Volumes/MAC_EK/tellpal-qa-private/p5e-iphone-8db5002f4a8a`.

Aşağıdaki yollar bu köke göredir:

| Kanıt | Yol | SHA-256 |
|---|---|---|
| Build receipt | `result.private.json` | `6ce17a28f9b1fd0f3d824e2141c811870318f03edba0a8071ec1f1ec791c68f5` |
| Özel operasyon adapter’ı | `operational-resume-7d69137d.private.py` | `fa3aa6dcee1cb0c96586a66c3c7478daeb69a8e64cdf701348a9c9fd9422d718` |
| Dar önek politikası | `operational-resume-7d69137d-baseline.private.json` | `1f6e72c60a0251c93da45f8082aadc31a84aef9fc712912bb8f7eb55f14ea261` |
| Operasyon sonuç bağlantısı | `operational-resume-7d69137d-result.private.json` | `ee1bd2e5cbb35f539fac77af8e6dec790584582f91f688774131b5b01b27a70e` |
| Kurulum ve yedek receipt’i | `install-attempt-73d75799/result.private.json` | `d9b6d1d69b54ee19558fc2a42dc86178915c3db70f3d41e638f55eb9f3d45167` |
| Başarılı ayrı launch receipt’i | `launch-retry-260ef62f/launch.private.json` | `ea1738eff79427465d21ead5ce51dcc2310ef15b6d90080c46d29b7fc3341bec` |
| İlk native başlangıç kopyası | `launch-retry-260ef62f/startup-session/session.jsonl` | `58e1fcf4d06de5ec3ef69489fa2031a4553b6372cdbc08ecd79f8b5b6e5c0b4f` |
| Son insan oturumu launch receipt’i | `human-retry-a8fbe812/launch.private.json` | `f27ac217c000e06b1e0c3ab39def21cc0a80a418e2a7d32f78a90ce45c4bdb18` |
| Son mühürlü native günlük | `collection-963b9e17/session.jsonl` | `5edd1f5f237db753ed26aba0088fc09cab6b1974ce460bd316ec687050e3d828` |
| Son doğrulama özeti | `collection-963b9e17/summary.private.json` | `77850e0e6e599a426f99ebe616018b836a543180bacd842a501e83b1b7c8d787` |

Kurulum receipt’inde `installation=INSTALLED` vardır. Aynı receipt’in üst durumu, ilk launch komutunun seçenek sırası hatası nedeniyle `INSTALL_FAILED` kalmıştır; bu kayıt sonradan değiştirilmedi. Başarılı launch ayrı receipt ile kanıtlanır. 65 dosyalı yedek `install-attempt-73d75799/backup-first/` ve `backup-verification/` altında; inventory SHA’sı `0b381a263d0ad1525195cfa02d3e1fe683f1fe634b23f091b5635ee203ecf985`.

## Son mühürlü oturum

Ana ajan aynı kurulu exact QA paketi için yeni oturumu `--terminate-existing` ile başlattı; reinstall veya binary değişikliği yapılmadı. Kullanıcı son oturum sonunda aynen **“24/24 oldu, Oturum kaydedildi mesajını gördüm”** dedi. Telefonun ağı açtırılmadan USB üzerinden aynı run’a alınan kayıt doğrulandı.

- Collection: `/Volumes/MAC_EK/tellpal-qa-private/p5e-iphone-8db5002f4a8a/collection-963b9e17`.
- SessionId: `4F359314-FA24-47D0-827E-7508E7D340C5`.
- Son segment: 88 olay, 84 presentation, **24 farklı caseId/crop**, bir seal.
- Sonuç: `all24Views=true`, `offlineSession=SEALED_VERIFIED`.
- Dört görsel: 1950, 1952, 8303, 8306; her biri original/PNG/WebP × tam/1:1 olarak kayda bağlı.
- Yerel validator yeniden çalıştırıldı; exact pin/SHA, 1712×2778 decode, DPR 3, aynı format geometrisi, filter, 1:1 ROI, kesintisiz olay/ağ sırası ve offline seal doğrulandı.
- Decoder colorSpace kayıtları `sRGB` ve `extendedSRGB`. Bunlar decoder metadata’sıdır; fiziksel panel P3 doğruluğu ölçümü değildir.
- Günlük SHA’sı, özetin `sessionSha256` alanı ve yeniden hesaplanan SHA eşit. Özet JSON hash’i de yukarıdaki tabloda kayıtlı.
- Collector `humanQualityAcceptance=NOT_RUN` döndürdü. Kullanıcı son mühürlü oturum için yeni ayrı renk/yazı kalite beyanı vermedi; bu alan değiştirilmedi.

## Önceki insan gözlemi ve reddedilen deneme

Kullanıcının ilk ifadesi aynen: **“24/24 bitirdim, fark görmedim”**. 24/24 sayacını ve kayıt mesajını görüp görmediği sorulunca netleştirmesi aynen: **“Hayır, sadece görsellere baktım”**. Önceki **“fark görmedim”** gözlemi ayrı insan beyanıdır; bu ilk denemede teknik 24/24 tamamlama veya mühür iddiası yoktur.

İlk USB collect kaydı `collection-1af675dc/session.jsonl` konumunda korundu; SHA-256 `2c9099a96485a7a8b80a95fbefcd65c025490de6be2ee4837d7d85bba7b32511`. Validator `sealed_session_missing` ile reddetti; başarı özeti üretmedi. İlk segmentte 74 olay, 65 presentation, 18 farklı görünüm ve sıfır seal vardı. Son olay mühür öncesi `lifecycle active=false` idi. İlk denemenin reddi son başarılı oturumla silinmedi.

İlk denemede eksik olanlar: 1950 PNG yazı kesiti; 1952 ve 8303 Orijinal/PNG yazı kesitleri; 8306 Orijinal tam görünüm. Bu altı görünüm son doğrulanan 24 görünüm içinde mevcut.

## Kapsam ve kalan sınırlar

İstenen iki hikâye/dört görsel için 24 görünüm ve çevrimdışı mühür doğrulaması tamamlandı. Teknik tamamlanma, önceki insan gözlemi ve son oturum için verilmemiş ayrı kalite beyanı birbirinden ayrıldı. Fiziksel P3 doğruluğu, hız ölçümü ve üretim profili kabulü `NOT_RUN` kalıyor; bütün hikâyelere sonuç genellenmiyor. Review sonrası kaynak korumaları yukarıda ayrı pakette doğrulandı. Fiziksel deneme önceki artifact’e aittir; yeni paket telefona kurulmadı. Son rapor güncellemesinde cihaz mutation yapılmadı. Eski fiziksel signed bundle inventory’si build ile aynı kaldı.
