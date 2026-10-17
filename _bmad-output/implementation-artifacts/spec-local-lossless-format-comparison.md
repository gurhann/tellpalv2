---
title: 'Üç mevcut görselde yerel kayıpsız format karşılaştırması'
type: 'chore'
created: '2026-10-18'
status: 'done'
baseline_commit: 'ac6c9720a60d908f433bc3e46c75eb158545f5d7'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/standards/commenting-standard.md'
  - '{project-root}/be/docs/evidence/png-lossless-compression/probe.py'
  - '{project-root}/be/docs/evidence/png-lossless-compression/test_optimization_guard.py'
---

<frozen-after-approval reason="Kullanıcının onayladığı dar yerel deney; yalnız kullanıcı değiştirebilir">

## Intent

**Sorun:** Düzeltilmiş PNG hâlâ eski JPEG'den büyük. Aynı pikselleri kayıpsız WebP ile saklamanın boyut ve yerel decode etkisini bilmiyoruz.

**Yaklaşım:** Yalnız 9778, 9816, 9818 için mevcut 822×1334 PNG → kayıpsız WebP deneyi. PNG ve WebP aynı geometride karşılaştırılır. Değişmeyen 1712×2778 JPEG ayrı byte referansıdır.

## Boundaries & Constraints

**Daima:** Ağ kapalı; girdiler ve eski kayıtlar hash ile korunur. Tek aday: Pillow `lossless=True, quality=100, method=6, exact=True`; yalnız deney ayarıdır. Üç WebP; başka resize veya kalite taraması yok. Açık hata denetimleri kullan; Python optimize modunu I/O öncesi reddet. Her çalışmada yeni, özel run dizini; eski dosyaları ezme. Türkçe belge ve yorumlar.

**Önce sor:** Bulut, DB, giriş, cihaz oturumu, paket kurulumu, üretim profili veya uygulama codec değişikliği gerektiren kapsam genişlemesi.

**Asla:** #94'e, eski kanıta veya `.memlog.md` değişikliğine dokunma. Java/Flutter/CMS, altyapı, retry, dağıtım ve backfill değiştirme. JPEG'i yeniden encode etme. Yerel byte'ı gerçek transfer, süreyi mobil performans veya piksel eşitliğini fiziksel kalite onayı olarak sunma. P0 ham kanıt sınırı ve P4b açık kalır.

## I/O & Edge-Case Matrix

| Durum | Girdi | Beklenen | Hata |
|---|---|---|---|
| Geçerli | Sabit üç PNG | Üç VP8L WebP; tüm RGB byte'ları aynı | Uyuşmazlıkta PASS yok |
| Bozuk girdi | Yanlış hash/geometri/mod/metadata | Encode öncesi ret | Açık hata |
| Sahte kayıpsız | Lossy veya bozuk RIFF | Ret | PASS yok |
| Güvensiz çalışma | `-O`, ağ yasağı yok veya mevcut çıktı | I/O öncesi ret / yeni çıktı yok | Açık hata |
| Kanıt değişikliği | Korunan dosya değişmiş | Ret | PASS yok |

</frozen-after-approval>

## Code Map

- Önceki `probe.py`: tam RGB/hash ve koruma örneği; yeniden çalıştırma, eski verification üzerine yazar.
- `be/docs/evidence/png-lossless-compression/verification.json`: altın `images[]` SHA/RGB pinleri. SHA256 `6d3b7a4432a8f94b6ff073d332cbcd98dd0e4ac2338d2f6b46606e1c030a7ce2`.
- `be/var/png-lossless-run-20261018-c1f43a2e/new/{id}.png`: mevcut düzeltilmiş girdiler; gerçek adları dizinden doğrula.
- `be/var/p4a2-preflight-dc591777-9e53-4e14-856f-f13fec00fa8c/source-{id}.jpg`: JPEG kontrolleri; eski kanıtta SHA/byte pinleri.
- Koruma: önceki PNG evidence dizini, PNG run dizinleri, #94 `be/var/p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09`, preflight dizini, `.memlog.md`. İçerik yayımlama; yalnız hash listesi.
- Yerel Python 3.14.6 / Pillow 12.3.0 / libwebp 1.6.0 mevcut. `/usr/bin/sips` ImageIO decoder'ı mevcut; format listesi tek başına decode kanıtı sayılmaz.

## Tasks & Acceptance

**Uygulama:**
- [x] `be/docs/evidence/local-lossless-formats/probe.py`: dar ağsız deney; sabit girdiler, sınırlandırılmış süreler, özel run; tüm çıktı/hash/parametre/sürüm kaydı.
- [x] `be/docs/evidence/local-lossless-formats/test_probe.py`: matris satırlarını anlamlı olumsuz testlerle doğrula; eski dosyaları kullanarak hata üretme.
- [x] `be/docs/evidence/local-lossless-formats/verification.json`: gerçek çalışmayı tamamla; ham decode örnekleri, ölçüm sınırları, koruma önce/sonra hash'leri ve tüm RGB eşitliği.
- [x] `be/docs/evidence/local-lossless-formats/verification.md`: Türkçe sonuç; aynı geometri PNG↔WebP tablosu, ayrı JPEG referansı ve açık kalan kapılar.

