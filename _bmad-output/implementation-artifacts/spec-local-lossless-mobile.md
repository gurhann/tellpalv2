---
title: 'Mevcut PNG ve kayıpsız WebP için fiziksel iPhone QA karşılaştırması'
type: 'chore'
created: '2026-10-18'
status: 'in-progress'
baseline_commit: '46fec42fab212483882ced989ce2a68cc418cef1'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/SKILL.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/references/architecture.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/references/bloc-patterns.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/references/dependency-injection.md'
  - '{project-root}/.codex/skills/flutter-scalable-app/references/performance.md'
  - '{project-root}/be/docs/adr/ADR-0018-flutter-skill-architecture-policy.md'
  - '{project-root}/be/docs/adr/ADR-0024-reading-image-comparison.md'
  - '{project-root}/be/docs/evidence/local-lossless-formats/verification.md'
---

<frozen-after-approval reason="Kullanıcının devam onayı; yalnız mevcut yerel dosyaların cihaz karşılaştırması">

## Intent

**Sorun:** Yerel WebP kazancı fiziksel cihazda decode/gösterim kanıtı taşımıyor.
**Yaklaşım:** Mevcut üç 822×1334 PNG/WebP çiftini, ayrı profile QA paketinde altı seri durumda göster. İlk fiziksel hedef erişilebilir iPhone 12'dir. Android emülatörü fiziksel Android sayılmaz.

## Boundaries & Constraints

**Daima:** Eski kanıt, #94, signing girdileri ve `.memlog.md` korunur. Kaynak/lock/fixture hash'leri ve gerçek cihaz/mode/bundle doğrulanır. Yalnız mevcut dosyalar; yeniden encode/resize yok. Flutter mimarisi ve normal akışlar korunur. Tek owned raster; seri decode; hata, iptal, background ve stop'ta kaynak bırakma. Türkçe çıktı.

**Önce sor:** Yeni bulut/DB/auth, dependency indirme, sertifika/provision oluşturma, üretim profili veya erişim politikası gerekirse. Yerel development signing metadata'sı kullanılabilir.

**Asla:** Mevcut authenticated kapıları gevşetme. Normal Runner, navigation/ses/cache veya backend değiştirme. Yeni storage/IPC/retry altyapısı ekleme. Debug/emülatör sonucunu fiziksel performans, RSS'yi peak/GPU veya görsel gösterimini insan kalite kabulü sayma. P0/P4b ve üretim kapıları açık kalır.

## I/O & Edge-Case Matrix

| Durum | Girdi | Beklenen | Hata |
|---|---|---|---|
| Geçerli | Sabit altı asset | Tam decode, RGB pin, paint ve ölçüm | PASS yalnız altısı tamamlanınca |
| Geçersiz | Bilinmeyen fixture/hash/format/boyut | Ret | Fallback ve PASS yok |
| Yanlış giriş | QA bayrağı kapalı veya profile dışı | Eski giriş / offline ret | Auth yolu değişmez |
| Yarış/kapanış | Paralel komut, eski iş, background/stop | Seri kapasite, geç sonuç gösterilmez | Son sayaçlar sıfır |
| Ortam yok | Fiziksel cihaz/signing/araç eksik | Açık NOT_RUN kaydı | Tamamlandı denmez |

</frozen-after-approval>

## Code Map

- `be/docs/evidence/local-lossless-formats/verification.json`: SHA `16adae98244f6821cdb15f81dcca4a7640c2075cbeaa2cbecfc34786a1e78100`; `images[]` mevcut altı path/byte/SHA ve RGB pinidir. Dosyayı değiştirme.
- `app/mobile/lib/features/reading_images/presentation/widgets/reading_image_comparison_raster.dart:25`: tam decode ve borrowing painter; değiştirmeden kullan. `ReadingImageComparisonState` mevcut Freezed QA durumudur.
- `app/mobile/lib/main_image_experiment.dart`: yalnız açık profile offline bayrağı için dar dispatch. Varsayılan akış aynı; normal `main.dart` değişmez.
- `tools/image-optimization/native_entry_driver.py:source_snapshot/private_mobile_snapshot`: özel snapshot reuse. `build_entry` auth/broker istediği için olduğu gibi çalıştırma.
- `app/mobile/ios/ImageExperiment/build_guard.py`: aynen korunur. Exact giriş, ayrı profile bundle, fiziksel iphoneos ve private output/signing şartları sürer.
- `app/mobile/test_driver/reading_comparison_entry.dart` debug'a kilitli; yeniden çalıştırma. FlutterDriver kontrol/ekran yakalama mekanizmasını kullan; medya HTTP'si yok.
- `ImageExperiment` native memory handler'ı admission ister; değiştirme. Dart `ProcessInfo.currentRss` varsa açık RSS anlık örneği kullan; yoksa unavailable kaydet.

