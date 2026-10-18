# ADR-0024: Kontrollü okuma görsel karşılaştırması

## Durum

2026-10-17 tarihinde onaylanan P4a2 spec kapsamında uygulanmıştır. Kod doğrulaması, sayısal deneme kararı ve fiziksel cihaz kabulü ayrı kapılardır.

## Karar

- Yalnız local/test QA girişinde `user → content::api → asset::api` yönü açılır. Normal Library/category sözleşmesi korunur. `user` gerçek revoke kontrollü Firebase bearer ve kanonik QA UID admission'ını sahiplenir; stub veya prod/production ortamı reddedilir.
- `content` sunucuda seçilmiş Türkçe, ücretsiz, iki sıralı sayfalı QA kopyasının retained LIVE kimliğini verir. TR71 seçilemez. `jobId` retained proof'tan alınır. Desired generation ve legacy processing durumu teslimat kimliği olmaz.
- I/O öncesi ve sonrası kısa content→publication→source admission→availability kesitleri kurulur. SDK pinned okuma, SHA/MIME/header/tam decode denetimi transaction dışındadır. Gerçek header sınırı teslimata özeldir; P2/P3 inspector ve profil parmak izi değiştirilmez. Gözlenen bozulma mevcut availability withdrawal yolunu kullanır. Kapasite/süre reddi sağlıklı LIVE'ı geri çekmez.
- Tüm yanıtlar `private,no-store` taşır. Kaynak kolu yalnız canonical PNG'dir. İstemci keyfi path/profile veya mutable original seçemez. Signed URL, Range ve otomatik retry yoktur.
- Sayısal run, encoded/piksel/tepe bellek/süre/lease ve toplam okuma işlem/byte sınırları zorunludur. V37 asset sayaçları başarısız okuma rezervasyonlarını ve restart sonrasındaki tüketimi saklar; aynı run'ın tavanı yükseltilemez. HTTP gate, body yazımı/istemci kopması bitene kadar tek teslimatı tutar. Storage/decode ayrıca mevcut tek kapasiteyi paylaşır.
- ADR-0018 için dar QA istisnası: `CachedNetworkImage` yerine abort edilebilir HTTP→checksum→owned buffer/descriptor/codec→`ui.Image`→klonlamayan `CustomPainter`/`paintImage` yolu kullanılır. Diske, global ImageCache'e veya offline/session depoya yazılmaz. Route'lar ortak seri iş kapısı kullanır; eski native iş bitmeden yeni kapasite açılmaz.
- Hesap/credential revoke, background, lease, status hatası ve LIVE değişimi görseli temizler. Son kontrol ile gönderim arasındaki revoke geri alınamaz; polling aralığı içinde anlık değişim garantisi yoktur.

## Sonuç ve sınırlar

Yeni QA fixture üretimi/yükleme/admission/yayın için ayrı sayısal bulut çalışma kaydı gerekir. Bu uygulama bu işleri yapmaz. Deneme kopyası PENDING kalır ve normal tüketicilere eklenmez. P4b, üretim profili, genel cache/session politikası, Android/iPhone kalite ve performans kabulü açıktır. Geometri modu ağsız çalışır; medya profili onayı sayılmaz.

## İlgili belgeler

- [P4a2 spec](../../../_bmad-output/implementation-artifacts/spec-p4a2-reading-image-comparison.md)
- [Teslimat sözleşmesi](../reading-image-comparison.md)
- [Doğrulama](../evidence/p4-reading-images/verification.md)

## Bağımsız inceleme sonrası teknik onarım

