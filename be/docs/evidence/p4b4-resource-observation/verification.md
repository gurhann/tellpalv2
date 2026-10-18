# P4b4 kaynak gözlemi doğrulaması

Tarih: 2026-10-19. Başlangıç: `52098d94f89c9743710d213c43bdcf318486108b`. Model oturumdan devralındı. Bu kayıt credential’sız yerel kaynak/test doğrulamasıdır. Yerel uygulama, testler ve üç bağımsız BMad incelemesi tamamlandı. Son koruma denetimiyle yalnız görev dosyaları yerel commit’e alınır; push/PR yapılmaz.

## Değişiklik

İki QA probe’unun mevcut `ownedResources()` yayılımı ortak `readingOwnedResources()` helper’ını çağırır. Üst düzey dört `active*` alanı korunur. `resourceObservation` v1 bloğu isolate boyunca sabit opak kimlik, integer slot ve kanonik onluk string byte/attempts taşır. Helper yalnız runtime getter’larını okur. Probe oluşturmak veya stop etmek sayacı/kimliği sıfırlamaz.

Saf `validate_resource_observations(samples)` raporu yalnız verilen JSON’u doğrular. Şemasız kayıt `legacy/measured=false` döner; v1 iddiasındaki eksik, null, yanlış tip/format/sürüm tipli `ResourceObservationError(code, path)` üretir. İlk attempts değeri baseline’dır. Yaşam değişimi ve azalan sayaç reddedilir. Loaded boyutları mevcut state olayından da çözülebilir. Tam drain dört active alanı ile slot/byte sıfırını gerektirir.

