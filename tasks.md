# TellPal v2 Backend Detailed Task Backlog

Bu dosya, mevcut repo durumuna gore sifirdan uygulanabilir backend backlog'udur.
`be/` dizini henuz mevcut olmadigi icin onceki planlarda gecen hicbir feature veya task uygulanmis kabul edilmez.
Bu nedenle bu dosyadaki tum task'lar varsayilan olarak `TODO` durumundadir.

Bu backlog su kaynaklarla hizalidir:

- `architecture.md`
- `.kiro/specs/tellpal-v2-backend/requirements.md`
- `.kiro/specs/tellpal-v2-backend/design.md`

## Mevcut Gercek Durum

- Repo su anda agirlikli olarak dokuman ve spec dosyalarindan olusuyor.
- `be/` altinda Spring Boot backend yok.
- Flyway migration zinciri yok.
- JPA domain modeli yok.
- Modulith package yapisi yok.
- Calisan test tabani yok.
- Bu nedenle hicbir feature `DONE` sayilmaz.

## Planlama Kurallari

- Her task tek bir coding turunda bitirilebilecek kadar dar olmali.
- Tercih edilen sira: schema -> domain -> repository -> application -> web -> tests.
- Her top-level package `com.tellpal.v2.<module>` bir Spring Modulith modulu olarak ele alinmali.
- Moduller arasi erisim sadece `api` paketleri, domain ID'leri veya application event'leri ile yapilmali.
- Baska modulun entity sinifina dogrudan bagimlilik kurulmamali.
- Ayrik bir `presentation` top-level paketi acilmamali.
- `shared` modulu yalnizca stabil ortak taslar icermeli; modul-spesifik entity veya service tasimamali.
- Bir task fazla genisliyorsa ayni feature icinde daha kucuk task'lara bolunmeli.
- Verification komutlari `be/` dizininden calistirilacak sekilde yazilmali.

## Feature Roadmap

| Feature | Hedef Cikti | Ana Moduller | Bagimliliklar | Durum |
|---|---|---|---|---|
| `F01` | Maven wrapper ile calisan backend iskeleti, temel config, Flyway baslangici ve test tabani | platform, shared | Yok | `DONE` |
| `F02` | Admin identity, rol modeli, JWT auth ve refresh token rotasyonu | admin, shared | `F01` | `TODO` |
| `F03` | Asset registry, storage abstraction ve media metadata sahipligi | asset, shared | `F01` | `TODO` |
| `F04` | Content write model, localizations, story pages ve contributor baglari | content, shared, asset.api | `F01`, `F03` | `TODO` |
| `F05` | Category write model ve dil bazli curation | category, content.api, asset.api | `F01`, `F03`, `F04` | `TODO` |
| `F06` | CMS admin API'leri ve admin-web adapter tabani | admin, content, category, asset | `F02`, `F03`, `F04`, `F05` | `TODO` |
| `F07` | Editorial publication policy ve A/B free-access yonetimi | content, category, admin web | `F04`, `F06` | `TODO` |
| `F08` | Mobile user ve profile yonetimi, Firebase UID esleme | user, shared, asset.api | `F01`, `F03` | `TODO` |
| `F09` | Offline-first event ingest ve analytics index tabani | event, user.api, content.api | `F04`, `F08` | `TODO` |
| `F10` | RevenueCat product catalog, webhook ingest ve attribution | purchase, user.api, event.api, content.api | `F04`, `F08`, `F09` | `TODO` |
| `F11` | Asset processing worker, packaging ve readiness akisi | asset, content.api, admin web | `F03`, `F04`, `F06`, `F07` | `TODO` |
| `F12` | Public content/category delivery API'leri | content, category, asset.api | `F05`, `F07`, `F11` | `TODO` |
| `F13` | Firebase legacy migration ve import akislari | user, event | `F08`, `F09` | `TODO` |
| `F14` | Hardening, modulith boundary sertlestirme ve release readiness | tum moduller | `F02`-`F13` | `TODO` |

## Global Exit Criteria

Bir feature ancak su durumda kapanabilir:

- ilgili feature altindaki tum task'lar `DONE` olmus olmali
- ilgili migration, service ve API katmanlari calisir halde olmali
- feature icin belirtilen verification adimlari basarili olmali
- modul sinirlari ihlal edilmemeli
- sonraki feature'i bloke eden acik teknik karar kalmamali

## Detailed Feature Tasks

### F01 Backend Foundation

- Outcome:
  - `be/` altinda calisan Spring Boot + Spring Modulith backend iskeleti, temel config, ilk Flyway migration'lari ve test tabani olusur.
- Depends on:
  - Yok
- Exit criteria:
  - `./mvnw test` en az smoke ve migration seviyesinde calisir.

#### Atomic Tasks

- `F01-T01` `DONE`
  - Objective: Maven wrapper tabanli backend modulu ve cekirdek bagimliliklari olusturmak.
  - Files/modules: `be/pom.xml`, `be/mvnw`, `be/mvnw.cmd`, `be/.mvn/wrapper/**`
  - Dependencies: Yok
  - Done when: Spring Boot, Spring Modulith, Flyway, PostgreSQL, JUnit 5, jqwik ve Testcontainers bagimliliklari `be/pom.xml` icinde tanimli olur.
  - Verify: `cd be && ./mvnw -q -DskipTests help:effective-pom`

