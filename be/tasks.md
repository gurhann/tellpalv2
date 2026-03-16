# Uygulama Planı: TellPal v2 Backend

## Genel Bakış

Spring Boot 4.0 + Spring Modulith tabanlı modüler monolith mimarisi. Tactical DDD pattern'leri ile `content`, `category`, `asset`, `event`, `purchase`, `user`, `admin`, `shared` modülleri. Görevler faz sırasına ve katman sırasına (DB → domain → application → infrastructure → presentation) göre düzenlenmiştir.

## Görevler

- [ ] 1. Proje İskeleti ve Shared Kernel
  - [ ] 1.1 Maven proje yapısını ve Spring Boot 4.0 / Spring Modulith bağımlılıklarını oluştur
    - `pom.xml` içinde Java 25, Spring Boot 4.0.0, Spring Modulith 1.3.0, PostgreSQL, Firebase Admin SDK, JJWT, jqwik, JUnit 5, Testcontainers bağımlılıklarını tanımla
    - `TellPalApplication.java` ana sınıfını oluştur
    - `application.yml` temel konfigürasyonunu oluştur (datasource, JPA, Flyway)
    - Docker Compose dosyasını oluştur (PostgreSQL 15 servisi)
    - _Gereksinimler: 17, 19_

  - [ ] 1.2 Shared kernel domain modellerini oluştur
    - `shared/domain/` altında `BaseEntity.java` (id, createdAt, updatedAt), `MediaAsset.java`, `MediaKind.java` enum, `Language.java`, `LocalizationStatus.java`, `ProcessingStatus.java` sınıflarını oluştur
    - `shared/infrastructure/persistence/BaseEntity.java` JPA base entity'sini oluştur
    - _Gereksinimler: 5, 22.17_

  - [ ] 1.3 Flyway migration altyapısını ve ilk DB şemasını oluştur
    - `V1__create_languages_table.sql`: `languages` tablosu
    - `V2__create_media_assets_table.sql`: `media_assets` tablosu (provider, object_path, kind, mime_type, bytes, checksum_sha256, download_url)
    - Unique constraint: (provider, object_path)
    - _Gereksinimler: 5.1, 5.2, 5.3, 17.2_

  - [ ] 1.4 Dil seed verisini oluştur
    - `V3__seed_languages.sql`: tr, en, es, pt, de dil kayıtlarını ekle (display_name, is_active=true)
    - `LanguageSeeder.java` veya Flyway migration ile seed
    - _Gereksinimler: 21.1, 21.2, 21.3, 21.4_

  - [ ]* 1.5 Özellik 32 için property testi yaz
    - **Özellik 32: Dil Seed Verisi Bütünlüğü**
    - **Doğrular: Gereksinim 21.1, 21.2, 21.3**
    - jqwik ile veritabanı başlatma sonrası tr/en/es/pt/de kayıtlarının varlığını ve is_active=true olduğunu doğrula


---

## Faz 1: CMS Backend

- [ ] 2. Admin Modülü — DB Şeması ve Domain
  - [ ] 2.1 Admin tabloları için Flyway migration oluştur
    - `V4__create_admin_tables.sql`: `admin_users` (username unique, password_hash, enabled, last_login_at), `admin_roles` (code PK, description), `admin_user_roles` (admin_user_id, role_code, created_at), `admin_refresh_tokens` (token_hash unique, issued_at, expires_at, revoked_at, replaced_by_token_hash, user_agent, ip)
    - Check constraint: `expires_at > issued_at`
    - _Gereksinimler: 13.1, 13.2, 13.5, 13.6, 14.1, 15.6, 15.7, 17.2, 17.5_

  - [ ] 2.2 Admin domain sınıflarını oluştur
    - `admin/domain/AdminUser.java` (JPA entity, bcrypt password_hash, enabled, lastLoginAt)
    - `admin/domain/AdminRole.java` (JPA entity, code PK)
    - `admin/domain/AdminUserRole.java` (JPA entity, composite key)
    - `admin/domain/AdminRefreshToken.java` (JPA entity, tokenHash, issuedAt, expiresAt, revokedAt, replacedByTokenHash)
    - `admin/domain/AdminRepository.java` interface
    - _Gereksinimler: 13, 14, 15_

  - [ ]* 2.3 Özellik 20 için property testi yaz
    - **Özellik 20: Admin Kullanıcı Adı Benzersizliği**
    - **Doğrular: Gereksinim 13.2**

  - [ ]* 2.4 Özellik 22 için property testi yaz
    - **Özellik 22: Refresh Token Hash Benzersizliği**
    - **Doğrular: Gereksinim 13.6**

  - [ ]* 2.5 Özellik 25 için property testi yaz
    - **Özellik 25: Token Süre Sonu Kısıtlaması**
    - **Doğrular: Gereksinim 15.7, 17.5**

