# TellPal Bileşen Envanteri

**Tarih:** 2026-07-28  
**Kapsam:** Ekranlar, tekrar kullanılabilir widget'lar, Cubit'ler ve temel servisler

## 1. Feature Bileşenleri

| Feature | Ekranlar | Başlıca state/veri | Harici bağımlılıklar |
|---|---|---|---|
| Auth | Splash, landing, onboarding, login, register, profile setup, verify/reset, PDF | 9 auth Cubit'i, Firebase user/profile | Firebase Auth, Database, Storage, Google, Apple |
| Stories | Home, reading, stories, search, category, info, content, lullaby | 12 story Cubit'i, Story/StoryPoint | REST, Storage, audio, RevenueCat |
| Parent Mode | Vitrin, kategori, bilgi, içerik, dinleme | 3 Cubit, ParentGuidanceBook | REST, Remote Config, audio, Database |
| Profile | Profil, edit, hesap, dil, parola, silme, feedback | 4 Cubit + premium/user type | Database, Auth, RevenueCat, permissions |
| Relax | Relax ve meditation info | RelaxCubit + PlayerManager | REST, Storage, audio, RevenueCat |

Ekranların rota ve geçiş ilişkileri [Ekran ve Akış Haritası](./screen-flow-map.md)
belgesinde ayrıntılıdır.

## 2. Ortak UI Bileşenleri

| Bileşen | Rol |
|---|---|
| `AlertMessageWidget` | Uyarı/mesaj sunumu |
| `AppRatingWidget` | Uygulama değerlendirme istemi |
| `BlueGradientContainer` | Ortak dekoratif arka plan |
| `MessageBoxWidget` | Mesaj/girdi kabı |
| `MessageSendWidget` | Mesaj gönderme girişi |
| `NoConnectionWidget` | Çevrimdışı/hata görünümü |
| `PopupWidget` | Genel popup yapısı |
| `PremiumBadge` | Premium işareti |
| `PremiumContentWidget` | Kilitli içeriğin sunumu |
| `SocialButton` | Sosyal giriş düğmesi |
| `UserTypeButton` | Çocuk/ebeveyn modu seçimi |

Bu bileşenler `lib/common/components/` altındadır. Tasarım tokenları ve erişilebilirlik
sözleşmeleri bileşen API'lerine açık biçimde yansıtılmamıştır.

## 3. Hikâye Widget'ları

| Grup | Bileşenler | Rol |
|---|---|---|
| Vitrin | `CarouselSliderWidget`, `CategoriesGrid`, `HorizontalStoryList`, `StoriesGrid`, `StoryItemWidget` | İçerik keşfi |
| Bilgi | `StoryInfoChangeButton` | Bilgi/aksiyon görünümü |
| Okuyucu panelleri | `TopStoryPanelWidget`, `BottomStoryPanelWidget`, `StoryPanelButtonWidget` | Okuma kontrolleri |
| Etkileşim | `StoryOptionsWidget` | StoryPoint seçimleri |
| Durum | `StoryLoadingWidget` | İçerik türüne göre yükleme |
| Tamamlama | `StoryLastPageWidget` | Puanlama, benzer içerik, paywall |

Okuyucu widget'ları `StoryContentView` ile sıkı bağlıdır; yeniden geliştirmede okuyucu
domain state'i ile saf sunum bileşenlerinin ayrılması gerekir.

## 4. Parent Mode Widget'ları

| Bileşen | Rol |
|---|---|
| `FreeParentModeWidget` | Günlük ücretsiz rehber |
| `HorizontalParentModeList` | Yatay kategori içeriği |
| `ParentModeGrid` | Kategori grid'i |
| `ParentModeItemWidget` | Rehber kartı ve premium kilit |

## 5. Relax Widget'ları

| Bileşen | Rol |
|---|---|
| `LullabyItemWidget` | Ninni kartı ve oynatma geçişi |
| `LullbyHorizontalList` | Ninni yatay listesi |
| `MeditationHorizontalList` | Meditasyon/sesli hikâye yatay listesi |
| `MeditationItemWidget` | Meditasyon veya sesli hikâye kartı |

