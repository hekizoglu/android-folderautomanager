# AppOrganizer Sağlık Raporu Düzeltme Yol Haritası

> Oluşturma: 2026-07-16  
> Kaynak uygulama sürümü: `1.3.47 (70)`  
> Kaynak cihaz: Xiaomi `2107113SR`, Android 14 / API 34  
> Hedef: Sağlık raporundaki yanıltıcı verileri düzeltmek, sayaçları tek doğruluk kaynağına bağlamak ve raporu gerçek bir teknik teşhis aracına dönüştürmek.  
> Uygulama kuralı: Maddeler aşağıdaki sırayla ele alınmalı; her madde kabul kriterleri ve testleri geçmeden sonraki önceliğe geçilmemeli.

---

## 1. Mevcut rapordan çıkan doğrulanmış bulgular

Kaynak raporda görülen temel değerler:

- Toplam uygulama: `110`
- Kullanıcı uygulaması: `84`
- Sistem uygulaması: `26`
- Kategori sayısı: `42`
- Kategorisiz: `0`
- Bekleyen inceleme: `8`
- Onaylı: `5`
- Atlanmış: `1`
- Arama: `24`, sıfır sonuç: `1`, ortalama gecikme: `25 ms`
- Toplam tıklama: `10`, ilk sonuç tıklaması: `6`
- Son 7 gün bildirim olayı: `385`
- Toplam yıldız: `3`
- Widget: `0`
- Crash: yok
- `Files index one-shot` sonraki çalışma zamanı: `292278994-08-17 10:12:55`

### Kesin sorunlar

1. Tek seferlik dosya indeksleme işi tamamlandığı hâlde `nextScheduleTimeMillis` gerçek bir tarihmiş gibi yazılıyor.
2. Sınıflandırma sayaçları toplam `84` kullanıcı uygulamasını açıklamıyor.
3. Sağlık raporu, uygulamada zaten bulunan `ClassificationAttentionPolicy` tek doğruluk kaynağını kullanmıyor.
4. Arama altyapısı kaynak ve aksiyon sayaçlarını tutmasına rağmen rapor yalnızca kaba toplamları gösteriyor.
5. Worker satırları kullanıcı tarafından kapalı özellik ile bozuk/eksik planlama durumunu ayırmıyor.
6. Misyon motoru raporu, yalnızca toplam yıldız ve son olayı gösterdiği için sistemin gerçekten değer üretip üretmediği anlaşılamıyor.

### Sorun olmayan fakat yanlış yorumlanabilecek durumlar

- `ACCESS_COARSE_LOCATION: denied`: Konuma bağlı aktif özellik yoksa hata sayılmamalı.
- `Auto backup: enabled=hayir, work=yok`: Kullanıcı özelliği kapattıysa normal durumdur; sağlık raporu bunu arıza gibi göstermemeli.
- `Kayıtlı widget id: 0`: Teknik hata değildir; ürün kullanım sinyalidir.
- `Crash kaydı yok`: Olumlu durumdur, ancak ANR/donma ölçümü olmadığı için tek başına yeterli sağlık göstergesi değildir.

---

# P0 — Yanlış ve güven kırıcı verileri düzelt

## P0.1 — Tek seferlik WorkManager işinde 292 milyon yılı gösterme

### Kök neden

Dosya:

- `app/src/main/java/com/armutlu/apporganizer/utils/DiagnosticsReportManager.kt`

Mevcut akış:

- `workerSummary()` bütün `WorkInfo` kayıtlarında `nextScheduleTimeMillisCompat()` çağırıyor.
- `nextScheduleTimeMillisCompat()` yalnızca değerin `0`dan büyük olup olmadığına bakıyor.
- Tamamlanmış tek seferlik işlerde WorkManager, gerçek bir gelecek planı olmayan sentinel/aşırı büyük bir değer döndürebiliyor.
- Bu değer `Date(value)` ile biçimlendirilerek `292278994-08-17` gibi anlamsız bir tarihe dönüşüyor.

### Yapılacak değişiklikler

#### A. Worker tipini modele ekle

`WorkerSpec` veri sınıfına iş tipini ekle:

```kotlin
private enum class WorkerKind {
    PERIODIC,
    ONE_SHOT,
}

private data class WorkerSpec(
    val label: String,
    val uniqueName: String,
    val kind: WorkerKind,
    val enabled: () -> Boolean,
)
```

`WORK_SPECS` içinde:

- `auto_backup_weekly` → `PERIODIC`
- `smart_insight_daily` → `PERIODIC`
- `suggestion_notification_daily` → `PERIODIC`
- `weekly_digest` → `PERIODIC`
- `files_index_periodic` → `PERIODIC`
- `files_index_once` → `ONE_SHOT`

#### B. Sonraki çalışma zamanını yalnızca gerçekten planlı işlerde göster

Yeni pure/helper fonksiyon oluştur:

```kotlin
internal fun workerNextRunText(
    state: WorkInfo.State,
    kind: WorkerKind,
    nextScheduleTimeMillis: Long?,
    now: Long,
): String?
```

Kurallar:

1. `SUCCEEDED`, `FAILED`, `CANCELLED` durumlarında `next=` yazma.
2. `ONE_SHOT + SUCCEEDED` için açıkça `tamamlandi, sonraki calisma yok` yaz.
3. `RUNNING` için `next=` yerine `su anda calisiyor` yaz.
4. `ENQUEUED` durumunda tarih ancak makul aralıktaysa gösterilsin.
5. Tarih `now`dan küçükse `gecmis/yeniden planlama bekliyor` olarak işaretlensin.
6. Tarih `now + 10 yıl` sınırından büyükse sentinel kabul edilip tarih yazılmasın.
7. `Long.MAX_VALUE` ve yakın değerleri kesin olarak filtrele.

Önerilen koruma:

```kotlin
private const val MAX_REASONABLE_SCHEDULE_HORIZON_DAYS = 3650L

private fun Long.isReasonableScheduleTime(now: Long): Boolean {
    val max = now + TimeUnit.DAYS.toMillis(MAX_REASONABLE_SCHEDULE_HORIZON_DAYS)
    return this in 1..max && this != Long.MAX_VALUE
}
```

#### C. Çıktıyı insan tarafından anlaşılır hâle getir

Beklenen örnekler:

```text
Files index one-shot: enabled=evet, work=SUCCEEDED, attempts=1, tamamlandi, sonraki calisma yok
Files index periodic: enabled=evet, work=ENQUEUED, attempts=0, next=2026-07-16 12:37:36
Auto backup: enabled=hayir, work=yok, durum=kullanici tarafindan kapali
```

### Testler

Dosya:

- `app/src/test/java/com/armutlu/apporganizer/utils/DiagnosticsReportManagerTest.kt`

Eklenecek testler:

1. `oneShotSucceeded_doesNotRenderNextDate`
2. `oneShotSucceeded_rendersNoNextRunMessage`
3. `periodicEnqueued_rendersReasonableNextDate`
4. `longMaxValue_isTreatedAsNoNextRun`
5. `farFutureSentinel_isNotFormattedAsDate`
6. `failedWork_doesNotRenderNextDate`
7. `runningWork_rendersCurrentlyRunning`

### Kabul kriterleri

- Raporda hiçbir koşulda 10 yıldan daha uzak bir worker tarihi görünmemeli.
- `files_index_once` tamamlandıysa sonraki çalışma tarihi yazılmamalı.
- Periyodik işlerin geçerli gelecek tarihi korunmalı.
- Mevcut worker özet testleri kırılmamalı; yeni durumlar kapsanmalı.

---

## P0.2 — Sınıflandırma sayaçlarını 84 kullanıcı uygulamasıyla uzlaştır

### Kök neden

İlgili dosyalar:

- `app/src/main/java/com/armutlu/apporganizer/utils/DiagnosticsReportManager.kt`
- `app/src/main/java/com/armutlu/apporganizer/data/local/AppDao.kt`
- `app/src/main/java/com/armutlu/apporganizer/domain/usecase/classify/ClassificationAttentionPolicy.kt`

Mevcut rapor farklı tanımları birlikte kullanıyor:

- `pendingCount`: `AppDao.getPendingClassificationApps(now)`
- `confirmedCount`: bütün uygulamalarda `classificationReviewState == CONFIRMED`
- `skippedCount`: bütün uygulamalarda `classificationReviewState == SKIPPED`
- `uncategorizedCount`: bütün uygulamalarda boş veya `uncategorized`

Bu sayaçlar:

- Aynı evreni kullanmıyor.
- Sistem uygulaması filtreleri farklı.
- Birbirleriyle örtüşebilir.
- Snooze edilen kayıtları görünmez bırakabilir.
- `CORRECTED`, `NOT_REQUIRED`, düşük güven, `OTHER`, çatışmalı sınıflandırma ve otomatik kabul edilen kayıtları açıklamaz.
- Toplam kullanıcı uygulamasıyla matematiksel olarak uzlaşmaz.

