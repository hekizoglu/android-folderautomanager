# Macrobenchmark Kurulum — AppOrganizer (D2026-07-23)

**Durum:** ✅ Modül kurulu ve hazır
**Zorluk:** 7/10 (Emülatör testinde çöp, gerçek cihazda >30 dk)
**Hedef:** Cold start < 800ms, page < 200ms, folder < 300ms

---

## 1. Modül Yapısı

```
benchmark/
├── build.gradle.kts              # ✅ Yapılandırılmış
├── README.md                     # Detaylı dokümantasyon
└── src/
    ├── main/kotlin/.../
    │   └── BaselineProfileGenerator.kt    # ✅ Baseline profile capture (5 kritik yol)
    └── androidTest/kotlin/.../
        └── AppOrganizerBenchmark.kt       # ✅ Macrobenchmark suite (7 test)

app/
├── build.gradle.kts              # ✅ Baseline profil bağlantısı (baselineProfile project dep)
└── src/main/res/raw/
    └── baseline-prof.txt         # (release build sonrası oluşur)
```

---

## 2. Yapılan Değişiklikler

### Yeni Dosyalar

1. **`benchmark/src/androidTest/kotlin/com/armutlu/apporganizer/benchmark/AppOrganizerBenchmark.kt`** (528 satır)
   - 7 macrobenchmark testi:
     - `coldStart()` — Soğuk başlatma (< 800ms hedef)
     - `warmStart()` — Sıcak başlatma
     - `pageTransition()` — HorizontalPager swipe (< 200ms)
     - `folderOpen()` — FolderTile tap (< 300ms)
     - `allAppsDrawerOpen()` — Blur + LazyColumn (< 250ms)
     - `settingsNavigation()` — Settings geçişi
     - `allAppsScroll()` — Scroll performansı (P95 < 16.67ms)
   - Her test 3 iterasyon, FrameTimingMetric ile P50/P95/P99 ölçümü

2. **`benchmark/src/main/kotlin/com/armutlu/apporganizer/benchmark/BaselineProfileGenerator.kt`** (84 satır)
   - Baseline profile üretici (PERF-3)
   - 5 kritik yol capture eder:
     1. Soğuk başlatma
     2. Klasör açma
     3. Geri dön
     4. AllAppsDrawer açma
     5. Geri dön
   - `androidx.benchmark.macro.junit4.BaselineProfileRule` kullanır
   - Release build'de ART profili oluşturur

3. **`benchmark/README.md`** (200+ satır)
   - Detaylı benchmark dokümantasyonu
   - Performance targets, çalıştırma komutları, sonuçlar yorumlama

4. **`BENCHMARK_SETUP.md`** (bu dosya)
   - Kurulum ve çalıştırma kılavuzu

### Değiştirilmemiş (Mevcut Konfigürasyon)

- `benchmark/build.gradle.kts` — Zaten `androidx.benchmark:benchmark-macro-junit4:1.2.4` içeriyor
- `settings.gradle.kts` — `:benchmark` zaten include'lu
- `app/build.gradle.kts` — `baselineProfile(project(":benchmark"))` dependency zaten var

---

## 3. Önşartlar

### Cihaz/Emülatör

- **Fiziksel Cihaz (Önerilen):**
  - Android 12+ (frame timing doğruluğu için)
  - USB debugging açık
  - Developer Options aktif
  - Pil > %50
  - Ekran açık, idle 15+ dakika (cache warm-up)
  
- **Emülatör (Destekleniyor ama Slow):**
  - API 30+ (minSdk 28, targetSdk 35 compatible)
  - Host CPU: quad-core+ (benchmark sırasında fan açılabilir)
  - İşaretleme: `-gpu host -no-audio` (hız için)
  - Beklenen test süresi: 45-60 dakika

### Geliştirici Ortamı

```powershell
# Android SDK kontrol
$androidHome = $env:ANDROID_HOME
if (!$androidHome) { Write-Host "ERROR: ANDROID_HOME yok"; exit 1 }

# ADB path
$adb = "$androidHome\platform-tools\adb.exe"
& $adb devices  # Cihaz bağlı mı kontrol et
```

---

## 4. Kurulum Adımları

### Adım 1: Proje İndir & Gradle Sync

```powershell
cd c:\Users\hekizoglu\Documents\AppOrganizer

# Gradle daemon temizle (eski hata kalmaması için)
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force

# İlk sync (dependencies indir)
.\gradlew --version
```

### Adım 2: Test APK Derle

```powershell
# Benchmark test APK derle (benchmarkRelease varyant)
.\gradlew :benchmark:assembleBenchmarkRelease

# Çıktı:
# benchmark/build/outputs/apk/benchmarkRelease/benchmark-benchmarkRelease.apk
```

### Adım 3: Target App APK Derle (Release)

```powershell
# Release build'i derle (baseline profile sonrası embed edilecek)
# Kısayol: şu aşamada debug APK de çalışır, release opsiyonel
.\gradlew :app:assembleDebug

# (Daha sonra release için:)
# .\gradlew :app:assembleRelease -PallowDebugReleaseSigning=true
```

