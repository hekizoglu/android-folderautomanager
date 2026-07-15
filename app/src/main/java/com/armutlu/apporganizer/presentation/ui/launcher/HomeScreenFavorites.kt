package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.AppAnalytics

internal enum class HomeContextualRowKind {
    SUGGESTIONS,
    RECENT_NOTIFICATIONS,
    RECENT_APPS,
    FAVORITES,
}

internal data class HomeContextualRow(
    val kind: HomeContextualRowKind,
    val apps: List<AppInfo>,
)

internal fun selectHomeContextualRow(
    favoritesEnabled: Boolean,
    favoriteApps: List<AppInfo>,
    suggestionsEnabled: Boolean,
    suggestedApps: List<AppInfo>,
    recentNotificationAppsEnabled: Boolean,
    recentNotificationApps: List<AppInfo>,
    recentAppsEnabled: Boolean,
    recentApps: List<AppInfo>,
    dockPackages: List<String>,
    maxApps: Int = 4,
): HomeContextualRow? {
    val dockPkgs = dockPackages.toSet()
    val favorites = favoriteApps.filterNot { it.packageName in dockPkgs }.take(maxApps)
    val favoritePkgs = favorites.mapTo(mutableSetOf()) { it.packageName }
    val suggestions = suggestedApps
        .filter { it.packageName !in dockPkgs && it.packageName !in favoritePkgs }
        .take(maxApps.coerceAtMost(3))
    val suggestionPkgs = suggestions.mapTo(mutableSetOf()) { it.packageName }
    val notifications = recentNotificationApps
        .filter {
            it.packageName !in dockPkgs &&
                it.packageName !in favoritePkgs &&
                it.packageName !in suggestionPkgs
        }
        .take(maxApps)
    val notificationPkgs = notifications.mapTo(mutableSetOf()) { it.packageName }
    val recent = recentApps
        .filter {
            it.packageName !in dockPkgs &&
                it.packageName !in favoritePkgs &&
                it.packageName !in suggestionPkgs &&
                it.packageName !in notificationPkgs
        }
        .take(maxApps)

    return when {
        suggestionsEnabled && suggestions.isNotEmpty() ->
            HomeContextualRow(HomeContextualRowKind.SUGGESTIONS, suggestions)
        recentNotificationAppsEnabled && notifications.isNotEmpty() ->
            HomeContextualRow(HomeContextualRowKind.RECENT_NOTIFICATIONS, notifications)
        recentAppsEnabled && recent.isNotEmpty() ->
            HomeContextualRow(HomeContextualRowKind.RECENT_APPS, recent)
        favoritesEnabled && favorites.isNotEmpty() ->
            HomeContextualRow(HomeContextualRowKind.FAVORITES, favorites)
        else -> null
    }
}

/**
 * Renders one contextual access row for the home hierarchy.
 *
 * P2.9 keeps the first glance calm: app suggestions, notification recents,
 * recent apps and favorites compete for a single row instead of stacking.
 */
@Composable
internal fun HomeFavoritesSection(
    favoritesEnabled: Boolean,
    favoriteApps: List<AppInfo>,
    suggestionsEnabled: Boolean,
    suggestedApps: List<AppInfo>,
    suggestionsIconSizeDp: Int = 40,
    recentNotificationAppsEnabled: Boolean = false,
    recentNotificationApps: List<AppInfo> = emptyList(),
    recentNotificationCounts: Map<String, Int> = emptyMap(),
    recentAppsEnabled: Boolean,
    recentApps: List<AppInfo>,
    dockPackages: List<String> = emptyList(),
    iconPackPkg: String,
    haptic: HapticFeedback,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    screenHeightDp: Int = 800,
    showSecondaryRowsInCompactMode: Boolean = false,
) {
    val compactMode = screenHeightDp < 640 && !showSecondaryRowsInCompactMode
    val row = selectHomeContextualRow(
        favoritesEnabled = favoritesEnabled,
        favoriteApps = favoriteApps,
        suggestionsEnabled = suggestionsEnabled && !compactMode,
        suggestedApps = suggestedApps,
        recentNotificationAppsEnabled = recentNotificationAppsEnabled && !compactMode,
        recentNotificationApps = recentNotificationApps,
        recentAppsEnabled = recentAppsEnabled && !compactMode,
        recentApps = recentApps,
        dockPackages = dockPackages,
    ) ?: return

    when (row.kind) {
        HomeContextualRowKind.FAVORITES -> {
            FavoritesRow(
                apps = row.apps,
                iconPackPkg = iconPackPkg,
                onAppClick = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    AppAnalytics.appLaunched("favorites")
                    onLaunchApp(pkg)
                },
                onAppLongClick = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAppLongClick(pkg)
                }
            )
        }
        HomeContextualRowKind.SUGGESTIONS -> {
            AppSuggestionsRow(
                apps = row.apps,
                iconPackPkg = iconPackPkg,
                iconSizeDp = suggestionsIconSizeDp,
                onAppClick = { app ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLaunchApp(app.packageName)
                },
                onAppLongClick = { app ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAppLongClick(app.packageName)
                }
            )
        }
        HomeContextualRowKind.RECENT_NOTIFICATIONS -> {
            RecentNotificationAppsRow(
                apps = row.apps,
                notificationCounts = recentNotificationCounts,
                iconPackPkg = iconPackPkg,
                onAppClick = { app ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLaunchApp(app.packageName)
                },
                onAppLongClick = { app ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAppLongClick(app.packageName)
                }
            )
        }
        HomeContextualRowKind.RECENT_APPS -> {
            RecentAppsRow(
                apps = row.apps,
                iconPackPkg = iconPackPkg,
                onAppClick = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLaunchApp(pkg)
                }
            )
        }
    }
}
