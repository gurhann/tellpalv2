# P4b5 elle tüketici ölçümü — inceleme düzeltmeleri

Tarih: 2026-10-20. İki Python bulgusu düzeltildi. Bu rapor, [v2 yerel doğrulama raporuna](verification-v2-2026-10-20.md) ek kanıttır; v1 ve v2 raporları tarihsel halleriyle korundu. Son kod için üç yeni bağımsız inceleme üst ajan tarafından tamamlanacaktır. Gerçek koşu kapısı kapalıdır.

## Düzeltilen davranış

`record-backend` ve önceki gerçek claim sonrasındaki diğer CLI hata yolları, kurulum claim’ine bağlı ADB binary yolunu ve SHA’sını `ensure_open` öncesinde çözer. İlk kurulum claim’i bu operational girdiyi kaydeder. Eksik ADB gerçek claim öncesinde reddedilir; sonraki çağrıda argüman atlanmışsa bağlı girdi kullanılır. Farklı ADB argümanı reddedilir. Hata yolunda credential, sahipliği doğrulanmış süreç, reverse ve backend ayrı ayrı temizlenir. ADB çözülemezse backend temizliği yine denenir. Değişmiş PID durdurulmaz; cleanup retry altyapısı eklenmedi.

`ManualController.run` için gerçek metodu kullanan sentetik test eklendi. Geçerli zincir, başlangıca eşit ve deadline’dan hemen önceki phase zamanları kabul edilir. Başlangıçtan önceki, deadline’a eşit ve deadline’dan sonraki phase zamanları tam `PHASE_OUTSIDE_WINDOW` koduyla reddedilir; `accepted.private.json` oluşmaz. Her mutasyonda receipt hash zinciri yeniden hesaplanır; dört ACK yeni receipt/PNG hash’leriyle üretilir. Cihaz ve backend başlatılmaz.

## Doğrulama

| Kapı | Sonuç |
|---|---|
| İlgili Python paketi | 20/20 geçti; skip yok |
| Tam P4b5 Python paketi | 64/64 geçti; skip yok |
| `git diff --check` | Temiz |
| Önceki başarılı Flutter testleri | 188 geçti; kaynak hash’leri değişmediği için tekrarlanmadı |
| Önceki başarılı Java testleri | 24 geçti; kaynak/class hash’leri değişmediği için tekrarlanmadı |

Gerçek CLI testi, gerçek controller ile `main` üzerinden record-backend hatasını çalıştırır. Verilen/atlanmış/yanlış ADB argümanı, ölü/değişmiş PID, credential cleanup hatası ve erken backend failure marker dalları kapsanır. Nonce sahibi credential dosyasının silinmesi, doğru süreç sahipliği, reverse kaldırma ve gerçek backend cleanup yolunun close request/exit kanıtı ayrı doğrulanır. Bağlı ADB bulunmayan dalda başka cihaz komutu olmadan backend cleanup kanıtlanır.

Ham Python logları özel kökte `receipts/python-manual-review-patch-0002.log` ve `receipts/python-tests-review-patch-0002.log` dosyalarıdır. Offline hazırlık logu `receipts/pub-offline-review-patch-0002.log` dosyasıdır.

## Bu turdaki kesin dosya listesi

- `tools/image-optimization/p4b5_manual_consumer_qa.py`
- `tools/image-optimization/test_p4b5_manual_consumer_qa.py`
- `be/docs/evidence/p4b5-manual-consumer-measurement/verification-review-patch-2026-10-20.md`

Önceki 32 kod/test/config dosyalık görev listesi v2 raporunda korunur. Bu turda yalnız iki Python kaynak/test dosyası değişti.

