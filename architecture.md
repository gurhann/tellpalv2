# Mimari Dokümanı

## 1. Amaç

Bu doküman, [requirements.md](./.kiro/specs/tellpal-v2-backend/requirements.md) ve [design.md](./.kiro/specs/tellpal-v2-backend/design.md) içindeki ürün ve teknik kararları, uygulanabilir bir backend mimarisine dönüştürür. Bu repo şu anda ağırlıklı olarak spec dosyaları içeriyor; hedef backend kod tabanı `be/` altında oluşturulacaktır.

Bu doküman şu sorulara net cevap verir:

- Sistemin modül sınırları nelerdir?
- Hangi veri hangi modülün sahipliğindedir?
- Hangi entegrasyonlar zorunludur ve nasıl izole edilir?
- İçerik yayını, asset işleme, event toplama ve satın alma akışları nasıl birlikte çalışır?
- Kod tabanı hangi kurallarla büyütülmelidir?

## 2. Kapsam

Kapsam dahilinde:

- CMS backend API'leri
- Mobil uygulama için public API'ler
- Admin kimlik doğrulama ve yetkilendirme
- Firebase Auth doğrulaması
- Firebase Storage referans yönetimi
- Asset işleme ve mobil paketleme
- RevenueCat webhook entegrasyonu
- Event tracking ve analitik için operasyonel veri toplama

Kapsam dışında:

- CMS frontend
- Mobil uygulama istemci kodu
- Push notification altyapısı
- Ayrı bir data warehouse veya BI katmanı
- Mikroservis ayrıştırması

## 3. Mimari İlkeler

### 3.1 Ana yaklaşım

Sistem, **Spring Modulith tabanlı modüler monolith** olarak tasarlanacaktır. Bu seçim şu nedenlerle yapılır:

- Ürün alanı birden fazla güçlü iş kuralı içeriyor, ancak dağıtık sistem karmaşıklığı henüz gerekmiyor.
- Modül sınırları baştan net kurulursa, ileride seçili modüller ayrıştırılabilir.
- Tek veritabanı ile transactional tutarlılık korunur.

### 3.2 DDD seviyesi

Tam stratejik DDD değil, **tactical DDD** uygulanacaktır:

- Aggregate, entity, value object ve domain service kavramları kullanılacak.
- Modüller bounded context gibi davranacak, ancak ayrı deployment birimleri olmayacak.
- CRUD ağırlıklı alanlarda gereksiz soyutlama yapılmayacak.

### 3.3 Kurallar

- Her top-level package `com.tellpal.v2.<module>` bir Spring Modulith modülüdür.
- Modüller birbirlerinin `domain` veya `infrastructure` paketlerine erişemez.
- Modüller arası iletişim sadece `api` paketleri, domain ID'leri veya uygulama event'leri üzerinden yapılır.
- Başka modülün entity nesnesi referanslanmaz; sadece ID veya read model kullanılır.
- `shared` modülü dumping ground olmayacaktır; sadece gerçekten ortak ve kararlı kavramlar burada kalır.
- Ayrı bir top-level `presentation` modülü oluşturulmayacaktır. HTTP adapter'ları ilgili modülün içinde yer alacaktır. Bu karar, modulith sınırlarını gevşeten merkezi controller katmanını önlemek için alınmıştır.

## 4. Teknoloji Tabanı

Hedef teknoloji tabanı:

- Dil: Java 25
- Framework: Spring Boot 4.x
- Modülerlik: Spring Modulith
- Veri erişimi: Spring Data JPA + Hibernate
- Veritabanı: PostgreSQL 15
- Migration: Flyway
- Güvenlik:
  - Admin auth için Spring Security + JWT
  - Mobil auth için Firebase Admin SDK
- Asset işleme:
  - Görsel varyantları için Thumbnailator
  - Ses optimizasyonu için FFmpeg
  - Paketleme için `java.util.zip`
- Test:
  - JUnit 5
  - jqwik
  - Testcontainers
  - Spring Modulith test desteği
- Gözlemlenebilirlik:
  - Spring Boot Actuator
  - Micrometer
  - JSON structured logging

Notlar:

- `design.md` içinde bazı alanlar "TBD" bırakılmıştı; bu doküman uygulanabilirlik için ilk tercihleri sabitler.
- `design.md` içindeki JavaScript/Jest benzeri test örnekleri normatif değildir. Backend testleri Java stack'i ile yazılacaktır.

## 5. Sistem Konteynerleri

