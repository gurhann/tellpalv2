---
title: 'P4b5 — DevTools ile gerçek QA detail/reader gözlemi'
type: 'chore'
created: '2026-10-20'
status: 'in-progress'
execution_status: 'closed_by_user'
execution_closed: '2026-10-20'
runtime_acceptance: 'not_verified'
baseline_commit: 'b155a2aa060931c1e268c7b9c14314d23c7fd725'
review_loop_iteration: 0
context: ['{project-root}/AGENTS.md']
---

<frozen-after-approval reason="Somut plan onayında sabitlenir">

## Amaç

Ağsız 6/6 gerçek teslimatı kanıtlamadı. Mevcut #94 PNG’lerini ayrı QA detail/reader rotalarında DevTools Network/Memory ve mevcut probe ile gözle. Normal tüketici geçişi yoktur.

## Sınırlar

**Daima:** [Sayısal önerinin](../../be/docs/evidence/p4b5-manual-retry-measurement/bounded-run-proposal-2026-10-20.json) kimlikleri, #94/tr retained LIVE generation 4/revision/digest, sayfa 1427→1428, QA UID, DB/V37 borcu ve bütün kaynak/genel tavanları aynen korunur. Tek 120 saniye; en çok 1 manifest, 12 status, 4 medya/13.170.884 byte, 17 HTTP. Birer kurulum, ilk başlangıç, backend başlangıcı ve giriş; eski tüketimler iade edilmez.

**Önce onay:** Plan onayı yalnız yerel test/inceleme içindir. Nihai liste/hedef için kopya/APK hazırlığı, bağlı APK/run ve üç ön inceleme sonrası gerçek koşu ayrıca onaylanır.

**Asla:** Üretim/upload, backfill, silme, DB sıfırlama/klon, limit artışı, otomatik tekrar, original fallback, fiziksel cihaz veya dağıtım yoktur. Yeni transport/IPC/retry/VM protokolü yoktur. `.memlog.md`, ilgisiz değişiklikler ve eski kanıtlar korunur; tanımsız süreç durdurulmaz.

| Durum | Beklenen |
|---|---|
| Bağlantı/yerel kapı eksik | Giriş ve pencere açılmaz |
| Retained kimlik, geometri veya bütçe uyuşmaz | Dur; kaynak veya profil değiştirme |
| Home ve aynı PID’ye dönüş | Kaynak bırakma; yeni admission/medya yok |
| Hata, süre dolumu veya kayıt kaybı | Başarı üretme; mevcut tüketimle kapat |

</frozen-after-approval>

## Kod haritası

- `app/mobile/test_driver/reading_consumer_manual_retry_entry.dart`: ayrı debug giriş. Aynı dizindeki `manual_consumer_qa/coordinator.dart` dört paint/ACK ve drain’i korur; `reading_consumer_qa_probe.dart` payload/geometri/kaynak kayıtlarını verir.
- `tools/image-optimization/p4b5_manual_consumer_qa.py`: `ManualController.run` screenshot/ACK toplar; gezinmeyi insan yapar. [Eski teknik ekin](image-optimization/p4b5-manual-retry-measurement.md) kapsam ve kaynak/APK/borç bağları salt okunur kalır.

## Görevler ve kabul

- [x] `tools/image-optimization/test_p4b5_manual_consumer_qa.py`: eski iki açık `run` configuration/receipt ve cleanup `/proc` bulgusunu iki kapsam için kapat. Runtime değişikliği gerekirse planı güncelle.
- [x] `be/docs/evidence/devtools-consumer/preparation-2026-10-20.md`: test, nihai kopya teklifi, APK/run bağları. Yalnız ek DevTools çıktıları mevcut MAC_EK kökünde `devtools-0001/` altında kalır; receipt/screenshot/ACK/cleanup yolları değişmez. Sırlar repoya girmez.
- [ ] Ayrı koşu onayıyla mevcut emülatörde aynı PID/entry/debug build’i doğrula; DevTools’u girişten önce bağla. Hazırlık/baseline süreleri değişmez. Sonra tek pencere: Başlat → detail → sayfa 1 → sayfa 2 → detail → Home → aynı uygulama → QA kapat. İnsan düğmeleri, host ekran/ACK’yi yönetir. Dönüşte beşinci medya yoktur.
- [ ] `be/docs/evidence/devtools-consumer/verification.md`: Network, dört ekran, probe ve sunucu sayımlarını uzlaştır. Üç son inceleme ve görevle sınırlı yerel commit; push yok.

