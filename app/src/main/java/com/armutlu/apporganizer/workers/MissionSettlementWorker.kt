package com.armutlu.apporganizer.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.armutlu.apporganizer.domain.usecase.missions.MissionWorkScheduler
import com.armutlu.apporganizer.domain.usecase.missions.SettleMissionInstancesUseCase
import com.armutlu.apporganizer.utils.WorkerTelemetryPrefs
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

/**
 * Dongu M04 — bir sonraki donem sinirinda (gece yarisi/hafta baslangici) calisan tek seferlik
 * is. Calisinca gecikmis butun gorev instance'larini sonuclandirir ([SettleMissionInstancesUseCase.settleOverdue])
 * ve bir sonraki sinira yeniden planlanir ([MissionWorkScheduler.scheduleNext]) — boylece zincir
 * kendini surdurur, PeriodicWorkRequest'e gerek kalmaz (donem sinirlari sabit araliklarla
 * cakismaz: gece yarisi her gun, hafta baslangici haftada bir).
 */
class MissionSettlementWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SettlementEntryPoint {
        fun settleMissionInstancesUseCase(): SettleMissionInstancesUseCase
        fun missionWorkScheduler(): MissionWorkScheduler
    }

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val startedAt = WorkerTelemetryPrefs.markStarted(ctx, WORK_NAME)
        return runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(ctx, SettlementEntryPoint::class.java)
            val useCase = entryPoint.settleMissionInstancesUseCase()
            val scheduler = entryPoint.missionWorkScheduler()

            val result = useCase.settleOverdue(System.currentTimeMillis())
            Timber.d(
                "MissionSettlementWorker: settled=${result.settledCount} stars=${result.starsAwarded} " +
                    "failures=${result.failures} dataUnavailable=${result.dataUnavailable} " +
                    "retryLater=${result.skippedRetryLater}",
            )

            // Zincirin devami: bir sonraki donem sinirina yeniden planla.
            scheduler.scheduleNext()

            WorkerTelemetryPrefs.markSucceeded(ctx, WORK_NAME, startedAt)
            Result.success()
        }.getOrElse { e ->
            Timber.e(e, "MissionSettlementWorker hatasi")
            WorkerTelemetryPrefs.markFailed(
                ctx,
                WORK_NAME,
                startedAt,
                WorkerTelemetryPrefs.FAILURE_UNKNOWN,
            )
            // Retry'de de zincir kopmasin diye scheduler tekrar cagrilir (bir sonraki sinira
            // planlama basarisiz olsa bile WorkManager kendi retry/backoff'unu uygular).
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "mission_settlement"
    }
}