## Tasks & Acceptance

- [x] `app/mobile/lib/main_image_experiment.dart`, `app/mobile/lib/app/local_lossless_qa*.dart`: opt-in profile QA kompozisyonu; mevcut Cubit/Freezed/get_it ve raster desenleri. Gerekirse ilgili reading_images QA alt dosyaları; annotation değişirse generator çalıştır.
- [x] `app/mobile/test_driver/local_lossless_driver.dart`: sonlu altı durum, paint sonrası ekranlar, ham ölçümler ve stop kontrolü.
- [x] `tools/image-optimization/local_lossless_mobile.py`: private snapshot'a altı değişmeyen asset/manifest; offline mevcut build/signing; bundle ve device kanıtı; normal checkout pubspec/asset listesi değişmez.
- [x] `app/mobile/test/reading_images/local_lossless_qa_test.dart`, `tools/image-optimization/test_local_lossless_mobile.py`: matris ve kaynak/kanıt koruması testleri.
- [ ] `be/docs/evidence/local-lossless-mobile/verification.*`: gerçek cihaz denemesi, ekranlar ve sınır raporu. Engelde başarısız kayıt korunur; kod doğrulaması cihaz kabulü sayılmaz.
- [x] `app/mobile/test_driver/local_lossless_emulator_entry.dart`, `tools/image-optimization/local_lossless_emulator.py`, `tools/image-optimization/test_local_lossless_emulator.py`: kullanıcının emülatör seçimi için aynı profile kapısını koruyan ayrı Android QA paketi; private snapshot, farklı applicationId, offline build ve exact APK/emülatör/nonce/VM bağı. Altı case ve stop için mevcut driver/doğrulayıcı yeniden kullanılır. Fiziksel kapılar gevşetilmez.
- [x] `be/docs/evidence/local-lossless-mobile/attempts/<yeni-id>/verification.*`: emülatör eki ayrı kayıt olur. `environment: android_emulator`, `physicalDevice:false`, `performanceAccepted:false`; altı case/screenshot ve kaynak temizliği kabulü ile fiziksel performans ayrılır. Eski raporlar ezilmez.
- [x] `tools/image-optimization/local_lossless_mobile.py`, `local_lossless_emulator.py` ve ilgili testler: kullanıcının seçtiği MAC_EK özel QA kökünde yeni emülatör çıktıları oluştur. Gerçek yol, sahiplik, 0700/0600 ve yeni dosya koşulları sürer. Build, driver ve geçici yazımları dış köke bağla; mevcut çevrimdışı cache'leri kullan. Native veya authenticated guard'ları değiştirme.
- [x] `tools/image-optimization/local_lossless_mobile.py`, `tools/image-optimization/test_local_lossless_mobile.py`: iPhone hazırlığında yeni özel önkontrol dosyasını ve zorunlu SHA256 eşini açık CLI girdisi olarak kabul et. Yalnız prepare aşamasında seç; path/SHA ve signing config bağını prepare makbuzunda koru. Eski varsayılan girdiyi gevşetme. Eksik eş, değişmiş girdi, yanlış izin veya sembolik bağ ret olur. İsteğe bağlı korunacak özel dizinler prepare öncesinde hash ile sabitlenir; eski raporlar değişmez.
- [x] `tools/image-optimization/local_lossless_mobile.py`, `tools/image-optimization/test_local_lossless_mobile.py`: son doğrulama raporu ve altı ekranın açıkça seçilen yeni MAC_EK hedefinde 0700/0600 ile saklanmasını destekle. Mevcut exclusive publisher ve redaksiyon yeniden kullanılır. Benzer adlı kök, checkout içi hedef ve symlink kaçağı ret olur. Yeni runtime katmanı veya IPC ekleme. Bu iPhone denemesinde repo hedefi seçilmez.
- [x] `tools/image-optimization/local_lossless_mobile.py`, `tools/image-optimization/test_local_lossless_mobile.py`: açık yeni önkontrolde Flutter cihaz kimliğini fiziksel `hardwareProperties.udid` ile eşleştir. Sabitlenmiş `flutter devices --machine --device-connection=attached --device-timeout=15` çıktısında aynı kimlik tam bir kez, desteklenen gerçek iOS cihazı olarak bulunmalı. Kaynak ve araç makbuzu SHA eşleri doğrulanır. CoreDevice UUID, eşleşmeyen/eksik/çoğul cihaz ve değiştirilmiş keşif kaydı prepare öncesinde reddedilir. Eski sabit varsayılan girdinin davranışı korunur; yeni runtime katmanı eklenmez.

