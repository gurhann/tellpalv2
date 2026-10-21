---
title: 'P5c — İki hikâyede yerel kayıpsız dönüşüm denemesi'
type: 'chore'
created: '2026-10-21'
status: 'done'
baseline_commit: 'd99a2c27c46c38dda1320df8bac3dc30aa4397cd'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/standards/commenting-standard.md'
---

<frozen-after-approval reason="Kullanıcının onayladığı yerel deney kapsamı">

## Amaç

**Sorun:** Doğrulanan 41 kaynakta kayıpsız dosya biçimlerinin boyut etkisi bilinmiyor.

**Yaklaşım:** Yalnız #18/TR ve #62/TR için aynı 1712×2778 ölçüde PNG ve kayıpsız WebP kopyaları üret. Açılan pikselleri ve renk profilini karşılaştır; boyut farkını ve yerel görsel inceleme sayfasını sun.

## Sınırlar

**Daima:** Orijinaller/önceki kanıtlar salt okunur. Yeni, özel, repo dışı dizin kullan; önce/sonra SHA eşitliğini doğrula. #62'nin Display P3 ICC'sini aynen koru. Alpha yalnız tüm değerler 255 ise kaldırılır. Renk dönüşümü yok; profilsiz JPEG'e sRGB onayı verilmez. Türkçe raporla.

**Önce sor:** Resize, renk uzayı dönüşümü, kayıplı sıkıştırma, yeni hikâye veya üretim entegrasyonu.

**Asla:** Ağ/DB/storage/credential/cihaz erişimi, canlı değişiklik, upload, job, backfill, deploy, silme veya üzerine yazma. Java/Flutter/CMS/P2 kuralları değişmez. Yerel sonuç üretim/cihaz/kalite/P2 kabulü değildir. JPEG'in geçmiş sıkıştırması geri kazanılmaz; çıktı büyüyebilir.

## Girdi ve uç durumlar

| Durum | Girdi | Beklenen | Hata |
|---|---|---|---|
| Geçerli | 41 doğrulanmış kaynak | 82 aday; tam RGB ve ICC eşitliği | Hatasızsa yerel deney tamam |
| Kaynak farkı | SHA/byte/rol/geometri farkı | Encode öncesi ret | Açık engel |
| Semantik fark | Şeffaf piksel, yeni orientation/profil | Otomatik düzeltme yok | Açık engel |
| Aday farkı | Bozuk/kayıplı çıktı, piksel/ICC farkı | Başarı verilmez | Hata kanıtı korunur |
| Güvensiz çıktı | Var olan yol veya bütçe aşımı | Üzerine yazılmaz | Çalışma durur |

</frozen-after-approval>

## Kod haritası

- `tools/image-optimization/p5b_source_io.py:19`: salt okunur, symlink izlemeyen sınırlı okuma.
- `tools/image-optimization/p5b_inventory.py`: rol/asset ayrımı.
- `be/docs/evidence/p5b-pilot-source-preflight/local-source-observations.json`: 17 JPEG/RGB ve 24 PNG/RGBA; 41 tek kare, opak kaynak.
- Kullanıcı home altındaki `Downloads/pofuduk_misir_pop` kaynak dizini ve `Downloads/Yalnz_metadata_okune_sürümü_kantlanmaz.csv`. CSV SHA: `9d7baf864996f46c283b90e35b7a9d582d748800865f3f2bd7a2148106c882c1`. Eşlemeyi P5b gibi kapak/sayfadan kurup hash ile doğrula.
- #62 ICC: 536 byte, Display P3; SHA `20789fdbea9835251a4f0796c8bf45cbd964896044886540da21ffc7457af0ab`. #18 ICC yok. Her iki grupta EXIF/XMP var; EXIF orientation etiketi yok.
- `be/docs/evidence/local-lossless-formats/probe.py:245`: WebP örneği; eski giriş/validator çalıştırılmaz, ICC'li container farklıdır.

## Görevler ve kabul

**Uygulama:**
- [x] `tools/image-optimization/p5c_local_lossless_compare.py`: P5b girdilerini doğrula; seri, süreli alt süreçte kaynak RGB'sinden PNG (`compress_level=9`) ve WebP (`lossless=True, quality=100, method=6, exact=True`) üret. ICC'yi koru; EXIF/XMP değişimini kaydet. Sınırlar: 64 MiB/kaynak, 40 milyon piksel, 120 saniye/kaynak, toplam 1 GiB çıktı. Yeni dosyaları exclusive oluştur.
- [x] Aynı araç: adayları yeniden açıp tüm RGB byte'larını kaynak decode referansıyla, ICC'yi kaynakla karşılaştır. WebP'de VP8L kodlamayı doğrula; ICC'li VP8X desteklenir. Format/byte/SHA/ölçü/runtime/ayar/süre/metadata/rol kaydı tut. Süre mobil performans değildir.
- [x] `tools/image-optimization/test_p5c_local_lossless_compare.py`: matrisi sentetik girdilerle sına; aynı RGB fakat yanlış ICC ve tek piksellik fark ayrı başarısız olsun.
- [x] `tools/image-optimization/P5C_LOCAL_LOSSLESS.md`: komutlar ve özel dizindeki HTML galerisi. Kaynak/PNG/WebP aynı ölçekte gösterilir; dış kaynak yoktur.
- [x] `be/docs/evidence/p5c-local-lossless/verification.md` ve `summary.json`: gerçek deney; hikâye/format toplamları, büyüyen dosyalar ve koruma kanıtı. Görsel/eşleme/özel yollar Git'e girmez.

