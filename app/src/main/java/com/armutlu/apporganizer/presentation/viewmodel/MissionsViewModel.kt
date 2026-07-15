package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.data.repository.MissionsRepository
import com.armutlu.apporganizer.domain.usecase.missions.MissionEngine
import com.armutlu.apporganizer.utils.TaskScoreManager
import com.armutlu.apporganizer.utils.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Gorevler ekrani durumu (D257) — MissionEngine gorevlerini uretir, ekran acilisinda
 * otomatik dogrular, tamamlananlara yildiz yazar (MissionPrefs). Tum hesap cihazda.
 */
@HiltViewModel
class MissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val missionsRepository: MissionsRepository,
) : ViewModel() {

    data class MissionUi(
        val id: String,
        val title: String,
        val starReward: Int,
        val completed: Boolean,
        val autoCheckable: Boolean,
    )

    data class MissionsUiState(
        val totalStars: Int = 0,
        val daily: List<MissionUi> = emptyList(),
        val weekly: List<MissionUi> = emptyList(),
        val taskScore: Int = 0,
        val taskScoreDelta: Int = 0,
        val taskScoreLastEvent: String = "",
        val celebrateStars: Int? = null, // yeni kazanilan yildiz — tebrik karti tetikler
        val loading: Boolean = true,
    )

    private val _uiState = MutableStateFlow(MissionsUiState())
    val uiState: StateFlow<MissionsUiState> = _uiState.asStateFlow()

    /** Ekran acilisinda cagrilir — otomatik gorevleri dogrular, yeni tamamlananlara yildiz yazar. */
    fun refresh() {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                runCatching { computeAndAward() }
                    .onFailure { e -> Timber.e(e, "Gorev durumu hesaplanamadi") }
                    .getOrNull()
            }
            if (state != null) _uiState.value = state
            else _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    fun dismissCelebration() {
        _uiState.value = _uiState.value.copy(celebrateStars = null)
    }

    private suspend fun computeAndAward(): MissionsUiState {
        missionsRepository.syncLegacyPrefsIfNeeded()
        val epochDay = LocalDate.now().toEpochDay()
        val epochWeek = epochDay / 7
        val input = buildCheckInput(epochDay, epochWeek)

        var newStars = 0
        val dailyDone = missionsRepository.getCompletedDailyIds(epochDay).toMutableSet()
        val daily = MissionEngine.generateDaily(epochDay).map { mission ->
            val already = mission.id in dailyDone
            val completed = already || MissionEngine.checkProgress(mission, input)
            if (completed && !already) {
                missionsRepository.markDailyCompleted(epochDay, mission.id)
                dailyDone += mission.id
                newStars += mission.starReward
            }
            mission.toUi(completed)
        }

        val weeklyDone = missionsRepository.getCompletedWeeklyIds(epochWeek).toMutableSet()
        val weekly = MissionEngine.generateWeekly(epochWeek).map { mission ->
            val already = mission.id in weeklyDone
            val completed = already || MissionEngine.checkProgress(mission, input)
            if (completed && !already) {
                missionsRepository.markWeeklyCompleted(epochWeek, mission.id)
                weeklyDone += mission.id
                newStars += mission.starReward
            }
            mission.toUi(completed)
        }

        val taskScore = TaskScoreManager.getSnapshotV2(context)
        return MissionsUiState(
            totalStars = missionsRepository.getTotalStars(),
            daily = daily,
            weekly = weekly,
            taskScore = taskScore.totalScore,
            taskScoreDelta = taskScore.lastDelta,
            taskScoreLastEvent = taskScore.lastEventLabel,
            celebrateStars = newStars.takeIf { it > 0 },
            loading = false,
        )
    }

    private suspend fun buildCheckInput(epochDay: Long, epochWeek: Long): MissionEngine.MissionCheckInput {
        val sessions = (UsageStatsHelper.getDailySessionUsage(context, days = 14)
            as? UsageStatsHelper.DailySessionResult.Available)?.days

        // Gun bazinda global ekran suresi (paketler ayni global degeri tasir — max al).
        val minutesByDay = sessions?.groupBy { it.epochDay }
            ?.mapValues { (_, list) ->
                (list.maxOfOrNull { it.globalForegroundMs } ?: 0L) / TimeUnit.MINUTES.toMillis(1)
            }

        val todayEntries = sessions?.filter { it.epochDay == epochDay }
        val usedAfter23 = todayEntries?.any { entry ->
            entry.hourlyForegroundMs.getOrNull(23)?.let { it > 0L } == true
        }

        fun weekMinutes(week: Long): Long? = minutesByDay
            ?.filterKeys { it / 7 == week }
            ?.values?.sum()

        return MissionEngine.MissionCheckInput(
            screenTimeMinutesToday = if (minutesByDay != null) minutesByDay[epochDay] ?: 0L else null,
            usedAfter23Today = usedAfter23,
            unlockCountToday = UsageStatsHelper.getUnlockCount(context, days = 1),
            weeklyScreenTimeMinutes = weekMinutes(epochWeek),
            previousWeeklyScreenTimeMinutes = weekMinutes(epochWeek - 1),
            taskEvents = missionsRepository.buildTaskEventInput(epochDay, epochWeek),
        )
    }

    private fun MissionEngine.Mission.toUi(completed: Boolean) = MissionUi(
        id = id,
        title = context.getString(titleRes(id)),
        starReward = starReward,
        completed = completed,
        autoCheckable = autoCheckable,
    )

    private fun titleRes(id: String): Int = when (id) {
        MissionEngine.DAILY_SCREEN_UNDER_3H -> R.string.mission_daily_screen_under_3h
        MissionEngine.DAILY_NO_LATE_NIGHT -> R.string.mission_daily_no_late_night
        MissionEngine.DAILY_UNLOCK_UNDER_30 -> R.string.mission_daily_unlock_under_30
        MissionEngine.DAILY_CLASSIFICATION_CLEANUP -> R.string.mission_daily_classification_cleanup
        MissionEngine.DAILY_VIEW_NOTIF_REPORT -> R.string.mission_daily_view_notif_report
        MissionEngine.WEEKLY_SCREEN_LESS -> R.string.mission_weekly_screen_less
        MissionEngine.WEEKLY_POSITIVE_ACTIONS -> R.string.mission_weekly_positive_actions
        else -> R.string.mission_unknown
    }
}
