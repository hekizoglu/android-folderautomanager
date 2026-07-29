package com.armutlu.apporganizer.domain.usecase.missions

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import java.time.Clock
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dongu M04 — bir SONRAKI yakin donem sinirina (gece yarisi veya hafta baslangici, hangisi
 * daha yakinsa) tek seferlik `MissionSettlementWorker` isi planlar. Exact alarm KULLANILMAZ —
 * WorkManager'in normal (Doze/battery-aware) zamanlamasina birakilir; roadmap'in "WorkManager tam
 * zamaninda calismazsa HOME_RESUME sirasinda catch-up settlement yapilir" kurali bu gecikmeyi
 * tolere eder.
 */
@Singleton
class MissionWorkScheduler @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val periodBoundaryResolver: PeriodBoundaryResolver,
    private val clock: Clock,
) {

    companion object {
        const val WORK_NAME = "mission_settlement"

        /** Isin en erken ne zaman calisabilecegini hesaplar (test edilebilirlik icin ayri fonksiyon). */
        fun computeInitialDelayMs(nowMillis: Long, nextMidnight: Instant, nextWeekBoundary: Instant): Long {
            val nextBoundaryMillis = minOf(nextMidnight.toEpochMilli(), nextWeekBoundary.toEpochMilli())
            return (nextBoundaryMillis - nowMillis).coerceAtLeast(0L)
        }
    }

    /**
     * Uygulama acilisinda mevcut aktif isi KORUR. REPLACE kullanmak, WorkManager'in Doze nedeniyle
     * gecikmis ama hala ENQUEUED olan gece yarisi isini her uygulama acilisinda ertesi gune
     * itiyordu. KEEP ile kacirilmis isin uygulama yeniden acildiginda calismasina izin verilir.
     */
    fun ensureNextScheduled() {
        enqueueNext(ExistingWorkPolicy.KEEP)
    }

    /**
     * Settlement worker tamamlandiktan sonra zincirin yeni donem sinirina tasinmasi icin kullanilir.
     * Burada REPLACE bilincli olarak korunur; tamamlanan sinira ait kayit yerine yeni sinir yazilir.
     */
    fun scheduleNext() {
        enqueueNext(ExistingWorkPolicy.REPLACE)
    }

    private fun enqueueNext(existingWorkPolicy: ExistingWorkPolicy) {
        val nowMillis = clock.millis()
        val nextMidnight = periodBoundaryResolver.nextLocalMidnight()
        val nextWeekBoundary = periodBoundaryResolver.nextWeekBoundary()
        val delayMs = computeInitialDelayMs(nowMillis, nextMidnight, nextWeekBoundary)

        val request = OneTimeWorkRequestBuilder<com.armutlu.apporganizer.workers.MissionSettlementWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            existingWorkPolicy,
            request,
        )
    }
}