## Doğrulama sonuçları

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b4-resource-observation-20261018-52098d9/`. Komutlar ve tam kaynak/log SHA-256 değerleri sıralı `receipts/` makbuzlarındadır. Log/makbuzlar CREATE_NEW ve `0600`, dizinler `0700` oluşturuldu. Test sayıları birbirine veya tekrar koşularına eklenmez.

| Ham log | Sonuç | SHA-256 |
|---|---|---|
| `logs/20-focused-restored.log` | Odaklı Flutter: 14 geçti | `f894d68acf9217b606e8ad96b544ecff92d7e4f7181090e1918b52afe64ba7ea` |
| `logs/12-flutter-full-corrected.log` | Tam Flutter: 507 geçti; 49 izlenen + 1 yeni test dosyası | `098e52a9be7b6e0d88bd7b1aa8c28f71d79dfae49debe838b3ecfbbd60cc2e6f` |
| `logs/10-flutter-analyze.log` | Analyzer: sorun yok | `8923743ed63390599bb9c10691a3f156520493cb071f5cd5ceb9f9aa0990c8d0` |
| `logs/30-python-final-restored.log` | Python: 106 geçti; 13 yeni + 93 eski test | `fff5ba4c7b3cf0232821e9f563a7debef4f6809c79d30f758b8043e82f76ceb1` |

Odaklı ve tam Flutter komutları `--no-pub --reporter expanded` kullanır. Tam küme `01-source-copy.private.json` içindeki 50 dosyayı tek koşuda çalıştırır. Analyzer `flutter analyze --no-pub` çalıştırır. Python komutu `-m unittest -v test_p4_resource_observation test_p4_consumer_qa test_p4_reading_comparison` kullanır. `P4_RESOURCE_DART_JSON_LOG`, son odaklı Flutter ham logunu gösterir. Eski yürütücülerin gerçek çalıştırma komutları çağrılmadı. Eski testlerin yazdırdığı emulator/APK başarı metinleri mock test çıktısıdır; yeni cihaz veya build kanıtı değildir.

## Gerçek Dart JSON → Python raporu

`reading_resource_observation_test.dart` önce gerçek native decode yapar; baseline `1` olduktan sonra ikinci native çağrının ilk await öncesindeki pending örneğini alır. `activeImages=0`, slot `1`, byte `"16"` görülür. İptal bildirimi rezervasyonu açmaz. İkinci çağrı native attempts artırmadan reddedilir; gerçek cleanup sonrasında probe stop JSON’u slot/byte `0` gösterir. Resident testi iki probe’un aynı getter değerlerini aktardığını, ikinci reddin attempts artırmadığını ve yeni isolate’ın farklı kimlik aldığını doğrular.

`reading_comparison_probe_test.dart` gerçek loopback HTTP → data source → repository → Cubit → native codec → probe JSON zincirini kullanır. Loaded state ve snapshot `822 × 1334 × 4 = 4386192` byte rezervasyonunu taşır; stop JSON’u drain’i gösterir. Consumer ve comparison paint testleri aynı native raster/epoch ile v1 alanlarını doğrular. Sır ve object key sızıntısı kontrolleri korunur.

Ham `P4_RESOURCE_JSON:` satırları Python’ın `test_real_dart_native_and_loopback_json_reaches_report` testinde doğrudan ayrıştırılır. Sonuç `P4_RESOURCE_REPORT:` satırında saklanır. İki dilde ayrı hazırlanmış fixture kullanılmaz. Son raporda her iki dizinin baseline/final attempts değeri `"1"/"2"`, delta `"1"`, maksimum gözlenen slot `1`; native-pending byte `"16"`, loopback-codec byte `"4386192"` olur. Lifetime değerleri ayrı isolate dizileri içinde sabittir.

## Matris ve mutasyonlar

Python ham çıktıda şu 13 test adıyla görünür:

```text
test_legacy_has_no_fabricated_measurement_or_delta
test_claimed_v1_null_missing_unknown_and_mixed_refused
test_decimal_type_and_canonical_format_matrix
test_exact_integer_slots_active_dimensions_matrix
test_pending_and_transfer_are_not_equated_with_visible_images
test_slot_byte_consistency
test_loaded_geometry_and_handles
test_drain_requires_all_handles_and_reservation
test_nonzero_baseline_large_lossless_delta_and_monotonic_order
test_lifetime_changes_refused_and_separate_sequence_has_own_baseline
test_more_than_4300_digits_remain_lossless_without_global_settings
test_empty_and_nonobject_sequence_refused
test_real_dart_native_and_loopback_json_reaches_report
```

Matris; legacy, eksik/null blok/alan, sürüm, bool/kesir/negatif, boşluk/üs/baştaki sıfır/Unicode rakam, büyük kayıpsız sayılar (4501/5000 basamak dahil), slot-byte tutarsızlığı, loaded geometri/handle, eksik drain, sıfır olmayan baseline, azalan attempts ve farklı lifetime durumlarını kapsar.

Altı mutasyon yalnız yeni özel kopyada ayrı ayrı uygulandı. Her biri derleme hatası yerine assertion hatasıyla yakalandı; ardından kaynak SHA-256 başlangıç değerine döndü. `29-mutation-final-summary.private.json` altı sonucu ve geri yüklemeyi son kaynak hash’lerine bağlar. Dart 13 kaydı aynı kaynak hash’iyle geçerlidir; son Python değişikliğinden sonra beş Python mutasyonu 24–28’de tekrarlandı. İlk 14–19 kayıtları korundu.

| Kayıt | Mutasyon | Sonuç |
|---|---|---|
| 13 | Getter slotunu sabit sıfır yap | Gerçek pending/resident Dart assertion’ı düştü |
| 24 | Legacy ölçüm/delta uydur | Legacy eşitlik assertion’ı düştü |
| 25 | Eksik onluk alanı sıfırla doldur | Tipli ret assertion’ı düştü |
| 26 | Lifetime korumasını kaldır | Farklı yaşam ret assertion’ı düştü |
| 27 | Artan sıra korumasını kaldır | Azalan sayaç ret assertion’ı düştü |
| 28 | Drain’de slot/byte rezervasyonunu yok say | Eksik drain ret assertion’ı düştü |

Geri yükleme sonrasında odaklı Flutter ve gerçek JSON’lu Python kümesi yeniden geçti (20/30).

Python’ın 4300 basamaklı int→string sınırı son öz denetimde giderildi. Kanonik onluk değerler dokuz basamaklı parçalarla biçimlenir; global interpreter limiti değiştirilmez. 23 ve mutasyon sonrası 30 koşuları 4501 basamaklı baseline/final, 5000 basamaklı byte ve büyük delta eşitliğini doğrudan doğrular. Dart dosyalarının hash’i değişmediği için tam Flutter/analyzer sonucu geçerlidir.

## İlk hatalar ve ortam kapanışı

İlk hatalı denemeler silinmedi: 02’de normal `test` içinden `runApp` gerektiren stop çağrısı reddedildi; ilgili yerel testler `testWidgets/runAsync` düzenine alındı. 03’te eksik izlenen `consumer-terminal-ledger.txt` ve mevcut interpreter’da olmayan websocket import’u saptandı. 06’daki geniş mevcut paket yolu, ilgisiz `typing.py` ile stdlib’i gölgeledi. Yalnız zaten kurulu pure Python websocket paketini gösteren özel symlink ile daraltıldı. 07’de tam kümenin sentetik TLS broker’ı göreli `.venv/bin/python` yolunu bulamadı; metadata’daki mevcut interpreter venv’ine symlink eklendi. 04/08/11 makbuzları bu düzeltmeleri ve mevcut paket kaynak hash’lerini tutar. Kurulum/indirme yapılmadı; ilk operational metadata değiştirilmedi.

`31-source-protection-closure.private.json`: 239 kaynak/test/fixture dosyası; repo/private farkı, eksik göreli import, eksik izlenen test ve izin farkı yok. Mevcut SDK/cache ve paket metadata’sı kullanıldı. Dirty `app/mobile/lib/main_image_experiment.dart` private kopyada HEAD’den alındı; repo dirty dosyası korunur. Untracked local-lossless işleri kapsam dışıdır. Bütün dirty/untracked ağaç test edildiği iddia edilmez.

Koruma hash denetimi: 3279 repo, 72 eski run, 192 P4a5, 296 P4b1, 281 P4b2 ve 260 P4b3 özel dosyasında eksik/değişen yok. Companion salt okunur kaldı. Frozen block SHA-256: `50c799efb1ab165c92f1e996e144510caaec9f6149bbba091d46cb9972833b23`; başlangıçla eşit. `git diff --check` temiz.

## Kaynak hash’leri

| Görev dosyası | SHA-256 |
|---|---|
| `app/mobile/test_driver/reading_resource_observation.dart` | `24c91acbf9f03c5efa9dcb90b66e6c0c0c23ad0854cf7c642e9b55d38827cf75` |
| `app/mobile/test_driver/reading_comparison_probe.dart` | `6f354494a20cb2bbf50fd73f8eb068960674f964446c45413677de12b34e2634` |
| `app/mobile/test_driver/reading_consumer_qa_probe.dart` | `afafef16ec5d9adc076773950b343d752f3949fd498943aa74ed86fe1de2e23d` |
| `app/mobile/test/reading_images/reading_resource_observation_test.dart` | `5c86e802a88a431b620a4f420291932417803ca33463d55b57a7d2e1b7467cd7` |
| `app/mobile/test/reading_images/reading_comparison_probe_test.dart` | `3ddbc69aa0a3a62743f780de7d2dc705239f3530d294ae69b024ec1cb3de82d1` |
| `app/mobile/test/reading_images/reading_consumer_qa_probe_test.dart` | `d74023b794888295676d58c256a1382bfec2200c41688660fe14a79124966764` |
| `tools/image-optimization/p4_resource_observation.py` | `d1412c8eabae5ed167bf465de57570b67ecbcd5c45e3ecda5f9db84d779d8aec` |
| `tools/image-optimization/test_p4_resource_observation.py` | `b3a37a49c6e0ead06532903920b8d68bd5f1cec4dd43d9636901ff3b40b5f472` |

## Açık sınırlar

Bu rapor yalnız gözlenen rezervasyon maksimumudur. Örnekler arası peak, süreç RSS, engine/GPU veya fiziksel Android/iPhone kabulü değildir. Kaynak gözlemi geometry/paint/auth/manifest kanıtını tamamlamaz. Eski kabul sonucu yeniden yazılmaz. Runtime/Cubit/transport ve P4b2 rezervasyon algoritması, normal tüketiciler ve eski yürütücüler değişmedi. Yeni gerçek run, bulut/giriş/cache/cihaz/üretim profili veya sayısal bütçe kararı yoktur. Genel P4b ve üretim/fiziksel kapılar açık kalır.


## Bağımsız inceleme ve kapanış

Üç BMad incelemesi bağlamsız ve oturumdan devralınan modelle yürütüldü. Kör inceleme,
sınır durumları incelemesi ve doğrulama boşluğu incelemesi tam tracked/untracked diff'i,
ayrı görev diff'ini ve başlangıçtaki ilgisiz değişikliklerin envanterini aldı.
Kaynak ve ham kayıtlar salt okunur incelendi. Üç katman da eyleme dönük bulgu üretmedi;
`intent_gap/bad_spec/patch/defer` sayıları `0/0/0/0`, açık düzeltme yoktur.
Sonuçlar `reviews/33-independent-reviews.private.json` içinde korunur.

Kök ajanın son denetimi 239 kaynak dosyasının private/repo eşitliğini, 507/14 Flutter
ve 106 Python sonlarını, analyzer'ı, gerçek JSON zincirini, matris testlerinin geçmiş
olmasını, altı mutasyonun assertion ve geri yükleme hash'lerini tekrar doğrular.
3279 repo /72 eski run /192 P4a5 /296 P4b1 /281 P4b2 /260 P4b3 dosyası ve frozen
block korunur. Son makbuz `receipts/34-root-completion.private.json` içindedir.

Yerel commit kapsamı iki probe, ortak Dart helper, üç Dart testi, saf Python modülü
ve testi, spec, salt okunur inceleme ve bu rapor olmak üzere 11 görev dosyasıdır.
Önceki kayıtlar, ilgisiz `.memlog.md`, dirty giriş ve local-lossless işleri korunur.
Eski yürütücülerin run/onay zincirleri, runtime ve normal tüketiciler değişmedi.
