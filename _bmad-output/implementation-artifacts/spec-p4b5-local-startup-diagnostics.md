---
title: 'P4b5 — Yerel başlangıç tanısı onarımı'
type: 'bugfix'
created: '2026-10-20'
status: 'done'
baseline_commit: 'aec7a7f31e1a2383e66c9bbc386bfda4f0d9b9bf'
review_loop_iteration: 0
context: ['{project-root}/AGENTS.md', '{project-root}/be/docs/adr/ADR-0018-flutter-skill-architecture-policy.md']
---

<frozen-after-approval reason="Onaylı amaç ve sınırlar">

## Amaç

İki başlangıç reddi nedenini kaybediyor. Host boş/bozuk cmdline ile yabancı süreç adını aynı kodla bildiriyor. Dart support türü ve canonical kontrolünü tek `directories failed` altında topluyor. Yerelde kanıtlanan bu tanı kaybını düzelt; gerçek geçmiş olayın bilinmeyen nedenini çözülmüş gösterme.

## Sınırlar

**Daima:** Tam paket/PID/başlangıç kimliği, symlink, canonical eşitlik, izin, exclusive-create ve terminal ret korunur. Eski başarısızlıklar, APK/run bağları, borç ve tavanlar değişmez. Normal uygulamanın erişim, ses, cache ve gezinmesi korunur.

**Önce onay:** Yeni kaynak kopyası, APK veya gerçek cihaz/uygulama koşusu bu planın dışındadır. Sonraki bulgu davranış değişikliği gerektirirse somut öneriyle dur.

**Asla:** Yeni kurulum/başlangıç, ADB/VM/backend/giriş/ölçüm veya bulut işi yoktur. Retry, süre uzatma, `.strip()` ile sahiplik toleransı, otomatik canonical/alias kabulü ve üst dizin izni değişikliği yoktur. Ham yol, cmdline, token ve istisna metni tanıya yazılmaz. `.memlog.md` ve ilgisiz dirty dosyalar korunur.

## Girdi ve uç durumlar

| Durum | Beklenen |
|---|---|
| Tam paket ve geçerli stat | Aynı süreç kimliği kabul edilir |
| Boş/bozuk/yabancı cmdline | Ayrı sabit tanı; terminal ret, süreç sinyali yok |
| Cmdline/stat okuma hatası veya timeout | Ayrı mevcut ret; retry yok |
| Geçerli ham support dizini, 0771 | Baseline oluşur; üst izin korunur |
| Eksik/yanlış tür veya son/ara symlink | Güvenli alt neden; baseline/HTTP/yazma yok |
| Canonical uyuşmazlığı veya resolve istisnası | Ayrı güvenli alt neden; fallback yok |

</frozen-after-approval>

## Kod haritası

- `tools/image-optimization/p4b5_manual_consumer_qa.py`: `process_identity`, `_process_query`, `safe_android_diagnostics`; tam byte eşitliği ve mevcut sekiz olay sınırı korunur.
- `app/mobile/test_driver/manual_consumer_qa/private_protocol.dart`: `initialize` support kontrolleri ve sabit diagnostic enum’ları. `failureMarker unavailable`, initialize bitmediyse beklenen korumadır.
- `app/mobile/test/reading_images/manual_consumer_protocol_test.dart`: `temporary()` ham yolu önceden canonical yapıyor; yeni testler bu kör noktayı tekrarlamaz.
- [Eski sonuç](../../be/docs/evidence/devtools-consumer/verification.md): salt okunur. Shell alias kaydı Dart’ın gerçek support string/resolve sonucunu içermez; host ret anının ham byte’ları yoktur.

## Görevler ve kabul