**Kabul:**
- Verilen koşu onayı yokluğu; hazırlık yapılınca gerçek kurulum/başlangıç/giriş/medya sıfırdır.
- Verilen onaylı koşu; dört çizim oluşunca rendition/rol/dil/revision, iki eksenli ihtiyaç, decode ve gerçek payload aynı kanıta bağlanır.
- Verilen kapanış; drain tamamlanınca dört native active sayacı ve reserved slot/byte sıfırdır; lifetime/attempt geçmişi korunur.

## Tasarım ve doğrulama

DevTools gözlemcidir; kota, integrity ve cleanup kanıtı mevcut hattandır. Memory başlangıç/geçiş/kapanışı aynı PID’de izler; pencere uzatılmaz. Network tüm native trafiği kanıtlamaz. Payload TLS/header toplamı değildir. Heap/external/RSS toplanmaz; boşluk, peak/GPU ve frame ölçüm eksikleri yazılır. RSS düşüşü drain değildir. VM adresi/token/header özel kalır. Debug/emülatör fiziksel performans değildir; GC/heap dump/hot reload yoktur.

Bootstrap/router/probe, Flutter analyzer, Python P4b5 ve ilgili Java testleri skipsiz geçer; kaynak/APK için üç bağımsız inceleme yapılır. Plan hazırlanırken test/ölçüm yapılmadı; sonraki yetkili hazırlık sonuçları aşağıdadır. Model oturumdan devralınır.

Devam eden transfer iptali/öncelik/eşzamanlılık, hesap değişimi, URL yenileme, cache/precache, session/retention, Library/ses, üretim profili ve fiziksel cihaz kabulü açık kalır. QA `private,no-store`/owned raster istisnası ve P0 ham kanıt/kalite sınırları korunur; P4b kapanmaz.

## Spec değişiklik kaydı

2026-10-20: Kullanıcı yalnız sade plan istedi. Eski manual-retry kaynakları ve başarısızlıkları değişmedi; yeni gerçek işlem açılmadı.

2026-10-20: Kullanıcı “A — Planı onayla, yerel hazırlığa geç” yanıtını verdi. Frozen amaç/sınırlar aynen korundu. Model oturumdan devralınır.

## Geçerli uygulama devri

Bu BMad step-03 devrinde yalnız ilk görev ve preparation raporunun yerel hazırlık bölümü yetkilidir. Gelecek kopya/APK, gerçek kurulum/başlangıç/backend/DB/giriş/medya ve koşu görevleri açık kalır; bunları tamamlanmış işaretleme. Workflow'u yeniden başlatma. Önceki dirty dosyalar ve eski rapor/spec/öneriler salt okunurdur; yalnız hedef test dosyasında mevcut değişiklikleri koruyan ekleme ve yeni preparation raporu yapılır. Anlamlı yerel testleri çalıştır; mevcut Flutter/Java kanıtlarını yeniden çalıştırılmadıysa tarihsel diye ayır. Yeni kaynak kopyası/bağımlılık indirmesi yoktur. Gerekli mevcut test girdilerini local-validation kaydındaki yollardan salt okunur bul; yoksa skip'i başarı sayma ve raporla. Sonuçları ve kaynak farkını üst ajana bildir; commit atma.

## Yerel hazırlık sonucu

