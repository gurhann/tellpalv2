# Görsel altyapısı geliştirme kabulü ve mobil devir

2026-10-23. **Geliştirme kabulü geçti.** [Paket 3 spec](../../../../_bmad-output/implementation-artifacts/spec-image-package-3-acceptance.md)
kapsamındaki tek Spring → normal Flutter kabulü, iki yeni sentetik yerel kaynakla
çalıştı. Üretim arşivi optimize edilmiş sayılmaz. Story 1.1 `done`, 1.2 `review`,
1.3 `backlog` olarak kaldı; tracker yazılmadı.

## Tek çalışan kabul

`ReadingImageLibraryLoopbackIT.realSpringNormalLibraryAppAndPageDecodeThenReleaseOnDelayedDeparture`
gerçek PostgreSQL 15/Flyway, Spring bean’leri, admission, hazırlama worker’ı,
bağımsız stored reread, atomik tamamlanma ve güncel LIVE’ı kullanır. Yalnız
`firebaseAssetStorageClient` disk leaf’i ve Firebase token verifier sentetiktir.
Yeni JPEG/PNG kaynakları yerel disk leaf’ine yazılıp gerçek `MediaAsset` kaydı ve
source admission üzerinden hazırlanır; admin upload UI veya Firebase kabulü değildir.

Java testi tek, 180 saniye sınırlandırılmış Flutter child başlatır; child ve
polling sırasında gözlenen descendant handle’larının çıkışı doğrulanır. Cleanup
beklemesi toplam 5 saniyeyle sınırlıdır. Release dosyasına yazma hatası, `try/finally`
ve hata toplayan adımlar sayesinde kill/await/nihai doğrulamayı atlatmaz. Bu launcher
ani worker ölümü veya geç spawn için genel süreç ağacı garantisi vermez; P1 guardian
kanıtı kendi kapsamındadır. Flutter, gerçek `configureLibraryDependencies`
opt-in’i ve generated DI ile `LibraryApp` açar. Normal `StoryDetailRoute` metadata’sı
ve `ReadingImagePageRoute`, gerçek repository/manifest/HTTP/`CachedNetworkImage`
yolunu kullanır. Capability, content API, DTO veya source yanıtı testte değiştirilmez.
Pasif `BlocObserver` yalnız oluşan Cubit’leri gözler. Boş, test kapsamlı
`HttpOverrides`, binding’in HTTP400 override’ını kaldırır. Root opt-in’i çıkarılırsa
normal detail’de consumer oluşma/çizim beklentisi başarısız olur.

| Yeni kaynak | Normal detail | Bağımsız sayfa | HTTP/byte sonucu |
|---|---|---|---|
| JPEG, 800×600 | Gerçek engine 594×446 | Gerçek engine 400×300 | `image/jpeg`, 10.795 byte, kaynakla byte/SHA eşit |
| PNG, 800×600 | Gerçek engine 594×446 | Gerçek engine 400×300 | Yeni `image/webp`, 112 byte; PNG kaynak 3.803 byte |

Tam SHA, LIVE/generation/job/manifest kimliği, kullanılan her rendition ve decode
boyutu [summary.json](summary.json) içindedir. Normal data source gerçek HTTP MIME,
uzunluk ve `private,no-store` başlığını; cache gerçek MIME imzası/SHA/geometriyi
sınar. Test, promoted bellek dosyasının byte/SHA’sını backend çıktısıyla ve gerçek
`RawImage` boyutlarını seçilen decode geometrisiyle karşılaştırır.

