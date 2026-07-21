package com.armutlu.apporganizer.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.armutlu.apporganizer"
private const val WAIT_TIMEOUT_MS = 5_000L

/**
 * Baseline Profile üretici — PERF-3.
 *
 * AppOrganizer tamamen Jetpack Compose ile yazıldığı için `android:id` tabanlı
 * View kaynak ID'leri YOK; UiAutomator seçicileri `contentDescription` (semantics)
 * veya jest (swipe) tabanlı çalışır:
 *  - Klasör açma: FolderTile.kt L125-127 `semantics(mergeDescendants=true) { contentDescription = folderLabel }`
 *    — folderLabel dinamik ("<kategori adı>, N uygulama...") olduğu için text/desc pattern eşleşmesi kullanılır.
 *  - AllAppsDrawer açma: buton YOK, HomeScreen üstten-yukarı swipe jestiyle açılıyor
 *    (bkz. HomeScreenComponents.kt L892-894 "Yukarı kaydırarak tüm uygulamaları aç" ipucu metni).
 *
 * Kapsanan kritik kullanıcı yolu (CLAUDE.md görev talimatı):
 *  1. Soğuk başlatma (MainActivity — LAUNCHER/MAIN intent-filter)
 *  2. İlk klasör tıklama (AppClassifier/kategori sorgusu + Room okuma tetiklenir)
 *  3. Geri dön
 *  4. AllAppsDrawer açma (blur + tam liste — Compose'un en ağır render yolu)
 *  5. Geri dön
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = TARGET_PACKAGE,
            maxIterations = 3,
            includeInStartupProfile = true
        ) {
            // 1) Soğuk başlatma — HomeScreen/MainActivity ilk render
            startActivityAndWait()
            device.waitForIdle(WAIT_TIMEOUT_MS)

            // 2) İlk klasör tile'ına dokun (varsa) — kategori sorgusu + FolderScreen render.
            // FolderTile semantics'i mergeDescendants ile Button rolünde tek bir node üretir;
            // contentDescription her zaman ", N uygulama" metnini içerir (bkz. FolderTile.kt L111).
            val folderTile = device.wait(
                Until.findObject(By.desc(java.util.regex.Pattern.compile(".*uygulama.*"))),
                WAIT_TIMEOUT_MS
            )
            if (folderTile != null) {
                folderTile.click()
                device.waitForIdle(2_000L)

                // 3) Geri dön
                device.pressBack()
                device.waitForIdle(1_000L)
            }

            // 4) AllAppsDrawer'ı aç — HomeScreen'de View tabanlı buton yok, yukarı swipe jesti gerekiyor.
            // Ekranın alt-orta noktasından üst-orta noktasına hızlı swipe.
            val displayWidth = device.displayWidth
            val displayHeight = device.displayHeight
            device.swipe(
                displayWidth / 2,
                (displayHeight * 0.85f).toInt(),
                displayWidth / 2,
                (displayHeight * 0.15f).toInt(),
                20
            )
            device.waitForIdle(2_000L)

            // 5) Geri dön
            device.pressBack()
            device.waitForIdle(500L)
        }
    }
}
