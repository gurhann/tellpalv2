# Ağsız görsel kontrolü

Bu ekran, mevcut altı yerel dosyayı Android emülatöründe elle incelemek içindir.
Yeni görsel üretmez. Firebase girişi, backend veya medya indirmesi kullanmaz.
Normal Library, detail, reader ve ses akışına bağlanmaz.

## Kullanım

1. Ekranı dik tut. **Başlat** düğmesine dokun.
2. Yazıların kenarlarına ve küçük ayrıntılara bak. Bulanıklık, kesilme veya renk farkını not et.
3. **Sonraki** ile PNG’den aynı görselin kayıpsız WebP karşılığına geç.
4. Kapak, Sayfa 1 ve Sayfa 2 çiftlerini sırayla incele. Son görselde **Bitir** düğmesine dokun.
5. Sohbette her çift için “aynı / fark var” yaz. Fark varsa görseli ve farkın yerini belirt.

Örnek: “Kapak aynı. Sayfa 1 WebP’de alt satır bulanık. Sayfa 2 aynı.”

Başlatmadan dosya yüklenmez ve üç dakikalık süre başlamaz. Başladıktan sonra
arka plana geçiş veya süre dolması kontrolü durdurur. Uygulamayı yeniden açmak
yeni bir yerel oturum açar; otomatik tekrar yapılmaz. Dosya veya görüntü
doğrulaması başarısız olursa hata görünür; başka görsele geçilmez.
Görsel alanı **Başlat** anında sabitlenir. Başlangıç yerleşimi beklenir;
kontrol sırasında alan değişirse oturum durur.

## Kanıtın kapsamı

Dosyalar, asset `9778`, `9816` ve `9818` için daha önce hazırlanmış
822×1334 PNG ve kayıpsız WebP çiftleridir. Manifest, dosya SHA-256 değerleri,
boyut, biçim ve decode edilen RGB özeti mevcut yerel QA yükleyicisinde doğrulanır.
Görsel alanı 822×1334 fiziksel pikseldir. Ekrana sığmıyorsa küçültmek yerine
kontrol reddedilir. Bu profil üretim ayarı olarak onaylanmış değildir.

Bu PNG’ler yerel karşılaştırma referansıdır. Ekran, tam çözünürlüklü ham orijinal
ile karşılaştırma yaptığını iddia etmez. Önceki ham kanıt eksikleri korunur.
Kayıpsız WebP sonucu, JPEG veya kayıplı WebP kalitesine genellenemez.

İnsan incelemesi tamamlanana kadar kalite kabulü **bekliyor** durumundadır.
Emülatör sonucu fiziksel Android veya iPhone kalite onayı değildir. HTTP byte,
URL yenileme, gerçek erişim, üretim bellek sınırları ve P4b yaşam döngüsü kabulü
bu kontrolle tamamlanmış sayılmaz.

## Yerel hazırlık

Giriş: `app/mobile/lib/main_manual_reading_images.dart`.
Yalnız `TELLPAL_MANUAL_IMAGE_QA=true` seçilmiş native debug çalışmasında açılır.
Flutter Driver veya VM komut bağlantısı gerektirmez.

Bu küçük kompozisyon mevcut, henüz Git’e alınmamış `local_lossless_qa*`
çalışmasını değiştirmeden kullanır. Tek başına temiz bir checkout için hazır
ürün özelliği değildir. Bu bağımlılıkları sessizce commit’e ekleme veya bu
commit’i tek başına dağıtma. Ürün entegrasyonu ayrı iş olarak kalır.

Özel kaynak/test/build kopyası ve mevcut altı dosya:
`/Volumes/MAC_EK/tellpal-qa-private/manual-offline-images-20261019-01/`.
Kaynak özeti, test/analiz logları ve ekran kanıtları burada tutulur.
Eski QA köklerine yazılmaz. Token, özel indirme URL’si ve imzalama sırrı alınmaz.
