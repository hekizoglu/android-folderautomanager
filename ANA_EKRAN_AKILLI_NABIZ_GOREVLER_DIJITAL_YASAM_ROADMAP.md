# AppOrganizer — Ana Ekran Akıllı Nabız, Görevler ve Dijital Yaşam Roadmap'i

> **Oluşturma tarihi:** 2026-07-17  
> **Kapsam:** Ana ekrandaki kayan yazı/haber şeridi, görev sistemi ve Dijital Yaşam kartının tek bir ürün sistemi olarak yeniden düzenlenmesi.  
> **Ana hedef:** Kullanıcıya aynı bilgiyi üç yerde tekrar etmek yerine, doğru bilgiyi doğru bileşende; mevcut değer, hedef, kalan miktar, neden ve eylem ile göstermek.  
> **Uygulama yöntemi:** Yapay zekâ her çalışmada yalnızca bir döngüyü tamamlar. Kod, test ve gerekiyorsa gerçek cihaz doğrulaması bitmeden durum `✅ Tamamlandı` yapılmaz.  
> **Birleştirme notu:** Bu dosya daha sonra genel AppOrganizer roadmap'iyle birleştirilebilir. Bu üç özellik için uygulama tamamlanana kadar tek teknik çalışma kaynağı bu dosyadır.

---

# 0. Çalışma ve durum kuralları

Her döngü aşağıdaki alanları taşımalıdır:

- **Amaç**
- **Mevcut sorun / kök neden**
- **Nasıl yapılmalı**
- **Değişecek dosyalar**
- **Yeni dosyalar**
- **Testler**
- **Kabul kriterleri**
- **Bağımlılıklar**
- **Durum**

Durum değerleri:

- `⏳ Bekliyor`: Kodlama başlamadı.
- `🚧 Devam ediyor`: Kod başladı, test veya doğrulama bitmedi.
- `🟡 Kısmen tamamlandı`: Kod ve temel test var; gerçek cihaz, erişilebilirlik, dönem sonu veya regresyon kanıtı eksik.
- `✅ Tamamlandı`: Kod, test, kabul kriteri ve gerekli cihaz kanıtı tamamlandı.
- `⛔ Bloke`: Dış bağımlılık veya Android sistem kısıtı bekleniyor.

Tamamlanan bir döngünün durum satırı şu biçime çevrilmelidir:

```text
**Durum:** ✅ Tamamlandı — Döngü HXX — commit: <SHA> — tarih: YYYY-MM-DD
```

## Yapay zekâ çalışma protokolü

Her döngüde yapay zekâ:

1. Önce döngüde belirtilen mevcut dosyaları yeniden okumalıdır.
2. Repo güncel kodu roadmap'ten farklıysa roadmap'i değil gerçek kodu esas almalı, farkı durum notuna yazmalıdır.
3. Yalnız ilgili döngünün kapsamındaki dosyaları değiştirmelidir.
4. Yeni davranış için unit test veya UI testi eklemelidir.
5. Türkçe ve İngilizce string kaynaklarını birlikte güncellemelidir.
6. Kullanıcıya gösterilen uygulama, kişi, bildirim veya dosya adlarını Firebase'e göndermemelidir.
7. İlgisiz refactor, toplu formatlama veya mimari yeniden yazım yapmamalıdır.
8. Test geçmeden roadmap durumunu tamamlandı yapmamalıdır.
9. Her döngüyü ayrı commit olarak tamamlamalıdır.
10. Bir sonraki döngüye otomatik geçmemelidir.

Önerilen doğrulama komutları:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Cihaz gerektiren döngülerde ayrıca:

```bash
./gradlew connectedDebugAndroidTest
```

---

# 1. Ürün kararı

Ana ekranda üç bileşenin görevi birbirinden ayrılmalıdır.

## 1.1 Görevler kartı

Görevi:

> Kullanıcının bugün veya bu hafta ne yapması gerektiğini, şu an nerede olduğunu ve ne kadar kaldığını göstermek.

Örnek:

```text
Görevler                                      1/3
Ekran süresi: 1 sa. 30 dk. / 3 sa.
1 sa. 30 dk. kaldı
```

## 1.2 Dijital Yaşam kartı

Görevi:

> Kullanıcının genel dijital düzen ve denge durumunu tek skor, trend, güven seviyesi ve en önemli neden ile açıklamak.

Örnek:

```text
Dijital Yaşam                                  72
Geçen haftaya göre +4
En büyük etki: Bildirim yoğunluğu
```

## 1.3 Akıllı Nabız Şeridi

Görevi:

> O anda bilinmesi veya yapılması gereken en değerli tek bilgiyi göstermek.

Örnek:

```text
⚠ 8 uygulamanın kategorisi kontrol bekliyor · İncele
```

Şerit şu görevleri yapmamalıdır:

- Sürekli günaydın/iyi akşamlar mesajı göstermek.
- Klasörde kaç uygulama olduğunu her gün tekrar etmek.
- Görev kartındaki aynı ilerlemeyi sürekli tekrar etmek.
- Dijital Yaşam kartındaki ham skoru tekrar göstermek.
- Kullanıcıyı bir öneriyi reddettiği için cezalandırmak.

## 1.4 Tekrar önleme kuralı

Aynı bilgi aynı anda yalnızca birincil bileşende gösterilmelidir:

| Bilgi | Birincil yer | Şeritte gösterilme şartı |
|---|---|---|
| Günlük görev ilerlemesi | Görevler kartı | Yalnız risk, son süre veya yeni başarı varsa |
| Dijital Yaşam skoru | Dijital Yaşam kartı | Yalnız anlamlı değişim veya çözülebilir sorun varsa |
| Bildirim/izin/sınıflandırma uyarısı | Akıllı Nabız Şeridi | Eylem gerektiriyorsa |
| Saat, tarih, hava | Pulse Clock | Şeritte tekrar edilmez |
| Haftalık rapor | Dijital Yaşam kartı detayı / rapor | Hazır olduğunda bir kez duyurulabilir |

---

# 2. Mevcut kod incelemesi ve kritik bulgular

## 2.1 Dijital skor iki farklı motorla hesaplanıyor — P0

Şu anda iki farklı skor yolu vardır:

### Yol A — Yeni ve doğru kaynak

```text
PulseClockViewModel
  → PulseInput
  → DigitalPulseEngine.compute()
  → PulseClockWidget içindeki PulseScoreRing
```

İlgili dosyalar:

- `presentation/viewmodel/PulseClockViewModel.kt`
- `domain/usecase/pulse/DigitalPulseEngine.kt`
- `presentation/ui/launcher/PulseClockWidget.kt`

### Yol B — Eski ve hafif kaynak

```text
LauncherViewModel
  → TickerComposer.computeDigitalLifeScore()
  → LauncherViewModel._digitalLifeScore
  → HomeScreen DigitalScoreCard
  → TickerComposer skor haberi
```

İlgili dosyalar:

- `presentation/ui/launcher/LauncherViewModel.kt`
- `utils/TickerComposer.kt`
- `presentation/ui/launcher/HomeScreen.kt`
- `presentation/ui/launcher/HomeTickerRow.kt`

`TickerComposer.computeDigitalLifeScore()` sosyal/oyun uygulaması oranını doğrudan ceza olarak kullanmaktadır. `DigitalPulseEngine` ise yüksek sosyal/oyun kullanımının tek başına ceza olmadığını açıkça belirtir. Bu nedenle iki skor aynı cihazda farklı sonuç verebilir.

Ek olarak `WrappedSnapshotPrefs.kt` içindeki yorum, ticker'ın kendi skorunu hesaplamaması ve son `DigitalPulseEngine` skorunu okuması gerektiğini söylemektedir; mevcut kod bu kuralla çelişmektedir.

**Karar:** Bütün skor tüketicileri yalnız `DigitalPulseEngine` tarafından üretilen tek snapshot'ı kullanacaktır.

## 2.2 Skor ana ekranda birden fazla yerde tekrar ediyor

Şu anda skor:

1. Pulse Clock içindeki halka,
2. Görevler kartının yanındaki ayrı `DigitalScoreCard`,
3. Zaman zaman ticker metni

olarak gösterilebilir.

**Karar:** Ana skoru gösteren tek birincil bileşen `Dijital Yaşam Kartı` olacaktır. Pulse Clock saat/tarih/hava işlevine sadeleşecek. Akıllı Nabız Şeridi ham skoru göstermeyecek; yalnız anlamlı değişimi veya eylem gerektiren nedeni gösterecektir.

## 2.3 Görevler yalnız tamamlandı/tamamlanmadı bilgisi taşıyor

Mevcut `MissionsViewModel.MissionUi` şu alanlarla sınırlıdır:

```kotlin
id
title
starReward
completed
autoCheckable
```

Bu model şunları gösteremez:

- Şu anki değer
- Hedef değer
- Kalan miktar
- İlerleme oranı
- Gün/hafta bitiş süresi
- Veri tazeliği
- Görevi yapacağı ekrana giden eylem
- Riskli veya dönem sonunu bekleyen durum

## 2.4 Dönemsel görevler erken tamamlanıyor — P0

Mevcut `MissionEngine.checkProgress()` anlık değeri doğrudan başarı kabul eder:

- Ekran süresi şu an 3 saatten düşükse başarı.
- Kilit açma şu an 30'dan düşükse başarı.
- 23:00 sonrası kullanım henüz yoksa başarı.
- Mevcut haftanın şu ana kadarki süresi geçen haftanın toplamından düşükse başarı.

`MissionsViewModel.computeAndAward()` görev ekranı açıldığında bu başarıları kalıcı yıldız olarak yazar. Günün veya haftanın kalanında hedef ihlal edilse bile yıldız geri alınmaz.

**Karar:** Üst sınır ve dönem karşılaştırma görevleri dönem bitmeden tamamlanmayacaktır.

## 2.5 Haftalık görev sınırı ISO haftası değil — P0

Mevcut kod `epochDay / 7` kullanmaktadır. Bu kullanıcıların beklediği pazartesi–pazar takvim haftası değildir.

**Karar:** Projede mevcut `WeekUtils` haftalık sınırlar için tek doğruluk kaynağı olacaktır.

## 2.6 Görev puan dengesi bozuk

`TaskScoreManager` içinde toplu öneriler uygulama sayısıyla doğrusal çarpılabilir. Örneğin 10 uygulamalık klasör önerisi +50 puan üretebilir. Ayrıca öneriyi reddetme veya erteleme negatif puan verir.

**Karar:**

- Toplu ödüller kademeli ve maksimum sınırlı olacaktır.
- Öneriyi reddetme veya erteleme puanı düşürmeyecektir.
- Görev etkisi Dijital Yaşam skoruna en fazla ±10 katkı vermeye devam edecektir.

## 2.7 Ticker içerik havuzu fazla ve düşük değerli

Mevcut `TickerComposer` şunları aynı listede üretmektedir:

- Bildirim özeti
- Saat bazlı selamlamalar
- Unutulan uygulamalar
- Günün şampiyonu
- Beş büyük klasör istatistiği
- İçgörü kartları
- Belirsiz sınıflandırma
- Özellik ipucu
- Haftalık özet
- Dijital skor

Bu yapı ana ekranı kullanıcıya hizmet eden bir alan yerine dönen bir içerik panosuna çevirebilir.

**Karar:** Şerit en fazla üç yüksek değerli öğe taşıyacak; yararlı öğe yoksa tamamen gizlenecektir.

---

# 3. Hedef mimari

```text
                         ┌────────────────────────────┐
                         │ HomeIntelligenceCoordinator│
                         └──────────────┬─────────────┘
                                        │
                 ┌──────────────────────┼──────────────────────┐
                 │                      │                      │
       ┌─────────▼─────────┐  ┌────────▼────────┐  ┌─────────▼────────┐
       │ DigitalPulseRepo  │  │ MissionRuntime  │  │ SmartTickerEngine │
       │ tek skor kaynağı  │  │ görev/ilerleme  │  │ seçim/sıralama    │
       └─────────┬─────────┘  └────────┬────────┘  └─────────┬────────┘
                 │                      │                      │
       ┌─────────▼─────────┐  ┌────────▼────────┐  ┌─────────▼────────┐
       │ Dijital Yaşam Kartı│  │ Görevler Kartı │  │ Akıllı Nabız      │
       └───────────────────┘  └─────────────────┘  └──────────────────┘
```

## 3.1 Tek skor snapshot modeli

```kotlin
data class DigitalPulseSnapshot(
    val totalScore: Int?,
    val baseScore: Int?,
    val taskContribution: Int,
    val previousScore: Int?,
    val scoreDelta: Int?,
    val organization: Int?,
    val attention: Int?,
    val balance: Int?,
    val cleanup: Int?,
    val consistency: Int?,
    val confidence: DataConfidence,
    val topReason: PulseScoreReason?,
    val computedAt: Long,
    val validUntil: Long,
    val missingSignals: Set<PulseSignal>,
)
```

## 3.2 Görev çalışma modeli

```kotlin
data class MissionRuntimeState(
    val missionId: String,
    val periodType: MissionPeriodType,
    val periodStartAt: Long,
    val periodEndAt: Long,
    val status: MissionStatus,
    val progressKind: MissionProgressKind,
    val currentValue: Long?,
    val targetValue: Long?,
    val remainingValue: Long?,
    val progressFraction: Float?,
    val currentLabel: String?,
    val remainingLabel: String?,
    val progressLabel: String?,
    val lastUpdatedAt: Long?,
    val freshness: MissionDataFreshness,
    val action: MissionAction?,
    val starReward: Int,
)
```

## 3.3 Akıllı şerit öğesi

```kotlin
data class SmartTickerItem(
    val id: String,
    val type: SmartTickerType,
    val title: String,
    val subtitle: String? = null,
    val icon: String,
    val priority: Int,
    val createdAt: Long,
    val expiresAt: Long?,
    val action: TickerAction?,
    val suggestionKey: String,
    val autoAdvanceAllowed: Boolean,
    val sensitive: Boolean = false,
)
```

## 3.4 Birleşik ana ekran durumu

```kotlin
data class HomeIntelligenceUiState(
    val missionSummary: HomeMissionSummary?,
    val pulseSummary: HomePulseSummary?,
    val tickerItems: List<SmartTickerItem>,
    val loading: Boolean,
)
```

---

# 4. Uygulama sırası

Döngüler şu sırayla uygulanmalıdır:

```text
H00 → H01 → H02 → H03 → H04
             ↓
M00 → M01 → M02 → M03 → M04 → M05 → M06 → M07 → M08
             ↓
D00 → D01 → D02 → D03 → D04
             ↓
T00 → T01 → T02 → T03 → T04 → T05
             ↓
U00 → U01 → U02 → U03 → U04
```

- `H`: Hazırlık ve ortak mimari
- `M`: Görev sistemi
- `D`: Dijital Yaşam sistemi
- `T`: Akıllı Nabız Şeridi
- `U`: Birleştirme, telemetri, test ve yayın

P0 doğruluk işleri bitmeden görsel iyileştirmeye geçilmemelidir.

---

# 5. Hazırlık ve ortak mimari döngüleri

## Döngü H00 — Mevcut davranışı testlerle kilitle

**Amaç:** Refactor öncesi mevcut hesap ve UI yollarının nereden geldiğini testlerle belgelemek.

**Mevcut sorun / kök neden:** Skor ve görev üretimi birden fazla ViewModel ve helper içinde dağılmıştır. Doğrudan refactor regresyon yaratabilir.

**Nasıl yapılmalı:**

1. `TickerComposer.computeDigitalLifeScore()` için mevcut davranışı gösteren test eklenmeli; daha sonra bu test kaldırılacak/deprecated olacaktır.
2. `DigitalPulseEngine.compute()` için aynı örnek girdide beklenen tek skor kaynağı test edilmelidir.
3. `MissionsViewModel` erken ödül problemini kanıtlayan testler eklenmelidir:
   - Sabah ekran süresi 90 dakika → ödül verilmemeli.
   - Saat 20:00 → gece görevi ödüllendirilmemeli.
   - Pazartesi mevcut hafta 60 dakika, geçen hafta 1000 dakika → haftalık ödül verilmemeli.
4. Ana ekranda aynı skorun halka ve kartta tekrarlandığını gösteren UI snapshot/test notu eklenmelidir.

**Değişecek dosyalar:**

- `app/src/test/.../TickerComposerTest.kt`
- `app/src/test/.../DigitalPulseEngineTest.kt`
- `app/src/test/.../MissionEngineTest.kt`
- Gerekirse yeni `MissionsViewModelTest.kt`

**Testler:** Yukarıdaki regresyon senaryoları.

**Kabul kriterleri:** P0 sorunları testlerle görünür hâle gelir; sonraki döngüler bu testleri doğru davranışa çevirir.

**Bağımlılıklar:** Yok.

**Durum:** ✅ Tamamlandı — Döngü H00 — commit: 275851a — tarih: 2026-07-17

---

## Döngü H01 — Tek zaman ve hafta çözümleyicisi oluştur

**Amaç:** Günlük ve haftalık bütün görev/skor hesaplarının aynı yerel zaman sınırlarını kullanması.

**Mevcut sorun / kök neden:** `LocalDate.now()`, `epochDay / 7`, `System.currentTimeMillis()/dayMs` ve `Calendar` farklı yerlerde kullanılmaktadır.

**Nasıl yapılmalı:**

Yeni saf sınıf:

```kotlin
class PeriodBoundaryResolver(
    private val clock: Clock,
    private val zoneId: ZoneId,
) {
    fun currentDay(): PeriodBoundary
    fun currentIsoWeek(): PeriodBoundary
    fun previousIsoWeek(): PeriodBoundary
    fun nextLocalMidnight(): Instant
    fun nextWeekBoundary(): Instant
}
```

`PeriodBoundary`:

```kotlin
data class PeriodBoundary(
    val startInclusive: Long,
    val endExclusive: Long,
    val epochDay: Long,
    val weekStartEpochDay: Long?,
)
```

Kurallar:

- Haftanın başlangıcı pazartesi olmalıdır.
- Sabit `24 * 60 * 60 * 1000` ile yerel gün sınırı hesaplanmamalıdır.
- Yaz/kış saati değişiminde 23 veya 25 saatlik günler doğru çalışmalıdır.
- Mevcut `WeekUtils` davranışı korunmalı veya resolver içinde tekleştirilmelidir.

**Değişecek dosyalar:**

- `utils/WeekUtils.kt`
- `MissionsViewModel.kt`
- `MissionsRepository.kt`
- `WrappedSnapshotPrefs.kt` içindeki gerekli zaman hesapları

**Yeni dosyalar:**

- `domain/time/PeriodBoundaryResolver.kt`
- `domain/time/PeriodBoundary.kt`
- Test dosyası

**Testler:**

- Pazartesi 00:00 hafta başlangıcı.
- Pazar 23:59 aynı hafta.
- Zaman dilimi değişimi.
- DST günü.
- Gece yarısı sınırı.

**Kabul kriterleri:** Görev, rapor ve trend hesapları aynı dönem sınırını kullanabilir.

**Bağımlılıklar:** H00.

**Durum:** ✅ Tamamlandı — Döngü H01 — commit: c5a158b — tarih: 2026-07-17 — Not: Mission/Wrapped tarafında `epochDay/7` → ISO geçişi kalıcı veri anahtarı riski nedeniyle M00-M02'ye bırakıldı; resolver şimdilik yeni altyapı + WeekUtils delegasyonu.

---

## Döngü H02 — HomeIntelligenceCoordinator iskeletini oluştur

**Amaç:** Ana ekranın görev, skor ve şerit verisini üç ayrı dağınık kaynaktan değil tek koordinatörden alması.

**Nasıl yapılmalı:**

Yeni koordinatör yalnız orkestrasyon yapmalıdır; skor veya görev kuralı hesaplamamalıdır.

```kotlin
@Singleton
class HomeIntelligenceCoordinator @Inject constructor(
    private val digitalPulseRepository: DigitalPulseRepository,
    private val missionRuntimeRepository: MissionRuntimeRepository,
    private val smartTickerEngine: SmartTickerEngine,
) {
    val state: StateFlow<HomeIntelligenceState>
    suspend fun refresh(reason: RefreshReason)
}
```

`RefreshReason`:

```kotlin
APP_START
HOME_RESUME
MISSION_EVENT
NOTIFICATION_EVENT
APP_CATALOG_CHANGED
MANUAL_REFRESH
```

Kurallar:

- Aynı anda birden fazla refresh çalışmamalıdır.
- Son başarılı snapshot korunmalıdır; geçici hata UI'yı boşaltmamalıdır.
- Ağ bağımlılığı olmamalıdır.
- IO işlemleri ana thread dışında çalışmalıdır.

**Değişecek dosyalar:**

- Hilt modülleri
- `LauncherViewModel.kt`
- İlerleyen döngülerde `PulseClockViewModel.kt`

**Yeni dosyalar:**

- `domain/home/HomeIntelligenceCoordinator.kt`
- `domain/home/HomeIntelligenceState.kt`
- `domain/home/RefreshReason.kt`

**Testler:**

- Eşzamanlı refresh tek işe düşer.
- Bir kaynak hata verince diğer iki kaynak korunur.
- Son başarılı state geçici hata sırasında kaybolmaz.

**Kabul kriterleri:** Ana ekranın birleşik veri akışı için test edilebilir iskelet oluşur.

**Bağımlılıklar:** H01.

**Durum:** ✅ Tamamlandı — Döngü H02 — commit: 575c56d — tarih: 2026-07-17 — Not: Üç kaynak (DigitalPulseRepository/MissionRuntimeRepository/SmartTickerEngine) minimal interface + no-op binding olarak eklendi; gerçek implementasyonlar D00/M/T döngülerinde bu sözleşmelere bağlanacak. Placeholder state tipleri §3.1-3.3 modellerine evrilecek.

