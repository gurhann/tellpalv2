# Backend Mimari Rehberi

## 1. Bu belge ne için var?

Bu belge, TellPal V2 backend kod tabanını ilk kez okuyacak veya projede değişiklik yapacak bir
backend geliştirici için hazırlanmış pratik bir mimari rehberdir.

Amaç, şu sorulara tek dokümanda cevap vermektir:

- Bu sistem ne yapıyor?
- Neden bu şekilde modüllere ayrılmış?
- Hangi modül hangi işin sahibi?
- Yeni bir özellik veya bug fix için nereye bakmalıyım?
- Modüller arası iletişim nasıl çalışıyor?
- İçerik yayınlama, asset işleme, event ingest ve satın alma akışları nasıl birbirine bağlanıyor?

Bu belge `architecture.md`nin yerine geçmez. `architecture.md` daha çok yüksek seviye mimari karar
ve ilkeleri anlatır. Bu rehber ise o kararların kod tabanında nasıl somutlaştığını açıklar.

İlgili diğer kaynaklar:

- `architecture.md`: yüksek seviye mimari ve temel tasarım kararları
- `be/docs/project-memory.md`: güncel proje varsayımları ve aktif karar özeti
- `be/docs/adr/`: kalıcı mimari/politika kararları
- `be/docs/modulith/`: otomatik üretilmiş modül bağımlılık çıktıları

## 2. Sistemi 5 dakikada anla

TellPal backend, içerik üretimi ve dağıtımı odaklı bir sistemdir. Admin kullanıcıları içerik,
kategori, contributor, medya ve erişim kurallarını yönetir. Mobil istemci bu içerikleri okur,
kullanıcı profilleri ile çalışır, event gönderir ve satın alma olaylarıyla ilişkilenen akışlar
backend tarafında işlenir.

Sistem tek deploy edilen bir backend uygulamasıdır ama içinde modül sınırları nettir. Bu yüzden
“tek veritabanlı modüler monolith” gibi düşünmek gerekir.

Kritik fikirler:

- Kod tabanı modüllere bölünmüştür.
- Her modül kendi iş alanının sahibidir.
- Modüller birbirlerinin iç sınıflarına değil, `api` paketlerine bağımlı olur.
- HTTP controller’lar merkezi bir katmanda değil, ilgili modülün içinde yer alır.
- Mobilde görünür içerik için sadece yayın durumu yetmez; asset işleme de tamamlanmış olmalıdır.
- İdempotency çoğunlukla veritabanı benzersizlik kısıtları ile korunur.

## 3. Neden Spring Modulith / modular monolith?

Bu projede seçilen mimari stil klasik “katmanlı tek parça uygulama” değil, modüler monolithtir.
Sebep, iş alanının çok sayıda bağımsız ama ilişkili alt problem içermesidir:

- admin auth
- content lifecycle
- category curation
- media asset ve processing
- mobile user/profile
- event ingest
- purchase ve attribution

Bu alt alanları tek deployment altında tutmak operasyonel karmaşıklığı azaltır. Ama modül sınırlarını
başta sert çizmek, sistem büyüdükçe kod tabanının çamurlaşmasını önler.

Bu projede modülerlik şu kurallarla uygulanır:

- Her top-level package `com.tellpal.v2.<module>` bir modüldür.
- Başka modülün `domain` veya `infrastructure` paketine erişilmez.
- Cross-module kullanım sadece `api`, event veya identifier üzerinden yapılır.
- Controller’lar ilgili modülün içinde bulunur; merkezi bir presentation katmanı yoktur.

## 4. Kod tabanını nasıl okumalıyım?

Projeye ilk kez bakarken önerilen okuma sırası:

1. `AGENTS.md`
2. `be/docs/project-memory.md`
3. `architecture.md`
4. `be/docs/backend-architecture-guide.tr.md`
5. İlgili modülün `package-info.java` ve `api` paketi
6. İlgili modülün `application` servisleri
7. Gerekirse `domain`
8. En son `web` ve `infrastructure`

Bir feature üzerinde çalışırken modül içi okuma sırası:

1. `web` içindeki ilgili controller
2. controller’ın çağırdığı `application` service
3. o servisin kullandığı `api` veya `domain` tipleri
4. repository ve adapter’lar
5. exception handler ve response mapper’lar

Bu sıra önemli çünkü bu projede gerçek iş akışı çoğunlukla `application` katmanında toplanır.

## 5. Paket ve katman yapısı

Her modül genelde şu yapıyı izler:

```text
<module>/
├── api/
├── application/
├── domain/
├── infrastructure/
└── web/
```

### `api`

Bu paket modülün dışarı açtığı sözleşmedir. Başka modüller ideal olarak sadece buraya bağımlı olur.

Burada tipik olarak şunlar bulunur:

- interface use-case’ler
- command/result tipleri
- cross-module reference veya record tipleri
- bazı modüllerde public query API’leri

Bir modülü anlamak için ilk bakılacak yerlerden biri burasıdır.

### `application`

Use-case orkestrasyonu burada yaşar. Transaction sınırları, business validation, repository
kullanımı, diğer modüllerin API’lerine çağrı ve hata semantiği genelde bu katmandadır.

Bu proje için en önemli katman burasıdır. “Sistem ne yapıyor?” sorusunun cevabı çoğu zaman burada
bulunur.

### `domain`

Aggregate, entity, invariant ve state transition mantıkları burada tutulur. Her şey burada değildir;
ama gerçekten modüle ait iş kuralları burada yaşar.

Kod okurken şu soruları domain tarafında cevaplamaya çalış:

- Bu kavramın sahibi kim?
- Hangi state geçişleri geçerli?
- Hangi alan kombinasyonları yasak?
- İdempotency veya görünürlük nasıl korunuyor?

### `infrastructure`

Framework ve dış sistem entegrasyonları burada bulunur:

- JPA repository adapter’ları
- security config
- Firebase adapter’ları
- RevenueCat doğrulama
- OpenAPI config

İş kuralı burada yaşamamalıdır.

### `web`

Dış dünya giriş noktaları burada yer alır:

- `web/admin`
- `web/mobile`
- `web/webhook`

Controller’lar mümkün olduğunca ince tutulur. Request parse eder, application servisi çağırır,
response döner.

## 6. Modül haritası

Bu backend’de şu top-level modüller vardır:

- `shared`
- `admin`
- `asset`
- `content`
- `category`
- `user`
- `event`
- `purchase`

### Modül matrisi

| Modül | Ana amacı | Tipik giriş noktaları | Dış bağımlılıklar |
|---|---|---|---|
| `shared` | ortak teknik yapı taşları | common web, persistence, language types | yok |
| `admin` | admin auth, role ve session yönetimi | `AdminAuthenticationApi`, admin security config | `shared` |
| `asset` | medya kaydı, processing ve delivery bundle | `AssetRegistryApi`, `AssetProcessingApi`, `ContentAssetBundleApi` | `shared`, `content.api` |
| `content` | içerik yaşam döngüsü, localization, story page, free access | `ContentManagementService`, `ContentPublicationService`, `PublicContentQueryApi` | `shared`, `asset.api` |
| `category` | kategori ve dil bazlı curation | `CategoryManagementService`, `CategoryCurationService`, `PublicCategoryQueryApi` | `shared`, `content.api`, `asset.api` |
| `user` | mobil kullanıcı ve profil yönetimi | `UserResolutionApi`, `UserProfileService` | `shared`, `asset.api` |
| `event` | mobil event ingest ve attribution adayları | `EventTrackingApi`, `EventAttributionApi` | `shared`, `user.api`, `content.api` |
| `purchase` | ürün kataloğu, RevenueCat ingest, attribution snapshot | `PurchaseCatalogApi`, `RevenueCatWebhookProcessingService`, `PurchaseAttributionService` | `shared`, `user.api`, `event.api`, `content.api` |

## 7. Modül modül detaylı açıklama

### 7.1 `shared`

#### Amaç

Gerçekten ortak ve kararlı teknik veya küçük domain yapı taşlarını barındırır. Bu modül “ortak her
şeyi atalım” yeri değildir.

