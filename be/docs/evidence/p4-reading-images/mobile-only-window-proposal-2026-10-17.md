# Mevcut #94 için tek ek mobil ölçüm penceresi

Durum: **Öneri; henüz onaylanmadı veya uygulanmadı.**

İlk 900 saniyelik pencere 17 Ekim 2026, 23.55.38,011 (Europe/Istanbul) itibarıyla
doldu. Yerel hazırlık, test ve inceleme bu süreyi tüketti. Süre sınırı uzatılmadı;
devam girişi gerçek Java kontrolünde context açmadan reddedildi. Yeni medya,
storage veya Firebase çağrısı yapılmadı. #94 çıktıları ve eski makbuzlar korunuyor.

## İstenen ayrı sayısal karar

1. Yerel test ve inceleme kapıları geçtikten sonra başlayan **tek 180 saniyelik**
   ek ölçüm penceresi. Pencere açılması özel ve exclusive bir makbuza yazılır.
   İlk OPEN zamanı ve başarısızlık geçmişi değiştirilmez. Yeni sayaç sıfırlama yoktur.
2. Önceki token yeterince geçerli değilse aynı kanonik QA UID için **en çok bir ek
   giriş**. Böylece tüm denemelerin sign-in tavanı 1'den 2'ye çıkar. Gerekmiyorsa
   giriş yapılmaz. Firebase verifier toplam tavanı 64 içinde kalır; bu girişin
   doğrulaması da aynı toplamdan düşülür. Yeni kullanıcı, anahtar veya IAM izni yoktur.

Bu iki değişiklik dışında kaynak, profil, depolama, erişim veya üretim sınırı artmaz.
Kullanıcının önceki “sınırları koru” isteği bu ek pencere/giriş onayı sayılmaz.

## Değişmeyen kapsam

- Aynı #94, Türkçe dil, job/revision/manifest/LIVE generation ve 822×1334 PNG profili.
- Yeni üretim, create, observe, yeni nesne, kaynak yükleme, publish/reconcile/worker,
  migration, silme, backfill veya dağıtım yoktur. Normal mobil uygulama değişmez.
- Önceki 14 create, 13 nesne, 41 read/215.362.417 byte ve 4 verifier korunur.
- En çok 23 ek pinned read; tüm denemelerde toplam 64 read/512 MiB tavanı.
  Yazma ve observe bu devamda kapalıdır. Aynı prefix korunur.
- V37 bütçesindeki 4 işlem/3.292.721 byte korunur; toplam 64 işlem/64 MiB değişmez.
- Firebase verifier toplam 64, birleşik mantıksal çağrı tavanı 159 değişmez.
  Bu devam create/observe açmadığından read/auth/sign-in en kötü toplamı da bu sınırın altındadır.
- Tek transfer/decode, aynı encoded/buffer/pixel/decoded/peak sınırları ve no-store teslimat.
- Aynı ayrı Android QA APK'sı; sabit altı durum ve gerçek paint/payload/decode/ekran/bellek
  kanıtı. Ekran, hesap, arka plan ve kapamada sahiplik temizliği zorunludur.

Hata veya eksik kanıt başarı sayılmaz. Yeni otomatik tekrar açılmaz. Fiziksel
Android/iPhone kalitesi, JPEG/WebP, P0 ham kanıtı, genel üretim profili ve P4b açık kalır.
