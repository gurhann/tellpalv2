# P4a2 kontrollü karşılaştırma sözleşmesi

Durum: **P4a2 kod kapsamı ve seçilmiş kaynak adayları onaylı; sayısal bulut çalışması ve cihaz kabulü ayrı kapılardır**. Tarih: 2026-10-17.
Kanonik görev: [P4a2 spec](../../_bmad-output/implementation-artifacts/spec-p4a2-reading-image-comparison.md).
Kaynak kanıtı: [ön inceleme](evidence/p4-reading-images/p4a2-preflight.md).

## Amaç ve açık kapılar

Tek kullanıcı amacı, seçilmiş gerçek deneme kaynağı ile doğrulanmış çıktıyı aynı mobil alanda karşılaştırmaktır. Ayrı local/test girişidir. Normal Library kartları, detail rotası, reader PoC, ses ve cache davranışları korunur. Mevcut Library→detail bağlantısı, çalışan bütünleşik reader geçişi varmış gibi sunulmaz. CMS çıktı önizlemesi bu dilimde yoktur.

| Kapı | Durum |
|---|---|
| Dar erişim | Ayrı local/test girişi ve kanonik QA UID onaylı. TR71 kilitli; yalnız yeni QA kopyasına free hakkı onaylı. |
| Teslimat | Mevcut Firebase SDK ile authenticated backend teslimatı seçildi. Yerel gerçek HTTP/iptal kanıtı var; Firebase ve cihaz yük kanıtı açık. |
| URL yenileme | Signed URL yok. Her istekte bearer doğrulanır; stale identity otomatik yenilenmez. |
| HTTP cache | `private,no-store` ve disk/offline yasağı seçildi. Yerel gerçek HTTP yanıtlarında doğrulandı; fiziksel cihaz denemesi açık. |
| Session/retention | Retained reader session yok. Görünen byte yalnız geçerli karşılaştırma sahipliği boyunca tutulur. |
| Üretim profili | Kapalı. Kaynak adayları, sayısal deneme profili ve bulut işlem kaydı farklı kararlardır. |
| Cihaz/kalite | Android/iPhone fiziksel kanıtı yok; P0 eksik ham kanıt sınırı sürer. |

## Kaynak ve deneme kopyası

TR71'in 19 JPEG kaynağı güncel olarak okundu; tümü mevcut P2 JPEG metadata kontrolünden kaldı. P2 kabulü genişletilmez. Onaylı ilk fixture kapsamı, kapak `9778` ve ilk iki sayfanın `9816/9818` ayrı QA kopyasıdır. Metin ve gerekli mevcut ses metadata referansları korunabilir; ses dosyası üretilmez/taşınmaz ve yeni ses davranışı eklenmez.

Yerel tam çözünürlüklü canonical PNG adayları ancak açık seçimden sonra yeni kaynak olarak kaydedilebilir. Bu kopyanın ücretsiz erişimi açık fixture kapsamıdır; TR71 `defaultFree=false` ve mevcut abonelik fixture'ı korunur. Kopya kategoriye veya Library home'a eklenmez. Normal listeler üzerindeki etkisizlik ayrıca doğrulanır.

Kontrol kolu, aynı LIVE işinin yakaladığı generation-pinned canonical kaynaktır. Çıktı kolu aynı işin doğrulanmış rendition'ıdır. İstemci keyfi asset/path veya eski mutable original isteyemez. UI kontrolü **canonical PNG kaynak** olarak adlandırır; bunu cihazda gösterilen eski JPEG saymaz. Eski JPEG byte tabanı ayrıca raporlanır. JPEG'in cihazda birebir karşılaştırılması ve decoder farkı bu iki kolun kanıtı değildir; raporda açık kalır.

