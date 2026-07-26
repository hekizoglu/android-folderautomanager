# Akıllı Bildirim Sistemi — Birleşik Doğrulama ve Merge Kapısı

> **Ana görev kaynağı:** `MANAGEMENT/AKILLI_BILDIRIM_SISTEMI_TASKS.md`  
> **Doğrulama branch'i:** `agent/smart-notification-validation`  
> **Birleşim commit'i:** `07d7e60b34dc515254969afdcd6ae63b53d77266`  
> **Main durumu:** Merge edilmedi. Hüseyin'in açık onayı bekleniyor.  
> **Kural:** Bu dosyadaki zorunlu kontroller tamamlanmadan PR hazır/merge edilebilir yapılmaz.

---

## 1. Bu Branch'in Kapsamı

Birleşik branch şu geliştirme zincirlerini birlikte taşır:

- On-device bildirim sınıflandırma ve önem politikası
- Tek aktif snapshot ve `SmartNotificationRepository`
- Okunmamış rozet modeli
- Room v23 → v24 içeriksiz metadata migration'ı
- `NotificationAnalyzer V2`
- Bildirim filtreleme feature flag ve preference çekirdeği
- Bildirim filtreleme ayar ekranı
- Notification Report V2 görünümü
- Room bildirim metni kalıcılığını engelleyen P0 gizlilik düzeltmesi
- Servis–repository entegrasyon testleri
- Sınıflandırıcı mikrobenchmarkları
- BackupManager V7 akıllı bildirim ayar export/import bağlantısı

Kaynak draft PR'lar: `#11`–`#20`.

---

## 2. Değiştirilmeyecek Ürün Kuralları

1. Android sistem bildirimi AppOrganizer tarafından silinmez veya sessize alınmaz.
2. Promosyon filtresi yalnız AppOrganizer rozeti, özeti ve rapor görünümünü etkiler.
3. Bildirim başlığı, gövdesi ve gönderen bilgisi Room'a, Firebase'e, loglara, tanılama raporuna veya yedeğe yazılmaz.
4. Kalıcı geçmiş yalnız paket adı, zaman, kategori, skor, bastırma ve sistem priority metadata'sı taşır.
5. Motor kapalıyken mevcut kullanıcıların klasik rozet davranışı korunur.
6. Performans sonucu gerçek cihaz benchmarkı olmadan tamamlandı kabul edilmez.

---

## 3. Masaüstü Gradle Doğrulaması

Windows PowerShell:

```powershell
cd "C:\Users\hekizoglu\Documents\AppOrganizer"

git fetch origin
git switch agent/smart-notification-validation
git pull

.\gradlew.bat testDebugUnitTest `
  --tests "*NotificationClassifierUseCaseTest" `
  --tests "*NotificationPriorityPolicyTest" `
  --tests "*NotificationPreviewStoreTest" `
  --tests "*AppNotificationListenerServiceTest" `
  --tests "*AppNotificationSnapshotReadTest" `
  --tests "*InMemorySmartNotificationRepositoryTest" `
  --tests "*UnreadNotificationModelTest" `
  --tests "*NotificationAnalyzerTest" `
  --tests "*SmartNotificationPrefsTest" `
  --tests "*SmartNotificationSettingsLabelsTest" `
  --tests "*NotificationReportV2OverviewTest" `
  --tests "*SmartNotificationServiceIntegrationTest"

.\gradlew.bat assembleDebug
```

### Başarı kapısı

- [x] `testDebugUnitTest` başarılı. (1294 test, 0 hata, 19 skipped — 3 tur düzeltme sonrası)
- [x] `assembleDebug` başarılı. (55s, EXIT 0)
- [x] Derleme çıktısında yeni compile error yok. (Sadece önceden var olan uyarılar — deprecated `priority`/`when` field kullanımı testlerde kasıtlı, `LocalLifecycleOwner` vb. proje geneli mevcut uyarılar.)
- [ ] APK boyutu önceki doğrulanmış build ile karşılaştırıldı. (Bekliyor — referans build yok.)
- [x] Sonuç tarihi, cihaz/PC bilgisi ve commit SHA bu dosyaya işlendi. (Aşağıda.)

**Doğrulama kaydı — 2026-07-26, masaüstü PC (cihaz/emülatör bağlı değildi):**

