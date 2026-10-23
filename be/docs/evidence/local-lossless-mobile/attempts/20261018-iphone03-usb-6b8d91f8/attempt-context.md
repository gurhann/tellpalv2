# Üçüncü iPhone denemesi: USB önkoşulu ve erken kapatma

Resmi sonuç **FAIL / DRIVER_FAILED**. Offline profile build geçti. İstenen bağlantı `wired` idi; çalıştırmanın kendi cihaz kaydı **`localNetwork`** gösterdi. Daha önceki `wired` ön kontrolü bu denemenin USB kanıtı sayılmadı. Bağlantı değişiminin kesin nedeni belirlenmedi.

Root'un yetkilendirdiği erken kapatma, yalnız private snapshot cwd, parent ve log handle bağı doğrulanan Flutter süreç grubuna **SIGINT** gönderdi. Komut **132826 ms** sonra sıfır çıkış koduyla kapandı; ham `toolStatus:ok` korundu. Driver sonucu oluşmadığından bu çıkış kodu PASS sayılmadı. Bu deneme timeout değildir.

Fiziksel ölçüm **NOT_RUN**: planlanan altı durumdan **0 kabul edilen durum**, **0 runtime örneği**, **0 screenshot**. Native decode, tam RGB, paint, fiziksel piksel ızgarası, Dart owned-handle stop ve RSS doğrulanmadı.

Erken kapatma sonrası own host grubunda ve exact kurulu QA bundle/install executable eşleşmesinde kalan süreç sayısı **0**. Ek host sinyali ve native stop gönderilmedi. Native süreç yokluğu Dart kaynak bırakma kabulü değildir.

İlgili QA kaynakları önceki 97 Flutter testi ve hedefli analyze ile aynı; donmuş deneme öncesi 26 core Python testi geçti. Mevcut fixture/signing çıktıları, önceki iki iPhone private kaydı, emülatör hazırlık FAIL kaydı, eski public raporlar ve `.memlog.md` hash'leri korundu. Hash bağları `attempt-context.json` içindedir.

Renderer gözlenmedi. HTTP ve gerçek OS background testi yapılmadı. İnsan kalite, tam genişlik reader, P0/P4b ve üretim kabulü açık. Yeni iPhone denemesi başlatılmadı.