---

## Döngü H03 — Ortak veri tazeliği modeli

**Amaç:** Kullanıcıya eski veri yeniymiş gibi gösterilmemesi.

**Nasıl yapılmalı:**

```kotlin
enum class DataFreshness {
    LIVE,
    RECENT,
    STALE,
    UNAVAILABLE,
}
```

Önerilen sınırlar:

- `LIVE`: Son 5 dakika.
- `RECENT`: Son 30 dakika.
- `STALE`: 30 dakikadan eski.
- `UNAVAILABLE`: İzin/veri yok veya hesap başarısız.

Görev ve skor snapshot'ları `computedAt/lastUpdatedAt` taşımalıdır.

UI kuralları:

- `STALE`: “Son güncelleme 42 dk önce” gösterilebilir.
- `UNAVAILABLE`: Sıfır skor veya sıfır kullanım gösterilmez.
- Kullanım erişimi yoksa CTA sunulur.

**Yeni dosyalar:**

- `domain/common/DataFreshness.kt`
- `domain/common/DataFreshnessResolver.kt`

**Testler:** Sınır değerler ve clock enjeksiyonu.

**Kabul kriterleri:** Üç bileşen aynı tazelik dilini kullanır.

**Bağımlılıklar:** H01.

**Durum:** ✅ Tamamlandı — Döngü H03 — commit: 520ad47 — tarih: 2026-07-17 — Not: Ekran/ViewModel entegrasyonu bilinçli ertelendi (M02/D00); bu döngüde sadece ortak model + resolver + Hilt binding.

---

## Döngü H04 — Güvenli hata ve fallback modeli

**Amaç:** Tek bir veri kaynağı başarısız olduğunda ana ekranın çökmesini veya tamamen boşalmasını önlemek.

**Nasıl yapılmalı:**

```kotlin
sealed interface HomeDataResult<out T> {
    data class Ready<T>(val value: T): HomeDataResult<T>
    data class Stale<T>(val value: T, val warningCode: String): HomeDataResult<T>
    data class Missing(val reason: MissingReason): HomeDataResult<Nothing>
    data class Failed(val errorCode: String): HomeDataResult<Nothing>
}
```

Serbest hata metni yerine sabit hata kodları kullanılmalıdır.

Örnek kodlar:

```text
USAGE_PERMISSION_MISSING
NOTIFICATION_DATA_UNAVAILABLE
PULSE_COMPUTE_FAILED
MISSION_METRIC_STALE
MISSION_SETTLEMENT_FAILED
```

**Değişecek dosyalar:** İlerleyen repository ve ViewModel dosyaları.

**Testler:** Bir kaynağın hata verdiği her kombinasyon.

**Kabul kriterleri:** Ana ekran hata durumlarında açıklayıcı ve güvenli kalır.

**Bağımlılıklar:** H02, H03.

**Durum:** ✅ Tamamlandı — Döngü H04 — commit: 88ed0c6 — tarih: 2026-07-17 — Faz kapanışı: tam test + assembleDebug yeşil, APK 27.01 MB Telegram'a gönderildi, v1.3.83 (106). Koordinatör kaynakları HomeDataResult ile sarıldı; diğer ViewModel/Repository entegrasyonları ilerleyen döngülerde.

---

# 6. Görev sistemi döngüleri

## Döngü M00 — MissionStatus ve dönemsel sonuç mantığı

**Amaç:** `completed: Boolean` yerine gerçek görev yaşam döngüsü kullanmak.

**Nasıl yapılmalı:**

```kotlin
enum class MissionStatus {
    DATA_UNAVAILABLE,
    NOT_STARTED,
    IN_PROGRESS,
    SAFE,
    AT_RISK,
    AWAITING_SETTLEMENT,
    COMPLETED,
    FAILED,
}
```

Kurallar:

- Eylem sayısı görevleri hedefe ulaşınca anında `COMPLETED` olabilir.
- Üst sınır görevleri dönem bitmeden `COMPLETED` olamaz.
- Üst sınır aşılırsa dönem bitmeden `FAILED` olabilir.
- Gece kullanmama görevi 23:00 öncesi `NOT_STARTED`, sonrasında `SAFE/FAILED` olur.
- Haftalık karşılaştırma hafta bitmeden `COMPLETED` olamaz.

`MissionEngine.checkProgress()` boolean döndürmek yerine yapılandırılmış değerlendirme döndürmelidir:

```kotlin
data class MissionEvaluation(
    val status: MissionStatus,
    val currentValue: Long?,
    val targetValue: Long?,
    val remainingValue: Long?,
    val failureReasonCode: String? = null,
)
```

**Değişecek dosyalar:**

- `domain/usecase/missions/MissionEngine.kt`
- `presentation/viewmodel/MissionsViewModel.kt`
- `app/src/test/.../MissionEngineTest.kt`

**Yeni dosyalar:**

- `MissionStatus.kt`
- `MissionEvaluation.kt`

**Testler:**

- Sabah 90/180 dakika → `IN_PROGRESS`, ödül yok.
- 180/180 → `FAILED`.
- Saat 20:00 gece görevi → `NOT_STARTED`.
- Saat 23:30 kullanım yok → `SAFE` fakat gün sonuna kadar ödül yok.
- Haftalık karşılaştırma hafta ortasında → `IN_PROGRESS`.

**Kabul kriterleri:** Dönemsel görevlerde erken yıldız verme tamamen engellenir.

**Bağımlılıklar:** H01.

**Durum:** ✅ Tamamlandı — Döngü M00 — commit: 3f95210 — tarih: 2026-07-17 — Not: Anında yıldız sadece eylem görevlerinde; üst sınır/gece/haftalık görevlerin dönem sonu sonuçlandırması M04'te. checkProgress deprecated köprü olarak duruyor. epochDay/7 hafta anahtarına dokunulmadı (M01).

---

## Döngü M01 — Görev örneklerini veritabanında sabitle

**Amaç:** Gün içinde görev listesinin veri/izin değişimine göre değişmemesi ve kişiselleştirilmiş hedefin korunması.

**Mevcut sorun / kök neden:** Görevler her refresh'te deterministik olarak yeniden üretilse de uygunluk verisi değişirse aynı gün görev seti değişebilir. Gelecekte kişisel hedefler üretildiğinde hedefin sabit saklanması gerekir.

**Nasıl yapılmalı:**

Yeni Room entity:

```kotlin
@Entity(
    tableName = "mission_instances",
    indices = [
        Index(value = ["periodType", "periodStartEpoch"]),
        Index(value = ["missionId", "periodType", "periodStartEpoch"], unique = true),
    ]
)
data class MissionInstanceEntity(
    @PrimaryKey val instanceId: String,
    val missionId: String,
    val periodType: String,
    val periodStartEpoch: Long,
    val periodStartAt: Long,
    val periodEndAt: Long,
    val targetValue: Long?,
    val baselineValue: Long?,
    val starReward: Int,
    val status: String,
    val assignedAt: Long,
    val settledAt: Long?,
    val definitionVersion: Int,
)
```

DAO fonksiyonları:

```text
getInstancesForPeriod
observeActiveInstances
insertAllIgnore
updateStatus
settleInstance
getUnsettledBefore
clearAll
```

Migration:

- Mevcut `AppDatabase` sürümü doğrulanmalı.
- Yeni tablo ekleyen açık migration yazılmalı.
- Destructive migration kullanılmamalıdır.
- Mevcut `mission_history` tamamlanma/yıldız ledger'ı olarak korunmalıdır.

**Değişecek dosyalar:**

- `data/local/AppDatabase.kt`
- `data/repository/MissionsRepository.kt`
- Room schema JSON

**Yeni dosyalar:**

- `domain/models/MissionInstanceEntity.kt`
- `data/local/MissionInstanceDao.kt`
- Migration testi

**Testler:**

- Aynı dönem ve görev ikinci kez eklenmez.
- Gün içinde hedef değişmez.
- Migration eski görev/yıldız kayıtlarını korur.
- Process death sonrası görev seti aynıdır.

**Kabul kriterleri:** Atanmış görevler ve hedefler dönem boyunca sabit ve kalıcıdır.

**Bağımlılıklar:** M00.

**Durum:** ✅ Tamamlandı — Döngü M01 — commit: 36a19cc — tarih: 2026-07-17 — Not: Gerçek DB sürümü v17 idi (roadmap v12 sanıyordu) → migration 17→18 yazıldı, schemas/18.json git'te. Dual-write: instance'lar görev üretimine paralel yazılıyor, okuma yolu M02-M04'te taşınacak. Haftalık instance anahtarı ISO (yeni tablo temiz başladı); mission_history eski anahtarla ledger. room-testing altyapısı yok — MigrationTestHelper testi M08 faz kapanışında değerlendirilecek.

---

## Döngü M02 — MissionMetricSnapshotProvider

**Amaç:** Bütün mevcut görev değerlerini tek ve zaman tutarlı snapshot'ta toplamak.

**Nasıl yapılmalı:**

```kotlin
data class MissionMetricSnapshot(
    val capturedAt: Long,
    val screenTimeMinutesToday: Long?,
    val unlockCountToday: Int?,
    val usedAfter23Today: Boolean?,
    val firstUseAfter23At: Long?,
    val screenTimeMinutesThisWeek: Long?,
    val screenTimeMinutesPreviousWeek: Long?,
    val classificationActionsToday: Int,
    val notificationReportViewedToday: Boolean,
    val positiveActionsThisWeek: Int,
    val freshness: DataFreshness,
)
```

Provider adımları:

1. Tek `now` değeri alır.
2. `PeriodBoundaryResolver` ile gün ve hafta sınırlarını çözer.
3. UsageStats verisini bir kez okur.
4. Günlük ekran süresini global foreground değerinden hesaplar.
5. Kilit açma sayısını okur.
6. 23:00 sonrası ilk kullanım zamanını çıkarır.
7. TaskScore event sayaçlarını DAO üzerinden okur.
8. Snapshot döndürür.

`MissionsViewModel.buildCheckInput()` içindeki veri toplama bu provider'a taşınmalıdır.

**Değişecek dosyalar:**

- `MissionsViewModel.kt`
- `MissionsRepository.kt`
- Gerekirse `UsageStatsHelper.kt`

**Yeni dosyalar:**

- `MissionMetricSnapshot.kt`
- `MissionMetricSnapshotProvider.kt`
- Test dosyası

**Testler:**

- İzin yoksa kullanım metrikleri null, eylem sayaçları korunur.
- 0 kullanım gerçek `0`, veri yok `null` olarak ayrılır.
- Haftalık sınırlar ISO haftasıdır.
- İlk gece kullanımı doğru bulunur.

**Kabul kriterleri:** Görev değerlendirmesi tek snapshot üzerinden yapılır; ViewModel veri hesaplamaz.

**Bağımlılıklar:** H01, H03, M01.