Sistem tek deploy edilen backend uygulamasından oluşur, ancak şu dış sistemlere bağımlıdır:

- PostgreSQL: sistemin kayıt sistemi
- Firebase Auth: mobil kullanıcı kimlik doğrulama sağlayıcısı
- Firebase Storage: medya dosyalarının fiziksel depolaması
- RevenueCat: abonelik yaşam döngüsü olay kaynağı

Karar:

- Medya dosyaları veritabanında tutulmaz.
- Veritabanında sadece metadata, referans ve işleme durumları tutulur.
- Dış servis hataları modül sınırlarında adapter seviyesinde soyutlanır.

## 6. Hedef Paket Yapısı

```text
be/src/main/java/com/tellpal/v2/
├── TellPalApplication.java
├── shared/
├── admin/
├── content/
├── category/
├── asset/
├── user/
├── event/
└── purchase/
```

Her modül mümkün olduğunca şu iç yapıyı izler:

```text
<module>/
├── api/
├── application/
├── domain/
├── infrastructure/
└── web/
```

`web/` altındaki adapter'lar kullanım amacına göre ayrılır:

- `web/admin`
- `web/mobile`
- `web/webhook`

## 7. Modül Sınırları ve Sorumluluklar

| Modül | Sorumluluk | Sahip olduğu ana veriler | İzin verilen dış bağımlılıklar |
|---|---|---|---|
| `shared` | Ortak teknik ve küçük domain yapı taşları | `languages`, ortak enum/value object'ler | Yok |
| `admin` | Admin kullanıcıları, roller, refresh token zinciri, JWT auth | `admin_users`, `admin_roles`, `admin_user_roles`, `admin_refresh_tokens` | `shared` |
| `content` | İçerik yaşam döngüsü, yerelleştirmeler, story sayfaları, contributor bağları, ninni playback/enstrüman seçimleri, ücretsiz erişim kuralı | `contents`, `content_localizations`, `story_pages`, `story_page_localizations`, `contributors`, `content_contributors`, `content_free_access`, `lullaby_playbacks`, `instrument_catalogs`, `instrument_catalog_localizations`, `lullaby_instruments` | `shared`, `asset.api`, `category.api` |
| `category` | Kategori yaşam döngüsü ve dil bazlı kürasyon | `categories`, `category_localizations`, `category_contents` | `shared`, `content.api`, `asset.api` |
| `asset` | Medya asset kaydı, Firebase Storage referansları, işleme/paketleme pipeline'ı | `media_assets`, `asset_processing` benzeri işlem kayıtları | `shared`, `content.api` |
| `user` | Mobil kullanıcı ve profil yönetimi, Firebase UID eşlemesi | `app_users`, `user_profiles` | `shared`, `asset.api`, `category.api`, yalnız local/test QA ve normal görsel tüketicisi için `content.api` (ADR-0024) |
| `event` | İçerik ve uygulama event ingest, idempotency, batch sync | `content_events`, `app_events` | `shared`, `user.api`, `content.api` |
| `purchase` | Ürün kataloğu, RevenueCat event ingest, attribution snapshot | `subscription_products`, `purchase_events`, `purchase_context_snapshots`, lookup tabloları | `shared`, `user.api`, `event.api`, `content.api` |

### 7.1 Önemli netleştirme

`MediaAsset` shared kernel'a konulmamalıdır. `media_assets` tablosu bir entity sahipliği gerektirir ve bu sahiplik `asset` modülündedir. `shared` içinde yalnızca `LanguageCode`, ortak zaman yardımcıları, base persistence sınıfı gibi gerçekten ortak yapı taşları kalmalıdır.

## 8. Aggregate ve Domain Kararları

### 8.1 `content` modülü

Ana aggregate'ler:

- `Content`
- `ContentLocalization`
- `StoryPage`
- `LullabyPlayback`
- `InstrumentCatalog`
- `LullabyInstrument`

Ek domain nesneleri:

- `Contributor`
- `ContentContributor`
- `FreeAccessEntry`

Kurallar:

- `Content` ana aggregate root'tur.
- `STORY` tipinde `page_count`, sayfa kümesiyle tutarlı olmak zorundadır.
- `MEDITATION` ve `LULLABY` için sayfa yönetimi kapalıdır; bir `STORY` localization'ının sesli
  deneyimi canonical içerik türü değildir.