- [ ] 3. Admin Modülü — Application ve Infrastructure
  - [ ] 3.1 JWT altyapısını oluştur
    - `admin/infrastructure/security/JwtTokenProvider.java`: access token (1 saat) ve refresh token (30 gün) üretimi, SHA-256 hash, doğrulama
    - `admin/infrastructure/security/JwtAuthenticationFilter.java`
    - _Gereksinimler: 13.3, 13.4, 13.5_

  - [ ] 3.2 AdminAuthApplicationService oluştur
    - `admin/application/AdminAuthApplicationService.java`: login (bcrypt doğrulama, JWT üretme, lastLoginAt güncelleme), refreshAccessToken (token rotasyonu, replaced_by_token_hash), logout (revoke), createAdminUser, assignRole, removeRole
    - _Gereksinimler: 13.3, 13.4, 13.7, 13.8, 13.9, 13.10, 14.2, 14.3, 14.4, 15.1, 15.2, 15.3, 15.4, 15.5_

  - [ ]* 3.3 Özellik 21 için property testi yaz
    - **Özellik 21: JWT Token Süre Sonu Validasyonu**
    - **Doğrular: Gereksinim 13.3**

  - [ ]* 3.4 Özellik 23 için property testi yaz
    - **Özellik 23: Refresh Token Rotasyon Zinciri**
    - **Doğrular: Gereksinim 15.2**

  - [ ]* 3.5 Özellik 24 için property testi yaz
    - **Özellik 24: İptal Edilmiş Token Reddi**
    - **Doğrular: Gereksinim 15.4**

- [ ] 4. Admin Modülü — Presentation Layer
  - [ ] 4.1 AdminAuthController oluştur
    - `presentation/api/admin/AdminAuthController.java`: `POST /api/admin/auth/login`, `POST /api/admin/auth/refresh`, `POST /api/admin/auth/logout`
    - Request/Response DTO'ları, exception handler
    - _Gereksinimler: 13, 15_

  - [ ] 4.2 AdminUserController oluştur
    - `presentation/api/admin/AdminUserController.java`: admin kullanıcı CRUD, rol atama/kaldırma endpoint'leri
    - JWT filter ile koruma, rol bazlı yetkilendirme
    - _Gereksinimler: 13.8, 14_

- [ ] 5. Checkpoint — Admin Auth testleri geçmeli
  - Tüm testlerin geçtiğini doğrula, sorular varsa kullanıcıya sor.

- [ ] 6. Content Modülü — DB Şeması
  - [ ] 6.1 Content tabloları için Flyway migration oluştur
    - `V5__create_content_tables.sql`:
      - `contents` (type enum, external_key unique, is_active, age_range, page_count)
      - `content_localizations` (content_id FK cascade, language_code FK, title, description, body_text, cover_media_id FK set null, audio_media_id FK set null, duration_minutes, status, processing_status, published_at)
      - `story_pages` (content_id FK cascade, page_number ≥ 1, illustration_media_id FK set null)
      - `story_page_localizations` (content_id FK cascade, page_number, language_code, text_content, audio_media_id FK set null)
      - `contributors` (display_name)
      - `content_contributors` (content_id FK cascade, contributor_id FK, role, language_code, credit_name, sort_order ≥ 0)
      - `content_free_access` (access_key, content_id FK cascade, language_code; unique: access_key+content_id+language_code)
    - Check constraints: page_number ≥ 1, sort_order ≥ 0, display_order ≥ 0
    - _Gereksinimler: 1, 2, 3, 5, 17.3, 17.4, 23.1_

  - [ ]* 6.2 Özellik 5 için property testi yaz
    - **Özellik 5: Harici Anahtar Benzersizliği**
    - **Doğrular: Gereksinim 2.9**

  - [ ]* 6.3 Özellik 10 için property testi yaz
    - **Özellik 10: Medya Varlık Benzersizliği**
    - **Doğrular: Gereksinim 5.3**

  - [ ]* 6.4 Özellik 49 için property testi yaz
    - **Özellik 49: ContentFreeAccess Benzersizliği**
    - **Doğrular: Gereksinim 23.1**

