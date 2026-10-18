# P4a4 inceleme ve uygulama haritası

Tarih: 2026-10-18. Durum: **yalnız plan girdisi**. Kanonik spec: [P4a4](../spec-p4a4-qa-consumer-delivery.md).
Git başlangıcı `339aee45a99e4fcda27132a2d9e62946267d142e`; branch `codex/image-optimization-p3-publication`.
P4a3 tamamlanmıştır. Genel P4 geçişi tamamlanmamıştır.

## Seçilen en küçük dilim

Ayrı local/test QA kompozisyonu, normal detail ve reader’ın **aynı gerçek sunum widget’larını** kullanır. Normal ekranların görsel alanı kopyalanmaz; sunum widget’larına çıkarılır ve iki tüketicide de kullanılır. Böylece mevcut doğrulanmış P3 görüntüleri gerçek tüketici geometrisinde sınanabilir.

Bu, normal Library→detail→reader ürün akışını açmaz. Mevcut detail CTA pasiftir ve LibraryRouter’da reader rotası yoktur. #94, PENDING kaldığı ve kategoriye eklenmediği için normal metadata API’lerinden okunamaz. Bu koşullar korunur.

QA başlığı yalnız `QA deneme hikâyesi #94`, sayfa etiketi descriptor’ın gerçek `pageNumber` değeridir. Gerçek başlık, açıklama, hikâye metni, kategori, contributor veya ses metadata’sı varmış gibi sunulmaz. `StoryDetail`/`StoryDetailIdentity` modellerine sahte categoryId doldurulmaz. `StoryManifest`/`MediaGrant` için sahte URL, expiry veya revision üretilmez. Gerçek ürün metadatası ve ses bootstrap’ı ayrı sözleşmedir.

QA görsel reader’ında açıkça “Bu QA denemesinde ses kapalı” yazılır. Ses yokluğu iddia edilmez. Bu dal AudioPlayer oluşturmaz, ses/grant endpoint’i çağırmaz. Normal reader’ın manifest, ses, selected→prepare, prefetch, cache ve dispose sahipliği korunur.

## Tarihsel çıktı seçimi

Tek mevcut aday #94, `tr`, LIVE generation `4`:

- Job: `bb9ae40b-e5ab-4054-885e-0b1fc14e841e`.
- Revision: `3ddef109a065667d6e44f19aabce9151f1295d8f1e2502a4ba5b14e29a5d458d`.
- Manifest digest: `d4636ac8e61967aad2f2a53d9aa1da736214075a915f2dd53b7d1d80fe1a42e2`.
- Kaynaklar `1712×2778` canonical PNG; dört rol çıktısı `822×1334`, `image/png`, her biri `3.292.721` byte.

| Rol | Kimlik | Rendition key |
|---|---|---|
| READING_COVER | localization 297, admitted asset 11261 | `230126349cc9db0487e2f77b0d488e53b4804acf2cd76d79b286fe54eb84e2f4` |
| READING_DETAIL | localization 297, admitted asset 11261 | `8d3c882f7d82a7f4e0f80894b04322407494dc96eb4e26bb3408de0e501a3c45` |
| READING_PAGE | localization 4656, pageId 1427, sıra 1, asset 11262 | `a02aac0b9869606049e1714b72faa459fef9913363e3801e8da62c02f5dbab92` |
| READING_PAGE | localization 4657, pageId 1428, sıra 2, asset 11263 | `02d8b7d9015ec2cb1a930552392bac42e33c951c7232bad8ff076075c23c1226` |

Detail `READING_DETAIL` kullanır; cover ile byte/SHA aynı olsa da rol/profile/key/generation birbirinin yerine geçmez. Reader binding’i rol + gerçek pageId/pageNumber ile çözülür; rastgele liste indeksi kimlik değildir. Yeni rota yalnız doğrulanmış rendition gösterir. Mevcut karşılaştırma ekranının canonical kaynak kolu korunur; yeni tüketici akışında original fallback yapılmaz.

Bu PNG’ler eski JPEG’lerden daha büyüktür. Bu dilim sıkıştırma kazancı iddia etmez. Sonraki yerel optimize PNG toplamı `5.112.408`, WebP toplamı `3.020.382` byte’tır; bunlar P3 manifest/LIVE’a bağlı değildir. Eski key altında sunulmaları yasaktır. Yeni üretim, upload, admission, profile fingerprint yenileme veya publish yapılmaz.

