# Gereksinimler Dokümanı

## Giriş

TellPal v2, çocuklar için çok dilli hikayeler, meditasyonlar, ninniler ve ses içerikleri sunan bir içerik platformunun tam backend yeniden tasarımıdır. Sistem, offline-first event takibi, CMS ile yönetilen içerik yayınlama, analitik için hazır veri toplama ve RevenueCat entegrasyonu ile abonelik yönetimini destekler. Backend, veri kalıcılığı için PostgreSQL ve medya varlıkları için Firebase Storage kullanır.

## Sözlük

- **Backend_Sistemi**: TellPal v2 sunucu tarafı uygulaması ve veritabanı
- **İçerik_Yönetim_Sistemi**: İçerik, kategori ve yayınlamayı yönetmek için admin arayüzü
- **Mobil_İstemci**: Backend API'lerini kullanan iOS/Android uygulaması
- **Firebase_Auth**: Uygulama kullanıcı kimliği için Firebase Authentication servisi
- **Firebase_Storage**: Medya dosya barındırma için Firebase Cloud Storage servisi
- **RevenueCat**: Üçüncü taraf abonelik yönetimi ve webhook sağlayıcısı
- **Admin_Kullanıcı**: İçerik Yönetim Sistemine erişimi olan kullanıcı (uygulama kullanıcılarından ayrı)
- **Uygulama_Kullanıcısı**: Mobil uygulamanın son kullanıcısı
- **Kullanıcı_Profili**: Bir uygulama kullanıcı hesabı içindeki profil (gelecekte çoklu profil özelliğini destekler)
- **İçerik**: Bir medya içeriği parçası (hikaye, meditasyon, ninni veya sesli hikaye)
- **İçerik_Yerelleştirmesi**: Çevrilmiş metin ve yerelleştirilmiş medya ile içeriğin dile özgü versiyonu
- **Kategori**: Organizasyon ve keşif için içerik öğelerinin gruplandırılması
- **Kategori_Yerelleştirmesi**: Çevrilmiş ad ve açıklama ile kategorinin dile özgü versiyonu
- **İçerik_Eventi**: İçerikle kullanıcı etkileşim eventi (START, EXIT, COMPLETE)
- **Uygulama_Eventi**: Uygulama içindeki kullanıcı etkileşim eventi (paywall, onboarding, vb.)
- **Satın_Alma_Eventi**: RevenueCat'ten gelen abonelik veya satın alma işlem eventi
- **Medya_Varlığı**: Firebase Storage'da saklanan bir dosyaya referans (görsel veya ses)
- **Dil_Kodu**: ISO dil kodu (tr, en, es, pt, de)
- **Event_ID**: İdempotent event gönderimi için istemci tarafından üretilen UUID
- **Harici_Anahtar**: CMS ve deep link'lerde kullanılan içerik için dil bağımsız, kararlı tanımlayıcı
- **Yayın_Durumu**: Yerelleştirilmiş içeriğin yayın durumu (DRAFT, PUBLISHED, ARCHIVED)
- **Ücretsiz_Erişim_Anahtarı**: Firebase Remote Config A/B testi için kullanılan string anahtar; hangi içeriklerin ücretsiz olduğunu belirler
- **Varsayılan_Erişim_Seti**: `default` anahtarıyla tanımlanan, A/B test anahtarı gönderilmediğinde uygulanan ücretsiz içerik seti

## Geliştirme Fazları

Proje aşağıdaki fazlarda geliştirilecektir. Her faz, bir önceki fazın üzerine inşa edilir ve bağımsız olarak test edilebilir çıktılar üretir.

### Faz 1: CMS Backend (Veritabanı ve API)
Bu fazda CMS için gerekli tüm backend altyapısı oluşturulacak:
- PostgreSQL veritabanı şeması oluşturma (tüm tablolar)
- Admin kimlik doğrulama API'leri (login, logout, token yenileme)
- Admin kullanıcı ve rol yönetimi API'leri
- İçerik yönetimi CRUD API'leri (içerik, yerelleştirme, sayfa yönetimi)
- Kategori yönetimi CRUD API'leri (kategori, yerelleştirme, içerik ilişkilendirme)
- Katkıda bulunan (contributor) yönetimi API'leri
- Medya varlığı yönetimi API'leri (Firebase Storage referansları)
- Dil seed verisi yükleme

**Çıktı:** CMS için kullanıma hazır REST API'leri

**İlgili Gereksinimler:** 1, 2, 3, 4, 5, 13, 14, 15, 17, 19, 20, 21, 22, 23

### Faz 2: CMS Ön Yüz
Bu fazda admin paneli kullanıcı arayüzü geliştirilecek:
- Admin panel web uygulaması (React/Vue/Angular)
- Admin login/logout ekranları
- İçerik yönetim ekranları (oluşturma, düzenleme, silme)
- Çok dilli içerik düzenleme arayüzü
- Kategori yönetim ekranları
- Kategori-içerik ilişkilendirme ve sıralama arayüzü
- Medya yükleme ve yönetim arayüzü
- Katkıda bulunan yönetim ekranları

**Çıktı:** Faz 1'deki API'leri kullanan tam fonksiyonel admin paneli

**Backend Gereksinimleri:** Yok (sadece frontend geliştirmesi)

**Not:** Bu faz sadece CMS ön yüz geliştirmesi içerir. Backend'de değişiklik yapılmaz, Faz 1'de oluşturulan API'ler kullanılır.

