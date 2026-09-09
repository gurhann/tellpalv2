---
stepsCompleted:
  - step-01-validate-prerequisites
  - step-02-design-epics
  - step-03-create-stories
  - step-04-final-validation
inputDocuments:
  - _bmad-output/specs/spec-story-audio-experience/SPEC.md
  - _bmad-output/specs/spec-lullaby-shared-audio-and-instruments/SPEC.md
  - _bmad-output/specs/spec-shared-textless-cover/SPEC.md
  - _bmad-output/planning-artifacts/architecture/architecture-tellpalv2-2026-09-06/ARCHITECTURE-SPINE.md
---

# tellpalv2 - Epic Breakdown

## Overview

This document decomposes the localized story-audio experience, shared lullaby playback, instrument catalog, and textless-cover ownership into implementable work. The current implementation scope is CMS/backend editorial modeling; mobile public endpoints and discovery are deferred to a later roadmap item. The three specs and their architecture spine are the sole requirements sources for this run.

## Requirements Inventory

### Functional Requirements

FR1: A `STORY` localization can own an optional full-story narration without creating a second content identity; existing page-read-aloud audio remains separate.

FR2 [DEFERRED]: Public discovery can return the same story as `READING` and, when that locale narration is ready, as `AUDIO_STORY`, while preserving one `contentId` and canonical type `STORY`.

FR3 [DEFERRED]: New public story contracts distinguish `canonicalType` from `experienceType`; legacy audio-discovery clients receive a compatible `AUDIO_STORY` projection during the selected rollout period.

FR4: A `LULLABY` owns one shared playback configuration: audio asset, common textless cover, duration, global musician credits, and one or more ordered catalog instruments.

FR5: Administrators select lullaby instruments from a catalog rather than entering free text; the catalog has stable codes and backend-localized display names.

FR6: A lullaby localization manages only its title and publication state; its audio, cover, duration, musician and instrument data cannot diverge by locale.

FR7 [DEFERRED]: Public lullaby reads combine a locale-specific title with one shared playback payload, including resolved cover/audio, duration, musicians, and ordered localized instrument labels.

FR8: STORY source textless covers, localized reading covers, and shared listening covers remain separate ownership concepts; CMS manages each at its correct scope.

FR13: CMS can manage a LULLABY's static listing cover separately from its animated playback/detail cover without copying either asset to localizations.

FR9 [DEFERRED]: STORY `READING` keeps its locale-specific `ContentLocalization.coverMediaId`, because its cover may contain translated title text.

FR10: CMS separates shared playback editors from locale editors: story narration belongs in its locale editor; lullaby common playback belongs in its content editor; lullaby and meditation locale editors never show a cover field.

FR11: New canonical content/category creation no longer permits `AUDIO_STORY`; meditation and lullaby remain canonical content types.

FR12: Before the database removes canonical `AUDIO_STORY` support, the migration rejects any persisted content, category, or processing row using it; legacy audio-story import remains out of scope.

### NonFunctional Requirements

NFR1: Content, category, and asset module boundaries remain compliant with Spring Modulith; cross-module access uses public APIs only.

NFR2: Asset processing has unambiguous typed identity: localization-scoped targets require `(contentId, languageCode)` and content-scoped targets require only `contentId`; database constraints prevent duplicate targets.

NFR3 [DEFERRED]: `READING`, `AUDIO_STORY`, and `LULLABY` mobile visibility is independently evaluated. A failed narration must not hide reading; a failed shared lullaby playback must hide every locale projection without mutating editorial publication data.

NFR4: Asset references must be positive IDs with expected media types; public URLs are resolved through `asset.api` at read time.

NFR5: Database constraints enforce unique, ordered lullaby-instrument links and prevent deletion of catalog instruments already in use; retirement may remove an instrument only from future selection.

NFR6 [DEFERRED]: Public collection filtering and pagination execute in the database before response projection.

NFR7: CMS changes follow existing UI standards: shared ownership fields are edited in one dominant content-level workflow, locale forms do not duplicate common state, and visual/interaction regression coverage accompanies layout-affecting changes.

