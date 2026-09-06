# Ninni Veri ve API Kontratı

## Sahiplik modeli

| Veri | Sahip | Dil bağımlılığı |
|---|---|---|
| Ninni kimliği ve türü | `Content` (`LULLABY`) | Yok |
| Ortak oynatma kaydı: ses asset'i, kapak asset'i, süre | `Content` altındaki ninni playback verisi | Yok |
| Müzik kredisi | Mevcut `ContentContributor` (`MUSICIAN`) | Yok |
| Enstrüman seçimi ve sırası | Ninni playback verisi ile enstrüman kataloğu arasındaki ilişki | Yok |
| Ninni adı | `ContentLocalization` | Evet |

`ContentLocalization` ninni için başlık dışındaki metin, kapak, ses ve süre alanlarını taşımaz. Bu kural, tek ses dosyasının birden fazla localization'a yanlışlıkla bağlanmasını önler.

## Enstrüman kataloğu

- Her katalog kaydının kararlı kimliği ve yönetilen görünen adı bulunur.
- Katalog, her desteklenen dil için kendi görünen adını taşır; mobil response seçilen locale'a uygun adı döner.
- İlk katalog Türkçe görünen adları: `Çelesta`, `Bell`, `Keman`, `Rhodes`, `Glockenspiel`, `Arp`, `Vibrafon`, `Yaylı Orkestra`.
- Ninni düzenleyicisi katalogdan çoklu seçim yapar; serbest metin yazamaz.
- Aynı enstrüman ninniye bir kez eklenebilir.
- `displayOrder`, mobilde gösterim sırasını belirler ve tekrarlanamaz.
- Katalog boşsa ninni kaydedilemez veya yayınlanamaz; en az bir enstrüman seçilmelidir.

## CMS akışı

1. Editör `LULLABY` oluşturur.
2. İçerik düzeyindeki Ninni Playback bölümünde ortak kapak, ses dosyası, süre ve enstrümanları seçer.
3. Contributor panelinden müzisyen atar.
4. Her localization sekmesinde yalnızca ninni adını girer ve yayın durumunu yönetir.
5. Yayın öncesi, ortak playback kaydı ile localization başlıklarının uygunluğu doğrulanır.

## Mobil response

Seçilen locale için örnek projection:

```json
{
  "contentId": 61,
  "type": "LULLABY",
  "languageCode": "tr",
  "title": "Dandini Dastana",
  "playback": {
    "audio": { "assetId": 901 },
    "cover": { "assetId": 305 },
    "durationMinutes": 10,
    "musicians": [{ "contributorId": 17, "displayName": "Ali Kaan Uysal" }],
    "instruments": [
      { "id": 3, "name": "Piyano", "displayOrder": 0 },
      { "id": 8, "name": "Glockenspiel", "displayOrder": 1 }
    ]
  }
}
```

Locale değiştiğinde `contentId` ve `playback` aynı kalır; yalnızca `languageCode` ve `title` değişir.

## Doğrulama matrisi

| Senaryo | Beklenen sonuç |
|---|---|
| TR ve EN adı | Aynı ses, kapak, süre ve enstrümanlar; farklı başlıklar. |
| Piyano + Glockenspiel | CMS'de sıralı iki katalog seçimi; mobilde aynı sırayla görünür. |
| Serbest metin enstrüman | Kabul edilmez. |
| Ses asset'i eksik | Ninni localization'ı mobilde görünmez. |
| Aynı enstrüman iki kez | Kaydetme ve veri katmanı reddeder. |