### Faz 3: Mobil Uygulama Entegrasyonu
Bu fazda mobil uygulama için backend API'leri geliştirilecek ve mobil uygulama baştan yazılmaya başlanacak:
- Mobil uygulama için public API endpoint'leri
- Firebase Auth entegrasyonu (kullanıcı kaydı, giriş)
- Kullanıcı profil yönetimi API'leri
- İçerik listeleme API'leri (dil bazlı, publish filtreli)
- Kategori listeleme API'leri (dil bazlı, publish filtreli)
- İçerik detay API'leri (sayfa bazlı, yerelleştirilmiş)
- Firebase verilerinin PostgreSQL'e taşınması (migration script'leri)

**Çıktı:** Mobil uygulamanın kullanacağı API'ler ve yeni mobil uygulama geliştirmesinin başlaması

**İlgili Gereksinimler:** 6, 18

### Faz 4: RevenueCat Entegrasyonu
Bu fazda abonelik ve satın alma yönetimi eklenecek:
- Abonelik ürün kataloğu yönetimi API'leri
- RevenueCat webhook endpoint'leri
- Satın alma event kayıt sistemi
- Webhook signature doğrulama
- Satın alma event doğrulama (lookup tablolar)

**Çıktı:** RevenueCat ile entegre abonelik yönetim sistemi

**İlgili Gereksinimler:** 9, 10, 12

### Faz 5: Analitik ve Event Tracking
Bu fazda kullanıcı davranış takibi ve analitik altyapısı eklenecek:
- İçerik event takibi API'leri (START, EXIT, COMPLETE)
- Uygulama event takibi API'leri (APP_OPENED, PAYWALL_SHOWN, vb.)
- Offline-first event queue sistemi (idempotent insert)
- Satın alma atıflandırma sistemi (attribution window)
- Analitik sorgu endpoint'leri
- Performans optimizasyonları (indexler)
- Dashboard ve raporlama API'leri

**Çıktı:** Tam fonksiyonel analitik ve event tracking sistemi

**İlgili Gereksinimler:** 7, 8, 11, 16

---

## Gereksinimler

Gereksinimler geliştirme fazlarına göre gruplandırılmıştır.

---

## Faz 1: CMS Backend Gereksinimleri


### Gereksinim 1: Çok Dilli İçerik Desteği

**Kullanıcı Hikayesi:** Bir içerik yöneticisi olarak, farklı bölgelerdeki kullanıcıların yerelleştirilmiş içeriğe erişebilmesi için içeriği birden fazla dilde bağımsız olarak yayınlamak istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi tr, en, es, pt ve de kodlarına sahip beş dili desteklemeli
2. İçerik oluşturulduğunda, Backend_Sistemi desteklenen her dil için ayrı yerelleştirmelere izin vermeli
3. Bir içerik yerelleştirmesinin durumu PUBLISHED olduğunda, Backend_Sistemi o içeriği ilgili dil için API yanıtlarına dahil etmeli
4. Bir içerik yerelleştirmesinin durumu DRAFT veya ARCHIVED olduğunda, Backend_Sistemi o içeriği ilgili dil için API yanıtlarından hariç tutmalı
5. Bir içeriğin talep edilen dilde yerelleştirmesi yoksa, Backend_Sistemi o içeriği o dil için API yanıtlarından hariç tutmalı
6. Backend_Sistemi her içerik yerelleştirmesi için dile özgü başlık, açıklama, kapak görseli ve ses dosyası referanslarını saklamalı
7. Backend_Sistemi içerik yerelleştirmeleri için dile özgü seslendirmen bilgilerini saklamalı


### Gereksinim 2: İçerik Tipi Yönetimi

**Kullanıcı Hikayesi:** Bir içerik yöneticisi olarak, her içerik tipinin mobil uygulamada düzgün şekilde render edilebilmesi için farklı içerik tiplerini uygun yapılarla yönetmek istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi dört içerik tipini desteklemeli: STORY, AUDIO_STORY, MEDITATION ve LULLABY
2. İçerik tipi STORY olduğunda, Backend_Sistemi 1'den başlayan sayfa numaralarıyla sayfalanmış içeriği saklamalı
3. İçerik tipi STORY olduğunda, Backend_Sistemi her sayfa için illüstrasyon medya referanslarını saklamalı
4. İçerik tipi STORY olduğunda, Backend_Sistemi her sayfa için dile özgü metin ve ses saklamalı
5. İçerik tipi STORY olduğunda, Backend_Sistemi sayfa sayısını içerik kaydında (page_count alanı) saklamalı ve sayfa eklenip çıkarıldığında otomatik güncellemeli
6. İçerik tipi MEDITATION veya AUDIO_STORY olduğunda, Backend_Sistemi içerik yerelleştirmesinde body_text alanında içerik metnini saklamalı
7. İçerik tipi LULLABY olduğunda, Backend_Sistemi body_text alanını isteğe bağlı olarak saklamalı (gelecekte sözlü müzikler için)
8. İçerik tipi MEDITATION, AUDIO_STORY veya LULLABY olduğunda, Backend_Sistemi içerik yerelleştirmesinde tek bir ses dosyası referansı saklamalı
9. Backend_Sistemi her içeriği CMS operasyonları ve deep linking için benzersiz bir harici anahtar ile ilişkilendirmeli
10. Backend_Sistemi her içerik için yaş aralığı ve aktif durumu saklamalı


### Gereksinim 3: İçerik Katkıda Bulunan Yönetimi

