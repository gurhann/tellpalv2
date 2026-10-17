# İlk PNG denemesi için boyut önerisi

Tarih: 2026-10-17. Kod temeli: `8f7adab818db262c9faa2385d299b3f851c3ddad`.
Durum: **Öneri; sayısal profil veya bulut çalışması onayı değildir.**

Sonraki ölçüm: MAC_EK alanı düzeldikten sonra mevcut Medium_Phone açıldı.
[Yeniden deneme](medium-phone-retry-2026-10-17.md), korunmuş ekran override'ı nedeniyle DPR `2.0`
ve `822` piksel slot genişliği kaydetti. Aşağıdaki `1080×1753` önerisi önceki `1080` piksel
geometriye aittir; yeni oturumun en küçük yeterli adayı olarak sunulmaz. Bu iki ortam için tek
bir sayısal profil kendiliğinden onaylanmış sayılmaz.

## Dayanak

MAC_EK SDK'sı ve sistem imajı ile alınmış [emülatör geometri kanıtı](emulator-geometry-2026-10-17.md)
kullanılır. `Medium_Phone` mevcut bölümde yaklaşık 682 MiB boşluk nedeniyle bu oturumda da
başlatılamadı. Verisi değiştirilmedi. Mevcut kanıtı tekrar üretmek için yeni emülatör açılmadı.
Repo yeni commit'teki tamamlanmış slot onarımıyla ilerletilir; eski APK güncel kod sayılmaz.

Seçilmiş üç canonical PNG kaynağı `1712×2778` boyutundadır. Kaynak kimlikleri ve hash'leri
[ön inceleme kaydındadır](tr71-source-preflight.json). Henüz upload/admission yapılmış sayılmaz.
Emülatör kapak slotu `1080×1200`, sayfa slotu `1080×1822,5` fiziksel pikseldir. DPR `2.625` olur.
Sırasıyla `cover` ve `contain` kullanılır. Bunlar fiziksel telefon ölçümü değildir.

## İki seçenek

| Seçenek | P2 hedef kutusu | Beklenen PNG çıktısı | Amaç |
|---|---|---|---|
| A, önerilen | `1081×1753` | `1080×1753` | Ölçülen emülatör alanının iki eksenli ihtiyacını karşılayan ilk aday. |
| B | `1712×2778` | `1712×2778` | Tam kaynak boyutunda yeniden encode kontrolü. Boyut azaltımını denemez. |

İki seçenekte de canonical kaynak kolu tam boyutta kalır. A, kaynağı yüzde 75'e indiren bir ayar
değildir; ekran alanı ve fit ile hesaplanan fiziksel ihtiyaçtır. Bu yüzden önceki Android 1×/0,75×
görsel değerlendirmeleriyle aynı deney olarak adlandırılmaz. PNG formatı, bu içerikte yeni bir
keskinlik veya kalite kabulü sağlamaz. JPEG/WebP belirsizliği ve P0 ham kanıt sınırlaması sürer.

## İki eksen ve yuvarlama

İstemci fiziksel slotun iki eksenini önce yukarı yuvarlar. Kaynak oranı `1712/2778` ile kapakta
`max`, sayfada `min` ölçeği uygulanır. Her iki durumda tam görüntü ihtiyacı `1080×1753` çıkar.
Kapak yüksekliğindeki olası kayan nokta yukarı yuvarlaması `1200→1201` olsa da sonucu değiştirmez.

P2 `EXACT_ASPECT_FIT_FLOOR` uygular. Hedef kutusunu doğrudan `1080×1753` vermek `1080×1752`
üretir; bu çıktı istemcinin ikinci eksen şartından kalır. `1081×1753` kutusu mevcut min/floor
hesabıyla `1080×1753` üretir. Motorun yuvarlama veya seçicinin yeterlilik kuralı değiştirilmez.
Bu kayıt boyut hesabıdır; encoder çalıştırılmış veya kaydedilmiş çıktı doğrulanmış değildir.

Tam kaynak `4.755.936` piksel, A adayı `1.893.240` pikseldir. Tek RGBA düzlemi varsayımıyla
sırasıyla `19.023.744` ve `7.572.960` byte eder. Bunlar toplam decoded/process/GPU bellek sınırı
veya encoded transfer tahmini değildir. PNG byte kazancı encode edilmeden bilinmez.

## Kapsam ve açık çalışma kapısı

Tam manifest üç rolü ister: `READING_COVER`, `READING_DETAIL`, `READING_PAGE`. İki sayfalı fixture
dört çıktı yükümlülüğü taşır. Karşılaştırma UI'sındaki Kapak seçimi index `1`, yani
`READING_DETAIL` girdisidir; sayfalar index `2/3` olur. Bu ekran `READING_COVER` tüketicisinin
ve normal Library/detail/reader rotalarının kabulünü sağlamaz. Aynı kutunun tüm rollere yalnız
bu dar fixture için önerilmesi, genel rol profili değildir.

Seçenek kararı sonrası, gerçek çalıştırmadan önce tek bir somut kayıt tamamlanmalıdır:

- Seçilmiş kutu/format, gerçek runtime ve profil fingerprint'leri; örtük profil yoktur.
- Ayrı ücretsiz QA content kimliği, tam iki sayfa ve kaynak hash'leri; TR71 erişimi korunur.
- İzinli local/test prefix ve kaynak/çıktı byte-piksel sınırları; gerekirse açık growth kararı.
- P2 parent/child heap, direct/RSS/temp, süre ve tek iş/encoder sınırları.
- Teslimat ve mobil buffer/decode/peak/transfer/lease/status sınırları; gerçek cihaz yeterliliği açık kalır.
- Upload/admission/capture/çıktı/doğrulama/karşılaştırma için toplam işlem/byte/nesne tavanları.
  Önceki 13 yeni nesne tahmini gerçek çağrı planıyla yeniden doğrulanmalıdır.
- Başarısızlıkta duruş, otomatik retry olmaması ve normal listelerden yalıtım kanıtı.

Bu belge o kaydın veya çalıştırma onayının yerine geçmez. Bulut üretimi, DB migration'ı, upload,
admission, yayın, silme ve dağıtım yapılmadı. P4b ve fiziksel Android/iPhone kalite/performans
kanıtı açık kalır.
