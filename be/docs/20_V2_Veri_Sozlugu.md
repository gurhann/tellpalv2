# V2 Veri Sözlüğü (Target Şema)

Kaynak şema: `docs/v2_schema_target.dbml` (dbdiagram.io / DBML).

Bu doküman, V2 hedef veritabanı tasarımındaki **tüm tabloların** ve **kolonların** amacını açıklar.

---

## Genel Konvansiyonlar

- **Zaman alanları**
  - `created_at`: kaydın oluşturulma zamanı (server-side).
  - `updated_at`: kaydın güncellenme zamanı (uygulama/trigger ile güncellenmeli).
  - `occurred_at`: olayın **client** tarafında gerçekleştiği zaman (özellikle event tabloları).
  - `ingested_at`: olayın **server** tarafından alındığı zaman (ingest).
  - Tüm timestamp alanları `timestamptz` olup UTC saklanması önerilir.
- **Dil bazlı görünürlük (publish)**
  - İçerik görünürlüğü `v2_content_localizations.status = 'PUBLISHED'`.
  - Kategori görünürlüğü `v2_category_localizations.status = 'PUBLISHED'`.
  - Bir dilde çevirisi olmayan içerik/kategori **o dilde görünmemeli** (uygulama seviyesinde).
- **Soft delete / yayından kaldırma**
  - `is_active=false` alanı “kayıt var ama listelerde görünmesin / kullanılamasın” için soft-disable yaklaşımıdır.
- **Idempotency**
  - `v2_content_events.event_id` ve `v2_app_events.event_id` client tarafından üretilen UUID olmalıdır (tekrar gönderimde duplicate insert engeli).
  - RevenueCat webhook idempotency için `v2_purchase_events.revenuecat_event_id` kullanılır (unique).
- **JSON alanları**
  - `jsonb` alanları (örn. `metadata`, `payload`, `raw_payload`) şema evrimi ve esneklik için kullanılır; kritik raporlama alanları mümkünse kolon olarak normalize tutulur.

---

## Enum ve Kod Setleri

Bu dokümanda bazı alanlar “enum” gibi davranır. PostgreSQL’de bunu **CHECK constraint**, **native enum** veya **lookup tablo + FK** ile enforce edebilirsiniz.

### Enum: `v2_media_kind`
- `IMAGE`: görsel dosya referansı
- `AUDIO`: ses dosya referansı

### Enum: `v2_content_type`
- `STORY`: sayfalı içerik (sayfalar + sayfa lokalizasyonları)
- `AUDIO_STORY`: sayfalı ama ağırlıkla ses/okuma deneyimi (yine sayfa kurgusu üzerinden)
- `MEDITATION`: tek sayfalık içerik (body_text + audio)
- `LULLABY`: tek sayfalık içerik (opsiyonel body_text + audio)

### Enum: `v2_localization_status`
- `DRAFT`: admin/CMS taslak; client’a görünmez
- `PUBLISHED`: client’a görünür
- `ARCHIVED`: tarihsel/geri çekilmiş; client’a görünmez

### Enum: `v2_category_type`
- `CONTENT`: uygulama içerik kategorileri
- `PARENT_GUIDANCE`: ebeveyn rehberi alanı (ayrı liste/akış)

### Enum: `v2_content_event_type`
- `START`: içerik başlatıldı
- `EXIT`: içerikten çıkıldı
- `COMPLETE`: içerik tamamlandı

### Enum: `v2_app_event_type` (genişletilmiş)
- `APP_OPENED`: uygulama açıldı
- `ONBOARDING_STARTED`: onboarding başladı
- `ONBOARDING_COMPLETED`: onboarding tamamlandı
- `ONBOARDING_SKIPPED`: onboarding atlandı
- `PAYWALL_SHOWN`: paywall gösterildi
- `LOCKED_CONTENT_CLICKED`: kilitli içerik tıklandı

### Enum: `v2_purchase_source`
- `REVENUECAT_WEBHOOK`: RevenueCat server-to-server webhook
- `CLIENT`: client tarafından gönderilen purchase/event