- Storage transport belirsizliği (429/503/reset) LIVE availability'yi kapatmaz. Tam okunmuş byte'ın SHA/MIME/header/decode ihlali veya authoritative pinned eksikliği withdrawal üretir. Bu ayrım yalnız P4a2 adapter'ındadır; P2/P3 fingerprint sınıfları değişmez.
- Son bearer doğrulaması dış I/O içerdiği için onun ardından body öncesi DB-only aynı LIVE/free/availability kesiti yeniden alınır.
- Response writer yalnız local/test embedded Tomcat 11 adapter'ında çalışır. Dar Valve, QA yoluna Coyote `CLOSE_NOW` abort capability'si verir. Desteklenmeyen veya capability'si wrapper'dan okunamayan container'da handler, auth/storage başlamadan reddeder. Normal consumer/server yollarına timeout veya gate eklenmez.
- Nonblocking Servlet yazıcı sabit küçük parçalar yazar. Ayrı response deadline, `async.complete` sonrası container flush beklerken de aktiftir. Deadline'da bağlantı abort edilir; payload referansı temizlenir. Permit container'ın `onComplete`/nihai `onError` cleanup sınırında bırakılır. Salt `dispose`/`complete` çağrısı sonlanma kanıtı sayılmaz; açık yavaş soket ve ikinci teslimat IT'si gerekir.
- İlk identity/liveGeneration route/Cubit yaşamı boyunca anchor olarak kalır. Lease/status/background sonrasında yeniden admission aynı anchor'ı karşılamalıdır. Seçili sayfa/kol korunur; farklı contentId byte öncesi reddedilir.
- `RawImage` yerine klonlamayan borrowing `CustomPainter`/`paintImage` kullanılır. Eski painter dispose edilen raster'dan görüntü alamaz. Owned native handle denetimleri engine display-list/GPU/RSS toplamının sıfırlandığını veya fiziksel peak bütçesini kanıtlamaz; cihaz profiling kapısı açıktır.
- SDK ve mobil Timer teknik süre aralığı pozitif `int32` milisaniyeyle sınırlıdır. Bu, üretim profil seçimi değildir. Mobil HTTP otomatik gzip açmaz; `Accept-Encoding: identity` ister ve beklenmeyen Content-Encoding'i reddeder. Byte ölçümü HTTP payload'ıdır; TLS/header maliyeti değildir.

## P4a4: ortak widget’larda ayrı QA teslimatı (2026-10-18)

[Onaylı plan](../../../_bmad-output/implementation-artifacts/spec-p4a4-qa-consumer-delivery.md)
normal detail ve reader’ın gerçek görsel yerleşimini saf ortak frame’lere çıkarır. Ayrı opt-in
QA rotaları aynı frame’lerde yalnız doğrulanmış rendition gösterir. Mevcut karşılaştırma girişi
varsayılan kalır; normal Library rotasına yeni bağ eklenmez.

- QA parent tek mevcut Cubit, sabit LIVE anchor ve seri iş kapısını sahiplenir. Child route yalnız
  content/page ID taşır. Rol ve sıralı page binding’i admitted manifestten çözülür; gerçek slot,
  DPR ve fit mevcut seçiciye verilir. Yetersiz varyant tipli ret verir; original fallback yoktur.
- Güncel route dışında kalan slot seçimi veya eski raster’ı boyayamaz. Akış kapanışı eski işi
  bekler; hesap/background/lease/status/LIVE kontrolleri aynı anchor ve sahiplik kurallarını korur.
  Yeni transport, retry, precache, disk veya global ImageCache eklenmez.
- QA ürün başlığı, kategori veya grant üretmez. Ses alanı açıkça “Bu QA denemesinde ses kapalı” der.
  Normal metadata, AudioPlayer, provider, prefetch ve gezinme sahipliği mevcut ekranlarda kalır.
- Güncel kimlik, **retained LIVE** kimliğidir. Bekleyen yeni desired iş sağlıklı retained çıktıyı
  geçersiz kılmaz; yeni LIVE benimsenirse eski anchor teslimattan önce ve sonra reddedilir.

Bu yerel uygulama gerçek #94 auth/storage veya cihaz çalışması açmaz. Eski çalışma pencereleri ve
sayaçları yeni yetki değildir. Yeni üretim, genel tüketici geçişi, üretim profili, P4b ve fiziksel
Android/iPhone kabulü açık kalır. Yerel optimize PNG/WebP dosyaları P3/LIVE sayılmaz.
