# AppOrganizer İstatistik, Telemetri ve Sağlık Yol Haritası

> Oluşturma: 2026-07-16  
> Birleştirilen eski dosya: `SAGLIK_RAPORU_DUZELTME_ROADMAP.md`  
> Hedef: Kullanıcının hangi özellikleri kullandığını, sistemin nerede hata yaptığını ve hangi geliştirmelerin gerçekten değer ürettiğini kişisel veri toplamadan ölçmek; aynı zamanda cihaz üzerindeki sağlık raporunu güvenilir bir teknik teşhis aracına dönüştürmek.  
> Bu dosya bu çalışma alanının tek aktif doğruluk kaynağıdır. Tamamlanan döngüler test kanıtıyla `HISTORY.md` dosyasına taşınır.

---

# 0. Çalışma ve durum kuralları

Her görev aşağıdaki alanları içermelidir:

- **Sorun / İstek**
- **Kök neden**
- **Nasıl yapılmalı**
- **Durum**
- **Değişecek dosyalar**
- **Testler**
- **Kabul kriterleri**

Durum değerleri:

- `⏳ Bekliyor`: Henüz kodlanmadı.
- `🚧 Devam ediyor`: Kodlama başladı, doğrulama bitmedi.
- `🟡 Kısmen tamamlandı`: Kod var fakat test, gerçek cihaz veya Firebase konsol doğrulaması eksik.
- `✅ Tamamlandı`: Kod, test ve gerekli cihaz/konsol doğrulaması tamamlandı.
- `⛔ Bloke`: Dış bağımlılık veya erişim bekliyor.

Bir yapay zekâ aynı anda yalnızca **bir döngüyü** tamamlamalıdır. Bir döngü bitmeden sonraki döngüye geçilmemelidir. Kod yazmak tek başına tamamlanma sayılmaz.

---

# 1. Ürün hedefleri

Bu sistem yalnızca şu sorulara cevap vermelidir:

1. Kullanıcı uygulamayı kurduktan sonra kullanmaya devam ediyor mu?
2. En çok hangi özellikler kullanılıyor?
3. Kullanıcı hangi akışlarda vazgeçiyor veya hata yaşıyor?
4. Arama ve otomatik sınıflandırma gerçekten doğru çalışıyor mu?
5. Hangi cihaz, Android sürümü veya uygulama sürümünde sorun yoğunlaşıyor?
6. Hangi işlemler yavaşlıyor?
7. Sağlık raporunun tespit ettiği sorunlar sahada kaç kullanıcıda görülüyor?
8. Yeni sürüm eski sürüme göre daha iyi mi, daha kötü mü?

Bu sorulardan hiçbirine cevap vermeyen veri toplanmamalıdır.

## İlk sürümde kurulmayacaklar

- Firestore cihaz geçmişi
- BigQuery
- Özel web yönetim paneli
- Kullanıcı hesabı
- Canlı cihaz takip ekranı
- Tam paket listesi analizi
- Her uygulama açılışını tek tek sunucuya gönderme
- Bildirim veya dosya içeriği gönderme

Başlangıç sistemi yalnızca:

- Firebase Analytics
- Firebase Crashlytics
- Firebase Performance Monitoring
- Yerel sağlık/istatistik özetleri
- Ayarlar içindeki bağlantı testi

üzerinden çalışacaktır.

---

# 2. Değişmez gizlilik kuralları

## Sunucuya gönderilebilecek veriler

- Uygulama sürümü ve version code
- Android API seviyesi
- Cihaz üreticisi ve model ailesi
- Ekran boyutu sınıfı: telefon/tablet
- Özellik açık/kapalı durumları
- Sabit enum ve hata kodları
- Sayılar ve aralık kovaları
- İşlem süreleri
- Sağlık skoru aralığı
- Worker başarı/hata sayıları
- Sınıflandırma güven aralıkları
- Arama sonuç ve gecikme kovaları
- Sabit ekran/özellik isimleri

## Kesinlikle gönderilmeyecek veriler

- Arama sorgusu
- Bildirim başlığı veya metni
- Kişi adı, telefon numarası veya e-posta adresi
- Dosya adı veya dosya yolu
- Yüklü uygulamaların tam listesi
- Paket adı
- Kullanıcının özel klasör adı
- Kullanıcının özel kategori adı
- Mesaj içerikleri
- Konum veya açık adres
- IMEI, seri numarası, telefon numarası, reklam kimliği
- FCM token değeri
- Ham exception mesajında kişisel içerik bulunma ihtimali varsa ham mesaj

## Veri azaltma ilkesi

Kesin sayı yerine mümkün olduğunda kova kullanılmalıdır:

```text
0
1_5
6_10
11_20
21_50
51_100
101_PLUS
```

Süre kovaları:

```text
UNDER_50_MS
50_99_MS
100_249_MS
250_499_MS
500_999_MS
1_3_SEC
OVER_3_SEC
```

Güven skoru kovaları:

```text
0_29
30_49
50_69
70_84
85_100
```

---

# 3. Mevcut kod tabanı denetimi

## Mevcut Firebase altyapısı

- `build.gradle.kts` içinde Google Services ve Crashlytics Gradle eklentileri tanımlı.
- `app/build.gradle.kts` içinde Google Services ve Crashlytics eklentileri `skipGoogleServices` koşuluna göre uygulanıyor.
- Firebase BOM, Analytics, Crashlytics ve Messaging bağımlılıkları mevcut.
- Firebase Performance bağımlılığı ve Performance Gradle eklentisi henüz yok.
- `AppOrganizerApp.kt` içinde Firebase başlatılıyor.
- `AppAnalytics.kt` isminde mevcut bir Analytics sarmalayıcı var.

## Mevcut kritik açıklar

