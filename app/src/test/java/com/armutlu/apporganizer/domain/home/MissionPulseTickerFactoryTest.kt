package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseScore
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseSnapshot
import com.armutlu.apporganizer.domain.usecase.pulse.PulseReasonId
import com.armutlu.apporganizer.domain.usecase.pulse.PulseScoreReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [MissionPulseTickerFactory] testleri (Döngü T03 —
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1727-1789).
 *
 * Sabit `nowMillis` ile deterministik — her üretim şartı ve tekrar önleme (roadmap 1.4)
 * ayrı test edilir: ham skor/rutin ilerleme ASLA üretilmemeli, yalnız risk/son süre/yeni
 * başarı (görev) ve anlamlı değişim/çözülebilir sorun (skor) şeride girmeli.
 */
class MissionPulseTickerFactoryTest {

    private val fixedNow = 1_700_000_000_000L

    private fun summary(
        completedCount: Int = 1,
        totalCount: Int = 3,
        primaryMissionId: String? = "m1",
        primaryTitle: String? = "Ekran süresi",
        primaryCurrentText: String? = "2 sa. 40 dk.",
        primaryRemainingText: String? = "20 dk. kaldı",
        primaryStatus: MissionStatus? = MissionStatus.IN_PROGRESS,
        primaryProgressFraction: Float? = null,
    ) = HomeMissionSummary(
        completedCount = completedCount,
        totalCount = totalCount,
        primaryMissionId = primaryMissionId,
        primaryTitle = primaryTitle,
        primaryCurrentText = primaryCurrentText,
        primaryRemainingText = primaryRemainingText,
        primaryStatus = primaryStatus,
        urgent = primaryStatus == MissionStatus.AT_RISK,
        primaryProgressFraction = primaryProgressFraction,
    )

    private fun pulseSnapshot(
        scoreDelta: Int? = null,
        previousScore: Int? = null,
        total: Int = 72,
        reasons: List<PulseScoreReason> = emptyList(),
    ) = DigitalPulseSnapshot(
        score = DigitalPulseScore(
            total = total,
            baseScore = total,
            taskContribution = 0,
            organization = 70,
            attention = 70,
            balance = 70,
            cleanup = 70,
            consistency = 70,
            confidence = DataConfidence.MEDIUM,
            reasons = reasons,
        ),
        computedAt = fixedNow,
        validUntil = fixedNow + 1,
        previousScore = previousScore,
        scoreDelta = scoreDelta,
    )

    // ---- Görev üreticileri ----