#### Sahip olduğu ana kavramlar

- `LanguageCode`
- ortak persistence altyapısı
- request correlation
- admin web support
- ortak OpenAPI config

#### Dışa açtığı yüzey

Teknik yardımcı tipler, ortak web/persistence davranışları.

#### Bağımlılıkları

Yoktur. Diğer modüller bunun üzerine oturur.

#### Kodda önce nereye bakılır?

- `shared/domain`
- `shared/web/admin`
- `shared/infrastructure/persistence`
- `shared/infrastructure/openapi`

#### Dikkat edilmesi gereken kurallar

- Business concept burada yaşamamalı.
- Bir tip yalnızca “birden fazla modül kullanıyor” diye otomatik olarak `shared`e taşınmamalı.

### 7.2 `admin`

#### Amaç

Admin kullanıcılarının kimlik doğrulama, token üretimi, refresh token rotasyonu ve rol tabanlı
yetkilendirilmesini yönetir.

#### Sahip olduğu ana kavramlar

- `AdminUser`
- `AdminRole`
- `AdminRefreshToken`
- admin JWT issuance

#### Dışa açtığı yüzey

- `AdminAuthenticationApi`
- login / refresh / logout akışları

#### Bağımlılıkları

- `shared`

#### Kodda önce nereye bakılır?

- `admin/web/admin/AdminAuthController`
- `admin/application/AdminAuthenticationService`
- `admin/infrastructure/security/AdminSecurityConfiguration`

#### Dikkat edilmesi gereken kurallar

- Refresh token’lar hash olarak saklanır.
- Refresh token rotasyonu zorunludur.
- Admin auth ile mobile auth tamamen ayrıdır.

### 7.3 `asset`

#### Amaç

Medya dosyalarının backend tarafındaki metadata kaydını, işleme durumunu ve mobil teslim için
üretilen bundle’ları yönetir.

#### Sahip olduğu ana kavramlar

- `MediaAsset`
- `AssetProcessing`
- content delivery asset bundle

#### Dışa açtığı yüzey

- `AssetRegistryApi`
- `AssetProcessingApi`
- `ContentAssetBundleApi`

#### Bağımlılıkları

- `shared`
- `content.api`

#### Kodda önce nereye bakılır?

- `asset/application/AssetRegistryService`
- `asset/application/AssetProcessingService`
- `asset/application/ContentAssetBundleService`
- `asset/web/admin/*`

#### Dikkat edilmesi gereken kurallar

- Asset referansının sahibi `asset` modülüdür.
- Processing state kalıcıdır; sadece in-memory async mantık değildir.
- `content` modülü asset kullanır ama asset entity’sinin sahibi değildir.

### 7.4 `content`

#### Amaç

İçeriklerin oluşturulması, localization yönetimi, story page yönetimi, publication ve free-access
kurallarının sahibidir.

#### Sahip olduğu ana kavramlar

- `Content`
- `ContentLocalization`
- `StoryPage`
- `StoryPageLocalization`
- `Contributor`
- `ContentContributor`
- `ContentFreeAccess`

#### Dışa açtığı yüzey

- `ContentLookupApi`
- `ContentLocalizationLookupApi`
- `ContentFreeAccessApi`
- `PublicContentQueryApi`

#### Bağımlılıkları

- `shared`
- `asset.api`

#### Kodda önce nereye bakılır?

- `content/application/ContentManagementService`
- `content/application/ContentPublicationService`
- `content/application/ContentFreeAccessService`
- `content/application/query/PublicContentQueryService`
- `content/web/admin/*`
- `content/web/mobile/ContentMobileController`

#### Dikkat edilmesi gereken kurallar

- `Content` ana aggregate’tir.
- `STORY` dışı tiplerde story page mantığı kullanılmaz.
- Mobil görünürlük için localization hem yayınlanmış hem de işlenmiş olmalıdır.
- Free access içerik görünürlüğünü etkileyen ayrı bir kural setidir.

### 7.5 `category`

#### Amaç