- `F01-T02` `DONE`
  - Objective: Uygulama bootstrap'i ve top-level modul package iskeletini kurmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/TellPalApplication.java`, `be/src/main/java/com/tellpal/v2/{shared,admin,asset,content,category,user,event,purchase}/package-info.java`
  - Dependencies: `F01-T01`
  - Done when: Modul paketleri mimari ile hizali olusur ve top-level `presentation` paketi yaratilmaz.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F01-T03` `DONE`
  - Objective: Local runtime config, Docker Compose ve environment orneklerini eklemek.
  - Files/modules: `be/src/main/resources/application.yml`, `be/src/main/resources/application-local.yml`, `be/compose.yml`, `be/.env.example`
  - Dependencies: `F01-T01`
  - Done when: PostgreSQL, JPA, Flyway ve local secret/property yukleme yolu tanimlanir.
  - Verify: `cd be && docker compose -f compose.yml config`

- `F01-T04` `DONE`
  - Objective: Flyway baseline ve `languages` seed zincirini olusturmak.
  - Files/modules: `be/src/main/resources/db/migration/V1__create_languages_table.sql`, `be/src/main/resources/db/migration/V2__seed_languages.sql`
  - Dependencies: `F01-T03`
  - Done when: `languages` tablosu ve `tr`, `en`, `es`, `pt`, `de` seed verileri olusur.
  - Verify: `cd be && ./mvnw flyway:migrate`

- `F01-T05` `DONE`
  - Objective: `shared` modul icin minimum ortak persistence ve dil yapilarini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/shared/domain/LanguageCode.java`, `be/src/main/java/com/tellpal/v2/shared/infrastructure/persistence/**`
  - Dependencies: `F01-T02`
  - Done when: `shared` icinde yalnizca `LanguageCode`, audit/persistence base ve benzeri stabil yapilar bulunur.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F01-T06` `DONE`
  - Objective: Test runtime config'i, smoke test ve Testcontainers base sinifini kurmak.
  - Files/modules: `be/src/test/resources/application-test.yml`, `be/src/test/java/com/tellpal/v2/TellPalApplicationSmokeTest.java`, `be/src/test/java/com/tellpal/v2/support/PostgresIntegrationTestBase.java`
  - Dependencies: `F01-T01`, `F01-T03`
  - Done when: Spring context test profili ile ayaga kalkar ve PostgreSQL tabanli integration test kalibi hazir olur.
  - Verify: `cd be && ./mvnw -q -Dtest=TellPalApplicationSmokeTest test`

- `F01-T07` `DONE`
  - Objective: Modulith boundary smoke testi ve dokumantasyon plugin tabanini eklemek.
  - Files/modules: `be/pom.xml`, `be/src/test/java/com/tellpal/v2/ApplicationModulesTest.java`
  - Dependencies: `F01-T02`, `F01-T06`
  - Done when: `ApplicationModules.of(TellPalApplication.class).verify()` calisir ve ileride boundary regressions yakalanabilir.
  - Verify: `cd be && ./mvnw -q -Dtest=ApplicationModulesTest test`

### F02 Admin Identity and Access

- Outcome:
  - Admin kullanicilari, roller, JWT access token ve refresh token rotasyonu olan guvenli auth modulu olusur.
- Depends on:
  - `F01`
- Exit criteria:
  - Login, refresh, logout ve disabled-user rejection akislari integration test ile kanitlanir.

#### Atomic Tasks

- `F02-T01` `TODO`
  - Objective: Admin schema migration'larini olusturmak.
  - Files/modules: `be/src/main/resources/db/migration/V3__create_admin_tables.sql`
  - Dependencies: `F01-T04`
  - Done when: `admin_users`, `admin_roles`, `admin_user_roles`, `admin_refresh_tokens` tablolari; unique ve check kisitlariyla birlikte kurulur ve temel rol kayitlari seed edilir.
  - Verify: `cd be && ./mvnw flyway:migrate`

- `F02-T02` `TODO`
  - Objective: Admin domain modeli ve repository katmanini kurmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/admin/domain/**`, `be/src/main/java/com/tellpal/v2/admin/infrastructure/persistence/**`
  - Dependencies: `F02-T01`, `F01-T05`
  - Done when: `AdminUser`, `AdminRole`, `AdminRefreshToken` ve repository adaptorlari DDD sinirlariyla olusur.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F02-T03` `TODO`
  - Objective: Password hashing, token hashing, JWT sign/verify ve Spring Security config'ini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/admin/infrastructure/security/**`, `be/src/main/resources/application.yml`
  - Dependencies: `F02-T02`
  - Done when: BCrypt, SHA-256, JWT secret config ve admin security filter chain hazir olur.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F02-T04` `TODO`
  - Objective: Login, refresh rotation, logout ve role assignment application service'lerini yazmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/admin/application/**`, `be/src/main/java/com/tellpal/v2/admin/api/**`
  - Dependencies: `F02-T02`, `F02-T03`
  - Done when: Refresh token zinciri hash ile saklanir, rotate edilir ve reuse durumlari reddedilir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F02-T05` `TODO`
  - Objective: Admin auth web adapter'larini ve DTO'larini expose etmek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/admin/web/admin/**`
  - Dependencies: `F02-T04`
  - Done when: `/api/admin/auth/login`, `/refresh`, `/logout` endpoint'leri ince controller olarak calisir.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F02-T06` `TODO`
  - Objective: Admin auth integration ve property testlerini eklemek.
  - Files/modules: `be/src/test/java/com/tellpal/v2/admin/**`
  - Dependencies: `F02-T05`
  - Done when: Login success/failure, disabled user, refresh rotation ve revoked token senaryolari testlerle korunur.
  - Verify: `cd be && ./mvnw -q -Dtest=*Admin* test`

