# Sesli Hikâye Veri ve API Kontratı

## Kavramlar

| Kavram | Sahiplik | Anlamı |
|---|---|---|
| Canonical içerik | `Content` | Hikâyenin tek kimliği; türü `STORY` olur. |
| Dil içeriği | `ContentLocalization` ve `StoryPageLocalization` | Başlık, açıklama, sayfa metni, resim ve sayfa anlatımlarının dile ait hali. |
| Sesli hikâye anlatımı | `ContentLocalization` | Bu dildeki hikâyenin uçtan uca dinleme deneyimi için ses kaynağı, süre ve hazırlık durumu. |
| Keşif deneyimi | Public read projection | Kullanıcıya `READING` veya `AUDIO_STORY` olarak sunulan kart/akış. Yeni içerik kimliği değildir. |

## Uygunluk kuralları

- `READING` deneyimi, mevcut hikâye yayınlama kurallarına göre localization yayınlanabildiğinde uygundur.
- `AUDIO_STORY` deneyimi, hedef localization yayınlanmış, aktif ve sesli anlatımın media/işleme doğrulaması tamamlanmış olduğunda uygundur.
- Bir localization sesli anlatım olmadan yayınlanabilir. Sesli anlatım eklendikten, işlendiikten veya kaldırıldıktan sonra yalnızca o localization'ın audio uygunluğu yeniden hesaplanır.
- Sesli anlatım asset'i `AUDIO` medya türünde olmalıdır. Süre, localization için sesli deneyime ait süredir.
- Sayfa-localization `audioMediaId` alanları korunur. Bunlar sayfa bazlı okuma anlatımıdır; tek parça sesli hikâye kaynağıyla karıştırılmaz.

## Public API projeksiyonu

Yeni veya sürümlenmiş public response alanları:

```json
{
  "contentId": 42,
  "canonicalType": "STORY",
  "experienceType": "AUDIO_STORY",
  "audioAvailable": true,
  "audioStatus": "COMPLETED"
}
```

- Genel hikâye okuma listesi `experienceType: READING` döner.
- Sesli hikâye rayı ve sesli arama grubu yalnızca `experienceType: AUDIO_STORY` için uygun localization'ları döner.
- Aynı `contentId` iki projeksiyonda bulunabilir. İstemci bunu çoğaltılmış içerik değil, aynı esere iki giriş noktası olarak ele alır.
- Geriye uyumluluk gereken eski audio-discovery endpoint'i, sadece kendi response yüzeyinde `type: AUDIO_STORY` dönebilir. Yeni admin ve core public sözleşmelerinde bu alan canonical tür yerine kullanılmaz.

## Kategori ve koleksiyon davranışı

- Canonical story kategorileri `STORY` türünde kalır; sesli hikâye için ayrı `CategoryType.AUDIO_STORY` oluşturulmaz veya korunmaz.
- Sesli hikâye koleksiyonu, `STORY` kategorilerinden audio-uygun localization projeksiyonu çıkarır.
- Bir editoryal rayın yalnızca sesli hikâye göstermesi gerekiyorsa bu, kategori türü yerine açık bir discovery/collection presentation kuralı olarak modellenir.

## CMS davranışı

- `STORY` localization düzenleyicisi, sayfa düzenleme akışından ayrı fakat onunla tutarlı bir “sesli anlatım” alanı sunar.
- Alan, ses asset'i, süre, hazırlık/yayın uygunluğu ve gerekli hata bilgisini gösterir.
- Bir dilde ses eklemek başka dillere alan, asset veya durum kopyalamaz.
- CMS yeni `AUDIO_STORY` oluşturmayı sunmaz.

## Kapsam dışı import

Eski veya dış kaynaktan gelen bağımsız `AUDIO_STORY` kayıtlarının toplu importu bu kontratın parçası değildir. Bu import gerektiğinde eşleme, geri alma, veri temizliği ve yayın etkisi için ayrı bir spec hazırlanacaktır.

## Doğrulama matrisi

| Senaryo | Beklenen sonuç |
|---|---|
| TR sesli, EN ses yok | TR `AUDIO_STORY` listesinde görünür; EN yalnızca `READING` görünür. |
| Aynı hikâye aramada iki grupta | Kartlar aynı `contentId` ile döner, `experienceType` farklıdır. |
| Ses asset'i hatalı veya işleme başarısız | Localization sesli rayda görünmez; okuma deneyimi etkilenmez. |
| Meditasyon/ninni | Canonical tür ve mevcut ses odaklı akış korunur. |
