# AppOrganizer — Akıllı Bildirim Sistemi Uygulama Görev Dosyası

> **Tek kaynak:** Akıllı Bildirim Sistemi geliştirmesi bu dosya üzerinden yürütülür.  
> **Gizlilik:** Bildirim başlığı ve metni cihaz dışına çıkmaz, Room veritabanına, Firebase'e, loglara, tanılama raporuna veya yedeğe yazılmaz.  
> **Branch kuralı:** Yapısal ve mimari değişiklikler ayrı feature branch'te hazırlanır. Hüseyin'in açık onayı olmadan `main` branch'e merge veya doğrudan push yapılmaz.  
> **Son güncelleme:** 2026-07-26  
> **Çalışma branch'i:** `agent/smart-notification-task-plan`

---

## 1. Ürün Kararları — Değiştirilmeden Uygulanacak Kurallar

1. **On-device only:** Sınıflandırma ve önem puanlama yalnız cihaz üzerinde çalışır. Harici LLM/API kullanılmaz.
2. **Android bildirimini silme yok:** Promosyon filtreleme yalnız AppOrganizer rozetini ve özetini etkiler. `cancelNotification()` ile sistem bildirimi habersizce iptal edilmez.
3. **İçerik kalıcılaştırılmaz:** Başlık/metin yalnız aktif bildirim belleğinde tutulur. Kalıcı raporda sadece paket adı, zaman, kategori, skor ve bastırma durumu gibi içeriksiz metadata bulunabilir.
4. **Üç kavram ayrı tutulur:**
   - Aktif bildirim: Android bildirim panelinde hâlâ duran kayıt.
   - Okunmamış bildirim: Son bildirim zamanı, kullanıcının uygulamayı son açışından yeni olan kayıt.
   - Geçmiş istatistik: Son 7/30 günde gelen olay sayısı.
5. **Tek sınıflandırma hattı:** Başlık/gövde ayrıştırması `NotificationPreviewStore` üzerinden yapılır. İkinci bir parser yazılmaz.
6. **Tek durum sahibi:** UI doğrudan `AppNotificationListenerService.companion object` alanlarına kalıcı olarak bağlanmaz. Hedef yapı enjekte edilen `SmartNotificationRepository` olur.
7. **Domain katmanında Compose rengi yok:** Kategori → renk dönüşümü presentation katmanındaki mapper üzerinden yapılır.
8. **Performans iddiası ölçümsüz yazılmaz:** “1 ms altında” bir hedef olabilir; benchmark kanıtı olmadan tamamlandı sayılmaz.
9. **Hassas içerik varsayılan gizli:** Yeni kurulumda hassas içerik maskeleme açık başlar.
10. **Eski kullanıcı güvenliği:** Mevcut kullanıcıların rozet davranışı migration sırasında sessizce değiştirilmez. Yeni motor önce feature flag ile açılır.

---

## 2. Durum Sembolleri

- `[x]` Kod + test + kanıt tamamlandı.
- `[~]` Kodun tamamı veya önemli bölümü mevcut; tam build/entegrasyon/ürün kabulü eksik.
- `[ ]` Başlanmadı.
- `[!]` Karar veya dış test gerektiriyor.

> **Kural:** Yalnız dosyanın var olması `[x]` için yeterli değildir. `testDebugUnitTest`, ilgili entegrasyon testi ve gerekiyorsa fiziksel cihaz kanıtı bulunmalıdır.

---

## 3. Mevcut Repo Gerçeği — 2026-07-26

### Hazır olanlar

- `NotificationCategory.kt` mevcut: `MESSAGING`, `DELIVERY`, `FINANCE`, `PROMOTION`, `REMINDER`, `SOCIAL`, `SYSTEM`, `OTHER`.
- `SmartNotification.kt` mevcut; içerik yalnız bellekte tutulacak şekilde tasarlanmış.
- `NotificationClassifierUseCase.kt` mevcut; kategori, hassaslık, 0–100 skor ve `shouldSuppress` üretir.
- `NotificationPreviewStore.kt` başlık, gövde ve birleşik metni ayrı çıkarır.
- `AppNotificationListenerService.kt` sınıflandırıcıya bağlıdır ve şu geçici akışları yayınlar:
  - `smartNotifications`
  - `smartBadgeCounts`
  - `categoryCounts`
