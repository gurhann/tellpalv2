# ADR-0013: İletişim ve belge dili

## Durum

2026-09-09 tarihinde kabul edildi. Proje sahibinin isteğiyle 2026-10-17 tarihinde güncellendi.
Önceki İngilizce belge varsayılanının yerine aşağıdaki Türkçe varsayılanı geçer.

## Bağlam

Proje sahibi, bundan sonra üretilen tüm belgeleri Türkçe okumak istiyor.
Belgeler açık ve sade olmalı; gerekli teknik terimler İngilizce kullanılabilmeli.

## Karar

- İletişim, sorular, ilerleme bildirimleri ve sonuç özetleri Türkçe yazılır.
- Yeni belgeler ve yeniden yazılan bölümler Türkçe üretilir. Teknik belgeler, spec ve planlar,
  ADR'ler, proje hafızası, raporlar, kullanım ve operasyon rehberleri bu kapsamdadır.
- API açıklamaları, kod yorumları ve Javadoc da aynı dil ve anlatım kuralına uyar.
- Kısa cümleler, doğrudan anlatım ve anlaşılır sözcükler kullanılır. Gereksiz ayrıntı ve jargondan kaçınılır.
- Yerleşik teknik terimler anlamı netleştiriyorsa İngilizce kalabilir. Gerekirse ilk kullanımda
  kısa Türkçe açıklama eklenir; terimler zorla çevrilmez.
- Kod kimlikleri, API alan adları, komutlar, dosya yolları, hata kodları ve makine anahtarları çevrilmez.
- Kural BMad iş akışlarına, şablonlardan üretilen belgelere ve alt ajanlara da uygulanır.
- Belirli bir çıktı için kullanıcının açık dil isteği önceliklidir.
- Mevcut belgeler topluca çevrilmez. Yalnız eklenen veya yeniden yazılan bölümler Türkçe olur;
  tam çeviri ayrıca istendiğinde yapılır.
- Ürün içerikleri ve çevirileri hedef kitlenin dilini korur.

## Sonuçlar

Belgeler proje sahibinin tercih ettiği dilde, kolay okunur biçimde hazırlanır.
Teknik doğruluk için gereken İngilizce terimler korunur. Eski belgelerle birlikte çalışılabilir;
bu karar toplu çeviri veya uygulama davranışı değişikliği gerektirmez.

## Değerlendirilen seçenekler

- İngilizce teknik belgelere devam etmek: güncel kullanıcı tercihine uymadığı için seçilmedi.
- Tüm teknik terimleri Türkçeleştirmek: anlamı bozabileceği için seçilmedi.

## İlgili dosyalar

- [AGENTS.md](../../../AGENTS.md)
- [Proje hafızası](../project-memory.md)
- [Kod açıklama politikası](ADR-0003-code-documentation-policy.md)
- [BMad ekip ayarları](../../../_bmad/custom/config.toml)

## Önceki kararlarla ilişkisi

ADR-0003'ün açıklama kapsamı ve kalite kuralları korunur; yalnız İngilizce dil zorunluluğu bu kararla değiştirilir.
Bu ADR'nin önceki İngilizce belge kararı, 2026-10-17 tarihli kullanıcı tercihiyle güncellenmiştir.
