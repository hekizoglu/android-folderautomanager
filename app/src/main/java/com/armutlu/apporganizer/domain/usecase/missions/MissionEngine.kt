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
        // Dongu G1 (kisisel gorev hedefi) — pinInstances sirasinda kararlastirilan, donem
        // boyunca SABIT kisisel hedef. null ise (tanisma modu veya henuz pin edilmemis) engine
        // sabit varsayilana (DEFAULT_SCREEN_TARGET_MINUTES/DEFAULT_UNLOCK_TARGET) duser.
        val personalScreenTargetMinutes: Long? = null,
        val personalUnlockTarget: Long? = null,
        // Dongu G3a — sabahin ilk yarim saatinde (ilk kullanimdan itibaren) sosyal kategori
        // acildi mi? null = veri yok (izin yok veya henuz ilk kullanim gerceklesmedi).
        val socialAppOpenedInFirst30MinToday: Boolean? = null,
        // Dongu G3a — Focus Mode'da bugun biriken dakika (basit prefs sayaci, AppPrefs).
        val focusModeMinutesToday: Long? = null,
        // Dongu G3b — uygulama-spesifik gorev (DAILY_APP_LIMIT). Hedef uygulamanin PAKET ADI
        // burada TASINMAZ (U02 - telemetriye asla gitmez); sadece bugunku kullanim dakikasi ve
        // (varsa) o gun icin secilmis hedef dakika gecirilir. Ikisi de null ise (aday yok/veri
        // yok) gorev isEligible() tarafindan havuza alinmaz.
        val appLimitUsageMinutesToday: Long? = null,
        val appLimitTargetMinutes: Long? = null,
    )

    data class TaskEventInput(
        val positiveEventsToday: Int = 0,
        val positiveEventsThisWeek: Int = 0,
        val classificationActionsToday: Int = 0,
        val notificationReportViewedToday: Boolean = false,
        // Dongu G3a — yeni eylem sayaclari (TaskScore event tablosundan, izin bagimsiz).
        val folderCustomizedToday: Boolean = false,
        val wrappedReportViewedThisWeek: Boolean = false,
    )

    /** Dongu G3a — ağırlıklı seçim girdisi: kullanıcının zayıf alanı (bkz. WeakAreaCategory). */
    enum class WeakAreaCategory { ORGANIZATION, ATTENTION, BALANCE, NONE }

    data class MissionSelectionInput(
        val checkInput: MissionCheckInput = MissionCheckInput(),
        val recentlyCompletedMissionIds: Set<String> = emptySet(),
        // Dongu G3a — kullanicinin en negatif Dijital Nabiz alt-skoru (PulseScoreReason'dan
        // turetilir, caller sorumlulugunda). NONE/bilinmiyor ise agirliksiz (eski davranis).
        val weakArea: WeakAreaCategory = WeakAreaCategory.NONE,
    )

    // Gorev id sabitleri - strings.xml eslesmeleri bu id'lere baglidir, degistirme.
    const val DAILY_SCREEN_UNDER_3H = "daily_screen_under_3h"
    const val DAILY_NO_LATE_NIGHT = "daily_no_late_night"
    const val DAILY_UNLOCK_UNDER_30 = "daily_unlock_under_30"
    const val DAILY_CLASSIFICATION_CLEANUP = "daily_classification_cleanup"
    const val DAILY_VIEW_NOTIF_REPORT = "daily_view_notif_report"
    const val WEEKLY_SCREEN_LESS = "weekly_screen_less"
    const val WEEKLY_POSITIVE_ACTIONS = "weekly_positive_actions"

    // Dongu G3a — yeni cekirdek gorevler (uygulama-spesifik DEGIL, G3b'ye kadar isimsiz).
    const val DAILY_ORGANIZE_UNCATEGORIZED = "daily_organize_uncategorized"
    const val DAILY_CUSTOMIZE_FOLDER = "daily_customize_folder"
    const val DAILY_MORNING_CALM = "daily_morning_calm"
    const val DAILY_FOCUS_SESSION = "daily_focus_session"
    const val DISCOVER_WEEKLY = "discover_weekly"

    // Dongu G3b — uygulama-spesifik gorev. TEK sabit id (dinamik degil); hedef paket adi
    // MissionCheckInput'a caller tarafindan (AppPrefs gunluk anahtari uzerinden) enjekte edilir,
    // DB semasi DEGISMEZ. Bkz. AppLimitCandidateSelector (aday secimi + hedef hesabi).
    const val DAILY_APP_LIMIT = "daily_app_limit"

    const val DAILY_STAR = 1
    const val WEEKLY_STAR = 2
    const val DAILY_MISSION_COUNT = 3

    // Dongu G1 — kisisel hedef yoksa (tanisma modu / henuz pin edilmemis) kullanilan sabit
    // varsayilanlar. Eski davranisla birebir ayni (roadmap oncesi sabit degerler).
    const val DEFAULT_SCREEN_TARGET_MINUTES = 180L
    const val DEFAULT_UNLOCK_TARGET = 30L

    // Dongu G3a — yeni gorev hedefleri (sabit, kisisellestirme kapsami disi).
    const val UNCATEGORIZED_ORGANIZE_TARGET = 2L
    const val FOCUS_SESSION_TARGET_MINUTES = 30L

    // Dongu G3a — DISCOVER_WEEKLY %10 nadir agirlik: shuffledWeeklyPool sirasindan BAGIMSIZ,
    // generateWeekly icinde ayrica uygulanir (bkz. asagidaki not).
    private const val DISCOVERY_WEIGHT_PERCENT = 10

    private val DAILY_POOL = listOf(
        Mission(DAILY_SCREEN_UNDER_3H, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        Mission(DAILY_NO_LATE_NIGHT, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        Mission(DAILY_UNLOCK_UNDER_30, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        Mission(DAILY_CLASSIFICATION_CLEANUP, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        Mission(DAILY_VIEW_NOTIF_REPORT, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        Mission(DAILY_ORGANIZE_UNCATEGORIZED, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        Mission(DAILY_CUSTOMIZE_FOLDER, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        Mission(DAILY_MORNING_CALM, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        Mission(DAILY_FOCUS_SESSION, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
        // Dongu G3b — havuz 12 -> 13. Aday yoksa isEligible() bunu ele alir (havuza girmez).
        Mission(DAILY_APP_LIMIT, MissionType.DAILY, DAILY_STAR, autoCheckable = true),
    )

    // Gorev id -> "kacinma" (pasif, AVOID_* kind) mi "eylem" (aninda tamamlanabilir) mi.
    // generateDaily kuralinda kullanilir: her gun en az 1 kacinma + 1 eylem gorevi.
    private val AVOIDANCE_MISSION_IDS = setOf(DAILY_NO_LATE_NIGHT, DAILY_MORNING_CALM)
    private val ACTION_MISSION_IDS = setOf(
        DAILY_CLASSIFICATION_CLEANUP,
        DAILY_VIEW_NOTIF_REPORT,
        DAILY_ORGANIZE_UNCATEGORIZED,
        DAILY_CUSTOMIZE_FOLDER,
        DAILY_FOCUS_SESSION,
    )

    // Gorev id -> hangi WeakAreaCategory'nin agirlikli secimini artirdigi (Dongu G3a).
    private val WEAK_AREA_BY_MISSION_ID = mapOf(
        DAILY_ORGANIZE_UNCATEGORIZED to WeakAreaCategory.ORGANIZATION,
        DAILY_CLASSIFICATION_CLEANUP to WeakAreaCategory.ORGANIZATION,
        DAILY_CUSTOMIZE_FOLDER to WeakAreaCategory.ORGANIZATION,
        DAILY_VIEW_NOTIF_REPORT to WeakAreaCategory.ATTENTION,
        DAILY_NO_LATE_NIGHT to WeakAreaCategory.ATTENTION,
        DAILY_MORNING_CALM to WeakAreaCategory.ATTENTION,
        DAILY_SCREEN_UNDER_3H to WeakAreaCategory.BALANCE,
        DAILY_UNLOCK_UNDER_30 to WeakAreaCategory.BALANCE,
        DAILY_FOCUS_SESSION to WeakAreaCategory.BALANCE,
        // Dongu G3b — plan G3 satiri: "BALANCE zayif alaninda 2x" agirlik.
        DAILY_APP_LIMIT to WeakAreaCategory.BALANCE,
    )
    private const val WEAK_AREA_WEIGHT = 2

    private val WEEKLY_POOL = listOf(
        Mission(WEEKLY_SCREEN_LESS, MissionType.WEEKLY, WEEKLY_STAR, autoCheckable = true),
        Mission(WEEKLY_POSITIVE_ACTIONS, MissionType.WEEKLY, WEEKLY_STAR, autoCheckable = true),
        Mission(DISCOVER_WEEKLY, MissionType.WEEKLY, WEEKLY_STAR, autoCheckable = true),
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
        val pool = when {
            withoutCooldown.size >= DAILY_MISSION_COUNT -> withoutCooldown
            eligible.size >= DAILY_MISSION_COUNT -> eligible
            withoutCooldown.isNotEmpty() -> withoutCooldown
            eligible.isNotEmpty() -> eligible
            else -> shuffled.filterNot { it.id in selection.recentlyCompletedMissionIds }
                .ifEmpty { shuffled }
        }
        // Dongu G3a — agirlikli secim: zayif alan gorevleri 2x sansla one cikar. Seed AYNI
        // epochDay + AYNI weakArea icin HER ZAMAN ayni sonucu uretir (Random(epochDay) taze
        // baslatilir, shuffledDailyPool'daki seed'den BAGIMSIZ ikinci bir deterministik akis).
        val weighted = weightedSample(pool, selection.weakArea, Random(epochDay), DAILY_MISSION_COUNT)
        val picked = if (weighted.size >= DAILY_MISSION_COUNT) {
            ensureAvoidanceAndActionMix(weighted, pool)
        } else {
            weighted
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

    /**
     * Gorev id -> ilerleme goruntuleme turu (Dongu M03). [MissionProgressCalculator] bu turu
     * kullanarak [MissionEvaluation]'i UI-hazir [MissionProgress]'e cevirir.
     */
    fun progressKindForMission(missionId: String): MissionProgressKind = when (missionId) {
        DAILY_SCREEN_UNDER_3H -> MissionProgressKind.UPPER_LIMIT
        DAILY_UNLOCK_UNDER_30 -> MissionProgressKind.UPPER_LIMIT
        DAILY_CLASSIFICATION_CLEANUP -> MissionProgressKind.ACTION_COUNT
        DAILY_VIEW_NOTIF_REPORT -> MissionProgressKind.BOOLEAN_ACTION
        DAILY_NO_LATE_NIGHT -> MissionProgressKind.AVOID_AFTER_TIME
        WEEKLY_SCREEN_LESS -> MissionProgressKind.PERIOD_COMPARISON
        WEEKLY_POSITIVE_ACTIONS -> MissionProgressKind.ACTION_COUNT
        // Dongu G3a — DAILY_FOCUS_SESSION suresi bir esik gorevidir (>= 30dk mi), "kac" degil;
        // BOOLEAN_ACTION (0/1 bayrak) ile ayni ACTION_COUNT gorseli yeterli/dogru — evaluate()
        // 0L/1L bayrak uretir, gercek dakika sayisi UI'da gosterilmez (esik gecildi mi onemli).
        DAILY_ORGANIZE_UNCATEGORIZED -> MissionProgressKind.ACTION_COUNT
        DAILY_CUSTOMIZE_FOLDER -> MissionProgressKind.BOOLEAN_ACTION
        DAILY_MORNING_CALM -> MissionProgressKind.AVOID_BEFORE_TIME
        DAILY_FOCUS_SESSION -> MissionProgressKind.BOOLEAN_ACTION
        DISCOVER_WEEKLY -> MissionProgressKind.BOOLEAN_ACTION
        // Dongu G3b — DAILY_SCREEN_UNDER_3H/DAILY_UNLOCK_UNDER_30 ile ayni gorsel dil (ust sinir).
        DAILY_APP_LIMIT -> MissionProgressKind.UPPER_LIMIT
        else -> MissionProgressKind.ACTION_COUNT
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
            target = input.personalScreenTargetMinutes ?: DEFAULT_SCREEN_TARGET_MINUTES,
            periodEnded = dayEnded,
        )
        DAILY_NO_LATE_NIGHT -> evaluateNoLateNight(input.usedAfter23Today, now, dayEnded)
        DAILY_UNLOCK_UNDER_30 -> evaluateUpperLimit(
            current = input.unlockCountToday?.toLong(),
            target = input.personalUnlockTarget ?: DEFAULT_UNLOCK_TARGET,
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
        // Dongu G3a — yeni cekirdek gorevler.
        DAILY_ORGANIZE_UNCATEGORIZED -> evaluateActionCount(
            current = input.taskEvents.classificationActionsToday.toLong(),
            target = UNCATEGORIZED_ORGANIZE_TARGET,
        )
        DAILY_CUSTOMIZE_FOLDER -> evaluateActionFlag(input.taskEvents.folderCustomizedToday)
        DAILY_MORNING_CALM -> evaluateAvoidBeforeTime(input.socialAppOpenedInFirst30MinToday)
        DAILY_FOCUS_SESSION -> evaluateActionFlag(
            (input.focusModeMinutesToday ?: 0L) >= FOCUS_SESSION_TARGET_MINUTES
        )
        DISCOVER_WEEKLY -> evaluateActionFlag(input.taskEvents.wrappedReportViewedThisWeek)
        // Dongu G3b — uygulama-spesifik ust sinir. Hedef/kullanim ikisi de veri gerektirir
        // (isEligible zaten aday yoksa havuza almaz, ama evaluate savunmaci kalir).
        DAILY_APP_LIMIT -> evaluateAppLimit(input.appLimitUsageMinutesToday, input.appLimitTargetMinutes, dayEnded)
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
     * Dongu G3b — DAILY_APP_LIMIT icin [evaluateUpperLimit] sarmalayicisi (block-body fonksiyon
     * kullanir cunku target null oldugunda erken DATA_UNAVAILABLE donmesi gerekir - evaluate()
     * expression-body oldugundan icinde dogrudan `return` yapilamaz, bu yuzden ayri fonksiyon).
     */
    private fun evaluateAppLimit(current: Long?, target: Long?, dayEnded: Boolean): MissionEvaluation {
        if (target == null) {
            return MissionEvaluation(
                status = MissionStatus.DATA_UNAVAILABLE,
                currentValue = current,
                targetValue = null,
                remainingValue = null,
            )
        }
        return evaluateUpperLimit(current = current, target = target, periodEnded = dayEnded)
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
     * Dongu G3a — sabah pozitifi: gunun ilk 30 dakikasinda sosyal kategoride uygulama acildi mi
     * (AVOID_AFTER_TIME'in sabah simetrigi). Veri saglayici ([MissionMetricSnapshotProvider])
     * "ilk 30 dakika" penceresini zaten kapali/acik olarak hesaplar (henuz ilk kullanim
     * gerceklesmediyse pencere acik sayilmaz — DATA_UNAVAILABLE yerine caller basitce null
     * gecirir, bu da burada DATA_UNAVAILABLE'a duser; gun ilerledikce gercek veri gelir).
     * Gece gorevinden farkli olarak zaman esigi caller'da (snapshot provider) cozulur -
     * MissionEngine burada saf bir bayrak degerlendirir, LocalTime almaz.
     */
    private fun evaluateAvoidBeforeTime(socialOpenedInFirst30Min: Boolean?): MissionEvaluation {
        if (socialOpenedInFirst30Min == null) {
            return MissionEvaluation(MissionStatus.DATA_UNAVAILABLE, null, null, null)
        }
        return if (socialOpenedInFirst30Min) {
            MissionEvaluation(
                status = MissionStatus.FAILED,
                currentValue = 1L,
                targetValue = 0L,
                remainingValue = 0L,
                failureReasonCode = "MORNING_SOCIAL_USAGE_DETECTED",
            )
        } else {
            MissionEvaluation(MissionStatus.COMPLETED, 0L, 0L, 0L)
        }
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
        // Dongu G3a
        DAILY_ORGANIZE_UNCATEGORIZED -> true
        DAILY_CUSTOMIZE_FOLDER -> true
        DAILY_MORNING_CALM -> input.socialAppOpenedInFirst30MinToday != null
        DAILY_FOCUS_SESSION -> true
        DISCOVER_WEEKLY -> true
        // Dongu G3b — hedef atanmamissa (aday yoksa AppLimitCandidateSelector null doner ->
        // caller appLimitTargetMinutes'i null birakir) gorev havuza HIC girmez ("aday yoksa
        // gorev havuza girmez" plan kurali).
        DAILY_APP_LIMIT -> input.appLimitTargetMinutes != null
        else -> false
    }

    /**
     * Dongu G3a — kullanicinin en zayif alanina (weakArea) 2x agirlik veren deterministik
     * secim. [Random.nextInt] tabanli agirlikli ornekleme (agirlikli "havuzdan cekme" yerine
     * TEK bir Random akisi kullanilir ki epochDay seed'i sabit kaldikca sonuc HER ZAMAN ayni
     * olsun). Her adimda kalan adaylarin agirlik toplami hesaplanir, [Random.nextInt] o toplam
     * araliginda bir nokta secer, hangi adaya dustugu (agirlik siniri) bulunur ve o aday
     * listeden cikarilir — klasik "weighted sampling without replacement".
     */
    private fun weightedSample(
        candidates: List<Mission>,
        weakArea: WeakAreaCategory,
        rnd: Random,
        count: Int,
    ): List<Mission> {
        val remaining = candidates.toMutableList()
        val result = mutableListOf<Mission>()
        fun weightOf(mission: Mission): Int =
            if (weakArea != WeakAreaCategory.NONE && WEAK_AREA_BY_MISSION_ID[mission.id] == weakArea) {
                WEAK_AREA_WEIGHT
            } else {
                1
            }
        while (result.size < count && remaining.isNotEmpty()) {
            val totalWeight = remaining.sumOf { weightOf(it) }
            var pick = rnd.nextInt(totalWeight)
            var chosenIndex = 0
            for (i in remaining.indices) {
                pick -= weightOf(remaining[i])
                if (pick < 0) {
                    chosenIndex = i
                    break
                }
            }
            result += remaining.removeAt(chosenIndex)
        }
        return result
    }

    /**
     * Dongu G3a — secilen gunluk 3'lu icinde en az 1 "kacinma" (AVOIDANCE_MISSION_IDS) ve en az
     * 1 "eylem" (ACTION_MISSION_IDS) gorevi olmasini garanti eder. `eligible` havuzunda uygun
     * aday varsa ama secilen 3'luye girmediyse, en dusuk oncelikli (son sıradaki) uygun-olmayan
     * turdeki gorevin yerine takas edilir — boylece 3 sayisi SABIT kalir, sadece ic karisim
     * duzelir. Aday yoksa (havuzda o turden hic eligible gorev yoksa) degisiklik yapilmaz
     * (kullaniciya haksizlik/sahte gorev DAYATILMAZ).
     */
    private fun ensureAvoidanceAndActionMix(
        picked: List<Mission>,
        eligiblePool: List<Mission>,
    ): List<Mission> {
        if (picked.size < DAILY_MISSION_COUNT) return picked
        var result = picked

        fun swapInIfMissing(idSet: Set<String>) {
            if (result.any { it.id in idSet }) return
            val candidate = eligiblePool.firstOrNull { it.id in idSet && it !in result } ?: return
            // Diger turden GEREKSIZ olan (ayni turde birden fazla varsa) son elemanla degistir;
            // yoksa listenin son elemaniyla degistir.
            val otherSet = if (idSet === AVOIDANCE_MISSION_IDS) ACTION_MISSION_IDS else AVOIDANCE_MISSION_IDS
            val redundantIndex = result.indices.lastOrNull { idx ->
                val id = result[idx].id
                id !in idSet && (id !in otherSet || result.count { it.id in otherSet } > 1)
            } ?: (result.size - 1)
            result = result.toMutableList().apply { set(redundantIndex, candidate) }
        }

        swapInIfMissing(AVOIDANCE_MISSION_IDS)
        swapInIfMissing(ACTION_MISSION_IDS)
        return result
    }
}
