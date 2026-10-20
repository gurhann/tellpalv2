# P4b5 başlangıç ve süreç çıkışı — yerel onarım

2026-10-20. Başlangıç commit’i `18d4cbafb084cf45d2052f2f68da8f2121751563`. Bu çalışma mevcut spec’in [dar onarım ekini](../../../../_bmad-output/implementation-artifacts/image-optimization/p4b5-manual-process-repair.md) uygular. Yalnız yerel kod, sentetik test ve rapor değişti.

Gerçek olayda hangi alt çağrının ayrıştığı ve baseline yokluğunun kesin nedeni **bilinmiyor**. Aşağıdaki kod kusurları testle düzeltildi; gerçek olayın bütün nedenlerinin çözüldüğü ileri sürülmez. Yeni gerçek deneme yapılmadı. Son diff’in üç bağımsız incelemesi üst ajana aittir.

## Düzeltilen sözleşmeler

`ManualAndroid.owner`, boş `pidof` sonucunda kayıtlı `/proc/<pid>/cmdline` ve `stat` okumalarını artık atlamaz. Paket komutu ile startTicks pozitif eşleşmelidir. Farklı keşif PID’si, yabancı komut, yeniden kullanılmış PID/startTicks, okunamayan veya zaman aşımına uğrayan kimlik kabul edilmez. Alt çağrılar aynı 15 saniyelik bütçenin kalan süresini kullanır; genel retry veya worker eklenmedi.

Cleanup üç durumu ayırır. Başarılı `/proc` listelemesinde kayıtlı PID bulunuyorsa pozitif paket/startTicks kimliği gerekir. Başarılı listelemede kayıtlı PID yoksa ve keşif de boşsa açık yokluk kanıtıdır. Listeleme/okuma hatası, bozuk çıktı veya keşif çelişkisi FAILED olur. Force-stop öncesinde ve sonrasında bu gerçek kontrol uygulanır. Sonrasında aynı süreç duruyorsa `QA_PROCESS_NOT_STOPPED`; okunamıyorsa FAILED yazılır. Açık eski-PID yokluğu olmadan `processExitConfirmed` üretilmez. Değişmiş/yabancı süreç durdurulmaz.

Python hata tanısı son sekiz sabit `operation`/`code` çiftiyle sınırlıdır. İzinli alt çağrılar pidof, cmdline, stat ve proc_list’tir. Cleanup ve CLI hata makbuzları yalnız filtrelenmiş bu alanları taşır. Yabancı cmdline, ham stderr/exception, token veya log içeriği eklenmez.

Normal Flutter main artık testlerin de kullandığı `bootstrapManualConsumerQa` yolunu çağırır. Gerçek get_it/injectable, QA coordinator ve özel protokol kullanılır. Yalnız support resolver, platform/süreç/izin girdisi, mount ve diagnostic hedefi testte değiştirilebilir. Anotasyon değişmedi; generator ve üretilen kaynaklar değiştirilmedi.

Bootstrap/protokol aşamaları mevcut debugPrint/logcat tanı yoluna en çok 16 sabit enum aşama/kod satırı verir. Resolver hatası coordinator kurulmadan önce gözlenir. Platform/proc/dizin/izin/baseline hatasında aşama kaybolmaz. `initialized=false` iken failure dosyası yazılmaz; sabit `failureMarker unavailable` tanısı görülür. Exception metni, stack, yol veya credential ürün tanısına alınmaz. Canonical yol, symlink, 0700/0600 kontrolleri ve platform filesDir izni korunur. Yeni kanal, socket, storage IPC veya katman kurulmadı.

## Son doğrulama sonuçları

| Kapı | Sonuç |
|---|---|
| Offline pub hazırlığı | Mevcut kilitli cache ile geçti; paket indirilmedi |
| Gerçek Flutter bootstrap paketi | 9/9 geçti; skip yok |
| `flutter test --no-pub test/reading_images` | 197/197 geçti; skip yok |
| `flutter analyze --no-pub` | No issues found |
| İlgili Python paketi | 25/25 geçti; skip yok |
| Tam `test_p4b5*.py` paketi | 69/69 geçti; skip yok |
| `git diff --check` | Temiz |
| Java | 698 Java kaynak girdisi değişmedi; testler tekrarlanmadı |