- [x] Python controller’da boş/bozuk cmdline tanısını ayır; kabul edilen paket byte’larını değiştirme. `tools/image-optimization/test_p4b5_startup_diagnostics.py` gerçek adapter metotlarını sentetik subprocess ile sınasın.
- [x] Dart protokolünde support türü, tür okuma hatası, canonical uyuşmazlığı ve resolve hatasını sabit alt kodlarla ayır. `app/mobile/test/reading_images/manual_consumer_startup_diagnostics_test.dart` mevcut bootstrap/DI/protokolü kullansın; yeni altyapı eklenmesin.
- [x] Her tablo satırı için anlamlı test çalıştır; mevcut bootstrap/protocol regresyonlarını ve analyzer’ı doğrula. Skip’i başarı sayma. Mevcut SDK/cache ile çalış; indirme veya kaynak kopyası yapma.
- [x] `be/docs/evidence/p4b5-startup-local/verification.md` uygulama kanıtını ve açık kök nedeni kaydetsin. Üç son inceleme BMad step-04’te yürütülüp aynı rapora eklenir. Eski pending değişiklikleri ayrı koru; kapanan ölçüm spec’ini tamamlandı yapma.

**Kabul:**
- Given geçersiz girdi, when gerçek doğrulayıcı çalışır, then ret ve tanı sınıfı kesin olur; ham sır metni çıkmaz.
- Given erken support reddi, when gerçek bootstrap çalışır, then baseline/configure/medya işi ve güvenilmeyen yola yazma oluşmaz.
- Given yerel testler geçer, when sonuç raporlanır, then bu sonuç gerçek Android başlangıcı veya P4b kabulü sayılmaz.

## Doğrulama

Yeni Python unittest modülü; Flutter yeni test, mevcut bootstrap/protocol testleri; ilgili Dart dosyalarında analyzer. Ham test logları yeni `MAC_EK/tellpal-qa-private/p4b5-startup-local-20261020-01/` altında kalır; yeni kaynak kopyası yoktur. Cihaz, gerçek ağ ve kapalı run kullanılmaz. Model oturumdan devralınır.

## Spec değişiklik kaydı

2026-10-20: Kullanıcı yeni ölçüm olmadan iki başlangıç sorununu yerel tanıyla inceleme ve düzeltme istedi. Araştırma tanı kaybını yeniden üretti; tarihsel kök nedeni kanıtlamadı. Bu plan o sınırlamayı açık tutar.


## Onay ve uygulama devri

2026-10-20: Kullanıcı “A — Planı uygula, yalnız yerel test yap” yanıtını verdi. Frozen blok değişmez; sprint story anahtarı yoktur. BMad step-03 uygulamasını yürüt; renderer/akışı yeniden başlatma. Model oturumdan devralınır.

Mevcut çalışma ağacı dirty ve kullanıcı bunu koruyarak bu dar onarımı onayladı. İki kaynakta önceki scope/sahiplik değişikliklerini koru. Yalnız bu plandaki iki kaynağı, iki yeni test dosyasını ve yeni raporu değiştir; eski test/spec/rapor/özel run dosyaları salt okunurdur. Kaynak kopyası veya APK hazırlama. Ham yerel test çıktıları için tam hedef `/Volumes/MAC_EK/tellpal-qa-private/p4b5-startup-local-20261020-01/`; eski dizin yoksa yeni `0700`, dosyalar `0600` ve exclusive oluştur. Mevcut Flutter `/Users/gurhankucuk/Documents/dev/flutter/bin/flutter`, Python `tools/image-optimization/.venv/bin/python`. Repo `app/mobile/.dart_tool/package_config.json` kendi köküne bağlı; repo içinden mevcut SDK/cache ile çalış. Yeni bağımlılık indirme veya pub upgrade yoktur. Test VM’si/headless Flutter, yasak gerçek uygulama/ADB/VM ölçümü değildir; gerçek emülatörün VM servisine bağlanma.

Uygulamada yeni dosya sistemi soyutlaması ekleme. Mevcut resolver/platform ve gerçek dosya sistemi, testte gerekiyorsa dar Directory fake kullan. Python’da kabul edilen cmdline byte kümesi aynı kalmalı; boş/bozuk ret ile yabancı paket ayrımı sabit kodlardan oluşsun, strip/normalizasyon/retry yoktur. Eski paketin NUL’suz tam byte eşitliği kabulünü kendiliğinden değiştirme. Dart tanısı yalnız enum/sabit kod taşır. `failureMarker unavailable` korunur; güvensiz yolda terminal dosyası oluşturulmaz.

