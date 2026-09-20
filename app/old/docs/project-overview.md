# TellPal - Proje Genel Bakışı

**Tarih:** 2026-07-28  
**Tür:** Flutter mobil uygulama  
**Mimari:** Feature tabanlı, kısmen katmanlı Clean Architecture

## Yönetici Özeti

TellPal; çocuklara etkileşimli hikâyeler, ninni, meditasyon ve sesli hikâyeler; ebeveynlere
ise rehber içerikleri sunan Android/iOS uygulamasıdır. Anonim kullanım, e-posta/Google/
Apple girişi, çoklu dil, premium abonelik, medya indirme ve çevrimdışı cache yetenekleri
bulunur.

Repository yalnız mobil istemciyi içerir. Hikâye ve rehber meta verisi dış REST API'den;
kimlik, kullanıcı, geçmiş ve dosyalar Firebase'den; premium erişim RevenueCat'ten gelir.
`v2/be` altında dosya bulunmadığı için bu dizin çalışan bir backend değildir.

Mevcut sistem yeniden geliştirilmeden önce korunması gereken davranışların en kritiği,
dallanan hikâye ağacı ile tek/iki ZIP medya indirme protokolüdür. İki ZIP modunda ikinci
paketin hazır oluşu, sayfa indeksi ve ses yaşam döngüsü birbiriyle yarışabildiğinden bu
alan karakterizasyon ve uçtan uca testlerle sabitlenmelidir.

## Proje Sınıflandırması

- **Repository tipi:** Tek parçalı monolith
- **Proje tipi:** Flutter mobil istemci
- **Birincil dil:** Dart
- **Platformlar:** Android, iOS
- **Mimari örüntü:** Feature-based presentation/domain/data katmanları
- **State:** BLoC/Cubit
- **DI:** GetIt
- **Navigasyon:** GoRouter + sınırlı Navigator/PersistentNavBarNavigator

## Teknoloji Yığını

| Alan | Teknoloji |
|---|---|
| Toolchain | Flutter 3.41.2, Dart 3.11.0 |
| UI | Flutter Material |
| State | flutter_bloc / bloc |
| DI | GetIt |
| Navigasyon | go_router |
| REST | Dio + Retrofit |
| Cloud | Firebase Auth, Realtime Database, Storage, Remote Config, Analytics |
| Premium | RevenueCat |
| Attribution | Adjust |
| Ses | audio_service + just_audio |
| Cache | SharedPreferences, Hive/Dio cache, cache manager, local files |
| Yerelleştirme | Slang + Flutter localizations |
| Test | flutter_test |

Uygulama sürümü `pubspec.yaml` içinde `1.0.35+333` olarak tanımlıdır.

## Ana Yetenekler

### Kimlik ve onboarding

- İlk çalıştırma ve onboarding kararı.
- Anonim devam.
- E-posta/parola, Google ve Apple ile giriş/kayıt.
- E-posta doğrulama ve parola sıfırlama.
- Çok adımlı kullanıcı profili kurulumu.

### Hikâyeler

- Kategori vitrini, arama, editör seçimi ve benzer içerik.
- Premium içerik kilidi.
- Dallanan StoryPoint karar ağacı.
- Sayfa görselleri, sesli anlatım ve arka plan müziği.
- Tek veya iki ZIP medya indirme.
- Okuma geçmişi, kalan sayfa, tamamlama ve puanlama.

### Rahatlama ve ebeveyn

- Ninni, meditasyon ve sesli hikâye.
- Ebeveyn rehberleri, kategori ve günlük ücretsiz içerik.
- Metin okuma ve sesli dinleme.

### Profil ve ticari akışlar

- Profil düzenleme, dil ve bildirim izni.
- Geri bildirim.
- Premium durum, paywall, offering ve promosyon kodu.
- Parola, çıkış ve hesap silme.

## Mimari Öne Çıkanlar

