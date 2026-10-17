# P4a1 manifest ve geometri doğrulaması

Tarih: 2026-10-17, Europe/Istanbul.
Başlangıç commit'i: `79b6cf4e64da7a41efc9185d4c003c412c0d63fe`.
Kapsam: `_bmad-output/implementation-artifacts/spec-p4a-reading-image-selection.md`.

## Uygulanan davranış

- `reading_images/domain` URL taşımayan immutable descriptor, kimlik, rol/bağ,
  sabitlenmiş kaynak metadata'sı, profil ve rendition tiplerini içerir. Listeler ve
  iç içe profil koleksiyonları kopyalanarak değişikliğe kapatılır.
- Doğrulayıcı beklenen content/dil/READING/revision/digest/job kimliğini, kapak/detail
  bağını, tam ve sıralı farklı sayfaları, kaynak eşleşmesini, profil kümesini,
  pozitif metadata'yı, SHA-256 biçimini ve P2 rendition kimliğini denetler.
  Dil, backend'in mevcut `tr/en/es/pt/de` canonical kümesiyle sınırlandırılır.
  `ReadingWork.Manifest` metadata yapısı yeniden kurulur ve P2 canonical SHA-256
  digest'i beklenen kimlikle karşılaştırılır. Çıktı yolu, sürümü, byte sayısı ve
  SHA'sı bu bağ kurulmadan doğrulanmış wrapper üretilmez.
  Boyutlar P2'nin floor-fit kuralına uymalıdır. Eksik descriptor kısmen kabul edilmez.
- Yalnız doğrulayıcının oluşturabildiği doğrulanmış descriptor seçiciye verilir.
  Seçici slot ve DPR'yi her çağrıda yeniden hesaplar. Fiziksel slotun ceil işleminden
  sonra `contain`/`cover` ölçek karşılaştırması ve iki eksenin ceil işlemi BigInt
  kesirleriyle yapılır. İhtiyaç kaynak boyutuna kırpılmaz. Kabul edilmiş profiller arasından
  iki ekseni karşılayan en küçük alan, ardından byte ve rendition kimliği seçilir.
- Decode boyutları da tek exact kesirle hesaplanır ve rendition boyutunu aşmaz. Piksel bütçesi
  zorunlu girdidir. Kaynak, rendition ve decode kaynağı yetersizlikleri ayrı tipli
  redlerdir. Orijinale düşme veya upscale yolu eklenmedi.

## Çalıştırılan kontroller

Komutlar `app/mobile/` altında çalıştırıldı.

| Komut | Sonuç |
|---|---|
| `dart format lib/features/reading_images test/reading_images` | Başarılı. |
| `flutter test test/reading_images` | İnceleme onarımından sonra 52 test başarılı. |
| `flutter analyze` | Başarılı; `No issues found!`. |
| `flutter test` | İnceleme onarımından sonra 392 test başarılı. Mevcut Library/detail/reader ve deney testleri dahil. |

Yeni testler yanlış/eksik kimliği, yanlış kaynak/nesne sürümünü, eksik/yinelenen/sırasız
rolleri ve sayfaları, profil/metadata/hash/MIME/byte/kimlik/floor-fit hatalarını ve
koleksiyonların sonradan değiştirilememesini kapsar. Seçim testleri tall/wide,
contain/cover, fractional DPR, pencere ve DPR değişimi, alan/byte/kimlik sıralaması,
tek eksen eksiği, kaynak yetersizliği, açık decode bütçesi ve fallback reddini kapsar.
96 geometri kombinasyonu bağımsız tamsayı kesri ve exhaustive aday oracle'ıyla sınanır.

İlk analizde yeni dosyalarda yedi süslü parantez biçim uyarısı görüldü; düzeltildi.
SDK'nin `engine.stamp` cache yazısı sandbox dışında kaldığı için ilk tam test denemesi
başlayamadı. İzinli tekrar ve son analiz başarılıdır. Anotasyon değişmediğinden
`build_runner` gerektiren yeni üretim yoktur.

## Bağımsız inceleme sonrası tek onarım turu