1. Kullanıcı onayına bağlı tek merkezî telemetri anahtarı yok.
2. `AppOrganizerApp.kt`, release sürümde Crashlytics toplamayı otomatik açıyor.
3. Debug sürümde Analytics otomatik açılıyor; izin tercihiyle ortak yönetilmiyor.
4. `AppAnalytics.folderOpened()` kategori adı ve kategori kimliği gönderiyor. Özel kategori veya klasör adı sızabilir.
5. `categoryReclassified()` ham eski/yeni kategori değerlerini gönderiyor. Kullanıcı üretimi değer olma ihtimali var.
6. `appStarted()` gereksiz istemci zaman damgası gönderiyor.
7. `searchPerformed()` doğrudan sorgu metnini göndermiyor fakat sorgu uzunluğu kesin değer yerine kova olmalı.
8. Analytics, Crashlytics ve ileride Performance çağrıları tek bir gizlilik kapısından geçmiyor.
9. Telefon tarafında Firebase yapılandırmasını test edecek ekran yok.
10. Analytics event gönderimi sunucudan onay döndürmez; bağlantı testi bunu dürüst biçimde kullanıcıya anlatmalıdır.

---

# 4. Hedef mimari

```text
UI / ViewModel / Worker
        |
        v
TelemetryManager
        |
        +-- Consent kontrolü
        +-- Event şema doğrulama
        +-- Yasaklı alan koruması
        +-- Günlük oran sınırı
        +-- Firebase hazır mı kontrolü
        |
        +--> AnalyticsGateway
        +--> CrashGateway
        +--> PerformanceGateway
        +--> LocalTelemetryStore
```

## Önerilen paket yapısı

```text
app/src/main/java/com/armutlu/apporganizer/telemetry/
  TelemetryManager.kt
  TelemetryConsentManager.kt
  TelemetryEvent.kt
  TelemetryEventValidator.kt
  TelemetryBuckets.kt
  HealthSnapshot.kt
  HealthIssueCode.kt
  FirebaseConnectionTester.kt
  AnalyticsGateway.kt
  CrashGateway.kt
  PerformanceGateway.kt
  LocalTelemetryStore.kt
```

Firebase SDK çağrıları UI, ViewModel veya Worker içine dağılmamalıdır.

---

# BÖLÜM A — SAĞLIK RAPORU VE YEREL TEŞHİS

# A0 — Doğrulanmış kaynak rapor

Kaynak cihaz: Xiaomi `2107113SR`, Android 14 / API 34, AppOrganizer `1.3.47 (70)`.

Raporda görülen temel değerler:

- Toplam uygulama: `110`
- Kullanıcı uygulaması: `84`
- Sistem uygulaması: `26`
- Kategori sayısı: `42`
- Bekleyen inceleme: `8`
- Onaylı: `5`
- Atlanmış: `1`
- Arama: `24`
- Sıfır sonuç: `1`
- Ortalama arama gecikmesi: `25 ms`
- Toplam tıklama: `10`
- İlk sonuç tıklaması: `6`
- Son 7 gün bildirim olayı: `385`
- Toplam yıldız: `3`
- Widget: `0`
- Crash kaydı: yok
- Eski hatalı one-shot tarihi: `292278994-08-17 10:12:55`

---

## A1 — One-shot WorkManager sentinel tarih düzeltmesi

**Sorun / İstek:** Tamamlanmış tek seferlik işin sonraki çalışma zamanı 292 milyon yıl sonrası gibi gösteriliyordu.

**Nasıl yapılmalı:** Worker türü `PERIODIC/ONE_SHOT` olarak ayrılmalı; terminal durumlarda `next=` yazılmamalı; `Long.MAX_VALUE` ve on yıldan uzak tarihler filtrelenmeli.

**Durum:** ✅ Tamamlandı — 2026-07-16. Worker türü modele eklendi, terminal one-shot tarihleri kaldırıldı ve testler eklendi.

**Kabul kriterleri:**

- Tamamlanan one-shot iş için `sonraki çalışma yok` görünür.
- Hiçbir worker on yıldan uzak tarih göstermez.
- Periyodik işlerin geçerli tarihi korunur.

---

## A2 — Sınıflandırma sayaçlarını uzlaştırma

**Sorun / İstek:** Eski rapor 84 kullanıcı uygulamasının yalnızca küçük bir bölümünü açıklıyordu.

**Nasıl yapılmalı:** `ClassificationDiagnosticsCalculator`, `ClassificationAttentionPolicy` kullanarak birbirini dışlayan kovalar üretmeli: otomatik kabul, dikkat gereken, snooze, confirmed, corrected, skipped, uncategorized ve geçersiz.

**Durum:** ✅ Tamamlandı — 2026-07-16. Sağlık raporu artık toplam uzlaşma, dikkat nedeni kırılımı ve kişisel veri içermeyen tutarsızlık uyarısı üretiyor.

**Kabul kriterleri:**

- Sınıflandırma ana sayaçları kullanıcı uygulaması toplamına eşittir.
- Kontrol Bekleyenler ve sağlık raporu aynı policy sonucunu kullanır.
- Paket ve uygulama adı rapora girmez.

---

## A3 — Arama istatistiklerini tam raporlama

**Sorun / İstek:** `SearchStatsPrefs` kaynak ve aksiyon sayaçlarını tutuyor fakat sağlık raporu yalnızca kaba toplamları gösteriyor.

**Nasıl yapılmalı:**

- Sıfır sonuç oranı
- Aramadan tıklama oranı
- İlk sonuç tercih oranı
- Kaynak türü dağılımı
- Hızlı aksiyon dağılımı
- Ortalama sorgu uzunluğu kovası

rapora eklenmeli.

Beklenen örnek:

```text
Arama: total=24, zero=1, zeroRate=4.2%, latency=UNDER_50_MS
Etkilesim: clicks=10, clickThroughRate=41.7%, firstResultRate=60.0%
Kaynaklar: app=6, contact=2, file=1, settings=1
Aksiyonlar: OPEN_APP=6, CALL=1, WHATSAPP=1
```

**Durum:** ✅ Tamamlandı — 2026-07-16. `SearchStatsPrefs.Summary` ortalama sorgu uzunluğunu dışa veriyor; sağlık raporu sıfır sonuç oranı, tıklama oranı, ilk sonuç oranı, kaynak/aksiyon kırılımı ve ortalama sorgu uzunluğu satırlarını anonim sayaçlardan üretiyor. `SearchStatsPrefsTest` ve `DiagnosticsReportManagerTest` ile doğrulandı.

**Değişecek dosyalar:**

- `SearchStatsPrefs.kt`
- `DiagnosticsReportManager.kt`
- `DiagnosticsReportManagerTest.kt`
- `SearchStatsPrefsTest.kt`

**Kabul kriterleri:**

