# P4b2: Native raster ön rezervasyonu — yerel doğrulama

Tarih: 2026-10-18. Başlangıç commit'i: `6c37af44921cb85de220bee504b8fe0dfe3dfaf6`.
[Onaylı spec](../../../../_bmad-output/implementation-artifacts/spec-p4b2-raster-reservation.md)
kapsamındaki kaynak ve yerel doğrulama tamamlandı. Son kaynakta **474 test geçti;
analyzer temizdir**. Üç bağımsız incelemede bulunan tek test boşluğu kapatıldı;
aynı inceleyen kapanışı doğruladı. Açık bulgu yoktur.

## Değişiklik ve sınır

Tek runtime değişikliği `ReadingComparisonRaster` içindedir. Aynı isolate'taki doğrudan
çağrılar ve farklı gate/Cubit örnekleri private tek owner alanını paylaşır. Encoded ve
bildirilen geometri/resource ön kontrollerinden sonra, ilk `ImmutableBuffer` çağrısından
önce slot ile `BigInt(width) × height × 4` tahmini decoded byte ayrılır. Descriptor'ın
gerçek boyutları, piksel, peak, frame, süre ve image kontrolleri korunur.

Başarı rezervasyonu raster'a devreder. Buffer/descriptor/codec cleanup rezervasyonu
bırakmaz; image dispose'u bırakır. Hata/iptal cleanup tamamlanınca kendi token'ını
bırakır. Token idempotenttir ve başka owner'ı bırakamaz. Dolu kapasite mevcut
`media_refused` yoluna gider; yeni run/limit slotu sıfırlamaz. Yeni scheduler, bekleme,
otomatik tekrar, politika, DI, router, Cubit veya transport değişikliği yoktur.

`bufferCreationAttempts` ilk native buffer çağrısının hemen önündeki artan gözlemdir;
reset API'si yoktur. `reservedSlots` ve `reservedDecodedBytes` salt okunur gözlemlerdir.
Bunlar RSS/GPU veya fiziksel peak ölçümü değildir. İkinci HTTP payload'ı ret öncesi
alınabilir. Tek-parent kullanıcı yolunda kanıtlanmış iki resident Cubit çakışması,
genel bellek sorunu veya cihaz kapanması teşhisi iddia edilmez.

## Kabul matrisi

Altı frozen satırın tamamı son `25-matrix-final-serial.private.log` içinde açık adlarıyla
geçen aşağıdaki testlerle karşılanır. Eşzamanlı expanded çıktı kısa test adlarının
bazılarını atladığı için aynı son kaynakta yalnız üç görev dosyası `--concurrency 1`
ile ayrıca koşuldu: **38 test geçti**. Dar ve tam küme sonuçları ayrı tutulur.

| Frozen durum | Geçen test ve gözlenen sonuç |
|---|---|
| Kapasite boş | `P4b2 pending iptal gerçek native cleanup bitmeden slotu açmaz`: decode çağrısı döner dönmez, ilk native await bitmeden slot `1`, tahmini byte `16`, başlangıç farkı `1`. |
| Pending/live owner | Aynı pending testi ikinci doğrudan çağrıyı native başlamadan reddeder. `P4b2 aynı gate iki Cubit resident owner varken ikinci native çağrı başlamaz` iki gerçek Cubit'i kapsar. `P4b2 loopback farklı gate ikinci byte teslim edilir native başlamaz` gerçek HTTP→repository→iki Cubit→native zincirinde ikinci payload'ı ve native başlangıç farkı `0`ı doğrular; ilk image korunur. |
| Başarı | `P4b2 resident devir farklı run ve eski double-dispose yeni ownerı korur`: decode Future'ı ve native cleanup bitmişken slot/byte `1/16` kalır. Aynı gate testi canonical resident byte `64`ü doğrular. Açık yeni seçim yalnız ilk owner bırakıldıktan sonra başarılıdır; sessiz tekrar yoktur. |
| Hata/iptal | Pending testi iptal bildiriminden hemen sonra slotun hâlâ dolu, gerçek completion sonrası boş olduğunu doğrular. `gerçek header metadata/piksel bütçesini aşınca codec ve kaynak bırakılır` metadata uyumsuzluğu, geç iptal ve bozuk encoded byte cleanup'ını kapsar. `P4b2 geçersiz hedef ve ön iptal native öncesinde reddedilir` sıfır/negatif/büyük/taşan hedef, encoded sınırı ve ön iptalde başlangıç farkı `0`ı doğrular. `P4b2 kaynak piksel bütçesi hedef sığsa da native öncesinde reddedilir` eşleşen 4×4 kaynak/metadata ve 2×2 hedefte `limits(pixels:4)` ile kaynak piksel ön kontrolünü ayrı doğrular: ret, başlangıç farkı `0`, slot/byte `0/0`. |
| Farklı run/limit; double-dispose | Resident testinde aynı ve daha büyük farklı run/limit dolu alanı değiştirmez. İlk raster bırakılır, yeni owner alınır; eski raster'ın ikinci dispose'u yeni slot/byte'ı ve image'ı korur. |
| Lifecycle/close | `consumer account/background/lease/status/live temizliği ve açık yeniden admission anchor koruması` ailesinde image bırakılır ve slot/byte `0/0` olur. `gerçek QA parent binding paused/resumed olayında rasterı bırakır; açık readmission bekler`, `gerçek QA parent UI close native gate release bitmeden kapanışı tamamlamaz` ve `eski native Future kapanmadan yeni HTTP/decode yok; geç epoch boyanmaz` parent, drain ve eski epoch korumalarını doğrular. P4b1 yavaş status/parent regresyonu da geçer. Her görev test dosyasının teardown'u sıfır rezervasyonu denetler. |