Tarihsel makbuz güncel admission değildir. Ayrı onaylı gerçek çalışmanın ön kontrolünde aynı tam identity, kaynak/çıktı generation/SHA/profile bağları, ücretsiz/aktif/editorial uygunluk, retained LIVE ve availability yeniden doğrulanmalıdır. Değişmiş veya eksikse durulur; yeni üretim veya otomatik yeni revision benimsemesi yapılmaz.

Kanıtlar: [P3 çalışma sonucu](../../../be/docs/evidence/p4-reading-images/real-run-result-2026-10-17.md), [Tam nesne/kimlik makbuzu](../../../be/docs/evidence/p4-reading-images/execution-resume-2-2026-10-17.json), [Android emülatör ölçümü](../../../be/docs/evidence/p4-reading-images/android-measurement-2026-10-18.md), [Yerel PNG düzeltmesi](../../../be/docs/evidence/png-lossless-compression/README.md), [Yerel WebP sınırı](../../../be/docs/evidence/local-lossless-formats/verification.md).

## Flutter kod haritası ve dosya planı

Mobil yollar `app/mobile/` altındadır.

| Mevcut kaynak | Korunacak / yeniden kullanılacak nokta |
|---|---|
| `lib/features/story_detail/presentation/view/story_detail_view.dart:159` `_HeroArea` | Clip, semantics, oran, blur, metadata/overlay ve geri düğmesi; normal hero/provider aynı sahibinde. |
| Aynı dosya `:298` `_DeferredAction` | Normal CTA pasif kalır; QA aksiyonu ayrı kompozisyondadır. |
| `lib/story_player_page.dart:347` `_buildReader` | SafeArea, header, Expanded görsel alanı ve kontrol yuvaları sunum olarak çıkarılır. |
| Aynı dosya `:128`, `:231` | Normal selected hazırlığı, AudioPlayer/grant ve next prefetch değiştirilmez. |
| `lib/story_reader_images.dart:5` | Mevcut provider sözleşmesi korunur; owned raster bu interface’e zorlanmaz. |
| `lib/core/presentation/widgets/reading_image_viewport.dart:18` | Mevcut son slot/DPR/fit ölçümü. |
| `lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart:15` | Mevcut selector/validator, account, anchor, generation, gate, lease/status, cancellation ve native cleanup. |
| Aynı dosya `:298` `changeChoice` | Akış içi rol/sayfa geçişi eski raster’ı bırakır; `_anchor` korunur. |
| `lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart:10` | Tek sahipli gerçek decoder; borrowing painter, disk/global ImageCache yok. |
| `lib/app/reading_image_comparison_router.dart:8` | Mevcut izole QA router; yeni klasörü generator kapsamına al. |
| `lib/app/reading_image_comparison_injection.dart:9` | Mevcut Cubit/repository/work gate kaydı kullanılır. Yeni taşıma/iş servisi gerekmez. |

Yeni sunum dosyaları:

- `lib/core/presentation/widgets/reading_detail_artwork_frame.dart`: yalnız Flutter/core presentation tipleri; mevcut oran/clip/Stack ve artwork/overlay yuvaları. Normal `_HeroArea` gerçek metadata’sını, mevcut hero ve kontrollerini bu frame’e verir. QA aynı frame’i açık QA label ile kullanır.
- `lib/core/presentation/widgets/reading_page_frame.dart`: mevcut reader yerleşimi; header, ölçülmüş görsel builder, progress/error, audio ve sayfa kontrol yuvaları. İş kuralı, domain entity, HTTP veya provider sahipliği almaz. Normal `_buildReader` kendi gerçek medya/kontrol widget’larını sağlar; QA ses kapalı bildirimi ve descriptor sayfa seçimini sağlar.

Yeni kompozisyon dosyaları `lib/app/reading_image_consumer_qa/` altında:

