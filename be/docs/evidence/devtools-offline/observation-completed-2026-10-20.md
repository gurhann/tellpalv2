# DevTools ağsız gözlem — altı görsel tamamlandı

2026-10-20. Sonuç: **`COMPLETED_OFFLINE_DEVTOOLS_OBSERVATION`, 6/6**. Onaylanan tek yeniden açılış kullanıldı. Altı görsel normal düğmelerle sırayla gösterildi; Bitir ve son bellek kaydı 120 saniye içinde tamamlandı. Önceki `NOT_RUN_TERMINAL_BACKGROUND`, 0/6 kaydı değişmedi.

## Kapsam ve doğrulama

- Yalnız `emulator-5554`, mevcut SDK ve `com.tellpal.tellpal_mobile_poc.offlineimageqa` kullanıldı. Önce eski PID `4898`/startTicks `60922` doğrulandı. Paket bir kez kapatılıp bir kez açıldı; yeni PID `6671`/startTicks `2274284` oldu. Başka uygulama veya emülatör yeniden başlatılmadı.
- Kurulu APK yeniden okundu; SHA-256 `ecd6b4bba5c87701158471b4db058053d751e7cb0d391669e3240ba23095a656` eşleşti. APK içindeki altı dosyanın byte/SHA kontrolleri ve manifest SHA-256 `7087bc93983022fed3ad920cc9864fe0cbe090892251edf5a2fc1a155c2bdaa9` geçti. APK kopyası, build veya kurulum yapılmadı.
- Yeni VM `getVM`/`getIsolate` yanıtları `package:tellpal_mobile_poc/main_manual_reading_images.dart` girişini doğruladı. Kullanıcıya açık SDK DevTools Memory ekranı yeni VM'ye bağlandı; canlı grafik ve sınıf listesi görüldü. Başlat öncesi hazır ekran ayrıca doğrulandı. Örneklerdeki process adı `Process 6671` olarak eşleşti.
- Yalnız ekranda doğrulanan Başlat, beş Sonraki ve Bitir düğmesine ADB ile birer kez dokunuldu. Uygulama kodu, özel yürütücü veya kontrol protokolü eklenmedi. Mevcut 180 saniyelik koruma değişmedi. Hot reload/restart, otomatik tekrar, zorunlu GC veya heap dump yoktur.
- Önceki on bir özel dosyanın ve üç korunan repo dosyasının hash'leri değişmedi. Eski tüketimler ve P4b5 kayıtları korunur. Yeni ağsız başlangıç ayrı sayılır; eski hakları iade etmez.

## Sıra ve süre

Zamanlar Başlat komutunun gönderildiği ana göredir. Görseller ve sıra/format etiketleri ayrı ekran görüntülerinden doğrulandı. Süreler decode veya paint benchmark'ı değildir; araç/inceleme süresini içerir.

| Gönderilen normal düğme | Beklenen ve ekranda görülen görsel | Zaman |
|---|---|---:|
| Başlat | 1/6 · Kapak · PNG (`9778-png`) | 0,000 s |
| Sonraki | 2/6 · Kapak · kayıpsız WebP (`9778-webp`) | 15,603 s |
| Sonraki | 3/6 · Sayfa 1 · PNG (`9816-png`) | 30,639 s |
| Sonraki | 4/6 · Sayfa 1 · kayıpsız WebP (`9816-webp`) | 48,436 s |
| Sonraki | 5/6 · Sayfa 2 · PNG (`9818-png`) | 82,458 s |
| Sonraki | 6/6 · Sayfa 2 · kayıpsız WebP (`9818-webp`) | 99,372 s |
| Bitir | “Kontrol bitti. Gözlemlerini sohbette paylaş.” | Komut 116,035 s; ekran 116,481 s |

Son bellek örneğinin zaman damgası **116,175 s** oldu. Son örnek ve bitiş ekranı 120 saniyelik sınır içinde kaydedildi; pencere uzatılmadı. Bitir sonrası uzun bekleme veya zorunlu GC gözlemi yapılmadı.

## Bellek eğrisi

`getMemoryUsage` ve `getProcessMemoryUsage` ile **122 örnek** alındı. İlk örnek Başlat'tan 43,106 saniye önce, son örnek 116,175 saniye sonradır. İlk kayıt 95, ikinci kayıt 26, Bitir sonrası kayıt bir örnek içerir. İki kayıt aynı tek gözleme aittir; yeni gösterim veya yeniden başlangıç değildir.

| Kanal | Başlat öncesi son örnek (−0,144 s) | Örneklenen en yüksek değer | Bitir sonrası son örnek (+116,175 s) |
|---|---:|---:|---:|
| Dart heap, byte | 68.537.168 | 78.216.272 | 73.469.584 |
| External, byte | 2.000 | 6.078.720 | 2.160 |
| VM process RSS, byte | 340.041.728 | 363.683.840 | 348.082.176 |