## İlk hata, mutasyon ve son koşu

`initial-red-02`: rezervasyon eklenmeden, yalnız native başlangıç gözlemi ve aynı gate
iki Cubit testiyle koşuldu. İkinci owner mevcut geç kontrolle reddedildi; buna rağmen
native başlangıç farkı beklenen `0` yerine `1` oldu. Bu transient ikinci native
başlangıçtır; iki kalıcı loaded image üretildiği iddia edilmez. Ham başarısız kayıt korundu.
`focused-04`: uygulama sonrası 37 test geçti.

Yalnız özel kopyada yedi mutasyon koşuldu. Hepsi exit `1` ve beklenen assertion
hatasıyla kırıldı; derleme hatası yoktur. `mutation-summary-12.private.json` kaynak
mutasyon hash'lerini ve geri alınmış runtime hash'ini kaydeder.

| Mutasyon logu | Kaldırılan koruma / kırılan assertion |
|---|---|
| `mutation-05-remove-reservation` | Owner'a rezervasyon yazımı yok; pending slot beklenen `1`, gerçek `0`. |
| `mutation-06-late-reservation` | Rezervasyon ilk native await sonrasına taşındı; pending slot beklenen `1`, gerçek `0`. |
| `mutation-07-early-success-release` | Başarılı devirde erken release; resident slot beklenen `1`, gerçek `0`. |
| `mutation-08-missing-error-release` | Hata finally release'i yok; metadata hatası sonrası slot beklenen `0`, gerçek `1`. |
| `mutation-09-missing-dispose-release` | Image dispose release'i yok; son slot beklenen `0`, gerçek `1`. |
| `mutation-10-double-dispose-other-owner` | Tekrar dispose release'i ve token guard'ları birlikte bozuldu; eski owner yeni slotu bıraktı: beklenen `1`, gerçek `0`. |
| `mutation-11-new-run-resets-owner` | Farklı run dolu owner'ı sıfırladı; resident slot beklenen `1`, gerçek `0`. |

İlk uygulama döneminde runtime ve raster/Cubit testleri yedi mutasyondan sonra değişmedi. Consumer testine
son lifecycle sıfır assertion'ları eklendi; dört görev dosyası yeniden eşitlendi.
İnceleme öncesi kaynakta sıralı `flutter test --no-pub test/reading_images test/story_detail --reporter expanded`
**211**, `flutter test --no-pub --reporter expanded` **473** test geçti.
49 izlenen test dosyasının tamamı kopyada bulunur. Dar küme tam kümenin içindedir;
sayılar toplanmaz. `flutter analyze --no-pub`: `No issues found!`.

## İnceleme bulgusu ve son kaynak dönemi

Blind inceleme bulgu bildirmedi; edge-case sonucu boştu. Verification incelemesinin
tek `medium/patch` bulgusu kaynak piksel ön kontrolünün bağımsız test boşluğuydu.
`P4b2 kaynak piksel bütçesi hedef sığsa da native öncesinde reddedilir` eklendi:
gerçek 4×4 PNG ve eşleşen metadata, 2×2 hedef, `limits(pixels:4)`. Test ret,
değişmeyen native başlangıç sayacı ve sıfır slot/byte doğrular. Metadata uyumsuzluğu
testi `limits(pixels:16)` ile korunur. Intent, spec ve ürün kodu değişmedi.

