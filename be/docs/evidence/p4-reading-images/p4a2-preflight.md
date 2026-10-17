# P4a2 ön inceleme ve karar kapıları

Tarih: 2026-10-17, Europe/Istanbul. Başlangıç commit'i: `39fee0c9106cd908f1aaade41545cdf858a36237`.
Durum: TR71 kaynak incelemesi ve dar teslimat yaklaşımı kullanıcı tarafından seçildi. Canonical PNG adayları ve yalnız QA kopyasının ücretsiz erişimi onaylıdır. Gerçek kaynak kabulü, fixture kimliği, sayısal profil ve bulut çalışması henüz oluşturulmadı. P4a1 tamamlandı; bu belge P4a2 uygulama veya gerçek cihaz kabulü değildir.

## Korunan çalışma durumu

Branch `codex/image-optimization-p3-publication`. Başlangıçtaki tek değişiklik ilgisiz `.memlog.md`; düzenlenmedi. CMS PID `55056`, `5173` portunda dinliyor. `8080` dinleyicisi görülmedi. Süreç durdurma veya backend başlatma yapılmadı. P4a1'in 392 test sonucu geçmiş kanıttır; bu ön incelemede uygulama testi yeniden çalıştırılmadı.

## Gerçek kaynak için yeni bulgu

Mevcut [P0 corpus kaydı](../../../../_bmad-output/implementation-artifacts/image-optimization/p0a-corpus.json) aşağıdaki eski gözlemleri içeriyor:

| Kayıt | Kaynak | Kayıtlı boyut / byte | Metadata |
|---|---|---|---|
| TR71 sayfa 1 | asset `9816`, JPEG | 1712×2778 / 1.122.418 | EXIF var; orientation kaydı yok; ICC yok. |
| TR71 okuma kapağı | asset `9778`, JPEG | 1712×2778 / 1.048.225 | EXIF var; orientation kaydı yok; ICC yok. |
| TR19 sayfa 1 | asset `2037`, JPEG | 1284×2279 / 847.415 | EXIF ve RGB ICC var. |

Bunlar güncel kaynak byte'larının yeniden doğrulanması değildir. Envanterde TR71 için iki, TR19 için bir Türkçe binding bulunuyor; bu sayılar hikâyelerin gerçek toplam sayfa sayısı değildir. Güncel tam dil/sayfa snapshot'ı ayrıca alınmalıdır.

`ReadingImageInspector.jpeg` yalnız JFIF APP0 metadata'sını kabul ediyor; diğer APP segmentlerini `JPEG_UNSUPPORTED_METADATA_OR_ORIENTATION` ile reddediyor. Orientation alanının boş olması EXIF segmentinin yokluğu anlamına gelmiyor. Kayıtlı kaynaklar değişmediyse mevcut dar P2 kabulüne doğrudan uymamaları beklenir. Bu, kod ve geçmiş metadata'dan yapılan çıkarımdır; yeni byte denemesi değildir.

Sonuç: seçilen hikâyenin kapak ve tüm sayfaları önce yeniden incelenmeli. Uyumsuzluk varsa açık engel olarak sunulmalı. Metadata sessizce silinmez, eski normalize edilmiş P0 referansı kendiliğinden yeni original sayılmaz ve P2 kabul politikası genişletilmez. Kullanıcı isterse mevcut doğrulanmış canonical PNG'nin ayrı deneme kopyasında kaynak seçilmesi veya P2 normalizasyonunun ayrı çalışılması somut alternatiflerdir. Her ikisi de açık kaynak kararı gerektirir; orijinaller korunur.

## Teslimat ve HTTP cache için yeni bulgu

Depoda kullanılan `google-cloud-storage` sürümü `2.58.1`. Kurulu SDK kaynaklarında `Storage.SignUrlOption.withQueryParams` ve `withV4Signature` mevcut. V4 sorgu parametrelerini imzaya bağlar; XML API'nin `generation` parametresi belirli nesne sürümünü seçer. Mevcut TellPal signer'ı yalnız path imzalıyor; bu yetenek henüz P4'e bağlanmadı. [Google XML GET Object](https://docs.cloud.google.com/storage/docs/xml-api/get-object-download)