2026-10-20: İki test açığı kapandı; 80 Python testi/0 skip ve üç yakalanan regresyon mutantı. Dört kaynak inceleme katmanı bulgu üretmedi. [Hazırlık raporu](../../be/docs/evidence/devtools-consumer/preparation-2026-10-20.md), [inceleme kaydı](../../be/docs/evidence/devtools-consumer/local-review-2026-10-20.json) ve [nihai kopya önerisi](../../be/docs/evidence/devtools-consumer/copy-payload-audit-2026-10-20.json) hazırdır. Liste 1166 dosya/8.565.689 byte’tır; kopya ve offline APK hazırlığı ayrı onay bekler. Gerçek QA işlemi yoktur. Sonraki görevler ve `in-progress` durumu korunur; bu incelemeler gelecekteki APK/run ön incelemesi yerine geçmez. Sprint story anahtarı yoktur; sprint kaydı değiştirilmedi.

## Güncel devir: onaylı kopya ve offline APK

Kullanıcı “A — Bu listeyi kopyala, offline APK hazırlığına geç” yanıtını verdi. Bu bölüm önceki yalnız-test devrinin sonraki yetkili aşamasıdır. [Nihai audit](../../be/docs/evidence/devtools-consumer/copy-payload-audit-2026-10-20.json), SHA-256 `7c216bde7d93947dbc385520c96668be6a4ef0061bcc6ac8fe8861cc40effaa9`, 1166 dosya/8.565.689 byte aynen onaylıdır. Hedef `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-retry-measurement-20261020-3ec09d1/source-0001`. Repo kaynakları ve eski audit/öneri/spec/kanıt değiştirilmez. Kopyada `copyFromBaselineCommit` satırını kullan; dirty experiment dosyasını alma. Yeni byte/liste farkı olursa kopyalama yapma ve raporla.

BMad step-03 devri: AGENTS.md ve Flutter skill’lerini uygula; workflow’u yeniden başlatma. Yalnız mevcut SDK/cache ile sıralı kopya → offline pub → self-root/source binding → test/analyzer → offline debug APK → paket/entry/byte/SHA ve Java/Python/run bağlarını hazırla. Eski teknik ek ile mevcut `Controller.build`/Gradle init yolunu kullan; uygulama veya runtime kodu ekleme/değiştirme. Kök zaten yalnız bu tur hazırlanan `devtools-0001/` log klasörünü içerir; bunu koru. `source-0001` ve yeni makbuzlarda exclusive creation uygula; eski prepare script’inin root-yokluğu varsayımını çalıştırma. Token, servis hesabı veya Firebase API yapılandırması alınmaz. Oluşan local.properties yalnız mevcut SDK yollarını taşır; yapılandırma veya bağımlılık indirilmez. Log/build/kanıt MAC_EK’te kalır; yeni rapor `be/docs/evidence/devtools-consumer/apk-preparation-2026-10-20.md` olur.

Gerekli native JSON’u bu kopyadaki Flutter testinden üret; eski logu yeni kanıt diye sunma. Python testleri, Flutter test/analyzer ve gereken Java compile/yerel test kapıları geçsin. Maven çalışması için `run-maven-tests` skill’ini oku; gerçek QA DB veya backend başlatma. Sentetik DB gerekecekse önce salt yerel kapsamı değerlendir; gerçek DB klonu yoktur. Mevcut araç/operational-tools yolu eski teknik ektedir; kimlik bilgisi okumaz. Eksik offline bağımlılık veya build hatasında kaydı koru; otomatik indirme/gerçek deneme yoktur.

Bu aşama QA kurulum/başlangıç, ADB cihaz işlemi, backend başlangıcı, giriş, medya veya gerçek ölçüm açmaz. Execution-approval ve gerçek claim üretme. Run-binding’in yalnız hazır artifact kimliği olması yeterlidir; gerçek koşu onayının yokluğunda execution gate’in kapalı kaldığını doğrula. Build/kopya başarısı DevTools bağlantısı veya P4b kabulü değildir. Son kaynak/APK/run için üst ajan üç bağımsız incelemeyi başlatır; uygulama ajanı commit atmadan sonucu ve kalan açık kapıları bildirir. Sonraki gerçek koşu ayrı somut onay bekler. Frozen amaç ve sayısal sınırlar değişmez; model oturumdan devralınır.

### İlk adayın yerel metadata onarımı