Uygulamada bunun için zaten tek doğruluk kaynağı vardır:

- `ClassificationAttentionPolicy.evaluate()`
- `ClassificationAttentionPolicy.attentionCount()`
- `ClassificationAttentionPolicy.attentionApps()`

Sağlık raporu bu policy yerine eski/dar DAO sorgusunu kullanmamalıdır.

### Yapılacak değişiklikler

#### A. Pure sınıflandırma teşhis hesaplayıcısı oluştur

Önerilen yeni dosya:

- `app/src/main/java/com/armutlu/apporganizer/domain/usecase/classify/ClassificationDiagnosticsCalculator.kt`

Önerilen model:

```kotlin
internal data class ClassificationDiagnostics(
    val totalUserApps: Int,
    val automaticAccepted: Int,
    val needsAttention: Int,
    val snoozed: Int,
    val confirmed: Int,
    val corrected: Int,
    val skipped: Int,
    val uncategorized: Int,
    val invalidOrUnknown: Int,
    val reconciledTotal: Int,
    val isConsistent: Boolean,
    val attentionByReason: Map<AttentionReason, Int>,
)
```

#### B. Kovaları birbirini dışlayan hâle getir

Her kullanıcı uygulaması yalnızca bir ana kovaya girmeli. Önerilen öncelik sırası:

1. Geçersiz/bilinmeyen veri
   - boş `classificationReviewState`
   - enum'a çevrilemeyen state
   - beklenmeyen category/source/reason birleşimi
2. `UNCATEGORIZED` / boş kategori
3. Aktif snooze
4. `CONFIRMED`
5. `CORRECTED`
6. `SKIPPED`
7. `ClassificationAttentionPolicy.evaluate(app, now) != null`
8. Geri kalanlar → `automaticAccepted`

Notlar:

- Ana kovalar matematiksel uzlaşma içindir.
- `attentionByReason` ayrıca alt kırılım verir; ana kovaların toplamını bozmaz.
- Gizli kullanıcı uygulamaları toplam evrenden çıkarılmamalı; ayrı `hiddenUserApps` satırı ile gösterilmeli. Aksi hâlde toplam uygulama sayısı yine açıklanamaz.
- Sistem uygulamaları sınıflandırma ana toplamına katılmamalı.

#### C. `DiagnosticsReportManager` içinde eski sayaçları kaldır

Kaldırılacak/doğrudan kullanılmayacak hesaplar:

```kotlin
val pendingCount = appDao.getPendingClassificationApps(now).first().size
val uncategorizedCount = apps.count { ... }
val confirmedCount = apps.count { ... }
val skippedCount = apps.count { ... }
```

Yerine:

```kotlin
val classificationDiagnostics =
    ClassificationDiagnosticsCalculator.calculate(apps, now)
```

kullanılmalı.

Bu değişiklikten sonra `kotlinx.coroutines.flow.first` yalnızca bu kullanım için import ediliyorsa kaldırılmalı.

#### D. Sağlık raporu çıktısını açık ve uzlaşabilir yap

Yeni `[Siniflandirma]` örneği:

```text
Mod: LOCAL_WITH_MANUFACTURER
Kullanici uygulamasi toplam: 84
Otomatik kabul edilen: 65
Dikkat gereken: 8
Ertelemede: 5
Kullanici onayli: 5
Kullanici duzeltmis: 0
Atlanmis: 1
Kategorisiz/bos: 0
Gecersiz/bilinmeyen durum: 0
Sayac toplami: 84
Tutarlilik: OK
Dikkat nedenleri: REVIEW_PENDING=8, LOW_CONFIDENCE=0, OTHER_WITHOUT_CONFIDENCE=0, UNCATEGORIZED=0, MISSING_CATEGORY=0, CLASSIFIER_CONFLICT=0
```

Sayılar örnektir; cihazdaki gerçek veriden hesaplanacaktır.

#### E. Tutarsızlıkta rapora kritik uyarı ekle

`isConsistent == false` ise `[Kritik Hatalar]` bölümüne kişisel veri içermeyen uyarı ekle:

```text
Siniflandirma sayac uyusmazligi: userApps=84, bucketTotal=82
```

Paket adı veya uygulama adı rapora yazılmamalı.

### Testler

Önerilen yeni test dosyası:

- `app/src/test/java/com/armutlu/apporganizer/domain/usecase/classify/ClassificationDiagnosticsCalculatorTest.kt`

Test senaryoları:

1. Her kullanıcı uygulaması tam bir kovaya düşer.
2. Sistem uygulaması sınıflandırma toplamına girmez.
3. Gizli kullanıcı uygulaması toplamı bozmadan ayrıca sayılır.
4. Aktif snooze, `needsAttention` yerine `snoozed` kovasına girer.
5. Süresi bitmiş snooze yeniden `needsAttention` olur.
6. `CONFIRMED` ve `CORRECTED` ayrı sayılır.
7. `SKIPPED` ayrı sayılır.
8. `OTHER` ve düşük güven, `ClassificationAttentionPolicy` ile aynı sonucu verir.
9. Boş kategori `uncategorized` kovasına girer.
10. Bilinmeyen review state `invalidOrUnknown` olur.
11. Bütün örneklerde `reconciledTotal == totalUserApps` sağlanır.
12. Rapordaki `Tutarlilik: OK/MISMATCH` doğru üretilir.

### Kabul kriterleri

- `[Siniflandirma]` bölümündeki ana sayaçların toplamı daima kullanıcı uygulaması toplamına eşit olmalı.
- Sağlık raporu ve `Kontrol Bekleyenler` ekranı aynı `ClassificationAttentionPolicy` sonucunu kullanmalı.
- Snooze edilen kayıtlar kaybolmamalı; ayrı görünmeli.
- Sistem uygulamaları kullanıcı sınıflandırma toplamına karışmamalı.
- Raporda paket adı, uygulama adı veya kişisel veri bulunmamalı.

---

# P1 — Raporu gerçek teşhis aracına dönüştür

## P1.1 — Arama metriklerini mevcut altyapının tamamıyla raporla

### Mevcut durum

Dosyalar:

- `app/src/main/java/com/armutlu/apporganizer/utils/SearchStatsPrefs.kt`
- `app/src/main/java/com/armutlu/apporganizer/utils/DiagnosticsReportManager.kt`
- `app/src/main/java/com/armutlu/apporganizer/data/repository/SearchRepository.kt`
- Arama sonucu tıklamalarını yapan launcher/UI dosyaları

`SearchStatsPrefs.Summary` zaten şunları içeriyor:

- `clickCountsByType`
- `actionCounts`
- `totalClicks`
- `firstResultClicks`

Ancak sağlık raporu yalnızca toplam arama, sıfır sonuç, gecikme ve iki tıklama sayısını yazıyor.

### Yapılacak değişiklikler

1. `SearchStatsPrefs.Summary` içine kayıtlı olduğu hâlde dışarı verilmeyen `avgQueryLength` ekle.
2. Aşağıdaki oranları güvenli biçimde hesaplayan pure fonksiyon ekle:
   - sıfır sonuç oranı
   - aramadan sonuca tıklama oranı
   - ilk sonuç tercih oranı
   - kaynak türüne göre tıklama dağılımı
   - aksiyon türüne göre dağılım
3. Sıfıra bölmeyi engelle.
4. Oranları bir ondalık basamakla göster.
5. Arama metnini, kişi adını, telefon numarasını ve dosya adını asla kaydetme/yazma.

Beklenen çıktı:

```text
Arama sayaci: total=24, zero=1, zeroRate=4.2%, avgLatencyMs=25
Sonuc etkilesimi: totalClicks=10, clickThroughRate=41.7%, firstResultClicks=6, firstResultRate=60.0%
Tiklama kaynaklari: app=6, contact=2, file=1, settings=1
Hizli aksiyonlar: OPEN_APP=6, CALL=1, WHATSAPP=1
Ortalama sorgu uzunlugu: 5.4 karakter
```

### Ek telemetri — ikinci aşama

Arama sonucuna tıklamadan gerçekleşen başarılı işlemleri ayırmak için anonim sonuç durumu eklenebilir:

- `RESULT_CLICKED`
- `QUICK_ACTION_EXECUTED`
- `DISMISSED`
- `NO_RESULT`

Bu kayıtlar yalnızca sayaç olmalı; sorgu metni tutulmamalı.

### Testler

- `SearchStatsPrefsTest.kt` genişletilmeli.
- Sıfır aramada bütün oranlar `0.0%` olmalı.
- Kaynak/aksiyon map sıralaması deterministik olmalı.
- Gizlilik testi raporda arama metninin bulunmadığını doğrulamaya devam etmeli.

### Kabul kriterleri

- Rapordaki tıklama oranları elle hesaplanan değerlerle aynı olmalı.
- Mevcut `24/1/25/10/6` örneği için oranlar sırasıyla yaklaşık `%4.2`, `%41.7`, `%60.0` çıkmalı.
- Hiçbir sorgu içeriği rapora veya SharedPreferences'a eklenmemeli.