- `notification_events` tablosu, `NotificationEventDao`, `NotificationAnalyzer`, `NotificationReportViewModel` ve `NotificationReportScreen` mevcut.
- `SmartAccessCard` içinde Bildirimler sekmesi mevcut; ancak yalnız uygulama + sayı gösterir.
- `FolderTile` içinde rozet ve bildirim alt metni mevcut; ancak yeni akıllı kategorileri kullanmaz.
- `BadgeColorEngine` mevcut; fakat uygulama kategorisi/paket adına göre çalışır, bildirim kategorisine göre değil.
- Sınıflandırıcı ve önizleme için temel unit testler mevcut.

### Eksik veya yanlış bağlanmış olanlar

- Servis her olayda aktif bildirimleri birden fazla kez tarıyor.
- Akıllı durum `companion object StateFlow` içinde; repository sahibi yok.
- Skor motoru ayrı dosyada değil, sınıflandırıcı içinde. Şimdilik bu bilinçli tutulacak; gereksiz sınıf ayrımı yapılmayacak.
- Regex/keyword eşleşmesi `contains` tabanlı; kelime sınırı ve İngilizce kapsamı yetersiz.
- Akıllı kategori/skor metadata'sı geçmiş raporuna yazılmıyor.
- Smart Access, FolderTile ve rapor ekranları yeni `SmartNotification` akışını kullanmıyor.
- Ayarlardaki mevcut “Akıllı Bildirimler” bölümü aslında `SmartInsightWorker` günlük özetlerini yönetiyor; Smart Notification Engine ayarlarıyla aynı kavram değil.
- Tam Android Gradle build ve fiziksel cihaz kabul kanıtı henüz yok.

---

## 4. Hedef Mimari

```text
NotificationListenerService
        │
        ▼
NotificationPreviewStore
(title/body extraction + sanitize)
        │
        ▼
NotificationClassifierUseCase
(category + sensitive + score + suppress)
        │
        ▼
SmartNotificationRepository
(active snapshot / package counts / category counts / top items)
        ├──────────────► LauncherViewModel / SmartAccess
        ├──────────────► Folder state / badges / ticker
        └──────────────► Settings preview

Posted event metadata
        │
        ▼
NotificationEventDao
(package + time + category + score bucket + suppressed; NO TEXT)
        │
        ▼
NotificationAnalyzer / NotificationReportViewModel
```

### Repository API hedefi

```kotlin
interface SmartNotificationRepository {
    val activeNotifications: StateFlow<List<SmartNotification>>
    val actionablePackageCounts: StateFlow<Map<String, Int>>
    val categoryCounts: StateFlow<Map<NotificationCategory, Int>>
    val suppressedCount: StateFlow<Int>

    suspend fun replaceActive(items: List<SmartNotification>)
    suspend fun remove(notificationKey: String)
    suspend fun clearActive()
}
```

Repository `@Singleton` olmalı; servis ve ViewModel aynı instance'ı Hilt üzerinden kullanmalıdır.

---

# 5. Aşamalı Görevler

## Aşama 0 — Güvenli Başlangıç ve Kanıt Tabanı

### [ ] AK-0.1 — Başlangıç build'i ve test kanıtı

**Amaç:** Yeni geliştirmeden önce mevcut branch'in gerçekten derlendiğini kanıtlamak.

**Çalıştır:**

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

**Kabul kriteri:**

- İki komut da başarılı.
- Çıktı özeti bu dosyaya tarih ve commit SHA ile yazılmış.
- Başlangıçta mevcut olan uyarılar ayrıca kaydedilmiş.

**Bağımlılık:** Yok. Tüm kod görevlerinden önce yapılır.

---

## Aşama 1 — Engine Core

### [~] AK-1.1 — Kategori ve aktif bildirim modelleri

**Mevcut:** `NotificationCategory.kt` ve `SmartNotification.kt` yazıldı.

**Kontrol edilecek:**

- Domain modellerinde Android/Compose bağımlılığı bulunmaması.
- `SOCIAL` ve `OTHER` kategorilerinin görev dokümanında ve UI mapper'larında unutulmaması.
- `importanceScore` değerinin her zaman `0..100` aralığında olması.
- `title` ve `text` alanlarının Room entity veya backup modeline dönüştürülmemesi.

**Kabul kriteri:** Model unit testleri + compile başarılı.

---

### [~] AK-1.2 — On-device sınıflandırıcı

