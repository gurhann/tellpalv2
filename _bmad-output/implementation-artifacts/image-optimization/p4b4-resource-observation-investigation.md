# P4b4: Kaynak gözlemini ölçüm JSON'una taşıma

Tarih: 2026-10-18. Başlangıç HEAD: `52098d94f89c9743710d213c43bdcf318486108b`.
Branch: `codex/image-optimization-p3-publication`. Bu kayıt salt kaynak incelemesidir;
yeni test, build veya gerçek ortam çalışması yapılmadı. P4b4, Epic story değildir.
BMad ve iki inceleme ajanı modeli oturumdan devraldı; override kullanılmadı.
Mevcut dirty/untracked işleri koruyarak devam etme yetkisi sürer.

## Kanıtlanan boşluk ve dar seçim

P4b2 native buffer başlamadan kapasite ve tahmini decoded byte ayırır. Buna rağmen
iki QA probe'u yalnız `activeBuffers/activeDescriptors/activeCodecs/activeImages`
aktarır. Buffer'ın ilk await'i sırasında bu dört değer sıfırken rezervasyon dolu
olabilir. Mevcut JSON, rezervasyonun varlığını veya gerçek cleanup sonrası
bırakıldığını tek başına kanıtlayamaz. P4b3 bu ölçüm boşluğunu değiştirmedi.

İki probe'un mevcut `ownedResources()` yayılımını kullan. Küçük, yalnız test_driver
kapsamında ortak bir gözlem helper'ı aynı alan adlarını, sürümü ve sayaç yaşamını
üretsin. Bu helper read-only getter'ları okur; native işi veya sayaçları yönetmez.
Yeni Python modülü yalnız kendisine verilen JSON değerlerini doğrular ve rapor
nesnesi döndürür. Eski yürütücüler, `validate_loaded`, VM/ADB sürüşü ve onay zinciri
aynı kalır. Yeni CLI, servis veya genel telemetry katmanı gerekmez.

## Kaynak haritası

Satırlar başlangıç HEAD'ine aittir; uygulamada sembolle doğrulanır.

| Yol / sembol | İnceleme ve yeniden kullanım |
|---|---|
| `app/mobile/lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart:17–21,53–73,114–135` | `reservedSlots`, `reservedDecodedBytes`, `bufferCreationAttempts`; rezervasyon ilk native await'ten önce alınır, gerçek cleanup/dispose ile bırakılır. Runtime salt okunur. |
| `app/mobile/test_driver/reading_consumer_qa_probe.dart:277–284,329–335,359–371` | State olayları ve snapshot `ownedResources()` map'ini yayar. Paint de snapshot döndürür. Yeni gözlem bu yolla gider. |
| `app/mobile/test_driver/reading_comparison_probe.dart:217–223,266–272,289–300` | İkinci probe aynı kaynak haritasını kullanır. Dosya adı `reading_comparison_probe.dart`tır. |
| `app/mobile/test/reading_images/reading_comparison_probe_test.dart:254–378` | Gerçek loopback/codec, snapshot JSON ve sır sızmaması kapsamı. `jwt/png/RealLocalHttp` consumer testinde de kullanılır. |
| `app/mobile/test/reading_images/reading_consumer_qa_probe_test.dart:177–310` | Gerçek router/bozuk loopback payload temizliği; native paint ve eski epoch reddi. |
| `app/mobile/test/reading_images/reading_image_comparison_raster_test.dart:76–156` | Gerçek native pending/cancel, ikinci owner reddi, resident ve double-dispose gözlemleri. Mevcut test düzeni yeniden kullanılabilir; bu dosya salt okunur kalır. |
| `app/mobile/test/reading_images/reading_image_consumer_qa_test.dart:279–456` | P4b3 gerçek reader/status ve P4b2 farklı gate/iki Cubit/native regresyonları. Davranış değişmez; mevcut testler çalışır. |
| `tools/image-optimization/p4_consumer_qa.py:653–689,857–879` | Eski geometri/paint/HTTP/active handle doğrulaması; rezervasyon bilgisi yok. Run sabit tüketilmiş P4a5 onayına bağlıdır. Salt okunur. |
| `tools/image-optimization/p4_reading_comparison.py:228–242,265` | Eski comparison VM/isolate ve loaded kontrolü. Yeni kaynak ölçümü iddiası eklenmez; salt okunur. |
| `tools/image-optimization/test_p4_consumer_qa.py:33–102` | Eski şemasız snapshot ve geometri ret örnekleri; mevcut kabulün regresyon kontrolü. Salt okunur. |

