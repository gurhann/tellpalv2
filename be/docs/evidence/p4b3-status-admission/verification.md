# P4b3: Status sonucunu admission yaşamına bağlama — yerel doğrulama

Tarih: 2026-10-18. Başlangıç commit'i: `b0ef8292a87127b81839a601053d658e952544d4`.
[Onaylı spec](../../../../_bmad-output/implementation-artifacts/spec-p4b3-status-admission-ownership.md)
ve [salt okunur inceleme](../../../../_bmad-output/implementation-artifacts/image-optimization/p4b3-status-admission-investigation.md)
izlendi. Model oturumdan devralındı; model override yapılmadı. Yerel uygulama,
doğrulama ve üç bağımsız BMad incelemesi tamamlandı. Son koruma denetimi ve yalnız
görev dosyalarının yerel commit'iyle kapanır; push/PR yapılmaz.

## Değişiklik

Tek runtime alanı `ReadingImageComparisonCubit` içindeki status sahipliğidir.
`_checkStatus` artık çizim `_epoch` değerini yakalamaz. Normal sonuç ve exception,
ayrı private `_ownsStatus` denetimini kullanır. Denetim kapanmamış/görünür Cubit'i,
iptal edilmemiş aynı status isteğini ve halen güncel aynı admission'ı birlikte arar.
`_retire` mevcut status cancellation'ını iptal ettiği için repository aynı metadata
nesnesini sonraki admission'da geri verse de emekli isteğin sonucu uygulanamaz.

Aynı admission'da sayfa, boyut/DPR/fit veya kol değişikliği status sonucunu eskitemez.
Ret/exception mevcut `status_refused`, geçersiz veya değişmiş LIVE mevcut `live_changed`
yolunu kullanır. Geçerli aynı LIVE yanıtı güncel state'i değiştirmez. Medya epoch'u,
sabit LIVE anchor, P4b1 ortak close/drain ve P4b2 native rezervasyonu korunur.
Public API, DI, transport, scheduler, retry, timeout veya politika eklenmedi.

## İlk hata ve kabul matrisi

`04-initial-red` düzeltmeden önce yeni sayfa/status-ret testiyle çalıştı. Exit `1`;
assertion beklenen `status_refused` yerine `null` gördü. Derleme hatası değildir.
Ham başarısız kayıt korundu. İlk odaklı koşu `05-focused` 69 test geçti.

Son `21-matrix-final-serial` kaydı iki görev test dosyasını `--reporter expanded
--concurrency 1` ile çalıştırır; **69 test geçer**. Aşağıdaki 31 yeni testin adları
ham çıktıda tek tek görünür; mevcut regresyonlar aynı koşudadır.

| Kabul durumu | Test ve somut gözlem |
|---|---|
| Sayfa, geometri veya `changeChoice` sırasında aynı admission; ret/exception | `P4b3 aynı admission seçim={page,geometry,choice} status={failure,exception}`: status isteği seçimde iptal edilmez; `status_refused` güncel medyayı iptal eder, admission/raster temizlenir. Eski iş bitmeden sonraki gate işi başlamaz. |
| Aynı seçim matrisi; farklı LIVE veya geçersiz manifest | `status={live,invalid}`: `live_changed`; güncel iş iptal edilir, eski kabul tutulmaz. Geometri testi boyut, DPR ve fit'i birlikte değiştirir. |
| Aynı seçim matrisi; geçerli aynı LIVE | `status=success`: state/admission nesnesi ve seçili kol/sayfa korunur; güncel medya iptal edilmez. Manifest/byte/status sayıları `1/2/1` kalır. |
| Retire/readmission; eski success/ret/exception/LIVE | `P4b3 aynı nesneli readmission eski status={success,failure,exception,live} sonucunu reddeder`: ikinci admission özellikle aynı nesnedir; eski status cancellation'ı iptal edilmiştir. Eski sonuç yeni state'i, admission'ı veya medya işini değiştirmez. |
| Account/background/lease sonrası eski sonuç | `P4b3 {account,background,lease} sonrası eski status={success,failure,exception} state canlandırmaz`: önce sırasıyla `account_changed`, `background`, `lease_expired` nedeni doğrulanır; sonra eski sonuç state nesnesini değiştiremez. |
| Gerçek reader sayfa değişimi; ret/farklı LIVE | `P4b3 gerçek reader sayfa değişimi pending HTTP status={failure,live}`: loopback HTTP → data source → repository → Cubit → gerçek QA reader. Status yanıtı sunucuda beklerken UI'da `Sonraki` ile sayfa 2 açılır. Yeni native image yüklendikten sonra status serbest bırakılır; yeni image dispose olur, raster widget kaldırılır ve rezervasyon slot/byte `0/0` olur. Önceki sayfanın image'ı da dispose edilmiştir. |
| Aynı gerçek zincir; geçerli status | `...status=success`: gerçek HTTP status transport completion'ı beklenir. Sayfa 2'nin state/image/admission'ı korunur; ek manifest, byte veya native buffer başlangıcı yoktur. Her üç HTTP testinde sayılar manifest/status/byte `1/1/3` kalır. |
| Close ve gerçek sonlanma | Mevcut `ortak close status ve medyayı bekler` altı sonuç/sıra testi, yavaş status soketli gerçek parent close, `gerçek QA parent UI close native gate release bitmeden kapanışı tamamlamaz` ve `eski native Future kapanmadan yeni HTTP/decode yok; geç epoch boyanmaz` geçer. P4b2 rezervasyon/cleanup regresyonları korunur. |

