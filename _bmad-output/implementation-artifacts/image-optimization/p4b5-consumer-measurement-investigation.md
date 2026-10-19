# P4b5: Tek sınırlı emülatör ölçümü — inceleme ve yürütme sınırı

Tarih: 2026-10-19. Başlangıç HEAD: `d80884da6c0fcc5b252278924b0dd0b2a2545e27`.
Branch: `codex/image-optimization-p3-publication`. P4b5 Epic story değildir. Bu kayıt
salt kaynak ve önceki kanıt incelemesidir; yeni test/build/DB/SDK/auth/ADB veya gerçek
koşu yapılmadı. İki inceleme ajanı modeli oturumdan devraldı; override yoktur.
Mevcut dirty/untracked işleri koruyarak aynı branch'te devam etme yetkisi sürer.

## Tek hedef ve sayısal taslak

Amaç P4b1–4 kodunu mevcut #94 retained LIVE ile gerçek QA emülatör zincirinde
ölçmektir. Yeni encoding, original karşılaştırması, kalite veya üretim profil seçimi
yoktur. #94'ün mevcut test PNG'lerini kullanmak sıkıştırma kazancını kanıtlamaz.

[Sayısal öneri](../../../be/docs/evidence/p4b5-consumer-measurement/bounded-run-proposal-2026-10-19.json)
SHA-256: `e49eb8b706143f09c4fbf1738e2254f54e491d104954baf31e546a4497ec5833`.
Öneri henüz onaylı değildir. Değişirse hash ve yeni scope bağları yeniden değerlendirilir.

| Kalem | Önerilen tek deneme |
|---|---:|
| Pencere | 120 saniye; yerel ve credential kapılarından sonra bir kez |
| Manifest / status / medya | 1 / en çok 12 / 4 |
| Mobil HTTP / payload | En çok 17 / 13.170.884 byte |
| Login / lookup / exchange / login verifier | Her biri en çok 1 |
| Login dahil verifier | En çok 22 |
| Yeni delivery rezervasyonu | En çok 25 işlem / 13.170.884 byte |
| QA kurulum / ilk başlatma / backend başlatma | Her biri en çok 1 |

SDK/paket/Java kurulumu veya indirmesi yoktur. Yerel kod/test/build hatası yeni gerçek
başlatma hakkı tüketmez; düzeltildikten sonra yerel kontroller tekrarlanabilir. Gerçek
başlatma/login/pencere hakkı çağrı öncesi rezerve edilir; başarısızlık sonrası yenilenmez.
Dört başarılı aktarım garanti değildir. Süre/kota/kimlik/kalite kapısı başarısızsa
terminal hata ve cleanup kanıtı üretilir; otomatik tekrar veya original fallback olmaz.

## Ölçüm kimliği ve eski borç

Yeni `measurementId`: `p4b5-7981ad25-c851-4ed6-af89-2d0fbfffd0f8`.
Kalıcı `budgetRunId`: `p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09`.
Storage prefix de mevcut `local/p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09` kalır.
Üç kavram birbirine eşitlenmez. Yeni ölçüm kimliği yeni bir V37 satırı, eski pencerede
yeniden giriş veya eski borcu sıfırlama hakkı değildir.

Eski gerçek P4a5 sonu V37 `39/64` işlem, `44.742.970/67.108.864` byte'tır. Yeni deneme
aynı DB ve aynı satırı kullanır; genel atomik tavan artmaz. Yeni ledger ayrıca bu
denemenin artışını 25 işlemle sınırlar. En kötü son değer `64` işlem ve `57.913.854`
byte olur. İşlem hesabı `1 manifest + 12 status + 4×(2 auth + 1 storage) = 25`.
Verifier hesabı login dahil `1+1+12+4×2=22` olur. Login toplamı yeni açık onayla en çok
4 olur; bu, üretim erişimi değildir. Diğer eski toplam/tavanlar öneri JSON'undadır.

