package com.armutlu.apporganizer.domain.usecase.missions

import android.content.Context
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.data.repository.MissionsRepository
import com.armutlu.apporganizer.domain.models.MissionInstanceEntity
import com.armutlu.apporganizer.domain.time.PeriodBoundary
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.utils.MissionStreakPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Dongu M07 — [MissionsViewModel.computeAndAward]'daki gorev uretim/degerlendirme mantiginin
 * paylasilan (tek hesaplama yolu) hali. Ana ekran (HomeMissionCard -> RealMissionRuntimeSource)
 * ve Gorevler ekrani (MissionsViewModel) AYNI bu use-case'i cagirir — iki ayri kopya
 * hesaplama olmasin diye.
 *
 * [awardStars]=false ile cagrildiginda YALNIZCA okuma yapar: mission_history'ye yildiz
 * YAZMAZ, settleOverdue/pinInstances/instance senkronu gibi yan etkili DB yazimlarini
 * ATLAR. Ana ekran karti salt-goruntuleme oldugu icin arka planda sessizce tetiklenen
 * refresh() cagrilari kullaniciya gorunmeyen odul/yan-etki YARATMAMALI — bu yuzden
 * awardStars=true SADECE MissionsViewModel (kullanicinin ekrani actigi an) tarafindan
 * kullanilir.
 */