NFR8: Flyway changes, REST contracts, persistence rules, module boundaries, and mobile/CMS behavior receive proportional unit, integration, and architecture test coverage.

### Additional Requirements

- `content` owns the experience model, playback intent, catalog selection, and public read composition; `asset` owns registration, resolved references, and operational processing state.
- Introduce `StoryNarration` as an optional child of `ContentLocalization`, with one full-story audio asset ID, duration, and localization-scoped processing target.
- Keep `LullabyPlayback` as an aggregate child of `Content`; use `listeningCoverMediaId` for its shared playback/detail cover and `listingCoverMediaId` for its separate static listing cover.
- Introduce `InstrumentCatalog`, `InstrumentCatalogLocalization`, and ordered `LullabyInstrument` links in the content module.
- Evolve `asset` processing command, record, repository, API, state transition and path conventions to explicit `LOCALIZATION` and `CONTENT` targets, including `findByContent(contentId)`.
- Public query services compose selected localization editorial fields with the relevant playback owner and `asset.api` status/output; they do not copy processing state into content. This public-read work is deferred with the mobile endpoint roadmap.
- Existing canonical architecture and ADR-0007 must be updated/superseded only after the implementation approach is ratified; catalog administration, seed set, legacy import, and exact public compatibility rollout mechanism remain deferred.

### UX Design Requirements

No UX design contract was included in this CE run. Existing CMS UI standards remain mandatory during implementation.

### FR Coverage Map

FR1: Epic 1 - Story localization'a tam anlatım ekleme.

FR2: Deferred roadmap - Mobilde aynı hikâyeyi okuma ve sesli deneyim olarak keşfetme.

FR3: Deferred roadmap - Canonical tür ve deneyim türünü ayıran, uyumlu public API sözleşmesi.

FR4: Epic 1 - Ortak ninni playback verisini yönetme.

FR5: Epic 1 - Katalogdan sıralı enstrüman seçimi.

FR6: Epic 1 - Ninni localization'ını başlık ve yayın durumuyla sınırlama.

FR7: Deferred roadmap - Yerelleştirilmiş ad ve ortak playback ile ninni mobil response'u.

FR8: Epic 1 - Sesli deneyimler için content-level textless kapak sahipliği.

FR9: Deferred roadmap - STORY `READING` için locale-specific kapak response'u.

FR10: Epic 1 - Ortak playback ve locale editörlerinin CMS'te ayrılması.

FR11: Epic 1 - Yeni canonical `AUDIO_STORY` oluşturmayı ve kategori kullanımını kapatma.

FR12: Epic 1 - Guarded Flyway schema contraction.

## Epic List

### Epic 1: Editörler ses ve kapak varlıklarını tutarlı yönetir

Editörler tek bir canonical hikâyeye dil bazlı anlatım ekleyebilir; ninninin ortak sesini, textless kapağını, müzisyenlerini ve katalogdan seçilmiş enstrümanlarını bir kez yönetebilir. Sistem, eski canonical `AUDIO_STORY` oluşturmayı önler ve şema değişimini veri kaybetmeden güvenceye alır.

**FRs covered:** FR1, FR4, FR5, FR6, FR8, FR10, FR11, FR12, FR13

## Epic 1: Editörler ses ve kapak varlıklarını tutarlı yönetir

Editörler tek bir canonical hikâyeye dil bazlı anlatım ekleyebilir; ninninin ortak sesini, textless kapağını, müzisyenlerini ve katalogdan seçilmiş enstrümanlarını bir kez yönetebilir. Sistem, eski canonical `AUDIO_STORY` oluşturmayı önler ve şema değişimini veri kaybetmeden güvenceye alır.

### Story 1.1: Playback işleme hedeflerini güvenilir biçimde ayırma

Bir CMS editörü olarak,
her dile ait ve tüm dillerde ortak medya işlemlerinin ayrı takip edilmesini istiyorum,
böylece bir ninninin ortak ses işlemi birden fazla dil için yinelenmez; hikâye anlatımları da doğru dile ait kalır.