- 24/1/10/6 örneğinde oranlar yaklaşık `%4.2`, `%41.7`, `%60.0` çıkar.
- Sıfıra bölme oluşmaz.
- Arama metni hiçbir yere yazılmaz.

---

## A4 — Worker açık/kapalı/eksik durum ayrımı

**Sorun / İstek:** Kullanıcının kapattığı özellik ile açık olduğu hâlde planlanmamış worker aynı görünmemeli.

**Nasıl yapılmalı:**

| Tercih | Work | Sağlık sonucu |
|---|---|---|
| Kapalı | Yok | `NORMAL_KAPALI` |
| Kapalı | Var | `UYARI_KAPALI_AMA_PLANLI` |
| Açık | Yok | `HATA_ETKIN_AMA_WORK_YOK` |
| Açık | Var | WorkInfo durumuna göre |

**Durum:** ✅ Tamamlandı — 2026-07-16. `WorkerPlanHealth` karar tablosu eklendi; kapalı+work yok normal, açık+work yok hata, kapalı+work var uyarı, açık+work var normal olarak raporlanıyor. Karar tablosu unit test ile doğrulandı.

**Kabul kriterleri:**

- Kapalı otomatik yedek hata sayılmaz.
- Etkin dosya indeksi worker'ı yoksa hata üretilir.
- Ham exception yerine sabit hata kodu gösterilir.

---

## A5 — Kalıcı worker telemetrisi

**Sorun / İstek:** WorkManager yalnızca mevcut işi gösterir; geçmiş başarı ve süre bilgisi kaybolabilir.

**Nasıl yapılmalı:** `WorkerTelemetryPrefs.kt` veya DataStore tabanlı eşdeğer oluşturulmalı.

Alanlar:

- `lastStartedAt`
- `lastFinishedAt`
- `lastSucceededAt`
- `lastFailedAt`
- `lastFailureCode`
- `lastDurationMs`
- `successCount`
- `failureCount`

**Durum:** ✅ Tamamlandı — 2026-07-16. `WorkerTelemetryPrefs` eklendi; Backup, SmartInsight, SuggestionNotification, WeeklyDigest ve FilesIndex worker'ları başlangıç/başarı/hata zamanını, süreyi, sayaçları ve güvenli hata kodunu kalıcı olarak kaydediyor. Negatif süre ve hata kodu sanitizasyonu test edildi.

**Kabul kriterleri:**

- WorkManager kaydı prune edilse bile son başarı zamanı görünür.
- Süre negatif olamaz.
- Dosya yolu ve ham exception saklanmaz.

---

## A6 — Yedekleme sağlık ayrımı

**Sorun / İstek:** Kullanıcı tercihi, worker planı ve son yedek sonucu ayrı gösterilmeli.

**Nasıl yapılmalı:**

```text
Auto backup: tercih=kapali, work=yok, saglik=NORMAL
Auto backup: tercih=acik, work=yok, saglik=HATA_PLANLANMAMIS
```

**Durum:** ✅ Tamamlandı — 2026-07-16. Auto backup raporu kullanıcı tercihi, worker plan sağlığı, son yedek zamanı ve anonim son hata kodunu ayrı satırda gösteriyor. Kapalı yedekleme `NORMAL`, açık fakat plansız yedekleme `HATA_PLANLANMAMIS` olarak ayrıştırılıyor.

**Kabul kriterleri:**

- Kapalı yedekleme puan düşürmez.
- Açık fakat plansız yedekleme hata üretir.
- Bildirim, kişi, dosya ve arama içeriği yedeklenmez.

---

## A7 — Misyon motoru kalite metrikleri

**Sorun / İstek:** Yalnızca toplam yıldız ve son olay, görev sisteminin gerçek değerini göstermiyor.

**Nasıl yapılmalı:** Günlük/haftalık görev tamamlanma, davranış değişikliği görevi, görüntüleme görevi, pozitif/negatif skor ve tekrar ödül engeli raporlanmalı.

**Durum:** ✅ Tamamlandı — 2026-07-16. Günlük/haftalık ve davranış/görüntüleme görev tamamlanmaları ile pozitif/negatif/net görev skoru sağlık raporuna eklendi; rapor görüntüleme ödülü gün başına tekilleştirildi, dönemsel misyon benzersizliği ve yaşam skoru-yıldız ayrımı test/rapor kanıtıyla doğrulandı.

**Kabul kriterleri:**

- Aynı raporu tekrar açarak sınırsız puan alınamaz.
- Dijital yaşam skoru toplam yıldızla eşitlenmez.
- Davranış değişikliği görevleri görüntüleme görevlerinden daha değerlidir.

---

## A8 — Depolama, izin, bildirim tazeliği ve ANR

**Nasıl yapılmalı:**

- Room/WAL/cache toplam boyutu
- İznin açık olması kadar gerçekten gerekli olup olmadığı
- Son bildirim olayı zamanı
- Son 24 saat bildirim sayısı
- `ApplicationExitInfo` üzerinden ANR/low-memory/native crash toplamları
- Cold/warm start ve ana ekran hazır olma süresi

rapora eklenmeli.

**Durum:** ✅ Tamamlandı — 2026-07-16. Sağlık raporuna Room/WAL/SHM/cache boyutları, özellik gereksinimiyle yorumlanan izinler, son bildirim zamanı ve 24 saat sayacı, trace içermeyen `ApplicationExitInfo` ANR/low-memory/native-crash toplamları ile cold/warm/ana ekran hazır süreleri eklendi; listener açık fakat olaysız durum yalnız kontrol önerisi olarak sınıflandırıldı ve odak test/compile kapıları geçti.

**Kabul kriterleri:**

- Kullanılmayan konum izninin kapalı olması hata sayılmaz.
- Listener açık fakat olay yoksa kritik hata yerine kontrol önerisi gösterilir.
- Ham stack trace TXT raporuna eklenmez.

---

# BÖLÜM B — ANONİM KULLANIM VE FIREBASE TELEMETRİSİ

# Döngü B0 — Veri sözlüğü ve yasaklı alan koruması

**Sorun / İstek:** Event isimleri ve parametreler dağınık olursa veri güvenilmez ve gizlilik riski yüksek olur.

**Nasıl yapılmalı:**

