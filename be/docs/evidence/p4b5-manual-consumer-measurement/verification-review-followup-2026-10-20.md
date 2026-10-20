# P4b5 elle tüketici ölçümü — ikinci inceleme düzeltmeleri

Tarih: 2026-10-20. Son üç incelemeden çıkan iki dar Python bulgusu düzeltildi. [V2 raporu](verification-v2-2026-10-20.md) ve [ilk inceleme düzeltmesi raporu](verification-review-patch-2026-10-20.md) tarihsel halleriyle korundu. Son sürüm üst ajanın üç yeni incelemesine hazırdır; gerçek koşu kapısı kapalıdır.

## Düzeltilen davranış

İlk `record-backend` çağrısı yanlış ADB veya erken backend failure marker nedeniyle kesildiğinde `launch.private.json` henüz yok olabilir. Cleanup artık backend-start claim’i varken backend sonucunu atlamaz. CLI’daki PID, aynı scope ve run SHA’sına bağlı reservation ile doğrulanır. Classpath kaydının adı/SHA’sı run bağıyla; Java binary yolu/SHA’sı classpath bağıyla karşılaştırılır. Gerçek `backend_identity`, exact PID/argv ve başlangıç kimliğini okur. Sahiplik ispatlanırsa launch kaydı yazılır ve mevcut `cleanup_before_ready` yolu çalışır. Süreç kimliği temizlik sırasında yeniden doğrulanır.

Ölü/zombie süreçte önceki başlangıç kimliği hiç kaydedilmemişse sahiplik kanıtı üretilmez. Eksik/geçersiz PID, yabancı komut/Java, değişmiş başlangıç kimliği, farklı startup PID’si veya bozuk reservation/run/classpath/Java bağı da başarı sayılmaz. Backend sonucu açıkça FAILED olur; başka süreç durdurulmaz. Yeni retry veya genel süreç durdurma altyapısı eklenmedi.

Mevcut CLI cleanup testi artık install claim’ini helper ile elle kurmaz. Sentetik Android adapter ile gerçek `ManualController.arm` çalışır; kurulum claim’i, başlangıç claim’i, süreç sahipliği ve baseline gerçek üretici tarafından yazılır. Test ADB yol/SHA’sını ve run SHA’sını doğrular. Sonraki gerçek `main` çağrısı aynı byte’ları korunan claim’i okuyup bağlı adapter ile cleanup yapar.

## Test kanıtları

| Kapı | Sonuç |
|---|---|
| İlgili Python paketi | 21/21 geçti; skip yok |
| Tam P4b5 Python paketi | 65/65 geçti; skip yok |
| `git diff --check` | Temiz |
| Önceki Flutter/Java kapıları | 188 Flutter ve 24 Java testi; değişmeyen hash’ler nedeniyle tekrarlanmadı |

Yeni ilk-kayıt testi başlangıçta hem launch hem local-ready dosyasının bulunmadığını doğrular. Gerçek `arm` ve `reserve_backend` üreticilerinden sonra yanlış ADB/erken failure marker × 12 süreç/bağ durumu olmak üzere 24 varyant gerçek CLI, gerçek `backend_identity` ve gerçek `cleanup_before_ready` üzerinden çalışır. Yalnız `ps` çıktısı ve Android cihaz sınırı sentetiktir; gerçek sinyal yasaklanır. Hazır sahipli backend close request ile kapanır. Diğer durumlarda backend FAILED olur, close request/sinyal üretilmez. Credential ve sahipli QA süreci bağımsız temizlenir.

Gerçek arm→claim→CLI testi verilen/atlanmış/yanlış ADB, ölü/değişmiş QA PID’si, credential cleanup hatası ve erken failure marker olmak üzere yedi dalda aynı üretilen install claim’ini kullanır. Önceki gerçek run timestamp/ACK testleri korunur ve tam pakete dahildir.

İki bellek içi regresyon mutasyonu ayrı sınandı. Arm’ın install claim’inden ADB alanlarını kaldırmak yedi alt senaryoyu; reserved backend cleanup dalını kaldırmak 24 alt senaryoyu kırdı. Her iki mutasyon reddedildi. Kaynak dosyaları mutasyon için değiştirilmedi.

Özel kökteki ham loglar:

- `receipts/python-manual-review-followup-0001.log`
- `receipts/python-tests-review-followup-0001.log`
- `receipts/python-review-followup-mutation-0001.log`
- `receipts/pub-offline-review-followup-0001.log`

## Bu turun kesin dosya listesi

- `tools/image-optimization/p4b5_manual_consumer_qa.py`
- `tools/image-optimization/test_p4b5_manual_consumer_qa.py`
- `be/docs/evidence/p4b5-manual-consumer-measurement/verification-review-followup-2026-10-20.md`

