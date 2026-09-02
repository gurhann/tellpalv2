# Contents Registry — Uygulama Sözleşmesi (taslak)

**Tarih:** 2026-09-02  
**Durum:** İlk ekran taslağından üretilmiş; kodlamaya hazır taslak  
**Kapsam:** CMS `/contents` kayıt ekranı. Story yükleme ve detay düzenleme akışları bu teslimin dışında kalır.

## Amaç

Editör, seçili dilde bir içeriğin mobil yayına neden engellendiğini liste ekranında
görür; arar, filtreler ve satırdan doğru detay çalışma alanına gider.

Bu ekran bir dashboard değildir. Tür × dil genel sağlık matrisi sonraki bir dashboard
teslimidir.

## Kesin UX sözleşmesi

- Başlangıç dili `tr`; kullanıcı değiştirdiğinde URL'de `?language=xx` korunur.
- Arama, **seçili dildeki başlığı**, `externalKey` ve sayısal `contentId` alanını kapsar.
- Varsayılan sıra `lastEditedAt DESC`, eşitlikte `contentId DESC` olur.
- Görünür filtreler: içerik türü, seçili dil ve üçlü yayın durumu.
- Satır tıklaması `/contents/:contentId?language=xx` yoluna gider. Detay rotası bu
  parametreyi yerelleştirme sekmesine başlangıç seçimi olarak uygular.
- Durumlar yalnızca seçili dil için hesaplanır:
  - `ACTION_REQUIRED` → en az bir yayın engeli vardır.
  - `READY_TO_PUBLISH` → tüm gerekli alanlar tamamdır; fakat mobilde yayınlanmıyordur.
  - `PUBLISHED` → tüm gerekli alanlar tamamdır ve lokalizasyon `PUBLISHED + COMPLETED` durumundadır.
- `ACTION_REQUIRED` satırında "N yayın engeli" düğmesi bulunur. Düğme tıklama,
  klavye ve dokunmatik ile açılan bir popover içinde bütün engelleri listeler.
  Masaüstü hover desteği yalnızca bu erişilebilir temel davranışın üzerine eklenebilir.
- Satır içi düzenleme yoktur. Popover tetikleyicisi satır navigasyonunu durdurur.
- Toolbar mevcut ortak `RegistryToolbar` üzerinden kurulur; yan görev rayı bu ekrandan
  kaldırılır. Özet toolbar'ın altında toplam sonuç, aktif filtreler ve aksiyon gerekli
  sayısını gösterir.

## STORY için çalışma yayınlanabilirlik kuralı

Bu kural, kullanıcının C2 çalışma kararını liste için uygulanabilir hale getirir.
Üretime geçmeden önce gerçek mobil sözleşmesiyle teyit edilmelidir.

| Koşul | Eksik olduğunda engel kodu | Not |
| --- | --- | --- |
| İçerik aktiftir | `CONTENT_INACTIVE` | Mobil sorgu pasif içerikleri zaten dışarıda bırakır. |
| Seçili dil lokalizasyonu vardır | `LOCALIZATION_MISSING` | Nadir ithalat/veri istisnası; yine de güvenli bir sonuç döner. |
| Başlık doludur | `TITLE_MISSING` | Mevcut domain zaten boş başlığa izin vermez. |
| Açıklama doludur | `DESCRIPTION_MISSING` | Yeni editoryal kontrol. |
| Lokalize kapak görseli vardır | `COVER_MISSING` | Kullanıcı kararı C3. `textlessCoverMediaId` bunun yerine geçmez. |
| En az bir hikâye sayfası vardır | `STORY_PAGES_MISSING` | Mevcut yayın politikasıyla uyumlu. |
| Her sayfada seçili dil lokalizasyonu vardır | `PAGE_LOCALIZATION_MISSING` | Sayfa numarası engel ayrıntısına eklenir. |
| Her sayfada metin, ses ve görsel vardır | `PAGE_TEXT_MISSING`, `PAGE_AUDIO_MISSING`, `PAGE_ILLUSTRATION_MISSING` | Sayfa numarası engel ayrıntısına eklenir. |
| İşleme tamamlanmıştır | `PROCESSING_NOT_COMPLETED` | `FAILED`, `PENDING`, `PROCESSING` ayrı açıklama ile döner. |

`READY_TO_PUBLISH`, yukarıdaki tüm kontroller geçip lokalizasyon henüz mobilde görünür
olmadığında döner. `PUBLISHED` ise bu kontroller ve `visibleToMobile=true` birlikte
sağlandığında döner. Arşivlenmiş bir lokalizasyon bu nedenle `READY_TO_PUBLISH`
olabilir; yayınlamak ayrı bir editoryal aksiyondur.

Mevcut backend, sayfa metni/ses/görseli ve en az bir sayfayı publish çağrısında zaten
doğrular. Açıklama ve lokalize kapak bugün yayın önkoşulu değildir. Bu iki alanın liste
durumu ile yayın komutu arasında çelişmemesi için, bu taslak uygulanırken aynı
`StoryPublicationReadinessPolicy` hem registry sorgusu hem de publish önkoşulu tarafından
kullanılmalıdır.

## API tasarımı

Mevcut `GET /api/admin/contents` korunur. Bu endpoint birçok CMS ekranı ve testte ham
yerelleştirme listesi olarak kullanılıyor; dönüş şeklini değiştirmek geriye dönük kırıcı olur.
Registry için yeni, amaç odaklı endpoint eklenir:

`GET /api/admin/content-registry?language=tr&type=STORY&readiness=ACTION_REQUIRED&q=orman&page=0&size=25`

