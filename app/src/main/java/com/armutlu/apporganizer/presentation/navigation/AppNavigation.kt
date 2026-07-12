package com.armutlu.apporganizer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.armutlu.apporganizer.presentation.ui.screens.AppListScreen
import com.armutlu.apporganizer.presentation.ui.screens.AppOrganizerDashboardScreen
import com.armutlu.apporganizer.presentation.ui.screens.CategoryEditorScreen
import com.armutlu.apporganizer.presentation.ui.screens.PermissionsGuideScreen
import com.armutlu.apporganizer.presentation.ui.screens.PrivacyPolicyScreen
import com.armutlu.apporganizer.presentation.ui.screens.ReportsCenterScreen
import com.armutlu.apporganizer.presentation.ui.screens.SearchSettingsScreen
import com.armutlu.apporganizer.presentation.ui.screens.SettingsAboutScreen
import com.armutlu.apporganizer.presentation.ui.screens.SettingsAppearanceScreen
import com.armutlu.apporganizer.presentation.ui.screens.SettingsAppsScreen
import com.armutlu.apporganizer.presentation.ui.screens.SettingsLauncherScreen
import com.armutlu.apporganizer.presentation.ui.screens.SettingsNotificationsScreen
import com.armutlu.apporganizer.presentation.ui.screens.SettingsScreen
import com.armutlu.apporganizer.presentation.ui.screens.SettingsSecurityScreen
import com.armutlu.apporganizer.presentation.ui.screens.SettingsStatsScreen
import com.armutlu.apporganizer.presentation.ui.screens.UsageReportScreen
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel

object Routes {
    const val APP_LIST = "app_list"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"
    const val PRIVACY_POLICY = "privacy_policy"
    const val USAGE_REPORT = "usage_report"
    const val DASHBOARD = "dashboard"
    const val REPORTS_CENTER = "reports_center"
    const val SEARCH_SETTINGS = "search_settings"
    const val NOTIFICATION_REPORT = "notification_report"
    const val WRAPPED_REPORT = "wrapped_report"
    const val PRIVACY_REPORT = "privacy_report"

    // U1: Ayarlar alt-ekran hiyerarşisi — her ana kategori kendi route'unda
    const val SETTINGS_APPEARANCE = "settings_appearance"
    const val SETTINGS_LAUNCHER = "settings_launcher"
    const val SETTINGS_NOTIFICATIONS = "settings_notifications"
    const val SETTINGS_APPS = "settings_apps"
    const val SETTINGS_STATS = "settings_stats"
    const val SETTINGS_SECURITY = "settings_security"
    const val SETTINGS_ABOUT = "settings_about"
    const val PERMISSIONS_GUIDE = "permissions_guide"

    // Güvenlik: MainActivity EXTRA_OPEN_ROUTE dışarıdan (üçüncü parti intent) gelebilir —
    // yalnızca burada tanımlı bilinen route'lara navigate edilmeli (whitelist doğrulaması).
    val ALL: Set<String> = setOf(
        APP_LIST, CATEGORIES, SETTINGS, PRIVACY_POLICY, USAGE_REPORT, DASHBOARD,
        REPORTS_CENTER, SEARCH_SETTINGS, NOTIFICATION_REPORT, WRAPPED_REPORT, PRIVACY_REPORT,
        SETTINGS_APPEARANCE, SETTINGS_LAUNCHER, SETTINGS_NOTIFICATIONS, SETTINGS_APPS,
        SETTINGS_STATS, SETTINGS_SECURITY, SETTINGS_ABOUT, PERMISSIONS_GUIDE
    )

    fun isValid(route: String?): Boolean = route != null && route in ALL
}

