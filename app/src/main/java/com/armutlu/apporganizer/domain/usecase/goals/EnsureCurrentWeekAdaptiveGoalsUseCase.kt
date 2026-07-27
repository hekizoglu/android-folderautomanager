package com.armutlu.apporganizer.domain.usecase.goals

import android.content.Context
import com.armutlu.apporganizer.data.local.WeeklyGoalDao
import com.armutlu.apporganizer.domain.models.WeeklyGoal
import com.armutlu.apporganizer.domain.models.WeeklyGoalMode
import com.armutlu.apporganizer.domain.models.WeeklyGoalStatus
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.domain.usecase.missions.AppLimitCandidateSelector
import com.armutlu.apporganizer.utils.AppPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P4 — roadmap §6 adım 4-5. Mevcut ISO haftası için AUTO modundaki kategori hedeflerini
 * hesaplar ve haftanın başlangıcına PİNLER (bir kez üretilir, hafta içinde tekrar değişmez —
 * [WeeklyGoalDao.getGoal] ile "bu hafta için zaten var mı" kontrolü idempotency'i sağlar,
 * ikinci çağrı mevcut satırı asla değiştirmez).
 *
 * MANUAL moddaki kategorilere DOKUNMAZ — kullanıcı rızası olmadan hiçbir mevcut hedef ezilmez
 * (roadmap §3). Sadece: (a) adaptif sistem açık, (b) kategori eligible (SOCIAL/GAMES/VIDEO +
 * kullanıcının elle eklediği), (c) o kategori için mevcut haftada henüz hiç hedef yok VEYA
 * mevcut hedef zaten AUTO ise yeni hesap üretir.
 */
@Singleton
class EnsureCurrentWeekAdaptiveGoalsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val weeklyGoalDao: WeeklyGoalDao,
    private val categoryUsageSnapshotProvider: CategoryUsageSnapshotProvider,
    private val periodBoundaryResolver: PeriodBoundaryResolver,
    private val clock: Clock,
) {

    suspend fun execute() {
        if (!AppPrefs.isAdaptiveGoalsEnabled(context)) return

        val currentWeekStart = periodBoundaryResolver.currentIsoWeek().weekStartEpochDay ?: return
        val eligibleCategories = eligibleCategoryIds()
        if (eligibleCategories.isEmpty()) return
        if (weeklyGoalDao.getAutoGoalsForWeek(currentWeekStart).size >= AppPrefs.getAdaptiveGoalsMaxActive(context)) {
            return
        }

        val snapshot = categoryUsageSnapshotProvider.capture()
        val pace = AdaptiveGoalPace.fromMissionTempo(AppPrefs.getAdaptiveGoalPaceTempo(context))
        val now = clock.millis()

        var activeAutoCount = weeklyGoalDao.getAutoGoalsForWeek(currentWeekStart).size
        val maxActive = AppPrefs.getAdaptiveGoalsMaxActive(context)

        for (categoryId in eligibleCategories) {
            if (activeAutoCount >= maxActive) break

            val existing = weeklyGoalDao.getGoal(categoryId, currentWeekStart)
            if (existing != null && existing.mode == WeeklyGoalMode.MANUAL) continue
            if (existing != null && existing.mode == WeeklyGoalMode.AUTO) continue // zaten pinlenmiş — dokunma

            val previousWeekActual = snapshot.previousWeekMinutes(categoryId) ?: continue
            val previousAutoGoal = findPreviousWeekGoal(categoryId, currentWeekStart)

            val result = AdaptiveCategoryTargetCalculator.calculate(
                AdaptiveCategoryTargetCalculator.Input(
                    previousWeekActualMinutes = previousWeekActual,
                    previousTargetMinutes = previousAutoGoal?.targetMinutes?.toLong(),
                    validDataDayCount = snapshot.validDataDayCount,
                    pace = pace,
                ),
            )

            val target = (result as? AdaptiveCategoryTargetCalculator.Result.Target)?.targetMinutes ?: continue

            weeklyGoalDao.upsert(
                WeeklyGoal(
                    categoryId = categoryId,
                    targetMinutes = target.toInt(),
                    weekStartEpochDay = currentWeekStart,
                    createdAt = now,
                    mode = WeeklyGoalMode.AUTO,
                    baselineMinutes = previousWeekActual,
                    previousWeekActualMinutes = previousWeekActual,
                    pace = pace.name,
                    status = WeeklyGoalStatus.ACTIVE,
                    generatedAt = now,
                ),
            )
            activeAutoCount++
        }
    }

    private suspend fun findPreviousWeekGoal(categoryId: String, currentWeekStart: Long): WeeklyGoal? {
        val previousWeekStart = periodBoundaryResolver.previousIsoWeek().weekStartEpochDay ?: return null
        if (previousWeekStart >= currentWeekStart) return null
        return weeklyGoalDao.getGoal(categoryId, previousWeekStart)?.takeIf { it.mode == WeeklyGoalMode.AUTO }
    }

    private fun eligibleCategoryIds(): Set<String> {
        val excluded = AppPrefs.getAdaptiveGoalsExcludedCategories(context)
        val included = AppPrefs.getAdaptiveGoalsIncludedCategories(context)
        return (AppLimitCandidateSelector.ELIGIBLE_CATEGORY_IDS + included) - excluded
    }
}