Gerçek `ManualAndroid` ve gerçek `ManualController.cleanup` testlerinde yalnız subprocess transport sentetiktir. Boş keşif + aynı yaşayan `/proc`, boş keşif + açık yokluk, farklı keşif PID’si, farklı ticks, yabancı komut, listeleme/cmdline/stat erişim hatası, keşif çelişkisi, timeout, force-stop sonrası yaşayan/okunamayan/yeniden kullanılmış süreç kapsanır. Sonuç makbuzu ve sınırlı tanı birlikte doğrulanır; fake owner yeni denetimi gizlemez.

Gerçek Flutter bootstrap → DI → coordinator → geçici özel protokol testinde configure/credential beklenmeden baseline oluşur. Medya Cubit sayısı, HTTP client sayısı, native buffer denemesi ve aktif/reserved kaynaklar sıfırdır. Gerçek entry wrapper/view bağı test edilir. Resolver, platform, proc okuma, stat ayrıştırma, izin, symlink, eksik dizin ve mount erken hata dalları sabit sırsız aşamalarla gözlenir. Güvenilmeyen yollarda dosya oluşmaz.

Regresyon mutasyonları ayrı kanıttır. Pozitif `/proc` sahiplik kontrolünün kaldırılması sekiz; boş keşfin doğrudan exit PASS yapılması 13 alt senaryoda reddedildi. Onarım kopyasında gerçek bootstrap `initialize` çağrısı kaldırılınca başarılı baseline testi `Expected waiting / Actual baseline` ile başarısız oldu. Mutasyon sonrası dosya geri yüklendi ve repo ile byte eşitliği doğrulandı. Mutant kaynak final kopyaya alınmadı.

İlk analyzer koşusundaki iki test süslü-parantez uyarısı giderildi; son tam Flutter ve analyzer kapıları yeniden geçti. İlk Python native entegrasyonu dış reporter JSON çerçevesini düz satır gibi okuduğu için reddedildi. Son koşuda aynı ham Flutter JSON’un üç `print.message` payload’ı değişmeden düz native loga çözüldü. Validator veya kanıt koşulları gevşetilmedi. İlk başarısız loglar korunur; aşağıdaki son loglar seçilir.

## Korunan kanıtlar ve yerel kopya

Yerel test kökü:
`/Volumes/MAC_EK/tellpal-qa-private/p4b5-manual-consumer-measurement-20261020-be80ce1/repairs/local-0001`.
Credential’sız test kopyası `source/`, çıktılar `logs/` altındadır. Mevcut Flutter/Python/cache kullanıldı. APK, classpath veya yeni source/run binding üretilmedi.

Source-0007’nin 1162 bağlı dosyası eski hash’leriyle eşleşir. Eski run-binding-0006 ve APK build-0012 hash’leri değişmedi. Eski executor, claim, terminal ve receipt dosyaları değiştirilmedi. Önceki borç ve bütçeler sabittir. Bu onarım yeni kurulum, app/backend başlangıcı, Firebase giriş, pencere, measurement, medya veya bulut işlemi açmadı. Testlerdeki geçici sentetik claim’ler gerçek deneme değildir.

Altı kaynak/test dosyası bütün son kapılardan sonra donduruldu. Repo ve onarım kopyası SHA’ları eşittir. `coordinator.freezed.dart` ve `injection.config.dart` eski hash’leriyle aynıdır. Kaynak/test manifesti yalnız yerel doğrulama kaydıdır; gerçek koşu bağı veya onayı değildir.

Manifest: `repairs/local-0001/source-test-manifest-0001.json`.
Manifest SHA-256: `6faba73121fe375c4d2519cc8aa45ef69238c28c0628ad3f9732ae8419335466`.

Üst ajan ek salt okunur Android protokol kontrolü yaptı. Mevcut emulator-5554 üzerinde gerçek `ManualAndroid.owned_process_state` eski kayıtlı owner için `absent` döndü; yeni `/proc` listeleme biçimi gerçek Android’de çalıştı. Bu kontrol gerçek yeniden ölçüm veya başlangıç değildir; süreç/start/stop/kurulum/network/claim açmadı. Kayıt `repairs/local-0001/readonly-android-proc-0001.private.json`, SHA-256 `d3542c82e79862e459f6ea1e8b4df3df76fc3c3d40efa490c11d87554641fb21`. Bu ek kayıt üst ajan tarafından üretildi.

## Kesin kaynak/test dosyaları ve SHA-256