- Ninni enstrüman seçimi `Content` altında, dil bağımsız katalog kodlarıyla ve sıfırdan başlayan sıralı bağlantılarla tutulur; görünen adlar katalog localization satırlarından çözülür.
- `MUSICIAN` contributor ilişkisi enstrüman kataloğundan ayrıdır ve enstrüman seçimi localization satırlarına kopyalanmaz.
- Mobil görünürlük için yalnızca `status = PUBLISHED` yeterli değildir; ilgili localization ayrıca `processing_status = COMPLETED` olmalıdır.

### 8.2 `category` modülü

Ana aggregate'ler:

- `Category`
- `CategoryLocalization`
- `CategoryContentLink`

Kurallar:

- Kategori görünürlüğü dil bazlıdır.
- Kürasyon kayıtları da dil bazlıdır.
- Listeleme sırası dil ve kategori bağlamında sahiplenilir.
- Category type is content-aligned: `STORY`, `MEDITATION`, `LULLABY`. `AUDIO_STORY` yalnızca
  `STORY` narration'ından türeyen bir discovery/presentation deneyimi olabilir; canonical category
  veya content type değildir.
- Bir kategori yalnizca kendi type degeri ile ayni content type kayitlarini curate edebilir.

### 8.3 `asset` modülü

Ana aggregate'ler:

- `MediaAsset`
- `AssetProcessing`

Kurallar:

- Orijinal, optimize edilmiş ve ZIP paketlenmiş dosyalar aynı `media_assets` tablosunda `kind` ile ayrıştırılır.
- İşleme süreci kalıcı durum makinesi olarak tutulur: `PENDING -> PROCESSING -> COMPLETED | FAILED`
- İşleme başarısız olduğunda yeniden deneme mümkündür.
- `STORY_PAGE_MEDIA`, eski `DELIVERY` yaşam döngüsünden ayrı bir iş türüdür. İçerik modülü sayfa sırasını ve yerelleştirilmiş kaynak asset kimliklerini public API sözleşmesiyle sağlar; asset modülü dönüştürme, nesne depolama ve doğrulamayı sahiplenir.
- Tamamlanan story-page revizyonları içerik/dil/revizyon anahtarıyla immutable manifestlerde tutulur. Revizyon girdisi kaynak görsel SHA-256 değerlerini ve dönüşüm profil sürümünü kapsar; yeni çıktılar `revisions/{revision}` altında yazılır.
- Deney manifesti yalnızca doğrulanmış görsel türevlerini sunar; sayfa sesleri mevcut kaynak asset kimliği olarak kalır. Bu iş türünün durumu eski mobil görünürlük/yerelleştirme durumunu değiştirmez.
- Deney teslimatı Firebase ID token doğrulaması ve `TELLPAL_STORY_PAGE_MEDIA_TEST_ALLOWED_FIREBASE_UIDS` allowlist'i gerektirir. Varsayılan allowlist boştur; manifest uç noktası kısa ömürlü imzalı medya erişimi üretir.

### 8.4 `user` modülü

Ana aggregate:

- `AppUser`

Child entity:

- `UserProfile`

Kurallar:

- Her kullanıcı için en fazla bir birincil profil vardır.
- Firebase UID sistem genelinde benzersizdir.

### 8.5 `event` modülü

Aggregate benzeri işlem kayıtları:

- `ContentEvent`
- `AppEvent`

Kurallar:

- `event_id` istemci tarafından üretilir ve idempotency anahtarıdır.
- `legacy_event_key` ayrı benzersizlik kısıtlarına sahiptir.
- Event kayıtları immutable kabul edilir; güncelleme yerine yeni kayıt üretilir.

### 8.6 `purchase` modülü

Ana aggregate'ler:

- `SubscriptionProduct`
- `PurchaseEvent`
- `PurchaseContextSnapshot`

Kurallar:

- RevenueCat payload ham haliyle saklanır.
- Normalize alanlar ayrıca kolon bazında tutulur.
- Bir satın alma event'i için en fazla bir attribution snapshot vardır.

## 9. Veri Sahipliği ve Persistans Stratejisi

### 9.1 Genel ilkeler

- Tüm zaman alanları `timestamptz` olarak saklanır.
- Tüm oluşturma ve güncelleme zamanları UTC tutulur.
- Temel iş verileri için `bigint` surrogate key kullanılır.
- Event tablolarında doğal olarak istemci tarafından üretildiği için `UUID` primary key kullanılır.
- JSONB sadece gerçekten şemasız alanlar için kullanılır:
  - event metadata/payload
  - RevenueCat raw payload
  - purchase profile snapshot

### 9.2 Silme davranışı