## Son dondurulmuş kaynak ve koşu bağı

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-consumer-measurement-20261020-be80ce1`.
Kaynak: `source-0005`; seçilen APK: `android/builds/build-0010/consumer.apk`.
APK SHA-256: `16a447f4eb3b0c9cd15f54a9a39005c72308b8d84c5ce495a5fbddd8da1f3a31`.
Paket: `com.tellpal.tellpal_mobile_poc.readingmanualqa`.

Bütün kod/test düzeltmeleri bitirilip 20+64 test geçtikten sonra iki Python dosyasının SHA’ları donduruldu. Kopyalama, offline pub hazırlığı, kaynak bağlama/derleme, closure/run bağlama ve son kök doğrulaması sırayla yürütüldü; her bağımlı işlem öncekinin exit_code=0 sonucunu bekledi. Uzun tam clone kullanılmadı; 1162 gerekli kaynak ve 1025 değişmeyen class dosyası taşındı. Derleme sırasında bağlı kaynaklar değiştirilmedi.

Bağ 1162 kaynak, 1303 classpath ve 1388 Python girdisini kapsar. Repository ve kopyadaki tüm bağlı dosyalar son kez aynı hash ile doğrulandı. `app/mobile/build.yaml` hem `files` hem `repositoryFiles` içinde aynı SHA ile yer alır. Package_config self root source-0005’tir. Kullanılan Android/package_config girdileri ve derlemenin 205 JSON/depfile/properties/CMake girdisi tarandı; source-0002/3/4 referansı yoktur.

`source-root-validation-0003`, başarılı 188 Flutter ve 24 Java testinin çalıştığı source-0002 ile 1076 mobil/Java kaynak girdisinin ve 1025 class dosyasının byte eşitliğini kaydeder. Python closure son iki Python dosyasını ve yeni source-0005 yolunu kapsar. Kaynak, APK, Python runtime/import closure ve eski iki denemenin proof zinciri salt okunur self-check’ten geçti.

| Kayıt | SHA-256 |
|---|---|
| `source-binding-0010.private.json` | `4a24f65a3ad8029a29eea875be9f6c5c2ccb92ba01d1f328c6d06e493f447f4b` |
| `selected-build-0004.private.json` | `16bfe6af6fe5f4d90193db7606f5b9133802690df56ffcb8cea591cc39d009a6` |
| `classpath-binding-0005.private.json` | `2e8c9cc8ca51783d5791ac3c8edc70c0f43cb34aa06205cca8ba50c7fe94f677` |
| `python-binding-0004.private.json` | `3ce7efb74a60c88b8010b89de0a08ae28a02c7f26a42d0fefbc31af88904bd39` |
| `run-binding-0004.private.json` | `478aa9d32fbff72ef01499446b652dd84becedc35db254376fb767040bbec47a` |
| `local-preparation-0004.private.json` | `526e7d92226d6e562d0dc8fc1598ac97b2989f51cef8c247f9b0b0a184382a11` |
| `source-root-validation-0003.private.json` | `4aaf606c2deb5e826614e2d2584432f78b56dbff5f7c92d9b87679a61299d182` |

Bu turda dondurulan iki kaynak SHA’sı:

| Dosya | SHA-256 |
|---|---|
| `tools/image-optimization/p4b5_manual_consumer_qa.py` | `7aa46bb30739c4c085189c50462dc6d39d1569458f58b154dc2479b17f2d15fc` |
| `tools/image-optimization/test_p4b5_manual_consumer_qa.py` | `27486a4d6a48bc1c88f818473ef3beaf4f7d9c18bcbf3f4037a353a82d2ebb0f` |

Önceki source-0003/build-0008/run-binding-0003 korunur. Source-0004/build-0009, son hata dalı düzeltilirken kaynak drift’i oluştuğu için wrapper tarafından `SOURCE_DRIFT` ile reddedildi; seçilmedi, kayıtları korundu. Yeni source-0005/build-0010 bu eksik denemenin yerine geçer. Daha eski build-0006 kısmi kaydı da korunur ve seçilmez.

## Açık kapılar

Son kod için üç yeni bağımsız inceleme bekleniyor. Gerçek execution approval ve install/initial-start/backend-start/login/window/measurement claim’i oluşturulmadı. Kurulum, backend başlangıcı, Firebase girişi, cihaz ölçümü ve medya isteği yapılmadı. Yerel testler gerçek cihaz/QA DB/storage/auth uygunluğunu veya fiziksel kaliteyi kanıtlamaz. Önceki rapordaki gerçek koşu bütçeleri ve açık P4b kalite/kabul kapıları korunur.
