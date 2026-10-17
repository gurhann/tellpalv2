# 822×1334 PNG denemesinin sonucu

Durum: **Backend hazırlığı tamamlandı; Android karşılaştırması tamamlanmadı.**

Kullanıcı aynı sınırlar içinde yeniden başlatmayı açıkça onayladı. Önceki hata
dosyaları hash'leriyle ayrı dosyalarda korundu. Aynı run/prefix, kaynaklar, profil,
klon ve QA oturumu kullanıldı. Yeni giriş yapılmadı. Yerel Python koruma testlerinin
29'u tekrar geçti. Java bağlantısı gerçek klonda salt okunur doğrulandı.

[Sayısal makbuz](execution-resume-2-2026-10-17.json) gerçek yürütmeyi kaydeder.
İlk iki başarısızlık makbuzu tarihsel kayıt olarak korunur.

## Tamamlanan backend kanıtı

Yalnız yerel klonda V37 uygulandı. Yeni Türkçe QA kopyası **#94** oluşturuldu.
Üç canonical PNG admitted kimliklerle bağlandı. Tek job gerçek encoder ile çalıştı;
kalıcı çıktılar doğrulandı ve ikinci reconcile otomatik LIVE oluşturdu. Elle LIVE
yazılmadı. Normal public ve Library API yalıtımı çalıştırıcı tarafından doğrulandı.

- Job: `bb9ae40b-e5ab-4054-885e-0b1fc14e841e`.
- Revision: `3ddef109a065667d6e44f19aabce9151f1295d8f1e2502a4ba5b14e29a5d458d`.
- Manifest: `d4636ac8e61967aad2f2a53d9aa1da736214075a915f2dd53b7d1d80fe1a42e2`.
- LIVE generation: `4`.
- Dört çıktı: cover, detail ve sıralı iki sayfa; tamamı **822×1334 PNG**.

Kaynak DB V36'da kaldı; eski asset_processing #112 hâlâ PENDING. Mevcut klon
kayıtlarının 11 tabloda korunduğu yapılandırılmış JSON karşılaştırmasıyla doğrulandı.
Çok satırlı JSON'u satırlara bölmek yanlış fark ürettiği için bu ilk tanılama sonucu
kullanılmadı; JSON alanları ve aynı anı gösteren saat dilimleri yapısal karşılaştırıldı.
Normal uygulama kodu ve çalışan diğer süreçler değiştirilmedi.

## Dosya boyutu sonucu

Bu değerler doğrulanmış nesne metadata'sıdır. Başarıyla tamamlanmış mobil HTTP
payload ölçümü veya görüntü kalitesi kabulü değildir.

| Görsel | Canonical PNG kaynak | Yeni PNG | Kaynağa göre azalma | Eski JPEG metadata'sı |
|---|---:|---:|---:|---:|
| Kapak/detail | 6.336.857 byte | 3.292.721 byte | %48,04 | 1.048.225 byte |
| Sayfa 1 | 5.941.567 byte | 3.292.721 byte | %44,58 | 1.122.418 byte |
| Sayfa 2 | 6.122.778 byte | 3.292.721 byte | %46,22 | 1.122.572 byte |

Üç gösterim için toplam yeni PNG boyutu 9.878.163 byte'tır. Eski JPEG toplamı
3.293.215 byte'tır; yeni PNG toplamı yaklaşık 3 kat büyüktür. Canonical PNG'ye göre
azalma, eski JPEG'e göre transfer kazancı sayılmaz. Eski JPEG cihazda kontrol kolu
olarak gösterilmedi. Cover ve detail ayrı rol/fingerprint ile aynı PNG byte'ını taşır.

## Mobil ölçümün durduğu yer

Mevcut ayrı QA APK'sı ve probe hash'leri doğrulandı. `emulator-5554`, 822×1828,
DPR 2.0 ve font ölçeği 1 ile kullanıldı. Gerçek manifest isteği başarılı oldu:
5.187 byte, 844 ms. İlk kapak yüklenirken kayıt Sayfa 1 seçimine geçti. Ölçüm aracı
bu aşamada sayfa değiştirme komutu göndermedi. Kullanıcı “Evet, düğmeye dokundum”
yanıtıyla manuel seçimi doğruladı. Böylece sabit altı durum sırası dış müdahaleyle
kesildi; bu çalışma otomatik karşılaştırma kanıtı olarak kullanılamaz.

