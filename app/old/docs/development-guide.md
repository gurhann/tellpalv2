# TellPal Geliştirme Rehberi

**Tarih:** 2026-07-28

## 1. Ön Koşullar

- Flutter SDK; tarama ortamında `3.41.2`.
- Dart SDK; tarama ortamında `3.11.0`.
- Android Studio/Android SDK ve JDK.
- iOS geliştirme için macOS, Xcode, CocoaPods ve geçerli signing hesabı.
- Firebase projesine yetkili, yerel servis yapılandırmaları.
- RevenueCat ve Adjust için ortam bazlı geçerli yapılandırma.

`pubspec.yaml` Dart aralığı `>=3.0.6 <4.0.0` olarak tanımlıdır. Yine de lockfile ve güncel
plugin'lerle uyum için ekip tarafından sabitlenmiş Flutter sürümü kullanılmalıdır.

## 2. İlk Kurulum

PowerShell:

```powershell
flutter --version
flutter pub get
dart run build_runner build --delete-conflicting-outputs
flutter doctor -v
```

Codegen yalnızca kaynak annotation veya yerelleştirme çıktıları eksik/güncel değilse
gereklidir. Repository'deki generated dosyaların mevcut olup olmadığını değişiklik
listesinde kontrol edin.

## 3. Ortam ve Servis Yapılandırması

Uygulama çalışmadan önce şu bağımlılıklar geçerli olmalıdır:

- Android Firebase yapılandırması yerel `android/app/` altında.
- iOS Firebase yapılandırması yerel `ios/Runner/` altında.
- Remote Config default/production değerleri, özellikle API base URL.
- RevenueCat platform anahtarları ve entitlement/offering tanımları.
- Adjust app/token/event eşleşmeleri.
- Firebase Auth sağlayıcıları: e-posta, Google ve Apple.
- Realtime Database ve Storage security rules.

Gerçek değerleri dokümana, log'a veya yeni source dosyalarına kopyalamayın. Mevcut
repository'de Android ve iOS Firebase servis dosyalarının Git tarafından izlendiği
tespit edilmiştir; bu durum repository güvenlik kuralıyla çelişir ve anahtar döndürme +
geçmiş temizleme planıyla ayrıca ele alınmalıdır.

## 4. Uygulamayı Çalıştırma

```powershell
flutter devices
flutter run
```

Belirli cihaz:

```powershell
flutter run -d <device-id>
```

Uygulama açılışında Remote Config, Firebase, RevenueCat ve içerik API'sine erişir.
Çevrimdışı çalışma testi yapılacaksa önce en az bir başarılı içerik/cache oturumu ile
başlangıç koşulu ayrıca belirtilmelidir.

## 5. Kod Üretimi

Retrofit ve Slang yapılandırmasına bağlı generated dosyalar için:

```powershell
dart run build_runner build --delete-conflicting-outputs
```

Sürekli geliştirme sırasında:

```powershell
dart run build_runner watch --delete-conflicting-outputs
```

Generated dosyalarda doğrudan değişiklik yapmayın. Model/API annotation'ını veya
yerelleştirme kaynağını değiştirin.

## 6. Statik Analiz ve Format

```powershell
dart format --output=none --set-exit-if-changed lib test
flutter analyze
```

Proje `flutter_lints` kullanır. `*.g.dart`, route generated ve plugin registrant gibi
generated dosyalar analyzer kapsamı dışında tutulur.

Kaynak kuralları:

- İki boşluk girinti.
- Dosya adları `snake_case.dart`.
- Sınıf/enum `PascalCase`, üye/değişken `camelCase`.
- Cubit sınıfları `Cubit`, state sınıfları `State` ile biter.
- Değişiklikleri mevcut feature katmanlarıyla uyumlu ve küçük tutun.

## 7. Testler

Tüm testler:

```powershell
flutter test
```

Tek dosya:

```powershell
flutter test test/<path>/<name>_test.dart
```

Coverage:

```powershell
flutter test --coverage
```

Mevcut testler dosya/ZIP güvenliği, ses ve premium guard alanlarında yoğunlaşır. Yeni
geliştirmede öncelikli eksikler:

- Auth ve kullanıcı profilinin Firebase emulator ile entegrasyon testleri.
- Retrofit repository sözleşme testleri.
- GoRouter ve deep-link widget testleri.
- Hikâyede ilk/ikinci ZIP sınırında hızlı kaydırma.
- Ses tamamlanması + kullanıcı kaydırması yarışları.
- Parent mode, relax ve profile kritik yolculukları.
- Premium kilit/paywall'ın uçtan uca davranışı.

## 8. Mimari Değişiklik Kontrol Listesi

Yeni bir endpoint/model:

1. Retrofit service sözleşmesini güncelle.
2. JSON modelini ve domain entity dönüşümünü güncelle.
3. Repository interface/implementation ve use case'i güncelle.
4. Cubit state'lerini güncelle.
5. Generated kodu yenile.
6. Fixture/model sözleşme testleri ekle.

Yeni bir ekran:

1. Rota ve tipli girdi sözleşmesini tanımla.
2. Feature-scoped state sahipliğini belirle.
3. Loading/error/empty davranışını ekle.
4. Localization ve analytics olaylarını ekle.
5. Premium erişim gerekiyorsa merkezi policy kullan.
6. Widget/navigasyon testi ekle.

## 9. Kırılgan Hikâye Akışı Kuralı

Şu alanlar yüksek risktir:

- `lib/features/stories/presentation/pages/story_content.dart`
- `lib/features/stories/presentation/cubit/remote/story_second_part_cubit.dart`
- `lib/common/services/firebase_service.dart` içindeki ZIP indirme/çıkarma

İkinci ZIP yükleme, ertelenmiş sayfa çizimi, yükleme overlay'i, sayfa sınırı veya ilgili
heuristic değişikliği öncesinde:

1. Değişiklik nedenini belgeleyin.
2. Hata senaryolarını çıkarın.
3. Kullanıcı/reviewer'dan açık onay alın.
4. Değişikliği küçük tutun ve geçici diagnostik ekleyin.
5. İlk/ikinci ZIP sınırında hızlı sayfa kaydırmayı fiziksel cihazda doğrulayın.

## 10. Debug ve Gözlemlenebilirlik

- GoRouter diagnostik logları açıktır.
- Ağ katmanı Dio hatalarını repository `DataFailed` sonucuna dönüştürür.
- Startup diagnostics ve deferred task coordinator başlangıç sorunlarını ayırmaya
  yardımcı olur.
- Audio command queue ve session manager ses yarışlarını incelemek için temel noktalardır.
- Hassas verileri loglamayın; auth tokenı, Storage URL tokenı, e-posta ve kullanıcı
  içeriğini maskeleyin.

Kullanıcı talebi gereği sürüm bazlı Crashlytics rapor/doküman dosyaları bu rehberin
kaynağı olarak kullanılmamıştır.

## 11. Cache Temizliği

Standart Flutter build temizliği:

```powershell
flutter clean
flutter pub get
```

Hikâye documents cache'i, SharedPreferences, Firebase offline cache ve Dio/Hive cache
ayrı katmanlardır. Test sırasında hangisinin temizlendiğini açıkça kaydedin; genel build
clean komutu uygulama verisini cihazdan temizlemez.

## 12. Yayın Öncesi Yerel Kontrol

```powershell
dart format --output=none --set-exit-if-changed lib test
flutter analyze
flutter test
flutter build apk
```

iOS build doğrulaması macOS üzerinde:

```bash
flutter build ipa
```

Yayın süreci ayrıntıları [Deployment Guide](./deployment-guide.md) belgesindedir.
