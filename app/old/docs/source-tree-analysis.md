# TellPal - Kaynak Ağacı Analizi

**Tarih:** 2026-07-28

## Genel Bakış

Repository tek bir Flutter mobil uygulamasıdır. Android ve iOS kabukları aynı Dart
uygulamasını çalıştırır. `v2/be` altında dosya bulunmadığından ayrı bir backend parçası
olarak sınıflandırılmamıştır.

## Tam Kaynak Yapısı

```text
tellpal/
├─ lib/
│  ├─ main.dart                     # uygulama composition root'u
│  ├─ injection_container.dart      # GetIt bağımlılık kayıtları
│  ├─ common/
│  │  ├─ components/                # ortak widget'lar ve global Cubit'ler
│  │  │  └─ cubit/                  # lazy provider/configuration
│  │  ├─ services/                  # auth, Firebase, RevenueCat, ses, cache, guard'lar
│  │  └─ ...
│  ├─ config/
│  │  ├─ routes/                    # GoRouter ve geçişler
│  │  ├─ theme/                     # uygulama teması
│  │  └─ ...                        # Firebase ve çalışma zamanı config bağlantıları
│  ├─ core/
│  │  ├─ resources/                 # DataState
│  │  ├─ usecases/                  # ortak use case tabanı
│  │  └─ ...                        # yardımcılar ve sabitler
│  ├─ features/
│  │  ├─ auth/
│  │  │  └─ presentation/           # auth ekranları, Cubit'ler, widget'lar
│  │  ├─ stories/
│  │  │  ├─ data/                   # Retrofit, model, repository implementation
│  │  │  ├─ domain/                 # entity, repository interface, use case
│  │  │  └─ presentation/           # sayfalar, widget'lar, Cubit'ler
│  │  ├─ parent_mode/
│  │  │  ├─ data/
│  │  │  ├─ domain/
│  │  │  └─ presentation/
│  │  ├─ profile/
│  │  │  ├─ data/
│  │  │  └─ presentation/
│  │  └─ relax/
│  │     ├─ domain/
│  │     └─ presentation/
│  ├─ gen/                          # Slang/generated yerelleştirme kodu
│  ├─ services/                     # uygulama düzeyi küçük servisler
│  └─ utils/                        # genel yardımcılar
├─ test/                            # unit/widget regresyon testleri
├─ assets/
│  ├─ animations/
│  ├─ customers/
│  ├─ fonts/
│  ├─ i18n/
│  ├─ icons/
│  ├─ images/
│  │  └─ landing/{tr,en,pt}/
│  ├─ logo/
│  └─ newIcons/
├─ android/
│  ├─ app/
│  │  ├─ build.gradle
│  │  └─ src/main/
│  ├─ build.gradle
│  └─ gradle.properties
├─ ios/
│  ├─ Flutter/
│  ├─ Runner/
│  ├─ Runner.xcodeproj/
│  └─ Podfile
├─ docs/                            # BMAD proje bilgisi ve mevcut raporlar
├─ design-artifacts/                # tasarım çıktıları
├─ reports/                         # proje raporları
├─ _bmad/                           # BMAD workflow altyapısı
├─ _bmad-output/                    # BMAD çalışma çıktıları
├─ .agents/                         # agent/skill tanımları
├─ .claude/ .codex/ .specify/      # geliştirme aracı yapılandırmaları
├─ pubspec.yaml                     # Flutter paket, asset ve sürüm tanımı
├─ pubspec.lock                     # çözümlenmiş Dart paketleri
├─ analysis_options.yaml            # analyzer/lint ayarları
├─ README.md
├─ architecture.md                  # mevcut kök mimari notu
├─ REVENUCAT_INTEGRATION.md
├─ AGENTS.md
└─ CLAUDE.md
```

`.dart_tool/`, `build/`, `node_modules/`, IDE dizinleri ve platform-generated geçici
çıktılar geliştirme ürünüdür; uygulama mimarisinin parçası değildir.

## Kritik Dizinler

### `lib/features/stories`

Uygulamanın en büyük ve en riskli feature'ıdır.

**Amaç:** İçerik keşfi, arama, kategori, etkileşimli okuma, medya indirme ve ses.  
**İçerik:** 77 Dart dosyası.  
**Kritik girişler:** `presentation/pages/story_content.dart`,
`presentation/cubit/remote/story_content_cubit.dart`,
`presentation/cubit/remote/story_second_part_cubit.dart`.  
**Entegrasyon:** REST + Remote Config + Storage + yerel dosya sistemi + audio + geçmiş.

### `lib/common/services`

Feature'lar arası teknik davranışların yoğunlaştığı servis katmanıdır.

**Amaç:** Firebase, auth, RevenueCat, cache, ses, başlangıç ve runtime guard'ları.  
**İçerik:** Servisler ve singleton yaşam döngüsü yardımcıları.  
**Kritik girişler:** `firebase_service.dart`, `firebase_auth_service.dart`,
`firebase_remote_config_service.dart`, `player_manager_service.dart`.

### `lib/common/components/cubit`

Global ve feature Cubit'lerinin uygulama ağacına sağlanmasını yönetir.

**Amaç:** Lazy initialization ve başlangıç öncelikleri.  
**İçerik:** `cubit_configurations.dart`, `lazy_bloc_provider.dart`, premium ve user type
Cubit'leri.  
**Entegrasyon:** `main.dart` ve GetIt singleton kayıtları.