**Acceptance Criteria:**

**Given** mevcut localization-bazlı asset processing kayıtları
**When** scoped processing modeli devreye alındığında
**Then** mevcut kayıtlar `LOCALIZATION(contentId, languageCode)` hedefi olarak korunur
**And** her localization hedefi için en fazla bir aktif işlem kaydı bulunur.

**Given** ninni gibi ortak playback’e sahip bir içerik
**When** `CONTENT(contentId)` hedefiyle processing planlanır
**Then** `languageCode` zorunlu olmaz ve tek kayıt oluşturulur
**And** aynı content hedefi için yinelenen aktif işlem kaydı oluşturulamaz.

**Given** `asset` modülünün public processing API’si
**When** `content` modülü iş planlama, yeniden deneme, tamamlama, hata veya durum okuma çağrısı yaptığında
**Then** hedef kapsamı açıkça belirtilir
**And** content-scoped kayıtlar `findByContent(contentId)` ile okunabilir.

**Given** CONTENT veya LOCALIZATION hedefli bir işlem başarısız olduğunda
**When** hata kaydedildiğinde
**Then** hata yalnızca kendi hedefine ait processing durumunu değiştirir
**And** başka dil veya başka içerik hedefi etkilenmez.

**Given** bu modelin Flyway migration’ı
**When** boş ve mevcut şemalı PostgreSQL veritabanlarında çalıştığında
**Then** hedef kapsamı ile dil alanı arasındaki uyumsuz kombinasyonlar veritabanı kısıtlarıyla reddedilir
**And** integration testleri her iki kapsam için planlama, tekrar deneme, tamamlama ve hata akışını doğrular.

### Story 1.2: Hikâyeye dil-bazlı tam anlatım ekleme

Bir CMS editörü olarak,
bir hikâyenin belirli diline tam sesli anlatım eklemek ve güncellemek istiyorum,
böylece aynı hikâye farklı dillerde bağımsız dinlenebilir sürümlere sahip olabilir.

**Acceptance Criteria:**

**Given** canonical türü `STORY` olan ve ilgili localization’ı bulunan bir içerik
**When** editör o localization için tam anlatım ses asset’i ve süre kaydeder
**Then** sistem `StoryNarration` verisini o `ContentLocalization` altında saklar
**And** ses asset’inin `AUDIO` türünde ve pozitif kimlikte olduğunu doğrular.

**Given** bir hikâye localization’ında sayfa bazlı ses kayıtları bulunduğunda
**When** tam anlatım eklenir veya değiştirilirse
**Then** mevcut `StoryPageLocalization` sesleri değişmez
**And** tam anlatımın varlığı sayfa seslerinden türetilmez.

**Given** tam anlatım eklenmiş veya değişmiş bir localization
**When** değişiklik kaydedildiğinde
**Then** Story 1.1’deki `LOCALIZATION(contentId, languageCode)` hedefi için processing planlanır ya da güncellenir
**And** başka localization’ların anlatım veya processing kayıtları etkilenmez.

**Given** bir `STORY` localization’ı
**When** admin API üzerinden okunur
**Then** tam anlatımın asset referansı, süre ve processing durumu ayrı bir narration alanında görünür
**And** içerik türü `STORY` olarak kalır; canonical `AUDIO_STORY` kaydı oluşturulmaz.

**Given** domain, API ve persistans değişiklikleri
**When** testler çalıştırıldığında
**Then** geçersiz asset türü, eksik localization, çapraz-dil güncellemesi ve sayfa sesiyle karışma durumları kapsanır
**And** Spring Modulith sınırları korunur.

### Story 1.3: Ortak textless kapak sahipliğini uygulama

Bir CMS editörü olarak,
textless kapağı içerik düzeyinde bir kez yönetmek istiyorum,
böylece sesli hikâye, ninni ve meditasyon her dilde doğru ortak görseli kullanır.

**Acceptance Criteria:**