İlk sürümde 28 yeni test ve toplam 368 test geçmişti. Bağımsız incelemeler double
yuvarlama hatalarını, maskelenmiş bağ bütünlüğü testlerini, eksik allowGrowth kabul
yolunu, çıktıları digest'e bağlamayan ilk kontrolü ve fazla geniş dil kabulünü buldu.
Bu eksikler aşağıdaki kalıcı regresyonlarla onarıldı. Son analiz öncesi canonical
helper'daki bir süslü parantez uyarısı da düzeltildi.

| Bulgu | Düzeltme ve çalışan test adı | Test sayısı |
|---|---|---|
| F1 | İhtiyaç exact BigInt kesrinden hesaplanır. `F1 exact ihtiyaç sınırı` testleri 1005×670→375×250, 2400×1600→204×136 ve 11×77→9×63 sınırlarını sınar. `F1 BigInt çapraz çarpımları int64 taşmasını önler` büyük alanlarda geometri ve sıralamayı sınar. | 4 |
| F2 | Decode exact kesirden hesaplanır. `F2 exact decode 218x218 tam bütçede kabul edilir` 47524 pikseli kabul eder. `F2 exact decode 218x218 bütçeden tek piksel fazla ise reddedilir` 47523 bütçesini reddeder. | 2 |
| F3 | `F3 aynı bozuk descriptor ve context incompleteBindings` aynı bozuk listeyi her iki girdiye verir: duplicate pageId, sırasız pageNumber, yanlış sayfa rolü ve farklı cover/detail kaynağı. Doğrudan `incompleteBindings` beklenir. | 4 |
| F4 | `F4 allowGrowth false/true aynı ... byte çıktısını reddeder/kabul eder` 1000000 ve 1000001 byte için false/true çiftlerini sınar. Bu sentetik kabul, üretim büyüme izni değildir. | 2 |
| F5 | `F5 gerçek Java P2 canonical golden ve digest birebir eşleşir`; `F5 sabit digest altında çıktı ... değişimi reddedilir` path/version/SHA/byte varyantları; aynı bozuk digest, çıktı dizisi sırası, snapshot bağı, Unicode metadata ve content/dil/revision alanlarının digest bağını sınayan beş ek test. | 10 |
| F6 | `F6 context ve descriptor aynı zz dilini taşısa da invalidIdentity` ve `F6 backend canonical dil kümesinin tamamı kabul edilir`. | 2 |

Toplam 24 regresyon eklendi; önceki 28 test korundu. F3 için kontrollü mutasyon
denemesi de yapıldı: her iki `_completeBindings` çağrısı geçici kaldırıldığında
dört F3 testi de başarısız oldu (`+0 -4`). Kaynak dosyası `finally` ile birebir geri
yüklendi; ardından son tam test paketi çalıştırıldı. Mutasyon kalıcı kodda yoktur.

### Bağımsız canonical golden

Golden, mevcut `be/target/classes` altındaki gerçek `ReadingWork.Manifest` ve
`ReadingIdentity` sınıflarıyla üretildi. Backend, DB veya bulut başlatılmadı.
Üretici kaynak: `be/docs/evidence/p4-reading-images/P4ManifestGolden.java`.
Java çıktısı `app/mobile/test/reading_images/fixtures/p2_manifest_golden.json`
dosyasında tutulur. Golden; sıralı dizileri, null pageId'yi, P2 alan adlarını,
tırnak/ters eğik çizgi kaçışını, kısa kontrol kaçışlarını ve `\\u0001/\\u001f/\\u007f`
ASCII kaçışlarını kapsar. Beklenen digest:
`e2b4ff6ff58310a42e25d300f8f0187ed73d36c8d9ceb718052cda595d4f71e9`.

Depo kökünden yeniden üretim:

```sh
javac -cp be/target/classes -d /private/tmp be/docs/evidence/p4-reading-images/P4ManifestGolden.java
java -cp be/target/classes:/private/tmp P4ManifestGolden /private/tmp/p4-manifest-golden.json
cmp /private/tmp/p4-manifest-golden.json app/mobile/test/reading_images/fixtures/p2_manifest_golden.json
```

## Kanıtın sınırı ve açık işler