- [ ] 7. Content Modülü — Domain ve Application
  - [ ] 7.1 Content domain sınıflarını oluştur
    - `content/domain/Content.java` (aggregate root, ContentType enum, externalKey, isActive, ageRange, pageCount)
    - `content/domain/ContentLocalization.java` (status, processingStatus, publishedAt)
    - `content/domain/StoryPage.java`, `content/domain/StoryPageLocalization.java`
    - `content/domain/ContentType.java` enum (STORY, AUDIO_STORY, MEDITATION, LULLABY)
    - `content/domain/ContentRepository.java` interface
    - _Gereksinimler: 1, 2_

  - [ ] 7.2 Contributor domain sınıflarını oluştur
    - `content/domain/Contributor.java`, `content/domain/ContentContributor.java`
    - `content/domain/ContributorRole.java` enum (AUTHOR, ILLUSTRATOR, NARRATOR, MUSICIAN)
    - _Gereksinimler: 3_

  - [ ] 7.3 ContentApplicationService oluştur
    - `content/application/ContentApplicationService.java`: createContent, updateContent, getContent, listContents (is_active filtresi), createLocalization, updateLocalization
    - `content/application/ContentPublishingService.java`: publishLocalization (iş kuralı: STORY için sayfalar hazır olmalı, MEDITATION için body_text dolu olmalı, processing COMPLETED olmalı)
    - page_count otomatik güncelleme (sayfa ekle/çıkar)
    - _Gereksinimler: 1.3, 1.4, 2.5, 2.6, 20_

  - [ ] 7.4 ContributorApplicationService oluştur
    - `content/application/ContributorApplicationService.java`: CRUD, addContentContributor, removeContentContributor, sort_order yönetimi
    - _Gereksinimler: 3_

  - [ ]* 7.5 Özellik 1 için property testi yaz
    - **Özellik 1: Dil Bazlı İçerik Görünürlüğü**
    - **Doğrular: Gereksinim 1.3, 1.4, 1.5**

  - [ ]* 7.6 Özellik 3 için property testi yaz
    - **Özellik 3: Story Sayfa Numarası Tutarlılığı**
    - **Doğrular: Gereksinim 2.2, 2.4, 2.5**

  - [ ]* 7.7 Özellik 4 için property testi yaz
    - **Özellik 4: İçerik Tipi Özel Alan Validasyonu**
    - **Doğrular: Gereksinim 2.6**

  - [ ]* 7.8 Özellik 6 için property testi yaz
    - **Özellik 6: Contributor Rol Sıralama Tutarlılığı**
    - **Doğrular: Gereksinim 3.4, 3.5**

  - [ ]* 7.9 Özellik 47 için property testi yaz
    - **Özellik 47: page_count Tutarlılığı**
    - **Doğrular: Gereksinim 2.5**

  - [ ]* 7.10 Özellik 48 için property testi yaz
    - **Özellik 48: STORY Dışı İçerikte page_count Null Olmalı**
    - **Doğrular: Gereksinim 2.1**

  - [ ]* 7.11 Özellik 31 için property testi yaz
    - **Özellik 31: Soft Delete Davranışı**
    - **Doğrular: Gereksinim 20.3, 20.4, 20.5**


- [ ] 8. Category Modülü — DB Şeması, Domain ve Application
  - [ ] 8.1 Category tabloları için Flyway migration oluştur
    - `V6__create_category_tables.sql`:
      - `categories` (slug unique, type enum CONTENT/PARENT_GUIDANCE, is_premium, is_active)
      - `category_localizations` (category_id FK cascade, language_code FK, name, description, image_media_id FK set null, status, published_at)
      - `category_contents` (category_id FK cascade, language_code, content_id FK cascade, display_order ≥ 0; unique: category_id+language_code+content_id)
    - _Gereksinimler: 4, 17.3_

  - [ ] 8.2 Category domain sınıflarını oluştur
    - `category/domain/Category.java` (aggregate root, slug, CategoryType enum, isActive)
    - `category/domain/CategoryLocalization.java` (status, publishedAt)
    - `category/domain/CategoryContent.java` (displayOrder)
    - `category/domain/CategoryRepository.java` interface
    - _Gereksinimler: 4_

  - [ ] 8.3 CategoryApplicationService ve CurationService oluştur
    - `category/application/CategoryApplicationService.java`: CRUD, createLocalization, updateLocalization, publishLocalization
    - `category/application/CurationService.java`: addContent (dil bazlı), updateContentOrder, removeContent, getCategoryContents (PUBLISHED filtreli, display_order sıralı)
    - _Gereksinimler: 4.5, 4.6, 4.7, 4.8, 4.9, 20_

  - [ ]* 8.4 Özellik 7 için property testi yaz
    - **Özellik 7: Kategori Dil Bazlı Görünürlük**
    - **Doğrular: Gereksinim 4.5, 4.6**

  - [ ]* 8.5 Özellik 8 için property testi yaz
    - **Özellik 8: Kategori İçerik Kürasyon Tutarlılığı**
    - **Doğrular: Gereksinim 4.7, 4.9**

  - [ ]* 8.6 Özellik 9 için property testi yaz
    - **Özellik 9: Display Order Benzersizliği**
    - **Doğrular: Gereksinim 4.8**

