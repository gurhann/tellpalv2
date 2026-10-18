# P4b1: Status kapanışı — yerel doğrulama

Tarih: 2026-10-18. Başlangıç commit'i: `35d0bb384b0aba642e4cbfb8cc0a4000fdccf909`.
[Onaylı spec](../../../../_bmad-output/implementation-artifacts/spec-p4b1-qa-status-drain.md)
uygulanmıştır. Son kaynakta **468 test geçti; analyzer temizdir**. Üç bağımsız BMad
incelemesi tamamlandı; açık bulgu yoktur.

## Değişiklik

Cubit status işinin Future'ını sahiplenir. `close()` ilk çağrıda yeni iş kabulünü
kapatır, mevcut işleri iptal eder ve hesap subscription'ı ile medya/status işlerinin
sonlanmasını bekler. Ardından Cubit stream'i kapanır. Tekrarlanan `close()` aynı
Future'ı döndürür. Geç status exception'ı kontrollü `status_refused` yoluna gider;
kapanış veya eski epoch yeni admission/paint üretemez. Status mevcut medya gate'ine
alınmamıştır. DI, router, transport, anchor ve native gate değişmemiştir.

İki görev test dosyasına sekiz Cubit senaryosu ve bir gerçek parent/HTTP senaryosu
eklendi. Mevcut selector, medya iptali, native gate ve normal tüketici testleri korundu.