**Durum:** ✅ Tamamlandı — Döngü M02 — commit: b87b055 — tarih: 2026-07-17 — Not: buildCheckInput'taki epochDay/7 haftalık sınır bug'ı provider'da ISO resolver sınırlarıyla düzeltildi. MissionCheckInput köprüyle korundu; MissionsRepository.buildTaskEventInput ölü kod kaldı (ileride temizlik).

---

## Döngü M03 — Görev ilerleme modeli ve formatlayıcı

**Amaç:** Her görevde mevcut durum, hedef ve kalan miktarı göstermek.

**Nasıl yapılmalı:**

```kotlin
enum class MissionProgressKind {
    UPPER_LIMIT,
    ACTION_COUNT,
    BOOLEAN_ACTION,
    AVOID_AFTER_TIME,
    PERIOD_COMPARISON,
}

data class MissionProgress(
    val currentValue: Long?,
    val targetValue: Long?,
    val remainingValue: Long?,
    val progressFraction: Float?,
    val exceededValue: Long?,
    val currentTextRes: MissionTextSpec?,
    val remainingTextRes: MissionTextSpec?,
    val progressTextRes: MissionTextSpec?,
)
```

UI'ya doğrudan Türkçe string verilmemelidir. `MissionTextSpec` resource id ve argüman taşımalıdır.

Örnek görevler:

### Ekran süresi

```text
Bugün ekran süreni 3 saatin altında tut
Şu an: 1 sa. 30 dk.
Kalan: 1 sa. 30 dk.
Limitin %50'si kullanıldı
```

### Kilit açma

```text
Bugün telefonu 30'dan az kez aç
Şu an: 12 / 30
Kalan: 18 açma
```

### Sınıflandırma

```text
Bugün 2 sınıflandırma kararını netleştir
Şu an: 1 / 2
Kalan: 1 uygulama
```

Kurallar:

- Negatif kalan değer gösterilmez; `exceededValue` kullanılır.
- Progress fraction `0f..1f` aralığında sınırlandırılır.
- Üst sınır görevinde dolu progress çubuğu başarı anlamına gelmemelidir; “limit kullanımı” etiketi olmalıdır.
- Süre formatı ortak formatter ile üretilmelidir.

**Yeni dosyalar:**

- `MissionProgress.kt`
- `MissionProgressKind.kt`
- `MissionProgressCalculator.kt`
- `MissionValueFormatter.kt`

**Değişecek dosyalar:**

- `MissionEngine.kt`
- `MissionsViewModel.kt`
- `values/strings.xml`
- `values-en/strings.xml`

**Testler:**

- 90/180 → kalan 90, fraction 0.5.
- 179/180 → kalan 1.
- 180/180 → exceeded 0 fakat hedef başarısız.
- 200/180 → exceeded 20.
- Null veri → metin yok, `DATA_UNAVAILABLE`.

**Kabul kriterleri:** Kullanıcı her ölçülebilir görevde şu anki değerini görebilir.

**Bağımlılıklar:** M00, M02.

**Durum:** ✅ Tamamlandı — Döngü M03 — commit: ab57061 — tarih: 2026-07-17 — Not: TextSpec resolve ViewModel'de (calculator saf Kotlin). MissionsScreen henüz yeni alanları kullanmıyor (M06). AVOID_AFTER_TIME sayısal metin üretmez — M06'da rozet/ikon UI'sı gerekir.

---

## Döngü M04 — Görev sonuçlandırma ve ödül servisi

**Amaç:** Yıldızın yalnız doğru zamanda ve bir kez verilmesi.

**Nasıl yapılmalı:**

Yeni use case:

```kotlin
class SettleMissionInstancesUseCase {
    suspend fun settleOverdue(now: Long): SettlementResult
    suspend fun completeActionMission(instanceId: String, event: MissionEvent): SettlementResult
}
```

Kurallar:

- Günlük limit görevleri gün dönemi bittikten sonra sonuçlandırılır.
- Haftalık karşılaştırma görevleri hafta bittikten sonra sonuçlandırılır.
- Eylem görevleri hedefe ulaşınca anında sonuçlanabilir.
- `mission_history` unique index'i ikinci ödülü engellemeye devam eder.
- Görev `FAILED` ise yıldız verilmez.
- Ödül yazımı ve instance status güncellemesi mümkünse `@Transaction` içinde yapılmalıdır.

WorkManager:

- `MissionSettlementWorker` tek seferlik iş olarak bir sonraki yerel dönem sınırına planlanır.
- Worker çalışınca gecikmiş bütün görevleri sonuçlandırır ve sonraki işi planlar.
- WorkManager tam zamanında çalışmazsa `HOME_RESUME` sırasında catch-up settlement yapılır.
- Exact alarm izni istenmez.

**Yeni dosyalar:**

- `SettleMissionInstancesUseCase.kt`
- `MissionSettlementWorker.kt`
- `MissionWorkScheduler.kt`

**Değişecek dosyalar:**

- `MissionsRepository.kt`
- `MissionHistoryDao.kt`
- `MissionInstanceDao.kt`
- Application başlangıç/scheduler kodu

**Testler:**

- Aynı görev iki defa settlement edilmez.
- Worker gecikse de catch-up doğru sonuç verir.
- Gün sonu 179/180 başarı, 180/180 başarısız.
- Hafta sonu mevcut < önceki başarı.
- Uygulama görev ekranı hiç açılmasa da yıldız yazılır.

**Kabul kriterleri:** Görev ekranının açılma zamanı ödülü etkilemez.

**Bağımlılıklar:** M01, M02, M03.

**Durum:** ✅ Tamamlandı — Döngü M04 — commit: 8422c48 — tarih: 2026-07-17 — Not: Geçmiş dönem metrikleri periodEndAt-1 anıyla sorgulanır (10 gün lookback); DATA_UNAVAILABLE'da 48 saat grace period (tazeyse sonraki catch-up'a bırakılır, eskiyse FAILED). Transaction runner soyutlamasıyla unit test edilebilir. UsageStats saklama süresi cihaza göre değişebilir — gerçek cihaz gözlemi U04 test matrisine not.

---

## Döngü M05 — Göreve özel eylemler ve route'lar

**Amaç:** Kullanıcının görevi tamamlayacağı ekrana tek dokunuşla gitmesi.

**Nasıl yapılmalı:**

```kotlin
sealed interface MissionAction {
    data object OpenClassificationReview : MissionAction
    data object OpenNotificationReport : MissionAction
    data object OpenUsageReport : MissionAction
    data object OpenSettingsUsageAccess : MissionAction
    data object None : MissionAction
}
```

Route çözümü UI içinde dağılmamalı; tek `MissionActionRouter` kullanılmalıdır.

Örnekler:

- Sınıflandırma görevi → `APP_LIST_UNCERTAIN`
- Bildirim raporu görevi → `NOTIFICATION_REPORT`
- Ekran süresi görevi → `USAGE_REPORT`
- Veri izni yok → Android kullanım erişimi ayarı

**Değişecek dosyalar:**

- `MissionsScreen.kt`
- `AppNavigation.kt`
- `MissionsViewModel.kt`

**Yeni dosyalar:**

- `MissionAction.kt`
- `MissionActionRouter.kt`

**Testler:** Her action doğru route/intent üretir; bilinmeyen action çökmez.

**Kabul kriterleri:** Eylem görevleri pasif metin değil, tamamlanabilir akış hâline gelir.

**Bağımlılıklar:** M03.

**Durum:** ✅ Tamamlandı — Döngü M05 — commit: 8b6da73 — tarih: 2026-07-17 — Not: Router domain'de Intent NESNESİ kurmaz (JVM test uyumu) — SystemIntent(intentAction) string taşır, Intent UI'da kurulur. Gerçek route adları: app_list?filter=uncertain, notification_report, usage_report.

---

## Döngü M06 — Görevler ekranını ilerleme odaklı yeniden tasarla

**Amaç:** Kullanıcı yalnız tik değil, ilerlemesini ve kalan hedefi görsün.

**Nasıl yapılmalı:**

`MissionUi` şu alanları taşımalıdır:

```kotlin
data class MissionUi(
    val id: String,
    val title: String,
    val status: MissionStatus,
    val currentText: String?,
    val remainingText: String?,
    val progressText: String?,
    val progressFraction: Float?,
    val deadlineText: String?,
    val actionLabel: String?,
    val action: MissionAction?,
    val starReward: Int,
)
```

Yeni `MissionRow` düzeni:

```text
[durum ikonu] Görev başlığı                         ⭐ 1
               Şu an: 1 sa. 30 dk.
               Kalan: 1 sa. 30 dk.
               [ilerleme çubuğu]
               Günün bitmesine 6 sa. 20 dk.   [Detay]
```

Durum dili:

- `SAFE`: “Hedef korunuyor”
- `AT_RISK`: “Limite yaklaştın”
- `AWAITING_SETTLEMENT`: “Dönem sonunda sonuçlanacak”
- `FAILED`: “Bugünkü hedef aşıldı”
- `DATA_UNAVAILABLE`: “Kullanım verisi alınamıyor” + CTA

Erişilebilirlik:

- Durum yalnız renkle anlatılmamalıdır.
- Progress semantics tanımlanmalıdır.
- Dokunma alanları minimum 48dp olmalıdır.
- Büyük yazıda metin kırpılmamalıdır.

**Değişecek dosyalar:**

- `MissionsScreen.kt`
- `MissionsViewModel.kt`
- `strings.xml` TR/EN

**Yeni dosyalar:** Gerekirse `MissionCard.kt`.

**Testler:** Compose UI testleri; büyük font; TalkBack contentDescription; boş/veri yok/yükleniyor.

**Kabul kriterleri:** Kullanıcı mevcut durumu ve kalan hedefi ekranda açıkça görür.

**Bağımlılıklar:** M03, M05.

**Durum:** ✅ Tamamlandı — Döngü M06 — commit: 02ffaa3 — tarih: 2026-07-17 — Not: MissionRow ayrı MissionCard.kt dosyasına çıkarıldı (300 satır kuralı). Compose UI test altyapısı projede olmadığından doğrulama saf Kotlin katmanında; Compose test altyapısı ihtiyacı U04 test matrisinde değerlendirilecek.

---

## Döngü M07 — Ana ekran Görevler kartını canlı hâle getir

**Amaç:** Sabit “Bugünün görevleri” metni yerine gerçek özet göstermek.

**Nasıl yapılmalı:**

Yeni model:

```kotlin
data class HomeMissionSummary(
    val completedCount: Int,
    val totalCount: Int,
    val primaryMissionId: String?,
    val primaryTitle: String?,
    val primaryCurrentText: String?,
    val primaryRemainingText: String?,
    val primaryStatus: MissionStatus?,
    val urgent: Boolean,
)
```

Birincil görev seçimi:

1. `AT_RISK`
2. Son süresi yaklaşan
3. Tek eylemle tamamlanabilecek
4. En yüksek ilerleme oranına sahip görev
5. İlk bekleyen görev