**Kabul ölçütleri:**
- Given sabit fixture, when altı durum gösterilir, then gerçek format, okunan asset byte/SHA, 822×1334 tam decode, tüm 3.289.644 RGB byte'ın pin eşitliği ve tamamlanan paint kaydedilir.
- Given cihaz DPR'ı, when yerleşim kurulur, then 822×1334 fiziksel pikselde 1:1 contain gösterim ve gerçek widget ölçüleri kaydedilir. Alan yetmezse ret. Tam genişlikte reader uygunluğu ayrı değerlendirilir; sessiz upscale yok.
- Given ölçüm, when raporlanır, then asset okuma, checksum, decode ve RGB readback süreleri ayrılır; decode sayacına readback/screenshot girmez. RSS önce/loaded/stop anlık örnektir; teorik RGBA 4.386.192 byte ayrı yazılır. HTTP transferi ölçülmediği açık kalır.
- Given stop, when biter, then owned buffer/descriptor/codec/image sıfır; global/disk ImageCache kullanılmaz. Gerçek OS background testi yapılmadıysa açık yazılır.
- Given Android emülatör eki, when aynı altı fixture çalışır, then aynı byte/RGB/paint/geometri/stop denetimleri geçer ve ayrı emülatör sonucu yazılır. Yanlış cihaz/ABI/package/entry/nonce/VM veya eski kanıt FAIL olur. Profile kapısı, normal Runner kaynakları ve mevcut emülatör ekran ayarı değişmez; emülatör sonucu fiziksel Android veya üretim performansı sayılmaz.
- Given onaylı dış QA kökü, when yeni emülatör adımı çalışır, then yeni build, driver ve geçici dosyalar bu kökte kalır. Yanlış sahiplik, gevşek izin, sembolik bağ, checkout içi hedef veya çakışan dosya reddedilir. Eski `/private/tmp` kanıtları yalnız okunabilir; eski raporlar korunur. Küçük kaynak ve sonuç belgeleri depoda kalır.
- Given yeni iPhone önkontrolü ve hash eşi, when prepare/build/run yapılır, then aynı signing config ve önkontrol kimliği korunur; eksik veya değişmiş girdi byte okunmadan/araç çağrısından önce reddedilir. Gerçek launch ve son device doğrulaması hâlâ fiziksel iPhone 12 ve güncel `wired` şartını arar.
- Given bu iPhone denemesinin açık MAC_EK sonuç hedefi, when verify biter, then rapor ve ekranlar yalnız yeni özel hedefte oluşur. Eski sonuçlar ezilmez; fiziksel çalışma veya insan kalite kabulü eksikse açık kalır.
- Given açık yeni iPhone önkontrolü, when cihaz seçimi doğrulanır, then fiziksel UDID, Flutter'ın attached listesindeki tek desteklenen gerçek iOS cihazı ve `device.selector` eşittir. Yanlış kimlik türü, değiştirilmiş keşif byte'ları veya farklı komutun makbuzu kaynak kopyası/derleme/launch öncesinde reddedilir.

## Spec Change Log

Ölçüm öncesi runtime incelemesi: fiziksel cihaz sorgusu ile VM sonucunun aynı çalışmaya ait olduğu bağlanmalı. Private build için tek kullanımlık compile-time run nonce; runtime/driver/verify eşleşmesi ve exact device + exact prebuilt binary launch makbuzu zorunludur. Yanlış/eski bağ reddi test edilir. RGB readback alanı alpha taraması, RGB kopyası ve SHA/pin denetimini de kapsar; etiket buna göre yazılır. Renderer gözlenmediyse unobserved kalır. Decode/readback sınırı ve mevcut auth kapıları korunur.