## Son kaynak ve çalışma ortamı

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b1-status-drain-20261018-35d0bb3/`.
Flutter komutları `source/app/mobile/` altında mevcut SDK/cache ile `--no-pub`
çalıştı. Paket/SDK indirilmedi. Ham log ve makbuzlar ayrı, numaralı CREATE_NEW
dosyalardır; dizinler `0700`, log/makbuzlar `0600` tutulur.

| Görev kaynağı | SHA-256 |
|---|---|
| `reading_image_comparison_cubit.dart` | `1e2f13361400dd06b43145d8f713b2e6316ca7a1cdd3efb9d99293f7e27aca63` |
| `reading_image_comparison_cubit_test.dart` | `8ecd081e7ec590b739d90f4f0a6c45689ffa1b274e62997bb592a9db9f710b45` |
| `reading_image_consumer_qa_test.dart` | `1dc08e3c2b8dd54a5a146ae7814d722eba63e34a86361dd1f514cba0956c6fef` |

Son snapshot 220 izlenen kaynak/fixture/paket varlığından oluşur: 216 mobil dosya,
bir retained descriptor JSON fixture'ı ve üç Python fixture modülü. Sıralı
`path + NUL + SHA-256 + LF` kaydının SHA-256 değeri
`426cb32e65dce0ad1b2e6214d676785d64788021c2cab837113d1e3259585e4f`.

Eşitlik, bu snapshot ve üç görev dosyası için doğrulanmıştır. Tek açık istisna:
korunan dirty `main_image_experiment.dart` repo'da aynen kalırken, özel kopyada
onun **HEAD sürümü** kullanıldı. Böylece untracked local-lossless import'u kopyaya
taşınmadı. Bütün dirty/untracked çalışma ağacının test edildiği iddia edilmez.

Mevcut dört paket metadata dosyası ve bir runtime çalıştırıcısı snapshot hash'inden
ayrı kaydedilmiştir. Eski sentetik pinned-TLS testi sabit göreli Python yolu
beklediği için dar çalıştırıcı mevcut repo `.venv/bin/python` yoluna yönlenir.
Yeni venv veya kurulum yoktur. Python tarafında yalnız mevcut sentetik
`python_fixture_broker.py` testi çalıştı; gerçek fixture entry'si çalışmadı.
Credential, `.env`, gerçek servis config'i ve untracked dosyalar kopyalanmadı.

## Frozen kabul matrisi

Son kaynak için bütün satırların geçen ham kanıtı `23-full-tracked-final.private.log` içindedir.

| Spec durumu | Test adı ve gözlenen sonuç |
|---|---|
| Status yok | `status başlamadan close mevcut medya işinin sonlanmasını bekler`; ayrıca `gerçek QA parent UI close native gate release bitmeden kapanışı tamamlamaz`. Medya/native completion bırakılmadan kapanış bitmez. |
| Status bekliyor | `ortak close status ve medyayı bekler: success, statusFirst=false`. İki cancellation bildirilir; medya bitse bile status beklerken `isClosed == false` kalır. |
| İptale rağmen geç success/error | `ortak close status ve medyayı bekler: <outcome>, statusFirst=<bool>` ailesinin altı testi: `success/failure/exception × true/false`. Geç success, Either hatası ve Future exception'ı yeni admission/raster/loaded üretmez; yakalanmamış hata oluşmaz. |
| Close sırasında seçim/timer/hesap/visibility | Aynı altı testte seçim, choice/retry, revoke, background/resume ve iptal edilmiş timer callback'leri kapanış sırasında çağrılır. State nesnesi aynı kalır; manifest/bytes/status çağrı sayıları `1/1/1` kalır. |
| İki close; status ve medya bekliyor | Aynı altı test `identical(firstClose, secondClose)` doğrular. Her iki completion sırası denenir; biri bitince ortak kapanış bitmez. `retire ile iptal edilmiş status da close drain kapsamındadır` önceden iptal edilmiş işi ayrıca kapsar. |
| Parent ve gerçek yavaş status gövdesi | `gerçek QA parent yavaş status soketini iptal eder ve status Future bitmeden kapanmaz`. Gerçek status isteğine ve kısmi gövdeye ulaşılır; aşağıdaki üç olay ayrı gözlenir. |

Parent testinde gerçek data source → repository → Cubit zinciri kullanılır.
Loopback sunucusu status başlıklarını ve gövdenin tek byte'ını gönderir. Sunucu
tarafındaki TCP peer kapanışı (`statusDisconnected`) ile gerçek data source
`read()`/`finally` sonlanması (`transportStopped`) ayrı gözlenir. Cancellation
sonrasında ikisi de tamamlanır. Test wrapper'ı çağırana dönen status Future'ının
completion'ını ayrıca tutar (`releaseCompletion`); bu bir ürün timeout'u değildir.
Bu completion bırakılmadan parent'ın kapalı UI'ı görünmez ve `PopScope.canPop`
false kalır. Bırakıldıktan sonra Cubit kapanır, child/raster kalkar ve parent
kapalı UI'a geçip pop iznini açar. Böylece soket sonlanması, wrapper Future'ı ve
parent tamamlanması birbirine eşit sayılmaz.

`eski native Future kapanmadan yeni HTTP/decode yok; geç epoch boyanmaz` ve mevcut
selector/geometri testleri de aynı son tam koşuda geçer. Tam küme Library, detail,
transfer, reader çözünürlüğü ve QA'da sesin kapalı olup oynatıcı açılmadığı durumları
kapsar. Normal PoC ses/gezinti kaynakları hash ile korunur; gerçek ses oynatma ve
birleşik ürün gezinmesi bu dilimde çalıştırılmadı.

## Komutlar ve ham kayıtlar

| Log | Sonuç / kaynak dönemi |
|---|---|
| `01-focused` | Önceki fixture: 36 geçti, yeni parent testinde bekleme hatası. |
| `02-status-fixture`, `03-status-drained-request`, `04-status-diagnostic` | Hedefli fixture tanıları başarısız. 04, drain/detach/flush'ın bittiğini gösterdi; completion fake zone'da bekliyordu. |
| `05-status-native-zone` | Parent kapanış Future'ını native runAsync içinde bekleyen test adımı düzeltildi. Ham hata korundu. |
| `06-status-parent` | Gerçek status/parent hedefli testi geçti. |
| `07-reading-detail` | 193 geçti; üç probe/helper import'u özel kopyada eksikti. Transitif kapanışla altı izlenen Dart yardımcı eklendi. |
| `08-reading-detail-complete` | 205 geçti; izlenen `consumer-retained-ready.json` fixture'ı eksikti. Tek JSON fixture eklendi. |
| `09-reading-detail-fixtures` | `flutter test --no-pub test/reading_images test/story_detail`: 206 geçti. Analyzer düzeltmesinden önceki kaynak dönemidir. |
| `10`–`13` | Önceki kaynakta dört mutasyon beklenen assertion'ları bozdu; her biri geri alındı. |
| `14-full-tracked` | 467 geçti; eski pinned-TLS testi özel kopyada interpreter yolu bulamadı. |
| `15-existing-tls-runtime` | Mevcut interpreter ve dar fixture modülleriyle eski pinned-TLS testi geçti. |
| `16-full-tracked-complete` | Önceki kaynak döneminde 468 geçti. |
| `17-analyze` | Bir `must_call_super` uyarısı ve üç stil bilgisi. `super.close()` ortak close Future'ına taşındı; null-aware liste öğesi ve test blokları düzeltildi. |
| `18-analyze-fixed` | Son kaynakta `flutter analyze --no-pub`: `No issues found!` |
| `19`–`22` | Son kaynakta aşağıdaki dört mutasyon beklenen assertion'lardan kırıldı. |
| `23-full-tracked-final` | Son kaynakta `flutter test --no-pub --reporter expanded`: 49 izlenen test dosyasında **468/468 geçti**. |

206 testlik dar küme tam kümenin içindedir; sonuçlar toplanmaz. Ham logların tam
adları, exit code'ları ve doğrulanan SHA-256 değerleri [verification.json](verification.json)
içindedir. Ham içerik özel kökte `logs/`, komut/zaman/log-hash makbuzları `receipts/`
altındadır. Eski başarısız kayıtların üstüne yazılmadı.

## Son kaynakta mutasyonlar

| Log | Yalnız özel kopyada kaldırılan koruma | Bozulan assertion |
|---|---|---|
| `19-mutation-status-await-final` | Status Future'ının drain beklemesi | Medya biterken kapanışın hâlâ beklemesi: beklenen false, gerçek true. |
| `20-mutation-parent-await-final` | Aynı status beklemesi; gerçek parent testi | Status wrapper beklerken `cubit.isClosed`: beklenen false, gerçek true. |
| `21-mutation-status-cancel-final` | `_statusCancellation?.cancel()` | Status cancellation: beklenen true, gerçek false. |
| `22-mutation-admission-final` | `select()` kapanış iş kabulü kapısı | Kapanış state nesnesinin aynı kalması: beklenen true, gerçek false. |

Dördü de test exit code `1` üretti. Her koşunun `finally` geri alma makbuzu
başlangıç ve dönüş SHA-256 değerlerini eşit doğrular. Sonrasında 23 tam küme geçti.

## Koruma ve açık sınırlar

`audit-post23.private.json`: 3.269 korunan repo dosyası, 72 eski run dosyası ve
192 eski P4a5 özel dosyasında sıfır fark. Frozen SHA-256
`7943349315705e710cfdb19c43d858fdb9b320a1a9e37f32dfa87784783bbee6` korunur.
220 snapshot dosyasında açıklanmamış eşitlik farkı yoktur. 49 izlenen test dosyası
ve bütün göreli Dart import/export/part bağımlılıkları mevcuttur. Runtime
çalıştırıcısının hash'i eşittir. `git diff --check` temizdir.

Gerçek Firebase/storage/backend/cihaz koşusu, üretim erişimi, upload, sayaç reseti,
silme veya dağıtım yapılmadı. Test watchdog'ları ürün kapanış garantisi değildir.
Çoklu-owner resident bellek rezervasyonu, üretim profili, cache/session/retention,
fiziksel Android/iPhone kalite ve GPU/peak bütçe kapıları açık kalır.

## Bağımsız incelemeler

Blind hunter, edge-case hunter ve verification-gap incelemeleri bağlamsız, oturumun
modeliyle yapıldı. Üçü de başlatıldıktan sonra sonuçlar değerlendirildi. Hiçbiri
düzeltme gerektiren bulgu bildirmedi. Tam tracked/untracked diff ile başlangıçta
mevcut korunan dosyaları ayıran görev envanteri kullanıldı. Ham sonuçlar özel kökte
`reviews/01-*.private.txt`, hash ve diff kimlikleri `receipts/review-01.private.json`
içindedir. Makbuz SHA-256:
`cec3d7ab51678eafe6835433b175e70aeffda39046fd10b7568fea1b37cb1529`.

Root son kontrolde tek belge düzeltmesi yaptı: test edilen kapsam ile normal
ses/gezinti kaynaklarının hash koruması ayrıldı. Bu `medium/patch` bulgusu kapandı.
Kod ve testler değişmedi; belge kontrolü ve `git diff --check` geçti. Ek test koşulmadı.
23 logun hash'i, üç görev kaynağı ve kabul matrisini kapsayan 11 test adı ayrıca
doğrulandı. Yalnız P4b1 dosyaları yerel commit kapsamındadır; push yapılmaz.
