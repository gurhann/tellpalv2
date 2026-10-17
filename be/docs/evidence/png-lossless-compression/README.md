# PNG kayıpsız sıkıştırma kanıtı

2026-10-18, yerel dosyalarla **PASS**. Üç sabit PNG kaynağından gerçek eski/yeni paketli motor
çalıştırıldı. Eski çıktı SHA/byte değerleri [#94 Android ölçümü](../p4-reading-images/android-measurement-2026-10-18/verification.json)
ile eşleşti. Yeni çıktılar **822×1334** kaldı. Her görüntüde **3.289.644 decoded RGB byte'ının tamamı**
eski çıktıyla aynıydı. Toplam 9.878.163 → 5.112.408 byte; kazanç **%48.25**.

| Kaynak | Eski PNG byte | Yeni PNG byte | PNG kazancı | Mevcut JPEG byte | Yeni PNG / JPEG |
|---|---:|---:|---:|---:|---:|
| Kapak/detail (`9778`) | 3.292.721 | 1.765.704 | %46.38 | 1.048.225 | 1.684× |
| Sayfa 1 (`9816`) | 3.292.721 | 1.658.329 | %49.64 | 1.122.418 | 1.477× |
| Sayfa 2 (`9818`) | 3.292.721 | 1.688.375 | %48.72 | 1.122.572 | 1.504× |

Mevcut JPEG dosyaları 1712×2778 boyutundadır; bunlar önceki gerçek dosyalardır, yeni JPEG
üretimi değildir. Yeni PNG'ler bu JPEG'lerden hâlâ büyüktür. Bu tablo format veya fiziksel
kalite kabulü vermez. Kaynak/çıktı/RGB hash'leri, profil descriptor'ları ve bütün karşılaştırmalar
[verification.json](verification.json) içindedir.

## Yöntem ve korumalar

- [PngLosslessProbe.java](PngLosslessProbe.java), `ReadingImageProcess.convert` girişini kullanır.
  Parent tek gerçek executable JAR classpath'iyle `PropertiesLauncher` üzerinden açılır. Gerçek
  child aynı JAR'ın paketli yolunu kullanır; test stub'ı veya yeniden yazılmış encoder yoktur.
- Kaynaklar mevcut `canonical-review-9778.png`, `canonical-review-9816.png` ve
  `canonical-review-9818.png` dosyalarıdır. İndirme yapılmadı. Eski JAR ve ayrılmış baseline
  dosyaları salt okunur kaldı; yeni JAR ayrı run dizinine kopyalandı.
- Eski ve yeni descriptor'da yalnız sıkıştırma ayarı ve motor binary kimlikleri farklıdır.
  Aynı 823×1334 hedef, kaynak/RGB/metadata kabulü, bütün kaynak/süreç limitleri ve kapalı growth
  ayarı kullanıldı. `compressionQuality=1.0` → `0.0`; tüm rollerin fingerprint/rendition kimlikleri değişti.
- Paketli parent ve image child JVM'lerine macOS `sandbox-exec` ile `deny network*` uygulandı.
  Loopback bind'in de `Operation not permitted` ile reddi doğrulandı. macOS setuid `/bin/ps`
  exec'ini sandbox içinde engellediği için ilk deneme fail-closed kaldı;
  [ham hata](probe-setuid-rejection.log) korunur. Başarılı çalışmada yalnız değişmemiş sistem
  `/bin/ps` yürütmesine `(with no-sandbox)` istisnası verildi. Bu komut mevcut child PID'sinin
  RSS'ini okur; parent/child JVM'lerinin ağ yasağını kaldırmaz. Sistem dosyası veya motor sınırı değiştirilmedi.
- Bağımsız Pillow 12.3.0 tam decode ile bütün RGB byte'ları karşılaştırıldı; örnek piksel
  veya yalnız metadata kontrolü kullanılmadı. Java runtime: Homebrew 25.0.2, macOS/aarch64.
- Baseline JAR, kaynaklar, eski run/manifest/ledger/hata kayıtları ve ilgisiz `.memlog.md` dahil
  **109 dosyanın** işlem öncesi/sonrası SHA değeri değişmedi. Önceden kayıtlı ledger,
  hata ve `.memlog.md` hash'leri ayrıca eski kanıtla karşılaştırıldı. DB, #94 LIVE, bulut ve oturum değişikliği yapılmadı.

## Süreler

| Kaynak | Eski süreç ms | Yeni süreç ms | Pillow eski decode ms | Pillow yeni decode ms |
|---|---:|---:|---:|---:|
| Kapak/detail | 1180.63 | 1310.63 | 9.42 | 10.45 |
| Sayfa 1 | 1119.28 | 1291.83 | 1.62 | 10.59 |
| Sayfa 2 | 1147.61 | 1306.11 | 1.62 | 10.56 |

Süreç süresi child başlatma, runtime/descriptor handshake, decode, resize, encode, RSS/disk
izleme, çıktı okuma ve geçici dosya temizliğini kapsar; **saf encode değildir**. Pillow süreleri
ayrı tam decode ölçümüdür. Bunlar tek yerel örneklerdir; benchmark, cihaz gecikmesi veya
üretim performansı kabulü değildir.

## Test matrisi

| Durum | Çalışan kanıt |
|---|---|
| Gerçek üç kaynak, eski #94 SHA/byte, tam RGB eşitliği | Paketli eski/yeni probe ve bağımsız Pillow: üçü de PASS. |
| Sıkıştırılabilir fixture; `1.0` regresyonu | `realChildPreservesEveryRgbPixelAndCompressesAgainstUncompressedControl`: gerçek child; diskten tam RGB eşitliği ve `1.0` kontrol boyutunun dörtte birinden küçük çıktı zorunlu. |
| Yeni fingerprint/job/rendition; eski pending dönüşüm öncesi reddi | `oldPngEncoderIdentityGetsDistinctWorkAndIsRejectedBeforeConversion`: aynı kaynak/limit; farklı kimlik; eski iş `ENCODER_RUNTIME_PROFILE_CHANGED`; dönüşüm çağrısı 0; yalnız yeni manifest. |
| Byte sınırı; eşit/büyük çıktı için açık growth kabulü | Gerçek child `OUTPUT_BYTE_LIMIT`; `equalPngBytesRequireExplicitGrowthAdmissionBeforeRealWorkerCanComplete` ve `growingPngBytesRequireExplicitGrowthAdmissionBeforeRealWorkerCanComplete`: ön byte koşulu, growth reddi ve açık kabul. Büyüyen sentetik checkerboard 44×33 → 40×30. |

[Odaklı test kaydı](focused-tests.log): **27 test**, 0 hata/başarısızlık/atlama.
[Tam `bash ./mvnw verify` kaydı](full-verify.log): **492 Surefire + 24 Failsafe = 516 test**,
0 hata/başarısızlık/atlama. Mevcut Colima default socket'i yalnız test PostgreSQL için kullanıldı;
Docker context `desktop-linux` olarak kaldı. Yalnız bu işte başlatılan test VM'i çalışma sonunda kapatıldı.

## Python optimizasyonu koruması

`probe.py`, `main()` girişinde `__debug__` kapalıysa açık `RuntimeError` ile durur;
`assert` kontrolleri kaldırılmış bir çalışma başarı üretemez. Bu ret argüman işleme,
dosya veya süreç I/O'sundan önce gelir.

[Çalışan test](test_optimization_guard.py), gerçek `-O` ve `PYTHONOPTIMIZE=1` girişlerinin
ikisini de `PYTHON_OPTIMIZATION_NOT_SUPPORTED` ile reddetti. Ayrı optimize subprocess'lerde
argparser, Path/open, kopyalama, ZIP/Pillow okuma ve süreç çağrılarına kurulan trap'ler hiç
çağrılmadı: iki durumda da `calls=[]`. [Ham test logu](optimization-guard-tests.log): **2 test PASS**.
Testin `unittest.assertEqual` kontrolleri optimizasyonla kaldırılmaz.

Düzeltme sonrası normal ağsız üç kaynak probe'u yeni run dizininde tekrar **PASS** verdi;
yukarıdaki süreler bu yeni çalışmaya aittir. Java/JAR değişmediği için 516 testlik tam verify
kanıtı korundu. Önceki public [verification.json sürümü](verification-before-optimization-guard.json)
ve [probe logu](probe-before-optimization-guard.log) ayrı saklandı.
[Hash koruma kaydı](optimization-guard-preservation.json), önceki iki local run ve arşivlenen
kanıtlardaki **17 dosyanın** değişmediğini ve eski/yeni public kanıt SHA değerlerini tutar.

## Yeniden çalıştırma ve artefaktlar

Repo kökünden, var olmayan yeni bir run dizini seçilerek:

```sh
python3 be/docs/evidence/png-lossless-compression/probe.py \
  --project-root "$PWD" \
  --new-jar be/target/tellpal-v2-backend-0.0.1-SNAPSHOT.jar \
  --workdir be/var/png-lossless-run-YENI-KIMLIK \
  --java-home /opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home
```

Bu macOS probe'u mevcut kaynaklar, baseline JAR, dependency classpath ve derlenmiş
`be/target/classes` ister. Çalışan komut [probe.py](probe.py) içindedir; [sonuç logu](probe.log)
başarı özetini tutar. Ağ erişimi, kimlik veya piksel farkında başarı kaydı yazılmaz.

- Eski JAR SHA-256: `1f90df8d50d5f7f85618480fb36dffa5ff4c7b1072d701c36fc79f1560e3ef6c`.
- Yeni JAR SHA-256: `618e80178b8734c05d5b71abb6913528c264d7d773864bde3745e2079cbc70b4`.
- Yeni paket ve tam PNG çıktıları ignored yerel
  [run dizininde](../../../var/png-lossless-run-20261018-c1f43a2e/); `candidate.jar`, `old/` ve `new/` altında saklanır.
  [Önceki başarılı run](../../../var/png-lossless-run-20261018-e81f83b0/) ve önceki başarısız deneme dizini de korunur.

Üretim profili, fiziksel Android/iPhone kalite kabulü, JPEG/WebP seçimi, P4b ve P0 eksik ham
kanıt kapıları açık kalır. Bu çalışma yeni bulut/oturum/cihaz penceresi veya normal tüketici geçişi açmaz.
