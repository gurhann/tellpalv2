# Yayın korumaları review doğrulaması

Sonuç: **PASS**. Bu kayıt, tamamlanan yerel format deneyinin review düzeltmesini açıklar. Gerçek format deneyi tekrar çalıştırılmadı.

Ölçümü üreten `probe.py`, kaynak değiştirilmeden önce final özel run içinde `measured-probe.py` adıyla exclusive kopyalandı. Kopyanın SHA256 değeri public kanıttaki `probeSha256` ile aynı: `05382966259933e6177c04ecb9e06fcc60b14a6e8710f8752fde65f452634461`.

Ölçüm kaydı `verification.json` ve `verification.md` değişmedi. Ölçümler korunmuş kaynak kopyasına aittir. Güncel `probe.py` farkı yalnız `publish` işlevinin `except` dalındadır. Bir kapatma hatası diğer stream'lerin kapatılmasını veya bu çağrının oluşturduğu dosyaların temizlenmesini atlatamaz. İlk exception korunur; ikincil hatalar note olarak eklenir. AST karşılaştırması işlevin kalan kısmının, encode/decode yollarının ve bütün parametrelerin aynı olduğunu doğruladı.

Tamamlanmış test komutu: `python3 -B -m unittest discover -s be/docs/evidence/local-lossless-formats -p 'test_*.py' -v`. Sonuç: **34 test geçti**, exit code `0`; unittest çıktısı `Ran 34 tests in 2.367s` ve `OK`. Sidecar hazırlanırken bu koşu tekrarlanmadı.

Yeni üç test yalnız geçici, sentetik dosyalar kullanır:

- Worker → measure_image yolunda aynı uzunluktaki native RGB çıktılarında tek byte farkı `SIPS_FULL_RGB_MISMATCH` üretir. Özel run içinde FAIL kaydı kalır; public PASS oluşmaz.
- Ölçüm sırasında sentetik korunan dosya değiştiğinde `PROTECTED_ARTIFACT_CHANGED` oluşur. Özel FAIL kaydı ve public PASS yokluğu doğrulanır.
- Yayın hata temizliğinde bir stream'in `close` hatası diğer stream'in kapanmasını ve yalnız yeni dosyaların temizlenmesini engellemez. İlk exception nesnesi ve mevcut bağımsız dosya korunur.

Önceki 31 test dosyası değişmedi. Review başlangıcında hash'i alınan 180 dosyanın önce/sonra değerleri eşit; listeler JSON kaydındadır. Bu liste eski kanıtları, final ölçüm çıktıları ve kaynak kopyasını, public doğrulama dosyalarını, `.memlog.md` dosyasını ve spec'i kapsar. `git diff --check` geçti. Frozen spec bölümü değişmedi.

| Dosya | SHA256 |
|---|---|
| `probe.py` | `00a5da622d84cfde592b8a45eac5e90ab7fd329b02e3d48a7217721e78cde3b4` |
| `test_probe.py` | `98e1b427e6fb905f6100db994dc685dbd91d05aaafe7d3bdeec9a3775598876c` |
| `test_publication_guards.py` | `09351fc1666fa3ceaa914e089f084b4f02706dd5e0f9f59b72897c31fddc5903` |
| `verification.json` | `16adae98244f6821cdb15f81dcca4a7640c2075cbeaa2cbecfc34786a1e78100` |
| `verification.md` | `dfa80aeeb60409e24558888e0350bde62e4bcc70fca5c81bcf2a72d8c8fe6bb3` |

Ölçülmüş kaynak kopyası: `be/var/local-lossless-formats-yqpxi6z1/measured-probe.py`.

P0 ham kanıt sınırı ve P4b açık kalır. Bu review testleri hata akışlarını sentetik RGB çıktılarıyla doğrular; yeni format, decode veya mobil performans ölçümü üretmez.