Fixture oluşturma dahil gerçek çalışma ayrı kayıt ister: kaynak hash'leri, kopya kimliği, dil/rol profilleri, tam sayfa kümesi, izinli local/test prefix, kaynak/çıktı byte-piksel sınırları, toplam okuma/yazma/nesne tavanı, deadline, eşzamanlılık ve kontrol bütçeleri. Yeni kopyanın legacy processing durumu `PENDING` kalır; normal public/list/detail/pages görünürlüğünden dışlandığı gerçek API ile sınanır. P3 LIVE bu alanı `COMPLETED` yapmaz; poller açılmaz. Bu yalıtım kanıtlanamazsa fixture çalışması durur. Üç kaynakta rol başına tek aday için 13 yeni nesne tahmini vardır: üç yüklenen kaynak, üç admitted kopya, üç captured kaynak, dört çıktı. Bu tahmin yeniden doğrulanır; sınırsız retry veya başarısız nesneyi gizleyen sayaç sıfırlama yoktur. Gerçek profil seçilmeden hiçbirini çalıştırma.

## Sorumluluk ve mimari

Onaylı dar yön `user → content::api → asset::api` olur. `user/package-info.java` yalnız `content::api` ekler. Ters bağımlılık ve döngü eklenmez. Bağımsız QA girişinde kategori iş kuralı bulunmadığı için yapay kategori veya aktarım facade'ı eklenmez. Bu değişiklik ADR-0024, architecture ve project memory'de açık kaydedilir; mevcut Library endpoint'i aynı category sözleşmesinde kalır.

`user`, gerçek Firebase bearer doğrulaması ve tek UID admission'ını sahiplenir. Özellik varsayılan kapalıdır; local/test gerekir, prod/production varsa açılmaz. Gerçek QA modunda stub token reddedilir. Her manifest/status/byte isteğinde doğrulama yapılır; token revoke kontrolünün etkinliği ayrıca doğrulanır. İstemci UID, subscriber/free hakkı veya storage path seçemez. Token/credential/URL loglanmaz. Gerçek oturum repo dışında tutulur.

`content`, server-configured tek QA contentId/tam dil, aktif içerik ve yerelleştirme, editorial uygunluk, ücretsiz erişim ve eligible retained LIVE'ı denetler. P4a2 abonelik sistemi eklemez; free olmayan içerik reddedilir. `ReadingPublicationApi.status` tek başına teslimat izni değildir. Legacy `PUBLISHED/COMPLETED` veya P3 desired generation teslimat kimliği olmaz.

`asset`, tam manifest, kaynak admission'ları, pinned kaynak/çıktı, boyut/SHA/MIME/tam decode ve availability bütünlüğünü sahiplenir. Descriptor P4a1 canonical digest ile aynıdır; eksik rol/sayfa kümesi kısmi başarı sayılmaz. P2 rol başına mevcut tek adayı üretir; rendition merdiveni eklenmez. İstemci P4a1 seçicinin iki eksenli minimum alan/byte/kimlik sırasını kullanır; kalite onayı çıkarmaz.

## İstek ve yarış sözleşmesi

