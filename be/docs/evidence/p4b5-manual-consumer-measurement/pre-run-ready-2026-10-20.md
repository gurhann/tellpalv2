# P4b5 elle ölçüm: yerel hazırlık tamam

2026-10-20. Onaylı yerel uygulama, test, APK hazırlığı ve üç inceleme tamamlandı. Gerçek kurulum, başlangıç, backend, giriş ve ölçüm yapılmadı. Bunlar ana planın açıkça istediği ayrı kullanıcı onayını bekliyor. P4b5 veya P4b tamamlanmış değildir.

## Seçilen sürüm ve kanıt

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-consumer-measurement-20261020-be80ce1`.

| Kayıt | Değer |
|---|---|
| Kaynak | `source-0007` |
| APK | `android/builds/build-0012/consumer.apk` |
| Paket | `com.tellpal.tellpal_mobile_poc.readingmanualqa` |
| APK SHA-256 | `5b38b105378d16ed71bda301587bbd93dac5d6ed0cbfd8789fb47820a65e98a5` |
| Kaynak bağı | `source-binding-0012.private.json` / `dfa1384c70e29d422c721f813ea19ae45e2f6e41a095d817b74b7800b124a975` |
| Koşu bağı | `run-binding-0006.private.json` / `5566ff98e5d54f8b08c877340d040d0c3723803c02db9ca38c4a20febdf891df` |

Son düzeltmeler ve test ayrıntıları [0003 raporundadır](verification-review-0003-2026-10-20.md). Flutter 188, Java 24 ve son Python paketi 66 test geçti. İlgili Python paketi 22 testtir; tam pakete dahildir. Son iki Python dosyası değişirken Flutter/Java kanıtı 1076 kaynak ve 1025 class dosyasının byte eşitliğiyle korundu. Analyzer temizdir. Yerel testler gerçek indirme veya cihaz kaynak kullanımı kanıtı değildir.

## Son üç inceleme

Üç inceleme aynı source-0007/build-0012/run-binding-0006 sürümünü değerlendirdi. Açık bulgu kalmadı. Önceki inceleme ve hata kayıtları tarihsel halleriyle korundu. Sonuçlar özel kökte yeni dosyalar olarak kaydedildi; kullanıcı koşu onayı yerine geçmez.

| İnceleme | Sonuç | Özel kayıt / SHA-256 |
|---|---|---|
| `p4b5_manual_blind_final` | Uygulanabilir bulgu yok | `reviews/pre-run-0006-01.private.json` / `3606df89f01ce17a6e0cdd765bbf7cbde14afe736d5318e3c6985d6cdb194fb3` |
| `p4b5_manual_edge_v2` | Bulgu yok | `reviews/pre-run-0006-02.private.json` / `8439c6015c7c23d0f1d8f1205dad9bdf670744d6e1e20fb280db8613c2507d04` |
| `p4b5_manual_verification_v2` | Doğrulama boşluğu yok | `reviews/pre-run-0006-03.private.json` / `fd9fd0d145e86458b7b4d27a55066170780a17513604a918a977d9a8e77b88df` |

Üst ajan 1162 kaynak, 1303 classpath ve 1388 Python girdisini bağımsız hash kontrolünden geçirdi. Önceki iki gerçek denemenin 18 dosyası ve tarihsel ledger değişmedi. Başlangıçtaki ilgisiz değişikliklerde, `.memlog.md` dahil, fark yoktur.

Kopyadaki gerçek controller ile salt okunur kaynak/Python/önceki proof kapıları ve üç inceleme bağı tekrar doğrulandı. Tam koşu kapısı, eksik `execution-approval-01.private.json` nedeniyle beklendiği gibi kapalı kaldı. Gerçek çalışma claim'i sıfırdır. Güncel DB, auth veya storage sorgulanmadı.

## Onaya sunulan tek gerçek koşu

Bu teklif yukarıdaki APK ve koşu bağı içindir. [Sabit sayısal önerinin](bounded-run-proposal-2026-10-20.json) SHA'sı `f45fb1ac17bc8227af3fae5b284efd614409d590d57749fcff49e942a6e3ee92` olarak korunur.

- Mevcut Android emülatöründe en çok bir QA kurulumu, ilk başlangıç, backend başlangıcı ve kanonik QA hesabıyla giriş.
- Kullanıcı hazır olduktan sonra tek 120 saniyelik pencere. En çok 1 manifest, 12 status, 4 medya / 13.170.884 byte ve toplam 17 HTTP.
- Mevcut #94 PNG'leriyle detail → sayfa 1 → sayfa 2 → detail → arka plan → geri dönüş → kapatma. Kullanıcı ekrandaki aşama yönergelerini izler; dört çizim host kaydına bağlanır.
- Eski kullanım borcu ve genel tavanlar korunur. Yeni üretim, bulut yazımı, otomatik tekrar, fiziksel cihaz veya dağıtım açılmaz. Çıktılar MAC_EK'te kalır; eski dosyaların üzerine yazılmaz.

Ayrı onaydan sonra mevcut bağlantı ve yerel kapılar doğrulanır. Süre, kimlik, bütçe veya kanıt hatasında deneme başarısız kapanır; yeni deneme kendiliğinden açılmaz. Sonuç ne olursa olsun terminal, cleanup ve son incelemeler hazırlanır. Görevle sınırlı yerel commit bu aşamanın sonundadır; şu anda commit veya push yapılmadı.

Fiziksel Android/iPhone kalitesi, RSS/GPU zirvesi, üretim profili, erişim/medya teslimi/URL yenileme/HTTP cache/session/retention kararları ve normal mobil tüketici geçişi açık kalır. P0 ham kanıt eksiği korunur. Android 1× ve iPhone'daki sınırlı 0,75× gözlemler genel profil onayı değildir; JPEG/WebP bulanıklığı çözülmüş sayılmaz.