### Enum: `v2_subscription_product_type`
- `SUBSCRIPTION`: yenilenen abonelik
- `NON_RENEWING`: yenilenmeyen ürün (tek sefer/periodik olmayan)

### Enum: `v2_billing_period_unit`
- `DAY`, `WEEK`, `MONTH`, `YEAR`: billing period birimi

### Enum: `v2_contributor_role`
- `AUTHOR`: yazar
- `ILLUSTRATOR`: çizer
- `NARRATOR`: seslendirmen (dil bazlı olabilir)
- `MUSICIAN`: müzisyen (özellikle ninni vb.)

### Lookup tabloları (analytics / doğrulama)

- `v2_purchase_event_types`: RevenueCat `type` seti (örn. `INITIAL_PURCHASE`, `RENEWAL`, `CANCELLATION`, `EXPIRATION`, …)
- `v2_subscription_period_types`: RevenueCat `period_type` seti (örn. `TRIAL`, `INTRO`, `NORMAL`, `PROMOTIONAL`, `PREPAID`)
- `v2_purchase_stores`: store kodları (örn. `APP_STORE`, `PLAY_STORE`, `STRIPE`, `RC_BILLING`, …)
- `v2_purchase_environments`: `SANDBOX` / `PRODUCTION`
- `v2_purchase_reason_codes`: iptal/expiration reason kodları (tek tabloda; `reason_type` ile ayrılır)

---

## Tablolar

### `v2_languages`

Amaç: Sistemde desteklenen dilleri tanımlar (seed ile yüklenir).

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `code` | `varchar(10)` | Dil kodu (örn. `tr`, `en`); lokalizasyon tablolarında FK olarak kullanılır. |
| `display_name` | `text` | Admin/CMS ekranlarında gösterilecek ad. |
| `is_active` | `boolean` | Dil aktif mi? (feature flag gibi; listelerde filtrelemek için). |
| `created_at` | `timestamptz` | Kaydın oluşturulma zamanı. |

---

### `v2_media_assets`

Amaç: Firebase Storage gibi dış bir storage provider’daki medya dosyalarının referanslarını tutar.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `id` | `bigint` | PK. |
| `provider` | `varchar(32)` | Storage sağlayıcı kodu (örn. `FIREBASE_STORAGE`). |
| `object_path` | `text` | Provider içindeki obje yolu (path). |
| `kind` | `v2_media_kind` | Medya türü (IMAGE/AUDIO). |
| `mime_type` | `text` | Opsiyonel; dosyanın MIME tipi (örn. `image/png`). |
| `bytes` | `bigint` | Opsiyonel; dosya boyutu. |
| `checksum_sha256` | `char(64)` | Opsiyonel; bütünlük/dedupe için hash. |
| `download_url` | `text` | Opsiyonel cache; liste ekranlarında hızlı kullanım için URL. (Güvenlik gereği loglamamak önerilir.) |
| `created_at` | `timestamptz` | Kaydın oluşturulma zamanı. |

Not: `(provider, object_path)` unique olmalıdır (aynı dosya bir kez kaydedilsin).

---

### `v2_app_users`

Amaç: Firebase kullanıcı kimliğini Postgres’te kanonik kullanıcı kaydına bağlar.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `id` | `bigint` | PK. |
| `firebase_uid` | `varchar(128)` | Firebase Auth UID; unique (aynı UID tek kullanıcı). |
| `is_allow_marketing` | `boolean` | Pazarlama izni / opt-in bilgisi. |
| `created_at` | `timestamptz` | Kaydın oluşturulma zamanı. |
| `updated_at` | `timestamptz` | Kaydın güncellenme zamanı. |

---

### `v2_user_profiles`

