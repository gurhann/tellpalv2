---
title: 'P4b5 — DevTools ile ağsız gözlem'
type: 'chore'
created: '2026-10-20'
status: 'done'
baseline_commit: '3ec09d1e124080d9ffebf5ec4cd098cbd6a177b8'
review_loop_iteration: 0
context: ['{project-root}/AGENTS.md']
---

<frozen-after-approval reason="Kullanıcının DevTools yönlendirmesi; sonraki gözlem plan onayında sabitlenir">

## Amaç

Mevcut ağsız QA uygulamasını SDK'nın DevTools aracıyla gözlemle. Bağlantıyı doğrula; sonra altı yerel dosyayı sırayla açıp bellek eğrisini kaydet. Özel yürütücüyü genişletme.

## Sınırlar

**Daima:** Mevcut APK, altı dosya, eski kayıtlar ve tüketimler korunur. Yalnız `offlineimageqa` paketi, `emulator-5554` ve mevcut SDK kullanılır. Yeni ham çıktı MAC_EK'te ayrı dosyalara gider. VM adresi özeldir; normal uygulama/ses/cache değişmez.

**Önce sor:** Yeni build/kopya/kurulum/başlangıç, indirme, fiziksel cihaz veya gerçek auth/medya gerekirse somut kapsamı sun. Eski P4b5 onayı veya claim'i yeniden açılmaz.

**Asla:** Üretim, encode, upload, DB/backend, Firebase girişi, medya HTTP, silme veya dağıtım yok. Hot reload/restart, otomatik tekrar, zorunlu GC ve heap dump yok. Emülatör gözlemi fiziksel performans/P4b kabulü değildir.

| Durum | Beklenen |
|---|---|
| Mevcut PID/VM | Doğru entry, bağlı Memory, canlı örnekler |
| Süreç/bağlantı yok | Kaydet ve dur; tekrar başlatma |
| Background/alan değişimi/süre dolumu | Mevcut terminal davranışını koru |

</frozen-after-approval>

## Kod haritası

- `app/mobile/lib/main_manual_reading_images.dart`: mevcut debug girişi ve DI; değiştirme.
- `app/mobile/lib/app/manual_reading_images_qa.dart`: Başlat → altı görsel → Bitir; background terminal, resume ile devam yok.
- `app/mobile/lib/app/local_lossless_qa/{pins,assets,cubit}.dart`: sabit dosya doğrulaması, seri decode, tek raster, mevcut 180 saniyelik koruma.
- [Bağlantı raporu](../../be/docs/evidence/devtools-offline/connection-2026-10-20.md): gerçek kanıt ve saklama yolları. SDK DevTools doğrudan VM'ye bağlanır; `flutter attach` kullanılmaz.

## Görevler ve kabul

- [x] Kurulu APK/altı dosyayı doğrula; aynı süreçte DevTools Memory bağlantısını ve üç canlı örneği kaydet.
- [x] Eski planı koru; sade plan ve bağlantı raporu yaz.
- [x] Onay sonrası başlangıç gözlemi → Başlat → altı görsel → Bitir → son gözlem. Başlat sonrası en çok 120 saniye; mevcut 180 saniye korumasını değiştirme, uzatma yapma.
- [x] Sıra/format ve heap, external, RSS eğrisini kısa rapor/ekranla kaydet. Eksik kanal ölçülmedi kalır; mutlak peak/GPU veya codec üstünlüğü çıkarma.

**Kabul:** Verilen mevcut uygulama, bağlantıda aynı PID/startTicks ve doğru entry ile örnek üretir. Verilen gözlem onayı, normal düğmelerle sıralı gösterim ve son bellek kaydı sağlar. Verilen hata, başarı veya otomatik tekrar üretmez.

## Doğrulama ve açık kapılar

Bağlantı ve onaylı tek altılı gözlem tamamlandı. Bitir ekranı 116,481 saniyede, son bellek örneği 116,175 saniyede kaydedildi. 122 örneğin eğrisindeki 18,319 ve 9,220 saniyelik boşluklar açık bırakıldı. [Tamamlanan gözlem raporu](../../be/docs/evidence/devtools-offline/observation-completed-2026-10-20.md). Kod değişmedi; build/unit test gerekmedi. Gerçek VM RPC ve Memory ekranı kanıttır. PNG/kayıpsız WebP seti canonical original karşılaştırması değildir. Checksum/readback ve debug/tool ek yükü belirtilir; RSS düşüşü tek başına sızıntı kanıtı olmaz.

Ağ ölçümü ayrı kalır: #94, 120 saniye, 1 manifest, en çok 12 status, 4 medya/13.170.884 byte ve 17 HTTP; eski V37/auth borcu korunur. Bu adım borcu sıfırlamaz. Erişim/cache/session/üretim profili, fiziksel cihaz, gerçek consumer lifecycle ve P0/P4b kapıları açıktır. Network görünümü tüm native trafiği kanıtlamaz. DevTools mode etiketi farkı bağlantı raporunda açık tutulur.

