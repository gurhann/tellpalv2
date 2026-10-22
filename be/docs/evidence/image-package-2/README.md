# Paket 2 — Normal geliştirme görsel tüketicisi

2026-10-22. Kapsam [onaylı spec](../../../../_bmad-output/implementation-artifacts/spec-image-package-2-consumer.md)
ve [ADR-0024](../../adr/ADR-0024-reading-image-comparison.md) ile sınırlıdır.

## Teslim edilen dilim

- Ayrı `/api/library/reading-images/{manifest,status,bytes}` ailesi, varsayılan kapalı
  normal local/test girişidir. Revoke kontrollü bearer + Library allowlist + exact-language
  ücretsiz STORY + uygun retained LIVE birlikte gerekir. Byte/status isteği sunucu hesabına
  ve generation/revision/digest/job/rol/sayfa/rendition seçimine bağlıdır.
- Storage öncesi/sonrası ve son bearer kontrolünden sonra erişim yeniden sınanır. Yalnız
  pinned rendition teslim edilir; source, Range, redirect ve otomatik retry yoktur.
  MIME/SHA/byte/geometri doğrulanır. Ortak bounded Tomcat writer/no-store/kapasite korunur.
- Normal Flutter DTO/repository/use case/Cubit ayrıdır. `CachedNetworkImage`, mevcut cache
  lifecycle'ını tam bellek metadata repository'si ve `MemoryFileSystem` ile kullanır.
  Fresh admission warm cache/provider'dan önce gelir. Tek app-scope permit; metadata,
  transfer/promotion, bekleyen status/byte Future’ları, decode, provider/frame ve gerçek cleanup
  bitene kadar tutulur. Yeni owner önceki gerçek release’i iptal edilebilir ve `requestWait`
  ile sınırlı bekler; overlap açılmaz.
- Lease/status/account/route/background çizimi kaldırır; eski callback yeni attempt'i
  dolduramaz. Retry aynı LIVE anchor için fresh admission ister. Telefon 1080, tablet 1600
  genişlik; 2048 yükseklik ve en çok 4.194.304 decoded piksel tavanı aşılmaz.
- `LibraryDevHarness.install(configuration, readingImages: explicitConfiguration)` opt-in'dir.
  Varsayılan normal entry değişmez. Detail, core capability üzerinden bağlanır; legacy hero
  null olduğunda da normal manifest çalışır. Bağımsız sayfa aynı bileşeni kullanır.
  Normal root QA config/repository/Cubit toplamaz.

## Ayarlar

Backend'de `tellpal.reading-consumer.enabled`, `run-id`, `max-encoded-bytes`, `max-pixels`,
`max-memory-bytes`, `deadline-millis`, `lease-millis`, `max-total-operations`, `max-total-bytes`
açıkça verilmelidir. Local/test profili ve mevcut P3 gate'i gerekir. Eksik/uygunsuz sınır,
prod/production, stub auth veya revoke kontrolü kapalı auth reddedilir. Library allowlist'i
mevcut `tellpal.library.test.allowed-firebase-uids` ayarıdır; subscriber fixture medya yetkisi değildir.

Mobil `ReadingImageConsumerConfiguration`; origin, doğrulanacak accountScope, bellek credential
callback'i/değişim stream'i ve açık encoded/cache/source-pixel/decode/request/status/lease
sınırlarını ister. Token dosyaya veya compile-time define'a yazılmaz. Bu ayarlar üretim profili değildir.

## Çalıştırılmış kabul matrisi