**Kabul ölçütleri:**
- Given iki pilot, when ölçüm tamamlanır, then 41 benzersiz kaynaktan 82 çıktı ve 43 rol izlenir; kapak/detail byte'ı iki kez sayılmaz.
- Given bir aday, when kayıpsız denir, then tam RGB ve ICC eşitliği birlikte kanıtlanır.
- Given sonuçlar, when rapor sunulur, then kazanç/büyüme kaynaklara göre ayrı görünür; orijinaller değişmez ve P2/üretim kabulü açık kalır.

## Değişiklik kaydı

- 2026-10-21: Yerel araç, codec/çıktı yardımcıları, 20 matris testi ve kullanım belgesi eklendi. Gerçek denemede 41 kaynak/82 aday/43 rol ve bütün RGB/ICC karşılaştırmaları doğrulandı. Kaynaklar, CSV/mapping ve önceki 272 kanıt dosyası korundu. Özel galeri üretildi; tarayıcı `file:` kısıtı nedeniyle iki kapak ve iki yazılı sayfanın 12 dosyası doğrudan incelendi. #62 WebP önizlemesindeki ton farkı izlenimi açık sınırlama olarak kaydedildi; kalite/cihaz/üretim kabulü verilmedi.

## Doğrulama

- `python3 -m unittest discover -s tools/image-optimization -p 'test_p5c_local_lossless_compare.py'`: bütün matris senaryoları geçer.
- Gerçek çalışmada 41 önce/sonra hash, 82 decode/ICC sonucu ve toplamları denetle. Galeride kapak/yazılı sayfaları incele; cihaz onayı sayma.
- Sonuç: P5c 20/20 ve P5b regresyon 22/22 test geçti. Gerçek dosya doğrulaması `LOCAL_LOSSLESS_COMPLETE`; galeri referansları statik doğrulandı. Tarayıcı render'ı ve görüntüleyici/Flutter renk yönetimi açık kalır. Ayrıntılar: [verification.md](../../be/docs/evidence/p5c-local-lossless/verification.md).
- Son paket kontrolünde özel run dizini artık bulunamadı; nedeni bilinmiyor. Ölçümler erişim kaybolmadan önce doğrulandı ve public özette korunuyor. Mevcut özel dosya erişimi yok; kalıcı, izinli özel dizinde yeniden çıktı üretimi açık. Bu implementation aşamasında ek encode başlatılmadı.
- İnceleme yaması: Başarılı encode sonrasındaki kaynak/CSV/mapping/önceki kanıt değişiklikleri ile toplam byte/tasarruf işareti/yüzde/büyüyen liste hesapları için beş test eklendi. P5c 25/25, P5b regresyon 22/22 geçti. Bellekteki hata ekleme kontrolleri iki eski test açığının kapandığını gösterdi. Encoder/runner ve ilk tarihsel summary değişmedi; yeni gerçek encode yapılmadı.

## Teslim durumu

Yerel deney ve bağımsız inceleme tamamlandı. P5c 25, P5b regresyon 22 test geçti. İlk geçici çıktı dizini sonradan kaybolduğu için aynı kodla izinli kalıcı dizinde yeniden üretim yapıldı; 82 aday ve galeri ilk ölçümle byte olarak aynı. Orijinaller/önceki çalışma dosyaları korundu. P2/üretim/cihaz ve WebP renk görünümü kabulü açık. Güncel sonuç: [kalıcı koşu raporu](../../be/docs/evidence/p5c-local-lossless/rerun-verification.md).

## Önerilen inceleme sırası

**Sonuç ve sınırlar**

- İki hikâyenin boyut kazancı farklı; WebP renk görünümü onayı açık.
  [rerun-verification.md:1](../../be/docs/evidence/p5c-local-lossless/rerun-verification.md#L1)

- İlk koşuyla aynı 82 aday ve kaynak korumasının sayısal kanıtı.
  [rerun-summary.json:1](../../be/docs/evidence/p5c-local-lossless/rerun-summary.json#L1)

**Dönüşüm ve koruma**

- Bütün ön kontroller encode öncesinde biter; süre ve koruma sonucu başarıyı sınırlar.
  [p5c_local_lossless_compare.py:136](../../tools/image-optimization/p5c_local_lossless_compare.py#L136)

- Renk dönüşümü yapmadan tam RGB ve ICC byte eşitliği aranır.
  [p5c_lossless_codec.py:109](../../tools/image-optimization/p5c_lossless_codec.py#L109)

- Yeni özel dosyalar exclusive oluşturulur; büyüme ve tasarruf ayrı hesaplanır.
  [p5c_lossless_output.py:34](../../tools/image-optimization/p5c_lossless_output.py#L34)

**Test ve kullanım**

- Yanlış ICC ve tek piksel farkı ayrı başarısızlık üretir.
  [test_p5c_local_lossless_compare.py:56](../../tools/image-optimization/test_p5c_local_lossless_compare.py#L56)

- Yeni girdiler yerine yalnız onaylı pilotlar ve yerel galeri kullanılır.
  [P5C_LOCAL_LOSSLESS.md:1](../../tools/image-optimization/P5C_LOCAL_LOSSLESS.md#L1)

- İlk ölçüm, görsel inceleme ve üç katmanın bulguları tarihsel olarak korunur.
  [verification.md:1](../../be/docs/evidence/p5c-local-lossless/verification.md#L1)
