# P4b2: Native raster ön rezervasyonu — plan incelemesi

Tarih: 2026-10-18. Başlangıç HEAD: `6c37af44921cb85de220bee504b8fe0dfe3dfaf6`.
Bu kayıt kaynak incelemesidir; yeni test, build, cihaz veya bulut çalışması yapılmadı.
P4b1'in 468 test, temiz analyzer ve üç inceleme sonucu önceki dilime aittir:
[kanıt](../../../be/docs/evidence/p4b1-status-drain/verification.md).
P3 commit'i `79b6cf4` bulundu. Branch `codex/image-optimization-p3-publication`.
Kullanıcının mevcut değişiklikleri koruyarak aynı çalışmayı sürdürme talimatı geçerlidir.
Bu bir Epic story değildir; sprint/story anahtarı yoktur. Model oturumdan devralınır.

## Kanıtlı boşluk ve erişilebilirlik sınırı

`ReadingComparisonWorkGate`, `_render` Future'larını sıraya koyar. Cubit başarılı
raster'ı state'e devrederken Future tamamlanır; image daha sonra da bellekte kalır.
İkinci Cubit ilk raster'ı bırakamaz. Aynı gate bile ikinci decode'u bu resident
image ile çakıştırabilir. Şimdiki ret, `getNextFrame()` image'ı yarattıktan sonra
`activeImages > maxOwnedImages` kontrolünde oluşur. İkinci image `finally` ile
bırakılır; iki kalıcı loaded image üretildiği iddia edilmez. Boşluk geçici native
allocation'ın önlenmemesidir. Yeni decode'un peak hesabı önceki resident image'ı içermez.

DI Cubit'i factory, gate'i singleton üretir. İki bağımsız sahip teknik olarak
oluşturulabilir. Ancak mevcut QA girişinde comparison veya consumer parent açılır;
parent child detail/reader rotalarına tek Cubit verir. Normal Library router'ı
reader'a bağlı değildir. Tek-parent kullanıcı yolunda iki resident Cubit çakışması
kanıtlanmış değildir. Bu dilim genel bellek sorununun veya cihaz kapanmasının
teşhisi olarak sunulamaz.

## Kod haritası

Satırlar yukarıdaki başlangıç HEAD'ine aittir; uygulamada sembolle doğrulanır.

| Yol / satır | Yeniden kullanım veya salt okunur sınır |
|---|---|
| `app/mobile/lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart:10–26` | Native handle sahibi, global QA sayaçları ve idempotent dispose. Değişikliğin tek runtime dosyası. |
| Aynı dosya `:39–105` | Buffer → descriptor → codec → image; her await sonrası cancellation, metadata/piksel/peak denetimi ve finally cleanup korunur. Geç owned-image kontrolü `:75–79`. |
| Aynı dosya `:125–140` | Painter borrowing yapar; klon üretmez. Disposed raster boyanmaz. |
| `app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart:208–279` | Selector ve doğrulanmış byte sonrası decode; state'e devir, stale completion cleanup. Salt okunur. |
| Aynı dosya `:48–78,357–394,400–403` | Emeklilik/choice/visibility, P4b1 ortak close/drain ve seri gate. Yeniden yazılmaz. |
| `app/mobile/lib/app/reading_image_comparison_configuration.dart:5–55` | Zorunlu mevcut `maxOwnedImages == 1`; sayılar açık run girdisidir. Yeni limit/varsayılan eklenmez. |
| `app/mobile/lib/app/reading_image_comparison_injection.config.dart:35–63` | Cubit factory ve singleton gate. Annotation/DI değişikliği gerekmez. |
| `app/mobile/lib/app/reading_image_consumer_qa/flow_screen.dart:29–79` | Tek parent/Cubit; close await ve lifecycle sahipliği korunur. |
| `app/mobile/lib/app/reading_image_consumer_qa/consumer_image_slot.dart:42–79` | Current route, epoch ve binding seçimi/paint kapısı korunur. |
| `app/mobile/test/reading_images/reading_image_comparison_raster_test.dart:62–130` | Gerçek PNG, metadata/bozuk byte/iptal, double-dispose ve native cleanup temeli. |
| `app/mobile/test/reading_images/reading_image_comparison_loaded_test.dart:17–120` | `png`, `NativeRepository`, `loaded`: iki gerçek Cubit için ağsız native kanıt. |
| `app/mobile/test/reading_images/reading_image_consumer_qa_test.dart:55–217,581–810,956–1043,1217–1280` | Gerçek loopback + native gate, parent kapanışı, account/background/lease/status/LIVE, eski iş ve P4b1 status drain regresyonları. |