## Değişiklik kaydı

2026-10-20: Kullanıcı DevTools'u seçti. Önceki manual-retry ve iki açık test bulgusu korunur; gerçek çalışma açılmadı.

2026-10-20: Onaylı altılı gözlem öncesinde aynı PID/startTicks ve canlı Memory bağlantısı doğrulandı. Mevcut ekran `background` terminal durumda bulundu; hiçbir düğmeye basılmadan `NOT_RUN_TERMINAL_BACKGROUND`, 0/6 olarak duruldu. Yeni başlangıç/otomatik tekrar yapılmadı; görevler açık kaldı. [Gözlem kaydı](../../be/docs/evidence/devtools-offline/observation-2026-10-20.md).

### Altılı gözlem önkontrolü ve önerilen tek başlangıç

Kullanıcı altılı gözlemi onayladı. Önkontrolde PID 4898/startTicks 60922 ve VM doğruydu; Android ekranı terminal `background` durumundaydı. Hiçbir düğmeye basılmadı; yeni altılı gözlem 0/6 kaldı. Bu oturum tekrar açılmadı.

**Ek onay bekleyen dar devam:** Aynı APK hash'i ve süreç sahipliği tekrar doğrulanınca yalnız `com.tellpal.tellpal_mobile_poc.offlineimageqa` bir kez kapatılıp bir kez açılır. Yeni APK/kopya/kurulum yoktur. DevTools yeni VM'ye bağlanır; Başlat hazır değilse durulur. Mevcut Android SDK/ADB ile yalnız ekranda doğrulanan Başlat → Sonraki → Bitir düğmelerine dokunulur. Yeni kaynak, özel kontrol protokolü veya uygulama kodu eklenmez.

Tek altılı geçişte 120 saniye, mevcut 180 saniyelik uygulama koruması ve diğer bütün sınırlar sürer. Hata/timeout/background halinde kayıt korunur; ikinci başlangıç veya otomatik tekrar yoktur. Eski P4b5 kurulum/başlangıç hakları iade edilmez; bu yeni ağsız başlangıç ayrı sayılır. Yeni auth, backend/DB, medya HTTP veya bulut işlemi açılmaz. Bu ek onay gelmeden süreç durdurulmaz veya açılmaz.

2026-10-20 ek onay: Kullanıcı “Evet, tek yeniden açılışı ve ADB ile altılı gözlemi onayla” yanıtını verdi. Yukarıdaki somut tek kapat/aç ve normal düğme etkileşimi artık onaylıdır. Eski 0/6 kaydı değiştirilmez; yeni oturum ve sonucu yeni numaralı dosyalarla kaydedilir. Bu ek onay, başka başlangıç/kurulum/üretim veya ağ çalışmasına genişletilemez.

2026-10-20 sonuç: Onaylanan tek kapat/aç tüketildi. Kurulu APK ve altı dosya doğrulandı; yeni PID 6671/startTicks 2274284 ve doğru entry ile Memory bağlandı. Normal ADB düğmeleriyle 6/6 ve Bitir tamamlandı. Son örnek 116,175 s, bitiş ekranı 116,481 s; uzatma veya tekrar yoktur. Yeni `0002` kanıtları ayrı tutuldu; eski on bir özel dosya ve üç korunan repo dosyasının hash'leri değişmedi. Eğride ölçülmeyen iki aralık doldurulmadı; GPU/mutlak peak/codec üstünlüğü ve P4b kabulü çıkarılmadı.

## Önerilen inceleme sırası

- Sonucu ve120s sınırını buradan incele.
  [observation-completed-2026-10-20.md:1](../../be/docs/evidence/devtools-offline/observation-completed-2026-10-20.md#L1)

- Bellek değerleri ve örnekleme boşluklarını birlikte değerlendir.
  [observation-completed-2026-10-20.md:29](../../be/docs/evidence/devtools-offline/observation-completed-2026-10-20.md#L29)

- Önceki0/6 kaydı yeni başarıdan ayrı korunur.
  [observation-2026-10-20.md:1](../../be/docs/evidence/devtools-offline/observation-2026-10-20.md#L1)

- Bağlantı, saklama ve kabul sınırlarını kontrol et.
  [connection-2026-10-20.md:1](../../be/docs/evidence/devtools-offline/connection-2026-10-20.md#L1)

- Üç son inceleme ve dosya doğrulamasının kapsamını gör.
  [review-2026-10-20.md:1](../../be/docs/evidence/devtools-offline/review-2026-10-20.md#L1)
