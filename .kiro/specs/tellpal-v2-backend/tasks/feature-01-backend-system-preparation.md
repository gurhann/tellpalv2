# Feature F01: Backend System Preparation

## Durum

- Status: `ACTIVE`
- Owner: `Codex`

## Hedef Sonuc

Bu feature tamamlandiginda `be/` altinda calisan, test tabani kurulmus, Flyway ile temel veritabani iskeleti olusan ve Spring Modulith sinirlari icin ilk smoke verification'i bulunan bir backend baslangic noktasi hazir olacak.

## Kapsam

- Maven wrapper ile backend projesinin baslatilmasi
- Spring Boot uygulama bootstrap'i
- Modul paket iskeleti
- Temel runtime ve local development config'i
- Flyway baseline ve `languages` tablosu
- Dil seed verisi
- Shared modulu icin minimum ortak yapilar
- Test ve architecture smoke test tabani

## Kapsam Disi

- Admin auth implementation
- Content, category, asset, user, event veya purchase domain davranisi
- REST controller'lar
- Firebase veya RevenueCat entegrasyonlari
- `media_assets` tablosu ve asset processing akisi

## Bagimliliklar

- Yok

## Exit Criteria

- `be/` dizini Maven wrapper ile build alabilir durumda olmali
- Spring Boot uygulamasi ve modul paketleri olusturulmus olmali
- Flyway migration'lari bos PostgreSQL veritabaninda calisabilmeli
- `languages` tablosu ve tohum verisi olusmali
- En az bir context smoke test, bir Flyway integration testi ve bir Modulith boundary testi mevcut olmali

## Task Dependency Order

- `F01-T01 -> F01-T02 -> F01-T03 -> F01-T04 -> F01-T05`
- `F01-T02 -> F01-T06`
- `F01-T01 + F01-T03 -> F01-T07`
- `F01-T04 + F01-T05 + F01-T07 -> F01-T08`
- `F01-T02 + F01-T07 -> F01-T09`

## Atomic Tasks

### F01-T01 `TODO` Maven wrapper ve backend module iskeletini olustur

- Objective:
  - `be/` altinda bagimsiz calisabilen Maven wrapper tabanli backend projesini olusturmak
- Files to create or modify:
  - `be/pom.xml`
  - `be/mvnw`
  - `be/mvnw.cmd`
  - `be/.mvn/wrapper/maven-wrapper.properties`
  - `be/.mvn/wrapper/maven-wrapper.jar`
- Dependencies:
  - Yok
- Acceptance criteria:
  - `be/` altinda Spring Boot, Spring Modulith, Flyway, PostgreSQL, JUnit 5, jqwik ve Testcontainers cekirdek bagimliliklarini tasiyan bir `pom.xml` bulunur
  - Maven wrapper dosyalari repoda yer alir
  - Backend komutlari `be/` icinden wrapper ile calistirilabilir
- Verification steps:
  - `cd be && ./mvnw -q -DskipTests help:effective-pom`

### F01-T02 `TODO` Uygulama bootstrap ve modul paket iskeletini kur

- Objective:
  - Mimariye uygun top-level modul paketlerini ve Spring Boot giris noktasini olusturmak
- Files to create or modify:
  - `be/src/main/java/com/tellpal/v2/TellPalApplication.java`
  - `be/src/main/java/com/tellpal/v2/shared/package-info.java`
  - `be/src/main/java/com/tellpal/v2/admin/package-info.java`
  - `be/src/main/java/com/tellpal/v2/content/package-info.java`
  - `be/src/main/java/com/tellpal/v2/category/package-info.java`
  - `be/src/main/java/com/tellpal/v2/asset/package-info.java`
  - `be/src/main/java/com/tellpal/v2/user/package-info.java`
  - `be/src/main/java/com/tellpal/v2/event/package-info.java`
  - `be/src/main/java/com/tellpal/v2/purchase/package-info.java`
- Dependencies:
  - `F01-T01`
- Acceptance criteria:
  - `TellPalApplication` sinifi Spring Boot uygulamasini baslatir
  - Mimari dokumandaki modul isimleriyle uyumlu top-level package iskeleti olusur
  - Ayrik `presentation` top-level paketi olusturulmaz
- Verification steps:
  - `cd be && ./mvnw -q -DskipTests compile`

### F01-T03 `TODO` Temel runtime config ve local development dosyalarini ekle

- Objective:
  - Uygulamanin local ortamda konfigure edilebilir ve tekrar uretilebilir sekilde baslatilmasini saglamak
- Files to create or modify:
  - `be/src/main/resources/application.yml`
  - `be/src/main/resources/application-local.yml`
  - `be/compose.yml`
  - `be/.env.example`
- Dependencies:
  - `F01-T01`
- Acceptance criteria:
  - Datasource, JPA ve Flyway icin temel ayarlar tanimlanir
  - Local PostgreSQL calistirmak icin compose dosyasi bulunur
  - Ortam degiskenleri icin ornek dosya bulunur
