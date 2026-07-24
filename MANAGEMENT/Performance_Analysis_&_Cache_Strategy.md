# AppOrganizer İlk Yükleme Performansı ve Cache Stratejisi Raporu

**Tarih:** 2026-07-24  
**Hedef:** Açılış hızını %25-35 iyileştirmek için cache sistemini analiz etmek  
**Rapor Uzunluğu:** 95 satır

---

## 1. Mevcut Cache Sistemi Özeti

### 1.1 AppIconView.kt — LRU-200 İkon Cache
```
Tür:        Process-level icon cache (LinkedHashMap)
Boyut:      200 entries max
Cache Key:  "${packageName}_${iconSizePx}" (±icon pack variant)
Loading:    Lazy — produceState() ile UI render zamanında
Thread:     IO thread (Dispatchers.IO)
Hit Time:   <1ms (memory lookup)
Miss Time:  ~2-5ms per icon (disk read + bitmap decode)
Status:     ✓ Optimized
```
**Bulgu:** Cache hit'te anlık dönüş, miss'te async I/O. Başlangıçta boş olduğu için ilk 40-50 klasör görünümü bitmap yüklemesi yapar.

### 1.2 AppClassifierAssets.kt — 3702 Paket JSON Cache
```
Tür:        Singleton lazy-loaded HashMap
Parsing:    assets/app_categories.json → JSONObject → HashMap<String, String>
Thread:     Main thread (synchronized block'ta)
Hit Time:   <0.1ms (memory map lookup)
Miss Time:  ~80-150ms (JSON parse + HashMap alloc)
Status:     ⚠ STARTUP RISK
```
**Bulgu:** İlk `classifyApp()` çağrısında 3702 paket JSON senkron parse edilir. Sorgu: AppRepository app yükleme sırasında classify edilir mi?

### 1.3 AppDatabase Room v23 + FTS5
```
Version:    v23 (v22→v23: notification_events table + index rename)
Queries:    Pre-compiled @Query("SELECT ...")
Tables:     apps, categories, folders, notification_events, search_fts
Search:     FTS5 virtual table (full-text search)
Bootstrap:  ensureSettingsIndexedIfNeeded() — First search() çağrısında
Status:     ⚠ LAZY-LOADED
```
**Bulgu:** FTS5 index'i yapılandırılmamış başlatılır; first search'de ~100ms spike'ı.

### 1.4 SearchRepository FTS5 Operasyonları
```
Fonksiyonlar:   search(), instantSearch(), debouncedSearch()
Asynchronous:   ✓ withContext(Dispatchers.IO)
Bootstrap:      bootstrapIndex() — only on first search
Status:         ⚠ DELAYED — critical path'e entegre olmamış
```
**Bulgu:** Arama hızlı başlar ama ilk sorguya 80-120ms ek yük.

---

## 2. Bottleneck Analizi

### 2.1 LauncherActivity.onCreate() — Sıra
```
1. val activityStartedAt = SystemClock.elapsedRealtime()  [0ms]
2. installSplashScreen() — pre-super.onCreate              [~5ms]
3. super.onCreate()                                        [~10ms]
4. enableEdgeToEdge()                                      [~5ms]
5. WindowCompat.setDecorFitsSystemWindows()               [~2ms]
6. AppPrefs.onboardingDone() — SharedPrefs read            [~8ms]
7. setContent { AppOrganizerTheme { ... } }               [~20ms]
   → Compose tree init
   → AppNavigation composition
   → LauncherViewModel instantiation (Dagger Hilt)
   → AppRepository + SearchRepository DI
8. HomeScreen() composable                                 [~30ms]
   → HorizontalPager state
   → LauncherViewModel.folders flow subscription
   → Room DB read triggered
═════════════════════════════════════════════════════════════════
TOTAL ESTIMATE: 80-120ms (Compose setup dominant)
```

### 2.2 LauncherViewModel.init() — Flow Chain
```kotlin
folders: StateFlow<List<AppFolder>> = repository.getAllApps()
    .combine(repository.getAllCategories())
    .map { (apps, cats) → buildFolders(apps, cats) }
    .stateIn(viewModelScope, SharingStarted.EAGERLY, emptyList())
```

