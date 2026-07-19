package com.armutlu.apporganizer.domain.usecase.insight

import com.armutlu.apporganizer.domain.home.SmartTickerType
import com.armutlu.apporganizer.domain.usecase.insight.DeviceTidinessInsights.AppUsageSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [DeviceTidinessInsights] testleri (Döngü G8 —
 * GOREV_SISTEMI_AKILLI_GELISTIRME_PLANI.md G8, satır 57-66).
 *
 * Eşik mantıkları: doluluk/kazanç/sayı/pay eşikleri altı-üstü, izin yokken üretim yok.
 * Toggle testi ayrı katmanda (RealSmartTickerSource) — burada saf üretici fonksiyonlar test edilir.
 */
class DeviceTidinessInsightsTest {

    private val fixedNow = 1_700_000_000_000L
    private val msPerDay = 24L * 3600 * 1000
    private val gb = 1024L * 1024 * 1024

    private val texts = DeviceTidinessInsights.TidinessTexts(
        storageTitle = { percent, unusedCount, gbFreed -> "storage:$percent:$unusedCount:$gbFreed" },
        unusedTitle = { count -> "unused:$count" },
        notificationTitle = { total, share -> "notif:$total:$share" },
        diagnosticsTitle = { "diagnostics" },
        actionInspect = "Incele",
        actionReport = "Rapor",
        actionFix = "Duzelt",
    )

    private fun unusedApp(pkg: String, daysAgo: Long, sizeBytes: Long) = AppUsageSnapshot(
        packageName = pkg,
        lastUsedTimestamp = fixedNow - daysAgo * msPerDay,
        appSizeBytes = sizeBytes,
    )

    // ---- storageOpportunity ----