Yeni seçim/status unit testleri pending medya Future'ının gerçek sonlanmasına kadar gate'in
kapalı kaldığını kanıtlar. Yeni HTTP testleri güncel resident native image temizliğini
kanıtlar. Bekleyen gerçek native Future'ın drain davranışı mevcut native regresyon testleriyle
ayrıca doğrulanır; bu üç gözlem tek bir eşzamanlı cihaz ölçümü olarak sunulmaz.

## Mutasyonlar ve son kontroller

Yalnız özel kopyada dört davranış mutasyonu çalıştı. Hepsi exit `1` ve beklenen
assertion hatasıyla kırıldı; derleme hatası yoktur. Her denemede runtime kaynağı
`finally` içinde geri alındı ve SHA-256 eşitliği doğrulandı.
`13-mutation-summary.private.json` mutasyon ve geri alma hash'lerini tutar.

| Mutasyon | Kırılan assertion |
|---|---|
| `09-mutation-render-epoch` | Normal sonucu tekrar render epoch'a bağlama: aynı admission sayfa/ret testinde beklenen `status_refused`, gerçek `null`. |
| `10-mutation-retired-request` | İstek cancellation guard'ını kaldırma: aynı nesneli readmission/ret testinde yeni state'in kimliği korunmaz; beklenen `true`, gerçek `false`. |
| `11-mutation-exception-epoch` | Exception kolunu render epoch'a bağlama: kol seçimi/exception testinde beklenen `status_refused`, gerçek `null`. |
| `12-mutation-exception-retired` | Exception kolunda admission/istek sahipliğini atlama: aynı nesneli readmission/exception testinde beklenen state kimliği `true`, gerçek `false`. |

Son kaynakta `flutter test --no-pub test/reading_images test/story_detail` **243 test**,
izlenen `test/` kümesinin tamamı **505 test** geçer. `flutter analyze --no-pub`
`No issues found!` ile biter. Matrisin 69 testi dar/tam sayılara eklenmez; tekrar
koşu sayıları toplanmaz. İlk analyzer `14-analyze-final` yalnız bir testte curly-brace
stil uyarısı verdi. Düzeltildi; son temiz sonuç `20-analyze-final` içindedir. İlk hata,
önceki koşular ve analyzer kaydı korunur. `git diff --check` temizdir.

## Kaynak, kopya ve koruma

Onaylı özel kök:
`/Volumes/MAC_EK/tellpal-qa-private/p4b3-status-admission-20261018-b0ef829/`.
SDK ve mevcut interpreter yolları `operational-tools.private.json` ile ek
`operational-tools-02.private.json` metadata'sından okundu. Mevcut paket cache'i
ve sentetik TLS interpreter'ı yeniden kullanıldı. SDK/paket/venv kurulmadı veya
indirilmedi. Gerçek servis credential/token/config dosyası alınmadı.

Önceki P4b2 kaynak/closure/fixture makbuzlarından dar kapanış çıkarıldı. Yalnız yeni
köke 220 izlenen kaynak/test/asset/paket dosyası ve mevcut üç paket metadata dosyası
kopyalandı. 49 izlenen `_test.dart` dosyasının tamamı mevcut; göreli
import/export/part hedeflerinde eksik yoktur. Seçili integration/probe kaynakları
import kapanışı içindir; live/cihaz integration testleri çalıştırılmadı.

`19-source-closure.private.json` kaynak eşitliğini ve hash'leri kaydeder. Tek istisna:
dirty `app/mobile/lib/main_image_experiment.dart` özel kopyada başlangıç HEAD
sürümüdür. Repo dosyası korunur. Untracked local-lossless kaynak/test/asset işleri
kopyalanmadı. Bu yüzden dirty/untracked ağacın tamamının test edildiği iddia edilmez.
Yeni private dizinler `0700`, log/makbuzlar `0600`; CREATE_NEW kullanıldı.

