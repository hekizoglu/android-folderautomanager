# Play Store Yayın Kanıt Kontrol Listesi

> Durum: Yerel hazırlık. Bu dosyanın varlığı Play Console adımlarının tamamlandığı anlamına gelmez.
> Dış aksiyon sahibi: Play Console hesap sahibi / release yetkilisi.

## 1. QUERY_ALL_PACKAGES izin beyanı

Önerilen beyan metni:

> AppOrganizer'ın kullanıcıya dönük temel işlevi, cihazdaki tüm başlatılabilir uygulamaları tek bir launcher yüzeyinde keşfetmek, otomatik kategorilere ayırmak, aramak ve başlatmaktır. Kullanıcı önceden bilinen sınırlı bir paket kümesiyle çalışmaz; cihazda kurulu herhangi bir uygulamanın görünür olması gerekir. Yalnız `<queries>` ile belirli paket veya intent gruplarını tanımlamak, bilinmeyen ve sonradan yüklenen uygulamaları eksik bırakır; bu durumda uygulama çekmecesi, arama ve otomatik klasörleme temel işlevleri bozulur. Kurulu uygulama envanteri cihazda işlenir; reklam veya analitik amacıyla satılmaz ya da paylaşılmaz.

Play Console adımları:

1. İmzalı release AAB'yi Internal/Closed track taslağına yükle.
2. `Policy > App content > Permissions declaration form` bölümünü aç.
3. AAB'de `QUERY_ALL_PACKAGES` izninin algılandığını doğrula.
4. Ana işlev olarak cihazdaki uygulamaları arama/keşfetme/başlatma kullanımını seç ve yukarıdaki gerekçeyi ürünün gerçek davranışına göre gir.
5. İnceleyenin izleyeceği yolu yaz: onboarding → launcher rolü → tüm uygulamalar → arama → klasör → uygulama başlatma.
6. Kişisel veri göstermeyen kesintisiz demo videosu ekle. Video; temiz cihazdaki farklı uygulamaların otomatik bulunması, aranması ve başlatılmasını göstermeli.
7. Formu gönder; tarihli form ekran görüntüsü ve Play kararını sakla.

Gerekli kanıtlar:

- AAB versionCode/versionName ve manifest izin çıktısı
- Store Listing'de launcher/uygulama keşfi ana işlev açıklaması
- Demo video URL'si ve inceleme erişim talimatı
- Hedefli `<queries>` yaklaşımının neden temel işlevi eksik bıraktığı teknik açıklama
- Gönderilmiş form ekran görüntüsü ve Policy Status sonucu

## 2. Privacy Policy ve Data Safety

1. `docs/privacy_policy.html` dosyasını herkese açık, giriş/coğrafi kısıt istemeyen ve PDF olmayan URL'de yayımla.
2. URL'nin HTTP 200 verdiğini ve uygulama içindeki bağlantıyla aynı olduğunu doğrula.
3. Aktif tüm sürümler ve SDK'lar için veri envanteri çıkar: kurulu uygulamalar, UsageStats, bildirim olayları/metni, kişiler, dosya indeksi, yedek/SAF sağlayıcısı, Firebase Analytics/Crashlytics/FCM ve isteğe bağlı DeepSeek.
4. Her veri türü için `collected/shared`, amaç, zorunlu/isteğe bağlı, geçici/kalıcı, aktarım şifreleme ve silme cevaplarını kod gerçeğine göre doldur.
5. `Policy > App content > Privacy policy` alanına URL'yi gir; `Data safety` formunu önizleyip gönder.
6. Uygulama içi politika ekranını ve prominent disclosure gerektiren izin akışlarını gerçek cihazda kaydet.

Gerekli kanıtlar:

- Yayın URL'si + HTTP 200 + tarih
- Uygulama içi politika ekran kaydı
- Data Safety cevaplarının ekran görüntüsü/dışa aktarımı
- SDK ve veri akışı envanteri
- Policy Status / inceleme sonucu

## 3. Release imza ve final AAB

1. Anahtar sahibi `scripts/create_release_keystore.ps1` ile upload/release anahtarını üretir veya mevcut güvenli anahtarı kullanır.
2. `release.jks` ve `keystore.properties` dosyalarının git dışında ve güvenli yedekte olduğunu doğrular.
3. Temiz committen `./gradlew bundleRelease` çalıştırır.
4. AAB için dosya adı, boyut, SHA-256, versionCode/versionName ve imza sertifikası SHA-256 parmak izini kaydeder; anahtar/parola kaydetmez.
5. Play App Signing şartlarını/yetkiyi doğrular ve AAB'yi test kanalına yükler.
6. `Preview and confirm` hataları, App Bundle Explorer ve pre-launch raporunu inceler.
7. Tüm App content/policy kapıları kapandıktan sonra production review'e gönderir.

Gerekli kanıtlar:

- Başarılı `bundleRelease` logu
- AAB SHA-256, boyut ve sürüm bilgisi
- Upload/app-signing sertifika parmak izleri
- App Bundle Explorer yükleme sonucu
- Pre-launch raporu ve track/release durumu

## Tamamlanma kuralı

Bu üç başlık, yalnız Play Console readback ve kanıt dosyaları mevcut olduğunda tamamlandı olarak işaretlenir. Yerel build, taslak metin veya ekran görüntüsü hazırlığı tek başına yayın kapısını kapatmaz.