---

## P1.2 — Worker özetinde kapalı özellik ile arızayı ayır

### Sorun

Şu iki durum aynı görünmemeli:

1. Kullanıcı özelliği kapattı, bu yüzden work yok.
2. Özellik açık ama work hiç planlanmamış/kaybolmuş.

### Yapılacak değişiklikler

`workerSummary()` karar tablosu:

| enabled | work listesi | Beklenen durum |
|---|---|---|
| hayır | boş | `NORMAL_KAPALI` |
| hayır | dolu | `UYARI: kapali ozellik icin work mevcut` |
| evet | boş | `HATA: etkin fakat work bulunamadi` |
| evet | dolu | WorkInfo durumuna göre normal değerlendirme |

Aşağıdaki terminal durumlar da açık yazılmalı:

- `FAILED`: deneme sayısı ve güvenli hata kodu
- `CANCELLED`: iptal edilmiş
- `SUCCEEDED`: tamamlandı
- `BLOCKED`: bağımlılık bekliyor
- `RUNNING`: çalışıyor
- `ENQUEUED`: planlı

### Hata nedenleri

WorkManager `outputData` içinde kişisel veri içermeyen hata kodu standardı oluştur:

```kotlin
const val KEY_FAILURE_CODE = "failure_code"
```

Örnek kodlar:

- `PERMISSION_DENIED`
- `IO_ERROR`
- `DATABASE_ERROR`
- `STORAGE_UNAVAILABLE`
- `UNKNOWN`

Ham exception mesajı sağlık raporuna doğrudan yazılmamalı.

### Kabul kriterleri

- `Auto backup disabled + work yok` hata sayılmamalı.
- `Files index periodic enabled + work yok` kritik uyarı üretmeli.
- Kapalı özellik için hâlâ planlı work varsa uyarı üretmeli.
- Worker raporu kişisel veri veya dosya yolu sızdırmamalı.

---

## P1.3 — Worker çalışma geçmişini kalıcı ve ölçülebilir yap

### Sorun

`WorkInfo` mevcut planın durumunu gösterir; geçmiş başarı, son çalışma süresi ve son hata zamanı her zaman güvenilir biçimde elde edilemez.

### Önerilen yeni dosya

- `app/src/main/java/com/armutlu/apporganizer/utils/WorkerTelemetryPrefs.kt`

Her worker için anonim alanlar:

- `lastStartedAt`
- `lastFinishedAt`
- `lastSucceededAt`
- `lastFailedAt`
- `lastFailureCode`
- `lastDurationMs`
- `successCount`
- `failureCount`

Her worker'ın `doWork()` başlangıç ve bitişinde ortak helper çağrılmalı.

Rapor örneği:

```text
Files index periodic: enabled=evet, work=ENQUEUED, next=...
  lastSuccess=2026-07-15 23:59:47, durationMs=842, success=12, failure=0
```

### Kabul kriterleri

- WorkManager geçmiş kaydı prune edilse bile son başarı zamanı görünmeli.
- Worker süresi negatif olamamalı.
- Ham exception metni yerine hata kodu tutulmalı.
- Sayaçlar istatistik sıfırlama akışına kontrollü biçimde eklenmeli.

---

## P1.4 — Yedekleme durumunu ürün tercihi ve teknik sağlık olarak ayır

### İlgili alanlar

- `AppPrefs.isAutoBackupEnabled(context)`
- `auto_backup_weekly`
- Backup worker/manager sınıfları
- İstatistik ve sağlık raporu

### Yapılacak değişiklikler

Rapor üç ayrı bilgi vermeli:

1. Kullanıcı tercihi: açık/kapalı
2. Worker planı: var/yok/durum
3. Son yedek: başarı zamanı/hata kodu/yedek kapsamı

Beklenen kapalı durum:

```text
Auto backup: tercih=kapali, work=yok, saglik=NORMAL
```

Beklenen açık fakat bozuk durum:

```text
Auto backup: tercih=acik, work=yok, saglik=HATA_PLANLANMAMIS
```

### Yedek kapsamı

Yedeklenebilir:

- Klasörler ve yerleşim
- Uygulama-kategori eşleşmeleri
- Manuel düzeltmeler ve kilitler
- Gizli uygulama tercihleri
- Kullanıcı ayarları
- Görev ilerlemeleri; ürün kararıyla

Varsayılan olarak yedeklenmemeli:

- Bildirim metinleri
- Kişi adları ve telefon numaraları
- Arama sorguları
- Dosya adları ve dosya yolları

