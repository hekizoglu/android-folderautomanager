package com.armutlu.apporganizer.domain.usecase.missions

import android.content.Context
import com.armutlu.apporganizer.data.local.MissionHistoryDao
import com.armutlu.apporganizer.data.local.MissionInstanceDao
import com.armutlu.apporganizer.domain.models.MissionHistoryEntry
import com.armutlu.apporganizer.domain.models.MissionInstanceEntity
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.utils.MissionStreakPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Odul yazimi + instance status guncellemesini tek atomik birim olarak calistiran soyutlama.
 * Uretimde [com.armutlu.apporganizer.data.local.AppDatabase.withTransaction]'a delege eder
 * (Room-KTX) — testlerde gercek bir Room ornegi kurmadan sade bir gecis (pass-through)
 * implementasyonuyla degistirilir (bkz. SettleMissionInstancesUseCaseTest).
 */
interface MissionSettlementTransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}

/**
 * Dongu M04 — Gorev sonuclandirma ve odul servisi
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satir 945-1001).
 *
 * Amac: yildizin YALNIZ dogru zamanda ve BIR KEZ verilmesi — gorev ekrani hic acilmasa bile.
 * `mission_instances` (M01) donem sinirlarini/hedeflerini sabit tutar; bu use case donemi biten
 * ama henuz sonuclanmamis ("assigned" + periodEndAt gecmis) instance'lari bulur, DONEM SONU
 * metrikleriyle degerlendirir, COMPLETED ise `mission_history`'ye yildiz yazar, her durumda
 * instance status+settledAt gunceller.
 *
 * Gecmis donem metrik stratejisi: [MissionMetricSnapshotProvider] "su an"i (bugun/bu hafta)
 * hesaplar, gecmis/kapanmis donemler icin uygun degildir. Bu yuzden [MissionUsageStatsSource]
 * dogrudan, donemin bitisine (`periodEndAt`) sabitlenmis `nowMillis` ile sorgulanir — boylece
 * UsageStats penceresi hala donemin gunlerini kapsiyorsa (sistem olay gunlugu birkac gun tutar)
 * dogru sonuc alinir. Kapsamiyorsa (pencere disi / eski donem) DATA_UNAVAILABLE dondurulur.
 *
 * DATA_UNAVAILABLE karari (roadmap onerisi benimsendi): donem bitisi 48 saatten ESKI ise
 * "tekrar denenmeyecek" kabul edilip settled+DATA_UNAVAILABLE (yildiz yok, FAILED de degil —
 * kullaniciya haksizlik yapilmaz, sadece odul verilmez) olarak isaretlenir. 48 saatten daha
 * TAZE ise settle EDILMEZ — bir sonraki catch-up cagrisinda (WorkManager veya HOME_RESUME)
 * UsageStats penceresi (7 gun) hala veriye erisebilecegi icin tekrar denenir. Boylece WorkManager
 * kisa sureli gecikse (ag/Doze) veri kaybi olmadan telafi edilir, ama sonsuza kadar da beklenmez.
 */