1. Manifest isteği, bearer admission sonrası kısa DB kesitinde ücretsiz erişimi ve eligible LIVE'ı alır. Kimlik `contentId/language/liveGeneration/revision/manifestDigest/jobId` olur. `jobId`, `live_proof_json` içinden alınır; yeni desired adayın `job_id` alanından alınmaz. Descriptor URL/token içermez. İzin süresi, kontrol sıklığı ve kaynak limitleri zorunlu çalışma ayarlarıdır; gizli varsayılan yoktur.
2. Byte isteği aynı tam kimliği, rol/sayfa/rendition seçimini taşır. Server bunları kendi LIVE manifestinden çözer. Rastgele profil/proof/path kabul etmez. I/O öncesi admission ve uygunluk denetlenir.
3. Storage okuma ve doğrulama DB transaction/lock dışında yürür. Mevcut SDK `openVersion` generation + generationMatch ile kullanılır. Bounded okuma tamamlanıp byte/SHA/MIME/tam decode doğrulanmadan HTTP body başlamaz. Doğrulanmamış streaming veya Range yolu eklenmez. İptal/timeout süresi ölçülmeden kesin sonlanma iddiası yapılmaz.
4. I/O sonrası kısa DB kesiti aynı LIVE, kaynak uygunluğu, availability ve free/editorial koşullarını yeniden kontrol eder. Gereken kilit sırası content→publication→asset admissions (ID sıralı)→availability olur. Eski proof availability epoch'u, normal yeni verification rezervasyonu nedeniyle kör eşitlik şartı yapılmaz; mevcut retained uygunluk kuralı korunur.
5. Gözlenen pinned byte bozulması/eksikliği mevcut withdrawal mekanizmasını kullanır. Kapasite veya timeout sağlıklı LIVE'ı bozuk saymaz. Uygun final kesitten sonra `private,no-store` ile yalnız doğrulanmış byte gönderilir; hata yanıtları da cache edilmez. Son kesitten sonra başka işlem durumu değiştirirse gönderilmiş byte geri alınamaz; anlık revoke garantisi yoktur.

HTTP sözleşmesi implementation sırasında REST standardıyla belgelenir: geçersiz token `401`, kapsam/erişim reddi `403`, olmayan kaynak `404`, eski LIVE kimliği `409`, geçersiz istek `400`, kaynak/deadline reddi ayrı sonlu hata. Hata response'ları özel storage bilgisi içermez. Kontrol isteği başarısızlığında otomatik retry yoktur; kullanıcı açık yeniden deneme yapabilir.

## Flutter sahipliği ve gösterim

Feature-first domain repository/use-case→data DTO/datasource/implementation→injectable Freezed Cubit→screen/view düzeni kullanılır. Repository `Either` döner; route yalnız ID taşır. Normal uygulamaya yeni Firebase paketi veya auth akışı eklenmez; mevcut geliştirme yapılandırmasının geçici bearer sağlayıcısı kullanılır. Hesap/token nesli değişimi bu sağlayıcıdan izlenemiyorsa karşılaştırma başlamaz.

Diske yazmayan bu onaylı QA kapsamı için `CachedNetworkImage` yerine sahipliği açık bellek raster'ı gerekir. ADR-0018 ve project memory yalnız bu istisnayı kaydeder. Genel tüketici/cache politikası değişmez. Abort edilebilir HTTP byte kaynağı→checksum→`ImmutableBuffer`/`ImageDescriptor`→sınırlı codec→sahip olunan `ui.Image`/`RawImage` yolu kullanılır; provider/precache ayrışması yoktur. Precache yapılmaz. Buffer/descriptor/codec/image dispose edilir; global ImageCache veya disk cache kullanılmaz.

İki kol sırayla aynı `LayoutBuilder` alanını, fit ve alignment'ı kullanır. Detail cover geometri ölçümü mevcut hero düzenini, sayfa contain ölçümü reader alanını referans alır. Normal widget'ların ağ/cache davranışı değiştirilmez. Görünür slot/DPR/fit değişince seçim yeniden yapılır. Görünmeyen iş başlatılmaz. Bir görünür medya işi vardır; eski iş gerçekten durmadan/sonlanmadan yeni iş kapasitesi serbest sayılmaz. Codec anında iptal edilemiyorsa tamamlanması beklenir, geç sonuç atılır.

Zorunlu deneme limitleri encoded byte, buffer toplamı, decode piksel/byte, sahip olunan görüntü, transfer/decode süresi, görünür status kontrol sıklığı ve kontrol sayısı/süresini kapsar. Birlikte tutulan byte ve decode tepe belleği hesaba katılır; yalnız final `ui.Image` sayılmaz. Sayılar eksikse medya modu başlamaz. Ağsız geometri ölçüm modu çalışabilir; bu profil onayı değildir.

