# TellPal Varlık ve Yerelleştirme Envanteri

**Tarih:** 2026-07-28  
**Toplam:** 184 dosya, yaklaşık 8.67 MiB

## 1. Dizin Bazında Envanter

| Dizin | Dosya | Boyut | Amaç |
|---|---:|---:|---|
| `assets/animations` | 4 | 734,246 B | JSON/Lottie animasyonları |
| `assets/customers` | 6 | 20,315 B | Müşteri tipi görselleri/ikonları |
| `assets/fonts` | 31 | 2,657,716 B | Uygulama font aileleri |
| `assets/i18n` | 5 | 100,976 B | Kaynak yerelleştirme JSON'ları |
| `assets/icons` | 56 | 111,311 B | Ana SVG/ikon seti |
| `assets/images` | 68 | 3,477,289 B | Genel görseller ve landing varyantları |
| `assets/logo` | 2 | 1,968,419 B | Logo dosyaları |
| `assets/newIcons` | 12 | 18,621 B | İkinci/yenilenmiş ikon seti |

`assets/images/landing` altında dil bazlı toplam 36 dosya bulunur; Türkçe, İngilizce ve
Portekizce setleri 12'şer dosyadır.

## 2. Dosya Tipi Envanteri

| Uzantı | Dosya | Boyut |
|---|---:|---:|
| `.svg` | 97 | 179,362 B |
| `.jpg` | 40 | 4,794,957 B |
| `.ttf` | 31 | 2,657,716 B |
| `.json` | 9 | 835,222 B |
| `.png` | 6 | 185,510 B |
| `.jpeg` | 1 | 436,126 B |

Boyutun büyük bölümü JPG landing/görselleri, fontlar ve logolardan gelir.

## 3. Yerelleştirme

Slang tarafından üretilen locale kodunda şu diller bulunur:

- İngilizce (`en`)
- Almanca (`de`)
- İspanyolca (`es`)
- Portekizce (`pt`)
- Türkçe (`tr`)

Mevcut UI seçim davranışı:

- Türkçe, İngilizce ve Almanca kullanıcıya sunulur.
- Portekizce Remote Config bayrağına bağlı olarak sunulabilir.
- İspanyolca kaynakta üretilmiş olsa da seçim UI'ında yorum satırına alınmıştır.

Bu nedenle “çeviri mevcut” ile “üründe seçilebilir” aynı liste değildir.

## 4. Pubspec ve Üretilen Çeviri İlişkisi

Font, görsel, ikon ve animasyon yolları `pubspec.yaml` altında asset olarak bildirilir.
`assets/i18n` kaynakları Slang codegen ile `lib/gen/` altındaki Dart koduna gömülür;
runtime asset olarak doğrudan okunmaması mümkündür.

Yeniden geliştirmede:

- Kaynak JSON'lar tek kaynak doğrusu olmalı.
- Codegen çıktıları kaynak kontrolü stratejisi açık olmalı.
- Dil ekleme işlemi UI seçimi, landing görselleri, Firebase Storage yolları ve backend
  `Accept-Language` desteğini birlikte güncellemeli.

## 5. Uzak Varlıklarla İlişki

Repository içindeki asset'ler yalnızca uygulama kabuğuna aittir. İçerik varlıklarının
büyük bölümü çalışma zamanında gelir:

- Hikâye sayfaları ve sesleri Firebase Storage ZIP'lerinde.
- Kapak ve kategori görselleri Storage/cache katmanında.
- Duyuru görselleri Remote Config + Storage kombinasyonunda.
- Ebeveyn rehberi görsel/ses adresleri API modelinde.
- PDF belgeleri dil bazlı uzak adreslerden indirilir.

Dolayısıyla uygulamanın tam varlık kataloğu yalnızca bu repository taramasıyla
çıkarılamaz; backend içerik kataloğu ve Storage manifesti ayrıca export edilmelidir.

## 6. Gözlenen Riskler

| Risk | Etki |
|---|---|
| `icons` ve `newIcons` ayrımı | İkon kaynağı ve kullanım standardı belirsiz. |
| Dil bazlı landing görselleri | Her yeni dil için metin ve görsel setinin eş zamanlı üretimi gerekir. |
| Büyük JPG/logo dosyaları | Açılış süresi ve paket boyutu etkilenebilir. |
| Uzak medya manifesti yok | Eksik dosya ve sürüm uyuşmazlığı geç fark edilir. |
| SVG/PNG/JPG karışımı | Ölçekleme ve tema renklendirme davranışı tutarsız olabilir. |
| Beş çeviri, dört veya daha az seçilebilir dil | Ürün ve çeviri kapsamı ayrışır. |

## 7. Yeniden Geliştirme İçin Öneriler

- Tek bir asset naming sözleşmesi ve generated asset accessor kullan.
- Görsel boyut, format ve sıkıştırma bütçelerini CI'da denetle.
- Landing görsellerindeki metni mümkünse görselden çıkarıp yerelleştirilmiş UI metni yap.
- Uzak içerik için sürümlü manifest, checksum ve boyut bilgisi üret.
- Kullanılmayan asset'leri referans taraması ve görsel doğrulama sonrası temizle.
- Dil etkinleştirmeyi tek bir yetenek matrisiyle yönet.

## Kaynak Kanıtları

- `assets/`
- `pubspec.yaml`
- `lib/gen/`
- `lib/features/profile/presentation/page/change_language.dart`
- `lib/common/services/firebase_service.dart`
- `lib/common/services/firebase_remote_config_service.dart`