Kart örnekleri:

```text
Görevler                                      1/3
Ekran süresi: 1 sa. 30 dk. / 3 sa.
1 sa. 30 dk. kaldı
```

```text
Görevler                                      2/3
1 uygulama kategorisi kaldı
```

```text
Görevler                                      3/3
Bugünkü görevler tamamlandı ⭐
```

Davranış:

- Tıklama görev ekranını açar.
- `DATA_UNAVAILABLE` durumunda “Kullanım erişimi gerekli” gösterir.
- Missions kapalıysa kart gizlenir.
- Kart verisi `HomeIntelligenceCoordinator` üzerinden akar.

**Değişecek dosyalar:**

- `HomeScreen.kt`
- Mevcut görev kartı composable'ı
- `LauncherViewModel.kt`
- `AppPrefs.kt` yalnız gerekirse

**Yeni dosyalar:**

- `HomeMissionSummary.kt`
- `HomeMissionCard.kt`

**Testler:**

- 0/3, 1/3, 3/3.
- Riskli görev önceliği.
- Veri yok.
- Missions kapalı.
- Tablet ve dar ekran.

**Kabul kriterleri:** Ana ekrandan görev ilerlemesi tek bakışta anlaşılır.

**Bağımlılıklar:** H02, M06.

**Durum:** ✅ Tamamlandı — Döngü M07 — commit: bf2d34b — tarih: 2026-07-17 — Not: MissionSummaryUseCase tek hesap yolu (ViewModel + home kartı çift hesap yapmaz; awardStars bayrağıyla yan etki kontrolü). AppPrefs'te missions toggle yok — kart gizleme gate'i T05 ayarlar döngüsünde eklenebilir. "Tek eylem kaldı" kuralı progressFraction>=0.99 olarak yorumlandı.

---

## Döngü M08 — Görev puanı ve ödül dengesini düzelt

**Amaç:** Puanın kullanıcı davranışını adil yansıtması ve spam üretmemesi.

**Nasıl yapılmalı:**

Yeni önerilen puanlar:

```text
Sınıflandırma onayı                +2
Gerçek sınıflandırma düzeltmesi    +4
Klasör önerisi kabulü              +3..+8
Benzer uygulama toplu kabulü       +3..+10
Bildirim raporu görüntüleme         0 veya +1/gün
Öneriyi erteleme                    0
Öneriyi reddetme                    0
```

Toplu ağırlık doğrusal olmamalıdır:

```kotlin
fun bulkReward(itemCount: Int): Int = when (itemCount) {
    0 -> 0
    1 -> 3
    in 2..5 -> 5
    in 6..10 -> 7
    else -> 10
}
```

`ClassificationSnoozed`, `FolderSuggestionSnoozed`, `FolderSuggestionDismissed` negatif delta üretmemelidir. Eski kayıtlar geriye dönük silinmemeli; yeni olaylar sıfır veya kayıt dışı olmalıdır.

**Değişecek dosyalar:**

- `TaskScoreManager.kt`
- `AppListViewModel.kt`
- Testler

**Testler:**

- 100 uygulamalık toplu işlem +10'u aşmaz.
- Reddetme toplam skoru düşürmez.
- Bildirim raporu aynı gün bir kez sayılır.
- Dijital pulse katkısı yine ±10 sınırındadır.

**Kabul kriterleri:** Tek işlem diğer bütün davranışları gölgeleyemez; kullanıcı öneri reddettiği için cezalandırılmaz.

**Bağımlılıklar:** M04.

**Durum:** ✅ Tamamlandı — Döngü M08 — commit: c1bc9a4 — tarih: 2026-07-17 — M FAZI KAPANDI: tam test + assembleDebug yeşil, APK v1.3.85 (108) Telegram'a gönderildi. Eski kayıtlar silinmedi; sadece yeni olaylar yeni tabloyla puanlanır.

---

# 7. Dijital Yaşam kartı döngüleri

## Döngü D00 — Eski skor motorunu kaldır ve tek kaynağa geç

**Amaç:** Ana ekran, ticker ve raporların aynı skoru göstermesi.

**Nasıl yapılmalı:**

1. `TickerComposer.computeDigitalLifeScore()` kullanımı kaldırılmalıdır.
2. `LauncherViewModel._digitalLifeScore` ve buna bağlı günlük skor rotasyonu kaldırılmalıdır.
3. `TickerComposer.compose()` parametrelerinden eski `digitalLifeScore` ve `digitalLifeScorePrevious` kaldırılmalı veya yeni `HomePulseSummary` ile değiştirilmelidir.
4. `WrappedSnapshotPrefs.updateDailyScore()` eski ticker yolu için kullanılmamalıdır. Geriye uyumluluk için önce deprecated yapılabilir; kullanılmadığı doğrulandıktan sonra ayrı temizlik döngüsünde kaldırılabilir.
5. `DigitalPulseEngine` tek hesap motoru olarak kalmalıdır.
6. `WrappedViewModel`, `PulseClockViewModel`, ana ekran kartı ve ticker aynı `DigitalPulseRepository` snapshot'ını tüketmelidir.

Yeni repository:

```kotlin
@Singleton
class DigitalPulseRepository @Inject constructor(...) {
    val snapshot: StateFlow<HomeDataResult<DigitalPulseSnapshot>>
    suspend fun refresh(force: Boolean = false)
}
```

Repository mevcut `PulseClockViewModel.compute()` içindeki input hazırlama işini devralmalıdır.

**Değişecek dosyalar:**

- `TickerComposer.kt`
- `LauncherViewModel.kt`
- `PulseClockViewModel.kt`
- `WrappedViewModel.kt`
- `WrappedSnapshotPrefs.kt`
- Hilt modülleri

**Yeni dosyalar:**

- `DigitalPulseRepository.kt`
- `DigitalPulseSnapshot.kt`
- `PulseInputFactory.kt`

**Testler:**

- Aynı snapshot ana ekran kartı ve raporda aynı skoru verir.
- Ticker kendi skorunu hesaplamaz.
- Sosyal/oyun oranı tek başına ceza değildir.
- Repository 15 dakika cache uygular.
- Force refresh cache'i aşar.

**Kabul kriterleri:** Repoda çalışan tek skor hesaplama girişi `DigitalPulseEngine.compute()` olur.

**Bağımlılıklar:** H02, H03.

**Durum:** ✅ Tamamlandı — Döngü D00 — commit: a280408 — tarih: 2026-07-17 — P0 2.1 çözüldü: V1 motor (~90 satır) silindi, ticker skor öğesi kaldırıldı (T döngüleri şeridi yeniden ele alacak). WrappedEngine iç hesabı korunuyor ama gösterilen sayı repository snapshot'ından override ediliyor. updateDailyScore deprecated — D01'de silinecek.

---

## Döngü D01 — Skor trend ve baseline mantığını güvenilir hâle getir

**Amaç:** “Geçen haftaya göre +4” bilgisinin gerçek takvim haftasına dayanması.

**Mevcut sorun / kök neden:** `updateWeeklyPulseScore()` yedi gün geçtiğinde rotasyon yapar; takvim haftası sınırıyla birebir aynı değildir. Günlük ticker skoru için ayrıca eski bir rotasyon vardır.

**Nasıl yapılmalı:**

- Trend snapshot'ı ISO hafta anahtarıyla saklanmalıdır.
- `weekStartEpochDay` baseline anahtarı olmalıdır.
- Mevcut hafta içinde baseline değişmemelidir.
- Önceki tam haftanın kapanış skoru karşılaştırma olarak kullanılmalıdır.
- İlk hafta `scoreDelta = null`, “Veri birikiyor” gösterilmelidir.
- Günlük ham skor karşılaştırması kaldırılmalıdır.

Önerilen model:

```kotlin
data class PulseHistoryEntry(
    val weekStartEpochDay: Long,
    val closingScore: Int,
    val confidence: String,
    val computedAt: Long,
)
```

İlk faz SharedPreferences ile agregat saklanabilir; Room zorunlu değildir. Ancak anahtarlar açık ve migration kontrollü olmalıdır.

**Değişecek dosyalar:**

- `WrappedSnapshotPrefs.kt`
- `DigitalPulseRepository.kt`
- `WrappedViewModel.kt`

**Testler:**

- Aynı hafta baseline değişmez.
- Yeni pazartesi önceki kapanış doğru döner.
- İlk hafta delta null.
- Zaman dilimi değişimi haftayı bozmaz.

**Kabul kriterleri:** Trend gerçek takvim haftasını temsil eder.

**Bağımlılıklar:** H01, D00.

**Durum:** ✅ Tamamlandı — Döngü D01 — commit: e78415a — tarih: 2026-07-17 — Not: SharedPreferences saklama (Room gerekmedi), 8 hafta retention, atlanan haftalarda en son kapanış karşılaştırılır (null değil). Eski rotasyon + updateDailyScore silindi, tek seferlik migration bayrağı var.

---

## Döngü D02 — Dijital Yaşam kartını bilgi kartına dönüştür

**Amaç:** Mevcut “Skor 72 / Dijital yaşam” kartını açıklayıcı ve eyleme geçirilebilir yapmak.

**Nasıl yapılmalı:**

`DigitalScoreCard` adı `DigitalLifeCard` olarak değiştirilmelidir.

Yeni model:

```kotlin
data class HomePulseSummary(
    val score: Int?,
    val statusLabel: String,
    val delta: Int?,
    val deltaLabel: String?,
    val topReasonLabel: String?,
    val confidence: DataConfidence,
    val freshness: DataFreshness,
    val actionLabel: String?,
)
```

Kart düzeni:

```text
Dijital Yaşam                                 72
İyi · Geçen haftaya göre +4
En büyük etki: Bildirim yoğunluğu
```

Dar kart düzeni:

```text
Dijital Yaşam                  72  ↗ +4
Bildirim yoğunluğu
```

Durum etiketleri nötr olmalıdır:

```text
80–100  Çok iyi
65–79   İyi
50–64   Dengeli
35–49   Dikkat gerekiyor
0–34    İyileştirme alanı var
```

Skor kullanıcıyı ahlaki olarak yargılamamalıdır. “Kötü kullanıcı” benzeri dil kullanılmamalıdır.

Confidence davranışı:

- `HIGH`: normal skor.
- `MEDIUM`: “Tahmini skor” veya bilgi ikonu.
- `LOW`: sayı yerine “Veri birikiyor” tercih edilmelidir; nötr 60 kullanıcıya kesin skor gibi gösterilmemelidir.

Freshness davranışı:

- `STALE`: küçük “Son güncelleme 45 dk önce”.
- `UNAVAILABLE`: “Kullanım erişimi gerekli” CTA.

Tıklama:

- İlk faz mevcut `WRAPPED_REPORT` rotasını açar.
- Yeni ayrı detay ekranı gereksiz yere oluşturulmaz; mevcut rapor yeterli değilse ayrı roadmap maddesi açılır.

**Değişecek dosyalar:**