- [ ] 9. MediaAsset Modülü — Infrastructure ve Firebase Storage
  - [ ] 9.1 Firebase Storage entegrasyonunu oluştur
    - `shared/infrastructure/firebase/FirebaseStorageService.java`: upload, getDownloadUrl, delete
    - Firebase Admin SDK konfigürasyonu (`FirebaseConfig.java`)
    - _Gereksinimler: 5_

  - [ ] 9.2 MediaAsset application service oluştur
    - `shared/application/MediaAssetService.java` (veya content modülü içinde): createMediaAsset, getMediaAsset, deleteMediaAsset, updateDownloadUrlCache, calculateChecksum
    - _Gereksinimler: 5.1, 5.2, 5.4, 5.5, 5.6_

  - [ ]* 9.3 Özellik 2 için property testi yaz
    - **Özellik 2: İçerik Yerelleştirme Bütünlüğü**
    - **Doğrular: Gereksinim 1.2**

- [ ] 10. A/B Test Ücretsiz Erişim Yönetimi
  - [ ] 10.1 ContentFreeAccess application service oluştur
    - `content/application/FreeAccessService.java`: addFreeAccess, removeFreeAccess, listFreeAccessByKey, resolveFreeKey (bilinmeyen key → default fallback), isFree(contentId, lang, freeKey) hesaplama
    - _Gereksinimler: 23.2, 23.3, 23.4, 23.5, 23.6, 23.7, 23.8, 23.9_

  - [ ]* 10.2 Özellik 43 için property testi yaz
    - **Özellik 43: Ücretsiz Erişim Varsayılan Key Bütünlüğü**
    - **Doğrular: Gereksinim 23.2, 23.4**

  - [ ]* 10.3 Özellik 44 için property testi yaz
    - **Özellik 44: Bilinmeyen Key Fallback**
    - **Doğrular: Gereksinim 23.5**

  - [ ]* 10.4 Özellik 45 için property testi yaz
    - **Özellik 45: Ücretsiz Erişim Dil Bazlı Tutarlılığı**
    - **Doğrular: Gereksinim 23.6, 23.7**

  - [ ]* 10.5 Özellik 46 için property testi yaz
    - **Özellik 46: isFree Hesaplama Tutarlılığı**
    - **Doğrular: Gereksinim 23.10**

- [ ] 11. CMS Admin Presentation Layer
  - [ ] 11.1 ContentAdminController oluştur
    - `presentation/api/admin/ContentAdminController.java`
    - `GET/POST /api/admin/contents`, `GET/PUT/DELETE /api/admin/contents/{id}`
    - `POST /api/admin/contents/{id}/localizations`, `PUT/DELETE /api/admin/contents/{id}/localizations/{lang}`
    - Story sayfa yönetimi endpoint'leri
    - _Gereksinimler: 1, 2, 20_

  - [ ] 11.2 CategoryAdminController oluştur
    - `presentation/api/admin/CategoryAdminController.java`
    - `GET/POST /api/admin/categories`, `GET/PUT/DELETE /api/admin/categories/{id}`
    - Localization ve kürasyon endpoint'leri (`POST/PUT/DELETE /api/admin/categories/{id}/contents`)
    - _Gereksinimler: 4, 20_

  - [ ] 11.3 ContributorAdminController ve MediaAdminController oluştur
    - `presentation/api/admin/ContributorAdminController.java`: CRUD endpoint'leri
    - `presentation/api/admin/MediaAdminController.java`: `POST /api/admin/media/upload`, `GET/DELETE /api/admin/media/{id}`
    - FreeAccess yönetim endpoint'leri
    - _Gereksinimler: 3, 5, 23.8, 23.9_

  - [ ]* 11.4 Özellik 26 için integration testi yaz
    - **Özellik 26: Foreign Key Bütünlüğü**
    - **Doğrular: Gereksinim 17.1, 17.6, 17.7**
    - Testcontainers ile cascade delete ve set null davranışlarını doğrula

  - [ ]* 11.5 Özellik 27 için property testi yaz
    - **Özellik 27: Negatif Olmayan Değer Kısıtlamaları**
    - **Doğrular: Gereksinim 17.3**

  - [ ]* 11.6 Özellik 28 için property testi yaz
    - **Özellik 28: Sayfa Numarası Başlangıç Validasyonu**
    - **Doğrular: Gereksinim 17.4**

  - [ ]* 11.7 Özellik 29 için property testi yaz
    - **Özellik 29: Zaman Damgası UTC Tutarlılığı**
    - **Doğrular: Gereksinim 19.1**

