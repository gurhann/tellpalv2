---
title: 'P4a5: yerel DB hesabı ve tek kurtarma hazırlığı'
status: 'done'
baseline_commit: '571a79b2b07941caffa0a3541f21d347138f82a5'
approval: '2026-10-18: A — Planı uygula ve tek denemeyle devam et'
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/be/docs/project-memory.md'
  - '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md'
  - '{project-root}/architecture.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-p4a5-existing-consumer-smoke.md'
  - '{project-root}/be/docs/evidence/p4-consumer-qa/local-backend-refusal-2026-10-18.md'
  - '{project-root}/.codex/skills/run-maven-tests/SKILL.md'
---

<frozen-after-approval reason="Kullanıcı tek yerel kurtarma planını A ile onayladı">
# P4a5: mevcut yerel hesapla tek kurtarma denemesi önerisi

Tarih: 2026-10-18. Durum: **onay bekliyor; uygulanmadı**.
Ana plan [P4a5 spec](../spec-p4a5-existing-consumer-smoke.md), değişmeyen sayısal öneri
[bounded run](../../../be/docs/evidence/p4-consumer-qa/bounded-run-proposal-2026-10-18.json).

## Sorun ve doğrulanmış çözüm

Bekleme APK'sı ve VM geçti. Yerel backend, başlatma komutunda DB kullanıcısı aktarılmadığı
için var olmayan `tellpal` rolüne bağlandı ve durdu. Mevcut `gurhankucuk` hesabının aynı
V37 klonuna salt okunur bağlantısı geçti; borç 25 işlem/31.572.086 byte değişmedi.
İlk backend ledger'ı bütün ek sayaçlar sıfırken kapandı. Login ve pencere açılmadı.

Çözüm yeni rol açmak değildir. QA başlatma girdisi yerel kullanıcıyı açıkça taşıyacak;
varsayılan role sessiz düşüş backend öncesinde reddedilecek. Ürün veya erişim politikası
değişmez. [Gerçek hata ve kapanış kanıtı](../../../be/docs/evidence/p4-consumer-qa/local-backend-refusal-2026-10-18.md)
korunur.

## Onaylanması istenen kapsam

Yerel kaynak düzeltmesi, testler ve üç hedefli inceleme sonrası aynı emülatör/APK ile
**bir ek bekleme başlangıcı ve bir ek yerel backend hazırlığı**. Yerel kapılar geçerse
önceden onaylı tek gerçek ölçüm yürür. Bu onay yeni giriş/pencere/medya hakkı eklemez.
Onay bütün hazırlık ve tek çalışmayı kapsar; aynı sınırlar tekrar sorulmaz.

- Gerçek yerel QA başlangıcı toplamı en çok 3, backend başlangıcı toplamı en çok 2 olur.
  Önceki başlangıç/rezervasyon kayıtları tüketilmiş kalır; silinmez veya yeniden açılmaz.
- Aynı özel kök kullanılır. Yeni backend kanıtı yalnız `backend-02/` altında tutulur.
  İlk `backend/` dosyaları aynı yollarında kalır. Yeni QA bağlantısı/niyet/hata kayıtları
  yeni numara alır; eski `connection.private.json` ve manuel reservation korunur.
- Yeni açık A kaydı, ilk backend failure/kapalı sıfır-tüketim ledger SHA'larına, son
  cleanup ve salt okunur DB kanıtına, önceki yerel reservation'a ve aynı APK'ya bağlanır.
  Backend ve QA start payları çağrıdan önce exclusive ayrılır. Eksik/yanlış onayda çağrı yoktur.
- Aynı V37 runId, klon ve borç kullanılır. İlk hazırlık ledger'ında herhangi bir ek
  harcama veya pencere varsa kurtarma reddedilir. Hiçbir bulut sayacı sıfırlanmaz.
- Yeni DB rolü, schema, üretim profili, kaynak görsel, encode/upload/publish/worker,
  kurulum, backfill, silme veya dağıtım yoktur. Yeni config GET'i de gerekmez.

## Sınırlı uygulama

1. `P4ConsumerQaSetup.java` ve testleri: açık yerel DB kullanıcı girdisini komut satırı
   datasource ayarına bağla; eksik/yanlış hesapta Spring/SDK öncesi ret. Yalnız bu onaya
   bağlı sabit `backend-02/` dizinini kabul et. Eski sıfır ledger/failure hash'i zorunlu olsun.
2. `p4_consumer_qa.py` ve testleri: yalnız bu olaya özel ikinci yerel onay/rezervasyon
   kaydıyla yeni backend/connection dosyalarını seç. Eski tek kullanımlık kapılar geri
   açılmasın. Genel retry mekanizması, yeni transport/storage process/IPC eklenmesin.
3. Etkilenen Python/Java koruma testleri; gerçek adapter ve kompozisyon sınırları;
   kaynak/özel kopya, frozen metin, eski dosya/hash ve `git diff --check` kapıları.
   Mobil/APK kaynakları değişmediği sürece yeniden build yoktur. Üç hedefli inceleme sonrası
   salt okunur mevcut DB/borç ve bekleme VM doğrulanır; ardından yalnız onaylı tek çalışma açılır.
4. Yeni sonuç, byte/sayaç, gerçek geometri/paint ve cleanup MAC_EK'e kaydedilir. Repo'ya
   yalnız hassas verisiz rapor yazılır. Başarısızlık yeniden deneme hakkı doğurmaz.

## Değişmeyen gerçek ölçüm sınırları