Başlangıçta aynı yerel DB adı, açık `gurhankucuk` kullanıcısı, V37 ve tam eski borç
zorunludur. Farklı/eksik satır, başka DB, yeni run satırı, restore/clone/reset veya
tavan değişimi reddedilir. Borç aynı satırda artar; eski P4a5 ledger/ready/approval
ve kaynak dosyaları değişmez. Aynı bütçeyi tüketen başka bir iş gözlenirse durulur;
hiçbir ilgisiz süreç durdurulmaz. V37 her durumda genel tavanı atomik korur; koşu
sonucu yalnız bu denemenin ledger/DB artışı uzlaşıyorsa kabul edilir.

Aynı DB adı/sayıları fiziksel klon kimliğini kriptografik kanıtlamaz. Gerçek DB'yi
klonlama/geri sarma ve özel claim'i silme/taşıma kapsam dışıdır. Yeni özel kökteki
CREATE_NEW startup/install/start/login/window claim'leri aynı ölçümün tekrarını
DB geri sarılsa bile kapatır. Claim/kanıt kaybında aynı ölçüm yeniden açılmaz.

## Kaynak haritası ve en küçük değişiklik

Satırlar başlangıç HEAD'ine aittir; uygulamada sembolle doğrulanır.

| Yol / sembol | Yeniden kullanım ve sınır |
|---|---|
| `be/src/main/java/com/tellpal/v2/asset/application/reading/ReadingComparisonBudget.java:17–28` | Aynı DB/run, eşit tavan ve atomik artış. Salt okunur; yeni schema/ürün kodu yok. |
| `be/src/main/java/com/tellpal/v2/content/application/publication/ReadingImageComparisonProperties.java:15–30` | Budget run retained nesne prefix'inden bağımsızdır. Mevcut profile sayısal bellek/byte sınırları korunur. |
| `be/src/test/java/com/tellpal/v2/support/P4ConsumerQaSetup.java:28–39,52–68,143–164,220–226` | Eski recovery/onay ve ön borç 25'e sabit. Eski dosya değişmez ve gerçek komutu çağrılmaz. |
| Aynı dizinde `P4ConsumerQaLedger.java:17–56,63–141`, `P4ConsumerQaStorage.java:15–23` | Typed wrapper, sıralı fsync ledger ve dört pinned read örüntüsü. Sabit eski onaylar yeni yetki olamaz. |
| `tools/image-optimization/p4_consumer_qa.py:230–377,556–561,653–689` | Scope'tan bağımsız Android/PID/startTicks, VM tap, mapping ve cleanup yardımcıları ile geometri oracle'ı yeniden kullanılabilir. Eski yürütücü/varsayılan/approval/recovery dalı değişmez; global sabit monkeypatch'i yapılmaz. |
| `app/mobile/test_driver/reading_consumer_qa_probe.dart:19–64,109–121,327–367` | Eski parser varsayılanı 180sn/18status olarak kalır. Yalnız compile-bound P4b5 beklenen scope eklenir; scope runtime mesajından öğrenilmez. |
| `app/mobile/test_driver/reading_consumer_qa_entry.dart` | Eski entry korunur. Yeni küçük `reading_p4b5_qa_entry.dart` debug-only scope'u açık seçer. |
| `tools/image-optimization/p4_resource_observation.py:59–112` | Yeni controller gerçek ham örnek dizisini bu saf kabulden geçirir. Eski legacy kabulü yeni ölçüm başarısı sayılamaz. Dosya salt okunur kalabilir. |
| `app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart:285–301,396–404` | Credential/lease minimumu ve gerçek status+medya drain'i korunur. Runtime/selector/raster/ses/cache değişmez. |

Yeni backend harness yalnız `be/src/test/java/com/tellpal/v2/support/` altında
`P4b5ConsumerQaSetup.java`, `P4b5ConsumerQaLedger.java`, `P4b5ConsumerQaStorage.java`
ve bunların setup/ledger/composition testleri olur. Test-only wiring mevcut Spring
kompozisyonu, Firebase verifier ve SDK adapter'ını sarar. Yeni storage process/IPC,
retry altyapısı veya üretim endpoint'i eklenmez. Paket sınırı ürün kodunda değişmez.

