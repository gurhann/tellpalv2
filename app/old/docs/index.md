# TellPal Dokümantasyon İndeksi

**Tür:** Tek parçalı Flutter mobil uygulama  
**Birincil dil:** Dart  
**Mimari:** Feature tabanlı, kısmen katmanlı Clean Architecture  
**Son güncelleme:** 2026-07-28

## Proje Özeti

TellPal; çocuk hikâyeleri, ninni/meditasyon/sesli içerik ve ebeveyn rehberleri sunan
Android/iOS uygulamasıdır. Mobil istemci dış REST API, Firebase, RevenueCat ve Adjust ile
çalışır. Bu repository çalışan bir backend içermez.

Bu dokümantasyon, mevcut uygulamanın yeniden geliştirilmesinden önce davranışları,
ekranları, servis sınırlarını, veri ilişkilerini ve riskleri uçtan uca görünür kılmak için
exhaustive kaynak taramasıyla hazırlanmıştır.

## Hızlı Referans

- **Teknoloji:** Flutter 3.41.2 / Dart 3.11.0
- **Uygulama sürümü:** `1.0.35+333`
- **Giriş noktası:** `lib/main.dart`
- **DI:** `lib/injection_container.dart`
- **Navigasyon:** `lib/config/routes/app_routes.dart`
- **State:** BLoC/Cubit + GetIt singleton'ları
- **Veri:** REST + Firebase Realtime Database/Storage
- **Premium:** RevenueCat
- **Platform:** Android 24+, iOS 15+
- **Deployment:** Manuel; CI/CD yapılandırması tespit edilmedi

## Önce Bunları Okuyun

Yeniden geliştirme kararları için önerilen sıra:

1. [Proje Genel Bakışı](./project-overview.md)
2. [Ekran ve Uçtan Uca Akış Haritası](./screen-flow-map.md)
3. [Teknik Mimari](./architecture.md)
4. [Backend ve Harici Servisler](./backend-services.md)
5. [Test Tabanı ve Stratejisi](./test-strategy.md)

## Üretilen Dokümantasyon

### Temel Belgeler

- [Proje Genel Bakışı](./project-overview.md) — ürün kapsamı, teknoloji ve yeniden
  geliştirme başlangıç sırası
- [Teknik Mimari](./architecture.md) — composition root, feature/katman sınırları,
  entegrasyonlar, cache, hikâye okuyucu ve hedef ilkeler
- [Kaynak Ağacı Analizi](./source-tree-analysis.md) — dizinler, giriş noktaları ve kritik
  dosyalar
- [Bileşen Envanteri](./component-inventory.md) — ekran/widget/Cubit/servis kataloğu

### Ürün ve Kullanıcı Akışları

- [Ekran ve Uçtan Uca Akış Haritası](./screen-flow-map.md) — 23 GoRouter rotası,
  Navigator ekranları, auth, hikâye, parent, relax ve profil yolculukları
- [Durum Yönetimi](./state-management.md) — 31 Cubit, GetIt ve yaşam döngüsü
- [Asset ve Yerelleştirme Envanteri](./asset-inventory.md) — 184 asset, dil ve uzak
  medya ilişkileri

### Veri ve Servis Sözleşmeleri

- [Backend ve Harici Servisler](./backend-services.md) — REST, Firebase, Remote Config,
  RevenueCat, Adjust ve audio sınırları
- [REST API Sözleşmeleri](./api-contracts.md) — hikâye, kategori ve ebeveyn endpoint'leri
- [Veri Modelleri ve İlişkileri](./data-models.md) — domain, Firebase, SharedPreferences
  ve dosya cache şemaları

### Geliştirme, Kalite ve Yayın

- [Geliştirme Rehberi](./development-guide.md) — kurulum, codegen, analiz, test ve
  kırılgan akış kuralları
- [Test Tabanı ve Yeniden Geliştirme Stratejisi](./test-strategy.md) — mevcut 27 test
  dosyası, boşluklar ve kritik karakterizasyon matrisi
- [Android Emülatör Doğrulama Raporu](./emulator-validation.md) — gerçek runtime ekranları,
  navigasyon, RevenueCat ve performans gözlemleri
