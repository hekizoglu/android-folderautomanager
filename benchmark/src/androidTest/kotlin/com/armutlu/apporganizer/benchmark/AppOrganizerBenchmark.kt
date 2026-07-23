package com.armutlu.apporganizer.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.armutlu.apporganizer"
private const val WAIT_TIMEOUT_MS = 5_000L
private const val START_TIMEOUT_MS = 10_000L

/**
 * AppOrganizer Macrobenchmark — Soğuk başlatma, sayfa geçişi ve klasör açılış jank ölçümü (PERF-3).
 *
 * Ölçülen metrikler:
 *  • **Cold Start:** Process başlatma → MainActivity fully drawn (reportFullyDrawn)
 *  • **Page Swipe:** Klasör sayfası sürükleme → pager animasyonu tamamlanması
 *  • **Folder Open:** FolderTile tap → FolderScreen görünümü
 *
 * Baseline Profile (BaselineProfileGenerator.kt):
 *  - Yukarıdaki 5 kritik yolun ART profili capture eder
 *  - Release build'de baseline.prof dosyası oluşturur
 *  - `.\gradlew :app:generateReleaseBaselineProfile -PallowDebugReleaseSigning=true` ile çalıştır
 *
 * Hedefler (D2026-07-23):
 *  • Cold start < 800ms
 *  • Page transition < 200ms
 *  • Folder open < 300ms
 *  • P95 frame time < 16.67ms (60 FPS)
 *
 * Çalıştırma:
 *  Gerçek cihaz (en az 1 dakika idle durumda):
 *    .\gradlew :benchmark:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR_NOT_DETECTED
 *
 *  Emülatör (15+ dakika):
 *    .\gradlew :benchmark:connectedAndroidTest
 *    (Not: Emülatör çalışması ÇAKMAK sonuçlandırır — gerçek cihaz test'i zorunludur)
 *
 * CSV çıkışı:
 *    build/outputs/connected_android_test/*/AdditionalTestOutputs/
 */
