package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val ROUTE_HOME = "launcher_home"
private const val ROUTE_FOLDER = "launcher_folder"
private const val ROUTE_HOME_LAYOUT_EDITOR = "home_layout_editor"

@Composable
fun LauncherNavGraph(
    viewModel: LauncherViewModel,
    onLaunchWidgetPicker: () -> Unit = {},
) {
    val navController = rememberNavController()

    // Geçiş hız/eğrileri AllAppsDrawer ile aynı (tween 300 LinearOutSlowIn / 220 FastOutLinearIn)
    // — "invisible launcher" hissi için tüm ekran geçişleri tutarlı olmalı (D199)
    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        enterTransition = { slideInVertically(tween(300, easing = LinearOutSlowInEasing)) { it } },
        exitTransition = { slideOutVertically(tween(220, easing = FastOutLinearInEasing)) { -it / 8 } },
        popEnterTransition = { slideInVertically(tween(300, easing = LinearOutSlowInEasing)) { -it / 8 } },
        popExitTransition = { slideOutVertically(tween(220, easing = FastOutLinearInEasing)) { it } },
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                viewModel = viewModel,
                onLaunchWidgetPicker = onLaunchWidgetPicker,
                onEditHomeLayout = { navController.navigate(ROUTE_HOME_LAYOUT_EDITOR) },
                onNavigateToFolder = { folder ->
                    viewModel.openFolder(folder)
                    navController.navigate(ROUTE_FOLDER)
                },
            )
        }

        composable(ROUTE_HOME_LAYOUT_EDITOR) {
            HomeLayoutEditorScreen(onClose = { navController.popBackStack() })
        }

        composable(ROUTE_FOLDER) {
            FolderScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.closeFolder()
                    navController.popBackStack()
                },
            )
        }
    }
}
