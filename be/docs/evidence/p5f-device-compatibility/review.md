# P5f inceleme kaydı

Üç bağımsız katman tamamlandı: genel hata incelemesi, uç durum incelemesi ve
doğrulama açığı incelemesi. Genel katmanda uygulanabilir bulgu yoktu. Diğer
katmanların yedi bulgusu bu işin guard/test yamaları olarak giderildi. Amaç,
format bütçesi ve onaylanmış sınırlar değişmedi; ertelenen bulgu yok.

| Bulgu | Önem | Yapılan düzeltme |
|---|---|---|
| Geciken seal yanıtı hata durumunu başarıyla ezebilir | Orta | `P5fCubit` yanıt sonrası geçerliliği tekrar kontrol eder; lifecycle/deadline testleri geçer |
| Aynı nonce tekrar kurulabilir | Orta | İki kurulum yolunda tek, exclusive mutasyon sınırı; kurulmuş/belirsiz ve yarım eski çağrı retleri test edilir |
| Denemenin alt süreleri toplam süreyi aşabilir | Orta | Sampler, raster ve build süreleri `trialMicros` ile sınırlanır; eşitlik/üst sınır test edilir |
| Zorunlu kanıt kökü symlink/dosya olabilir | Yüksek | Gerçek, symlink olmayan dizin şartı; her iki ret sınanır |
| Pilot bütçesinin ret sınırı sınanmıyor | Orta | İki Pofuduk örneğinde decode/çizim ve iki bellek eşiğinin altı/eşiti/üstü test edilir |
| Native sampler koşul değişimi sınanmıyor | Orta | Aynı üretim Swift kaynakları Mac test dublörleriyle çalışır; stop, bildirim, idle geri yükleme ve seal reddi doğrulanır |
| Gerçek ekran-painter bağlantısı sınanmıyor | Orta | `P5fScreen` ve gerçek raster/painter üzerinden Cubit denemesi ilerletilir; doğrudan `painted()` çağrısıyla atlanmaz |

Sonuç: 24 P5f Python ve 27 Flutter testi geçti; analyze, iOS Swift typecheck,
Python derleme ve diff kontrolleri temiz. Önceki 15 P5e Python ve dört hedefli
Java testi ile üç gerçek CLI olumsuz senaryosu da geçti.

Yamalar telefona kurulmadı. Ölçümün imzalı artifact'i, snapshot kaydı, ham günlüğü
ve ilk özeti değişmedi. Aynı 144 ölçüm + 12 ısınma yeni doğrulayıcıdan tekrar
`SEALED_VERIFIED` geçti. Sonuçlar ve hash'ler [sayısal kayıtta](iphone-performance.json).
Canlı geçiş hâlâ `HOLD`; üretim imajı ve fiziksel panel kabulü yapılmadı.
