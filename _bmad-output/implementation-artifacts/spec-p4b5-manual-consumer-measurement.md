---
title: 'P4b5 — elle başlatılan tek tüketici ölçümü'
type: 'chore'
created: '2026-10-20'
status: 'in-review'
baseline_commit: 'be80ce192e756348ffd87e0b7fa6d20a4e8abbca'
review_loop_iteration: 1
context: ['{project-root}/AGENTS.md', '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md']
---

<frozen-after-approval reason="İnsan onayından sonra amaç yalnız kullanıcı tarafından değiştirilebilir">

## Amaç



VM bağlantısı önceki iki denemeyi medya alınmadan durdurdu. Ayrı QA girişinde kullanıcı akışı elle ilerletsin; gerçek detail/reader hattının seçimi, byte tüketimi, çizimi ve kaynak bırakması kaydedilsin. Mevcut #94 PNG’leri kullanılacak.

## Sınırlar

**Her zaman:** Ekteki [yürütme ayrıntıları](image-optimization/p4b5-manual-consumer-measurement-investigation.md) ve [sayısal öneri](../../be/docs/evidence/p4b5-manual-consumer-measurement/bounded-run-proposal-2026-10-20.json) bu planın parçasıdır. Önceki borç, kayıtlar, kapalı denemeler ve genel tavanlar korunur. Sıra detail → sayfa 1 → sayfa 2 → detail → arka plan → geri dönüş → kapatmadır. Tek pencere 120 saniye; en çok 1 manifest, 12 status, 4 medya / 13.170.884 byte ve 17 HTTP. Sessiz original fallback yoktur.

**Önce onay:** Bu planın onayı yalnız yerel uygulama, credential’sız MAC_EK kopyası, test ve incelemeyi açar. Gerçek kurulum/başlangıç, backend, giriş ve ölçüm; incelemelerden sonra somut APK ve koşu kaydıyla ayrıca onaya sunulur. Limit değişikliği ve ikinci deneme ayrıca onay gerektirir.

**Asla:** Yeni üretim, bulut yazımı, otomatik tekrar, eski denemeyi açma, toplu işlem, fiziksel cihaz veya dağıtım yoktur. Normal uygulamanın erişim, ses, gezinme ve cache davranışı değiştirilmez.

## Girdi ve sınır matrisi

| Durum | Beklenen sonuç |
|---|---|
| Doğru kimlik, tek Başlat | Ortak tüketici hattında dört doğrulanmış çizim |
| Eski/yanlış kayıt, ikinci Başlat, sıra hatası | Yeni istek açmadan kapatma |
| Süre/hata, eksik kanıt veya PID değişimi | Başarısız terminal; tekrar yok |
| Arka plan, geri dönüş, kapatma | Yeni medya yok; sahip olunan kaynaklar bırakılır |

</frozen-after-approval>

## Kod haritası

Ayrıntı belgesindeki dosya ve sembol haritası uygulanır. Yeni `test_driver/reading_consumer_manual_entry.dart`, mevcut `ReadingConsumerQaProbe`, `ReadingImageComparisonCubit` ve ortak görsel alanlarını kullanır. Yeni Python yürütücüsü mevcut saf doğrulayıcıları kullanır; VM kapısını atlayarak başarı üretmez.

## Görevler ve kabul

- [x] Ayrıntı belgesindeki sırayla sabit kapsamı, özel dosya aktarımını ve elle ilerleyen QA girişini ekle.
- [x] Tek başlatma, sıra, süre, kaynak bırakma, gizlilik ve borç koruma testlerini ekle; mevcut tüketici testlerini koru.
- [x] Üç bağımsız inceleme ve kaynak→APK bağı doğrulamasından sonra gerçek koşu teklifini hazırla; ayrı onay bekle.

**Kabul:** Yerel kapılar sağlandığında, kullanıcı koşuyu ayrıca onaylarsa, tek deneme on aşamanın kanıtını ve backend terminalini uzlaştırır. Eksik kanıt başarı sayılmaz. Başarılı emülatör ölçümü fiziksel kaliteyi, RSS/GPU zirvesini, üretim profilini veya tüm P4b’yi kapatmaz.

## Spec değişiklik kaydı