Descriptor denetimi P2'nin `revision/snapshot/outputs/binding/stored` metadata
yapısını kurar; sıralı dizileri ve ASCII canonical escaping kuralını korur.
`tellpal.reading.manifest.v1` domain SHA'sı yeniden hesaplanıp beklenen identity
digest'iyle karşılaştırılır. Rendition kimliği de rol, kaynak SHA'sı ve profil
parmak izinden P2 domain ayracıyla yeniden hesaplanır. P2 manifestinin içermediği
orijinal kaynak metadata'sı ve kabul edilmiş profil metadata'sı ayrıca beklenen
bağlamla eşleştirilir. Job/experience kimliği beklenen bağlamla doğrulanır.
Descriptor kontrolü gerçek nesne byte'larının SHA/MIME/boyut doğrulaması veya erişim
izni değildir. İlk sürümün eksik digest kontrolü yukarıdaki onarım geçmişinde korunur.

Çok adaylı test descriptor'ları yalnız seçici sıralamasını sınayan sentetik
girdilerdir. Mevcut P2'nin rol başına tek çıktısı değişmedi; varyant merdiveni üretilmedi.

Başlangıç incelemesinde bağlı Android cihaz bulunmadığı, fiziksel iPhone'un çevrimdışı
olduğu kaydedildi. Bu uygulamada cihaz/simülatör başlatılmadı. Fiziksel cihazda gerçek
aktarım, decode, görüntü kalitesi, peak/settled bellek ve yaşam döngüsü kanıtları
`unavailable` durumundadır; başarılı testler bu kanıtların yerine geçmez.

Normal tüketici, UI, ağ/storage/cache, ses/auth, üretim ayarı ve backend uygulaması
değiştirilmedi. `.memlog.md` ve mevcut süreçler korundu. P4a2 teslimat/karşılaştırma
ve P4b kaynak/yaşam döngüsü kararları açıktır; bu çalışma tamamlanmış P4 değildir.
Bağımsız üç BMad incelemesinin bulguları bu tek onarım turunda uygulandı.
Son kabul ve iş akışı durumunu ana oturum yönetir.

## P4a2 kontrollü karşılaştırma uygulaması — 2026-10-17

Başlangıç commit'i `39fee0c9106cd908f1aaade41545cdf858a36237`.
Kaynak sözleşme [P4a2 spec](../../../../_bmad-output/implementation-artifacts/spec-p4a2-reading-image-comparison.md)
ve [teslimat sözleşmesi](../../reading-image-comparison.md) dosyalarıdır. Bu bölüm önceki P4a1
sonuçlarını değiştirmez. Backend/Flutter kodu, gerçek bulut çalışması ve fiziksel cihaz kabulü ayrıdır.

Yeni local/test endpoint'leri her istekte revoke kontrollü gerçek bearer yapılandırması ve tek QA UID
ister. Sunucu ayrı Türkçe ücretsiz iki sayfalı kopyayı seçer; TR71 reddedilir. Kimlik retained
`live_proof_json` içinden alınır. Storage I/O kısa DB kesitlerinin dışında, generation-pinned ve bounded
çalışır. Aynı kimlik/free/editorial/source/availability son kesitte yeniden denetlenir. Body başlamadan
SHA/MIME/gerçek header/tam decode doğrulanır. Canonical kol yalnız PNG'dir. Tüm yanıtlar
`private,no-store` taşır; yanlış binding ve Range reddedilir. Handler gate'i response yazımı/kopması veya çalışma süresine bağlı abort cleanup
bitene kadar tek teslimatı tutar. V37 mantıksal bearer doğrulaması ve storage payload okumasını işlem/byte rezervasyonlarına bağlar; başarısızlıkta ve restart'ta sayaç korunur. Yapılandırma/P3 kapısı ve toplam rezervasyon Firebase I/O öncesinde denetlenir. SDK'nin credential/transport iç çağrıları için fiziksel bulut fatura ölçümü yapılmış sayılmaz.
Migration yalnız Testcontainers veritabanlarında uygulandı; mevcut yerel/üretim DB değiştirilmedi.

