---
title: 'P5e: iPhone’da çevrimdışı görsel karşılaştırması'
type: 'feature'
created: '2026-10-22'
status: 'done'
baseline_commit: 'c4d35a98503f3c1c6d5cd4a2edb356c3ab940992'
review_loop_iteration: 0
context:
  - '/Users/gurhankucuk/Documents/GitHub/tellpalv2/.codex/skills/flutter-scalable-app/SKILL.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** P5d emülatör sonucu, gerçek iPhone ekranında renk ve yazı kalitesini kanıtlamaz.

**Approach:** iPhone 12’de Kocaman Bir Sarılma ve Pofuduk Mısır Pops’un kapak/ilk sayfalarını karşılaştır: 1950, 1952, 8303, 8306; original/PNG/WebP. Kullanıcı 12 görünüm ve aynı 1:1 yazı kesitleri arasında geçer. P5c dosyaları yeniden dönüştürülmez.

## Boundaries & Constraints

**Always:** Kaynakları, P5c/P5d’yi ve mevcut çalışma ağacını koru. Ayrı release QA başlangıcı, Freezed Cubit, auto_route ve injectable/get_it kullan. Aynı raster/viewport/filter/DPR; seri decode ve tek raster sahipliği. Kurulum mevcut `com.tellpal.tellpalMobilePoc.imageexperiment.release` deneme uygulamasını günceller; önce veri alanını özel dizine çıkarıp doğrula. Eski çalıştırılabilir dosyanın geri yüklenmesi garanti değildir; bu durum onay özetinde görünür. Uygulamayı silme.

**Ask First:** Yeni SDK/bağımlılık/signing kurulumu, başka cihaz/paket, dosya temizliği veya kapsam genişlemesi.

**Never:** Normal TellPal uygulaması, backend, Firebase, auth, storage, canlı istek, yayın veya deploy. Ekran görüntüsünden fiziksel P3 doğruluğu; bu örneklerden tüm hikâyeler veya üretim profili için kabul çıkarma.

## I/O & Edge-Case Matrix

| Durum | Davranış |
|---|---|
| Manifest/SHA/ICC/boyut yanlış | Gösterimden önce ret; fallback yok |
| İlk ağ durumu bilinmiyor veya erişilebilir | Karşılaştırma kapalı; kullanıcıya çevrimdışı adımı |
| Ağ geri gelir | Rasterı bırak; oturumu çevrimdışı kabul etme |
| Hızlı seçim/arka plan/decode hatası | Eski sonucu boyama; kaynakları bırak; açık hata |
| Signing/cache/alan/yedekleme eksik | İndirme, silme veya kurulum yapmadan dur |

</frozen-after-approval>

## Code Map

- `app/mobile/lib/features/reading_images/p5d_offline/domain/p5d_pins.dart:2`, `data/p5d_manifest_repository.dart:30`, `data/p5d_icc.dart`: sabit 12 dosya doğrulaması; salt okunur yeniden kullan.
- `app/mobile/lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart:32`, `app/mobile/lib/core/presentation/widgets/reading_image_viewport.dart:26`: decoder/sahiplik ve kapak geometrisi.
- `app/mobile/lib/features/reading_images/p5d_offline/domain/p5d_text_crops.dart:4`: aynı yazı alanları. `presentation/p5d_cubit.dart:46` sabit ölçüm sırası nedeniyle kullanılmaz.
- `tools/image-optimization/p5d_offline_app.py:103`: sınırlı kaynak snapshot örneği. `local_lossless_mobile.py:414`: manuel signing örneği; eski driver ve kayıp preflight kullanılmaz.
- P5c yaşayan kök: `/Users/gurhankucuk/.codex/visualizations/2026/10/21/01a15d22-a8a6-7b62-a0b1-4838e13bbee2/p5c-lossless-r2-c48dfb97`.
- Salt okunur ön kontrol: fiziksel iPhone/USB/kilit açık; CoreDevice ağı seçiyor. Mevcut exact QA profili cihazı kapsıyor; wildcard yok. Eski release QA kurulu, arşivlenmiş binary doğrulanamadı. Mac ana diskinde yaklaşık 116 MiB, MAC_EK’te 304 GiB boş alan var.

## Tasks & Acceptance