### Kabul kriterleri

- Kapalı yedekleme kritik hata üretmemeli.
- Açık ancak plansız yedekleme uyarı/hata üretmeli.
- Son yedek zamanı ve anonim hata kodu görülebilmeli.
- Gizlilik kapsamı otomatik test ile korunmalı.

---

## P1.5 — Misyon motoru raporunu davranış kalitesiyle ilişkilendir

### İlgili dosyalar

- `app/src/main/java/com/armutlu/apporganizer/data/repository/MissionsRepository.kt`
- `app/src/main/java/com/armutlu/apporganizer/utils/MissionPrefs.kt`
- `app/src/main/java/com/armutlu/apporganizer/utils/TaskScoreManager.kt`
- `app/src/main/java/com/armutlu/apporganizer/domain/usecase/missions/MissionEngine.kt`
- `app/src/main/java/com/armutlu/apporganizer/utils/DiagnosticsReportManager.kt`

### Sorun

`Toplam yildiz: 3` ve son olayın `Bildirim raporu acildi` olması tek başına motorun sağlığını göstermez. Rapor açma gibi düşük maliyetli davranışların dijital yaşam skorunu gereğinden fazla yükseltme riski vardır.

### Yapılacak değişiklikler

Rapora anonim sayaçlar ekle:

- Bugün tamamlanan görev
- Bu hafta tamamlanan görev
- Aktif günlük görev sayısı
- Aktif haftalık görev sayısı
- Davranış değişikliği görevleri
- Yalnızca ekran açma/görüntüleme görevleri
- Son 7 gün pozitif skor
- Son 7 gün negatif skor
- Aynı eventKey için tekrar ödül engeli durumu

### Puanlama ilkesi

1. Rapor açma/ekran görüntüleme gibi görevler düşük ödüllü olmalı.
2. Kullanım azaltma, sınıflandırma düzeltme, gereksiz bildirim azaltma gibi ölçülebilir davranışlar daha yüksek ödüllü olmalı.
3. Aynı ekranı tekrar tekrar açarak sınırsız puan alınamamalı.
4. Günlük/haftalık idempotency veritabanı seviyesinde korunmalı.
5. Dijital yaşam skoru doğrudan toplam yıldızla eşitlenmemeli; yıldız motivasyon metriği, skor davranış metriği olmalı.

### Kabul kriterleri

- Aynı `NotificationReportViewed` olayı aynı gün sınırsız puan üretememeli.
- Rapor açma görevi tek başına dijital yaşam skorunu belirgin artırmamalı.
- Sağlık raporu misyon motorunun aktif fakat kullanılmıyor olmasıyla bozuk olmasını ayırabilmeli.

---

# P2 — Sağlık raporunun kapsamını genişlet

## P2.1 — Veritabanı ve önbellek boyutları

Eklenecek anonim metrikler:

- Room veritabanı boyutu
- WAL/SHM boyutu
- Cache dizini toplam boyutu
- Dosya indeksindeki öğe sayısı
- Tahmini indeks başına bayt

Kurallar:

- Dosya adları rapora yazılmamalı.
- Yalnızca toplam bayt/MB gösterilmeli.
- Boyut hesabı rapor oluşturmayı belirgin yavaşlatmamalı.

---

## P2.2 — İzinlerin ihtiyaç durumunu da göster

Sadece `granted/denied` yerine özellik bağı ekle:

```text
READ_CONTACTS: granted, gerekli=evet, neden=contacts_search_enabled
ACCESS_COARSE_LOCATION: denied, gerekli=hayir, saglik=NORMAL
POST_NOTIFICATIONS: granted, gerekli=evet
```

### Kabul kriterleri

- Kullanılmayan özellik için reddedilen izin hata sayılmamalı.
- Etkin özellik gerekli izne sahip değilse uyarı üretilmeli.

---

## P2.3 — Bildirim dinleyicisinin tazeliğini ölç

Mevcut toplam event sayısı tek başına listener'ın şu anda çalıştığını kanıtlamaz.

Eklenecek alanlar:

- Son bildirim olayı zamanı
- Son 24 saat event sayısı
- Son 7 gün günlük ortalama
- Listener izinli fakat uzun süredir event yok uyarısı

Dikkat:

- Bildirim metni ve paket listesi yazılmamalı.
- Telefon gerçekten bildirim almamış olabilir; bu yüzden sıfır olay doğrudan kritik hata değil, `kontrol önerisi` olmalı.

---

## P2.4 — ANR ve donma görünürlüğü

