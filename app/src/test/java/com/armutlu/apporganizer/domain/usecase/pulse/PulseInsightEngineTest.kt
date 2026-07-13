package com.armutlu.apporganizer.domain.usecase.pulse

import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PulseInsightEngine testleri (D244) — kullanıcının istediği 5 senaryo: öncelik sırası,
 * sahte içgörü yok, tekrar önleme (dönüşümlü gösterim), uzunluk/arg kontrolü, olumlu/olumsuz.
 */
class PulseInsightEngineTest {

    private val now = System.currentTimeMillis()
    private val day = 24L * 60 * 60 * 1000

    private fun app(pkg: String, categoryId: String = "productivity") = WrappedEngine.AppSnapshot(
        packageName = pkg,
        appName = pkg,
        categoryId = categoryId,
        usageCount = 10L,
        lastUsedTimestamp = now - 1 * day,
        installTime = now - 100 * day,
        firstInstalledTime = now - 100 * day,
        appSizeBytes = 0L,
        isHidden = false,
        isSystemApp = false,
    )

    // 1) Öncelik sırası: ciddi bildirim sorunu, olumlu gelişmeden önce gelir.
    @Test
    fun `notification issue has higher priority than positive insight`() {
        val input = PulseInput(
            apps = listOf(app("com.a")),
            notification = PulseNotificationSignals(20, 5, 5),
            previousCategoryUsage = null,
            nowMillis = now,
        )
        val pulse = DigitalPulseEngine.compute(input)
        val specs = PulseInsightEngine.generate(input, pulse)
        val top = specs.minByOrNull { it.priority }
        assertEquals(PulseInsightType.NOTIF_ISSUE, top?.type)
    }

    // 2) Veri yoksa sahte içgörü üretilmez (apps boş → liste boş döner).
    @Test
    fun `no fabricated insight when there is no data`() {
        val input = PulseInput(apps = emptyList(), notification = null, previousCategoryUsage = null, nowMillis = now)
        val pulse = DigitalPulseEngine.compute(input)
        val specs = PulseInsightEngine.generate(input, pulse)
        assertTrue(specs.isEmpty())
    }

    // 3) Tekrar önleme: son gösterilen id varsa ve başka aday varsa, aynısı seçilmez.
    @Test
    fun `pickInsight avoids repeating the last shown id when alternatives exist`() {
        val specs = listOf(
            PulseInsightSpec("a", PulseInsightType.GENERAL, priority = 0, positive = null, routeKey = null),
            PulseInsightSpec("b", PulseInsightType.GENERAL, priority = 1, positive = null, routeKey = null),
        )
        val picked = PulseInsightEngine.pickInsight(specs, lastShownId = "a")
        assertEquals("b", picked?.id)
    }

    // 4) Tek aday varsa, son gösterilenle aynı olsa bile o gösterilir (boş kalmasın).
    @Test
    fun `pickInsight returns the only candidate even if it matches last shown id`() {
        val specs = listOf(
            PulseInsightSpec("a", PulseInsightType.GENERAL, priority = 0, positive = null, routeKey = null),
        )
        val picked = PulseInsightEngine.pickInsight(specs, lastShownId = "a")
        assertEquals("a", picked?.id)
    }

    // 5) Olumlu/olumsuz işaretleme doğru: bildirim yükü sakinse pozitif, kullanılmayan
    // uygulama önerisi olumsuz (nötr değil, ama yargılayıcı değil — sadece pozitif=false).
    @Test
    fun `positive and negative insight flags are set correctly`() {
        val calmInput = PulseInput(
            apps = listOf(app("com.a")),
            notification = PulseNotificationSignals(10, 0, 0),
            previousCategoryUsage = null,
            nowMillis = now,
        )
        val calmPulse = DigitalPulseEngine.compute(calmInput)
        val calmSpecs = PulseInsightEngine.generate(calmInput, calmPulse)
        assertTrue(calmSpecs.any { it.type == PulseInsightType.NOTIF_CALM && it.positive == true })

        val unusedApps = (1..6).map {
            WrappedEngine.AppSnapshot(
                packageName = "com.unused.app$it",
                appName = "app$it",
                categoryId = "productivity",
                usageCount = 0L,
                lastUsedTimestamp = now - 90 * day,
                installTime = now - 200 * day,
                firstInstalledTime = now - 200 * day,
                appSizeBytes = 0L,
                isHidden = false,
                isSystemApp = false,
            )
        }
        val unusedInput = PulseInput(apps = unusedApps, notification = null, previousCategoryUsage = null, nowMillis = now)
        val unusedPulse = DigitalPulseEngine.compute(unusedInput)
        val unusedSpecs = PulseInsightEngine.generate(unusedInput, unusedPulse)
        assertTrue(unusedSpecs.any { it.type == PulseInsightType.UNUSED_APPS && it.positive == false })
    }
}
