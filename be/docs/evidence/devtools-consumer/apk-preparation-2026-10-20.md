# DevTools consumer offline APK hazırlığı

2026-10-20. [Spec’in güncel devrindeki](../../../../_bmad-output/implementation-artifacts/spec-p4b5-devtools-consumer-observation.md) açık kopya/offline APK onayı uygulandı. Kaynak kopyası, yerel testler ve debug APK üretildi. **Güncel aday `build-0002` / `run-binding-0002` üç bağımsız artifact incelemesini geçti; ayrı gerçek koşu onayını bekliyor.** İlk adayın `SOURCE_DRIFT` reddi ve dosyaları korunur; aşağıda onarım açıklanır. Gerçek QA koşusu açılmadı; kurulum, uygulama başlangıcı, QA backend başlangıcı, giriş, pencere ve QA medya isteği bu aşamada **0**’dır. Commit/push yapılmadı.

## Kaynak kopyası

Onaylı [audit](copy-payload-audit-2026-10-20.json) SHA-256 `7c216bde7d93947dbc385520c96668be6a4ef0061bcc6ac8fe8861cc40effaa9` yeniden doğrulandı. **1.166 dosya / 8.565.689 byte**, `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-retry-measurement-20261020-3ec09d1/source-0001` hedefine kopyalandı. Kopya öncesi tüm repo byte/hash değerleri eşleşti; kopya sonrası ve build sonrası 1.166 hash yeniden eşleşti. `main_image_experiment.dart` için audit’in `copyFromBaselineCommit` değeri `3ec09d1e124080d9ffebf5ec4cd098cbd6a177b8` kullanıldı; dirty dosya alınmadı.

Mevcut `devtools-0001/` korundu. Yeni kaynak dizini ve makbuzlar exclusive creation ile oluşturuldu. Kopya dizinleri `0700`, dosyaları `0600`’dır. Kaynak/runtime kodu değiştirilmedi. Token, servis hesabı veya Firebase API yapılandırması okunmadı/kopyalanmadı. `android/local.properties` yalnız mevcut Android ve Flutter SDK yollarını içerir.

`flutter pub get --offline` exit 0 verdi. `package_config.json` içindeki `tellpal_mobile_poc` kendi `source-0001/app/mobile` köküne çözüldü. Pub’ın genel “Downloading packages...” satırı offline komutta da yazıldı; komut `--offline` ile yalnız mevcut cache’i kullandı. Bağımlılık kurulumu/indirmesi veya SDK değişikliği yapılmadı. 50 tarihsel proof dosyasının hash’i salt okunur doğrulandı; önceki tüketimler iade edilmedi.

## Bu kopyada doğrulama

| Kapı | Sonuç |
|---|---|
| `flutter test --no-pub --reporter expanded test/reading_images` | 197 test, 0 skip, PASS |
| `flutter analyze --no-pub` | 0 bulgu, PASS |
| Python P4b5 paketi | 80 test, 0 skip, PASS |
| Maven wrapper, offline `P4b5ConsumerQa*Test,P4b5ConsumerQaIT` | 26 test, 0 skip, PASS; main/test compile geçti |
| Mevcut `Controller.build` / Gradle init, offline `:app:assembleDebug` | PASS, 1m 54s |

Python dört JSON ortam girdisi bu kopyanın yeni `logs/flutter-tests-0001.log` dosyasını kullandı. Yeni `P4B5_MANUAL_RETRY_RESOURCE_JSON` gerçek probe/native codec kullanan **headless Flutter test engine** çıktısıdır. Android uygulama süreci, emülatör veya DevTools gözlemi değildir. Sentetik loopback fixture’ı retained #94 medyasının teslimat kanıtı sayılmaz. Tarihsel 232 Flutter testinin 35 görev dışı local-lossless testi onaylı kopyada bulunmadığı için bu koşunun sayısı 197’dir.