**Given** `STORY`, `LULLABY` veya `MEDITATION` türünde bir içerik
**When** editör content-level textless kapak seçer veya değiştirirse
**Then** değer `Content.textlessCoverMediaId` olarak saklanır
**And** referansın pozitif kimlikli bir `IMAGE` asset’i olduğu doğrulanır.

**Given** bir `STORY` localization’ı
**When** okuma ve sesli-hikâye deneyimleri için kapak çözülürse
**Then** `READING`, locale-specific `ContentLocalization.coverMediaId` kullanır
**And** sesli-hikâye deneyimi ortak `textlessCoverMediaId` kullanır.

**Given** `LULLABY` veya `MEDITATION` localization’ı
**When** localization oluşturulur ya da güncellenirse
**Then** localization seviyesinde kapak referansı kabul edilmez
**And** ortak kapak tüm diller için content-level kaynaktan çözülür.

**Given** ortak-kapak gerektiren bir mobil deneyim
**When** textless kapak eksik, geçersiz veya çözümlenemiyorsa
**Then** ilgili deneyim mobilde görünür kabul edilmez
**And** hata başka içerik türü veya localization’ın editoryal durumunu değiştirmez.

**Given** persistans ve public-query değişiklikleri
**When** testler çalıştırıldığında
**Then** locale-specific STORY okuma kapağı ile ortak audio kapak ayrımı doğrulanır
**And** ninni/meditasyon için locale-level cover yazımı ve yanlış medya türü reddedilir.

### Story 1.4: Ninninin ortak playback’ini ve başlık-only localization’ını yönetme

Bir CMS editörü olarak,
bir ninninin sesini, süresini ve müzisyen bilgisini tek kez; her dildeki adını ise ayrı yönetmek istiyorum,
böylece sözsüz ninni tüm dillerde aynı müzik deneyimini sunar.

**Acceptance Criteria:**

**Given** canonical türü `LULLABY` olan bir içerik
**When** editör ortak playback ses asset’ini ve süreyi kaydeder veya güncellerse
**Then** sistem bunları tek `LullabyPlayback` kaydında content düzeyinde saklar
**And** ses asset’inin `AUDIO` türünde olduğu doğrulanır.

**Given** ortak ninni playback’i değiştiğinde
**When** kayıt başarılı olduğunda
**Then** Story 1.1’deki `CONTENT(contentId)` processing hedefi planlanır ya da güncellenir
**And** ninni localization’ları için yinelenen audio veya duration kaydı oluşturulmaz.

**Given** bir ninni için birden fazla localization
**When** editör bir localization’ı oluşturur veya güncellerse
**Then** yalnızca başlık ve yayın durumu kabul edilir
**And** body, description, cover, audio ve duration alanları reddedilir.

**Given** ninniye müzisyen katkısı atanır
**When** editör `MUSICIAN` rolüyle contributor seçerse
**Then** atama global content contributor olarak saklanır
**And** belirli bir locale’a bağlanarak ninni dilleri arasında farklılaştırılamaz.

**Given** admin API ile ninni okunur
**When** içerik ve localization verisi görüntülenirse
**Then** ortak playback ile global müzisyen kredileri bir kez gösterilir
**And** localization kayıtları yalnızca kendi başlık/yayın durumlarını gösterir.

**Given** domain ve persistans değişiklikleri
**When** testler çalıştırıldığında
**Then** eksik veya yanlış türde ses, locale-level playback yazımı ve locale-specific müzisyen ataması reddedilir
**And** ortak playback güncellemesinin bütün dil kayıtlarını koruduğu doğrulanır.

### Story 1.5: Ninni için katalogdan sıralı enstrüman seçme

Bir CMS editörü olarak,
ninniye kontrollü enstrüman kataloğundan bir veya daha fazla enstrüman seçmek istiyorum,
böylece “Piyano · Glockenspiel” gibi bilgiler tutarlı, sıralı ve dile uygun gösterilebilir.

**Acceptance Criteria:**

**Given** içerik modülünde kararlı kodlara sahip enstrüman katalog kayıtları
**When** katalog kaydı desteklenen bir dil için okunursa
**Then** kararlı `code` ile o dile ait `displayName` birlikte döner
**And** bir kataloğun görünen adı ninni localization’ına kopyalanmaz.