- [ ] 12. Checkpoint — Faz 1 testleri geçmeli
  - Tüm testlerin geçtiğini doğrula, sorular varsa kullanıcıya sor.


---

## Faz 3: Mobil Uygulama Entegrasyonu

- [ ] 13. User Modülü — DB Şeması ve Domain
  - [ ] 13.1 User tabloları için Flyway migration oluştur
    - `V7__create_user_tables.sql`:
      - `app_users` (firebase_uid unique, is_allow_marketing)
      - `user_profiles` (user_id FK cascade, name, age_range, avatar_media_id FK set null, favorite_genres text[], main_purposes text[], is_primary)
    - Partial unique index: `CREATE UNIQUE INDEX ON user_profiles (user_id) WHERE is_primary = true`
    - _Gereksinimler: 6.2, 6.3, 6.6, 17.2, 17.8_

  - [ ] 13.2 User domain sınıflarını oluştur
    - `user/domain/AppUser.java` (firebaseUid, isAllowMarketing)
    - `user/domain/UserProfile.java` (name, ageRange, favoriteGenres, mainPurposes, isPrimary)
    - `user/domain/UserRepository.java` interface
    - _Gereksinimler: 6_

  - [ ]* 13.3 Özellik 11 için property testi yaz
    - **Özellik 11: Firebase UID Benzersizliği**
    - **Doğrular: Gereksinim 6.2**

  - [ ]* 13.4 Özellik 12 için property testi yaz
    - **Özellik 12: Birincil Profil Tekilliği**
    - **Doğrular: Gereksinim 6.3, 6.6**

- [ ] 14. User Modülü — Application ve Firebase Auth
  - [ ] 14.1 Firebase Auth entegrasyonunu oluştur
    - `user/infrastructure/firebase/FirebaseAuthService.java`: Firebase ID token doğrulama, UID çıkarma
    - `FirebaseAuthFilter.java`: public API endpoint'leri için Firebase token doğrulama
    - _Gereksinimler: 6.1_

  - [ ] 14.2 UserApplicationService oluştur
    - `user/application/UserApplicationService.java`: registerOrGetUser (firebase_uid ile upsert, varsayılan profil oluşturma), getUser, updateProfile, listProfiles
    - _Gereksinimler: 6.1, 6.3, 6.4, 6.5, 6.7_

- [ ] 15. Public API — İçerik ve Kategori Endpoint'leri
  - [ ] 15.1 ContentPublicController oluştur
    - `presentation/api/public/ContentPublicController.java`
    - `GET /api/contents?lang={lang}&type={type}&freeKey={key}`: PUBLISHED + processing_status=COMPLETED filtreli, isFree hesaplamalı
    - `GET /api/contents/{id}?lang={lang}`: detay, ZIP referansları ve kapak varyantları dahil
    - `GET /api/contents/{id}/pages?lang={lang}`: story sayfaları
    - _Gereksinimler: 1.3, 22.18, 22.19, 23.3, 23.4, 23.10_

  - [ ] 15.2 CategoryPublicController oluştur
    - `presentation/api/public/CategoryPublicController.java`
    - `GET /api/categories?lang={lang}&type={type}`: PUBLISHED filtreli
    - `GET /api/categories/{slug}?lang={lang}`: detay
    - `GET /api/categories/{slug}/contents?lang={lang}&freeKey={key}`: display_order sıralı, PUBLISHED+COMPLETED içerikler
    - _Gereksinimler: 4.5, 4.9_

  - [ ] 15.3 ProfileController oluştur
    - `presentation/api/public/ProfileController.java`
    - `POST /api/auth/register`, `GET /api/auth/me`
    - `GET/POST /api/profiles`, `GET/PUT /api/profiles/{id}`
    - Firebase Auth token ile korumalı
    - _Gereksinimler: 6_

  - [ ]* 15.4 Özellik 37 için property testi yaz
    - **Özellik 37: Asset İşleme Durumu Tutarlılığı**
    - **Doğrular: Gereksinim 22.18**

- [ ] 16. Firebase Migration Desteği
  - [ ] 16.1 Migration script altyapısını oluştur
    - `user/application/FirebaseMigrationService.java`: importUser (firebase_uid ile idempotent upsert), importContentEvent (legacy_event_key ile), importAppEvent (legacy_event_key ile), event tipi eşleme (START_CONTENT→START, LEFT_CONTENT→EXIT, FINISH_CONTENT→COMPLETE)
    - _Gereksinimler: 18.1, 18.2, 18.3, 18.4, 18.5, 18.6, 18.7_

  - [ ]* 16.2 Özellik 33 için property testi yaz
    - **Özellik 33: Firebase Migration İdempotency**
    - **Doğrular: Gereksinim 18.1**

  - [ ]* 16.3 Özellik 34 için property testi yaz
    - **Özellik 34: Firebase Event Tipi Eşleme**
    - **Doğrular: Gereksinim 18.4**

