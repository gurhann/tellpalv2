# P4a5: mevcut #94 çıktılarıyla gerçek emülatör koşusu

Tarih: 2026-10-18. **Dar P4a5 kabulü geçti.** Ayrı QA rotasında
detail → sayfa 1 → sayfa 2 → detail akışı, mevcut üç doğrulanmış PNG ile çalıştı.
Yeni üretim yapılmadı. Bu sonuç genel mobil geçişi veya üretim profilini onaylamaz.

## Ortam ve kimlik

Android ARM64/API 36 emülatörü `emulator-5554`; mevcut 822×1828 ekran override'ı,
320 dpi, DPR 2 ve font ölçeği 1 korundu. Debug APK SHA-256:
`102521e8c77a30784e3f2af185f5e810d539edfb69f76eea8aa4a541a9685166`.
Kurulu aynı APK kullanıldı; yeniden build veya kurulum yoktu.

Kanonik QA UID `jLRypTmoBrhhX21HsDJojurcKTC3`, içerik #94, dil `tr`, LIVE generation 4:

- Revision: `3ddef109a065667d6e44f19aabce9151f1295d8f1e2502a4ba5b14e29a5d458d`.
- Manifest: `d4636ac8e61967aad2f2a53d9aa1da736214075a915f2dd53b7d1d80fe1a42e2`.
- Job: `bb9ae40b-e5ab-4054-885e-0b1fc14e841e`.
- Run: `p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09`; eski borç sıfırlanmadı.

Yeni backend, mevcut V37 klonunda açık `gurhankucuk` kullanıcısıyla çalıştı.
Aynı Java 25 binary'si ve 1 GiB heap ile yapılan ayrı salt okunur JDBC ön kontrolü
DB hesabını, şemayı ve önceki borcu doğruladı. Ürün DB ayarı, rol veya şema değişmedi.

## Gerçek teslimat ve gösterim

Her satırda encoded dosya **822×1334 PNG / 3.292.721 byte** idi. Tek manifest/anchor
korundu. Kaynak, profil, rendition, rol, sayfa, revision ve manifest kimlikleri
beklenen kayıtlarla eşleşti. `canonicalSource=false`; original fallback yoktu.

| Geçiş | Rol / sayfa | Gerçek alan, logical px | Fit / DPR | Gereken görsel px | Decode px | Paint epoch | Medya aktarımı |
|---|---|---|---|---|---|---|---|
| 1 | `readingDetail` / 0 | 411×456,67 | cover / 2 | 822×1334 | 822×1334 | 1 | 1.850 ms |
| 2 | `readingPage` / 1, id 1427 | 411×662 | contain / 2 | 816×1324 | 816×1325 | 3 | 1.835 ms |
| 3 | `readingPage` / 2, id 1428 | 411×662 | contain / 2 | 816×1324 | 816×1325 | 5 | 1.371 ms |
| 4 | `readingDetail` / 0 | 411×456,67 | cover / 2 | 822×1334 | 822×1334 | 8 | 1.253 ms |

Detail alanı fiziksel 822×914 px'tir. `cover`, kırpılan görselin de ölçeğini korumak
için 822×1334 px ister. Reader alanı fiziksel 822×1324 px'tir; `contain` ile sığan
görselin genişlik ihtiyacı 816 px olur. DPR azaltılmadı. Decode boyutları iki eksendeki
ihtiyacı karşıladı. Dört paint kaydı aynı yükleme epoch'larıyla doğrulandı.

Manifestte bu roller için mevcut tek doğrulanmış boyut kullanıldı. Bu koşu yeni bir
varyant merdivenini veya bütün cihazlar için en iyi byte/kalite profilini kanıtlamaz.
Aktarım süreleri tek debug koşusunun gözlemidir; kontrollü performans karşılaştırması değildir.

Dört ekran ve kapanış ekranı görsel olarak incelendi. Kapak ile doğru sıralı iki sayfa
görünüyor. Fiziksel cihazda okunabilirlik/kalite kabulü verilmedi.

## Sayaçlar ve sınırlar

| Ölçüm | Gerçek sonuç | Onaylı yeni sınır |
|---|---:|---:|
| Medya read | 4 | 4 |
| Medya payload | 13.170.884 byte | 13.170.884 byte |
| Manifest / status | 1 / 1 | 1 / 18 |
| Mobil HTTP | 6 | 23 |
| Login dahil ek verifier rezervasyonu | 11 | 28 |
| Ek QA login | 1 | 1 |
| Yeni pencere | 1 | 1, en çok 180 saniye |

Pencere açılışı `1792343619415`, son sınırı `1792343799415` epoch ms idi.
Koşu pencere dolmadan kapandı. Son yeni ledger satırı:
`CLOSED 4 13170884 11 1 1 4`.

Kümülatif read **51**, read byte **256.812.666**, verifier **31**, login **3** oldu.
Storage logical çağrı **73**, birleşik logical çağrı **107** oldu. Bunlar mantıksal
rezervasyon sayaçlarıdır; SDK'nın iç HTTP/OAuth istekleri veya fatura ölçümü değildir.
Yeni create/write/observe yoktu; önceki bu sayaçlar değişmedi. Fatura sorgulanmadı.