- İçerik, kategori, ürün gibi ana verilerde soft delete veya state transition tercih edilir.
- Event ve purchase verileri fiziksel olarak silinmez.
- Join tabloları ve child kayıtlar için gerekli yerde cascade delete kullanılır.
- Opsiyonel medya referanslarında `SET NULL` uygulanır.

### 9.3 İdempotency yaklaşımı

İdempotency uygulama koduyla değil, öncelikle **veritabanı benzersizlik kısıtları** ile garanti altına alınır:

- `event_id`
- `revenuecat_event_id`
- `refresh_token_hash`
- `external_key`
- `firebase_uid`
- `(profile_id, legacy_event_key)` partial unique index

Uygulama servisleri bu çatışmaları iş kuralına uygun yanıta çevirir.

### 9.4 Transaction yaklaşımı

- Her application service çağrısı, kendi aggregate sınırında transaction açar.
- Modüller arası zincir iş akışlarında aynı transaction'a yayılma hedeflenmez.
- Modüller arası yan etkiler application event ile tetiklenir.
- Event ingest için `SERIALIZABLE` yerine unique index + upsert/idempotent insert yaklaşımı tercih edilir.

## 10. API Mimarisi

### 10.1 Admin API

Amaç:

- CMS kullanımını desteklemek
- Admin auth ile korunmak
- CRUD ve operasyonel komutları expose etmek

Namespace:

- `/api/admin/auth/**`
- `/api/admin/contents/**`
- `/api/admin/categories/**`
- `/api/admin/contributors/**`
- `/api/admin/media/**`
- `/api/admin/free-access/**`

### 10.2 Mobil/Public API

Amaç:

- Mobil uygulamanın içerik keşfi ve profil işlemlerini desteklemek

Namespace:

- `/api/contents/**`
- `/api/categories/**`
- `/api/profiles/**`
- `/api/events/**`

Kural:

- İçerik ve kategori listeleme endpoint'leri anonim kullanılabilir.
- Profil ve event endpoint'leri Firebase auth zorunludur.

### 10.3 Webhook API

Namespace:

- `/api/webhooks/revenuecat`

Kural:

- RevenueCat `Authorization` header doğrulaması controller sınırında yapılır.
- Doğrulama başarısızsa event işlenmez.

## 11. Temel İş Akışları

### 11.1 İçerik yayınlama ve mobil görünürlük

1. Admin içerik ve localization oluşturur.
2. Localization `PUBLISHED` durumuna alınır.
3. `content` modülü yayın kontrolünü yapar.
4. `asset` modülü ilgili localization için işleme kaydı açar veya manuel tetikleme bekler.
5. Asset işleme tamamlanınca optimize dosyalar ve ZIP paketleri üretilir.
6. `processing_status = COMPLETED` olduktan sonra içerik mobil API'de görünür olur.

Bu karar, gereksinimlerdeki iki ayrı kavramı netleştirir:

- Editoryal yayın durumu
- Mobil dağıtıma hazır olma durumu

### 11.2 Event ingest

1. Mobil istemci tekil veya batch event gönderir.
2. `event` modülü kimlik doğrulama ve profil bağlamını doğrular.
3. Event kayıtları `occurred_at` ve server-side `ingested_at` ile saklanır.
4. Duplicate kayıtlar unique constraint ile engellenir.
5. Yanıt idempotent olacak şekilde başarılı kabul edilebilir.

### 11.3 RevenueCat webhook

1. Webhook controller imzayı doğrular.
2. Ham payload audit amaçlı saklanır.
3. Normalize alanlar lookup tablolarına göre doğrulanır.
4. `purchase` modülü satın alma kaydını oluşturur.
5. Ardından attribution snapshot üretimi tetiklenir.

### 11.4 Attribution

Attribution kuralı sabittir:

- Varsayılan pencere: 24 saat
- Öncelik 1: en son `LOCKED_CONTENT_CLICKED`
- Öncelik 2: en son `PAYWALL_SHOWN`

Bu karar `purchase` modülüne aittir; `event` modülü yalnızca olay verisini sağlar.

## 12. Asset İşleme Mimarisi

`design.md` içinde async işleme için `@Async` ve queue seçenekleri bırakılmıştı. Bu dokümanda şu karar alınır:

- İşleme talebi application event veya admin komutu ile başlatılır.
- Kalıcı bir `AssetProcessing` kaydı oluşturulur.
- Worker mantığı sadece bellekte çalışan fire-and-forget yapı olamaz.
- İlk sürümde veritabanı destekli job polling yaklaşımı kullanılabilir.
- Ölçek ihtiyacı artarsa bu worker modeli SQS/RabbitMQ tabanlı kuyruğa taşınabilir; dış kontratlar değişmez.

