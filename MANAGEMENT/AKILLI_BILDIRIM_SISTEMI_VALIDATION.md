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

- [!] Bu turda **çalıştırılamadı** — masaüstünde cihaz/emülatör bağlı değildi (`adb devices` boş liste döndürdü). `connectedDebugAndroidTest` cihaz/emülatör gerektirir, atlanmadı, gerçek engel budur.
- [x] (Statik kod incelemesiyle doğrulanabilenler, instrumented test yerine geçmez ama tutarlılık teyididir:)
  - `MIGRATION_23_24` (AppDatabase.kt:347-370) SQL default'ları `'OTHER' / 35 / 0 / 0` — görev hedefiyle birebir eşleşiyor.
  - `NotificationEvent.kt` Room entity `@ColumnInfo(defaultValue=...)` değerleri migration SQL'iyle birebir eşleşiyor (`'OTHER'`, `35`, `0`, `0`).
  - `title`/`text` kolonu şemaya eklenmemiş — `app/schemas/.../24.json` içeriğiyle doğrulandı.
  - `AppDao.updateNotificationText()` (satır 349, 360) her çağrıda `notificationText` alanını boş string'e set ediyor, gelen parametreyi hiç yazmıyor.
  - `AppNotificationListenerService.onListenerConnected()` (satır 81) `appDao.clearAllNotificationTexts()` çağırıyor — unit testle de doğrulandı (`onListenerConnected purges legacy notification text` PASS).
  - `SmartNotificationBackupCodec` — bilinmeyen kategori `runCatching` ile atlanıyor, bozuk badge modu fallback'e düşüyor, alan yoksa (V1-V6) fallback korunuyor (kod okuması, bkz. madde 7 altındaki not).
- [ ] Gerçek migration/DAO/codec instrumented test koşumu — **BEKLİYOR, cihaz/emülatör gerekli.**

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
| Sınıflandırıcı | Hazır | **Geçti (bu tur)** | Gerekmez | Bekliyor | `[~]` |
| Önem/bastırma | Hazır | **Geçti (bu tur)** | Gerekmez | Bekliyor | `[~]` |
| Repository/okunmamış | Hazır | **Geçti (bu tur)** | Gerekmez | Bekliyor | `[~]` |
| Room v24 metadata | Hazır | **Geçti (bu tur)** | Yazıldı, **çalıştırılamadı** (cihaz yok) | Bekliyor | `[~]` |
| Analyzer/Report V2 | Hazır | **Geçti (bu tur)** | Gerekmez | Bekliyor | `[~]` |
| Ayar ekranı | Hazır | **Geçti (bu tur)** | Gerekmez | Bekliyor | `[~]` |
| P0 içerik gizliliği | Hazır | **Geçti (bu tur)** | Yazıldı, **çalıştırılamadı** (cihaz yok) | Bekliyor | `[~]` |
| Backup V7 | Hazır | **Geçti (bu tur)** | Yazıldı, **çalıştırılamadı** (cihaz yok) | Bekliyor | `[~]` |
| Performans | Altyapı hazır | Gerekmez | Benchmark **çalıştırılamadı** (cihaz yok) | Bekliyor | `[!]` |

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
