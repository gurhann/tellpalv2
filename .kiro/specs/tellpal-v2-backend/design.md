# Tasarım Dokümanı

## Genel Bakış

TellPal v2 Backend, çocuklar için çok dilli hikayeler, meditasyonlar, ninniler ve ses içerikleri sunan bir içerik platformunun sunucu tarafı uygulamasıdır. Sistem, offline-first event takibi, CMS ile yönetilen içerik yayınlama, analitik için hazır veri toplama ve RevenueCat entegrasyonu ile abonelik yönetimini destekler.

### Temel Özellikler

- **Çok Dilli İçerik Yönetimi**: İçerik ve kategoriler dil bazında bağımsız olarak yayınlanabilir
- **İçerik Tipi Çeşitliliği**: STORY (sayfalı), AUDIO_STORY, MEDITATION ve LULLABY içerik tipleri
- **Dil Bazlı Kürasyon**: Aynı kategori farklı dillerde farklı içerik ve sıralama ile yayınlanabilir
- **Asset İşleme ve Paketleme**: Yüksek çözünürlüklü medya dosyalarının otomatik optimizasyonu ve mobil için ZIP paketleme
- **Offline-First Event Tracking**: İdempotent event kayıt sistemi ile güvenilir analitik
- **RevenueCat Entegrasyonu**: Webhook tabanlı abonelik ve satın alma yönetimi
- **Admin Auth Sistemi**: Firebase Auth'tan bağımsız JWT tabanlı admin kimlik doğrulama
- **Firebase Storage Entegrasyonu**: Medya dosyaları için referans tabanlı yönetim

### Teknoloji Stack

- **Veritabanı**: PostgreSQL (timestamptz ile UTC zaman yönetimi)
- **Medya Depolama**: Firebase Storage (referans tabanlı)
- **Kimlik Doğrulama**: 
  - Uygulama kullanıcıları için Firebase Auth
  - Admin kullanıcıları için JWT (bcrypt + SHA-256)
- **Abonelik Yönetimi**: RevenueCat webhook entegrasyonu

## Teknik Kısıtlar ve Teknoloji Stack

### Backend Framework ve Dil

- **Dil**: Java 25
- **Framework**: Spring Boot 4.0.0
- **Build Tool**: Maven
- **ORM**: Spring Data JPA + Hibernate

### Veritabanı

- **RDBMS**: PostgreSQL 15
- **Connection Pool**: HikariCP (Spring Boot default)
- **Migration Tool**: Flyway veya Liquibase
- **Zaman Yönetimi**: timestamptz (UTC)

### Medya ve Depolama

- **Cloud Storage**: Firebase Storage
- **Resim İşleme**: TBD (Thumbnailator, ImageMagick, veya imgscalr)
- **Ses İşleme**: TBD (FFmpeg veya Jave2)
- **ZIP Oluşturma**: Java built-in (java.util.zip)

### Kimlik Doğrulama ve Güvenlik

- **Uygulama Kullanıcıları**: Firebase Authentication
- **Admin Kullanıcıları**: JWT (JSON Web Tokens)
  - Access Token: 1 saat süre
  - Refresh Token: 30 gün süre
- **Şifre Hashing**: BCrypt
- **Token Hashing**: SHA-256
- **HTTPS**: Zorunlu (production)

### Dış Servisler

- **Abonelik Yönetimi**: RevenueCat
- **Push Notifications**: Firebase Cloud Messaging (gelecek)
- **Analytics**: Custom (PostgreSQL tabanlı)

### Deployment ve DevOps

- **Containerization**: Docker
- **Orchestration**: Docker Compose (development/staging)
- **Cloud Platform**: AWS (production)
  - AWS ECS veya EC2 ile Docker deployment
  - AWS RDS for PostgreSQL
  - Firebase Storage (Google Cloud)
- **CI/CD**: TBD (GitHub Actions, GitLab CI, veya Jenkins)
- **Monitoring**: TBD (Prometheus + Grafana, veya AWS CloudWatch)
- **Logging**: SLF4J + Logback (JSON format)

### Performans ve Ölçeklenebilirlik

- **API Rate Limiting**: Spring Cloud Gateway veya Bucket4j
- **Caching**: Redis (optional, gelecek)
- **Async Processing**: Spring @Async + ThreadPoolTaskExecutor
- **Queue System**: TBD (RabbitMQ, AWS SQS, veya database-backed queue)

### Geliştirme Araçları

- **IDE**: IntelliJ IDEA (önerilen)
- **Code Style**: Google Java Style Guide
- **Testing Framework**: 
  - JUnit 5
  - Mockito
  - Testcontainers (integration tests)
  - fast-check equivalent: jqwik (property-based testing)
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)

### Minimum Sistem Gereksinimleri

**Development:**
- Java 25 JDK
- Maven 3.9+
- Docker 24+
- PostgreSQL 15 (Docker ile)
- 8GB RAM
- 20GB disk space

**Production:**
- 2 CPU cores (minimum)
- 4GB RAM (minimum)
- 50GB disk space
- PostgreSQL 15
- HTTPS sertifikası

### Versiyonlama ve Bağımlılıklar

**Core Dependencies:**
```xml
<properties>
    <java.version>25</java.version>
    <spring-boot.version>4.0.0</spring-boot.version>
    <spring-modulith.version>1.3.0</spring-modulith.version>
    <postgresql.version>42.7.0</postgresql.version>
    <firebase-admin.version>9.3.0</firebase-admin.version>
    <jjwt.version>0.12.0</jjwt.version>
</properties>

<dependencies>
    <!-- Spring Modulith -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-events-api</artifactId>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-docs</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**TBD (To Be Determined):**
- Resim işleme kütüphanesi (Thumbnailator vs ImageMagick vs imgscalr)
- Ses işleme kütüphanesi (FFmpeg/Jave2 vs alternatifler)
- CI/CD pipeline tool
- Monitoring ve alerting solution
- Queue system (async job processing için)



## Mimari

### Mimari Yaklaşım: Tactical DDD (Domain-Driven Design)

TellPal v2 Backend, karmaşık iş mantığını yönetmek için **Tactical DDD** yaklaşımını kullanır. Tam DDD yerine, sadece tactical pattern'ler uygulanarak gereksiz karmaşıklıktan kaçınılır.

#### Modüler Monolith: Spring Modulith

Proje, **Spring Modulith** kullanarak modüler monolith mimarisi ile geliştirilir. Bu yaklaşım:
- Modül boundaries'lerini compile-time'da zorlar
- Event-driven modül iletişimi sağlar
- Gelecekte microservice'e geçişi kolaylaştırır
- Otomatik dokümantasyon ve test desteği sunar

**Application Modules:**
- `content` - İçerik yönetimi
- `category` - Kategori ve kürasyon
- `asset` - Asset işleme ve paketleme
- `event` - Event tracking (content, app events)
- `purchase` - Satın alma ve attribution
- `user` - Kullanıcı ve profil yönetimi
- `admin` - Admin kimlik doğrulama
- `shared` - Shared kernel (ortak domain modelleri)

**Modül İletişimi:**
- Modüller arası direkt bağımlılık yasak
- Sadece `api` package'ları public
- Event-driven communication (Spring Modulith Events)
- Shared kernel için exception (MediaAsset, Language vb.)

#### Kullanılan DDD Pattern'leri

**Aggregates (Sınırlı Sayıda):**
- `ContentAggregate`: Content + Localizations + Pages + Contributors
- `CategoryAggregate`: Category + Localizations + Content Curation
- `AssetProcessingAggregate`: Processing Workflow + Status Management
- `UserAggregate`: User + Profiles

**Entities & Value Objects:**
- **Entities**: Content, Category, User, Event, MediaAsset
- **Value Objects**: LocalizationStatus, ProcessingStatus, ContentType, MediaKind

**Domain Services:**
- `ContentPublishingService`: İçerik yayınlama iş mantığı
- `AssetProcessingService`: Asset optimizasyonu ve paketleme
- `AttributionService`: Satın alma atıflandırma (24h window logic)
- `CurationService`: Dil bazlı içerik kürasyon

**Repositories:**
- Data access için interface'ler (domain layer)
- JPA implementasyonları (infrastructure layer)

#### Kullanılmayan DDD Pattern'leri

- ❌ **Bounded Contexts**: Tek monolith yeterli
- ❌ **Event Sourcing**: Gereksiz karmaşıklık
- ❌ **CQRS**: Basit CRUD yeterli
- ❌ **Domain Events**: Spring Events yeterli (şimdilik)

#### Proje Yapısı

```
src/main/java/com/tellpal/v2/
├── content/                         # Content Module
│   ├── ContentModule.java          # @ApplicationModule
│   ├── api/                        # Public API (diğer modüller için)
│   │   ├── ContentPublishedEvent.java
│   │   ├── ContentQueryService.java
│   │   └── dto/
│   ├── domain/
│   │   ├── Content.java            # Aggregate Root
│   │   ├── ContentLocalization.java
│   │   ├── StoryPage.java
│   │   ├── ContentType.java
│   │   └── ContentRepository.java
│   ├── application/
│   │   ├── ContentApplicationService.java
│   │   ├── ContentPublishingService.java  # Domain Service
│   │   └── internal/               # Internal (modül içi)
│   └── infrastructure/
│       ├── persistence/
│       └── event/
│           └── ContentEventListener.java
│
├── category/                        # Category Module
│   ├── CategoryModule.java
│   ├── api/
│   │   ├── CategoryPublishedEvent.java
│   │   └── CategoryQueryService.java
│   ├── domain/
│   │   ├── Category.java
│   │   ├── CategoryLocalization.java
│   │   └── CategoryRepository.java
│   ├── application/
│   │   ├── CategoryApplicationService.java
│   │   └── CurationService.java
│   └── infrastructure/
│
├── asset/                           # Asset Processing Module
│   ├── AssetModule.java
│   ├── api/
│   │   ├── AssetProcessingCompletedEvent.java
│   │   ├── AssetProcessingFailedEvent.java
│   │   └── AssetProcessingQueryService.java
│   ├── domain/
│   │   ├── AssetProcessing.java    # Aggregate Root
│   │   ├── ProcessingStatus.java
│   │   └── AssetProcessingRepository.java
│   ├── application/
│   │   ├── AssetProcessingApplicationService.java
│   │   ├── ImageOptimizationService.java
│   │   ├── AudioOptimizationService.java
│   │   └── ZipPackagingService.java
│   └── infrastructure/
│       ├── media/
│       └── event/
│           └── AssetEventListener.java  # Listens to ContentPublishedEvent
│
├── event/                           # Event Tracking Module
│   ├── EventModule.java
│   ├── api/
│   │   └── EventQueryService.java
│   ├── domain/
│   │   ├── ContentEvent.java
│   │   ├── AppEvent.java
│   │   └── EventRepository.java
│   ├── application/
│   │   └── EventApplicationService.java
│   └── infrastructure/
│
├── purchase/                        # Purchase & Attribution Module
│   ├── PurchaseModule.java
│   ├── api/
│   │   ├── PurchaseRecordedEvent.java
│   │   └── PurchaseQueryService.java
│   ├── domain/
│   │   ├── PurchaseEvent.java
│   │   ├── AttributionSnapshot.java
│   │   └── PurchaseRepository.java
│   ├── application/
│   │   ├── PurchaseApplicationService.java
│   │   └── AttributionService.java  # Domain Service
│   └── infrastructure/
│       ├── revenuecat/
│       └── event/
│
├── user/                            # User & Profile Module
│   ├── UserModule.java
│   ├── api/
│   │   ├── UserRegisteredEvent.java
│   │   └── UserQueryService.java
│   ├── domain/
│   │   ├── AppUser.java
│   │   ├── UserProfile.java
│   │   └── UserRepository.java
│   ├── application/
│   │   └── UserApplicationService.java
│   └── infrastructure/
│       └── firebase/
│
├── admin/                           # Admin Auth Module
│   ├── AdminModule.java
│   ├── api/
│   │   └── AdminAuthService.java
│   ├── domain/
│   │   ├── AdminUser.java
│   │   ├── AdminRole.java
│   │   └── AdminRepository.java
│   ├── application/
│   │   └── AdminAuthApplicationService.java
│   └── infrastructure/
│       └── security/
│
├── shared/                          # Shared Kernel
│   ├── domain/
│   │   ├── MediaAsset.java         # Value Object
│   │   ├── MediaKind.java          # Enum
│   │   ├── Language.java           # Value Object
│   │   └── LocalizationStatus.java
│   └── infrastructure/
│       ├── firebase/
│       │   └── FirebaseStorageService.java
│       └── persistence/
│           └── BaseEntity.java
│
└── presentation/                    # Presentation Layer (API)
    ├── api/
    │   ├── admin/
    │   │   ├── ContentAdminController.java
    │   │   ├── CategoryAdminController.java
    │   │   ├── AssetProcessingController.java
    │   │   └── AdminAuthController.java
    │   ├── public/
    │   │   ├── ContentPublicController.java
    │   │   ├── CategoryPublicController.java
    │   │   ├── EventController.java
    │   │   └── ProfileController.java
    │   └── webhook/
    │       └── RevenueCatWebhookController.java
    ├── dto/
    ├── mapper/
    └── exception/
