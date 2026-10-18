# P4a4 bağımsız inceleme kaydı

Tarih: 2026-10-18. Görev: [ortak detail/reader QA teslimatı](spec-p4a4-qa-consumer-delivery.md).
Baseline: `339aee45a99e4fcda27132a2d9e62946267d142e`.

Üç inceleyici aynı oturum modelini kullandı. Önceki konuşmayı almadan, ayrı ve eşzamanlı çalıştı.
Görev farkı tracked/untracked 26 dosyayı kapsadı. Önceden kirli dosyalar hash denetimiyle ayrıldı;
tam baseline farkı ve kapsam makbuzu MAC_EK özel kökünde saklandı. İnceleme sırasında index değişmedi.

## Blind Hunter sonucu

“Eyleme dönüştürülebilir bulgu yok.”

## Edge Case Hunter sonucu

`[]` — eyleme dönüştürülebilir bulgu yok.

## Verification Gap sonucu ve sınıflandırma

İki bulgu bağımsız değerlendirildi. İkisi de mevcut onaylı lifecycle/kapanış beklentisinin
widget bağlantısını sınamak için mekanik test ekler; niyet veya spec değişikliği gerektirmez.
Önem derecesi `medium`, kategori `patch` seçildi. `intent_gap`, `bad_spec` veya `defer` yoktur.

1. **Gerçek Flutter lifecycle bağlantısı sınanmıyordu.** Cubit’e doğrudan `visibility` çağıran
   testler parent observer’ın kaydını veya olay aktarımını kanıtlamıyordu. Observer kaldırılsa
   eski testler geçebilirdi. `reading_image_consumer_qa_test.dart` içine gerçek QA router’ında
   binding paused/resumed olayı gönderen test eklendi. Raster dispose/removal, HTTP duruşu,
   resume sonrası açık readmission gereği birlikte doğrulandı.
2. **UI kapanışı bekleyen native işle birlikte sınanmıyordu.** Router testi işler bittikten sonra,
   drain testleri ise doğrudan Cubit üzerinde kapanıyordu. Parent `_drain` içindeki `await`
   kaldırıldığında bu ilişki kaçabilirdi. Gerçek parent UI düğmesi, kontrollü native gate/image
   işiyle sınandı. Release öncesi Cubit kapalı değil, kapanış metni yok ve PopScope kapalı;
   release sonrası drain tamamlanmış ve native sahiplik sıfırdır.

Uygulama ajanı iki patch’i tek görevde aldı. Repository ürün kaynakları değişmedi; yalnız test dosyası
genişledi. İki yeni test önce dar koşuda geçti. Sonra özel kopyada kapanış `await`’i ve observer kaydı
ayrı ayrı kaldırıldı. İlgili testler beklenen assertion ile başarısız oldu. Kaynak her mutasyondan
sonra aynı hash’e geri getirildi; repository bu mutasyonlara hiç katılmadı.

## Son doğrulama

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4a4-consumers-20261018-fb8ed1cb`.

- `mobile-regression-05.log`: **228/228 geçti**, exit 0.
- `mobile-analyze-04.log`: 13 dosyada sorun yok, exit 0; `git diff --check` temiz.
- `mobile-review-patch-tests-03.log`: iki yeni parent testi geçti.
- `mobile-review-mutation-drain-await.log`, `mobile-review-mutation-lifecycle-observer.log`:
  beklenen assertion nedeniyle exit 1. `mobile-review-mutations.private.json` geri dönüş hash’ini tutar.
- `mobile-source-identity-review-patch-02.private.json`: kaynak SHA
  `5bfac569ac3de1264f67cd47701f3170a1108e54fa403b708027cdc740962ef6`; repo/kopya farkı yok.
- `mobile-protection-audit-review-patch-02.private.json`: 3206 korunan dosya ve onaylı blok/context değişmedi.
- Backend ilk tam koşu `backend-test-02.log` ve inceleme sonrası `backend-test-03.log`: gerçek
  PostgreSQL/Flyway/Tomcat ve modül sınırlarında **30/30 geçti**, hata/atlama yok.

Gerçek #94, Firebase/storage, fiziksel cihaz veya yeni üretim yapılmadı. Genel tüketici geçişi,
P4b, fiziksel kalite/peak kaynak ve üretim profili kapıları açık kalır.