Yeni testlerle tablo satırlarını doğrula; ilgili mevcut bootstrap/protocol regresyonlarını çalıştır. Tüm eski Python modüllerini gerekmeden çalıştırıp native JSON skip’lerini başarı diye yazma; ilgili hermetik adapter/sahiplik regresyon sınıflarını seç ve filtre kapsamını açık kaydet. Gerekli Flutter önbelleği eksikse offline kuralları koruyarak raporla. Mevcut testlerin ham support yolunu canonical yapan helper’ı yeni negatif tanı testlerinde kullanma. Rapor, giderilen tanı kaybı ile hâlâ bilinmeyen tarihsel nedeni ayırsın. Implementer test sonucunu ve tablo eşlemesini üst ajana bildirir; üç son inceleme üst ajandadır. Commit atma; eski pending kaynaklar commit’e taşınmaz.

Görev dışındaki 3.377 repo dosyasının başlangıç yol/SHA zinciri `07ebc2922999dea0007d1a210920caab490ae5e3e28cb7aae30829c2f6abb18c` olarak root tarafından doğrulandı. Başlangıçtaki iki hedef kaynağın byte’ları root oturumunda saklıdır; root son inceleme için yalnız bu tur farkını çıkaracaktır.


## Uygulama doğrulaması

13 Python ve 41 Flutter testi/0 skip geçti; dört ilgili Dart dosyasının analyzer sonucu temizdir. İlk test fixture hataları ayrı loglarda korundu. Onaylı tablonun altı satırı raporda çalışan testlere bağlandı. Frozen blok, 14 eski özel kanıt dosyası ve görev dışı 3.377 repo dosyasının zinciri yeniden doğrulandı. Üç inceleme adımı step-04’e aittir; rapor yazma göreviyle karıştırılmaz. Bu teknik sıralama açıklaması inceleme yükümlülüğünü veya frozen kapsamı değiştirmez. Yeni kaynak/APK/cihaz koşusu yoktur; tarihsel kök neden açıktır.


## Sonuç

Üç inceleme tamamlandı. İki katmanda bulgu yok; Edge katmanındaki geçici dizin taşınabilirliği bulgusu test-only patch olarak kapatıldı. Aynı 13 Python/41 Flutter testi ve dört dosya analyzer tekrar geçti; skip yoktur. Yalnız yerel tanı kaybı onarımı tamamlandı. Tarihsel kök neden ve P4b/cihaz kabulü açık kalır. Eski pending kodu korumak için kaynak ve yeni testler çalışma ağacındadır; yerel commit yalnız bu yeni spec ve kanıtları kapsar.

## Önerilen inceleme sırası

**Başlangıç doğrulaması**

- Support türü ve canonical retlerini ayrı sabit kodlarla gösterir.
  [private_protocol.dart:129](../../app/mobile/test_driver/manual_consumer_qa/private_protocol.dart#L129)

- Tam paket eşitliğini koruyarak boş ve bozuk cmdline’ı ayırır.
  [p4b5_manual_consumer_qa.py:267](../../tools/image-optimization/p4b5_manual_consumer_qa.py#L267)

**Yerel kanıt**

- Ham support yollarında gerçek bootstrap ve yazmadan ret davranışını sınar.
  [manual_consumer_startup_diagnostics_test.dart:70](../../app/mobile/test/reading_images/manual_consumer_startup_diagnostics_test.dart#L70)

- Gerçek adapter/cleanup üzerinde tanıyı ve terminal reddi doğrular.
  [test_p4b5_startup_diagnostics.py:12](../../tools/image-optimization/test_p4b5_startup_diagnostics.py#L12)

- Çalışan testleri, eski hataları ve açık cihaz kabulünü ayırır.
  [verification.md:1](../../be/docs/evidence/p4b5-startup-local/verification.md#L1)
