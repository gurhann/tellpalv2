---
title: 'P5f: iPhone hız/bellek ve yerel sunucu uyumluluğu'
type: 'feature'
created: '2026-10-22'
status: 'done'
baseline_commit: '4e0a42e74ef4e2b117174b4d846ebe47d1a542fc'
review_loop_iteration: 0
context:
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Amaç

P5e release QA paketinde 1950/1952/8303/8306 görsellerinin orijinal/PNG/WebP
hızını ve belleğini karşılaştır. Yerel Java kontrolüyle format kararını değerlendir.

## Sınırlar

**Her zaman:** Orijinalleri/P5c'yi salt oku; eski kanıtları ve 88 çalışma dosyasını
hash ile koru. Freezed Cubit/auto_route/injectable, aynı decoder/geometri. QA
verisini iki kopyayla doğrulayıp aynı paketi güncelle. Silme; eski binary geri
yüklemesi garanti değildir.

**Önce sor:** Yeni SDK/bağımlılık/signing, başka cihaz/paket veya dosya temizliği.

**Asla:** Canlı/backend ağı, Firebase, DB, normal uygulama, yayın/deploy, kaynak
dönüştürme veya sunucu kabul kurallarını değiştirme. Üretim hazırlığı çıkarma.

## Ölçüm sözleşmesi

- Ön doğrulama, 12 ısınma; altı format sırası ikişer kez, dönüşümlü görsel sırası.
  Her dosyaya 12 örnek: toplam 144 ölçülen deneme.
- Hash/ICC önceden; ölçümde readback/screenshot/yazma yok. Okuma, decode, ilk
  framework çizimi ve eşleşmiş frame raster süreleri ayrı; panel sunumu değildir.
  Frame `epoch/index/frameNumber` ile eşleşir; farklı saat alanları karıştırılmaz.
- Native `phys_footprint`: başlangıç/yüklü/bırakma sonrası, 10 ms örneklenmiş
  maksimum ve örnek boşlukları. GPU/gerçek tepe değildir; tek raster sahipliği.
- Sıcak asset; medyan/min/max ve tur farkları. İki Pofuduk örneğinde WebP'nin
  orijinale göre medyan decode/çizim artışı ≤%20, medyan örneklenmiş süreç
  maksimumu artışı ≤`max(8 MiB, %10)`: yalnız yerel pilot, üretim bütçesi değil.

| Durum | Davranış |
|---|---|
| SHA/ICC/frame/sayaç hatası | Geçersiz; eksik değer sıfır değildir |
| Ağ/arka plan/düşük güç/nominal dışı termal | Durdur; sessiz örnek eleme yok |
| >10 sn/deneme, >10 dk/oturum, >512 MiB | İptal/temizlik; başarı yok |
| USB/kilit/yedek/cache/imza eksik | Kurulum yapmadan dur; açık neden |

</frozen-after-approval>

## Kod haritası

- `app/mobile/lib/features/reading_images/p5e_offline/presentation/p5e_cubit.dart:177`:
  24 görünüm sözleşmesi korunur.
- `app/mobile/lib/features/reading_images/p5d_offline/data/p5d_manifest_repository.dart:50`:
  pin/ICC; `:81` hash işlemini zamanlı okumadan ayır.
- `app/mobile/lib/features/reading_images/p5d_offline/presentation/p5d_frame_timing.dart:71`:
  frame eşleştirmesi; aynı feature'ın `presentation/widgets/reading_image_comparison_raster.dart`
  decoder/sahiplik kaynağıdır.
- `app/mobile/ios/ImageExperiment/ImageExperimentLedger.swift:98`: native sayaç;
  yalnız dar bellek okumasını kullan. Tüm ledger taşınmaz.
- `tools/image-optimization/p5e_iphone_offline.py:239`: doğrulanmış veri yedeği,
  offline build ve aynı cihaz/paket koruması.
- `be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingImageInspector.java:18`:
  gerçek kontrol; child/process kullanılmaz. iPhone ön kontrolü bağlantı zaman aşımıyla bitti.

## İşler ve kabul

- [x] `app/mobile/lib/features/reading_images/p5f_performance/`: ayrı repository,
  dengeli plan, Cubit/state/ekran. Mevcut `app/p5e_offline/` kompozisyonuna bağla.
- [x] `tools/image-optimization/p5e_ios_host/P5fPerformance.swift`, `AppDelegate.swift`,
  `project.rb`: sınırlı native sampler ve ayrı oturum/mühür.
- [x] `tools/image-optimization/p5f_iphone_performance.py`, `p5f_performance_validation.py`:
  build/kurulum/toplama, ayrı schema, koruma hash'leri; P5e kaydını yeniden yorumlama.
- [x] `be/src/test/java/com/tellpal/v2/asset/reading/P5fLocalCompatibilityProbe.java`:
  taze inspector ile 123 dosya; SHA/byte/ret, yerel test profili, JVM/class kimliği.
  Spring/DB/storage yok.
- [x] `tools/image-optimization/test_p5f_iphone_performance.py`,
  `app/mobile/test/reading_images/p5f_performance_test.dart`: sıra, zaman sınırı,
  iptal, frame/sayaç, eksik/tekrar/mühür ve bozuk yedek retleri.
- [x] `be/docs/evidence/p5f-device-compatibility/verification.md`: kısa sonuç ve
  sayısal özet; yapılmayan adım `NOT_RUN`.

