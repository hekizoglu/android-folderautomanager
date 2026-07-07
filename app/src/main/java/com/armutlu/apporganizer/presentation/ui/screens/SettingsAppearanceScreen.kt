package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.armutlu.apporganizer.presentation.ui.theme.AppFont
import com.armutlu.apporganizer.presentation.ui.theme.AppTheme
import com.armutlu.apporganizer.presentation.ui.theme.ThemePreferences

/**
 * U1: Görünüm alt ekranı — tema ve yazı tipi ayarları.
 * Mevcut SettingsAppearanceSection composable'ını kendi route'unda sarar,
 * ana SettingsScreen artık sadece menü/hub görevi görür.
 */
@Composable
fun SettingsAppearanceScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences(context) }
    val currentTheme by themePrefs.themeFlow.collectAsState(initial = AppTheme.TEAL)
    val currentFont by themePrefs.fontFlow.collectAsState(initial = AppFont.DEFAULT)

    SettingsSubScreenScaffold(title = "Görünüm", onNavigateBack = onNavigateBack) {
        item {
            SettingsAppearanceSection(
                themePrefs = themePrefs,
                currentTheme = currentTheme,
                currentFont = currentFont,
            )
        }
    }
}
