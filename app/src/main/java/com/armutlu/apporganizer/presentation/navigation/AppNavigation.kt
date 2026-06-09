package com.armutlu.apporganizer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.armutlu.apporganizer.presentation.ui.screens.AppListScreen
import com.armutlu.apporganizer.presentation.ui.screens.CategoryEditorScreen
import com.armutlu.apporganizer.presentation.ui.screens.SettingsScreen
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel

object Routes {
    const val APP_LIST = "app_list"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(viewModel: AppListViewModel, onSendBugReport: () -> Unit = {}) {
    val navController = rememberNavController()

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
                onSendBugReport = onSendBugReport
            )
        }
    }
}