Crash kaydı olmaması uygulamanın tamamen sağlıklı olduğunu göstermez.

Android sürümünün izin verdiği ölçüde:

- Son çıkış nedenleri (`ApplicationExitInfo`)
- ANR sayısı
- Low memory kill
- Native crash
- Kullanıcı tarafından durdurulma

anonim toplamlarla raporlanmalı.

Ham stack trace sağlık raporuna eklenmemeli; mevcut `CrashReporter` güvenli özet yaklaşımı korunmalı.

---

## P2.5 — Uygulama başlangıç ve ana ekran üretim süresi

Eklenecek metrikler:

- Cold start son değer / EMA
- Warm start son değer / EMA
- Ana ekran ilk çizim süresi
- Uygulama katalog reconcile süresi
- Usage sync süresi
- Dosya indeksleme süresi

Bunlar `PerformanceTelemetryPrefs` benzeri anonim sayaçlarda tutulabilir.

---

# 3. `DiagnosticsReportSnapshot` yeniden düzenleme planı

Mevcut snapshot çok sayıda düz alan taşımaktadır. Yeni alanlar eklenirken tek sınıfı kontrolsüz büyütmemek için alt modeller önerilir:

```kotlin
internal data class DiagnosticsReportSnapshot(
    val app: AppDiagnostics,
    val permissions: PermissionDiagnostics,
    val catalog: CatalogDiagnostics,
    val classification: ClassificationDiagnostics,
    val search: SearchDiagnostics,
    val notifications: NotificationDiagnostics,
    val missions: MissionDiagnostics,
    val widgets: WidgetDiagnostics,
    val workers: List<WorkerDiagnostics>,
    val storage: StorageDiagnostics,
    val criticalIssues: List<DiagnosticIssue>,
)
```

Ortak sorun modeli:

```kotlin
internal enum class DiagnosticSeverity {
    INFO,
    WARNING,
    ERROR,
}

internal data class DiagnosticIssue(
    val code: String,
    val severity: DiagnosticSeverity,
    val summary: String,
)
```

Örnek kodlar:

- `CLASSIFICATION_COUNT_MISMATCH`
- `WORK_ENABLED_BUT_MISSING`
- `WORK_DISABLED_BUT_SCHEDULED`
- `INVALID_NEXT_SCHEDULE_TIME`
- `REQUIRED_PERMISSION_MISSING`
- `FILE_INDEX_STALE`
- `DATABASE_SIZE_HIGH`

### Kural

`renderReport()` yalnızca hazır snapshot'ı metne çevirmeli. Hesaplama, policy ve veri uzlaştırma işi render fonksiyonunun içine konmamalı.

---

# 4. Sağlık skoru eklenirse uygulanacak kurallar

Tek bir `Sağlık: 85/100` skoru eklenebilir; ancak dekoratif ve yanıltıcı olmamalıdır.

Önerilen ağırlık:

- Kritik veri tutarlılığı: %30
- Worker/arka plan iş sağlığı: %20
- İzin ve kaynak erişimi: %15
- Arama/indeks tazeliği: %15
- Crash/ANR: %15
- Yedekleme: %5; kullanıcı kapattıysa puan düşürme

Kurallar:

- Kullanıcının bilinçli kapattığı özellik puan düşürmemeli.
- Widget kullanmamak puan düşürmemeli.
- Konum izni gerekmiyorsa reddedilmesi puan düşürmemeli.
- Sınıflandırma sayaç uyuşmazlığı ciddi puan düşürmeli.
- Geçersiz worker tarihi ciddi veri güveni sorunu sayılmalı.
- Her puan kesintisinin raporda açıklanabilir bir `DiagnosticIssue` karşılığı olmalı.

---

# 5. Test ve doğrulama matrisi

## Unit test

Çalıştırılacak minimum komut:

```bash
./gradlew testDebugUnitTest
```

Zorunlu test alanları:

- Worker tarih filtreleme
- Sınıflandırma kovaları ve toplam uzlaşması
- Arama oran hesapları
- Snapshot render
- Gizlilik sızıntısı testi
- Sıfıra bölme ve boş veri senaryoları

## Static analysis

```bash
./gradlew lintDebug
./gradlew detekt
```

## Cihaz/emülatör doğrulaması

En az şu senaryolar denenmeli:

1. Dosya indeksi tek seferlik iş `SUCCEEDED`
2. Periyodik indeks `ENQUEUED`
3. Otomatik yedek kapalı
4. Otomatik yedek açık fakat work iptal edilmiş
5. Snooze edilmiş sınıflandırma kaydı
6. Düşük güvenli ve `OTHER` kategori kaydı
7. Hiç arama yapılmamış temiz kurulum
8. Arama yapılmış, tıklama yapılmamış
9. Kişi/dosya arama kaynakları kapalı
10. Bildirim listener açık fakat event yok

