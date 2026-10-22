---
title: 'Görsel optimizasyonu kapanışı ve mobil Epic 1’e dönüş'
date: '2026-10-22'
status: accepted
workflow: bmad-correct-course
review_mode: batch
change_scope: moderate
implementation_authorized: true
production_operations_authorized: false
---

# Görsel optimizasyonu kapanışı ve mobil Epic 1’e dönüş

**Öneri: Ayrı görsel araştırmasını kapatalım. Kalan altyapı işini üç teslim
paketinde tamamlayıp Mobil Epic 1 / Story 1.3’e dönelim. Gerçek okuyucu
entegrasyonunu ve yayın kabulünü ilgili mobil story’lerin içinde bitirelim.**

Bu plan 2026-10-22 tarihinde kullanıcı tarafından kabul edildi. Paket 1 yerel geliştirme
kabulünü tamamladı; Paket 2 ve Paket 3 de yerel geliştirme kabulünü tamamladı. [Tek kapanış kaydı](../../be/docs/evidence/image-infrastructure-acceptance/README.md) gerçek sonucu ve mobil devri tutar. Planın
kabulü, canlıya yazma veya dağıtım izni değildir; mobil story durumları değişmedi.

## 1. Neden plan değişiyor?

Kullanıcı görsel optimizasyonunun çok sayıda küçük deney ve alt aşamaya
bölündüğünü, uygulama geliştirmesinden fazla zaman aldığını belirtti. Amaç,
aynı soruları yeniden sınamadan kalan gerçek ürün işini tamamlamaktır.

İki önemli ayrım var:

- **Dosya boyutu ile RAM aynı şey değildir.** Kayıpsız WebP indirme/depolama
  byte’ını azaltabilir. Aynı piksel boyutunda decode edilen görüntünün belleğini
  aynı oranda azaltmış sayılmaz. Ekrana uygun decode boyutu ve aynı anda
  tutulan görseller uygulama tarafında sınırlandırılmalıdır.
- **Normal okuyucu henüz geliştirilmedi.** Story 1.3 ilk sayfayı başlatır;
  Story 1.4 sayfa geçişlerini ekler. Tam okuyucunun kabulünü bu story’lere
  dönmenin önkoşulu yapmak döngü yaratır. Mevcut QA okuyucusu tam ürün okuyucusu
  sayılmayacaktır.

## 2. Nerede olduğumuz

| Alan | Kanıtlanan sonuç | Henüz kanıtlanmayan / yapılmayan |
|---|---|---|
| P0 — Deneyler | Sınırlı codec, geometri ve cihaz sonuçları var | Bütün cihaz/rol/kaynaklar için genel üretim profili yok |
| P1 — Sözleşme | Revizyon, doğrulama ve yayın sorumlulukları kabul edilmiş | Tek başına çalışan ürün veya üretim izni değildir |
| P2 — Hazırlama motoru | Dar local/test PNG/JPEG motoru uygulanmış | Seçilen gerçek renk/metadata ve WebP üretim yolu açık |
| P3 — Yayın akışı | Yerel hazırlama, retry ve uygun revizyonun benimsenmesi uygulanmış | Üretim etkinleştirmesi ve normal mobil tüketici geçişi yok |
| P4 — Tüketici | QA manifest/seçim/çizim ve kaynak yönetimi kodu var; dar emülatör akışı geçti | Normal okuyucu ve bütün yaşam döngüsü kabulü yok |
| P4b5 — Eski ölçüm hattı | Kullanıcı kararıyla kapatılmış | Teknik olarak PASS sayılmaz; kendiliğinden yeniden açılmayacak |
| P5a | Paylaşılan CSV’lerden canlı metadata envanteri çıkarılmış | Dosya varlığı/kalitesi ve geçiş uygunluğu bütün arşiv için kanıtlanmış değil |
| P5b–P5g | İki hikâye için kaynak, kayıpsız aday, çevrimdışı kalite ve iPhone ölçümleri; 41 dosyada yerel Java uyumluluğu | Hazır adayları kopyalamak yeni yayınlar için genel WebP üretimi değildir |