- `HomeTickerRow.kt` içindeki `DigitalScoreCard`
- `HomeScreen.kt`
- `strings.xml` TR/EN

**Yeni dosyalar:**

- `DigitalLifeCard.kt`
- `HomePulseSummary.kt`

**Testler:**

- Yüksek/orta/düşük confidence.
- Delta pozitif/negatif/null.
- Uzun top reason.
- Dar ekran/tablet.
- Büyük font.
- Kullanım izni yok.

**Kabul kriterleri:** Kullanıcı skorun yönünü ve en önemli nedenini tek bakışta anlar.

**Bağımlılıklar:** D00, D01.

**Durum:** ✅ Tamamlandı — Döngü D02 — commit: c7834a5 — tarih: 2026-07-17 — Not: Confidence engine'in mevcut computeConfidence'ından geliyor (yeni türetme gerekmedi). topReason = max |delta| (minimal; D04 PulseReasonPresenter tam yapacak). UNAVAILABLE'da tıklama kapalı.

---

## Döngü D03 — Pulse Clock içindeki skor tekrarını kaldır

**Amaç:** Ana ekranda skorun tek yerde görünmesi.

**Nasıl yapılmalı:**

Seçilen ürün kararı:

- `DijitalLifeCard` skorun birincil ve varsayılan gösterimidir.
- `PulseClockWidget` saat, tarih, hava ve isteğe bağlı kısa içgörü gösterir.
- `PulseScoreRing` varsayılan ana ekran düzeninden kaldırılır.
- `KEY_HOME_SCORE_VISIBLE` için migration uygulanmalıdır.

Migration seçenekleri:

1. Yeni `KEY_DIGITAL_LIFE_CARD_VISIBLE` eklenir ve varsayılan `true` olur.
2. Eski `KEY_HOME_SCORE_VISIBLE` mevcut kullanıcı tercihi olarak okunur:
   - Eski skor görünürse yeni kart görünür yapılır.
   - Pulse ring kapatılır.
3. `PulseScoreRing` kodu hemen silinmek yerine bir sürüm deprecated tutulabilir; hiçbir çağrı kalmadığı test edildikten sonra kaldırılır.

Saat kartının yüksekliği score ring kaldırıldıktan sonra yeniden değerlendirilmelidir. Klasör gridine daha fazla alan kazandırılması hedeflenmelidir.

**Değişecek dosyalar:**

- `PulseClockWidget.kt`
- `SettingsHomeScreenSection.kt`
- `AppPrefs.kt`
- `HomeScreen.kt`
- TR/EN strings

**Testler:**

- Skor ana ekranda tek kez görünür.
- Eski kullanıcı ayarı yeni karta migrate olur.
- Minimal/Pulse/Glass saat stilleri bozulmaz.
- Kompakt ekranda klasör alanı artar veya korunur.

**Kabul kriterleri:** Aynı ham skor saat, kart ve ticker'da tekrarlanmaz.

**Bağımlılıklar:** D02.

**Durum:** ✅ Tamamlandı — Döngü D03 — commit: 32f8edc — tarih: 2026-07-17 — Not: PulseScoreRing @Deprecated bırakıldı (U01 temizliğinde silinebilir). Saat kartı 20dp/16dp kısaldı. PulseClockViewModel skor state'i içgörü üretimi için korundu. BackupManager yeni anahtarı da taşıyor.

---

## Döngü D04 — Skor nedeni ve çözüm rotası

**Amaç:** Dijital Yaşam kartı yalnız durum değil, bir sonraki doğru eylemi de gösterebilsin.

**Nasıl yapılmalı:**

`PulseScoreReason` → kullanıcı metni ve action eşlemesi tek sınıfta yapılmalıdır:

```kotlin
class PulseReasonPresenter {
    fun present(reason: PulseScoreReason): PresentedPulseReason
}

data class PresentedPulseReason(
    val label: String,
    val action: PulseAction?,
    val positive: Boolean?,
)
```

Örnek eşlemeler:

- `ORGANIZATION_UNCATEGORIZED` → “Kategorisiz 8 uygulama” → Kontrol Bekleyenler.
- `ATTENTION_NOISY` → “Bildirim yoğunluğu” → Bildirim Raporu.
- `CLEANUP_UNUSED` → “Uzun süredir kullanılmayan 6 uygulama” → Uygulamalar.
- `BALANCE_SHIFT` → “Kullanım dağılımın değişti” → Haftalık Rapor.
- `TASK_MISSIONS` → “Görev ilerlemen skora +4 katkı verdi” → Görevler.

Pozitif nedenlerde zorunlu CTA gerekmez.

**Yeni dosyalar:**

- `PulseReasonPresenter.kt`
- `PulseAction.kt`
- `PulseActionRouter.kt`

**Değişecek dosyalar:**

- `DigitalLifeCard.kt`
- `Wrapped` detay ekranı gerekirse

**Testler:** Her reason id doğru metin/action üretir; bilinmeyen reason güvenli fallback döner.

**Kabul kriterleri:** Kullanıcı skorun neden değiştiğini ve ne yapabileceğini anlayabilir.

**Bağımlılıklar:** D02.

**Durum:** ✅ Tamamlandı — Döngü D04 — commit: b98673b — tarih: 2026-07-17 — D FAZI KAPANDI: tam test + assembleDebug yeşil, APK v1.3.86 (109) Telegram'a gönderildi. PulseActionRouter ayrı router (MissionActionRouter'dan bağımsız); 15 PulseReasonId eşlendi; pozitif nedenlerde CTA yok.

---

# 8. Akıllı Nabız Şeridi döngüleri

## Döngü T00 — Düşük değerli ve tekrarlı içerikleri temizle

**Amaç:** Şeridi dekoratif metin alanından eylem ve içgörü alanına dönüştürmek.

**Kaldırılacak veya dönüştürülecek mevcut içerikler:**

### Kaldır

- Sabah/öğle/akşam/gece genel selamlamaları.
- “Günün şampiyonu” ham uygulama mesajları.
- Her gün en büyük beş klasörün uygulama sayıları.
- Ham Dijital Yaşam skor mesajı.

### Koşula bağla

- Unutulan uygulama: En fazla bir öğe; 60+ gün; kullanıcı daha önce gizlemediyse.
- Özellik ipucu: İlk 14 gün, günde en fazla bir, özellik henüz kullanılmadıysa.
- Haftalık özet: Rapor gerçekten hazırsa bir kez.
- Bildirim özeti: Salt sayı yerine anormal değişim veya eylem varsa.
- Klasör bilgisi: Klasör aşırı kalabalık, yeni uygulama veya temizlik önerisi varsa.

### Koru ve yükselt

- Belirsiz sınıflandırma.
- İzin/sağlık sorunu.
- Görev riski veya son süre.
- Anlamlı Dijital Yaşam değişimi.
- Haftalık rapor hazır bildirimi.

**Değişecek dosyalar:**

- `TickerComposer.kt`
- `TickerComposerTest.kt`

**Testler:** Yararlı veri yoksa boş liste; greeting/champion/raw score üretilmez.

**Kabul kriterleri:** Şerit sırf dolu görünmek için içerik üretmez.

**Bağımlılıklar:** D00, M07.

**Durum:** ✅ Tamamlandı — Döngü T00 — commit: b8b7da9 — tarih: 2026-07-17 — Not: Selamlama/şampiyon/klasör-istatistiği üreticileri silindi (-142 satır). Eşik değişiklikleri (45→60 gün, ipucu ilk-14-gün) SmartTickerItem gerektirdiği için T01+'a bırakıldı.

---

## Döngü T01 — SmartTickerItem ve içerik türleri

**Amaç:** Metin ve priority'den ibaret `TickerSpec` yerine davranış taşıyan model kullanmak.

**Nasıl yapılmalı:**

```kotlin
enum class SmartTickerType {
    CRITICAL_HEALTH,
    ACTION_REQUIRED,
    MISSION_PROGRESS,
    MISSION_ACHIEVEMENT,
    PULSE_CHANGE,
    CONTEXTUAL_SUGGESTION,
    WEEKLY_REPORT,
    FEATURE_DISCOVERY,
}
```

Model alanları:

- `id`
- `type`
- `title`
- `subtitle`
- `priority`
- `createdAt`
- `expiresAt`
- `action`
- `suggestionKey`
- `autoAdvanceAllowed`
- `sensitive`

`TickerItem` UI modeli buna göre güncellenmelidir. `text` tek alan yerine başlık/alt başlık kullanılması kartın daha kısa ve okunur olmasını sağlar.

**Değişecek dosyalar:**

- `TickerComposer.kt`
- `LauncherViewModel.kt`
- `HomeTickerRow.kt`

**Yeni dosyalar:**

- `SmartTickerItem.kt`
- `SmartTickerType.kt`
- `TickerAction.kt`

**Testler:** Model mapping, route/action, expiry.

**Kabul kriterleri:** Her ticker öğesi tür, süre ve davranış olarak açıklanabilir.

**Bağımlılıklar:** T00.