## Verification

- İlgili Python testleri, Flutter reading_images testleri ve hedefli `flutter analyze` geçer; generated annotation değişirse build_runner.
- Fiziksel profile build + altı durum + ekran/stop kanıtı. Eski dosyalara hash denetimi; token/UDID/signing içeriği public kanıta girmez.
- Yeni public kayıt exclusive oluşur; eski başarı/hata kayıtları ezilmez. SDK/Pods yalnız offline cache; eksikte dur ve kaydet. Maven tekrar gerekmez.

## İlk iPhone denemesinin yürütme girdisi (tarihî)

Private preflight: `/private/tmp/tellpal-offline-reading-iphone-preflight-c8913e4241094a43a7bf080f866dc9a0.json`; SHA256 `fe02cb8638ff620fa23d1578fec991c4d66f9c66d1cf41124fd29d895c7f6faf`. İçindeki signing config yalnız okunur. Yeni config/output /private/tmp altında exclusive ve 0600/0700 olur. Cihaz selector veya signing içeriğini public rapora taşıma. Eski signing output/config hash ile korunur.

Kullanıcının “Devam edelim” onayı bu dar cihaz karşılaştırmasının yürütme yetkisidir. İlk yürütmede fiziksel Android yanıtı bekleniyordu; sonraki seçim aşağıdaki devam kaydındadır. iPhone erişim ve signing önkoşulları o deneme öncesinde geçmişti. Bu tarihî önkontrol dosyası temizleme sonrasında mevcut değildir; yeni fiziksel deneme öncesinde yeniden doğrulanmalıdır.

## 18 Ekim devam kaydı

Kullanıcı fiziksel Android yerine şimdilik emülatörü seçti. iPhone ve Mac'in kilidini açtı; sonrasında iPhone'u USB ile bağladı. Salt okunur ön kontrol USB bağlantısını `transportType:wired` olarak doğruladı. Bu seçimler yeni encode, bulut, auth veya üretim profili onayı değildir.

İlk iPhone girişimi 600025 ms, ikinci girişim 600052 ms sonunda zaman aşımına uğradı. İkisinde de 0/6 kayıtlı case ve 0 screenshot vardır. İkinci girişimde VM keşif uyarısı görüldü; kesin hata nedeni kanıtlanmadı. Görünür izin/hata penceresi olmadığı kullanıcı tarafından bildirildi. Native QA süreci kalmadı; bu, Dart stop/handle kabulü yerine geçmez. Eski başarısız raporlar ve özel makbuzlar korunur. İkinci kayıt: [deneme bağlamı](../../be/docs/evidence/local-lossless-mobile/attempts/20261018-iphone02-0c719c2a/attempt-context.md).

Emülatör eki aynı kullanıcı amacının ek ortam doğrulamasıdır. Yalnız test girişi ve platforma özgü yerel build/kanıt komutları eklenir; yeni runtime katmanı, native IPC, storage süreci veya retry sistemi yoktur. Mevcut altı dosya ve Dart QA/driver paylaşılır. Java/SDK/pub/Gradle cache eksikse online fallback yapılmaz. Mevcut emülatör API36 arm64-v8a; ekran override822×1828 ve density320 korunur. Gerçek widget/DPR yine çalışırken doğrulanır. Planlanan örnek sayısı ile kabul edilmiş ölçüm sayısı ayrı raporlanır.

Üçüncü iPhone girişimi USB isteğiyle hazırlandı. Ön kontrolde `wired`, run kaydında `localNetwork` görüldü. Bu girişim USB kanıtı sayılmadı. Yalnız tanımlanmış QA process group'u SIGINT ile erken kapatıldı; 132826 ms, timeout değildir. SDK exit0 döndürse de driver sonucu bulunmadığından FAIL kaldı. 0/6 case, 0 screenshot ve cleanup sonrası 0 native QA süreci vardır; Dart stop/handle ölçümü yoktur. [Üçüncü girişim bağlamı](../../be/docs/evidence/local-lossless-mobile/attempts/20261018-iphone03-usb-6b8d91f8/attempt-context.md) korunur.