`20-pixel-preflight-case` geçti. `21-mutation-pixel-preflight` yalnız
`declaredPixels > maxDecodePixels` ön kontrolünü kaldırdı; diğer guard'lar ve
descriptor sonrası gerçek piksel kontrolü kaldı. Test native başlangıç farkında
beklenen `0`, gerçek `1` ile kırıldı; derleme hatası yoktur. Runtime SHA-256
`90a9921fee6d8cdc2cea7241a84c52416adbb3f3e3a0f424842a5445b9008cca` geri alındı.
Makbuz: `pixel-preflight-mutation-21.private.json`. Eski 13 log ve yedi mutasyon
kaydı değişmedi; değişmeyen runtime için eski mutasyonlar tekrar koşulmadı.

Son test kaynağında `22-narrow-final`: **212**, `23-full-tracked-final`: **474**,
`25-matrix-final-serial`: **38** test geçti. `24-analyze-final`: `No issues found!`.
Bu kümeler örtüşür; sayılar toplanmaz. Önceki 211/473/37 sonuçları eski test
kaynağına aittir; yeni test ve kaynak hash'i aşağıda ayrı kaydedilir.

## Kaynak eşitliği ve koruma

Özel kök: `/Volumes/MAC_EK/tellpal-qa-private/p4b2-raster-reservation-20261018-6c37af4/`.
Mevcut SDK/cache/interpreter kullanıldı; paket, SDK veya venv kurulmadı/indirilmedi.
Yalnız izlenen gerekli kaynak/test/asset/paket ve sentetik fixture bağımlılıkları
kopyalandı. Dizinler `0700`, yeni log/makbuzlar `0600` ve CREATE_NEW'dur.

| Görev dosyası | Son kaynak SHA-256 |
|---|---|
| `reading_image_comparison_raster.dart` | `90a9921fee6d8cdc2cea7241a84c52416adbb3f3e3a0f424842a5445b9008cca` |
| `reading_image_comparison_raster_test.dart` | `eb45ef69b864c6559af0c2e13b529660014c6d631d1447c3c20e60b5c1ac57d3` |
| `reading_image_comparison_loaded_test.dart` | `7e18bc0e87ed775165bf3cfdd5f9b98b7c4901427159c6fd66c694aa4f54dacc` |
| `reading_image_consumer_qa_test.dart` | `2e7199d4ee29e50ef67dcb08f78240e98d1119cc9d255948c0738ca2655b2bf5` |

`audit-final-26.private.json`: 220 snapshot dosyasında açıklanmamış eşitlik farkı yok.
Sıralı `path + NUL + SHA-256 + LF` snapshot hash'i:
`8be24fe70604dd9ac759708fcc3f7d53a2dad30237db3acebafd17b11ef6d11d`.
Tek açık istisna: korunan dirty `main_image_experiment.dart` özel kopyada HEAD sürümüdür;
repo dosyası aynen korunur. Bütün dirty/untracked ağacın test edildiği iddia edilmez.
Göreli Dart import/export/part kapanışı ve 49 izlenen test dosyası doğrulandı.
Mevcut dört paket metadata dosyası ile sentetik TLS testinin mevcut Python interpreter'ına
yönlenen dar launcher ayrıca kaydedildi. Credential, token, `.env`, gerçek servis
config'i ve untracked local-lossless kaynakları kopyalanmadı.

3272 korunan repo, 72 eski run,
192 eski P4a5 ve 296 eski P4b1 özel dosyasında sıfır fark.
Frozen block SHA-256: `b5fc2eedc2d3eb04df45e39450cffd9e3d9c2c43aac978c5042458fdee9a4fb2`. İnceleme belgesi, `.memlog.md`, dirty giriş ve
untracked işler korunur. `git diff --check` temizdir.

## Ham kayıt kimlikleri

Komut, zaman ve log hash'leri özel kökün `receipts/` alanındadır; ham çıktılar
`logs/` altındadır. İlk hata ve mutasyon kayıtları silinmedi veya üzerlerine yazılmadı.