Kategorilerin lifecycle’ını ve dil bazlı curated content ilişkilerini yönetir.

#### Sahip olduğu ana kavramlar

- `Category`
- `CategoryLocalization`
- `CategoryContent`

#### Dışa açtığı yüzey

- `CategoryLookupApi`
- `PublicCategoryQueryApi`

#### Bağımlılıkları

- `shared`
- `content.api`
- `asset.api`

#### Kodda önce nereye bakılır?

- `category/application/CategoryManagementService`
- `category/application/CategoryCurationService`
- `category/application/query/PublicCategoryQueryService`
- `category/web/admin/*`
- `category/web/mobile/CategoryMobileController`

#### Dikkat edilmesi gereken kurallar

- Görünürlük dil bazlıdır.
- Curation da dil bazlı sahiplenilir.
- Kategori içeriği eklerken `content` modülünün public/visible contract’larına dikkat edilir.

### 7.6 `user`

#### Amaç

Mobil kullanıcıların sisteme bağlanmasını, Firebase UID eşlemesini ve profil yönetimini sağlar.

#### Sahip olduğu ana kavramlar

- `AppUser`
- `UserProfile`
- Firebase token verification ve user resolution

#### Dışa açtığı yüzey

- `UserResolutionApi`
- `UserLookupApi`

#### Bağımlılıkları

- `shared`
- `asset.api`

#### Kodda önce nereye bakılır?

- `user/application/UserResolutionService`
- `user/application/UserProfileService`
- `user/web/mobile/ProfileMobileController`
- `user/infrastructure/firebase/*`

#### Dikkat edilmesi gereken kurallar

- Firebase backend’in auth provider’ıdır.
- Backend mobile için ayrı token sistemi üretmez.
- Bir kullanıcının primary profile kuralı vardır.

### 7.7 `event`

#### Amaç

Mobil istemciden gelen içerik ve uygulama event’lerini toplar, idempotent şekilde saklar ve purchase
attribution için aday veri sağlar.

#### Sahip olduğu ana kavramlar

- `ContentEvent`
- `AppEvent`
- attribution candidate query

#### Dışa açtığı yüzey

- `EventTrackingApi`
- `EventAttributionApi`

#### Bağımlılıkları

- `shared`
- `user.api`
- `content.api`

#### Kodda önce nereye bakılır?

- `event/application/EventTrackingService`
- `event/web/mobile/EventMobileController`

#### Dikkat edilmesi gereken kurallar

- Event kayıtları immutable kabul edilir.
- İdempotency için `event_id` ve legacy key kısıtları kullanılır.
- Event modülü attribution kararını vermez; sadece veri sağlar.

### 7.8 `purchase`

#### Amaç

Ürün kataloğunu yönetir, RevenueCat webhook’larını işler ve purchase attribution snapshot üretir.

#### Sahip olduğu ana kavramlar

- `SubscriptionProduct`
- `PurchaseEvent`
- `PurchaseContextSnapshot`

#### Dışa açtığı yüzey

- purchase catalog use-case’leri
- RevenueCat webhook processing
- attribution snapshot üretimi

#### Bağımlılıkları

- `shared`
- `user.api`
- `event.api`
- `content.api`

#### Kodda önce nereye bakılır?

- `purchase/application/PurchaseCatalogService`
- `purchase/application/RevenueCatWebhookService`
- `purchase/application/RevenueCatWebhookProcessingService`
- `purchase/application/PurchaseAttributionService`
- `purchase/web/webhook/RevenueCatWebhookController`

#### Dikkat edilmesi gereken kurallar

- RevenueCat payload ham olarak saklanır, normalize alanlar ayrıca tutulur.
- Bir purchase event için en fazla bir snapshot üretilir.
- Attribution logic purchase modülüne aittir; event modülü sadece input sağlar.

## 8. Request türleri ve dış dünya giriş noktaları

### Admin request’leri

Admin yüzeyi CMS ve operasyonel kullanım içindir. Genel namespace:

- `/api/admin/auth/**`
- `/api/admin/**`

Özellikleri:

- admin JWT ile korunur
- `AdminApiController` tabanlı exception ve problem detail modeli kullanılır
- create/update/publish/retry gibi komut ağırlıklı endpoint’ler içerir

### Mobile/Public request’leri

Mobil istemciye hizmet eden yüzeydir. Namespace örnekleri:

- `/api/contents/**`
- `/api/categories/**`
- `/api/profiles/**`
- `/api/events/**`

Özellikleri:

- içerik ve kategori discovery endpoint’leri public/anonim olabilir
- profile ve event endpoint’leri Firebase bearer token ister
- response’lar mobil kullanım odaklı read model’lerdir

### Webhook request’leri

Dış sistem callback’leri burada karşılanır:

- `/api/webhooks/revenuecat`

Özellikleri:

- header tabanlı doğrulama vardır
- request önce doğrulanır, sonra işlenir
- business response’tan çok güvenli ingest ve durable kayıt önemlidir

## 9. Ana uçtan uca akışlar

### 9.1 Admin authentication

Akış:

1. `AdminAuthController` login veya refresh isteğini alır.
2. `AdminAuthenticationService` credentials veya refresh token’ı doğrular.
3. `AdminUser` ve `AdminRefreshToken` state’i güncellenir.
4. Yeni access token ve refresh token üretilir.
5. Sonuç client’a döner.

Burada dikkat:

- refresh token reuse conflict yaratır
- disabled admin kullanıcı giriş yapamaz
- logout idempotent olacak şekilde toleranslıdır

### 9.2 Content publication ve mobile görünürlük

Akış:

1. Admin içerik oluşturur.
2. Localization oluşturur/günceller.
3. Gerekli asset referansları bağlanır.
4. `ContentPublicationService` publish komutunu işler.
5. Asset tarafında ilgili localization için processing tamamlanır.
6. `PublicContentQueryService` artık içeriği mobilde görünür kabul eder.

Buradaki önemli nokta:

- “yayınlandı” ile “mobilde gerçekten servis edilebilir” aynı şey değildir

### 9.3 Asset processing

Akış:

1. Admin veya sistem processing talebi oluşturur.
2. `AssetProcessingService` kalıcı processing kaydı açar.
3. Worker/poller mantığı processing state’ini yürütür.
4. Tamamlandığında bundle ve optimize asset’ler hazır olur.
5. Content/read tarafı bunu mobil görünürlükte kullanır.

### 9.4 Mobile content/category read

Akış:

1. Mobile controller request’i alır.
2. Query service dil, görünürlük ve free-access kurallarını uygular.
3. Gerekli asset veya bundle verileri join edilir.
4. Mobile response modeli döner.

Önemli not:

- mobile controller çoğu zaman doğrudan query service’lere gider
- asıl “what is visible?” kuralı query/application katmanındadır

### 9.5 Mobile profile ve event ingest

Akış:

1. Request bearer token ile gelir.
2. resolver token’ı `UserResolutionApi` üzerinden çözer
3. user/profile context elde edilir
4. ilgili application service çalışır

Event tarafında ayrıca:

5. duplicate event kontrolü yapılır
6. receipt döner

### 9.6 RevenueCat webhook ve attribution

Akış:

1. `RevenueCatWebhookController` header ve payload’ı alır.
2. authorization doğrulanır.
3. `RevenueCatWebhookService` purchase event’i oluşturur.
4. `RevenueCatWebhookProcessingService` hemen attribution snapshot üretir.
5. sonuç loglanır ve response döner.

Buradaki önemli ayrım:

- ingest ile attribution aynı akışta birleşiyor ama roller ayrıdır
- webhook service ingest yapar
- attribution service snapshot üretir

## 10. Veri sahipliği, transaction ve idempotency

### Veri sahipliği

Her ana tablo veya kavramın sahibi tek bir modüldür. Başka modül o veriyi kullanabilir ama sahip
olamaz.

Örnekler:

- admin kullanıcı ve token verisi `admin`
- media asset ve processing verisi `asset`
- content ve localization verisi `content`
- category ve curated content link’i `category`
- app user ve profile `user`
- event kayıtları `event`
- purchase event ve snapshot `purchase`