- `flow_screen.dart`: `@RoutePage`, parent scope; mevcut DI’den **tek** `ReadingImageComparisonCubit`, lifecycle/route görünürlüğü ve kapanış sahibi.
- `detail_screen.dart`: ID taşıyan child route; ortak detail frame, yalnız QA etiketi ve uygun admission sonrası reader aksiyonu.
- `reader_screen.dart`: ID taşıyan child route; ortak reader frame ve descriptor’daki sıralı iki sayfa. Ürün StoryManifest/MediaGrant oluşturmaz.
- `consumer_image_slot.dart`: frame geometrisini mevcut `select` çağrısına bağlayan presentation widget’ı. Detail `READING_DETAIL`; reader `READING_PAGE`. Doğrulanmış binding’i mevcut descriptor içinden çözer ve existing raster painter’ını kullanır; selector/facade veya taşıma servisi eklemez.

`lib/app/reading_image_comparison_router.dart` ve `reading_image_comparison_app.dart`: açık opt-in tüketici girişini ekle; eski comparison rotası varsayılan olarak aynı kalır. Yeni route argümanları yalnız contentId/pageId gibi ID’lerdir; descriptor/token/Cubit taşınmaz. Codegen taraması yalnız mevcut reading_images ile yeni QA klasörünü kapsar. Giriş seçimi geçerli numeric configuration yokken gerçek medya açmaz. Gerekirse mevcut `reading_image_comparison_injection.dart` kapsamı yalnız bu kompozisyon için ayarlanır; ikinci GetIt/transport/queue kurulmaz. Annotation değişince ilgili `.gr.dart`/`.config.dart` çıktıları mevcut build_runner ile üretilir.

Normal `LibraryRouter`, genel detail/data/repository ve legacy reader entry değişmez. `story_detail ↔ reading_images` feature importu eklenmez; yalnız `app/` kompozisyonu feature bağımlılıklarını birleştirir. Core’a P3/QA iş modeli, descriptor veya native image sahipliği taşınmaz. Yeni domain modeli/Cubit gerekmez.

## Akış, geometri ve sahiplik

1. QA kapısı açık ve geçerli onaylı configuration varsa tek flow başlar. Sunucu seçilmiş contentId/UID/dili kabul etmeden görsel gösterilmez. Kimlik `contentId/language/liveGeneration/revision/manifestDigest/jobId` birlikte doğrulanır.
2. “Güncel” sunucunun uygun **retained LIVE işaretçisidir**; latest desired generation değildir. Yeni desired iş hazırlanırken sağlıklı retained LIVE sırf eski diye reddedilmez. İlk admission flow boyunca anchor olur.
3. Görünür detail veya seçili sayfa önceliklidir. `changeChoice` eski raster/işi iptal eder; mevcut ortak work gate eski native Future gerçekten bitmeden yenisini başlatmaz. QA prefetch/precache/provider yolu yoktur. Limitler mevcut açık QA configuration’dan gelir; üretim sayısı veya yeni limit seçilmez.
4. Detail→reader, reader sayfa değişimi ve geri dönüş aynı Cubit/anchor içinde kalır. Örtülen detail raster tutmaz; child route ayrı Cubit kurarak anchor sıfırlamaz. Page binding doğrulanmadan byte istenmez. Dış kontroller farklı olabileceği için QA slot yüksekliği normal ekranla eşittir iddiası yapılmaz.
5. Account/credential değişimi, background, lease/status hatası veya LIVE değişimi mevcut temizleme/re-admission kurallarını kullanır. Kendiliğinden oturum yenileme, yeni sign-in, yeni anchor veya retained-session hakkı yoktur. Dönüşte açık hata/yeniden giriş durumu gösterilir. Aynı flow’daki tekrar admission anchor’ı değiştirmez.
6. Route kapanışında `close`/native drain beklenir; `dispose`, cache clear veya iptal talebi tek başına sonlanma kanıtı değildir. Ani route/OS kapanışında raster hemen bırakılır ve ortak gate bitmemiş işin kapasitesini korur. Eski epoch sonucu boyanamaz.

Eski emülatör örneğinde detail `411×456,667`, DPR `2`, cover ve reader `411×694`, DPR `2`, contain ihtiyacı mevcut `822×1334` adayı karşılamıştır. Bunlar sabit UI boyutu yapılmaz. Yeni gerçek frame ölçümü esas alınır. Aynı detail genişliğinde DPR `2.625` çıktı yetersizdir; açık shortage beklenir. Yüksek DPR’de canonical PNG’ye dönerek başarı gösterilmez. Rol başına tek aday gerçek çoklu merdiven seçimi veya üretim kalitesi kanıtı değildir.

