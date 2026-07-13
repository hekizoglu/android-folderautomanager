package com.armutlu.apporganizer.domain.usecase.pulse

import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DigitalPulseEngine V2 senaryo testleri (D244) — kullanıcının verdiği 12 maddelik listeyi
 * kapsar: boş liste crash yok, eksik izin cezalandırmaz, yüksek sosyal/oyun tek başına
 * düşürmez, skor hep 0..100, ilk hafta delta null, confidence düşer vb.
 */
class DigitalPulseEngineTest {

    private val now = System.currentTimeMillis()
    private val day = 24L * 60 * 60 * 1000

    private fun app(
        pkg: String,
        categoryId: String,
        usageCount: Long = 10L,
        lastUsedTimestamp: Long = now - 1 * day,
        installTime: Long = now - 100 * day,
        firstInstalledTime: Long = now - 100 * day,
        isSystemApp: Boolean = false,
        isHidden: Boolean = false,
    ) = WrappedEngine.AppSnapshot(
        packageName = pkg,
        appName = pkg.substringAfterLast('.'),
        categoryId = categoryId,
        usageCount = usageCount,
        lastUsedTimestamp = lastUsedTimestamp,
        installTime = installTime,
        firstInstalledTime = firstInstalledTime,
        appSizeBytes = 10L * 1024 * 1024,
        isHidden = isHidden,
        isSystemApp = isSystemApp,
    )

    // 1) Boş liste crash yok, skor 0..100 döner.
    @Test
    fun `empty app list does not crash and returns neutral-ish score in range`() {
        val result = DigitalPulseEngine.compute(PulseInput(apps = emptyList(), notification = null, previousCategoryUsage = null, nowMillis = now))
        assertTrue(result.total in 0..100)
        assertTrue(result.organization in 0..100)
    }

    // 2) Bildirim izni yoksa (notification = null) dikkat alt skoru cezalandırılmaz — nötr kalır.
    @Test
    fun `missing notification permission does not penalize attention subscore`() {
        val apps = listOf(app("com.a", "productivity"))
        val result = DigitalPulseEngine.compute(PulseInput(apps = apps, notification = null, previousCategoryUsage = null, nowMillis = now))
        assertEquals(DigitalPulseEngine.NEUTRAL_SUBSCORE, result.attention)
    }

    // 3) Yüksek sosyal/oyun kullanımı TEK BAŞINA skoru düşürmez (V1 -15 cezası kaldırıldı).
    @Test
    fun `high social and game usage alone does not lower the score`() {
        val socialHeavy = (1..10).map {
            app("com.social.app$it", categoryId = "social", usageCount = 500L)
        }
        val balanced = (1..10).map {
            app("com.mixed.app$it", categoryId = "productivity", usageCount = 500L)
        }
        val socialResult = DigitalPulseEngine.compute(
            PulseInput(apps = socialHeavy, notification = null, previousCategoryUsage = null, nowMillis = now)
        )
        val balancedResult = DigitalPulseEngine.compute(
            PulseInput(apps = balanced, notification = null, previousCategoryUsage = null, nowMillis = now)
        )
        // Kategori tek başına organization/attention/cleanup/consistency'yi etkilemediği için
        // iki senaryonun toplam skoru birbirine yakın olmalı (sosyal olduğu için otomatik ceza yok).
        assertEquals(balancedResult.total, socialResult.total)
    }

    // 4) Skor her zaman 0..100 aralığında (aşırı kötü senaryoda bile).
    @Test
    fun `score is always clamped between 0 and 100 in worst case scenario`() {
        val apps = (1..20).map {
            app(
                "com.bad.app$it",
                categoryId = "uncategorized",
                usageCount = 1000L,
                lastUsedTimestamp = now - 90 * day,
                installTime = now - 200 * day,
                firstInstalledTime = now - 200 * day,
            )
        }
        val result = DigitalPulseEngine.compute(
            PulseInput(
                apps = apps,
                notification = PulseNotificationSignals(500, 20, 20, nightCount = 200),
                previousCategoryUsage = mapOf("productivity" to 100L),
                unlockCount = 900,
                previousUnlockCount = 10,
                nowMillis = now,
            )
        )
        assertTrue(result.total in 0..100)
        assertTrue(result.organization in 0..100)
        assertTrue(result.attention in 0..100)
        assertTrue(result.balance in 0..100)
        assertTrue(result.cleanup in 0..100)
        assertTrue(result.consistency in 0..100)
    }

    // 5) İlk hafta (previousCategoryUsage null) balance nötr kalır — sahte karşılaştırma yok.
    @Test
    fun `first week with no baseline keeps balance subscore neutral`() {
        val apps = listOf(app("com.a", "social", usageCount = 500L))
        val result = DigitalPulseEngine.compute(
            PulseInput(apps = apps, notification = null, previousCategoryUsage = null, nowMillis = now)
        )
        assertEquals(DigitalPulseEngine.NEUTRAL_SUBSCORE, result.balance)
    }