**Given** başlangıç enstrüman kataloğu
**When** Story 1.5’in referans verisi oluşturulduğunda
**Then** Türkçe kayıtlar `Çelesta`, `Bell`, `Keman`, `Rhodes`, `Glockenspiel`, `Arp`, `Vibrafon` ve `Yaylı Orkestra` değerlerini içerir
**And** her kayıt, desteklenen diğer diller için eklenmesi gereken katalog-localization değerlerini ayrı taşır.

**Given** ortak playback’i bulunan bir ninni
**When** editör katalogdan bir veya daha fazla enstrüman seçerse
**Then** seçimler `LullabyInstrument` ilişkileri olarak content düzeyinde saklanır
**And** her ilişki kalıcı, sıfırdan başlayan bir `displayOrder` taşır.

**Given** seçilmiş enstrümanların sırası
**When** editör sıralamayı değiştirirse
**Then** sistem eksiksiz ve yinelenmeyen bir sıra permütasyonu ister
**And** mobil kullanımı için seçilen sıra korunur.

**Given** enstrüman seçimi
**When** bilinmeyen, emekliye ayrılmış, yinelenen veya serbest metin değer gönderilirse
**Then** sistem bunu reddeder
**And** mevcut geçerli enstrüman seçimleri değişmeden kalır.

**Given** bir katalog enstrümanı en az bir ninnide kullanılıyorsa
**When** katalog yönetimi bu enstrümanı silmeye çalışırsa
**Then** silme reddedilir
**And** kayıt yalnızca gelecekteki seçimlerden emekliye ayrılabilir; mevcut ninni response’larında çözümlenmeye devam eder.

**Given** ninni ve enstrüman verisi
**When** testler çalıştırıldığında
**Then** çoklu seçim, sıralama, locale’a göre label çözümü, tekrar, emekliye ayırma ve silme koruması doğrulanır
**And** `MUSICIAN` contributor kredisiyle enstrüman kataloğunun ayrı kavramlar olduğu korunur.

### Story 1.6: Canonical `AUDIO_STORY` türünü güvenle kaldırma

Bir CMS editörü olarak,
aynı hikâyenin sesli sürümünü ayrı içerik türü olarak oluşturamamak istiyorum,
böylece tek hikâye kimliği ve sesli deneyim modeli korunur.

**Acceptance Criteria:**

**Given** canonical `AUDIO_STORY` desteğini kaldıran Flyway migration’ı
**When** veritabanında `AUDIO_STORY` türünde content, category veya asset-processing kaydı varsa
**Then** migration işlemi veri değiştirmeden başarısız olur
**And** hata, hangi kayıt sınıfının temizlenmesi gerektiğini anlaşılır biçimde belirtir.

**Given** `AUDIO_STORY` kullanan ilgili kayıt bulunmadığında
**When** migration çalıştığında
**Then** `contents`, kategori ve asset-processing şema kısıtları canonical `AUDIO_STORY` değerini artık kabul etmez
**And** scoped processing modelinin yeni kısıtları korunur.

**Given** content ve category uygulama API’leri
**When** istemci canonical `AUDIO_STORY` ile create veya update isteği gönderirse
**Then** istek doğrulama hatasıyla reddedilir
**And** `STORY`, `MEDITATION` ve `LULLABY` akışları etkilenmez.

**Given** CMS içerik oluşturma sözleşmesi
**When** backend tarafından desteklenen içerik türleri okunursa
**Then** `AUDIO_STORY` canonical seçenek olarak sunulmaz
**And** sesli hikâye deneyimi yalnızca `STORY` narration verisinden türetilir.

**Given** bu kalıcı mimari değişiklik
**When** story tamamlandığında
**Then** mevcut architecture dokümanı ve ADR-0007’yi supersede eden ADR/project-memory güncellemesi hazırlanır
**And** legacy bağımsız sesli hikâye importunun bu story’nin kapsamı dışında olduğu açıkça belgelenir.