```

#### Spring Modulith Event Flow Örnekleri

**Scenario 1: Content Publishing → Asset Processing**

```java
// Content Module - Emit Event
@Service
@Transactional
class ContentPublishingService {
    private final ApplicationEventPublisher events;
    
    public void publishLocalization(Long contentId, String lang) {
        // Business logic
        content.publish(lang);
        contentRepository.save(content);
        
        // Emit event (async)
        events.publishEvent(new ContentPublishedEvent(contentId, lang));
    }
}

// Asset Module - Listen Event
@ApplicationModuleListener
class AssetEventListener {
    private final AssetProcessingApplicationService assetService;
    
    void on(ContentPublishedEvent event) {
        // Start asset processing asynchronously
        assetService.startProcessing(event.contentId(), event.languageCode());
    }
}
```

**Scenario 2: Asset Processing Complete → Content Ready**

```java
// Asset Module - Emit Event
@Service
@Transactional
class AssetProcessingApplicationService {
    private final ApplicationEventPublisher events;
    
    public void completeProcessing(Long contentId, String lang) {
        // Business logic
        processing.markCompleted();
        repository.save(processing);
        
        // Emit event
        events.publishEvent(new AssetProcessingCompletedEvent(contentId, lang));
    }
}

// Content Module - Listen Event
@ApplicationModuleListener
class ContentEventListener {
    private final ContentApplicationService contentService;
    
    void on(AssetProcessingCompletedEvent event) {
        // Mark content as ready for mobile API
        contentService.markAsReady(event.contentId(), event.languageCode());
    }
}
```

**Scenario 3: Purchase Event → Attribution Snapshot**

```java
// Purchase Module - Emit Event
@Service
@Transactional
class PurchaseApplicationService {
    private final ApplicationEventPublisher events;
    
    public void recordPurchase(PurchaseEventDto dto) {
        // Business logic
        PurchaseEvent purchase = createPurchaseEvent(dto);
        repository.save(purchase);
        
        // Emit event
        events.publishEvent(new PurchaseRecordedEvent(purchase.getId()));
    }
}

// Purchase Module - Listen Own Event (same module)
@ApplicationModuleListener
class AttributionEventListener {
    private final AttributionService attributionService;
    
    void on(PurchaseRecordedEvent event) {
        // Create attribution snapshot (24h window logic)
        attributionService.createSnapshot(event.purchaseEventId());
    }
}
```

#### Modül Bağımlılık Kuralları

**Allowed:**
- ✅ Modül → Shared Kernel
- ✅ Modül → Kendi internal package'ları
- ✅ Modül → Başka modülün `api` package'ı (sadece interface/event)
- ✅ Presentation → Tüm modüllerin `api` package'ları

**Forbidden:**
- ❌ Modül → Başka modülün `domain` package'ı
- ❌ Modül → Başka modülün `application` package'ı
- ❌ Modül → Başka modülün `infrastructure` package'ı
- ❌ Circular dependencies (A → B → A)

**Enforcement:**
Spring Modulith otomatik olarak bu kuralları kontrol eder ve ihlal durumunda compile-time hatası verir.

#### Spring Modulith Test Desteği

```java
@SpringBootTest
@ApplicationModuleTest
class ContentModuleTests {
    
    @Test
    void shouldPublishContentAndTriggerAssetProcessing() {
        // Given
        Long contentId = createTestContent();
        
        // When
        contentService.publishLocalization(contentId, "tr");
        
        // Then - Event published
        assertThat(events.published(ContentPublishedEvent.class))
            .hasSize(1)
            .first()
            .satisfies(event -> {
                assertThat(event.contentId()).isEqualTo(contentId);
                assertThat(event.languageCode()).isEqualTo("tr");
            });
    }
    
    @Test
    void moduleDependenciesAreValid() {
        // Spring Modulith automatically validates module boundaries
        ApplicationModules.of(TellPalApplication.class).verify();
    }
}
```

#### Modül Dokümantasyonu

Spring Modulith otomatik olarak modül diyagramları ve dokümantasyon üretir:

```java
@Test
void generateModuleDocumentation() {
    ApplicationModules modules = ApplicationModules.of(TellPalApplication.class);
    
    // Generate module structure diagram
    new Documenter(modules)
        .writeModulesAsPlantUml()
        .writeIndividualModulesAsPlantUml();
}
```

Üretilen dokümantasyon:
- Modül yapısı diyagramı
- Modül bağımlılık grafiği
- Event flow diyagramları
- API dokümantasyonu



#### Katman Sorumlulukları

**Domain Layer:**
- İş mantığı ve kuralları
- Aggregate'ler ve Entity'ler
- Domain Service'ler (karmaşık iş mantığı)
- Repository interface'leri
- Framework bağımsız (Pure Java)

**Application Layer:**
- Use case orchestration
- Transaction yönetimi
- DTO dönüşümleri
- Domain Service'leri koordine eder

**Infrastructure Layer:**
- Teknik implementasyonlar
- Database access (JPA)
- External service entegrasyonları
- Framework-specific kod

**Presentation Layer:**
- REST API endpoints
- Request/Response handling
- Validation
- Exception handling

#### Aggregate Boundaries

**ContentAggregate:**
```java
Content (Root)
├── ContentLocalization (Entity)
│   ├── StoryPage (Entity, STORY için)
│   └── StoryPageLocalization (Entity)
└── ContentContributor (Entity)
```

**CategoryAggregate:**
```java
Category (Root)
├── CategoryLocalization (Entity)
└── CategoryContent (Entity, Curation)
```

**AssetProcessingAggregate:**
```java
AssetProcessing (Root)
├── ProcessingStatus (Value Object)
├── ProcessedAssets (Collection)
└── ProcessingError (Value Object, optional)
```

#### Domain Service Örnekleri

**ContentPublishingService:**
```java
@Service
public class ContentPublishingService {
    public void publishLocalization(Content content, String languageCode) {
        // İş kuralı: STORY için tüm sayfalar hazır olmalı
        // İş kuralı: MEDITATION için body_text dolu olmalı
        // İş kuralı: Processing COMPLETED olmalı
    }
}
```

**AttributionService:**
```java
@Service
public class AttributionService {
    public AttributionSnapshot createSnapshot(PurchaseEvent purchase) {
        // İş kuralı: 24h window içinde LOCKED_CONTENT_CLICKED
        // Yoksa PAYWALL_SHOWN eventi bul
        // Profile snapshot al
    }
}
```

### Katmanlı Mimari

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  ┌──────────────────┐              ┌──────────────────┐    │
│  │   CMS Frontend   │              │  Mobile Client   │    │
│  │   (React/Vue)    │              │   (iOS/Android)  │    │
│  └──────────────────┘              └──────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway Layer                       │
│  ┌──────────────────┐              ┌──────────────────┐    │
│  │   Admin API      │              │   Public API     │    │
│  │  (JWT Auth)      │              │ (Firebase Auth)  │    │
│  └──────────────────┘              └──────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                     Business Logic Layer                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Content    │  │   Category   │  │    Event     │     │
│  │   Service    │  │   Service    │  │   Service    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Purchase   │  │    Admin     │  │    Media     │     │
│  │   Service    │  │   Service    │  │   Service    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Data Access Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Repository  │  │  Repository  │  │  Repository  │     │
│  │   Pattern    │  │   Pattern    │  │   Pattern    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Persistence Layer                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              PostgreSQL Database                      │  │
│  │  - Content & Localization Tables                     │  │
│  │  - Category & Curation Tables                        │  │
│  │  - Event Tables (Content, App, Purchase)             │  │
│  │  - Admin Auth Tables                                 │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     External Services                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Firebase   │  │   Firebase   │  │  RevenueCat  │     │
│  │     Auth     │  │   Storage    │  │   Webhook    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### Veri Akışı Modelleri

#### İçerik Yayınlama Akışı
```
CMS Admin → Admin API → Content Service → PostgreSQL
                                ↓
                         Media Service → Firebase Storage (referans)
                                ↓
                         Localization Service (dil bazlı publish)