**Mevcut:** Türkçe anahtar kelimeler ve paket sinyalleriyle çalışan sınıflandırıcı yazıldı.

**Yapılacak:**

1. Anahtar kelime setlerini yalnız normalize edilmiş biçimde tut.
2. Türkçe yanında temel İngilizce terimleri ekle:
   - delivery: `shipped`, `out for delivery`, `delivered`, `order`
   - finance: `balance`, `transaction`, `payment`, `verification code`, `otp`
   - promotion: `discount`, `sale`, `coupon`, `offer`, `deal`
   - reminder: `reminder`, `meeting`, `appointment`, `starts in`
3. Kısa kelimelerde ham substring yerine kelime sınırı/phrase matcher kullan.
4. Paket eşleşmesinde genel `contains` yerine exact/prefix kuralı kullan.
5. Çakışma önceliklerini saf testlerle kilitle.

**Zorunlu çakışma testleri:**

- Banka uygulaması + “%50 kampanya” → `PROMOTION`
- Alışveriş uygulaması + “doğrulama kodu” → `FINANCE`
- WhatsApp + “toplantı tamamlandı” → `MESSAGING`
- Takvim + “toplantı 10 dakika sonra” → `REMINDER`
- Instagram + “giriş kodu” → `FINANCE` veya güvenlik yüksek öncelik; `SOCIAL` olmamalı
- Sistem güncellemesi → `SYSTEM`

**Kabul kriteri:** En az 30 fixture; kategori doğruluğu test setinde ≥ %90.

---

### [~] AK-1.3 — Önem skoru ve bastırma politikası

**Mimari karar:** Şimdilik skor fonksiyonu `NotificationClassifierUseCase` içinde kalır. Ayrı `NotificationPriorityScorer.kt` yalnız skor kuralları bağımsız büyürse çıkarılır.

**Yapılacak:**

- Skor girdileri açıkça belgelenir: kategori tabanı, Android priority, aciliyet, güvenlik, düşük değer, kullanıcı override.
- Promosyon bastırma yalnız AppOrganizer görünümünü etkiler.
- Finans/güvenlik bildirimi kampanya kelimesi içermiyorsa düşük skora düşemez.
- Skor nedeni debug amaçlı enum olarak üretilebilir; gerçek bildirim metni loglanmaz.

**Kabul kriteri:**

- Tüm sonuçlar `0..100`.
- Kritik güvenlik fixture'ları ≥ 80.
- Saf promosyon fixture'ları < 40.
- Normal mesajlar bastırılmaz.

**Güncel uygulama kanıtı — 2026-07-26:**

- Feature branch: `agent/smart-notification-classifier-hardening`
- Kod commit'i: `c928a032`
- Test commit'i: `d55db0c9`
- Kritik OTP/güvenlik olayları için minimum skor: `80`
- Promosyonlar için maksimum skor: `39`
- Android priority girdisi `-2..2` aralığına clamp edildi.
- Düşük değerli içerik için ayrı ceza; mesajlaşma paketleri için güven bonusu tanımlandı.
- Standalone Kotlin doğrulaması: **36 kategori fixture + 6 skor politikası geçti.**
- Tam `testDebugUnitTest` ve `assembleDebug`: **BEKLİYOR**
- Main merge onayı: **BEKLİYOR**

---

### [ ] AK-1.4 — Kullanıcı kuralı ve override modeli

**Yeni dosyalar:**

- `domain/models/NotificationRule.kt`
- `domain/usecase/notification/ApplyNotificationRulesUseCase.kt`

**Minimum model:**

```kotlin
data class NotificationRule(
    val packageName: String,
    val forcedCategory: NotificationCategory? = null,
    val alwaysShow: Boolean = false,
    val alwaysSuppressInAppOrganizer: Boolean = false,
    val hideContent: Boolean = false,
)
```

**Kural:** Kullanıcı tercihi otomatik sınıflandırmadan üstündür. Android sistem bildirimi yine iptal edilmez.

**Bağımlılık:** AK-1.2 ve AK-1.3.

---

## Aşama 2 — Service ve State Sahipliği

### [~] AK-2.1 — Listener entegrasyonu

**Mevcut:** Servis sınıflandırıcıyı çağırıyor ve akıllı geçici akışlar üretiyor.

**Eksik:**

