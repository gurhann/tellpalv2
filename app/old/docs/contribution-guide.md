# TellPal Katkı Rehberi

**Tarih:** 2026-07-28  
**Kaynak:** Repository `AGENTS.md` ve mevcut proje yapılandırmaları

## 1. Değişiklik İlkeleri

- Değişikliği istenen kapsamla sınırlı ve küçük tutun.
- Mevcut feature/layer organizasyonuyla uyumlu ilerleyin.
- Kullanıcıya ait veya ilgisiz çalışma ağacı değişikliklerini koruyun.
- Generated dosyaları elle düzenlemeyin.
- Kapsam dışı geniş refactor veya opportunistic cleanup yapmayın.
- Harici servis ve veri sözleşmesi değişiyorsa migration/geri uyumluluğu belgeleyin.

## 2. Kod Organizasyonu

- `lib/common`: paylaşılan bileşen ve servisler.
- `lib/config`: tema, route ve config.
- `lib/core`: ortak altyapı/domain temelleri.
- `lib/features`: ürün feature'ları.
- `test`: `lib` yapısını mümkün olduğunca yansıtan testler.

Feature içinde mevcutsa şu yönü koruyun:

```text
presentation
  → domain/usecase + repository interface
  → data/repository + remote source + model
```

Doğrudan GetIt/service erişimi eklemek yerine mevcut constructor injection ve use case
sınırlarını tercih edin.

## 3. Dart Stil Kuralları

- İki boşluk girinti.
- Makul olduğunda satır uzunluğu 100 karakterin altında.
- Dosya adı: `snake_case.dart`.
- Sınıf ve enum: `PascalCase`.
- Üye ve değişken: `camelCase`.
- Cubit sınıfı `Cubit`, state sınıfı `State` ile biter.
- Feature widget'ları ilgili `presentation/widgets/` altında tutulur.
- `analysis_options.yaml` ve `flutter_lints` kurallarını izleyin.

Değişiklik öncesi/sonrası:

```powershell
dart format lib test
flutter analyze
```

## 4. Bağımlılık ve Codegen

Bağımlılık değişikliği:

```powershell
flutter pub get
```

Retrofit/Slang veya ilgili generated çıktı:

```powershell
dart run build_runner build --delete-conflicting-outputs
```

Generated drift oluşmadığını Git değişiklik listesinde doğrulayın. Yeni paket eklerken:

- Native Android/iOS etkisini.
- Minimum SDK gereksinimini.
- Privacy/permission gereksinimini.
- Paket boyutunu.
- Lisans ve bakım durumunu.
- Mevcut paketle işlev çakışmasını değerlendirin.

## 5. Test Beklentileri

Framework `flutter_test`tir. Dosyalar `*_test.dart` olarak adlandırılır ve mümkünse
`lib/` yolunu yansıtır.

```powershell
flutter test
```

Değişiklik türüne göre:

- Cubit/use case: unit test.
- Widget/ekran: loading, success, error ve user action widget testleri.
- Model/API: JSON fixture/contract test.
- Navigasyon: route input ve deep-link testi.
- Ses/dosya: temp file ve fake adapter ile yarış/iptal testi.
- Kritik yolculuk: entegrasyon veya E2E.

Bug fix, hatayı önce yeniden üreten regresyon testi içermelidir.

## 6. Kırılgan Hikâye Akışı Onay Kapısı

Şu alanlar geniş refactor veya fırsatçı temizlikte değiştirilmemelidir:

- `lib/features/stories/presentation/pages/story_content.dart`
- `lib/features/stories/presentation/cubit/remote/story_second_part_cubit.dart`
- `lib/common/services/firebase_service.dart` içindeki ZIP indirme/çıkarma

Şunlardan biri değişecekse önce açık kullanıcı/reviewer onayı gerekir:

- İkinci ZIP yükleme.
- Ertelenmiş sayfa çizimi.
- Loading overlay davranışı.
- Sayfa/paket sınırı.
- İlgili heuristic ve state reset davranışları.

Onay öncesi:

1. Değişiklik gerekçesini yazın.
2. Failure mode ve geri alma planını çıkarın.
3. Var olan davranışı karakterizasyon testiyle sabitleyin.

Onay sonrası:

1. Değişikliği minimal tutun.
2. Gerekiyorsa geçici diagnostics ekleyin.
3. İlk/ikinci ZIP sınırında hızlı sayfa kaydırmayı manuel ve otomatik doğrulayın.

## 7. Güvenlik

- Secret, token, signing parolası veya private key commit etmeyin.
- Android Firebase servis dosyasını VCS dışında tutun.
- iOS Firebase servis dosyasını yalnız güvenli build injection ile sağlayın.
- Adjust/RevenueCat/Firebase anahtarlarını ortam bazlı yönetin.
- Uygulama loglarında e-posta, UID, auth tokenı, Storage tokenı ve feedback içeriğini
  maskeleyin.
- Remote Config'i gizli değer deposu olarak kullanmayın.

Mevcut Firebase servis dosyalarının Git tarafından izlendiği tespit edilmiştir. Bu
durumda yalnız dosyayı silmek yeterli değildir; anahtar kısıtlama/rotation ve geçmiş
exposure değerlendirmesi gerekir.

## 8. Commit

- Emir kipinde, kısa ve anlamlı başlık kullanın: `Add`, `Fix`, `Update`.
- Birbiriyle ilgili değişiklikleri aynı commit'te gruplayın.
- Commit body'de gerekçe, önemli trade-off ve test sonucunu yazın.
- Varsa issue'yu `Closes #123` gibi referanslayın.
- Generated veya format değişikliklerini işlevsel değişiklikle anlaşılmaz biçimde
  karıştırmayın.

Örnek:

```text
Add story API contract tests

Cover missing category type and malformed story point responses.
Tests: flutter test test/features/stories
```

## 9. Pull Request

PR açıklaması şunları içermelidir:

- Problem ve kullanıcı etkisi.
- Çözüm kapsamı ve kapsam dışı alanlar.
- Mimari/veri/API etkisi.
- Test komutları ve sonuçları.
- UI değişikliğinde ekran görüntüsü/video.
- Bug fix için yeniden üretme adımları.
- Migration, rollout ve rollback notu.
- Kırılgan story flow'a dokunuluyorsa alınan açık onay.

## 10. Review Kontrol Listesi

- [ ] Değişiklik istenen kapsamda
- [ ] İlgisiz dosyalar değiştirilmemiş
- [ ] Analyzer temiz
- [ ] Testler başarılı
- [ ] Yeni davranış için test var
- [ ] Generated dosyalar güncel
- [ ] Route, localization ve analytics etkileri değerlendirildi
- [ ] Secret/PII eklenmedi
- [ ] Cache/migration/geri uyumluluk değerlendirildi
- [ ] Kırılgan story flow kuralı uygulandı

## İlişkili Belgeler

- [Development Guide](./development-guide.md)
- [Test Strategy](./test-strategy.md)
- [Architecture](./architecture.md)
- [Deployment Guide](./deployment-guide.md)
