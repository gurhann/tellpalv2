# TellPal REST API Sözleşmeleri

**Tarih:** 2026-07-28  
**İstemci:** Flutter / Dio / Retrofit  
**Base URL:** Firebase Remote Config üzerinden çalışma zamanında alınır; değer güvenlik
nedeniyle bu belgede yer almaz.

## 1. Ortak İstemci Davranışı

Tüm REST çağrıları `Dio` üzerinden oluşturulan Retrofit servisleriyle yapılır.

| Özellik | Mevcut davranış |
|---|---|
| Dil | `Accept-Language` başlığı yerel dil tercihinden gelir. |
| Uygulama sürümü | `App-Version` başlığı eklenir. |
| Yetkilendirme | REST istemcisinde görünür bir bearer/API auth başlığı yoktur. |
| Başarı modeli | Retrofit `HttpResponse<T>` → repository → `DataSuccess<T>`. |
| Hata modeli | `DioException` → `DataFailed<T>`. |
| Önbellek | Hive destekli Dio önbelleği; gerektiğinde bellek yedeği. |
| Hata anında önbellek | 401 ve 404 dışındaki bazı ağ hatalarında eski veri kullanılabilir. |
| Zorla önbellek süresi | En fazla yaklaşık 5 dakika eski veri kabul edilir. |

Backend bu repoda bulunmaz. `v2/be` altında yalnızca boş dizin iskeleti vardır; dolayısıyla
aşağıdaki sözleşmeler mobil istemcinin beklentileridir, backend implementasyonu değildir.

## 2. Hikâye Uç Noktaları

| Metot | Yol | Parametreler | Yanıt | Tüketen işlev |
|---|---|---|---|---|
| GET | `/story` | Yok | `Story[]` | Tüm hikâyeler |
| GET | `/story/info/{id}` | `id: int` | `Story` | Hikâye bilgi ekranı |
| GET | `/story/{id}` | `id: int` | `StoryPoint` | Etkileşimli içerik ağacı |
| GET | `/story/with-category` | `count?: int` | `CategoryWithStories[]` | Hikâye vitrini |
| GET | `/story/with-category/listening` | `count?: int` | `CategoryWithStories[]` | Rahatlama/sesli içerik vitrini |
| GET | `/story/category/{category_id}` | `category_id: int` | `StoryWithCategoryType[]` | Kategori detayı |
| GET | `/story/search` | `search: string` | `StorySearchByCategoryType[]` | Arama |
| GET | `/story/by-id-list` | `idList: string` | `Story[]` | Kullanıcı geçmişi/listesi |
| GET | `/story/editorSelected` | `count?: int` | `Story[]` | Editör seçimi |
| GET | `/story/similarContent` | `storyIdList?: string`, `search?: string` | `Story[]` | Benzer içerik |

### 2.1 `Story`

```text
id: int
name: string
author: string
summary: string
imageName?: string
hasVoice: bool
isNew: bool
pageCount: int
dubbing?: string
music?: string
version: int
isFullImage: bool
illustrator?: string
isMusicActive: bool
summaryImageName?: string
duration?: string
ageRange?: string
isPremium: bool
imageAddress?: string
summaryImageAddress?: string
```

### 2.2 `StoryPoint`

Etkileşimli hikâye düğümüdür:

```text
content: page content payload
next: StoryPoint[]
optionAnswer?: option metadata/text
imageName?: string
voiceFileName?: string
```

`next` dizisi boşsa son sayfa, tek elemanlıysa doğrusal devam, iki elemanlıysa kullanıcı
seçimi olarak yorumlanır. İstemci bu yapıyı çalışma zamanında aktif sayfa dizisine çevirir.

### 2.3 Kategori Modelleri

```text
StoryCategory:
  id: int
  name: string
  description: string
  type: STORY | LULLABY | AUDIO_STORY | MEDITATION
  imageUrl?: string
  isPremium: bool

CategoryWithStories:
  category: StoryCategory
  stories: Story[]
```

`StoryWithCategoryType` ve `StorySearchByCategoryType`, temel hikâye alanlarına içerik
türü/kategori bağlamı ekler. Bilinmeyen veya boş kategori tipi istemcide `STORY` değerine
düşer.

## 3. Kategori Uç Noktası

| Metot | Yol | Yanıt | Kullanım |
|---|---|---|---|
| GET | `/category` | `StoryCategory[]` | Kategori listesi |