- [Build ve Dağıtım Rehberi](./deployment-guide.md) — Android/iOS build, signing,
  servis kontrolü, CI/CD hedefi ve rollout
- [Katkı Rehberi](./contribution-guide.md) — kod stili, test, güvenlik, commit/PR ve
  kırılgan hikâye akışı onay kuralları

### Workflow Durumu

- [Project Scan Report](./project-scan-report.json) — BMAD tarama adımları,
  resumability ve çıktı listesi

## Kritik Uyarılar

### Hikâye iki ZIP akışı

Şu alanlar yüksek risklidir:

- `lib/features/stories/presentation/pages/story_content.dart`
- `lib/features/stories/presentation/cubit/remote/story_second_part_cubit.dart`
- `lib/common/services/firebase_service.dart`

İkinci ZIP, ertelenmiş sayfa, loading overlay veya sayfa sınırı değişmeden önce gerekçe
ve hata senaryoları belgelenmeli, açık kullanıcı/reviewer onayı alınmalıdır.

### Secret ve Firebase dosyaları

Android ve iOS Firebase servis dosyaları Git tarafından izlenmektedir. Gerçek değerleri
dokümanlara taşımayın. Rotation, geçmiş exposure değerlendirmesi ve CI secret/file
injection planı gerekir.

### Backend yok

`v2/be` dosyasız dizin iskeletidir. API davranışları bu repository'de implement
edilmemiştir; [REST API Sözleşmeleri](./api-contracts.md) mobil istemcinin mevcut
beklentisini gösterir.

## Mevcut Repository Belgeleri

- [README](../README.md) — temel proje notları
- [Kök Mimari Belgesi](../architecture.md) — daha önce hazırlanmış mimari notlar
- [RevenueCat Integration](../REVENUCAT_INTEGRATION.md) — premium entegrasyon notları
- [AGENTS](../AGENTS.md) — repository geliştirme ve güvenlik kuralları
- [CLAUDE](../CLAUDE.md) — proje çalışma notları
- [Lazy Cubit README](../lib/common/components/cubit/README.md) — Cubit lazy yükleme
  notları

Bu mevcut belgeler kaynak kodla karşılaştırılmış; yeni `docs/` çıktıları güncel kaynak
taraması temel alınarak hazırlanmıştır.

## Kullanıcı Tarafından Hariç Tutulan Alan

Şu klasörlerin yalnız varlığı kaydedilmiş, içerikleri incelenmemiştir:

- `docs/1.0.32_crashlytcs/`
- `docs/1.0.33_crashlytcs/`
- `docs/1.0.34_crashlytcs/`

## Başlangıç Komutları

### Ön Koşullar

Uyumlu Flutter/Dart, Android SDK/JDK; iOS için macOS/Xcode/CocoaPods ve yetkili servis
yapılandırmaları.

### Kurulum ve Çalıştırma

```powershell
flutter pub get
dart run build_runner build --delete-conflicting-outputs
flutter run
```

### Analiz ve Test

```powershell
dart format --output=none --set-exit-if-changed lib test
flutter analyze
flutter test
```

## AI Destekli Geliştirme İçin

### UI veya akış değişikliği

`screen-flow-map.md`, `component-inventory.md`, `state-management.md`

### API/backend entegrasyonu

`architecture.md`, `backend-services.md`, `api-contracts.md`, `data-models.md`

### Hikâye okuyucu

`architecture.md` bölüm 8, `screen-flow-map.md` bölüm 5,
`test-strategy.md` bölüm 5 ve repository'deki kırılgan akış kuralı

### Build/yayın

`development-guide.md`, `deployment-guide.md`

### Kod değişikliği ve review

`contribution-guide.md`, ilgili feature mimarisi ve `test-strategy.md`

### Yeni uygulamayı planlama

`project-overview.md`, tüm mevcut davranış belgeleri ve `test-strategy.md` ile başlayın.
Bir sonraki BMAD çıktısı ürün gereksinimleri/PRD ve hedef mimari olmalıdır; bu belge seti
mevcut durumun kaynak doğruluğuna dayalı başlangıç bağlamıdır.

---

_Dokümantasyon BMAD Method `document-project` workflow ile üretildi._