Sonraki fiziksel QA girişimi için güncel `transportType:wired` başlatma önkoşuludur. `--device-connection=attached` ve `--no-publish-port` resmi SDK seçenekleri kullanılır. Bunlar CoreDevice route'unu zorlamaz; ağ bağlantısına düşen cihaz kabul edilmez. Global Xcode, ağ veya izin ayarı değiştirilmez. Bu sınır yalnız yerel QA komutuna aittir; ürünün medya erişim politikası değildir.

Her platformun build'i yalnız kendi sabit kaynak snapshot'ını kullanır. Ortak app, helper ve spec dondurulduğunda o build'in kapsamı dışında kalan Android Python araçlarının düzeltmesi ayrı ilerleyebilir. Gerçek emülatör ve USB iPhone ölçümleri sırayla yapılır. Cihaz kabulü eksikse bu spec tamamlandı gösterilmez.

## Dış disk devam kaydı

Kullanıcı yeni QA çıktılarını MAC_EK'te tutmayı seçti. Yeni özel kök `/Volumes/MAC_EK/tellpal-qa-private` gerçek dizin, kullanıcıya ait ve 0700'dür. Mevcut Gradle ve Pub cache'leri zaten MAC_EK'e bağlıdır; yeni indirme yapılmaz. Yeni ham çıktılar ve geçici dosyalar dış diske gider. Bu seçim eski dosyaları silme veya taşıma yetkisi değildir.

İlk dış disk denemesinde Gradle build geçti; paket doğrulaması ölçümü başlatmadı. Cache'teki profile motorunun sembollü `libflutter.so` girdisi 155.038.656 byte olup doğrulayıcının genel 128 MiB okuma sınırını aşıyordu. APK ayrıca üç ABI içeriyordu. Yalnız özel QA init script'inde profile buildType filtresi ARM64'e sabitlenir ve bu motor girdisinin debug sembolleri paketlemede korunur. Böylece upstream profile motoruyla tam byte/SHA eşitliği aranır. Yalnız bilinen motor girdisinin okuma sınırı 256 MiB olur; diğer sınırlar korunur. Bu paketleme, üretim APK boyutu veya standart üretim bellek/performans kanıtı değildir. Görsel fixture'lar değişmez; fiziksel performans ve insan kalite kabulü kapalı kalır.

İkinci emülatör denemesinde prepare ve offline pub get geçti; `compileFlutterBuildProfile` başarısız oldu. APK ve driver sonucu yoktur; 0 case ve 0 screenshot vardır. Ana disk doluluğu hata makbuzu ve public rapor yazımını da engelledi (`Errno 28`). Derleme hatasının kesin kök nedeni kanıtlanmadı. Kullanıcı alan açarken önceki `/private/tmp/tellpal-local-lossless-*` klasörlerini temizlediğini doğruladı. Bu nedenle eski ham kayıtların korunduğu veya yeniden doğrulanabildiği iddia edilmez. Depodaki raporlar ve MAC_EK'e hash ile kopyalanan 30 görev kaynak/rapor dosyası korunur. Önceki emülatör denemesinin durumu ayrı bir kurtarma makbuzunda açıklanır; bu, temizlenen ham kayıtların yerine geçmez. Yeni deneme yalnız kalan sabit girdilerden yeni ve ayrı bir kanıt üretir.

## Emülatör ölçümünün sonucu

`20261018-emulator04-external-b8c4867e` için ayrı ARM64 profile paketi, tek driver çağrısı ve son doğrulama geçti. [Sonuç raporu](../../be/docs/evidence/local-lossless-mobile/attempts/20261018-emulator04-external-b8c4867e/verification.md) ve JSON kaydı, 6/6 durum ile altı ekranı içerir. Tam RGB pin eşitliği ve DPR 2'de 822×1334 fiziksel pikselde 1:1 contain gösterim doğrulandı. Stop sonrası dört aktif kaynak sayacı sıfırdır. Driver süresi 53.941 ms'dir. Her asset/format için tek sıralı örnek vardır; soğuk başlangıç etkisi ayrıştırılmadı. Hız veya bellek üstünlüğü çıkarılmaz.

