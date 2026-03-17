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
| `content` | İçerik yaşam döngüsü, yerelleştirmeler, story sayfaları, contributor bağları, ücretsiz erişim kuralı | `contents`, `content_localizations`, `story_pages`, `story_page_localizations`, `contributors`, `content_contributors`, `content_free_access` | `shared`, `asset.api` |
| `category` | Kategori yaşam döngüsü ve dil bazlı kürasyon | `categories`, `category_localizations`, `category_contents` | `shared`, `content.api`, `asset.api` |
| `asset` | Medya asset kaydı, Firebase Storage referansları, işleme/paketleme pipeline'ı | `media_assets`, `asset_processing` benzeri işlem kayıtları | `shared`, `content.api` |
| `user` | Mobil kullanıcı ve profil yönetimi, Firebase UID eşlemesi | `app_users`, `user_profiles` | `shared`, `asset.api` |
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

Ek domain nesneleri:

- `Contributor`
- `ContentContributor`
- `FreeAccessEntry`

Kurallar:

- `Content` ana aggregate root'tur.
- `STORY` tipinde `page_count`, sayfa kümesiyle tutarlı olmak zorundadır.
- `AUDIO_STORY`, `MEDITATION`, `LULLABY` için sayfa yönetimi kapalıdır.
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

### 8.3 `asset` modülü

Ana aggregate'ler:

- `MediaAsset`
- `AssetProcessing`

Kurallar:

- Orijinal, optimize edilmiş ve ZIP paketlenmiş dosyalar aynı `media_assets` tablosunda `kind` ile ayrıştırılır.
- İşleme süreci kalıcı durum makinesi olarak tutulur: `PENDING -> PROCESSING -> COMPLETED | FAILED`
- İşleme başarısız olduğunda yeniden deneme mümkündür.

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

- Signature doğrulaması controller sınırında yapılır.
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

- RevenueCat imzası doğrulanmadan payload işlenmez.
- Raw payload sadece doğrulanmış istekler için saklanır.

### 13.4 Gizli bilgiler

- JWT signing key
- Firebase service account bilgileri
- RevenueCat webhook secret
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