Flutter domain repository/use-case→JsonSerializable data→injectable Freezed Cubit→ayrı auto_route
screen/view yolunu kullanır. P4a1 domain seçici/doğrulayıcı değiştirilmedi. Yeni giriş varsayılan olarak
ağsız geometri modudur. Mevcut geçici credential sağlayıcısının revoke akışı izlenir; static token
modu yoktur. Ortak seri kapı eski HTTP/native Future bitmeden yeni medya işi başlatmaz. Native
buffer/descriptor/codec/image sayaçları iptal/başarısızlık/başarı sonrası sıfıra dönmeyi sınar. Yaşam
döngüsü, route örtülmesi, hesap değişimi, status hatası, lease ve kontrol sınırları görseli temizler.
Decode süresi aşılırsa hata hemen görünür; iptal edilemeyen codec sonlanana kadar kapasite tutulur.
Bellek toplamları BigInt ile hesaplanır; int64 taşması admission sağlayamaz. Global/disk ImageCache
ve precache yolu eklenmedi. Source kolu tam kaynak boyutunda; rendition kolu P4a1 decode boyutunda
çözülür. Her ikisi aynı slot, fit ve merkez hizasını kullanır.

### P4a2 matris denetimi

| Spec satırı | Çalışan test / kanıt | Sınır |
|---|---|---|
| Geçerli bağlam aynı LIVE kaynak/çıktı byte'ını verir | `ReadingImageComparisonServiceIT.retainedJobComesFromLiveProofAndPinnedBytesAreVerified`; Flutter `gerçek HTTP descriptor P4a1 digest ve tüm rol/sayfa profilleriyle doğrulanır` | PostgreSQL ve yerel storage/HTTP; gerçek Firebase oturumu değildir. |
| Yanlış UID/dil/rol, kilitli içerik veya eski LIVE reddedilir | `realBearerIsRequiredAndEveryAuthErrorIsNoStore`, `stubAndRevocationDisabledConfigurationsFailClosed`, `lockedCopyAndForgedGenerationRefuseBeforeStorage` | Firebase verifier sentetik porttur; token/revoke gerçek cloud kanıtı açıktır. |
| I/O sırasında erişim kapanırsa son response reddedilir | `freeAccessChangeDuringIoRejectsFinalResponseAndDoesNotWithdrawHealthyMedia` | Gerçek DB kesitleri + gerçek pinned okuma. |
| Eksik/bozuk çıktı açık hatadır; sağlıklı LIVE kapasitede korunur | `corruptPinnedOutputWithdrawsAvailabilityAndParallelCapacityRefusesWithoutWithdrawal`, `actualHeaderAboveDeliveryPixelBudgetRefusesBeforeFullDecode` | Gerçek pinned dosya bozulması; kaynak/metadata tamlığı mevcut P2/P3 suite ile birlikte sınanır. |
| İki eksen yetersizse original/upscale seçilmez | P4a1 `tek eksen eksiği, kaynak upscale ve original fallback reddedilir`, `decode açık piksel bütçesine uyar; ihtiyaç bütçeye kırpılmaz` | Rol başına tek P2 çıktı korunur. |
| Aynı slot/fit/alignment ve kol değişimi | `ağsız geometri aynı slotta kaynak ve rendition kolunu değiştirir` | Sentetik Flutter viewport; fiziksel kalite kanıtı değildir. |
| Byte/decode sınırı ve gerçek payload bütünlüğü | `gerçek yerel HTTP private no-store ve uzun gövde sınırı`, `gerçek HTTP payload SHA eşleşir; yanlış SHA başarı sayılmaz`, `gerçek header metadata/piksel bütçesini aşınca codec ve kaynak bırakılır` | Gerçek loopback HTTP ve native codec; üretim ağ/süre garantisi değildir. |
| Eski tamamlanmalar, hesap/background/close iptali | `gerçek HTTP iptali yavaş gövde Future sonlanmasını bekler`, `eski Future bitmeden yeni iş kapasite alamaz ve geç admission atılır`, `hesap revoke anında görsel sahipliğini ve pending admission neslini kapatır` | Codec anlık iptal edilmez; Future sonu beklenir. |
| Status/lease/kontrol hatası görünür işi kapatır | `görünür status hatası pending byte işini eski nesil yapar ve tekrar denemez`, `lease süresi pending decode/transfer neslini kapatır; yeni LIVE otomatik alınmaz` | Polling aralığında anlık revoke garantisi yoktur. |
| Tek server medya işi yavaş response boyunca tutulur | `ReadingImageComparisonTestControllerIT.slowActualHttpBodyKeepsGateUntilDisconnect` | Gerçek Tomcat, yavaş TCP socket ve paralel ikinci HTTP isteği. |
| Toplam işlem/byte tavanı reset/taşma ile aşılamaz | `durableRunReservationsDoNotResetOrRaiseCeilings`, `explicitMemoryArithmeticRejectsOverflowAndInsufficientPeak`, Flutter `native int64 sınırındaki bellek toplamları taşma ile admission alamaz` | V37 yalnız bu teslimatın payload okuma rezervasyonlarını sayar; fixture yazılarını yetkilendirmez. |
| Ayarsız giriş buluta dokunmaz | `çalışma ayarları yokken ağ çağrısı yapılmaz`, backend `disabledRunRejectsBeforeAnyFirebaseCall`; properties varsayılanları kapalı | Gerçek run/profil seçilmedi. |
| Normal Library/detail/reader/ses/cache korunur | Tam Flutter suite ve tam backend verify; `ApplicationModulesTest` yeni dar user→content yönünü doğrular | Üretim rollout veya P4b kabulü değildir. |

