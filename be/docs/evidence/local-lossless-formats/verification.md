# Yerel kayıpsız format karşılaştırması

Sonuç: **PASS**. Ölçüm: 2026-10-17T22:16:25Z. Üç mevcut PNG aynı geometride kayıpsız WebP ile karşılaştırıldı.

## Aynı geometride PNG ↔ WebP

Her dosya 822×1334, tek kare ve opaque RGB. Her birinde **3.289.644 RGB byte** altın pinle ve PNG ↔ WebP arasında tamamen eşleşti. RIFF yalnız bir `VP8L` chunk içeriyor; ICC/EXIF/XMP, alpha ve animasyon yok.

| Görsel | PNG byte | WebP byte | Tasarruf byte | Tasarruf |
|---|---:|---:|---:|---:|
| 9778 | 1,765,704 | 1,017,010 | 748,694 | %42.40 |
| 9816 | 1,658,329 | 993,082 | 665,247 | %40.12 |
| 9818 | 1,688,375 | 1,010,290 | 678,085 | %40.16 |

## Yerel süreler

Encode süresi önceden yüklenmiş RGB’nin yalnız belleğe WebP encode işlemini kapsar. Pillow encoder kayıtları süre başlamadan yüklendi. Dosya okuma, RGB hazırlama, doğrulama ve diske yazma kapsam dışıdır. Pillow `open+load` için beş tur dönüşümlü sıra kullanıldı; tabloda medyan var. Ham on örnek JSON içindedir. Önbellek temizlenmedi; encode ısınması yapılmadı. Bu dar örnekler benchmark veya mobil performans kanıtı değildir.

| Görsel | WebP belleğe encode ms | PNG open+load medyan ms | WebP open+load medyan ms | sips PNG ms | sips WebP ms |
|---|---:|---:|---:|---:|---:|
| 9778 | 6109.171 | 10.059 | 20.243 | 107.473 | 103.421 |
| 9816 | 7655.961 | 9.972 | 20.286 | 95.231 | 100.486 |
| 9818 | 7430.879 | 10.419 | 20.165 | 92.543 | 99.751 |

`/usr/bin/sips` her PNG ve WebP’yi ayrı, tam PNG çıktısına decode etti. ImageIO içindeki bütün RGB byte’ları eşleşti. sips süreleri süreç başlangıcı + decode + PNG export + süreç bitişini içerir; Pillow veya tarihî Java süreleriyle kıyaslanmaz. sips çıktıları Pillow ile yalnız RGB byte karşılaştırması için okundu; bağımsız kaynak decode işlemini ImageIO yaptı.

## Ayrı JPEG byte referansı

Aşağıdaki değişmeyen JPEG’ler **1712×2778** boyutundadır. Yeniden encode edilmedi. Geometri farklı olduğundan yalnız mevcut dosya boyutu referansıdır; format veya kalite karşılaştırması değildir.

| Görsel | Mevcut JPEG byte | SHA256 |
|---|---:|---|
| 9778 | 1,048,225 | `03f4ed692f96678afb1a0aeab03bc58ab97b25020b417a51b6958adbce199c6e` |
| 9816 | 1,122,418 | `24d27b3384c5ecf706f1c4458bc3bc869a2e55e85e4ca46e449a4e5dbdcbe075` |
| 9818 | 1,122,572 | `f8a4d63118b9131bd51aee9d6035faf88d9a397a7f691bd77844b9108baa72f1` |

## Tekrarlanabilirlik ve sınırlar

Python 3.14.6; Pillow 12.3.0; libwebp 1.6.0; sips-316; macOS-27.0.1-arm64-arm-64bit-Mach-O. Tek deney ayarı: `lossless=True, quality=100, method=6, exact=True`. Başka aday, resize veya kalite taraması yok.

Çalışma dizini: `be/var/local-lossless-formats-yqpxi6z1` (izin `0700`). WebP’ler ve sips PNG çıktıları bu özel dizindedir. Public JSON yalnız hash, parametre, sürüm ve ölçüm kaydı içerir; korunan dosyaların içeriği yayımlanmaz.

Önceki PNG evidence, tüm eski PNG run dizinleri, #94 dizini, preflight ve `.memlog.md` dahil 161 korunan dosyanın önce/sonra hash listeleri eşit. Altın verification SHA256: `6d3b7a4432a8f94b6ff073d332cbcd98dd0e4ac2338d2f6b46606e1c030a7ce2`. Başlangıçta var olan `.memlog.md` içeriği korunur.

Ölçüm düzeltmesi: `be/var/local-lossless-formats-vbqqt735` ilk deneme kaydı değiştirilmeden korundu. İlk `Image.save` çağrısındaki plugin yüklemesi encode süresine girebildiği için encoder kayıtları zamanlayıcıdan önce açıkça yüklendi ve yeni özel run oluşturuldu. İlk public JSON/Markdown, ilk run içine `initial-public-verification.*` adıyla taşındı; ham kayıtlar ve çıktılar duruyor. Tek adayın parametreleri aynı kaldı. Her yeni WebP önceki run ile byte düzeyinde aynı; iki run da aynı altın RGB pinlerini karşılıyor. İlk süreler sonuç tablosuna alınmadı.

macOS `sandbox-exec` `(deny network*)` kuralı bütün ölçüm sürecini ve sips dahil alt süreçleri kapsadı. Gerçek loopback bind `EPERM` ile reddedildi; native `/usr/bin/nc` de devraldığı ağ yasağında aynı reddi verdi. Paket kurulmadı; bulut, DB, giriş veya cihaz oturumu kullanılmadı. Her alt süreç için en çok 30 saniye; native ağ kontrolünde 5 saniye; tüm ölçümde 240 saniye sınırı var. Dosyalar exclusive oluşturulur; mevcut public kanıtın üzerine yazılmaz.

**Açık kapılar:** P0 ham kanıt sınırı ve P4b açık kalır. Yerel byte gerçek transfer değildir. RGB eşitliği fiziksel kalite onayı değildir. Üretim profili veya uygulama codec kararı alınmadı; Java/Flutter/CMS, altyapı ve dağıtım değişmedi.
