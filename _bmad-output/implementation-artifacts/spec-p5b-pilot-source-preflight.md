---
title: 'P5b — Pilot Hikâyelerin Kaynak Doğrulaması ve Geçiş Hazırlığı'
type: 'chore'
created: '2026-10-21'
status: 'done'
delivery_scope: 'local_preparation_tools'
real_pilot_status: 'awaiting_user_inputs'
baseline_commit: 'c8071716fc6b22afb817c5dfdaf741d3b800e08e'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** P5a canlı metadata envanterini tamamladı. Dosyaların gerçek biçimi, bütünlüğü ve piksel özellikleri henüz bilinmiyor.

**Approach:** #18/TR `story.kocaman-bir-sarilma` ve #62/TR `story.pofuduk-misir-pops` için tek SELECT ve yerel dosya inceleyicisi hazırlansın. Kullanıcı sorguyu çalıştırıp CSV ve orijinalleri sağlar; araç dönüşüm öncesi engelleri gösterir.

## Boundaries & Constraints

**Always:** Canlıya ajan bağlanmaz. Kullanıcı DataGrip/IntelliJ kullanır; CSV ve orijinaller özel girdidir. Kaynaklar yalnız okunur. P5a beklentisi 41 dosya, 43 rol bağı ve 126.497.327 bayttır; güncel farklar raporlanır. Kapak ve detay kaynağı paylaşabilir.

**Ask First:** Pilot kapsamını büyütme veya canlıdan otomatik dosya indirme.

**Never:** DB/storage yazma, kaynak değiştirme, dönüşüm, backfill, job, yayın, deploy, credential kullanımı. SQL'den piksel veya yerel kopyadan güncel canlı nesne/sürüm doğrulaması çıkarma. GIF ve eksik PT kapağı düzeltmeleri kapsam dışıdır.

## I/O & Edge-Case Matrix

| Durum | Girdi | Sonuç | Hata davranışı |
|---|---|---|---|
| Normal | Pilot CSV ve eşlenmiş orijinaller | Her rol ve benzersiz kaynak için gözlem | Kaynaklar değişmez |
| Eksik/farklı | Eksik dosya/bağ, SHA veya bayt uyuşmazlığı | Açık engel ve beklenen/gözlenen değer | Başarılı sayılmaz |
| Sorunlu görsel | Bozuk, çok büyük veya destek dışı dosya | Kaynak bazlı hata | Sınırsız decode yapılmaz |
| Eksik kanıt | Orijinal veya kayıtlı SHA yok | Doğrulanamadı | Değer uydurulmaz |

</frozen-after-approval>

## Code Map

- `tools/image-optimization/p5a_csv_inventory.sql`: tablo/alan ve LEFT JOIN örneği; eksik bağları koru.
- `be/docs/evidence/p5a-production-inventory/findings.md`: pilot sayıları ve envanter sınırları.
- `tools/image-optimization/native_entry_config.py`: Pillow verify/yeniden aç/load örneği; launcher import etme.
- `be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingImageInspector.java`: P2 dar PNG/JPEG sözleşmesi; Pillow başarısı P2 kabulü değildir. Bu adımda P2 sonucu `NOT_EVALUATED` kalır.
- `tools/image-optimization/pyproject.toml`: mevcut Pillow bağımlılığı.

## Tasks & Acceptance

**Execution:**
- [x] `tools/image-optimization/p5b_pilot_sources.sql`: tek SELECT ile iki pilotun READING_COVER, READING_DETAIL ve READING_PAGE bağlarını çıkar. Zaman, hikâye/dil, rol/sayfa, asset ID, provider/object_path, MIME, bayt ve SHA döndür; eksik hikâye/çeviri/sayfa/medya bağlarını kaybetme.
- [x] `tools/image-optimization/p5b_local_source_preflight.py`: başlıklı CSV ve JSON `{assetId: yerelDosyaYolu}` eşlemesini doğrula. SHA/bayt, gerçek biçim, ölçü, EXIF/orientation, ICC, alpha/şeffaf piksel, frame ve tam decode gözlemlerini JSON raporla. Symlink/normal olmayan dosyayı reddet; 64 MiB/dosya ve 40 milyon piksel yerel sınırı uygula; girdilerin üzerine yazma. Sınırlar üretim profili değildir. Pilot farklarını ve gözlenmeyen canlı özelliklerini belirt; hatada sıfır dışı çık.
- [x] `tools/image-optimization/test_p5b_local_source_preflight.py`: matrisi sentetik JPEG/PNG ve bozuk/eksik/uyuşmayan girdilerle doğrula; ağ ve gerçek kaynak kullanma.
- [x] `tools/image-optimization/P5B_PILOT_PREFLIGHT.md`: DataGrip CSV export, dosya eşleme ve yerel komutları açıkla. SQL tek başına yeterli değildir; orijinalleri kullanıcı sağlar.
- [x] `be/docs/evidence/p5b-pilot-source-preflight/verification.md`: sentetik test kanıtını ve beklenen gerçek girdileri kaydet. Özel CSV, object_path ve orijinalleri repoya ekleme.