### Gerçek deneme ve cihaz kapıları

Bu görevde gerçek fixture oluşturulmadı; canonical PNG yükleme, source admission, P3 yayın veya
Firebase teslimat denemesi yapılmadı. Kullanıcı kapak `9778`, sayfa `9816/9818` canonical PNG adaylarını, yalnız ayrı QA kopyasının ücretsiz erişimini ve dar modül yönünü onayladı. Ayrı QA kopyasının gerçek content/admission/job kimliği, sayısal rol profilleri, tam çalışma kaydı, kaynak/çıktı ve toplam bulut tavanları henüz oluşturulmadı. Ön incelemede legacy JPEG ve yerel canonical PNG byte tabanları zaten ölçülüp hash'lendi:

| Kaynak | Legacy JPEG byte | Yerel canonical PNG byte |
|---|---:|---:|
| Kapak 9778 | 1.048.225 | 6.336.857 |
| Sayfa 1, 9816 | 1.122.418 | 5.941.567 |
| Sayfa 2, 9818 | 1.122.572 | 6.122.778 |

Hash ve RGB eşitliği kanıtı [ön incelemede](p4a2-preflight.md) ve [nesne kaydında](tr71-source-preflight.json) korunur. Bunlar admitted/cloud çıktı kimliği değildir. Henüz ölçülmeyenler gerçek karşılaştırma HTTP payload'ı, üretilen/seçilen rendition, cihaz decode/tepe bellek/zaman ve kalite görüntüleridir. Bu alanların boşluğu sentetik testlerle kapatılmaz. TR71 erişimi, kategori, legacy poller,
normal yayın/işleme bayrakları ve çalışan süreçler değiştirilmedi.

Android: `adb devices` fiziksel cihaz göstermedi. iPhone: inventory fiziksel iPhone 12'yi
`available (paired)` gösterdi; gerçek `devicectl device info apps --search tellpal --timeout 10`
bağlantısı timeout oldu. Mevcut provision profilleri yalnız imageexperiment.profile/release ve
UITest kimlikleri içindir. Ayrı comparison kimliği için uygun mevcut profil bulunmadı. Cihaza hiçbir
uygulama kurulmadı veya mevcut uygulama verisi değiştirilmedi; Apple portal işlemi yapılmadı.
Fiziksel ağsız geometri, Android/iPhone kalite ve performans kabulü **açık** kalır. Inventory durumu
başarılı cihaz yürütmesi olarak raporlanmaz.

### Sentetik slot ölçümü

`reading_image_comparison_view_test.dart`, viewport'u açıkça `1170×2532` fiziksel piksel ve
DPR `3` olarak kurar. Gerçek raster slotu sabit widget anahtarıyla ölçülür; AppBar/scroll clip'i
ölçülmez. Kapak slotu `390×433,333333` logical pixel, `cover`, merkezdir. Sayfa slotu
`390×652` logical pixel, `contain`, merkezdir. Kol değişiminde slot boyutu aynı kalır ve ağ
çağrısı sayısı sıfırdır. Bu sentetik viewport sonucudur; iPhone veya Android cihaz ölçümü değildir.