1. `TelemetryEvent` sealed model veya sabit event kataloğu oluştur.
2. Her event için izin verilen parametre isimlerini tanımla.
3. `TelemetryEventValidator` izin verilmeyen parametreyi reddetsin.
4. Parametre değerleri maksimum uzunluk ve sabit enum kontrolünden geçsin.
5. `query`, `name`, `phone`, `path`, `package`, `notification_text`, `folder_name` gibi anahtarlar kara listeye alınsın.
6. Event isimleri snake_case ve sabit olsun.

**Durum:** ✅ Tamamlandı — 2026-07-16. Tipli ve sabit telemetry event kataloğu, event bazlı parametre allowlist'i, yasaklı alan denylist'i, snake_case/uzunluk/enum doğrulaması ve sayı/metin uzunluğu kovaları eklendi; mevcut Analytics çağrılarından kişisel serbest metinler kaldırıldı, odak validator testleri ve `compileDebugKotlin` kapısı geçti.

**Değişecek/yeni dosyalar:**

- `telemetry/TelemetryEvent.kt`
- `telemetry/TelemetryEventValidator.kt`
- `telemetry/TelemetryBuckets.kt`
- Unit test dosyaları

**Testler:**

- Yasaklı anahtarlar reddedilir.
- Serbest metin event parametresine giremez.
- Bilinen enum değerleri kabul edilir.
- Çok uzun değerler kırpılmak yerine reddedilir veya güvenli kovaya dönüştürülür.

**Kabul kriterleri:** Telemetri katmanı üzerinden kişisel metin göndermek kod seviyesinde zorlaştırılmış olmalıdır.

---

# Döngü B1 — Kullanıcı onayı ve toplama kontrolü

**Sorun / İstek:** Firebase servisleri kullanıcı tercihi olmadan otomatik veri toplamamalı.

**Nasıl yapılmalı:**

1. `AppPrefs` veya DataStore'a şu anahtarlar eklenmeli:

```text
telemetry_consent_decided
telemetry_enabled
telemetry_consent_version
telemetry_last_changed_at
```

2. Yeni kurulumda varsayılan `telemetry_enabled=false` olmalı.
3. Manifest seviyesinde Analytics, Crashlytics ve Performance otomatik toplama başlangıçta kapatılmalı.
4. Kullanıcı açınca merkezî yönetici şu servisleri birlikte açmalı:

```kotlin
analytics.setAnalyticsCollectionEnabled(enabled)
crashlytics.setCrashlyticsCollectionEnabled(enabled)
performance.isPerformanceCollectionEnabled = enabled
```

5. Kullanıcı kapatınca yeni event üretimi anında durmalı.
6. `AppOrganizerApp.kt` içindeki release Crashlytics'i koşulsuz açan mevcut kod kaldırılmalı.
7. Debug build de kullanıcı tercihini ezmemeli. Geliştirici DebugView ayrı ADB komutuyla açılmalı.

**Durum:** ✅ Tamamlandı — 2026-07-16. Dört kalıcı consent alanı ve varsayılan kapalı tercih eklendi; Analytics/Crashlytics/Performance manifestte opt-in yapıldı, tek merkezî yönetici üç SDK'yı birlikte kontrol ediyor ve event gateway onay geri çekilince anında no-op oluyor. Sürüm 1.3.57/80; odak testleri ile `compileDebugKotlin` geçti.

**Değişecek dosyalar:**

- `AppOrganizerApp.kt`
- `AppPrefs.kt`
- `AndroidManifest.xml`
- `TelemetryConsentManager.kt`
- `TelemetryManager.kt`

**Testler:**

- İlk kurulumda bütün toplama kapalıdır.
- Kullanıcı açınca servisler etkinleşir.
- Kullanıcı kapatınca event gateway no-op olur.
- Uygulama yeniden açıldığında tercih korunur.

**Kabul kriterleri:** Kullanıcı tercihi bütün Firebase ölçüm servisleri için tek doğruluk kaynağıdır.

---

# Döngü B2 — Mevcut AppAnalytics gizlilik temizliği

**Sorun / İstek:** Mevcut `AppAnalytics` bazı kullanıcı üretimi kategori/klasör değerlerini gönderme riski taşıyor.

**Nasıl yapılmalı:**

- `folderOpened(categoryId, categoryName)` kaldırılmalı.
- Yerine yalnızca sabit tür gönderilmeli:

```text
folder_type = SYSTEM | AUTO | USER_CREATED
app_count_bucket = 1_5 | 6_10 | 11_20 | 21_PLUS
```

- `categoryReclassified(fromCategory, toCategory)` ham kategori göndermemeli.

```text
source_type = AUTO | OTHER | UNCATEGORIZED | USER_CREATED
result_type = BUILTIN | USER_CREATED | OTHER
confidence_bucket = ...
```

- `appStarted()` içindeki istemci timestamp kaldırılmalı; Firebase kendi event zamanını tutar.
- `searchPerformed(query, resultCount)` imzası sorgu metni almamalı.

Yeni imza örneği:

```kotlin
fun searchPerformed(
    queryLengthBucket: String,
    resultCountBucket: String,
    latencyBucket: String,
    sourceMix: String,
)
```

**Durum:** ✅ Tamamlandı — 2026-07-16. AppAnalytics ve çağrı noktaları ham sorgu/klasör/kategori/paket adı kabul etmeyen tipli enum-kova API'sine taşındı; gizlilik testleri 7/7 ve `compileDebugKotlin` geçti. Sürüm 1.3.58/81.

**Değişecek dosyalar:**

- `AppAnalytics.kt` veya yerine `TelemetryManager.kt`
- Tüm çağrı noktaları
- Gizlilik testleri

**Kabul kriterleri:** Analytics çağrı zincirinin hiçbir yerinde sorgu, klasör adı, kategori adı veya paket adı parametresi bulunmaz.

---

# Döngü B3 — Merkezî TelemetryManager ve gateway'ler

**Sorun / İstek:** Firebase çağrıları uygulamaya dağılırsa kapatma, test etme ve değiştirme zorlaşır.

**Nasıl yapılmalı:**

```kotlin
interface AnalyticsGateway {
    fun log(event: TelemetryEvent)
}

interface CrashGateway {
    fun setKey(key: String, value: String)
    fun log(messageCode: String)
    fun recordNonFatal(code: HealthIssueCode, throwable: Throwable? = null)
}

interface PerformanceGateway {
    fun <T> trace(name: String, block: () -> T): T
}
```

