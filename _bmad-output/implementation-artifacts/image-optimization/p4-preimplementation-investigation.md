# P4 başlangıç incelemesi

Tarih: 2026-10-17, Europe/Istanbul. Başlangıç kaydı: salt okunur araştırma tamamlandı. Kullanıcının sonraki **A** yanıtı yalnız P4a1 hazırlık kodunu onayladı. Bu dilim uygulandı ve [kanıt](../../../be/docs/evidence/p4-reading-images/verification.md) ile [inceleme kapanışı](../../../be/docs/evidence/p4-reading-images/review.md) kaydedildi. P4a2/P4b ürün kararları açıktır. Bu belge yeni erişim, üretim profili veya bulut çalıştırması yetkisi değildir.

## Doğrulanan başlangıç

- Branch ve HEAD: `codex/image-optimization-p3-publication`, `79b6cf4e64da7a41efc9185d4c003c412c0d63fe`. Kullanıcının verdiği P3 commit'i HEAD'dir.
- Başlangıçtaki tek değişiklik `_bmad-output/party-mode/memories/installed/.memlog.md`. Korundu; içeriği düzenlenmedi.
- CMS: PID `55056`, çalışma dizini `cms/`, Node 22; `[::1]:5173` dinleniyor. `8080` dinleyicisi yok. Java PID `49929` IntelliJ çalışma dizininde, `99534` IntelliJ derleme sunucusunda. Süreç durdurulmadı veya backend yeniden başlatılmadı.
- `adb devices -l`: bağlı cihaz yok. `xcrun xctrace list devices`: fiziksel iPhone çevrimdışı; iOS simülatörleri kayıtlı. Simülatör çalıştırılmadı. İlk sandbox denemeleri erişim nedeniyle başarısızdı; izinli salt okunur tekrarlar bu sonuçları verdi. Fiziksel cihaz kalite, byte veya bellek kanıtı yok.
- BMad iş akışı bir kez render edildi. Step 1 ve Step 2 izlendi. Kullanıcının mevcut değişiklikleri koruyarak ilerleme talebi Git'teki kirli durum için geçerlidir; `.memlog.md` temizlenmedi. Model ayarı oturumdan devralındı. Step 2'nin istediği derin araştırma iki salt okunur alt görevle yapıldı.

Zorunlu P1/P2/P3 belgeleri, parent SPEC/plan/client contract, P3 CMS kanıtları, ADR-0018 ve Flutter skill'inin mimari, performans, navigation ve DI referansları incelendi. Depoda ek `project-context.md` bulunmadı.

## Kodun gerçekten sağladığı sınırlar

| Alan | Kanıt | P4 sonucu |
|---|---|---|
| P3 LIVE | `be/src/main/java/com/tellpal/v2/content/api/ReadingPublicationApi.java:3`; `content/application/publication/ReadingPublicationService.java:50` | Durum ve revision/digest vardır. Teslim API'si, kullanıcı entitlement veya session hakkı yoktur. |
| P2 manifest | `be/src/main/java/com/tellpal/v2/asset/domain/reading/ReadingWork.java:63`; `asset/api/ReadingImagePreparationApi.java:47` | Rol, sayfa, kaynak SHA, profil, rendition, pinned nesne sürümü, MIME/byte/SHA/boyut taşınır. Rol/bağ başına tek çıktı vardır; hazır varyant merdiveni yoktur. |
| Doğrulama | `be/src/main/java/com/tellpal/v2/asset/application/reading/ReadingImagePreparationService.java:109`; `asset/api/ReadingPublicationReadyApi.java:6` | Fresh pinned byte doğrulaması ile DB uygunluk kontrolü ayrıdır. Status yanıtı tek başına fresh byte kanıtı değildir. |
| Eski tester | `be/src/main/java/com/tellpal/v2/user/web/mobile/TestStoryPageMediaController.java:68` | V32 deney manifesti; P3 LIVE veya abonelik kontrolü sağlamaz. P3 çıktısı yerine kullanılamaz. |
| Library/detail erişimi | `be/src/main/java/com/tellpal/v2/user/application/LibraryHomeTestQueryService.java:20`; `asset/application/StoryDetailHeroQueryService.java:49` | Firebase allowlist ve server subscriber fixture vardır. Metadata/kilit sunumu reader medya izni değildir; mevcut Hero original sunar. |
| Modül sınırı | `be/src/main/java/com/tellpal/v2/user/package-info.java:1` | `user → content` izni yoktur. Sonraki teslim akışı mevcut `user → category → content/asset` public API sınırlarını korumalıdır. |
| İmzalama | `be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/FirebaseStorageAssetClient.java:44` | Eski signer path alır; generation pinlemez. P3 output'ları registry asset'i değildir. Eski registry URL yenilemesi doğrudan kullanılamaz. |
| Normal public tüketim | `be/src/main/java/com/tellpal/v2/content/application/query/PublicContentQueryService.java:143` | Eski editorial/processing görünürlüğü ve localized original bağlantıları devam eder. P4 yeni hak çıkaramaz. |
| Mobil girişler | `app/mobile/lib/app/router.dart:20`; `features/story_detail/presentation/view/story_detail_view.dart:298` | Library/detail router'ında reader yok; CTA pasif. Ayrı `StoryPlayerPage` PoC vardır. Birleşik çalışan Library→detail→reader akışı varsayılmaz. |
| Mobil bağımlılıklar | `app/mobile/pubspec.yaml` | Firebase mobil SDK kurulmamış. Backend mevcut Firebase/GCS SDK'sını kullanır; istemci bellekte token ve signed URL kullanır. Yeni SDK/transport katmanı sırf bu nedenle eklenmez. |