Lease/status zamanlama testleri byte işine giriş sinyalinden sonra enjekte edilmiş manuel saatle
`9 ms` açık / `10 ms` kapalı sınırını doğrular. Üretim saatinin `Timer`/`DateTime` davranışı ve
süre değerleri değişmez. İlk wall-clock `40 ms` test beklemesi yoğun tam suite'te admission'dan önce
sayılabildiği için kararsızdı; keyfi beklemeyi büyütmek yerine bu kontrollü saatle değiştirildi.

İstemci iptali loopback HTTP'de sınandı. İstemci koptuğunda backend'in henüz body başlamamış
pinned SDK okumasının anında kesileceği iddia edilmez; SDK okumayı bitirene/kendi süre kontrolüne
ulaşana kadar backend gate'i tutulur. SDK slow-trickle/auth mutlak sonlanma yeterliliği ve gerçek
Firebase cihaz aktarımı bu testlerle kapanmaz.

### Son kod doğrulaması

| Komut / kapsam | Sonuç |
|---|---|
| `be/`: Colima socket'i açıkça seçilerek `bash mvnw -q verify` | Başarılı; 470 Surefire + 13 Failsafe testi, 0 failure/error/skip. Yeni karşılaştırma IT'leri 4 controller + 5 content + 3 asset testidir; kalan Failsafe module dokümantasyonudur. |
| `app/mobile/`: `dart run build_runner build --delete-conflicting-outputs` | Başarılı; DTO, Freezed, injectable ve auto_route çıktıları generator ile üretildi. |
| `app/mobile/`: `flutter analyze` | Başarılı; `No issues found!`. |
| `app/mobile/`: `flutter test` | Başarılı; son enjekte saat/DI ağacı ile 406 test. |
| `git diff --check` | Başarılı. |

Son geçerli backend logu `/tmp/p4a2-verify-final-tree.log`, raporlar `be/target/surefire-reports`
ve `be/target/failsafe-reports` altındadır. Son Flutter logu `/tmp/p4a2-flutter-final-tree.log`,
analyzer `/tmp/p4a2-analyze-final.log`, generator `/tmp/p4a2-build-runner.log` dosyalarıdır.
Ağsız geometri çıktısı `/tmp/p4a2-reading-final2.log` içindedir. Bu geçici loglar repoya eklenmez.

İlk sandbox denemeleri SDK cache yazısını ve Docker/process ölçümünü engelledi; izinli tekrarlar
Colima'nın mevcut socket'i ile gerçek PostgreSQL üzerinde tamamlandı. İlk yeni controller binding
hatası 422 yerine 400 olacak şekilde düzeltildi. V37 sonrasında iki eski migration testinin yalnız
latest version beklentisi `36→37` güncellendi; veri ve constraint assert'leri korundu. Önceki başarısız
denemeler son yeşil sonuç yerine kullanılmadı. Bağımsız BMad incelemesi ve iş akışı durumunu ana
oturum yönetir; fiziksel cihaz ve gerçek bulut çalışması bu kod doğrulamasından ayrı kalır.

### Bağımsız inceleme sonrası onarım kanıtı — 2026-10-17

F1–F14, onaylanan davranışın teknik onarımı ve test boşluğunun kapatılmasıdır. Frozen spec bloğu,
P2/P3 fingerprint sınıfları ve P4a1 seçici/doğrulayıcı değiştirilmedi. Sayısal üretim profili veya gerçek
fixture/cloud çalışması seçilmedi. Önceki doğrulama sonuçları yukarıda tarihçe olarak korunur.