Parametreler:

| Parametre | Kural |
| --- | --- |
| `language` | Zorunlu; geçerli `LanguageCode`. CMS ilk isteği `tr` ile yapar. |
| `type` | Opsiyonel `ContentType`. |
| `readiness` | Opsiyonel üçlü durum enum'u. |
| `q` | Opsiyonel; boşluk kırpılır, başlık/external key/ID üzerinde büyük-küçük harf duyarsız aranır. |
| `page` | Opsiyonel, sıfır tabanlı, varsayılan `0`. |
| `size` | Opsiyonel, varsayılan `25`, üst sınır `100`. |

Önerilen yanıt:

```json
{
  "items": [
    {
      "contentId": 42,
      "type": "STORY",
      "externalKey": "story.ay-isigi",
      "pageCount": 14,
      "selectedLanguage": "tr",
      "title": "Ay Işığındaki Bahçe",
      "readiness": "ACTION_REQUIRED",
      "blockers": [
        { "code": "COVER_MISSING", "pageNumber": null },
        { "code": "PAGE_AUDIO_MISSING", "pageNumber": 7 }
      ],
      "lastEditedAt": "2026-09-02T13:46:00Z"
    }
  ],
  "page": 0,
  "size": 25,
  "totalItems": 78
}
```

`blockers` yalnızca `ACTION_REQUIRED` öğelerde dolu döner; istemci bunları Türkçe/İngilizce
etiketlere çevirir. API, ham alanlardan türetilen merkezi yayınlanabilirlik kararını taşır;
böylece farklı ekranların farklı hesap yapması engellenir.

`lastEditedAt`, aşağıdakilerin en yenisidir: content kaydı, seçili content localization,
story pages ve seçili dildeki story-page localizations. Bu tanım, editörün "son
güncellenen" beklentisini gerçekten karşılar. Sıralama veritabanında yapılmalıdır;
mevcut tüm kayıtları fetch edip istemcide sıralamak kabul edilmez.

Repository katmanı, sayfalama ile koleksiyon `join fetch` karışımından kaçınır. Önce filtre,
durum ve sıralamaya göre sayfalı içerik kimliklerini/read-model projection'ını bulur; ardından
yalnızca bu sayfanın STORY sayfaları ve seçili dil localizations'larını yükler. Application
service, ortak readiness politikasını çalıştırır.

## CMS uygulama işi

1. Yeni registry API şemasını `content-admin.ts` içinde Zod ile doğrula; query parametreleri ve
   React Query anahtarı (`language`, `type`, `readiness`, `q`, `page`, `size`) dahil edilir.
2. `/contents` route'u yerel, tüm listeyi getiren filtreleme yerine server-side registry sorgusunu
   kullanır. Arama debounce/deferred değer ile yapılır ve her yeni arama/filtre sayfayı `0`a döndürür.
3. URL durumu en az `language`, `type`, `readiness`, `q`, `page` parametrelerini taşır. Boş ve
   varsayılan değerler URL'den temizlenir; varsayılan dil hariç dil korunur.
4. `ContentListTable`, yeni registry-row view modeline geçer. Sütunlar: içerik (başlık,
   external key, ID), tür, seçili dil durumu, sayfa sayısı ve son güncelleme.
   Story olmayan türde sayfa hücresi `—` olur.
5. Durum rozeti sadece renge dayanmaz. `Aksiyon gerekli` satırında blocker popover düğmesi
   görünür; yayınlanan/yayına hazır satırlarda anlaşılır metin rozetleri kullanılır.
6. Detay route'u `language` parametresini `ContentLocalizationTabs` başlangıç seçimine geçirir;
   sekme değişimi de URL'yi günceller. Böylece liste-detal bağlamı korunur.
7. Oluşturma akışı korunur; başarılı oluşturma `/contents/:id?language=tr` ile açılır.

## Test ve kabul ölçütleri

Backend:

- `language` zorunluluğu, enum/parametre doğrulaması ve sayfa sınırları controller testi.
- Her blocker kodu için birim test; özellikle birden çok eksik alanın eksiksiz dönmesi.
- `READY_TO_PUBLISH` ve `PUBLISHED` ayrımı; pasif, arşivlenmiş, başarısız işleme ve seçili dil
  lokalizasyonu olmayan kayıt senaryoları.
- `lastEditedAt` ve varsayılan sıralama entegrasyon testi.
- Eski `GET /api/admin/contents` yanıtı ve kullanan akışlar değişmeden kalır.

CMS:

- API şeması, URL filtre serileştirme ve row view-model birim testleri.
- Playwright: `tr` varsayılanı, filtre/arama isteği, seçili dilin detail URL'sine taşınması,
  blocker popover'ın fare/klavye ile açılması ve satır navigasyonunu tetiklememesi.
- Görsel regresyon: Contents toolbar ve sonuç tablosu için 390, 768, 1280, 1440 genişlikleri.
  Dar ekranda toolbar sarar; yatay toolbar kaydırması oluşmaz.

Doğrulama komutları, kod değişikliğinde:

```text
cd be  && .\mvnw test
cd cms && npm run test
cd cms && npm run build
cd cms && npm run test:e2e:visual
```

## Uygulamadan önce tek anlamlı doğrulama

Bu plan, açıklama ve lokalize kapağı da gerçek publish önkoşulu yapar. Böylece CMS'te
"Yayına hazır" görünen bir hikâye publish çağrısında sonradan sürpriz yaşamaz. Bu, mevcut
backend davranışını **sıkılaştıran** tek ürün kararıdır; diğer STORY kontrolleri zaten publish
politikasında var.
