package com.armutlu.apporganizer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.armutlu.apporganizer.presentation.ui.screens.AppListScreen
import com.armutlu.apporganizer.presentation.ui.screens.AppOrganizerDashboardScreen
import com.armutlu.apporganizer.presentation.ui.screens.CategoryEditorScreen
import com.armutlu.apporganizer.presentation.ui.screens.PrivacyPolicyScreen
import com.armutlu.apporganizer.presentation.ui.screens.ReportsCenterScreen
import com.armutlu.apporganizer.presentation.ui.screens.SearchSettingsScreen
import com.armutlu.apporganizer.presentation.ui.screens.SettingsScreen
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
        navController.navigate(route)
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
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPrivacyPolicy = { navController.navigate(Routes.PRIVACY_POLICY) },
                onNavigateToUsageReport = { navController.navigate(Routes.USAGE_REPORT) },
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToReportsCenter = { navController.navigate(Routes.REPORTS_CENTER) },
                onNavigateToSearchSettings = { navController.navigate(Routes.SEARCH_SETTINGS) }
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.REPORTS_CENTER) {
            ReportsCenterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD) },
                onNavigateToUsageReport = { navController.navigate(Routes.USAGE_REPORT) },
            )
        }
        composable(Routes.SEARCH_SETTINGS) {
            SearchSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