| Bulgu | Onarım ve çalışan test |
|---|---|
| F1 | `ReadingImageComparisonServiceIT.transport429503AndResetPreserveLiveButAuthoritativeMissingWithdraws`, `completePinnedWrongShaWithdrawsLive`, `completePinnedWrongMimeWithdrawsLive`: transport belirsizliği sağlıklı LIVE'ı korur; authoritative eksiklik ve tam okunmuş bozuk byte withdrawal üretir. |
| F2 | `F2 anchor identity değişimi background retry ve kol değişiminde byte öncesi reddedilir` ve aynı `generation` testi: ilk LIVE anchor route/Cubit yaşamı boyunca korunur. Gerçek PNG testi aynı anchor ile source/rendition geçişini başarıyla tamamlar. |
| F3 | `ReadingImageComparisonTestControllerIT.encodedHandlerUsesSameGateAndSlowOpenSocketTimesOutWithPayloadCleanup`: gerçek Tomcat'te kodlanmış prefix ve `/byt%65s` gate'i aşamaz; status gate beklemeden 200 döner; tüm yanıtlar no-store'dur. |
| F4 | `ReadingImageDeliveryServiceIT.sdkDeadlineRangeRejectsAboveIntBeforeAnyIo`, `F4 Timer Duration dönüşümünü aşan pozitif süre admission alamaz`, `F4 büyük server lease değeri Timer öncesi ve byte çağrısından önce reddedilir`: teknik int32 milisaniye üst sınırı Firebase/byte/Timer öncesinde reddedilir. |
| F5 | `encodedHandlerUsesSameGateAndSlowOpenSocketTimesOutWithPayloadCleanup`, `slowActualHttpBodyKeepsGateUntilDisconnect`, `unsupportedContainerRefusesBeforeServiceIo`: açık ve tüketmeyen gerçek socket deadline sonrası payload/permit cleanup'ını ve ikinci teslimatı kanıtlar; desteklenmeyen container I/O öncesi reddedilir. |
| F6 | `F2 F6 F7 F8 F9 gerçek PNG loaded source rendition resize ve klonsuz native widget sahipliği`: borrowing painter ek ui.Image clone'u tutmaz; eski widget sonraki frame'e geçmeden eski native handle kapanır ve yeni byte/decode işi başlar. Resize ve close da sınanır. |
| F7 | Aynı gerçek PNG testi lease/error sonrası `entryIndex=3`, `canonicalSource=false` seçimini koruyup aynı görsel/kol ile yeniden admission kurar. |
| F8 | Aynı gerçek PNG testi farklı contentId için hata verir ve byte çağrısı sayısının artmadığını doğrular. |
| F9 | Aynı gerçek PNG testi Cubit/view loaded yolunda canonical görüntünün gerçek `4×4`, rendition seçiminin `1×1`, resize sonrasının `2×2` olduğunu doğrular. |
| F10 | `ReadingImageComparisonServiceIT.postIoRevokedBearerReturns401WithoutImageOrWithdrawalThroughActualServiceController`: gerçek service/controller/pinned storage yolunda ikinci verifier revoke olur; 401 + ProblemDetail + private,no-store döner, image body gönderilmez, sağlıklı LIVE korunur. |
| F11 | `ReadingImageComparisonServiceIT.successfulAndFailedPinnedDeliverySpendBudgetBeforeStorageAndAcrossServiceRestart`: gerçek teslimat başarılı ve 503 ile başarısız pinned okuma rezervasyonlarını harcar; yeniden oluşturulan servis ikinci teslimatı storage'a dokunmadan reddeder. |
| F12 | `ReadingImageComparisonTestControllerIT.generatedMobileOpenApiContainsTypedComparisonContract`: üretilen `/v3/api-docs/mobile` manifest/status Admission, PNG binary ve önemli hataların ProblemDetail sözleşmesini içerir. Yalnız yeni QA yolu mobile grubuna eklendi. |
| F13 | `F13 gerçek gzip response reddedilir; identity payload için otomatik açma kapalıdır`, `gerçek HTTP payload SHA eşleşir; yanlış SHA başarı sayılmaz`: autoUncompress kapalı, Accept-Encoding identity; gzip reddedilir, normal payload SHA ve byte sayısı doğrulanır. Sayaç TLS/header byte'ı iddia etmez. |
| F14 | `ReadingImageComparisonServiceIT.freeChangeDuringSuccessfulSecondBearerRequiresFinalDbCut`: başarılı ikinci bearer I/O'su sırasında free erişim kapanır; ardından DB-only son kesit 403 döndürür, image body çıkmaz ve sağlıklı availability korunur. |