@Singleton
class SettleMissionInstancesUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRunner: MissionSettlementTransactionRunner,
    private val missionInstanceDao: MissionInstanceDao,
    private val missionHistoryDao: MissionHistoryDao,
    private val usageStatsSource: MissionUsageStatsSource,
    private val zoneId: ZoneId,
) {

    data class SettlementResult(
        val settledCount: Int = 0,
        val starsAwarded: Int = 0,
        val failures: Int = 0,
        val dataUnavailable: Int = 0,
        val skippedRetryLater: Int = 0,
    )

    companion object {
        /** Donem bitiminden bu kadar eskiyse artik veri beklenmez — settled+DATA_UNAVAILABLE. */
        val STALE_RETRY_GRACE_MS: Long = TimeUnit.HOURS.toMillis(48)

        /** UsageStats event gunlugunun tipik olarak eristigi gecmis pencere (gun). */
        private const val HISTORICAL_LOOKBACK_DAYS = 10

        /** Dongu G4 — epochDay -> gun ortasi millis donusumu icin. */
        private const val MS_PER_DAY = 24L * 3600 * 1000
    }

    /**
     * Donemi biten (periodEndAt <= now) ama hala "assigned" olan butun instance'lari bulur ve
     * sonuclandirir. Ayni instance iki kez sonuclanmaz (getUnsettledBefore sadece status=assigned
     * doner; settleInstance status'u degistirdikten sonra sorgu bir daha eslesmez).
     */
    suspend fun settleOverdue(now: Long): SettlementResult {
        val overdue = missionInstanceDao.getUnsettledBefore(now)
        if (overdue.isEmpty()) return SettlementResult()

        var settledCount = 0
        var starsAwarded = 0
        var failures = 0
        var dataUnavailable = 0
        var skippedRetryLater = 0

        // F3 (G4 duzeltmesi) — burada yalniz DOKUNULAN gunlerin listesi tutulur; advance()'e
        // giden completed/total sayilari donguden SONRA DB'nin gun-butunu sorgularindan
        // (countCompletedForDay/countSettledForDay) okunur. Batch ici sayim YANLISTI: anında
        // tamamlanan gorevler (completeActionMission) bu batch'e hic girmedigi icin gun 0/1
        // gorunebiliyordu (gercek 2/3) — ters yonde de tek gorevlik batch sahte %100 verebiliyordu.
        val touchedDailyEpochDays = mutableSetOf<Long>()

        for (instance in overdue) {
            val ageMs = now - instance.periodEndAt
            val evaluation = evaluateHistorical(instance)

            if (evaluation.status == MissionStatus.DATA_UNAVAILABLE && ageMs < STALE_RETRY_GRACE_MS) {
                // Henuz "yeterince eski" degil — UsageStats penceresi hala veriye erisebilir,
                // bir sonraki catch-up'a birak (settle ETME, tekrar denensin).
                skippedRetryLater++
                continue
            }

            val settledAt = now
            val finalStatus = when (evaluation.status) {
                MissionStatus.COMPLETED -> MissionInstanceEntity.STATUS_COMPLETED
                // F4: veri-yok gorev FAILED olarak YAZILMAZ — raporlar/seri kirlenmesin.
                MissionStatus.DATA_UNAVAILABLE -> MissionInstanceEntity.STATUS_DATA_UNAVAILABLE
                else -> MissionInstanceEntity.STATUS_FAILED
            }

            val starsForInstance = transactionRunner.runInTransaction {
                var awarded = 0
                if (evaluation.status == MissionStatus.COMPLETED) {
                    val inserted = missionHistoryDao.insert(
                        MissionHistoryEntry(
                            missionId = instance.missionId,
                            periodType = instance.periodType,
                            periodStartEpoch = instance.periodStartEpoch,
                            completedAt = settledAt,
                            starReward = instance.starReward,
                            source = "settlement",
                        )
                    )
                    // insert OnConflictStrategy.IGNORE -> -1 doner ikinci deneme/carpisma durumunda.
                    // unique index (missionId, periodType, periodStartEpoch) ikinci odulu engeller.
                    if (inserted != -1L) awarded = instance.starReward
                }
                missionInstanceDao.settleInstance(instance.instanceId, finalStatus, settledAt)
                awarded
            }

            settledCount++
            starsAwarded += starsForInstance
            when (evaluation.status) {
                MissionStatus.COMPLETED -> Unit
                MissionStatus.DATA_UNAVAILABLE -> dataUnavailable++
                else -> failures++
            }

            if (instance.periodType == MissionInstanceEntity.PERIOD_DAILY) {
                touchedDailyEpochDays.add(instance.periodStartEpoch)
            }
        }

        if (settledCount > 0 || skippedRetryLater > 0) {
            Timber.d(
                "MissionSettlement: settled=$settledCount stars=$starsAwarded failures=$failures " +
                    "dataUnavailable=$dataUnavailable retryLater=$skippedRetryLater",
            )
        }

        // Dongu G4 — her kapanan gunun seri durumunu GUNCELLE. MissionStreakPrefs.advance kendi
        // idempotency'sini tasir (lastCountedEpochDay ayniysa hicbir sey degismez) — burada ayrica
        // "zaten sayildi mi" kontrolu GEREKMEZ, ama yine de guvenlik icin runCatching ile sarilir:
        // seri hesaplama hatasi ODUL/INSTANCE durumunu ETKILEMEMELI (bagimsiz katman).
        runCatching {
            touchedDailyEpochDays.forEach { epochDay ->
                // Gun kapanisinin GERCEK tablosu: tum settle edilmis instance'lar (erken tamamlananlar
                // dahil) — batch'te ne oldugundan bagimsiz.
                val completedCount = missionInstanceDao.countCompletedForDay(epochDay)
                val totalCount = missionInstanceDao.countSettledForDay(epochDay)
                // F4: gunun TUM gorevleri veri-yok kapandiysa (totalCount 0 — sorgu
                // data_unavailable'i saymaz) gun NOTR'dur: seri ne ilerler ne kirilir.
                if (totalCount == 0) return@forEach
                // Dondurma hakkinin haftalik yenilenmesi SETTLENEN GUNUN kendi ISO haftasina gore
                // olmali (settlement gecikmis gunleri toplu isleyebilir — "now"in haftasi yanlis
                // olur). Gunun ortasina (12:00) sabitlenmis bir Clock ile o gunun haftasi cozulur.
                val dayMiddayMillis = epochDay * MS_PER_DAY + MS_PER_DAY / 2
                val dayResolver = PeriodBoundaryResolver(Clock.fixed(Instant.ofEpochMilli(dayMiddayMillis), zoneId), zoneId)
                val weekBoundary = dayResolver.currentIsoWeek()
                val weekStartEpochDay = weekBoundary.weekStartEpochDay ?: weekBoundary.epochDay
                MissionStreakPrefs.advance(
                    context = context,
                    epochDay = epochDay,
                    completedCount = completedCount,
                    totalCount = totalCount,
                    weekStartEpochDayForFreezeCheck = weekStartEpochDay,
                )
            }
        }.onFailure { e -> Timber.w(e, "MissionStreakPrefs.advance basarisiz (settlement etkilenmedi)") }

        return SettlementResult(
            settledCount = settledCount,
            starsAwarded = starsAwarded,
            failures = failures,
            dataUnavailable = dataUnavailable,
            skippedRetryLater = skippedRetryLater,
        )
    }

    /**
     * Eylem/bayrak gorevleri hedefe ulasinca ANINDA tamamlanir (MissionsViewModel'deki mevcut
     * "instantlyCompletableMissionIds" yolunun M01 instance kaydiyla senkronize edilmesi).
     * Odul mission_history'ye zaten cagiran tarafca (MissionsViewModel.markDailyCompleted/
     * markWeeklyCompleted) yazilmis olur — bu fonksiyon SADECE instance status'unu COMPLETED+
     * settled olarak isaretler, ikinci bir odul YAZMAZ (mission_history'ye dokunmaz).
     */
    suspend fun completeActionMission(
        instanceId: String,
        settledAt: Long = System.currentTimeMillis(),
    ): SettlementResult {
        missionInstanceDao.settleInstance(instanceId, MissionInstanceEntity.STATUS_COMPLETED, settledAt)
        return SettlementResult(settledCount = 1, starsAwarded = 0)
    }

    /**
     * Instance'in gorev tipine gore donem SONU metrikleriyle degerlendirir.
     * Ust sinir gorevleri (ekran suresi/kilit acma) ve gece gorevi -> o donemin GUNUNE
     * (periodEndAt'tan bir "tik" once, yani donemin son gunu) ait UsageStats verisi gerekir.
     * Haftalik karsilastirma -> donemin haftasi + bir onceki hafta toplam dakikasi gerekir.
     * Eylem/bayrak gorevleri buraya hic dusmez (aninda tamamlanir, assigned kalmazlar bekliyorsa
     * kullanici hic yapmamis demektir -> FAILED, yildiz yok — adil: hedef gerceklesmedi).
     */
    private fun evaluateHistorical(instance: MissionInstanceEntity): MissionEvaluation {
        val mission = MissionEngine.Mission(
            id = instance.missionId,
            type = if (instance.periodType == MissionInstanceEntity.PERIOD_WEEKLY) {
                MissionEngine.MissionType.WEEKLY
            } else {
                MissionEngine.MissionType.DAILY
            },
            starReward = instance.starReward,
            autoCheckable = true,
        )

        // Donemin son ani (periodEndAt gece yarisidir -> 1ms once donemin kendi gunu/haftasi icinde).
        val periodAnchor = (instance.periodEndAt - 1).coerceAtLeast(instance.periodStartAt)
        val checkInput = buildHistoricalCheckInput(instance, periodAnchor)
            ?: return MissionEvaluation(MissionStatus.DATA_UNAVAILABLE, null, instance.targetValue, null)

        return MissionEngine.evaluate(
            mission = mission,
            input = checkInput,
            now = LocalTime.MAX,
            dayEnded = instance.periodType == MissionInstanceEntity.PERIOD_DAILY,
            weekEnded = instance.periodType == MissionInstanceEntity.PERIOD_WEEKLY,
        )
    }

    /**
     * [MissionUsageStatsSource]'u donemin bitisine (`anchorMillis`) sabitlenmis pencerelerle
     * sorgular. Veri alinamazsa (izin yok / event gunlugu pencere disina cikmis) null doner ->
     * cagiran taraf DATA_UNAVAILABLE'a duser.
     */
    private fun buildHistoricalCheckInput(
        instance: MissionInstanceEntity,
        anchorMillis: Long,
    ): MissionEngine.MissionCheckInput? {
        val taskEvents = MissionEngine.TaskEventInput(
            positiveEventsToday = 0,
            positiveEventsThisWeek = 0,
            classificationActionsToday = 0,
            notificationReportViewedToday = false,
        )

        return when (instance.missionId) {
            MissionEngine.DAILY_SCREEN_UNDER_3H -> {
                val minutes = dailyForegroundMinutes(anchorMillis) ?: return null
                MissionEngine.MissionCheckInput(
                    screenTimeMinutesToday = minutes,
                    taskEvents = taskEvents,
                    // Dongu G1: pin edilmis kisisel hedef (donem boyunca sabit) settlement'ta da
                    // AYNI olmali — aksi halde pin sirasinda 2sa.40dk ile tamamlanmis sayilan bir
                    // gorev, settlement'ta sabit 180dk ile degerlendirilip tutarsizlik yaratir.
                    personalScreenTargetMinutes = instance.targetValue,
                )
            }
            MissionEngine.DAILY_UNLOCK_UNDER_30 -> {
                val unlocks = unlockCountForDay(anchorMillis) ?: return null
                MissionEngine.MissionCheckInput(
                    unlockCountToday = unlocks,
                    taskEvents = taskEvents,
                    personalUnlockTarget = instance.targetValue,
                )
            }
            MissionEngine.DAILY_NO_LATE_NIGHT -> {
                val usedAfter23 = usedAfter23(anchorMillis) ?: return null
                MissionEngine.MissionCheckInput(usedAfter23Today = usedAfter23, taskEvents = taskEvents)
            }
            MissionEngine.WEEKLY_SCREEN_LESS -> {
                val currentWeek = weeklyForegroundMinutes(
                    startInclusive = instance.periodStartAt,
                    endExclusive = instance.periodEndAt,
                    anchorMillis = anchorMillis,
                ) ?: return null
                val previous = instance.baselineValue ?: return null
                MissionEngine.MissionCheckInput(
                    weeklyScreenTimeMinutes = currentWeek,
                    previousWeeklyScreenTimeMinutes = previous,
                    taskEvents = taskEvents,
                )
            }
            else -> null
        }
    }

    private fun epochDayAt(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate().toEpochDay()

    private fun dailyForegroundMinutes(anchorMillis: Long): Long? {
        val targetEpochDay = epochDayAt(anchorMillis)
        val sessions = usageStatsSource.getDailySessionUsage(
            context,
            days = HISTORICAL_LOOKBACK_DAYS,
            nowMillis = anchorMillis,
        ) ?: return null
        val dayEntries = sessions.filter { it.epochDay == targetEpochDay }
        if (dayEntries.isEmpty()) return null
        return (dayEntries.maxOfOrNull { it.globalForegroundMs } ?: 0L) / TimeUnit.MINUTES.toMillis(1)
    }

    private fun weeklyForegroundMinutes(startInclusive: Long, endExclusive: Long, anchorMillis: Long): Long? {
        val sessions = usageStatsSource.getDailySessionUsage(
            context,
            days = HISTORICAL_LOOKBACK_DAYS,
            nowMillis = anchorMillis,
        ) ?: return null
        val startEpochDay = epochDayAt(startInclusive)
        val endEpochDay = epochDayAt(endExclusive - 1)
        val minutesByDay = sessions
            .filter { it.epochDay in startEpochDay..endEpochDay }
            .groupBy { it.epochDay }
            .mapValues { (_, list) -> (list.maxOfOrNull { it.globalForegroundMs } ?: 0L) / TimeUnit.MINUTES.toMillis(1) }
        if (minutesByDay.isEmpty()) return null
        return minutesByDay.values.sum()
    }

    private fun unlockCountForDay(anchorMillis: Long): Int? =
        usageStatsSource.getUnlockCount(context, days = 1, nowMillis = anchorMillis)

    private fun usedAfter23(anchorMillis: Long): Boolean? {
        val targetEpochDay = epochDayAt(anchorMillis)
        val sessions = usageStatsSource.getDailySessionUsage(
            context,
            days = HISTORICAL_LOOKBACK_DAYS,
            nowMillis = anchorMillis,
        ) ?: return null
        val dayEntries = sessions.filter { it.epochDay == targetEpochDay }
        if (dayEntries.isEmpty()) return null
        return dayEntries.any { entry -> entry.hourlyForegroundMs.getOrNull(23)?.let { it > 0L } == true }
    }
}