- Her olayda `rebuildCounts()` ve `updatePreviewState()` ayrı ayrı aktif bildirim tarıyor.
- UI için kalıcı state sahibi yok.
- Hata `runCatching` ile sessizce yutuluyor; içerik yazmadan yalnız hata türü Timber'a gönderilmeli.

**Yapılacak:**

- Tek `rebuildActiveSnapshot()` fonksiyonu oluştur.
- `activeNotifications` bir kez okunup tek geçişte count + preview + classification üretilsin.
- Burst bildirimlerde 50–100 ms conflated refresh/debounce değerlendirilsin.
- `onNotificationRemoved` yalnız anahtar bazlı repository remove + gerekirse reconciliation yapsın.

**Kabul kriteri:** Bir posted callback için aktif bildirim listesi en fazla bir kez tam taranır.

---

### [ ] AK-2.2 — SmartNotificationRepository

**Yeni dosyalar:**

- `data/repository/SmartNotificationRepository.kt`
- `data/repository/InMemorySmartNotificationRepository.kt`
- Hilt binding/provider

**Yapılacak:**

- Servisteki yeni `companion object` akışları repository'ye taşınır.
- Eski `badgeCounts/latestTexts/previewItems/lastPostedAt` geçiş süresince korunabilir.
- Yeni UI yalnız repository/ViewModel üzerinden beslenir.
- Process yeniden başladığında repository aktif bildirim snapshot'ını listener bağlanınca yeniden kurar.

**Kabul kriteri:**

- Servis ve ViewModel aynı singleton repository instance'ını kullanır.
- Repository saf unit testlerle replace/remove/clear davranışlarını doğrular.
- UI kodu `AppNotificationListenerService.smartNotifications` alanını doğrudan okumaz.

**Bağımlılık:** AK-2.1.

---

### [ ] AK-2.3 — Okundu/aktif/geçmiş ayrımının bağlanması

**Dosyalar:**

- `UnreadNotificationModel.kt`
- `NotificationReadPrefs.kt`
- `LauncherViewModel.kt`
- `SmartNotificationRepository`

**Yapılacak:**

- Akıllı rozet sayısı = bastırılmamış + aktif + okunmamış kayıtlar.
- Uygulama launcher'dan açıldığında `lastReadAt` güncellenir.
- Geçmiş rapor sayıları rozet açılmasıyla silinmez.

**Kabul kriteri:** Uygulama açıldıktan sonra launcher rozeti sıfırlanır; 7 günlük rapor değişmez.

---

## Aşama 3 — İçeriksiz Room Metadata ve Analiz

### [~] AK-3.1 — Mevcut geçmiş kayıt altyapısı

**Mevcut:** `NotificationEvent(packageName, postedAt)`, DAO sorguları, 30 gün temizliği ve 7 günlük analiz var.

**Eksik:** Kategori, skor ve AppOrganizer'da bastırılma metadata'sı yok.

---

### [ ] AK-3.2 — NotificationEvent metadata migration

**Dosyalar:**

- `domain/models/NotificationEvent.kt`
- `data/local/NotificationEventDao.kt`
- `data/local/AppDatabase.kt`
- `AppDatabaseTest.kt`

**Eklenecek içeriksiz alanlar:**

- `category: String`
- `importanceScore: Int`
- `wasSuppressed: Boolean`
- `systemPriority: Int`

**Kesinlikle eklenmeyecek:** `title`, `text`, sender, OTP, tutar, hesap bilgisi.

**Migration:** Veritabanı mevcut sürümünden bir üst sürüme çıkarılır. Non-null kolonların SQL default değeri ile Room entity `@ColumnInfo(defaultValue = ...)` değeri birebir eşleşmelidir.

**DAO sorguları:**

- kategori dağılımı
- toplam bastırılan sayı
- önem aralığına göre sayı
- saatlik/gece dağılımı

**Kabul kriteri:**

- Eski v23 fixture DB yeni sürüme veri kaybetmeden açılır.
- Room schema doğrulaması başarılı.
- Veritabanında bildirim metni bulunmadığını doğrulayan test vardır.

**Bağımlılık:** AK-1.3.

---

### [ ] AK-3.3 — NotificationAnalyzer V2

**Yeni çıktılar:**

- `totalReceived`
- `actionableCount`
- `suppressedCount`
- `categoryDistribution`
- `highPriorityCount`
- `nightCount`
- `mostTalkative`
- `mostDistracting`
- `topPromotionSources`

