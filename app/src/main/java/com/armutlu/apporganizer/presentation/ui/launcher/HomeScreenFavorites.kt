package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.AppAnalytics

/**
 * HomeScreen'deki favori, öneri ve son kullanılan uygulama satırlarını
 * tek bir composable altında toplayan section.
 *
 * Cache destekli öneriler: suggestedApps 30 dakikada bir yenilenir, günde aynı saatte açılan uygulamayı önerir.
 *
 * Parametre sayısını azaltmak için lambdalar HomeScreen'den geçirilir.
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
    // Küçük ekranlarda (< 640dp) sadece favorileri göster — öneri+son kullanılanlar grid'i iter
    val compactMode = screenHeightDp < 640 && !showSecondaryRowsInCompactMode
    val dockPkgs = dockPackages.toSet()
    val favoritePkgs = favoriteApps.mapTo(mutableSetOf()) { it.packageName }
    val visibleSuggestions = suggestedApps
        .filter { it.packageName !in dockPkgs && it.packageName !in favoritePkgs }
        .take(3)
    val suggestionPkgs = visibleSuggestions.mapTo(mutableSetOf()) { it.packageName }
    val visibleRecent = recentApps
        .filter {
            it.packageName !in dockPkgs &&
                it.packageName !in favoritePkgs &&
                it.packageName !in suggestionPkgs
        }
        .take(4)

    if (favoritesEnabled && favoriteApps.isNotEmpty()) {
        FavoritesRow(
            apps = favoriteApps,
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

    if (!compactMode && suggestionsEnabled && visibleSuggestions.isNotEmpty()) {
        AppSuggestionsRow(
            apps = visibleSuggestions,
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

    if (!compactMode && recentNotificationAppsEnabled && recentNotificationApps.isNotEmpty()) {
        RecentNotificationAppsRow(
            apps = recentNotificationApps,
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

    if (!compactMode && recentAppsEnabled && visibleRecent.isNotEmpty()) {
        RecentAppsRow(
            apps = visibleRecent,
            iconPackPkg = iconPackPkg,
            onAppClick = { pkg ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLaunchApp(pkg)
            }
        )
    }
}