**Given** Flyway, domain ve API testleri
**When** test paketi çalıştırıldığında
**Then** hem korumalı başarısızlık hem temiz geçiş senaryoları doğrulanır
**And** modül sınırı testi geçer.

### Story 1.7: CMS’te playback ve localization editörlerini ayrıştırma

Bir CMS editörü olarak,
her medya bilgisini doğru sahiplik düzeyinde düzenlemek istiyorum,
böylece aynı ses, kapak veya enstrüman bilgisini dillere tekrar tekrar girmem.

**Acceptance Criteria:**

**Given** CMS içerik oluşturma formu
**When** editör içerik türü seçerse
**Then** yalnızca `STORY`, `MEDITATION` ve `LULLABY` seçenekleri görünür
**And** `AUDIO_STORY` seçeneği hiçbir create veya filtre akışında görünmez.

**Given** bir `STORY` localization editörü
**When** editör tam anlatımı yönetirse
**Then** o dile ait narration ses asset’i, süre ve processing durumu ayrı bir anlatım alanında düzenlenir
**And** mevcut story-page metin, resim ve sayfa sesi akışı korunur.

**Given** `STORY`, `LULLABY` veya `MEDITATION` içerik editörü
**When** ortak textless kapak yönetilirse
**Then** kapak yalnızca content-level çalışma alanında seçilir
**And** editör, hangi deneyimlerin bu ortak kapağı kullandığını anlayabilir.

**Given** bir `LULLABY` içerik editörü
**When** editör playback bilgilerini yönetirse
**Then** ortak ses, süre, global müzisyen kredisi ve sıralı katalog enstrümanları aynı content-level akışta düzenlenir
**And** ninni localization editörü yalnızca başlık ve yayın durumunu gösterir.

**Given** bir `MEDITATION` localization editörü
**When** editör içerik bilgisini güncellerse
**Then** dil-bazlı ses ve metin alanları korunur
**And** localization-level kapak alanı gösterilmez.

**Given** bu UI değişiklikleri
**When** farklı viewport’larda doğrulanır
**Then** CMS UI standartlarındaki tek baskın editör akışı, görünür etiketler, erişilebilir seçimler ve kompakt asset-picker kuralları korunur
**And** ilgili component/interaction testleri ile görsel regresyon kapsamı eklenir.

### Story 1.8: Ninninin liste ve playback kapaklarını ayrı yönetme

Bir CMS editörü olarak, ninninin listede gösterilen statik kapağını ve açıldığında kullanılan
animasyonlu kapağını ayrı yönetmek istiyorum, böylece eski verideki iki görsel rolü kaybolmadan
yeni sisteme taşınabilir.

**Acceptance Criteria:**

**Given** `LULLABY` türünde bir içerik
**When** editör statik liste kapağını seçer, değiştirir veya temizlerse
**Then** değer content scope'ta `listingCoverMediaId` olarak saklanır
**And** varsa pozitif kimlikli bir `IMAGE` asset'ine referans verir.

**Given** `LULLABY` türünde bir içerik
**When** editör playback/detail kapağını seçer veya değiştirirse
**Then** mevcut `listeningCoverMediaId`, `listingCoverMediaId`den bağımsız kalır
**And** `IMAGE` olarak kaydedilmiş GIF asset'ini referans alabilir.

**Given** birden fazla localization'a sahip bir ninni
**When** ortak kapaklardan biri değişirse
**Then** localization, playback ses/süre, müzisyen, enstrüman, processing durumu ve public/mobile sözleşmesi değişmez.

**Given** `LULLABY` dışındaki bir içerik
**When** `listingCoverMediaId` içeren bir güncelleme gönderilirse
**Then** istek reddedilir ve kayıtlı değer değişmez.

**Given** CMS içerik detay ekranı
**When** editör bir ninniyi açarsa
**Then** statik liste kapağı ile animasyonlu playback kapağı ayrı, anlaşılır etiketli seçiciler olarak görünür
**And** asset doğrulama hataları ilgili alanda gösterilir.
