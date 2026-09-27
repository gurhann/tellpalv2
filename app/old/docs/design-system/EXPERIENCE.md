---
name: TellPal 1.0.35 Experience
status: final
updated: 2026-09-25
sources:
  - ../screen-flow-map.md
  - ../emulator-validation.md
  - DESIGN.md
---

## Foundation

Mevcut ürün Android/iOS Flutter mobil uygulamasıdır. Bu sözleşmenin doğrudan görsel kanıtı Android'dir; iOS ekranları çalıştırılarak doğrulanmadı. Arayüzün görsel kaynağı [DESIGN.md](./DESIGN.md), yüzey bazlı uygulama ayrıntıları [ekran tarifleri](./SCREENS.md) ve kanıt durumu [kapsam matrisi](./COVERAGE.md) içindedir. İki farklı navigasyon mekanizması (GoRouter ve doğrudan Navigator/PersistentNavBarNavigator) kaynakta vardır; burada kullanıcıya görünen geçişler esas alınır.

## Information Architecture

```text
Açılış → ilk kullanım tanıtımı → hesap seçimi
                         ├─ e-posta/Google/Apple kayıt veya giriş → profil kurulumu
                         └─ hesapsız devam
Ana kabuk ├─ Kitaplık → arama/kategori → hikâye bilgisi → okuyucu → bitiş
          ├─ Uyku → ninni/meditasyon/sesli hikâye → bilgi/oynatıcı
          └─ Profil → düzenle, dil, hesap, geri bildirim
Çocuk/ebeveyn seçimi → ebeveyn rehberi → bilgi → okuma/dinleme
Premium kart veya çağrı → RevenueCat paywall
```

Ana kabukta üç alt sekme bulunur. Sekme etiketi Remote Config ve dil seçimine göre değişebilir; sekme sayısı üçtür. Kategori satırlarının sağındaki aksiyon tam listeye, kartlar bilgi/oynatıcıya gider. Çocuk/ebeveyn seçimi bir modal üzerinden yapılır. Sayfa arka navigasyonu ana kabukta Kitaplık'a dönebilir; detay sayfalarında önceki sayfaya döner. Ayrıntılı route eşlemesi `COVERAGE.md` içindedir.

## Voice and Tone

Bu yeniden üretim belgesi belirli bir doğal dile bağlanmaz. Görsel tariflerde `screen.title`, `section.subtitle`, `action.start`, `content.summary`, `form.error` gibi **anlamsal alan kimlikleri** kullanılır. Gerçek metinler uygulamanın i18n kaynağından veya API'den gelir. Bölüm başlıklarında emoji ve içerik metadata'sı yerelleştirilmiş metnin parçası olabilir; ikonlarla otomatik değiştirilmemelidir. Kitap kapağı ve bazı landing görsellerindeki yazı görsele gömülüdür ve ayrı bir UI çevirisi gibi ele alınmaz.

## Component Patterns

| Bileşen | Davranış | Görsel kaynak |
|---|---|---|
| Alt sekme | Kitaplık/Uyku/Profil arasında geçiş, seçili durum korunur | `DESIGN.md` Components tablosu |
| İçerik kartı | Ücretsiz kart detay açar; kilitli kart premium yoluna girebilir | `{components.story-cover}` |
| Hikâye bilgi CTA'sı | Ücretsiz → okuyucu; premium → paywall; seslendirme anahtarı ayrıca vardır | `{components.button-primary}` ve ekran gradient varyantı |
| Ses kartı | Ninni doğrudan oynatıcıya; meditasyon/sesli hikâye önce bilgi ekranına gider | `{rounded.media-card}` |
| Okuyucu | Sayfa kaydırma, ses eşliği, ikinci ZIP sınırında yükleme, seçim varsa dal, son ekran | Tam ekran medya varyantı |
| Profil ayar satırı | Dil, bildirim, hesap, geri bildirim işlemlerini açar | `{colors.surface}`, `{colors.ink}` |
| Form | Geçersiz/boş alan için alan altında hata; parola görünürlüğü ayrı kontrol | `{components.input}` |
| Modal | Mevcut ekrandan ayrılmadan seçim veya onay; iptal/dış alan yolu modal tipine göre | `{colors.surface}` |

## State Patterns

| Durum | Görünen davranış | Kanıt düzeyi |
|---|---|---|
| İlk açılış | Splash ardından bildirim izni ve üç tanıtım sayfası | Android görüntüsü + runtime |
| Anonim | Kitaplık açık; profil üyelik çağrısı gösterir | Android görüntüsü |
| Kayıtlı/doğrulanmamış | `User` varsayılan adı ve e-posta doğrulama uyarısı; içerik erişimi engellenmedi | Android runtime |
| Yükleme | Ortada dairesel progress veya içeriğin iskelet/placeholder alanı | Kod + bazı görüntüler |
| İçerik hatası | Ekrana özel hata metni/yüzeyi; tek evrensel hata kalıbı yok | Kod |
| Premium kilit | Kartta kilit rozeti/bulanıklaşma; bilgi sayfasında premium CTA | Android görüntüsü |
| Billing kullanılamıyor | RevenueCat kaynaklı `Error 3` modalı | Android görüntüsü |
| Form hatası | İlgili alan altında hata, form yerleşimini aşağı iter | Android UI ağacı + kod |
| Oynatılıyor/duraklatıldı | Ortak oynatıcıda play/pause ve ilerleme | Android görüntüsü |