İlk adayın gerçek kopya CLI varsayılan kökü `source-0001` iken source-binding `repositoryFiles` özgün dirty experiment hash’ini içerdiğinden `SOURCE_DRIFT` verir. Elle `root=repo` kontrolü bağlı CLI kanıtı değildir. İlk binding/build/run/logları değişmez; yerel ret ayrı kaydedilir, gerçek başarısız deneme/terminal/claim üretilmez. Onaylı 1166 kaynak byte’ı ve audit SHA değişmez.

Mevcut opsiyonel `repositoryFiles` alanını içermeyen yeni numaralı source-binding hazırla; gate mevcut fallback ile kendi gerçek kopya kökünün `files` değerlerini doğrulasın. Özgün repo haritası source-freeze/audit’te değişmeden korunur; bağımsız özgün repo ve kopya hash kontrolleri yeniden geçsin. Runtime koduna veya onaylı kaynaklara dokunma. Bu metadata değişikliği için mevcut `Controller.build` ile yeni numaralı build ve yeni seçili/classpath/Python/run bağları hazırlanır. Varsayılan kopya import ve `ManualController(selected=...)` ile gate(false), retry_bindings, selected_build ve Java repoRoot/copy-source bağlarını doğrula; elle kök override’ıyla başarı iddiası üretme. Yeni son aday üç incelemeye gider; gerçek gate yalnız execution approval eksikliğinde kapanmalıdır. Mevcut kopya ve offline APK hazırlığı onayı sürer; yeni kopya, gerçek işlem veya limit değişikliği yoktur.

## Offline APK aşaması sonucu

Onaylı kopya ve testler tamamlandı: 197 Flutter, 80 Python, 26 Java testi/0 skip; analyzer temiz. İlk metadata adayı yerel olarak reddedildi ve korundu. Kaynak/runtime değişmeden `source-binding-0002` ve `build-0002`/`run-binding-0002` hazırlandı. APK SHA `c331c42ac10ba445a7835f8db26829539e262ddad3b879be9692e5d34847cbe7`; run SHA `cceb45940273fc0b760cc99a8dc9a0c41c733263949e1bc8f27f7d18ad9da7a1`. Gerçek kopya varsayılan köküyle kontrol geçti; üç son bağımsız artifact incelemesi bulgu üretmedi. [APK raporu](../../be/docs/evidence/devtools-consumer/apk-preparation-2026-10-20.md), [inceleme](../../be/docs/evidence/devtools-consumer/apk-review-2026-10-20.json), [tek gerçek koşu önerisi](../../be/docs/evidence/devtools-consumer/single-run-proposal-2026-10-20.json) hazırdır. Öneri SHA `fff01ae4780be54f1d40e9c9c03676f9f72ed84563277cc796137059da987de9`. Gerçek execution approval, claim ve QA işlemi yoktur. Son iki görev ayrı gerçek koşu onayı bekler; `in-progress` korunur.


## Güncel devir: tek gerçek koşu onaylandı

Kullanıcı 2026-10-20 tarihinde “A — Bu APK/run ile tek sınırlı ölçümü onayla” yanıtıyla `single-run-proposal-2026-10-20.json` SHA `fff01ae4780be54f1d40e9c9c03676f9f72ed84563277cc796137059da987de9` teklifini onayladı. Bu devir önceki yalnız hazırlık kısıtının sonraki yetkili aşamasıdır. `build-0002` APK SHA `c331c42ac10ba445a7835f8db26829539e262ddad3b879be9692e5d34847cbe7`, run-binding SHA `cceb45940273fc0b760cc99a8dc9a0c41c733263949e1bc8f27f7d18ad9da7a1` ve teklifteki bütün sınırlar aynen geçerlidir. Yeni kaynak, APK veya limit değişikliği yoktur.