| Ham log | Exit | SHA-256 |
|---|---|---|
| `15-narrow-final.private.log` | 0 | `61cbfd0b03990d0c9e2194c004318d4efb126b6be94fa4c7269774de8e1efa85` |
| `16-full-tracked-complete.private.log` | 0 | `2cc4cbbff70003626f5ad21b57179b427db2d1f338baf0efc00e112295650367` |
| `17-analyze.private.log` | 0 | `9a94510c60f28720d36cb243ef4a74f7e6bab14f4aaa04f543f85488a80fd7c9` |
| `19-matrix-final-serial.private.log` | 0 | `09a7903d95044b45cc4004298fe4d397bcc359157002b88c4246e76818fed6f7` |
| `20-pixel-preflight-case.private.log` | 0 | `c9c316ad5b79d33c50943ebddaa29657c3dd77987d2ce0eb511994ac2d9d4ca5` |
| `21-mutation-pixel-preflight.private.log` | 1 | `df3cd80fd9ffd346619bd7cc87de8831cf5a4f4561800df81ab7af6147bcae27` |
| `22-narrow-final.private.log` | 0 | `9ae0253a0172cc9c88e3f39c6fe6866f505a154713f1b538293fe7942ac2673c` |
| `23-full-tracked-final.private.log` | 0 | `2c027debf831d9da85a3e69e47841a991b04af145fe8ed24adf9ff1d447bbecd` |
| `24-analyze-final.private.log` | 0 | `6b98ff936ce9f981455500a0701d0fd4323f9731313f0d0140948297335e9543` |
| `25-matrix-final-serial.private.log` | 0 | `5599ec0cf9472595a72fe6245cc25cef5664b7b8d5d0d21e239995f2ca1bd703` |
| `focused-04.private.log` | 0 | `0a2143232ee0c0673fd35ee7301a441d22f65c1d4ebb861cc36cac54f123d40d` |
| `initial-red-02.private.log` | 1 | `705e3c8ccb1aee0a6f03271539ae83a8433ec53b7cf4f8afe30c08d5915b79a7` |
| `mutation-05-remove-reservation.private.log` | 1 | `69d008725cd9ae3aa5f763d73f1dd426d5d00a7182902810a2d9a49f110d5941` |
| `mutation-06-late-reservation.private.log` | 1 | `e83bd486d43c64a5ad148cb81eb33ab6e4e3403e4031ffdc8191e9d347b71b55` |
| `mutation-07-early-success-release.private.log` | 1 | `425a09b6a452064040008ae86bcd591096e594e04f190ffcccfea3390a60de7e` |
| `mutation-08-missing-error-release.private.log` | 1 | `5981cbc4d7fff23b2839b57d766fe75d439ad64f10433bf8a5ba1c1b528f8a68` |
| `mutation-09-missing-dispose-release.private.log` | 1 | `68f9458cbef6cf7d976cda0f36e52014ac802879fbef8e535dc292c26d3608ef` |
| `mutation-10-double-dispose-other-owner.private.log` | 1 | `0322ee1e97b34f6e4f879a01f1c5f6d98178d3642cfcf277e848ee10777f6647` |
| `mutation-11-new-run-resets-owner.private.log` | 1 | `425a09b6a452064040008ae86bcd591096e594e04f190ffcccfea3390a60de7e` |

## Açık kalan kapılar

Yalnız P4b2 dosyaları yerel commit kapsamındadır. Push/PR, gerçek Firebase/storage/backend/cihaz koşusu,
üretim erişimi, upload, sayaç reseti, silme veya dağıtım yapılmadı. Fiziksel Android/iPhone
kalite, RSS/GPU/peak bütçesi, genel P4b ve üretim profili kabulü açık kalır.

## Bağımsız inceleme kapanışı

Üç bağlamsız inceleyen oturum modelini devraldı; model override yapılmadı. Hepsi
başlatıldıktan sonra sonuçlar değerlendirildi. Başlangıçta var olan korunan değişiklikler
envanterde ayrıldı; tracked/untracked tam diff ve P4b2 görev diff'i oluşturuldu.
Blind/edge bulgu bildirmedi. Tek verification bulgusu `medium/patch` olarak sınıflandı;
test ve sekizinci mutasyon eklendikten sonra aynı inceleyen bulgunun kapandığını doğruladı.
İnceleme düzeltmesinde frozen intent ve runtime değişmedi. Root 19 log hash'ini, dört kaynak kimliğini ve son
matris logundaki 16 kabul/regresyon test adını ayrıca doğruladı.

Sonuç metinleri özel kökün `reviews/`, ilk sınıflandırma
`receipts/review-triage-01.private.json`, kapanış
`receipts/review-closure-02.private.json` dosyasındadır. Kapanış makbuzu SHA-256:
`07239db76ed38fbdc09c3dbf7ac0c313842afbbf24cc84fccc9f33dbfefd8a2a`.