En yüksek değerler her kanalın ayrı örneklerinden gelir; aynı ana ait oldukları varsayılmaz. Bunlar örneklenen değerlerdir, **mutlak peak değildir**. External tek başına bütün native/GPU belleğini temsil etmez. GPU ölçülmedi.

Örnekleme çoğunlukla yaklaşık bir saniyeliktir. **61,466–79,785 s arasında 18,319 s**, **106,955–116,175 s arasında 9,220 s** boşluk vardır. İlk örnekleyici sabit süresinde tamamlandı; ayrı son bölüm ve Bitir sonrası örnekle aralarda kayıt yoktur. Çizimde bu aralıklar gri gösterildi; noktalar arası çizgi çekilmedi. Bu aralıklardaki değişimler ölçülmedi; eğrinin her transient'i yakaladığı iddia edilmez.

Son external değeri başlangıca yakındır. Heap ve RSS başlangıcın üzerindedir. Tek geçiş ve kısa son gözlem sızıntıyı kanıtlamaz veya dışlamaz; RSS düşüşü de tek başına kaynak bırakma kanıtı değildir. Checksum, RGBA→RGB readback ve RGB checksum ile debug/tool ek yükü ölçüme dahildir. PNG/kayıpsız WebP seti canonical original karşılaştırması değildir; codec hız/bellek üstünlüğü çıkarılmaz.

## Kanıtların saklanması

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/devtools-offline-20261020-01/`. VM adresi yalnız `connection-0002.private.json` içindedir; repoya alınmadı. Ham çıktılar ve ekranlar MAC_EK'te kalır.

| Dosya | İçerik | SHA-256 |
|---|---|---|
| `observation-result-0002.json` | Sonuç, koruma kontrolleri ve 23 yeni dosyanın byte/SHA listesi | `46544d0987a5cbb6e2da5c14c1c365f265b277c480d5bf2b1184003a75968b28` |
| `reopen-preflight-0002.json` | Kurulu APK/altı dosya ve eski süreç doğrulaması | `13b639f90485c5150b04251bdd134ec6432f496b1e9377590bf7d53c532dc831` |
| `memory-samples-0002.jsonl` | İlk 95 ham bellek örneği | `7115fa636f93b209e7bbf48c2550ff2792261a14daf5178def55209f83d822db` |
| `memory-tail-0002.jsonl` | Son bölümde 26 ham bellek örneği | `159514364977608f1236d643f07219d62c7309cc98c7f61e86a3516646e2b4d3` |
| `memory-after-finish-0002.json` | Bitir sonrası ham bellek örneği | `c45d5a9caf8ecbfa73643d5af97e01896c3993490586996a029e005c8343ed1f` |
| `memory-curve-0002.csv` | Zaman, heap, external ve RSS, byte | `bfba697077df2f189cdb9b2a686b74b369a51c05d30cb2653f4d32f516314b3a` |
| `memory-curve-0002.png` | Boşlukları koruyan üç kanallı çizim | `213a1706d2e24f489de6e138dbd8e3751514357002a5aca704b89b24a9f2440c` |
| `observation-finished-0002.png` | Bitir ekranı | `e1b3551015759abab8390927ba59d839995b3b9ad079961b2eabdfb46b188487` |

`observation-case1-0002.png`–`observation-case6-0002.png` sıra/formatı, `observation-ready-0002.png` ve `observation-before-start-0002.png` hazır durumu kanıtlar. Normal düğme komutlarının zamanları `observation-start-0002.json` ve `observation-actions-0002.jsonl` içindedir. `devtools-memory-before-0002.png` ve `devtools-memory-during-0002.png` kullanıcıya açık Memory ekranlarının kayıtlarıdır. Bütün bu dosyaların hash'leri sonuç JSON'undadır. Çizim yalnız mevcut kayıtlardan üretildi; yeni ölçüm yapmadı.

## Açık kalan sınırlar

Kod değişmedi; build/unit test gerekmedi. Bu, emülatörde yerel dosya gösterimi ve gözlemsel bellek kaydıdır. Fiziksel cihaz performansı veya P4b kabulü değildir. Tarihsel debug kaydı ile DevTools'un profile etiketi farkı çözülmedi; üretim profili sonucu çıkarılmaz.

Backend/DB, Firebase girişi, medya HTTP, encode, upload, bulut veya üretim işlemi başlatılmadı. Cihazın dış ağı kapatılmadı; bütün native trafik bağımsız denetlenmedi. Ağsızlık kapsamı mevcut asset tabanlı giriş ve bu adımda auth/medya çağrısı başlatılmamasıdır. Network görünümü tüm native trafiği kanıtlamaz.

Ayrı ağ ölçümünün #94, 120 saniye, 1 manifest, en çok 12 status, 4 medya/13.170.884 byte ve 17 HTTP sınırları değişmedi. Eski V37/auth borcu sıfırlanmadı. Erişim/cache/session/üretim profili, fiziksel cihaz, gerçek consumer lifecycle ve P0/P4b kapıları açık kalır.