Son salt okunur DB kontrolü V37 operations **39/64**, reserved bytes
**44.742.970/67.108.864** doğruladı. Aynı run ve klon kullanıldı.

## Yaşam döngüsü ve kaynak sınırı

Arka plan ve geri dönüş snapshot'larında owned image 0 idi; HTTP ve rezervasyon
olayları değişmedi. Aynı PID/cmdline/startTicks dönüşten önce ve sonra doğrulandı.
Kontrol ile dönüş komutu arasındaki OS yarışı atomik olarak dışlanmış sayılmaz.
Gözlenen süreç değişimi veya ölümünde yeni dönüş komutu gönderilmez; retry yoktur.

UI kapanışında tek Cubit kapalıydı; izlenen buffer, descriptor, codec ve image
sayaçlarının hepsi 0 idi. Credential/deadline bağları kapandı. Bu sayaçlar bütün
Flutter engine/GPU belleğini temsil etmez.

| An | Toplam PSS, KiB | Toplam RSS, KiB |
|---|---:|---:|
| Boş bekleme sonrası başlangıç | 269.687 | 335.128 |
| Detail | 308.946 | 375.820 |
| Sayfa 1 | 305.789 | 373.196 |
| Sayfa 2 | 303.310 | 370.720 |
| Detail dönüş | 307.221 | 374.628 |
| UI kapandıktan sonra | 301.187 | 371.664 |

Bunlar tüm debug sürecinin anlık ölçümleridir; peak, GPU veya üretim profili değildir.
UI kapanışındaki toplam bellek başlangıca dönmedi. Buradan leak yoktur veya genel
decoded bellek sınırı uygundur sonucu çıkarılamaz. P4b kaynak bütçesi açık kalır.

VM, yeni reverse/forward ve yalnız owned QA süreci temizlendi. Backend drain makbuzu
PID ile eşleşti ve süreç exit code 0 ile sonlandı. Son ayrı kontrolde QA/backend PID
yokluğu, reverse boşluğu ve eski `tcp:53421 → tcp:39447` eşlemesinin korunması doğrulandı.

## Önceki hatalar ve kayıtların korunması

Onaylı kurtarmada bir ek QA başlangıcı ve bir ek backend başlangıcı kullanıldı;
bu görevin toplamları sırasıyla 3 ve 2 oldu. Eski başarısızlıklar yerinde kaldı.
Yeni backend kanıtı yalnız `backend-02/`, bağlantı `connection-03.private.json` altında.

İlk pasif `set_frame_sync` sorgusu `VM_REFUSED` döndü. Aynı PID üzerinde salt okunur
isolate sorgusu driver kaydının hazır olduğunu gösterdi; ardından boş snapshot geçti.
Süreç yeniden başlatılmadı; bu aşamada backend/login/medya yoktu. İlk ret tanısı saklandı.
Ön kontroldeki APK yolu ret kaydı ve yanlış etiketli ilk tanı da silinmedi; ikinci
makbuz gerçek `~~` yolunu ve PID yokluğunu açıklığa kavuşturdu.

3241 korunan repo dosyası, iki salt okunur plan, 72 eski run dosyası ve kurtarma öncesi
94 özel dosya hash'leriyle değişmedi. İlgisiz `.memlog.md` korundu. İlk geliştirme
loglarından daha önce kaybolan ham kayıtlar yalnız özetle temsil edilir; bu sınırlama sürer.

## Doğrulama ve kanıt konumu

Son kaynaklarda Python **85/85**, Java **19/19** geçti. Üç bağımsız inceleme ve bulgu
kapanışları temizdi. Önceki Flutter **234/234**, analyzer ve gerçek PostgreSQL/Tomcat/modül
**30/30** kanıtı korunur; son yalnız QA araç düzeltmelerinden sonra mobil/IT tekrar koşulmadı.

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb`.
Ham ekran, build, token ve çalışma kayıtları burada kalır; repo'ya ekran/token kopyalanmadı.

- `observations/01..04.{json,png}`, `05-closed.png`, `verification.json`, `attempt-result.json`.
- `backend-02/{ledger.log,local-ready.json,ready.json,backend-closed.json,process-exit.private.json}`.
- `receipts/final-runtime-verification-01.private.json`: son byte, DB, cleanup ve hash kontrolü.
- `receipts/waiting-vm-{diagnosis,preflight}-03.private.json`: ilk ret ve aynı PID'deki hazır kapısı.
- `reviews/runtime-recovery-closure-01.private.json`: üç kapanış incelemesi.
- `logs/python-runtime-recovery-07.log`, `java-runtime-recovery-03.log`.

P0'ın eksik ham kanıtı, fiziksel Android/iPhone kalite onayı, JPEG/WebP bulanıklığı,
üretim profili, normal mobil tüketici geçişi ve P4b açık kalır. Erişim politikası,
medya teslimi/URL yenileme, HTTP cache ve session/retention ürün kapıları bu QA sonucu
ile onaylanmaz. Normal Library/detail/reader ses, cache ve gezinme akışları değiştirilmedi.
