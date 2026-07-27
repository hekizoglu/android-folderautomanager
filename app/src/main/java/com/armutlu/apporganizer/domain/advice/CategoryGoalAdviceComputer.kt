package com.armutlu.apporganizer.domain.advice

import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.WeeklyGoal
import com.armutlu.apporganizer.domain.usecase.goals.CategoryUsageSnapshot
import java.time.Clock

/**
 * P8 — [DashboardViewModel][com.armutlu.apporganizer.presentation.viewmodel.DashboardViewModel]
 * VE [MissionsViewModel][com.armutlu.apporganizer.presentation.viewmodel.MissionsViewModel] AYNI
 * bu fonksiyonu çağırır — Dashboard'daki TodayCard'ın ADVICE'ı ile Görevler ekranındaki
 * "Bugünün Tavsiyesi" kartı FARKLI hesaplama yapmasın diye (roadmap P8 "ikinci bir kart deseni
 * yazılmaz" ilkesinin doğal uzantısı: hesaplama da tekil olmalı).
 */
data class CategoryGoalForAdvice(
    val categoryId: String,
    val goal: WeeklyGoal,
    val previousWeekMinutes: Long?,
    val currentWeekMinutesSoFar: Long?,
)

suspend fun computeDigitalAdvice(
    snapshot: CategoryUsageSnapshot,
    goalsUi: List<CategoryGoalForAdvice>,
    appDao: AppDao,
    clock: Clock,
): DigitalAdvice? {
    val exceeded = goalsUi.count { g ->
        val used = g.currentWeekMinutesSoFar
        used != null && used > g.goal.targetMinutes
    }
    val atRisk = goalsUi.count { g ->
        val used = g.currentWeekMinutesSoFar
        used != null && used <= g.goal.targetMinutes && used >= g.goal.targetMinutes * 0.8
    }
    val allOnTrack = goalsUi.isNotEmpty() && goalsUi.all { g ->
        val used = g.currentWeekMinutesSoFar
        used != null && used <= g.goal.targetMinutes
    }
    val categoryChanges = goalsUi.mapNotNull { g ->
        val previous = g.previousWeekMinutes
        val current = g.currentWeekMinutesSoFar
        if (previous == null || current == null) return@mapNotNull null
        CategoryUsageChange(
            // Kategori adı burada çözülmez (kimlik motora taşınmaz) — mesaj şablonu genel
            // "bir kategoride" ifadesi kullanır, spesifik ad ileride UI katmanında zenginleştirilebilir.
            categoryNameRes = R.string.advice_usage_increase_generic_category,
            previousWeekMinutes = previous,
            currentWeekMinutes = current,
        )
    }

    val apps = runCatching { appDao.getAllApps() }.getOrDefault(emptyList())
    val now = clock.millis()
    val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
    val unusedCount = apps.count { app ->
        !app.isHidden && !app.isSystemApp &&
            app.lastUsedTimestamp > 0L && (now - app.lastUsedTimestamp) > thirtyDaysMs
    }
    val uncategorizedCount = apps.count { app ->
        !app.isHidden && !app.isSystemApp && app.categoryId == Category.CAT_UNCATEGORIZED
    }

    val input = DigitalAdviceInput(
        permissionFreshness = snapshot.freshness,
        categoryUsageChanges = categoryChanges,
        exceededCategoryGoalCount = exceeded,
        atRiskCategoryGoalCount = atRisk,
        allCategoryGoalsOnTrack = allOnTrack,
        hasAnyCategoryGoal = goalsUi.isNotEmpty(),
        unusedAppCount = unusedCount,
        uncategorizedAppCount = uncategorizedCount,
    )
    return DigitalAdviceEngine.evaluate(input, now)
}