```

#### Mobil İçerik Tüketim Akışı
```
Mobile Client → Public API → Content Service → PostgreSQL
                                ↓
                         (status=PUBLISHED filtresi)
                                ↓
                         Dil bazlı içerik + medya referansları
```

#### Event Tracking Akışı (Offline-First)
```
Mobile Client (offline) → Event Queue (local)
                                ↓
                         (network available)
                                ↓
                         Public API (batch sync)
                                ↓
                         Event Service (idempotent insert)
                                ↓
                         PostgreSQL (event_id UUID PK)
```

#### RevenueCat Webhook Akışı
```
RevenueCat → Webhook Endpoint → Signature Validation
                                ↓
                         Purchase Service
                                ↓
                         PostgreSQL (idempotent: revenuecat_event_id)
                                ↓
                         Attribution Service (24h window)
```

#### Asset İşleme ve Paketleme Akışı
```
CMS Admin → "Paketlemeyi Başlat" Butonu
                                ↓
                         Admin API → Asset Processing Service
                                ↓
                         processing_status = PROCESSING
                                ↓
                    ┌───────────┴───────────┐
                    ▼                       ▼
            Resim Optimizasyonu      Ses Optimizasyonu
            (4 varyant üret)         (format + bitrate)
                    │                       │
                    └───────────┬───────────┘
                                ▼
                         ZIP Paketleme
                         (STORY: 2 ZIP, Diğer: 1 ZIP)
                                ↓
                         Firebase Storage Upload
                         (/content/{type}/{key}/{lang}/processed/)
                                ↓
                         Media Assets Kayıt
                         (THUMBNAIL_*, DETAIL_*, CONTENT_ZIP*)
                                ↓
                         processing_status = COMPLETED
                                ↓
                         Mobil API'de Görünür
```


## Bileşenler ve Arayüzler

### API Bileşenleri

#### Admin API

**Sorumluluklar:**
- Admin kullanıcı kimlik doğrulama ve yetkilendirme
- İçerik CRUD operasyonları
- Kategori CRUD operasyonları
- Medya varlık yönetimi
- Contributor yönetimi

**Endpoint Grupları:**

```
POST   /api/admin/auth/login
POST   /api/admin/auth/refresh
POST   /api/admin/auth/logout

GET    /api/admin/contents
POST   /api/admin/contents
GET    /api/admin/contents/{id}
PUT    /api/admin/contents/{id}
DELETE /api/admin/contents/{id}

POST   /api/admin/contents/{id}/localizations
PUT    /api/admin/contents/{id}/localizations/{lang}
DELETE /api/admin/contents/{id}/localizations/{lang}

GET    /api/admin/categories
POST   /api/admin/categories
GET    /api/admin/categories/{id}
PUT    /api/admin/categories/{id}
DELETE /api/admin/categories/{id}

POST   /api/admin/categories/{id}/localizations
PUT    /api/admin/categories/{id}/localizations/{lang}
DELETE /api/admin/categories/{id}/localizations/{lang}

POST   /api/admin/categories/{id}/contents
PUT    /api/admin/categories/{id}/contents/{contentId}/order
DELETE /api/admin/categories/{id}/contents/{contentId}

GET    /api/admin/contributors
POST   /api/admin/contributors
PUT    /api/admin/contributors/{id}
DELETE /api/admin/contributors/{id}

POST   /api/admin/media/upload
GET    /api/admin/media/{id}
DELETE /api/admin/media/{id}

POST   /api/admin/contents/{id}/localizations/{lang}/process-assets
GET    /api/admin/contents/{id}/localizations/{lang}/processing-status
```

#### Public API

**Sorumluluklar:**
- Mobil uygulama için içerik listeleme ve detay
- Kategori listeleme ve içerik kürasyon
- Event tracking (content, app events)
- Kullanıcı profil yönetimi

**Endpoint Grupları:**

```
POST   /api/auth/register
GET    /api/auth/me

GET    /api/profiles
POST   /api/profiles
GET    /api/profiles/{id}
PUT    /api/profiles/{id}

GET    /api/contents?lang={lang}&type={type}&category={slug}
GET    /api/contents/{id}?lang={lang}
GET    /api/contents/{id}/pages?lang={lang}

GET    /api/categories?lang={lang}&type={type}
GET    /api/categories/{slug}?lang={lang}
GET    /api/categories/{slug}/contents?lang={lang}

POST   /api/events/content
POST   /api/events/app
POST   /api/events/batch
```

#### Webhook API

**Sorumluluklar:**
- RevenueCat webhook alma ve doğrulama
- Purchase event kaydetme
- Attribution snapshot oluşturma

**Endpoint:**

```
POST   /api/webhooks/revenuecat
```

### Servis Bileşenleri

#### ContentService

**Sorumluluklar:**
- İçerik CRUD operasyonları
- Localization yönetimi
- Story sayfa yönetimi
- İçerik tipi validasyonu
- Publish durumu yönetimi

**Anahtar Metodlar:**
```typescript
interface ContentService {
  createContent(data: CreateContentDto): Promise<Content>
  updateContent(id: number, data: UpdateContentDto): Promise<Content>
  getContent(id: number): Promise<Content>
  listContents(filters: ContentFilters): Promise<Content[]>
  
  createLocalization(contentId: number, lang: string, data: LocalizationDto): Promise<ContentLocalization>
  updateLocalization(contentId: number, lang: string, data: LocalizationDto): Promise<ContentLocalization>
  publishLocalization(contentId: number, lang: string): Promise<void>
  
  // Story-specific
  createStoryPages(contentId: number, pages: StoryPageDto[]): Promise<void>
  updateStoryPage(contentId: number, pageNum: number, data: StoryPageDto): Promise<void>
}
```

#### CategoryService

**Sorumluluklar:**
- Kategori CRUD operasyonları
- Kategori localization yönetimi
- Dil bazlı içerik kürasyon
- Display order yönetimi

**Anahtar Metodlar:**
```typescript
interface CategoryService {
  createCategory(data: CreateCategoryDto): Promise<Category>
  updateCategory(id: number, data: UpdateCategoryDto): Promise<Category>
  getCategory(id: number): Promise<Category>
  listCategories(filters: CategoryFilters): Promise<Category[]>
  
  createLocalization(categoryId: number, lang: string, data: CategoryLocalizationDto): Promise<CategoryLocalization>
  updateLocalization(categoryId: number, lang: string, data: CategoryLocalizationDto): Promise<CategoryLocalization>
  
  addContent(categoryId: number, lang: string, contentId: number, order: number): Promise<void>
  updateContentOrder(categoryId: number, lang: string, contentId: number, newOrder: number): Promise<void>
  removeContent(categoryId: number, lang: string, contentId: number): Promise<void>
  
  getCategoryContents(categoryId: number, lang: string): Promise<Content[]>
}
```

#### EventService

**Sorumluluklar:**
- İdempotent event kaydetme
- Batch event işleme
- Event validasyonu
- Legacy event migration desteği

**Anahtar Metodlar:**
```typescript
interface EventService {
  recordContentEvent(event: ContentEventDto): Promise<void>
  recordAppEvent(event: AppEventDto): Promise<void>
  recordBatchEvents(events: EventDto[]): Promise<BatchResult>
  
  // Idempotency kontrolü
  isEventProcessed(eventId: string): Promise<boolean>
  
  // Analytics queries
  getContentEngagement(contentId: number, dateRange: DateRange): Promise<EngagementStats>
  getUserActivity(profileId: number, dateRange: DateRange): Promise<ActivityStats>
}
```

#### PurchaseService

**Sorumluluklar:**
- RevenueCat webhook işleme
- Purchase event kaydetme
- Signature doğrulama
- Attribution snapshot oluşturma
- Subscription product katalog yönetimi

**Anahtar Metodlar:**
```typescript
interface PurchaseService {
  processWebhook(payload: RevenueCatWebhook, signature: string): Promise<void>
  validateSignature(payload: string, signature: string): boolean
  
  recordPurchaseEvent(event: PurchaseEventDto): Promise<PurchaseEvent>
  createAttributionSnapshot(purchaseEventId: number): Promise<PurchaseContextSnapshot>
  
  // Product catalog
  createProduct(product: SubscriptionProductDto): Promise<SubscriptionProduct>
  updateProduct(store: string, productId: string, data: UpdateProductDto): Promise<SubscriptionProduct>
  getProduct(store: string, productId: string): Promise<SubscriptionProduct>
}
```

#### AdminAuthService

**Sorumluluklar:**
- Admin kullanıcı kimlik doğrulama
- JWT token üretme ve doğrulama
- Refresh token yönetimi ve rotasyon
- Rol tabanlı yetkilendirme

**Anahtar Metodlar:**
```typescript
interface AdminAuthService {
  login(username: string, password: string): Promise<AuthTokens>
  refreshAccessToken(refreshToken: string): Promise<AuthTokens>
  logout(refreshToken: string): Promise<void>
  