**Durum:** ✅ Tamamlandı — Döngü T01 — commit: b4fe7fe — tarih: 2026-07-18 — Not: Koordinatörün üç kaynağı da artık gerçek. Eski TickerItem UI köprüyle korunuyor (T04'te yenilenecek); string'ler kod içi literal kaldı (T04'te resource'a taşınacak).

---

## Döngü T02 — TickerRanker sıralama ve tekrar motoru

**Amaç:** En değerli en fazla üç öğeyi seçmek.

**Nasıl yapılmalı:**

Yeni saf sınıf:

```kotlin
object TickerRanker {
    fun rank(
        candidates: List<SmartTickerItem>,
        history: TickerHistory,
        now: Long,
    ): List<SmartTickerItem>
}
```

Önerilen skor:

```text
Toplam = önem + eyleme geçirilebilirlik + güncellik + kişiselleştirme + yenilik
         - tekrar cezası - rahatsız etme cezası
```

Önerilen puanlar:

```text
Kritik sağlık/izin sorunu             +100
Bugün çözülebilen eylem               +70
Görev son süresi/risk                 +65
Anlamlı skor değişimi                 +50
Haftalık rapor hazır                  +45
Bağlamsal kısayol                     +30
Özellik keşfi                         +10
Bugün daha önce gösterildi            -35
Son üç günde üç kez gösterildi        -70
Tıklanıp tamamlandı                   -100
Kullanıcı türü kapattı                filtrele
Süresi doldu                          filtrele
```

Kurallar:

- En fazla 3 öğe.
- Aynı türden en fazla 1 öğe; kritik hariç.
- Görev kartını tekrar eden normal görev öğesi gösterilmez.
- Ham skor gösterilmez.
- Hiç aday yoksa şerit gizlenir.

Mevcut `SuggestionCoordinator` tekrar geçmişi mümkün olduğunca yeniden kullanılmalıdır. Paralel ikinci history sistemi oluşturulmadan önce mevcut API incelenmelidir.

**Yeni dosyalar:**

- `TickerRanker.kt`
- `TickerCandidateFactory.kt`
- `TickerHistory.kt` gerekirse

**Değişecek dosyalar:**

- `LauncherViewModel.kt`
- `SuggestionCoordinator` yalnız gerekiyorsa

**Testler:** Öncelik, TTL, dedupe, tür kotası, boş durum, üç öğe sınırı.

**Kabul kriterleri:** Kullanıcıya aynı anda yalnız en değerli üç bilgi gösterilir.

**Bağımlılıklar:** T01.

**Durum:** ⏳ Bekliyor

---

## Döngü T03 — Görev ve Dijital Yaşam entegrasyonu

**Amaç:** Şeridin görev kartı ve skor kartını tekrar etmeden önemli değişimleri duyurması.

**Görev şeridi kuralları:**

Şerit görev öğesi yalnız şu durumlarda üretir:

- Görev `AT_RISK` oldu.
- Dönem bitimine 2 saatten az kaldı ve görev tamamlanmadı.
- Tek eylemle tamamlanabilecek görev var.
- Yeni yıldız kazanıldı.
- Günlük bütün görevler tamamlandı.

Örnekler:

```text
⏳ Ekran süresi hedefinde yalnız 12 dk kaldı · Detay
```

```text
⭐ Bugünkü 3 görevi tamamladın · Sonuçları gör
```

Normal `1/3 tamamlandı` bilgisi şeritte gösterilmez; görev kartında kalır.

**Dijital Yaşam şeridi kuralları:**

Şerit pulse öğesi yalnız şu durumlarda üretir:

- Haftalık skor değişimi mutlak 5 veya daha fazla.
- Top reason çözülebilir bir sorundur.
- Veri güveni LOW'dan MEDIUM/HIGH'a geçti.
- Haftalık rapor hazırdır.

Örnekler:

```text
📈 Dijital Yaşam skorun 6 puan yükseldi · Nedenini gör
```

```text
🔔 Bildirim yoğunluğu skorunu etkiliyor · Raporu aç
```

Ham `Skor 72` mesajı gösterilmez.

**Değişecek dosyalar:**

- `TickerCandidateFactory.kt`
- `HomeIntelligenceCoordinator.kt`
- `MissionsRepository/summary`
- `DigitalPulseRepository/summary`

**Testler:** Her üretim şartı ve tekrar önleme.

**Kabul kriterleri:** Şerit diğer kartların kopyası değil, değişim ve aciliyet kanalı olur.

**Bağımlılıklar:** M07, D04, T02.

**Durum:** ⏳ Bekliyor

---

## Döngü T04 — HomeTickerRow davranış ve erişilebilirlik yenilemesi

**Amaç:** Sürekli kayan ve hızlı değişen metni sakin, okunur bir bileşene çevirmek.

**Nasıl yapılmalı:**

- `basicMarquee(iterations = Int.MAX_VALUE)` kaldırılmalıdır.
- Başlık en fazla 1 satır, alt başlık en fazla 1 satır olmalıdır.
- Varsayılan otomatik geçiş 10 saniye olmalıdır.
- Kritik öğe otomatik geçmemelidir.
- Kullanıcı dokununca otomatik geçiş en az 15 saniye durmalıdır.
- Kullanıcı manuel sağ/sol yaptığında timer sıfırlanmalıdır.
- Ana ekran görünür değilken timer çalışmamalıdır.
- TalkBack/erişilebilirlik aktifken otomatik geçiş kapatılmalıdır.
- Sistem animasyon azaltma tercihinde yalnız fade veya animasyonsuz değişim kullanılmalıdır.
- `1/3` sayfa göstergesi görünür olmalıdır.
- Yatay gesture ana folder pager ile çakışmamalıdır.
- Alternatif olarak küçük önceki/sonraki düğmeleri erişilebilirlik aksiyonu olarak sunulmalıdır.

Uzun basma menüsü:

```text
Bu bilgiyi gizle
Bu tür bilgileri gösterme
8 saat ertele
1 gün ertele
Akıllı Nabız ayarları
Şeridi kapat
```

Mevcut yalnız bütün şeridi sessize alma davranışı içerik bazlı kontrolle genişletilmelidir.

**Değişecek dosyalar:**

- `HomeTickerRow.kt`
- `HomeScreen.kt`
- `AppPrefs.kt`
- TR/EN strings

**Testler:**

- Auto advance.
- Kritik öğe sabit.
- TalkBack.
- Büyük font.
- 1 ve 3 öğe.
- Gesture çakışması.
- Mute/snooze süre dolumu.

**Kabul kriterleri:** Şerit dikkat dağıtmadan okunabilir ve tamamen kontrol edilebilir olur.

**Bağımlılıklar:** T01, T02.

**Durum:** ⏳ Bekliyor

---

## Döngü T05 — Akıllı Nabız ayarları

**Amaç:** Kullanıcının içerik türlerini ayrı ayrı kontrol edebilmesi.

**Ayar ekranı:** `Ayarlar > Ana Ekran > Akıllı Nabız Şeridi`

```text
Akıllı Nabız Şeridi                 Açık

Gösterilecek içerikler
[✓] Yapılması gerekenler
[✓] Görev uyarıları ve başarılar
[✓] Dijital Yaşam değişimleri
[✓] Haftalık rapor
[✓] Zaman bazlı öneriler
[ ] Özellik ipuçları
[✓] Sistem sağlık uyarıları

Otomatik geçiş                      Açık
Geçiş süresi                        10 saniye
Hassas bilgileri göster             Kapalı
Sessiz saatler                      23:00–07:00
```

`Haber Şeridi` adı `Akıllı Nabız Şeridi` olarak değiştirilmelidir.

Yeni AppPrefs anahtarları:

```text
KEY_SMART_TICKER_ENABLED
KEY_SMART_TICKER_ACTIONS
KEY_SMART_TICKER_MISSIONS
KEY_SMART_TICKER_PULSE
KEY_SMART_TICKER_REPORTS
KEY_SMART_TICKER_CONTEXTUAL
KEY_SMART_TICKER_DISCOVERY
KEY_SMART_TICKER_HEALTH
KEY_SMART_TICKER_AUTO_ADVANCE
KEY_SMART_TICKER_INTERVAL_SECONDS
KEY_SMART_TICKER_SENSITIVE
```

Eski `KEY_TICKER_ENABLED` yeni anahtara migrate edilmelidir.

**Değişecek dosyalar:**

- `SettingsHomeScreenSection.kt`
- `AppPrefs.kt`
- `HomeScreen.kt`
- TR/EN strings

**Testler:** Preference migration, toggle davranışı, tür filtreleme.

**Kabul kriterleri:** Kullanıcı bir içerik türünü kapatırken diğer önemli uyarıları kaybetmez.

**Bağımlılıklar:** T01, T04.

**Durum:** ⏳ Bekliyor

---

# 9. Birleştirme, telemetri ve kalite döngüleri

## Döngü U00 — Ana ekran kart yerleşimini birleştir

**Amaç:** Görevler ve Dijital Yaşam kartlarının eşit, sade ve adaptif çalışması.

**Nasıl yapılmalı:**

Yerleşim kuralları:

- İki kart da varsa yan yana ve eşit yükseklikte.
- Yalnız biri varsa tam genişlik.
- İkisi de yoksa satır tamamen gizli.
- Dar ekran veya büyük fontta dikey yerleşime geçilebilir.
- Kart yüksekliği sabit metni kırpmamalı; içerik için kontrollü adaptif yükseklik kullanılmalıdır.
- Klavye açıkken ikincil satırlar mevcut davranışla gizlenmeye devam etmelidir.
- Tablet genişliğinde gereksiz büyüme olmamalıdır; maksimum içerik genişliği uygulanabilir.

Önerilen composable:

```kotlin
@Composable
fun HomeIntelligenceCardsRow(
    mission: HomeMissionSummary?,
    pulse: HomePulseSummary?,
    onMissionClick: () -> Unit,
    onPulseClick: () -> Unit,
)
```

**Değişecek dosyalar:**

- `HomeScreen.kt`
- Eski görev/digital card composable'ları

**Yeni dosyalar:**

- `HomeIntelligenceCardsRow.kt`

**Testler:** Telefon, küçük telefon, tablet, büyük yazı, tek kart, iki kart, kart yok.

**Kabul kriterleri:** Ana ekran sade, dengeli ve kırpılmasız görünür.

**Bağımlılıklar:** M07, D02, D03.

**Durum:** ⏳ Bekliyor

---

## Döngü U01 — LauncherViewModel sorumluluklarını sadeleştir

**Amaç:** `LauncherViewModel` içindeki ticker üretimi ve eski skor hesabını kaldırmak.

**Nasıl yapılmalı:**

- `LauncherViewModel._digitalLifeScore` kaldırılmalıdır.
- `tickerItems` üretimindeki snapshot inşa, skor hesaplama ve history kayıt işi `HomeIntelligenceCoordinator`/`SmartTickerEngine` tarafına taşınmalıdır.
- ViewModel yalnız UI state'i expose etmeli ve eylemleri yönlendirmelidir.
- `resolveTickerRoute()` yerine yapılandırılmış `TickerActionRouter` kullanılmalıdır.
- `refreshInsights()` ticker için zorunlu olmaktan çıkarılmalı; hangi insight'ların yeni sistemde kalacağı açık olmalıdır.

Önerilen state:

```kotlin
val homeIntelligence: StateFlow<HomeIntelligenceUiState>
```

**Değişecek dosyalar:**

- `LauncherViewModel.kt`
- `HomeScreen.kt`
- Hilt injection

**Testler:** ViewModel state mapping ve refresh lifecycle.

**Kabul kriterleri:** LauncherViewModel iş kuralı değil, UI orkestrasyonu yapar.

**Bağımlılıklar:** H02, T03, U00.

**Durum:** ⏳ Bekliyor

---

## Döngü U02 — Gizlilik güvenli telemetri

**Amaç:** Hangi kart ve şerit türlerinin işe yaradığını kişisel içerik toplamadan ölçmek.

Mevcut merkezî `TelemetryManager` varsa onun üzerinden çalışılmalıdır. Yoksa telemetri roadmap'indeki merkezî yapı beklenmelidir; doğrudan Firebase çağrıları UI dosyalarına yazılmamalıdır.

Event'ler:

```text
home_mission_card_viewed
home_mission_card_opened
mission_progress_viewed
mission_completed
mission_failed
home_pulse_card_viewed
home_pulse_card_opened
ticker_impression
ticker_opened
ticker_dismissed
ticker_snoozed
ticker_type_disabled
ticker_manual_next
ticker_auto_advanced
```

İzin verilen parametreler:

```text
mission_type
action_type
status
progress_bucket
score_bucket
confidence
item_type
priority_bucket
position
```

Gönderilmesi yasak alanlar:

- Görevdeki uygulama adı
- Paket adı
- Kişi adı
- Bildirim metni
- Dosya adı
- Özel klasör adı
- Ticker başlık/metni
- Ham kullanım sorgusu

**Değişecek dosyalar:**

- `TelemetryManager` veya mevcut analytics wrapper
- Kart ve ticker event çağrı noktaları

**Testler:** Event allowlist; yasak parametrelerin reddedilmesi.

**Kabul kriterleri:** Ürün kullanımı ölçülür, kişisel içerik gönderilmez.

**Bağımlılıklar:** M07, D02, T04; telemetri altyapısı.

**Durum:** ⏳ Bekliyor

---

## Döngü U03 — Sağlık raporuna yeni sistem durumlarını ekle

**Amaç:** Görev, skor ve ticker sorunlarının kullanıcı sağlık raporundan teşhis edilebilmesi.

Eklenecek anonim agregatlar:

```text
[Görev Sistemi]
Aktif: Evet
Atanmış günlük: 3
Tamamlanan günlük: 1
Riskli: 1
Settlement bekleyen: 1
Son settlement: 2026-07-17 00:12
Son worker durumu: SUCCEEDED

[Dijital Yaşam]
Skor: 72
Confidence: HIGH
Hesap zamanı: ...
Veri tazeliği: RECENT
Tek skor kaynağı: DigitalPulseEngine

[Akıllı Nabız]
Aday: 6
Gösterilen: 3
Sessize alınan tür: 1
Son seçim hatası: Yok
```

Rapor uygulama/kişi/dosya/bildirim içeriği içermemelidir.

Sağlık uyarıları:

```text
MISSION_SETTLEMENT_STALE
MISSION_PROGRESS_DATA_STALE
PULSE_SNAPSHOT_STALE
PULSE_SOURCE_MISMATCH
TICKER_EMPTY_WITH_ACTIONABLE_ITEMS
```

**Değişecek dosyalar:**

- `DiagnosticsReportManager.kt`
- Testleri

**Testler:** Gizlilik ve tutarlılık.

**Kabul kriterleri:** Bu üç sistem tek raporla uzaktan teşhis edilebilir.

**Bağımlılıklar:** U01.

**Durum:** ⏳ Bekliyor

---

## Döngü U04 — Tam test matrisi ve yayın kapısı

**Amaç:** Değişikliklerin gerçek cihazlarda güvenilir çalışması.

## Unit test matrisi

### Görevler

- Günlük üst sınır erken tamamlanmaz.
- Gün sonunda doğru settlement.
- Haftalık ISO sınırı.
- Eylem görevi anlık tamamlanır.
- Aynı görev iki kez ödüllendirilmez.
- Progress current/target/remaining.
- İzin yok ve stale veri.
- Saat dilimi değişimi.
- Toplu puan cap.

### Dijital Yaşam

- Tek motor.
- Aynı snapshot bütün tüketicilerde aynı skor.
- Confidence low davranışı.
- Weekly baseline.
- Task contribution cap.
- Top reason/action eşleme.

### Akıllı Nabız

- En fazla 3 öğe.
- TTL.
- Dedupe.
- Tür kotası.
- Görev ve skor tekrarı yok.
- Yararlı aday yoksa boş.
- Kullanıcı kapattığı tür gösterilmez.

## Compose UI test matrisi

- Görev 1/3, 3/3, riskli, veri yok.
- Dijital kart skor/delta/reason/veri birikiyor.
- Ticker 1/3, manuel geçiş, kritik item.
- Büyük font %200.
- TalkBack semantics.
- Animasyonlar kapalı.
- Açık/koyu ve farklı duvar kâğıtları.

## Gerçek cihaz matrisi

1. Ana günlük kullanılan Android telefon.
2. Temiz kurulum test telefonu.
3. İzinleri kapatıp bozma testi yapılan telefon.
4. Android tablet.

Cihaz senaryoları:

- Kullanım erişimi açık/kapalı.
- Bildirim erişimi açık/kapalı.
- Gece yarısı geçişi.
- Pazartesi hafta geçişi.
- Uygulama kapalıyken worker.
- Pil optimizasyonu.
- Yeniden başlatma/process death.
- Ekran döndürme/tablet.
- Ana ekran pager ile ticker gesture çakışması.

## Yayın kapıları

- `testDebugUnitTest` geçmeli.
- `lintDebug` kritik hata vermemeli.
- Debug APK üretilmeli.
- En az bir telefon ve tablette smoke test yapılmalı.
- Sağlık raporunda skor kaynağı tek görünmeli.
- Aynı skor ana ekranda bir kez görünmeli.
- Sabah görev ekranı açmak yıldız üretmemeli.
- Ticker yararlı içerik yokken gizlenmeli.

**Değişecek dosyalar:** Testler, gerekirse CI workflow.

**Kabul kriterleri:** Bütün yayın kapıları kanıtla geçer.

**Bağımlılıklar:** Tüm önceki döngüler.

**Durum:** ⏳ Bekliyor

---

# 10. Dosya bazlı değişiklik haritası

## `app/src/main/java/com/armutlu/apporganizer/utils/TickerComposer.kt`

- Eski greeting, champion, raw folder count ve raw score üretimini kaldır.
- `computeDigitalLifeScore()` kullanımını bitir; sonrasında fonksiyonu kaldır.
- Aday üretimini `TickerCandidateFactory` tarafına taşı.
- Saf düşük seviye yardımcılar kalabilir.

## `presentation/ui/launcher/LauncherViewModel.kt`

- `_digitalLifeScore` kaldır.
- Eski `tickerItems combine` iş kurallarını kaldır.
- `HomeIntelligenceCoordinator.state` tüket.
- Route string mapping yerine action router kullan.

## `presentation/ui/launcher/HomeTickerRow.kt`

- `DigitalScoreCard` kodunu yeni `DigitalLifeCard` dosyasına taşı.
- Marquee kaldır.
- Başlık/alt başlık, sayfa göstergesi, pause ve erişilebilirlik ekle.
- İçerik bazlı menü ekle.

## `presentation/ui/launcher/HomeScreen.kt`

- Görev ve skor kartı satırını `HomeIntelligenceCardsRow` ile değiştir.
- Ticker'ı birleşik state'ten render et.
- İki ayrı skor tüketimini kaldır.
- Boş ticker alanında yer ayırma.

## `presentation/ui/launcher/PulseClockWidget.kt`

- Score ring tekrarını kaldır/migrate et.
- Saat/tarih/hava odaklı sade düzen.
- Yükseklik ve kompakt davranışı yeniden test et.

## `presentation/viewmodel/PulseClockViewModel.kt`

- Skor hesaplama sorumluluğunu `DigitalPulseRepository`ye taşı.
- Gerekirse yalnız saat widget'ına ait hava/içgörü state'i bırak.
- Aynı usage/notification verisini ikinci kez hesaplama.

## `domain/usecase/pulse/DigitalPulseEngine.kt`

- Tek motor olarak korunur.
- İş kuralları bu dosyada kalır.
- UI metni/route eklenmez.
- Yeni reason testleri eklenir.

## `utils/WrappedSnapshotPrefs.kt`

- ISO hafta baseline migration.
- Eski ticker günlük score key'lerini deprecated et.
- En son snapshot cache'ini repository ile uyumlu hâle getir.

## `presentation/viewmodel/WrappedViewModel.kt`

- DigitalPulseRepository snapshot'ını yeniden kullan.
- Aynı pulse input'un ikinci kez üretilmesini engelle.
- Rapor ve kart skorunun aynı olduğunu test et.

## `domain/usecase/missions/MissionEngine.kt`

- Boolean check yerine evaluation/status/progress.
- Erken tamamlama kaldır.
- Hedef ve progress kind taşı.
- Saf ve Android bağımsız kal.

## `data/repository/MissionsRepository.kt`

- Mission instance CRUD.
- Runtime summary.
- Settlement transaction.
- Event sayaçları ve history ledger.

## `presentation/viewmodel/MissionsViewModel.kt`

- Veri hesaplamayı provider'a taşı.
- Yeni MissionUi mapping.
- Ekran açılışında ödül verme davranışını kaldır.
- Refresh yalnız state yenilesin ve overdue settlement catch-up çağırabilsin.

## `presentation/ui/screens/MissionsScreen.kt`

- Progress, remaining, deadline, CTA.
- Status semantics.
- Kutlama yalnız yeni gerçek ödülde.

## `utils/TaskScoreManager.kt`

- Negatif öneri cezalarını kaldır.
- Bulk reward cap.
- Event label'larını resource/presentation katmanına taşımayı değerlendir; veritabanında sabit eventKey önemlidir.

## `utils/AppPrefs.kt`

- Akıllı Nabız içerik ayarları.
- Digital Life kart görünürlüğü.
- Eski ticker ve clock score preference migration.

## `presentation/ui/screens/SettingsHomeScreenSection.kt`

- “Haber Şeridi” → “Akıllı Nabız Şeridi”.
- Alt ayar ekranı/expandable section.
- Dijital Yaşam kartı görünürlüğü.
- Eski score ring ayarını migrate et.

## `utils/DiagnosticsReportManager.kt`

- Görev settlement, pulse source ve ticker ranker sağlık bilgileri.

---

# 11. Definition of Done

Bu roadmap tamamen tamamlanmış sayılabilmesi için:

1. Dijital skor yalnız `DigitalPulseEngine` tarafından hesaplanmalıdır.
2. Ana ekranda skor yalnız Dijital Yaşam kartında birincil olarak görünmelidir.
3. Pulse Clock skor tekrarından arındırılmalıdır.
4. Ticker ham skoru tekrar etmemelidir.
5. Görev ekranı mevcut değer, hedef ve kalan miktarı göstermelidir.
6. Sabah görev ekranı açmak dönemsel göreve yıldız kazandırmamalıdır.
7. Haftalık görev pazartesi–pazar sınırını kullanmalıdır.
8. Görevler ekranı açılmasa bile dönem sonunda sonuçlanmalıdır.
9. Ana ekran görev kartı gerçek `x/y` ve en yakın hedefi göstermelidir.
10. Ticker en fazla üç yüksek değerli öğe göstermelidir.
11. Yararlı ticker öğesi yoksa şerit gizlenmelidir.
12. TalkBack ve büyük font desteği doğrulanmalıdır.
13. Telefon ve tablette smoke test geçmelidir.
14. Sağlık raporu üç sistemin durumunu göstermelidir.
15. Telemetri kişisel metin veya ad göndermemelidir.
16. Tüm döngülerde durum satırı commit ve tarihle `✅ Tamamlandı` olmalıdır.

---

# 12. İlk uygulanacak paket

İlk kodlama paketi yalnız şu döngülerden oluşmalıdır:

```text
H00 — Regresyon testleri
H01 — Dönem sınırları
M00 — Erken görev tamamlanmasını kaldır
D00 — Tek skor kaynağı
```

Bu dört döngü bitmeden görsel tasarım döngülerine geçilmemelidir. Çünkü mevcut en büyük risk görsel değil, yanlış görev ödülü ve farklı skor gösterilmesidir.