### `lib/config/routes`

GoRouter rota sözleşmesidir.

**Amaç:** 23 rota, sayfa geçişleri ve ön yükleme.  
**Kritik giriş:** `app_routes.dart`.  
**Not:** Bazı ekranlar bu dizinin dışında Navigator ile açılır.

### `lib/injection_container.dart`

Uygulamanın composition root bağımlılık grafiğidir.

**Amaç:** Firebase, Dio, cache, API servisleri, repository, use case ve Cubit kaydı.  
**Risk:** Çok sayıda singleton, test izolasyonu ve yaşam döngüsü yönetimini zorlaştırır.

### `test`

**Amaç:** Unit/widget regresyon koruması.  
**İçerik:** 28 dosya; bunların 27'si `*_test.dart` test kaynağıdır.  
**Güçlü alanlar:** ZIP/dosya güvenliği, ses, RevenueCat guard/queue, runtime guard'lar.  
**Zayıf alanlar:** Auth/REST entegrasyonu, rota akışları, iki ZIP hızlı kaydırma sınırı,
profile/parent/relax UI ve gerçek uçtan uca senaryolar.

## Giriş Noktaları

- **Ana giriş:** `lib/main.dart`
- **DI başlangıcı:** `lib/injection_container.dart`
- **Navigasyon:** `lib/config/routes/app_routes.dart`
- **Android uygulama tanımı:** `android/app/src/main/AndroidManifest.xml`
- **iOS uygulama tanımı:** `ios/Runner/Info.plist`

`main.dart`; Firebase başlatma, Remote Config, dependency injection, RevenueCat,
analitik/attribution, global hata yakalama, lazy Cubit ağacı, performans izleme, ses yaşam
döngüsü ve router kurulumunu aynı başlangıç zincirinde toplar.

## Dosya Organizasyonu Örüntüleri

- Feature tabanlı üst seviye ayrım.
- Stories ve parent_mode içinde `data/domain/presentation` katmanları.
- Auth, profile ve relax feature'larında Clean Architecture katmanlarının daha kısmi
  uygulanması.
- Cubit ve state dosyalarının çoğunlukla ayrı tutulması.
- Widget'ların `presentation/widgets` altında feature'a özel tutulması.
- Retrofit ve Slang generated dosyalarının kaynak dosyaların yanında/`gen` altında
  bulunması.
- Servis ve DI erişiminin `common` ve composition root'ta merkezileşmesi.

## Ana Dosya Tipleri

| Tip | Örüntü | Amaç | Örnek |
|---|---|---|---|
| Ekran | `pages/*.dart`, `page/*.dart` | Rota veya navigasyon hedefi | `story_content.dart` |
| Widget | `widgets/*.dart` | Tekrar kullanılabilir UI | `story_item_widget.dart` |
| Cubit | `*_cubit.dart` | Durum ve async koordinasyon | `stories_cubit.dart` |
| State | `*_state.dart` | UI durumları | `stories_state.dart` |
| Entity | `domain/entities/*.dart` | Domain modeli | `story_entity.dart` |
| Model | `data/models/*.dart` | JSON/Firebase dönüşümü | `story_model.dart` |
| Repository | `repository/*.dart` | Veri erişim sınırı | `story_repository_impl.dart` |
| API | `*_api_service.dart` | Retrofit sözleşmesi | `story_api_service.dart` |
| Generated | `*.g.dart`, `lib/gen/*` | Codegen çıktısı | `story_api_service.g.dart` |
| Test | `*_test.dart` | Unit/widget test | `*_test.dart` |

## Asset Konumları

- **Görsel/landing:** `assets/images` — 68 dosya
- **İkonlar:** `assets/icons`, `assets/newIcons` — 68 dosya
- **Fontlar:** `assets/fonts` — 31 dosya
- **Animasyon:** `assets/animations` — 4 dosya
- **Yerelleştirme kaynakları:** `assets/i18n` — 5 dosya
- **Logo:** `assets/logo` — 2 dosya

Ayrıntılar [Asset Inventory](./asset-inventory.md) belgesindedir.

## Yapılandırma Dosyaları

- `pubspec.yaml`: sürüm, paketler, asset bildirimleri.
- `pubspec.lock`: çözümlenmiş paket sürümleri.
- `analysis_options.yaml`: Flutter lints ve generated dosya dışlamaları.
- `android/app/build.gradle`: Android SDK, application ID, signing ve native bağımlılıklar.
- `ios/Podfile`: iOS 15, CocoaPods ve notification permission flag'i.
- `ios/Runner/Info.plist`: iOS yetenek/metadata.
- `.metadata`: Flutter proje oluşturma/migration metadata'sı.

## Geliştirme Notları

- Generated dosyaları elle değiştirmeyin; build_runner/Slang kaynağını güncelleyin.
- Kırılgan iki ZIP hikâye akışına geniş refactor içinde dokunmayın; önce neden ve hata
  senaryolarını belgeleyip açık onay alın.
- Secret içeren platform dosyalarını çıktı veya dokümana kopyalamayın.
- Yeni feature eklerken DI, Cubit configuration, route, analytics ve localization
  değişikliklerinin birlikte gerekip gerekmediğini kontrol edin.
- `v2/be`, mevcut haliyle backend değildir.

---

_BMAD Method `document-project` workflow ile üretildi._