  validateAccessToken(token: string): Promise<AdminUser>
  revokeRefreshToken(tokenHash: string): Promise<void>
  
  // User management
  createAdminUser(data: CreateAdminUserDto): Promise<AdminUser>
  updateAdminUser(id: number, data: UpdateAdminUserDto): Promise<AdminUser>
  assignRole(userId: number, roleCode: string): Promise<void>
  removeRole(userId: number, roleCode: string): Promise<void>
}
```

#### MediaService

**Sorumluluklar:**
- Firebase Storage entegrasyonu
- Medya varlık referans yönetimi
- Download URL cache yönetimi
- Medya metadata yönetimi

**Anahtar Metodlar:**
```typescript
interface MediaService {
  uploadMedia(file: File, path: string): Promise<MediaAsset>
  getMediaAsset(id: number): Promise<MediaAsset>
  deleteMediaAsset(id: number): Promise<void>
  
  // Firebase Storage operations
  getDownloadUrl(objectPath: string): Promise<string>
  updateDownloadUrlCache(id: number): Promise<void>
  
  // Metadata
  calculateChecksum(file: File): Promise<string>
  extractMetadata(file: File): Promise<MediaMetadata>
}
```

#### ContributorService

**Sorumluluklar:**
- Contributor CRUD operasyonları
- İçerik-contributor ilişki yönetimi
- Dil bazlı credit yönetimi
- Sıralama yönetimi

**Anahtar Metodlar:**
```typescript
interface ContributorService {
  createContributor(data: CreateContributorDto): Promise<Contributor>
  updateContributor(id: number, data: UpdateContributorDto): Promise<Contributor>
  getContributor(id: number): Promise<Contributor>
  searchContributors(query: string): Promise<Contributor[]>
  
  addContentContributor(contentId: number, contributorId: number, role: ContributorRole, lang?: string): Promise<void>
  updateContentContributor(id: number, data: UpdateContentContributorDto): Promise<void>
  removeContentContributor(id: number): Promise<void>
  
  getContentContributors(contentId: number, lang?: string): Promise<ContentContributor[]>
}
```

#### AssetProcessingService

**Sorumluluklar:**
- Yüksek çözünürlüklü medya dosyalarının optimizasyonu
- Resim varyantları üretimi (thumbnail ve detail boyutları)
- Ses dosyası sıkıştırma ve format dönüşümü
- ZIP paket oluşturma (STORY için 2 paket, diğerleri için 1 paket)
- Firebase Storage'a optimize edilmiş dosyaların yüklenmesi
- İşlem durumu takibi ve hata yönetimi

**Anahtar Metodlar:**
```typescript
interface AssetProcessingService {
  // Ana işlem tetikleme
  startProcessing(contentId: number, lang: string): Promise<void>
  getProcessingStatus(contentId: number, lang: string): Promise<ProcessingStatus>
  retryProcessing(contentId: number, lang: string): Promise<void>
  
  // Resim optimizasyonu
  optimizeImage(sourceMediaId: number, targetSize: ImageSize): Promise<MediaAsset>
  generateCoverVariants(sourceMediaId: number): Promise<CoverVariants>
  
  // Ses optimizasyonu
  optimizeAudio(sourceMediaId: number, config: AudioConfig): Promise<MediaAsset>
  
  // ZIP paketleme
  createStoryPackages(contentId: number, lang: string): Promise<StoryPackages>
  createSingleContentPackage(contentId: number, lang: string): Promise<MediaAsset>
  
  // Firebase Storage operasyonları
  uploadProcessedAsset(file: Buffer, path: string, metadata: AssetMetadata): Promise<string>
  getProcessedAssetUrl(contentId: number, lang: string, assetType: string): Promise<string>
}

interface ProcessingStatus {
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  startedAt?: Date
  completedAt?: Date
  errorMessage?: string
  progress?: number  // 0-100
}

interface CoverVariants {
  thumbnailPhone: MediaAsset
  thumbnailTablet: MediaAsset
  detailPhone: MediaAsset
  detailTablet: MediaAsset
}

interface StoryPackages {
  part1: MediaAsset  // İlk 3 sayfa
  part2: MediaAsset  // Kalan sayfalar
}

interface ImageSize {
  width: number
  height: number
  quality: number
  format: 'webp' | 'jpeg' | 'png'
}

interface AudioConfig {
  format: 'mp3' | 'aac' | 'ogg'
  bitrate: number  // kbps
  channels: 1 | 2  // mono | stereo
}
```


## Veri Modelleri

### İçerik Modeli

#### Content (Kanonik İçerik)
```typescript
interface Content {
  id: number
  type: ContentType  // STORY | AUDIO_STORY | MEDITATION | LULLABY
  externalKey: string  // Unique, dil bağımsız anahtar
  isActive: boolean
  ageRange?: number  // Tek yaş değeri (3, 4, 5, vb.)
  pageCount?: number  // Sadece STORY tipi için, diğerleri null
  createdAt: Date
  updatedAt: Date
}
```

#### ContentFreeAccess (A/B Test Ücretsiz Erişim)
```typescript
interface ContentFreeAccess {
  id: number
  accessKey: string      // "default" | "experiment_set_a" | "experiment_set_b" vb.
  contentId: number
  languageCode: string   // Dil bazlı: tr, en, es, pt, de
  createdAt: Date
}
// Unique constraint: (accessKey, contentId, languageCode)
// "default" key her zaman mevcut olmalı
```

#### ContentLocalization (Dil Bazlı İçerik)
```typescript
interface ContentLocalization {
  contentId: number
  languageCode: string  // tr, en, es, pt, de
  title: string
  description?: string
  bodyText?: string  // MEDITATION, AUDIO_STORY için
  coverMediaId?: number
  audioMediaId?: number  // Tek sayfalık içerikler için
  durationMinutes?: number
  status: LocalizationStatus  // DRAFT | PUBLISHED | ARCHIVED
  processingStatus: ProcessingStatus  // PENDING | PROCESSING | COMPLETED | FAILED
  publishedAt?: Date
  createdAt: Date
  updatedAt: Date
}
```

#### StoryPage (Sayfa Kurgusu)
```typescript
interface StoryPage {
  contentId: number
  pageNumber: number  // 1'den başlar
  illustrationMediaId: number
}
```

#### StoryPageLocalization (Sayfa Metni ve Sesi)
```typescript
interface StoryPageLocalization {
  contentId: number
  pageNumber: number
  languageCode: string
  textContent?: string
  audioMediaId?: number
  createdAt: Date
  updatedAt: Date
}
```

### Contributor Modeli

#### Contributor
```typescript
interface Contributor {
  id: number
  displayName: string
  createdAt: Date
  updatedAt: Date
}
```

#### ContentContributor
```typescript
interface ContentContributor {
  id: number
  contentId: number
  contributorId: number
  role: ContributorRole  // AUTHOR | ILLUSTRATOR | NARRATOR | MUSICIAN
  languageCode?: string  // Dil bazlı roller için (özellikle NARRATOR)
  creditName?: string  // Override display name
  sortOrder: number
  createdAt: Date
  updatedAt: Date
}
```

### Kategori Modeli

#### Category (Kanonik Kategori)
```typescript
interface Category {
  id: number
  slug: string  // Unique, kalıcı anahtar
  type: CategoryType  // CONTENT | PARENT_GUIDANCE
  isPremium: boolean
  isActive: boolean
  createdAt: Date
  updatedAt: Date
}
```

#### CategoryLocalization (Dil Bazlı Kategori)
```typescript
interface CategoryLocalization {
  categoryId: number
  languageCode: string
  name: string
  description?: string
  imageMediaId?: number
  status: LocalizationStatus  // DRAFT | PUBLISHED | ARCHIVED
  publishedAt?: Date
  createdAt: Date
  updatedAt: Date
}
```

#### CategoryContent (Dil Bazlı Kürasyon)
```typescript
interface CategoryContent {
  categoryId: number
  languageCode: string
  contentId: number
  displayOrder: number
  createdAt: Date
}
```

### Event Modeli

#### ContentEvent (İçerik Etkileşim Eventi)
```typescript
interface ContentEvent {
  eventId: string  // UUID, client üretir (idempotency)
  profileId: number
  contentId: number
  languageCode: string
  eventType: ContentEventType  // START | EXIT | COMPLETE
  occurredAt: Date  // Client zamanı
  ingestedAt: Date  // Server zamanı
  sessionId?: string  // UUID
  leftPage?: number  // EXIT için
  engagementSeconds?: number
  metadata?: Record<string, any>  // app_version, platform, country vb.
  legacyEventKey?: string  // Firebase migration için
}
```

#### AppEvent (Uygulama Eventi)
```typescript
interface AppEvent {
  eventId: string  // UUID, client üretir
  profileId: number
  eventType: AppEventType  // APP_OPENED | ONBOARDING_STARTED | ONBOARDING_COMPLETED | 
                           // ONBOARDING_SKIPPED | PAYWALL_SHOWN | LOCKED_CONTENT_CLICKED
  contentId?: number  // LOCKED_CONTENT_CLICKED için
  occurredAt: Date  // Client zamanı
  ingestedAt: Date  // Server zamanı
  payload?: Record<string, any>  // Event'e özgü bağlam
  legacyEventKey?: string  // Firebase migration için
}
```

### Purchase Modeli

#### SubscriptionProduct (Ürün Kataloğu)
```typescript
interface SubscriptionProduct {
  store: string  // APP_STORE | PLAY_STORE | STRIPE | RC_BILLING | AMAZON
  productId: string  // Store'a özgü product ID
  productType: SubscriptionProductType  // SUBSCRIPTION | NON_RENEWING
  billingPeriodUnit: BillingPeriodUnit  // DAY | WEEK | MONTH | YEAR
  billingPeriodCount: number
  entitlementIds: string[] | Record<string, any>  // RevenueCat mapping
  isActive: boolean
  createdAt: Date
  updatedAt: Date
}
```

#### PurchaseEvent (Satın Alma Eventi)
```typescript
interface PurchaseEvent {
  id: number
  userId: number
  occurredAt: Date  // purchased_at_ms
  ingestedAt: Date
  source: PurchaseSource  // REVENUECAT_WEBHOOK | CLIENT
  eventType: string  // INITIAL_PURCHASE | RENEWAL | CANCELLATION | EXPIRATION vb.
  productId: string
  entitlementId?: string
  store: string
  priceMicros?: number  // Legacy
  currency: string
  isTrial?: boolean  // Legacy
  revenuecatEventId?: string  // Webhook idempotency
  rawPayload: Record<string, any>
  createdAt: Date
  