## P4 dilimleri

### P4a1: karara bağlı olmayan hazırlık

[Taslak spec](../spec-p4a-reading-image-selection.md) yalnız saf Dart manifest bağlamı doğrulaması ve iki eksenli seçim kuralını kapsar. Ağ, imzalama, cache ve normal UI değişmez. Bu sonuç mobil geçişin veya gerçek hikâye kabulünün tamamlanması değildir.

Geometri girdileri gerçek widget alanı, DPR, `contain`/`cover` ve kaynak boyutlarıdır. Fiziksel alan önce yukarı yuvarlanır. Kaynağın oranıyla contain için küçük, cover için büyük ölçek alınır. Rendition kabulünde ihtiyaç kaynak boyutuna kırpılmaz; decode ayrı ve kaynak sınırında hesaplanır. İki ekseni karşılayan, açıkça kabul edilmiş profil kümesindeki en küçük piksel alanı seçilir; eşitlikte byte, sonra rendition kimliği kullanılır. Bu sıralama byte tasarrufu veya kalite garantisi değildir. Oran yuvarlaması P2 floor-fit kuralıyla sınanır; bağımsız iki boyutla esnetme yapılmaz.

Mevcut `RasterCase.geometry` (`app/mobile/lib/features/image_experiment/domain/entity/raster_case.dart:41`) hesap örneğidir; deneyin sayısal sınırları taşınmaz. `StoryDetailHero` (`features/story_detail/presentation/widgets/story_detail_hero.dart:17`) hâlen phone/tablet decode tavanı kullanır; ilk hazırlık bunu değiştirmez.

### P4a2: ilk gerçek karşılaştırma ve teslimat

Önerilen hedef tek açıkça seçilmiş STORY/dil üzerinde ayrı local/test girişidir. Orijinal, açıkça etiketli kontrol koludur; adayın hatasında fallback değildir. Detail `cover`, reader `contain` kullanarak aynı kaynak ve aynı widget alanında sırayla gösterilir. Kaynak ve tüm sıralı sayfalar ön incelemeden geçmeden profil sayıları seçilmez. Mevcut gerçek kaynaklar yerinde yeniden bağlanmaz; gerekiyorsa ayrıca onaylı deneme kopyası hazırlanır.

P3 eligible LIVE, kaynak/profil ve tam manifest kimliği public modül API'leri üzerinden bağlanmalıdır. Yalnız P2 READY adayını sunmak LIVE teslimatı sayılmaz. Manifestteki kaynak/çıktı object generation, byte/SHA/MIME ve boyutlar korunmalıdır. Yenileme, aynı revision/digest/rendition için yeni transport denemesi üretmelidir; yeni kimlik otomatik olarak eskisinin yerine konmaz. Nesne sürümü değişirse teslim reddedilir.

Reader için `app/mobile/lib/story_reader_images.dart:5` seam'i ve `story_player_page.dart:380` gerçek LayoutBuilder/DPR ölçümü kullanılabilir. `reader_resolution_images.dart:99` aynı final provider'ın prepare/display paylaşımını gösterir; sentetik deney sınıfı genel teslimat olarak açılmaz. Yeni screen/view, Freezed Cubit, domain use case/repository, DTO ve injectable/auto_route kaydı ADR-0018'e uyar. Ses altyapısı yeniden yazılmaz; görsel denemesi otomatik ses başlatmaz.