Mevcut tercih korunur: **Kocaman Bir Sarılma (#18): 17 JPEG aynen;
Pofuduk Mısır Pops (#62): 24 kayıpsız WebP adayı.** Bu karar diğer bütün
görseller için aynı formatın zorunlu olduğu anlamına gelmez.

P5g’nin 26 profil ve 12 normal profil testi geçti. Bu sayılar yeni üretim
testleri veya bütün optimizasyonun kapanışı olarak sunulmayacak.

Mobil tracker: **Story 1.1 `done`, Story 1.2 `review`, Story 1.3 `backlog`.**
Story 1.2 geliştirme doğrulaması tamam; insan kabulü/tracker devri ayrı kalır.
Bu plan onu kendiliğinden `done` yapmaz.

## 3. İki ayrı bitiş noktası

### A — Altyapı geliştirme kapanışı: mobil epic’e dönüş

Aşağıdaki üç paket tamamlanır. Tekrarlanan ayrı deney hattı kapanır. Backend
ve ortak mobil görsel katmanı, Story 1.3’ün tüketebileceği durumda teslim edilir.
Normal reader ekranı, anlatım sesi ve sayfa gezinmesi burada yeniden yazılmaz.

Bu noktanın doğru adı **“görsel altyapısı geliştirme kabulü”**dür. Henüz
**“bütün görseller üretimde optimize edildi”** denmez.

### B — Üretim kapanışı: uygulama entegrasyonu ve kontrollü geçiş

Gerçek okuyucu davranışı ilgili mobil story’lerde doğrulanır. Sonra onaylı
canlı pilot ve gereken mevcut içerik geçişi yapılır. Ancak bu sonuçlardan sonra
ilgili üretim kapsamı tamamlandı sayılır. Aşağıdaki 7. bölüm bu işleri sahipli
ve bitiş koşullu olarak tutar; gereksinimleri silmez.

**Önerilen dönüş noktası A’dır.** B’yi beklemek, henüz yazılmamış okuyucuyu
görsel araştırma dalında geliştirmek anlamına gelir. Kullanıcı B tamamlanmadan
dönmemeyi seçerse Story 1.3/1.4 ve gereken medya yaşam döngüsü işleri de önce
yapılmalıdır; bunlar aynı anda hem önkoşul hem sonraki iş olamaz.

## 4. Dönüşten önce kalan üç teslim paketi

### Paket 1 — Gerçek görsel hazırlama hattını tamamla

**Sonuç:** Uygun yeni yüklemeler ve iki pilot hikâyenin dosyaları, asıl
backend hattında doğrulanmış görsel çıktısına dönüşebilsin.

| İş | Yapılacak | Bitiş ölçütü |
|---|---|---|
| K1.1 | Her rol için kaynak kabulünü, çıktı formatını, boyutu ve eşit/büyüyen çıktı kuralını tek profilde tanımla. Mevcut küçük JPEG’i gereksiz yeniden kodlama. | Kopya yalnız açıkça kabul edilmiş kaynak/profil eşliğinde geçer; sessiz orijinal fallback yoktur. |
| K1.2 | Gerçek WebP üretimini ekle. P5g’deki reader, WebP writer yerine sayılmasın. Kullanılacak encoder bağımlılığı/runtime’ı ve metadata koruması bu işin tek teknik kararı olsun. | Geçici yeni PNG/JPEG yüklemesinden gerçek çıktı üretilir; yalnız P5c’den hazır WebP kopyalanmaz. Encoder yoksa iş tamamlanmış sayılmaz. |
| K1.3 | Kaynak admission, capture, child ve kaydedilmiş çıktı doğrulamasını JPEG/PNG/WebP ve kabul edilmiş ICC/XMP/TIFF davranışıyla uyumlu yap. | Pilotun tam RGB/renk/metadata eşliği korunur. Desteklenmeyen alpha, orientation, animasyon ve bozuk girdi açıkça reddedilir. |
| K1.4 | Sabit `.png`/`image/png` varsayımlarını çıktı profiline bağla; codec ve gerçek runtime’ı fingerprint’e kat. | Dosya yolu, MIME, byte, SHA, geometri ve manifest aynı gerçek çıktıyı tanımlar; eski profil sessizce yeniden kullanılmaz. |
| K1.5 | Gerçek deployment Java/Linux tabanını yerelde kullan; parent/child bellek, süre, disk, concurrency ve teardown kontrollerini aynı hatta uygula. | Paketli uygulama/runtime testi geçer. Sınır aşan iş ölür ve başarısız kalır; test-only macOS supervisor kanıtı production kodunun yerine sayılmaz. |

Ana yüzeyler: `be/pom.xml`, `ReadingProfiles`, `ReadingImageInspector`,
`ReadingSourceAdmissionService`, `ReadingImageChild`, `ReadingImageWorker`,
`ReadingImagePreparationService` ve mevcut süreç/storage adapter’ları.

Sayısal profil, boyut ve çalışma sınırları bu paketin başında mevcut slot ve
ölçüm kayıtlarıyla somutlaştırılır. P5g’nin 1712×2778 örnek geometrisi,
256 MiB heap/512 MiB RSS değerleri genel üretim profili diye kopyalanmaz.
Kaynak yetersizse upscale ile sahte kalite üretilmez.

**Sınır:** Üretim bağımlılığı veya mevcut SDK-only storage mimarisini değiştiren
bir çözüm eski P5g onayından türetilemez. Somut uygulama kapsamı içinde görünür
olmalıdır. Storage slow-trickle/authentication altında mutlak süre sınırı
açığı ayrıca sınanır; kanıtlanamazsa üretim engeli olarak kalır. Watchdog ile
deneyi durdurmak, gerçek storage çağrısının mutlak süre garantisi sayılmaz.

### Paket 2 — Uygulamanın kullanacağı görsel katmanını hazırla

**Sonuç:** Story 1.3, yeni bir medya deneyi kurmadan aynı doğrulanmış görsel
katmanını kullanabilsin.

| İş | Yapılacak | Bitiş ölçütü |
|---|---|---|
| K2.1 | Normal tüketici için gerekli görsel sözleşmesini tamamla: içerik/dil/rol, immutable revision/manifest/rendition, MIME/SHA/byte/boyut ve açık hata davranışı. | Yerel backend gerçek yanıtlarla doğrulanır; `/api/test/reading-image-comparison` normal teslimat sözleşmesi diye kullanılmaz. |
| K2.2 | Metadata görünürlüğü ile korumalı tam görsel erişimini ayır. Görsel erişimi, kullanıcı/revizyon kapsamlı cache ve yenileme kurallarının bu tüketiciye gereken bölümünü kesinleştir. | Yetkisiz, yanlış hesap/dil veya geçersiz revizyon isteği cache üzerinden de erişim kazanmaz; URL yenileme kimliği parçalamaz. |
| K2.3 | Var olan manifest/seçim/bütünlük/decode kodunu feature-first yapı içinde ürünün ortak görsel katmanına bağla. BLoC/Cubit, auto_route ve DI sahipliğini koru. | Story 1.3 için typed repository/use case/bileşen hazırdır; tam reader UI veya ikinci medya yöneticisi yazılmaz. |
| K2.4 | Mevcut detail kapak tüketicisinde kontrollü geliştirme entegrasyonu yap; page renderer’ını ürün katmanıyla doğrula. Slot×DPR, iki eksenli fit, sınırlı decode ve iptali uygula. | Doğru rendition çizilir; bozuk/yarım çıktı cache’e hazır diye girmez; ayrılma ve geç yanıt temizlenir. QA ekranı tam kullanıcı yolculuğu diye sunulmaz. |

Bu iş tam ses/oynatma/session sözleşmesini kapatmaz. Görsel alanları ve gerçek
görsel dosyaları hazırlanır; anlatım sesiyle ilk sayfa kabulü Story 1.3’te,
sayfa geçişleri Story 1.4’te birleşir. Genel paywall, hesap migration’ı veya
tam tester/CMS iş akışı bu pakete taşınmaz. Gerekli erişim kontrolü ise
sonraya bırakılmaz.

Ortak katman mevcut üretim istemcisinin hesap/cache ve disposal kurallarını
korur. QA’ya verilmiş özel bellek/renderer istisnası normal tüketiciye otomatik
aktarılmaz. Yeni bir cache mimarisi veya SDK gerekiyorsa bunun kapsamı ayrıca
görünür yapılır; korumalı görsellere varsayılan kalıcı cache uygulanmaz.

### Paket 3 — Tek entegrasyon kabulü ve mobil epic’e devir

**Sonuç:** Aynı kaynak ve tek kabul listesiyle geliştirme hattının çalıştığı
kanıtlansın; ayrı P5h/P5i deneme zinciri oluşmasın.

| İş | Yapılacak | Bitiş ölçütü |
|---|---|---|
| K3.1 | İki hikâyenin 41 görselini gerçek hazırlama hattından geçir; bir geçici yeni yükleme/yayın senaryosu da çalıştır. Yalnız yeni yerel çıktılara yaz. | JPEG kopyası byte olarak aynı; kayıpsız çıktılar piksel ve kabul edilmiş metadata olarak aynı; kaydedilmiş dosyaların tamamı yeniden doğrulanmış. |
| K3.2 | Yerel PostgreSQL ile admission → hazırlama → bağımsız reread → atomik READY → uygun güncel LIVE zincirini doğrula. | Yeni profil/formatlarla eksik-bozuk çıktı, retry, restart, eski attempt/profil ve başarısız replacement altında eski LIVE korunması geçer. Yerel LIVE, canlı yayın demek değildir. |
| K3.3 | Değişen kod için gerekli backend verify/Modulith, Flutter test/analyze ve Story 1.1/1.2 regresyonlarını bir kez çalıştır. CMS değiştiyse kendi build/test’i eklenir. | İlgili kontrollerde açıklanmamış hata veya skip yok; değişmeyen eski cihaz/codec karşılaştırmaları yeniden koşulmaz. |
| K3.4 | Yerel gerçek backend ile detail/görsel katmanı entegrasyonunu, gecikme ve sahiplik davranışını doğrula. | Kullanılan rendition, alınan byte, decode boyutu, çizim ve bırakma davranışı izlenebilir; debug/emülatör sonucu fiziksel cihaz performansı diye sunulmaz. |
| K3.5 | Tek kısa kabul kaydı çıkar; açık üretim işlerini aşağıdaki sahiplerine devret. Story 1.2’nin insan kabulü durumunu doğrula. | Altyapıda açık kritik iş yok; Story 1.3’e gerekli görsel API, fixture ve bileşen hazır; insan kabulü yoksa 1.2 tracker’ı sessizce `done` olmaz. |

**Çalıştırma ortamı:** Önce yalnız yerel dosyalar, yalıtılmış yerel depolama ve
yerel PostgreSQL. Firebase/production DB/storage kullanılmaz. Mevcut sentetik
cloud kanıtı yalnız kendi kapsamıyla korunur. Gerçek Firebase/cihaz çalışması
gerekiyorsa önce somut hedef ve yazma sınırı belirlenir; bu belge otomatik izin
vermez. Yerel depolama testi gerçek Firebase kanıtı diye adlandırılmaz.

**Yeniden kullanılacaklar:** P5c’nin pinli kaynak/adayı, P5e görsel kabulü,
P5f’nin mühürlü iPhone ölçümü, P5g’nin raw RGB/metadata ve olumsuz örnekleri,
P2/P3’ün işlem/lease/retry testleri, P4’ün seçim ve kaynak yönetimi kodu.
Yeni kod değişen davranış için sınanır; bütün geçmiş tekrar oynatılmaz.

## 5. Mobil epic’e dönüş kabul listesi

Bu liste plan anında yapılmış sayılmaz; uygulama sonunda doldurulacak.

- [x] Gerçek yeni görsel üretimi çalışıyor; yalnız hazır aday kopyası yok.
- [x] Kabul edilmiş JPEG/PNG/WebP, renk/metadata ve geometri davranışı gerçek hatta aynı.
- [x] Çıktı bütünlüğü, atomik yayın, eski sürümün korunması ve gerekli iptal/kaynak sınırları geçiyor.
- [x] Paketli hedef runtime yerelde doğrulanmış; normal tüketiciye gereken görsel sözleşmesi hazır.
- [x] Ortak mobil bileşen hazır; mevcut Library/detail davranışı bozulmamış.
- [x] Yerel geliştirme kabulünün kritik açıkları kapanmış; üretime özgü kalanlar sahipli ve görünür.
- [x] Story 1.2 insan devri sonuçlanmış veya açık durumu doğru kaydedilmiş.
- [x] Story 1.3’ün görsel bağımlılıkları somut; üretim onayı varsayılmamış.

Sonra **Mobil Epic 1 → Story 1.3: Seçilen hikâyenin ilk sayfasını başlat**
işine geçilir. İlk sayfa görseli/anlatımı, Ses tercihi, loading/retry ve ayrılma
iptali bu story’de yapılır. Ardından Story 1.4 sayfa geçişlerini tamamlar.

## 6. İşin tekrar uzamasını önleyen çalışma kuralı

- Üç teslim paketi tek bir iş listesinde izlenir; her olumsuz örnek yeni faz olmaz.
- Yeni format, benchmark aracı, QA uygulaması veya alternatif codec araştırması açılmaz.
- Desteklenen ilk kapsam STORY/READING’dir. GIF/alpha/animasyon, dinleme/ninni
  rolleri ve arşiv temizliği bu kapanışa eklenmez; desteklenmeyen kaynaklar açıkça reddedilir.
- Yeni test yalnız değişen kod, bulunan hata veya açık kabul koşulu için eklenir.
- Bir engel çıkarsa mevcut maddesi üzerinde çözülür. Farklı mimari veya kapsam
  gerekiyorsa tek somut karar olarak sunulur; sonsuz ön çalışma dizisi başlatılmaz.
- Her paket sonunda tek kısa sonuç: ne çalışıyor, ne kaldı, bitiş koşulu geçti mi?
- Kesin gün sözü verilmez. Göreli yük: Paket 1 yüksek, Paket 2 orta/yüksek,
  Paket 3 orta. Bunlar birer oturumluk küçük görev değildir. Encoder/runtime
  bağımlılığı Paket 1’in başında sonuçlandırılır; belirsizlik plan boyunca taşınmaz.

## 7. Epic’e döndükten sonra zorunlu kalan üretim işleri

Bunlar iptal edilmez. Mevcut mobil/release akışında tamamlanır; ayrı araştırma
kolu olarak geliştirmeyi sürekli durdurmaz.

| İş | Sahibi / sırası | Gerçek bitiş koşulu |
|---|---|---|
| Normal reader ilk sayfa ve sayfa geçişleri | Mobil Epic 1, Story 1.3 → 1.4 | Library → detail → yetkili başlangıç → doğru dil/revizyon/görsel/ses → gezinme/Back gerçek akışta geçer. |
| Gerçek yaşam döngüsü, sınırlı hazırlık ve erişim yenilemesi | İlgili Epic 1 medya/oturum story’leri; özellikle 1.7, 1.11, 1.12 ve release performans kabulü | Soğuk/sıcak cache, hızlı gezinme, arka plan, bozuk medya, expiry/retry ve hesap ayrımı doğru; bellek/gecikme sınırları çalışan akıştan ölçülüp ilgili kabulden önce sabitlenir. |
| Tam tester/yayın ve korumalı teslimat politikası | EXT-MEDIA/EXT-PUBLICATION’ın tüketici dilimleri, Epic 5 ve Epic 7 | Ordinary/tester/subscriber ayrımı, retained session, revoke/renewal/cache/retention ve mutlak storage süre açıkları üretime çıkmadan kapanır. |
| Yeni yayınlarda üretim etkinleştirmesi | Backend/CMS ve operasyon; tüketici kabulü ve ayrı canlı onayından sonra | Gerçek üretimde Publish → hazırlama → bağımsız çıktı doğrulaması → uygun güncel READY/LIVE zinciri çalışır. Bozuk/eksik çıktı yeni revizyonu yayınlatmaz; önceki uygun LIVE korunur. |
| İki hikâyelik kontrollü canlı pilot | Üretim hazırlığı ve ayrı operasyon onayından sonra | Kesin dil/rol listesi, değişmez yeni yollar, yazmasız prova, küçük etkinleştirme ve eski revizyona geri dönüş gerçekten doğrulanır. Orijinaller ve mevcut oturumlar korunur. |
| Mevcut uygun hikâyelere yaygınlaştırma | Pilot başarılı olduktan sonra gruplar halinde içerik geçişi | Uygun içeriklerin durumu izlenir; başarısızlıklar ayrı raporlanır. Eksik kaynak veya desteklenmeyen format sessizce başarılı sayılmaz. |

Onaylı sıra korunur: **önce yeni yayınlarda zorunlu optimizasyon, ardından
mevcut içeriklerin kontrollü geçişi.** Yerel yeni-yayın testi, üretimdeki bu
etkinleştirmenin kanıtı sayılmaz.

78 hikâyenin tamamını şimdi taşımak Story 1.3’e dönüş koşulu değildir.
P5a’da görülen eksik PT kapak, 133 GIF MIME kaydı ve 342 eksik SHA kaydı,
arşiv geçişinde sahipli işlerdir; her biri ayrı hikâye adedi değildir. Bu
istisnalar çözülmeden bütün arşiv optimize edildi denmez. Temizlik/silme ayrı
iştir; pilot veya backfill onayının içine gizlenmez.

Ana test ortamı PRD’de kararlaştırıldığı gibi Android emülatörleri ve iOS
Simulator’dır; mevcut fiziksel cihazlar tamamlayıcıdır. Yeni fiziksel cihaz
satın alma veya her değişiklikte yeniden iPhone ölçümü şartı eklenmez.
Gerçek platform/üretim kanıtı gerektiren durumlar kendi release kapısında kalır.

## 8. Hangi belgede ne değişecek?

| Belge / kayıt | Önce | Önerilen değişiklik |
|---|---|---|
| Görsel ana uygulama planı | Bazı P3/P4 başlıkları eski ileriye dönük metinde; P5a sonrası durum eksik | Tarihli tek durum/kapanış özeti ve bu üç teslim paketine bağlantı; eski kanıtlar silinmez. |
| Story 1.2 sonrası takip notu | Açık uçlu ayrı görsel çalışması | Altyapı dönüş koşulu A ve üretim koşulu B ayrı kaydedilir. |
| Mobil epic / dış bağımlılıklar | Genel EXT-MEDIA/EXT-PUBLICATION açık | Yalnız tamamlanan tüketici diliminin kanıtı bağlanır; bütün dış bağımlılık kapandı denmez. |
| Mobil sprint durumu | 1.1 done, 1.2 review, 1.3 backlog | İnsan kabulünden sonra uygun durum güncellenir; plan isteği kabul yerine sayılmaz. |
| Görsel P0 tracker’ı | Yalnız P0 epic’ini izliyor | Kapsam etiketi korunur; bütün P1–P5 bitti diye kapatılmaz. Gerekli eski review devirleri kanıtla sonuçlandırılır. |
| ADR / proje hafızası / runbook | Üretim kapıları kapalı | Yalnız gerçekten kabul edilen codec/profil/teslimat veya dağıtım davranışı değişince güncellenir. Bu öneri yeni mimari seçmez. |

PRD’nin ürün işlevleri, minimum platformları ve veri/erişim koruması değişmez.
Görünür ürün yeniden tasarımı yoktur. Önceki çalışan kodu geri almak veya
deney kanıtlarını silmek önerilmez. Değişiklik, iş sırası ve kabul sınırıdır.

## 9. Devir ve plan onayı

**Yöntem:** Mevcut planı doğrudan düzeltme ve kalan işi sınırlama. MVP’nin
kullanıcı işlevleri azaltılmaz; tamamlanmış kod geri alınmaz. Kapsam değişikliği
orta düzeydedir: backend, mobil ve kabul işlerinin sırası netleşir.

- Backend geliştirme Paket 1 ve Paket 2’nin sunucu sözleşmesini sahiplenir.
- Mobil geliştirme Paket 2’nin ortak bileşenini, sonra Story 1.3/1.4’ü sahiplenir.
- QA/geliştirme Paket 3’ün tek kabul listesini ve gerçek kanıtı toplar.
- Ürün sahibi kapsamı ve Story 1.2 devrini değerlendirir. Canlı pilot/rollout
  için daha sonra somut operasyon kapsamını ayrıca onaylar.

**2026-10-22 kullanıcı onayı:** “Tamam planı uygulamaya başlayalım”.
Üç paketlik uygulama sırası kabul edildi; Paket 1 başladı. Canlı işlem ve
dağıtım onayı verilmedi. Mobil story durumları değişmedi.

### Uygulama durumu

| Paket | Durum | Kalan iş |
|---|---|---|
| 1 — Görsel hazırlama | Geliştirme kabulü tamam: 41 dosya, son Linux ARM64 JAR ve 595 çalışan test geçti; beş inceleme bulgusu kapandı | Üretim yeterlilik sınırları aşağıdaki sahipli işlerde korunur |
| 2 — Ortak mobil görsel katmanı | Geliştirme kabulü tamam: 603 backend, 654 tam Flutter ve son yamalarla 36 hedefli test geçti | V2 normal root açığı Paket 3 kabulünde kapandı; Paket 2 kanıtı değiştirilmedi |
| 3 — Tek kabul ve epic'e dönüş | Geliştirme kabulü tamam: gerçek Spring→normal LibraryApp/detail ve sayfa; Maven 1 + iç Flutter 1, sıfır failure/error/skip; analyzer temiz | Story 1.3 backlog'da; audio/start ve üretim işleri sahipli devredildi |

**2026-10-23 devir:** [Paket 3 tek sonuç ve K3.1–K3.5 eşlemesi](../../be/docs/evidence/image-infrastructure-acceptance/README.md)
Paket 1/Paket 2'yi tekrar çalıştırmadan kullanır. Yeni JPEG ve PNG yerel kaynaklar gerçek
hazırlama→LIVE→normal HTTP→detail/sayfa engine decode yolundan geçti. Gecikmeli ayrılma
istemci transport/cache/provider cleanup'ını ve permit bırakılmasını kanıtlar. Eski ana
uygulama planı ve mobil tracker yazılmadı. 1.1 `done`, 1.2 `review`, 1.3 `backlog` kaldı;
Story 1.2 insan kabulü açık, Story 1.3 uygulanmış değildir.

Story 1.3'e `/api/library/reading-images/{manifest,status,bytes}`, typed repository/use case/
Cubit ve normal widget ile çalışan root opt-in devredilir. Mobil Story 1.3 + backend
EXT-MEDIA sahipleri protected-start/ilk sayfa audio sözleşmesini ve anlatım tercihini
uygulamadan önce tamamlar. Bölüm 7'deki üretim sahiplikleri sürer; backend/operasyon SDK
mutlak storage deadline ve AMD64 yeterliliğini, release sahipleri gerçek cihaz kabulünü
üretime çıkmadan kapatır. Bütün arşiv optimize edilmiş sayılmaz.

Paket 1'in sonuçları [tek kanıt kaydında](../../be/docs/evidence/image-package-1/README.md)
tutulur. Paket 2'nin [uygulama sözleşmesi](../implementation-artifacts/spec-image-package-2-consumer.md)
onaylı kapsamın kod haritasını ve kabul ölçütlerini içerir. Bu durum tablosu üretim
etkinleştirmesi veya canlı geçiş onayı değildir.

## 10. Dayanaklar ve değişiklik kontrolü

- [Görsel ana planı](../specs/spec-publish-time-image-optimization/implementation-plan.md),
  [ana spec](../specs/spec-publish-time-image-optimization/SPEC.md) ve
  [P0 epic kapsamı](image-optimization/epics.md).
- [P2 kanıtı](../../be/docs/evidence/p2-reading-preparation/verification.md),
  [P3 kanıtı](../../be/docs/evidence/p3-reading-publication/verification.md),
  [P4a5 dar kabulü](../../be/docs/evidence/p4-consumer-qa/verification-2026-10-18.md).
- [P4b5 kullanıcı kapanışı](../../be/docs/evidence/image-optimization-handoff-2026-10-20.md).
- [P5a envanteri](../../be/docs/evidence/p5a-production-inventory/findings.md),
  [P5f format kararı](../../be/docs/evidence/p5f-format-decision/decision.md),
  [P5f iPhone/uyumluluk sonucu](../../be/docs/evidence/p5f-device-compatibility/verification.md),
  [P5g sonucu](../../be/docs/evidence/p5g-local-server-compatibility/sonuc.md).
- [Mobil epic’ler](epics-mobile.md), [mobil tracker](../implementation-artifacts/mobile/sprint-status.yaml),
  [Story 1.2 geliştirme kabulü](../implementation-artifacts/mobile/library-1-2-development-acceptance.md),
  [ürün gereksinimleri](prds/prd-tellpalv2-2026-09-20/prd.md).

BMad kapsam kontrolü: tetikleyici, mevcut kanıt, epic/PRD/mimari/UX etkisi,
alternatifler, iş sırası, devir ve bitiş ölçütleri değerlendirildi. Yeni epic,
ürün kapsamı daraltması veya UI değişikliği gerekmiyor. Açık kalanlar:
gerçek uygulama/test sonuçları ve üretim için ayrı onay.
Eski P4b5 hattını açma veya eski başarısız kayıtları başarılı sayma önerilmiyor.