## Gözlem sözleşmesi

Her iki probe'un state, snapshot, paint ve stop çıktısına mevcut map yayılımıyla
aynı `resourceObservation` bloğu eklenir. Mevcut üst düzey `active*` alanları korunur.
Bloğun zorunlu alanları:

- `version`: kesin integer `1`.
- `lifetimeId`: helper'ın aynı Dart isolate yaşamında sabit tuttuğu, sır veya URL
  içermeyen opak kimlik. Yeni probe/run için sayaç sıfırlanmaz veya kimlik yenilenmez.
  Yeni isolate yaşamı yeni kimlik alır. Basit isolate-local token yeterlidir; yeni
  oturum/DI/IPC altyapısı değildir. Kimlik kullanıcı grant'i veya gerçek VM doğrulaması değildir.
- `reservedSlots`: kesin integer `0` veya `1`.
- `reservedDecodedBytes`: kanonik negatif olmayan onluk string; boşta `"0"`.
- `bufferCreationAttempts`: kanonik negatif olmayan onluk string. Native `int`
  değeri kesirli JSON sayılarına dönüştürülmez; byte ve artan sayaç kayıpsız aktarılır.

Kanonik onluk gösterim `0` veya başında sıfır bulunmayan rakam dizisidir. İşaret,
boşluk, üs, kesir, bool ve null kabul edilmez. Python keyfi hassasiyetli tamsayı
kullanabilir; float, clamp veya varsayılan `0` dönüşümü yapmaz. Girdinin yapısal
uygunluğu ve teknik temsil sınırı denetlenir; üretim bellek bütçesi seçilmez.
Gözlem boyutu mevcut sonlu probe mesaj/event kuralları içinde kalır.

Saf Python doğrulayıcıya verilen örnekler için:

- Yeni blok **yoksa** sonuç `legacy`, `measured=false`; yeni alan/delta uydurulmaz.
  Bu, eski kabul sonucunu yeniden değerlendirmez. Blok var fakat null/eksik/bilinmeyen
  sürümse ret olur; legacy'ye düşürülmez. Legacy ile v1 karışık dizi yeni kanıt olmaz.
- V1 dizi tek `lifetimeId` taşır. İlk attempts değeri baseline'dır; sıfır varsayılmaz.
  Sonraki değerler azalamaz. Fark yalnız bu tek yaşam içinde hesaplanır. Yaşam
  değişince aynı dizi reddedilir; başka dizi bağımsız baseline ile başlayabilir.
- Slot `0` ise tahmini byte `0`; slot `1` ise byte pozitif olmalıdır. Mevcut `active*`
  alanları da kesin negatif olmayan integer olmalıdır. Kararlı loaded örneğinde
  image `1`, diğer active handle'lar `0`, slot `1` ve tahmini byte doğrulanmış decode
  genişlik × yükseklik × `4` ile eşleşir. Boyutlar kesin pozitif integer olmalıdır.
- Pending rezervasyon örneğinde image `0`, slot `1` geçerlidir. Her `loading` durumu
  rezervasyon alınmış demek değildir; transfer aşamasındaki slot `0` yasaklanmaz.
  Bu ayrım testte örneğin alındığı gerçek native aşamayla kurulur.
- Tam drain/stop iddiasında dört active değer ve slot/byte `0` olmalıdır; yalnız
  görünür image'ın kalkması yeterli değildir. Artan attempts sıfıra dönmez.

Python yalnız kaynak gözlemini doğrular. Eksik geometry/paint/auth/manifest kanıtını
bununla tamamlamaz. Örneklerin maksimumu yalnız gözlenen rezervasyon maksimumudur;
örnekler arası peak, süreç RSS, engine/GPU veya fiziksel kaynak sınırı değildir.

## Yerel doğrulama ve kabul

Yeni Dart testleri gerçek `ReadingComparisonRaster.decode` ve getter'lardan iki
probe'un `ownedResources/snapshot/paint/stop` JSON'unu sınasın. Yeni helper'ın tek
başına elle değer alan testleri yeterli değildir. İlk await öncesi pending slot/byte,
iptal bildirimi sonrası doluluk, gerçek sonlanmada sıfır; resident image ve ikinci
ret sırasında artmayan native sayaç kanıtlansın. Önceden native iş yapılarak baseline'ın
sıfır olmadığı durum da kapsansın; sayaç reset API'si eklenmesin.