**Kullanıcı Hikayesi:** Bir içerik yöneticisi olarak, kullanıcılara uygun kredilerin gösterilebilmesi için içerik için yazarları, çizerleri, seslendirmenleri ve müzisyenleri takip etmek istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi görüntüleme adlarıyla katkıda bulunan kayıtlarını saklamalı
2. Backend_Sistemi katkıda bulunanları içerikle şu roller kullanarak ilişkilendirmeli: AUTHOR, ILLUSTRATOR, NARRATOR ve MUSICIAN
3. Bir katkıda bulunan rolü dile özgü olduğunda, Backend_Sistemi katkıda bulunan ilişkilendirmesiyle birlikte dil kodunu saklamalı
4. Backend_Sistemi tek bir içerik öğesi için aynı role sahip birden fazla katkıda bulunana izin vermeli
5. Aynı rol için birden fazla katkıda bulunan mevcut olduğunda, Backend_Sistemi onları sıralama değerine göre sıralamalı
6. Backend_Sistemi her içerik katkıda bulunan ilişkilendirmesi için isteğe bağlı kredi adı geçersiz kılmasına izin vermeli


### Gereksinim 4: Dil Bazlı Kürasyon ile Kategori Yönetimi

**Kullanıcı Hikayesi:** Bir içerik yöneticisi olarak, farklı pazarların özelleştirilmiş içerik koleksiyonlarına sahip olabilmesi için içeriği dile özgü kürasyon ile kategoriler halinde organize etmek istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi kategori tiplerini icerik tipleri ile ayni eksende desteklemeli: STORY, AUDIO_STORY, MEDITATION ve LULLABY
2. Backend_Sistemi her kategoriyi benzersiz bir slug tanımlayıcısı ile ilişkilendirmeli
3. Bir kategori oluşturulduğunda, Backend_Sistemi desteklenen her dil için ayrı yerelleştirmelere izin vermeli
4. Backend_Sistemi her kategori yerelleştirmesi için dile özgü ad, açıklama ve görsel saklamalı
5. Bir kategori yerelleştirmesinin durumu PUBLISHED olduğunda, Backend_Sistemi o kategoriyi ilgili dil için API yanıtlarına dahil etmeli
6. Bir kategori yerelleştirmesinin durumu DRAFT veya ARCHIVED olduğunda, Backend_Sistemi o kategoriyi ilgili dil için API yanıtlarından hariç tutmalı
7. Backend_Sistemi içeriği kategorilerle dil bazında ilişkilendirmeli
8. İçerik bir dil için bir kategori ile ilişkilendirildiğinde, Backend_Sistemi bir görüntüleme sırası değeri saklamalı
9. Bir dil için kategori içeriklerini alırken, Backend_Sistemi yalnızca o dilde PUBLISHED durumuna sahip içeriği görüntüleme sırasına göre sıralanmış olarak döndürmeli


### Gereksinim 5: Medya Varlığı Yönetimi

**Kullanıcı Hikayesi:** Bir içerik yöneticisi olarak, içeriğin dosyaları veritabanında saklamadan görseller ve sesler içerebilmesi için Firebase Storage'da saklanan medya dosyalarına referans vermek istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi sağlayıcı tipi, nesne yolu ve medya türü ile medya varlığı referanslarını saklamalı
2. Backend_Sistemi iki medya türünü desteklemeli: IMAGE ve AUDIO
3. Backend_Sistemi sağlayıcı ve nesne yolu kombinasyonunda benzersizliği zorunlu kılmalı
4. Backend_Sistemi medya varlıkları için MIME tipi, bayt cinsinden dosya boyutu ve SHA-256 checksum dahil isteğe bağlı metadata saklamalı
5. Backend_Sistemi liste ekranı performansını iyileştirmek için medya varlıkları için isteğe bağlı indirme URL önbelleklemeye izin vermeli
6. Bir medya varlığı oluşturulduğunda, Backend_Sistemi oluşturma zaman damgasını kaydetmeli


### Gereksinim 13: Admin Kimlik Doğrulama ve Yetkilendirme

**Kullanıcı Hikayesi:** Bir sistem yöneticisi olarak, CMS erişiminin güvenli ve Firebase Auth'tan bağımsız olması için admin kullanıcıları uygulama kullanıcılarından ayrı olarak JWT tabanlı kimlik doğrulama ile yönetmek istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi admin kullanıcıları kullanıcı adı ve bcrypt-hash'lenmiş şifre ile saklamalı
2. Backend_Sistemi admin kullanıcı adlarında benzersizliği zorunlu kılmalı
3. Bir admin kullanıcı geçerli kimlik bilgileriyle kimlik doğrulaması yaptığında, Backend_Sistemi 1 saatlik süre sonu ile bir JWT erişim token'ı vermeli
4. Bir admin kullanıcı geçerli kimlik bilgileriyle kimlik doğrulaması yaptığında, Backend_Sistemi 30 günlük süre sonu ile bir yenileme token'ı vermeli
5. Backend_Sistemi yenileme token'larını SHA-256 hash'leri olarak saklamalı
6. Backend_Sistemi yenileme token hash'lerinde benzersizliği zorunlu kılmalı
7. Bir yenileme token'ı kullanıldığında, Backend_Sistemi token hash'ini, süre sonunu ve iptal durumunu doğrulamalı
8. Backend_Sistemi admin kullanıcıların etkinleştirilmesine veya devre dışı bırakılmasına izin vermeli
9. Bir admin kullanıcı devre dışı bırakıldığında, Backend_Sistemi kimlik doğrulama girişimlerini reddetmeli
10. Backend_Sistemi her admin kullanıcı için son oturum açma zaman damgasını kaydetmeli