### Adım 4: APK'ler Cihaza Yükle

```powershell
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"

# App
& $adb install -r app\build\outputs\apk\debug\app-debug.apk

# Test APK
& $adb install -r benchmark\build\outputs\apk\benchmarkRelease\benchmark-benchmarkRelease.apk

# Doğrula
& $adb shell pm list packages | grep "apporganizer"
```

---

## 5. Baseline Profile Üretimi (İlk Kez)

```powershell
cd c:\Users\hekizoglu\Documents\AppOrganizer

# Device/emulator 15+ dakika idle iken çalıştır
.\gradlew :app:generateReleaseBaselineProfile -PallowDebugReleaseSigning=true

# Beklenen çıktı (10-20 dakika):
# > Task :benchmark:collectNonMinifiedReleaseBaselineProfile
# > Baseline profile generated at:
#   app/build/outputs/baseline_profile_src/release/baseline-prof.txt (~50-100 KB)
```

**Çıktı:**
```
app/build/outputs/baseline_profile_src/release/baseline-prof.txt
```

**İçerik (örnek):**
```
0x0e7d    # Classes to precompile (16-bit offsets, ART format)
0x1234
...
HLI
com/armutlu/apporganizer/presentation/ui/launcher/HomeScreen.<init>
com/armutlu/apporganizer/data/repository/AppRepository.getAllApps
...
```

**Release Build'e Embed Etme:**
```powershell
# Release APK derle (baseline prof otomatik embed edilir)
.\gradlew :app:assembleRelease -PallowDebugReleaseSigning=true

# Doğrula (APK içinde baseline prof var mı):
# unzip -l app/build/outputs/apk/release/app-release.apk | grep baseline
```

---

## 6. Macrobenchmark Testleri Çalıştır

### Tüm Testler

```powershell
cd c:\Users\hekizoglu\Documents\AppOrganizer

# Physical device
.\gradlew :benchmark:connectedAndroidTest

# Emulator (warnings suppress)
.\gradlew :benchmark:connectedAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR_NOT_DETECTED
```

**Beklenen Süre:** 15-30 dakika (device), 45-90 dakika (emulator)

### Tek Test Çalıştır

```powershell
# Cold start
.\gradlew :benchmark:connectedBenchmarkReleaseAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.armutlu.apporganizer.benchmark.AppOrganizerBenchmark#coldStart

# Folder open
.\gradlew :benchmark:connectedBenchmarkReleaseAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.armutlu.apporganizer.benchmark.AppOrganizerBenchmark#folderOpen
```

---

## 7. Sonuçları Oku

### CSV Çıktısı Konumu

```
build/outputs/connected_android_test/*/AdditionalTestOutputs/
├── benchmark-results-physical-DEVICE_MODEL-API_LEVEL.txt
└── benchmarkData/
    ├── AppOrganizerBenchmark_coldStart.json
    ├── AppOrganizerBenchmark_warmStart.json
    ├── AppOrganizerBenchmark_pageTransition.json
    ├── AppOrganizerBenchmark_folderOpen.json
    ├── AppOrganizerBenchmark_allAppsDrawerOpen.json
    ├── AppOrganizerBenchmark_settingsNavigation.json
    └── AppOrganizerBenchmark_allAppsScroll.json
```

### Örnek Sonuç İncelemesi

```powershell
# Dosyayı aç
$results = Get-Content 'build/outputs/connected_android_test/*/AdditionalTestOutputs/benchmark-results-physical-*.txt'
$results | Select-String "coldStart|median|frame"
```

**Örnek Çıktı:**
```
AppOrganizerBenchmark#coldStart
  MedianMs: 650
  MinMs: 620
  MaxMs: 720
  FRAME_TIMING_P50: 12.3
  FRAME_TIMING_P90: 18.7
  FRAME_TIMING_P95: 22.1
  FRAME_TIMING_P99: 25.0

AppOrganizerBenchmark#pageTransition
  MedianMs: 180
  MinMs: 170
  MaxMs: 195
  FRAME_TIMING_P95: 16.8
```

### Yorumlama

| Test | Result | Durum | İşlem |
|------|--------|-------|-------|
| coldStart: 650ms | < 800ms ✅ | PASS | Devam |
| coldStart: 950ms | > 800ms ❌ | FAIL | Profiling/optimization gerekli |
| pageTransition: 185ms | < 200ms ✅ | PASS | Devam |
| FRAME_TIMING_P95: 22ms | > 16.67ms ⚠️ | CAUTION | 1-2 frame drop, ok |
| FRAME_TIMING_P99: 50ms | >> 16.67ms ❌ | FAIL | Recomposition tuning gerekli |

---

## 8. Optimize Etme (Hedefler Aşıldıysa)

### Cold Start > 800ms
- **Sebep:** AppClassifier init, Room migration, Firebase init
- **Çözüm:**
  ```kotlin
  // LazyColumnLauncher: Kategorileri demand'de load et
  // LazyCollection pattern: AppRepository.getFolder(catId) async
  // Baseline profile coverage artır (generateReleaseBaselineProfile 5 kez)
  ```