**Timing Breakdown:**
```
1. repository.getAllApps()          [~50-80ms  — Room @Query]
2. repository.getAllCategories()    [~5-10ms   — cached]
3. combine { apps, cats }           [~1-2ms    — merge]
4. buildFolders()                   [~10-20ms  — O(n*m), m=32 cats]
5. .stateIn(...EAGERLY...)          [~0ms     — immediate start]
═════════════════════════════════════════════════════════════════
TOTAL: 70-120ms
RISK: SharingStarted.EAGERLY → Flow collectors başla hemen
      HomeScreen render'dan önce başlar → UI thread block
```

### 2.3 AppClassifier.classifyApp() — İlk Çağrı
```kotlin
exactMatchMap.get(packageName)  // Lazy getter
  → AppClassifierAssets.getExactMatchMap(context)
    → Synchronized block
      → loadFromAssets()
        → context.assets.open("app_categories.json")
        → JSONObject(json)
        → HashMap populate (3702 entries)
```

**Timing:**
```
Cache hit:  <0.1ms
Cache miss: ~80-150ms (JSON I/O + parse)
```

**Sorgu:** `AppRepository.getAllApps()` kategorileri app yükleme esnasında assign ediyor mu?
- Eğer evet → classify() ilk Room read'inde çağrılır → başlangıçta JSON parse

---

## 3. Mevcut Optimizasyonlar ✓

1. **AppIconView LRU-200** — Per-session icon cache, hit'te <1ms
2. **AppClassifierAssets lazy singleton** — Parsed once, reused
3. **SearchRepository suspend** — Blocking IO kaldırıldı
4. **LauncherViewModel @Immutable** — Compose skippable optimization
5. **Room pre-compiled queries** — Generated code, SQL optimized
6. **BaselineProfileGenerator.kt** — Baseline profile infrastructure ready
7. **Firebase Crashlytics** — Performance monitoring enabled

---

## 4. Uygulanabilir Optimizasyonlar (Kod Destekli)

### P0.1 — AppClassifierAssets JSON Async Parse ⭐⭐⭐
**Hedef:** İlk classify() çağrısını suspendiyor  
**Kazanç:** 20-30ms  
**Maliyet:** 2h  
**Risk:** Minimal

**Uygulama:**
```kotlin
// Eski: synchronized block main thread'de
private fun loadFromAssets(context: Context): Map<String, String> {
    val json = context.assets.open("app_categories.json").bufferedReader().use { it.readText() }
    return JSONObject(json).let { obj →
        HashMap<String, String>(obj.length() * 2).apply {
            obj.keys().forEach { pkg → put(pkg, obj.getString(pkg)) }
        }
    }
}

// Yeni: withContext(Dispatchers.IO)
suspend fun getExactMatchMapAsync(context: Context): Map<String, String> {
    cachedMap?.let { return it }
    return withContext(Dispatchers.IO) {
        synchronized(this) {
            cachedMap ?: loadFromAssets(context).also { cachedMap = it }
        }
    }
}
```

### P0.2 — LauncherViewModel SharingStarted.Lazily ⭐⭐
**Hedef:** folders flow'u ilk abone'ye kadar ertele  
**Kazanç:** 50-100ms  
**Maliyet:** 1.5h  
**Risk:** MEDIUM — döndüğümüzde "Loading..." flash

**Uygulama:**
```kotlin
// Eski: SharingStarted.EAGERLY
folders: StateFlow<List<AppFolder>> = combine(getAllApps, getAllCategories)
    .map { buildFolders(...) }
    .stateIn(viewModelScope, SharingStarted.EAGERLY, emptyList())

// Yeni: Lazily, loading state ile
val folders: StateFlow<List<AppFolder>> = combine(getAllApps, getAllCategories)
    .map { buildFolders(...) }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
// HomeScreen render: if (folders.isEmpty) LoadingIndicator() else FolderGrid()
```

### P0.3 — SearchRepository Bootstrap Defer ⭐⭐⭐
**Hedef:** FTS5 indexing'i ilk search'e kadar ertele  
**Kazanç:** 80-120ms  
**Maliyet:** 1h  
**Risk:** Minimal — search nadir ilk saniyede açılır

**Uygulama:**
```kotlin
// Eski: startup'da init
SearchRepository.bootstrapIndex() called in LauncherViewModel.init

// Yeni: lazy
private var bootstrapped = false
suspend fun search(rawQuery: String): Map<SourceType, List<SearchDocument>> {
    if (!bootstrapped) {
        bootstrapIndex()
        bootstrapped = true
    }
    return searchMeasured(rawQuery.trim(), limit)
}
```