### Gereksinim 14: Admin Rol Tabanlı Erişim Kontrolü

**Kullanıcı Hikayesi:** Bir sistem yöneticisi olarak, CMS özelliklerine erişimin sorumluluklara göre kontrol edilebilmesi için admin kullanıcılara roller atamak istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi benzersiz rol kodları ve açıklamalarıyla admin rollerini saklamalı
2. Backend_Sistemi tek bir admin kullanıcıya birden fazla rolün atanmasına izin vermeli
3. Bir admin rolü bir kullanıcıya atandığında, Backend_Sistemi atama zaman damgasını kaydetmeli
4. Backend_Sistemi rol atamalarının admin kullanıcılardan kaldırılmasına izin vermeli


### Gereksinim 15: Yenileme Token Yönetimi ve Rotasyonu

**Kullanıcı Hikayesi:** Bir güvenlik mühendisi olarak, ele geçirilmiş token'ların geçersiz kılınabilmesi ve token yeniden kullanımının önlenebilmesi için yenileme token rotasyonu ve iptali uygulamak istiyorum.

#### Kabul Kriterleri

1. Yeni bir erişim token'ı elde etmek için bir yenileme token'ı kullanıldığında, Backend_Sistemi yeni bir yenileme token'ı vermeli
2. Rotasyon yoluyla yeni bir yenileme token'ı verildiğinde, Backend_Sistemi yeni token kaydında değiştirilen token'ın hash'ini saklamalı
3. Backend_Sistemi bir iptal zaman damgası ayarlayarak yenileme token'larının iptal edilmesine izin vermeli
4. İptal edilmiş bir yenileme token'ı sunulduğunda, Backend_Sistemi kimlik doğrulama isteğini reddetmeli
5. Bir yenileme token'ının süresi dolduğunda, Backend_Sistemi o token'ı kullanan kimlik doğrulama isteklerini reddetmeli
6. Backend_Sistemi her yenileme token'ı için isteğe bağlı kullanıcı aracısı ve IP adresi saklamalı
7. Backend_Sistemi token süre sonu zamanının veriliş zamanından büyük olmasını zorunlu kılmalı


### Gereksinim 17: Veritabanı Şema Bütünlüğü ve Kısıtlamaları

**Kullanıcı Hikayesi:** Bir veritabanı yöneticisi olarak, geçersiz verilerin eklenememesi ve ilişkilerin korunması için şemanın veri bütünlüğü kurallarını zorunlu kılmasını istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi uygun cascade veya set null davranışlarıyla tüm ilişki referansları için foreign key kısıtlamalarını zorunlu kılmalı
2. Backend_Sistemi Firebase UID, medya varlığı sağlayıcı ve yol, kategori slug, içerik harici anahtar, admin kullanıcı adı ve yenileme token hash'i üzerinde benzersizlik kısıtlamalarını zorunlu kılmalı
3. Backend_Sistemi sayfa numaraları, görüntüleme sıraları, etkileşim saniyeleri ve parasal tutarlar üzerinde negatif olmayan değerler için check kısıtlamalarını zorunlu kılmalı
4. Backend_Sistemi 1'den başlayan geçerli sayfa numaraları için check kısıtlamalarını zorunlu kılmalı
5. Backend_Sistemi veriliş zamanlarından büyük token süre sonu zamanları için check kısıtlamalarını zorunlu kılmalı
6. Backend_Sistemi üst kayıtlar silindiğinde bağımlı kayıtlar için cascade delete kullanmalı
7. Backend_Sistemi referans verilen kayıtlar silindiğinde isteğe bağlı referanslar için set null kullanmalı
8. Backend_Sistemi birincil profiller ve eski event anahtarları dahil koşullu benzersizlik gereksinimleri için kısmi benzersiz indeksler zorunlu kılmalı


### Gereksinim 19: Zaman Damgası Yönetimi ve Saat Dilimi İşleme

**Kullanıcı Hikayesi:** Bir veri analisti olarak, zamana dayalı sorguların farklı bölgelerde doğru olması için tüm zaman damgalarının saat dilimi bilgisiyle UTC'de saklanmasını istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi tüm zaman damgası alanlarını timestamptz veri tipini kullanarak saklamalı
2. Bir kayıt oluşturulduğunda, Backend_Sistemi created_at'i geçerli UTC zaman damgasına ayarlamalı
3. Bir kayıt güncellendiğinde, Backend_Sistemi updated_at'i geçerli UTC zaman damgasına ayarlamalı
4. Bir event alındığında, Backend_Sistemi ingested_at'i geçerli UTC zaman damgasına ayarlamalı
5. İstemci tarafından bir event gönderildiğinde, Backend_Sistemi istemci tarafından sağlanan occurred_at zaman damgasını korumalı
6. Backend_Sistemi tüm event kayıtları için ayrı occurred_at ve ingested_at zaman damgalarını tutmalı


### Gereksinim 20: İçerik ve Kategori Soft Delete

**Kullanıcı Hikayesi:** Bir içerik yöneticisi olarak, tarihsel referansların geçerli kalması ve öğelerin kullanıcılardan gizlenmesi için içeriği ve kategorileri silmeden devre dışı bırakmak istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi varsayılan değeri true olan içerik kayıtları için bir is_active bayrağı saklamalı
2. Backend_Sistemi varsayılan değeri true olan kategori kayıtları için bir is_active bayrağı saklamalı
3. İçerik is_active false olduğunda, Backend_Sistemi o içeriği API liste yanıtlarından hariç tutmalı
4. Kategori is_active false olduğunda, Backend_Sistemi o kategoriyi API liste yanıtlarından hariç tutmalı
5. İçerik veya kategori is_active false olduğunda, Backend_Sistemi tüm veritabanı ilişkilerini ve tarihsel event referanslarını korumalı