  // Analytics alanları
  eventTimestampAt?: Date
  expirationAt?: Date
  gracePeriodExpirationAt?: Date
  autoResumeAt?: Date
  periodType?: string  // TRIAL | INTRO | NORMAL | PROMOTIONAL | PREPAID
  isTrialConversion?: boolean
  cancelReason?: string
  expirationReason?: string
  price?: number  // USD
  priceInPurchasedCurrency?: number
  taxPercentage?: number
  commissionPercentage?: number
  transactionId?: string
  originalTransactionId?: string
  renewalNumber?: number
  offerCode?: string
  countryCode?: string
  environment?: string  // SANDBOX | PRODUCTION
  presentedOfferingId?: string
  newProductId?: string  // PRODUCT_CHANGE için
  netRevenueMicros?: number
}
```

#### PurchaseContextSnapshot (Attribution Snapshot)
```typescript
interface PurchaseContextSnapshot {
  id: number
  purchaseEventId: number  // Unique
  userId: number
  profileId?: number
  attributionWindowSeconds: number  // Default: 86400 (24 saat)
  attributedAppEventId?: string  // UUID
  attributedContentId?: number
  profileSnapshot: Record<string, any>  // age_range, favorite_genres, main_purposes vb.
  createdAt: Date
}
```

### Kullanıcı Modeli

#### AppUser (Uygulama Kullanıcısı)
```typescript
interface AppUser {
  id: number
  firebaseUid: string  // Unique
  isAllowMarketing: boolean
  createdAt: Date
  updatedAt: Date
}
```

#### UserProfile (Kullanıcı Profili)
```typescript
interface UserProfile {
  id: number
  userId: number
  name?: string
  ageRange: string  // UNKNOWN | ZERO_TO_TWO | THREE_TO_FIVE vb.
  avatarMediaId?: number
  favoriteGenres: string[]  // App enum değerleri
  mainPurposes: string[]  // App enum değerleri
  isPrimary: boolean
  createdAt: Date
  updatedAt: Date
}
```

### Admin Modeli

#### AdminUser
```typescript
interface AdminUser {
  id: number
  username: string  // Unique
  passwordHash: string  // bcrypt
  enabled: boolean
  createdAt: Date
  updatedAt: Date
  lastLoginAt?: Date
}
```

#### AdminRole
```typescript
interface AdminRole {
  code: string  // PK
  description?: string
}
```

#### AdminUserRole
```typescript
interface AdminUserRole {
  adminUserId: number
  roleCode: string
  createdAt: Date
}
```

#### AdminRefreshToken
```typescript
interface AdminRefreshToken {
  id: number
  adminUserId: number
  tokenHash: string  // SHA-256, unique
  issuedAt: Date
  expiresAt: Date
  revokedAt?: Date
  replacedByTokenHash?: string
  userAgent?: string
  ip?: string
  createdAt: Date
}
```

### Medya Modeli

#### MediaAsset
```typescript
interface MediaAsset {
  id: number
  provider: string  // FIREBASE_STORAGE
  objectPath: string  // Storage path
  kind: MediaKind  // ORIGINAL_IMAGE | ORIGINAL_AUDIO | THUMBNAIL_PHONE | THUMBNAIL_TABLET | 
                   // DETAIL_PHONE | DETAIL_TABLET | OPTIMIZED_AUDIO | 
                   // CONTENT_ZIP | CONTENT_ZIP_PART1 | CONTENT_ZIP_PART2
  mimeType?: string
  bytes?: number
  checksumSha256?: string
  downloadUrl?: string  // Cache
  createdAt: Date
}

enum MediaKind {
  // Orijinal dosyalar
  ORIGINAL_IMAGE = 'ORIGINAL_IMAGE',
  ORIGINAL_AUDIO = 'ORIGINAL_AUDIO',
  
  // Optimize edilmiş resim varyantları
  THUMBNAIL_PHONE = 'THUMBNAIL_PHONE',
  THUMBNAIL_TABLET = 'THUMBNAIL_TABLET',
  DETAIL_PHONE = 'DETAIL_PHONE',
  DETAIL_TABLET = 'DETAIL_TABLET',
  
  // Optimize edilmiş ses
  OPTIMIZED_AUDIO = 'OPTIMIZED_AUDIO',
  