    // 6) Eksik veri → confidence LOW/MEDIUM'a düşer, asla ekstra ceza vermez.
    @Test
    fun `missing signals lower confidence but never crash or force score to zero`() {
        val apps = listOf(app("com.a", "productivity"))
        val result = DigitalPulseEngine.compute(
            PulseInput(apps = apps, notification = null, previousCategoryUsage = null, hasUsageAccess = false, nowMillis = now)
        )
        assertEquals(DataConfidence.LOW, result.confidence)
        assertTrue(result.total in 0..100)
    }

    // 7) Tüm sinyaller mevcut olunca confidence HIGH olur.
    @Test
    fun `all signals present yields high confidence`() {
        val apps = listOf(app("com.a", "productivity"))
        val result = DigitalPulseEngine.compute(
            PulseInput(
                apps = apps,
                notification = PulseNotificationSignals(5, 0, 0),
                previousCategoryUsage = mapOf("productivity" to 5L),
                unlockCount = 50,
                previousUnlockCount = 45,
                hasUsageAccess = true,
                nowMillis = now,
            )
        )
        assertEquals(DataConfidence.HIGH, result.confidence)
    }

    // 8) Sistem uygulamaları ve yeni kurulanlar temizlik skorunda cezalandırılmaz.
    @Test
    fun `system apps and recently installed apps are excluded from cleanup penalty`() {
        val apps = listOf(
            app("com.system", "productivity", isSystemApp = true, lastUsedTimestamp = now - 200 * day, installTime = now - 300 * day, firstInstalledTime = now - 300 * day),
            app("com.new", "productivity", lastUsedTimestamp = now - 200 * day, installTime = now - 5 * day, firstInstalledTime = now - 5 * day),
        )
        val result = DigitalPulseEngine.compute(
            PulseInput(apps = apps, notification = null, previousCategoryUsage = null, nowMillis = now)
        )
        // Değerlendirmeye giren uygun uygulama kalmadığı için nötr olmalı.
        assertEquals(DigitalPulseEngine.NEUTRAL_SUBSCORE, result.cleanup)
    }

    // 9) Kilit açma sayısı yüksek ama önceki haftaya göre stabilse istikrar cezalandırılmaz.
    @Test
    fun `high but stable unlock count does not penalize consistency`() {
        val apps = listOf(app("com.a", "productivity"))
        val result = DigitalPulseEngine.compute(
            PulseInput(apps = apps, notification = null, previousCategoryUsage = null, unlockCount = 300, previousUnlockCount = 290, nowMillis = now)
        )
        assertTrue(result.consistency >= 70)
    }

    // 10) Kilit açma sayısı sert değişince istikrar düşer.
    @Test
    fun `volatile unlock count lowers consistency`() {
        val apps = listOf(app("com.a", "productivity"))
        val result = DigitalPulseEngine.compute(
            PulseInput(apps = apps, notification = null, previousCategoryUsage = null, unlockCount = 300, previousUnlockCount = 50, nowMillis = now)
        )
        assertTrue(result.consistency < DigitalPulseEngine.NEUTRAL_SUBSCORE)
    }

    // 11) Çok sayıda kategorisiz uygulama düzeni düşürür.
    @Test
    fun `many uncategorized apps lower organization score`() {
        val tidy = (1..15).map { app("com.tidy.app$it", categoryId = "productivity") }
        val messy = (1..15).map { app("com.messy.app$it", categoryId = "uncategorized") }
        val tidyResult = DigitalPulseEngine.compute(PulseInput(apps = tidy, notification = null, previousCategoryUsage = null, nowMillis = now))
        val messyResult = DigitalPulseEngine.compute(PulseInput(apps = messy, notification = null, previousCategoryUsage = null, nowMillis = now))
        assertTrue(messyResult.organization < tidyResult.organization)
    }

    // 12) Rahatsız edici bildirim yükü dikkat skorunu düşürür ama toplamı 0'ın altına düşürmez.
    @Test
    fun `disturbing notification load lowers attention but never crashes below zero`() {
        val apps = listOf(app("com.a", "productivity"))
        val result = DigitalPulseEngine.compute(
            PulseInput(
                apps = apps,
                notification = PulseNotificationSignals(200, 10, 10, nightCount = 100),
                previousCategoryUsage = null,
                nowMillis = now,
            )
        )
        assertTrue(result.attention < DigitalPulseEngine.NEUTRAL_SUBSCORE)
        assertTrue(result.attention >= 0)
        assertTrue(result.total in 0..100)
    }
}
