# DevTools ağsız gözlem — terminal durumda durduruldu

2026-10-20. Sonuç: **`NOT_RUN_TERMINAL_BACKGROUND`, 0/6**. Bağlantı başarılıdır; yeni altılı gözlem başlamamıştır.

## Bulgular

- Yalnız mevcut `emulator-5554` ve `com.tellpal.tellpal_mobile_poc.offlineimageqa` incelendi. PID `4898`, `/proc` startTicks `60922` olarak korundu.
- VM `getVM` ve `getIsolate` yanıtları doğru girişi gösterdi: `package:tellpal_mobile_poc/main_manual_reading_images.dart`. Mevcut SDK DevTools Memory ekranında canlı grafik ve sınıf listesi görüldü. DevTools sunucu süreci `44796` ve önceki sahiplik kaydı eşleşti.
- Android durum bilgisi QA activity'sini `RESUMED`, görünür ve 822×1828 alanında gösterdi. Ancak ekran **“Kontrol arka plana geçince durdu. Uygulamayı yeniden açabilirsin.”** mesajını ve devre dışı **“Kontrol durdu”** düğmesini gösterdi. Mevcut terminal durum, ön planda olmasına rağmen devam edemez.
- Başlat, Sonraki veya Bitir düğmesine basılmadı. Yeni gözlem penceresi açılmadı. Background geçişinin zamanı ve nedeni bu kontrolde belirlenmedi.
- Yeni build, kopya, kurulum, uygulama/emülatör başlangıcı, hot reload/restart, GC veya heap dump yapılmadı. Backend, DB, Firebase girişi, medya HTTP, encode, upload ve dağıtım başlatılmadı.
- Kurulu APK için önceki bağlantı kaydındaki SHA-256 `ecd6b4bba5c87701158471b4db058053d751e7cb0d391669e3240ba23095a656` korunur. Bu tur kurulu APK yeniden hash'lenmedi; güncel hash doğrulaması iddia edilmez.

## Yalnız bekleme örnekleri

Altılı gösterim başlatılmadan bağlantı kontrolü sırasında aynı VM'den üç örnek alındı. Ekranın terminal durumda olduğu bu örneklerden sonra görüldü; background geçişinin örneklere göre zamanı bilinmiyor. Bunlar altı dosyanın bellek eğrisi değildir. İlk ham kayıttaki `READY_VM_SAME_PROCESS` yalnız VM bağlantısını doğrular; uygulamanın Başlat durumunda olduğunu doğrulamaz. Eski ham kayıt değiştirilmedi.

| İlk örneğe göre süre | Dart heap (byte) | External (byte) | VM process RSS (byte) |
|---:|---:|---:|---:|
| 0 ms | 76.881.808 | 3.472 | 168.730.624 |
| 1.091 ms | 76.891.024 | 3.472 | 168.730.624 |
| 2.241 ms | 76.900.912 | 3.472 | 168.730.624 |

Heap kapasitesi üç örnekte de 81.182.720 byte oldu. GPU ve mutlak peak ölçülmedi. Bu örneklerden codec üstünlüğü, sızıntı veya fiziksel cihaz/P4b kabulü çıkarılmaz. Mevcut checksum/readback ve debug/tool ek yükü sürer; önceki DevTools mode etiketi farkı çözülmedi.

## Yeni kanıtlar

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/devtools-offline-20261020-01/`. Ham dosyalar repoya kopyalanmadı; VM erişim adresi bu raporda yer almaz.

| Yeni dosya | SHA-256 |
|---|---|
| `observation-preflight-0001.json` | `6ade372301143a2dbc3ae79cf25f9ac8e62827d92b49ded23c7c73e9879237e0` |
| `observation-before-0001.png` | `6b84ee914844a6d9b036db8d2a8f824b2d5987c28568ba27bed2249f28e3af03` |
| `observation-stopped-0001.json` | `7b2bac860ceeb5c0ecab349429157ed4ff47a1697bb636f738cec06ed60e5281` |
| `observation-result-0001.json` | `a2545d56cc54ed865b84fdd876f1e65852deceba587b78dbdb5a4e4d2c2cb4be` |

Sonuç JSON'u önceki yedi özel dosyanın ve yeni ham çıktıların byte/SHA listesini içerir. Önceki yedi dosyanın tüm hash'leri değişmedi. Aşağıdaki korunan repo dosyaları da önceki `preserved-inputs-0001.json` kaydıyla eşleşti:

| Korunan dosya | SHA-256 |
|---|---|
| `_bmad-output/party-mode/memories/installed/.memlog.md` | `338cc79fa0194c80e533a1bfc01c0a3349959cf88a6dfd5177c9c8f3e9e1800c` |
| `app/mobile/lib/main_image_experiment.dart` | `c56e1941ec45ab503a560d2590c6acc2220d2549224d971aa071580ba653ae17` |
| `be/docs/evidence/p4b5-manual-retry-measurement/bounded-run-proposal-2026-10-20.json` | `a3aab71cd8ae4685888c6d7d8004332bfc5fd9ff6d7ef91decb34e44ecd65fc6` |

## Açık kalanlar

Kod değişmedi; build/unit test çalıştırılmadı. Altı dosyanın sıra/format gösterimi, Başlat→Bitir akışı ve son bellek kaydı tamamlanmadı. Yeni başlangıç mevcut onayın kapsamına alınmadı. Süreç yeniden başlatılmadı; terminal davranış ve 180 saniyelik koruma korundu. Eski kayıtlar, tüketimler, V37/auth borcu ve ağ ölçümü sınırları değişmedi. Fiziksel cihaz, gerçek consumer lifecycle, üretim profilleri ve P0/P4b kapıları açıktır.