Amaç: Kullanıcının uygulama içi profili (tek kullanıcı → çoklu profil için hazır).

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `id` | `bigint` | PK. |
| `user_id` | `bigint` | `v2_app_users.id` FK; profilin sahibi. |
| `name` | `text` | Profil adı (opsiyonel). |
| `age_range` | `varchar(32)` | Segment/yaş aralığı kodu (uygulama enum’u; örn. `UNKNOWN`, `ZERO_TO_TWO`, …). |
| `avatar_media_id` | `bigint` | `v2_media_assets.id` FK; profil avatarı. |
| `favorite_genres` | `text[]` | Favori türler (app enum string değerleri). |
| `main_purposes` | `text[]` | Kullanım amacı etiketleri (app enum string değerleri). |
| `is_primary` | `boolean` | Kullanıcının ana profili mi? (1 kullanıcıda sadece 1 primary önerilir). |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |
| `updated_at` | `timestamptz` | Güncellenme zamanı. |

Not: “tek primary profil” kuralı için `(user_id) UNIQUE WHERE is_primary=true` partial unique index önerilir.

---

### `v2_contents`

Amaç: İçeriğin dil bağımsız (kanonik) kaydı.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `id` | `bigint` | PK. |
| `type` | `v2_content_type` | İçerik türü. |
| `external_key` | `text` | CMS/import/deep-link için dil bağımsız kalıcı anahtar; NOT NULL + UNIQUE. |
| `is_premium` | `boolean` | Premium meta bilgisi (kilitlemeyi çoğunlukla client yapar). |
| `is_active` | `boolean` | Soft disable; içerik listelerden saklanır. |
| `age_range` | `int` | Opsiyonel; tek yaş (örn. 3/4/5). |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |
| `updated_at` | `timestamptz` | Güncellenme zamanı. |

Not: Yazar/çizer/seslendirmen/müzisyen gibi “credit” alanları `v2_content_contributors` üzerinden yönetilir.

---

### `v2_contributors`

Amaç: İçeriklerde geçen kişi/kurum kayıtları (yazar, seslendirmen, müzisyen vb.) için sözlük/dimension tablo.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `id` | `bigint` | PK. |
| `display_name` | `text` | Gösterim adı (arama için ana alan). |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |
| `updated_at` | `timestamptz` | Güncellenme zamanı. |

Not: İsim araması gerekiyorsa `display_name` için `pg_trgm` + GIN index önerilir.

---

### `v2_content_contributors`

Amaç: İçerik ↔ contributor ilişkisi ve rol bilgisi. Dil bazlı credit’leri destekler (örn. seslendirmen dil bazında değişebilir).

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `id` | `bigint` | PK. |
| `content_id` | `bigint` | `v2_contents.id` FK; hangi içerik. |
| `contributor_id` | `bigint` | `v2_contributors.id` FK; hangi kişi/kurum. |
| `role` | `v2_contributor_role` | Contributor rolü (AUTHOR/ILLUSTRATOR/NARRATOR/MUSICIAN). |
| `language_code` | `varchar(10)` | Opsiyonel; rol dil bazlıysa (özellikle NARRATOR) doldurulur. |
| `credit_name` | `text` | Opsiyonel; ekranda gösterilecek isim override’ı (varsayılan contributor.display_name). |
| `sort_order` | `int` | Birden fazla kişi varsa sıralama (0..n). |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |
| `updated_at` | `timestamptz` | Güncellenme zamanı. |

Notlar:
- Tekillik önerisi: `language_code IS NULL` iken `(content_id, role, contributor_id)` unique; `language_code IS NOT NULL` iken `(content_id, role, contributor_id, language_code)` unique (partial unique index).
- `language_code` NULL olabilir; doluysa `v2_languages.code`’a referans vermelidir.

---

### `v2_content_localizations`

Amaç: İçeriğin dil bazlı metin, medya ve publish durumunu tutar.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `content_id` | `bigint` | PK parçası; `v2_contents.id` FK. |
| `language_code` | `varchar(10)` | PK parçası; `v2_languages.code` FK. |
| `title` | `text` | Lokalize başlık. |
| `description` | `text` | Lokalize açıklama/summary. |
| `body_text` | `text` | Tek sayfalık içeriklerde (meditasyon gibi) ana metin. |
| `cover_media_id` | `bigint` | Kapak görseli (`v2_media_assets.id`). |
| `audio_media_id` | `bigint` | Tek sayfalık içeriklerde ana ses dosyası (`v2_media_assets.id`). |
| `duration_minutes` | `int` | Dil bazlı süre (dakika). |
| `status` | `v2_localization_status` | Dil bazlı yayın durumu (publish filtrelerinde kullanılır). |
| `published_at` | `timestamptz` | Yayın zamanı; sıralama/analiz için. |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |
| `updated_at` | `timestamptz` | Güncellenme zamanı. |