## Backend yeniden kullanım ve test haritası

Backend üretim kodu/endpoint/schema değişikliği planlanmıyor. Şunlar korunur:

- `be/src/main/java/com/tellpal/v2/user/web/mobile/ReadingImageComparisonTestController.java:38`: mevcut manifest/status/bytes; Range reddi ve tek response gate’i.
- `.../user/application/ReadingImageComparisonTestService.java:22`: gerçek revoke kontrollü bearer, kanonik UID ve I/O sonrası yeniden bearer/final DB kesiti.
- `.../content/application/publication/ReadingImageComparisonService.java:56`: ücretsiz/aktif/exact-language/editorial uygunluk ve retained LIVE; desired iş kimliği teslimat kimliği değildir.
- `.../asset/application/reading/ReadingImageDeliveryService.java:41`: SDK pinned okuma; tam byte/SHA/MIME/header/decode; I/O transaction dışında. Bozulma withdrawal yapar, geçici kapasite/transport reddi sağlıklı LIVE’ı bozmaz.
- `.../asset/application/reading/ReadingComparisonBudget.java:17`: kalıcı rezervasyon; restart/sayaç sıfırlama veya aynı run tavanını artırma yok.
- `.../user/web/mobile/ReadingImageComparisonCacheFilter.java:12`: hatalar dahil `private,no-store`.

Mevcut normal metadata sorgusundaki COMPLETED ve category membership koşulları gevşetilmez. TR71’in eski V1 image/audio signed URL manifesti #94 ile karıştırılmaz. PENDING kopya normal public/detail/pages/Library’den dışarıda kalır. #112 için poller açılmaz veya durum elle değiştirilmez.

## Test dosyaları ve kabul kanıtı

Yeni `app/mobile/test/reading_images/reading_image_consumer_qa_test.dart`: gerçek decoder ve bounded loopback HTTP ile tek QA detail→reader→iki sayfa→geri→kapanış yolunu sınar. Tam role/page/identity, selected rendition, gerçek slot/DPR/fit, decode, çizilmiş frame ve payload byte kaydı ilişkilendirilir. Özellikle:

- Opt-in kapalıyken yeni I/O yok; yetki reddi medya byte’ı vermez. Yanlış dil/rol/page sırası/revision/digest/profile/generation, eksik ve bozuk çıktı ret üretir. Legacy original/provider sayacı sıfır kalır.
- Tall/wide/kesirli DPR/resize/text-scale ve shortage mevcut bağımsız selector oracle’ı ile sınanır. `geometry.fit` ile gerçek painter’ın fit’i eşleşir. Yerel optimize dosyanın eski SHA/key altında sunulması reddedilir.
- Hızlı sayfa/route değişiminde eski epoch boyanmaz. Önceki native iş sürerken yeni storage/decode başlamaz. Tek image sahipliği, no-store/disk/global ImageCache etkisizliği; account/background/resume/lease/LIVE değişimi ve gerçek bounded close sınanır. Native sahiplik sayaçlarının sıfırı GPU/RSS toplamının sıfırı sayılmaz.
- Yeni QA dalında AudioPlayer ve audio/grant/network prefetch çağrısı sıfırdır. Normal kolun provider/prepare/precache/generation/AudioPlayer ve gezinme sırası mevcut regresyon fixture’larıyla değişmeden geçer.

Yeni `app/mobile/test/reading_consumer_frames_test.dart`: çıkarılan frame’lerde mevcut oran, clip/blur/semantics/padding, reader kontrol alanları ve 1×1 raster intrinsic boyut/merkez korumasını sınar.

Mevcut `test/story_detail/story_detail_view_test.dart`, `test/story_detail/story_detail_navigation_test.dart`, `test/image_experiment/reader_resolution_test.dart`, `test/reading_image_viewport_test.dart`, `test/reading_images/` korunur ve yeniden çalıştırılır. Mevcut comparison ekranı ve yerel lossless QA dosyaları bu işe katılıp yeniden yazılmaz.