### Gereksinim 21: Dil Seed Verisi

**Kullanıcı Hikayesi:** Bir sistem yöneticisi olarak, içerik yöneticilerinin hemen yerelleştirmeler oluşturmaya başlayabilmesi için desteklenen dillerin veritabanında önceden doldurulmasını istiyorum.

#### Kabul Kriterleri

1. Veritabanı şeması başlatıldığında, Backend_Sistemi tr, en, es, pt ve de için dil kayıtları eklemelidir
2. Backend_Sistemi her dil için görüntüleme adlarını ayarlamalı: Türkçe, İngilizce, İspanyolca, Portekizce ve Almanca
3. Backend_Sistemi tüm seed diller için is_active'i true olarak ayarlamalı
4. Veritabanı şeması başlatıldığında, Backend_Sistemi tüm seed diller için oluşturma zaman damgalarını kaydetmeli


### Gereksinim 22: Asset İşleme ve Mobil Paketleme Sistemi

**Kullanıcı Hikayesi:** Bir içerik yöneticisi olarak, mobil uygulamanın optimize edilmiş ve sıkıştırılmış içerik paketleri alabilmesi için yüksek çözünürlüklü orijinal medya dosyalarının otomatik olarak işlenmesini ve paketlenmesini istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi her içerik yerelleştirmesi için bir processing_status alanı saklamalı: PENDING, PROCESSING, COMPLETED veya FAILED
2. Bir içerik yöneticisi CMS'de "Paketlemeyi Başlat" butonuna tıkladığında, Backend_Sistemi o dil için asset işleme sürecini manuel olarak tetiklemeli
3. Asset işleme süreci başladığında, Backend_Sistemi processing_status'u PROCESSING olarak ayarlamalı
4. Backend_Sistemi orijinal yüksek çözünürlüklü medya dosyalarını Firebase Storage'da `/content/{content_type}/{external_key}/{lang}/original/` yolunda saklamalı
5. Backend_Sistemi işlenmiş ve optimize edilmiş medya dosyalarını Firebase Storage'da `/content/{content_type}/{external_key}/{lang}/processed/` yolunda saklamalı
6. Backend_Sistemi her kapak görseli için dört optimize varyant üretmeli: thumbnail_phone, thumbnail_tablet, detail_phone ve detail_tablet
7. Backend_Sistemi resim optimizasyonu için mobil platformlar için en uygun sıkıştırma formatını ve kalite ayarlarını kullanmalı
8. Backend_Sistemi ses optimizasyonu için ses kalitesini kaybetmeden minimum dosya boyutunu hedefleyen format ve bitrate kullanmalı
9. İçerik tipi STORY olduğunda, Backend_Sistemi iki ZIP paketi oluşturmalı: ilk 3 sayfa için `{external_key}_part1.zip` ve kalan sayfalar için `{external_key}_part2.zip`
10. İçerik tipi AUDIO_STORY, MEDITATION veya LULLABY olduğunda, Backend_Sistemi tüm optimize edilmiş assetleri içeren tek bir ZIP paketi oluşturmalı: `{external_key}.zip`
11. Backend_Sistemi ZIP paketleri içindeki dosyaları sayfa numarasına göre isimlendirmeli: `1.jpg`, `1.mp3`, `2.jpg`, `2.mp3` vb.
12. Backend_Sistemi ZIP paketlerini Firebase Storage'da `/content/{content_type}/{external_key}/{lang}/packages/` yolunda saklamalı
13. Asset işleme başarıyla tamamlandığında, Backend_Sistemi processing_status'u COMPLETED olarak ayarlamalı
14. Asset işleme başarısız olduğunda, Backend_Sistemi processing_status'u FAILED olarak ayarlamalı ve hata detaylarını loglamalı
15. Processing_status FAILED olduğunda, Backend_Sistemi içerik yöneticisinin CMS'den işlemi manuel olarak yeniden tetiklemesine izin vermeli
16. Backend_Sistemi orijinal medya dosyalarını, optimize edilmiş varyantları ve ZIP paketlerini aynı media_assets tablosunda farklı kind değerleriyle saklamalı
17. Backend_Sistemi şu medya kind değerlerini desteklemeli: ORIGINAL_IMAGE, ORIGINAL_AUDIO, THUMBNAIL_PHONE, THUMBNAIL_TABLET, DETAIL_PHONE, DETAIL_TABLET, OPTIMIZED_AUDIO, CONTENT_ZIP, CONTENT_ZIP_PART1, CONTENT_ZIP_PART2
18. Processing_status COMPLETED olmadığı sürece, Backend_Sistemi o dil için içerik yerelleştirmesini mobil API yanıtlarından hariç tutmalı
19. Mobil API bir içerik döndürdüğünde, Backend_Sistemi ZIP paket referanslarını ve optimize edilmiş kapak varyantlarını yanıta dahil etmeli
20. Backend_Sistemi asset işleme sürecini asenkron olarak (arka planda) çalıştırmalı ve işlem sırasında CMS'in yanıt vermesini engellememelidir


### Gereksinim 23: A/B Test Destekli Ücretsiz İçerik Erişim Yönetimi