@Composable
fun AppNavigation(
    viewModel: AppListViewModel,
    externalRoute: String? = null,
    onExternalRouteConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    androidx.compose.runtime.LaunchedEffect(externalRoute) {
        val route = externalRoute ?: return@LaunchedEffect
        // Güvenlik: exported MainActivity üzerinden dışarıdan enjekte edilebilecek route
        // string'i whitelist'e karşı doğrulanmadan navController'a verilmez.
        if (!Routes.isValid(route)) {
            timber.log.Timber.w("Bilinmeyen/gecersiz externalRoute yok sayildi: %s", route)
            onExternalRouteConsumed()
            return@LaunchedEffect
        }
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
        }
        onExternalRouteConsumed()
    }

    NavHost(navController = navController, startDestination = Routes.APP_LIST) {
        composable(Routes.APP_LIST) {
            AppListScreen(
                viewModel = viewModel,
                onNavigateToCategories = { navController.navigate(Routes.CATEGORIES) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.CATEGORIES) {
            CategoryEditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            // U1: Ayarlar hub'ı — her kategori satırı kendi alt route'una gider
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAppearance = { navController.navigate(Routes.SETTINGS_APPEARANCE) },
                onNavigateToLauncher = { navController.navigate(Routes.SETTINGS_LAUNCHER) },
                onNavigateToNotifications = { navController.navigate(Routes.SETTINGS_NOTIFICATIONS) },
                onNavigateToSearchSettings = { navController.navigate(Routes.SEARCH_SETTINGS) },
                onNavigateToApps = { navController.navigate(Routes.SETTINGS_APPS) },
                onNavigateToStats = { navController.navigate(Routes.SETTINGS_STATS) },
                onNavigateToSecurity = { navController.navigate(Routes.SETTINGS_SECURITY) },
                onNavigateToAbout = { navController.navigate(Routes.SETTINGS_ABOUT) },
                onNavigateToPermissionsGuide = { navController.navigate(Routes.PERMISSIONS_GUIDE) }
            )
        }
        // U1: Ayarlar alt ekranları
        composable(Routes.SETTINGS_APPEARANCE) {
            SettingsAppearanceScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_LAUNCHER) {
            SettingsLauncherScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSearchSettings = { navController.navigate(Routes.SEARCH_SETTINGS) }
            )
        }
        composable(Routes.SETTINGS_NOTIFICATIONS) {
            SettingsNotificationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNotificationReport = { navController.navigate(Routes.NOTIFICATION_REPORT) }
            )
        }
        composable(Routes.SETTINGS_APPS) {
            SettingsAppsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS_STATS) {
            SettingsStatsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReportsCenter = { navController.navigate(Routes.REPORTS_CENTER) },
                onNavigateToNotificationReport = { navController.navigate(Routes.NOTIFICATION_REPORT) }
            )
        }
        composable(Routes.SETTINGS_SECURITY) {
            SettingsSecurityScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.PERMISSIONS_GUIDE) {
            PermissionsGuideScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS_ABOUT) {
            SettingsAboutScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPrivacyPolicy = { navController.navigate(Routes.PRIVACY_POLICY) },
                onNavigateToUsageReport = { navController.navigate(Routes.USAGE_REPORT) },
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) }
            )
        }
        composable(Routes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.USAGE_REPORT) {
            UsageReportScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DASHBOARD) {
            AppOrganizerDashboardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                // Dashboard → detay raporu ("Detaylı Rapor →" linki, spec Risk 6)
                onNavigateToUsageReport = { navController.navigate(Routes.USAGE_REPORT) }
            )
        }
        composable(Routes.REPORTS_CENTER) {
            ReportsCenterScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToUsageReport = { navController.navigate(Routes.USAGE_REPORT) },
                onNavigateToNotificationReport = { navController.navigate(Routes.NOTIFICATION_REPORT) },
                onNavigateToWrappedReport = { navController.navigate(Routes.WRAPPED_REPORT) },
                onNavigateToPrivacyReport = { navController.navigate(Routes.PRIVACY_REPORT) },
            )
        }
        composable(Routes.WRAPPED_REPORT) {
            com.armutlu.apporganizer.presentation.ui.screens.WrappedReportScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNotificationReport = { navController.navigate(Routes.NOTIFICATION_REPORT) },
            )
        }
        composable(Routes.SEARCH_SETTINGS) {
            SearchSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.NOTIFICATION_REPORT) {
            com.armutlu.apporganizer.presentation.ui.screens.NotificationReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PRIVACY_REPORT) {
            com.armutlu.apporganizer.presentation.ui.screens.PrivacyReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