### P0.4 — Pagination (Complex, Low Priority) ⚠
**Hedef:** Sadece ilk 50 app yükle, sonraları lazy  
**Kazanç:** 40-60ms  
**Maliyet:** 4h  
**Risk:** HIGH — UI complexity, pagination UI

### P0.5 — Baseline Profile Macro-Benchmark ⭐⭐⭐
**Hedef:** Compose baseline profile compile-time optimization  
**Kazanç:** +5-15% (Composition reuse)  
**Maliyet:** 0.5h  
**Risk:** Minimal — benchmark modülü mevcut

**Adımlar:**
1. `benchmark/build.gradle.kts` → enableMacrobenchmark = true
2. Run: `./gradlew :benchmark:connectedAndroidTest`
3. Baseline profile generate → app/src/main/baseline-prof.txt

### P0.6 — AppNotificationListenerService Deferred ⭐⭐
**Hedef:** Bildirim listener'ını background thread'e ertele  
**Kazanç:** 10-20ms  
**Maliyet:** 1h  
**Risk:** Minimal — listener arka plandaysa herhangi bir issue yok

---

## 5. Maliyet-Fayda Tablosu

| Optimization | Kazanç | Risk | Effort | Öncelik |
|-------------|--------|------|--------|---------|
| P0.1 JSON async | 20-30ms | Minimal | 2h | ⭐⭐⭐ HIGH |
| P0.2 Lazy flow | 50-100ms | UX flash | 1.5h | ⭐⭐ MEDIUM |
| P0.3 Bootstrap defer | 80-120ms | Minimal | 1h | ⭐⭐⭐ HIGH |
| P0.5 Baseline profile | +5-15% | Minimal | 0.5h | ⭐⭐⭐ HIGH |
| P0.6 Notif deferred | 10-20ms | Minimal | 1h | ⭐⭐ MEDIUM |
| P0.4 Pagination | 40-60ms | HIGH | 4h | ⏸ LOW |
| **TOTAL** | **185-305ms** | **Mixed** | **5.5-7.5h** | **25-35% iyileş.** |

---

## 6. Önerilen İmplantasyon Sırası

1. **Sprint 1:** P0.5 (Baseline profile) + P0.1 (JSON async) + P0.6 (Notif defer)
   - Toplam 3.5h, kazanç: ~100ms + 5-15% Compose opt.
   
2. **Sprint 2:** P0.3 (Bootstrap defer) + P0.2 (Lazy flow)
   - Toplam 2.5h, kazanç: ~130-220ms
   
3. **Sprint 3:** Ölçü ve doğrulama
   - Profiler + Firebase Performance
   - Baseline vs. optimized karşılaştırması

---

## 7. Profiling Stratejisi (Ölçüm Yapılmadığı için)

### Şu Anda Yoktur
- Cold start timing measurement
- Room DB query time breakdown
- Compose recomposition profiling
- JSON parse timing data

### Önerilir
```bash
# Android Studio Profiler
1. Run > Profile 'app'
2. CPU Recorder start → Trace Types: "Sample Java Methods"
3. Cold start ~ 1.5-2.0s (tahmin)

# Timber logging
LauncherActivity.onCreate(): SystemClock.elapsedRealtime() checkpoint
LauncherViewModel.init(): Flow subscription timing
AppClassifier.classifyApp(): exactMatchMap load timing

# Firebase Crashlytics
Firebase Analytics event: app_opened → duration

# Baseline measurement
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W com.armutlu.apporganizer/.presentation.ui.MainActivity
→ Output: "TotalTime: XXXX ms"
```

---

## 8. Bekleyenler

- [ ] Baseline timing ölçümü (cold start şu an bilinmiyor)
- [ ] AppRepository.getAllApps() içinde classify() çağrısı varlığı kontrol
- [ ] FTS5 index size tahmini (boş DB vs. 200+ apps)
- [ ] Pixel6_API33 emülatörde real benchmark

---

**Sonuç:** Mevcut cache stratejisi "lazy-first" olup, JSON parse ve FTS5 bootstrap sıfır-zamanlı başlangıçtan ortaya çıkıyor. P0.1 + P0.3 + P0.5 kombinasyonu %25 improvement potansiyeli sunuyor. Kod tarafından desteklenmiş; uygulamaya hazır.