### F03 Asset Registry and Storage Abstraction

- Outcome:
  - Asset modulu media metadata sahipligi, storage abstraction ve signed URL cache politikasiyla kurulur.
- Depends on:
  - `F01`
- Exit criteria:
  - `(provider, object_path)` benzersizligi ve metadata persistansi integration test ile dogrulanir.

#### Atomic Tasks

- `F03-T01` `TODO`
  - Objective: `media_assets` tablosunu olusturan migration'i yazmak.
  - Files/modules: `be/src/main/resources/db/migration/V4__create_media_assets_table.sql`
  - Dependencies: `F01-T04`
  - Done when: Provider, object path, media type, kind, metadata ve signed URL cache kolonlari PostgreSQL 15 uyumlu sekilde olusur.
  - Verify: `cd be && ./mvnw flyway:migrate`

- `F03-T02` `TODO`
  - Objective: `MediaAsset` aggregate'i, enum'lari ve persistence adaptorunu kurmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/asset/domain/**`, `be/src/main/java/com/tellpal/v2/asset/infrastructure/persistence/**`
  - Dependencies: `F03-T01`, `F01-T05`
  - Done when: Asset sahipligi `asset` modulunde kalir ve `shared` icine entity tasinmaz.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F03-T03` `TODO`
  - Objective: Diger moduller icin `asset.api` contract'larini ve storage abstraction'ini tanimlamak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/asset/api/**`, `be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/**`
  - Dependencies: `F03-T02`
  - Done when: Asset query/register contract'lari ve Firebase/local stub adapter yuzleri olusur.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F03-T04` `TODO`
  - Objective: Asset register/update/query application service'lerini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/asset/application/**`
  - Dependencies: `F03-T02`, `F03-T03`
  - Done when: Asset kaydi, metadata guncelleme ve signed URL cache yenileme akislari service seviyesinde calisir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F03-T05` `TODO`
  - Objective: Asset repository ve migration davranisini integration test ile kilitlemek.
  - Files/modules: `be/src/test/java/com/tellpal/v2/asset/**`
  - Dependencies: `F03-T04`
  - Done when: Unique constraint, nullable metadata alanlari ve timestamp davranisi test edilir.
  - Verify: `cd be && ./mvnw -q -Dtest=*Asset* test`

### F04 Content Core Write Model

- Outcome:
  - Content, localization, story page ve contributor write modeli modul sinirlariyla kurulur.
- Depends on:
  - `F01`, `F03`
- Exit criteria:
  - Story page sayisi, content type kurallari ve contributor siralama kurallari testlerle korunur.

#### Atomic Tasks

- `F04-T01` `TODO`
  - Objective: `contents`, `content_localizations` ve `story_pages` migration'larini yazmak.
  - Files/modules: `be/src/main/resources/db/migration/V5__create_content_tables.sql`
  - Dependencies: `F03-T01`
  - Done when: Content tipi, external key, age range, localization status ve story page kisitlari semada olusur.
  - Verify: `cd be && ./mvnw flyway:migrate`

- `F04-T02` `TODO`
  - Objective: `contributors` ve `content_contributors` migration'larini eklemek.
  - Files/modules: `be/src/main/resources/db/migration/V6__create_contributor_tables.sql`
  - Dependencies: `F04-T01`
  - Done when: Dil baglamli contributor baglari ve siralama degerleri veritabaninda temsil edilir.
  - Verify: `cd be && ./mvnw flyway:migrate`

- `F04-T03` `TODO`
  - Objective: Content aggregate, localization, story page ve contributor domain modelini kurmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/content/domain/**`
  - Dependencies: `F04-T01`, `F04-T02`
  - Done when: `STORY` ve story-disindaki tipler icin kurallar domain icinde ifade edilir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F04-T04` `TODO`
  - Objective: Content repository adaptorleri ile `content.api` contract'larini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/content/infrastructure/persistence/**`, `be/src/main/java/com/tellpal/v2/content/api/**`
  - Dependencies: `F04-T03`
  - Done when: Diger moduller content'i ID ve API kontratlari uzerinden gorebilir; entity import'u yapmaz.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F04-T05` `TODO`
  - Objective: Content create/update, localization management, story page management ve contributor assignment service'lerini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/content/application/**`
  - Dependencies: `F04-T03`, `F04-T04`, `F03-T03`
  - Done when: Content write use-case'leri controller'dan bagimsiz application service'lerle yurutulur.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F04-T06` `TODO`
  - Objective: Content core icin unit, property ve integration testlerini yazmak.
  - Files/modules: `be/src/test/java/com/tellpal/v2/content/**`
  - Dependencies: `F04-T05`
  - Done when: `page_count`, localization visibility kurali ve contributor ordering regressions test ile yakalanir.
  - Verify: `cd be && ./mvnw -q -Dtest=*Content* test`

### F05 Category and Language-Based Curation

- Outcome:
  - Category aggregate'i, localization'lari ve dil bazli category-content ordering modeli tamamlanir.
- Depends on:
  - `F01`, `F03`, `F04`
- Exit criteria:
  - Dil bazli published category gosterimi ve ordering davranisi integration test ile kanitlanir.

#### Atomic Tasks

- `F05-T01` `TODO`
  - Objective: Category semasini olusturan migration'lari eklemek.
  - Files/modules: `be/src/main/resources/db/migration/V7__create_category_tables.sql`
  - Dependencies: `F04-T01`
  - Done when: `categories`, `category_localizations`, `category_contents` tablolari slug, status ve ordering kisitlariyla olusur.
  - Verify: `cd be && ./mvnw flyway:migrate`

- `F05-T02` `TODO`
  - Objective: Category domain modeli ve repository adaptorlari olusturmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/category/domain/**`, `be/src/main/java/com/tellpal/v2/category/infrastructure/persistence/**`
  - Dependencies: `F05-T01`
  - Done when: Category aggregate'i localization ve curation child kayitlariyla birlikte tanimlanir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F05-T03` `TODO`
  - Objective: `category.api` contract'larini olusturmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/category/api/**`
  - Dependencies: `F05-T02`
  - Done when: Diger moduller kategori sorgulari icin API kontratina baglanir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F05-T04` `TODO`
  - Objective: Category create/update/localization/curation application service'lerini yazmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/category/application/**`
  - Dependencies: `F05-T02`, `F05-T03`, `F04-T04`
  - Done when: Dil bazli content ekleme/siralama use-case'leri kategori modulunde sahiplenilir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F05-T05` `TODO`
  - Objective: Category modulu icin integration ve property testlerini eklemek.
  - Files/modules: `be/src/test/java/com/tellpal/v2/category/**`
  - Dependencies: `F05-T04`
  - Done when: Slug benzersizligi, localization status ve ordering kurallari test edilir.
  - Verify: `cd be && ./mvnw -q -Dtest=*Category* test`

### F06 CMS Admin APIs

- Outcome:
  - Admin tarafinda content, category, contributor ve media islemleri icin ince controller katmani kurulur.
- Depends on:
  - `F02`, `F03`, `F04`, `F05`
- Exit criteria:
  - Yetkili admin ile CRUD akislari HTTP seviyesinde integration test ile kanitlanir.

#### Atomic Tasks

- `F06-T01` `TODO`
  - Objective: Admin web ortak katmanini kurmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/shared/web/admin/**`, `be/src/main/java/com/tellpal/v2/admin/web/admin/**`
  - Dependencies: `F02-T05`
  - Done when: Request auth extraction, problem details/error mapping ve request logging ortaklasir.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F06-T02` `TODO`
  - Objective: Content admin endpoint'lerini expose etmek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/content/web/admin/**`
  - Dependencies: `F06-T01`, `F04-T05`
  - Done when: Content create/update, localization yonetimi ve story page endpoint'leri application service'leri kullanir.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F06-T03` `TODO`
  - Objective: Category ve curation admin endpoint'lerini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/category/web/admin/**`
  - Dependencies: `F06-T01`, `F05-T04`
  - Done when: Category CRUD, localization ve ordering endpoint'leri hazir olur.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F06-T04` `TODO`
  - Objective: Media asset registry endpoint'lerini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/asset/web/admin/**`
  - Dependencies: `F06-T01`, `F03-T04`
  - Done when: Asset create/list/update endpoint'leri `asset.application` uzerinden calisir.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F06-T05` `TODO`
  - Objective: Contributor yonetimi icin admin endpoint'lerini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/content/web/admin/**`
  - Dependencies: `F06-T02`, `F04-T05`
  - Done when: Contributor create/update/list ve content'e baglama operasyonlari HTTP seviyesinde acilir.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F06-T06` `TODO`
  - Objective: Admin API integration test paketini yazmak.
  - Files/modules: `be/src/test/java/com/tellpal/v2/**/web/admin/**`
  - Dependencies: `F06-T02`, `F06-T03`, `F06-T04`, `F06-T05`
  - Done when: Auth, validation, conflict ve happy-path davranislari endpoint bazinda test edilir.
  - Verify: `cd be && ./mvnw -q -Dtest=*Admin*IntegrationTest test`

### F07 Editorial Publication and Free Access

- Outcome:
  - Editorial publication policy netlesir ve `content_free_access` tabanli A/B free-content yonetimi eklenir.
- Depends on:
  - `F04`, `F06`
- Exit criteria:
  - Unknown `freeKey` icin `default` fallback ve publication validation testlerle korunur.

#### Atomic Tasks

- `F07-T01` `TODO`
  - Objective: `content_free_access` semasini ve ilgili kisitlari eklemek.
  - Files/modules: `be/src/main/resources/db/migration/V8__create_content_free_access_table.sql`
  - Dependencies: `F04-T01`
  - Done when: `(access_key, content_id, language_code)` benzersizligi ve `default` anahtarini destekleyen tablo olusur.
  - Verify: `cd be && ./mvnw flyway:migrate`

- `F07-T02` `TODO`
  - Objective: Publication policy domain/application mantigini tanimlamak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/content/domain/**`, `be/src/main/java/com/tellpal/v2/content/application/**`
  - Dependencies: `F04-T05`
  - Done when: Localization publish/archive gecis kurallari merkezi policy ile yurutulur.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F07-T03` `TODO`
  - Objective: Free-access application service ve query kurallarini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/content/application/**`, `be/src/main/java/com/tellpal/v2/content/api/**`
  - Dependencies: `F07-T01`, `F04-T05`
  - Done when: `default` key, explicit `freeKey` ve unknown-key fallback mantigi service seviyesinde calisir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F07-T04` `TODO`
  - Objective: Free-access ve publication admin endpoint'lerini expose etmek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/content/web/admin/**`
  - Dependencies: `F07-T02`, `F07-T03`, `F06-T01`
  - Done when: Publication state gecisleri ve free-access CRUD operasyonlari controller seviyesinde acilir.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F07-T05` `TODO`
  - Objective: Publication ve free-access kurallari icin test paketini eklemek.
  - Files/modules: `be/src/test/java/com/tellpal/v2/content/**`
  - Dependencies: `F07-T04`
  - Done when: Unknown key fallback, duplicate free-access kaydi ve invalid publication senaryolari test edilir.
  - Verify: `cd be && ./mvnw -q -Dtest=*FreeAccess* test`

### F08 Mobile User and Profile

- Outcome:
  - Firebase UID ile eslenen app user ve default profile write modeli kurulur.
- Depends on:
  - `F01`, `F03`
- Exit criteria:
  - Tek primary profile kurali ve Firebase UID benzersizligi testlerle dogrulanir.

#### Atomic Tasks

- `F08-T01` `TODO`
  - Objective: `app_users` ve `user_profiles` migration'larini yazmak.
  - Files/modules: `be/src/main/resources/db/migration/V9__create_user_tables.sql`
  - Dependencies: `F01-T04`
  - Done when: `firebase_uid` unique kisiti ve tek primary profile icin partial unique index olusur.
  - Verify: `cd be && ./mvnw flyway:migrate`

- `F08-T02` `TODO`
  - Objective: AppUser aggregate'i, UserProfile child entity'si ve persistence katmanini olusturmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/user/domain/**`, `be/src/main/java/com/tellpal/v2/user/infrastructure/persistence/**`
  - Dependencies: `F08-T01`
  - Done when: Kullanici ve profil sahipligi `user` modulunde netlesir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F08-T03` `TODO`
  - Objective: Firebase token verify abstraction ve user resolution service'ini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/user/infrastructure/firebase/**`, `be/src/main/java/com/tellpal/v2/user/application/**`, `be/src/main/java/com/tellpal/v2/user/api/**`
  - Dependencies: `F08-T02`
  - Done when: Firebase token'dan app user bulma/yaratma ve default profile olusturma akisi service seviyesinde calisir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F08-T04` `TODO`
  - Objective: Mobile profile endpoint'lerini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/user/web/public/**`
  - Dependencies: `F08-T03`
  - Done when: Profil okuma ve guncelleme endpoint'leri Firebase auth baglami ile calisir.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F08-T05` `TODO`
  - Objective: User/profile integration ve property testlerini yazmak.
  - Files/modules: `be/src/test/java/com/tellpal/v2/user/**`
  - Dependencies: `F08-T04`
  - Done when: Duplicate `firebase_uid`, multi-primary profile ve default profile olusumu test edilir.
  - Verify: `cd be && ./mvnw -q -Dtest=*User* test`

### F09 Event Tracking Foundation

- Outcome:
  - Offline-first content/app event ingest, idempotent insert ve analytics index tabani olusur.
- Depends on:
  - `F04`, `F08`
- Exit criteria:
  - Duplicate `event_id` ve `legacy_event_key` davranisi property + integration test ile korunur.

#### Atomic Tasks

- `F09-T01` `TODO`
  - Objective: Event tablolarini ve analytics index'lerini yaratan migration'i yazmak.
  - Files/modules: `be/src/main/resources/db/migration/V10__create_event_tables.sql`
  - Dependencies: `F08-T01`, `F04-T01`
  - Done when: `content_events`, `app_events`, unique/event index'leri ve partial unique legacy index'leri olusur.
  - Verify: `cd be && ./mvnw flyway:migrate`

- `F09-T02` `TODO`
  - Objective: ContentEvent ve AppEvent domain modelini ve persistence katmanini kurmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/event/domain/**`, `be/src/main/java/com/tellpal/v2/event/infrastructure/persistence/**`
  - Dependencies: `F09-T01`
  - Done when: Event kayitlari immutable davranis varsayimiyla modellenir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F09-T03` `TODO`
  - Objective: Single ve batch ingest application service'lerini yazmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/event/application/**`, `be/src/main/java/com/tellpal/v2/event/api/**`
  - Dependencies: `F09-T02`, `F08-T03`, `F04-T04`
  - Done when: Duplicate insert'ler unique constraint ve idempotent cevap mantigiyla ele alinir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F09-T04` `TODO`
  - Objective: Mobile event ingest endpoint'lerini expose etmek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/event/web/public/**`
  - Dependencies: `F09-T03`
  - Done when: Content/app event ve batch endpoint'leri Firebase auth ile korunur.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F09-T05` `TODO`
  - Objective: Event tracking property ve integration testlerini eklemek.
  - Files/modules: `be/src/test/java/com/tellpal/v2/event/**`
  - Dependencies: `F09-T04`
  - Done when: Duplicate event, batch sync ve legacy import collision davranislari test edilir.
  - Verify: `cd be && ./mvnw -q -Dtest=*Event* test`

### F10 Purchase and Attribution

- Outcome:
  - RevenueCat product catalog, webhook ingest ve 24 saatlik attribution mantigi purchase modulunde sahiplenilir.
- Depends on:
  - `F04`, `F08`, `F09`
- Exit criteria:
  - Webhook idempotency, lookup validation ve attribution precedence integration test ile dogrulanir.

#### Atomic Tasks

- `F10-T01` `TODO`
  - Objective: Purchase lookup tablolarini, product catalog'u ve purchase event semasini olusturmak.
  - Files/modules: `be/src/main/resources/db/migration/V11__create_purchase_tables.sql`
  - Dependencies: `F09-T01`
  - Done when: `subscription_products`, purchase lookup tablolari, `purchase_events` ve `purchase_context_snapshots` olusur.
  - Verify: `cd be && ./mvnw flyway:migrate`

- `F10-T02` `TODO`
  - Objective: Purchase domain modeli ve persistence katmanini kurmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/purchase/domain/**`, `be/src/main/java/com/tellpal/v2/purchase/infrastructure/persistence/**`
  - Dependencies: `F10-T01`
  - Done when: `SubscriptionProduct`, `PurchaseEvent`, `PurchaseContextSnapshot` aggregate sahiplikleri netlesir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F10-T03` `TODO`
  - Objective: Product catalog application service ve admin kontratlarini yazmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/purchase/application/**`, `be/src/main/java/com/tellpal/v2/purchase/api/**`
  - Dependencies: `F10-T02`
  - Done when: Product create/update/deactivate operasyonlari controller-disinda service katmaninda toplanir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F10-T04` `TODO`
  - Objective: RevenueCat signature verify adapter ve webhook ingest service'ini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/purchase/infrastructure/revenuecat/**`, `be/src/main/java/com/tellpal/v2/purchase/application/**`
  - Dependencies: `F10-T02`, `F10-T03`, `F08-T03`
  - Done when: Raw payload saklanir, lookup validation yapilir ve duplicate `revenuecat_event_id` reddedilir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F10-T05` `TODO`
  - Objective: Attribution service'ini event/user/content API'lerine baglayarak yazmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/purchase/application/**`
  - Dependencies: `F10-T04`, `F09-T03`, `F08-T03`, `F04-T04`
  - Done when: Son `LOCKED_CONTENT_CLICKED`, yoksa son `PAYWALL_SHOWN` secimi 24 saat penceresinde uygulanir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F10-T06` `TODO`
  - Objective: Webhook endpoint'i ve purchase integration/property testlerini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/purchase/web/webhook/**`, `be/src/test/java/com/tellpal/v2/purchase/**`
  - Dependencies: `F10-T05`
  - Done when: Signature fail, duplicate event, unknown lookup ve attribution precedence senaryolari test edilir.
  - Verify: `cd be && ./mvnw -q -Dtest=*Purchase* test`

### F11 Asset Processing and Packaging

- Outcome:
  - Asset processing state machine, worker modeli, packaging ve readiness bildirim akisi tamamlanir.
- Depends on:
  - `F03`, `F04`, `F06`, `F07`
- Exit criteria:
  - Processing durum gecisleri ve paket/variant kayitlari integration test ile kanitlanir.

#### Atomic Tasks

- `F11-T01` `TODO`
  - Objective: `asset_processing` tablosunu ve localization `processing_status` kolonunu eklemek.
  - Files/modules: `be/src/main/resources/db/migration/V12__create_asset_processing_tables.sql`
  - Dependencies: `F07-T01`, `F04-T01`
  - Done when: `PENDING -> PROCESSING -> COMPLETED|FAILED` durum makinasi semada temsil edilir.
  - Verify: `cd be && ./mvnw flyway:migrate`

- `F11-T02` `TODO`
  - Objective: Processing domain modeli ve persistence katmanini kurmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/asset/domain/**`, `be/src/main/java/com/tellpal/v2/asset/infrastructure/persistence/**`
  - Dependencies: `F11-T01`, `F03-T02`
  - Done when: `AssetProcessing` aggregate'i retry ve terminal-state kurallariyla olusur.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F11-T03` `TODO`
  - Objective: Processing application service, content readiness event'leri ve retry akisini yazmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/asset/application/**`, `be/src/main/java/com/tellpal/v2/asset/api/**`, `be/src/main/java/com/tellpal/v2/content/api/**`
  - Dependencies: `F11-T02`, `F04-T04`, `F07-T02`
  - Done when: Manual start, retry, completion/failure ve content'e readiness bildirimi service katmaninda yonetilir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F11-T04` `TODO`
  - Objective: Worker/poller altyapisini ve storage path builder'ini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/asset/infrastructure/processing/**`, `be/src/main/java/com/tellpal/v2/asset/infrastructure/storage/**`
  - Dependencies: `F11-T03`
  - Done when: Uygulama restart oldugunda is kaybi yasatmayan db-backed polling modeli kurulur.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F11-T05` `TODO`
  - Objective: Image/audio optimization ve ZIP packaging adapter'larini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/asset/infrastructure/media/**`
  - Dependencies: `F11-T04`
  - Done when: Cover variants, optimized audio ve story/non-story ZIP naming kurallari kodlanir.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F11-T06` `TODO`
  - Objective: Processing trigger/status admin endpoint'lerini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/asset/web/admin/**`
  - Dependencies: `F11-T03`, `F06-T04`
  - Done when: CMS tarafindan processing baslatma, yeniden deneme ve durum sorgulama endpoint'leri calisir.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F11-T07` `TODO`
  - Objective: Asset processing integration testlerini eklemek.
  - Files/modules: `be/src/test/java/com/tellpal/v2/asset/**`, `be/src/test/java/com/tellpal/v2/content/**`
  - Dependencies: `F11-T05`, `F11-T06`
  - Done when: Status transition, generated asset kind'lari ve completed-ready callback davranisi test edilir.
  - Verify: `cd be && ./mvnw -q -Dtest=*Processing* test`

### F12 Public Content and Category Delivery

- Outcome:
  - Mobil uygulama icin public content ve category API'leri language, publication ve processing readiness kurallariyla acilir.
- Depends on:
  - `F05`, `F07`, `F11`
- Exit criteria:
  - Yalnizca `PUBLISHED + COMPLETED` localization'larin ve ilgili category curation kayitlarinin donduruldugu testlerle korunur.

#### Atomic Tasks

- `F12-T01` `TODO`
  - Objective: Public content query modelini ve service katmanini yazmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/content/application/query/**`, `be/src/main/java/com/tellpal/v2/content/api/**`
  - Dependencies: `F07-T03`, `F11-T03`
  - Done when: Language, freeKey, processing_status ve published filtreleri tek yerde uygulanir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F12-T02` `TODO`
  - Objective: Public category query modelini ve service katmanini yazmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/category/application/query/**`, `be/src/main/java/com/tellpal/v2/category/api/**`
  - Dependencies: `F05-T04`, `F12-T01`
  - Done when: Dil bazli curated ordering ve published category filtreleri sorgu katmaninda toplanir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F12-T03` `TODO`
  - Objective: Public content controller'larini expose etmek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/content/web/public/**`
  - Dependencies: `F12-T01`
  - Done when: `/api/contents` liste ve detay endpoint'leri isFree, package refs ve optimized cover refs ile cevap verir.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F12-T04` `TODO`
  - Objective: Public category controller'larini expose etmek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/category/web/public/**`
  - Dependencies: `F12-T02`
  - Done when: `/api/categories` ve kategori bazli content listeleme endpoint'leri curation sirasini korur.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F12-T05` `TODO`
  - Objective: Public delivery integration testlerini eklemek.
  - Files/modules: `be/src/test/java/com/tellpal/v2/**/web/public/**`
  - Dependencies: `F12-T03`, `F12-T04`
  - Done when: Language filtering, completed-only visibility ve default freeKey fallback HTTP seviyesinde test edilir.
  - Verify: `cd be && ./mvnw -q -Dtest=*Public* test`

### F13 Firebase Migration Support

- Outcome:
  - Legacy Firebase user ve event verilerini PostgreSQL'e tasiyan kontrollu import akislari olusur.
- Depends on:
  - `F08`, `F09`
- Exit criteria:
  - Sample fixture'lar ile tekrar calistirilabilir import akislari ve duplicate korumasi test edilir.

#### Atomic Tasks

- `F13-T01` `TODO`
  - Objective: Import giris formatini, staging dizinini ve dry-run komut contract'ini tanimlamak.
  - Files/modules: `be/docs/firebase-migration.md`, `be/src/main/java/com/tellpal/v2/user/infrastructure/firebase/migration/**`
  - Dependencies: `F08-T03`, `F09-T03`
  - Done when: Hangi input dosyalarinin nasil okunacagi ve dry-run davranisinin ne oldugu yazili hale gelir.
  - Verify: Dokuman ve komut sinifi derlenir

- `F13-T02` `TODO`
  - Objective: Legacy user/profile import service'ini yazmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/user/infrastructure/firebase/migration/**`, `be/src/main/java/com/tellpal/v2/user/application/**`
  - Dependencies: `F13-T01`
  - Done when: Firebase UID bazli upsert ve default profile olusturma import akisinda desteklenir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F13-T03` `TODO`
  - Objective: Legacy content/app event import service'ini yazmak.
  - Files/modules: `be/src/main/java/com/tellpal/v2/event/infrastructure/firebase/migration/**`, `be/src/main/java/com/tellpal/v2/event/application/**`
  - Dependencies: `F13-T01`
  - Done when: `legacy_event_key` unique kurali korunarak event import edilebilir.
  - Verify: `cd be && ./mvnw -q -DskipTests compile`

- `F13-T04` `TODO`
  - Objective: Firebase migration integration testlerini fixture verisi ile eklemek.
  - Files/modules: `be/src/test/java/com/tellpal/v2/**/migration/**`, `be/src/test/resources/fixtures/firebase/**`
  - Dependencies: `F13-T02`, `F13-T03`
  - Done when: Dry-run, duplicate import ve happy-path import senaryolari test edilir.
  - Verify: `cd be && ./mvnw -q -Dtest=*Migration* test`

### F14 Hardening and Release Readiness

- Outcome:
  - Modul sinirlari sertlesir, operasyonel gorunurluk eklenir ve release-ready verify zinciri kurulur.
- Depends on:
  - `F02`-`F13`
- Exit criteria:
  - `./mvnw verify` seviyesinde tekrarlanabilir kalite kapisi ve temel release runbook'u hazir olur.

#### Atomic Tasks

- `F14-T01` `TODO`
  - Objective: Tum moduller icin boundary ve architecture testlerini sertlestirmek.
  - Files/modules: `be/src/test/java/com/tellpal/v2/**/ApplicationModules*.java`
  - Dependencies: `F12-T05`
  - Done when: Illegal internal package erisimleri ve circular dependency'ler test ile yakalanir.
  - Verify: `cd be && ./mvnw -q -Dtest=*ApplicationModules* test`

- `F14-T02` `TODO`
  - Objective: Structured logging, request ID ve actuator/micrometer tabanini eklemek.
  - Files/modules: `be/src/main/resources/application.yml`, `be/src/main/java/com/tellpal/v2/shared/infrastructure/observability/**`
  - Dependencies: `F12-T05`
  - Done when: Request ID, auth fail, webhook fail ve processing transition loglari standardize edilir.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F14-T03` `TODO`
  - Objective: Security hardening ve config validation katmanini eklemek.
  - Files/modules: `be/src/main/java/com/tellpal/v2/shared/infrastructure/security/**`, `be/src/main/resources/application.yml`
  - Dependencies: `F02-T05`, `F10-T06`, `F12-T05`
  - Done when: Missing secret fail-fast, error sanitization ve production-safe varsayilanlar tanimlanir.
  - Verify: `cd be && ./mvnw -q -DskipTests test-compile`

- `F14-T04` `TODO`
  - Objective: Maven verify zinciri, docs generation ve CI workflow tabanini kurmak.
  - Files/modules: `be/pom.xml`, `.github/workflows/backend-verify.yml`, `be/docs/**`
  - Dependencies: `F14-T01`, `F14-T02`, `F14-T03`
  - Done when: Test, verify, Flyway, Modulith docs ve packaging adimlari otomasyona baglanir.
  - Verify: `cd be && ./mvnw verify`

- `F14-T05` `TODO`
  - Objective: Local runbook, sample bootstrap verisi ve operasyon notlarini yazmak.
  - Files/modules: `be/docs/local-development.md`, `be/docs/release-checklist.md`, `be/docs/bootstrap-notes.md`
  - Dependencies: `F14-T04`
  - Done when: Lokal kurulum, secret beklentileri, bootstrap notlari ve release checklist'i yazili hale gelir.
  - Verify: Dokumanlar ve migration compile/flyway seviyesinde dogrulanir

- `F14-T06` `TODO`
  - Objective: Son release verification turunu ve kalan acik risk listesini cikarmak.
  - Files/modules: `be/docs/release-readiness-report.md`
  - Dependencies: `F14-T05`
  - Done when: Calisan komutlar, bilincli ertelenen riskler ve acik kararlar tek raporda toplanir.
  - Verify: `cd be && ./mvnw test && ./mvnw verify`

## Immediate Execution Order

Kodlama icin ilk uygulanacak siralama:

1. `F01-T01` -> `F01-T07`
2. `F02-T01` -> `F02-T06`
3. `F03-T01` -> `F03-T05`
4. `F04-T01` -> `F04-T06`
5. `F05-T01` -> `F05-T05`
6. `F06-T01` -> `F06-T06`

Bu noktadan sonra public/mobile akislarina gecmeden once `F07`, `F08`, `F09`, `F10`, `F11` tamamlanmalidir.

## Notes

- Bu dosya, mevcut durumda uygulanabilir backlog'un kaynak kaydidir.
- `.kiro/specs/tellpal-v2-backend/tasks/` altindaki onceki task taslaklari yardimci referans olarak kalabilir, ancak implementation baslangicinda bu dosya esas alinmalidir.
- Mimari karar degisirse once `architecture.md`, sonra bu backlog guncellenmelidir.
