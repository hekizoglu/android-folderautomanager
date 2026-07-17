package com.armutlu.apporganizer.domain.usecase.missions

import java.time.LocalTime
import java.util.Random

/**
 * Gorev motoru (D257) - saf Kotlin, Android bagimliligi yok, unit test edilebilir.
 * Gunluk 3 + haftalik 2 gorev uretir; secim deterministiktir (seed = epochDay/epochWeek),
 * yani ayni gun icinde ekran her acilista ayni gorevler gorunur.
 *
 * Basliklar UI katmaninda string resource ile cozulur (PulseInsightSpec pattern'i) -
 * engine yalnizca id tasir, kullaniciya gorunen metin icermez.
 */
object MissionEngine {
    private const val DAILY_MISSION_COOLDOWN_DAYS = 2L
    private const val WEEKLY_MISSION_COOLDOWN_WEEKS = 1L

    enum class MissionType { DAILY, WEEKLY }

    /**
     * @param autoCheckable false = motor dogrulayamaz; ancak P1.4 ile aktif havuz
     * sadece sistemin gercek sinyalle dogrulayabildigi gorevlerden olusur.
     */
    data class Mission(
        val id: String,
        val type: MissionType,
        val starReward: Int,
        val autoCheckable: Boolean,
    )

    /** Gorev dogrulama girdisi - alanlar mevcut motorlardan (UsageStatsHelper vb.) beslenir. */
    data class MissionCheckInput(
        val screenTimeMinutesToday: Long? = null,
        val usedAfter23Today: Boolean? = null,
        val unlockCountToday: Int? = null,
        val weeklyScreenTimeMinutes: Long? = null,
        val previousWeeklyScreenTimeMinutes: Long? = null,
        val taskEvents: TaskEventInput = TaskEventInput(),
    )

    data class TaskEventInput(
        val positiveEventsToday: Int = 0,
        val positiveEventsThisWeek: Int = 0,
        val classificationActionsToday: Int = 0,
        val notificationReportViewedToday: Boolean = false,
    )

    data class MissionSelectionInput(
        val checkInput: MissionCheckInput = MissionCheckInput(),
        val recentlyCompletedMissionIds: Set<String> = emptySet(),
    )

    // Gorev id sabitleri - strings.xml eslesmeleri bu id'lere baglidir, degistirme.
    const val DAILY_SCREEN_UNDER_3H = "daily_screen_under_3h"
    const val DAILY_NO_LATE_NIGHT = "daily_no_late_night"
    const val DAILY_UNLOCK_UNDER_30 = "daily_unlock_under_30"
    const val DAILY_CLASSIFICATION_CLEANUP = "daily_classification_cleanup"
    const val DAILY_VIEW_NOTIF_REPORT = "daily_view_notif_report"
    const val WEEKLY_SCREEN_LESS = "weekly_screen_less"
    const val WEEKLY_POSITIVE_ACTIONS = "weekly_positive_actions"

    const val DAILY_STAR = 1
    const val WEEKLY_STAR = 2
    const val DAILY_MISSION_COUNT = 3