İki `bytes` isteği başarısız kaydedildi: 1.049 ms ve 7 ms. HTTP durum kodu ve asıl
exception bu probe'da tutulmadığı için kesin hata nedeni çıkarılamaz. İptal veya
sunucu kapasite reddi olasılığı, doğrulanmış kök neden sayılmaz. İlk `loaded/paint`
makbuzu ve altı durumun ekranları oluşmadı; byte/decode/görsel kalite kabulü yoktur.
`finished=false` makbuzu korundu. Altı durumun başarılı olduğuna dair dosya üretilmedi.

Araç stop/socket/reverse/forward temizliğini tamamladı. Son tanılamada buffer,
descriptor, codec ve image sahiplikleri 0; credential ve deadline kapalıdır.
Kapanış sonrası debug emülatör örneği PSS 207.092 KiB, RSS 190.348 KiB'dır.
Bu değerler uygulama süreç örneğidir; aktif karşılaştırmanın tepe belleği veya üretim
bellek sınırının ölçümü değildir.

[Kapanış ekranı](medium-phone-failed-closed-2026-10-17.png) yalnız QA bekleme ekranını
gösterir. Başarılı görsel gösterimi olarak sunulmaz. Bize ait backend `close.request`
ile kapandı; ledger `CLOSED MEDIA_OWNERSHIP_RELEASED` kaydetti. Yeni üretim, retry,
silme veya bucket listeleme yapılmadı; kısmi kanıt ve 13 nesne korundu.

## Tüketilen bütçe

| Sayaç | Gerçek kalıcı rezervasyon / sınır |
|---|---:|
| Create denemesi | 14 / 14 |
| Yazma payload | 74.711.347 / 117.440.512 byte |
| Farklı yeni nesne | 13 / 13 |
| Yeni nesne byte | 68.374.490 / 109.051.904 byte |
| Pinned read | 41 / 64 |
| Okuma payload | 215.362.417 / 536.870.912 byte |
| Observe | 8 / 16 |
| Firebase verifier, önceki login doğrulaması dahil | 4 / 64 |
| QA sign-in, tüm denemeler toplamı | 1 / 1 |
| V37 teslimat işlemi | 4 / 64 |
| V37 teslimat byte rezervasyonu | 3.292.721 / 67.108.864 byte |

Sayaçlar SDK HTTP/chunk/auth iç çağrılarını ve TLS byte'larını ölçmez. Başarısız
mobil istemci isteğinde sunucu storage rezervasyonu tüketilmiş olabilir; harcanan
pay geri verilmedi. Önceki Java hatası storage aşamasından önceydi; arşivlenen ledger
yalnız OPEN ve daha önce tüketilmiş tek login doğrulama payını içeriyordu. Bu pay yeni
login veya yeni çağrı olarak çoğaltılmadı. İlk uygulama yapılandırması okuması önceki
makbuzdaki ayrı ön hazırlık sayımında kalır.

## Açık kalan doğrulama

Seçim değişimi kullanıcı tarafından doğrulandı. İki HTTP reddinin kesin hata nedeni
kaydedilmedi; sonraki ölçümün tanılama sınırı budur. Yeni üretim gerekmiyor.
Mevcut #94 çıktılarıyla yapılabilecek sonraki mobil ölçüm, harcanmış
bütçeyi ve süre/oturum kapılarını koruyan ayrı somut yürütme kararı ister. Mevcut
ledger veya V37 sayaçları sıfırlanamaz; yeni prefix ile tavan aşılamaz.

Fiziksel Android/iPhone kalitesi, altı gerçek render/payload/decode/bellek ölçümü,
JPEG/WebP bulanıklığı, P0 eksik ham kanıtı, genel üretim profili, normal mobil
tüketicilerin geçişi ve P4b açık kalır. Bu deneme tamamlanmış P4 olarak gösterilmez.