- `lib/main.dart` ve `lib/injection_container.dart` composition root'u oluşturur.
- 31 Cubit lazy provider üzerinden sunulur; çoğu GetIt singleton'ıdır.
- Stories ve parent_mode data/domain/presentation ayrımına sahiptir.
- Remote Config API base URL'den medya paketleme biçimine kadar kritik kararlar taşır.
- Altı farklı cache/kalıcılık katmanı bulunur.
- REST meta verisi ile Storage medya dosyaları ayrı yayın yollarıdır.
- Hikâye okuyucu state'i Cubit, widget, dosya sistemi ve audio servislerine dağılmıştır.

## Geliştirme Özeti

### Ön Koşullar

- Uyumlu Flutter/Dart SDK
- Android SDK/JDK
- iOS için macOS/Xcode/CocoaPods
- Yetkili Firebase, RevenueCat ve Adjust ortam yapılandırmaları

### Başlangıç

```powershell
flutter pub get
dart run build_runner build --delete-conflicting-outputs
flutter run
```

### Ana Komutlar

- **Bağımlılıklar:** `flutter pub get`
- **Çalıştırma:** `flutter run`
- **Codegen:** `dart run build_runner build --delete-conflicting-outputs`
- **Analiz:** `flutter analyze`
- **Test:** `flutter test`
- **Android:** `flutter build appbundle`
- **iOS:** `flutter build ipa`

## Repository Yapısı

- `lib/common`: ortak UI ve platform servisleri.
- `lib/config`: route, tema ve config.
- `lib/core`: ortak domain/network temelleri.
- `lib/features`: auth, stories, parent_mode, profile, relax.
- `lib/gen`: generated localization.
- `assets`: uygulama kabuğu görsel, ikon, font ve çevirileri.
- `android`, `ios`: platform projeleri.
- `test`: unit/widget regresyon testleri.

Ayrıntılı ağaç için [Source Tree Analysis](./source-tree-analysis.md) belgesine bakın.

## Mevcut Durumun Önemli Riskleri

1. Firebase platform servis dosyaları Git tarafından izleniyor.
2. İki ZIP hikâye akışı sabit sayfa sınırı ve dağıtık state'e bağlı.
3. 31 Cubit'in singleton yaşam döngüsü eski state taşıyabilir.
4. GoRouter ve Navigator birlikte kullanılıyor.
5. Kullanıcı/feedback şemalarının birden fazla nesli var.
6. Cache invalidation merkezî değil.
7. CI/CD bulunmuyor ve iOS/pubspec sürüm değerleri drift ediyor.
8. Kritik ürün yolculuklarında E2E ve contract testleri eksik.

## Yeniden Geliştirme Başlangıç Sırası

1. Mevcut davranış ve backend/Storage sözleşmelerini kabul testleriyle sabitle.
2. Auth/session/profile sınırını çıkar.
3. İçerik kataloglarını ID tabanlı route'larla kur.
4. Hikâye okuyucuyu manifest tabanlı tek session state machine olarak tasarla.
5. Audio, parent mode, profile ve premium akışlarını dilimler halinde taşı.
6. Eski/yeni uygulamayı kontrollü migration ve kademeli rollout ile paralel doğrula.

## Dokümantasyon Haritası

- [index.md](./index.md) — ana dokümantasyon indeksi
- [architecture.md](./architecture.md) — ayrıntılı mimari
- [screen-flow-map.md](./screen-flow-map.md) — ekranlar ve uçtan uca akışlar
- [backend-services.md](./backend-services.md) — dış servisler
- [api-contracts.md](./api-contracts.md) — REST sözleşmeleri
- [data-models.md](./data-models.md) — veri modelleri ve ilişkileri
- [state-management.md](./state-management.md) — Cubit/DI yaşam döngüsü
- [component-inventory.md](./component-inventory.md) — ekran/widget/servis envanteri
- [source-tree-analysis.md](./source-tree-analysis.md) — kaynak ağacı
- [development-guide.md](./development-guide.md) — geliştirme
- [deployment-guide.md](./deployment-guide.md) — build ve yayın
- [contribution-guide.md](./contribution-guide.md) — katkı, review ve güvenlik kuralları
- [test-strategy.md](./test-strategy.md) — test tabanı ve hedef strateji
- [emulator-validation.md](./emulator-validation.md) — Android emülatör runtime doğrulaması

---

_BMAD Method `document-project` workflow ile üretildi._