Not: Seslendirmen/voice actor gibi dil bazlı credit alanları `v2_content_contributors` (role=`NARRATOR`, language_code=...) üzerinden okunmalıdır.

---

### `v2_content_story_details`

Amaç: Sadece `STORY` tipi içeriklere ait ek detaylar.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `content_id` | `bigint` | PK/FK; `v2_contents.id`. |
| `page_count` | `int` | Sayfa sayısı (tamlık doğrulaması / editör kontrolü). |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |
| `updated_at` | `timestamptz` | Güncellenme zamanı. |

Not: “Bu satır sadece STORY için olmalı” kuralı uygulama/trigger ile enforce edilir.
Not: Çizer gibi contributor bilgileri `v2_content_contributors` (role=`ILLUSTRATOR`) üzerinden yönetilir.

---

### `v2_story_pages`

Amaç: Story’nin dil bağımsız sayfa kurgusu (ör. illüstrasyon).

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `content_id` | `bigint` | PK parçası; ilgili story (`v2_contents.id`). |
| `page_number` | `int` | PK parçası; 1’den başlayan sayfa numarası. |
| `illustration_media_id` | `bigint` | Sayfa görseli (`v2_media_assets.id`). |

---

### `v2_story_page_localizations`

Amaç: Story sayfalarının dil bazlı metin ve sesleri.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `content_id` | `bigint` | PK parçası; story id. |
| `page_number` | `int` | PK parçası; sayfa numarası. |
| `language_code` | `varchar(10)` | PK parçası; dil kodu. |
| `text_content` | `text` | Sayfa metni. |
| `audio_media_id` | `bigint` | Sayfa ses dosyası (`v2_media_assets.id`) — dil bazlı. |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |
| `updated_at` | `timestamptz` | Güncellenme zamanı. |

---

### `v2_categories`

Amaç: Kategori kanonik kaydı (dil bağımsız); slug ile kalıcı anahtar.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `id` | `bigint` | PK. |
| `slug` | `text` | Kategori kalıcı anahtarı; unique (örn. `sleep`, `focus`). |
| `type` | `v2_category_type` | Kategori alanı (content / parent guidance). |
| `is_premium` | `boolean` | Premium meta (kategorideki içerikler premium olabilir). |
| `is_active` | `boolean` | Soft disable; kategori listelerden saklanır. |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |
| `updated_at` | `timestamptz` | Güncellenme zamanı. |

---

### `v2_category_localizations`

Amaç: Kategorinin dil bazlı adı/açıklaması/görseli + publish durumu.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `category_id` | `bigint` | PK parçası; `v2_categories.id` FK. |
| `language_code` | `varchar(10)` | PK parçası; `v2_languages.code` FK. |
| `name` | `text` | Lokalize kategori adı. |
| `description` | `text` | Lokalize açıklama. |
| `image_media_id` | `bigint` | Kategori görseli (`v2_media_assets.id`). |
| `status` | `v2_localization_status` | Dil bazlı yayın durumu. |
| `published_at` | `timestamptz` | Yayın zamanı; sıralama/analiz için. |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |
| `updated_at` | `timestamptz` | Güncellenme zamanı. |

---

### `v2_category_contents`

Amaç: Kategori → içerik ilişkisini **dil bazında** ve sıralı şekilde tutar.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `category_id` | `bigint` | PK parçası; kategori id. |
| `language_code` | `varchar(10)` | PK parçası; ilişki dili. |
| `content_id` | `bigint` | PK parçası; içerik id. |
| `display_order` | `int` | Liste sırası (aynı kategori+dil içinde unique önerilir). |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |

Not: Bu tasarım, aynı içerik/kategorinin farklı dillerde farklı kürasyonla listelenmesini sağlar.

---

### `v2_content_events`

Amaç: Offline-first içerik tüketim event’leri (START/EXIT/COMPLETE) — analytics için temel kaynak.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `event_id` | `uuid` | PK; client üretir (idempotency). |
| `profile_id` | `bigint` | `v2_user_profiles.id` FK; event’in sahibi profil. |
| `content_id` | `bigint` | `v2_contents.id` FK; hangi içerik. |
| `language_code` | `varchar(10)` | `v2_languages.code` FK; tüketim dili (lokalizasyon FK’si zorunlu değil). |
| `event_type` | `v2_content_event_type` | START/EXIT/COMPLETE. |
| `occurred_at` | `timestamptz` | Client zamanı (analitik zaman pencereleri için esas). |
| `ingested_at` | `timestamptz` | Server ingest zamanı (pipeline ölçümü). |
| `session_id` | `uuid` | Opsiyonel; aynı oturum içindeki event’leri gruplayabilmek için. |
| `left_page` | `int` | Opsiyonel; EXIT anında kalınan/çıkılan sayfa (story). |
| `engagement_seconds` | `int` | Opsiyonel; etkileşim süresi (sn). |
| `metadata` | `jsonb` | Opsiyonel; ek segment alanları (`app_version`, `platform`, `country` vb.). |
| `legacy_event_key` | `text` | Opsiyonel; Firebase import idempotency/tekillik için. |

Not: `legacy_event_key` varsa `(profile_id, legacy_event_key)` unique (partial) önerilir.

---

### `v2_app_events`

Amaç: Uygulama içi funnel/monetization event’leri (paywall, onboarding, vb.).

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `event_id` | `uuid` | PK; client üretir (idempotency). |
| `profile_id` | `bigint` | `v2_user_profiles.id` FK. |
| `event_type` | `v2_app_event_type` | Event taksonomisi (APP_OPENED, PAYWALL_SHOWN, …). |
| `content_id` | `bigint` | Opsiyonel; event bir içerikle ilişkiliyse (`LOCKED_CONTENT_CLICKED`). |
| `occurred_at` | `timestamptz` | Client zamanı. |
| `ingested_at` | `timestamptz` | Server ingest zamanı. |
| `payload` | `jsonb` | Opsiyonel; event’e özgü bağlam (örn. offering id, ekran adı). |
| `legacy_event_key` | `text` | Opsiyonel; import idempotency için. |

---

### `v2_subscription_products`

Amaç: Abonelik ürün kataloğu (store + product_id ile kanonik).

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `store` | `varchar(32)` | PK parçası; store kodu (lookup/FK önerilir). |
| `product_id` | `text` | PK parçası; store product id (Play Store’da `subscription_id:base_plan_id` gibi ham değer olabilir). |
| `product_type` | `v2_subscription_product_type` | SUBSCRIPTION / NON_RENEWING. |
| `billing_period_unit` | `v2_billing_period_unit` | Period birimi. |
| `billing_period_count` | `int` | Period çarpanı (1/3/12 vb.). |
| `entitlement_ids` | `jsonb` | RevenueCat entitlement mapping (liste/obje). |
| `is_active` | `boolean` | Ürün aktif mi? |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |
| `updated_at` | `timestamptz` | Güncellenme zamanı. |

Not: Fiyat bilgisi burada kanonik tutulmaz; gerçek fiyatlar purchase event payload’larından gelir.

---

### `v2_purchase_event_types`

Amaç: Purchase event tiplerinin (RevenueCat `type`) doğrulama tablosu.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `code` | `varchar(64)` | PK; event type kodu. |
| `description` | `text` | Açıklama. |
| `is_active` | `boolean` | Kod aktif mi? (yeni kod eklemek için). |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |

---

### `v2_subscription_period_types`

