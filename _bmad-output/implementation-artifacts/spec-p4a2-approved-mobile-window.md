---
title: 'P4a2: onaylı 180 saniyelik mobil ölçüm penceresi'
type: 'feature'
created: '2026-10-18'
status: 'done'
review_loop_iteration: 0
baseline_commit: '671b5d9a12bf0d08d92d437eae9c0b5dbd359556'
---

## Amaç ve onay

Kullanıcı “180 saniye ve gerekirse 1 ek giriş onaylı” dedi. Mevcut #94 çıktılarıyla
altı durum ölçülür. İlk pencereyi uzatmayan önceki araç tarihsel davranışını korur.
Bu yeni giriş yalnız ayrı, onaylı ek pencereyi uygular.

<frozen-after-approval reason="Ek süre ve koşullu tek ek giriş kullanıcı tarafından açıkça onaylandı.">

Yeni child yalnız `mobile-window-180` olur. Aynı run/prefix, #94, Türkçe, aynı
LIVE/revision/manifest/job, kaynaklar ve 822×1334 PNG profili korunur. Yeni üretim,
create/observe, migration, publish/reconcile/worker veya silme yoktur.

Önceki ledger SHA `60086ea1ad1bdccbcc2423481fcd5a20cd4777602c3e87b1c3636732b298b839`
ve kayıtlar değişmez. Harcanan read41/215.362.417 byte, verifier4 ve V37 tüketimi
korunur. Mevcut token artık yetersizdir; aynı kanonik QA UID için tek ek login ve
doğrulaması önceden exclusive rezerve edilir. Toplam sign-in2/verifier64 sınırı;
diğer ilk JSON sınırları ve birleşik159 çağrı tavanı korunur.

Yerel kapılar, oturum ve aynı LIVE denetimi geçtikten sonra tek 180.000 ms pencere
exclusive `window.json` ile açılır. Sayaçlar sıfırlanmaz. Pending context'te SDK
read/auth yasaktır. Tekrar ve hata otomatik yeni pencere açmaz. Token repo dışındadır.
Normal mobil uygulama/Library/detail/reader/ses/cache kodu ve APK içeriği değişmez.
Fiziksel kalite, genel üretim profili, JPEG/WebP, P0 ham kanıtı ve P4b açık kalır.

</frozen-after-approval>

## Görevler ve kabul

- `P4LocalComparisonLedger.java`: frozen tüketimli pending child; yerel kapılardan
  sonra tek pencere; read/auth süre ve kalan bütçe; write/observe kapalı.
- `P4ExistingComparisonSetup.java`: iki dar child modu; aynı LIVE ve oturum; yeni
  ready ile window başlangıç/sonu eşitliği. V37 sayaçlarını sıfırlamaz.
- `p4_reading_comparison.py`: yeni child onayı, değişmeyen ledger, aynı LIVE/window;
  VM öncesi süre/attempt kontrolü ve kalan süreyle sınırlı altı durum.
- İlgili Java/Python testleri; gerçek ölçüm ve hassas veri içermeyen kanıt raporu.

Geçerli onay ve aynı LIVE verildiğinde yalnız kalan read/auth açılır. Pending,
yanlış süre/scope/mühür veya ikinci çağrıda dış I/O oluşmaz. Altı durum başarılıysa
kimlik, gerçek payload, decode, paint, ekran, bellek örnekleri ve kapanış kaydedilir.
Eksik kanıt başarı sayılmaz.

## Doğrulama

Wrapper tam Maven testi, Python koruma testleri ve üç bağımsız BMad inceleme katmanı.
Gerçek login yalnız bu kapılardan sonra; pencere gerçek Java yerel kapılarından sonra.
Flutter kaynak/annotation değişmediği için yeniden APK üretimi veya generator yoktur.

## İnceleme ve düzeltmeler

Üç bağımsız inceleme tamamlandı. Blind inceleme bulgu üretmedi. Diğer bulgular
patch olarak giderildi; hedefli yeniden inceleme açık bulgu bırakmadı.

- Approval ve giriş sayaçları `BigInteger` ile tam karşılaştırılır; taşma kabul edilmez.
- Onaylı pencere wall clock yanında monoton saatle de en çok 180 saniyedir.
- Gerçek girişin kullandığı `prepareReady`, LIVE ve ikinci credential kapıları
  geçmeden window/ready yazmaz. Aynı tüketici metodu başarı ve ret testlerinde çalışır.
- Mobil kalan süre cihaz preflight'ından önce monoton son tarihe bağlanır.
  Capture ve tüm lifecycle adımları öncesinde/sonrasında denetlenir.
- Altıncı screenshot 181 saniyeye taşarsa başarı yazılmaz. Harcanmış bir saniye
  geri verilmez. Süre dolan preflight VM veya yeni attempt açmaz.

Python mobil 27 test geçti. İlk Java hali 488 testle geçti; son patch'lerin tam
paket doğrulaması ayrıca kaydedilir. Ek login ve gerçek pencere bu kapıdan sonra açılır.

## Son yerel doğrulama

Wrapper tam paket **490 testle** geçti; hata/atlama yok. Python mobil27 ve launcher13
test geçti. İlk final paketteki tek hata, test fixture'ının persisted JSON yerine
valueToTree sayı türü kullanmasıydı; wire JSON ile düzeltildi. Sekiz odaklı test ve
ardından tam 490 test yeniden geçti. Üç inceleme katmanı ve hedefli yeniden inceleme
açık bulgu bırakmadı. Bu bölüm ölçüm öncesi yerel kapının sonucudur.

## Gerçek ölçüm sonucu

Yerel kapılardan sonra aynı QA hesabına yalnız bir ek giriş yapıldı. Tek ek pencere
açıldı. Yeni üretim olmadan #94'ün detail ve iki sayfası için kaynak/çıktı altı durum
emülatörde tamamlandı. Başarı kaydı pencere başlangıcından 127.925 ms sonra yazıldı.
Arka plan, geri dönüş ve kapanışta sahip olunan kaynaklar 0 oldu; ek medya isteği yoktu.
Bize ait backend kapandı. Önceki ledger, hata kayıtları ve 11 tablodaki eski satırlar korundu.

Pinned read 47/64, toplam okuma 243.641.782 byte, verifier 20/64, sign-in 2/2;
create 14 ve observe 8 değişmedi. Fiziksel kalite ve genel P4/P4b açık kalır.
[Gerçek ölçüm ve ekranlar](../../be/docs/evidence/p4-reading-images/android-measurement-2026-10-18.md).

## Önerilen inceleme sırası

- LIVE ve oturum kapıları tek pencere açılışından önce geçer.
  [P4ExistingComparisonSetup.java:65](../../be/src/test/java/com/tellpal/v2/support/P4ExistingComparisonSetup.java#L65)

- Eski borçlar korunur; pending aşamada dış I/O kapalıdır.
  [P4LocalComparisonLedger.java:58](../../be/src/test/java/com/tellpal/v2/support/P4LocalComparisonLedger.java#L58)

- Süre ve görüntü kanıtı tamamlanmadan başarı yazılmaz.
  [p4_reading_comparison.py:306](../../tools/image-optimization/p4_reading_comparison.py#L306)

- Gerçek tüketici kapı sırası yerel dosyalarla sınanır.
  [P4ApprovedWindowLedgerTest.java:152](../../be/src/test/java/com/tellpal/v2/support/P4ApprovedWindowLedgerTest.java#L152)

- Geç kalan ekran görüntüsü başarısız kanıt üretir.
  [test_p4_reading_comparison.py:146](../../tools/image-optimization/test_p4_reading_comparison.py#L146)