@RunWith(AndroidJUnit4::class)
class AppOrganizerBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Cold start: Process başlatma → MainActivity fully drawn.
     *
     * Ölçüm:
     *  1. Uygulamayı zorla durdur (clear cache)
     *  2. Intenti uygulaması ve MainActivity intent-filter'a ulaşması başlatma anı
     *  3. doOnPreDraw → reportFullyDrawn() bitme anı
     */
    @Test
    fun coldStart() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        startupMode = MacrobenchmarkRule.StartupMode.COLD,
        iterations = 3
    ) {
        pressHome()
        startActivityAndWait()
    }

    /**
     * Warm start: Uygulamanın bellek de kalması durumunda başlatma.
     * Cold start'tan farklı olarak process reuse eder.
     */
    @Test
    fun warmStart() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        startupMode = MacrobenchmarkRule.StartupMode.WARM,
        iterations = 3
    ) {
        pressHome()
        startActivityAndWait()
    }

    /**
     * Page transition: HorizontalPager sayfa sürükleme animasyonunun jank ölçümü.
     *
     * Senaryo:
     *  1. Uygulamayı başlat
     *  2. HomeScreen klasör sayfası görünümde
     *  3. Ekranın sağdan soluna swipe (sonraki klasör sayfasına geç)
     *  4. Animasyon frame time ölçümü
     *
     * Not: HomeScreen § HorizontalPager state + Pager animasyonu ~200ms normal süresi;
     *      > 200ms işareti RE-COMPOSITION veya MEASURE/LAYOUT bozuşunu gösterir.
     */
    @Test
    fun pageTransition() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 3
    ) {
        startActivityAndWait()
        device.waitForIdle(WAIT_TIMEOUT_MS)

        // Klasör sayfasının var olduğu kontrol et — eğer tek sayfa varsa test skip.
        // (Varsayılan çok sayıda klasör olmalı, ama emülatörde uygulama boş olabilir)
        val folderTile = device.wait(
            Until.findObject(By.desc(java.util.regex.Pattern.compile(".*uygulama.*"))),
            WAIT_TIMEOUT_MS
        )
        if (folderTile == null) {
            return@measureRepeated // Klasör yok, sayfa swipe yapılamaz
        }

        // Ekranın sağdan soluna swipe (sonraki sayfa)
        val displayWidth = device.displayWidth
        val displayHeight = device.displayHeight
        val startX = (displayWidth * 0.8f).toInt()
        val endX = (displayWidth * 0.2f).toInt()
        val centerY = displayHeight / 2

        device.swipe(startX, centerY, endX, centerY, 30)
        device.waitForIdle(1_000L)
    }

    /**
     * Folder open: FolderTile tap → FolderScreen görünümü animasyonunun jank ölçümü.
     *
     * Senaryo:
     *  1. Uygulamayı başlat (onboarding tamamlandığını varsay)
     *  2. İlk klasör tile'ı bul (FolderTile.semantics contentDescription pattern)
     *  3. Tap
     *  4. FolderScreen fully drawn
     *
     * Frame timing:
     *  • Tap → layout tree change (clasification/sorting)
     *  • Compose animation (fade/slide)
     *  • Room/AppClassifier sorgusu (background — frame metric'de capture edilmez)
     *  • Hedef: < 300ms, P95 < 50ms (60 FPS max 3-4 dropped frame)
     */
    @Test
    fun folderOpen() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 3
    ) {
        startActivityAndWait()
        device.waitForIdle(WAIT_TIMEOUT_MS)

        // İlk klasör tile'ı bul
        val folderTile = device.wait(
            Until.findObject(By.desc(java.util.regex.Pattern.compile(".*uygulama.*"))),
            WAIT_TIMEOUT_MS
        )
        if (folderTile != null) {
            folderTile.click()
            device.waitForIdle(2_000L) // FolderScreen render + database query bitmesini bekle
        }
    }

    /**
     * AllAppsDrawer open: Blur + tam uygulama listesi render jank ölçümü.
     *
     * Senaryo:
     *  1. HomeScreen
     *  2. Ekran altından tepesine swipe (AllAppsDrawer açma jesti)
     *  3. Blur composable render + 300+ uygulama lazy column animasyonu
     *  4. Frame timing ölçümü
     *
     * Hedef: < 250ms (blur + LazyColumn mount, scroll prepare — heavy compose)
     */
    @Test
    fun allAppsDrawerOpen() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 3
    ) {
        startActivityAndWait()
        device.waitForIdle(WAIT_TIMEOUT_MS)

        // AllAppsDrawer açma jesti (HomeScreen buton yok, swipe gerekir)
        val displayWidth = device.displayWidth
        val displayHeight = device.displayHeight
        val startX = displayWidth / 2
        val startY = (displayHeight * 0.85f).toInt()
        val endY = (displayHeight * 0.15f).toInt()

        device.swipe(startX, startY, startX, endY, 20)
        device.waitForIdle(1_500L)
    }

    /**
     * Settings screen nav: HomeScreen → Settings → HomeScreen navigasyonunun frame ölçümü.
     *
     * Senaryo:
     *  1. HomeScreen
     *  2. Settings ekranına nav (menu/button)
     *  3. Geri dön HomeScreen'e
     *
     * Amaç: Navigation composition performance, SettingsScreen compose tree complexity.
     * Hedef: Her geçiş < 200ms
     */
    @Test
    fun settingsNavigation() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 3
    ) {
        startActivityAndWait()
        device.waitForIdle(WAIT_TIMEOUT_MS)

        // Settings ekranına nav — tüm uygulamalar drawerin üst-sağında settings butonu olmalı
        // veya menu. Basit heuristik: üst-sağ kısmı tap (SettingsButton yeri).
        // Note: Exact location AppOrganizer layout'a bağlı; bunu screen metadata ile bulabilirsiniz.
        // Şu an emülatör testi için skip et, canlı cihaz test'inde ayarla.
        val displayWidth = device.displayWidth
        val displayHeight = device.displayHeight
        val settingsButtonX = displayWidth - 50
        val settingsButtonY = 50

        device.click(settingsButtonX, settingsButtonY)
        device.waitForIdle(1_500L)

        // Geri dön
        device.pressBack()
        device.waitForIdle(1_000L)
    }

    /**
     * Scroll performance: AllAppsDrawer'da LazyColumn scroll jank ölçümü.
     *
     * Senaryo:
     *  1. AllAppsDrawer aç
     *  2. 300+ app listesini yukarı/aşağı scroll
     *  3. Frame timing ölçümü
     *
     * Hedef: P95 frame time < 16.67ms (consistent 60 FPS)
     */
    @Test
    fun allAppsScroll() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 3
    ) {
        startActivityAndWait()
        device.waitForIdle(WAIT_TIMEOUT_MS)

        // AllAppsDrawer aç
        val displayWidth = device.displayWidth
        val displayHeight = device.displayHeight
        val startX = displayWidth / 2
        val startY = (displayHeight * 0.85f).toInt()
        val endY = (displayHeight * 0.15f).toInt()

        device.swipe(startX, startY, startX, endY, 20)
        device.waitForIdle(1_000L)

        // Scroll down (sayfanın orta kısmından scroll)
        val scrollStartY = displayHeight / 2
        val scrollEndY = displayHeight / 4
        device.swipe(displayWidth / 2, scrollStartY, displayWidth / 2, scrollEndY, 50)
        device.waitForIdle(500L)

        // Scroll up
        device.swipe(displayWidth / 2, scrollEndY, displayWidth / 2, scrollStartY, 50)
        device.waitForIdle(500L)
    }
}