Backend’e özgü ek matris, mevcut `be/src/test/java/com/tellpal/v2/content/application/publication/ReadingImageComparisonServiceIT.java` ve `be/src/test/java/com/tellpal/v2/user/web/mobile/ReadingImageComparisonTestControllerIT.java` içinde sınırlı test ekler: aynı manifestten detail+iki sıralı page teslimi; değişmiş LIVE’ın I/O öncesi/sonrası reddi; archive/deactivation/free kapanışı; kalıcı tüketim; PENDING kopyanın normal yüzeylerden dışlanması. Mevcut fake pinned disk storage ve PostgreSQL/Tomcat HTTP fixture’ları kullanılır. Bulut veya üretim DB testi değildir.

## İleride uygulanacak doğrulama komutları

Yeni QA çıktıları MAC_EK’te benzersiz özel klasörde; eski snapshot/log ve başarısız denemeler korunur. Kimlik/token/sign URL repo ve rapora girmez. Kaynak kopyası yalnız gerekli dosyalarla sınırlıdır; önceki P4a3 klasörüne yeni P4a4 kaynak yazılmaz.

Mobil özel çalışma kopyasında:

- Annotation değiştiğinde `dart run build_runner build --delete-conflicting-outputs`; yalnız ilgili router/DI generated farkı kabul edilir.
- `flutter test --no-pub test/reading_consumer_frames_test.dart test/reading_image_viewport_test.dart test/reading_images test/story_detail test/image_experiment/reader_resolution_test.dart`.
- Değişen dosyalarda `flutter analyze --no-pub`; `git diff --check`.

Backend özel yerel test ortamında `be/` altında: `./mvnw -Dtest=ReadingImageComparisonServiceIT,ReadingImageComparisonTestControllerIT,ApplicationModulesTest test`. Yerel Postgres/Testcontainers veya Docker ortamı yoksa bu sınır kaydedilir; mock geçişi gerçek IT yerine konmaz. Çalışan backend/frontend tanımlanmadan durdurulmaz. Başka kaynak DB veya bucket’a bağlanılmaz.

## Politikalar ve yürütme kapıları

Uygulama sırasında `apply-project-standards`, AGENTS ve Türkçe belge/yorum kuralı uygulanır. ADR-0018/0024, `be/docs/project-memory.md`, `architecture.md` ve `be/docs/reading-image-comparison.md` değişikliği yalnız bu ayrı QA kompozisyonunda normal tüketici sunum widget’larının owned-raster kullanımını kapsar. Genel cache, abonelik, session/retention veya üretim izni açılmaz. Plan aşamasında bu kabul edilmiş belgeler değiştirilmedi.

Gerçek #94 denemesi ayrı kapıdır: eski `900+180` saniye pencereleri bitmiştir; eski token, giriş hakkı, ledger kalanı ve runId yeni yetki değildir. Yeni somut süre, auth/byte/status çağrısı, toplam okuma/byte ve mevcut tüketimi koruyan yürütme kaydı onaylanmadan gerçek Firebase auth/storage veya cihaz başlatılmaz. 10 USD sınırı geri getirilmez; bütçe/fatura sorgulanmaz. Bu kapı yerel test planını engellemez.

Fiziksel Android/iPhone, RSS/GPU/peak kaynak, yüksek DPR kapsamı ve genel üretim profili ayrıca doğrulanmalıdır. iPhone ertelenmiştir; Rosetta kurulumu yetkilendirilmemiştir. Android 0,75× bulanıklığı, örneğe özgü Android 1×/iPhone 0,75× kararları, JPEG/WebP belirsizliği ve P0 ham kanıt sınırlaması korunur. P4b eşzamanlılık/LRU/preload profili bu planla onaylanmaz.

## Bu planlama turunun sonucu

Yalnız dosya incelemesi ve iki yeni plan belgesi hazırlanmıştır. Test, encoder, DB, Firebase, cihaz, oturum veya çalışan uygulama süreci başlatılmamıştır. Mevcut kayıtlar değiştirilmemiştir. Hazır plan, kod uygulaması ve gerçek cihaz/bulut denemesi farklı onay kapılarıdır.
