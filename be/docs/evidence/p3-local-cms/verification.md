# P3 yerel CMS deneme kanıtı

2026-10-17 tarihinde kullanıcının açık onayıyla tek sentetik STORY üzerinde çalışıldı. Yeni hikâye `#93`, dil `tr`; mevcut 91 içerik ve medya bağları korundu. Bu, üretim veya cihaz kalite onayı değildir.

## Gerçek sonuç

- CMS’de mevcut `tellpal_yonet` oturumundan **Dili yayina al** tıklandı. İlk durum `PENDING` oldu.
- Yerel profilin açıklaması P1 ASCII kimlik sözleşmesine uymadığı için ilk kimlik hesaplama durdu. O anda görsel işi ve manifest sayısı sıfırdı; bulut çıktı yazısı başlamamıştı.
- Açıklama yerine ASCII teknik kanıt kodu kullanıldı. Aynı kaynak/admission, prefix, geometri ve limitler korundu; seed veya kaynak kabulü tekrarlanmadı. Profil kimliği değişimi istenen generation değerini `2` yaptı.
- Gerçek scheduler ve child encoder, işi ilk attempt içinde `COMPLETED` yaptı. Güncel generation otomatik `LIVE` oldu; diagnostic yok, `live_eligible=true`.
- Kapak, detail ve tek sayfa çıktılarının her biri 40×30, 3.698 byte PNG. Fresh pinned stored-object verification başarılı; availability epoch `2`, kontrol zamanı `2026-10-17T14:44:32.289587+03:00`.

| Alan | Değer |
|---|---|
| Job | `1c61e316-9df1-4994-be1a-167a833a747c` |
| Revision | `5d7bc3793d933e2b30f09ebbd63d590e798ecd3e736728004b91e19ab2ef264e` |
| Manifest digest | `308d8b3f997111f9fc5c936e7dc9c1200fd0ef897948e291f39702513ef0f063` |
| Runtime | Homebrew OpenJDK 25.0.2, macOS, aarch64 |
| Encoder | `com.sun.imageio.plugins.png.PNGImageWriter`, PNG/Deflate, metadata yok |

Makine özeti [live-result.json](live-result.json) içindedir. Manifest, pinned nesne sürümleri, SHA-256 değerleri, profil/runtime ve fresh doğrulama sonucu bu özette bulunur. Tam DB kanıtı, önce/sonra karşılaştırmaları ve ham loglar Git dışındaki özel `be/var/p3-local-cms/` dizininde tutulur; token veya özel indirme URL’si bu belgeye alınmadı.

## Kontroller ve sınırlar

- Tam backend `bash ./mvnw -o verify`: 467 Surefire ve 1 Failsafe testi başarılı; hata veya atlama yok.
- Son inceleme düzeltmeleri sonrası odaklı Java testleri: 14/14; P3 Python 16/16, tüm launcher Python testleri 34/34 başarılı. Gerçek helper admission→seed→Publish→LIVE entegrasyonu disk storage ve Testcontainers PostgreSQL ile ayrıca geçti. Offline `--prepare` başarılı. Standart CI akışına aynı Python komutu eklendi; uzak CI çalıştırılmadı.
- Gerçek preflight profil/DB çakışmalarını buldu; çocuk süreç env önceliği düzeltildi ve mevcut path builder’a yalnız güvenli local/test alt prefix uyumu eklendi.
- Kaynak 80×60 opak RGB8 PNG’dir. Kaynak, admitted ve retained kopyalar ile üç çıktı toplam altı nesne kimliğine karşılık gelir. Her kaynak kopya 14.528 byte; kayıtlı toplam 54.678 byte’tır. Silme, backfill veya dağıtım yapılmadı.
- Eski medya işleme poller’ı bu denemede kapalıdır. CMS’nin eski mobil/işleme sayacı P3 LIVE göstergesi değildir. Ses yalnız LOCAL_STUB kaydıdır; bu deneme sesli hikâye veya P4 tüketici testi sayılmaz.
- Chrome’daki son kart kontrolü tamamlandı: **“Doğrulanmış okuma revizyonu benimsendi”** ve yerel/test kapsam açıklaması mevcut oturumda gözlendi. Kart hash göstermez; revision ve manifest digest API/DB kanıtından doğrulandı.

Son bağımsız incelemenin beş bulgusu kapatıldı; ayrıntılar [inceleme kaydı](review.md) içindedir. Eski canlı makbuz, #93 LIVE/job kimliği aynı yerel DB üzerinde doğrulandıktan sonra DB URL alanıyla tamamlandı. Yeni seed, admission veya yayın isteği oluşturulmadı. Backend ve CMS son kontrolde HTTP 200 verdi.