Amaç: RevenueCat `period_type` doğrulama tablosu.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `code` | `varchar(32)` | PK; period type kodu (TRIAL/INTRO/NORMAL/…). |
| `description` | `text` | Açıklama. |
| `is_active` | `boolean` | Aktiflik. |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |

---

### `v2_purchase_stores`

Amaç: Store kodlarının doğrulama tablosu.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `code` | `varchar(32)` | PK; store kodu. |
| `description` | `text` | Açıklama. |
| `is_active` | `boolean` | Aktiflik. |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |

---

### `v2_purchase_environments`

Amaç: Purchase environment doğrulama tablosu (sandbox/production).

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `code` | `varchar(16)` | PK; environment kodu. |
| `description` | `text` | Açıklama. |
| `is_active` | `boolean` | Aktiflik. |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |

---

### `v2_purchase_reason_codes`

Amaç: Cancellation/expiration reason kodlarını tek tabloda yönetmek.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `code` | `varchar(32)` | PK; reason kodu. |
| `reason_type` | `varchar(32)` | Kodun türü (örn. `CANCEL_REASON`, `EXPIRATION_REASON`). |
| `description` | `text` | Açıklama. |
| `is_active` | `boolean` | Aktiflik. |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |

---

### `v2_purchase_events`

Amaç: Purchase event kaydı (RevenueCat webhook veya client) + ham payload + normalize analitik alanlar.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `id` | `bigint` | PK. |
| `user_id` | `bigint` | `v2_app_users.id` FK; satın alan kullanıcı. |
| `occurred_at` | `timestamptz` | Satın alma zamanı (doc: `purchased_at_ms`). |
| `ingested_at` | `timestamptz` | Server ingest zamanı. |
| `source` | `v2_purchase_source` | Webhook mu client mı? |
| `event_type` | `varchar(64)` | RevenueCat event type (lookup/FK önerilir). |
| `product_id` | `text` | Ürün kimliği (ham). |
| `entitlement_id` | `text` | Opsiyonel entitlement id (payload’da gelebilir). |
| `store` | `varchar(32)` | Store kodu (lookup/FK). |
| `price_micros` | `bigint` | Legacy/uyumluluk alanı; mikro birim fiyat. |
| `currency` | `varchar(3)` | Para birimi (ISO-4217). |
| `is_trial` | `boolean` | Legacy/uyumluluk trial flag’i. |
| `revenuecat_event_id` | `text` | Webhook `id`; idempotency için unique önerilir. |
| `raw_payload` | `jsonb` | Ham webhook/client payload. |
| `created_at` | `timestamptz` | Kaydın oluşturulma zamanı. |
| `event_timestamp_at` | `timestamptz` | Webhook oluşturulma zamanı (`event_timestamp_ms`). |
| `expiration_at` | `timestamptz` | Bitiş zamanı (`expiration_at_ms`). |
| `grace_period_expiration_at` | `timestamptz` | Grace period bitişi. |
| `auto_resume_at` | `timestamptz` | Auto-resume zamanı (paused). |
| `period_type` | `varchar(32)` | TRIAL/INTRO/NORMAL/… (lookup/FK). |
| `is_trial_conversion` | `boolean` | Trial’dan paid’e dönüşüm mü? |
| `cancel_reason` | `varchar(32)` | İptal nedeni kodu (lookup/FK). |
| `expiration_reason` | `varchar(32)` | Expiration nedeni kodu (lookup/FK). |
| `price` | `numeric` | USD fiyat (doc notuna göre). |
| `price_in_purchased_currency` | `numeric` | Orijinal para biriminde fiyat. |
| `tax_percentage` | `numeric` | Vergi oranı. |
| `commission_percentage` | `numeric` | Store komisyon oranı. |
| `transaction_id` | `text` | Store transaction id. |
| `original_transaction_id` | `text` | İlk transaction id. |
| `renewal_number` | `int` | Yenileme numarası. |
| `offer_code` | `text` | Offer code (varsa). |
| `country_code` | `varchar(2)` | Ülke kodu (ISO-3166). |
| `environment` | `varchar(16)` | SANDBOX/PRODUCTION (lookup/FK). |
| `presented_offering_id` | `text` | Sunulan offering id (varsa). |
| `new_product_id` | `text` | Product change durumunda yeni product id. |
| `net_revenue_micros` | `bigint` | Opsiyonel; net gelir (hesaplanmış). |