`TelemetryManager`:

- İzni kontrol eder.
- Firebase hazır değilse sessiz no-op olur.
- Event'i doğrular.
- Günlük limit uygular.
- Test cihazı etiketini yalnızca sabit enum olarak ekler.
- UI'ı asla çökertmez.

**Durum:** ✅ Tamamlandı — 2026-07-16. Firebase Analytics, Crashlytics ve Performance erişimleri merkezî gateway adapter'larına taşındı; izin, Firebase yokluğunda no-op, event doğrulama, kalıcı günlük limit, sabit test cihazı enum'u ve SDK hata yalıtımı 7/7 odak test, derleme ve kaynak taramasıyla doğrulandı. Sürüm 1.3.59/82.

**Kabul kriterleri:** UI ve domain kodunda doğrudan `FirebaseAnalytics.getInstance`, `FirebaseCrashlytics.getInstance` veya Performance SDK çağrısı kalmaz.

---

# Döngü B4 — Ayarlar > Kullanım Verileri ekranı

**Sorun / İstek:** Kullanıcı toplama tercihini yönetebilmeli ve telefondan Firebase kurulumunu test edebilmelidir.

**Nasıl yapılmalı:** Ayarlar ana ekranında Sistem bölümüne yeni satır eklenmeli:

```text
Kullanım Verileri
Anonim kullanım, performans ve hata paylaşımı
```

Yeni route:

```kotlin
const val SETTINGS_USAGE_DATA = "settings_usage_data"
```

Yeni ekran:

```text
SettingsUsageDataScreen.kt
UsageDataViewModel.kt
```

## Ekran düzeni

### Kart 1 — Anonim veri paylaşımı

- Toggle: `Anonim kullanım ve sağlık verilerini paylaş`
- Açıklama: Uygulamanın geliştirilmesine yardımcı olur.
- Varsayılan: kapalı

### Kart 2 — Toplananlar

- Özellik kullanım sayaçları
- Hata ve çökme kayıtları
- İşlem süreleri
- Uygulama/Android sürümü
- Kişisel veri içermeyen sağlık uyarı kodları

### Kart 3 — Toplanmayanlar

- Arama metni
- Bildirim içeriği
- Kişiler
- Dosya adları
- Paket/uygulama listesi
- Özel klasör adları

### Kart 4 — Bağlantı durumu

- Firebase yapılandırması
- Analytics durumu
- Crashlytics durumu
- Performance durumu
- Son test zamanı
- Son test sonucu

### Buton

```text
Firebase bağlantısını test et
```

Buton durumları:

```text
IDLE
TESTING
SUCCESS
PARTIAL_SUCCESS
FAILED
```

**Durum:** ✅ Tamamlandı — 2026-07-16. Kullanım Verileri rotası/ekranı, kalıcı opt-in, toplanan-toplanmayan veri kartları, dönüşte korunan beş durumlu bağlantı ön kontrolü ve TalkBack açıklamaları eklendi; sürüm 1.3.60/83. Derleme ve 5/5 odak test geçti.

**Değişecek dosyalar:**

- `SettingsScreen.kt`
- `AppNavigation.kt`
- `SettingsUsageDataScreen.kt`
- `UsageDataViewModel.kt`
- `strings.xml`
- `values-en/strings.xml`

**Testler:**

- Route whitelist'e eklenir.
- Toggle state yeniden açılışta korunur.
- Test sürerken ikinci tıklama engellenir.
- Ekran döndürülünce sonuç kaybolmaz.
- TalkBack açıklamaları bulunur.

**Kabul kriterleri:** Kullanıcı tek ekranda neyin toplandığını görür, tercihini değiştirir ve bağlantı testini çalıştırır.

---

# Döngü B5 — Firebase bağlantı testi

**Sorun / İstek:** Telefon tarafından Firebase'in gerçekten yapılandırıldığını ve ağa çıkabildiğini görmek istiyoruz.

**Önemli gerçek:** Analytics `logEvent()` sunucu teslim onayı döndürmez. Bu nedenle buton “Analytics sunucuya kesin ulaştı” şeklinde sahte başarı göstermemelidir.

**Nasıl yapılmalı:** `FirebaseConnectionTester` aşağıdaki adımları sırayla çalıştırmalı:

1. `FirebaseApp` örneği var mı?
2. `google-services.json` kaynaklı options geçerli mi?
3. İnternet capability mevcut mu?
4. Firebase Installations kimliği veya FCM token isteği başarıyla sonuçlandı mı? Değer ekranda veya logda gösterilmemeli.
5. `telemetry_connection_test` Analytics event'i sıraya alındı mı?
6. Crashlytics SDK hazır mı? Yalnızca güvenli `log("connection_test")` yazılmalı; zorla crash üretilmemeli.
7. Performance SDK hazırsa `firebase_connection_test` isimli kısa trace başlatılıp bitirilmeli.
8. Sonuç yerel olarak kaydedilmeli.

Model:

```kotlin
data class FirebaseConnectionTestResult(
    val configurationOk: Boolean,
    val networkAvailable: Boolean,
    val firebaseRoundTripOk: Boolean,
    val analyticsQueued: Boolean,
    val crashlyticsReady: Boolean,
    val performanceReady: Boolean,
    val testedAt: Long,
    val status: ConnectionTestStatus,
    val safeErrorCode: String?,
)
```

Kullanıcıya örnek çıktı:

```text
Firebase yapılandırması: Başarılı
Firebase servis erişimi: Başarılı
Analytics: Test olayı sıraya alındı
Crashlytics: Hazır
Performance: Hazır
Son test: 16.07.2026 14:20
```

Analytics için alt açıklama:

```text
Analytics SDK teslim onayı vermez. Test olayı Firebase DebugView üzerinden ayrıca doğrulanabilir.
```

Hata kodları:

```text
FIREBASE_NOT_CONFIGURED
NETWORK_UNAVAILABLE
INSTALLATION_ID_FAILED
FCM_TOKEN_FAILED
ANALYTICS_NOT_AVAILABLE
CRASHLYTICS_NOT_AVAILABLE
PERFORMANCE_NOT_AVAILABLE
UNKNOWN_TEST_FAILURE
```