Görevin önceki 32 kod/test/config dosyalık listesi v2 raporundadır. Bu turda yalnız iki Python kaynak/test dosyası değişti.

## Son kaynak ve APK bağı

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-consumer-measurement-20261020-be80ce1`.
Kaynak: `source-0006`; APK: `android/builds/build-0011/consumer.apk`.
Paket: `com.tellpal.tellpal_mobile_poc.readingmanualqa`.
APK SHA-256: `4b653a951c629aab43ad400f4bb1a346bb7782f39c7149dcfb1020bc1eea81d1`.

Bütün Python dalları ve testler bitirildikten sonra iki dosyanın hash’i donduruldu. Ardından yalnız gerekli 1162 kaynak ve 1025 değişmeyen class dosyası kopyalandı. Kopyalama, offline pub, kaynak bağlama/derleme, closure/run bağlama ve son kök doğrulaması sırayla yürütüldü; her bağımlı işlem öncekinin exit_code=0 sonucunu bekledi. Build sırasında veya sonrasında kod/test değiştirilmedi. Önceki tüm kaynaklar, APK’lar ve bağlar korundu.

Son bağ 1162 kaynak, 1303 classpath ve 1388 Python girdisini kapsar. Repository ve kopyadaki bütün bağlı dosyalar son kez aynı hash ile doğrulandı. `app/mobile/build.yaml` hem `files` hem `repositoryFiles` içinde aynı SHA’yla yer alır. Package_config self root source-0006’dır. Kullanılan Android girdileri ve derlemenin 205 JSON/depfile/properties/CMake girdisi tarandı; source-0002/3/4/5 referansı yoktur.

`source-root-validation-0004`, başarılı 188 Flutter ve 24 Java testinin çalıştığı source-0002 ile 1076 mobil/Java kaynak girdisinin ve 1025 class dosyasının byte eşitliğini kaydeder. Yeni Python closure iki güncel Python dosyasını kapsar. Kaynak/APK/Python/classpath ve eski iki denemenin proof zinciri self-check’ten geçti.

| Kayıt | SHA-256 |
|---|---|
| `source-binding-0011.private.json` | `467d75c72bc10985a08d14bcfdb512ca1ee32590a7ecfaa9910731ecf06943da` |
| `selected-build-0005.private.json` | `8333a720b5223a279ed06beb5c05962fdf72d3928657413830c7f5481b1887d1` |
| `classpath-binding-0006.private.json` | `e0e91b9ad4a5bcf2332252714da17dedc2e647fcfe6c347a1e8286d9fe4e99a1` |
| `python-binding-0005.private.json` | `26cf55a122287cbac01eb6dd6d2920648427749113258f46803d290fd251d0aa` |
| `run-binding-0005.private.json` | `e48d4881e13f99e9d8055c1736f95674212999b3ad90ef724776e06f260e546c` |
| `local-preparation-0005.private.json` | `4a6a50a94a5b48d86b46b7d0df4e3105aceba9a4595564ae8b4b48ad199063ae` |
| `source-root-validation-0004.private.json` | `aa336be347c14905520524d2a85e8484fe3ad598d6a742cbc0a86dfd6b2eae40` |

Dondurulan kaynak SHA’ları:

| Dosya | SHA-256 |
|---|---|
| `tools/image-optimization/p4b5_manual_consumer_qa.py` | `86a848fac34b2169b6586d8f40d7b90add045e38c46de83d905a1f5d1f84135e` |
| `tools/image-optimization/test_p4b5_manual_consumer_qa.py` | `51bc80ff94f624b4be2a798e795945291a44fcfab643049efbb53d74db9ebdb3` |

## Açık kapılar ve sınırlar

Son kod için üç yeni bağımsız inceleme üst ajana aittir. Gerçek execution approval ve install/initial-start/backend-start/login/window/measurement claim’i oluşturulmadı. Gerçek kurulum, backend başlangıcı, Firebase girişi, cihaz ölçümü veya medya isteği yapılmadı.

İlk backend kaydı hiç oluşmadan süreç kaybolmuşsa başlangıç kimliği kanıtlanamaz; temizlik sonucu FAILED kalır. Yabancı/değişmiş süreç durdurulmaz. Bu durumlara sahte cleanup başarısı yazılmaz. Yerel testler gerçek cihaz/QA DB/storage/auth uygunluğunu veya fiziksel kaliteyi kanıtlamaz. V2 raporundaki gerçek koşu bütçeleri ve açık P4b kalite/kabul kapıları korunur.