## Regresyon kontrolü

- Sağlık raporu paylaşma intent'i çalışmalı.
- FileProvider URI izni korunmalı.
- Rapor dosyası UTF-8 açılmalı.
- Türkçe karakterler bozulmamalı.
- Rapor oluşturma ana thread'i belirgin bloke etmemeli.
- Paket adı, kişi adı, telefon numarası, bildirim metni, arama sorgusu, dosya adı ve dosya yolu sızmamalı.

---

# 6. Uygulama sırası

| Sıra | İş | Öncelik | Tahmini risk | Bağımlılık |
|---:|---|---|---|---|
| 1 | Tek seferlik worker tarih sentinel düzeltmesi | P0 | Düşük | Yok |
| 2 | Worker tarih/helper unit testleri | P0 | Düşük | 1 |
| 3 | ClassificationDiagnosticsCalculator | P0 | Orta | ClassificationAttentionPolicy |
| 4 | Sağlık raporunu yeni sınıflandırma modeline bağlama | P0 | Orta | 3 |
| 5 | Sınıflandırma uzlaşma ve gizlilik testleri | P0 | Orta | 3-4 |
| 6 | Worker açık/kapalı/eksik karar tablosu | P1 | Düşük-Orta | 1 |
| 7 | Arama oranları ve kaynak kırılımı | P1 | Düşük | SearchStatsPrefs |
| 8 | Worker kalıcı telemetrisi | P1 | Orta | Worker sınıfları |
| 9 | Yedekleme sağlık ayrımı | P1 | Orta | Backup worker |
| 10 | Misyon kalite metrikleri ve tekrar ödül denetimi | P1 | Orta-Yüksek | MissionsRepository/MissionEngine |
| 11 | Depolama, izin ihtiyacı, bildirim tazeliği | P2 | Orta | İlgili yardımcılar |
| 12 | ANR/performans telemetrisi | P2 | Orta-Yüksek | Android sürüm uyumluluğu |

---

# 7. İlk uygulanacak minimum güvenli paket

İlk kod değişikliği yalnızca aşağıdaki kapsamda tutulabilir:

1. `DiagnosticsReportManager.kt`
   - Worker türü
   - Sentinel tarih filtresi
   - Terminal state metinleri
2. `ClassificationDiagnosticsCalculator.kt`
   - Birbirini dışlayan sayaçlar
   - `ClassificationAttentionPolicy` kullanımı
3. `DiagnosticsReportSnapshot`
   - Yeni sınıflandırma alanları
4. `renderReport()`
   - Uzlaşan sınıflandırma çıktısı
   - Worker terminal durum çıktısı
5. Testler
   - Worker tarihleri
   - Sınıflandırma toplamı
   - Gizlilik

Bu minimum paket tamamlanınca cihazda yeniden sağlık raporu alınmalı. Beklenen temel sonuç:

```text
Files index one-shot: ... tamamlandi, sonraki calisma yok
```

ve:

```text
Kullanici uygulamasi toplam: 84
Sayac toplami: 84
Tutarlilik: OK
```

Bu iki sonuç görülmeden P1/P2 genişletmelerine geçilmemelidir.

---

# 8. Tamamlanma tanımı

Bu yol haritasının P0 bölümü aşağıdaki koşullarda tamamlanmış sayılır:

- 292 milyon yılı veya benzeri sentinel tarih rapordan tamamen kalkmıştır.
- Tek seferlik tamamlanan işler açıkça `sonraki çalışma yok` olarak görünür.
- Sınıflandırma ana sayaçları kullanıcı uygulaması toplamına tam eşittir.
- `Kontrol Bekleyenler` ile sağlık raporu aynı attention policy sonucunu kullanır.
- Snooze, corrected, skipped ve otomatik kabul edilen kayıtlar görünürdür.
- Bütün yeni unit testler geçer.
- Lint/detekt yeni hata üretmez.
- Sağlık raporu kişisel veri sızdırmaz.
- Xiaomi 2107113SR / Android 14 cihazında yeni raporla manuel doğrulama yapılmıştır.

P1/P2 tamamlanma durumu daha sonra `HISTORY.md` içine döngü numarasıyla taşınmalı; aktif olmayan tamamlanmış maddeler bu dosyada sürekli büyütülmemelidir.
