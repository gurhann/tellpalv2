# P4b5 elle ölçüm: ilk inceleme kararı

2026-10-20. İlk kaynak/APK/run bağı yerel testleri geçti; üç inceleme koşuyu açmak için yeterli bulmadı. Hiçbir gerçek aşama çalıştırılmadı. PASS makbuzu ve execution approval üretilmedi.

İncelenen run SHA: `055a15d2b098212237877e9dbd58fc393a1f449bc9e4ff01d81636bd88b40a6f`.
Reviewer’lar: `p4b5_manual_blind`, `p4b5_manual_edge`, `p4b5_manual_verification`.

| Bulgu | Sonuç / sınıf | Gereken düzeltme |
|---|---|---|
| Paint receipt’inden sonra kullanıcı screenshot alınmadan ilerleyebilir | high / bad_spec | Aynı epoch ve receipt’e bağlı capture onayı gelene kadar gezinmeyi kapat |
| ADB exec kanalının uzak exit sonucu ve shell quoting sözleşmesi eksik | high / bad_spec | Shell v2 üzerinden sabit, doğru quoted komut ve gerçek uzak sonuç doğrulaması |
| Gerçek widget/protokol ve yeni onay kapılarının test şekli yeterince açık değil | high / bad_spec | Teknik ekte gerçek sınır ve mutasyon testlerini zorunlu kıl |
| Platform filesDir için 0700 beklentisi baseline’ı reddediyor | high / patch | Platform üst dizinini değiştirme; QA alt dizinlerini özel tut |
| Process ölünce nonce sahibi credential temizliğine ulaşılamıyor | high / patch | Dosya temizliğini çalışan PID şartından ayır |
| Credential temizliği hata verirse sahipli süreç durdurulmuyor | high / patch | Temizlik adımlarını bağımsız ve sahiplik kontrollü çalıştır |
| Önceki CLI claim’i varken yeni aşamanın claim öncesi hatası terminal yazmayabilir | high / patch | Kalıcı claim/run bağıyla başarısız denemeyi kapat |
| Cleanup hata verip app failure yazsa da stopped receipt’i kabul edilebilir | high / patch | Başarılı cleanup sonrası stopped; host ayrıca failure’ı reddeder |
| Baseline ADB alt çağrıları kalan 30 saniyeye bağlı değil | high / patch | Kalan süreyi bütün alt çağrılara taşı; son sonucu deadline ile kontrol et |

Kör ve sınır incelemesindeki aynı iddia/aynı düzeltme bulguları birleştirildi. Alt sıradaki patch ve test boşlukları, bad_spec nedeniyle yeniden türetme sırasında birlikte giderilecek; ayrı olumlu kabul verilmedi. Kullanıcı amacı veya tavanlarda bir boşluk bulunmadı; yeni ürün kararı gerekmiyor.

BMad tur 1 gereği yalnız bu görevin kod/test değişiklikleri başlangıca alındı. KEEP talimatları ana spec ve teknik eke işlendi. İlk 1160 kaynak dosyası hash kontrolüyle MAC_EK’te `review-iteration-0001-source/` altında saklandı. İlk APK’lar, test logları, source/run/classpath/Python bağları ve önceki iki gerçek denemenin kanıtları korundu. Yeniden türetme ve yeni üç inceleme bekleniyor. Eski test sayıları yeni kodun kabulü değildir.