Java kapısı için bu kökte yeni boş PostgreSQL 18 cluster’ı ve iki `p4b5_synthetic_devtools_*` veritabanı oluşturuldu. Gerçek QA DB’ye bağlanılmadı veya klon alınmadı. İki V37 integration testi skipsiz geçti. Script yalnız başlattığı cluster’ı `finally` içinde kapattı; shutdown logu ve `postmaster.pid` yokluğu doğrulandı. Bu sentetik test sunucusu QA backend başlangıcı değildir.

Ham test/build/kanıt dosyaları MAC_EK’te kaldı. Repo içine ham log, APK, token veya credential alınmadı.

## İlk adayın korunan artifact kimliği

Özel kök aşağıdaki tabloda `ROOT` olarak kısaltılmıştır: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-retry-measurement-20261020-3ec09d1`.

- APK: `ROOT/android/builds/build-0001/consumer.apk`, **84.273.783 byte**.
- SHA-256: `c331c42ac10ba445a7835f8db26829539e262ddad3b879be9692e5d34847cbe7`.
- Paket: `com.tellpal.tellpal_mobile_poc.readingmanualretryqa`.
- Entry: `test_driver/reading_consumer_manual_retry_entry.dart`; debug badging doğrulandı.
- Entry, measurementId ve proposal SHA APK kernel içinde bulundu; kernel dependency dosyası seçili kopyanın entry yolunu içerir.
- APK ZIP adları ve içerikleri Firebase API key, private key ve service-account işaretleri için tarandı; bulgu yoktur. Bu sınırlı kontrol kapsamlı güvenlik denetimi iddiası taşımaz.

| Makbuz (`ROOT/receipts/`) | SHA-256 |
|---|---|
| `source-binding-0001.private.json` | `7f71854aa90c8df86cb3647efc6b584ded1b746d0d45301985fb340c7f53569c` |
| `classpath-binding-0001.private.json` | `3e41d0face64d4e162e2991e13ff1087b5829f6d91d1213090982fd166cef254` |
| `python-binding-0001.private.json` | `bbd51ab55ea012547353880657b21f8cca987bd3f3863919bb4b11982145c5b4` |
| `selected-build-0001.private.json` | `668cfb61a2323cd6a435b1e3928571148f59355ee989f72360a7ea071dc787cf` |
| `prior-attempt-proof-0001.private.json` | `bd6bf6da2a5ecb9bf5ba4fe38477884b0e1eab14c3f7ea24bd7b805f4bf3282b` |
| `run-binding-0001.private.json` | `a1f95a9e26c6e9773322f49c52156f5ae8cf35aba668b3ec23b2eb77203ddf29` |
| `apk-audit-0001.private.json` | `5c90d2b253d7686b081ee3a3416fcaed27959b7abe7116e584f8b63ea001dac9` |
| `local-preparation-0001.private.json` | `54384af80e470a393936dcd8b0a5592763253aeb83f11985a18eebd8f6d27e28` |

İlk adayın Java bağı 280 sıralı classpath girdisi / 1.304 dosya içerir. Girdiler bu kopyanın başarılı Surefire test raporlarından alınmıştır; son boş ayraç bir bağımlılık girdisi olarak seçilmedi. Python bağı 1.388 dosya, mevcut interpreter/binary SHA’sı, seçili controller ve owned import closure’ı içerir. Önceki üçüncü taraf dosyaların hash’leri doğrulandı; owned kaynak yolları bu kopyaya bağlandı. İlk kontrolde `gate(reviews=False)`, `retry_bindings`, `selected_build` ve `binding_links` özgün repo kökü elle verilerek geçti. Bu kontrol gerçek bağlı CLI’nin varsayılan kökü için kabul kanıtı değildir; aşağıdaki onarım bunu düzeltir.

## Ham log bağları

| Dosya (`ROOT/`) | Byte | SHA-256 |
|---|---:|---|
| `logs/pub-offline-0001.log` | 536 | `8ba0e855cee8c17405af01041b2445376ba3b2be751f15a181262a86bd451c26` |
| `logs/flutter-tests-0001.log` | 302994 | `ccbca3cb07cc2690e76c0531aae1d19910ce7c690742d560913649e288185f37` |
| `logs/flutter-analyze-0001.log` | 96 | `0d85811b153c173cbc1d4cff10e903d81edd6c434eeb1aa992d766d73e443b79` |
| `logs/python-tests-0001.log` | 4122 | `7413edf5852cd7a2319fdb1971cd5ea013ca6cc6d5670eecfb6d7131693e93a9` |
| `logs/java-tests-0001.log` | 4366 | `e1072d01acf4b4012317a95d0b35503630763c5d0e9906eadcde24ab38bd93fe` |

Build logu `ROOT/android/builds/build-0001/build.log`, badging `badging.log`, sentetik DB setup/shutdown logları `ROOT/synthetic-postgres-0001/` altındadır. Üretilen package/Gradle girdilerinin hash’leri local-preparation makbuzunda tutulur.

## Kapalı gerçek koşu kapısı ve açık işler

`execution-approval-01.private.json` yoktur. İlk adayda kök elle verilerek çalıştırılan `gate()` eksik execution approval nedeniyle `FileNotFoundError` verdi; gerçek varsayılan kök ise `SOURCE_DRIFT` verdi. Güncel adayın varsayılan kökle kapanış kanıtı aşağıdadır. Gerçek stage claim sayısı **0**’dır. Artifact kimliği gerçek execution hakkı üretmez. Bir sonraki çalışma için üst ajanın aynı son kaynak/APK/run üzerinde üç bağımsız incelemeyi tamamlaması ve ayrı somut gerçek koşu onayı alması gerekir. Bu rapor bu incelemelerin yerine geçmez.

DevTools bağlanmadı; ADB cihaz işlemi yapılmadı. Network/Memory, dört gerçek ekran/ACK, probe/sunucu sayımları, aynı PID’ye Home/dönüş ve native drain gözlenmedi. `verification.md` veya gerçek başarı/claim üretilmedi. P4b kabulü ve spec’in sonraki görevleri açıktır.

Retained #94/tr LIVE generation 4/revision/digest, 1427→1428, kanonik QA UID ve aynı DB/V37 borcu değişmedi; güncel QA DB sorgusu yapılmadı. Tek 120 saniye, 1 manifest, 12 status, 4 medya/13.170.884 byte ve 17 HTTP dahil sayısal sınırlar aynen korunur. Eski kanıtlar, spec/öneri/audit, `.memlog.md` ve ilgisiz değişiklikler korunmuştur.

## İlk adayın bağlama hatası

Üst ajan gerçek bağlı CLI kökünü ayrıca kontrol etti. Önceki `gate(reviews=False)` kanıtında controller’a özgün repo kökü elle verilmişti. Oysa kopyadaki CLI varsayılanı `source-0001` köküdür. `repositoryFiles` alanındaki dirty experiment hash’i bu kökte aranır; onaylı baseline kopyasıyla uyuşmadığı için gerçek varsayılan controller `SOURCE_DRIFT` verdi. Yeni kaynak kopyası veya gerçek QA işlemi gerektirmeden, mevcut kayıtların yanında yeni numaralı kaynak/build/run bağları hazırlandı. Özgün repo hash’leri audit/source-freeze kaydında korunur. Bu ilk aday gerçek koşuya teklif edilmez.


## Güncel aday: yalnız metadata onarımı

[Spec’in yerel metadata onarımı](../../../../_bmad-output/implementation-artifacts/spec-p4b5-devtools-consumer-observation.md) uygulandı. İlk adayın 17 makbuz/build dosyası byte/hash düzeyinde korundu. `local-artifact-rejection-0001.private.json`, ilk adayın `SOURCE_DRIFT` sonucunu yalnız yerel metadata reddi olarak kaydeder. `FAILED_NO_RETRY`, terminal, execution approval veya gerçek claim üretilmedi.

Yeni `source-binding-0002` opsiyonel `repositoryFiles` alanını içermez. Mevcut runtime fallback’i `files` hash’lerini gerçek `source-0001` kökünde doğrular. Özgün repo hash haritası audit ve immutable `source-freeze-0001` içinde kalır; yeni binding bu provenance kaydının SHA’sını taşır. Onaylı audit SHA’sı, 1.166 kaynak dosyası / 8.565.689 byte ve tüm özgün repo/kopya hash’leri onarım öncesi ve sonrasında yeniden eşleşti. Kaynak/runtime değişmedi; yeni kaynak kopyası oluşturulmadı.

Mevcut kopyadan import ve **kök override’ı verilmemiş** `ManualController(selected=P4B5_MANUAL_RETRY)` kullanıldı. `qa.ROOT`, `prior.ROOT` ve controller kökü gerçekten `source-0001` idi. `gate(reviews=False)`, `retry_bindings`, `selected_build` ve `binding_links` geçti. Tam `gate()` yalnız `receipts/execution-approval-01.private.json` yokluğu nedeniyle kapandı; başka bir ret başarı diye değerlendirilmedi.

Aynı varsayılan controller’ın mevcut `Controller.build` yolu yeni `build-0002` üretti: offline Gradle **PASS, 18 saniye**, 20 task çalıştı / 151 task günceldi. Yeni APK ilk APK ile byte/hash olarak aynıdır: **84.273.783 byte**, SHA-256 `c331c42ac10ba445a7835f8db26829539e262ddad3b879be9692e5d34847cbe7`. Yeni seçili yol `ROOT/android/builds/build-0002/consumer.apk` olur. Paket, entry, debug badging, kernel dependency/kimliği ve sınırlı credential pattern taraması tekrar geçti.

Kaynak değişmediği için 197 Flutter / 80 Python / 26 Java skipsiz test ve analyzer logları tekrar çalıştırılmadan aynı kaynağın kanıtı olarak korundu. Yeni classpath/Python bağları ayrı numarayla oluşturuldu; içerikleri önceki bağlarla aynı hash’tedir. Bu metadata onarımına ilişkin yeni yerel kapı kanıtları aşağıdadır.

Java’nın mevcut derlenmiş `validateBindings(source-0001,input)` yöntemi yeni source/copy ve classpath hash’lerini geçti. `validateReviewedRuntime` aynı repoRoot, yeni run/source/build bağlarını ve APK hash’ini geçtikten sonra **eksik gerçek `backend-start.claim.private.json` dosyasında** durdu. Bu kontrol uygulamanın `main`/`run` yöntemini çağırmadı; DB, credential, backend veya gerçek claim açmadı. Java’nın tüm gerçek çalışma kapısının geçtiği iddia edilmez.

İlk yerel Java verifier fixture’ında kendi `runBindingFile`/SHA alanları eksikti; `NUMBERED_RECEIPT_REQUIRED` reddi ayrı `logs/java-bindings-0002.log` dosyasında korundu. Fixture’a gerçek yeni bağın self-link alanları eklendi. Ayrı verifier/log ile kontrol geçti (`logs/java-bindings-0003.log`). Bu fixture düzeltmesi onaylı kaynakları veya runtime’ı değiştirmedi; ilk ret gizlenmedi.

| Güncel makbuz (`ROOT/receipts/`) | SHA-256 |
|---|---|
| `source-binding-0002.private.json` | `f9d1306819a1e8fa69bb13cdccd81fd2a2c06790b9a6e5bb16ba778591b579b6` |
| `classpath-binding-0002.private.json` | `3e41d0face64d4e162e2991e13ff1087b5829f6d91d1213090982fd166cef254` |
| `python-binding-0002.private.json` | `bbd51ab55ea012547353880657b21f8cca987bd3f3863919bb4b11982145c5b4` |
| `selected-build-0002.private.json` | `d7eeb965a70e5a2851847261ae632398783e2f5c7896e5e517b071b04f48e323` |
| `run-binding-0002.private.json` | `cceb45940273fc0b760cc99a8dc9a0c41c733263949e1bc8f27f7d18ad9da7a1` |
| `apk-audit-0002.private.json` | `5c90d2b253d7686b081ee3a3416fcaed27959b7abe7116e584f8b63ea001dac9` |
| `local-preparation-0002.private.json` | `3a210ec47ef5ba130ac2b37fadbc198658eb88d501e71f30347311669ccc146f` |
| `default-root-validation-0001.private.json` | `8570a49b16cb0505af40236abffd722c22355dd89b0b770bf3cd5d115c73d1a1` |
| `local-artifact-rejection-0001.private.json` | `51f26e807ada0c0107bfecc02ee3b4590bd162e5a46da329779ff0d324c9304c` |

Ek ham kanıtlar MAC_EK’te kalır:

| Dosya (`ROOT/`) | Byte | SHA-256 |
|---|---:|---|
| `android/builds/build-0002/build.log` | 11533 | `21faf53975ea301c93f3368ee4e9864f58ad8262a548c934bf19b46ec8485b8d` |
| `logs/java-bindings-0002.log` | 1440 | `1a29f375aec9616dedbffb362b3d5184d56c8b5b0a7f582147c72ad0d58ead46` |
| `logs/java-bindings-0003.log` | 108 | `ba6337baf834ed055d5f8ff3e9a1cd3786cc090438035070e46ba1b5e2b614be` |

Gerçek claim, terminal ve execution approval sayısı **0** olarak kaldı. Yeni aday üç bağımsız kaynak/APK/run incelemesine hazırdır; incelemeler henüz tamamlandı sayılmaz. Ayrı gerçek koşu onayı, Android/DevTools bağlantısı, aynı PID gözlemi, retained teslimat ve drain kabulü hâlâ açıktır.

## Son bağımsız inceleme ve somut teklif

Güncel `source-binding-0002` / `build-0002` / `run-binding-0002` için Blind Hunter, Edge Case Hunter ve Verification Gap incelemeleri bulgu üretmedi. Üç farklı ajanın makbuzları `ROOT/reviews/pre-run-0002-01.private.json`–`03` olarak yazıldı. [İnceleme kaydı](apk-review-2026-10-20.json) bunların SHA’larını tutar. İlk adaya ait önceki okumalar son adayın incelemesi yerine sayılmadı.

Üst ajan da varsayılan kopya controller’ıyla source/retry/build bağlarını bağımsız doğruladı. Tam kapı yalnız eksik gerçek execution approval’da kapandı. Görev dışındaki 3.369 repo dosyasının sıralı yol/SHA zinciri değişmedi: `e68c31f9659ceb31d6066d872ff2a337cac2d1a7a51b7b7c95d9b59f2dd85405`.

[Tek gerçek koşu önerisi](single-run-proposal-2026-10-20.json), SHA-256 `fff01ae4780be54f1d40e9c9c03676f9f72ed84563277cc796137059da987de9`, yalnız bu APK/run’a bağlıdır. Eski sayısal öneri ve bütçe tavanları aynen taşındı. DevTools bağlantısı girişten önce doğrulanır; dört gösterim, Home/dönüş ve kapanış tek 120 saniyeye sığmalıdır. Dönüşte yeni medya/admission açılmaz. Hata terminaldir; otomatik tekrar yoktur. Bu belge veya üç PASS makbuzu gerçek execution approval değildir. Gerçek kurulum/backend/giriş/medya hâlâ sıfırdır; ayrı onay gelmeden sonraki aşamaya geçilmez.