- [ ] 17. Checkpoint — Faz 3 testleri geçmeli
  - Tüm testlerin geçtiğini doğrula, sorular varsa kullanıcıya sor.


---

## Faz 4: RevenueCat Entegrasyonu

- [ ] 18. Purchase Modülü — DB Şeması ve Lookup Tabloları
  - [ ] 18.1 Purchase lookup tabloları için Flyway migration oluştur
    - `V8__create_purchase_lookup_tables.sql`:
      - `purchase_event_types` (code PK, description, is_active): INITIAL_PURCHASE, RENEWAL, CANCELLATION, EXPIRATION, UNCANCELLATION, BILLING_ISSUE, PRODUCT_CHANGE, TRANSFER, SUBSCRIPTION_PAUSED, SUBSCRIPTION_EXTENDED, TEMPORARY_ENTITLEMENT_GRANT
      - `subscription_period_types` (code PK): TRIAL, INTRO, NORMAL, PROMOTIONAL, PREPAID
      - `purchase_stores` (code PK): APP_STORE, PLAY_STORE, STRIPE, RC_BILLING, AMAZON
      - `purchase_environments` (code PK): SANDBOX, PRODUCTION
      - `purchase_reason_codes` (code PK, reason_type: CANCEL_REASON/EXPIRATION_REASON, is_active)
    - Seed data: tüm geçerli değerleri ekle
    - _Gereksinimler: 12.1, 12.2, 12.3, 12.4, 12.5_

  - [ ] 18.2 Purchase ana tabloları için Flyway migration oluştur
    - `V9__create_purchase_tables.sql`:
      - `subscription_products` (store, product_id; unique: store+product_id, product_type, billing_period_unit, billing_period_count, entitlement_ids jsonb, is_active)
      - `purchase_events` (user_id FK, occurred_at, ingested_at, source, event_type FK, product_id, store FK, currency, price, revenuecat_event_id unique, raw_payload jsonb, tüm analytics alanları)
      - `purchase_context_snapshots` (purchase_event_id unique FK, user_id FK, profile_id FK set null, attribution_window_seconds, attributed_app_event_id, attributed_content_id FK set null, profile_snapshot jsonb)
    - _Gereksinimler: 9, 10, 11, 12.7_

  - [ ]* 18.3 Özellik 16 için property testi yaz
    - **Özellik 16: Abonelik Ürün Benzersizliği**
    - **Doğrular: Gereksinim 9.1**

  - [ ]* 18.4 Özellik 17 için property testi yaz
    - **Özellik 17: Purchase Event İdempotency**
    - **Doğrular: Gereksinim 10.2**

  - [ ]* 18.5 Özellik 18 için property testi yaz
    - **Özellik 18: Purchase Context Snapshot Tekilliği**
    - **Doğrular: Gereksinim 11.2**

  - [ ]* 18.6 Özellik 35 için property testi yaz
    - **Özellik 35: Purchase Event Lookup Validasyonu**
    - **Doğrular: Gereksinim 12.1, 12.2, 12.3, 12.4, 12.5, 12.7**

- [ ] 19. Purchase Modülü — Domain ve Application
  - [ ] 19.1 Purchase domain sınıflarını oluştur
    - `purchase/domain/SubscriptionProduct.java`, `purchase/domain/PurchaseEvent.java`, `purchase/domain/PurchaseContextSnapshot.java`
    - `purchase/domain/PurchaseRepository.java` interface
    - _Gereksinimler: 9, 10, 11_

  - [ ] 19.2 PurchaseApplicationService oluştur
    - `purchase/application/PurchaseApplicationService.java`: recordPurchaseEvent (idempotent: revenuecat_event_id), createProduct, updateProduct
    - _Gereksinimler: 9, 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8, 10.9, 10.10_

  - [ ] 19.3 AttributionService oluştur
    - `purchase/application/AttributionService.java`: createSnapshot (24h window, LOCKED_CONTENT_CLICKED önce, yoksa PAYWALL_SHOWN, profil snapshot)
    - _Gereksinimler: 11.1, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8_

  - [ ]* 19.4 Özellik 19 için property testi yaz
    - **Özellik 19: Attribution Penceresi Mantığı**
    - **Doğrular: Gereksinim 11.5, 11.6**