Yeni `tools/image-optimization/p4b5_consumer_qa.py` scope-bound aşamaları ve P4b5
kaynak/phase kabulünü sahiplenir. Yalnız tekrar kullanılabilir mevcut helper'ları
çağırır; eski `plan/run/login/prepare` veya recovery yetkisini çağırmaz. Tüm controller'ı
kopyalamak veya eski sabitleri yerinde değiştirmek yerine bu dar varyant kullanılır.
Varsayılan komut gerçek dosya/credential/cihaz/DB/SDK yan etkisi açmaz; execution açık
onay, exact proposal hash'i ve tek kullanımlık claim ister.

Dart probe'a dar beklenen-scope parametresi eklenir; eski varsayılan davranış ve
eski testler korunur. Yeni entry beklenen measurementId/proposal/window/status
bağını compile-time seçer. Yeni runtime mesajı bu bağların tümünü taşımalıdır;
keyfi config, eski proposal veya runtime'dan öğrenilen hash reddedilir.
Observed source'un HTTP sınırı da aynı scope'un 12 status tavanına bağlıdır.
Normal app/router ve comparison entry değişmez. Public DI/domain API eklenmez.

## Koşudan önceki sıralı kapılar

1. İzinli gerekli Flutter/Android/backend/tools kaynak/test/fixture kapanışı özel
   kopyaya alınır. SDK, Gradle/Maven/pub cache, Java ve interpreter yeniden kullanılır.
   Paket/SDK indirilmez. Son kaynak manifesti test, backend classpath ve APK hash'ine bağlanır.
2. Yerel Python/Dart/Java testleri ve analyzer geçer. Gerçek loopback/native JSON
   zinciri yeni phase kabulüne ulaşır. Gerçek PostgreSQL/V37 ve mevcut HTTP/modül
   regresyonu da koşar; yeni modül bağımlılığı veya migration yoktur. Yalnız mock ve
   derleme yeterli değildir. İzole sentetik test DB'si gerçek QA DB'sinden ayrıdır.
3. Kök ajan üç bağımsız koşu-öncesi güvenlik incelemesi yürütür. Hazırlık ajanı bu
   noktaya kadar gerçek başlangıç/auth/cihaz çalışması yapmaz. Bulgular kapatılıp
   kaynak/hash yeniden eşleşmeden gerçek işlemler açılmaz. Bu operasyon kapısı,
   gerçek kanıtı da inceleyen son BMad step-04 incelemelerinin yerine geçmez.
4. Mevcut `emulator-5554` ve mevcut 822×1828 /320dpi /fontScale1 düzeni salt okunur
   doğrulanır. Yeni emulator başlatılmaz, geometri override değiştirilmez. QA süreci
   zaten canlıysa ona dokunulmaz; koşu açılmaz. Mevcut SDK/bellek/disk ve açık DB
   kullanıcısıyla ayrı salt okunur JDBC kapısı, Spring/SDK başlamadan doğrulanır.
5. Yeni private root'ta claim/reservation alınarak en çok bir APK kurulumu ve ilk
   QA başlangıcı yapılır. VM main isolate/PID/startTicks, driver, source→APK ve boş
   snapshot kapısı geçer. Configure öncesi kaynak baseline'ı `B` kaydedilir; `B=0`
   zorlanmaz. Hiçbir login/medya bu kapıdan önce başlamaz.
6. Claim'i Spring/SDK'den önce alan tek backend, aynı V37 DB'de eski borcu ve exact
   #94 retained proof'u doğrular. Local/test profil, 1GiB heap, aynı Java25 binary,
   açık DB hesabı ve gerçek prefix zorunludur. Worker/poller/reconcile kapalıdır.
   Backend portu dinamik loopback'tir; mevcut backend/frontend süreçlerine dokunulmaz.
7. Local-ready ve QA/VM/PID/DB kapıları hâlâ geçerliyken en çok bir login yapılır.
   Lookup/exchange/verifier rezervasyonu credential dosyası/worker'dan önce fsync
   edilir. Mevcut Firebase SDK, kanonik UID ve mevcut config yolları kullanılır;
   credential veya web API key repo/log/mesaja yazılmaz. Yeni Firebase config indirme
   veya izin/rol değişimi kapsamda değildir. Eksik config varsa gerçek giriş açılmaz.
