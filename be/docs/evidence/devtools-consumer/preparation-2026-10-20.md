# DevTools consumer yerel hazırlık kaydı

2026-10-20. Yalnız [spec'in geçerli uygulama devri](../../../../_bmad-output/implementation-artifacts/spec-p4b5-devtools-consumer-observation.md) kapsamındaki yerel test hazırlığı tamamlandı. Gerçek koşu yapılmadı. İlk görevin iki test açığı kapatıldı; kopya/APK/run ve gerçek gözlem görevleri açıktır.

## Yerel değişiklik

`tools/image-optimization/test_p4b5_manual_consumer_qa.py` dosyasına iki test ve bunların sentetik fixture'ları eklendi. Önceki 60.403 byte aynen korunur. Bu önekin SHA-256 değeri `34e05b4a7497092c33fc55488e03ac045a0a79d7c0401b2636f57935419d9c85` olup eski payload audit'iyle eşleşir. Ek 13.252 byte sonrası dosya 73.655 byte'tır; SHA-256: `1c3b7eeca5c947c25d82b8811c3722a822b7f29b7eae05b43352be0f80faba92`.

- Gerçek `ManualController.run` her iki sabit manual kapsamla çağrılır. Altı pencere durumu, toplam 12 alt senaryodur: normal, başlangıç sınırı ve bitişten hemen önce kabul edilir; başlangıçtan önce, bitişte ve bitişten sonra kabul dosyası oluşmaz. Teslim edilen configuration, seçili measurement/proposal/budget kimliği, on receipt'in ham byte/hash bağı, dört screenshot/ACK ve kabul dosyası doğrulanır. Kaynak validator'ları ve runtime configuration üretimi gerçektir; cihaz/backend/giriş kapıları sentetiktir. Global `SCOPE` değiştirilmez.
- Gerçek `ManualController.cleanup` ve `ManualAndroid`, iki sabit kapsam için 14 transport durumunda çalışır. Toplam 28 alt senaryo; boş `pidof`, aynı sahip, açık `/proc` yokluğu, PID/start/komut değişimi, okuma hatası, çelişki, kapanmayan veya yeniden kullanılan PID ve süre aşımını kapsar. Stop yalnız seçili pakete gider. Stop öncesi cmdline/stat ve sonrası `/proc` kontrol edilir. Belirsiz veya değişmiş sahip durdurulmaz; çıkış doğrulanmadan PASS üretilmez. Cleanup/terminal kimliği ve sır içermeyen sınırlı tanı da doğrulanır.

Runtime, bootstrap, router, probe, Java ve Dart kaynakları değiştirilmedi. Spec, eski kanıtlar, eski payload manifestleri ve `.memlog.md` korundu. Runtime değişikliği veya plan değişikliği gerekmedi.

## Bu tur doğrulama

Yeni iki hedef test **PASS**; 12 run ve 28 cleanup alt senaryosu geçti. Tam Python P4b5 paketi **80 test, 0 skip, PASS**:

```sh
env PYTHONDONTWRITEBYTECODE=1 \
  P4B5_DART_JSON_LOG=/private/tmp/p4b5-flutter-tests.log \
  P4B5_RETRY_DART_JSON_LOG=/private/tmp/p4b5-flutter-tests.log \
  P4B5_MANUAL_DART_JSON_LOG=/private/tmp/p4b5-flutter-tests.log \
  P4B5_MANUAL_RETRY_DART_JSON_LOG=/private/tmp/p4b5-flutter-tests.log \
  python3 -m unittest discover -s tools/image-optimization -p 'test_p4b5*.py'
```

Gerekli dört JSON girdisi, eski [local-validation kaydının](../p4b5-manual-retry-measurement/local-validation-2026-10-20.json) gösterdiği mevcut Flutter logundan salt okunur alındı. Log SHA-256 değeri kayıtla eşleşir: `f221020e9fde4fdbd167561596a854fed6fdd2189c00eb9fd55003aa38d9e577`. Bu tur yeni native ölçüm üretilmedi.

İlk tam koşuda sandbox, mevcut testin kendisinin oluşturduğu sentetik Python alt sürecini inceleyen `ps` çağrısını `EPERM` ile engelledi. Aynı yerel test komutu otomatik onaylı sandbox yükseltmesiyle geçti. Bu test gerçek Java/backend/SDK veya QA süreci çalıştırmaz. İlk başarısız koşu başarı sayılmadı.

Yeni testlerin anlamlılığı, runtime dosyalarına yazmadan bellekte üç regresyon mutantıyla sınandı. Eski kapsamla configuration üretme mutantı 3 failure/3 error; eski pakete stop gönderme mutantı 5 failure; kapanıştaki `/proc` doğrulamasını kaldırma mutantı 10 failure verdi. Üçü de yakalandı; skip yoktur. `git diff --check` geçti.

Ham loglar repoya alınmadı:

| Yerel log | SHA-256 |
|---|---|
| `/private/tmp/devtools-consumer-python-2026-10-20-approved.log` | `81caf11ba97e58e3409a4c29150e8949116adfda341b0dded0233350818e2201` |
| `/private/tmp/devtools-consumer-mutations-2026-10-20.log` | `84805fc3ffcd3fe0a65c1add576cf31d345c1103d1a7683b34b39f8342f06f47` |

## Tarihsel kanıt ve açık bağlar

Flutter testleri, analyzer ve Java testleri bu tur yeniden çalıştırılmadı. Eski kayıttaki 232 Flutter testi/0 skip, analyzer 0 bulgu ve 26 Java testi/0 skip tarihsel kanıttır. Mevcut analyzer ve Java loglarının SHA-256 değerleri de eski kayıtla eşleşir: sırasıyla `5c4e3548fc33425998055b05a3f91a6bb70a77c3e77ceca79487fac1643d6c4f` ve `3ab28e3127fb25c1af7972fa66d1187254a16ea45f28316d8132af0a3f03026c`. Bunlar yeni kaynak/APK kabulü veya gerçek medya teslimatı değildir.

Nihai kopya listesi/hedefinin açık onayı, yeni kaynak bağı, APK, run bağı ve üç kaynak/APK ön incelemesi bekliyor. Eski 1166 dosyalık payload manifesti bu ek testi içermez; güncel kopya onayı yerine kullanılamaz. Bu tur yeni kaynak kopyası, bağımlılık indirmesi, APK/build veya run/koşu onay kaydı oluşturulmadı. Commit ve push yapılmadı.

`p4b5-manual-retry-af5cbc33-ee49-41ef-aa04-986cb3af292f` için bu tur gerçek kurulum, ilk başlangıç, backend başlangıcı, giriş, ölçüm penceresi ve medya çağrıları **0**'dır. Tarihsel tüketimler iade edilmedi. Retained #94/tr LIVE generation 4/revision/digest, 1427→1428 sırası, QA UID, DB/V37 borcu ve önerideki bütün kaynak/genel tavanlar değişmez. Tek 120 saniye, 1 manifest, 12 status, 4 medya/13.170.884 byte ve 17 HTTP sınırı korunur.

Ayrı gerçek koşu onayı olmadan DevTools bağlantısı, giriş ve pencere açılmayacak. İleride yalnız ek DevTools çıktıları mevcut MAC_EK kökünde `devtools-0001/` altında tutulacak; receipt/screenshot/ACK/cleanup yolları değişmeyecek. Bu hazırlık Network/Memory/four-paint uzlaştırması veya drain ölçümü değildir. Network'ün native trafik kapsamı, payload/TLS ayrımı, heap/external/RSS ayrımı, peak/GPU/frame eksikleri ve debug/emülatör sınırı gerçek koşu raporunda açık tutulmalıdır. P4b ve spec'teki ertelenmiş kapsamlar kapanmadı.

## Üst ajan doğrulaması ve nihai kopya teklifi

İlk başarısız test logu da korundu. Üç ham logun toplam 10.685 byte kopyası MAC_EK’te `p4b5-manual-retry-measurement-20261020-3ec09d1/devtools-0001/local-preparation-0001/` altına alındı. Kök `/Volumes/MAC_EK/tellpal-qa-private/` olarak kalır. Dizinler `0700`, dosyalar `0600`; geçici asıllar silinmedi. Bu dizin yalnız yerel test çıktısıdır; kaynak kopyası veya çalışma claim’i değildir. Üç dosyanın boyut/hash manifesti `manifest.json`, SHA-256: `a613f423af6debdf7623fb35fc3f72dec767a453fc60a5a3c8853f6509493c38`.

Hedef test/spec/yeni evidence klasörü dışındaki **3.368 dosyanın** sıralı yol/SHA zinciri başlangıç ve son kontrolde eşleşti: `6a0f96304f0a23e5a819bda75f56d7241b47262b8462215c4ed9268475bf7923`. `.memlog.md`, ilgisiz çalışma ve eski kanıtlar bu kapsamdadır.

Yeni [kopya listesi](copy-payload-audit-2026-10-20.json) **1.166 dosya / 8.565.689 byte** içerir. SHA-256: `7c216bde7d93947dbc385520c96668be6a4ef0061bcc6ac8fe8861cc40effaa9`. Eski audit değiştirilmedi. Bütün kaynak hash’leri yeniden eşleşti; tek fark 13.252 byte’lık test eklemesidir. Görev dışı `main_image_experiment.dart` için eski audit’teki commit sürümü kopya adayı olarak korunur; dirty dosyaya dokunulmaz. Credential/token/Firebase API yapılandırması alınmaz.

Önerilen kaynak hedefi değişmedi: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-retry-measurement-20261020-3ec09d1/source-0001`. Kaynak kopyası henüz yoktur; yerel hazırlık log dizininin varlığı kopya izni sayılmaz. Bu tam liste/hedef için açık onaydan sonra offline APK hazırlığı ve kaynak/APK incelemeleri yapılabilir. Gerçek kurulum/backend/giriş/medya ve ölçüm ayrıca onay bekler. Önceki otomatik onay incelemesi eski byte listesini güncel listeye yeterli görmediği için somut liste sunulur; eski retler ve haklar değişmez.

## Yerel kaynak incelemeleri

BMad code-review dört katmanla tamamlandı. Blind Hunter, Edge Case Hunter, Verification Gap ve Acceptance Auditor bu turdaki değişiklikte bulgu bildirmedi. [İnceleme kaydı](local-review-2026-10-20.json) ve [yalnız bu tur eklenen testlerin diff’i](local-preparation-review.diff) saklandı. Diff SHA-256: `3eead8d6f1da8c0df564ff5d401ba534e3f030ccfc4c3d5c93630f8ad720e6bb`.

Bu, yerel test hazırlığının incelemesidir. Son kaynak/APK/run için gereken üç koşu öncesi inceleme yapılmış sayılmaz. Tüm spec tamamlanmadığı için `in-progress` kalır; kaynak kopyası ve gerçek koşu açılmadı. Bu aşamada commit/push yoktur.

## Sonraki onaylı aşama

Kullanıcının tam liste/hedef onayından sonra kaynak kopyası ve offline APK hazırlığı tamamlandı. [Güncel APK raporu](apk-preparation-2026-10-20.md) ilk metadata reddini, onarılmış ikinci adayı ve son üç bağımsız artifact incelemesini ayırır. [Somut gerçek koşu önerisi](single-run-proposal-2026-10-20.json) ayrı onay bekler; bu önceki yerel hazırlık kaydı gerçek koşu kanıtına çevrilmez.
