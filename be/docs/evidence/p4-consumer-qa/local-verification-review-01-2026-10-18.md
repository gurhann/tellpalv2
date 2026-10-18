# P4a5 ön çalışma incelemesi: dar guard düzeltmeleri

Tarih: 2026-10-18. Bu kayıt, [ilk yerel doğrulamanın](local-verification-2026-10-18.md)
üzerine gelen üç ön çalışma incelemesinin düzeltmelerini tutar. Önceki kayıt ve ham loglar korunur.
Triage özel kökte `reviews/pre-run-review-triage-01.private.json` içindedir.

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb`.
Bu düzeltme diliminde gerçek backend, cihaz, auth, storage, APK build veya arm çalıştırılmadı.
Testlerdeki build/OS/ADB/login sınırları mock'tur. Root'ın önceki offline `build-0001` çıktısı
korundu; nihai kaynağın yeni build/hash/bağlama ve gerçek çalışma kapıları root'a devredildi.

## Değişiklikler

1. Mobil kaynak haritası derlemeden önce sabitlenir. Derleme sonrası ve makbuz yazımı öncesi
   repo ve özel kopya bu aynı haritayı karşılamalıdır. Derleme sırasında iki kopya birlikte
   değişse bile yeni hash ile başarılı makbuz üretilemez.
2. Arm hatası yalnız yeni QA PID/başlangıç kimliğini ve yeni forward eşlemesini temizler.
   VM keşfi, bağlantı makbuzu ve cleanup hatası ayrı başarısız makbuzda kalır. Eski eşlemeler
   kaldırılmaz. Bağlantı kimliği proposal/paket/serial/PID/startTicks/port alanlarına bağlanır.
3. `local-ready.json` mevcut beş dakikalık kapının başlangıç ve deadline değerlerini taşır.
   Bu alanlar medya penceresi açmaz. Login öncesi owned PID/komut, kapalı/başarısız işaretler,
   doğru timestamp ve en az mevcut 10+2 saniyelik login payı denetlenir. Eksik, stale,
   expired veya rollback durumunda tek giriş rezervasyonu tüketilmez. Java kapısı credential
   ve DB kontrollerinden sonra, medya penceresi açılmadan hemen önce de yeniden denetlenir.
4. Run, build/source denetiminden önce güvenli bağlantı kimliğini alır. Kaynak/build reddinde
   mevcut owned QA ve forward temizlenir. Geçersiz kimlik, değişen PID/startTicks veya farklı
   eşleme üzerinden ilgisiz süreç/eşleme kapatılmaz.
5. Backend cleanup başarısı için özel `backend-closed.json` içindeki proposal/runId/PID/closed
   bağı ve gözlenen süreç çıkışı birlikte zorunludur. Eksik/yanlış makbuz, crash veya watchdog
   başarı değildir. Cleanup başarısızsa `finished=false` kalır; `verification.json` oluşmaz.
6. Spring'den alınan gerçek hizmet ve verifier tüketicileri, delegate çağrısından önce kalıcı
   `AUTH_RESERVED` satırını görür. Süre/bütçe dolunca delegate çalışmaz. Böylece verifier
   sarmasındaki rezervasyon kaldırılırsa yeni testler başarısız olur.

## Doğrulama

| Kapı | Sonuç | Yeni ham log |
|---|---|---|
| Etkilenen ve eski Python sürücü testleri | 57/57 geçti | `logs/python-review-03.log` |
| Java ledger/composition/setup testleri | 12/12 geçti; hata/atlama yok | `logs/java-review-02.log` |
| Kaynak kopyası ve koruma hash denetimi | Eşitlik/koruma makbuzunda | `receipts/implementation-audit-review-01.private.json` |
| `git diff --check` | Geçti | Audit sonucu |

Python komutu `python3 -m unittest discover -s tools/image-optimization -p 'test_p4*py'` idi.
Başarılar yalnız test fixture'larına aittir. Gerçek `arm`, `login`, `close_backend` ve `run`
yardımcıları testlerde çağrıldı; OS/süreç/ADB sınırları mock edildi. Bu testler eşzamanlı kaynak
mutasyonunu, arm VM/makbuz/cleanup hatalarını, stale login rezervasyonunu ve kaynak reddinde
cleanup'ı doğrular. Gerçek kapanış helper'ının matched drain/crash/watchdog sonucu, run'ın
başarı dosyası üretme davranışıyla birlikte sınandı.

Java komutu, özel kopyada mevcut wrapper ile offline çalıştı:
`sh .../mvnw -o -Dtest=P4ConsumerQaLedgerTest,P4ConsumerQaCompositionTest,P4ConsumerQaSetupTest test`.
Yeni sonuç dağılımı ledger 5, composition 3, setup 4 testtir.

`python-review-01.log` 55, `python-review-02.log` 57 ve `java-review-01.log` 12 başarılı
ara testi korur. Sonraki kayıtlar bunların üzerine yazılmaz. Önceki ilk dilimin kayıp log
sınırı ilk rapordaki açıklamasıyla korunur.

Mobil ve üretim kaynakları değişmedi. Önceki 234 mobil/analyzer ve 30 gerçek PostgreSQL/Tomcat/
modül IT sonucu bu dilimde yeniden çalıştırılmadı; yeni guard'ların gerçek cihaz veya backend
kabulü olarak sunulmaz. Colima veya host PostgreSQL'e bu dilimde dokunulmadı.

## Devir ve açık kapılar

Değişen kaynaklar yalnız `p4_consumer_qa.py`, `test_p4_consumer_qa.py`, `P4ConsumerQaSetup.java`,
`P4ConsumerQaSetupTest.java`, `P4ConsumerQaCompositionTest.java` dosyalarıdır. Tam SHA değerleri
`receipts/implementation-audit-review-01.private.json` içindedir. Donmuş spec/proposal,
3241 korunan repo dosyası ve 72 eski çalışma dosyası değişmez.

Gerçek Android `/proc` ve ADB sahiplik okumaları ile backend'in makbuz sonrası çıkışı henüz
bu yeni guard'larla cihazda sınanmadı. Root yeni immutable build ve kapatılma incelemesinden
sonra bu hazırlığı yürütür. Gerçek auth için mevcut web API yapılandırma yolu girdi kapısı
korunur; yeni key/management API/kurulum veya indirme açılmaz. Tek gerçek login/pencere/medya
koşusu, P4b, fiziksel cihaz kalite ve peak kaynak kabulü açık kalır.