- Commit SHA: `4b811ae5`
- İlk çalıştırmada derleme hatası: `NotificationReportV2Overview.kt:13` ve `SmartNotificationFilterSettingsCard.kt:11` — `androidx.compose.foundation.layout.weight` top-level olarak import edilmiş, ancak bu isim yalnız `RowScope`/`ColumnScope` extension'ı olarak var (`internal` erişim hatası). Kök neden: gereksiz/yanlış import satırı. **Düzeltme:** İki dosyadan da import satırı silindi (Compose'da scope-bağlı `.weight()` ekstra import istemez).
- İkinci çalıştırmada 10 test hatası: `AppNotificationListenerServiceTest`, `SmartNotificationServiceIntegrationTest`, `NotificationPreviewStoreTest` — `io.mockk.MockKException: Missing mocked calls`. Kök neden: `android.app.Notification.priority` ve `.when` Java'da `public` **field**'dir (getter değil), MockK `every {}` bunları stub edemez. **Düzeltme:** `every { x.priority } returns 0` → doğrudan `x.priority = 0` field ataması (relaxed mock zaten mutable field ataması destekler).
- Üçüncü çalıştırmada aynı hata `.when` satırında zincirleme çıktı (aynı kök neden, `priority` düzeltilirken `.when` unutulmamıştı ama `every{}` hâlâ kullanılıyordu) → aynı düzeltme `.when` için de uygulandı.
- Ayrıca `NotificationPreviewStoreTest` başarısızlığı: `TestStatusBarNotificationFactory.kt` gerçek `Bundle()`/`Notification()` nesnesi new'liyordu; JVM unit test ortamında (`isReturnDefaultValues=true`) Android framework sınıfları no-op döner, bu yüzden `putCharSequence`/`getCharSequence` hep boş kalıyordu. **Düzeltme:** Factory, projedeki diğer test dosyalarıyla tutarlı şekilde mockk'lu `Bundle`/`Notification` kullanacak şekilde yeniden yazıldı.
- Dördüncü çalıştırma: **BUILD SUCCESSFUL**, 1294/1294 test yeşil.
- `assembleDebug`: **BUILD SUCCESSFUL**, 55s.
- Beklenmedik ek dosya: `app/schemas/.../24.json` — Room schema export derleme sırasında otomatik üretildi (build.gradle.kts `room.schemaLocation` ayarı gereği), kod ile içerik doğrulanıp commit'e dahil edildi (migration testleri bu dosyayı gerektirir).

---

## 4. Room ve Instrumented Testler

Cihaz veya emülatör bağlıyken:

```powershell
.\gradlew.bat connectedDebugAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.armutlu.apporganizer.NotificationMetadataMigrationTest,com.armutlu.apporganizer.NotificationMetadataDaoTest,com.armutlu.apporganizer.NotificationTextPrivacyDaoTest,com.armutlu.apporganizer.utils.SmartNotificationBackupCodecTest
```

### Başarı kapısı