**Kabul:**
- Given USB/açık kilit/yedek, when QA güncellenir, then normal uygulama/kaynaklar korunur.
- Given çevrimdışı oturum, when deney biter, then 144 örnek/temizlik mühürlüdür; kesinti reddedilir.
- Given yerel probe, when çalışır, then retler kaynak/JVM kimliklidir; provider üretim kabulü değildir.

## Plan değişiklikleri

## Doğrulama

Fiziksel iPhone oturumu `ff2ed531f5d3 / collection-e6b43012`:
`SEALED_VERIFIED`, 12 ısınma + 144 ölçüm, 159 olay, temizlik ve mühür tam.
Son koruma geçti; 41 orijinal ve 88 eski çalışma dosyası da değişmedi.
Pofuduk'un iki WebP örneği yerel süre/bellek sınırlarını geçti. Yerel gerçek
inspector 0/123 kabul verdi; canlı geçiş `HOLD`, üretim imajı `NOT_RUN`.

24 P5f + 15 P5e Python, 27 Flutter, 1 opt-in başlangıç ve 4 Java testi geçti;
3 gerçek Java CLI olumsuz senaryosu reddedildi. Dart analyze, Swift typecheck
ve ağ engelli Release build geçti. Tam 156 tur testi, iptal token'ının sonraki
tura taşınma hatasını yakaladı; düzeltme ölçülen son pakette doğrulandı.
Matris satırları SHA/ICC/frame/sayaç, ağ/lifecycle/termal/düşük güç,
süre/bellek ve kurulum/yedek ret testleriyle kapsandı; skip yok.

[Kısa sonuç ve kanıtlar](../../be/docs/evidence/p5f-device-compatibility/verification.md).

Hedefli Python unittest, Flutter test/analyze; annotation değişirse offline generator.
Mevcut cache ile ağ engelli Release build. Yerel JVM probe'u üretim imajı
kanıtı değildir; denenmezse `NOT_RUN`. Önce/sonra koruma hash'leri eşleşir.
Özel medya/yedek/ham günlükler Git dışındadır.

## İnceleme

Üç bağımsız inceleme tamamlandı. Yedi bulgu guard/test yaması olarak giderildi;
intent veya frozen blok değişmedi. Geç seal yanıtı, tekrar kurulum sınırı, alt süre
tutarlılığı ve zorunlu kanıt dizinleri korundu. Pilot eşikleri, gerçek native
sampler kesintisi ve gerçek ekran paint sınırı test edildi.

Aynı fiziksel kayıt yeni doğrulayıcıyla `SEALED_VERIFIED` geçti. Ölçülen artifact,
snapshot, ham günlük ve ilk özet değişmedi. İnceleme yamaları telefona kurulmadı;
yeni fiziksel ölçüm yapılmadı. [İnceleme kaydı](../../be/docs/evidence/p5f-device-compatibility/review.md).

## Önerilen inceleme sırası

**Ölçüm akışı**

- Dengeli denemeleri tek raster sahibiyle yürütür; başarısızlığı başarıya çeviremez.
  [p5f_cubit.dart:95](../../app/mobile/lib/features/reading_images/p5f_performance/presentation/p5f_cubit.dart#L95)

- Hash ve ICC kontrolünü süreli dosya okumasından ayırır.
  [p5f_manifest_repository.dart:9](../../app/mobile/lib/features/reading_images/p5f_performance/data/p5f_manifest_repository.dart#L9)

- Gerçek çizimden epoch, index ve engine frame kanıtı üretir.
  [p5f_screen.dart:11](../../app/mobile/lib/features/reading_images/p5f_performance/presentation/p5f_screen.dart#L11)

**Native ve host sınırları**

- Örneklenmiş süreç belleğini koşul kontrolleri ve tek mühürle kaydeder.
  [P5fPerformance.swift:7](../../tools/image-optimization/p5e_ios_host/P5fPerformance.swift#L7)

- Aynı nonce için tekrarlı veya belirsiz kurulumu engeller.
  [p5f_iphone_performance.py:254](../../tools/image-optimization/p5f_iphone_performance.py#L254)

- Eksik, tutarsız veya mühürsüz veriyi reddeder; pilot bütçesini hesaplar.
  [p5f_performance_validation.py:31](../../tools/image-optimization/p5f_performance_validation.py#L31)

**Sunucu ve sonuç**

- Gerçek inspector ile pinli dosyaları dönüştürmeden sınar.
  [P5fLocalCompatibilityProbe.java:22](../../be/src/test/java/com/tellpal/v2/asset/reading/P5fLocalCompatibilityProbe.java#L22)

- Telefon sonucu, yerel format kararı ve canlı geçiş sınırını birlikte açıklar.
  [verification.md:1](../../be/docs/evidence/p5f-device-compatibility/verification.md#L1)

**Destekleyici testler**

- Gerçek 156 turu, geç yanıtları ve ekranın painter bağlantısını sınar.
  [p5f_performance_test.dart:255](../../app/mobile/test/reading_images/p5f_performance_test.dart#L255)

- Bütçe eşiklerini, koruma dizinlerini ve kurulum retlerini sınar.
  [test_p5f_iphone_performance.py:48](../../tools/image-optimization/test_p5f_iphone_performance.py#L48)

- Üretim sampler kaynaklarını host dublörleriyle gerçek koşul kesintisinde çalıştırır.
  [main.swift:1](../../tools/image-optimization/p5f_native_test/main.swift#L1)