## Seçilen dar yaklaşım

Rezervasyon mevcut raster dosyasında private, aynı isolate kapsamlı tutulur.
Mevcut statik native sayaçlarla aynı sahiplik alanıdır. Gate'e taşımak doğrudan
decode çağrılarını ve farklı gate örneklerini kaçırır; yeni DI/service katmanı gerekmez.

- Pozitif hedef ve mevcut encoded/geometri/resource kontrolleri ardından, ilk
  `ImmutableBuffer.fromUint8List` çağrısından önce ve await olmadan tek slot ile
  `BigInt(width) × height × 4` tahmini çıktı byte'ı ayrılır. Taşma kontrolü korunur.
- Slot pending native iş boyunca ve başarılı image bellekte kaldığı sürece doludur.
  Başarıda rezervasyon raster'a devredilir. Hata/iptalde native await ve finally
  cleanup bitmeden boşalmaz; başarılı raster'da yalnız gerçek dispose bırakır.
- Token yalnız kendi tutarını bir kez bırakır. Başarısız başvuru veya tekrar dispose
  başka sahibin rezervasyonunu azaltamaz. Yeni run/config dolu slotu sıfırlamaz.
- Dolu slotta ret mevcut Cubit `media_refused` yoluna gider. İlk owner korunur.
  Bekleme, otomatik tekrar, farklı sahibin image'ını atma veya yeni hata UI'ı eklenmez.
- Mevcut tek sahip sınırı değişmez. Slot boşalınca yeni owner kendi mevcut
  `maxDecodedBytes` ve diğer limitleriyle değerlendirilir. Sabit process-wide byte
  tavanı, çok-owner bütçe politikası veya yeni üretim profili seçilmez.
- Descriptor'dan okunan gerçek boyut, output, frame, süre, piksel ve peak kontrolleri
  kaldırılmaz. Rezervasyon manifestin doğru olduğunu varsayan bir decode kanıtı değildir.

Bu yalnız tahmini decoded çıktı sahipliğinin ön korumasıdır. İkinci HTTP payload'ı
ret öncesinde alınmış olabilir; encoded transfer/buffer toplamını veya RSS/GPU,
renderer tutulumu ve fiziksel transient peak'i sınırladığı iddia edilmez.

## Kanıtın ayırt etmesi gereken durumlar

Native buffer çağrısından hemen önce artan private sayaç için salt okunur
`bufferCreationAttempts` gözlemi eklenir. Pending/live rezervasyon ve tahmini byte
gözlemleri de salt okunur olur. Reset veya mutasyon API'si eklenmez. Testler önce/sonra
farkını kullanır. Yalnız final `activeImages == 1` eski geç kontrolü de geçirir;
ikinci ret sırasında native başlangıç farkı sıfır olmalıdır.

1. Önce gerçek PNG ve yeni gözlemle eski geç kontrolü gösteren başarısız yerel test
   kaydı alınır; sonra ön rezervasyon uygulanır. İki Cubit aynı gate ile ilk loaded
   owner'ı korurken ikinci decode native başlamadan reddedilir. Farklı gate ve
   doğrudan decoder çağrıları da aynı alanı paylaşır.
2. İlk decode Future'ı başlatılır başlatılmaz, ilk await tamamlanmadan ikinci
   başvuru reddedilir. İlk iptal bildirilse bile gerçek cleanup tamamlanmadan slot
   boşalmamalıdır. Test için yeni native scheduler veya geciktirme katmanı eklenmez.
3. Başarıda output byte'ı resident kalır. İlk raster bırakıldıktan sonra ikinci
   owner ancak açık yeni seçim/readmission ile yüklenir; sessiz tekrar yoktur.