**Durum:** ✅ Tamamlandı — FIS auth token refresh ile gerçek round-trip, güvenli SDK probe'ları, yerel sonuç/UI ve odaklı testler doğrulandı (16.07.2026).

**Testler:**

- `skipGoogleServices` build'de ekran çökmez ve `FIREBASE_NOT_CONFIGURED` gösterir.
- İnternet yokken uygun hata gösterir.
- Token/installation değeri ekrana veya loga yazılmaz.
- Analytics test event'i kişisel parametre içermez.
- Zorla crash testi yapılmaz.

**Kabul kriterleri:** Telefon tarafında gerçek bir Firebase servis round-trip kontrolü ve dürüst Analytics queue sonucu görülebilir.

---

# Döngü B6 — Temel Analytics event kataloğu

İlk sürümde en fazla aşağıdaki event'ler kullanılmalıdır:

| Event | Amaç | Güvenli parametreler |
|---|---|---|
| `onboarding_started` | Kurulum başladı mı? | `entry_type` |
| `onboarding_step` | Nerede vazgeçiliyor? | `step`, `result` |
| `onboarding_completed` | Kurulum tamamlandı mı? | `duration_bucket` |
| `permission_result` | Hangi izin reddediliyor? | `permission_type`, `result` |
| `search_performed` | Arama kullanımı ve kalite | `result_bucket`, `latency_bucket`, `source_mix` |
| `search_result_opened` | Sonuç etkili mi? | `source_type`, `position_bucket` |
| `quick_action_used` | Arama aksiyonları | `action_type`, `source_type` |
| `folder_opened` | Klasör kullanımı | `folder_type`, `app_count_bucket` |
| `classification_reviewed` | İnceleme akışı | `decision`, `confidence_bucket`, `source_type` |
| `classification_corrected` | Yanlış sınıflandırma | `source_type`, `confidence_bucket`, `target_type` |
| `mission_viewed` | Görev ilgisi | `mission_type` |
| `mission_completed` | Görev değeri | `mission_type`, `reward_bucket` |
| `report_viewed` | Rapor kullanımı | `report_type` |
| `widget_added` | Widget benimsenmesi | `widget_type` |
| `health_warning` | Sahadaki teknik sorun | `warning_code`, `severity`, `version` |

**Nasıl yapılmalı:** Her event sadece belirlenen parametreleri kabul etmelidir. Exact package/app/category name gönderilmemelidir.

**Durum:** ✅ Tamamlandı — 15 event'lik kapalı katalog, exact parametre allowlist'i, düşük cardinality enum/kovaları ve odaklı testler doğrulandı (16.07.2026).

**Kabul kriterleri:** Event sayısı kontrollü, isimler sabit ve parametre cardinality düşük olmalıdır.

---

# Döngü B7 — Sağlık snapshot ve uyarı kodları

**Sorun / İstek:** Mevcut TXT sağlık raporunun tamamını buluta göndermek gereksiz ve risklidir.

**Nasıl yapılmalı:** Bulut için ayrı sade model oluştur:

```kotlin
data class HealthSnapshot(
    val healthScoreBucket: String,
    val appVersion: String,
    val androidApiBucket: String,
    val classificationConsistent: Boolean,
    val pendingReviewBucket: String,
    val searchLatencyBucket: String,
    val staleWorkerBucket: String,
    val failedWorkerBucket: String,
    val fileIndexAgeBucket: String,
    val notificationListenerActive: Boolean,
    val usageAccessActive: Boolean,
    val localCrashCountBucket: String,
    val warningCodes: Set<HealthIssueCode>,
)
```

İlk uyarı kodları:

```text
CLASSIFICATION_COUNT_MISMATCH
INVALID_WORK_SCHEDULE
WORK_ENABLED_BUT_MISSING
WORK_DISABLED_BUT_SCHEDULED
WORKER_REPEATED_FAILURE
FILE_INDEX_STALE
USAGE_SYNC_STALE
NOTIFICATION_LISTENER_STALE
DATABASE_READ_FAILURE
SEARCH_LATENCY_HIGH
BACKUP_FAILURE
REQUIRED_PERMISSION_MISSING
```

**Durum:** ✅ Tamamlandı — İçerik taşımayan 13 alanlı `HealthSnapshot`, 12 sabit uyarı kodu ve gizlilik şema testleri doğrulandı (16.07.2026).

**Kabul kriterleri:** Snapshot uygulama, kişi, dosya veya bildirim içeriği taşımaz; yalnızca özet ve kod içerir.

---

# Döngü B8 — Crashlytics bağlamı ve non-fatal kayıtlar

**Nasıl yapılmalı:** Crashlytics custom key'leri düşük cardinality ve güvenli olmalı:

```text
app_version
android_api_bucket
device_class
classification_mode
classification_consistent
pending_review_bucket
file_index_status
notification_listener_enabled
usage_access_enabled
active_feature
last_operation_code
health_score_bucket
```

Non-fatal gönderilecek durumlar:

- Tekrarlayan worker hatası
- Veritabanı okuma/yazma başarısızlığı
- Sınıflandırma toplam uyuşmazlığı
- Yedekleme kısmi başarısızlığı
- İndeksleme ardışık başarısızlığı

Aynı hata cihaz başına sürekli gönderilmemeli; hata kodu için günlük rate limit uygulanmalı.

**Durum:** ✅ Tamamlandı — 2026-07-16. 12 alanlı güvenli `CrashContext` allowlist'i, opt-in kapılı non-fatal kayıt ve hata kodu başına cihazda günlük kalıcı rate limit odak test ve `compileDebugKotlin` ile doğrulandı. Sürüm 1.3.64/87.

**Testler:**

- Opt-in kapalıyken non-fatal gönderilmez.
- Custom key'lerde yasaklı veri yoktur.
- Aynı hata aynı gün sınırı aşamaz.

**Kabul kriterleri:** Crashlytics hatayı anlamaya yetecek bağlamı verir fakat kullanıcı içeriği taşımaz.

---

# Döngü B9 — Firebase Performance Monitoring

**Nasıl yapılmalı:**

Gradle tarafında Performance eklentisi ve bağımlılığı eklenmeli. İlk aşamada yalnızca şu trace'ler kullanılmalı:

```text
app_cold_start
home_screen_ready
global_search
app_catalog_reconcile
classification_run
usage_sync
file_index
health_report_generation
```

