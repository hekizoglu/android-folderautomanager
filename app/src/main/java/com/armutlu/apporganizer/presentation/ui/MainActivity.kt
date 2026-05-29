package com.armutlu.apporganizer.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.armutlu.apporganizer.presentation.navigation.AppNavigation
import com.armutlu.apporganizer.presentation.ui.theme.AppOrganizerTheme
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * MainActivity - Main entry point for the app
 * Sets up Compose UI and navigation
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: AppListViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.d("MainActivity created")
        
        setContent {
            AppOrganizerTheme {
                AppOrganizerApp(viewModel)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MainActivity destroyed")
    }
}

/**
 * Root composable for the app
 */
@Composable
fun AppOrganizerApp(viewModel: AppListViewModel) {
    Surface(
        color = Color.Transparent
    ) {
        AppNavigation(viewModel)
    }
}