### Frame Timing P95 > 20ms (> 1.2 frame @ 60fps)
- **Sebep:** LazyColumn recomposition, derivedStateOf overuse
- **Çözüm:**
  ```kotlin
  // Profetto trace: android_studio_profiler.trace al
  // Studio > Profiler > Live Objects tab ile allocation track et
  // Unnecessary recompose kaldır (remember/derivedStateOf pattern)
  ```

### AllAppsDrawer > 250ms
- **Sebep:** Blur composable render + 300+ item mount
- **Çözüm:**
  ```kotlin
  // BlurModifier optimizasyon: graphicsLayer sampling disabled
  // LazyColumn initialScrollPosition = 0
  // Icon async load'u pre-cache et
  ```

---

## 9. CI/CD Entegrasyonu (Opsiyonel)

GitHub Actions workflow (`.github/workflows/benchmark.yml`):

```yaml
name: Performance Benchmark
on:
  push:
    branches: [main]
  schedule:
    - cron: '0 2 * * 1'  # Pazartesi 02:00 UTC

jobs:
  benchmark:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - uses: android-actions/setup-android@v2
      - run: ./gradlew :benchmark:connectedAndroidTest
      - uses: actions/upload-artifact@v3
        with:
          name: benchmark-results
          path: build/outputs/connected_android_test/
```

---

## 10. Checklist — Hazırlık

- [ ] `:benchmark` modülü `:app` ile derlenebiliyor (`./gradlew :benchmark:assemble`)
- [ ] `BaselineProfileGenerator.kt` 5 kritik yol implement'li
- [ ] `AppOrganizerBenchmark.kt` 7 test implement'li
- [ ] Cihaz USB debugged'e bağlı (`adb devices` pozitif)
- [ ] Cihaz 15+ dakika idle, ekran açık
- [ ] APK'ler yüklendi (`adb shell pm list packages | grep apporganizer`)
- [ ] Baseline profile üretildi (`app/build/outputs/baseline_profile_src/release/` var)
- [ ] Ilk benchmark run başlatıldı (`./gradlew :benchmark:connectedAndroidTest`)
- [ ] Sonuçlar kaydedildi ve hedeflere karşı kontrol edildi

---

## 11. Hata Giderme

### Build Hatası: `Cannot locate tasks that match ':benchmark:...'`
```powershell
# Gradle daemon temizle
Get-Process java | Stop-Process -Force
Remove-Item -Recurse -Force .gradle

# Retry
.\gradlew :benchmark:tasks
```

### APK Yükleme Hatası: `INSTALL_FAILED_VERSION_DOWNGRADE`
```powershell
# Önceki versiyon kaldır
adb uninstall com.armutlu.apporganizer
adb uninstall com.armutlu.apporganizer.benchmark

# Yeniden yükle
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r benchmark/build/outputs/apk/benchmarkRelease/benchmark-benchmarkRelease.apk
```

### Test Timeout: `JUNIT_FAILURE: Test took xxx seconds to complete`
- Emulator CPU yüksek kullanım? OS resources tight?
- Cihaz ekranı kilitli mi? `adb shell input keyevent POWER` ile aç
- Arka plan uygulamaları kapat (Developer Options > Don't keep activities)

### Baseline Profile Boş: `baseline-prof.txt < 1KB`
- `useConnectedDevices = true` benchmark'de aktif mi?
- Device 15+ dakika idle'da mı?
- AppOrganizerBenchmark testleri çalıştı mı (5 kez)?

---

## 12. Sonraki Adımlar

**D2026-07-24:**
- Baseline profile cihazda üret (15 dakika)
- Macrobenchmark testleri çalıştır (30 dakika)
- Sonuçları hedeflerle karşılaştır

**D2026-07-25:**
- Hedefleri aşan testleri profil et (Perfetto)
- Optimizasyon önerilerini topla

**D2026-08-01:**
- Recomposition tuning (cold start > 800ms ise)
- Scroll jank fix (P95 > 20ms ise)
- Baseline profile üretime dahil et

**D2026-08-15:**
- CI/CD'ye benchmark step ekle
- Release build'e baseline profile embed et
- Play Store versiyonda hedefleri doğrula

---

## Referanslar

- **CLAUDE.md §5:** Uyumluluk matrisi, AGP/Kotlin/BOM versions
- **LEARNINGS.md:** BuildCache lock fix (D181), Unit test UTF-8 issue (D201)
- [Google Benchmark Macro Docs](https://developer.android.com/studio/profile/benchmark)
- [Baseline Profile Guide](https://developer.android.com/topic/performance/baseline-profiles)
- [Perfetto Tracing](https://perfetto.dev/)

---

**Son Güncelleme:** 2026-07-23  
**Kodu Yazan:** Claude Haiku 4.5  
**Durum:** ✅ Hazır — İlk test bekleniyor
