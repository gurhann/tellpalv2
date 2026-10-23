# Android emülatöründe yerel kayıpsız format karşılaştırması

Sonuç: **PASS**.

Ortam Android emülatörüdür. Fiziksel cihaz, üretim performansı ve insan kalite kabulü sağlanmadı.

Ayrı profile paketinde altı sabit durumun encoded byte/SHA, 822×1334 tam decode, tüm 3.289.644 RGB byte eşitliği, gerçek DPR ve 1:1 contain paint bilgileri ile altı screenshot doğrulandı.

| Durum | Asset okuma µs | SHA µs | Decode µs | RGB readback µs |
|---|---:|---:|---:|---:|
| 9778-png | 339 | 18672 | 82255 | 66879 |
| 9778-webp | 336 | 9514 | 37495 | 39137 |
| 9816-png | 319 | 15353 | 19987 | 42512 |
| 9816-webp | 414 | 10322 | 44470 | 43875 |
| 9818-png | 285 | 16169 | 20260 | 47101 |
| 9818-webp | 249 | 9239 | 39408 | 38515 |

Stop sonrası owned buffer/descriptor/codec/image sayaçları sıfırdır.

Decode süresine RGB readback veya screenshot dahil değildir. RGB readback, alpha tarama, RGB kopyası ve SHA doğrulamasını içerir. Plan her format ve asset için tek seri örnektir. Kabul edilen örnek sayısı JSON kaydında ayrıca verilir. Hız ve RSS farklarından üretim üstünlüğü çıkarılamaz. RSS anlık süreç örneğidir; peak/GPU değildir. Loaded RSS doğrulama yükünü içerir. Teorik RGBA görsel başına 4.386.192 byte'tır. Renderer gözlenmedi (`unobserved`).

HTTP transferi ölçülmedi; asset boyutları HTTP byte ölçümü değildir. Gerçek OS background testi NOT_RUN. Fiziksel Android ve gerçek iPhone kabulü, tam reader ve insan kalite kabulü ayrı kalır. P0/P4b ve üretim kapıları açıktır.