- [x] `be/docs/evidence/local-lossless-formats/test_publication_guards.py`: incelemede bulunan iki yayınlama korumasını ve cleanup hatasını sentetik worker testleriyle kapat.
- [x] `be/docs/evidence/local-lossless-formats/review-verification.json` ve `.md`: orijinal kanıtı değiştirmeden kaynak bağı ve inceleme doğrulamasını kaydet.

**Kabul ölçütleri:**
- Given sabit girdiler, when deney biter, then üç dosyanın her birinde 3.289.644 RGB byte'ı altın pinle eşleşir; format/tek kare/822×1334/opaque RGB/metadata denetlenir.
- Given PNG ve WebP, when `/usr/bin/sips` her ikisini tam PNG'ye decode eder, then bu bağımsız decoder içindeki bütün RGB byte'ları eşittir. Başarısızlık gizlenmez.
- Given yüklü RGB, when WebP encode ölçülür, then süre yalnız belleğe encode'u kapsar. PNG/WebP `open+load` için beş dönüşümlü örnek ve medyan kaydedilir; sips başlangıç+decode+PNG export süresi ayrıdır. Tarihî Java süresiyle kıyas yok.
- Given deney, when tamamlanır, then eski kayıtların hash'leri değişmez; gerçek loopback bind EPERM ile ağ yasağını kanıtlar. Native alt süreç de aynı yasağı devralır.

## Spec Change Log

İnceleme yamaları: native RGB uyuşmazlığı ve korunan dosya değişikliğinde worker’ın PASS yayımlamadığını sınayan iki akış testi eklenecek. Public kanıt yazımı hata verdiğinde cleanup da hata verirse kalan kapatmaların atlanması düzeltilecek. Tamamlanmış ölçüm kaynak kopyasıyla korunacak; yalnız test ve cleanup değişikliği için ölçüm yinelenmeyecek.

## Verification

- `python3 -m unittest discover -s be/docs/evidence/local-lossless-formats -p 'test_*.py' -v`: bütün matris testleri geçer.
- `python3 be/docs/evidence/local-lossless-formats/probe.py`: yeni özel run ve gerçek ölçüm; yalnız tüm kontrollerden sonra yeni public kanıtı exclusive oluşturur. Hata kayıtlarını korur.
- `git diff --check`: kaynak/belge temiz. Üretim kodu değişmediği için Maven/Flutter tekrar çalıştırılmaz.

Kapsam, kullanıcının “Olur” yanıtıyla onaylanan üç dosyalık yerel deneydir. Yeni ürün politikası değildir.

Doğrulama: 31 test geçti. Matrisin beş satırı testlerle ve üç gerçek dosyalık PASS kanıtıyla karşılandı. 161 korunan dosyanın hash değerleri bağımsız olarak yeniden kontrol edildi. PNG toplamı 5.112.408 byte, WebP toplamı 3.020.382 byte; tasarruf %40,92. Her iki decoder içinde tam RGB eşitliği doğrulandı.

İnceleme tamamlandı: üç bağımsız katman çalıştı. Üç yama kapatıldı; toplam 34 test geçti. İnceleme kaydındaki 180 dosyanın hash değerleri bağımsız denetimde eşleşti. Bundan sonraki spec değişikliği yalnız tamamlanma durumu ve inceleme bağlantılarıdır. Orijinal deney yeniden çalıştırılmadı.

## Suggested Review Order

**Sonuç ve kapsam**

- Aynı geometrideki boyut kazanımını ve tam piksel eşitliğini incele.
  [verification.md:5](../../be/docs/evidence/local-lossless-formats/verification.md#L5)

- Yerel sürelerin sınırını ve farklı geometrili JPEG referansını ayırt et.
  [verification.md:15](../../be/docs/evidence/local-lossless-formats/verification.md#L15)

- Ölçülmüş kaynak ile inceleme sonrası kaynak arasındaki bağı doğrula.
  [review-verification.md:5](../../be/docs/evidence/local-lossless-formats/review-verification.md#L5)

**Girdi ve yayın korumaları**

- Sabit girdiler, ağ yasağı ve eski kanıt korumasının birleştiği giriş.
  [probe.py:370](../../be/docs/evidence/local-lossless-formats/probe.py#L370)

- Gerçek lossless container ve bütün RGB byte denetimi.
  [probe.py:202](../../be/docs/evidence/local-lossless-formats/probe.py#L202)

- Yayın hatasında ilk hata korunur; kalan cleanup girişimleri devam eder.
  [probe.py:74](../../be/docs/evidence/local-lossless-formats/probe.py#L74)

**Testler**

- Native RGB farkı ve korunan dosya değişiminde PASS engellenir.
  [test_publication_guards.py:82](../../be/docs/evidence/local-lossless-formats/test_publication_guards.py#L82)

- Cleanup hatası diğer kapatmaları engellemez ve mevcut dosyaları korur.
  [test_publication_guards.py:97](../../be/docs/evidence/local-lossless-formats/test_publication_guards.py#L97)
