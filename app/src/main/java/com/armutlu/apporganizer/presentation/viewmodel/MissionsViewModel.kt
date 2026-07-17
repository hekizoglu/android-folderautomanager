package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.data.repository.MissionsRepository
import com.armutlu.apporganizer.domain.models.MissionInstanceEntity
import com.armutlu.apporganizer.domain.time.PeriodBoundary
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.domain.usecase.missions.MissionAction
import com.armutlu.apporganizer.domain.usecase.missions.MissionEngine
import com.armutlu.apporganizer.domain.usecase.missions.MissionEvaluation
import com.armutlu.apporganizer.domain.usecase.missions.MissionMetricSnapshotProvider
import com.armutlu.apporganizer.domain.usecase.missions.MissionProgressCalculator
import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.domain.usecase.missions.MissionTextSpec
import com.armutlu.apporganizer.domain.usecase.missions.MissionValueFormatter
import com.armutlu.apporganizer.domain.usecase.missions.SettleMissionInstancesUseCase
import com.armutlu.apporganizer.domain.usecase.missions.toMissionCheckInput
import com.armutlu.apporganizer.utils.TaskScoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
    private val missionMetricSnapshotProvider: MissionMetricSnapshotProvider,
    private val settleMissionInstancesUseCase: SettleMissionInstancesUseCase,
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
    )

    private val _uiState = MutableStateFlow(MissionsUiState())
    val uiState: StateFlow<MissionsUiState> = _uiState.asStateFlow()

    // M00: donem sinirlarini (gun/hafta bitti mi) hesaplamak icin H01 altyapisi (sistem saat dilimi).
    private val periodBoundaryResolver = PeriodBoundaryResolver(Clock.systemDefaultZone(), ZoneId.systemDefault())

    // Aninda (eylem sayisi/bayrak) tamamlanabilir gorevler — SADECE bunlar computeAndAward'da
    // yildiz kazandirir. Donemsel (ust sinir / haftalik karsilastirma / gece) gorevler
    // settlement'a kadar (M04) odul YAZMAZ.
    private val instantlyCompletableMissionIds = setOf(
        MissionEngine.DAILY_CLASSIFICATION_CLEANUP,
        MissionEngine.DAILY_VIEW_NOTIF_REPORT,
        MissionEngine.WEEKLY_POSITIVE_ACTIONS,
    )

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
        // Dongu M04 — HOME_RESUME catch-up: WorkManager donem sinirinda (gece yarisi/hafta
        // baslangici) tam zamaninda calismamis olabilir (Doze, batarya optimizasyonu, telefon
        // kapali). Ekran her acildiginda gecikmis (periodEndAt gecmis ama hala "assigned")
        // instance'lar burada da sonuclandirilir — boylece yildiz, gorev ekrani hic acilmasa bile
        // WorkManager'dan, ekran acilirsa da buradan garanti yazilir (ayni instance iki kez
        // sonuclanmaz, bkz. SettleMissionInstancesUseCase.settleOverdue).
        runCatching { settleMissionInstancesUseCase.settleOverdue(System.currentTimeMillis()) }
            .onFailure { e -> Timber.w(e, "Catch-up settlement basarisiz") }
        val epochDay = LocalDate.now().toEpochDay()
        val epochWeek = epochDay / 7
        // M02: tum gorev metrikleri tek zaman-tutarli snapshot'tan gelir — ViewModel hesaplama yapmaz.
        val snapshot = missionMetricSnapshotProvider.capture()
        val input = snapshot.toMissionCheckInput()
        val dailyCooldownIds = missionsRepository.getRecentlyCompletedDailyIds(
            currentEpochDay = epochDay,
            cooldownDays = MissionEngine.dailyCooldownDays(),
        )
        val weeklyCooldownIds = missionsRepository.getRecentlyCompletedWeeklyIds(
            currentEpochWeek = epochWeek,
            cooldownWeeks = MissionEngine.weeklyCooldownWeeks(),
        )

        val now = LocalTime.now()
        // refresh() sadece ekran acikken (kullanici o an telefonu kullanirken) cagrilir, yani
        // gorulen an her zaman donemin (gun/hafta) SIRASINDADIR - donem bitmis olamaz. Gercek
        // "donem bitti" sinirlari (gece yarisi / Pazartesi 00:00) settlement is'idir (M04),
        // orada PeriodBoundaryResolver.nextLocalMidnight()/nextWeekBoundary() ile tetiklenecek
        // arka plan islemi kullanilacaktir. Referans birligini korumak icin burada tutulur.
        val dayBoundary = periodBoundaryResolver.currentDay()
        val weekBoundary = periodBoundaryResolver.currentIsoWeek()
        val dayEnded = false
        val weekEnded = false

        // M01: hedef/baseline degerleri MissionEngine.evaluate() ile ayni sabitler — sadece
        // kalici kayit icin, degerlendirme mantigini degistirmez.
        val dailyTargetValues = mapOf(
            MissionEngine.DAILY_SCREEN_UNDER_3H to 180L,
            MissionEngine.DAILY_UNLOCK_UNDER_30 to 30L,
        )
        val weeklyTargetValues = mapOf(
            MissionEngine.WEEKLY_POSITIVE_ACTIONS to 3L,
        )
        val weeklyBaselineValues = mapOf(
            MissionEngine.WEEKLY_SCREEN_LESS to input.previousWeeklyScreenTimeMinutes,
        )

        var newStars = 0
        val dailyDone = missionsRepository.getCompletedDailyIds(epochDay).toMutableSet()
        val dailyMissions = MissionEngine.generateDaily(
            epochDay = epochDay,
            selection = MissionEngine.MissionSelectionInput(
                checkInput = input,
                recentlyCompletedMissionIds = dailyCooldownIds,
            )
        )
        missionsRepository.pinInstances(
            missions = dailyMissions,
            periodType = MissionInstanceEntity.PERIOD_DAILY,
            boundary = dayBoundary,
            targetValues = dailyTargetValues,
        )
        val daily = dailyMissions.map { mission ->
            val already = mission.id in dailyDone
            val evaluation = MissionEngine.evaluate(mission, input, now, dayEnded, weekEnded)
            val status = if (already) MissionStatus.COMPLETED else evaluation.status
            val justCompleted = status == MissionStatus.COMPLETED && !already
            if (justCompleted && mission.id in instantlyCompletableMissionIds) {
                missionsRepository.markDailyCompleted(epochDay, mission.id)
                dailyDone += mission.id
                newStars += mission.starReward
                // M04: mission_instances kaydini da COMPLETED+settled isaretle — odul zaten
                // markDailyCompleted ile mission_history'ye yazildi, completeActionMission
                // ikinci bir odul YAZMAZ (sadece instance status senkronu).
                val instanceId = MissionInstanceEntity.buildInstanceId(
                    mission.id,
                    MissionInstanceEntity.PERIOD_DAILY,
                    dayBoundary.epochDay,
                )
                runCatching { settleMissionInstancesUseCase.completeActionMission(instanceId) }
                    .onFailure { e -> Timber.w(e, "Instance senkronu basarisiz: $instanceId") }
            }
            mission.toUi(status, evaluation, dayBoundary)
        }

        val weeklyDone = missionsRepository.getCompletedWeeklyIds(epochWeek).toMutableSet()
        val weeklyMissions = MissionEngine.generateWeekly(
            epochWeek = epochWeek,
            selection = MissionEngine.MissionSelectionInput(
                checkInput = input,
                recentlyCompletedMissionIds = weeklyCooldownIds,
            )
        )
        missionsRepository.pinInstances(
            missions = weeklyMissions,
            periodType = MissionInstanceEntity.PERIOD_WEEKLY,
            boundary = weekBoundary,
            targetValues = weeklyTargetValues,
            baselineValues = weeklyBaselineValues,
        )
        val weekly = weeklyMissions.map { mission ->
            val already = mission.id in weeklyDone
            val evaluation = MissionEngine.evaluate(mission, input, now, dayEnded, weekEnded)
            val status = if (already) MissionStatus.COMPLETED else evaluation.status
            val justCompleted = status == MissionStatus.COMPLETED && !already
            if (justCompleted && mission.id in instantlyCompletableMissionIds) {
                missionsRepository.markWeeklyCompleted(epochWeek, mission.id)
                weeklyDone += mission.id
                newStars += mission.starReward
                val weeklyPeriodStartEpoch = weekBoundary.weekStartEpochDay ?: weekBoundary.epochDay
                val instanceId = MissionInstanceEntity.buildInstanceId(
                    mission.id,
                    MissionInstanceEntity.PERIOD_WEEKLY,
                    weeklyPeriodStartEpoch,
                )
                runCatching { settleMissionInstancesUseCase.completeActionMission(instanceId) }
                    .onFailure { e -> Timber.w(e, "Instance senkronu basarisiz: $instanceId") }
            }
            mission.toUi(status, evaluation, weekBoundary)
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

    private fun MissionEngine.Mission.toUi(
        status: MissionStatus,
        evaluation: MissionEvaluation,
        periodBoundary: PeriodBoundary,
    ): MissionUi {
        // Dongu M03: MissionProgressCalculator saf Kotlin ciktisi -> burada context.getString ile
        // gosterime hazir stringe cozulur. Iki katmanli spec'ler (durationSpec ic ice gecebilir)
        // resolveTextSpec ile ozyinelemeli cozulur.
        val progress = MissionProgressCalculator.calculate(evaluation, MissionEngine.progressKindForMission(id))
        // Dongu M05: DATA_UNAVAILABLE durumunda gorevin normal eylemi (orn. rapor ekrani)
        // anlamsizdir — kullaniciyi once izin vermeye yonlendirmek gerekir.
        val action = if (status == MissionStatus.DATA_UNAVAILABLE) {
            MissionAction.OpenSettingsUsageAccess
        } else {
            actionFor(id)
        }
        return MissionUi(
            id = id,
            title = context.getString(titleRes(id)),
            starReward = starReward,
            status = status,
            autoCheckable = autoCheckable,
            currentText = progress.currentTextRes?.let { resolveTextSpec(it) },
            remainingText = progress.remainingTextRes?.let { resolveTextSpec(it) },
            progressText = progress.progressTextRes?.let { resolveTextSpec(it) },
            progressFraction = progress.progressFraction,
            deadlineText = deadlineTextFor(status, periodBoundary, type),
            action = action,
            actionLabel = actionLabelRes(action)?.let { context.getString(it) },
        )
    }

    /**
     * Dongu M06: "Gunun/haftanin bitmesine <sure>" — donem henuz surerken (SAFE/AT_RISK/
     * IN_PROGRESS/NOT_STARTED) gosterilir. Donem zaten kesinlesmisse (COMPLETED/FAILED/
     * AWAITING_SETTLEMENT) veya veri yoksa (DATA_UNAVAILABLE) deadline anlamsizdir -> null.
     */
    private fun deadlineTextFor(
        status: MissionStatus,
        periodBoundary: PeriodBoundary,
        missionType: MissionEngine.MissionType,
    ): String? {
        val showsDeadline = status == MissionStatus.SAFE ||
            status == MissionStatus.AT_RISK ||
            status == MissionStatus.IN_PROGRESS ||
            status == MissionStatus.NOT_STARTED
        if (!showsDeadline) return null
        val remainingMillis = periodBoundary.endExclusive - periodBoundaryResolver.nowMillis()
        if (remainingMillis <= 0) return null
        val remainingMinutes = remainingMillis / 60_000L
        val durationText = resolveTextSpec(MissionValueFormatter.durationSpec(remainingMinutes))
        val labelRes = if (missionType == MissionEngine.MissionType.WEEKLY) {
            R.string.mission_deadline_week
        } else {
            R.string.mission_deadline_day
        }
        return context.getString(labelRes, durationText)
    }

    /** Dongu M05: gorev id -> eylem eslemesi. Route/Intent cozumu MissionActionRouter'da yapilir. */
    private fun actionFor(id: String): MissionAction = when (id) {
        MissionEngine.DAILY_CLASSIFICATION_CLEANUP -> MissionAction.OpenClassificationReview
        MissionEngine.DAILY_VIEW_NOTIF_REPORT -> MissionAction.OpenNotificationReport
        MissionEngine.DAILY_SCREEN_UNDER_3H,
        MissionEngine.DAILY_UNLOCK_UNDER_30,
        MissionEngine.DAILY_NO_LATE_NIGHT,
        MissionEngine.WEEKLY_SCREEN_LESS -> MissionAction.OpenUsageReport
        MissionEngine.WEEKLY_POSITIVE_ACTIONS -> MissionAction.None
        else -> MissionAction.None
    }

    private fun actionLabelRes(action: MissionAction): Int? = when (action) {
        MissionAction.OpenClassificationReview -> R.string.mission_action_review
        MissionAction.OpenNotificationReport -> R.string.mission_action_open_report
        MissionAction.OpenUsageReport -> R.string.mission_action_open_report
        MissionAction.OpenSettingsUsageAccess -> R.string.mission_action_grant_access
        MissionAction.None -> null
    }

    /**
     * [MissionTextSpec] argumanlari baska bir [MissionTextSpec] tasiyabilir (orn. "Şu an: %1$s"
     * kalibinin argumani "1 sa. 30 dk." formatlayan ic durationSpec'tir) — bu yuzden cozumleme
     * ozyinelemelidir. Duz (Int/Long/String) argumanlar oldugu gibi `getString`'e verilir.
     */
    private fun resolveTextSpec(spec: MissionTextSpec): String {
        val resolvedArgs = spec.args.map { arg ->
            if (arg is MissionTextSpec) resolveTextSpec(arg) else arg
        }
        return context.getString(spec.resId, *resolvedArgs.toTypedArray())
    }

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