Bu dilim P4a1'in kodlamasından bağımsız ürün kararlarına bağlıdır. Henüz uygulama onayı veya çalışan giriş yoktur.

### P4b: kaynak kullanımı ve yaşam döngüsü

| Konu | Mevcut durum | Ayrı işin kabulü |
|---|---|---|
| Görünür önceliği | Library admission FIFO; aktif transfer sınırları cache'e özeldir. | Görünür iş önce; eski spekülatif kuyruk bırakılır. Ortak transfer/decode/decoded-byte bütçeleri ölçüm ve açık seçim ister. |
| Bellek | 48-entry/32 MiB global LRU bir kontroldür; live/pending ve toplam process belleği değildir. | Mevcut tavanlar artırılmaz. Decode sırasında ve görünür sahiplerde kaynak sınırı uygulanır; yalnız cache sayacıyla güvenlik iddia edilmez. |
| Provider | Deney adapter'ı aynı bounded provider'ı paylaşır; normal PoC yolu bunu genel olarak kanıtlamaz. | Precache/display aynı final provider/geometry; hata gözlenir. Future tamamlanması retention/paint kanıtı değildir. |
| İptal | Generation kontrolü UI'ı korur; transport/decode iptalini kanıtlamaz. | Hızlı sayfa/route değişiminde socket settlement, listener/image clone bırakma ve eski işin promotion yapamaması ölçülür. |
| Hesap ve ekran ömrü | Dev token/context sabit; genel account-switch akışı yok. | Composition owner kaynakları kapatır; eski account/session sonucu kullanılamaz. Route kapanışı uygulama ses oturumunu kapatmaz. |
| Arka plan/geri dönüş | Deneyde detached kapanışı var; pause/resume politikası eksik. | Geri dönüşte admission/URL tekrar kontrolü; eski sonuçların ekrana kurulamaması; sesin mevcut kurallarının korunması. |
| Cache/expiry | Disk cache mevcut; hazır memory-only/no-store görsel yolu bulunmadı. | Seçilen HTTP politikası gerçekten uygulanır. ETag/SHA veya URL'nin varlığı yetki yerine geçmez. |

Bir/iki decode ve current/current+next kolları yalnız deney seçenekleridir. Burada hiçbir concurrency, preload, RAM veya TTL değeri onaylanmadı. P4a2 de kendi çalıştırılması için gerekli asgari iptal/teardown korumalarını erteleyemez; P4b kapsamının ayrılması sınırsız denemeye izin vermez.

## Açık ürün kararları ve seçenekler

| Kapı | Önerilen sınırlı seçenek | Alternatif ve sonucu |
|---|---|---|
| Gerçek hikâye | TR71'in kapak ve tüm sayfalarını önce salt okunur incelemek; önceki sayfa-1 gözlemleri yalnız geçmiş kanıttır. | TR19 veya kullanıcının verdiği başka kimlik/dil. #93 gerçek kalite kaynağı değildir. Henüz seçim yapılmadı. |
| Erişim | Ayrı local/test girişi, varsayılan kapalı, seçilmiş tek hikâye/dil ve mevcut QA UID; kilitli reader isteğini reddetmek. | Normal üretim geçişi için authoritative entitlement, preview ve reader hakları önce tanımlanır. Server fixture gerçek abonelik sayılmaz. |
| Medya teslimi | Mevcut Firebase SDK'sıyla sürümü sabitlenmiş kısa ömürlü URL; istemci gerçek byte/SHA/MIME/boyut/decode kontrolü. SDK ve gerçek HTTP davranışı ayrıca doğrulanır. | Aynı SDK üzerinden authenticated backend streaming; ek backend transfer yükü ve cancellation kanıtı gerekir. Yeni storage process/IPC veya retry katmanı gerekmez. Henüz yöntem seçilmedi. |
| HTTP cache | İlk kontrollü denemede disk/offline yeniden kullanımını kapatan açık politika; bunun için mevcut disk pipeline'ına ayrıca uyum gerekir. | `private,no-cache` ile her yeniden kullanımda doğrulama. Daha uzun disk/offline izinleri ayrıca entitlement/session kararı ister. `private`, `no-cache`, `no-store` birbirinin yerine kullanılamaz. |
| URL yenileme/session | Süre dolunca güncel admission ve aynı LIVE kimliğini kontrol et; LIVE değişmişse karşılaştırmayı durdurup açık yeniden açma iste. | Eski revision için retained-session yenileme; session bağı, revoke ve retention sözleşmesi gerekir. Eski TTL varsayılanı taşınmaz. |
| Retention | İlk dilimde cleanup/silme yok; yeni offline/session hakkı yok. | Süre, revoke ve aktif referans koruması ayrıca seçilir. Saklanan nesne tek başına erişim izni değildir. |
| Profil | Desteklenen opak RGB8 sRGB kaynaklar ve doğrudan orijinalden PNG; sayılar gerçek kaynak/slot/DPR ön incelemesinden sonra açık seçilir. | JPEG/WebP ayrı keskinlik/doğrulama çalışması ister. Growth ve kaynak yeterliliği kabulü profil bazında yapılır. Üretim profili yoktur. |
| Üretim | EXT-MEDIA, EXT-PUBLICATION ve entitlement/session/cache kapıları kapalı kalır. | Yaygınlaştırma, backfill veya dağıtım ayrı somut kapsam ve onay gerektirir. |