  // ZIP paketleri
  CONTENT_ZIP = 'CONTENT_ZIP',              // Tek ZIP (AUDIO_STORY, MEDITATION, LULLABY)
  CONTENT_ZIP_PART1 = 'CONTENT_ZIP_PART1',  // STORY ilk 3 sayfa
  CONTENT_ZIP_PART2 = 'CONTENT_ZIP_PART2',  // STORY kalan sayfalar
}
```

### Lookup Tabloları

#### Language
```typescript
interface Language {
  code: string  // tr, en, es, pt, de
  displayName: string
  isActive: boolean
  createdAt: Date
}
```

#### PurchaseEventType
```typescript
interface PurchaseEventType {
  code: string
  description?: string
  isActive: boolean
  createdAt: Date
}
```

#### SubscriptionPeriodType
```typescript
interface SubscriptionPeriodType {
  code: string  // TRIAL | INTRO | NORMAL | PROMOTIONAL | PREPAID
  description?: string
  isActive: boolean
  createdAt: Date
}
```

#### PurchaseStore
```typescript
interface PurchaseStore {
  code: string  // APP_STORE | PLAY_STORE | STRIPE | RC_BILLING | AMAZON
  description?: string
  isActive: boolean
  createdAt: Date
}
```

#### PurchaseEnvironment
```typescript
interface PurchaseEnvironment {
  code: string  // SANDBOX | PRODUCTION
  description?: string
  isActive: boolean
  createdAt: Date
}
```

#### PurchaseReasonCode
```typescript
interface PurchaseReasonCode {
  code: string
  reasonType: string  // CANCEL_REASON | EXPIRATION_REASON
  description?: string
  isActive: boolean
  createdAt: Date
}
```

## Doğruluk Özellikleri

*Bir özellik (property), bir sistemin tüm geçerli çalıştırmalarında doğru olması gereken bir karakteristik veya davranıştır - esasen, sistemin ne yapması gerektiği hakkında resmi bir ifadedir. Özellikler, insan tarafından okunabilir spesifikasyonlar ile makine tarafından doğrulanabilir doğruluk garantileri arasında köprü görevi görür.*

### Özellik 1: Dil Bazlı İçerik Görünürlüğü

*Herhangi bir* dil kodu için, o dilde PUBLISHED durumuna sahip olmayan içerik yerelleştirmeleri API yanıtlarında görünmemelidir.

**Doğrular: Gereksinim 1.3, 1.4, 1.5**

### Özellik 2: İçerik Yerelleştirme Bütünlüğü

*Herhangi bir* içerik yerelleştirmesi için, yerelleştirme kaydı oluşturulduğunda, ilgili dil kodu ve içerik ID'si geçerli kayıtlara referans vermelidir.

**Doğrular: Gereksinim 1.2**

### Özellik 3: Story Sayfa Numarası Tutarlılığı

*Herhangi bir* STORY tipi içerik için, sayfa numaraları 1'den başlamalı ve content.page_count ile gerçek sayfa kaydı sayısı her zaman eşleşmelidir.

**Doğrular: Gereksinim 2.2, 2.4, 2.5**

### Özellik 4: İçerik Tipi Özel Alan Validasyonu

*Herhangi bir* MEDITATION veya AUDIO_STORY tipi içerik için, ilgili dil yerelleştirmesinde body_text alanı dolu olmalıdır.

**Doğrular: Gereksinim 2.6**

### Özellik 5: Harici Anahtar Benzersizliği

*Herhangi bir* iki farklı içerik için, external_key değerleri farklı olmalıdır.

**Doğrular: Gereksinim 2.9**

### Özellik 6: Contributor Rol Sıralama Tutarlılığı

*Herhangi bir* içerik ve rol kombinasyonu için, aynı role sahip birden fazla contributor varsa, sort_order değerleri benzersiz ve negatif olmayan olmalıdır.

**Doğrular: Gereksinim 3.4, 3.5**

### Özellik 7: Kategori Dil Bazlı Görünürlük

*Herhangi bir* dil kodu için, o dilde PUBLISHED durumuna sahip olmayan kategori yerelleştirmeleri API yanıtlarında görünmemelidir.

**Doğrular: Gereksinim 4.5, 4.6**

### Özellik 8: Kategori İçerik Kürasyon Tutarlılığı

*Herhangi bir* kategori ve dil kombinasyonu için, category_contents tablosundaki her kayıt, aynı dilde hem PUBLISHED kategori yerelleştirmesi hem de PUBLISHED içerik yerelleştirmesi ile eşleşmelidir.

**Doğrular: Gereksinim 4.7, 4.9**

### Özellik 9: Display Order Benzersizliği

*Herhangi bir* kategori ve dil kombinasyonu için, display_order değerleri benzersiz ve negatif olmayan olmalıdır.

**Doğrular: Gereksinim 4.8**

### Özellik 10: Medya Varlık Benzersizliği

*Herhangi bir* iki medya varlığı için, (provider, object_path) kombinasyonu benzersiz olmalıdır.

**Doğrular: Gereksinim 5.3**

### Özellik 11: Firebase UID Benzersizliği

*Herhangi bir* iki uygulama kullanıcısı için, firebase_uid değerleri farklı olmalıdır.

**Doğrular: Gereksinim 6.2**

### Özellik 12: Birincil Profil Tekilliği

*Herhangi bir* uygulama kullanıcısı için, en fazla bir profil is_primary=true olarak işaretlenmelidir.

**Doğrular: Gereksinim 6.3, 6.6**

### Özellik 13: İçerik Event İdempotency

*Herhangi bir* event_id UUID'si için, aynı event_id ile birden fazla içerik eventi kaydedildiğinde, sadece ilk kayıt veritabanına eklenmelidir.

**Doğrular: Gereksinim 7.2**

### Özellik 14: Legacy Event Key Benzersizliği

*Herhangi bir* profil ve legacy_event_key kombinasyonu için, içerik eventlerinde bu kombinasyon benzersiz olmalıdır (legacy_event_key NULL olmadığında).

**Doğrular: Gereksinim 7.8, 18.5**

### Özellik 15: Uygulama Event İdempotency

*Herhangi bir* event_id UUID'si için, aynı event_id ile birden fazla uygulama eventi kaydedildiğinde, sadece ilk kayıt veritabanına eklenmelidir.

**Doğrular: Gereksinim 8.2**

### Özellik 16: Abonelik Ürün Benzersizliği

*Herhangi bir* (store, product_id) kombinasyonu için, subscription_products tablosunda bu kombinasyon benzersiz olmalıdır.

**Doğrular: Gereksinim 9.1**

### Özellik 17: Purchase Event İdempotency

*Herhangi bir* revenuecat_event_id için, aynı revenuecat_event_id ile birden fazla satın alma eventi kaydedildiğinde, sadece ilk kayıt veritabanına eklenmelidir.

**Doğrular: Gereksinim 10.2**

### Özellik 18: Purchase Context Snapshot Tekilliği

*Herhangi bir* satın alma eventi için, en fazla bir purchase_context_snapshot kaydı bulunmalıdır.

**Doğrular: Gereksinim 11.2**

### Özellik 19: Attribution Penceresi Mantığı

*Herhangi bir* satın alma eventi için, attribution snapshot oluşturulduğunda, attribution_window_seconds içindeki en son LOCKED_CONTENT_CLICKED eventi veya yoksa en son PAYWALL_SHOWN eventi seçilmelidir.

**Doğrular: Gereksinim 11.5, 11.6**

### Özellik 20: Admin Kullanıcı Adı Benzersizliği

*Herhangi bir* iki admin kullanıcısı için, username değerleri farklı olmalıdır.

**Doğrular: Gereksinim 13.2**

### Özellik 21: JWT Token Süre Sonu Validasyonu

*Herhangi bir* JWT erişim token'ı için, token'ın süre sonu zamanı verilme zamanından sonra olmalıdır.

**Doğrular: Gereksinim 13.3**

### Özellik 22: Refresh Token Hash Benzersizliği

*Herhangi bir* iki refresh token için, token_hash değerleri farklı olmalıdır.

**Doğrular: Gereksinim 13.6**

### Özellik 23: Refresh Token Rotasyon Zinciri

*Herhangi bir* refresh token rotasyonu için, yeni token kaydı oluşturulduğunda, replaced_by_token_hash alanı yeni token'ın hash'ini içermelidir.

**Doğrular: Gereksinim 15.2**

### Özellik 24: İptal Edilmiş Token Reddi

*Herhangi bir* refresh token için, revoked_at değeri NULL değilse, bu token ile kimlik doğrulama istekleri reddedilmelidir.

**Doğrular: Gereksinim 15.4**

### Özellik 25: Token Süre Sonu Kısıtlaması

*Herhangi bir* refresh token için, expires_at değeri issued_at değerinden büyük olmalıdır.

**Doğrular: Gereksinim 15.7, 17.5**

### Özellik 26: Foreign Key Bütünlüğü

*Herhangi bir* foreign key ilişkisi için, referans verilen kayıt silindiğinde, uygun cascade veya set null davranışı uygulanmalıdır.

**Doğrular: Gereksinim 17.1, 17.6, 17.7**

### Özellik 27: Negatif Olmayan Değer Kısıtlamaları

*Herhangi bir* sayfa numarası, görüntüleme sırası, etkileşim saniyesi veya parasal tutar için, değer negatif olmamalıdır.

**Doğrular: Gereksinim 17.3**

### Özellik 28: Sayfa Numarası Başlangıç Validasyonu

*Herhangi bir* story sayfası için, page_number değeri 1 veya daha büyük olmalıdır.

**Doğrular: Gereksinim 17.4**

### Özellik 29: Zaman Damgası UTC Tutarlılığı

*Herhangi bir* zaman damgası alanı için, değer timestamptz tipinde saklanmalı ve UTC olarak yorumlanmalıdır.

**Doğrular: Gereksinim 19.1**

### Özellik 30: Event Zaman Ayrımı

*Herhangi bir* event kaydı için, hem occurred_at (client zamanı) hem de ingested_at (server zamanı) alanları bulunmalıdır.

**Doğrular: Gereksinim 19.5, 19.6**

### Özellik 31: Soft Delete Davranışı

*Herhangi bir* is_active=false olan içerik veya kategori için, kayıt API liste yanıtlarından hariç tutulmalı ancak veritabanı ilişkileri ve tarihsel referanslar korunmalıdır.

**Doğrular: Gereksinim 20.3, 20.4, 20.5**

### Özellik 32: Dil Seed Verisi Bütünlüğü

*Herhangi bir* veritabanı başlatma işlemi için, tr, en, es, pt ve de dil kodları için kayıtlar oluşturulmalı ve is_active=true olarak ayarlanmalıdır.

**Doğrular: Gereksinim 21.1, 21.2, 21.3**

### Özellik 33: Firebase Migration İdempotency

*Herhangi bir* Firebase kullanıcı kaydı için, aynı firebase_uid ile birden fazla import işlemi yapıldığında, sadece ilk kayıt oluşturulmalıdır.

**Doğrular: Gereksinim 18.1**

### Özellik 34: Firebase Event Tipi Eşleme

*Herhangi bir* Firebase geçmiş kaydı için, START_CONTENT → START, LEFT_CONTENT → EXIT ve FINISH_CONTENT → COMPLETE olarak eşlenmelidir.

**Doğrular: Gereksinim 18.4**

### Özellik 35: Purchase Event Lookup Validasyonu

*Herhangi bir* satın alma eventi için, event_type, period_type, store, environment ve reason kodları ilgili lookup tablolarındaki geçerli değerlerle eşleşmelidir.

**Doğrular: Gereksinim 12.1, 12.2, 12.3, 12.4, 12.5, 12.7**

### Özellik 36: Analitik İndeks Performansı

*Herhangi bir* event sorgulama işlemi için, profile_id, content_id, event_type ve occurred_at kombinasyonlarında indeksler kullanılmalıdır.

**Doğrular: Gereksinim 16.1, 16.2, 16.3, 16.5, 16.6, 16.7**

### Özellik 37: Asset İşleme Durumu Tutarlılığı

*Herhangi bir* içerik yerelleştirmesi için, processing_status COMPLETED olmadığı sürece, o dil için içerik mobil API yanıtlarında görünmemelidir; bu kural `isFree` durumundan bağımsız olarak uygulanmalıdır.

**Doğrular: Gereksinim 22.18**

### Özellik 38: ZIP Paket Varyant Tutarlılığı

*Herhangi bir* STORY tipi içerik için, processing_status COMPLETED olduğunda, CONTENT_ZIP_PART1 ve CONTENT_ZIP_PART2 kind değerlerine sahip iki medya varlığı bulunmalıdır.

**Doğrular: Gereksinim 22.9**

### Özellik 39: Tek ZIP Paket Tutarlılığı

*Herhangi bir* AUDIO_STORY, MEDITATION veya LULLABY tipi içerik için, processing_status COMPLETED olduğunda, CONTENT_ZIP kind değerine sahip bir medya varlığı bulunmalıdır.

**Doğrular: Gereksinim 22.10**

### Özellik 40: Kapak Varyantları Tutarlılığı

*Herhangi bir* içerik yerelleştirmesi için, processing_status COMPLETED olduğunda, THUMBNAIL_PHONE, THUMBNAIL_TABLET, DETAIL_PHONE ve DETAIL_TABLET kind değerlerine sahip dört medya varlığı bulunmalıdır.

**Doğrular: Gereksinim 22.6**

### Özellik 41: Firebase Storage Yol Yapısı

*Herhangi bir* işlenmiş medya varlığı için, objectPath değeri `/content/{content_type}/{external_key}/{lang}/processed/` formatında olmalıdır.

**Doğrular: Gereksinim 22.5**

### Özellik 42: İşlem Durumu Geçiş Validasyonu

*Herhangi bir* asset işleme süreci için, processing_status değeri sadece PENDING → PROCESSING → COMPLETED veya PENDING → PROCESSING → FAILED geçişlerini yapabilmelidir.

**Doğrular: Gereksinim 22.3, 22.13, 22.14**

### Özellik 43: Ücretsiz Erişim Varsayılan Key Bütünlüğü

*Herhangi bir* `freeKey` parametresi içermeyen içerik listesi isteği için, Backend_Sistemi `default` anahtarına karşılık gelen ücretsiz erişim kayıtlarını uygulamalıdır.

**Doğrular: Gereksinim 23.2, 23.4**

### Özellik 44: Bilinmeyen Key Fallback

*Herhangi bir* `content_free_access` tablosunda bulunmayan `freeKey` değeri için, Backend_Sistemi `default` anahtarına geri dönmelidir.

**Doğrular: Gereksinim 23.5**

### Özellik 45: Ücretsiz Erişim Dil Bazlı Tutarlılığı

*Herhangi bir* (accessKey, contentId, languageCode) kombinasyonu için, `content_free_access` tablosunda bu kombinasyon benzersiz olmalıdır.

**Doğrular: Gereksinim 23.6, 23.7**

### Özellik 46: isFree Hesaplama Tutarlılığı

*Herhangi bir* içerik listesi yanıtı için, her içeriğin `isFree` değeri, aktif `freeKey` ve ilgili dil kodu kombinasyonunun `content_free_access` tablosunda bulunup bulunmamasıyla tutarlı olmalıdır.

**Doğrular: Gereksinim 23.10**

### Özellik 47: page_count Tutarlılığı

*Herhangi bir* STORY tipi içerik için, `content.page_count` değeri her zaman `story_pages` tablosundaki o içeriğe ait kayıt sayısına eşit olmalıdır; sayfa eklendiğinde veya silindiğinde `page_count` otomatik güncellenmelidir.

**Doğrular: Gereksinim 2.5**

### Özellik 48: STORY Dışı İçerikte page_count Null Olmalı

*Herhangi bir* AUDIO_STORY, MEDITATION veya LULLABY tipi içerik için, `content.page_count` değeri NULL olmalıdır.

**Doğrular: Gereksinim 2.1**

### Özellik 49: ContentFreeAccess Benzersizliği

*Herhangi bir* (accessKey, contentId, languageCode) kombinasyonu için, `content_free_access` tablosunda bu kombinasyon benzersiz olmalıdır.

**Doğrular: Gereksinim 23.1**




## Hata Yönetimi

### Hata Kategorileri

#### Validasyon Hataları (4xx)

**400 Bad Request**
- Geçersiz request body formatı
- Eksik zorunlu alanlar
- Geçersiz enum değerleri
- Negatif sayısal değerler
- Geçersiz UUID formatı

**401 Unauthorized**
- Eksik veya geçersiz JWT token
- Süresi dolmuş erişim token'ı
- İptal edilmiş refresh token
- Geçersiz Firebase Auth token

**403 Forbidden**
- Yetersiz rol yetkileri
- Devre dışı bırakılmış admin kullanıcı
- Erişim izni olmayan kaynak

**404 Not Found**
- Var olmayan kaynak ID'si
- Silinmiş veya is_active=false kayıt
- Talep edilen dilde yerelleştirme yok

**409 Conflict**
- Benzersizlik kısıtlaması ihlali (unique constraint)
- İdempotent event tekrarı (kabul edilir, 200 döner)
- External key çakışması
- Firebase UID çakışması

**422 Unprocessable Entity**
- İş mantığı kuralı ihlali
- STORY olmayan içeriğe sayfa ekleme
- PUBLISHED olmayan içeriği kategoriye ekleme
- Geçersiz içerik tipi-alan kombinasyonu

#### Sunucu Hataları (5xx)

**500 Internal Server Error**
- Beklenmeyen uygulama hatası
- Veritabanı bağlantı hatası
- İşlenmemiş exception

**502 Bad Gateway**
- Firebase Storage erişim hatası
- RevenueCat API hatası

**503 Service Unavailable**
- Veritabanı bakım modu
- Rate limit aşımı

### Hata Yanıt Formatı

Tüm hata yanıtları tutarlı bir format kullanmalıdır:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "İçerik yerelleştirmesi için title alanı zorunludur",
    "details": {
      "field": "title",
      "constraint": "required"
    },
    "timestamp": "2024-01-15T10:30:00Z",
    "requestId": "req_abc123"
  }
}
```