8. Login hazır olduktan sonra yerel en çok 5 dakikalık kapı içinde tek 120sn pencere
   açılır. Backend wall/monotonic rollback, exact onay ve deadline guard'ları korunur.
   Backend lease180sn üst sınırdır; mobil credential/control deadline120sn ile daha
   erken kapanır. Byte/piksel/bellek/transfer/decode profilleri aynı kalır.
9. Gerçek UI butonlarıyla detail→sayfa1→sayfa2→detail sürülür. Yeni üretim veya source
   kolu yoktur. Kullanıcı ölçüm sırasında ekrana dokunmaması konusunda bilgilendirilir.
   Beklenmeyen insan/UI hareketi veya kimlik değişimi terminal ret üretir; otomatik tekrar yoktur.

## Mobil kanıtın kabulü

Sıralı ham örnekler: configure öncesi baseline; dört gerçek paint; background öncesi,
background, aynı süreçle resume, UI close ve awaited stop. Tek VM isolate/PID/startTicks
ve tek `lifetimeId` gerekir. Baseline ve sonrasındaki sayılar sıfırlanmaz.

Her paint mevcut geometri/rol/page/rendition/revision/manifest/source/SHA/payload ve
aynı-epoch frame kontrolünden geçer. Ayrıca P4b4 kaynak kabulü `v1/measured=true`
olmalıdır. Image1, diğer active handle'lar0, slot1 ve byte `decodedWidth×decodedHeight×4`
olur. Attempts farkları `1,2,3,4`; sonraki tüm örneklerde `4` kalır. Beşinci native
girişim, eksik/legacy/karma kaynak şeması veya farklı yaşam ölçümü geçersiz kılar.

Background, resume, closedUi ve stop'ta **phase tarafından açık drain** gerekir:
dört active, slot ve byte0. `stopped=false` olması bu kontrolü atlatamaz. Önceden
başlamış isteğin completion kaydı yeni istek değildir; background sonrası yeni
HTTP rezervasyonu/medya/decode yokluğu ayrı kontrol edilir. Awaited Cubit close
P4b1 status/medya sonlanmasını bekler. Background kaynak sıfırı tek başına bütün
status HTTP Future'larının bittiği iddiası değildir.

Yeni caller `ok/configured/stopped/cubitClosed/credentialActive/deadlineActive` ve
varsa `drained` alanlarında kesin bool ister. Eksik, null, 0/1, string veya liste
truthiness ile kabul edilmez. HTTP olayının `ok` değeri de kesin bool olur. Baseline'da
configure yok; loaded'da configured; closedUi'de Cubit kapalı; awaited stop'ta
credential/deadline kapalı olmalıdır. Ham örneğe sonradan `drained=true` eklenmez.
Phase sonucu ayrı raporda tutulur; raw JSON hash'i korunur.

Dört paint ve kapanış ekranı ile gerçek aktarım sayıları saklanır. PSS/RSS alınabilirse
ayrı debug süreç snapshot'ı olarak etiketlenir; rezervasyonlarla toplanmaz. Sampler
maksimumu süreç/engine/GPU peak'i değildir. Ayrı normal Library veya ses doğrulaması
bu koşuya katılmaz; normal akışlar değişmez.

## Cleanup, hata ve tekrar yasağı

Başarılı veya başarısız denemede VM, yalnız owned PID/startTicks ile doğrulanmış QA
süreci/backend ve bu denemenin eklediği forward/reverse eşlemeleri kapatılır.
Pre-existing mapping, emulator/ADB server, başka QA/backend/CMS süreçleri korunur.
UI close ve probe stop drain'i ayrı kanıttır. Force-stop temizliği yalnız pozitif
owned kimlikle en çok bir kez yapılır; PID replacement veya belirsizlikte durulur.
Backend drain makbuzu PID/proposal/measurement ve süreç çıkışıyla eşleşmelidir.

