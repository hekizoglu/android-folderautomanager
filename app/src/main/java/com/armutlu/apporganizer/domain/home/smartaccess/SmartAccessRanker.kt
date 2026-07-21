package com.armutlu.apporganizer.domain.home.smartaccess

import com.armutlu.apporganizer.domain.models.AppInfo

object SmartAccessRanker {
    const val MAX_ITEMS = 5

    fun rankNow(
        candidates: List<SmartAccessCandidate>,
        ownPackageName: String,
        excludedPackages: Set<String> = emptySet(),
        favoritesFallback: List<AppInfo> = emptyList(),
        recentFallback: List<AppInfo> = emptyList(),
    ): List<AppInfo> {
        val ranked = candidates
            .filter { it.app.isInstalled && !it.app.isHidden }
            .filter { it.app.packageName != ownPackageName && it.app.packageName !in excludedPackages }
            .distinctBy { it.app.packageName }
            .sortedWith(
                compareByDescending<SmartAccessCandidate> { score(it) }
                    .thenByDescending { it.app.lastUsedTimestamp }
                    .thenBy { it.app.packageName }
            )
            .map { it.app }

        return SmartAccessDedupePolicy.visibleUnique(
            apps = ranked + recentFallback + favoritesFallback,
            ownPackageName = ownPackageName,
            excludedPackages = excludedPackages,
        ).take(MAX_ITEMS)
    }

    fun recent(
        apps: List<AppInfo>,
        ownPackageName: String,
        excludedPackages: Set<String> = emptySet(),
    ): List<AppInfo> = SmartAccessDedupePolicy.visibleUnique(
        apps = apps.filter { it.lastUsedTimestamp > 0L }.sortedByDescending { it.lastUsedTimestamp },
        ownPackageName = ownPackageName,
        excludedPackages = excludedPackages,
    ).take(MAX_ITEMS)

    fun score(candidate: SmartAccessCandidate): Float =
        candidate.sameTimeSlotScore.normalized() * .45f +
            candidate.recencyScore.normalized() * .25f +
            candidate.frequencyScore.normalized() * .20f +
            candidate.weekdayContextScore.normalized() * .10f

    private fun Float.normalized(): Float = coerceIn(0f, 1f)
}