- [x] `NotificationMetadataMigrationTest`, `NotificationMetadataDaoTest`, `NotificationTextPrivacyDaoTest`, `SmartNotificationBackupCodecTest` — **9 test cihazda çalıştı, 9/9 geçti, 0 hata.**
- [x] V23 satırları v24 migration sonrası korunuyor. (`NotificationMetadataMigrationTest` PASS)
- [x] Yeni kolon varsayılanları `OTHER / 35 / false / 0`. (`NotificationMetadataMigrationTest`/`NotificationMetadataDaoTest` PASS, statik kod incelemesiyle de teyitli: `MIGRATION_23_24` SQL default'ları ve `NotificationEvent.kt` `@ColumnInfo(defaultValue=...)` birebir eşleşiyor)
- [x] Kategori, bastırma ve önem sorguları doğru sonuç veriyor. (`NotificationMetadataDaoTest` PASS)
- [x] Dolu bildirim metni DAO'ya verilse bile Room'da boş kalıyor. (`NotificationTextPrivacyDaoTest` PASS)
- [x] Eski kalıcı metinler listener bağlantısında temizleniyor. (`NotificationTextPrivacyDaoTest` PASS + unit test `onListenerConnected purges legacy notification text` PASS)
- [x] V7 backup codec tam turu başarılı. (`SmartNotificationBackupCodecTest` PASS)
- [x] V1–V6 yedekleri yeni alan olmadığı için bozulmuyor. (`SmartNotificationBackupCodecTest` kapsamında test edildi, PASS)

**Doğrulama kaydı — 2026-07-26, Xiaomi 24116RACCG (Android 16):**

- İlk çalıştırma denemesinde `HeroDashboardInteractionTest.kt` (smart notification kapsamı dışı, aynı androidTest APK'sında derlenen ayrı bir dosya) derleme hatası verdi: `assertDoesNotExist`/`assertExists` için gereksiz/yanlış top-level import — bu iki fonksiyon `SemanticsNodeInteraction` sınıfının member metodu, import gerektirmiyor (jar decompile ile doğrulandı: `AssertionsKt` içinde yok, `SemanticsNodeInteraction.class` içinde `public final ... assertDoesNotExist()`/`assertExists(String)` olarak tanımlı). Bu tek dosyadaki hata tüm `connectedDebugAndroidTest` APK derlemesini bloklamıştı (instrumented testler tek APK'da derlenir). İki import satırı silinerek düzeltildi; düzeltme zaten commit `bf75bf8c`'de mevcuttu (aynı fix paralel olarak push edilmişti).
- Cihaz bağlantısında iki ayrı ortam sorunu yaşandı: (1) İlk denemede yanlış/farklı cihaz bağlıydı (stok MIUI launcher, AppOrganizer kurulu değildi) — `INSTALL_FAILED_USER_RESTRICTED`. (2) Doğru cihaza geçildikten sonra `adb devices` "unauthorized" gösterdi — telefonda USB hata ayıklama yetkilendirme diyaloğu onaylanarak çözüldü. Üçüncü denemede `device 'X' not found` geçici bağlantı kopması (bir dahaki denemede kendiliğinden düzeldi).
- Dördüncü deneme: **BUILD SUCCESSFUL, 46s, 9/9 test PASS.**

---

## 5. Gerçek Cihaz Ürün Kabulü

En az iki Android telefon; mümkünse farklı üretici/API seviyesi:

### Bildirim davranışı

- [ ] WhatsApp mesajı `MESSAGING` olarak görünür ve bastırılmaz.
- [ ] Banka OTP/giriş kodu yüksek öncelikli ve hassas kabul edilir.
- [ ] Alışveriş kampanyası promosyon filtresi açıkken AppOrganizer rozetinden düşer.
- [ ] Kargo bildirimi `DELIVERY` olarak kalır; promosyon sayılmaz.
- [ ] Takvim/randevu bildirimi `REMINDER` olur.
- [ ] Ongoing medya bildirimi geçmiş ve akıllı rozet hesabına girmez.
- [ ] Uygulama açılınca AppOrganizer okunmamış rozeti sıfırlanır.
- [ ] Android bildirim panelindeki gerçek bildirim silinmez.

### Ayarlar ekranı

- [ ] “Bildirim Filtreleme” ve “Akıllı Özetler” birbirinden açıkça ayrılmış.
- [ ] Motor kapalı/açık değişimi anlık uygulanıyor.
- [ ] Promosyon filtresi anlık uygulanıyor.
- [ ] Kategori görünürlük seçimleri anlık uygulanıyor.
- [ ] Klasik/kategori rozet modu seçimi korunuyor.
- [ ] Küçük ekran, tablet ve %200 font ölçeğinde taşma yok.

### Rapor ekranı

- [ ] Eyleme değer ve filtrelenen toplamlar doğru.
- [ ] Kategori dağılımı doğru.
- [ ] Gece ve yüksek öncelik sayıları doğru.
- [ ] Promosyon kaynağına dokununca doğru Android uygulama ayarı açılıyor.
- [ ] Raporda bildirim başlığı/gövdesi görünmüyor.

### Yedekleme

- [ ] Manuel export JSON sürümü `7`.
- [ ] `smartNotificationSettings` alanı mevcut.
- [ ] Export dosyasında `notificationText`, başlık, gövde, OTP, tutar veya IBAN yok.
- [ ] Ayarlar değiştirildikten sonra import eski tercihleri geri getiriyor.
- [ ] V6 örnek yedeği import edildiğinde mevcut akıllı bildirim ayarları korunuyor.
- [ ] Otomatik yerel/Drive yedeği de V7 alanını taşıyor.

---

## 6. Performans Benchmarkı

Gerçek cihazda:

```powershell
.\gradlew.bat :benchmark:connectedAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.armutlu.apporganizer.benchmark.SmartNotificationEngineBenchmark
```

Ölçülen yollar:

- tek bildirim sınıflandırması
- 10 aktif bildirim snapshot'ı
- 50 aktif bildirim snapshot'ı
- 100 aktif bildirim snapshot'ı

### Kabul yaklaşımı

- [ ] Median ve P95 sonuçları kaydedildi.
- [ ] Cihaz modeli, Android sürümü ve termal durum kaydedildi.
- [ ] 50/100 bildirim snapshot'ında belirgin doğrusal dışı sıçrama yok.
- [ ] Listener callback sırasında ana thread blokajı görülmüyor.
- [ ] Sonuç alınmadan “1 ms altında” veya “%50 hızlı” iddiası yazılmıyor.

---

## 7. Gizlilik Denetimi

Repo ve cihaz kontrolü:

- [ ] `apps.notificationText` kolonunda dolu satır yok.
- [ ] `notification_events` tablosunda içerik kolonu yok.
- [ ] Firebase Analytics/Crashlytics parametrelerinde bildirim metni yok.
- [ ] Timber/logcat çıktısında bildirim metni yok.
- [ ] Tanılama raporunda bildirim metni/paket listesi yok; yalnız toplu metadata var.
- [ ] Android Auto Backup ve cihaz transferi Room DB'yi hariç tutuyor.
- [ ] Manuel JSON yedek yalnız tercihleri taşıyor.

---

## 8. Merge Öncesi Karar Tablosu

| Alan | Kod | Unit test | Instrumented | Fiziksel cihaz | Durum |
|---|---:|---:|---:|---:|---|
| Sınıflandırıcı | Hazır | **Geçti** | Gerekmez | Bekliyor | `[~]` |
| Önem/bastırma | Hazır | **Geçti** | Gerekmez | Bekliyor | `[~]` |
| Repository/okunmamış | Hazır | **Geçti** | Gerekmez | Bekliyor | `[~]` |
| Room v24 metadata | Hazır | **Geçti** | **Geçti (9/9, Xiaomi 24116RACCG/Android16)** | Bekliyor | `[~]` |
| Analyzer/Report V2 | Hazır | **Geçti** | Gerekmez | Bekliyor | `[~]` |
| Ayar ekranı | Hazır | **Geçti** | Gerekmez | Bekliyor | `[~]` |
| P0 içerik gizliliği | Hazır | **Geçti** | **Geçti (9/9, Xiaomi 24116RACCG/Android16)** | Bekliyor | `[~]` |
| Backup V7 | Hazır | **Geçti** | **Geçti (9/9, Xiaomi 24116RACCG/Android16)** | Bekliyor | `[~]` |
| Performans | Altyapı hazır | Gerekmez | Benchmark **çalıştırılamadı** (bu turda koşulmadı) | Bekliyor | `[!]` |

**Not:** "Fiziksel cihaz" sütunu VALIDATION.md madde 5'teki *ürün kabul senaryolarını* (WhatsApp/banka/kargo bildirim davranışı, ayarlar ekranı UX, rapor ekranı, gerçek yedek alma) ifade eder — bunlar instrumented testten farklıdır ve bu turda **koşulmadı**. Tek cihazda instrumented test PASS oldu; VALIDATION.md madde 9'daki "en az iki telefon" kuralı için ikinci cihaz da gerekiyor.

---

## 9. Merge ve Geri Dönüş Kuralı

Merge yalnız şu koşullarda yapılır:

1. Masaüstü unit test ve debug build başarılıdır.
2. Room migration/gizlilik/backup instrumented testleri başarılıdır.
3. En az iki telefonda temel posted/removed/reconnect davranışı doğrulanmıştır.
4. P0 gizlilik maddelerinde ihlal yoktur.
5. Hüseyin açıkça `main` merge onayı vermiştir.

Sorun çıkarsa `main` geçmişi rewrite/force-push yapılmaz. İlgili commit veya PR normal `revert` ile geri alınır.

---

## 10. Sonuç Kaydı

| Tarih | Commit | Test cihazı | Unit/Build | Instrumented | Benchmark | Karar |
|---|---|---|---|---|---|---|
| — | `07d7e60` | — | Bekliyor | Bekliyor | Bekliyor | Merge yok |
| 2026-07-26 | `4b811ae5` | Yok (masaüstü PC, cihaz/emülatör bağlı değildi) | **Geçti** (1294/1294 test, testDebugUnitTest+assembleDebug) | **Çalıştırılamadı** (cihaz yok) | **Çalıştırılamadı** (cihaz yok) | Merge yok — cihaz doğrulaması bekliyor |
| 2026-07-26 | `bf75bf8c` | Xiaomi 24116RACCG, Android 16 | **Geçti** (bir önceki satırla aynı) | **Geçti** (9/9: Room v24 migration, DAO, gizlilik, Backup V7 codec) | **Çalıştırılamadı** (bu turda koşulmadı) | Merge yok — ikinci cihaz + ürün kabul senaryoları + benchmark bekliyor |