    @Test
    fun `storage - below fullness threshold does not produce`() {
        val apps = listOf(unusedApp("a", 100, 600 * 1024 * 1024L))
        val result = DeviceTidinessInsights.storageOpportunity(
            hasUsageAccessPermission = true,
            totalBytes = 100 * gb,
            freeBytes = 20 * gb, // 80% used, below 85% threshold
            apps = apps,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertNull(result)
    }

    @Test
    fun `storage - full enough but reclaimable below 500MB does not produce`() {
        val apps = listOf(unusedApp("a", 100, 100 * 1024 * 1024L))
        val result = DeviceTidinessInsights.storageOpportunity(
            hasUsageAccessPermission = true,
            totalBytes = 100 * gb,
            freeBytes = 10 * gb, // 90% used
            apps = apps,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertNull(result)
    }

    @Test
    fun `storage - full and reclaimable above thresholds produces item`() {
        val apps = listOf(
            unusedApp("a", 100, 400 * 1024 * 1024L),
            unusedApp("b", 120, 300 * 1024 * 1024L),
        )
        val result = DeviceTidinessInsights.storageOpportunity(
            hasUsageAccessPermission = true,
            totalBytes = 100 * gb,
            freeBytes = 10 * gb, // 90% used
            apps = apps,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertTrue(result != null)
        assertEquals(SmartTickerType.CONTEXTUAL_SUGGESTION, result!!.type)
        assertEquals("tidiness_storage", result.suggestionKey)
    }

    @Test
    fun `storage - no usage access permission does not produce`() {
        val apps = listOf(unusedApp("a", 100, 800 * 1024 * 1024L))
        val result = DeviceTidinessInsights.storageOpportunity(
            hasUsageAccessPermission = false,
            totalBytes = 100 * gb,
            freeBytes = 5 * gb,
            apps = apps,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertNull(result)
    }

    @Test
    fun `storage - recently used apps are excluded from reclaimable size`() {
        val apps = listOf(unusedApp("a", 10, 800 * 1024 * 1024L)) // only 10 days unused
        val result = DeviceTidinessInsights.storageOpportunity(
            hasUsageAccessPermission = true,
            totalBytes = 100 * gb,
            freeBytes = 5 * gb,
            apps = apps,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertNull(result)
    }

    // ---- unusedAppsOpportunity ----

    @Test
    fun `unused - below minimum count does not produce`() {
        val apps = (1..4).map { unusedApp("pkg$it", 100, 10 * 1024 * 1024L) }
        val result = DeviceTidinessInsights.unusedAppsOpportunity(
            hasUsageAccessPermission = true,
            apps = apps,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertNull(result)
    }

    @Test
    fun `unused - at minimum count produces item`() {
        val apps = (1..5).map { unusedApp("pkg$it", 100, 10 * 1024 * 1024L) }
        val result = DeviceTidinessInsights.unusedAppsOpportunity(
            hasUsageAccessPermission = true,
            apps = apps,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertTrue(result != null)
        assertEquals("tidiness_unused", result!!.suggestionKey)
    }

    @Test
    fun `unused - no usage access permission does not produce`() {
        val apps = (1..10).map { unusedApp("pkg$it", 100, 10 * 1024 * 1024L) }
        val result = DeviceTidinessInsights.unusedAppsOpportunity(
            hasUsageAccessPermission = false,
            apps = apps,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertNull(result)
    }

    @Test
    fun `unused - exactly at 90 day boundary counts as unused`() {
        val apps = (1..5).map { unusedApp("pkg$it", 90, 10 * 1024 * 1024L) }
        val result = DeviceTidinessInsights.unusedAppsOpportunity(
            hasUsageAccessPermission = true,
            apps = apps,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertTrue(result != null)
    }

    @Test
    fun `unused - 89 days does not count as unused`() {
        val apps = (1..5).map { unusedApp("pkg$it", 89, 10 * 1024 * 1024L) }
        val result = DeviceTidinessInsights.unusedAppsOpportunity(
            hasUsageAccessPermission = true,
            apps = apps,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertNull(result)
    }

    // ---- notificationLoadOpportunity ----

    @Test
    fun `notifications - below weekly total threshold does not produce`() {
        val counts = listOf(80, 50, 30)
        val result = DeviceTidinessInsights.notificationLoadOpportunity(
            weeklyCountsByPackage = counts,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertNull(result)
    }

    @Test
    fun `notifications - above total but top3 share below threshold does not produce`() {
        // total = 210 (>=200), top3 = 30+30+30 = 90, share = 42% (<60%)
        val counts = listOf(30, 30, 30, 30, 30, 30, 30)
        val result = DeviceTidinessInsights.notificationLoadOpportunity(
            weeklyCountsByPackage = counts,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertNull(result)
    }

    @Test
    fun `notifications - above total and top3 share above threshold produces item`() {
        // total = 220, top3 = 80+70+60 = 210, share = 95%
        val counts = listOf(80, 70, 60, 5, 5)
        val result = DeviceTidinessInsights.notificationLoadOpportunity(
            weeklyCountsByPackage = counts,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertTrue(result != null)
        assertEquals("tidiness_notifications", result!!.suggestionKey)
    }

    // ---- selfDiagnosisOpportunity ----

    @Test
    fun `diagnosis - permission granted does not produce`() {
        val result = DeviceTidinessInsights.selfDiagnosisOpportunity(
            hasUsageAccessPermission = true,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertNull(result)
    }

    @Test
    fun `diagnosis - permission denied produces item`() {
        val result = DeviceTidinessInsights.selfDiagnosisOpportunity(
            hasUsageAccessPermission = false,
            nowMillis = fixedNow,
            texts = texts,
        )
        assertTrue(result != null)
        assertEquals("tidiness_permission", result!!.suggestionKey)
    }

    // ---- all() ----

    @Test
    fun `all - permission denied and no notification load yields only diagnosis item`() {
        val apps = (1..10).map { unusedApp("pkg$it", 100, 100 * 1024 * 1024L) }
        val items = DeviceTidinessInsights.all(
            hasUsageAccessPermission = false,
            totalBytes = 100 * gb,
            freeBytes = 5 * gb,
            apps = apps,
            weeklyNotificationCountsByPackage = listOf(10, 5),
            nowMillis = fixedNow,
            texts = texts,
        )
        assertEquals(1, items.size)
        assertEquals("tidiness_permission", items.first().suggestionKey)
    }

    @Test
    fun `all - permission denied does not gate notification load item`() {
        // Notification load is independent of usage-access permission (own data source).
        val apps = (1..10).map { unusedApp("pkg$it", 100, 100 * 1024 * 1024L) }
        val items = DeviceTidinessInsights.all(
            hasUsageAccessPermission = false,
            totalBytes = 100 * gb,
            freeBytes = 5 * gb,
            apps = apps,
            weeklyNotificationCountsByPackage = listOf(300),
            nowMillis = fixedNow,
            texts = texts,
        )
        assertEquals(2, items.size)
        assertTrue(items.any { it.suggestionKey == "tidiness_permission" })
        assertTrue(items.any { it.suggestionKey == "tidiness_notifications" })
    }

    @Test
    fun `all - no conditions met yields empty list`() {
        val apps = listOf(unusedApp("a", 5, 10 * 1024 * 1024L))
        val items = DeviceTidinessInsights.all(
            hasUsageAccessPermission = true,
            totalBytes = 100 * gb,
            freeBytes = 50 * gb,
            apps = apps,
            weeklyNotificationCountsByPackage = listOf(10, 5),
            nowMillis = fixedNow,
            texts = texts,
        )
        assertTrue(items.isEmpty())
    }
}