Her async tamamlanma ekran/account/token/identity/selection nesliyle eşleşir. Ekran kapanışı, hesap/token değişimi, background, kontrol hatası, lease expiry veya gözlenen LIVE değişimi görseli temizler ve işleri durdurur. Foreground ve süre yenileme yeniden bearer admission + aynı LIVE kontrolü ister; yeni LIVE otomatik devralınmaz. Görünürken status kontrolü sınırlı polling yapar; sonsuz çalışma veya otomatik hata retry yoktur. Poll aralarında değişiklik anında bilinemez; bu aralık ve eski görselin azami tutulma süresi cihaz raporunda belirtilir. Backend signed URL üretmediğinden URL expiry yerine bearer/admission süresi sınanır.

## Anlamlı doğrulama

- Gerçek PostgreSQL/Testcontainers: kapsam/erişim/LIVE değişiminin I/O öncesi-sonrası reddi; eski desired işinden ayrı retained LIVE; availability withdrawal; rollback ve module sınırları.
- Gerçek yerel HTTP ve codec: eksik/yanlış byte, uzun gövde, geç tamamlanma, iptal/close/background/hesap değişimi; kaynak ve decode sahipliği sayaçları. Mock/derleme bu kanıtın yerine geçmez.
- Flutter: aynı slot/fit, rol/sıralı sayfa seçimi, descriptor/digest, shortage, görünür iş önceliği, disposal ve disk yokluğu. Mevcut Library/detail/reader/ses/cache regresyonları.
- Gerçek QA çalışması: kaynak/profile/run kimliği; seçilen rendition ve ihtiyaç; HTTP payload byte; legacy JPEG ve canonical PNG tabanları ayrı; decode boyutu/tepe bellek/zaman, iptal ve görüntü kanıtı. Android ve iPhone ayrı kabul edilir. Ulaşılamayan cihaz açık bırakılır.

P4a2 kodunun doğrulanması, gerçek fixture çalışması ve fiziksel cihaz kabulü ayrı sonuçlardır. P4b genel tüketici entegrasyonu, çoklu görünür iş/prefetch planlaması, üretim bellek/cache/session profilleri ve geniş yaşam döngüsü yeterliliği bu dilimden otomatik çıkmaz. Android 0,75× bulanıklığı, örnekte 1× netliği ve seçili iPhone PNG kabulü genel ayar değildir; JPEG/WebP sorunu ve P0 ham kanıt eksikliği kapanmaz.

## Uygulama kod haritası

Backend yolları `be/src/main/java/com/tellpal/v2/` altındadır. Yeni sınıf adları uygulama hedefidir; mevcut sınıflar gibi gösterilmez.

| Yol | İşlem / dayanak |
|---|---|
| `user/web/mobile/ReadingImageComparisonTestController.java` | Yeni manifest/status/byte endpoint'leri; REST standardı, ProblemDetail ve no-store. |
| `user/application/ReadingImageComparisonTestService.java` | Yeni gerçek bearer, tek UID, profile/scope admission ve final response koordinasyonu. |
| `user/application/LibraryHomeTestQueryService.java` | Salt okunur auth/audience örneği; subscriber fixture veya Library servisi değiştirilmez. |
| `user/package-info.java` | Yalnız `content::api` yönünü ekle. |
| `content/api/ReadingImageComparisonApi.java` | Yeni dar capture/revalidate/delivery kimlik sözleşmesi; public API iç tiplere sızmaz. |
| `content/application/publication/ReadingImageComparisonService.java` | Yeni ücretsiz QA scope ve retained LIVE kesitleri; I/O'yu transaction dışında koordine et. |
| `content/application/publication/ReadingPublicationService.java` | Mevcut retained durum uygunluğunun dayanağı; desired/live karışmasın. |
| `content/infrastructure/persistence/publication/JdbcReadingPublicationStore.java` | Mevcut proof/generation persistence; dar DB okuma eklenmesi gerekirse yeni davranışı normal yayın akışından ayır. |
| `asset/api/ReadingImageDeliveryApi.java` | Yeni typed manifest ve bounded verified byte sözleşmesi; keyfi storage kimliği kabul etme. |
| `asset/application/reading/ReadingImageDeliveryService.java` | Yeni manifest/kaynak/çıktı çözümü ve integrity withdrawal. |
| `asset/infrastructure/storage/ReadingObjectStorage.java` | Mevcut pinned/bounded SDK okuması; yeni storage katmanı ekleme. |
| `asset/application/reading/ReadingPublicationReadyService.java` | Mevcut `retainedEligible` kaynağı; çağrı DB transaction ister. |
| `asset/domain/reading/ReadingProfiles.java` | Salt okunur fingerprint kapsamı. Mevcut hazırlama sınıfları değiştirilirse profile drift'ini gizleme. |