Kullanıcı altı ekran ve iki raporun belirtilen yerel depo klasörüne kopyalanmasını açıkça onayladı. Toplam 9.196.359 byte kopyalandı; altı PNG özel asıllarıyla birebir eşleşti. Son doğrulamadaki 4.345 korunan girdi değişmedi; önceki 12 rapor ve ilgisiz `.memlog.md` ayrıca kontrol edildi. İlk kopyalama komutu build'in Java ortamını taşımadığı için reddedildi. Bu işlemde oluşan iki hata raporu hash ile MAC_EK'te arşivlendi; yalnız o geçici depo çıktısı geri alındı. Aynı build araçları sabitlenince son doğrulama geçti. Yeni build veya ölçüm başlatılmadı.

Ölçümün kaynak SHA256 değeri `5a646c791d18744e70691e7274eca0465a65250e56685cf6b2367b1d93269f3b` olarak korunur. Ölçümdeki 365 kaynak dosyasının tam kopyası MAC_EK'teki deneme kökünde `measurement-source/` altındadır. Bu takip kutuları ve devam kaydı ölçüm sonrasında güncellenmiştir; ölçümün kaynak sürümü bu son belge sürümüyle karıştırılmaz. Sonuç JSON SHA256 değeri `aff1be6ac23e6a39170123892438646db469219016805a79b326871c184de462`'dir.

Bu spec `in-progress` kalır. Fiziksel iPhone/Android doğrulaması ve insan kalite kabulü tamamlanmadı. Medya HTTP aktarımı ile gerçek OS background testi ölçülmedi. Tam reader, P0, P4b ve üretim kapıları açıktır. BMad inceleme ve kapanış adımına geçilmedi.

## iPhone 12 için yeni yerel hazırlık

Kullanıcı iPhone 12 ile devam etmeyi, önce bağlantı/yerel hazırlık kontrollerini ve mevcut altı dosyayı kullanmayı onayladı. Yeni QA çıktıları MAC_EK'te kalır. Yeni üretim, bulut işi, auth, sertifika/profil oluşturma veya dependency indirme yetkisi verilmedi.

Yeni özel önkontrol kökü `/Volumes/MAC_EK/tellpal-qa-private/iphone12-preflight-20261018-f245f23e`'dir. `signing-preflight.private.json` SHA256 değeri `cdafaeab69d4a4033bd41bf8dd7f7447ed15f0d9e260ae36c682b7a30792d9bb`'dir. Cihaz kimliği, mevcut sertifika/profil eşleşmeleri ve signing config burada özeldir. `operational-tools.private.json` mevcut Flutter/Xcode yollarını içerir. Kaynakların uyarlama öncesi 363 dosyalık kopyası `source-before-adaptation/` altındadır; eski emülatör kanıtları ayrı ve değişmez kalır.

Salt okunur kontroller fiziksel iPhone 12, iOS 26.7, eşleşmiş USB (`wired`), Developer Mode açık, cihazda passcode gerekmiyor ve Mac kilidi açık sonuçlarını verdi. Mevcut uygulama ve UITest runner provisioning profilleri cihazı kapsıyor, 2027-10-11'e kadar geçerli ve aynı yerel geliştirme kimliğiyle eşleşiyor. Yeni sertifika/profil oluşturulmadı. Geliştirici disk metadatası okunabildi; bu, gerçek debugger/VM bağlantısının çalıştığını kanıtlamaz.

Mevcut altı görselin byte/SHA/header kontrolleri geçti. iOS profile engine cache'te var. CocoaPods bağımlılıkları mevcut Flutter/plugin path kaynaklarından gelir; prepare/build yalnız çevrimdışı çalışır. Eski checkout Pods Manifest.lock, mevcut Podfile.lock ile aynı değildir; checkout'a dokunma. Yeni private snapshot'ta `pod install --deployment --no-repo-update` mevcut SDK/pub cache ile çalışır; dış ağ kapalıdır. Eksik girdi halinde hata korunur, indirme açılmaz.