4. Bozuk encoded, metadata uyuşmazlığı, geçersiz/taşan hedef, önceden/geç iptal,
   yanlış veya daha büyük run/limit ile başvuru sınanır. Hata kendi rezervasyonunu
   temizler; başarısız ikinci başvuru ilk owner'ı değiştirmez. Double-dispose,
   özellikle başka owner sonradan slotu aldığında, onun rezervasyonunu bırakamaz.
5. Gerçek loopback → repository → iki Cubit → native decode testinde ilk image
   açık kalır, ikinci byte teslimatı gözlenir, ikinci native başlangıç farkı sıfırdır.
   Native cleanup ve açık yeni seçim sonrası başarı ayrı doğrulanır. Bu Firebase/storage
   veya gerçek backend deneyi değildir.
6. Mevcut account/background/resume/lease/status/LIVE/choice/close testlerine
   rezervasyon assert'leri eklenir. Sonlanmada native sayaçlar ile reserved slot/byte
   sıfır olur; pending cancellation sırasında erken sıfırlanmaz. Eski epoch boyanmaz.
   P4b1 status+medya drain testleri korunur. Gerçek ses oynatma test edilmiş sayılmaz.

Mutasyonlar özel test kopyasında ayrı loglanır: rezervasyonu kaldırma veya ilk
native çağrı sonrasına geciktirme; başarılı raster'da erken release; hata cleanup
release'ini kaldırma; dispose release'ini kaldırma/iki kez çalıştırma; dolu slotu
yeni config ile sıfırlama. Her biri ilgili assertion'dan kırılmalı; son kaynak hash'i
geri alınmalıdır. Başarısız kayıtlar silinmez veya üzerlerine yazılmaz.

## Yerel doğrulama ve koruma

Somut plan onayına şu gerekli yerel kopya kapsamı dahildir:
`/Volumes/MAC_EK/tellpal-qa-private/p4b2-raster-reservation-20261018-6c37af4/`.
Yalnız izlenen gerekli Flutter kaynak/test/fixture/paket varlıkları ve sentetik
pinned-TLS testinin mevcut Python modülleri kopyalanır. Mevcut SDK, paket cache'i
ve interpreter yeniden kullanılır; paket/SDK/venv kurulmaz veya indirilmez.
Credential, token, `.env`, gerçek servis config'i ve ilgisiz untracked işler alınmaz.
Korunan dirty `main_image_experiment.dart` yerine özel kopyada açıkça kaydedilen
HEAD sürümü kullanılabilir. Bu istisna ve dependency kapanışı hash makbuzunda yazılır;
bütün dirty/untracked ağacın test edildiği söylenmez.

Dizinler `0700`, yeni log/makbuzlar `0600` ve CREATE_NEW olur. Uygulama öncesi repo,
eski `be/var`, P4a5/P4b1 kanıt dosyaları ve frozen block hash'leri mühürlenir; yalnız
spec'teki runtime/test/kanıt dosyaları değişebilir. Bu inceleme kaydı read-only kalır.
`.memlog.md`, `main_image_experiment.dart`, untracked local-lossless işleri ve eski
başarısız denemeler korunur. Süreç durdurma yoktur.

Özel kopyada `flutter test --no-pub test/reading_images test/story_detail`, izlenen
tam Flutter test kümesi ve `flutter analyze --no-pub` geçer. Dar küme tam kümenin
alt kümesidir; sayılar toplanmaz. İlk başarısız kanıt, mutasyon ve final kaynak
dönemleri ayrılır. Kaynak eşitliği, korunan hash'ler ve `git diff --check` doğrulanır.
Üç bağımsız BMad incelemesi, bulgu kapanışları ve yalnız görev dosyalarının yerel
commit'i aynı iş akışındadır. Push/PR yayını yoktur.

## Açık kalan kapılar

Normal Library/detail/PoC reader, ses, provider, cache, prefetch ve erişim kuralları
değişmez. EXT-MEDIA/EXT-PUBLICATION, URL yenileme, HTTP cache, session/retention,
çok-owner scheduler ve üretim sayısal profili kararları ayrıdır. Firebase giriş,
backend/DB, ADB/APK/cihaz, yeni encoder/üretim/upload, fatura sorgusu, dağıtım veya
silme bu plana dahil değildir. Fiziksel Android/iPhone kalite ve peak/GPU kanıtı
üretilmez. P0 ham kanıt sınırlaması ve JPEG/WebP bulanıklığı açık kalır.
