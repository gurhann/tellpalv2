# P4b5 yeni APK ve tek tekrar: yürütme haritası

2026-10-20. Bu belge [ana spec'in](../spec-p4b5-manual-retry-measurement.md) teknik ekidir. Yalnız plan hazırlanmıştır; uygulama, derleme, cihaz, DB, auth veya bulut işlemi yapılmamıştır.

## Doğrulanan başlangıç

HEAD `3ec09d1e124080d9ffebf5ec4cd098cbd6a177b8`, branch `codex/image-optimization-p3-publication`. Son yerel onarım 197 Flutter / 69 Python testinden ve üç incelemeden geçti; analyzer temizdir. Eski APK bu onarımı içermez. [Onarım raporu](../../../be/docs/evidence/p4b5-manual-consumer-measurement/local-repair-2026-10-20.md) tarihsel kanıttır; yeni kapsamın test veya başlangıç kabulü değildir.

`.memlog.md`, `main_image_experiment.dart` ve local-lossless değişiklikleri görev dışıdır. Önceki üç P4b5 denemesi, özel kaynaklar, APK'lar ve yürütücüler değiştirilemez. Model oturumdan devralınır; override yoktur. Uygulama BMad adımına göre tek ajanla sıralı yapılır; üç incelemeyi üst ajan başlatır.

Yeni sabit kimlik:

- measurementId: `p4b5-manual-retry-af5cbc33-ee49-41ef-aa04-986cb3af292f`.
- Öneri: `be/docs/evidence/p4b5-manual-retry-measurement/bounded-run-proposal-2026-10-20.json`.
- Öneri SHA-256: `a3aab71cd8ae4685888c6d7d8004332bfc5fd9ff6d7ef91decb34e44ecd65fc6`.
- Yeni özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-retry-measurement-20261020-3ec09d1`.
- Paket: `com.tellpal.tellpal_mobile_poc.readingmanualretryqa`.
- Entry: `test_driver/reading_consumer_manual_retry_entry.dart`.

Yeni paket, eski appdata'yı silmeden ayrı başlangıç sağlar. Normal TellPal ve eski QA paketleri kaldırılmaz. Yeni paket beklenmedik biçimde zaten yüklüyse ilk claim'den önce ret verilir; `install -r`, clear-data veya otomatik kaldırma ile geçilmez. Yeni host kökünde eski claim/onay/run kopyalanarak hak üretilmez.

## Dosya ve sembol haritası

Yollar repo köküne göredir. Semboller esas alınır; satırlar mevcut sürümdeki ankordur.

| Yol / sembol | İş |
|---|---|
| `tools/image-optimization/p4b5_qa_scope.py`: `QaScope`, `SCOPES`, `select_scope` | Sabit `P4B5_MANUAL_RETRY`, paket ve entry metadata'sını ekle. Varsayılan P4B5 kalır. Serbest root/paket/ID kabul edilmez. |
| `tools/image-optimization/p4b5_manual_consumer_qa.py`: `ManualController`, `ManualAndroid`, `validate_prior_proof`, receipt/capture validator'ları, `main` | Yalnız iki sabit manual kapsamı instance üzerinden seç. Global SCOPE/PACKAGE mutasyonu veya kopya controller yoktur. Bütün run-as/cmdline/support/install/start/cleanup ve command-prefix selected sabitten gelir. `/proc` onarımı ve sınırlı tanı paylaşılır. |
| `tools/image-optimization/p4b5_consumer_qa.py`: `Controller.__init__`, `copied_source`, `gate`, `build`, VM CLI/arm/baseline | İki manual kapsamda numaralı kaynak, local-only plan onayı ve ayrı execution approval zorunlu olur. VM yolları ikisini de reddeder. Mevcut özel Gradle init seçili entry/paketi üretir; normal Android applicationId dosyası değişmez. |
| `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQaScope.java` | Yeni enum/package metadata'sı; üç geçmişin exact sıra/root/ID/proposal/run bağı. Mevcut legacy sabitler ve default değişmez. |
| Aynı dizinde `P4b5ConsumerQaSetup.java` | Manual-aile sourceDirectory, APK entry/package, ayrı onay, ortak Python controller ve import closure kapılarını genişlet. Mevcut Setup/Ledger/Storage teslimatı yeniden kullanılır; üretim Java/REST/şema değişmez. |
| `app/mobile/test_driver/reading_consumer_qa_probe.dart`: `ConsumerQaExpectedScope`, overflow ve liveRaster kontrolü | Kapalı constructor'lı sabit sınıfa yeni sabit ekle; iki manual kapsam aynı overflow terminali ve gerçek raster-ready koşulunu uygulasın. |
| `app/mobile/test_driver/manual_consumer_qa/{private_protocol,coordinator}.dart` | Protokol instance'ı yalnız iki sabitten birini taşır. Coordinator kapsamı protokolden alır. Nonce/PID/startTicks/lifetime, 10 phase, 4 capture ACK, 0700/0600 ve symlink koruması sürer. |
| `app/mobile/test_driver/reading_consumer_manual_entry.dart`: `bootstrapManualConsumerQa` | Mevcut main eski sabitte kalır. Ortak bootstrap yalnız sabit manual kapsam seçimine açılır. Yeni ince entry bu fonksiyonu yeni sabitle çağırır. Yeni router/Cubit veya kanal yoktur. |
| `tools/image-optimization/test_p4b5_manual_consumer_qa.py` ve ilgili scope/runtime testleri | İki kapsam, üç proof, exact onay ve source/import/entry/package mutantları. Gerçek adapter/cleanup ve önceki onarım mutasyonları korunur. |
| `be/src/test/java/com/tellpal/v2/support/P4b5ConsumerQa{Scope,Setup,Manual}Test.java`, `P4b5ConsumerQaIT.java` | Tam geçerli yeni-scope fixture kapıdan gerçekten geçsin; sentetik PostgreSQL kota/debt ve olumsuz matris. |
| `app/mobile/test/reading_images/manual_consumer_{bootstrap,protocol,qa,router}_test.dart`, `reading_p4b5_qa_probe_test.dart` | Yeni ince entry'den gerçek bootstrap/DI/protokol; eski/yeni scope karışımı, router/paint/ACK/overflow ve native kaynak kanıtı. |

### Kaynak ve kimlik doğrulamasında zorunlu düzeltmeler

Sadece yeni sabit eklemek yeterli değildir. Bütün doğrudan `P4B5_MANUAL` / Dart manual equality dalları iki sabit manual kapsam açısından incelenir. Legacy varsayılanlar ve eski özel yürütücüler korunur.

Python'da yüklenmiş her owned modülün `__file__` yolu doğrudan seçilmiş kopyadaki beklenen dosya olmalıdır. `files` içinde hem eski hem yeni core yollarının bulunması eski importu meşrulaştırmaz. Eski owned import girdisi ve yanlış command-prefix, claim oluşmadan reddedilir. Üçüncü taraf runtime/import closure ve binary hash'leri de korunur.

Java'daki mevcut “valid artifact” testi eksik proof'ta duruyor. Yeni kapsam için eksiksiz source/CP/selected/Python/üç-proof/ayrı-onay fixture'ı gerçek son kapıdan geçmelidir. Ardından sourceDirectory, entry, package, command-prefix, import ve onay alanları tek tek bozulur; ret ve sıfır gerçek işlem doğrulanır. Yeni manual dala eski `source` fallback'i verilmesi olumlu testi bozmalıdır.

Yeni native log prefix'i `P4B5_MANUAL_RETRY_RESOURCE_JSON:` olur. JSON dış zarfı measurementId/proposalSha256/budgetRunId taşır; içerideki gerçek probe/configure/receipt kimliği de aynı olmalıdır. Eski sample'ı yeni prefix veya yalnız yeni dış zarfla etiketlemek kabul edilmez. Mevcut saf resource validator payload için kullanılır; yeni çelişkili kimlik mutantları eklenir.

## Üç eski deneme ve borç

Plan hazırlanırken yalnız yerel dosya hash'leri doğrulandı: ilk 12, ikinci 6, üçüncü 32 dosya; toplam 50 bağlı kanıt. İlk iki geçmiş öneriden aynen taşındı. Üçüncü zincir yeni öneride tek tek listelidir. Önceki kaynak bağları kendi eski özel kopyalarında değerlendirilir; güncel repo ile eski kaynak eşitliği beklenmez. Eski CLI çalıştırılarak proof üretilmez.

Üçüncü denemenin terminali `FAILED_NO_RETRY`, hatası `arm / QA_OWNER_CHANGED` kalır. Bir kurulum ve ilk başlangıç tüketildi; backend/login/window/medya sıfırdır. İlk `cleanup.ok=true`, süreç çıkışı kanıtı değildir. `runtime-summary-0001` ve `runtime-post-cleanup-audit-0001` bunu false olarak tutar; son audit aynı sahipli PID kapatıldıktan sonra `/proc` yokluğunu doğrular. Eksik/bozuk audit veya yalnız ilk cleanup PASS yeni denemeyi açmaz. Tarihsel hazırlık makbuzlarındaki sıfır tüketim, sonraki gerçek harcamayı sıfırlamaz. Backend ledger uydurulmaz.

Aynı budgetRunId, DB, kullanıcı `gurhankucuk`, storage prefix ve kanonik QA UID sürer. Son bilinen V37 borcu 39 işlem / 44.742.970 byte'tır; bu plan güncel DB sorgusu değildir. Gerçek kapıda aynı satır tam eşleşmelidir. Drift, kalan kapasite veya retained LIVE uyuşmazlığında klon, restore, yeni satır veya tavan artışı yoktur.

Yeni ayrıca onaylanan tek deneme, P4b5 kurulum/ilk başlangıç toplamını üçten en çok dörde çıkarır. Eski üç hak iade edilmez. Genel rezervasyon tavanları ve önceki pinned read/byte/auth borcu değişmez.

## Sıralı yerel hazırlık

1. Sabit katalog ve üç proof doğrulamasını uygulayıp Python/Java/Dart kimliklerini yeni öneri SHA'sına bağla. Tekrarlanan helper/worker veya production kodu ekleme.
2. Mevcut düzeltmeleri koruyan testleri tamamla. Yeni kapsamın gerçek bootstrap, router/ACK ve native JSON'uyla Python doğrulaması çalışsın. Eski native sample yeni kapsamın kanıtı değildir.
3. Bütün kaynak değişiklikleri ve yerel testler bitince hash'leri dondur. Yalnız gerekli credential'sız kaynak/test/build kopyasını yeni MAC_EK köküne al. Token, servis hesabı veya API yapılandırması kaynak/APK'ya girmez.
4. Kopyalama exit_code=0 → `flutter pub get --offline` → package_config self-root kontrolü → kaynak bağlama → mevcut Gradle init ile offline debug APK sırasını uygula. Bağımlı adımlar eşzamanlı çalışmaz. Build sırasında kaynak değiştirilmez; kısmi/drift çıktıları korunur ve seçilmez.
5. APK badging/entry/paket/SHA, numaralı kaynak closure, Java classpath/binary, Python binary/owned import closure ve komut önekini aynı run kaydına bağla. Üretilen Android girdileri eski source dizinlerine işaret edemez. Mevcut SDK/cache kullanılır; yeni paket/runtime kurulmaz. Normal Gradle dosyaları ve release imzası değişmez.
6. Aynı son kaynak/APK/run için üç bağımsız inceleme tamamla. Açık bulgu yoksa somut gerçek koşu teklifini kullanıcıya sun ve dur. Plan onayı gerçek kurulum, backend, giriş veya pencereyi açmaz. Exact onay şeması test edilir; insan açıklaması bu makbuza ekstra alan olarak eklenmez.

## Ayrı onaydan sonraki tek deneme

1. Kullanıcı hazır olsun; mevcut emulator-5554 ve aynı geometri/DPR/font scale doğrulansın. Yeni AVD veya ayar değişimi yapılmaz. Süreçler PID/komut/aidiyetle tanımlanır; yeni paket yokluğu ve yerel kapılar doğrulanır. Başka süreç durdurulmaz.
2. Bir kurulum, kurulu APK SHA kontrolü ve bir ilk başlangıç. Kurulum en çok 120 saniye, gerçek ağsız baseline en çok 30 saniye. Boş keşif süreç ölümü değildir; paket/startTicks pozitif eşleşmesi gerekir. Baseline yoksa sabit sırsız bootstrap aşamaları toplanıp terminal kapanır. Credential ve API açılmaz.
3. Aynı PID/startTicks/lifetime açık kalır. Başta kapat/restart provası yoktur. Son kapanış sonunda doğrulanır. Backend reservation sonrası `Popen.pid` alınır alınmaz `record-backend` yapılır; local-ready beklenerek geciktirilmez. Bağlı Java argv tam `-Xmx1g`, bağlı classpath sırası ve sabit scope/state argümanlarıdır. `TELLPAL_DB_USERNAME=gurhankucuk`, mevcut yerel boş parola sözleşmesi korunur. Backend en çok 180 saniye; bütün yerel hazırlık 300 saniyedir.
4. Güncel DB borcu, #94/tr retained LIVE generation 4, revision/digest ve sayfa 1427 → 1428 doğrulanır. Yerel kapılar geçmeden giriş yoktur. Mevcut servis hesabı bu hazırlıkta SDK tarafından okunabilir; daha erken hiç okunmadığı iddia edilmez.
5. En çok bir lookup/exchange/verification/giriş: mevcut 10 saniye + 2 saniye cleanup; token en az 180 saniye geçerli. Kullanıcı hazır olmadan giriş/pencere yoktur. Son DB/admission ve credential kapısından sonra tek 120 saniye açılır; özel configure stdin ile taşınır. İnsan gecikmesi pencereye dahildir; uzatma/duraklatma yoktur.
6. Kullanıcı yönergelerle Başlat → detail → sayfa 1 → sayfa 2 → detail → Home → aynı PID'ye dönüş → QA kapat sırasını izler. Her paint için mevcut host screenshot/ACK bağı gerekir. Resume yeni admission değildir. Mevcut seçici iki fiziksel eksenin ihtiyacını karşılayan en küçük doğrulanmış varyantı kullanır. Bekleyen iş bırakılmadan başka görünür iş açılmaz; original fallback ve ek prefetch yoktur.
7. On phase ve dört ACK, UI/frame, payload, seçilen rendition, gerçek widget alanı/DPR/fit/decode boyutu ile uzlaştırılır. Kapanış/stop credential, byte/slot ve native buffer/codec/image sayaçlarını sıfırlar. QA süreci yalnız sahiplik kanıtıyla durdurulur; boş pidof tek başına exit PASS değildir. Sonuç ne olursa olsun terminal, temizlik, üç son inceleme ve görevle sınırlı yerel commit hazırlanır. Yeni deneme otomatik açılmaz.

## Sayısal uzlaştırma ve kabul sınırı

Tek pencere 120000 ms; 1 manifest, status s∈[0,12], dört medya [1,2,3,1], her biri 3292721 byte. HTTP=5+s≤17; login dahil verifier=10+s≤22; ek delivery=13+s≤25; son V37 işlem=52+s≤64. Verifier ve delivery üst üste toplanmaz. En kötü son rezervasyon 57913854 byte, mevcut 67108864 tavanının altındadır. Diğer kaynak/tepe/decode/transfer/lease sınırları öneride eskiyle byte eşitliğindedir; üretim ayarı değildir.

Eksik çizim, süre/kimlik/manifest ihlali, tanı var ama baseline yokluğu, cleanup hatası veya belirsiz süreç durumu başarı sayılmaz. Auth/abonelik/uygunluk, normal Library/detail/reader, ses ve cache değiştirilmez. Erişim, URL yenileme/expiry, HTTP cache, hesap değişimi/session/retention, üretim profili ve normal tüketici geçişi açık kalır. Fiziksel Android/iPhone, JPEG/WebP, P0 ham kanıt ve RSS/GPU zirvesi bu emülatör denemesiyle kapanmaz. Android 1× ve bazı iPhone 0,75× gözlemleri genel üretim onayı değildir. Yeni üretim/backfill/silme/dağıtım veya fatura sorgusu yoktur.

## Doğrulama kapıları

Komutlar gelecekteki uygulama içindir; plan hazırlanırken çalıştırılmadı.

- `python3 -m unittest discover -s tools/image-optimization -p 'test_p4b5*.py'`: gerekli yeni native JSON env girdisiyle skipsiz.
- Flutter kopyasında `flutter pub get --offline`, `flutter test --no-pub test/reading_images`, `flutter analyze --no-pub`: yeni ve eski kapsamlar geçer. Anotasyon değişirse generator bir kez; değişmezse üretilen dosyalar korunur.
- `be/` wrapper ve `run-maven-tests` skill'iyle ilgili `P4b5ConsumerQa*Test` / `P4b5ConsumerQaIT`: tam olumlu kapı, üç-proof ret matrisi ve sentetik PostgreSQL/V37 kota testi. Gerçek QA DB testi/klonu yoktur.
- Eksik/fazla onay alanı, eski scope/root/source/import/entry/package, eksik audit, yanlış lifecycle tüketimi, bozuk yeni native zarf/inner kimlik ve APK drift retleri. Gerçek claim/API çağrısı sıfır kalır.
- `/proc` pozitif kontrolü kaldırma ve boş pidof'u exit sayma mutasyonları; yeni entry'nin initialize çağrısını kaldırma testi. Native payload yalnız prefixle yeniden etiketlenince test kırılmalıdır.

Kaynak/APK/run raporu ve gerçek deneme raporu `be/docs/evidence/p4b5-manual-retry-measurement/` altında sırsız özet olur. Ham dosyalar MAC_EK'te kalır; repo içine kopyalama yoktur.