- [ ] 20. RevenueCat Webhook Infrastructure
  - [ ] 20.1 RevenueCat webhook controller ve signature doğrulama oluştur
    - `presentation/api/webhook/RevenueCatWebhookController.java`: `POST /api/webhooks/revenuecat`
    - `purchase/infrastructure/revenuecat/RevenueCatSignatureValidator.java`: HMAC-SHA256 signature doğrulama
    - Raw payload JSON olarak sakla
    - _Gereksinimler: 10.1, 10.4_

- [ ] 21. Checkpoint — Faz 4 testleri geçmeli
  - Tüm testlerin geçtiğini doğrula, sorular varsa kullanıcıya sor.


---

## Faz 5: Analitik ve Event Tracking

- [ ] 22. Event Modülü — DB Şeması
  - [ ] 22.1 Event tabloları için Flyway migration oluştur
    - `V10__create_event_tables.sql`:
      - `content_events` (event_id UUID PK, profile_id FK, content_id FK, language_code, event_type, occurred_at, ingested_at, session_id, left_page ≥ 0, engagement_seconds ≥ 0, metadata jsonb, legacy_event_key)
      - `app_events` (event_id UUID PK, profile_id FK, event_type, content_id FK set null, occurred_at, ingested_at, payload jsonb, legacy_event_key)
      - Partial unique index: `(profile_id, legacy_event_key) WHERE legacy_event_key IS NOT NULL` — her iki tablo için
    - _Gereksinimler: 7, 8, 17.3, 17.8_

  - [ ] 22.2 Analitik indekslerini oluştur
    - `V11__create_event_indexes.sql`:
      - `content_events`: (profile_id, occurred_at), (content_id, occurred_at), (event_type, occurred_at), (session_id)
      - `app_events`: (profile_id, occurred_at), (content_id, occurred_at), (event_type, occurred_at)
      - `purchase_events`: (user_id, occurred_at)
      - `purchase_context_snapshots`: (user_id, created_at)
    - _Gereksinimler: 16.1, 16.2, 16.3, 16.4, 16.5, 16.6, 16.7, 16.8, 16.9_

  - [ ]* 22.3 Özellik 36 için property testi yaz
    - **Özellik 36: Analitik İndeks Performansı**
    - **Doğrular: Gereksinim 16.1, 16.2, 16.3, 16.5, 16.6, 16.7**
    - EXPLAIN ANALYZE ile indeks kullanımını doğrula

- [ ] 23. Event Modülü — Domain ve Application
  - [ ] 23.1 Event domain sınıflarını oluştur
    - `event/domain/ContentEvent.java` (eventId UUID PK, profileId, contentId, languageCode, ContentEventType enum, occurredAt, ingestedAt, sessionId, leftPage, engagementSeconds, metadata, legacyEventKey)
    - `event/domain/AppEvent.java` (eventId UUID PK, profileId, AppEventType enum, contentId, occurredAt, ingestedAt, payload, legacyEventKey)
    - `event/domain/ContentEventType.java` enum (START, EXIT, COMPLETE)
    - `event/domain/AppEventType.java` enum (APP_OPENED, ONBOARDING_STARTED, ONBOARDING_COMPLETED, ONBOARDING_SKIPPED, PAYWALL_SHOWN, LOCKED_CONTENT_CLICKED)
    - `event/domain/EventRepository.java` interface
    - _Gereksinimler: 7, 8_

  - [ ] 23.2 EventApplicationService oluştur
    - `event/application/EventApplicationService.java`: recordContentEvent (idempotent: event_id PK), recordAppEvent (idempotent: event_id PK), recordBatchEvents (mixed batch), legacy_event_key benzersizlik kontrolü
    - _Gereksinimler: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8_

  - [ ]* 23.3 Özellik 13 için property testi yaz
    - **Özellik 13: İçerik Event İdempotency**
    - **Doğrular: Gereksinim 7.2**

  - [ ]* 23.4 Özellik 14 için property testi yaz
    - **Özellik 14: Legacy Event Key Benzersizliği**
    - **Doğrular: Gereksinim 7.8, 18.5**

  - [ ]* 23.5 Özellik 15 için property testi yaz
    - **Özellik 15: Uygulama Event İdempotency**
    - **Doğrular: Gereksinim 8.2**

  - [ ]* 23.6 Özellik 30 için property testi yaz
    - **Özellik 30: Event Zaman Ayrımı**
    - **Doğrular: Gereksinim 19.5, 19.6**

- [ ] 24. Event Tracking Presentation Layer
  - [ ] 24.1 EventController oluştur
    - `presentation/api/public/EventController.java`
    - `POST /api/events/content`: tek içerik eventi
    - `POST /api/events/app`: tek uygulama eventi
    - `POST /api/events/batch`: karışık batch (offline-first sync)
    - Firebase Auth token ile korumalı, ingested_at server tarafında set edilmeli
    - _Gereksinimler: 7, 8_