Flutter yolları `app/mobile/lib/` altındadır. P4a1 domain entity/validator/selector değişmeden kullanılır; zorunlu bir düzeltme çıkarsa regresyon kanıtıyla ayrıca kaydedilir.

| Yol | İşlem |
|---|---|
| `features/reading_images/domain/repository/reading_image_comparison_repository.dart` | Yeni `Either` repository; native/HTTP tipi domain'e sızmaz. |
| `features/reading_images/domain/use_case/load_reading_image_comparison.dart` | Yeni descriptor admission ve seçim koordinasyonu. |
| `features/reading_images/data/model/reading_image_comparison_model.dart` | Yeni JsonSerializable DTO; domain entity'yi genişletir. |
| `features/reading_images/data/data_source/reading_image_comparison_data_source.dart` | Yeni bounded, abort edilebilir HTTP ve no-store kontrolleri. |
| `features/reading_images/data/repository/reading_image_comparison_repository_impl.dart` | Yeni tipli hata eşlemesi ve byte bütünlüğü. |
| `features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart` | Yeni injectable sahiplik, kontrol ve lifecycle durumu. |
| `features/reading_images/presentation/cubit/reading_image_comparison_state.dart` | Yeni Freezed durumlar; initial/loading/empty/error/loaded. |
| `features/reading_images/presentation/screen/reading_image_comparison_screen.dart` | Yeni RoutePage + DI kompozisyonu; yalnız ID. |
| `features/reading_images/presentation/view/reading_image_comparison_view.dart` | Yeni aynı slotta ardışık kontrol/rendition ve açık hata. |
| `features/reading_images/presentation/widgets/reading_image_comparison_raster.dart` | Yeni owned raster/codec ve dispose. |
| `app/reading_image_comparison_app.dart`, `app/reading_image_comparison_configuration.dart`, `app/reading_image_comparison_router.dart` | Ayrı dev girişi, token/account nesli, zorunlu ayarlar ve auto_route. Normal router'ı değiştirme. |
| `main_reading_image_comparison.dart`, `app/injection.dart` | Ayrı giriş ve mevcut DI kalıpları; üretilen dosyaları generator ile güncelle. |

Yeni backend testleri `be/src/test/java/com/tellpal/v2/` altında `user/web/mobile/ReadingImageComparisonTestControllerIT.java`, `content/application/publication/ReadingImageComparisonServiceIT.java`, `asset/application/reading/ReadingImageDeliveryServiceIT.java` olur. Mevcut module verification testi çalıştırılır. Gerçek PostgreSQL ve storage fake/yerel bounded stream, erişim ve yarışları ayrı sınar; gerçek Firebase teslimatı olarak etiketlenmez.

Yeni Flutter testleri `app/mobile/test/reading_images/reading_image_comparison_cubit_test.dart`, `reading_image_comparison_http_test.dart`, `reading_image_comparison_raster_test.dart`, `reading_image_comparison_view_test.dart` olur. Native codec, yerel HTTP ve sahiplik yarışları kapsanır. Fiziksel cihaz denemesi ayrı `verification.md` kaydı ister.