| Frozen satır | Geçen somut testler |
|---|---|
| Yetkili ücretsiz LIVE: gerçek rendition, fresh admission ardından bounded çizim | `ReadingImageConsumerIT.realHttpDeliversOnlyVerifiedJpegOrWebpForThreePagesAndRejectsClosedAccess` iki formatta, cover/detail + üç sayfa; mobil `CachedNetworkImage gerçek JPEG/WEBP decode/promotion ve warm tekrar`, `legacy hero null olan detail opt-in ile normal tüketiciyi çağırır` |
| Yanlış hesap/dil/revizyon veya withdrawal: byte/cache erişimi yok | Aynı gerçek HTTP testinin hesap/dil/free retleri; `ordinaryBytesRecheckFreeAfterIoAndNeverAdmitStaleIdentityOrCorruption`; `accountIsServerDerivedAndRevocationIsCheckedAfterStorage`; mobil `fresh admission reddi var olan warm dosyayı çizime açmaz`, hesap değişimi |
| Bozuk/yarım/MIME/SHA farklı çıktı: promotion yok, eski callback atılır | Gerçek PG/storage corruption testi; mevcut `ReadingImageComparisonServiceIT` transport/integrity regresyonları; mobil bozuk byte/policy/digest, geçerli canonical digest altında JPEG SHA/byte eşitliği retleri, gerçek HTTP MIME/length/no-store/redirect ve geç admission/callback testleri |
| Lease/iptal/geç yanıt: çizim kalkar, gerçek cleanup'a kadar sahiplik tutulur | `ReadingImageConsumerHttpIT.slowSocketHoldsSharedPermitAndDeadlineReleasesPayloadBeforeAnotherTransfer`; mobil gerçek lease Timer/provider, örtülen route/background, admission beklerken örtülen route; aynı tur load→suspend/close; bloklu status/byte settlement; cleanup hata recovery; süreli/iptalli owner bekleme; background resize/DPR; loaded status reddi; gerçek HTTP açık header/body requestWait ve external cancellation testleri |

Mobil disk-yazmama testi, `dart:io` File/Directory/temp çağrılarını hata ile engellerken dosya,
metadata ve warm promotion yolunu çalıştırır; sıfır disk çağrısı bekler. Yeni model eski PNG_FIT'i
korur; null/eksik policy eski PNG'dir, unknown policy reddedilir. Eski QA PNG descriptor wire
biçimi de korunur; eski retained-ready fixture/kanıtları değiştirilmemiştir.

Kesin test sayıları ve ham log SHA'ları [verification.json](verification.json) içindedir.
654 testlik tam Flutter koşusu inceleme yamalarından önce geçti. Son farkla birlikte 36 hedefli
test geçti; analyzer temizdir. R1–R6, V1, V3 ve V4 eşlemesi JSON içindedir. Backend inceleme
yamasında değişmediği için verify tekrarlanmadı. Maven sayıları tam verify loguna aittir; rapor
dizinindeki üç stale hedefli XML toplamın dışında tutuldu.

## Kanıtın sınırı

Backend testi gerçek PostgreSQL, disk-backed immutable fixture ve embedded Tomcat/gerçek HTTP
kullanır. Firebase verifier mock'tur; gerçek Firebase/token/bulut çalışması sayılmaz. Flutter testi
normal widget ve gerçek engine JPEG/WebP decode/promotion kullanır; sentetik fixture'dır.
Ham loglar özel run dizinindedir; gerçek kaynak medya veya token eklenmemiştir.

V2 açık kabulü, Paket 3’ün tek gerçek backend→`configureLibraryDependencies(...readingImages:)`
→`LibraryApp`/detail route koşusunda tamamlanacaktır. P2’de capability’nin elle kurulduğu test
gerçek root kompozisyonu kanıtı sayılmaz. Paket 3 ayrıca epic’e devri tamamlar.
Story 1.2 insan kabulü veya Story 1.3 tamamlanmış sayılmaz. Üretim/Firebase/deploy, genel
reader/audio/session/paywall, fiziksel Android/iPhone ve AMD64 yeterliliği açılmamıştır.
SDK-only storage'ın mutlak in-flight deadline açığı sürer. P1 encoder/fingerprint davranışı
ve eski kaynak/kanıt dosyaları korunmuştur.