Uygulama/native/auth guard'ları değişmez. Hazırlık girdisinin ve özel sonuç hedefinin dar CLI uyarlaması ile ilgili negatif testler tamamlanınca yeni unique MAC_EK run kökü oluşturulur. Aynı altı asset private snapshot'a kopyalanır; 822×1334 profile paketinin kimliği ve kaynağı doğrulanır. Yeni build ve tek sonlu driver çağrısı sırayla yürütülür. Her iki aşamanın önünde araç yolları/ortamı sabitlenir; launch öncesi cihazın kilit/USB durumu yeniden okunur. Yalnız tanımlanmış QA süreci temizlenebilir; diğer backend/frontend/cihaz süreçleri durdurulmaz. Gerçek 6/6 ve stop kanıtı olmadan fiziksel sonuç PASS sayılmaz. Sonuç ve ekranlar aynı run'ın yeni özel `report/` dizininde kalır; repo ihracatı bu devam kapsamına girmez.

## iPhone cihaz kimliği düzeltmesi

`iphone04-external-9b134c3a` için 76 Python testi, 97 Flutter testi, hedefli analiz, çevrimdışı profile build ve imza denetimi geçti. Ancak hazırlık girdisinde Flutter selector'ı olarak CoreDevice UUID seçilmişti. Flutter aynı fiziksel telefonu hardware UDID ile listeledi; 6.129 ms içinde yanlış kimliği reddetti. Uygulama açılması veya VM bağlantısı kanıtlanmadı: 0/6 durum, 0 ekran. Son cihaz sorgusu `wired`, QA native süreç sayısı sıfırdır; Dart stop ölçülmedi. Bu seçim hatası kök hazırlık adımında yapıldı. Cihaz veya görsel başarısızlığı sonucu çıkarılmaz.

Başarısız denemenin özel JSON raporu SHA256 `edad2894f752ad42a40daf749bb12b173c3014e75455b20b1efc56be048011db` ile MAC_EK'te korunur. Eski önkontrol ve kaynak arşivi değişmez. Dar devam, cihaz kimliğini doğrulayıp aynı yerel kapsamı sürdürür; yeni görsel veya bulut işi yapılmaz.

Yeni özel önkontrol `iphone12-selector-check-20261018-0bc27f49/signing-preflight.private.json` dosyasıdır; onaylı MAC_EK QA köküne göredir. SHA256 `c66c225f51fafccedebcdf72631f87c70882583cdaa5245767149cd1a394e592`'dir. Aynı dizindeki `operational-tools.private.json` araçları ve bu girdiyi sabitler. Yeni `flutterDiscovery` alanı özel attached listesini, liste SHA'sını, araç makbuzunu/SHA'sını ve mevcut Flutter çalıştırıcısını içerir. `device.hardwareEvidencePath` ve SHA eşi önceki gerçek fiziksel cihaz kaydını gösterir; `hardwareUdid` ve `selector` bu kayıttaki UDID'dir. Özel kimlikler public belgeye yazılmaz. Attached keşfinde tam bir desteklenen gerçek iOS eşleşmesi doğrulandı; bu henüz uygulama/VM bağlantısı değildir.

Seçim koruması ve anlamlı negatif testler geçince, önceki iPhone başarısız run'ı ve yeni önkontrol dahil mevcut kanıtlar hash ile korunur. Yeni unique MAC_EK run kökünde ayrı prepare/build ve tek sonlu driver çağrısı yapılır. İki deneme birleştirilmez; başarılıymış gibi eski makbuz düzenlenmez. Mevcut altı dosya ve aynı native/auth guard'ları kullanılır. Önceki 97 mobil test ve analiz izi korunur; mobil/native/test kaynaklarının birebir aynı kaldığı doğrulanırsa sırf Python seçici düzeltmesi için bu testler tekrar edilmez. Yeni Python testleri çalışır. Yeni ölçüm öncesinde wired/kilit kapısı tekrar okunur. Başarısızlıkta rapor korunur; otomatik tekrar döngüsü açılmaz. Yeni sonuçlar yalnız kendi özel `report/` dizininde kalır.

## Doğru UDID ile denemenin ortam engeli

Yeni seçim koruması ve 83 Python testi geçti. `iphone05-udid-27c64b0e` prepare kaynağı SHA256 `c190f73369ac3082dcd99460721f5d470c23a1b8df19fe80cddb64ca13211f11` ile sabittir; 363 dosyanın tam kopyası bu özel run'ın `measurement-source/` dizinindedir. Önceki run ile aynı kalan 349 mobil/native/test kaynağı, 97 geçen Flutter testi ve hedefli analiz izinin yeniden kullanımını doğrular. Matrisin kapı, geçersiz fixture, geç sonuç/background/stop/close ve ortam yok satırları bu testlerle örtülür; Python keşfinde tam 83 test bulunur, çalışma çıktısı 83/OK ve skip yoktur.