### Hata Kodları

#### Validasyon Hataları
- `VALIDATION_ERROR`: Genel validasyon hatası
- `MISSING_REQUIRED_FIELD`: Zorunlu alan eksik
- `INVALID_FORMAT`: Geçersiz format
- `INVALID_ENUM_VALUE`: Geçersiz enum değeri
- `NEGATIVE_VALUE_NOT_ALLOWED`: Negatif değer kabul edilmez

#### Kimlik Doğrulama Hataları
- `INVALID_CREDENTIALS`: Geçersiz kullanıcı adı veya şifre
- `TOKEN_EXPIRED`: Token süresi dolmuş
- `TOKEN_REVOKED`: Token iptal edilmiş
- `INVALID_TOKEN`: Geçersiz token formatı
- `INSUFFICIENT_PERMISSIONS`: Yetersiz yetki

#### İş Mantığı Hataları
- `RESOURCE_NOT_FOUND`: Kaynak bulunamadı
- `DUPLICATE_RESOURCE`: Kaynak zaten mevcut
- `INVALID_STATE_TRANSITION`: Geçersiz durum geçişi
- `CONSTRAINT_VIOLATION`: Kısıtlama ihlali
- `BUSINESS_RULE_VIOLATION`: İş kuralı ihlali

#### Dış Servis Hataları
- `FIREBASE_STORAGE_ERROR`: Firebase Storage hatası
- `FIREBASE_AUTH_ERROR`: Firebase Auth hatası
- `REVENUECAT_ERROR`: RevenueCat hatası
- `EXTERNAL_SERVICE_UNAVAILABLE`: Dış servis erişilemez

### Hata Loglama

Tüm hatalar yapılandırılmış log formatında kaydedilmelidir:

```json
{
  "level": "error",
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "req_abc123",
  "userId": 12345,
  "endpoint": "/api/admin/contents",
  "method": "POST",
  "errorCode": "VALIDATION_ERROR",
  "errorMessage": "İçerik yerelleştirmesi için title alanı zorunludur",
  "stackTrace": "...",
  "context": {
    "contentId": 789,
    "languageCode": "tr"
  }
}
```

### Retry Stratejisi

#### İdempotent İşlemler
- GET, PUT, DELETE istekleri güvenle tekrar denenebilir
- Event kayıt işlemleri (event_id UUID ile idempotent)
- RevenueCat webhook işleme (revenuecat_event_id ile idempotent)

#### Retry Politikası
- Exponential backoff: 1s, 2s, 4s, 8s, 16s
- Maksimum 5 deneme
- 5xx hataları için retry
- 4xx hataları için retry yapılmaz (client hatası)

#### Circuit Breaker
- Firebase Storage için circuit breaker
- 5 ardışık hata sonrası 30 saniye açık
- Half-open durumda 1 test isteği
- Başarılı olursa kapalı duruma geç

### Transaction Yönetimi

#### ACID Garantileri
- İçerik + yerelleştirme oluşturma: tek transaction
- Kategori + içerik ilişkilendirme: tek transaction
- Purchase event + attribution snapshot: tek transaction
- Admin kullanıcı + rol atama: tek transaction

#### Isolation Level
- Varsayılan: READ COMMITTED
- Event kayıt işlemleri: SERIALIZABLE (idempotency için)
- Analitik sorgular: READ UNCOMMITTED (performans için)

#### Deadlock Önleme
- Tutarlı lock sırası: parent → child
- Kısa transaction süreleri (< 1 saniye)
- Optimistic locking (version field) kullanımı

## Test Stratejisi

### Test Piramidi

```
        ┌─────────────────┐
        │   E2E Tests     │  %10
        │   (Cypress)     │
        └─────────────────┘
       ┌───────────────────┐
       │ Integration Tests │  %30
       │   (Testcontainers)│
       └───────────────────┘
      ┌─────────────────────┐
      │    Unit Tests       │  %60
      │  (Jest/Vitest)      │
      └─────────────────────┘
```

### Unit Test Stratejisi

**Kapsam:**
- Servis katmanı iş mantığı
- Validasyon fonksiyonları
- Utility fonksiyonları
- DTO dönüşümleri

**Araçlar:**
- Test framework: Jest veya Vitest
- Mocking: jest.mock() veya vi.mock()
- Assertion: expect() API

**Örnek Test Senaryoları:**
- ContentService.createContent() geçersiz tip ile hata fırlatmalı
- EventService.recordContentEvent() duplicate event_id ile idempotent olmalı
- AdminAuthService.login() geçersiz şifre ile hata fırlatmalı
- CategoryService.addContent() PUBLISHED olmayan içerik ile hata fırlatmalı
- AssetProcessingService.startProcessing() PENDING olmayan durum ile hata fırlatmalı
- AssetProcessingService.generateCoverVariants() 4 varyant üretmeli

### Property-Based Test Stratejisi

**Kapsam:**
- Doğruluk özellikleri (36 özellik)
- İdempotency testleri
- Validasyon kuralları
- Veri bütünlüğü

**Araçlar:**
- Property-based testing library: fast-check (JavaScript/TypeScript)
- Minimum 100 iterasyon per test
- Random data generation

**Test Etiketleme:**
Her property test, ilgili tasarım özelliğine referans vermelidir:

```typescript
// Feature: tellpal-v2-backend, Property 13: İçerik Event İdempotency
test('duplicate event_id should be idempotent', async () => {
  await fc.assert(
    fc.asyncProperty(
      fc.uuid(),
      fc.integer({ min: 1 }),
      fc.integer({ min: 1 }),
      async (eventId, profileId, contentId) => {
        const event = {
          eventId,
          profileId,
          contentId,
          languageCode: 'tr',
          eventType: 'START',
          occurredAt: new Date(),
        };
        
        await eventService.recordContentEvent(event);
        await eventService.recordContentEvent(event); // Duplicate
        
        const count = await db.query(
          'SELECT COUNT(*) FROM v2_content_events WHERE event_id = $1',
          [eventId]
        );
        
        expect(count.rows[0].count).toBe('1');
      }
    ),
    { numRuns: 100 }
  );
});
```

**Property Test Örnekleri:**

1. **Özellik 1: Dil Bazlı İçerik Görünürlüğü**
```typescript
// Feature: tellpal-v2-backend, Property 1: Dil Bazlı İçerik Görünürlüğü
test('unpublished content should not appear in API responses', async () => {
  await fc.assert(
    fc.asyncProperty(
      fc.constantFrom('tr', 'en', 'es', 'pt', 'de'),
      fc.constantFrom('DRAFT', 'ARCHIVED'),
      async (lang, status) => {
        const content = await createTestContent();
        await createTestLocalization(content.id, lang, { status });
        
        const result = await contentService.listContents({ lang });
        
        expect(result.find(c => c.id === content.id)).toBeUndefined();
      }
    ),
    { numRuns: 100 }
  );
});
```