- 2026-10-20 / tur 1: Üç inceleme ADB uzak sonuç/quoting sözleşmesi, screenshot–epoch eşleştirmesi ve gerçek widget/protokol/onay testlerinde boşluk buldu (`bad_spec`). Teknik ekte bağlayıcı düzeltmeler eklendi. Amaç ve limitler değişmedi. İlk kod/test değişiklikleri yalnız bu görev kapsamında geri alındı; kaynak ve bütün test/build kayıtları MAC_EK’te korundu. KEEP: sabit kimlik/proposal, iki eski proof, QA-only DI/Freezed ve opt-in sınırlar, bütçe/credential kapıları, normal consumer hattı ve başarılı regresyonlar. Bilinen kusurlar geri taşınmayacak.

## Doğrulama

Ayrıntı belgesindeki Flutter, Python ve Java kontrolleri uygulanır. İlk yerel test/build sonuçları [raporda](../../be/docs/evidence/p4b5-manual-consumer-measurement/verification-2026-10-20.md) korunur; inceleme kusurları nedeniyle koşu kabulü değildir. Yeniden türetme sonrası testler, yeni numaralı kaynak/APK bağları ve üç inceleme gerekir. Gerçek kurulum/başlangıç, backend, giriş ve ölçüm çalıştırılmadı; ayrı somut koşu onayı gereklidir.

V2 ve sonraki düzeltmeler tarihsel raporlarında korunur. [Yerel hazırlık ve somut koşu teklifi](../../be/docs/evidence/p4b5-manual-consumer-measurement/pre-run-ready-2026-10-20.md) koşu öncesi durumu kaydeder: Flutter 188, Python 66 ve Java 24 test geçti; analyzer temizdir. Source-0007/build-0012, run-binding-0006 ile bağlıdır. Üç koşu öncesi incelemede açık bulgu yoktu; üç PASS kaydı aynı kaynak/run SHA'sına bağlandı. Sonraki ayrı onay ve başarısız gerçek deneme aşağıdadır. Yerel hazırlık, gerçek ölçüm kabulü veya tüm P4b5'in tamamlanması olarak gösterilmez.

## Gerçek deneme sonucu

Kullanıcının ayrı `a` onayıyla source-0007/build-0012/run-binding-0006 için tek kurulum ve ilk başlangıç yapıldı. Ağsız başlangıç sahipliği `QA_OWNER_CHANGED` ile reddedildi; terminal `FAILED_NO_RETRY` kapalıdır. Backend, giriş, pencere ve medya tüketimi sıfırdır. İlk cleanup çıkış kaydı sonraki aynı PID kanıtıyla geçersiz sayıldı; sahipliği yeniden doğrulanan QA süreci kapatılıp eski PID'nin yokluğu ayrıca kaydedildi. Eski kayıtlar değiştirilmedi. [Deneme raporu](../../be/docs/evidence/p4b5-manual-consumer-measurement/runtime-attempt-2026-10-20.md) kesin kanıtı ve kök neden belirsizliğini ayırır. Yeni gerçek deneme yapılmaz. P4b5 kabulü sağlanmadığı için durum `in-review` kalır.

## Önerilen inceleme sırası

- Ayrı QA girişi mevcut router ve ortak tüketici ekranlarını kullanır.
  [reading_consumer_manual_entry.dart:12](../../app/mobile/test_driver/reading_consumer_manual_entry.dart#L12)
- Koordinatör tek Başlat, aşama sırası ve lifecycle kurallarını yürütür.
  [coordinator.dart:24](../../app/mobile/test_driver/manual_consumer_qa/coordinator.dart#L24)
- Özel dosya protokolü kimlik, ekran kaydı onayı ve temizliği bağlar.
  [private_protocol.dart:38](../../app/mobile/test_driver/manual_consumer_qa/private_protocol.dart#L38)
- Gerçek yürütme onayı yerel hazırlık onayından ayrı denetlenir.
  [p4b5_manual_consumer_qa.py:243](../../tools/image-optimization/p4b5_manual_consumer_qa.py#L243)
- Eksik ilk süreç kaydında sahiplik kanıtlanır; hazır backend kaynakları doğrulanarak bırakır.
  [p4b5_manual_consumer_qa.py:428](../../tools/image-optimization/p4b5_manual_consumer_qa.py#L428)
- Gerçek üretici ve CLI testi ilk kayıt yarışını ve geçersiz sahiplikleri sınar.
  [test_p4b5_manual_consumer_qa.py:549](../../tools/image-optimization/test_p4b5_manual_consumer_qa.py#L549)
- Java testi ayrı onay ve kapsam kapısını sentetik girdilerle doğrular.
  [P4b5ConsumerQaManualTest.java:11](../../be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaManualTest.java#L11)