Kanonik UID ve mevcut #94/tr retained LIVE/manifest/profil korunur. En çok bir ek giriş
(kümülatif tavan 3), kapılar sonrası tek 180 saniye, dört rendition/13.170.884 byte,
manifest 1/status en çok 18/HTTP en çok 23, login dahil ek verifier en çok 28.
Eski diğer toplam tavanlar korunur. Local startup sayıları bu eski bulut borçlarından
ayrı izlenir; tüketim saklanmaz. Yeni başarısızlıkta otomatik restart, ek login veya pencere yoktur.

Given açık kurtarma onayı ve geçen yerel kapılar, when tek çalışma yürür, then dört
gerçek UI paint'i eski retained kimlik ve doğru slot/DPR/fit ile doğrulanır; kaynak
sahipliği kapanır. Ortam/kimlik/süre hatasında gerçek sonuç başarısız/açık kalır.
P4b, genel tüketici geçişi, üretim profili ve fiziksel cihaz kalite kabulü kapanmaz.
</frozen-after-approval>

## Onaylı uygulama ve devir

Kullanıcı yukarıdaki planı call_560b96125e194407b87bf43dc3db6a95 yanıtında A ile onayladı.
Bu kayıt ana spec'in donmuş metnini değiştirmez. Yalnız belirtilen tek ek yerel QA/backend
hazırlığı yetkisini ekler; bulut/ölçüm sayıları ve başarısızlık sınırı korunur.

Özel kök `/Volumes/MAC_EK/tellpal-qa-private/p4a5-consumer-smoke-20261018-7e031feb`.
Gerçek başarısızlık/cleanup/hash bağları `receipts/root-local-backend-refusal-handoff-01.private.json`
içindedir. `protected-before.private.json` 3241 repo ve 72 eski run dosyasını korur.
Kurtarma öncesindeki bütün mevcut özel QA dosyaları da yeni değişmez makbuzda mühürlenir.
Yeni onay, QA/backend rezervasyonları ve numaralı aktif backend/connection seçimi yalnız
bu sabit olay/hash kapsamı için hazırlanır; eski kapılar geri açılmaz, dosyalar taşınmaz.

Uygulama ajanı yalnız kaynaklar, anlamlı Python/Java testleri, hassas verisiz rapor ve
kaynak kimliği üretir. Mevcut `source/` özel kopyasında yalnız gerekli değişen kaynakları
güncelleyip offline test yapabilir; credential/token kopyalanmaz. Test logları ayrı
numaralı CREATE_NEW dosyalardır. Ara devre kadar gerçek backend/ADB/Firebase/login/Storage,
APK build/kurulum/start veya pencere açma yoktur. Root üç hedefli inceleme sonrasında
tek gerçek çalışmayı sahiplenir. Kurulum/indirme/Colima veya ilgisiz süreç işlemi yoktur.

Eksik DB kullanıcı girdisi Spring başlamadan reddedilir. Doğrulanmış yerel kullanıcı
`gurhankucuk` yalnız bu test başlatma girdisine aittir; ürün config varsayılanına yazılmaz.
Backend'in eski failure/ledger/cleanup borcunu doğrulayan ön kapısı gerçek çalışmadan önce
hata harcamasının sıfır olduğunu kanıtlar. Yeni backend startup rezervasyonu JVM'den,
QA rezervasyonu cihaz çağrısından önce exclusive olmalıdır; ikisi ayrı tüketilir.
Sonraki başarısızlık otomatik veya gizli ek hak üretmez.

Root gerçek girişte `tools/image-optimization/.venv/bin/python` ortamını, VM sürüşünde
websocket modülü bulunan mevcut sistem `python3` ortamını kullanır. Giriş yardımcısının
zaman/boyut/UID bağları ve doğrudan private token dosyası korunur. Ortamları birleştirmek
veya yeni paket kurmak gerekmez. Aynı runtime-auth/web-api.private.json ve kullanıcı
seçimli mevcut 0600 Admin dosyası kullanılır; yeni config GET'i yapılmaz.

Ara devir, yeni onay/rezervasyon/aktif path şemalarını ve gerçek çalışma komutlarını
sırlar olmadan açıkça belgeler; root başlamadan kuralları tek tek denetleyebilir.
Ana görevin gerçek kabulü tamamlanmadan normal BMad son inceleme/commit adımına geçilmez.

## 2026-10-18 kurtarma sonucu

Onaylı tek ek QA/backend başlangıcı ve önceden onaylı tek giriş/pencere kullanıldı.
Dar gerçek kabul geçti; eski başarısızlıklar ve sayaçlar korundu. Yeni backend
`backend-02/`, yeni bağlantı `connection-03.private.json` altında kaldı.
[Gerçek sonuç ve kapsam sınırları](../../../be/docs/evidence/p4-consumer-qa/verification-2026-10-18.md).
Normal BMad son inceleme ve yerel kayıt adımları ana spec üzerinden tamamlanır.

## 2026-10-18 offline uygulama devri

Dar kurtarma kaynakları ve testleri uygulandı. Python 79/79 ve Java 17/17 geçti.
3241 korunan repo dosyası, 72 eski run dosyası, 94 özel dosya ve iki frozen onay bloğu
değişmedi. Üretim ve mobil kaynakları değişmedi; yeni APK build/kurulum yapılmadı.
Gerçek A/reservation dosyaları, backend/QA başlangıcı, login, Storage ve pencere bu dilimde
oluşturulmadı/başlatılmadı. Üç hedefli inceleme ve gerçek tek çalışma root'a devredildi.

Kesin yeni onay/rezervasyon/aktif yol şemaları, komut sırası ve test logları
[uygulama devrindedir](../../../be/docs/evidence/p4-consumer-qa/local-runtime-recovery-implementation-2026-10-18.md).
Spec `in-progress` kalır; ana kabul ve normal BMad son inceleme/commit henüz tamamlanmadı.