**Kullanıcı Hikayesi:** Bir ürün yöneticisi olarak, ücretsiz içeriklerin dönüşüm oranını ölçebilmek için Firebase Remote Config A/B testi ile farklı kullanıcı gruplarına farklı ücretsiz içerik setleri sunmak istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi ücretsiz içerik erişim kayıtlarını anahtar, içerik ID ve dil kodu kombinasyonuyla saklamalı
2. Backend_Sistemi `default` anahtarıyla tanımlanan varsayılan bir ücretsiz içerik seti içermeli
3. Mobil_İstemci `freeKey` query parametresi ile bir erişim anahtarı gönderdiğinde, Backend_Sistemi o anahtara karşılık gelen içerikleri ücretsiz olarak işaretlemeli
4. Mobil_İstemci `freeKey` parametresi göndermediğinde, Backend_Sistemi `default` anahtarına karşılık gelen içerikleri ücretsiz olarak işaretlemeli
5. Bilinmeyen bir `freeKey` değeri gönderildiğinde, Backend_Sistemi `default` anahtarına geri dönmeli
6. Ücretsiz erişim durumu dil bazlı olmalı; aynı içerik bir dilde ücretsiz, başka bir dilde premium olabilmeli
7. Bir içeriğin ücretsiz olup olmadığı, `content_free_access` tablosunda o içerik için ilgili anahtar ve dil kombinasyonunda kayıt bulunup bulunmamasıyla belirlenmeli
8. Backend_Sistemi ücretsiz erişim kayıtlarının CMS üzerinden yönetilmesine (ekleme, silme) izin vermeli
9. Backend_Sistemi her erişim anahtarı için hangi içeriklerin ücretsiz olduğunu listeleme API'si sunmalı
10. İçerik listesi dönerken, Backend_Sistemi her içerik için `isFree` alanını aktif anahtara göre hesaplayarak yanıta dahil etmeli

---

## Faz 3: Mobil Uygulama Entegrasyonu Gereksinimleri


### Gereksinim 6: Uygulama Kullanıcısı ve Profil Yönetimi

**Kullanıcı Hikayesi:** Bir mobil uygulama kullanıcısı olarak, tercihlerimin ve geçmişimin kalıcı olması için Firebase kimlik doğrulamam bir backend profiline bağlanmalı.

#### Kabul Kriterleri

1. Bir kullanıcı Firebase Auth ile kimlik doğrulaması yaptığında, Backend_Sistemi Firebase UID kullanarak bir uygulama kullanıcı kaydı oluşturmalı veya almalı
2. Backend_Sistemi tüm uygulama kullanıcıları arasında Firebase UID'de benzersizliği zorunlu kılmalı
3. Bir uygulama kullanıcısı oluşturulduğunda, Backend_Sistemi is_primary true olarak ayarlanmış varsayılan bir kullanıcı profili oluşturmalı
4. Backend_Sistemi ad, yaş aralığı, avatar medya referansı, favori türler ve ana amaçlar dahil profil özniteliklerini saklamalı
5. Backend_Sistemi favori türleri ve ana amaçları mobil uygulama numaralandırmalarıyla eşleşen string değerleri dizileri olarak saklamalı
6. Backend_Sistemi her uygulama kullanıcısının en fazla bir birincil profile sahip olmasını zorunlu kılmalı
7. Backend_Sistemi her uygulama kullanıcısı için pazarlama onayı tercihini saklamalı


### Gereksinim 18: Firebase Veri Taşıma Desteği

**Kullanıcı Hikayesi:** Bir veri mühendisi olarak, tarihsel kullanıcı verilerinin ve eventlerin yeni sistemde korunması için mevcut Firebase verilerini PostgreSQL'e taşımak istiyorum.

#### Kabul Kriterleri

1. Firebase kullanıcı kayıtları içe aktarılırken, Backend_Sistemi Firebase UID'leriyle uygulama kullanıcıları oluşturmalı
2. Firebase kullanıcı kayıtları içe aktarılırken, Backend_Sistemi her uygulama kullanıcısı için varsayılan kullanıcı profilleri oluşturmalı
3. Firebase geçmiş kayıtları içe aktarılırken, Backend_Sistemi legacy_event_key değerleriyle içerik eventleri oluşturmalı
4. Firebase geçmiş kayıtları içe aktarılırken, Backend_Sistemi Firebase event tiplerini içerik event tiplerine eşlemeli: START_CONTENT'i START'a, LEFT_CONTENT'i EXIT'e ve FINISH_CONTENT'i COMPLETE'e
5. Backend_Sistemi içe aktarılan eventler için profile_id ve legacy_event_key kombinasyonunda benzersizliği zorunlu kılmalı
6. Firebase uygulama eventleri içe aktarılırken, Backend_Sistemi legacy_event_key değerleriyle uygulama eventleri oluşturmalı
7. Backend_Sistemi içe aktarılan uygulama eventleri için profile_id ve legacy_event_key kombinasyonunda benzersizliği zorunlu kılmalı

---

## Faz 4: RevenueCat Entegrasyonu Gereksinimleri

### Gereksinim 9: Abonelik Ürün Kataloğu