2. **Özellik 3: Story Sayfa Numarası Tutarlılığı**
```typescript
// Feature: tellpal-v2-backend, Property 3: Story Sayfa Numarası Tutarlılığı
test('story pages should start at 1 and match page_count', async () => {
  await fc.assert(
    fc.asyncProperty(
      fc.integer({ min: 1, max: 50 }),
      async (pageCount) => {
        const content = await createTestContent({ type: 'STORY' });
        const pages = Array.from({ length: pageCount }, (_, i) => ({
          pageNumber: i + 1,
          illustrationMediaId: 1,
        }));
        
        await contentService.createStoryPages(content.id, pages);
        
        const details = await db.query(
          'SELECT page_count FROM v2_content_story_details WHERE content_id = $1',
          [content.id]
        );
        const actualPages = await db.query(
          'SELECT COUNT(*) FROM v2_story_pages WHERE content_id = $1',
          [content.id]
        );
        
        expect(details.rows[0].page_count).toBe(pageCount);
        expect(parseInt(actualPages.rows[0].count)).toBe(pageCount);
      }
    ),
    { numRuns: 100 }
  );
});
```

3. **Özellik 19: Attribution Penceresi Mantığı**
```typescript
// Feature: tellpal-v2-backend, Property 19: Attribution Penceresi Mantığı
test('attribution should select most recent LOCKED_CONTENT_CLICKED or PAYWALL_SHOWN', async () => {
  await fc.assert(
    fc.asyncProperty(
      fc.integer({ min: 1, max: 48 }), // hours before purchase
      async (hoursBefore) => {
        const user = await createTestUser();
        const profile = await createTestProfile(user.id);
        
        const paywallEvent = await createTestAppEvent({
          profileId: profile.id,
          eventType: 'PAYWALL_SHOWN',
          occurredAt: new Date(Date.now() - hoursBefore * 3600000),
        });
        
        const purchaseEvent = await createTestPurchaseEvent({
          userId: user.id,
          occurredAt: new Date(),
        });
        
        const snapshot = await purchaseService.createAttributionSnapshot(
          purchaseEvent.id
        );
        
        if (hoursBefore <= 24) {
          expect(snapshot.attributedAppEventId).toBe(paywallEvent.eventId);
        } else {
          expect(snapshot.attributedAppEventId).toBeNull();
        }
      }
    ),
    { numRuns: 100 }
  );
});
```

4. **Özellik 37: Asset İşleme Durumu Tutarlılığı**
```typescript
// Feature: tellpal-v2-backend, Property 37: Asset İşleme Durumu Tutarlılığı
test('content with incomplete processing should not appear in mobile API', async () => {
  await fc.assert(
    fc.asyncProperty(
      fc.constantFrom('tr', 'en', 'es', 'pt', 'de'),
      fc.constantFrom('PENDING', 'PROCESSING', 'FAILED'),
      async (lang, processingStatus) => {
        const content = await createTestContent();
        await createTestLocalization(content.id, lang, { 
          status: 'PUBLISHED',
          processingStatus 
        });
        
        const result = await contentService.listContents({ lang });
        
        expect(result.find(c => c.id === content.id)).toBeUndefined();
      }
    ),
    { numRuns: 100 }
  );
});
```

5. **Özellik 40: Kapak Varyantları Tutarlılığı**
```typescript
// Feature: tellpal-v2-backend, Property 40: Kapak Varyantları Tutarlılığı
test('completed processing should have all four cover variants', async () => {
  await fc.assert(
    fc.asyncProperty(
      fc.constantFrom('tr', 'en', 'es', 'pt', 'de'),
      async (lang) => {
        const content = await createTestContent();
        const localization = await createTestLocalization(content.id, lang, {
          status: 'PUBLISHED',
          processingStatus: 'PENDING'
        });
        
        await assetProcessingService.startProcessing(content.id, lang);
        
        // Wait for processing to complete
        await waitForProcessingComplete(content.id, lang);
        
        const variants = await db.query(
          `SELECT kind FROM v2_media_assets 
           WHERE object_path LIKE $1 
           AND kind IN ('THUMBNAIL_PHONE', 'THUMBNAIL_TABLET', 'DETAIL_PHONE', 'DETAIL_TABLET')`,
          [`%/content/%/${content.externalKey}/${lang}/processed/%`]
        );
        
        expect(variants.rows).toHaveLength(4);
        expect(variants.rows.map(r => r.kind)).toContain('THUMBNAIL_PHONE');
        expect(variants.rows.map(r => r.kind)).toContain('THUMBNAIL_TABLET');
        expect(variants.rows.map(r => r.kind)).toContain('DETAIL_PHONE');
        expect(variants.rows.map(r => r.kind)).toContain('DETAIL_TABLET');
      }
    ),
    { numRuns: 100 }
  );
});
```


### Integration Test Stratejisi

**Kapsam:**
- API endpoint testleri
- Veritabanı entegrasyonu
- Firebase Storage entegrasyonu
- RevenueCat webhook işleme

**Araçlar:**
- Testcontainers: PostgreSQL container
- Supertest: HTTP assertion
- Firebase Admin SDK: Mock Firebase services

**Test Senaryoları:**
- Admin login flow (JWT üretme ve doğrulama)
- İçerik oluşturma ve yayınlama flow
- Kategori kürasyon flow
- Event batch sync flow
- RevenueCat webhook işleme flow
- Asset işleme ve paketleme flow (resim optimizasyonu, ses sıkıştırma, ZIP oluşturma)

**Örnek Integration Test:**
```typescript
describe('Content Management Flow', () => {
  let db: PostgresContainer;
  let app: Express;
  let adminToken: string;
  
  beforeAll(async () => {
    db = await new PostgresContainer().start();
    app = createApp({ dbUrl: db.getConnectionString() });
    adminToken = await loginAsAdmin();
  });
  
  test('should create content with localization and publish', async () => {
    // Create content
    const createResponse = await request(app)
      .post('/api/admin/contents')
      .set('Authorization', `Bearer ${adminToken}`)
      .send({
        type: 'STORY',
        externalKey: 'test-story-1',
        isPremium: false,
      })
      .expect(201);
    
    const contentId = createResponse.body.id;
    
    // Add Turkish localization
    await request(app)
      .post(`/api/admin/contents/${contentId}/localizations`)
      .set('Authorization', `Bearer ${adminToken}`)
      .send({
        languageCode: 'tr',
        title: 'Test Hikaye',
        description: 'Test açıklama',
        status: 'PUBLISHED',
      })
      .expect(201);
    
    // Verify content appears in Turkish API
    const listResponse = await request(app)
      .get('/api/contents?lang=tr')
      .expect(200);
    
    expect(listResponse.body.find(c => c.id === contentId)).toBeDefined();
    
    // Verify content does not appear in English API
    const enListResponse = await request(app)
      .get('/api/contents?lang=en')
      .expect(200);
    
    expect(enListResponse.body.find(c => c.id === contentId)).toBeUndefined();
  });
});
```

### E2E Test Stratejisi

**Kapsam:**
- Kritik kullanıcı akışları
- CMS admin panel akışları
- Mobil uygulama akışları

**Araçlar:**
- Cypress veya Playwright
- Test ortamı: staging environment

**Test Senaryoları:**
- Admin: İçerik oluşturma, düzenleme, yayınlama
- Admin: Kategori oluşturma ve içerik kürasyon
- Admin: Asset işleme tetikleme ve durum takibi
- Mobile: İçerik listeleme ve detay görüntüleme
- Mobile: ZIP paket indirme ve açma
- Mobile: Event tracking (offline ve online)
- Mobile: Satın alma flow (sandbox)

### Test Coverage Hedefleri

- **Unit Tests**: %80+ kod coverage
- **Property Tests**: Tüm 49 doğruluk özelliği
- **Integration Tests**: Tüm API endpoint'leri
- **E2E Tests**: Kritik kullanıcı akışları

### CI/CD Pipeline

```yaml
stages:
  - lint
  - unit-test
  - property-test
  - integration-test
  - build
  - e2e-test
  - deploy

unit-test:
  script:
    - npm run test:unit
    - npm run test:coverage
  coverage: '/Lines\s*:\s*(\d+\.\d+)%/'
  
property-test:
  script:
    - npm run test:property
  # Minimum 100 iterations per property
  
integration-test:
  services:
    - postgres:15
  script:
    - npm run test:integration
    
e2e-test:
  stage: e2e-test
  script:
    - npm run test:e2e
  environment:
    name: staging
```

### Test Data Yönetimi

**Seed Data:**
- Dil kayıtları (tr, en, es, pt, de)
- Admin roller (ADMIN, CONTENT_MANAGER, VIEWER)
- Lookup tabloları (event types, stores, vb.)

**Test Fixtures:**
- Test içerik şablonları
- Test kategori şablonları
- Test kullanıcı profilleri
- Test medya varlıkları

**Data Cleanup:**
- Her test sonrası transaction rollback
- Integration testlerde test-specific schema
- E2E testlerde test kullanıcı cleanup script

### Performance Testing

**Load Testing:**
- Apache JMeter veya k6
- Hedef: 1000 RPS (requests per second)
- Event endpoint: 5000 RPS (batch)

**Stress Testing:**
- Maksimum kapasite testi
- Graceful degradation kontrolü

**Endurance Testing:**
- 24 saat sürekli yük
- Memory leak kontrolü
- Connection pool kontrolü

### Monitoring ve Alerting

**Metrikler:**
- API response time (p50, p95, p99)
- Error rate (4xx, 5xx)
- Database query time
- Event ingestion rate
- Purchase webhook processing time
- Asset processing time (resim, ses, ZIP)
- Asset processing queue length

**Alertler:**
- Error rate > %5
- Response time p95 > 1s
- Database connection pool > %80
- Event queue backlog > 10000
- Asset processing failures > 10 per hour
- Asset processing queue > 100 items
