---
title: 'P4a2: mevcut #94 çıktılarıyla mobil ölçüm'
type: 'feature'
created: '2026-10-17'
status: 'done'
review_loop_iteration: 0
baseline_commit: 'cd8c7747f1cabd5dd17276f31993d23249d73518'
---

## Onaylanan amaç

Kullanıcı mevcut #94 çıktılarıyla yeniden üretim yapmadan mobil ölçümün tamamlanmasını
istedi. Önceki kayıtlar ve sınırlar korunacak. Ekrana dokunmayacağını belirtti.

<frozen-after-approval reason="Kullanıcının mevcut çıktılarla devam isteği açıktır.">

Tek hedef: aynı QA kopyasının altı kaynak/rendition durumunu ölçmek. Kaynak/profil,
run/prefix ve LIVE kimliği değişmez. Yeni içerik, üretim, create/observe, worker,
reconcile, migration, login veya silme yoktur. Normal mobil uygulama değişmez.

Önceki ledger SHA-256 `60086ea1ad1bdccbcc2423481fcd5a20cd4777602c3e87b1c3636732b298b839`
ve OPEN zamanı `1792269638011` sabittir. İlk 900.000 ms penceresi uzatılamaz. Harcanan
41 read/215.362.417 byte ve 4 verifier korunur. Tüm toplam ve faz tavanları ilk
onaylı JSON'dan gelir. V37 bütçesi 4 işlem/3.292.721 byte tüketimini korur. Önceki
ledger ve başarısız mobil attempt değişmez. Child `mobile-continuation` tek kullanımlıktır.

Eksik/geçersiz oturum, kimlik, mühür, süre veya bütçe halinde devam reddedilir.
Hata yeni otomatik tekrar açmaz. Fiziksel cihaz ve üretim profili kabulü çıkarılmaz.

</frozen-after-approval>

## Uygulama

- `be/src/test/java/com/tellpal/v2/support/P4LocalComparisonLedger.java`: yalnız
  mühürlü kapanmış faz 5'i child kaydına taşı; eski sayaç/zamanı devral; yazmayı reddet.
- `be/src/test/java/com/tellpal/v2/support/P4ExistingComparisonSetup.java`: aynı
  klon/#94 için migration kapalı context; descriptor/identity eşitliği; sonlu sunucu.
- `tools/image-optimization/p4_reading_comparison.py`: yalnız onaylı child state;
  parent ledger/identity/descriptor mührü; eski attempt korunur.
- İlgili ledger/Python testleri: süre/sayaç drift, tekrar, yanlış scope ve write reddi.
- `be/docs/evidence/p4-reading-images/`: gerçek altı render/payload/decode/kapanış
  kanıtı veya açık başarısızlık sınırı; hassas veri yayımlanmaz.

## Kabul

- Geçerli kapalı ledger ve aynı LIVE verildiğinde yalnız kalan read/auth bütçesi açılır.
- Süre/scope/mühür değiştiğinde SDK veya VM çağrısından önce ret oluşur.
- Tek devam yeniden çağrıldığında yeni I/O başlamaz; eski kayıt korunur.
- Altı durum tamamlandığında aynı kimlik ve boyutlar, gerçek byte, paint, sahiplik,
  ekran ve kapanış ölçülür. Eksik kanıt başarı sayılmaz.

## Doğrulama

Wrapper Maven testleri; ilgili Python koruma testleri; bağımsız BMad inceleme katmanları.
Flutter ürün kodu veya APK içeriği değişmiyor; aynı APK hash'i yeniden doğrulanır.
Gerçek ölçüm bütün yerel kapılar geçtikten sonra ve ilk son tarih dolmadan başlar.

## İnceleme ve düzeltmeler

Üç bağımsız inceleme tamamlandı. Yinelenen süre bulgusu birleştirildi; üç patch giderildi:

- Python ilk son tarihi Android/VM erişiminden önce denetler. Mobil bekleme süresi
  kalan ilk pencereyle sınırlanır; configure/select/paint öncesi süre kontrol edilir.
- Child attempt zaten varsa cihaz preflight'ından önce ret oluşur.
- Java testi saatini doğrulanan sabitten değil, mühürlü fixture'ın OPEN satırından
  türetir. Runtime aynı OPEN bağını ayrıca doğrular. Süre uzatma regresyonu reddedilir.

Python mobil 21 ve launcher 13 test geçti. Java odaklı 13 test ilk uygulamada geçti;
son OPEN patch'i tam Maven paketinde yeniden doğrulanır. İlk tam test Docker kapalı
olduğu için ortam hatası verdi; aynı testler mevcut Colima ile tekrar çalıştırıldı.

## Hazırlık sonucu ve gerçek çalışma sınırı

Wrapper tam test paketi **482 testle** geçti; hata/atlama yok. Python mobil21 ve
launcher13 test geçti. Üç inceleme patch'i kapandı. İlk test Docker kapalıyken ortam
hatası verdi; Colima socket'i komuta açıkça seçilince tam paket geçti.

İlk900 saniyelik süre yerel kontroller sırasında doldu. Gerçek Java girişinin
context açmadan reddi doğrulandı; child ledger/mobile attempt oluşmadı. Yeni
cloud/auth/medya çağrısı yok. Yardımcı kod tamamdır; mobil ölçüm tamamlanmadı.
Ek süre/koşullu giriş kararı ayrı öneridedir ve henüz uygulanmadı.

## Önerilen inceleme sırası

- Mevcut LIVE kimliğini koruyan giriş; migration ve üretim kapalıdır.
  [P4ExistingComparisonSetup.java:20](../../be/src/test/java/com/tellpal/v2/support/P4ExistingComparisonSetup.java#L20)

- Mühürlü ledger tüketimini ve ilk son tarihi devralır.
  [P4LocalComparisonLedger.java:46](../../be/src/test/java/com/tellpal/v2/support/P4LocalComparisonLedger.java#L46)

- Süre ve tek kullanımlık child kontrolü cihaz erişiminden önce çalışır.
  [p4_reading_comparison.py:305](../../tools/image-optimization/p4_reading_comparison.py#L305)

- Fixture OPEN zamanı bağımsız son tarih kanıtıdır.
  [P4ExistingComparisonLedgerTest.java:25](../../be/src/test/java/com/tellpal/v2/support/P4ExistingComparisonLedgerTest.java#L25)

- Süresi dolan devam VM veya cihaz erişimine ulaşmaz.
  [test_p4_reading_comparison.py:142](../../tools/image-optimization/test_p4_reading_comparison.py#L142)