Neden:

- Uygulama restart olduğunda iş kaybı yaşanmamalı.
- `PROCESSING` ve `FAILED` durumlarının operasyonel takibi yapılabilmeli.

Storage yolu standardı:

- Orijinal: `/content/{content_type}/{external_key}/{lang}/original/`
- İşlenmiş: `/content/{content_type}/{external_key}/{lang}/processed/`
- Paketler: `/content/{content_type}/{external_key}/{lang}/packages/`

## 13. Güvenlik Mimarisi

### 13.1 Admin güvenliği

- Admin kullanıcıları uygulama kullanıcılarından tamamen ayrıdır.
- Şifreler BCrypt ile hash'lenir.
- Refresh token'lar düz metin değil, SHA-256 hash olarak saklanır.
- Refresh token rotasyonu zorunludur.
- Yetkilendirme rol tabanlıdır.

### 13.2 Mobil güvenlik

- Firebase ID token doğrulaması backend tarafında yapılır.
- Backend kendi mobil access token sistemini üretmez.
- Profil ve event endpoint'leri kullanıcı bağlamı olmadan çalışmaz.

### 13.3 Webhook güvenliği

- RevenueCat `Authorization` header değeri doğrulanmadan payload işlenmez.
- Raw payload sadece doğrulanmış istekler için saklanır.

### 13.4 Gizli bilgiler

- JWT signing key
- Firebase service account bilgileri
- RevenueCat webhook `Authorization` header değeri
- Veritabanı bağlantı bilgileri

tamamı environment veya secret manager üzerinden sağlanır; repoya yazılmaz.

## 14. Gözlemlenebilirlik ve Operasyon

Zorunlu operasyonel görünürlük:

- Request ID bazlı structured log
- Admin auth başarısız giriş logları
- Asset processing durum geçiş logları
- RevenueCat webhook doğrulama ve ingest logları
- Event ingest throughput ve duplicate oranı

Ölçülecek metrikler:

- HTTP latency
- 4xx/5xx oranı
- Asset processing süresi
- Failed processing sayısı
- Event ingest hızı
- RevenueCat webhook işlem süresi

## 15. Test ve Kalite Kapıları

Test stratejisi bu mimarinin zorunlu parçasıdır:

- Unit test: domain kuralları ve application service davranışı
- Property test: jqwik ile invariants ve idempotency
- Integration test: Testcontainers + PostgreSQL
- Architecture test: `ApplicationModules.verify()`

Zorunlu kalite kapıları:

- Her modül için boundary testi
- Her kritik unique/check constraint için veritabanı testi
- İçerik görünürlüğü, asset readiness, attribution ve token rotasyonu için property test
- Flyway migration'ların boş veritabanında çalışması

## 16. Uygulama İçin Açık Kararlar

Bu doküman aşağıdaki kararları kapanmış kabul eder:

- Mimari stil: modüler monolith
- Modülerlik mekanizması: Spring Modulith
- Persistans: PostgreSQL + Flyway
- Medya depolama: Firebase Storage referans modeli
- Mobil auth: Firebase Auth doğrulama
- Admin auth: JWT + refresh token rotasyonu
- Asset işleme: kalıcı durum + worker modeli
- Test stack'i: JUnit 5 + jqwik + Testcontainers

Aşağıdaki alanlar implementasyon sırasında somutlanabilir, ancak mimariyi bozmaz:

- FFmpeg çağrısının doğrudan process mi yoksa wrapper library ile mi yapılacağı
- Signed URL cache süresi
- Rate limiting'in ilk sürümde aktif edilip edilmeyeceği

## 17. Sonuç

Bu backend için doğru mimari, tek deploy edilen ama modül sınırları sert olan bir Spring Modulith sistemidir. En kritik tasarım kararları şunlardır:

- merkezi `presentation` modülü yok; adapter ilgili modülün içinde
- `asset` modülü hem medya referanslarının hem de işleme pipeline'ının sahibidir
- mobil görünürlük için `PUBLISHED` tek başına yeterli değildir, `COMPLETED` işleme de gerekir
- idempotency uygulama seviyesinde tahmini değil, veritabanı kısıtlarıyla garanti altındadır
- async işleme kalıcı durum makinesi ile yürütülür

Bu doküman, [tasks.md](./.kiro/specs/tellpal-v2-backend/tasks.md) içindeki uygulama planı için mimari referans olarak kullanılmalıdır.


