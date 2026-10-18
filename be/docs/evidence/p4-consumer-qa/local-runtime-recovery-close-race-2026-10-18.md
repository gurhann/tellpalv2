# P4a5: stop sonrası doğal kapanış yarışı

Tarih: 2026-10-18. Bu tek offline patch, [önceki son patch'e](local-runtime-recovery-final-patch-2026-10-18.md)
eklenen kapanış yarışını düzeltir. Yeni stop, restart, gerçek başlangıç, login,
ölçüm, DB, ADB veya SDK işlemi yapılmadı.

Stop sonrası `pidof` pozitifken süreç `/proc/.../cmdline` veya `stat` okunmadan
kapanırsa kimlik okuması hata verebilir. `close_owned_qa()` bu hatadan sonra aynı
iki saniyelik deadline içinde tek fresh PID sorgusu yapar. Yalnız yokluk doğrulanırsa
`STOPPED_OWNED_QA` döner. PID canlı/değişmişse, sorgu hata verirse veya süre dolarsa
cleanup başarısızdır. İkinci force-stop yoktur.

Gerçek Android adapter'ını fake subprocess sınırında kullanan testler hem cmdline
hem stat sırasındaki doğal çıkışı ve son boş PID sorgusunu gözler. Gerçek `run()`
caller testi de bu yarışta cleanup başarısını doğrular. Canlı kalan, replacement,
gözlem hatası ve önceki timeout testleri korunur. Bunlar sentetik test sonuçlarıdır.

Python **93/93** geçti: `logs/python-runtime-recovery-09.log`.
Java kaynakları değişmedi; önceki **20/20** sonucu korunur, tekrar çalıştırılmadı.
Mobil 234/234 ve gerçek IT 30/30 da tekrar koşulmadı. Yeni kaynak kimliği/audit
`receipts/implementation-{source-identity,audit}-runtime-recovery-05.private.json`
dosyalarındadır. Ana spec byte düzeyinde aynı ve `in-review` durumundadır;
ana/kurtarma frozen blokları değişmedi.

Gerçek başarılı koşu **03** kimliğine, önceki offline cleanup/test patch'i **04**,
bu tek yarış düzeltmesi **05** kimliğine aittir. Gerçek koşu tekrarlanmadı;
`final-runtime-verification-01.private.json` ve gerçek cleanup sonucu değişmedi.
Bu tur öncesi 185 özel dosya ile 13 eski repo raporu mühürlendi. Önceki 176 dosya,
12 rapor, tokenlar ve diğer eski kayıtlar da bu korumanın içindedir.
