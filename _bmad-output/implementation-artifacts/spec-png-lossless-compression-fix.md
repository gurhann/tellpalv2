---
title: 'PNG kayıpsız sıkıştırma ve doğrulama düzeltmesi'
type: 'bugfix'
created: '2026-10-18'
status: 'done'
review_loop_iteration: 0
baseline_commit: 'b38ea0e17f902ea2fc2d691531cca899b31d78aa'
context: ['{project-root}/AGENTS.md', '{project-root}/be/docs/project-memory.md', '{project-root}/be/docs/adr/ADR-0022-reading-image-preparation-engine.md', '{project-root}/architecture.md']
---

<frozen-after-approval reason="Kullanıcı aynı piksellerle yerel kayıpsız sıkıştırma ve doğrulama düzeltmesini onayladı.">

## Amaç

PNG encoder'ındaki `compressionQuality=1.0` sıkıştırmayı kapatır. Önceki testler
doğru görsel üretimini sınadı; sıkıştırma verimini sınamadı. Aynı ayarı descriptor
ve gerçek child için tek kaynaktan kullan; yerel adayda güçlü kayıpsız sıkıştırmayı
seç. Aynı 822×1334 piksellerin korunduğunu ve gerçek byte farkını kanıtla.

## Sınırlar

**Her zaman:** Mevcut geometri, RGB/metadata kabulü, kaynak ve süreç sınırları korunur.
Yeni encoder ayarı yeni profil/rendition kimliği üretir. Eski #94, manifest, çıktı,
ledger, hata kayıtları ve ilgisiz `.memlog.md` değişmez. Belgeler Türkçedir.

**Ayrı karar:** Yeni bulut/oturum/cihaz penceresi, üretim profili veya erişim politikası.

**Kapsam dışı:** Cloud, DB, migration, yayın, eski nesneleri değiştirme, deployment,
JPEG/WebP seçimi, mobil kod/normal tüketiciler ve P4b. Yeni altyapı eklenmez.

## I/O ve hata matrisi

| Durum | Girdi | Beklenen sonuç | Ret |
|---|---|---|---|
| Gerçek yerel kaynak | Sabit üç PNG/SHA, aynı kutu | Eski payload SHA'sı #94 ile aynı; yeni PNG'nin tüm RGB pikselleri aynı | Hash/piksel farkında başarı yok |
| Sıkıştırılabilir fixture | Gerçek child | Aynı pikseller; sıkıştırmasız kontrole göre belirgin küçük dosya | Ayar1.0 regresyonu testten geçmez |
| Eski profil | Aynı kaynak/limit, eski encoder descriptor | Yeni fingerprint/key; eski pending iş dönüşümden önce reddedilir | Eski çıktı yeni profil diye benimsenmez |
| Sınır/büyüme | Byte limiti, eşit/büyük çıktı | Mevcut ret ve açık growth kabulü korunur | Fixture yanlış nedenle geçmez |

</frozen-after-approval>

## Kod haritası

- `be/src/main/java/com/tellpal/v2/asset/domain/reading/ReadingProfiles.java:61`: encoder descriptor ve runtime/binary kimliği; ayarın tek kaynağı.
- `be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingImageChild.java:42`: gerçek PNG writer; resize değişmez.
- `be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingImageProcess.java:25`: mevcut sınırlı child girişi ve doğrulanmış süreç çıkışı.
- `be/src/test/java/com/tellpal/v2/asset/reading/ReadingImageProcessTest.java`: gerçek child ve diskten decode testleri.
- `be/src/test/java/com/tellpal/v2/asset/reading/ReadingPreparationIntegrationTest.java`: PostgreSQL/disk worker ve growth sınırları.
- `be/docs/evidence/p4-reading-images/android-measurement-2026-10-18/verification.json`: salt okunur eski payload SHA/byte ve JPEG tabanı.
- `be/var/p4a2-preflight-dc591777-9e53-4e14-856f-f13fec00fa8c/`: mevcut kaynaklar; yeniden indirme yok.
- `be/var/png-lossless-20261018-91ab1145-80bd-4dbe-a461-4fa28fa46787/`: değişiklik öncesi JAR, hash ve dependency classpath ayrıldı; eski child/profile sınıfları mevcut derlemeyle eşleşti. Bu baseline salt okunur kalır.

## Görevler ve kabul