BMad uygulama ajanı önce salt okunur hazırlık yapar: somut teklif/artifact/review hash’leri, kopya varsayılan kökü, mevcut SDK/ADB ve `emulator-5554`, paket yokluğu, gerekli eski/protected/input dosyaları ve backend yerel hazırlık girdileri doğrulanır. Sırların değerlerini çıktıya veya repoya yazma. Eksik emülatörü başlatma; eksik koşulda claim tüketmeden bulguyu üst ajana bildir. Backend için önceki girdileri ve koruma makbuzlarını salt okunur incele; belirsiz dosyayı varsayımla üretme.

Bu ilk devirde yalnız hazırlık ve gerekirse onayın özel makbuzunu oluştur. Kurulum/ilk başlangıç/backend/giriş/pencere henüz başlatma; hazırlık sonucunu üst ajana bildir ve bekle. Üst ajan DevTools ve insan düğmeleri koordinasyonunu üstlenir; hazırlık geçince aynı uygulama ajanına sırayla arm ve backend/host toplama görevlerini verir. DevTools bağlantısı girişten önce doğrulanır. İnsan hazır olmadan credential/pencere açılmaz. Hata halinde otomatik tekrar yoktur. Ham kayıtlar mevcut MAC_EK kökünde kalır; son rapor, üç inceleme ve görevle sınırlı yerel commit onaylıdır. Frozen bölüm, eski kanıtlar, `.memlog.md` ve ilgisiz dirty dosyalar korunur. Model oturumdan devralınır; commit öncesi üst ajana bildir.


## Tek gerçek denemenin sonucu

2026-10-20: Onaylı tek arm, 16,886 saniyede `QA_PROCESS_COMMAND_CHANGED` ile terminal `FAILED_NO_RETRY` oldu. Bir kurulum ve ilk başlangıç tüketildi; backend/giriş/pencere/measurement ve QA medya aşamaları açılmadı. Yeni consumer DevTools bağlantısı ve baseline kabulü yoktur. İlk cleanup başarısız kaydı değişmedi. Aynı PID/başlangıç tick’i ve paket sonradan tekrar olumlu doğrulandı; yalnız sahipliği doğrulanan QA süreci mevcut adapter ile kapatıldı ve `/proc` yokluğu doğrulandı. Ek cleanup PASS, ilk sonucu değiştirmez.

Dart logcat ayrıca `directories failed → failureMarker unavailable` gösterdi. Host cmdline reddi ile bu uygulama bulgusu arasında kesin nedensellik kurulmadı. Salt okunur alias tanısı aynı dizin kimliğini gösterdi; Dart’ın gerçek support/resolve sonucu olmadığı için kök neden açık kaldı. Kaynak/APK/binding değiştirilmedi; tekrar yapılmadı. [Sonuç raporu](../../be/docs/evidence/devtools-consumer/verification.md) başarısız deneme ve açık kabul sınırlarını kaydeder. Son iki görev tam kabul sağlamadı; status ve ölçüm görevi açık kalır. BMad step-03, dört paint/teslimat kabulü sağlanmış gibi kapatılmaz. Sonuç raporunun ayrı üç kanıt incelemesi yapılır; yalnız mevcut DevTools belgeleri için yerel commit hazırlanır. Eski pending kaynaklar ve `.memlog.md` commit kapsamına alınmaz.


## Kullanıcı kararıyla yürütme kapanışı — 2026-10-20

Kullanıcı ek başlangıç/ölçüm çalışmasını sürdürmemeye ve sonraki adıma geçmeye karar verdi. Bu spec’in yürütmesi kapatıldı; açık görevler teknik olarak tamamlanmadı. `status: in-progress` eksik teknik kabulü tarihsel olarak korur; `execution_status: closed_by_user` nedeniyle bu spec kendiliğinden sürdürülmez. Mevcut kapalı run, tüketim ve başarısızlık kayıtları değişmez. Yerel tanı onarımı kendi spec’inde tamamdır; gerçek başlangıç kök nedeni ve P4b/cihaz kabulü teknik borçtur. [Kapanış ve P5 devri](../../be/docs/evidence/image-optimization-handoff-2026-10-20.md) güncel çalışma yönünü kaydeder. Yeni kullanıcı talebi olmadan yeni APK/başlangıç/ölçüm planlanmaz veya çalıştırılmaz.