Terminal ledger, aynı V37 satırının yeni artışı ve immutable önceki borç uzlaşır.
Tüm giriş/okuma/byte rezervasyonları hata halinde tüketilmiş kalır. Saklanan byte
ile gerçekten aktarılan byte ayrı kaydedilir. Ham failure/timeout/ret logları korunur.
DB/key/config/cihaz/driver/izin sorunu yeni başlangıç veya login hakkı üretmez.
Tek gerçek deneme başarısızsa rapor bunu söyler; P4b5 mobil kabulü tamamlandı denmez.
Düzeltme incelemesi sürdürülebilir, gerçek tekrar için yeni somut onay gerekir.

## Test ve koruma kapsamı

Yeni Java setup/ledger/composition testleri: exact onay, seed39 borcu, aynı atomik
64 tavanı, ek25 quota, pencere/clock rollback, replay, auth/read sırası, integrity ve
storage write/observe reddi. Gerçek PostgreSQL/V37 testinde rezervasyon restart ile
kalır; kaynak/DB/claim drift ve başarısız I/O tüketimi sınanır. Maven için kurulu
`run-maven-tests` skill'i uygulanır; mevcut dependency cache'i kullanılır.

Yeni Dart/gerçek loopback testleri: eski parser/default bozulmadan yeni compile-bound
scope; yanlış measurement/proposal/pencere; typed flag ve phase drain. Python testleri
`7→8,9,10,11→11`, beşinci decode, lifetime/reset, legacy, loaded uyumsuzluk ve
`stopped=false` drain reddi; controller'ın gerçek JSON→P4b4 rapor çağrısı. Eski93
regresyon ve P4b4 Python testleri korunur. Yerel mutasyonlar en az scope/attempt,
extra-decode, phase-drain/typed flag ve backend quota guard'larını assertion'dan
bozsun. Restore/hash ve izlenen tüm Flutter test/analyzer sonucu kaydedilir.
Son gerçek koşu kanıtı ayrıca üç bağımsız BMad incelemesinden geçer; yalnız görev
kapsamı yerel commit'e girer. Push/PR yoktur.

Onay gerekli credential'sız Flutter/Android/backend/tools kaynak/test/fixture/paket
kapanışının ve yeni QA çıktılarının şu kökte tutulmasını kapsar:
`/Volumes/MAC_EK/tellpal-qa-private/p4b5-consumer-measurement-20261019-d80884d/`.
Dizinler0700, yeni log/makbuz0600 ve CREATE_NEW/sıralı olur. SDK/cache/keystore veya
gerçek servis config'i kaynak kopyasına alınmaz; gerekli runtime credential yalnız
kendi private dosyalarında kullanılır. Token/web API key/VM URL'leri repo'ya girmez.
Dirty `main_image_experiment.dart` kopyada HEAD istisnası olabilir; repo korunur.
Untracked local-lossless işleri kapsam dışıdır. Bütün dirty/untracked ağaç test edildiği
söylenmez. Android kaynak/import ve backend classpath kapanışı ayrıca hash'lenir.

Başlangıçta repo, eski `be/var`, P4a5/P4b1/P4b2/P4b3/P4b4 özel kaynak/kanıt dosyaları
ve frozen block'lar mühürlenir. `.memlog.md`, dirty giriş ve eski hata/approval/token
kayıtları korunur. Onaydan sonra bu companion ve öneri JSON'u salt okunur kalır;
çalışma sonucu ayrı verification dosyasında üretilir.

## Açık ürün kapıları

EXT-MEDIA/EXT-PUBLICATION, korumalı erişim, URL yenileme, HTTP cache/session/retention
ve sayısal üretim profili kararları açıktır. Genel P4b ve normal tüketici geçişi bu
koşuyla kapanmaz. Fiziksel Android/iPhone, profile/release, kalite/RSS/GPU/peak kabulü,
P0 ham kanıt kısıtı ve JPEG/WebP bulanıklığı ayrı kalır. Yeni generation/upload/observe,
yayın/backfill/silme/Flyway/rol/dağıtım ve fatura sorgusu yoktur. Gözlenen authoritative
integrity hatasında mevcut #94 availability withdrawal kuralı dışında sağlıklı LIVE'a
ve kaynaklara dokunulmaz. V37 rezervasyon yazımı bu koşunun zorunlu DB yan etkisidir.