İkinci frozen satırda yeni page route’u gerçek disk rendition okumasını beklerken
owner 1’dir. Route’tan ayrılınca çizim kaldırılır; gerçek istemci transport Future,
cache, decode ve provider/frame cleanup’ı ile Cubit kapanır, owner 0 olur. Sunucu
disk beklemesi bundan sonra açıkça bırakılır. Bu sonuç, istemci iptalinin sunucudaki
storage çağrısını anında sonlandırdığı iddiası değildir. Disk beklemesinin ayrı
8 saniye tavanı vardır. Flutter, owner 0/Cubit kapalı anında RUN’a ait
`delay-waiting` marker’ını ve bitiş kaydının yokluğunu doğrular; client cleanup
kaydından sonra release verir. Sunucu sonucu `RELEASED`, `timedOut=false` ve
`clientCleanupObserved=true` olarak hem Flutter hem Java tarafından sınanır.
Timeout başarı sayılamaz. Engine/cihaz tepe RAM veya performans ölçümü yapılmadı.

## K3.1–K3.5 karşılığı

| Madde | Kullanılan kanıt |
|---|---|
| K3.1 | [Paket 1](../image-package-1/README.md): 41 kaynak, 17 aynı JPEG/24 yeni WebP, tam RGB/metadata ve son sınıflarla bağımsız reread. Bu koşu: iki yeni yerel kaynak ve gerçek LIVE. |
| K3.2 | Paket 1’in P2/P3 atomik READY, retry/restart/stale/profil ve önceki LIVE koruma testleri; yeni koşunun gerçek Spring/PG hazırlama→LIVE zinciri. |
| K3.3 | [Paket 2](../image-package-2/verification.json): 603 çalışan backend, 654 tam Flutter ve son 36 hedefli test. Yeni koşu: Maven 1 ve iç Flutter 1, sıfır failure/error/skip; yeni test analyzer temiz. Önceki sonuçlar tekrar çalıştırılmış gibi sunulmaz. |
| K3.4 | Bu tek kabulün normal root/detail + bağımsız page çizimi ve gecikmeli ayrılma sonucu. Paket 2 incelemesinin V2 açığı burada kapanır; Paket 2 kanıtları salt okunur kaldı. |
| K3.5 | Bu kayıt, [mobil epic](../../../../_bmad-output/planning-artifacts/epics-mobile.md) ve [onaylı devir](../../../../_bmad-output/planning-artifacts/sprint-change-proposal-2026-10-22.md). Story 1.2 insan kabulü açık; 1.3 uygulanmış sayılmaz. |

## Sınırlar ve çalıştırma

Gate varsayılan kapalıdır; zorunlu kabul açıkça `-Dtellpal.package3.acceptance=true`
ve `-Dtest=ReadingImageLibraryLoopbackIT` ile `be/` altında `bash mvnw test` üzerinden
seçilir. Seçili kabulde eksik Flutter/native araç başarısızlıktır. Flutter standalone
testi yalnız Java child environment’iyle açılır; varsayılan skip PASS değildir.

Özel run kökü `/private/tmp/tellpal-package3-run-path`; final koşu `attempt-07/<runId>` altındadır. Java her çalışmada
`CREATE_DIRECTORY` ile yeni RUN dizini açar; önceki marker/result dosyaları
kullanılamaz veya yeniden yazılamaz. Caller kökündeki backend logu buna engel değildir.
Ham loglar, source/çıktı dosyaları ve geçici credential Git dışındadır. Credential
rastgele üretilir, yalnız bellek/environment’te taşınır. Docker/native/Flutter ortam
anahtarları ve tüm sayısal sınırlar JSON’da kayıtlıdır. Başarısız ilk beş deneme ve inceleme öncesi `attempt-06` PASS kaydı
ayrı tutulur; son yama koşusu olarak yeniden etiketlenmez.

P1/P2 kanıtları, 88 önceden kirli dosya, 41 orijinal, 124 P5c ve 7 P5f dosyasında
SHA değişikliği yoktur. Önceden kirli ana uygulama planı ve mobil tracker yazılmadı.
Yeni üretim kodu, codec/41 kaynak/telefon/tam verify koşusu yoktur. Firebase,
canlı DB/storage, deploy ve push yoktur. SDK mutlak storage deadline, AMD64 üretim
yeterliliği, gerçek audio/start/session/cache-retention, fiziksel cihaz ve üretim
etkinleştirmesi bağlantılı onaylı devirde açık kalır.