Kullanıcıya hikâye ön incelemesi ve ayrı local/test giriş tercihi soruldu. Yanıt gelmeden seçim yapılmış sayılmaz. Profil sayıları ve bulut çalıştırması bu soruların kapsamı değildir. 10 USD sınırı kaldırılmıştır; bütçe/fatura sorgulanmadı.

## Anlamlı doğrulama planı

1. Saf manifest testleri: content/dil/experience/revision/digest/job bağlamı; eksik, duplicate veya sırasız sayfa; yanlış rol/profil/kaynak; bozuk hash/byte/MIME/boyut ve floor-fit uyumsuzluğu. Descriptor doğrulaması gerçek dosya kontrolü diye raporlanmaz.
2. Saf seçim testleri: contain/cover, tall/wide, fractional DPR, rotation/window yeniden hesabı, iki eksenden birinde eksik aday, kaynak yetersizliği ve decode/resource reddi. Sabit küçük matris ile bağımsız brute-force seçim oracle'ı karşılaştırılır; orijinale düşen yol bulunmaz.
3. P4a2'de gerçek PostgreSQL/encoder/pinned depolama + HTTP/auth testleri: eligibility ve imzalama arasında nesil/availability değişimi; yanlış kullanıcı, kilit, dil, revision; stale yenileme; missing/corrupt/version değişimi; HTTP cache ve expiry. İmzalama DB transaction dışında kalır. Backend değişirse `bash ./mvnw verify` ve modül sınırları çalıştırılır.
4. Flutter generation/analyze ve ilgili regresyonlar; gerçek provider/decode, iptal ve byte kontrolü. Yeni iş Library/detail geri dönüş, scroll, narration tercihi ve mevcut cache davranışlarını bozmaz.
5. Seçilmiş gerçek hikâyede aynı slotta orijinal/aday kontrolü. Android ve iPhone için seçilen rendition/profile/revision, gerçek aktarılan byte, decode boyutu, görünen sonuç, cold/warm durumu, peak/settled process/native kaynaklar ve return-to-idle kaydı tutulur. Üretilemeyen sayaç/cihaz gözlemi `unavailable` kalır. Frame callback fiziksel sunum diye etiketlenmez.

Bu incelemede uygulama testi veya gerçek cihaz denemesi çalıştırılmadı. Cihaz/süre/altyapı yokluğu PASS sayılmaz. Yeni bulut üretimi, auth session oluşturma, DB yazısı, kaynak kabulü, seed, LIVE değişimi, legacy poller açılması, backfill ve silme yapılmadı.

## Korunan kalite sınırları

Android'deki 0,75× yazıları bulanıklaştırdı; 1× için kullanıcı “net” dedi. iPhone'da gösterilen bazı 0,75× PNG'ler kabul edildi. Bu gözlemler gösterilen örnek/geometrilere aittir; üretim ayarı veya tüm cihazlar için alt sınır değildir. JPEG/WebP keskinliği çözülmedi. P0'ın eksik ham native ledger/build/cleanup kanıtı sınırlaması sürer. #93'ün 80×60 → 40×30 sentetik PNG sonucu bu boşluğu kapatmaz. Legacy `asset_processing #112` kaydı ve kapalı poller korunur.