F5 için yalnız standart Servlet async timeout denemesi, `async.complete()` sonrasında container flush
beklerken süre denetimini sürdüremedi; başarısız ara log `/tmp/p4a2-repair-targeted2.log` başarı kanıtı
değildir. Son çözüm local/test embedded Tomcat 11 adapter'ının dar Coyote `CLOSE_NOW` capability'sini
kullanır. Ayrı deadline task gerçek `onComplete`/nihai `onError` cleanup'ına kadar yaşar; payload temizlenir,
permit son cleanup'ta bırakılır. Hedefli son test `/tmp/p4a2-repair-targeted-final.log` içinde 22 yeni IT
(7 controller, 11 content, 4 asset) ile geçti. Bu, pinned SDK okuması veya auth çağrısının mutlak ve
anında iptal yeterliliğini kapatmaz. Desteklenen runtime ve fail-closed sınırı ADR-0024'te kayıtlıdır.

Native testler owned buffer/descriptor/codec/ui.Image sayaçlarını ve debug open handle'ları doğrular.
Engine raster-thread/display-list/GPU/RSS toplamının sıfırlandığı veya fiziksel tepe belleğin bütçeye
uyduğu iddia edilmez. Fiziksel cihaz profiling, gerçek HTTP cihaz aktarımı ve kalite kabulü açıktır.

Son generator `/tmp/p4a2-repair-generator.log` ile başarılıdır. Son analyzer
`/tmp/p4a2-repair-analyze-final.log` içinde `No issues found!` döndürdü. Son tam Flutter
`/tmp/p4a2-repair-flutter.log` içinde **412 test** ile geçti.

Son tam backend komutu `be/` içinde
`DOCKER_HOST=unix:///Users/gurhankucuk/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock bash mvnw -q verify`
olarak çalıştı. `/tmp/p4a2-repair-verify.log` **exit 0** ile bitti. Güncel XML toplamları **470 Surefire +
23 Failsafe**, sıfır failure/error/skip'tir. Failsafe kapsamı 22 karşılaştırma testi ve bir module
dokümantasyon testidir. PostgreSQL 15.19 Testcontainers kullanıldı; mevcut yerel DB'ye migration
uygulanmadı. `git diff --check` de geçti. Generator kaynaklı ilgisiz story_detail farkı bırakılmadı.
Ana oturum bağımsız inceleme kapanışını ve son dosya temizliğini yönetir.

### Android emülatör geometri doğrulaması ve dar onarım — 2026-10-17

`MAC_EK` üzerindeki SDK ve AVD bulundu. Fiziksel disk 480,1 GB olsa da `MAC_EK` bölümü
118,5 GB ve doludur; yaklaşık 360,1 GB bölüm dışı alan bulunur. [Alan incelemesi](mac-ek-space-2026-10-17.md)
salt okunur sonuçları taşır; silme veya bölüm değişikliği yapılmadı.

İç diskte yeni geçici AVD açıldı. Ağsız Flutter ekranında gerçek `RenderClipRect` ölçümü ve
kapak/iki sayfa için altı ekran kaydı alındı. Kol düğmesinin değişen metni sayfa slotunu
48 logical pixel değiştiriyordu. Metin alanı sabitlendi; iki kol kapakta
`411,428571×457,142857`, sayfalarda `411,428571×694,285714`, DPR `2.625` oldu.
Önce/sonra ham kayıtlar, hot reload kaynak hash'i, ilk test başarısızlığı ve sınırlar
[emülatör kanıtında](emulator-geometry-2026-10-17.md) açıklanır.

Yeni regresyonlar dahil son seri Flutter suite **414 test** ile geçti; analiz temizdir.
İlk paralel suite'te değişmeyen bir Library testi `loaded/loading` başarısızlığı verdi;
aynı dosyanın 8 testi ayrıca geçti. Bu başarısızlık kayıttan çıkarılmadı. Bağımsız blind review
yeni bulgu vermedi. Emülatör sonlandırıldı; yalnız onun geçici oturumu kapandı.

Bu, çalışan emülatör geometri kanıtıdır; fiziksel cihaz, gerçek medya/byte/decode/bellek veya
kalite kabulü değildir. Sayısal çalışma ve bulut işlemi onaylanmadı/başlatılmadı. P4b ve P0 ham
kanıt sınırlamaları sürer. Normal tüketiciler, backend/frontend, TR71, legacy #112 ve `.memlog.md` korundu.
