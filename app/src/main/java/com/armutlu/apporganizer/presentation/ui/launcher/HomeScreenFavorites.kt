package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.HomeLayoutItem
import com.armutlu.apporganizer.domain.models.HomeSectionId
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

internal fun HomeContextualRowKind.sectionId(): HomeSectionId = when (this) {
    HomeContextualRowKind.FAVORITES -> HomeSectionId.FAVORITES
    HomeContextualRowKind.SUGGESTIONS -> HomeSectionId.SUGGESTIONS
    HomeContextualRowKind.RECENT_NOTIFICATIONS -> HomeSectionId.RECENT_NOTIFICATIONS
    HomeContextualRowKind.RECENT_APPS -> HomeSectionId.RECENT_APPS
}

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

    val sectionId = row.kind.sectionId()
    HomeSectionRenderer(
        items = listOf(HomeLayoutItem(sectionId, sectionId.defaultZone, order = 0, visible = true)),
    ) { renderedSectionId -> when (renderedSectionId) {
        HomeSectionId.FAVORITES -> HomeFavoritesRowSection(row.apps, iconPackPkg, haptic, onLaunchApp, onAppLongClick)
        HomeSectionId.SUGGESTIONS -> HomeSuggestionsRowSection(row.apps, iconPackPkg, suggestionsIconSizeDp, haptic, onLaunchApp, onAppLongClick)
        HomeSectionId.RECENT_NOTIFICATIONS -> HomeRecentNotificationsRowSection(row.apps, recentNotificationCounts, iconPackPkg, haptic, onLaunchApp, onAppLongClick)
        HomeSectionId.RECENT_APPS -> HomeRecentAppsRowSection(row.apps, iconPackPkg, haptic, onLaunchApp)
        else -> Unit
    } }
}

@Composable
internal fun HomeFavoritesRowSection(apps: List<AppInfo>, iconPackPkg: String, haptic: HapticFeedback, onLaunchApp: (String) -> Unit, onAppLongClick: (String) -> Unit) =
    FavoritesRow(apps, iconPackPkg, onAppClick = { pkg ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        AppAnalytics.appLaunched("favorites")
        onLaunchApp(pkg)
    }, onAppLongClick = { pkg ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onAppLongClick(pkg)
    })

@Composable
internal fun HomeSuggestionsRowSection(apps: List<AppInfo>, iconPackPkg: String, iconSizeDp: Int, haptic: HapticFeedback, onLaunchApp: (String) -> Unit, onAppLongClick: (String) -> Unit) =
    AppSuggestionsRow(apps, iconPackPkg, iconSizeDp, onAppClick = { app ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onLaunchApp(app.packageName)
    }, onAppLongClick = { app ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onAppLongClick(app.packageName)
    })

@Composable
internal fun HomeRecentNotificationsRowSection(apps: List<AppInfo>, notificationCounts: Map<String, Int>, iconPackPkg: String, haptic: HapticFeedback, onLaunchApp: (String) -> Unit, onAppLongClick: (String) -> Unit) =
    RecentNotificationAppsRow(apps, notificationCounts, iconPackPkg, onAppClick = { app ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onLaunchApp(app.packageName)
    }, onAppLongClick = { app ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onAppLongClick(app.packageName)
    })

@Composable
internal fun HomeRecentAppsRowSection(apps: List<AppInfo>, iconPackPkg: String, haptic: HapticFeedback, onLaunchApp: (String) -> Unit) =
    RecentAppsRow(apps, iconPackPkg, onAppClick = { pkg ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onLaunchApp(pkg)
    })