**Execution:**
- [x] `app/mobile/lib/features/reading_images/p5e_offline/`: Cubit/Freezed ekran; dört görsel, üç format ve tam/1:1 seçimleri; Türkçe yükleme/hata durumları. Kontroller üç formatta aynı görsel alanını korur.
- [x] `app/mobile/lib/app/p5e_offline_qa.dart`, `app/p5e_offline/`, `p5e_offline_bindings/`: normal injector taramasından bağımsız DI/router; yalnız iOS release ve özel compile flag ile açılır. Native ortam doğrulaması ve lifecycle temizliği.
- [x] `tools/image-optimization/p5e_ios_host/`: minimal plugin içermeyen host; mevcut manuel profile signing. `NWPathMonitor` ilk callback ve değişimlerini MethodChannel ile verir; yalnız `.unsatisfied` kabul edilir. Bağlantı denemesi yapılmaz. Yerel oturum kaydı seçilen dosya, geometri, decoder colorSpace ve ağ durumunu içerir.
- [x] `tools/image-optimization/p5e_iphone_offline.py`: benzersiz özel snapshot; hash koruması; mevcut cache ile offline build; paket/entitlement/plugin denetimi, QA veri yedeği ve yalnız hedef cihazda install. Build/tmp/cache MAC_EK’e yönlendirilir; ölçülen alan yetmezse durulur. Sonuçlar sonradan container’dan alınır. Ağ kapalıyken kontrol bağlantısı yoksa telefon elle kullanılır; screenshot zorunlu değildir.
- [x] `tools/image-optimization/test_p5e_iphone_offline.py`, `app/mobile/test/reading_images/p5e_offline_test.dart`: matristeki retler, seçim yarışları ve gerçek lifecycle temizliği; mevcut P5d korumaları.
- [x] `be/docs/evidence/p5e-iphone-offline/verification.md`: kısa Türkçe sonuç; özel görseller/yedekler Git dışında. Yapılmayan adımlar açıkça `NOT_RUN`.

**Acceptance Criteria:**
- Given doğrulanmış paket, when kurulur, then normal uygulama ve kaynaklar değişmez; eski QA veri yedeği doğrulanmıştır.
- Given uçak modu ve Wi-Fi kapalı, when native ağ durumu doğrulanır, then 12 görünüm ve 1:1 kesitler cihazda açılır; her görünüm yerel kayda bağlanır.
- Given renk/yazı karşılaştırması, when kullanıcı görüşünü verir, then gözlem aynen kaydedilir; görüş yoksa insan kalite kabulü `NOT_RUN` kalır.

## Spec Change Log

## Verification

Hedefli Python unittest, Flutter test/analyze ve annotation değişiminde mevcut generator’lar offline çalışır. Kurulumdan önce signed bundle kimliği/profili, sınırlı kaynak kapanışı ve Firebase/network client yokluğu denetlenir. iOS’ta plist açıklaması bulunmaması ağ engeli sayılmaz. Kaynak/çalışma ağacı hash’leri öncesi-sonrası eşleşir. Hız veya tam gamut eşitliği iddiası üretilmez.

## Önerilen inceleme sırası

**Başlangıç ve çevrimdışı sınır**

- Ayrı release başlangıcını ve Türkçe kapalı ret ekranını incele.
  [p5e_offline_qa.dart:15](../../app/mobile/lib/app/p5e_offline_qa.dart#L15)

- İlk ağ bildirimi ve sonraki değişimler, gösterimin çevrimdışı kalmasını sağlar.
  [AppDelegate.swift:37](../../tools/image-optimization/p5e_ios_host/AppDelegate.swift#L37)

- İmza, yedek, kilit ve belirsiz kurulum sonucu korumalarını izle.
  [p5e_iphone_offline.py:342](../../tools/image-optimization/p5e_iphone_offline.py#L342)

**Görsel sahipliği ve kayıt**

- Süre sınırı ve geç kaynak temizliği, tek raster sahipliğini korur.
  [p5e_cubit.dart:80](../../app/mobile/lib/features/reading_images/p5e_offline/presentation/p5e_cubit.dart#L80)

- Tek kayıt isteği ve yinelenen ağ bildirimi korumasını kontrol et.
  [p5e_cubit.dart:257](../../app/mobile/lib/features/reading_images/p5e_offline/presentation/p5e_cubit.dart#L257)

- Tam görünüm ve bire bir yazı kesiti aynı dosyaları çizer.
  [p5e_screen.dart:162](../../app/mobile/lib/features/reading_images/p5e_offline/presentation/p5e_screen.dart#L162)

- Cihaz kaydı, 24 görünümü ve çevrimdışı bitişi birlikte doğrular.
  [p5e_session_validation.py:7](../../tools/image-optimization/p5e_session_validation.py#L7)

**Destekleyici kanıtlar**

- Gerçek çizimin DPR3 altında fiziksel piksel ölçeğini sınayan testi incele.
  [p5e_offline_test.dart:315](../../app/mobile/test/reading_images/p5e_offline_test.dart#L315)

- Yedek uyuşmazlıkları gerçek dosya içerikleriyle reddedilir.
  [test_p5e_iphone_offline.py:108](../../tools/image-optimization/test_p5e_iphone_offline.py#L108)

- Gerçek alt süreçte yerel socket erişimi reddedilir.
  [test_p5e_iphone_offline.py:78](../../tools/image-optimization/test_p5e_iphone_offline.py#L78)

- Fiziksel deneme ile son koruma derlemesinin kanıtlarını ayrı oku.
  [verification.md:1](../../be/docs/evidence/p5e-iphone-offline/verification.md#L1)
