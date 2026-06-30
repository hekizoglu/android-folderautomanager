package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val ROUTE_HOME = "launcher_home"
private const val ROUTE_FOLDER = "launcher_folder"

@Composable
fun LauncherNavGraph(
    viewModel: LauncherViewModel,
    onLaunchWidgetPicker: () -> Unit = {},
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        enterTransition = { slideInVertically { it } },
        exitTransition = { slideOutVertically { -it / 8 } },
        popEnterTransition = { slideInVertically { -it / 8 } },
        popExitTransition = { slideOutVertically { it } },
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                viewModel = viewModel,
                onLaunchWidgetPicker = onLaunchWidgetPicker,
                onNavigateToFolder = { folder ->
                    viewModel.openFolder(folder)
                    navController.navigate(ROUTE_FOLDER)
                },
            )
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