### Story 1.1 Library home contract (ADR-0019)

`category` owns independent opt-in row configuration for each presentation language, including audience and an explicit content/curation-language mapping. One environment-configurable eligible preview limit defaults to five. Ordinary rows require the selected language; no implicit fallback exists. Configuration saves lock one language and atomically exchange occupied positions without editing within-category card order.

`content::api` supplies bounded scalar STORY summaries; `asset::api` supplies only verified ready cover variants. `LISTING_COVER` preparation verifies actual stored JPEG bytes before an atomic two-variant ready manifest. Captured processing ID, attempt, lease, source and profile guard both completion and failure; stale callbacks cannot modify a replacement attempt. Queries never prepare artwork or expose full media.

The approved `user` → `category::api` edge admits real verified Firebase identities through an empty-by-default local/test allowlist and a server subscriber-fixture subset. `/api/test/library` is unavailable in production profiles. This endpoint and its fixtures establish controlled listing behavior, not production entitlement or protected-media rights. See ADR-0019 and the Story 1.1 evidence artifact for the contract and acceptance limits.

### STORY okuma görselleri revizyon/hazırlık sözleşmesi (ADR-0021)

2026-10-16 tarihinde kabul edilen [P1 sözleşmesi](be/docs/image-revision-readiness-contract.md) ve
[ADR-0021](be/docs/adr/ADR-0021-image-revision-readiness-contract.md), gelecek implementasyonun
tasarımını tanımlar; P1 belgelendirme adımı runtime değişikliği yapmamıştır. Sonraki yerel/test
implementasyonları aşağıda ADR-0022/0023 altında açıklanır. İlk kapsam dil bazında STORY/READING yerelleştirilmiş
okuma kapağı/detail ve sıralı sayfa görselleridir. Kaynak artwork, listening, narration, LULLABY ve
mevcut listing rolleri ayrıdır; listing placeholder davranışı korunur.

`content`, yayın niyeti/nesli, dil/sayfa sırası, editoryal/erişim uygunluğu ve LIVE işaretçisinin sahibidir.
Yayınla isteği kalıcı hazırlığı otomatik başlatır; tüm zorunlu görseller doğrulandığında hâlâ güncel ve
uygun olan nesil ek revizyon onayı olmadan otomatik yayına alınır. `asset`, gerçek kaynak byte
parmak izi, tüm yakalanmış profil/encoder/normalizasyon ayarları, immutable çıktılar ve kalıcı işleri
sahiplenir. READY ve LIVE ayrıdır; eski `PUBLISHED`/`COMPLETED` veya manuel hazır komutları yeni
sözleşmenin kanıtı değildir. Görsel kimliği yayın neslinden ayrılarak görsel dışı düzenlemelerde
doğrulanmış medyanın tekrar kodlanmadan kullanılması sağlanır.

Gelecek implementasyonda tüm çıktıların depodaki MIME/byte/SHA/boyut/decode doğrulaması zorunludur.
Manifest ve iş tamamlanması tek atomik DB transaction'ında kaydedilir; başarı ve hata aynı
iş/deneme/lease/kaynak/profil sahipliğiyle korunur. `content`, güncel nesil/uygunluk ve tam READY
kimliğini public `asset::api` üzerinden kısa DB tutarlılık sınırında doğrulayarak LIVE işaretçisini
atomik değiştirir; modüller arası entity/repository erişimi açılmaz. İndirme, dönüşüm, depolama ve
URL imzalama DB transaction'larına girmez. Kalıcı intent ve uzlaştırma restart sonrasında devam eder.

Terminal geçiş yalnızca `PROCESSING` durumundan yapılır ve ilk commit edilen sonuç kazanır.
Aynı denemenin geç başarı/hata bildirimi terminal sonucu veya immutable READY kaydını değiştiremez;
aynı sonucun tekrarı salt okunur kanıt döndürür. Açık retry yeni yakalanmış deneme/lease kimliği alır.

Başarısız veya eski aday önceki uygun live/session revizyonunu korur; ilk yayın doğrulama olmadan
yeni sözleşmede erişilemez kalır. Saklanan snapshot mutable sayfa satırlarının ömründen bağımsızdır;
son sayfanın silinmesi güncel adayı açıkça geçersiz kılar. Immutable kimlik yetki, URL yenileme veya
cache izni vermez. Reuse yalnızca doğrulanmış rol/kaynak/profil çıktılarıyla, eşit/büyüyen çıktı ise
açık profil kabulüyle mümkündür. Yeni ortak aggregate veya modül bağımlılığı eklenmez.

