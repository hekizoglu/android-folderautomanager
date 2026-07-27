package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.local.WeeklyGoalDao
import com.armutlu.apporganizer.domain.advice.CategoryGoalForAdvice
import com.armutlu.apporganizer.domain.advice.DigitalAdvice
import com.armutlu.apporganizer.domain.advice.computeDigitalAdvice
import com.armutlu.apporganizer.domain.models.WeeklyGoal
import com.armutlu.apporganizer.domain.usecase.missions.MissionUsageStatsSource
import com.armutlu.apporganizer.domain.models.WeeklyGoalMode
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.domain.usecase.goals.CategoryUsageSnapshotProvider
import com.armutlu.apporganizer.domain.usecase.goals.EnsureCurrentWeekAdaptiveGoalsUseCase
import com.armutlu.apporganizer.domain.usecase.goals.SettlePreviousWeekAdaptiveGoalsUseCase
import com.armutlu.apporganizer.utils.AppPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * P5 — Dashboard'a özel business logic. `AppListViewModel` büyütülmeyecek diye (CLAUDE.md §7)
 * `weeklyGoals`/`setWeeklyGoal`/`deleteWeeklyGoal` buraya taşındı — sadece Dashboard kullanıyordu
 * (grep doğrulandı). Ayrıca P1'in `CategoryUsageSnapshotProvider`'ını burada IO thread'de
 * çağırır — eski `AppOrganizerDashboardScreen`'in Compose içi senkron `UsageStatsHelper` çağrısı
 * (roadmap S7/S9 bulgusu) burada ORTADAN KALKAR.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val weeklyGoalDao: WeeklyGoalDao,
    private val categoryUsageSnapshotProvider: CategoryUsageSnapshotProvider,
    private val ensureCurrentWeekAdaptiveGoalsUseCase: EnsureCurrentWeekAdaptiveGoalsUseCase,
    private val settlePreviousWeekAdaptiveGoalsUseCase: SettlePreviousWeekAdaptiveGoalsUseCase,
    private val periodBoundaryResolver: PeriodBoundaryResolver,
    private val appDao: AppDao,
    private val clock: Clock,
    private val usageStatsSource: MissionUsageStatsSource,
    private val notificationEventDao: NotificationEventDao,
) : ViewModel() {

    data class DashboardGoalsUiState(
        // P8 — domain/advice.CategoryGoalForAdvice doğrudan kullanılır (computeDigitalAdvice
        // ile paylaşılan tek veri şekli, kod tekrarı yok).
        val goals: List<CategoryGoalForAdvice> = emptyList(),
        val isLearningMode: Boolean = false,
        val isLoading: Boolean = true,
    )

    private val currentWeekStart = periodBoundaryResolver.currentIsoWeek().weekStartEpochDay ?: 0L

    private val _uiState = MutableStateFlow(DashboardGoalsUiState())
    val uiState: StateFlow<DashboardGoalsUiState> = _uiState.asStateFlow()

    val weeklyGoals: StateFlow<List<WeeklyGoal>> =
        weeklyGoalDao.observeGoals(currentWeekStart)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // P7b — DigitalAdviceEngine çıktısı, TodayCard'ın ADVICE türü için (HomeScreen üzerinden
    // TodayCardSelector.select()'a geçirilir).
    private val _advice = MutableStateFlow<DigitalAdvice?>(null)
    val advice: StateFlow<DigitalAdvice?> = _advice.asStateFlow()

    init {
        refresh()
    }

    /**
     * P4 yaşam döngüsünü tetikler (idempotent — Dashboard açılışı bu 4 tetikleyici noktadan
     * biri, roadmap §6) ve ardından kategori snapshot'ını okuyup UI state'i doldurur.
     * Tüm ağır iş [Dispatchers.IO] altında — Compose composition thread'i bloklanmaz
     * (roadmap S7/S9 fix).
     */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching {
                if (AppPrefs.isGoalsEnabled(context)) {
                    settlePreviousWeekAdaptiveGoalsUseCase.execute()
                    ensureCurrentWeekAdaptiveGoalsUseCase.execute()
                }
            }.onFailure { e -> Timber.w(e, "Adaptive goals lifecycle failed on Dashboard refresh") }

            runCatching {
                val snapshot = categoryUsageSnapshotProvider.capture()
                val goals = weeklyGoalDao.getGoalsForWeek(currentWeekStart)
                val goalsUi = goals.map { goal ->
                    CategoryGoalForAdvice(
                        categoryId = goal.categoryId,
                        goal = goal,
                        previousWeekMinutes = snapshot.previousWeekMinutes(goal.categoryId),
                        currentWeekMinutesSoFar = snapshot.currentWeekMinutes(goal.categoryId),
                    )
                }
                _uiState.value = DashboardGoalsUiState(
                    goals = goalsUi,
                    isLearningMode = goals.isEmpty() && snapshot.validDataDayCount < 5,
                    isLoading = false,
                )
                _advice.value = computeDigitalAdvice(
                    snapshot, goalsUi, appDao, clock,
                    context = context,
                    usageStatsSource = usageStatsSource,
                    notificationEventDao = notificationEventDao,
                )
            }.onFailure { e ->
                Timber.w(e, "Category usage snapshot failed on Dashboard refresh")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setWeeklyGoal(categoryId: String, targetMinutes: Int) {
        if (categoryId.isBlank()) return
        viewModelScope.launch {
            weeklyGoalDao.upsert(
                WeeklyGoal(
                    categoryId = categoryId,
                    targetMinutes = targetMinutes.coerceIn(1, 7 * 24 * 60),
                    weekStartEpochDay = currentWeekStart,
                    mode = WeeklyGoalMode.MANUAL,
                ),
            )
        }
    }

    fun deleteWeeklyGoal(categoryId: String) {
        viewModelScope.launch {
            weeklyGoalDao.delete(categoryId, currentWeekStart)
        }
    }

    /** Kullanıcı bir AUTO hedefi manuel moda alır — bir daha otomatik hesaplama üzerine yazmaz. */
    fun switchToManual(categoryId: String) {
        viewModelScope.launch {
            weeklyGoalDao.setMode(categoryId, currentWeekStart, WeeklyGoalMode.MANUAL.name)
            refresh()
        }
    }

    /** Kullanıcı bir kategoriyi tekrar otomatik hesaplamaya açar — bir sonraki hafta başında yeniden üretilir. */
    fun switchToAuto(categoryId: String) {
        viewModelScope.launch {
            weeklyGoalDao.setMode(categoryId, currentWeekStart, WeeklyGoalMode.AUTO.name)
            refresh()
        }
    }

    fun excludeCategoryFromAdaptive(categoryId: String) {
        val current = AppPrefs.getAdaptiveGoalsExcludedCategories(context)
        AppPrefs.setAdaptiveGoalsExcludedCategories(context, current + categoryId)
    }

    fun includeCategoryInAdaptive(categoryId: String) {
        val current = AppPrefs.getAdaptiveGoalsIncludedCategories(context)
        AppPrefs.setAdaptiveGoalsIncludedCategories(context, current + categoryId)
        val excluded = AppPrefs.getAdaptiveGoalsExcludedCategories(context)
        AppPrefs.setAdaptiveGoalsExcludedCategories(context, excluded - categoryId)
    }
}