**Kullanıcı Hikayesi:** Bir ürün yöneticisi olarak, satın alma eventlerinin bilinen ürünlere karşı doğrulanabilmesi ve analiz edilebilmesi için bir abonelik ürün kataloğu tutmak istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi mağaza kodu ve ürün ID'si kombinasyonuyla tanımlanan abonelik ürünlerini saklamalı
2. Backend_Sistemi iki ürün tipini desteklemeli: SUBSCRIPTION ve NON_RENEWING
3. Backend_Sistemi birim (DAY, WEEK, MONTH, YEAR) ve sayı ile faturalama dönemi bilgilerini saklamalı
4. Backend_Sistemi her abonelik ürünü için RevenueCat yetkilendirme ID eşlemelerini saklamalı
5. Backend_Sistemi abonelik ürünlerinin etkin olmayan olarak işaretlenmesine izin vermeli
6. Bir abonelik ürünü oluşturulduğunda veya güncellendiğinde, Backend_Sistemi zaman damgasını kaydetmeli


### Gereksinim 10: Satın Alma Event Takibi ve RevenueCat Entegrasyonu

**Kullanıcı Hikayesi:** Bir iş analisti olarak, gelir, kayıp ve abonelik metriklerini analiz edebilmek için RevenueCat'ten gelen tüm abonelik yaşam döngüsü eventlerini yakalamak istiyorum.

#### Kabul Kriterleri

1. RevenueCat bir webhook gönderdiğinde, Backend_Sistemi tüm RevenueCat alanlarıyla satın alma eventini kabul etmeli ve saklamalı
2. Backend_Sistemi yinelenen bir revenuecat_event_id ile bir satın alma eventi aldığında, Backend_Sistemi yeni bir kayıt oluşturmadan yinelemeyi reddetmeli
3. Backend_Sistemi satın alma event kaynağını REVENUECAT_WEBHOOK veya CLIENT olarak saklamalı
4. Backend_Sistemi her satın alma eventi için tam ham webhook payload'ını JSON olarak saklamalı
5. Backend_Sistemi RevenueCat'ten event_type, product_id, store, currency, price, transaction_id ve country_code dahil normalize edilmiş alanları saklamalı
6. Backend_Sistemi expiration_at, period_type, renewal_number, cancel_reason ve expiration_reason dahil abonelik yaşam döngüsü alanlarını saklamalı
7. Backend_Sistemi her satın alma eventi için iki zaman damgası saklamalı: satın alma zamanından occurred_at ve sunucudan ingested_at
8. Backend_Sistemi deneme dönüşümü, ödemesiz dönem, otomatik devam, vergi yüzdesi ve komisyon yüzdesi için isteğe bağlı alanları saklamalı
9. Backend_Sistemi her satın alma eventini bir uygulama kullanıcısıyla ilişkilendirmeli
10. Bir satın alma eventi bilinen bir abonelik ürününe referans verdiğinde, Backend_Sistemi abonelik ürün kataloğuna bağlantı kurmalı


### Gereksinim 12: Satın Alma Event Doğrulama için Arama Tabloları

**Kullanıcı Hikayesi:** Bir veri mühendisi olarak, analitik sorguların güvenilir olması ve veri kalitesinin korunması için satın alma event alanlarını bilinen değer setlerine karşı doğrulamak istiyorum.

#### Kabul Kriterleri

1. Backend_Sistemi INITIAL_PURCHASE, RENEWAL, CANCELLATION, EXPIRATION ve diğer RevenueCat event tipleri dahil geçerli satın alma event tiplerinin bir arama tablosunu tutmalı
2. Backend_Sistemi TRIAL, INTRO, NORMAL, PROMOTIONAL ve PREPAID dahil geçerli abonelik dönem tiplerinin bir arama tablosunu tutmalı
3. Backend_Sistemi APP_STORE, PLAY_STORE, STRIPE, RC_BILLING ve AMAZON dahil geçerli satın alma mağazalarının bir arama tablosunu tutmalı
4. Backend_Sistemi geçerli satın alma ortamlarının bir arama tablosunu tutmalı: SANDBOX ve PRODUCTION
5. Backend_Sistemi iptal ve süre sonu nedeni kodları için neden tipi sınıflandırmasıyla satın alma neden kodlarının bir arama tablosunu tutmalı
6. Backend_Sistemi arama tablosu girişlerinin etkin olmayan olarak işaretlenmesine izin vermeli
7. Bir satın alma eventi bir arama tablosu değerine referans verdiğinde, Backend_Sistemi arama tablosuna karşı doğrulama yapmalı

---

## Faz 5: Analitik ve Event Tracking Gereksinimleri

### Gereksinim 7: Offline-First İçerik Event Takibi

**Kullanıcı Hikayesi:** Bir mobil uygulama kullanıcısı olarak, ilerlememin ve analitiğimin güvenilir bir şekilde yakalanması için içerik etkileşimlerimin offline olsa bile takip edilmesini istiyorum.

#### Kabul Kriterleri

1. Mobil_İstemci bir içerik eventi gönderdiğinde, Backend_Sistemi istemci tarafından üretilen bir event_id UUID'si kabul etmeli
2. Backend_Sistemi yinelenen bir event_id ile bir içerik eventi aldığında, Backend_Sistemi yeni bir kayıt oluşturmadan yinelemeyi reddetmeli
3. Backend_Sistemi üç içerik event tipini desteklemeli: START, EXIT ve COMPLETE
4. Bir içerik eventi gönderildiğinde, Backend_Sistemi profil ID'si, içerik ID'si, dil kodu ve event tipini saklamalı
5. Backend_Sistemi her içerik eventi için iki zaman damgası saklamalı: istemciden occurred_at ve sunucudan ingested_at
6. Backend_Sistemi içerik eventleri için isteğe bağlı oturum ID'si, ayrılan sayfa numarası ve etkileşim saniyeleri saklamalı
7. Backend_Sistemi segmentasyon alanlarını desteklemek için içerik eventleri için isteğe bağlı metadata'yı JSON olarak saklamalı
8. Bir içerik eventi legacy_event_key içerdiğinde, Backend_Sistemi profil ID'si ve legacy_event_key kombinasyonunda benzersizliği zorunlu kılmalı