Üretim profilleri/sayısal sınırlar, alpha/renk/orientation/animasyon politikaları, korumalı erişim/cache/
yenileme/retention, tüketici geçişi ve rollout ayrı kapılardır. P0 Android 1× / iPhone 0.75× kararı
yalnızca gösterilen örnek/geometrileri kapsar ve ham kanıt saklama kısıtı devam eder. Bu karar P0'ın
tamamını kapatmaz; P2 veya tüm yayın akışını hazır ilan etmez. ADR-0006/0008/0010/0017/0019 ve
mevcut yayın/tüketici davranışları korunur.

### P2 STORY/READING preparation engine boundary (ADR-0022)

The approved [P2 engine specification](_bmad-output/implementation-artifacts/spec-p2-verified-reading-image-preparation.md)
adds an internal, disabled-by-default preparation boundary; bounded implementation, reviews and verification are complete.
`content::api` supplies a consistent exact-language scalar cover/detail/page snapshot, including invalid
or empty input. `asset::api` owns captured-byte/profile work, version-pinned immutable storage,
complete manifests and atomic attempt-fenced job finalization. Retained page provenance does not
depend on mutable page-row lifetime. This adds no asset-to-content module dependency.

[ADR-0022](be/docs/adr/ADR-0022-reading-image-preparation-engine.md) permits explicit local test profiles
only: narrow opaque RGB8 sRGB PNG/JPEG admission, PNG output directly from originals, no upscale,
explicit growth admission and supervised child-process/resource limits. Production admission remains
closed; broader normalization, numerical production budgets and device quality remain gated.

READY certifies captured media, not publication generation, LIVE or access. P3 owns the separate
Publish, durable desired-generation mutation/invalidation and eligible-adoption boundary described below. Existing listing,
page experiment, legacy statuses and consumers remain unchanged. No new delivery endpoint, signed
URL, session/cache/retention right, deployment, backfill or deletion is introduced. See
[engine documentation](be/docs/reading-image-preparation.md) for implementation and evidence limits.

Reading-only SDK clones use the existing SDK non-idempotent no-retry handler for both classifications,
preventing internal HTTP media resumptions without changing legacy clients. New profile identities
bind this policy and adapter/SDK-handler implementation bytes. Socket timeouts enforce inactivity;
outer monotonic checks cannot preempt a continuously progressing blocking read. Absolute in-flight
duration, including slow trickles/authentication, remains a production-qualification gate. The
SDK-only correction adds no storage worker/process/IPC layer. Diagnostic launcher watchdog evidence
does not close this gate; live GCS execution remains separately authorized. See
[storage-probe evidence](be/docs/evidence/p2-storage-probe/README.md).


### Internal local/test STORY/READING publication (ADR-0023)

[ADR-0023](be/docs/adr/ADR-0023-reading-publication-orchestration.md) adds disabled-by-default,
explicit local/test P3 orchestration. Publish commits exact-language durable intent; an external-I/O-free
claim/attachment/adoption protocol reconnects P2 work after restart. All relevant content/page/access
writers lock the content aggregate before publication scope rows. Pure assembly compares the current
ordered snapshot inside that lock. Desired generation, dispatch lease and complete effective runtime/encoder identity of current explicit profiles
fence automatic adoption; retained LIVE stores its own generation, revision and manifest proof.

`asset::api` exposes explicit immutable source admission and a fresh outside-transaction verification
operation followed by a DB-only proof guard. Admission conditionally copies pinned verified bytes to
new backend-owned original IDs and preserves old originals; explicit content/page rebinding is required.
Signed writes, upload tokens, registry registration and unconditional upload paths cannot target the
reserved namespace. Source eligibility/availability epochs fence stale verification and adoption;
immutable manifests and orphan evidence remain retained. Consistent lock order is content, scope,
source admissions in asset-ID order, then availability. No asset-to-content dependency is introduced.

A failed candidate preserves the previous eligible LIVE independently of the newest page set. Archive
and deactivation close eligibility, and a subsequent Publish needs a fresh adoption before opening the
new intent. Manual editorial/processing flags cannot certify LIVE. The admin status/retry API and CMS
feedback identify this local/test contract and preserve unsaved input/language. Existing ordinary,
listing and tester delivery, protected access, sessions, cache/retention and P4 consumer migration remain
unchanged. Production activation and cloud execution need separate authorization and qualification.

### Kontrollü QA görsel karşılaştırması (ADR-0024)