    private val DAILY_POOL = listOf(
        Mission(DAILY_SCREEN_UNDER_3H, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        Mission(DAILY_NO_LATE_NIGHT, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        Mission(DAILY_UNLOCK_UNDER_30, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        Mission(DAILY_CLASSIFICATION_CLEANUP, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        Mission(DAILY_VIEW_NOTIF_REPORT, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
    )

    private val WEEKLY_POOL = listOf(
        Mission(WEEKLY_SCREEN_LESS, MissionType.WEEKLY, WEEKLY_STAR, autoCheckable = true),
        Mission(WEEKLY_POSITIVE_ACTIONS, MissionType.WEEKLY, WEEKLY_STAR, autoCheckable = true),
    )

    /** Gunun 3 gorevi - seed epochDay oldugundan ayni gun hep ayni set doner. */
    fun generateDaily(epochDay: Long): List<Mission> =
        generateDaily(epochDay, MissionSelectionInput())

    fun generateDaily(
        epochDay: Long,
        selection: MissionSelectionInput,
    ): List<Mission> {
        val shuffled = shuffledDailyPool(epochDay)
        val eligible = shuffled.filter { isEligible(it, selection.checkInput) }
        val withoutCooldown = eligible.filterNot { it.id in selection.recentlyCompletedMissionIds }
        val picked = when {
            withoutCooldown.size >= DAILY_MISSION_COUNT -> withoutCooldown
            eligible.size >= DAILY_MISSION_COUNT -> eligible
            withoutCooldown.isNotEmpty() -> withoutCooldown
            eligible.isNotEmpty() -> eligible
            else -> shuffled.filterNot { it.id in selection.recentlyCompletedMissionIds }
                .ifEmpty { shuffled }
        }
        return picked.take(DAILY_MISSION_COUNT).sortedBy { it.id }
    }

    /** Haftanin 2 gorevi - havuz 2 eleman oldugundan hepsi doner; cooldown uygular. */
    fun generateWeekly(epochWeek: Long): List<Mission> =
        generateWeekly(epochWeek, MissionSelectionInput())

    fun generateWeekly(
        epochWeek: Long,
        selection: MissionSelectionInput,
    ): List<Mission> {
        val shuffled = shuffledWeeklyPool(epochWeek)
        val eligible = shuffled.filter { isEligible(it, selection.checkInput) }
        val withoutCooldown = eligible.filterNot { it.id in selection.recentlyCompletedMissionIds }
        return when {
            withoutCooldown.isNotEmpty() -> withoutCooldown
            eligible.isNotEmpty() -> eligible
            else -> shuffled
        }
    }

    fun starRewardForMission(missionId: String): Int = when (missionId) {
        in DAILY_POOL.map { it.id } -> DAILY_STAR
        in WEEKLY_POOL.map { it.id } -> WEEKLY_STAR
        else -> 0
    }

    fun dailyCooldownDays(): Long = DAILY_MISSION_COOLDOWN_DAYS

    fun weeklyCooldownWeeks(): Long = WEEKLY_MISSION_COOLDOWN_WEEKS

    /**
     * KOPRU (Dongu M00): eski boolean sozlesmesi [evaluate]'in uzerine kurulur —
     * "basarili" sadece status == COMPLETED oldugunda true doner. Donemsel gorevlerde
     * artik erken (donem bitmeden) true DONMEZ; bu, P0 2.4'un cozumudur.
     * Cagiran taraflar (MissionsViewModel) kademeli olarak [evaluate]'e gecmelidir.
     */
    @Deprecated(
        "Zaman/donem farkindaligi olmayan boolean sozlesme yerine evaluate() kullanin.",
        ReplaceWith("evaluate(mission, input, now).status == MissionStatus.COMPLETED"),
    )
    fun checkProgress(mission: Mission, input: MissionCheckInput, now: LocalTime = LocalTime.now()): Boolean =
        evaluate(mission, input, now).status == MissionStatus.COMPLETED

    /**
     * Gorevi zaman/donem farkinda degerlendirir (Dongu M00). [dayEnded]/[weekEnded] cagiran
     * tarafca (PeriodBoundaryResolver ile) hesaplanip gecirilir — MissionEngine saf kalir,
     * Android/Clock bagimliligi almaz.
     *
     * Kurallar (roadmap M00):
     * - Eylem sayisi gorevleri hedefe ulasinca aninda COMPLETED.
     * - Ust sinir gorevleri (ekran suresi, kilit acma) donem bitmeden COMPLETED OLAMAZ:
     *   deger hedefin altindaysa IN_PROGRESS (>= %80 kullanimda AT_RISK), donem bitince
     *   settlement'ta kesinlesir (bu fonksiyon dayEnded=true oldugunda COMPLETED/FAILED doner).
     *   Ust sinir ASILIRSA donem bitmeden FAILED olabilir.
     * - Gece gorevi 23:00 oncesi NOT_STARTED; 23:00 sonrasi kullanim yoksa SAFE (odul yok,
     *   gun bitince COMPLETED), kullanim varsa FAILED.
     * - Haftalik karsilastirma hafta bitmeden COMPLETED olamaz -> IN_PROGRESS.
     * - Veri yoksa DATA_UNAVAILABLE.
     */
    fun evaluate(
        mission: Mission,
        input: MissionCheckInput,
        now: LocalTime = LocalTime.now(),
        dayEnded: Boolean = false,
        weekEnded: Boolean = false,
    ): MissionEvaluation = when (mission.id) {
        DAILY_SCREEN_UNDER_3H -> evaluateUpperLimit(
            current = input.screenTimeMinutesToday,
            target = 180L,
            periodEnded = dayEnded,
        )
        DAILY_NO_LATE_NIGHT -> evaluateNoLateNight(input.usedAfter23Today, now, dayEnded)
        DAILY_UNLOCK_UNDER_30 -> evaluateUpperLimit(
            current = input.unlockCountToday?.toLong(),
            target = 30L,
            periodEnded = dayEnded,
        )
        DAILY_CLASSIFICATION_CLEANUP -> evaluateActionCount(
            current = input.taskEvents.classificationActionsToday.toLong(),
            target = 1L,
        )
        DAILY_VIEW_NOTIF_REPORT -> evaluateActionFlag(input.taskEvents.notificationReportViewedToday)
        WEEKLY_SCREEN_LESS -> evaluateWeeklyComparison(
            current = input.weeklyScreenTimeMinutes,
            previous = input.previousWeeklyScreenTimeMinutes,
            weekEnded = weekEnded,
        )
        WEEKLY_POSITIVE_ACTIONS -> evaluateActionCount(
            current = input.taskEvents.positiveEventsThisWeek.toLong(),
            target = 3L,
        )
        else -> MissionEvaluation(
            status = MissionStatus.DATA_UNAVAILABLE,
            currentValue = null,
            targetValue = null,
            remainingValue = null,
        )
    }

    /** Eylem sayisi gorevleri: hedefe ulasinca aninda COMPLETED, ulasmadiysa IN_PROGRESS. */
    private fun evaluateActionCount(current: Long, target: Long): MissionEvaluation {
        val remaining = (target - current).coerceAtLeast(0L)
        return MissionEvaluation(
            status = if (current >= target) MissionStatus.COMPLETED else MissionStatus.IN_PROGRESS,
            currentValue = current,
            targetValue = target,
            remainingValue = remaining,
        )
    }

    /** Tek seferlik bayrak gorevleri (orn. rapor goruntulendi mi). */
    private fun evaluateActionFlag(done: Boolean): MissionEvaluation = MissionEvaluation(
        status = if (done) MissionStatus.COMPLETED else MissionStatus.IN_PROGRESS,
        currentValue = if (done) 1L else 0L,
        targetValue = 1L,
        remainingValue = if (done) 0L else 1L,
    )

    /**
     * Ust sinir gorevleri: veri yoksa DATA_UNAVAILABLE; hedef asildiysa (donem bitmemis olsa
     * bile) FAILED; donem bittiyse ve hedef altindaysa COMPLETED, ustundeyse FAILED;
     * donem surerken hedefin altindaysa IN_PROGRESS (>= %80 kullanimda AT_RISK).
     */
    private fun evaluateUpperLimit(current: Long?, target: Long, periodEnded: Boolean): MissionEvaluation {
        if (current == null) {
            return MissionEvaluation(MissionStatus.DATA_UNAVAILABLE, null, target, null)
        }
        val remaining = (target - current).coerceAtLeast(0L)
        if (current >= target) {
            return MissionEvaluation(
                status = MissionStatus.FAILED,
                currentValue = current,
                targetValue = target,
                remainingValue = 0L,
                failureReasonCode = "UPPER_LIMIT_EXCEEDED",
            )
        }
        if (periodEnded) {
            return MissionEvaluation(MissionStatus.COMPLETED, current, target, remaining)
        }
        val ratio = current.toDouble() / target.toDouble()
        val status = if (ratio >= 0.8) MissionStatus.AT_RISK else MissionStatus.IN_PROGRESS
        return MissionEvaluation(status, current, target, remaining)
    }

    /**
     * Gece kullanmama gorevi: 23:00 oncesi NOT_STARTED (veri var/yok fark etmez — donem
     * henuz baslamadi). 23:00 sonrasi: veri yoksa DATA_UNAVAILABLE; kullanim varsa
     * (dogrudan FAILED — donem bitmeden bile kesinlesir, kural asildi); kullanim yoksa
     * SAFE (gun bitmediyse) veya COMPLETED (gun bittiyse).
     */
    private fun evaluateNoLateNight(usedAfter23: Boolean?, now: LocalTime, dayEnded: Boolean): MissionEvaluation {
        val nightStart = LocalTime.of(23, 0)
        if (now.isBefore(nightStart) && !dayEnded) {
            return MissionEvaluation(MissionStatus.NOT_STARTED, null, null, null)
        }
        if (usedAfter23 == null) {
            return MissionEvaluation(MissionStatus.DATA_UNAVAILABLE, null, null, null)
        }
        if (usedAfter23) {
            return MissionEvaluation(
                status = MissionStatus.FAILED,
                currentValue = 1L,
                targetValue = 0L,
                remainingValue = 0L,
                failureReasonCode = "LATE_NIGHT_USAGE_DETECTED",
            )
        }
        val status = if (dayEnded) MissionStatus.COMPLETED else MissionStatus.SAFE
        return MissionEvaluation(status, 0L, 0L, 0L)
    }

    /**
     * Haftalik karsilastirma: baseline yoksa DATA_UNAVAILABLE; hafta bitmediyse her zaman
     * IN_PROGRESS (erken odul yok — P0 2.4 fix); hafta bittiyse simdiki < onceki ise
     * COMPLETED, degilse FAILED.
     */
    private fun evaluateWeeklyComparison(current: Long?, previous: Long?, weekEnded: Boolean): MissionEvaluation {
        if (current == null || previous == null || previous <= 0L) {
            return MissionEvaluation(MissionStatus.DATA_UNAVAILABLE, current, previous, null)
        }
        if (!weekEnded) {
            return MissionEvaluation(MissionStatus.IN_PROGRESS, current, previous, (previous - current).coerceAtLeast(0L))
        }
        return if (current < previous) {
            MissionEvaluation(MissionStatus.COMPLETED, current, previous, 0L)
        } else {
            MissionEvaluation(
                status = MissionStatus.FAILED,
                currentValue = current,
                targetValue = previous,
                remainingValue = 0L,
                failureReasonCode = "WEEKLY_COMPARISON_NOT_IMPROVED",
            )
        }
    }

    private fun shuffledDailyPool(epochDay: Long): List<Mission> {
        val order = DAILY_POOL.indices.toMutableList()
        val rnd = Random(epochDay)
        for (i in order.size - 1 downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = order[i]
            order[i] = order[j]
            order[j] = tmp
        }
        return order.map { DAILY_POOL[it] }
    }

    private fun shuffledWeeklyPool(epochWeek: Long): List<Mission> {
        val order = WEEKLY_POOL.indices.toMutableList()
        val rnd = Random(epochWeek)
        for (i in order.size - 1 downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = order[i]
            order[i] = order[j]
            order[j] = tmp
        }
        return order.map { WEEKLY_POOL[it] }
    }

    private fun isEligible(mission: Mission, input: MissionCheckInput): Boolean = when (mission.id) {
        DAILY_SCREEN_UNDER_3H -> input.screenTimeMinutesToday != null
        DAILY_NO_LATE_NIGHT -> input.usedAfter23Today != null
        DAILY_UNLOCK_UNDER_30 -> input.unlockCountToday != null
        DAILY_CLASSIFICATION_CLEANUP -> true
        DAILY_VIEW_NOTIF_REPORT -> true
        WEEKLY_SCREEN_LESS ->
            input.weeklyScreenTimeMinutes != null &&
                input.previousWeeklyScreenTimeMinutes != null &&
                input.previousWeeklyScreenTimeMinutes > 0L
        WEEKLY_POSITIVE_ACTIONS -> true
        else -> false
    }
}