### Transaction yaklaşımı

Bu projede transaction sınırı çoğunlukla application service seviyesindedir. Amaç, her modülün kendi
aggregate veya use-case sınırında tutarlı kalmasıdır.

Kural:

- modüller arası “tek büyük transaction” hedeflenmez
- zincir akışlarda event veya API çağrılarıyla ilerlenir

### İdempotency yaklaşımı

İdempotency çoğunlukla uygulama kodundan önce veritabanı kısıtları ile korunur.

Örnekler:

- `event_id`
- `revenuecat_event_id`
- `external_key`
- `firebase_uid`
- refresh token hash

Application service katmanı bu çatışmaları business-level response’a çevirir.

## 11. Güvenlik modeli

### Admin auth

- Spring Security + JWT
- refresh token rotasyonu
- role tabanlı yetkilendirme

### Mobile auth

- Firebase ID token backend’de doğrulanır
- backend mobile için kendi auth sistemini kurmaz

### Webhook auth

- RevenueCat authorization header doğrulanır

### API docs

- OpenAPI/Swagger vardır
- varsayılan kapalıdır
- local veya explicit config ile açılır

## 12. Test ve kalite kapıları

Projede beklenen kalite yaklaşımı:

- unit test: domain ve application davranışı
- property test: invariant ve idempotency
- integration test: PostgreSQL/Testcontainers
- architecture test: modulith boundary doğrulaması

Bir değişiklik yaparken şu soruları sor:

- Bu iş kuralını test ediyor muyum?
- Bu persistence constraint gerçekten korunuyor mu?
- Modül sınırını deliyor muyum?
- Yeni public contract eklediysem dokümante ettim mi?

## 13. Kodda bir değişiklik yapacaksam nereye bakmalıyım?

### Yeni admin use-case

Önce bak:

- ilgili `web/admin` controller
- ilgili `application` service
- modül `api` paketi

### Yeni mobile read endpoint

Önce bak:

- ilgili `web/mobile` controller
- `query` altındaki public query service
- response mapper’lar

### Yeni domain kuralı

Önce bak:

- ilgili aggregate
- o aggregate’i kullanan application service
- property/integration test yüzeyi

### Cross-module veri ihtiyacı

Önce bak:

- karşı modülün `api` paketi
- eğer gerekli contract yoksa yeni `api` record/interface eklenmesi gerekir

Şunu yapma:

- başka modülün JPA entity’sine doğrudan bağlanma
- başka modülün repository’sini kullanma
- `infrastructure` içinden business karar verme

## 14. Bu projede sık düşülen mimari hatalar

- `application` yerine controller içinde iş kuralı yazmak
- başka modülün `domain` tipine doğrudan bağımlı olmak
- `shared` modülünü convenience dump alanına çevirmek
- “published” ile “mobile-visible” kavramını karıştırmak
- idempotency’yi sadece Java koduna bırakmak
- request/response modeli ile domain modelini birebir aynı şey sanmak
- query use-case’lerinde görünürlük ve auth kurallarını controller’da çözmeye çalışmak

## 15. Projeyi okurken zihinsel model

Bu backend’i şu şekilde düşünmek en faydalısı:

- `admin`: sistemi yöneten insanların oturum ve yetkisi
- `content`: yayınlanacak şeyin kendisi
- `asset`: içeriğin medya tarafı
- `category`: içeriğin keşif ve gruplama tarafı
- `user`: mobil kimlik ve profil
- `event`: kullanıcı davranışı kaydı
- `purchase`: gelir/satın alma ve attribution
- `shared`: ortak ama dar altyapı zemini

Bir geliştirici olarak en doğru refleks şudur:

1. Sorunun hangi modüle ait olduğunu bul
2. Önce o modülün `api` ve `application` katmanını oku
3. Gerekirse `domain` kurallarına in
4. En son adapter ve persistence detayına git

Bu proje doğru okunduğunda “bir sürü controller ve entity” koleksiyonu değildir; net görevleri olan
iş alanı modüllerinin aynı backend içinde birlikte çalıştığı bir sistemdir.
