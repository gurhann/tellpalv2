# Fiziksel iPhone yerel kayıpsız format karşılaştırması

Sonuç: **FAIL**.

Plan: her asset ve format için bir seri örnek, toplam altı durum. Kabul edilen runtime ölçümü: **0/6 durum**; kabul edilen screenshot: **0/6**. Planlanan örnek sayısı ölçümün tamamlandığını göstermez.

Fiziksel cihaz kabulü tamamlanmadı. Gerekçe: `DRIVER_FAILED`. Özel deneme ve hata kayıtları korundu.

Decode süresine RGB readback veya screenshot dahil değildir. RGB readback süresi native readback, alpha tarama, RGB kopyası ve SHA doğrulamasının toplamıdır. Plan her format için tek seri örnekle sınırlıdır; format hızı genellenemez. RSS değerleri anlık süreç örnekleridir; peak, GPU belleği veya phys_footprint değildir. Loaded RSS, RGB readback ve checksum sonrasıdır; doğrulama ek yükünü içerir. Saf decoder veya RGBA resident farkı çıkarılamaz. Teorik RGBA tutarı görsel başına 4.386.192 byte'tır. Renderer gözlenmedi (`unobserved`).

HTTP transferi ölçülmedi. Gerçek OS background testi yapılmadı. Tam genişlikte reader uygunluğu ve insan görsel kalite kabulü ayrıca değerlendirilmelidir. P0/P4b ve üretim kapıları açıktır.