### Gereksinim 8: Uygulama Event Takibi

**Kullanıcı Hikayesi:** Bir ürün analisti olarak, dönüşüm hunilerini ve kullanıcı aktivasyonunu ölçebilmek için paywall ve onboarding gibi uygulama özellikleriyle kullanıcı etkileşimlerini takip etmek istiyorum.

#### Kabul Kriterleri

1. Mobil_İstemci bir uygulama eventi gönderdiğinde, Backend_Sistemi istemci tarafından üretilen bir event_id UUID'si kabul etmeli
2. Backend_Sistemi yinelenen bir event_id ile bir uygulama eventi aldığında, Backend_Sistemi yeni bir kayıt oluşturmadan yinelemeyi reddetmeli
3. Backend_Sistemi altı uygulama event tipini desteklemeli: APP_OPENED, ONBOARDING_STARTED, ONBOARDING_COMPLETED, ONBOARDING_SKIPPED, PAYWALL_SHOWN ve LOCKED_CONTENT_CLICKED
4. Bir uygulama eventi gönderildiğinde, Backend_Sistemi profil ID'si ve event tipini saklamalı
5. Backend_Sistemi her uygulama eventi için iki zaman damgası saklamalı: istemciden occurred_at ve sunucudan ingested_at
6. Bir uygulama eventi içerikle ilişkili olduğunda, Backend_Sistemi içerik ID'sini saklamalı
7. Backend_Sistemi evente özgü bağlamı yakalamak için uygulama eventleri için isteğe bağlı payload'ı JSON olarak saklamalı
8. Bir uygulama eventi legacy_event_key içerdiğinde, Backend_Sistemi profil ID'si ve legacy_event_key kombinasyonunda benzersizliği zorunlu kılmalı


### Gereksinim 11: Satın Alma Atıflandırma ve Bağlam Anlık Görüntüleri

**Kullanıcı Hikayesi:** Bir pazarlama analisti olarak, dönüşüm stratejilerini optimize edebilmek için hangi içerik veya paywall etkileşimlerinin satın almalara yol açtığını anlamak istiyorum.

#### Kabul Kriterleri

1. Bir satın alma eventi kaydedildiğinde, Backend_Sistemi bir satın alma bağlam anlık görüntüsü oluşturmalı
2. Backend_Sistemi her satın alma eventinin en fazla bir bağlam anlık görüntüsüne sahip olmasını zorunlu kılmalı
3. Backend_Sistemi satın alma anındaki kullanıcı profili anlık görüntüsünü yaş aralığı, favori türler ve ana amaçlar dahil JSON olarak saklamalı
4. Backend_Sistemi tetikleyici uygulama eventini tanımlamak için varsayılan olarak 24 saatlik bir atıflandırma penceresi kullanmalı
5. Atıflandırılan uygulama eventini tanımlarken, Backend_Sistemi atıflandırma penceresi içindeki en son LOCKED_CONTENT_CLICKED eventini seçmeli
6. Atıflandırma penceresi içinde LOCKED_CONTENT_CLICKED eventi yoksa, Backend_Sistemi en son PAYWALL_SHOWN eventini seçmeli
7. Atıflandırılan bir uygulama eventi tanımlandığında, Backend_Sistemi bağlam anlık görüntüsünde uygulama event ID'sini ve ilişkili içerik ID'sini saklamalı
8. Backend_Sistemi her bağlam anlık görüntüsü için atıflandırma penceresi süresini saniye cinsinden saklamalı


### Gereksinim 16: Analitik Sorgu Desteği

**Kullanıcı Hikayesi:** Bir veri analisti olarak, kullanıcı davranışı ve gelir hakkında raporlar oluşturabilmek için içerik etkileşimi ve satın alma metriklerini verimli bir şekilde sorgulamak istiyorum.

#### Kabul Kriterleri

1. İçerik eventlerini profile göre sorgularken, Backend_Sistemi profile_id ve occurred_at üzerinde bir indeks kullanmalı
2. İçerik eventlerini içeriğe göre sorgularken, Backend_Sistemi content_id ve occurred_at üzerinde bir indeks kullanmalı
3. İçerik eventlerini event tipine göre sorgularken, Backend_Sistemi event_type ve occurred_at üzerinde bir indeks kullanmalı
4. İçerik eventlerini oturuma göre sorgularken, Backend_Sistemi session_id üzerinde bir indeks kullanmalı
5. Uygulama eventlerini profile göre sorgularken, Backend_Sistemi profile_id ve occurred_at üzerinde bir indeks kullanmalı
6. Uygulama eventlerini içeriğe göre sorgularken, Backend_Sistemi content_id ve occurred_at üzerinde bir indeks kullanmalı
7. Uygulama eventlerini event tipine göre sorgularken, Backend_Sistemi event_type ve occurred_at üzerinde bir indeks kullanmalı
8. Satın alma eventlerini kullanıcıya göre sorgularken, Backend_Sistemi user_id ve occurred_at üzerinde bir indeks kullanmalı
9. Satın alma bağlam anlık görüntülerini kullanıcıya göre sorgularken, Backend_Sistemi user_id ve created_at üzerinde bir indeks kullanmalı
