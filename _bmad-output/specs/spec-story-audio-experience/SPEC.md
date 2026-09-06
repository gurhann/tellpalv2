---
id: SPEC-story-audio-experience
companions:
  - audio-story-contract.md
sources: []
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate.

# Yerelleştirilmiş Sesli Hikâye Deneyimi

## Why

Sesli sürümü metni, sayfaları ve editoryal kimliği aynı olan hikâyeler bugün ayrı `AUDIO_STORY` kayıtları olarak yönetiliyor. Bu, içeriği, kategorilemeyi ve yaşam döngüsünü çoğaltıyor. Hikâye tek bir canonical `STORY` olarak kalırken her dilde bağımsız bir sesli anlatım deneyimi sunulmalıdır.

## Capabilities

- **CAP-1**
  - **intent:** CMS kullanıcısı bir `STORY` localization'ına sesli anlatım bilgisini ve medya referansını bağlayarak aynı hikâyenin okuma ve dinleme deneyimlerini yönetir.
  - **success:** Bir hikâyenin Türkçe localization'ı sesli anlatıma sahipken İngilizce localization'ı yalnızca okunabilir durumda kalabilir; ikisi ayrı `Content` kaydı oluşturmaz.

- **CAP-2**
  - **intent:** Mobil istemci bir hikâye localization'ının sesli deneyime hazır olup olmadığını görür ve hazır olanları sesli hikâye keşif yüzeylerinde listeler.
  - **success:** Aynı `contentId`, arama veya koleksiyon yanıtında okuma deneyimi olarak ve ses uygun olduğunda `AUDIO_STORY` deneyimi olarak ayrı kart projeksiyonlarıyla dönebilir.

- **CAP-3**
  - **intent:** API sözleşmesi canonical içerik sınıfını istemciye açık biçimde ayırırken sesli hikâye görünümünü korur.
  - **success:** Yeni sözleşmedeki sesli kartlar `canonicalType: STORY` ve `experienceType: AUDIO_STORY` taşır; eski istemci uyumluluğu gereken response yüzeylerinde `type: AUDIO_STORY` üretilebilir.

## Constraints

- `AUDIO_STORY`, aynı anlatının sesli sürümü için canonical `ContentType`, `CategoryType` veya yeni bir `Content` kimliği olamaz; yalnızca keşif/presentation deneyimidir.
- Ses uygunluğu localization bazındadır; bir dildeki sesin varlığı, diğer dilin yayın ve ses durumunu değiştiremez.
- Mevcut sayfa-localization sesleri okuma deneyiminin sayfa anlatımı olarak korunur; sesli hikâye deneyimi için localization seviyesindeki tek parça ses kaydı ayrıca desteklenir.
- `MEDITATION` ve `LULLABY` canonical, ses-odaklı içerik türleri olarak kalır.

## Non-goals

- Meditasyon veya ninnileri `STORY` modeline dönüştürmek.
- Aynı hikâyenin tüm dillerinde sesli anlatımı zorunlu kılmak.
- Eski bağımsız `AUDIO_STORY` verisini topluca içe aktarmak veya dönüştürmek.
- Mobil istemcinin arama ve ana sayfa düzenini yeniden tasarlamak.

## Success signal

- Editör tek bir hikâye ve localization üzerinden okuma ile sesli anlatımı yönetir; mobilde aynı hikâye kimliği hem normal hikâye hem sesli hikâye koleksiyonlarında doğru deneyim bilgisiyle sunulur.
- Yeni içeriklerde aynı anlatı için yinelenen canonical `Content` kaydı oluşturulmaz; mevcut istemcilerin sesli hikâye akışı yeni response sözleşmesiyle çalışmaya devam eder.

## Assumptions

- Sesli hikâye deneyiminin bir localization'a bağlanan tek parça ses kaynağı vardır; mevcut sayfa sesleri bu kaynağın yerine geçmez.