## 4. Ebeveyn Rehberi Uç Noktaları

| Metot | Yol | Parametreler | Yanıt | Kullanım |
|---|---|---|---|---|
| GET | `/parent-guidance-book` | Yok | `ParentGuidanceBook[]` | Tüm rehberler |
| GET | `/parent-guidance-book/with-category` | `count?: int` | `CategoryWithParentGuidanceBooks[]` | Ebeveyn vitrini |
| GET | `/parent-guidance-book/category/{category_id}` | `category_id?: int` | `ParentGuidanceBook[]` | Kategori detayı |
| GET | `/parent-guidance-book/{parentGuidanceBookId}` | `parentGuidanceBookId: int` | `ParentGuidanceBook` | Günlük ücretsiz/tek rehber |

### 4.1 `ParentGuidanceBook`

```text
id: int
title: string
author: string
summary: string
info: string
content: string
audioUrl: string
imageUrl: string
isPremium: bool
duration: string
```

### 4.2 Ebeveyn Kategori Modelleri

```text
ParentGuidanceBookCategory:
  id: int
  name: string
  description: string

CategoryWithParentGuidanceBooks:
  category: ParentGuidanceBookCategory
  parentGuidanceBooks: ParentGuidanceBook[]
```

## 5. API ve Dosya İçeriği Ayrımı

REST API, içerik meta verisi ve etkileşimli hikâye ağacını döndürür. Büyük medya
dosyaları REST yanıtının parçası değildir:

- Hikâye ZIP paketleri Firebase Storage'dan indirilir.
- Kapak, özet, kategori ve avatar görselleri Storage adreslerinden alınır.
- Ebeveyn rehberi ses/görsel adresleri model alanlarında taşınır.
- Günlük ücretsiz içerik seçimi Remote Config'den gelir.

Bu nedenle bir hikâyenin kullanılabilir olması için REST meta verisi, Remote Config
paketleme kararı ve Storage dosya düzeninin birbiriyle uyumlu olması gerekir.

## 6. Gözlenen Sözleşme Riskleri

| Risk | Açıklama |
|---|---|
| Sürüm kontrollü şema yok | Retrofit modelleri alanların varlığı ve tipine doğrudan bağlıdır. |
| REST auth görünmüyor | Endpoint'lerin dışarıya açık olup olmadığı backend tarafında doğrulanmalıdır. |
| `count` nullable | Backend'in eksik parametre davranışı açıkça belgelenmemiştir. |
| `idList` string | Ayraç, maksimum uzunluk ve boş liste davranışı kodda sözleşmeye bağlanmamıştır. |
| Hata gövdesi modellenmemiş | UI çoğunlukla `DioException` düzeyinde genel hata görür. |
| Kategori fallback'i sessiz | Bilinmeyen backend tipi `STORY` olur ve veri hatasını saklayabilir. |
| Dosya manifesti yok | Sayfa → ZIP paketi ilişkisi istemcide sabit indeks varsayımına bağlıdır. |
| Base URL çalışma zamanında | Yanlış Remote Config değeri tüm REST katmanını etkiler. |

## 7. Yeniden Geliştirme İçin Hedef Sözleşme

- OpenAPI ile sürümlenmiş endpoint ve hata şemaları.
- Kimlik doğrulama/yetkilendirme gereksiniminin açık tanımı.
- Sayfalama, arama sınırları ve `idList` formatının kesinleştirilmesi.
- Hikâye medya manifesti: paket kimliği, sayfa aralığı, checksum, boyut ve sürüm.
- REST ve Storage içerik sürümlerinin tek yayın kimliğiyle bağlanması.
- İstemci hata kodları: çevrimdışı, yetkisiz, bulunamadı, bozuk medya, sürüm uyuşmazlığı.
- Sözleşme testleri ve fixture tabanlı model geriye uyumluluk testleri.

## Kaynak Kanıtları

- `lib/features/stories/data/data_sources/remote/story_api_service.dart`
- `lib/features/stories/data/data_sources/remote/category_api_service.dart`
- `lib/features/parent_mode/data/data_sources/remote/parent_mode_api_service.dart`
- `lib/features/**/data/models/`
- `lib/features/**/data/repository/`
- `lib/injection_container.dart`

> Gizli anahtarlar, servis kimlikleri ve gerçek base URL değerleri belgeye alınmamıştır.
