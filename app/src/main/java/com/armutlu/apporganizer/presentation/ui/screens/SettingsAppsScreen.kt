package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel

/**
 * U1: Uygulamalar alt ekranı — sistem uygulamaları, uygulama yönetimi,
 * gizli uygulamalar ve Diğer klasörü (LLM sınıflandırma dahil).
 * Mevcut settingsAppsSection LazyListScope uzantısını kendi route'unda sarar.
 */
@Composable
fun SettingsAppsScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToClassificationReview: () -> Unit,
    onNavigateToFolderSuggestions: () -> Unit,
) {
    val context = LocalContext.current
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val otherApps by viewModel.otherApps.collectAsState()
    val llmCategorizing by viewModel.llmCategorizing.collectAsState()
    val llmProgress by viewModel.llmProgress.collectAsState()
    val classifyResult by viewModel.classifyResult.collectAsState()

    // LLM sınıflandırma sonucu Toast — tetikleyen aksiyonlar bu ekranda
    // olduğu için hub'dan buraya taşındı (U1)
    LaunchedEffect(classifyResult) {
        if (classifyResult.isNotBlank()) {
            android.widget.Toast.makeText(context, classifyResult, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    SettingsSubScreenScaffold(title = "Uygulamalar", onNavigateBack = onNavigateBack) {
        settingsAppsSection(
            showSystemApps = showSystemApps,
            viewModel = viewModel,
            hiddenApps = hiddenApps,
            otherApps = otherApps,
            llmCategorizing = llmCategorizing,
            llmProgress = llmProgress,
            onNavigateToClassificationReview = onNavigateToClassificationReview,
            onNavigateToFolderSuggestions = onNavigateToFolderSuggestions
        )
    }
}