**Acceptance Criteria:**
- Given P5a baz çizgisi, when güncel CSV karşılaştırılır, then rol ve benzersiz dosya farkları ayrı görünür; eksik değerler sıfır kabul edilmez.
- Given yalnız metadata veya sentetik testler, when rapor hazırlanır, then gerçek pilot doğrulaması ve P5b tamamlandı denmez.
- Given yerel dosyalar incelenir, when rapor üretilir, then hiçbir kaynak değişmez; kalite, üretim kabulü ve canlı nesne sürümü doğrulanmış sayılmaz.

## Spec Change Log

## Verification

- `python3 -m unittest discover -s tools/image-optimization -p 'test_p5b_local_source_preflight.py'`: sentetik senaryolar geçer; kaynak hash'leri korunur.
- SELECT'i mevcut migration şeması ve sentetik pilotlarla yerel PostgreSQL'de dene; eksik bağların görünürlüğünü doğrula. Canlı sorguyu ajan çalıştırmaz.

## Teslim durumu

Onaylanan araç hazırlığı tamamlandı. Gerçek pilot kaynak doğrulaması ve P5b kabulü, kullanıcının güncel CSV ve orijinalleri gelene kadar açıktır. İnceleme bulguları düzeltildi; 22 yerel test iki Python/Pillow ortamında, 3 SQL testi geçici PostgreSQL 18.6 üzerinde geçti. Frozen kapsam değişmedi.

## Önerilen inceleme sırası

**Kullanıcı akışı**

- Tek SELECT, başlıklı CSV ve orijinal dosya eşleme adımları.
  [P5B_PILOT_PREFLIGHT.md:1](../../tools/image-optimization/P5B_PILOT_PREFLIGHT.md#L1)

- Eksik bağlar korunur; aşırı placeholder üretimi sınırlıdır.
  [p5b_pilot_sources.sql:1](../../tools/image-optimization/p5b_pilot_sources.sql#L1)

**Kaynak gözlemi**

- Yerel rapor üretimi, eşleşme sonuçları ve açık kabul sınırları.
  [p5b_local_source_preflight.py:69](../../tools/image-optimization/p5b_local_source_preflight.py#L69)

- Benzersiz kaynak sayımları ve çelişkili metadata toplamları.
  [p5b_inventory.py:109](../../tools/image-optimization/p5b_inventory.py#L109)

- EXIF, alpha ve decode gözlemi P2 kabulünden ayrılır.
  [p5b_image_probe.py:13](../../tools/image-optimization/p5b_image_probe.py#L13)

- Kaynaklar salt okunur açılır; var olan dosyanın üzerine rapor yazılmaz.
  [p5b_source_io.py:19](../../tools/image-optimization/p5b_source_io.py#L19)

**Doğrulama**

- SHA hatası ve kaynakların değişmeden kalması sentetik dosyalarla sınanır.
  [test_p5b_local_source_preflight.py:118](../../tools/image-optimization/test_p5b_local_source_preflight.py#L118)

- Gerçek SELECT çıktısı yerel inceleyiciye bağlanır.
  [test_p5b_pilot_sql.py:110](../../tools/image-optimization/test_p5b_pilot_sql.py#L110)

- Test sonuçları ve beklenen gerçek girdiler kaydedilir.
  [verification.md:1](../../be/docs/evidence/p5b-pilot-source-preflight/verification.md#L1)
