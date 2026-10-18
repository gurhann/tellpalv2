# P4a5: bekleme bağlantısı geçti, yerel backend reddedildi

Tarih: 2026-10-18. Kullanıcı, aynı APK/emülatör için bir ek yerel başlangıcı ve kapılar
geçerse önceki tek ölçüme devam edilmesini onayladı. **Bekleme bağlantısı geçti; gerçek
ölçüm başlamadı.** Önceki hata kayıtları korunur.

## Çalışan ve reddedilen adımlar

- `build-0002` aynı APK hash'iyle bir kez açıldı. Yeni PID/startTicks, VM bağlantısı ve
  pasif probe snapshot'ı doğrulandı. Snapshot'ta HTTP olayı, Cubit veya owned raster yoktu.
- Giriş yardımcısının mevcut Python ortamı ve özel girdileri doğrulandı. Sürüşün mevcut
  sistem Python ortamında websocket modülü vardır. Firebase venv'inde websocket arayan
  birleşik import kontrolü başarısız oldu; kurulum yapılmadı. Giriş ve sürüş kendi mevcut
  ortamlarında ayrı doğrulandı. Bu yerel import kontrolü auth denemesi değildir.
- Test edilmiş Java sınıflarıyla backend bir kez başlatıldı. Loopback, aynı V37 klonu,
  kapalı Flyway/scheduler/poller ve 1 GiB heap ayarları kullanıldı. Spring/JPA başlangıcı
  `FATAL: role "tellpal" does not exist` nedeniyle reddedildi. LIVE/manifest ve yerel
  hazır kapısı tamamlanmadı.
- Başlatma komutunda `TELLPAL_DB_USERNAME` eksikti. `application.yml` varsayılan `tellpal`
  rolünü seçti. Bu, root'ın yerel başlatma ayarını aktarma hatasıdır. Yeni rol oluşturulmadı.
- Mevcut `gurhankucuk` hesabıyla aynı klona tek salt okunur `psql` sorgusu geçti.
  `transaction_read_only=on`, V37 operations **25**, reserved bytes **31.572.086** ve
  tavanlar **64 / 67.108.864** doğrulandı. Veritabanı veya rol değiştirilmedi.

## Tüketim ve kapanış

Yeni backend ledger'ı `OPEN_PENDING` ve eski borçtan sonra
`CLOSED 0 0 0 0 0 0` ile kapandı. Yeni login, verifier, Storage read/create/observe,
manifest/status/medya HTTP çağrısı ve ölçüm penceresi **sıfırdır**. Önceki tek Hosting
config GET'i tekrar edilmedi. Onaylı tek giriş ve 180 saniyelik pencere kullanılmadı.

Probe'a `stop` gönderildi; credential/deadline ve bütün owned buffer/descriptor/codec/image
sayaçları sıfır doğrulandı. Yalnız bu başlangıca ait forward kaldırıldı; QA süreci
PID/cmdline/startTicks bağıyla kapatıldı ve PID yokluğu doğrulandı. Backend kendi hata
çıkışıyla sonlandı. Bu, başarılı backend drain kanıtı olarak sunulmaz.
Eski `tcp:53421 → tcp:39447` eşlemesi korundu; yeni reverse yoktur. Başka süreç durdurulmadı.

Özel kök `/Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb`:

- `android/local-waiting-approval-01.private.json` ve reservation: kullanılan ek yerel izin.
- `android/arm-launch-intent-02.private.json`, `connection.private.json`: başarılı bağlantı.
- `receipts/waiting-vm-preflight-02.private.json`: gerçek boş bekleme snapshot'ı.
- `backend/backend-runtime.log`, `failure.json`, `ledger.log`, `launch.private.json`: hata.
- `receipts/local-backend-failure-cleanup-01.private.json`: gerçek temizlik sonucu.
- `receipts/local-db-role-readonly-01.private.json`: mevcut hesap ve değişmeyen DB borcu.

İlk başarısız arm makbuzu, eski build/test/run kayıtları ve `.memlog.md` korunur.
Yeni kaynak değişikliği yapılmadı; son 68 Python, 12 Java, önceki 234 Flutter ve 30 gerçek
IT sonuçları korunur. Bu sonuçlar gerçek teslimat başarısı değildir. Dört UI paint'i,
medya transferi, fiziksel kalite ve peak kaynak kanıtı açık kalır.

Onaylı başarısızlık sınırı nedeniyle ikinci backend veya başka QA başlangıcı yapılmadı.
Karara sunulan dar çözüm [yerel kurtarma planındadır](../../../../_bmad-output/implementation-artifacts/image-optimization/p4a5-local-runtime-recovery.md).