Yalnız bağımsız local/test P4a2 girişinde `user → content::api → asset::api` yönü kullanılır.
Gerçek revoke kontrollü bearer/tek QA UID, sunucuda seçilmiş ücretsiz Türkçe iki sayfalı kopya ve
retained LIVE proof birlikte doğrulanır. I/O dışındaki kısa DB kesitleri aynı kimliği yeniden sınar.
Teslimat generation-pinned canonical PNG veya doğrulanmış rendition byte'ıdır; tüm yanıtlar
`private,no-store` taşır. V37 toplam mantıksal auth kontrolü ve storage payload okuması işlem/byte rezervasyonu restart ile sıfırlanmaz. HTTP body
bitene kadar tek teslimat gate'i tutulur. Normal Library/detail/reader davranışı değişmez.
Flutter'da yalnız bu QA kapsamı diske/global ImageCache'e yazmayan owned raster ve borrowing painter istisnasını
kullanır. Sayısal ayarlar yokken yalnız ağsız geometri ölçülür. [ADR-0024](be/docs/adr/ADR-0024-reading-image-comparison.md)
ve [kanıt kaydı](be/docs/evidence/p4-reading-images/verification.md) gerçek çalışma ve cihaz kapılarını ayırır.

P4a4, bu mevcut hattı ayrı opt-in QA detail/reader rotalarında normal ekranlarla paylaşılan saf
`core/presentation` frame’lerine bağlar. `app/` kompozisyonu tek mevcut Cubit/anchor ve iş kapısı
sahibidir; child route’lar yalnız ID taşır. Güncel olmayan slot seçim/paint yapamaz. Manifestin
detail ve sıralı page bağları gerçek slot/DPR/fit ile seçilir; shortage sessiz fallback üretmez.
Normal ekranların metadata, provider, ses, prefetch ve cache sahipliği korunur; QA ses alanı kapalıdır.
Yeni backend modül bağımlılığı veya üretim erişimi açılmaz. Retained LIVE güncel teslimat kimliğidir;
bekleyen desired onun yerine geçmez. [P4a4 kapsamı](_bmad-output/implementation-artifacts/spec-p4a4-qa-consumer-delivery.md)
yerel kod/test dilimidir; gerçek #94 auth/storage/cihaz çalışması ve genel tüketici geçişi ayrı kalır.

### Normal local/test okuma görsel tüketicisi: Paket 2 (ADR-0024)

Ayrı `/api/library/reading-images` facade'ı, `user → content::api → asset::api` yönünde
varsayılan kapalı normal geliştirme teslimatı sağlar. Açık sayısal run/bütçe/lease ayarları ve
local/test profili zorunludur; prod/production reddedilir. Revoke kontrollü Firebase bearer ve mevcut
Library allowlist, aktif exact-language ücretsiz STORY ve retained LIVE erişim kesitiyle birleşir.
Subscriber fixture ve metadata tam medya yetkisi değildir. QA'nın sabit içerik/dil/sayfa/PNG kısıtları
bu yola taşınmaz; QA davranışı korunur.

Normal byte yolu yalnız doğrulanmış JPEG/PNG/WebP rendition sunar. Hesap, generation, revision,
manifest digest, job ve rol/sayfa/rendition her isteği bağlar. Storage ve son bearer denetimi etrafında
DB erişimi tekrar doğrulanır. Ortak tek HTTP permit'i, bounded Tomcat writer, no-store ve kaynak
sınırları korunur. Yeni source fallback, URL/projection/digest protokolü veya üretim girişi açılmaz.

Mobil normal tüketici, ayrı typed repository/use case/Cubit ve `CachedNetworkImage` ile çalışır.
`LibraryImageCache` lifecycle'ı bellek dosyası ve tam bellek metadata repository'siyle kurulur.
Fresh admission cache/provider'dan önce gelir. App-scope tek sahip metadata/transfer/promotion/
decode/provider/frame/cache cleanup bitene kadar kapasiteyi tutar. Lease/account/status/route/
background çizimi kaldırır; retry aynı anchor için fresh admission ister. Detail bağlantısı
feature bağımsız core capability ile açık opt-in'dir; bağımsız sayfa aynı bileşeni kullanır.

[ADR-0024](be/docs/adr/ADR-0024-reading-image-comparison.md) ve
[Paket 2 kanıtı](be/docs/evidence/image-package-2/README.md), bu sentetik local/test dilimini
production/Firebase/cihaz yeterliliğinden ve Paket 3'ün tek backend–mobil kabulünden ayırır.