- [ ] 25. Asset Processing Modülü — Domain ve Application
  - [ ] 25.1 Asset processing domain sınıflarını oluştur
    - `asset/domain/AssetProcessing.java` (aggregate root, contentId, languageCode, ProcessingStatus, startedAt, completedAt, errorMessage)
    - `asset/domain/ProcessingStatus.java` value object (PENDING→PROCESSING→COMPLETED/FAILED geçiş validasyonu)
    - `asset/domain/AssetProcessingRepository.java` interface
    - _Gereksinimler: 22.1, 22.3, 22.13, 22.14_

  - [ ] 25.2 ImageOptimizationService ve AudioOptimizationService oluştur
    - `asset/application/ImageOptimizationService.java`: 4 kapak varyantı üretimi (THUMBNAIL_PHONE, THUMBNAIL_TABLET, DETAIL_PHONE, DETAIL_TABLET)
    - `asset/application/AudioOptimizationService.java`: ses sıkıştırma ve format dönüşümü
    - _Gereksinimler: 22.6, 22.7, 22.8_

  - [ ] 25.3 ZipPackagingService oluştur
    - `asset/application/ZipPackagingService.java`: STORY için 2 ZIP (part1: ilk 3 sayfa, part2: kalan), diğerleri için 1 ZIP; dosya isimlendirme: `1.jpg`, `1.mp3`, `2.jpg`, `2.mp3` vb.
    - _Gereksinimler: 22.9, 22.10, 22.11, 22.12_

  - [ ] 25.4 AssetProcessingApplicationService oluştur
    - `asset/application/AssetProcessingApplicationService.java`: startProcessing (async, @Async), getProcessingStatus, retryProcessing (FAILED → PENDING → PROCESSING)
    - Firebase Storage'a upload: `/content/{type}/{key}/{lang}/processed/` ve `/packages/`
    - MediaAsset kayıtları oluştur (THUMBNAIL_*, DETAIL_*, OPTIMIZED_AUDIO, CONTENT_ZIP*)
    - _Gereksinimler: 22.2, 22.3, 22.4, 22.5, 22.13, 22.14, 22.15, 22.16, 22.20_

  - [ ]* 25.5 Özellik 38 için property testi yaz
    - **Özellik 38: ZIP Paket Varyant Tutarlılığı**
    - **Doğrular: Gereksinim 22.9**

  - [ ]* 25.6 Özellik 39 için property testi yaz
    - **Özellik 39: Tek ZIP Paket Tutarlılığı**
    - **Doğrular: Gereksinim 22.10**

  - [ ]* 25.7 Özellik 40 için property testi yaz
    - **Özellik 40: Kapak Varyantları Tutarlılığı**
    - **Doğrular: Gereksinim 22.6**

  - [ ]* 25.8 Özellik 41 için property testi yaz
    - **Özellik 41: Firebase Storage Yol Yapısı**
    - **Doğrular: Gereksinim 22.5**

  - [ ]* 25.9 Özellik 42 için property testi yaz
    - **Özellik 42: İşlem Durumu Geçiş Validasyonu**
    - **Doğrular: Gereksinim 22.3, 22.13, 22.14**

- [ ] 26. Asset Processing Presentation Layer
  - [ ] 26.1 AssetProcessingController oluştur
    - `presentation/api/admin/AssetProcessingController.java`
    - `POST /api/admin/contents/{id}/localizations/{lang}/process-assets`: işlemi manuel tetikle
    - `GET /api/admin/contents/{id}/localizations/{lang}/processing-status`: durum sorgula
    - _Gereksinimler: 22.2, 22.15_

- [ ] 27. Spring Modulith Modül Sınırlarını Doğrula
  - [ ] 27.1 Modül bağımlılık testlerini oluştur
    - `ApplicationModulesTest.java`: `ApplicationModules.of(TellPalApplication.class).verify()` ile tüm modül sınırlarını doğrula
    - Her modül için `@ApplicationModuleTest` ile izole test
    - _Gereksinimler: Mimari kısıtlar_

- [ ] 28. Final Checkpoint — Tüm testler geçmeli
  - Tüm unit, property ve integration testlerin geçtiğini doğrula, sorular varsa kullanıcıya sor.

## Notlar

- `*` ile işaretli görevler isteğe bağlıdır, MVP için atlanabilir
- Her görev ilgili gereksinimlere referans verir
- Property testleri jqwik ile yazılır, minimum 100 iterasyon
- Integration testleri Testcontainers ile PostgreSQL 15 container kullanır
- Checkpoint görevleri artımlı doğrulama sağlar
- Faz 2 (CMS Ön Yüz) backend görevi içermediğinden bu planda yer almaz