Notlar:
- `(store, product_id)` → `v2_subscription_products` linki ürün katalog eşlemesi içindir; `store` NULL ise FK enforce edilmez (Postgres davranışı).
- Numeric alanlarda (price/percent/micros) negatif değerleri engellemek için CHECK constraint önerilir.

---

### `v2_purchase_context_snapshots`

Amaç: Purchase anındaki (veya sonrasında) attribution için “snapshot” kayıtları.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `id` | `bigint` | PK. |
| `purchase_event_id` | `bigint` | `v2_purchase_events.id` FK; unique (1 purchase → 1 snapshot). |
| `user_id` | `bigint` | `v2_app_users.id` FK. |
| `profile_id` | `bigint` | Opsiyonel; `v2_user_profiles.id` (profil bulunamazsa NULL). |
| `attribution_window_seconds` | `int` | Geriye dönük attribution penceresi (default 86400 = 24 saat). |
| `attributed_app_event_id` | `uuid` | Opsiyonel; purchase’ı tetiklediği varsayılan `v2_app_events.event_id`. |
| `attributed_content_id` | `bigint` | Opsiyonel; ilişkilendirilen içerik (`v2_contents.id`). |
| `profile_snapshot` | `jsonb` | Zorunlu; purchase anındaki profil özet bilgisi (örn. age_range, amaçlar). |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |

---

### `v2_admin_users`

Amaç: CMS/admin panel kullanıcıları (app kullanıcılarından ayrıdır).

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `id` | `bigint` | PK. |
| `username` | `varchar(128)` | Admin kullanıcı adı; unique. |
| `password_hash` | `text` | Bcrypt hash (ham şifre saklanmaz). |
| `enabled` | `boolean` | Hesap aktif mi? |
| `created_at` | `timestamptz` | Oluşturulma zamanı. |
| `updated_at` | `timestamptz` | Güncellenme zamanı. |
| `last_login_at` | `timestamptz` | Son login zamanı (opsiyonel). |

---

### `v2_admin_roles`

Amaç: Rol tanımları (RBAC).

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `code` | `varchar(64)` | PK; rol kodu (örn. `ADMIN`). |
| `description` | `text` | Açıklama. |

---

### `v2_admin_user_roles`

Amaç: Admin kullanıcı ↔ rol eşlemesi.

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `admin_user_id` | `bigint` | PK parçası; `v2_admin_users.id` FK. |
| `role_code` | `varchar(64)` | PK parçası; `v2_admin_roles.code` FK. |
| `created_at` | `timestamptz` | Atama zamanı. |

---

### `v2_admin_refresh_tokens`

Amaç: Admin auth refresh token yönetimi (hash’li saklama, revoke, rotation).

| Kolon | Tip | Amaç |
| --- | --- | --- |
| `id` | `bigint` | PK. |
| `admin_user_id` | `bigint` | `v2_admin_users.id` FK. |
| `token_hash` | `char(64)` | Refresh token’ın SHA-256 hash’i; unique. (Ham token DB’ye yazılmaz.) |
| `issued_at` | `timestamptz` | Token oluşturulma zamanı. |
| `expires_at` | `timestamptz` | Token geçerlilik bitişi. |
| `revoked_at` | `timestamptz` | İptal edildiyse iptal zamanı. |
| `replaced_by_token_hash` | `char(64)` | Rotation sırasında yeni token hash referansı. |
| `user_agent` | `text` | Opsiyonel; token üretildiği cihaz bilgisi. |
| `ip` | `inet` | Opsiyonel; token üretildiği IP. |
| `created_at` | `timestamptz` | Kaydın oluşturulma zamanı. |

Not: `expires_at > issued_at` CHECK constraint önerilir.