Başlangıç `02-before-implementation` ve son `22-protection-final` makbuzu 3277 korunan repo dosyası,
72 eski run, 192 P4a5, 296 P4b1 ve 281 P4b2 özel dosyasında sıfır fark doğrular.
Companion inceleme, `.memlog.md`, dirty giriş, local-lossless işleri ve eski başarısız
kayıtlar korunur. Frozen block SHA-256 değişmedi:
`36985e1fd1aa249aed1b5081b8f7f9629c0ac939b8a075c8ae21f6521bb6b3c5`.

| Son görev kaynağı | SHA-256 |
|---|---|
| `app/mobile/lib/features/reading_images/presentation/cubit/reading_image_comparison_cubit.dart` | `18d8bff8ac8aca7535e1fd9df84ff258ff0e6eb4a3a8b2fd52f56037b66c0444` |
| `app/mobile/test/reading_images/reading_image_comparison_cubit_test.dart` | `bab8f1c1cf7f9ae64e54165ec507882d1651af7c012ae13e1151a942ec6d81e7` |
| `app/mobile/test/reading_images/reading_image_consumer_qa_test.dart` | `4e2490a9807910b5f97509540f1ff81d94f3390e6e8cbcc14f6286c1d1b9fb54` |

## Ham kayıt kimlikleri

Komut, çalışma dizini, exit, süre ve log hash'i özel kökün `receipts/` alanındadır.
Ham çıktılar `logs/` altındadır. Kaynaklar geri alındı; aşağıdaki başarısız kayıtlar
silinmedi veya üzerlerine yazılmadı.

| Ham log | Exit | SHA-256 |
|---|---|---|
| `04-initial-red.private.log` | 1 | `0ed1dd279ea4cf6575962420fbd8d14dc162f320cb9db6d82b4bba514a66dcb9` |
| `15-narrow-final.private.log` | 0 | `b0bc79c5163b2d36fa3c61f6429ee777f00f997dfa321bdf68bb853897abed77` |
| `16-full-tracked-final.private.log` | 0 | `5f4138f8b27a6e50d02078669ceca74c74bfd20c71f95bc5fd797d9b8cdfd1da` |
| `20-analyze-final.private.log` | 0 | `6b98ff936ce9f981455500a0701d0fd4323f9731313f0d0140948297335e9543` |
| `21-matrix-final-serial.private.log` | 0 | `5e7d451ca9e7a5616b02f2dd960d1d4838a184bbd3c9b19669d928adf2abec6f` |
| `09-mutation-render-epoch.private.log` | 1 | `e1de20ac2b808aa8bfd7808410bb8db4ef0687ce31f971269f9c612687106adc` |
| `10-mutation-retired-request.private.log` | 1 | `7568b163e089f5ea24a5a8cbfa37b3c2ed459a99d8c61ea5383f5a58b006aa1b` |
| `11-mutation-exception-epoch.private.log` | 1 | `c748266784aa8973785c086b08bbbe230a8c3fe69907728129eccf5b431fce5d` |
| `12-mutation-exception-retired.private.log` | 1 | `36b21d8e8e9e1c3cf1150adb07f5536e103d6541d72bd11330207e5ee31b6237` |

## Bağımsız incelemeler

BMad'ın üç bağlamsız inceleme katmanı aynı oturum modelini devraldı. Her biri tam
tracked/untracked diff'i, ayrı görev diff'ini ve başlangıçtaki ilgisiz değişikliklerin
envanterini aldı. Kod, spec ve ham kayıtlar salt okunur incelendi. Kör inceleme,
sınır durumları incelemesi ve doğrulama boşluğu incelemesi eyleme dönük bulgu üretmedi.
`intent_gap/bad_spec/patch/defer` sayıları `0/0/0/0`; açık düzeltme yoktur.
`reviews/24-independent-reviews.private.json` sonuçları ve sınıflandırmayı korur.

Kök ajanın son denetimi kaynak/test hash'lerini, 31 yeni test adının gerçekten geçmiş
olmasını, tam/dar/serial/analyzer sonlarını, tüm eski dosyaları ve frozen block'u
yeniden doğrular. Son makbuz `receipts/25-root-completion.private.json` içindedir.
Commit kapsamı yalnız Cubit, iki test, spec, salt okunur inceleme ve bu rapordur.

## Açık kapılar

Genel P4b, üretim profili ve fiziksel Android/iPhone kalite/RSS/GPU/peak kabulü açıktır.
Push/PR, gerçek backend/DB/Firebase/storage/cihaz koşusu, upload, üretim işlemi veya
sayısal bütçe değişikliği yapılmadı. Polling aralığında anlık revoke garantisi eklenmedi.
