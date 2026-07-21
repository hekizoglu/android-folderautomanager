package com.armutlu.apporganizer.domain.home.smartaccess

import com.armutlu.apporganizer.domain.models.AppInfo

/** Ham kullanım sinyallerini ranker'ın 0..1 aday modeline dönüştüren tek Hero giriş noktası. */
object SmartAccessCoordinator {
    private const val RECENCY_WINDOW_MS = 7L * 24 * 60 * 60 * 1000

    fun rankNow(
        apps: List<AppInfo>,
        slotPackages: List<String>,
        frequencyScores: Map<String, Float>,
        weekdayScores: Map<String, Float>,
        ownPackageName: String,
        nowMillis: Long,
        favoritesFallback: List<AppInfo> = emptyList(),
        recentFallback: List<AppInfo> = emptyList(),
    ): List<AppInfo> {
        val slotCount = slotPackages.size.coerceAtLeast(1)
        val slotScore = slotPackages.distinct().mapIndexed { index, pkg ->
            pkg to (slotCount - index).toFloat() / slotCount
        }.toMap()
        val candidates = apps.map { app ->
            val age = (nowMillis - app.lastUsedTimestamp).coerceAtLeast(0L)
            SmartAccessCandidate(
                app = app,
                sameTimeSlotScore = slotScore[app.packageName] ?: 0f,
                recencyScore = if (app.lastUsedTimestamp <= 0L) 0f
                    else 1f - age.coerceAtMost(RECENCY_WINDOW_MS).toFloat() / RECENCY_WINDOW_MS,
                frequencyScore = frequencyScores[app.packageName] ?: 0f,
                weekdayContextScore = weekdayScores[app.packageName] ?: 0f,
            )
        }
        return SmartAccessRanker.rankNow(
            candidates = candidates,
            ownPackageName = ownPackageName,
            favoritesFallback = favoritesFallback,
            recentFallback = recentFallback,
        )
    }
}