| Dosya | SHA-256 |
|---|---|
| `app/mobile/test/reading_images/manual_consumer_bootstrap_test.dart` | `05f2bb4744c81e381b87bd7eee149dfbd2518e2d7f6d098f51e7a9754cd6dd58` |
| `app/mobile/test_driver/manual_consumer_qa/coordinator.dart` | `1cd675eae7f802a023688e3e7b3d7f1c77ff0adff035d08873ea7611a639f7ad` |
| `app/mobile/test_driver/manual_consumer_qa/private_protocol.dart` | `7fa6d01ec1f515cf3d0cbfc0c2bf8b8fca5d8060930a2f1b6ec9ed8b967bf6a5` |
| `app/mobile/test_driver/reading_consumer_manual_entry.dart` | `e2b2ae8e0bad4645f8418ae6801dc441fcf4c592fe88f313c61bf197bf1df58c` |
| `tools/image-optimization/p4b5_manual_consumer_qa.py` | `1f1ca6be7ad9d88b0f76502459783c036a824384d8f9bb10cc69419ca84177c9` |
| `tools/image-optimization/test_p4b5_manual_consumer_qa.py` | `6635232f4fca472ab5446e138599934ac508887bc00bd5de02b45cfc03894ec5` |

Yeni rapor: `be/docs/evidence/p4b5-manual-consumer-measurement/local-repair-2026-10-20.md`.
Ana spec’in nonfrozen onarım bağlamı ve eki üst ajan tarafından yönetildi. Önceden değişmiş `.memlog.md`, `main_image_experiment.dart` ve local_lossless dosyaları bu patch’e alınmadı.

## Son loglar ve SHA-256

Bütün yollar `repairs/local-0001/logs/` görelidir.

| Log | SHA-256 |
|---|---|
| `flutter-analyze-0002.log` | `ef3dbfce335ca33aa1c9dc9874530360b013bc82da3d5688ce2c5cebcabfc51f` |
| `flutter-bootstrap-0001.jsonl` | `9a1f5f3451b5ff27022a991a76c5c1a88b8efdf39bf806f69194db6b12aff826` |
| `flutter-bootstrap-mutation-0001.jsonl` | `d9acda5159ffd039d2801b40740ed8034e3102e8ca81d557fe54519347fa4a3a` |
| `flutter-native-messages-0001.log` | `80a3064828fb1e0447565bcf97c05913363091ca8d7984597703d94f223c4ad1` |
| `flutter-reading-images-0002.jsonl` | `c11a333b7175cb3c9f5da5924e8fc764adc918509b800ed97673b8c988fdf855` |
| `pub-offline-0001.log` | `8ba0e855cee8c17405af01041b2445376ba3b2be751f15a181262a86bd451c26` |
| `python-all-0001.log` | `0ce74e660a27b7ed6829ccd1857f13bea4dc3d31476141ca263b38d19265bc15` |
| `python-manual-0002.log` | `a0276ef03c7fb9d6d305e03cd53d5d512e9b00987dc4be1e4bb9e77bc61f5b59` |
| `python-mutations-0001.log` | `4a55ee48174020f8edd1477585589a9069db68009dc25111577adffcd20a29ee` |

## Son üç inceleme

`p4b5_manual_blind_final`, `p4b5_manual_edge_v2` ve `p4b5_manual_verification_v2` aynı son kaynak/test değişikliklerini inceledi. Uygulanabilir ek bulgu veya doğrulama boşluğu kalmadı. Üst ajan altı dosyanın repo/test kopyası hash'lerini, dokuz logu, 698 Java kaynağını, iki generator çıktısını ve eski source-0007'nin 1162 bağlı girdisini ayrıca doğruladı. Frozen blok ve görev dışı değişiklikler korundu.

Son incelemeler yalnız yerel manifestle bağlıdır: `repairs/local-0001/repair-reviews-0001.private.json`, SHA-256 `aa34b2a5be01f3fab40845f833b60e7ae8bbd8697f355457f79e3643e06fe027`. Bu kayıt gerçek koşu onayı veya ölçüm kabulü değildir.

Yerel kod/test onarımı tamamlandı. Gerçek tekrar ve cihaz üzerindeki kesin kök neden doğrulaması bu teslimin kapsamı dışındadır. Önceki tek deneme kapalı kalır. Yeni ölçüm için düzeltilmiş kaynaklardan yeni APK ve ayrı somut koşu planı gerekir; mevcut APK'da bu onarım yoktur. P4b5 kabulü açık kalır.