@Singleton
class MissionSummaryUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val missionsRepository: MissionsRepository,
    private val missionMetricSnapshotProvider: MissionMetricSnapshotProvider,
    private val settleMissionInstancesUseCase: SettleMissionInstancesUseCase,
) {

    /** [MissionsViewModel.MissionUi] ile alan alan ayni — domain katmaninda, Android View bagimliligi yok (String'ler zaten cozulmus). */
    data class MissionOutcome(
        val id: String,
        val title: String,
        val starReward: Int,
        val status: MissionStatus,
        val autoCheckable: Boolean,
        val currentText: String?,
        val remainingText: String?,
        val progressText: String?,
        val progressFraction: Float?,
        val deadlineText: String?,
        val action: MissionAction,
        val actionLabel: String?,
        // Dongu G5 — bu compute() cagrisinda COMPLETED'a YENI gecti mi (kutlama animasyonu
        // tetikleyicisi). awardStars=false cagrilarinda (Ana ekran karti, sessiz refresh) da
        // dolu gelir ama MissionsViewModel SADECE awardStars=true ile calisir — pratikte bu
        // alan yalniz kullanicinin Gorevler ekranini actigi an anlamlidir.
        val justCompleted: Boolean = false,
        // Dongu G3b — SADECE DAILY_APP_LIMIT icin dolu gelir; uzun basis "App Info" hedefini
        // kurmak icin UI katmanina (MissionsScreen) gecirilir. Telemetri/analytics event'lerine
        // ASLA yazilmaz (U02) — sadece Intent data URI'sinde ve baslikta kullanilir.
        val longPressTargetPackageName: String? = null,
    )

    data class Result(
        val totalStars: Int,
        val daily: List<MissionOutcome>,
        val weekly: List<MissionOutcome>,
        val newlyAwardedStars: Int,
        // Dongu G4 — okuma amacli, MissionStreakPrefs.read() ile doldurulur (compute() DB yazimi
        // yapmaz, sadece SettleMissionInstancesUseCase.settleOverdue TARAFINDAN ilerletilmis
        // durumu YANSITIR). awardStars=false cagrilarinda da guvenle okunabilir.
        val streak: MissionStreakPrefs.StreakState,
    )

    // Aninda (eylem sayisi/bayrak) tamamlanabilir gorevler — SADECE bunlar awardStars=true iken
    // yildiz kazandirir. Donemsel (ust sinir/haftalik karsilastirma/gece) gorevler settlement'a
    // kadar (M04) odul YAZMAZ. MissionsViewModel'deki orijinal set ile birebir aynidir.
    private val instantlyCompletableMissionIds = setOf(
        MissionEngine.DAILY_CLASSIFICATION_CLEANUP,
        MissionEngine.DAILY_VIEW_NOTIF_REPORT,
        MissionEngine.WEEKLY_POSITIVE_ACTIONS,
        // Dongu G3a — yeni eylem/bayrak gorevleri, ayni "aninda tamamlanabilir" sozlesmesine
        // girer (donemsel ust sinir/haftalik karsilastirma DEGIL — settlement beklemezler).
        // DAILY_MORNING_CALM istisnadir: AVOID_BEFORE_TIME kacinma gorevi olsa da, penceresi
        // (ilk 30dk) zaten kapandiktan SONRA COMPLETED doner (bkz. evaluateAvoidBeforeTime) —
        // bu yuzden erken odul riski YOK, aninda yildiz yazilabilir.
        MissionEngine.DAILY_ORGANIZE_UNCATEGORIZED,
        MissionEngine.DAILY_CUSTOMIZE_FOLDER,
        MissionEngine.DAILY_MORNING_CALM,
        MissionEngine.DAILY_FOCUS_SESSION,
        MissionEngine.DISCOVER_WEEKLY,
    )

    private val periodBoundaryResolver = PeriodBoundaryResolver(Clock.systemDefaultZone(), ZoneId.systemDefault())

    /**
     * @param awardStars true ise mevcut MissionsViewModel davranisi (yildiz yazimi + instance
     * senkronu + settleOverdue catch-up) birebir korunur. false ise sadece guncel durumu okur,
     * hicbir DB yazimi yapmaz (Ana ekran karti icin).
     */
    suspend fun compute(awardStars: Boolean): Result {
        missionsRepository.syncLegacyPrefsIfNeeded()
        if (awardStars) {
            runCatching { settleMissionInstancesUseCase.settleOverdue(System.currentTimeMillis()) }
                .onFailure { e -> Timber.w(e, "Catch-up settlement basarisiz") }
        }
        val epochDay = LocalDate.now().toEpochDay()
        val epochWeek = epochDay / 7
        val snapshot = missionMetricSnapshotProvider.capture()

        val now = LocalTime.now()
        val dayBoundary = periodBoundaryResolver.currentDay()
        val weekBoundary = periodBoundaryResolver.currentIsoWeek()
        val dayEnded = false
        val weekEnded = false

        // Dongu G1 — kisisel hedef ONCE bu donem icin daha once pin edilmis mi diye kontrol
        // edilir (varsa o SABIT deger kullanilir, tanisma/tempo degisikligi donem ortasinda
        // hedefi degistiremez). Pin edilmemisse tempo tercihine gore YENI hesaplanir; awardStars
        // ise bu deger asagida pinInstances ile sabitlenir.
        val existingDailyInstances = missionsRepository.getInstancesForPeriod(
            MissionInstanceEntity.PERIOD_DAILY,
            dayBoundary.epochDay,
        ).associateBy { it.missionId }

        val tempo = com.armutlu.apporganizer.utils.AppPrefs.getMissionTempo(context).coefficient
        val personalScreenTarget = existingDailyInstances[MissionEngine.DAILY_SCREEN_UNDER_3H]?.targetValue
            ?: PersonalTargetCalculator.calculateScreenTimeTarget(snapshot.screenTimeMinutesLast7CompletedDays, tempo)
        val personalUnlockTarget = existingDailyInstances[MissionEngine.DAILY_UNLOCK_UNDER_30]?.targetValue
            ?: PersonalTargetCalculator.calculateUnlockTarget(snapshot.unlockCountLast7CompletedDays, tempo)

        // Dongu G3b — uygulama-spesifik gorev. ONCE bugun icin daha once pin edilmis PAKET var
        // mi bakilir (AppPrefs — donem boyunca SABIT, gun ortasinda kullanim degisip farkli bir
        // uygulama one cikarsa hedef KAYMAZ). Yoksa AppLimitCandidateSelector ile YENI aday
        // secilir ve HEMEN pin edilir (sonraki cagrilarda ayni paket kullanilsin diye) — aday
        // yoksa (esik altinda/kategori disi) pin edilecek bir sey YOKTUR, appLimitTargetMinutes
        // null kalir ve MissionEngine.isEligible() gorevi havuza almaz.
        var pinnedAppLimitPackage = com.armutlu.apporganizer.utils.AppPrefs.getAppLimitTargetPackage(context, epochDay)
        var appLimitUsageMinutesToday = snapshot.appLimitUsageMinutesToday
        val appLimitTargetMinutes: Long? = if (pinnedAppLimitPackage != null) {
            existingDailyInstances[MissionEngine.DAILY_APP_LIMIT]?.targetValue
                ?: AppLimitCandidateSelector.calculateTarget(
                    snapshot.appLimitCandidates.firstOrNull { it.packageName == pinnedAppLimitPackage }
                        ?.dailyMinutesLast7Days ?: emptyList(),
                    tempo,
                )
        } else {
            val selected = AppLimitCandidateSelector.selectCandidate(snapshot.appLimitCandidates, tempo)
            if (selected != null) {
                // Dongu M07 sozlesmesi: awardStars=false (Ana ekran karti sessiz refresh) HICBIR
                // DB/prefs YAN ETKISI YARATMAZ — bu yuzden pin YAZIMI SADECE awardStars=true iken
                // yapilir (MissionsViewModel, kullanicinin ekrani actigi an). false cagrida aday
                // yine de GORUNTULENIR (gecici, o cagriya ozel) ama SONRAKI cagrida FARKLI bir
                // aday secilebilir — bu, DB'ye hicbir sey yazilmadigi surece kabul edilebilir
                // (Ana ekran karti zaten "en guncel durumu" gosterir, sabitlik garantisi vermez).
                if (awardStars) {
                    com.armutlu.apporganizer.utils.AppPrefs.setAppLimitTargetPackage(context, epochDay, selected.packageName)
                }
                pinnedAppLimitPackage = selected.packageName
                // Yeni pin: bugunku kullanim provider'da HENUZ bu paket icin okunmadi (o an
                // pinlenmemisti). 0 dakika ile baslamak guvenlidir — gercek 0 (kullanim yok) ile
                // veri-yok ayrimini bozmaz, cunku appLimitCandidates zaten izin VARKEN doldu
                // (izin yoksa selectCandidate zaten null doner, bu dala hic girilmez).
                appLimitUsageMinutesToday = 0L
                selected.targetMinutes
            } else {
                null
            }
        }

        val input = snapshot.toMissionCheckInput().copy(
            personalScreenTargetMinutes = personalScreenTarget,
            personalUnlockTarget = personalUnlockTarget,
            appLimitUsageMinutesToday = appLimitUsageMinutesToday,
            appLimitTargetMinutes = appLimitTargetMinutes,
        )
        val appLimitTargetPackageName = pinnedAppLimitPackage
        val dailyCooldownIds = missionsRepository.getRecentlyCompletedDailyIds(
            currentEpochDay = epochDay,
            cooldownDays = MissionEngine.dailyCooldownDays(),
        )
        val weeklyCooldownIds = missionsRepository.getRecentlyCompletedWeeklyIds(
            currentEpochWeek = epochWeek,
            cooldownWeeks = MissionEngine.weeklyCooldownWeeks(),
        )

        val dailyTargetValues = buildMap {
            put(MissionEngine.DAILY_SCREEN_UNDER_3H, personalScreenTarget ?: MissionEngine.DEFAULT_SCREEN_TARGET_MINUTES)
            put(MissionEngine.DAILY_UNLOCK_UNDER_30, personalUnlockTarget ?: MissionEngine.DEFAULT_UNLOCK_TARGET)
            // Dongu G3b — aday varsa hedefi de instance'a pinle (donem boyunca sabit kalsin).
            if (appLimitTargetMinutes != null) put(MissionEngine.DAILY_APP_LIMIT, appLimitTargetMinutes)
        }
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
        if (awardStars) {
            missionsRepository.pinInstances(
                missions = dailyMissions,
                periodType = MissionInstanceEntity.PERIOD_DAILY,
                boundary = dayBoundary,
                targetValues = dailyTargetValues,
            )
        }
        val daily = dailyMissions.map { mission ->
            val already = mission.id in dailyDone
            val evaluation = MissionEngine.evaluate(mission, input, now, dayEnded, weekEnded)
            val status = if (already) MissionStatus.COMPLETED else evaluation.status
            val justCompleted = status == MissionStatus.COMPLETED && !already
            if (awardStars && justCompleted && mission.id in instantlyCompletableMissionIds) {
                missionsRepository.markDailyCompleted(epochDay, mission.id)
                dailyDone += mission.id
                newStars += mission.starReward
                val instanceId = MissionInstanceEntity.buildInstanceId(
                    mission.id,
                    MissionInstanceEntity.PERIOD_DAILY,
                    dayBoundary.epochDay,
                )
                runCatching { settleMissionInstancesUseCase.completeActionMission(instanceId) }
                    .onFailure { e -> Timber.w(e, "Instance senkronu basarisiz: $instanceId") }
            }
            mission.toOutcome(status, evaluation, dayBoundary, justCompleted = justCompleted, appLimitTargetPackageName = appLimitTargetPackageName)
        }

        val weeklyDone = missionsRepository.getCompletedWeeklyIds(epochWeek).toMutableSet()
        val weeklyMissions = MissionEngine.generateWeekly(
            epochWeek = epochWeek,
            selection = MissionEngine.MissionSelectionInput(
                checkInput = input,
                recentlyCompletedMissionIds = weeklyCooldownIds,
            )
        )
        if (awardStars) {
            missionsRepository.pinInstances(
                missions = weeklyMissions,
                periodType = MissionInstanceEntity.PERIOD_WEEKLY,
                boundary = weekBoundary,
                targetValues = weeklyTargetValues,
                baselineValues = weeklyBaselineValues,
            )
        }
        val weekly = weeklyMissions.map { mission ->
            val already = mission.id in weeklyDone
            val evaluation = MissionEngine.evaluate(mission, input, now, dayEnded, weekEnded)
            val status = if (already) MissionStatus.COMPLETED else evaluation.status
            val justCompleted = status == MissionStatus.COMPLETED && !already
            if (awardStars && justCompleted && mission.id in instantlyCompletableMissionIds) {
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
            mission.toOutcome(status, evaluation, weekBoundary, justCompleted = justCompleted)
        }

        return Result(
            totalStars = missionsRepository.getTotalStars(),
            daily = daily,
            weekly = weekly,
            newlyAwardedStars = newStars,
            streak = MissionStreakPrefs.read(context),
        )
    }

    private fun MissionEngine.Mission.toOutcome(
        status: MissionStatus,
        evaluation: MissionEvaluation,
        periodBoundary: PeriodBoundary,
        justCompleted: Boolean = false,
        // Dongu G3b — SADECE DAILY_APP_LIMIT baslik/eylem uretimi icin kullanilir. Bu deger
        // MissionOutcome/telemetriye ASLA yazilmaz (U02) — sadece context.getString(...) ile
        // BASLIK METNINE gomulur ve actionFor()'da Intent data URI'sine gecer.
        appLimitTargetPackageName: String? = null,
    ): MissionOutcome {
        val progress = MissionProgressCalculator.calculate(
            evaluation,
            MissionEngine.progressKindForMission(id),
            countBased = id == MissionEngine.DAILY_UNLOCK_UNDER_30,
        )
        val action = if (status == MissionStatus.DATA_UNAVAILABLE) {
            MissionAction.OpenSettingsUsageAccess
        } else {
            actionFor(id, appLimitTargetPackageName)
        }
        return MissionOutcome(
            id = id,
            title = titleFor(id, evaluation.targetValue, appLimitTargetPackageName),
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
            justCompleted = justCompleted,
            longPressTargetPackageName = if (id == MissionEngine.DAILY_APP_LIMIT) appLimitTargetPackageName else null,
        )
    }

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

    private fun actionFor(id: String, appLimitTargetPackageName: String? = null): MissionAction = when (id) {
        MissionEngine.DAILY_CLASSIFICATION_CLEANUP -> MissionAction.OpenClassificationReview
        MissionEngine.DAILY_VIEW_NOTIF_REPORT -> MissionAction.OpenNotificationReport
        MissionEngine.DAILY_SCREEN_UNDER_3H,
        MissionEngine.DAILY_UNLOCK_UNDER_30,
        MissionEngine.WEEKLY_SCREEN_LESS -> MissionAction.OpenUsageReport
        MissionEngine.DAILY_NO_LATE_NIGHT -> MissionAction.OpenDoNotDisturbSettings
        MissionEngine.WEEKLY_POSITIVE_ACTIONS -> MissionAction.None
        // Dongu G3a — DAILY_ORGANIZE_UNCATEGORIZED ayni sinyali (siniflandirma aksiyonu)
        // kullandigindan DAILY_CLASSIFICATION_CLEANUP ile ayni hedef ekrana yonlendirilir.
        MissionEngine.DAILY_ORGANIZE_UNCATEGORIZED -> MissionAction.OpenClassificationReview
        MissionEngine.DAILY_CUSTOMIZE_FOLDER -> MissionAction.None
        MissionEngine.DAILY_MORNING_CALM -> MissionAction.None
        MissionEngine.DAILY_FOCUS_SESSION -> MissionAction.None
        MissionEngine.DISCOVER_WEEKLY -> MissionAction.None
        // Dongu G3b (plan G2 tablosu satiri: "gorev satiri -> o uygulamanin kullanim detayi").
        // Bu ekranda ayrica paket-ozel kullanim detayi YOKTUR, en yakin karsilik genel kullanim
        // raporudur. Uzun basisla App Info (paket URI'li SystemIntent) MissionsScreen'de AYRI
        // bir gesture olarak kurulur (bkz. MissionActionRouter.resolveLongPress).
        MissionEngine.DAILY_APP_LIMIT -> if (appLimitTargetPackageName != null) {
            MissionAction.OpenUsageReport
        } else {
            MissionAction.None
        }
        else -> MissionAction.None
    }

    private fun actionLabelRes(action: MissionAction): Int? = when (action) {
        MissionAction.OpenClassificationReview -> R.string.mission_action_review
        MissionAction.OpenNotificationReport -> R.string.mission_action_open_report
        MissionAction.OpenUsageReport -> R.string.mission_action_open_report
        MissionAction.OpenSettingsUsageAccess -> R.string.mission_action_grant_access
        MissionAction.OpenDoNotDisturbSettings -> R.string.mission_action_open_dnd_settings
        is MissionAction.OpenAppInfo -> null // uzun-basisla tetiklenir, gorunur eylem butonu YOK
        MissionAction.None -> null
    }

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
        // Dongu G3a
        MissionEngine.DAILY_ORGANIZE_UNCATEGORIZED -> R.string.mission_daily_organize_uncategorized
        MissionEngine.DAILY_CUSTOMIZE_FOLDER -> R.string.mission_daily_customize_folder
        MissionEngine.DAILY_MORNING_CALM -> R.string.mission_daily_morning_calm
        MissionEngine.DAILY_FOCUS_SESSION -> R.string.mission_daily_focus_session
        MissionEngine.DISCOVER_WEEKLY -> R.string.mission_discover_weekly
        else -> R.string.mission_unknown
    }

    /**
     * Dongu G1 — ust sinir gorevlerinde hedef sabit varsayilandan farkliysa (kisisel hedef
     * atanmis) kisisellestirilmis basligi, aksi halde eski sabit basligi doner. [target]
     * evaluate() sonucundaki targetValue'dur (pinInstances ile ayni deger, donem boyunca sabit).
     */
    private fun titleFor(id: String, target: Long?, appLimitTargetPackageName: String? = null): String = when (id) {
        MissionEngine.DAILY_SCREEN_UNDER_3H -> if (target != null && target != MissionEngine.DEFAULT_SCREEN_TARGET_MINUTES) {
            context.getString(
                R.string.mission_daily_screen_under_personal,
                resolveTextSpec(MissionValueFormatter.durationSpec(target)),
            )
        } else {
            context.getString(R.string.mission_daily_screen_under_3h)
        }
        MissionEngine.DAILY_UNLOCK_UNDER_30 -> if (target != null && target != MissionEngine.DEFAULT_UNLOCK_TARGET) {
            context.getString(R.string.mission_daily_unlock_under_personal, target.toInt())
        } else {
            context.getString(R.string.mission_daily_unlock_under_30)
        }
        // Dongu G3b — "Bugün [Uygulama]'yı [hedef] altında tut". Uygulama etiketi PackageManager'dan
        // COZULUR (Room'daki appName degil - guncel/kullanicinin gordugu ad garanti edilir).
        // Etiket cozulemezse (uygulama kaldirildi vb.) genel bir baslika DUSER — asla bos/crash
        // OLMAZ. Paket adinin KENDISI bu metnin DISINA (telemetriye) hicbir zaman gecmez.
        MissionEngine.DAILY_APP_LIMIT -> {
            val label = appLimitTargetPackageName?.let { resolveAppLabel(it) }
            if (label != null && target != null) {
                context.getString(
                    R.string.mission_daily_app_limit,
                    label,
                    resolveTextSpec(MissionValueFormatter.durationSpec(target)),
                )
            } else {
                context.getString(R.string.mission_daily_app_limit_fallback)
            }
        }
        else -> context.getString(titleRes(id))
    }

    /** Dongu G3b — kurulu uygulamanin gorunen adi. Kaldirilmissa/erisilemezse null (crash yerine fallback baslik). */
    private fun resolveAppLabel(packageName: String): String? = runCatching {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    }.getOrNull()
}
