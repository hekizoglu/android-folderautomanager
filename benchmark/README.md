# AppOrganizer Benchmark Module — Macrobenchmark & Baseline Profile

## Overview

This module (`benchmark`) contains performance measurement tests for AppOrganizer launcher:

- **Baseline Profile Generator** (`BaselineProfileGenerator.kt`): Captures critical user paths for ART optimization
- **Macrobenchmark Suite** (`AppOrganizerBenchmark.kt`): Measures cold start, page transitions, and frame timing

## Performance Targets (D2026-07-23)

| Metric | Target | Notes |
|--------|--------|-------|
| Cold Start | < 800ms | Process launch → `reportFullyDrawn()` |
| Warm Start | < 400ms | App in memory, clear + restart |
| Page Transition (Swipe) | < 200ms | HorizontalPager animation |
| Folder Open (Tap) | < 300ms | FolderTile click → FolderScreen visible |
| AllAppsDrawer Open | < 250ms | Blur + LazyColumn mount |
| Scroll (AllAppsDrawer) | P95 < 16.67ms | 60 FPS consistency |

## Architecture

### BaselineProfileGenerator
- **File:** `src/main/kotlin/com/armutlu/apporganizer/benchmark/BaselineProfileGenerator.kt`
- **Purpose:** Captures ART (Android Runtime) profiling data for optimized app startup
- **Flow:**
  1. Launch app (cold start)
  2. Click first folder (Room query + classification)
  3. Back to home
  4. Swipe up to open AllAppsDrawer (blur + list)
  5. Back

### AppOrganizerBenchmark
- **File:** `src/androidTest/kotlin/com/armutlu/apporganizer/benchmark/AppOrganizerBenchmark.kt`
- **Purpose:** Measures frame timing and performance metrics for critical user journeys
- **Metrics Used:** `FrameTimingMetric()` (P50, P90, P95, P99 frame times)
- **Tests:**
  - `coldStart()` — Process launch timing (3 iterations)
  - `warmStart()` — App in-memory restart (3 iterations)
  - `pageTransition()` — ClassFolder swipe animation
  - `folderOpen()` — Tap → FolderScreen render
  - `allAppsDrawerOpen()` — Blur + LazyColumn load
  - `settingsNavigation()` — HomeScreen → Settings → Home
  - `allAppsScroll()` — Scroll performance in drawer

## Running Benchmarks

### Prerequisites
- Physical Android device or high-end emulator (15+ minutes idle)
- `minSdk = 28`, `targetSdk = 35`
- Device in developer mode, USB debugging enabled
- ADB recognized: `adb devices`

### 1. Generate Baseline Profile

```powershell
cd c:\Users\hekizoglu\Documents\AppOrganizer

# On device (recommended) or emulator (15+ min, slow)
.\gradlew :app:generateReleaseBaselineProfile -PallowDebugReleaseSigning=true
```

**Output:**
```
app/build/outputs/baseline_profile_src/release/baseline-prof.txt
→ Embedded in release APK at build time
```

**Duration:** 10-15 minutes (device), 30+ minutes (emulator)

### 2. Run Macrobenchmarks

```powershell
cd c:\Users\hekizoglu\Documents\AppOrganizer

# All tests
.\gradlew :benchmark:connectedAndroidTest

# Single test
.\gradlew :benchmark:connectedBenchmarkReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.armutlu.apporganizer.benchmark.AppOrganizerBenchmark#coldStart

# Emulator only (suppress warnings, results unreliable)
.\gradlew :benchmark:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR_NOT_DETECTED
```

### 3. View Results

After tests complete:

```
build/outputs/connected_android_test/*/AdditionalTestOutputs/
├── benchmark-results-physical-*.txt (device results)
└── benchmarkData/
    ├── AppOrganizerBenchmark_coldStart.json
    ├── AppOrganizerBenchmark_warmStart.json
    ├── AppOrganizerBenchmark_pageTransition.json
    ├── AppOrganizerBenchmark_folderOpen.json
    └── ... (other tests)
```

**Sample output line:**
```
AppOrganizerBenchmark#coldStart:
  median: 650ms, min: 620ms, max: 720ms
  p95: 710ms, p99: 720ms
  FrameTimings: dropped_frames=2, P95_frame_time=18.2ms
```

## UiAutomator Selectors (Jetpack Compose)

Since AppOrganizer is pure Compose with no View IDs, benchmarks use:

1. **`By.desc(Pattern)`** — `contentDescription` from `semantics {}`
   ```kotlin
   // FolderTile.kt: semantics { contentDescription = "Müzik, 42 uygulama" }
   By.desc(Pattern.compile(".*uygulama.*"))
   ```

2. **Swipe gestures** — No button elements
   ```kotlin
   device.swipe(fromX, fromY, toX, toY, steps)  // AllAppsDrawer swipe
   ```

3. **Custom matchers** — Found in `BaselineProfileGenerator` as reference

## Build Configuration

### build.gradle.kts
- **Plugin:** `com.android.test` (test APK, not app)
- **Baseline Profile Plugin:** `androidx.baselineprofile`
- **Dependencies:**
  - `androidx.benchmark:benchmark-macro-junit4:1.2.4`
  - `androidx.test.uiautomator:uiautomator:2.3.0`
  - `androidx.test.espresso:espresso-core:3.5.1`

### targetProjectPath
- Benchmark APK targets `:app` module
- Baseline profile data feeds back to app release build

## Known Issues

1. **Emulator unreliable:** Results vary wildly due to host machine CPU scheduling. Physical device required for production benchmarks.

2. **Baseline Profile Plugin AGP 8.6.1 warning:** Plugin tested only up to AGP 8.3.0; 8.6.1 works but logs warning. Safe to ignore.

3. **UiAutomator race conditions:** Swipe timing depends on device frame rate. Add `device.waitForIdle()` after gestures.

4. **Build cache lock (D181):** If `checkBenchmarkReleaseAarMetadata` fails with "Unable to delete directory," kill Gradle daemons:
   ```powershell
   Get-Process java | Stop-Process -Force
   Remove-Item -Recurse -Force benchmark\build
   ```

## Integration with Release Build

When `:app:assembleRelease` is called:

1. Baseline profile data is merged into release APK
2. `aapt2` compiles profile to binary format
3. ART uses it during first app boot for optimized compilation
4. ~15-20% cold start improvement typical

## Troubleshooting

### Test fails with "Package not installed"
```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb install -r .\benchmark\build\outputs\apk\benchmarkRelease\benchmark-benchmarkRelease.apk
```

### "No matching variant" error on `generateReleaseBaselineProfile`
- Ensure `allowDebugReleaseSigning=true` flag is passed
- Check `:app` has release build type defined
- Verify `:benchmark` is included in `settings.gradle.kts`

### Frame timing shows as `null` in results
- Device may have disabled frame profiling (developer option)
- Check: **Settings → Developer Options → Profile GPU rendering: Off**
- Ensure device running Android 12+ for accurate frame timing

## Next Steps

1. **D2026-07-24:** Run baseline profile on production device
2. **D2026-07-25:** Measure cold start/warm start vs. targets
3. **D2026-08-01:** Profile with Perfetto traces if P95 > 50ms
4. **D2026-08-15:** Integrate into CI/CD (GitHub Actions macrobenchmark step)

## References

- [Google Benchmark macro documentation](https://developer.android.com/studio/profile/benchmark)
- [Baseline Profile best practices](https://developer.android.com/topic/performance/baseline-profiles)
- [UiAutomator API](https://developer.android.com/training/testing/ui-automator)
- [FrameTimingMetric guide](https://developer.android.com/studio/profile/measure-ui-performance)