## Uygulanan local/test girişinin çalıştırma ayarları

Kod kapsamı [ADR-0024](adr/ADR-0024-reading-image-comparison.md) ile kaydedildi. Aşağıdaki ayarlar
boş/sıfır varsayılanıyla kapalıdır; örnek sayısal profil sağlanmaz. Bu bölüm gerçek çalışma kararı değildir.

Backend `tellpal.reading-comparison` altında `enabled`, `content-id` (TR71 hariç ayrı kopya),
`language=tr`, `run-id`, `max-encoded-bytes`, `max-pixels`, `max-memory-bytes`, `deadline-millis`,
`lease-millis`, `max-total-operations`, `max-total-bytes` ister. Firebase `stub-tokens-enabled=false`
ve `check-revoked=true` olmalıdır. Uygun retained P3 LIVE ayrıca gereklidir. V37 yalnız asset-owned
okuma rezervasyonu saklar; aynı run'ın tavanını değiştirmek reddedilir. Rezervasyon başarısızlıkta
geri verilmez. Upload/admission/çıktı üretimi bu sayaçla yetkilendirilmez; onların ayrı toplam bulut
kaydı tamamlanmadan fixture çalışması yapılmaz. Migration bu görevde yalnız Testcontainers'ta sınanır.

Mobil ayrı giriş `lib/main_reading_image_comparison.dart` dosyasıdır. Varsayılan yalnız ağsız geometri
modudur. Debugger/test kompozisyonu, `ReadingImageComparisonHarness.configuration` içine mevcut
geçici `ImageExperimentCredentials` sağlayıcısını ve açık `ReadingComparisonLimits` değerlerini verir;
`contentId` yalnız route kimliğidir. Credential/account nesli değişirken eski sağlayıcı revoke edilir.
Token UI, compile-time define veya dosyaya yazılmaz. Süresi dolan/revoke edilen sağlayıcı yeniden
kullanılmaz. Normal uygulamaya yeni auth paketi veya giriş akışı eklenmez.

Manifest: `GET /api/test/reading-image-comparison/manifest?contentId=...&language=tr`.
Status ve bytes; `contentId/language/liveGeneration/revision/manifestDigest/jobId` tam kimliğini ister.
Bytes ayrıca `role/pageNumber/renditionKey/canonicalSource` seçimini alır. İstek header'ı Firebase
bearer'dır; hata yanıtları ProblemDetail'dır. URL/proof/path istemci tarafından seçilemez.

## Onarım sonrası adapter sınırları

P4a2 response teslimatı local/test embedded Tomcat 11 capability'sini ister. Dar Valve yalnız
karşılaştırma yoluna abort callback'i verir. Desteklenmeyen container/wrapper auth/storage öncesi
reddedilir. Nonblocking yazım ve çalışma deadline'ı `async.complete` sonrası bekleyen flush'ı da
sınırlar. Bağlantı abortu sonrası container'ın onComplete/onError cleanup'ı permit'i bırakır.
Storage SDK'nin mutlak in-flight iptali bu response düzeltmesiyle doğrulanmış sayılmaz.

Client route'un ilk LIVE kimliğini saklar; yeni admission anchor'ı değiştiremez. Yeni LIVE için yeni
route/Cubit yaşamı gerekir. Hata/lease/background seçili sayfa ve kolu korur. Raster borrowing painter
ile çizilir; ui.Image clone'u oluşturulmaz. Native sahiplik testi engine/GPU/RSS toplamı kanıtı değildir.
HTTP identity payload dışında Content-Encoding kabul edilmez; gzip transparan açılmaz. Teknik
Timer/SDK sınırı int32 milisaniyedir; sayısal üretim/deneme profili hâlâ ayrı karardır.
