# P5g — Yerel sunucu uyumluluğu

**Sonuç: İki hikâyenin 41 görseli yerel Java denemesinden geçti.**

- 17 JPEG yeni klasöre aynen kopyalandı.
- 24 mevcut kayıpsız WebP, orijinal PNG'lerle karşılaştırıldı. Bütün piksel
  değerleri, renk profili ve taşınan bilgiler eşleşti.
- Yeni dosyalar ayrı süreçlerde yeniden açılarak doğrulandı. Toplam boyut
  **76.797.735 byte**; yeniden sıkıştırma yapılmadı.
- Canlıya, veritabanına ve telefona erişilmedi. 41 orijinal, 124 P5c girdisi,
  yedi P5f kanıtı ve önceden değişmiş 88 çalışma dosyası aynen korundu.

Deneme için ayrı, varsayılan olarak kapalı bir Java okuyucusu hazırlandı.
**Mevcut sunucu kuralları değişmedi; bu dosyalar henüz üretime kabul edilmiş
değil.** Üretim ortamındaki Java/container doğrulaması ayrıca yapılmalıdır.

26 uyumluluk testi, normal profilde 12 regresyon testi ve gerçek komut satırı
çalıştırması başarılı. Ayrıntılar: [doğrulama kaydı](dogrulama.md) ve
[sayısal kanıt](kanit.json).
