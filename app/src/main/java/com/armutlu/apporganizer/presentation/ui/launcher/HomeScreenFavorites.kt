package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.AppAnalytics

/**
 * HomeScreen'deki favori, öneri ve son kullanılan uygulama satırlarını
 * tek bir composable altında toplayan section.
 *
 * Parametre sayısını azaltmak için lambdalar HomeScreen'den geçirilir.
 */
@Composable
internal fun HomeFavoritesSection(
    favoritesEnabled: Boolean,
    favoriteApps: List<AppInfo>,
    suggestionsEnabled: Boolean,
    suggestedApps: List<AppInfo>,
    recentAppsEnabled: Boolean,
    recentApps: List<AppInfo>,
    iconPackPkg: String,
    haptic: HapticFeedback,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
) {
    if (favoritesEnabled && favoriteApps.isNotEmpty()) {
        FavoritesRow(
            apps = favoriteApps,
            iconPackPkg = iconPackPkg,
            onAppClick = { pkg ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                AppAnalytics.appLaunched(pkg, "favorites")
                onLaunchApp(pkg)
            },
            onAppLongClick = { pkg ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAppLongClick(pkg)
            }
        )
    }

    if (suggestionsEnabled && suggestedApps.isNotEmpty()) {
        AppSuggestionsRow(
            apps = suggestedApps,
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

    if (recentAppsEnabled && recentApps.isNotEmpty()) {
        RecentAppsRow(
            apps = recentApps,
            iconPackPkg = iconPackPkg,
            onAppClick = { pkg ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLaunchApp(pkg)
            }
        )
    }
}
