# Google Play Veri Güvenliği Beyan Taslağı

> Durum: Kod ve SDK envanterine göre yerel taslak. Play Console'a girilmiş veya gönderilmiş değildir.
> Son doğrulama: 16 Temmuz 2026, AppOrganizer 1.3.67 (versionCode 90).

## Form özeti

- Uygulama kullanıcı verisi toplar: **Evet**.
- Uygulama kullanıcı verisini paylaşır: **Hayır**. Firebase/Google, geliştirici adına hizmet sağlayıcı olarak işler; Play formundaki hizmet sağlayıcı istisnası uygulanır.
- Veriler aktarım sırasında şifrelenir: **Evet** (Firebase ve DeepSeek HTTPS uçları).
- Telemetri isteğe bağlıdır: **Evet**. Analytics, Crashlytics ve Performance varsayılan kapalıdır ve Kullanım Verileri anahtarıyla birlikte yönetilir.
- Veri silme talebi: Uygulama içi yerel sıfırlama yerel veriyi siler; Firebase/DeepSeek tarafındaki talepler için gizlilik politikasındaki geliştirici iletişim adresi kullanılır.

## Toplanan veri türleri

| Play veri türü | Kaynak ve kapsam | Amaç | Zorunluluk / kontrol |
|---|---|---|---|
| Uygulama etkinliği > Uygulama etkileşimleri | Firebase Analytics: sabit özellik kullanım sayaçları, kapalı enum ve kovalar; arama metni, paket/uygulama adı ve özel ad yok | Analiz | İsteğe bağlı; Kullanım Verileri anahtarı kapalıyken gönderilmez |
| Uygulama bilgileri ve performansı > Kilitlenme günlükleri | Firebase Crashlytics: stack trace, uygulama/Android sürümü, cihaz modeli, sabit sağlık kodları | Analiz; hata teşhisi ve uygulama kararlılığını iyileştirme | İsteğe bağlı; kapatma ağ geçidini hemen kapatır, SDK otomatik toplama değişikliği sonraki açılışta tamamen uygulanır |
| Uygulama bilgileri ve performansı > Tanılama | Firebase Performance: sabit işlem izleri ve süreleri | Analiz; performans teşhisi | İsteğe bağlı; anahtar kapalıyken yeni iz başlatılmaz |
| Uygulama bilgileri ve performansı > Diğer performans verileri | Günlük anonim sağlık özeti: sabit uyarı kodları, indeks yaşı ve sınırlı sayaç/kovalar | Analiz; hata teşhisi | İsteğe bağlı; anahtar kapatılınca worker iptal edilir |
| Cihaz veya diğer kimlikler | Firebase Installation ID ve FCM kayıt tokenı | Uygulama işlevselliği (backend bağlantısı ve kategori veritabanı güncelleme bildirimi); Firebase telemetrisi açıkken analiz/hata teşhisi | FCM, telemetri anahtarından bağımsızdır; telemetri amaçlı kullanım isteğe bağlıdır |
| Uygulama etkinliği > Yüklü uygulamalar | Kullanıcı DeepSeek sınıflandırmasını açıkça etkinleştirip kendi API anahtarını girerse yalnız sınıflandırılacak uygulamanın adı ve paket adı | Uygulama işlevselliği | İsteğe bağlı; özellik kullanılmadığında gönderilmez |

## Cihazdan çıkmayan ve “toplanan” sayılmayan veriler

Kurulu uygulama envanteri (DeepSeek istisnası dışında), UsageStats geçmişi, bildirim içeriği, kişiler,
dosya adları, özel klasör adları ve yerel raporlar cihazda işlenir. SAF ile seçilen bulut belge sağlayıcısına
kullanıcının açık eylemiyle yazılan yedek, sağlayıcının koşullarına tabidir ve Firebase telemetrisi değildir.

## Play Console readback kontrolü

1. Bu matrisi aktif tüm artifact ve bölgelerin gerçek davranışıyla karşılaştır.
2. `Policy > App content > Data safety` alanına yanıtları girip önizlemeyi dışa aktar.
3. Gizlilik politikası URL'sinin HTTP 200 verdiğini ve uygulama içindeki URL ile aynı olduğunu doğrula.
4. Form ekran görüntüsü/dışa aktarımı ile Policy Status sonucunu `docs/PLAY_RELEASE_EVIDENCE_CHECKLIST.md` kanıtlarına ekle.
5. Bu kanıtlar olmadan B13 tamamlandı sayılmaz.

## Resmî kaynaklar

- https://support.google.com/googleplay/android-developer/answer/10787469
- https://support.google.com/googleplay/android-developer/answer/10144311
- https://firebase.google.com/docs/analytics/android/configure-data-collection
- https://firebase.google.com/docs/crashlytics/customize-crash-reports
- https://firebase.google.com/docs/perf-mon/disable-sdk