    @Test
    fun `null summary produces no items`() {
        val result = MissionPulseTickerFactory.missionCandidates(null, fixedNow)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `AT_RISK mission produces a MISSION_PROGRESS item`() {
        val result = MissionPulseTickerFactory.missionCandidates(
            summary(primaryStatus = MissionStatus.AT_RISK),
            fixedNow,
        )

        assertEquals(1, result.size)
        val item = result.single()
        assertEquals(SmartTickerType.MISSION_PROGRESS, item.type)
        assertEquals(TickerAction.OpenMissions, item.action)
        assertTrue(item.title.contains("Limite yaklaştın"))
    }

    @Test
    fun `IN_PROGRESS mission without single-action-away fraction produces no item`() {
        val result = MissionPulseTickerFactory.missionCandidates(
            summary(primaryStatus = MissionStatus.IN_PROGRESS, primaryProgressFraction = 0.4f),
            fixedNow,
        )

        assertTrue("Rutin ilerleme (1/3, %40) şeride girmemeli — kart zaten gösteriyor", result.isEmpty())
    }

    @Test
    fun `NOT_STARTED or SAFE mission produces no item`() {
        val notStarted = MissionPulseTickerFactory.missionCandidates(
            summary(primaryStatus = MissionStatus.NOT_STARTED),
            fixedNow,
        )
        val safe = MissionPulseTickerFactory.missionCandidates(
            summary(primaryStatus = MissionStatus.SAFE),
            fixedNow,
        )

        assertTrue(notStarted.isEmpty())
        assertTrue(safe.isEmpty())
    }

    @Test
    fun `single-action-away mission (progressFraction over threshold) produces a MISSION_PROGRESS item`() {
        val result = MissionPulseTickerFactory.missionCandidates(
            summary(primaryStatus = MissionStatus.IN_PROGRESS, primaryProgressFraction = 0.99f),
            fixedNow,
        )

        assertEquals(1, result.size)
        assertEquals(SmartTickerType.MISSION_PROGRESS, result.single().type)
        assertEquals("mission_single_action_m1", result.single().suggestionKey)
    }

    @Test
    fun `just-below-threshold progressFraction does not produce single-action item`() {
        val result = MissionPulseTickerFactory.missionCandidates(
            summary(primaryStatus = MissionStatus.IN_PROGRESS, primaryProgressFraction = 0.98f),
            fixedNow,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `COMPLETED primary mission produces a single MISSION_ACHIEVEMENT item, once per period via suggestionKey`() {
        val result = MissionPulseTickerFactory.missionCandidates(
            summary(primaryStatus = MissionStatus.COMPLETED, completedCount = 1, totalCount = 3),
            fixedNow,
        )

        assertEquals(1, result.size)
        val item = result.single()
        assertEquals(SmartTickerType.MISSION_ACHIEVEMENT, item.type)
        assertEquals("mission_completed_m1", item.suggestionKey)
        assertEquals(item.suggestionKey, item.dedupeKey)
    }

    @Test
    fun `all missions completed produces a single celebratory achievement item with a stable suggestionKey`() {
        val result = MissionPulseTickerFactory.missionCandidates(
            summary(completedCount = 3, totalCount = 3, primaryStatus = MissionStatus.COMPLETED),
            fixedNow,
        )

        assertEquals(1, result.size)
        val item = result.single()
        assertEquals(SmartTickerType.MISSION_ACHIEVEMENT, item.type)
        assertEquals("mission_all_completed", item.suggestionKey)
        assertTrue(item.title.contains("3"))
    }

    @Test
    fun `zero total missions (no data) produces no item`() {
        val result = MissionPulseTickerFactory.missionCandidates(
            summary(completedCount = 0, totalCount = 0, primaryMissionId = null, primaryStatus = null),
            fixedNow,
        )

        assertTrue(result.isEmpty())
    }

    // ---- Skor (Dijital Yaşam) üreticileri ----

    @Test
    fun `null snapshot produces no items`() {
        val result = MissionPulseTickerFactory.pulseCandidates(null, fixedNow)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `delta below threshold produces no PULSE_CHANGE item`() {
        val result = MissionPulseTickerFactory.pulseCandidates(
            pulseSnapshot(scoreDelta = 4),
            fixedNow,
        )

        assertTrue(
            "Eşik altı değişim (|4| < 5) ham skor gibi şeride girmemeli",
            result.none { it.suggestionKey == "pulse_score_delta" },
        )
    }

    @Test
    fun `delta at or above threshold produces a PULSE_CHANGE item without raw score`() {
        val result = MissionPulseTickerFactory.pulseCandidates(
            pulseSnapshot(scoreDelta = 6),
            fixedNow,
        )

        val item = result.single { it.suggestionKey == "pulse_score_delta" }
        assertEquals(SmartTickerType.PULSE_CHANGE, item.type)
        assertEquals(TickerAction.OpenWeeklyReport, item.action)
        assertTrue(item.title.contains("+6"))
        assertTrue("Ham skor (ör. '72') şeride asla girmemeli", !item.title.contains("72"))
    }

    @Test
    fun `negative delta at threshold produces a PULSE_CHANGE item with negative wording`() {
        val result = MissionPulseTickerFactory.pulseCandidates(
            pulseSnapshot(scoreDelta = -5),
            fixedNow,
        )

        val item = result.single { it.suggestionKey == "pulse_score_delta" }
        assertTrue(item.title.contains("-5"))
        assertTrue(item.title.contains("düştü"))
    }

    @Test
    fun `resolvable negative reason with an action produces a PULSE_CHANGE issue item`() {
        val result = MissionPulseTickerFactory.pulseCandidates(
            pulseSnapshot(
                scoreDelta = 0,
                reasons = listOf(PulseScoreReason(id = PulseReasonId.ATTENTION_NOISY, delta = -8)),
            ),
            fixedNow,
        )

        val item = result.single { it.suggestionKey == "pulse_resolvable_ATTENTION_NOISY" }
        assertEquals(SmartTickerType.PULSE_CHANGE, item.type)
        assertEquals(TickerAction.OpenNotificationReport, item.action)
    }

    @Test
    fun `negative reason without an actionable PulseAction produces no issue item`() {
        // ATTENTION_NO_PERMISSION -> PulseAction.None (bkz. PulseReasonPresenter) — çözülebilir değil.
        val result = MissionPulseTickerFactory.pulseCandidates(
            pulseSnapshot(
                scoreDelta = 0,
                reasons = listOf(PulseScoreReason(id = PulseReasonId.ATTENTION_NO_PERMISSION, delta = 0)),
            ),
            fixedNow,
        )

        assertTrue(result.none { it.suggestionKey?.startsWith("pulse_resolvable_") == true })
    }

    @Test
    fun `positive-only reasons produce no resolvable issue item`() {
        val result = MissionPulseTickerFactory.pulseCandidates(
            pulseSnapshot(
                scoreDelta = 0,
                reasons = listOf(PulseScoreReason(id = PulseReasonId.ORGANIZATION_HIGH, delta = 5)),
            ),
            fixedNow,
        )

        assertTrue(result.none { it.suggestionKey?.startsWith("pulse_resolvable_") == true })
    }

    @Test
    fun `raw score value never appears verbatim in produced titles`() {
        val result = MissionPulseTickerFactory.pulseCandidates(
            pulseSnapshot(
                scoreDelta = 10,
                total = 72,
                reasons = listOf(PulseScoreReason(id = PulseReasonId.CLEANUP_UNUSED, delta = -6, value = 4)),
            ),
            fixedNow,
        )

        result.forEach { item ->
            assertTrue("'${item.title}' ham skor 72 icermemeli", !item.title.contains("72"))
            assertTrue("'${item.title}' 'Skor' kelimesini icermemeli", !item.title.contains("Skor"))
        }
    }

    // ---- Ranker entegrasyonu ile toplam üst sınır ----

    @Test
    fun `mission and pulse candidates combined with composer output still respect TickerRanker MAX_VISIBLE`() {
        val missionItems = MissionPulseTickerFactory.missionCandidates(
            summary(primaryStatus = MissionStatus.AT_RISK),
            fixedNow,
        )
        val pulseItems = MissionPulseTickerFactory.pulseCandidates(
            pulseSnapshot(
                scoreDelta = 8,
                reasons = listOf(PulseScoreReason(id = PulseReasonId.ATTENTION_NOISY, delta = -8)),
            ),
            fixedNow,
        )
        val otherItems = listOf(
            SmartTickerItem(
                id = "tip_of_day",
                type = SmartTickerType.FEATURE_DISCOVERY,
                title = "ipucu",
                icon = "💡",
                priority = 5,
                createdAt = fixedNow,
            ),
            SmartTickerItem(
                id = "weekly_summary",
                type = SmartTickerType.WEEKLY_REPORT,
                title = "haftalik ozet",
                icon = "📊",
                priority = 60,
                createdAt = fixedNow,
            ),
        )

        val ranked = TickerRanker.rank(
            candidates = missionItems + pulseItems + otherItems,
            now = fixedNow,
        )

        assertTrue(ranked.size <= TickerRanker.MAX_VISIBLE)
        assertEquals(3, ranked.size)
    }
}
