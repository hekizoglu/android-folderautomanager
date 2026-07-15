package com.armutlu.apporganizer.domain.usecase.missions

import java.util.Random

/**
 * Gorev motoru (D257) — saf Kotlin, Android bagimliligi yok, unit test edilebilir.
 * Gunluk 3 + haftalik 2 gorev uretir; secim deterministiktir (seed = epochDay/epochWeek),
 * yani ayni gun icinde ekran her acilista AYNI gorevler gorunur.
 *
 * Basliklar UI katmaninda string resource ile cozulur (PulseInsightSpec pattern'i) —
 * engine yalnizca id tasir, kullaniciya gorunen metin icermez.
 */
object MissionEngine {

    enum class MissionType { DAILY, WEEKLY }

    /**
     * @param autoCheckable false = motor dogrulayamaz; UI "Tamamladim" butonu gosterir ve
     *        tamamlama manuallyCompletedIds uzerinden gelir.
     */
    data class Mission(
        val id: String,
        val type: MissionType,
        val starReward: Int,
        val autoCheckable: Boolean,
    )

    /** Gorev dogrulama girdisi — alanlar mevcut motorlardan (UsageStatsHelper vb.) beslenir. */
    data class MissionCheckInput(
        val screenTimeMinutesToday: Long? = null,      // null = kullanim izni yok
        val usedAfter23Today: Boolean? = null,         // null = saatlik veri yok
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

    // Gorev id sabitleri — MissionPrefs ve strings.xml eslesmeleri bu id'lere baglidir, DEGISTIRME.
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

    /** Gunun 3 gorevi — seed epochDay oldugundan ayni gun hep ayni set doner. */
    fun generateDaily(epochDay: Long): List<Mission> {
        val order = DAILY_POOL.indices.toMutableList()
        val rnd = Random(epochDay)
        // Fisher-Yates — java.util.Random deterministik, platformlar arasi sabit.
        for (i in order.size - 1 downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = order[i]; order[i] = order[j]; order[j] = tmp
        }
        return order.take(DAILY_MISSION_COUNT).map { DAILY_POOL[it] }.sortedBy { it.id }
    }

    /** Haftanin 2 gorevi — havuz 2 eleman oldugundan hepsi doner; seed ileride buyuyecek havuz icin. */
    fun generateWeekly(epochWeek: Long): List<Mission> = WEEKLY_POOL

    fun starRewardForMission(missionId: String): Int = when (missionId) {
        in DAILY_POOL.map { it.id } -> DAILY_STAR
        in WEEKLY_POOL.map { it.id } -> WEEKLY_STAR
        else -> 0
    }

    /**
     * Gorev tamamlandi mi? Manuel isaretlenenler (manuallyCompletedIds) her zaman true;
     * otomatik gorevlerde ilgili veri null ise false (uydurma basari yok).
     */
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
}
