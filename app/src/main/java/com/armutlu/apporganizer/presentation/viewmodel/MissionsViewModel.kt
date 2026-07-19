package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.domain.usecase.missions.MissionAction
import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.domain.usecase.missions.MissionSummaryUseCase
import com.armutlu.apporganizer.utils.TaskScoreManager
import java.time.LocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val missionSummaryUseCase: MissionSummaryUseCase,
) : ViewModel() {

    data class MissionUi(
        val id: String,
        val title: String,
        val starReward: Int,
        val status: MissionStatus,
        val autoCheckable: Boolean,
        // Dongu M03: MissionProgressCalculator ciktisindan cozulmus, gosterime hazir metinler.
        // Nullable — veri yoksa (DATA_UNAVAILABLE) veya gorev tipi metin uretmiyorsa (orn.
        // AVOID_AFTER_TIME) null kalir. MissionsScreen M06'ya kadar bu alanlari KULLANMAK
        // ZORUNDA DEGIL — ekran mevcut haliyle derlenip calismaya devam eder.
        val currentText: String? = null,
        val remainingText: String? = null,
        val progressText: String? = null,
        val progressFraction: Float? = null,
        // Dongu M06: donemin (gun/hafta) bitisine kalan sure — "Gunun bitmesine 6 sa. 20 dk."
        // PeriodBoundaryResolver'dan hesaplanir, MissionValueFormatter.durationSpec ile
        // formatlanir. Donem zaten bitmisse (AWAITING_SETTLEMENT/COMPLETED/FAILED) null olabilir.
        val deadlineText: String? = null,
        // Dongu M05: gorevi tamamlayacagi ekrana tek dokunusla goturen eylem. None ise
        // MissionsScreen eylem butonu gostermez. DATA_UNAVAILABLE durumunda MissionActionRouter
        // hedefi ne olursa olsun OpenSettingsUsageAccess'e dusurulur (asagida actionFor()).
        val action: MissionAction = MissionAction.None,
        val actionLabel: String? = null,
        // Dongu G5 — bu refresh() cagrisinda COMPLETED'a YENI gecti mi (bir onceki durum
        // COMPLETED degildi). MissionRow tek seferlik kutlama animasyonu/haptic icin kullanir;
        // ayni gorev ikinci recomposition'da tekrar true GELMEZ (viewModel yalniz bu compute
        // cagrisinin sonucunu tasir, kalici bir "gorulmedi" bayragi tutmaz — celeprasyon zaten
        // LaunchedEffect(mission.id, justCompleted) ile bir kez tetiklenir).
        val justCompleted: Boolean = false,
        // Dongu G3b — SADECE DAILY_APP_LIMIT icin dolu gelir (uzun basis "App Info" hedefi).
        // Telemetriye ASLA yazilmaz (U02) — MissionsScreen SADECE Intent kurmak icin okur.
        val longPressTargetPackageName: String? = null,
    ) {
        // M06'da status'e gore yeniden tasarlanana kadar UI kirilmasin diye korunur.
        val completed: Boolean get() = status == MissionStatus.COMPLETED
    }

    data class MissionsUiState(
        val totalStars: Int = 0,
        val daily: List<MissionUi> = emptyList(),
        val weekly: List<MissionUi> = emptyList(),
        val taskScore: Int = 0,
        val taskScoreDelta: Int = 0,
        val taskScoreLastEvent: String = "",
        val celebrateStars: Int? = null, // yeni kazanilan yildiz — tebrik karti tetikler
        val loading: Boolean = true,
        // Dongu G4 — Streak (Seri) Sistemi.
        val currentStreak: Int = 0,
        val bestStreak: Int = 0,
        val goldenStreak: Int = 0,
        val streakFrozenYesterday: Boolean = false,
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

    /**
     * Dongu M07: gercek hesaplama artik [MissionSummaryUseCase] icinde — Gorevler ekrani ve
     * Ana ekran karti (HomeMissionCard) AYNI hesaplama yolunu paylasir. Bu fonksiyon sadece
     * use-case sonucunu mevcut [MissionUi]/[MissionsUiState] sekline esler — davranis (yildiz
     * yazimi, instance senkronu, settleOverdue catch-up) awardStars=true ile birebir korunur.
     */
    private suspend fun computeAndAward(): MissionsUiState {
        val result = missionSummaryUseCase.compute(awardStars = true)
        val taskScore = TaskScoreManager.getSnapshotV2(context)
        val todayEpochDay = LocalDate.now().toEpochDay()
        return MissionsUiState(
            totalStars = result.totalStars,
            daily = result.daily.map { it.toMissionUi() },
            weekly = result.weekly.map { it.toMissionUi() },
            taskScore = taskScore.totalScore,
            taskScoreDelta = taskScore.lastDelta,
            taskScoreLastEvent = taskScore.lastEventLabel,
            celebrateStars = result.newlyAwardedStars.takeIf { it > 0 },
            loading = false,
            currentStreak = result.streak.currentStreak,
            bestStreak = result.streak.bestStreak,
            goldenStreak = result.streak.goldenStreak,
            streakFrozenYesterday = result.streak.wasFrozenYesterday(todayEpochDay),
        )
    }

    private fun MissionSummaryUseCase.MissionOutcome.toMissionUi(): MissionUi = MissionUi(
        id = id,
        title = title,
        starReward = starReward,
        status = status,
        autoCheckable = autoCheckable,
        currentText = currentText,
        remainingText = remainingText,
        progressText = progressText,
        progressFraction = progressFraction,
        deadlineText = deadlineText,
        action = action,
        actionLabel = actionLabel,
        justCompleted = justCompleted,
        longPressTargetPackageName = longPressTargetPackageName,
    )
}
