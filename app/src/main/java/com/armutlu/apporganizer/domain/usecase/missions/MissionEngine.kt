package com.armutlu.apporganizer.domain.usecase.missions

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

    /** Otomatik gorevlerde ilgili veri null ise false (uydurma basari yok). */
    fun checkProgress(mission: Mission, input: MissionCheckInput): Boolean {
        return when (mission.id) {
            DAILY_SCREEN_UNDER_3H ->
                input.screenTimeMinutesToday != null && input.screenTimeMinutesToday < 180L
            DAILY_NO_LATE_NIGHT ->
                input.usedAfter23Today == false
            DAILY_UNLOCK_UNDER_30 ->
                input.unlockCountToday != null && input.unlockCountToday in 1 until 30
            DAILY_CLASSIFICATION_CLEANUP ->
                input.taskEvents.classificationActionsToday > 0
            DAILY_VIEW_NOTIF_REPORT ->
                input.taskEvents.notificationReportViewedToday
            WEEKLY_SCREEN_LESS ->
                input.weeklyScreenTimeMinutes != null &&
                    input.previousWeeklyScreenTimeMinutes != null &&
                    input.previousWeeklyScreenTimeMinutes > 0L &&
                    input.weeklyScreenTimeMinutes < input.previousWeeklyScreenTimeMinutes
            WEEKLY_POSITIVE_ACTIONS ->
                input.taskEvents.positiveEventsThisWeek >= 3
            else -> false
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