Signed URL, geçerliliği boyunca URL'yi elinde tutan kişiye erişim verir; hesap değişiminde anlık revoke kanıtı değildir. Signed URL'ler XML API kullanır. [Google signed URL sözleşmesi](https://docs.cloud.google.com/storage/docs/access-control/signed-urls)

Cloud Storage, yanıt `Cache-Control` değerini saklanan nesne metadata'sından alır; istek başlığı bunu değiştirmez. TellPal `createImmutable` şu an yalnız MIME ayarlıyor, açık cache politikası yazmıyor. Gerçek nesne başlıkları okunmadı; bunların `no-store` olduğu söylenemez. İstemci disk cache'ini kapatmak tek başına HTTP politikasını kanıtlamaz. [Google nesne metadata sözleşmesi](https://docs.cloud.google.com/storage/docs/metadata#cache-control)

| Seçenek | Gereken somut çalışma | Sınır |
|---|---|---|
| Önerilen ilk dar deneme: kimliği doğrulanmış backend teslimatı | Mevcut Firebase SDK `openVersion` ile pinned okuma; her istekte admission ve aynı LIVE kontrolü; açık `private,no-store` yanıt; istemcide diske yazmadan byte/SHA/MIME/decode doğrulaması. | Backend transfer kapasitesi ve istemci iptali sınanmalı. Yeni storage process/IPC/retry gerekmez. Bu bir öneridir. |
| Doğrudan sürümü sabitlenmiş signed URL | V4 + generation; kaynak ve çıktılarda gerçek cache metadata kanıtı; URL expiry ve hesap değişimi sınırlarının kabulü. | `no-store` için yeni deneme nesnelerinin metadata politikası ayrıca seçilmeli. Mevcut nesneler sessizce değiştirilmez. |

Mevcut CMS preview endpoint'i streaming için bir uygulama örneğidir; admin token'ı ve beş dakikalık private cache davranışı P4 mobil hakkına taşınamaz. `user → category → content/asset` public modül sınırları korunur.

## Kullanıcıdan beklenen dar seçim

1. TR71, TR19 veya başka bir content/dil için güncel kaynak ön incelemesi.
2. Ayrı local/test girişi, seçilen tek hikâye/dil ve mevcut QA UID; kilitli reader erişimi, disk/offline saklama ve retained-session yenileme olmaması. Süre dolunca güncel admission ve aynı LIVE tekrar kontrol edilir; LIVE değişmişse deneme durur. Alternatif, önce normal mobil erişim/session/cache sözleşmesini kararlaştırmaktır.

Bu bölüm ilk soru kaydıdır. Kullanıcı daha sonra TR71 ön incelemesini, ayrı local/test girişini ve kimliği doğrulanmış backend teslimatını seçti. Bu onay yeni ücretsiz içerik veya abonelik hakkı oluşturmaz. Teknik teslimat önerisi seçilen bu sınıra göre spec'te somutlaştırılır. Sayısal profil ve growth kararı gerçek kaynak/slot/DPR sonuçlarından sonra ayrıca sunulur. Profil seçilmeden bulutta yeni üretim başlamaz.

Bu belgenin ilk sürümünde bulut nesnesi okunmamıştı. Aşağıdaki ikinci aşamada yalnız kaynak okundu. DB, auth session, kaynak kabulü, seed, yayın veya poller durumu değiştirilmedi. Bütçe/fatura sorgulanmadı. Mevcut P0 kalite ve ham kanıt sınırlamaları, P4a2 cihaz doğrulaması ve P4b kaynak/yaşam döngüsü kapıları açık kalır.


## 2026-10-17: güncel TR71 kanıtı

Salt okunur yerel PostgreSQL snapshot'ında TR71 `Zümrüt Ormanı`, etkin `STORY`, Türkçe yerelleştirme `233`, `PUBLISHED`, legacy `PENDING` bulundu. Kapak ve sıralı **18 sayfa** eksiksiz bağlıdır. `defaultFree=false`; mevcut QA UID abonelik fixture'ında değildir. P3 scope ve kabul edilmiş kaynak sayısı sıfırdır. İnceleme sonrası tekrar alınan kaynak snapshot'ı aynıdır. İşlem `READ ONLY` idi; hiçbir kayıt değiştirilmedi.

Mevcut Firebase Storage SDK `2.58.1`, salt okunur OAuth kapsamı ve `generation` sabitlemesiyle 19 kaynak okundu. Toplam **17.423.573 byte**. Hepsinin byte sayısı ve SHA-256 değeri registry ile eşleşti. Hepsi `1712×2778`, tek kare, alfa içermeyen JPEG'dir. Gerçek P2 `ReadingImageInspector.jpeg` kontrolü 19 kaynağı da `JPEG_UNSUPPORTED_METADATA_OR_ORIENTATION` ile reddetti. Ayrı tam ImageIO decode başarılıdır; bu, P2 admission/prepare/publish çalıştırıldığı anlamına gelmez. Pillow kontrolünde 19/19 EXIF içerir, orientation alanı ve ICC yoktur.

Gizli alan içermeyen nesne bazlı sonuçlar: [tr71-source-preflight.json](tr71-source-preflight.json). Kaynaklar, diagnostic kodları ve özel snapshot `be/var/` altında, Git dışında ve özel izinlerle tutulur. Token, credential, object path ve özel URL bu rapora alınmadı. Okuma 20 MiB/nesne, 40 milyon piksel, sıralı decode ve 512 MiB JVM tanısal sınırlarıyla yapıldı. Dış watchdog 120 saniyeydi; bu sınırlar üretim profili veya SDK'nın kesin iptal süresi kanıtı değildir.

### Yerel canonical PNG adayları

Kapak `9778`, sayfa 1 `9816`, sayfa 2 `9818` için aynı çözünürlükte RGB PNG adayı üretildi. Resize/crop yapılmadı. Aynı Java decoder ile JPEG'den elde edilen RGB ve yeniden açılan PNG RGB SHA-256 değerleri eşittir. Bağımsız PNG incelemesinde yalnız IHDR/IDAT/IEND, RGB ve EXIF/ICC yokluğu görüldü. Bu sonuç cihazlar arası JPEG decode eşitliğini veya görsel kalite kabulünü kanıtlamaz.

| Kaynak asset | JPEG byte | PNG aday byte | RGB SHA-256 |
|---|---:|---:|---|
| 9778 | 1.048.225 | 6.336.857 | `0cbed689538eaddfb7b6d28940744c4abe3d472f3757382c6d9f8140b0f9e312` |
| 9816 | 1.122.418 | 5.941.567 | `8793385396fbb4f7351a9f42c7efb1b37d9a77afc31c9cddd4c94ff039766dae` |
| 9818 | 1.122.572 | 6.122.778 | `648e41568d28112cd87cc980d51e4d0d1f70a561851a69d829376f11efe0637c` |

Bunlar seçilmiş/admitted kaynak, P2 çıktısı veya bulut nesnesi değildir. PNG'ler yaklaşık 5–6 kat büyüktür. İleride karşılaştırma yapılırsa mevcut JPEG ve canonical PNG byte tabanları ayrı raporlanır. Canonical PNG kontrol kolu, cihazda gösterilmiş eski JPEG diye etiketlenmez.

### Sonraki somut öneri

TR71'i ve tüm 18 sayfayı koru. Kapak + ilk iki sayfadan ayrı bir yerel QA kopyası kullan; yalnız bu kopyanın ücretsiz test erişimi ayrıca onaylansın. Canonical adaylar ayrıca kaynak seçimi gerektirir. Kopya normal Library'ye eklenmez; karşılaştırma bağımsız local/test girişinden yapılır. Yapay kategori eklemek yerine `user → content::api` yönünde dar, döngüsüz bir bağımlılık ve ayrı ADR önerilir. Henüz mimari politika değiştirilmedi.

Bu onaylar kod kapsamını ve kaynak adayını belirleyebilir; bulut çalıştırmasını başlatmaz. Önce cihazdaki gerçek slot/DPR/fit ölçülür. Sayısal rendition, transfer/decode/bellek/deadline/status kontrol sınırları ve toplam bulut işlem/byte tavanı ayrı çalıştırma kaydında seçilir. Bu kayıt yoksa gerçek fixture/yayın/üretim çalışmaz. Üç kaynak ve rol başına tek çıktı varsayımında en çok 13 yeni nesne beklenir; bu bir tahmin ve öneridir, yetki değildir.

Fiziksel cihaz, gerçek bearer oturumu, görüntülenen rendition, HTTP iptali, decode/bellek ve kalite kanıtı henüz yoktur. P0 ham kanıt eksikliği, JPEG/WebP bulanıklığı ve genel üretim profili kapıları açıktır. P3 #93 sentetik sonuç ve legacy #112 `PENDING` durumu korunur. Bulut yazısı, kaynak kabulü, yeni QA içerik, yayın veya süreç durdurma yapılmadı.