Çevrimdışı Xcode profile build 56.799 ms'de geçti; strict codesign denetimi başarılıdır. Güncel cihaz ve kilit kapısı fiziksel iPhone 12, doğru hardware UDID, `wired`, Developer Mode açık, passcode gerekmiyor ve Mac kilidi açık sonucunu verdi. Tek SDK çağrısı telefonu buldu ve kurulum/başlatma adımına geçti; 34.102 ms sonunda Mac'teki `iproxy` çalıştırılamadığı için başarısız oldu. SDK'nin "Installing and launching" çıktısı Dart uygulamasının veya VM bağlantısının çalıştığını kanıtlamaz. Sonuç 0/6 durum ve 0 ekrandır; Dart stop/RSS ölçümü yoktur.

Salt okunur tanı, arm64 Mac üzerinde SDK `iproxy` dosyasının yalnız x86_64 olduğunu, 0755/exec izninin bulunduğunu ve hatanın `incorrect architecture` olduğunu doğruladı. Rosetta çalışma yolları ve paket kaydı bulunamadı; PATH üzerinde hazır native `iproxy` yoktur. Yerel SDK kaynak kodu Rosetta önerisini mimari uyuşmazlığı errno 86 için üretir. macOS 27.0.1'de Apple'ın desteklediği çözüm Rosetta kurulumudur; [Apple açıklaması](https://support.apple.com/tr-tr/102527) ve yerel `softwareupdate --help` ile doğrulandı. Kurulum internetten yeni sistem yazılımı indirir ve lisans kabulü gerektirir; henüz kurulum yetkisi verilmedi.

Özel `report/verification.json` SHA256 `8f1ab374a0ffa476aea25532512fd426209c6cc843b016a8a2ecb0f0aa5a01ef` ile korunur. Dizin 0700, rapor dosyaları 0600'dür. Özel deneme bağlamı, mimari tanı ve native cleanup makbuzları aynı run'dadır. 235 sabit dosya ve 6.721 eski ağaç girdisi değişmedi. Bu run'ın `.profile` bundle'ına ait native süreç kalmadı. Ayrı `.release` bundle'ına ait mevcut süreç korundu; hiçbir terminate komutu çalıştırılmadı.

Yeni görsel üretimi, bulut/auth işlemi veya repo'ya sonuç ihracatı yapılmadı. Kurulum/yeni deneme başlamadı. Spec `in-progress` kalır; gerçek iPhone 6/6, ekran ve kaynak bırakma kabulü tamamlanmadı. İnsan kalite kabulü, HTTP aktarımı, OS background, tam reader, P0/P4b ve üretim kapıları açıktır. BMad step03 sürer; inceleme/kapanışa geçilmez. Rosetta için açık onay gelirse önce yalnız kurulum ve mevcut `iproxy --help` çalıştırma kontrolü yapılır; ardından yeni ve ayrı sınırlı deneme planı kaynakta kaydedilir. Onay olmadan yeni dependency indirilmez veya yeni cihaz çağrısı açılmaz.

## Fiziksel ölçümün ertelenmesi

2026-10-18 tarihinde kullanıcı, Rosetta kurmadan iPhone ölçümünü açık eksik olarak erteleyip P4a çalışmasına devam etmeyi kabul etti. Bu spec tamamlanmış sayılmaz; yeni kurulum veya cihaz çağrısı açılmaz. MAC_EK'teki iki başarısız iPhone denemesi ve önceki emülatör kanıtı korunur. Mevcut üç çiftin tam RGB eşitliği ve yerel WebP byte kazancı geliştirme girdisidir; fiziksel iPhone, üretim performansı veya genel codec/profil kabulü değildir.

P4a1 domain seçici/doğrulayıcı ve P4a2 karşılaştırma bağlantısı zaten uygulanmıştır. Sonraki dilim bunları tekrar etmeden normal tüketici bağlantısındaki gerçek eksikten seçilir. Normal erişim, medya teslimi, cache/session/retention ve üretim profili kapıları bu erteleme kararıyla açılmaz.