Mevcut gerçek loopback → repository → Cubit → probe → JSON zinciri korunup en az bir
başarılı native çıktı ve cleanup gözlemi yeni alanlarla doğrulansın. Bu Dart testinin
gerçek JSON çıktısı özel kanıtta saklanıp yeni Python saf doğrulayıcısına verilerek
rapor nesnesine kadar geçsin. Yalnız iki dilde ayrı hazırlanmış fixture yeterli değildir.
JSON üretim/ayrıştırma ve rapor nesnesi yereldir; yeni canlı protokol veya IPC değildir.

Python testleri legacy, null/eksik blok, sürüm, bool/kesir/negatif/boşluk/üs/baştaki
sıfır, büyük kayıpsız sayılar, tutarsız slot-byte, loaded geometri, eksik drain,
sıfır olmayan baseline, azalan attempts ve farklı lifetime matrisiyle çalışsın.
Mutasyonlar özel kopyada en az şu iddiaları assertion'dan düşürsün: gerçek getter
bağlantısı yerine sıfır; legacy/missing ölçümü sıfırla doldurma; lifetime/artan sıra
korumasını kaldırma; slot rezervasyonunu drain'de yok sayma. Derleme hatası kanıt değildir.
Her denemeden sonra kaynak hash ile geri alınır; ilk başarısız denemeler korunur.

Mevcut SDK/cache ile focused Flutter testleri, izlenen tam Flutter kümesi ve analyzer;
yeni Python testleri ve eski iki yürütücünün ilgili yerel testleri geçsin. Yeni test
adları ham çıktıda görünmeli; dar/tam/tekrar sayıları toplanmamalıdır. Kaynak ve
relative import kapanışı, korunan dosyalar, frozen block ve diff-check doğrulansın.
Üç bağımsız BMad incelemesi ve görevle sınırlı yerel commit yapılır; push/PR yoktur.

## Özel kaynak/test kopyası ve koruma

Plan onayı, gerekli izlenen kaynak/test/fixture/paket varlıklarının credential'sız
özel kopyasını ve yerel doğrulamayı da kapsar:
`/Volumes/MAC_EK/tellpal-qa-private/p4b4-resource-observation-20261018-52098d9/`.
MAC_EK'te yeni dizinler `0700`, log/makbuzlar `0600`, CREATE_NEW ve sıralı olur.
Yalnız gereken kapanış alınır. SDK/cache ve mevcut sentetik TLS interpreter'ı
kullanılır; kurulum/indirme yapılmaz. Kimlik bilgisi, token, `.env` ve gerçek servis
config'i kopyalanmaz. Dirty `main_image_experiment.dart` için private kopyada HEAD
istisnası açıkça kaydedilebilir; repo dosyası değişmez. Untracked local-lossless
kapsam dışıdır. Bütün dirty/untracked ağacın test edildiği söylenmez.

Uygulama öncesi repo, eski `be/var`, P4a5/P4b1/P4b2/P4b3 özel kaynak/kanıt dosyaları
ve frozen hash'ler mühürlenir. `.memlog.md`, dirty giriş, local-lossless işleri ve
eski başarılı/başarısız kayıtlar korunur. Bu companion onaydan sonra salt okunur kalır.
Çalışan süreçlere dokunulmaz. Eski kaynak kopyaları yalnız okunur; yeni çalışma yalnız
yeni private kökte yapılır. Eski test sonucu P4b4 sonucu olarak devralınmaz.

## Açık kapılar

Eski P4a5 run/plan hash'leri ve sabitleri değiştirilmez. Eski deneme onayı tüketilmiştir;
yeni giriş/pencere/backend/DB/Firebase/storage/ADB/APK/cihaz hakkı vermez. Bu görev
hiçbirini çalıştırmaz. Yeni üretim/upload, deploy/backfill/silme ve fatura sorgusu yoktur.
Normal Library/detail/reader, ses/cache/prefetch, selector/Cubit/transport ve P4b2
rezervasyon algoritması değişmez. Yeni scheduler/retry/telemetry servisi eklenmez.
OQ-2/OQ-5, EXT-MEDIA/EXT-PUBLICATION, URL yenileme, HTTP cache/session/retention,
üretim profili ve sayısal bütçe kapıları açıktır. Fiziksel Android/iPhone, profile/release,
kalite/RSS/GPU/peak kabulü yapılmaz. P0 ham kanıt sınırlaması ve JPEG/WebP bulanıklığı sürer.