- [x] `ReadingProfiles.java`, `ReadingImageChild.java`: tek ayarı bağla; piksel dönüşümünü koru.
- [x] İlgili Java testleri: tam RGB eşitliği, byte verimi, yeni kimlik, eski iş reddi ve growth regresyonları.
- [x] `be/docs/evidence/png-lossless-compression/`: gerçek paketli eski/yeni motorla ağsız yerel probe, bağımsız Pillow decode ve rapor.
- [x] `be/docs/adr/ADR-0022-reading-image-preparation-engine.md`, `be/docs/project-memory.md`, `be/docs/reading-image-preparation.md`: sıkıştırma anlamını ve ayrı verimlilik ölçütünü kaydet.

**Kabul:** Aynı kaynak verildiğinde gerçek eski/yeni child çalıştırılırsa, çıktı
822×1334 kalır; eski SHA mevcut #94 ile eşleşir; tüm decoded RGB byte'ları eşittir.
Rapor eski/yeni PNG ve mevcut JPEG boyutlarını ayrı gösterir. Boyut kazancı ölçülmeden
optimizasyon başarılı sayılmaz. Süre, saf encode diye sunulmaz.

**Kabul:** Aynı limitler ve eski encoder kimliği verildiğinde, yeni iş ve rendition
ayrılır; eski pending iş yeni encoder ile sessiz çalışmaz. Mevcut LIVE yeniden yazılmaz.

## Spec değişiklik kaydı

## Doğrulama

Odaklı Java regresyonları; wrapper ile tam `bash ./mvnw verify`; üç bağımsız BMad
incelemesi. Yeni JAR ve değişiklik öncesi ayrılan JAR mevcut process yolu ile çalışır.
Ağ erişimi sandbox ile kapalıdır. Kaynak ve çıktı hash'leri, bütün RGB eşitliği,
byte oranları ve süreç/decode süreleri ayrı kaydedilir. Fiziksel kalite, genel profil,
JPEG/WebP ve P0 eksik ham kanıt sınırlaması açık kalır.

Testler mevcut Colima default socket'ini kullanabilir; Docker bağlamı değişmez.
Yalnız bu iş için başlatılan boş test VM'i sonunda kapatılır. Başka süreç durdurulmaz.

## Sonuç

Tam wrapper verify 492 Surefire + 24 Failsafe = 516 testle geçti; hata ve atlama yok.
Gerçek eski/yeni paketli motor ve bağımsız Pillow üç kaynakta tüm RGB byte'larını
eşit buldu. Eski payload SHA'ları #94 ile eşleşti. PNG toplamı 9.878.163 byte'tan
5.112.408 byte'a indi (%48,25). Mevcut JPEG toplamından hâlâ %55,24 büyüktür.

Üç bağımsız incelemenin tek patch'i, optimizasyonlu Python'da assert kaybıydı.
Açık ret ve iki çalışan test eklendi; normal gerçek probe tekrar geçti. Hedefli
yeniden inceleme açık bulgu bırakmadı. Ana Java/JAR değişmedi. Önceki kayıtlar,
109 korunan dosya ve ilk başarılı yerel run hash'leri korundu.

## Önerilen inceleme sırası

- Tek ayar kaynağı descriptor ile gerçek encoder davranışını bağlar.
  [ReadingProfiles.java:10](../../be/src/main/java/com/tellpal/v2/asset/domain/reading/ReadingProfiles.java#L10)

- Geometriyi koruyarak aynı kayıpsız sıkıştırma ayarını uygular.
  [ReadingImageChild.java:45](../../be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingImageChild.java#L45)

- Gerçek kaynaklarda paketli motor, tam RGB ve byte sonuçlarını gösterir.
  [README.md:1](../../be/docs/evidence/png-lossless-compression/README.md#L1)

- Eski profil yeni iş ve çıktı kimliğiyle karıştırılamaz.
  [ReadingPreparationIntegrationTest.java:224](../../be/src/test/java/com/tellpal/v2/asset/reading/ReadingPreparationIntegrationTest.java#L224)

- Gerçek child testi sıkıştırmasız regresyonu, piksel farkını ve byte sınırını yakalar.
  [ReadingImageProcessTest.java:18](../../be/src/test/java/com/tellpal/v2/asset/reading/ReadingImageProcessTest.java#L18)

- Optimizasyonlu Python iş I/O’sundan önce açıkça reddedilir.
  [probe.py:35](../../be/docs/evidence/png-lossless-compression/probe.py#L35)
