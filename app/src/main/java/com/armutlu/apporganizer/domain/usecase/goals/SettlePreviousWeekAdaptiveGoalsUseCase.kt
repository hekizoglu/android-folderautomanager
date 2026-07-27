package com.armutlu.apporganizer.domain.usecase.goals

import com.armutlu.apporganizer.data.local.WeeklyGoalDao
import com.armutlu.apporganizer.domain.models.WeeklyGoalStatus
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P4 — roadmap §6 adım 1-3 + §2 (S1 fix). Önceki (bir önceki ISO haftadaki) TÜM hedefleri
 * (MANUAL dahil — üst-sınır sözleşmesi mode'dan bağımsızdır, roadmap §2), o haftanın GERÇEK
 * kategori kullanımına göre kesin duruma geçirir (COMPLETED/EXCEEDED/DATA_UNAVAILABLE). Hafta
 * bitmeden başarı verilmez — bu use-case sadece TAMAMLANMIŞ önceki hafta için çalışır, mevcut
 * haftanın hedeflerine dokunmaz.
 *
 * İdempotent: [WeeklyGoalDao.getUnsettledGoals] `settledAt IS NULL` filtresiyle zaten settle
 * edilmiş hedefleri atlar — aynı hafta için iki kez çağrılırsa ikinci çağrı hiçbir şey yapmaz
 * (roadmap §6 adım 6 "aynı use-case ikinci kez çalışırsa... tekrar ödül yazılmaz").
 */
@Singleton
class SettlePreviousWeekAdaptiveGoalsUseCase @Inject constructor(
    private val weeklyGoalDao: WeeklyGoalDao,
    private val categoryUsageSnapshotProvider: CategoryUsageSnapshotProvider,
    private val periodBoundaryResolver: PeriodBoundaryResolver,
    private val clock: Clock,
) {

    /** @return settle edilen ve [WeeklyGoalStatus.COMPLETED] olan hedef sayısı (bildirim tetiklemek için). */
    suspend fun execute(): Int {
        val previousWeekBoundary = periodBoundaryResolver.previousIsoWeek()
        val previousWeekStart = previousWeekBoundary.weekStartEpochDay ?: return 0

        val unsettled = weeklyGoalDao.getUnsettledGoals(previousWeekStart)
        if (unsettled.isEmpty()) return 0

        val snapshot = categoryUsageSnapshotProvider.capture()
        val settledAt = clock.millis()
        var completedCount = 0

        unsettled.forEach { goal ->
            val actualMinutes = snapshot.previousWeekMinutes(goal.categoryId)
            val status = when {
                actualMinutes == null -> WeeklyGoalStatus.DATA_UNAVAILABLE
                actualMinutes <= goal.targetMinutes -> WeeklyGoalStatus.COMPLETED
                else -> WeeklyGoalStatus.EXCEEDED
            }
            weeklyGoalDao.settle(goal.categoryId, previousWeekStart, status.name, settledAt)
            if (status == WeeklyGoalStatus.COMPLETED) {
                weeklyGoalDao.markAchieved(goal.categoryId, previousWeekStart, settledAt)
                completedCount++
            }
        }
        return completedCount
    }
}