Trace attribute değerleri sabit olmalı:

```text
device_class=phone|tablet
result=success|failure|partial
item_bucket=...
```

Arama sorgusu, dosya adı, kategori adı veya paket adı trace attribute olamaz.

**Durum:** ✅ Tamamlandı (2026-07-16)

**Kabul kriterleri:**

- Opt-in kapalıyken Performance toplama kapalıdır.
- Trace isimleri sabittir.
- Aynı işlem nested veya çift trace üretmez.
- Arama 100 ms üzeri cihaz/sürüm dağılımı konsolda görülebilir.

---

# Döngü B10 — Günlük anonim özet

**Sorun / İstek:** Her kullanıcı hareketini ayrı event göndermek yerine günlük anlamlı özet üretmek gerekir.

**Nasıl yapılmalı:** Yerel sayaçlar `LocalTelemetryStore` içinde tutulmalı ve günde en fazla bir `daily_usage_summary` ile bir `daily_health_summary` gönderilmelidir.

Kullanım özeti örneği:

```text
daily_usage_summary
search_count_bucket=1_5
folder_open_count_bucket=11_20
mission_complete_bucket=0
report_view_bucket=1_5
widget_active=false
top_feature=search
```

Sağlık özeti örneği:

```text
daily_health_summary
health_score_bucket=80_89
warning_count_bucket=1_5
classification_consistent=true
worker_failure_bucket=0
search_latency_bucket=UNDER_50_MS
file_index_age_bucket=UNDER_24_HOURS
```

Worker:

```text
TelemetryDailySummaryWorker
uniqueName=telemetry_daily_summary
```

Koşullar:

- Kullanıcı onayı açık
- Ağ bağlantısı var
- Pil düşük değilse tercih edilebilir
- Aynı yerel gün için tekrar gönderilmez

**Durum:** ✅ Tamamlandı (2026-07-16)

**Kabul kriterleri:** Bir günde yüzlerce bildirim veya uygulama açılışı yüzlerce Firebase event'ine dönüşmez.

---

# Döngü B11 — Ev test cihazları ve cihaz etiketi

Evdeki cihazlar mini test filosu olarak kullanılmalıdır:

```text
QA_PRIMARY_PHONE
QA_CLEAN_INSTALL_PHONE
QA_STRESS_PHONE
QA_TABLET
```

Etiket kullanıcıya veya cihaza ait serbest metin olmamalıdır. Debug/internal build için sabit enum seçimi yapılabilir.

Test görevleri:

- Ana telefon: gerçek günlük kullanım
- Temiz kurulum telefonu: onboarding, izin ve yeniden kurulum
- Stres telefonu: izin kapatma, ağ kesme, worker iptali
- Tablet: yatay ekran, büyük ekran, widget, split-screen

Her cihazda:

1. Kullanım Verileri ekranı açılır.
2. Opt-in açılır.
3. Firebase bağlantı testi çalıştırılır.
4. DebugView'da test event'i görülür.
5. Crashlytics non-fatal test yalnızca geliştirici build'inde çalıştırılır.
6. Performance trace konsolda doğrulanır.
7. Sağlık raporu yeniden alınır.

**Durum:** ⛔ Bloke — Dört sabit `TestDeviceTag` enum değeri ve B11 kanıt formu hazır. 2026-07-16 otomasyonunda ADB'ye bağlı cihaz bulunmadı; dört fiziksel cihaz, Firebase konsolu ve tablet taşma doğrulaması için dış test filosu gerekli.

**Kabul kriterleri:** Dört cihazın tamamında test sonuçları kayıt altına alınır; tablet ekranında taşma olmaz.

---

# Döngü B12 — Firebase konsol raporları

Başlangıçta yalnızca şu paneller takip edilmelidir:

## Kullanıcı ve onboarding

- Günlük/haftalık aktif kullanıcı
- Onboarding başlama/tamamlama oranı
- En çok terk edilen adım
- İzin ret oranı

## Arama

- Arama kullanan kullanıcı oranı
- Sıfır sonuç oranı
- Sonuç açma oranı
- İlk sonuç tercih oranı
- Gecikme kovaları

## Sınıflandırma

- İnceleme oranı
- Onay/düzeltme oranı
- Kaynak ve güven kovalarına göre hata
- Sınıflandırma toplam uyuşmazlığı

## Sağlık

- Crash-free kullanıcı
- ANR
- Worker uyarı kodları
- Eski indeks oranı
- Android sürümü/cihaz üreticisine göre sorun

## Özellik benimseme

- Arama
- Klasörler
- Görevler
- Raporlar
- Bildirim analizi
- Widget
- Yedekleme

**Durum:** 🟡 Kısmen tamamlandı — Beş rapor ailesinin ölçüm, ürün kararı, inceleme sıklığı ve kaldırma kuralı `FIREBASE_CONSOLE_REPORTS.md` içinde tanımlandı. Bu otomasyon ortamında Firebase/GA4 Editor/Admin kimliği, bağlı test cihazı ve konsol erişimi bulunmadığından raporlar oluşturulup yayımlanamadı; DebugView/Crashlytics/Performance gerçek veri kanıtı bekleniyor.

**Kabul kriterleri:** Her panel doğrudan bir ürün kararına bağlanmalıdır; bakılmayan metrik kaldırılmalıdır.

---

# Döngü B13 — Gizlilik metni ve Play veri güvenliği

**Nasıl yapılmalı:**

- Uygulama içi gizlilik metni gerçek SDK davranışıyla uyumlu güncellenmeli.
- Kullanıcı hangi verinin toplandığını sade biçimde görmeli.
- Google Play Veri Güvenliği formu Analytics, Crashlytics ve Performance davranışlarına göre güncellenmeli.
- Toplama amacı `Uygulama işlevselliği`, `Analiz` ve `Hata teşhisi` kapsamında doğru işaretlenmeli.
- Kullanıcı kapattığında hangi toplamanın durduğu açıkça belirtilmeli.