- Verification steps:
  - `cd be && docker compose -f compose.yml config`

### F01-T04 `TODO` Flyway baseline ve languages tablo migration'ini olustur

- Objective:
  - Ortak dil verisi icin ilk veritabani sema temelini kurmak
- Files to create or modify:
  - `be/src/main/resources/db/migration/V1__create_languages_table.sql`
- Dependencies:
  - `F01-T03`
- Acceptance criteria:
  - `languages` tablosu `code`, `display_name`, `is_active`, timestamp alanlari ile olusturulur
  - Dil kodu benzersizligi ve temel butunluk kisitlari migration icinde tanimlanir
  - SQL PostgreSQL 15 ile uyumludur
- Verification steps:
  - `cd be && ./mvnw flyway:migrate`

### F01-T05 `TODO` Dil seed migration'ini ekle

- Objective:
  - Gereksinimlerde belirtilen 5 destekli dilin bos veritabanina migration ile yuklenmesini saglamak
- Files to create or modify:
  - `be/src/main/resources/db/migration/V2__seed_languages.sql`
- Dependencies:
  - `F01-T04`
- Acceptance criteria:
  - `tr`, `en`, `es`, `pt`, `de` kayitlari migration ile eklenir
  - Gosterim adlari ve `is_active = true` degerleri gereksinimlerle uyumludur
  - Temiz veritabaninda V1+V2 birlikte sorunsuz calisir
- Verification steps:
  - `cd be && ./mvnw flyway:migrate`

### F01-T06 `TODO` Shared modul icin minimum ortak persistence ve language yapilarini ekle

- Objective:
  - Sonraki feature'larda tekrar kullanilacak en kucuk ortak backend yapilarini olusturmak
- Files to create or modify:
  - `be/src/main/java/com/tellpal/v2/shared/domain/LanguageCode.java`
  - `be/src/main/java/com/tellpal/v2/shared/infrastructure/persistence/BaseJpaEntity.java`
  - `be/src/main/java/com/tellpal/v2/shared/infrastructure/persistence/AuditableEntityListener.java`
- Dependencies:
  - `F01-T02`
- Acceptance criteria:
  - `shared` modulu sadece gercekten ortak ve kararli yapilari icerir
  - `MediaAsset` veya modullere ozel domain entity'leri `shared` altina konulmaz
  - Ortak JPA auditing tabani sonraki entity'ler icin kullanilabilir halde olur
- Verification steps:
  - `cd be && ./mvnw -q -DskipTests compile`

### F01-T07 `TODO` Test runtime config'i ve context smoke test tabanini kur

- Objective:
  - Backend'in test ortaminda ayaga kalkabildigini gosteren minimum test altyapisini kurmak
- Files to create or modify:
  - `be/src/test/resources/application-test.yml`
  - `be/src/test/java/com/tellpal/v2/TellPalApplicationSmokeTest.java`
  - `be/src/test/java/com/tellpal/v2/support/PostgresIntegrationTestBase.java`
- Dependencies:
  - `F01-T01`
  - `F01-T03`
- Acceptance criteria:
  - Test profili veritabani ve Spring ayarlari tanimlidir
  - En az bir smoke test Spring context'in yuklenebildigini dogrular
  - Testcontainers tabani sonraki integration testler icin yeniden kullanilabilir olur
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=TellPalApplicationSmokeTest test`

### F01-T08 `TODO` Flyway ve dil seed'i dogrulayan integration testi ekle

- Objective:
  - Migration zincirinin bos veritabaninda gercekten calistigini ve dil verisini uretebildigini kanitlamak
- Files to create or modify:
  - `be/src/test/java/com/tellpal/v2/shared/LanguageSeedIntegrationTest.java`
- Dependencies:
  - `F01-T04`
  - `F01-T05`
  - `F01-T07`
- Acceptance criteria:
  - Testcontainers PostgreSQL uzerinde migration'lar uygulanir
  - Test sonunda bes dil kaydinin var oldugu ve aktif oldugu dogrulanir
  - Test ileride bozulursa sema veya seed regresyonu anlasilir
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=LanguageSeedIntegrationTest test`

### F01-T09 `TODO` Spring Modulith boundary smoke test'ini ekle

- Objective:
  - Kurulan paket yapisinin mimari sinirlari test edilebilir oldugunu gostermek
- Files to create or modify:
  - `be/src/test/java/com/tellpal/v2/ApplicationModulesTest.java`
- Dependencies:
  - `F01-T02`
  - `F01-T07`
- Acceptance criteria:
  - `ApplicationModules.of(TellPalApplication.class).verify()` calistiran bir test bulunur
  - Modul yapisi test seviyesinde dogrulanabilir hale gelir
  - Sonraki feature'larda sinir ihlalleri bu testle yakalanabilir
- Verification steps:
  - `cd be && ./mvnw -q -Dtest=ApplicationModulesTest test`

## Progress Log

- `2026-03-16`: feature ve atomic task listesi olusturuldu