**Kural:** İçerik rapora taşınmaz; yalnız metadata analizi yapılır.

---

## Aşama 4 — UI ve UX

### [ ] AK-4.1 — Folder smart badge mapper

**Dosyalar:**

- `FolderTile.kt`
- `NotificationCategoryUiMapper.kt` (yeni)

**Yapılacak:**

- Folder rozet sayısı `actionablePackageCounts` üzerinden hesaplanır.
- Rozet rengi klasördeki en yüksek öncelikli aktif bildirimin kategorisinden gelir.
- Çok renkli küçük noktalar yerine tek ana kategori rengi + toplam sayı kullanılır.
- Promosyon varsayılan olarak rozet sayısına girmez.

**Kabul kriteri:** Aynı klasörde finans + mesaj varsa finans öncelikli renk; sayı yalnız bastırılmamış kayıtları içerir.

---

### [ ] AK-4.2 — Smart Access Bildirimler sekmesi V2

**Dosyalar:**

- `SmartAccessModels.kt`
- `SmartAccessCard.kt`
- `LauncherViewModel.kt`

**Yeni UI:**

```text
3 Mesaj · 1 Kargo · 1 Finans
[WhatsApp 3] [Trendyol 1] [Akbank 1]
2 promosyon filtrelendi
```

**Kabul kriteri:**

- Kategori özeti gösterilir.
- Hassas içerik açıkça gösterilmez.
- Promosyon filtre sayısı ayrı ve nötr biçimde görünür.
- İzin yok/veri yok/yükleniyor durumları korunur.

---

### [ ] AK-4.3 — Folder smart ticker

**Yapılacak:**

- En yüksek skorlu, bastırılmamış, okunmamış aktif bildirim seçilir.
- Hassas ise içerik yerine `Akbank · Güvenlik bildirimi` gibi maskeli metin gösterilir.
- Aynı klasörde iki alt satır oluşmaz; kullanılmayan uygulama bilgisi ile ticker aynı anda gösterilmez.

**Kabul kriteri:** Klasör altı alan taşmaz; en fazla 2 satır; dokununca doğru uygulama açılır.

---

## Aşama 5 — Ayarlar, Gizlilik ve Rapor

### [ ] AK-5.1 — Smart Notification Engine ayarları

**Mimari karar:** Mevcut `SmartInsightWorker` bölümü “Akıllı Özetler” olarak yeniden adlandırılır. Yeni motor ayrı “Bildirim Filtreleme” bölümü olur.

**Tercihler:**

- Motor açık/kapalı
- Promosyonları AppOrganizer rozetinden düşür
- Hassas içeriği gizle
- Gösterilecek kategoriler
- Kategori bazlı rozet / klasik uygulama rozeti

**Varsayılanlar:**

- Motor: açık
- Promosyon filtresi: açık
- Hassas içerik gizleme: açık
- Sistem bildirimini iptal etme: hiçbir zaman

**Uygulama notu:** Büyük `AppPrefs.kt` yerine ayrı `SmartNotificationPrefs.kt` tercih edilir; mevcut backup/import politikasına açıkça eklenir.

---

### [ ] AK-5.2 — Notification Report V2

**Eklenecek bölümler:**

- Bugün / 7 gün toplam bildirim
- Eyleme değer / filtrelenen oran
- Kategori dağılımı
- En çok bölen uygulamalar
- Gece bildirimleri
- Öneri: uygulamayı sessize alma veya uygulama bazlı AppOrganizer filtresi

**Kural:** AppOrganizer Android kanal ayarını kendiliğinden değiştirmez; kullanıcı sistem ayarına yönlendirilir.

---

## Aşama 6 — Test ve Doğrulama

### [ ] AK-6.1 — Unit test tamamlama

**Testler:**

- 30+ classifier fixture
- skor sınırları ve sistem priority etkisi
- promotion suppression
- sensitive masking
- repository replace/remove/clear
- analyzer category aggregation
- AppPrefs migration testleri

**Minimum fixture:** 30 sınıflandırma + 10 çakışma + 10 gizlilik/maskeleme senaryosu.

---

### [ ] AK-6.2 — Service entegrasyon testleri

**Testler:**

- posted → repository snapshot güncellenir
- removed → ilgili key ve counts düşer
- listener reconnect → snapshot yeniden kurulur
- ongoing bildirim politika testi
- promosyon → normal count var, actionable count yok
- uygulama okundu → smart badge 0, geçmiş rapor korunur
- analytics kapalı → aktif akıllı UI çalışabilir; kalıcı event yazılmaz