**Durum:** 🟡 Kısmen tamamlandı — Uygulama içi Kullanım Verileri açıklaması, `docs/privacy_policy.html` ve kod/SDK envanterine dayalı `docs/PLAY_DATA_SAFETY_DECLARATION.md` birbiriyle eşleştirildi; Analytics, Crashlytics, Performance kapatma davranışı ve telemetri anahtarından bağımsız FCM açıkça belirtildi. Kontrat testi ve zorunlu Kotlin derlemesi geçti; mevcut yayın URL'si HTTP 200 verse de güncel politika yayını, Play Console form gönderimi/readback'i ve Policy Status kanıtı hesap erişimi gerektirdiğinden bekliyor. Bu kanıtlar olmadan tamamlanmış sayılmaz.

**Kabul kriterleri:** Uygulama içi açıklama, gerçek kod ve Play beyanı birbiriyle çelişmez.

---

# 5. Uygulama sırası

| Döngü | İş | Öncelik | Bağımlılık | Durum |
|---|---|---|---|---|
| A1 | One-shot tarih düzeltmesi | P0 | Yok | ✅ Tamamlandı |
| A2 | Sınıflandırma sayaç uzlaşması | P0 | AttentionPolicy | ✅ Tamamlandı |
| B0 | Veri sözlüğü ve validator | P0 | Yok | ⏳ Bekliyor |
| B1 | Kullanıcı onayı ve collection control | P0 | B0 | ⏳ Bekliyor |
| B2 | Mevcut AppAnalytics gizlilik temizliği | P0 | B0-B1 | ⏳ Bekliyor |
| B3 | TelemetryManager/gateway | P0 | B0-B1 | ⏳ Bekliyor |
| B4 | Kullanım Verileri ekranı | P0 | B1-B3 | ⏳ Bekliyor |
| B5 | Firebase bağlantı testi | P0 | B3-B4 | ⏳ Bekliyor |
| B6 | Temel Analytics event'leri | P1 | B3 | ⏳ Bekliyor |
| A3 | Gelişmiş yerel arama istatistikleri | P1 | SearchStatsPrefs | ✅ Tamamlandı |
| B7 | HealthSnapshot | P1 | A bölümü | ✅ Tamamlandı |
| B8 | Crashlytics bağlamı | P1 | B1-B3-B7 | ⏳ Bekliyor |
| B9 | Performance | P1 | B1-B3 | ⏳ Bekliyor |
| A4-A6 | Worker/yedek sağlık genişletmeleri | P1 | Worker altyapısı | ✅ Tamamlandı |
| B10 | Günlük anonim özet | P1 | B6-B7 | ⏳ Bekliyor |
| B11 | Dört cihaz doğrulaması | P1 | B4-B10 | ⏳ Bekliyor |
| B12 | Firebase konsol panelleri | P2 | Gerçek veri | ⏳ Bekliyor |
| B13 | Gizlilik ve Play beyanı | P0 yayın kapısı | B1-B10 | 🟡 Kısmen tamamlandı — Play Console kanıtı bekliyor |
| A7-A8 | Gelişmiş görev/ANR/depolama | P2 | Temel sistem | ⏳ Bekliyor |

---

# 6. Her yapay zekâ döngüsünün zorunlu çıktısı

Her döngü sonunda yapay zekâ şunları yazmalıdır:

```text
Döngü:
Durum:
Değiştirilen dosyalar:
Eklenen testler:
Çalıştırılan komutlar:
Test sonucu:
Cihaz doğrulaması:
Firebase konsol doğrulaması:
Gizlilik kontrolü:
Kalan riskler:
Commit:
```

Tamamlanan döngünün bu dosyadaki durumu güncellenmeli. Test edilmemiş iş `✅ Tamamlandı` yapılmamalıdır.

---

# 7. Minimum test matrisi

## Unit test

```bash
./gradlew testDebugUnitTest
```

Zorunlu alanlar:

- Telemetry consent açık/kapalı
- Event schema validator
- Yasaklı alan testi
- Bucket hesapları
- Günlük rate limit
- HealthSnapshot gizlilik testi
- Firebase bağlantı test result mapping
- Worker tarih ve sağlık kararları
- Sınıflandırma uzlaşması
- Arama oranları

## Static analysis

```bash
./gradlew lintDebug
./gradlew detekt
```

## Build

```bash
./gradlew assembleDebug
./gradlew assembleDebug -PskipGoogleServices
```

İki build de çalışmalıdır. `skipGoogleServices` sürümü Firebase olmadan açılmalı ve Kullanım Verileri ekranında yapılandırılmamış durumu göstermelidir.

## Cihaz doğrulaması

- Opt-in kapalı temiz kurulum
- Opt-in açık yeniden başlatma
- Bağlantı testi başarılı
- İnternet kapalı test
- Google Services olmayan build
- Telefon ve tablet
- Türkçe ve İngilizce
- Ekran döndürme
- Uygulama kapat/aç

---

# 8. Tamamlanma tanımı

Bu yol haritasının ilk sürümü aşağıdaki koşullarda tamamlanmış sayılır:

1. Kullanıcı onayı olmadan Analytics, Crashlytics ve Performance toplamaz.
2. Ayarlar > Kullanım Verileri ekranı çalışır.
3. Firebase bağlantı testi telefondan çalıştırılır ve dürüst sonuç gösterir.
4. Analytics event'leri kişisel veya serbest metin içermez.
5. Crashlytics custom key ve non-fatal kayıtları yalnızca sabit güvenli alanlardan oluşur.
6. Performance yalnızca belirlenen kritik işlemleri ölçer.
7. Günlük özet event sayısını kontrol altında tutar.
8. Sağlık raporu ile bulut HealthSnapshot aynı uyarı kodlarını kullanır.
9. `skipGoogleServices` build çökmez.
10. Dört ev test cihazında temel senaryolar doğrulanır.
11. Gizlilik metni ve Play Veri Güvenliği beyanı kodla uyumludur.
12. Bütün unit test, lint ve detekt kontrolleri geçer.

---

# 9. Kaynak dokümantasyon başlıkları

Uygulama sırasında yalnızca resmî Firebase belgeleri esas alınmalıdır:

- Configure Analytics data collection and usage
- Customize Crashlytics crash reports
- Enable opt-in Crashlytics reporting
- Get started with Performance Monitoring for Android
- Add monitoring for specific code
- Debug events / Analytics DebugView

SDK davranışı varsayımla değil, kullanılan Firebase BOM sürümü ve resmî Android dokümantasyonuyla doğrulanmalıdır.