Dosya adındaki `lullby` yazımı mevcut kaynakla aynıdır ve yeniden geliştirmede
standardize edilmelidir.

## 6. Profil ve Auth Widget'ları

| Bileşen | Rol |
|---|---|
| `LanguageSelectorTileWidget` | Dil seçimi |
| `NotificationSwitchWidget` | Bildirim izin durumu |
| `PromotionWidget` | Promosyon kodu girişi |
| `ProgressBar` | Profil kurulum ilerlemesi |

## 7. Temel Servis Bileşenleri

### Veri ve kimlik

- `FirebaseAuthService`
- `FirebaseService`
- `FirebseRealDatabaseService`
- `FirebaseRemoteConfigService`
- `SharedPreferenceService`
- `GoogleSignInHelper`

`FirebseRealDatabaseService` sınıf/dosya adındaki yazım mevcut kaynakla aynıdır.

### Premium ve ticari durum

- `PremiumCacheService`
- `PremiumStatusRecoveryService`
- `RevenueCatAttributeSyncQueue`
- `RevenueCatConfigGate`
- `RevenueCatUserIdentityGuard`
- `SetUserTypeInformationService`

### Ses

- `PlayerManager`
- `AudioCommandQueue`
- `AudioScreenExitController`
- `AudioServiceLifecycleInitializer`
- `AudioSessionManager`

### Dosya, görsel ve indirme güvenliği

- `AdvancedImageCache`
- `AppCacheManagers`
- `DownloadCompletionGuard`
- `ImageStreamListenerManager`
- `StoryDirectoryGuard`

### Başlangıç ve operasyonel korumalar

- `StartupDeferredTaskCoordinator`
- `StartupDiagnosticsRegistry`
- `FirebaseAuthReloadGuard`
- `RealtimeDatabaseOperationGuard`
- `AuthExceptionHandler`

Kullanıcının isteği doğrultusunda sürüm bazlı Crashlytics rapor/dokümanları kapsam
dışındadır; bu envanter onların içeriğini kullanmaz.

## 8. Repository ve Use Case Bileşenleri

Her ana feature genel olarak şu katmanları içerir:

```text
presentation/page + widgets + cubit
  → domain/usecases
  → domain/repository (interface)
  → data/repository (implementation)
  → data/data_sources/remote
  → data/models
```

Auth işlemlerinin bir bölümü doğrudan Firebase servislerine, içerik feature'ları ise
Retrofit repository'lerine dayanır. Dolayısıyla katman disiplini feature'lar arasında
tamamen aynı değildir.

## 9. Üretilen Bileşenler

- Retrofit `.g.dart` istemci implementasyonları build_runner tarafından üretilir.
- Slang `lib/gen/strings.g.dart` ve locale dosyaları yerelleştirme kodudur.
- Platform plugin registrant dosyaları Flutter araçları tarafından yönetilir.

Üretilen dosyalar elle düzenlenmemeli; kaynak annotation/çeviri dosyaları değiştirilip
codegen çalıştırılmalıdır.

## 10. Yeniden Kullanım ve Tasarım Sistemi Bulguları

- Ortak widget havuzu var, ancak feature widget'ları benzer kart/liste davranışlarını
  tekrar ediyor.
- Premium kilit kontrolü kart ve ekran seviyelerinde dağınık.
- Loading/error/empty state bileşenlerinin tek bir standart sözleşmesi yok.
- Görsel isimleri `icons`, `newIcons` ve feature içi kullanım arasında dağınık.
- Tema ve metin stilleri bulunmasına rağmen bileşen varyantları tipli bir tasarım sistemi
  kataloğunda değil.
- Erişilebilirlik, semantic label, minimum dokunma alanı ve text scaling davranışları
  sistematik bir sözleşmeyle belgelenmemiş.

## Kaynak Kanıtları

- `lib/common/components/`
- `lib/features/**/presentation/pages/`
- `lib/features/**/presentation/page/`
- `lib/features/**/presentation/widgets/`
- `lib/features/**/presentation/cubit/`
- `lib/common/services/`