## Interaction Primitives

- Tap: kart, CTA, sekme, ayar satırı, modal seçimi.
- Yatay kaydırma: içerik rayları; dikey kaydırma: ana listeler, bilgi ve profil sayfaları.
- Hikâye okuyucu: sayfa ileri/geri ve varsa seçenek seçimi. İkinci ZIP sınırı gerçek içerik yükleme olayıdır; ara loader görünümü korunur.
- Sistem geri hareketi: detaydan önceki yüzeye; ana kabuğun Uyku/Profil sekmesinden Kitaplık sekmesine. Kaynak `home.dart` Android/iOS kapanma davranışlarını ayrı uygular.
- Ses oynatıcı: otomatik başlatma, duraklat/devam, çıkış. Ninni 10:19, örnek meditasyon 6:35 ve sesli hikâye 8:00 runtime'da izlendi; süreler içerik verisidir.

## Accessibility Floor

Bu bölüm mevcut ürünün eksiksiz erişilebilirlik doğrulaması değildir. Android UI ağacında ana düğme ve sekme etiketleri görünür; bazı simgelerde `NAF` ve boş açıklama bulundu. Yeniden uygulamada görsel eşdeğerlik korunurken her dokunulabilir simgenin rolü, adı ve seçili durumu eklenmeli; metin ölçeklenmesi ve en az 48dp dokunma hedefi kontrol edilmelidir. Bu son iki koşul, mevcut koddan doğrulanmış özellik değil, uygulama sırasında sağlanacak kabul ölçütüdür. Görsel kontrastı yalnız token değerlerinden varsaymayın; gerçek medya üstü metinleri ayrıca ölçün.

## Responsive & Platform

Telefon referans Android çerçevesi yaklaşık 411×914dp'dir. Tablet boyutu **800×1280dp portre** olarak aynı AVD'de geçici ekran override ile sınandı. `ScreenUtil` Android kısa kenarı **≥600dp** olduğunda tablet dalını açılışta seçer; uygulama portreye kilitlidir. Kitaplık/uyku rayları yatay kalır, modal ortalanır, profil tam genişliğe yayılır. Her yüzey aynı şekilde yeniden akmaz: iki sütunlu kategori grid'i iki sütun kalırken profil seçenekleri üçe çıkar. Ayrıntı ve görüntü kanıtı [TABLET.md](./TABLET.md). Gerçek tablet donanımı, pencere yeniden boyutlandırma ve iPad görsel paritesi gözlenmedi; bu durumlar ayrı test ister.

## Key Flows

### 1. İlk oturum — ebeveyn çocuk için içerik arıyor

1. Uygulamayı açar, tanıtım sayfalarını geçer ve hesap seçer.
2. Kitaplıkta kategorileri ve kapak kartlarını görür; arama veya bölüm listesini kullanır.
3. Ücretsiz hikâye bilgi sayfasında süre, yaş, açıklama ve katkı sahiplerini inceler.
4. Başlatır; sayfalı okuyucuda ilerler ve bitiş görünümüne gelir.

### 2. Uyku — ninni veya rehberli ses

1. Alt sekmeden Uyku'ya geçer.
2. Ninni kartına dokunursa doğrudan oynatıcı; meditasyon veya sesli hikâyede önce bilgi ekranı açılır.
3. Ses başlar; play/pause veya çıkış kullanılır. Kilitli kart premium yüzeyine gider.

### 3. Hesap — kayıt ve geri dönüş

1. E-posta kayıt formunda e-posta, parola ve gerekli veri işleme onayı girilir.
2. Kayıt sonrası beş adımlı profil sihirbazı açılır; her adım `Skip` ile geçilebilir.
3. Ana kabuk ve kayıtlı profil görünür. Çıkış hesap ayarlarında onay modalı gösterir.
4. Tekrar giriş aynı hesapla doğrudan ana kabuğa döner; doğrulanmamış e-posta buna engel olmadı.

### 4. Ebeveyn modu

1. Ana ekran üstündeki mod seçimini açar.
2. Ebeveyn modunda günlük ücretsiz özet ve rehber listelerini görür.
3. Bilgi sayfasından metin veya sesli oynatma akışına geçer; mod seçimiyle çocuk tarafına dönebilir.

## Belgeleme kuralı

Ekran görüntüsü görülen yapıyı, kaynak kod davranışı ve sabit ölçüyü kanıtlar. Çelişkide mevcut runtime görünümü önceliklidir; kod farkı ilgili ekranın notunda saklanır. Bu `EXPERIENCE.md` ve `DESIGN.md` sözleşmesi, `mockups/` örneklerinden üstündür.
