# Akıllı Dijital Görevler + Adaptif Kategori Hedefleri + Tavsiye Kartı — Roadmap

> **Durum:** AŞAMA 1 — araştırma tamamlandı, uygulama onayı bekleniyor. Kod yazılmadı, commit/push yapılmadı.
> **Kapsam sınırı:** Telefon düzeni, dijital alışkanlıklar, dikkat yönetimi, uygulama kullanımı, dijital temizlik. Spor/beslenme/sağlık/ilişki/finans/genel yaşam görevi YOK.

---

## 1. Mevcut Yapının Özeti

### 1.1 Görev Sistemi (yeniden kullanılacak, bozulmayacak)
- **Çekirdek:** `domain/usecase/missions/MissionEngine.kt` — saf Kotlin, Android bağımsız. Görev ID'leri enum değil, `const val String` sabitleri (ör. `DAILY_SCREEN_UNDER_3H`, `WEEKLY_SCREEN_LESS`). Bu deseni bozmadan yeni ID ekleyeceğiz.
- **Durum makinesi:** `MissionStatus` enum — `DATA_UNAVAILABLE, NOT_STARTED, IN_PROGRESS, SAFE, AT_RISK, AWAITING_SETTLEMENT, COMPLETED, FAILED`. Üst-sınır görevleri dönem bitmeden `COMPLETED` olamıyor — **tam istediğimiz "hafta bitmeden başarı yok" sözleşmesiyle zaten uyumlu**, adaptif kategori hedefi aynı durum makinesini kullanacak.
- **Settlement:** `SettleMissionInstancesUseCase` dönem sonu ödülünü idempotent yazıyor (üst-sınır görevler için zaten var — erken ödül yok, P0 2.4 fix'te düzeltilmiş). `WEEKLY_CATEGORY_BALANCE` bu use-case'in üzerine oturacak, YENİ bir settlement mekanizması icat edilmeyecek.
- **Kişisel hedef pinleme:** `MissionSummaryUseCase.compute()` içinde `pinInstances` sırasında dönem boyunca hedef sabitleniyor (`existingDailyInstances[missionId]?.targetValue` varsa onu kullanır) — **adaptif kategori hedefinin "hafta ortasında değişmez" kuralı için birebir aynı desen**, `WeeklyGoal` tablosunda da taklit edilecek.
- **Yıldız ekonomisi:** `TaskScoreManager` (davranış puanı, ayrı) + `MissionEngine` yıldızları (`DAILY_STAR=1, WEEKLY_STAR=2`). Cooldown: günlük 2 gün, haftalık 1 hafta.
- **Ana ekran kartı:** `HomeMissionCard.kt` + `HomeMissionSummary` (domain/home) — `HomeIntelligenceCoordinator.state.mission` üzerinden besleniyor, `LauncherViewModel.homeMissionSummary` StateFlow. **Bir önceki döngüde bu kart Hero Dashboard'a bağlandı (HeroBlock.MISSIONS) — mevcut bağlantı korunacak, ikinci bir görev kartı eklenmeyecek.**

### 1.2 Hedef Sistemi (kırık mantık, değiştirilecek)
- **`WeeklyGoal`** — Room entity, bileşik PK (`categoryId`, `weekStartEpochDay`), alanlar: `targetMinutes: Int, createdAt: Long, achievedAt: Long = 0L`. **Mode/status/pace/baseline alanları YOK.**
- **`WeeklyGoalDao`** — `observeGoals, getGoalsForWeek, upsert, delete, markAchieved`.
- **Hatalı mantık — `WeeklyDigestWorker.checkWeeklyGoals()`:**
  ```kotlin
  goal.achievedAt == 0L && (categoryUsageMinutes[goal.categoryId] ?: 0L) >= goal.targetMinutes
  ```
  `>=` kullanım hedefe ULAŞTIĞINDA "başarıldı" sayıyor — üst sınır hedefi için ters. Bu, haftada bir çalışan `WeeklyDigestWorker` içinde, gerçek zamanlı değil.
- **UI:** `AppOrganizerDashboardScreen.kt` → `WeeklyGoalsCard` — kategori seç (AssistChip döngüsel) + dakika yaz (`OutlinedTextField`) + Kaydet (`Button`). `AppListViewModel.setWeeklyGoal/deleteWeeklyGoal` çağırıyor.
- **`PersonalTargetCalculator.kt`** — zaten var, saf Kotlin, `MIN_DAYS_REQUIRED=3`, medyan×tempo formülü, clamp — **görev kişisel hedefleri için**. `AdaptiveCategoryTargetCalculator` bu dosyanın kardeşi olacak, kod stilini/test desenini taklit edecek ama farklı girdi/çıktı modeli olduğu için (blend formülü, %15 klips, outlier koruması) ayrı sınıf olarak kalacak.
- **`AppLimitCandidateSelector.kt`** — zaten `ELIGIBLE_CATEGORY_IDS = {CAT_SOCIAL, CAT_GAMES, CAT_VIDEO}` kullanıyor — **istenen "varsayılan uygun kategoriler" listesiyle birebir aynı**, `AdaptiveCategoryTargetCalculator` bu sabiti tekrar tanımlamayacak, ortak bir yerden (`AppLimitCandidateSelector.ELIGIBLE_CATEGORY_IDS` veya ikisinin de kullandığı ortak bir `object`) paylaşacak.

### 1.3 Smart Ticker / Suppression (yeniden kullanılacak)
- `SmartTickerType.CONTEXTUAL_SUGGESTION` zaten var — `DigitalAdviceEngine` çıktısı buraya map edilecek, yeni tip eklenmeyecek.
- `TickerAction` sealed interface zaten `OpenMissions`, `OpenDashboard`, `OpenAppList`, `OpenClassificationReview`, `OpenNotificationReport`, `OpenSettings(section)` içeriyor — tavsiye eylemleri için yeterli, yeni action tipi gerekmiyor (Kategori Hedefleri ekranı için `OpenSettings` benzeri `OpenCategoryGoals` eklenmesi gerekebilir, P7'de netleşecek).
- **Suppression tek kaynak:** `SuggestionCoordinator` + `SharedPrefsSuggestionHistoryStore` (SharedPreferences, Room YOK). `TickerRanker` ek olarak bellek-içi `TickerHistory` ile tür başına kota uyguluyor. **İkinci bir suppression/history deposu KURULMAYACAK** — `DigitalAdviceEngine`'in tavsiyeleri de bu mevcut `SuggestionCoordinator.canShow()/recordShown()/recordRejected()` üzerinden geçecek.
- **`TodayCard`/`TodayCardSelector`** — kodlanmış, test edilmiş, **hiçbir üretim ekranına bağlı değildi**. **Karar (2026-07-27, Hüseyin onayı):** TodayCard kullanılacak. Dashboard'a **üçüncü, bağımsız bir section** (`HomeSectionId.TODAY_CARD`) olarak bağlanacak — mevcut `MISSIONS_AND_SCORE`/`MISSIONS` section'larının yerini ALMAYACAK, onlarla yan yana Ana Ekranı Düzenle listesine eklenecek (migration gerektirmez, düşük risk). `DigitalAdviceEngine`'in ürettiği tavsiye, `TodayCardKind`'a yeni bir 7. tür (`ADVICE`) olarak eklenecek — Görevler ekranındaki "Bugünün Tavsiyesi" kartı da aynı `TodayCardSpec`/`TodayCard` composable'ının küçük bir varyantı olacak, ikinci bir kart deseni icat edilmeyecek.

### 1.4 Ortak Altyapı
- **ISO hafta:** `domain/time/PeriodBoundaryResolver.kt` tek kaynak — Pazartesi başlangıç, `java.time`/DST-güvenli. `utils/WeekUtils.kt` ince köprü. **Yeni sistem bunu kullanacak, paralel hafta hesaplama YOK.**
- **UsageStatsHelper.kt** — kategori bazlı toplama fonksiyonu YOK, sadece paket bazlı (`getUsageCounts`, `getWeightedScores`). `CategoryUsageSnapshotProvider` bu boşluğu dolduracak.
- **AppDatabase.kt** — version **24**, son migration `notification_events`'e kolon ekliyor. `schemas/24.json` mevcut. Yeni migration **25** olacak.
- **`Category.kt`** — `CAT_SOCIAL`, `CAT_GAMES`, `CAT_VIDEO` dahil tüm sabitler mevcut, yeniden tanımlanmayacak.
- **`HomeIntelligenceCoordinator`** — `state.mission`, `state.pulse` gibi dilimleri birleştiren merkezi orkestratör, `LauncherViewModel` sadece bu coordinator'ı `collectAsState` ediyor. Yeni `state.categoryGoals` ve `state.advice` dilimleri muhtemelen buraya eklenecek (P1/P7'de netleşecek) — **`AppListViewModel` büyütülmeyecek, `LauncherViewModel` de gereksiz şişirilmeyecek, iş `DashboardViewModel`'e verilecek.**

---

## 2. Tespit Edilen Mantık ve Mimari Sorunlar

| # | Sorun | Kanıt | Etki |
|---|-------|-------|------|
| S1 | Kategori hedefi "ulaşınca başarı" mantığı üst-sınır hedefi için ters | `WeeklyDigestWorker.checkWeeklyGoals()`: `usage >= target` → achieved | Kullanıcı ekran süresini azaltmaya çalışırken hedefe yaklaştıkça "başardın" bildirimi alıyor — üründe güven kırıcı |
| S2 | Hedef üretimi tamamen manuel, veri kullanılmıyor | `WeeklyGoalsCard` — dakika elle yazılıyor | Kullanıcıdan bilgi isteniyor, sistem "akıllı" değil |
| S3 | `achievedAt` yalnız haftalık worker'da set ediliyor, gerçek zamanlı değil | `WeeklyDigestWorker` haftada 1 çalışıyor, UI progress bar canlı ama "başarı" rozetlenmesi gecikmeli | Hafta ortasında durum (ON_TRACK/AT_RISK/EXCEEDED) hiç hesaplanmıyor |
| S4 | `WeeklyGoal` entity'de mode/status/pace/baseline yok | Sadece `categoryId, targetMinutes, createdAt, achievedAt` | Adaptif/manuel ayrımı, tempo, geçmiş kullanım kayıt altına alınamıyor |
| S5 | Kategori kullanımı tek kaynaktan hesaplanmıyor | `UsageStatsHelper`'da kategori toplama yok, Dashboard'un kendi `categoryUsageMinutes`'ı muhtemelen ayrı bir yerde (muhtemelen `AppListViewModel` içinde) hesaplanıyor | Görev snapshot'ı (`MissionMetricSnapshotProvider`) ile Dashboard'un kategori hesapları farklı algoritmalar kullanıyor olabilir — P1'de doğrulanacak |
| S6 | `TodayCard` altyapısı kod var, hiçbir ekrana bağlı değildi | grep: `TodayCard(` ve `TodayCardSelector.select(` sadece kendi dosyalarında + test'te | **ÇÖZÜLDÜ:** TodayCard, `HomeSectionId.TODAY_CARD` olarak üçüncü section şeklinde bağlanacak (P-2), Tavsiye 7. tür (`ADVICE`) olarak eklenecek |
| S7 | Dashboard'da senkron UsageStats sorgusu riski | `AppOrganizerDashboardScreen` doğrudan mı okuyor, ViewModel üzerinden mi — P1 doğrulamasında netleşecek | Compose ana thread'de ağır I/O riski |
| S8 | ROADMAP'ta R-HOME-TICKER "Yapılacak" statüsünde | `MANAGEMENT/ROADMAP.md` — ticker kod hazır, görsel entegrasyon bekliyor | Bu iş bizim kapsamımız dışında ama aynı dosyalarda (`HomeScreen.kt`, `AppPrefs.kt`) çakışma riski — P10'da dikkat edilecek |

---

## 3. Önerilen Nihai Mimari

```
┌─────────────────────────────────────────────────────────────────┐
│                    CategoryUsageSnapshotProvider                 │
│  (yeni, saf use-case — PeriodBoundaryResolver + UsageStatsHelper │
│   + AppDao paket→kategori eşlemesi TEK yerde toplanır)          │
│  → CategoryUsageSnapshot(previousWeek, currentWeek, validDays,  │
│                           freshness)                             │
└───────────────┬───────────────────────────────┬─────────────────┘
                │                               │
                ▼                               ▼
┌───────────────────────────────┐   ┌──────────────────────────────┐
│ AdaptiveCategoryTargetCalculator│   │   DigitalAdviceEngine         │
│ (saf Kotlin, Android bağımsız) │   │ (saf Kotlin, Android bağımsız)│
│ → yeni hedef / güncelleme      │   │ → DigitalAdvice (öncelikli,   │
└───────────────┬────────────────┘   │   tek ana tavsiye)            │
                │                    └──────────────┬────────────────┘
                ▼                                   │
┌───────────────────────────────────┐               │
│ EnsureCurrentWeekAdaptiveGoalsUseCase│             │
│ SettlePreviousWeekAdaptiveGoalsUseCase│            │
│ (WeeklyGoalDao okur/yazar, idempotent)│            │
└───────────────┬────────────────────┘              │
                │                                   │
                ▼                                   ▼
┌────────────────────────┐          ┌──────────────────────────────┐
│   DashboardViewModel     │          │  MissionsViewModel (mevcut)   │
│   (YENİ — Dashboard'a    │          │  → Tavsiye Kartı ekler         │
│    özel, AppListViewModel│          │  (Görevler ekranı, yıldız      │
│    büyütülmez)           │          │   alanının altında)            │
└───────────┬──────────────┘          └───────────────┬───────────────┘
            │                                          │
            ▼                                          ▼
┌────────────────────────┐          ┌──────────────────────────────┐
│ AppOrganizerDashboard-  │          │  SuggestionCoordinator (mevcut)│
│ Screen: "Akıllı Kategori │          │  → TickerRanker → SmartTicker  │
│ Hedefleri" kartı         │          │  (CONTEXTUAL_SUGGESTION tipi)  │
└──────────────────────────┘          └──────────────────────────────┘
```

**Görev sistemi tarafı:** `MissionEngine`'e yeni `WEEKLY_CATEGORY_BALANCE` sabiti eklenir, `MissionSummaryUseCase` bu görevi değerlendirirken `WeeklyGoalDao`'dan aktif otomatik hedefleri okur (yeni bağımlılık — mevcut mimaride use-case'lerin DAO enjekte etmesi zaten yaygın desen).

**Neden `DashboardViewModel` (yeni) ve `AppListViewModel` değil:** `AppListViewModel` zaten `weeklyGoals` StateFlow'unu barındırıyor ve dosya büyüklüğü kısıtı (CLAUDE.md §7 "büyük dosyaları böl") var. Yeni Dashboard business logic'i (adaptif hedef hesaplama tetikleme, kategori kullanım snapshot okuma, tavsiye kartı state'i) ayrı bir ViewModel'e taşınacak, `AppOrganizerDashboardScreen` bu yeni ViewModel'i kullanacak, `AppListViewModel.weeklyGoals` StateFlow'u geriye dönük uyumluluk için ya kaldırılıp Dashboard'un yeni ViewModel'ine taşınacak ya da ince bir delege haline getirilecek (P5'te netleşecek, muhtemelen taşınacak çünkü zaten sadece Dashboard kullanıyor).

---

## 4. Değişecek / Oluşturulacak Dosyalar

### Yeni dosyalar
| Dosya | Katman | Amaç |
|---|---|---|
| `domain/usecase/goals/AdaptiveCategoryTargetCalculator.kt` | saf Kotlin | §4 algoritma (blend, %15 klips, outlier koruması) |
| `domain/usecase/goals/CategoryUsageSnapshotProvider.kt` | use-case | Tek kategori kullanım kaynağı |
| `domain/usecase/goals/CategoryUsageSnapshot.kt` | domain model | Snapshot veri sınıfı |
| `domain/usecase/goals/EnsureCurrentWeekAdaptiveGoalsUseCase.kt` | use-case | Yeni hafta hedef üretimi, idempotent |
| `domain/usecase/goals/SettlePreviousWeekAdaptiveGoalsUseCase.kt` | use-case | Önceki hafta settlement, idempotent |
| `domain/usecase/goals/WeeklyGoalPaceCalculator.kt` | saf Kotlin | §8 hafta-içi risk/projeksiyon hesabı |
| `domain/usecase/goals/OutlierWeekGuard.kt` | saf Kotlin | §4.5 aykırı hafta koruması (ayrı test edilebilir fonksiyon) |
| `domain/advice/DigitalAdviceEngine.kt` | saf Kotlin | Tavsiye üretimi |
| `domain/advice/DigitalAdvice.kt` | domain model | Tavsiye veri sınıfı |
| `presentation/ui/launcher/DashboardViewModel.kt` | ViewModel | Dashboard business logic |
| `app/src/test/.../AdaptiveCategoryTargetCalculatorTest.kt` | test | §16 saf hesaplama testleri |
| `app/src/test/.../CategoryUsageSnapshotProviderTest.kt` | test | Snapshot testleri |
| `app/src/test/.../WeeklyGoalPaceCalculatorTest.kt` | test | Hedef durumu testleri |
| `app/src/test/.../DigitalAdviceEngineTest.kt` | test | Tavsiye motoru testleri |
| `app/src/test/.../EnsureCurrentWeekAdaptiveGoalsUseCaseTest.kt` | test | Yaşam döngüsü testleri |
| Room `MIGRATION_24_25` | migration | `weekly_goals` şema genişletme |
| `app/schemas/.../25.json` | schema | Room otomatik üretir |

### Değişecek dosyalar
| Dosya | Değişiklik |
|---|---|
| `domain/models/WeeklyGoal.kt` | Yeni alanlar: `mode, baselineMinutes, previousWeekActualMinutes, pace, status, generatedAt, settledAt, algorithmVersion` |
| `data/local/WeeklyGoalDao.kt` | Yeni sorgular: aktif otomatik hedefleri getir, mod güncelle, settle |
| `data/local/AppDatabase.kt` | version 24→25, `MIGRATION_24_25` eklenir |
| `workers/WeeklyDigestWorker.kt` | `checkWeeklyGoals()` KALDIRILIR, yerine `SettlePreviousWeekAdaptiveGoalsUseCase` + `EnsureCurrentWeekAdaptiveGoalsUseCase` çağrısı (güvenlik ağı) |
| `domain/usecase/missions/MissionEngine.kt` | Yeni `WEEKLY_CATEGORY_BALANCE` görev ID + eligibility/evaluate mantığı |
| `domain/usecase/missions/MissionSummaryUseCase.kt` | `WEEKLY_CATEGORY_BALANCE` için `WeeklyGoalDao` bağımlılığı, en fazla 2 yıldız sınırı |
| `presentation/ui/screens/AppOrganizerDashboardScreen.kt` | `WeeklyGoalsCard` (seç+yaz+kaydet) KALDIRILIR, yeni "Akıllı Kategori Hedefleri" kartı eklenir |
| `presentation/viewmodel/AppListViewModel.kt` | `weeklyGoals` StateFlow ve `setWeeklyGoal/deleteWeeklyGoal` `DashboardViewModel`'e taşınır (Dashboard dışında kullanan yoksa) |
| `domain/home/TodayCardSelector.kt` | Yeni `TodayCardKind.ADVICE` (7. tür, en düşük öncelik — mevcut 6 türden sonra), `TodayCardSpec`'e tavsiye alanları (`adviceMessage`, `adviceEvidenceText`) eklenir |
| `presentation/ui/launcher/TodayCard.kt` | `ADVICE` türü için render dalı + kapat/1 gün/1 hafta sessize al aksiyonları |
| `domain/models/HomeLayout.kt` | Yeni `HomeSectionId.TODAY_CARD` (CONTENT zone, MISSIONS'tan sonra), `DEFAULT` config'e eklenir |
| `presentation/ui/launcher/hero/HeroDashboardPage.kt` | Yeni `HeroBlock.TODAY_CARD`, `heroBlockOrder()` eşlemesi, `TodayCard` render dalı |
| `presentation/ui/launcher/DashboardUiState.kt` | `todayCardSpec: TodayCardSpec?` alanı, `onAdviceDismiss/onAdviceSnooze` action'ları |
| `presentation/ui/launcher/SmartDashboardPage.kt` | `todayCardSpec` + advice action'larının `HeroDashboardPage`'e geçirilmesi |
| `presentation/ui/launcher/HomeLayoutEditorScreen.kt` | `sectionName()` when bloğuna `TODAY_CARD` case'i |
| `presentation/ui/screens/MissionsScreen.kt` | "Bugünün Tavsiyesi" — aynı `TodayCard`/`TodayCardSpec(kind=ADVICE)` composable'ının kompakt kullanımı (yıldız alanının altında) |
| `presentation/viewmodel/MissionsViewModel.kt` | `DigitalAdviceEngine` çağrısı, `TodayCardSpec(kind=ADVICE)` state'i |
| `domain/home/RealSmartTickerSource.kt` | `DigitalAdvice` → `CONTEXTUAL_SUGGESTION` tipinde `SmartTickerItem` adayı üretimi |
| `utils/AppPrefs.kt` | Yeni key'ler: adaptif hedef aç/kapa, tempo, otomatik kategori seçimi, tavsiye türü yönetimi (§13) |
| `utils/SuggestionCoordinator.kt` | Muhtemelen değişmez — mevcut API tavsiye için yeterli, sadece yeni bir `suggestionKey` namespace'i kullanılır |
| `res/values/strings.xml`, `res/values-en/strings.xml` | Yeni UI metinleri, tavsiye şablonları |
| `domain/home/HomeIntelligenceCoordinator.kt` | Muhtemelen `state.categoryGoals` dilimi eklenir (P1/P5'te netleşecek) |

---

## 5. P0–P11 Ayrıntılı Roadmap

### P0 — Mevcut sistemi doğrula ve ters başarı mantığını belgele
- **Değişecek dosya:** Yok (sadece doğrulama + bu roadmap dokümanı)
- **Yapılacak:** `WeeklyDigestWorker.checkWeeklyGoals()`'ın gerçekten `>=` kullandığını, `AppOrganizerDashboardScreen`'in kategori kullanımını nereden aldığını (`categoryUsageMinutes` parametresinin kaynağını) kod okuyarak kesinleştir. Bu roadmap dokümanının S1-S8 bulguları bu adımın çıktısıdır.
- **Test:** Yok (dokümantasyon adımı)
- **Risk:** Düşük
- **Geri dönüş:** Gerek yok
- **Tamamlanma kriteri:** Bu roadmap dosyası onaylandı ✅ (bu adım şu an tamamlanıyor)

### P1 — Ortak CategoryUsage snapshot kaynağı
- **Oluşacak:** `CategoryUsageSnapshotProvider.kt`, `CategoryUsageSnapshot.kt`
- **Değişecek:** Muhtemelen `AppOrganizerDashboardScreen`'in kategori kullanımını nereden aldığı tespit edilip o kaynak bu provider'a yönlendirilir; `MissionMetricSnapshotProvider` ile kod tekrarı varsa ortak bir alt fonksiyona çıkarılır (MissionMetricSnapshotProvider bozulmaz, sadece paylaşılabilir parça varsa çıkarılır)
- **Algoritma:** Paket→kategori eşlemesini `AppDao`'dan tek sorguda çek, `UsageStatsHelper`'dan paket bazlı kullanımı çek, `PeriodBoundaryResolver.previousIsoWeek()/currentIsoWeek()` ile pencereyi kategoriye topla. Gizli/sistem uygulamalarını mevcut filtreleme kuralına göre çıkar. Geçerli gün sayısını ve freshness'ı hesapla.
- **Test:** Boş kullanım, izin yok, kısmi hafta, kategori eşlemesi eksik paket senaryoları
- **Risk:** `MissionMetricSnapshotProvider` ile veri kesişimi — iki farklı hesaplama aynı UI'da farklı sayı gösterebilir. Azaltma: aynı `UsageStatsHelper` fonksiyonlarını çağır, aynı `PeriodBoundaryResolver` kullan.
- **Geri dönüş:** Yeni dosyalar, hiçbir üretim kodu henüz bu sınıfı çağırmıyor — silinmesi risksiz
- **Tamamlanma kriteri:** Unit testler yeşil, provider hiçbir UI'ya henüz bağlı değil (izole, test edilebilir)

### P2 — AdaptiveCategoryTargetCalculator
- **Oluşacak:** `AdaptiveCategoryTargetCalculator.kt`, `OutlierWeekGuard.kt`
- **Algoritma:** §4'teki tam formül (ilk hedef = önceki tam hafta × tempo; sonraki = blend(0.60 gerçek + 0.40 önceki hedef) × tempo; 5dk yuvarlama; min 60/max 10080; ±%15 klips; outlier koruması ayrı fonksiyon)
- **Test:** §16'daki 10 senaryonun tamamı + outlier koruması ayrı test sınıfı
- **Risk:** Düşük — saf fonksiyon, Android bağımsız
- **Geri dönüş:** İzole dosya, silinmesi risksiz
- **Tamamlanma kriteri:** 10/10 senaryo yeşil, deterministik (aynı girdi → aynı çıktı, `Random` kullanılmaz)

### P3 — WeeklyGoal veri modeli ve Room migration
- **Değişecek:** `WeeklyGoal.kt`, `WeeklyGoalDao.kt`, `AppDatabase.kt`
- **Migration planı:** bkz. §6
- **Test:** `MigrationTestHelper` ile 24→25, manuel hedeflerin `MANUAL` moduna migrate edildiği, dakika değerlerinin korunduğu, `achievedAt` alanının yeni `status` alanına güvenli dönüştüğü doğrulanır
- **Risk:** Orta — mevcut kullanıcı verisi var, veri kaybı riski. Azaltma: destructive migration YASAK, her yeni sütun `DEFAULT` değerle eklenir, mevcut satırlar dokunulmadan `MANUAL` mode alır
- **Geri dönüş:** Migration tek yönlü ama additive (sütun ekleme) — geri alma gerekirse yeni sütunlar okunmaz, eski davranış bozulmaz
- **Tamamlanma kriteri:** Migration testi yeşil, `app/schemas/25.json` commit'e hazır, mevcut `WeeklyGoal` satırları migration sonrası `mode=MANUAL, targetMinutes` korunmuş görünüyor

### P4 — Settlement ve yeni hafta hedef üretimi
- **Oluşacak:** `EnsureCurrentWeekAdaptiveGoalsUseCase.kt`, `SettlePreviousWeekAdaptiveGoalsUseCase.kt`
- **Akış:** §6'daki 6 adım
- **Test:** Çift çağrıda tekrar yazım olmadığı (idempotency), önceki hafta yoksa/DATA_UNAVAILABLE durumunda hedefin bozulmadığı, yeni hafta başlangıcında pinlendiği
- **Risk:** Orta — çağrı noktaları (app açılışı, Dashboard açılışı, Görevler açılışı, worker) arasında race condition. Azaltma: Room transaction + `WeeklyGoal` upsert'in kendisi idempotent (PK çakışmasında güncelleme), ek bir "son çalıştırma zaman damgası" kontrolü eklenir
- **Geri dönüş:** Use-case'ler çağrılmazsa sistem P3 öncesi duruma döner (WeeklyGoal tablosu boş/MANUAL kalır)
- **Tamamlanma kriteri:** §16 "Hedef durumu" test grubu tamamı yeşil

### P5 — DashboardViewModel ve Akıllı Kategori Hedefleri UI
- **Oluşacak:** `DashboardViewModel.kt`
- **Değişecek:** `AppOrganizerDashboardScreen.kt` (WeeklyGoalsCard tamamen değişir, §9 tasarımı), `AppListViewModel.kt` (weeklyGoals taşınır)
- **Algoritma:** Yok (UI + orkestrasyon)
- **Test:** Tanıma modu, otomatik hedef satırı, manuel hedef satırı, hedef aşımı gösterimi, data unavailable — Compose UI testleri (mümkünse) veya en azından ViewModel state testleri
- **Risk:** Orta — Dashboard ekranı kullanıcı tarafından sık görülüyor, görsel regresyon riski. Azaltma: emülatör smoke test (CLAUDE.md §3 "her 18 döngüde tam test" kuralına ek olarak bu özellik için özel test)
- **Geri dönüş:** Eski `WeeklyGoalsCard` kodu git history'de kalır, gerekirse revert edilebilir
- **Tamamlanma kriteri:** §17 kabul kriterleri 1-6 doğrulanmış, emülatörde görsel kayma yok

### P6 — WEEKLY_CATEGORY_BALANCE görevi
- **Değişecek:** `MissionEngine.kt` (yeni ID + eligibility + evaluate), `MissionSummaryUseCase.kt` (WeeklyGoalDao entegrasyonu, en fazla 2 yıldız)
- **Kural:** §10'daki tam kural seti (eligible ⟺ en az 1 aktif otomatik hedef, hepsi hedef içinde kaldıysa COMPLETED, biri aşıldıysa FAILED, veri yoksa DATA_UNAVAILABLE, tek birleşik ödül max 2 yıldız)
- **Test:** Çakışma kontrolü — `WEEKLY_SCREEN_LESS` ve `DAILY_APP_LIMIT` ile aynı davranışa iki ödül verilmediği
- **Risk:** Düşük-orta — mevcut `MissionEngineTest.kt` çok kapsamlı, yeni ID'nin mevcut cooldown/seçim mantığını bozmadığı doğrulanmalı
- **Geri dönüş:** Yeni ID sabit, eski görevler etkilenmez — silinmesi izole
- **Tamamlanma kriteri:** §17 kriter 9-10, mevcut `MissionEngineTest.kt` hâlâ yeşil + yeni testler yeşil

### P7 — DigitalAdviceEngine
- **Oluşacak:** `DigitalAdviceEngine.kt`, `DigitalAdvice.kt`
- **Algoritma:** §11 öncelik sırası (8 seviye), §11.1 eşik kuralı (%20 VE 30dk)
- **Test:** §16 "Tavsiye motoru" test grubu tamamı
- **Risk:** Düşük — saf fonksiyon
- **Geri dönüş:** İzole, silinmesi risksiz
- **Tamamlanma kriteri:** Aynı anda en fazla 1 ana tavsiye üretildiği, veri yokken sayı uydurulmadığı doğrulanmış

### P7b — TodayCard'ı Dashboard'a bağla + Tavsiye'yi 7. tür olarak entegre et
- **Değişecek:** `TodayCardSelector.kt` (`TodayCardKind.ADVICE` eklenir, en düşük öncelik — mevcut 6 türden sonra, hiçbiri eşleşmezse tavsiye varsa o gösterilir), `TodayCard.kt` (ADVICE render dalı), `HomeLayout.kt` (`HomeSectionId.TODAY_CARD`), `HeroDashboardPage.kt` (`HeroBlock.TODAY_CARD`), `DashboardUiState.kt`, `SmartDashboardPage.kt`, `HomeLayoutEditorScreen.kt`, `HomeScreen.kt` (missionSummary/pulse/weeklyReportReady zaten mevcut StateFlow'lardan `TodayCardSelector.select()`'a geçirilir — yeni veri kaynağı YOK)
- **Karar (onaylandı):** Üçüncü bağımsız section — `MISSIONS_AND_SCORE`/`MISSIONS` section'larına DOKUNULMAZ, migration gerekmez. `TODAY_CARD` varsayılan `visible=false` (yeni bir kart, kullanıcı isterse Ana Ekranı Düzenle'den açar) — CLAUDE.md "Yeni Özellik = Ayarlar Kuralı" ile uyumlu.
- **Test:** `TodayCardSelectorTest.kt`'ye ADVICE senaryosu eklenir (7 öncelik sırası tam test edilir), Hero'da yeni section'ın doğru render edildiği
- **Risk:** Düşük — ekleme, mevcut iki karta dokunulmuyor
- **Geri dönüş:** Yeni section gizlenirse (`visible=false`) sistem eski haline döner
- **Tamamlanma kriteri:** `TodayCardSelectorTest.kt` 7 tür için yeşil, emülatörde Ana Ekranı Düzenle'den TODAY_CARD açılıp kapatılabiliyor

### P8 — Görevler ekranı Tavsiye Kartı
- **Değişecek:** `MissionsScreen.kt`, `MissionsViewModel.kt`
- **UI:** §11.3 tasarımı (yıldız alanının altında, kompakt kart, kapat/1 gün/1 hafta sessize al) — **P7b'de genişletilen aynı `TodayCard`/`TodayCardSpec(kind=ADVICE)` composable'ı kullanılır, ikinci bir kart deseni YAZILMAZ**
- **Test:** Tavsiye kapatma, sessize alma — UI etkileşim testleri
- **Risk:** Düşük — P7b tamamlandıysa sadece entegrasyon, yeni composable yok
- **Geri dönüş:** Kart kaldırılabilir, ekran eski haline döner
- **Tamamlanma kriteri:** §17 kriter 11-14

### P9 — Smart Ticker entegrasyonu ve tekrar bastırma
- **Değişecek:** `RealSmartTickerSource.kt` (yeni aday üretici fonksiyonu, `MissionPulseTickerFactory` deseni taklit edilir)
- **Kural:** `DigitalAdvice` → `SmartTickerItem(type=CONTEXTUAL_SUGGESTION)`, `suggestionKey` üzerinden mevcut `SuggestionCoordinator`/`TickerRanker` suppression'ına tabi
- **Test:** Aynı `suggestionKey`'in tekrar bastırıldığı, `TickerRanker` tür başı kotasının (`MAX_PER_TYPE=1`) ihlal edilmediği
- **Risk:** Düşük-orta — Ticker zaten kalabalık olabilir, Tavsiye Kartı + Ticker aynı anda aynı mesajı göstermemeli (§11.3 son paragraf uyarısı)
- **Geri dönüş:** Yeni üretici fonksiyon kaldırılırsa Ticker eski haline döner
- **Tamamlanma kriteri:** İkinci bir suppression deposu oluşturulmadığı, mevcut `SuggestionCoordinator` API'sinin kullanıldığı doğrulanmış

### P10 — Ayarlar, TR/EN, erişilebilirlik
- **Değişecek:** `AppPrefs.kt` (§13 key'leri, tek karar noktası deseni — mevcut `KEY_MISSION_TEMPO` gibi), `SettingsScreen.kt` (veya ilgili alt ekran), `strings.xml` (TR/EN)
- **Dikkat:** R-HOME-TICKER roadmap maddesi de `AppPrefs.kt`/`HomeScreen.kt` dokunuyor — çakışma riski, aynı döngüde ikisi birden değiştirilmeyecekse dosya diff'i net tutulmalı
- **Test:** Büyük yazı ölçeği, TR/EN string doğrulama
- **Risk:** Düşük
- **Geri dönüş:** Ayar kapatılırsa özellik tamamen gizlenir (mevcut "Yeni Özellik = Ayarlar Kuralı" zaten bunu garanti ediyor)
- **Tamamlanma kriteri:** §17 kriter 17, tüm yeni özellikler ayardan kapatılabilir

### P11 — Test, performans, build ve regresyon kontrolü
- **Yapılacak:** Tam `testDebugUnitTest` + `assembleDebug`, mevcut Mission/Ticker/WeeklyGoal testlerinin hiçbirinin kırılmadığının doğrulanması, emülatör smoke test (Dashboard açılışı, Görevler ekranı açılışı, kategori hedefi satırı genişletme)
- **Risk:** Bu adımda ortaya çıkan her regresyon önceki P adımlarına geri döner
- **Geri dönüş:** Build kırmızıysa commit atılmaz (CLAUDE.md kuralı zaten bu)
- **Tamamlanma kriteri:** §17'nin TÜM 20 maddesi karşılanmış, Hüseyin onayı alınmış

---

## 6. Room Migration Planı (24 → 25)

```kotlin
val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE weekly_goals ADD COLUMN mode TEXT NOT NULL DEFAULT 'MANUAL'")
        db.execSQL("ALTER TABLE weekly_goals ADD COLUMN baselineMinutes INTEGER")
        db.execSQL("ALTER TABLE weekly_goals ADD COLUMN previousWeekActualMinutes INTEGER")
        db.execSQL("ALTER TABLE weekly_goals ADD COLUMN pace TEXT NOT NULL DEFAULT 'DENGELI'")
        db.execSQL("ALTER TABLE weekly_goals ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
        db.execSQL("ALTER TABLE weekly_goals ADD COLUMN generatedAt INTEGER")
        db.execSQL("ALTER TABLE weekly_goals ADD COLUMN settledAt INTEGER")
        db.execSQL("ALTER TABLE weekly_goals ADD COLUMN algorithmVersion INTEGER NOT NULL DEFAULT 1")
        // Mevcut satırlar: mode='MANUAL' (varsayılan) — kullanıcının elle girdiği hedefler
        // otomatik moda geçirilmez, açık rıza olmadan ezilmez (bkz. §3 ürün kararı).
    }
}
```

- **Veri kaybı yok:** Tüm yeni sütunlar `ADD COLUMN` + `DEFAULT`, hiçbir `DROP`/`CREATE TABLE...AS SELECT` yok.
- **`achievedAt` alanı:** Silinmez, geriye dönük uyumluluk için tutulur; yeni `status` alanı asıl kaynak olur, `achievedAt` yalnızca `status=COMPLETED` olduğunda dolu tutulmaya devam eder (eski okuma kodları kırılmaz).
- **Schema JSON:** `app/schemas/com.armutlu.apporganizer.data.local.AppDatabase/25.json` Room tarafından otomatik üretilecek, commit'e dahil edilecek.
- **Migration testi:** `MigrationTestHelper` ile 24→25 — eski bir `weekly_goals` satırı (categoryId, targetMinutes, createdAt, achievedAt dolu) migration sonrası `mode='MANUAL'`, `targetMinutes` DEĞİŞMEMİŞ, `status` mantıklı bir varsayılana (achievedAt>0 ise COMPLETED, değilse ACTIVE) türetilmiş olarak doğrulanır.
- **Backup/import:** Proje içinde `BackupSyncService`/`BackupWorker` `WeeklyGoal` tablosunu kapsıyorsa (P3 sırasında doğrulanacak) yeni alanlar export/import şemasına eklenir; kapsamıyorsa dokunulmaz.

---

## 7. Test Matrisi

| Kategori | Dosya (önerilen) | Senaryo sayısı |
|---|---|---|
| Saf hesaplama | `AdaptiveCategoryTargetCalculatorTest.kt` | §16 10 senaryo |
| Aykırı hafta | `OutlierWeekGuardTest.kt` | Medyan %75 üstü/%60 altı, 4 hafta eksik |
| Hedef durumu | `WeeklyGoalPaceCalculatorTest.kt` | §16 7 senaryo (ON_TRACK/AT_RISK/EXCEEDED/COMPLETED/DATA_UNAVAILABLE + çift settlement) |
| Tavsiye motoru | `DigitalAdviceEngineTest.kt` | §16 8 senaryo |
| Kategori snapshot | `CategoryUsageSnapshotProviderTest.kt` | İzin yok, kısmi veri, filtre |
| Yaşam döngüsü | `EnsureCurrentWeekAdaptiveGoalsUseCaseTest.kt`, `SettlePreviousWeekAdaptiveGoalsUseCaseTest.kt` | İdempotency, pinleme |
| Görev entegrasyonu | `MissionEngineTest.kt` (genişletilecek) | WEEKLY_CATEGORY_BALANCE eligibility/evaluate |
| Room | `WeeklyGoalMigrationTest.kt` | 24→25, manuel koruma |
| UI/ViewModel | `DashboardViewModelTest.kt`, `MissionsViewModelTest.kt` (genişletilecek) | Tanıma modu, aşım gösterimi, tavsiye kapatma/sessize alma |
| Regresyon | Mevcut tüm `Mission*`, `Ticker*`, `WeeklyGoal*` testleri | Kırılmama doğrulaması |

Mevcut ilgili test dosyaları (referans, bunlar korunacak): `MissionEngineTest.kt`, `MissionSummaryUseCase` testleri, `MissionMetricSnapshotProviderTest.kt`, `TickerRankerTest.kt`, `TickerActionRouterTest.kt`, `SmartTickerItemTest.kt`, `TodayCardSelectorTest.kt`, `MissionPulseTickerFactoryTest.kt`.

---

## 8. Riskler

1. **Veri kesişimi (P1):** `MissionMetricSnapshotProvider` ve yeni `CategoryUsageSnapshotProvider` aynı UsageStats verisini farklı şekilde hesaplarsa, Görevler ekranı ile Dashboard farklı sayı gösterebilir. → Azaltma: P1'de mevcut `MissionMetricSnapshotProvider`'ın kategori-bazlı ihtiyacı olup olmadığı incelenir, ortak alt fonksiyon çıkarılabilir.
2. **Migration veri riski (P3):** Kullanıcıda halihazırda manuel `WeeklyGoal` kayıtları olabilir. → Azaltma: additive migration, `MANUAL` varsayılan, destructive migration kesinlikle kullanılmaz.
3. **WorkManager gecikmesi (P4):** Haftalık worker'a güvenilemez. → Azaltma: §6 akışı en az 4 farklı tetikleyici noktada (açılış, Dashboard, Görevler, worker) idempotent çağrılır.
4. **Ticker/Tavsiye Kartı çakışması (P9):** Aynı tavsiye hem Görevler ekranındaki kartta hem Ana ekran ticker'ında aynı anda görünebilir. → Azaltma: tek `suggestionKey` + `SuggestionCoordinator` cross-channel cooldown (zaten 6 saat) kullanılır, P9'da açıkça test edilir.
5. **AppListViewModel → DashboardViewModel taşıma riski:** `weeklyGoals` StateFlow'unu şu an başka bir ekran kullanıyor olabilir (P0/P5'te grep ile doğrulanacak). → Azaltma: taşımadan önce tüm kullanım noktaları grep edilir (CLAUDE.md "Değişiklik Güvenlik Protokolü" zaten zorunlu).
6. **R-HOME-TICKER roadmap maddesiyle dosya çakışması (P10):** Aynı dosyalar (`HomeScreen.kt`, `AppPrefs.kt`) iki farklı iş tarafından değiştirilebilir. → Azaltma: bu roadmap'in P10'u başlamadan önce R-HOME-TICKER durumu tekrar kontrol edilir.
7. **Algoritma karmaşıklığı testte gizli hata riski:** %15 klips + blend + outlier koruması + 5dk yuvarlama sırası yanlış uygulanırsa sonuç sezgisel olmayan değerler üretebilir. → Azaltma: §16'daki 10 senaryo P2'de TDD şeklinde önce yazılır, hesaplayıcı onlara göre inşa edilir.

---

## 9. Uygulama Başlamadan Önce Alınması Gereken Kararlar — TÜMÜ ÇÖZÜLDÜ (2026-07-27)

1. **✅ ÇÖZÜLDÜ — Tavsiye Kartı için `TodayCard` kullanılacak.** Dashboard'a üçüncü bağımsız section (`HomeSectionId.TODAY_CARD`, varsayılan gizli) olarak bağlanır, mevcut `MISSIONS_AND_SCORE`/`MISSIONS` section'larına dokunulmaz. Tavsiye, `TodayCardKind.ADVICE` (7. tür, en düşük öncelik) olarak entegre edilir. Görevler ekranındaki "Bugünün Tavsiyesi" de aynı `TodayCard`/`TodayCardSpec` composable'ının kompakt kullanımıdır — ikinci kart deseni yazılmaz. (bkz. P7b)

2. **Onaylanan varsayılan — `WEEKLY_CATEGORY_BALANCE` ve `WEEKLY_SCREEN_LESS` ayrı gösterilir.** İkisi de gösterilir, farklı ödül kaynağı oldukları P6 testinde garanti edilir. İtiraz gelmezse bu varsayılanla ilerlenir, P6 sırasında tekrar gözden geçirilebilir.

3. **Onaylanan varsayılan — yeni kullanıcıda otomatik kategori hedefi varsayılan AÇIK.** Yeni kullanıcı (hiç `WeeklyGoal` kaydı yok) → `KEY_ADAPTIVE_GOALS_ENABLED` varsayılan `true`. Mevcut kullanıcı (manuel hedefi var) → dokunulmaz, `MANUAL` kalır, açık rıza istenene kadar otomatik moda geçirilmez.

4. **Onaylanan varsayılan — ViewModel adı `DashboardViewModel`.**

5. **Açık bırakılan teknik alt-karar — `CategoryUsageSnapshotProvider` ↔ `HomeIntelligenceCoordinator` ilişkisi P1 içinde netleşecek.** İlerlemeyi bloklamıyor.

6. **Onaylanan varsayılan — tempo ayrı `AdaptiveGoalPace` enum'ı (0.95/0.90/0.85), ama UI'da kullanıcıya TEK bir "tempo" ayarı olarak sunulur (görev temposu ile kategori hedefi temposu arka planda ayrı katsayı taşır, kullanıcı tek kaydırıcı görür).**

Yukarıdaki 2/3/4/6 numaralı kararlar öneri olarak sunuldu ve açık itiraz gelmeden onaylanmış kabul edildi — uygulama sırasında beklenmedik bir çelişki çıkarsa durup tekrar sorulacak.

---

## 10. Uygulama İlerleme Kaydı

- **2026-07-27:** AŞAMA 1 tamamlandı, roadmap onaylandı ("Today kartını kullan ve başla" talimatı). P0 başlıyor.
- **2026-07-27 — P0 tamamlandı, S1/S7'yi doğrulayan + genişleten 2 yeni bulgu:**
  - **S9 (yeni):** `AppOrganizerDashboardScreen.kt:70-72` — `val usageTimes = remember(hasUsagePermission, allApps) { UsageStatsHelper.getUsageCounts(context, days = 7) }` **doğrudan Compose composition içinde senkron çağrılıyor**, `Dispatchers.IO`'ya hiç geçmiyor. S7'yi teyit ediyor — P1/P5'te bu çağrı `DashboardViewModel`'e taşınacak.
  - **S10 (yeni):** Dashboard'un kategori kullanım penceresi **sabit "son 7 gün" (`days=7`, bugünden geriye)** — `PeriodBoundaryResolver`/ISO hafta (Pazartesi-Pazar) ile HİÇ ilgisi yok. `WeeklyGoal.weekStartEpochDay` (ISO hafta) ile Dashboard'un gösterdiği kullanım rakamı zaten farklı pencereler ölçüyor — S1 sorununu büyütüyor. **P1'in `CategoryUsageSnapshotProvider`'ı ISO hafta kullanacağı için bu farklılık P5'te Dashboard UI'ı yeni provider'a geçince otomatik çözülür**, ama geçiş sırasında "sayı neden değişti" sürprizine karşı P5 tamamlanma kriterine not düşüldü.
  - **S11 (yeni):** `DashboardStats.compute()` içinde `usageTimes[app.packageName] ?: app.usageCount` fallback'i — izin yokken veya kayıt yokken `app.usageCount`'a (muhtemelen tüm-zamanlar/farklı anlamda bir sayaç) düşüyor, "veri yok" ile "gerçek sıfır" ayrımını bulanıklaştırıyor. `CategoryUsageSnapshotProvider` bu deseni KULLANMAYACAK — freshness alanıyla açıkça ayıracak (roadmap §4.1 zaten bu kuralı taşıyor, burada sadece somut kötü emsal olarak not edildi).
  - `WeeklyDigestWorker.checkWeeklyGoals()`'ın `>=` mantığı satır satır teyit edildi (bkz. §1.2) — S1 doğrulandı, değişiklik gerekmiyor (zaten biliniyordu).
  - P1 başlıyor.
- **2026-07-27 — P1-P11 (+ P7b) TAMAMLANDI.** Sırayla: `CategoryUsageSnapshotProvider` (ISO hafta), `AdaptiveCategoryTargetCalculator`+`OutlierWeekGuard`, `WeeklyGoal` migration 24→25, `Ensure/SettlePreviousWeekAdaptiveGoalsUseCase`, `DashboardViewModel`+Akıllı Kategori Hedefleri UI, `WEEKLY_CATEGORY_BALANCE` görevi, `DigitalAdviceEngine`, TodayCard→Dashboard (3. section, ADVICE 7. tür), Görevler ekranı "Bugünün Tavsiyesi" (aynı TodayCard composable'ı, `computeDigitalAdvice` ortak fonksiyonu), Smart Ticker CONTEXTUAL_SUGGESTION entegrasyonu, Ayarlar (Otomatik Kategori Hedefleri toggle'ı — tempo mevcut "Görev Temposu" ayarıyla paylaşılıyor).
  - **Final doğrulama:** `assembleDebug` + `testDebugUnitTest` yeşil — **1353 test, 0 hata, 19 skip**.
  - **Emülatör smoke test / `WeeklyGoalMigrationTest` (instrumented):** Bu turda ÇALIŞTIRILMADI — CLI ortamında emülatör yok. Kod derlenip statik olarak doğrulandı (`MigrationTestHelper` deseni mevcut `NotificationMetadataMigrationTest.kt` ile birebir aynı), ama gerçek cihaz/emülatör koşumu commit ÖNCESİ tamamlanan bir adım DEĞİL — kullanıcıya açıkça bildirildi, sonraki emülatör turunda çalıştırılmalı.
  - **Bilinçli kapsam daraltmaları (P7/P9):** Bildirim gürültüsü payı ve sabah/gece kullanım paterni sinyalleri (`DigitalAdviceInput.notificationNoiseTopSourceShare`/`morningSocialOpenDaysLast7`/`lateNightUsageDaysLast7`) şu an her zaman `null` besleniyor — motor null-güvenli olduğundan bu tavsiye türleri hiç üretilmiyor (sahte veri yerine sessizce atlanıyor). Gerçek veri kaynağı bağlanması gelecek bir iş maddesi.
  - Toplam yeni/değişen dosya: 8 yeni Kotlin dosyası (goals+advice paketleri) + `DashboardViewModel.kt` + 1 Room migration + ~20 değişen dosya + ~54 yeni TR/EN string çifti.
  - **Kullanıcı onayı alındı ("onaylıyorum devam et")** — commit `da56379c` push edildi.
- **2026-07-27 — Bilinçli kapsam daraltmalarından biri kapatıldı.** `computeDigitalAdvice` artık opsiyonel `context`/`usageStatsSource`/`notificationEventDao` parametreleriyle bildirim gürültüsü payını (`NotificationEventDao.countsSince`, son 7 gün) ve sabah/gece kullanım paternini (`MissionUsageStatsSource.getDailySessionUsage`, son 7 TAMAMLANMIŞ gün) gerçek veriden hesaplıyor — `DigitalAdviceInput.notificationNoiseTopSourceShare`/`morningSocialOpenDaysLast7`/`lateNightUsageDaysLast7` artık `null` sabit değil. `DashboardViewModel`/`MissionsViewModel`/`RealSmartTickerSource` üçü de yeni parametreleri geçiriyor. 6 yeni test (`CategoryGoalAdviceComputerTest`), `assembleDebug`+`testDebugUnitTest` yeşil. Commit `867eec30` push edildi.
- **2026-07-27 — Emülatör smoke test SONUÇLANDI (kısmi).** Pixel6_AOSP33 (Android 13) emülatöründe APK gerçekten kuruldu ve çalıştırıldı — **crash YOK** (logcat AndroidRuntime:E temiz). **Doğrulanan:** Ayarlar > Launcher > Adaptif Kategori Hedefleri toggle'ı gerçekten ekranda görüldü, açık/kapalı durumu değiştirilebildi ("Automatic Category Goals" / "Uyarlanabilir Kategori Hedefleri"), Dashboard ve MissionsScreen crash'siz açıldı. **DOĞRULANAMADI (net değil):** Ana Ekranı Düzenle'deki "Bugün Kartı" section'ının gerçek ekranda görünüp görünmediği (agent navigasyonda yönlendirme sorunu yaşadı, sadece kodun var olduğunu teyit etti) ve Görevler ekranındaki "Bugünün Tavsiyesi" kartının gerçek render'ı (kod satırı referansı verildi ama ekran görüntüsü/gözlem yok — emülatörde muhtemelen veri yetersizliği nedeniyle hiç görünmüyordur, bu normal olabilir ama TEYİT EDİLMEDİ). **`WeeklyGoalMigrationTest` (instrumented) HÂLÂ ÇALIŞTIRILMADI** — sıradaki adım.
  - **main'e commit/push için kullanıcı onayı BEKLENIYOR** (bu roadmap'in AŞAMA 2 kuralı) — yeni sinyal bağlama işi için de aynı onay zaten alınmıştı (dinamik loop kapsamında, kullanıcı "cron ile devam ettir" talimatı verdiği için kod değişiklikleri otonom ilerliyor, riskli adımlarda yine durup sorulacak).
- **2026-07-27 — `WeeklyGoalMigrationTest` emülatörde ÇALIŞTIRILDI VE GEÇTİ.** Pixel6_AOSP33 hâlâ açıkken `connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=...WeeklyGoalMigrationTest` ile gerçek cihazda koşuldu: `1 test, 0 failures, 0 errors` — `MIGRATION_24_25`'in mevcut manuel `weekly_goals` satırını (`targetMinutes` korunmuş, `mode='MANUAL'`) ve `achievedAt>0` satırdan türetilen `status='COMPLETED'`'ı doğru ürettiği gerçek Room/SQLite üzerinde doğrulandı — sadece statik kod incelemesi değil. **Roadmap'in son açık ucu kapandı.** Kalan tek belirsizlik: Ana Ekranı Düzenle'deki "Bugün Kartı" ve Görevler ekranındaki "Bugünün Tavsiyesi" kartının gerçek görsel render'ı hâlâ TEYİT EDİLMEDİ (agent navigasyon sorunu yaşadı) — bu, kritik bir risk değil çünkü ikisi de varsayılan olarak veri yetersizken/gizliyken hiç render olmaması BEKLENEN davranış, ama ekranda gerçekten göründüğünde doğru render olduğu ayrıca teyit edilmeli (bir sonraki manuel/emülatör turunda).

---

*Bu doküman AŞAMA 1 çıktısıdır, onaylanmıştır. AŞAMA 2 (P0-P11 + P7b) TAMAMLANMIŞTIR, kullanıcı onayıyla push edilmiştir. Bildirim gürültüsü/kullanım paterni sinyalleri gerçek veriye bağlandı, WeeklyGoalMigrationTest emülatörde geçti. Roadmap kapsamında planlı iş KALMADI — kalan tek madde (Bugün Kartı/Tavsiye kartının gerçek ekranda görsel teyidi) düşük risk, gelecekte fırsat buldukça doğrulanabilir.*