---

### [ ] AK-6.3 — Gizlilik denetimi

**Aranacak:**

```text
NotificationEvent
notification_events
DiagnosticsReportManager
BackupManager
Firebase
Timber
Log.
```

**Kabul kriteri:** Başlık/gövde/sender/OTP/tutar; DB, backup, telemetry, Crashlytics custom key veya log içinde bulunmaz.

---

### [ ] AK-6.4 — Performans ölçümü

**Ölçülecek:**

- tek bildirim sınıflandırma p50/p95
- 25/50/100 aktif bildirim snapshot süresi
- burst sırasında main-thread jank
- bellek: 100 aktif `SmartNotification`
- listener reconnect süresi

**Başlangıç hedefleri:**

- Tek sınıflandırma p95 < 2 ms
- 100 aktif bildirim snapshot p95 < 16 ms veya background thread'de tamamlanma
- Bildirim gelişi sırasında gözle görülür frame drop olmaması
- Metin cache'i servis bağlantısı kesilince temizlenmesi

**Kural:** Sonuçlar cihaz/model/API seviyesiyle raporlanır.

---

### [ ] AK-6.5 — Fiziksel cihaz kabul matrisi

| Test grubu | Minimum |
|---|---:|
| Android sürümü | Android 10/12/14/15 veya erişilebilen en yakın dağılım |
| Telefon | En az 3 farklı üretici |
| Tablet | En az 1 cihaz |
| Uygulama örneği | WhatsApp/Telegram, Trendyol veya eşdeğer, banka test bildirimi, takvim, Instagram |
| Dil | Türkçe + İngilizce sistem dili |

**Senaryolar:** ekran kapalı/açık, çoklu WhatsApp mesajı, grup mesajı, OTP, banka kampanyası, kargo, promosyon, notification removal, uygulamayı açarak okuma, servis iznini kapatıp açma.

---

## 6. Uygulama Sırası — Değiştirilmemeli

1. `AK-0.1` başlangıç build/test
2. `AK-1.2` classifier hardening
3. `AK-1.3` scoring policy
4. `AK-2.1` tek snapshot taraması
5. `AK-2.2` repository
6. `AK-2.3` unread bağlantısı
7. `AK-3.2` Room metadata migration
8. `AK-3.3` analyzer V2
9. `AK-5.1` ayarlar ve feature flag
10. `AK-4.2` Smart Access
11. `AK-4.1` folder badge
12. `AK-4.3` folder ticker
13. `AK-5.2` rapor V2
14. `AK-6.1–6.5` tam doğrulama

> UI, repository ve ayarlar tamamlanmadan mevcut `smartBadgeCounts` doğrudan üretim rozetinin yerine geçirilmez.

---

## 7. Her Görev Sonunda Zorunlu Kanıt

Her tamamlanan görev altına şu blok eklenir:

```text
Durum: [x]
Branch:
Commit:
Değişen dosyalar:
Çalıştırılan testler:
Sonuç:
Fiziksel cihaz kanıtı:
Bilinen kalan risk:
Main merge onayı: BEKLİYOR / ONAYLANDI
```

Ardından sırasıyla:

1. Bu dosya güncellenir.
2. `MANAGEMENT/ROADMAP.md` ilgili satır güncellenir.
3. Tamamlandıysa `MANAGEMENT/HISTORY.md` kısa kanıtla güncellenir.
4. Hüseyin'e branch diff'i ve test sonucu sunulur.
5. Açık onay gelmeden `main`e merge edilmez.

---

## 8. İlk Devam Görevi

### Sıradaki görev: `AK-0.1 + AK-1.2`

**Kapsam:** Yalnız başlangıç test kanıtı ve classifier hardening.

**Bakılacak dosyalar:**

- `NotificationClassifierUseCase.kt`
- `NotificationClassifierUseCaseTest.kt`
- `NotificationCategory.kt`
- `SmartNotification.kt`
- Bu görev dosyası

**Yapılmayacak:**

- UI değişikliği
- Room migration
- `main` push/merge
- Bildirim iptali
- Harici API/LLM

**Çıktı:** 30+ fixture'lı doğrulanmış classifier, test sonucu ve onaya hazır branch diff'i.
